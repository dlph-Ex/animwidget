package io.github.animwidget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View

/** 极简 HSV 调色器：上方 SV 方块，下方色相条。 */
class HsvPickerView(context: Context) : View(context) {

    var color: Int = 0xFF16A34A.toInt()
        set(value) {
            field = value
            val hsv = FloatArray(3)
            Color.colorToHSV(value, hsv)
            hue = hsv[0]
            sat = hsv[1]
            valueV = hsv[2]
            invalidate()
        }

    private var hue = 120f
    private var sat = 0.8f
    private var valueV = 0.8f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val svRect = RectF()
    private val hueRect = RectF()
    private val whiteGradient = LinearGradient(
        0f, 0f, 1f, 0f,
        Color.WHITE, 0x00FFFFFF, Shader.TileMode.CLAMP
    )
    private val blackGradient = LinearGradient(
        0f, 0f, 0f, 1f,
        Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = 12f * resources.displayMetrics.density
        val hueHeight = 30f * resources.displayMetrics.density
        svRect.set(padding, padding, w - padding, h - padding - hueHeight - padding)
        hueRect.set(padding, h - padding - hueHeight, w - padding, h - padding)
        whiteGradient
        blackGradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (svRect.isEmpty) return

        paint.shader = null
        paint.color = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        canvas.drawRoundRect(svRect, 12f, 12f, paint)

        paint.shader = whiteGradient
        canvas.save()
        canvas.clipRect(svRect)
        canvas.drawRect(svRect, paint)
        paint.shader = blackGradient
        canvas.drawRect(svRect, paint)
        canvas.restore()

        // 色相条
        val colors = intArrayOf(
            Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE,
            Color.MAGENTA, Color.RED
        )
        paint.shader = LinearGradient(
            hueRect.left, hueRect.top, hueRect.right, hueRect.top,
            colors, null, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(hueRect, 10f, 10f, paint)
        paint.shader = null

        // 指示器
        val sx = svRect.left + sat * svRect.width()
        val sy = svRect.top + (1f - valueV) * svRect.height()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * resources.displayMetrics.density
        paint.color = Color.WHITE
        canvas.drawCircle(sx, sy, 9f * resources.displayMetrics.density, paint)
        paint.color = Color.BLACK
        paint.strokeWidth = 1f * resources.displayMetrics.density
        canvas.drawCircle(sx, sy, 9f * resources.displayMetrics.density, paint)

        val hx = hueRect.left + (hue / 360f) * hueRect.width()
        paint.color = Color.WHITE
        paint.strokeWidth = 3f * resources.displayMetrics.density
        canvas.drawCircle(hx, hueRect.centerY(), 9f * resources.displayMetrics.density, paint)
        paint.style = Paint.Style.FILL
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (event.y >= hueRect.top - 10f) {
                    hue = ((event.x - hueRect.left) / hueRect.width()).coerceIn(0f, 1f) * 360f
                } else {
                    sat = ((event.x - svRect.left) / svRect.width()).coerceIn(0f, 1f)
                    valueV = 1f - ((event.y - svRect.top) / svRect.height()).coerceIn(0f, 1f)
                }
                color = Color.HSVToColor(floatArrayOf(hue, sat, valueV))
                invalidate()
                return true
            }
        }
        return true
    }
}
