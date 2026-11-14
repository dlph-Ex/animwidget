package io.github.animwidget;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

/**
 * v1.10.104（需求 2.1）：**开屏光轮的配色来自桌面壁纸**。
 *
 * 做法：
 *   1) 把壁纸缩到 48×48 再算直方图（4bit/通道，4096 桶，桶内取平均色）；
 *   2) 先丢掉「太暗 / 太灰」的桶（壁纸大面积的黑白灰做光轮不好看）；
 *   3) 从出现最多的颜色开始，用 **farthest-first** 依次挑「离已选颜色最远」的那一个，
 *      并且要求任意两色的 RGB 欧氏距离 > {@link #MIN_DIST}（你说的「不能过于接近 ≤10」）；
 *   4) 挑出来的 5 个色再提一点饱和度/压一点亮度，保证叠在模糊背景上仍然是「光」不是「土」。
 *
 * 采样一次就存进 prefs，之后开屏沿用同一套（否则每次进都不一样，观感不稳）。
 * 取不到壁纸（部分 ROM 会拦截）→ 返回 null，调用方回落到系统配色 —— 特性保留。
 */
public final class WallpaperPalette {

    public static final int COUNT = 5;
    /** 两色 RGB 欧氏距离必须大于它，否则算「过于接近」。 */
    public static final double MIN_DIST = 10.0;

    private static final String PREFS = "settings";
    private static final String KEY = "intro_ring_colors";
    private static final int BUCKETS = 4096;

    private WallpaperPalette() {
    }

    /** 已采样过的 5 色（没有则 null）。 */
    public static int[] cached(Context c) {
        String raw = c.getSharedPreferences(PREFS, 0).getString(KEY, null);
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split(",");
        if (parts.length != COUNT) {
            return null;
        }
        int[] out = new int[COUNT];
        try {
            for (int i = 0; i < COUNT; i++) {
                out[i] = Integer.parseInt(parts[i].trim()) | 0xFF000000;
            }
        } catch (Throwable th) {
            return null;
        }
        return out;
    }

    /** 有缓存用缓存，没有就采样并落盘。取不到壁纸返回 null。 */
    public static int[] ensure(Context c) {
        int[] hit = cached(c);
        if (hit != null) {
            return hit;
        }
        int[] sampled = sample(c);
        if (sampled == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < COUNT; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(sampled[i]);
        }
        c.getSharedPreferences(PREFS, 0).edit().putString(KEY, sb.toString()).apply();
        return sampled;
    }

    /** 清掉缓存（以后要「重新采集」时用）。 */
    public static void clear(Context c) {
        c.getSharedPreferences(PREFS, 0).edit().remove(KEY).apply();
    }

    private static int[] sample(Context c) {
        Bitmap thumb = null;
        try {
            WallpaperManager manager = WallpaperManager.getInstance(c);
            Drawable drawable = manager.getDrawable();
            if (drawable == null) {
                return null;
            }
            thumb = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(thumb);
            drawable.setBounds(0, 0, 48, 48);
            drawable.draw(canvas);
            return pick(thumb);
        } catch (Throwable th) {
            return null;
        } finally {
            if (thumb != null && !thumb.isRecycled()) {
                thumb.recycle();
            }
        }
    }

    private static int[] pick(Bitmap bmp) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int[] px = new int[w * h];
        bmp.getPixels(px, 0, w, 0, 0, w, h);

        int[] count = new int[BUCKETS];
        int[] sumR = new int[BUCKETS];
        int[] sumG = new int[BUCKETS];
        int[] sumB = new int[BUCKETS];
        float[] hsv = new float[3];
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            if (((p >>> 24) & 0xFF) < 200) {
                continue;
            }
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;
            Color.RGBToHSV(r, g, b, hsv);
            if (hsv[2] < 0.10f || hsv[1] < 0.08f) {
                continue;
            }
            int key = ((r >> 4) << 8) | ((g >> 4) << 4) | (b >> 4);
            count[key]++;
            sumR[key] += r;
            sumG[key] += g;
            sumB[key] += b;
        }

        int[] picked = pickFrom(count, sumR, sumG, sumB);
        if (picked == null) {
            // 壁纸本身就是黑白灰：放宽滤镜，至少别让开屏变单色
            for (int i = 0; i < px.length; i++) {
                int p = px[i];
                if (((p >>> 24) & 0xFF) < 200) {
                    continue;
                }
                int r = (p >> 16) & 0xFF;
                int g = (p >> 8) & 0xFF;
                int b = p & 0xFF;
                int key = ((r >> 4) << 8) | ((g >> 4) << 4) | (b >> 4);
                count[key]++;
                sumR[key] += r;
                sumG[key] += g;
                sumB[key] += b;
            }
            picked = pickFrom(count, sumR, sumG, sumB);
        }
        if (picked == null) {
            return null;
        }
        int[] out = new int[COUNT];
        for (int i = 0; i < COUNT; i++) {
            out[i] = liven(picked[i]);
        }
        return out;
    }

    private static int[] pickFrom(int[] count, int[] sumR, int[] sumG, int[] sumB) {
        java.util.List<int[]> cand = new java.util.ArrayList<>();
        for (int k = 0; k < BUCKETS; k++) {
            if (count[k] <= 0) {
                continue;
            }
            int r = sumR[k] / count[k];
            int g = sumG[k] / count[k];
            int b = sumB[k] / count[k];
            cand.add(new int[]{count[k], r, g, b});
        }
        if (cand.isEmpty()) {
            return null;
        }
        java.util.Collections.sort(cand, (a, b2) -> b2[0] - a[0]);
        int[] chosen = new int[COUNT];
        int n = 0;
        // 第一个：出现最多的颜色
        int[] first = cand.get(0);
        chosen[n++] = Color.rgb(first[1], first[2], first[3]);
        // 之后：每次挑「离已选颜色最远」的，同时要求距离 > MIN_DIST
        while (n < COUNT) {
            int bestIndex = -1;
            double bestScore = -1.0;
            for (int i = 0; i < cand.size() && i < 96; i++) {
                int[] c = cand.get(i);
                double minDist = Double.MAX_VALUE;
                for (int j = 0; j < n; j++) {
                    double d = dist(c[1], c[2], c[3], chosen[j]);
                    if (d < minDist) {
                        minDist = d;
                    }
                }
                if (minDist <= MIN_DIST) {
                    continue;
                }
                double score = minDist + Math.min(400.0, c[0]) * 0.05;
                if (score > bestScore) {
                    bestScore = score;
                    bestIndex = i;
                }
            }
            if (bestIndex < 0) {
                // 挑不满 5 个：从剩下的高频里补，凑够数量（不再要求距离）
                for (int i = 0; i < cand.size(); i++) {
                    int[] c = cand.get(i);
                    boolean used = false;
                    for (int j = 0; j < n; j++) {
                        if (chosen[j] == Color.rgb(c[1], c[2], c[3])) {
                            used = true;
                            break;
                        }
                    }
                    if (!used) {
                        chosen[n++] = Color.rgb(c[1], c[2], c[3]);
                        break;
                    }
                }
                if (n < COUNT && bestIndex < 0) {
                    int[] c = cand.get(n % cand.size());
                    chosen[n++] = Color.rgb(c[1], c[2], c[3]);
                }
                continue;
            }
            int[] c = cand.get(bestIndex);
            chosen[n++] = Color.rgb(c[1], c[2], c[3]);
        }
        return chosen;
    }

    private static double dist(int r1, int g1, int b1, int color) {
        double dr = r1 - Color.red(color);
        double dg = g1 - Color.green(color);
        double db = b1 - Color.blue(color);
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    /** 提饱和、压极端亮度：壁纸原色涂上去是「脏色」，光轮要的是「亮色」。 */
    private static int liven(int color) {
        float[] hsv = new float[3];
        Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), hsv);
        hsv[1] = Math.min(1.0f, hsv[1] * 1.35f + 0.10f);
        hsv[2] = Math.max(0.42f, Math.min(0.92f, hsv[2]));
        return Color.HSVToColor(hsv) | 0xFF000000;
    }
}
