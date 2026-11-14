package io.github.animwidget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * v1.10.410：共享位图通道的「预读环」—— <b>不依赖 Android</b>（泛型 T = 解好的位图），可在 JVM 上单测。
 *
 * <p>取代 v1.10.344~409 的「字符串键 LinkedHashMap + 每拍无条件重排 want+1/want+2」。
 * 旧逻辑的三个病（前两个是**源码级**事实，不需要日志）：
 * <ol>
 *   <li><b>重复解码</b>：只要 want 已在手，每拍无条件再排 +1、+2，而 {@code scheduleDecode}
 *       只看「在飞 ≥3 就不再排」，不查「已经在环里」→ 稳态下每个推出的帧被解了约 2 次，
 *       解码池一半的算力白烧。这是 §18 约束②（解码池容量）真正的吃水线。</li>
 *   <li><b>回退到「未来帧」</b>：{@code takeReady} 找不到 exact 时返回「插入顺序最后一张」，
 *       它可能是 want+2 → 画面先跳到未来、下一拍 exact 又回到过去（帧号回退）。</li>
 *   <li><b>预读被动</b>：只有 want 已在手才预读 +1/+2；want 缺的那一拍整条流水线是空的。</li>
 * </ol>
 *
 * <p>新规则：
 * <ul>
 *   <li>每拍以 want 为锚，窗口 = [want, want+lead]，只补「既不在手、也不在飞」的帧（零重复）。</li>
 *   <li>lead 自适应：lead = ceil(1.5×延迟均 / 帧间隔) + 1，夹在 [2,5]，再受内存预算夹住。
 *       「延迟」= 提交→完成（含排队），所以解码池一忙 lead 自己变大。</li>
 *   <li>取帧：exact → 往<b>回</b>最多 3 帧的最近邻 → 否则 HOLD（保持屏上那张，不推）→ 环空 EMPTY（调用方走 URI 兜底）。
 *       <b>永远不取未来帧</b>。</li>
 *   <li>连续 HOLD 超过 {@link #HOLD_LIMIT} 拍 → {@link Pick#holdStreak} 超过上限，调用方必须放弃 HOLD
 *       退回 URI —— 宁可贵一帧，也不许冻屏（这是 C 家补丁没有的自愈阀）。</li>
 *   <li>窗口外的帧立即淘汰（内存 = (lead+BEHIND_KEEP+1)×帧字节）；排队中的任务开跑前复核「还在窗口里吗」，
 *       不在就丢弃（过载时自动卸载，而不是越排越长）。</li>
 * </ul>
 */
final class FrameRing<T> {

    /**
     * v1.10.486：**3**（原 2）。
     *
     * <p>真机数据（485 的 W23）：分层帧让单帧解码从 13~14ms 降到 10~11ms，
     * `lead = ceil(1.5×延迟/间隔)+1` 于是自动从 3 掉到 2 —— 看着是好事，实际上
     * **两路的预读窗口重叠随之变小**，`共享帧=` 命中率从 48% 掉到 **17~26%**，
     * 解码次数反而从 ~160 涨到 240。两个优化互相抵消。
     *
     * <p>预读提前量是「跨组件共享」的燃料：窗口越大，另一路来要同一帧时它越可能已经在表里。
     * 所以把下限抬到 3（多留一条预读帧，720p 约 +3.7MB/路），让这条收益不再被延迟下降吃掉。</p>
     */
    static final int MIN_LEAD = 3;
    static final int MAX_LEAD = 5;
    /** 环里最多保留「播放头之后 lead 帧 + 之前 BEHIND_KEEP 帧」。 */
    static final int BEHIND_KEEP = 2;
    /** 取不到 exact 时，往回最多找几帧。 */
    static final int BEHIND_TAKE = 3;
    /**
     * 连续这么拍都只拿得到 HOLD 就不许再保持了（80ms@50fps）。
     * 冻屏比掉帧难看得多，所以宁可退回 URI 让桌面解一次。
     */
    static final int HOLD_LIMIT = 4;

    /** 全部组件的环内存预算下限（共享内存）。 */
    static final long MIN_BUDGET = 96L << 20;
    /**
     * 上限 192MB。<b>为什么不是固定 96MB</b>：1920×1080 一帧 8.3MB，
     * 96MB÷3 组件 = 32MB/路 = 只够 3 帧 → lead 被夹到 1 → 22ms 的解码压着 20ms 的拍，
     * 每拍都是「回」或「保持」（N=3 时必现）。预算必须随「实际活跃路数 × 单帧字节」长。
     */
    static final long MAX_BUDGET = 192L << 20;
    /** 超过这么久没来取帧的组件不再占预算。 */
    private static final long ACTIVE_MS = 3000L;

    static final int EXACT = 0;
    static final int BEHIND = 1;
    static final int HOLD = 2;
    static final int EMPTY = 3;

    /** 一次 {@link #step} 的结果。 */
    static final class Pick<T> {
        T frame;
        int kind = EMPTY;
        int behind;
        int epoch;
        int lead;
        /** 已经连续第几拍 HOLD（0 = 本拍不是 HOLD）。> {@link #HOLD_LIMIT} = 调用方必须退回 URI。 */
        int holdStreak;
        /** 需要提交解码的帧号（按紧迫度升序），长度 nSubmit。 */
        final int[] submit = new int[MAX_LEAD + 1];
        /** 与 submit 对应的紧迫度（0 = 现在就要）。 */
        final int[] dist = new int[MAX_LEAD + 1];
        int nSubmit;
        /** v1.10.481：本拍的总帧数（共享帧的键里要用它归一化帧号）。 */
        int count;
    }

    private static final class Lane<T> {
        String stamp;
        int count;
        int epoch;
        final HashMap<Integer, T> ready = new HashMap<>();
        final HashSet<Integer> pending = new HashSet<>();
        /** 解码失败（常见：这一帧第一遍烘焙还没写出来）后的冷却：避免每拍重试。 */
        final HashMap<Integer, Long> retryAt = new HashMap<>();
        int lastWant = -1;
        int lead = MIN_LEAD;
        int holdStreak;
        double intervalMs = 20.0;
        double latEwma;          // 提交→完成（含排队）
        double svcEwma;          // 纯解码耗时
        long frameBytes = 4L << 20;
        long budgetNow = MIN_BUDGET;
        long lastTouch;
        // 窗口统计（snapshot(true) 清零）
        int exact, behind, hold, empty, drop, done, fail, maxLead, maxRing;
    }

    private final HashMap<Integer, Lane<T>> lanes = new HashMap<>();
    /** >0 = 固定预算（单测/显式指定）；<=0 = 按活跃路数自适应。 */
    private final long fixedBudget;

    FrameRing() {
        this(-1L);
    }

    FrameRing(long budgetBytes) {
        this.fixedBudget = budgetBytes;
    }

    /** 当前生效的预算（W36 要用它算环内存占用）。 */
    long budgetNow() {
        return fixedBudget > 0L ? fixedBudget : MIN_BUDGET;
    }

    private long budgetFor(int active, long frameBytes) {
        if (fixedBudget > 0L) {
            return Math.min(fixedBudget, MAX_BUDGET);
        }
        long need = active * (MIN_LEAD + BEHIND_KEEP + 1) * Math.max(frameBytes, 1L);
        return Math.min(MAX_BUDGET, Math.max(MIN_BUDGET, need));
    }

    /** 取一帧并规划预读。{@code nowMs} 由调用方传入（单测可控）。 */
    synchronized Pick<T> step(int id, String stamp, int count, int want, double intervalMs, long nowMs) {
        Pick<T> p = new Pick<>();
        p.count = count;
        if (count <= 0 || want < 0) {
            return p;
        }
        want = want % count;
        Lane<T> l = lanes.get(id);
        if (l == null) {
            l = new Lane<>();
            l.stamp = stamp;
            l.count = count;
            lanes.put(id, l);
        } else if (!stamp.equals(l.stamp) || l.count != count) {
            // 画布换代 / 帧数变了：整环作废（在飞的任务靠 epoch 失效）
            l.epoch++;
            l.ready.clear();
            l.pending.clear();
            l.retryAt.clear();
            l.stamp = stamp;
            l.count = count;
            l.lastWant = -1;
            l.holdStreak = 0;
        }
        l.lastTouch = nowMs;
        l.lastWant = want;
        l.intervalMs = Math.max(intervalMs, 1.0);
        int active = 0;
        for (Lane<T> o : lanes.values()) {
            if (nowMs - o.lastTouch <= ACTIVE_MS) {
                active++;
            }
        }
        active = Math.max(active, 1);
        long bud = budgetFor(active, l.frameBytes);
        l.budgetNow = bud;
        int lead = leadFor(l.latEwma, l.intervalMs, bud, active, l.frameBytes, count);
        l.lead = lead;
        if (lead > l.maxLead) {
            l.maxLead = lead;
        }
        p.lead = lead;
        p.epoch = l.epoch;

        // 1) 取帧：exact → 往回最近邻 → HOLD / EMPTY（绝不取未来帧）
        T exact = l.ready.get(Integer.valueOf(want));
        if (exact != null) {
            p.frame = exact;
            p.kind = EXACT;
            l.exact++;
            l.holdStreak = 0;
        } else {
            for (int b = 1; b <= BEHIND_TAKE && b < count; b++) {
                T t = l.ready.get(Integer.valueOf(((want - b) % count + count) % count));
                if (t != null) {
                    p.frame = t;
                    p.kind = BEHIND;
                    p.behind = b;
                    l.behind++;
                    break;
                }
            }
            if (p.frame != null) {
                l.holdStreak = 0;
            } else if (l.ready.isEmpty()) {
                p.kind = EMPTY;
                l.empty++;
                l.holdStreak = 0;
            } else {
                p.kind = HOLD;
                l.hold++;
                l.holdStreak++;
                p.holdStreak = l.holdStreak;
                if (l.holdStreak > HOLD_LIMIT) {
                    l.holdStreak = 0;       // 自愈：这一拍调用方会退回 URI，下拍重新开始计
                }
            }
        }

        // 2) 规划：窗口里「不在手也不在飞」的才提交（零重复）
        for (int d = 0; d <= lead; d++) {
            int idx = (want + d) % count;
            if (!l.ready.containsKey(Integer.valueOf(idx)) && !l.pending.contains(Integer.valueOf(idx))) {
                Long cool = l.retryAt.get(Integer.valueOf(idx));
                if (cool != null) {
                    if (cool.longValue() > nowMs) {
                        continue;
                    }
                    l.retryAt.remove(Integer.valueOf(idx));
                }
                l.pending.add(Integer.valueOf(idx));
                p.submit[p.nSubmit] = idx;
                p.dist[p.nSubmit] = d;
                p.nSubmit++;
            }
        }

        // 3) 淘汰窗口外的帧（只丢引用；共享位图可能还在 Binder 里，绝不 recycle）
        if (!l.ready.isEmpty()) {
            List<Integer> drop = null;
            for (Integer k : l.ready.keySet()) {
                int ahead = ((k.intValue() - want) % count + count) % count;
                int back = ((want - k.intValue()) % count + count) % count;
                if (ahead > lead && back > BEHIND_KEEP) {
                    if (drop == null) {
                        drop = new ArrayList<>();
                    }
                    drop.add(k);
                }
            }
            if (drop != null) {
                for (Integer k : drop) {
                    l.ready.remove(k);
                }
            }
        }
        if (l.ready.size() > l.maxRing) {
            l.maxRing = l.ready.size();
        }
        return p;
    }

    /** 解码任务开跑前复核：这一帧现在还在窗口里吗（不在 → 丢弃，别白烧 CPU）。 */
    synchronized boolean wanted(int id, int epoch, int idx) {
        Lane<T> l = lanes.get(id);
        if (l == null || l.epoch != epoch || l.lastWant < 0) {
            return false;
        }
        int ahead = ((idx - l.lastWant) % l.count + l.count) % l.count;
        return ahead <= l.lead + 1;
    }

    /** 解码任务结束（成功 frame!=null；失败/丢弃 frame==null）。返回 true = 已入环。 */
    synchronized boolean complete(int id, int epoch, int idx, T frame, long latencyMs, long svcMs,
                                  long frameBytes, boolean dropped, long nowMs) {
        Lane<T> l = lanes.get(id);
        if (l == null || l.epoch != epoch) {
            return false;
        }
        l.pending.remove(Integer.valueOf(idx));
        if (dropped) {
            l.drop++;
            return false;
        }
        if (frame == null) {
            l.fail++;
            l.retryAt.put(Integer.valueOf(idx), Long.valueOf(nowMs + (long) (2.0 * l.intervalMs)));
            return false;
        }
        l.done++;
        if (frameBytes > 0L) {
            l.frameBytes = frameBytes;
        }
        // 快攻慢衰：突发变慢要立刻让 lead 涨，恢复要慢一点
        l.latEwma = ewma(l.latEwma, latencyMs);
        l.svcEwma = ewma(l.svcEwma, svcMs);
        l.ready.put(Integer.valueOf(idx), frame);
        if (l.ready.size() > l.maxRing) {
            l.maxRing = l.ready.size();
        }
        return true;
    }

    private static double ewma(double old, double x) {
        if (old <= 0.0) {
            return x;
        }
        return x > old ? (0.5 * x + 0.5 * old) : (0.1 * x + 0.9 * old);
    }

    /**
     * lead = ceil(1.5×延迟/间隔)+1，夹 [MIN_LEAD,5]，再被内存预算与帧数夹住（最小 1）。
     *
     * <p><b>v1.10.494：加两道「雪崩闸」。</b></p>
     *
     * <p>真机六路日志（10:09:47 那一个窗口）出现过一次雪崩：
     * {@code 解码均27ms 延迟均65ms 滞后132 弃62 排队峰29 环[回74/空6]} ——
     * 因为 lead 是按延迟自适应的（延迟越大预读越深），而 N 路一起变慢时，
     * 「延迟↑ → 预读↑ → 队列更深 → 延迟↑」是个**正反馈**，一进去就出不来。</p>
     *
     * <ol>
     *   <li><b>积压闸</b>：延迟超过 3 个帧间隔（= 已经排到第 4 帧了）时，lead 直接压到 1 ——
     *       这时候该做的是「少要一点」，不是「多要一点」。</li>
     *   <li><b>路数闸</b>：lead 上限随活跃路数收窄（{@code 6 - 路数}），6 路时最多 0 → 取 1；
     *       4 路时最多 2。解码池是共享的，路越多每路越不该贪。</li>
     * </ol>
     */
    static int leadFor(double latMs, double intervalMs, long budget, int activeLanes, long frameBytes, int count) {
        // 积压闸：延迟超过 3 拍 ⇒ 只保底 1 帧预读（先止血，别再加压）
        if (latMs >= 3.0 * Math.max(intervalMs, 1.0)) {
            return 1;
        }
        int lead;
        if (latMs <= 0.0) {
            lead = MIN_LEAD;
        } else {
            lead = (int) Math.ceil(1.5 * latMs / Math.max(intervalMs, 1.0)) + 1;
            lead = Math.max(MIN_LEAD, Math.min(MAX_LEAD, lead));
        }
        // 路数闸：解码池是共享的，活跃路数越多，每路预读越浅
        int laneCap = Math.max(0, 6 - Math.max(activeLanes, 1));
        lead = Math.min(lead, Math.max(laneCap, 1));
        long perLane = budget / Math.max(activeLanes, 1);
        int memLead = (int) Math.max(1L, perLane / Math.max(frameBytes, 1L) - (BEHIND_KEEP + 1));
        lead = Math.min(lead, memLead);
        lead = Math.min(lead, Math.max(count - 1, 0));
        return Math.max(lead, 0);
    }

    /** 单个组件的窗口统计快照。 */
    static final class LaneStat {
        int id;
        int fps;
        int lead;
        /** 当前（未清零）的提前量，容量模型用它估环内存。 */
        int curLead;
        int ringMax;
        long frameBytes;
        long budgetNow;
        double latMs;
        double svcMs;
        int exact, behind, hold, empty, drop, done, fail;
    }

    /** 取各组件窗口统计；{@code reset}=true 时清零计数（EWMA 保留）。仅统计活跃组件。 */
    synchronized List<LaneStat> snapshot(boolean reset, long nowMs) {
        List<LaneStat> out = new ArrayList<>();
        for (java.util.Map.Entry<Integer, Lane<T>> e : lanes.entrySet()) {
            Lane<T> l = e.getValue();
            if (nowMs - l.lastTouch > ACTIVE_MS) {
                continue;
            }
            LaneStat s = new LaneStat();
            s.id = e.getKey().intValue();
            s.fps = (int) Math.round(1000.0 / Math.max(l.intervalMs, 1.0));
            s.lead = l.maxLead;
            s.curLead = l.lead;
            s.ringMax = l.maxRing;
            s.frameBytes = l.frameBytes;
            s.budgetNow = l.budgetNow;
            s.latMs = l.latEwma;
            s.svcMs = l.svcEwma;
            s.exact = l.exact;
            s.behind = l.behind;
            s.hold = l.hold;
            s.empty = l.empty;
            s.drop = l.drop;
            s.done = l.done;
            s.fail = l.fail;
            out.add(s);
            if (reset) {
                l.exact = l.behind = l.hold = l.empty = l.drop = l.done = l.fail = 0;
                l.maxLead = 0;
                l.maxRing = l.ready.size();
            }
        }
        return out;
    }

    /** 组件被删/换：释放它的环。 */
    synchronized void forget(int id) {
        Lane<T> l = lanes.remove(Integer.valueOf(id));
        if (l != null) {
            l.epoch++;
            l.ready.clear();
            l.pending.clear();
            l.retryAt.clear();
        }
    }
}
