package io.github.animwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 预览虚线框：从「{@code preview_box} 的 foreground」改成**自绘的独立 View**。
 *
 * <h3>为什么不是 foreground</h3>
 * <ol>
 *   <li><b>贴不住画面。</b>foreground 贴的是 {@code preview_box} = **画布**（= 布局框）。
 *       而画面在画布里是「内接」的（{@code FrameComposer.contentRect}），除「填充」外一定比画布小，
 *       于是两边必然留空。这里只认 {@link #setFrame} 给的矩形，
 *       那个矩形由 {@code FrameComposer.pictureRect} 算出，是**画面**的矩形。</li>
 *   <li><b>形变会被 layout 打断。</b>改 foreground 的 View 尺寸要 {@code setLayoutParams}，
 *       每帧一次 {@code requestLayout}；{@code preview_frame} 上那个
 *       {@code OnLayoutChangeListener} 会跟着每帧回调 {@code updatePreviewBox()}，
 *       形变动画就被"取消 → 从当前值重启"无限重置，等于没有动画。
 *       这个 View **不参与布局**（{@code fill_parent} 固定），形变只是改一个 {@link RectF} 再
 *       {@link #invalidate()}，一次 layout 都不触发 —— 打断只剩"从当前矩形续走"这一种结果。</li>
 * </ol>
 *
 * <h3>观感口径</h3>
 * 完全沿用 v1.10.213/214 定下的那三条几何约束（LESSONS 25）：笔画 2dp、虚 6dp / 隔 5dp、圆角 2dp，
 * 且**画面到虚线内沿 = 1dp**。笔画是骑在路径上画的，所以路径要相对画面矩形外扩
 * {@code 笔画/2 + 1dp = 2dp}，内沿才正好离画面 1dp。
 */
public final class PreviewFrameView extends View {

    private final Paint paint = new Paint(1 /* ANTI_ALIAS_FLAG */);
    private final Path path = new Path();
    private final RectF frame = new RectF();
    private final RectF tmp = new RectF();
    private float radius;
    private float outset;
    private boolean hasFrame;

    public PreviewFrameView(@NotNull Context context) {
        this(context, null);
    }

    public PreviewFrameView(@NotNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreviewFrameView(@NotNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float d = getResources().getDisplayMetrics().density;
        this.radius = 2.0f * d;
        // 笔画 2dp → 半笔 1dp；再留 1dp 间隙 ⇒ 路径外扩 2dp
        this.outset = 2.0f * d;
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(2.0f * d);
        this.paint.setColor(0x66707a9a);
        this.paint.setPathEffect(new DashPathEffect(new float[]{6.0f * d, 5.0f * d}, 0.0f));
        // 纯装饰层：绝不参与触摸（非 clickable 的 View 在 onTouchEvent 里返回 false，
        // 事件会落到它下面的 preview_box 上）
        setClickable(false);
        setFocusable(false);
    }

    /** 虚线取色（壁纸主色取反那套，见 ConfigActivity.applyOutlineFromWallpaper）。 */
    public final void setLineColor(int color) {
        if (this.paint.getColor() != color) {
            this.paint.setColor(color);
            invalidate();
        }
    }

    public final int lineColor() {
        return this.paint.getColor();
    }

    public final boolean hasFrame() {
        return this.hasFrame;
    }

    /** 当前正在画的矩形（不含 2dp 外扩），屏幕坐标。没画过时返回空矩形。 */
    @NotNull
    public final RectF frame() {
        return new RectF(this.frame);
    }

    /** 直接落位，不做动画（首次进页 / 无处可动时）。 */
    public final void setFrame(float left, float top, float right, float bottom) {
        this.frame.set(left, top, right, bottom);
        this.hasFrame = true;
        invalidate();
    }

    @Override
    protected void onDraw(@NotNull Canvas canvas) {
        if (!this.hasFrame || this.frame.width() <= 0.0f || this.frame.height() <= 0.0f) {
            return;
        }
        this.tmp.set(this.frame);
        this.tmp.inset(-this.outset, -this.outset);
        // 必须走 Path：硬件加速画布上 drawRoundRect + DashPathEffect 会被当成实线
        this.path.reset();
        this.path.addRoundRect(this.tmp, this.radius + this.outset, this.radius + this.outset,
                Path.Direction.CW);
        canvas.drawPath(this.path, this.paint);
    }
}
