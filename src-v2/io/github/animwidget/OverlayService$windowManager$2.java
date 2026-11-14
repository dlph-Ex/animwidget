package io.github.animwidget;

import android.view.WindowManager;
import kotlin.Metadata;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.internal.Lambda;

/* compiled from: OverlayService.kt */
@Metadata(mv = {1, 9, 0}, k = 3, xi = 48, d1 = {"��\n\n��\n\u0002\u0018\u0002\n\u0002\b\u0002\u0010��\u001a\n \u0002*\u0004\u0018\u00010\u00010\u0001H\n¢\u0006\u0002\b\u0003"}, d2 = {"<anonymous>", "Landroid/view/WindowManager;", "kotlin.jvm.PlatformType", "invoke"})
/* loaded from: classes.jar:io/github/animwidget/OverlayService$windowManager$2.class */
final class OverlayService$windowManager$2 implements Function0<WindowManager> {
    final /* synthetic */ OverlayService this$0;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    OverlayService$windowManager$2(OverlayService $receiver) {
        this.this$0 = $receiver;
    }

    /* renamed from: invoke, reason: merged with bridge method [inline-methods] */
    public final WindowManager invoke() {
        return (WindowManager) this.this$0.getSystemService(WindowManager.class);
    }
}
