package io.github.animwidget;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;

/**
 * v1.10.417（需求）：**实时跟随系统深浅色**。
 *
 * <p>三个弹窗的颜色都是在打开那一刻用 {@link ThemeManager#colors} 算一次就定死的，
 * 所以「弹窗开着 → 从控制中心切深色 → 弹窗还是旧配色」。
 * 这里挂一个 Application 级的 {@link ComponentCallbacks2}：系统 uiMode 一变，
 * 就通知弹窗把自己整块重建（颜色本身就是每次现算的，重建即跟随）。
 *
 * <p>只在「跟随系统」模式下才会触发重建 —— 用户在 App 里显式选了深/浅时，
 * 系统切深色不该把弹窗重建一次（{@code isDark} 的值没变，等于什么都不会发生）。
 */
final class UiModeWatcher {

    interface Listener {
        void onSystemModeChanged();
    }

    private final Context app;
    private final Listener listener;
    private boolean dark;
    private boolean registered;

    private final ComponentCallbacks2 callbacks = new ComponentCallbacks2() {
        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            boolean now = ThemeManager.INSTANCE.isDark(app);
            if (now != dark) {
                dark = now;
                try {
                    listener.onSystemModeChanged();
                } catch (Throwable ignored) {
                }
            }
        }

        @Override
        public void onLowMemory() {
        }

        @Override
        public void onTrimMemory(int level) {
        }
    };

    UiModeWatcher(Context context, Listener listener) {
        Context c = context.getApplicationContext();
        this.app = c != null ? c : context;
        this.listener = listener;
        this.dark = ThemeManager.INSTANCE.isDark(this.app);
    }

    void start() {
        if (registered) {
            this.dark = ThemeManager.INSTANCE.isDark(this.app);
            return;
        }
        try {
            app.registerComponentCallbacks(callbacks);
            registered = true;
        } catch (Throwable ignored) {
        }
    }

    void stop() {
        if (!registered) {
            return;
        }
        try {
            app.unregisterComponentCallbacks(callbacks);
        } catch (Throwable ignored) {
        }
        registered = false;
    }
}
