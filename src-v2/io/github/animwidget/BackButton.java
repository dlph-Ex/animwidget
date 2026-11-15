package io.github.animwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v1.10.523（需求）：设置页左上角**返回按钮**（自绘图标 + 按压动效）。
 *
 * <h3>参考与取舍（详细清单见本地 docs/SETTINGS-BACK-BUTTON.md，18 条）</h3>
 * 采用了其中 3 条：
 * <ol>
 *   <li><b>Material Symbols {@code arrow_back} 的字形</b>（参考 #13）：Android 上「返回」的母题
 *       是「横杆 + 45° 箭头」，不是 iOS 那种纯 chevron —— 这个应用是 Android 单平台，
 *       跟系统返回手势的语义对齐比"跨平台统一"重要；</li>
 *   <li><b>Material 3 Icon button 的尺寸口径</b>（参考 #1/#3/#4）：视觉 40dp、**触摸目标 48dp**
 *       （控件本身就是 48dp 见方，圆底 40dp 居中画），满足"目标 ≥48dp / 与相邻元素 ≥8dp"；</li>
 *   <li><b>M3 的 state layer 与按压反馈</b>（参考 #2）：按下加一层强调色 18% 的叠加 + 缩到 0.90，
 *       松手弹回；不用系统 ripple（它会把圆形底裁成方波），自己画。</li>
 * </ol>
 *
 * <h3>自绘的部分</h3>
 * <ul>
 *   <li>线宽 2.2dp、圆头圆角、箭头开口 72°（比 Material 的 45° 略收，48dp 里更利落）；</li>
 *   <li>按下时箭头整体**左移 1.2dp**（指向动作方向），与缩放同时进行；</li>
 *   <li>「取消」交互：手指按下后移出控件 12dp 以外再松手 = 不触发点击（标准按钮语义），
 *       移回范围内又能继续点击；</li>
 *   <li>点击时给一次 {@link HapticFeedbackConstants#VIRTUAL_KEY} 触感。</li>
 * </ul>
 */
public final class BackButton extends View {

    /** 视觉圆底直径（dp）。 */
    private static final float DISC_DP = 40.0f;
    /** 控件边长（dp）= 触摸目标，Material 要求 ≥48dp。 */
    private static final float BOX_DP = 48.0f;
    /** 箭头线宽（dp）。 */
    private static final float STROKE_DP = 2.2f;
    /** 箭头半高（dp）：决定开口角度。 */
    private static final float HEAD_DP = 5.4f;
    /** 箭头横杆半长（dp）。 */
    private static final float SHAFT_DP = 5.6f;
    /** 按下时的位移（dp，负 = 向左）。 */
    private static final float NUDGE_DP = -1.2f;

    private final Paint pDisc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pRing = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pArrow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrow = new Path();
    private final float dp;
    private final int cancelSlop;

    private int accent = 0xFF22C55E;
    private float scale = 1.0f;
    private float nudge = 0.0f;
    private float layer = 0.0f;
    private boolean pressed;

    /**
     * v1.10.524（崩溃修复）：**必须提供 (Context, AttributeSet) 构造器**。
     *
     * <p>这一颗按钮是写在 {@code activity_settings.xml} 里的自定义 View，
     * {@code LayoutInflater} 是按 {@code (Context, AttributeSet)} 反射找构造器的；
     * 523 只给了单参构造 → 一进设置页就 {@code InflateException}（表现为一进页面就崩）。
     * 项目里其它自定义控件（IconTileView / GlyphView / ShapeChipView…）都是三个构造器齐全，
     * 这里补齐，并且允许在 XML 里指定 style。</p>
     */
    public BackButton(@NotNull Context context, @Nullable android.util.AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BackButton(@NotNull Context context, @Nullable android.util.AttributeSet attrs,
                      int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.dp = context.getResources().getDisplayMetrics().density;
        this.cancelSlop = ViewConfiguration.get(context).getScaledTouchSlop() * 2;
        this.pDisc.setStyle(Paint.Style.FILL);
        this.pRing.setStyle(Paint.Style.STROKE);
        this.pRing.setStrokeWidth(1.0f * this.dp);
        this.pArrow.setStyle(Paint.Style.STROKE);
        this.pArrow.setStrokeCap(Paint.Cap.ROUND);
        this.pArrow.setStrokeJoin(Paint.Join.ROUND);
        this.pArrow.setStrokeWidth(STROKE_DP * this.dp);
        setClickable(true);
        setFocusable(true);
        applyColors(ThemeManager.INSTANCE.colors(context));
    }

    public BackButton(@NotNull Context context) {
        this(context, null);
    }

    /** 换肤入口（ThemeManager.paint 会调；自己也在构造时调一次）。 */
    public void applyColors(@NotNull ThemeColors c) {
        this.accent = c.getPrimary();
        this.pArrow.setColor(this.accent);
        this.pRing.setColor(ThemeManager.INSTANCE.withAlpha(this.accent, 0.26f));
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int side = (int) (BOX_DP * this.dp);
        setMeasuredDimension(resolve(widthMeasureSpec, side), resolve(heightMeasureSpec, side));
    }

    private int resolve(int spec, int def) {
        int mode = MeasureSpec.getMode(spec);
        int size = MeasureSpec.getSize(spec);
        if (mode == MeasureSpec.EXACTLY) {
            return size;
        }
        if (mode == MeasureSpec.AT_MOST) {
            return Math.min(size, def);
        }
        return def;
    }

    @Override
    protected void onDraw(@NotNull Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2.0f;
        float cy = h / 2.0f;
        float r = DISC_DP * this.dp / 2.0f * this.scale;

        // 圆底：常态 = 强调色 14%；按下再叠 18%（state layer），所以按下去看得出"沉了一下"
        int base = ThemeManager.INSTANCE.withAlpha(this.accent, 0.14f + 0.18f * this.layer);
        this.pDisc.setColor(base);
        canvas.drawCircle(cx, cy, r, this.pDisc);
        // 细描边：浅色模式下把圆从白底上"托"出来，深色下不至于糊成一片
        canvas.drawCircle(cx, cy, r - 0.5f * this.dp, this.pRing);

        // 箭头：横杆 + 45° 头（Material Symbols arrow_back 的骨架，角度与长度按 48dp 重调）
        float ax = cx + this.nudge * this.dp;
        float ay = cy;
        float shaft = SHAFT_DP * this.dp * this.scale;
        float head = HEAD_DP * this.dp * this.scale;
        this.arrow.reset();
        this.arrow.moveTo(ax + shaft, ay);
        this.arrow.lineTo(ax - shaft, ay);
        this.arrow.moveTo(ax - shaft, ay);
        this.arrow.lineTo(ax - shaft + head * 0.82f, ay - head);
        this.arrow.moveTo(ax - shaft, ay);
        this.arrow.lineTo(ax - shaft + head * 0.82f, ay + head);
        canvas.drawPath(this.arrow, this.pArrow);
    }

    // ================================================================ 触摸

    @Override
    public boolean onTouchEvent(@NotNull MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                setPressedVisual(true);
                return true;
            case MotionEvent.ACTION_MOVE: {
                boolean outside = event.getX() < -this.cancelSlop
                        || event.getY() < -this.cancelSlop
                        || event.getX() > getWidth() + this.cancelSlop
                        || event.getY() > getHeight() + this.cancelSlop;
                // 只在状态**真的变了**时起动画：MOVE 每帧都来，不能每次都新建 ValueAnimator
                if (outside == this.pressed) {
                    setPressedVisual(!outside);
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                boolean hit = event.getX() >= 0 && event.getY() >= 0
                        && event.getX() <= getWidth() && event.getY() <= getHeight();
                boolean wasPressed = this.pressed;
                setPressedVisual(false);
                if (wasPressed && hit) {
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    performClick();
                }
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                setPressedVisual(false);
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    /** 按下态的唯一入口：只在变化时更新，动画从当前值接续。 */
    private void setPressedVisual(boolean down) {
        if (this.pressed == down && Math.abs(this.layer - (down ? 1.0f : 0.0f)) < 0.001f) {
            return;
        }
        this.pressed = down;
        animatePress(down);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /** 按下 / 松开的两段动效：缩放 + 左移 + state layer，全部只改浮点，不触发重排。 */
    private void animatePress(final boolean down) {
        float targetScale = down ? 0.90f : 1.0f;
        float targetNudge = down ? NUDGE_DP : 0.0f;
        float targetLayer = down ? 1.0f : 0.0f;
        if (Math.abs(this.scale - targetScale) < 0.001f
                && Math.abs(this.nudge - targetNudge) < 0.001f) {
            this.layer = targetLayer;
            invalidate();
            return;
        }
        android.animation.ValueAnimator anim =
                android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
        anim.setDuration(down ? 90L : 180L);
        anim.setInterpolator(down
                ? new android.view.animation.DecelerateInterpolator()
                : new android.view.animation.OvershootInterpolator(1.6f));
        final float fromScale = this.scale;
        final float fromNudge = this.nudge;
        final float fromLayer = this.layer;
        anim.addUpdateListener(a -> {
            float t = ((Float) a.getAnimatedValue()).floatValue();
            this.scale = fromScale + (targetScale - fromScale) * t;
            this.nudge = fromNudge + (targetNudge - fromNudge) * t;
            this.layer = fromLayer + (targetLayer - fromLayer) * t;
            invalidate();
        });
        anim.start();
    }
}
