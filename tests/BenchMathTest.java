package io.github.animwidget;

/**
 * v1.10.478（W48）：{@link BenchMath} 的脱机自检（verify-apk.sh [26] 的一部分）。
 *
 * <p>要钉住的都是「写错了日志照样好看」的那类错：</p>
 * <ul>
 *   <li>中位数：偶数个必须取靠前那个，而不是平均数、更不是最大值（最大值 = 抖动进日志）；</li>
 *   <li>空样本 / 全零样本不许除零、不许报「省了 100%」；</li>
 *   <li>判据方向：合计 &ge; WebP 时必须写「不划算」（写反了会让人以为这条路成立）；</li>
 *   <li>毫秒必须带一位小数（v1.10.479：整毫秒会把小画布的差异全抹平）；</li>
 *   <li>跳过的那一行必须把**原因和门槛**都写出来（478 就是静默跳过，真机上一次都没测到）。</li>
 * </ul>
 */
public final class BenchMathTest {

    private static int total = 0;
    private static int bad = 0;

    public static void main(String[] args) {
        // ---- 中位数
        eq("median 奇数个", 5L, BenchMath.median(new long[]{9L, 1L, 5L, 3L, 7L}));
        eq("median 偶数个取靠前", 3L, BenchMath.median(new long[]{1L, 3L, 7L, 9L}));
        eq("median 单个", 42L, BenchMath.median(new long[]{42L}));
        eq("median 空", 0L, BenchMath.median(new long[0]));
        eq("median null", 0L, BenchMath.median(null));
        eq("median 已排序不受影响", 4L, BenchMath.median(new long[]{1L, 2L, 4L, 8L, 16L, 32L}));
        eq("median 被离群值污染", 100L, BenchMath.median(new long[]{100L, 100L, 100L, 999999L}));

        // ---- 纳秒 → 毫秒（一位小数）
        eqs("ms 0.4", "0.4", BenchMath.ms(400000L));
        eqs("ms 0.45→0.5", "0.5", BenchMath.ms(450000L));
        eqs("ms 12.6", "12.6", BenchMath.ms(12600000L));
        eqs("ms 6", "6.0", BenchMath.ms(6000000L));
        eqs("ms 负数→0.0", "0.0", BenchMath.ms(-5L));

        // ---- 纳秒 → 整毫秒（只给比例用）
        eq("ns2ms 0.4→0", 0L, BenchMath.ns2ms(400000L));
        eq("ns2ms 12.6→13", 13L, BenchMath.ns2ms(12600000L));

        // ---- 省下的百分比（**纳秒口径**：479 的真机行 0.9ms vs 0.5ms 被整毫秒舍成 100%）
        eq("省 50%", 50L, BenchMath.savePercent(10000000L, 5000000L));
        eq("省 0%", 0L, BenchMath.savePercent(10000000L, 10000000L));
        eq("负 = 更贵", -30L, BenchMath.savePercent(10000000L, 13000000L));
        eq("基准为 0 不许崩", 0L, BenchMath.savePercent(0L, 5000000L));
        // 真机 479 那一行：0.90ms vs 0.45+0.04ms —— 必须 ≈46%，不是 100%
        eq("亚毫秒样本不许报 100%", 46L, BenchMath.savePercent(900000L, 490000L));

        // ---- 测量行：字段齐全 + 判据方向
        String cheap = BenchMath.line(425, 117, 235, 879, 869, 13000000L, 5000000L, 2000000L);
        ok(cheap.startsWith("解码对拍 "), "行首必须是「解码对拍」：" + cheap);
        ok(cheap.contains("组件=425"), "缺组件号（多组件时看不出是谁）：" + cheap);
        ok(cheap.contains("帧=117/235"), "缺帧号：" + cheap);
        ok(cheap.contains("画布=879x869"), "缺画布：" + cheap);
        ok(cheap.contains("WebP=13.0ms"), "WebP 毫秒不对：" + cheap);
        ok(cheap.contains("JPEG=5.0ms"), "JPEG 毫秒不对：" + cheap);
        ok(cheap.contains("合成=2.0ms"), "合成毫秒不对：" + cheap);
        ok(cheap.contains("合计=7.0ms"), "合计必须 = JPEG + 合成：" + cheap);
        ok(cheap.contains("省=46%"), "百分比不对（(13-7)/13≈46%）：" + cheap);
        ok(cheap.contains("判据=JPEG+合成更便宜"), "便宜时判据写反了：" + cheap);
        ok(cheap.contains("WebP=13.0ms") && !cheap.contains("=13ms"),
           "毫秒必须带一位小数（整毫秒会抹平小画布的差异）：" + cheap);

        // 478/479 的真机行（240x240）：样本太小 → 判据必须写「样本太小」，不许写更便宜
        String tiny = BenchMath.line(426, 8, 16, 240, 240, 900000L, 450000L, 40000L);
        ok(tiny.contains("WebP=0.9ms") && tiny.contains("JPEG=0.5ms"), "小数格式不对：" + tiny);
        ok(tiny.contains("合计=0.5ms"), "合计必须用纳秒相加再舍入：" + tiny);
        ok(tiny.contains("省=46%"), "亚毫秒样本的百分比必须用纳秒算（46%，不是 100%）：" + tiny);
        ok(tiny.contains("判据=样本太小"), "WebP<1ms 时必须判「样本太小」：" + tiny);
        ok(!tiny.contains("省=100%"), "亚毫秒样本不许报 100%：" + tiny);

        String expensive = BenchMath.line(1, 0, 10, 100, 100, 4000000L, 5000000L, 3000000L);
        ok(expensive.contains("判据=JPEG 这条路不划算"), "不便宜时必须写不划算：" + expensive);
        ok(expensive.contains("省=-100%"), "负数百分比不对：" + expensive);

        String zero = BenchMath.line(1, 0, 1, 100, 100, 0L, 0L, 0L);
        ok(zero.contains("判据=JPEG 这条路不划算"), "WebP 未知（0）时不许判成更便宜：" + zero);
        ok(zero.contains("省=0%"), "基准 0 时应写 0%：" + zero);

        // ---- 跳过行（v1.10.479：不许静默）
        // 门槛 4 万 px：240x240=57.6k 现在**在范围内**（会去测），要测「跳过」得用真小的
        String small = BenchMath.skip(425, 150, 150, 40000, 4000000);
        ok(small.startsWith("解码对拍 跳过="), "跳过行必须以「解码对拍 跳过=」开头：" + small);
        ok(small.contains("跳过=画布太小"), "原因要写清楚是太小：" + small);
        ok(small.contains("组件=425") && small.contains("画布=150x150"),
           "跳过行缺组件/画布：" + small);
        ok(small.contains("像素=22k"), "跳过行要写实际像素数：" + small);
        ok(small.contains("门槛=40k~4M"), "跳过行要写门槛（否则不知道该怎么调）：" + small);

        String huge = BenchMath.skip(7, 3000, 3000, 40000, 4000000);
        ok(huge.contains("跳过=画布太大"), "超大画布的原因写错了：" + huge);
        ok(huge.contains("像素=9000k"), "超大画布的像素数写错了：" + huge);

        if (bad > 0) {
            System.out.println("BENCHMATH FAILED " + bad + "/" + total);
            System.exit(1);
        }
        System.out.println("BENCHMATH OK " + total);
    }

    private static void eq(String what, long want, long got) {
        total++;
        if (want != got) {
            bad++;
            System.out.println("  FAIL " + what + "：want=" + want + " got=" + got);
        }
    }

    private static void eqs(String what, String want, String got) {
        total++;
        if (!want.equals(got)) {
            bad++;
            System.out.println("  FAIL " + what + "：want=" + want + " got=" + got);
        }
    }

    private static void ok(boolean cond, String msg) {
        total++;
        if (!cond) {
            bad++;
            System.out.println("  FAIL " + msg);
        }
    }
}
