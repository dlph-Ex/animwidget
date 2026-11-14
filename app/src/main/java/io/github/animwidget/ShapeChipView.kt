package io.github.animwidget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import kotlin.math.min

/** 形状选择格：直接画出缩略图，不再用文字描述。 */
class ShapeChipView(context: Context) : View(context) {

    var shape: ShapeKind = ShapeKind.ROUNDED
    var chosen: Boolean = false

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val density = resources.displayMetrics.density
        val radius = 12f * density

        bgPaint.color = if (chosen) 0xFFDCFCE7.toInt() else 0xFFE7EAEE.toInt()
        canvas.drawRoundRect(0f, 0f, w, h, radius, radius, bgPaint)

        val densityText = resources.displayMetrics.density
        textPaint.textSize = 10f * densityText
        textPaint.color = if (chosen) 0xFF16A34A.toInt() else 0xFF6B7280.toInt()
        val labelHeight = 14f * densityText
        val shapeHeight = h - labelHeight

        val size = min(w, shapeHeight) * 0.72f
        canvas.save()
        canvas.translate((w - size) / 2f, (shapeHeight - size) / 2f)
        val path = ShapeFactory.path(shape, size, size)
        fillPaint.color = 0xFF374151.toInt()
        canvas.drawPath(path, fillPaint)
        canvas.restore()

        if (chosen) {
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 3f * density
            strokePaint.color = 0xFF16A34A.toInt()
            canvas.drawRoundRect(0f, 0f, w, h, radius, radius, strokePaint)
        }
    }
}
