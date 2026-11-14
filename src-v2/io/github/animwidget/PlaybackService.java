package io.github.animwidget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kotlin.Metadata;
import kotlin.collections.CollectionsKt;
import kotlin.collections.MapsKt;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.ranges.RangesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* compiled from: PlaybackService.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��\u0080\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0010%\n\u0002\u0010\b\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n��\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0010\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0003*\u0002\u000f\u0015\u0018�� 22\u00020\u0001:\u000223B\u0005¢\u0006\u0002\u0010\u0002J\u0010\u0010\u001d\u001a\u00020\u001e2\u0006\u0010\u001f\u001a\u00020 H\u0014J\b\u0010!\u001a\u00020\"H\u0002J\b\u0010#\u001a\u00020\u001eH\u0002J\u0014\u0010$\u001a\u0004\u0018\u00010%2\b\u0010&\u001a\u0004\u0018\u00010'H\u0016J\b\u0010(\u001a\u00020\u001eH\u0016J\b\u0010)\u001a\u00020\u001eH\u0016J\"\u0010*\u001a\u00020\u00072\b\u0010&\u001a\u0004\u0018\u00010'2\u0006\u0010+\u001a\u00020\u00072\u0006\u0010,\u001a\u00020\u0007H\u0016J\u0010\u0010-\u001a\u00020\u001e2\u0006\u0010.\u001a\u00020\u0012H\u0002J\b\u0010/\u001a\u000200H\u0002J\b\u00101\u001a\u00020\u001eH\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e¢\u0006\u0002\n��R\u001a\u0010\u0005\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u000e¢\u0006\u0002\n��R\u0010\u0010\t\u001a\u0004\u0018\u00010\nX\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010\u000b\u001a\u00020\u0007X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010\f\u001a\u00020\rX\u0082\u000e¢\u0006\u0002\n��R\u0010\u0010\u000e\u001a\u00020\u000fX\u0082\u0004¢\u0006\u0004\n\u0002\u0010\u0010R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010\u0013\u001a\u00020\u0012X\u0082\u000e¢\u0006\u0002\n��R\u0010\u0010\u0014\u001a\u00020\u0015X\u0082\u0004¢\u0006\u0004\n\u0002\u0010\u0016R*\u0010\u0017\u001a\u001e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00190\u0018j\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u0019`\u001aX\u0082\u0004¢\u0006\u0002\n��R\u0010\u0010\u001b\u001a\u0004\u0018\u00010\u001cX\u0082\u000e¢\u0006\u0002\n��"}, d2 = {"Lio/github/animwidget/PlaybackService;", "Landroid/app/Service;", "()V", "cache", "Lio/github/animwidget/FrameCache;", "configs", "", "", "Lio/github/animwidget/WidgetConfig;", "handler", "Landroid/os/Handler;", "lastGeneration", "lastGenerationCheck", "", "loop", "io/github/animwidget/PlaybackService$loop$1", "Lio/github/animwidget/PlaybackService$loop$1;", "running", "", "screenOn", "screenReceiver", "io/github/animwidget/PlaybackService$screenReceiver$1", "Lio/github/animwidget/PlaybackService$screenReceiver$1;", "states", "Ljava/util/HashMap;", "Lio/github/animwidget/PlaybackService$State;", "Lkotlin/collections/HashMap;", "thread", "Landroid/os/HandlerThread;", "attachBaseContext", "", "newBase", "Landroid/content/Context;", "buildNotification", "Landroid/app/Notification;", "ensureChannel", "onBind", "Landroid/os/IBinder;", "intent", "Landroid/content/Intent;", "onCreate", "onDestroy", "onStartCommand", "flags", "startId", "reload", "force", "stopPendingIntent", "Landroid/app/PendingIntent;", "tick", "Companion", "State"})
@SourceDebugExtension({"SMAP\nPlaybackService.kt\nKotlin\n*S Kotlin\n*F\n+ 1 PlaybackService.kt\nio/github/animwidget/PlaybackService\n+ 2 Maps.kt\nkotlin/collections/MapsKt__MapsKt\n+ 3 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,276:1\n372#2,7:277\n766#3:284\n857#3,2:285\n1194#3,2:287\n1222#3,4:289\n*S KotlinDebug\n*F\n+ 1 PlaybackService.kt\nio/github/animwidget/PlaybackService\n*L\n195#1:277,7\n225#1:284\n225#1:285,2\n226#1:287,2\n226#1:289,4\n*E\n"})
/* loaded from: classes.jar:io/github/animwidget/PlaybackService.class */
public final class PlaybackService extends Service {

    @NotNull
    public static final Companion Companion = new Companion(null);

    @Nullable
    HandlerThread thread;

    @Nullable
    Handler handler;

    @Nullable
    FrameCache cache;
    boolean running;
    long lastGenerationCheck;
    // v1.10.1：受限状态标记 + 实测帧率统计（每 3 秒进日志，不逐帧刷）
    boolean restricted;
    long fpsWindowStart;
    int fpsWindowFrames;
    /** v1.10.65：W10 窗口内累计的「一帧渲染耗时」，用来看还剩多少预算（30ms 一拍）。 */
    long fpsWindowCost;
    /**
     * v1.10.79：**每个组件单独的统计窗口**。
     * 之前 W10 是「全体渲染次数 / 时间」，目标帧率却是单个组件的 ——
     * 两个组件各 24fps 时会显示「实测 46.9fps（目标 24）」，看起来像 bug，其实是两份相加。
     */
    private static final class Win {
        long startAt;
        int frames;
        long cost;
        int targetFps;
        long intervalMs;
        /** 本窗口内该组件被跳过（没抢到这一拍）的次数 */
        int starved;
        /**
         * v1.10.348：把原来那个 `重帧=` 拆成两个真实的量。
         * 345/347 的 `重帧` 在"强制往前一格"之后会**每拍都成立**（实测 152 次/152 帧），
         * 读起来像"全是重复帧"，其实画面一帧都没重复 —— 是无用的绿灯变红灯。
         *   · 补帧：墙钟没前进（或倒退），我们强制补了一格 → 保证画面不停
         *   · 掉帧：墙钟已经前进 ≥2 格（我们落后了）→ 真正会看到的"跳"
         */
        int fixed;
        int skipped;
        /** v1.10.126：逐帧推送间隔采样（问「抖动到底多少」时看 p50/p95） */
        final Gaps gaps = new Gaps();
    }

    /**
     * v1.10.126：**逐帧推送间隔**的环形采样（256 个 int，零分配）。
     *
     * 为什么单独采：3 秒窗口的「实测 fps」只能说明均值对不对，说明不了单帧有没有顿一下。
     * 桌面是 60Hz vsync，30fps 内容正常是「每帧占 2 个 vsync」（33.3ms）；
     * 只要某一拍迟到超过 ~8ms，就可能掉成一个 vsync 的顿挫 —— 这个计数才是有意义的抖动指标。
     */
    private static final class Gaps {
        private final int[] buf = new int[256];
        private int count;
        private int pos;
        private long lastAt;

        void add(long now) {
            if (lastAt == 0L) {
                lastAt = now;
                return;
            }
            long gap = now - lastAt;
            lastAt = now;
            if (gap < 0L || gap > 2000L) {
                return;   // 秒表跳变/长时间没播：不计入
            }
            buf[pos] = (int) gap;
            pos = (pos + 1) & 255;
            if (count < 256) {
                count++;
            }
        }

        /** @return 「 帧隔 p50=33ms p95=34ms 最大=41ms 迟到>8ms=2次」，没样本返回空串 */
        @NotNull
        String report(int budgetMs) {
            if (count < 5) {
                this.count = 0;
                this.pos = 0;
                return "";
            }
            int n = this.count;
            int[] copy = new int[n];
            for (int i = 0; i < n; i++) {
                copy[i] = buf[i];
            }
            java.util.Arrays.sort(copy);
            int p50 = copy[(int) ((n - 1) * 0.50f)];
            int p95 = copy[(int) ((n - 1) * 0.95f)];
            int max = copy[n - 1];
            int late = 0;
            int threshold = Math.max(budgetMs, 1) + 8;
            for (int i = 0; i < n; i++) {
                if (copy[i] > threshold) {
                    late++;
                }
            }
            this.count = 0;
            this.pos = 0;
            return " 帧隔 p50=" + p50 + "ms p95=" + p95 + "ms 最大=" + max + "ms"
                    + " 迟到>" + (threshold - Math.max(budgetMs, 1)) + "ms=" + late + "次";
        }
    }

    @NotNull
    private final HashMap<Integer, Win> wins = new HashMap<>();

    @NotNull
    private final Win win(int widgetId) {
        Integer key = Integer.valueOf(widgetId);
        Win w = this.wins.get(key);
        if (w == null) {
            w = new Win();
            // v1.10.81：**创建即开始计时**。
            // 以前 startAt 是等到「合计窗口到 3 秒」那一刻才第一次赋值，而 frames 从创建就在累加 ——
            // 首个 W10 窗口实际统计了约 6 秒的帧、却按 3 秒算，于是两个组件各自显示 145 帧
            // （合计才 144），看起来像「每个组件都翻倍」。
            w.startAt = SystemClock.elapsedRealtime();
            this.wins.put(key, w);
        }
        return w;
    }

    // ------------------------------------------------------------------ v1.10.78 节拍诊断
    /** tick() 真正跑起来的次数。 */
    private long diagTicks;
    /** 往队列里 post 过多少次（和 tick 次数对不上 = 重复排程）。 */
    private long diagPosts;
    /** 相邻两次 tick 的间隔累计，用来算「平均实际间隔」。 */
    private long diagGapSum;
    private int diagGapCount;
    /** v1.10.335：请求休眠的累计。**它 ≈ 间隔 − 本拍渲染耗时，不是迟到**（v1.10.337 更正）。 */
    private long diagSleepSum;
    /** v1.10.337：真正的唤醒迟到 —— 上一拍请求的时刻 vs 这一拍真正跑起来的时刻。 */
    private long diagSleepWakeAt;
    private long diagLatenessSum;
    private int diagLatenessN;
    private long diagLastTickAt;
    /** 视图尺寸变更 / 其它模块手动推帧的次数（对比用）。 */
    private int diagRenderFromProvider;

    /**
     * v1.10.341：当前有多少个组件在推帧（画布预算要用它分摊桌面的解码能力）。
     * 由 tick() 每拍更新，WidgetRender.budgetFor 只读 —— 这样"拖大以后本该多大"
     * 和"桌面上同时有几个在动"是同一个数，不会各算一套。
     */
    private static volatile int playingCount = 1;

    public static int animPlayingCount() {
        return playingCount;
    }

    /** v1.10.66：一拍的真实间隔（ms），喂给 W10 的「预算」。 */
    private long lastIntervalMs = 33L;
    /** v1.10.333：本拍内最多允许渲染多久（= 帧间隔，10~24ms）。W23 用它解释「一拍能渲几个」。 */
    private long lastRenderBudgetMs = 10L;
    /** v1.10.66：本窗口内「进入受限」的次数 —— 用来解释丢的帧去哪了。 */
    private int fpsWindowPauses;
    /**
     * v1.10.338：本窗口里有多少拍「撞上预热」。
     * 外部分析 P2-3 的要求：预热线程提到 DEFAULT 后可能反挤 launcher，
     * 必须能对比「预热期 vs 空闲期」的 推最慢 / 桌面取用率。没有这个计数就没法分层对比。
     */
    private int fpsWindowPrewarmTicks;
    // v1.10.1：状态检查自带节奏，不跟着帧率翻倍查用量
    long deskNextCheckAt;
    volatile int lastTargetFps;
    /** v1.10.53：每个组件的推帧上限（×10），以及上次记日志的值（避免刷屏）。 */
    private final HashMap<Integer, Integer> pushCapLogged = new HashMap<>();

    @NotNull
    private static final String ACTION_START = "io.github.animwidget.action.START";

    @NotNull
    private static final String ACTION_STOP = "io.github.animwidget.action.STOP";

    @NotNull
    private static final String CHANNEL_ID = "animated_widget";
    private static final int NOTIFICATION_ID = 1001;
    static volatile boolean isRunning;

    /**
     * v1.10.123：每个组件「最近一次真正推上桌面的帧号」。
     * 尺寸变更时用它接上当前画面 —— 以前 W22 一律重推第 0 帧，
     * 用户看到的就是「拖大组件 → 视频跳回开头 → 再跳回来」，很像「突然加速」。
     */
    private static final java.util.concurrent.ConcurrentHashMap<Integer, Integer> LAST_INDEX =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** 没有记录（服务没跑过）就返回 0。 */
    static int lastIndex(int widgetId) {
        Integer v = LAST_INDEX.get(Integer.valueOf(widgetId));
        return v == null ? 0 : v.intValue();
    }
    private boolean screenOn = true;

    @NotNull
    private Map<Integer, WidgetConfig> configs = new HashMap();

    @NotNull
    private final HashMap<Integer, State> states = new HashMap<>();
    private int lastGeneration = -1;

    @NotNull
    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() { // from class: io.github.animwidget.PlaybackService$screenReceiver$1
        /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue
        java.lang.NullPointerException: Cannot invoke "java.util.List.iterator()" because the return value of "jadx.core.dex.visitors.regions.SwitchOverStringVisitor$SwitchData.getNewCases()" is null
        	at jadx.core.dex.visitors.regions.SwitchOverStringVisitor.restoreSwitchOverString(SwitchOverStringVisitor.java:109)
        	at jadx.core.dex.visitors.regions.SwitchOverStringVisitor.visitRegion(SwitchOverStringVisitor.java:66)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseIterativeStepInternal(DepthRegionTraversal.java:77)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseIterativeStepInternal(DepthRegionTraversal.java:82)
         */
        @Override // android.content.BroadcastReceiver
        public void onReceive(@Nullable Context context, @Nullable Intent intent) {
            String action = intent != null ? intent.getAction() : null;
            if (action != null) {
                switch (action.hashCode()) {
                    case -2128145023:
                        if (action.equals("android.intent.action.SCREEN_OFF")) {
                            PlaybackService.this.screenOn = false;
                            break;
                        }
                        break;
                    case -1454123155:
                        if (action.equals("android.intent.action.SCREEN_ON")) {
                            PlaybackService.this.screenOn = true;
                            break;
                        }
                        break;
                }
            }
        }
    };

    @NotNull
    private final Runnable loop = new Runnable() { // from class: io.github.animwidget.PlaybackService$loop$1
        @Override // java.lang.Runnable
        public void run() {
            if (PlaybackService.this.running) {
                try {
                    if (PlaybackService.this.screenOn) {
                        PlaybackService.this.tick();
                    } else {
                        Handler handler = PlaybackService.this.handler;
                        if (handler != null) {
                            handler.postDelayed(this, 500L);
                        }
                    }
                } catch (Throwable th) {
                    // v1.8.11：这里以前静默吞掉并 100ms 重试 —— 持续异常就是
                    // 「组件冻住 + 死循环烧电」，还没任何记录。补编号 + 崩溃日志。
                    Diag.warnThrottled(PlaybackService.this, "W01", "推帧循环异常: "
                            + th.getClass().getSimpleName()
                            + (th.getMessage() != null ? "/" + th.getMessage() : ""));
                    CrashLog.save(PlaybackService.this, Thread.currentThread(), th);
                    Handler handler2 = PlaybackService.this.handler;
                    if (handler2 != null) {
                        handler2.postDelayed(this, 100L);
                    }
                }
            }
        }
    };

    /* compiled from: PlaybackService.kt */
    @Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��.\n\u0002\u0018\u0002\n\u0002\u0010��\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\b\n��\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u0002\n��\n\u0002\u0018\u0002\n��\b\u0086\u0003\u0018��2\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J\u000e\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u0010J\u000e\u0010\u0011\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u0010R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T¢\u0006\u0002\n��R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T¢\u0006\u0002\n��R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T¢\u0006\u0002\n��R\u000e\u0010\u0007\u001a\u00020\bX\u0082T¢\u0006\u0002\n��R\u001e\u0010\u000b\u001a\u00020\n2\u0006\u0010\t\u001a\u00020\n@BX\u0086\u000e¢\u0006\b\n��\u001a\u0004\b\u000b\u0010\f"}, d2 = {"Lio/github/animwidget/PlaybackService$Companion;", "", "()V", "ACTION_START", "", "ACTION_STOP", "CHANNEL_ID", "NOTIFICATION_ID", "", "<set-?>", "", "isRunning", "()Z", "start", "", "context", "Landroid/content/Context;", "stop"})
    /* loaded from: classes.jar:io/github/animwidget/PlaybackService$Companion.class */
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker $constructor_marker) {
            this();
        }

        private Companion() {
        }

        public final boolean isRunning() {
            return PlaybackService.isRunning;
        }

        public final void start(@NotNull Context context) {
            Intrinsics.checkNotNullParameter(context, "context");
            if (context.getSharedPreferences("settings", 0).getBoolean("all_stopped", false)) {
                return;
            }
            try {
                context.startForegroundService(new Intent(context, (Class<?>) PlaybackService.class).setAction(PlaybackService.ACTION_START));
            } catch (Throwable th) {
            }
        }

        public final void stop(@NotNull Context context) {
            Intrinsics.checkNotNullParameter(context, "context");
            try {
                context.stopService(new Intent(context, (Class<?>) PlaybackService.class));
            } catch (Throwable th) {
            }
        }
    }

    /* compiled from: PlaybackService.kt */
    @Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��$\n\u0002\u0018\u0002\n\u0002\u0010��\n��\n\u0002\u0010\b\n��\n\u0002\u0010\t\n\u0002\b\r\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u000e\b\u0082\b\u0018��2\u00020\u0001B\u0019\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005¢\u0006\u0002\u0010\u0006J\t\u0010\u000f\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0010\u001a\u00020\u0005HÆ\u0003J\u001d\u0010\u0011\u001a\u00020��2\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005HÆ\u0001J\u0013\u0010\u0012\u001a\u00020\u00132\b\u0010\u0014\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u0010\u0015\u001a\u00020\u0003HÖ\u0001J\t\u0010\u0016\u001a\u00020\u0017HÖ\u0001R\u001a\u0010\u0002\u001a\u00020\u0003X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b\u0007\u0010\b\"\u0004\b\t\u0010\nR\u001a\u0010\u0004\u001a\u00020\u0005X\u0086\u000e¢\u0006\u000e\n��\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000e"}, d2 = {"Lio/github/animwidget/PlaybackService$State;", "", "index", "", "nextAt", "", "(IJ)V", "getIndex", "()I", "setIndex", "(I)V", "getNextAt", "()J", "setNextAt", "(J)V", "component1", "component2", "copy", "equals", "", "other", "hashCode", "toString", ""})
    /* loaded from: classes.jar:io/github/animwidget/PlaybackService$State.class */
    private static final class State {
        private int index;
        private long nextAt;
        /** v1.10.20：本组件开始播放的时刻（墙钟基准，用于按时间取帧）。 */
        private long startAt;
        /** 缓存的帧率×10（避免每帧读 SharedPreferences）。 */
        private int fpsX10;

        public final int component1() {
            return this.index;
        }

        public final long component2() {
            return this.nextAt;
        }

        @NotNull
        public final State copy(int index, long nextAt) {
            return new State(index, nextAt);
        }

        public static /* synthetic */ State copy$default(State state, int i, long j, int i2, Object obj) {
            if ((i2 & 1) != 0) {
                i = state.index;
            }
            if ((i2 & 2) != 0) {
                j = state.nextAt;
            }
            return state.copy(i, j);
        }

        @NotNull
        public String toString() {
            return "State(index=" + this.index + ", nextAt=" + this.nextAt + ')';
        }

        public int hashCode() {
            int result = Integer.hashCode(this.index);
            return (result * 31) + Long.hashCode(this.nextAt);
        }

        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof State)) {
                return false;
            }
            State state = (State) other;
            return this.index == state.index && this.nextAt == state.nextAt;
        }

        public State() {
            this(0, 0L, 3, null);
        }

        public State(int index, long nextAt) {
            this.index = index;
            this.nextAt = nextAt;
        }

        public State(int index, long nextAt, long startAt) {
            this.index = index;
            this.nextAt = nextAt;
            this.startAt = startAt;
        }

        public /* synthetic */ State(int i, long j, int i2, DefaultConstructorMarker defaultConstructorMarker) {
            this((i2 & 1) != 0 ? 0 : i, (i2 & 2) != 0 ? 0L : j);
        }

        public final int getIndex() {
            return this.index;
        }

        public final void setIndex(int i) {
            this.index = i;
        }

        public final long getNextAt() {
            return this.nextAt;
        }

        public final void setNextAt(long j) {
            this.nextAt = j;
        }
    }

    @Override // android.app.Service, android.content.ContextWrapper
    protected void attachBaseContext(@NotNull Context newBase) {
        Intrinsics.checkNotNullParameter(newBase, "newBase");
        super.attachBaseContext(LocaleHelper.INSTANCE.wrap(newBase));
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        // v1.8.11：小部件推帧服务可能在「主界面从未打开」的纯净进程里启动，
        // 崩溃钩子没装 → 任何异常都不留 last_error.txt。这里补装（幂等）。
        CrashLog.install(this);
        // A04：推帧服务拉起（v1.8.13）
        Diag.warn(this, "A04", "推帧服务启动");
        // v1.10.263：**清孤儿推帧目录**。实测 W33 报 `磁盘帧=123MB` —— 反复建/删组件、反复拖尺寸，
        // `push/w<id>/` 只涨不降（删组件时服务没跑就留下来了）。这里在服务启动时对一次账：
        // 只保留「当前真正存在的组件 id」的目录。文件操作放后台线程，不堵启动。
        final Context appCtx = getApplicationContext();
        try {
            new Thread(new Runnable() { // from class: io.github.animwidget.PlaybackService$prune
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        // v1.10.492：孤儿清理必须把多格组件也算成「活着」，否则它的缓存会被删光
                        int[] ids = MultiCell.allIds(appCtx);
                        java.util.HashSet<Integer> alive = new java.util.HashSet<>();
                        if (ids != null) {
                            for (int id : ids) {
                                alive.add(Integer.valueOf(id));
                            }
                        }
                        long[] r = WidgetUriPush.pruneOrphans(appCtx, alive);
                        if (r[0] > 0L) {
                            Diag.warn(appCtx, "W33", "清理孤儿推帧目录 目录数=" + r[0]
                                    + " 回收=" + (r[1] / 1048576L) + "MB");
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }, "prune-push").start();
        } catch (Throwable ignored) {
        }
        Companion companion = Companion;
        isRunning = true;
        ensureChannel();
        HandlerThread worker = new HandlerThread("widget-loop", -4);
        worker.start();
        this.thread = worker;
        this.handler = new Handler(worker.getLooper());
        int heapKb = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // v1.9.8：缓存预算 heap/6 → heap/4 —— 命中率更高，桌面逐帧播放更少重合成
        this.cache = new FrameCache(heapKb / 4);
        IntentFilter $this$onCreate_u24lambda_u240 = new IntentFilter();
        $this$onCreate_u24lambda_u240.addAction("android.intent.action.SCREEN_ON");
        $this$onCreate_u24lambda_u240.addAction("android.intent.action.SCREEN_OFF");
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(this.screenReceiver, $this$onCreate_u24lambda_u240, 4);
        } else {
            registerReceiver(this.screenReceiver, $this$onCreate_u24lambda_u240);
        }
    }

    @Override // android.app.Service
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        // v1.10.78：被拉起几次、当时是否已在跑 —— 重复 start 也是双节拍的可疑来源
        Diag.warnThrottled(this, "W21", "onStartCommand action="
                + (intent != null ? intent.getAction() : "null") + " running=" + this.running);
        if (Intrinsics.areEqual(intent != null ? intent.getAction() : null, ACTION_STOP)) {
            getSharedPreferences("settings", 0).edit().putBoolean("all_stopped", true).apply();
            stopSelf();
            return 2;
        }
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, 1073741824);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        if (!this.running) {
            this.running = true;
            reload(true);
            Handler handler = this.handler;
            if (handler != null) {
                handler.post(this.loop);
                return 2;
            }
            return 2;
        }
        return 2;
    }

    @Override // android.app.Service
    public void onDestroy() {
        this.running = false;
        Companion companion = Companion;
        isRunning = false;
        Handler handler = this.handler;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        try {
            unregisterReceiver(this.screenReceiver);
        } catch (Throwable th) {
        }
        HandlerThread handlerThread = this.thread;
        if (handlerThread != null) {
            handlerThread.quitSafely();
        }
        this.thread = null;
        this.handler = null;
        this.idCache = null;
        java.util.concurrent.ExecutorService exec = this.deskExec;
        if (exec != null) {
            try {
                exec.shutdownNow();
            } catch (Throwable ignored) {
            }
            this.deskExec = null;
        }
        FrameCache frameCache = this.cache;
        if (frameCache != null) {
            frameCache.clear();
        }
        this.cache = null;
        super.onDestroy();
    }

    @Override // android.app.Service
    @Nullable
    public IBinder onBind(@Nullable Intent intent) {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void tick() {
        Object obj;
        // v1.10.1：状态机返回「下次该在什么时候回来看」，不再固定 600ms 轮询
        long nowCheck = SystemClock.elapsedRealtime();
        // v1.10.78：节拍诊断 —— 记下 tick 次数与相邻间隔
        this.diagTicks++;
        // v1.10.80：只有「上一拍也是可播状态」时才算节拍 —— 受限状态是按 200ms 轮询的，
        // 把它算进平均间隔会让「均隔」永远偏大，W20 的判定也就失效了。
        if (!this.restricted && this.diagLastTickAt > 0L) {
            this.diagGapSum += nowCheck - this.diagLastTickAt;
            this.diagGapCount++;
        }
        this.diagLastTickAt = nowCheck;
        // v1.10.337：真正的唤醒迟到。v1.10.335 我错把「均隔 − 休眠均」当成迟到 ——
        // 那个差值是**本拍的渲染耗时**（实测 20−14=6ms ≈ 渲/拍 3.00×单帧 1.8ms，正好对上）。
        if (!this.restricted && this.diagSleepWakeAt > 0L) {
            long late = nowCheck - this.diagSleepWakeAt;
            if (late >= 0L && late < 500L) {
                this.diagLatenessSum += late;
                this.diagLatenessN++;
            }
        }
        this.diagSleepWakeAt = 0L;
        if (WidgetPrewarm.activeBakes() > 0) {
            this.fpsWindowPrewarmTicks++;
        }
        refreshProbe(nowCheck);   // v1.10.276：播放位置快照（W33 用，每秒一次）
        if (nowCheck >= this.deskNextCheckAt) {
            // v1.10.69：UsageStats 查询可能要 5~20ms，放在推帧线程上就等于每 500ms 吃掉
            // 半拍到一整拍（用户能看到的丢帧）。统一交给常驻后台线程，推帧线程只读
            // DeskWatcher 的 volatile 结果（canPlay）。
            if (!this.deskQueryBusy) {
                this.deskQueryBusy = true;
                final Context app = getApplicationContext();
                try {
                    ensureDeskExec();
                    final java.util.concurrent.ExecutorService exec = this.deskExec;
                    if (exec != null) {
                        exec.execute(() -> {
                            long suggest = 500L;
                            try {
                                suggest = DeskWatcher.INSTANCE.tick(app);
                            } catch (Throwable th) {
                                Diag.warnThrottled(app, "W19", "桌面状态查询异常: " + th.getClass().getSimpleName());
                            } finally {
                                this.deskQuerySuggest = Math.max(suggest, 200L);
                                this.deskQueryBusy = false;
                            }
                        });
                    } else {
                        this.deskQueryBusy = false;
                    }
                } catch (Throwable th) {
                    this.deskQueryBusy = false;
                }
            }
            this.deskNextCheckAt = nowCheck + Math.max(this.deskQuerySuggest, 200L);
        }
        if (!DeskWatcher.INSTANCE.getCanPlay()) {
            enterRestricted();
            Handler restrictedHandler = this.handler;
            if (restrictedHandler != null) {
                this.diagPosts++;   // v1.10.78
                restrictedHandler.postDelayed(this.loop,
                        Math.max(this.deskNextCheckAt - SystemClock.elapsedRealtime(), 80L));
            }
            return;
        }
        leaveRestricted();
        long now = SystemClock.elapsedRealtime();
        if (now - this.lastGenerationCheck >= 500) {
            this.lastGenerationCheck = now;
            if (WidgetStore.INSTANCE.generation(this) != this.lastGeneration) {
                reload(false);
            }
        }
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        int[] ids = widgetIdsCached();   // v1.10.62：1.5s 内复用，别每拍打一次 binder
        if (ids.length == 0) {
            this.running = false;
            stopSelf();
            return;
        }
        // v1.10.341：先数一下有几个组件真的要推帧（多帧才占桌面解码预算；静态图只推一次）
        try {
            int playing = 0;
            for (int id : ids) {
                WidgetConfig c = this.configs.get(Integer.valueOf(id));
                if (c != null && c.getFrameCount() > 1) {
                    playing++;
                }
            }
            playingCount = Math.max(playing, 1);
        } catch (Throwable ignored) {
        }
        long nextAt = now + 1000;
        // v1.10.52：**一拍只渲染一个组件**。
        // 原来同一拍里把「所有到期组件」连着渲染 —— 用户有 4 个组件时，
        // 主线程会被一口气堵住 3~4 个渲染周期（每个 ~30-40ms，含合成 + Binder 推 RemoteViews），
        // launcher 收不到更新、本页预览的贴图也只能排队 → 两边都表现为「明显掉帧」。
        // 现在一个组件渲完就把主线程让出去，剩下 4ms 后再来（nextAt 已过期 → delay 被压到 4ms），
        // 总吞吐不变，但主线程不再被长段占用。
        // v1.10.259（反馈「2 个组件」实测）：**一拍只渲一个**在单组件时没问题，
        // 两个组件时却是硬上限：tick 实测只有 13ms 一次 → 总渲染 ~77 次/秒，
        // 而两个组件各自需要 45 + 33.3 = 78 次/秒 → 两边都只能跑到 ~94%
        // （日志：196 从 44.7 → 42.2fps，195 33.3 → 31.3fps，命中都是 100%）。
        // 现在改成「一拍内可以连着渲染多个，但总耗时不超过 10ms」——
        // 单帧成本 2ms 时两个组件一起渲完仍只用 4ms，远低于 22ms 预算；
        // 一旦某一帧真的贵（合成 120ms 那种），第一帧就超预算，后面的照旧让给下一拍。
        final long loopStart = SystemClock.elapsedRealtime();
        // v1.10.333：**一拍的渲染上限 = 一个帧间隔**（原来写死 10ms）。
        //
        // 实测（21:12:44~:56，三个 1:1/50fps 组件）：渲染 350 次 / 3012ms = 116 次/秒，
        // 每个组件只有 37~41fps，而三个组件各需要 50fps = 150 次/秒；
        // 同时「均隔=10ms、每拍只渲 1.2 个、单帧=3.2~4.2ms」—— 一拍里有 6ms 在空转。
        // 原因就是这 10ms：三个组件同时到期时第三个必然被推迟到下一拍，于是三者的相位
        // 永久错开，每 20ms 的循环里要付三次 Handler 延迟（实测 p50=22~26ms 而不是 20ms）。
        // 下限 10ms 保留（低帧率组件不必放开），上限 24ms 防止一次渲染意外贵时堵太久。
        final long renderBudgetMs = Math.max(10L, Math.min(tightestIntervalMs(), 24L));
        this.lastRenderBudgetMs = renderBudgetMs;
        int rendersThisTick = 0;
        // v1.10.349（实测抓到）：三个组件**同一时刻**推时，桌面的取用率是 152/152/**123** ——
        // 每次都是最后一个（推帧顺序里的第 3 个）被桌面合并掉约 17%，用户看到的就是
        // "前两个基本流畅，第 3 个帧率明显不对"。这不是它自己慢，是它总排在最后挨刀。
        // 治法：把这个组件的相位按桌面顺序均匀铺开（0, 6.7, 13.3ms）。
        // 每个组件仍各有自己的 20ms 绝对栅格，迟到不累积，所以不会回到 335 之前"24ms/41.7fps"。
        final int phaseN = Math.max(ids.length, 1);
        int phaseIdx = 0;
        for (int widgetId : ids) {
            WidgetConfig config = this.configs.get(Integer.valueOf(widgetId));
            // v1.10.13：覆盖层能不能起来不确定，不能因此把组件停成 0fps ——
            // 开着覆盖层时软件推帧照旧（用户看到 0fps = 覆盖层没起来 + 推帧被跳过）
            if (config != null && config.getFrameCount() > 0) {
                Map $this$getOrPut$iv = this.states;
                Integer numValueOf = Integer.valueOf(widgetId);
                Object value$iv = $this$getOrPut$iv.get(numValueOf);
                if (value$iv == null) {
                    State state = new State(0, 0L, 3, null);
                    $this$getOrPut$iv.put(numValueOf, state);
                    obj = state;
                } else {
                    obj = value$iv;
                }
                State state2 = (State) obj;
                final int myPhase = phaseIdx;
                if (state2.getNextAt() == 0) {
                    // 第 i 个组件延后 i×(间隔/N)：三组件 50fps 时是 0 / 6.7 / 13.3ms
                    long iv = Math.max(this.lastIntervalMs, 8L);
                    state2.setNextAt(now + iv * myPhase / phaseN);
                }
                // v1.10.470（#4 第 2 步）：**这一代已经有动图文件 → 桌面自己播，不再逐帧推**。
                // 每个心跳都问一次「这一代推过没有」（换代后 ≤1s 自动推新的一版，见 pushIfChanged
                // 的 路径+大小+mtime 记忆）；推过就把 nextAt 钉死，这个组件从此不再占推帧预算。
                // 文件不存在 / 超上限（AnimPush.MAX_BYTES）→ 什么都不做，老路径照旧。
                if (config.getFrameCount() > 1 && AnimPush.adopted(this, config, now)) {
                    // 看门狗：桌面一次都没来取过 anim.webp → 这条路在这台设备上不通，回退逐帧
                    //（宁可多推帧，不许冻在上一张）。取过就一直自播到换代。
                    if (AnimPush.alive(widgetId, now)) {
                        AnimPush.pushIfChanged(this, manager, config);
                        state2.setNextAt(Long.MAX_VALUE);
                        phaseIdx++;
                        continue;
                    }
                    Diag.warnThrottled(this, "W45", "动图没人取，回退逐帧 组件=" + widgetId
                            + "（桌面可能不支持动图 WebP）");
                }
                // v1.10.473：**恢复闸**。上一拍把它交给了自播（nextAt 被钉成 MAX），
                // 这一拍判定又不成立了（用户关掉「URI 推送」/ 文件被删 / 看门狗判死）——
                // 必须把 nextAt 拉回「现在」，否则它永远不会再被推帧，只能等进程重启。
                if (state2.getNextAt() == Long.MAX_VALUE
                        && !AnimPush.adopted(this, config, now)) {
                    state2.setNextAt(now);
                    Diag.warnThrottled(this, "W45", "自播结束，恢复逐帧推送 组件=" + widgetId);
                }
                boolean canRender = rendersThisTick == 0
                        || (SystemClock.elapsedRealtime() - loopStart < renderBudgetMs);
                if (now >= state2.getNextAt() && canRender) {
                    rendersThisTick++;
                    if (config.getFrameCount() <= 1) {
                        // v1.10.57：静态组件也记一下目标帧率，否则 W10 里一直写「目标 0」误导排查
                        this.lastTargetFps = 1;
                        if (state2.getIndex() == 0) {
                            Intrinsics.checkNotNull(manager);
                            WidgetRender.INSTANCE.render(this, manager, config, 0, this.cache);
                            state2.setIndex(1);
                        }
                        state2.setNextAt(Long.MAX_VALUE);
                    } else {
                        Intrinsics.checkNotNull(manager);
                        // v1.10.20：帧号按「墙钟」算 —— 推帧能力不足（实测 ~24fps）时跳过中间帧，
                        // 而不是把整段放慢。这样 50fps 素材的总时长仍然与源文件一致。
                        if (state2.fpsX10 == 0) {
                            state2.fpsX10 = WidgetStore.INSTANCE.fpsX10(this, config.getWidgetId(), config.getFps());
                            this.lastTargetFps = Math.max(Math.round(state2.fpsX10 / 10.0f), 1);
                        }
                        // v1.10.53：推帧带宽预算 —— 每帧要过一次 Binder（位图 = 渲染尺寸×4 字节，
                        // 22 万像素的上限 ≈ 880KB/帧），再加上 launcher 的纹理上传。
                        // 50fps × 880KB ≈ 44MB/s 会把 launcher 的 RenderThread 打满，
                        // 整个桌面和前台 App 一起掉帧（用户遇到的「2K60 组件连锁」）。
                        // 这里把「每秒字节数」限在 28MB/s：大组件最多 ~31fps，小组件不受影响。
                        int trueFpsX10 = Math.max(state2.fpsX10, 10);
                        int pushFpsX10 = Math.min(trueFpsX10,
                                WidgetRender.INSTANCE.pushFpsCapX10(this, manager, config));
                        notePushCap(config.getWidgetId(), trueFpsX10, pushFpsX10);
                        long interval = Math.max(10000L / Math.max(pushFpsX10, 10), 8L);
                        this.lastIntervalMs = interval;   // v1.10.66：W10 里报真实预算
                        if (state2.startAt == 0L) {
                            state2.startAt = SystemClock.elapsedRealtime();
                        }
                        long elapsed = SystemClock.elapsedRealtime() - state2.startAt;
                        // 帧号仍按**真实帧率**取：限流只少推几帧，动画总时长不变（v1.10.20 的设计）
                        // v1.10.347（自审 346 的漏洞）：**帧号只许往前走**。
                        //
                        // 墙钟算出来的帧号 `wallDue` 有三种病态：等于上一帧（重帧 = 画面停一拍，
                        // 实测 12~28 次/3 秒）、落到上一帧之前（倒退 = 画面往回跳）、
                        // 或者绕圈（289→0，这是正常的）。
                        // 346 只处理了"等于"，漏了"倒退"；而且它把 dupFrame 放到强制 +1 **之后**统计，
                        // 于是 `重帧=` 永远读到 0 —— 我自己刚加的指标当场失效。
                        // 现在统一成"前向距离"：
                        //   diff = (wall - last + 帧数) % 帧数
                        //   · diff == 0（重帧）或 diff > 帧数/2（倒退）→ 强制走 1 帧；
                        //   · 其余（1..帧数/2，含正常绕圈）→ 按墙钟走。
                        // 统计仍在**墙钟的结论**上记，所以 `重帧=` 依然能反映真实情况。
                        int countAll = config.getFrameCount();
                        int wallDue = (int) (((elapsed * trueFpsX10) / 10000L) % countAll);
                        int lastIdx = state2.getIndex();
                        int fwd = ((wallDue - lastIdx) % countAll + countAll) % countAll;
                        final boolean forced = (fwd == 0 || fwd > countAll / 2);
                        final int skippedFrames = forced ? 0 : (fwd - 1);
                        if (forced) {
                            fwd = 1;
                        }
                        int due = (lastIdx + fwd) % countAll;
                        this.lastRenderedId = config.getWidgetId();
                        long cost = WidgetRender.INSTANCE.render(this, manager, config, due, this.cache);
                        if (cost > 0L) {
                            this.fpsWindowCost += cost;
                        }
                        // v1.10.79：按组件分别累计
                        Win win = win(config.getWidgetId());
                        win.frames++;
                        if (forced) {
                            win.fixed++;
                        }
                        if (skippedFrames > 0) {
                            win.skipped++;
                        }
                        if (cost > 0L) {
                            win.cost += cost;
                        }
                        // v1.10.259：**目标帧率必须取自本组件的 state**。
                        // 原来写 `this.lastTargetFps`，而这个字段只在 `state2.fpsX10 == 0`
                        // 的那一次（首次）才被赋值 —— 多组件时它一直是「另一个组件」的帧率。
                        // 实测：195 是 33.3fps 的动图，W10 却写「目标 45」（那是 196 的），
                        // 于是看起来像「只跑 31fps 远远不够」，其实已经 94% 达标。
                        win.targetFps = Math.max(Math.round(state2.fpsX10 / 10.0f), 1);
                        this.lastTargetFps = win.targetFps;
                        win.intervalMs = interval;
                        win.gaps.add(SystemClock.elapsedRealtime());
                        this.fpsWindowFrames++;
                        state2.setIndex(due);
                        LAST_INDEX.put(Integer.valueOf(config.getWidgetId()), Integer.valueOf(due));
                        // v1.10.335：**固定速率排程**（外部分析 P1，实测数字对得上）。
                        //
                        // 现场（21:51:28~:40，三个 50fps 组件，拍内上限已是 20ms）：
                        //   渲染=368 次 / 3001ms（122.6 次/秒）、tick=308、均隔=9ms、组件间隔=24ms
                        //   一拍 3.8ms 工作 + 约 6ms 睡眠 → 每组件 20+4=24ms，正好等于日志的 24ms。
                        // 也就是说把帧率吃掉的是**排程口径**，不是渲染成本：
                        //   • 三个组件错相 → 一拍只渲一个 → 剩下的时间都在等下一个到期；
                        //   • 等待时 `nextAt` 已经过去，delay 被 4ms 下限夹住 → 每多付一次 4ms+唤醒迟到；
                        //   • v334 把基准改成 loopStart（实际醒来时刻）后会再引入 λ 的累积偏差
                        //     （周期 = 20+λ，λ≈1~2ms → 47~45fps），仍够不到 50。
                        //
                        // 现在：**在一条绝对时间栅格上排程** `next = 上一拍的排程时刻 + 间隔`。
                        // 这样每次唤醒的迟到 λ 不会累积（迟到 2ms 时下一次只睡 18ms，周期仍是 20）。
                        // 只有当「这一拍的排程时刻 + 一个间隔」都已经过去（即落后超过一整拍，
                        // 例如某帧渲染特别贵）才重新对齐到本拍 loopStart —— 重对齐时多个组件
                        // 共用同一个基准，相位自然重新合上。
                        long step = RangesKt.coerceAtLeast(interval, 8L);
                        long prevNext = state2.getNextAt();
                        // v1.10.410：重新对齐时**保留相位**。原来所有落后组件都落到同一个
                        // loopStart+step，三个 50fps 组件的推送会挤在同一刻、挨个排队 →
                        // 最后一个被桌面合并（v1.10.349 之前那个「第 3 个帧率明显不对」的老病）。
                        // 单组件时 myPhase=0、phaseN=1，结果与原来完全相同。
                        long nextTickAt = (prevNext <= 0L || prevNext + step <= SystemClock.elapsedRealtime())
                                ? loopStart + step + step * myPhase / phaseN   // 落后超过一整拍 → 重对齐并错相
                                : prevNext + step;              // 正常：固定速率，迟到不累积
                        // 下限 4ms → 1ms（外部分析：这个下限每生效一次就白丢 3ms 节拍；
                        // 实测平均只睡 6ms 时它是主要误差源之一）
                        state2.setNextAt(Math.max(nextTickAt, SystemClock.elapsedRealtime() + 1L));
                    }
                }
                phaseIdx++;
                long nextAt2 = state2.getNextAt();
                boolean z = 1 <= nextAt2 && nextAt2 < nextAt;
                if (z) {
                    nextAt = state2.getNextAt();
                }
            }
        }
        reportFpsIfDue();
        // v1.10.335：下限 4ms → 1ms。4ms 的由来是"别空转"，但实测它是 50fps 打不满的主因之一：
        // 拍与拍之间本该睡 10~18ms，一旦被夹到 4ms，每个周期就白丢 4ms（日志里组件间隔 24ms = 20+4）。
        // 1ms 仍能防死循环（真正被预算挡下的组件走的是同一条 delay，而每次 tick 至少渲一个）。
        long delay = RangesKt.coerceIn(nextAt - SystemClock.elapsedRealtime(), 1L, 1000L);
        this.diagSleepSum += delay;
        this.diagSleepWakeAt = SystemClock.elapsedRealtime() + delay;
        Handler handler2 = this.handler;
        if (handler2 != null) {
            this.diagPosts++;   // v1.10.78
            handler2.postDelayed(this.loop, delay);
        }
    }

    /**
     * v1.10.262：**进程内存探针**（W33）。
     *
     * 用户问「多组件常驻内存占 16GB 的 5%（800MB）了吗」，这需要有客观数字，不能靠估。
     * 一行里给四个口径，避免混读：
     *   · `java=a/b`   —— 我们的 Java 堆已用 / 上限（上限由系统按机型给，16GB 机通常 512MB）
     *   · `PSS=...`    —— 本进程实际驻留（内核计，含位图/native），**这才是「占了多少内存」**
     *   · `帧缓存`      —— 合成帧 LRU 的占用（上限 = java 堆上限 / 4，见 onCreate）
     *   · `磁盘帧`      —— 推帧用的 WebP 落盘总量（不在内存里，但吃存储）
     *   · `本机 avail/total` —— 整机可用 / 总内存
     *
     * 同时给出 `占整机 x.x%`，直接回答「是否 ≤5%」。
     * 注意：**桌面（launcher）每帧还要解码一次 WebP**，那部分账记在 launcher 进程，
     * 不在这里 —— 所以这一行只覆盖「我们这一半」。
     */
    public static void reportMemory(@NotNull Context context, @NotNull String why) {
        try {
            Runtime rt = Runtime.getRuntime();
            long usedMb = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
            long maxMb = rt.maxMemory() / 1048576L;
            long totalMb = 0L;
            long availMb = 0L;
            try {
                android.app.ActivityManager am = (android.app.ActivityManager)
                        context.getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    android.app.ActivityManager.MemoryInfo info = new android.app.ActivityManager.MemoryInfo();
                    am.getMemoryInfo(info);
                    totalMb = info.totalMem / 1048576L;
                    availMb = info.availMem / 1048576L;
                }
            } catch (Throwable ignored) {
            }
            int pssKb = 0;
            int nativeKb = 0;
            try {
                android.os.Debug.MemoryInfo dm = new android.os.Debug.MemoryInfo();
                android.os.Debug.getMemoryInfo(dm);
                pssKb = dm.getTotalPss();
                nativeKb = dm.nativePss;
            } catch (Throwable ignored) {
            }
            long diskMb = 0L;
            try {
                diskMb = WidgetUriPush.cacheBytesAll(context) / 1048576L;
            } catch (Throwable ignored) {
            }
            long pssMb = pssKb > 0 ? (pssKb / 1024L) : 0L;
            String pct = "";
            if (pssMb > 0L && totalMb > 0L) {
                pct = String.format(java.util.Locale.US, " 占整机=%.1f%%", pssMb * 100.0f / totalMb);
            }
            Diag.warn(context, "W33", "进程内存 原因=" + why
                    + " java=" + usedMb + "/" + maxMb + "MB"
                    + (pssMb > 0L ? " PSS=" + pssMb + "MB" : "")
                    + (nativeKb > 0 ? " native=" + (nativeKb / 1024L) + "MB" : "")
                    + (cacheKb > 0 ? " 帧缓存=" + (cacheKb / 1024) + "MB" : "")
                    + " 磁盘帧=" + diskMb + "MB"
                    + " 本机=" + availMb + "/" + totalMb + "MB"
                    + pct
                    // v1.10.276：**播放位置探针**。用户报「画面播放速度加快」，这是唯一能
                    // 客观判定的一条：两次 W33 之间的「帧号差 ÷ 时间差」必须等于该组件的 fps。
                    // 例如 50fps 的组件，相隔 60 秒的两条 W33 应相差 3000 帧（模总帧数）。
                    + lastProbe);
        } catch (Throwable th) {
            Diag.warn(context, "W33", "进程内存探针异常: " + th.getClass().getSimpleName());
        }
    }

    /**
     * v1.10.276：**播放位置快照**（给周期 W33 用）——`组件=帧号/总帧@fps`。
     *
     * 为什么要有它：用户报「画面播放速度疑似变快」，而"看起来快"可能是
     * 真的快 / 跳帧 / 反复重播 / 停在旧帧再跳，光看截图无法分辨。
     * 只要日志里有「帧号 + 时间」，速率就能算出来：**Δ帧 ÷ Δ秒 必须等于 fps**。
     * 这里每秒刷新一次（不是每拍），避免在 100 次/秒的推帧循环里拼字符串。
     */
    static volatile String lastProbe = "";
    private static long lastProbeAt;

    private final void refreshProbe(long nowMs) {
        if (nowMs - lastProbeAt < 1000L) {
            return;
        }
        lastProbeAt = nowMs;
        try {
            if (this.states.isEmpty()) {
                lastProbe = " 播放=无";
                return;
            }
            StringBuilder sb = new StringBuilder(64);
            for (java.util.Map.Entry<Integer, State> e : this.states.entrySet()) {
                if (e == null || e.getValue() == null) {
                    continue;
                }
                State st = e.getValue();
                int count = 0;
                WidgetConfig cfg = this.configs.get(e.getKey());
                if (cfg != null) {
                    count = cfg.getFrameCount();
                }
                sb.append(' ').append(e.getKey()).append('=').append(st.index).append('/').append(count);
                if (st.fpsX10 > 0) {
                    sb.append('@').append(String.format(java.util.Locale.US, "%.1f", st.fpsX10 / 10.0f));
                }
            }
            lastProbe = " 播放=" + sb.toString().trim();
        } catch (Throwable th) {
            lastProbe = " 播放=?";
        }
    }

    /** v1.10.262：最近一次统计窗口里帧缓存的占用（KB）—— 静态字段，探针在别的页面也能读到。 */
    static volatile int cacheKb = 0;

    /** v1.10.262：上次打内存探针的时刻。 */
    private static long lastMemProbeAt = 0L;

    /**
     * v1.10.53：按「每帧字节数」算这个组件最多能推多快（×10 帧率）。
     * 预算 28MB/s ≈ 22 万像素组件 31fps / 10 万像素组件 70fps（等于不限）。
     *
     * v1.10.119：**删掉本类里那份私有实现**。它在 v1.10.117 加了「URI 模式豁免」，
     * 但 v1.10.53 起真正被调用的是 {@code WidgetRender.pushFpsCapX10(context, manager, config)}，
     * 于是那份豁免从来没生效过 —— 双视频日志里 30fps 仍被限到 18.7fps 就是这个原因。
     * 豁免现在写在 WidgetRender 的两个重载里，调用方不需要知道传输方式。
     */
    /** 触发限流时记一条 W13（每个组件只在限流值变化时记，不刷屏）。 */
    private final void notePushCap(int widgetId, int trueFpsX10, int pushFpsX10) {
        try {
            if (pushFpsX10 >= trueFpsX10) {
                this.pushCapLogged.remove(Integer.valueOf(widgetId));
                return;
            }
            Integer last = this.pushCapLogged.get(Integer.valueOf(widgetId));
            if (last != null && last.intValue() == pushFpsX10) {
                return;
            }
            this.pushCapLogged.put(Integer.valueOf(widgetId), Integer.valueOf(pushFpsX10));
            int[] size = WidgetRender.INSTANCE.canvasSize(this, widgetId);
            long bytes = (long) size[0] * (long) size[1] * 4L;
            Diag.warn(this, "W13", "组件 " + widgetId + " 每帧 " + (bytes / 1024) + "KB × "
                    + (trueFpsX10 / 10.0f) + "fps = " + (bytes * trueFpsX10 / 10000000L)
                    + "MB/s → 已限到 " + (pushFpsX10 / 10.0f) + "fps（位图模式预算 28MB/s，避免整个桌面掉帧）");
        } catch (Throwable ignored) {
        }
    }

    /** v1.10.119：日志里直接写明这一拍用的是哪条通道，排查不用再靠「1459KB」反推。 */
    @NotNull
    private final String transportLabel() {
        try {
            // v1.10.253：先报**事实** —— 最近一次 render 真正走的通道（WidgetRender 记录）。
            // 原来只按开关猜：静态图 1:1 高清借 URI 推、但 URI 开关是关的，
            // 日志就会写成「传输=位图」，与真实情况相反。
            String real = WidgetRender.recentTransport();
            if (real != null && real.length() > 0) {
                return real;
            }
            if (WidgetUriPush.enabled(this)) {
                return "URI";
            }
            if (WidgetFlip.enabled(this)) {
                return "预装帧";
            }
        } catch (Throwable ignored) {
        }
        return "位图";
    }

    /** v1.10.61：最近渲染的组件 id（W10 里带上画布尺寸用）。 */
    private int lastRenderedId;

    /**
     * v1.10.69：桌面状态查询**永远**不在推帧线程上做（原「均衡模式」开关已删除）。
     * 用常驻单线程执行器，而不是每次 new Thread —— 每 500ms 建一个线程本身也要吃 CPU。
     */
    @Nullable
    private java.util.concurrent.ExecutorService deskExec;
    private volatile boolean deskQueryBusy;
    private long deskQuerySuggest = 500L;

    private final void ensureDeskExec() {
        if (this.deskExec == null) {
            this.deskExec = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread th = new Thread(r, "desk-query");
                th.setDaemon(true);
                return th;
            });
        }
    }

    /** v1.10.62：组件 id 列表缓存 —— getAppWidgetIds 是 binder 调用，每拍都查白花 1~3ms。 */
    @Nullable
    private int[] idCache;
    private long idCacheAt;

    @NotNull
    private final int[] widgetIdsCached() {
        long now = SystemClock.elapsedRealtime();
        int[] cached = this.idCache;
        if (cached != null && now - this.idCacheAt < 1500L) {
            return cached;
        }
        int[] ids = WidgetProvider.Companion.widgetIds(this);
        this.idCache = ids;
        this.idCacheAt = now;
        return ids;
    }

    private final int lastWidgetId() {
        return this.lastRenderedId;
    }

    /** v1.10.1：实测帧率汇总（3 秒一条）—— 判断「抽帧」用数据而不是肉眼。 */
    /**
     * v1.10.272：W23 的「预算」必须是**最紧的那个组件**的间隔。
     *
     * 原来用 `this.lastIntervalMs` —— 那是个**共享字段**，最后一次渲染的是谁就是谁：
     * 50fps 的组件渲完写 20ms、33fps 的写 30ms，于是同一行 W23 里
     * 「每组件≈27ms / 预算=20ms」与「/ 预算=30ms」交替出现（实测 04:40:27 与 04:40:30）。
     * 读的人会以为预算在跳，其实只是记录口径错了（LESSONS 76 同一个坑）。
     */
    private final long tightestIntervalMs() {
        long min = 0L;
        for (Win w : this.wins.values()) {
            if (w.intervalMs > 0L && (min == 0L || w.intervalMs < min)) {
                min = w.intervalMs;
            }
        }
        return min > 0L ? min : this.lastIntervalMs;
    }

    private final void reportFpsIfDue() {
        long now = SystemClock.elapsedRealtime();
        // v1.10.262：顺带把帧缓存占用记下来（给 W33 探针用），每窗口一次，零成本
        try {
            FrameCache fc = this.cache;
            if (fc != null) {
                cacheKb = fc.sizeKb();
            }
        } catch (Throwable ignored) {
        }
        if (this.fpsWindowStart == 0L) {
            this.fpsWindowStart = now;
            this.fpsWindowFrames = 0;
            return;
        }
        long elapsed = now - this.fpsWindowStart;
        if (elapsed < 3000L) {
            return;
        }
        float fps = this.fpsWindowFrames * 1000.0f / Math.max(elapsed, 1L);
        // v1.10.79：**先按组件各打一行**（每个组件的目标帧率/画布/单帧都独立，不会互相污染）
        try {
            java.util.Iterator<java.util.Map.Entry<Integer, Win>> it = this.wins.entrySet().iterator();
            while (it.hasNext()) {
                java.util.Map.Entry<Integer, Win> entry = it.next();
                Win w = entry.getValue();
                if (w.startAt == 0L) {
                    w.startAt = now;
                    continue;
                }
                long winElapsed = now - w.startAt;
                if (winElapsed < 3000L) {
                    continue;
                }
                if (w.frames <= 0) {
                    // 这个组件这段时间没推过帧（帧数≤1/不在桌面）—— 不刷屏，直接重置窗口
                    w.startAt = now;
                    continue;
                }
                String winCanvas = "";
                try {
                    int[] size = WidgetRender.INSTANCE.canvasSize(this, entry.getKey().intValue());
                    winCanvas = " 画布=" + size[0] + "x" + size[1];
                } catch (Throwable ignored) {
                }
                float winFps = w.frames * 1000.0f / Math.max(winElapsed, 1L);
                float winCost = w.cost / (float) Math.max(w.frames, 1);
                Diag.warn(this, "W10", "组件 " + entry.getKey() + " 实测 "
                        + String.format(java.util.Locale.US, "%.1f", winFps)
                        + "fps / " + w.frames + " 帧 / " + winElapsed + "ms（目标 " + w.targetFps + "）"
                        + winCanvas
                        + " 传输=" + WidgetRender.recentTransport(entry.getKey().intValue())
                        + WidgetUriPush.statLine(entry.getKey().intValue())
                        // v1.10.351：现渲染通道的耗时（读帧/合计的 p50 与 p95、超拍数、失败数）
                        + WidgetUriPush.takeRenderStats(entry.getKey().intValue())
                        + " 单帧=" + String.format(java.util.Locale.US, "%.1f", winCost) + "ms"
                        + " 预算=" + w.intervalMs + "ms"
                        // v1.10.336：**这个组件被桌面取走了几帧**。
                        // 我们推多少已经知道了；桌面取走多少才是"它到底有没有在看"。
                        // 取用≈帧数 → 桌面全取（卡也只卡在它自己的解码/上屏）；
                        // 取用明显少于帧数 → 桌面在限流，推得再准也没用。
                        + " 取用=" + FrameProvider.takeFetch(entry.getKey().intValue()) + "次"
                        + " 补帧=" + w.fixed + "次 掉帧=" + w.skipped + "次"
                        + w.gaps.report((int) w.intervalMs));
                w.startAt = now;
                w.frames = 0;
                w.cost = 0L;
                w.fixed = 0;
                w.skipped = 0;
            }
        } catch (Throwable ignored) {
        }
        // v1.10.262：每 60 秒打一条内存占用（W33），无需 ADB —— 它就在普通日志里。
        if (now - lastMemProbeAt >= 60000L) {
            lastMemProbeAt = now;
            reportMemory(this, this.restricted ? "周期·后台闲置(60s)" : "周期·桌面可见(60s)");
        }
        // 合计一行（多组件时「总渲染次数」会等于各组件之和，这里明确标注，避免被误读成帧率翻倍）
        float avgCost = this.fpsWindowFrames > 0
                ? this.fpsWindowCost / (float) this.fpsWindowFrames : 0f;
        long[] fetch;
        try {
            fetch = FrameProvider.takeStats();
        } catch (Throwable th) {
            fetch = new long[]{0L, 0L};
        }
        String shared = "";
        try {
            String st = WidgetUriPush.takeSharedStats();
            if (st.length() > 0) {
                shared = " " + st;
            }
        } catch (Throwable ignored) {
        }
        String soc = "";
        try {
            // v1.10.419（外部复核）：加线程有没有用，只看这一项 ——
            // 稳态与烘焙期两行对比，均频掉 ≥20% 或温度 ≥55℃ 就是已降频，加线程是负收益。
            soc = SocProbe.sample();
        } catch (Throwable ignored) {
        }
        Diag.warn(this, "W23", "推帧合计 渲染=" + this.fpsWindowFrames + " 次 / " + elapsed + "ms"
                + "（" + String.format(java.util.Locale.US, "%.1f", fps) + " 次/秒，组件数=" + this.wins.size() + "）"
                + " 传输=" + transportLabel()
                + " 单帧均=" + String.format(java.util.Locale.US, "%.1f", avgCost) + "ms"
                + " 暂停=" + this.fpsWindowPauses
                + " │ tick=" + this.diagTicks + " 调度=" + this.diagPosts
                + " 均隔=" + (this.diagGapCount > 0 ? (this.diagGapSum / this.diagGapCount) : 0) + "ms"
                // v1.10.335：请求休眠 vs 实际间隔 → 差值就是唤醒迟到（判断 50fps 差在哪一环）
                + " 休眠均=" + (this.diagPosts > 0 ? (this.diagSleepSum / this.diagPosts) : 0) + "ms"
                + " 唤醒迟到均=" + (this.diagLatenessN > 0
                        ? (this.diagLatenessSum / this.diagLatenessN) : 0) + "ms"
                // v1.10.333：原来这里是「均隔 × 组件数」，它默认"一拍只渲一个组件"——
                // 实测 116 次/秒、均隔 10ms 时它算出 30ms（33fps），而真实每组件间隔是
                // 3012×3/350 = 25.8ms（38.7fps）。口径错了 15%，还被当成"没达标"的证据。
                // 现在直接给真实的「每个组件平均多久轮到一次」，和预算并列才有可比性。
                + " 组件间隔=" + (this.fpsWindowFrames > 0
                        ? (elapsed * Math.max(this.wins.size(), 1) / this.fpsWindowFrames) : 0) + "ms"
                + " 渲/拍=" + String.format(java.util.Locale.US, "%.2f",
                        this.fpsWindowFrames / (float) Math.max(this.diagTicks, 1))
                + " 拍内上限=" + this.lastRenderBudgetMs + "ms / 预算=" + tightestIntervalMs() + "ms"
                // v1.10.335：**桌面到底取走了几帧**。以前只能数自己推了几次，
                // launcher 合并/丢弃我们看不见 —— 1080p 50fps 三组件时这是唯一能分清
                // 「我们推不上去」和「launcher 解不过来」的量。
                + " │ 桌面取用=" + fetch[0] + "次 回退=" + fetch[1]
                // v1.10.343：共享位图通道的自检数据（没走这条路时为空）
                + shared
                + " 撞预热拍=" + (this.diagTicks > 0
                        ? (int) Math.round(this.fpsWindowPrewarmTicks * 100.0 / this.diagTicks) : 0) + "%"
                + soc + "）");
        // v1.10.78：**节拍异常判定** —— 平均 tick 间隔明显小于「预算」就说明一拍被跑了两遍
        // （重复排程 / 两套计时基准 / 尺寸变更同时推进），这行会直接把结论写出来。
        try {
            long avgGap = this.diagGapCount > 0 ? (this.diagGapSum / this.diagGapCount) : 0L;
            // v1.10.79：多组件时 tick 会按 N 倍频率跑（一拍一个组件），均隔变小是正常的，不再误报
            if (this.wins.size() <= 1 && avgGap > 0L && avgGap * 3L < this.lastIntervalMs * 2L) {
                Diag.warn(this, "W20", "推帧节拍异常：平均间隔=" + avgGap + "ms，预算=" + this.lastIntervalMs
                        + "ms，tick=" + this.diagTicks + " 调度=" + this.diagPosts
                        + "，实测=" + String.format(java.util.Locale.US, "%.1f", fps) + "fps / 目标=" + this.lastTargetFps
                        + "（疑似一拍跑两遍）");
            }
        } catch (Throwable ignored) {
        }
        this.diagTicks = 0L;
        this.diagPosts = 0L;
        this.diagGapSum = 0L;
        this.diagGapCount = 0;
        this.diagSleepSum = 0L;
        this.diagLatenessSum = 0L;
        this.diagLatenessN = 0;
        this.diagSleepWakeAt = 0L;
        this.fpsWindowPrewarmTicks = 0;
        this.diagLastTickAt = 0L;
        this.fpsWindowCost = 0L;
        this.fpsWindowPauses = 0;
        this.fpsWindowStart = now;
        this.fpsWindowFrames = 0;
        this.fpsWindowCost = 0L;
    }

    /** 进入受限状态：停止渲染、释放解码缓存（只留必要配置）。 */
    private final void enterRestricted() {
        if (this.restricted) {
            return;
        }
        this.restricted = true;
        this.fpsWindowPauses++;   // v1.10.66：记进 W10 窗口，解释丢帧
        Diag.warn(this, "W11", "进入受限状态：停止推帧（复用已合成的帧，不清缓存）");
        // v1.10.422：这里原本会补跑「推迟的升档」；现在烘焙在导出时一次烘完，
        // 不再存在待补的升档任务（整套机制已删除，见 WidgetPrewarm 的注释与 LESSONS 176）。
        // v1.10.263：用户要的是「后台时」的数字 —— 进入受限这一刻就是最纯的后台态，
        // 直接拍一张快照，不用等 60 秒周期（周期那条可能正好落在前台）。
        reportMemory(this, "进入后台/受限");
        // v1.10.60：**不清 cache**（24 帧全重解码重合成会「卡一下」）。
        // v1.10.264：但 **states 会在恢复时重置**（见 leaveRestricted）—— 用户明确要求
        // 「除图片外都从起点播放」：按墙钟算帧号时，受限这段墙钟照走，恢复一开就跳到别的帧。
        // 所以现在是「帧缓存留着（不重解码）+ 播放位置归零（从第 0 帧）」。
        this.fpsWindowStart = 0L;
        this.fpsWindowFrames = 0;
    }

    private final void leaveRestricted() {
        if (this.restricted) {
            this.restricted = false;
            this.diagLastTickAt = 0L;   // v1.10.80：恢复后的第一拍不并入节拍统计
            // v1.10.336：**窗口统计跟着一起归零**。
            // 实测（22:38:07 那行）：`tick=308 均隔=20ms`，而 3004ms 的窗口里 308 拍不可能
            // 是 20ms 一次 —— 因为受限期间 tick() 照走（diagTicks++）却不计 gap，
            // 于是恢复后的第一个窗口把"屏幕熄灭那段"的拍数并进来了，`渲/拍` 被读成 1.46。
            // 这不是性能问题，是口径问题；重开窗口最省事，也最不容易被误读。
            this.fpsWindowStart = 0L;
            this.fpsWindowFrames = 0;
            this.fpsWindowCost = 0L;
            this.diagTicks = 0L;
            this.diagPosts = 0L;
            this.diagGapSum = 0L;
            this.diagGapCount = 0;
            this.diagSleepSum = 0L;
            this.diagLatenessSum = 0L;
            this.diagLatenessN = 0;
            this.diagSleepWakeAt = 0L;
            this.fpsWindowPrewarmTicks = 0;
            // v1.10.264（反馈「切回桌面后视频进度会跳到某个地方」）：
            // 以前是「接着播」—— 但帧号是**按墙钟算**的（`due = elapsed × fps % 帧数`），
            // 受限这段时间墙钟一直在走，于是恢复的第一拍会算出「如果一直播到现在该到哪一帧」，
            // 用户看到的就是「进度莫名跳了一下」。现在改成**从第 0 帧重播**：
            // 重置每个组件的 startAt / index / nextAt，墙钟基准归零 → 下一拍取第 0 帧。
            // 静图（帧数≤1）不受影响：它本来就只有第 0 帧，重置后只是再推一次。
            int reset = 0;
            try {
                for (State st : this.states.values()) {
                    if (st == null) {
                        continue;
                    }
                    st.startAt = 0L;
                    st.index = 0;
                    st.nextAt = 0L;
                    reset++;
                }
                // W22 / repushOne 读的是这张表，也要一起归零，否则日志里还写「当前位置 帧=87」
                LAST_INDEX.clear();
                // 每个组件的统计窗口也归零：否则恢复后的第一行 W10 会把「受限的那段时间」
                // 也算进分母（实测出现过「实测 4.1fps / 98 帧 / 24055ms」这种吓人的数）。
                for (Win w : this.wins.values()) {
                    if (w != null) {
                        w.startAt = now();
                        w.frames = 0;
                        w.cost = 0L;
                    }
                }
            } catch (Throwable ignored) {
            }
            Diag.warn(this, "W12", "恢复不受限：从第 0 帧重播（静态图重推一次） 组件数=" + reset);
            reportMemory(this, "回到前台/恢复推帧");
            this.fpsWindowStart = 0L;
            this.fpsWindowFrames = 0;
        }
    }

    private static long now() {
        return SystemClock.elapsedRealtime();
    }

    private final void reload(boolean force) {
        int generation = WidgetStore.INSTANCE.generation(this);
        if (force || generation != this.lastGeneration) {
            this.lastGeneration = generation;
            Iterable $this$filter$iv = WidgetStore.INSTANCE.all(this);
            Collection destination$iv$iv = new ArrayList();
            for (Object element$iv$iv : $this$filter$iv) {
                WidgetConfig it = (WidgetConfig) element$iv$iv;
                if (it.getFrameCount() > 0) {
                    destination$iv$iv.add(element$iv$iv);
                }
            }
            Iterable $this$associateBy$iv = (List) destination$iv$iv;
            int capacity$iv = RangesKt.coerceAtLeast(MapsKt.mapCapacity(CollectionsKt.collectionSizeOrDefault($this$associateBy$iv, 10)), 16);
            Map destination$iv$iv2 = new LinkedHashMap(capacity$iv);
            for (Object element$iv$iv2 : $this$associateBy$iv) {
                WidgetConfig it2 = (WidgetConfig) element$iv$iv2;
                destination$iv$iv2.put(Integer.valueOf(it2.getWidgetId()), element$iv$iv2);
            }
            this.idCache = null;   // v1.10.62：配置变了 → 组件 id 列表重新查
            this.configs = MapsKt.toMutableMap(destination$iv$iv2);
            // v1.10.81：已删除组件的统计窗口一并清掉（否则残留条目会继续被遍历）
            try {
                this.wins.keySet().retainAll(this.configs.keySet());
            } catch (Throwable ignored) {
            }
            this.states.clear();
            FrameCache frameCache = this.cache;
            if (frameCache != null) {
                frameCache.clear();
            }
        }
    }

    private final void ensureChannel() {
        NotificationManager manager = (NotificationManager) getSystemService(NotificationManager.class);
        if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel $this$ensureChannel_u24lambda_u244 = new NotificationChannel(CHANNEL_ID, getString(R.string.app_name), 2);
            $this$ensureChannel_u24lambda_u244.setDescription(getString(R.string.notif_text));
            $this$ensureChannel_u24lambda_u244.setShowBadge(false);
            $this$ensureChannel_u24lambda_u244.setSound(null, null);
            manager.createNotificationChannel($this$ensureChannel_u24lambda_u244);
        }
    }

    private final Notification buildNotification() {
        PendingIntent openApp = WidgetRender.INSTANCE.contentIntent(this);
        Notification.Action stopAction = new Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_stat_anim), getString(R.string.notif_stop), stopPendingIntent()).build();
        Intrinsics.checkNotNullExpressionValue(stopAction, "build(...)");
        Notification notificationBuild = new Notification.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_stat_anim).setContentTitle(getString(R.string.notif_title)).setContentText(getString(R.string.notif_text)).setOngoing(true).setOnlyAlertOnce(true).setContentIntent(openApp).addAction(stopAction).build();
        Intrinsics.checkNotNullExpressionValue(notificationBuild, "build(...)");
        return notificationBuild;
    }

    private final PendingIntent stopPendingIntent() {
        Intent intent = new Intent(this, (Class<?>) PlaybackService.class).setAction(ACTION_STOP);
        Intrinsics.checkNotNullExpressionValue(intent, "setAction(...)");
        PendingIntent service = PendingIntent.getService(this, 1, intent, 201326592);
        Intrinsics.checkNotNullExpressionValue(service, "getService(...)");
        return service;
    }
}
