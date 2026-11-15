package io.github.animwidget;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import java.util.List;
import kotlin.Metadata;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* compiled from: ThemeManager.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��l\n\u0002\u0018\u0002\n\u0002\u0010��\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n��\n\u0002\u0010\u000b\n��\n\u0002\u0010\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010\u0007\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0003\bÆ\u0002\u0018��2\u00020\u0001:\u00012B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J \u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\t2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eH\u0002J\u000e\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0012J\u000e\u0010\u0013\u001a\u00020\u00102\u0006\u0010\u0014\u001a\u00020\u0015J\u0016\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u000eJ\u000e\u0010\n\u001a\u00020\t2\u0006\u0010\u0018\u001a\u00020\u0019J\u000e\u0010\u001b\u001a\u00020\f2\u0006\u0010\u0018\u001a\u00020\u0019J\u000e\u0010\u001c\u001a\u00020\u000e2\u0006\u0010\u0018\u001a\u00020\u0019J\u0010\u0010\u001d\u001a\u00020\u000e2\u0006\u0010\u001e\u001a\u00020\fH\u0002J&\u0010\u001f\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u00192\u0006\u0010 \u001a\u00020\f2\u0006\u0010!\u001a\u00020\f2\u0006\u0010\"\u001a\u00020#J\u0018\u0010$\u001a\u00020\u00102\u0006\u0010%\u001a\u00020\u00152\u0006\u0010&\u001a\u00020\tH\u0002J \u0010'\u001a\u00020\f2\u0006\u0010\u0018\u001a\u00020\u00192\u0006\u0010(\u001a\u00020)2\u0006\u0010*\u001a\u00020\fH\u0002J\u001e\u0010+\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001e\u001a\u00020\f2\u0006\u0010\"\u001a\u00020#J\u0018\u0010,\u001a\u00020\u00102\u0006\u0010%\u001a\u00020-2\u0006\u0010&\u001a\u00020\tH\u0002J\u0018\u0010.\u001a\u00020\u00102\u0006\u0010%\u001a\u00020/2\u0006\u0010&\u001a\u00020\tH\u0002J\u0018\u00100\u001a\u00020\f2\u0006\u0010\u001e\u001a\u00020\f2\u0006\u00101\u001a\u00020#H\u0002R\u0017\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004¢\u0006\b\n��\u001a\u0004\b\u0006\u0010\u0007"}, d2 = {"Lio/github/animwidget/ThemeManager;", "", "()V", "presets", "", "Lio/github/animwidget/ThemeManager$Preset;", "getPresets", "()Ljava/util/List;", "adjust", "Lio/github/animwidget/ThemeColors;", "colors", "contrast", "", "invert", "", "applySystemBars", "", "activity", "Landroid/app/Activity;", "applyTo", "root", "Landroid/view/View;", "chip", "Landroid/graphics/drawable/GradientDrawable;", "context", "Landroid/content/Context;", "selected", "dialogTheme", "isDark", "isLight", "color", "outlined", "fill", "stroke", "radiusDp", "", "paint", "view", "c", "pick", "key", "", "fallback", "rounded", "styleButton", "Landroid/widget/Button;", "styleText", "Landroid/widget/TextView;", "withAlpha", "alpha", "Preset"})
/* loaded from: classes.jar:io/github/animwidget/ThemeManager.class */
public final class ThemeManager {

    @NotNull
    public static final ThemeManager INSTANCE = new ThemeManager();

    @NotNull
    private static final List<Preset> presets = CollectionsKt.listOf(new Preset[]{new Preset(R.string.palette_framesmith, 0xFF22C55E, 0xFF7AA2F7, 0xFF2AC3DE), new Preset(R.string.palette_catppuccin, -7849489, -14784779, -2126307), new Preset(R.string.palette_nord, -10583636, -8281663, -4944211), new Preset(R.string.palette_dracula, -4353031, -7607811, -34362), new Preset(R.string.palette_gruvbox, -2647775, -12221048, -3398627), new Preset(R.string.palette_tokyo, -8740105, -4482313, -8531969), new Preset(R.string.palette_rose, -1347694, -6500392, -3889177)});

    /**
     * v1.10.403：**预设的「角色色」补充表**（error / slider），与 {@link #presets} 同序。
     *
     * <p>为什么单独一张表而不是塞进 {@code Preset}：那个类是 Kotlin data class 编译产物
     * （component1..4 / copy / copy$default / equals / hashCode 一整套），加字段等于把那一整套
     * 手改一遍，任何一个抄漏都会变成"复制预设时丢色"这种静默 bug。这里只加一张只读表，
     * 索引就是预设序号，改动面最小。</p>
     *
     * <p>每行 = { 深色 error, 浅色 error, slider }。规则：</p>
     * <ul>
     *   <li>error 用各家主题自己的「红」（Material 默认那两个 tone 只是兜底）；</li>
     *   <li>slider 没有独立的"滑块色"就取辅助色 secondary —— 这样 0 号预设与老行为逐值等价。</li>
     * </ul>
     */
    private static final int[][] PRESET_ROLES = {
            // v1.10.408：每行扩成 5 列 = {error深, error浅, slider, surface深, neutral深}
            // 只对「深色」生效（浅色模式一律白底），这样正文/次要文字色不需要重新推导，
            // 不会出现"换了预设正文读不清"的风险。0 号预设逐值等于老常量。
            {-19285, -5036514, 0xFF7AA2F7, -14934236, -14012875},
            // Catppuccin Mocha：base #1E1E2E / surface0 #313244，red #F38BA8，sapphire #74C7EC
            {0xFFF38BA8, 0xFFD20F39, 0xFF74C7EC, 0xFF1E1E2E, 0xFF313244},
            // Nord：polar night #2E3440 / #3B4252，red #BF616A，frost #88C0D0
            {0xFFBF616A, 0xFFBF616A, 0xFF88C0D0, 0xFF2E3440, 0xFF3B4252},
            // Dracula：#282A36 / #44475A，red #FF5555，cyan #8BE9FD
            {0xFFFF5555, 0xFFFF5555, 0xFF8BE9FD, 0xFF282A36, 0xFF44475A},
            // Gruvbox dark：#282828 / #3C3836，red #FB4934，blue #83A598
            {0xFFFB4934, 0xFFCC241D, 0xFF83A598, 0xFF282828, 0xFF3C3836},
            // Tokyo Night：#1A1B26 / #292E42，red #F7768E，blue #7AA2F7
            {0xFFF7768E, 0xFFF7768E, 0xFF7AA2F7, 0xFF1A1B26, 0xFF292E42},
            // Rosé Pine：#191724 / #26233A，love #EB6F92，foam #9CCFD8
            {0xFFEB6F92, 0xFFEB6F92, 0xFF9CCFD8, 0xFF191724, 0xFF26233A},
    };

    /** 当前预设的角色色表行（越界回落到 0 号 = 默认，不会崩）。 */
    @NotNull
    private static int[] presetRoles(@NotNull Context context) {
        int i = RangesKt.coerceIn(ThemePrefs.INSTANCE.getPreset(context), 0, PRESET_ROLES.length - 1);
        return PRESET_ROLES[i];
    }

    private ThemeManager() {
    }

    /* compiled from: ThemeManager.kt */
    @Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��\u001e\n\u0002\u0018\u0002\n\u0002\u0010��\n��\n\u0002\u0010\b\n\u0002\b\u000f\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u000e\b\u0086\b\u0018��2\u00020\u0001B%\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0003¢\u0006\u0002\u0010\u0007J\t\u0010\r\u001a\u00020\u0003HÆ\u0003J\t\u0010\u000e\u001a\u00020\u0003HÆ\u0003J\t\u0010\u000f\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0010\u001a\u00020\u0003HÆ\u0003J1\u0010\u0011\u001a\u00020��2\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u0003HÆ\u0001J\u0013\u0010\u0012\u001a\u00020\u00132\b\u0010\u0014\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u0010\u0015\u001a\u00020\u0003HÖ\u0001J\t\u0010\u0016\u001a\u00020\u0017HÖ\u0001R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b\b\u0010\tR\u0011\u0010\u0004\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b\n\u0010\tR\u0011\u0010\u0005\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b\u000b\u0010\tR\u0011\u0010\u0006\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b\f\u0010\t"}, d2 = {"Lio/github/animwidget/ThemeManager$Preset;", "", "labelRes", "", "primary", "secondary", "tertiary", "(IIII)V", "getLabelRes", "()I", "getPrimary", "getSecondary", "getTertiary", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "hashCode", "toString", ""})
    /* loaded from: classes.jar:io/github/animwidget/ThemeManager$Preset.class */
    public static final class Preset {
        private final int labelRes;
        private final int primary;
        private final int secondary;
        private final int tertiary;

        public final int component1() {
            return this.labelRes;
        }

        public final int component2() {
            return this.primary;
        }

        public final int component3() {
            return this.secondary;
        }

        public final int component4() {
            return this.tertiary;
        }

        @NotNull
        public final Preset copy(int labelRes, int primary, int secondary, int tertiary) {
            return new Preset(labelRes, primary, secondary, tertiary);
        }

        public static /* synthetic */ Preset copy$default(Preset preset, int i, int i2, int i3, int i4, int i5, Object obj) {
            if ((i5 & 1) != 0) {
                i = preset.labelRes;
            }
            if ((i5 & 2) != 0) {
                i2 = preset.primary;
            }
            if ((i5 & 4) != 0) {
                i3 = preset.secondary;
            }
            if ((i5 & 8) != 0) {
                i4 = preset.tertiary;
            }
            return preset.copy(i, i2, i3, i4);
        }

        @NotNull
        public String toString() {
            return "Preset(labelRes=" + this.labelRes + ", primary=" + this.primary + ", secondary=" + this.secondary + ", tertiary=" + this.tertiary + ')';
        }

        public int hashCode() {
            int result = Integer.hashCode(this.labelRes);
            return (((((result * 31) + Integer.hashCode(this.primary)) * 31) + Integer.hashCode(this.secondary)) * 31) + Integer.hashCode(this.tertiary);
        }

        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Preset)) {
                return false;
            }
            Preset preset = (Preset) other;
            return this.labelRes == preset.labelRes && this.primary == preset.primary && this.secondary == preset.secondary && this.tertiary == preset.tertiary;
        }

        public Preset(int labelRes, int primary, int secondary, int tertiary) {
            this.labelRes = labelRes;
            this.primary = primary;
            this.secondary = secondary;
            this.tertiary = tertiary;
        }

        public final int getLabelRes() {
            return this.labelRes;
        }

        public final int getPrimary() {
            return this.primary;
        }

        public final int getSecondary() {
            return this.secondary;
        }

        public final int getTertiary() {
            return this.tertiary;
        }
    }

    @NotNull
    public final List<Preset> getPresets() {
        return presets;
    }

    public final boolean isDark(@NotNull Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
        switch (ThemePrefs.INSTANCE.getMode(context)) {
            case 1:
                return false;
            case 2:
                return true;
            default:
                // 跟随系统：以系统资源为准。Activity 的 base context 被 LocaleHelper 包裹过，
                // 它的 Configuration 快照在某些 ROM 上会停留在创建时的 uiMode，"或"运算会把页面永久锁在深色。
                int systemUiMode = Resources.getSystem().getConfiguration().uiMode;
                if ((systemUiMode & 48) != 0) {
                    return (systemUiMode & 48) == 32;
                }
                return (context.getResources().getConfiguration().uiMode & 48) == 32;
        }
    }

    /**
     * v1.10.266：**拖动时的临时对比度**（null = 用存储值）。
     *
     * 拖动滑杆要「实时看着颜色变」，但每一拍都写盘再整页重建太重（`refreshThemeInPlace`
     * 会重建性能卡/方案网格/色行，60fps 下必然卡）。所以拆成两层：
     *   · 拖动中：这里放一个**临时值**，页面只做 `applyTo`（纯换色，不动结构）；
     *   · 松手后：置 null，再走一次完整刷新。
     */
    private static volatile Float previewContrast = null;

    public static void setPreviewContrast(@Nullable Float level) {
        previewContrast = level;
    }

    @Nullable
    public static Float previewContrast() {
        return previewContrast;
    }

    /** 当前**生效**的对比度：拖动中的临时值优先，否则读存储。 */
    public static float contrastOf(@NotNull Context context) {
        Float p = previewContrast;
        return p != null ? p.floatValue() : ThemePrefs.INSTANCE.getContrast(context);
    }

    @NotNull
    public final ThemeColors colors(@NotNull Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
        ThemeColors raw = rawColors(context);
        return adjust(raw, contrastOf(context), ThemePrefs.INSTANCE.getInvert(context));
    }

    /**
     * v1.10.47：未经「反转 + 对比度」处理的原始配色。
     * 「保存自定义方案」必须存这一份 —— 存 adjust 之后的值会导致对比度被叠加两次。
     */
    @NotNull
    public final ThemeColors rawColors(@NotNull Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
        boolean dark = isDark(context);
        Preset preset = presets.get(RangesKt.coerceIn(ThemePrefs.INSTANCE.getPreset(context), 0, presets.size() - 1));
        int primary = pick(context, "primary", preset.getPrimary());
        int secondary = pick(context, "secondary", preset.getSecondary());
        int tertiary = pick(context, "tertiary", preset.getTertiary());
        // v1.10.408：预设自带深色底色（浅色模式仍是一律白底）。
        // 用户手动改过的底色优先级更高（下面 liveBase 那一层照旧）。
        int[] roles = presetRoles(context);
        int surfaceBase = dark ? (roles.length > 3 ? roles[3] : -14934236) : -1;
        int neutralBase = dark ? (roles.length > 4 ? roles[4] : -14012875) : -1578258;
        // v1.10.202：底色（表面 / 中性变体）走「按模式各一份」的**唯一通道**：
        //   surface / neutral       = 当前生效值（配色方案存档投影过来，或用户刚改的那次）
        //   surface_d / neutral_d   = 深色模式下该用哪个
        //   surface_l / neutral_l   = 浅色模式下该用哪个
        // 这三个位置**只有两个写入口**：激活配色方案（ThemePrefs.applyBaseSnapshot）、
        // 用户改底色（ThemePrefs.applyBaseEdit）。谁都不许只写一半。
        int surfaceLive = liveBase(context, "surface", dark);
        int surface = surfaceLive != 0 ? surfaceLive : surfaceBase;
        int neutralLive = liveBase(context, "neutral", dark);
        int neutral = neutralLive != 0 ? neutralLive : neutralBase;
        // v1.10.47：滑块色默认取辅助色
        // v1.10.403：预设现在带自己的滑块色（没有就用 secondary，与老行为等价）
        int slider = pick(context, "slider", presetRoles(context)[2] != 0 ? presetRoles(context)[2] : secondary);
        // v1.10.203：错误色（破坏性 / 失败）。**只有一个可编辑值**，
        // 「错误提示底色」「on 色」全部由它和当前表面推出来：
        //   onError          = 黑或白（onColor）
        //   errorContainer   = 错误色按 16% 压在表面上（永远是一层"底"）
        //   onErrorContainer = 黑或白（onColor）
        // 默认取 Material 3 的 error（深色 tone 80 / 浅色 tone 40）。
        // v1.10.403：预设带自己的错误色（深浅各一），用户改过的值仍然优先
        int error = pick(context, "error", dark ? roles[0] : roles[1]);
        int errorContainer = blendOver(error, surface, ERROR_CONTAINER_ALPHA);
        int background = dark ? -15658216 : -526086;
        int text = dark ? -1710101 : -14735049;
        int muted = dark ? -6511697 : -9735552;
        return new ThemeColors(background, surface, neutral, primary, secondary, tertiary, text, muted,
                isLight(primary) ? -15658216 : -1, slider,
                error,
                // 派生值先按未反转/未调对比度算一遍，adjust 会用**最终**的 error 重算这几个
                bestOnColor(error),
                errorContainer,
                bestOnColor(errorContainer));
    }

    /** 某个模式下「用户要的底色」；0 = 没有覆盖，用预设默认值。 */
    public final int liveBase(@NotNull Context context, @NotNull String key, boolean dark) {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(key, "key");
        // ① 本模式自己的那一份（v1.10.201 起按模式分开存：深色下选纯白、浅色下选纯黑互不干扰）
        int scoped = ThemePrefs.INSTANCE.getCustom(context, key + (dark ? "_d" : "_l"));
        if (scoped != 0) {
            return scoped;
        }
        int flat = ThemePrefs.INSTANCE.getCustom(context, key);
        if (flat == 0) {
            return 0;
        }
        // ② 老数据（v1.10.200/201 之前只存了一份）：靠「写入时是深色还是浅色」的标记判断；
        //    连标记都没有的（v1.10.199 及以前）只能按明暗方向推断。
        int mode = ThemePrefs.INSTANCE.getCustom(context, key + "_mode");
        if (mode != 0) {
            return mode == (dark ? 1 : 2) ? flat : defaultBase(key, dark);
        }
        return isLight(flat) == dark ? defaultBase(key, dark) : flat;
    }

    /** v1.10.203：错误提示底色 = 错误色压在表面上的比例（Material 的「container」观感）。 */
    private static final float ERROR_CONTAINER_ALPHA = 0.16f;

    /**
     * v1.10.203：把 fg 按 alpha 叠在 bg 上（结果不透明）。
     *
     * 「错误提示底色」用它从错误色推出来 —— 只存一个红，就不会出现两个红互相顶位。
     */
    public static int blendOver(int fg, int bg, float alpha) {
        float a = alpha < 0.0f ? 0.0f : (alpha > 1.0f ? 1.0f : alpha);
        int r = Math.round(android.graphics.Color.red(bg) * (1.0f - a) + android.graphics.Color.red(fg) * a);
        int g = Math.round(android.graphics.Color.green(bg) * (1.0f - a) + android.graphics.Color.green(fg) * a);
        int b = Math.round(android.graphics.Color.blue(bg) * (1.0f - a) + android.graphics.Color.blue(fg) * a);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * v1.10.207：某个语义色**在没有覆盖值时**显示成什么（已经过 invert / 对比度）。
     *
     * 调色面板的「重置」只把**面板里的值**改回这个数（不写存储）；
     * 「保存」时如果值正好等于它，就把覆盖值删掉（= 回到默认），否则写入。
     * 这样「重置 → 取消」什么都不会发生，「重置 → 保存」才真的回默认。
     */
    public final int defaultValue(@NotNull Context context, @NotNull String key) {
        return displayValue(context, key, defaultRawValue(context, key));
    }

    /**
     * v1.10.270：把**存储里的原始色**换算成**界面里真正画出来的色**（按角色，不是通道乘法）。
     *
     * <p>旧实现走 {@code adjust$map}（每个通道 ×(1+对比度×3.5%)）—— 那是 v1.10.265 之前的
     * 整体拉伸。v1.10.268 起身份色不再经过它，于是「面板显示 A、界面画 B」：
     * 用户在对比度=1 时选 #6750A4，存储被反解成 #634E9E，界面又原样画 #634E9E ——
     * **选的和出来的不是一个色，而且每保存一次漂一次**（v1.10.210 修过的病又漏回来了，
     * 因为它当初只是把乘法**反解**了一边，模型换了却没跟着换）。</p>
     *
     * <p>现在这张映射表与 {@link #adjust} 是**同一套算法**：</p>
     * <ul>
     *   <li>身份色（主/辅/第三/滑块/错误）：只 invert，对比度不碰 —— 界面画的就是它；</li>
     *   <li>表面（卡片）：钉住，只 invert；</li>
     *   <li>中性变体 / 页面底 / 正文 / 次要文字：按角色算，与 adjust 完全同源
     *       （{@link Contrast#layers}、{@link Contrast#surface}、{@link Contrast#foreground}）。</li>
     * </ul>
     */
    public final int displayValue(@NotNull Context context, @NotNull String key, int raw) {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(key, "key");
        boolean invert = ThemePrefs.INSTANCE.getInvert(context);
        int v = invertColor(raw, invert);
        float level = Contrast.clampLevel(contrastOf(context));
        if (level == 0.0f || "surface".contentEquals(key)) {
            // 对比度=0：一切都是原色；卡片层永远钉住。
            return v;
        }
        // v1.10.271：表面三层（页面底/卡片/中性变体）在对比度下**一格都不动**，
        // 所以这里只有正文/次要文字需要按角色算。判据与 adjust() 同源。
        ThemeColors base = rawColors(context);
        int surf = invertColor(base.getSurface(), invert);
        if ("text".contentEquals(key) || "textMuted".contentEquals(key)) {
            return Contrast.foreground(v, surf, level, Contrast.targetFor(key, level));
        }
        return v;
    }

    /**
     * v1.10.210：把**面板里选的显示色**反解成**存储用的原始色**。
     *
     * <p>为什么需要：面板里选的是「界面上看到的色」，存储/方案存档存的是**基色**
     * （对比度是显示层再加的一档）。两边不换算就会「选完保存颜色变一点」。</p>
     *
     * <p>两条规则（v1.10.270 重写）：</p>
     * <ol>
     *   <li>现有覆盖值如果**显示出来正好是选的那个色** → 原样保留（不动就不漂）。这一条是
     *       防漂移的主力：打开面板什么都不改就保存，必须逐像素不变。</li>
     *   <li>否则把选中的色直接当基色（反掉 invert）。<b>不再做通道乘法的反解</b> ——
     *       那套乘法在 v1.10.265 就被删了，继续反解它等于用旧模型解释新界面
     *       （v1.10.268 之后身份色不参与对比度，反解反而会把#6750A4 存成 #634E9E）。
     *       对 neutral 这类会被再加工的角色，界面会在你选的基色上再加一档对比度 ——
     *       这是设计（基色是身份、对比度是显示层），且因为规则①，**反复保存不会累积漂移**。</li>
     * </ol>
     */
    public final int rawFromDisplay(@NotNull Context context, @NotNull String key, int display) {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(key, "key");
        int current = ThemePrefs.INSTANCE.getCustom(context, key);
        if (current != 0 && displayValue(context, key, current) == display) {
            return current;
        }
        return invertColor(display, ThemePrefs.INSTANCE.getInvert(context));
    }

    /**
     * v1.10.209：某个语义色**没有覆盖值时的原始色**（不做 invert/对比度）。
     *
     * 「已修改」判定必须用它把「无覆盖(0)」翻译成真正生效的颜色再比较 ——
     * 直接拿 0 去和存档里的颜色比，就会把一个"回到默认"的操作判成「已修改」。
     */
    public final int defaultRawValue(@NotNull Context context, @NotNull String key) {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(key, "key");
        boolean dark = isDark(context);
        Preset preset = presets.get(RangesKt.coerceIn(ThemePrefs.INSTANCE.getPreset(context), 0, presets.size() - 1));
        int raw;
        if ("primary".contentEquals(key)) {
            raw = preset.getPrimary();
        } else if ("secondary".contentEquals(key)) {
            raw = preset.getSecondary();
        } else if ("tertiary".contentEquals(key)) {
            raw = preset.getTertiary();
        } else if ("slider".contentEquals(key)) {
            // 滑块色的默认 = 当前的辅助色（与 rawColors 里的 pick 完全同一来源）
            raw = pick(context, "secondary", preset.getSecondary());
        } else if ("error".contentEquals(key)) {
            raw = dark ? -19285 : -5036514;
        } else if ("surface".contentEquals(key)) {
            raw = defaultBase("surface", dark);
        } else if ("neutral".contentEquals(key)) {
            raw = defaultBase("neutral", dark);
        } else {
            return 0;
        }
        return raw;
    }

    /** 预设默认底色（深色/浅色各一份），与主题其它默认值同源。 */
    public static int defaultBase(@NotNull String key, boolean dark) {
        if ("surface".contentEquals(key)) {
            return dark ? -14934236 : -1;
        }
        return dark ? -14012875 : -1578258;
    }

    /**
     * v1.10.202：把一个**只存了一份**的底色（老方案的存档值）拆成「深色该用哪个 / 浅色该用哪个」。
     *
     * 方向不对（深色模式下却是浅色）→ 该模式用默认值 —— 这就是 v1.10.83 的老规则，
     * 对新存的方案不再需要（它们按模式各存一份），但读老存档时行为保持不变。
     */
    public final int baseForMode(@NotNull String key, int flat, boolean dark) {
        Intrinsics.checkNotNullParameter(key, "key");
        if (flat == 0) {
            return defaultBase(key, dark);
        }
        return isLight(flat) == dark ? defaultBase(key, dark) : flat;
    }

    private final int pick(Context context, String key, int fallback) {
        int stored = ThemePrefs.INSTANCE.getCustom(context, key);
        return stored != 0 ? stored : fallback;
    }

    /**
     * v1.10.199（需求）：副标题色 —— Material 3 的「中性变体 + 对比度自适应」。
     *
     * 规则：
     *   · 基底取**中性变体**（不用主色/辅助色/第三色 —— 那三个是强调色，只给按钮/选中态）；
     *   · 深色从 tone 80 起、浅色从 tone 30 起；
     *   · **不把 tone 写死**：从基底往「远离背景」的方向逐步调，直到与当前背景的
     *     对比度达到 4.5:1（小字下限）就停。
     * 返回的是**不透明色**（从色板取色，不是 alpha 叠在背景上）。
     */
    /**
     * v1.10.271：**线条色的唯一出口**（分割线 / 未选中描边 / 容器边 / 输入框边）。
     *
     * <p>用户的中性变体在浅色下只比页面底暗 16 级（1.136:1）—— 那是设计值，不是 bug，
     * 但也确实"淡"。对比度往上时把这条线朝**正文色**混（{@link Contrast#line}）：
     * 正文色永远在「远离卡片」的一侧，混过去必然同时拉开与卡片、与页面底的距离，
     * 色相还留在用户选的色系里；level=0 时混合系数为 0，**逐像素等于用户设置**。</p>
     *
     * <p>为什么必须走这一个出口：线条是**一维**的（1dp 宽），它没有"填充"可以兜底 ——
     * 一旦线条色和背后的层同色，它就不是"变淡"而是**整条消失**（v1.10.270 之前
     * "分割线变淡/按钮融为一体"都是这个）。所以线条色只能有一份实现，
     * 各处不许再自己拼。</p>
     */
    public final int lineColor(@NotNull Context context, @NotNull ThemeColors c) {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(c, "c");
        return Contrast.line(c.getNeutral(), c.getText(), contrastOf(context));
    }

    public final int subtitleColor(@NotNull Context context) {
        ThemeColors c = colors(context);
        int bg = c.getSurface();
        int fg = c.getNeutral();
        boolean dark = isDark(context);
        for (int i = 0; i < 24 && contrastRatio(fg, bg) < 4.5f; i++) {
            fg = nudgeAway(fg, dark, 0.045f);
        }
        return fg | 0xFF000000;
    }

    /**
     * v1.10.201（反馈）：软胶囊（pill_soft）的底色。
     *
     * 以前直接用 `neutral` 纯色 —— 用户把「中性变体」拉到纯白/纯黑时，胶囊会和卡片同色直接「消失」，
     * 看着像按钮坏了。现在保证与表面至少 6% 亮度差：不够就自动往远离方向推一档。
     */
    private static int pillSoftColor(@NotNull ThemeColors c) {
        int base = c.getNeutral();
        int bg = c.getSurface();
        float want = Math.abs(luminanceOf(base) - luminanceOf(bg));
        int out = base;
        for (int i = 0; i < 8 && want < 0.06f; i++) {
            out = nudgeAway(out, luminanceOf(bg) < 0.5f, 0.06f);
            want = Math.abs(luminanceOf(out) - luminanceOf(bg));
        }
        return out;
    }

    /** 往「远离背景」的方向挪一档亮度（深色更亮 / 浅色更暗）。 */
    private static int nudgeAway(int color, boolean dark, float step) {
        int d = Math.max(1, Math.round(step * 255.0f));
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        if (dark) {
            r = Math.min(255, r + d);
            g = Math.min(255, g + d);
            b = Math.min(255, b + d);
        } else {
            r = Math.max(0, r - d);
            g = Math.max(0, g - d);
            b = Math.max(0, b - d);
        }
        return Color.rgb(r, g, b);
    }

    private static float contrastRatio(int a, int b) {
        float la = luminanceOf(a);
        float lb = luminanceOf(b);
        return (Math.max(la, lb) + 0.05f) / (Math.min(la, lb) + 0.05f);
    }

    private static float luminanceOf(int color) {
        return (float) (0.2126 * relChannel(Color.red(color))
                + 0.7152 * relChannel(Color.green(color))
                + 0.0722 * relChannel(Color.blue(color)));
    }

    private static double relChannel(int v) {
        double s = v / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    private final boolean isLight(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return (((r * 299) + (g * 587)) + (b * 114)) / 1000 > 160;
    }

    /** v1.10.47：给定底色上可读的前景（黑或白）—— 滑块自绘要用。 */
    public final int onColor(int background) {
        return isLight(background) ? -15658216 : -1;
    }

    /**
     * v1.10.203：**错误色这类「用户可以任意改」的底**，前景按「黑/白里对比度更高的那个」取。
     *
     * 与 {@link #onColor} 的区别：onColor 用亮度阈值判断（历史行为，主色按钮一直在用，不动它），
     * 阈值法在**中灰**附近会选到对比度偏低的一侧（例如 #808080 上白字只有 3.9:1）。
     * 错误色是危险动作的最后一道可读性底线，所以这里直接比算出来的比值，取更好的那个。
     */
    public final int bestOnColor(int background) {
        float black = contrastRatio(-15658216, background);
        float white = contrastRatio(-1, background);
        return white >= black ? -1 : -15658216;
    }

    /**
     * v1.10.265：**按角色施加对比度**（替换掉原来「每个颜色乘同一个系数」的整体拉伸）。
     *
     * 旧行为（{@code c × (1 + 对比度×0.035)}）会把浅色界面挤在近白区的页面底/卡片/分割线
     * 一起推到纯白 —— 卡片边界消失、分割线变淡、层次被磨平（用户原话就是这个）。
     *
     * v1.10.271 起的规则见 {@link Contrast} 类注释：**底一律不动（逐像素等于用户设置）**，
     * 对比度只作用于前景 —— 正文/次要文字逐档挪到目标比值就停；
     * 线条（分割线/描边）由 {@link #lineColor} 把中性变体朝正文色混。
     * **对比度 = 0 时直接返回原色**，保证默认外观与旧版逐像素一致（不引入意外回归）。
     */
    private final ThemeColors adjust(ThemeColors colors, float contrast, boolean invert) {
        float level = Contrast.clampLevel(contrast);
        if (level == 0.0f && !invert) {
            return colors;
        }
        int surfRaw = invertColor(colors.getSurface(), invert);
        // v1.10.271：五层解算收敛到 Contrast.layers 一处（脱机自检调的是同一个函数）。
        //
        // **表面层（页面底 / 卡片 / 中性变体）在这里是纯旁路：原样返回。**
        // 这已经是第三次改这个决定了，把三次的原因写清楚，免得下次又"优化"回去：
        //   · v1.10.265：旧模型把近白全推到 255 → 层被磨平。第一次用「页面底往下走」补层差；
        //   · v1.10.270：修「中性变体停在原地被页面底撞上（1.003:1）」→ 让中性变体跟着走。
        //     代价是**用户选的中性变体被改写**（#E7EAEE → #BDC0C4）；
        //   · v1.10.271（当前）：用户明确要求「不动任何用户设置的色值」。
        //     而浅色页面底只比中性变体亮 16 级，**它往下走必经用户那个中间色**
        //     （-15 级时就同色了）。所以"推层"和"保留用户色"数学上不能同时成立 ——
        //     唯一不动的解就是：底一律不推，对比度只作用于前景（文字 + 线条）。
        //     高对比 ≠ 背景更深（WCAG 1.4.6/1.4.11 说的是前景与底的比值）。
        Contrast.Layers L = Contrast.layers(surfRaw,
                invertColor(colors.getBackground(), invert),
                invertColor(colors.getNeutral(), invert),
                invertColor(colors.getText(), invert),
                invertColor(colors.getTextMuted(), invert), level);
        int surface = L.surface;
        int background = L.background;
        // 前景：文字按角色挪到目标比值就停（已够的就不动）
        int text = L.text;
        int muted = L.muted;
        int neutral = L.neutral;
        // v1.10.268：**身份色一律不动**（primary/secondary/tertiary/slider/error/表面三层）。
        // 反面教材：v1.10.265 曾对主色也「朝远离卡片挪」，于是同一个主色在浅色/深色下
        // 变成两种颜色（用户原话：「在深色模式下和浅色模式下是两个不同的色」）。
        int primary = invertColor(colors.getPrimary(), invert);
        int secondary = invertColor(colors.getSecondary(), invert);
        int tertiary = invertColor(colors.getTertiary(), invert);
        int slider = invertColor(colors.getSlider(), invert);
        int error0 = invertColor(colors.getError(), invert);
        int errorContainer = blendOver(error0, surface, ERROR_CONTAINER_ALPHA);
        return colors.copy(background, surface, neutral, primary, secondary, tertiary, text, muted,
                bestOnColor(primary), slider, error0, bestOnColor(error0),
                errorContainer, bestOnColor(errorContainer));
    }

    /** 反转（对比度不再走这里的通道乘法）。 */
    private static int invertColor(int color, boolean invert) {
        if (!invert) {
            return color;
        }
        return Color.rgb(255 - Color.red(color), 255 - Color.green(color), 255 - Color.blue(color));
    }

    /**
     * v1.10.417（需求）：**整屏模糊底**的确认类弹窗 → 非浮动主题（{@link R.style#Theme_AnimWidget_Dialog}）。
     *
     * <p>原来的 `Theme_Material_Dialog_Alert` 是浮动窗：系统会把窗口按「浮动窗」内缩，
     * 模糊底永远盖不到状态栏/导航栏那两条，栏色刷透明也不会变 —— 因为那两条不是这个窗口画的。
     * 换成 `windowIsFloating=false` 的自定义主题后，窗口铺满整屏，
     * `setDecorFitsSystemWindows(false)` + 透明栏色才真的生效。
     *
     * <p>深浅两套都在 `res/values/styles.xml`，`windowLightStatusBar` 分别设 true/false，
     * 状态栏图标才会跟着底色走。
     */
    public final int dialogTheme(@NotNull Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
        return isDark(context) ? R.style.Theme_AnimWidget_Dialog : R.style.Theme_AnimWidget_Dialog_Light;
    }


    public final void applySystemBars(@NotNull Activity activity) {
        WindowInsetsController insetsController;
        Intrinsics.checkNotNullParameter(activity, "activity");
        ThemeColors c = colors(activity);
        boolean dark = isDark(activity);
        Window window = activity.getWindow();
        // v1.10.370：设置页顶部要「全透明」—— 它的内容已经铺到屏幕最上沿
        // （EdgeToEdge.applyScrollUnderBars），状态栏再刷成不透明底色就把这条路堵死了；
        // 而且 35+ 上 setStatusBarColor 本身已废弃。
        //
        // v1.10.393：判据从「是不是设置页」改成「这一页有没有真的铺到边」
        // （EdgeToEdge.isUnderBars）。主页这一版也走同一条路：内容滚到状态栏/导航栏下面，
        // 所以两条栏都刷透明。没有铺到边的页面照旧刷页面底色 —— 透明会让内容钻到栏下面，
        // 那些页面没有为它留内边距。
        boolean edge = EdgeToEdge.INSTANCE.isUnderBars(activity);
        window.setStatusBarColor(edge ? android.graphics.Color.TRANSPARENT : c.getBackground());
        window.setNavigationBarColor(edge ? android.graphics.Color.TRANSPARENT : c.getBackground());
        if (Build.VERSION.SDK_INT >= 29) {
            window.setNavigationBarContrastEnforced(false);
        }
        window.setBackgroundDrawable(new ColorDrawable(c.getBackground()));
        final View decor = window.getDecorView();
        decor.setBackgroundColor(c.getBackground());
        decor.invalidate();
        // 有些 ROM 在系统栏/窗口背景变更后不会立刻重绘，下一帧再补一次。
        decor.post(new Runnable() {
            @Override
            public void run() {
                decor.invalidate();
            }
        });
        if (Build.VERSION.SDK_INT >= 30) {
            insetsController = window.getInsetsController();
        } else {
            insetsController = null;
        }
        WindowInsetsController controller = insetsController;
        if (controller != null) {
            controller.setSystemBarsAppearance(dark ? 0 : 24, 24);
        } else {
            int flags = window.getDecorView().getSystemUiVisibility();
            window.getDecorView().setSystemUiVisibility(dark ? flags & (8208 ^ (-1)) : flags | 8208);
        }
    }

    public final void applyTo(@NotNull View root) {
        Intrinsics.checkNotNullParameter(root, "root");
        Context context = root.getContext();
        Intrinsics.checkNotNullExpressionValue(context, "getContext(...)");
        ThemeColors c = colors(context);
        // 页面底：content 自身 + 内容页的第一层（ScrollView / FrameLayout / LinearLayout）都铺主题底色，
        // 布局里写死的背景色不会再挡住深色模式。
        root.setBackgroundColor(c.getBackground());
        if (root instanceof ViewGroup) {
            ViewGroup content = (ViewGroup) root;
            if (content.getChildCount() > 0) {
                View page = content.getChildAt(0);
                if (page != null && page.getTag() == null) {
                    page.setBackgroundColor(c.getBackground());
                }
            }
        }
        paint(root, c);
        settle(root);
        // v1.10.185：换肤后哪一层跟上了，由「整页上色」这一步自动记一笔（H02）。
        // 只有本次换肤确实在等这个页面时才会打，onCreate 的正常上色不会刷屏。
        try {
            ThemeSwitch.notePainted(root);
        } catch (Throwable ignored) {
        }
    }

    /** 结束残留的 ripple/pressed 动画并重绘整棵树：原地换肤时不会留下半透明的"鬼影"。 */
    public final void settle(@NotNull View view) {
        try {
            view.jumpDrawablesToCurrentState();
        } catch (Throwable ignored) {
        }
        view.invalidate();
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = group.getChildAt(i);
                if (child != null) {
                    settle(child);
                }
            }
        }
    }

    /**
     * v1.10.98：**只按 tag 刷样式**，不动任何背景底 —— 给「代码里后建的子树」用。
     * （applyTo 会把传入节点自身铺成页面底色，套在卡片里的子树上会盖住卡片。）
     */
    public final void styleTree(@NotNull View view) {
        paint(view, colors(view.getContext()));
    }

    /**
     * v1.10.240：SeekBar 的表面色。
     *
     * <p>「滑块色」这个 token 以前只给动图选择器用，设置页里的滑块永远是主色，
     * 于是用户改完滑块色回来看不出任何变化。现在由 {@link ThemePrefs#getSliderScope} 决定：
     * 只有动图选择器要「自定义滑块色」时才用 slider，否则全应用统一用主色（默认）。</p>
     */
    public final int sliderSurfaceColor(@NotNull Context context, @NotNull ThemeColors c) {
        if (context != null && ThemePrefs.INSTANCE.getSliderScope(context) == ThemePrefs.SLIDER_SCOPE_GLOBAL) {
            return c.getSlider();
        }
        return c.getPrimary();
    }

    private final void paint(View view, ThemeColors c) {
        // v1.10.98：换肤/刷新时顺手把字体铺上（幂等，只在新字体的第一次生效）
        Fonts.applyOne(view);

        // v1.10.166（反馈「没有实时更新」）：**自绘控件在这里统一上色**。
        //
        // 自绘控件不继承任何系统属性（没有 textColor / buttonTint 可跟随），
        // 颜色只能靠代码显式下发。之前它们只在**构造时**取一次 ThemeColors，
        // 于是深浅色 / 主色 / 对比度一变，滑块就停在旧色 —— 词元是对的，是刷新入口没接上。
        //
        // 放在这里而不是各页面手动调，是因为所有刷色路径最后都走 paint()：
        //   冷启动 applyTo / 切预设 / 改对比度 / 500ms 深色轮询 / Morph.crossfade 里的原地刷 /
        //   语言切换后重建 —— 一处接上，全部覆盖，不用各处「记得再调一次」。
        //
        // 注意必须 return：LanguageRowView 是 ViewGroup，若继续递归下去，
        // 下面 TextView 分支会把「选中行的主色文字」重新刷成正文色。
        if (view instanceof FontScaleSlider) {
            ((FontScaleSlider) view).applyColors(c);
            return;
        }
        if (view instanceof LanguageRowView) {
            ((LanguageRowView) view).applyTheme(c);
            return;
        }
        // v1.10.202（反馈「切深浅色后性能卡的选项框不亮，点一下才回来」）：
        // 自绘开关在这里统一上色。它以前是「FrameLayout + GradientDrawable」——
        // 一个裸的 ViewGroup 没有 tag，paint() 的默认分支碰不到它，
        // 于是换肤只换了页面，开关框一直停在建视图那一刻的旧配色（深色底上≈看不见）。
        if (view instanceof SwitchBoxView) {
            ((SwitchBoxView) view).applyColors(c);
            return;
        }
        // v1.10.240（需求）：设置页卡片头的线性图标。它替代了原来「TextView 塞 ☾ / ⚡」的做法，
        // 颜色只有一条来源（tag 决定的强调色），接进这里就等于接进了所有换肤路径。
        // v1.10.463/464：语义色编辑页里的分段选择器（虚线色三档 + 滑块色作用范围）。
        // 自绘控件「不进来就停在旧配色」—— 接进这条链才覆盖全部换肤路径。
        // v1.10.464 起全仓库只有这一份分段选择器实现（老的 SliderScopeSwitch 已删除）。
        if (view instanceof SegmentedRow) {
            ((SegmentedRow) view).applyColors(c);
            return;
        }
        if (view instanceof IconTileView) {
            ((IconTileView) view).applyColors(c);
            return;
        }
        // v1.10.523：设置页左上角的返回按钮（自绘圆底 + 箭头）。
        // 和 IconTileView 同一条链：不进来就会停在"建视图那一刻"的配色。
        if (view instanceof BackButton) {
            ((BackButton) view).applyColors(c);
            return;
        }
        // v1.10.221：阴影曲线面板也是自绘的（曲线、控制点、网格全在 onDraw 里现算）。
        // 它和 SwitchBoxView 一样：**颜色不烘焙、不缓存**，接进这条链只为拿到一次 invalidate。
        if (view instanceof ShadowCurveView) {
            ((ShadowCurveView) view).applyColors(c);
            return;
        }
        // v1.10.496：剩余性能卡里的分段余量表（自绘）。和 SwitchBoxView 同一条链：
        // 颜色不烘焙，任何换肤入口（冷启动 applyTo / 切预设 / 改对比度 / 500ms 深色轮询 /
        // 叠化里的原地刷新）都会把这张表重新上色，不会停在「建视图那一刻」的旧配色。
        if (view instanceof PerfMeterView) {
            ((PerfMeterView) view).applyColors(c);
            return;
        }

        if (view instanceof Switch) {
            ((Switch) view).setTextColor(c.getText());
            ((Switch) view).setThumbTintList(ColorStateList.valueOf(c.getPrimary()));
            ((Switch) view).setTrackTintList(ColorStateList.valueOf(withAlpha(c.getPrimary(), 0.38f)));
            return;
        }
        if (view instanceof CheckBox) {
            ((CheckBox) view).setTextColor(c.getText());
            return;
        }
        if (view instanceof RadioButton) {
            RadioButton radio = (RadioButton) view;
            radio.setTextColor(c.getText());
            // v1.10.157（语言页打磨）：单选圆圈跟着主色走，深浅色都一眼可辨
            radio.setButtonTintList(ColorStateList.valueOf(c.getPrimary()));
            return;
        }
        if (!(view instanceof Button)) {
            if (!(view instanceof TextView)) {
                if (view instanceof SeekBar) {
                    // v1.10.240（需求）：滑块表面色的唯一取值口。
                    // 「仅覆盖动图选择器」= 主色（老行为）；「全局」= 滑块色。
                    int barColor = sliderSurfaceColor(view.getContext(), c);
                    ((SeekBar) view).setProgressTintList(ColorStateList.valueOf(barColor));
                    ((SeekBar) view).setThumbTintList(ColorStateList.valueOf(barColor));
                    return;
                }
                if (view instanceof ProgressBar) {
                    ((ProgressBar) view).setIndeterminateTintList(ColorStateList.valueOf(c.getPrimary()));
                    return;
                }
                if (!(view instanceof ImageView)) {
                    if (view instanceof ViewGroup) {
                        if (((ViewGroup) view).getId() != 2131230720 && ((ViewGroup) view).getId() != 2131230723) {
                            if (Intrinsics.areEqual("card", ((ViewGroup) view).getTag())) {
                                Context context = ((ViewGroup) view).getContext();
                                Intrinsics.checkNotNullExpressionValue(context, "getContext(...)");
                                ((ViewGroup) view).setBackground(rounded(context, c.getSurface(), 22.0f));
                            } else if (Intrinsics.areEqual("pill_soft", ((ViewGroup) view).getTag())) {
                                Context context = ((ViewGroup) view).getContext();
                                Intrinsics.checkNotNullExpressionValue(context, "getContext(...)");
                                ((ViewGroup) view).setBackground(rounded(context, pillSoftColor(c), 999.0f));
                            } else if (Intrinsics.areEqual("pill_stack", ((ViewGroup) view).getTag())) {
                                // v1.10.361：「关于 / 复制 QQ」两格合并的竖向块。**不许用 999** ——
                                // 两格叠起来会变成一个椭圆棒，用户原话「不要做那么圆，留个圆角就行」。
                                Context context = ((ViewGroup) view).getContext();
                                Intrinsics.checkNotNullExpressionValue(context, "getContext(...)");
                                ((ViewGroup) view).setBackground(rounded(context, pillSoftColor(c), 18.0f));
                            } else if (Intrinsics.areEqual("divider", ((ViewGroup) view).getTag())) {
                                // v1.10.271：线条走唯一出口（中性变体朝正文色混，随对比度加深）
                                view.setBackgroundColor(lineColor(((ViewGroup) view).getContext(), c));
                            } else if (Intrinsics.areEqual("band", ((ViewGroup) view).getTag())) {
                                // v1.10.392：主页「新建组件」色带。**半径 0**（直角）——
                                // 这是整页唯一的高饱和面积，圆角会立刻把它变回一枚「大胶囊按钮」，
                                // 版式又退回上一版。
                                // v1.10.521：填充从「整幅 primary」改成与开发者页名片同一套渐变
                                // （TL_BR，primary 92% → secondary 86%），参数只有一处：heroFill()。
                                Context context = ((ViewGroup) view).getContext();
                                Intrinsics.checkNotNullExpressionValue(context, "getContext(...)");
                                ((ViewGroup) view).setBackground(heroFill(context, 0.0f));
                            }
                        }
                        int childCount = ((ViewGroup) view).getChildCount();
                        for (int i = 0; i < childCount; i++) {
                            View childAt = ((ViewGroup) view).getChildAt(i);
                            Intrinsics.checkNotNullExpressionValue(childAt, "getChildAt(...)");
                            paint(childAt, c);
                        }
                        return;
                    }
                    if ((view instanceof View) && Intrinsics.areEqual("divider", view.getTag())) {
                        view.setBackgroundColor(lineColor(view.getContext(), c));
                        return;
                    }
                    if ((view instanceof View) && Intrinsics.areEqual("divider_soft", view.getTag())) {
                        view.setBackgroundColor(withAlpha(c.getText(), 0.24f));
                        return;
                    }
                    return;
                }
                return;
            }
            styleText((TextView) view, c);
            return;
        }
        styleButton((Button) view, c);
    }

    private final void styleText(TextView view, ThemeColors c) {
        Object tag = view.getTag();
        // v1.10.98：首页降权两级 —— dim = 次级信息，dim2 = 说明文字
        if (Intrinsics.areEqual(tag, "dim")) {
            view.setTextColor(withAlpha(c.getText(), 0.62f));
            return;
        }
        if (Intrinsics.areEqual(tag, "dim2")) {
            view.setTextColor(withAlpha(c.getText(), 0.44f));
            return;
        }
        // v1.10.197：设置页「性能」卡里那些长副标题 —— 比 muted 更弱一档，不跟标题抢。
        if (Intrinsics.areEqual(tag, "muted_quiet")) {
            view.setTextColor(subtitleColor(view.getContext()));
            return;
        }
        // v1.10.392：主页「新建组件」色带上的两行字。色带整幅是 primary，
        // 所以这两行**必须**跟 onPrimary 走 —— 用正文色的话，用户换成浅色主色
        // （比如浅黄、浅粉）时字就糊在色带里了。副行是同一色的 74%。
        if (Intrinsics.areEqual(tag, "on_primary")) {
            view.setTextColor(c.getOnPrimary());
            return;
        }
        if (Intrinsics.areEqual(tag, "on_primary_dim")) {
            view.setTextColor(withAlpha(c.getOnPrimary(), 0.74f));
            return;
        }
        if (!(Intrinsics.areEqual(tag, "section") ? true : Intrinsics.areEqual(tag, "muted"))) {
            if (!Intrinsics.areEqual(tag, "chip")) {
                if (Intrinsics.areEqual(tag, "fab")) {
                    view.setTextColor(c.getOnPrimary());
                    GradientDrawable $this$styleText_u24lambda_u240 = new GradientDrawable();
                    $this$styleText_u24lambda_u240.setShape(1);
                    $this$styleText_u24lambda_u240.setColor(c.getPrimary());
                    view.setBackground($this$styleText_u24lambda_u240);
                    return;
                }
                if (Intrinsics.areEqual(tag, "icon_tile")) {
                    view.setTextColor(c.getPrimary());
                    Context context = view.getContext();
                    Intrinsics.checkNotNullExpressionValue(context, "getContext(...)");
                    view.setBackground(rounded(context, withAlpha(c.getPrimary(), 0.16f), 14.0f));
                    return;
                }
                if (Intrinsics.areEqual(tag, "icon_tile_2")) {
                    view.setTextColor(c.getSecondary());
                    Context context2 = view.getContext();
                    Intrinsics.checkNotNullExpressionValue(context2, "getContext(...)");
                    view.setBackground(rounded(context2, withAlpha(c.getSecondary(), 0.18f), 14.0f));
                    return;
                }
                if (Intrinsics.areEqual(tag, "icon_tile_3")) {
                    view.setTextColor(c.getTertiary());
                    Context context3 = view.getContext();
                    Intrinsics.checkNotNullExpressionValue(context3, "getContext(...)");
                    view.setBackground(rounded(context3, withAlpha(c.getTertiary(), 0.18f), 14.0f));
                    return;
                }
                if (Intrinsics.areEqual(tag, "chip_value")) {
                    exposeShadow(view);
                    view.setTextColor(c.getPrimary());
                    Context context4 = view.getContext();
                    Intrinsics.checkNotNullExpressionValue(context4, "getContext(...)");
                    view.setBackground(rounded(context4, withAlpha(c.getPrimary(), 0.14f), 999.0f));
                    return;
                }
                if (Intrinsics.areEqual(tag, "close_pill")) {
                    exposeShadow(view);
                    view.setTextColor(c.getPrimary());
                    Context context5 = view.getContext();
                    Intrinsics.checkNotNullExpressionValue(context5, "getContext(...)");
                    view.setBackground(rounded(context5, withAlpha(c.getPrimary(), 0.18f), 999.0f));
                    return;
                }
                // v1.10.362：「关于 / 复制 QQ」两格各自的**可见表面**。
                // 上一版两格是 ghost_text（完全透明），3D 倾斜 + 缩到 0.96 只带动了几个字，
                // 容器那层静态 pill_soft 一动不动 —— 用户原话「倾斜/回弹依旧没出现」。
                // 现在上格只圆上面两角、下格只圆下面两角：平时拼成一枚合并胶囊，
                // 按下时**看到的是实体色块在倒**，倾斜才成立。
                if (Intrinsics.areEqual(tag, "pill_half_top")) {
                    exposeShadow(view);
                    view.setTextColor(c.getText());
                    view.setBackground(halfPill(view.getContext(), c, true));
                    return;
                }
                if (Intrinsics.areEqual(tag, "pill_half_bottom")) {
                    exposeShadow(view);
                    view.setTextColor(c.getText());
                    view.setBackground(halfPill(view.getContext(), c, false));
                    return;
                }
                view.setTextColor(c.getText());
                return;
            }
            return;
        }
        view.setTextColor(c.getTextMuted());
    }

    private final void styleButton(Button view, ThemeColors c) {
        view.setAllCaps(false);
        // v1.10.393：浅色模式下「阴影被界面截掉」的根因在**父容器**，不在按钮自己。
        // Material 的按钮自带 2dp elevation，而我们的行容器高度经常正好等于按钮高度
        // （按钮行 wrap_content、行内没有额外内边距）—— 于是投影只画得出左右两边，
        // 上下被父容器的边界齐齐切掉一条。这里只放开裁剪，不动位置。
        //
        // v1.10.394：**投影本身也得补回来**。我们在 v1.10.150 里把 stateListAnimator 摘掉
        // （为了不要系统那圈「按下浮起」的动画），而 Material 的 2dp elevation 正是由那个
        // animator 的默认状态给的 —— 摘掉之后按钮的 elevation 一直是 0，
        // 于是"阴影被截掉"的真相是：**很多按钮一开始就没有投影**，或者只在个别容器里露出一条。
        // 现在显式把 2dp 写回去（Material 的标准值），配合 exposeShadow 放开裁剪：
        // 投影四边都是完整软边。文字按钮（ghost*）没有底色，保持扁平。
        float shadowDensity = view.getResources().getDisplayMetrics().density;
        view.setElevation(2.0f * shadowDensity);
        exposeShadow(view);
        // v1.10.150（反馈：胶囊按钮阴影被截断 / 被下一个控件挡住）：
        // 这些按钮的 elevation 阴影来自系统 Material 主题的 stateListAnimator，
        // 我们从来没设计过阴影 —— 阴影超出父容器就被裁成一条硬边，或者被下一个控件盖住。
        // 统一关掉它：位置、尺寸、颜色都不动，只是回到本来的扁平胶囊。
        view.setStateListAnimator(null);
        Object tag = view.getTag();
        if (Intrinsics.areEqual(tag, "primary")) {
            Context context = view.getContext();
            Intrinsics.checkNotNullExpressionValue(context, "getContext(...)");
            view.setBackground(rounded(context, c.getPrimary(), 28.0f));
            view.setTextColor(c.getOnPrimary());
            return;
        }
        if (Intrinsics.areEqual(tag, "pill_outline")) {
            if (view.isSelected()) {
                Context context2 = view.getContext();
                Intrinsics.checkNotNullExpressionValue(context2, "getContext(...)");
                view.setBackground(rounded(context2, c.getPrimary(), 999.0f));
                view.setTextColor(c.getOnPrimary());
                return;
            }
            Context context3 = view.getContext();
            Intrinsics.checkNotNullExpressionValue(context3, "getContext(...)");
            // v1.10.271：未选中胶囊只有 1dp 描边，描边色即全部（不能让它跟着底色一起消失）
            view.setBackground(outlined(context3, 0, lineColor(context3, c), 999.0f));
            view.setTextColor(c.getText());
            return;
        }
        if (Intrinsics.areEqual(tag, "pill_soft")) {
            Context context4 = view.getContext();
            Intrinsics.checkNotNullExpressionValue(context4, "getContext(...)");
            view.setBackground(rounded(context4, pillSoftColor(c), 999.0f));
            view.setTextColor(c.getText());
            return;
        }
        // v1.10.522：**分组胶囊** —— 外侧大圆角、相邻内侧小圆角。
        // 竖向（group_pill_first / middle / last）用于「调试与记录」那三格；
        // 横向（group_pill_left / right）用于「展开 / 收起」这一对。
        int groupSlot = groupSlotOf(tag);
        if (groupSlot >= 0) {
            view.setBackground(groupedPill(view.getContext(), groupSlot, !isHorizontalGroup(tag)));
            view.setTextColor(c.getText());
            return;
        }
        if (Intrinsics.areEqual(tag, "ghost_text")) {
            view.setBackground((android.graphics.drawable.Drawable) null);
            view.setElevation(0.0f);
            view.setTextColor(c.getText());
            return;
        }
        // v1.10.392：主页底部的「全部停止」。它是次级动作，不能和色带抢颜色，
        // 所以是「纯文字 + 62% 灰」，不是 primary 色的链接。
        if (Intrinsics.areEqual(tag, "ghost_dim")) {
            view.setBackground((android.graphics.drawable.Drawable) null);
            view.setElevation(0.0f);
            view.setTextColor(withAlpha(c.getText(), 0.62f));
            return;
        }
        if (Intrinsics.areEqual(tag, "ghost")) {
            view.setBackground(null);
            view.setElevation(0.0f);
            view.setTextColor(c.getPrimary());
        } else {
            if (Intrinsics.areEqual(tag, "danger")) {
                Context context5 = view.getContext();
                Intrinsics.checkNotNullExpressionValue(context5, "getContext(...)");
                // v1.10.203：破坏性按钮不再是硬编码的 #B3261E —— 跟「错误色」一起换。
                view.setBackground(rounded(context5, c.getError(), 999.0f));
                view.setTextColor(c.getOnError());
                return;
            }
            Context context6 = view.getContext();
            Intrinsics.checkNotNullExpressionValue(context6, "getContext(...)");
            view.setBackground(rounded(context6, c.getPrimary(), 28.0f));
            view.setTextColor(c.getOnPrimary());
        }
    }

    /**
     * v1.10.393：**让一个控件的投影画得出来**（只动裁剪，不动阴影与位置）。
     *
     * <p>Android 的投影是「控件画在父容器给自己分配的那块画布上」——父容器一旦
     * {@code clipChildren=true}（默认）、而且自己的高度/宽度正好等于控件，投影就会被
     * 沿着父容器的边界齐齐切掉一条。浅色模式下投影最显眼，所以这个问题在那里最容易看出来
     * （深色模式下投影本来就几乎看不见）。</p>
     *
     * <p>规则（最多往上两级，遇到「面」就停）：</p>
     * <ol>
     *   <li>紧邻的透明容器（行/列）：放开 {@code clipChildren} + {@code clipToPadding} ——
     *       投影可以溢出到行外面，落到更深一层的底上；</li>
     *   <li>再往上一层如果是「面」（有 background 的卡片/面板）：**只**放开
     *       {@code clipToPadding}（让它愿意把投影画进自己的内边距里），
     *       卡片自己的外沿依旧裁剪 —— 投影不会跑到卡片外面去。</li>
     * </ol>
     *
     * <p>幂等：重复调用只是把两个布尔值再设一遍，不会累积副作用。</p>
     */
    public final void exposeShadow(@NotNull View child) {
        android.view.ViewParent node = child.getParent();
        for (int level = 0; level < 2; level++) {
            if (!(node instanceof ViewGroup)) {
                return;
            }
            ViewGroup group = (ViewGroup) node;
            if (group.getBackground() != null) {
                // 「面」：只把内边距这一圈让出来，卡片外沿继续裁剪
                if (group.getClipToPadding()) {
                    group.setClipToPadding(false);
                }
                return;
            }
            if (group.getClipChildren()) {
                group.setClipChildren(false);
            }
            if (group.getClipToPadding()) {
                group.setClipToPadding(false);
            }
            node = group.getParent();
        }
    }

    @NotNull
    /**
     * v1.10.71：**把对话框窗口的底色清成透明**。
     *
     * 我们每个对话框都是自己在根布局上画「圆角卡片」，但 Dialog 的窗口默认还带着
     * 系统主题的方形背景（浅色主题下就是一块白底）—— 圆角四个角就露出白三角，
     * 用户看到的就是「没抠图」。
     */
    public final void clearDialogWindow(@NotNull android.app.Dialog dialog) {
        try {
            android.view.Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * v1.10.362：合并胶囊的「上半格 / 下半格」。
     *
     * <p>只圆**外端**两角、内端留直角 —— 上下两格贴在一起时拼成一条直线，
     * 整体看还是那枚 18dp 圆角的竖向胶囊；按下倾斜时两格能各自转，
     * 因为**每一格自己就是一块实色**，不再是「透明格子 + 静态外框」。
     *
     * <p>半径沿用 18dp（v1.10.361 按用户反馈从 999 降到 18 的那个值，别改回 999）。
     */
    @NotNull
    private static GradientDrawable halfPill(@NotNull Context context, @NotNull ThemeColors c,
                                             boolean top) {
        float r = 18.0f * context.getResources().getDisplayMetrics().density;
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(pillSoftColor(c));
        // 顺序：左上、右上、右下、左下（每角 x/y 各一个）
        gd.setCornerRadii(top
                ? new float[]{r, r, r, r, 0.0f, 0.0f, 0.0f, 0.0f}
                : new float[]{0.0f, 0.0f, 0.0f, 0.0f, r, r, r, r});
        return gd;
    }

    public final GradientDrawable rounded(@NotNull Context context, int color, float radiusDp) {
        Intrinsics.checkNotNullParameter(context, "context");
        float density = context.getResources().getDisplayMetrics().density;
        GradientDrawable $this$rounded_u24lambda_u241 = new GradientDrawable();
        $this$rounded_u24lambda_u241.setColor(color);
        $this$rounded_u24lambda_u241.setCornerRadius(radiusDp * density);
        return $this$rounded_u24lambda_u241;
    }

    // ================================================================ v1.10.522 分组圆角

    /** 分组里的位置：单独一个（四角都大）。 */
    public static final int GROUP_SINGLE = 0;
    /** 分组里的第一个（上 / 左端）。 */
    public static final int GROUP_FIRST = 1;
    /** 分组里的中间项（相邻两侧的角都小）。 */
    public static final int GROUP_MIDDLE = 2;
    /** 分组里的最后一个（下 / 右端）。 */
    public static final int GROUP_LAST = 3;

    /** 按钮组的外侧圆角（dp）。 */
    public static final float GROUP_PILL_OUTER_DP = 18.0f;
    /** 按钮组的**相邻内侧**圆角（dp）—— 外侧大、相邻内侧小，拼起来才是「一个容器」。 */
    public static final float GROUP_PILL_INNER_DP = 8.0f;

    /**
     * v1.10.522：**分组圆角（Container Shape）**，外侧大、相邻内侧小。
     *
     * <p>一组相邻控件拼成一个容器：**最外侧的角大、相邻的内侧角小**。
     * 竖向排列时「上端两角 / 下端两角」是外侧，横向排列时「左端两角 / 右端两角」是外侧；
     * 中间项四角一律取内侧小圆角。</p>
     *
     * <p>只画「面」：颜色由调用方给（性能卡的极淡底、按钮的中性底各用各的），
     * 这里不做任何对比度推导 —— 与 {@link #rounded} 同一个口径，方便逐处替换。</p>
     *
     * @param slot     {@link #GROUP_SINGLE} / {@link #GROUP_FIRST} / {@link #GROUP_MIDDLE} / {@link #GROUP_LAST}
     * @param vertical true = 上下叠（竖排按钮、卡片里的行）；false = 左右并排（两颗胶囊、横排卡片）
     * @param outerDp  外侧圆角（dp）
     * @param innerDp  相邻内侧圆角（dp）
     */
    @NotNull
    public final GradientDrawable grouped(@NotNull Context context, int color, int slot,
                                          boolean vertical, float outerDp, float innerDp) {
        Intrinsics.checkNotNullParameter(context, "context");
        float density = context.getResources().getDisplayMetrics().density;
        float o = outerDp * density;
        float i = innerDp * density;
        float tl;
        float tr;
        float br;
        float bl;
        if (vertical) {
            switch (slot) {
                case GROUP_FIRST:
                    tl = o; tr = o; br = i; bl = i;
                    break;
                case GROUP_LAST:
                    tl = i; tr = i; br = o; bl = o;
                    break;
                case GROUP_MIDDLE:
                    tl = i; tr = i; br = i; bl = i;
                    break;
                default:
                    tl = o; tr = o; br = o; bl = o;
                    break;
            }
        } else {
            switch (slot) {
                case GROUP_FIRST:
                    tl = o; tr = i; br = i; bl = o;
                    break;
                case GROUP_LAST:
                    tl = i; tr = o; br = o; bl = i;
                    break;
                case GROUP_MIDDLE:
                    tl = i; tr = i; br = i; bl = i;
                    break;
                default:
                    tl = o; tr = o; br = o; bl = o;
                    break;
            }
        }
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadii(new float[]{tl, tl, tr, tr, br, br, bl, bl});
        return d;
    }

    /** 按钮组的一格：中性底 + 正文色字（竖向 3 格 / 横向 2 格都走这里）。 */
    @NotNull
    public final GradientDrawable groupedPill(@NotNull Context context, int slot, boolean vertical) {
        ThemeColors c = colors(context);
        return grouped(context, pillSoftColor(c), slot, vertical,
                GROUP_PILL_OUTER_DP, GROUP_PILL_INNER_DP);
    }

    /** tag → 分组位置；不是分组 tag 返回 -1。 */
    public static int groupSlotOf(@Nullable Object tag) {
        if (!(tag instanceof String)) {
            return -1;
        }
        String t = (String) tag;
        if ("group_pill_first".contentEquals(t) || "group_pill_left".contentEquals(t)) {
            return GROUP_FIRST;
        }
        if ("group_pill_middle".contentEquals(t)) {
            return GROUP_MIDDLE;
        }
        if ("group_pill_last".contentEquals(t) || "group_pill_right".contentEquals(t)) {
            return GROUP_LAST;
        }
        return -1;
    }

    /** 分组 tag 是不是「横向成对」的那两个（left / right）。 */
    public static boolean isHorizontalGroup(@Nullable Object tag) {
        if (!(tag instanceof String)) {
            return false;
        }
        String t = (String) tag;
        return "group_pill_left".contentEquals(t) || "group_pill_right".contentEquals(t);
    }

    @NotNull
    public final GradientDrawable outlined(@NotNull Context context, int fill, int stroke, float radiusDp) {
        Intrinsics.checkNotNullParameter(context, "context");
        float density = context.getResources().getDisplayMetrics().density;
        // v1.10.271：传进来的描边色如果是「中性变体」，一律改走线条色的唯一出口
        // （lineColor：随对比度朝正文色混）。这样全应用 ~10 处 outlined(...) 调用点
        // 不用各改一遍，也不会漏一处 —— 漏一处就是那个控件在高对比下"没有边界"。
        ThemeColors c = colors(context);
        int realStroke = stroke == c.getNeutral() ? lineColor(context, c) : stroke;
        GradientDrawable $this$outlined_u24lambda_u242 = new GradientDrawable();
        $this$outlined_u24lambda_u242.setColor(fill);
        $this$outlined_u24lambda_u242.setStroke(RangesKt.coerceAtLeast((int) (1.0f * density), 1), realStroke);
        $this$outlined_u24lambda_u242.setCornerRadius(radiusDp * density);
        return $this$outlined_u24lambda_u242;
    }

    @NotNull
    public final GradientDrawable chip(@NotNull Context context, boolean selected) {
        Intrinsics.checkNotNullParameter(context, "context");
        ThemeColors c = colors(context);
        float density = context.getResources().getDisplayMetrics().density;
        GradientDrawable $this$chip_u24lambda_u243 = new GradientDrawable();
        $this$chip_u24lambda_u243.setCornerRadius(14.0f * density);
        if (selected) {
            $this$chip_u24lambda_u243.setColor(INSTANCE.withAlpha(c.getPrimary(), 0.12f));
            $this$chip_u24lambda_u243.setStroke(RangesKt.coerceAtLeast((int) (1.5f * density), 2), c.getPrimary());
        } else {
            $this$chip_u24lambda_u243.setColor(c.getNeutral());
        }
        return $this$chip_u24lambda_u243;
    }

    public final int withAlpha(int color, float alpha) {
        int a = RangesKt.coerceIn((int) (Color.alpha(color) * alpha), 0, 255);
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * 高饱和渐变：开发者页「创作者名片」与主页「新建组件」色带共用这一套参数。
     *
     * <p>左上 → 右下（{@link GradientDrawable.Orientation#TL_BR}），
     * primary 92% → secondary 86%（透明度沿用名片那张卡的取值）。
     * 圆角由调用方给：名片 24dp；主页色带 0（直角，它是整页唯一的高饱和面积，
     * 加圆角会立刻变回「大胶囊按钮」）。</p>
     */
    @NotNull
    public final GradientDrawable heroFill(@NotNull Context context, float radiusDp) {
        Intrinsics.checkNotNullParameter(context, "context");
        ThemeColors c = colors(context);
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{withAlpha(c.getPrimary(), 0.92f), withAlpha(c.getSecondary(), 0.86f)});
        gd.setCornerRadius(radiusDp * context.getResources().getDisplayMetrics().density);
        return gd;
    }
}
