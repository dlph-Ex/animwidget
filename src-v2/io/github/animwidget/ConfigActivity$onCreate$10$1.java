package io.github.animwidget;

import android.widget.SeekBar;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Lambda;

/* compiled from: ConfigActivity.kt */
@Metadata(mv = {1, 9, 0}, k = 3, xi = 48, d1 = {"��\u000e\n��\n\u0002\u0010\u0002\n��\n\u0002\u0010\b\n��\u0010��\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\n¢\u0006\u0002\b\u0004"}, d2 = {"<anonymous>", "", "value", "", "invoke"})
/* loaded from: classes.jar:io/github/animwidget/ConfigActivity$onCreate$10$1.class */
final class ConfigActivity$onCreate$10$1 implements Function1<Integer, Unit> {
    final /* synthetic */ ConfigActivity this$0;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    ConfigActivity$onCreate$10$1(ConfigActivity $receiver) {
        this.this$0 = $receiver;
    }

    public final Unit invoke(Integer value) {
        SeekBar seekBar = this.this$0.seekShadow;
        if (seekBar == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekShadow");
            seekBar = null;
        }
        seekBar.setProgress(value.intValue());
        this.this$0.updateLabels();
        this.this$0.refreshPreviewNow();
        return Unit.INSTANCE;
    }
}
