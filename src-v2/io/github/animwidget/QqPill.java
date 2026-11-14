package io.github.animwidget;

import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.View;

import java.util.WeakHashMap;

/**
 * v1.10.362：**胶囊按压的「Q 弹」反馈**（开发者页「关于 / 复制 QQ」那枚合并胶囊用）。
 *
 * <h3>为什么 v1.10.359~361 三次「倾斜没出现」</h3>
 * 不是弹簧没跑，是**没有东西可看**，两个独立原因同时存在：
 * <ol>
 *   <li>转的两格是 {@code ghost_text}（背景 null），胶囊的实色在**父容器**上。
 *       父容器不动，那 12° 只能带动几个汉字 —— 肉眼看就是「什么也没发生」。</li>
 *   <li>相机距离取 2600dp。透视宽度差 ≈ 半高/相机距离：半高 20dp / 2600dp ≈ 0.8%，
 *       亚像素级。这一项就算有实色也看不出来。</li>
 * </ol>
 * 现在：每一格自带实色（上格只圆上面两角、下格只圆下面两角），相机距离取**自身高度的
 * 2.8 倍**（≈112dp），透视宽度差 7% 量级，看得见。
 *
 * <h3>转轴：合页模型</h3>
 * 两格共用的那条 1dp 分隔线就是合页。所以
 * <ul>
 *   <li>上格 pivotY = 底边（h），正角度 → 顶边往后倒；</li>
 *   <li>下格 pivotY = 顶边（0），负角度 → 底边往后倒。</li>
 * </ul>
 * 两端往后倒、中间不动，配 0.96 的缩放，读起来就是「被按进去又弹回来」。
 *
 * <h3>弹簧参数</h3>
 * {@link SpringAnim.Spec}(k=1300, c=36, m=1)：ω₀≈36.1、ζ≈0.50、过冲 ≈16%、周期 ≈0.20s、
 * 时长（包络到 0.5%）= 294ms。
 *
 * <h3>为什么要「弹簧跑完 + 再等 SETTLE_MS 才开弹层」</h3>
 * v1.10.362 是在 {@code onAnimationEnd} 里直接 performClick —— 结束回调与「弹层挂上来」
 * 发生在**同一帧**，回弹最后一小段（以及回弹落定那一帧）会被弹层的模糊底直接盖住，
 * 用户看到的是「回弹被界面挡掉一部分」。现在落定后再等 80ms（≈5 帧）才开，
 * 保证整段回弹都被画完；这中间再按一次会**撤掉待触发的点击**，不会双开。
 */
final class QqPill {

    /** 倾斜角度（用户给的区间 8°~12°，取上界：低频大位移比小角度更容易看见）。 */
    private static final float ANGLE = 12.0f;
    /** 按下时缩到 0.96。 */
    private static final float PRESS_SCALE = 0.96f;
    /** 相机距离 = 视图高度的倍数（小 = 透视强）。2.8 是「明显但不廉价」的一档：12° 时近端撑宽 ~4%、远端收窄 ~4%。 */
    private static final float CAMERA_HEIGHT_X = 2.8f;

    /** 低阻尼弹簧：ζ≈0.50，过冲 ≈16%，时长 294ms。 */
    private static final SpringAnim.Spec TILT = new SpringAnim.Spec(1300.0f, 36.0f, 1.0f);
    /** 回弹落定后留给「看清楚」的时间（ms）：这段走完才真正打开弹层。 */
    private static final long SETTLE_MS = 80L;

    /** 每枚胶囊当前在跑的动画（WeakHashMap：视图被回收就自动丢掉，不会泄漏）。 */
    private static final WeakHashMap<View, ValueAnimator> RUNNING = new WeakHashMap<>();

    private QqPill() {
    }

    /**
     * 给一枚胶囊装上下压反馈。
     *
     * @param up true = 上方那一格（顶边往后倒），false = 下方那一格（底边往后倒）
     */
    static void install(final View target, final boolean up) {
        if (target == null) {
            return;
        }
        // 上格 +12°、下格 -12°（绕各自的外侧转轴，见类注释的合页模型）
        final float sign = up ? 1.0f : -1.0f;
        // 待触发的点击（弹簧跑完 + SETTLE_MS 后才发）——按下时撤掉，避免连点双开
        final Runnable[] pendingClick = new Runnable[1];
        target.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        cancelPending(v, pendingClick);
                        installRig(v, up);
                        animate(v, sign, 1.0f, null);  // 按下即响应，不等抬手
                        v.setPressed(true);
                        return true;
                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        // 回弹动画**跑完**才触发点击：弹层一起就跟上来，回弹一帧都看不见。
                        animate(v, sign, 0.0f, new Runnable() {
                            @Override
                            public void run() {
                                final Runnable click = new Runnable() {
                                    @Override
                                    public void run() {
                                        v.performClick();
                                    }
                                };
                                pendingClick[0] = click;
                                v.postDelayed(click, SETTLE_MS);
                            }
                        });
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        cancelPending(v, pendingClick);
                        v.setPressed(false);
                        animate(v, sign, 0.0f, null);   // 取消不触发点击
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    /** 撤掉还没发出去的点击（新按下 / 手势取消时）。 */
    private static void cancelPending(View v, Runnable[] pendingClick) {
        Runnable pending = pendingClick[0];
        if (pending != null) {
            v.removeCallbacks(pending);
            pendingClick[0] = null;
        }
    }

    /**
     * 3D 透视脚手架：相机距离 + 转轴。
     *
     * <p>必须在**布局完成之后**调（要用真实高度），所以放在 ACTION_DOWN 里而不是 install 里。
     */
    private static void installRig(View v, boolean up) {
        int h = v.getHeight();
        float density = v.getResources().getDisplayMetrics().density;
        // 相机距离单位是 px；拿不到高度时退回 112dp（= 2.8 × 40dp，和上面的常量同一个口径）
        v.setCameraDistance(h > 0 ? h * CAMERA_HEIGHT_X : density * 112.0f);
        if (h > 0) {
            // 合页：上格绕底边、下格绕顶边
            v.setPivotY(up ? h : 0.0f);
            v.setPivotX(v.getWidth() * 0.5f);
        }
    }

    /**
     * 从当前位置弹到目标姿态。
     *
     * @param sign ±1：上格 +1、下格 -1
     * @param to   1 = 按下（倾斜 + 缩小），0 = 松开（回正）
     */
    private static void animate(final View v, final float sign, final float to,
                                final Runnable onEnd) {
        ValueAnimator prev = RUNNING.get(v);
        if (prev != null) {
            // **先摘掉再 cancel**：cancel 会回调 onAnimationEnd，那时如果 RUNNING 里还是它，
            // 就会被误判成「正常结束」→ 连点一次触发两次点击。
            RUNNING.remove(v);
            prev.cancel();
        }
        final float unit = sign * ANGLE;
        // 反解起点：rotationX 已经是 ±ANGLE 的多少倍，就从那个归一化值接着走
        float from = v.getRotationX() / unit;
        final ValueAnimator a = ValueAnimator.ofFloat(from, to);
        a.setDuration(TILT.durationMs());
        // Spec.interpolator() 在过冲时会返回 >1 的值 —— 这正是「弹」的来源
        a.setInterpolator(TILT.interpolator());
        a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator anim) {
                float t = ((Float) anim.getAnimatedValue()).floatValue();
                v.setRotationX(unit * t);
                // 缩放跟着倾斜量走，但夹在 1.0 以内：过冲时不会再放大
                float k = Math.min(1.0f, Math.abs(t));
                float s = 1.0f - (1.0f - PRESS_SCALE) * k;
                v.setScaleX(s);
                v.setScaleY(s);
            }
        });
        if (onEnd != null) {
            a.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    // cancel() 也会走到这里 —— 用「当前是否仍是这段动画」判断，
                    // 避免连点时误触发两次点击。
                    if (RUNNING.get(v) == animation) {
                        RUNNING.remove(v);
                        onEnd.run();
                    }
                }
            });
        }
        a.start();
        RUNNING.put(v, a);
    }

    /** 宿主换主题/销毁时收尾（避免动画在已 detach 的视图上继续跑）。 */
    static void reset(View v) {
        if (v == null) {
            return;
        }
        ValueAnimator a = RUNNING.remove(v);
        if (a != null) {
            a.cancel();
        }
        v.setRotationX(0.0f);
        v.setScaleX(1.0f);
        v.setScaleY(1.0f);
    }

    /** 给「关于」胶囊用：判断这一枚是否已经装过（避免重复 setOnTouchListener 覆盖点击）。 */
    static boolean isInstalled(View v) {
        return v != null && RUNNING.containsKey(v);
    }
}
