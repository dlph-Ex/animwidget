package io.github.animwidget;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import org.jetbrains.annotations.Nullable;

/**
 * v1.10.105（反馈 1）：**毛玻璃卡片底**。
 *
 * 之前弹窗卡片是纯色圆角块，看着就像一块贴纸。现在：
 *   · 填充用带 alpha 的 surface（能透出背后的模糊光）；
 *   · 描边是一条 **1.2dp 的渐变高光边**：左上亮 → 右下几乎透明，
 *     模拟真实玻璃边缘受光的样子（Material 3 / iOS 毛玻璃都是这个套路）；
 *   · 浅色主题自动换成低透明度墨色描边，不会在浅底上"发白看不见"。
 */
public final class GlassFrameDrawable extends Drawable {

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /** v1.10.417：两处「发光域」的画笔（左上 / 右下各一个径向渐变）。 */
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF outer = new RectF();
    private final RectF inner = new RectF();
    private final android.graphics.Path clip = new android.graphics.Path();
    /** 两个发光域的 shader（只在 bounds 变化时重建，别在 draw 里 new）。 */
    @Nullable
    private Shader glowTopLeft;
    @Nullable
    private Shader glowBottomRight;

    private final int fillColor;
    private final float radiusPx;
    private final float strokePx;
    private final boolean dark;
    private int alpha = 255;

    public GlassFrameDrawable(int fillColor, float radiusPx, float strokePx, boolean dark) {
        this.fillColor = fillColor;
        this.radiusPx = radiusPx;
        this.strokePx = Math.max(1.0f, strokePx);
        this.dark = dark;
        this.fillPaint.setStyle(Paint.Style.FILL);
        this.strokePaint.setStyle(Paint.Style.STROKE);
        this.strokePaint.setStrokeWidth(this.strokePx);
        this.glowPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        this.outer.set(bounds);
        // v1.10.417（需求）：**浅色模式下描边也要看得清**。
        // 原来浅色是从 0x44000000（26% 墨）起淡出，白底上几乎看不见；加到 36%，
        // 并把中段提到 20%、尾段 8% —— 仍然是一圈很细的高光边，不是死黑线。
        int[] colors = this.dark
                ? new int[]{0x7AFFFFFF, 0x28FFFFFF, 0x0FFFFFFF, 0x00FFFFFF}
                : new int[]{0x5C000000, 0x33000000, 0x14000000, 0x00000000};
        // 沿着「左上 → 右下」给描边上色：受光面亮，背面虚
        Shader shader = new LinearGradient(bounds.left, bounds.top,
                bounds.left + bounds.width() * 0.62f, bounds.bottom,
                colors, new float[]{0.0f, 0.38f, 0.72f, 1.0f},
                Shader.TileMode.CLAMP);
        this.strokePaint.setShader(shader);
        // 圆角裁切路径：发光只许在卡片内部，绝不许溢到外面
        this.clip.reset();
        this.clip.addRoundRect(this.outer, this.radiusPx, this.radiusPx,
                android.graphics.Path.Direction.CW);
        // 两个发光域（左上 / 右下）：半径取长边的 0.62，中心最亮、随半径衰减到 0
        float glowRadius = Math.max(bounds.width(), bounds.height()) * 0.62f;
        int core = this.dark ? 0x24FFFFFF : 0x12000000;
        int edge = this.dark ? 0x00FFFFFF : 0x00000000;
        this.glowTopLeft = new android.graphics.RadialGradient(
                bounds.left + bounds.width() * 0.10f, bounds.top + bounds.height() * 0.08f,
                glowRadius, new int[]{core, edge}, new float[]{0.0f, 1.0f}, Shader.TileMode.CLAMP);
        // 右下比左上弱一档：主光在左上，这个只负责「另一侧也有反光」
        int brCore = this.dark ? 0x1AFFFFFF : 0x0E000000;
        this.glowBottomRight = new android.graphics.RadialGradient(
                bounds.right - bounds.width() * 0.10f, bounds.bottom - bounds.height() * 0.08f,
                glowRadius, new int[]{brCore, edge}, new float[]{0.0f, 1.0f}, Shader.TileMode.CLAMP);
    }

    /**
     * v1.10.417（需求）：**两个发光域 —— 左上 + 右下**。
     *
     * <p>原来只有左上角有光（描边的渐变从左上往右下淡），右下角完全哑 ——
     * 一张卡片只受一个方向的光，看着像贴纸。现在两角各一个径向渐变：
     * 中心最亮、随半径衰减到 0，所以「离发光域越远光越暗」；
     * 两个域在中间叠加 ≈ 0，卡面中段最沉，正好撑出玻璃的厚度。
     *
     * <p>没做「右上 / 左下」：它们在真实玻璃上是被遮住的背光面，加了会变成四角发亮的塑料片。
     */
    private void drawGlow(Canvas canvas) {
        float w = this.outer.width();
        float h = this.outer.height();
        if (w <= 0.0f || h <= 0.0f) {
            return;
        }
        if (this.glowTopLeft == null || this.glowBottomRight == null) {
            return;
        }
        canvas.save();
        canvas.clipPath(this.clip);
        this.glowPaint.setAlpha(this.alpha);
        this.glowPaint.setShader(this.glowTopLeft);          // 左上
        canvas.drawRect(this.outer, this.glowPaint);
        this.glowPaint.setShader(this.glowBottomRight);      // 右下
        canvas.drawRect(this.outer, this.glowPaint);
        canvas.restore();
        this.glowPaint.setShader(null);
    }

    @Override
    public void draw(Canvas canvas) {
        this.fillPaint.setColor(this.fillColor);
        this.fillPaint.setAlpha(this.alpha);
        canvas.drawRoundRect(this.outer, this.radiusPx, this.radiusPx, this.fillPaint);
        // 发光压在填充之上、描边之下：先有"光斑"，再由描边收口
        drawGlow(canvas);

        float half = this.strokePx / 2.0f;
        this.inner.set(this.outer.left + half, this.outer.top + half,
                this.outer.right - half, this.outer.bottom - half);
        float r = Math.max(0.0f, this.radiusPx - half);
        this.strokePaint.setAlpha(this.alpha);
        canvas.drawRoundRect(this.inner, r, r, this.strokePaint);
    }

    @Override
    public void getOutline(Outline outline) {
        // 给 setElevation 的阴影一个圆角轮廓，不然会画成方块
        outline.setRoundRect((int) Math.ceil(this.outer.left), (int) Math.ceil(this.outer.top),
                (int) Math.floor(this.outer.right), (int) Math.floor(this.outer.bottom),
                Math.max(0.0f, this.radiusPx));
    }

    @Override
    public void setAlpha(int value) {
        this.alpha = Math.max(0, Math.min(255, value));
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.fillPaint.setColorFilter(colorFilter);
        this.strokePaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return this.alpha >= 255 ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT;
    }
}
