package io.github.animwidget;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/**
 * v1.10.158（语言项打磨）：语言行右侧的**勾**，纯 Path 画。
 *
 * 为什么不用系统 RadioButton 的圆圈：系统圆圈用的是 `buttonTint`，深浅色切换时
 * 与主色对不齐；而且圆圈在左、文案在右，选中/未选中的差别只有一个小圆点。
 * 这里换成「选中 = 主色淡底 + 主色勾 + 主色文字」一眼可辨，
 * 勾用 Drawable 挂在 TextView 的右侧（compound drawable），不动布局层级。
 *
 * v1.10.159（重做）：加了 **progress（0~1 笔画生长）** 与**运行期换色**。
 * 生长是按「折线总长」插值算的，不是整体 scale —— 缩放会让线宽一起变大，
 * 看着像「弹一下」；生长是笔尖往前走，配 180ms 缓动是一次正经的落笔。
 */
public final class CheckMarkDrawable extends Drawable {

    private final Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float size;

    private int color;
    private float progress = 1.0f;

    public CheckMarkDrawable(int color, float sizePx, float strokePx) {
        this.color = color;
        this.size = Math.max(1.0f, sizePx);
        this.pen.setStyle(Paint.Style.STROKE);
        this.pen.setStrokeCap(Paint.Cap.ROUND);
        this.pen.setStrokeJoin(Paint.Join.ROUND);
        this.pen.setStrokeWidth(Math.max(1.0f, strokePx));
        this.pen.setColor(color);
    }

    /** 0 = 一笔没画（未选中），1 = 画完。 */
    public void setProgress(float p) {
        this.progress = p < 0.0f ? 0.0f : (p > 1.0f ? 1.0f : p);
        invalidateSelf();
    }

    public float getProgress() {
        return this.progress;
    }

    /** 主题换色（不重建 Drawable，避免掉帧）。 */
    public void setColorAlpha(int argb) {
        if (this.color != argb) {
            this.color = argb;
            this.pen.setColor(argb);
            invalidateSelf();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.color == 0 || (this.color >>> 24) == 0 || this.progress <= 0.0f) {
            return;   // 透明 = 不画（未选中态）
        }
        float cx = getBounds().centerX();
        float cy = getBounds().centerY();
        float g = this.size / 2.0f;
        float x0 = cx - g;
        float y0 = cy + g * 0.08f;
        float x1 = cx - g * 0.25f;
        float y1 = cy + g * 0.75f;
        float x2 = cx + g;
        float y2 = cy - g * 0.72f;

        float l1 = (float) Math.hypot(x1 - x0, y1 - y0);
        float l2 = (float) Math.hypot(x2 - x1, y2 - y1);
        float total = l1 + l2;
        if (total <= 0.0f) {
            return;
        }
        float want = total * this.progress;
        this.path.reset();
        this.path.moveTo(x0, y0);
        if (want <= l1) {
            float t = want / l1;
            this.path.lineTo(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t);
        } else {
            this.path.lineTo(x1, y1);
            float t = (want - l1) / l2;
            this.path.lineTo(x1 + (x2 - x1) * t, y1 + (y2 - y1) * t);
        }
        canvas.drawPath(this.path, this.pen);
    }

    @Override
    public void setAlpha(int alpha) {
        this.pen.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        this.pen.setColorFilter(filter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) (this.size * 1.7f);
    }

    @Override
    public int getIntrinsicHeight() {
        return (int) (this.size * 1.7f);
    }
}
