package io.github.animwidget;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.math.MathKt;
import kotlin.ranges.RangesKt;
import org.jetbrains.annotations.NotNull;

/* compiled from: AspectRatioFrameLayout.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0005\n\u0002\u0010\u0002\n��\n\u0002\u0010\b\n��\u0018��2\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0002\u0010\u0004J\u0018\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u000eH\u0014R\u001a\u0010\u0005\u001a\u00020\u0006X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b\u0007\u0010\b\"\u0004\b\t\u0010\n"}, d2 = {"Lio/github/animwidget/AspectRatioFrameLayout;", "Landroid/widget/FrameLayout;", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "ratio", "", "getRatio", "()F", "setRatio", "(F)V", "onMeasure", "", "widthMeasureSpec", "", "heightMeasureSpec"})
/* loaded from: classes.jar:io/github/animwidget/AspectRatioFrameLayout.class */
public final class AspectRatioFrameLayout extends FrameLayout {
    float ratio;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public AspectRatioFrameLayout(@NotNull Context context) {
        super(context);
        Intrinsics.checkNotNullParameter(context, "context");
        this.ratio = 1.0f;
    }

    public final float getRatio() {
        return this.ratio;
    }

    public final void setRatio(float f) {
        this.ratio = f;
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        int w = maxWidth;
        int h = this.ratio > 0.0f ? MathKt.roundToInt(w / this.ratio) : maxHeight;
        if (h > maxHeight) {
            h = maxHeight;
            w = this.ratio > 0.0f ? MathKt.roundToInt(h * this.ratio) : maxWidth;
        }
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(RangesKt.coerceAtLeast(w, 1), 1073741824), View.MeasureSpec.makeMeasureSpec(RangesKt.coerceAtLeast(h, 1), 1073741824));
    }
}
