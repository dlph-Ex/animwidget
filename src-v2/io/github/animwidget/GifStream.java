package io.github.animwidget;

import android.graphics.Bitmap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * GIF 流式解码（v1.10.498）。
 *
 * <p>为什么要有这个类：{@link GifFrames} 与 {@link SourceFps#readAll} 都要求
 * **整份文件在 byte[] 里**。一条 210MB 的 GIF 走
 * {@code ByteStreamsKt.readBytes}（内部 ByteArrayOutputStream 扩容，峰值接近 2×）
 * 直接把堆吃穿；OOM 又在 {@code readBytes} 的 {@code catch (Throwable)} 里被吞成 null，
 * 上层只能报「动图解码失败」，用户看到的就是「这种体积的素材导不进去」。
 *
 * <p>GIF 本身是严格顺序的块结构（LSD → 全局调色板 → 逐块），所以整条链可以只用
 * 「一帧 LZW 数据 + 一份画布」的内存走完：
 * <ul>
 *   <li>{@link #probe} 只走块头，数帧数、累加延时，不解 LZW（一次线性读盘）；</li>
 *   <li>{@link #decode} 逐帧读 LZW 数据子块，解完立刻合成出 Bitmap、回调、回收。</li>
 * </ul>
 *
 * <p>LZW、调色板合成、缩放、dispose 规则全部复用 {@link GifFrames} 的现有实现
 * （那边已把 {@code decode(byte[])} 拆成可按段调用的静态方法），因此两条路径
 * **逐像素结果一致**，不存在「小文件和大文件解出来不一样」。
 *
 * <p>内存：{@code canvas int[W*H]} + {@code previous int[W*H]}（只在 dispose=3 时）
 * + 一帧 LZW 缓冲 + 一帧索引；与文件体积、帧数都无关。
 */
final class GifStream {

    /** 帧序列回调；返回 false 提前结束。Bitmap 只在回调期间有效（返回后由解码器回收）。 */
    interface Sink {
        boolean onFrame(int index, @NotNull Bitmap frame, int delayMs);
    }

    private static final int BLOCK_EXTENSION = 0x21;
    private static final int BLOCK_IMAGE = 0x2C;
    private static final int BLOCK_TRAILER = 0x3B;
    private static final int EXT_GRAPHIC = 0xF9;

    /** 与 {@link GifFrames} 一致的逻辑屏上限（一次 canvas 分配 4 字节/像素）。 */
    private static final int MAX_SCREEN = 4096;

    private GifStream() {
    }

    // ------------------------------------------------------------------ 探测

    /**
     * 只走块结构：尺寸、帧数、每帧延时（不解 LZW）。不是 GIF / 结构崩了返回 null。
     * 与 {@link GifFrames#info(byte[])} 口径完全一致（延时 0 视为「文件没写」，交给 fillDelay 补中位数）。
     */
    @Nullable
    static GifFrames.Meta probe(@Nullable InputStream raw) {
        if (raw == null) {
            return null;
        }
        try {
            BufferedInputStream in = wrap(raw);
            byte[] head = new byte[13];
            readFully(in, head, 0, head.length);
            if (!isGifHeader(head)) {
                return null;
            }
            int width = u16(head, 6);
            int height = u16(head, 8);
            if (width <= 0 || height <= 0 || width > 20000 || height > 20000) {
                return null;
            }
            int packed = head[10] & 255;
            if ((packed & 0x80) != 0) {
                skipFully(in, 3L * (1 << ((packed & 7) + 1)));
            }
            int[] delays = new int[256];
            int count = 0;
            int total = 0;
            boolean estimated = false;
            int pendingDelay = -1;
            while (true) {
                int block = in.read();
                if (block < 0 || block == BLOCK_TRAILER) {
                    break;
                }
                if (block == BLOCK_EXTENSION) {
                    int label = in.read();
                    if (label < 0) {
                        break;
                    }
                    if (label == EXT_GRAPHIC) {
                        byte[] body = readSubBlocks(in);
                        int delayCs = body.length >= 3 ? ((body[1] & 255) | ((body[2] & 255) << 8)) : 0;
                        int delay = GifFrames.clampDelayMs(delayCs);
                        pendingDelay = delay;
                        if (delayCs <= 0) {
                            estimated = true;
                        }
                    } else {
                        skipSubBlocks(in);
                    }
                    continue;
                }
                if (block == BLOCK_IMAGE) {
                    byte[] desc = new byte[9];
                    readFully(in, desc, 0, desc.length);
                    int fPacked = desc[8] & 255;
                    int frameDelay;
                    if (pendingDelay > 0) {
                        frameDelay = pendingDelay;
                    } else {
                        // 这帧压根没有 GCE 延时：100ms 是推算的，必须标出来（UI 显示「≈」）
                        frameDelay = 0;
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
                    if ((fPacked & 0x80) != 0) {
                        skipFully(in, 3L * (1 << ((fPacked & 7) + 1)));
                    }
                    int minCodeSize = in.read();   // LZW 最小码长（跳数据块前先读掉）
                    if (minCodeSize < 0) {
                        break;
                    }
                    skipSubBlocks(in);
                    continue;
                }
                // 未知字节：与 byte[] 版一致地跳过 1 个字节继续（不整段放弃）
                continue;
            }
            if (count <= 0) {
                return null;
            }
            int fill = GifFrames.fillDelay(delays, count);
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
            return new GifFrames.Meta(width, height, count,
                    total > 0 ? total : count * GifFrames.DEFAULT_DELAY_MS,
                    estimated, filled, out);
        } catch (Throwable th) {
            return null;
        }
    }

    // ------------------------------------------------------------------ 解码

    /**
     * 顺序解码：逐帧回调。maxEdge &gt; 0 时把每帧等比缩到该边长内。
     *
     * @param headDelays 探测阶段算出的每帧延时（可空）；非空且对应项 &gt; 0 时以它为准，
     *                   保证「预览/导出」与探测用的是同一份时间轴。
     * @return true 表示至少出过一帧
     */
    static boolean decode(@Nullable InputStream raw, @Nullable int[] headDelays, int maxEdge,
                          @NotNull Sink sink) {
        return decode(raw, headDelays, maxEdge, sink, false);
    }

    /**
     * v1.10.501：{@code sinkOwnsFrame = true} 时**不再回收**交给 sink 的 Bitmap，
     * 所有权转移给调用方（导出并行编码就是这么用的：解码线程把帧交给编码池，由编码线程回收）。
     * 默认 {@code false} 保持原来的「回调期间有效」契约，预览/其它调用方零改动。
     */
    static boolean decode(@Nullable InputStream raw, @Nullable int[] headDelays, int maxEdge,
                          @NotNull Sink sink, boolean sinkOwnsFrame) {
        if (raw == null) {
            return false;
        }
        int frameIndex = 0;
        BufferedInputStream in;
        try {
            in = wrap(raw);
        } catch (Throwable th) {
            return false;
        }
        try {
            byte[] head = new byte[13];
            readFully(in, head, 0, head.length);
            if (!isGifHeader(head)) {
                return false;
            }
            int screenW = u16(head, 6);
            int screenH = u16(head, 8);
            if (screenW <= 0 || screenH <= 0 || screenW > MAX_SCREEN || screenH > MAX_SCREEN) {
                return false;
            }
            int packed = head[10] & 255;
            int[] globalTable = null;
            if ((packed & 0x80) != 0) {
                int n = 1 << ((packed & 7) + 1);
                byte[] rgb = new byte[n * 3];
                readFully(in, rgb, 0, rgb.length);
                globalTable = palette(rgb, n);
            }
            int[] canvas = new int[screenW * screenH];
            int[] previous = null;
            int[] indexes = null;
            Chunks chunks = new Chunks();

            int rawIndex = 0;
            int pendingDelay = 0;
            int lastDelay = 0;
            int pendingTransparent = -1;
            int pendingDispose = 0;

            int prevX = 0;
            int prevY = 0;
            int prevW = 0;
            int prevH = 0;
            int prevDispose = 0;

            while (true) {
                int block = in.read();
                if (block < 0 || block == BLOCK_TRAILER) {
                    break;
                }
                if (block == BLOCK_EXTENSION) {
                    int label = in.read();
                    if (label < 0) {
                        break;
                    }
                    if (label == EXT_GRAPHIC) {
                        byte[] body = readSubBlocks(in);
                        if (body.length >= 3) {
                            int flags = body[0] & 255;
                            int delayCs = (body[1] & 255) | ((body[2] & 255) << 8);
                            int delay = GifFrames.clampDelayMs(delayCs);
                            if (delay > 0) {
                                lastDelay = delay;
                            }
                            pendingDelay = delay;
                            pendingTransparent = (flags & 0x01) != 0 && body.length >= 4
                                    ? (body[3] & 255) : -1;
                            pendingDispose = (flags >> 2) & 0x07;
                        }
                    } else {
                        skipSubBlocks(in);
                    }
                    continue;
                }
                if (block != BLOCK_IMAGE) {
                    // 与 byte[] 版一致：跳过这 1 个字节继续找下一个块
                    continue;
                }
                byte[] desc = new byte[9];
                readFully(in, desc, 0, desc.length);
                int fx = u16(desc, 0);
                int fy = u16(desc, 2);
                int fw = u16(desc, 4);
                int fh = u16(desc, 6);
                int fPacked = desc[8] & 255;
                boolean interlaced = (fPacked & 0x40) != 0;
                int[] palette = globalTable;
                if ((fPacked & 0x80) != 0) {
                    int n = 1 << ((fPacked & 7) + 1);
                    byte[] rgb = new byte[n * 3];
                    readFully(in, rgb, 0, rgb.length);
                    palette = palette(rgb, n);
                }
                int minCodeSize = in.read();
                if (minCodeSize < 0) {
                    break;
                }
                final int rawIdx = rawIndex++;
                byte[] data = chunks.read(in);
                if (palette == null) {
                    // 与原版一致：调色板不可用 → 这帧不出，重置 pending 并沿用 100ms
                    pendingDelay = GifFrames.DEFAULT_DELAY_MS;
                    pendingTransparent = -1;
                    pendingDispose = 0;
                    continue;
                }
                if (fw <= 0 || fh <= 0) {
                    // 与原版一致：尺寸非法的帧直接跳过，**不**动 pending（下一帧沿用这条 GCE）
                    continue;
                }
                // 上一帧的 dispose 规则
                if (prevDispose == 2) {
                    GifFrames.clearRect(canvas, screenW, screenH, prevX, prevY, prevW, prevH);
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
                boolean ok = GifFrames.lzwDecode(data, chunks.length(), minCodeSize, fw * fh, indexes);
                if (ok) {
                    GifFrames.blit(canvas, screenW, screenH, indexes, fw, fh, fx, fy,
                            palette, pendingTransparent, interlaced);
                }
                // 这一帧像素坏了：沿用上一画面照常出帧（与 byte[] 版一致，不中断）

                int delay = (headDelays != null && rawIdx < headDelays.length && headDelays[rawIdx] > 0)
                        ? headDelays[rawIdx]
                        : (pendingDelay > 0 ? pendingDelay
                        : (lastDelay > 0 ? lastDelay : GifFrames.DEFAULT_DELAY_MS));
                int dispose = pendingDispose;
                pendingDelay = 0;
                pendingTransparent = -1;
                pendingDispose = 0;

                prevX = fx;
                prevY = fy;
                prevW = fw;
                prevH = fh;
                prevDispose = dispose;

                Bitmap out = GifFrames.toBitmap(canvas, screenW, screenH, maxEdge);
                if (out == null) {
                    break;
                }
                boolean cont = sink.onFrame(frameIndex, out, delay);
                if (!sinkOwnsFrame && !out.isRecycled()) {
                    out.recycle();
                }
                if (!cont) {
                    return true;
                }
                frameIndex++;
            }
        } catch (EOFException eof) {
            // 流提前结束：已经解出来的帧照样算数（半截文件也不该整段丢弃）
            return frameIndex > 0;
        } catch (IOException io) {
            return frameIndex > 0;
        } catch (Throwable th) {
            return frameIndex > 0;
        }
        return frameIndex > 0;
    }

    // ------------------------------------------------------------------ 内部工具

    private static BufferedInputStream wrap(@NotNull InputStream raw) {
        if (raw instanceof BufferedInputStream && ((BufferedInputStream) raw).markSupported()) {
            // 已经是缓冲流就不再套一层
            return (BufferedInputStream) raw;
        }
        return new BufferedInputStream(raw, 1 << 16);
    }

    private static boolean isGifHeader(@NotNull byte[] head) {
        return head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8'
                && (head[4] == '7' || head[4] == '9') && head[5] == 'a';
    }

    private static int u16(@NotNull byte[] b, int off) {
        return (b[off] & 255) | ((b[off + 1] & 255) << 8);
    }

    private static void readFully(@NotNull InputStream in, @NotNull byte[] dst, int off, int len)
            throws IOException {
        int done = 0;
        while (done < len) {
            int r = in.read(dst, off + done, len - done);
            if (r < 0) {
                throw new EOFException("gif: unexpected end at " + (off + done));
            }
            done += r;
        }
    }

    private static void skipFully(@NotNull InputStream in, long n) throws IOException {
        long left = n;
        while (left > 0) {
            long skipped = in.skip(left);
            if (skipped <= 0) {
                if (in.read() < 0) {
                    throw new EOFException("gif: skip past end");
                }
                left--;
            } else {
                left -= skipped;
            }
        }
    }

    /** 读一个数据子块序列（读到 0x00 终止符），返回数据；调用方用完即弃。 */
    @NotNull
    private static byte[] readSubBlocks(@NotNull InputStream in) throws IOException {
        Chunks chunks = new Chunks();
        chunks.read(in);
        byte[] out = new byte[chunks.length()];
        System.arraycopy(chunks.buf, 0, out, 0, out.length);
        return out;
    }

    /** 跳过一个数据子块序列（不分配）。 */
    private static void skipSubBlocks(@NotNull InputStream in) throws IOException {
        while (true) {
            int size = in.read();
            if (size < 0 || size == 0) {
                return;
            }
            skipFully(in, size);
        }
    }

    @NotNull
    private static int[] palette(@NotNull byte[] rgb, int count) {
        int[] table = new int[count];
        for (int i = 0; i < count; i++) {
            int r = rgb[i * 3] & 255;
            int g = rgb[i * 3 + 1] & 255;
            int b = rgb[i * 3 + 2] & 255;
            table[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        return table;
    }

    /**
     * LZW 数据子块的可复用缓冲（每帧 reset，内存不随帧数增长）。
     * 210MB 的 GIF 单帧 LZW 一般 几百 KB，一路扩容到峰值后就稳定复用。
     */
    private static final class Chunks {
        byte[] buf = new byte[16 * 1024];
        private int len;

        int length() {
            return len;
        }

        @NotNull
        byte[] read(@NotNull InputStream in) throws IOException {
            len = 0;
            while (true) {
                int size = in.read();
                if (size < 0 || size == 0) {
                    break;
                }
                ensure(len + size);
                readFully(in, buf, len, size);
                len += size;
            }
            return buf;
        }

        private void ensure(int need) {
            if (need <= buf.length) {
                return;
            }
            int cap = buf.length;
            while (cap < need) {
                cap <<= 1;
            }
            byte[] grown = new byte[cap];
            System.arraycopy(buf, 0, grown, 0, len);
            buf = grown;
        }
    }
}
