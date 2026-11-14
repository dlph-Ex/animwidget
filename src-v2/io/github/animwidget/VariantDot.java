package io.github.animwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import org.jetbrains.annotations.NotNull;

/**
 * v1.10.70：简单变体的圆点 —— 一个圆里三段：
 *   上半 = 主色、左下 = 第三色、右下 = 辅助色（对应"一个种子色推出整套配色"）。
 * 点一下就是把这个种子算出的整套配色里的**当前槽位**填进去。
 */
final class VariantDot extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int primary;
    private final int secondary;
    private final int tertiary;

    VariantDot(@NotNull Context context, @NotNull int[] palette) {
        super(context);
        this.primary = palette[Palette.PRIMARY];
        this.secondary = palette[Palette.SECONDARY];
        this.tertiary = palette[Palette.TERTIARY];
        this.paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(@NotNull Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float size = Math.min(w, h);
        float cx = w / 2.0f;
        float cy = h / 2.0f;
        float r = size / 2.0f;
        this.rect.set(cx - r, cy - r, cx + r, cy + r);
        // Android 的角度：0° 在 3 点方向、顺时针
        this.paint.setColor(this.primary);
        canvas.drawArc(this.rect, 180.0f, 180.0f, true, this.paint);   // 上半
        this.paint.setColor(this.tertiary);
        canvas.drawArc(this.rect, 90.0f, 90.0f, true, this.paint);     // 左下
        this.paint.setColor(this.secondary);
        canvas.drawArc(this.rect, 0.0f, 90.0f, true, this.paint);      // 右下
        // v1.10.148（反馈 2）：一圈极淡描边。白卡片上亮黄色圆点原本没有边界，
        // 看着就是「糊在上面的一块」；加 1dp 轮廓后每个点都有清楚的边，"光感"立起来。
        float dp = getResources().getDisplayMetrics().density;
        ThemeColors c = ThemeManager.INSTANCE.colors(getContext());
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(1.0f * dp);
        this.paint.setColor(ThemeManager.INSTANCE.withAlpha(c.getText(), 0.14f));
        canvas.drawCircle(cx, cy, Math.max(0.0f, r - 0.5f * dp), this.paint);
        this.paint.setStyle(Paint.Style.FILL);
    }
}
