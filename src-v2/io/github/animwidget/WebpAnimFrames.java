package io.github.animwidget;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 极简动图 WebP 解码器（v1.9.6，v1.10.0 改流式）。
 *
 * 很多「.gif 动图」其实是动图 WebP（Movie 解不动、不是 PNG 也走不了 APNG 路径）。
 * 按 WebP 容器规范手工解析：
 *   VP8X 拿画布尺寸 → ANMF 逐帧（位置/尺寸/时长/dispose/blend 标志 + VP8/VP8L 位流）
 *   → 把每帧位流重组成一个独立的最小 WebP 文件 → BitmapFactory 解码
 *   → 按 dispose/blend 规则合成到全画幅画布。
 *
 * v1.10.0：改成「边解边吐」（each），一次只驻留一帧位流 + 一张输出图。
 */
final class WebpAnimFrames {

    /** 帧回调；返回 false 提前结束。 */
    interface Sink {
        boolean onFrame(int index, @NotNull Bitmap frame, int delayMs);
    }

    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP = {'W', 'E', 'B', 'P'};

    private WebpAnimFrames() {
    }

    static boolean isWebp(@Nullable byte[] d) {
        return d != null && d.length >= 16
                && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P';
    }

    /** 轻量信息解析（不解码）：{width, height, frameCount, totalDelayMs}；不是动图 WebP 返回 null。 */
    static int[] info(byte[] data) {
        try {
            return parseInfo(data);
        } catch (Throwable th) {
            return null;
        }
    }

    /** 一次解完全部帧（内存 = 帧数 × 画布），仅供离线场景；预览走 each。 */
    @Nullable
    static List<ApngFrames.Frame> decode(byte[] data) {
        final List<ApngFrames.Frame> out = new ArrayList<>();
        boolean ok = each(data, 0, new Sink() {
            @Override
            public boolean onFrame(int index, @NotNull Bitmap frame, int delayMs) {
                out.add(new ApngFrames.Frame(frame, delayMs));
                return true;
            }
        });
        return ok && !out.isEmpty() ? out : null;
    }

    private static int[] parseInfo(byte[] d) {
        if (!isWebp(d)) {
            return null;
        }
        int canvasW = 0, canvasH = 0;
        int frames = 0, totalDelay = 0;
        int pos = 12;
        while (pos + 8 <= d.length) {
            String fourcc = new String(d, pos, 4);
            int size = readIntLE(d, pos + 4);
            int dataStart = pos + 8;
            if (size < 0 || dataStart + size > d.length) {
                break;
            }
            if ("VP8X".equals(fourcc) && size >= 10) {
                canvasW = 1 + readU24LE(d, dataStart + 4);
                canvasH = 1 + readU24LE(d, dataStart + 7);
            } else if ("ANMF".equals(fourcc) && size >= 16) {
                frames++;
                totalDelay += readU24LE(d, dataStart + 12);
            }
            pos = dataStart + size + (size & 1);
        }
        if (frames <= 0) {
            return null;
        }
        if (canvasW <= 0 || canvasH <= 0) {
            canvasW = 0;
            canvasH = 0;
            pos = 12;
            while (pos + 8 <= d.length) {
                String fourcc = new String(d, pos, 4);
                int size = readIntLE(d, pos + 4);
                int dataStart = pos + 8;
                if (size < 0 || dataStart + size > d.length) {
                    break;
                }
                if ("ANMF".equals(fourcc) && size >= 16) {
                    int x = readU24LE(d, dataStart);
                    int y = readU24LE(d, dataStart + 3);
                    int w = 1 + readU24LE(d, dataStart + 6);
                    int h = 1 + readU24LE(d, dataStart + 9);
                    canvasW = Math.max(canvasW, x + w);
                    canvasH = Math.max(canvasH, y + h);
                }
                pos = dataStart + size + (size & 1);
            }
        }
        if (canvasW <= 0 || canvasH <= 0) {
            return null;
        }
        return new int[]{canvasW, canvasH, frames, totalDelay};
    }

    /** 流式解码：逐帧回调。maxEdge > 0 时每帧等比缩到该边长内。 */
    static boolean each(@Nullable byte[] d, int maxEdge, @NotNull Sink sink) {
        try {
            return eachInner(d, maxEdge, sink);
        } catch (Throwable th) {
            return false;
        }
    }

    private static boolean eachInner(@Nullable byte[] d, int maxEdge, @NotNull Sink sink) {
        if (!isWebp(d)) {
            return false;
        }
        int[] info = parseInfo(d);
        if (info == null) {
            return false;
        }
        int canvasW = info[0];
        int canvasH = info[1];
        if (canvasW > 8192 || canvasH > 8192) {
            return false;
        }
        Bitmap canvas = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(canvas);
        Paint srcPaint = new Paint();
        srcPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        Paint overPaint = new Paint();
        Paint clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        int prevX = 0, prevY = 0, prevW = 0, prevH = 0;
        int pendingDisposeX = 0, pendingDisposeY = 0, pendingDisposeW = 0, pendingDisposeH = 0;
        boolean disposePending = false;
        int index = 0;
        boolean keepGoing = true;

        int pos = 12;
        while (pos + 8 <= d.length && keepGoing) {
            String fourcc = new String(d, pos, 4);
            int size = readIntLE(d, pos + 4);
            int dataStart = pos + 8;
            if (size < 0 || dataStart + size > d.length) {
                break;
            }
            if ("ANMF".equals(fourcc) && size >= 16) {
                int x = readU24LE(d, dataStart);
                int y = readU24LE(d, dataStart + 3);
                int w = 1 + readU24LE(d, dataStart + 6);
                int h = 1 + readU24LE(d, dataStart + 9);
                int delay = Math.max(readU24LE(d, dataStart + 12), 10);
                int flags = d[dataStart + 15] & 0xff;
                // v1.10.456（真机日志实证的 bug）：这两个 bit 原来**读反了**。
                // 规范是 bit0 = blending（1 = 不与上一帧合成），bit1 = disposal（1 = 置背景）。
                // 读反的后果：每一帧都被 alpha 混合到上一帧上 —— 圆角/阴影边缘会糊成一圈，
                // 全画布帧还会互相叠影（用户看到的就是"边缘发黑、画面像残影"）。
                boolean noBlend = (flags & 0x01) != 0;   // 1 = 不做 alpha 混合（直接覆盖）
                boolean dispose = (flags & 0x02) != 0;   // 1 = 本帧显示完后置为背景
                int fh = dataStart + 16;
                int frameEnd = dataStart + size;
                if (fh + 8 > frameEnd) {
                    break;
                }
                // v1.10.456：**ANMF 里不一定只有一个位流块。**
                // 有损帧是 `ALPH` + `VP8 `（alpha 单独一块），无损帧才是单个 `VP8L`。
                // 旧代码把「四字节里的第 2~4 字节」当成块长度、把 fh+4 当位流起点（那是长度字段本身），
                // 于是任何一个子块都解析失败 → each() 返回 false → 上游记
                // 「P10 动图: 解码出 0 帧」→ 退回静态首帧（用户导入动图 WebP 不播放的真因）。
                byte[] alphaChunk = null;
                int bs = -1;
                int bstreamSize = 0;
                int chunkPos = fh;
                while (chunkPos + 8 <= frameEnd) {
                    String cc = new String(d, chunkPos, 4);
                    int sz = readIntLE(d, chunkPos + 4);
                    if (sz < 0 || chunkPos + 8 + sz > frameEnd) {
                        break;
                    }
                    if ("ALPH".equals(cc)) {
                        alphaChunk = sliceChunk(d, chunkPos, sz);
                    } else if ("VP8 ".equals(cc) || "VP8L".equals(cc)) {
                        bs = chunkPos + 8;
                        bstreamSize = sz;
                        break;
                    }
                    chunkPos += 8 + sz + (sz & 1);
                }
                if (bs < 0 || bstreamSize <= 0) {
                    break;
                }
                if (disposePending) {
                    c.drawRect(new Rect(pendingDisposeX, pendingDisposeY,
                            pendingDisposeX + pendingDisposeW, pendingDisposeY + pendingDisposeH), clearPaint);
                    disposePending = false;
                }
                Bitmap frame = decodeFrame(d, bs, bstreamSize, alphaChunk, w, h);
                if (frame != null) {
                    c.drawBitmap(frame, x, y, noBlend ? srcPaint : overPaint);
                    frame.recycle();
                }
                Bitmap out = FrameUtil.fit(canvas, maxEdge);
                keepGoing = sink.onFrame(index, out, delay);
                if (out != null && out != canvas && !out.isRecycled()) {
                    out.recycle();
                }
                index++;
                prevX = x;
                prevY = y;
                prevW = w;
                prevH = h;
                if (dispose) {
                    pendingDisposeX = x;
                    pendingDisposeY = y;
                    pendingDisposeW = w;
                    pendingDisposeH = h;
                    disposePending = true;
                }
            }
            pos = dataStart + size + (size & 1);
        }
        if (!canvas.isRecycled()) {
            canvas.recycle();
        }
        return index > 0;
    }

    /**
     * 把一帧的位流重组成独立的最小 WebP 文件并解码。
     *
     * <p>v1.10.456：**必须把 ALPH 一起带上**。有损帧的 alpha 在单独的 ALPH 块里，
     * 只拼 VP8 的话，透明区域会变成不透明黑块；而 ALPH 存在时容器规范要求有 VP8X
     * 且 flags 的 alpha 位（0x10）置位，否则解码器会忽略 ALPH。
     */
    @Nullable
    private static Bitmap decodeFrame(byte[] d, int bs, int size,
                                      @Nullable byte[] alphaChunk, int width, int height) {
        if (size <= 0) {
            return null;
        }
        boolean vp8l = (d[bs] & 0xff) == 0x2f;   // VB8L 起始码 0x2F
        String fourcc = vp8l ? "VP8L" : "VP8 ";
        int alphaLen = alphaChunk == null ? 0 : alphaChunk.length;
        boolean needVp8x = alphaLen > 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream(64 + alphaLen + size);
        int riffSize = 4 + (needVp8x ? 18 : 0) + alphaLen + 8 + size + (size & 1);
        out.write(RIFF, 0, 4);
        writeIntLE(out, riffSize);
        out.write(WEBP, 0, 4);
        if (needVp8x) {
            byte[] vp8x = {'V', 'P', '8', 'X'};
            out.write(vp8x, 0, 4);
            writeIntLE(out, 10);
            out.write(0x10);                    // flags：bit4 = 有 alpha
            out.write(0);
            out.write(0);
            out.write(0);
            out.write((width - 1) & 0xff);      // 画布宽-1（24 位）
            out.write(((width - 1) >> 8) & 0xff);
            out.write(((width - 1) >> 16) & 0xff);
            out.write((height - 1) & 0xff);     // 画布高-1（24 位）
            out.write(((height - 1) >> 8) & 0xff);
            out.write(((height - 1) >> 16) & 0xff);
            out.write(alphaChunk, 0, alphaLen);
        }
        byte[] cc = fourcc.getBytes();
        out.write(cc, 0, 4);
        writeIntLE(out, size);
        out.write(d, bs, size);
        if ((size & 1) != 0) {
            out.write(0);
        }
        byte[] file = out.toByteArray();
        return ColorFix.toSrgb(BitmapFactory.decodeByteArray(file, 0, file.length));
    }

    /** 取出一整个块（4CC + 长度 + 负载 + 补齐），供拼最小容器时原样搬运。 */
    @NotNull
    private static byte[] sliceChunk(byte[] d, int pos, int size) {
        int len = 8 + size + (size & 1);
        byte[] out = new byte[len];
        System.arraycopy(d, pos, out, 0, Math.min(len, d.length - pos));
        return out;
    }

    private static int readIntLE(byte[] d, int off) {
        return (d[off] & 0xff) | ((d[off + 1] & 0xff) << 8) | ((d[off + 2] & 0xff) << 16) | ((d[off + 3] & 0xff) << 24);
    }

    private static int readU24LE(byte[] d, int off) {
        return (d[off] & 0xff) | ((d[off + 1] & 0xff) << 8) | ((d[off + 2] & 0xff) << 16);
    }

    private static void writeIntLE(ByteArrayOutputStream out, int v) {
        out.write(v & 0xff);
        out.write((v >>> 8) & 0xff);
        out.write((v >>> 16) & 0xff);
        out.write((v >>> 24) & 0xff);
    }
}
