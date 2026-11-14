package io.github.animwidget

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

/**
 * 唯一的帧合成实现：布局裁剪 + 形状蒙版 + 不透明度 + 边框 + 旋转 + 等比例缩放 + 阴影。
 * 导出（WidgetRender）与预览（PreviewRenderer）都走这里，保证像素一致。
 *
 * 几何规则（v1.1.0 起）：
 * 1. 蒙版始终填满「布局内容区」，不再按素材比例缩小 —— 这是「蒙版填不满布局」的修复点；
 *    - 布局=填充 / 原始比例：内容区 = 整个组件
 *    - 布局=16:9 / 4:3 / 1:1 …：内容区 = 居中的比例框
 *    - 布局=原始比例：内容区按素材宽高比贴合，蒙版与画面严丝合缝
 * 2. 内容是否裁切：
 *    - 填充：裁切填满（布局语义就是“填满”）
 *    - 原始比例：完整显示
 *    - 固定比例：由「适配画面」开关决定（开=完整显示，关=裁切填满）
 * 3. 阴影开启时，蒙版按阴影半径向内留白，阴影画在形状外侧。
 */
object FrameComposer {

    fun compose(
        source: Bitmap,
        width: Int,
        height: Int,
        layout: LayoutMode,
        shape: ShapeKind,
        alphaPercent: Int,
        border: Boolean,
        scalePercent: Int = 100,
        rotation: Int = 0,
        shapeRotation: Int = 0,
        fitContent: Boolean = true,
        shadowPercent: Int = 0
    ): Bitmap {
        val safeWidth = width.coerceIn(1, 4096)
        val safeHeight = height.coerceIn(1, 4096)
        val out = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val w = safeWidth.toFloat()
        val h = safeHeight.toFloat()

        // 等比例缩放：内容 + 蒙版 + 边框 + 阴影整体围绕中心缩放
        val scale = scalePercent.coerceIn(0, 100) / 100f
        if (scale <= 0.001f) return out
        canvas.save()
        if (scale < 0.999f) canvas.scale(scale, scale, w / 2f, h / 2f)

        // 蒙版几何：始终填满布局内容区
        val area = contentRect(source, safeWidth, safeHeight, layout)
        val strength = shadowPercent.coerceIn(0, 100)
        val margin = if (strength > 0) {
            (min(safeWidth, safeHeight) * 0.055f * strength / 100f).coerceAtLeast(2f)
        } else {
            0f
        }
        val inset = RectF(
            area.left + margin,
            area.top + margin,
            area.right - margin,
            area.bottom - margin
        )
        val hasInset = inset.width() > 8f && inset.height() > 8f
        val box = if (hasInset) inset else area

        val path = ShapeFactory.path(shape, box.width(), box.height())
        path.offset(box.left, box.top)
        if (shapeRotation != 0) {
            val matrix = Matrix()
            matrix.postRotate(shapeRotation.toFloat(), w / 2f, h / 2f)
            path.transform(matrix)
        }

        if (strength > 0 && hasInset) {
            drawShadow(canvas, path, strength, margin)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.alpha = (255f * alphaPercent.coerceIn(0, 100) / 100f).toInt()

        val fit = when (layout) {
            LayoutMode.FIT -> true
            LayoutMode.FILL -> false
            else -> fitContent
        }

        canvas.save()
        canvas.clipPath(path)
        drawContent(canvas, source, box, rotation, paint, fit)
        canvas.restore()

        if (border) {
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = (min(safeWidth, safeHeight) * 0.018f).coerceAtLeast(2f)
                color = Color.argb(200, 255, 255, 255)
            }
            canvas.drawPath(path, stroke)
        }
        canvas.restore()
        return out
    }

    /** 阴影：先画模糊形状，再把形状内部挖掉，避免半透明内容被压暗。 */
    private fun drawShadow(canvas: Canvas, path: Path, strength: Int, margin: Float) {
        val blur = (margin * 0.55f).coerceAtLeast(1.2f)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((strength * 2.5f).toInt().coerceIn(12, 255), 0, 0, 0)
            maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, shadowPaint)

        val cut = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
        canvas.drawPath(path, cut)
    }

    private fun drawContent(
        canvas: Canvas,
        source: Bitmap,
        area: RectF,
        rotation: Int,
        paint: Paint,
        fit: Boolean
    ) {
        if (rotation == 0) {
            if (fit) {
                val scale = min(area.width() / source.width, area.height() / source.height)
                val dw = source.width * scale
                val dh = source.height * scale
                val left = area.left + (area.width() - dw) / 2f
                val top = area.top + (area.height() - dh) / 2f
                canvas.drawBitmap(source, null, RectF(left, top, left + dw, top + dh), paint)
            } else {
                val scale = max(area.width() / source.width, area.height() / source.height)
                val sw = (area.width() / scale).toInt().coerceIn(1, source.width)
                val sh = (area.height() / scale).toInt().coerceIn(1, source.height)
                val sx = (source.width - sw) / 2
                val sy = (source.height - sh) / 2
                canvas.drawBitmap(source, Rect(sx, sy, sx + sw, sy + sh), area, paint)
            }
            return
        }

        // 旋转后按包围盒等比缩放，保证画面不被裁切
        val radians = Math.toRadians(rotation.toDouble())
        val cosValue = kotlin.math.abs(kotlin.math.cos(radians)).toFloat()
        val sinValue = kotlin.math.abs(kotlin.math.sin(radians)).toFloat()
        val rotatedWidth = source.width * cosValue + source.height * sinValue
        val rotatedHeight = source.width * sinValue + source.height * cosValue
        val scale = min(area.width() / rotatedWidth, area.height() / rotatedHeight)
        val drawWidth = source.width * scale
        val drawHeight = source.height * scale
        val centerX = area.centerX()
        val centerY = area.centerY()
        canvas.save()
        canvas.rotate(rotation.toFloat(), centerX, centerY)
        canvas.drawBitmap(
            source,
            null,
            RectF(
                centerX - drawWidth / 2f,
                centerY - drawHeight / 2f,
                centerX + drawWidth / 2f,
                centerY + drawHeight / 2f
            ),
            paint
        )
        canvas.restore()
    }

    /** 布局内容区：蒙版就长在这个框里，永远填满它。 */
    fun contentRect(source: Bitmap, width: Int, height: Int, layout: LayoutMode): RectF {
        if (layout == LayoutMode.FIT) return autoContentRect(source, width, height)
        return contentArea(width, height, layout)
    }

    private fun autoContentRect(source: Bitmap, width: Int, height: Int): RectF {
        val srcRatio = source.width.toFloat() / source.height.coerceAtLeast(1)
        val areaRatio = width.toFloat() / height.coerceAtLeast(1)
        val contentWidth: Float
        val contentHeight: Float
        if (srcRatio > areaRatio) {
            contentWidth = width.toFloat()
            contentHeight = width / srcRatio
        } else {
            contentHeight = height.toFloat()
            contentWidth = height * srcRatio
        }
        val left = (width - contentWidth) / 2f
        val top = (height - contentHeight) / 2f
        return RectF(left, top, left + contentWidth, top + contentHeight)
    }

    fun contentArea(width: Int, height: Int, layout: LayoutMode): RectF {
        if (layout.ratio <= 0f) {
            return RectF(0f, 0f, width.toFloat(), height.toFloat())
        }
        val widgetRatio = width.toFloat() / height.coerceAtLeast(1)
        return if (widgetRatio > layout.ratio) {
            val contentWidth = height * layout.ratio
            val left = (width - contentWidth) / 2f
            RectF(left, 0f, left + contentWidth, height.toFloat())
        } else {
            val contentHeight = width / layout.ratio
            val top = (height - contentHeight) / 2f
            RectF(0f, top, width.toFloat(), top + contentHeight)
        }
    }
}
