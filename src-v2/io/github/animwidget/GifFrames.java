package io.github.animwidget;

import android.graphics.Bitmap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 手写 GIF 解码器（v1.10.0，零依赖）。
 *
 * 为什么不用 {@code android.graphics.Movie}：
 * 1) Movie 自己解析帧延时，对「延时为 0 / 极短」的帧会按内部规则改长，
 *    和文件里写的字节不一致 —— 我们按字节取 t 去 setTime，取回来的却可能是上一帧，
 *    表现就是「预览/导出抽帧、帧数对不上、和原图观感差一截」。
 * 2) Movie 读不出帧数，帧率只能靠猜（以前直接留空 → 落到默认 12fps）。
 * 3) Movie.duration() 对部分 GIF 返回 0（v1.9.0 踩过）。
 *
 * 这里按 GIF89a 规范自己走一遍：
 *   LSD → 全局调色板 → 逐块（GCE / 图像描述符 / 其他扩展）
 *   图像数据用标准 LZW 解出索引 → 按 dispose/blend 合成到画布
 * 每帧的真实显示时长从 GCE 读（单位 1/100 s），并按浏览器/GIF 通行规则
 * 把「0~1（<20ms）」的帧当 100ms（这是观感基准，不是我们自己编的）。
 *
 * 只做顺序解码（一次一帧），调用方边解边用，内存与帧数无关。
 */
final class GifFrames {

    /** v1.10.134：**1cs 就是 10ms**，不再补 100ms（ffmpeg fps=80 的 GIF 靠这条被误算成 6 倍长）。 */
    static final int DEFAULT_DELAY_MS = 100;

    private static final int EXT_GRAPHIC = 0xF9;
    private static final int BLOCK_EXTENSION = 0x21;
    private static final int BLOCK_IMAGE = 0x2C;
    private static final int BLOCK_TRAILER = 0x3B;

    /** 帧序列回调；返回 false 提前结束。Bitmap 只在回调期间有效（返回后由解码器回收）。 */
    interface Sink {
        boolean onFrame(int index, @NotNull Bitmap frame, int delayMs);
    }

    /** 轻量信息（不解像素）：帧数、总时长、每帧延时。 */
    static final class Meta {
        final int width;
        final int height;
        final int frames;
        final int totalDelayMs;
        final boolean estimated;
        /** v1.10.135：**有多少帧的延时是「补」出来的**（文件里没写），日志必须标出来。 */
        final int filled;
        @NotNull
        final int[] delays;

        Meta(int width, int height, int frames, int totalDelayMs, boolean estimated,
             int filled, @NotNull int[] delays) {
            this.width = width;
            this.height = height;
            this.frames = frames;
            this.totalDelayMs = totalDelayMs;
            this.estimated = estimated;
            this.filled = filled;
            this.delays = delays;
        }
    }

    private GifFrames() {
    }

    /** 是不是 GIF 的魔数。 */
    static boolean isGif(@Nullable byte[] d) {
        return d != null && d.length >= 13
                && d[0] == 'G' && d[1] == 'I' && d[2] == 'F' && d[3] == '8';
    }

    /** 逻辑屏幕尺寸（只读头部 10 字节）。 */
    static int[] size(@Nullable byte[] d) {
        if (!isGif(d) || d.length < 10) {
            return null;
        }
        int w = (d[6] & 255) | ((d[7] & 255) << 8);
        int h = (d[8] & 255) | ((d[9] & 255) << 8);
        if (w <= 0 || h <= 0 || w > 20000 || h > 20000) {
            return null;
        }
        return new int[]{w, h};
    }

    /**
     * 帧数 + 每帧延时（只走块结构，不解 LZW，很快）。
     * 不是 GIF / 结构崩了返回 null。
     */
    @Nullable
    static Meta info(@Nullable byte[] d) {
        if (!isGif(d)) {
            return null;
        }
        int[] size = size(d);
        if (size == null) {
            return null;
        }
        int packed = d[10] & 255;
        int pos = (packed & 0x80) != 0 ? 13 + 3 * (1 << ((packed & 7) + 1)) : 13;
        int[] delays = new int[256];
        int count = 0;
        int total = 0;
        boolean estimated = false;
        int pendingDelay = -1;
        int guard = 0;
        while (pos >= 0 && pos < d.length && guard++ < 1000000) {
            int block = d[pos] & 255;
            if (block == BLOCK_TRAILER) {
                break;
            }
            if (block == BLOCK_EXTENSION) {
                int label = pos + 1 < d.length ? d[pos + 1] & 255 : -1;
                if (label == EXT_GRAPHIC) {
                    int delay = readDelayCs(d, pos);
                    pendingDelay = clampDelayMs(delay);
                    if (delay <= 0) {
                        estimated = true;
                    }
                }
                pos = skipSubBlocks(d, pos + 2);
                continue;
            }
            if (block == BLOCK_IMAGE) {
                if (pos + 10 > d.length) {
                    break;
                }
                int frameDelay;
                if (pendingDelay > 0) {
                    frameDelay = pendingDelay;
                } else {
                    // v1.10.27（需求 3）：这帧压根没有 GCE 延时 —— 100ms 是我们推算的，
                    // 必须打上标记（UI 上会显示「≈」），不能当成「识别到的确切帧率」
                    frameDelay = 0;   // 0 = 未声明，下面 fillDelay 用文件内中位数补
                    estimated = true;
                }
                pendingDelay = -1;
                if (count == delays.length) {
                    int[] grown = new int[delays.length * 2];
                    System.arraycopy(delays, 0, grown, 0, delays.length);
                    delays = grown;
                }
                delays[count] = frameDelay;
                count++;
                int local = d[pos + 9] & 255;
                int localTable = (local & 0x80) != 0 ? 3 * (1 << ((local & 7) + 1)) : 0;
                // 描述符 10 字节 + 局部调色板 + 1 字节 LZW 最小码长 + 数据子块
                pos = skipSubBlocks(d, pos + 10 + localTable + 1);
                continue;
            }
            pos++;
        }
        if (count <= 0) {
            return null;
        }
        int fill = fillDelay(delays, count);
        int[] out = new int[count];
        for (int i = 0; i < count; i++) {
            int v = delays[i] > 0 ? delays[i] : fill;
            out[i] = v;
            total += v;
        }
        int filled = 0;
        for (int i = 0; i < count; i++) {
            if (out[i] > 0 && delays[i] <= 0) {
                filled++;
            }
        }
        return new Meta(size[0], size[1], count, total > 0 ? total : count * DEFAULT_DELAY_MS,
                estimated, filled, out);
    }

    /**
     * 顺序解码：逐帧回调。maxEdge > 0 时把每帧等比缩到该边长内。
     * 返回 true 表示至少出过一帧。
     */
    static boolean decode(@Nullable byte[] d, int maxEdge, @NotNull Sink sink) {
        if (!isGif(d)) {
            return false;
        }
        int[] size = size(d);
        if (size == null) {
            return false;
        }
        int screenW = size[0];
        int screenH = size[1];
        if (screenW > 4096 || screenH > 4096) {
            // 逻辑屏幕过大（恶意/异常文件）直接放弃，避免一次分配几百 MB
            return false;
        }
        int packed = d[10] & 255;
        int pos = 13;
        int[] globalTable = null;
        if ((packed & 0x80) != 0) {
            int gctSize = 1 << ((packed & 7) + 1);
            globalTable = readPalette(d, pos, gctSize, null);
            pos += gctSize * 3;
        }
        int[] canvas = new int[screenW * screenH];
        Meta headMeta = info(d);
        int[] headDelays = headMeta != null ? headMeta.delays : null;
        int rawIndex = 0;   // 与 info() 一致：每个图像描述符 +1
        int[] previous = null;
        int[] indexes = null;

        int pendingDelay = 0;
        int lastDelay = 0;   // v1.10.134：本帧没写延时时沿用上一帧的（同一文件通常只有一种节奏）
        int pendingTransparent = -1;
        int pendingDispose = 0;
        int frameIndex = 0;

        int prevX = 0;
        int prevY = 0;
        int prevW = 0;
        int prevH = 0;
        int prevDispose = 0;
        int prevTransparent = -1;

        int guard = 0;
        while (pos >= 0 && pos < d.length && guard++ < 1000000) {
            int block = d[pos] & 255;
            if (block == BLOCK_TRAILER) {
                break;
            }
            if (block == BLOCK_EXTENSION) {
                int label = pos + 1 < d.length ? d[pos + 1] & 255 : -1;
                if (label == EXT_GRAPHIC) {
                    int flags = pos + 3 < d.length ? d[pos + 3] & 255 : 0;
                    int delay = clampDelayMs(readDelayCs(d, pos));
                    if (delay > 0) {
                        lastDelay = delay;
                    }
                    pendingDelay = delay;
                    pendingTransparent = (flags & 0x01) != 0 && pos + 6 < d.length ? d[pos + 6] & 255 : -1;
                    pendingDispose = (flags >> 2) & 0x07;
                }
                pos = skipSubBlocks(d, pos + 2);
                continue;
            }
            if (block != BLOCK_IMAGE) {
                pos++;
                continue;
            }
            if (pos + 10 > d.length) {
                break;
            }
            final int rawIdx = rawIndex++;
            int fx = (d[pos + 1] & 255) | ((d[pos + 2] & 255) << 8);
            int fy = (d[pos + 3] & 255) | ((d[pos + 4] & 255) << 8);
            int fw = (d[pos + 5] & 255) | ((d[pos + 6] & 255) << 8);
            int fh = (d[pos + 7] & 255) | ((d[pos + 8] & 255) << 8);
            int fPacked = d[pos + 9] & 255;
            boolean interlaced = (fPacked & 0x40) != 0;
            int[] palette = globalTable;
            int cursor = pos + 10;
            if ((fPacked & 0x80) != 0) {
                int lctSize = 1 << ((fPacked & 7) + 1);
                palette = readPalette(d, cursor, lctSize, null);   // 不复用：否则会原地覆盖全局调色板
                cursor += lctSize * 3;
            }
            if (palette == null || cursor >= d.length) {
                pos = skipSubBlocks(d, cursor);
                pendingDelay = DEFAULT_DELAY_MS;
                pendingTransparent = -1;
                pendingDispose = 0;
                continue;
            }
            int minCodeSize = d[cursor] & 255;
            cursor++;
            byte[] data = collectSubBlocks(d, cursor);
            pos = skipSubBlocks(d, cursor);
            if (fw <= 0 || fh <= 0) {
                continue;
            }
            // 上一帧的 dispose 规则
            if (prevDispose == 2) {
                clearRect(canvas, screenW, screenH, prevX, prevY, prevW, prevH);
            } else if (prevDispose == 3 && previous != null) {
                System.arraycopy(previous, 0, canvas, 0, Math.min(previous.length, canvas.length));
            }
            if (prevDispose == 3) {
                previous = null;
            }
            if (pendingDispose == 3) {
                if (previous == null || previous.length != canvas.length) {
                    previous = new int[canvas.length];
                }
                System.arraycopy(canvas, 0, previous, 0, canvas.length);
            }
            if (indexes == null || indexes.length < fw * fh) {
                indexes = new int[fw * fh];
            }
            boolean ok = lzwDecode(data, minCodeSize, fw * fh, indexes);
            if (ok) {
                blit(canvas, screenW, screenH, indexes, fw, fh, fx, fy, palette, pendingTransparent, interlaced);
            }
            // 这一帧像素坏了：沿用上一画面照常出帧（原来直接 break，后面所有帧都丢）

            int delay = (headDelays != null && rawIdx < headDelays.length && headDelays[rawIdx] > 0)
                    ? headDelays[rawIdx]
                    : (pendingDelay > 0 ? pendingDelay
                    : (lastDelay > 0 ? lastDelay : DEFAULT_DELAY_MS));
            int transparent = pendingTransparent;
            int dispose = pendingDispose;
            pendingDelay = 0;
            pendingTransparent = -1;
            pendingDispose = 0;

            prevX = fx;
            prevY = fy;
            prevW = fw;
            prevH = fh;
            prevDispose = dispose;
            prevTransparent = transparent;

            Bitmap out = toBitmap(canvas, screenW, screenH, maxEdge);
            if (out == null) {
                break;
            }
            boolean cont = sink.onFrame(frameIndex, out, delay);
            // v1.10.0：帧只在回调期间有效 —— 回调返回后立即回收，内存与帧数无关
            if (!out.isRecycled()) {
                out.recycle();
            }
            if (!cont) {
                return true;
            }
            frameIndex++;
        }
        return frameIndex > 0;
    }

    // ---------------------------------------------------------------- 内部工具

    static int clampDelayMs(int centiseconds) {
        // v1.10.134（实测翻车的根因）：**1cs 就是 10ms，不再按浏览器规则补 100ms**。
        // ffmpeg `fps=80` 转出来的 GIF，每帧写的是 1cs/2cs 交替（本意 12.5ms），
        // 原来 <2cs 一律补 100ms → 947 帧被算成 75.7 秒，而它实际只有 11.8 秒。
        if (centiseconds <= 0) {
            return 0;   // 0 = 文件没声明，交给上层用「同文件里已声明的延时中位数」补
        }
        return centiseconds * 10;
    }

    /** 把「没声明延时」的帧补成本文件里已声明延时的**中位数**（一个文件通常只有一种节奏）。 */
    static int fillDelay(@NotNull int[] delays, int count) {
        int[] vals = new int[count];
        int n = 0;
        for (int i = 0; i < count; i++) {
            if (delays[i] > 0) {
                vals[n++] = delays[i];
            }
        }
        if (n <= 0) {
            return DEFAULT_DELAY_MS;   // 整个文件都没写延时：仍按浏览器的 100ms
        }
        java.util.Arrays.sort(vals, 0, n);
        return vals[n / 2];
    }

    private static int readDelayCs(@NotNull byte[] d, int extPos) {
        if (extPos + 5 >= d.length) {
            return 0;
        }
        return (d[extPos + 4] & 255) | ((d[extPos + 5] & 255) << 8);
    }

    private static int[] readPalette(@NotNull byte[] d, int start, int count, @Nullable int[] reuse) {
        if (start < 0 || start + count * 3 > d.length) {
            return null;
        }
        int[] table = reuse != null && reuse.length >= count ? reuse : new int[count];
        for (int i = 0; i < count; i++) {
            int r = d[start + i * 3] & 255;
            int g = d[start + i * 3 + 1] & 255;
            int b = d[start + i * 3 + 2] & 255;
            table[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        return table;
    }

    /** 跳过数据子块，返回终止符（0x00）之后的位置。 */
    private static int skipSubBlocks(@NotNull byte[] d, int start) {
        int pos = start;
        while (pos >= 0 && pos < d.length) {
            int size = d[pos] & 255;
            pos++;
            if (size == 0) {
                return pos;
            }
            pos += size;
        }
        return Math.min(Math.max(pos, 0), d.length);
    }

    @Nullable
    private static byte[] collectSubBlocks(@NotNull byte[] d, int start) {
        int pos = start;
        int total = 0;
        while (pos < d.length) {
            int size = d[pos] & 255;
            pos++;
            if (size == 0) {
                break;
            }
            total += size;
            pos += size;
        }
        if (total <= 0) {
            return new byte[0];
        }
        byte[] out = new byte[total];
        pos = start;
        int at = 0;
        while (pos < d.length) {
            int size = d[pos] & 255;
            pos++;
            if (size == 0) {
                break;
            }
            System.arraycopy(d, pos, out, at, size);
            at += size;
            pos += size;
        }
        return out;
    }

    /** 标准 GIF LZW 解码（LSB 优先，码长可变）。 */
    static boolean lzwDecode(@NotNull byte[] data, int minCodeSize, int pixelCount, @NotNull int[] out) {
        return lzwDecode(data, data.length, minCodeSize, pixelCount, out);
    }

    /**
     * v1.10.498：同上，但只解 {@code data[0, dataLen)}。
     * 流式解码器 {@link GifStream} 复用一个 grow-only 缓冲，长度比真实数据大，
     * 传 data.length 会把缓冲区尾巴的垃圾字节也解进去 —— 那是花屏/错帧的根源。
     */
    static boolean lzwDecode(@NotNull byte[] data, int dataLen, int minCodeSize, int pixelCount,
                             @NotNull int[] out) {
        if (minCodeSize < 2 || minCodeSize > 11 || pixelCount <= 0) {
            return false;
        }
        final int clearCode = 1 << minCodeSize;
        final int endCode = clearCode + 1;
        int codeSize = minCodeSize + 1;
        int available = clearCode + 2;

        int[] prefix = new int[4096];
        byte[] suffix = new byte[4096];
        byte[] stack = new byte[4096];
        for (int i = 0; i < clearCode; i++) {
            prefix[i] = -1;
            suffix[i] = (byte) i;
        }

        int outPos = 0;
        int old = -1;
        int firstChar = 0;
        int bitBuffer = 0;
        int bitCount = 0;
        int bytePos = 0;

        while (outPos < pixelCount) {
            while (bitCount < codeSize) {
                if (bytePos >= dataLen) {
                    return outPos == pixelCount;
                }
                bitBuffer |= (data[bytePos++] & 255) << bitCount;
                bitCount += 8;
            }
            int code = bitBuffer & ((1 << codeSize) - 1);
            bitBuffer >>>= codeSize;
            bitCount -= codeSize;

            if (code == endCode) {
                return outPos == pixelCount;
            }
            if (code == clearCode) {
                codeSize = minCodeSize + 1;
                available = clearCode + 2;
                old = -1;
                continue;
            }
            if (old == -1) {
                if (code >= available) {
                    return false;
                }
                out[outPos++] = suffix[code] & 255;
                firstChar = suffix[code] & 255;
                old = code;
                continue;
            }
            int sp = 0;
            int code2 = code;
            if (code2 >= available) {
                // KwKwK：码值等于「下一个待分配码」时，本串 = 上一串 + 上一串的首字节。
                // 栈是「倒序压、正序弹」，所以这条补在最前面压（最后弹出）。
                if (code2 > available) {
                    return false;
                }
                stack[sp++] = (byte) firstChar;
                code2 = old;
            }
            int guard = 0;
            while (code2 != -1) {
                if (code2 < 0 || code2 >= available || sp >= stack.length) {
                    return false;
                }
                stack[sp++] = suffix[code2];
                code2 = prefix[code2];
                if (++guard > 4200) {
                    return false;
                }
            }
            if (sp <= 0) {
                return false;
            }
            byte first = stack[sp - 1];
            while (sp > 0 && outPos < pixelCount) {
                out[outPos++] = stack[--sp] & 255;
            }
            firstChar = first & 255;
            if (available < 4096) {
                prefix[available] = old;
                suffix[available] = first;
                available++;
                if (available == (1 << codeSize) && codeSize < 12) {
                    codeSize++;
                }
            }
            old = code;
        }
        return true;
    }

    static void blit(@NotNull int[] canvas, int screenW, int screenH, @NotNull int[] indexes,
                             int fw, int fh, int fx, int fy, @NotNull int[] palette, int transparent, boolean interlaced) {
        int paletteSize = palette.length;
        final int[] passStart = {0, 4, 2, 1};
        final int[] passStep = {8, 8, 4, 2};
        int pass = 0;
        int passRow = 0;
        for (int y = 0; y < fh; y++) {
            int destY;
            if (!interlaced) {
                destY = fy + y;
            } else {
                while (pass < 4 && passRow >= interlacedRows(fh, pass)) {
                    passRow = 0;
                    pass++;
                }
                if (pass >= 4) {
                    break;
                }
                destY = fy + passStart[pass] + passRow * passStep[pass];
                passRow++;
            }
            if (destY < 0 || destY >= screenH) {
                continue;
            }
            int src = y * fw;
            int dstBase = destY * screenW;
            for (int x = 0; x < fw; x++) {
                int idx = indexes[src + x];
                if (idx == transparent) {
                    continue;
                }
                int col = fx + x;
                if (col < 0 || col >= screenW) {
                    continue;
                }
                canvas[dstBase + col] = idx < paletteSize ? palette[idx] : 0xFFFFFFFF;
            }
        }
    }

    static int interlacedRows(int height, int pass) {
        switch (pass) {
            case 0:
                return (height + 7) >> 3;
            case 1:
                return (height + 3) >> 3;
            case 2:
                return (height + 1) >> 2;
            default:
                return height >> 1;
        }
    }

    static void clearRect(@NotNull int[] canvas, int screenW, int screenH, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int x2 = Math.min(x + w, screenW);
        int y2 = Math.min(y + h, screenH);
        int x1 = Math.max(x, 0);
        int y1 = Math.max(y, 0);
        for (int row = y1; row < y2; row++) {
            int base = row * screenW;
            for (int col = x1; col < x2; col++) {
                canvas[base + col] = 0;
            }
        }
    }

    @Nullable
    static Bitmap toBitmap(@NotNull int[] canvas, int w, int h, int maxEdge) {
        try {
            Bitmap full = Bitmap.createBitmap(canvas, w, h, Bitmap.Config.ARGB_8888);
            if (full == null) {
                return null;
            }
            int longest = Math.max(w, h);
            if (maxEdge <= 0 || longest <= maxEdge) {
                return full;
            }
            float ratio = maxEdge / (float) longest;
            int outW = Math.max(1, (int) (w * ratio));
            int outH = Math.max(1, (int) (h * ratio));
            Bitmap scaled = Bitmap.createScaledBitmap(full, outW, outH, true);
            if (scaled != full) {
                full.recycle();
            }
            return scaled;
        } catch (Throwable th) {
            return null;
        }
    }
}
