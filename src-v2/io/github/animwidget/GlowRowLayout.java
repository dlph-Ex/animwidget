package io.github.animwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.widget.FrameLayout;

/**
 * v1.10.162（反馈）：**光晕层**。
 *
 * 为什么光晕不能画在滑块自己身上：
 * 滑块的 bounds 是 47.3dp 高，拇指圆心距上边只有 **14.5dp**，
 * 而完整光晕要 **19.07dp**（拇指放大态 11.87 + 3 圈 × 2.4）。
 * 硬件渲染下子 View 只能画在自己的 bounds 里 ——
 * v1.10.160 直接把 inset/centerY 撑大到 20dp（整块条目从 47dp 涨到 60dp，被判「简单粗暴」），
 * v1.10.161 把光晕缩到 13.5dp（可见环只剩 3dp、alpha 5%，等同删掉光晕）。
 *
 * 正解：换绘制容器。本类的高度 = 整行（标题 20dp + 滑块 47dp + 内边距 ≈ 87dp），
 * 在 `dispatchDraw`（子视图画完）之后补画光晕：
 *  · 光晕盖在轨道上、压在拇指下面 —— 与 v1.10.160 的层序完全一致；
 *  · 因为容器够大，19dp 光晕**画得完整**，四边都不会被裁；
 *  · 滑块自己的尺寸、轨道、拇指、档位文字、行高、内外边距**一个都没动**。
 *
 * 三层硬环的参数与 v1.10.160 一模一样（0.05 / 0.062 / 0.074 叠加，圈距 2.4dp），
 * 所以「除了不再被裁，观感和之前相同」。
 */
public final class GlowRowLayout extends FrameLayout {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** 光晕要跟随的滑块；为 null 时本类不画任何东西。 */
    private FontScaleSlider anchor;

    /** 滑块左上角相对本容器的偏移（滑块隔着一层 LinearLayout，不能直接用 getLeft）。 */
    private int anchorOffsetX;
    private int anchorOffsetY;

    public GlowRowLayout(Context context) {
        super(context);
        init();
    }

    public GlowRowLayout(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GlowRowLayout(Context context, android.util.AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.FILL);
        // 光晕纯绘制，不需要背景/点击/焦点，免得抢滑块的触摸
        setWillNotDraw(true);
        setClipChildren(false);
        setClipToPadding(false);
    }

    public void setAnchor(FontScaleSlider slider) {
        this.anchor = slider;
        if (slider != null) {
            slider.setGlowHost(this);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int x = 0;
        int y = 0;
        View v = this.anchor;
        int guard = 0;
        while (v != null && v != this && guard++ < 24) {
            x += v.getLeft();
            y += v.getTop();
            android.view.ViewParent parent = v.getParent();
            v = parent instanceof View ? (View) parent : null;
        }
        this.anchorOffsetX = x;
        this.anchorOffsetY = y;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        drawGlow(canvas);
    }

    private void drawGlow(Canvas canvas) {
        FontScaleSlider s = this.anchor;
        if (s == null || s.getWidth() <= 0) {
            return;
        }
        int color = s.glowColor();
        if ((color >>> 24) == 0) {
            return;
        }
        float cx = anchorOffsetX + s.thumbCenterX();
        float cy = anchorOffsetY + s.glowCenterY();
        float base = s.thumbRadiusNow();
        float gap = s.glowRingGap();
        // 与 v1.10.160 相同的三层：越靠内越亮，叠加出柔和外扩
        for (int k = 3; k >= 1; k--) {
            paint.setColor(withAlpha(color, 0.05f - 0.012f * (3 - k)));
            canvas.drawCircle(cx, cy, base + k * gap, paint);
        }
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(255.0f * (alpha < 0.0f ? 0.0f : (alpha > 1.0f ? 1.0f : alpha)));
        return (color & 0x00FFFFFF) | (a << 24);
    }
}
