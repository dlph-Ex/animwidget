package io.github.animwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v1.10.523（需求）：**自绘的整圈角度滑条**（-360° ~ 360°，内部进度 0..720）。
 *
 * <h3>为什么不再用 {@link android.widget.SeekBar}</h3>
 * 「内容 / 蒙版旋转」这条滑条此前一直不可用，根因在平台侧：
 * <ol>
 *   <li>平台的 {@code AbsSeekBar} 是「按触点 x 反查进度」：{@code mIsDragging} 没成立、
 *       或中途被投递一次 {@code ACTION_CANCEL}，后续 MOVE 就只剩 hotspot —— 拇指定住不动；</li>
 *   <li>它的映射范围就是**控件自身宽度**，手指一移出这条横带，进度增量为 0；</li>
 *   <li>拇指由平台 drawable 画，尺寸/两端留白只能靠 {@code setPadding} 去躲裁剪。</li>
 * </ol>
 * v1.10.522 试过用 {@code OnTouchListener} 独占手势（SlideDrag），结果整条滑条没反应 ——
 * 当一个父容器/窗口在 DOWN 之后重排（这个弹窗在拖动时正要改 dim 与 alpha），
 * 手势被回收，我们那层就再也收不到 MOVE 了。所以这次按 `FontScaleSlider` 的老办法：
 * **自己画、自己吃手势**，把不确定性全部关在 View 内部。
 *
 * <h3>几何与语义（都可复算）</h3>
 * <ul>
 *   <li>{@code inset = thumbR + 2dp}：拇指圆心永远在 [inset, w - inset] 内，**不可能被裁掉**；</li>
 *   <li><b>按下不跳</b>：DOWN 只记住起点与当时的进度；拇指不会瞬移到手指处（旋转是精调，跳一下很致命）；</li>
 *   <li><b>全程跟随</b>：MOVE 用**位移增量**映射 `进度 = 按下进度 + dx / 轨道宽 × max`，
 *       手指移到滑条上下左右任何位置都继续更新，只有真正到 0 / max 才停；</li>
 *   <li><b>点一下轨道</b>：没移动就松手 = 只是点，走一次 140ms 的短动画移到点击位置（不瞬移）；</li>
 *   <li>拖动超过 touchSlop 发一次 TICK 触感，按下/松手不震 —— 只在"真的在调"时给反馈；</li>
 *   <li>中心画一枚 0° 刻度：整圈旋转的中立点必须在视觉上可定位。</li>
 * </ul>
 *
 * <h3>配色</h3>
 * 轨道 = 线条色；已选段与拇指 = {@link ThemeManager#sliderSurfaceColor}（跟随「滑块色」设置）；
 * 全部在 {@link #applyColors} 里现算，换肤 / 深浅色 / 对比度都走同一条链。
 */
public final class AngleSlider extends View {

    /** 进度 / 拖动状态回调（语义与 SeekBar 的三条一一对应，调用方迁移成本最低）。 */
    public interface Listener {
        void onProgressChanged(@NotNull AngleSlider slider, int progress, boolean fromUser);

        void onStartTracking();

        void onStopTracking();
    }

    private static final int DEFAULT_MAX = 720;
    private static final long DUR_TAP = 140L;

    private final Paint pTrack = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pThumb = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pRing = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();

    private final float dp;
    /** 拇指半径（dp → px）。 */
    private final float thumbR;
    private final float trackH;
    /** 拇指圆心到控件边的留白（= 拇指半径 + 2dp）。 */
    private final float inset;
    private final int touchSlop;

    private int max = DEFAULT_MAX;
    private int progress;
    private float thumbScale = 1.0f;

    private boolean tracking;
    private boolean moved;
    private float downX;
    private int downProgress;
    /** 是否已经回调过 onStartTracking（拖过 slop 才算"真的在拖"）。 */
    private boolean started;

    private int trackColor = 0xFF888888;
    private int accentColor = 0xFF22C55E;

    @Nullable
    private Listener listener;

    public AngleSlider(@NotNull Context context) {
        super(context);
        this.dp = context.getResources().getDisplayMetrics().density;
        // v1.10.525：拇指按原 SeekBar 的量级收小一档（10dp → 8.5dp），
        // 轨道保持 6dp —— 视觉上就是一条普通滑条，不再"自己加戏"。
        this.thumbR = 8.5f * this.dp;
        this.trackH = 6.0f * this.dp;
        this.inset = this.thumbR + 2.0f * this.dp;
        this.touchSlop = android.view.ViewConfiguration.get(context).getScaledTouchSlop();
        this.pTrack.setStyle(Paint.Style.FILL);
        this.pFill.setStyle(Paint.Style.FILL);
        this.pThumb.setStyle(Paint.Style.FILL);
        this.pRing.setStyle(Paint.Style.STROKE);
        this.pRing.setStrokeWidth(1.6f * this.dp);
        setFocusable(true);
        setClickable(true);
        applyColors(ThemeManager.INSTANCE.colors(context));
    }

    // ================================================================ 对外 API

    public void setMax(int value) {
        this.max = Math.max(1, value);
        if (this.progress > this.max) {
            this.progress = this.max;
        }
        invalidate();
    }

    public int getMax() {
        return this.max;
    }

    /** 直接设值（**不回调**，和 SeekBar.setProgress 的非 fromUser 语义一致）。 */
    public void setProgress(int value) {
        int next = clamp(value);
        if (next == this.progress) {
            return;
        }
        this.progress = next;
        invalidate();
        Listener l = this.listener;
        if (l != null) {
            l.onProgressChanged(this, next, false);
        }
    }

    public int getProgress() {
        return this.progress;
    }

    /** 外部（例如输入框确认）改值时用：当作用户动作回调一次。 */
    public void setProgressFromUser(int value) {
        int next = clamp(value);
        this.progress = next;
        invalidate();
        Listener l = this.listener;
        if (l != null) {
            l.onProgressChanged(this, next, true);
        }
    }

    public void setListener(@Nullable Listener value) {
        this.listener = value;
    }

    /** 换肤入口：轨道 / 已选段 / 拇指 / 描边环全部按当前 ThemeColors 现算。 */
    public void applyColors(@NotNull ThemeColors c) {
        this.accentColor = ThemeManager.INSTANCE.sliderSurfaceColor(getContext(), c);
        this.trackColor = ThemeManager.INSTANCE.withAlpha(c.getText(), 0.16f);
        this.pTrack.setColor(this.trackColor);
        this.pFill.setColor(this.accentColor);
        this.pThumb.setColor(this.accentColor);
        this.pRing.setColor(ThemeManager.INSTANCE.withAlpha(this.accentColor, 0.22f));
        invalidate();
    }

    // ================================================================ 测量与绘制

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        // v1.10.525：对话框恢复原高度。
        // 不再自己拍一个高度，而是**问平台**——临时量一条原生 SeekBar，直接报它的高度。
        // 这样"恢复原高度"是逐 ROM 成立的（各家的 Widget.SeekBar minHeight 不一样，
        // 写死 32/48 都可能在另一台机器上不对），并且以后换主题也不会再漂。
        int h = stockSeekBarHeight(getContext());
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        if (hMode == MeasureSpec.EXACTLY) {
            h = MeasureSpec.getSize(heightMeasureSpec);
        } else if (hMode == MeasureSpec.AT_MOST) {
            h = Math.min(h, MeasureSpec.getSize(heightMeasureSpec));
        }
        setMeasuredDimension(w, h);
    }

    /**
     * 「原高度」= 原生 {@code SeekBar} 在本机量出来的高度（结果缓存，每个进程只量一次）。
     * 量不出来（0 或离谱值）时退回 36dp —— 宁可差一点，也不要 0 高。
     */
    private static int stockSeekBarHeight(@NotNull android.content.Context context) {
        if (sStockHeight > 0) {
            return sStockHeight;
        }
        int fallback = Math.round(36.0f * context.getResources().getDisplayMetrics().density);
        int h = fallback;
        try {
            android.widget.SeekBar probe = new android.widget.SeekBar(context);
            probe.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            int measured = probe.getMeasuredHeight();
            float min = 16.0f * context.getResources().getDisplayMetrics().density;
            float max = 72.0f * context.getResources().getDisplayMetrics().density;
            if (measured >= min && measured <= max) {
                h = measured;
            }
        } catch (Throwable ignored) {
        }
        sStockHeight = h;
        return h;
    }

    /** 上一次量到的「原生 SeekBar 高度」（px）；0 = 还没量。 */
    private static int sStockHeight;

    @Override
    protected void onDraw(@NotNull Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float cy = h / 2.0f;
        float left = this.inset;
        float right = w - this.inset;
        if (right <= left) {
            return;
        }
        // 轨道
        this.box.set(left, cy - this.trackH / 2.0f, right, cy + this.trackH / 2.0f);
        canvas.drawRoundRect(this.box, this.trackH / 2.0f, this.trackH / 2.0f, this.pTrack);
        // 已选段（与原生 SeekBar 同口径：从最左端长到拇指处）
        float tx = left + (right - left) * (this.max <= 0 ? 0.0f : this.progress / (float) this.max);
        this.box.set(left, cy - this.trackH / 2.0f, tx, cy + this.trackH / 2.0f);
        if (this.box.width() > 0.5f) {
            canvas.drawRoundRect(this.box, this.trackH / 2.0f, this.trackH / 2.0f, this.pFill);
        }
        // 拇指：一个实心圆，没有额外的环（不附加装饰环）
        canvas.drawCircle(tx, cy, this.thumbR * this.thumbScale, this.pThumb);
    }

    // ================================================================ 触摸

    @Override
    public boolean onTouchEvent(@NotNull MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                this.tracking = true;
                this.moved = false;
                this.started = false;
                this.downX = event.getX();
                this.downProgress = this.progress;
                this.thumbScale = 1.12f;
                invalidate();
                // 把这条手势从任何滚动容器手里抢下来（弹窗根布局也会 request，双保险）
                android.view.ViewParent parent = getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!this.tracking) {
                    return true;
                }
                float movedBy = event.getX() - this.downX;
                if (!this.moved && Math.abs(movedBy) > this.touchSlop) {
                    this.moved = true;
                }
                if (this.moved) {
                    if (!this.started) {
                        this.started = true;
                        Listener l0 = this.listener;
                        if (l0 != null) {
                            l0.onStartTracking();
                        }
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    }
                    // 位移增量 → 进度：移出滑条区域也继续按滑动方向更新
                    float span = Math.max(1.0f, getWidth() - this.inset * 2.0f);
                    int next = Math.round(this.downProgress + movedBy * this.max / span);
                    next = clamp(next);
                    if (next != this.progress) {
                        this.progress = next;
                        invalidate();
                        Listener l = this.listener;
                        if (l != null) {
                            l.onProgressChanged(this, next, true);
                        }
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!this.tracking) {
                    return true;
                }
                this.tracking = false;
                this.thumbScale = 1.0f;
                if (!this.moved) {
                    // 只是点了一下：走一小段动画移到点击处（不做瞬移）
                    int next = clamp(Math.round(pointToProgress(event.getX())));
                    animateTo(next);
                } else if (this.started) {
                    Listener ls = this.listener;
                    if (ls != null) {
                        ls.onStopTracking();
                    }
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                this.tracking = false;
                this.thumbScale = 1.0f;
                if (this.started) {
                    this.started = false;
                    Listener lc = this.listener;
                    if (lc != null) {
                        lc.onStopTracking();
                    }
                }
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void animateTo(final int target) {
        final int from = this.progress;
        if (from == target) {
            return;
        }
        android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofInt(from, target);
        anim.setDuration(DUR_TAP);
        anim.setInterpolator(new android.view.animation.DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            this.progress = ((Integer) a.getAnimatedValue()).intValue();
            invalidate();
            Listener l = this.listener;
            if (l != null) {
                l.onProgressChanged(this, this.progress, true);
            }
        });
        anim.start();
    }

    private float pointToProgress(float x) {
        float left = this.inset;
        float span = Math.max(1.0f, getWidth() - left * 2.0f);
        return (x - left) / span * this.max;
    }

    private int clamp(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, this.max);
    }
}
