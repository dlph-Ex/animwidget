package io.github.animwidget;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import org.jetbrains.annotations.NotNull;

/**
 * v1.10.463：预览虚线色的**唯一读写口**（档位 + 自定义色 + 壁纸主色采样）。
 *
 * <h3>为什么要有这个类</h3>
 * 462 把这三件事写成三处：档位在 {@code SettingsActivity} 里直接读 {@code settings} 的
 * {@code outline_mode}、自定义色又是一个 {@code static} 方法、壁纸采样在自己 {@code ConfigActivity}
 * 里先算一遍 —— 于是「语义色页显示的颜色」和「预览框真正画出来的颜色」没有一个共同的来源，
 * 迟早对不上（这正是 462 被否的第三条：底层有函数、界面上没有入口）。
 *
 * <p>现在只有一条链：{@link ThemePrefs}（存储）→ 本类（口径 + 壁纸主色）→
 * {@link OutlineColor#resolve}（唯一的合成函数）→ 两个调用方（语义色行的色块 / 预览框的虚线）。
 * 语义色页与配置页画出来的一定是同一个颜色，因为算它的是同一个函数。</p>
 *
 * <h3>壁纸主色从哪来</h3>
 * 48×48 缩略图 → {@link OutlineColor#dominant}（5 位/通道分桶，桶内取平均）。
 * 采样一次约 5ms，进程内缓存；{@code ConfigActivity} 每次自己采样完会顺手
 * {@link #noteWallpaperBg} 推过来 —— 于是用户换完壁纸先看配置页再回来，这里立刻是新的。
 */
final class OutlineSettings {

    /** 采样边长（与 {@code ConfigActivity.applyOutlineFromWallpaper} 完全一致）。 */
    static final int SAMPLE = 48;

    /** 「还没采过」的哨兵值（-1 表示采过但读不到壁纸，是有效结论，不能混用）。 */
    private static final int UNKNOWN = Integer.MIN_VALUE;
    private static final Object CACHE_LOCK = new Object();
    /** 壁纸主色（0xRRGGBB）；{@link #UNKNOWN} 表示还没采过，-1 表示采过但读不到。 */
    private static int cachedBg = UNKNOWN;

    private OutlineSettings() {
    }

    // ------------------------------------------------------------------ 档位

    /** 当前档位（0=跟随壁纸 / 1=默认 / 2=自定义，与 {@link OutlineColor.Mode} 同序）。 */
    static int modeIndex(@NotNull Context context) {
        return ThemePrefs.INSTANCE.getOutlineMode(context);
    }

    static void setModeIndex(@NotNull Context context, int index) {
        ThemePrefs.INSTANCE.setOutlineMode(context, index);
    }

    @NotNull
    static OutlineColor.Mode mode(@NotNull Context context) {
        OutlineColor.Mode[] all = OutlineColor.Mode.values();
        int i = modeIndex(context);
        return all[Math.max(0, Math.min(i, all.length - 1))];
    }

    /** 界面上的三档文字（顺序必须与 {@link OutlineColor.Mode} 一致）。 */
    @NotNull
    static String[] modeLabels() {
        String lang = java.util.Locale.getDefault().getLanguage();
        boolean zh = lang != null && lang.startsWith("zh");
        return new String[]{
                zh ? "跟随壁纸" : "Follow wallpaper",
                zh ? "默认" : "Default",
                zh ? "自定义" : "Custom"};
    }

    // ------------------------------------------------------------- 自定义色

    /** 自定义档的颜色（0xRRGGBB）；从没设过 → 中性灰（{@link OutlineColor#NEUTRAL_GRAY}）。 */
    static int customRgb(@NotNull Context context) {
        int raw = ThemePrefs.INSTANCE.getOutlineCustom(context);
        return raw == 0 ? OutlineColor.NEUTRAL_GRAY : (raw & 0xFFFFFF);
    }

    /** 用户到底设过没有（决定「切到自定义档」时要不要把取色器挪到他那次的颜色）。 */
    static boolean hasCustom(@NotNull Context context) {
        return ThemePrefs.INSTANCE.hasOutlineCustom(context);
    }

    static void setCustomRgb(@NotNull Context context, int rgb) {
        ThemePrefs.INSTANCE.setOutlineCustom(context, rgb);
    }

    // ------------------------------------------------------------- 壁纸主色

    /**
     * 壁纸主色（0xRRGGBB）；读不到壁纸 = -1。
     *
     * <p>只在第一次调用时采样一次，之后走进程内缓存。</p>
     */
    static int wallpaperBg(@NotNull Context context) {
        synchronized (CACHE_LOCK) {
            if (cachedBg != UNKNOWN) {
                return cachedBg;
            }
        }
        int value = sampleWallpaper(context);
        synchronized (CACHE_LOCK) {
            cachedBg = value;
        }
        return value;
    }

    /** 别人（配置页）自己采样完把结果推过来，省掉这里再解一次壁纸。 */
    static void noteWallpaperBg(int dominantRgb) {
        synchronized (CACHE_LOCK) {
            cachedBg = dominantRgb < 0 ? -1 : (dominantRgb & 0xFFFFFF);
        }
    }

    /** 当前生效的虚线色 —— **预览框真正画出来的那个颜色**。 */
    static int currentColor(@NotNull Context context) {
        return colorForMode(context, modeIndex(context));
    }

    /**
     * 某一档在当前壁纸下会画成什么颜色。
     *
     * <p>v1.10.464（用户）：非自定义档时，首页这一行的色块必须**和编辑页的预览块一模一样**。
     * 做法不是让两边各算一遍，而是都调这一个函数 —— 编辑页切档时也用它重画预览，
     * 于是「编辑页看到什么」和「存下去之后首页显示什么」在结构上就是同一个数。</p>
     */
    static int colorForMode(@NotNull Context context, int index) {
        OutlineColor.Mode[] all = OutlineColor.Mode.values();
        int i = Math.max(0, Math.min(index, all.length - 1));
        return OutlineColor.resolve(all[i], wallpaperBg(context), customRgb(context));
    }

    private static int sampleWallpaper(@NotNull Context context) {
        Bitmap thumb = null;
        try {
            WallpaperManager manager = WallpaperManager.getInstance(context);
            if (manager == null) {
                return -1;
            }
            Drawable drawable = manager.getDrawable();
            if (drawable == null) {
                return -1;
            }
            thumb = Bitmap.createBitmap(SAMPLE, SAMPLE, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(thumb);
            drawable.setBounds(0, 0, SAMPLE, SAMPLE);
            drawable.draw(canvas);
            int[] px = new int[SAMPLE * SAMPLE];
            thumb.getPixels(px, 0, SAMPLE, 0, 0, SAMPLE, SAMPLE);
            return OutlineColor.dominant(px);
        } catch (Throwable th) {
            return -1;
        } finally {
            if (thumb != null && !thumb.isRecycled()) {
                thumb.recycle();
            }
        }
    }
}
