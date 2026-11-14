package io.github.animwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;

/* compiled from: ShapeChipView.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��:\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\b\n\u0002\u0018\u0002\n��\n\u0002\u0010\u000b\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\r\n\u0002\u0010\u0002\n��\n\u0002\u0018\u0002\u0018��2\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0002\u0010\u0004J\u0010\u0010%\u001a\u00020&2\u0006\u0010'\u001a\u00020(H\u0014R\u001a\u0010\u0005\u001a\u00020\u0006X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b\u0007\u0010\b\"\u0004\b\t\u0010\nR\u001a\u0010\u000b\u001a\u00020\u0006X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b\f\u0010\b\"\u0004\b\r\u0010\nR\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u0004¢\u0006\u0002\n��R\u001a\u0010\u0010\u001a\u00020\u0011X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b\u0012\u0010\u0013\"\u0004\b\u0014\u0010\u0015R\u000e\u0010\u0016\u001a\u00020\u000fX\u0082\u0004¢\u0006\u0002\n��R\u001a\u0010\u0017\u001a\u00020\u0018X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b\u0019\u0010\u001a\"\u0004\b\u001b\u0010\u001cR\u001a\u0010\u001d\u001a\u00020\u0006X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b\u001e\u0010\b\"\u0004\b\u001f\u0010\nR\u000e\u0010 \u001a\u00020\u000fX\u0082\u0004¢\u0006\u0002\n��R\u001a\u0010!\u001a\u00020\u0006X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b\"\u0010\b\"\u0004\b#\u0010\nR\u000e\u0010$\u001a\u00020\u000fX\u0082\u0004¢\u0006\u0002\n��"}, d2 = {"Lio/github/animwidget/ShapeChipView;", "Landroid/view/View;", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "accentColor", "", "getAccentColor", "()I", "setAccentColor", "(I)V", "bgColor", "getBgColor", "setBgColor", "bgPaint", "Landroid/graphics/Paint;", "chosen", "", "getChosen", "()Z", "setChosen", "(Z)V", "fillPaint", "shape", "Lio/github/animwidget/ShapeKind;", "getShape", "()Lio/github/animwidget/ShapeKind;", "setShape", "(Lio/github/animwidget/ShapeKind;)V", "shapeColor", "getShapeColor", "setShapeColor", "strokePaint", "textColor", "getTextColor", "setTextColor", "textPaint", "onDraw", "", "canvas", "Landroid/graphics/Canvas;"})
/* loaded from: classes.jar:io/github/animwidget/ShapeChipView.class */
public final class ShapeChipView extends View {

    @NotNull
    ShapeKind shape;
    boolean chosen;
    int accentColor;
    int bgColor;
    int shapeColor;
    int textColor;

    @NotNull
    final Paint bgPaint;

    @NotNull
    final Paint fillPaint;

    @NotNull
    final Paint strokePaint;

    @NotNull
    final Paint textPaint;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public ShapeChipView(@NotNull Context context) {
        super(context);
        Intrinsics.checkNotNullParameter(context, "context");
        this.shape = ShapeKind.ROUNDED;
        this.accentColor = -15293622;
        this.bgColor = -1578258;
        this.shapeColor = -13156015;
        this.textColor = -9735552;
        this.bgPaint = new Paint(1);
        this.fillPaint = new Paint(1);
        this.strokePaint = new Paint(1);
        Paint $this$textPaint_u24lambda_u240 = new Paint(1);
        $this$textPaint_u24lambda_u240.setTextAlign(Paint.Align.CENTER);
        this.textPaint = $this$textPaint_u24lambda_u240;
    }

    @NotNull
    public final ShapeKind getShape() {
        return this.shape;
    }

    public final void setShape(@NotNull ShapeKind shapeKind) {
        Intrinsics.checkNotNullParameter(shapeKind, "<set-?>");
        this.shape = shapeKind;
    }

    public final boolean getChosen() {
        return this.chosen;
    }

    public final void setChosen(boolean z) {
        this.chosen = z;
    }

    public final int getAccentColor() {
        return this.accentColor;
    }

    public final void setAccentColor(int i) {
        this.accentColor = i;
    }

    public final int getBgColor() {
        return this.bgColor;
    }

    public final void setBgColor(int i) {
        this.bgColor = i;
    }

    public final int getShapeColor() {
        return this.shapeColor;
    }

    public final void setShapeColor(int i) {
        this.shapeColor = i;
    }

    public final int getTextColor() {
        return this.textColor;
    }

    public final void setTextColor(int i) {
        this.textColor = i;
    }

    @Override // android.view.View
    protected void onDraw(@NotNull Canvas canvas) {
        Intrinsics.checkNotNullParameter(canvas, "canvas");
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0.0f || h <= 0.0f) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        float radius = 12.0f * density;
        this.bgPaint.setColor(this.bgColor);
        canvas.drawRoundRect(0.0f, 0.0f, w, h, radius, radius, this.bgPaint);
        if (this.chosen) {
            this.bgPaint.setColor((this.accentColor & 16777215) | 570425344);
            canvas.drawRoundRect(0.0f, 0.0f, w, h, radius, radius, this.bgPaint);
        }
        // v1.10.212：**回滚**v1.10.211 的「图形占满整格」。
        // 那一版把图形按整格宽高画（只留 5dp 内边距），格子从「深色圆角卡片里放一个图形」
        // 变成了「一整块浅色方块」，一眼分不清是格子还是图形（RECT / 超椭圆 / 圆形 三格几乎一样）。
        // 回到 0.72*min(w,h) 的居中方框；图形按自己的设计比例画（pathFitted），图标才认得出。
        float size = Math.min(w, h) * 0.72f;
        canvas.save();
        canvas.translate((w - size) / 2.0f, (h - size) / 2.0f);
        Path path = ShapeFactory.INSTANCE.pathFitted(this.shape, size, size);
        this.fillPaint.setColor(this.shapeColor);
        canvas.drawPath(path, this.fillPaint);
        canvas.restore();
        if (this.chosen) {
            this.strokePaint.setStyle(Paint.Style.STROKE);
            this.strokePaint.setStrokeWidth(3.0f * density);
            this.strokePaint.setColor(this.accentColor);
            canvas.drawRoundRect(0.0f, 0.0f, w, h, radius, radius, this.strokePaint);
        }
    }
}
