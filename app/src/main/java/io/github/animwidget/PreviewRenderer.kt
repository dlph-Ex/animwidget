package io.github.animwidget

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * 预览专用：按清晰度缩放、按质量做一次有损编码，再交给 FrameComposer 合成。
 * 合成部分与导出完全共用 FrameComposer，保证像素级一致。
 */
object PreviewRenderer {

    fun render(
        source: Bitmap,
        viewWidth: Int,
        viewHeight: Int,
        layout: LayoutMode,
        shape: ShapeKind,
        alphaPercent: Int,
        border: Boolean,
        maxEdge: Int,
        quality: Int,
        scalePercent: Int = 100,
        rotation: Int = 0,
        shapeRotation: Int = 0,
        fitContent: Boolean = true,
        shadowPercent: Int = 0
    ): Bitmap {
        val width = viewWidth.coerceIn(96, 1080)
        val height = viewHeight.coerceIn(96, 1080)

        val scaled = scaleToEdge(source, maxEdge.coerceIn(120, 1920))
        val compressed = compress(scaled, quality.coerceIn(50, 100))

        return try {
            FrameComposer.compose(
                compressed, width, height, layout, shape, alphaPercent, border,
                scalePercent, rotation, shapeRotation, fitContent, shadowPercent
            )
        } finally {
            if (scaled !== source && scaled !== compressed) scaled.recycle()
            if (compressed !== source) compressed.recycle()
        }
    }

    private fun scaleToEdge(source: Bitmap, maxEdge: Int): Bitmap {
        val longest = max(source.width, source.height)
        if (longest <= maxEdge || longest == 0) return source
        val ratio = maxEdge.toFloat() / longest
        return Bitmap.createScaledBitmap(
            source,
            (source.width * ratio).coerceAtLeast(1f).toInt(),
            (source.height * ratio).coerceAtLeast(1f).toInt(),
            true
        )
    }

    @Suppress("DEPRECATION")
    private fun compress(source: Bitmap, quality: Int): Bitmap {
        return try {
            val bytes = ByteArrayOutputStream()
            val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.WEBP
            }
            if (!source.compress(format, quality, bytes)) return source
            val data = bytes.toByteArray()
            BitmapFactory.decodeByteArray(data, 0, data.size) ?: source
        } catch (t: Throwable) {
            source
        }
    }
}
