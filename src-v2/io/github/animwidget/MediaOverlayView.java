package io.github.animwidget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Lambda;
import kotlin.ranges.RangesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* compiled from: MediaOverlayView.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��B\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n��\n\u0002\u0018\u0002\n��\n\u0002\u0010\t\n��\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\u0018��2\u00020\u0001B)\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u0012\u0006\u0010\b\u001a\u00020\t¢\u0006\u0002\u0010\nJ\b\u0010\u0011\u001a\u00020\u0012H\u0014J\u0010\u0010\u0013\u001a\u00020\u00122\u0006\u0010\u0014\u001a\u00020\u0015H\u0015R\u0010\u0010\u0004\u001a\u0004\u0018\u00010\u0005X\u0082\u0004¢\u0006\u0002\n��R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004¢\u0006\u0002\n��R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004¢\u0006\u0002\n��R\u0010\u0010\u0006\u001a\u0004\u0018\u00010\u0007X\u0082\u0004¢\u0006\u0002\n��R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u0004¢\u0006\u0002\n��R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u000e¢\u0006\u0002\n��"}, d2 = {"Lio/github/animwidget/MediaOverlayView;", "Landroid/view/View;", "context", "Landroid/content/Context;", "bitmap", "Landroid/graphics/Bitmap;", "movie", "Landroid/graphics/Movie;", "config", "Lio/github/animwidget/WidgetConfig;", "(Landroid/content/Context;Landroid/graphics/Bitmap;Landroid/graphics/Movie;Lio/github/animwidget/WidgetConfig;)V", "frameDuration", "", "paint", "Landroid/graphics/Paint;", "startedAt", "", "onAttachedToWindow", "", "onDraw", "canvas", "Landroid/graphics/Canvas;"})
/* loaded from: classes.jar:io/github/animwidget/MediaOverlayView.class */
public final class MediaOverlayView extends View {

    @Nullable
    final Bitmap bitmap;

    @Nullable
    final Movie movie;

    @NotNull
    final WidgetConfig config;

    @NotNull
    final Paint paint;
    final int frameDuration;
    long startedAt;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public MediaOverlayView(@NotNull Context context, @Nullable Bitmap bitmap, @Nullable Movie movie, @NotNull WidgetConfig config) {
        this(context, bitmap, movie, config, 0);
    }

    /**
     * v1.10.0：多带一个「真实总时长」。
     * Movie.duration() 对部分 GIF 返回 0（v1.9.0 踩过），以前 frameDuration 被兜成 1ms →
     * 覆盖层的 GIF 等于定在第一帧（看着像「不动」）。总时长改成字节解析的结果。
     */
    public MediaOverlayView(@NotNull Context context, @Nullable Bitmap bitmap, @Nullable Movie movie,
                            @NotNull WidgetConfig config, int durationMs) {
        super(context);
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(config, "config");
        this.bitmap = bitmap;
        this.movie = movie;
        this.config = config;
        this.paint = new Paint(3);
        int fallback = durationMs > 0 ? durationMs : (this.movie != null ? this.movie.duration() : 0);
        this.frameDuration = RangesKt.coerceAtLeast(fallback, 1);
        setWillNotDraw(false);
        setLayerType(1, null);
    }

    @Override // android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.startedAt = SystemClock.uptimeMillis();
    }

    @Override // android.view.View
    @SuppressLint({"DrawAllocation"})
    protected void onDraw(@NotNull Canvas canvas) {
        Intrinsics.checkNotNullParameter(canvas, "canvas");
        super.onDraw(canvas);
        // v1.8.11：onDraw 跑在悬浮窗所在进程的 UI 线程，drawFrame/movie.draw
        // 任何异常都会直接杀掉进程（看起来就是闪退）。一帧画坏跳过即可。
        try {
            drawContent(canvas);
        } catch (Throwable th) {
            Diag.warnThrottled(getContext(), "O01", "悬浮窗绘制异常: " + th.getClass().getSimpleName()
                    + (th.getMessage() != null ? "/" + th.getMessage() : ""));
            CrashLog.save(getContext(), Thread.currentThread(), th);
        }
    }

    private final void drawContent(@NotNull Canvas canvas) {
        int width;
        int height;
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        if (viewWidth <= 0 || viewHeight <= 0) {
            return;
        }
        if (this.movie != null) {
            width = this.movie.width();
        } else {
            Bitmap bitmap = this.bitmap;
            width = bitmap != null ? bitmap.getWidth() : 0;
        }
        int sourceWidth = width;
        if (this.movie != null) {
            height = this.movie.height();
        } else {
            Bitmap bitmap2 = this.bitmap;
            height = bitmap2 != null ? bitmap2.getHeight() : 0;
        }
        int sourceHeight = height;
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return;
        }
        if (this.movie != null) {
            int elapsed = (int) ((SystemClock.uptimeMillis() - this.startedAt) % this.frameDuration);
            this.movie.setTime(elapsed);
        }
        FrameComposer.INSTANCE.drawFrame(canvas, viewWidth, viewHeight, sourceWidth, sourceHeight, this.config.getLayout(), this.config.getShape(), this.config.getBorder(), this.config.getScalePercent(), this.config.getRotation(), this.config.getShapeRotation(), this.config.getFitContent(), this.config.getShadowPercent(), this.config.getShadowCurve(), 0, this.config.getOffsetXPercent(), this.config.getOffsetYPercent(), this.config.getContentScalePercent(), this.config.getMaskOffsetXPercent(), this.config.getMaskOffsetYPercent(), null, new AnonymousClass1(sourceWidth, sourceHeight));
        if (this.movie != null) {
            postInvalidateOnAnimation();
        }
    }

    /* compiled from: MediaOverlayView.kt */
    @Metadata(mv = {1, 9, 0}, k = 3, xi = 48, d1 = {"��\u0014\n��\n\u0002\u0010\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\u0010��\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\n¢\u0006\u0002\b\u0006"}, d2 = {"<anonymous>", "", "target", "Landroid/graphics/Canvas;", "dest", "Landroid/graphics/RectF;", "invoke"})
    /* renamed from: io.github.animwidget.MediaOverlayView$onDraw$1, reason: invalid class name */
    /* loaded from: classes.jar:io/github/animwidget/MediaOverlayView$onDraw$1.class */
    final class AnonymousClass1 implements Function2<Canvas, RectF, Unit> {
        final /* synthetic */ int $sourceWidth;
        final /* synthetic */ int $sourceHeight;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        AnonymousClass1(int $sourceWidth, int $sourceHeight) {
            this.$sourceWidth = $sourceWidth;
            this.$sourceHeight = $sourceHeight;
        }

        public final Unit invoke(@NotNull Canvas target, @NotNull RectF dest) {
            Intrinsics.checkNotNullParameter(target, "target");
            Intrinsics.checkNotNullParameter(dest, "dest");
            if (MediaOverlayView.this.movie == null) {
                if (MediaOverlayView.this.bitmap != null && !MediaOverlayView.this.bitmap.isRecycled()) {
                    target.drawBitmap(MediaOverlayView.this.bitmap, (Rect) null, dest, MediaOverlayView.this.paint);
                    return Unit.INSTANCE;
                }
                return Unit.INSTANCE;
            }
            target.save();
            target.translate(dest.left, dest.top);
            target.scale(dest.width() / this.$sourceWidth, dest.height() / this.$sourceHeight);
            MediaOverlayView.this.movie.draw(target, 0.0f, 0.0f);
            target.restore();
            return Unit.INSTANCE;
        }
    }
}
