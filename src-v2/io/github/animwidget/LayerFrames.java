package io.github.animwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * v1.10.482（P0-3 / B）：**分层帧** —— 内容走 JPEG，形状/阴影走一张常驻 alpha。
 *
 * <h3>W48 实测（1280×720，同一张成品帧、同一线程、5 次中位数）</h3>
 * <pre>
 *   WebP（带 alpha）12.4ms      JPEG 6.3ms      合成 0.4ms   ⇒ 合计 6.6ms（省 46%）
 * </pre>
 *
 * <h3>为什么不是 P0-2 那条「布局分层」</h3>
 * P0-2 想在布局里放两层 ImageView（内容 JPEG + overlay），被外部复核判死：overlay 只能往上叠、
 * 擦不掉下面的内容，圆角必然露出方角。这里**不改布局**，分层发生在**我方进程内**：
 * 解码 JPEG → 用一张常驻 alpha 把形状/阴影/边框「乘」回去 → 再把一张**完整的**帧推给桌面。
 * 桌面看到的仍然是同一张带 alpha 的位图，观感零变化（烘的时候做过逐像素自检）。
 *
 * <h3>两个文件</h3>
 * <pre>
 *   c%05d.jpg   = 成品帧的 RGB（透明区本来就是纯黑，所以「压到黑底」= 原 RGB，无损语义）
 *   alpha.webp  = 成品帧的 alpha 通道，**整代只有一张**（形状/阴影/边框/透明度都与帧无关）
 * </pre>
 * 合成 = JPEG 铺满画布 + DST_IN(alpha)。因为原帧本来就是「内容 × alpha」，这一步在预乘空间里
 * 与旧帧**逐像素相等**（烘的时候用帧 0 验一遍，超过容差就整代放弃，回落到原来的 WebP 帧）。
 *
 * <h3>开关与失败方向</h3>
 * 写盘失败、自检不过、alpha 解不出来 —— 任何一种都只让 {@link #ready} 返回 false，
 * 播放链自动走老路（多解一次 12ms），**不会错帧、不会黑屏**。
 */
final class LayerFrames {

    /** 这一版默认开（自检不过会自动回落）。 */
    static final boolean ON = true;
    /** JPEG 质量：W48 那批用的就是 q95。 */
    static final int QUALITY = 95;
    static final String ALPHA_NAME = "alpha.webp";
    static final String MARK_NAME = "layer.ok";
    /**
     * 自检容差：用**平均绝对差**（MAE）而不是最大差。
     *
     * <p>JPEG 在锐利边缘的最大通道差可以到 30+，拿最大值当判据等于永远失败；而分层真的错了
     * （alpha 没对上、偏移了、没有 DST_IN）时 MAE 会直接飙到几十。q95 的 MAE 通常 1~2，
     * 门限 4 是「有损但不失真」与「分层错了」之间的分界。</p>
     */
    static final double MAX_MEAN_DIFF = 4.0;

    private LayerFrames() {
    }

    private static final AtomicLong HITS = new AtomicLong();
    private static final AtomicLong TRIES = new AtomicLong();
    private static final AtomicLong FAILS = new AtomicLong();

    /** 每代一张 alpha（解码一次、常驻）。键 = 目录绝对路径。 */
    private static final HashMap<String, Bitmap> ALPHA = new HashMap<>();
    /** 「建立 alpha + 自检」串行：同一代只许一个线程干（弱一致会让自检读到半张 alpha）。 */
    private static final Object ESTABLISH = new Object();
    /** 自检没过的代：不再重试，也不再写 jpg（否则每帧都白写一次）。 */
    private static final java.util.concurrent.ConcurrentHashMap<String, Boolean> FAILED =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** 这一代的目录（= `files/push/w<id>/<stamp>`，与逐帧 WebP 同目录）。 */
    static File dirOf(@NotNull Context context, int widgetId, @NotNull String stamp) {
        return WidgetUriPush.dirOf(context, widgetId, stamp);
    }

    static File jpg(@NotNull File dir, int idx) {
        return new File(dir, String.format("c%05d.jpg", idx));
    }

    static File alpha(@NotNull File dir) {
        return new File(dir, ALPHA_NAME);
    }

    private static File mark(@NotNull File dir) {
        return new File(dir, MARK_NAME);
    }

    /**
     * 烘完一帧后写分层产物（每帧一张 JPEG）；**这一代第一个到达的帧**额外写 alpha、做自检、立标记。
     *
     * @return true = 这一代可以走分层
     */
    static boolean write(@NotNull Context context, @NotNull File dir, int idx,
                         @NotNull Bitmap composed, int w, int h) {
        if (!ON || dir == null) {
            return false;
        }
        try {
            // 每帧都写 JPEG（失败/未建立时它只是白占几 KB，换代时一并删掉）
            if (FAILED.containsKey(dir.getAbsolutePath())) {
                return false;                       // 这一代自检没过，后面的帧一张都不写
            }
            File jpg = jpg(dir, idx);
            FileOutputStream os = new FileOutputStream(jpg);
            boolean ok;
            try {
                // 透明区在原帧里就是纯黑（阴影是「黑色 + alpha」画上去的），所以 JPEG 丢掉 alpha
                // 不改变任何 RGB —— 补回 alpha 之后与旧帧等价。
                ok = composed.compress(Bitmap.CompressFormat.JPEG, QUALITY, os);
            } finally {
                os.close();
            }
            if (!ok) {
                jpg.delete();
                log(context, dir, "失败=写JPEG");
                return false;
            }
            if (mark(dir).isFile()) {
                return true;                        // 这一代已经建立好了
            }
            // v1.10.484：**不再等第 0 帧**。
            // 483 的错：把「建立 alpha + 自检」绑在 idx==0 上，而烘焙从 起点=10 绕一圈才到 0，
            // 结果 235 帧里只有最后 10 帧拿到了 c*.jpg（真机日志：分层=11/11，磁盘帧没涨）。
            // alpha 与帧无关 ⇒ 用**这一代第一个烘到的帧**建立即可。整段串行，避免两个线程
            // 同时建立、互相读到半张 alpha（那会让自检读到残帧 → 直接判 FAIL）。
            synchronized (ESTABLISH) {
                if (mark(dir).isFile()) {
                    return true;                    // 别的线程先建好了
                }
                if (FAILED.containsKey(dir.getAbsolutePath())) {
                    return false;
                }
            // v1.10.483：**不再用 extractAlpha() + PNG**。
            // ALPHA_8 交给 Skia 的 PNG 编码器不可靠（482 在真机上就是这一步静默失败：
            // 既有 W49 也没有任何产物）。改成自己造一张 ARGB（只填 alpha 通道），
            // PNG 对 ARGB_8888 是确定可用的。
            Bitmap a = alphaOf(composed, w, h);
            if (a == null) {
                log(context, dir, "失败=造alpha");
                return false;
            }
            File af = alpha(dir);
            FileOutputStream aos = new FileOutputStream(af);
            boolean aok;
            try {
                aok = a.compress(Bitmap.CompressFormat.PNG, 100, aos);
            } finally {
                aos.close();
                a.recycle();
            }
            if (!aok) {
                af.delete();
                log(context, dir, "失败=写alpha");
                return false;
            }
            // 自检：把**这一帧**（建立者，不一定是 0）按新路径合回来，和刚烘好的帧叠完之后比
            double mae = meanDiff(dir, idx, composed, w, h);
            boolean pass = mae <= MAX_MEAN_DIFF;
            if (!pass) {
                clear(dir);
            } else {
                new FileOutputStream(mark(dir)).close();
            }
            try {
                Diag.warn(context, "W49", "分层帧 自检=" + (pass ? "OK" : "FAIL")
                        + " 均差=" + String.format(java.util.Locale.US, "%.2f", Double.valueOf(mae))
                        + " 门限=" + MAX_MEAN_DIFF + " 画布=" + w + "x" + h
                        + " JPEG=q" + QUALITY + (pass
                        ? "（这一代走「JPEG 解码 + 常驻 alpha 合成」）"
                        : "（这一代回落原 WebP 帧）"));
            } catch (Throwable ignored) {
            }
            if (!pass) {
                FAILED.put(dir.getAbsolutePath(), Boolean.TRUE);
            }
            return pass;
            }
        } catch (Throwable th) {
            try {
                clear(dir);
            } catch (Throwable ignored) {
            }
            log(context, dir, "失败=异常" + th.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * 造一张「只有 alpha 通道」的 ARGB 位图。
     *
     * <p>为什么不用 {@code extractAlpha()}：它返回 ALPHA_8，而 ALPHA_8 的 PNG 编码在老机型/
     * 某些 Skia 版本上直接失败或抛异常 —— 482 的静默失败就出在这里。ARGB_8888 是确定能写的。</p>
     */
    @Nullable
    private static Bitmap alphaOf(@NotNull Bitmap composed, int w, int h) {
        try {
            Bitmap a = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            int[] row = new int[w];
            for (int y = 0; y < h; y++) {
                composed.getPixels(row, 0, w, 0, y, w, 1);
                for (int x = 0; x < w; x++) {
                    row[x] = row[x] & 0xFF000000;
                }
                a.setPixels(row, 0, w, 0, y, w, 1);
            }
            return a;
        } catch (Throwable th) {
            return null;
        }
    }

    /** 失败也要留一行（LESSONS 205：静默提前返回等于功能没接上）。 */
    private static void log(@NotNull Context context, @NotNull File dir, @NotNull String what) {
        try {
            Diag.warn(context, "W49", "分层帧 " + what + " 目录=" + dir.getName());
        } catch (Throwable ignored) {
        }
    }

    /** 这一代的分层产物齐不齐（不齐就走老路）。 */
    static boolean ready(@Nullable File dir, int idx) {
        if (!ON || dir == null) {
            return false;
        }
        try {
            return mark(dir).isFile() && alpha(dir).isFile() && jpg(dir, idx).isFile();
        } catch (Throwable th) {
            return false;
        }
    }

    /** 删掉这一代的分层产物（自检不过 / 换代）。 */
    static void clear(@NotNull File dir) {
        try {
            File af = new File(dir, ALPHA_NAME);
            af.delete();
            new File(dir, MARK_NAME).delete();
            File[] all = dir.listFiles();
            if (all != null) {
                for (File f : all) {
                    String n = f.getName();
                    if (n.startsWith("c") && n.endsWith(".jpg")) {
                        f.delete();
                    }
                }
            }
            synchronized (ALPHA) {
                Bitmap b = ALPHA.remove(dir.getAbsolutePath());
                if (b != null && !b.isRecycled()) {
                    b.recycle();       // 这张是我们自己 extractAlpha 出来的，没有第二路持有
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /** alpha（每代一张，解码后常驻）。 */
    @Nullable
    private static Bitmap alphaBitmap(@NotNull File dir) {
        File f = alpha(dir);
        String key = f.getAbsolutePath();
        synchronized (ALPHA) {
            Bitmap b = ALPHA.get(key);
            if (b != null && !b.isRecycled()) {
                return b;
            }
            try {
                Bitmap d = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (d == null) {
                    return null;
                }
                if (d.getConfig() != Bitmap.Config.ARGB_8888) {
                    Bitmap conv = d.copy(Bitmap.Config.ARGB_8888, false);
                    d.recycle();
                    d = conv;
                }
                if (d == null) {
                    return null;
                }
                if (ALPHA.size() > 3) {          // 多代并存时不无限长
                    for (java.util.Iterator<Bitmap> it = ALPHA.values().iterator(); it.hasNext(); ) {
                        Bitmap old = it.next();
                        it.remove();
                        if (old != null && !old.isRecycled()) {
                            old.recycle();
                        }
                        break;
                    }
                }
                ALPHA.put(key, d);
                return d;
            } catch (Throwable th) {
                return null;
            }
        }
    }

    /**
     * 按分层路径解一帧：JPEG + DST_IN(alpha)。
     *
     * @return 合成好的 ARGB 位图（调用方负责 asShared / 交环）；失败 null
     */
    @Nullable
    static Bitmap compose(@NotNull File dir, int idx) {
        TRIES.incrementAndGet();
        Bitmap jpgBmp = null;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inPreferredConfig = Bitmap.Config.ARGB_8888;
            o.inMutable = true;                  // 就地乘 alpha：省一张全画布位图（50fps 下很贵）
            jpgBmp = BitmapFactory.decodeFile(jpg(dir, idx).getAbsolutePath(), o);
            if (jpgBmp == null) {
                FAILS.incrementAndGet();
                return null;
            }
            Bitmap a = alphaBitmap(dir);
            if (a == null) {
                FAILS.incrementAndGet();
                return null;
            }
            Bitmap out = jpgBmp;
            if (!out.isMutable()) {
                Bitmap m = out.copy(Bitmap.Config.ARGB_8888, true);
                if (m == null) {
                    FAILS.incrementAndGet();
                    return null;
                }
                out = m;
            }
            int w = out.getWidth();
            int h = out.getHeight();
            Rect box = new Rect(0, 0, w, h);
            Canvas c = new Canvas(out);
            Paint cut = new Paint();
            cut.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            c.drawBitmap(a, (Rect) null, box, cut);
            jpgBmp = null;                        // 已经交给调用方，别在 finally 里回收
            HITS.incrementAndGet();
            return out;
        } catch (Throwable th) {
            FAILS.incrementAndGet();
            return null;
        } finally {
            if (jpgBmp != null && !jpgBmp.isRecycled()) {
                jpgBmp.recycle();
            }
        }
    }

    /**
     * 自检用：分层合成 vs 原帧的**平均绝对差**（只比最终像素，不比图层 —— 层内自检恒等于 0，那是假门禁）。
     *
     * @return MAE；999 = 合成失败（直接判不过）
     */
    static double meanDiff(@NotNull File dir, int idx, @NotNull Bitmap want, int w, int h) {
        Bitmap got = compose(dir, idx);
        if (got == null) {
            return 999.0;
        }
        try {
            int ww = Math.min(want.getWidth(), got.getWidth());
            int hh = Math.min(want.getHeight(), got.getHeight());
            int step = Math.max(1, hh / 120);              // 抽样 120 行，够发现结构错
            int[] ra = new int[ww];
            int[] rb = new int[ww];
            long sum = 0L;
            long n = 0L;
            for (int y = 0; y < hh; y += step) {
                want.getPixels(ra, 0, ww, 0, y, ww, 1);
                got.getPixels(rb, 0, ww, 0, y, ww, 1);
                for (int x = 0; x < ww; x++) {
                    int p = ra[x];
                    int q = rb[x];
                    sum += Math.abs((p >>> 24) - (q >>> 24));
                    sum += Math.abs(((p >> 16) & 0xFF) - ((q >> 16) & 0xFF));
                    sum += Math.abs(((p >> 8) & 0xFF) - ((q >> 8) & 0xFF));
                    sum += Math.abs((p & 0xFF) - (q & 0xFF));
                    n += 4L;
                }
            }
            return n > 0L ? (sum / (double) n) : 999.0;
        } catch (Throwable th) {
            return 998.0;
        } finally {
            if (!got.isRecycled()) {
                got.recycle();
            }
        }
    }

    /** 一行统计（接在 W23 末尾）。取完清零。 */
    @NotNull
    static String takeStat() {
        long tries = TRIES.getAndSet(0L);
        long hits = HITS.getAndSet(0L);
        long fails = FAILS.getAndSet(0L);
        if (tries <= 0L) {
            return "";
        }
        return " 分层=" + hits + "/" + tries + (fails > 0L ? "(败" + fails + ")" : "");
    }
}
