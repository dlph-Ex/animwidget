package io.github.animwidget;

/**
 * v1.10.223：**阴影剖面**（扩散 + 渐变长度 + 渐变形状）。
 *
 * <h3>为什么又换了一次模型</h3>
 * v1.10.221~222 用的是「4 个控制点的单调三次插值」。用户装了以后说：
 * 「更像加了层半透明雾，不是阴影」。
 *
 * <p>我把两张图逐像素量出来对比（各自按**自己的可见跨度**归一化，否则比的是两种口径）：</p>
 * <pre>
 *   距边缘    参考图(图2)    旧的 S 形剖面    新的直线剖面
 *   0%        100%          100%            100%
 *   24%        75%           86%             77%
 *   47%        50%           56%             55%
 *   71%        29%           20%             32%
 *   94%        11%            7%             10%
 * </pre>
 *
 * <p>参考图是一条**近似直线**的渐变（拟合 α = 35%·(1−u)¹·⁰）；旧剖面是 S 形 ——
 * 贴边附近太扁（24% 处还有 86%，参考图已经掉到 75%），中后段又掉太快（71% 处只剩 20%，
 * 参考图还有 29%），一头平一头陡，整体摊成一片低对比的灰 —— 用户的原话是
 * 「更像加了层半透明雾，不是阴影」。这不是"再深一点/再浅一点"能修的，是形状本身错了。</p>
 *
 * <h3>现在的模型（三个量，每个都能用手指说清楚）</h3>
 * <pre>
 *   u = clamp((d − 扩散) / 渐变长度, 0, 1)         d = 离画面边缘的距离
 *   α(u) = 峰值 × (1 − u)^渐变形状
 * </pre>
 * <ul>
 *   <li><b>扩散</b>：贴着画面的一圈「实心」阴影（这一段不透明度一直是峰值）。0 = 从边缘就开始淡；</li>
 *   <li><b>渐变长度</b>：从峰值淡到 0 要走多远（占画布宽的百分比）；</li>
 *   <li><b>渐变形状</b>：曝光曲线。1.0 = 均匀直下（参考图就是 1.0）；&gt;1 = 贴边更实、外侧拖长；
 *       &lt;1 = 贴边就淡、外沿收得急。</li>
 * </ul>
 *
 * <p>「阴影出来多少，画面缩多少」这条规则没变：画面每边缩进 = 这里算出来的可见延伸
 * （α 衰减到 ≤4/255 的那个距离）。</p>
 *
 * <h3>坐标</h3>
 * 轴坐标 {@code u ∈ [0,1]}，{@code 1.0} = 画布宽的 {@link #WIDTH_MAX_FRAC}（8%）。
 * 扩散/渐变长度都以轴坐标存，渲染时乘画布宽。
 *
 * <h3>线程</h3>
 * 不可变：拖动滑块时每次造一个新对象，后台预烘/预览线程永远读到某一帧的完整剖面。
 */
public final class ShadowCurve {

    /** 曲线横轴的上限：画布宽的 8%（扩散 + 渐变长度的总和不能超过它）。 */
    public static final float WIDTH_MAX_FRAC = 0.08f;

    /**
     * 默认：无扩散 + **3.5% 渐变** + 形状 **1.15**。
     *
     * <p>v1.10.226：这是把用户最新给的**两张同机整屏截图**（1260×2800，同一壁纸、同一比例）
     * 逐像素量出来、再对我的剖面模型做最小二乘拟合的结果：</p>
     * <pre>
     *   距画面边缘    参考（图2）实测    拟合（本默认值）
     *   0px          34.0%            33.3%
     *   8px          26.5%            24.7%
     *   16px         16.5%            16.4%   ← 半高点 16px 完全对上
     *   24px          8.5%             8.6%
     *   32px          2.5%             1.6%
     * </pre>
     * <p>逐点最大偏差 0.018（≈4.6/255），可见跨度 3.3~4.7% 组件宽。</p>
     *
     * <p><b>教训（LESSONS 39）</b>：上一版我把默认加宽到 8.0%，是拿"裁剪图"量的 ——
     * 裁剪图被聊天窗缩放过、尺度未知，估出来的 7.1% 是假数。整屏截图里两个组件同尺度，
     * 一量就清楚了：我 7.8%、参考 5.1%（按组件宽），而且参考的半高点只在外侧的 35% 处
     * （我 45%）—— 参考是"更窄、更收"的，不是更宽。</p>
     */
    public static final float DEFAULT_SPREAD = 0.0f;
    public static final float DEFAULT_GRAD = 0.438f;      // × 8% = 3.5%
    public static final float DEFAULT_GAMMA = 1.15f;

    public static final ShadowCurve DEFAULT = new ShadowCurve(DEFAULT_SPREAD, DEFAULT_GRAD, DEFAULT_GAMMA);

    // ------------------------------------------------ 编辑范围（面板滑块 / 解码共用）
    /** 扩散上限：4.8% 画布宽（再宽就只剩一圈实心黑边了）。 */
    public static final float SPREAD_MAX = 0.60f;
    /**
     * 渐变长度：1.0% ~ **12.8%** 画布宽。
     *
     * <p>上限从 6.4% 提到 12.8%：参考图的阴影是 7.6% 宽，旧上限（6.4%）连它都够不到
     * （「参数范围比参考实现窄」本身就是 bug，不是口味问题）。</p>
     */
    public static final float GRAD_MIN = 0.125f;
    public static final float GRAD_MAX = 1.60f;
    /** 渐变形状（指数）：0.5 ~ 3.0。 */
    public static final float GAMMA_MIN = 0.5f;
    public static final float GAMMA_MAX = 3.0f;

    private final float spread;
    private final float grad;
    private final float gamma;

    public ShadowCurve(float spread, float grad, float gamma) {
        float sp = clamp(spread, 0.0f, SPREAD_MAX);
        float gr = clamp(grad, GRAD_MIN, GRAD_MAX);
        // 扩散 + 渐变不能越过上限，否则画面会被压掉超过 12.8%/边（那是"没有画面"了）
        if (sp + gr > GRAD_MAX) {
            sp = Math.max(0.0f, GRAD_MAX - gr);
        }
        this.spread = sp;
        this.grad = gr;
        this.gamma = clamp(gamma, GAMMA_MIN, GAMMA_MAX);
    }

    public float spread() {
        return this.spread;
    }

    public float grad() {
        return this.grad;
    }

    public float gamma() {
        return this.gamma;
    }

    /** 剖面在轴坐标 u 处的不透明度（0~1）。扩散段恒为 1，渐变段是 (1−t)^γ，末端之后为 0。 */
    public float alphaAt(float u) {
        float t = (u - this.spread) / this.grad;
        if (t <= 0.0f) {
            return 1.0f;   // 扩散段：整段都是峰值
        }
        if (t >= 1.0f) {
            return 0.0f;
        }
        if (this.gamma == 1.0f) {
            return 1.0f - t;
        }
        return (float) Math.pow(1.0f - t, this.gamma);
    }

    /** 剖面归零的位置（轴坐标）= 扩散 + 渐变长度。 */
    public float getEnd() {
        return this.spread + this.grad;
    }

    /**
     * 阴影**可见**延伸到哪儿（轴坐标）：解 {@code (1−t)^γ = 门限/峰值}。
     *
     * <p>这就是「画面每边缩进多少」的那个量 —— 不预设留白、不透支（LESSONS 30）。</p>
     */
    public float visibleEnd(float peak, float threshold) {
        if (peak <= threshold || peak <= 0.0f) {
            return 0.0f;
        }
        float ratio = Math.min(threshold / peak, 1.0f);
        float t = 1.0f - (float) Math.pow(ratio, 1.0f / this.gamma);
        return this.spread + this.grad * clamp(t, 0.0f, 1.0f);
    }

    public boolean isDefault() {
        return Math.abs(this.spread - DEFAULT.spread) < 1.0e-4f
                && Math.abs(this.grad - DEFAULT.grad) < 1.0e-4f
                && Math.abs(this.gamma - DEFAULT.gamma) < 1.0e-4f;
    }

    /**
     * 存档格式：3 个 3 位小数 {@code spread,grad,gamma}（{@link java.util.Locale#US}，
     * 否则某些区域的小数点是逗号，会把存档拆成 6 段 —— LESSONS 10 那类 bug）。
     *
     * <p>v1.10.221/222 存的是 6 个数的旧格式：解档时位数不对就退回默认剖面（老配置的阴影
     * 会回到默认形状，不会崩、也不会画出怪东西）。</p>
     */
    public String encode() {
        return fmt(this.spread) + "," + fmt(this.grad) + "," + fmt(this.gamma);
    }

    private static String fmt(float v) {
        return String.format(java.util.Locale.US, "%.3f", Float.valueOf(v));
    }

    public static ShadowCurve parse(String s) {
        if (s == null || s.length() == 0) {
            return DEFAULT;
        }
        String[] parts = s.split(",");
        if (parts.length != 3) {
            return DEFAULT;
        }
        try {
            float[] p = new float[3];
            for (int i = 0; i < 3; i++) {
                p[i] = Float.parseFloat(parts[i].trim());
                if (Float.isNaN(p[i]) || Float.isInfinite(p[i])) {
                    return DEFAULT;
                }
            }
            return new ShadowCurve(p[0], p[1], p[2]);
        } catch (Throwable t) {
            return DEFAULT;
        }
    }

    private static float clamp(float v, float lo, float hi) {
        if (v < lo) {
            return lo;
        }
        return v > hi ? hi : v;
    }

    @Override
    public String toString() {
        return "ShadowCurve[" + encode() + "]";
    }
}
