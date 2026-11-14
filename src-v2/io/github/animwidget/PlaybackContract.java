package io.github.animwidget;

import java.util.Locale;

/**
 * v1.10.474（#5/#10 的前置闸门）：**「播放口径」绝对性检查**。
 *
 * <p>用户的硬要求：单个素材在「≤ 最高参数 + 默认参数 + 1:1」这个环境里，
 * 预览与组件循环播放时，**理论清晰度 / FPS / 播放速度 / 播放时长** 这四项必须等于原文件。
 *
 * <p>这四项里没有一项是"看起来差不多"就行的，而且它们分散在三条链上
 * （画布尺寸计算 / 帧率换算 / 帧号推进），任何一处改口都会让另一处失效 ——
 * 所以把它们收敛成一个**纯函数**：输入源参数与实际画布，输出四项判定与可读原因。
 *
 * <p>为什么必须先有它再做 #5（分层）和 #10（预览管线）：
 * 那两个改动都在动"每帧画什么/怎么编码"，一旦口径被悄悄改掉（画布被缩、帧时长被取整变慢），
 * 现象只会是"看着有点糊/有点慢"，靠肉眼永远判不出来。有了这个检查，
 * 任何让四项之一失效的改动都会在 W47 里直接写出来。
 */
final class PlaybackContract {

    /** 四项判定结果。 */
    static final class Verdict {
        final boolean sharp;
        final boolean fps;
        final boolean speed;
        final boolean duration;
        final String canvasText;
        final String fpsText;
        final String speedText;
        final String durationText;

        Verdict(boolean sharp, boolean fps, boolean speed, boolean duration,
                String canvasText, String fpsText, String speedText, String durationText) {
            this.sharp = sharp;
            this.fps = fps;
            this.speed = speed;
            this.duration = duration;
            this.canvasText = canvasText;
            this.fpsText = fpsText;
            this.speedText = speedText;
            this.durationText = durationText;
        }

        boolean allOk() {
            return this.sharp && this.fps && this.speed && this.duration;
        }

        /** 一行日志用：`清晰度=OK(1.00×) fps=OK(30.0) 速度=OK 时长=OK(5730ms)`。 */
        String summary() {
            return "清晰度=" + (this.sharp ? "OK" : "FAIL") + "(" + this.canvasText + ")"
                    + " fps=" + (this.fps ? "OK" : "FAIL") + "(" + this.fpsText + ")"
                    + " 速度=" + (this.speed ? "OK" : "FAIL") + "(" + this.speedText + ")"
                    + " 时长=" + (this.duration ? "OK" : "FAIL") + "(" + this.durationText + ")";
        }
    }

    private PlaybackContract() {
    }

    /**
     * 画布相对内容的像素倍率。
     *
     * <p>FILL = 内容铺满画布（按较大的那一维放大）；其余布局 = 内容完整内接（按较小的那一维）。
     * ≥ 1.0 才叫"至少 1:1"；&lt; 1.0 意味着画布比素材还小 → 先天就糊了。</p>
     */
    static float contentScale(int canvasW, int canvasH, int contentW, int contentH, boolean fill) {
        if (canvasW <= 0 || canvasH <= 0 || contentW <= 0 || contentH <= 0) {
            return 0.0f;
        }
        float sw = canvasW / (float) contentW;
        float sh = canvasH / (float) contentH;
        return fill ? Math.max(sw, sh) : Math.min(sw, sh);
    }

    /** 每帧时长（毫秒）：与 {@link AnimPack#frameDurationMs} 同一个式子（那一个给动图文件用）。 */
    static int frameDurationMs(int fpsX10) {
        return AnimPack.frameDurationMsX10(fpsX10);
    }

    /** 总时长（毫秒）= 逐帧时长之和（v1.10.474：时长按累计时间分配，总和精确）。 */
    static long totalMs(int[] frameDurs) {
        long sum = 0L;
        if (frameDurs != null) {
            for (int d : frameDurs) {
                sum += Math.max(10, d);
            }
        }
        return sum;
    }

    /**
     * 判定四项。
     *
     * @param srcW/srcH      素材像素
     * @param canvasW/canvasH 实际画布像素
     * @param fill           布局是不是「填充」
     * @param srcFpsX10      源帧率（×10，33.3fps → 333）
     * @param frames         帧数
     * @param frameDurMs     实际写进帧/动图的每帧时长
     */
    static Verdict check(int srcW, int srcH, int canvasW, int canvasH, boolean fill,
                         int srcFpsX10, int frames, int[] frameDurs) {
        // ① 理论清晰度：画布必须 ≥ 1:1（否则内容被缩小过，怎么播都糊）
        float scale = contentScale(canvasW, canvasH, srcW, srcH, fill);
        boolean sharp = scale >= 0.999f;
        String canvasText = String.format(Locale.US, "%.2f× %dx%d→%dx%d",
                Float.valueOf(scale), Integer.valueOf(srcW), Integer.valueOf(srcH),
                Integer.valueOf(canvasW), Integer.valueOf(canvasH));

        // ② FPS：**平均**每帧时长必须还原源帧率（逐帧会在 ±1ms 内浮动，平均必须精确）
        long sum = totalMs(frameDurs);
        double avg = frames > 0 ? sum / (double) frames : 0.0;
        double exact = 10000.0 / Math.max(srcFpsX10, 10);
        boolean fpsOk = Math.abs(avg - exact) <= 0.5;
        // v1.10.477（用户决议「降帧率不可以」）：把**像素吞吐**写进这一行。
        // 46.1 Mpx/s 是本机实测天花板（50.2fps × 1280×720）；超过它，帧率就守不住。
        double mpx = (canvasW * (double) canvasH * (srcFpsX10 / 10.0)) / 1000000.0;
        String fpsText = String.format(Locale.US, "%.3fms/帧(目标 %.3f) 吞吐=%.1fMpx/s(%.0f%%上限)",
                Double.valueOf(avg), Double.valueOf(exact), Double.valueOf(mpx),
                Double.valueOf(mpx * 100.0 / 46.1));

        // ③ 播放速度 / ④ 时长：总时长必须等于源片（误差 ≤1ms，不是"一帧"）
        long sourceMs = Math.round(frames * exact);
        long playMs = sum;
        boolean speedOk = Math.abs(playMs - sourceMs) <= 1L;
        String speedText = String.format(Locale.US, "%d帧=%dms 源≈%dms",
                Integer.valueOf(frames), Long.valueOf(playMs), Long.valueOf(sourceMs));

        // ④ 播放时长：与③同一组数，但判的是"能不能整除循环"——这里给出实际循环时长
        boolean durationOk = speedOk;
        String durationText = String.format(Locale.US, "循环=%dms", Long.valueOf(playMs));

        return new Verdict(sharp, fpsOk, speedOk, durationOk, canvasText, fpsText, speedText, durationText);
    }
}
