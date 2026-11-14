package io.github.animwidget;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 日志导出记录（v1.10.38）。
 *
 * 「历史导出」页面读的就是这份数据：每次导出成功登记一条（路径 + content URI + 时间 + 大小）。
 * 列表展示时会过滤掉文件已不存在的（用户手动删过就不显示），所以不需要额外的“删除”状态。
 */
final class ExportHistory {

    private static final String PREF = "export_history";
    private static final String KEY = "items";
    private static final int MAX = 300;

    static final class Entry {
        String path;
        String uri;
        String name;
        long time;
        long bytes;
        boolean images;
    }

    private ExportHistory() {
    }

    static synchronized void add(Context context, String path, String uri, String name, long bytes, boolean images) {
        try {
            List<Entry> list = read(context);
            Entry entry = new Entry();
            entry.path = path != null ? path : "";
            entry.uri = uri != null ? uri : "";
            entry.name = name != null ? name : "";
            entry.time = System.currentTimeMillis();
            entry.bytes = bytes;
            entry.images = images;
            list.add(0, entry);
            while (list.size() > MAX) {
                list.remove(list.size() - 1);
            }
            write(context, list);
        } catch (Throwable ignored) {
        }
    }

    /** 只返回文件仍然存在的记录（已删除的不显示）。 */
    static synchronized List<Entry> list(Context context) {
        List<Entry> all = read(context);
        List<Entry> out = new ArrayList<>();
        boolean changed = false;
        for (Entry entry : all) {
            if (exists(context, entry)) {
                out.add(entry);
            } else {
                changed = true;
            }
        }
        if (changed) {
            write(context, out);
        }
        return out;
    }

    /** 删除记录 + 删文件（手动删除）。 */
    static synchronized void remove(Context context, Entry entry) {
        try {
            deleteFile(context, entry);
        } catch (Throwable ignored) {
        }
        List<Entry> list = read(context);
        List<Entry> out = new ArrayList<>();
        for (Entry one : list) {
            if (one.time == entry.time && sameName(one.name, entry.name)) {
                continue;
            }
            out.add(one);
        }
        write(context, out);
    }

    private static boolean sameName(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean exists(Context context, Entry entry) {
        try {
            if (entry.uri != null && entry.uri.length() > 0) {
                ContentResolver resolver = context.getContentResolver();
                Cursor cursor = resolver.query(Uri.parse(entry.uri), new String[]{"_id"}, null, null, null);
                if (cursor != null) {
                    boolean ok = cursor.moveToFirst();
                    cursor.close();
                    if (ok) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return entry.path != null && entry.path.length() > 0 && new File(entry.path).exists();
    }

    private static void deleteFile(Context context, Entry entry) {
        if (entry.uri != null && entry.uri.length() > 0) {
            try {
                context.getContentResolver().delete(Uri.parse(entry.uri), null, null);
                return;
            } catch (Throwable ignored) {
            }
        }
        if (entry.path != null && entry.path.length() > 0) {
            new File(entry.path).delete();
        }
    }

    private static List<Entry> read(Context context) {
        List<Entry> out = new ArrayList<>();
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF, 0);
            JSONArray array = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                Entry entry = new Entry();
                entry.path = object.optString("path", "");
                entry.uri = object.optString("uri", "");
                entry.name = object.optString("name", "");
                entry.time = object.optLong("time", 0L);
                entry.bytes = object.optLong("bytes", 0L);
                entry.images = object.optBoolean("images", false);
                out.add(entry);
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static void write(Context context, List<Entry> list) {
        try {
            JSONArray array = new JSONArray();
            for (Entry entry : list) {
                JSONObject object = new JSONObject();
                object.put("path", entry.path);
                object.put("uri", entry.uri);
                object.put("name", entry.name);
                object.put("time", entry.time);
                object.put("bytes", entry.bytes);
                object.put("images", entry.images);
                array.put(object);
            }
            context.getSharedPreferences(PREF, 0).edit().putString(KEY, array.toString()).apply();
        } catch (Throwable ignored) {
        }
    }
}
