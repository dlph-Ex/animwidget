package io.github.animwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Process;
import android.os.SystemClock;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.jetbrains.annotations.NotNull;

/**
 * v1.10.118：URI 模式的**后台预热编码**。
 * v1.10.119：**画布改由「真正要渲染的那一下」决定**。
 *
 * v1.10.118 的致命 bug（双视频实测抓到）：
 *   保存配置时就开热，用的是 `widgetSize()` 当时能查到的尺寸 —— 而 vivo 桌面刚开始只给
 *   「110×110dp」这个默认值（`onAppWidgetOptionsChanged` 要晚一拍才把真实格子尺寸发过来），
 *   于是预热把 300 帧全按 **360×360** 编好，等到真正推帧时画布已经是 **597×626**。
 *   缓存 key 里带尺寸 → 一个字都对不上 → 命中率 0%，单帧还是 39.8ms，预热 6 秒全白干。
 *
 * 现在改成：**渲染路径（{@link WidgetRender#render}）在算出画布之后调用 {@link #ensure}**。
 * 那一刻的尺寸就是桌面真实尺寸，热出来的帧必定对得上；尺寸再变（拖拽/旋转），
 * 指纹（stamp）跟着变 → 自动按新画布重热，旧画布目录整目录删掉。
 *
 * 规则：
 *   · 独立后台线程（THREAD_PRIORITY_BACKGROUND），3 路并行编码，不抢推帧主线程；
 *   · 指纹没变就直接返回（等于零开销，每帧都会调用它）；
 *   · 已存在的帧文件直接跳过（断点续热）；
 *   · 指纹变了/开关关了 → 旧任务自己退出（代号 gen 校验），不会写脏数据。
 */
public final class WidgetPrewarm {

    private static final Map<Integer, Integer> GENERATION = new HashMap<>();
    /** 这个组件当前「已经开热 / 热完了」的指纹（=`stamp`，含画布尺寸）。 */
    private static final Map<Integer, String> READY = new HashMap<>();
    /** 已经为哪个指纹补热过一次（避免坏帧导致无限重试）。 */
    private static final Map<Integer, String> RETRIED = new HashMap<>();
    /** 正在烘的组件（渲染路径据此决定「顶帧」还是「现合成」）。 */
    private static final java.util.Set<Integer> BUSY = new java.util.HashSet<>();
    /**
     * v1.10.448：**导出烘焙正在进行**的组件 id。
     *
     * <p>实机日志（v1.10.447，11:36:46）说明了整个 bug：
     * `W29 导出即烘` 之后 3 秒 `W29 等待烘焙：3 秒未开始，强制重开一批`，而且**从头到尾没有 W13**。
     * 也就是说 `bake()` 在 `stillCurrent()` 那一步就返回了 —— 它启动之后，
     * 桌面那一侧每秒一次的 `render()` 走了 `ensure()`，而 ensure 发现 stamp 不同 →
     * `cancel(widgetId)` 把代次 +1 → 我们的导出烘焙当场变成"过期批次"、**一声不响地退出**。
     * 于是进度永远 0/235，等待循环等一个已经死掉的批次。
     *
     * <p>现在导出烘焙期间置这条锁：ensure()/渲染路径直接让路（不 cancel、不 start），
     * 只有配置真的变了（reset）才允许打断。
     */
    private static final java.util.Set<Integer> EXPORTING = new java.util.HashSet<>();

    /**
     * v1.10.120：前台正在导出/解析时，预热线程让路。
     * 用「时间戳」而不是布尔：即使少调一次 false（异常/被杀），15 秒后也会自己恢复，
     * 不会出现「预热永远被暂停」的死状态。
     */
    private static volatile long foregroundUntil;

    /**
     * v1.10.333：**同时只烘一个组件**（全局一个烘位，FIFO 排队）。
     *
     * 实测（21:12:29，三个 1:1 组件同时开烘）：18 个编码线程抢 8 核，
     * 311/312/313 分别只跑到 23/21/17 帧/秒；而同一个组件独占时是 50 帧/秒
     * （21:12:27 W13 用时=5695ms 速率=50帧/秒）。
     * 换算成总吞吐：并发 3×23 帧/秒 × 23 万 ≈ 16M px/s，独占 50 × 39 万 ≈ 20M px/s
     * —— 并发不是"多干活"，是**互相拖慢 20%~25%**，还把推帧循环一起拖到 19~28fps。
     *
     * 所以排队：后来的组件先停在 BUSY 态（推帧端继续顶帧，不会冻住），
     * 等前一个烘完再开。代价是第三个组件的帧要晚十几秒到齐（有 E 的前段补推，
     * 0.2s 内就有画面），换来的是每一个都能跑满烘速。
     */
    // v1.10.423：烘焙进度对外可见 —— 导出时那条任务条要显示「正在烘焙 47/235」，
    // 并且在烘完之前不许放用户回桌面（用户口径：宁可等一次，也不要"导出后还卡一会儿"）。
    private static volatile int progWidget = -1;
    private static volatile int progDone = 0;
    private static volatile int progTotal = 0;

    /** @return {已完成, 总数, 是否在烘这一路}；不在烘这一路时第 3 位是 0。 */
    @NotNull
    public static int[] bakeProgress(int widgetId) {
        boolean mine = progWidget == widgetId && progTotal > 0;
        return new int[]{progDone, progTotal, mine ? 1 : 0};
    }

    private static void progressBegin(int widgetId, int total) {
        progWidget = widgetId;
        progDone = 0;
        progTotal = Math.max(total, 0);
    }

    private static void progressAt(int widgetId, int done, int total) {
        if (progWidget == widgetId) {
            progDone = Math.max(done, 0);
            progTotal = Math.max(total, 0);
        }
    }

    private static final java.util.concurrent.Semaphore BAKE_SLOT =
            new java.util.concurrent.Semaphore(1, true);
    /** 真正在编码的批次数（排队中的不算）。自校准只信「==1 且没让过路」的批次。 */
    private static final AtomicInteger BAKING = new AtomicInteger(0);
    /** 本批有没有因为前台任务（导出/解析）让过路 —— 让过路的批次吞吐不可信。 */
    private static volatile boolean YIELDED;

    /**
     * v1.10.335（外部分析 P2-1）：**烘帧线程的 CPU 时间账**。
     *
     * 外部指出「6 线程只有约 4.5 个线程的有效吞吐」，并且我们用的是
     * `THREAD_PRIORITY_BACKGROUND`（nice 10）—— 很可能被限在小核 / 被别的任务抢占。
     * 这条计量直接回答它：CPU 时间 ÷ (线程数 × 墙钟)。
     *   · ≈1.00 → 满核在跑，瓶颈就是编码本身；
     *   · <0.85 → 被抢占/限核 → 提优先级有意义（v1.10.335 已提到 DEFAULT）。
     * 同时记「读源帧」耗时：单帧线程时间 = 95ms（6×4.6s/290）而编码只有 71ms + 合成 2ms，
     * 那 22ms 必须落到某个具体环节上，不能靠猜。
     */
    private static final AtomicLong BATCH_CPU_NS = new AtomicLong();
    private static final AtomicLong BATCH_LOAD_MS = new AtomicLong();

    private WidgetPrewarm() {
    }

    /** 设置页里存的预热线程数（0/未设 = 自动）。键与 SettingsActivity 一致。 */
    public static final String PREF_THREADS = "prewarm_threads";

    /**
     * v1.10.191：预热线程数。
     *
     * 老算法（=「自动」）：max(2, min(4, 核数-3))，8 核就是 4。
     * 用户可以自己调 4~8 —— 烘帧追不上播放时加线程能缩短冷窗口，但预热线程是
     * MIN_PRIORITY，和桌面播放抢 CPU，调太高（尤其低端机）反而让播放掉帧。
     * 所以这里再夹一次「不超过核数」，避免用户填 8 而机器只有 4 核。
     */
    /**
     * v1.10.256：画布静置多久才开烘（去抖）。
     * v1.10.288：800 → 1500ms（只对多帧批次生效，单帧见 ensure 里的 quick）。
     * 实测（12:07，301 帧）vivo 拖动时相邻两档尺寸只隔 1~2 秒（12:07:13 / :14 / :16），
     * 800ms 挡不住 —— 一次拖动开了 4 批、烧了 ~15 秒 CPU，其中 3 批作废。
     */
    // v1.10.292（用户硬性要求："拖动组件大小 = 画面 1s 内自动适配"）：
    // 实测 首帧命中 距画布变化=1684ms，其中 距开烘=182ms → **变化→开烘 = 1502ms**，
    // 也就是这一版的 1500ms 静置一个人就把 1 秒预算吃光了。
    // 1500 → 400ms：变化→开烘 ≈ 480ms，+ 首帧烘 ≈ 200ms → 首个正确画面 ≈ 0.7s。
    static final long SETTLE_MS = 300L;   // v1.10.350：400 → 300（用户硬要求"拖动后 1s 内接上"）

    /**
     * v1.10.339：**两段烘**（拖动场景的止损）。
     *
     * 实测（23:07:15~23:07:54，用户把 314 从 662x643 一路拖到 1080x1080）：
     *   662x643 → 4.3s 全量烘完；拖到 662x869（284ms 后作废）；拖到 879x869 → 7.6s 烘完；
     *   拖到 866x1080（498ms 后作废）；拖到 1080x1080 → 10.8s 烘完。
     * 一次拖动 = 3 次全量 + 2 次作废 ≈ 23 秒的 5 线程编码、单组件磁盘峰值 50MB。
     * 根因是"画布一静置 400ms 就开一炉 290 帧"，而用户这时往往还在拖。
     *
     * 现在：先只烘**前段 48 帧**（≈1 秒播放量，保证新画布立刻有画面），
     * 然后**等画布真正稳定 FRONT_STABLE_MS** 再放行剩下的。
     * 拖动中画布一变 → gen 被顶掉 → 这一炉只白烧 ≤48 帧（≈0.3s），不再烧 290 帧。
     * 等待期间播放继续用旧画布的帧（顶帧），画面是动的、只是稍软。
     */
    // v1.10.350：48 → 72 帧（50fps 下 1.44 秒）。前段必须够长到"撑过闸门"，
// 否则闸门一开，播放头早就把前段吃完了 → 又变 PPT。
    static final int FRONT_FRAMES = 72;
    // v1.10.350：1500 → 600。1500 太保守：前段 72 帧只需 ~1.2s 就烘完，
    // 却要白等到 1.5s 才放行剩下的 —— 那 0.3~0.8s 正好是"已经能播、但接不上"的窗口。
    static final long FRONT_STABLE_MS = 600L;

    /** v1.10.256：上一次见到的画布指纹 + 时间（只用于去抖，与 READY 语义不同）。 */
    private static final java.util.HashMap<Integer, String> STAMP_SEEN = new java.util.HashMap<>();
    private static final java.util.HashMap<Integer, Long> STAMP_AT = new java.util.HashMap<>();

    /**
     * v1.10.289：**"首次上屏时间"的量尺**（HANDOFF 第 9 节验收项，之前日志里量不出来）。
     * 画布一变（ensure 看到新指纹）记 [变化时刻, 0]；真正开烘时补 [.., 开烘时刻]；
     * render() 第一次在新画布上命中缓存时 noteFirstHit 打一行 W13 并清掉，所以每次尺寸变化只打一行。
     * 只写日志，不影响任何行为。
     */
    private static final java.util.concurrent.ConcurrentHashMap<Integer, long[]> FIRST_HIT =
            new java.util.concurrent.ConcurrentHashMap<>();

    static void noteFirstHit(@NotNull Context context, int widgetId) {
        long[] p = FIRST_HIT.remove(Integer.valueOf(widgetId));
        if (p == null) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        Diag.warn(context, "W13", "首帧命中 组件=" + widgetId
                + " 距画布变化=" + (now - p[0]) + "ms"
                + (p[1] > 0L ? " 距开烘=" + (now - p[1]) + "ms" : " （开烘前就命中：这块画布已热）"));
    }

    /**
     * v1.10.398：**这是「设定值」，不是「这一刻真正开几条线程」**。
     *
     * <p>v1.10.397 把「播放期间减半」直接塞进了这个函数，后果是：设置页那一行、
     * 输入框的预填、A05/W13 日志全都读到 2 —— 用户把设定从 4 改到 8，界面还是显示 2，
     * 看起来「怎么改都没用」（实际烘帧确实按 8 存着，只是显示被压过）。</p>
     *
     * <p>现在拆成两个出口：这个 = 用户设定（按核数夹住）；{@link #effectiveThreads} =
     * 这一刻真正开几条（播放期间减半）。界面只许读这个。</p>
     */
    public static int threadSetting(@NotNull Context context) {
        int cores = Runtime.getRuntime().availableProcessors();
        int auto = Math.max(2, Math.min(4, cores - 3));
        try {
            int want = context.getSharedPreferences("settings", 0).getInt(PREF_THREADS, 0);
            if (want <= 0) {
                return auto;
            }
            int hi = Math.max(1, Math.min(8, cores));
            int lo = Math.min(4, hi);
            return Math.max(lo, Math.min(want, hi));
        } catch (Throwable th) {
            return auto;
        }
    }

    /**
     * v1.10.406：**取消「播放期间减半」**。
     *
     * <p>v1.10.397 加的减半是为了让推帧不被抢核，但两份实测都推翻了它：
     * ① 抖动来自「这一拍要现渲染」，不是烘帧线程多（5 线程 85% CPU 时只要命中现成帧，p95 仍 35ms）；
     * ② 减半直接把烘焙拖成 3 线程 → 235 帧要 31s 以上 → **永远烘不完** → 现渲染越滚越多，
     * 反而制造了更多抖动。</p>
     *
     * <p>现在两个出口都是「设定值」：界面读 {@link #threadSetting}，烘帧读这个。
     * 保留方法名是为了让 W13/W29 的日志口径不变。</p>
     */
    public static int effectiveThreads(@NotNull Context context) {
        return threadSetting(context);
    }

    // v1.10.497：**删掉 `duringPlayback()`（播放期间烘帧减半）的残留实现**。
    //
    // v1.10.406 已经取消了「减半」，两个出口并成了同一个值；但这个方法一直躺在文件里没人调用，
    // 于是 W13 的日志里还留着一个 `(设定…，播放期间让路)` 的分支 —— 那个分支**永远不可能成立**
    // （workers 恒等于 threadSetting），读日志的人却会以为存在「播放期间让路」这回事。
    // 用户这次问「预热线程数 4~8 到底有没有用」，答案是：有用，而且是**真正开几条线程**；
    // 为了让这个答案在代码里一眼看得见，把死代码与假分支一起清掉。

    /**
     * v1.10.257：这一版画布**还没烘好**吗（没烘过 / 正在烘 / 刚变还在静置）。
     *
     * 用途：推帧时如果画布还没烘出来，就**别现场合成** —— 一帧 100ms+（含编码），
     * 会把节拍直接打歪。改成顶一张旧画布的邻帧（2ms），等预热补上再自然接上。
     *
     * 判据刻意用 READY（= 已下过预热的画布指纹），不用「文件在不在」：
     * 文件可能是上一轮别的画布留下的，用它判断会把「还没烘」误判成「已就绪」。
     */
    static boolean pending(@NotNull Context context, @NotNull WidgetConfig config, int w, int h) {
        try {
            String stamp = WidgetUriPush.stamp(context, config, w, h);
            synchronized (GENERATION) {
                return !stamp.equals(READY.get(Integer.valueOf(config.getWidgetId())));
            }
        } catch (Throwable th) {
            return false;
        }
    }

    /**
     * v1.10.119：渲染路径调用 —— 指纹变了才开热。    /**
     * v1.10.119：渲染路径调用 —— 指纹变了才开热。
     * 每帧都调用，必须够便宜（一次 map 查询 + 一次字符串比较）。
     */
    public static void ensure(@NotNull Context context, @NotNull WidgetConfig config,
                              int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        try {
            // v1.10.254：动图高清 / 1:1 档是靠 URI 推的，即使全局 URI 开关关着也要预热，
            // 否则第一圈每帧现烘（130 万 px 合成 + 编码）必然掉帧。
            if (!WidgetRender.needUri(context, config)) {
                return;
            }
            int widgetId = config.getWidgetId();
            synchronized (EXPORTING) {
                if (EXPORTING.contains(Integer.valueOf(widgetId))) {
                    return;   // v1.10.448：导出烘焙在跑，别顶掉它（详见 EXPORTING 的注释）
                }
            }
            String stamp = WidgetUriPush.stamp(context, config, width, height);
            Integer id = Integer.valueOf(widgetId);
            synchronized (GENERATION) {
                // v1.10.256：**画布刚变过先不烘** —— 去抖 800ms。
                //
                // 实测（动图 1:1 / 165 帧 / 1210×1113 素材）：在桌面拖一次尺寸，画布从
                // 360×331 → 597×549 → 989×910 → 1030×947 连跳四档，于是连开四批预热，
                // 前三批全部「本批作废」（W13 原文），白烧 CPU 和磁盘几十秒。
                // 拖动是低频操作，等它停稳再烘一次就够；这 800ms 里播放本来就在用顶帧。
                String prev = STAMP_SEEN.get(id);
                long now = android.os.SystemClock.elapsedRealtime();
                // v1.10.287：**单帧（静态图）不等静置，画布一变就烘**。
                // 800ms 去抖是为多帧批次设的（拖动连变几档会白烧几批，见上）；单帧一次只是
                // ~130ms，白烧也无所谓。实测（W13 11:45:40 延后 → 11:45:43 才开始）静态图松手后
                // 要等：去抖 + 静置 800ms + 下一个 1s 空转 tick 才会再进 ensure + bake 里 250ms，
                // 叠起来 1.5~3s 桌面都停在拉伸的旧画布上。
                final boolean quick = config.getFrameCount() <= 1;
                if (!stamp.equals(prev)) {
                    STAMP_SEEN.put(id, stamp);
                    STAMP_AT.put(id, Long.valueOf(now));
                    if (prev != null) {
                        // v1.10.272：**画布一变就让正在跑的那一批立刻退出**（而不是等 800ms 后
                        // 新批次 start() 才把 gen 顶掉）。实测：拖动时每一档画布都让旧批次多烘
                        // 5~6 帧（157~204ms/帧 × 6 线程）才作废，三批白烧 ~4 秒 CPU。
                        // 注意必须同时摘掉 BUSY —— 新批次还没开，BUSY 若留着，
                        // 「busy()=true 但没人会去清它」就成了永久卡死的标志。
                        cancel(widgetId);
                        FIRST_HIT.put(id, new long[]{now, 0L});   // v1.10.289
                        Diag.warnThrottled(context, "W13", (quick ? "预热立即 组件=" : "预热延后 组件=") + widgetId
                                + (quick ? " 画布刚变（单帧，免静置，立即烘）"
                                        : " 画布刚变（等静置 " + SETTLE_MS + "ms 再烘，避免拖动连开多批）"));
                    }
                    if (!quick) {
                        return;
                    }
                }
                Long at = STAMP_AT.get(id);
                if (!quick && at != null && now - at.longValue() < SETTLE_MS) {
                    return;
                }
                if (stamp.equals(READY.get(id))) {
                    return;
                }
                READY.put(id, stamp);
            }
            start(context, config, width, height, stamp, 0);
        } catch (Throwable th) {
            // 启动本身失败（线程建不出来等）：撤掉记忆，下一帧还会再试一次
            synchronized (GENERATION) {
                READY.remove(Integer.valueOf(config.getWidgetId()));
            }
        }
    }

    /**
     * v1.10.443：**导出链专用 —— 不等去抖、不看「已热」记忆，直接开烘。**
     *
     * <p>为什么必须有这一个：{@link #ensure} 里有多帧的 800ms 去抖和 `READY` 记忆，
     * 而导出时画布/参数**一定刚变过**（新素材、新参数），于是 `stamp != prev` → 直接 return，
     * **一帧都没开始烘**。这就是"导出时从来没看见『正在烘焙 x/y』"的真因：
     * 等待循环在等一个永远不会启动的批次，20 秒后超时放行、配置提交、帧留给回桌面去烘。
     *
     * <p>用户口径（v1.10.443）："真正意义上一次导出、绝不再烘，换一分钟完全能接受"。
     * 所以这里把两道闸门都绕开；代次生成、BUSY 占位、作废旧批次、按目标质量直出、
     * 写"这一代已烘过"标记，全部沿用 {@link #start} / {@link #bakeBody} 的原路径。
     */
    public static void bakeNow(@NotNull Context context, @NotNull WidgetConfig config,
                               int width, int height) {
        bakeNow(context, config, width, height, 0);
    }

    /**
     * @param frames 这次导出的真实帧数（0 = 用 config 里的）。
     *
     * <p>v1.10.449：**这个参数是 bug 的解**。导出链传进来的 `snapshot` 是「导出开始时」的
     * 配置副本，那时 `frameCount` 还是 0（帧数是导出完成后才有的）。而 `bake()` 第一件事就是
     * `if (count <= 0) return;` —— **静默返回**。所以 11:49:40 之后 61 秒里既没有 W13、
     * 也没有"代次已变"，进度永远 0/235（用户："等待进度条消失，然后没有任何反应"）。
     */
    public static void bakeNow(@NotNull Context context, @NotNull WidgetConfig config,
                               int width, int height, int frames) {
        if (width <= 0 || height <= 0) {
            Diag.warn(context, "W29", "导出即烘跳过：画布无效 " + width + "x" + height);
            return;
        }
        if (!WidgetRender.needUri(context, config)) {
            Diag.warn(context, "W29", "导出即烘跳过：当前不走 URI 推送（静态图/档位 0）");
            return;
        }
        if (frames <= 0 && config.getFrameCount() <= 0) {
            Diag.warn(context, "W29", "导出即烘跳过：帧数为 0（config=0，result=0）");
            return;
        }
        // v1.10.450：**必须用"帧数已经写对"的配置去烘**。
        // 导出链传进来的 snapshot 是导出开始那一刻的副本（frameCount=0），而
        // `WidgetStore.loadFrame()` 第一句就是 `if (config.getFrameCount() <= 0) return null;`
        // —— 于是 235 帧全部"加载失败"，5ms 就烘完（日志：完成=0 失败=235 用时=5ms）。
        final WidgetConfig cfg = frames > 0 ? withFrameCount(config, frames) : config;
        Integer id = Integer.valueOf(cfg.getWidgetId());
        String stamp = WidgetUriPush.stamp(context, cfg, width, height);
        // v1.10.452：**同步把进度归零**。以前只有 bakeBody 里才 progressBegin，而 bakeBody 得先拿到
        // BAKE_SLOT；同一个组件上一次烘焙留下的「235/235」会在这段窗口里被 awaitBake 读到 ——
        // 于是"刚开烘就判完成"、立刻关页，用户看到的是"进度条闪一下、桌面没画面"。
        progressBegin(id.intValue(), frames > 0 ? frames : cfg.getFrameCount());
        synchronized (EXPORTING) {
            EXPORTING.add(id);
        }
        synchronized (GENERATION) {
            STAMP_SEEN.put(id, stamp);
            STAMP_AT.put(id, Long.valueOf(SystemClock.elapsedRealtime()));
            READY.put(id, stamp);
        }
        start(context, cfg, width, height, stamp, frames);
    }

    /** v1.10.450：复制一份配置、只把 frameCount 换成导出真实帧数（其余逐字段照抄）。 */
    @NotNull
    private static WidgetConfig withFrameCount(@NotNull WidgetConfig c, int count) {
        if (c.getFrameCount() == count) {
            return c;
        }
        try {
            return c.copy(c.getWidgetId(), c.getKind(), c.getSourceName(), c.getSourceUri(),
                    c.getFps(), c.getMaxEdge(), c.getQuality(), count,
                    c.getFrameWidth(), c.getFrameHeight(),
                    c.getShape(), c.getShapeAlpha(), c.getBorder(), c.getLayout(),
                    c.getOverlayEnabled(), c.getOverlayVisible(), c.getOverlayX(), c.getOverlayY(),
                    c.getOverlayWidth(), c.getScalePercent(), c.getShadowPercent(),
                    c.getRotation(), c.getShapeRotation(), c.getFitContent(),
                    c.getOffsetXPercent(), c.getOffsetYPercent(), c.getContentScalePercent(),
                    c.getMaskOffsetXPercent(), c.getMaskOffsetYPercent(), c.getShadowCurve());
        } catch (Throwable th) {
            return c;   // 复制失败就退回原配置（烘焙会照常报"帧数为 0"），不要在这里崩
        }
    }

    /** v1.10.120：配置页有导出/解析在跑时置位（自动 15 秒过期）。 */
    public static void setForegroundBusy(boolean on) {
        foregroundUntil = on
                ? SystemClock.elapsedRealtime() + 15000L
                : 0L;
    }

    /** v1.10.338：当前真正在编码的批次数（W23 用它标「这一窗口有没有撞上预热」）。 */
    public static int activeBakes() {
        return BAKING.get();
    }

    /** v1.10.120：这个组件是不是正在烘帧（是 → 渲染路径不要在主线程现合成）。 */
    public static boolean busy(int widgetId) {
        synchronized (BUSY) {
            return BUSY.contains(Integer.valueOf(widgetId));
        }
    }

    /**
     * v1.10.272：**只让正在跑的那一批退出**（不动 READY 记忆）。
     *
     * 为什么单独一个方法：画布刚变时我们要"停旧批"，但不想像 {@link #reset} 那样把
     * 「已热」记忆也清掉 —— 旧画布的帧还在磁盘上，`pending()` 会用指纹比对把
     * "新画布没烘"这件事自己算出来，不需要我们猜。
     */
    public static void cancel(int widgetId) {
        synchronized (GENERATION) {
            Integer old = GENERATION.get(Integer.valueOf(widgetId));
            GENERATION.put(Integer.valueOf(widgetId), Integer.valueOf(old == null ? 1 : old.intValue() + 1));
        }
        synchronized (BUSY) {
            BUSY.remove(Integer.valueOf(widgetId));
        }
    }

    /** 配置变了：作废「已热」记忆并让正在跑的热任务退出（下一次渲染会自动重热）。 */
    public static void reset(@NotNull Context context, int widgetId) {
        synchronized (GENERATION) {
            READY.remove(Integer.valueOf(widgetId));
            Integer old = GENERATION.get(Integer.valueOf(widgetId));
            GENERATION.put(Integer.valueOf(widgetId), Integer.valueOf(old == null ? 1 : old.intValue() + 1));
        }
    }

    /** 打开/关闭 URI 模式：所有组件都重新判断一次。 */
    public static void resetAll() {
        synchronized (GENERATION) {
            READY.clear();
            for (Integer id : new ArrayList<>(GENERATION.keySet())) {
                Integer old = GENERATION.get(id);
                GENERATION.put(id, Integer.valueOf(old == null ? 1 : old.intValue() + 1));
            }
        }
    }

    private static void start(@NotNull Context context, @NotNull WidgetConfig config,
                              int width, int height, @NotNull String stamp, int frames) {
        final int widgetId = config.getWidgetId();
        final int gen;
        synchronized (GENERATION) {
            Integer old = GENERATION.get(Integer.valueOf(widgetId));
            gen = (old == null ? 0 : old.intValue()) + 1;
            GENERATION.put(Integer.valueOf(widgetId), Integer.valueOf(gen));
        }
        final Context app = context.getApplicationContext();
        Thread worker = new Thread(() -> run(app, config, width, height, stamp, gen, frames),
                "anim-prewarm-" + widgetId);
        worker.setPriority(Thread.MIN_PRIORITY);
        // v1.10.120：从「决定要烘」这一刻就算 busy（含前面 900ms 去抖）——
        // 否则那段窗口里渲染线程还会在主线程现合成 125ms/帧，把邻居一起拖死。
        synchronized (BUSY) {
            BUSY.add(Integer.valueOf(widgetId));
        }
        try {
            worker.start();
        } catch (Throwable th) {
            synchronized (BUSY) {
                BUSY.remove(Integer.valueOf(widgetId));
            }
            synchronized (EXPORTING) {
                EXPORTING.remove(Integer.valueOf(widgetId));   // 线程都没起来，别把锁留下
            }
            throw th;
        }
    }

    private static void run(@NotNull Context context, @NotNull WidgetConfig config,
                            int width, int height, @NotNull String stamp, int gen, int frames) {
        final int widgetId = config.getWidgetId();
        try {
            bake(context, config, width, height, stamp, gen, frames);
        } finally {
            // v1.10.121（日志实锤的 bug）：**只有「最新那一批」才有资格摘掉 BUSY**。
            // 原来无脑 remove —— 尺寸连变两次时，老的烘任务退出会把新手任务的标志一起摘掉，
            // 于是渲染路径以为「没在烘」，又回到主线程现合成（日志 17:20:34 单帧=117ms、
            // 邻居 156 被拖到 6.8fps，就是这么来的）。
            synchronized (EXPORTING) {
                EXPORTING.remove(Integer.valueOf(widgetId));   // v1.10.448：导出烘焙这一批结束（正常/异常）
            }
            synchronized (BUSY) {
                if (stillCurrent(widgetId, gen)) {
                    BUSY.remove(Integer.valueOf(widgetId));
                }
            }
            if (stillCurrent(widgetId, gen)) {
                // 旧画布目录（prepare 时改名成 *.trash.*）现在才真正删掉：
                // 那会儿可能还有写盘/桌面读取正在进行，改动名就是为了避开这一段。
                WidgetUriPush.sweepTrash(context, widgetId, stamp);
            }
        }
    }

    private static void bake(@NotNull Context context, @NotNull WidgetConfig config,
                             int width, int height, @NotNull String stamp, int gen, int frames) {
        // v1.10.335：BACKGROUND(10) → DEFAULT(0)。推帧线程是 DISPLAY(-4)，仍然压我们一头，
        // 所以"让路"这件事由优先级天然保证；而 BACKGROUND 在 8 核机上很容易被塞进小核/被抢占。
        Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
        int widgetId = config.getWidgetId();
        int count = frames > 0 ? frames : config.getFrameCount();
        if (count <= 0) {
            Diag.warn(context, "W13", "预热跳过：帧数为 0 组件=" + widgetId
                    + "（frames=" + frames + " config=" + config.getFrameCount() + "）");
            return;
        }
        // v1.10.288：不再先睡 250ms。它原本只为挡「保存后紧跟的那一次尺寸修正」，
        // 现在 ensure 已按 SETTLE_MS(1.5s) 静置过，这 250ms 纯属白等。
        if (!stillCurrent(widgetId, gen)) {
            // v1.10.448：**不许静默**。以前这里一声不响地 return，用户看到的就是
            // "进度永远 0/235、也没有任何日志能解释"（11:36 那次排查花了两个版本）。
            Diag.warn(context, "W13", "预热放弃：代次已变 组件=" + widgetId + " gen=" + gen
                    + " 现=" + GENERATION.get(Integer.valueOf(widgetId)));
            return;
        }
        // v1.10.333：**排队拿烘位**（同时只烘一个组件，见 BAKE_SLOT 的注释）。
        long waitFrom = SystemClock.elapsedRealtime();
        try {
            BAKE_SLOT.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return;
        }
        boolean counted = false;
        try {
            long waited = SystemClock.elapsedRealtime() - waitFrom;
            if (waited >= 300L) {
                Diag.warn(context, "W13", "预热排队 组件=" + widgetId + " 等=" + waited
                        + "ms（同时只烘一个：并发时 18 线程抢 8 核，总吞吐反而低 20%+）");
            }
            // 排队期间画布可能又变了、或者被取消了：拿到烘位后必须再确认一次
            if (!stillCurrent(widgetId, gen)) {
                return;
            }
            YIELDED = false;
            BAKING.incrementAndGet();
            counted = true;
            bakeBody(context, config, width, height, stamp, gen, count);
        } finally {
            if (counted) {
                BAKING.decrementAndGet();
            }
            BAKE_SLOT.release();
        }
    }

    // v1.10.422：整套「升档推迟」（Upgrade / PENDING_UPGRADES / deferUpgrade / flushPendingUpgrades /
    // drainUpgrades / upgradePass）已删除 —— 烘焙现在只有一遍，导出时按目标质量直出。
    // 删除理由记录在 LESSONS 176：那套机制只是为了"导出后立刻能播"，
    // 而用户的口径是"导出时一次烘完"，两者的目标互斥，留着就是死代码。

    /** v1.10.333：真正的烘帧体（烘位已拿到；拆出来只为让 try/finally 包得干净）。 */
    /**
     * v1.10.469（#4）：把这一代的逐帧 WebP 拼成**一个**动图文件（`anim.webp`，与帧文件同目录）。
     *
     * <p>为什么放在烘焙线程里、烘完之后：这些帧本来就全是热的（刚写完），顺序读一遍写一个文件
     * 是纯 IO；而它只在「导出/预热」期间发生一次，不在播放路径上。</p>
     *
     * <p>失败只打日志：这一版播放链还是逐帧推送，动图文件先落盘观察，
     * 下一版才把 `setImageViewUri` 换成一个动图 URI。</p>
     */
    private static void packAnim(@NotNull Context context, @NotNull WidgetConfig config,
                                 int width, int height, int count) {
        try {
            long started = SystemClock.elapsedRealtime();
            File dir = WidgetUriPush.bakedDir(context, config, width, height);
            java.util.List<File> frames = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                File f = WidgetUriPush.bakedFrame(context, config, i, width, height);
                if (!f.isFile() || f.length() <= 0L) {
                    Diag.warn(context, "W44", "动图打包放弃：第 " + i + " 帧不在（组件="
                            + config.getWidgetId() + " 帧数=" + count + "）");
                    return;
                }
                frames.add(f);
            }
            File out = new File(dir, AnimPack.NAME);
            int n = AnimPack.packFrames(frames,
                    WidgetStore.INSTANCE.fpsX10(context, config.getWidgetId(), config.getFps()), out);
            long spent = SystemClock.elapsedRealtime() - started;
            if (n <= 0) {
                Diag.warn(context, "W44", "动图打包失败（" + AnimPack.reason(n) + "）组件="
                        + config.getWidgetId() + " 帧数=" + count + " 用时=" + spent + "ms");
                return;
            }
            // v1.10.472：换代了 —— 让 AnimPush 的判定缓存立刻失效，下一次推帧就会用新文件
            AnimPush.forget(config.getWidgetId());
            Diag.warn(context, "W44", "动图打包 组件=" + config.getWidgetId() + " 帧=" + n
                    + " 每帧=" + AnimPack.frameDurationMs(config.getFps()) + "ms"
                    + " 大小=" + (out.length() / 1024L) + "KB"
                    // v1.10.473：每帧均值 —— 判「文件为什么这么大」就看这一个数
                    + " 均=" + (out.length() / Math.max(n, 1) / 1024L) + "KB/帧"
                    + " 画布=" + width + "x" + height
                    + " 用时=" + spent + "ms");
        } catch (Throwable th) {
            Diag.warn(context, "W44", "动图打包异常: " + th.getClass().getSimpleName());
        }
    }

    private static void bakeBody(@NotNull Context context, @NotNull WidgetConfig config,
                                 int width, int height, @NotNull String stamp, int gen, int frames) {
        int widgetId = config.getWidgetId();
        int count = frames > 0 ? frames : config.getFrameCount();
        if (count <= 0) {
            Diag.warn(context, "W13", "预热跳过（bakeBody）：帧数为 0 组件=" + widgetId);
            return;
        }
        long started = SystemClock.elapsedRealtime();
        BATCH_CPU_NS.set(0L);
        BATCH_LOAD_MS.set(0L);
        int quality = WidgetUriPush.encodeQuality(config);
        // v1.10.120：3 → 4 路。实测 3 路在 597×626 上只有 28~29 帧/秒（比播放需要的 30
        // 还慢一丝），冷启动永远填不平；加一路才有余量在播放需要之前先烘出来。
        // v1.10.191：改由设置页「性能 → 预热线程数」决定（4~8，默认自动 = 上式）。
        int workers = effectiveThreads(context);
        long[] prepared = WidgetUriPush.prepare(context, config, width, height);
        // v1.10.288：**从播放头往前烘，而不是从第 0 帧**。
        // 病根（12:07:17 W10：命中=0% 跳过=77/86）：按 0→N 烘，播放头在 259 时新画布只有
        // 0..几十帧、被作废的旧批次只有 0..221 —— nearestCached 只往**回**找 8 帧、
        // nearestLoose 只在 ±8 里找，全落空，于是每拍"跳过"、桌面冻住，直到播放头绕回开头。
        // 现在起点 = 当前播放头 + 10 帧（≈0.33s，够第一波 workers 烘完），之后环形往后烘；
        // 烘速（实测 34fps）略高于播放（30fps），前沿应能一直领先播放头，冻结窗口 ≈ 0.3s。
        // 总量不变（仍是全盘），只是顺序变了。
        final int startAt = (Math.max(0, PlaybackService.lastIndex(widgetId)) + 10) % count;
        long[] hitRec = FIRST_HIT.get(Integer.valueOf(widgetId));   // v1.10.289
        if (hitRec != null && hitRec[1] == 0L) {
            hitRec[1] = SystemClock.elapsedRealtime();
        }
        Diag.warn(context, "W13", "预热开始 组件=" + widgetId + " 帧数=" + count
                + " 起点=" + startAt
                + " 画布=" + width + "x" + height
                // v1.10.452：**烘焙一开始就把"要读的帧到底在不在"写进日志**。
                // 450/451 花了两个版本才从"完成=0 失败=235"反推出是读帧路径的问题，
                // 这一行让下一次一眼看完（正式目录 / 暂存目录各一个存在性）。
                + " 首帧文件=" + (WidgetStore.INSTANCE.frameFile(context, widgetId, 0).exists() ? "正式有" : "正式无")
                + "/" + (WidgetStore.INSTANCE.stagingFile(context, widgetId, "f", 0).exists() ? "暂存有" : "暂存无")
                + " 质量=" + (quality >= 100 ? "无损" : String.valueOf(quality))
                + " 线程=" + workers          // v1.10.497：就是真实开出来的线程数（没有「播放期间让路」了）
                + " 已热=" + (prepared[1] / 1024L) + "KB"
                + (prepared[0] > 0 ? " 旧画布=" + prepared[0] + "组(烘完再删)" : ""));
        // v1.10.459：**分层判据**。阴影/蒙版/边框与画面内容无关，300 帧里一模一样；
        // 这个指纹就是"overlay 能不能跨帧复用"的唯一判据。这一版只打日志，不改行为。
        try {
            Diag.warn(context, "W41", "分层指纹=" + FrameLayers.fingerprint(
                    WidgetRender.effLayout(config, new int[]{width, height}).name(),
                    config.getShape().name(), config.getShapeAlpha(), config.getBorder(),
                    config.getScalePercent(), config.getRotation(), config.getShapeRotation(),
                    config.getFitContent(), config.getShadowPercent(), config.getShadowCurve(),
                    config.getOffsetXPercent(), config.getOffsetYPercent(),
                    config.getContentScalePercent(), config.getMaskOffsetXPercent(),
                    config.getMaskOffsetYPercent(), width, height)
                    + " " + FrameLayers.summary(config.getShape().name(), config.getBorder(),
                    config.getShadowPercent(), config.getShapeRotation(), config.getShapeAlpha())
                    + " 帧=" + count + "（overlay 只由这些参数决定，跨帧可复用）");
        } catch (Throwable ignored) {
        }
        // v1.10.474：**播放口径绝对性检查**（用户的四项：清晰度 / FPS / 速度 / 时长）。
        // 放在预热真正开始之前打 —— 它判的是"这一代将要怎么播"，不是"播得怎么样"。
        try {
            int srcW = config.getFrameWidth() > 0 ? config.getFrameWidth() : width;
            int srcH = config.getFrameHeight() > 0 ? config.getFrameHeight() : height;
            int fpsX10 = WidgetStore.INSTANCE.fpsX10(context, widgetId, config.getFps());
            boolean fill = config.getLayout() == LayoutMode.FILL;
            PlaybackContract.Verdict v = PlaybackContract.check(srcW, srcH, width, height, fill,
                    fpsX10, count, AnimPack.frameDurations(count, fpsX10));
            // v1.10.497（外部评审 Q5 落地）：**画布 → 格子**的显示倍率也一起打。
            // 「清晰度=OK 却仍然糊」的唯一解释是这一项 >1：桌面 centerCrop 会把画布放大
            // max(格子/画布)。以前它只写在设置页的一句副标题里，现在有数字。
            String deskText = "";
            try {
                int[] box = WidgetRender.INSTANCE.widgetSize(context,
                        android.appwidget.AppWidgetManager.getInstance(context), widgetId);
                if (box != null && box.length >= 2 && box[0] > 0 && box[1] > 0) {
                    float cover = AspectMath.displayScale(width, height, box[0], box[1]);
                    if (cover > 0.0f) {
                        deskText = " 桌面=" + String.format(java.util.Locale.US, "%.2f", cover) + "×"
                                + (cover > AspectMath.DISPLAY_BLUR ? "(放大，会比画布糊)" : "")
                                + " 格子=" + box[0] + "x" + box[1];
                    }
                }
            } catch (Throwable ignored) {
            }
            Diag.warn(context, "W47", "播放口径 组件=" + widgetId
                    + " 源=" + srcW + "x" + srcH + "/" + (fpsX10 / 10.0f) + "fps/" + count + "帧"
                    + " 画布=" + width + "x" + height
                    + " " + v.summary()
                    + deskText
                    + (v.allOk() ? "" : "  ← 四项里有不成立项，改任何渲染/编码前先修这里"));
        } catch (Throwable th) {
            Diag.warn(context, "W47", "播放口径检查异常: " + th.getClass().getSimpleName());
        }
        progressBegin(widgetId, count);
        final AtomicInteger next = new AtomicInteger(0);
        // v1.10.421：逐帧指纹，只用来统计「码流里有多少连续重复帧」（那些帧播放时不需要解码）
        final java.util.concurrent.atomic.AtomicLongArray hashes =
                new java.util.concurrent.atomic.AtomicLongArray(count);
        final AtomicInteger done = new AtomicInteger(0);
        final AtomicInteger fail = new AtomicInteger(0);
        // v1.10.321（E：预热前段就绪即推）：
        // 以前必须等**全量**烘完才 repushOne —— 420 帧 / 26fps 的那十几秒里，桌面只能顶旧画布
        // 或者在等。现在每烘到 progStep 帧就补推一次（本批最多 2 次），推之前**必须**确认
        // 播放头那一帧已经在当前画布上（cachedUri != null）：否则 render() 会回落到旧画布，
        // 那就是用户报过的"导出后第一拍小一圈"。
        // 注意：这里只改"什么时候推"，不改画布尺寸 —— 画布一旦变小，vivo 会按 intrinsic
        // 把整张卡片量小一圈（v1.10.283~299 的老 bug），所以"面积/4 的草稿层"不能这么加。
        final int progStep = Math.max(24, count / 4);
        final AtomicInteger progAt = new AtomicInteger(progStep);
        final AtomicInteger progLeft = new AtomicInteger(2);
        // v1.10.339：两段烘的闸门（前段烘完 + 画布稳定 才放行剩余帧）
        final int frontFrames = count > FRONT_FRAMES * 2 ? FRONT_FRAMES : 0;
        final java.util.concurrent.atomic.AtomicBoolean phase2 =
                new java.util.concurrent.atomic.AtomicBoolean(frontFrames == 0);
        // v1.10.422（用户定的产品口径）：**不再是"先 q75 铺满、再找空闲升档"，而是导出时一遍按目标质量烘完**。
        //
        // 原来的两遍是为了「导出后立刻能播」：第一遍 q75 快、第二遍等没人看时再升 q93。
        // 代价是：加一个组件会让桌面抖 16 秒、升档还要挑熄屏窗口、日志里还会出现
        // 「升档推迟/升档未完成」这类只有我们自己看得懂的状态。
        // 现在反过来：**烘焙只发生在导出/导入期间**，那时应用盖在桌面上、根本不推帧，
        // 把核全给烘焙才是最快的 —— 用户等一次（有进度），回到桌面就是最终画质。
        //
        // 「这一代是否已经按目标质量烘过」用一个空标记文件记（`q93`），
        // 不匹配就整批强制覆盖（把以前留下的 q75 帧重写成 q93），匹配就整批跳过。
        final boolean batchHot = WidgetUriPush.upgradeDone(
                context, config, width, height, quality);
        WidgetUriPush.setPass(0, !batchHot);
        List<Thread> pool = new ArrayList<>(workers);
        for (int i = 0; i < workers; i++) {
            Thread t = new Thread(() -> work(context, config, width, height, gen, count,
                    startAt, next, done, fail, progAt, progStep, progLeft, frontFrames, phase2, hashes),
                    "anim-prewarm-" + widgetId + "-" + i);
            t.setPriority(Thread.NORM_PRIORITY);
            pool.add(t);
            t.start();
        }
        for (Thread t : pool) {
            try {
                t.join();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // v1.10.421：数连续重复帧（i 与 i+1 指纹相同 = 这两帧画面完全一样）。
        // 播放时这种帧可以直接沿用上一张、**一次解码都不用**。
        int dupFrames = 0;
        for (int i = 0; i < count; i++) {
            long a = hashes.get(i);
            long b = hashes.get((i + 1) % count);
            if (a != 0L && a == b) {
                dupFrames++;
            }
        }
        // v1.10.422：整批结束 —— 复位「这一遍」的开关，并在成功时写「这一代已按目标质量烘过」的标记。
        // 标记写进「这一代」的目录（目录名含渲染参数 + 素材指纹），换参数就自动失效。
        WidgetUriPush.setPass(0, false);
        final long spentPass1 = SystemClock.elapsedRealtime() - started;
        final int bakedPass1 = done.get();
        if (fail.get() == 0 && bakedPass1 >= count
                && stillCurrent(widgetId, gen)) {
            WidgetUriPush.markUpgradeDone(context, config, width, height, quality);
        }
        boolean current = stillCurrent(widgetId, gen);
        long spent = SystemClock.elapsedRealtime() - started;
        int baked = done.get();
        // v1.10.469（#4 · 第 1 步）：**这一代全部帧烘好之后**，顺手把它们拼成一个动图 WebP。
        // 只在「没失败帧 + 仍是当前代」时做；换画布/换素材会换目录，旧目录由 prepare 清掉。
        // 这一版只落盘、不切播放链（下一版才把多帧推送换成单 URI）。
        if (current && fail.get() == 0 && baked >= count) {
            packAnim(context, config, width, height, count);
            // v1.10.478：**编码/解码对拍**（W48）。P0-2（不透明内容帧 + 常驻 overlay）被外部两份
            // 复核判死之后，剩下唯一没被量过的账是「JPEG 解码到底比带 alpha 的 WebP 快多少」。
            // 取中间那一帧的**成品文件**（播放时真解的就是它），同一线程上比：
            // WebP 解码 vs JPEG 解码 vs 「JPEG + 常驻蒙版合成」。诊断失败什么都不打。
            try {
                int mid = count / 2;
                DecodeBench.probe(context,
                        WidgetUriPush.bakedFrame(context, config, mid, width, height),
                        widgetId, mid, count, width, height);
            } catch (Throwable ignored) {
            }
        }
        // v1.10.321（B）：用这一批的**实测吞吐**做自校准（作废批次不参与，不然会拿废数据压画质）。
        // 系数变了会打一行 W34，下一拍算画布时自动生效。
        // v1.10.412：**只用第一遍的数**（spentPass1 / bakedPass1）—— 第二遍故意跑在 nice10，
        // 把它算进吞吐等于"因为我用慢速度在后台升级画质，所以这个画布烘不动"→ 系数白掉一档。
        if (current && bakedPass1 > 0) {
            // v1.10.333：**只信「独占、且没给前台任务让过路」的批次**。
            // 实测（21:11:27~21:11:35 vs 21:12:21~21:12:27，同一画布 633x615、同一素材 290 帧）：
            //   311 这批和 312 的导出撞在一起 → 编码均 136ms、实测 32 帧/秒、系数 1.00→0.57；
            //   313 独占                 → 编码均  88ms、实测 50 帧/秒。
            // 1.55 倍的差距全在「当时机器闲不闲」上，跟画布大小一点关系都没有 ——
            // 拿这种数去压画质，等于因为旁边有人在搬东西就判定自己画得太大。
            boolean alone = BAKING.get() <= 1 && !YIELDED;
            String why = YIELDED ? "前台让路" : (BAKING.get() > 1 ? "并发预热" : "");
            // v1.10.334：1:1 档（和静态图）根本就不看系数 —— 那就不许再算、也不许再打
            // 「系数 0.55→0.57」这种行。实测 21:51:33 就出现过一条：画布 662x643 明明是
            // 1:1 的格子像素、跟系数毫无关系，日志却在报系数变化 —— 读日志的人会以为
            // 它起作用了（这正是"日志文案与代码不一致等于说谎"）。
            if (BakeLadder.calibrated(WidgetRender.animQuality(context, config), config.getFrameCount())) {
                BakeCal.note(context, widgetId, config.getFps(), spentPass1, bakedPass1, alone, why);
            } else {
                BakeCal.resetIfStale(context, widgetId);
            }
        }
        // v1.10.168（反馈 ①）：缓存体积拆成「本组 + 旧」。
        // 只给总量会误导：换画布时旧目录要「烘完再删」，总量里同时包含新旧两套，
        // 于是出现过「画布 290→276（变小），缓存 1615KB→3130KB（变大）」这种貌似矛盾的数字。
        // 现在分别四舍五入再相加，保证「本组 + 旧 = 合计」这条加法恒成立（各自误差 ≤0.5KB）。
        long mineB = WidgetUriPush.cacheBytesOf(context, widgetId, stamp);
        long totalB = WidgetUriPush.cacheBytes(context, widgetId);
        long oldB = Math.max(0L, totalB - mineB);
        long mineK = (mineB + 512L) / 1024L;
        long oldK = (oldB + 512L) / 1024L;
        // v1.10.188（W27）：把「一帧 130ms 花在哪」摊开 —— 阴影 / 内容 / 其它 / 编码。
        // 只在真有样本时打，且每次预热只打一条，不刷屏。
        String mix = FrameComposer.takeStats();
        String enc = WidgetUriPush.takeEncodeStats();
        int cpuPct = 0;
        if (spent > 0L && workers > 0) {
            cpuPct = (int) Math.round(BATCH_CPU_NS.get() * 100.0
                    / (spent * (double) workers * 1000000.0));
        }
        long loadAvg = baked > 0 ? (BATCH_LOAD_MS.get() / Math.max(baked, 1)) : 0L;
        if (!mix.isEmpty()) {
            Diag.warn(context, "W27", "合成分解 " + mix
                    + (enc.isEmpty() ? "" : " " + enc)
                    + " 读帧均=" + loadAvg + "ms"
                    // v1.10.419（外部复核指出的口径错误）：`编码均=` 是**整个 writeEncoded**
                    // （mkdirs + open + compress + flush + close + rename，含 IO），
                    // `编码CPU=` 只包住 compress 那一段；312ms 不能除以线程数当 CPU 用。
                    // 这里再补一个「每帧线程 CPU 时间」，它才是排除了等待的真成本。
                    + " 线程CPU均=" + (baked > 0
                            ? (BATCH_CPU_NS.get() / Math.max(baked, 1) / 1000000L) : 0L) + "ms"
                    + " 画布=" + width + "x" + height + " 组件=" + widgetId);
        }
        Diag.warn(context, "W13", "预热完成 组件=" + widgetId + " 画布=" + width + "x" + height
                + " 完成=" + baked + " 失败=" + fail.get()
                // v1.10.339：分两段时写出前段帧数（拖动中被作废的批次会显示 完成≈48）
                + (frontFrames > 0 ? " 前段=" + frontFrames : "")
                // v1.10.412：**吞吐判据只看第一遍**。第二遍（升档）故意跑在 nice10，
                // 它慢不代表这个画布烘不动；把两遍合起来算，读者会看到「速率 19 帧/秒」
                // 然后误判「画布太大」，实际上第一遍已经是 38 帧/秒。
                + " 首遍=" + bakedPass1 + " 用时=" + spentPass1 + "ms"
                + " 速率=" + (spentPass1 > 0 ? ((bakedPass1 * 1000L) / spentPass1) : 0) + "帧/秒"
                // v1.10.421：连续重复帧数（占全片比例）。这些帧播放时不需要解码。
                + " 重复=" + dupFrames + "/" + count + "(" + Math.round(dupFrames * 100.0 / Math.max(count, 1)) + "%)"
                // v1.10.335：CPU 占用率（线程 CPU 时间 ÷ 线程数×墙钟）。<85% 就是被抢占/限核
                + " CPU=" + cpuPct + "%"
                // v1.10.153：q95/q97 的唯一差别就是这个体积（帧率和推最慢都不会变）
                + " 缓存=本组" + mineK + "KB+旧" + oldK + "KB=合计" + (mineK + oldK) + "KB"
                + (current ? "" : "（画布/配置已变更，本批作废）"));
        // v1.10.261：**烘完必须重推一次**。静态图只推一次，如果它那次正赶上「还没烘好」，
        // 推上去的是旧画布的邻帧，桌面就会永远停在放大的旧帧上（实测 419×626 放大到 2.46×）。
        //
        // v1.10.397：**动图不补推**。动图每一拍都会自己推下一帧（≤33ms），
        // 这里再插一帧等于在时间轴上插了个重复帧 —— 录屏里「第 2 轮播到 3/4 毫无征兆抖一下」
        // 就是预热完成那一刻插进来的。补推只对「一辈子只推一次」的静态图有意义。
        if (current && config.getFrameCount() <= 1) {
            WidgetRender.INSTANCE.repushOne(context, widgetId, "预热完成");
        }
        if (current && fail.get() > 0) {
            // 有失败帧：撤掉「已热」记忆，下一帧渲染时再补一次（已成功的帧会跳过）。
            // 每个指纹只补一次，避免某一帧永久坏掉 → 每次渲染都重开一轮。
            boolean retried;
            synchronized (GENERATION) {
                retried = stamp.equals(RETRIED.get(Integer.valueOf(widgetId)));
                RETRIED.put(Integer.valueOf(widgetId), stamp);
                if (!retried) {
                    READY.remove(Integer.valueOf(widgetId));
                }
            }
        }
    }

    /**
     * v1.10.421：一帧的「廉价指纹」（每 1024 个像素取一个，再按 FNV 混一次）。
     * 用来数连续重复帧，**不参与任何写盘/判据**。取样 900 次 ≈ 0.05ms。
     */
    private static long frameHash(@NotNull Bitmap bitmap) {
        try {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            long hash = -3750763034362895579L;   // FNV-1a 64 偏移
            int step = 1024;
            for (int y = 0; y < h; y += 4) {
                for (int x = 0; x < w; x += step / 4) {
                    int px = bitmap.getPixel(x, y);
                    hash ^= (px & 0xFFFFFFFFL);
                    hash *= 1099511628211L;
                }
            }
            return hash;
        } catch (Throwable th) {
            return 0L;
        }
    }

    /**
     * v1.10.321（E）：预热前段就绪就补推一次。
     *
     * 只有三个条件同时满足才推：
     *   1. 已经烘到下一个检查点（每 {@code step} 帧一个，本批最多 2 次）；
     *   2. 本批仍然是"当前批次"（{@code stillCurrent} 在 repush 里也会被 render 复核）；
     *   3. **播放头那一帧已经在当前画布上** —— 否则 render() 会回落到旧画布，
     *      那正是用户报过的「导出后第一拍小一圈」。
     * 任何异常都吞掉：这是"提前变好"，失败只是退回原来的"等全量"。
     */
    private static void maybeRepushEarly(@NotNull Context context, @NotNull WidgetConfig config,
                                         int width, int height, @NotNull AtomicInteger done,
                                         @NotNull AtomicInteger progAt, int step,
                                         @NotNull AtomicInteger progLeft) {
        try {
            if (progLeft.get() <= 0) {
                return;
            }
            int at = progAt.get();
            if (done.get() < at) {
                return;
            }
            if (!progAt.compareAndSet(at, at + step)) {
                return;
            }
            if (progLeft.decrementAndGet() < 0) {
                return;
            }
            int widgetId = config.getWidgetId();
            int head = Math.max(0, PlaybackService.lastIndex(widgetId));
            if (WidgetUriPush.cachedUri(context, config, head, width, height) == null) {
                return;
            }
            // v1.10.397：同上 —— 动图不插帧（下一拍自然接上），只有静态图需要"变好"这一刻
            if (config.getFrameCount() <= 1) {
                WidgetRender.INSTANCE.repushOne(context, widgetId, "预热前段就绪");
            }
        } catch (Throwable ignored) {
        }
    }

    private static void work(@NotNull Context context, @NotNull WidgetConfig config,
                             int width, int height, int gen, int count, int startAt,
                             @NotNull AtomicInteger next, @NotNull AtomicInteger done,
                             @NotNull AtomicInteger fail,
                             @NotNull AtomicInteger progAt, int progStep,
                             @NotNull AtomicInteger progLeft,
                             int frontFrames, @NotNull java.util.concurrent.atomic.AtomicBoolean phase2) {
        work(context, config, width, height, gen, count, startAt, next, done, fail,
                progAt, progStep, progLeft, frontFrames, phase2, null);
    }

    /**
     * v1.10.421：多一个 {@code hashes} 参数（可空）。**只统计，不改行为**。
     *
     * <p>动机（用户问"能不解码吗"）：如果素材本身是 25fps 抽成 50fps，那相邻两帧**完全一样**，
     * 这些帧在播放时根本不需要解码 —— 直接沿用上一张就行，观感零损失。
     * 先量出「码流里到底有多少连续重复帧」，再决定要不要做"跳过重复帧的解码"。
     */
    private static void work(@NotNull Context context, @NotNull WidgetConfig config,
                             int width, int height, int gen, int count, int startAt,
                             @NotNull AtomicInteger next, @NotNull AtomicInteger done,
                             @NotNull AtomicInteger fail,
                             @NotNull AtomicInteger progAt, int progStep,
                             @NotNull AtomicInteger progLeft,
                             int frontFrames, @NotNull java.util.concurrent.atomic.AtomicBoolean phase2,
                             java.util.concurrent.atomic.AtomicLongArray hashes) {
        // v1.10.422：**只有一遍了**（导出时按目标质量直出），所以这里永远是 DEFAULT。
        // 原本第二遍要 nice10，是因为它和播放抢核；现在烘焙只发生在导出/导入期间，
        // 那时桌面被应用盖住、根本不推帧，把核全给它才最快。
        Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
        long cpu0 = android.os.Debug.threadCpuTimeNanos();
        long loadMs = 0L;
        try {
        while (true) {
            if (!stillCurrent(config.getWidgetId(), gen)) {
                return;
            }

            // 前台在导出/解析时让路（每次让 150ms，让出 CPU 给带进度条的那个任务）
            if (SystemClock.elapsedRealtime() < foregroundUntil) {
                YIELDED = true;   // v1.10.333：本批让过路 → 吞吐不可信，不参与自校准
                try {
                    Thread.sleep(150L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
                continue;
            }
            // v1.10.339：两段烘的闸门 —— 前段（≈1 秒播放量）烘完后先停一下，
            // 等画布真正稳定再继续。拖动时画布一变，gen 就被顶掉，
            // 这一炉只会白烧前段（≤48 帧 ≈0.3s），不再白烧 290 帧（4~11s）。
            if (!phase2.get()) {
                if (done.get() < frontFrames) {
                    // 前段还没烘完，继续干
                } else if (stableFor(config.getWidgetId()) >= FRONT_STABLE_MS) {
                    phase2.set(true);
                } else {
                    if (!stillCurrent(config.getWidgetId(), gen)) {
                        return;
                    }
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    continue;
                }
            }
            int n = next.getAndIncrement();
            if (n >= count) {
                return;
            }
            int index = (startAt + n) % count;   // v1.10.288：环形，从播放头开始
            // v1.10.422：这里只看「文件在不在」。**质量是否达标由 batchHot 统一决定** ——
            // 不达标时整批 setPass(force=true)（writeEncoded 里会强制覆盖），
            // 达标时整批跳过。把「在哪判断」收成一处，407~412 那个"第二遍空转"就不会再发生。
            if (!WidgetUriPush.passForce()
                    && WidgetUriPush.cachedUri(context, config, index, width, height) != null) {
                done.incrementAndGet();
                progressAt(config.getWidgetId(), done.get(), count);
                maybeRepushEarly(context, config, width, height, done, progAt, progStep, progLeft);
                continue;
            }
            Bitmap frame = null;
            Bitmap masked = null;
            try {
                long load0 = SystemClock.elapsedRealtime();
                frame = WidgetStore.INSTANCE.loadFrame(context, config, index, width, height);
                loadMs += SystemClock.elapsedRealtime() - load0;
                if (frame == null) {
                    // v1.10.452（LESSONS ②：任何 `return/continue` 都必须留日志）：
                    // 449~451 的日志里只有一句「完成=0 失败=235 用时=5ms」，读的人没法判断
                    // 是"文件不在"还是"解码失败"。这里把前 3 个失败帧的两条路径存在性直接打出来。
                    int failed = fail.incrementAndGet();
                    if (failed <= 3) {
                        Diag.warn(context, "W29", "读帧失败 index=" + index
                                + " 组件=" + config.getWidgetId()
                                + " 正式=" + WidgetStore.INSTANCE.frameFile(context, config.getWidgetId(), index).exists()
                                + " 暂存=" + WidgetStore.INSTANCE.stagingFile(context, config.getWidgetId(), "f", index).exists()
                                + " 配置帧数=" + config.getFrameCount() + "/本批=" + count);
                    }
                    continue;
                }
                masked = FrameComposer.INSTANCE.compose(frame, width, height,
                        // v1.10.278：与 render() 同一个判据 —— 否则预热会按 FIT 烘一批、
                        // 推的时候按 FILL 画，桌面就该显示两种卡片形状了。
                        WidgetRender.effLayout(config, new int[]{width, height}),
                        config.getShape(), config.getShapeAlpha(),
                        config.getBorder(), config.getScalePercent(), config.getRotation(),
                        config.getShapeRotation(), config.getFitContent(),
                        config.getShadowPercent(), config.getShadowCurve(), config.getOffsetXPercent(),
                        config.getOffsetYPercent(), config.getContentScalePercent(),
                        config.getMaskOffsetXPercent(), config.getMaskOffsetYPercent());
                if (masked != null && hashes != null) {
                    hashes.set(index, frameHash(masked));
                }
                if (masked == null || !WidgetUriPush.writeEncoded(context, config, index,
                        masked, width, height)) {
                    fail.incrementAndGet();
                } else {
                    // v1.10.482（B）：**分层产物**（c%05d.jpg + alpha.webp）。第 0 帧会顺便做
                    // 「JPEG + DST_IN(alpha) 还原旧帧」的逐像素自检，超过容差就整代回落。
                    try {
                        LayerFrames.write(context,
                                WidgetUriPush.bakedDir(context, config, width, height),
                                index, masked, width, height);
                    } catch (Throwable ignored) {
                    }
                    done.incrementAndGet();
                    progressAt(config.getWidgetId(), done.get(), count);
                    maybeRepushEarly(context, config, width, height, done, progAt, progStep, progLeft);
                }
            } catch (Throwable th) {
                fail.incrementAndGet();
            } finally {
                if (masked != null && !masked.isRecycled()) {
                    masked.recycle();
                }
                if (frame != null && !frame.isRecycled()) {
                    frame.recycle();
                }
            }
        }
        } finally {
            // v1.10.335：把这一路的 CPU 时间与读帧耗时并进本批的总账
            long cpu = android.os.Debug.threadCpuTimeNanos() - cpu0;
            if (cpu > 0L) {
                BATCH_CPU_NS.addAndGet(cpu);
            }
            if (loadMs > 0L) {
                BATCH_LOAD_MS.addAndGet(loadMs);
            }
        }
    }

    /** v1.10.339：这个组件的画布已经多久没变过了（毫秒）。 */
    private static long stableFor(int widgetId) {
        synchronized (GENERATION) {
            Long at = STAMP_AT.get(Integer.valueOf(widgetId));
            return at == null ? Long.MAX_VALUE / 4L
                    : (SystemClock.elapsedRealtime() - at.longValue());
        }
    }

    private static boolean stillCurrent(int widgetId, int gen) {
        synchronized (GENERATION) {
            Integer now = GENERATION.get(Integer.valueOf(widgetId));
            return now != null && now.intValue() == gen;
        }
    }
}
