package io.github.animwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v1.10.526：**形状格容器**——选中框由它自己画。
 *
 * <h3>525 为什么完全看不见</h3>
 * 那一版把框做成「另一个 View 浮在上面」，靠 `post()` 回调去读格子的 `getLeft()/getWidth()`。
 * 但 `buildShapeGrid()` 刚 `removeAllViews()` + `addView()`，**新格子此时还没测量**，
 * `post` 的回调抢在布局之前跑 → 读到的矩形是 (0,0,0,0) → 框被判定为空，一帧都没画出来。
 * 而且这类"等布局"的时序问题在真机上永远比模拟器更早暴露。
 *
 * <h3>现在怎么做</h3>
 * 不做成独立 View，而是把 {@code ll_shapes} 包进这个 FrameLayout 子类：
 * <ul>
 *   <li>目标格子的矩形在 {@link #dispatchDraw} 里**现场算**（子 View 此刻一定已经布局完），
 *       不存在任何"等一帧"的问题，滚动/重建/换字体都不需要重新同步；</li>
 *   <li>`super.dispatchDraw()` 之后画 = 一定压在格子上面；</li>
 *   <li>动画只持有「起点矩形 + 进度 t」，终点每帧现取 —— 布局变了也依然落在正确的格子上。</li>
 * </ul>
 *
 * <h3>外观与动画</h3>
 * 描边 3dp + 13% 强调底 + 12dp 圆角（改版前的原样）；切换时框从旧格子**点到点**移到新格子，
 * 280ms、`PathInterpolator(0.10, 0.75, 0.25, 1.0)` —— **由快到慢**（ease-out）；
 * 动画途中再选别的格子，从**当前显示中的矩形**接着走，不跳回起点。
 */
public final class ShapeGridHost extends FrameLayout {

    private static final float RADIUS_DP = 12.0f;
    private static final float STROKE_DP = 3.0f;
    private static final float FILL_ALPHA = 0.13f;
    private static final long DUR_MOVE = 280L;

    private final Paint pFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF from = new RectF();
    private final RectF live = new RectF();
    private final RectF target = new RectF();
    private final float dp;

    @Nullable
    private ShapeKind chosen;
    @Nullable
    private android.animation.ValueAnimator anim;
    /** 0..1：动画进度；1 = 已落位。 */
    private float t = 1.0f;
    private boolean hasFrame;

    public ShapeGridHost(@NotNull Context context) {
        this(context, null);
    }

    public ShapeGridHost(@NotNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShapeGridHost(@NotNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.dp = context.getResources().getDisplayMetrics().density;
        this.pFill.setStyle(Paint.Style.FILL);
        this.pStroke.setStyle(Paint.Style.STROKE);
        this.pStroke.setStrokeWidth(STROKE_DP * this.dp);
        setWillNotDraw(false);
        setAccent(ThemeManager.INSTANCE.colors(context).getPrimary());
    }

    public void setAccent(int color) {
        this.pFill.setColor(ThemeManager.INSTANCE.withAlpha(color, FILL_ALPHA));
        this.pStroke.setColor(color);
        invalidate();
    }

    /**
     * 指定「选中框」应该落在哪个形状上。
     *
     * @param shape   null = 不画框
     * @param animate true = 从当前位置平滑移过去；false = 直接落位
     */
    public void setChosen(@Nullable ShapeKind shape, boolean animate) {
        this.chosen = shape;
        if (shape == null) {
            cancelAnim();
            this.hasFrame = false;
            this.t = 1.0f;
            invalidate();
            return;
        }
        if (!animate || !this.hasFrame) {
            cancelAnim();
            this.t = 1.0f;
            invalidate();     // 落位矩形在 dispatchDraw 里现算
            return;
        }
        // 起点 = 当前显示中的矩形（可能是上一段动画的半途值）
        captureLive();
        this.from.set(this.live);
        cancelAnim();
        this.t = 0.0f;
        android.animation.ValueAnimator a = android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
        a.setDuration(DUR_MOVE);
        // v1.10.528：**由快到慢**（ease-out）。
        // 现在换成强减速曲线：起步就把大部分距离走掉，末段慢慢贴住目标。
        a.setInterpolator(new android.view.animation.PathInterpolator(0.10f, 0.75f, 0.25f, 1.0f));
        a.addUpdateListener(animation -> {
            this.t = ((Float) animation.getAnimatedValue()).floatValue();
            invalidate();
        });
        this.anim = a;
        a.start();
    }

    private void cancelAnim() {
        android.animation.ValueAnimator running = this.anim;
        if (running != null) {
            this.anim = null;
            running.cancel();
        }
    }

    /**
     * 找到当前选中形状对应的格子。
     *
     * <p>v1.10.527（真机日志 `A05 形状选中框 形状=ROUNDED 格子=未找到`）：525/526 这里
     * 按「本容器 > 列 > chip」两层写死，但实际结构是三层的 ——
     * {@code ShapeGridHost > ll_shapes(LinearLayout) > 列 > chip}：本容器的直接子节点
     * 只有一个 {@code ll_shapes}，于是"列"其实是容器本身，"chip"其实是列，永远匹配不到。
     * 现在改成**递归**找 {@link ShapeChipView}：结构再怎么调都不会再错。</p>
     */
    @Nullable
    public View findChosenChip() {
        ShapeKind want = this.chosen;
        if (want == null) {
            return null;
        }
        return findChip(this, want, 0);
    }

    @Nullable
    private static View findChip(@Nullable View node, @NotNull ShapeKind want, int depth) {
        if (node == null || depth > 6) {
            return null;
        }
        if (node instanceof ShapeChipView && ((ShapeChipView) node).getShape() == want) {
            return node;
        }
        if (!(node instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) node;
        for (int i = 0; i < group.getChildCount(); i++) {
            View found = findChip(group.getChildAt(i), want, depth + 1);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** 目标格子在**本容器坐标系**里的矩形；找不到返回 false。 */
    private boolean computeTarget(@NotNull RectF out) {
        View chip = findChosenChip();
        if (chip == null || chip.getWidth() <= 0 || chip.getHeight() <= 0) {
            return false;
        }
        // chip → 列 → 本容器：逐层累加 left/top（两者的父链最多两层）
        float x = chip.getLeft();
        float y = chip.getTop();
        android.view.ViewParent p = chip.getParent();
        int hop = 0;
        while (p instanceof View && p != this && hop < 4) {
            View v = (View) p;
            x += v.getLeft();
            y += v.getTop();
            p = v.getParent();
            hop++;
        }
        out.set(x, y, x + chip.getWidth(), y + chip.getHeight());
        return true;
    }

    /** 当前显示中的矩形（动画途中也能拿到准确值）。 */
    private void captureLive() {
        if (computeTarget(this.target)) {
            if (this.t >= 1.0f || !this.hasFrame) {
                this.live.set(this.target);
            } else {
                this.live.set(
                        this.from.left + (this.target.left - this.from.left) * this.t,
                        this.from.top + (this.target.top - this.from.top) * this.t,
                        this.from.right + (this.target.right - this.from.right) * this.t,
                        this.from.bottom + (this.target.bottom - this.from.bottom) * this.t);
            }
        }
    }

    @Override
    protected void dispatchDraw(@NotNull Canvas canvas) {
        super.dispatchDraw(canvas);
        if (this.chosen == null || !computeTarget(this.target)) {
            this.hasFrame = false;
            return;
        }
        if (this.t >= 1.0f || !this.hasFrame) {
            this.live.set(this.target);
        } else {
            this.live.set(
                    this.from.left + (this.target.left - this.from.left) * this.t,
                    this.from.top + (this.target.top - this.from.top) * this.t,
                    this.from.right + (this.target.right - this.from.right) * this.t,
                    this.from.bottom + (this.target.bottom - this.from.bottom) * this.t);
        }
        this.hasFrame = true;
        float r = RADIUS_DP * this.dp;
        canvas.drawRoundRect(this.live, r, r, this.pFill);
        RectF stroke = new RectF(this.live);
        float half = STROKE_DP * this.dp / 2.0f;
        stroke.inset(half, half);
        canvas.drawRoundRect(stroke, Math.max(0.0f, r - half), Math.max(0.0f, r - half), this.pStroke);
    }
}
