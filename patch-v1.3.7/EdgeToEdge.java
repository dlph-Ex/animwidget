package io.github.animwidget;

import android.app.Activity;
import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;

import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;

/**
 * 全屏沉浸：SDK 35+ 强制 edge-to-edge。
 * 组件配置页的预览要顶到屏幕最上沿（没有那条白色横条），所以它不吃 top inset；
 * 主界面 / 设置页仍然留出状态栏高度。
 */
public final class EdgeToEdge {

    public static final EdgeToEdge INSTANCE = new EdgeToEdge();

    private EdgeToEdge() {
    }

    public final void apply(@NotNull Activity activity) {
        Intrinsics.checkNotNullParameter(activity, "activity");
        if (Build.VERSION.SDK_INT < 35) {
            return;
        }
        Window window = activity.getWindow();
        window.setDecorFitsSystemWindows(false);
        View root = activity.findViewById(android.R.id.content);
        final boolean ignoreTop = activity instanceof ConfigActivity;
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                Intrinsics.checkNotNullParameter(view, "view");
                Intrinsics.checkNotNullParameter(insets, "insets");
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                Intrinsics.checkNotNullExpressionValue(bars, "getInsets(...)");
                view.setPadding(bars.left, ignoreTop ? 0 : bars.top, bars.right, bars.bottom);
                return insets;
            }
        });
    }
}
