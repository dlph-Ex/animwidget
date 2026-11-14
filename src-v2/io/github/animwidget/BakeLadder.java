package io.github.animwidget;

/**
 * v1.10.321（B）：画布预算的**纯函数**部分 —— 与 Android 无关，可离线对拍。
 *
 * 拆出来的原因和 {@link FoldMotion} 一样：判据一旦写在"需要真机才能跑"的地方，
 * 就只能靠日志反推；放到这里，`scripts/bakeladder-math-check.sh` 每次出包都会逐点验一遍。
 *
 * 两个函数：
 *   · {@link #budgetFor(float, int)}：理论预算 → 这一拍真正用的预算（乘系数 + 夹到阶梯）
 *   · {@link #nextFactor(float, float, float)}：实测吞吐 vs 目标吞吐 → 下一批的系数
 */
public final class BakeLadder {

    /** 系数下限：再慢也不许把画质压到这个以下 */
    public static final float MIN = 0.55f;
    /** 系数上限：校准只允许"降画质换流畅"，永不把预算抬到默认值以上 */
    public static final float MAX = 1.0f;

    /**
     * 允许的面积预算（px），降序；相邻档约 10%。
     *
     * 为什么是 10% 一档：
     *   · 尺寸拖动带来的面积抖动通常在 1%~3%，10% 的档距足以让它**总是落回同一档**
     *     （这是"拖动不重烘"的保险，也是同一画布的缓存能复用的前提）；
     *   · 10% 也是"往下取整"的最大代价 —— 不会出现"417k 掉到 350k"那种掉 16% 的账。
     */
    public static final int[] LADDER = {
            2200000, 900000, 810000, 730000, 660000, 590000, 530000, 480000,
            430000, 390000, 350000, 315000, 285000, 255000, 230000, 205000,
            185000, 165000, 145000, 130000, 115000, 100000, 90000};

    private BakeLadder() {
    }

    /**
     * 理论预算 → 实际预算。
     *
     * 规则：先乘系数，再**往下取整到最近的档位**（最大代价 10%）。
     * 为什么不做"最近档"：最近档会在档位边界上让 ±1.5% 的抖动跨档（实测 410000±1.5% 会
     * 分别落到 430000/350000），那正是"拖一下尺寸就整批重烘"的来源。往下取整虽然固定少一点，
     * 但**单调、抗抖、永不高于理论预算** —— 三个性质都能被离线对拍证明。
     */
    public static int budgetFor(float factor, int wanted) {
        if (wanted <= 0) {
            return wanted;
        }
        float f = clamp(factor);
        long after = Math.round(wanted * (double) f);
        int best = LADDER[LADDER.length - 1];
        for (int v : LADDER) {
            if (v <= after) {
                best = v;
                break;
            }
        }
        return Math.min(best, wanted);
    }

    /**
     * v1.10.333：**这一档要不要受自校准（系数 + 阶梯）约束**。
     *
     * 动图「1:1」档回答的是"我要原清晰度"，它的预算是 `min(格子, 220 万)` ——
     * 一个由用户显式选择、不需要任何人再打折的数。实测（21:11:27~21:12:59，三组件
     * 1:1 / 50fps / 1080 素材）证明拿它去校准是纯亏：
     *
     *   · 阶梯取整：格子 662x643 = 42.6 万，系数 1.00 也只能落到 39 万 = 633x615。
     *     "贴满格子"这件事在 1:1 档里**永远达不到**，差 4.4% 的上采样。
     *   · 测量被污染：311 的预热和 312 的导出撞在一起 → 编码均 136ms（独热时 88ms）→
     *     实测 33 帧/秒 → 比例控制一步 1.00→0.57 → 画布 486x472（23 万），桌面放大 1.36 倍。
     *   · 三组件同时预热时测得 17~23 帧/秒 → 三个组件一起砸到下限 0.55。
     *   · 而稳态（命中 100%）它们也只有 37~41fps —— 卡在推帧节拍上，不在烘速上。
     *     也就是说这次降档**一帧都没换来**，只换来永久发虚。
     *
     * 所以：`lvl >= 2`（用户选的 1:1）不受约束；静态图（单帧）本来也不该受约束
     * （它的预算已经是 HIRES_AREA 早退，这里是第二道保险）。
     * 「标准（0）」和「高清（1）」不受影响 —— 那两档的画质是"够用"而不是承诺。
     *
     * @param lvl        动图分辨率档位：0 标准 / 1 高清 / 2 1:1
     * @param frameCount 帧数（<=1 表示静态图）
     */
    public static boolean calibrated(int lvl, int frameCount) {
        if (frameCount <= 1) {
            return false;
        }
        return lvl < 2;
    }

    /**
     * 下一批的自校准系数。
     *
     * @param now      当前系数
     * @param measured 这批的实测聚合烘速（帧/秒）
     * @param target   目标烘速（= 播放 fps × 1.15，留 15% 余量）
     */
    public static float nextFactor(float now, float measured, float target) {
        float f = clamp(now);
        if (measured <= 0f || target <= 0f) {
            return f;
        }
        float next = f;
        if (measured < target * 0.90f) {
            // v1.10.331：**比例控制**。吞吐 ≈ 6 线程/单帧耗时，而单帧耗时 ∝ 面积，
            // 所以"面积 × (实测/目标)"一轮就能到位。
            // 旧版每次只乘 0.90：从 1.0 到 0.55 要 6 批 ≈ 1 分钟，期间用户一直看顶帧。
            float ratio = measured / target;
            next = f * Math.max(0.35f, ratio);
        } else if (measured > target * 1.80f) {
            next = f * 1.06f;            // 富余很多 → 快一点回升
        } else if (measured > target * 1.05f) {
            // v1.10.332：只要稳定跑赢目标 5% 就慢慢往上爬。
            // 原来只有 >1.8 倍才回升，于是"59 帧/秒 vs 目标 57.5"这种合格但不夸张的机器
            // 会永远停在曾经的 0.91 上 —— 画布 633 而格子 662，1:1 差 4.5%。
            next = f * 1.04f;
        }
        return clamp(next);
    }

    public static float clamp(float f) {
        if (Float.isNaN(f) || f <= 0f) {
            return MAX;
        }
        return Math.max(MIN, Math.min(MAX, f));
    }
}
