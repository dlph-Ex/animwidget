package io.github.animwidget;

/**
 * v1.10.495：**剩余性能空间**的算法（纯逻辑，可脱机自检）。
 *
 * <h3>这笔账从哪来（全部是本项目真机实测，不是拍脑袋）</h3>
 * <ul>
 *   <li><b>解码吞吐</b>：1280×720 一帧解 12.4ms ⇒ **单核 71 Mpx/s**（`W48`，隔离线程、5 次中位数）。</li>
 *   <li><b>推送成本</b>：一次 `updateAppWidget`（含 RemoteViews 重建）实测 **1.5ms**（`W10 单帧均`）。</li>
 *   <li><b>同素材共享</b>：N 路同素材只解一份（分母只算一次）；实测 6 路 720p50 只解 152 次/3s。</li>
 *   <li><b>系统预留</b>：tick 线程、桌面合成、我们的服务常驻 ≈ **1.5 核**。</li>
 *   <li><b>热设计</b>：真机出现过 1787MHz → 1107MHz（降 40%）的窗口，所以可用核数再乘一个保守系数 0.8。</li>
 * </ul>
 *
 * <p><b>它不是什么</b>：不是「桌面能不能画出来」的保证。桌面侧（apply executor / 纹理带宽）
 * 我们测不到，这里算的只是**我们自己这一侧**的余量。所以文案只说「性能」，不说「一定能播」。</p>
 */
final class PerfMath {

    /** 单核解码吞吐（Mpx/s），实测 71。 */
    static final double DECODE_MPX_PER_CORE = 71.0;
    /** 一次推送的实测成本（ms）。 */
    static final double PUSH_MS = 1.5;
    /** 留给系统/桌面/我们自己的常驻开销（核）。 */
    static final double RESERVE_CORES = 1.5;
    /** 热降频的保守系数（实测见过掉 40%，取 0.8 不至于把话说满）。 */
    static final double THERMAL_FACTOR = 0.8;

    private PerfMath() {
    }

    /** 一路组件的解码核数（{@code shared=true} 表示与另一路同素材，解码已经算过，不再重复计）。 */
    static double decodeCores(long pixels, int fps, boolean shared) {
        if (shared || pixels <= 0L || fps <= 0) {
            return 0.0;
        }
        return (pixels * (double) fps / 1000000.0) / DECODE_MPX_PER_CORE;
    }

    /** 一路组件的推送核数（这一项**不会**因为共享而消失：每一路都要单独 apply）。 */
    static double pushCores(int fps) {
        if (fps <= 0) {
            return 0.0;
        }
        return fps * PUSH_MS / 1000.0;
    }

    /**
     * 这台机器能拿来推帧的核数：核数 − 预留，再乘热系数。
     * 下限 1.0（再差也要让一路转起来），上限 6.0（本机六路实测的总量级）。
     */
    static double budgetCores(int cores) {
        double c = Math.max(cores - RESERVE_CORES, 0.0) * THERMAL_FACTOR;
        return Math.max(1.0, Math.min(6.0, c));
    }

    /** 结果。 */
    static final class Verdict {
        /** 已用核数。 */
        final double used;
        /** 可用核数。 */
        final double budget;
        /** 剩余百分比（0~100，可为 0）。 */
        final int percent;
        /** 还能再加几个「与候选同参数」的组件（>=0）。 */
        final int more;
        /** 候选本身是否就已超出余量（true = 不建议加）。 */
        final boolean tooHeavy;

        Verdict(double used, double budget, int percent, int more, boolean tooHeavy) {
            this.used = used;
            this.budget = budget;
            this.percent = percent;
            this.more = more;
            this.tooHeavy = tooHeavy;
        }
    }

    /**
     * @param usedCores   现有全部组件的合计核数（含共享折算）
     * @param cores       设备核数（{@code Runtime.availableProcessors()}）
     * @param candidate   候选组件的单路核数（解码 + 推送）
     */
    static Verdict verdict(double usedCores, int cores, double candidate) {
        double budget = budgetCores(cores);
        double left = budget - usedCores;
        if (left < 0.0) {
            left = 0.0;
        }
        int percent = budget <= 0.0 ? 0 : (int) Math.round(100.0 * left / budget);
        int more;
        if (candidate <= 0.0001) {
            more = 99;                       // 静态图：几乎不占，按「很多」处理
        } else {
            more = (int) Math.floor(left / candidate);
        }
        if (more < 0) {
            more = 0;
        }
        if (more > 99) {
            more = 99;
        }
        boolean tooHeavy = candidate > 0.0001 && candidate > left;
        return new Verdict(usedCores, budget, Math.max(0, Math.min(100, percent)), more, tooHeavy);
    }
}
