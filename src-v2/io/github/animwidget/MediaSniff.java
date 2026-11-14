package io.github.animwidget;

import android.content.Context;
import android.graphics.ImageDecoder;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import java.io.InputStream;
import java.util.Locale;

/**
 * 文件头魔数嗅探（v1.8.15）。
 *
 * 系统选择器的 MIME 过滤在部分 ROM（vivo 相册等）上形同虚设：
 * GIF 页一样能选中 MP4。选完不信声明类型，读文件前段自己判：
 *   GIF87a/89a        → "gif"
 *   RIFF....WEBP+ANIM → "webp-anim"（动图 WebP）
 *   PNG + acTL        → "apng"
 *   ftyp avif/heic/…  → "image"；ftyp avis/msf1（序列）→ "heif-anim"
 *   ....ftyp（其它）  → "video"
 *   其它常见图片头     → "image"
 *   认不出            → null（维持用户所选）
 *
 * v1.10.26：
 *  - 读取前 64KB（原来 1024 字节，大 PNG 的 acTL 在 tEXt/iCCP 之后就漏判）
 *  - 支持 AVIF / HEIC 序列（动图）识别
 *  - 新增 isAnimatedImage()：嗅探 + ImageDecoder 兜底，图片页一律拦动图
 */
final class MediaSniff {

    private MediaSniff() {
    }

    static String sniff(Context context, Uri uri) {
        try {
            InputStream in = context.getContentResolver().openInputStream(uri);
            if (in == null) {
                return null;
            }
            // v1.10.0：64 → 1024 —— 有些 APNG 的 acTL 前面还有 tEXt/gAMA 块，
            // 64 字节读不到 acTL 就会被当成静态图（GIF 页直接拒绝）
            // v1.10.26：1024 → 65536 —— 相册里的大 PNG（带 iCCP/eXIf）acTL 常常在几 KB 之后
            byte[] head = new byte[65536];
            int n = 0;
            try {
                while (n < head.length) {
                    int r = in.read(head, n, head.length - n);
                    if (r < 0) {
                        break;
                    }
                    n += r;
                }
            } finally {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
            }
            return sniffBytes(head, n);
        } catch (Throwable th) {
            return null;
        }
    }

    static String sniffBytes(byte[] b, int n) {
        if (n >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8'
                && (b[4] == '7' || b[4] == '9') && b[5] == 'a') {
            return "gif";
        }
        if (n >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            // v1.9.5：VP8X 标志字节在偏移 16，动画位 = bit4(0x10)
            // （之前误读偏移 20 —— 那是画布宽度字节，动图 WebP 会被判成静态图）
            // v1.10.31：必须先确认是扩展格式 VP8X —— 老的 VP8/VP8L 静态 WebP 第 17 字节
            // 是图像数据，bit4 完全可能碰巧为 1，原来那样判会把静态 WebP 当成动图
            // （选择器里「还能看到普通图片」就是它漏的）。
            boolean vp8x = n >= 16 && b[12] == 'V' && b[13] == 'P' && b[14] == '8' && b[15] == 'X';
            if (vp8x && n >= 17 && (b[16] & 0x10) != 0) {
                return "webp-anim";
            }
            if (contains(b, n, "ANIM") || contains(b, n, "ANMF")) {
                return "webp-anim";
            }
            return "image";
        }
        if (n >= 8 && (b[0] & 0xff) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            if (contains(b, n, "acTL")) {
                return "apng";
            }
            return "image";
        }
        if (n >= 12 && b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') {
            // v1.10.26：ISO-BMFF 家族里 AVIF/HEIC 也是 ftyp，
            // 原来一律当 video —— 图片页选到"动态 HEIC/AVIF"看不出毛病
            String major = fourcc(b, 8);
            if (isIsoImageBrand(major)) {
                boolean sequence = "avis".equals(major) || "msf1".equals(major)
                        || "hevc".equals(major) || "hevx".equals(major)
                        || contains(b, n, "avis") || contains(b, n, "msf1");
                return sequence ? "heif-anim" : "image";
            }
            return "video";
        }
        if (n >= 3 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8) {
            return "image"; // jpeg
        }
        if (n >= 2 && b[0] == 'B' && b[1] == 'M') {
            return "image";
        }
        return null;
    }

    /** ftyp 的 major brand 是否是「图片类」ISO 容器（AVIF / HEIC / MIAF …）。 */
    private static boolean isIsoImageBrand(String major) {
        return "avif".equals(major) || "avis".equals(major)
                || "heic".equals(major) || "heix".equals(major)
                || "heim".equals(major) || "heis".equals(major)
                || "hevc".equals(major) || "hevx".equals(major)
                || "mif1".equals(major) || "msf1".equals(major)
                || "mif2".equals(major) || "miaf".equals(major);
    }

    private static String fourcc(byte[] b, int off) {
        try {
            char[] c = new char[4];
            for (int i = 0; i < 4; i++) {
                c[i] = (char) (b[off + i] & 0xff);
            }
            return new String(c);
        } catch (Throwable th) {
            return "";
        }
    }

    /**
     * v1.10.26：图片页「这到底是不是动图」的最终判定。
     *
     * 嗅探能认出 gif / apng / 动图 webp / AVIF 序列，但有两类会漏：
     *   1) acTL 在 64KB 之外的超大 PNG
     *   2) 某些 ROM 包装过的动图（容器套壳）
     * 所以再用系统解码器兜底问一次 —— ImageDecoder 只有在真是多帧时才给
     * AnimatedImageDrawable，这是最权威的答案。解码目标 1×1，几乎不占内存。
     */
    static boolean isAnimatedImage(Context context, Uri uri, String sniffed) {
        if ("gif".equals(sniffed) || "apng".equals(sniffed) || "webp-anim".equals(sniffed)
                || "heif-anim".equals(sniffed)) {
            return true;
        }
        if ("video".equals(sniffed)) {
            return true;
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.Source src = ImageDecoder.createSource(context.getContentResolver(), uri);
                Drawable d = ImageDecoder.decodeDrawable(src, (decoder, info, source) -> {
                    try {
                        decoder.setTargetSize(1, 1);
                    } catch (Throwable ignored) {
                    }
                });
                return d instanceof AnimatedImageDrawable;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean contains(byte[] b, int n, String tag) {
        int t0 = tag.charAt(0), t1 = tag.charAt(1), t2 = tag.charAt(2), t3 = tag.charAt(3);
        for (int i = 0; i + 3 < n; i++) {
            if (b[i] == t0 && b[i + 1] == t1 && b[i + 2] == t2 && b[i + 3] == t3) {
                return true;
            }
        }
        return false;
    }

    /** 嗅探结果对应的 MediaKind；认不出返回传入值。 */
    static MediaKind kindOf(String sniff, MediaKind fallback) {
        if (sniff == null) {
            return fallback;
        }
        switch (sniff.toLowerCase(Locale.US)) {
            case "gif":
            case "apng":
            case "webp-anim":
            case "heif-anim":
                return MediaKind.GIF;
            case "video":
                return MediaKind.VIDEO;
            case "image":
                return MediaKind.IMAGE;
            default:
                return fallback;
        }
    }
}
