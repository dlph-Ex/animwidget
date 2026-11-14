package io.github.animwidget;

import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 日志导出（v1.10.27，v1.10.47 改名）。
 *
 * 两种产物：
 *   1) TXT —— 全文一次写完，文件名 {@code Component_<年份后两位+小时>_<分>_<秒>.txt}
 *      （2025-10-23 18:37:25 → Component_2518_37_25.txt）；
 *   2) 分批长图 —— 每张最多 100 行，固定像素宽高、PNG 无损，不做任何降采样。
 *
 * 保存位置（Android 10+，不需要任何权限）：
 *   TXT → Download/Component_…….txt（下载根目录）
 *   PNG → Pictures/animwidget-log-时间-01of03.png
 * Android 9 及以下落到应用专属外部目录（无需存储权限）。
 *
 * 注意：MediaStore.Downloads 是 API 29 才有的类，所以「新路径」全部拆进独立方法，
 * 旧系统根本不会解析到那些常量（避免 NoClassDefFoundError）。
 *
 * v1.10.47：删掉 A13/A14/A15/A16 那一串诊断（用户要求）。只保留失败时的
 * 一行 A03 —— 写入失败必须留痕，否则用户只看到「导出成功」却落了私有目录。
 */
final class LogExporter {

    static final int LINES_PER_IMAGE = 100;
    static final int FONT_SIZE = 18;
    static final int LINE_HEIGHT = 26;
    static final int PAD = 24;
    static final int HEADER = 84;

    /** v1.10.35：最后一次写入的 content URI —— 「导出并打开」用它 */
    @Nullable
    private static volatile Uri lastUri;

    @Nullable
    static Uri getLastUri() {
        return lastUri;
    }

    private LogExporter() {
    }

    /** 导出 TXT。@return 给用户看的保存位置 */
    static String exportTxt(Context context, String text) throws Exception {
        String name = txtName();
        byte[] bytes = text.getBytes("UTF-8");
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                return saveTxtModern(context, name, bytes);
            } catch (Throwable th) {
                // 必须留痕：否则用户看到「导出成功」，实际落在了私有目录。
                Diag.warn(context, "A03", "日志写入下载目录失败，回退应用目录: "
                        + th.getClass().getSimpleName() + "/" + th.getMessage());
                return saveLegacy(context, name, bytes) + "（下载目录不可写，已存到应用目录）";
            }
        }
        return saveLegacy(context, name, bytes);
    }

    /** 分批导出长图。@return 给用户看的保存位置（张数 + 目录） */
    static String exportImages(Context context, String text) throws Exception {
        List<String> lines = splitLines(text);
        if (lines.isEmpty()) {
            throw new IllegalStateException(context.getString(R.string.log_export_empty));
        }
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.MONOSPACE);
        paint.setTextSize(FONT_SIZE);
        paint.setColor(0xFFE8E8E8);
        float widest = 0f;
        for (int i = 0; i < lines.size(); i++) {
            widest = Math.max(widest, paint.measureText(lines.get(i)));
        }
        int width = (int) Math.min(3600, Math.max(1080, widest + PAD * 2));
        int batches = (lines.size() + LINES_PER_IMAGE - 1) / LINES_PER_IMAGE;
        String stamp = stamp();
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
        titlePaint.setTextSize(26);
        titlePaint.setColor(0xFF9AD0FF);
        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(0xFF3C3C3C);
        linePaint.setStrokeWidth(2.0f);
        String dateLabel = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        int doneCount = 0;
        for (int b = 0; b < batches; b++) {
            int from = b * LINES_PER_IMAGE;
            int rows = Math.min(LINES_PER_IMAGE, lines.size() - from);
            int height = HEADER + rows * LINE_HEIGHT + PAD;
            Bitmap bitmap = null;
            try {
                // v1.10.28：RGB_565 —— 纯色背景 + 单色文字用不到 alpha，
                // 内存和 PNG 编码量直接减半（长图导出从「十几秒」降到「几秒」）
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(0xFF121212);
                // 页眉：时间 + 第几张 + 总行数（留白，避免顶到边缘）
                canvas.drawText("AnimWidget 日志  " + dateLabel + "   "
                        + (b + 1) + "/" + batches + "  (" + lines.size() + " 行)", PAD, 44, titlePaint);
                canvas.drawLine(PAD, 58, width - PAD, 58, linePaint);
                float y = HEADER + LINE_HEIGHT - 6;
                for (int i = 0; i < rows; i++) {
                    canvas.drawText(lines.get(from + i), PAD, y, paint);
                    y += LINE_HEIGHT;
                }
                String name = "animwidget-log-" + stamp + "-"
                        + String.format(Locale.US, "%02d", b + 1)
                        + "of" + String.format(Locale.US, "%02d", batches) + ".png";
                if (Build.VERSION.SDK_INT >= 29) {
                    savePngModern(context, bitmap, name);
                } else {
                    saveLegacy(context, name, bitmap);
                }
                doneCount++;
            } finally {
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        }
        if (doneCount == 0) {
            throw new IllegalStateException("长图 0 张");
        }
        if (Build.VERSION.SDK_INT >= 29) {
            // 通知系统重新索引整个目录（相册/文件管理器都能立刻看到）
            String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures";
            scanFile(context, dir, "image/png");
        }
        return batches + " 张 · " + (Build.VERSION.SDK_INT >= 29
                ? Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures"
                : "应用专属目录");
    }

    // ---------------------------------------------------------------- 新路径（API 29+）

    private static String saveTxtModern(Context context, String name, byte[] bytes) throws Exception {
        long nowSec = System.currentTimeMillis() / 1000L;
        // v1.10.45：回到最标准的 MediaStore.Downloads 写法：
        //   1) 插入时 IS_PENDING=1（文件未完成状态）
        //   2) 写完 → 置 IS_PENDING=0 + 刷修改时间
        //   3) 再触发一次系统扫描
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        values.put(MediaStore.MediaColumns.DATE_ADDED, nowSec);
        values.put(MediaStore.MediaColumns.DATE_MODIFIED, nowSec);
        values.put(MediaStore.MediaColumns.IS_PENDING, Integer.valueOf(1));

        Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri uri = context.getContentResolver().insert(collection, values);
        if (uri == null) {
            uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        }
        if (uri == null) {
            throw new IllegalStateException("MediaStore 插入失败（insert 返回 null）");
        }
        OutputStream out = context.getContentResolver().openOutputStream(uri);
        if (out == null) {
            throw new IllegalStateException("openOutputStream 返回 null");
        }
        try {
            out.write(bytes);
            out.flush();
        } finally {
            out.close();
        }
        // 置为「已完成」——这一步不给，索引里永远是 pending，系统应用看不到
        ContentValues done = new ContentValues();
        done.put(MediaStore.MediaColumns.IS_PENDING, Integer.valueOf(0));
        done.put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000L);
        context.getContentResolver().update(uri, done, null, null);
        lastUri = uri;

        String absolute = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Download/" + name;
        scanFile(context, absolute);
        ExportHistory.add(context, absolute, uri.toString(), name, bytes.length, false);
        return absolute + "（" + human(bytes.length) + "）";
    }

    /** 查 MediaStore 里这条记录的大小；查不到返回 -1。 */
    private static long querySize(Context context, Uri uri) {
        android.database.Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri,
                    new String[]{MediaStore.MediaColumns.SIZE}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Throwable ignored) {
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Throwable ignored) {
                }
            }
        }
        return -1L;
    }

    private static long savePngModern(Context context, Bitmap bitmap, String name) throws Exception {
        long nowSec = System.currentTimeMillis() / 1000L;
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
        values.put(MediaStore.MediaColumns.DATE_ADDED, nowSec);
        values.put(MediaStore.MediaColumns.DATE_MODIFIED, nowSec);
        values.put(MediaStore.MediaColumns.IS_PENDING, Integer.valueOf(1));
        Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IllegalStateException("MediaStore insert 返回 null");
        }
        OutputStream out = context.getContentResolver().openOutputStream(uri);
        if (out == null) {
            throw new IllegalStateException("openOutputStream 返回 null");
        }
        try {
            // PNG 无损：不做任何采样，长图也不会糊
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
        } finally {
            out.close();
        }
        ContentValues done = new ContentValues();
        done.put(MediaStore.MediaColumns.IS_PENDING, Integer.valueOf(0));
        done.put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000L);
        context.getContentResolver().update(uri, done, null, null);
        long stored = querySize(context, uri);
        // v1.10.38：登记到「历史导出」
        ExportHistory.add(context,
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/" + name,
                uri.toString(), name, stored, true);
        return stored;
    }

    // ---------------------------------------------------------------- 旧路径（API 26-28）

    private static String saveLegacy(Context context, String name, byte[] bytes) throws Exception {
        File file = new File(legacyDir(context), name);
        FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(bytes);
            out.flush();
        } finally {
            out.close();
        }
        scanFile(context, file.getAbsolutePath(), "text/plain");
        return file.getAbsolutePath();
    }

    private static void saveLegacy(Context context, String name, Bitmap bitmap) throws Exception {
        File file = new File(legacyDir(context), name);
        FileOutputStream out = new FileOutputStream(file);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
        } finally {
            out.close();
        }
    }

    private static File legacyDir(Context context) {
        File dir = new File(context.getExternalFilesDir(null), "export");
        dir.mkdirs();
        return dir;
    }

    // ---------------------------------------------------------------- 工具

    private static List<String> splitLines(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        for (String raw : text.split("\n", -1)) {
            if (raw.trim().length() == 0) {
                continue;
            }
            out.add(raw.replace("\t", "    ").replace("\r", ""));
        }
        return out;
    }

    /** 请求系统重新索引指定路径（文件或目录）。 */
    private static void scanFile(final Context context, final String path) {
        scanFile(context, path, "text/plain");
    }

    private static void scanFile(final Context context, final String path, final String mime) {
        try {
            android.media.MediaScannerConnection.scanFile(context, new String[]{path},
                    mime != null ? new String[]{mime} : null, null);
            Intent legacy = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            legacy.setData(Uri.fromFile(new File(path)));
            context.sendBroadcast(legacy);
        } catch (Throwable ignored) {
        }
    }

    private static String human(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
    }

    /**
     * v1.10.47：TXT 统一命名 {@code Component_N_N_N.txt}。
     *   第 1 个 N = 年份后两位 + 小时两位，例 2025-10-23 18:37:25 → 2518
     *   第 2 个 N = 分钟（两位）→ 37
     *   第 3 个 N = 秒（两位）→ 25
     * 即 Component_2518_37_25.txt。三段取自同一个时间戳，跨秒也不会自相矛盾。
     */
    static String txtName() {
        Date now = new Date();
        String yh = new SimpleDateFormat("yyHH", Locale.US).format(now);
        String mm = new SimpleDateFormat("mm", Locale.US).format(now);
        String ss = new SimpleDateFormat("ss", Locale.US).format(now);
        return "Component_" + yh + "_" + mm + "_" + ss + ".txt";
    }

    private static String stamp() {
        return new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
    }
}
