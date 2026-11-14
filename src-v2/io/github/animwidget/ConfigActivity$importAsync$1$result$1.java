package io.github.animwidget;

import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Lambda;

/* compiled from: ConfigActivity.kt */
@Metadata(mv = {1, 9, 0}, k = 3, xi = 48, d1 = {"��\u0010\n��\n\u0002\u0010\u0002\n��\n\u0002\u0010\b\n\u0002\b\u0002\u0010��\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0003H\n¢\u0006\u0002\b\u0005"}, d2 = {"<anonymous>", "", "done", "", "total", "invoke"})
/* loaded from: classes.jar:io/github/animwidget/ConfigActivity$importAsync$1$result$1.class */
final class ConfigActivity$importAsync$1$result$1 implements Function2<Integer, Integer, Unit> {
    final /* synthetic */ ConfigActivity this$0;
    final /* synthetic */ int $gen;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    ConfigActivity$importAsync$1$result$1(ConfigActivity $receiver, int gen) {
        this.this$0 = $receiver;
        this.$gen = gen;
    }

    public final Unit invoke(Integer p1, Integer p2) {
        final int done = p1.intValue();
        final int total = p2.intValue();
        final int gen = this.$gen;
        this.this$0.ui.post(() -> invoke$lambda$0(this.this$0, done, total, gen));
        return Unit.INSTANCE;
    }

    private static final void invoke$lambda$0(ConfigActivity this$0, int $done, int $total, int $gen) {
        // v1.10.1：进度只写任务状态（内部 100ms 节流）；旧任务的进度直接丢弃
        this$0.onParseProgress($done, $total, $gen);
    }
}
