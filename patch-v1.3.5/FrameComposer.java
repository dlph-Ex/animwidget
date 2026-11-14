package io.github.animwidget;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Lambda;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.ranges.RangesKt;
import org.jetbrains.annotations.NotNull;

/* compiled from: FrameComposer.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��^\n\u0002\u0018\u0002\n\u0002\u0010��\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\n\n\u0002\u0010\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\bÆ\u0002\u0018��2\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J(\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\t2\u0006\u0010\u000b\u001a\u00020\t2\u0006\u0010\f\u001a\u00020\tH\u0002J\u008e\u0001\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u000e2\u0006\u0010\u000b\u001a\u00020\t2\u0006\u0010\f\u001a\u00020\t2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\t2\u0006\u0010\u0015\u001a\u00020\u00162\b\b\u0002\u0010\u0017\u001a\u00020\t2\b\b\u0002\u0010\u0018\u001a\u00020\t2\b\b\u0002\u0010\u0019\u001a\u00020\t2\b\b\u0002\u0010\u001a\u001a\u00020\u00162\b\b\u0002\u0010\u001b\u001a\u00020\t2\b\b\u0002\u0010\u001c\u001a\u00020\u00042\b\b\u0002\u0010\u001d\u001a\u00020\u00042\b\b\u0002\u0010\u001e\u001a\u00020\tJ.\u0010\u001f\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\t2\u0006\u0010\u000b\u001a\u00020\t2\u0006\u0010\f\u001a\u00020\t2\u0006\u0010\u0010\u001a\u00020\u0011Jj\u0010 \u001a\u00020!2\u0006\u0010\"\u001a\u00020#2\u0006\u0010$\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\t2\u0006\u0010\u0018\u001a\u00020\t2\u0006\u0010%\u001a\u00020\u00162\u0006\u0010\u001c\u001a\u00020\u00042\u0006\u0010\u001d\u001a\u00020\u00042\u0006\u0010&\u001a\u00020\u00042\u0018\u0010'\u001a\u0014\u0012\u0004\u0012\u00020#\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020!0(H\u0002J°\u0001\u0010)\u001a\u00020!2\u0006\u0010\"\u001a\u00020#2\u0006\u0010\u000b\u001a\u00020\t2\u0006\u0010\f\u001a\u00020\t2\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\t2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\t2\u0006\u0010\u0018\u001a\u00020\t2\u0006\u0010\u0019\u001a\u00020\t2\u0006\u0010\u001a\u001a\u00020\u00162\u0006\u0010\u001b\u001a\u00020\t2\b\b\u0002\u0010\u0014\u001a\u00020\t2\b\b\u0002\u0010\u001c\u001a\u00020\u00042\b\b\u0002\u0010\u001d\u001a\u00020\u00042\b\b\u0002\u0010\u001e\u001a\u00020\t2\u0018\u0010'\u001a\u0014\u0012\u0004\u0012\u00020#\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020!0(J0\u0010*\u001a\u00020!2\u0006\u0010\"\u001a\u00020#2\u0006\u0010+\u001a\u00020,2\u0006\u0010-\u001a\u00020\t2\u0006\u0010.\u001a\u00020\u00042\u0006\u0010/\u001a\u00020\tH\u0002J\u001a\u00100\u001a\u0004\u0018\u00010,2\u0006\u0010+\u001a\u00020,2\u0006\u00101\u001a\u00020\u0004H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T¢\u0006\u0002\n��R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T¢\u0006\u0002\n��"}, d2 = {"Lio/github/animwidget/FrameComposer;", "", "()V", "CONTENT_BLEED", "", "SHADOW_CUT_INSET", "autoContentRect", "Landroid/graphics/RectF;", "sourceWidth", "", "sourceHeight", "width", "height", "compose", "Landroid/graphics/Bitmap;", "source", "layout", "Lio/github/animwidget/LayoutMode;", "shape", "Lio/github/animwidget/ShapeKind;", "alphaPercent", "border", "", "scalePercent", "rotation", "shapeRotation", "fitContent", "shadowPercent", "offsetXPercent", "offsetYPercent", "contentScalePercent", "contentRect", "drawContent", "", "canvas", "Landroid/graphics/Canvas;", "area", "fit", "contentScale", "drawSource", "Lkotlin/Function2;", "drawFrame", "drawShadow", "path", "Landroid/graphics/Path;", "strength", "margin", "alpha", "insetPath", "inset"})
@SourceDebugExtension({"SMAP\nFrameComposer.kt\nKotlin\n*S Kotlin\n*F\n+ 1 FrameComposer.kt\nio/github/animwidget/FrameComposer\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,354:1\n1#2:355\n*E\n"})
/* loaded from: classes.jar:io/github/animwidget/FrameComposer.class */
public final class FrameComposer {

    @NotNull
    public static final FrameComposer INSTANCE = new FrameComposer();
    private static final float SHADOW_CUT_INSET = 2.0f;
    private static final float CONTENT_BLEED = 2.0f;

    /* compiled from: FrameComposer.kt */
    @Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    /* loaded from: classes.jar:io/github/animwidget/FrameComposer$WhenMappings.class */
    public static /* synthetic */ class WhenMappings {
        public static final /* synthetic */ int[] $EnumSwitchMapping$0;

        static {
            int[] iArr = new int[LayoutMode.values().length];
            try {
                iArr[LayoutMode.FIT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[LayoutMode.FILL.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            $EnumSwitchMapping$0 = iArr;
        }
    }

    private FrameComposer() {
    }

    public static /* synthetic */ Bitmap compose$default(FrameComposer frameComposer, Bitmap bitmap, int i, int i2, LayoutMode layoutMode, ShapeKind shapeKind, int i3, boolean z, int i4, int i5, int i6, boolean z2, int i7, float f, float f2, int i8, int i9, Object obj) {
        if ((i9 & 128) != 0) {
            i4 = 100;
        }
        if ((i9 & 256) != 0) {
            i5 = 0;
        }
        if ((i9 & 512) != 0) {
            i6 = 0;
        }
        if ((i9 & 1024) != 0) {
            z2 = true;
        }
        if ((i9 & 2048) != 0) {
            i7 = 0;
        }
        if ((i9 & 4096) != 0) {
            f = 0.0f;
        }
        if ((i9 & 8192) != 0) {
            f2 = 0.0f;
        }
        if ((i9 & 16384) != 0) {
            i8 = 100;
        }
        return frameComposer.compose(bitmap, i, i2, layoutMode, shapeKind, i3, z, i4, i5, i6, z2, i7, f, f2, i8);
    }

    @NotNull
    public final Bitmap compose(@NotNull Bitmap source, int width, int height, @NotNull LayoutMode layout, @NotNull ShapeKind shape, int alphaPercent, boolean border, int scalePercent, int rotation, int shapeRotation, boolean fitContent, int shadowPercent, float offsetXPercent, float offsetYPercent, int contentScalePercent) {
        Intrinsics.checkNotNullParameter(source, "source");
        Intrinsics.checkNotNullParameter(layout, "layout");
        Intrinsics.checkNotNullParameter(shape, "shape");
        int safeWidth = RangesKt.coerceIn(width, 1, 4096);
        int safeHeight = RangesKt.coerceIn(height, 1, 4096);
        Bitmap out = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888);
        Intrinsics.checkNotNullExpressionValue(out, "createBitmap(...)");
        Paint $this$compose_u24lambda_u240 = new Paint(3);
        $this$compose_u24lambda_u240.setAlpha(RangesKt.coerceIn((int) ((255.0f * RangesKt.coerceIn(alphaPercent, 0, 100)) / 100.0f), 0, 255));
        drawFrame(new Canvas(out), safeWidth, safeHeight, source.getWidth(), source.getHeight(), layout, shape, border, scalePercent, rotation, shapeRotation, fitContent, shadowPercent, alphaPercent, offsetXPercent, offsetYPercent, contentScalePercent, new AnonymousClass1(source, $this$compose_u24lambda_u240));
        return out;
    }

    /* compiled from: FrameComposer.kt */
    @Metadata(mv = {1, 9, 0}, k = 3, xi = 48, d1 = {"��\u0014\n��\n\u0002\u0010\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\u0010��\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\n¢\u0006\u0002\b\u0006"}, d2 = {"<anonymous>", "", "canvas", "Landroid/graphics/Canvas;", "dest", "Landroid/graphics/RectF;", "invoke"})
    /* renamed from: io.github.animwidget.FrameComposer$compose$1, reason: invalid class name */
    /* loaded from: classes.jar:io/github/animwidget/FrameComposer$compose$1.class */
    static final class AnonymousClass1 implements Function2<Canvas, RectF, Unit> {
        final /* synthetic */ Bitmap $source;
        final /* synthetic */ Paint $paint;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        AnonymousClass1(Bitmap $source, Paint $paint) {
            this.$source = $source;
            this.$paint = $paint;
        }

        public final Unit invoke(@NotNull Canvas canvas, @NotNull RectF dest) {
            Intrinsics.checkNotNullParameter(canvas, "canvas");
            Intrinsics.checkNotNullParameter(dest, "dest");
            canvas.drawBitmap(this.$source, (Rect) null, dest, this.$paint);
            return Unit.INSTANCE;
        }
    }

    public static /* synthetic */ void drawFrame$default(FrameComposer frameComposer, Canvas canvas, int i, int i2, int i3, int i4, LayoutMode layoutMode, ShapeKind shapeKind, boolean z, int i5, int i6, int i7, boolean z2, int i8, int i9, float f, float f2, int i10, Function2 function2, int i11, Object obj) {
        if ((i11 & 8192) != 0) {
            i9 = 100;
        }
        if ((i11 & 16384) != 0) {
            f = 0.0f;
        }
        if ((i11 & 32768) != 0) {
            f2 = 0.0f;
        }
        if ((i11 & 65536) != 0) {
            i10 = 100;
        }
        frameComposer.drawFrame(canvas, i, i2, i3, i4, layoutMode, shapeKind, z, i5, i6, i7, z2, i8, i9, f, f2, i10, function2);
    }

    public final void drawFrame(@NotNull Canvas canvas, int width, int height, int sourceWidth, int sourceHeight, @NotNull LayoutMode layout, @NotNull ShapeKind shape, boolean border, int scalePercent, int rotation, int shapeRotation, boolean fitContent, int shadowPercent, int alphaPercent, float offsetXPercent, float offsetYPercent, int contentScalePercent, @NotNull Function2<? super Canvas, ? super RectF, Unit> function2) {
        float fCoerceAtLeast;
        boolean z;
        Intrinsics.checkNotNullParameter(canvas, "canvas");
        Intrinsics.checkNotNullParameter(layout, "layout");
        Intrinsics.checkNotNullParameter(shape, "shape");
        Intrinsics.checkNotNullParameter(function2, "drawSource");
        int alpha = RangesKt.coerceIn(alphaPercent, 0, 100);
        int safeWidth = RangesKt.coerceIn(width, 1, 4096);
        int safeHeight = RangesKt.coerceIn(height, 1, 4096);
        float w = safeWidth;
        float h = safeHeight;
        int srcWidth = RangesKt.coerceAtLeast(sourceWidth, 1);
        int srcHeight = RangesKt.coerceAtLeast(sourceHeight, 1);
        float scale = RangesKt.coerceIn(scalePercent, 0, 100) / 100.0f;
        if (scale <= 0.001f) {
            return;
        }
        canvas.save();
        if (scale < 0.999f) {
            canvas.scale(scale, scale, w / 2.0f, h / 2.0f);
        }
        RectF area = contentRect(srcWidth, srcHeight, safeWidth, safeHeight, layout);
        // 兜底：极端/未知比例（source 只有 1px、或布局被算成 0 宽）时，退回整块画布，
        // 否则 clipRect + clipPath 会把内容全裁掉，表现为「切原始比例后素材消失」。
        if (area.width() < 2.0f || area.height() < 2.0f || area.isEmpty()) {
            area = new RectF(0.0f, 0.0f, w, h);
        }
        canvas.clipRect(area);
        int strength = RangesKt.coerceIn(shadowPercent, 0, 100);
        if (strength > 0) {
            fCoerceAtLeast = RangesKt.coerceAtLeast(((Math.min(safeWidth, safeHeight) * 0.035f) * strength) / 100.0f, 2.0f);
        } else {
            fCoerceAtLeast = 0.0f;
        }
        float margin = fCoerceAtLeast;
        RectF inset = insetPreservingRatio(area, margin);
        boolean hasInset = inset.width() > 8.0f && inset.height() > 8.0f;
        RectF box = hasInset ? inset : area;
        if (box.width() < 2.0f || box.height() < 2.0f || box.isEmpty()) {
            box = new RectF(area);
        }
        Path path = ShapeFactory.INSTANCE.path(shape, box.width(), box.height());
        path.offset(box.left, box.top);
        if (shapeRotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(shapeRotation, w / 2.0f, h / 2.0f);
            path.transform(matrix);
        }
        if (strength > 0 && hasInset) {
            drawShadow(canvas, path, strength, margin, alpha);
        }
        switch (WhenMappings.$EnumSwitchMapping$0[layout.ordinal()]) {
            case 1:
                z = true;
                break;
            case 2:
                z = false;
                break;
            default:
                z = fitContent;
                break;
        }
        boolean fit = z;
        canvas.save();
        canvas.clipPath(path);
        RectF drawBox = new RectF(box);
        drawBox.inset(-2.0f, -2.0f);
        drawContent(canvas, drawBox, srcWidth, srcHeight, rotation, fit, RangesKt.coerceIn(offsetXPercent, -50.0f, 50.0f), RangesKt.coerceIn(offsetYPercent, -50.0f, 50.0f), RangesKt.coerceIn(contentScalePercent, 0, 300) / 100.0f, function2);
        canvas.restore();
        if (border) {
            Paint $this$drawFrame_u24lambda_u241 = new Paint(1);
            $this$drawFrame_u24lambda_u241.setStyle(Paint.Style.STROKE);
            $this$drawFrame_u24lambda_u241.setStrokeWidth(RangesKt.coerceAtLeast(Math.min(safeWidth, safeHeight) * 0.018f, 2.0f));
            $this$drawFrame_u24lambda_u241.setColor(Color.argb((200 * alpha) / 100, 255, 255, 255));
            canvas.drawPath(path, $this$drawFrame_u24lambda_u241);
        }
        canvas.restore();
    }


    /** 按 area 的宽高比向内收缩 margin，避免蒙版被非等比内缩拉成另一个比例（阴影 + 原始比例时会露边）。 */
    private static RectF insetPreservingRatio(@NotNull RectF area, float margin) {
        if (margin <= 0.01f) {
            return new RectF(area);
        }
        float aw = area.width();
        float ah = area.height();
        if (aw <= 0.0f || ah <= 0.0f) {
            return new RectF(area);
        }
        float maxW = aw - (margin * 2.0f);
        float maxH = ah - (margin * 2.0f);
        if (maxW <= 1.0f || maxH <= 1.0f) {
            return new RectF(area);
        }
        float ratio = aw / ah;
        float w = maxW;
        float h = maxW / ratio;
        if (h > maxH) {
            h = maxH;
            w = maxH * ratio;
        }
        float cx = area.centerX();
        float cy = area.centerY();
        return new RectF(cx - (w / 2.0f), cy - (h / 2.0f), cx + (w / 2.0f), cy + (h / 2.0f));
    }

    private final void drawShadow(Canvas canvas, Path path, int strength, float margin, int alpha) {
        float blur = RangesKt.coerceAtLeast(margin * 0.7f, 1.2f);
        Paint $this$drawShadow_u24lambda_u242 = new Paint(1);
        int shadowAlpha = RangesKt.coerceIn((int) (((strength * 1.9f) * alpha) / 100.0f), 0, 210);
        $this$drawShadow_u24lambda_u242.setColor(Color.argb(shadowAlpha, 0, 0, 0));
        $this$drawShadow_u24lambda_u242.setMaskFilter(new BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL));
        canvas.drawPath(path, $this$drawShadow_u24lambda_u242);
        Paint $this$drawShadow_u24lambda_u243 = new Paint(1);
        $this$drawShadow_u24lambda_u243.setStyle(Paint.Style.FILL);
        $this$drawShadow_u24lambda_u243.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        Path cutPath = insetPath(path, 2.0f);
        if (cutPath != null) {
            canvas.drawPath(cutPath, $this$drawShadow_u24lambda_u243);
        } else {
            canvas.drawPath(path, $this$drawShadow_u24lambda_u243);
        }
    }

    private final Path insetPath(Path path, float inset) {
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);
        float width = bounds.width();
        float height = bounds.height();
        if (width <= inset * 2.5f || height <= inset * 2.5f) {
            return null;
        }
        float sx = (width - (inset * 2.0f)) / width;
        float sy = (height - (inset * 2.0f)) / height;
        Matrix matrix = new Matrix();
        matrix.setScale(sx, sy);
        matrix.postTranslate(bounds.centerX() * (1.0f - sx), bounds.centerY() * (1.0f - sy));
        Path $this$insetPath_u24lambda_u244 = new Path(path);
        $this$insetPath_u24lambda_u244.transform(matrix);
        return $this$insetPath_u24lambda_u244;
    }

    private final void drawContent(Canvas canvas, RectF area, int sourceWidth, int sourceHeight, int rotation, boolean fit, float offsetXPercent, float offsetYPercent, float contentScale, Function2<? super Canvas, ? super RectF, Unit> function2) {
        float dx = (area.width() * offsetXPercent) / 100.0f;
        float dy = (area.height() * offsetYPercent) / 100.0f;
        if (rotation == 0) {
            if (fit) {
                float scale = Math.min(area.width() / sourceWidth, area.height() / sourceHeight) * contentScale;
                float dw = sourceWidth * scale;
                float dh = sourceHeight * scale;
                float left = (area.centerX() + dx) - (dw / 2.0f);
                float top = (area.centerY() + dy) - (dh / 2.0f);
                function2.invoke(canvas, new RectF(left, top, left + dw, top + dh));
                return;
            }
            float scale2 = Math.max(area.width() / sourceWidth, area.height() / sourceHeight) * contentScale;
            float dw2 = sourceWidth * scale2;
            float dh2 = sourceHeight * scale2;
            float left2 = (area.centerX() - (dw2 / 2.0f)) + dx;
            float top2 = (area.centerY() - (dh2 / 2.0f)) + dy;
            canvas.save();
            canvas.clipRect(area);
            function2.invoke(canvas, new RectF(left2, top2, left2 + dw2, top2 + dh2));
            canvas.restore();
            return;
        }
        double radians = Math.toRadians(rotation);
        float cosValue = (float) Math.abs(Math.cos(radians));
        float sinValue = (float) Math.abs(Math.sin(radians));
        float rotatedWidth = (sourceWidth * cosValue) + (sourceHeight * sinValue);
        float rotatedHeight = (sourceWidth * sinValue) + (sourceHeight * cosValue);
        float scale3 = Math.max(area.width() / rotatedWidth, area.height() / rotatedHeight);
        float drawWidth = sourceWidth * scale3;
        float drawHeight = sourceHeight * scale3;
        float centerX = area.centerX() + dx;
        float centerY = area.centerY() + dy;
        canvas.save();
        canvas.rotate(rotation, centerX, centerY);
        function2.invoke(canvas, new RectF(centerX - (drawWidth / 2.0f), centerY - (drawHeight / 2.0f), centerX + (drawWidth / 2.0f), centerY + (drawHeight / 2.0f)));
        canvas.restore();
    }

    @NotNull
    public final RectF contentRect(int sourceWidth, int sourceHeight, int width, int height, @NotNull LayoutMode layout) {
        Intrinsics.checkNotNullParameter(layout, "layout");
        if (layout == LayoutMode.FIT) {
            return autoContentRect(sourceWidth, sourceHeight, width, height);
        }
        if (layout.getRatio() <= 0.0f) {
            return new RectF(0.0f, 0.0f, width, height);
        }
        float widgetRatio = width / RangesKt.coerceAtLeast(height, 1);
        if (widgetRatio > layout.getRatio()) {
            float contentWidth = height * layout.getRatio();
            float left = (width - contentWidth) / 2.0f;
            return new RectF(left, 0.0f, left + contentWidth, height);
        }
        float contentHeight = width / layout.getRatio();
        float top = (height - contentHeight) / 2.0f;
        return new RectF(0.0f, top, width, top + contentHeight);
    }

    private final RectF autoContentRect(int sourceWidth, int sourceHeight, int width, int height) {
        float contentHeight;
        float contentWidth;
        float srcRatio = sourceWidth / RangesKt.coerceAtLeast(sourceHeight, 1);
        float areaRatio = width / RangesKt.coerceAtLeast(height, 1);
        if (srcRatio > areaRatio) {
            contentWidth = width;
            contentHeight = width / srcRatio;
        } else {
            contentHeight = height;
            contentWidth = height * srcRatio;
        }
        float left = (width - contentWidth) / 2.0f;
        float top = (height - contentHeight) / 2.0f;
        return new RectF(left, top, left + contentWidth, top + contentHeight);
    }
}
