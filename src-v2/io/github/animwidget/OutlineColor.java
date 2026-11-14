package io.github.animwidget;

/**
 * v1.10.460：预览虚线框的**取色判据**（纯计算，无 android 依赖，可离线自检）。
 *
 * <p>问题（用户实测）：虚线颜色写死成 `#66707a9a`（半透明蓝灰），壁纸刚好也是蓝灰调时
 * 对比度极低，「肉眼须仔细才能看出来」——等于没有框。
 *
 * <p>解法（用户指定）：取**壁纸所有像素里占比最大的那一档颜色**（"颜色领域最大"），
 * 用它 RGB 的相反值画虚线。这样框永远落在壁纸主色的对立面：
 * 壁纸越蓝越亮，框就越橙越暗，不需要知道壁纸是什么内容。
 *
 * <p>为什么要在 5 位/通道（32×32×32 桶）里统计：壁纸是照片，同一块区域的颜色也有细微噪声，
 * 逐色统计会碎成几万个只出现一两次的颜色，"最大"就没有意义。5 位量化后同色系会聚成一桶，
 * 再在桶内求平均 —— 既稳又便宜（48×48 采样 = 2304 像素，< 1ms）。
 */
final class OutlineColor {

    /** 兜底的虚线色（壁纸读不到时用，与旧 XML 一致）。 */
    static final int FALLBACK = 0x66707A9A;

    private OutlineColor() {
    }

    /**
     * 取「颜色领域最大」：量化到 5 位/通道后出现次数最多的那一桶，桶内取平均。
     *
     * @param argb 采样像素（ARGB_8888）；alpha &lt; 8 的透明像素不参战
     * @return 0xRRGGBB 形式的主色；没有有效像素返回 -1
     */
    static int dominant(int[] argb) {
        if (argb == null || argb.length == 0) {
            return -1;
        }
        java.util.HashMap<Integer, long[]> buckets = new java.util.HashMap<>();
        for (int px : argb) {
            int a = (px >>> 24) & 0xff;
            if (a < 8) {
                continue;   // 透明像素（壁纸边缘/圆角外）不算
            }
            int r = (px >> 16) & 0xff;
            int g = (px >> 8) & 0xff;
            int b = px & 0xff;
            int key = ((r >> 3) << 10) | ((g >> 3) << 5) | (b >> 3);
            Integer k = Integer.valueOf(key);
            long[] acc = buckets.get(k);
            if (acc == null) {
                buckets.put(k, new long[]{1L, r, g, b});
            } else {
                acc[0]++;
                acc[1] += r;
                acc[2] += g;
                acc[3] += b;
            }
        }
        long best = 0L;
        long[] bestAcc = null;
        for (long[] acc : buckets.values()) {
            if (acc[0] > best) {
                best = acc[0];
                bestAcc = acc;
            }
        }
        if (bestAcc == null || bestAcc[0] <= 0L) {
            return -1;
        }
        int r = (int) (bestAcc[1] / bestAcc[0]);
        int g = (int) (bestAcc[2] / bestAcc[0]);
        int b = (int) (bestAcc[3] / bestAcc[0]);
        return ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
    }

    /** 相反值：每通道取反，**不透明**（虚线必须实心才看得见）。 */
    static int inverse(int rgb) {
        int r = 255 - ((rgb >> 16) & 0xff);
        int g = 255 - ((rgb >> 8) & 0xff);
        int b = 255 - (rgb & 0xff);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * 壁纸主色 → 虚线色。壁纸读不到（-1）时退回 {@link #FALLBACK}。
     *
     * <p>用户口径是"用主色的相反值"，所以这里不做任何"亮度补偿"之类的自作主张 ——
     * 唯一做的事是把结果强制成不透明。
     */
    static int outlineFor(int dominantRgb) {
        if (dominantRgb < 0) {
            return FALLBACK;
        }
        return inverse(dominantRgb);
    }

    // ------------------------------------------------------------------ 默认方案（v1.10.462）

    /**
     * 默认方案的**固定中性灰底**（用户指定：不取反色，色相始终是中性灰）。
     * 128/128/128 是中性灰的中点，往两边都有足够行程。
     */
    static final int NEUTRAL_GRAY = 0x808080;

    /** 暗背景提亮的比例（相对"到纯白还剩多少"）。 */
    static final float BRIGHTEN_K = 0.55f;
    /** 亮背景压暗的比例（相对"这一档灰本身的多少"）。 */
    static final float DARKEN_K = 0.55f;
    /** 亮度阈值：L ≥ 0.5 视为亮背景（用户口径）。 */
    static final float L_THRESHOLD = 0.5f;

    /**
     * 背景的相对亮度 L（0~1）。
     *
     * <p>用 WCAG 的口径：先按 sRGB 反伽马线性化，再取 0.2126R + 0.7152G + 0.0722B。
     * 不用「(r+g+b)/3」—— 那个在深蓝和深绿上会给出一模一样的亮度，人眼完全不是这样。
     */
    static float luminance(int rgb) {
        double r = channel(((rgb >> 16) & 0xff) / 255.0);
        double g = channel(((rgb >> 8) & 0xff) / 255.0);
        double b = channel((rgb & 0xff) / 255.0);
        return (float) (0.2126 * r + 0.7152 * g + 0.0722 * b);
    }

    private static double channel(double c) {
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    /**
     * **默认方案**：固定中性灰底 + 按背景亮度整体提亮/压暗。
     *
     * <p>用户口径（原文）：基础色固定一个中性灰；参考背景是虚线框所在位置的实际背景；
     * 判据是背景亮度 L（0~1）：L ≥ 0.5（亮背景）→ 灰框整体压暗，L &lt; 0.5（暗背景）→ 整体提亮；
     * **不取反色，只在灰色上做明暗偏移，色相始终中性灰**。
     *
     * @param backgroundRgb 虚线框所在位置的背景色（0xRRGGBB）；&lt; 0 表示读不到 → 直接用中性灰
     * @return 不透明的虚线色，r == g == b（恒为中性灰）
     */
    static int defaultFor(int backgroundRgb) {
        // ⚠️ 这里必须用**单通道值** 0x80，不能用 NEUTRAL_GRAY（0x808080 是三通道打包值，
        // 拿它做乘法会瞬间溢出到 255 —— 自检当场抓到：「亮背景压暗」变成了纯白）。
        final int base = 0x80;
        // -1 = 读不到背景。注意这里**不能**用 `< 0` 判：ARGB 形式（0xFFxxxxxx）本身就是负数，
        // 用 `< 0` 会把「亮白背景」误判成「读不到」，永远只给中性灰（自检当场抓到过这个错）。
        if (backgroundRgb == -1) {
            return 0xFF000000 | NEUTRAL_GRAY;
        }
        int bg = backgroundRgb & 0xFFFFFF;
        int v;
        if (luminance(bg) >= L_THRESHOLD) {
            v = base * 45 / 100;                     // 亮背景 → 压暗到 45%（128 → 57）
        } else {
            v = base + (255 - base) * 55 / 100;      // 暗背景 → 提亮到 82%（128 → 197）
        }
        v = Math.max(0, Math.min(255, v));
        return 0xFF000000 | (v << 16) | (v << 8) | v;
    }

    // ------------------------------------------------------------------ 三档口径（v1.10.462）

    /** 预览虚线色取色方式（语义色编辑页的预设组件三选一）。 */
    enum Mode {
        /** 跟随壁纸：壁纸主色的相反值（v1.10.460 的现有方案）。 */
        FOLLOW_WALLPAPER,
        /** 默认：中性灰按背景亮度整体提亮/压暗，**不取反**。 */
        DEFAULT,
        /** 自定义：用户选的颜色（不再随壁纸变）。 */
        CUSTOM
    }

    /**
     * 唯一入口：三档口径收敛成一个颜色。UI 只调这个，不自己分支 ——
     * 否则「预设组件选了什么」和「实际画成什么」迟早对不上。
     *
     * @param mode       档位
     * @param backgroundRgb 虚线框所在位置的背景色（0xRRGGBB 或 ARGB；-1 = 读不到）
     * @param customRgb  自定义颜色（仅 CUSTOM 用；其余档忽略）
     */
    static int resolve(@org.jetbrains.annotations.NotNull Mode mode, int backgroundRgb, int customRgb) {
        switch (mode) {
            case FOLLOW_WALLPAPER:
                return outlineFor(backgroundRgb == -1 ? -1 : (backgroundRgb & 0xFFFFFF));
            case CUSTOM:
                // 自定义必须是实色：用户选的颜色如果带 alpha，虚线在亮壁纸上会"变淡到看不见"
                return 0xFF000000 | (customRgb & 0xFFFFFF);
            case DEFAULT:
            default:
                return defaultFor(backgroundRgb);
        }
    }

    /** 日志用：`#RRGGBB`。 */
    static String hex(int rgb) {
        return String.format(java.util.Locale.US, "#%06X", rgb & 0xFFFFFF);
    }
}
