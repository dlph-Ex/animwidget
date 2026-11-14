package io.github.animwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

import org.jetbrains.annotations.NotNull;

/**
 * v1.10.491：多格组件的 provider（桌面组件列表里新增的一项「多格组件」）。
 *
 * <p>除了「初始布局是 2×3 六格」之外，它和 {@link WidgetProvider} 完全一样：
 * 配置仍走 `ConfigActivity`（{@code widget_multi_info.xml} 里 {@code android:configure}），
 * 播放仍走同一条推帧链 —— 差别只在出口：{@link WidgetUriPush} 发现这一路是多格时，
 * 用 {@link MultiCell#views} 一次把 6 个格子都设上，于是**每拍只 apply 一次**。</p>
 *
 * <p>为什么要单独一个 provider 而不是在 {@link WidgetProvider} 里加开关：
 * 桌面组件列表是按 receiver + meta-data 注册的，两种形态必须是两个 receiver；
 * 而且「这一路是多格」可以由系统事实（provider 组件名）判定，不需要我们存标记。</p>
 */
public final class MultiCellProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(@NotNull Context context, @NotNull AppWidgetManager appWidgetManager,
                         @NotNull int[] appWidgetIds) {
        CrashLog.install(context);
        Diag.warn(context, "A02", "onUpdate 进入（多格）, 组件数=" + appWidgetIds.length);
        for (int widgetId : appWidgetIds) {
            try {
                MultiCell.forget(widgetId);          // provider 刚确定，缓存可能还没有
                WidgetConfig config = WidgetStore.INSTANCE.load(context, widgetId);
                if (config != null) {
                    WidgetRender.INSTANCE.render(context, appWidgetManager, config, 0, null);
                } else {
                    // 还没选素材：六个空格子不显示任何东西，等 ConfigActivity 回填
                    appWidgetManager.updateAppWidget(widgetId, MultiCell.views(context, null));
                }
            } catch (Throwable th) {
                Diag.warn(context, "W03", "onUpdate（多格）组件 " + widgetId + " 异常: "
                        + th.getClass().getSimpleName()
                        + (th.getMessage() != null ? "/" + th.getMessage() : ""));
                CrashLog.save(context, Thread.currentThread(), th);
            }
        }
        PlaybackService.Companion.start(context);
    }

    @Override
    public void onDeleted(@NotNull Context context, @NotNull int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            try {
                MultiCell.forget(id);
                WidgetUriPush.clearWidget(context, id);
            } catch (Throwable ignored) {
            }
        }
        super.onDeleted(context, appWidgetIds);
    }
}
