package io.github.animwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Bundle

/** 桌面小部件入口：每个实例独立配置，静态首帧 + 启动播放服务。 */
class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            val config = WidgetStore.load(context, widgetId) ?: continue
            WidgetRender.render(context, appWidgetManager, config, 0, null)
        }
        PlaybackService.start(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val config = WidgetStore.load(context, appWidgetId) ?: return
        WidgetRender.render(context, appWidgetManager, config, 0, null)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            WidgetStore.delete(context, widgetId)
        }
    }

    override fun onDisabled(context: Context) {
        PlaybackService.stop(context)
    }

    companion object {
        fun widgetIds(context: Context): IntArray {
            val manager = AppWidgetManager.getInstance(context)
            return manager.getAppWidgetIds(ComponentName(context, WidgetProvider::class.java))
        }
    }
}
