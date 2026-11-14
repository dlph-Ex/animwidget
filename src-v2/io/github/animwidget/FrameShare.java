package io.github.animwidget;

import android.graphics.Bitmap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * v1.10.481（P0-3 / A）：**同素材共享解码 N→1** 的帧表。
 *
 * <h3>这条改动解决什么</h3>
 * `传输=位图共享` 这条通道上，每帧的解码成本全在我们自己进程里
 * （`DecodeTask → decodeFrameFile → ImageDecoder(ALLOCATOR_SHARED_MEMORY)`，实测 720p ≈ 12~13ms）。
 * 一个组件 50fps 就吃掉约 0.6 个核；**同一个素材放 N 个组件就是 N 份解码** ——
 * 而它们的像素完全一样。这里就是那张「解一次、N 路共用」的表。
 *
 * <h3>为什么只做「结果缓存」，不做「同一帧并发去重」</h3>
 * 预读环（{@link FrameRing}）每拍会提交 [want, want+lead] 这个窗口，两个组件即使起步时间不同，
 * 窗口也会大量重叠（lead 由延迟自适应，实测 3~5 帧）。所以第二个组件来要同一帧时，
 * 第一份**通常已经解完并放进这张表**。加锁等待（single-flight）能再多省一点，但会把
 * 解码线程堵在别人的解码上 —— 堵住的代价是**掉帧**，而掉帧是用户唯一不能接受的东西
 * （见 W47 的「fps 不许降」口径）。所以这里刻意只做「缓存」，宁可多解一次，不许等。
 *
 * <h3>生命周期（为什么绝不 recycle）</h3>
 * 与 {@link FrameRing} 同一个口径：**只丢引用，绝不回收**。
 * 共享位图可能同时在两个组件的 RemoteViews 里、甚至已经过 Binder 在桌面进程里；
 * `recycle()` 一次就是「另一路黑屏」。表被淘汰的条目只是不再被查到，位图交给 GC/finalizer。
 *
 * <p>内存：上限 {@link #MAX_BYTES}（24MB）+ {@link #MAX_ENTRIES}（12 帧），按访问顺序 LRU。
 * 720p 一帧 3.7MB ⇒ 实际约 6 帧，正好覆盖两路的重叠窗口。</p>
 */
final class FrameShare {

    /** 表的上限。720p 一帧 3.7MB ⇒ 约 6 帧；再大就只是白占内存（环里已经有预读了）。 */
    // v1.10.494：40MB → 56MB。六路时预读窗口重叠更多，表越大命中越高（720p 一帧 3.7MB ⇒ 约 15 帧）
    static final long MAX_BYTES = 56L << 20;
    static final int MAX_ENTRIES = 16;

    /** 访问顺序 = true：get 也算「用过」，淘汰的是最久没被要过的帧。 */
    private static final LinkedHashMap<String, Bitmap> LIVE =
            new LinkedHashMap<>(16, 0.75f, true);

    private static long bytes;

    private static final AtomicLong HITS = new AtomicLong();
    private static final AtomicLong ASKS = new AtomicLong();
    private static final AtomicLong PUTS = new AtomicLong();
    private static final AtomicLong EVICTS = new AtomicLong();
    /** v1.10.488：「同一帧去重」命中的次数（不算表命中，但省掉的解码一样多）。 */
    private static final AtomicLong FLIGHT_HITS = new AtomicLong();

    private FrameShare() {
    }

    /** 查一帧。命中返回**不可回收**的位图（调用方只许读）。 */
    @Nullable
    static Bitmap get(@Nullable String key) {
        if (key == null) {
            return null;
        }
        synchronized (LIVE) {
            ASKS.incrementAndGet();
            Bitmap b = LIVE.get(key);
            if (b != null && !b.isRecycled()) {
                HITS.incrementAndGet();
                return b;
            }
            if (b != null) {
                LIVE.remove(key);            // 被别处回收过（理论上不会，兜底）
            }
            return null;
        }
    }

    /** 解完一帧后放进来（同键已存在就丢弃新的那一份，绝不替换正在被读的对象）。 */
    static void put(@Nullable String key, @Nullable Bitmap b) {
        if (key == null || b == null || b.isRecycled()) {
            return;
        }
        long size = 0L;
        try {
            size = b.getAllocationByteCount();
        } catch (Throwable ignored) {
            size = (long) b.getWidth() * (long) b.getHeight() * 4L;
        }
        synchronized (LIVE) {
            PUTS.incrementAndGet();
            if (LIVE.containsKey(key)) {
                return;
            }
            LIVE.put(key, b);
            bytes += size;
            evictLocked();
        }
    }

    /** 换代/关掉时清空（只清引用，不 recycle —— 见类注释）。 */
    static void clear() {
        synchronized (LIVE) {
            LIVE.clear();
            bytes = 0L;
        }
    }

    private static void evictLocked() {
        Iterator<java.util.Map.Entry<String, Bitmap>> it = LIVE.entrySet().iterator();
        while (it.hasNext() && (bytes > MAX_BYTES || LIVE.size() > MAX_ENTRIES)) {
            java.util.Map.Entry<String, Bitmap> e = it.next();
            Bitmap b = e.getValue();
            long size = 0L;
            try {
                size = b != null ? b.getAllocationByteCount() : 0L;
            } catch (Throwable ignored) {
            }
            it.remove();
            bytes -= size;
            EVICTS.incrementAndGet();
        }
        if (bytes < 0L) {
            bytes = 0L;
        }
    }

    /** 内容签名：文件长度 + 前 2KB 的 FNV-1a 64。失败返回 0（→ 键里长度为 0 → 不共享）。 */
    static long headHash(@Nullable File f) {
        if (f == null || !f.isFile()) {
            return 0L;
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            byte[] buf = new byte[FrameShareKey.HEAD_BYTES];
            int off = 0;
            while (off < buf.length) {
                int n = in.read(buf, off, buf.length - off);
                if (n <= 0) {
                    break;
                }
                off += n;
            }
            return FrameShareKey.fnv1a(buf, off);
        } catch (Throwable th) {
            return 0L;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /** 一行统计（`W23` 的 `共享帧=` 用它）。取完清零。 */
    @NotNull
    static String takeStat() {
        long asks = ASKS.getAndSet(0L);
        long hits = HITS.getAndSet(0L);
        long puts = PUTS.getAndSet(0L);
        long evicts = EVICTS.getAndSet(0L);
        long flight = FLIGHT_HITS.getAndSet(0L);
        if (asks <= 0L && puts <= 0L) {
            return "";
        }
        int entries;
        long held;
        synchronized (LIVE) {
            entries = LIVE.size();
            held = bytes;
        }
        long rate = asks > 0L ? Math.round(100.0d * hits / (double) asks) : 0L;
        // v1.10.488：`同帧=` 是「同一帧去重」救下的解码次数 —— 487 之前它没被计数，
        // 于是日志里 共享帧 只有 9~25%，看起来像没生效，其实真正省下的解码在 `同帧=` 里。
        long saved = hugs2(hits, flight);
        return " 共享帧=" + hits + "/" + asks + "(" + rate + "%) 同帧=" + flight
                + " 合计省=" + saved + "/" + (asks + flight) + " 存=" + puts
                + " 淘=" + evicts + " 表=" + entries + "帧/" + (held / (1024L * 1024L)) + "MB";
    }

    // ---------------------------------------------------------------- 同一帧并发去重（single-flight）
    /**
     * v1.10.487：**同一帧只解一次**。
     *
     * <p>485/486 的真机数据显示：两个组件是**同相位**的（日志 `播放=438=147/235 439=147/235`），
     * 每一拍它们几乎同时来要同一帧 —— 但两边都查不到表（谁都还没解完），于是**各解一次**。
     * 所以命中率只有 17~30%，而不是理论上限 50%。</p>
     *
     * <p>做法：第一个到的当 leader 去解，后来的等它的结果（最多 {@link #WAIT_MS}）。
     * 一拍 20ms、分层帧解码 10~12ms，等 14ms 仍赶得上；等不到就自己解（宁可多解一次，
     * 也绝不因为等待而掉帧——这是用户的硬口径）。</p>
     */
    static final long WAIT_MS = 14L;

    static final class Flight {
        boolean leader;
        Bitmap result;
        boolean done;
    }

    private static final java.util.HashMap<String, Flight> FLIGHT = new java.util.HashMap<>();

    /** 登记/加入一次「同一帧的解码」。返回的对象里 {@code leader} 表示是不是由自己去解。 */
    @Nullable
    static Flight begin(@Nullable String key) {
        if (key == null) {
            return null;
        }
        synchronized (FLIGHT) {
            Flight f = FLIGHT.get(key);
            if (f != null) {
                f.leader = false;
                return f;
            }
            f = new Flight();
            f.leader = true;
            FLIGHT.put(key, f);
            return f;
        }
    }

    /** 等 leader 的结果（最多 ms）。拿不到返回 null，调用方自己去解。 */
    @Nullable
    static Bitmap await(@NotNull Flight f, long ms) {
        long end = android.os.SystemClock.elapsedRealtime() + ms;
        synchronized (FLIGHT) {
            while (!f.done) {
                long left = end - android.os.SystemClock.elapsedRealtime();
                if (left <= 0L) {
                    return null;
                }
                try {
                    FLIGHT.wait(left);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            return f.result;
        }
    }

    /** 等待者拿到了 leader 的结果（= 这一帧没重复解码）。 */
    static void noteFlightHit() {
        FLIGHT_HITS.incrementAndGet();
    }

    /** leader 解完：公布结果、叫醒等待者、并把它放进共享表。 */
    static void finish(@Nullable String key, @Nullable Flight f, @Nullable Bitmap b) {
        if (key == null || f == null) {
            return;
        }
        if (b != null) {
            put(key, b);
        }
        synchronized (FLIGHT) {
            f.result = b;
            f.done = true;
            FLIGHT.remove(key);
            FLIGHT.notifyAll();
        }
    }

    /** leader 放弃（异常/丢弃）：必须叫醒等待者，否则他们白等到超时。 */
    static void abandon(@Nullable String key, @Nullable Flight f) {
        if (key == null || f == null) {
            return;
        }
        synchronized (FLIGHT) {
            f.done = true;
            f.result = null;
            FLIGHT.remove(key);
            FLIGHT.notifyAll();
        }
    }

    /** 表命中 + 同帧去重 = 真正没重复解码的次数。 */
    static long hugs2(long tableHits, long flightHits) {
        return tableHits + flightHits;
    }

    /** 单测/体检用：当前条目数。 */
    static int size() {
        synchronized (LIVE) {
            return LIVE.size();
        }
    }

    /** 单测/体检用：当前占用字节。 */
    static long heldBytes() {
        synchronized (LIVE) {
            return bytes;
        }
    }
}
