package io.github.animwidget;

import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;
import java.util.Locale;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;

/* compiled from: LocaleHelper.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��8\n\u0002\u0018\u0002\n\u0002\u0010��\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0010\u000b\n��\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\bÆ\u0002\u0018��2\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J\u000e\u0010\t\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u000bJ\u000e\u0010\f\u001a\u00020\r2\u0006\u0010\n\u001a\u00020\u000bJ\u0016\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\u0010\u001a\u00020\u0004J\u0010\u0010\u0011\u001a\u00020\u00122\u0006\u0010\n\u001a\u00020\u000bH\u0002J\u000e\u0010\u0013\u001a\u00020\u000b2\u0006\u0010\u0014\u001a\u00020\u000bR\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T¢\u0006\u0002\n��R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T¢\u0006\u0002\n��R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T¢\u0006\u0002\n��R\u000e\u0010\u0007\u001a\u00020\bX\u0082T¢\u0006\u0002\n��"}, d2 = {"Lio/github/animwidget/LocaleHelper;", "", "()V", "CHINESE", "", "ENGLISH", "FOLLOW_SYSTEM", "PREF", "", "get", "context", "Landroid/content/Context;", "needsRecreate", "", "set", "", "value", "systemLocale", "Ljava/util/Locale;", "wrap", "base"})
/* loaded from: classes.jar:io/github/animwidget/LocaleHelper.class */
public final class LocaleHelper {

    @NotNull
    public static final LocaleHelper INSTANCE = new LocaleHelper();
    public static final int FOLLOW_SYSTEM = 0;
    public static final int CHINESE = 1;
    public static final int ENGLISH = 2;

    @NotNull
    private static final String PREF = "ui_language";

    private LocaleHelper() {
    }

    public final int get(@NotNull Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
        return context.getSharedPreferences("settings", 0).getInt(PREF, 0);
    }

    public final void set(@NotNull Context context, int value) {
        Intrinsics.checkNotNullParameter(context, "context");
        context.getSharedPreferences("settings", 0).edit().putInt(PREF, value).apply();
    }

    /**
     * v1.10.1（需求 9）：尊重系统「应用语言」设置。
     * 系统里给本应用单独设过语言时，读过来作为我们的语言（只读同步 ——
     * 不写回系统，否则系统会重建所有界面，反而破坏「不折叠、不丢状态」）。
     */
    public static synchronized void syncFromSystem(@NotNull Context context) {
        if (android.os.Build.VERSION.SDK_INT < 33) {
            return;
        }
        try {
            android.app.LocaleManager manager =
                    (android.app.LocaleManager) context.getSystemService(Context.LOCALE_SERVICE);
            if (manager == null) {
                return;
            }
            LocaleList locales = manager.getApplicationLocales();
            if (locales == null || locales.isEmpty()) {
                return;
            }
            String language = locales.get(0).getLanguage();
            int value = "zh".equals(language) ? CHINESE : ("en".equals(language) ? ENGLISH : FOLLOW_SYSTEM);
            if (value != INSTANCE.get(context)) {
                INSTANCE.set(context, value);
            }
        } catch (Throwable ignored) {
        }
    }

    @NotNull
    public final Context wrap(@NotNull Context base) {
        Locale locale;
        Intrinsics.checkNotNullParameter(base, "base");
        int mode = get(base);
        if (mode == 0) {
            locale = systemLocale(base);
        } else {
            locale = new Locale(mode == 1 ? "zh" : "en");
        }
        Locale locale2 = locale;
        Locale.setDefault(locale2);
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(locale2);
        Context contextCreateConfigurationContext = base.createConfigurationContext(config);
        Intrinsics.checkNotNullExpressionValue(contextCreateConfigurationContext, "createConfigurationContext(...)");
        return contextCreateConfigurationContext;
    }

    private final Locale systemLocale(Context context) {
        LocaleList locales = context.getApplicationContext().getResources().getConfiguration().getLocales();
        Intrinsics.checkNotNullExpressionValue(locales, "getLocales(...)");
        if (locales.isEmpty()) {
            Locale locale = Locale.getDefault();
            Intrinsics.checkNotNullExpressionValue(locale, "getDefault(...)");
            return locale;
        }
        Locale locale2 = locales.get(0);
        Intrinsics.checkNotNullExpressionValue(locale2, "get(...)");
        return locale2;
    }

    public final boolean needsRecreate(@NotNull Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
        int mode = get(context);
        Locale desired = mode == 0 ? systemLocale(context) : new Locale(mode == 1 ? "zh" : "en");
        // 比对「这个页面现在真正用的语言」和「应该用的语言」。
        // I18nActivity 原地换语言时会覆盖 getResources()，所以这里读到的就是最新值。
        LocaleList locales = context.getResources().getConfiguration().getLocales();
        String current = !locales.isEmpty() ? locales.get(0).getLanguage() : Locale.getDefault().getLanguage();
        return !Intrinsics.areEqual(current, desired.getLanguage());
    }
}
