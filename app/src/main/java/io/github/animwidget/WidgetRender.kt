package io.github.animwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.LruCache
import android.widget.RemoteViews
import kotlin.math.max
import kotlin.math.min

/** 帧位图缓存：key 由 widget、尺寸、帧号组成。 */
class FrameCache(maxKb: Int) {
    private val cache = object : LruCache<String, Bitmap>(maxKb.coerceAtLeast(1024)) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            if (evicted) oldValue.recycle()
        }
    }

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    fun clear() {
        cache.evictAll()
    }
}

object WidgetRender {

    fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun widgetSize(context: Context, manager: AppWidgetManager, widgetId: Int): IntArray {
        val options = manager.getAppWidgetOptions(widgetId)
        val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
        val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
        val density = context.resources.displayMetrics.density
        val width = (minWidthDp * density).toInt().coerceIn(64, 1440)
        val height = (minHeightDp * density).toInt().coerceIn(64, 1440)
        return intArrayOf(width, height)
    }

    /**
     * 渲染指定帧到某个小部件。返回本次耗时（毫秒），用于播放节拍补偿。
     * 位图会放进 cache，后续同键直接命中。
     */
    fun render(
        context: Context,
        manager: AppWidgetManager,
        config: WidgetConfig,
        index: Int,
        cache: FrameCache?
    ): Long {
        val started = android.os.SystemClock.elapsedRealtime()
        val size = widgetSize(context, manager, config.widgetId)
        val key = buildString {
            append(config.widgetId).append(':')
            append(size[0]).append(':').append(size[1]).append(':')
            append(config.layout.name).append(':')
            append(config.shape.name).append(':')
            append(config.shapeAlpha).append(':')
            append(config.scalePercent).append(':')
            append(config.shadowPercent).append(':')
            append(config.fitContent).append(':')
            append(config.rotation).append(':')
            append(config.shapeRotation).append(':')
            append(if (config.border) 1 else 0).append(':')
            append(index)
        }

        val cached = cache?.get(key)
        val bitmap: Bitmap = if (cached != null) {
            cached
        } else {
            val frame = WidgetStore.loadFrame(context, config, index, size[0], size[1])
                ?: return 0L
            val masked = try {
                FrameComposer.compose(
                    frame, size[0], size[1], config.layout,
                    config.shape, config.shapeAlpha, config.border,
                    config.scalePercent, config.rotation, config.shapeRotation,
                    config.fitContent, config.shadowPercent
                )
            } finally {
                frame.recycle()
            }
            cache?.put(key, masked)
            masked
        }

        val views = RemoteViews(context.packageName, R.layout.widget_media)
        views.setImageViewBitmap(R.id.widget_image, bitmap)
        views.setOnClickPendingIntent(R.id.widget_image, contentIntent(context))
        try {
            manager.updateAppWidget(config.widgetId, views)
        } catch (_: Throwable) {
        }
        return android.os.SystemClock.elapsedRealtime() - started
    }
}
