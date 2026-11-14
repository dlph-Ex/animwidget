package io.github.animwidget;

import android.graphics.Bitmap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** 位图小工具：等比缩边（v1.10.0）。 */
final class FrameUtil {

    private FrameUtil() {
    }

    /**
     * v1.10.170（反馈 ②）：把「帧率×10」打印成**不会被读错**的形式。
     *
     * 332 这种整数是 ×10 存的（333 = 33.3fps）。日志里只写 `fps=333` 会让人以为 333fps，
     * 之前 G10 / V10 / P02 / K01 四处都是这么写的。统一成
     * `fpsX10=333(33.3fps)` —— 名字带 X10，括号里给真实值，两个口径同时在场，谁都读不错。
     * 只影响日志文本，不影响任何逻辑。
     */
    @NotNull
    static String fpsX10Text(int fpsX10) {
        return "fpsX10=" + fpsX10 + "("
                + String.format(java.util.Locale.US, "%.1f", fpsX10 / 10.0f) + "fps)";
    }

    /**
     * 等比缩到「最长边 ≤ maxEdge」。
     * 不需要缩时原样返回 src（调用方不要 recycle 返回值以外的东西）；
     * 需要缩时返回一张新图（调用方用完自行 recycle）。
     */
    @NotNull
    static Bitmap fit(@NotNull Bitmap src, int maxEdge) {
        int w = src.getWidth();
        int h = src.getHeight();
        int longest = Math.max(w, h);
        if (maxEdge <= 0 || longest <= maxEdge || longest <= 1) {
            return src;
        }
        // v1.8.2 教训：这里必须是 float 除法，否则 ratio = 0 → 1×1 帧
        float ratio = maxEdge / (float) longest;
        int outW = Math.max(1, (int) (w * ratio));
        int outH = Math.max(1, (int) (h * ratio));
        Bitmap scaled = Bitmap.createScaledBitmap(src, outW, outH, true);
        return scaled != null ? scaled : src;
    }

    @Nullable
    static int[] bounds(@Nullable Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        return new int[]{bitmap.getWidth(), bitmap.getHeight()};
    }
}
