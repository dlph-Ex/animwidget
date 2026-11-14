package io.github.animwidget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import java.util.List;
import kotlin.Metadata;
import kotlin.collections.ArraysKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;
import org.jetbrains.annotations.NotNull;

/* compiled from: ShapeFactory.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��B\n\u0002\u0018\u0002\n\u0002\u0010��\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0010\u0007\n\u0002\b\u0003\n\u0002\u0010\u000b\n��\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u000e\bÆ\u0002\u0018��2\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J0\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\r2\u0006\u0010\u000f\u001a\u00020\r2\u0006\u0010\u0010\u001a\u00020\u0011H\u0002J&\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00132\u0006\u0010\u0015\u001a\u00020\u00052\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u0011J\u001e\u0010\n\u001a\u00020\u000b2\u0006\u0010\u0015\u001a\u00020\u00052\u0006\u0010\u0019\u001a\u00020\r2\u0006\u0010\u001a\u001a\u00020\rJ8\u0010\u001b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\u001c\u001a\u00020\r2\u0006\u0010\u001d\u001a\u00020\r2\u0006\u0010\u001e\u001a\u00020\r2\u0006\u0010\u001f\u001a\u00020\u00172\u0006\u0010 \u001a\u00020\rH\u0002J8\u0010!\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\u001c\u001a\u00020\r2\u0006\u0010\u001d\u001a\u00020\r2\u0006\u0010\"\u001a\u00020\r2\u0006\u0010#\u001a\u00020\r2\u0006\u0010$\u001a\u00020\u0017H\u0002J \u0010%\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\rH\u0002R\u0017\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004¢\u0006\b\n��\u001a\u0004\b\u0006\u0010\u0007"}, d2 = {"Lio/github/animwidget/ShapeFactory;", "", "()V", "all", "", "Lio/github/animwidget/ShapeKind;", "getAll", "()Ljava/util/List;", "addFixedRatio", "", "path", "Landroid/graphics/Path;", "w", "", "h", "ratio", "rounded", "", "apply", "Landroid/graphics/Bitmap;", "content", "shape", "alphaPercent", "", "border", "width", "height", "polygon", "cx", "cy", "radius", "sides", "rotation", "star", "outer", "inner", "points", "stretchToBox"})
/* loaded from: classes.jar:io/github/animwidget/ShapeFactory.class */
public final class ShapeFactory {

    @NotNull
    public static final ShapeFactory INSTANCE = new ShapeFactory();

    @NotNull
    private static final List<ShapeKind> all = ArraysKt.toList(ShapeKind.values());

    /* compiled from: ShapeFactory.kt */
    @Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    /* loaded from: classes.jar:io/github/animwidget/ShapeFactory$WhenMappings.class */
    public static /* synthetic */ class WhenMappings {
        public static final /* synthetic */ int[] $EnumSwitchMapping$0;

        static {
            int[] iArr = new int[ShapeKind.values().length];
            try {
                iArr[ShapeKind.RECT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[ShapeKind.ROUNDED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[ShapeKind.SQUIRCLE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[ShapeKind.CIRCLE.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr[ShapeKind.ELLIPSE.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                iArr[ShapeKind.CAPSULE.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                iArr[ShapeKind.DIAMOND.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                iArr[ShapeKind.TRIANGLE.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                iArr[ShapeKind.HEXAGON.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                iArr[ShapeKind.OCTAGON.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                iArr[ShapeKind.STAR.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                iArr[ShapeKind.STAR6.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                iArr[ShapeKind.STAR8.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
            try {
                iArr[ShapeKind.LEAF.ordinal()] = 14;
            } catch (NoSuchFieldError e14) {
            }
            try {
                iArr[ShapeKind.PETAL.ordinal()] = 15;
            } catch (NoSuchFieldError e15) {
            }
            try {
                iArr[ShapeKind.CLOVER.ordinal()] = 16;
            } catch (NoSuchFieldError e16) {
            }
            try {
                iArr[ShapeKind.HEART.ordinal()] = 17;
            } catch (NoSuchFieldError e17) {
            }
            try {
                iArr[ShapeKind.TEARDROP.ordinal()] = 18;
            } catch (NoSuchFieldError e18) {
            }
            try {
                iArr[ShapeKind.SHIELD.ordinal()] = 19;
            } catch (NoSuchFieldError e19) {
            }
            try {
                iArr[ShapeKind.CROSS.ordinal()] = 20;
            } catch (NoSuchFieldError e20) {
            }
            try {
                iArr[ShapeKind.WAVE.ordinal()] = 21;
            } catch (NoSuchFieldError e21) {
            }
            try {
                iArr[ShapeKind.BOOKMARK.ordinal()] = 22;
            } catch (NoSuchFieldError e22) {
            }
            try {
                iArr[ShapeKind.MESSAGE.ordinal()] = 23;
            } catch (NoSuchFieldError e23) {
            }
            try {
                iArr[ShapeKind.SQUARE_1_1.ordinal()] = 24;
            } catch (NoSuchFieldError e24) {
            }
            try {
                iArr[ShapeKind.RECT_4_3.ordinal()] = 25;
            } catch (NoSuchFieldError e25) {
            }
            try {
                iArr[ShapeKind.RECT_3_4.ordinal()] = 26;
            } catch (NoSuchFieldError e26) {
            }
            try {
                iArr[ShapeKind.RECT_16_9.ordinal()] = 27;
            } catch (NoSuchFieldError e27) {
            }
            try {
                iArr[ShapeKind.RECT_9_16.ordinal()] = 28;
            } catch (NoSuchFieldError e28) {
            }
            try {
                iArr[ShapeKind.RECT_3_2.ordinal()] = 29;
            } catch (NoSuchFieldError e29) {
            }
            try {
                iArr[ShapeKind.RECT_2_3.ordinal()] = 30;
            } catch (NoSuchFieldError e30) {
            }
            try {
                iArr[ShapeKind.RECT_21_9.ordinal()] = 31;
            } catch (NoSuchFieldError e31) {
            }
            try {
                iArr[ShapeKind.RECT_2_1.ordinal()] = 32;
            } catch (NoSuchFieldError e32) {
            }
            try {
                iArr[ShapeKind.ROUND_1_1.ordinal()] = 33;
            } catch (NoSuchFieldError e33) {
            }
            try {
                iArr[ShapeKind.ROUND_4_3.ordinal()] = 34;
            } catch (NoSuchFieldError e34) {
            }
            try {
                iArr[ShapeKind.ROUND_3_4.ordinal()] = 35;
            } catch (NoSuchFieldError e35) {
            }
            try {
                iArr[ShapeKind.BLOB.ordinal()] = 36;
            } catch (NoSuchFieldError e36) {
            }
            $EnumSwitchMapping$0 = iArr;
        }
    }

    private ShapeFactory() {
    }

    @NotNull
    public final List<ShapeKind> getAll() {
        return all;
    }

    /**
     * 蒙版 / 画面用的路径：**永远铺满整个框**。
     *
     * <p>v1.10.213 回滚：v1.10.211 让「有自己比例」的形状（星/心/眼/云…）按自身比例居中放进
     * 布局框，结果在「原始比例」布局里，非正方形的画面框被一个内接正方形顶替 ——
     * 画面直接缩掉一大半（用户报的「选某些蒙版画面就回缩」）。蒙版的第一职责是**占满画面框**，
     * 所以这里一律铺满；缩略图那点"形状好不好看"交给 {@link #pathFitted}。</p>
     *
     * <p>固定比例形状（1:1 / 16:9 …）例外：它们本来就是"在框里居中放一个该比例的窗口"。</p>
     */
    @NotNull
    public final Path path(@NotNull ShapeKind shape, float width, float height) {
        Intrinsics.checkNotNullParameter(shape, "shape");
        float w = RangesKt.coerceAtLeast(width, 1.0f);
        float h = RangesKt.coerceAtLeast(height, 1.0f);
        Path path = new Path();
        if (!buildGlyph(path, shape, w, h)) {
            buildLegacy(path, shape, w, h);
        }
        if (shape.getAspect() <= 0.0f) {
            stretchToBox(path, w, h);
        }
        return path;
    }

    /**
     * 缩略图（形状格子里那个小图）专用的路径：按形状自己的设计比例居中放进框里。
     *
     * <p>这一步**只影响缩略图**：眼睛(1.7)、云朵(1.5)、闪电(0.62) 这类形状如果按方格子
     * 强行拉伸，图标会认不出；蒙版仍然走 {@link #path} 铺满画面框，两者互不影响。</p>
     */
    @NotNull
    public final Path pathFitted(@NotNull ShapeKind shape, float width, float height) {
        Intrinsics.checkNotNullParameter(shape, "shape");
        float W = RangesKt.coerceAtLeast(width, 1.0f);
        float H = RangesKt.coerceAtLeast(height, 1.0f);
        RectF box = designBox(shape, W, H);
        float w = box.width();
        float h = box.height();
        Path path = new Path();
        if (!buildGlyph(path, shape, w, h)) {
            buildLegacy(path, shape, w, h);
        }
        if (shape.getAspect() <= 0.0f) {
            stretchToBox(path, w, h);
        }
        if (box.left != 0.0f || box.top != 0.0f) {
            path.offset(box.left, box.top);
        }
        return path;
    }

    /**
     * v1.10.211 新增的 12 个自绘异形（v1.10.213：只在这里画几何，框由调用方给）。
     *
     * <p>一律在 **0..1 单位框**里描述（y 向下），最后用 setRectToRect(FILL) 映射到目标框：
     * 单位框就是这个形状的「标准姿势」。设计约束（由 scripts/shape-check.py 复算把关）：</p>
     * <ul>
     *   <li>包围盒必须正好是 0..1 × 0..1 —— 铺满框时不需要额外拉伸；</li>
     *   <li>只用闭合路径，不用 DST_OUT / 描边（蒙版必须是实心闭合区域）；</li>
     *   <li>需要"曲线顶点落在框边"时，顶点放在**在线点**上（控制点不算数）。</li>
     * </ul>
     *
     * @return true = 这个形状由本方法负责；false = 交给 buildLegacy
     */
    private final boolean buildGlyph(Path path, ShapeKind shape, float w, float h) {
        switch (shape) {
            case MOON:   // 月牙：外弧贴满上/下/右，内弧向右收，开口朝右
                path.moveTo(0.00f, 0.00f);
                path.cubicTo(0.62f, 0.03f, 1.00f, 0.25f, 1.00f, 0.50f);
                path.cubicTo(1.00f, 0.75f, 0.62f, 0.97f, 0.00f, 1.00f);
                path.cubicTo(0.38f, 0.85f, 0.55f, 0.69f, 0.55f, 0.50f);
                path.cubicTo(0.55f, 0.31f, 0.38f, 0.15f, 0.00f, 0.00f);
                path.close();
                break;
            case CLOUD: { // 云朵：底座矩形 + 三个圆凸包的并集（圆映射后自然变扁椭圆，像真云）
                Path base = new Path();
                base.addRect(new RectF(0.00f, 0.62f, 1.00f, 1.00f), Path.Direction.CW);
                path.set(base);
                float[][] bumps = {{0.22f, 0.60f, 0.22f}, {0.50f, 0.34f, 0.34f}, {0.78f, 0.60f, 0.22f}};
                for (float[] bump : bumps) {
                    Path circle = new Path();
                    circle.addCircle(bump[0], bump[1], bump[2], Path.Direction.CW);
                    path.op(circle, Path.Op.UNION);
                }
                break;
            }
            case BOLT:   // 闪电：折线闪电，上下都顶到框
                path.moveTo(0.72f, 0.00f);
                path.lineTo(0.00f, 0.62f);
                path.lineTo(0.40f, 0.62f);
                path.lineTo(0.22f, 1.00f);
                path.lineTo(1.00f, 0.34f);
                path.lineTo(0.58f, 0.34f);
                path.close();
                break;
            case ARCH:   // 拱门：上方半圆拱，下面直腿
                path.moveTo(0.00f, 1.00f);
                path.lineTo(0.00f, 0.46f);
                path.cubicTo(0.00f, 0.14f, 0.22f, 0.00f, 0.50f, 0.00f);
                path.cubicTo(0.78f, 0.00f, 1.00f, 0.14f, 1.00f, 0.46f);
                path.lineTo(1.00f, 1.00f);
                path.close();
                break;
            case EYE:    // 眼睛：两端收成尖角（控制点贴住尖角，切线近似垂直 → 出尖）
                path.moveTo(0.00f, 0.50f);
                path.cubicTo(0.02f, 0.28f, 0.22f, 0.00f, 0.50f, 0.00f);
                path.cubicTo(0.78f, 0.00f, 0.98f, 0.28f, 1.00f, 0.50f);
                path.cubicTo(0.98f, 0.72f, 0.78f, 1.00f, 0.50f, 1.00f);
                path.cubicTo(0.22f, 1.00f, 0.02f, 0.72f, 0.00f, 0.50f);
                path.close();
                break;
            case GEM:    // 宝石：平顶 + 两侧切角 + 尖底
                path.moveTo(0.22f, 0.00f);
                path.lineTo(0.78f, 0.00f);
                path.lineTo(1.00f, 0.34f);
                path.lineTo(0.50f, 1.00f);
                path.lineTo(0.00f, 0.34f);
                path.close();
                break;
            case GEAR: { // 齿轮：中心圆盘 + 12 个梯形齿的并集
                Path core = new Path();
                core.addCircle(0.50f, 0.50f, 0.34f, Path.Direction.CW);
                path.set(core);
                int teeth = 12;   // 30° 一齿：0° 与 90° 都正好有齿 → 包围盒正好 0..1
                double step = Math.PI * 2.0d / teeth;
                for (int i = 0; i < teeth; i++) {
                    double a = step * i;
                    double half = step * 0.30d;
                    Path tooth = new Path();
                    tooth.moveTo(0.50f + (float) Math.cos(a - half) * 0.30f,
                                0.50f + (float) Math.sin(a - half) * 0.30f);
                    tooth.lineTo(0.50f + (float) Math.cos(a - half * 0.62d) * 0.50f,
                                 0.50f + (float) Math.sin(a - half * 0.62d) * 0.50f);
                    tooth.lineTo(0.50f + (float) Math.cos(a + half * 0.62d) * 0.50f,
                                 0.50f + (float) Math.sin(a + half * 0.62d) * 0.50f);
                    tooth.lineTo(0.50f + (float) Math.cos(a + half) * 0.30f,
                                 0.50f + (float) Math.sin(a + half) * 0.30f);
                    tooth.close();
                    path.op(tooth, Path.Op.UNION);
                }
                break;
            }
            case YINYANG:   // 阴阳：左半圆 + 上下两个小圆 = 太极鱼
                path.moveTo(0.50f, 0.00f);
                path.arcTo(new RectF(0.00f, 0.00f, 1.00f, 1.00f), -90.0f, -180.0f, false);
                path.close();
                Path up = new Path();
                up.addCircle(0.50f, 0.25f, 0.25f, Path.Direction.CW);
                path.op(up, Path.Op.UNION);
                Path down = new Path();
                down.addCircle(0.50f, 0.75f, 0.25f, Path.Direction.CW);
                path.op(down, Path.Op.UNION);
                break;
            case TICKET:   // 票券：圆角矩形，左右各咬掉一个半圆缺口
                Path body = new Path();
                body.addRoundRect(new RectF(0.00f, 0.00f, 1.00f, 1.00f), 0.10f, 0.10f, Path.Direction.CW);
                path.set(body);
                Path notchLeft = new Path();
                notchLeft.addCircle(0.00f, 0.50f, 0.12f, Path.Direction.CW);
                path.op(notchLeft, Path.Op.DIFFERENCE);
                Path notchRight = new Path();
                notchRight.addCircle(1.00f, 0.50f, 0.12f, Path.Direction.CW);
                path.op(notchRight, Path.Op.DIFFERENCE);
                break;
            case KEYHOLE:   // 钥匙孔：圆头 + 向下张开的梯形尾（尾底正好顶满宽度）
                path.addCircle(0.50f, 0.32f, 0.32f, Path.Direction.CW);
                Path stem = new Path();
                stem.moveTo(0.30f, 0.56f);
                stem.lineTo(0.70f, 0.56f);
                stem.lineTo(1.00f, 1.00f);
                stem.lineTo(0.00f, 1.00f);
                stem.close();
                path.op(stem, Path.Op.UNION);
                break;
            case SUNBURST: {   // 星芒：12 道细尖的太阳芒
                int points = 12;
                for (int i = 0; i < points * 2; i++) {
                    float radius = (i % 2 == 0) ? 0.50f : 0.22f;
                    double angle = (-Math.PI / 2.0d) + (Math.PI * i) / points;
                    float x = 0.50f + (float) Math.cos(angle) * radius;
                    float y = 0.50f + (float) Math.sin(angle) * radius;
                    if (i == 0) {
                        path.moveTo(x, y);
                    } else {
                        path.lineTo(x, y);
                    }
                }
                path.close();
                break;
            }
            case HOURGLASS:   // 沙漏：上下两个梯形，中间收腰
                path.moveTo(0.00f, 0.00f);
                path.lineTo(1.00f, 0.00f);
                path.cubicTo(0.94f, 0.32f, 0.72f, 0.42f, 0.72f, 0.50f);
                path.cubicTo(0.72f, 0.58f, 0.94f, 0.68f, 1.00f, 1.00f);
                path.lineTo(0.00f, 1.00f);
                path.cubicTo(0.06f, 0.68f, 0.28f, 0.58f, 0.28f, 0.50f);
                path.cubicTo(0.28f, 0.42f, 0.06f, 0.32f, 0.00f, 0.00f);
                path.close();
                break;
            default:
                return false;
        }
        Matrix matrix = new Matrix();
        matrix.setRectToRect(new RectF(0.00f, 0.00f, 1.00f, 1.00f),
                new RectF(0.00f, 0.00f, w, h), Matrix.ScaleToFit.FILL);
        path.transform(matrix);
        return true;
    }

    /** 设计框：natural>0 的形状按自身比例居中放进 (W,H)；其余（含固定比例形状，它们自己居中）铺满整个框。 */
    private static RectF designBox(ShapeKind shape, float W, float H) {
        float nat = shape.getNatural();
        if (nat <= 0.0f) {
            return new RectF(0.0f, 0.0f, W, H);
        }
        float bw = W;
        float bh = W / nat;
        if (bh > H) {
            bh = H;
            bw = H * nat;
        }
        float left = (W - bw) / 2.0f;
        float top = (H - bh) / 2.0f;
        return new RectF(left, top, left + bw, top + bh);
    }

    /** 老形状（v1.10.210 之前就有的 36 个）：原样搬过来，只在「设计框」里画。 */
    private final void buildLegacy(Path path, ShapeKind shape, float w, float h) {
        float unit = Math.min(w, h);
        RectF rect = new RectF(0.0f, 0.0f, w, h);
        float cx = w / 2.0f;
        float cy = h / 2.0f;
        switch (WhenMappings.$EnumSwitchMapping$0[shape.ordinal()]) {
            case 1:
                path.addRect(rect, Path.Direction.CW);
                break;
            case 2:
                float r = unit * 0.18f;
                path.addRoundRect(rect, r, r, Path.Direction.CW);
                break;
            case 3:
                float r2 = unit * 0.42f;
                path.addRoundRect(rect, r2, r2, Path.Direction.CW);
                break;
            case 4:
                float left = (w - unit) / 2.0f;
                float top = (h - unit) / 2.0f;
                path.addOval(new RectF(left, top, left + unit, top + unit), Path.Direction.CW);
                break;
            case 5:
                path.addOval(rect, Path.Direction.CW);
                break;
            case 6:
                float r3 = unit / 2.0f;
                path.addRoundRect(rect, r3, r3, Path.Direction.CW);
                break;
            case 7:
                path.moveTo(cx, 0.0f);
                path.lineTo(w, cy);
                path.lineTo(cx, h);
                path.lineTo(0.0f, cy);
                path.close();
                break;
            case 8:
                path.moveTo(cx, h * 0.06f);
                path.lineTo(w * 0.96f, h * 0.95f);
                path.lineTo(w * 0.04f, h * 0.95f);
                path.close();
                break;
            case 9:
                polygon(path, cx, cy, unit * 0.5f, 6, 0.0f);
                break;
            case 10:
                polygon(path, cx, cy, unit * 0.5f, 8, 22.5f);
                break;
            case 11:
                star(path, cx, cy, unit * 0.5f, unit * 0.21f, 5);
                break;
            case 12:
                star(path, cx, cy, unit * 0.5f, unit * 0.26f, 6);
                break;
            case 13:
                star(path, cx, cy, unit * 0.5f, unit * 0.3f, 8);
                break;
            case 14:
                float big = unit * 0.5f;
                float small = unit * 0.04f;
                path.addRoundRect(rect, new float[]{big, big, small, small, big, big, small, small}, Path.Direction.CW);
                break;
            case 15:
                float r4 = unit * 0.21f;
                float orbit = unit * 0.29f;
                for (int i = 0; i < 10; i++) {
                    double a = (6.283185307179586d * i) / 10;
                    Path $this$path_u24lambda_u240 = new Path();
                    $this$path_u24lambda_u240.addCircle(cx + (((float) Math.cos(a)) * orbit), cy + (((float) Math.sin(a)) * orbit), r4, Path.Direction.CW);
                    path.op($this$path_u24lambda_u240, Path.Op.UNION);
                }
                // v1.10.212：10 个圆的圆心在 r=0.29 上、半径只有 0.21 → 中心 0.08 的圆盘没有任何
                // 一个圆盖到，蒙版正中间是个**洞**（桌面上会漏出一个壁纸点，缩略图小时看不出来）。
                // 补一个花芯圆：半径 0.16 > 0.08（堵死洞），且与每个花瓣圆重叠（0.29 < 0.16+0.21）→ 连通。
                Path petalCore = new Path();
                petalCore.addCircle(cx, cy, unit * 0.16f, Path.Direction.CW);
                path.op(petalCore, Path.Op.UNION);
                break;
            case 16:
                float r5 = unit * 0.26f;
                float offset = unit * 0.2f;
                for (int dx : new int[]{-1, 1}) {
                    for (int dy : new int[]{-1, 1}) {
                        Path $this$path_u24lambda_u241 = new Path();
                        $this$path_u24lambda_u241.addCircle(cx + (dx * offset), cy + (dy * offset), r5, Path.Direction.CW);
                        path.op($this$path_u24lambda_u241, Path.Op.UNION);
                    }
                }
                // v1.10.212：四个叶圆的圆心距原点 0.283 > 半径 0.26 → 正中也是个小洞，一并补芯
                Path cloverCore = new Path();
                cloverCore.addCircle(cx, cy, unit * 0.12f, Path.Direction.CW);
                path.op(cloverCore, Path.Op.UNION);
                break;
            case 17:
                path.moveTo(cx, h * 0.92f);
                path.cubicTo(w * 0.02f, h * 0.56f, w * 0.06f, h * 0.06f, cx, h * 0.26f);
                path.cubicTo(w * 0.94f, h * 0.06f, w * 0.98f, h * 0.56f, cx, h * 0.92f);
                path.close();
                break;
            case 18:
                path.moveTo(cx, 0.0f);
                path.cubicTo(w * 0.85f, h * 0.35f, w * 0.9f, h * 0.8f, cx, h);
                path.cubicTo(w * 0.1f, h * 0.8f, w * 0.15f, h * 0.35f, cx, 0.0f);
                path.close();
                break;
            case 19:
                path.moveTo(w * 0.06f, h * 0.12f);
                path.quadTo(cx, 0.0f, w * 0.94f, h * 0.12f);
                path.lineTo(w * 0.88f, h * 0.58f);
                path.quadTo(w * 0.8f, h * 0.86f, cx, h);
                path.quadTo(w * 0.2f, h * 0.86f, w * 0.12f, h * 0.58f);
                path.close();
                break;
            case 20:
                float t = w * 0.18f;
                float b = h * 0.18f;
                path.moveTo(cx - (t / 2), 0.0f);
                path.lineTo(cx + (t / 2), 0.0f);
                path.lineTo(cx + (t / 2), cy - (b / 2));
                path.lineTo(w, cy - (b / 2));
                path.lineTo(w, cy + (b / 2));
                path.lineTo(cx + (t / 2), cy + (b / 2));
                path.lineTo(cx + (t / 2), h);
                path.lineTo(cx - (t / 2), h);
                path.lineTo(cx - (t / 2), cy + (b / 2));
                path.lineTo(0.0f, cy + (b / 2));
                path.lineTo(0.0f, cy - (b / 2));
                path.lineTo(cx - (t / 2), cy - (b / 2));
                path.close();
                break;
            case 21:
                path.moveTo(0.0f, h * 0.12f);
                path.quadTo(w * 0.25f, 0.0f, w * 0.5f, h * 0.12f);
                path.quadTo(w * 0.75f, h * 0.24f, w, h * 0.12f);
                path.lineTo(w, h * 0.88f);
                path.quadTo(w * 0.75f, h, w * 0.5f, h * 0.88f);
                path.quadTo(w * 0.25f, h * 0.76f, 0.0f, h * 0.88f);
                path.close();
                break;
            case 22:
                path.moveTo(0.0f, 0.0f);
                path.lineTo(w, 0.0f);
                path.lineTo(w, h);
                path.lineTo(cx, h * 0.78f);
                path.lineTo(0.0f, h);
                path.close();
                break;
            case 23:
                path.addRoundRect(new RectF(0.0f, 0.0f, w, h * 0.8f), unit * 0.18f, unit * 0.18f, Path.Direction.CW);
                Path $this$path_u24lambda_u242 = new Path();
                $this$path_u24lambda_u242.moveTo(w * 0.24f, h * 0.78f);
                $this$path_u24lambda_u242.lineTo(w * 0.38f, h);
                $this$path_u24lambda_u242.lineTo(w * 0.48f, h * 0.78f);
                $this$path_u24lambda_u242.close();
                path.op($this$path_u24lambda_u242, Path.Op.UNION);
                break;
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
                addFixedRatio(path, w, h, shape.getAspect(), false);
                break;
            case 33:
            case 34:
            case 35:
                addFixedRatio(path, w, h, shape.getAspect(), true);
                break;
            case 36:
                float rx = w * 0.5f;
                float ry = h * 0.5f;
                float[] wobble = {0.92f, 1.06f, 0.95f, 1.08f, 0.9f, 1.05f, 0.96f, 1.04f};
                for (int i2 = 0; i2 < 17; i2++) {
                    double a2 = (6.283185307179586d * i2) / 16;
                    float k = wobble[i2 % wobble.length];
                    float x = cx + (((float) Math.cos(a2)) * rx * k);
                    float y = cy + (((float) Math.sin(a2)) * ry * k);
                    if (i2 == 0) {
                        path.moveTo(x, y);
                    } else {
                        path.lineTo(x, y);
                    }
                }
                path.close();
                break;
        }
    }

    private final void stretchToBox(Path path, float w, float h) {
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);
        float bw = bounds.width();
        float bh = bounds.height();
        if (bw <= 0.01f || bh <= 0.01f) {
            return;
        }
        float sx = w / bw;
        float sy = h / bh;
        if (Math.abs(sx - 1.0f) >= 0.002f || Math.abs(sy - 1.0f) >= 0.002f) {
            Matrix matrix = new Matrix();
            matrix.setScale(sx, sy);
            matrix.postTranslate((-bounds.left) * sx, (-bounds.top) * sy);
            path.transform(matrix);
        }
    }

    private final void addFixedRatio(Path path, float w, float h, float ratio, boolean rounded) {
        float rh;
        float rw;
        if (ratio <= 0.0f) {
            return;
        }
        float boxRatio = w / h;
        if (ratio > boxRatio) {
            rw = w;
            rh = w / ratio;
        } else {
            rh = h;
            rw = h * ratio;
        }
        float left = (w - rw) / 2.0f;
        float top = (h - rh) / 2.0f;
        RectF rect = new RectF(left, top, left + rw, top + rh);
        if (rounded) {
            float r = Math.min(rw, rh) * 0.18f;
            path.addRoundRect(rect, r, r, Path.Direction.CW);
        } else {
            path.addRect(rect, Path.Direction.CW);
        }
    }

    private final void polygon(Path path, float cx, float cy, float radius, int sides, float rotation) {
        for (int i = 0; i < sides; i++) {
            double a = (Math.toRadians(rotation) + ((6.283185307179586d * i) / sides)) - 1.5707963267948966d;
            float x = cx + (((float) Math.cos(a)) * radius);
            float y = cy + (((float) Math.sin(a)) * radius);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
    }

    private final void star(Path path, float cx, float cy, float outer, float inner, int points) {
        int i = points * 2;
        for (int i2 = 0; i2 < i; i2++) {
            float radius = i2 % 2 == 0 ? outer : inner;
            double a = (-1.5707963267948966d) + ((3.141592653589793d * i2) / points);
            float x = cx + (((float) Math.cos(a)) * radius);
            float y = cy + (((float) Math.sin(a)) * radius);
            if (i2 == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
    }

    @NotNull
    public final Bitmap apply(@NotNull Bitmap content, @NotNull ShapeKind shape, int alphaPercent, boolean border) {
        Intrinsics.checkNotNullParameter(content, "content");
        Intrinsics.checkNotNullParameter(shape, "shape");
        int alpha = RangesKt.coerceIn(alphaPercent, 0, 100);
        if (shape == ShapeKind.RECT && alpha == 100 && !border) {
            return content;
        }
        int width = content.getWidth();
        int height = content.getHeight();
        Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Intrinsics.checkNotNullExpressionValue(out, "createBitmap(...)");
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(3);
        paint.setAlpha(RangesKt.coerceIn((int) ((255.0f * alpha) / 100.0f), 0, 255));
        Path maskPath = path(shape, width, height);
        canvas.save();
        canvas.clipPath(maskPath);
        canvas.drawBitmap(content, 0.0f, 0.0f, paint);
        canvas.restore();
        Paint maskPaint = new Paint(1);
        maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawPath(maskPath, maskPaint);
        if (border) {
            Paint $this$apply_u24lambda_u243 = new Paint(1);
            $this$apply_u24lambda_u243.setStyle(Paint.Style.STROKE);
            $this$apply_u24lambda_u243.setStrokeWidth(RangesKt.coerceAtLeast(Math.min(width, height) * 0.018f, 2.0f));
            $this$apply_u24lambda_u243.setColor(Color.argb(200, 255, 255, 255));
            canvas.drawPath(maskPath, $this$apply_u24lambda_u243);
        }
        return out;
    }
}
