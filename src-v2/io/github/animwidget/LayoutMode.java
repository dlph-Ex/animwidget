package io.github.animwidget;

import android.content.Context;
import kotlin.Metadata;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;

/* compiled from: WidgetConfig.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��(\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n��\n\u0002\u0010\u000e\n��\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0007\b\u0086\u0081\u0002\u0018��2\b\u0012\u0004\u0012\u00020��0\u0001B\u0017\b\u0002\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005¢\u0006\u0002\u0010\u0006J\u000e\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u000f\u001a\u00020\u0010R\u0011\u0010\u0007\u001a\u00020\b8F¢\u0006\u0006\u001a\u0004\b\t\u0010\nR\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n��\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0004\u001a\u00020\u0005¢\u0006\b\n��\u001a\u0004\b\r\u0010\u000ej\u0002\b\u0011j\u0002\b\u0012j\u0002\b\u0013j\u0002\b\u0014j\u0002\b\u0015j\u0002\b\u0016j\u0002\b\u0017"}, d2 = {"Lio/github/animwidget/LayoutMode;", "", "label", "", "ratio", "", "(Ljava/lang/String;ILjava/lang/String;F)V", "fitsContent", "", "getFitsContent", "()Z", "getLabel", "()Ljava/lang/String;", "getRatio", "()F", "context", "Landroid/content/Context;", "FIT", "FILL", "R16_9", "R4_3", "R1_1", "R9_16", "R21_9"})
/* loaded from: classes.jar:io/github/animwidget/LayoutMode.class */
public enum LayoutMode {
    FIT("原始比例", 0.0f),
    FILL("填充", 0.0f),
    R16_9("16:9", 1.7777778f),
    R4_3("4:3", 1.3333334f),
    R1_1("1:1", 1.0f),
    R9_16("9:16", 0.5625f),
    R21_9("21:9", 2.3333333f);


    @NotNull
    final String label;
    final float ratio;
    private static final /* synthetic */ EnumEntries $ENTRIES = EnumEntriesKt.enumEntries(values());

    /* compiled from: WidgetConfig.kt */
    @Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    /* loaded from: classes.jar:io/github/animwidget/LayoutMode$WhenMappings.class */
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

    @NotNull
    public static EnumEntries<LayoutMode> getEntries() {
        return $ENTRIES;
    }

    LayoutMode(String label, float ratio) {
        this.label = label;
        this.ratio = ratio;
    }

    @NotNull
    public final String getLabel() {
        return this.label;
    }

    public final float getRatio() {
        return this.ratio;
    }

    public final boolean getFitsContent() {
        return this == FIT;
    }

    @NotNull
    public final String label(@NotNull Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
        switch (WhenMappings.$EnumSwitchMapping$0[ordinal()]) {
            case 1:
                String string = context.getString(R.string.layout_fit);
                Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
                return string;
            case 2:
                String string2 = context.getString(R.string.layout_fill);
                Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
                return string2;
            default:
                return this.label;
        }
    }
}
