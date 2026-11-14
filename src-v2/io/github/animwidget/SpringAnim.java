package io.github.animwidget;

import android.animation.TimeInterpolator;
import android.view.View;

import org.jetbrains.annotations.NotNull;

/**
 * v1.10.265：**阻尼弹簧**（可过冲、可回弹），给「弹出感」用。
 *
 * <h3>为什么不用系统那几条缓动曲线</h3>
 * 减速曲线（decelerate / EASE_STD）从起点单调逼近终点，**永远不会有"越过再回来"**，
 * 所以看起来只是"快—慢"，没有质量感。真正的"弹"需要一个二阶系统：
 *
 * <pre>
 *   ω₀ = √(k/m)                      自然频率（刚度/质量）
 *   ζ  = c / (2√(km))                阻尼比：&lt;1 会过冲，=1 刚好不冲过头，&gt;1 迟钝
 *   x(t) = 1 − e^(−ζω₀t) · [cos(ω_d t) + (ζω₀/ω_d)·sin(ω_d t)]      ω_d = ω₀√(1−ζ²)
 * </pre>
 *
 * 这是**欠阻尼阶跃响应**的解析解，所以不需要逐帧积分、也不依赖 androidx.dynamicanimation，
 * 可以塞进任意 {@link TimeInterpolator}（api 14+ 就有）。
 * 时长的取法：包络衰减到 0.5% 所需的时间 `t = −ln(0.005)/(ζω₀)`（再夹到 120~520ms）。
 *
 * 参考（参数都是公开文档里的原值）：
 * <ul>
 *   <li>Android {@code SpringForce}：STIFFNESS_HIGH=10000 / MEDIUM=1500 / LOW=200 /
 *       VERY_LOW=50；DAMPING_RATIO_NO_BOUNCY=1.0 / LOW_BOUNCY=0.75 / MEDIUM_BOUNCY=0.5 /
 *       HIGH_BOUNCY=0.2</li>
 *   <li>Material 3 Expressive spring tokens（spatial / effects 各四档）</li>
 *   <li>Framer Motion default spring：stiffness 100 / damping 10 / mass 1</li>
 *   <li>react-spring 预设：default(170/26)、wobbly(180/12)、stiff(210/20)、slow(280/60)</li>
 * </ul>
 */
public final class SpringAnim {

    /** 一档弹簧的配方。 */
    public static final class Spec {
        public final float stiffness;
        public final float damping;
        public final float mass;

        public Spec(float stiffness, float damping, float mass) {
            this.stiffness = stiffness;
            this.damping = damping;
            this.mass = mass;
        }

        public float omega0() {
            return (float) Math.sqrt(stiffness / Math.max(mass, 0.01f));
        }

        public float zeta() {
            return (float) (damping / (2.0 * Math.sqrt(stiffness * Math.max(mass, 0.01f))));
        }

        /** 包络衰减到 0.5% 的时间（= 视觉上"停了"的时刻），夹在 120~520ms。 */
        public long durationMs() {
            float z = Math.max(zeta(), 0.05f);
            float t = (float) (-Math.log(0.005) / (z * omega0()));
            long ms = Math.round(t * 1000.0f);
            return Math.max(120L, Math.min(520L, ms));
        }

        /** 归一化位置（可以 &gt;1 = 过冲，也可能 &lt;0 = 回弹到起点这一侧）。 */
        public float position(float seconds) {
            float z = zeta();
            float w0 = omega0();
            if (z >= 0.999f) {
                // 临界/过阻尼：没有振荡项，避免 ω_d 接近 0 时除零
                return (float) (1.0 - Math.exp(-w0 * seconds) * (1.0 + w0 * seconds));
            }
            float wd = (float) (w0 * Math.sqrt(1.0f - z * z));
            double e = Math.exp(-z * w0 * seconds);
            return (float) (1.0 - e * (Math.cos(wd * seconds) + (z * w0 / wd) * Math.sin(wd * seconds)));
        }

        @NotNull
        public TimeInterpolator interpolator() {
            final long dur = durationMs();
            return new TimeInterpolator() {
                @Override
                public float getInterpolation(float input) {
                    return position(input * dur / 1000.0f);
                }
            };
        }
    }

    // ---------------------------------------------------------------- 预设
    /**
     * 弹出：k=700 c=30 m=1 → ω₀≈26.5、ζ≈0.567、**过冲 ≈12%**、时长 ≈353ms。
     * 落在 1 之后回弹一次，肉眼就是"弹了一下"；12% 是能明显看见但不廉价的量
     * （Android HIGH_BOUNCY 是 0.2≈52%，那是玩具感；MEDIUM_BOUNCY 0.5≈16%）。
     */
    public static final Spec POP = new Spec(700.0f, 30.0f, 1.0f);
    /**
     * 收起：k=900 c=49 m=1 → ω₀=30、ζ≈0.817、过冲 ≈1%（几乎不回弹）、时长 ≈216ms。
     * 用户要求"关闭更利落"：保留质量感（加速→刹车）但不来回晃。
     */
    public static final Spec CLOSE = new Spec(900.0f, 49.0f, 1.0f);

    private SpringAnim() {
    }

    /**
     * 一次把「缩放 + 竖直位移 + 透明」跑在同一条弹簧上（同一个插值器 → 三者永远同步）。
     *
     * @param fromScale 起始缩放（典型 0.86，配 POP 会轻微过冲到 ~1.03 再落回）
     * @param fromDy    起始竖直偏移（px，正数=从下方弹上来）
     * @param alphaMs   透明度单独用一条**不过冲**的短曲线（过冲的 alpha 会闪）
     */
    public static void popIn(@NotNull final View v, final float fromScale, final float fromDy,
                             final long alphaMs, @NotNull final Spec spec) {
        final long dur = spec.durationMs();
        final TimeInterpolator spring = spec.interpolator();
        v.setAlpha(0.0f);
        v.setScaleX(fromScale);
        v.setScaleY(fromScale);
        v.setTranslationY(fromDy);
        v.animate().cancel();
        final android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
        anim.setDuration(dur);
        anim.setInterpolator(new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                return input;   // 真身曲线在下面按帧算，这里保持线性时间
            }
        });
        anim.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(android.animation.ValueAnimator a) {
                float t = ((Float) a.getAnimatedValue()).floatValue();
                float x = spring.getInterpolation(t);
                float s = fromScale + (1.0f - fromScale) * x;
                v.setScaleX(s);
                v.setScaleY(s);
                v.setTranslationY(fromDy * (1.0f - x));
                // 透明度单独走：前 alphaMs 内线性到 1，过冲不会让画面闪
                float a2 = Math.min(1.0f, t * dur / Math.max(alphaMs, 1L));
                v.setAlpha(a2);
            }
        });
        anim.start();
    }

    /** 收起：弹簧压到 fromScale、往下掉 fromDy、淡出；结束回调里做 detach。 */
    public static void popOut(@NotNull final View v, final float toScale, final float toDy,
                              final Runnable end, @NotNull final Spec spec) {
        final long dur = Math.min(spec.durationMs(), 260L);
        final TimeInterpolator spring = spec.interpolator();
        v.animate().cancel();
        final android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
        anim.setDuration(dur);
        anim.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(android.animation.ValueAnimator a) {
                float t = ((Float) a.getAnimatedValue()).floatValue();
                float x = spring.getInterpolation(t);
                float s = 1.0f + (toScale - 1.0f) * x;
                v.setScaleX(s);
                v.setScaleY(s);
                v.setTranslationY(toDy * x);
                v.setAlpha(Math.max(0.0f, 1.0f - t * 1.4f));
            }
        });
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            private boolean done;

            @Override
            public void onAnimationEnd(android.animation.Animator a) {
                if (done) {
                    return;
                }
                done = true;
                if (end != null) {
                    end.run();
                }
            }
        });
        anim.start();
    }
}
