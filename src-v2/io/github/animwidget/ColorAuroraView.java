package io.github.animwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import org.jetbrains.annotations.NotNull;

/**
 * v1.10.140（Z1~Z4）：**调色页的「巨型光轮 + 100% 模糊」背景**。
 *
 * 需求逐条对应：
 *   · Z1 背景 100% 高斯模糊 —— 在 1/{@link #DOWNSCALE} 分辨率上画 3 团柔光，
 *     再做两遍横竖盒式模糊（等效半径 ×2）后双线性放大，结构完全消失，只剩色雾；
 *   · Z2 3 个动态巨型光轮 —— 每团 = 巨型径向柔光 + 一颗偏心副核（同色、更小），
 *     中心沿 sin/cos 复合曲线缓慢漂移、半径呼吸；**位置从上次退出的相位继续**，
 *     所以看起来是「一直在那里慢慢转」，而不是每次重新开始；
 *   · Z3 参考的是开屏页已经在用的 {@link AuroraView} 那套（低分辨率 + 盒式模糊），
 *     不是新造渲染管线 —— 三个轮子比五个轮子更省，60fps 也不需要；
 *   · Z4 三色 = 主色 / 辅助色 / 第三色各占一轮，**排列每次换**：{@link #nextOrder}
 *     记录最近 3 次进入用过的排列（3! = 6 种），新的一次一定不与这 3 次重复。
 *
 * 颜色不在这里取（由调用方传进来），这样它将来也能给别的页面当背景。
 */
public final class ColorAuroraView extends View {

    /** 低分辨率倍率：越小越糊、越快。 */
    private static final int DOWNSCALE = 8;
    /** 小图上的盒式模糊半径（两遍横竖 ⇒ 等效半径 ×2）。 */
    private static final int BLUR_RADIUS = 9;
    /** 巨型光轮数量（Z2：3 个）。 */
    private static final int RINGS = 3;

    private static final String PREFS = "settings";
    private static final String KEY_PHASE = "color_aurora_phase";
    private static final String KEY_HISTORY = "color_aurora_history";

    /** 3 个轮子各自的相位/性格（位置差开，永远不会同步成一坨）。 */
    private static final float[] SEED = {0.00f, 2.15f, 4.35f};
    /** 半径系数：一大一小一中，叠出「有前有后」的层次。 */
    private static final float[] RADIUS = {1.02f, 0.82f, 1.20f};
    /** v1.10.145：整体播放速度倍率（用户要求「提高一点点」）。 */
    private static final float SPEED = 1.30f;

    private final Paint blobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blitPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private final Shader[] shaders = new Shader[RINGS];

    private int[] ringColors = {0xFF7C4DFF, 0xFF26C6DA, 0xFFEF5350};
    private int baseColor = 0xFF101014;

    private Bitmap low;
    private Canvas lowCanvas;
    private int lw;
    private int lh;
    private int[] bufA;
    private int[] bufB;

    /** 相位起点（秒）—— 从上次退出时的位置继续。 */
    private float phase;
    private long startAt;
    private float r0 = 1.0f;
    private boolean running = true;

    public ColorAuroraView(Context context) {
        super(context);
        buildShaders();
    }

    public ColorAuroraView(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        buildShaders();
    }

    /** 三个轮子的颜色（顺序 = 轮 0/1/2，调用方按 {@link #nextOrder} 排好）。 */
    public void setRingColors(@NotNull int[] colors) {
        if (colors != null && colors.length >= RINGS) {
            this.ringColors = new int[]{colors[0], colors[1], colors[2]};
            buildShaders();
            invalidate();
        }
    }

    public void setBaseColor(int color) {
        this.baseColor = color;
        invalidate();
    }

    /** 时间轴起点（秒）：进入页面时用上次存的值，光轮位置就接上了。 */
    public void setPhase(float seconds) {
        this.phase = seconds;
        this.startAt = SystemClock.uptimeMillis();
        invalidate();
    }

    /** 当前相位（秒）：退出时存回去。 */
    public float currentPhase() {
        if (this.startAt == 0L) {
            this.startAt = SystemClock.uptimeMillis();
        }
        return this.phase + (SystemClock.uptimeMillis() - this.startAt) / 1000.0f;
    }

    public void stop() {
        this.running = false;
    }

    private void buildShaders() {
        for (int i = 0; i < RINGS; i++) {
            int color = this.ringColors[i] | 0xFF000000;
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
            this.low = null;
            this.lowCanvas = null;
            this.bufA = null;
            this.bufB = null;
        }
        // 巨型：基准半径扑到短边的 0.72（v1.10.145 再放大一圈，三个叠加后整屏都是色雾）
        this.r0 = Math.max(8.0f, (float) Math.min(this.lw, this.lh) * 0.72f);
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
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.running = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        this.running = false;
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
        if (this.startAt == 0L) {
            this.startAt = SystemClock.uptimeMillis();
        }
        float t = currentPhase() * SPEED;

        Canvas c = this.lowCanvas;
        if (c == null) {
            c = new Canvas(target);
            this.lowCanvas = c;
        }
        c.drawColor(this.baseColor);

        for (int i = 0; i < RINGS; i++) {
            float ph = SEED[i];
            // 轮心漂移：两条不同频率的三角波相乘，曲线光滑无折角，周期很长（看不出循环）
            float cx = this.lw * (0.50f + 0.30f * sin(t * 0.055f + ph) * cos(t * 0.037f + ph * 0.7f));
            float cy = this.lh * (0.50f + 0.28f * cos(t * 0.048f + ph * 1.3f));
            // 半径呼吸 ±10%
            float radius = this.r0 * RADIUS[i] * (1.0f + 0.10f * sin(t * 0.070f + ph * 1.9f));

            // ① 巨型主核
            c.save();
            c.translate(cx, cy);
            c.scale(radius, radius);
            this.blobPaint.setShader(this.shaders[i]);
            this.blobPaint.setAlpha(140);
            c.drawCircle(0.0f, 0.0f, 1.0f, this.blobPaint);
            this.blobPaint.setShader(null);
            c.restore();

            // ② 偏心副核：让每个轮子不是标准圆，边界更有机
            float px = cx + cos(t * 0.090f + ph) * radius * 0.42f;
            float py = cy + sin(t * 0.075f + ph) * radius * 0.38f;
            c.save();
            c.translate(px, py);
            c.scale(radius * 0.62f, radius * 0.62f);
            this.blobPaint.setShader(this.shaders[i]);
            this.blobPaint.setAlpha(120);
            c.drawCircle(0.0f, 0.0f, 1.0f, this.blobPaint);
            this.blobPaint.setShader(null);
            c.restore();
        }

        // Z1：100% 模糊 —— 两遍横竖盒式模糊（在 1/8 分辨率上）
        blur(target);

        canvas.drawBitmap(target, null, new Rect(0, 0, w, h), this.blitPaint);

        if (this.running && isShown()) {
            // 30fps 足够：光轮几十秒才走完一个周期，60fps 只是白烧电
            postInvalidateDelayed(33L);
        }
    }

    /**
     * v1.10.147（反馈 2）：取画面**顶部/底部一条**的平均色，给系统栏上色用。
     *
     * Dialog 是 floating 窗口，系统栏那一块其实仍由系统绘制 —— 透明那套在 ROM 上
     * 不一定听（实测图里还是白条）。改成「把系统栏刷成紧挨着它的那条色雾的平均色」，
     * 视觉上就接上了，而且不依赖 ROM 支不支持透明系统栏。
     */
    public int edgeColor(boolean top) {
        int[] a = this.bufA;
        int w = this.lw;
        int h = this.lh;
        if (a == null || w <= 0 || h <= 0 || a.length < w * h) {
            return this.baseColor;
        }
        int band = Math.max(1, h / 12);
        int y0 = top ? 0 : (h - band);
        int y1 = top ? band : h;
        long r = 0L;
        long g = 0L;
        long b = 0L;
        int count = 0;
        for (int y = y0; y < y1; y++) {
            int row = y * w;
            for (int x = 0; x < w; x += 2) {
                int p = a[row + x];
                r += (p >> 16) & 0xFF;
                g += (p >> 8) & 0xFF;
                b += p & 0xFF;
                count++;
            }
        }
        if (count == 0) {
            return this.baseColor;
        }
        int rr = (int) (r / count);
        int gg = (int) (g / count);
        int bb = (int) (b / count);
        if (rr == 0 && gg == 0 && bb == 0) {
            return this.baseColor;
        }
        return 0xFF000000 | (rr << 16) | (gg << 8) | bb;
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
        AuroraView.blurH(a, b, w, h, BLUR_RADIUS);
        AuroraView.blurV(b, a, w, h, BLUR_RADIUS);
        AuroraView.blurH(a, b, w, h, BLUR_RADIUS);
        AuroraView.blurV(b, a, w, h, BLUR_RADIUS);
        bmp.setPixels(a, 0, w, 0, 0, w, h);
    }

    private static float sin(float v) {
        return (float) Math.sin(v);
    }

    private static float cos(float v) {
        return (float) Math.cos(v);
    }

    // ------------------------------------------------------------ 相位 / 排列

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, 0);
    }

    /** 上次退出时的相位；没有记录就随机一个起点（等于「第一次随机」）。 */
    public static float loadPhase(@NotNull Context context) {
        float value = prefs(context).getFloat(KEY_PHASE, -1.0f);
        // v1.10.150：日志里发现相位恒为 0（A06 相位=0 ×8 次），说明之前存进 prefs 的是 0。
        // <=0 一律当作「没有记录」，重新随机一个起点，避免光轮永远停在同一个位置。
        if (value <= 0.0f || Float.isNaN(value) || Float.isInfinite(value)) {
            value = 1.0f + (float) (Math.random() * 600.0);
        }
        return value;
    }

    public static void savePhase(@NotNull Context context, float phase) {
        if (Float.isNaN(phase) || Float.isInfinite(phase)) {
            return;
        }
        prefs(context).edit().putFloat(KEY_PHASE, phase).apply();
    }

    /** v1.10.149（A5）：当前记录的排列历史，给诊断日志 A06 用。 */
    @NotNull
    public static String history(@NotNull Context context) {
        String value = prefs(context).getString(KEY_HISTORY, "");
        return value == null ? "" : value;
    }

    private static final String[] PERMUTATIONS = {
            "012", "021", "102", "120", "201", "210"
    };

    /**
     * Z4：给出本次进入要用的「颜色排列」。
     *
     * 返回 3 个索引（0=主色、1=辅助色、2=第三色），含义是「轮 0/1/2 各用哪个色」。
     * 记录最近 3 次用过的排列；本次一定与它们都不同（6 种排列，最多排掉 3 种，恒有 3 种可选）。
     */
    @NotNull
    public static int[] nextOrder(@NotNull Context context) {
        SharedPreferences p = prefs(context);
        String history = p.getString(KEY_HISTORY, "");
        java.util.ArrayList<String> used = new java.util.ArrayList<>();
        if (history != null && history.length() > 0) {
            for (String token : history.split(",")) {
                if (token != null && token.length() == 3) {
                    used.add(token);
                }
            }
        }
        java.util.ArrayList<String> candidates = new java.util.ArrayList<>();
        for (String permutation : PERMUTATIONS) {
            if (!used.contains(permutation)) {
                candidates.add(permutation);
            }
        }
        if (candidates.isEmpty()) {
            // 理论上到不了这里（6 - 3 = 3）；真到了就清空历史重来，绝不抛异常
            used.clear();
            for (String permutation : PERMUTATIONS) {
                candidates.add(permutation);
            }
        }
        String pick = candidates.get((int) (Math.random() * candidates.size()));
        used.add(pick);
        while (used.size() > 3) {
            used.remove(0);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < used.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(used.get(i));
        }
        p.edit().putString(KEY_HISTORY, sb.toString()).apply();
        return new int[]{pick.charAt(0) - '0', pick.charAt(1) - '0', pick.charAt(2) - '0'};
    }
}
