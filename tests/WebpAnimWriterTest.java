package io.github.animwidget;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * v1.10.455：动图 WebP 封装器的**离线自检入口**（不进 APK）。
 *
 * <p>为什么单独一个 main：{@link WebpAnimWriter} 只依赖 java.io，所以可以在纯 JDK 上跑，
 * 不需要 android-all.jar —— 这样门禁里就能端到端验证「拼出来的文件真的能被解码器逐帧读出来」，
 * 而不是只验证"代码看起来对"。配对脚本：{@code scripts/webp-anim-check.sh}（Pillow 逐帧对像素与时长）。
 *
 * <p>用法：{@code java io.github.animwidget.WebpAnimWriterTest <out.webp> <fpsX10> <frame0.webp> ...}
 */
public final class WebpAnimWriterTest {

    public static void main(String[] args) {
        if (args == null || args.length < 3) {
            System.out.println("USAGE: <out.webp> <fpsX10> <frame.webp> ...");
            System.exit(2);
        }
        File out = new File(args[0]);
        int fpsX10 = Integer.parseInt(args[1]);
        List<File> frames = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            frames.add(new File(args[i]));
        }
        int dur = WebpAnimWriter.frameDurationMs(fpsX10);
        int n = WebpAnimWriter.mux(out, frames, dur, WebpAnimWriter.LOOP_FOREVER);
        System.out.println("MUXED " + n + "/" + frames.size() + " dur=" + dur + "ms size=" + out.length());
        if (n != frames.size()) {
            System.out.println("MUX FAILED");
            System.exit(1);
        }
    }

    private WebpAnimWriterTest() {
    }
}
