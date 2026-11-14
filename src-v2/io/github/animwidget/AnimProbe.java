package io.github.animwidget;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * v1.10.177（反馈）：**大体积动图的时长**。
 *
 * 上一版要求「整个文件读进 `byte[]`」才能走块结构，于是 > 8MB 的素材直接放弃 ——
 * 偏偏「算不出时长」的就是那些又长又大的。这里改成**流式**走容器结构：
 *   · 内存固定（64KB 缓冲），与文件多大无关，不再有 8MB 上限；
 *   · 只读块头、跳过数据体，**不碰一帧像素、不做 LZW、不建 Bitmap**；
 *   · 规则与内存版**共用同一批函数**（{@link GifFrames#clampDelayMs} /
 *     {@link GifFrames#fillDelay} / {@link ApngFrames#frameDelayMs}），
 *     所以「流式」和「内存」两种走法算出来的时长必然一致（已对齐验证）。
 *
 * 返回统一是 `{时长ms, 是否估算(0/1), 宽, 高}`；认出格式但解不出返回 null。
 */
final class AnimProbe {

    private AnimProbe() {
    }

    /** 按嗅探结果分派。传入的 `InputStream` 会被包装成 64KB 缓冲，调用方负责关闭。 */
    static int[] probe(InputStream raw, String sniff) {
        if (raw == null || sniff == null) {
            return null;
        }
        InputStream in = raw instanceof BufferedInputStream
                ? raw : new BufferedInputStream(raw, 1 << 16);
        try {
            if ("gif".equals(sniff)) {
                return gif(in);
            }
            if ("apng".equals(sniff)) {
                return apng(in);
            }
            if ("webp-anim".equals(sniff)) {
                return webp(in);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    // ------------------------------------------------------------------ GIF

    private static int[] gif(InputStream in) throws IOException {
        byte[] head = new byte[13];
        if (!readFully(in, head, 13)) {
            return null;
        }
        if (head[0] != 'G' || head[1] != 'I' || head[2] != 'F' || head[3] != '8') {
            return null;
        }
        int w = u16(head, 6);
        int h = u16(head, 8);
        if (w <= 0 || h <= 0) {
            return null;
        }
        int packed = head[10] & 0xFF;
        if ((packed & 0x80) != 0 && !skip(in, 3L * (1 << ((packed & 7) + 1)))) {
            return null;
        }
        int[] delays = new int[64];
        int count = 0;
        boolean estimated = false;
        int pending = -1;
        while (true) {
            int block = in.read();
            if (block < 0 || block == 0x3B) {
                break;
            }
            if (block == 0x21) {
                // 扩展块：0x21 <label> <子块链>
                int label = in.read();
                if (label < 0) {
                    break;
                }
                if (label == 0xF9) {
                    // 图形控制：size(4) flags delayLo delayHi transparent 00
                    int size = in.read();
                    if (size < 0) {
                        break;
                    }
                    if (size < 4) {
                        if (!skipSubBlocks(in)) {
                            break;
                        }
                        continue;
                    }
                    int flags = in.read();
                    int lo = in.read();
                    int hi = in.read();
                    int transparent = in.read();
                    if (flags < 0 || lo < 0 || hi < 0 || transparent < 0) {
                        break;
                    }
                    int cs = lo | (hi << 8);
                    pending = GifFrames.clampDelayMs(cs);
                    if (cs <= 0) {
                        estimated = true;
                    }
                    if (size > 4 && !skip(in, size - 4)) {
                        break;
                    }
                    if (!skipSubBlocks(in)) {
                        break;
                    }
                } else if (!skipSubBlocks(in)) {
                    break;
                }
                continue;
            }
            if (block == 0x2C) {
                // 图像描述符：x(2) y(2) w(2) h(2) packed(1) [局部调色板] minCode(1) 子块链
                byte[] desc = new byte[9];
                if (!readFully(in, desc, 9)) {
                    break;
                }
                int fPacked = desc[8] & 0xFF;
                if ((fPacked & 0x80) != 0 && !skip(in, 3L * (1 << ((fPacked & 7) + 1)))) {
                    break;
                }
                if (in.read() < 0) {
                    break;
                }
                if (!skipSubBlocks(in)) {
                    break;
                }
                int frameDelay;
                if (pending > 0) {
                    frameDelay = pending;
                } else {
                    frameDelay = GifFrames.DEFAULT_DELAY_MS;
                    estimated = true;
                }
                pending = -1;
                if (count == delays.length) {
                    delays = java.util.Arrays.copyOf(delays, delays.length * 2);
                }
                delays[count++] = frameDelay;
                continue;
            }
            // 其它字节（对齐填充等）：与内存版一样当噪声跳过
        }
        if (count <= 0) {
            return null;
        }
        int fill = GifFrames.fillDelay(delays, count);
        int total = 0;
        int filled = 0;
        for (int i = 0; i < count; i++) {
            int v = delays[i] > 0 ? delays[i] : fill;
            if (delays[i] <= 0) {
                filled++;
            }
            total += v;
        }
        int ms = total > 0 ? total : count * GifFrames.DEFAULT_DELAY_MS;
        return new int[]{ms, (estimated || filled > 0) ? 1 : 0, w, h, count};
    }

    // ------------------------------------------------------------------ APNG

    private static int[] apng(InputStream in) throws IOException {
        byte[] sig = new byte[8];
        if (!readFully(in, sig, 8)) {
            return null;
        }
        if ((sig[0] & 0xFF) != 0x89 || sig[1] != 'P' || sig[2] != 'N' || sig[3] != 'G') {
            return null;
        }
        boolean hasAcTl = false;
        int w = 0;
        int h = 0;
        int frames = 0;
        int total = 0;
        byte[] hdr = new byte[8];
        while (readFully(in, hdr, 8)) {
            int len = ((hdr[0] & 0xFF) << 24) | ((hdr[1] & 0xFF) << 16)
                    | ((hdr[2] & 0xFF) << 8) | (hdr[3] & 0xFF);
            if (len < 0) {
                return null;
            }
            String type = new String(hdr, 4, 4, StandardCharsets.US_ASCII);
            if ("IHDR".equals(type)) {
                byte[] d = new byte[8];
                if (len < 8 || !readFully(in, d, 8)) {
                    return null;
                }
                w = i32(d, 0);
                h = i32(d, 4);
                if (!skip(in, len - 8L + 4L)) {
                    return null;
                }
            } else if ("acTL".equals(type)) {
                hasAcTl = true;
                if (!skip(in, len + 4L)) {
                    return null;
                }
            } else if ("fcTL".equals(type)) {
                if (len < 26) {
                    if (!skip(in, len + 4L)) {
                        return null;
                    }
                    continue;
                }
                byte[] d = new byte[26];
                if (!readFully(in, d, 26)) {
                    return null;
                }
                frames++;
                // 与内存版同一个函数（delay_num/delay_den → ms，下限 10ms）
                total += ApngFrames.frameDelayMs(d, 0);
                if (!skip(in, len - 26L + 4L)) {
                    return null;
                }
            } else if ("IEND".equals(type)) {
                break;
            } else if (!skip(in, len + 4L)) {
                return null;
            }
        }
        if (!hasAcTl || w <= 0 || h <= 0 || frames <= 0) {
            return null;
        }
        // APNG 的延时本来就写在每个 fcTL 里，没有「我们补的」成分
        return new int[]{total, 0, w, h, frames};
    }

    // ------------------------------------------------------------------ WebP

    private static int[] webp(InputStream in) throws IOException {
        byte[] riff = new byte[12];
        if (!readFully(in, riff, 12)) {
            return null;
        }
        if (riff[0] != 'R' || riff[1] != 'I' || riff[2] != 'F' || riff[3] != 'F'
                || riff[8] != 'W' || riff[9] != 'E' || riff[10] != 'B' || riff[11] != 'P') {
            return null;
        }
        int canvasW = 0;
        int canvasH = 0;
        int frames = 0;
        int total = 0;
        byte[] hdr = new byte[8];
        while (readFully(in, hdr, 8)) {
            String fourcc = new String(hdr, 0, 4, StandardCharsets.US_ASCII);
            int size = ((hdr[4] & 0xFF)) | ((hdr[5] & 0xFF) << 8)
                    | ((hdr[6] & 0xFF) << 16) | ((hdr[7] & 0xFF) << 24);
            if (size < 0) {
                return null;
            }
            long padded = size + (size & 1);
            if ("VP8X".equals(fourcc) && size >= 10) {
                byte[] d = new byte[10];
                if (!readFully(in, d, 10)) {
                    return null;
                }
                canvasW = 1 + u24(d, 4);
                canvasH = 1 + u24(d, 7);
                if (!skip(in, padded - 10L)) {
                    return null;
                }
            } else if ("ANMF".equals(fourcc) && size >= 16) {
                // ANMF：x(3) y(3) w(3) h(3) duration(3) flags(1)
                byte[] d = new byte[16];
                if (!readFully(in, d, 16)) {
                    return null;
                }
                frames++;
                total += u24(d, 12);
                if (!skip(in, padded - 16L)) {
                    return null;
                }
            } else if (!skip(in, padded)) {
                return null;
            }
        }
        if (frames <= 0) {
            return null;
        }
        return new int[]{total, 0, canvasW, canvasH, frames};
    }

    // ------------------------------------------------------------------ 工具

    private static boolean skipSubBlocks(InputStream in) throws IOException {
        while (true) {
            int len = in.read();
            if (len < 0) {
                return false;
            }
            if (len == 0) {
                return true;
            }
            if (!skip(in, len)) {
                return false;
            }
        }
    }

    private static boolean readFully(InputStream in, byte[] buffer, int want) throws IOException {
        int got = 0;
        while (got < want) {
            int r = in.read(buffer, got, want - got);
            if (r < 0) {
                return false;
            }
            got += r;
        }
        return true;
    }

    /** 跳过 bytes 个字节；流不支持 skip 时用读丢弃（内存仍然只有 1 字节）。 */
    private static boolean skip(InputStream in, long bytes) throws IOException {
        long left = bytes;
        while (left > 0) {
            long n = in.skip(left);
            if (n > 0) {
                left -= n;
                continue;
            }
            if (in.read() < 0) {
                return false;
            }
            left--;
        }
        return true;
    }

    private static int u16(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
    }

    private static int u24(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8) | ((b[off + 2] & 0xFF) << 16);
    }

    private static int i32(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24) | ((b[off + 1] & 0xFF) << 16)
                | ((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF);
    }
}
