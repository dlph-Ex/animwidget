package io.github.animwidget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.Intrinsics;
import kotlin.math.MathKt;
import kotlin.ranges.RangesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* compiled from: ContentGestureView.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��t\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0018\u0002\n��\n\u0002\u0010\u000b\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u000e\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0010\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018��2\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0002\u0010\u0004J\u0006\u0010S\u001a\u000207J\b\u0010T\u001a\u000207H\u0014J\u0010\u0010U\u001a\u0002072\u0006\u0010V\u001a\u00020WH\u0014J(\u0010X\u001a\u0002072\u0006\u0010Y\u001a\u00020\u00062\u0006\u0010Z\u001a\u00020\u00062\u0006\u0010[\u001a\u00020\u00062\u0006\u0010\\\u001a\u00020\u0006H\u0014J\u0010\u0010]\u001a\u00020\u000e2\u0006\u0010^\u001a\u00020_H\u0017J\u0018\u0010`\u001a\u0002072\u0006\u0010Y\u001a\u00020\u00062\u0006\u0010Z\u001a\u00020\u0006H\u0002J\u0006\u0010a\u001a\u000207J\u000e\u0010b\u001a\u0002072\u0006\u0010c\u001a\u00020\u0006R\u001a\u0010\u0005\u001a\u00020\u0006X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b\u0007\u0010\b\"\u0004\b\t\u0010\nR\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004¢\u0006\u0002\n��R\u001a\u0010\r\u001a\u00020\u000eX\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b\u000f\u0010\u0010\"\u0004\b\u0011\u0010\u0012R\u000e\u0010\u0013\u001a\u00020\fX\u0082\u0004¢\u0006\u0002\n��R\u001a\u0010\u0014\u001a\u00020\u0006X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b\u0015\u0010\b\"\u0004\b\u0016\u0010\nR\u000e\u0010\u0017\u001a\u00020\u0018X\u0082\u0004¢\u0006\u0002\n��R\u000e\u0010\u0019\u001a\u00020\u000eX\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010\u001a\u001a\u00020\fX\u0082\u0004¢\u0006\u0002\n��R\u000e\u0010\u001b\u001a\u00020\u000eX\u0082\u000e¢\u0006\u0002\n��R\u001a\u0010\u001c\u001a\u00020\u000eX\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b\u001d\u0010\u0010\"\u0004\b\u001e\u0010\u0012R\u000e\u0010\u001f\u001a\u00020 X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010!\u001a\u00020 X\u0082\u000e¢\u0006\u0002\n��R\u001a\u0010\"\u001a\u00020#X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b$\u0010%\"\u0004\b&\u0010'R\u000e\u0010(\u001a\u00020\fX\u0082\u0004¢\u0006\u0002\n��R\u001a\u0010)\u001a\u00020 X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b*\u0010+\"\u0004\b,\u0010-R\u001a\u0010.\u001a\u00020 X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b/\u0010+\"\u0004\b0\u0010-RL\u00101\u001a4\u0012\u0013\u0012\u00110 ¢\u0006\f\b3\u0012\b\b4\u0012\u0004\b\b(5\u0012\u0013\u0012\u00110 ¢\u0006\f\b3\u0012\b\b4\u0012\u0004\b\b(6\u0012\u0004\u0012\u000207\u0018\u000102X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b8\u00109\"\u0004\b:\u0010;R\u0010\u0010<\u001a\u0004\u0018\u00010=X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010>\u001a\u00020\fX\u0082\u0004¢\u0006\u0002\n��R\u001a\u0010?\u001a\u00020\u0006X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b@\u0010\b\"\u0004\bA\u0010\nR\u001a\u0010B\u001a\u00020\u0006X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\bC\u0010\b\"\u0004\bD\u0010\nR\u001a\u0010E\u001a\u00020FX\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\bG\u0010H\"\u0004\bI\u0010JR\u001a\u0010K\u001a\u00020\u0006X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\bL\u0010\b\"\u0004\bM\u0010\nR\u001c\u0010N\u001a\u0004\u0018\u00010=X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\bO\u0010P\"\u0004\bQ\u0010R"}, d2 = {"Lio/github/animwidget/ContentGestureView;", "Landroid/view/View;", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "alphaPercent", "", "getAlphaPercent", "()I", "setAlphaPercent", "(I)V", "bitmapPaint", "Landroid/graphics/Paint;", "border", "", "getBorder", "()Z", "setBorder", "(Z)V", "borderPaint", "contentScalePercent", "getContentScalePercent", "setContentScalePercent", "dest", "Landroid/graphics/RectF;", "dirty", "dotPaint", "dragging", "fitContent", "getFitContent", "setFitContent", "lastX", "", "lastY", "layout", "Lio/github/animwidget/LayoutMode;", "getLayout", "()Lio/github/animwidget/LayoutMode;", "setLayout", "(Lio/github/animwidget/LayoutMode;)V", "linePaint", "offsetXPercent", "getOffsetXPercent", "()F", "setOffsetXPercent", "(F)V", "offsetYPercent", "getOffsetYPercent", "setOffsetYPercent", "onChange", "Lkotlin/Function2;", "Lkotlin/ParameterName;", "name", "offsetX", "offsetY", "", "getOnChange", "()Lkotlin/jvm/functions/Function2;", "setOnChange", "(Lkotlin/jvm/functions/Function2;)V", "preview", "Landroid/graphics/Bitmap;", "ringPaint", "rotation", "getRotation", "setRotation", "shadowPercent", "getShadowPercent", "setShadowPercent", "shape", "Lio/github/animwidget/ShapeKind;", "getShape", "()Lio/github/animwidget/ShapeKind;", "setShape", "(Lio/github/animwidget/ShapeKind;)V", "shapeRotation", "getShapeRotation", "setShapeRotation", "source", "getSource", "()Landroid/graphics/Bitmap;", "setSource", "(Landroid/graphics/Bitmap;)V", "invalidateParams", "onDetachedFromWindow", "onDraw", "canvas", "Landroid/graphics/Canvas;", "onSizeChanged", "w", "h", "oldw", "oldh", "onTouchEvent", "event", "Landroid/view/MotionEvent;", "rebuild", "resetParams", "setContentScale", "value"})
/* loaded from: classes.jar:io/github/animwidget/ContentGestureView.class */
public final class ContentGestureView extends View {

    @Nullable
    Bitmap source;

    @NotNull
    LayoutMode layout;

    @NotNull
    ShapeKind shape;
    int shapeRotation;
    boolean fitContent;
    int alphaPercent;
    boolean border;
    int shadowPercent;
    /** v1.10.221：阴影曲线（空串 = 默认曲线）。由调用方 push（和 shadowPercent 同路）。 */
    String shadowCurve = "";
    int contentScalePercent;
    int rotation;
    float offsetXPercent;
    float offsetYPercent;
    float maskOffsetXPercent;
    float maskOffsetYPercent;

    /** true：拖动蒙版；false：拖动蒙版里的内容。 */
    boolean dragMask;

    @Nullable
    Function2<? super Float, ? super Float, Unit> onChange;

    @Nullable
    Bitmap preview;
    boolean dirty;

    @NotNull
    final Paint bitmapPaint;

    @NotNull
    final RectF dest;

    @NotNull
    final Paint linePaint;

    @NotNull
    final Paint ringPaint;

    @NotNull
    final Paint dotPaint;

    @NotNull
    final Paint borderPaint;
    float lastX;
    float lastY;
    boolean dragging;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public ContentGestureView(@NotNull Context context) {
        super(context);
        Intrinsics.checkNotNullParameter(context, "context");
        this.layout = LayoutMode.FILL;
        this.shape = ShapeKind.ROUNDED;
        this.fitContent = true;
        this.alphaPercent = 100;
        this.contentScalePercent = 100;
        this.dirty = true;
        this.bitmapPaint = new Paint(3);
        this.dest = new RectF();
        Paint $this$linePaint_u24lambda_u240 = new Paint(1);
        $this$linePaint_u24lambda_u240.setColor(-855638017);
        $this$linePaint_u24lambda_u240.setStyle(Paint.Style.STROKE);
        this.linePaint = $this$linePaint_u24lambda_u240;
        Paint $this$ringPaint_u24lambda_u241 = new Paint(1);
        $this$ringPaint_u24lambda_u241.setColor(-1);
        $this$ringPaint_u24lambda_u241.setStyle(Paint.Style.STROKE);
        this.ringPaint = $this$ringPaint_u24lambda_u241;
        Paint $this$dotPaint_u24lambda_u242 = new Paint(1);
        $this$dotPaint_u24lambda_u242.setColor(-1);
        $this$dotPaint_u24lambda_u242.setStyle(Paint.Style.FILL);
        this.dotPaint = $this$dotPaint_u24lambda_u242;
        Paint $this$borderPaint_u24lambda_u243 = new Paint(1);
        $this$borderPaint_u24lambda_u243.setColor(872415231);
        $this$borderPaint_u24lambda_u243.setStyle(Paint.Style.STROKE);
        this.borderPaint = $this$borderPaint_u24lambda_u243;
    }

    @Nullable
    public final Bitmap getSource() {
        return this.source;
    }

    public final void setSource(@Nullable Bitmap bitmap) {
        this.source = bitmap;
    }

    @NotNull
    public final LayoutMode getLayout() {
        return this.layout;
    }

    public final void setLayout(@NotNull LayoutMode layoutMode) {
        Intrinsics.checkNotNullParameter(layoutMode, "<set-?>");
        this.layout = layoutMode;
    }

    @NotNull
    public final ShapeKind getShape() {
        return this.shape;
    }

    public final void setShape(@NotNull ShapeKind shapeKind) {
        Intrinsics.checkNotNullParameter(shapeKind, "<set-?>");
        this.shape = shapeKind;
    }

    public final int getShapeRotation() {
        return this.shapeRotation;
    }

    public final void setShapeRotation(int i) {
        this.shapeRotation = i;
    }

    public final boolean getFitContent() {
        return this.fitContent;
    }

    public final void setFitContent(boolean z) {
        this.fitContent = z;
    }

    public final int getAlphaPercent() {
        return this.alphaPercent;
    }

    public final void setAlphaPercent(int i) {
        this.alphaPercent = i;
    }

    public final boolean getBorder() {
        return this.border;
    }

    public final void setBorder(boolean z) {
        this.border = z;
    }

    public final int getShadowPercent() {
        return this.shadowPercent;
    }

    public final void setShadowPercent(int i) {
        this.shadowPercent = i;
    }

    public final void setShadowCurve(String s) {
        this.shadowCurve = s == null ? "" : s;
    }

    public final int getContentScalePercent() {
        return this.contentScalePercent;
    }

    public final void setContentScalePercent(int i) {
        this.contentScalePercent = i;
    }

    public final int getMaskRotation() {
        return this.rotation;
    }

    public final void setMaskRotation(int i) {
        this.rotation = i;
    }

    public final float getOffsetXPercent() {
        return this.offsetXPercent;
    }

    public final void setOffsetXPercent(float f) {
        this.offsetXPercent = f;
    }

    public final float getOffsetYPercent() {
        return this.offsetYPercent;
    }

    public final void setOffsetYPercent(float f) {
        this.offsetYPercent = f;
    }

    public final float getMaskOffsetXPercent() {
        return this.maskOffsetXPercent;
    }

    public final void setMaskOffsetXPercent(float f) {
        this.maskOffsetXPercent = f;
    }

    public final float getMaskOffsetYPercent() {
        return this.maskOffsetYPercent;
    }

    public final void setMaskOffsetYPercent(float f) {
        this.maskOffsetYPercent = f;
    }

    public final boolean getDragMask() {
        return this.dragMask;
    }

    public final void setDragMask(boolean z) {
        this.dragMask = z;
    }

    /** 当前被拖动的那一对偏移（十字准星的位置）。 */
    public final float getActiveXPercent() {
        return this.dragMask ? this.maskOffsetXPercent : this.offsetXPercent;
    }

    public final float getActiveYPercent() {
        return this.dragMask ? this.maskOffsetYPercent : this.offsetYPercent;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public final Function2<Float, Float, Unit> getOnChange() {
        return (Function2<Float, Float, Unit>) this.onChange;
    }

    public final void setOnChange(@Nullable Function2<? super Float, ? super Float, Unit> function2) {
        this.onChange = function2;
    }

    public final void setContentScale(int value) {
        int v = RangesKt.coerceIn(value, 0, 200);   // v1.10.73：滑条改成 -100~+100 → 内部 0~200
        if (v == this.contentScalePercent) {
            return;
        }
        this.contentScalePercent = v;
        this.dirty = true;
        invalidate();
    }

    public final void invalidateParams() {
        this.dirty = true;
        invalidate();
    }

    public final void resetParams() {
        if (this.dragMask) {
            this.maskOffsetXPercent = 0.0f;
            this.maskOffsetYPercent = 0.0f;
        } else {
            this.contentScalePercent = 100;
            this.offsetXPercent = 0.0f;
            this.offsetYPercent = 0.0f;
        }
        this.dirty = true;
        invalidate();
        Function2<? super Float, ? super Float, Unit> function2 = this.onChange;
        if (function2 != null) {
            function2.invoke(Float.valueOf(getActiveXPercent()), Float.valueOf(getActiveYPercent()));
        }
    }

    @Override // android.view.View
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.dirty = true;
    }

    @Override // android.view.View
    protected void onDetachedFromWindow() {
        Bitmap bitmap = this.preview;
        if (bitmap != null) {
            bitmap.recycle();
        }
        this.preview = null;
        super.onDetachedFromWindow();
    }

    @Override // android.view.View
    protected void onDraw(@NotNull Canvas canvas) {
        Intrinsics.checkNotNullParameter(canvas, "canvas");
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        if (this.dirty) {
            rebuild(w, h);
            this.dirty = false;
        }
        Bitmap bmp = this.preview;
        if (bmp != null) {
            this.dest.set(0.0f, 0.0f, w, h);
            canvas.drawBitmap(bmp, (Rect) null, this.dest, this.bitmapPaint);
        }
        float density = getResources().getDisplayMetrics().density;
        float margin = 13.0f * density;
        float limitX = RangesKt.coerceAtLeast(w - margin, margin);
        float limitY = RangesKt.coerceAtLeast(h - margin, margin);
        float cx = RangesKt.coerceIn((w / 2.0f) + ((getActiveXPercent() / 100.0f) * w), margin, limitX);
        float cy = RangesKt.coerceIn((h / 2.0f) + ((getActiveYPercent() / 100.0f) * h), margin, limitY);
        this.borderPaint.setStrokeWidth(1.2f * density);
        canvas.drawRoundRect(1.0f, 1.0f, w - 1.0f, h - 1.0f, 14.0f * density, 14.0f * density, this.borderPaint);
        this.linePaint.setStrokeWidth(1.2f * density);
        this.linePaint.setPathEffect(new DashPathEffect(new float[]{4.0f * density, 4.0f * density}, 0.0f));
        canvas.drawLine(cx, 0.0f, cx, h, this.linePaint);
        canvas.drawLine(0.0f, cy, w, cy, this.linePaint);
        this.linePaint.setPathEffect(null);
        this.ringPaint.setStrokeWidth(2.2f * density);
        canvas.drawCircle(cx, cy, 11.0f * density, this.ringPaint);
        canvas.drawCircle(cx, cy, 3.0f * density, this.dotPaint);
    }

    private final void rebuild(int w, int h) {
        Bitmap bitmapCompose;
        Bitmap src = this.source;
        if (src == null || src.isRecycled()) {
            Bitmap bitmap = this.preview;
            if (bitmap != null) {
                bitmap.recycle();
            }
            this.preview = null;
            return;
        }
        float factor = Math.min(1.0f, 720.0f / Math.max(w, h));
        int cw = RangesKt.coerceAtLeast(MathKt.roundToInt(w * factor), 1);
        int ch = RangesKt.coerceAtLeast(MathKt.roundToInt(h * factor), 1);
        try {
            bitmapCompose = FrameComposer.INSTANCE.compose(src, cw, ch, this.layout, this.shape, this.alphaPercent, this.border, 100, this.rotation, this.shapeRotation, this.fitContent, this.shadowPercent, this.shadowCurve, this.offsetXPercent, this.offsetYPercent, this.contentScalePercent, this.maskOffsetXPercent, this.maskOffsetYPercent);
        } catch (Throwable th) {
            bitmapCompose = null;
        }
        Bitmap out = bitmapCompose;
        Bitmap bitmap2 = this.preview;
        if (bitmap2 != null) {
            bitmap2.recycle();
        }
        this.preview = out;
    }

    @Override // android.view.View
    @SuppressLint({"ClickableViewAccessibility"})
    public boolean onTouchEvent(@NotNull MotionEvent event) {
        float fCoerceIn;
        float fCoerceIn2;
        Intrinsics.checkNotNullParameter(event, "event");
        switch (event.getActionMasked()) {
            case 0:
                this.lastX = event.getX();
                this.lastY = event.getY();
                this.dragging = true;
                ViewParent parent = getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                    break;
                }
                break;
            case 1:
            case 3:
                this.dragging = false;
                ViewParent parent2 = getParent();
                if (parent2 != null) {
                    parent2.requestDisallowInterceptTouchEvent(false);
                    break;
                }
                break;
            case 2:
                if (this.dragging) {
                    float dx = ((event.getX() - this.lastX) / getWidth()) * 100.0f;
                    float dy = ((event.getY() - this.lastY) / getHeight()) * 100.0f;
                    this.lastX = event.getX();
                    this.lastY = event.getY();
                    float margin = 13.0f * getResources().getDisplayMetrics().density;
                    float minX = ((margin - (getWidth() / 2.0f)) / getWidth()) * 100.0f;
                    float maxX = (((getWidth() - margin) - (getWidth() / 2.0f)) / getWidth()) * 100.0f;
                    float minY = ((margin - (getHeight() / 2.0f)) / getHeight()) * 100.0f;
                    float maxY = (((getHeight() - margin) - (getHeight() / 2.0f)) / getHeight()) * 100.0f;
                    float curX = getActiveXPercent();
                    float curY = getActiveYPercent();
                    if (minX <= maxX) {
                        fCoerceIn = RangesKt.coerceIn(curX + dx, minX, maxX);
                    } else {
                        fCoerceIn = 0.0f;
                    }
                    float nx = fCoerceIn;
                    if (minY <= maxY) {
                        fCoerceIn2 = RangesKt.coerceIn(curY + dy, minY, maxY);
                    } else {
                        fCoerceIn2 = 0.0f;
                    }
                    float ny = fCoerceIn2;
                    if (this.dragMask) {
                        this.maskOffsetXPercent = nx;
                        this.maskOffsetYPercent = ny;
                    } else {
                        this.offsetXPercent = nx;
                        this.offsetYPercent = ny;
                    }
                    this.dirty = true;
                    invalidate();
                    Function2<? super Float, ? super Float, Unit> function2 = this.onChange;
                    if (function2 != null) {
                        function2.invoke(Float.valueOf(nx), Float.valueOf(ny));
                        break;
                    }
                }
                break;
        }
        return true;
    }
}
