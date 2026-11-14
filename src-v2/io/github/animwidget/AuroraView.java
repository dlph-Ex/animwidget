package io.github.animwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/**
 * v1.10.98 → v1.10.104：开屏 / 开发者页的**流动背景**。
 *
 * 需求 3 的硬要求是「背景流动 + 100% 高斯模糊」；v1.10.104（需求 2）再升级成
 * **动态多色彩巨型光轮**（数量仍是 5 个，不增不改）：
 *
 *   · 每个「光轮」= 3 颗沿圆周转动的光斑（间隔 120°），整体缓慢自转 + 半径呼吸 + 中心漂移；
 *   · 5 个轮子的转速/相位/大小各自不同，所以永远不会有整屏同步的机械感；
 *   · 颜色来自 {@link WallpaperPalette}（桌面壁纸采样 5 色），取不到时回落到当前配色方案；
 *   · 最后在 1/{@link #DOWNSCALE} 分辨率上做两遍横竖盒式模糊再放大 —— 结构全抹平，
 *     视觉等价于 100% 高斯模糊，而且 Android 26 起表现一致。
 */
public final class AuroraView extends View {

    /** 低分辨率倍率：越小越快、越糊。 */
    private static final int DOWNSCALE = 6;
    /** 小图上的模糊半径（两遍横竖 ⇒ 等效半径 ×2）。 */
    private static final int BLUR_RADIUS = 7;
    /** 光轮数量（需求 2.1：数量不变，仍是 5）。 */
    private static final int BLOBS = 5;
    /** 每个轮子上的光斑数。 */
    private static final int DOTS = 3;
    private static final float TAU3 = 2.0943952f;

    private final Paint blobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blitPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);

    private Bitmap low;
    private Canvas lowCanvas;
    private int lw;
    private int lh;
    private int[] bufA;
    private int[] bufB;

    private final Shader[] shaders = new Shader[BLOBS];
    private final float[] seed = {0.00f, 1.90f, 3.70f, 5.10f, 2.60f};
    private final float[] radiusScale = {1.00f, 0.88f, 0.78f, 0.94f, 0.72f};
    private final float[] spin = {0.24f, -0.31f, 0.19f, -0.27f, 0.35f};

    private int baseColor;
    private int[] ringColors;
    private long startAt;
    private boolean frozen;
    private float r0 = 1.0f;

    public AuroraView(Context context) {
        super(context);
        init();
    }

    public AuroraView(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AuroraView(Context context, android.util.AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_NONE, null);
        refreshPalette();
    }

    /** 冻结动画（静态背景用，省电）。 */
    public void setFrozen(boolean value) {
        this.frozen = value;
        invalidate();
    }

    /**
     * 指定 5 个光轮颜色（壁纸采样）。传 null 回到「跟随当前配色方案」。
     */
    public void setRingColors(int[] colors) {
        this.ringColors = colors;
        buildShaders();
        invalidate();
    }

    /** 配色变了以后重新取底色（切深浅色 / 换方案时调）。 */
    public void refreshPalette() {
        ThemeColors c = ThemeManager.INSTANCE.colors(getContext());
        this.baseColor = c.getBackground();
        buildShaders();
        invalidate();
    }

    private void buildShaders() {
        int[] cols = this.ringColors;
        if (cols == null || cols.length < BLOBS) {
            // 兜底：跟随配色方案（特性保留）
            ThemeColors c = ThemeManager.INSTANCE.colors(getContext());
            cols = new int[]{c.getPrimary(), c.getSecondary(), c.getTertiary(),
                    c.getSecondary(), c.getPrimary()};
        }
        for (int i = 0; i < BLOBS; i++) {
            int color = cols[i % cols.length] | 0xFF000000;
            int clear = color & 0x00FFFFFF;
            // 单位半径的径向渐变：绘制时用 canvas.scale 拉伸到任意大小
            this.shaders[i] = new RadialGradient(0.0f, 0.0f, 1.0f,
                    new int[]{color, clear}, new float[]{0.0f, 1.0f},
                    Shader.TileMode.CLAMP);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.lw = Math.max(16, w / DOWNSCALE);
        this.lh = Math.max(16, h / DOWNSCALE);
        release();
        try {
            this.low = Bitmap.createBitmap(this.lw, this.lh, Bitmap.Config.ARGB_8888);
            this.lowCanvas = new Canvas(this.low);
            this.bufA = new int[this.lw * this.lh];
            this.bufB = new int[this.lw * this.lh];
        } catch (Throwable th) {
            // 分配失败就只画纯底色，不新增诊断号
            this.low = null;
            this.lowCanvas = null;
            this.bufA = null;
            this.bufB = null;
        }
        // 光轮基准半径：铺满短边（巨型）
        this.r0 = Math.max(8.0f, (float) Math.min(this.lw, this.lh) * 0.44f);
    }

    private void release() {
        Bitmap b = this.low;
        if (b != null && !b.isRecycled()) {
            b.recycle();
        }
        this.low = null;
        this.lowCanvas = null;
        this.bufA = null;
        this.bufB = null;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        release();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap target = this.low;
        int w = getWidth();
        int h = getHeight();
        if (target == null || target.isRecycled() || w <= 0 || h <= 0) {
            canvas.drawColor(this.baseColor);
            return;
        }
        long now = android.os.SystemClock.uptimeMillis();
        if (this.startAt == 0L) {
            this.startAt = now;
        }
        float t = (now - this.startAt) / 1000.0f;

        Canvas c = this.lowCanvas;
        if (c == null) {
            c = new Canvas(target);
            this.lowCanvas = c;
        }
        c.drawColor(this.baseColor);

        for (int i = 0; i < BLOBS; i++) {
            float ph = this.seed[i];
            // 轮心缓慢漂移（sin/cos 复合，曲线 C∞ 连续，没有折角）
            float cx = this.lw * (0.50f + 0.28f * sin(t * 0.11f + ph) * cos(t * 0.07f + ph * 0.6f));
            float cy = this.lh * (0.50f + 0.26f * cos(t * 0.09f + ph * 1.2f));
            // 轮半径呼吸
            float radius = this.r0 * this.radiusScale[i] * (1.0f + 0.14f * sin(t * 0.13f + ph * 1.7f));
            float orbit = radius * 0.46f;
            float dot = radius * 0.75f;
            float angle = t * this.spin[i] + ph;
            // 轮心柔光：让每个「轮」先是一团光，再被 3 颗绕行光斑带出方向感
            float core = radius * 0.95f;
            c.save();
            c.translate(cx, cy);
            c.scale(core, core);
            this.blobPaint.setShader(this.shaders[i]);
            this.blobPaint.setAlpha(72);
            c.drawCircle(0.0f, 0.0f, 1.0f, this.blobPaint);
            this.blobPaint.setAlpha(255);
            this.blobPaint.setShader(null);
            c.restore();
            for (int k = 0; k < DOTS; k++) {
                float a = angle + k * TAU3;
                float px = cx + cos(a) * orbit;
                float py = cy + sin(a) * orbit;
                c.save();
                c.translate(px, py);
                c.scale(dot, dot);
                this.blobPaint.setShader(this.shaders[i]);
                c.drawCircle(0.0f, 0.0f, 1.0f, this.blobPaint);
                this.blobPaint.setShader(null);
                c.restore();
            }
        }

        // 100% 模糊：两遍横竖盒式模糊（在 1/6 分辨率上）
        blur(target);

        android.graphics.Rect dst = new android.graphics.Rect(0, 0, w, h);
        canvas.drawBitmap(target, null, dst, this.blitPaint);

        if (!this.frozen && isShown()) {
            postInvalidateOnAnimation();
        }
    }

    private void blur(Bitmap bmp) {
        int[] a = this.bufA;
        int[] b = this.bufB;
        int w = this.lw;
        int h = this.lh;
        if (a == null || b == null || a.length < w * h) {
            return;
        }
        bmp.getPixels(a, 0, w, 0, 0, w, h);
        blurH(a, b, w, h, BLUR_RADIUS);
        blurV(b, a, w, h, BLUR_RADIUS);
        blurH(a, b, w, h, BLUR_RADIUS);
        blurV(b, a, w, h, BLUR_RADIUS);
        bmp.setPixels(a, 0, w, 0, 0, w, h);
    }

    private static float sin(float v) {
        return (float) Math.sin(v);
    }

    private static float cos(float v) {
        return (float) Math.cos(v);
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    static void blurH(int[] in, int[] out, int w, int h, int r) {
        int div = r * 2 + 1;
        for (int y = 0; y < h; y++) {
            int row = y * w;
            int sa = 0;
            int sr = 0;
            int sg = 0;
            int sb = 0;
            for (int i = -r; i <= r; i++) {
                int p = in[row + clamp(i, 0, w - 1)];
                sa += (p >>> 24) & 0xFF;
                sr += (p >> 16) & 0xFF;
                sg += (p >> 8) & 0xFF;
                sb += p & 0xFF;
            }
            for (int x = 0; x < w; x++) {
                out[row + x] = ((sa / div) << 24) | ((sr / div) << 16) | ((sg / div) << 8) | (sb / div);
                int po = in[row + clamp(x - r, 0, w - 1)];
                int pi = in[row + clamp(x + r + 1, 0, w - 1)];
                sa += ((pi >>> 24) & 0xFF) - ((po >>> 24) & 0xFF);
                sr += ((pi >> 16) & 0xFF) - ((po >> 16) & 0xFF);
                sg += ((pi >> 8) & 0xFF) - ((po >> 8) & 0xFF);
                sb += (pi & 0xFF) - (po & 0xFF);
            }
        }
    }

    static void blurV(int[] in, int[] out, int w, int h, int r) {
        int div = r * 2 + 1;
        for (int x = 0; x < w; x++) {
            int sa = 0;
            int sr = 0;
            int sg = 0;
            int sb = 0;
            for (int i = -r; i <= r; i++) {
                int p = in[clamp(i, 0, h - 1) * w + x];
                sa += (p >>> 24) & 0xFF;
                sr += (p >> 16) & 0xFF;
                sg += (p >> 8) & 0xFF;
                sb += p & 0xFF;
            }
            for (int y = 0; y < h; y++) {
                out[y * w + x] = ((sa / div) << 24) | ((sr / div) << 16) | ((sg / div) << 8) | (sb / div);
                int po = in[clamp(y - r, 0, h - 1) * w + x];
                int pi = in[clamp(y + r + 1, 0, h - 1) * w + x];
                sa += ((pi >>> 24) & 0xFF) - ((po >>> 24) & 0xFF);
                sr += ((pi >> 16) & 0xFF) - ((po >> 16) & 0xFF);
                sg += ((pi >> 8) & 0xFF) - ((po >> 8) & 0xFF);
                sb += (pi & 0xFF) - (po & 0xFF);
            }
        }
    }
}
