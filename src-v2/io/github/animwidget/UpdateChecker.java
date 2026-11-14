package io.github.animwidget;

import android.app.Activity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * v1.10.386：**检测更新**（「关于」页版本行点一下就走这里）。
 *
 * <h3>为什么是 GitHub Releases 而不是自建接口</h3>
 * 仓库本来就放在 GitHub，Release 的 `tag_name` 就是版本号，不需要额外服务；
 * 代价是必须申请 `INTERNET` 权限（此前全应用零网络）。本类只做一件事：
 * 拉 `releases/latest` 的 JSON、正则取 `tag_name`、和当前 versionName 比大小。
 *
 * <h3>线程与失败</h3>
 * 网络在独立线程跑，回调切回 UI 线程；任何异常都只报「检测失败」，不抛给调用方 ——
 * 这个按钮不能因为没网就把设置页搞崩。
 */
final class UpdateChecker {

    /** 仓库地址（与「开源仓库」行显示的一致，一处定义两处用）。 */
    static final String REPO_URL = "https://github.com/dlph-Ex/animwidget";
    static final String RELEASES_URL = REPO_URL + "/releases/latest";

    private static final String API = "https://api.github.com/repos/dlph-Ex/animwidget/releases/latest";
    private static final Pattern TAG = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");

    private UpdateChecker() {
    }

    interface Callback {
        /**
         * @param ok    网络与解析是否成功
         * @param tag   远端版本号（去掉前缀 v），失败时为 null
         * @param error 失败原因短名（只在诊断时用）
         */
        void onResult(boolean ok, @Nullable String tag, @Nullable String error);
    }

    static void check(@NotNull final Activity activity, @NotNull final Callback callback) {
        Thread t = new Thread(() -> {
            boolean ok = false;
            String tag = null;
            String error = null;
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(API).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setRequestProperty("User-Agent", "FrameSmith-UpdateCheck");
                int code = conn.getResponseCode();
                if (code == 200) {
                    InputStream in = conn.getInputStream();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        bos.write(buf, 0, n);
                    }
                    in.close();
                    Matcher m = TAG.matcher(bos.toString("UTF-8"));
                    if (m.find()) {
                        tag = strip(m.group(1));
                        ok = true;
                    } else {
                        error = "no-tag";
                    }
                } else {
                    error = "http-" + code;
                }
            } catch (Throwable th) {
                error = th.getClass().getSimpleName();
            } finally {
                if (conn != null) {
                    try {
                        conn.disconnect();
                    } catch (Throwable ignored) {
                    }
                }
            }
            final boolean fOk = ok;
            final String fTag = tag;
            final String fErr = error;
            activity.runOnUiThread(() -> callback.onResult(fOk, fTag, fErr));
        }, "update-check");
        t.setDaemon(true);
        t.start();
    }

    /** 去掉 `v` / `V` 前缀与空白。 */
    @Nullable
    static String strip(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.startsWith("v") || s.startsWith("V")) {
            s = s.substring(1);
        }
        return s;
    }

    /**
     * 版本号比较：按点分段比数值（`1.10.386` vs `1.10.400`）。
     * 段数不同时短的补 0；非数字段按 0 处理（我们的版本号一直是纯数字）。
     *
     * @return &gt;0 表示 a 更新
     */
    static int compare(@Nullable String a, @Nullable String b) {
        int[] x = parts(a);
        int[] y = parts(b);
        int n = Math.max(x.length, y.length);
        for (int i = 0; i < n; i++) {
            int va = i < x.length ? x[i] : 0;
            int vb = i < y.length ? y[i] : 0;
            if (va != vb) {
                return va > vb ? 1 : -1;
            }
        }
        return 0;
    }

    private static int[] parts(@Nullable String v) {
        String s = strip(v);
        if (s == null || s.isEmpty()) {
            return new int[0];
        }
        String[] seg = s.split("\\.");
        int[] out = new int[seg.length];
        for (int i = 0; i < seg.length; i++) {
            int value = 0;
            String cur = seg[i].trim();
            int end = 0;
            while (end < cur.length() && Character.isDigit(cur.charAt(end))) {
                end++;
            }
            if (end > 0) {
                try {
                    value = Integer.parseInt(cur.substring(0, end));
                } catch (Throwable ignored) {
                    value = 0;
                }
            }
            out[i] = value;
        }
        return out;
    }
}
