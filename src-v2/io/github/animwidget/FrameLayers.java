package io.github.animwidget;

/**
 * v1.10.459（#5/#10 地基）：**「内容 / overlay 分层」的判据层**。
 *
 * <p>为什么要分层（只做判据、这一版不改行为）：现在每一帧都由 {@link FrameComposer#compose}
 * 现场画「阴影 + 内容 + 圆角蒙版 + 边框」四样。其中**阴影、蒙版、边框与画面内容无关** ——
 * 它们只由配置决定，300 帧里一模一样。每帧重画一遍等于把同一张静态图渲染 300 次。
 *
 * <p>所以真正该做的是：把「不随帧变化的那一层」合成一次（overlay），之后每帧只合成内容。
 * 但**能不能复用**取决于一个明确判据：决定 overlay 的那些参数有没有变。这里提供的
 * {@link #fingerprint} 就是这个判据 —— 参数完全一致 → 指纹一致 → overlay 可以直接复用。
 *
 * <p>⚠️ 这一版只有判据 + 日志（{@code W41}），**没有**任何行为变化：
 * 不换布局、不换编码、不换推送。真正的分层在后续版本接（见 docs/BRIEF-CONTENT-OVERLAY.md）。
 *
 * <p>另外把一条硬事实钉在这里：**content 帧不能改成 JPEG**。
 * 圆角/阴影/边框全靠像素 alpha，而 JPEG 没有 alpha；RemoteViews 也没有裁剪能力，
 * 所以「不透明内容帧 + 顶部盖一张 alpha overlay」在透明桌面上**必然露出方角**。
 * 内容帧继续用带 alpha 的 WebP；能搬出去的是阴影和边框（它们各自是独立的一层）。
 */
final class FrameLayers {

    private FrameLayers() {
    }

    /**
     * overlay 指纹。**只吃「决定 overlay 的那组参数」**，不吃任何跟画面内容有关的字段
     * （帧率、质量、帧数、素材地址都不参与）。
     *
     * @param layout        布局（fits 影响内容框，也影响阴影包围盒 → 必须算进来）
     * @param shape         蒙版形状
     * @param alphaPercent  透明度
     * @param border        边框
     * @param scalePercent  内容缩放
     * @param rotation      内容旋转
     * @param shapeRotation 蒙版旋转
     * @param fitContent    「适配内容」开关
     * @param shadowPercent 阴影强度
     * @param shadowCurve   阴影曲线（默认曲线是空串）
     * @param offsetX       水平偏移（%）
     * @param offsetY       垂直偏移（%）
     * @param contentScale  内容缩放（%）
     * @param maskOffsetX   蒙版水平偏移（%）
     * @param maskOffsetY   蒙版垂直偏移（%）
     * @param width         画布宽（像素）
     * @param height        画布高（像素）
     */
    static String fingerprint(@org.jetbrains.annotations.NotNull String layout, @org.jetbrains.annotations.NotNull String shape,
                              int alphaPercent, boolean border, int scalePercent, int rotation,
                              int shapeRotation, boolean fitContent, int shadowPercent,
                              @org.jetbrains.annotations.Nullable String shadowCurve,
                              float offsetX, float offsetY, int contentScale,
                              float maskOffsetX, float maskOffsetY, int width, int height) {
        StringBuilder sb = new StringBuilder(96);
        sb.append(layout).append('|').append(shape)
                .append('|').append(alphaPercent)
                .append('|').append(border ? 1 : 0)
                .append('|').append(scalePercent)
                .append('|').append(rotation)
                .append('|').append(shapeRotation)
                .append('|').append(fitContent ? 1 : 0)
                .append('|').append(shadowPercent)
                .append('|').append(shadowCurve == null ? "" : shadowCurve)
                .append('|').append(q(offsetX)).append(',').append(q(offsetY))
                .append('|').append(contentScale)
                .append('|').append(q(maskOffsetX)).append(',').append(q(maskOffsetY))
                .append('|').append(width).append('x').append(height);
        // FNV-1a 64：只用来判「变没变」，不做安全用途
        long hash = -3750763034362895579L;
        for (int i = 0; i < sb.length(); i++) {
            hash ^= sb.charAt(i);
            hash *= 1099511628211L;
        }
        return Long.toHexString(hash);
    }

    /** 参数浮点用「两位小数」参战：0.30 与 0.30000001 必须算同一个指纹。 */
    private static String q(float v) {
        return String.format(java.util.Locale.US, "%.2f", v);
    }

    /** 日志用的一行摘要（不含内容相关字段）。 */
    @org.jetbrains.annotations.NotNull
    static String summary(@org.jetbrains.annotations.NotNull String shape, boolean border,
                          int shadowPercent, int shapeRotation, int alphaPercent) {
        return "蒙版=" + shape + " 边框=" + (border ? "有" : "无")
                + " 阴影=" + shadowPercent + " 蒙版旋转=" + shapeRotation + "°"
                + " 透明度=" + alphaPercent + "%";
    }
}
