package io.github.animwidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Movie
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * 选完素材后立刻在内存里抽一批帧，供配置页逐帧预览。
 *
 * 不落盘、不写缓存；帧已经按清晰度缩放并做过一次质量压缩，
 * 因此播放时只需要 FrameComposer 做形状/布局合成，和导出的合成路径完全一致。
 */
object PreviewFrames {

    data class Result(
        val frames: List<Bitmap>,
        val fps: Int,
        val durationMs: Long
    )

    fun load(
        context: Context,
        uri: Uri,
        kind: MediaKind,
        fps: Int,
        maxEdge: Int,
        quality: Int,
        limit: Int = 0,
        onProgress: (Int, Int) -> Unit
    ): Result {
        val safeFps = fps.coerceIn(1, 50)
        val frameCap = if (limit > 0) limit else (40 * safeFps).coerceAtMost(400)
        val edge = if (kind == MediaKind.IMAGE) {
            maxEdge.coerceIn(120, 2560)
        } else {
            maxEdge.coerceIn(120, 480)
        }
        val q = quality.coerceIn(50, 100)
        val frames = ArrayList<Bitmap>()

        try {
            when (kind) {
                MediaKind.IMAGE -> {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it, null, options)
                    }
                    if (options.outWidth > 0) {
                        val sample = sampleSize(options.outWidth, options.outHeight, edge, edge)
                        val decodeOptions = BitmapFactory.Options().apply {
                            inSampleSize = sample
                        }
                        val bitmap = context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it, null, decodeOptions)
                        } ?: error("image decode failed")
                        frames.add(compress(scaleToEdge(bitmap, edge), q).also {
                            if (it !== bitmap) bitmap.recycle()
                        })
                    }
                    onProgress(1, 1)
                }

                MediaKind.GIF -> {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("gif read failed")
                    val movie = Movie.decodeByteArray(bytes, 0, bytes.size)
                        ?: error("gif decode failed")
                    val duration = if (movie.duration() > 0) movie.duration() else 1000
                    val planned = (duration.toLong() * safeFps / 1000L).toInt()
                        .coerceIn(1, frameCap)
                    val ratio = minOf(1f, edge.toFloat() / max(movie.width(), movie.height()))
                    val outW = (movie.width() * ratio).toInt().coerceAtLeast(1)
                    val outH = (movie.height() * ratio).toInt().coerceAtLeast(1)
                    for (index in 0 until planned) {
                        movie.setTime(
                            (index.toLong() * 1000L / safeFps)
                                .coerceAtMost(duration - 1L).toInt()
                        )
                        val bitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.scale(
                            outW.toFloat() / movie.width().coerceAtLeast(1),
                            outH.toFloat() / movie.height().coerceAtLeast(1)
                        )
                        movie.draw(canvas, 0f, 0f)
                        val processed = compress(bitmap, q)
                        if (processed !== bitmap) bitmap.recycle()
                        frames.add(processed)
                        onProgress(index + 1, planned)
                    }
                }

                MediaKind.VIDEO -> {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, uri)
                        val durationMs = retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull() ?: 0L
                        val planned = if (durationMs > 0) {
                            (durationMs * safeFps / 1000L).toInt().coerceIn(1, frameCap)
                        } else {
                            limit
                        }
                        for (index in 0 until planned) {
                            val frame = retriever.getFrameAtTime(
                                index * 1_000_000L / safeFps,
                                MediaMetadataRetriever.OPTION_CLOSEST
                            )
                            if (frame != null) {
                                val scaled = scaleToEdge(frame, edge)
                                val processed = compress(scaled, q)
                                if (scaled !== frame) scaled.recycle()
                                frame.recycle()
                                frames.add(processed)
                            }
                            onProgress(index + 1, planned)
                        }
                    } finally {
                        retriever.release()
                    }
                }
            }
        } catch (t: Throwable) {
            frames.forEach { it.recycle() }
            frames.clear()
        }

        val durationMs = if (frames.isEmpty()) 0L else {
            frames.size.toLong() * 1000L / safeFps
        }
        return Result(frames, safeFps, durationMs)
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

    private fun sampleSize(sw: Int, sh: Int, tw: Int, th: Int): Int {
        var sample = 1
        while (sw / (sample * 2) >= tw && sh / (sample * 2) >= th) sample *= 2
        return sample
    }
}
