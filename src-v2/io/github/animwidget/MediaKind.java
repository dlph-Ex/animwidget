package io.github.animwidget;

import kotlin.Metadata;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import org.jetbrains.annotations.NotNull;

/* compiled from: WidgetConfig.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0004\b\u0086\u0081\u0002\u0018��2\b\u0012\u0004\u0012\u00020��0\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005"}, d2 = {"Lio/github/animwidget/MediaKind;", "", "(Ljava/lang/String;I)V", "VIDEO", "GIF", "IMAGE"})
/* loaded from: classes.jar:io/github/animwidget/MediaKind.class */
public enum MediaKind {
    VIDEO,
    GIF,
    IMAGE;

    private static final /* synthetic */ EnumEntries $ENTRIES = EnumEntriesKt.enumEntries(values());

    @NotNull
    public static EnumEntries<MediaKind> getEntries() {
        return $ENTRIES;
    }
}
