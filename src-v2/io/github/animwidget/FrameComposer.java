package io.github.animwidget;

import android.graphics.Bitmap;
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
import org.jetbrains.annotations.Nullable;

/* compiled from: FrameComposer.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��^\n\u0002\u0018\u0002\n\u0002\u0010��\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\n\n\u0002\u0010\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\bÆ\u0002\u0018��2\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J(\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\t2\u0006\u0010\u000b\u001a\u00020\t2\u0006\u0010\f\u001a\u00020\tH\u0002J\u008e\u0001\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u000e2\u0006\u0010\u000b\u001a\u00020\t2\u0006\u0010\f\u001a\u00020\t2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\t2\u0006\u0010\u0015\u001a\u00020\u00162\b\b\u0002\u0010\u0017\u001a\u00020\t2\b\b\u0002\u0010\u0018\u001a\u00020\t2\b\b\u0002\u0010\u0019\u001a\u00020\t2\b\b\u0002\u0010\u001a\u001a\u00020\u00162\b\b\u0002\u0010\u001b\u001a\u00020\t2\b\b\u0002\u0010\u001c\u001a\u00020\u00042\b\b\u0002\u0010\u001d\u001a\u00020\u00042\b\b\u0002\u0010\u001e\u001a\u00020\tJ.\u0010\u001f\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\t2\u0006\u0010\u000b\u001a\u00020\t2\u0006\u0010\f\u001a\u00020\t2\u0006\u0010\u0010\u001a\u00020\u0011Jj\u0010 \u001a\u00020!2\u0006\u0010\"\u001a\u00020#2\u0006\u0010$\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\t2\u0006\u0010\u0018\u001a\u00020\t2\u0006\u0010%\u001a\u00020\u00162\u0006\u0010\u001c\u001a\u00020\u00042\u0006\u0010\u001d\u001a\u00020\u00042\u0006\u0010&\u001a\u00020\u00042\u0018\u0010'\u001a\u0014\u0012\u0004\u0012\u00020#\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020!0(H\u0002J°\u0001\u0010)\u001a\u00020!2\u0006\u0010\"\u001a\u00020#2\u0006\u0010\u000b\u001a\u00020\t2\u0006\u0010\f\u001a\u00020\t2\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\t2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\t2\u0006\u0010\u0018\u001a\u00020\t2\u0006\u0010\u0019\u001a\u00020\t2\u0006\u0010\u001a\u001a\u00020\u00162\u0006\u0010\u001b\u001a\u00020\t2\b\b\u0002\u0010\u0014\u001a\u00020\t2\b\b\u0002\u0010\u001c\u001a\u00020\u00042\b\b\u0002\u0010\u001d\u001a\u00020\u00042\b\b\u0002\u0010\u001e\u001a\u00020\t2\u0018\u0010'\u001a\u0014\u0012\u0004\u0012\u00020#\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020!0(J0\u0010*\u001a\u00020!2\u0006\u0010\"\u001a\u00020#2\u0006\u0010+\u001a\u00020,2\u0006\u0010-\u001a\u00020\t2\u0006\u0010.\u001a\u00020\u00042\u0006\u0010/\u001a\u00020\tH\u0002J\u001a\u00100\u001a\u0004\u0018\u00010,2\u0006\u0010+\u001a\u00020,2\u0006\u00101\u001a\u00020\u0004H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T¢\u0006\u0002\n��R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T¢\u0006\u0002\n��"}, d2 = {"Lio/github/animwidget/FrameComposer;", "", "()V", "CONTENT_BLEED", "", "SHADOW_CUT_INSET", "autoContentRect", "Landroid/graphics/RectF;", "sourceWidth", "", "sourceHeight", "width", "height", "compose", "Landroid/graphics/Bitmap;", "source", "layout", "Lio/github/animwidget/LayoutMode;", "shape", "Lio/github/animwidget/ShapeKind;", "alphaPercent", "border", "", "scalePercent", "rotation", "shapeRotation", "fitContent", "shadowPercent", "offsetXPercent", "offsetYPercent", "contentScalePercent", "contentRect", "drawContent", "", "canvas", "Landroid/graphics/Canvas;", "area", "fit", "contentScale", "drawSource", "Lkotlin/Function2;", "drawFrame", "drawShadow", "path", "Landroid/graphics/Path;", "strength", "margin", "alpha", "insetPath", "inset"})
@SourceDebugExtension({"SMAP\nFrameComposer.kt\nKotlin\n*S Kotlin\n*F\n+ 1 FrameComposer.kt\nio/github/animwidget/FrameComposer\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,354:1\n1#2:355\n*E\n"})
/* loaded from: classes.jar:io/github/animwidget/FrameComposer.class */
public final class FrameComposer {

    // ---------------------------------------------------------------- 阴影（v1.10.227：复刻参考实现）
    /**
     * 阴影**不再有自造模型**。
     *
     * <p>v1.10.211~226 这里是「扩散 + 渐变长度 + 渐变形状」三参数曲线，配 25~28 条描边近似；
     * 每版都必须拿用户的截图逐像素反推参数，连续两版量错口径（LESSONS 37/38）。</p>
     *
     * <p>v1.10.227 起几何全部来自反编译出的参考实现
     * {@code com.photo.beautifulwidgets.utils.d.G(Bitmap,int)}，见 {@link ShadowLayer}：
     * 每边留白 {@code 2*unit}（unit = 画面框宽/35）、模糊半径 = unit（σ = unit/√3）、
     * 不透明度 = 强度 + 20。合成器这边只剩两件事 —— 把画面按留白内缩、把烘好的阴影贴上去。</p>
     */
    private static final float SHADOW_CUT_INSET = 2.0f;

    // ---------------------------------------------------------------- W27 合成分解
    /**
     * v1.10.188：**合成耗时的分解统计**（W27）。
     *
     * 改尺寸「卡一轮」的定性已经清楚：预热一帧要 ~130ms、只能 30 帧/秒，而播放要 50 帧/秒。
     * 但 130ms 花在哪**不能猜**（LESSONS：不许拿自己补的值当客观证据）。这里把一帧拆成
     * 「阴影 / 内容（含 clip）/ 其它（建位图、建路径、边框）」，由预热在收尾时汇总打一条 W27。
     * 只做累加，成本是每帧 3 次 elapsedRealtime —— 相对 100ms 可以忽略。
     */
    private static final java.util.concurrent.atomic.AtomicLong N_FRAMES =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong N_TOTAL =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong N_SHADOW =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong N_CONTENT =
            new java.util.concurrent.atomic.AtomicLong();

    private static void note(long totalMs, long shadowMs, long contentMs) {
        N_FRAMES.incrementAndGet();
        N_TOTAL.addAndGet(totalMs);
        N_SHADOW.addAndGet(shadowMs);
        N_CONTENT.addAndGet(contentMs);
    }

    /** 取走并清零（谁取谁打日志）。没有样本时返回空串。 */
    public static String takeStats() {
        long n = N_FRAMES.getAndSet(0L);
        long total = N_TOTAL.getAndSet(0L);
        long shadow = N_SHADOW.getAndSet(0L);
        long content = N_CONTENT.getAndSet(0L);
        if (n <= 0L) {
            return "";
        }
        long other = Math.max(0L, total - shadow - content);
        return "帧=" + n
                + " 合计均=" + (total / n) + "ms"
                + " 阴影均=" + (shadow / n) + "ms"
                + " 内容均=" + (content / n) + "ms"
                + " 其它均=" + (other / n) + "ms";
    }

    @NotNull
    public static final FrameComposer INSTANCE = new FrameComposer();
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

    @NotNull
    public final Bitmap compose(@NotNull Bitmap source, int width, int height, @NotNull LayoutMode layout, @NotNull ShapeKind shape, int alphaPercent, boolean border, int scalePercent, int rotation, int shapeRotation, boolean fitContent, int shadowPercent, @Nullable String shadowCurve, float offsetXPercent, float offsetYPercent, int contentScalePercent, float maskOffsetXPercent, float maskOffsetYPercent) {
        Intrinsics.checkNotNullParameter(source, "source");
        Intrinsics.checkNotNullParameter(layout, "layout");
        Intrinsics.checkNotNullParameter(shape, "shape");
        int safeWidth = RangesKt.coerceIn(width, 1, 4096);
        int safeHeight = RangesKt.coerceIn(height, 1, 4096);
        long _t0 = android.os.SystemClock.elapsedRealtime();
        Bitmap out = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888);
        Intrinsics.checkNotNullExpressionValue(out, "createBitmap(...)");
        Paint $this$compose_u24lambda_u240 = new Paint(3);
        $this$compose_u24lambda_u240.setAlpha(RangesKt.coerceIn((int) ((255.0f * RangesKt.coerceIn(alphaPercent, 0, 100)) / 100.0f), 0, 255));
        // v1.8.6：有阴影时先量一下素材的“不透明包围盒”，让阴影贴着画面而不是透明留白。
        long[] acc = new long[2];
        drawFrame(new Canvas(out), safeWidth, safeHeight, source.getWidth(), source.getHeight(), layout, shape, border, scalePercent, rotation, shapeRotation, fitContent, shadowPercent, shadowCurve, alphaPercent, offsetXPercent, offsetYPercent, contentScalePercent, maskOffsetXPercent, maskOffsetYPercent, source, new AnonymousClass1(source, $this$compose_u24lambda_u240), acc);
        note(android.os.SystemClock.elapsedRealtime() - _t0, acc[0], acc[1]);
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

    public final void drawFrame(@NotNull Canvas canvas, int width, int height, int sourceWidth, int sourceHeight, @NotNull LayoutMode layout, @NotNull ShapeKind shape, boolean border, int scalePercent, int rotation, int shapeRotation, boolean fitContent, int shadowPercent, @Nullable String shadowCurve, int alphaPercent, float offsetXPercent, float offsetYPercent, int contentScalePercent, float maskOffsetXPercent, float maskOffsetYPercent, @Nullable Bitmap alphaSource, @NotNull Function2<? super Canvas, ? super RectF, Unit> function2) {
        drawFrame(canvas, width, height, sourceWidth, sourceHeight, layout, shape, border, scalePercent, rotation, shapeRotation, fitContent, shadowPercent, shadowCurve, alphaPercent, offsetXPercent, offsetYPercent, contentScalePercent, maskOffsetXPercent, maskOffsetYPercent, alphaSource, function2, null);
    }

    /** acc != null 时：acc[0] 累加阴影耗时、acc[1] 累加内容（clip+drawContent）耗时。 */
    public final void drawFrame(@NotNull Canvas canvas, int width, int height, int sourceWidth, int sourceHeight, @NotNull LayoutMode layout, @NotNull ShapeKind shape, boolean border, int scalePercent, int rotation, int shapeRotation, boolean fitContent, int shadowPercent, @Nullable String shadowCurve, int alphaPercent, float offsetXPercent, float offsetYPercent, int contentScalePercent, float maskOffsetXPercent, float maskOffsetYPercent, @Nullable Bitmap alphaSource, @NotNull Function2<? super Canvas, ? super RectF, Unit> function2, @Nullable long[] acc) {
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
        // v1.10.233：阴影 = 参考实现那条**单层高斯尾**（按外部复核结论重写）。
        //
        //   α(d) = A·Φ((δ − d)/σ),   A = (强度+20)/255
        //   σ = L/(ρ + z),  z = −Φ⁻¹(1/(255A)),  δ = ρσ
        //   留白（画面每边缩）= L  —— 因果：阴影看得见多远，画面就缩多少
        //
        // 新参数：强度（0~100）、范围 L（1%~8% 画面框宽，默认 5%）、紧实度 ρ（−0.5~1.5，默认 0.55）。
        // 旧的「扩散 / 模糊半径 / 收边」三个量已删除：它们能凑出"带肩 + 断崖"，
        // 而参考实机没有肩也没有断崖（复核：可见宽 5.0%、半高点 32%、贴边 0.70A）。
        int strength = RangesKt.coerceIn(shadowPercent, 0, ShadowSpec.STRENGTH_MAX);
        ShadowSpec spec = ShadowSpec.parse(shadowCurve);
        int shadowAlpha = strength <= 0 ? 0 : ShadowSpec.effectiveAlpha(strength, alpha);
        // 留白基准 = 画面框（LESSONS 21：按画布算会让阴影比画面大一圈）
        float rangePx = spec.rangePx(area.width());
        float sigmaPx = ShadowSpec.sigmaPx(rangePx, spec.rho(), shadowAlpha > 0 ? shadowAlpha : 1);
        float deltaPx = ShadowSpec.deltaPx(sigmaPx, spec.rho());
        float blurPx = ShadowSpec.blurRadiusPx(sigmaPx);
        float need = shadowAlpha > 0 ? rangePx : 0.0f;
        // v1.10.533：**画面框 = 阴影留白内缩之后的框** —— 这一段抽成 drawBox()，
        // 因为预览虚线也要贴到同一个框（画面每边缩多少，虚线就得缩多少，否则阴影一开又不贴了）。
        RectF box = drawBox(area, w, h, alphaPercent, shadowPercent, shadowCurve);
        Path path = ShapeFactory.INSTANCE.path(shape, box.width(), box.height());
        path.offset(box.left, box.top);
        if (shapeRotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(shapeRotation, w / 2.0f, h / 2.0f);
            path.transform(matrix);
        }
        // v1.8.1：内容适配用的蒙版路径（不含位置偏移）。蒙版是蒙版、画面是画面，
        // 「调整蒙版位置」只挪窗口，绝不带着画面一起走。
        Path contentPath = new Path(path);
        float maskDx = (box.width() * RangesKt.coerceIn(maskOffsetXPercent, -50.0f, 50.0f)) / 100.0f;
        float maskDy = (box.height() * RangesKt.coerceIn(maskOffsetYPercent, -50.0f, 50.0f)) / 100.0f;
        if (maskDx != 0.0f || maskDy != 0.0f) {
            Matrix shift = new Matrix();
            shift.postTranslate(maskDx, maskDy);
            path.transform(shift);
        }
        // v1.10.533：判定抽成 fitsContent()，因为「画面矩形」也要用同一条判据算
        // （虚线框贴的是画面，画面多少全看这里，不能再写成两份）。
        boolean fit = fitsContent(layout, fitContent);
        long _tShadow = acc != null ? android.os.SystemClock.elapsedRealtime() : 0L;
        // 贴一块烘好的阴影（每帧只有 1 次 drawBitmap）。
        float room = Math.min(Math.min(box.left, w - box.right), Math.min(box.top, h - box.bottom));
        int padPx = (int) Math.ceil(need) + 1;      // +1px 保险：截断点一定落在位图外面
        ShadowLayer layer = null;
        if (shadowAlpha > 0 && need > 0.0f && room >= need - 0.5f) {
            // 轮廓平移到画面框左上角为原点：阴影位图只跟形状/尺寸有关，跟组件在桌面上的位置无关
            Path local = new Path(path);
            local.offset(-box.left, -box.top);
            layer = ShadowLayer.obtain(
                    local,
                    (int) Math.ceil(box.width()),
                    (int) Math.ceil(box.height()),
                    deltaPx,
                    blurPx,
                    padPx,
                    shape.ordinal(),
                    shapeRotation,
                    Math.round(maskDx * 8.0f),
                    Math.round(maskDy * 8.0f));
        }
        if (layer != null && shadowAlpha > 0) {
            Bitmap shadow = layer.bitmap();
            if (!shadow.isRecycled()) {
                Paint shadowPaint = new Paint();
                shadowPaint.setAlpha(shadowAlpha);
                canvas.drawBitmap(shadow, Math.round(box.left) - layer.pad(), Math.round(box.top) - layer.pad(), shadowPaint);
            }
            // 挖掉画面内部：整体透明度 < 100 时不让阴影从半透明画面底下透出来
            // （v1.10.72 起的行为，保留 —— 参考实现的画面永远不透明，没这个问题）
            Paint cut = new Paint(1);
            cut.setStyle(Paint.Style.FILL);
            cut.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            Path cutPath = insetPath(path, SHADOW_CUT_INSET);
            canvas.drawPath(cutPath != null ? cutPath : path, cut);
        }
        if (acc != null) {
            long now = android.os.SystemClock.elapsedRealtime();
            acc[0] += now - _tShadow;
            _tShadow = now;
        }
        canvas.save();
        canvas.clipRect(area);
        canvas.clipPath(path);
        // 内容按"蒙版实际占的区域"适配：固定比例的蒙版（1:1 / 4:3 / 16:9 …）比布局框小，
        // 之前把内容铺满布局框再被蒙版裁掉，看起来就是"上下被截掉"。
        RectF drawBox = new RectF(maskBounds(contentPath, box));
        drawBox.inset(-2.0f, -2.0f);
        drawContent(canvas, drawBox, srcWidth, srcHeight, rotation, fit, RangesKt.coerceIn(offsetXPercent, -50.0f, 50.0f), RangesKt.coerceIn(offsetYPercent, -50.0f, 50.0f), RangesKt.coerceIn(contentScalePercent, 0, 300) / 100.0f, function2);
        canvas.restore();
        if (acc != null) {
            long now = android.os.SystemClock.elapsedRealtime();
            acc[1] += now - _tShadow;
        }
        if (border) {
            Paint $this$drawFrame_u24lambda_u241 = new Paint(1);
            $this$drawFrame_u24lambda_u241.setStyle(Paint.Style.STROKE);
            float sw = RangesKt.coerceAtLeast(Math.min(safeWidth, safeHeight) * 0.018f, 2.0f);
            $this$drawFrame_u24lambda_u241.setStrokeWidth(sw);
            $this$drawFrame_u24lambda_u241.setColor(Color.argb((200 * alpha) / 100, 255, 255, 255));
            // v1.10.295（用户："拉到正常尺寸也会出现细微问题"，放大后能看到边缘一条深浅不匀的细边）：
            // 卡片铺满画布时，描边是**骑在画布边界上**画的 —— 外侧一半被画布裁掉，
            // 于是上/左各剩半条、下/右可能整条，圆角处还会错位，肉眼看就是"边缘不干净"。
            // 现在描边单独用**内缩半个线宽**的路径画，整条线都落在画布内，圆角处也均匀。
            Path strokePath = path;
            try {
                RectF inner = insetPreservingRatio(box, sw * 0.5f);
                if (inner.width() > 8.0f && inner.height() > 8.0f) {
                    strokePath = ShapeFactory.INSTANCE.path(shape, inner.width(), inner.height());
                    strokePath.offset(inner.left, inner.top);
                }
            } catch (Throwable ignored) {
            }
            canvas.drawPath(strokePath, $this$drawFrame_u24lambda_u241);
        }
        canvas.restore();
    }



    /**
     * v1.10.533：**画面框** —— 画面框 area 按阴影留白内缩之后的框。
     *
     * <p>{@code drawFrame} 与 {@link #pictureRect} 共用这一份：阴影一开，画面每边都要缩，
     * 虚线框若还贴在 area 上就贴不住画面。
     */
    @NotNull
    private static RectF drawBox(@NotNull RectF area, float canvasWidth, float canvasHeight,
                                 int alphaPercent, int shadowPercent, @Nullable String shadowCurve) {
        int strength = RangesKt.coerceIn(shadowPercent, 0, ShadowSpec.STRENGTH_MAX);
        ShadowSpec spec = ShadowSpec.parse(shadowCurve);
        int alpha = RangesKt.coerceIn(alphaPercent, 0, 100);
        int shadowAlpha = strength <= 0 ? 0 : ShadowSpec.effectiveAlpha(strength, alpha);
        // 留白基准 = 画面框（LESSONS 21：按画布算会让阴影比画面大一圈）
        float rangePx = spec.rangePx(area.width());
        float need = shadowAlpha > 0 ? rangePx : 0.0f;
        // v1.10.216（保留）：**先借画布的空白边距**。画面框 area 往往比画布小（原始比例布局的
        // 上下留白、固定比例布局的左右留白），那圈空白本来就是透明的，阴影画进去不占画面一点地方。
        // 只有空白不够（画布比例 = 画面比例）时，画面才按「差额」内缩。
        float free = Math.max(0.0f, Math.min(Math.min(area.left, canvasWidth - area.right),
                Math.min(area.top, canvasHeight - area.bottom)));
        float margin = Math.max(0.0f, need - free);
        RectF inset = insetPreservingRatio(area, margin);
        boolean hasInset = inset.width() > 8.0f && inset.height() > 8.0f;
        // v1.10.280：**撤销 v1.10.279 的衬底**。它把同一帧缩到 1/8 再放大铺在画面四周，
        // 而素材（GIF/贴纸）是**透明底**的 —— 柔化副本从透明区透出来，就变成沿轮廓的一圈灰雾
        // 透明素材上不存在"填充"这回事。
        RectF box = hasInset ? inset : area;
        if (box.width() < 2.0f || box.height() < 2.0f || box.isEmpty()) {
            box = new RectF(area);
        }
        return box;
    }

    /**
     * 绕自身中心转 rotation 度之后的包围盒（旋转分支画的是转过的矩形，虚线要包住它）。
     *
     * <p>v1.10.533：这里**不用 android.graphics.Matrix** —— 它在 android-all.jar 里是 native，
     * 脱机门禁跑不起来（`UnsatisfiedLinkError: nCreate`）。四个角自己算，纯三角函数，
     * 门禁能直接验。
     */
    @NotNull
    private static RectF rotatedBounds(@NotNull RectF rect, int rotation) {
        double radians = Math.toRadians(rotation);
        float cosValue = (float) Math.cos(radians);
        float sinValue = (float) Math.sin(radians);
        float cx = rect.centerX();
        float cy = rect.centerY();
        float[] xs = {rect.left, rect.right, rect.left, rect.right};
        float[] ys = {rect.top, rect.top, rect.bottom, rect.bottom};
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float dx = xs[i] - cx;
            float dy = ys[i] - cy;
            float px = cx + (dx * cosValue) - (dy * sinValue);
            float py = cy + (dx * sinValue) + (dy * cosValue);
            minX = Math.min(minX, px);
            minY = Math.min(minY, py);
            maxX = Math.max(maxX, px);
            maxY = Math.max(maxY, py);
        }
        return new RectF(minX, minY, maxX, maxY);
    }

    /** 蒙版路径的包围盒（和布局框取交集）；退化时回退到布局框。 */
    private static RectF maskBounds(@NotNull Path path, @NotNull RectF box) {
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);
        if (bounds.width() < 4.0f || bounds.height() < 4.0f) {
            return new RectF(box);
        }
        return bounds;
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

    /**
     * v1.10.533：**画面被画到哪一块**（画布坐标，未旋转；旋转由调用方 canvas.rotate 施加）。
     *
     * <p>抽出来是为了让 {@code drawContent} 与 {@link #pictureRect} 用同一份算术 ——
     * 预览虚线要贴的就是这块矩形，两边各写一份就一定会有一次"贴不住"。
     */
    @NotNull
    private static RectF contentDrawRect(@NotNull RectF area, int sourceWidth, int sourceHeight,
                                         int rotation, boolean fit, float offsetXPercent,
                                         float offsetYPercent, float contentScale) {
        int sw = RangesKt.coerceAtLeast(sourceWidth, 1);
        int sh = RangesKt.coerceAtLeast(sourceHeight, 1);
        float dx = (area.width() * offsetXPercent) / 100.0f;
        float dy = (area.height() * offsetYPercent) / 100.0f;
        if (rotation != 0) {
            double radians = Math.toRadians(rotation);
            float cosValue = (float) Math.abs(Math.cos(radians));
            float sinValue = (float) Math.abs(Math.sin(radians));
            float rotatedWidth = (sw * cosValue) + (sh * sinValue);
            float rotatedHeight = (sw * sinValue) + (sh * cosValue);
            float scale = Math.max(area.width() / rotatedWidth, area.height() / rotatedHeight);
            float drawWidth = sw * scale;
            float drawHeight = sh * scale;
            float centerX = area.centerX() + dx;
            float centerY = area.centerY() + dy;
            return new RectF(centerX - (drawWidth / 2.0f), centerY - (drawHeight / 2.0f),
                    centerX + (drawWidth / 2.0f), centerY + (drawHeight / 2.0f));
        }
        float scale = (fit ? Math.min(area.width() / sw, area.height() / sh)
                : Math.max(area.width() / sw, area.height() / sh)) * contentScale;
        float drawWidth = sw * scale;
        float drawHeight = sh * scale;
        float left = (area.centerX() + dx) - (drawWidth / 2.0f);
        float top = (area.centerY() + dy) - (drawHeight / 2.0f);
        return new RectF(left, top, left + drawWidth, top + drawHeight);
    }

    private final void drawContent(Canvas canvas, RectF area, int sourceWidth, int sourceHeight, int rotation, boolean fit, float offsetXPercent, float offsetYPercent, float contentScale, Function2<? super Canvas, ? super RectF, Unit> function2) {
        RectF dest = contentDrawRect(area, sourceWidth, sourceHeight, rotation, fit,
                offsetXPercent, offsetYPercent, contentScale);
        if (rotation != 0) {
            // 旋转：以 (area.center + 位移) 为轴心转，画的是未旋转的那块矩形。
            // v1.10.537：**这里必须和 cover 分支一样裁到画面框**。
            // 原来这条分支没有 clipRect，放大后再旋转，四个角会铺到框外（阴影留白里也照铺）。
            canvas.save();
            canvas.clipRect(area);
            canvas.rotate(rotation, dest.centerX(), dest.centerY());
            function2.invoke(canvas, dest);
            canvas.restore();
            return;
        }
        if (fit) {
            function2.invoke(canvas, dest);
            return;
        }
        canvas.save();
        canvas.clipRect(area);
        function2.invoke(canvas, dest);
        canvas.restore();
    }

    @NotNull
    public final RectF contentRect(int sourceWidth, int sourceHeight, int width, int height, @NotNull LayoutMode layout) {
        Intrinsics.checkNotNullParameter(layout, "layout");
        if (layout == LayoutMode.FIT) {
            // v1.10.285（用户验收标准，逐字）："**无论布局蒙版，无论桌面组件宽度，画面必须显示全
            // （填充除外），不能出现丁点画面截断**"。
            //
            // 所以原始比例 = 完整内接：画布比例=格子比例（桌面 centerCrop 恒等，不二次裁），
            // 画面在画布内等比内接、居中，一格像素都不切；多出来的方向是**透明**（露壁纸），
            // 这是"必须显示全"的唯一几何解。
            // 想要铺满裁边 → 用「填充」。两个语义不再互相偷。
            // v1.10.298：A 方案生效（桌面恒 fitCenter，永不裁切）→ 撤销 v1.10.297 的 2% 安全边。
            // 画面恢复满尺寸，四周不再有那圈白留边。
            return autoContentRect(sourceWidth, sourceHeight, width, height);        }
        if (layout.getRatio() <= 0.0f) {
            return new RectF(0.0f, 0.0f, width, height);
        }
        float widgetRatio = width / (float) RangesKt.coerceAtLeast(height, 1);
        if (widgetRatio > layout.getRatio()) {
            float contentWidth = height * layout.getRatio();
            float left = (width - contentWidth) / 2.0f;
            return new RectF(left, 0.0f, left + contentWidth, height);
        }
        float contentHeight = width / layout.getRatio();
        float top = (height - contentHeight) / 2.0f;
        return new RectF(0.0f, top, width, top + contentHeight);
    }

    /**
     * v1.10.533：**画面是不是"内接"**（true = fit，完整显示；false = cover，裁切填满）。
     *
     * <p>唯一判据来源：{@code drawFrame} 与 {@link #pictureRect} 都调它，不许再各写一份。
     */
    public static boolean fitsContent(@NotNull LayoutMode layout, boolean fitContent) {
        Intrinsics.checkNotNullParameter(layout, "layout");
        if (layout == LayoutMode.FIT) {
            // 原始比例：画布比例 = 素材比例（previewRatio 决定的），内接即等于铺满 → 不裁切也贴满。
            return true;
        }
        // v1.10.538：固定比例的「适配画面」保持可点。
        // 固定比例由开关决定：
        //   开 = 内接（完整显示，不裁切）→ 画面必然在两侧留白，虚线框仍是布局框、不追画面；
        //   关 = 裁切填满 → 画面填满布局框，四周正好 1dp（要"四周统一"就选这一档）。
        return fitContent;
    }

    /** 默认参数（无旋转 / 缩放 / 位移 / 阴影）的重载，见下面的完整版。 */
    @NotNull
    public final RectF pictureRect(int sourceWidth, int sourceHeight, int width, int height,
                                   @NotNull LayoutMode layout, boolean fitContent) {
        return pictureRect(sourceWidth, sourceHeight, width, height, layout, fitContent,
                0, 100, 0.0f, 0.0f, 100, 0, null);
    }

    /**
     * **画面矩形** —— 画面在一张 W×H 的画布里到底占哪一块（画布坐标）。
     *
     * <p>虚线框**不贴**这个矩形：虚线 = 布局框（画布），只随布局变；拖动 / 缩放 / 旋转画面时框不动。
     * 这个函数用于**核对「画面有没有超出框」**（脱机门禁 preview-frame-check.sh），
     * 以及需要"画面占比"一类几何的地方。
     *
     * <p>整条链与 {@code drawFrame} 同源，顺序也一致：
     * <pre>
     *   area  = contentRect()          // 画面框（布局比例决定的一块）
     *   box   = drawBox()              // 阴影留白内缩（阴影一开画面就缩）
     *   dest  = contentDrawRect()      // 内接/铺满 + 内容缩放 + 位移 + 旋转
     *   vis   = dest ∩ 画布            // 位图边界就是画布的裁切线
     *   vis   = vis ∩ box              // 铺满分支里 drawContent 还会 clipRect(box)
     * </pre>
     *
     * <p>形状路径（{@code maskBounds}）与那 2px 出血不计入：那是形状自身轮廓与抗锯齿的量级，
     * 计入反而会和"画面框"这个词打架（蒙版是蒙版、画面是画面，见 drawFrame 里那段注释）。
     */
    @NotNull
    public final RectF pictureRect(int sourceWidth, int sourceHeight, int width, int height,
                                   @NotNull LayoutMode layout, boolean fitContent,
                                   int rotation, int contentScalePercent,
                                   float offsetXPercent, float offsetYPercent,
                                   int alphaPercent, int shadowPercent, @Nullable String shadowCurve) {
        Intrinsics.checkNotNullParameter(layout, "layout");
        int sw = RangesKt.coerceAtLeast(sourceWidth, 1);
        int sh = RangesKt.coerceAtLeast(sourceHeight, 1);
        int w = RangesKt.coerceAtLeast(width, 1);
        int h = RangesKt.coerceAtLeast(height, 1);
        RectF area = contentRect(sw, sh, w, h, layout);
        if (area.width() < 2.0f || area.height() < 2.0f || area.isEmpty()) {
            area = new RectF(0.0f, 0.0f, w, h);
        }
        RectF box = drawBox(area, w, h, alphaPercent, shadowPercent, shadowCurve);
        boolean fit = fitsContent(layout, fitContent);
        RectF dest = contentDrawRect(box, sw, sh, rotation, fit,
                RangesKt.coerceIn(offsetXPercent, -50.0f, 50.0f),
                RangesKt.coerceIn(offsetYPercent, -50.0f, 50.0f),
                RangesKt.coerceIn(contentScalePercent, 0, 300) / 100.0f);
        if (rotation != 0) {
            dest = rotatedBounds(dest, rotation);
        }
        RectF visible = new RectF(dest);
        boolean hit = visible.intersect(new RectF(0.0f, 0.0f, w, h));
        if (hit && !fit && rotation == 0) {
            hit = visible.intersect(box);
        }
        if (!hit || visible.width() < 2.0f || visible.height() < 2.0f || visible.isEmpty()) {
            // 退化（画面被位移/缩放推到画布外）：退回画面框，绝不返回空矩形（否则虚线消失）
            return new RectF(area);
        }
        return visible;
    }

    private final RectF autoContentRect(int sourceWidth, int sourceHeight, int width, int height) {
        float contentHeight;
        float contentWidth;
        // v1.8.3：两个 int 相除会得到 0（竖图必现），contentWidth 随之为 0，
        // 退化兜底后整块画布被当成内容区 —— 「原始比例」实际按「填充」裁切了。
        float srcRatio = sourceWidth / (float) RangesKt.coerceAtLeast(sourceHeight, 1);
        float areaRatio = width / (float) RangesKt.coerceAtLeast(height, 1);
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
