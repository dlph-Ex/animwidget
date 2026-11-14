package io.github.animwidget;

import android.app.Activity;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 用原地重新着色代替 Activity.recreate()：
 * 切换深浅色 / 换预设 / 改自定义颜色时，页面不重建、滚动位置与菜单展开状态全部保留。
 */
public final class ThemePatch {

    private static Method settingsRefresh;
    private static Field settingsAppliedDark;

    static {
        try {
            Method m = SettingsActivity.class.getDeclaredMethod("refreshThemeInPlace");
            m.setAccessible(true);
            settingsRefresh = m;
        } catch (Throwable th) {
            settingsRefresh = null;
        }
        try {
            Field f = SettingsActivity.class.getDeclaredField("appliedDark");
            f.setAccessible(true);
            settingsAppliedDark = f;
        } catch (Throwable th) {
            settingsAppliedDark = null;
        }
    }

    private ThemePatch() {
    }

    public static void inPlaceRefresh(Activity activity) {
        if (activity == null) {
            return;
        }
        if (activity instanceof SettingsActivity && settingsRefresh != null) {
            try {
                settingsRefresh.invoke(activity);
                syncAppliedDark(activity);
                return;
            } catch (Throwable th) {
                // 反射失败就退化成通用重绘
            }
        }
        View content = activity.findViewById(android.R.id.content);
        if (content != null) {
            ThemeManager.INSTANCE.applyTo(content);
        }
        ThemeManager.INSTANCE.applySystemBars(activity);
    }

    /** 刷新后把 appliedDark 同步到当前主题，避免 500ms 轮询再触发一次重建式重绘。 */
    private static void syncAppliedDark(Activity activity) {
        if (settingsAppliedDark == null) {
            return;
        }
        try {
            settingsAppliedDark.setBoolean(activity, ThemeManager.INSTANCE.isDark(activity));
        } catch (Throwable ignored) {
        }
    }
}
