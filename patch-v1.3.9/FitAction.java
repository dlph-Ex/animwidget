package io.github.animwidget;

import android.app.Activity;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 「完整适配画面」右侧按钮的接线与动作。
 *
 * ConfigActivity 里没有这个按钮的引用（它是 v1.3.9 新加的），
 * 所以在这里用反射把 selectedFitContent 置 true，再调它自己的
 * refreshFitButton() / refreshPreviewNow()。
 */
public final class FitAction {

    private static Field fitField;
    private static Method refreshButton;
    private static Method refreshPreview;

    static {
        try {
            Field f = ConfigActivity.class.getDeclaredField("selectedFitContent");
            f.setAccessible(true);
            fitField = f;
        } catch (Throwable th) {
            fitField = null;
        }
        try {
            Method m = ConfigActivity.class.getDeclaredMethod("refreshFitButton");
            m.setAccessible(true);
            refreshButton = m;
        } catch (Throwable th) {
            refreshButton = null;
        }
        try {
            Method m = ConfigActivity.class.getDeclaredMethod("refreshPreviewNow");
            m.setAccessible(true);
            refreshPreview = m;
        } catch (Throwable th) {
            refreshPreview = null;
        }
    }

    private FitAction() {
    }

    /** 在配置页里给 btn_fit_full 挂上点击事件（幂等）。 */
    public static void wire(Activity activity) {
        if (!(activity instanceof ConfigActivity)) {
            return;
        }
        View content = activity.findViewById(android.R.id.content);
        if (content == null) {
            return;
        }
        final Activity target = activity;
        content.post(new Runnable() {
            @Override
            public void run() {
                int id = target.getResources().getIdentifier("btn_fit_full", "id", target.getPackageName());
                if (id == 0) {
                    return;
                }
                View button = target.findViewById(id);
                if (button == null || button.hasOnClickListeners()) {
                    return;
                }
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        applyFullFit(target);
                    }
                });
            }
        });
    }

    /** 让内容完整装进蒙版（fitContent = true），并立即重绘预览。 */
    public static void applyFullFit(Activity activity) {
        if (!(activity instanceof ConfigActivity)) {
            return;
        }
        try {
            if (fitField != null) {
                fitField.setBoolean(activity, true);
            }
            if (refreshButton != null) {
                refreshButton.invoke(activity);
            }
            if (refreshPreview != null) {
                refreshPreview.invoke(activity);
            }
        } catch (Throwable th) {
            // 反射失败就什么都不做，别崩界面
        }
    }
}
