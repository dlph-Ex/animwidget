package io.github.animwidget;

import android.graphics.Bitmap;
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
import java.util.zip.CRC32;

/**
 * 极简 APNG 解码器（v1.8.15，v1.10.0 改流式）。
 *
 * 很多扩展名为 .gif 的动图其实是 APNG（PNG 魔数），Movie 解不动 → 以前只能退成
 * 静态首帧（预览冻结的根因之一）。这里按 APNG 规范手工解码：
 *   acTL 拿帧数 → 每帧 fcTL（尺寸/偏移/延时/dispose/blend）+ fdAT（图像数据）
 *   → 重组出单帧 PNG（复用原 IHDR 的位深/色彩类型）→ BitmapFactory 解码
 *   → 按 dispose/blend 规则合成到全画幅画布。
 *
 * v1.10.0：改成「边解边吐」（each）—— 一次只驻留一帧的压缩数据 + 一张输出图，
 * 帧数与内存无关；同时修好「第一帧存在 IDAT 里」的 APNG（以前直接判成非 APNG）。
 */
final class ApngFrames {

    static final class Frame {
        final Bitmap bitmap;
        final int delayMs;

        Frame(Bitmap bitmap, int delayMs) {
            this.bitmap = bitmap;
            this.delayMs = delayMs;
        }
    }

    /** 帧回调；返回 false 提前结束。 */
    interface Sink {
        boolean onFrame(int index, @NotNull Bitmap frame, int delayMs);
    }

    private static final byte[] PNG_SIG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final int DISPOSE_NONE = 0;
    private static final int DISPOSE_BACKGROUND = 1;
    private static final int DISPOSE_PREVIOUS = 2;
    private static final int BLEND_SOURCE = 0;
    private static final int BLEND_OVER = 1;

    private ApngFrames() {
    }

    static boolean isPng(@Nullable byte[] data) {
        if (data == null || data.length < 8) {
            return false;
        }
        for (int i = 0; i < 8; i++) {
            if (data[i] != PNG_SIG[i]) {
                return false;
            }
        }
        return true;
    }

    /** 轻量信息解析（不解码）：返回 {width, height, frameCount, totalDelayMs}；不是 APNG 返回 null。 */
    static int[] info(byte[] data) {
        if (!isPng(data)) {
            return null;
        }
        boolean hasAcTl = false;
        int w = 0, h = 0, frames = 0, totalDelay = 0;
        int pos = 8;
        while (pos + 12 <= data.length) {
            int len = readInt(data, pos);
            String type = new String(data, pos + 4, 4);
            int dataStart = pos + 8;
            if (len < 0 || dataStart + len + 4 > data.length) {
                break;
            }
            switch (type) {
                case "IHDR":
                    if (len >= 8) {
                        w = readInt(data, dataStart);
                        h = readInt(data, dataStart + 4);
                    }
                    break;
                case "acTL":
                    hasAcTl = true;
                    break;
                case "fcTL":
                    if (len >= 26) {
                        frames++;
                        totalDelay += frameDelayMs(data, dataStart);
                    }
                    break;
                case "IEND":
                    pos = data.length;
                    break;
                default:
                    break;
            }
            if (type.equals("IEND")) {
                break;
            }
            pos = dataStart + len + 4;
        }
        if (!hasAcTl || w <= 0 || h <= 0 || frames <= 0) {
            return null;
        }
        return new int[]{w, h, frames, totalDelay};
    }

    /** 帧延时（毫秒）：delay_num / delay_den（den 缺省 100）。 */
    static int frameDelayMs(@NotNull byte[] data, int fcTlDataStart) {
        int num = readShort(data, fcTlDataStart + 20);
        int den = readShort(data, fcTlDataStart + 22);
        if (den == 0) {
            den = 100;
        }
        int ms = (int) (num * 1000L / den);
        return Math.max(ms, 10);
    }

    /** 一次解完全部帧（内存 = 帧数 × 画布），只在导出等离线场景用；预览一律走 each。 */
    @Nullable
    static List<Frame> decode(byte[] data) {
        final List<Frame> out = new ArrayList<>();
        boolean ok = each(data, 0, new Sink() {
            @Override
            public boolean onFrame(int index, @NotNull Bitmap frame, int delayMs) {
                out.add(new Frame(frame, delayMs));
                return true;
            }
        });
        return ok && !out.isEmpty() ? out : null;
    }

    /** 流式解码：逐帧回调。maxEdge > 0 时每帧等比缩到该边长内。 */
    static boolean each(@Nullable byte[] data, int maxEdge, @NotNull Sink sink) {
        try {
            return eachInner(data, maxEdge, sink);
        } catch (Throwable th) {
            return false;
        }
    }

    private static boolean eachInner(@Nullable byte[] data, int maxEdge, @NotNull Sink sink) {
        if (!isPng(data)) {
            return false;
        }
        byte[] ihdr = null;
        boolean animated = false;
        int fullW = 0, fullH = 0;

        Bitmap canvas = null;
        Canvas c = null;
        Paint srcPaint = new Paint();
        srcPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        Paint overPaint = new Paint();
        Paint clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        int[] pending = null;                 // 当前帧 fcTL：{w,h,x,y,delay,dispose,blend}
        List<byte[]> pendingData = null;
        int frameIndex = 0;
        boolean keepGoing = true;

        int pos = 8;
        while (pos + 12 <= data.length) {
            int len = readInt(data, pos);
            String type = new String(data, pos + 4, 4);
            int dataStart = pos + 8;
            if (len < 0 || dataStart + len + 4 > data.length) {
                break;
            }
            if ("IHDR".equals(type)) {
                if (len < 13) {
                    return false;
                }
                ihdr = new byte[13];
                System.arraycopy(data, dataStart, ihdr, 0, 13);
                fullW = readInt(ihdr, 0);
                fullH = readInt(ihdr, 4);
                if (fullW <= 0 || fullH <= 0 || fullW > 8192 || fullH > 8192) {
                    return false;
                }
            } else if ("acTL".equals(type)) {
                animated = true;
            } else if ("fcTL".equals(type)) {
                if (len < 26 || !animated) {
                    return false;
                }
                if (canvas == null) {
                    canvas = Bitmap.createBitmap(fullW, fullH, Bitmap.Config.ARGB_8888);
                    c = new Canvas(canvas);
                }
                if (pending != null && !finishFrame(canvas, c, srcPaint, overPaint, clearPaint,
                        ihdr, pending, pendingData, maxEdge, frameIndex, sink)) {
                    keepGoing = false;
                    break;
                }
                if (pending != null) {
                    frameIndex++;
                }
                pending = new int[]{readInt(data, dataStart + 4), readInt(data, dataStart + 8),
                        readInt(data, dataStart + 12), readInt(data, dataStart + 16),
                        frameDelayMs(data, dataStart), data[dataStart + 24] & 0xff, data[dataStart + 25] & 0xff};
                pendingData = new ArrayList<>();
            } else if (("IDAT".equals(type) || "fdAT".equals(type)) && pending != null) {
                int skip = "fdAT".equals(type) ? 4 : 0;
                if (len - skip > 0) {
                    byte[] payload = new byte[len - skip];
                    System.arraycopy(data, dataStart + skip, payload, 0, len - skip);
                    pendingData.add(payload);
                }
            } else if ("IEND".equals(type)) {
                if (pending != null) {
                    finishFrame(canvas, c, srcPaint, overPaint, clearPaint,
                            ihdr, pending, pendingData, maxEdge, frameIndex, sink);
                    frameIndex++;
                    pending = null;
                }
                break;
            }
            pos = dataStart + len + 4;
        }
        if (canvas != null && !canvas.isRecycled()) {
            canvas.recycle();
        }
        return frameIndex > 0 && keepGoing;
    }

    /**
     * 画一帧 → 上抛 → 按本帧 dispose 规则清理画布。
     * 返回 false 表示调用方要求提前结束。
     */
    private static boolean finishFrame(Bitmap canvas, Canvas c, Paint srcPaint, Paint overPaint, Paint clearPaint,
                                       byte[] ihdr, int[] fctl, List<byte[]> idats, int maxEdge, int index,
                                       @NotNull Sink sink) {
        if (canvas == null || c == null || fctl == null || idats == null || idats.isEmpty()) {
            return true;
        }
        int fw = fctl[0];
        int fh = fctl[1];
        int fx = fctl[2];
        int fy = fctl[3];
        if (fw <= 0 || fh <= 0 || fx < 0 || fy < 0) {
            return true;
        }
        Bitmap before = null;
        if (fctl[5] == DISPOSE_PREVIOUS) {
            try {
                before = Bitmap.createBitmap(canvas, fx, fy, Math.min(fw, canvas.getWidth() - fx),
                        Math.min(fh, canvas.getHeight() - fy));
            } catch (Throwable ignored) {
                before = null;
            }
        }
        Bitmap frame = decodePng(ihdr, fw, fh, idats);
        if (frame != null) {
            c.drawBitmap(frame, fx, fy, fctl[6] == BLEND_SOURCE ? srcPaint : overPaint);
            frame.recycle();
        }
        Bitmap out = FrameUtil.fit(canvas, maxEdge);
        boolean cont = sink.onFrame(index, out, fctl[4]);
        if (out != null && out != canvas && !out.isRecycled()) {
            out.recycle();
        }
        if (fctl[5] == DISPOSE_BACKGROUND) {
            c.drawRect(new Rect(fx, fy, fx + fw, fy + fh), clearPaint);
        } else if (fctl[5] == DISPOSE_PREVIOUS && before != null) {
            c.drawBitmap(before, fx, fy, srcPaint);
        }
        if (before != null && !before.isRecycled()) {
            before.recycle();
        }
        return cont;
    }

    /** 用原 IHDR 的位深/色彩类型 + 本帧尺寸重组一个合法 PNG 并解码。 */
    @Nullable
    private static Bitmap decodePng(byte[] ihdr, int w, int h, @NotNull List<byte[]> idats) {
        byte[] png = buildPng(ihdr, w, h, idats);
        if (png == null) {
            return null;
        }
        return ColorFix.toSrgb(android.graphics.BitmapFactory.decodeByteArray(png, 0, png.length));
    }

    private static byte[] buildPng(byte[] ihdr, int w, int h, List<byte[]> idats) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(PNG_SIG, 0, 8);
            byte[] newIhdr = new byte[13];
            writeInt(newIhdr, 0, w);
            writeInt(newIhdr, 4, h);
            System.arraycopy(ihdr, 8, newIhdr, 8, 5); // bitDepth/colorType/compression/filter/interlace
            writeChunk(out, "IHDR", newIhdr);
            for (byte[] d : idats) {
                writeChunk(out, "IDAT", d);
            }
            writeChunk(out, "IEND", new byte[0]);
            return out.toByteArray();
        } catch (Throwable th) {
            return null;
        }
    }

    private static void writeChunk(ByteArrayOutputStream out, String type, byte[] payload) {
        byte[] lenBuf = new byte[4];
        writeInt(lenBuf, 0, payload.length);
        out.write(lenBuf, 0, 4);
        byte[] typeBuf = type.getBytes();
        out.write(typeBuf, 0, 4);
        out.write(payload, 0, payload.length);
        CRC32 crc = new CRC32();
        crc.update(typeBuf);
        crc.update(payload);
        byte[] crcBuf = new byte[4];
        writeInt(crcBuf, 0, (int) crc.getValue());
        out.write(crcBuf, 0, 4);
    }

    private static int readInt(byte[] d, int off) {
        return ((d[off] & 0xff) << 24) | ((d[off + 1] & 0xff) << 16) | ((d[off + 2] & 0xff) << 8) | (d[off + 3] & 0xff);
    }

    private static int readShort(byte[] d, int off) {
        return ((d[off] & 0xff) << 8) | (d[off + 1] & 0xff);
    }

    private static void writeInt(byte[] d, int off, int v) {
        d[off] = (byte) (v >>> 24);
        d[off + 1] = (byte) (v >>> 16);
        d[off + 2] = (byte) (v >>> 8);
        d[off + 3] = (byte) v;
    }
}
