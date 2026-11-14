package io.github.animwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.View;
import android.widget.ProgressBar;

import org.jetbrains.annotations.Nullable;

/**
 * 解析圆环（v1.10.1）。
 *
 * 需求 4 / 8：
 * • 独立循环动画，只由系统帧回调（Choreographer/vsync）驱动；
 * • 角度按【时间】算，不依赖帧数 —— 60 / 90 / 120Hz 与自适应刷新率下速度一致，刷新率变了立即跟随；
 * • 完全不吃任务进度和进度回调（进度只影响下面的解析条），导出再重也不会让圆环卡住；
 * • 遵循系统「动画时长缩放」：设为 0（关闭动画）时圆环不转，只画静态弧。
 *
 * 仍然继承 ProgressBar，这样布局里的 id / 显隐 / alpha 用法与以前完全一致。
 */
public final class SpinRingView extends ProgressBar {

    /** 转一圈的时间（按时间算，所以与刷新率无关）。 */
    private static final long PERIOD_MS = 1100L;

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final long startedAt = SystemClock.elapsedRealtime();
    private boolean animating;
    @Nullable
    private Choreographer.FrameCallback frameCallback;

    public SpinRingView(Context context) {
        this(context, null);
    }

    public SpinRingView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpinRingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float density = getResources().getDisplayMetrics().density;
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        arcPaint.setStrokeWidth(Math.max(2.0f, 3.0f * density));
        arcPaint.setColor(0xFF16A34A);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setStrokeWidth(Math.max(2.0f, 3.0f * density));
        trackPaint.setColor(0x33888888);
    }

    /** 主题/黑白切换时重染。 */
    public void setColors(int arcColor, int trackColor) {
        arcPaint.setColor(arcColor);
        trackPaint.setColor((trackColor & 0x00FFFFFF) | 0x33000000);
        invalidate();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        float stroke = arcPaint.getStrokeWidth();
        float inset = stroke * 0.5f + 1.0f;
        oval.set(inset, inset, width - inset, height - inset);
        canvas.drawArc(oval, 0.0f, 359.9f, false, trackPaint);
        double phase = animationPhase();
        float start = (float) (phase * 360.0) - 90.0f;
        float sweep = 80.0f + 50.0f * (float) Math.abs(Math.sin(phase * Math.PI));
        canvas.drawArc(oval, start, sweep, false, arcPaint);
        if (shouldSpin() && !animating) {
            startSpin();
        }
    }

    /** 0~1 的相位：由时间决定（动画缩放为 0 时固定在 0.25，画一条静态弧）。 */
    private double animationPhase() {
        float scale = animatorScale();
        if (scale <= 0.0f) {
            return 0.25;
        }
        long elapsed = SystemClock.elapsedRealtime() - startedAt;
        double turns = (elapsed % ((long) (PERIOD_MS / Math.max(scale, 0.01f))))
                / (PERIOD_MS / Math.max(scale, 0.01f));
        return turns;
    }

    private boolean shouldSpin() {
        return animatorScale() > 0.0f && getVisibility() == View.VISIBLE && isAttachedToWindow();
    }

    /** 系统「动画时长缩放」：0 = 关闭动画。 */
    private float animatorScale() {
        try {
            return Settings.Global.getFloat(getContext().getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f);
        } catch (Throwable th) {
            return 1.0f;
        }
    }

    private void startSpin() {
        stopSpin();
        animating = true;
        frameCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                if (!animating) {
                    return;
                }
                invalidate();
                if (shouldSpin()) {
                    Choreographer.getInstance().postFrameCallback(this);
                } else {
                    animating = false;
                }
            }
        };
        Choreographer.getInstance().postFrameCallback(frameCallback);
    }

    private void stopSpin() {
        if (frameCallback != null) {
            Choreographer.getInstance().removeFrameCallback(frameCallback);
            frameCallback = null;
        }
        animating = false;
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == View.VISIBLE) {
            invalidate();
        } else {
            stopSpin();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopSpin();
        super.onDetachedFromWindow();
    }
}
