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
    private static final List<Preset> presets = CollectionsKt.listOf(new Preset[]{new Preset(R.string.palette_catppuccin, -7849489, -14784779, -2126307), new Preset(R.string.palette_nord, -10583636, -8281663, -4944211), new Preset(R.string.palette_dracula, -4353031, -7607811, -34362), new Preset(R.string.palette_gruvbox, -2647775, -12221048, -3398627), new Preset(R.string.palette_tokyo, -8740105, -4482313, -8531969), new Preset(R.string.palette_rose, -1347694, -6500392, -3889177)});

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

    @NotNull
    public final ThemeColors colors(@NotNull Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
        boolean dark = isDark(context);
        Preset preset = presets.get(RangesKt.coerceIn(ThemePrefs.INSTANCE.getPreset(context), 0, presets.size() - 1));
        int primary = pick(context, "primary", preset.getPrimary());
        int secondary = pick(context, "secondary", preset.getSecondary());
        int tertiary = pick(context, "tertiary", preset.getTertiary());
        int surfaceBase = dark ? -14934236 : -1;
        int neutralBase = dark ? -14012875 : -1578258;
        int surface = pick(context, "surface", surfaceBase);
        int neutral = pick(context, "neutral", neutralBase);
        int background = dark ? -15658216 : -526086;
        int text = dark ? -1710101 : -14735049;
        int muted = dark ? -6511697 : -9735552;
        ThemeColors c = new ThemeColors(background, surface, neutral, primary, secondary, tertiary, text, muted, isLight(primary) ? -15658216 : -1);
        return adjust(c, ThemePrefs.INSTANCE.getContrast(context), ThemePrefs.INSTANCE.getInvert(context));
    }

    private final int pick(Context context, String key, int fallback) {
        int stored = ThemePrefs.INSTANCE.getCustom(context, key);
        return stored != 0 ? stored : fallback;
    }

    private final boolean isLight(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return (((r * 299) + (g * 587)) + (b * 114)) / 1000 > 160;
    }

    private static final int adjust$map(boolean $invert, int $contrast, int color) {
        int iRgb;
        if ($invert) {
            iRgb = Color.rgb(255 - Color.red(color), 255 - Color.green(color), 255 - Color.blue(color));
        } else {
            iRgb = color;
        }
        int c = iRgb;
        if ($contrast != 0) {
            float f = 1.0f + ($contrast * 0.035f);
            c = Color.rgb(RangesKt.coerceIn((int) (Color.red(c) * f), 0, 255), RangesKt.coerceIn((int) (Color.green(c) * f), 0, 255), RangesKt.coerceIn((int) (Color.blue(c) * f), 0, 255));
        }
        return c;
    }

    private final ThemeColors adjust(ThemeColors colors, int contrast, boolean invert) {
        return colors.copy(adjust$map(invert, contrast, colors.getBackground()), adjust$map(invert, contrast, colors.getSurface()), adjust$map(invert, contrast, colors.getNeutral()), adjust$map(invert, contrast, colors.getPrimary()), adjust$map(invert, contrast, colors.getSecondary()), adjust$map(invert, contrast, colors.getTertiary()), adjust$map(invert, contrast, colors.getText()), adjust$map(invert, contrast, colors.getTextMuted()), (isLight(colors.getPrimary()) && invert) ? -15658216 : colors.getOnPrimary());
    }

    public final int dialogTheme(@NotNull Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
        if (isDark(context)) {
            return android.R.style.Theme_Material_Dialog_Alert;
        }
        return android.R.style.Theme_Material_Light_Dialog_Alert;
    }

    public final void applySystemBars(@NotNull Activity activity) {
        WindowInsetsController insetsController;
        Intrinsics.checkNotNullParameter(activity, "activity");
        ThemeColors c = colors(activity);
        boolean dark = isDark(activity);
        Window window = activity.getWindow();
        window.setStatusBarColor(c.getBackground());
        window.setNavigationBarColor(c.getBackground());
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

    private final void paint(View view, ThemeColors c) {
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
            ((RadioButton) view).setTextColor(c.getText());
            return;
        }
        if (!(view instanceof Button)) {
            if (!(view instanceof TextView)) {
                if (view instanceof SeekBar) {
                    ((SeekBar) view).setProgressTintList(ColorStateList.valueOf(c.getPrimary()));
                    ((SeekBar) view).setThumbTintList(ColorStateList.valueOf(c.getPrimary()));
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
                            } else if (Intrinsics.areEqual("divider", ((ViewGroup) view).getTag())) {
                                view.setBackgroundColor(c.getNeutral());
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
                        view.setBackgroundColor(c.getNeutral());
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
                    view.setTextColor(c.getPrimary());
                    Context context4 = view.getContext();
                    Intrinsics.checkNotNullExpressionValue(context4, "getContext(...)");
                    view.setBackground(rounded(context4, withAlpha(c.getPrimary(), 0.14f), 999.0f));
                    return;
                }
                if (Intrinsics.areEqual(tag, "close_pill")) {
                    view.setTextColor(c.getPrimary());
                    Context context5 = view.getContext();
                    Intrinsics.checkNotNullExpressionValue(context5, "getContext(...)");
                    view.setBackground(rounded(context5, withAlpha(c.getPrimary(), 0.18f), 999.0f));
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
            view.setBackground(outlined(context3, 0, c.getNeutral(), 999.0f));
            view.setTextColor(c.getText());
            return;
        }
        if (Intrinsics.areEqual(tag, "pill_soft")) {
            Context context4 = view.getContext();
            Intrinsics.checkNotNullExpressionValue(context4, "getContext(...)");
            view.setBackground(rounded(context4, c.getNeutral(), 999.0f));
            view.setTextColor(c.getText());
            return;
        }
        if (Intrinsics.areEqual(tag, "ghost")) {
            view.setBackground(null);
            view.setTextColor(c.getPrimary());
        } else {
            if (Intrinsics.areEqual(tag, "danger")) {
                Context context5 = view.getContext();
                Intrinsics.checkNotNullExpressionValue(context5, "getContext(...)");
                view.setBackground(rounded(context5, -5036514, 999.0f));
                view.setTextColor(-1);
                return;
            }
            Context context6 = view.getContext();
            Intrinsics.checkNotNullExpressionValue(context6, "getContext(...)");
            view.setBackground(rounded(context6, c.getPrimary(), 28.0f));
            view.setTextColor(c.getOnPrimary());
        }
    }

    @NotNull
    public final GradientDrawable rounded(@NotNull Context context, int color, float radiusDp) {
        Intrinsics.checkNotNullParameter(context, "context");
        float density = context.getResources().getDisplayMetrics().density;
        GradientDrawable $this$rounded_u24lambda_u241 = new GradientDrawable();
        $this$rounded_u24lambda_u241.setColor(color);
        $this$rounded_u24lambda_u241.setCornerRadius(radiusDp * density);
        return $this$rounded_u24lambda_u241;
    }

    @NotNull
    public final GradientDrawable outlined(@NotNull Context context, int fill, int stroke, float radiusDp) {
        Intrinsics.checkNotNullParameter(context, "context");
        float density = context.getResources().getDisplayMetrics().density;
        GradientDrawable $this$outlined_u24lambda_u242 = new GradientDrawable();
        $this$outlined_u24lambda_u242.setColor(fill);
        $this$outlined_u24lambda_u242.setStroke(RangesKt.coerceAtLeast((int) (1.0f * density), 1), stroke);
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

    private final int withAlpha(int color, float alpha) {
        int a = RangesKt.coerceIn((int) (Color.alpha(color) * alpha), 0, 255);
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }
}
