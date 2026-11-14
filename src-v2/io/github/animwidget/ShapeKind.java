package io.github.animwidget;

import android.content.Context;
import kotlin.Metadata;
import kotlin.NoWhenBranchMatchedException;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;

/* compiled from: WidgetConfig.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��$\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b$\b\u0086\u0081\u0002\u0018��2\b\u0012\u0004\u0012\u00020��0\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J\u000e\u0010\u0007\u001a\u00020\b2\u0006\u0010\u000b\u001a\u00020\fR\u0011\u0010\u0003\u001a\u00020\u00048F¢\u0006\u0006\u001a\u0004\b\u0005\u0010\u0006R\u0011\u0010\u0007\u001a\u00020\b8F¢\u0006\u0006\u001a\u0004\b\t\u0010\nj\u0002\b\rj\u0002\b\u000ej\u0002\b\u000fj\u0002\b\u0010j\u0002\b\u0011j\u0002\b\u0012j\u0002\b\u0013j\u0002\b\u0014j\u0002\b\u0015j\u0002\b\u0016j\u0002\b\u0017j\u0002\b\u0018j\u0002\b\u0019j\u0002\b\u001aj\u0002\b\u001bj\u0002\b\u001cj\u0002\b\u001dj\u0002\b\u001ej\u0002\b\u001fj\u0002\b j\u0002\b!j\u0002\b\"j\u0002\b#j\u0002\b$j\u0002\b%j\u0002\b&j\u0002\b'j\u0002\b(j\u0002\b)j\u0002\b*j\u0002\b+j\u0002\b,j\u0002\b-j\u0002\b.j\u0002\b/j\u0002\b0"}, d2 = {"Lio/github/animwidget/ShapeKind;", "", "(Ljava/lang/String;I)V", "aspect", "", "getAspect", "()F", "label", "", "getLabel", "()Ljava/lang/String;", "context", "Landroid/content/Context;", "RECT", "ROUNDED", "SQUIRCLE", "CIRCLE", "ELLIPSE", "CAPSULE", "DIAMOND", "TRIANGLE", "HEXAGON", "OCTAGON", "STAR", "STAR6", "STAR8", "LEAF", "PETAL", "CLOVER", "HEART", "TEARDROP", "SHIELD", "CROSS", "WAVE", "BOOKMARK", "MESSAGE", "BLOB", "SQUARE_1_1", "ROUND_1_1", "RECT_4_3", "ROUND_4_3", "RECT_3_4", "ROUND_3_4", "RECT_16_9", "RECT_9_16", "RECT_3_2", "RECT_2_3", "RECT_21_9", "RECT_2_1"})
/* loaded from: classes.jar:io/github/animwidget/ShapeKind.class */
public enum ShapeKind {
    RECT,
    ROUNDED,
    SQUIRCLE,
    CIRCLE,
    ELLIPSE,
    CAPSULE,
    DIAMOND,
    TRIANGLE,
    HEXAGON,
    OCTAGON,
    STAR,
    STAR6,
    STAR8,
    LEAF,
    PETAL,
    CLOVER,
    HEART,
    TEARDROP,
    SHIELD,
    CROSS,
    WAVE,
    BOOKMARK,
    MESSAGE,
    BLOB,
    SQUARE_1_1,
    ROUND_1_1,
    RECT_4_3,
    ROUND_4_3,
    RECT_3_4,
    ROUND_3_4,
    RECT_16_9,
    RECT_9_16,
    RECT_3_2,
    RECT_2_3,
    RECT_21_9,
    RECT_2_1,
    // v1.10.211：12 个自绘异形（全部追加在末尾，老配置的 ordinal 不变）
    MOON,
    CLOUD,
    BOLT,
    ARCH,
    EYE,
    GEM,
    GEAR,
    YINYANG,
    TICKET,
    KEYHOLE,
    SUNBURST,
    HOURGLASS;

    private static final /* synthetic */ EnumEntries $ENTRIES = EnumEntriesKt.enumEntries(values());

    /* compiled from: WidgetConfig.kt */
    @Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    /* loaded from: classes.jar:io/github/animwidget/ShapeKind$WhenMappings.class */
    public static /* synthetic */ class WhenMappings {
        public static final /* synthetic */ int[] $EnumSwitchMapping$0;

        static {
            int[] iArr = new int[ShapeKind.values().length];
            try {
                iArr[ShapeKind.SQUARE_1_1.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[ShapeKind.ROUND_1_1.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[ShapeKind.RECT_4_3.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[ShapeKind.ROUND_4_3.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr[ShapeKind.RECT_3_4.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                iArr[ShapeKind.ROUND_3_4.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                iArr[ShapeKind.RECT_16_9.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                iArr[ShapeKind.RECT_9_16.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                iArr[ShapeKind.RECT_3_2.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                iArr[ShapeKind.RECT_2_3.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                iArr[ShapeKind.RECT_21_9.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                iArr[ShapeKind.RECT_2_1.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                iArr[ShapeKind.RECT.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
            try {
                iArr[ShapeKind.ROUNDED.ordinal()] = 14;
            } catch (NoSuchFieldError e14) {
            }
            try {
                iArr[ShapeKind.SQUIRCLE.ordinal()] = 15;
            } catch (NoSuchFieldError e15) {
            }
            try {
                iArr[ShapeKind.CIRCLE.ordinal()] = 16;
            } catch (NoSuchFieldError e16) {
            }
            try {
                iArr[ShapeKind.ELLIPSE.ordinal()] = 17;
            } catch (NoSuchFieldError e17) {
            }
            try {
                iArr[ShapeKind.CAPSULE.ordinal()] = 18;
            } catch (NoSuchFieldError e18) {
            }
            try {
                iArr[ShapeKind.DIAMOND.ordinal()] = 19;
            } catch (NoSuchFieldError e19) {
            }
            try {
                iArr[ShapeKind.TRIANGLE.ordinal()] = 20;
            } catch (NoSuchFieldError e20) {
            }
            try {
                iArr[ShapeKind.HEXAGON.ordinal()] = 21;
            } catch (NoSuchFieldError e21) {
            }
            try {
                iArr[ShapeKind.OCTAGON.ordinal()] = 22;
            } catch (NoSuchFieldError e22) {
            }
            try {
                iArr[ShapeKind.STAR.ordinal()] = 23;
            } catch (NoSuchFieldError e23) {
            }
            try {
                iArr[ShapeKind.STAR6.ordinal()] = 24;
            } catch (NoSuchFieldError e24) {
            }
            try {
                iArr[ShapeKind.STAR8.ordinal()] = 25;
            } catch (NoSuchFieldError e25) {
            }
            try {
                iArr[ShapeKind.LEAF.ordinal()] = 26;
            } catch (NoSuchFieldError e26) {
            }
            try {
                iArr[ShapeKind.PETAL.ordinal()] = 27;
            } catch (NoSuchFieldError e27) {
            }
            try {
                iArr[ShapeKind.CLOVER.ordinal()] = 28;
            } catch (NoSuchFieldError e28) {
            }
            try {
                iArr[ShapeKind.HEART.ordinal()] = 29;
            } catch (NoSuchFieldError e29) {
            }
            try {
                iArr[ShapeKind.TEARDROP.ordinal()] = 30;
            } catch (NoSuchFieldError e30) {
            }
            try {
                iArr[ShapeKind.SHIELD.ordinal()] = 31;
            } catch (NoSuchFieldError e31) {
            }
            try {
                iArr[ShapeKind.CROSS.ordinal()] = 32;
            } catch (NoSuchFieldError e32) {
            }
            try {
                iArr[ShapeKind.WAVE.ordinal()] = 33;
            } catch (NoSuchFieldError e33) {
            }
            try {
                iArr[ShapeKind.BOOKMARK.ordinal()] = 34;
            } catch (NoSuchFieldError e34) {
            }
            try {
                iArr[ShapeKind.MESSAGE.ordinal()] = 35;
            } catch (NoSuchFieldError e35) {
            }
            try {
                iArr[ShapeKind.BLOB.ordinal()] = 36;
            } catch (NoSuchFieldError e36) {
            }
            // v1.10.211 新增异形
            try { iArr[ShapeKind.MOON.ordinal()] = 37; } catch (NoSuchFieldError e) { }
            try { iArr[ShapeKind.CLOUD.ordinal()] = 38; } catch (NoSuchFieldError e) { }
            try { iArr[ShapeKind.BOLT.ordinal()] = 39; } catch (NoSuchFieldError e) { }
            try { iArr[ShapeKind.ARCH.ordinal()] = 40; } catch (NoSuchFieldError e) { }
            try { iArr[ShapeKind.EYE.ordinal()] = 41; } catch (NoSuchFieldError e) { }
            try { iArr[ShapeKind.GEM.ordinal()] = 42; } catch (NoSuchFieldError e) { }
            try { iArr[ShapeKind.GEAR.ordinal()] = 43; } catch (NoSuchFieldError e) { }
            try { iArr[ShapeKind.YINYANG.ordinal()] = 44; } catch (NoSuchFieldError e) { }
            try { iArr[ShapeKind.TICKET.ordinal()] = 45; } catch (NoSuchFieldError e) { }
            try { iArr[ShapeKind.KEYHOLE.ordinal()] = 46; } catch (NoSuchFieldError e) { }
            try { iArr[ShapeKind.SUNBURST.ordinal()] = 47; } catch (NoSuchFieldError e) { }
            try { iArr[ShapeKind.HOURGLASS.ordinal()] = 48; } catch (NoSuchFieldError e) { }
            $EnumSwitchMapping$0 = iArr;
        }
    }

    /**
     * v1.10.211：形状自身的「设计比例」= 宽/高。
     *
     * <p>0 表示**随框拉伸**：矩形 / 圆角 / 超椭圆 / 椭圆 / 胶囊 / 叶子 / 十字 / 波浪 / 书签 /
     * 对话框 / 三角 / 菱形 / 六边 / 八边 这类「框形」，拉长是正确表现（16:9 的胶囊就该是长条）。
     * 大于 0 表示**保比例**：星形、心形、水滴、盾形、眼睛、云朵…… 这些一旦按框拉伸就会「扭曲」，
     * 必须按自己的比例居中放进布局框（画面/蒙版都跟着这个比例走，不会截一半）。</p>
     */
    public final float getNatural() {
        switch (WhenMappings.$EnumSwitchMapping$0[ordinal()]) {
            case 4:   // CIRCLE
                return 1.0f;
            case 11:  // STAR
            case 12:  // STAR6
            case 13:  // STAR8
            case 15:  // PETAL
            case 16:  // CLOVER
            case 17:  // HEART
            case 18:  // TEARDROP
            case 19:  // SHIELD
            case 36:  // BLOB
                return 1.0f;
            case 37:  // MOON
                return 1.0f;
            case 38:  // CLOUD
                return 1.5f;
            case 39:  // BOLT
                return 0.62f;
            case 40:  // ARCH
                return 0.85f;
            case 41:  // EYE
                return 1.7f;
            case 42:  // GEM
                return 1.0f;
            case 43:  // GEAR
                return 1.0f;
            case 44:  // YINYANG
                return 1.0f;
            case 45:  // TICKET
                return 1.5f;
            case 46:  // KEYHOLE
                return 1.0f;
            case 47:  // SUNBURST
                return 1.0f;
            case 48:  // HOURGLASS
                return 0.75f;
            default:
                return 0.0f;
        }
    }

    @NotNull
    public static EnumEntries<ShapeKind> getEntries() {
        return $ENTRIES;
    }

    public final float getAspect() {
        switch (WhenMappings.$EnumSwitchMapping$0[ordinal()]) {
            case 1:
            case 2:
                return 1.0f;
            case 3:
            case 4:
                return 1.3333334f;
            case 5:
            case 6:
                return 0.75f;
            case 7:
                return 1.7777778f;
            case 8:
                return 0.5625f;
            case 9:
                return 1.5f;
            case 10:
                return 0.6666667f;
            case 11:
                return 2.3333333f;
            case 12:
                return 2.0f;
            default:
                return 0.0f;
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: kotlin.NoWhenBranchMatchedException */
    @NotNull
    public final String label(@NotNull Context context) throws NoWhenBranchMatchedException {
        Intrinsics.checkNotNullParameter(context, "context");
        switch (WhenMappings.$EnumSwitchMapping$0[ordinal()]) {
            case 1:
                return "1:1";
            case 2:
                return "1:1 " + context.getString(R.string.shape_rounded_suffix);
            case 3:
                return "4:3";
            case 4:
                return "4:3 " + context.getString(R.string.shape_rounded_suffix);
            case 5:
                return "3:4";
            case 6:
                return "3:4 " + context.getString(R.string.shape_rounded_suffix);
            case 7:
                return "16:9";
            case 8:
                return "9:16";
            case 9:
                return "3:2";
            case 10:
                return "2:3";
            case 11:
                return "21:9";
            case 12:
                return "2:1";
            case 13:
                String string = context.getString(R.string.shape_rect);
                Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
                return string;
            case 14:
                String string2 = context.getString(R.string.shape_rounded);
                Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
                return string2;
            case 15:
                String string3 = context.getString(R.string.shape_squircle);
                Intrinsics.checkNotNullExpressionValue(string3, "getString(...)");
                return string3;
            case 16:
                String string4 = context.getString(R.string.shape_circle);
                Intrinsics.checkNotNullExpressionValue(string4, "getString(...)");
                return string4;
            case 17:
                String string5 = context.getString(R.string.shape_ellipse);
                Intrinsics.checkNotNullExpressionValue(string5, "getString(...)");
                return string5;
            case 18:
                String string6 = context.getString(R.string.shape_capsule);
                Intrinsics.checkNotNullExpressionValue(string6, "getString(...)");
                return string6;
            case 19:
                String string7 = context.getString(R.string.shape_diamond);
                Intrinsics.checkNotNullExpressionValue(string7, "getString(...)");
                return string7;
            case 20:
                String string8 = context.getString(R.string.shape_triangle);
                Intrinsics.checkNotNullExpressionValue(string8, "getString(...)");
                return string8;
            case 21:
                String string9 = context.getString(R.string.shape_hexagon);
                Intrinsics.checkNotNullExpressionValue(string9, "getString(...)");
                return string9;
            case 22:
                String string10 = context.getString(R.string.shape_octagon);
                Intrinsics.checkNotNullExpressionValue(string10, "getString(...)");
                return string10;
            case 23:
                String string11 = context.getString(R.string.shape_star);
                Intrinsics.checkNotNullExpressionValue(string11, "getString(...)");
                return string11;
            case 24:
                String string12 = context.getString(R.string.shape_star6);
                Intrinsics.checkNotNullExpressionValue(string12, "getString(...)");
                return string12;
            case 25:
                String string13 = context.getString(R.string.shape_star8);
                Intrinsics.checkNotNullExpressionValue(string13, "getString(...)");
                return string13;
            case 26:
                String string14 = context.getString(R.string.shape_leaf);
                Intrinsics.checkNotNullExpressionValue(string14, "getString(...)");
                return string14;
            case 27:
                String string15 = context.getString(R.string.shape_petal);
                Intrinsics.checkNotNullExpressionValue(string15, "getString(...)");
                return string15;
            case 28:
                String string16 = context.getString(R.string.shape_clover);
                Intrinsics.checkNotNullExpressionValue(string16, "getString(...)");
                return string16;
            case 29:
                String string17 = context.getString(R.string.shape_heart);
                Intrinsics.checkNotNullExpressionValue(string17, "getString(...)");
                return string17;
            case 30:
                String string18 = context.getString(R.string.shape_teardrop);
                Intrinsics.checkNotNullExpressionValue(string18, "getString(...)");
                return string18;
            case 31:
                String string19 = context.getString(R.string.shape_shield);
                Intrinsics.checkNotNullExpressionValue(string19, "getString(...)");
                return string19;
            case 32:
                String string20 = context.getString(R.string.shape_cross);
                Intrinsics.checkNotNullExpressionValue(string20, "getString(...)");
                return string20;
            case 33:
                String string21 = context.getString(R.string.shape_wave);
                Intrinsics.checkNotNullExpressionValue(string21, "getString(...)");
                return string21;
            case 34:
                String string22 = context.getString(R.string.shape_bookmark);
                Intrinsics.checkNotNullExpressionValue(string22, "getString(...)");
                return string22;
            case 35:
                String string23 = context.getString(R.string.shape_message);
                Intrinsics.checkNotNullExpressionValue(string23, "getString(...)");
                return string23;
            case 36:
                String string24 = context.getString(R.string.shape_blob);
                Intrinsics.checkNotNullExpressionValue(string24, "getString(...)");
                return string24;
            case 37:
                String s37 = context.getString(R.string.shape_moon);
                Intrinsics.checkNotNullExpressionValue(s37, "getString(...)");
                return s37;
            case 38:
                String s38 = context.getString(R.string.shape_cloud);
                Intrinsics.checkNotNullExpressionValue(s38, "getString(...)");
                return s38;
            case 39:
                String s39 = context.getString(R.string.shape_bolt);
                Intrinsics.checkNotNullExpressionValue(s39, "getString(...)");
                return s39;
            case 40:
                String s40 = context.getString(R.string.shape_arch);
                Intrinsics.checkNotNullExpressionValue(s40, "getString(...)");
                return s40;
            case 41:
                String s41 = context.getString(R.string.shape_eye);
                Intrinsics.checkNotNullExpressionValue(s41, "getString(...)");
                return s41;
            case 42:
                String s42 = context.getString(R.string.shape_gem);
                Intrinsics.checkNotNullExpressionValue(s42, "getString(...)");
                return s42;
            case 43:
                String s43 = context.getString(R.string.shape_gear);
                Intrinsics.checkNotNullExpressionValue(s43, "getString(...)");
                return s43;
            case 44:
                String s44 = context.getString(R.string.shape_yinyang);
                Intrinsics.checkNotNullExpressionValue(s44, "getString(...)");
                return s44;
            case 45:
                String s45 = context.getString(R.string.shape_ticket);
                Intrinsics.checkNotNullExpressionValue(s45, "getString(...)");
                return s45;
            case 46:
                String s46 = context.getString(R.string.shape_keyhole);
                Intrinsics.checkNotNullExpressionValue(s46, "getString(...)");
                return s46;
            case 47:
                String s47 = context.getString(R.string.shape_sunburst);
                Intrinsics.checkNotNullExpressionValue(s47, "getString(...)");
                return s47;
            case 48:
                String s48 = context.getString(R.string.shape_hourglass);
                Intrinsics.checkNotNullExpressionValue(s48, "getString(...)");
                return s48;
            default:
                throw new NoWhenBranchMatchedException();
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: kotlin.NoWhenBranchMatchedException */
    @NotNull
    public final String getLabel() throws NoWhenBranchMatchedException {
        switch (WhenMappings.$EnumSwitchMapping$0[ordinal()]) {
            case 1:
                return "1:1 方形";
            case 2:
                return "1:1 圆角";
            case 3:
                return "4:3";
            case 4:
                return "4:3 圆角";
            case 5:
                return "3:4";
            case 6:
                return "3:4 圆角";
            case 7:
                return "16:9";
            case 8:
                return "9:16";
            case 9:
                return "3:2";
            case 10:
                return "2:3";
            case 11:
                return "21:9";
            case 12:
                return "2:1";
            case 13:
                return "方形";
            case 14:
                return "圆角";
            case 15:
                return "超椭圆";
            case 16:
                return "圆形";
            case 17:
                return "椭圆";
            case 18:
                return "胶囊";
            case 19:
                return "菱形";
            case 20:
                return "三角";
            case 21:
                return "六边";
            case 22:
                return "八边";
            case 23:
                return "五角星";
            case 24:
                return "六角星";
            case 25:
                return "八角星";
            case 26:
                return "叶子";
            case 27:
                return "花瓣";
            case 28:
                return "四叶";
            case 29:
                return "心形";
            case 30:
                return "水滴";
            case 31:
                return "盾形";
            case 32:
                return "十字";
            case 33:
                return "波浪";
            case 34:
                return "书签";
            case 35:
                return "对话框";
            case 36:
                return "异形";
            case 37:
                return "月牙";
            case 38:
                return "云朵";
            case 39:
                return "闪电";
            case 40:
                return "拱门";
            case 41:
                return "眼睛";
            case 42:
                return "宝石";
            case 43:
                return "齿轮";
            case 44:
                return "阴阳";
            case 45:
                return "票券";
            case 46:
                return "钥匙孔";
            case 47:
                return "星芒";
            case 48:
                return "沙漏";
            default:
                throw new NoWhenBranchMatchedException();
        }
    }
}
