package io.github.animwidget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Movie
import android.graphics.Paint
import android.os.SystemClock
import android.view.View

/**
 * 自绘的 GIF / 静态图覆盖层内容。
 * 直接画 Bitmap 并用 clipPath 套形状，所以支持任意形状蒙版，也不受 RemoteViews 限制。
 */
class MediaOverlayView(
    context: Context,
    private val bitmap: Bitmap?,
    private val movie: Movie?,
    private val shape: ShapeKind,
    private val alphaPercent: Int
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        alpha = (255f * alphaPercent.coerceIn(0, 100) / 100f).toInt()
    }
    private val frameDuration = (movie?.duration() ?: 0).coerceAtLeast(1)
    private var startedAt = 0L

    init {
        setWillNotDraw(false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startedAt = SystemClock.uptimeMillis()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0f || height <= 0f) return

        val path = ShapeFactory.path(shape, width, height)
        canvas.save()
        canvas.clipPath(path)

        if (movie != null && movie.width() > 0 && movie.height() > 0) {
            val elapsed = (SystemClock.uptimeMillis() - startedAt) % frameDuration
            movie.setTime(elapsed.toInt())
            val scale = minOf(width / movie.width(), height / movie.height())
            val dx = (width - movie.width() * scale) / 2f
            val dy = (height - movie.height() * scale) / 2f
            canvas.translate(dx, dy)
            canvas.scale(scale, scale)
            movie.draw(canvas, 0f, 0f)
        } else if (bitmap != null && !bitmap.isRecycled) {
            val scale = maxOf(width / bitmap.width, height / bitmap.height)
            val drawWidth = bitmap.width * scale
            val drawHeight = bitmap.height * scale
            val left = (width - drawWidth) / 2f
            val top = (height - drawHeight) / 2f
            canvas.drawBitmap(bitmap, null, android.graphics.RectF(left, top, left + drawWidth, top + drawHeight), paint)
        }
        canvas.restore()

        if (movie != null) {
            postInvalidateOnAnimation()
        }
    }
}
