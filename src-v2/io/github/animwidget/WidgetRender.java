package io.github.animwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.RemoteViews;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* compiled from: WidgetRender.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��<\n\u0002\u0018\u0002\n\u0002\u0010��\n\u0002\b\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0010\t\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0010\b\n��\n\u0002\u0018\u0002\n��\n\u0002\u0010\u0015\n��\bÆ\u0002\u0018��2\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J\u000e\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J0\u0010\u0007\u001a\u00020\b2\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000e2\b\u0010\u000f\u001a\u0004\u0018\u00010\u0010J\u001e\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u0013\u001a\u00020\u000e"}, d2 = {"Lio/github/animwidget/WidgetRender;", "", "()V", "contentIntent", "Landroid/app/PendingIntent;", "context", "Landroid/content/Context;", "render", "", "manager", "Landroid/appwidget/AppWidgetManager;", "config", "Lio/github/animwidget/WidgetConfig;", "index", "", "cache", "Lio/github/animwidget/FrameCache;", "widgetSize", "", "widgetId"})
/* loaded from: classes.jar:io/github/animwidget/WidgetRender.class */
public final class WidgetRender {

    @NotNull
    public static final WidgetRender INSTANCE = new WidgetRender();

    private WidgetRender() {
    }

    @Nullable
    private static volatile PendingIntent cachedIntent;
    /** v1.10.11：RemoteViews 原型 —— 每帧 `new RemoteViews(原型)` 只拷贝动作列表，
     *  不再每帧重新解析 widget_media.xml（原来这是推帧循环里最大的一笔开销）。 */
    @Nullable
    private static volatile RemoteViews prototype;
    /**
     * v1.10.108：**「原始比例」专用的原型**（fitCenter）。
     * 原始比例导出的是「素材比例」的帧，而桌面格子是另一个比例 ——
     * centerCrop 会把画面左右/上下裁掉，这正是「预览正常、桌面被裁」的原因。
     * 这一模式下改成完整显示（四周留透明，桌面壁纸透出），内容不再丢。
     */
    private static volatile RemoteViews prototypeFit;

    @NotNull
    public final PendingIntent contentIntent(@NotNull Context context) {
        PendingIntent cached = cachedIntent;
        if (cached != null) {
            return cached;
        }
        Intrinsics.checkNotNullParameter(context, "context");
        Intent intent = new Intent(context, (Class<?>) MainActivity.class).addFlags(335544320);
        Intrinsics.checkNotNullExpressionValue(intent, "addFlags(...)");
        PendingIntent activity = PendingIntent.getActivity(context, 0, intent, 201326592);
        Intrinsics.checkNotNullExpressionValue(activity, "getActivity(...)");
        cachedIntent = activity;
        return activity;
    }

    private static final java.util.HashMap<Integer, int[]> SIZE_CACHE = new java.util.HashMap<>();
    private static final java.util.HashMap<Integer, Long> SIZE_AT = new java.util.HashMap<>();
    /**
     * v1.10.252：尺寸缓存改成**有锁**。
     *
     * 原来只有主线程写（`invalidateSize` 由 onAppWidgetOptionsChanged 调），读在推帧线程；
     * 现在「切高清开关」会从后台线程重推一遍（{@link #repushAll}），两个线程同时写同一个
     * HashMap 是会真的把表写坏的（Java 8 HashMap 并发 rehash 会成环 → 该线程卡死不返回）。
     * 这把小锁只保护这两张表，持有时间都是微秒级，不会影响帧率。
     */
    private static final Object SIZE_LOCK = new Object();
    /**
     * v1.10.324：**launcher 还在报默认 110dp** 的组件（真尺寸还没送过来）。
     *
     * 实测（18:51:08 那一拍）：导出完成时 options 还是 110x110dp（= 130x115dp，加完 vivo padding），
     * 0.3 秒后同一个组件才报 182x191dp（= 662x643px）。按 110dp 算出来的画布比例 1.130、
     * 真格子 1.030 → 桌面 fitCenter 内接 → 用户看到"先是比本应的小一圈"。
     * 这里只在"真尺寸还没到"的第一次推帧时**跳过**（并延时补推），不做任何比例妥协。
     */
    private static final java.util.HashSet<Integer> BOX_DEFAULT = new java.util.HashSet<>();
    /**
     * v1.10.325：**跳过到什么时候**（widgetId → 截止时刻）。原来是"只跳一次"，被日志否掉了：
     * 19:29:04 那一秒里跳过一次之后，20ms 后的下一拍又按 110dp 推了 426x377 的错比例帧
     * （W28 画布=426x377 格子=426x377），小一圈照旧出现。
     * 现在改成"**只要 launcher 还报默认尺寸就一直跳**"，但有 5 秒硬上限：
     * 超时后按旧行为推（某些 launcher 可能永不回报真尺寸，不能让组件永久空白）。
     */
    private static final java.util.HashMap<Integer, Long> BOX_SKIP_UNTIL = new java.util.HashMap<>();
    /** v1.10.325：等真尺寸的硬上限（超时就按估算尺寸推，宁可小一圈也不留空白）。 */
    private static final long SKIP_BOX_MAX_MS = 5000L;
    /** v1.10.326：跳过期间复查 options 的间隔。 */
    private static final long BOX_RECHECK_MS = 200L;
    private static final java.util.HashMap<Integer, Long> BOX_RECHECK_AT = new java.util.HashMap<>();

    /**
     * v1.10.58：连续合成失败 → 冷却。
     * 「硬件位图」那次事故里，每帧都抛一次 IllegalArgumentException，
     * 5 秒往日志写了 2972 行崩溃栈，还一直白烧电。失败 3 次就暂停 3 秒再试，
     * 恢复后留一条 W16，既不会刷屏，也不会把组件永久停死。
     */
    private static final java.util.HashMap<Integer, Integer> FAIL_STREAK = new java.util.HashMap<>();
    /** v1.10.136：「预装帧跳过」每个「组件+帧数」只提示一次 —— warnThrottled 是全局单槽，会被别的码挤掉而反复刷。 */
    private static final java.util.HashSet<String> FLIP_SKIP_LOGGED = new java.util.HashSet<>();
    private static final java.util.HashMap<Integer, Long> FAIL_UNTIL = new java.util.HashMap<>();

    /**
     * v1.10.54：这个组件「推得动」的最高帧率（×10）。
     * 每帧位图 = 渲染尺寸 ×4 字节，过一次 Binder + 一次 launcher 纹理上传；
     * 预算 28MB/s —— 22 万像素的组件 ≈ 31fps，小组件不受限。
     */
    public final int pushFpsCapX10(@NotNull Context context, int widgetId) {
        // v1.10.119（致命 bug 修复）：URI / 预装帧模式下**位图不过 Binder**（只传一个 URI），
        // 28MB/s 的带宽预算在这里没有意义 —— 原来只在 PlaybackService 里一份**从未被调到**的
        // 私有方法里加了这层豁免，实际调用的是本方法，于是 30fps 仍被限到 18.7fps
        //（v1.10.118 日志：「每帧 1459KB × 30.0fps = 44MB/s → 已限到 18.7fps」）。
        if (WidgetUriPush.enabled(context) || WidgetFlip.enabled(context)) {
            return 600;
        }
        // v1.10.259：**这里也要豁免高清/1:1**。这个重载是配置页保存时调用的，
        // 原来只判了两个开关，于是档 1/2 的组件在保存那一刻仍按「22 万字节/帧」算预算，
        // 往 T16 里写一条「只能推 31.8fps」的假警告（导出早就不会被压帧了，这句纯误导）。
        try {
            WidgetConfig cfg = WidgetStore.INSTANCE.load(context, widgetId);
            if (cfg != null && needUri(context, cfg)) {
                return 600;
            }
        } catch (Throwable ignored) {
        }
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] size = widgetSize(context, manager, widgetId);
            long bytes = (long) size[0] * (long) size[1] * 4L;
            if (bytes <= 0L) {
                return 600;
            }
            int cap = (int) (28000000L * 10L / bytes);
            return Math.max(100, Math.min(cap, 600));
        } catch (Throwable th) {
            return 600;
        }
    }

    /** v1.10.192：画布无关帧的开关键（设置页「性能 → 画布无关帧」）。 */
    static final String PREF_NOCANVAS = "nocanvas_frames";

    /** v1.10.196：导出后是否提前烘帧（默认关）。 */
    static final String PREF_HEAT_ON_EXPORT = "heat_on_export";

    static boolean prefHeatOnExport(@NotNull Context context) {
        try {
            return context.getSharedPreferences("settings", 0)
                    .getBoolean(PREF_HEAT_ON_EXPORT, false);
        } catch (Throwable th) {
            return false;
        }
    }

    // ---------------------------------------------------------------- v1.10.251 静态图 1:1

    /**
     * v1.10.251：**静态图 1:1 高清**（默认关，见设置→性能）。
     *
     * 为什么需要：位图推送走 `setImageViewBitmap`，一张 ARGB 位图直接进 Binder，
     * 单次事务 ~1MB → 画布被压到 **22 万 px**。而桌面上一个 3×3 卡片在 3.28 密度下
     * 就是 1030×1441 = **148 万 px**，于是「396×554 的图被桌面放大 2.60×」——
     * 用户看到的「2.5K 素材一点都不清楚」就是这么来的（日志 W22 的「桌面放大」）。
     *
     * 静态图（帧数=1）只有**一次**推送，没有逐帧成本，所以可以
     * ① 画布直接按「卡片」真实像素烘（面积预算 220 万 px ≈ 8.8MB）；
     * ② 用 URI 推送（文件交给桌面自己读），Binder 里只有一个字符串 → 不再受 1MB 限制。
     * 桌面把这张图按 1:1 画在卡片上，`W22` 的「桌面放大」应当变成 **1.00×**。
     */
    static final String PREF_HIRES_STATIC = "hires_static";

    /** 1:1 高清的画布面积预算（px）。220 万 ≈ 1260×1762 的整屏，够覆盖任何单张卡片。 */
    static final int HIRES_AREA = 2200000;
    /** v1.10.331：50fps 下 WebP 通道能持续供帧的画布上限（实测 38.9 万→59fps、90 万→28fps）。 */
    static final int WEBP_50FPS_CAP = 452000;

    /**
     * v1.10.341：**桌面（launcher）每秒能解多少像素** —— 这是"画面一大就变 PPT"的真正天花板。
     *
     * 实测（23:27:36~23:28:00，同一台机器、同一份素材，只有画布大小不同）：
     *   · 3 个组件全是 662x643（42.6 万）：`渲染=453/3.0s`、每个组件 `取用≈145~152`（100%，桌面全取）、
     *     用户看到的是流畅；
     *   · 换成 2 个 662x643 + 1 个 879x869（76.4 万）：那一个的 `取用=127~146`（85~97%），
     *     用户看到的是"卡得跟 PPT 似的"。
     * 换成每秒像素：63.9M px/s 时桌面吃得下；80.8M px/s 时吃不下。
     * 所以桌面解码预算 ≈ **64M px/s**（取"吃得下"的那一档做上限，不赌中间值）。
     *
     * 结论（必须写进产品）：`50fps × 组件数` 固定时，**每个组件能用的像素 = 64M ÷ (fps × 组件数)**：
     *   1 个组件 → 128 万（任何卡片都够）；2 个 → 64 万；3 个 → 42.6 万（正好 3×3）。
     * 这就是用户实测"3×3 以下不卡、比 3×3 大就 PPT"的算术来源 —— 不是我们的推帧问题
     * （推帧 150 次/秒、唤醒迟到 0ms、推最慢 1~5ms 全部达标）。
     */
    static final int LAUNCHER_PX_PER_SEC = 64000000;
    /** 再小的卡片也留个下限，别让组件数多时把画布压成马赛克。 */
    static final int LAUNCHER_SHARE_FLOOR = 200000;

    /**
     * v1.10.350：**走共享位图时画布的上限**（45 万 px）。
     *
     * 用户硬要求："拖动组件大小后，画面 1s 内接上继续播放（从 PPT 转回流畅）"。
     * 实测（05:35，879x643 = 56.5 万）：首帧 612ms ✓ 达标，但全量 290 帧烘了 **9371ms**、
     * 速率只有 30 帧/秒 < 播放 50 帧/秒 —— 也就是说烘帧永远追不上播放头，
     * 在这 9.4 秒里一直是顶帧（=用户看到的 PPT），**烘完才顺畅**。
     *
     * 共享位图解决的是"桌面解码"，没有解决"我们自己烘帧"（≈27M px/s）。
     * 要让烘帧持续跑赢播放：`27M ÷ (50 × 1.2) ≈ 45 万`。
     * 代价必须说清：**5×5 的格子（76.4 万）只能按 45 万出画面**（上采样 1.3×），
     * 换来的是"1 秒接上"；76.4 万 + 50fps + 1 秒接上，这台机器上三者不可兼得。
     */
    // v1.10.351：SHARED_CANVAS_CAP(45 万) 已撤销 —— 过渡期的帧源改成「现渲染」之后，
    // 「烘速 ≥ 播速」不再是流畅的前提，压画布换 T_swap=0 的取舍没有意义了。
    // 留着这个常量只会让人以为 1:1 还被夹着，所以删掉。

    static boolean hiResStaticPref(@NotNull Context context) {
        try {
            return context.getSharedPreferences("settings", 0)
                    .getBoolean(PREF_HIRES_STATIC, false);
        } catch (Throwable th) {
            return false;
        }
    }

    static void setHiResStatic(@NotNull Context context, boolean on) {
        try {
            context.getSharedPreferences("settings", 0).edit()
                    .putBoolean(PREF_HIRES_STATIC, on).apply();
        } catch (Throwable ignored) {
        }
        // 画布口径变了 → 尺寸缓存与「画布口径已记」都要作废，否则新尺寸不会立刻生效
        synchronized (SIZE_LOCK) {
            SIZE_CACHE.clear();
            SIZE_AT.clear();
        }
        synchronized (CANVAS_SEEN) {
            CANVAS_SEEN.clear();
        }
    }

    // ------------------------------------------------ v1.10.254 动图分辨率（三档）

    /**
     * 动图/视频的分辨率档位（0/1/2）。静态图不走这里（走编辑页那个「高清 1:1」勾选）。
     *
     * 为什么需要这一档：**动图之前根本没有 1:1 这条路径**。
     *  · 位图推送（默认）：Binder 1MB → 画布封顶 22 万 px（396×554），桌面放大 2.6×；
     *  · URI 推送开着：长边封 1080 → 773×1080，放大 1.33×；
     *  · 再往上就没有开关了 —— 这次补上。
     *
     * 三档的代价不同，所以让用户自己选（默认仍是 0，不改变既有行为）：
     *  · 0 标准：22 万 px / 位图。兼容性最好，帧率最稳。
     *  · 1 高清：画布**仍与格子无关**（长边 min(素材,1440)、面积 ≤ 130 万），走 URI 推送。
     *           拖动尺寸不重烘（只改清晰度），预热帧不会被拖拽作废。
     *  · 2 1:1 ：按**卡片真实像素**烘（面积 ≤ 220 万），走 URI 推送。桌面 1.00× 不再放大；
     *           代价是拖动尺寸会重烘（动画要把整批帧重烘一遍）。
     */
    static final String PREF_ANIM_QUALITY = "anim_quality";

    /** 档 1 的面积上限（px）：1440 长边 × 0.715 比例 ≈ 148 万，留一点余量会超，所以 130 万够常见卡片。 */
    static final int ANIM_AREA_HD = 1300000;

    /** v1.10.260：每个组件自己一份（`"196=1"`），没单独设过就回落全局默认。 */
    static final String PREF_ANIM_IDS = "anim_quality_ids";

    static int animQuality(@NotNull Context context) {
        try {
            int v = context.getSharedPreferences("settings", 0).getInt(PREF_ANIM_QUALITY, 0);
            return v < 0 ? 0 : (v > 2 ? 2 : v);
        } catch (Throwable th) {
            return 0;
        }
    }

    /** 某个组件的档位；没有单独设过 → 全局默认。 */
    static int animQuality(@NotNull Context context, @NotNull WidgetConfig config) {
        return animQualityWidget(context, config.getWidgetId(), animQuality(context));
    }

    static int animQualityWidget(@NotNull Context context, int widgetId, int fallback) {
        try {
            java.util.Set<String> set = context.getSharedPreferences("settings", 0)
                    .getStringSet(PREF_ANIM_IDS, null);
            if (set == null) {
                return fallback;
            }
            String prefix = widgetId + "=";
            for (String s : set) {
                if (s == null || !s.startsWith(prefix)) {
                    continue;
                }
                try {
                    int v = Integer.parseInt(s.substring(prefix.length()));
                    return v < 0 ? 0 : (v > 2 ? 2 : v);
                } catch (NumberFormatException ignored) {
                    return fallback;
                }
            }
            return fallback;
        } catch (Throwable th) {
            return fallback;
        }
    }

    /** 写一个组件自己的档位；**不存在的组件写进去也不会有人读**，没有副作用。 */
    static void setAnimQualityWidget(@NotNull Context context, int widgetId, int level) {
        int v = level < 0 ? 0 : (level > 2 ? 2 : level);
        try {
            android.content.SharedPreferences sp = context.getSharedPreferences("settings", 0);
            java.util.Set<String> old = sp.getStringSet(PREF_ANIM_IDS, null);
            java.util.HashSet<String> set = new java.util.HashSet<>();
            if (old != null) {
                set.addAll(old);
            }
            String prefix = widgetId + "=";
            java.util.Iterator<String> it = set.iterator();
            while (it.hasNext()) {
                String s = it.next();
                if (s != null && s.startsWith(prefix)) {
                    it.remove();
                }
            }
            set.add(prefix + v);
            sp.edit().putStringSet(PREF_ANIM_IDS, set).apply();
            synchronized (SIZE_LOCK) {
                SIZE_CACHE.remove(Integer.valueOf(widgetId));
                SIZE_AT.remove(Integer.valueOf(widgetId));
            }
            synchronized (CANVAS_SEEN) {
                CANVAS_SEEN.remove(Integer.valueOf(widgetId));
            }
        } catch (Throwable ignored) {
        }
    }

    static void setAnimQuality(@NotNull Context context, int level) {
        try {
            context.getSharedPreferences("settings", 0).edit()
                    .putInt(PREF_ANIM_QUALITY, level < 0 ? 0 : (level > 2 ? 2 : level)).apply();
        } catch (Throwable ignored) {
        }
        synchronized (SIZE_LOCK) {
            SIZE_CACHE.clear();
            SIZE_AT.clear();
        }
        synchronized (CANVAS_SEEN) {
            CANVAS_SEEN.clear();
        }
    }

    /**
     * 这个组件这一刻**需不需要**走 URI 推送（文件给桌面自己读）。
     *
     * 三个来源：全局 URI 开关、静态图 1:1 高清、动图档位 ≥1（后两个都绕不开 1MB）。
     */
    static boolean needUri(@NotNull Context context, @NotNull WidgetConfig config) {
        if (WidgetUriPush.enabled(context)) {
            return true;
        }
        if (animQuality(context, config) >= 1) {
            return true;
        }
        return hiResStatic(context, config);
    }

    /**
     * v1.10.252：**每个组件自己一份**（`设置` 里那个开关只是「新组件的默认值」）。
     *
     * 存成 StringSet（`"188=1"`），不动 WidgetConfig 的构造签名 —— 那是个 26 参数的 data class，
     * 加字段要改所有构造点，风险远大于收益。
     */
    static final String PREF_HIRES_IDS = "hires_widgets";

    /** 读某个组件的勾选；没有单独设过就回落 `fallback`（= 全局默认）。 */
    static boolean hiResWidget(@NotNull Context context, int widgetId, boolean fallback) {
        try {
            java.util.Set<String> set = context.getSharedPreferences("settings", 0)
                    .getStringSet(PREF_HIRES_IDS, null);
            if (set == null) {
                return fallback;
            }
            String prefix = widgetId + "=";
            for (String s : set) {
                if (s != null && s.startsWith(prefix)) {
                    return "1".equals(s.substring(prefix.length()));
                }
            }
            return fallback;
        } catch (Throwable th) {
            return fallback;
        }
    }

    static void setHiResWidget(@NotNull Context context, int widgetId, boolean on) {
        try {
            android.content.SharedPreferences sp = context.getSharedPreferences("settings", 0);
            java.util.Set<String> old = sp.getStringSet(PREF_HIRES_IDS, null);
            java.util.HashSet<String> set = new java.util.HashSet<>();
            if (old != null) {
                set.addAll(old);
            }
            String prefix = widgetId + "=";
            java.util.Iterator<String> it = set.iterator();
            while (it.hasNext()) {
                String s = it.next();
                if (s != null && s.startsWith(prefix)) {
                    it.remove();
                }
            }
            set.add(prefix + (on ? "1" : "0"));
            sp.edit().putStringSet(PREF_HIRES_IDS, set).apply();
            // 画布口径变了 → 这个组件的尺寸缓存/「画布已记」必须作废，否则下一次还是老画布
            synchronized (SIZE_LOCK) {
                SIZE_CACHE.remove(Integer.valueOf(widgetId));
                SIZE_AT.remove(Integer.valueOf(widgetId));
            }
            synchronized (CANVAS_SEEN) {
                CANVAS_SEEN.remove(Integer.valueOf(widgetId));
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * v1.10.261：**预热烘完 → 把这个组件重推一次**。
     *
     * 为什么必须有：v1.10.257 为了拖动不掉帧，做了「画布还没烘好就先推旧画布的邻帧（顶帧）」。
     * 对**动图**没问题（下一拍还会再渲）；但**静态图一辈子只推一次**，于是：
     *   拖大 → 渲染时 1030×1538 还没烘好 → 推了旧的 419×626
     *   → 预热烘完 → **没有任何一拍会再推** → 桌面永远停在 419×626 被放大 2.46×。
     * 实测就是这么糊的（23:32:14 画布=1030x1538，但推上去的仍是 419×626）。
     *
     * 所以：预热一收尾就主动重推一次。此时 `READY == stamp`（pending=false），
     * 走的是 cachedUri 命中那条路（2ms），不会触发二次烘。
     */
    public final void repushOne(@NotNull Context context, int widgetId, @NotNull String why) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            WidgetConfig cfg = WidgetStore.INSTANCE.load(context, widgetId);
            if (cfg == null) {
                return;
            }
            invalidateSize(widgetId);
            WidgetUriPush.forgetLast(widgetId);
            render(context, manager, cfg, PlaybackService.lastIndex(widgetId), null);
            Diag.warn(context, "W32", "重推单个组件 组件=" + widgetId + " 原因=" + why);
        } catch (Throwable th) {
            Diag.warn(context, "W32", "重推单个组件失败 组件=" + widgetId + " "
                    + th.getClass().getSimpleName() + " 原因=" + why);
        }
    }

    /**
     * v1.10.252：**把「开关/勾选」立刻落到桌面上**。
     *
     * 用户反馈：「这东西是不是开启/关掉后拖动桌面组件时才生效？」—— 是的，之前就是这样。
     * 原因：画布只在**推帧那一刻**才算（`render` → `canvasFor`），而静态图一辈子只推一次；
     * 开关只改了「下次算画布时用什么尺寸」，没有触发任何推送，桌面自然不会变，
     * 直到用户去拖一下组件（`onAppWidgetOptionsChanged` → 重推）。
     *
     * 这里主动把所有已放置的组件重推一遍（走的就是拖拽时那条 `render` 路径，语义完全一致）。
     */
    public final void repushAll(@NotNull Context context, @NotNull String why) {
        int idsLen = 0;
        int done = 0;
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = MultiCell.allIds(context);   // v1.10.492：单格 + 多格
            if (ids != null) {
                idsLen = ids.length;
                for (int id : ids) {
                    try {
                        WidgetConfig cfg = WidgetStore.INSTANCE.load(context, id);
                        if (cfg == null) {
                            continue;
                        }
                        invalidateSize(id);
                        // 画布固定时帧的 URI 不会变，必须清掉「同一张不重推」的去重记录
                        WidgetUriPush.forgetLast(id);
                        render(context, manager, cfg, PlaybackService.lastIndex(id), null);
                        done++;
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable th) {
            Diag.warn(context, "W32", "重推全部组件失败: " + th.getClass().getSimpleName() + " 原因=" + why);
            return;
        }
        Diag.warn(context, "W32", "重推全部组件 原因=" + why + " 组件数=" + idsLen + " 已推=" + done);
    }

    /**
     * 这一刻能不能走 1:1 高清：**单帧 + 图片 + （该组件的勾选，或全局默认）**。
     *
     * 动图不走：每帧 8.8MB 的合成 + 编码会直接把 30fps 打下来，
     * 那不是清晰度问题而是「只能看幻灯片」，所以高清只给静态图。
     */
    static boolean hiResStatic(@NotNull Context context, @NotNull WidgetConfig config) {
        if (!hiResWidget(context, config.getWidgetId(), hiResStaticPref(context))) {
            return false;
        }
        if (config.getFrameCount() > 1) {
            return false;
        }
        return config.getKind() == MediaKind.IMAGE;
    }

    /**
     * v1.10.253：**最近一次真正用的传输通道**。
     *
     * 原来 `W23` 的「传输=」是 `transportLabel()` 按开关**猜**出来的（只看 URI 开关 / 预装帧开关）。
     * 静态图 1:1 高清是「借 URI 这条路、但不开 URI 开关」，于是日志里写成「传输=位图」——
     * 明明推的是 content:// URI。这条假证据会让人以后排查时朝错的方向走，所以改成**记录事实**。
     */
    private static volatile String LAST_TRANSPORT = "";
    /**
     * v1.10.346：**按组件记传输通道**。
     * 以前只有全局一个字符串，而 W10 是逐组件打印的 —— 三个组件一个走 URI、一个走共享位图时，
     * 每个组件那一行写的都是"最后一次"的通道，读不出来是谁在用什么。
     * 用户说"第 3 个帧率明显不对"时，我连它走的哪条路都看不到，就是被这个坑的。
     */
    private static final java.util.HashMap<Integer, String> TRANSPORT =
            new java.util.HashMap<>();

    @NotNull
    static String recentTransport() {
        return LAST_TRANSPORT;
    }

    @NotNull
    static String recentTransport(int widgetId) {
        synchronized (TRANSPORT) {
            String s = TRANSPORT.get(Integer.valueOf(widgetId));
            return s != null ? s : LAST_TRANSPORT;
        }
    }

    static void noteTransport(@NotNull String how) {
        LAST_TRANSPORT = how;
    }

    static void noteTransport(int widgetId, @NotNull String how) {
        LAST_TRANSPORT = how;
        synchronized (TRANSPORT) {
            TRANSPORT.put(Integer.valueOf(widgetId), how);
        }
    }

    /** 开关，默认开。 */
    static boolean noCanvasPref(@NotNull Context context) {
        try {
            return context.getSharedPreferences("settings", 0).getBoolean(PREF_NOCANVAS, true);
        } catch (Throwable th) {
            return true;
        }
    }

    /**
     * v1.10.192：**帧画布** —— 只由「素材比例 + 长边」决定，**与桌面格子无关**。
     *
     * 这是「拖动尺寸零重烘」的地基：画布一旦与格子无关，拖动就只是让桌面自己做
     * fitCenter（ImageView 的 scaleType 在 XML 里固定），不会产生任何新文件。
     *
     * 长边 = min(素材长边, 1080)：1080 覆盖了满宽组件（屏宽 1080px），
     * 再大只是白占体积；再用 URI 面积上限 90 万 px 兜一次（方形素材会撞）。
     *
     * @return null = 素材尺寸未知 → 调用方走旧逻辑
     */
    @Nullable
    static int[] frameCanvas(@NotNull WidgetConfig config) {
        return frameCanvas(config, 1080, 900000L);
    }

    @Nullable
    static int[] frameCanvas(@NotNull WidgetConfig config, int maxEdge, long maxArea) {
        return frameCanvas(config, maxEdge, maxArea, config.getLayout());
    }

    @Nullable
    static int[] frameCanvas(@NotNull WidgetConfig config, int maxEdge, long maxArea,
                             @NotNull LayoutMode eff) {
        int cap = Math.max(240, maxEdge);
        long areaCap = Math.max(100000L, maxArea);
        int fw = config.getFrameWidth();
        int fh = config.getFrameHeight();
        if (fw <= 1 || fh <= 1) {
            return null;
        }
        // v1.10.292（用户绝对要求："拖动组件大小 1 秒内适配，画面必须完整，不得有例外的缩放/截断"）：
        // **原始比例恢复「画布与格子无关」**。画布只由素材比例 + 长边上限决定（1080），
        // 导出后烘一次就固定；之后拖动尺寸只改桌面的 fitCenter，**零重烘、零延迟**。
        //
        // v1.10.279 曾把这里对 FIT 短路（要求画布跟着格子同形），代价是**每换一档尺寸就整盘重烘**
        // （420 帧 × 180ms ≈ 76 秒），拖动时必然掉帧、比例乱、显示不全 —— 就是用户这十几版的抱怨。
        // 方向反了：要"快"就必须让画布与格子无关，要"完整"就必须 fitCenter，两者本来不冲突。
        // v1.10.193：画布的**比例**必须与 `FrameComposer.contentRect()` 给 `area` 的比例一致，
        // 才能保证「蒙版/边框/阴影/圆角半径」在缩放后逐像素不变：
        //   · 原始比例 → 素材比例
        //   · 1:1 / 16:9 / 4:3 / 9:16 / 21:9 → 布局比例（area 的比例就是它）
        //   · 填充 → area 比例 = 格子比例，无法与格子解耦 → 返回 null（回落旧通道）
        float wantRatio;
        if (eff == LayoutMode.FIT) {
            wantRatio = fw / (float) fh;
        } else if (eff.getRatio() > 0.0f) {
            wantRatio = eff.getRatio();
        } else {
            return null;
        }
        int longEdge = Math.max(fw, fh);
        int L = Math.max(96, Math.min(longEdge, cap));
        {
            int w0;
            int h0;
            if (wantRatio >= 1.0f) {
                w0 = L;
                h0 = Math.max(96, Math.round(L / wantRatio));
            } else {
                h0 = L;
                w0 = Math.max(96, Math.round(L * wantRatio));
            }
            long area0 = (long) w0 * h0;
            if (area0 > areaCap) {
                double k = Math.sqrt(areaCap / (double) area0);
                w0 = Math.max(96, (int) Math.round(w0 * k));
                h0 = Math.max(96, (int) Math.round(h0 * k));
            }
            return new int[]{w0, h0};
        }
    }


    /**
     * v1.10.193：能不能走「画布无关帧」通道。
     *
     * 关键前提已经由 {@link #frameCanvas} 保证：**帧画布的比例 = contentRect 给 area 的比例**
     * （原始比例→素材比例；1:1 / 16:9 … →布局比例）。两边比例一致，所以所有「按 min(w,h)
     * 取百分比」的量（圆角半径、描边宽度、阴影 margin）在缩放后数值完全不变 → 逐像素等价。
     * 于是蒙版、边框、阴影、星形也全都不用限制。
     *
     * 唯一排除的是**填充**：它的 area 比例 = 格子比例，画布没法与格子解耦（frameCanvas 返回 null）。
     */
    static boolean noCanvasOk(@NotNull Context context, @NotNull WidgetConfig config) {
        if (!WidgetUriPush.enabled(context) && animQuality(context, config) < 1) {
            return false;
        }
        if (!noCanvasPref(context)) {
            return false;
        }
        return frameCanvas(context, config) != null;
    }

    /**
     * v1.10.278：**「原始比例」到底铺不铺满 —— 全项目唯一判据**。
     *
     * 前 3 版（1.10.270~277）把这件事拆成两半：①合成层按 FIT 画一张比格子小的卡片，
     * ②再赌桌面 ImageView 的 scaleType（fitCenter 留壁纸 / centerCrop 裁一圈）。
     * 于是同一个问题有两个"作者"，日志里永远自相矛盾，用户三次说「还是截断」。
     *
     * 现在只留一个**事实**：素材比例与格子比例差超过 10% 时，「原始比例」按**铺满**处理
     * （卡=格子、素材多出的边在卡片内部裁掉，形状保持完整——这正是「填充」布局的做法）。
     * 判据、画布、合成、预热四处共用本方法，桌面 scaleType 恒为 centerCrop 且比例恒等。
     */
    /** 这一拍**真正**要用的布局（合成、画布、预热、日志四处共用）。 */
    @NotNull
    static LayoutMode effLayout(@NotNull WidgetConfig config, @Nullable int[] box) {
        return config.getLayout();
    }

    /** 按当前动图档位取的帧画布（档 0：1080/90 万；档 1：1440/130 万）。 */
    @Nullable
    static int[] frameCanvas(@NotNull Context context, @NotNull WidgetConfig config) {
        return frameCanvas(context, config, config.getLayout());
    }

    @Nullable
    static int[] frameCanvas(@NotNull Context context, @NotNull WidgetConfig config,
                             @NotNull LayoutMode eff) {
        if (animQuality(context, config) >= 1) {
            return frameCanvas(config, 1440, ANIM_AREA_HD, eff);
        }
        return frameCanvas(config, 1080, 900000L, eff);
    }

    /** 这一拍该用哪块画布（帧画布 or 旧的车格画布）。 */
    @NotNull
    /**
     * v1.10.275：**桌面视图用 fitCenter 还是 centerCrop —— 唯一判据**（渲染与日志共用）。
     *
     * 之前这段判断写在 `render()` 里两次（URI 路径的 `fitNow`、位图路径的 `fitView`），
     * 而且两者公式不同；日志里又各自描述一遍 —— 于是"日志说留壁纸、实际在裁"这种自相矛盾
     * 根本查不出来。现在只有这一处。
     *
     * 规则：
     *   · 填充（FILL）  → centerCrop（铺满是设计，裁切是预期的）；
     *   · 原始比例（FIT）→ fitCenter；
     *   · 其它比例（16:9 / 4:3 / 1:1 …）→ **画布比例和格子比例不一致时也 fitCenter**。
     *     否则 centerCrop 必然把画布边缘裁掉（用户："调整组件大小，依旧会出现截断"）——
     *     实测 1:1 的组件放在 1030x1079 的格子里，centerCrop 会把上下各裁 2.3%，
     *     圆角形状被切掉一条边，看起来就是"截断"。
     */
    public final boolean fitViewFor(@NotNull WidgetConfig config, @Nullable int[] size, @Nullable int[] box) {
        // v1.10.298（A 方案，用户："A方案，走起吧"）：**scaleType 只看布局，与尺寸无关**。
        //
        //   · 填充(FILL) → centerCrop：铺满是它的语义，裁切是预期的；
        //   · 其它（原始比例 / 1:1 / 16:9 / 9:16 / 4:3 …）→ fitCenter：桌面**等比例内接**，
        //     数学上不可能裁到画面 —— 不管格子估成 2.6 还是 2.7，也不管 vivo 报不报真实尺寸。
        //
        // 为什么这是唯一解：之前判据是"画布比例 vs 格子比例"，只要两者差 0.3%，centerCrop 就把
        // 差异翻译成"裁一整条"（实测 1095x416 的格子里画面被切掉一条）。判据一旦和尺寸无关，
        // 这个错误就再也无法发生 —— 我们不需要知道真实格子像素。
        //
        // 副产品：scaleType 不变 ⇒ 拖动时 RemoteViews 一个字节都不用改 ⇒ 桌面自己重新 measure
        // 就适配完，0ms、不产生新画布、不重烘、不重推（"1684ms / 一次拖动烧 15 秒"一起消失）。
        return config.getLayout() != LayoutMode.FILL;
    }

    /**
     * v1.10.283：**让位图的 intrinsic 一定不小于格子**（A 方案下仍保留，防桌面按 intrinsic 摆）。
     *
     * 桌面用 AT_MOST 量 RemoteViews → ImageView.onMeasure 取 min(drawable.intrinsic, specSize)，
     * intrinsic = 位图像素 × density/160（实测 3.28）。画布不够大时视图就比格子小一圈（贴左上、露壁纸）。
     * 这条只依赖"下限"，对桌面少报尺寸是自愈的：多出来的部分 centerCrop/fitCenter 会缩回去。
     */
    static int[] coverFloor(@NotNull Context context, @NotNull int[] canvas, @NotNull int[] box) {
        try {
            float d = context.getResources().getDisplayMetrics().density;
            if (d <= 0.0f || canvas[0] <= 0 || canvas[1] <= 0 || box[0] <= 0 || box[1] <= 0) {
                return canvas;
            }
            float need = 1.30f;
            // v1.10.299（用户："导出后画面先比本应状态小一圈，再扩展回正常"）：
            // 保存那一刻桌面给的还是 provider 默认尺寸（110dp），画布按它算就偏小 →
            // 桌面 AT_MOST 取 min(intrinsic, 格子)，视图先被量成"比格子小一圈"，
            // 真尺寸到了再长回来 —— 就是你看到的那个短暂缩小。
            // 默认尺寸不可信时，按 420dp 兜底：位图 intrinsic 一定盖得住真实格子，
            // 第一拍就是满的（多出来的像素由 fitCenter 缩回去，画面不变）。
            float dw = box[0] / d;
            float dh = box[1] / d;
            if (dw <= 112.0f && dh <= 112.0f) {
                dw = 420.0f;
                dh = 420.0f;
            }
            int minW = Math.round(dw * need) + 24;
            int minH = Math.round(dh * need) + 24;
            float k = Math.max(minW / (float) canvas[0], minH / (float) canvas[1]);
            if (k <= 1.02f) {
                return canvas;
            }
            int w = Math.max(96, Math.round(canvas[0] * k));
            int h = Math.max(96, Math.round(canvas[1] * k));
            long area = (long) w * h;
            long cap = 320000L;
            if (area > cap) {
                double s2 = Math.sqrt(cap / (double) area);
                w = Math.max(96, (int) Math.round(w * s2));
                h = Math.max(96, (int) Math.round(h * s2));
            }
            return new int[]{w, h};
        } catch (Throwable th) {
            return canvas;
        }
    }

    static int[] canvasFor(@NotNull Context context, @NotNull WidgetConfig config, @NotNull int[] box) {
        // v1.10.278：这一拍**真正**要用的布局（原始比例但比例差 >10% → 填充）。
        // 画布、合成、预热全用它，别再让桌面去"解释"比例。
        LayoutMode eff = effLayout(config, box);
        int[] c = canvasFor0(context, config, box, eff);
        return coverFloor(context, c, box);
    }

    private static int[] canvasFor0(@NotNull Context context, @NotNull WidgetConfig config,
                                    @NotNull int[] box, @NotNull LayoutMode eff) {
        // v1.10.251：静态图 1:1 高清优先 —— 单帧、没有逐帧成本，所以直接按「卡片」真实像素烘，
        // 桌面拿到 1:1 的图就不再放大（代价：拖动尺寸会重烘，静态图重烘一次是毫秒级）。
        // v1.10.291：**高帧率素材的画布预算要压到"烘得比播得快"**（上一版只压了 else 分支，
        // 而实测这台机器走的是「动图 1:1」档 → HIRES_AREA(220 万) → 画布 813x1079=88 万，
        // 预热只有 21~26fps，而播放要 50fps → 烘速永远追不上 → 命中率恒为 0%：
        //   W13 预热完成 完成=420 用时=15887ms 速率=26帧/秒 画布=597x853
        //   W10 组件 238 实测 48.6fps 命中=0%(0/146) 跳过=146次
        // 现在所有档位共用同一个预算出口。
        int lvl = animQuality(context, config);
        int budget = budgetFor(context, config, lvl, box, eff);
        // v1.10.397：**画布与格子彻底解耦（除「填充」外，所有类别、所有档位）**。
        //
        // 现场（06:41:49~57 的录屏日志）：拖动组件大小 → 格子 662x643 → 662x869 → 879x869 →
        // 1018x807，每一档都换画布 → 每次整批重烘 603 帧（26~62 帧/秒 × 8 核 85% CPU），
        // 而画布比例 != 新格子比例 → 桌面 fitCenter 把旧画布「按比例缩进去」——
        // 用户看到的就是「零点几秒内画面缩小 → 迅速正常」，紧接着是预热与推帧抢 CPU 的抖动。
        //
        // 现在的规则只有一条：**画布只由「素材比例 + 像素预算」决定，与格子无关**。
        //   · 静态图 → 长边 1440 / 220 万（单帧，给足像素，不跟格子）；
        //   · 动图   → 档位预算（标准 1080/90 万、高清 1440/130 万）；
        //   · 比例   → 原始比例=素材比例，固定比例=该布局比例（contentRect 的 area 比例）。
        // 收益：拖动只改桌面自己的 fitCenter，**零重烘、零延迟、零比例跳变**。
        // 代价：格子比画布大时由桌面放大（等比，不会裁切）—— 这是「连贯」换「逐像素」的取舍，
        // 也是用户这次明确要的东西。
        //
        // 「填充」是唯一例外：它的 area = 格子本身（画面必须铺满格子、多出的边要裁掉），
        // 画布没法与格子解耦；但它的 centerCrop 让拖动天然连续（旧画布也总是铺满新格子）。
        // v1.10.399：这条「画布无关帧」通道重新由**开关**决定（v1.10.397 曾无视开关一律走它，
        // 于是「逐像素等于设定参数」这个诉求在界面上失去了控制入口）。
        //
        //   · 开关 = 开（默认）：画布只由「素材比例 + 像素预算」决定 → 拖动零重烘、
        //     桌面等比缩放（格子比画布大时会放大，不是逐像素）；
        //   · 开关 = 关：画布严格按**格子像素**（1:1 档上限 220 万）→ 逐像素等于设定，
        //     代价是每次拖动尺寸都要整批重烘。
        //
        // 两者不可能同时成立：逐像素要求画布随格子变，连续要求画布不随格子变。
        // 所以把它做成开关，而不是替用户选。
        if (eff != LayoutMode.FILL && noCanvasOk(context, config)) {
            // v1.10.404（用户决议「对齐 1920」）：长边上限 1440 → **1920**。
            // 之前导出上限是 1920（动图/视频）、画布上限却是 1440 —— 1920 的素材烘完只剩 1440，
            // 「清晰度 1920」到不了桌面。现在两边同顶：1920×1080 = 207 万 ≤ 220 万，放得下；
            // 4:3 的 1920×1440（276 万）会按面积等比夹到 ~1740×1305（仍是缩小，不放大、不编像素）。
            // v1.10.406（C 家方案）：**长边上限按帧率分档**。
            //
            // 1920×1080@50fps 需要 104M px/s 的烘焙吞吐，本机只有 ~26M px/s（6 线程）——
            // 实测（10:30 日志）现渲染 60~104ms/帧、撞预热拍=100%、烘焙从头到尾没完成。
            // 所以像素必须给帧率让路：
            //   · fps ≥ 45 → 长边 1280（1280×720 = 92 万，q93 下烘焙 ≈53fps，跟得上 50fps）；
            //   · fps ≥ 30 → 长边 1920（1080p 原像素，30fps 下吞吐够）；
            //   · 其它     → 1920（素材本身更小时由 frameCanvas 自己封顶）。
            // 仍然是「不超素材、不跟格子、体积不变」——只是上限由帧率决定。
            int edgeCap = config.getFps() >= 45 ? 1280 : 1920;
            int[] fc = (hiResStatic(context, config) || lvl >= 2)
                    ? frameCanvas(config, edgeCap, HIRES_AREA, eff)
                    : frameCanvas(context, config, eff);
            if (fc != null) {
                // v1.10.402（用户决议，2026-11-07）：**画布永远等于素材原生像素**。
                //
                //   · 不许上采样到格子（不编造像素）；
                //   · 也不许为了"贴合格子"而烘一块更小的画布 —— 素材的清晰度与体积**恒定**，
                //     与格子大小、拖动过程完全无关。
                //
                // 于是这一趟只由「素材 + 档位上限」决定：拖动不改画布、不重烘、几何不跳；
                // 桌面自己 fitCenter 缩放（缩小=清晰，放大只发生在格子 > 素材时，属物理极限）。
                // v1.10.401 那套「按格子取阶梯」正是被这条决议否掉的（小格子会烘更小的画布
                // → 体积变化），已撤掉。
                return fc;
            }
        }
        // v1.10.290：**高帧率素材的画布预算要压到"烘得比播得快"**。
        //
        // 实测（18:07，420 帧 / 50fps / 画布 380x853=32 万像素）：
        //   W13 预热完成 完成=420 用时=8907ms （≈21ms/帧墙钟 × 6 线程 → 单帧线程 127ms）
        //   W13 预热开始 … 质量=97 线程=6        → 聚合烘速 47fps
        //   W10 组件 237 实测 49.3fps 命中=0%(1/148) 顶帧=82 跳过=65
        // 烘速 47 < 播放 50 → **永远追不上**，命中率恒为 0，桌面全程在放"顶帧/跳过"，
        // 用户看到的既是"刷新率不理想"，也是"一帧内缩后恢复"（顶的是别的画布/别的比例）。
        //
        // 唯一不需要动画质的杠杆：把像素数降下来。fps≥45 时预算 90 万 → 24 万
        // （380x853 → 327x734，面积 -26%，烘速 ≈62fps > 50fps，留出余量）。
        return renderCanvas(context, config, box, budget, eff);
    }

    /**
     * v1.10.291：这一拍允许烘多大（**唯一出口**）。
     *
     * 依据：预热吞吐 ≈ 6 线程 / 单帧线程耗时，而单帧耗时 ∝ 像素数。
     * 实测 597x853(51 万) → 26fps、813x853(69 万) → 21fps、380x853(32 万) → 47fps。
     * 播放 50fps 意味着**烘速必须 >50fps**，否则命中率恒 0（桌面全程放顶帧/跳过）。
     * 按实测线性外推：≤24 万像素才能到 ~50fps 量级，所以 fps≥45 的素材一律压到 24 万。
     * 代价是画面被放大（813x1079 的格子 → 490x650 画布，放大 1.66×），
     * 这是"流畅"与"清晰"之间必须做的取舍，由 fps 触发、不偷偷改画质参数。
     */
    static int budgetFor(@NotNull Context context, @NotNull WidgetConfig config, int lvl) {
        return budgetFor(context, config, lvl, null, null);
    }

    /**
     * v1.10.292：预算按「编码真实成本」算，而不是按画布面积一刀切。
     *
     * 实测（同一台机器上的参照：87.7 万像素 WebP q97 ≈ 180ms，JPEG q95 ≈ 5ms）：
     *   · 帧完全不透明（视频、铺满、无圆角/阴影）→ 走 JPEG，成本可忽略 → 放开到 1:1（HIRES_AREA）；
     *   · 带 alpha（letterbox 透明边、蒙版角、阴影）→ 仍是 WebP，成本 ≈ **画面内容面积**
     *     （实测 42% 内容 ≈ 54% 耗时），所以 letterbox 越宽，画布反而可以越大。
     * JPEG 是否可用「先乐观、后修正」：没见过 alpha 帧且预测无 letterbox → 乐观放开；
     * 一旦预热里见到带 alpha 的帧（粘性），下一拍预算自动退回 WebP 口径，不会来回抖。
     */
    static int budgetFor(@NotNull Context context, @NotNull WidgetConfig config, int lvl,
                         @Nullable int[] box, @Nullable LayoutMode eff) {
        // v1.10.321（B 自校准）：先按老口径算出"这台机器理论上该烘多大"，再交给 BakeCal
        // 乘上实测系数并夹到档位阶梯上。两件事都在 BakeCal 里，这里只做一个例外：
        // HIRES_AREA（全不透明帧走 JPEG 的 1:1 档）**不参与校准** —— 那条路的编码成本
        // 实测 ≈5ms/帧，拿预热吞吐去压它等于白掉清晰度。
        int base = baseBudgetFor(context, config, lvl, box, eff);
        if (base >= HIRES_AREA) {
            return base;
        }
        // v1.10.333（用户：1:1 就得是 1:1）：**动图 1:1 档退出自校准与阶梯**。
        // 判据在 BakeLadder.calibrated（纯函数，门禁 [14] 逐点对拍），理由写在那里：
        // 系数 1.0 时阶梯也只给 39 万（格子 42.6 万），而一旦撞上导出/并发预热，
        // 实测烘速会被拖到 30 帧/秒以下 → 比例控制一步砸到 0.55 → 画布 23 万、
        // 桌面放大 1.36 倍，且回升门槛（>目标 1.05 倍 ≈ 60.9 帧/秒）永远够不到。
        // 稳态实测又证明这条降档换不来帧率（命中 100% 时仍是 37~41fps，瓶颈在拍）。
        if (!BakeLadder.calibrated(lvl, config.getFrameCount())) {
            return base;
        }
        return BakeCal.budget(context, config.getWidgetId(), base);
    }

    /**
     * v1.10.341：这个组件允许用的画布像素 = 桌面解码预算 ÷ (fps × 同时播放的组件数)。
     * 组件数由 PlaybackService 每拍上报（它才知道现在有几个在推帧）。
     */
    static int launcherSharePx(@NotNull WidgetConfig config) {
        int fps = Math.max(config.getFps(), 1);
        int n = Math.max(PlaybackService.animPlayingCount(), 1);
        long share = (long) LAUNCHER_PX_PER_SEC / ((long) fps * (long) n);
        if (share < LAUNCHER_SHARE_FLOOR) {
            share = LAUNCHER_SHARE_FLOOR;
        }
        return (int) Math.min(share, Integer.MAX_VALUE);
    }

    /** v1.10.321：老口径（v1.10.292 的实测模型），只产"理论预算"，不含自校准与阶梯。 */
    private static int baseBudgetFor(@NotNull Context context, @NotNull WidgetConfig config, int lvl,
                                     @Nullable int[] box, @Nullable LayoutMode eff) {
        if (config.getFps() >= 45 && config.getFrameCount() > 1) {
            // v1.10.330：**用户在配置页选的「动图分辨率」必须真的生效**。
            // 原来这条高帧率分支直接落到 `return 240000`，把 lvl 完全忽略 —— 选了「1:1」
            // 也还是 24 万画布（在 3×3 的 42.6 万格子上等于 1.3 倍放大、发虚）。
            // 这是典型的"配置≠行为"：开关在，代码根本没读。
            // 现在：lvl=2（1:1）→ 不超过格子；lvl=1（高清）→ 同样受格子封顶；
            //       lvl=0（默认「标准」）→ 维持原来的 24 万保守口径，老用户行为不变。
            // 自校准在预算出口继续生效：选了 1:1 但机器烘不及，会自动往下降档。
            int cellArea = (box != null && box[0] > 0 && box[1] > 0) ? box[0] * box[1] : 0;
            if (lvl >= 1 && cellArea > 0) {
                // v1.10.331：**"1:1"不等于"无限大"**。实测（20:57:51~20:58:06）：
                // 把 307 拖到 879x1095 时，lvl=2 直接给了 90 万画布 →
                // 编码 183~189ms/帧、预热 28 帧/秒（要 57）、命中 0%、顶帧 133 次、实测掉到 43fps。
                // 所以 WebP 通道必须有自己的吞吐上限：50fps 下能持续供帧的量级 ≈45 万 px
                // （实测 38.9 万 → 59fps ✓，90 万 → 28fps ✗）。只有 JPEG 通道才允许冲 220 万。
                boolean jpegOk = box != null && eff != null
                        && WidgetUriPush.alphaState(context, config, box[0], box[1]) >= 0;
                // v1.10.332（用户："改了后还是 1:1 吗？"）：**1:1 档不该被常数砍**。
                // 这是用户显式选的"我要清晰"，上限只保留 HIRES_AREA；
                // 撑不撑得住交给自校准（331 已改成按比例一轮收敛）去测 → 降档由**实测**决定，
                // 不由我在这里拍一个 45.2 万的常数。这样 4×5 的大格子也会先尝试真 1:1，
                // 烘不动时下一批就自动退到能维持 50fps 的面积。
                // 「高清」档不是 1:1 的承诺，仍保留 45.2 万上限（避免误伤流畅度）。
                if (lvl >= 2) {
                    // v1.10.341：1:1 仍然"按格子像素"，但**不许超过桌面每秒能解的像素预算**。
                    // 依据见 LAUNCHER_PX_PER_SEC 的注释：3 组件 × 50fps 时每个只能用 ≈42.6 万，
                    // 超了桌面就画不出来（用户看到的"PPT"就是这个）。
                    // 1 个组件时预算是 128 万，实际卡片都到不了，等于不生效。
                    // v1.10.344：大格子（>45 万）走共享位图、桌面零解码 → **不再受桌面解码预算限制**。
                    // 否则 341 的预算会把画布夹到 42.6 万，而 343 又要画布 >45 万才启用共享位图，
                    // 两个判据互相抵消（这正是"导出后正常、过一会儿又卡"的切换点）。
                    if (WidgetUriPush.sharedForCanvas(config, box,
                            Math.min(HIRES_AREA, cellArea))) {
                        // v1.10.351：**撤销 v1.10.350 的 45 万画布上限**。
                        // 350 那个上限的前提是「流畅必须等烘帧」—— 烘速 ≥ 播速才能接上，
                        // 于是把画布压到 27M ÷ 50fps ≈ 54 万以下（取 45 万）换 T_swap = 0。
                        // 现在过渡期的帧源改成「现渲染」（见 WidgetUriPush.pushRendered）：
                        // 流畅不再依赖烘速，烘帧只决定「什么时候卸掉现渲染那 2.6 核」，与观感无关。
                        // 所以画布回到格子原生 —— 否则会出现**倒挂**：过渡期是 76.4 万原生
                        // （现渲染按格子合成），烘完却切到 45 万（放大 1.3 倍），
                        // 用户看到的就是「清了一下又糊回去」。
                        return Math.min(HIRES_AREA, cellArea);
                    }
                    int share = launcherSharePx(config);
                    return Math.min(Math.min(HIRES_AREA, cellArea), share);
                }
                int cap = jpegOk ? HIRES_AREA : WEBP_50FPS_CAP;
                return Math.min(Math.min(HIRES_AREA, cellArea), cap);
            }
            if (config.getFrameCount() > 40 && box != null && eff != null
                    && box[0] > 0 && box[1] > 0 && config.getFrameWidth() > 0 && config.getFrameHeight() > 0) {
                float rb = box[0] / (float) box[1];
                float rs = config.getFrameWidth() / (float) config.getFrameHeight();
                float f;                                     // 画面内容占画布面积的比例
                if (eff == LayoutMode.FILL) {
                    f = 1.0f;
                } else if (eff == LayoutMode.FIT) {
                    f = Math.min(rb / rs, rs / rb);
                } else {
                    f = 0.6f;                                // 固定比例蒙版：保守
                }
                boolean predictedOpaque = f > 0.98f;          // 无 letterbox（FILL 或比例吻合的 FIT）
                if (predictedOpaque
                        && WidgetUriPush.alphaState(context, config, box[0], box[1]) >= 0) {
                    return HIRES_AREA;                       // 全不透明 → JPEG → 1:1
                }
                // WebP 口径：成本 ≈ 0.15 + 0.85·内容占比（实测）；24 万是「内容」面积预算
                float g = 0.15f + 0.85f * Math.max(0.05f, Math.min(1.0f, f));
                return Math.min(900000, Math.round(240000f / g));
            }
            return 240000;
        }
        if (hiResStatic(context, config) || lvl >= 2) {
            return HIRES_AREA;
        }
        return lvl >= 1 ? 900000 : 220000;
    }

    /** W28：画布口径变了才打一条（拖动时不再刷屏）。 */
    static void diagCanvas(@NotNull Context context, @NotNull WidgetConfig config,
                           @NotNull int[] canvas, @NotNull int[] box) {
        try {
            String tag = canvas[0] + "x" + canvas[1];
            synchronized (CANVAS_SEEN) {
                if (tag.equals(CANVAS_SEEN.get(Integer.valueOf(config.getWidgetId())))) {
                    return;
                }
                CANVAS_SEEN.put(Integer.valueOf(config.getWidgetId()), tag);
            }
            // v1.10.341：画布被"桌面解码预算"夹小时必须写清楚，否则读者会以为 1:1 又被打折了
            String capped = "";
            try {
                long cellArea = (long) box[0] * (long) box[1];
                long canvArea = (long) canvas[0] * (long) canvas[1];
                if (animQuality(context, config) >= 2 && cellArea > 0L && canvArea < cellArea * 0.98) {
                    if (WidgetUriPush.sharedForCanvas(config, box, canvArea)) {
                        // v1.10.351：**撤销 350 的 45 万上限**。过渡期改由「现渲染直推」供帧，
                        // 「烘速 ≥ 播速」不再是流畅的前提。画布小于格子的唯一原因只剩
                        // HIRES_AREA(220 万) 这个天花板，所以这里如实写，不再冒充「烘帧速度」。
                        capped = "（1:1 按格子原生，但撞上 220 万 px 天花板：" + (HIRES_AREA / 10000)
                                + " 万 px；v1.10.351 起画布不再受烘帧速度夹取）";
                    } else {
                        capped = "（1:1 被桌面解码预算夹小：" + PlaybackService.animPlayingCount() + " 个组件 × "
                                + config.getFps() + "fps → 每个只够 "
                                + (launcherSharePx(config) / 10000) + " 万 px；"
                                + "桌面每秒只能解约 " + (LAUNCHER_PX_PER_SEC / 1000000) + "M 像素）";
                    }
                }
            } catch (Throwable ignored) {
            }
            // v1.10.428（用户要的第二条）：**画布真的变了就自动重烘这一代**。
            // 以前只在 W28 打一行日志 —— 如果某次因为桌面没报尺寸、按默认格子烘了一代，
            // 真尺寸到了也只会"记一笔"，不会重做，桌面就一直停在错的画布上（放大/版式错）。
            // 这里补上自愈：ensure 会按新画布算新指纹 → 旧帧不算热 → 自动重烘。
            try {
                WidgetPrewarm.ensure(context, config, canvas[0], canvas[1]);
            } catch (Throwable ignored) {
            }
            Diag.warn(context, "W28", "帧画布 组件=" + config.getWidgetId()
                    + " 画布=" + canvas[0] + "x" + canvas[1]
                    + " 格子=" + box[0] + "x" + box[1]
                    + capped
                    + " 布局=" + config.getLayout().name()
                    + " 素材=" + config.getFrameWidth() + "x" + config.getFrameHeight()
                    + (hiResStatic(context, config)
                    ? "（静态图：画布按素材/长边上限烘，与格子无关）"
                    : animQuality(context, config) >= 2
                    // v1.10.333：1:1 档的画布现在**就是**格子像素（不乘系数、不夹阶梯），
                    // 原来那句"按素材长边上限烘、与格子无关、拖动不重烘"从 330 起就不成立了。
                    ? "（动画/视频 1:1：画布=素材像素（长边按帧率：50fps→1280 / 30fps→1920，面积 220 万），与格子无关）"
                    : animQuality(context, config) >= 1
                    ? "（动图高清：长边 1440 / 面积 130 万，仍与格子无关；URI 推送）"
                    : noCanvasOk(context, config)
                    ? "（画布无关帧：拖尺寸不重烘）" : "（旧通道：随格子重烘）"));
        } catch (Throwable ignored) {
        }
    }

    static final java.util.HashMap<Integer, String> CANVAS_SEEN = new java.util.HashMap<>();

    /**
     * v1.10.60：这个组件当前实际推送的画布尺寸（诊断用；找不到配置就退回组件框）。
     */
    @NotNull
    public final int[] canvasSize(@NotNull Context context, int widgetId) {
        try {
            WidgetConfig config = WidgetStore.INSTANCE.load(context, widgetId);
            int[] box = widgetSize(context, AppWidgetManager.getInstance(context), widgetId);
            if (config == null) {
                return box;
            }
            return canvasFor(context, config, box);
        } catch (Throwable ignored) {
            return new int[]{0, 0};
        }
    }

    /**
     * v1.10.60：按**真正推上去的画布**算预算。
     * 画布已经按素材原尺寸封顶（canvasForSource），再拿组件框算就等于白扣一半额度 ——
     * 280px 的 GIF 明明每帧只推 397KB，却被按 879KB 限到 31.8fps。
     */
    public final int pushFpsCapX10(@NotNull Context context, @NotNull AppWidgetManager manager,
                                   @NotNull WidgetConfig config) {
        // v1.10.119：同上 —— 这是 PlaybackService 真正调用的那个重载，豁免必须写在这里。
        if (WidgetUriPush.enabled(context) || WidgetFlip.enabled(context)) {
            return 600;
        }
        // v1.10.253：静态图 1:1 高清走 URI 推（不经 Binder），按 22 万字节算预算会得出
        // 假的「只能推 31.8fps」，还会往 T16 里写一条误导人的限流提示。
        if (needUri(context, config)) {
            return 600;
        }
        try {
            int[] box = widgetSize(context, manager, config.getWidgetId());
            int[] size = renderCanvas(context, config, box);
            long bytes = (long) size[0] * (long) size[1] * 4L;
            if (bytes <= 0L) {
                return 600;
            }
            int cap = (int) (28000000L * 10L / bytes);
            return Math.max(100, Math.min(cap, 600));
        } catch (Throwable th) {
            return 600;
        }
    }

    /**
     * v1.10.56：这个组件在桌面上真正会用的宽高比。
     *
     * 编辑页拿它当预览框比例 —— 必须和 {@link #widgetSize} 的推导**完全同一套**
     * （dp × density、再按 22 万像素等比封顶），否则「预览里是这个形状、贴到桌面变成另一个形状」。
     * 老实现是 `minWidth / minHeight` 两个 int 相除（3 格宽 2 格高 = 340/220 → 1，而不是 1.55），
     * 预览框被压成正方形，桌面上却是 1.55:1 —— 用户看到的就是「导出后画面比例变了」。
     */
    public final float aspect(@NotNull Context context, int widgetId) {
        try {
            int[] size = widgetSize(context, AppWidgetManager.getInstance(context), widgetId);
            if (size[0] > 0 && size[1] > 0) {
                return size[0] / (float) size[1];
            }
        } catch (Throwable ignored) {
        }
        return 1.7777778f;
    }

    /**
     * v1.10.57：尺寸缓存作废。系统说「尺寸变了」就必须立刻重算，
     * 否则 3 秒内还在用旧尺寸推帧 —— 静态图片组件更糟：它一辈子只推一次，
     * 会一直停在那张「按旧尺寸合成、被 launcher 放大」的图上。
     */
    public final void invalidateSize(int widgetId) {
        synchronized (SIZE_LOCK) {
            SIZE_AT.remove(Integer.valueOf(widgetId));
            SIZE_CACHE.remove(Integer.valueOf(widgetId));
        }
    }

    @NotNull
    /** v1.10.426：上一次量到的可信格子（跨重启保留）。 */
    private static final String PREF_GRID = "grid_cache";

    private static void saveGrid(@NotNull Context context, int widgetId, @NotNull int[] box) {
        try {
            if (box[0] <= 0 || box[1] <= 0) {
                return;
            }
            context.getSharedPreferences(PREF_GRID, 0).edit()
                    .putString(String.valueOf(widgetId), box[0] + "x" + box[1]).apply();
        } catch (Throwable ignored) {
        }
    }

    /**
     * v1.10.446（用户："有漏洞不补，漏着干什么"）：**把导出这一刻的格子冻结下来**。
     *
     * <p>真正的漏不在画布公式，而在"格子从哪来"：导出烘帧和运行时 render 都走
     * {@link #widgetSize}，而它在"桌面只给默认 110dp"的机型（vivo `com.bbk.launcher2` 实测）
     * 会退回 {@link #loadGrid} 缓存 —— 缓存是空的，于是每次兜底算出来的值可能不同，
     * 画布跟着变，回桌面自然重烘。
     *
     * <p>导出时把这一刻的格子写进缓存：之后 {@link #widgetSize} 在"不可信"分支读到的
     * 就是同一组数字，两边锁死，**同一份素材导完就不会再烘第二次**。
     * （真上报尺寸的机型走"可信"分支，本来就用实时值，这一写不影响它们。）
     */
    public final void freezeBox(@NotNull Context context, int widgetId, int w, int h) {
        saveGrid(context, widgetId, new int[]{w, h});
    }

    /**
     * v1.10.446：按**运行时同款**口径量一次格子并冻结（内部自己取 AppWidgetManager）。
     *
     * <p>放在这里而不是调用方：`widgetSize()` 返回 int[]，跨类在 try 块里接这个数组
     * 会让 ASM 校验器算不出类型（[4] 报 `expected [I, but found Object`），
     * 所以数组只在同类的栈上走，调用方拿不到数组。
     */
    public final void freezeBoxFromRuntime(@NotNull Context context, int widgetId) {
        try {
            int[] box = widgetSize(context, android.appwidget.AppWidgetManager.getInstance(context), widgetId);
            if (box[0] > 0 && box[1] > 0) {
                saveGrid(context, widgetId, box);
            }
        } catch (Throwable ignored) {
        }
    }

    /** @return 上次量到的格子；没量过返回 null。 */
    @Nullable
    public final int[] loadGrid(@NotNull Context context, int widgetId) {
        try {
            String v = context.getSharedPreferences(PREF_GRID, 0)
                    .getString(String.valueOf(widgetId), null);
            if (v == null) {
                return null;
            }
            int x = v.indexOf('x');
            if (x <= 0) {
                return null;
            }
            int w = Integer.parseInt(v.substring(0, x));
            int h = Integer.parseInt(v.substring(x + 1));
            return (w > 0 && h > 0) ? new int[]{w, h} : null;
        } catch (Throwable th) {
            return null;
        }
    }

    public final int[] widgetSize(@NotNull Context context, @NotNull AppWidgetManager manager, int widgetId) {
        // v1.10.15：尺寸 3 秒内复用 —— 原来每帧都 getAppWidgetOptions（一次 binder），
        // 在推帧循环里是纯浪费；尺寸本来也只有拖拽改变时才会变。
        long now = SystemClock.elapsedRealtime();
        synchronized (SIZE_LOCK) {
            Long at = SIZE_AT.get(Integer.valueOf(widgetId));
            int[] cached = SIZE_CACHE.get(Integer.valueOf(widgetId));
            if (cached != null && at != null && now - at.longValue() < 3000L) {
                return cached;
            }
        }
        int[] fresh = computeWidgetSize(context, manager, widgetId);
        // v1.10.326：**尺寸不可信时不进缓存**。否则 3 秒内所有调用都拿到"110dp 默认值"，
        // 真尺寸到了也没人重算 —— 跳过窗口会一直拖到超时（外部分析 §7.3 指出的正是这个坑）。
        boolean unreliable;
        synchronized (BOX_DEFAULT) {
            unreliable = BOX_DEFAULT.contains(Integer.valueOf(widgetId));
        }
        if (!unreliable) {
            synchronized (SIZE_LOCK) {
                SIZE_CACHE.put(Integer.valueOf(widgetId), fresh);
                SIZE_AT.put(Integer.valueOf(widgetId), Long.valueOf(now));
            }
            // v1.10.426（用户："格子在一个设备上测一次，不就完全不用测了吗"）：
            // 格子只由「组件在桌面的位置/跨度 + 桌面格数 + 密度」决定，**不会自己变**，
            // 只有用户拖动/换桌面布局才会变（那时会收到 W22 尺寸变更）。
            // 所以可信值落一次盘，下次导出/预热直接用，不必再等桌面报。
            saveGrid(context, widgetId, fresh);
        } else {
            int[] saved = loadGrid(context, widgetId);
            if (saved != null) {
                return saved;   // 拿不到可信值 → 用上次量到的（比 110dp 默认值靠谱得多）
            }
        }
        return fresh;
    }

    @NotNull
    private final int[] computeWidgetSize(@NotNull Context context, @NotNull AppWidgetManager manager, int widgetId) {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(manager, "manager");
        Bundle options = manager.getAppWidgetOptions(widgetId);
        float density = context.getResources().getDisplayMetrics().density;
        // v1.10.57：**尺寸必须和 launcher 真正给的那块区域同比例**，否则 RemoteViews 里的
        // ImageView（scaleType=fitCenter）会把整张位图等比缩到能放下 —— 用户看到的就是
        // 「导出完好好的，等几秒画面缩小了一圈、四周多出一圈壁纸」。
        // 老实现只读 appWidgetMinWidth/MinHeight（这是**竖屏**的下界），横屏时拿反了：
        // 官方语义是「竖屏 = minWidth × maxHeight，横屏 = maxWidth × minHeight」。
        // 先用 dp 推算出一份「保底尺寸」（老系统 / launcher 没给真实尺寸时只能用这个）
        int minW = options.getInt("appWidgetMinWidth", 0);
        int maxW = options.getInt("appWidgetMaxWidth", 0);
        int minH = options.getInt("appWidgetMinHeight", 0);
        int maxH = options.getInt("appWidgetMaxHeight", 0);
        boolean portrait = context.getResources().getConfiguration().orientation
                != android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        int widthDp = portrait ? minW : maxW;
        int heightDp = portrait ? maxH : minH;
        boolean defaultLike = (minW <= 110 && minH <= 110) || (minW <= 0 && minH <= 0);
        if (widthDp <= 0 || heightDp <= 0) {
            widthDp = minW > 0 ? minW : 110;
            heightDp = minH > 0 ? minH : 110;
            defaultLike = true;
        }
        // v1.10.324：真尺寸还没送来（全是 110dp）→ 打上标记，让 render 这一拍别按它烘画布
        synchronized (BOX_DEFAULT) {
            if (defaultLike) {
                BOX_DEFAULT.add(Integer.valueOf(widgetId));
            } else {
                BOX_DEFAULT.remove(Integer.valueOf(widgetId));
            }
        }
        // v1.10.293（用户："组件拖到差不多是原素材画面比例时，画面明显出现截断"）：
        // **vivo 报的 min/max dp 不含它自己的内边距**，而且横竖补得不一样：
        //   日志报 314x329dp，实测白框 1097x1096px = 334x334dp → 差 +20dp 宽 / +5dp 高。
        // 只补尺寸不补比例等于白补：画布比例 0.955 vs 真实格子 1.000 → 桌面 centerCrop
        // 每条边裁 ~2.3%，而"画面刚好填满画布"的那一刻正是最容易被看出截断的时候。
        // 现在把 padding 加回 dp 再换算，画布比例 = 真实格子比例，centerCrop 变恒等。
        final int LCH_PAD_W_DP = 20;
        final int LCH_PAD_H_DP = 5;
        widthDp += LCH_PAD_W_DP;
        heightDp += LCH_PAD_H_DP;
        int rawWidth = (int) (widthDp * density);
        int rawHeight = (int) (heightDp * density);
        String from = (portrait ? "竖屏 " : "横屏 ") + widthDp + "x" + heightDp + "dp×"
                + String.format(java.util.Locale.US, "%.2f", density);
        int[] exact = exactSizePx(options);
        if (exact == null) {
            // v1.10.112：日志里说清楚「为什么只能用 dp 估算」，
            // 免得看到「竖屏 182x191dp×3.28」还得猜是不是被判定挡掉了。
            from = from + "（launcher 未提供 OPTION_APPWIDGET_SIZES，按 dp 估算）";
        }
        if (exact != null) {
            // 这个字段名义上是 px，但不少 OEM 往里塞的是 dp（塞 dp 时会明显小于 dp×density 的估计）。
            // 只在「和估计值同一个量级」时才采信，否则宁可继续用 dp 估计 ——
            // 采信一个 dp 值当 px，组件会被放大约 3 倍、糊得没法看。
            float kx = exact[0] / (float) Math.max(rawWidth, 1);
            float ky = exact[1] / (float) Math.max(rawHeight, 1);
            // v1.10.111（反馈）：0.6~1.8 太窄，vivo 桌面给的 sizes 一直被判不采信，
            // 于是尺寸退回 dp 估计（日志里长期是 110~314dp）。放宽到 0.35~2.8：
            // 真实 px 与「dp×density」差 3 倍以上才会被拒（那才是把 dp 当 px 的坏 ROM）。
            boolean plausible = kx > 0.35f && kx < 2.8f && ky > 0.35f && ky < 2.8f;
            if (plausible) {
                rawWidth = exact[0];
                rawHeight = exact[1];
                from = "launcher sizes=" + exact[0] + "x" + exact[1] + "px"
                        + "（dp 估计 " + widthDp + "x" + heightDp + "dp）";
            } else {
                from = from + "；sizes=" + exact[0] + "x" + exact[1]
                        + " 与估计差太多（×" + String.format(java.util.Locale.US, "%.2f", kx)
                        + "/" + String.format(java.util.Locale.US, "%.2f", ky) + "），不采信";
            }
        }
        // v1.10.111（反馈）：原来上限写死 1440，3.28 密度的长组件（606dp≈1988px）会被夹到 1440，
        // 比例从 0.52 变成 0.71 —— 帧比例一歪，桌面就越看越像「被裁」。
        // 这里只兜下限；真正的体积保护在下面的面积封顶（等比缩放，不改比例）。
        int width = RangesKt.coerceIn(rawWidth, 64, 4096);
        int height = RangesKt.coerceIn(rawHeight, 64, 4096);
        // v1.6.1：RemoteViews 把 Bitmap 塞进 Parcel，Binder 单次事务只有 ~1MB，
        // 大组件（比如 4×4 在 3x 屏上 750×750）ARGB 就是 2.2MB，updateAppWidget
        // 会抛 TransactionTooLargeException；上面 catch(Throwable) 一吞，桌面就
        // 只剩 initialLayout —— 表现为「组件加上了但是空白」。这里按面积封顶。
        // v1.10.128：**面积封顶挪到 renderCanvas 里、按「卡片」算**。
        // 这里只保留 dp→px 与 64~4096 的合理性边界；格子本身不再被面积压小，
        // 否则大组件会莫名其妙少掉 4~11%（1030 → 990 → 上采样发糊）。
        int rawW = width;
        int rawH = height;
        // v1.10.26（需求 5）：桌面画质证据 —— 尺寸来源、换算 px、封顶后实际渲染 px。
        // 渲染 px 比格子 px 小多少，就是那部分「被系统放大」的像素。
        Diag.warnThrottled(context, "F11", "小部件 尺寸来源=" + from
                + " → " + rawW + "x" + rawH + "px → 格子=" + width + "x" + height + "px"
                + " 比例=" + String.format(java.util.Locale.US, "%.3f", width / (float) Math.max(height, 1))
                + "（面积上限按「卡片」算：URI 90 万 / 位图 22 万 / 静态图 1:1 与动图 1:1 220 万 / 动图高清 130 万；真正推的尺寸见 W22/预热 的 画布=）");
        return new int[]{width, height};
    }

    /**
     * v1.10.57：API 31+ launcher 会直接给「真实像素尺寸」（OPTION_APPWIDGET_SIZES）。
     * 有这个就不再用 dp × density 估 —— 估出来的比例只要差一点，
     * ImageView 的 fitCenter 就会把整张位图缩小一圈。
     * @return null = launcher 没给（老系统/没实现），调用方退回 dp 推算
     */
    @Nullable
    private static int[] exactSizePx(@NotNull Bundle options) {
        try {
            if (android.os.Build.VERSION.SDK_INT < 31) {
                return null;
            }
            java.util.ArrayList<android.util.SizeF> list =
                    options.getParcelableArrayList(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_SIZES);
            if (list == null || list.isEmpty()) {
                return null;
            }
            for (android.util.SizeF size : list) {
                if (size == null) {
                    continue;
                }
                int w = Math.round(size.getWidth());
                int h = Math.round(size.getHeight());
                if (w > 0 && h > 0) {
                    return new int[]{w, h};
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** v1.10.58：统一记一次失败 —— 前 3 次带堆栈，之后只计数，第 3 次起冷却 3 秒。 */
    private static void noteFailure(@NotNull Context context, int widgetId, @NotNull Throwable th) {
        try {
            Integer id = Integer.valueOf(widgetId);
            Integer old = FAIL_STREAK.get(id);
            int streak = old != null ? old.intValue() + 1 : 1;
            FAIL_STREAK.put(id, Integer.valueOf(streak));
            Diag.warnThrottled(context, "W02", "小部件帧合成失败: " + th.getClass().getSimpleName()
                    + (th.getMessage() != null ? "/" + th.getMessage() : "")
                    + " 连续=" + streak);
            if (streak <= 3) {
                CrashLog.save(context, Thread.currentThread(), th);
            }
            if (streak >= 3) {
                FAIL_UNTIL.put(id, Long.valueOf(SystemClock.elapsedRealtime() + 3000L));
                Diag.warn(context, "W16", "组件 " + widgetId + " 连续失败 " + streak
                        + " 次（" + th.getClass().getSimpleName()
                        + "），暂停推帧 3 秒再试 —— 桌面会短暂显示上一帧或占位图");
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * v1.10.124：**真正的渲染画布**。
     *
     * 「原始比例」模式下，卡片本来就只占格子里「素材形状」的那一块，四周是透明的
     * （{@link FrameComposer#contentRect} 就是这么算的）。既然四周永远透明，
     * 就没必要把整块格子都编成图 —— **画布直接取这一块**。
     *
     * 好处不只是省 40% 的编码量：推给桌面的那张图**比例只由素材决定，与格子估算无关**。
     * 于是「桌面没报真实尺寸（vivo 不给 OPTION_APPWIDGET_SIZES）」「拖拽中途报了 0.62/0.75/1.30
     * 这些中间值」都只会让画面**清晰度**变化，绝不会再出现「整体比例缩一下」。
     */
    @NotNull
    static int[] renderCanvas(@NotNull Context context, @NotNull WidgetConfig config, @NotNull int[] box) {
        return renderCanvas(context, config, box, WidgetUriPush.enabled(context) ? 900000 : 220000);
    }

    /**
     * v1.10.128：可指定面积上限的版本。
     *
     * 为什么需要：**预装帧（A 方案）装帧走的是 setImageViewBitmap**（一次事务一张位图），
     * 就算同时开着 URI 推送，它的上限也必须是**位图的 22 万**，不能跟着 URI 放宽到 90 万 ——
     * 否则一次事务 2MB+，必然 TransactionTooLargeException，A 会被判定「没跑通」并自动关掉。
     */
    @NotNull
    static int[] renderCanvas(@NotNull Context context, @NotNull WidgetConfig config,
                              @NotNull int[] box, int maxPixels) {
        return renderCanvas(context, config, box, maxPixels, config.getLayout());
    }

    @NotNull
    static int[] renderCanvas(@NotNull Context context, @NotNull WidgetConfig config,
                              @NotNull int[] box, int maxPixels, @NotNull LayoutMode eff) {
        int fw = config.getFrameWidth();
        int fh = config.getFrameHeight();
        // v1.10.171（反馈）：**FIT 模式下画布的参考矩形必须是「卡片」，不是「格子」**。
        //
        // 现象（08:18:59 那批日志）：格子 597×400 装 1:1 素材 290，
        //   · 旧逻辑：拿**格子最长边 597** 去封顶 → 290×(400/597) = 290×194 → FIT 修正 → 194×194；
        //     桌面卡片其实是 400×400，于是 194 被放大 2.06×，白白丢掉 33% 的像素；
        //   · 新逻辑：先按素材比例把格子换算成**卡片** 400×400，再封顶 → 290×290（= min(素材, 卡片)），
        //     桌面放大 1.38×。体积 962KB → 约 1.6MB，单帧推送仍是 4ms 级（预算 30ms），不影响 33fps。
        //
        // 只在 FIT 下改；铺满（非 FIT）仍然按格子算 —— 那种模式下卡片就是格子，没有这层差异。
        // 素材比卡片大时行为不变（例如 2048 素材 / 597 格子：两种算法都给 597）。
        // v1.10.279：画布**永远跟格子同形**（原始比例也不例外）——
        // 卡片就是格子，画面在卡片里居中，四周由衬底补满；画布形状=格子形状，
        // 桌面 centerCrop 即恒等变换，谁也不再多裁一次。
        int[] capped = canvasForSource(box, fw, fh);
        int w = capped[0];
        int h = capped[1];
        // v1.10.128：**面积封顶按「真正推上去的那块」算**（不再按格子算）。
        // 原来封顶加在格子上（URI 90 万 / 位图 22 万），而卡片通常只占格子的一半左右，
        // 于是 4×4 组件仍被上采样（1030 → 990）。现在先算出卡片，再按面积等比封顶，
        // 大组件可以真正做到 1:1；位图模式的 22 万也落在真正过 Binder 的那张图上，更准。
        long area = (long) w * h;
        if (area > maxPixels) {
            double k = Math.sqrt(maxPixels / (double) area);
            w = Math.max(96, (int) (w * k));
            h = Math.max(96, (int) (h * k));
        }
        return new int[]{w, h};
    }

    /**
     * v1.10.60：**渲染画布不超过「素材原尺寸 × 1.1」**。
     *
     * 例：280×280 的 GIF 贴到 597×626 的组件上，以前每帧都按 458×480 合成 = 879KB/帧，
     * Binder 预算只够推 31.8fps（日志 W13），实测只有 25.7fps —— 33.3fps 的动图必然掉帧。
     * 而 458px 的画布对 280px 的素材没有任何画质收益（ImageView 反正要放大到 597），
     * 纯粹是白花 CPU + 白吃 Binder 字节。
     * 现在按素材尺寸封顶：280 → 308×322 ≈ 397KB/帧，预算够推 70fps，合成也快 3 倍。
     * 素材比组件大时（如 2048px 图片）行为完全不变（还是按组件框合成）。
     */
    @NotNull
    private static int widgetIdOf(@NotNull WidgetConfig config) {
        return config.getWidgetId();
    }

    /**
     * v1.10.172：FIT 模式下「卡片」矩形的**唯一出处**。
     *
     * 画布尺寸（{@link #renderCanvas}）和诊断日志（`W22` 的「卡片=…」）都从这里取，
     * 避免同一个事实在两处各算一套 —— 之前 `W22` 把「卡片=画布」写死成文案，
     * v1.10.171 把画布改成「min(素材, 卡片)」之后那句话就不成立了（卡片 380 而画布 280）。
     */
    @NotNull
    static int[] cardRect(@NotNull int[] box, int fw, int fh) {
        if (fw <= 0 || fh <= 0) {
            return new int[]{box[0], box[1]};
        }
        float srcRatio = fw / (float) fh;
        float boxRatio = box[0] / (float) Math.max(box[1], 1);
        if (srcRatio > boxRatio) {
            // 素材更宽：卡片先吃满格子的宽，高按素材收
            return new int[]{box[0], Math.max(96, Math.round(box[0] / srcRatio))};
        }
        // 素材更高/更方：卡片先吃满格子的高，宽按素材收
        return new int[]{Math.max(96, Math.round(box[1] * srcRatio)), box[1]};
    }

    static int[] canvasForSource(@NotNull int[] box, int frameWidth, int frameHeight) {
        int w = box[0];
        int h = box[1];
        int fw = Math.max(frameWidth, 0);
        int fh = Math.max(frameHeight, 0);
        if (fw > 0 && fh > 0) {
            // v1.10.131（D1）：1.1 → **1.0**。原来允许画布比素材最长边大 10%，
            // 那 10% 是凭空放大的像素 —— 桌面反正还要再缩/再放，纯属白编 4~10% 的体积。
            float limit = Math.max(fw, fh) * 1.0f;
            float longest = Math.max(w, h);
            if (longest > limit && longest > 0f) {
                float k = limit / longest;
                w = RangesKt.coerceAtLeast((int) (w * k), 96);
                h = RangesKt.coerceAtLeast((int) (h * k), 96);
            }
        }
        return new int[]{w, h};
    }

    /**
     * v1.10.109：位图撞 Binder 事务上限时的**一次性重试**：压到面积 150k 再推。
     * 单独成方法是为了让字节码结构干净（嵌套 try/finally 曾把 ClassCheck 的分析器搞崩）。
     */
    private static boolean retrySmall(@NotNull Context context, @NotNull AppWidgetManager manager,
                                      @NotNull RemoteViews base, int widgetId, @NotNull Bitmap src,
                                      int minW, int minH) {
        Bitmap small = null;
        try {
            long area = (long) src.getWidth() * (long) src.getHeight();
            if (area <= 150000L) {
                return false;
            }
            double factor = Math.sqrt(150000.0 / (double) area);
            int w = Math.max(64, (int) (src.getWidth() * factor));
            int h = Math.max(64, (int) (src.getHeight() * factor));
            // v1.10.322：压小之后的位图如果比格子还小，**宁可不推**。
            // 桌面按 intrinsic 量视图（AT_MOST），推一张 150k 的图 = 整张卡片被量小一圈，
            // 下一帧正常帧到了再弹回去 —— 用户看到的就是"先小一圈再恢复"。
            // 少一帧远好过多一次可笑的缩放。
            if (minW > 0 && w < minW) {
                return false;
            }
            if (minH > 0 && h < minH) {
                return false;
            }
            small = Bitmap.createScaledBitmap(src, w, h, true);
            RemoteViews views = new RemoteViews(base);
            views.setImageViewBitmap(R.id.widget_image, small);
            manager.updateAppWidget(widgetId, views);
            return true;
        } catch (Throwable th) {
            return false;
        } finally {
            if (small != null && !small.isRecycled()) {
                small.recycle();
            }
        }
    }

    /**
     * v1.10.324：尺寸未知（launcher 仍报默认 110dp）时，跳过一次推帧并延时补推。
     *
     * 只跳**一次**：某些 launcher 可能永远不回报真尺寸，那时宁可退回"按 110dp 估算"的旧行为，
     * 也不能让组件一直挂着占位图。
     */
    /** v1.10.328：推一个纯透明 RemoteViews，顶掉 launcher 自己的"组件加载中/应用图标"占位。 */
    private static void pushBlank(@NotNull Context context, int widgetId) {
        try {
            android.appwidget.AppWidgetManager manager = android.appwidget.AppWidgetManager.getInstance(context);
            if (manager == null) {
                return;
            }
            manager.updateAppWidget(widgetId,
                    new RemoteViews(context.getPackageName(), R.layout.widget_blank));
        } catch (Throwable ignored) {
        }
    }

    private static boolean skipWhileBoxUnknown(@NotNull Context context, @NotNull WidgetConfig config) {
        final int widgetId = config.getWidgetId();
        final Integer key = Integer.valueOf(widgetId);
        final long now = android.os.SystemClock.elapsedRealtime();
        boolean first = false;
        // v1.10.326（外部分析 §7.3）：跳过期间必须**重新读 options**，不能只信 SIZE_CACHE ——
        // 否则真尺寸到了也看不到，会一直跳到 5 秒上限。这里 200ms 复查一次（50 次/秒的 binder 调用太贵）。
        boolean recheck;
        synchronized (BOX_RECHECK_AT) {
            Long at = BOX_RECHECK_AT.get(key);
            recheck = at == null || now - at.longValue() >= BOX_RECHECK_MS;
            if (recheck) {
                BOX_RECHECK_AT.put(key, Long.valueOf(now));
            }
        }
        if (recheck) {
            INSTANCE.invalidateSize(widgetId);
            INSTANCE.widgetSize(context, android.appwidget.AppWidgetManager.getInstance(context), widgetId);
        }
        synchronized (BOX_DEFAULT) {
            if (!BOX_DEFAULT.contains(key)) {
                BOX_SKIP_UNTIL.remove(key);
                synchronized (BOX_RECHECK_AT) {
                    BOX_RECHECK_AT.remove(key);
                }
                return false;
            }
            Long until = BOX_SKIP_UNTIL.get(key);
            if (until == null) {
                until = Long.valueOf(now + SKIP_BOX_MAX_MS);
                BOX_SKIP_UNTIL.put(key, until);
                first = true;
            }
            if (now >= until.longValue()) {
                // 等够了：退回旧行为（按估算尺寸推），绝不把组件永久挂在占位图上
                BOX_SKIP_UNTIL.remove(key);
                return false;
            }
        }
        if (!first) {
            return true;
        }
        // v1.10.328：**主动把 launcher 那层"加载中/应用图标"顶掉**。
        // 327 只把 initialLayout 换成空白，但 vivo 在"组件刚放上去、还没收到我们的 update"时
        // 画的是它自己的加载态（应用图标），跟 initialLayout 无关。这里直接推一个空白 RemoteViews，
        // 让桌面立刻换成我们的透明视图 —— 用户看到的就从"图标闪一下"变成"空白 0.3 秒"。
        pushBlank(context, widgetId);
        try {
            final Context app = context.getApplicationContext() != null
                    ? context.getApplicationContext() : context;
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    INSTANCE.invalidateSize(widgetId);
                    INSTANCE.repushOne(app, widgetId, "尺寸未知延后");
                } catch (Throwable ignored) {
                }
            }, 700L);
            Diag.warn(context, "W28", "尺寸未知延后 id=" + widgetId
                    + "（launcher 仍报默认 110dp；先不按错比例烘帧，最多等 " + SKIP_BOX_MAX_MS + "ms）");
        } catch (Throwable ignored) {
        }
        return true;
    }

    public final long render(@NotNull Context context, @NotNull AppWidgetManager manager, @NotNull WidgetConfig config, int index, @Nullable FrameCache cache) {
        // v1.10.513（需求①·限制模式）：非原版签名不向桌面推帧。
        // 这里是本项目**唯一的推帧出口**（下面 471 那段注释写明：tick / 重推 / 更新 / 拖尺寸全绕不过去），
        // 所以限制只需要放这一处；返回 0L = 这一拍不排下一次，语义与下面 W45 那条完全相同。
        if (Integrity.restricted(context)) {
            return 0L;
        }
        // v1.10.471（#4 第 2 步修正）：**这一代已经交给桌面自播时，任何推帧都不许覆盖它**。
        //
        // 470 只在 PlaybackService 的 tick 里拦，于是 `W32 重推单个组件` / onUpdate / 尺寸变化
        // 这些一次性路径照样把位图推了上去 —— 日志里 `W45 动图推送` 之后 3 秒又出现
        // `推2 换图1`，桌面上的动图当场被替换成一张静止帧（用户：「导出后直接不播放」）。
        // 现在闸门放在唯一的推帧出口这里，tick / 重推 / 更新 / 拖尺寸全都绕不过去。
        if (config.getFrameCount() > 1 && AnimPush.adopted(context, config,
                android.os.SystemClock.elapsedRealtime())) {
            AnimPush.pushIfChanged(context, manager, config);
            Diag.warnThrottled(context, "W45", "已自播，跳过推帧 组件=" + config.getWidgetId()
                    + "（否则会把动图覆盖成静止帧）");
            return 0L;
        }
        Bitmap bitmap;
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(manager, "manager");
        Intrinsics.checkNotNullParameter(config, "config");
        long started = SystemClock.elapsedRealtime();
        // v1.10.113（A 方案）：预装帧模式 —— 先把帧装进 launcher，之后每帧只发切换指令。
        // 没跑通（launcher 不认 ViewFlipper / 帧读不出来）就**自动关掉并回退**，不留半死状态。
        if (WidgetFlip.enabled(context) && config.getFrameCount() > WidgetFlip.MAX_USEFUL_FRAMES) {
            // v1.10.135：A 方案只有 6 槽位，帧数一多就退化成「6 张图快速轮播」——
            // 用户实测（592 帧）就是「头 20 多秒一卡一卡」，直到 A 自己失败回落才变顺。
            // 这里直接跳过 A，全部走逐帧（URI 优先），语义与画面都正确。
            String skipKey = config.getWidgetId() + ":" + config.getFrameCount();
            boolean firstSkip;
            synchronized (FLIP_SKIP_LOGGED) {
                firstSkip = FLIP_SKIP_LOGGED.add(skipKey);
            }
            if (firstSkip) {
                Diag.warn(context, "W13", "预装帧跳过：素材 " + config.getFrameCount()
                        + " 帧 > " + WidgetFlip.MAX_USEFUL_FRAMES + "，A 方案只有 6 槽位会把动画变成幻灯片");
            }
        } else if (WidgetFlip.enabled(context)) {
            long spent = WidgetFlip.pump(context, manager, config, index);
            if (spent > 0L) {
                return spent;
            }
            if (spent < 0L) {
                // -1：帧/配置还没就绪（常见于「刚保存、服务手里还是旧配置」）——
                // 这一拍先让逐帧路径顶上，**不关开关**，下一拍再试。
                Diag.warnThrottled(context, "W13", "预装帧暂未就绪，本拍走逐帧（组件 "
                        + config.getWidgetId() + " 帧数=" + config.getFrameCount() + "）");
            } else {
                WidgetFlip.setEnabled(context, false);
                Diag.warn(context, "W13", "预装帧模式未跑通，已自动回退到逐帧推图（组件 "
                        + config.getWidgetId() + "）");
            }
        }
        Integer id = Integer.valueOf(config.getWidgetId());
        Long cooldown = FAIL_UNTIL.get(id);
        if (cooldown != null && started < cooldown.longValue()) {
            return 0L; // 连续失败后的冷却期：不重试、不记日志
        }
        // v1.10.324：launcher 还在报默认 110dp 时，**这一拍不烘、不推** —— 按 110dp 算出来的
        // 画布比例（1.130）和真格子（1.030）差 9.7%，推上去就是"比本应的小一圈"。
        // 每个组件只跳一次，700ms 后清缓存补推（那时 options 已经到位）。
        if (skipWhileBoxUnknown(context, config)) {
            return 0L;
        }
        int[] box = widgetSize(context, manager, config.getWidgetId());
        // v1.10.323：位图「不得小于格子」的下限（单位是**位图像素**）。
        // 实测（W22 帧画布那一行）：桌面摆 = 位图 569x553 × 3.28 = 1867x1815px ——
        // intrinsic = 位图像素 × density，所以位图只要 ≥ 格子px ÷ density 就盖得住格子；
        // 反过来，小于这个值的位图会被桌面按 intrinsic 量小一圈（v1.10.283 的老 bug）。
        float den = context.getResources().getDisplayMetrics().density;
        if (den <= 0f) {
            den = 1f;
        }
        // v1.10.352：**这里原来多除了一次 density**，导致 v1.10.322 想拦的东西从来没被拦住。
        // 实测（06:54 日志）：旧代帧 662x643 被原尺寸推进 879x869 的格子 —— 662 >= 879/3.28=268
        // 一路放行，于是 vivo 按 min(intrinsic, specSize) 把卡片量成 662，用户看到的就是
        // 「画面大小抖动」（每拍在 879 和 662 之间来回）。
        // BitmapFactory 解出来的位图 intrinsic 就是**像素数**（targetDensity == srcDensity），
        // 不该再除 density。现在下限 = 格子像素本身。
        int minBmpW = Math.max(1, box[0]);
        int minBmpH = Math.max(1, box[1]);
        // v1.10.192：「原始比例」布局改走**帧画布**（只跟素材比例有关，与格子无关）——
        // 拖动尺寸不再产生新文件，桌面自己 fitCenter；其它布局继续按格子算（旧行为）。
        int[] size = canvasFor(context, config, box);
        diagCanvas(context, config, size, box);
        // v1.10.251：静态图 1:1 高清**借** URI 推送这条路（画布已经大于 1MB 上限，只有文件能过去）。
        // 与「URI 推送」开关不同：这里只对单帧图片生效，开关关着也照走；失败仍回退到位图路径。
        boolean uriPush = needUri(context, config);
        // v1.10.351：**每一拍都在这里记账**（守恒分母）。放在 uriPush 之前，
        // 是因为 uriPush=false 时走的是位图路径，那条路径同样会记一次 `现渲染=`；
        // 如果 tick 写在 `if (uriPush)` 里面，非 URI 通道的拍就永远对不上。
        WidgetUriPush.noteTick(config.getWidgetId());
        // v1.10.119：**预热在这里启动**。只有走到这一步的尺寸才是桌面真实画布
        // （配置页保存时 vivo 还只给 110×110dp 的默认值，按那个热出来的帧全都对不上）。
        // 指纹没变时 ensure 只是一次 map 查询，可以每帧调用。
        if (uriPush) {
            WidgetPrewarm.ensure(context, config, size[0], size[1]);
        }
        StringBuilder $this$render_u24lambda_u240 = new StringBuilder();
        $this$render_u24lambda_u240.append(config.getWidgetId()).append(':');
        $this$render_u24lambda_u240.append(size[0]).append(':').append(size[1]).append(':');
        $this$render_u24lambda_u240.append(config.getLayout().name()).append(':');
        $this$render_u24lambda_u240.append(config.getShape().name()).append(':');
        $this$render_u24lambda_u240.append(config.getShapeAlpha()).append(':');
        $this$render_u24lambda_u240.append(config.getScalePercent()).append(':');
        $this$render_u24lambda_u240.append(config.getShadowPercent()).append(':');
        // v1.10.221：曲线也进 key —— 只改曲线不改强度时，缓存帧不能还留着旧阴影
        $this$render_u24lambda_u240.append(config.getShadowCurve()).append(':');
        $this$render_u24lambda_u240.append(config.getOffsetXPercent()).append(':').append(config.getOffsetYPercent()).append(':');
        $this$render_u24lambda_u240.append(config.getMaskOffsetXPercent()).append(':').append(config.getMaskOffsetYPercent()).append(':');
        $this$render_u24lambda_u240.append(config.getContentScalePercent()).append(':');
        $this$render_u24lambda_u240.append(config.getFitContent()).append(':');
        $this$render_u24lambda_u240.append(config.getRotation()).append(':');
        $this$render_u24lambda_u240.append(config.getShapeRotation()).append(':');
        $this$render_u24lambda_u240.append(config.getBorder() ? 1 : 0).append(':');
        $this$render_u24lambda_u240.append(index);
        String key = $this$render_u24lambda_u240.toString();
        Intrinsics.checkNotNullExpressionValue(key, "toString(...)");
        Bitmap cached = cache != null ? cache.get(key) : null;
        if (cached != null) {
            if (uriPush) {
                WidgetUriPush.noteHit(config.getWidgetId());
            }
            bitmap = cached;
        } else {
            // v1.10.117（反馈）：URI 模式下如果这一帧**已经编码落盘**，直接推 URI ——
            // 合成、编码、写盘全跳过（实测这一跳能把单帧从 58ms 降到 1~2ms）。
            if (uriPush) {
                android.net.Uri hit = WidgetUriPush.cachedUri(context, config, index, size[0], size[1]);
                // v1.10.275：与日志共用同一个判据（见 fitViewFor）
                boolean fitNow = fitViewFor(config, size, box);
                // v1.10.344：把**格子**一起传下去 —— 大格子走「共享位图」（桌面零解码）。
                // 原来这里判断完还硬写 noteTransport("URI")，把 pushCached 内部记的
                // 「位图共享」覆盖掉了 —— 日志因此一直在说 URI，我上一轮据此判断全错。
                if (hit != null && WidgetUriPush.pushCached(context, manager, config, hit, fitNow, box)) {
                    WidgetUriPush.noteHit(config.getWidgetId());
                    WidgetPrewarm.noteFirstHit(context, config.getWidgetId());   // v1.10.289
                    if (WidgetRender.recentTransport().length() == 0) {
                        noteTransport("URI");
                    }
                    return SystemClock.elapsedRealtime() - started;
                }
                // v1.10.351（外部分析三家交叉验证后的主路径）：**新代这一帧还没烘好 —— 不顶帧、不跳过。**
                //
                // 根因（三家都没看到、在代码里才看得见的那个死结）：
                //   ① nearestCached(新代, back=8) 排在 nearestAnyCanvas(旧代) 之前 —— 新代只要烘出
                //      「播放头往回 8 帧」内的任何一帧就命中，旧代同号帧永远轮不到，画面帧号被
                //      钉在烘帧前沿上（实测 39.3fps + 命中=0% + 最大落后恒 8）。
                //   ② 即使轮到旧代，v1.10.322 的 minW/minH 会一律拒绝「比格子小的旧画布」——
                //      42.6 万（662x643）顶进 76.4 万（879x869）格子必然被拒。
                // 两条各自正确的修复互相堵死，于是退化成「顶帧 → 跳过 → 现场合成 222ms」。
                //
                // 现在改成：烘好的帧没有 → **现渲染一张原尺寸（格子像素）的直接推**。
                // 它省掉编码那 119ms（27M px/s 的全部来源），读帧 14ms + 合成 3ms 在 4 条
                // frame-decode 线程上跑，推帧线程只取现成的，节拍照旧 20ms。
                // 画质：过渡期就是原生分辨率，烘完切到磁盘帧时**没有清晰度台阶**。
                // v1.10.351：目标尺寸传 **size（画布）** 而不是 box（格子）——
                // 稳态推的是「画布尺寸的磁盘帧」，现渲染必须与它同尺寸，否则会出现倒挂
                // （过渡期按格子合成 76.4 万很清，烘完切到画布 24~45 万反而变糊）。
                // 画布 ≥ 格子这件事由「1:1 档撤销 45 万上限」保证（见 budgetFor）；
                // 其它档画布本来就小于格子，那时稳态与过渡期同样偏小，是既有行为，不在 351 改。
                if (WidgetUriPush.pushRendered(context, manager, config, index, size, fitNow)) {
                    WidgetPrewarm.noteFirstHit(context, config.getWidgetId());
                    return SystemClock.elapsedRealtime() - started;
                }
                // v1.10.120（反馈「有几帧卡卡的」）：**预热还在烘的时候，这一拍顶一张已经烘好的相邻帧**。
                // 原因：813×853 现合成一帧要 125ms（日志实测 单帧=125.7ms），而「一拍只渲染一个组件」，
                // 这一拍堵 125ms 就把同一拍里的另一个组件一起饿死（实测邻居 29.9fps → 7.7fps）。
                // 顶帧只要 2~3ms，节拍照旧不断；烘好的新帧下一拍就会接上，最多落后 90 帧（≈3 秒）。
                // v1.10.257（反馈「连续拖动 fps 明显抖动」的根因）：
                // 原来这里只判 `busy`。可 v1.10.256 给预热加了 800ms 静置去抖 —— 拖动期间
                // 预热**不在跑**（busy=false），于是每一拍都走到下面的「现场合成」：
                // 1030×947 合成+编码 ≈ 120ms，节拍直接崩（实测 p95=117ms、最大 193ms、
                // 迟到>8ms 高达 22 次、fps 45→32.5）。
                // 现在只要**这一版画布还没烘好**（pending）就同样走顶帧，拖动期间画面不掉节拍；
                // 静置 800ms 后预热补烘，补上后自然切换成真帧（pending 变 false → 正常命中）。
                if (WidgetPrewarm.busy(config.getWidgetId())
                        || WidgetPrewarm.pending(context, config, size[0], size[1])) {
                    // v1.10.123（反馈「组件放大后视频突然加速」）：回看上限从 90 帧收到 **8 帧**。
                    // 原来允许落后 90 帧（3 秒）：冷启动期间内容越拉越后，烘完的一瞬间位置追回墙钟，
                    // 看起来就是「视频突然快进了几秒」。现在最多落后 8 帧（0.27 秒），追回时肉眼看不出；
                    // 超过就退回主线程现合成，宁可贵一下，也不让内容跑快的错觉出现。
                    android.net.Uri near = WidgetUriPush.nearestCached(context, config, index,
                            size[0], size[1], 8);
                    if (near == null) {
                        // 新画布还一帧都没有（刚拖大/刚导出）→ 先顶旧画布的同号帧：内容对、只是分辨率不同
                        // v1.10.322：但**比格子小的旧画布一律不许顶**（见 nearestAnyCanvas 的注释）：
                        // 位图小于格子时 vivo 会按 intrinsic 把整张卡片量小一圈 ——
                        // 用户报的"导出后未播放前有 1 帧先小一圈再恢复正常"就是这一帧。
                        near = WidgetUriPush.nearestAnyCanvas(context, config, index, size[0], size[1],
                                minBmpW, minBmpH);
                    }
                    // v1.10.272：**比例一致的旧画布也没有**（拖动时画布是 597x1305 → 813x1305 →
                    // 1030x853 连跳，每档比例都不同，上面那个"比例一致"的筛选必然全落空）——
                    // 这时原来会掉进下面的「现场合成」：一帧 222ms 编码，单线程推帧被堵死，
                    // 三个组件一起掉到 3.8fps（日志 04:39:48~04:40:04，整整 17 秒）。
                    // 现在退一步：拿任意比例的旧画布 + fitCenter 内接（不裁切、四周露壁纸）。
                    boolean lose = false;
                    if (near == null) {
                        // v1.10.292（用户："还会出现内缩后恢复正常的 BUG"）：
                        // 这里原来会把**比例和当前格子不一致的旧画布**当顶帧推上去，桌面 centerCrop
                        // 之后画面在一个比例里被装进另一个比例的格子 —— 肉眼看就是"缩一下再恢复"。
                        // 现在干脆不推：宁可这一拍保持上一帧（≤0.7 秒就会被真画布顶掉），
                        // 也不要出现任何"例外的缩放"。
                        near = null;
                    }
                    // v1.10.278：旧画布一律 centerCrop（false）。fitCenter 会把它缩成格子中间
                    // 一小块 —— 那正是用户反复说的"截断"；centerCrop 的代价只是多裁一点点。
                    if (near != null
                            // v1.10.300（用户："首次播放时第 1 帧整体放大→变回正常"）：
                            // 这里一直写死 centerCrop —— 顶上去的旧画布被"铺满"（看起来偏大），
                            // 下一拍真帧走 fitCenter（内接）就缩回正常 → 首帧"放大一下"。
                            // 现在与正常推帧共用同一个判据（fitViewFor），首帧和后续帧同一种缩放。
                            // v1.10.309：换了素材（帧数变了）就不许顶上一份素材的旧画布——
                            // 那份画布的留边比例不同，fitCenter 内接进新格子会让画面"先小一圈"。
                            && !WidgetUriPush.countChanged(config.getWidgetId(), config.getFrameCount())
                            && WidgetUriPush.pushCached(context, manager, config, near,
                                    fitViewFor(config, size, box))) {
                        WidgetUriPush.notePushedSize(config.getWidgetId(), size[0], size[1]);
                    WidgetUriPush.notePushedCount(config.getWidgetId(), config.getFrameCount());
                        WidgetUriPush.noteStale(config.getWidgetId());
                        noteTransport("URI");
                        return SystemClock.elapsedRealtime() - started;
                    }
                    // v1.10.279（用户连报两次「导出后先显示应用图标过会才恢复」）：
                    // 上面三条兜底全落空 = 这个组件**一帧都还没推过**，桌面此刻挂着的正是
                    // 我们自己推的占位图 widget_preview。这时候"这一拍不推"就等于把占位图
                    // 钉在桌面上 —— 日志 09:53:23 保存、09:53:32 才由预热 W32 补推，中间 9 秒
                    // 用户看到的就是那个深色方块+绿三角。
                    // 所以：推过帧的组件才允许跳过（拖动时防饿死）；首帧必须当场合成。
                    // v1.10.286：占位图要顶掉，**尺寸变了也要当场顶掉**（否则桌面停在旧画布上被拉伸，
                    // 用户看到的就是"变长条没实时渲染"）。只有"尺寸没变、且桌面已显示真内容"才允许跳过。
                    if (!WidgetUriPush.showingPlaceholder(config.getWidgetId())
                            && !WidgetUriPush.sizeStale(config.getWidgetId(), size[0], size[1])) {
                        WidgetUriPush.noteSkip(config.getWidgetId());
                        return 0L;
                    }
                }
                WidgetUriPush.noteMiss(config.getWidgetId());
            }
            Bitmap frame = WidgetStore.INSTANCE.loadFrame(context, config, index, size[0], size[1]);
            if (frame == null) {
                // v1.10.56：**绝不能静默**。这里以前直接 return 0，桌面就永远停在占位图（应用图标）上，
                // 用户完全无从判断是「没导出」「帧文件丢了」还是「解码失败」。
                // W14 把三件事写清楚：第几帧、配置说有几帧、那个文件到底在不在。
                try {
                    boolean exists = WidgetStore.INSTANCE.frameFile(context, config.getWidgetId(), index).exists();
                    Diag.warnThrottled(context, "W14", "小部件取不到帧: id=" + config.getWidgetId()
                            + " idx=" + index + " count=" + config.getFrameCount()
                            + " kind=" + config.getKind()
                            + " 文件存在=" + exists
                            + " fw/fh=" + config.getFrameWidth() + "x" + config.getFrameHeight()
                            + " 渲染=" + size[0] + "x" + size[1]
                            + " 组件框=" + box[0] + "x" + box[1]);
                } catch (Throwable ignored) {
                }
                // v1.10.351：这一拍没推出任何帧 → 归「跳过」，否则四维之和不等于拍数（守恒会报 ❗）
                WidgetUriPush.noteSkip(config.getWidgetId());
                noteFailure(context, config.getWidgetId(),
                        new IllegalStateException("loadFrame=null idx=" + index));
                return 0L;
            }
            try {
                Bitmap masked = FrameComposer.INSTANCE.compose(frame, size[0], size[1], effLayout(config, box), config.getShape(), config.getShapeAlpha(), config.getBorder(), config.getScalePercent(), config.getRotation(), config.getShapeRotation(), config.getFitContent(), config.getShadowPercent(), config.getShadowCurve(), config.getOffsetXPercent(), config.getOffsetYPercent(), config.getContentScalePercent(), config.getMaskOffsetXPercent(), config.getMaskOffsetYPercent());
                frame.recycle();
                if (cache != null) {
                    cache.put(key, masked);
                }
                bitmap = masked;
            } catch (Throwable th) {
                frame.recycle();
                // v1.8.11：这里原来 rethrow —— WidgetProvider.onUpdate 在 UI 线程调本方法，
                // 一抛就是广播崩溃杀进程（闪退）。改成记录后返回 0，下一帧还会重试。
                WidgetUriPush.noteSkip(config.getWidgetId());
                noteFailure(context, config.getWidgetId(), th);
                return 0L;
            }
        }
        Bitmap bitmap2 = bitmap;
        // v1.10.193：新通道下画布比例 = area 比例，所以 1:1 / 16:9 这些也要用 fitCenter 的
        // layout（桌面把帧等比内接进格子，天然还原旧的 area 居中）；填充仍用 centerCrop。
        // v1.10.275：与 URI 路径、与日志共用同一个判据
        boolean fitView = fitViewFor(config, size, box);
        // v1.10.116（B 方案）：URI 模式 —— 帧落成文件，Binder 里只走一个 content:// 路径。
        // 清晰度不再受 1MB 限制；失败（写盘/授权/桌面不认）会自动回退到位图推送。
        if (uriPush) {
            if (WidgetUriPush.push(context, manager, config, index, bitmap2, fitView)) {
                WidgetUriPush.notePushedSize(config.getWidgetId(), size[0], size[1]);
                    WidgetUriPush.notePushedCount(config.getWidgetId(), config.getFrameCount());
                // v1.10.351：这条是「同步现渲染」兜底（预热没在跑时才会走到）——
                // 与异步现渲染同一口径，归进 `现渲染=`，否则守恒会差一拍。
                WidgetUriPush.noteRender(config.getWidgetId());
                noteTransport("URI");
                Integer okStreak = FAIL_STREAK.remove(id);
                if (okStreak != null && okStreak.intValue() > 0) {
                    FAIL_UNTIL.remove(id);
                    Diag.warn(context, "W31", "组件 " + widgetIdOf(config) + " 恢复推帧（此前连续失败 "
                            + okStreak + " 次）");
                }
                return SystemClock.elapsedRealtime() - started;
            }
        }
        RemoteViews base = fitView ? prototypeFit : prototype;
        if (base == null) {
            base = new RemoteViews(context.getPackageName(),
                    fitView ? R.layout.widget_media_fit : R.layout.widget_media);
            base.setOnClickPendingIntent(R.id.widget_image, contentIntent(context));
            if (fitView) {
                prototypeFit = base;
            } else {
                prototype = base;
            }
        }
        RemoteViews views = new RemoteViews(base);
        WidgetUriPush.noteRender(config.getWidgetId());   // v1.10.351：同 3)，归现渲染
        noteTransport("位图");
        views.setImageViewBitmap(R.id.widget_image, bitmap2);
        try {
            manager.updateAppWidget(config.getWidgetId(), views);
        } catch (Throwable th2) {
            // v1.10.109：**推帧失败原来被静默吞掉**。
            // 桌面看到的是「上一帧」——组件放大后如果新帧推不上去（Binder 单次事务约 1MB，
            // 大组件的位图很容易撞 TransactionTooLargeException），用户看到的就是
            // 「画面还停在旧比例上被裁/被切」。记 W13（已有代号，不新增）并压小重试一次。
            Diag.warn(context, "W30", "updateAppWidget 失败: " + th2.getClass().getSimpleName()
                    + " 组件=" + config.getWidgetId()
                    + " 位图=" + bitmap2.getWidth() + "x" + bitmap2.getHeight()
                    + " ≈" + (bitmap2.getAllocationByteCount() / 1024) + "KB");
            boolean ok = retrySmall(context, manager, base, config.getWidgetId(), bitmap2, minBmpW, minBmpH);
            Diag.warn(context, "W30", ok ? "已自动压小并重推成功" : "压小重推未成功");
        }
        // 这一帧成功推上桌面 → 清掉失败计数（如果之前在冷却/连败，留一条恢复日志）
        Integer streak = FAIL_STREAK.remove(id);
        if (streak != null && streak.intValue() > 0) {
            FAIL_UNTIL.remove(id);
            Diag.warn(context, "W31", "组件 " + config.getWidgetId() + " 恢复推帧（此前连续失败 " + streak + " 次）");
        }
        return SystemClock.elapsedRealtime() - started;
    }
}
