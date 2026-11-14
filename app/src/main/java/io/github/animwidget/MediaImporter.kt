package io.github.animwidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Movie
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/** 把 MP4 / GIF / 图片导入成某个 widget 的 WebP 帧序列。 */
object MediaImporter {

    const val MAX_FRAMES = 600
    private const val MIN_FRAMES = 2
    private const val MAX_TOTAL_BYTES = 250L * 1024 * 1024

    data class Result(val frameCount: Int, val width: Int, val height: Int)

    fun import(
        context: Context,
        config: WidgetConfig,
        uri: Uri,
        onProgress: (Int, Int) -> Unit
    ): Result {
        val dir = WidgetStore.framesDir(context, config.widgetId)
        dir.listFiles()?.forEach { it.delete() }
        val frameId = config.widgetId
        val fps = config.fps.coerceIn(1, 50)
        val edge = config.maxEdge.coerceIn(120, 2560)
        val quality = config.quality.coerceIn(50, 100)

        return when (config.kind) {
            MediaKind.IMAGE -> importImage(context, frameId, uri, edge, quality, onProgress)
            MediaKind.GIF -> importGif(context, frameId, uri, fps, edge, quality, onProgress)
            MediaKind.VIDEO -> importVideo(context, frameId, uri, fps, edge, quality, onProgress)
        }
    }

    private fun importImage(
        context: Context,
        frameId: Int,
        uri: Uri,
        maxEdge: Int,
        quality: Int,
        onProgress: (Int, Int) -> Unit
    ): Result {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            error(context.getString(R.string.err_image_decode))
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge, maxEdge)
        }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: error(context.getString(R.string.err_image_decode))

        val scaled = scaleToMaxEdge(bitmap, maxEdge)
        if (scaled !== bitmap) bitmap.recycle()
        val bytes = writeFrame(scaled, WidgetStore.tempFile(context, frameId, 0), quality)
        val width = scaled.width
        val height = scaled.height
        scaled.recycle()
        require(bytes > 0L) { context.getString(R.string.err_image_decode) }

        onProgress(1, 1)
        return Result(compact(context, frameId), width, height)
    }

    private fun importVideo(
        context: Context,
        frameId: Int,
        uri: Uri,
        fps: Int,
        maxEdge: Int,
        quality: Int,
        onProgress: (Int, Int) -> Unit
    ): Result {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            require(durationMs > 0L) { context.getString(R.string.err_no_duration) }

            val usableMs = durationMs.coerceAtMost(40_000L)
            val planned = ((usableMs * fps) / 1000L).toInt().coerceIn(MIN_FRAMES, MAX_FRAMES)
            var totalBytes = 0L
            var width = 0
            var height = 0

            for (index in 0 until planned) {
                val frame = retriever.getFrameAtTime(
                    index * 1_000_000L / fps,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
                if (frame != null) {
                    val scaled = scaleToMaxEdge(frame, maxEdge)
                    if (width == 0) {
                        width = scaled.width
                        height = scaled.height
                    }
                    val bytes = writeFrame(
                        scaled,
                        WidgetStore.tempFile(context, frameId, index),
                        quality
                    )
                    if (scaled !== frame) scaled.recycle()
                    frame.recycle()
                    if (bytes <= 0L) break
                    totalBytes += bytes
                }
                onProgress(index + 1, planned)
                if (totalBytes >= MAX_TOTAL_BYTES) break
            }
            return Result(compact(context, frameId), width, height)
        } finally {
            retriever.release()
        }
    }

    @Suppress("DEPRECATION")
    private fun importGif(
        context: Context,
        frameId: Int,
        uri: Uri,
        fps: Int,
        maxEdge: Int,
        quality: Int,
        onProgress: (Int, Int) -> Unit
    ): Result {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error(context.getString(R.string.err_gif_decode))
        val movie = Movie.decodeByteArray(bytes, 0, bytes.size)
            ?: error(context.getString(R.string.err_gif_decode))

        val duration = if (movie.duration() > 0) movie.duration() else 1000
        val usable = duration.coerceAtMost(40_000)
        val planned = (usable.toLong() * fps / 1000L).toInt().coerceIn(MIN_FRAMES, MAX_FRAMES)

        val sourceWidth = movie.width().coerceAtLeast(1)
        val sourceHeight = movie.height().coerceAtLeast(1)
        val ratio = minOf(1f, maxEdge.toFloat() / max(sourceWidth, sourceHeight))
        val outWidth = (sourceWidth * ratio).toInt().coerceAtLeast(1)
        val outHeight = (sourceHeight * ratio).toInt().coerceAtLeast(1)

        var totalBytes = 0L
        for (index in 0 until planned) {
            // 关键：按设定帧率沿时间轴取点（index/fps），而不是把整段均匀压缩到 planned 帧。
            // 否则帧数被 MAX_FRAMES 截断时，播放时长会短于原时长，看起来就是"加速"。
            val timeMs = (index.toLong() * 1000L / fps).coerceAtMost(duration - 1L)
            movie.setTime(timeMs.toInt())
            val bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.scale(outWidth.toFloat() / sourceWidth, outHeight.toFloat() / sourceHeight)
            movie.draw(canvas, 0f, 0f)

            val frameBytes = writeFrame(
                bitmap,
                WidgetStore.tempFile(context, frameId, index),
                quality
            )
            bitmap.recycle()
            if (frameBytes <= 0L) break
            totalBytes += frameBytes

            onProgress(index + 1, planned)
            if (totalBytes >= MAX_TOTAL_BYTES) break
        }
        return Result(compact(context, frameId), outWidth, outHeight)
    }

    private fun writeFrame(bitmap: Bitmap, target: File, quality: Int): Long {
        val format = webpFormat()
        return try {
            FileOutputStream(target).use { output ->
                if (!bitmap.compress(format, quality, output)) return 0L
            }
            target.length()
        } catch (t: Throwable) {
            0L
        }
    }

    @Suppress("DEPRECATION")
    private fun webpFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }

    private fun scaleToMaxEdge(source: Bitmap, maxEdge: Int): Bitmap {
        val longest = max(source.width, source.height)
        if (longest <= maxEdge || longest == 0) return source
        val ratio = maxEdge.toFloat() / longest
        val width = (source.width * ratio).toInt().coerceAtLeast(1)
        val height = (source.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    private fun sampleSize(sw: Int, sh: Int, tw: Int, th: Int): Int {
        var sample = 1
        while (sw / (sample * 2) >= tw && sh / (sample * 2) >= th) sample *= 2
        return sample
    }

    private fun compact(context: Context, frameId: Int): Int {
        val dir = WidgetStore.framesDir(context, frameId)
        val temps = dir.listFiles { f -> f.name.startsWith("tmp_") }
            ?.sortedBy { it.name } ?: return 0
        var count = 0
        for (file in temps) {
            val target = WidgetStore.frameFile(context, frameId, count)
            if (file.absolutePath != target.absolutePath) file.renameTo(target)
            count++
        }
        return count
    }
}
