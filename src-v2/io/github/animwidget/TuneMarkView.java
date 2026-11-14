package io.github.animwidget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.PathInterpolator;

/**
 * v1.10.98 / v1.10.99（需求 2 / 反馈 3）：首页右上角那颗「设置」。
 *
 * 造型：圆底 + 三根导轨 + 三个错位的把手，读起来就是「调节器」，
 * 一眼知道是设置，又不是系统那颗千篇一律的齿轮。
 *
 * v1.10.99 重写了触摸动画（反馈 3）：
 *   · 按下后 **先等 80ms** 再起动画 —— 指尖的按压反馈先到位，动画不会和点击同一帧糊住；
 *   · **700ms 内只触发一次**（连点不会排队、不会叠出两套动画打架）；
 *   · 三个把手**错峰**出发（0 / 46 / 92ms，各 470ms），
 *     曲线用 (0.26, 1.30, 0.34, 1.00) 的三次贝塞尔 —— 先快、过冲 ~8%、再收住，
 *     就是 AE 里手拉手柄的「冲一下再稳住」；同时每颗把手在途中略微放大再收回（抓取感）。
 */
public final class TuneMarkView extends View {

    private static final float[] START = {0.26f, 0.74f, 0.40f};
    private static final float[] NEXT_A = {0.74f, 0.34f, 0.78f};
    private static final float[] NEXT_B = {0.48f, 0.64f, 0.26f};

    /** 按下到动画开始的延迟（ms）。 */
    private static final long START_DELAY = 110L;
    /** 连点冷却（ms）：这段时间内再点不叠加动画、也不再打开第二个页面。 */
    private static final long COOLDOWN = 700L;
    /**
     * 点完「先让动画播完、再进设置页」的延时（ms）：
     * {@link #START_DELAY} + 三段行程 + 一点余量。
     * 以前点一下立刻 startActivity，页面瞬间被盖住 —— 所以「看不到动画」。
     */
    public static final long OPEN_DELAY = 610L;
    /** 每个把手的行程时长（ms）。 */
    private static final long TRAVEL = 470L;
    /** 把手出发的错峰间隔（ms）。 */
    private static final long STAGGER = 46L;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] pos = {START[0], START[1], START[2]};
    private final float[] from = new float[3];
    private final float[] to = new float[3];
    private final float[] pulse = new float[3];

    private final PathInterpolator ease = new PathInterpolator(0.26f, 1.30f, 0.34f, 1.00f);

    private ValueAnimator anim;
    private int step;
    private long lastNudgeAt;

    private final Runnable start = new Runnable() {
        @Override
        public void run() {
            runAnimation();
        }
    };

    public TuneMarkView(Context context) {
        super(context);
        init();
    }

    public TuneMarkView(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TuneMarkView(Context context, android.util.AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private static final PathInterpolator PRESS_DOWN =
            new PathInterpolator(0.20f, 0.62f, 0.28f, 1.00f);
    private static final PathInterpolator PRESS_UP =
            new PathInterpolator(0.34f, 1.24f, 0.36f, 1.00f);

    private void init() {
        setClickable(true);
        setFocusable(true);
        // 按下即缩、抬起回弹：手指落下这一下就有反馈，
        // 然后是 START_DELAY 的停顿，再走三段把手 —— 整条节奏才读得出来。
        setOnTouchListener((v, e) -> {
            int action = e.getActionMasked();
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                animate().scaleX(0.93f).scaleY(0.93f)
                        .setDuration(110L).setInterpolator(PRESS_DOWN).start();
            } else if (action == android.view.MotionEvent.ACTION_UP
                    || action == android.view.MotionEvent.ACTION_CANCEL) {
                animate().scaleX(1.0f).scaleY(1.0f)
                        .setDuration(260L).setInterpolator(PRESS_UP).start();
            }
            return false;
        });
    }

    /**
     * 点按时调用：冷却期内直接忽略，否则延迟 80ms 起动画。
     *
     * @return true = 这次真的触发了（调用方可以据此安排「动画播完再跳页」）
     */
    public boolean nudge() {
        long now = SystemClock.uptimeMillis();
        if (now - this.lastNudgeAt < COOLDOWN) {
            return false;
        }
        this.lastNudgeAt = now;
        removeCallbacks(this.start);
        postDelayed(this.start, START_DELAY);
        return true;
    }

    private void runAnimation() {
        if (this.anim != null) {
            this.anim.cancel();
            this.anim = null;
        }
        float[] target = (this.step % 2 == 0) ? NEXT_A : NEXT_B;
        this.step++;
        for (int i = 0; i < 3; i++) {
            this.from[i] = this.pos[i];
            this.to[i] = target[i];
        }
        final long total = STAGGER * 2 + TRAVEL;
        ValueAnimator a = ValueAnimator.ofFloat(0.0f, 1.0f);
        a.setDuration(total);
        a.setInterpolator(null);
        a.addUpdateListener(animation -> {
            long at = animation.getCurrentPlayTime();
            for (int i = 0; i < 3; i++) {
                float local = (at - i * STAGGER) / (float) TRAVEL;
                if (local < 0.0f) {
                    local = 0.0f;
                }
                if (local > 1.0f) {
                    local = 1.0f;
                }
                float eased = TuneMarkView.this.ease.getInterpolation(local);
                TuneMarkView.this.pos[i] = TuneMarkView.this.from[i]
                        + (TuneMarkView.this.to[i] - TuneMarkView.this.from[i]) * eased;
                TuneMarkView.this.pulse[i] = (float) Math.sin(Math.PI * local);
            }
            TuneMarkView.this.invalidate();
        });
        a.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                for (int i = 0; i < 3; i++) {
                    TuneMarkView.this.pulse[i] = 0.0f;
                    TuneMarkView.this.pos[i] = TuneMarkView.this.to[i];
                }
                TuneMarkView.this.invalidate();
            }
        });
        this.anim = a;
        a.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(this.start);
        if (this.anim != null) {
            this.anim.cancel();
            this.anim = null;
        }
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * alpha);
        if (a <= 0) {
            a = 1;
        }
        return (a << 24) | (color & 0x00FFFFFF);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0.0f || h <= 0.0f) {
            return;
        }
        ThemeColors c = ThemeManager.INSTANCE.colors(getContext());
        float dp = getResources().getDisplayMetrics().density;

        float glow = Math.max(this.pulse[0], Math.max(this.pulse[1], this.pulse[2]));

        // 圆底（动画时略亮一点点）
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(c.getNeutral());
        canvas.drawCircle(w / 2.0f, h / 2.0f, Math.min(w, h) / 2.0f, this.paint);

        // 三根导轨
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeCap(Paint.Cap.ROUND);
        this.paint.setStrokeWidth(1.9f * dp);
        this.paint.setColor(withAlpha(c.getText(), 0.34f + 0.14f * glow));
        float left = w * 0.255f;
        float right = w * 0.745f;
        float[] ys = {h * 0.355f, h * 0.50f, h * 0.645f};
        for (int i = 0; i < 3; i++) {
            canvas.drawLine(left, ys[i], right, ys[i], this.paint);
        }

        // 三个把手（行程中略微放大）
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(c.getPrimary());
        float knob = 3.0f * dp;
        for (int i = 0; i < 3; i++) {
            float x = left + (right - left) * this.pos[i];
            canvas.drawCircle(x, ys[i], knob * (1.0f + 0.26f * this.pulse[i]), this.paint);
        }
    }
}
