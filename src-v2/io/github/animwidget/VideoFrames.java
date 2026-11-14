package io.github.animwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * 视频抽帧（v1.10.2，顺序解码）。
 *
 * 为什么不再用 MediaMetadataRetriever.getFrameAtTime：
 * 它每次调用都要「seek 到关键帧再解到目标时间」，一段 16 秒的视频每帧 ~210ms
 * （V06 首帧耗时 210ms 就是证据）→ 480 帧要 100 秒，用户看到的就是「导入后一直没反应」。
 *
 * 这里改成 MediaCodec 顺序解一次：整段解码一遍（硬件解码，720p 每秒几百帧的余量），
 * 按时间轴把需要的帧导出来，16 秒素材通常 2~4 秒完成。
 *
 * 顺带把色彩做对：按 MediaFormat 里的 color standard/range（BT.601 / 709 / 2020，
 * full / limited）选 YUV→RGB 矩阵，不再套一个固定公式。
 */
final class VideoFrames {

    interface Sink {
        /** @return false 表示提前结束（已经够了） */
        boolean onFrame(int index, @NotNull Bitmap frame);
    }

    private VideoFrames() {
    }

    /**
     * 顺序解码并按时间轴采样。
     * @return 实际出帧数；0 / 负数 = 这条路走不通（调用方回退到 MediaMetadataRetriever）
     */
    static int extract(@NotNull Context context, @NotNull Uri uri, int outFpsX10, int outCount, int maxEdge,
                       @NotNull Sink sink) {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec codec = null;
        try {
            extractor.setDataSource(context, uri, null);
            int track = -1;
            MediaFormat format = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat candidate = extractor.getTrackFormat(i);
                String mime = candidate.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("video/")) {
                    track = i;
                    format = candidate;
                    break;
                }
            }
            if (track < 0 || format == null) {
                return 0;
            }
            extractor.selectTrack(track);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime == null) {
                return 0;
            }
            int rotation = 0;
            try {
                if (format.containsKey(MediaFormat.KEY_ROTATION)) {
                    rotation = format.getInteger(MediaFormat.KEY_ROTATION);
                }
            } catch (Throwable ignored) {
            }
            int colorStandard = intOrDefault(format, "color-standard", 0);
            int colorRange = intOrDefault(format, "color-range", 0);
            int colorTransfer = intOrDefault(format, "color-transfer", 0);
            // v1.10.8：HDR（PQ=3 / HLG=4）不能当 SDR 直接显示，否则发灰
            // v1.10.10：必须「BT.2020 + PQ/HLG」才认为真 HDR ——
            // 很多 SDR 视频被错误标成 transfer=3（你那条 BT.709 标了 PQ），
            // 一律套 HDR 曲线会让画面发灰/发亮（用户反馈「像加了层 HDR」）
            boolean realHdr = (colorStandard == 6) && (colorTransfer == 3 || colorTransfer == 4);
            float[] hdrLinear = realHdr ? buildHdrToLinear(colorTransfer == 3) : null;
            Diag.warn(context, "C03", "视频解码 " + mime + " 旋转=" + rotation
                    + " 色彩标准=" + colorStandard + " 范围=" + colorRange
                    + " 传输=" + colorTransfer + (hdrLinear != null ? "（HDR→SDR：线性 tone map + BT.2020→sRGB）" : ""));

            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(format, null, null, 0);
            codec.start();

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean inputDone = false;
            boolean outputDone = false;
            int emitted = 0;
            int decoderWidth = intOrDefault(format, MediaFormat.KEY_WIDTH, 0);
            int decoderHeight = intOrDefault(format, MediaFormat.KEY_HEIGHT, 0);
            int colorFormat = -1;

            while (!outputDone) {
                if (!inputDone) {
                    int inIndex = codec.dequeueInputBuffer(10000L);
                    if (inIndex >= 0) {
                        ByteBuffer buffer = codec.getInputBuffer(inIndex);
                        if (buffer == null) {
                            inputDone = true;
                        } else {
                            int size = extractor.readSampleData(buffer, 0);
                            if (size < 0) {
                                codec.queueInputBuffer(inIndex, 0, 0, 0,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                inputDone = true;
                            } else {
                                long sampleTime = extractor.getSampleTime();
                                codec.queueInputBuffer(inIndex, 0, size, sampleTime, 0);
                                extractor.advance();
                            }
                        }
                    }
                }
                int outIndex = codec.dequeueOutputBuffer(info, 10000L);
                if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat outFormat = codec.getOutputFormat();
                    try {
                        colorFormat = outFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT);
                    } catch (Throwable ignored) {
                    }
                    decoderWidth = intOrDefault(outFormat, MediaFormat.KEY_WIDTH, decoderWidth);
                    decoderHeight = intOrDefault(outFormat, MediaFormat.KEY_HEIGHT, decoderHeight);
                    continue;
                }
                if (outIndex < 0) {
                    continue;
                }
                boolean eos = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                if (info.size > 0 && emitted < outCount) {
                    long timeUs = info.presentationTimeUs;
                    // 时间轴采样：所有「时间点 <= 本帧时间」的输出帧都用这一帧。
                    // +500000 是四舍五入 —— 否则 30fps 时 33333us 会被截断成 0，
                    // 每两帧才出一个输出帧（帧数腰斩、播放变慢）。
                    // fpsX10（330 = 33.0fps）：时间点 j 落在 t 之内就产出第 j 帧
                    int wantUpTo = (int) Math.min(outCount,
                            ((timeUs * (long) Math.max(outFpsX10, 10)) + 50000L) / 10000000L + 1);
                    if (wantUpTo > emitted) {
                        Bitmap frame = decodeOutput(codec, outIndex, info, decoderWidth, decoderHeight,
                                colorFormat, colorStandard, colorRange, hdrLinear, rotation, maxEdge);
                        if (frame != null) {
                            for (int i = emitted; i < wantUpTo; i++) {
                                if (!sink.onFrame(i, frame)) {
                                    outputDone = true;
                                    break;
                                }
                            }
                            emitted = Math.max(emitted, wantUpTo);
                            if (!frame.isRecycled()) {
                                frame.recycle();
                            }
                        }
                    }
                }
                codec.releaseOutputBuffer(outIndex, false);
                if (eos) {
                    outputDone = true;
                }
                if (emitted >= outCount) {
                    outputDone = true;
                }
            }
            Diag.warn(context, "C04", "视频顺序解码出 " + emitted + "/" + outCount + " 帧");
            return emitted;
        } catch (Throwable th) {
            Diag.warn(context, "C05", "顺序解码中止: " + th.getClass().getSimpleName()
                    + (th.getMessage() != null ? "/" + th.getMessage() : ""));
            return 0;
        } finally {
            if (codec != null) {
                try {
                    codec.stop();
                } catch (Throwable ignored) {
                }
                try {
                    codec.release();
                } catch (Throwable ignored) {
                }
            }
            try {
                extractor.release();
            } catch (Throwable ignored) {
            }
        }
    }

    @Nullable
    private static Bitmap decodeOutput(MediaCodec codec, int outIndex, MediaCodec.BufferInfo info,
                                       int width, int height, int colorFormat, int colorStandard, int colorRange,
                                       float[] hdrLinear, int rotation, int maxEdge) {
        Bitmap bitmap = null;
        try {
            android.media.Image image = codec.getOutputImage(outIndex);
            if (image != null) {
                bitmap = imageToBitmap(image, colorStandard, colorRange, hdrLinear);
                image.close();
            }
        } catch (Throwable ignored) {
            bitmap = null;
        }
        if (bitmap == null) {
            try {
                ByteBuffer buffer = codec.getOutputBuffer(outIndex);
                if (buffer != null && width > 0 && height > 0) {
                    bitmap = yuvBufferToBitmap(buffer, width, height, colorFormat, colorStandard, colorRange, hdrLinear);
                }
            } catch (Throwable ignored) {
                bitmap = null;
            }
        }
        if (bitmap == null) {
            return null;
        }
        if (rotation != 0) {
            try {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (rotated != bitmap && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                bitmap = rotated;
            } catch (Throwable ignored) {
            }
        }
        Bitmap fitted = FrameUtil.fit(bitmap, maxEdge);
        if (fitted != bitmap && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return ColorFix.toSrgb(fitted);
    }

    /**
     * Image（YUV_420_888 / flexible）→ ARGB Bitmap，按色彩标准换算。
     * v1.10.4：按行批量取字节再算 —— 原来逐像素 ByteBuffer.get() 每帧要 170 万次调用，
     * 800×720 单帧就要几十毫秒；批量行拷贝后单帧只要几毫秒。
     */
    @Nullable
    private static Bitmap imageToBitmap(@NotNull android.media.Image image, int standard, int range, float[] hdrLinear) {
        try {
            android.graphics.Rect crop = image.getCropRect();
            int left = Math.max(crop.left, 0);
            int top = Math.max(crop.top, 0);
            int width = Math.min(crop.width(), image.getWidth() - left);
            int height = Math.min(crop.height(), image.getHeight() - top);
            if (width <= 0 || height <= 0) {
                return null;
            }
            android.media.Image.Plane[] planes = image.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();
            int yRowStride = planes[0].getRowStride();
            int yPixelStride = Math.max(planes[0].getPixelStride(), 1);
            int uRowStride = planes[1].getRowStride();
            int uPixelStride = Math.max(planes[1].getPixelStride(), 1);
            int vRowStride = planes[2].getRowStride();
            int vPixelStride = Math.max(planes[2].getPixelStride(), 1);

            byte[] yRowBytes = new byte[Math.min(yRowStride, width * yPixelStride + yPixelStride)];
            int chromaWidth = (width / 2) + 1;
            byte[] uRowBytes = new byte[Math.min(uRowStride, chromaWidth * uPixelStride + uPixelStride)];
            byte[] vRowBytes = new byte[Math.min(vRowStride, chromaWidth * vPixelStride + vPixelStride)];
            int[] argb = new int[width * height];
            for (int row = 0; row < height; row++) {
                int yBase = (top + row) * yRowStride + left * yPixelStride;
                int uvRow = (top + row) / 2;
                int uBase = uvRow * uRowStride + (left / 2) * uPixelStride;
                int vBase = uvRow * vRowStride + (left / 2) * vPixelStride;
                int outBase = row * width;
                fill(yBuffer, yBase, yRowBytes);
                fill(uBuffer, uBase, uRowBytes);
                fill(vBuffer, vBase, vRowBytes);
                for (int col = 0; col < width; col++) {
                    int y = yRowBytes[col * yPixelStride] & 0xFF;
                    int u = uRowBytes[(col / 2) * uPixelStride] & 0xFF;
                    int v = vRowBytes[(col / 2) * vPixelStride] & 0xFF;
                    argb[outBase + col] = yuvToArgb(y, u, v, standard, range, hdrLinear);
                }
            }
            return Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888);
        } catch (Throwable th) {
            return null;
        }
    }

    /**
     * v1.10.149（B7）：HDR（PQ/HLG）→ **线性 BT.2020**（已 tone map 到 SDR 峰值 1.0）。
     *
     * 以前是把 PQ/HLG 曲线直接折成 sRGB 编码值、再逐通道查表 —— 少了**色域转换**，
     * BT.2020 的宽色域当 709 显示，绿/红的饱和度会冲出去（画面「艳得失真」）。
     * 现在按正确顺序走：编码值 → 线性光 → tone map → 2020→709 矩阵 → sRGB gamma。
     */
    @NotNull
    private static float[] buildHdrToLinear(boolean pq) {
        float[] lut = new float[256];
        for (int i = 0; i < 256; i++) {
            double v = i / 255.0;
            double linear;
            if (pq) {
                // SMPTE ST 2084 EOTF → 归一化到 10000 nit，再按 203 nit 白点压缩
                double m1 = 0.1593017578125, m2 = 78.84375, c1 = 0.8359375, c2 = 18.8515625, c3 = 18.6875;
                double p = Math.pow(v, 1.0 / m2);
                double num = Math.max(p - c1, 0.0);
                double den = c2 - c3 * p;
                double y = den <= 0 ? 0 : Math.pow(num / den, 1.0 / m1);   // 0..1 = 0..10000nit
                linear = y * 10000.0 / 203.0;
            } else {
                // HLG（BT.2100）
                double a = 0.17883277, b = 0.28466892, c = 0.55991073;
                double scene = v <= 0.5 ? (v * v) / 3.0 : (Math.exp((v - c) / a) + b) / 12.0;
                linear = scene * 1000.0 / 203.0;
            }
            // Reinhard 变体：把 >1 的高光压回 1 以内
            linear = linear / (1.0 + linear * 0.6);
            lut[i] = (float) Math.max(0.0, Math.min(1.0, linear));
        }
        return lut;
    }

    /** BT.2020 primaries → BT.709/sRGB primaries 的线性光矩阵。 */
    private static final float[] M2020_TO_709 = {
            1.660491f, -0.587641f, -0.072850f,
            -0.124550f, 1.132900f, -0.008350f,
            -0.018151f, -0.100579f, 1.118730f
    };

    /** 线性 → sRGB 编码值的查表（1024 档，省掉每像素 pow）。 */
    private static final int[] LINEAR_TO_SRGB = buildLinearToSrgb();

    @NotNull
    private static int[] buildLinearToSrgb() {
        int[] table = new int[1025];
        for (int i = 0; i <= 1024; i++) {
            double v = i / 1024.0;
            double s = v <= 0.0031308 ? 12.92 * v : 1.055 * Math.pow(v, 1.0 / 2.4) - 0.055;
            table[i] = (int) Math.round(Math.max(0.0, Math.min(1.0, s)) * 255.0);
        }
        return table;
    }

    private static int srgb8(float linear) {
        if (linear <= 0.0f) {
            return 0;
        }
        if (linear >= 1.0f) {
            return 255;
        }
        return LINEAR_TO_SRGB[(int) (linear * 1024.0f)];
    }

    /** 从绝对位置拷贝一整段到数组（越界时少拷，不抛）。 */
    private static void fill(@NotNull ByteBuffer buffer, int position, @NotNull byte[] dest) {
        int size = Math.min(dest.length, Math.max(buffer.limit() - position, 0));
        if (size <= 0) {
            return;
        }
        buffer.position(position);
        buffer.get(dest, 0, size);
    }

    /** 非 Image 路径（COLOR_FormatYUV420Planar=19 / SemiPlanar=21）。 */
    @Nullable
    private static Bitmap yuvBufferToBitmap(@NotNull ByteBuffer buffer, int width, int height,
                                            int colorFormat, int standard, int range, float[] hdrLinear) {
        try {
            buffer.rewind();
            int[] argb = new int[width * height];
            if (colorFormat == MediaCodecInfoCompat.SEMI_PLANAR) {
                int ySize = width * height;
                for (int row = 0; row < height; row++) {
                    int uvRow = (row / 2) * width;
                    int outBase = row * width;
                    for (int col = 0; col < width; col++) {
                        int y = buffer.get(row * width + col) & 0xFF;
                        int u = buffer.get(ySize + uvRow + (col & ~1)) & 0xFF;
                        int v = buffer.get(ySize + uvRow + (col | 1)) & 0xFF;
                        argb[outBase + col] = yuvToArgb(y, u, v, standard, range, hdrLinear);
                    }
                }
            } else if (colorFormat == MediaCodecInfoCompat.PLANAR) {
                int ySize = width * height;
                int uvSize = (width / 2) * (height / 2);
                for (int row = 0; row < height; row++) {
                    int outBase = row * width;
                    int uvRow = (row / 2) * (width / 2);
                    for (int col = 0; col < width; col++) {
                        int y = buffer.get(row * width + col) & 0xFF;
                        int u = buffer.get(ySize + uvRow + (col / 2)) & 0xFF;
                        int v = buffer.get(ySize + uvSize + uvRow + (col / 2)) & 0xFF;
                        argb[outBase + col] = yuvToArgb(y, u, v, standard, range, hdrLinear);
                    }
                }
            } else {
                return null;
            }
            return Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888);
        } catch (Throwable th) {
            return null;
        }
    }

    /**
     * YUV → ARGB（需求 1：按色彩标准/范围选矩阵，不再套一个固定公式）。
     * standard: 1=BT709 2=BT601_NTSC 4=BT601_PAL 6=BT2020（其余按分辨率猜）
     * range: 1=FULL 2=LIMITED
     */
    private static int yuvToArgb(int y, int u, int v, int standard, int range, float[] hdrLinear) {
        float yy = y;
        float uu = u - 128.0f;
        float vv = v - 128.0f;
        if (range != 1) {
            // limited range（16~235）
            yy = (y - 16) * 1.164384f;
            if (yy < 0.0f) {
                yy = 0.0f;
            }
        }
        float r;
        float g;
        float b;
        if (standard == 6) {              // BT.2020
            r = yy + 1.6787f * vv;
            g = yy - 0.1873f * uu - 0.6504f * vv;
            b = yy + 2.1418f * uu;
        } else if (standard == 1 || standard == 0) {   // BT.709（默认，HD 素材最常见）
            r = yy + 1.7927f * vv;
            g = yy - 0.2132f * uu - 0.5329f * vv;
            b = yy + 2.1124f * uu;
        } else {                           // BT.601
            r = yy + 1.5960f * vv;
            g = yy - 0.3910f * uu - 0.8130f * vv;
            b = yy + 2.0180f * uu;
        }
        int ri = clamp8((int) (r + 0.5f));
        int gi = clamp8((int) (g + 0.5f));
        int bi = clamp8((int) (b + 0.5f));
        if (hdrLinear != null) {
            // v1.10.149：线性光里做 2020→709 色域转换，再编码回 sRGB
            float rl = hdrLinear[ri];
            float gl = hdrLinear[gi];
            float bl = hdrLinear[bi];
            float sr = M2020_TO_709[0] * rl + M2020_TO_709[1] * gl + M2020_TO_709[2] * bl;
            float sg = M2020_TO_709[3] * rl + M2020_TO_709[4] * gl + M2020_TO_709[5] * bl;
            float sb = M2020_TO_709[6] * rl + M2020_TO_709[7] * gl + M2020_TO_709[8] * bl;
            return 0xFF000000 | (srgb8(sr) << 16) | (srgb8(sg) << 8) | srgb8(sb);
        }
        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }

    private static int clamp8(int value) {
        return value < 0 ? 0 : (value > 255 ? 255 : value);
    }

    private static int intOrDefault(@NotNull MediaFormat format, @NotNull String key, int fallback) {
        try {
            return format.getInteger(key);
        } catch (Throwable th) {
            return fallback;
        }
    }

    /** MediaCodecInfo.CodecCapabilities 里的几个常量（避免额外 import 冲突）。 */
    private static final class MediaCodecInfoCompat {
        static final int PLANAR = 19;
        static final int SEMI_PLANAR = 21;
    }
}
