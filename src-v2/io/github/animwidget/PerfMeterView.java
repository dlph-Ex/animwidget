package io.github.animwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * v1.10.496：**剩余性能的分段余量表**（自绘，10 格）。
 *
 * <p>用户要求把「剩余性能」从一行纯文本改成有排版的东西。选分段条而不是 ProgressBar，原因有三：</p>
 * <ul>
 *   <li>ProgressBar 是系统控件，它的 track 在深浅两套主题下反差极低（深色上几乎看不见），
 *       而且我们已经在 {@code ThemeManager.paint()} 里只给 ProgressBar 设 indeterminateTint —— 进度色管不到；</li>
 *   <li>分段（10 格）能一眼数出「还能加几个」的量级，连续条只能看出「大概一半」；</li>
 *   <li>自绘 = 颜色不烘焙，和 {@link SwitchBoxView} 同一条换肤链路（{@code ThemeManager.paint()} →
 *       {@link #applyColors(ThemeColors)} → invalidate），换深浅色/切预设不会停在旧配色。</li>
 * </ul>
 *
 * <p><b>画的长度是「剩余」而不是「已用」</b>：用户关心的是「还有多少地方可以加组件」，
 * 满格 = 一点没占，空格 = 挤满了。配色跟着「紧不紧张」走（宽裕=主色 / 偏紧=辅助色 / 到顶=错误色），
 * 由 {@link #setTone(int)} 下发，和卡片里的百分比同源（都来自 {@link PerfMath.Verdict}）。</p>
 */
public final class PerfMeterView extends View {

    /** 格数：10 格刚好能「数」，再多就数不清了。 */
    private static final int SEGMENTS = 10;

    private final Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private final float density;

    /** 剩余百分比；{@code -1} = 还没算出来（只画轨道，读作「未知」，不假装是 0 或 100）。 */
    private int percent = -1;
    /** 0 = 宽裕，1 = 偏紧，2 = 到顶/超预算。 */
    private int tone = 0;
    /** 最近一次换肤下发的配色（{@code null} 时绘制现取一次 —— 布局预览/未换肤也不会画空）。 */
    private ThemeColors colors;

    public PerfMeterView(Context context) {
        this(context, null);
    }

    public PerfMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PerfMeterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.density = getResources().getDisplayMetrics().density;
        this.pen.setStyle(Paint.Style.FILL);
        this.setWillNotDraw(false);
    }

    /**
     * 换肤链路入口（{@code ThemeManager.paint} → 这里 → invalidate）。
     *
     * <p>和 {@link SwitchBoxView} 一样**不烘焙**颜色：只记下当前主题，绘制时按它算。
     * 存 {@link ThemeColors} 是安全的 —— 那是一张纯 int 的值对象，不持有 Activity。</p>
     */
    public void applyColors(ThemeColors c) {
        this.colors = c;
        invalidate();
    }

    /** 剩余百分比（0~100，超范围夹取，{@code < 0} 保持「未知」）。 */
    public void setPercent(int value) {
        int v = value < 0 ? -1 : Math.max(0, Math.min(100, value));
        if (this.percent != v) {
            this.percent = v;
            invalidate();
        }
    }

    /** 0 = 宽裕（主色）/ 1 = 偏紧（辅助色）/ 2 = 到顶或超预算（错误色）。 */
    public void setTone(int value) {
        int v = value < 0 ? 0 : Math.min(2, value);
        if (this.tone != v) {
            this.tone = v;
            invalidate();
        }
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * alpha);
        if (a <= 0) {
            a = 1;
        }
        return (a << 24) | (color & 0x00FFFFFF);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        ThemeColors c = this.colors;
        if (c == null) {
            try {
                c = ThemeManager.INSTANCE.colors(getContext());
            } catch (Throwable ignored) {
                c = null;
            }
        }
        final int accent = c == null ? 0xFF7AA2F7
                : (this.tone == 2 ? c.getError() : (this.tone == 1 ? c.getTertiary() : c.getPrimary()));
        final int track = c == null ? 0x22888888 : withAlpha(c.getText(), 0.14f);

        // 段宽/缝：缝跟着高度走（10dp 高度 → 约 5dp 缝），但不超过总宽的 2%，并且不小于 2dp
        // （低于 2dp 在高密度屏上等于没缝，10 格会糊成一根连续条）
        float gap = Math.max(2.0f * this.density, Math.min(h * 0.55f, w * 0.02f));
        float segW = (w - gap * (SEGMENTS - 1)) / SEGMENTS;
        if (segW <= 1.0f) {
            gap = 0.0f;
            segW = w / (float) SEGMENTS;
        }
        // 圆角按段宽夹一次：segW < h 时严格按 h/2 画会变成「胶囊里的胶囊」，边缘糊成一团
        float radius = Math.min(h / 2.0f, segW / 2.0f);
        if (radius < 0.0f) {
            radius = 0.0f;
        }
        int filled = 0;
        if (this.percent >= 0) {
            filled = Math.round(this.percent * SEGMENTS / 100.0f);
            // 还有余量就至少亮一格：0.x% 显示成「一格都没有」会被读成「没算出来」
            if (filled <= 0 && this.percent > 0) {
                filled = 1;
            }
        }
        for (int i = 0; i < SEGMENTS; i++) {
            float left = i * (segW + gap);
            this.box.set(left, 0.0f, left + segW, h);
            this.pen.setColor(i < filled ? accent : track);
            canvas.drawRoundRect(this.box, radius, radius, this.pen);
        }
    }
}
