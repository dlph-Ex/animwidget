package io.github.animwidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.os.SystemClock;

import java.io.File;
import java.util.HashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v1.10.470（#4 第 2 步）：**让桌面自己播动图文件**，不再逐帧推送。
 *
 * <h3>判定（v1.10.472 起带缓存）</h3>
 * 这一代烘焙目录里有 {@code anim.webp}、体积 ≤ {@link #MAX_BYTES}、帧数 ≤ {@link #MAX_FRAMES}，
 * 且用户开着「URI 推送」→ 视为可自播：{@code setImageViewUri} 推**一次**，之后
 * {@code PlaybackService} 不再给它排队、{@code WidgetRender.render()} 也不再推任何单帧。
 *
 * <h3>为什么必须有缓存</h3>
 * {@link #adopted} 是在**每一帧**的推帧出口被问的（50fps 就是每秒 50 次）。
 * 471 的第一版在这里做了三件重活：读 SharedPreferences、{@code listFiles()} 扫目录、
 * 每帧打一条日志 —— 实测日志 50 行/秒（264 行同样的话），而且目录扫描是磁盘 IO。
 * 现在结论按 {@link #CACHE_MS} 缓存，日志只在**结论变化**时记一条。
 *
 * <h3>为什么有帧数上限</h3>
 * 实测 1280×720 / 235 帧 / 15.7MB 那一档在 vivo 上不自播（见 PLAN-4-ANIM-URI.md）。
 * 在弄清原因之前只让小组件走自播 —— 宁可少一个特性，不许把用户的组件冻住。
 */
final class AnimPush {

    /**
     * v1.10.475：**自播默认关闭**。
     *
     * <p>**外部两模型独立核对 AOSP 源码后的结论（2026-11）**：`RemoteViews.setImageViewUri` →
     * `ImageView.setImageURI` → `ImageDecoder.decodeDrawable` 在 `info.isAnimated()` 时**确实**返回
     * `AnimatedImageDrawable`（API 28+），但**没有任何人调用 `start()`**：ImageView / RemoteViews /
     * AppWidgetHostView 里都搜不到 `Animatable.start()`，而 `AnimatedImageDrawable` 默认不自动播放。
     * 所以「首帧能画出来、但一秒都不动」是**必然**结果 —— 与我们封装器、文件大小、帧数全都无关。
     * 这也解释了用户说「会播呀」：他见过的 widget 动图是 APK 内置资源走
     * `&lt;animated-image android:autoStart="true"&gt;`（那条 XML 会显式 start），不是运行时素材。</p>
     *
     * <p>两次真机验证的经过也一并留在这里：470 是「推上去 3 秒后被一次性重推覆盖」；471 修掉覆盖之后，
     * 474 再测 —— `W45 桌面取走动图` 出现了，但桌面**没有动**（这一代之后再没推过一帧），
     * 用户看到的就是「导出后不播放」。结论：这条设备上 `setImageViewUri` + 动图 WebP
     * **不能自播**（至少 16MB / 235 帧这一档不行）。</p>
     *
     * <p>所以默认关掉，不再拿用户的桌面做实验。代码与门禁都留着（打包 W44、口径 W47 照旧），
     * 将来要用（小文件、或确认能播的路径）把这里改 true 并重新验一遍。</p>
     */
    static final boolean SELF_PLAY = false;


    /**
     * 单文件上限 **24MB**（超过就继续逐帧推）。
     *
     * <p>实测三档：372×372/140 帧 = 3.5MB、1280×720/235 帧 = 15.7MB、546×602/172 帧 = 21.4MB。
     * 我们的单帧是 q97 全画布 WebP（约 0.37 字节/像素），所以「小画布」不等于「小文件」——
     * 6MB 的上限会把 21MB 这种正常内容全部挡在外面（v1.10.472 的日志就是这么发生的）。
     * 现在放到 24MB，让这几种真实素材都能进自播，用真机判定「桌面到底能不能播大文件」。</p>
     */
    static final long MAX_BYTES = 24L * 1024L * 1024L;

    /** 帧数上限 **240**（实测最大的是 235 帧，留一点余量）。 */
    static final int MAX_FRAMES = 240;

    /** 推送后多久没被桌面取走就判定「不支持」，回退逐帧。 */
    static final long GRACE_MS = 4000L;

    /** 判定结果的缓存时长（毫秒）。换代会由 {@link #forget} 立刻失效。 */
    static final long CACHE_MS = 2000L;

    /** 每组件：已经推过的那一代（`路径@大小#mtime`）。 */
    private static final HashMap<Integer, String> PUSHED = new HashMap<>();
    /** 每组件：上一次推送的时刻（看门狗用）。 */
    private static final HashMap<Integer, Long> PUSHED_AT = new HashMap<>();
    /** 每组件：判定缓存。 */
    private static final HashMap<Integer, Hit> HITS = new HashMap<>();
    /** 每组件：上一次记过的结论（同一结论不重复记日志）。 */
    private static final HashMap<Integer, String> LAST_REASON = new HashMap<>();

    private AnimPush() {
    }

    /** 一次判定的结果：可自播 → file 非空、why 为空；不可 → file 为空、why 是原因。 */
    private static final class Hit {
        final long at;
        final File file;
        final String why;

        Hit(long at, @Nullable File file, @Nullable String why) {
            this.at = at;
            this.file = file;
            this.why = why;
        }
    }

    /**
     * 判定（带缓存）。**这是唯一的重活入口**，其它方法都走它。
     *
     * @param frameCount 配置里的帧数（≤0 表示不检查帧数上限）
     */
    @NotNull
    private static Hit verdict(@NotNull Context context, int widgetId, int frameCount) {
        long now = SystemClock.elapsedRealtime();
        synchronized (HITS) {
            Hit cached = HITS.get(Integer.valueOf(widgetId));
            if (cached != null && now - cached.at < CACHE_MS) {
                return cached;
            }
        }
        File file = null;
        String why = null;
        if (!SELF_PLAY) {
            why = "自播默认关闭（两次真机验证桌面不自播）";
        } else if (!WidgetUriPush.enabled(context)) {
            why = "URI 推送开关是关的";
        } else if (frameCount > 0 && frameCount > MAX_FRAMES) {
            why = "帧数 " + frameCount + " > " + MAX_FRAMES;
        } else {
            File found = WidgetUriPush.animFileNewest(context, widgetId);
            if (found == null) {
                why = "这一代还没有 anim.webp";
            } else if (found.length() > MAX_BYTES) {
                why = "文件 " + (found.length() / 1024L / 1024L) + "MB > "
                        + (MAX_BYTES / 1024L / 1024L) + "MB";
            } else if (found.length() <= 0L) {
                why = "文件是空的";
            } else {
                file = found;
            }
        }
        Hit hit = new Hit(now, file, why);
        synchronized (HITS) {
            HITS.put(Integer.valueOf(widgetId), hit);
        }
        // 只在**结论变化**时记一条 —— 这个函数每秒被问几十次，不能每帧打日志
        String now2 = file != null ? "ok" : why;
        boolean changed;
        synchronized (LAST_REASON) {
            String prev = LAST_REASON.put(Integer.valueOf(widgetId), now2);
            changed = !now2.equals(prev);
        }
        if (changed) {
            if (file != null) {
                Diag.warn(context, "W45", "动图自播可用 组件=" + widgetId + " 帧=" + frameCount
                        + " 大小=" + (file.length() / 1024L) + "KB（等桌面来取）");
            } else {
                Diag.warn(context, "W45", "动图自播未启用 组件=" + widgetId + "：" + why);
            }
        }
        return hit;
    }

    /**
     * 这一代可推的动图文件；不能推返回 null。
     *
     * <p>保留这个重载是给「只要文件、不关心原因」的地方用（{@link #pushIfChanged}）。</p>
     */
    @Nullable
    static File ready(@NotNull Context context, int widgetId, int frameCount) {
        return verdict(context, widgetId, frameCount).file;
    }

    /**
     * v1.10.471：**这个组件现在是不是已经交给桌面自播了**。
     *
     * <p>{@code WidgetRender.render()} 进门先问这一句：是 → 只补推一次动图 URI，
     * 然后**直接返回，不再推任何单帧**。470 只在 tick 里拦，于是
     * {@code W32 重推单个组件 / onUpdate / 尺寸变化} 照样把位图推上去，
     * 把刚推的动图覆盖成一张静止帧（用户：「导出后不播放」）。</p>
     */
    static boolean adopted(@NotNull Context context, @NotNull WidgetConfig config, long nowMs) {
        int widgetId = config.getWidgetId();
        if (verdict(context, widgetId, config.getFrameCount()).file == null) {
            return false;
        }
        return alive(widgetId, nowMs);
    }

    /**
     * 换代/首次才推；同一代不重复推。
     *
     * @return true = 这次真的推上去了
     */
    static boolean pushIfChanged(@NotNull Context context, @Nullable AppWidgetManager manager,
                                 @NotNull WidgetConfig config) {
        int widgetId = config.getWidgetId();
        File file = ready(context, widgetId, config.getFrameCount());
        if (file == null || manager == null) {
            return false;
        }
        String key = file.getAbsolutePath() + "@" + file.length() + "#" + file.lastModified();
        synchronized (PUSHED) {
            if (key.equals(PUSHED.get(Integer.valueOf(widgetId)))) {
                return false;
            }
        }
        File dir = file.getParentFile();
        String stamp = dir != null ? dir.getName() : "";
        if (!WidgetUriPush.pushAnimUri(context, manager, config, file, stamp)) {
            Diag.warnThrottled(context, "W45", "动图推送失败 组件=" + widgetId
                    + " 大小=" + (file.length() / 1024L) + "KB（桌面拿不到就会停在上一张）");
            return false;
        }
        synchronized (PUSHED) {
            PUSHED.put(Integer.valueOf(widgetId), key);
            PUSHED_AT.put(Integer.valueOf(widgetId), Long.valueOf(SystemClock.elapsedRealtime()));
        }
        Diag.warn(context, "W45", "动图推送 组件=" + widgetId
                + " 帧=" + config.getFrameCount()
                + " 大小=" + (file.length() / 1024L) + "KB"
                + " 目录=" + stamp
                + "（这一代交给桌面自播，不再逐帧推）");
        return true;
    }

    /**
     * v1.10.470：**自播看门狗** —— 这个组件还该不该「不再逐帧推」。
     *
     * <p>判据只有一条：桌面有没有来取过 {@code anim.webp}。
     * · 取过 → 自播成立（这一代一直自播，直到换代）；
     * · 还没推过 / 推过不到 {@link #GRACE_MS} → 再等等；
     * · 推过超过宽限期、桌面一次都没取 → 判定这条路不通，回退逐帧（宁可多推帧，不许冻屏）。</p>
     */
    static boolean alive(int widgetId, long nowMs) {
        if (FrameProvider.animFetched(widgetId)) {
            return true;
        }
        synchronized (PUSHED) {
            Long at = PUSHED_AT.get(Integer.valueOf(widgetId));
            return at == null || nowMs - at.longValue() < GRACE_MS;
        }
    }

    /** 忘记「推过哪一代」（导出/换素材后调；不调也只是多等一个缓存周期）。 */
    static void forget(int widgetId) {
        synchronized (PUSHED) {
            PUSHED.remove(Integer.valueOf(widgetId));
            PUSHED_AT.remove(Integer.valueOf(widgetId));
        }
        synchronized (HITS) {
            HITS.remove(Integer.valueOf(widgetId));
        }
        synchronized (LAST_REASON) {
            LAST_REASON.remove(Integer.valueOf(widgetId));
        }
    }
}
