package io.github.animwidget;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.List;

/**
 * v1.10.455（P1）：**动图 WebP 封装器**（RIFF / ANIM / ANMF 写出端）。
 *
 * <p>动机（docs/REVIEW-CONSOLIDATED-447.md 的 P1）：如果桌面组件只需要**一个** URI，
 * 让 launcher 里的 {@code AnimatedImageDrawable} 自己播，就能做到「零 IPC、PlaybackService 可下掉、
 * App 退出后还在动」。问题是 Android 没有公开的动图 WebP 编码 API，本工程也没有 NDK。
 *
 * <p>解：我们早就有「逐帧的单帧 WebP」（烘焙产物 `f%05d.webp` 就是），
 * 而 ANMF 帧内容允许**直接放现成的 WebP 位流块**（ALPH + VP8/VP8L），不需要重新编码。
 * 所以这里只做容器拼装：把 N 个单帧 WebP 的位流块按规范塞进 ANMF，外面套 ANIM/VP8X。
 * 纯 Java、零依赖、一个文件一帧地流式写，内存 ≈ 单帧大小。
 *
 * <p><b>当前接入状态</b>：只被离线自检调用（{@code tests/WebpAnimWriterTest.java} +
 * {@code scripts/webp-anim-check.sh} 用 Pillow 逐帧对像素和时长）。**还没有接进推送链** ——
 * 那一步要把 {@code WidgetUriPush} 的多帧推送换成单 URI，属于观感/架构变更，单独一版做。
 *
 * <p>格式要点（WebP 容器规范 / libwebp mux）：
 * <ul>
 *   <li>{@code VP8X}：flags bit1 = 动画、bit4 = alpha；画布宽高各 3 字节（值 - 1）；</li>
 *   <li>{@code ANIM}：背景色 4 字节（BGRA，仅 dispose-to-background 时可见）+ 循环次数 2 字节（0=无限）；</li>
 *   <li>{@code ANMF}：X/2、Y/2、W-1、H-1、时长(ms) 各 3 字节 + 1 字节 flags；
 *       flags = [6 位保留][B 混合位][D 处置位] ⇒ <b>bit1 = B（1 = 不与上一帧合成，即覆盖）</b>、
 *       <b>bit0 = D（1 = 先处置到背景）</b>。我们要「不混合 + 不处置」= {@code 0x02}。
 *       （v1.10.455 的注释把位序写反过一次，v1.10.497 更正；写错的后果只在局部帧上暴露，
 *       全画布帧两种位序画面一致 —— 外部评审同一份结论，见 {@code scripts/webp-anim-check.py}）</li>
 *   <li>每个子块长度都要补齐到偶数（chunk 内 padding 不计入 size）。</li>
 * </ul>
 */
final class WebpAnimWriter {

    /** 循环次数：0 = 无限。 */
    static final int LOOP_FOREVER = 0;

    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP = {'W', 'E', 'B', 'P'};

    private WebpAnimWriter() {
    }

    /**
     * 把一批「同尺寸单帧 WebP」拼成一个动图 WebP。
     *
     * <p>先写 `out.tmp` 再改名：中途失败不会留下半截文件（launcher 拿到半截 WebP 会直接不显示）。
     *
     * @param out        目标文件
     * @param frames     有序帧文件（单帧 WebP，尺寸必须一致 —— 不一致会返回 false，不猜）
     * @param frameDurMs 每帧时长（毫秒，会夹到 ≥10ms）
     * @return 成功帧数（> 0 才算成功）；失败返回 0
     */
    static int mux(File out, List<File> frames, int frameDurMs, int loop) {
        int[] durs = null;
        if (frames != null) {
            durs = new int[frames.size()];
            for (int i = 0; i < durs.length; i++) {
                durs[i] = frameDurMs;
            }
        }
        return mux(out, frames, durs, loop);
    }

    /**
     * v1.10.474：**逐帧时长**版本。
     *
     * <p>为什么要它：容器里的时长是**整数毫秒**，而 30fps 是 33.333ms ——
     * 每帧都写 33ms，172 帧就比原片短 57ms（1.7%）。用户要的是「播放时长必须等于原文件」，
     * 所以时长必须**按累计时间分配**（33/33/34…），总时长精确到毫秒。
     * 单帧误差仍 ≤1ms（人眼看不出），总时长不漂。</p>
     */
    static int mux(File out, List<File> frames, int[] frameDurs, int loop) {
        if (out == null || frames == null || frames.isEmpty() || frameDurs == null
                || frameDurs.length < frames.size()) {
            return 0;
        }
        final File tmp = new File(out.getAbsolutePath() + ".tmp");
        RandomAccessFile raf = null;
        int canvasW = 0;
        int canvasH = 0;
        boolean anyAlpha = false;
        int written = 0;
        try {
            raf = new RandomAccessFile(tmp, "rw");
            raf.setLength(0L);
            // 头部先占位：RIFF 的长度要等全部帧写完才知道
            raf.write(RIFF);
            writeIntLE(raf, 0);
            raf.write(WEBP);
            long afterHeader = raf.getFilePointer();   // VP8X 的位置，画布尺寸要等第一帧
            // ---- 第一遍：先把第一帧读出来，才知道画布尺寸
            Frame first = readFrame(frames.get(0));
            if (first == null) {
                raf.close();
                raf = null;
                tmp.delete();
                return 0;
            }
            canvasW = first.width;
            canvasH = first.height;
            anyAlpha = first.hasAlpha;
            // ---- VP8X
            raf.write(new byte[]{'V', 'P', '8', 'X'});
            writeIntLE(raf, 10);
            raf.write(first.hasAlpha ? 0x12 : 0x02);   // bit1=动画 bit4=alpha
            raf.write(new byte[]{0, 0, 0});
            write24(raf, canvasW - 1);
            write24(raf, canvasH - 1);
            // ---- ANIM
            raf.write(new byte[]{'A', 'N', 'I', 'M'});
            writeIntLE(raf, 6);
            writeIntLE(raf, 0);                        // 背景色（BGRA）= 全透明
            raf.write(loop & 0xff);
            raf.write((loop >> 8) & 0xff);
            // ---- 帧
            Frame fr = first;
            for (int i = 0; i < frames.size(); i++) {
                if (i > 0) {
                    fr = readFrame(frames.get(i));
                }
                if (fr == null) {
                    break;   // 某一帧坏了：已经写进去的保持完好，按成功帧数返回
                }
                if (fr.width != canvasW || fr.height != canvasH) {
                    // 尺寸不一致绝不硬拼（会出现画面撕裂/偏移），如实返回已写帧数
                    break;
                }
                if (fr.hasAlpha) {
                    anyAlpha = true;
                }
                int payload = (fr.alpha == null ? 0 : fr.alpha.length) + fr.bits.length;
                raf.write(new byte[]{'A', 'N', 'M', 'F'});
                writeIntLE(raf, 16 + payload);
                write24(raf, 0);          // X / 2
                write24(raf, 0);          // Y / 2
                write24(raf, canvasW - 1); // W - 1
                write24(raf, canvasH - 1); // H - 1
                write24(raf, Math.max(10, frameDurs[i]));
                // v1.10.476：**bit0 = dispose、bit1 = blend**（libwebp muxread.c）。
                // 我们要「不混合 + 不处置」= 0x02。旧代码写 0x01，实际语义是
                // 「与上一帧 alpha 混合 + 每帧前清回背景色」—— 因为帧是满画布、ANIM 背景全透明，
                // 像素结果碰巧一样；但只要背景不透明或出现局部矩形帧，就会擦出黑块。
                raf.write(0x02);
                if (fr.alpha != null) {
                    raf.write(fr.alpha);
                }
                raf.write(fr.bits);
                written++;
            }
            if (written <= 0) {
                raf.close();
                raf = null;
                tmp.delete();
                return 0;
            }
            // ---- 回填 RIFF 长度 + VP8X 的 alpha 标志
            long total = raf.getFilePointer();
            raf.seek(4L);
            writeIntLE(raf, (int) (total - 8L));
            if (anyAlpha) {
                raf.seek(afterHeader + 8L);   // 4CC(4) + size(4) 之后就是 flags 字节
                raf.write(0x12);
            }
            raf.close();
            raf = null;
            if (out.exists() && !out.delete()) {
                tmp.delete();
                return 0;
            }
            if (!tmp.renameTo(out)) {
                tmp.delete();
                return 0;
            }
            return written;
        } catch (Throwable th) {
            return 0;
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (Throwable ignored) {
                }
                tmp.delete();
            }
        }
    }

    /** 按 fpsX10 算每帧毫秒（500 = 50.0fps → 20ms）；夹到 10~1000ms。 */
    static int frameDurationMs(int fpsX10) {
        int f = Math.max(fpsX10, 10);
        return Math.max(10, Math.min(1000, 10000 / f));
    }

    // ------------------------------------------------------------------ 解析单帧

    /** 一帧的位流块（已含 chunk 头与 padding，可直接拼进 ANMF）。 */
    private static final class Frame {
        byte[] alpha;
        byte[] bits;
        int width;
        int height;
        boolean hasAlpha;
    }

    /** 读一个单帧 WebP，取出 ALPH + VP8/VP8L 两块，并解析出宽高。 */
    private static Frame readFrame(File file) {
        byte[] d = readAll(file);
        if (d == null || d.length < 20) {
            return null;
        }
        if (!(d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P')) {
            return null;
        }
        Frame fr = new Frame();
        int pos = 12;
        while (pos + 8 <= d.length) {
            String cc = new String(d, pos, 4);
            int size = readIntLE(d, pos + 4);
            if (size < 0 || pos + 8 + size > d.length) {
                break;
            }
            int total = 8 + size + (size & 1);
            if ("ALPH".equals(cc)) {
                fr.alpha = slice(d, pos, total);
                fr.hasAlpha = true;
            } else if ("VP8 ".equals(cc) || "VP8L".equals(cc)) {
                fr.bits = slice(d, pos, total);
                int[] dims = dimsOf(d, pos + 8, size, cc);
                if (dims == null) {
                    return null;
                }
                fr.width = dims[0];
                fr.height = dims[1];
                // v1.10.455（自检抓到的真 bug）：**无损帧的 alpha 不在 ALPH 里，而在 VP8L 头里**。
                // VP8L 头第 28 位就是 alpha_is_used；不看它，VP8X 就不带 alpha 标志，
                // 解码器按不透明处理 → 整帧 alpha 被当成 255（自检：8 帧像素全不一致）。
                if ("VP8L".equals(cc) && dims.length > 2 && dims[2] != 0) {
                    fr.hasAlpha = true;
                }
            }
            pos += total;
        }
        if (fr.bits == null || fr.width <= 0 || fr.height <= 0) {
            return null;
        }
        return fr;
    }

    /** 从 VP8/VP8L 位流里读宽高；VP8L 额外返回第 3 位 = alpha_is_used（0/1）。 */
    private static int[] dimsOf(byte[] d, int off, int size, String cc) {
        try {
            if ("VP8L".equals(cc)) {
                if (size < 5 || (d[off] & 0xff) != 0x2f) {
                    return null;
                }
                int b1 = d[off + 1] & 0xff;
                int b2 = d[off + 2] & 0xff;
                int b3 = d[off + 3] & 0xff;
                int b4 = d[off + 4] & 0xff;
                int w = 1 + (((b2 & 0x3f) << 8) | b1);
                int h = 1 + (((b4 & 0x0f) << 10) | (b3 << 2) | ((b2 & 0xc0) >> 6));
                int alpha = (b4 >> 4) & 0x01;   // bit 28 = alpha_is_used
                return new int[]{w, h, alpha};
            }
            // VP8：3 字节 frame tag + 3 字节起始码 9D 01 2A + 宽高各 2 字节
            if (size < 10) {
                return null;
            }
            if ((d[off + 3] & 0xff) != 0x9d || (d[off + 4] & 0xff) != 0x01
                    || (d[off + 5] & 0xff) != 0x2a) {
                return null;
            }
            int w = ((d[off + 6] & 0xff) | ((d[off + 7] & 0x3f) << 8));
            int h = ((d[off + 8] & 0xff) | ((d[off + 9] & 0x3f) << 8));
            return new int[]{w, h};
        } catch (Throwable th) {
            return null;
        }
    }

    // ------------------------------------------------------------------ 小工具

    private static byte[] readAll(File file) {
        java.io.FileInputStream in = null;
        try {
            in = new java.io.FileInputStream(file);
            long len = file.length();
            if (len <= 0L || len > 32L * 1024L * 1024L) {
                return null;   // 单帧 >32MB 绝不可能是我们的产物
            }
            byte[] buf = new byte[(int) len];
            int got = 0;
            while (got < buf.length) {
                int n = in.read(buf, got, buf.length - got);
                if (n <= 0) {
                    break;
                }
                got += n;
            }
            return got == buf.length ? buf : null;
        } catch (Throwable th) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static byte[] slice(byte[] d, int off, int len) {
        byte[] out = new byte[len];
        System.arraycopy(d, off, out, 0, len);
        return out;
    }

    private static int readIntLE(byte[] d, int off) {
        return (d[off] & 0xff) | ((d[off + 1] & 0xff) << 8)
                | ((d[off + 2] & 0xff) << 16) | ((d[off + 3] & 0xff) << 24);
    }

    private static void writeIntLE(RandomAccessFile raf, int v) throws java.io.IOException {
        raf.write(v & 0xff);
        raf.write((v >> 8) & 0xff);
        raf.write((v >> 16) & 0xff);
        raf.write((v >> 24) & 0xff);
    }

    private static void write24(RandomAccessFile raf, int v) throws java.io.IOException {
        raf.write(v & 0xff);
        raf.write((v >> 8) & 0xff);
        raf.write((v >> 16) & 0xff);
    }
}
