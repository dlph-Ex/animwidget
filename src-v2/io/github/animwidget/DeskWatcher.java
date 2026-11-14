package io.github.animwidget;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 桌面状态机（v1.10.1 重写）。
 *
 * 产品定义（用户规格 0）：
 *   APP      —— 应用自己的任意界面在前台：不限 CPU/内存，预览正常播放
 *   DESK     —— 桌面连续停留 ≥3 秒：组件开始播放，同样不受限
 *   RESTRICTED —— 其它应用 / 桌面不足 3 秒 / 息屏锁屏：立即进入，停止一切播放与解码
 *
 * 判断方式尽量「事件优先 + 少轮询」：
 *   • 应用前后台：Activity 的 onStart/onStop 直接告诉我们，**不查用量**
 *   • 息屏/锁屏：SCREEN_ON/OFF 广播（PlaybackService 注册）
 *   • 桌面/其它应用：UsageStatsManager 查询「最近 1.5 秒」的切前台事件
 *     （旧实现每次都查 10 分钟窗口，开销大）
 *   • 桌面 3 秒计时：返回精确的下一次检查时刻，不靠固定轮询撞点
 */
public final class DeskWatcher {

    @NotNull
    public static final DeskWatcher INSTANCE = new DeskWatcher();

    /** 桌面停留多久算进入 DESK 状态。 */
    private static final long DESK_DELAY_MS = 3000;
    /** 查切前台事件的时间窗（越短越省）。 */
    private static final long EVENT_WINDOW_MS = 1500;
    /** 受限状态下重新探测的间隔。 */
    private static final long RESTRICTED_POLL_MS = 600;
    /** v1.10.275：受限状态下「未知」持续这么久就当作桌面（有界兜底，避免画面停十几秒）。 */
    private static final long UNKNOWN_RESUME_MS = 2500L;
    /** DESK 状态下探测「是否离开桌面」的间隔。 */
    private static final long DESK_POLL_MS = 500;

    /**
     * v1.10.60：连续「不是桌面」的观测次数。
     * 通知栏、音量面板、systemui 这类瞬时前台会让 UsageStats 报出别的包名，
     * 以前一次就判「离开桌面」→ 回来要重新等 3 秒 → 动画卡住再从头播（日志里 2 分钟抖了 8 次）。
     * 现在要连续 3 次（≈1.5 秒）才真的判离开。
     */
    private static final int AWAY_TOLERANCE = 3;
    private static int awayStreak;
    private static volatile boolean canPlay;
    private static volatile long deskSince;
    @Nullable
    private static String launcherPackage;
    @Nullable
    private static String lastForeground;
    private static long lastQueryAt;
    /**
     * v1.10.275：**「未知」不是「离开」**。
     *
     * UsageStats 只在**切前台**那一刻吐事件；用户安静地待在桌面上时，窗口里一个事件都没有。
     * 旧代码这时拿 `lastForeground`（几分钟前那个应用）当现状 —— 于是"回到桌面后
     * UsageStats 不再吐事件"被判成"还在别的应用里"，一直受限。
     * 实测日志：08:42:07 回桌面，到 08:43:06 才恢复（**42 秒**没有任何推送）。
     */
    private static long unknownSince;
    @Nullable
    private static String lastQueryResult;
    /** v1.10.65：桌面查询挪到了后台线程，前台计数用原子类型（免锁、免脏读）。 */
    private static final java.util.concurrent.atomic.AtomicInteger appForegroundCount =
            new java.util.concurrent.atomic.AtomicInteger();

    private DeskWatcher() {
    }

    public final boolean getCanPlay() {
        return canPlay;
    }

    /**
     * 应用自己的界面进入前台/退到后台（v1.10.1）。
     * 进入时立刻把状态置为「非桌面」，并跳过后续的用量查询。
     */
    public static synchronized void setAppForeground(boolean foreground) {
        if (foreground) {
            appForegroundCount.incrementAndGet();
        } else {
            appForegroundCount.updateAndGet(v -> Math.max(v - 1, 0));
        }
        if (appForegroundCount.get() > 0) {
            deskSince = 0L;
            canPlay = false;
        } else {
            // 刚回到后台：马上重新判断（可能是桌面，也可能直接是别的应用）
            lastQueryAt = 0L;
            lastQueryResult = null;
        }
    }

    public static boolean isAppForeground() {
        return appForegroundCount.get() > 0;
    }

    public final boolean hasAccess(@NotNull Context context) {
        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
            if (appOps == null) {
                return false;
            }
            int mode;
            if (Build.VERSION.SDK_INT >= 29) {
                mode = appOps.unsafeCheckOpNoThrow("android:get_usage_stats", Process.myUid(), context.getPackageName());
            } else {
                mode = appOps.checkOpNoThrow("android:get_usage_stats", Process.myUid(), context.getPackageName());
            }
            return mode == 0;
        } catch (Throwable th) {
            return false;
        }
    }

    /**
     * 推进状态机。
     * @return 建议的下次检查间隔（毫秒）—— 由调用方 postDelayed，避免无脑轮询
     */
    public final long tick(@NotNull Context context) {
        PowerManager power = null;
        try {
            power = (PowerManager) context.getSystemService(PowerManager.class);
        } catch (Throwable ignored) {
        }
        if (power != null && !power.isInteractive()) {
            // 息屏 = 离开
            deskSince = 0L;
            canPlay = false;
            return 2000L;
        }
        if (isAppForeground()) {
            // 应用自己在前台：组件不可见，无需查用量
            deskSince = 0L;
            canPlay = false;
            return RESTRICTED_POLL_MS;
        }
        if (launcherPackage == null) {
            launcherPackage = resolveLauncher(context);
        }
        String launcher = launcherPackage;
        if (launcher == null || launcher.isEmpty() || !hasAccess(context)) {
            // 没有「使用情况访问」权限时无法判断，维持旧行为：允许播放
            canPlay = true;
            return DESK_POLL_MS;
        }
        // v1.10.275：`foregroundPackage` 返回 null = **这个时间窗里没有任何切前台事件**，
        // 不是"没有前台应用"。下面两种 null 都要按"未知"处理，而不是拿旧值当现状：
        //   ① 窗口里没事件（桌面上安静待着最常见）；
        //   ② 最近事件是**我们自己**，但 appForegroundCount 已经是 0 —— 说明那条事件过期了
        //      （我们从 onStop 退到后台，UsageStats 还没吐出桌面那条）。
        String foreground = foregroundPackage(context);
        long now = SystemClock.elapsedRealtime();
        if (foreground != null && foreground.equals(context.getPackageName()) && !isAppForeground()) {
            foreground = null;
        }
        if (foreground == null) {
            if (unknownSince == 0L) {
                unknownSince = now;
            }
            // v1.10.276：**未知时沿用「最后一次确认的前台是谁」**。
            //   · 最后一次确认是桌面 → 继续播（桌面上安静待着不会有事件，这是正常现象）；
            //   · 最后一次确认是**别的应用** → 保持受限（别在别人界面里空转）。
            // v1.10.275 只写了"已经在播就继续播"，后果实测：用户在其它应用里待了 100+ 秒，
            // 我们还在后台满帧编码（日志 O11 前台=com.psyche.kelivo 未知=102625ms play=true）。
            boolean lastIsDesk = lastForeground == null || lastForeground.equals(launcher);
            if (canPlay && lastIsDesk) {
                return DESK_POLL_MS;
            }
            if (lastForeground == null && deskSince == 0L) {
                // 冷启动、完全没有历史：维持"判断不了就允许播"的老行为，但走满 3 秒静置
                deskSince = now;
                return DESK_DELAY_MS + 30L;
            }
            // 受限中且"未知"持续够久：**大概率是用户就在桌面/桌面没吐事件**。
            // 这是有界的兜底（最多多播 ~2.5 秒），否则会出现"画面停十几秒甚至 42 秒"。
            if (lastIsDesk && now - unknownSince > UNKNOWN_RESUME_MS) {
                if (deskSince == 0L) {
                    deskSince = now;
                }
                if (now - deskSince >= DESK_DELAY_MS) {
                    canPlay = true;
                    return DESK_POLL_MS;
                }
                return Math.max(DESK_DELAY_MS - (now - deskSince) + 30L, 60L);
            }
            return RESTRICTED_POLL_MS;
        }
        unknownSince = 0L;
        lastForeground = foreground;
        if (!launcher.equals(foreground)) {
            awayStreak++;
            // 已经在播（DESK）时容忍瞬时抖动，别为了 1 次误判让动画卡 3 秒再从头播
            if (canPlay && awayStreak < AWAY_TOLERANCE) {
                return DESK_POLL_MS;
            }
            deskSince = 0L;
            canPlay = false;
            return RESTRICTED_POLL_MS;
        }
        awayStreak = 0;
        if (deskSince == 0L) {
            deskSince = now;
        }
        long elapsed = now - deskSince;
        if (elapsed < DESK_DELAY_MS) {
            canPlay = false;
            // 精确在「满 3 秒」那一刻回来检查
            return Math.max(DESK_DELAY_MS - elapsed + 30L, 60L);
        }
        canPlay = true;
        return DESK_POLL_MS;
    }

    public final void reset() {
        deskSince = 0L;
        canPlay = true;
        lastQueryAt = 0L;
        lastQueryResult = null;
    }

    /** v1.10.275：已经「未知」了多久（ms，0 = 刚拿到有效事件）——O11 里打出来定位用。 */
    public static long unknownFor() {
        long u = unknownSince;
        if (u == 0L) {
            return 0L;
        }
        return Math.max(0L, SystemClock.elapsedRealtime() - u);
    }

    @Nullable
    public final String foreground() {
        return lastForeground;
    }

    @Nullable
    public final String launcher(@NotNull Context context) {
        if (launcherPackage == null) {
            launcherPackage = resolveLauncher(context);
        }
        return launcherPackage;
    }

    private final String resolveLauncher(Context context) {
        try {
            Intent intent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME");
            ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, 65536);
            String resolved = resolveInfo == null || resolveInfo.activityInfo == null
                    ? null : resolveInfo.activityInfo.packageName;
            if (resolved != null && !resolved.isEmpty()) {
                return resolved;
            }
            List<ResolveInfo> all = context.getPackageManager().queryIntentActivities(intent, 0);
            if (all == null || all.isEmpty()) {
                return null;
            }
            ActivityInfo info = all.get(0).activityInfo;
            return info == null ? null : info.packageName;
        } catch (Throwable th) {
            return null;
        }
    }

    /** 最近 1.5 秒内最后一个切到前台的应用（25ms 内复用结果，避免多处重复查询）。 */
    @Nullable
    private final String foregroundPackage(Context context) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastQueryAt < 250L) {
            return lastQueryResult;
        }
        lastQueryAt = now;
        String result = null;
        try {
            UsageStatsManager usage = (UsageStatsManager) context.getSystemService(UsageStatsManager.class);
            if (usage == null) {
                lastQueryResult = null;
                return null;
            }
            long end = System.currentTimeMillis();
            UsageEvents events = usage.queryEvents(end - EVENT_WINDOW_MS, end);
            if (events != null) {
                UsageEvents.Event event = new UsageEvents.Event();
                while (events.hasNextEvent()) {
                    events.getNextEvent(event);
                    if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        result = event.getPackageName();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        lastQueryResult = result;
        return result;
    }
}
