package io.github.animwidget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v1.10.464：**分段选择器**（平行四边形 + 四角 45° 切角，靠左、按文字长度收窄）。
 *
 * <h3>形态（用户口径，逐条对应）</h3>
 * <ul>
 *   <li><b>平行四边形</b>：上边相对下边右移 {@link #SKEW_DP}，左右两条边是斜的 ——
 *       不是胶囊、也不是矩形；</li>
 *   <li><b>四个角是圆弧</b>：半径 {@link #CORNER_DP}（按盒子大小夹），用二次贝塞尔倒圆 ——
 *       465 那版是沿 45° 直切，用户一句「都不是圆角」把它否了；</li>
 *   <li><b>靠左</b>：宽度按文字长度算（{@code onMeasure} 用 resolveSize，父容器给
 *       WRAP_CONTENT 就只占自己要的那点），由调用方用 {@code gravity = START} 摆到左边；</li>
 *   <li><b>有过渡</b>：选中块用 {@link ValueAnimator} + {@link Ease#IN_OUT} 滑 180ms；</li>
 *   <li><b>选中块与轨道同一副形状</b>（v1.10.465，用户：「选中图标也请改为同圆角平行四边形，
 *       而不是直角四边形」）：它也是用 {@link #buildPolygon} 逐点画出来的斜四边形 + 四角切角，
 *       只是盒子随动画在两根刻度之间移动/变宽 —— <b>不是</b>一条矩形色带。</li>
 * </ul>
 *
 * <p>颜色由 {@link ThemeManager#paint} 统一下发（换肤 / 深浅 / 对比度都会走到），
 * 材质与页面胶囊同一套：轨道 = 中性 55%、1dp 正文 12% 发丝线、选中块 = 主色 78%、选中字 = onPrimary。</p>
 *
 * <p>v1.10.464：**作用范围（滑块色）也换成这个控件** —— 用户指出「滑块色的分段选择器没有适配
 * 新组件」，于是全仓库只留这一份分段选择器的实现，老的 {@code SliderScopeSwitch} 已删除。</p>
 */
public final class SegmentedRow extends View {

    /** 用户点了**另一段**才回调（同一段再点不回调，和系统分段按钮一致）。 */
    interface OnSegmentClick {
        void onClick(int index);
    }

    private static final float HEIGHT_DP = 34.0f;
    /** 四个角的**圆角半径**（真圆弧；v1.10.466 之前这里是 45° 直切，用户判为「不是圆角」）。 */
    private static final float CORNER_DP = 9.0f;
    /** 平行四边形的斜度：上边相对下边右移的量。 */
    private static final float SKEW_DP = 5.0f;
    /** 轨道内边距（选中块与轨道上下边缘之间留的那道线）。 */
    private static final float PAD_DP = 3.0f;
    /** 每一段文字的左右呼吸空间。 */
    private static final float SEG_PAD_DP = 13.0f;
    private static final float TEXT_SP = 12.5f;
    /** 与页面里其它切换动效同一档时长。 */
    private static final long DURATION = 180L;

    private final float density;
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path trackPath = new Path();
    private final Path pillPath = new Path();

    private String[] labels = new String[0];
    /** 每一段的宽度（px，含段内左右 padding）—— 按文字实时量，不写死等宽。 */
    private float[] widths = new float[0];
    private int selected;
    /** 选中块的**真实位置**（动画期间在两根刻度之间，0 = 第一段）。 */
    private float pos;
    @Nullable
    private ValueAnimator anim;
    @Nullable
    private OnSegmentClick listener;

    /** 兜底配色（还没被 {@link #applyColors} 下发时用；深色面板下不至于看不见）。 */
    private int colorTrack = 0x8C2A2A2A;
    private int colorPill = 0xC73D7BFF;
    private int colorBorder = 0x1FFFFFFF;
    private int colorText = 0xFF9A9A9A;
    private int colorTextSel = 0xFFFFFFFF;

    public SegmentedRow(@NotNull Context context) {
        super(context);
        this.density = getResources().getDisplayMetrics().density;
        float scaled = getResources().getDisplayMetrics().scaledDensity;
        // 字号跟系统字体，但**封顶 1.25 倍**：几段文字要在一行里全放下
        this.textPaint.setTextSize(TEXT_SP * Math.min(scaled, this.density * 1.25f));
        this.textPaint.setTextAlign(Paint.Align.CENTER);
        this.borderPaint.setStyle(Paint.Style.STROKE);
        this.borderPaint.setStrokeWidth(Math.max(1.0f, 1.0f * this.density));
        setClickable(true);
        setFocusable(true);
    }

    // ---------------------------------------------------------------- 外部接口

    void setLabels(@NotNull String[] values) {
        this.labels = values;
        this.widths = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            this.widths[i] = this.textPaint.measureText(values[i]) + SEG_PAD_DP * 2.0f * this.density;
        }
        this.pos = Math.max(0, Math.min(this.selected, Math.max(values.length - 1, 0)));
        requestLayout();
        invalidate();
    }

    /**
     * 选中第 index 段。
     *
     * @param animate true = 滑过去（用户点击 / 档位变化）；false = 直接落位（首次进页面）
     */
    void setSelected(int index, boolean animate) {
        int safe = Math.max(0, Math.min(index, Math.max(this.labels.length - 1, 0)));
        this.selected = safe;
        if (!animate || Math.abs(this.pos - safe) < 0.001f) {
            cancelAnim();
            this.pos = safe;
            invalidate();
            return;
        }
        final float from = this.pos;
        final float to = safe;
        cancelAnim();
        ValueAnimator a = ValueAnimator.ofFloat(0.0f, 1.0f);
        a.setDuration(DURATION);
        a.setInterpolator(t -> Ease.IN_OUT.at(t));
        a.addUpdateListener(x -> {
            SegmentedRow.this.pos = from + (to - from) * ((Float) x.getAnimatedValue());
            SegmentedRow.this.invalidate();
        });
        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                SegmentedRow.this.pos = to;
                SegmentedRow.this.invalidate();
            }
        });
        this.anim = a;
        a.start();
    }

    int getSelected() {
        return this.selected;
    }

    void setOnSegmentClick(@Nullable OnSegmentClick l) {
        this.listener = l;
    }

    /** 由 {@link ThemeManager#paint} 统一下发（材质与页面胶囊逐项对齐）。 */
    void applyColors(@NotNull ThemeColors c) {
        ThemeManager tm = ThemeManager.INSTANCE;
        this.colorTrack = tm.withAlpha(c.getNeutral(), 0.55f);   // 同「取消」胶囊
        this.colorPill = tm.withAlpha(c.getPrimary(), 0.78f);    // 同「保存」胶囊
        this.colorBorder = tm.withAlpha(c.getText(), 0.12f);     // 同「取消」的发丝线
        this.colorText = c.getTextMuted();
        this.colorTextSel = c.getOnPrimary();
        invalidate();
    }

    // ---------------------------------------------------------------- 测量与绘制

    /** 轨道内容起点（左边留一条 padding + 斜度的一半，斜边才不会切到字）。 */
    private float contentLeft() {
        return PAD_DP * this.density + SKEW_DP * this.density / 2.0f;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float content = 0.0f;
        for (float w : this.widths) {
            content += w;
        }
        // 宽度只按自己需要的量算：父容器给 WRAP_CONTENT 时就是「靠左的一小条」
        int want = Math.round(content + PAD_DP * 2.0f * this.density + SKEW_DP * this.density);
        int h = Math.round(HEIGHT_DP * this.density);
        setMeasuredDimension(resolveSize(want, widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    /**
     * 画一个**圆角平行四边形**的轮廓（轨道与选中块共用这一份实现）。
     *
     * <pre>
     *      ╱‾‾‾‾‾‾╲        4 个顶点：上边右移 s（平行四边形），四个角都是**圆弧**（半径 r）
     *     ╱       ╲
     *     ╲       ╱
     *      ╲_____╱
     * </pre>
     *
     * <p>v1.10.466（用户：「都不是圆角」）：465 那版是四个角**沿 45° 直切**——看起来是切角，
     * 不是圆角。现在改成**真的圆弧**：每个顶点取两条边的切点，用二次贝塞尔（控制点就是顶点）
     * 把角倒圆。半径按盒子大小夹（{@link #CORNER_DP}），所以窄的选中块也不会倒成一团。</p>
     *
     * <p>把「盒子」当参数（left/top/right/bottom）而不是只画整块：选中块就是同一个函数
     * 画在它自己那个盒子里 —— 这样两块**形状必然一致**，不会出现「轨道是圆角、选中块是直角」。</p>
     */
    private void buildPolygon(@NotNull Path out, float left, float top, float right, float bottom) {
        float w = right - left;
        float h = bottom - top;
        out.reset();
        if (w <= 0.0f || h <= 0.0f) {
            return;
        }
        float s = Math.min(SKEW_DP * this.density, Math.max(0.0f, w / 6.0f));
        float r = Math.min(CORNER_DP * this.density, Math.min(h / 2.0f, w / 3.0f));
        // 平行四边形四个顶点（顺时针）：左上、右上、右下、左下
        float[] xs = {left + s, right, right - s, left};
        float[] ys = {top, top, bottom, bottom};
        // 每个角：取两条边上的切点，再用二次贝塞尔把角倒圆（控制点 = 顶点本身）
        float[][] cut = new float[4][2];   // [i] = 沿「前一条边」/「后一条边」的切点距离
        for (int i = 0; i < 4; i++) {
            int p = (i + 3) & 3;
            int n = (i + 1) & 3;
            float ux = xs[p] - xs[i];
            float uy = ys[p] - ys[i];
            float vx = xs[n] - xs[i];
            float vy = ys[n] - ys[i];
            float lu = (float) Math.hypot(ux, uy);
            float lv = (float) Math.hypot(vx, vy);
            if (lu <= 0.0f || lv <= 0.0f) {
                cut[i][0] = 0.0f;
                cut[i][1] = 0.0f;
                continue;
            }
            ux /= lu;
            uy /= lu;
            vx /= lv;
            vy /= lv;
            float cos = Math.max(-1.0f, Math.min(1.0f, ux * vx + uy * vy));
            float half = (float) (Math.acos(cos) / 2.0);
            // d = r / tan(θ/2)：θ 越大（角越平）切点越短；90° 时 d = r
            float d = (float) (r / Math.max(0.05f, Math.tan(half)));
            d = Math.min(d, Math.min(lu, lv) * 0.45f);
            cut[i][0] = d;
            cut[i][1] = d;
        }
        // 起点：最后一个角「后一条边」上的切点
        out.moveTo(xs[3] + (xs[0] - xs[3]) / dist(xs[3], ys[3], xs[0], ys[0]) * cut[3][1],
                ys[3] + (ys[0] - ys[3]) / dist(xs[3], ys[3], xs[0], ys[0]) * cut[3][1]);
        for (int i = 0; i < 4; i++) {
            int p = (i + 3) & 3;
            int n = (i + 1) & 3;
            float lu = dist(xs[i], ys[i], xs[p], ys[p]);
            float lv = dist(xs[i], ys[i], xs[n], ys[n]);
            // 前一条边上的切点（进入这个角）
            out.lineTo(xs[i] + (xs[p] - xs[i]) / lu * cut[i][0],
                    ys[i] + (ys[p] - ys[i]) / lu * cut[i][0]);
            // 倒圆到后一条边上的切点
            out.quadTo(xs[i], ys[i],
                    xs[i] + (xs[n] - xs[i]) / lv * cut[i][1],
                    ys[i] + (ys[n] - ys[i]) / lv * cut[i][1]);
        }
        out.close();
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        return (float) Math.hypot(x2 - x1, y2 - y1);
    }

    /** 第 index 段左边缘相对内容起点的偏移；index 可以是小数（动画中间态）。 */
    private float offsetOf(float index) {
        int n = this.widths.length;
        if (n == 0) {
            return 0.0f;
        }
        if (index <= 0.0f) {
            return 0.0f;
        }
        int i = (int) Math.floor(index);
        float frac = index - i;
        if (i >= n) {
            float all = 0.0f;
            for (float w : this.widths) {
                all += w;
            }
            return all;
        }
        float acc = 0.0f;
        for (int k = 0; k < i; k++) {
            acc += this.widths[k];
        }
        return acc + this.widths[i] * Math.max(0.0f, Math.min(1.0f, frac));
    }

    @Override
    protected void onDraw(@NotNull Canvas canvas) {
        int n = this.labels.length;
        if (n <= 0) {
            return;
        }
        float w = getWidth();
        float h = getHeight();
        if (w <= 0.0f || h <= 0.0f) {
            return;
        }
        buildPolygon(this.trackPath, 0.0f, 0.0f, w, h);
        this.trackPaint.setColor(this.colorTrack);
        canvas.drawPath(this.trackPath, this.trackPaint);

        // 选中块：**与轨道同一套点**的斜四边形 + 四角切角，盒子随动画在两根刻度之间移动/变宽。
        // （v1.10.465：之前是「矩形色带被轨道裁剪」，静止在中段时就是一个直角四边形 —— 用户否了。）
        float base = contentLeft();
        float left = base + offsetOf(this.pos);
        float right = base + offsetOf(this.pos + 1.0f);
        float pad = PAD_DP * this.density;
        buildPolygon(this.pillPath, left, pad, right, h - pad);
        this.pillPaint.setColor(this.colorPill);
        // 仍然按轨道形状裁一刀：小屏/大字体下盒子可能略超出轨道，裁掉比「溢出一点」体面
        int save = canvas.save();
        canvas.clipPath(this.trackPath);
        canvas.drawPath(this.pillPath, this.pillPaint);
        canvas.restoreToCount(save);

        // 文字：颜色按选中块覆盖比例插值
        float baseline = h / 2.0f - (this.textPaint.descent() + this.textPaint.ascent()) / 2.0f;
        float acc = base;
        for (int i = 0; i < n; i++) {
            float cov = coverage(i, this.pos);
            this.textPaint.setColor(mix(this.colorText, this.colorTextSel, cov));
            this.textPaint.setFakeBoldText(cov > 0.5f);
            canvas.drawText(this.labels[i], acc + this.widths[i] / 2.0f, baseline, this.textPaint);
            acc += this.widths[i];
        }
        this.textPaint.setFakeBoldText(false);

        this.borderPaint.setColor(this.colorBorder);
        canvas.drawPath(this.trackPath, this.borderPaint);
    }

    /** 第 i 段被选中块盖住的比例（0~1）—— 文字色靠它插值，滑动时不会「啪」地换色。 */
    private static float coverage(int i, float pos) {
        float d = Math.abs(pos - i);
        return d >= 1.0f ? 0.0f : 1.0f - d;
    }

    /** sRGB 分量线性插值（与 ArgbEvaluator 同口径）。 */
    private static int mix(int a, int b, float f) {
        if (f <= 0.0f) {
            return a;
        }
        if (f >= 1.0f) {
            return b;
        }
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * f + 0.5f);
        int g = (int) (ag + (bg - ag) * f + 0.5f);
        int bl = (int) (ab + (bb - ab) * f + 0.5f);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private void cancelAnim() {
        ValueAnimator a = this.anim;
        this.anim = null;
        if (a != null) {
            try {
                a.cancel();
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelAnim();
        super.onDetachedFromWindow();
    }

    // ---------------------------------------------------------------- 触摸

    @Override
    public boolean onTouchEvent(@NotNull MotionEvent event) {
        if (!isEnabled() || this.labels.length <= 0) {
            return false;
        }
        int idx = indexAt(event.getX());
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                animate().cancel();
                animate().alpha(0.86f).setDuration(90L).start();
                return true;
            case MotionEvent.ACTION_CANCEL:
                animate().cancel();
                animate().alpha(1.0f).setDuration(120L).start();
                return true;
            case MotionEvent.ACTION_UP:
                animate().cancel();
                animate().alpha(1.0f).setDuration(120L).start();
                performClick();
                if (idx != this.selected) {
                    setSelected(idx, true);
                    if (this.listener != null) {
                        this.listener.onClick(idx);
                    }
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private int indexAt(float x) {
        float acc = contentLeft();
        int n = this.labels.length;
        for (int i = 0; i < n; i++) {
            if (x < acc + this.widths[i] || i == n - 1) {
                return i;
            }
            acc += this.widths[i];
        }
        return 0;
    }
}
