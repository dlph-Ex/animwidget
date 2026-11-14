package io.github.animwidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.widget.RemoteViews;

/**
 * v1.10.113（需求 1/2：A 方案「预装帧」）—— **和现在的逐帧推图并行的一套可选系统**。
 *
 * 现状（默认）：每帧合成一张位图 → `updateAppWidget` 搬过去 ≈ 880KB/帧。
 * 两组件 30fps 就是 50+MB/s，Binder（单次 1MB）与 launcher 的 inflate 都扛不住，
 * 所以实测会掉到 25~29fps。这是「搬图太贵」，不是系统禁止高频。
 *
 * 本模式（实验，默认关）：数组里先放 6 个 ImageView（ViewFlipper），
 *   1) 只在**第一次**（或配置变化后）把 6 帧**分批**装进 launcher —— 每次事务只搬一张，绕开 1MB；
 *   2) 之后每帧只发 `setDisplayedChild(slot)`，事务大小 ≈ 几十字节。
 * 带宽从 880KB/帧 掉到 ~0.1KB/帧，频率上限不再由 Binder 决定。
 *
 * 代价（如实写在开关旁边）：
 *   · 只有 6 个显示槽 → 帧数多的素材会被**等间隔抽成 6 帧**（观感是 6 帧循环，不是全帧）；
 *   · launcher 端常驻 6 张位图（≈5MB）；
 *   · OEM 桌面若忽略 ViewFlipper，会一直停在第一帧 —— 此时关掉开关即可回到原样（开关就是后路）。
 *
 * 失败一律**自动回退**：装载失败 → 关开关 → 交回 {@link WidgetRender} 的逐帧路径。
 */
public final class WidgetFlip {

    /**
     * v1.10.135：**帧数超过这个数就不要再走 A 方案**。
     *
     * A 方案只有 {@link #SLOTS} 个槽位，`slot = index × SLOTS / count` ——
     * 实测 592 帧的动图被它变成「6 张图快速轮播」，看起来就是一卡一卡的幻灯片，
     * 而不是「更快的一条路」。6 槽位最多承载 12 帧还没什么损失，再多就必须回逐帧。
     */
    /** 显示槽数量（= widget_media_flip.xml 里的 ImageView 个数）。 */
    public static final int SLOTS = 6;

    /** v1.10.135：帧数超过 12 就别用 A 方案了（6 槽位只能无损表达 6 帧，12 帧勉强）。 */
    static final int MAX_USEFUL_FRAMES = SLOTS * 2;

    private static final int[] IMAGE_IDS = {
            R.id.flip_0, R.id.flip_1, R.id.flip_2,
            R.id.flip_3, R.id.flip_4, R.id.flip_5
    };

    private static final String PREFS = "settings";
    private static final String PREF = "flip_mode";

    /** widgetId → 已经装好的那批帧的「配置指纹」，配置一变就重装。 */
    private static final java.util.HashMap<Integer, String> PREPARED = new java.util.HashMap<>();

    private WidgetFlip() {
    }

    public static boolean enabled(Context c) {
        return c.getSharedPreferences(PREFS, 0).getBoolean(PREF, false);
    }

    public static void setEnabled(Context c, boolean on) {
        c.getSharedPreferences(PREFS, 0).edit().putBoolean(PREF, on).apply();
        PREPARED.clear();
    }

    /** 帧/形状/布局任一处变了都要重装。 */
    private static String stamp(WidgetConfig config, int[] box) {
        StringBuilder sb = new StringBuilder();
        sb.append(box[0]).append('x').append(box[1]).append(':')
                .append(config.getFrameCount()).append(':')
                .append(config.getFrameWidth()).append('x').append(config.getFrameHeight()).append(':')
                .append(config.getLayout().name()).append(':')
                .append(config.getShape().name()).append(':')
                .append(config.getShapeAlpha()).append(':')
                .append(config.getScalePercent()).append(':')
                .append(config.getRotation()).append(':')
                .append(config.getShapeRotation()).append(':')
                .append(config.getContentScalePercent()).append(':')
                .append(config.getMaskOffsetXPercent()).append(',').append(config.getMaskOffsetYPercent()).append(':')
                .append(config.getOffsetXPercent()).append(',').append(config.getOffsetYPercent()).append(':')
                .append(config.getShadowPercent()).append(':')
                .append(config.getBorder() ? 1 : 0).append(':')
                .append(config.getFitContent());
        return sb.toString();
    }

    /**
     * 推一帧（预装帧模式）。
     *
     * @return 耗时 ms；**0 表示这个模式没跑通**，调用方应回退到逐帧推图
     */
    public static long pump(Context context, AppWidgetManager manager, WidgetConfig config, int index) {
        long started = SystemClock.elapsedRealtime();
        int widgetId = config.getWidgetId();
        int[] box = WidgetRender.INSTANCE.widgetSize(context, manager, widgetId);
        String want = stamp(config, box);
        String have = PREPARED.get(Integer.valueOf(widgetId));
        if (!want.equals(have)) {
            PREPARED.remove(Integer.valueOf(widgetId));
            int prepared = prepare(context, manager, config, want, box);
            if (prepared < 0) {
                // -1：配置/帧还没就绪（例如刚保存、PlaybackService 手里还是旧 config）
                // 这次先让逐帧路径顶上，**不要关开关**，下一拍再试
                return -1L;
            }
            if (prepared == 0) {
                return 0L;
            }
        }
        int count = Math.max(1, config.getFrameCount());
        int slot = (int) ((long) index * SLOTS / count);
        if (slot < 0) {
            slot = 0;
        }
        if (slot >= SLOTS) {
            slot = SLOTS - 1;
        }
        try {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_media_flip);
            views.setDisplayedChild(R.id.widget_flipper, slot);
            manager.partiallyUpdateAppWidget(new int[]{widgetId}, views);
        } catch (Throwable th) {
            PREPARED.remove(Integer.valueOf(widgetId));
            return 0L;
        }
        return SystemClock.elapsedRealtime() - started;
    }

    /** 分批把 6 帧装进 launcher（每次事务只含一张位图）。 */
    private static int prepare(Context context, AppWidgetManager manager,
                               WidgetConfig config, String want, int[] box) {
        int widgetId = config.getWidgetId();
        if (config.getFrameCount() <= 0) {
            return -1;
        }
        try {
            // v1.10.128：A 方案装帧是位图事务，上限固定 22 万（跟着 URI 放宽会撞 1MB → 被自己判死）
            int[] size = WidgetRender.renderCanvas(context, config, box, 220000);
            int count = Math.max(1, config.getFrameCount());

            // 1) 先让 launcher 把这个布局 inflate 出来（否则 partialUpdate 找不到视图）
            RemoteViews base = new RemoteViews(context.getPackageName(), R.layout.widget_media_flip);
            base.setOnClickPendingIntent(R.id.widget_flipper, WidgetRender.INSTANCE.contentIntent(context));
            manager.updateAppWidget(widgetId, base);

            // 2) 逐张装载：等间隔取 SLOTS 帧，保证首尾都覆盖到
            for (int slot = 0; slot < SLOTS; slot++) {
                int idx = count <= 1 ? 0
                        : (int) Math.round((double) slot * (count - 1) / (double) (SLOTS - 1));
                Bitmap frame = WidgetStore.INSTANCE.loadFrame(context, config, idx, size[0], size[1]);
                if (frame == null) {
                    return -1;
                }
                Bitmap masked;
                try {
                    masked = FrameComposer.INSTANCE.compose(frame, size[0], size[1],
                            config.getLayout(), config.getShape(), config.getShapeAlpha(),
                            config.getBorder(), config.getScalePercent(), config.getRotation(),
                            config.getShapeRotation(), config.getFitContent(),
                            config.getShadowPercent(), config.getShadowCurve(), config.getOffsetXPercent(),
                            config.getOffsetYPercent(), config.getContentScalePercent(),
                            config.getMaskOffsetXPercent(), config.getMaskOffsetYPercent());
                } finally {
                    if (!frame.isRecycled()) {
                        frame.recycle();
                    }
                }
                if (masked == null) {
                    return 0;
                }
                RemoteViews one = new RemoteViews(context.getPackageName(), R.layout.widget_media_flip);
                one.setImageViewBitmap(IMAGE_IDS[slot], masked);
                manager.partiallyUpdateAppWidget(new int[]{widgetId}, one);
                if (!masked.isRecycled()) {
                    masked.recycle();
                }
            }
            PREPARED.put(Integer.valueOf(widgetId), want);
            Diag.warn(context, "W13", "预装帧装载完成 组件=" + widgetId
                    + " 槽位=" + SLOTS + " 画布=" + size[0] + "x" + size[1]
                    + "（之后每帧只发切换指令）");
            return 1;
        } catch (Throwable th) {
            Diag.warn(context, "W13", "预装帧装载失败: " + th.getClass().getSimpleName()
                    + " 组件=" + widgetId);
            return 0;
        }
    }
}
