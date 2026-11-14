package io.github.animwidget;

import java.util.Locale;

/**
 * v1.10.233：阴影模型 = **参考实现的那条单层高斯尾**（按外部复核结论重写）。
 *
 * <h3>参考实机的阴影到底长什么样（复核结论）</h3>
 * 参考包 {@code com.photo.beautifulwidgets.utils.d.G()} 里 {@code extractAlpha} 带 MaskFilter 时
 * 返回的是**外扩**位图（外扩量 m ≈ 2.8σ，offset 为负），代码却把整张位图拉伸到 {@code 画面 + 4u} ——
 * 于是高斯的 50% 点被推到画面边**外** δ ≈ 0.35u。结果剖面是：
 *
 * <pre>
 *   α(d) = A · Φ((δ − d) / σ)          A = (强度+20)/255
 *   δ ≈ 0.9%W        σ ≈ 1.7%W
 *   贴边 = Φ(δ/σ)·A ≈ 0.70A（强度100时 33%）——既不是 A 也不是 A/2
 *   到 d = 2u（5.7%W）只剩 ≈0.3%A —— **没有硬沿**
 * </pre>
 *
 * <p>外部复核用 Skia 的三遍 box 模糊 + 整图拉伸复算，预测「可见宽 4.9~5.0%、半高点占跨度 31~33%」，
 * 与用户同尺度整屏实测（5.1% / 32%）吻合；而"裁到原尺寸"那种假设会预测 5.7% / 100% / 24% 硬沿，
 * 对不上。所以：**参考是单层高斯尾，没有肩、没有断崖。**</p>
 *
 * <h3>本类的参数（两个物理量 + 强度）</h3>
 * <ul>
 *   <li>{@link #range()} <b>范围 L</b>（1%~8% 画面框宽，默认 5%）：阴影**看得见多远**，
 *       也就是「画面每边缩多少」（因果：出多少缩多少）。</li>
 *   <li>{@link #rho()} <b>紧实度 ρ = δ/σ</b>（−0.5~1.5，默认 0.55）：贴边有多实。
 *       ρ 就是「高斯 50% 点在画面边外几个 σ」：ρ=0.55 → 贴边 0.70A；ρ 越小越淡越拖尾
 *       （雾），越大越实越紧（硬）。</li>
 *   <li>强度（0~100）：不透明度 = 强度 + 20（参考实现原样，0 = 关）。</li>
 * </ul>
 *
 * <pre>
 *   z(α) = −Φ⁻¹(1/(255·α))      ← 让尾巴在 L 处正好掉到 1/255
 *   σ    = L / (ρ + z)           z 随强度略变（α 小 → z 大 → σ 略小）
 *   δ    = ρ·σ                   （可为负：ρ&lt;0 时 50% 点在画面里面）
 * </pre>
 *
 * <h3>为什么删掉「扩散 / 模糊半径 / 收边」</h3>
 * 那三个量是我自己造的组合：它们能凑出"带肩 + 断崖"（扩散顶住贴边、收边切一刀），
 * 而参考实机**没有肩也没有断崖** —— 带肩/断崖多半来自 G() 之外的布局（白底 RelativeLayout
 * 边界、centerCrop 裁切或截图缩放），不该复刻。现在 3 个旋钮一一对应可测量的量：
 * 强度、范围、紧实度；尾部固定截在 1/255（看不见），所以"收边"这个量自然消失。
 *
 * <h3>存档</h3>
 * {@code WidgetConfig.shadowCurve} 存 {@code "4:范围,紧实度"}；v1.10.221~232 的老串
 * （{@code "2:.."} / {@code "3:.."} / 无前缀）一律读成默认值。
 */
public final class ShadowSpec {

    // ---------------------------------------------------------------- 常量
    /** Skia：{@code BlurMaskFilter(radius)} 的 σ = radius × 1/√3。 */
    public static final float SIGMA_PER_BLUR = 0.57735026f;

    /** {@code setAlpha(强度 + 20)}。 */
    public static final int ALPHA_BIAS = 20;

    /** 强度上限 = 参考实现 SeekBar 的 max。 */
    public static final int STRENGTH_MAX = 100;

    /** 尾部截断点：1/255（参考实机在留白边上的残余量级），门限这么低就不会有可见的沿。 */
    public static final float CUT = 1.0f / 255.0f;

    public static final float RANGE_MIN = 0.010f;
    public static final float RANGE_MAX = 0.080f;
    public static final float RHO_MIN = -0.5f;
    public static final float RHO_MAX = 1.5f;

    /**
     * 默认 = **由 S01 真机实测反推的参考等效参数**（v1.10.236，修正了 v235 的口径错误）。
     *
     * <p>实测（用户手机，改进后的探头）：{@code r=16 → σ实测=10.38，extractAlpha 源 256×64 →
     * 返回 308×116，offset=-26,-26 → m=26px}。</p>
     *
     * <p><b>v1.10.235 错在哪</b>：我用「实测的 m」配「公式给的 σ（0.57735r+0.5=9.74）」，
     * 两者不是同一口径 —— 探头的 10.38 说明设备上的核比公式宽 6.6%。必须**成对使用实测值**：</p>
     *
     * <pre>
     *   尺度 f = σ实测/σ公式 = 10.38/9.74 = 1.066      k = m/σ实测 = 26/10.38 = 2.505
     *
     *   σ_ref(u) = f·(0.57735u + 0.5)        u = 画面宽/35
     *   m        = k·σ_ref
     *   s        = (W+4u)/(W+2m)             参考实现把外扩位图整张拉伸到「画面+4u」
     *   δ        = 2u − m·s                  高斯 50% 点在画面边外
     *   σₑ       = σ_ref·s
     *   ρ        = δ/σₑ                       W=980 时 0.581（随 W 在 0.46~0.60 间缓变）
     *   L        = δ + z·σₑ                   z = −Φ⁻¹(1/(255A)) = 2.394
     * </pre>
     *
     * <p>结果：<b>L ≈ 5.51%W（对 W 极稳）、ρ ≈ 0.58</b>（W=980）→ 贴边 0.72A = 33.9%、
     * 半高点/可见宽 0.316。与三项独立证据一致：同尺度整屏实测（可见宽 5.1%、半高点 32%）、
     * 外部复核复刻的 ρ 区间（0.49~0.62）、以及 S01 的 m。</p>
     */
    public static final float RANGE_DEFAULT = 0.0551f;
    public static final float RHO_DEFAULT = 0.58f;

    public static final ShadowSpec DEFAULT = new ShadowSpec(RANGE_DEFAULT, RHO_DEFAULT);

    private final float range;
    private final float rho;

    public ShadowSpec(float rangeFrac, float rho) {
        this.range = clamp(rangeFrac, RANGE_MIN, RANGE_MAX);
        this.rho = clamp(rho, RHO_MIN, RHO_MAX);
    }

    /** 范围 L（占画面框宽的比例）：阴影看得见多远 = 画面每边缩多少。 */
    public float range() {
        return this.range;
    }

    /** 紧实度 ρ = δ/σ：高斯 50% 点在画面边外几个 σ。 */
    public float rho() {
        return this.rho;
    }

    // ---------------------------------------------------------------- 数学（门禁/UI 共用，别在别处抄第二份）
    public static int alpha255(int strength) {
        int v = strength < 0 ? 0 : (strength > STRENGTH_MAX ? STRENGTH_MAX : strength);
        return v + ALPHA_BIAS;
    }

    /** 实际生效的不透明度（0~255）= (强度+20) × 整体透明度（0 = 关）。 */
    public static int effectiveAlpha(int strength, int alphaPercent) {
        int p = alphaPercent < 0 ? 0 : (alphaPercent > 100 ? 100 : alphaPercent);
        return (alpha255(strength) * p) / 100;
    }

    /** 范围（px）。 */
    public float rangePx(float width) {
        return this.range * width;
    }

    /** {@code z(α) = −Φ⁻¹(CUT/α)}：尾巴在 L 处掉到 1/255 的那个 σ 数。 */
    public static float zFor(int alpha255) {
        double a = alpha255 / 255.0d;
        if (a <= 0.0d) {
            return 8.0f;
        }
        double ratio = CUT / a;
        double p = Math.min(Math.max(ratio, 1.0e-9d), 0.499999d);
        return (float) (-invNormalCdf(p));
    }

    /** σ（px）= L / (ρ + z)。 */
    public static float sigmaPx(float rangePx, float rho, int alpha255) {
        float denom = rho + zFor(alpha255);
        if (denom < 0.05f) {
            denom = 0.05f;
        }
        return Math.max(rangePx / denom, 0.35f);
    }

    /** δ（px）= ρσ：高斯 50% 点相对画面边的偏移（负 = 在画面里面）。 */
    public static float deltaPx(float sigmaPx, float rho) {
        return rho * sigmaPx;
    }

    /**
     * σ → 给 {@code BlurMaskFilter} 的半径。
     *
     * <p>v1.10.235：按 S01 实测（r=16 时 σ≈10.0，比 r/√3=9.24 更接近 0.57735r+0.5=9.74）改用
     * Skia 的实际换算式 {@code σ = 0.57735·r + 0.5} 的**反解**：{@code r = (σ − 0.5)/0.57735}。
     * 偏差从"半径给大 3%~8%"降到 1% 量级。</p>
     */
    public static float blurRadiusPx(float sigmaPx) {
        float r = (sigmaPx - 0.5f) / SIGMA_PER_BLUR;
        return Math.max(r, 0.3f);
    }

    /** α(d)：d = 离画面边缘的距离（px）。 */
    public static float alphaAt(float d, int alpha255, float sigmaPx, float deltaPx) {
        double a = alpha255 / 255.0d;
        if (sigmaPx <= 0.0f) {
            return d <= deltaPx ? (float) a : 0.0f;
        }
        return (float) (a * normalCdf((deltaPx - d) / sigmaPx));
    }

    /** 贴边不透明度（0~255）→ 0~1。 */
    public static float edgeAlpha(int alpha255, float rho) {
        return (float) ((alpha255 / 255.0d) * normalCdf(rho));
    }

    // ---------------------------------------------------------------- 正态分布工具
    static double normalCdf(double x) {
        return 0.5d * erfc(-x / Math.sqrt(2.0d));
    }

    static double invNormalCdf(double p) {
        if (p <= 0.0d) {
            return -6.0d;
        }
        if (p >= 1.0d) {
            return 6.0d;
        }
        double lo = -8.0d;
        double hi = 8.0d;
        for (int i = 0; i < 80; i++) {
            double mid = (lo + hi) / 2.0d;
            if (normalCdf(mid) < p) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2.0d;
    }

    /** erfc 的 Chebyshev 近似（|ε| < 1.2e-7）。Android 没有 Math.erf。 */
    static double erfc(double x) {
        double z = Math.abs(x);
        double t = 1.0d / (1.0d + 0.5d * z);
        double s = 0.17087277d;
        s = (-0.82215223d) + (t * s);
        s = 1.48851587d + (t * s);
        s = (-1.13520398d) + (t * s);
        s = 0.27886807d + (t * s);
        s = (-0.18628806d) + (t * s);
        s = 0.09678418d + (t * s);
        s = 0.37409196d + (t * s);
        s = 1.00002368d + (t * s);
        double ans = t * Math.exp((((-z) * z) - 1.26551223d) + (t * s));
        return x >= 0.0d ? ans : 2.0d - ans;
    }

    // ---------------------------------------------------------------- 滑块映射（数字 = 千分比）
    /** 范围滑块：progress = 千分比（10~80 → 1.0%~8.0%）。 */
    public static float progressToRange(int progress) {
        return clamp(progress, 10, 800) / 1000.0f;
    }

    public static int rangeToProgress(float frac) {
        return Math.round(clamp(frac, RANGE_MIN, RANGE_MAX) * 1000.0f);
    }

    /** 紧实度滑块：0~500 → ρ = progress/250 − 0.5（覆盖 −0.5~1.5）。 */
    public static float progressToRho(int progress) {
        return clamp(progress, 0, 2000) / 250.0f - 0.5f;
    }

    public static int rhoToProgress(float rho) {
        return Math.round(clamp(rho, RHO_MIN, RHO_MAX) * 250.0f + 125.0f);
    }

    // ---------------------------------------------------------------- 存档
    public String encode() {
        return "4:" + fmt(this.range) + "," + fmt(this.rho);
    }

    private static String fmt(float v) {
        return String.format(Locale.US, "%.3f", Float.valueOf(v));
    }

    public boolean isDefault() {
        return Math.abs(this.range - DEFAULT.range) < 1.0e-4f
                && Math.abs(this.rho - DEFAULT.rho) < 1.0e-4f;
    }

    public static ShadowSpec parse(String s) {
        if (s == null || s.length() == 0 || !s.startsWith("4:")) {
            return DEFAULT;   // 老格式（"2:" / "3:" / 无前缀）一律回默认，不乱画
        }
        String[] parts = s.substring(2).split(",");
        if (parts.length != 2) {
            return DEFAULT;
        }
        try {
            float a = Float.parseFloat(parts[0].trim());
            float b = Float.parseFloat(parts[1].trim());
            if (Float.isNaN(a) || Float.isNaN(b) || Float.isInfinite(a) || Float.isInfinite(b)) {
                return DEFAULT;
            }
            return new ShadowSpec(a, b);
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
        return "ShadowSpec[range=" + fmt(this.range) + ", rho=" + fmt(this.rho) + "]";
    }
}
