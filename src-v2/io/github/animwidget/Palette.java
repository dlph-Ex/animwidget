package io.github.animwidget;

import android.graphics.Color;

import org.jetbrains.annotations.NotNull;

/**
 * v1.10.70：**简单变体 = 只存一个「种子色」，其余颜色自动算出来。**
 *
 * 规则（Material 3 的 TonalSpot 方案，这里用 HSV 做等价近似 —— 本应用零第三方依赖，
 * 不能引 material-color-utilities 的 CAM16）：
 *
 *   • 主色   = 种子色本身
 *   • 辅助色 = 同色相，彩度调低（×0.36）
 *   • 第三色 = 色相 +60°，彩度居中（×0.62）
 *   • 表面   = 同色相，彩度 ≤6%，亮度按深浅色模式取（暗 0.10 / 亮 0.98）
 *   • 中性变体 = 同色相，彩度 ≤8%，亮度（暗 0.22 / 亮 0.90）
 *   • 滑块色 = 主色
 *   • 对比度 = contrastLevel（-10~10 → -1000~1000 的 ×100 整数）把表面/中性变体往外推一点
 *
 * 预设只存种子色，渲染时再算；用户自存的方案仍然把 6 个颜色全存下来
 * （因为用户可能手动改过某个槽位，不能从种子还原）。
 */
final class Palette {

    static final int PRIMARY = 0;
    static final int SECONDARY = 1;
    static final int TERTIARY = 2;
    static final int SURFACE = 3;
    static final int NEUTRAL = 4;
    static final int SLIDER = 5;

    /** 简单变体的 12 个种子（只存这一行数字，其余全靠 fromSeed 算）。 */
    static final int[] SEEDS = {
            0xFFE53425, 0xFF7A1414, 0xFF7B3F14, 0xFFEF8A2E, 0xFFFAF550, 0xFFA3DC58,
            0xFF2E7D32, 0xFF00897B, 0xFF1E88E5, 0xFF3949AB, 0xFF8E24AA, 0xFFD81B60
    };

    private Palette() {
    }

    /**
     * @param seed         种子色（ARGB）
     * @param dark         当前是不是深色模式
     * @param contrastX100 对比度 ×100（-1000 ~ 1000）
     * @return {主色, 辅助色, 第三色, 表面, 中性变体, 滑块色}
     */
    @NotNull
    static int[] fromSeed(int seed, boolean dark, int contrastX100) {
        float[] hsv = new float[3];
        Color.colorToHSV(seed | 0xFF000000, hsv);
        float h = hsv[0];
        float s = hsv[1];
        float v = hsv[2];
        float k = clamp(contrastX100 / 1000.0f, -1.0f, 1.0f);

        // v1.10.148（反馈 2）：**深色/浅色用同一套亮度**，不再按模式各调各的。
        // 上一版浅色压到 0.86、深色保持 1.0，两边观感差一截；现在统一 0.94 —— 比原色沉一点
        // （白底上不发荧光），但不会发闷。色相/饱和度不动，点选后填进槽位的也是这套色。
        float vScale = 0.94f;
        float sScale = 1.0f;
        int primary = tone(seed | 0xFF000000, vScale, sScale);
        int secondary = tone(rgb(h, clamp(s * 0.36f, 0.06f, 0.9f), clamp(v * 0.95f, 0.10f, 1.0f)), vScale, sScale);
        int tertiary = tone(rgb((h + 60.0f) % 360.0f, clamp(s * 0.62f, 0.10f, 0.95f), clamp(v * 0.98f, 0.10f, 1.0f)), vScale, sScale);

        float surfaceV = dark
                ? clamp(0.10f - 0.04f * k, 0.02f, 0.26f)
                : clamp(0.98f - 0.03f * k, 0.84f, 1.00f);
        float neutralV = dark
                ? clamp(0.22f - 0.05f * k, 0.06f, 0.40f)
                : clamp(0.90f + 0.05f * k, 0.70f, 0.99f);
        int surface = rgb(h, Math.min(s, 0.06f), surfaceV);
        int neutral = rgb(h, Math.min(s, 0.08f), neutralV);

        return new int[]{primary, secondary, tertiary, surface, neutral, primary};
    }

    /** 从 6 色里取出某个槽位对应的颜色（槽位名与 ThemePrefs 的 key 一致）。 */
    static int slot(@NotNull int[] palette, @NotNull String key) {
        if ("secondary".equals(key)) {
            return palette[SECONDARY];
        }
        if ("tertiary".equals(key)) {
            return palette[TERTIARY];
        }
        if ("surface".equals(key)) {
            return palette[SURFACE];
        }
        if ("neutral".equals(key)) {
            return palette[NEUTRAL];
        }
        if ("slider".equals(key)) {
            return palette[SLIDER];
        }
        return palette[PRIMARY];
    }

    /** 按比例调亮度/饱和（色相不动）。 */
    private static int tone(int color, float vScale, float sScale) {
        if (vScale == 1.0f && sScale == 1.0f) {
            return color;
        }
        float[] hsv = new float[3];
        Color.colorToHSV(color | 0xFF000000, hsv);
        hsv[1] = clamp(hsv[1] * sScale, 0.0f, 1.0f);
        hsv[2] = clamp(hsv[2] * vScale, 0.02f, 1.0f);
        return Color.HSVToColor(hsv);
    }

    private static int rgb(float h, float s, float v) {
        return Color.HSVToColor(new float[]{Math.max(0.0f, Math.min(360.0f, h)),
                Math.max(0.0f, Math.min(1.0f, s)), Math.max(0.0f, Math.min(1.0f, v))});
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }
}
