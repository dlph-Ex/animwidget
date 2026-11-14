package io.github.animwidget

import android.content.Context
import org.json.JSONObject
import java.io.File

enum class MediaKind { VIDEO, GIF, IMAGE }

enum class LayoutMode(val label: String, val ratio: Float) {
    FIT("原始比例", 0f),
    FILL("填充", 0f),
    R16_9("16:9", 16f / 9f),
    R4_3("4:3", 4f / 3f),
    R1_1("1:1", 1f),
    R9_16("9:16", 9f / 16f),
    R21_9("21:9", 21f / 9f);

    val fitsContent: Boolean get() = this == FIT
}

enum class ShapeKind {
    RECT, ROUNDED, SQUIRCLE, CIRCLE, ELLIPSE, CAPSULE,
    DIAMOND, TRIANGLE, HEXAGON, OCTAGON, STAR, STAR6, STAR8,
    LEAF, PETAL, CLOVER, HEART, TEARDROP, SHIELD, CROSS,
    WAVE, BOOKMARK, MESSAGE, BLOB;

    val label: String
        get() = when (this) {
            RECT -> "方形"
            ROUNDED -> "圆角"
            SQUIRCLE -> "超椭圆"
            CIRCLE -> "圆形"
            ELLIPSE -> "椭圆"
            CAPSULE -> "胶囊"
            DIAMOND -> "菱形"
            TRIANGLE -> "三角"
            HEXAGON -> "六边"
            OCTAGON -> "八边"
            STAR -> "五角星"
            STAR6 -> "六角星"
            STAR8 -> "八角星"
            LEAF -> "叶子"
            PETAL -> "花瓣"
            CLOVER -> "四叶"
            HEART -> "心形"
            TEARDROP -> "水滴"
            SHIELD -> "盾形"
            CROSS -> "十字"
            WAVE -> "波浪"
            BOOKMARK -> "书签"
            MESSAGE -> "对话框"
            BLOB -> "异形"
        }
}

/** 每个桌面小部件实例一份配置。 */
data class WidgetConfig(
    val widgetId: Int,
    val kind: MediaKind = MediaKind.VIDEO,
    val sourceName: String = "",
    val sourceUri: String = "",
    val fps: Int = 12,
    val maxEdge: Int = 720,
    val quality: Int = 90,
    val frameCount: Int = 0,
    val frameWidth: Int = 0,
    val frameHeight: Int = 0,
    val shape: ShapeKind = ShapeKind.RECT,
    val shapeAlpha: Int = 100,
    val border: Boolean = false,
    val layout: LayoutMode = LayoutMode.FILL,
    // 流畅覆盖层（悬浮窗，60fps）
    val overlayEnabled: Boolean = false,
    val overlayVisible: Boolean = false,
    val overlayX: Int = 0,        // dp，相对屏幕左上角
    val overlayY: Int = 0,        // dp
    val overlayWidth: Int = 0,    // dp，0 表示默认（屏幕宽度的 60%）
    val scalePercent: Int = 100,  // 等比例缩放 0–100（100 = 填满布局）
    val shadowPercent: Int = 0,   // 阴影 0–100（0 = 关闭）
    val rotation: Int = 0,        // 内容旋转 -360..360
    val shapeRotation: Int = 0,   // 蒙版旋转 -360..360
    val fitContent: Boolean = true // 适配画面：固定比例布局下 开=完整显示 / 关=裁切填满
)

/**
 * 多实例存储：
 * - 配置：SharedPreferences("widgets") 里每个 widgetId 一个 JSON
 * - 帧：files/w/<widgetId>/frames/f%05d.webp
 * - generation 递增用于让播放服务感知配置变化
 */
object WidgetStore {

    private const val PREF = "widgets"
    private const val KEY_GENERATION = "generation"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun dir(context: Context, widgetId: Int): File = File(context.filesDir, "w/$widgetId")

    fun framesDir(context: Context, widgetId: Int): File =
        File(dir(context, widgetId), "frames").apply { mkdirs() }

    fun frameFile(context: Context, widgetId: Int, index: Int): File =
        File(framesDir(context, widgetId), "f%05d.webp".format(index))

    fun tempFile(context: Context, widgetId: Int, index: Int): File =
        File(framesDir(context, widgetId), "tmp_%05d.webp".format(index))

    fun generation(context: Context): Int = prefs(context).getInt(KEY_GENERATION, 0)

    private fun bump(context: Context) {
        prefs(context).edit().putInt(KEY_GENERATION, generation(context) + 1).apply()
    }

    fun save(context: Context, config: WidgetConfig) {
        val json = JSONObject().apply {
            put("id", config.widgetId)
            put("kind", config.kind.name)
            put("name", config.sourceName)
            put("uri", config.sourceUri)
            put("fps", config.fps)
            put("edge", config.maxEdge)
            put("quality", config.quality)
            put("count", config.frameCount)
            put("fw", config.frameWidth)
            put("fh", config.frameHeight)
            put("shape", config.shape.name)
            put("alpha", config.shapeAlpha)
            put("border", config.border)
            put("layout", config.layout.name)
            put("scale", config.scalePercent)
            put("shadow", config.shadowPercent)
            put("rotate", config.rotation)
            put("shapeRotate", config.shapeRotation)
            put("fitContent", config.fitContent)
            put("overlay", config.overlayEnabled)
            put("overlayVisible", config.overlayVisible)
            put("ox", config.overlayX)
            put("oy", config.overlayY)
            put("ow", config.overlayWidth)
        }
        prefs(context).edit()
            .putString(config.widgetId.toString(), json.toString())
            .apply()
        bump(context)
    }

    fun load(context: Context, widgetId: Int): WidgetConfig? {
        val raw = prefs(context).getString(widgetId.toString(), null) ?: return null
        return try {
            val j = JSONObject(raw)
            WidgetConfig(
                widgetId = widgetId,
                kind = runCatching { MediaKind.valueOf(j.optString("kind", "VIDEO")) }
                    .getOrDefault(MediaKind.VIDEO),
                sourceName = j.optString("name", ""),
                sourceUri = j.optString("uri", ""),
                fps = j.optInt("fps", 12).coerceIn(1, 50),
                maxEdge = j.optInt("edge", 720).coerceIn(120, 2560),
                scalePercent = j.optInt(
                    "scale",
                    // 旧版本「填充（内容内缩）0–30」换算成等比例缩放百分比
                    (100 - j.optInt("pad", 0) / 2).coerceIn(0, 100)
                ).coerceIn(0, 100),
                shadowPercent = j.optInt("shadow", 0).coerceIn(0, 100),
                rotation = j.optInt("rotate", 0).coerceIn(-360, 360),
                shapeRotation = j.optInt("shapeRotate", 0).coerceIn(-360, 360),
                fitContent = j.optBoolean("fitContent", true),
                quality = j.optInt("quality", 90).coerceIn(50, 100),
                frameCount = j.optInt("count", 0),
                frameWidth = j.optInt("fw", 0),
                frameHeight = j.optInt("fh", 0),
                shape = runCatching { ShapeKind.valueOf(j.optString("shape", "ROUNDED")) }
                    .getOrDefault(ShapeKind.ROUNDED),
                shapeAlpha = j.optInt("alpha", 100).coerceIn(0, 100),
                border = j.optBoolean("border", false),
                layout = runCatching { LayoutMode.valueOf(j.optString("layout", "FILL")) }
                    .getOrDefault(LayoutMode.FILL),
                overlayEnabled = j.optBoolean("overlay", false),
                overlayVisible = j.optBoolean("overlayVisible", false),
                overlayX = j.optInt("ox", 0),
                overlayY = j.optInt("oy", 0),
                overlayWidth = j.optInt("ow", 0)
            )
        } catch (t: Throwable) {
            null
        }
    }

    fun all(context: Context): List<WidgetConfig> {
        val keys = prefs(context).all.keys
            .filter { it.toIntOrNull() != null }
            .sortedBy { it.toInt() }
        return keys.mapNotNull { load(context, it.toInt()) }
    }

    fun delete(context: Context, widgetId: Int) {
        prefs(context).edit().remove(widgetId.toString()).apply()
        dir(context, widgetId).deleteRecursively()
        bump(context)
    }

    /** 读出指定帧，按需采样解码（调用方负责 recycle）。 */
    fun loadFrame(
        context: Context,
        config: WidgetConfig,
        index: Int,
        targetWidth: Int = 0,
        targetHeight: Int = 0
    ): android.graphics.Bitmap? {
        if (config.frameCount <= 0) return null
        val safe = ((index % config.frameCount) + config.frameCount) % config.frameCount
        val file = frameFile(context, config.widgetId, safe)
        if (!file.exists()) return null
        val options = android.graphics.BitmapFactory.Options().apply {
            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            if (targetWidth > 0 && targetHeight > 0 &&
                config.frameWidth > 0 && config.frameHeight > 0
            ) {
                inSampleSize = sampleSize(
                    config.frameWidth, config.frameHeight, targetWidth, targetHeight
                )
            }
        }
        return try {
            android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (t: Throwable) {
            null
        }
    }

    private fun sampleSize(sw: Int, sh: Int, tw: Int, th: Int): Int {
        var sample = 1
        while (sw / (sample * 2) >= tw && sh / (sample * 2) >= th) sample *= 2
        return sample
    }
}
