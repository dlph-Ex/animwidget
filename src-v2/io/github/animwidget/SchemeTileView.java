package io.github.animwidget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import org.jetbrains.annotations.Nullable;

/**
 * 自定义配色方案的小图标（v1.10.47，v1.10.47.1 重做）。
 *
 * 形状：一个圆分成四块（左上=主色、右上=辅助色、左下=第三色、右下=滑块色），外圈齿轮按用户要求不要。
 *
 * v1.10.47.1（用户看图反馈）：
 *   • 图标整体缩小 4dp，选中外圈不再被上下裁掉（原来半径贴着 View 边，环被切了一刀）；
 *   • 勾号按图 2 重做：中央一个「圆角方」（超椭圆）包住勾，底色 = 主色与辅助色的中间值；
 *   • 占位图标按首图重做：柔和的竖向渐变 + 「⊕」符号（圈 + 加号），不再是一个死板的实心圆 + 加号；
 *   • 勾选/取消都有动画：外圈淡入放大、圆角方从 45% 弹到 100%（过冲）；取消时倒放。
 */
final class SchemeTileView extends View {

    static final int KIND_SCHEME = 0;
    static final int KIND_ADD = 1;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final RectF box = new RectF();
    private final Path path = new Path();
    private final float density;

    private int primary;
    private int secondary;
    private int tertiary;
    private int quaternary;
    private int ringColor;
    private int kind = KIND_SCHEME;
    /** 选中进度 0~1（动画中可短暂 >1，用来做过冲） */
    private float active;
    private float holeFraction;
    private float holeAlpha = 1.0f;
    /** v1.10.92：当前方案的配色/对比度有未保存改动 → 勾选标记画成半透明。 */
    private boolean dirty;

    @Nullable
    private ValueAnimator activeAnimator;
    @Nullable
    private ValueAnimator holeAnimator;

    SchemeTileView(Context context) {
        super(context);
        this.density = context.getResources().getDisplayMetrics().density;
        this.stroke.setStyle(Paint.Style.STROKE);
        this.stroke.setStrokeWidth(2.2f * this.density);
        this.stroke.setStrokeCap(Paint.Cap.ROUND);
        this.stroke.setStrokeJoin(Paint.Join.ROUND);
        this.ring.setStyle(Paint.Style.STROKE);
        this.ring.setStrokeWidth(2.0f * this.density);
        setClickable(true);
        setFocusable(true);
    }

    void setQuadrants(int primary, int secondary, int tertiary, int quaternary) {
        this.primary = primary;
        this.secondary = secondary;
        this.tertiary = tertiary;
        this.quaternary = quaternary;
        invalidate();
    }

    void setKind(int kind) {
        this.kind = kind;
        invalidate();
    }

    void setRingColor(int color) {
        this.ringColor = color;
        invalidate();
    }

    /** v1.10.92：把勾选标记画成半透明，表示「这个方案当前有未保存的改动」。 */
    void setDirty(boolean dirty) {
        if (this.dirty == dirty) {
            return;
        }
        this.dirty = dirty;
        invalidate();
    }

    /** 不做动画地设置选中态（列表重建时用）。 */
    void setActiveVisual(boolean on) {
        cancelActiveAnim();
        this.active = on ? 1.0f : 0.0f;
        invalidate();
    }

    boolean isActiveVisual() {
        return this.active >= 0.5f;
    }

    /** 勾选/取消勾选动画（点选、删除后跳转都用它，效果一致）。 */
    void animateActive(boolean on) {
        cancelActiveAnim();
        if (on && this.active <= 0.0f) {
            this.active = 0.0f;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(this.active, on ? 1.0f : 0.0f);
        animator.setDuration(on ? 280L : 180L);
        animator.setInterpolator(on
                ? new OvershootInterpolator(1.15f)
                : new DecelerateInterpolator(1.6f));
        animator.addUpdateListener((a) -> {
            this.active = ((Float) a.getAnimatedValue()).floatValue();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                SchemeTileView.this.active = on ? 1.0f : 0.0f;
                SchemeTileView.this.activeAnimator = null;
                invalidate();
            }
        });
        this.activeAnimator = animator;
        animator.start();
    }

    private void cancelActiveAnim() {
        if (this.activeAnimator != null) {
            try {
                this.activeAnimator.cancel();
            } catch (Throwable ignored) {
            }
            this.activeAnimator = null;
        }
    }

    /** 黑色领域：fraction 0→1 = 半径 0→铺满整个图标。 */
    void setHole(float fraction) {
        this.holeFraction = fraction;
        invalidate();
    }

    void setHoleAlpha(float alpha) {
        this.holeAlpha = alpha;
        invalidate();
    }

    /**
     * 删除用的「吞噬」动画：黑色领域从中央长大到盖满图标（240ms）→
     * 停一拍（60ms）→ 黑色领域自己渐隐（150ms）→ 回调。
     */
    void swallow(@Nullable final Runnable onDone) {
        cancelHole();
        final ValueAnimator grow = ValueAnimator.ofFloat(0.0f, 1.0f);
        grow.setDuration(240L);
        grow.setInterpolator(new AccelerateDecelerateInterpolator());
        grow.addUpdateListener((a) -> setHole(((Float) a.getAnimatedValue()).floatValue()));
        grow.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                this.cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (this.cancelled) {
                    return;
                }
                ValueAnimator fade = ValueAnimator.ofFloat(1.0f, 0.0f);
                fade.setDuration(150L);
                fade.setStartDelay(60L);
                fade.setInterpolator(new DecelerateInterpolator());
                fade.addUpdateListener((a) -> setHoleAlpha(((Float) a.getAnimatedValue()).floatValue()));
                fade.addListener(new AnimatorListenerAdapter() {
                    private boolean innerCancelled;

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        this.innerCancelled = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!this.innerCancelled && onDone != null) {
                            onDone.run();
                        }
                    }
                });
                SchemeTileView.this.holeAnimator = fade;
                fade.start();
            }
        });
        this.holeAnimator = grow;
        grow.start();
    }

    void cancelHole() {
        if (this.holeAnimator != null) {
            try {
                this.holeAnimator.cancel();
            } catch (Throwable ignored) {
            }
            this.holeAnimator = null;
        }
        this.holeFraction = 0.0f;
        this.holeAlpha = 1.0f;
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelHole();
        cancelActiveAnim();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        // v1.10.371（用户反馈「放大时边框被截断」）：外圈画在 radius + 2dp 处，
        // 入场的 popIn 还会把整块放大到 1.05 —— 所以留白必须够：
        //   (min/2 − 5 + 2 + 1) × 1.05 < min/2   （半径 28dp 时 27.3 < 28 ✓）
        // 原来是 −4dp / +2.2dp / 1.09 倍，最外沿 28.3dp 顶出 View，被行的 clipChildren 切掉。
        float radius = Math.min(w, h) / 2.0f - 5.0f * this.density;
        if (radius <= 0.0f) {
            return;
        }
        float cx = w / 2.0f;
        float cy = h / 2.0f;
        this.oval.set(cx - radius, cy - radius, cx + radius, cy + radius);

        if (this.kind == KIND_ADD) {
            drawAdd(canvas, cx, cy, radius);
            return;
        }

        // v1.10.50：四象限重画 —— 原来四个 drawArc 首尾相接，抗锯齿在接缝处各占一半覆盖率，
        // 叠起来只剩 75%，于是四条半径线上各有一条「透出背景」的黑缝（用户看到的「歪七扭八」）。
        // 现在：① 整圆先用主色打底（缝底下也是主色，永远透不出背景）
        //       ② 另外三块扇形各自向两边多扫 1°，把接缝彻底盖住（位移 <1px，肉眼无感）
        this.fill.setShader(null);
        this.fill.setAlpha(255);
        this.fill.setColor(this.primary);
        canvas.drawCircle(cx, cy, radius, this.fill);
        float overlap = 1.0f;
        this.fill.setColor(this.secondary);
        canvas.drawArc(this.oval, 270.0f - overlap, 90.0f + overlap * 2.0f, true, this.fill);
        this.fill.setColor(this.tertiary);
        canvas.drawArc(this.oval, 90.0f - overlap, 90.0f + overlap * 2.0f, true, this.fill);
        this.fill.setColor(this.quaternary);
        canvas.drawArc(this.oval, 0.0f - overlap, 90.0f + overlap * 2.0f, true, this.fill);

        if (this.active > 0.01f) {
            drawActiveMarker(canvas, cx, cy, radius);
        }

        if (this.holeFraction > 0.0f && this.holeAlpha > 0.0f) {
            this.fill.setShader(null);
            this.fill.setColor(Color.BLACK);
            this.fill.setAlpha((int) (255 * Math.max(0.0f, Math.min(1.0f, this.holeAlpha))));
            canvas.drawCircle(cx, cy, radius * Math.min(1.0f, this.holeFraction) * 1.06f, this.fill);
            this.fill.setAlpha(255);
        }
    }

    /** 选中标记：中央圆角方（主色与辅助色的中间色）裹着一个勾；外圈同时淡入。 */
    private void drawActiveMarker(Canvas canvas, float cx, float cy, float radius) {
        float p = Math.min(1.15f, Math.max(0.0f, this.active));
        float alpha = Math.min(1.0f, p * 1.5f);
        // v1.10.92：有未保存改动时，勾选标记整体半透明（一眼能看出「还没存进方案」）
        int a = (int) (255 * alpha * (this.dirty ? 0.42f : 1.0f));

        // 外圈：位置固定，靠「由细变粗 + 淡入」出现（半径一放大就顶到 View 边，会被裁掉）
        this.ring.setColor(this.ringColor);
        this.ring.setAlpha(a);
        this.ring.setStrokeWidth((1.1f + 0.9f * Math.min(1.0f, p)) * this.density);
        canvas.drawCircle(cx, cy, radius + 2.0f * this.density, this.ring);
        this.ring.setStrokeWidth(2.0f * this.density);

        // 圆角方：从 45% 长到 100%，尺寸带一点过冲
        float scale = 0.45f + 0.55f * p;
        float side = radius * 1.10f * scale;
        float half = side / 2.0f;
        int mid = between(this.primary, this.secondary);
        this.fill.setShader(new LinearGradient(0.0f, cy - half, 0.0f, cy + half,
                lighten(mid, 1.06f), darken(mid, 0.93f), Shader.TileMode.CLAMP));
        this.fill.setAlpha(a);
        this.box.set(cx - half, cy - half, cx + half, cy + half);
        canvas.drawRoundRect(this.box, side * 0.32f, side * 0.32f, this.fill);
        this.fill.setShader(null);
        this.fill.setAlpha(255);

        // 勾
        this.stroke.setColor(checkInk(mid));
        this.stroke.setStrokeWidth(Math.max(1.8f * this.density, side * 0.135f));
        this.stroke.setAlpha(a);
        float s = side;
        this.path.reset();
        this.path.moveTo(cx - s * 0.21f, cy + s * 0.01f);
        this.path.lineTo(cx - s * 0.05f, cy + s * 0.17f);
        this.path.lineTo(cx + s * 0.22f, cy - s * 0.15f);
        canvas.drawPath(this.path, this.stroke);
        this.stroke.setAlpha(255);
    }

    /** 占位图标：柔和渐变底 + 「⊕」（圈 + 加号）。 */
    private void drawAdd(Canvas canvas, float cx, float cy, float radius) {
        int base = this.primary;
        this.fill.setShader(new LinearGradient(0.0f, cy - radius, 0.0f, cy + radius,
                lighten(base, 1.18f), darken(base, 0.88f), Shader.TileMode.CLAMP));
        this.fill.setAlpha(255);
        canvas.drawCircle(cx, cy, radius, this.fill);
        this.fill.setShader(null);

        // 顶部一圈柔光：让饼面不那么「平」
        this.ring.setColor(lighten(base, 1.5f));
        this.ring.setAlpha(70);
        this.ring.setStrokeWidth(1.2f * this.density);
        canvas.drawCircle(cx, cy - 0.5f * this.density, radius - 0.8f * this.density, this.ring);
        this.ring.setAlpha(255);

        int ink = ThemeManager.INSTANCE.onColor(base);
        this.stroke.setColor(ink);
        this.stroke.setAlpha(235);
        this.stroke.setStrokeWidth(2.0f * this.density);
        float circle = radius * 0.56f;
        canvas.drawCircle(cx, cy, circle, this.stroke);
        this.stroke.setStrokeWidth(2.2f * this.density);
        float arm = radius * 0.25f;
        canvas.drawLine(cx - arm, cy, cx + arm, cy, this.stroke);
        canvas.drawLine(cx, cy - arm, cx, cy + arm, this.stroke);
        this.stroke.setAlpha(255);
    }

    /** 勾的颜色：优先主色；主色和底色太接近时退回自动黑白。 */
    private int checkInk(int mid) {
        float a = luminance(this.primary);
        float b = luminance(mid);
        if (Math.abs(a - b) < 0.16f) {
            return ThemeManager.INSTANCE.onColor(mid);
        }
        return this.primary;
    }

    private static float luminance(int color) {
        return (((Color.red(color) * 299) + (Color.green(color) * 587) + (Color.blue(color) * 114)
        ) / 1000.0f) / 255.0f;
    }

    private static int lighten(int color, float factor) {
        return scale(color, factor);
    }

    private static int darken(int color, float factor) {
        return scale(color, factor);
    }

    private static int scale(int color, float factor) {
        int r = Math.min(255, Math.max(0, (int) (Color.red(color) * factor)));
        int g = Math.min(255, Math.max(0, (int) (Color.green(color) * factor)));
        int b = Math.min(255, Math.max(0, (int) (Color.blue(color) * factor)));
        return Color.rgb(r, g, b);
    }

    private static int between(int a, int b) {
        return Color.rgb(
                (Color.red(a) + Color.red(b)) / 2,
                (Color.green(a) + Color.green(b)) / 2,
                (Color.blue(a) + Color.blue(b)) / 2);
    }
}
