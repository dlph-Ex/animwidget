package io.github.animwidget;

import android.graphics.Bitmap;

import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 预览渲染（编辑页专用）。
 *
 * v1.3.7 起去掉每次渲染都做的 WebP 编解码往返（原来哪怕 quality=100 也会
 * encode→decode 一遍，一次 20~40ms，正是拖滑块卡顿的主因）。现在直接
 * 等比缩放 → FrameComposer.compose，尺寸上限 1080，单帧通常 3~8ms。
 */
public final class PreviewRenderer {

    public static final PreviewRenderer INSTANCE = new PreviewRenderer();

    private PreviewRenderer() {
    }

    public static /* synthetic */ Bitmap render$default(PreviewRenderer previewRenderer, Bitmap bitmap, int i, int i2,
                                                       LayoutMode layoutMode, ShapeKind shapeKind, int i3, boolean z,
                                                       int i4, int i5, int i6, int i7, int i8, boolean z2, int i9,
                                                       float f, float f2, int i10, float f3, float f4, String str3, int i11, Object obj) {
        if ((i11 & 512) != 0) {
            i6 = 100;
        }
        if ((i11 & 1024) != 0) {
            i7 = 0;
        }
        if ((i11 & 2048) != 0) {
            i8 = 0;
        }
        if ((i11 & 4096) != 0) {
            z2 = true;
        }
        if ((i11 & 8192) != 0) {
            i9 = 0;
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
        if ((i11 & 131072) != 0) {
            f3 = 0.0f;
        }
        if ((i11 & 262144) != 0) {
            f4 = 0.0f;
        }
        if ((i11 & 524288) != 0) {
            str3 = "";
        }
        return previewRenderer.render(bitmap, i, i2, layoutMode, shapeKind, i3, z, i4, i5, i6, i7, i8, z2, i9, str3, f, f2, i10, 0.0f, 0.0f);
    }

    @NotNull
    public final Bitmap render(@NotNull Bitmap source, int viewWidth, int viewHeight, @NotNull LayoutMode layout,
                               @NotNull ShapeKind shape, int alphaPercent, boolean border, int maxEdge, int quality,
                               int scalePercent, int rotation, int shapeRotation, boolean fitContent,
                               int shadowPercent, @Nullable String shadowCurve, float offsetXPercent, float offsetYPercent, int contentScalePercent,
                               float maskOffsetXPercent, float maskOffsetYPercent) {
        Intrinsics.checkNotNullParameter(source, "source");
        Intrinsics.checkNotNullParameter(layout, "layout");
        Intrinsics.checkNotNullParameter(shape, "shape");
        int longest = RangesKt.coerceAtLeast(Math.max(viewWidth, viewHeight), 1);
        float factor = longest > 1080 ? 1080.0f / longest : 1.0f;
        int width = RangesKt.coerceIn((int) (viewWidth * factor), 96, 1080);
        int height = RangesKt.coerceIn((int) (viewHeight * factor), 96, 1080);
        Bitmap scaled = scaleToEdge(source, RangesKt.coerceIn(maxEdge, 120, 1920));
        try {
            return FrameComposer.INSTANCE.compose(scaled, width, height, layout, shape, alphaPercent, border,
                    scalePercent, rotation, shapeRotation, fitContent, shadowPercent, shadowCurve, offsetXPercent,
                    offsetYPercent, contentScalePercent, maskOffsetXPercent, maskOffsetYPercent);
        } finally {
            if (scaled != source && !scaled.isRecycled()) {
                scaled.recycle();
            }
        }
    }

    @NotNull
    private final Bitmap scaleToEdge(@NotNull Bitmap source, int maxEdge) {
        int longest = Math.max(source.getWidth(), source.getHeight());
        if (longest <= maxEdge || longest == 0) {
            return source;
        }
        float ratio = maxEdge / (float) longest;
        Bitmap scaled = Bitmap.createScaledBitmap(source,
                (int) RangesKt.coerceAtLeast(source.getWidth() * ratio, 1.0f),
                (int) RangesKt.coerceAtLeast(source.getHeight() * ratio, 1.0f), true);
        Intrinsics.checkNotNullExpressionValue(scaled, "createScaledBitmap(...)");
        return scaled;
    }
}
