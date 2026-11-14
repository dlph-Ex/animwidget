package io.github.animwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* compiled from: HsvPickerView.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��Z\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0010\b\n\u0002\b\u0006\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n��\n\u0002\u0018\u0002\u0018��2\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0002\u0010\u0004J\u0010\u0010#\u001a\u00020\u00172\u0006\u0010$\u001a\u00020%H\u0014J(\u0010&\u001a\u00020\u00172\u0006\u0010'\u001a\u00020\b2\u0006\u0010(\u001a\u00020\b2\u0006\u0010)\u001a\u00020\b2\u0006\u0010*\u001a\u00020\bH\u0014J\u0010\u0010+\u001a\u00020,2\u0006\u0010-\u001a\u00020.H\u0016R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e¢\u0006\u0002\n��R$\u0010\t\u001a\u00020\b2\u0006\u0010\u0007\u001a\u00020\b@FX\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b\n\u0010\u000b\"\u0004\b\f\u0010\rR\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u000e¢\u0006\u0002\n��R\u0010\u0010\u0010\u001a\u0004\u0018\u00010\u0006X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u0004¢\u0006\u0002\n��R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u0004¢\u0006\u0002\n��R(\u0010\u0015\u001a\u0010\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u0017\u0018\u00010\u0016X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b\u0018\u0010\u0019\"\u0004\b\u001a\u0010\u001bR\u000e\u0010\u001c\u001a\u00020\u001dX\u0082\u0004¢\u0006\u0002\n��R\u000e\u0010\u001e\u001a\u00020\u000fX\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010\u001f\u001a\u00020\u0012X\u0082\u0004¢\u0006\u0002\n��R\u000e\u0010 \u001a\u00020\u0014X\u0082\u0004¢\u0006\u0002\n��R\u000e\u0010!\u001a\u00020\u000fX\u0082\u000e¢\u0006\u0002\n��R\u0010\u0010\"\u001a\u0004\u0018\u00010\u0006X\u0082\u000e¢\u0006\u0002\n��"}, d2 = {"Lio/github/animwidget/HsvPickerView;", "Landroid/view/View;", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "blackGradient", "Landroid/graphics/LinearGradient;", "value", "", "color", "getColor", "()I", "setColor", "(I)V", "hue", "", "hueGradient", "huePath", "Landroid/graphics/Path;", "hueRect", "Landroid/graphics/RectF;", "onColorChanged", "Lkotlin/Function1;", "", "getOnColorChanged", "()Lkotlin/jvm/functions/Function1;", "setOnColorChanged", "(Lkotlin/jvm/functions/Function1;)V", "paint", "Landroid/graphics/Paint;", "sat", "svPath", "svRect", "valueV", "whiteGradient", "onDraw", "canvas", "Landroid/graphics/Canvas;", "onSizeChanged", "w", "h", "oldw", "oldh", "onTouchEvent", "", "event", "Landroid/view/MotionEvent;"})
/* loaded from: classes.jar:io/github/animwidget/HsvPickerView.class */
public final class HsvPickerView extends View {
    int color;

    @Nullable
    Function1<? super Integer, Unit> onColorChanged;
    float hue;
    float sat;
    float valueV;

    @NotNull
    final Paint paint;

    @NotNull
    final RectF svRect;

    @NotNull
    final RectF hueRect;

    @NotNull
    final Path svPath;

    @NotNull
    final Path huePath;

    @Nullable
    LinearGradient whiteGradient;

    @Nullable
    LinearGradient blackGradient;

    @Nullable
    LinearGradient hueGradient;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public HsvPickerView(@NotNull Context context) {
        super(context);
        Intrinsics.checkNotNullParameter(context, "context");
        this.color = -15293622;
        this.hue = 120.0f;
        this.sat = 0.8f;
        this.valueV = 0.8f;
        this.paint = new Paint(1);
        this.svRect = new RectF();
        this.hueRect = new RectF();
        this.svPath = new Path();
        this.huePath = new Path();
    }

    public final int getColor() {
        return this.color;
    }

    public final void setColor(int value) {
        this.color = value;
        float[] hsv = new float[3];
        Color.colorToHSV(value, hsv);
        this.hue = hsv[0];
        this.sat = hsv[1];
        this.valueV = hsv[2];
        invalidate();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public final Function1<Integer, Unit> getOnColorChanged() {
        return (Function1<Integer, Unit>) this.onColorChanged;
    }

    public final void setOnColorChanged(@Nullable Function1<? super Integer, Unit> function1) {
        this.onColorChanged = function1;
    }

    @Override // android.view.View
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float d = getResources().getDisplayMetrics().density;
        float pad = 10.0f * d;
        float hueWidth = 26.0f * d;
        float gap = 10.0f * d;
        this.svRect.set(pad, pad, ((w - pad) - hueWidth) - gap, h - pad);
        this.hueRect.set((w - pad) - hueWidth, pad, w - pad, h - pad);
        float r = 14.0f * d;
        this.svPath.reset();
        this.svPath.addRoundRect(this.svRect, r, r, Path.Direction.CW);
        this.huePath.reset();
        this.huePath.addRoundRect(this.hueRect, hueWidth / 2.0f, hueWidth / 2.0f, Path.Direction.CW);
        this.whiteGradient = new LinearGradient(this.svRect.left, this.svRect.top, this.svRect.right, this.svRect.top, -1, 16777215, Shader.TileMode.CLAMP);
        this.blackGradient = new LinearGradient(this.svRect.left, this.svRect.top, this.svRect.left, this.svRect.bottom, 0, -16777216, Shader.TileMode.CLAMP);
        this.hueGradient = new LinearGradient(this.hueRect.left, this.hueRect.top, this.hueRect.left, this.hueRect.bottom, new int[]{-65536, -256, -16711936, -16711681, -16776961, -65281, -65536}, (float[]) null, Shader.TileMode.CLAMP);
    }

    @Override // android.view.View
    protected void onDraw(@NotNull Canvas canvas) {
        Intrinsics.checkNotNullParameter(canvas, "canvas");
        super.onDraw(canvas);
        if (this.svRect.isEmpty() || this.hueRect.isEmpty()) {
            return;
        }
        float d = getResources().getDisplayMetrics().density;
        canvas.save();
        canvas.clipPath(this.svPath);
        this.paint.setShader(null);
        this.paint.setColor(Color.HSVToColor(new float[]{this.hue, 1.0f, 1.0f}));
        canvas.drawRect(this.svRect, this.paint);
        LinearGradient it = this.whiteGradient;
        if (it != null) {
            this.paint.setShader(it);
            canvas.drawRect(this.svRect, this.paint);
        }
        LinearGradient it2 = this.blackGradient;
        if (it2 != null) {
            this.paint.setShader(it2);
            canvas.drawRect(this.svRect, this.paint);
        }
        canvas.restore();
        canvas.save();
        canvas.clipPath(this.huePath);
        this.paint.setShader(this.hueGradient);
        canvas.drawRect(this.hueRect, this.paint);
        canvas.restore();
        this.paint.setShader(null);
        // v1.10.145（反馈 3）：色盘和色相条加一圈极淡描边。
        // 浅色主题下色相条的橙/黄端和白卡片融成一片，根本看不出这是一根滑块。
        ThemeColors edgeColors = ThemeManager.INSTANCE.colors(getContext());
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(1.0f * d);
        int edge = ThemeManager.INSTANCE.withAlpha(edgeColors.getText(), 0.18f);
        this.paint.setColor(edge);
        canvas.drawPath(this.svPath, this.paint);
        canvas.drawPath(this.huePath, this.paint);
        float sx = this.svRect.left + (this.sat * this.svRect.width());
        float sy = this.svRect.top + ((1.0f - this.valueV) * this.svRect.height());
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(2.2f * d);
        this.paint.setColor(-1);
        canvas.drawCircle(sx, sy, 9.0f * d, this.paint);
        this.paint.setStrokeWidth(1.0f * d);
        this.paint.setColor(-16777216);
        canvas.drawCircle(sx, sy, 9.0f * d, this.paint);
        float hy = this.hueRect.top + ((this.hue / 360.0f) * this.hueRect.height());
        // v1.10.144（反馈 2）：指示环必须**始终完整落在滑块内部**。
        // 滑到顶/底时 hy 正好贴在端点上，环有一半画到滑块圆角外面（视觉上就是「图标消失了」）。
        float half = 6.0f * d;
        if (hy < this.hueRect.top + half) {
            hy = this.hueRect.top + half;
        } else if (hy > this.hueRect.bottom - half) {
            hy = this.hueRect.bottom - half;
        }
        RectF ring = new RectF(this.hueRect.left - (3.0f * d), hy - half, this.hueRect.right + (3.0f * d), hy + half);
        this.paint.setColor(-1);
        this.paint.setStrokeWidth(2.0f * d);
        canvas.drawRoundRect(ring, 6.0f * d, 6.0f * d, this.paint);
        this.paint.setStyle(Paint.Style.FILL);
    }

    private boolean draggingHue;

    @Override // android.view.View
    public boolean onTouchEvent(@NotNull MotionEvent event) {
        Intrinsics.checkNotNullParameter(event, "event");
        // v1.10.463：**自绘控件的 isEnabled 必须自己判**。
        // View.onTouchEvent 会在 disabled 时吞掉点击，但我们整个方法都重写了 —— 于是
        // 语义色页「非自定义档置灰」时，色盘看上去灰了、手指一拖照样改色（假禁用）。
        if (!isEnabled()) {
            return false;
        }
        float d = getResources().getDisplayMetrics().density;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            // v1.10.142：按下那一刻就锁定「这次拖的是滑块还是色盘」。
            // 老实现每一帧都拿当前 x 重新判断 —— 手指从右侧滑块横着拖到左边（色盘区）时，
            // 判定突然翻成色盘，色相就停住不动了（就是「拖到其他区域就断开」）。
            this.draggingHue = event.getX() >= this.hueRect.left - (8.0f * d);
        }
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            if (this.draggingHue) {
                this.hue = RangesKt.coerceIn((event.getY() - this.hueRect.top) / this.hueRect.height(), 0.0f, 1.0f) * 360.0f;
            } else {
                this.sat = RangesKt.coerceIn((event.getX() - this.svRect.left) / this.svRect.width(), 0.0f, 1.0f);
                this.valueV = 1.0f - RangesKt.coerceIn((event.getY() - this.svRect.top) / this.svRect.height(), 0.0f, 1.0f);
            }
            // v1.10.144（反馈 2）：这里不能调 setColor —— 它内部会用 colorToHSV 把 hue 重算一遍，
            // 纯红附近 0°/360° 会来回翻，指示环瞬间跳到滑块另一头（看着也是「消失」）。
            // 拖动时直接用自己算好的 hue/sat/value 出颜色。
            this.color = Color.HSVToColor(new float[]{this.hue, this.sat, this.valueV});
            invalidate();
            Function1<? super Integer, Unit> function1 = this.onColorChanged;
            if (function1 != null) {
                function1.invoke(Integer.valueOf(this.color));
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            this.draggingHue = false;
        }
        return true;
    }
}
