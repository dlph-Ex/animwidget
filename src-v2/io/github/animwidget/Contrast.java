package io.github.animwidget;

import android.graphics.Color;

import org.jetbrains.annotations.NotNull;

/**
 * v1.10.265：**对比度的计算模型**（把「整体拉伸」换成「按角色处理」）。
 *
 * <h3>为什么重写</h3>
 * 旧实现（{@code adjust$map}，v1.10.270 已删）对**每个颜色**做 `c × f`（f = 1 + 对比度×0.035），
 * 相当于以黑为锚点整体提亮/压暗。浅色界面的页面底、卡片、中性变体本来就挤在
 * 近白区（比如 247 / 253 / 255），一乘就全部截断成 255 —— 卡片边界消失、分割线变淡、
 * 层次被"磨平"。深色模式同理（近黑全变 0）。
 *
 * <h3>v1.10.271：**不再移动任何底色**（这是"不动用户参数"的唯一解）</h3>
 * v1.10.265~270 的做法是「卡片钉住、页面底随对比度往下走」，用明度差来表达"高对比"。
 * 这条路有个结构性缺陷 —— 浅色下的三层顺序是：
 * <pre>
 *   卡片 #FFFFFF (lum 1.000) &gt; 页面底 #F7F8FA (0.938) &gt; 中性变体 #E7EAEE (0.820)
 * </pre>
 * 页面底**夹在中间，只比中性变体亮 16 级**。它往下走要去"离开卡片"，就必然**先撞上**
 * 用户的中间色：
 * <pre>
 *   - 0 级：页面底 #F7F8FA，离中性变体 1.136:1
 *   - 5 级：1.087   -10 级：1.040   -15 级：1.007 ← 同色
 *   - 要重新拉开到 1.5:1，得走到 -55 级（页面底 #C0C1C3，整页变中灰）
 * </pre>
 * 于是"页面底随对比度移动"和"用户自己选的中性变体原样保留"**数学上不能同时成立**
 * （用户选什么色我们不知道，任何向下移动都可能正好路过它）。
 *
 * 所以现在：<b>底（页面底 / 卡片 / 中性变体）一律不动，任何对比度档位都逐像素等于用户设置</b>；
 * 对比度只作用在**前景**上 —— 正文、次要文字、以及所有"线条"（分割线 / 未选中描边 /
 * 容器边）。这也正是 WCAG 的口径：高对比说的是"文字/图形与其背景的比值"（1.4.6 正文、
 * 1.4.11 非文字），从来不是"背景要更深"。
 *
 * 目标比值随滑块走（level = -1 ~ +1，0 时**完全不动**）：
 *   · 正文/次要文字：4.5 → 7 → 11
 *   · 线条（分割线/描边）：1.136（原设计）→ 约 2.1 → 约 2.6 —— 由 {@link #line} 把
 *     用户的中性变体朝**正文色**混（正文色永远在"远离卡片"的一侧，所以混过去必然
 *     同时拉开与卡片、与页面底的距离）
 */
public final class Contrast {

    /** 滑块量程：-1.00 ~ +1.00。 */
    public static final float LEVEL_MAX = 1.0f;

    /**
     * v1.10.271：线条（分割线 / 描边）朝正文色混合的最大比例。
     *
     * level=1 时浅色 #E7EAEE → #8E959D（对页面底 2.8:1、对卡片 3.0:1），
     * 正好落在 WCAG 1.4.11「非文字 ≥3:1」附近，而不至于变成一条黑线。
     */
    private static final float LINE_MIX = 0.42f;

    private Contrast() {
    }

    public static float clampLevel(float level) {
        if (level < -LEVEL_MAX) {
            return -LEVEL_MAX;
        }
        return level > LEVEL_MAX ? LEVEL_MAX : level;
    }

    /** WCAG 相对亮度对比度（1 ~ 21）。 */
    public static float ratio(int a, int b) {
        float la = luminance(a);
        float lb = luminance(b);
        return (Math.max(la, lb) + 0.05f) / (Math.min(la, lb) + 0.05f);
    }

    public static float luminance(int color) {
        return (float) (0.2126 * rel(Color.red(color))
                + 0.7152 * rel(Color.green(color))
                + 0.0722 * rel(Color.blue(color)));
    }

    private static double rel(int v) {
        double s = v / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    public static boolean isDarkColor(int color) {
        return luminance(color) < 0.35f;
    }

    /** 同时离两层的最小比值（一个容器要"两层都看得见"）。 */
    public static float minRatio(int color, int a, int b) {
        return Math.min(ratio(color, a), ratio(color, b));
    }

    /** 前景目标比值：正文/次要文字。 */
    public static float textTarget(float level) {
        return 4.5f + 6.5f * Math.max(level, 0.0f);
    }

    /** 前景目标比值：描边 / 分割线。 */
    public static float strokeTarget(float level) {
        return 1.5f + 1.7f * Math.max(level, 0.0f);
    }

    /** 前景目标比值：强调色（主/辅/第三/滑块/错误）。 */
    public static float accentTarget(float level) {
        return 3.0f + 4.0f * Math.max(level, 0.0f);
    }

    /**
     * 把一个前景色朝「远离背景」的方向挪，直到比值达到 `target` 就停。
     *
     * 只做**等量平移**（RGB 同加同减），所以色相基本不变 —— 不会把紫色主色推成黑白；
     * 已经达标的颜色**原样返回**（对比度调高时，正文不会被迫变成纯黑）。
     */
    public static int foreground(int color, int bg, float level, float target) {
        if (level == 0.0f) {
            return color;
        }
        if (level < 0.0f) {
            // 负对比度 = **主动降低分离度**（Material 的 -1 也是这个语义）：
            // 前景朝背景靠，但不至于完全看不见（下限 1.6:1）。
            float want = Math.max(1.6f, 4.5f + 3.0f * level);
            int soft = color;
            for (int i = 0; i < 32; i++) {
                if (ratio(soft, bg) <= want) {
                    break;
                }
                soft = stepToward(soft, bg, 6);
            }
            return soft;
        }
        if (ratio(color, bg) >= target) {
            return color;
        }
        boolean awayFromBlack = isDarkColor(bg);
        int out = color;
        // 步长 3.5%（≈9 级），最多 48 步 —— 足够从任何中间色走到黑或白
        for (int i = 0; i < 48; i++) {
            if (ratio(out, bg) >= target) {
                break;
            }
            out = stepAway(out, awayFromBlack, 9);
        }
        return out;
    }

    /**
     * 保色相混色：`base` 按比例 `k` 朝 `toward` 走（k=0 返回 base，逐像素不变）。
     *
     * 和 {@link #stepAway} 的区别：那是"每步 ±固定级数"，这是"按比例"——
     * 结果连续、不会在滑杆上跳档，适合做"线条随对比度加深"这种参数化量。
     */
    public static int tint(int base, int toward, float k) {
        if (k <= 0.0f) {
            return base;
        }
        float a = k > 1.0f ? 1.0f : k;
        int r = Math.round(Color.red(base) * (1.0f - a) + Color.red(toward) * a);
        int g = Math.round(Color.green(base) * (1.0f - a) + Color.green(toward) * a);
        int b = Math.round(Color.blue(base) * (1.0f - a) + Color.blue(toward) * a);
        return Color.rgb(Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    /**
     * v1.10.271：**线条色**（分割线 / 未选中描边 / 容器边）。
     *
     * 用户的中性变体在浅色下只比页面底暗 16 级（1.136:1）—— 这个"设计值"本身很淡，
     * 但它**永远不会更差**（因为底不再移动，见类注释）。高对比时把这条线朝**正文色**混：
     * 正文色永远在「远离卡片」的一侧，混过去必然同时拉开与卡片、与页面底的距离，
     * 而且色相还留在用户选的色系上（不是硬编码的灰）。
     *
     * `toward` 传的是**已经推过对比度的正文色**，所以深色模式自动往亮走、浅色模式往暗走，
     * 不需要这里再判断模式。
     */
    public static int line(int base, int toward, float level) {
        return tint(base, toward, LINE_MIX * clampLevel(level > 0.0f ? level : 0.0f));
    }

    /**
     * v1.10.271：**五层一次性解算**。
     *
     * ThemeManager.adjust() 与脱机自检（toolchain/ThemeMathTest）调的是**同一个函数** ——
     * 颜色模型只允许有一份实现，否则「门禁算一套、界面画另一套」，
     * 门禁就会在错误的模型上永远 OK（LESSONS 4/86/87 的同一个坑）。
     *
     * <b>表面层（background / surface / neutral）在这里是纯旁路</b>：
     * 原样返回。这不是"忘了算"，而是 v1.10.271 的设计决定 —— 底一旦随对比度移动，
     * 就必然路过用户自己选的中间色（见类注释里的那张表）。只有 text / muted 会被推。
     */
    public static Layers layers(int surfaceBase, int backgroundBase, int neutralBase,
                                int textBase, int mutedBase, float level) {
        float lv = clampLevel(level);
        int text = foreground(textBase, surfaceBase, lv, targetFor("text", lv));
        int muted = foreground(mutedBase, surfaceBase, lv, targetFor("textMuted", lv));
        return new Layers(backgroundBase, surfaceBase, neutralBase, text, muted);
    }

    /** {@link #layers} 的返回值（不可变，五个角色各一个）。 */
    public static final class Layers {
        public final int background;
        public final int surface;
        public final int neutral;
        public final int text;
        public final int muted;

        Layers(int background, int surface, int neutral, int text, int muted) {
            this.background = background;
            this.surface = surface;
            this.neutral = neutral;
            this.text = text;
            this.muted = muted;
        }
    }

    /** 朝背景色靠一步（负对比度用）。 */
    public static int stepToward(int color, int bg, int step) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int tr = Color.red(bg);
        int tg = Color.green(bg);
        int tb = Color.blue(bg);
        r = r < tr ? Math.min(tr, r + step) : Math.max(tr, r - step);
        g = g < tg ? Math.min(tg, g + step) : Math.max(tg, g - step);
        b = b < tb ? Math.min(tb, b + step) : Math.max(tb, b - step);
        return Color.rgb(r, g, b);
    }

    /** 往远离背景的方向挪一步（深底变亮 / 浅底变暗）。 */
    public static int stepAway(int color, boolean towardWhite, int step) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        if (towardWhite) {
            r = Math.min(255, r + step);
            g = Math.min(255, g + step);
            b = Math.min(255, b + step);
        } else {
            r = Math.max(0, r - step);
            g = Math.max(0, g - step);
            b = Math.max(0, b - step);
        }
        return Color.rgb(r, g, b);
    }

    /**
     * 按「颜色在界面里的角色」给它算目标比值。`key` 是编辑页里的色键
     * （surface / neutral / text / textMuted / primary / secondary / tertiary / slider / error）。
     */
    public static float targetFor(@NotNull String key, float level) {
        if ("neutral".equals(key)) {
            return strokeTarget(level);
        }
        if ("text".equals(key) || "textMuted".equals(key) || "text_muted".equals(key)) {
            return textTarget(level);
        }
        return accentTarget(level);
    }
}
