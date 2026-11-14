package io.github.animwidget

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlin.math.roundToInt

/** 读取素材自身的帧率，用于界面提示。 */
object SourceFps {

    /** 返回素材时长（毫秒），图片为 null。 */
    fun duration(context: Context, uri: Uri, kind: MediaKind): Long? {
        return try {
            when (kind) {
                MediaKind.VIDEO -> {
                    val r = android.media.MediaMetadataRetriever()
                    try {
                        r.setDataSource(context, uri)
                        r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull()
                    } finally {
                        r.release()
                    }
                }
                MediaKind.GIF -> {
                    val m = context.contentResolver.openInputStream(uri)?.use { android.graphics.Movie.decodeStream(it) }
                    if (m != null && m.duration() > 0) m.duration().toLong() else null
                }
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    /** 返回 "宽x高" 或 null，分别适配视频 / GIF / 图片。 */
    fun resolution(context: Context, uri: Uri, kind: MediaKind): String? {
        return try {
            when (kind) {
                MediaKind.VIDEO -> {
                    val r = android.media.MediaMetadataRetriever()
                    try {
                        r.setDataSource(context, uri)
                        val w = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                        val h = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                        if (w != null && h != null) "${w}x$h" else null
                    } finally { r.release() }
                }
                MediaKind.GIF -> {
                    val m = context.contentResolver.openInputStream(uri)?.use { android.graphics.Movie.decodeStream(it) }
                    if (m != null) "${m.width()}x${m.height()}" else null
                }
                MediaKind.IMAGE -> {
                    val o = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use {
                        android.graphics.BitmapFactory.decodeStream(it, null, o)
                    }
                    if (o.outWidth > 0) "${o.outWidth}x${o.outHeight}" else null
                }
            }
        } catch (_: Throwable) { null }
    }

    fun video(context: Context, uri: Uri): Int? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(context, uri, null)
            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (!mime.startsWith("video/")) continue
                if (!format.containsKey(MediaFormat.KEY_FRAME_RATE)) continue
                val fps = try {
                    format.getInteger(MediaFormat.KEY_FRAME_RATE)
                } catch (_: Throwable) {
                    try {
                        format.getFloat(MediaFormat.KEY_FRAME_RATE).roundToInt()
                    } catch (_: Throwable) {
                        return null
                    }
                }
                if (fps > 0) return fps
            }
            null
        } catch (_: Throwable) {
            null
        } finally {
            try {
                extractor.release()
            } catch (_: Throwable) {
            }
        }
    }

    /** 解析 GIF 的帧数 / 总延迟，算平均帧率。 */
    fun gif(context: Context, uri: Uri): Int? {
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: Throwable) {
            null
        } ?: return null
        if (bytes.size < 14) return null

        var pos = 6
        val packed = bytes[10].toInt() and 0xFF
        if (packed and 0x80 != 0) {
            pos = 13 + 3 * (1 shl ((packed and 0x07) + 1))
        } else {
            pos = 13
        }

        var frames = 0
        var totalDelayMs = 0

        while (pos < bytes.size) {
            when (bytes[pos].toInt() and 0xFF) {
                0x21 -> {
                    val label = if (pos + 1 < bytes.size) bytes[pos + 1].toInt() and 0xFF else -1
                    if (label == 0xF9 && pos + 6 < bytes.size) {
                        val delay = (bytes[pos + 4].toInt() and 0xFF) or
                            ((bytes[pos + 5].toInt() and 0xFF) shl 8)
                        totalDelayMs += delay * 10
                    }
                    pos += 2
                    pos = skipSubBlocks(bytes, pos)
                }
                0x2C -> {
                    frames++
                    if (pos + 10 > bytes.size) break
                    val local = bytes[pos + 9].toInt() and 0xFF
                    val localTable = if (local and 0x80 != 0) {
                        3 * (1 shl ((local and 0x07) + 1))
                    } else {
                        0
                    }
                    // 0x2C + 9 字节描述 + 局部色表 + 1 字节 LZW 最小码长
                    pos += 10 + localTable + 1
                    pos = skipSubBlocks(bytes, pos)
                }
                0x3B -> break
                else -> pos++
            }
        }

        if (frames <= 0 || totalDelayMs <= 0) return null
        return (frames * 1000f / totalDelayMs).roundToInt().coerceAtLeast(1)
    }

    private fun skipSubBlocks(bytes: ByteArray, start: Int): Int {
        var pos = start
        while (pos < bytes.size) {
            val size = bytes[pos].toInt() and 0xFF
            pos++
            if (size == 0) break
            pos += size
        }
        return pos
    }
}
