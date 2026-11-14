package io.github.animwidget;

/**
 * v1.10.88：**三次贝塞尔缓动**（等价 CSS `cubic-bezier(x1,y1,x2,y2)`，也就是 AE 里手拉贝塞尔手柄）。
 *
 * 为什么要有这个类：
 * v1.10.87 的补位动画是「三段线性/平方斜率」拼出来的 ——
 * 缩小用线性、平移用平方、复原又是线性，在 p=0.32 / p=0.78 两个关键帧上是**折角**，
 * 一阶导（速度）在那里突然跳变，肉眼就是「到位那一下很生硬」。
 *
 * 现在每个关键帧都用贝塞尔，并且让关键帧处速度≈0（AE 的 Easy Ease），
 * 整条曲线 C1 连续，速度不会跳；复原段还带 1.2% 过冲再落回，是 AE 常见的「回弹」手感。
 *
 * 求解：先用牛顿法解 x(u)=t，失败再用二分兜底 —— 任何输入都返回有限值，绝不 NaN。
 */
final class Ease {

    /**
     * 两端平滑（Easy Ease）：平移、黑洞长大都用它。
     *
     * 控制点 (0.45, 0, 0.40, 1) 是实测挑出来的：速度峰值落在 42%，
     * 90% 位移在 72% 时间点完成 —— 尾部不拖长（原来的 (0.35,0,0.25,1)
     * 到 65% 就吃掉 90% 位移，剩下 35% 的时间只挪最后 10%，
     * 视觉上就是「先冲过去、再慢慢蹭」，也是「不够流畅」的来源之一）。
     */
    static final Ease IN_OUT = new Ease(0.45f, 0.00f, 0.40f, 1.00f);

    /** 快出慢收：淡入、缩小用。 */
    static final Ease OUT = new Ease(0.20f, 0.62f, 0.28f, 1.00f);

    /** 带过冲的回落（scale 专用）：曲线峰值 1.28 → 实际缩放峰值约 +1.2%。 */
    static final Ease OVERSHOOT = new Ease(0.45f, 0.00f, 0.25f, 2.10f);

    private final float x1;
    private final float y1;
    private final float x2;
    private final float y2;

    Ease(float x1, float y1, float x2, float y2) {
        this.x1 = clamp01(x1);
        this.y1 = y1;
        this.x2 = clamp01(x2);
        this.y2 = y2;
    }

    /** 0~1 的进度 → 缓动后的进度（可能略大于 1，带过冲）。 */
    float at(float t) {
        if (t <= 0.0f) {
            return 0.0f;
        }
        if (t >= 1.0f) {
            return 1.0f;
        }
        float u = solve(t);
        return curve(u, this.y1, this.y2);
    }

    private float solve(float x) {
        float u = x;
        // 牛顿法：8 次足够把误差压到 1e-5（x1/x2 已夹在 (0,1)，x 方向严格单调，斜率恒正）
        for (int i = 0; i < 8; i++) {
            float diff = curve(u, this.x1, this.x2) - x;
            if (diff < 1.0e-5f && diff > -1.0e-5f) {
                return u;
            }
            float d = slope(u, this.x1, this.x2);
            if (d < 1.0e-6f) {
                break;
            }
            u -= diff / d;
            if (u < 0.0f) {
                u = 0.0f;
            } else if (u > 1.0f) {
                u = 1.0f;
            }
        }
        // 二分兜底（单调收敛，保证不返回 NaN）
        float lo = 0.0f;
        float hi = 1.0f;
        u = x;
        for (int i = 0; i < 24; i++) {
            float cx = curve(u, this.x1, this.x2);
            if (Math.abs(cx - x) < 1.0e-5f) {
                return u;
            }
            if (cx < x) {
                lo = u;
            } else {
                hi = u;
            }
            u = (lo + hi) * 0.5f;
        }
        return u;
    }

    /** 三次贝塞尔的一维分量：3(1-u)²u·p1 + 3(1-u)u²·p2 + u³。 */
    private static float curve(float u, float p1, float p2) {
        float m = 1.0f - u;
        return 3.0f * m * m * u * p1 + 3.0f * m * u * u * p2 + u * u * u;
    }

    /** 上面那条曲线对 u 的导数。 */
    private static float slope(float u, float p1, float p2) {
        float m = 1.0f - u;
        return 3.0f * m * m * p1 + 6.0f * m * u * (p2 - p1) + 3.0f * u * u * (1.0f - p2);
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }

    @Override
    public String toString() {
        return "Ease(" + this.x1 + "," + this.y1 + "," + this.x2 + "," + this.y2 + ")";
    }
}
