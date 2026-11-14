package io.github.animwidget;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * v1.10.185：**换肤诊断（H 系列）**。
 *
 * 用户要求：①在任何页面切换深浅色都要进日志；②要能回答「在叠加页面第 N 层切的时候，
 * 第 1 层和第 2 层跟没跟」。
 *
 * 「叠加页面」在这一版里最大是 2 层：
 *   第 1 层 = 真正的那一页（极光 + 遮罩 + 卡片）；
 *   第 2 层 = 赞赏弹层（卡片本身 + 它底下**那张页面截图的模糊底**）。
 * 少跟一层，用户看到的就是「一半新配色、一半旧配色」。
 *
 * 三条日志：
 *   H01 换肤       —— 每次切换记一条（同一次被多个页面/轮询同时发现时标「同一次」），
 *                     带「谁发现的 / 来源 / 新深色 / 开了几层 / 各层是谁」；
 *   H02 换肤跟随   —— 每一层重画完各记一条：`页=… 层=i/n 延迟=…ms`；
 *                     这一条是从 {@code ThemeManager.applyTo()} 里自动打的 ——
 *                     任何页面只要走了「整页上色」就一定会留下痕迹，不用一个个改；
 *   H03 换肤未跟随 —— 1500ms 内没等到某层的 H02 就告警一次（说明那一层漏刷新）。
 */
final class ThemeSwitch {

    /** 同一次系统切换会被「配置回调 + 轮询 + 重入」多次发现，600ms 内算同一次。 */
    private static final long DEDUPE_MS = 600L;
    /** 等一层刷新的上限：超过就算没跟上。 */
    private static final long WAIT_MS = 1500L;

    private static final Handler UI = new Handler(Looper.getMainLooper());
    /** page → 发现时间（等它的 H02）。 */
    private static final Map<String, Long> PENDING = new HashMap<>();
    /** page → 最近一次跟随时间（弹层用它判断「宿主层跟没跟」）。 */
    private static final Map<String, Long> DONE_AT = new HashMap<>();

    private static Context appCtx;

    private ThemeSwitch() {
    }

    /** 当前叠了几层（赞赏/关于弹层开着就是 2）。 */
    static int layers() {
        return (RewardSheet.isOpen() || AboutSheet.isOpen()) ? 2 : 1;
    }

    /**
     * 配置回调里立刻查一次（比 500ms 盯盘快，来源写 configChange）。
     *
     * **只有深浅色真的变了才记日志** —— 字体倍率 / 语言 / 旋转也会走
     * `onConfigurationChanged`，旧版在这里无条件记一条，结果是
     * 「深色=false→false」这种既不说明问题、又会把 PENDING 重新挂上、
     * 1500ms 后误报 H03 的假信号（v1.10.185 实测日志里两条都出现了）。
     */
    static void checkNow(Activity activity, String source) {
        if (activity == null) {
            return;
        }
        armed = activity;
        boolean dark = ThemeManager.INSTANCE.isDark(activity);
        if (!baselineReady) {
            baselineDark = dark;
            baselineReady = true;
            return;
        }
        if (dark == baselineDark) {
            return;
        }
        fire(activity, source);
    }

    /** 真的换了：记 H01，并把「这一层还没跟」挂进等待表。 */
    private static void fire(Activity activity, String source) {
        boolean dark = ThemeManager.INSTANCE.isDark(activity);
        boolean wasDark = baselineDark;
        baselineDark = dark;
        String page = activity.getClass().getSimpleName();
        long now = SystemClock.uptimeMillis();
        Long done = DONE_AT.get(page);
        if (done == null || now - done > DEDUPE_MS) {
            PENDING.put(page, now);
        }
        Context app = activity.getApplicationContext();
        if (app != null) {
            appCtx = app;
        }
        try {
            int total = layers();
            StringBuilder stack = new StringBuilder();
            stack.append("第1层=").append(page);
            if (total > 1) {
                stack.append(" 第2层=").append(RewardSheet.isOpen() ? "RewardSheet" : "AboutSheet");
            }
            Diag.warn(activity, "H01", "换肤 页=" + page + " 来源=" + source
                    + " 深色=" + wasDark + "→" + dark
                    + " 层数=" + total + " " + stack);
        } catch (Throwable ignored) {
        }
        UI.removeCallbacks(WATCHDOG);
        UI.postDelayed(WATCHDOG, WAIT_MS);
    }

    /** 某一层重画完成。layer 从 1 起，of = 总层数。 */
    static void refreshed(Context ctx, String page, int layer, int of, String extra) {
        Long was = PENDING.remove(page);
        long now = SystemClock.uptimeMillis();
        DONE_AT.put(page, now);
        try {
            String wait = was == null ? "-" : (now - was) + "ms";
            Diag.warn(ctx, "H02", "换肤跟随 页=" + page + " 层=" + layer + "/" + of
                    + " 延迟=" + wait
                    + (extra == null || extra.isEmpty() ? "" : " " + extra));
        } catch (Throwable ignored) {
        }
    }

    /**
     * 「整页上色」时自动打 H02 —— 只有本次换肤确实在等这个页面时才打，
     * 所以 onCreate 里正常的 applyTo 不会刷屏。
     */
    static void notePainted(View root) {
        if (root == null || PENDING.isEmpty()) {
            return;
        }
        Context ctx = root.getContext();
        Activity act = unwrap(ctx);
        String page = act != null ? act.getClass().getSimpleName() : null;
        if (page == null || !PENDING.containsKey(page)) {
            return;
        }
        String extra;
        try {
            ThemeColors c = ThemeManager.INSTANCE.colors(act);
            extra = "深色=" + ThemeManager.INSTANCE.isDark(act) + " 背景=" + hex(c.getBackground());
        } catch (Throwable th) {
            extra = "";
        }
        refreshed(act, page, 1, layers(), extra);
    }

    /** page 是否在 withinMs 内报过跟随（弹层用它判断「宿主层跟没跟」）。 */
    static boolean followedRecently(String page, long withinMs) {
        Long at = DONE_AT.get(page);
        if (at == null) {
            return false;
        }
        return SystemClock.uptimeMillis() - at <= withinMs;
    }

    static void forget(String page) {
        PENDING.remove(page);
        DONE_AT.remove(page);
    }

    private static String hex(int color) {
        return String.format(java.util.Locale.US, "#%06X", color & 0x00FFFFFF);
    }

    private static Activity unwrap(Context ctx) {
        int guard = 0;
        while (ctx instanceof ContextWrapper && guard++ < 6) {
            if (ctx instanceof Activity) {
                return (Activity) ctx;
            }
            Context next = ((ContextWrapper) ctx).getBaseContext();
            if (next == null || next == ctx) {
                break;
            }
            ctx = next;
        }
        return ctx instanceof Activity ? (Activity) ctx : null;
    }

    // ---------------- 自己盯着：不依赖各页面自己的 500ms 轮询 ----------------

    /** 当前在前台的页面（只挂一个，onPause 就摘）。 */
    private static Activity armed;
    /** 最近一次确认过的深浅色（基线）；变了才记 H01。 */
    private static boolean baselineDark;
    private static boolean baselineReady;

    private static final Runnable POLL = new Runnable() {
        @Override
        public void run() {
            Activity a = armed;
            if (a == null) {
                return;
            }
            boolean finish;
            try {
                finish = a.isFinishing() || a.isDestroyed();
            } catch (Throwable th) {
                finish = a.isFinishing();
            }
            if (finish) {
                armed = null;
                return;
            }
            boolean dark = ThemeManager.INSTANCE.isDark(a);
            if (baselineReady && dark != baselineDark) {
                fire(a, "轮询");
            }
            UI.postDelayed(this, 500L);
        }
    };

    /** 页面进入前台：接管「模式变了没」的盯盘（任何页面都自动覆盖，不用一页页配）。 */
    static void arm(Activity activity) {
        if (activity == null) {
            return;
        }
        armed = activity;
        boolean dark = ThemeManager.INSTANCE.isDark(activity);
        if (!baselineReady) {
            baselineDark = dark;
            baselineReady = true;
        } else if (dark != baselineDark) {
            // 后台期间被切过模式：回前台补一条（来源 onResume）
            fire(activity, "onResume");
        }
        UI.removeCallbacks(POLL);
        UI.postDelayed(POLL, 500L);
    }

    static void disarm(Activity activity) {
        if (armed == activity) {
            armed = null;
            UI.removeCallbacks(POLL);
        }
    }

    private static final Runnable WATCHDOG = new Runnable() {
        @Override
        public void run() {
            if (PENDING.isEmpty()) {
                return;
            }
            long now = SystemClock.uptimeMillis();
            List<String> missing = new ArrayList<>();
            for (Map.Entry<String, Long> e : PENDING.entrySet()) {
                Long done = DONE_AT.get(e.getKey());
                boolean followed = done != null && done >= e.getValue();
                if (!followed && now - e.getValue() >= WAIT_MS - 60) {
                    missing.add(e.getKey());
                }
            }
            if (missing.isEmpty()) {
                UI.postDelayed(this, 200L);
                return;
            }
            PENDING.clear();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < missing.size(); i++) {
                if (i > 0) {
                    sb.append('、');
                }
                sb.append(missing.get(i));
            }
            try {
                Diag.warn(appCtx, "H03", "换肤未跟随 页=" + sb + " 已等=" + WAIT_MS + "ms");
            } catch (Throwable ignored) {
            }
        }
    };
}
