package io.github.animwidget;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 日志 30 分钟保留窗口的**行为级离线测试**（v1.10.439）。
 *
 * <p>直接反射真正的 {@link LogBook}（不是抄一份逻辑），所以跑过的就是装机的那段代码。
 * 只碰 prune/expired/undo 这些不依赖 Context 的路径：file IO 那边传 null context 会自动跳过。
 *
 * <p>跑法：javac 后 `java io.github.animwidget.LogBookTtlProbe`，输出 `TTL OK n` 才算过。
 */
public final class LogBookTtlProbe {

    private static final long MIN = 60 * 1000L;
    private static Field entriesF;
    private static Field clearedF;
    private static Method pruneM;
    private static Constructor<?> entryCtor;
    private static int checks;
    private static int fails;

    public static void main(String[] args) throws Exception {
        Class<?> book = Class.forName("io.github.animwidget.LogBook");
        entriesF = book.getDeclaredField("entries");
        entriesF.setAccessible(true);
        clearedF = book.getDeclaredField("cleared");
        clearedF.setAccessible(true);
        pruneM = book.getDeclaredMethod("prune", long.class);
        pruneM.setAccessible(true);
        Class<?> entryCls = Class.forName("io.github.animwidget.LogBook$Entry");
        entryCtor = entryCls.getDeclaredConstructor(long.class, char.class, String.class);
        entryCtor.setAccessible(true);

        long now = 1_700_000_000_000L;

        // ① 29 分钟留着、31 分钟删掉（按条目自己的时间戳，不是按启动时刻）
        reset();
        add(now - 31 * MIN, "31min");
        add(now - 29 * MIN, "29min");
        int dropped = prune(now);
        expect("① 过期一条", dropped == 1 && size() == 1 && lastText().equals("29min"));

        // ② 边界：正好 30 分钟整 → 还留着（判据是 `>`，不是 `>=`）
        reset();
        add(now - 30 * MIN, "exactly30");
        expect("② 边界 30:00 留着", prune(now) == 0 && size() == 1);

        // ③ 时钟被往回调：时间戳跑到未来 10 分钟 → 删掉（否则永远等不到过期）
        reset();
        add(now + 10 * MIN, "future");
        expect("③ 未来时间戳删掉", prune(now) == 1 && size() == 0);

        // ④ 未来 2 分钟（抖动容差内）→ 留着
        reset();
        add(now + 2 * MIN, "slightFuture");
        expect("④ 容差内保留", prune(now) == 0 && size() == 1);

        // ⑤ 清屏快照也吃同一个 TTL：整份都过期 → cleared 变 null（canUndo 随之为 false）
        reset();
        add(now - 31 * MIN, "old1");
        add(now - 35 * MIN, "old2");
        setCleared(new ArrayList<Object>(entries()));
        clearEntries();
        prune(now);
        expect("⑤ 过期快照被清空", cleared() == null);

        // ⑥ 清屏快照里还有新鲜的 → 保留新鲜的，过期的不占内存
        reset();
        add(now - 40 * MIN, "stale");
        add(now - 5 * MIN, "fresh");
        setCleared(new ArrayList<Object>(entries()));
        clearEntries();
        prune(now);
        List<Object> c = cleared();
        expect("⑥ 快照只留新鲜的", c != null && c.size() == 1 && text(c.get(0)).equals("fresh"));

        // ⑦ 撤回：整份快照都过期 → 返回 false，绝不 NPE（v1.10.439 修的那个坑）
        reset();
        add(System.currentTimeMillis() - 31 * MIN, "staleOnly");
        setCleared(new ArrayList<Object>(entries()));
        clearEntries();
        boolean undone;
        try {
            undone = (Boolean) invokeUndo();
        } catch (Throwable th) {
            undone = true;   // 抛异常 = 失败
        }
        expect("⑦ 过期快照撤回安全返回 false", !undone && size() == 0);

        // ⑧ 撤回：快照没过期 → 正常并回内存，条数对
        reset();
        add(System.currentTimeMillis() - 5 * MIN, "keep");
        setCleared(new ArrayList<Object>(entries()));
        clearEntries();
        boolean back = (Boolean) invokeUndo();
        expect("⑧ 有效快照能撤回", back && size() == 1 && lastText().equals("keep"));

        // ⑨ 清屏不重置计时：同一个 Entry 对象清屏→撤回后时间戳不变
        //    （undo 内部读真实墙钟，所以这两条用真实时间构造）
        reset();
        long realNow = System.currentTimeMillis();
        long t0 = realNow - 20 * MIN;
        Object e = entry(t0, "clock");
        entries().add(e);
        setCleared(new ArrayList<Object>(entries()));
        clearEntries();
        invokeUndo();
        expect("⑨ 清屏撤回不改时间戳", size() == 1 && text(entries().get(0)).equals("clock")
                && ts(entries().get(0)) == t0 && entries().get(0) == e);

        // ⑩ 撤回后 15 分钟（累计 35 分钟）→ 该条目同样被删（同一个判据，不因为"撤回过"而续命）
        int d2 = prune(realNow + 15 * MIN);
        expect("⑩ 撤回过的条目照样按 30 分钟过期", d2 == 1 && size() == 0);

        System.out.println((fails == 0 ? "TTL OK " : "TTL FAIL ") + checks + " 条断言");
        if (fails != 0) {
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- 反射帮手

    private static void reset() throws Exception {
        clearEntries();
        setCleared(null);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> entries() throws Exception {
        return (List<Object>) entriesF.get(null);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> cleared() throws Exception {
        return (List<Object>) clearedF.get(null);
    }

    private static void setCleared(List<Object> value) throws Exception {
        clearedF.set(null, value);
    }

    private static void clearEntries() throws Exception {
        entries().clear();
    }

    private static Object entry(long ts, String text) throws Exception {
        return entryCtor.newInstance(ts, 'D', text);
    }

    private static void add(long ts, String text) throws Exception {
        entries().add(entry(ts, text));
    }

    private static int size() throws Exception {
        return entries().size();
    }

    private static String lastText() throws Exception {
        List<Object> all = entries();
        return all.isEmpty() ? "" : text(all.get(all.size() - 1));
    }

    private static int prune(long now) throws Exception {
        return (Integer) pruneM.invoke(null, now);
    }

    /** undo 内部读真实墙钟 —— 调它的用例必须用 System.currentTimeMillis() 构造时间戳。 */
    private static Object invokeUndo() throws Exception {
        Method undo = Class.forName("io.github.animwidget.LogBook")
                .getDeclaredMethod("undo", android.content.Context.class);
        undo.setAccessible(true);
        return undo.invoke(null, new Object[]{null});
    }

    private static long ts(Object e) throws Exception {
        Field f = e.getClass().getDeclaredField("ts");
        f.setAccessible(true);
        return (Long) f.get(e);
    }

    private static String text(Object e) throws Exception {
        Field f = e.getClass().getDeclaredField("text");
        f.setAccessible(true);
        return (String) f.get(e);
    }

    private static void expect(String name, boolean ok) {
        checks++;
        if (!ok) {
            fails++;
            System.out.println("  FAIL " + name);
        }
    }
}
