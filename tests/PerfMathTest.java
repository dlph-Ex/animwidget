package io.github.animwidget;

/**
 * v1.10.495：{@link PerfMath} 的脱机自检（verify-apk.sh [30] 的一部分）。
 *
 * <p>这一行文字给用户看的是「还能再加几个组件」，说错了就是误导他往桌面上堆组件。
 * 所以钉住三件事：</p>
 * <ul>
 *   <li>共享折算：同素材两路只算一份解码，但推送要算两份；</li>
 *   <li>余量百分比永远在 0~100，超了不能显示负数；</li>
 *   <li>「不建议再加」的判定方向：单路需求 &gt; 剩余余量时必须置位。</li>
 * </ul>
 */
public final class PerfMathTest {

    private static int total = 0;
    private static int bad = 0;

    public static void main(String[] args) {
        // 720p（0.92Mpx）50fps：解码 = 46.08/71 ≈ 0.649 核；推送 = 50×1.5/1000 = 0.075 核
        double d720 = PerfMath.decodeCores(921600L, 50, false);
        close("720p50 解码核", 0.649, d720, 0.01);
        close("720p50 推送核", 0.075, PerfMath.pushCores(50), 0.001);
        close("共享后不再计解码", 0.0, PerfMath.decodeCores(921600L, 50, true), 0.0001);

        // 1080p50：2.07Mpx × 50 = 103.7 Mpx/s ⇒ 1.46 核
        close("1080p50 解码核", 1.46, PerfMath.decodeCores(2073600L, 50, false), 0.02);

        // 预算：8 核 − 1.5 = 6.5，×0.8 = 5.2
        close("8 核预算", 5.2, PerfMath.budgetCores(8), 0.001);
        close("2 核也有下限 1.0", 1.0, PerfMath.budgetCores(2), 0.001);
        close("超多核封顶 6.0", 6.0, PerfMath.budgetCores(16), 0.001);

        // 空机：用掉 0，候选 0.72 核 ⇒ 还能加 7 个（5.2/0.72=7.2）
        double cand720 = d720 + PerfMath.pushCores(50);
        PerfMath.Verdict v1 = PerfMath.verdict(0.0, 8, cand720);
        eq("空机百分比 100", 100, v1.percent);
        eq("空机还能加 7 个", 7, v1.more);
        ok(!v1.tooHeavy, "空机不该判「不建议」");

        // 已经用了 4.8 核：剩 0.4 核 ⇒ 0 个，且候选 0.72 > 0.4 ⇒ 不建议再加
        PerfMath.Verdict v2 = PerfMath.verdict(4.8, 8, cand720);
        eq("紧张时 0 个", 0, v2.more);
        ok(v2.tooHeavy, "剩 0.4 核却要 0.72 核，必须判「不建议」");
        ok(v2.percent >= 0 && v2.percent <= 8, "百分比应在 0~8 之间，实际 " + v2.percent);

        // 超预算：不能出现负百分比
        PerfMath.Verdict v3 = PerfMath.verdict(9.0, 8, cand720);
        eq("超预算百分比夹到 0", 0, v3.percent);
        eq("超预算 0 个", 0, v3.more);
        ok(v3.tooHeavy, "超预算必须判「不建议」");

        // 静态图（候选≈0）：按「很多」处理，但不许是 100 以上
        PerfMath.Verdict v4 = PerfMath.verdict(1.0, 8, 0.0);
        eq("静态图算很多（99 上限）", 99, v4.more);

        // 候选为 0 时也应给出 99 而不是除零
        ok(v4.percent > 0, "空余量百分比应大于 0");

        if (bad > 0) {
            System.out.println("PERFMATH FAILED " + bad + "/" + total);
            System.exit(1);
        }
        System.out.println("PERFMATH OK " + total);
    }

    private static void ok(boolean cond, String msg) {
        total++;
        if (!cond) {
            bad++;
            System.out.println("  FAIL " + msg);
        }
    }

    private static void eq(String what, int want, int got) {
        total++;
        if (want != got) {
            bad++;
            System.out.println("  FAIL " + what + "：want=" + want + " got=" + got);
        }
    }

    private static void close(String what, double want, double got, double tol) {
        total++;
        if (Math.abs(want - got) > tol) {
            bad++;
            System.out.println("  FAIL " + what + "：want≈" + want + " got=" + got);
        }
    }
}
