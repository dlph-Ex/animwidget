package io.github.animwidget;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v1.10.228：把「参考实现那一次模糊」烘成一块位图，每帧只贴一次。
 *
 * <h3>参考实现（反编译 {@code com.photo.beautifulwidgets.utils.d.G()}）到底做了什么</h3>
 * <pre>
 *   Paint p = new Paint();
 *   p.setMaskFilter(new BlurMaskFilter(unit, Blur.NORMAL));   // unit = 图宽/35
 *   ... 拿这个 paint 把轮廓模糊出来，涂成黑，alpha = 强度+20 ...
 *   canvas.drawBitmap(bitmap, unit*2, unit*2, null);          // 画面在留白里居中
 * </pre>
 *
 * <p>它中间绕了一圈 {@code extractAlpha}（返回的位图多大、要不要拉伸，取决于 Skia 的实现），
 * 然后靠 {@code pad = 4*unit} 把这个不确定量盖住。我们不需要绕：**同一个
 * {@code BlurMaskFilter(radius)} 直接作用在 {@code drawPath} 上**，
 * 得到的就是同一条高斯（Skia 里 σ = radius × 0.57735），而且留白是我们按
 * {@link ShadowSpec#inset} 算出来、恰好等于「阴影看得见的延伸」的那个距离，
 * 不会被裁、也不会留空环。</p>
 *
 * <h3>为什么烘一遍就够</h3>
 * 每帧都换画面，但**轮廓（蒙版形状 + 扩散 + 模糊半径）不变**，所以阴影位图只跟
 * 「画面框尺寸 / 形状 / 旋转 / 蒙版偏移 / 扩散 / 模糊 / 留白」有关。
 * 每帧只剩 1 次 {@code drawBitmap} —— 比 v1.10.221 的 25~28 条描边便宜得多。
 *
 * <h3>缓存</h3>
 * 3 格（组件 / 配置页预览 / 手势层会同时在不同尺寸上画）。逐字段比较签名，不看 hashCode。
 * 被挤掉的那块**不 recycle**（别的线程可能正拿着它画这一帧，recycle 会崩），交给 GC。
 */
public final class ShadowLayer {

    private static final int SLOTS = 3;

    private static final Object LOCK = new Object();
    private static final ShadowLayer[] SLOT = new ShadowLayer[SLOTS];
    private static final long[] SLOT_AGE = new long[SLOTS];
    private static long tick;

    private final Bitmap bitmap;
    private final int pad;
    // 缓存签名
    private final int sigBoxW;
    private final int sigBoxH;
    private final int sigShape;
    private final int sigRotation;
    private final int sigMaskX;
    private final int sigMaskY;
    private final int sigDilateQ;
    private final int sigBlurQ;
    private final int sigPad;

    private ShadowLayer(Bitmap bitmap, int pad, int boxW, int boxH, int shape, int rotation,
                        int maskX, int maskY, int dilateQ, int blurQ) {
        this.bitmap = bitmap;
        this.pad = pad;
        this.sigBoxW = boxW;
        this.sigBoxH = boxH;
        this.sigShape = shape;
        this.sigRotation = rotation;
        this.sigMaskX = maskX;
        this.sigMaskY = maskY;
        this.sigDilateQ = dilateQ;
        this.sigBlurQ = blurQ;
        this.sigPad = pad;
    }

    /** 烘好的阴影（黑色 + alpha，尺寸 = 画面框四周各外扩 {@link #pad()}）。 */
    @NotNull
    public final Bitmap bitmap() {
        return this.bitmap;
    }

    /** 每边留白 px（= 画面框外扩多少，也是画面每边缩进多少）。 */
    public final int pad() {
        return this.pad;
    }

    public final long bytes() {
        return (long) this.bitmap.getByteCount();
    }

    // ---------------------------------------------------------------- 取一块（命中缓存就复用）
    /**
     * @param localPath 画面轮廓（**已平移到画面框左上角为原点**，含蒙版偏移/旋转）
     * @param boxW      画面框宽（px，向上取整）
     * @param boxH      画面框高（px，向上取整）
     * @param dilatePx  δ（高斯 50% 点相对画面边的偏移；负值 = 收缩，50% 点在画面里面）
     * @param blurPx    给 BlurMaskFilter 的半径（σ = blurPx/√3）
     * @param padPx     每边留白（= 范围 L = 画面每边缩进）
     */
    @Nullable
    public static ShadowLayer obtain(@NotNull Path localPath, int boxW, int boxH, float dilatePx, float blurPx, int padPx,
                                     int shapeOrd, int rotation, int maskXq, int maskYq) {
        int bw = Math.max(boxW, 2);
        int bh = Math.max(boxH, 2);
        int pad = Math.max(padPx, 1);
        if (bw < 8 || bh < 8) {
            return null;
        }
        int dilateQ = Math.round(dilatePx * 4.0f);   // 1/4 px 精度当签名
        int blurQ = Math.round(blurPx * 4.0f);
        ShadowLayer hit = find(bw, bh, shapeOrd, rotation, maskXq, maskYq, dilateQ, blurQ, pad);
        if (hit != null) {
            return hit;
        }
        synchronized (LOCK) {
            hit = find(bw, bh, shapeOrd, rotation, maskXq, maskYq, dilateQ, blurQ, pad);
            if (hit != null) {
                return hit;
            }
            Bitmap bmp;
            try {
                bmp = bake(localPath, bw, bh, dilatePx, Math.max(blurPx, 0.5f), pad);
            } catch (Throwable t) {
                // OOM / 极端尺寸：没有阴影也比每帧抛异常强
                return null;
            }
            ShadowLayer fresh = new ShadowLayer(bmp, pad, bw, bh, shapeOrd, rotation, maskXq, maskYq, dilateQ, blurQ);
            // 有空格就用空格；三格都满就淘汰最久没用过的那格（内存上限 = 3 块位图）
            int victim = -1;
            int lru = 0;
            for (int i = 0; i < SLOTS; i++) {
                if (SLOT[i] == null) {
                    victim = i;
                    break;
                }
                if (SLOT_AGE[i] < SLOT_AGE[lru]) {
                    lru = i;
                }
            }
            if (victim < 0) {
                victim = lru;
            }
            SLOT[victim] = fresh;
            SLOT_AGE[victim] = ++tick;
            return fresh;
        }
    }

    @Nullable
    private static ShadowLayer find(int bw, int bh, int shapeOrd, int rotation, int maskXq, int maskYq,
                                    int dilateQ, int blurQ, int pad) {
        for (int i = 0; i < SLOTS; i++) {
            ShadowLayer s = SLOT[i];
            if (s != null
                    && s.sigBoxW == bw && s.sigBoxH == bh
                    && s.sigShape == shapeOrd && s.sigRotation == rotation
                    && s.sigMaskX == maskXq && s.sigMaskY == maskYq
                    && s.sigDilateQ == dilateQ && s.sigBlurQ == blurQ
                    && s.sigPad == pad) {
                SLOT_AGE[i] = ++tick;
                return s;
            }
        }
        return null;
    }

    /**
     * 真的画一次：一次 {@code drawPath} + {@code BlurMaskFilter}（和参考实现同一个滤镜、同一个半径）。
     *
     * <p>画布 = 画面框四周各外扩 {@code pad}，轮廓画在正中（外扩 {@code dilate} 之后）。
     * 模糊的尾巴在 {@code pad} 处已经淡到 {@link ShadowSpec#VISIBLE}（5/255），
     * 所以边缘那一下截断看不出来（门禁会断言这一点）。</p>
     */
    private static Bitmap bake(@NotNull Path localPath, int boxW, int boxH, float dilatePx, float blurPx, int pad) {
        int outW = boxW + pad * 2;
        int outH = boxH + pad * 2;
        Bitmap out = Bitmap.createBitmap(outW, outH, Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(1);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        // 参考实现：new BlurMaskFilter(unit, Blur.NORMAL)；Skia 里 σ = radius × 0.57735
        paint.setMaskFilter(new BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL));
        canvas.translate(pad - dilatePx, pad - dilatePx);
        if (Math.abs(dilatePx) > 0.01f) {
            // δ：把轮廓按比例外扩（δ>0）或收缩（δ<0）—— 等价于把高斯 50% 点挪到画面边外 δ 处
            canvas.scale((boxW + dilatePx * 2.0f) / boxW, (boxH + dilatePx * 2.0f) / boxH);
        }
        canvas.drawPath(localPath, paint);
        return out;
    }

    /** 诊断用：当前缓存里有几块、共多少字节。 */
    public static String stats() {
        int n = 0;
        long bytes = 0;
        for (int i = 0; i < SLOTS; i++) {
            ShadowLayer s = SLOT[i];
            if (s != null) {
                n++;
                bytes += s.bytes();
            }
        }
        return "阴影缓存=" + n + " 块，" + (bytes / 1024L) + "KB";
    }
}
