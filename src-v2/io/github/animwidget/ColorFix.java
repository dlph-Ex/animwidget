package io.github.animwidget;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorSpace;
import android.graphics.Paint;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 色彩统一（v1.10.1）。
 *
 * 素材色差的根因：解码出来的位图可能是广色域（Display P3 / BT.2020）或非 ARGB_8888，
 * 而我们落盘用的是 WebP / PNG —— 这两种编码不写 ICC 配置，广色域像素值被当成 sRGB 读回，
 * 于是预览、导出、桌面全都偏色（典型的「更艳 / 更灰」）。
 *
 * 做法：解码后**立刻归一化成 sRGB + ARGB_8888**，之后所有环节（缩放/合成/编码）都在 sRGB 里，
 * 颜色只转一次、且是标准转换。视频帧同理（MediaMetadataRetriever 给的可能带色彩空间）。
 */
final class ColorFix {

    @Nullable
    private static ColorSpace srgb;

    private ColorFix() {
    }

    @NotNull
    static ColorSpace srgb() {
        ColorSpace cs = srgb;
        if (cs == null) {
            cs = ColorSpace.get(ColorSpace.Named.SRGB);
            srgb = cs;
        }
        return cs;
    }

    /** 解码选项：ARGB_8888 + 直接解成 sRGB（避免多一次拷贝）。 */
    @NotNull
    static BitmapFactory.Options options() {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try {
            opts.inPreferredColorSpace = srgb();
        } catch (Throwable ignored) {
        }
        return opts;
    }

    /**
     * 归一化成 sRGB + ARGB_8888。
     * 已经是 sRGB ARGB_8888 时**原样返回**（不复制）；否则返回新图（调用方负责回收）。
     */
    @Nullable
    static Bitmap toSrgb(@Nullable Bitmap src) {
        if (src == null || src.isRecycled()) {
            return src;
        }
        boolean needColor = false;
        try {
            ColorSpace cs = src.getColorSpace();
            needColor = cs != null && !cs.isSrgb();
        } catch (Throwable ignored) {
        }
        // v1.10.58：**硬件位图必须先转成软件位图**。
        // 硬件位图（API 28+ ImageDecoder 默认分配器）不能被软件 Canvas 绘制，也不能 compress()；
        // 而下面那条 Canvas 路径本身就会因为「软件 Canvas 画硬件位图」抛异常，
        // 被 catch 吞掉后原样返回硬件位图 —— 上游再一画就崩（动图组件每帧都失败）。
        boolean hardware = false;
        try {
            hardware = src.getConfig() == Bitmap.Config.HARDWARE;
        } catch (Throwable ignored) {
        }
        if (hardware) {
            try {
                Bitmap copy = src.copy(Bitmap.Config.ARGB_8888, false);
                if (copy != null) {
                    return copy;
                }
            } catch (Throwable ignored) {
            }
            return null;
        }
        boolean needConfig = src.getConfig() != Bitmap.Config.ARGB_8888;
        if (!needColor && !needConfig) {
            return src;
        }
        try {
            Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(),
                    Bitmap.Config.ARGB_8888, true, srgb());
            if (out == null) {
                return src;
            }
            Canvas canvas = new Canvas(out);
            canvas.drawBitmap(src, 0.0f, 0.0f, new Paint(3));
            return out;
        } catch (Throwable th) {
            return src;
        }
    }

    /** 诊断用：这块位图到底是什么色彩空间 / 什么格式。 */
    @NotNull
    static String describe(@Nullable Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return "null";
        }
        String cs;
        try {
            ColorSpace space = bitmap.getColorSpace();
            cs = space == null ? "sRGB(默认)" : space.getName();
        } catch (Throwable th) {
            cs = "?";
        }
        String config;
        try {
            config = String.valueOf(bitmap.getConfig());
        } catch (Throwable th) {
            config = "?";
        }
        return cs + "/" + config;
    }

    /** 诊断用：中心点像素（#RRGGBB），和原文件对比色差时直接比这个数。 */
    @NotNull
    static String sample(@Nullable Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled() || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
            return "null";
        }
        try {
            int x = bitmap.getWidth() / 2;
            int y = bitmap.getHeight() / 2;
            int color = bitmap.getPixel(x, y);
            return String.format(java.util.Locale.US, "#%06X", color & 0xFFFFFF)
                    + " @(" + x + "," + y + ") alpha=" + ((color >>> 24) & 0xFF);
        } catch (Throwable th) {
            return "?";
        }
    }
}
