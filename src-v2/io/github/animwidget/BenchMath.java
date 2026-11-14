package io.github.animwidget;

import java.util.Locale;

/**
 * v1.10.478：{@link DecodeBench} 的**纯逻辑**部分（不碰 android，可脱机自检）。
 *
 * <p>拆出来的理由和 {@link PlaybackContract} 一样：一行日志里最容易悄悄错掉的两件事是
 * <b>中位数取错</b>（取到最大/最小值，于是「省了多少」全靠抖动）和
 * <b>判据写反</b>（合计比 WebP 还大却报「更便宜」）。这两个都能在脱机环境里钉死。</p>
 *
 * <p>v1.10.479：毫秒改成**一位小数**。478 用整毫秒，结果在 240×240 这类小画布上
 * 三个数全被四舍五入成 0~1ms，「省=」就成了噪声（真机日志里 W48 干脆没出现，
 * 因为门槛 10 万 px 把它挡了）。现在：① 门槛降到 4 万 px；② 报小数；
 * ③ 跳过时必须**留一行原因**，不许静默。</p>
 *
 * <p>v1.10.480：**「省%」改成用纳秒算**，并且加了「样本太小」判据。
 * 479 的真机行是 `WebP=0.9ms JPEG=0.5ms 合成=0.0ms 合计=0.5ms 省=100%` ——
 * 100% 是假的：0.5ms 四舍五入成 1、0.45+0.04 舍成 0，于是"合计"变 0。
 * 这种行最危险，因为读的人会得出「JPEG 免费」。现在：WebP &lt; 1ms 时判据直接写
 * `样本太小(WebP<1ms)` —— 这个尺度上量不出任何结论，只能说明"这一代配置太小"。</p>
 */
final class BenchMath {

    private BenchMath() {
    }

    /** 中位数（偶数个取靠前那个）：首尾各甩掉一次抖动，比均值稳得多。 */
    static long median(long[] a) {
        if (a == null || a.length == 0) {
            return 0L;
        }
        long[] c = new long[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        java.util.Arrays.sort(c);
        return c[(c.length - 1) / 2];
    }

    /** 纳秒 → 毫秒（保留一位小数；整数毫秒会把小画布的差异全部抹平）。 */
    static String ms(long ns) {
        if (ns <= 0L) {
            return "0.0";
        }
        return String.format(Locale.US, "%.1f", Double.valueOf(ns / 1000000.0d));
    }

    /** 纳秒 → 整毫秒（只给「省%」算比例用）。 */
    static long ns2ms(long ns) {
        return ns <= 0L ? 0L : Math.round(ns / 1000000.0d);
    }

    /** 省下的百分比（**用纳秒算**；可为负：负 = 换编码反而更贵）。 */
    static long savePercent(long webpNs, long sumNs) {
        if (webpNs <= 0L) {
            return 0L;
        }
        return Math.round(100.0d * (webpNs - sumNs) / (double) webpNs);
    }

    /** 低于这个时长（1ms）的测量没有决策价值：四舍五入与缓存效应都比差异大。 */
    static final long TINY_NS = 1000000L;

    /** 一行测量结果（判据同时写出来，读日志的人不用自己算）。 */
    static String line(int widgetId, int idx, int count, int w, int h,
                       long webpNs, long jpegNs, long blendNs) {
        long sumNs = jpegNs + blendNs;
        String verdict;
        if (webpNs <= 0L) {
            verdict = "JPEG 这条路不划算";            // 基准没测到，不许判成便宜
        } else if (webpNs < TINY_NS) {
            verdict = "样本太小(WebP<1ms，换大画布)";   // 这个尺度上什么结论都下不了
        } else {
            verdict = sumNs < webpNs ? "JPEG+合成更便宜" : "JPEG 这条路不划算";
        }
        return "解码对拍 组件=" + widgetId + " 帧=" + idx + "/" + count + " 画布=" + w + "x" + h
                + " WebP=" + ms(webpNs) + "ms JPEG=" + ms(jpegNs) + "ms 合成=" + ms(blendNs) + "ms"
                + " 合计=" + ms(sumNs) + "ms 省=" + savePercent(webpNs, sumNs) + "%"
                + " 判据=" + verdict;
    }

    /** 跳过时的那一行（静默跳过 = 用户以为坏了，本来就踩过一次）。 */
    static String skip(int widgetId, int w, int h, int minPx, int maxPx) {
        long px = (long) w * (long) h;
        String why = px < minPx ? "画布太小" : (px > maxPx ? "画布太大" : "拿不到尺寸");
        return "解码对拍 跳过=" + why + " 组件=" + widgetId + " 画布=" + w + "x" + h
                + " 像素=" + (px / 1000L) + "k 门槛=" + (minPx / 1000L) + "k~" + (maxPx / 1000000L) + "M"
                + "（这一代量不出解码成本，换大一点的组件/素材再测）";
    }
}
