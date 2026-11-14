package io.github.animwidget;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v1.10.116（B 方案）：给桌面进程**直接读帧文件**用的只读 ContentProvider。
 *
 * 背景：`updateAppWidget` 里塞位图要过一次 Binder 事务，上限约 1MB → 一帧只能 ~22 万像素，
 * 组件拉大后就被放大发糊。改成 `setImageViewUri` 之后，Binder 里只走一个 URI 字符串，
 * **清晰度不再受 1MB 限制**（代价是桌面每帧要自己解码一次文件）。
 *
 * 权限：provider 本身 `exported=false`，由我们每次推送时显式
 * `grantUriPermission(launcher包名, uri, FLAG_GRANT_READ_URI_PERMISSION)` 单条授权 ——
 * 不需要把文件夹暴露成公共目录，也不污染相册。
 *
 * v1.10.119：路径改成 `w<组件id>/<画布指纹>/<帧号>.webp`（三层），
 * 这样换画布就换目录，旧画布整目录删掉，不会误删正在用的帧。
 */
public final class FrameProvider extends ContentProvider {

    public static final String AUTHORITY = "io.github.animwidget.frames";

    /** content://io.github.animwidget.frames/w<组件id>/<画布指纹>/<文件名> */
    @NotNull
    public static Uri uriFor(int widgetId, @NotNull String stamp, @NotNull String name) {
        return Uri.parse("content://" + AUTHORITY + "/w" + widgetId + "/" + stamp + "/" + name);
    }

    /** 最近一次成功 serve 的 URI（自检用）。 */
    private static volatile Uri LAST_URI;

    @Override
    public boolean onCreate() {
        return true;
    }

    /**
     * v1.10.335：**桌面到底取走了多少帧**（外部分析 P1 的最后一条判据）。
     *
     * 以前我们只能数"自己推了几次"（W23 渲染=），launcher 会不会合并/丢弃完全看不见：
     *   · 取用率 ≈ 推送率 → 每一帧都被取走，掉帧在 launcher 的绘制侧；
     *   · 取用率明显更低 → launcher 自己在限流，我们推得再快也没用。
     * 同时统计「回退」= 请求的帧文件不在（被淘汰或还没烘）→ 返回最近的一帧。
     * 回退可以不为 0（画面仍是正常画面），但**取不到任何文件**才算错（那种情况仍抛异常）。
     */
    private static final java.util.concurrent.atomic.AtomicLong FETCH_N =
            new java.util.concurrent.atomic.AtomicLong();

    /**
     * v1.10.338（外部分析：open→close 不一定假）：**先测，再决定信不信**。
     *
     * 争议点：`ParcelFileDescriptor.open(file, mode, handler, OnCloseListener)` 的回调
     * 到底是"对端关闭"还是"框架把 fd 写进 Binder 回复后就地关闭"。
     * 我不再断言，改成在真机上自检：
     *   拿自己的一个 URI，`ContentResolver.openFileDescriptor` 打开后**故意持有 50ms / 100ms**，
     *   看回调报回来的时长是不是 ≈50 / ≈100。
     *   · 报 ≈0~2ms  → 回调发生在框架侧，这个时长是假读数（丢弃，不写进 W10）；
     *   · 报 ≈50/100 → 回调真的跟到对端关闭，之后就能用它估 launcher 解码时长。
     * 判据还写进了 W35 那一行，下一份日志直接可判。
     */
    private static final java.util.concurrent.atomic.AtomicLong LAST_HOLD_MS =
            new java.util.concurrent.atomic.AtomicLong(-1L);
    private static final java.util.concurrent.atomic.AtomicBoolean SELF_TEST_RUN =
            new java.util.concurrent.atomic.AtomicBoolean();
    private static volatile android.os.Handler closeHandler;

    /** 回调所在的 Handler（构造在单独线程上，避免在 Binder 线程里要求 Looper）。 */
    private static android.os.Handler closeHandler() {
        android.os.Handler h = closeHandler;
        if (h == null) {
            synchronized (FrameProvider.class) {
                h = closeHandler;
                if (h == null) {
                    android.os.HandlerThread t = new android.os.HandlerThread("frame-close", -2);
                    t.start();
                    h = new android.os.Handler(t.getLooper());
                    closeHandler = h;
                }
            }
        }
        return h;
    }
    private static final java.util.concurrent.atomic.AtomicLong FETCH_FALLBACK =
            new java.util.concurrent.atomic.AtomicLong();

    /** v1.10.470（#4 第 2 步）：动图文件被取走的次数（自播看门狗用；0 = 桌面根本没来取）。 */
    private static final java.util.concurrent.ConcurrentHashMap<Integer,
            java.util.concurrent.atomic.AtomicLong> ANIM_FETCH =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** 这个组件的 `anim.webp` 有没有被桌面取走过。 */
    static boolean animFetched(int widgetId) {
        java.util.concurrent.atomic.AtomicLong n = ANIM_FETCH.get(Integer.valueOf(widgetId));
        return n != null && n.get() > 0L;
    }

    /** v1.10.336：按组件分桶的取用次数（W10 每个组件一行，直接对比「帧数 vs 取用」）。 */
    private static final java.util.concurrent.ConcurrentHashMap<Integer,
            java.util.concurrent.atomic.AtomicLong> FETCH_BY_ID =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * v1.10.336：**这个组件**这一窗口被桌面取走了几帧（读后清零）。
     *
     * 用来分清两种"看起来一样"的卡：我们推不上去（帧数就少）／桌面取走后又丢了
     * （帧数够、取用也够，但桌面每帧解码慢到画不出来）。后者我们改不了，
     * 唯一的杠杆是画布像素数。
     */
    public static long takeFetch(int widgetId) {
        java.util.concurrent.atomic.AtomicLong c = FETCH_BY_ID.get(Integer.valueOf(widgetId));
        return c == null ? 0L : c.getAndSet(0L);
    }

    /** @return {取用次数, 回退次数}，读后清零（每 3 秒被 W23 取一次）。 */
    @NotNull
    public static long[] takeStats() {
        return new long[]{FETCH_N.getAndSet(0L), FETCH_FALLBACK.getAndSet(0L)};
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NotNull Uri uri, @NotNull String mode)
            throws FileNotFoundException {
        java.util.List<String> segments = uri.getPathSegments();
        if (segments == null || segments.size() < 2 || segments.size() > 3) {
            throw new FileNotFoundException("bad uri: " + uri);
        }
        for (String segment : segments) {
            if (segment == null || segment.isEmpty() || segment.contains("..")
                    || segment.contains("/") || segment.indexOf('\\') >= 0) {
                throw new FileNotFoundException("bad path: " + uri);
            }
        }
        String dirName = segments.get(0);
        if (!dirName.startsWith("w")) {
            throw new FileNotFoundException("bad widget id: " + uri);
        }
        int widgetId;
        try {
            widgetId = Integer.parseInt(dirName.substring(1));
        } catch (Throwable th) {
            throw new FileNotFoundException("bad widget id: " + uri);
        }
        File file = resolve(widgetId, segments);
        if (file == null) {
            throw new FileNotFoundException("no context: " + uri);
        }
        if (!file.exists()) {
            // v1.10.335：**回退到同一目录里最近的一帧**，不再直接抛 FileNotFound。
            // 理由：画布换代时旧目录会被淘汰（磁盘上限），而 launcher 手里可能还捏着
            // 旧 URI（缓存的 RemoteViews 重新 inflate 时会再来取一次）。抛异常 → 那块卡片
            // 空白/报错；回退 → 拿到同内容的邻帧，画面正确、只是时刻差几帧。
            // v1.10.338（外部分析）：整代目录被删时"同目录"也空了 → 再加第二层：
            // 按组件 id 找到**当前代目录**（根下 mtime 最新的那个），在里面取最近帧。
            File fallback = nearestInSameDir(file);
            if (fallback == null) {
                fallback = nearestInNewestDir(getContext(), widgetId, file.getName());
            }
            if (fallback == null) {
                throw new FileNotFoundException(file.getAbsolutePath());
            }
            FETCH_FALLBACK.incrementAndGet();
            file = fallback;
        }
        FETCH_N.incrementAndGet();
        // v1.10.470（#4 第 2 步）：桌面**什么时候真的来取**那个动图文件 —— 这是「首帧延迟」
        // 唯一能测的量（推送时刻见 W45 动图推送，两者的时间差就是桌面开始解码的时间）。
        try {
            if (AnimPack.NAME.contentEquals(file.getName()) && getContext() != null) {
                java.util.concurrent.atomic.AtomicLong n = ANIM_FETCH.get(Integer.valueOf(widgetId));
                if (n == null) {
                    java.util.concurrent.atomic.AtomicLong fresh = new java.util.concurrent.atomic.AtomicLong();
                    java.util.concurrent.atomic.AtomicLong prev =
                            ANIM_FETCH.putIfAbsent(Integer.valueOf(widgetId), fresh);
                    n = prev != null ? prev : fresh;
                }
                n.incrementAndGet();
                Diag.warnThrottled(getContext(), "W45", "桌面取走动图 组件=" + widgetId
                        + " 大小=" + (file.length() / 1024L) + "KB（桌面已开始解码/自播）");
            }
        } catch (Throwable ignored) {
        }
        java.util.concurrent.atomic.AtomicLong per = FETCH_BY_ID.get(Integer.valueOf(widgetId));
        if (per == null) {
            per = new java.util.concurrent.atomic.AtomicLong();
            java.util.concurrent.atomic.AtomicLong prev =
                    FETCH_BY_ID.putIfAbsent(Integer.valueOf(widgetId), per);
            if (prev != null) {
                per = prev;
            }
        }
        per.incrementAndGet();
        LAST_URI = uri;
        // v1.10.340：Context 直接从 openFile 传进去。
        // （338 用的是静态 WeakReference，实测 23:06~23:10 那份日志里 W35 一行都没出来，
        //   说明那条路拿不到 Context 就静默 return 了 —— 静默失败的自检等于没有自检。）
        selfTestOnce(getContext() == null ? null : getContext().getApplicationContext());
        final long openAt = android.os.SystemClock.elapsedRealtime();
        try {
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY,
                    closeHandler(), new ParcelFileDescriptor.OnCloseListener() {
                        @Override
                        public void onClose(java.io.IOException e) {
                            LAST_HOLD_MS.set(android.os.SystemClock.elapsedRealtime() - openAt);
                        }
                    });
        } catch (java.io.IOException io) {
            // 带回调的重载会抛 IOException：退回最朴素的那个，别因为埋点把取帧搞失败
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        }
    }

    /**
     * v1.10.294：**MIME 按文件内容嗅探，不再按扩展名假定**。
     *
     * v1.10.292 起不透明帧走 JPEG（实测编码 180ms → 4.7ms），但文件名仍是 `.webp`。
     * 原来这里无条件返回 `image/webp` —— 桌面若按 MIME 选解码器，就会把一个 JPEG 当 WebP 解，
     * 轻则首帧失败，重则整块组件空着。按内容嗅探后，扩展名叫什么都不影响。
     */
    @Nullable
    @Override
    public String getType(@NotNull Uri uri) {
        java.util.List<String> segments = uri.getPathSegments();
        if (segments != null && segments.size() >= 2 && segments.get(0).startsWith("w")) {
            try {
                int widgetId = Integer.parseInt(segments.get(0).substring(1));
                File f = resolve(widgetId, segments);
                if (f != null) {
                    String sniffed = sniff(f);
                    if (sniffed != null) {
                        return sniffed;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        String path = uri.getLastPathSegment();
        if (path != null && path.endsWith(".png")) {
            return "image/png";
        }
        return "image/webp";
    }

    /** 只读前 4 字节判格式：JPEG FF D8 FF / PNG 89 50 4E 47 / RIFF+WEBP。 */
    @Nullable
    private static String sniff(@NotNull File f) {
        java.io.FileInputStream in = null;
        try {
            in = new java.io.FileInputStream(f);
            byte[] b = new byte[12];
            int n = in.read(b);
            if (n >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
                return "image/jpeg";
            }
            if (n >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
                return "image/png";
            }
            if (n >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
                return "image/webp";
            }
            return null;
        } catch (Throwable th) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /**
     * v1.10.338：**一次性自检** —— 我们自己持有 fd 50ms / 100ms，看回调报多少。
     * 只跑一次（进程内），失败静默。结论直接写在 W35 那一行里。
     */
    private static void selfTestOnce(@Nullable android.content.Context app) {
        if (app == null) {
            return;   // 拿不到 Context：不消费标志，下一次取帧再试（不静默放弃）
        }
        if (!SELF_TEST_RUN.compareAndSet(false, true)) {
            return;
        }
        Thread t = new Thread(() -> {
            long a = holdTest(app, 50L);
            long b = holdTest(app, 100L);
            String verdict;
            if (a >= 35L && a <= 90L && b >= 70L && b <= 170L) {
                verdict = "可信（回调跟到对端关闭，可用它估桌面解码时长）";
            } else if (a >= 0L && a <= 5L && b >= 0L && b <= 5L) {
                verdict = "假读数（回调发生在框架侧，丢弃，不写进 W10）";
            } else {
                verdict = "不确定（需要再测一次）";
            }
            Diag.warn(app, "W35", "取用自检 持50ms→报" + a + "ms 持100ms→报" + b + "ms 判定=" + verdict);
        }, "frame-selftest");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /** 打开我们自己的一个 URI（取最近一次 openFile 成功过的那个），持有 holdMs 后关闭。 */
    private static long holdTest(@NotNull android.content.Context context, long holdMs) {
        Uri uri = LAST_URI;
        if (uri == null) {
            return -2L;
        }
        ParcelFileDescriptor pfd = null;
        try {
            LAST_HOLD_MS.set(-1L);
            pfd = context.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd == null) {
                return -3L;
            }
            Thread.sleep(holdMs);
            pfd.close();
            pfd = null;
            // 回调在另一个线程上，给它一点时间
            for (int i = 0; i < 20 && LAST_HOLD_MS.get() < 0L; i++) {
                Thread.sleep(5L);
            }
            return LAST_HOLD_MS.get();
        } catch (Throwable th) {
            return -4L;
        } finally {
            if (pfd != null) {
                try {
                    pfd.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /**
     * v1.10.338：第二层回退 —— 按组件 id 找**当前代目录**（根下 mtime 最新的子目录），
     * 先找同名文件，再找帧号最接近的一帧。整代目录被删时它的"同目录"也空了，靠这层兜住。
     */
    @Nullable
    private static File nearestInNewestDir(@Nullable android.content.Context context,
                                           int widgetId, @NotNull String name) {
        try {
            if (context == null) {
                return null;
            }
            File root = new File(new File(context.getFilesDir(), "push"), "w" + widgetId);
            File[] subs = root.listFiles();
            if (subs == null || subs.length == 0) {
                return null;
            }
            File newest = null;
            long bestAt = Long.MIN_VALUE;
            for (File sub : subs) {
                if (sub == null || !sub.isDirectory() || sub.getName().contains(".trash.")) {
                    continue;
                }
                long at = sub.lastModified();
                if (at > bestAt) {
                    bestAt = at;
                    newest = sub;
                }
            }
            if (newest == null) {
                return null;
            }
            File same = new File(newest, name);
            if (same.isFile() && same.length() > 0L) {
                return same;
            }
            return nearestInSameDir(same);
        } catch (Throwable th) {
            return null;
        }
    }

    /**
     * v1.10.335：同一目录里按「帧号最接近」找一张现存的帧文件。
     *
     * 只为兜底：帧文件名形如 `00123.webp`，这里取序号最接近的那张。
     * 目录里一张都没有时返回 null（调用方仍然抛 FileNotFound，语义不变）。
     */
    @Nullable
    private static File nearestInSameDir(@NotNull File wanted) {
        try {
            File dir = wanted.getParentFile();
            if (dir == null || !dir.isDirectory()) {
                return null;
            }
            int want = indexOf(wanted.getName());
            File[] all = dir.listFiles();
            if (all == null || all.length == 0) {
                return null;
            }
            File best = null;
            long bestGap = Long.MAX_VALUE;
            for (File f : all) {
                if (f == null || !f.isFile() || f.getName().endsWith(".tmp")) {
                    continue;
                }
                int idx = indexOf(f.getName());
                if (idx < 0) {
                    continue;
                }
                long gap = (want < 0) ? 0L : Math.abs((long) idx - (long) want);
                if (gap < bestGap) {
                    bestGap = gap;
                    best = f;
                }
            }
            return best;
        } catch (Throwable th) {
            return null;
        }
    }

    /** `00123.webp` → 123；解析不出来返回 -1。 */
    private static int indexOf(@NotNull String name) {
        int dot = name.indexOf('.');
        String digits = dot > 0 ? name.substring(0, dot) : name;
        try {
            return Integer.parseInt(digits);
        } catch (Throwable th) {
            return -1;
        }
    }

    /**
     * v1.10.342：把我们自己的 content:// URI 反解成 File（不经过 provider、不计取用次数）。
     * 用于「共享位图」实验：同一张已烘好的帧，可以走 URI（桌面解码），也可以我们自己解码后
     * 用 setImageViewBitmap(asShared) 推过去（桌面零解码）。两条路必须读**同一份文件**。
     */
    @Nullable
    public static File fileOf(@Nullable android.content.Context context, @NotNull Uri uri) {
        try {
            if (context == null) {
                return null;
            }
            java.util.List<String> seg = uri.getPathSegments();
            if (seg == null || seg.size() != 3) {
                return null;
            }
            String dirName = seg.get(0);
            if (!dirName.startsWith("w")) {
                return null;
            }
            File root = new File(new File(context.getFilesDir(), "push"), dirName);
            return new File(new File(root, seg.get(1)), seg.get(2));
        } catch (Throwable th) {
            return null;
        }
    }

    /** 文件定位的唯一出处（openFile 与 getType 共用，避免两套路径拼法）。 */
    @Nullable
    private File resolve(int widgetId, @NotNull java.util.List<String> segments) {
        android.content.Context context = getContext();
        if (context == null || segments.size() < 2) {
            return null;
        }
        File dir = new File(new File(context.getFilesDir(), "push"), "w" + widgetId);
        File file = new File(dir, segments.get(1));
        if (segments.size() == 3) {
            file = new File(file, segments.get(2));
        }
        return file;
    }

    @Nullable
    @Override
    public Cursor query(@NotNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NotNull Uri uri, @NotNull ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NotNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NotNull Uri uri, @NotNull ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
