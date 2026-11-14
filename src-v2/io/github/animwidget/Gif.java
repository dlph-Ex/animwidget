package io.github.animwidget;

import android.graphics.Movie;

import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * GIF 解码工具（v1.8.5）。
 *
 * `Movie.decodeStream()` 在部分 ROM / content:// 流上会直接返回 null
 * （流不可 seek），而这里所有路径（缩略图、尺寸探测、导入、悬浮层）
 * 统一改成「读全字节 → decodeByteArray」，跟导入链路保持一致。
 */
final class Gif {

    private Gif() {
    }

    @Nullable
    static Movie decode(@Nullable InputStream input) {
        if (input == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = input.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            byte[] data = out.toByteArray();
            if (data.length == 0) {
                return null;
            }
            return Movie.decodeByteArray(data, 0, data.length);
        } catch (Throwable th) {
            return null;
        } finally {
            try {
                input.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
