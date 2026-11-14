package io.github.animwidget;

/**
 * v1.10.419：**SoC 频率 / 温度探针**（只读，免 root，拿不到就什么都不打）。
 *
 * <p>它回答的是「加线程还有没有用」，而不是"更省电"：
 * <ul>
 *   <li><b>稳态 vs 烘焙期两行对比，均频掉 ≥20% 或温度 ≥55℃</b> → 已经在降频，
 *       此时再加解码线程 = 让同一批核分给更多线程，只会更慢（v1.10.414 实测
 *       烘焙期 `编码CPU=54%` 而 CPU 时间反而涨了 1.7 倍，就是这种形态）；</li>
 *   <li><b>掉 ≤10%</b> → 不是降频，真的是算得慢，加线程才可能有用。</li>
 * </ul>
 *
 * <p>不做任何写入：只读 {@code scaling_cur_freq} 与 {@code thermal_zone*}{@code /temp}，
 * 逐核求和后取平均；最高温取所有 thermal zone 的最大值。
 * 某些 ROM 这两个路径都读不到 —— 那就返回空串，日志里不会多一个字。
 */
final class SocProbe {

    /** 进程内见过的最高均频，用来算「掉了百分之几」（开机后第一次读到就是基线）。 */
    private static long peakAvgMhz = 0L;

    private SocProbe() {
    }

    /** 形如 ` 频率均=2175MHz(掉0%) 温度=38℃`；读不到就返回 ""。 */
    static String sample() {
        try {
            int cores = Runtime.getRuntime().availableProcessors();
            long sum = 0L;
            int n = 0;
            long min = -1L;
            for (int i = 0; i < cores; i++) {
                long khz = readLong("/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_cur_freq");
                if (khz > 0L) {
                    sum += khz;
                    n++;
                    if (min < 0L || khz < min) {
                        min = khz;
                    }
                }
            }
            if (n == 0) {
                return "";
            }
            long avgMhz = sum / n / 1000L;
            if (avgMhz <= 0L) {
                return "";
            }
            if (avgMhz > peakAvgMhz) {
                peakAvgMhz = avgMhz;
            }
            int drop = peakAvgMhz > 0L
                    ? (int) Math.round((peakAvgMhz - avgMhz) * 100.0 / peakAvgMhz) : 0;
            int temp = hottestTempC();
            StringBuilder sb = new StringBuilder(48);
            sb.append(" 频率均=").append(avgMhz).append("MHz(掉").append(drop).append("%)")
                    .append(" 最低=").append(min / 1000L);
            if (temp > 0) {
                sb.append(" 温度=").append(temp).append("℃");
            }
            if (drop >= 20 || temp >= 55) {
                sb.append("（已降频：加线程是负收益，只能降 Σfps）");
            }
            return sb.toString();
        } catch (Throwable th) {
            return "";
        }
    }

    private static long readLong(String path) {
        java.io.BufferedReader reader = null;
        try {
            reader = new java.io.BufferedReader(new java.io.FileReader(path));
            String line = reader.readLine();
            if (line == null) {
                return -1L;
            }
            return Long.parseLong(line.trim());
        } catch (Throwable th) {
            return -1L;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /** 所有 thermal zone 里最热的那个（毫摄氏度 → 摄氏度）。 */
    private static int hottestTempC() {
        long hottest = -1L;
        for (int i = 0; i < 24; i++) {
            long milli = readLong("/sys/class/thermal/thermal_zone" + i + "/temp");
            if (milli > hottest) {
                hottest = milli;
            }
        }
        if (hottest <= 0L) {
            return -1;
        }
        long c = hottest > 1000L ? hottest / 1000L : hottest;   // 有的 ROM 直接给摄氏度
        return c > 200L ? -1 : (int) c;                          // 明显不合理就当没读到
    }
}
