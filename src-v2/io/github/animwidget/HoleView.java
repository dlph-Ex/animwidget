package io.github.animwidget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.LinearInterpolator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v1.10.87：**黑洞吞噬动画**（删除历史导出用）。
 *
 * 时间线（共 640ms）：
 * <pre>
 *   0   → 110   中央**渐出**出现一个「点」（alpha 0→1，点本身也略微长大）
 *   110 → 340   点持续扩大，直到**完整吞没条目**（圆的直径/圆角一起长到条目尺寸）
 *   340 → 400   停一拍（此时条目已被盖满，调用方在这一刻把卡片置透明）
 *   400 → 640   黑洞自身渐隐（alpha 1→0）
 * </pre>
 *
 * ⚠️ 坐标系问题的终极解法：**不再算任何坐标**。
 * 本 View 直接 addView 到「条目自己的容器」（FrameLayout，与卡片同尺寸），铺 MATCH_PARENT，
 * 于是它天然就是 (0,0)-(w,h) —— 既不用 getLocationOnScreen，也不会踩到
 * {@code android.R.id.content} 的 inset padding / 滚动偏移 / 状态栏高度。
 * （v1.10.84 挂 LinearLayout 量出 0 高；v1.10.85/86 挂根布局算屏幕坐标，偏半个行高。）
 *
 * 颜色也不再用裸的「中性色」：见 {@link #holeColorFor(int, int)}。
 */
final class HoleView extends View {

    static final long FADE_IN = 110L;
    static final long GROW = 230L;
    static final long HOLD = 60L;
    static final long FADE_OUT = 240L;
    /** 盖满条目的时刻（调用方在这里把卡片视图置透明）。 */
    static final long COVERED_AT = FADE_IN + GROW;              // 340
    /** 整段动画结束时刻（调用方在这里真正删除 + 刷新列表）。 */
    static final long TOTAL = COVERED_AT + HOLD + FADE_OUT;     // 640

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;

    private int color;
    private float cornerRadius;
    private float dotDiameter;
    private float fraction;
    private float dotScale = 1.0f;
    private float holeAlpha;

    @Nullable
    private ValueAnimator animator;
    @Nullable
    private Runnable coveredAction;
    private boolean coveredFired;

    HoleView(@NotNull Context context, int holeColor) {
        super(context);
        this.color = holeColor;
        this.density = context.getResources().getDisplayMetrics().density;
        this.fill.setStyle(Paint.Style.FILL);
        setClickable(false);
        setFocusable(false);
        // 不吃触摸：黑洞是纯装饰，点按仍然落到下面的卡片上
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** 条目的圆角（黑洞长到最大时形状 = 条目本身）。 */
    void setCornerRadius(float radiusPx) {
        this.cornerRadius = Math.max(0.0f, radiusPx);
        this.dotDiameter = 0.0f;
        invalidate();
    }

    void cancel() {
        this.coveredAction = null;
        if (this.animator != null) {
            try {
                this.animator.cancel();
            } catch (Throwable ignored) {
            }
            this.animator = null;
        }
        this.fraction = 0.0f;
        this.holeAlpha = 0.0f;
        this.dotScale = 1.0f;
        this.coveredFired = false;
        invalidate();
    }

    /**
     * 播一次完整的吞噬动画。
     *
     * @param onCovered 黑洞**盖满条目**的瞬间（340ms）—— 调用方在这时把卡片置透明，
     *                  否则黑洞渐隐时会露出「完好无损的卡片」，最后再突然消失，很假。
     * @param onEnd     动画结束（640ms）—— 调用方在这时真正删除数据 + 重建列表。
     */
    void play(@Nullable final Runnable onCovered, @Nullable final Runnable onEnd) {
        cancel();
        this.coveredAction = onCovered;
        final ValueAnimator anim = ValueAnimator.ofFloat(0.0f, 1.0f);
        anim.setDuration(TOTAL);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float v = ((Float) animation.getAnimatedValue()).floatValue();
                applyTimeline((long) (v * TOTAL));
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
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
                HoleView.this.animator = null;
                HoleView.this.fraction = 1.0f;
                HoleView.this.holeAlpha = 0.0f;
                HoleView.this.invalidate();
                if (onEnd != null) {
                    onEnd.run();
                }
            }
        });
        this.animator = anim;
        anim.start();
        invalidate();
    }

    /**
     * 时间线 → 视觉参数（纯函数，方便靠日志复核）。
     *
     * v1.10.88：所有段都走三次贝塞尔（{@link Ease}），关键帧处速度≈0 ——
     * 之前淡入是线性、长大是 smoothstep、淡出是平方，段与段之间速度会跳一下（折角）。
     */
    private void applyTimeline(long t) {
        float alpha;
        float p;
        if (t <= FADE_IN) {
            // ① 渐出：一个点从无到有（alpha 0→1），同时自身 62% → 100%
            float k = FADE_IN <= 0L ? 1.0f : (float) t / (float) FADE_IN;
            alpha = Ease.OUT.at(clamp01(k));
            p = 0.0f;
            this.dotScale = 0.62f + 0.38f * Ease.OUT.at(clamp01(k));
        } else if (t <= COVERED_AT) {
            // ② 持续扩大：点 → 完整条目（两端平滑，先快后慢）
            float k = (t - FADE_IN) / (float) GROW;
            alpha = 1.0f;
            p = Ease.IN_OUT.at(clamp01(k));
            this.dotScale = 1.0f;
        } else if (t <= COVERED_AT + HOLD) {
            alpha = 1.0f;
            p = 1.0f;
        } else {
            // ③ 渐隐：两端都慢，中间快（不是一上来就往下掉）
            float k = clamp01((t - COVERED_AT - HOLD) / (float) FADE_OUT);
            alpha = 1.0f - Ease.IN_OUT.at(k);
            p = 1.0f;
        }
        this.fraction = p;
        this.holeAlpha = alpha;
        invalidate();
        if (!this.coveredFired && t >= COVERED_AT) {
            this.coveredFired = true;
            Runnable action = this.coveredAction;
            this.coveredAction = null;
            if (action != null) {
                action.run();
            }
        }
    }

    @Override
    protected void onDraw(@NotNull Canvas canvas) {
        if (this.holeAlpha <= 0.002f) {
            return;
        }
        float w = getWidth();
        float h = getHeight();
        if (w <= 0.0f || h <= 0.0f) {
            return;
        }
        if (this.dotDiameter <= 0.0f) {
            float min = Math.min(w, h);
            // 「点」的直径：行高的三分之一左右，并夹在 12~26dp（短条目也不会小成看不见）
            float d = min * 0.34f;
            float lo = 12.0f * this.density;
            float hi = 26.0f * this.density;
            if (d < lo) {
                d = lo;
            }
            if (d > hi) {
                d = hi;
            }
            if (d > min) {
                d = min;
            }
            this.dotDiameter = d;
        }
        float p = clamp01(this.fraction);
        float d0 = this.dotDiameter * this.dotScale;
        if (d0 > min2(w, h)) {
            d0 = min2(w, h);
        }
        // 直径/圆角同时从「点」长到「条目」：中途是宽圆角胶囊，视觉上像黑洞在把卡片吸进去
        float dw = d0 + (w - d0) * p;
        float dh = d0 + (h - d0) * p;
        float r = (d0 / 2.0f) + (this.cornerRadius - d0 / 2.0f) * p;
        float limit = Math.min(dw, dh) / 2.0f;
        if (r > limit) {
            r = limit;
        }
        if (r < 0.0f) {
            r = 0.0f;
        }
        float cx = w / 2.0f;
        float cy = h / 2.0f;
        this.rect.set(cx - dw / 2.0f, cy - dh / 2.0f, cx + dw / 2.0f, cy + dh / 2.0f);
        this.fill.setColor(this.color);
        this.fill.setAlpha((int) (255.0f * clamp01(this.holeAlpha)));
        canvas.drawRoundRect(this.rect, r, r, this.fill);
        this.fill.setAlpha(255);
    }

    @Override
    protected void onDetachedFromWindow() {
        cancel();
        super.onDetachedFromWindow();
    }

    // ───────────────────────── 颜色：中性色 + 强制对比 ─────────────────────────

    /**
     * v1.10.87：**黑洞颜色 = 系统中性色，但强制和条目底色拉开对比度**。
     *
     * 之前的教训（用户原话：「你是没想到用户会自定义系统配色吗？条目背景色与中性色只是比较接近」）：
     * 默认深色配色里 表面=#1A1A1A、中性=#38383F，对比度只有 1.4；用户再自定义一下，
     * 两个颜色可以完全一样 —— 黑洞画上去等于没画。
     *
     * 规则：中性色与条目底色的对比度不足 {@code 3.0} 时，按条目底色的明暗**往反方向推**：
     *  • 亮底（相对亮度 ≥ 0.18）→ 把中性色往黑推，直到 3.0
     *  • 暗底 → 把中性色往白推，直到 3.0
     * 保持中性色的色相（不引入彩度），只改明度，所以看起来仍然是「那种灰」。
     */
    static int holeColorFor(int card, int neutral) {
        final double target = 3.0;
        int base = neutral | 0xFF000000;
        if (contrast(base, card) >= target) {
            return base;
        }
        boolean lightCard = relativeLuminance(card) >= 0.18;
        int tr = lightCard ? 0 : 255;
        int tg = tr;
        int tb = tr;
        int br = Color.red(base);
        int bg = Color.green(base);
        int bb = Color.blue(base);
        for (int step = 1; step <= 20; step++) {
            float t = step / 20.0f;
            int c = Color.rgb(Math.round(br + (tr - br) * t),
                    Math.round(bg + (tg - bg) * t),
                    Math.round(bb + (tb - bb) * t));
            if (contrast(c, card) >= target) {
                return c;
            }
        }
        return Color.rgb(tr, tg, tb);
    }

    /** WCAG 相对亮度（0~1）。 */
    static double relativeLuminance(int color) {
        return 0.2126 * linear(Color.red(color))
                + 0.7152 * linear(Color.green(color))
                + 0.0722 * linear(Color.blue(color));
    }

    private static double linear(int channel) {
        double c = channel / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    /** WCAG 对比度（1~21）。 */
    static double contrast(int a, int b) {
        double la = relativeLuminance(a);
        double lb = relativeLuminance(b);
        double hi = Math.max(la, lb);
        double lo = Math.min(la, lb);
        return (hi + 0.05) / (lo + 0.05);
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }

    private static float min2(float a, float b) {
        return a < b ? a : b;
    }

    @NotNull
    @Override
    public String toString() {
        return "HoleView";
    }
}
