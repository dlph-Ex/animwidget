package io.github.animwidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.widget.RemoteViews;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v1.10.116（B 方案）：**让桌面自己读帧文件**，绕开 Binder 的 1MB 上限。
 *
 * 做法：
 *   1. 把合成好的这一帧编码成 WebP（帧数少走无损，帧数多走有损 95，和导出一致）写到
 *      `files/push/w<组件id>/<画布指纹>/<帧号>.webp`；
 *   2. 用 {@link FrameProvider} 生成 content:// URI，并**只对桌面包名**授予读权限；
 *   3. `RemoteViews.setImageViewUri()` —— Binder 里只有一个短字符串。
 *
 * v1.10.119 修的三件事（v1.10.118 的双视频实测暴露）：
 *   · **缓存目录改成「一个画布一个子目录」**：原来所有画布/配置混在一个目录里，靠
 *     「文件数超 400 就删掉最旧的一半」清理 —— 300 帧的组件换一次画布就会把正在用的
 *     那批删掉，命中率归零。现在换画布只删**其它画布目录**，当前画布一帧不动。
 *   · **编码质量按帧数选**：原来无条件 q100（API 30+ 就是无损），597×626 一帧要 40ms+
 *     且落盘体积是画布像素的 20 倍 —— 预热 300 帧要 6 秒，全是白等。现在 >40 帧走 q95
 *     （和导出帧同一质量档，肉眼无差），编码快 3 倍、体积小一个数量级。
 *   · **授权不再每帧发一次**：把「已授权过的 URI」记在会话里，循环第二圈起零授权开销。
 */
public final class WidgetUriPush {

    private static final String PREFS = "settings";
    private static final String PREF = "uri_mode";
    private static final String PUSH_DIR = "push";

    /** 连续写盘/推送失败计数（3 次判定该路径不可用）。 */
    private static final java.util.HashMap<Integer, Integer> FAILS = new java.util.HashMap<>();

    /** v1.10.119：本进程内已授权过的 `包名|URI`，避免每帧一次 binder 授权。 */
    private static final Set<String> GRANTED = new HashSet<>();

    /** 目录创建/清理的互斥（预热线程 + 推帧线程 + 配置页）。 */
    private static final Object LOCK = new Object();

    /** v1.10.119：命中/顶帧/推送耗时统计（每 3 秒的 W10 里打出来）。 */
    private static final java.util.HashMap<Integer, Stat> STAT = new java.util.HashMap<>();

    static final class Stat {
        int hit;
        int miss;
        int stale;
        /**
         * v1.10.351：**这一窗口的拍数**（守恒分母）。
         *
         * 为什么必须单独记：外部分析（D 家）指出，`命中 + 顶帧 + 跳过` 现在是互斥守恒的，
         * 但**没有「代」这个维度**。v1.10.351 引入「现渲染直推」之后，过渡期推的是现合成的
         * 原生尺寸帧 —— 它既不是「新代同号」，如果还按老口径计进 `顶帧`，那么修复上线后
         * 日志里 `顶帧` 仍会是 119/120，跟修复前一模一样。**改动是对的，日志会自证失败。**
         * 所以四维必须各自独立计数，并且和拍数做守恒断言。
         */
        int tick;
        /** v1.10.351：这一窗口「现渲染直推」成功上屏的拍数（= 过渡期在正常工作）。 */
        int render;
        long pushMax;
        int pushSlow;
        /** v1.10.123：这一窗口里「顶帧」最多落后多少帧（>8 说明管道跟不上，会被限住不再拉大）。 */
        int lagMax;
        /**
         * v1.10.272：这一窗口里「**连顶帧都没有、干脆不推**」的次数。
         *
         * 场景：刚拖完尺寸、新画布一帧还没烘出来、旧画布又已经被清掉 —— 这时现合成一帧要
         * 220ms（编码），会把**所有**组件一起饿到 3.8fps（日志实测 21 秒）。宁可这一拍不推
         * （桌面停在上一帧），也不让主线程为它堵 220ms。
         */
        int skip;
    }

    @NotNull
    private static Stat stat(int widgetId) {
        Stat s = STAT.get(Integer.valueOf(widgetId));
        if (s == null) {
            s = new Stat();
            STAT.put(Integer.valueOf(widgetId), s);
        }
        return s;
    }

    public static void noteHit(int widgetId) {
        synchronized (STAT) {
            stat(widgetId).hit++;
        }
    }

    public static void noteMiss(int widgetId) {
        synchronized (STAT) {
            stat(widgetId).miss++;
        }
    }

    /** v1.10.120：这一拍没能拿到「该帧」，先用一张已经烘好的相邻帧顶上（不阻塞主线程）。 */
    public static void noteStale(int widgetId) {
        synchronized (STAT) {
            stat(widgetId).stale++;
        }
    }

    /** v1.10.272：这一拍没有任何可推的帧（新画布还没烘、旧画布已清）—— 主动跳过，不现合成。 */
    public static void noteSkip(int widgetId) {
        synchronized (STAT) {
            stat(widgetId).skip++;
        }
    }

    /** v1.10.351：这一拍开始（守恒分母）。每拍每个组件一次。 */
    public static void noteTick(int widgetId) {
        synchronized (STAT) {
            stat(widgetId).tick++;
        }
    }

    /** v1.10.351：这一拍推上屏的是「现渲染」出来的原生尺寸帧（过渡期的正常工作状态）。 */
    public static void noteRender(int widgetId) {
        synchronized (STAT) {
            stat(widgetId).render++;
        }
    }

    /** v1.10.123：记一次「顶帧落后了几帧」，用来判断会不会出现「追帧加速」。 */
    public static void noteStaleLag(int widgetId, int lag) {
        if (lag <= 0) {
            return;
        }
        synchronized (STAT) {
            Stat st = stat(widgetId);
            if (lag > st.lagMax) {
                st.lagMax = lag;
            }
        }
    }

    /** 推一帧并计时 —— updateAppWidget 是同步 binder，桌面卡住时它会直接把我们拖慢。 */
    private static void send(@NotNull AppWidgetManager manager, int widgetId,
                             @NotNull RemoteViews views) {
        long t0 = SystemClock.elapsedRealtime();
        manager.updateAppWidget(widgetId, views);
        // v1.10.279：真内容已经上屏 → 占位图状态清位
        synchronized (PLACEHOLDER) {
            PLACEHOLDER.remove(Integer.valueOf(widgetId));
        }
        long cost = SystemClock.elapsedRealtime() - t0;
        synchronized (STAT) {
            Stat s = stat(widgetId);
            if (cost > s.pushMax) {
                s.pushMax = cost;
            }
            if (cost >= 10L) {
                s.pushSlow++;
            }
        }
    }

    /** 取一次统计并清零（W10 每 3 秒调用一次）。 */
    @NotNull
    public static String statLine(int widgetId) {
        synchronized (STAT) {
            Stat s = STAT.get(Integer.valueOf(widgetId));
            if (s == null) {
                return "";
            }
            int total = s.hit + s.miss + s.stale + s.skip;
            if (total <= 0 && s.tick <= 0 && s.render <= 0) {
                return "";
            }
            // v1.10.351：四维互斥计数 + 守恒。分母是**拍数**，不是「本该可以直接给帧」的次数 ——
            // 过渡期现渲染是正常工作状态，拿老分母算命中率会显示成一个刺眼的 0%。
            int accounted = s.hit + s.render + s.stale + s.skip;
            int shots = Math.max(s.tick, accounted);
            StringBuilder sb = new StringBuilder(96);
            sb.append(" 拍=").append(shots)
                    .append(" 命中新=").append(s.hit)
                    .append(" 现渲染=").append(s.render)
                    .append(" 邻帧=").append(s.stale)
                    .append(" 跳过=").append(s.skip);
            if (accounted != shots) {
                // 四维之和 ≠ 拍数 → 有拍没归类。看到这个就说明计数漏了一处，别拿后面的数当结论。
                sb.append(" ❗守恒=").append(accounted).append('/').append(shots);
            }
            if (s.stale > 0 && s.lagMax > 0) {
                sb.append(" 最大落后=").append(s.lagMax).append("帧");
            }
            if (s.miss > 0) {
                sb.append(" 缺帧=").append(s.miss).append("次");
            }
            if (s.pushMax > 0) {
                sb.append(" 推最慢=").append(s.pushMax).append("ms");
                if (s.pushSlow > 0) {
                    sb.append(" 超10ms=").append(s.pushSlow).append("次");
                }
            }
            s.hit = 0;
            s.miss = 0;
            s.stale = 0;
            s.skip = 0;
            s.tick = 0;
            s.render = 0;
            s.pushMax = 0L;
            s.pushSlow = 0;
            s.lagMax = 0;
            return sb.toString();
        }
    }

    private WidgetUriPush() {
    }

    public static boolean enabled(Context c) {
        try {
            return c.getSharedPreferences(PREFS, 0).getBoolean(PREF, false);
        } catch (Throwable th) {
            return false;
        }
    }

    public static void setEnabled(Context c, boolean on) {
        try {
            c.getSharedPreferences(PREFS, 0).edit().putBoolean(PREF, on).apply();
        } catch (Throwable ignored) {
        }
        FAILS.clear();
        synchronized (GRANTED) {
            GRANTED.clear();
        }
        synchronized (LAST) {
            LAST.clear();
        }
        // 开关一变就作废预热的「已热」记忆 —— 重新打开后要按当前画布重新热一遍
        WidgetPrewarm.resetAll();
    }

    // ---------------------------------------------------------------- 指纹 / 目录

    /** 画布指纹：形状/尺寸/参数一变就换目录名，避免读到旧图。 */
    @NotNull
    public static String stamp(@NotNull Context context, @NotNull WidgetConfig config, int w, int h) {
        return renderTag(config) + "_" + contentTag(context, config) + "_" + w + "x" + h;
    }

    /** 影响合成结果的参数（与以前一致）。 */
    @NotNull
    private static String renderTag(@NotNull WidgetConfig config) {
        return config.getLayout().name() + "_" + config.getShape().name() + "_"
                + config.getShapeAlpha() + "_" + (config.getBorder() ? 1 : 0) + "_"
                + config.getScalePercent() + "_" + config.getRotation() + "_"
                + config.getShapeRotation() + "_" + config.getContentScalePercent() + "_"
                + config.getMaskOffsetXPercent() + "_" + config.getMaskOffsetYPercent() + "_"
                + config.getOffsetXPercent() + "_" + config.getOffsetYPercent() + "_"
                + config.getShadowPercent() + "_" + (config.getFitContent() ? 1 : 0);
    }

    /**
     * v1.10.119：**素材内容的指纹**。
     *
     * 原来的指纹只有「形状/参数/画布尺寸」，没有素材本身 —— 于是「换个视频重新导出、
     * 形状参数一模一样」时，缓存文件名完全相同 → 桌面一直播上一个视频（且永远不刷新）。
     * 这里把「导出指纹」也算进去：帧文件的修改时间（每次 commitStaging 都会变）+ 素材来源
     * + 帧数/质量/边长/类型，任何一项变了就换目录。
     */
    @NotNull
    private static String contentTag(@NotNull Context context, @NotNull WidgetConfig config) {
        long mtime = 0L;
        try {
            mtime = WidgetStore.INSTANCE.frameFile(context, config.getWidgetId(), 0).lastModified();
        } catch (Throwable ignored) {
        }
        String src = config.getSourceName() + "|" + config.getSourceUri() + "|"
                + config.getFrameCount() + "|" + config.getQuality() + "|"
                + config.getMaxEdge() + "|" + config.getKind();
        int h = 0x811c9dc5;
        for (int i = 0; i < src.length(); i++) {
            h ^= src.charAt(i);
            h *= 0x01000193;
        }
        return Integer.toHexString(h) + Long.toHexString(mtime);
    }

    /**
     * 编码质量：帧数少（≤40）走无损保色；帧数多走 **q97**（v1.10.132 起，原 95）。
     *
     * 为什么提：P23 无损自检实测，766 帧视频（导出 q95）实际偏差是
     * 「最大通道差 27 / 48% 像素不同」；再叠一次 q95 就是两代有损。
     * 提到 97 后第二代的偏差明显收窄，代价是编码慢约 30%（预热本来就比播放快）。
     */
    /**
     * v1.10.406：**质量跟着帧率走**。
     *
     * <p>实测（D 家按 W27 拟合）：q97 单帧 ≈ 面积/5.5M×1000 + 20ms，q93 约为其 0.55 倍。
     * 50fps 时 6 线程在 1280×720 下 q97 只有 32fps（追不上），q93 约 53fps（够）。
     * 所以：≤40 帧仍走无损；≥45fps 走 q93；其余 q97。</p>
     */
    /** v1.10.407：两遍烘焙用的临时质量（0 = 用常规档位）。 */
    private static volatile int PASS_QUALITY = 0;
    /** v1.10.407：第二遍必须覆盖第一遍写下的文件。 */
    private static volatile boolean PASS_FORCE = false;

    /** v1.10.407：切换「这一遍」的质量与是否覆盖（两遍烘焙专用；用完必须 setPass(0,false)）。 */
    public static void setPass(int quality, boolean force) {
        PASS_QUALITY = quality;
        PASS_FORCE = force;
    }

    /**
     * v1.10.413：这一遍是不是「升档（第二遍）」。
     *
     * <p>它同时决定两件事，所以必须只有一个来源：
     * <ol>
     *   <li>要不要绕过「文件已存在就跳过」（{@link #writeEncoded} 里的短路）；
     *   <li>要不要跑在 nice10（后台优先级，播放期间不许抢解码的核）。
     * </ol>
     * 这两个本来都是"升档"的语义，分成两处判断必然漏一个。
     */
    public static boolean passForce() {
        return PASS_FORCE;
    }

    public static int encodeQuality(@NotNull WidgetConfig config) {
        if (PASS_QUALITY > 0) {
            return PASS_QUALITY;
        }
        if (config.getFrameCount() <= 40) {
            return 100;
        }
        return config.getFps() >= 45 ? 93 : 97;
    }

    @NotNull
    private static Bitmap.CompressFormat formatFor(int quality) {
        if (Build.VERSION.SDK_INT < 30) {
            return Bitmap.CompressFormat.PNG;
        }
        return quality >= 100 ? Bitmap.CompressFormat.WEBP_LOSSLESS : Bitmap.CompressFormat.WEBP_LOSSY;
    }

    @NotNull
    private static String extension() {
        return Build.VERSION.SDK_INT >= 30 ? ".webp" : ".png";
    }

    @NotNull
    private static String fileName(int index) {
        return String.format(java.util.Locale.US, "%05d", Integer.valueOf(index)) + extension();
    }

    /**
     * v1.10.153：某个组件推送缓存目录的总字节数（给 W13 用）。
     * q95/q97 这类「只改编码质量」的对比，体积差只有这一处能看出来（帧率/推最慢都不受影响）。
     */
    public static long cacheBytes(@NotNull Context context, int widgetId) {
        return sizeOf(widgetRoot(context, widgetId));
    }

    /**
     * v1.10.168：**只算当前这一批画布（某个指纹目录）**的字节数。
     *
     * 为什么需要：{@link #cacheBytes} 返回的是整个 `w<id>/` —— 换画布时旧目录要等
     * 「烘完再删」，所以总量里会**同时包含新旧两套**。日志只写总量会出现
     * 「画布从 290 缩到 276，缓存反而从 1615KB 涨到 3130KB」这种看起来矛盾的数字。
     * 拆成本组 / 旧之后，两数相加恒等于总量，口径就可复算了。
     */
    public static long cacheBytesOf(@NotNull Context context, int widgetId, @NotNull String stamp) {
        return sizeOf(new File(widgetRoot(context, widgetId), stamp));
    }

    /** v1.10.262：所有组件的推帧落盘总量（W33 内存/存储探针用）。 */
    public static long cacheBytesAll(@NotNull Context context) {
        return sizeOf(new File(context.getFilesDir(), PUSH_DIR));
    }

    @NotNull
    private static File widgetRoot(@NotNull Context context, int widgetId) {
        return new File(new File(context.getFilesDir(), PUSH_DIR), "w" + widgetId);
    }

    @NotNull
    private static File dirFor(@NotNull Context context, @NotNull WidgetConfig config, int w, int h) {
        return new File(widgetRoot(context, config.getWidgetId()), stamp(context, config, w, h));
    }

    /**
     * v1.10.469（#4）：**这一代烘焙产物所在的目录**（逐帧 WebP 就在里面）。
     *
     * <p>打包成单文件动图时要按 `00000.webp…` 的顺序把这些帧拼起来，
     * 所以这里把目录暴露出来 —— 不另建一套路径规则（两套规则迟早不一致）。</p>
     */
    /** v1.10.482：播放链只拿得到 (wid, stamp)，这是同一套目录规则的入口（不要再写第二套）。 */
    @NotNull
    static File dirOf(@NotNull Context context, int widgetId, @NotNull String stamp) {
        return new File(widgetRoot(context, widgetId), stamp);
    }

    @NotNull
    static File bakedDir(@NotNull Context context, @NotNull WidgetConfig config, int w, int h) {
        return dirFor(context, config, w, h);
    }

    /** 这一代里第 index 帧的文件（打包器用；可能不存在，调用方判）。 */
    @NotNull
    static File bakedFrame(@NotNull Context context, @NotNull WidgetConfig config,
                           int index, int w, int h) {
        return new File(dirFor(context, config, w, h), fileName(index));
    }

    /**
     * v1.10.470（#4 第 2 步）：这个组件**最新一代**的动图文件（`anim.webp`）；没有返回 null。
     *
     * <p>为什么按 mtime 取最新而不按 stamp 拼路径：换画布后旧目录可能还在（烘完才删），
     * 而 `anim.webp` 是烘完那一刻写的 —— mtime 最新的一定是当前代。
     * 扫描成本 = 每次心跳列一遍 `<w<id>>` 下的目录（通常 1~2 个）。</p>
     */
    @Nullable
    static File animFileNewest(@NotNull Context context, int widgetId) {
        File root = widgetRoot(context, widgetId);
        File[] dirs = root.listFiles();
        if (dirs == null) {
            return null;
        }
        File best = null;
        long bestAt = -1L;
        for (File dir : dirs) {
            if (dir == null || !dir.isDirectory()) {
                continue;
            }
            File file = new File(dir, AnimPack.NAME);
            if (!file.isFile()) {
                continue;
            }
            long at = file.lastModified();
            if (at > bestAt) {
                bestAt = at;
                best = file;
            }
        }
        return best;
    }

    /**
     * v1.10.470（#4 第 2 步）：把**一个**动图 URI 推给桌面（自播）。
     *
     * <p>与逐帧推送唯一的区别：URI 指向 `anim.webp`，而且**不需要再推第二次**。
     * 权限仍是单条授权（只给桌面包名），沿用 {@code grantOnce} 的缓存。</p>
     */
    static boolean pushAnimUri(@NotNull Context context, @NotNull AppWidgetManager manager,
                               @NotNull WidgetConfig config, @NotNull File file,
                               @NotNull String stamp) {
        int widgetId = config.getWidgetId();
        try {
            String launcher = launcherPackage(context);
            if (launcher == null) {
                return false;
            }
            Uri uri = FrameProvider.uriFor(widgetId, stamp, file.getName());
            if (!grantOnce(context, launcher, uri)) {
                return false;
            }
            boolean fit = WidgetRender.INSTANCE.fitViewFor(config, null, null);
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    fit ? R.layout.widget_media_fit : R.layout.widget_media);
            views.setImageViewUri(R.id.widget_image, uri);
            views.setOnClickPendingIntent(R.id.widget_image,
                    WidgetRender.INSTANCE.contentIntent(context));
            send(manager, widgetId, views);
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    /**
     * v1.10.414：这一代是不是**已经升过档**（q75→目标质量）。
     *
     * <p>没有这个标记的话，第二遍每次预热都会把 235 帧重编码一遍 —— 实测 413 里
     * 365/366 的 `首遍=10ms`（文件已热，全跳过）却仍然跑了 `总用时=12553ms`，
     * 那 12 秒纯属白烧，而且期间还在和播放抢 CPU。
     * 标记文件写在「这一代」的目录里（目录名含渲染参数 + 素材指纹），换参数就自动失效。
     */
    public static boolean upgradeDone(@NotNull Context context, @NotNull WidgetConfig config,
                                      int w, int h, int quality) {
        try {
            return new File(dirFor(context, config, w, h), "q" + quality).exists();
        } catch (Throwable th) {
            return false;   // 拿不准就当没升过：宁可多烘一次，也不要漏掉清晰度
        }
    }

    /** v1.10.414：标记这一代已升到目标质量（空文件；0 字节，不计入缓存体积）。 */
    public static void markUpgradeDone(@NotNull Context context, @NotNull WidgetConfig config,
                                       int w, int h, int quality) {
        try {
            File dir = dirFor(context, config, w, h);
            synchronized (LOCK) {
                if (!dir.isDirectory() && !dir.mkdirs()) {
                    return;
                }
            }
            new File(dir, "q" + quality).createNewFile();
        } catch (Throwable ignored) {
        }
    }

    /**
     * v1.10.119：开热之前先把**其它画布**的缓存删掉（当前画布保留，断点续热靠它）。
     * @return {删掉的旧目录数, 当前画布已占字节数}
     */
    @NotNull
    public static long[] prepare(@NotNull Context context, @NotNull WidgetConfig config, int w, int h) {
        File keep = dirFor(context, config, w, h);
        File root = widgetRoot(context, config.getWidgetId());
        int others = 0;
        long bytes = 0L;
        java.util.List<File> oldDirs = new java.util.ArrayList<>();
        synchronized (LOCK) {
            if (!keep.isDirectory()) {
                keep.mkdirs();
            }
            File[] subs = root.listFiles();
            if (subs != null) {
                for (File sub : subs) {
                    if (sub == null) {
                        continue;
                    }
                    if (sub.getName().equals(keep.getName())) {
                        bytes += sizeOf(sub);
                        continue;
                    }
                    if (sub.isDirectory()) {
                        others++;           // 旧画布：**先留着** —— 烘的这十几秒里还要拿它顶帧
                        oldDirs.add(sub);
                    } else {
                        sub.delete();       // 旧版的平铺文件（无子目录）一并清掉
                    }
                }
            }
            // v1.10.339：**旧代只留最新的一代**（其余立即删）。
            // 实测（23:07:54）：单个组件同时存在 3 代 = 本组 27.6MB + 旧 22.4MB = 50MB，
            // 整机 53MB —— 一次拖动就能把存储吃掉 50MB。留一代就够顶帧用
            // （顶帧只需要"另一套能看的帧"，不需要三套）。
            // 删除是安全的：FrameProvider.openFile 找不到文件时会回退到
            // 当前代目录里帧号最接近的一帧（v1.10.335/338），不会出现空白卡片。
            if (oldDirs.size() > 1) {
                oldDirs.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                for (int i = 1; i < oldDirs.size(); i++) {
                    File victim = oldDirs.get(i);
                    if (victim != null && victim.isDirectory()) {
                        deleteTree(victim);
                        others--;
                    }
                }
            }
            // 上次进程被杀留下的半截 .tmp（写盘失败/中断）一起清掉。
            // v1.10.121（日志里两次「URI 推送失败(写盘失败)」的根因）：**必须加 60 秒保护** ——
            // 推帧线程正在写 `00042.webp.<线程>.<纳秒>.tmp`，这里一删，它的 rename 就找不到目标，
            // 于是报「写盘失败」并回退位图推送（大组件还会撞 1MB 再压小，画面先糊一下）。
            File[] leftovers = keep.listFiles();
            if (leftovers != null) {
                long now = System.currentTimeMillis();
                for (File f : leftovers) {
                    if (f != null && f.getName().endsWith(".tmp")
                            && now - f.lastModified() > 60000L) {
                        f.delete();
                    }
                }
            }
        }
        return new long[]{others, bytes};
    }

    /**
     * v1.10.120：**预热跑完之后**才删旧画布目录。
     *
     * 原来在预热开始时就递归删，正好删在「渲染线程刚写好 .tmp、正要 rename 成正式名」的中间，
     * 目标目录没了 → 日志里那条 `URI 推送失败(写盘失败)`；而且桌面可能正在读旧目录里的文件。
     * 改到现在：那时新画布已经播了几秒，旧目录没人引用了；再加一道「最后修改时间超过 5 秒」的
     * 保险，避免刚烘完的上一批被误删。
     */
    public static void sweepTrash(@NotNull Context context, int widgetId, @NotNull String keepStamp) {
        try {
            File root = widgetRoot(context, widgetId);
            File[] subs = root.listFiles();
            if (subs == null) {
                return;
            }
            long now = System.currentTimeMillis();
            synchronized (LOCK) {
                for (File sub : subs) {
                    if (sub == null) {
                        continue;
                    }
                    String name = sub.getName();
                    if (name.equals(keepStamp)) {
                        continue;
                    }
                    if (!sub.isDirectory()) {
                        continue;
                    }
                    if (name.contains(".trash.") || now - sub.lastModified() > 5000L) {
                        deleteTree(sub);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /** 组件被删除：整包推送缓存一起删。 */
    public static void clearWidget(@NotNull Context context, int widgetId) {
        forgetRing(widgetId);   // v1.10.410：预读环一并释放（否则删掉的组件一直占环内存预算）
        // v1.10.481：这一路的共享身份也一并作废（共享表本身不清：键是内容寻址的，
        // 别的组件可能正在用；表按 24MB/12 帧 LRU 自己收敛）
        try {
            SHARE_BASE.remove(Integer.valueOf(widgetId));
        } catch (Throwable ignored) {
        }
        try {
            SHARED_CACHE.clear();   // v1.10.411：判据缓存跟着作废
        } catch (Throwable ignored) {
        }
        try {
            File root = widgetRoot(context, widgetId);
            synchronized (LOCK) {
                deleteTree(root);
            }
            synchronized (GRANTED) {
                GRANTED.clear();
            }
            synchronized (LAST) {
                LAST.remove(Integer.valueOf(widgetId));
            }
        } catch (Throwable ignored) {
        }
    }

    // ---------------------------------------------------------------- 写盘 / 命中

    /**
     * 只做「编码 + 落盘」（预热用，不推送）。
     * @return true = 文件已就绪（已存在也算成功）
     */
    /** v1.10.188（W27）：编码（compress）+ 写盘耗时累加，由预热收尾时汇总。 */
    private static final java.util.concurrent.atomic.AtomicLong ENC_MS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong ENC_N =
            new java.util.concurrent.atomic.AtomicLong();

    /**
     * v1.10.338（外部分析：`CPU=xx%` 的判别规则有漏洞 —— 100% 也可能是在小核/降频上跑）。
     * 所以再补两个只有编码段才有的量：
     *   · `编码CPU=`：**只包住 compress 那一段**的线程 CPU 时间 ÷ 墙钟（<90% 就是被抢占）；
     *   · `核分布`：每次编码结束时读 `/proc/self/task/<tid>/stat` 第 39 字段（所在 CPU），
     *     按核号统计平均编码耗时。若某些核慢 ≥1.5×，那就是小核 ——
     *     v332(19.5M px/s)/v333(27M px/s) 那 1.4× 的差异可以直接由它判定，不用猜。
     */
    private static final java.util.concurrent.atomic.AtomicLong ENC_CPU_NS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong ENC_WALL_MS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final long[] CORE_MS = new long[16];
    private static final long[] CORE_N = new long[16];

    /** 取走并清零编码统计。 */
    public static String takeEncodeStats() {
        long n = ENC_N.getAndSet(0L);
        long ms = ENC_MS.getAndSet(0L);
        long j = JPEG_N.getAndSet(0L);
        long encCpu = ENC_CPU_NS.getAndSet(0L);
        long encWall = ENC_WALL_MS.getAndSet(0L);
        long[] coreMs = new long[16];
        long[] coreN = new long[16];
        synchronized (CORE_MS) {
            System.arraycopy(CORE_MS, 0, coreMs, 0, 16);
            System.arraycopy(CORE_N, 0, coreN, 0, 16);
            java.util.Arrays.fill(CORE_MS, 0L);
            java.util.Arrays.fill(CORE_N, 0L);
        }
        if (n <= 0L) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("编码均=").append(ms / n).append("ms(").append(n).append("帧) JPEG=").append(j).append('/').append(n);
        if (encWall > 0L) {
            sb.append(" 编码CPU=").append(Math.round(encCpu * 100.0 / (encWall * 1000000.0))).append('%');
        }
        long fast = Long.MAX_VALUE;
        long slow = 0L;
        int fastCore = -1;
        int slowCore = -1;
        sb.append(" 核:");
        for (int i = 0; i < 16; i++) {
            if (coreN[i] <= 0L) {
                continue;
            }
            long avg = coreMs[i] / coreN[i];
            sb.append(' ').append(i).append('=').append(avg).append("ms×").append(coreN[i]);
            if (avg < fast) {
                fast = avg;
                fastCore = i;
            }
            if (avg > slow) {
                slow = avg;
                slowCore = i;
            }
        }
        if (fastCore >= 0 && slow > 0L) {
            sb.append(" 快核=").append(fastCore).append('/').append(fast).append("ms 慢核=")
                    .append(slowCore).append('/').append(slow).append("ms 倍数=")
                    .append(String.format(java.util.Locale.US, "%.2f", slow / (float) Math.max(fast, 1L)));
        }
        return sb.toString();
    }

    /** v1.10.338：本帧在哪个 CPU 上编码（`/proc/self/task/<tid>/stat` 第 39 字段）。 */
    private static int cpuOfCurrentThread() {
        try {
            String tid = String.valueOf(android.os.Process.myTid());
            String line;
            java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.FileReader("/proc/self/task/" + tid + "/stat"));
            try {
                line = r.readLine();
            } finally {
                r.close();
            }
            if (line == null) {
                return -1;
            }
            // 第 2 字段 comm 带空格且在括号里：从最后一个 ')' 之后切
            int p = line.lastIndexOf(')');
            if (p < 0 || p + 2 >= line.length()) {
                return -1;
            }
            String[] f = line.substring(p + 2).split(" ");
            // 之后第 0 个是第 3 字段(state)，第 39 字段 → 下标 39-3=36
            if (f.length <= 36) {
                return -1;
            }
            int cpu = Integer.parseInt(f[36]);
            return (cpu >= 0 && cpu < 16) ? cpu : -1;
        } catch (Throwable th) {
            return -1;
        }
    }

    // ---------------------------------------------------------------- v1.10.292：不透明帧走 JPEG
    /**
     * 编码耗时实测（87.7 万像素、q97）：WebP 约 154~180ms/帧，JPEG q95 约 5ms/帧（≈30 倍）。
     * WebP 是逐帧吞吐的最大头（占预热约 88%），而「完全不透明」的帧（视频、铺满、无蒙版角/阴影）
     * 根本不需要 alpha，所以：整帧 alpha 全是 255 → JPEG；否则保持原来的 WebP，行为不变。
     *
     * 只在「有损档」（帧数 > 40，quality<100）启用；≤40 帧仍走无损，画质口径不动。
     * 文件名仍是 .webp：桌面/BitmapFactory/ImageDecoder 按内容嗅探格式，不看扩展名。
     */
    static final int JPEG_QUALITY = 95;
    private static final java.util.concurrent.atomic.AtomicLong JPEG_N =
            new java.util.concurrent.atomic.AtomicLong();
    /** 组件 → 「有没有见过非不透明帧」的记忆；键 = 配置指纹 + 画布比例档（见 {@link #alphaKey}）。 */
    private static final java.util.concurrent.ConcurrentHashMap<String, Boolean> ALPHA_SEEN =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final ThreadLocal<java.nio.ByteBuffer> SCAN_BUF = new ThreadLocal<>();

    /** 画布比例分档（约 5% 一档）：letterbox 只在某些比例下出现，不能让它污染别的比例。 */
    static int ratioBucket(int w, int h) {
        if (w <= 0 || h <= 0) {
            return 0;
        }
        return Math.round(20f * (float) Math.log(w / (double) h));
    }

    @NotNull
    static String alphaKey(@NotNull Context context, @NotNull WidgetConfig config, int w, int h) {
        return config.getWidgetId() + "|" + renderTag(config) + "_" + contentTag(context, config)
                + "|r" + ratioBucket(w, h);
    }

    /**
     * 0 = 还没见过（乐观）；1 = 见过且全是不透明帧；-1 = 见过带 alpha 的帧（粘性，直到指纹变化）。
     * 预算（WidgetRender.budgetFor）用它决定能不能放开 1:1 画布。
     */
    static int alphaState(@NotNull Context context, @NotNull WidgetConfig config, int w, int h) {
        Boolean seen = ALPHA_SEEN.get(alphaKey(context, config, w, h));
        return seen == null ? 0 : (seen.booleanValue() ? -1 : 1);
    }

    private static void noteAlpha(@NotNull Context context, @NotNull WidgetConfig config,
                                  int w, int h, boolean opaque) {
        String k = alphaKey(context, config, w, h);
        if (!opaque) {
            ALPHA_SEEN.put(k, Boolean.TRUE);            // 粘性：见过一次带 alpha 就不再放开
        } else {
            ALPHA_SEEN.putIfAbsent(k, Boolean.FALSE);   // 不覆盖 TRUE
        }
    }

    /** 整帧 alpha 是否全为 255。先看四角（圆角蒙版/letterbox 几乎都能在这里就否决），再整块扫。 */
    static boolean isFullyOpaque(@NotNull Bitmap b) {
        try {
            if (b.isRecycled() || b.getConfig() != Bitmap.Config.ARGB_8888) {
                return false;       // 硬件位图/其它格式：不冒险，走 WebP
            }
            if (!b.hasAlpha()) {
                return true;
            }
            int w = b.getWidth();
            int h = b.getHeight();
            if (((b.getPixel(0, 0) >>> 24) & 0xFF) != 255 || ((b.getPixel(w - 1, 0) >>> 24) & 0xFF) != 255
                    || ((b.getPixel(0, h - 1) >>> 24) & 0xFF) != 255
                    || ((b.getPixel(w - 1, h - 1) >>> 24) & 0xFF) != 255) {
                return false;
            }
            // v1.10.405（P3-a）：**抽样代替全图扫描**。
            //
            // 原来这里把整张位图 copyPixels 到一个 direct buffer 再逐像素判 alpha：
            // 1920×1080 = 207 万次读取 + 一次 8MB 拷贝 ≈ **8~15ms/帧**，而且每次
            // capacity 变化还会 allocateDirect 一整块（GC/直接内存压力）。
            // 现在：四角已判 + 按 37 行步长抽样整行、行内每 4 像素取一个 →
            // 单帧约 2 万次读取（<1ms）。结论本身是**粘性**的（noteAlpha/alphaState 按
            // stamp 记住），所以只有画布换代后的头几十帧走这条路，抽样误差不会累积。
            final int step = Math.max(1, h / 37);
            final int[] row = new int[w];
            for (int y = 0; y < h; y += step) {
                b.getPixels(row, 0, w, 0, y, w, 1);
                for (int x = 0; x < w; x += 4) {
                    if ((row[x] >>> 24) != 0xFF) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    public static boolean writeEncoded(@NotNull Context context, @NotNull WidgetConfig config,
                                       int index, @NotNull Bitmap bitmap, int width, int height) {
        if (bitmap.isRecycled() || width <= 0 || height <= 0) {
            return false;
        }
        File dir = dirFor(context, config, width, height);
        File file = new File(dir, fileName(index));
        if (!PASS_FORCE && file.exists() && file.length() > 0L) {
            return true;   // v1.10.407：第二遍（升级质量）必须绕过这个短路
        }
        synchronized (LOCK) {
            if (!dir.isDirectory() && !dir.mkdirs()) {
                return false;
            }
        }
        int quality = encodeQuality(config);
        // 先写临时文件再 rename：预热（3 路）与推帧线程可能同时写同一帧，
        // 临时文件名带线程 + 纳秒，避免两边撞同一个 .tmp 把文件写坏
        File tmp = new File(dir, fileName(index) + "."
                + Thread.currentThread().getId() + "." + System.nanoTime() + ".tmp");
        OutputStream out = null;
        long _encStart = android.os.SystemClock.elapsedRealtime();
        try {
            // v1.10.189（W27 实测：**编码均=73ms/帧**，合成只有 3ms）：
            // 一帧 WebP 只有几十 KB，但直接写在 FileOutputStream 上是**无缓冲**的 ——
            // Skia 分很多小段写，每段一次 write 系统调用，4 个预热线程同时打盘时更甚。
            // 加 64KB 缓冲不改变任何字节（同一份 WebP），只是把几百次系统调用并成十几次。
            out = new java.io.BufferedOutputStream(new FileOutputStream(tmp), 64 * 1024);
            // v1.10.292：有损档 + 整帧不透明 → JPEG（≈5ms，WebP ≈154ms）；其余完全不变。
            boolean opaque = quality < 100 && Build.VERSION.SDK_INT >= 30 && isFullyOpaque(bitmap);
            if (quality < 100) {
                noteAlpha(context, config, width, height, opaque);
            }
            // v1.10.338：只包住 compress 这一段（不含读源帧/合成/写盘）→ 才能回答
            // 「是编码本身贵，还是线程被抢/被塞到小核」。
            long _cpu0 = android.os.Debug.threadCpuTimeNanos();
            long _wall0 = android.os.SystemClock.elapsedRealtime();
            boolean okEnc = opaque
                    ? bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                    : bitmap.compress(formatFor(quality), quality, out);
            long _wall = android.os.SystemClock.elapsedRealtime() - _wall0;
            long _cpu = android.os.Debug.threadCpuTimeNanos() - _cpu0;
            if (_cpu > 0L) {
                ENC_CPU_NS.addAndGet(_cpu);
            }
            if (_wall > 0L) {
                ENC_WALL_MS.addAndGet(_wall);
            }
            int _core = cpuOfCurrentThread();
            if (_core >= 0 && _wall > 0L) {
                synchronized (CORE_MS) {
                    CORE_MS[_core] += _wall;
                    CORE_N[_core]++;
                }
            }
            if (!okEnc) {
                return false;
            }
            if (opaque) {
                JPEG_N.incrementAndGet();
            }
            out.flush();
        } catch (Throwable th) {
            // v1.10.121：原来静默 return false，日志里只剩一句「写盘失败」，看不出是权限、空间还是并发
            try {
                Diag.warnThrottled(context, "W30", "缓存写盘失败 "
                        + th.getClass().getSimpleName()
                        + " 剩余空间=" + (context.getFilesDir().getUsableSpace() / 1048576L) + "MB"
                        + " 帧=" + index + " 画布=" + width + "x" + height);
            } catch (Throwable ignored2) {
            }
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable closeError) {
                    out = null;
                }
            }
        }
        ENC_MS.addAndGet(android.os.SystemClock.elapsedRealtime() - _encStart);
        ENC_N.incrementAndGet();
        if (tmp.renameTo(file)) {
            return true;
        }
        tmp.delete();
        return file.exists() && file.length() > 0L;
    }

    /** 这一帧是否已经编码落盘（命中就直接给 URI，连合成都省了）。 */
    @Nullable
    public static Uri cachedUri(@NotNull Context context, @NotNull WidgetConfig config,
                                int index, int width, int height) {
        File file = new File(dirFor(context, config, width, height), fileName(index));
        if (!file.exists() || file.length() <= 0L) {
            return null;
        }
        return FrameProvider.uriFor(config.getWidgetId(), stamp(context, config, width, height), file.getName());
    }

    /**
     * v1.10.120：往前找一帧**已经烘好**的帧（最多 back 帧）。
     *
     * 用途：预热还在烘的时候，让推帧循环「顶一张旧帧」而不是在主线程里现合成 ——
     * 813×853 现合成一帧要 125ms（日志实测），一拍堵 125ms 会把**同一拍里的另一个组件**
     * 也一起饿死（实测邻居从 29.9fps 掉到 7.7fps）。顶一帧只要 2~3ms，节拍照旧。
     */
    @Nullable
    public static Uri nearestCached(@NotNull Context context, @NotNull WidgetConfig config,
                                    int index, int width, int height, int back) {
        int count = config.getFrameCount();
        if (count <= 0) {
            return null;
        }
        File dir = dirFor(context, config, width, height);
        int start = ((index % count) + count) % count;
        int steps = Math.min(back, count);
        for (int step = 0; step <= steps; step++) {
            int i = ((start - step) % count + count) % count;
            File f = new File(dir, fileName(i));
            if (f.exists() && f.length() > 0L) {
                noteStaleLag(config.getWidgetId(), step);
                return FrameProvider.uriFor(config.getWidgetId(),
                        stamp(context, config, width, height), f.getName());
            }
        }
        return null;
    }

    /**
     * v1.10.194：尺寸变了必须**强制重推一次**。
     *
     * 画布无关帧（v1.10.192）之后，拖动尺寸不再改变帧的 URI ——
     * 于是 `pushCached` 的「同一张就不推」会把这次重推吃掉，
     * 桌面可能停在「按旧格子渲染的那一版」上被裁（v1.10.109 那个老 bug 复发）。
     * 尺寸静置后先把它清掉，保证这一拍一定真的 updateAppWidget。
     */
    public static void forgetLast(int widgetId) {
        synchronized (LAST) {
            LAST.remove(Integer.valueOf(widgetId));
        }
    }

    /** 每个组件「最后一张已经推上屏幕的帧」，用于「顶帧时不重复推同一张」。 */
    private static final java.util.HashMap<Integer, String> LAST = new java.util.HashMap<>();

    /**
     * v1.10.120：在**别的画布目录**里找同一帧号的成品。
     *
     * 场景：刚把组件拖大 → 新画布一帧都还没有，旧画布那 300 帧还在（prepare 还没跑）。
     * 这时候先把旧画布的同号帧顶上去 —— 内容正确、只是分辨率不同，ImageView 会自己缩放，
     * 视觉上是「糊一下立刻变清晰」，而不是「掉到 8fps 卡一下」。
     */
    @Nullable
    public static Uri nearestAnyCanvas(@NotNull Context context, @NotNull WidgetConfig config,
                                       int index, int width, int height) {
        return nearestAnyCanvas(context, config, index, width, height, 0, 0);
    }

    /**
     * v1.10.322（用户：\"导出到桌面后、未播放前有 1 帧画面先比本应的小一圈，再变回正常\"）：
     * **顶旧画布必须同时满足「像素不小于格子」**。
     *
     * 病根：桌面用 AT_MOST 量 RemoteViews，ImageView 取 {@code min(drawable.intrinsic, specSize)} ——
     * 位图比格子小，视图就会被量小一圈（老 bug v1.10.283）。导出后的第一拍新画布一帧都没有，
     * 于是这里会顶上一份**同比例但更小**的旧画布：比例一致 → fitCenter 把它放大铺满，
     * 但 intrinsic 仍小于格子 → vivo 把整张卡片量小 → 就是那\"一小圈\"。
     * 下一拍真帧（像素 ≥ 格子）到位 → 立刻恢复正常。所以症状正好是\"1 帧\"。
     *
     * @param minW 旧画布像素宽下限（传格子宽；0 = 不限制，为旧调用点保留）
     */
    public static Uri nearestAnyCanvas(@NotNull Context context, @NotNull WidgetConfig config,
                                       int index, int width, int height, int minW, int minH) {
        int count = config.getFrameCount();
        if (count <= 0 || width <= 0 || height <= 0) {
            return null;
        }
        int i = ((index % count) + count) % count;
        String name = fileName(i);
        File[] subs = widgetRoot(context, config.getWidgetId()).listFiles();
        if (subs == null) {
            return null;
        }
        float want = width / (float) height;
        File best = null;
        int bestGap = Integer.MAX_VALUE;
        for (File sub : subs) {
            if (sub == null || !sub.isDirectory() || sub.getName().contains(".trash.")) {
                continue;
            }
            // v1.10.121（反馈「播放后比例明显收缩」）：**只接受比例一致的那份旧画布**。
            // 原来随便拿最新的一份顶上 —— 例如 597×853（比例 0.700）的帧被顶进 813×853 的格子里，
            // 桌面 fitCenter 一缩，肉眼看到的就是「画面突然收窄」。
            int[] size = parseSize(sub.getName());
            if (size == null) {
                continue;
            }
            // v1.10.322：比格子小的旧画布一律不要（顶上去 = 卡片被量小一圈）
            if (minW > 0 && size[0] < minW) {
                continue;
            }
            if (minH > 0 && size[1] < minH) {
                continue;
            }
            float got = size[0] / (float) size[1];
            if (Math.abs(got - want) / want > 0.02f) {
                continue;
            }
            // v1.10.125：**挑宽度最接近的那一份**。
            // 换画布时目录里往往同时留着 597×373 / 813×508 / 990×618 三份，
            // 以前按 listFiles() 的返回顺序取第一个 —— 可能挑到最小那份，画面会先糊一下再变清。
            int gap = Math.abs(size[0] - width);
            if (gap < bestGap) {
                File f = new File(sub, name);
                if (f.exists() && f.length() > 0L) {
                    best = sub;
                    bestGap = gap;
                }
            }
        }
        if (best != null) {
            return FrameProvider.uriFor(config.getWidgetId(), best.getName(), name);
        }
        return null;
    }

    /**
     * v1.10.272：**兜底顶帧 —— 连比例都不要了**（比 {@link #nearestAnyCanvas} 宽松）。
     *
     * <h3>为什么需要它（实测日志）</h3>
     * 拖动尺寸时画布是 597x1305 → 813x1305 → 1030x1305 → 1030x853 这样连跳的，
     * **每一档的比例都不一样**。{@code nearestAnyCanvas} 要求比例一致（±2%，v1.10.121 为防止
     * "画面收窄"加的），于是这一整段时间里它一律返回 null → 渲染路径掉进「现场合成」
     * 分支：1030x853 一帧 = 合成 7ms + **编码 222ms**。实测后果：
     * <pre>
     *   04:39:48  三个组件同时 3.8fps（帧隔 p50=260ms、单帧=251ms）
     *   04:39:51  同上 3.8fps —— 一直持续到 04:40:04 新画布烘完（共 17 秒）
     * </pre>
     * 单线程推帧被 250ms/帧的合成堵死，**不相干的另外两个组件也跟着掉**（用户："桌面三组件
     * 出现 fps 抖动"）。所以这里退一步：拿一份**任意比例**的旧画布顶上，配 `fitView=true`
     * （fitCenter 等比内接、四周露壁纸）—— 比例不对只是"小一圈"，比"截断 + 3.8fps"好得多。
     *
     * @return 帧文件最短缺的那份旧画布上的 URI；一帧都没有时返回 null
     */
    @Nullable
    public static android.net.Uri nearestLoose(@NotNull Context context, @NotNull WidgetConfig config,
                                               int index, int width, int height, int maxGap) {
        int count = config.getFrameCount();
        if (count <= 0 || width <= 0 || height <= 0) {
            return null;
        }
        int i = ((index % count) + count) % count;
        File[] subs = widgetRoot(context, config.getWidgetId()).listFiles();
        if (subs == null) {
            return null;
        }
        // 先挑「像素最多」的那份旧画布：它是被缩小的，不会糊
        File best = null;
        long bestArea = -1L;
        for (File sub : subs) {
            if (sub == null || !sub.isDirectory() || sub.getName().contains(".trash.")) {
                continue;
            }
            int[] size = parseSize(sub.getName());
            if (size == null) {
                continue;
            }
            long area = (long) size[0] * (long) size[1];
            if (area > bestArea) {
                best = sub;
                bestArea = area;
            }
        }
        if (best == null) {
            return null;
        }
        // 在它里面找离 index 最近、且真的存在的一帧
        for (int step = 0; step <= Math.max(maxGap, 1); step++) {
            int[] cand = step == 0 ? new int[]{i} : new int[]{(i - step + count) % count, (i + step) % count};
            for (int idx : cand) {
                File f = new File(best, fileName(idx));
                if (f.exists() && f.length() > 0L) {
                    return FrameProvider.uriFor(config.getWidgetId(), best.getName(), fileName(idx));
                }
            }
        }
        return null;
    }

    /** 目录名（=画布指纹）结尾是 `_<宽>x<高>`，解析出来用于比例比对。 */
    @Nullable
    private static int[] parseSize(@NotNull String dirName) {
        try {
            int i = dirName.lastIndexOf('_');
            if (i < 0 || i + 1 >= dirName.length()) {
                return null;
            }
            String tail = dirName.substring(i + 1);
            int x = tail.indexOf('x');
            if (x <= 0 || x + 1 >= tail.length()) {
                return null;
            }
            int w = Integer.parseInt(tail.substring(0, x));
            int h = Integer.parseInt(tail.substring(x + 1));
            if (w <= 0 || h <= 0) {
                return null;
            }
            return new int[]{w, h};
        } catch (Throwable th) {
            return null;
        }
    }

    /**
     * v1.10.279：桌面此刻显示的是不是**我们自己推的占位图**（widget_preview，深色方块+绿三角）。
     *
     * 这个状态只由两条路径改：{@code showPlaceholder}（置位）和 {@code send}（真内容，清位）。
     * 不用 LAST / EVER 那种缓存状态，因为它们会被 forgetLast（拖动改尺寸）清掉 ——
     * 那样每次拖尺寸都会被误判成"没见过帧"，于是每拍都现合成，回到 v1.10.272 的 3.8fps。
     *
     * 用处只有一个：占位图必须在**下一拍立刻**被真内容顶掉（日志 09:53:23 保存 → 09:53:32
     * 才由预热补推，中间 9 秒桌面挂的就是这张占位图）；只要桌面已经有真内容，这拍允许不推。
     */
    private static final java.util.HashSet<Integer> PLACEHOLDER = new java.util.HashSet<>();

    /**
     * v1.10.286：**上一次成功推上去的画布尺寸**。
     *
     * 用户："变长条又没实时渲染"。病根：拖动时画布一变，render() 就撞上"预热还没烘好"分支，
     * 而那条分支在没有同尺寸缓存时**什么都不推**（noteSkip）→ 桌面停在旧画布上被拉伸，
     * 看着就是"尺寸变了但画面不动"。现在只要"桌面拿到的尺寸 ≠ 这一拍的画布尺寸"，
     * 就当场合成一帧推过去（一帧成本 5~30ms，拖动期间也扛得住）。
     */
    private static final java.util.HashMap<Integer, int[]> LAST_SIZE = new java.util.HashMap<>();

    public static void notePushedSize(int widgetId, int w, int h) {
        synchronized (LAST_SIZE) {
            LAST_SIZE.put(Integer.valueOf(widgetId), new int[]{w, h});
        }
    }

    public static boolean sizeStale(int widgetId, int w, int h) {
        synchronized (LAST_SIZE) {
            int[] s = LAST_SIZE.get(Integer.valueOf(widgetId));
            return s == null || s[0] != w || s[1] != h;
        }
    }

    /**
     * v1.10.309（用户："导出到桌面第 1 帧先小一圈再变回正常，概率出现"）：
     * **上一次推上去的帧数**。素材一换（导出）帧数就变；此时若还推「上一份素材的旧画布」，
     * fitCenter 会把那份**不同比例/不同留边**的画布内接进新格子 → 画面先小一圈，真帧到了再长回来。
     * 帧数不同 = 换了素材 = 不许顶旧画布，宁可这一拍不推。
     */
    private static final java.util.HashMap<Integer, Integer> LAST_COUNT = new java.util.HashMap<>();

    public static void notePushedCount(int widgetId, int count) {
        synchronized (LAST_COUNT) {
            LAST_COUNT.put(Integer.valueOf(widgetId), Integer.valueOf(count));
        }
    }

    public static boolean countChanged(int widgetId, int count) {
        synchronized (LAST_COUNT) {
            Integer c = LAST_COUNT.get(Integer.valueOf(widgetId));
            return c == null || c.intValue() != count;
        }
    }

    public static void notePlaceholder(int widgetId) {
        synchronized (PLACEHOLDER) {
            PLACEHOLDER.add(Integer.valueOf(widgetId));
        }
    }

    public static boolean showingPlaceholder(int widgetId) {
        synchronized (PLACEHOLDER) {
            return PLACEHOLDER.contains(Integer.valueOf(widgetId));
        }
    }

    /**
     * v1.10.342（外部分析方案 A）：**共享位图通道** —— 让桌面「零解码」。
     *
     * 外部给出一个关键事实：`setImageViewUri` 在 API 28+ 的 ImageView 里走
     * `ImageDecoder.decodeDrawable`，且被强制 `ALLOCATOR_SOFTWARE` ——
     * 也就是说**每一帧都在 launcher 进程里用软件解一遍**。他们把 64M px/s 解释成
     * "单线程软件解码打满"：42.6万 px × 150 帧/秒 = 63.9M px/s ≈ 6.7ms/帧，正好一条解码线程。
     *
     * 如果这是真的，那么**把解码搬到我们这边**就能彻底绕开这道墙：
     *   `Bitmap.asShared()` → 像素放共享内存 → Binder 里只走一个 fd（不是 3MB 像素），
     *   `setImageViewBitmap(shared)` → 桌面只设置 drawable、零解码，剩下的是纹理上传。
     *
     * 实验范围（刻意收窄，不动已知好用的路径）：
     *   · 只在**画布面积 > 45 万 px** 时启用（662x643 = 42.6 万 继续走 URI，保持现状）；
     *   · 每帧新建 RemoteViews（复用会让 launcher 的 bitmap 缓存累积，最终撞内存上限）；
     *   · 解码失败 / asShared 抛异常 → 立刻回落到原 URI 路径，不会把画面搞没。
     * 日志判据：W10/W23 的 `传输=` 会变成 `位图共享`；`单帧=` 会从 1~2ms 涨到 ~10ms（我们替桌面解码）。
     */
    /** v1.10.344：**按格子面积**决定走不走共享位图（v1.10.411 起它只是「大格子一律零解码」的兜底）。 */
    static final int SHARED_GRID_MIN_AREA = 450000;

    /**
     * v1.10.411：桌面进程每秒能解的像素（实测口径：3 路 50fps×92 万 px ≈ 138M 会明显掉帧，
     * 单路 ≈ 46M 尚可）。这个数只用来定「要不要让桌面解码」，不参与画布计算。
     */
    static final long DESKTOP_PX_PER_SEC = 64L << 20;
    /** 一路组件最多占桌面预算的 1/4：超过就换共享位图（桌面零解码）。 */
    static final long SHARED_PX_PER_SEC = DESKTOP_PX_PER_SEC / 4;

    /** 判据缓存（只在推帧线程上读写）。 */
    private static final java.util.HashMap<Long, Boolean> SHARED_CACHE = new java.util.HashMap<>();

    /**
     * v1.10.344：这张卡片要不要走「共享位图」通道。
     *
     * 343 的判据是**画布面积 > 45 万**，结果和 v1.10.341 的桌面解码预算上限撞在一起：
     * 3 个组件时每个画布被夹到 42.6 万（< 45 万）→ 共享位图被自动关掉 → 又回到 URI。
     * 用户实测到的"导出后正常、过一会儿又卡"就是这个切换（`渲染=3` 的窗口 422 次推送全走 URI）：
     * 现在改成看**格子**：格子大 = 这张卡片本来就该清晰 = 走共享位图，并**同时豁免 341 的画布夹取**。
     *
     * <p><b>v1.10.411：看格子也是错的。</b>实测（13:53~13:56 两份日志，同素材同参数 3 个组件）：
     * <pre>
     *   格子 662x643（42.6 万 &lt; 45 万）→ 走 URI   → 桌面每帧自己解 92 万 px × 3 路 = 138M px/s
     *                                              → W10 全部达标（p95=22ms 掉帧=0）而**画面在卡**
     *   格子 879x643（56.5 万 ≥ 45 万）→ 走共享   → 桌面取用=0、零解码 → 当场丝滑
     * </pre>
     * 两次的画布、帧率、素材**一模一样**，唯一的差别是「谁在解码」。桌面每秒只能解约 64M px，
     * 3 路 50fps 的 1280×720 要它解 138M px/s —— 超了 2 倍，卡是必然的，与我方无关。
     * 所以判据只能挂在「**桌面每帧要解多少像素**」上，而格子面积跟这个量没有任何关系
     * （用户那次"拖一下变流畅"并不是拖对了，只是把格子跨过了 45 万这条无关的门槛）。
     *
     * <p>现在的判据 = `画布px × fps ≥ 桌面预算的 1/4（16M px/s）` → 走共享位图。
     * 这个阈值**故意不随组件数变**：若按 `64M/当前路数` 算，别的组件一暂停就会让本组件
     * 在 URI/共享之间来回翻，那种抖动比一开始就固定在共享更难看。
     */
    static boolean wantShared(@Nullable Context context, @Nullable WidgetConfig config,
                              @Nullable int[] box) {
        try {
            if (config == null || config.getFrameCount() <= 1) {
                return false;            // 静态图只推一次，没必要
            }
            if (MultiCell.isMulti(context, config.getWidgetId())) {
                return true;             // v1.10.491：多格必须走位图（URI 只能填一个格子）
            }
            if (android.os.Build.VERSION.SDK_INT < 27) {
                return false;
            }
            if (box == null || box[0] <= 0 || box[1] <= 0) {
                return false;            // 拿不到格子就算不出画布 → 保持原样
            }
            if ((long) box[0] * (long) box[1] >= SHARED_GRID_MIN_AREA) {
                return true;             // 老判据保留：大格子本来就该零解码
            }
            if (context == null) {
                return false;
            }
            long key = sharedKey(context, config, box);
            Boolean hit = SHARED_CACHE.get(Long.valueOf(key));
            if (hit == null) {
                int[] canvas = WidgetRender.canvasFor(context, config, box);
                boolean shared = canvas != null && canvas[0] > 0 && canvas[1] > 0
                        && sharedForCanvas(config, box, (long) canvas[0] * (long) canvas[1]);
                if (SHARED_CACHE.size() > 64) {
                    SHARED_CACHE.clear();
                }
                hit = Boolean.valueOf(shared);
                SHARED_CACHE.put(Long.valueOf(key), hit);
            }
            return hit.booleanValue();
        } catch (Throwable th) {
            return false;
        }
    }

    /**
     * 判据本体（**不查画布，所以 {@link WidgetRender#canvasFor0} 可以安全调用它**）：
     * 给定「如果不做夹取，本该用的画布像素」，桌面每秒要解 `canvasPx × fps` 这么多像素，
     * 超过它预算的 1/4 就让桌面别解了（改由我们 4 线程解码 + 共享位图）。
     *
     * <p>注意调用方向：`canvasFor0` 先问「会不会走共享」，再决定要不要按桌面预算夹小画布 ——
     * 若这里反过来去算画布，就成了递归（410 的版本就是这么埋的）。
     */
    static boolean sharedForCanvas(@Nullable WidgetConfig config, @Nullable int[] box, long canvasPx) {
        if (config == null || config.getFrameCount() <= 1) {
            return false;
        }
        if (android.os.Build.VERSION.SDK_INT < 27) {
            return false;
        }
        if (box == null || box[0] <= 0 || box[1] <= 0) {
            return false;
        }
        if ((long) box[0] * (long) box[1] >= SHARED_GRID_MIN_AREA) {
            return true;
        }
        if (canvasPx <= 0L) {
            return false;
        }
        return canvasPx * Math.max(config.getFps(), 1) >= SHARED_PX_PER_SEC;
    }

    /**
     * 判据缓存键：`id|格子w|格子h|fps|帧数`。`canvasFor` 要读 SharedPreferences（`getStringSet`
     * 每次都要防御性拷贝一份），而这条路径是 50fps × N 在跑的 —— 不缓存就是每拍几百次拷贝。
     * 只在推帧线程上读写，不需要加锁。画质档位改变会连带重推/重烘，格子或帧数也会变，键自然失效。
     */
    private static long sharedKey(@Nullable Context context, @NotNull WidgetConfig config,
                                 @NotNull int[] box) {
        int quality = 0;
        try {
            quality = context.getSharedPreferences("settings", 0)
                    .getInt(WidgetRender.PREF_ANIM_QUALITY, 0);
        } catch (Throwable ignored) {
        }
        long k = ((long) config.getWidgetId()) & 0xFFFFL;
        k = (k << 13) | (((long) box[0]) & 0x1FFFL);
        k = (k << 13) | (((long) box[1]) & 0x1FFFL);
        k = (k << 7) | (((long) Math.min(Math.max(config.getFps(), 1), 127)) & 0x7FL);
        k = (k << 3) | (((long) quality) & 0x7L);
        k = (k << 11) | (((long) config.getFrameCount()) & 0x7FFL);
        return k;
    }

    /**
     * v1.10.344：共享位图通道（后台解码 + 预读，推帧线程只取现成的）。
     *
     * 343 的实测（01:36:07 单组件窗口）：`解码均18ms×113 推152 滞后150` ——
     * 推送 50fps 但每一帧都是"上一帧"，因为**解码只排了一个在飞**，
     * 一个解码 18ms + 一拍 20ms = 每个 tick 只来得及解一帧，永远差一帧。
     * `换图=` 直接报"真正换了几张不同的图"，它才是用户看到的帧率。
     *
     * <p>v1.10.410：线程数**是 4**（HANDOFF §18.3 一直写的 3 是错的，W23 行里的
     * 「3 条后台线程解码」也是同一条错误文案）。池改成<b>优先级队列</b>：
     * 紧迫度 0 = 这一拍就要的帧，先于任何组件的预读帧。
     *
     * <p><b>⚠️ 队列里的每个元素都必须是 {@link Pri}</b>：{@code PriorityBlockingQueue} 在 offer
     * 与 siftUp 时会强转 Comparable，混进一个裸 lambda 就抛 ClassCastException
     * （在提交侧被 catch 吞掉 → 那条通道静默失效）。所以三个提交点（解码 / 现合成 / 过渡期现合成）
     * 统一走 {@link #watchful} 包一层，不许直接把裸 lambda 丢进去。
     */
    private static final java.util.concurrent.ThreadPoolExecutor BMP_POOL =
            // v1.10.494：4 → 6（六路同播时每路各有一个在飞任务，4 线程会互相等）
            new java.util.concurrent.ThreadPoolExecutor(6, 6, 0L,
                    java.util.concurrent.TimeUnit.MILLISECONDS,
                    new java.util.concurrent.PriorityBlockingQueue<Runnable>(),
                    r -> {
                        // v1.10.347（自审修 bug）：**优先级必须设在新线程里**。
                        // 345 把 `setThreadPriority(-2)` 写在 ThreadFactory 里 —— 那是在**提交任务的线程**
                        // （推帧线程，本身是 -4）上执行的，等于把推帧线程从 -4 降到 -2，
                        // 而真正干活的解码线程仍是 DEFAULT(0)。方向完全反了。
                        Thread t = new Thread(() -> {
                            android.os.Process.setThreadPriority(-2);   // 比预热(0)高、比推帧(-4)低
                            r.run();
                        }, "frame-decode");
                        return t;
                    });

    /** 提交序号：同紧迫度按先来后到。 */
    private static final java.util.concurrent.atomic.AtomicLong BG_SEQ =
            new java.util.concurrent.atomic.AtomicLong();

    /**
     * v1.10.410：后台任务的统一外壳。{@code PriorityBlockingQueue} 里的<b>唯一</b>元素类型。
     *
     * <p>{@code dist} 越小越先跑：0 = 这一拍就要的解码帧，1 = 现合成/过渡期现合成，
     * 2..5 = 预读帧。同级按提交序，保证不会饿死。
     */
    private static final class Pri implements Runnable, Comparable<Pri> {
        final int dist;
        final long seq;
        final Runnable body;

        Pri(int dist, long seq, Runnable body) {
            this.dist = dist;
            this.seq = seq;
            this.body = body;
        }

        @Override
        public int compareTo(Pri o) {
            if (dist != o.dist) {
                return dist < o.dist ? -1 : 1;
            }
            return seq < o.seq ? -1 : (seq > o.seq ? 1 : 0);
        }

        @Override
        public void run() {
            body.run();
        }
    }

    /** 把任意任务按紧迫度丢进解码池（唯一入口，见 BMP_POOL 注释里的坑）。 */
    static void watchful(int dist, @NotNull Runnable body) {
        BMP_POOL.execute(new Pri(dist, BG_SEQ.incrementAndGet(), body));
    }

    /** v1.10.410：每个组件一个预读环（取代字符串键的 LinkedHashMap，见 {@link FrameRing}）。 */
    private static final FrameRing<Bitmap> RING = new FrameRing<>();
    private static final java.util.concurrent.atomic.AtomicLong BMP_MS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong BMP_N =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong BMP_PUSH =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong BMP_FRESH =
            new java.util.concurrent.atomic.AtomicLong();
    /** v1.10.410：提交→解完（含排队）的毫秒总和，÷BMP_N = `延迟均`。 */
    private static final java.util.concurrent.atomic.AtomicLong BMP_LAT =
            new java.util.concurrent.atomic.AtomicLong();
    /** v1.10.410：取不到 exact 也没有近邻帧、这拍不推（屏上保持）的次数。 */
    private static final java.util.concurrent.atomic.AtomicLong BMP_HOLD =
            new java.util.concurrent.atomic.AtomicLong();
    /** v1.10.410：排队任务开跑前发现已过窗口而丢弃的次数（过载自动卸载）。 */
    private static final java.util.concurrent.atomic.AtomicLong BMP_DROP =
            new java.util.concurrent.atomic.AtomicLong();
    /** v1.10.410：解码池队列峰值。 */
    private static final java.util.concurrent.atomic.AtomicInteger BMP_QPEAK =
            new java.util.concurrent.atomic.AtomicInteger();
    /**
     * v1.10.481（P0-3 / A）：**跨组件共享解码**。
     *
     * <p>`共享帧=` = 命中数 / 查询数（另附表大小与占用）。命中一次 = 这一帧**一次解码都没做**。
     * 组件 → 跨组件稳定身份（`renderTag|素材|画布`，**不含 mtime**，见 {@link FrameShareKey}）。</p>
     */
    private static final java.util.HashMap<Integer, String> SHARE_BASE = new java.util.HashMap<>();

    /** 跨组件稳定的身份：不含 mtime 的那一半 stamp（mtime 只用来让「本组件」重烘，不该参与共享键）。 */
    @Nullable
    static String shareBase(@Nullable WidgetConfig config, @Nullable String stamp) {
        if (config == null || stamp == null) {
            return null;
        }
        int cut = stamp.lastIndexOf('_');
        String wh = (cut > 0 && cut + 1 < stamp.length()) ? stamp.substring(cut + 1) : "";
        return renderTag(config) + "|" + srcId(config) + "|" + wh;
    }

    /** 素材身份（与 {@code contentTag} 里那段字符串逐字一致，只是不含首帧 mtime）。 */
    @NotNull
    private static String srcId(@NotNull WidgetConfig config) {
        return config.getSourceName() + "|" + config.getSourceUri() + "|"
                + config.getFrameCount() + "|" + config.getQuality() + "|"
                + config.getMaxEdge() + "|" + config.getKind();
    }

    /** v1.10.419：直解共享 —— 命中数 / 总解码数 / `asShared()` 返回自身的次数（= 真零拷贝）。 */
    private static final java.util.concurrent.atomic.AtomicLong DIRECT_N =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong DEC_N =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong SAME_N =
            new java.util.concurrent.atomic.AtomicLong();
    /** v1.10.419：直解开关。失败一次就置 false（永久回落），日志里看得见。 */
    private static volatile boolean DIRECT_SHARED = true;
    /**
     * v1.10.352：**`换图=` 的口径修正**。
     *
     * 实测（06:54 日志）：`共享=解码均11ms×452 推449 换图0 滞后449` —— 换图恒 0、滞后恒等于推送数，
     * 但 W10 同时写着 `实测 50.0fps / 帧隔 p50=20ms / 掉帧=0`，画面明显在动。
     * 原因：`fresh` 只认「拿到 exact」这一种情况，而旧逻辑找不到 exact 时
     * 会回退返回表里**最新的一张**（帧号只差 1~2），那其实是**换了图**，却被记成"滞后"。
     * 现在改成看「上屏的 Bitmap 对象有没有变」——那才是用户看到的帧率。
     */
    private static final java.util.HashMap<Integer, Integer> LAST_BMP_ID = new java.util.HashMap<>();

    /**
     * v1.10.344：`换图=` = 真正换了几张不同的图（这才是用户看到的帧率）。
     *
     * <p>v1.10.410 在「共享=解码均×N 推 换图 滞后」之后<b>追加</b>（前缀口径不变，旧脚本照用）：
     * <ul>
     *   <li>`延迟均=`：提交→解完（含排队）。比 `解码均` 大很多 = 解码池在排队。</li>
     *   <li>`保持=`：取不到 exact、也没有「播放头之前 ≤3 帧」的邻帧，于是<b>不推</b>、屏上保持原样
     *       （旧逻辑会推「未来帧」造成帧号回退）。连续 4 拍仍取不到会强制退回 URI，不许冻屏。</li>
     *   <li>`弃=`：排队任务开跑前发现已不在窗口（播放头已跳过）而丢弃——过载时会变大，是自动卸载。</li>
     *   <li>`排队峰=`：解码池队列窗口内峰值。</li>
     *   <li>`环[组件:预读L/环R/回B/空E/败F]`：逐组件。预读 = 本窗口最大提前量；环 = 最多同时持有几帧；
     *       回 = 用邻帧顶替的拍数；空 = 环为空走了 URI 兜底的拍数；败 = 解码失败数。</li>
     * </ul>
     * <b>判据：`解码×` 应 ≈ `推`</b>（v1.10.409 是它的 2 倍，因为每拍无条件重排 +1/+2）；
     * `回`/`空`/`败` 应 ≈ 0。
     */
    @NotNull
    public static String takeSharedStats() {
        long n = BMP_N.getAndSet(0L);
        long ms = BMP_MS.getAndSet(0L);
        long lat = BMP_LAT.getAndSet(0L);
        long push = BMP_PUSH.getAndSet(0L);
        long fresh = BMP_FRESH.getAndSet(0L);
        long hold = BMP_HOLD.getAndSet(0L);
        long drop = BMP_DROP.getAndSet(0L);
        int qpeak = BMP_QPEAK.getAndSet(0);
        if (n <= 0L && push <= 0L && hold <= 0L) {
            return "";
        }
        StringBuilder sb = new StringBuilder(160);
        sb.append("共享=解码均").append(n > 0L ? (ms / n) : 0L).append("ms×").append(n)
                .append(" 推").append(push)
                .append(" 换图").append(fresh).append(" 滞后").append(push - fresh)
                .append(" 延迟均=").append(n > 0L ? (lat / n) : 0L).append("ms")
                .append(" 保持=").append(hold).append(" 弃=").append(drop)
                .append(" 排队峰=").append(qpeak)
                // v1.10.419：直解共享（`零拷贝` 与 `直解` 应都接近解码总数；`解码均` 应降 1.5ms 以上）
                .append(" 直解=").append(DIRECT_N.getAndSet(0L)).append('/').append(DEC_N.getAndSet(0L))
                .append(" 零拷贝=").append(SAME_N.getAndSet(0L))
                .append(FrameShare.takeStat())
                .append(LayerFrames.takeStat())
                .append(DIRECT_SHARED ? "" : "(直解已关)");
        try {
            java.util.List<FrameRing.LaneStat> ls = RING.snapshot(true, SystemClock.elapsedRealtime());
            for (FrameRing.LaneStat l : ls) {
                sb.append(" 环[").append(l.id).append(":预读").append(l.lead)
                        .append("/环").append(l.ringMax)
                        .append("/回").append(l.behind)
                        .append("/空").append(l.empty)
                        .append("/败").append(l.fail).append(']');
            }
        } catch (Throwable ignored) {
        }
        return sb.toString();
    }

    /** v1.10.410：各组件的预读环快照（只读 EWMA，不清计数）—— 给容量行用。 */
    @NotNull
    static java.util.List<FrameRing.LaneStat> ringLanes() {
        return RING.snapshot(false, SystemClock.elapsedRealtime());
    }

    /** v1.10.410：组件被删时释放它的预读环（由 {@link #clearWidget} 调用）。 */
    static void forgetRing(int widgetId) {
        try {
            RING.forget(widgetId);
        } catch (Throwable ignored) {
        }
    }

    /** v1.10.410：一个解码任务（紧迫度由 {@link Pri} 外面那层管）。 */
    private static final class DecodeTask implements Runnable {
        final Context app;
        final int wid;
        final String stamp;
        final int epoch;
        final int idx;
        final long submittedAt;
        /** v1.10.481：跨组件稳定的身份（renderTag|srcId|画布），不含 mtime。 */
        @Nullable
        final String shareBase;
        /** v1.10.481：帧数（键里要它来归一化帧号）。 */
        final int count;

        DecodeTask(Context app, int wid, String stamp, int epoch, int idx, long submittedAt,
                   @Nullable String shareBase, int count) {
            this.app = app;
            this.wid = wid;
            this.stamp = stamp;
            this.epoch = epoch;
            this.idx = idx;
            this.submittedAt = submittedAt;
            this.shareBase = shareBase;
            this.count = count;
        }

        @Override
        public void run() {
            long t0 = SystemClock.elapsedRealtime();
            if (!RING.wanted(wid, epoch, idx)) {
                RING.complete(wid, epoch, idx, null, 0L, 0L, 0L, true, t0);
                BMP_DROP.incrementAndGet();
                return;
            }
            Bitmap store = null;
            long bytes = 0L;
            // v1.10.481（P0-3 / A）：**先查共享帧表**。同素材的另一个组件大概率已经把这一帧解好了
            // （预读窗口重叠，见 FrameShare 的类注释）。命中 = 一次解码都不做，直接把同一张位图
            // 交给自己这一路 —— 像素完全一样，观感零变化。绝不等待、绝不上锁（等待=掉帧）。
            String shareKey = null;
            try {
                File fShare = FrameProvider.fileOf(app, FrameProvider.uriFor(wid, stamp, fileName(idx)));
                if (fShare != null) {
                    shareKey = FrameShareKey.of(shareBase, idx, count, fShare.length(),
                            FrameShare.headHash(fShare));
                }
            } catch (Throwable ignored) {
            }
            File dirNow = null;
            try {
                dirNow = dirOf(app, wid, stamp);
            } catch (Throwable ignored) {
            }
            // v1.10.485：**共享表必须先查**。
            // 484 把分层放在前面，于是两路各自解各自的 JPEG —— 日志里 `分层=301/301` 很漂亮，
            // 但 `共享帧=0/0`、`解码均×301`：A 的收益整个丢掉了（解的次数又回到 N 倍）。
            // 分层帧同样进共享表（合成完就 put），所以顺序必须是：先查表 → 再分层 → 再老路。
            Bitmap shared = FrameShare.get(shareKey);
            if (shared != null) {
                bytes = (long) shared.getWidth() * (long) shared.getHeight() * 4L;
                RING.complete(wid, epoch, idx, shared, 0L, 0L, bytes, false,
                        SystemClock.elapsedRealtime());
                return;
            }
            // v1.10.487：**同一帧只解一次**。两个组件同相位时，这一拍会同时来要同一帧，
            // 而表里谁都还没有 —— 于是各解一次（实测命中率只有 17~30%，理论上限 50%）。
            // 第一个到的当 leader 去解，后来的等它的结果（最多 WAIT_MS；等不到就自己解，绝不掉帧）。
            FrameShare.Flight flight = FrameShare.begin(shareKey);
            if (flight != null && !flight.leader) {
                Bitmap got = FrameShare.await(flight, FrameShare.WAIT_MS);
                if (got != null && !got.isRecycled()) {
                    FrameShare.noteFlightHit();          // 这一帧是等别人解出来的，没重复解码
                    long bytesW = (long) got.getWidth() * (long) got.getHeight() * 4L;
                    RING.complete(wid, epoch, idx, got, 0L, 0L, bytesW, false,
                            SystemClock.elapsedRealtime());
                    return;
                }
                // 等不到：自己解（不发布，避免覆盖 leader 的结果）
            }
            if (LayerFrames.ready(dirNow, idx)) {
                // v1.10.482（B）：**分层帧** —— 解一张 JPEG（q95，无 alpha）+ 一次 DST_IN 把
                // 形状/阴影/边框乘回来。W48 实测 12.4ms → 6.6ms。合成失败/自检不过就落回老路。
                Bitmap layered = LayerFrames.compose(dirNow, idx);
                if (layered == null) {
                    FrameShare.abandon(shareKey, flight);
                }
                if (layered != null) {
                    Bitmap store0 = layered;
                    try {
                        Bitmap sh0 = layered.asShared();
                        if (sh0 != null) {
                            store0 = sh0;
                        }
                    } catch (Throwable ignored) {
                    }
                    long bytes0 = (long) store0.getWidth() * (long) store0.getHeight() * 4L;
                    boolean taken1 = RING.complete(wid, epoch, idx, store0, 0L, 0L, bytes0, false,
                            SystemClock.elapsedRealtime());
                    long t1l = SystemClock.elapsedRealtime();
                    if (taken1) {
                        // 记账口径：分层帧也是「我们这边解出来的」，必须进 解码均（否则日志会显示
                        // 解码均只剩兜底那几帧，看起来像「解码突然变快了」）
                        BMP_MS.addAndGet(t1l - t0);
                        BMP_LAT.addAndGet(t1l - submittedAt);
                        BMP_N.incrementAndGet();
                        FrameShare.put(shareKey, store0);   // 另一路来了直接拿合成好的
                    } else if (!store0.isRecycled()) {
                        FrameShare.put(shareKey, store0);
                    }
                    FrameShare.finish(shareKey, flight, store0);   // 叫醒正在等这一帧的那一路
                    return;
                }
            }
            try {
                Bitmap b = decodeFrameFile(app, FrameProvider.uriFor(wid, stamp, fileName(idx)));
                if (b != null) {
                    Bitmap sh = null;
                    try {
                        sh = b.asShared();
                    } catch (Throwable ignored) {
                    }
                    if (sh == b) {
                        SAME_N.incrementAndGet();   // 像素本来就在共享内存里 → 零拷贝
                    }
                    if (sh != null && sh != b) {
                        // v1.10.410：asShared() 拿到的是**另一张**（344~409 一直靠 GC 回收原图，
                        // 说明它必然不是共享同一块像素，否则早就 use-after-free 了）。
                        // 50fps × 3 组件 ≈ 550MB/s 的 native 垃圾 → 进程级 GC 停顿，就是那个 43ms 尖峰。
                        // 只回收原图；共享位图可能还在 Binder 里，绝不 recycle。
                        b.recycle();
                        store = sh;
                    } else {
                        store = b;
                    }
                    bytes = (long) store.getWidth() * (long) store.getHeight() * 4L;
                }
            } catch (Throwable ignored) {
                store = null;
            }
            if (store == null) {
                FrameShare.abandon(shareKey, flight);
            }
            long t1 = SystemClock.elapsedRealtime();
            boolean taken = RING.complete(wid, epoch, idx, store, t1 - submittedAt, t1 - t0, bytes, false, t1);
            if (store != null) {
                if (taken) {
                    BMP_MS.addAndGet(t1 - t0);
                    BMP_LAT.addAndGet(t1 - submittedAt);
                    BMP_N.incrementAndGet();
                    // v1.10.481：这一帧已经解好了 → 公布给共享表，另一路来了就直接拿
                    FrameShare.put(shareKey, store);
                    FrameShare.finish(shareKey, flight, store);
                } else if (!store.isRecycled()) {
                    // v1.10.481：迟到的结果不再直接扔掉 —— 它可能正好是另一路下一拍要的帧。
                    // 先放进共享表（表满了会按 LRU 淘掉旧的），再交还 GC；绝不 recycle 共享位图。
                    FrameShare.put(shareKey, store);
                    // v1.10.487：迟到也要叫醒等待者（否则他们要空等到超时再自己解一遍）
                    FrameShare.finish(shareKey, flight, store);
                }
            }
        }
    }

    /** 提交一帧解码。{@code dist}=0 时排在最前（这一拍就要的帧）。 */
    private static void submitDecode(@NotNull Context context, int wid, @NotNull String stamp,
                                     @NotNull FrameRing.Pick<Bitmap> p, int i) {
        DecodeTask task = new DecodeTask(context.getApplicationContext(), wid, stamp, p.epoch,
                p.submit[i], SystemClock.elapsedRealtime(), SHARE_BASE.get(Integer.valueOf(wid)),
                p.count);
        try {
            BMP_POOL.execute(new Pri(p.dist[i], BG_SEQ.incrementAndGet(), task));
            int q = BMP_POOL.getQueue().size();
            int old;
            while (q > (old = BMP_QPEAK.get()) && !BMP_QPEAK.compareAndSet(old, q)) {
                // CAS 取峰值
            }
        } catch (Throwable th) {
            // 提交失败：把「在飞」标记撤掉，否则这一帧永远不会再被排
            RING.complete(wid, p.epoch, p.submit[i], null, 0L, 0L, 0L, true, SystemClock.elapsedRealtime());
        }
    }

    // ============================================================ v1.10.351：过渡期「现渲染直推」

    /** v1.10.351：开关（设置里可关，回到 v1.10.350 的「顶帧/跳过」行为）。 */
    private static final String PREF_RENDER = "render_fallback";

    /** 现渲染通道的开关状态。每拍读一次（SharedPreferences 命中是内存里的，不落盘）。 */
    static boolean renderFallbackOn(@Nullable Context context) {
        try {
            return context != null && context.getSharedPreferences(PREFS, 0)
                    .getBoolean(PREF_RENDER, true);
        } catch (Throwable th) {
            return true;
        }
    }

    /** 每个组件最近渲染好的几帧（key 见 {@link #renderKey}，最多 3 张）。 */
    private static final java.util.HashMap<Integer, java.util.LinkedHashMap<String, Bitmap>> R_MAP =
            new java.util.HashMap<>();
    /** 在飞的渲染任务 key（防同一帧被排两次；全局上限 6）。 */
    private static final java.util.HashSet<String> R_INFLIGHT = new java.util.HashSet<>();
    /** 每个组件的「合成签名」，一变就作废内存里的帧（形状/参数/尺寸改了不能还推旧图）。 */
    private static final java.util.HashMap<Integer, Integer> R_SIG = new java.util.HashMap<>();

    /** v1.10.351：现渲染通道的耗时统计（W10 每 3 秒取一次）。 */
    static final class RenderStat {
        int done;
        int fail;
        int push;
        int exact;
        int slow;
        /** v1.10.352：帧源拆分 —— 旧代重做（11+3ms）/ 源帧现渲染（28ms）。 */
        int foreignSrc;
        int renderSrc;
        final java.util.ArrayList<Long> readMs = new java.util.ArrayList<>();
        final java.util.ArrayList<Long> totalMs = new java.util.ArrayList<>();
    }

    private static final java.util.HashMap<Integer, RenderStat> R_STAT = new java.util.HashMap<>();

    @NotNull
    private static RenderStat rstat(int widgetId) {
        RenderStat s = R_STAT.get(Integer.valueOf(widgetId));
        if (s == null) {
            s = new RenderStat();
            R_STAT.put(Integer.valueOf(widgetId), s);
        }
        return s;
    }

    /** v1.10.351：现渲染的缓存键（帧号 + 目标尺寸，签名不进来是因为签名一变整张表会被清空）。 */
    @NotNull
    private static String renderKey(int index, int w, int h) {
        return "R" + index + "@" + w + "x" + h;
    }

    /** 影响合成结果的参数指纹（形状/阴影/边框/旋转/偏移…），只用来判断「这张表还能不能用」。 */
    private static int renderSig(@NotNull WidgetConfig config, int w, int h) {
        return ((w * 31 + h) * 31 + renderTag(config).hashCode()) * 31
                + config.getFrameCount();
    }

    /**
     * v1.10.351：**过渡期主路径** —— 新代这一帧还没烘好时，当场渲染一张原尺寸的直接上屏。
     *
     * <pre>
     * 烘帧 27M px/s ÷ 76.4 万 = 35 帧/秒  &lt;  播放 50 帧/秒
     * ⇒ 新代永远追不上播放头，任何排程参数都改不了（T_swap 是阶跃，不是斜坡）
     * ⇒ 过渡期唯一能保证 50fps 的帧源只剩「现渲染」：读源帧 14ms + 合成 3ms = 17ms，
     *    省掉的是编码那 119ms（27M px/s 的全部来源）
     * </pre>
     *
     * 必须在 {@link #BMP_POOL} 上跑：推帧线程是单线程、20ms 一拍、一拍推 3 个组件，
     * 3 × 17ms 会把节拍直接压到 19fps（比顶帧更糟）。
     *
     * @param size **画布**像素尺寸（与稳态推的磁盘帧同尺寸）。
     *             不能传格子：稳态推的是画布尺寸的帧，现渲染若按格子合成，就会
     *             「过渡期 76.4 万很清 → 烘完切到 24~45 万反而变糊」，用户看到的是倒挂。
     *             画布 ≥ 格子由调用方保证（v1.10.351：1:1 档撤销 45 万上限）。
     */
    static boolean pushRendered(@NotNull Context context, @NotNull AppWidgetManager manager,
                                @NotNull WidgetConfig config, int index, int[] size,
                                boolean fitView) {
        if (!renderFallbackOn(context)) {
            return false;
        }
        if (size == null || size.length < 2 || size[0] <= 0 || size[1] <= 0) {
            return false;
        }
        if (config.getFrameCount() <= 1) {
            return false;                       // 静态图只推一次，不走过渡
        }
        if (android.os.Build.VERSION.SDK_INT < 27) {
            // asShared() 是 API 27 才有的。没有它就只能走 Binder 传像素，76 万 px = 3MB
            // 必然撞 TransactionTooLargeException —— 与其每拍失败一次，不如直接不走这条路。
            return false;
        }
        int widgetId = config.getWidgetId();
        int w = size[0];
        int h = size[1];
        // 签名变了（形状/尺寸/参数）→ 内存里的帧全部作废，别推上一套参数的图
        int sig = renderSig(config, w, h);
        synchronized (R_MAP) {
            Integer old = R_SIG.get(Integer.valueOf(widgetId));
            if (old == null || old.intValue() != sig) {
                java.util.LinkedHashMap<String, Bitmap> dead =
                        R_MAP.remove(Integer.valueOf(widgetId));
                if (dead != null) {
                    dead.clear();
                }
                R_SIG.put(Integer.valueOf(widgetId), Integer.valueOf(sig));
            }
        }
        int count = config.getFrameCount();
        int i = ((index % count) + count) % count;
        // v1.10.352：**先试上一代帧**（解 WebP 11ms + compose 3ms ≈ 15ms，压得进 20ms 一拍）。
        // 只有旧代这一帧不存在（前段作废批次/目录被清）才退回源帧现渲染（28ms，会超拍）。
        File dir = foreignDir(context, config, w, h);
        if (dir != null && !new File(dir, fileName(i)).exists()) {
            // v1.10.353：旧代目录可能已经在 3 秒缓存期内被 prepare() 清掉了
            // （连续拖动两次时，缓存指向的是「两代前」）。作废缓存重算一次，
            // 否则会白白退回源帧现渲染（41ms，必然超拍）。
            synchronized (FOREIGN) {
                FOREIGN.remove(Integer.valueOf(widgetId));
            }
            dir = foreignDir(context, config, w, h);
            if (dir != null && !new File(dir, fileName(i)).exists()) {
                dir = null;
            }
        }
        boolean[] exact = new boolean[]{false};
        Bitmap ready;
        if (dir != null) {
            ready = takeRender(widgetId, "F" + i + "@" + w + "x" + h, exact);
            ensureForeign(context, config, dir, (i + 1) % count, w, h);
            ensureForeign(context, config, dir, (i + 2) % count, w, h);
            if (!hasRender(widgetId, "F" + i + "@" + w + "x" + h)) {
                ensureForeign(context, config, dir, i, w, h);
            }
        } else {
            ready = takeRender(widgetId, renderKey(i, w, h), exact);
            ensureRender(context, config, (i + 1) % count, w, h);
            ensureRender(context, config, (i + 2) % count, w, h);
            if (!hasRender(widgetId, renderKey(i, w, h))) {
                ensureRender(context, config, i, w, h);
            }
        }
        if (ready == null) {
            return false;                       // 这一拍交给老路径兜底（顶一张 / 跳过），最多一两拍
        }
        try {
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    fitView ? R.layout.widget_media_fit : R.layout.widget_media);
            views.setImageViewBitmap(R.id.widget_image, ready);
            views.setOnClickPendingIntent(R.id.widget_image,
                    WidgetRender.INSTANCE.contentIntent(context));
            send(manager, widgetId, views);
            FAILS.remove(Integer.valueOf(widgetId));
            WidgetRender.noteTransport(widgetId, "现渲染");
            noteRender(widgetId);
            synchronized (R_STAT) {
                RenderStat st = rstat(widgetId);
                st.push++;
                if (ready.getWidth() >= w && ready.getHeight() >= h) {
                    st.exact++;
                }
            }
            return true;
        } catch (Throwable th) {
            Diag.warnThrottled(context, "W30", "现渲染直推失败 "
                    + th.getClass().getSimpleName() + " → 回落顶帧");
            return false;
        }
    }

    // ------------------------------ v1.10.352：过渡期改用「上一代帧」当数据源

    /**
     * v1.10.352：**过渡期的数据源从「源帧」换成「上一代已烘好的 WebP」。**
     *
     * v1.10.351 的实测（06:54 日志）：
     * <pre>
     *   现渲染=推51 读28/49ms 合计34/76ms 超拍=149
     *   共享=解码均11ms×452
     * </pre>
     * 读**源帧**要 28ms（1080x1080 解码 + 色彩空间转换 + 和 5 条预热线程抢 CPU），
     * 解**已烘好的 WebP** 只要 11ms。而一拍只有 20ms —— 34ms 永远差半拍，
     * 于是 34%~96% 的拍在「跳过」，用户看到的就是**帧率骤降**。
     *
     * 现在的做法：取旧代同号帧（11ms）→ 裁出它的内容矩形 → 交给
     * {@link FrameComposer#compose} 按新画布重新合成（形状/阴影/边框全部重做，3ms）
     * → asShared 直推。合计 ≈15ms，能压进 20ms。
     *
     * 为什么必须裁内容矩形再 compose，而不是「整帧等比放大」：
     * 旧帧的透明边（letterbox）是按旧画布比例烘进去的，整帧放大后内容中心会偏，
     * 比例差大时（1.367 → 1.011）还会把内容挤成一小块 —— 正是用户反复说的「内缩」。
     * 裁出内容矩形后，内容比例恒等于源素材比例，compose 会按新画布重新算出正确的内容矩形。
     */
    private static final java.util.HashMap<Integer, Object[]> FOREIGN = new java.util.HashMap<>();

    /**
     * 找「上一代」画布目录（3 秒缓存）。
     * 判据：尺寸和当前画布不同、不是 .trash、目录名能解析出 WxH；挑**面积最接近**的那份（最清晰）。
     */
    @Nullable
    private static File foreignDir(@NotNull Context context, @NotNull WidgetConfig config,
                                   int w, int h) {
        long now = SystemClock.elapsedRealtime();
        synchronized (FOREIGN) {
            Object[] c = FOREIGN.get(Integer.valueOf(config.getWidgetId()));
            // v1.10.354：**null 不进缓存**。353 这里把「当时没找到旧代」也缓存了 3 秒，
            // 而画布刚变的那一瞬旧代目录还没稳定 → 第一次算出 null → 后面 3 秒全部返回 null
            // → 整段过渡期都退回读源帧（21ms x 50 次 = 白烧），日志里就是
            // `旧代=0 源帧=150 合计26/40ms 超拍=142`。
            if (c != null && c[1] != null && ((Long) c[0]).longValue() > now - 3000L) {
                return (File) c[1];
            }
        }
        File[] subs = widgetRoot(context, config.getWidgetId()).listFiles();
        File best = null;
        long bestGap = Long.MAX_VALUE;
        if (subs != null) {
            long want = (long) w * h;
            for (File sub : subs) {
                if (sub == null || !sub.isDirectory()) {
                    continue;
                }
                String n = sub.getName();
                if (n.contains(".trash.")) {
                    continue;
                }
                int[] sz = parseSize(n);
                if (sz == null) {
                    continue;
                }
                if (sz[0] == w && sz[1] == h) {
                    continue;                       // 就是当前代，不是「上一代」
                }
                long gap = Math.abs((long) sz[0] * sz[1] - want);
                if (gap < bestGap) {
                    bestGap = gap;
                    best = sub;
                }
            }
        }
        synchronized (FOREIGN) {
            FOREIGN.put(Integer.valueOf(config.getWidgetId()),
                    new Object[]{Long.valueOf(now), best});
        }
        return best;
    }

    /** 把旧代帧解出来（不走 ContentProvider，直接读文件）。 */
    @Nullable
    private static Bitmap decodeFileQuiet(@NotNull File f) {
        try {
            if (f == null || !f.isFile() || f.length() <= 0L) {
                return null;
            }
            return android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath());
        } catch (Throwable th) {
            return null;
        }
    }

    /**
     * v1.10.352：后台把「旧代同号帧」重做成新画布尺寸的一帧。
     *
     * 步骤：解旧帧(11ms) → 裁内容矩形 → compose 到新画布(3ms) → asShared。
     * 裁出来的内容比例 = 源素材比例，所以 compose 算出的内容矩形与「真源帧直烘」完全一致，
     * 过渡期与稳态之间**只有分辨率差，没有版式差**。
     */
    static void scheduleForeign(@NotNull Context context, @NotNull WidgetConfig config,
                                @NotNull File dir, int index, int w, int h) {
        final int widgetId = config.getWidgetId();
        final String key = "F" + index + "@" + w + "x" + h;
        synchronized (R_INFLIGHT) {
            if (R_INFLIGHT.contains(key) || R_INFLIGHT.size() >= 12) {
                return;
            }
            R_INFLIGHT.add(key);
        }
        final Context app = context.getApplicationContext() != null
                ? context.getApplicationContext() : context;
        final WidgetConfig cfg = config;
        final int[] oldSize = parseSize(dir.getName());
        final File file = new File(dir, fileName(index));
        final int idx = index;
        try {
            watchful(1, () -> {
                long t0 = SystemClock.elapsedRealtime();
                Bitmap old = null;
                Bitmap content = null;
                Bitmap out = null;
                long readMs = 0L;
                try {
                    old = decodeFileQuiet(file);
                    readMs = SystemClock.elapsedRealtime() - t0;
                    if (old != null && oldSize != null) {
                        // 旧帧的内容矩形（旧画布内），比例 = 源素材比例
                        android.graphics.RectF src = FrameComposer.INSTANCE.contentRect(
                                cfg.getFrameWidth(), cfg.getFrameHeight(),
                                oldSize[0], oldSize[1],
                                WidgetRender.effLayout(cfg, new int[]{oldSize[0], oldSize[1]}));
                        int l = Math.max(0, Math.round(src.left));
                        int t = Math.max(0, Math.round(src.top));
                        int rw = Math.min(old.getWidth() - l, Math.round(src.width()));
                        int rh = Math.min(old.getHeight() - t, Math.round(src.height()));
                        if (rw > 0 && rh > 0) {
                            content = Bitmap.createBitmap(old, l, t, rw, rh);
                        }
                    }
                    if (old != null && oldSize != null) {
                        // v1.10.354：**不再调 compose，改成「裁形状 + 一次 drawBitmap」。**
                        //
                        // 353 的实测（07:20 日志）：`旧代=147 读10/28ms 合计21/45ms 超拍=98` ——
                        // 阴影传 0 之后 compose 只降到 ~8ms，合计 21ms 仍然压不进 20ms 一拍。
                        // 而 compose 里对我们**只有一条是必需的**：把内容缩放到新画布。
                        // 形状可以用 clipPath 一次解决（1ms），阴影直接沿用旧帧里的那份
                        // （跟着内容一起缩放，天然只有一层），边框同理。
                        // 于是成本从「解码13 + compose8」变成「解码13 + 绘制4 + clip1」≈ 18ms。
                        android.graphics.RectF src = FrameComposer.INSTANCE.contentRect(
                                cfg.getFrameWidth(), cfg.getFrameHeight(),
                                oldSize[0], oldSize[1],
                                WidgetRender.effLayout(cfg, new int[]{oldSize[0], oldSize[1]}));
                        android.graphics.RectF dst = FrameComposer.INSTANCE.contentRect(
                                cfg.getFrameWidth(), cfg.getFrameHeight(), w, h,
                                WidgetRender.effLayout(cfg, new int[]{w, h}));
                        out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                        android.graphics.Canvas cv = new android.graphics.Canvas(out);
                        try {
                            android.graphics.Path clip = ShapeFactory.INSTANCE.path(
                                    cfg.getShape(), w, h);
                            float rot = cfg.getShapeRotation();
                            if (rot != 0f) {
                                android.graphics.Matrix m = new android.graphics.Matrix();
                                m.setRotate(rot, w / 2f, h / 2f);
                                clip.transform(m);
                            }
                            cv.clipPath(clip);
                        } catch (Throwable ignored) {
                        }
                        android.graphics.Paint paint = new android.graphics.Paint();
                        paint.setFilterBitmap(true);
                        paint.setDither(false);
                        // 源矩形必须是整数 Rect（Canvas 只有 Rect->RectF 那个重载）
                        android.graphics.Rect srcI = new android.graphics.Rect();
                        src.round(srcI);
                        if (srcI.left < 0) {
                            srcI.left = 0;
                        }
                        if (srcI.top < 0) {
                            srcI.top = 0;
                        }
                        if (srcI.right > old.getWidth()) {
                            srcI.right = old.getWidth();
                        }
                        if (srcI.bottom > old.getHeight()) {
                            srcI.bottom = old.getHeight();
                        }
                        if (srcI.width() > 0 && srcI.height() > 0) {
                            cv.drawBitmap(old, srcI, dst, paint);
                        }
                        content = null;                 // 不再需要裁出来的中间位图
                    }
                } catch (Throwable ignored) {
                } finally {
                    if (old != null && old != content && !old.isRecycled()) {
                        old.recycle();
                    }
                }
                if (out != null) {
                    Bitmap share = null;
                    try {
                        share = out.asShared();
                    } catch (Throwable ignored) {
                    }
                    Bitmap store = share != null ? share : out;
                    synchronized (R_MAP) {
                        java.util.LinkedHashMap<String, Bitmap> m =
                                R_MAP.get(Integer.valueOf(widgetId));
                        if (m == null) {
                            m = new java.util.LinkedHashMap<String, Bitmap>() {
                                @Override
                                protected boolean removeEldestEntry(
                                        java.util.Map.Entry<String, Bitmap> eldest) {
                                    return size() > 5;
                                }
                            };
                            R_MAP.put(Integer.valueOf(widgetId), m);
                        }
                        m.put(key, store);
                    }
                    long total = SystemClock.elapsedRealtime() - t0;
                    synchronized (R_STAT) {
                        RenderStat st = rstat(widgetId);
                        st.done++;
                        st.foreignSrc++;
                        st.readMs.add(Long.valueOf(readMs));
                        st.totalMs.add(Long.valueOf(total));
                        if (total >= 20L) {
                            st.slow++;
                        }
                        if (st.readMs.size() > 240) {
                            st.readMs.remove(0);
                            st.totalMs.remove(0);
                        }
                    }
                } else {
                    synchronized (R_STAT) {
                        rstat(widgetId).fail++;
                    }
                }
                synchronized (R_INFLIGHT) {
                    R_INFLIGHT.remove(key);
                }
            });
        } catch (Throwable th) {
            synchronized (R_INFLIGHT) {
                R_INFLIGHT.remove(key);
            }
        }
    }

    /** v1.10.352：这一帧（来自旧代）既不在表里也不在飞 → 排一次。 */
    private static void ensureForeign(@NotNull Context context, @NotNull WidgetConfig config,
                                      @NotNull File dir, int index, int w, int h) {
        if (hasRender(config.getWidgetId(), "F" + index + "@" + w + "x" + h)) {
            return;
        }
        scheduleForeign(context, config, dir, index, w, h);
    }

    /** v1.10.351：这一帧既不在表里、也不在飞 → 排一次（流水线补位用）。 */
    private static void ensureRender(@NotNull Context context, @NotNull WidgetConfig config,
                                     int index, int w, int h) {
        if (hasRender(config.getWidgetId(), renderKey(index, w, h))) {
            return;
        }
        scheduleRender(context, config, index, w, h);
    }

    /**
     * v1.10.353：取一帧过渡位图。**exact 没有就给最新一张**（`takeReady` 的同一套策略）。
     *
     * 351/352 只返回 exact，于是「合成 26ms > 一拍 20ms」时下一拍必然拿不到 → `跳过`，
     * 用户看到的就是「画面停一下」。而重复一张旧图最多让有效帧率掉到 ~38fps，
     * **画面还是连续的**——这比冻住好得多（共享位图通道一直是这么干的，实测 50fps 稳）。
     *
     * @param exact 出参：返回的是不是正好那一帧（用来算「换图」）
     */
    @Nullable
    private static Bitmap takeRender(int widgetId, @NotNull String want, boolean[] exact) {
        synchronized (R_MAP) {
            java.util.LinkedHashMap<String, Bitmap> m = R_MAP.get(Integer.valueOf(widgetId));
            if (m == null || m.isEmpty()) {
                return null;
            }
            Bitmap b = m.get(want);
            if (b != null && !b.isRecycled()) {
                if (exact != null) {
                    exact[0] = true;
                }
                return b;
            }
            // v1.10.353：**回退只能往回找，不许往前跳**。
            // 预读的 i+1 / i+2 往往比 i 先解好，如果按「最后插入的那张」回退，
            // 画面会先跳到未来再退回来 —— 那本身就是一眼可见的抖动。
            int wantNo = frameNo(want);
            Bitmap best = null;
            int bestNo = Integer.MIN_VALUE;
            for (java.util.Map.Entry<String, Bitmap> en : m.entrySet()) {
                Bitmap x = en.getValue();
                if (x == null || x.isRecycled()) {
                    continue;
                }
                int no = frameNo(en.getKey());
                if (no > wantNo) {
                    continue;                   // 未来的帧不要
                }
                if (no > bestNo) {
                    bestNo = no;
                    best = x;
                }
            }
            if (exact != null) {
                exact[0] = false;
            }
            return best;
        }
    }

    /** 从 `F123@879x869` / `R123@879x869` 里取帧号；解析不出来返回 -1。 */
    private static int frameNo(@NotNull String key) {
        int i = 1;
        int n = 0;
        boolean any = false;
        while (i < key.length() && key.charAt(i) >= '0' && key.charAt(i) <= '9') {
            n = n * 10 + (key.charAt(i) - '0');
            any = true;
            i++;
        }
        return any ? n : -1;
    }

    /** 只判断有没有，不取（补位用）。 */
    private static boolean hasRender(int widgetId, @NotNull String want) {
        synchronized (R_MAP) {
            java.util.LinkedHashMap<String, Bitmap> m = R_MAP.get(Integer.valueOf(widgetId));
            return m != null && m.get(want) != null;
        }
    }

    /** v1.10.351：后台渲染一帧（读源帧 → 合成到 w×h → asShared）。 */
    static void scheduleRender(@NotNull Context context, @NotNull WidgetConfig config,
                               int index, int w, int h) {
        final int widgetId = config.getWidgetId();
        final String key = renderKey(index, w, h);
        synchronized (R_INFLIGHT) {
            // 上限 12：3 个组件 × 每组件 3 帧预读 = 9，留 3 张余量。
            // 上限 6 会把第 3 个组件的预读全拒掉，退化成「每拍现合一张」→ 跟不上就掉帧。
            if (R_INFLIGHT.contains(key) || R_INFLIGHT.size() >= 12) {
                return;
            }
            R_INFLIGHT.add(key);
        }
        final Context app = context.getApplicationContext() != null
                ? context.getApplicationContext() : context;
        final WidgetConfig cfg = config;
        final int idx = index;
        try {
            watchful(1, () -> {
                long t0 = SystemClock.elapsedRealtime();
                Bitmap src = null;
                Bitmap out = null;
                long readMs = 0L;
                try {
                    src = WidgetStore.INSTANCE.loadFrame(app, cfg, idx, w, h);
                    readMs = SystemClock.elapsedRealtime() - t0;
                    if (src != null) {
                        // 与预热、render() 同一个判据（v1.10.278）：按「真正要用的布局」合成，
                        // 否则预热按 FIT 烘一批、推的时候按 FILL 画，桌面会出现两种卡片形状。
                        out = FrameComposer.INSTANCE.compose(src, w, h,
                                WidgetRender.effLayout(cfg, new int[]{w, h}),
                                cfg.getShape(), cfg.getShapeAlpha(), cfg.getBorder(),
                                cfg.getScalePercent(), cfg.getRotation(), cfg.getShapeRotation(),
                                cfg.getFitContent(), cfg.getShadowPercent(), cfg.getShadowCurve(),
                                cfg.getOffsetXPercent(), cfg.getOffsetYPercent(),
                                cfg.getContentScalePercent(), cfg.getMaskOffsetXPercent(),
                                cfg.getMaskOffsetYPercent());
                    }
                } catch (Throwable ignored) {
                } finally {
                    if (src != null && src != out && !src.isRecycled()) {
                        src.recycle();
                    }
                }
                if (out != null) {
                    Bitmap share = null;
                    try {
                        share = out.asShared();     // → 共享内存，Binder 只传一个 fd
                    } catch (Throwable ignored) {
                    }
                    Bitmap store = share != null ? share : out;
                    synchronized (R_MAP) {
                        java.util.LinkedHashMap<String, Bitmap> m =
                                R_MAP.get(Integer.valueOf(widgetId));
                        if (m == null) {
                            m = new java.util.LinkedHashMap<String, Bitmap>() {
                                @Override
                                protected boolean removeEldestEntry(
                                        java.util.Map.Entry<String, Bitmap> eldest) {
                                    return size() > 5;
                                }
                            };
                            R_MAP.put(Integer.valueOf(widgetId), m);
                        }
                        m.put(key, store);
                    }
                    long total = SystemClock.elapsedRealtime() - t0;
                    synchronized (R_STAT) {
                        RenderStat st = rstat(widgetId);
                        st.done++;
                        st.renderSrc++;
                        st.readMs.add(Long.valueOf(readMs));
                        st.totalMs.add(Long.valueOf(total));
                        if (total >= 20L) {
                            st.slow++;              // 超过一拍预算：这一拍一定迟到
                        }
                        if (st.readMs.size() > 240) {
                            st.readMs.remove(0);
                            st.totalMs.remove(0);
                        }
                    }
                } else {
                    synchronized (R_STAT) {
                        rstat(widgetId).fail++;
                    }
                }
                synchronized (R_INFLIGHT) {
                    R_INFLIGHT.remove(key);
                }
            });
        } catch (Throwable th) {
            synchronized (R_INFLIGHT) {
                R_INFLIGHT.remove(key);
            }
        }
    }

    /** v1.10.351：现渲染通道统计（W10 每 3 秒取一次，读后清样本）。 */
    @NotNull
    public static String takeRenderStats(int widgetId) {
        RenderStat st;
        synchronized (R_STAT) {
            st = R_STAT.remove(Integer.valueOf(widgetId));
        }
        if (st == null || (st.done <= 0 && st.push <= 0)) {
            return "";
        }
        return " 现渲染=推" + st.push + " 尺寸对" + st.exact
                + " 旧代=" + st.foreignSrc + " 源帧=" + st.renderSrc
                + " 读" + pct(st.readMs, 50) + "/" + pct(st.readMs, 95) + "ms"
                + " 合计" + pct(st.totalMs, 50) + "/" + pct(st.totalMs, 95) + "ms"
                + " 超拍=" + st.slow + " 失败=" + st.fail;
    }

    /** 样本的 p 分位（样本为空返回 0）。 */
    private static long pct(@NotNull java.util.ArrayList<Long> xs, int p) {
        if (xs.isEmpty()) {
            return 0L;
        }
        java.util.ArrayList<Long> copy = new java.util.ArrayList<>(xs);
        java.util.Collections.sort(copy);
        int i = (int) Math.round(copy.size() * p / 100.0);
        if (i <= 0) {
            i = 1;
        }
        if (i > copy.size()) {
            i = copy.size();
        }
        return copy.get(i - 1).longValue();
    }

    /** v1.10.344：从 content URI 取画布指纹（/w<id>/<stamp>/<文件名>）。 */
    @Nullable
    static String stampOfUri(@NotNull Uri uri) {
        try {
            java.util.List<String> seg = uri.getPathSegments();
            return (seg != null && seg.size() == 3) ? seg.get(1) : null;
        } catch (Throwable th) {
            return null;
        }
    }

    /** v1.10.344：从 content URI 取帧号（文件名 `00123.webp` → 123），解析不出来返回 -1。 */
    static int indexOfUri(@NotNull Uri uri) {
        try {
            String name = uri.getLastPathSegment();
            if (name == null) {
                return -1;
            }
            int dot = name.indexOf('.');
            return Integer.parseInt(dot > 0 ? name.substring(0, dot) : name);
        } catch (Throwable th) {
            return -1;
        }
    }

    /** v1.10.342：把一帧文件解成位图（和桌面解的是同一份 WebP）。 */
    @Nullable
    /**
     * v1.10.419：**直解到共享内存**（省掉 {@code asShared()} 的第二次 3.7MB 拷贝 + 分配）。
     *
     * <p>原来每帧：BitmapFactory 解出 3.7MB 位图 → {@code asShared()} 再拷 3.7MB → 原图 recycle。
     * 用 {@link android.graphics.ImageDecoder} + {@code ALLOCATOR_SHARED_MEMORY}（API 30+）
     * 可以让解码器**直接把像素落在共享内存上**，此时 {@code asShared()} 退化成返回自身，
     * 拷贝与分配全省。外部两家复核独立算出的收益都是 3~4ms/帧（14.5 → 11ms 左右），
     * 正好是 N=5 缺的那一段。
     *
     * <p>三条退路：SDK &lt; 30 / 解码抛异常 / 解出来不是 ARGB_8888（或不是"已经共享"的），
     * 一律回落到原来的 {@link android.graphics.BitmapFactory}，行为与 v415 完全一致。
     * 失败一次就永久关闭直解 —— 不在每帧上反复试错。
     */
    static Bitmap decodeFrameFile(@NotNull Context context, @NotNull Uri uri) {
        return decodeFrameFile(FrameProvider.fileOf(context, uri));
    }

    /** v1.10.481：已经拿到 File 的版本（共享键要用同一个 File 算签名，别开两遍）。 */
    static Bitmap decodeFrameFile(@Nullable File f) {
        if (f == null || !f.isFile() || f.length() <= 0L) {
            return null;
        }
        DEC_N.incrementAndGet();
        Bitmap direct = decodeShared(f);
        if (direct != null) {
            DIRECT_N.incrementAndGet();
            return direct;
        }
        try {
            Bitmap b = android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath());
            if (b == null) {
                return null;
            }
            return b;
        } catch (Throwable th) {
            return null;
        }
    }

    /** 直解到共享内存；不可用返回 null（调用方回落 BitmapFactory）。 */
    @Nullable
    private static Bitmap decodeShared(@NotNull File f) {
        if (!DIRECT_SHARED || android.os.Build.VERSION.SDK_INT < 30) {
            return null;
        }
        try {
            android.graphics.ImageDecoder.Source src =
                    android.graphics.ImageDecoder.createSource(f);
            Bitmap b = android.graphics.ImageDecoder.decodeBitmap(src,
                    (decoder, info, source) ->
                            decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SHARED_MEMORY));
            if (b == null || b.getConfig() != Bitmap.Config.ARGB_8888) {
                return null;
            }
            return b;
        } catch (Throwable th) {
            DIRECT_SHARED = false;   // 这台机器/这个格式不行 → 永久回落，不再每帧试
            return null;
        }
    }

    /** 已授权过的 URI 直接推（零合成、零编码）。 */
    public static boolean pushCached(@NotNull Context context, @NotNull AppWidgetManager manager,
                                     @NotNull WidgetConfig config, @NotNull Uri uri, boolean fitView) {
        return pushCached(context, manager, config, uri, fitView, null);
    }

    /** v1.10.344：带格子尺寸的版本 —— 大格子走「共享位图」，小格子保持 URI。 */
    public static boolean pushCached(@NotNull Context context, @NotNull AppWidgetManager manager,
                                     @NotNull WidgetConfig config, @NotNull Uri uri, boolean fitView,
                                     @Nullable int[] box) {
        int widgetId = config.getWidgetId();
        // v1.10.193：fitView 也算进「同一张图」的判据 —— 换布局时 uri 可能没变，
        // 但窗口布局（fitCenter / centerCrop）必须跟着换，不能被去重吃掉。
        String key = widgetId + "|" + fitView + "|" + uri;
        synchronized (LAST) {
            // v1.10.120：「顶帧」时如果屏幕上已经就是这一张，就不必再推一遍 ——
            // 既省一次 binder，也省桌面一次解码（顶帧期间这类重复最多）。
            if (key.equals(LAST.get(Integer.valueOf(widgetId)))) {
                return true;
            }
        }
        // v1.10.411：判据从「格子 ≥45 万 px」改成「桌面每帧要解多少像素」—— 见 wantShared 的注释
        if (wantShared(context, config, box)) {
            // 画布指纹直接从 URI 里取（URI = /w<id>/<stamp>/<帧文件名>），不用再算一遍
            String stampNow = stampOfUri(uri);
            // v1.10.481：把「跨组件稳定身份」记在这一路身上（解码任务要用它算共享键）
            try {
                SHARE_BASE.put(Integer.valueOf(widgetId), shareBase(config, stampNow));
            } catch (Throwable ignored) {
            }
            int idx = indexOfUri(uri);
            int count = config.getFrameCount();
            FrameRing.Pick<Bitmap> pick = null;
            if (stampNow != null && idx >= 0 && count > 0) {
                // v1.10.410：预读环。以「这一拍要的帧」为锚，窗口 [want, want+lead] 里只补
                // 既不在手、也不在飞的帧（345 的「want 在手就无条件再排 +1、+2」= 每帧解两次，
                // 解码池一半算力白烧；这就是 §18 约束②吃水线的真身）。
                double intervalMs = 1000.0 / Math.max(config.getFps(), 1);
                pick = RING.step(widgetId, stampNow, count, idx % count, intervalMs,
                        SystemClock.elapsedRealtime());
                for (int i = 0; i < pick.nSubmit; i++) {
                    submitDecode(context, widgetId, stampNow, pick, i);
                }
            }
            Bitmap ready = pick != null ? pick.frame : null;
            if (ready == null && pick != null && pick.kind == FrameRing.HOLD
                    && pick.holdStreak <= FrameRing.HOLD_LIMIT) {
                // 环里有帧，但都在播放头「之后」或太旧：这一拍不推（屏上保持上一张），也不退回 URI
                //（那会让桌面白解一遍 92 万像素）。`保持=` 计数。
                // 连续超过 HOLD_LIMIT 拍就不再保持、落下去走 URI —— 宁可贵一帧，不许冻屏。
                BMP_HOLD.incrementAndGet();
                WidgetRender.noteTransport(widgetId, "位图共享");
                return true;
            }
            if (ready != null) {
                try {
                    // v1.10.491：多格组件 —— 一张位图设进 6 个格子，**每拍只有一次 apply**
                    //（6 个独立组件是每拍 6 次；这是外部复核给出的唯一有数学依据的杠杆）
                    RemoteViews views = MultiCell.isMulti(context, widgetId)
                            ? MultiCell.views(context, ready)
                            : new RemoteViews(context.getPackageName(),
                            fitView ? R.layout.widget_media_fit : R.layout.widget_media);
                    if (!MultiCell.isMulti(context, widgetId)) {
                        views.setImageViewBitmap(R.id.widget_image, ready);
                    }
                    views.setOnClickPendingIntent(R.id.widget_image,
                            WidgetRender.INSTANCE.contentIntent(context));
                    send(manager, widgetId, views);
                    FAILS.remove(Integer.valueOf(widgetId));
                    WidgetRender.noteTransport(widgetId, "位图共享");
                    BMP_PUSH.incrementAndGet();
                    int idHash = System.identityHashCode(ready);
                    boolean changed;
                    synchronized (LAST_BMP_ID) {
                        Integer prev = LAST_BMP_ID.put(Integer.valueOf(widgetId),
                                Integer.valueOf(idHash));
                        changed = prev == null || prev.intValue() != idHash;
                    }
                    if (changed) {
                        BMP_FRESH.incrementAndGet();
                    }
                    return true;
                } catch (Throwable th) {
                    Diag.warnThrottled(context, "W30", "共享位图推送失败 "
                            + th.getClass().getSimpleName() + " → 回落 URI");
                }
            }
        }
        try {
            String launcher = launcherPackage(context);
            if (launcher == null || !grantOnce(context, launcher, uri)) {
                return false;
            }
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    fitView ? R.layout.widget_media_fit : R.layout.widget_media);
            views.setImageViewUri(R.id.widget_image, uri);
            views.setOnClickPendingIntent(R.id.widget_image, WidgetRender.INSTANCE.contentIntent(context));
            send(manager, widgetId, views);
            FAILS.remove(Integer.valueOf(widgetId));
            WidgetRender.noteTransport(widgetId, "URI");   // v1.10.344：事实由这里记，调用方不许再覆盖
            synchronized (LAST) {
                LAST.put(Integer.valueOf(widgetId), key);
            }
            return true;
        } catch (Throwable th) {
            return fail(context, widgetId, "缓存推送失败");
        }
    }

    /**
     * 把这一帧落成文件并推给桌面。
     *
     * @return true = 已用 URI 方式推上去了；false = 没成，调用方应回退到位图推送
     */
    public static boolean push(@NotNull Context context, @NotNull AppWidgetManager manager,
                               @NotNull WidgetConfig config, int index, @NotNull Bitmap bitmap,
                               boolean fitView) {
        int widgetId = config.getWidgetId();
        if (bitmap.isRecycled()) {
            return false;
        }
        if (!writeEncoded(context, config, index, bitmap, bitmap.getWidth(), bitmap.getHeight())) {
            return fail(context, widgetId, "写盘失败");
        }
        String launcher = launcherPackage(context);
        if (launcher == null) {
            return fail(context, widgetId, "取不到桌面包名");
        }
        Uri uri = FrameProvider.uriFor(widgetId,
                stamp(context, config, bitmap.getWidth(), bitmap.getHeight()), fileName(index));
        if (!grantOnce(context, launcher, uri)) {
            return fail(context, widgetId, "授权失败");
        }
        try {
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    fitView ? R.layout.widget_media_fit : R.layout.widget_media);
            views.setImageViewUri(R.id.widget_image, uri);
            views.setOnClickPendingIntent(R.id.widget_image, WidgetRender.INSTANCE.contentIntent(context));
            send(manager, widgetId, views);
            FAILS.remove(Integer.valueOf(widgetId));
            return true;
        } catch (Throwable th) {
            return fail(context, widgetId, "updateAppWidget 失败 " + th.getClass().getSimpleName());
        }
    }

    // ---------------------------------------------------------------- 权限

    /**
     * v1.10.119：同一个 URI 只授权一次。
     * 循环播第二圈时 300 帧都已在集合里 → 每帧省掉一次 binder 授权。
     */
    private static boolean grantOnce(@NotNull Context context, @NotNull String launcher,
                                     @NotNull Uri uri) {
        String key = launcher + "|" + uri;
        synchronized (GRANTED) {
            if (GRANTED.contains(key)) {
                return true;
            }
        }
        try {
            context.grantUriPermission(launcher, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Throwable th) {
            return false;
        }
        synchronized (GRANTED) {
            if (GRANTED.size() > 8192) {
                GRANTED.clear();   // 兜底：极端情况下不留无限增长的集合
            }
            GRANTED.add(key);
        }
        return true;
    }

    private static boolean fail(Context context, int widgetId, String why) {
        LAUNCHER = null;   // 下一次重新解析桌面包名（换桌面/包名变化时能自愈）
        Integer old = FAILS.get(Integer.valueOf(widgetId));
        int n = (old == null ? 0 : old.intValue()) + 1;
        FAILS.put(Integer.valueOf(widgetId), Integer.valueOf(n));
        Diag.warnThrottled(context, "W30", "URI 推送失败(" + why + ") 组件=" + widgetId
                + " 连续=" + n);
        if (n >= 3) {
            setEnabled(context, false);
            Diag.warn(context, "W30", "URI 模式连续 3 次失败，已自动关闭并回到位图推送");
            // v1.10.251：**静态图 1:1 高清也是靠 URI 走的**。URI 这条路连续失败还硬撑的话，
            // 每拍都要先烘一张 148 万 px 的大图、再撞 Binder 失败、再被 retrySmall 压到 15 万
            // —— 比原来的 22 万还糊。所以这里一起关掉，画布立刻回到 22 万（W28 会再打一条）。
            if (WidgetRender.hiResStaticPref(context)) {
                WidgetRender.setHiResStatic(context, false);
                Diag.warn(context, "W30", "静态图 1:1 高清也一并关闭（画布回到 22 万 px，清晰度按位图上限）");
            }
        }
        return false;
    }

    /** v1.10.119：桌面包名缓存 —— `resolveActivity(CATEGORY_HOME)` 是 PackageManager 查询，
     *  一帧查一次在 60 帧/秒时是纯浪费（命中缓存后单帧只剩「授权查表 + 一个 binder 推送」）。 */
    @Nullable
    private static volatile String LAUNCHER;

    @Nullable
    private static String launcherPackage(@NotNull Context context) {
        String cached = LAUNCHER;
        if (cached != null) {
            return cached;
        }
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo info = context.getPackageManager().resolveActivity(home, 0);
            if (info != null && info.activityInfo != null) {
                LAUNCHER = info.activityInfo.packageName;
                return LAUNCHER;
            }
        } catch (Throwable th) {
            return null;
        }
        return null;
    }

    // ---------------------------------------------------------------- 工具

    /**
     * v1.10.263：**清掉已经不存在的组件的推帧目录**。
     *
     * 实测（22:57 那条 W33）：`磁盘帧=123MB` —— 用户反复建/删组件、反复拖尺寸，
     * 每个 `w<id>/` 下会留最后那一版画布，删组件时如果服务没跑（onDeleted 没到）
     * 这些目录就成了孤儿，只涨不降。
     *
     * @return {删掉的目录数, 回收的字节数}
     */
    @NotNull
    public static long[] pruneOrphans(@NotNull Context context, @NotNull java.util.Set<Integer> alive) {
        long dirs = 0L;
        long bytes = 0L;
        try {
            File root = new File(context.getFilesDir(), PUSH_DIR);
            File[] kids = root.listFiles();
            if (kids == null) {
                return new long[]{0L, 0L};
            }
            for (File kid : kids) {
                if (kid == null || !kid.isDirectory()) {
                    continue;
                }
                String name = kid.getName();
                if (name == null || !name.startsWith("w")) {
                    continue;
                }
                int id;
                try {
                    id = Integer.parseInt(name.substring(1));
                } catch (NumberFormatException e) {
                    continue;
                }
                if (alive.contains(Integer.valueOf(id))) {
                    continue;
                }
                bytes += sizeOf(kid);
                deleteTree(kid);
                dirs++;
            }
        } catch (Throwable ignored) {
        }
        return new long[]{dirs, bytes};
    }

    private static long sizeOf(@NotNull File dir) {
        long total = 0L;
        File[] files = dir.listFiles();
        if (files == null) {
            return 0L;
        }
        for (File f : files) {
            if (f == null) {
                continue;
            }
            total += f.isDirectory() ? sizeOf(f) : f.length();
        }
        return total;
    }

    private static void deleteTree(@Nullable File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteTree(child);
            }
        }
        file.delete();
    }
}
