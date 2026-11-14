package io.github.animwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * v1.10.222：阴影剖面的**只读预览图**。
 *
 * <p>v1.10.221 这里是一个可拖 4 点的曲线编辑器，用户判定「拖了画面不变、还得再碰一下主滑块」——
 * 根因是预览的合成缓存 key 漏了曲线（见 {@code ConfigActivity.paramsKey}），
 * 但**交互本身也不该是这个样子**：曲线编辑器把「形状」和「宽度」混在一个平面里，
 * 手指按不准相邻的点，也没有数值可看。现在改成 6 个独立滑块（{@code curve_v0 … curve_end}），
 * 这块图只负责**把 6 个数画成一条曲线**，一眼看得出自己调的是个什么东西。</p>
 *
 * <p>它不可交互：不接 {@code onTouchEvent}，拖动不会改任何参数；颜色不缓存，
 * 每次绘制现取 {@link ThemeManager#colors}，换肤只需一次 invalidate（同 SwitchBoxView）。</p>
 */
public final class ShadowCurveView extends View {

    /** 绘图区内边距（dp）：给描边留地方。 */
    private static final float PAD_L = 10.0f;
    private static final float PAD_R = 10.0f;
    private static final float PAD_T = 10.0f;
    private static final float PAD_B = 10.0f;

    private final Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path line = new Path();
    private final Path fill = new Path();
    private final RectF plot = new RectF();

    @org.jetbrains.annotations.NotNull
    private ShadowCurve curve = ShadowCurve.DEFAULT;

    public ShadowCurveView(Context context) {
        super(context);
    }

    public ShadowCurveView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ShadowCurveView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @org.jetbrains.annotations.NotNull
    public ShadowCurve getCurve() {
        return this.curve;
    }

    public void setCurve(@org.jetbrains.annotations.Nullable ShadowCurve value) {
        ShadowCurve next = value == null ? ShadowCurve.DEFAULT : value;
        if (next == this.curve) {
            return;
        }
        this.curve = next;
        invalidate();
    }

    /** 换肤链路入口（ThemeManager.paint → 这里 → invalidate）。颜色不缓存，绘制时现取。 */
    public void applyColors(ThemeColors c) {
        invalidate();
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * alpha);
        if (a <= 0) {
            a = 1;
        }
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private void measurePlot() {
        float dp = getResources().getDisplayMetrics().density;
        this.plot.set(PAD_L * dp, PAD_T * dp,
                Math.max(PAD_L * dp + 1.0f, getWidth() - PAD_R * dp),
                Math.max(PAD_T * dp + 1.0f, getHeight() - PAD_B * dp));
    }

    private float px(float u) {
        return this.plot.left + u * this.plot.width();
    }

    private float py(float v) {
        return this.plot.bottom - v * this.plot.height();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0.0f || h <= 0.0f) {
            return;
        }
        float dp = getResources().getDisplayMetrics().density;
        ThemeColors c = ThemeManager.INSTANCE.colors(getContext());
        measurePlot();

        // ① 底板
        this.pen.setStyle(Paint.Style.FILL);
        this.pen.setShader(null);
        this.pen.setColor(withAlpha(c.getText(), 0.045f));
        canvas.drawRoundRect(0.0f, 0.0f, w, h, 14.0f * dp, 14.0f * dp, this.pen);

        // ② 网格
        this.pen.setStyle(Paint.Style.STROKE);
        this.pen.setStrokeWidth(Math.max(1.0f, dp));
        this.pen.setColor(withAlpha(c.getText(), 0.07f));
        for (int i = 1; i < 4; i++) {
            float x = this.plot.left + (this.plot.width() * i) / 4.0f;
            canvas.drawLine(x, this.plot.top, x, this.plot.bottom, this.pen);
            float y = this.plot.top + (this.plot.height() * i) / 4.0f;
            canvas.drawLine(this.plot.left, y, this.plot.right, y, this.pen);
        }

        // ③ 曲线 + 下方填充（越靠画面越深）
        int samples = 48;
        this.line.reset();
        this.fill.reset();
        this.fill.moveTo(px(0.0f), py(0.0f));
        for (int i = 0; i <= samples; i++) {
            float u = i / (float) samples;
            float v = this.curve.alphaAt(u);
            float x = px(u);
            this.line.lineTo(x, py(v));
            this.fill.lineTo(x, py(v));
        }
        this.fill.lineTo(px(1.0f), py(0.0f));
        this.fill.close();
        this.pen.setStyle(Paint.Style.FILL);
        this.pen.setShader(new LinearGradient(0.0f, this.plot.top, 0.0f, this.plot.bottom,
                withAlpha(c.getPrimary(), 0.26f), withAlpha(c.getPrimary(), 0.015f), Shader.TileMode.CLAMP));
        canvas.drawPath(this.fill, this.pen);
        this.pen.setShader(null);

        this.pen.setStyle(Paint.Style.STROKE);
        this.pen.setStrokeWidth(2.0f * dp);
        this.pen.setStrokeCap(Paint.Cap.ROUND);
        this.pen.setStrokeJoin(Paint.Join.ROUND);
        this.pen.setColor(c.getPrimary());
        canvas.drawPath(this.line, this.pen);

        // ④ 末端：右边就是「没有阴影的地方」
        float endX = px(this.curve.getEnd());
        this.pen.setStrokeWidth(Math.max(1.0f, 1.1f * dp));
        this.pen.setPathEffect(new DashPathEffect(new float[]{4.0f * dp, 4.0f * dp}, 0.0f));
        this.pen.setColor(withAlpha(c.getText(), 0.30f));
        canvas.drawLine(endX, this.plot.top, endX, this.plot.bottom, this.pen);
        this.pen.setPathEffect(null);
    }
}
