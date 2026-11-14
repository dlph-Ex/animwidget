package io.github.animwidget;

import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* compiled from: WidgetConfig.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��<\n\u0002\u0018\u0002\n\u0002\u0010��\n��\n\u0002\u0010\b\n��\n\u0002\u0018\u0002\n��\n\u0002\u0010\u000e\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n��\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0010\u0007\n\u0002\bE\b\u0086\b\u0018��2\u00020\u0001B\u0091\u0002\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u0012\b\b\u0002\u0010\b\u001a\u00020\u0007\u0012\b\b\u0002\u0010\t\u001a\u00020\u0003\u0012\b\b\u0002\u0010\n\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u000b\u001a\u00020\u0003\u0012\b\b\u0002\u0010\f\u001a\u00020\u0003\u0012\b\b\u0002\u0010\r\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u000e\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u000f\u001a\u00020\u0010\u0012\b\b\u0002\u0010\u0011\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0012\u001a\u00020\u0013\u0012\b\b\u0002\u0010\u0014\u001a\u00020\u0015\u0012\b\b\u0002\u0010\u0016\u001a\u00020\u0013\u0012\b\b\u0002\u0010\u0017\u001a\u00020\u0013\u0012\b\b\u0002\u0010\u0018\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0019\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u001a\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u001b\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u001c\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u001d\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u001e\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u001f\u001a\u00020\u0013\u0012\b\b\u0002\u0010 \u001a\u00020!\u0012\b\b\u0002\u0010\"\u001a\u00020!\u0012\b\b\u0002\u0010#\u001a\u00020\u0003¢\u0006\u0002\u0010$J\t\u0010G\u001a\u00020\u0003HÆ\u0003J\t\u0010H\u001a\u00020\u0003HÆ\u0003J\t\u0010I\u001a\u00020\u0010HÆ\u0003J\t\u0010J\u001a\u00020\u0003HÆ\u0003J\t\u0010K\u001a\u00020\u0013HÆ\u0003J\t\u0010L\u001a\u00020\u0015HÆ\u0003J\t\u0010M\u001a\u00020\u0013HÆ\u0003J\t\u0010N\u001a\u00020\u0013HÆ\u0003J\t\u0010O\u001a\u00020\u0003HÆ\u0003J\t\u0010P\u001a\u00020\u0003HÆ\u0003J\t\u0010Q\u001a\u00020\u0003HÆ\u0003J\t\u0010R\u001a\u00020\u0005HÆ\u0003J\t\u0010S\u001a\u00020\u0003HÆ\u0003J\t\u0010T\u001a\u00020\u0003HÆ\u0003J\t\u0010U\u001a\u00020\u0003HÆ\u0003J\t\u0010V\u001a\u00020\u0003HÆ\u0003J\t\u0010W\u001a\u00020\u0013HÆ\u0003J\t\u0010X\u001a\u00020!HÆ\u0003J\t\u0010Y\u001a\u00020!HÆ\u0003J\t\u0010Z\u001a\u00020\u0003HÆ\u0003J\t\u0010[\u001a\u00020\u0007HÆ\u0003J\t\u0010\\\u001a\u00020\u0007HÆ\u0003J\t\u0010]\u001a\u00020\u0003HÆ\u0003J\t\u0010^\u001a\u00020\u0003HÆ\u0003J\t\u0010_\u001a\u00020\u0003HÆ\u0003J\t\u0010`\u001a\u00020\u0003HÆ\u0003J\t\u0010a\u001a\u00020\u0003HÆ\u0003J\u0097\u0002\u0010b\u001a\u00020��2\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\u00072\b\b\u0002\u0010\t\u001a\u00020\u00032\b\b\u0002\u0010\n\u001a\u00020\u00032\b\b\u0002\u0010\u000b\u001a\u00020\u00032\b\b\u0002\u0010\f\u001a\u00020\u00032\b\b\u0002\u0010\r\u001a\u00020\u00032\b\b\u0002\u0010\u000e\u001a\u00020\u00032\b\b\u0002\u0010\u000f\u001a\u00020\u00102\b\b\u0002\u0010\u0011\u001a\u00020\u00032\b\b\u0002\u0010\u0012\u001a\u00020\u00132\b\b\u0002\u0010\u0014\u001a\u00020\u00152\b\b\u0002\u0010\u0016\u001a\u00020\u00132\b\b\u0002\u0010\u0017\u001a\u00020\u00132\b\b\u0002\u0010\u0018\u001a\u00020\u00032\b\b\u0002\u0010\u0019\u001a\u00020\u00032\b\b\u0002\u0010\u001a\u001a\u00020\u00032\b\b\u0002\u0010\u001b\u001a\u00020\u00032\b\b\u0002\u0010\u001c\u001a\u00020\u00032\b\b\u0002\u0010\u001d\u001a\u00020\u00032\b\b\u0002\u0010\u001e\u001a\u00020\u00032\b\b\u0002\u0010\u001f\u001a\u00020\u00132\b\b\u0002\u0010 \u001a\u00020!2\b\b\u0002\u0010\"\u001a\u00020!2\b\b\u0002\u0010#\u001a\u00020\u0003HÆ\u0001J\u0013\u0010c\u001a\u00020\u00132\b\u0010d\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u0010e\u001a\u00020\u0003HÖ\u0001J\t\u0010f\u001a\u00020\u0007HÖ\u0001R\u0011\u0010\u0012\u001a\u00020\u0013¢\u0006\b\n��\u001a\u0004\b%\u0010&R\u0011\u0010#\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b'\u0010(R\u0011\u0010\u001f\u001a\u00020\u0013¢\u0006\b\n��\u001a\u0004\b)\u0010&R\u0011\u0010\t\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b*\u0010(R\u0011\u0010\f\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b+\u0010(R\u0011\u0010\u000e\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b,\u0010(R\u0011\u0010\r\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b-\u0010(R\u0011\u0010\u0004\u001a\u00020\u0005¢\u0006\b\n��\u001a\u0004\b.\u0010/R\u0011\u0010\u0014\u001a\u00020\u0015¢\u0006\b\n��\u001a\u0004\b0\u00101R\u0011\u0010\n\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b2\u0010(R\u0011\u0010 \u001a\u00020!¢\u0006\b\n��\u001a\u0004\b3\u00104R\u0011\u0010\"\u001a\u00020!¢\u0006\b\n��\u001a\u0004\b5\u00104R\u0011\u0010\u0016\u001a\u00020\u0013¢\u0006\b\n��\u001a\u0004\b6\u0010&R\u0011\u0010\u0017\u001a\u00020\u0013¢\u0006\b\n��\u001a\u0004\b7\u0010&R\u0011\u0010\u001a\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b8\u0010(R\u0011\u0010\u0018\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b9\u0010(R\u0011\u0010\u0019\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b:\u0010(R\u0011\u0010\u000b\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b;\u0010(R\u0011\u0010\u001d\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b<\u0010(R\u0011\u0010\u001b\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b=\u0010(R\u0011\u0010\u001c\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b>\u0010(R\u0011\u0010\u000f\u001a\u00020\u0010¢\u0006\b\n��\u001a\u0004\b?\u0010@R\u0011\u0010\u0011\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\bA\u0010(R\u0011\u0010\u001e\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\bB\u0010(R\u0011\u0010\u0006\u001a\u00020\u0007¢\u0006\b\n��\u001a\u0004\bC\u0010DR\u0011\u0010\b\u001a\u00020\u0007¢\u0006\b\n��\u001a\u0004\bE\u0010DR\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\bF\u0010("}, d2 = {"Lio/github/animwidget/WidgetConfig;", "", "widgetId", "", "kind", "Lio/github/animwidget/MediaKind;", "sourceName", "", "sourceUri", "fps", "maxEdge", "quality", "frameCount", "frameWidth", "frameHeight", "shape", "Lio/github/animwidget/ShapeKind;", "shapeAlpha", "border", "", "layout", "Lio/github/animwidget/LayoutMode;", "overlayEnabled", "overlayVisible", "overlayX", "overlayY", "overlayWidth", "scalePercent", "shadowPercent", "rotation", "shapeRotation", "fitContent", "offsetXPercent", "", "offsetYPercent", "contentScalePercent", "(ILio/github/animwidget/MediaKind;Ljava/lang/String;Ljava/lang/String;IIIIIILio/github/animwidget/ShapeKind;IZLio/github/animwidget/LayoutMode;ZZIIIIIIIZFFI)V", "getBorder", "()Z", "getContentScalePercent", "()I", "getFitContent", "getFps", "getFrameCount", "getFrameHeight", "getFrameWidth", "getKind", "()Lio/github/animwidget/MediaKind;", "getLayout", "()Lio/github/animwidget/LayoutMode;", "getMaxEdge", "getOffsetXPercent", "()F", "getOffsetYPercent", "getOverlayEnabled", "getOverlayVisible", "getOverlayWidth", "getOverlayX", "getOverlayY", "getQuality", "getRotation", "getScalePercent", "getShadowPercent", "getShape", "()Lio/github/animwidget/ShapeKind;", "getShapeAlpha", "getShapeRotation", "getSourceName", "()Ljava/lang/String;", "getSourceUri", "getWidgetId", "component1", "component10", "component11", "component12", "component13", "component14", "component15", "component16", "component17", "component18", "component19", "component2", "component20", "component21", "component22", "component23", "component24", "component25", "component26", "component27", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "other", "hashCode", "toString"})
/* loaded from: classes.jar:io/github/animwidget/WidgetConfig.class */
public final class WidgetConfig {
    final int widgetId;

    @NotNull
    final MediaKind kind;

    @NotNull
    final String sourceName;

    @NotNull
    final String sourceUri;
    final int fps;
    final int maxEdge;
    final int quality;
    final int frameCount;
    final int frameWidth;
    final int frameHeight;

    @NotNull
    final ShapeKind shape;
    final int shapeAlpha;
    final boolean border;

    @NotNull
    final LayoutMode layout;
    final boolean overlayEnabled;
    final boolean overlayVisible;
    final int overlayX;
    final int overlayY;
    final int overlayWidth;
    final int scalePercent;
    final int shadowPercent;
    /** v1.10.221：阴影剖面曲线（{@link ShadowCurve#encode()} 的 6 个 3 位小数）；空串 = 默认曲线。 */
    final String shadowCurve;
    final int rotation;
    final int shapeRotation;
    final boolean fitContent;
    final float offsetXPercent;
    final float offsetYPercent;
    final int contentScalePercent;
    final float maskOffsetXPercent;
    final float maskOffsetYPercent;

    public final int component1() {
        return this.widgetId;
    }

    @NotNull
    public final MediaKind component2() {
        return this.kind;
    }

    @NotNull
    public final String component3() {
        return this.sourceName;
    }

    @NotNull
    public final String component4() {
        return this.sourceUri;
    }

    public final int component5() {
        return this.fps;
    }

    public final int component6() {
        return this.maxEdge;
    }

    public final int component7() {
        return this.quality;
    }

    public final int component8() {
        return this.frameCount;
    }

    public final int component9() {
        return this.frameWidth;
    }

    public final int component10() {
        return this.frameHeight;
    }

    @NotNull
    public final ShapeKind component11() {
        return this.shape;
    }

    public final int component12() {
        return this.shapeAlpha;
    }

    public final boolean component13() {
        return this.border;
    }

    @NotNull
    public final LayoutMode component14() {
        return this.layout;
    }

    public final boolean component15() {
        return this.overlayEnabled;
    }

    public final boolean component16() {
        return this.overlayVisible;
    }

    public final int component17() {
        return this.overlayX;
    }

    public final int component18() {
        return this.overlayY;
    }

    public final int component19() {
        return this.overlayWidth;
    }

    public final int component20() {
        return this.scalePercent;
    }

    public final int component21() {
        return this.shadowPercent;
    }

    public final int component22() {
        return this.rotation;
    }

    public final int component23() {
        return this.shapeRotation;
    }

    public final boolean component24() {
        return this.fitContent;
    }

    public final float component25() {
        return this.offsetXPercent;
    }

    public final float component26() {
        return this.offsetYPercent;
    }

    public final int component27() {
        return this.contentScalePercent;
    }

    public final float component28() {
        return this.maskOffsetXPercent;
    }

    public final float component29() {
        return this.maskOffsetYPercent;
    }

    @NotNull
    public final WidgetConfig copy(int widgetId, @NotNull MediaKind kind, @NotNull String sourceName, @NotNull String sourceUri, int fps, int maxEdge, int quality, int frameCount, int frameWidth, int frameHeight, @NotNull ShapeKind shape, int shapeAlpha, boolean border, @NotNull LayoutMode layout, boolean overlayEnabled, boolean overlayVisible, int overlayX, int overlayY, int overlayWidth, int scalePercent, int shadowPercent, int rotation, int shapeRotation, boolean fitContent, float offsetXPercent, float offsetYPercent, int contentScalePercent, float maskOffsetXPercent, float maskOffsetYPercent, String shadowCurve) {
        Intrinsics.checkNotNullParameter(kind, "kind");
        Intrinsics.checkNotNullParameter(sourceName, "sourceName");
        Intrinsics.checkNotNullParameter(sourceUri, "sourceUri");
        Intrinsics.checkNotNullParameter(shape, "shape");
        Intrinsics.checkNotNullParameter(layout, "layout");
        return new WidgetConfig(widgetId, kind, sourceName, sourceUri, fps, maxEdge, quality, frameCount, frameWidth, frameHeight, shape, shapeAlpha, border, layout, overlayEnabled, overlayVisible, overlayX, overlayY, overlayWidth, scalePercent, shadowPercent, rotation, shapeRotation, fitContent, offsetXPercent, offsetYPercent, contentScalePercent, maskOffsetXPercent, maskOffsetYPercent, shadowCurve);
    }

    public static /* synthetic */ WidgetConfig copy$default(WidgetConfig widgetConfig, int i, MediaKind mediaKind, String str, String str2, int i2, int i3, int i4, int i5, int i6, int i7, ShapeKind shapeKind, int i8, boolean z, LayoutMode layoutMode, boolean z2, boolean z3, int i9, int i10, int i11, int i12, int i13, int i14, int i15, boolean z4, float f, float f2, int i16, float f3, float f4, String str3, int i17, Object obj) {
        if ((i17 & 1) != 0) {
            i = widgetConfig.widgetId;
        }
        if ((i17 & 2) != 0) {
            mediaKind = widgetConfig.kind;
        }
        if ((i17 & 4) != 0) {
            str = widgetConfig.sourceName;
        }
        if ((i17 & 8) != 0) {
            str2 = widgetConfig.sourceUri;
        }
        if ((i17 & 16) != 0) {
            i2 = widgetConfig.fps;
        }
        if ((i17 & 32) != 0) {
            i3 = widgetConfig.maxEdge;
        }
        if ((i17 & 64) != 0) {
            i4 = widgetConfig.quality;
        }
        if ((i17 & 128) != 0) {
            i5 = widgetConfig.frameCount;
        }
        if ((i17 & 256) != 0) {
            i6 = widgetConfig.frameWidth;
        }
        if ((i17 & 512) != 0) {
            i7 = widgetConfig.frameHeight;
        }
        if ((i17 & 1024) != 0) {
            shapeKind = widgetConfig.shape;
        }
        if ((i17 & 2048) != 0) {
            i8 = widgetConfig.shapeAlpha;
        }
        if ((i17 & 4096) != 0) {
            z = widgetConfig.border;
        }
        if ((i17 & 8192) != 0) {
            layoutMode = widgetConfig.layout;
        }
        if ((i17 & 16384) != 0) {
            z2 = widgetConfig.overlayEnabled;
        }
        if ((i17 & 32768) != 0) {
            z3 = widgetConfig.overlayVisible;
        }
        if ((i17 & 65536) != 0) {
            i9 = widgetConfig.overlayX;
        }
        if ((i17 & 131072) != 0) {
            i10 = widgetConfig.overlayY;
        }
        if ((i17 & 262144) != 0) {
            i11 = widgetConfig.overlayWidth;
        }
        if ((i17 & 524288) != 0) {
            i12 = widgetConfig.scalePercent;
        }
        if ((i17 & 1048576) != 0) {
            i13 = widgetConfig.shadowPercent;
        }
        if ((i17 & 2097152) != 0) {
            i14 = widgetConfig.rotation;
        }
        if ((i17 & 4194304) != 0) {
            i15 = widgetConfig.shapeRotation;
        }
        if ((i17 & 8388608) != 0) {
            z4 = widgetConfig.fitContent;
        }
        if ((i17 & 16777216) != 0) {
            f = widgetConfig.offsetXPercent;
        }
        if ((i17 & 33554432) != 0) {
            f2 = widgetConfig.offsetYPercent;
        }
        if ((i17 & 67108864) != 0) {
            i16 = widgetConfig.contentScalePercent;
        }
        if ((i17 & 134217728) != 0) {
            f3 = widgetConfig.maskOffsetXPercent;
        }
        if ((i17 & 268435456) != 0) {
            f4 = widgetConfig.maskOffsetYPercent;
        }
        if ((i17 & 536870912) != 0) {
            str3 = widgetConfig.shadowCurve;
        }
        return widgetConfig.copy(i, mediaKind, str, str2, i2, i3, i4, i5, i6, i7, shapeKind, i8, z, layoutMode, z2, z3, i9, i10, i11, i12, i13, i14, i15, z4, f, f2, i16, f3, f4, str3);
    }

    @NotNull
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WidgetConfig(widgetId=").append(this.widgetId).append(", kind=").append(this.kind).append(", sourceName=").append(this.sourceName).append(", sourceUri=").append(this.sourceUri).append(", fps=").append(this.fps).append(", maxEdge=").append(this.maxEdge).append(", quality=").append(this.quality).append(", frameCount=").append(this.frameCount).append(", frameWidth=").append(this.frameWidth).append(", frameHeight=").append(this.frameHeight).append(", shape=").append(this.shape).append(", shapeAlpha=");
        sb.append(this.shapeAlpha).append(", border=").append(this.border).append(", layout=").append(this.layout).append(", overlayEnabled=").append(this.overlayEnabled).append(", overlayVisible=").append(this.overlayVisible).append(", overlayX=").append(this.overlayX).append(", overlayY=").append(this.overlayY).append(", overlayWidth=").append(this.overlayWidth).append(", scalePercent=").append(this.scalePercent).append(", shadowPercent=").append(this.shadowPercent).append(", rotation=").append(this.rotation).append(", shapeRotation=").append(this.shapeRotation).append(", shadowCurve=").append(this.shadowCurve);
        sb.append(", fitContent=").append(this.fitContent).append(", offsetXPercent=").append(this.offsetXPercent).append(", offsetYPercent=").append(this.offsetYPercent).append(", contentScalePercent=").append(this.contentScalePercent).append(", maskOffsetXPercent=").append(this.maskOffsetXPercent).append(", maskOffsetYPercent=").append(this.maskOffsetYPercent).append(')');
        return sb.toString();
    }

    public int hashCode() {
        int result = Integer.hashCode(this.widgetId);
        result = (((((((((((((((((((((((((((((((((((((((((((((((((((result * 31) + this.kind.hashCode()) * 31) + this.sourceName.hashCode()) * 31) + this.sourceUri.hashCode()) * 31) + Integer.hashCode(this.fps)) * 31) + Integer.hashCode(this.maxEdge)) * 31) + Integer.hashCode(this.quality)) * 31) + Integer.hashCode(this.frameCount)) * 31) + Integer.hashCode(this.frameWidth)) * 31) + Integer.hashCode(this.frameHeight)) * 31) + this.shape.hashCode()) * 31) + Integer.hashCode(this.shapeAlpha)) * 31) + Boolean.hashCode(this.border)) * 31) + this.layout.hashCode()) * 31) + Boolean.hashCode(this.overlayEnabled)) * 31) + Boolean.hashCode(this.overlayVisible)) * 31) + Integer.hashCode(this.overlayX)) * 31) + Integer.hashCode(this.overlayY)) * 31) + Integer.hashCode(this.overlayWidth)) * 31) + Integer.hashCode(this.scalePercent)) * 31) + Integer.hashCode(this.shadowPercent)) * 31) + Integer.hashCode(this.rotation)) * 31) + Integer.hashCode(this.shapeRotation)) * 31) + Boolean.hashCode(this.fitContent)) * 31) + Float.hashCode(this.offsetXPercent)) * 31) + Float.hashCode(this.offsetYPercent)) * 31) + Integer.hashCode(this.contentScalePercent);
        result = (result * 31) + Float.hashCode(this.maskOffsetXPercent);
        result = (result * 31) + Float.hashCode(this.maskOffsetYPercent);
        result = (result * 31) + this.shadowCurve.hashCode();
        return result;
    }

    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WidgetConfig)) {
            return false;
        }
        WidgetConfig widgetConfig = (WidgetConfig) other;
        return this.widgetId == widgetConfig.widgetId && this.kind == widgetConfig.kind && Intrinsics.areEqual(this.sourceName, widgetConfig.sourceName) && Intrinsics.areEqual(this.sourceUri, widgetConfig.sourceUri) && this.fps == widgetConfig.fps && this.maxEdge == widgetConfig.maxEdge && this.quality == widgetConfig.quality && this.frameCount == widgetConfig.frameCount && this.frameWidth == widgetConfig.frameWidth && this.frameHeight == widgetConfig.frameHeight && this.shape == widgetConfig.shape && this.shapeAlpha == widgetConfig.shapeAlpha && this.border == widgetConfig.border && this.layout == widgetConfig.layout && this.overlayEnabled == widgetConfig.overlayEnabled && this.overlayVisible == widgetConfig.overlayVisible && this.overlayX == widgetConfig.overlayX && this.overlayY == widgetConfig.overlayY && this.overlayWidth == widgetConfig.overlayWidth && this.scalePercent == widgetConfig.scalePercent && this.shadowPercent == widgetConfig.shadowPercent && this.rotation == widgetConfig.rotation && this.shapeRotation == widgetConfig.shapeRotation && this.fitContent == widgetConfig.fitContent && Float.compare(this.offsetXPercent, widgetConfig.offsetXPercent) == 0 && Float.compare(this.offsetYPercent, widgetConfig.offsetYPercent) == 0 && this.contentScalePercent == widgetConfig.contentScalePercent && Float.compare(this.maskOffsetXPercent, widgetConfig.maskOffsetXPercent) == 0 && Float.compare(this.maskOffsetYPercent, widgetConfig.maskOffsetYPercent) == 0 && Intrinsics.areEqual(this.shadowCurve, widgetConfig.shadowCurve);
    }

    public WidgetConfig(int widgetId, @NotNull MediaKind kind, @NotNull String sourceName, @NotNull String sourceUri, int fps, int maxEdge, int quality, int frameCount, int frameWidth, int frameHeight, @NotNull ShapeKind shape, int shapeAlpha, boolean border, @NotNull LayoutMode layout, boolean overlayEnabled, boolean overlayVisible, int overlayX, int overlayY, int overlayWidth, int scalePercent, int shadowPercent, int rotation, int shapeRotation, boolean fitContent, float offsetXPercent, float offsetYPercent, int contentScalePercent, float maskOffsetXPercent, float maskOffsetYPercent, String shadowCurve) {
        Intrinsics.checkNotNullParameter(kind, "kind");
        Intrinsics.checkNotNullParameter(sourceName, "sourceName");
        Intrinsics.checkNotNullParameter(sourceUri, "sourceUri");
        Intrinsics.checkNotNullParameter(shape, "shape");
        Intrinsics.checkNotNullParameter(layout, "layout");
        this.widgetId = widgetId;
        this.kind = kind;
        this.sourceName = sourceName;
        this.sourceUri = sourceUri;
        this.fps = fps;
        this.maxEdge = maxEdge;
        this.quality = quality;
        this.frameCount = frameCount;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.shape = shape;
        this.shapeAlpha = shapeAlpha;
        this.border = border;
        this.layout = layout;
        this.overlayEnabled = overlayEnabled;
        this.overlayVisible = overlayVisible;
        this.overlayX = overlayX;
        this.overlayY = overlayY;
        this.overlayWidth = overlayWidth;
        this.scalePercent = scalePercent;
        this.shadowPercent = shadowPercent;
        this.shadowCurve = shadowCurve == null ? "" : shadowCurve;
        this.rotation = rotation;
        this.shapeRotation = shapeRotation;
        this.fitContent = fitContent;
        this.offsetXPercent = offsetXPercent;
        this.offsetYPercent = offsetYPercent;
        this.contentScalePercent = contentScalePercent;
        this.maskOffsetXPercent = maskOffsetXPercent;
        this.maskOffsetYPercent = maskOffsetYPercent;
    }

    public /* synthetic */ WidgetConfig(int i, MediaKind mediaKind, String str, String str2, int i2, int i3, int i4, int i5, int i6, int i7, ShapeKind shapeKind, int i8, boolean z, LayoutMode layoutMode, boolean z2, boolean z3, int i9, int i10, int i11, int i12, int i13, int i14, int i15, boolean z4, float f, float f2, int i16, float f3, float f4, String str3, int i17, DefaultConstructorMarker defaultConstructorMarker) {
        this(i, (i17 & 2) != 0 ? MediaKind.VIDEO : mediaKind, (i17 & 4) != 0 ? "" : str, (i17 & 8) != 0 ? "" : str2, (i17 & 16) != 0 ? 12 : i2, (i17 & 32) != 0 ? 720 : i3, (i17 & 64) != 0 ? 90 : i4, (i17 & 128) != 0 ? 0 : i5, (i17 & 256) != 0 ? 0 : i6, (i17 & 512) != 0 ? 0 : i7, (i17 & 1024) != 0 ? ShapeKind.RECT : shapeKind, (i17 & 2048) != 0 ? 100 : i8, (i17 & 4096) != 0 ? false : z, (i17 & 8192) != 0 ? LayoutMode.FILL : layoutMode, (i17 & 16384) != 0 ? false : z2, (i17 & 32768) != 0 ? false : z3, (i17 & 65536) != 0 ? 0 : i9, (i17 & 131072) != 0 ? 0 : i10, (i17 & 262144) != 0 ? 0 : i11, (i17 & 524288) != 0 ? 100 : i12, (i17 & 1048576) != 0 ? 0 : i13, (i17 & 2097152) != 0 ? 0 : i14, (i17 & 4194304) != 0 ? 0 : i15, (i17 & 8388608) != 0 ? true : z4, (i17 & 16777216) != 0 ? 0.0f : f, (i17 & 33554432) != 0 ? 0.0f : f2, (i17 & 67108864) != 0 ? 100 : i16, (i17 & 134217728) != 0 ? 0.0f : f3, (i17 & 268435456) != 0 ? 0.0f : f4, (i17 & 536870912) != 0 ? "" : str3);
    }

    public final int getWidgetId() {
        return this.widgetId;
    }

    @NotNull
    public final MediaKind getKind() {
        return this.kind;
    }

    @NotNull
    public final String getSourceName() {
        return this.sourceName;
    }

    @NotNull
    public final String getSourceUri() {
        return this.sourceUri;
    }

    public final int getFps() {
        return this.fps;
    }

    public final int getMaxEdge() {
        return this.maxEdge;
    }

    public final int getQuality() {
        return this.quality;
    }

    public final int getFrameCount() {
        return this.frameCount;
    }

    public final int getFrameWidth() {
        return this.frameWidth;
    }

    public final int getFrameHeight() {
        return this.frameHeight;
    }

    @NotNull
    public final ShapeKind getShape() {
        return this.shape;
    }

    public final int getShapeAlpha() {
        return this.shapeAlpha;
    }

    public final boolean getBorder() {
        return this.border;
    }

    @NotNull
    public final LayoutMode getLayout() {
        return this.layout;
    }

    public final boolean getOverlayEnabled() {
        return this.overlayEnabled;
    }

    public final boolean getOverlayVisible() {
        return this.overlayVisible;
    }

    public final int getOverlayX() {
        return this.overlayX;
    }

    public final int getOverlayY() {
        return this.overlayY;
    }

    public final int getOverlayWidth() {
        return this.overlayWidth;
    }

    public final int getScalePercent() {
        return this.scalePercent;
    }

    public final int getShadowPercent() {
        return this.shadowPercent;
    }

    /** v1.10.221：阴影曲线（存档串）。空串/坏串由 {@link ShadowCurve#parse} 兜底成默认曲线。 */
    @NotNull
    public final String getShadowCurve() {
        return this.shadowCurve;
    }

    public final int getRotation() {
        return this.rotation;
    }

    public final int getShapeRotation() {
        return this.shapeRotation;
    }

    public final boolean getFitContent() {
        return this.fitContent;
    }

    public final float getOffsetXPercent() {
        return this.offsetXPercent;
    }

    public final float getOffsetYPercent() {
        return this.offsetYPercent;
    }

    public final int getContentScalePercent() {
        return this.contentScalePercent;
    }

    public final float getMaskOffsetXPercent() {
        return this.maskOffsetXPercent;
    }

    public final float getMaskOffsetYPercent() {
        return this.maskOffsetYPercent;
    }
}
