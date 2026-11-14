package io.github.animwidget;

import android.os.Handler;
import android.os.SystemClock;
import android.widget.ProgressBar;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

import java.util.List;

/* compiled from: ConfigActivity.kt */
@Metadata(mv = {1, 9, 0}, k = 3, xi = 48, d1 = {"��\u0010\n��\n\u0002\u0010\u0002\n��\n\u0002\u0010\b\n\u0002\b\u0002\u0010��\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0003H\n¢\u0006\u0002\b\u0005"}, d2 = {"<anonymous>", "", "done", "", "total", "invoke"})
/* loaded from: classes.jar:io/github/animwidget/ConfigActivity$loadPreviewFrames$1$result$1.class */
final class ConfigActivity$loadPreviewFrames$1$result$1 implements PreviewFrames.Progress {
    final /* synthetic */ ConfigActivity this$0;
    final /* synthetic */ int $gen;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    ConfigActivity$loadPreviewFrames$1$result$1(ConfigActivity $receiver, int gen) {
        this.this$0 = $receiver;
        this.$gen = gen;
    }

    public final void invoke(int done, int total, List<String> framesSoFar, int fps) {
        // v1.9.3：迟到（已被换代）的抽帧不再碰解析条
        if ($gen != this.this$0.extractGeneration) {
            return;
        }
        // 解析期间不发布帧 —— 规格：解析完成后才播放。这里只推进解析条（200ms 降频）
        long now = SystemClock.elapsedRealtime();
        if (now - this.this$0.lastTextMs >= 200L) {
            this.this$0.lastTextMs = now;
            final int d = done;
            final int t = total;
            this.this$0.ui.post(() -> invoke$lambda$0(this.this$0, d, t));
        }
    }

    private static final void invoke$lambda$0(ConfigActivity this$0, int $done, int $total) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        // v1.10.3：圆环 + 解析条 + 预览区底部的文字条，统一走一个入口
        this$0.onPreviewParseProgress($done, $total);
    }
}
