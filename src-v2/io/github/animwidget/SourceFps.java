package io.github.animwidget;
import android.graphics.BitmapFactory.Options;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Movie;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import java.io.InputStream;
import kotlin.Metadata;
import kotlin.NoWhenBranchMatchedException;
import kotlin.io.ByteStreamsKt;
import kotlin.io.CloseableKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.math.MathKt;
import kotlin.ranges.RangesKt;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* compiled from: SourceFps.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��>\n\u0002\u0018\u0002\n\u0002\u0010��\n\u0002\b\u0002\n\u0002\u0010\t\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0012\n\u0002\b\u0002\bÆ\u0002\u0018��2\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J%\u0010\u0003\u001a\u0004\u0018\u00010\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n¢\u0006\u0002\u0010\u000bJ\u001d\u0010\f\u001a\u0004\u0018\u00010\r2\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b¢\u0006\u0002\u0010\u000eJ \u0010\u000f\u001a\u0004\u0018\u00010\u00102\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nJ\u0018\u0010\u0011\u001a\u00020\r2\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\rH\u0002J\u001d\u0010\u0015\u001a\u0004\u0018\u00010\r2\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b¢\u0006\u0002\u0010\u000e"}, d2 = {"Lio/github/animwidget/SourceFps;", "", "()V", "duration", "", "context", "Landroid/content/Context;", "uri", "Landroid/net/Uri;", "kind", "Lio/github/animwidget/MediaKind;", "(Landroid/content/Context;Landroid/net/Uri;Lio/github/animwidget/MediaKind;)Ljava/lang/Long;", "gif", "", "(Landroid/content/Context;Landroid/net/Uri;)Ljava/lang/Integer;", "resolution", "", "skipSubBlocks", "bytes", "", "start", "video"})
@SourceDebugExtension({"SMAP\nSourceFps.kt\nKotlin\n*S Kotlin\n*F\n+ 1 SourceFps.kt\nio/github/animwidget/SourceFps\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,161:1\n1#2:162\n*E\n"})
/* loaded from: classes.jar:io/github/animwidget/SourceFps.class */
public final class SourceFps {

    @NotNull
    public static final SourceFps INSTANCE = new SourceFps();

    /* compiled from: SourceFps.kt */
    @Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    /* loaded from: classes.jar:io/github/animwidget/SourceFps$WhenMappings.class */
    public static /* synthetic */ class WhenMappings {
        public static final /* synthetic */ int[] $EnumSwitchMapping$0;

        static {
            int[] iArr = new int[MediaKind.values().length];
            try {
                iArr[MediaKind.VIDEO.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[MediaKind.GIF.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[MediaKind.IMAGE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            $EnumSwitchMapping$0 = iArr;
        }
    }

    private SourceFps() {
    }

    /* JADX WARN: Finally extract failed */
    @Nullable
    public final Long duration(@NotNull Context context, @NotNull Uri uri, @NotNull MediaKind kind) throws java.io.IOException {
        Long l;
        Long lValueOf;
        Movie movie;
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(uri, "uri");
        Intrinsics.checkNotNullParameter(kind, "kind");
        try {
        } catch (Throwable th) {
            l = null;
        }
        switch (WhenMappings.$EnumSwitchMapping$0[kind.ordinal()]) {
            case 1:
                MediaMetadataRetriever r = new MediaMetadataRetriever();
                try {
                    r.setDataSource(context, uri);
                    String strExtractMetadata = r.extractMetadata(9);
                    Long longOrNull = strExtractMetadata != null ? StringsKt.toLongOrNull(strExtractMetadata) : null;
                    r.release();
                    lValueOf = longOrNull;
                    l = lValueOf;
                    return l;
                } catch (Throwable th2) {
                    r.release();
                    throw th2;
                }
            case 2:
                // v1.9.4：嗅探后分别解析 —— APNG 读 acTL/fcTL 延时，GIF 读 GCE 延时
                // v1.10.498：GIF 改流式。原来这里也 readBytes 整份文件 ——
                // 一条 210MB 的 GIF 光「显示来源时长」就能把堆吃穿（选完图界面卡住/OOM）。
                String durSniff = MediaSniff.sniff(context, uri);
                if ("apng".equals(durSniff) || "webp-anim".equals(durSniff)) {
                    byte[] durBytes = readBytes(context, uri);
                    if (durBytes == null) {
                        lValueOf = null;
                    } else if ("apng".equals(durSniff)) {
                        int[] info = ApngFrames.info(durBytes);
                        lValueOf = (info != null && info[3] > 0) ? Long.valueOf(info[3]) : null;
                    } else {
                        // v1.9.6：动图 WebP 时长 = ANMF 延时求和
                        int[] info = WebpAnimFrames.info(durBytes);
                        lValueOf = (info != null && info[3] > 0) ? Long.valueOf(info[3]) : null;
                    }
                } else {
                    Anim streamAnim = probeAnimGifStream(context, uri);
                    lValueOf = (streamAnim != null && streamAnim.durationMs > 0)
                            ? Long.valueOf(streamAnim.durationMs) : null;
                }
                l = lValueOf;
                return l;
            default:
                lValueOf = null;
                l = lValueOf;
                return l;
        }
    }

    /* JADX WARN: Finally extract failed */
    @Nullable
    public final String resolution(@NotNull Context context, @NotNull Uri uri, @NotNull MediaKind kind) throws java.io.IOException {
        String str;
        String string;
        InputStream inputStream;
        Movie movie;
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(uri, "uri");
        Intrinsics.checkNotNullParameter(kind, "kind");
        try {
        } catch (Throwable th) {
            str = null;
        }
        switch (WhenMappings.$EnumSwitchMapping$0[kind.ordinal()]) {
            case 1:
                MediaMetadataRetriever r = new MediaMetadataRetriever();
                try {
                    r.setDataSource(context, uri);
                    String strExtractMetadata = r.extractMetadata(18);
                    Integer w = strExtractMetadata != null ? StringsKt.toIntOrNull(strExtractMetadata) : null;
                    String strExtractMetadata2 = r.extractMetadata(19);
                    Integer h = strExtractMetadata2 != null ? StringsKt.toIntOrNull(strExtractMetadata2) : null;
                    String string2 = (w == null || h == null) ? null : new StringBuilder().append(w).append('x').append(h).toString();
                    r.release();
                    string = string2;
                    str = string;
                    return str;
                } catch (Throwable th2) {
                    r.release();
                    throw th2;
                }
            case 2:
                // v1.9.4：嗅探实际格式分别解析 —— APNG 误按 GIF 读字节 6-9 会得到垃圾尺寸，
                // 预览框比例错乱 → 画面被 fitXY 拉伸。
                byte[] mediaBytes = readBytes(context, uri);
                String sniffed = mediaBytes != null ? MediaSniff.sniffBytes(mediaBytes, mediaBytes.length) : null;
                if ("apng".equals(sniffed)) {
                    int[] info = ApngFrames.info(mediaBytes);
                    if (info != null) {
                        string = new StringBuilder().append(info[0]).append('x').append(info[1]).toString();
                        str = string;
                        return str;
                    }
                    str = null;
                    return str;
                }
                if ("webp-anim".equals(sniffed)) {
                    // v1.9.6：动图 WebP 读 VP8X 画布尺寸
                    int[] info = WebpAnimFrames.info(mediaBytes);
                    if (info != null) {
                        string = new StringBuilder().append(info[0]).append('x').append(info[1]).toString();
                        str = string;
                        return str;
                    }
                    str = null;
                    return str;
                }
                if (mediaBytes != null && mediaBytes.length >= 10
                        && ("gif".equals(sniffed) || sniffed == null)) {
                    int gifW = (mediaBytes[6] & 255) | ((mediaBytes[7] & 255) << 8);
                    int gifH = (mediaBytes[8] & 255) | ((mediaBytes[9] & 255) << 8);
                    if (gifW > 0 && gifH > 0 && gifW < 20000 && gifH < 20000) {
                        string = new StringBuilder().append(gifW).append('x').append(gifH).toString();
                        str = string;
                        return str;
                    }
                }
                str = null;
                return str;
            case 3:
                BitmapFactory.Options $this$resolution_u24lambda_u242 = new BitmapFactory.Options();
                $this$resolution_u24lambda_u242.inJustDecodeBounds = true;
                InputStream inputStreamOpenInputStream2 = context.getContentResolver().openInputStream(uri);
                if (inputStreamOpenInputStream2 != null) {
                    inputStream = inputStreamOpenInputStream2;
                    Throwable th4 = null;
                    try {
                        try {
                            InputStream it2 = inputStream;
                            BitmapFactory.decodeStream(it2, null, $this$resolution_u24lambda_u242);
                            CloseableKt.closeFinally(inputStream, (Throwable) null);
                        } finally {
                        }
                    } finally {
                    }
                }
                string = $this$resolution_u24lambda_u242.outWidth > 0 ? new StringBuilder().append($this$resolution_u24lambda_u242.outWidth).append('x').append($this$resolution_u24lambda_u242.outHeight).toString() : null;
                str = string;
                return str;
            default:
                throw new NoWhenBranchMatchedException();
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockSplitter
        jadx.core.utils.exceptions.JadxRuntimeException: Unexpected missing predecessor for block: B:13:0x0062
        	at jadx.core.dex.visitors.blocks.BlockSplitter.addTempConnectionsForExcHandlers(BlockSplitter.java:280)
        	at jadx.core.dex.visitors.blocks.BlockSplitter.visit(BlockSplitter.java:79)
        */
    @org.jetbrains.annotations.Nullable
   public final Integer video(@NotNull Context context, @NotNull Uri uri) throws java.io.IOException {
      Intrinsics.checkNotNullParameter(context, "context");
      Intrinsics.checkNotNullParameter(uri, "uri");
      MediaExtractor extractor = new MediaExtractor();

      try {
         extractor.setDataSource(context, uri, null);
         int index = 0;

         for (int _ = extractor.getTrackCount(); index < _; index++) {
            MediaFormat var10000 = extractor.getTrackFormat(index);
            Intrinsics.checkNotNullExpressionValue(var10000, "getTrackFormat(...)");
            MediaFormat format = var10000;
            String var31 = format.getString("mime");
            if (var31 != null) {
               String mime = var31;
               if (mime.startsWith("video/") && format.containsKey("frame-rate")) {
                  int var9;
                  try {
                     var9 = format.getInteger("frame-rate");
                  } catch (Throwable var26) {
                     int var11;
                     try {
                        var11 = MathKt.roundToInt(format.getFloat("frame-rate"));
                     } catch (Throwable var25) {
                        return null;
                     }

                     var9 = var11;
                  }

                  if (var9 > 0) {
                     return var9;
                  }
               }
            }
         }

         return null;
      } catch (Throwable var27) {
         return null;
      } finally {
         try {
            extractor.release();
         } catch (Throwable var24) {
         }
      }
   }

    @Nullable
    public final Integer gif(@NotNull Context context, @NotNull Uri uri) throws java.io.IOException {
        byte[] bArr;
        byte[] bArr2;
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(uri, "uri");
        try {
            InputStream inputStreamOpenInputStream = context.getContentResolver().openInputStream(uri);
            if (inputStreamOpenInputStream != null) {
                InputStream inputStream = inputStreamOpenInputStream;
                Throwable th = null;
                try {
                    try {
                        InputStream it = inputStream;
                        byte[] bytes = ByteStreamsKt.readBytes(it);
                        CloseableKt.closeFinally(inputStream, (Throwable) null);
                        bArr2 = bytes;
                    } catch (Throwable th2) {
                        th = th2;
                        throw th2;
                    }
                } catch (Throwable th3) {
                    CloseableKt.closeFinally(inputStream, th);
                    throw th3;
                }
            } else {
                bArr2 = null;
            }
            bArr = bArr2;
        } catch (Throwable th4) {
            bArr = null;
        }
        byte[] bytes2 = bArr;
        // v1.9.4：嗅探 —— APNG 用帧数/总延时算 fps；动图 WebP/静态图没有帧率概念
        String fpsSniff = bytes2 != null ? MediaSniff.sniffBytes(bytes2, bytes2.length) : null;
        if ("apng".equals(fpsSniff)) {
            int[] info = ApngFrames.info(bytes2);
            if (info != null && info[2] > 0 && info[3] > 0) {
                return Integer.valueOf(RangesKt.coerceAtLeast(MathKt.roundToInt(info[2] * 1000.0f / info[3]), 1));
            }
            return null;
        }
        if ("webp-anim".equals(fpsSniff)) {
            // v1.9.6：动图 WebP 帧率 = 帧数/总延时
            int[] info = WebpAnimFrames.info(bytes2);
            if (info != null && info[2] > 0 && info[3] > 0) {
                return Integer.valueOf(RangesKt.coerceAtLeast(MathKt.roundToInt(info[2] * 1000.0f / info[3]), 1));
            }
            return null;
        }
        if ("image".equals(fpsSniff) || "webp-anim".equals(fpsSniff)) {
            return null;
        }
        // v1.9.9：GIF 帧率字节解析抽成静态方法（probeAnimated 一次读取复用）
        return gifFpsBytes(bytes2);
    }

    /**
     * v1.10.0：GIF 帧率 —— 改由 {@link GifFrames#info} 的逐帧延时换算。
     *
     * 老实现的两个坑：
     * 1) 延时全为 0 的 GIF（编码器没写 GCE 延时）总时长算成 0 → 返回 null →
     *    上层只好沿用默认 12fps（用户看到的「识别成 12~16fps 和观感不符」）。
     *    现在按浏览器/GIF 通行规则把 <20ms 的帧当 100ms，帧率一定能算出来。
     * 2) 老循环里 `frames <= 0 && totalDelay > 0` 的分支永远不成立，等于只靠结尾兜底。
     */
    @Nullable
    static Integer gifFpsBytes(@NotNull byte[] bytes2) {
        GifFrames.Meta meta = GifFrames.info(bytes2);
        if (meta == null || meta.frames <= 0 || meta.totalDelayMs <= 0) {
            return null;
        }
        int fpsX10 = fpsX10(meta.frames, meta.totalDelayMs);
        return Integer.valueOf(RangesKt.coerceAtLeast(MathKt.roundToInt(fpsX10 / 10.0f), 1));
    }

    /** 平均帧率 ×10（保留一位小数，避免 12.5 → 13 这种四舍五入误差）。 */
    static int fpsX10(int frames, int totalDelayMs) {
        if (frames <= 0 || totalDelayMs <= 0) {
            return 0;
        }
        long x10 = Math.round(frames * 10000.0 / totalDelayMs);
        return (int) Math.max(1, Math.min(x10, 500));
    }

    @Nullable
    public static Long gifDurationMs(@NotNull byte[] bytes) {
        // v1.10.0：逐帧延时求和（延时 0/极短按 100ms，见 GifFrames）
        GifFrames.Meta meta = GifFrames.info(bytes);
        if (meta == null || meta.totalDelayMs <= 0) {
            return null;
        }
        return Long.valueOf(meta.totalDelayMs);
    }

    /** 动图素材的统一探测结果（v1.10.0）。 */
    public static final class Anim {
        public final int width;
        public final int height;
        public final int frames;
        public final int durationMs;
        /** 帧率 ×10；0 = 未知。 */
        public final int fpsX10;
        /** 帧率是按「推算延时」算出来的（文件里没有可信的每帧时长）。 */
        public final boolean estimated;
        /** v1.10.135：延时表里「补出来」的帧数（0 = 全是文件里真实写的）。 */
        public final int filled;
        @Nullable
        public final int[] delays;

        Anim(int width, int height, int frames, int durationMs, int fpsX10, boolean estimated,
             int filled, @Nullable int[] delays) {
            this.width = width;
            this.height = height;
            this.frames = frames;
            this.durationMs = durationMs;
            this.fpsX10 = fpsX10;
            this.estimated = estimated;
            this.filled = filled;
            this.delays = delays;
        }

        @Override
        @NotNull
        public String toString() {
            return "Anim(" + width + "x" + height + ", frames=" + frames + ", dur=" + durationMs
                    + "ms, fps=" + (fpsX10 / 10.0f) + (estimated ? "≈" : "") + ")";
        }
    }

    /** 一次读取、一次解析：动图的基础参数（尺寸/帧数/时长/帧率/每帧延时）。 */
    @Nullable
    static Anim probeAnim(@Nullable byte[] bytes) {
        if (bytes == null || bytes.length < 16) {
            return null;
        }
        String sniff = MediaSniff.sniffBytes(bytes, bytes.length);
        int fpsX10 = 0;
        int w = 0, h = 0, frames = 0, dur = 0;
        boolean estimated = false;
        int filled = 0;
        int[] delays = null;
        if ("gif".equals(sniff)) {
            GifFrames.Meta meta = GifFrames.info(bytes);
            if (meta == null) {
                return null;
            }
            w = meta.width;
            h = meta.height;
            frames = meta.frames;
            dur = meta.totalDelayMs;
            estimated = meta.estimated;
            filled = meta.filled;
            delays = meta.delays;
            fpsX10 = fpsX10(frames, dur);
        } else if ("apng".equals(sniff)) {
            int[] info = ApngFrames.info(bytes);
            if (info == null) {
                return null;
            }
            w = info[0];
            h = info[1];
            frames = info[2];
            dur = info[3];
            fpsX10 = fpsX10(frames, dur);
        } else if ("webp-anim".equals(sniff)) {
            int[] info = WebpAnimFrames.info(bytes);
            if (info == null) {
                return null;
            }
            w = info[0];
            h = info[1];
            frames = info[2];
            dur = info[3];
            fpsX10 = fpsX10(frames, dur);
        } else {
            return null;
        }
        if (frames <= 0) {
            return null;
        }
        return new Anim(w, h, frames, dur, fpsX10, estimated, filled, delays);
    }

    /**
     * v1.10.498：GIF 专用**流式**探测（不把整份文件读进堆）。
     *
     * <p>为什么单开一条：{@link #probeAnim(byte[])} 要求整份 byte[]，一条 210MB 的 GIF
     * 在 {@link #readAll} 里就要先分配 210MB（扩容峰值接近 2×），
     * OOM 又被吞成 null → 上层只能报「动图解码失败」。GIF 是顺序块结构，
     * 探测只需要走一遍块头，内存恒定。
     */
    @Nullable
    static Anim probeAnimGifStream(@NotNull Context context, @NotNull Uri uri) {
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
            if (in == null) {
                return null;
            }
            GifFrames.Meta meta = GifStream.probe(in);
            if (meta == null || meta.frames <= 0) {
                return null;
            }
            return new Anim(meta.width, meta.height, meta.frames, meta.totalDelayMs,
                    fpsX10(meta.frames, meta.totalDelayMs), meta.estimated, meta.filled, meta.delays);
        } catch (Throwable th) {
            return null;
        } finally {
            closeQuietly(in);
        }
    }

    /** 打开素材流（调用方负责关）；失败返回 null。 */
    @Nullable
    static InputStream openStream(@NotNull Context context, @NotNull Uri uri) {
        try {
            return context.getContentResolver().openInputStream(uri);
        } catch (Throwable th) {
            return null;
        }
    }

    private static void closeQuietly(@Nullable InputStream in) {
        if (in == null) {
            return;
        }
        try {
            in.close();
        } catch (Throwable ignored) {
        }
    }

    /**
     * v1.10.0：只读文件头部（≤128KB）就能拿到的尺寸 —— 导入后「基本信息」立刻上屏，
     * 不用等整份文件读完（大动图要几百毫秒，用户感受就是「信息出来太晚」）。
     * @return {width, height}；读不到返回 null
     */
    @Nullable
    static int[] headerSize(@NotNull Context context, @NotNull Uri uri) {
        byte[] head = readHead(context, uri, 128 * 1024);
        if (head == null || head.length < 24) {
            return null;
        }
        String sniff = MediaSniff.sniffBytes(head, head.length);
        if ("gif".equals(sniff)) {
            return GifFrames.size(head);
        }
        if ("apng".equals(sniff)) {
            // PNG：8 字节签名 + 4 长度 + "IHDR" + w/h
            int w = readIntBE(head, 16);
            int h = readIntBE(head, 20);
            if (w > 0 && h > 0 && w <= 20000 && h <= 20000) {
                return new int[]{w, h};
            }
            return null;
        }
        if ("webp-anim".equals(sniff) || "webp".equals(sniff)) {
            int[] info = WebpAnimFrames.info(head);
            if (info != null) {
                return new int[]{info[0], info[1]};
            }
            // 静态 WebP：VP8X 画布 / VP8 帧头
            return webpSize(head);
        }
        if ("image".equals(sniff)) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            try {
                BitmapFactory.decodeByteArray(head, 0, head.length, opts);
            } catch (Throwable ignored) {
            }
            if (opts.outWidth > 0 && opts.outHeight > 0) {
                return new int[]{opts.outWidth, opts.outHeight};
            }
        }
        return null;
    }

    @Nullable
    private static int[] webpSize(@NotNull byte[] d) {
        int pos = 12;
        while (pos + 8 <= d.length) {
            String fourcc = new String(d, pos, 4);
            int size = (d[pos + 4] & 0xff) | ((d[pos + 5] & 0xff) << 8) | ((d[pos + 6] & 0xff) << 16) | ((d[pos + 7] & 0xff) << 24);
            int dataStart = pos + 8;
            if (size < 0 || dataStart + size > d.length) {
                return null;
            }
            if ("VP8X".equals(fourcc) && size >= 10) {
                int w = 1 + ((d[dataStart + 4] & 0xff) | ((d[dataStart + 5] & 0xff) << 8) | ((d[dataStart + 6] & 0xff) << 16));
                int h = 1 + ((d[dataStart + 7] & 0xff) | ((d[dataStart + 8] & 0xff) << 8) | ((d[dataStart + 9] & 0xff) << 16));
                return new int[]{w, h};
            }
            if ("VP8 ".equals(fourcc) && size >= 10 && (d[dataStart + 3] & 0xff) == 0x9d && (d[dataStart + 4] & 0xff) == 0x01 && (d[dataStart + 5] & 0xff) == 0x2a) {
                int w = ((d[dataStart + 6] & 0xff) | ((d[dataStart + 7] & 0xff) << 8)) & 0x3fff;
                int h = ((d[dataStart + 8] & 0xff) | ((d[dataStart + 9] & 0xff) << 8)) & 0x3fff;
                if (w > 0 && h > 0) {
                    return new int[]{w, h};
                }
            }
            pos = dataStart + size + (size & 1);
        }
        return null;
    }

    private static int readIntBE(@NotNull byte[] d, int off) {
        return ((d[off] & 0xff) << 24) | ((d[off + 1] & 0xff) << 16) | ((d[off + 2] & 0xff) << 8) | (d[off + 3] & 0xff);
    }

    /** 只读前 maxBytes 字节（不整份载入）。 */
    @Nullable
    private static byte[] readHead(@NotNull Context context, @NotNull Uri uri, int maxBytes) {
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
            if (in == null) {
                return null;
            }
            byte[] buffer = new byte[maxBytes];
            int offset = 0;
            while (offset < maxBytes) {
                int read = in.read(buffer, offset, maxBytes - offset);
                if (read <= 0) {
                    break;
                }
                offset += read;
            }
            if (offset <= 0) {
                return null;
            }
            if (offset == maxBytes) {
                return buffer;
            }
            byte[] out = new byte[offset];
            System.arraycopy(buffer, 0, out, 0, offset);
            return out;
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

    /** v1.9.9：包内可见的一次性读取（探测只读文件一遍）。 */
    @Nullable
    static byte[] readAll(@NotNull Context context, @NotNull Uri uri) {
        return readBytes(context, uri);
    }

    @Nullable
    private static byte[] readBytes(@NotNull Context context, @NotNull Uri uri) {        try {
            InputStream in = context.getContentResolver().openInputStream(uri);
            if (in == null) {
                return null;
            }
            try {
                return ByteStreamsKt.readBytes(in);
            } finally {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable th) {
            return null;
        }
    }

    private static final int skipSubBlocks(byte[] bytes, int start) {
        int pos;
        int i = start;
        while (true) {
            pos = i;
            if (pos >= bytes.length) {
                break;
            }
            int size = bytes[pos] & 255;
            pos++;
            if (size == 0) {
                break;
            }
            i = pos + size;
        }
        return pos;
    }
}
