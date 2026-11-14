package io.github.animwidget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** 24 种形状蒙版。所有形状都在目标位图的完整尺寸内绘制，其余区域透明。 */
object ShapeFactory {

    val all: List<ShapeKind> = ShapeKind.values().toList()

    fun path(shape: ShapeKind, width: Float, height: Float): Path {
        val w = width.coerceAtLeast(1f)
        val h = height.coerceAtLeast(1f)
        val unit = min(w, h)
        val rect = RectF(0f, 0f, w, h)
        val path = Path()
        val cx = w / 2f
        val cy = h / 2f

        when (shape) {
            ShapeKind.RECT -> path.addRect(rect, Path.Direction.CW)
            ShapeKind.ROUNDED -> {
                val r = unit * 0.18f
                path.addRoundRect(rect, r, r, Path.Direction.CW)
            }
            ShapeKind.SQUIRCLE -> {
                val r = unit * 0.42f
                path.addRoundRect(rect, r, r, Path.Direction.CW)
            }
            ShapeKind.CIRCLE -> {
                val d = unit
                val left = (w - d) / 2f
                val top = (h - d) / 2f
                path.addOval(RectF(left, top, left + d, top + d), Path.Direction.CW)
            }
            ShapeKind.ELLIPSE -> path.addOval(rect, Path.Direction.CW)
            ShapeKind.CAPSULE -> {
                val r = unit / 2f
                path.addRoundRect(rect, r, r, Path.Direction.CW)
            }
            ShapeKind.DIAMOND -> {
                path.moveTo(cx, 0f)
                path.lineTo(w, cy)
                path.lineTo(cx, h)
                path.lineTo(0f, cy)
                path.close()
            }
            ShapeKind.TRIANGLE -> {
                path.moveTo(cx, h * 0.06f)
                path.lineTo(w * 0.96f, h * 0.95f)
                path.lineTo(w * 0.04f, h * 0.95f)
                path.close()
            }
            ShapeKind.HEXAGON -> polygon(path, cx, cy, unit * 0.5f, 6, 0f)
            ShapeKind.OCTAGON -> polygon(path, cx, cy, unit * 0.5f, 8, 22.5f)
            ShapeKind.STAR -> star(path, cx, cy, unit * 0.5f, unit * 0.21f, 5)
            ShapeKind.STAR6 -> star(path, cx, cy, unit * 0.5f, unit * 0.26f, 6)
            ShapeKind.STAR8 -> star(path, cx, cy, unit * 0.5f, unit * 0.3f, 8)
            ShapeKind.LEAF -> {
                val big = unit * 0.5f
                val small = unit * 0.04f
                path.addRoundRect(
                    rect,
                    floatArrayOf(big, big, small, small, big, big, small, small),
                    Path.Direction.CW
                )
            }
            ShapeKind.PETAL -> {
                val r = unit * 0.21f
                val orbit = unit * 0.29f
                for (i in 0 until 10) {
                    val a = Math.PI * 2 * i / 10
                    val circle = Path().apply {
                        addCircle(
                            cx + cos(a).toFloat() * orbit,
                            cy + sin(a).toFloat() * orbit,
                            r,
                            Path.Direction.CW
                        )
                    }
                    path.op(circle, Path.Op.UNION)
                }
            }
            ShapeKind.CLOVER -> {
                val r = unit * 0.26f
                val offset = unit * 0.2f
                for (dx in intArrayOf(-1, 1)) {
                    for (dy in intArrayOf(-1, 1)) {
                        val circle = Path().apply {
                            addCircle(cx + dx * offset, cy + dy * offset, r, Path.Direction.CW)
                        }
                        path.op(circle, Path.Op.UNION)
                    }
                }
            }
            ShapeKind.HEART -> {
                path.moveTo(cx, h * 0.92f)
                path.cubicTo(w * 0.02f, h * 0.56f, w * 0.06f, h * 0.06f, cx, h * 0.26f)
                path.cubicTo(w * 0.94f, h * 0.06f, w * 0.98f, h * 0.56f, cx, h * 0.92f)
                path.close()
            }
            ShapeKind.TEARDROP -> {
                path.moveTo(cx, 0f)
                path.cubicTo(w * 0.85f, h * 0.35f, w * 0.9f, h * 0.8f, cx, h)
                path.cubicTo(w * 0.1f, h * 0.8f, w * 0.15f, h * 0.35f, cx, 0f)
                path.close()
            }
            ShapeKind.SHIELD -> {
                path.moveTo(w * 0.06f, h * 0.12f)
                path.quadTo(cx, 0f, w * 0.94f, h * 0.12f)
                path.lineTo(w * 0.88f, h * 0.58f)
                path.quadTo(w * 0.8f, h * 0.86f, cx, h)
                path.quadTo(w * 0.2f, h * 0.86f, w * 0.12f, h * 0.58f)
                path.close()
            }
            ShapeKind.CROSS -> {
                val t = w * 0.18f
                val b = h * 0.18f
                path.moveTo(cx - t / 2, 0f)
                path.lineTo(cx + t / 2, 0f)
                path.lineTo(cx + t / 2, cy - b / 2)
                path.lineTo(w, cy - b / 2)
                path.lineTo(w, cy + b / 2)
                path.lineTo(cx + t / 2, cy + b / 2)
                path.lineTo(cx + t / 2, h)
                path.lineTo(cx - t / 2, h)
                path.lineTo(cx - t / 2, cy + b / 2)
                path.lineTo(0f, cy + b / 2)
                path.lineTo(0f, cy - b / 2)
                path.lineTo(cx - t / 2, cy - b / 2)
                path.close()
            }
            ShapeKind.WAVE -> {
                path.moveTo(0f, h * 0.12f)
                path.quadTo(w * 0.25f, 0f, w * 0.5f, h * 0.12f)
                path.quadTo(w * 0.75f, h * 0.24f, w, h * 0.12f)
                path.lineTo(w, h * 0.88f)
                path.quadTo(w * 0.75f, h, w * 0.5f, h * 0.88f)
                path.quadTo(w * 0.25f, h * 0.76f, 0f, h * 0.88f)
                path.close()
            }
            ShapeKind.BOOKMARK -> {
                path.moveTo(0f, 0f)
                path.lineTo(w, 0f)
                path.lineTo(w, h)
                path.lineTo(cx, h * 0.78f)
                path.lineTo(0f, h)
                path.close()
            }
            ShapeKind.MESSAGE -> {
                path.addRoundRect(RectF(0f, 0f, w, h * 0.8f), unit * 0.18f, unit * 0.18f, Path.Direction.CW)
                val tail = Path().apply {
                    moveTo(w * 0.24f, h * 0.78f)
                    lineTo(w * 0.38f, h)
                    lineTo(w * 0.48f, h * 0.78f)
                    close()
                }
                path.op(tail, Path.Op.UNION)
            }
            ShapeKind.BLOB -> {
                val rx = w * 0.5f
                val ry = h * 0.5f
                val wobble = floatArrayOf(0.92f, 1.06f, 0.95f, 1.08f, 0.9f, 1.05f, 0.96f, 1.04f)
                for (i in 0..16) {
                    val a = Math.PI * 2 * i / 16
                    val k = wobble[i % wobble.size]
                    val x = cx + cos(a).toFloat() * rx * k
                    val y = cy + sin(a).toFloat() * ry * k
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
        }
        return path
    }

    private fun polygon(path: Path, cx: Float, cy: Float, radius: Float, sides: Int, rotation: Float) {
        for (i in 0 until sides) {
            val a = Math.toRadians(rotation.toDouble()) + Math.PI * 2 * i / sides - Math.PI / 2
            val x = cx + cos(a).toFloat() * radius
            val y = cy + sin(a).toFloat() * radius
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }

    private fun star(path: Path, cx: Float, cy: Float, outer: Float, inner: Float, points: Int) {
        for (i in 0 until points * 2) {
            val radius = if (i % 2 == 0) outer else inner
            val a = -Math.PI / 2 + Math.PI * i / points
            val x = cx + cos(a).toFloat() * radius
            val y = cy + sin(a).toFloat() * radius
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }

    fun apply(content: Bitmap, shape: ShapeKind, alphaPercent: Int, border: Boolean): Bitmap {
        val alpha = alphaPercent.coerceIn(0, 100)
        if (shape == ShapeKind.RECT && alpha == 100 && !border) return content

        val width = content.width
        val height = content.height
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.alpha = (255f * alpha / 100f).toInt().coerceIn(0, 255)
        val maskPath = path(shape, width.toFloat(), height.toFloat())

        canvas.save()
        canvas.clipPath(maskPath)
        canvas.drawBitmap(content, 0f, 0f, paint)
        canvas.restore()

        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawPath(maskPath, maskPaint)

        if (border) {
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = (min(width, height) * 0.018f).coerceAtLeast(2f)
                color = Color.argb(200, 255, 255, 255)
            }
            canvas.drawPath(maskPath, stroke)
        }
        return out
    }
}
