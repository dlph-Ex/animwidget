package io.github.animwidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.ComponentName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v1.10.491：**多格组件**（1 个 host 里 6 个 ImageView）。
 *
 * <h3>为什么做它</h3>
 * 6 个独立组件 = 每秒 300 次 `updateAppWidget`；AOSP 每个 host 只保留一个 in-flight apply、
 * 新帧到达即 cancel，所以 6 路 50fps 的门槛是 `T_apply ≤ 1/(N·f) = 3.33ms`（三家外部复核一致）。
 * 把 6 路塞进**一个** host 后，apply 降到 50 次/秒，门槛回到 20ms。
 *
 * <p><b>代价明说</b>：纹理上传不省 —— 6 个 ImageView 就是 6 份纹理，720p 下 1.106GB/s，
 * 这是物理下限（外部一致）。我们省的是 apply 次数、RemoteViews 重建、和每帧的 Binder 往返。</p>
 *
 * <h3>怎么判定「这一路是多格」</h3>
 * 用系统事实 `AppWidgetManager.getAppWidgetInfo(id).provider`，不另存标记文件 ——
 * 标记会被备份恢复、清数据、以及我们自己的 bug 写乱，provider 不会。
 * 结果按 widgetId 缓存在内存里（provider 在一个 widget 生命周期内不会变）。
 */
final class MultiCell {

    /** 格数 = 布局里的 ImageView 个数（2 行 × 3 列）。 */
    static final int CELLS = 6;

    private static final java.util.HashMap<Integer, Boolean> MULTI = new java.util.HashMap<>();

    private MultiCell() {
    }

    /** 这一路是不是多格组件。判不出来按「不是」处理（回落单格，画面不会错）。 */
    static boolean isMulti(@Nullable Context context, int widgetId) {
        if (context == null) {
            return false;
        }
        synchronized (MULTI) {
            Boolean hit = MULTI.get(Integer.valueOf(widgetId));
            if (hit != null) {
                return hit.booleanValue();
            }
        }
        boolean multi = false;
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(context);
            android.appwidget.AppWidgetProviderInfo info = m.getAppWidgetInfo(widgetId);
            if (info != null && info.provider != null) {
                ComponentName self = new ComponentName(context, MultiCellProvider.class);
                multi = self.equals(info.provider);
            }
        } catch (Throwable ignored) {
        }
        synchronized (MULTI) {
            MULTI.put(Integer.valueOf(widgetId), Boolean.valueOf(multi));
        }
        return multi;
    }

    /**
     * v1.10.492：**两种 provider 的全部组件 id**（单格 + 多格）。
     *
     * <p>为什么必须合并：`PlaybackService` 的 tick 循环、孤儿缓存清理、repushAll 三处以前都只查
     * {@code getAppWidgetIds(WidgetProvider)}；多格组件是另一个 receiver，于是它对播放链**不存在** ——
     * 真机现象就是「加完多格组件，一次帧都不推」（日志里连 W23 都没有），而且它的缓存还会被当成
     * 孤儿删掉。</p>
     */
    @NotNull
    static int[] allIds(@Nullable Context context) {
        if (context == null) {
            return new int[0];
        }
        java.util.LinkedHashSet<Integer> set = new java.util.LinkedHashSet<>();
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(context);
            int[] a = m.getAppWidgetIds(new ComponentName(context, WidgetProvider.class));
            int[] b = m.getAppWidgetIds(new ComponentName(context, MultiCellProvider.class));
            if (a != null) {
                for (int id : a) {
                    set.add(Integer.valueOf(id));
                }
            }
            if (b != null) {
                for (int id : b) {
                    set.add(Integer.valueOf(id));
                }
            }
        } catch (Throwable ignored) {
        }
        int[] out = new int[set.size()];
        int i = 0;
        for (Integer id : set) {
            out[i++] = id.intValue();
        }
        return out;
    }

    /** 组件被删时忘掉缓存。 */
    static void forget(int widgetId) {
        synchronized (MULTI) {
            MULTI.remove(Integer.valueOf(widgetId));
        }
    }

    /**
     * 多格 RemoteViews：**同一张位图**设进 6 个格子。
     *
     * <p>现在做的是「6 格同源」—— 对「6 路同一个素材」来说与 6 个独立组件等价，
     * 但只花 1 次 apply。每格独立素材/相位是下一步（那会把解码拉回 6 倍，需要重新算账）。</p>
     */
    @NotNull
    static android.widget.RemoteViews views(@NotNull Context context, @Nullable android.graphics.Bitmap frame) {
        android.widget.RemoteViews v = new android.widget.RemoteViews(context.getPackageName(),
                R.layout.widget_multi);
        if (frame != null) {
            v.setImageViewBitmap(R.id.widget_cell0, frame);
            v.setImageViewBitmap(R.id.widget_cell1, frame);
            v.setImageViewBitmap(R.id.widget_cell2, frame);
            v.setImageViewBitmap(R.id.widget_cell3, frame);
            v.setImageViewBitmap(R.id.widget_cell4, frame);
            v.setImageViewBitmap(R.id.widget_cell5, frame);
        }
        return v;
    }
}
