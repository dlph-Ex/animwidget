package io.github.animwidget;

/**
 * 一套解析后的主题色（已过 invert + 对比度）。
 *
 * v1.10.47：新增第 10 个字段 slider（滑块色）—— 内置动图选择器右侧滑块用它，
 * 默认取辅助色，可单独在「语义色」里改。
 *
 * v1.10.203：新增 4 个字段 error / onError / errorContainer / onErrorContainer。
 *
 * 为什么要加：全应用所有「破坏性 / 失败」的表达（删除按钮、确认弹窗的强调色、错误文字）
 * 一直是**硬编码的 #B3261E**（三处：danger 按钮、动作条删除键、确认弹窗）。
 * 换配色方案时它是唯一一个「不跟着变」的颜色 —— 用户把主色拖成紫色，删除键还是砖红。
 *
 * 分工（这一条决定了「永远可读」）：
 *   · **error / errorContainer 是用户可调的**（语义色面板里两行；也是方案存档的一部分）；
 *   · **onError / onErrorContainer 永远是推导出来的**（{@code ThemeManager.onColor} 取黑或白），
 *     所以无论用户把错误色拖成什么、无论开不开反转 / 对比度，压在它上面的字都读得清。
 *     这两个字段在结构里保留，是因为绘制方需要成对取用，不该各自去算。
 */
public final class ThemeColors {
    final int background;
    final int surface;
    final int neutral;
    final int primary;
    final int secondary;
    final int tertiary;
    final int text;
    final int textMuted;
    final int onPrimary;
    final int slider;
    final int error;
    final int onError;
    final int errorContainer;
    final int onErrorContainer;

    public int component1() {
        return this.background;
    }

    public int component2() {
        return this.surface;
    }

    public int component3() {
        return this.neutral;
    }

    public int component4() {
        return this.primary;
    }

    public int component5() {
        return this.secondary;
    }

    public int component6() {
        return this.tertiary;
    }

    public int component7() {
        return this.text;
    }

    public int component8() {
        return this.textMuted;
    }

    public int component9() {
        return this.onPrimary;
    }

    public int component10() {
        return this.slider;
    }

    public int component11() {
        return this.error;
    }

    public int component12() {
        return this.onError;
    }

    public int component13() {
        return this.errorContainer;
    }

    public int component14() {
        return this.onErrorContainer;
    }

    public ThemeColors copy(int background, int surface, int neutral, int primary, int secondary,
                            int tertiary, int text, int textMuted, int onPrimary, int slider,
                            int error, int onError, int errorContainer, int onErrorContainer) {
        return new ThemeColors(background, surface, neutral, primary, secondary, tertiary,
                text, textMuted, onPrimary, slider, error, onError, errorContainer, onErrorContainer);
    }

    public static ThemeColors copy$default(ThemeColors c, int background, int surface, int neutral,
                                           int primary, int secondary, int tertiary, int text,
                                           int textMuted, int onPrimary, int slider,
                                           int error, int onError, int errorContainer,
                                           int onErrorContainer, int mask, Object obj) {
        if ((mask & 1) != 0) {
            background = c.background;
        }
        if ((mask & 2) != 0) {
            surface = c.surface;
        }
        if ((mask & 4) != 0) {
            neutral = c.neutral;
        }
        if ((mask & 8) != 0) {
            primary = c.primary;
        }
        if ((mask & 16) != 0) {
            secondary = c.secondary;
        }
        if ((mask & 32) != 0) {
            tertiary = c.tertiary;
        }
        if ((mask & 64) != 0) {
            text = c.text;
        }
        if ((mask & 128) != 0) {
            textMuted = c.textMuted;
        }
        if ((mask & 256) != 0) {
            onPrimary = c.onPrimary;
        }
        if ((mask & 512) != 0) {
            slider = c.slider;
        }
        if ((mask & 1024) != 0) {
            error = c.error;
        }
        if ((mask & 2048) != 0) {
            onError = c.onError;
        }
        if ((mask & 4096) != 0) {
            errorContainer = c.errorContainer;
        }
        if ((mask & 8192) != 0) {
            onErrorContainer = c.onErrorContainer;
        }
        return c.copy(background, surface, neutral, primary, secondary, tertiary, text, textMuted,
                onPrimary, slider, error, onError, errorContainer, onErrorContainer);
    }

    public String toString() {
        return "ThemeColors(background=" + this.background + ", surface=" + this.surface
                + ", neutral=" + this.neutral + ", primary=" + this.primary
                + ", secondary=" + this.secondary + ", tertiary=" + this.tertiary
                + ", text=" + this.text + ", textMuted=" + this.textMuted
                + ", onPrimary=" + this.onPrimary + ", slider=" + this.slider
                + ", error=" + this.error + ", onError=" + this.onError
                + ", errorContainer=" + this.errorContainer
                + ", onErrorContainer=" + this.onErrorContainer + ')';
    }

    public int hashCode() {
        // 与 Kotlin data class 等价（(h*31 + c) 逐个滚），只是写成一行一步，方便数清 14 个字段
        int result = Integer.hashCode(this.background);
        result = (result * 31) + Integer.hashCode(this.surface);
        result = (result * 31) + Integer.hashCode(this.neutral);
        result = (result * 31) + Integer.hashCode(this.primary);
        result = (result * 31) + Integer.hashCode(this.secondary);
        result = (result * 31) + Integer.hashCode(this.tertiary);
        result = (result * 31) + Integer.hashCode(this.text);
        result = (result * 31) + Integer.hashCode(this.textMuted);
        result = (result * 31) + Integer.hashCode(this.onPrimary);
        result = (result * 31) + Integer.hashCode(this.slider);
        result = (result * 31) + Integer.hashCode(this.error);
        result = (result * 31) + Integer.hashCode(this.onError);
        result = (result * 31) + Integer.hashCode(this.errorContainer);
        result = (result * 31) + Integer.hashCode(this.onErrorContainer);
        return result;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ThemeColors)) {
            return false;
        }
        ThemeColors c = (ThemeColors) other;
        return this.background == c.background && this.surface == c.surface
                && this.neutral == c.neutral && this.primary == c.primary
                && this.secondary == c.secondary && this.tertiary == c.tertiary
                && this.text == c.text && this.textMuted == c.textMuted
                && this.onPrimary == c.onPrimary && this.slider == c.slider
                && this.error == c.error && this.onError == c.onError
                && this.errorContainer == c.errorContainer
                && this.onErrorContainer == c.onErrorContainer;
    }

    public ThemeColors(int background, int surface, int neutral, int primary, int secondary,
                       int tertiary, int text, int textMuted, int onPrimary, int slider,
                       int error, int onError, int errorContainer, int onErrorContainer) {
        this.background = background;
        this.surface = surface;
        this.neutral = neutral;
        this.primary = primary;
        this.secondary = secondary;
        this.tertiary = tertiary;
        this.text = text;
        this.textMuted = textMuted;
        this.onPrimary = onPrimary;
        this.slider = slider;
        this.error = error;
        this.onError = onError;
        this.errorContainer = errorContainer;
        this.onErrorContainer = onErrorContainer;
    }

    public int getBackground() {
        return this.background;
    }

    public int getSurface() {
        return this.surface;
    }

    public int getNeutral() {
        return this.neutral;
    }

    public int getPrimary() {
        return this.primary;
    }

    public int getSecondary() {
        return this.secondary;
    }

    public int getTertiary() {
        return this.tertiary;
    }

    public int getText() {
        return this.text;
    }

    public int getTextMuted() {
        return this.textMuted;
    }

    public int getOnPrimary() {
        return this.onPrimary;
    }

    public int getSlider() {
        return this.slider;
    }

    public int getError() {
        return this.error;
    }

    public int getOnError() {
        return this.onError;
    }

    public int getErrorContainer() {
        return this.errorContainer;
    }

    public int getOnErrorContainer() {
        return this.onErrorContainer;
    }
}
