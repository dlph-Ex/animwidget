package io.github.animwidget;

import android.content.Context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 日志中心（v1.10.0）。
 *
 * 设计要点（用户规格）：
 * • 所有日志（含崩溃）只保留最近 30 分钟 —— 按【条目自己的时间戳】计时，进程重启不重置，
 *   **清屏/撤回都不重置这个计时**（{@link Entry#ts} 不可变，清空和撤回只是搬运同一个对象）；
 * • 落盘 {@code files/log.txt}（追加写），内存里也留一份；
 * • 「清屏」清空当前日志并记住这一次的内容，允许【撤回一次】：撤回只还原
 *   仍在 30 分钟内的条目，并与清屏后新产生的日志按时间合并（不覆盖）；
 * • 退出日志页后撤回入口失效（{@link #forgetUndo()}）。
 *
 * <p>v1.10.439（用户："30 分钟有没有 BUG？不留任何残存数据"）：原来只有**内存**讲 TTL，
 * 另外三处会残存，全部补上：
 * <ol>
 *   <li><b>撤回快照</b> {@link #cleared} 不按 TTL 清理 —— 清屏后放着不管，过期条目一直占内存，
 *       撤回时再被过滤掉（等于白留一份，还不合"不留残存"的口径）；</li>
 *   <li><b>磁盘文件</b>只在下一条日志触发的 20 秒重写时才删过期行 —— 应用闲置/被杀时，
 *       过期行会一直躺在 {@code log.txt} 里（下次启动才被清）；</li>
 *   <li><b>系统时间往回调</b>时，旧条目的时间戳"在将来"，永远等不到过期（见 {@link #SKEW_MS}）。</li>
 * </ol>
 *
 * 所有方法都必须「绝不抛异常、绝不长时间阻塞」—— 日志坏掉不能连累业务。
 */
final class LogBook {

    /** 保留窗口：30 分钟（v1.10.1：全项目只在这里定义一次，其余地方全部读它）。 */
    static final long TTL_MS = 30 * 60 * 1000L;
    private static final int MAX_ENTRIES = 3000;
    private static final String FILE = "log.txt";
    private static final Object LOCK = new Object();

    private static final ArrayList<Entry> entries = new ArrayList<>();
    private static boolean loaded;
    @Nullable
    private static ArrayList<Entry> cleared;
    private static long lastRewrite;
    /** 时间戳比现在晚太多 = 系统时间被回调过，这种条目不能留着等"未来过期"。 */
    private static final long SKEW_MS = 5 * 60 * 1000L;
    /** 后台重写文件是否正在跑（保证同一时刻只有一条，也不会丢新行）。 */
    private static boolean syncBusy;

    static final class Entry {
        final long ts;
        final char level;
        @NotNull
        final String text;

        Entry(long ts, char level, @NotNull String text) {
            this.ts = ts;
            this.level = level;
            this.text = text;
        }
    }

    private LogBook() {
    }

    /** 记一条（自动按行拆分，逐行带同一个时间戳）。 */
    static void add(@Nullable Context context, char level, @Nullable String text) {
        if (text == null || text.length() == 0) {
            return;
        }
        try {
            long now = System.currentTimeMillis();
            String[] lines = text.split("\n");
            synchronized (LOCK) {
                ensureLoaded(context);
                if (prune(now) > 0) {
                    rewriteAsync(context);   // v1.10.439：过期条目内存删了，文件也要跟着删
                }
                for (String raw : lines) {
                    String line = sanitize(raw);
                    if (line.length() == 0) {
                        continue;
                    }
                    Entry entry = new Entry(now, level, line);
                    entries.add(entry);
                    append(context, entry);
                }
                while (entries.size() > MAX_ENTRIES) {
                    entries.remove(0);
                }
                if (now - lastRewrite > 20000L) {
                    rewrite(context);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /** 日志页正文（时间倒序，最新在最上面）；没有内容返回 null。 */
    @Nullable
    static String render(@Nullable Context context) {
        try {
            synchronized (LOCK) {
                ensureLoaded(context);
                if (prune(System.currentTimeMillis()) > 0) {
                    rewriteAsync(context);
                }
                if (entries.isEmpty()) {
                    return null;
                }
                SimpleDateFormat fmt = timeFormat(context);
                StringBuilder sb = new StringBuilder(entries.size() * 64);
                for (int i = entries.size() - 1; i >= 0; i--) {
                    Entry entry = entries.get(i);
                    sb.append(fmt != null ? fmt.format(new Date(entry.ts)) : String.valueOf(entry.ts / 1000))
                            .append(' ').append(entry.level)
                            .append(' ').append(entry.text)
                            .append('\n');
                }
                return sb.toString();
            }
        } catch (Throwable th) {
            return null;
        }
    }

    /**
     * v1.10.429：**只拼末尾 maxLines 行**（渲染用）。
     *
     * <p>原来 {@link #render} 会把**全部**日志拼成一个 String，然后让界面自己截掉前面 ——
     * 也就是说"只显示 3000 行"这个限制是在**峰值内存之后**才生效的：
     * 一份几万行、每行七八百字的日志，先要拼出几十 MB 的字符串，再截断一次，
     * 打开日志页就黑屏回桌面（进程被 LMK 干掉）就是这么来的。
     *
     * <p>现在从最新往回数够 maxLines 就停，峰值内存 ≈ 要显示的那部分。
     * **不减少任何一行显示内容**（显示仍然是末尾 maxLines 行，只是不再先把整份拼出来）。
     *
     * @return {文本, 是否还有更早的行被省略}
     */
    @Nullable
    static String[] renderTail(@Nullable Context context, int maxLines) {
        try {
            synchronized (LOCK) {
                ensureLoaded(context);
                // v1.10.439：日志页每 2 秒走这里 —— 过期条目一被清掉，就顺手把文件也重写
                //（后台线程 + 30 秒节流），别让"内存没了、磁盘还留着"过夜。
                if (prune(System.currentTimeMillis()) > 0) {
                    rewriteAsync(context);
                }
                if (entries.isEmpty() || maxLines <= 0) {
                    return null;
                }
                SimpleDateFormat fmt = timeFormat(context);
                int n = Math.min(entries.size(), maxLines);
                StringBuilder sb = new StringBuilder(n * 96);
                for (int i = entries.size() - 1, k = 0; i >= 0 && k < n; i--, k++) {
                    Entry entry = entries.get(i);
                    sb.append(fmt != null ? fmt.format(new Date(entry.ts))
                                    : String.valueOf(entry.ts / 1000))
                            .append(' ').append(entry.level)
                            .append(' ').append(entry.text)
                            .append('\n');
                }
                return new String[]{sb.toString(), entries.size() > n ? "1" : "0"};
            }
        } catch (Throwable th) {
            return null;
        }
    }

    static boolean hasEntries(@Nullable Context context) {
        synchronized (LOCK) {
            ensureLoaded(context);
            if (prune(System.currentTimeMillis()) > 0) {
                rewriteAsync(context);
            }
            return !entries.isEmpty();
        }
    }

    /** 清屏：清掉当前日志，并记住这一次的内容供撤回。 */
    static boolean clear(@Nullable Context context) {
        synchronized (LOCK) {
            ensureLoaded(context);
            long now = System.currentTimeMillis();
            prune(now);
            if (entries.isEmpty()) {
                return false;
            }
            cleared = new ArrayList<>(entries);
            entries.clear();
            rewrite(context);
            return true;
        }
    }

    static boolean canUndo() {
        synchronized (LOCK) {
            return cleared != null && !cleared.isEmpty();
        }
    }

    /** 撤回最近一次清屏：只还原仍未过期的条目（{@link #TTL_MS}），与新日志按时间合并。 */
    static boolean undo(@Nullable Context context) {
        synchronized (LOCK) {
            ensureLoaded(context);
            if (cleared == null || cleared.isEmpty()) {
                return false;
            }
            long now = System.currentTimeMillis();
            prune(now);
            if (cleared == null) {
                // v1.10.439：prune 会把"整份快照都过期了"的 cleared 清成 null ——
                // 这里必须重新判一次，否则下面 for-each 直接 NPE（撤回按钮一点就崩）。
                return false;
            }
            for (Entry entry : cleared) {
                if (!expired(entry, now)) {   // v1.10.439：和实时日志同一个判据（含时钟回调）
                    entries.add(entry);
                }
            }
            cleared = null;
            Collections.sort(entries, new Comparator<Entry>() {
                @Override
                public int compare(Entry a, Entry b) {
                    return Long.compare(a.ts, b.ts);
                }
            });
            rewrite(context);
            return true;
        }
    }

    /** 退出日志页：撤回入口失效（用户规格）。 */
    static void forgetUndo() {
        synchronized (LOCK) {
            cleared = null;
        }
    }

    /** 清空所有（含 memory + 文件），不给撤回。 */
    static void wipe(@Nullable Context context) {
        synchronized (LOCK) {
            entries.clear();
            cleared = null;
            loaded = true;
            rewrite(context);
        }
    }

    // ------------------------------------------------------------------ 内部

    /**
     * 按时间戳清过期条目，返回这次清掉几条。
     *
     * <p>v1.10.439：**撤回快照跟实时日志走同一个 TTL**；进度比现在晚 {@link #SKEW_MS} 以上的
     * 条目也清掉（系统时间被往回调过时，那些条目"永远等不到过期"）。
     */
    private static int prune(long now) {
        int dropped = 0;
        int i = 0;
        while (i < entries.size()) {
            if (expired(entries.get(i), now)) {
                entries.remove(i);
                dropped++;
            } else {
                i++;
            }
        }
        if (cleared != null) {
            int j = 0;
            while (j < cleared.size()) {
                if (expired(cleared.get(j), now)) {
                    cleared.remove(j);
                    dropped++;
                } else {
                    j++;
                }
            }
            if (cleared.isEmpty()) {
                cleared = null;
            }
        }
        return dropped;
    }

    /** 这一条是不是该删了（太旧，或者时间戳跑到未来去了）。 */
    private static boolean expired(@NotNull Entry entry, long now) {
        long age = now - entry.ts;
        return age > TTL_MS || age < -SKEW_MS;
    }

    private static void ensureLoaded(@Nullable Context context) {
        if (loaded || context == null) {
            return;
        }
        loaded = true;
        File file = new File(context.getFilesDir(), FILE);
        if (!file.exists()) {
            return;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                Entry entry = parse(line);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable ignored) {
                }
            }
        }
        if (prune(System.currentTimeMillis()) > 0) {
            // v1.10.439：冷启动读进来的过期行，当场就把文件重写掉 ——
            // 否则它们要等到"下一条日志触发的 20 秒重写"才真正从磁盘消失。
            rewriteAsync(context);
        }
        Collections.sort(entries, new Comparator<Entry>() {
            @Override
            public int compare(Entry a, Entry b) {
                return Long.compare(a.ts, b.ts);
            }
        });
    }

    @Nullable
    private static Entry parse(@NotNull String line) {
        try {
            int first = line.indexOf('\t');
            int second = line.indexOf('\t', first + 1);
            if (first <= 0 || second <= first) {
                return null;
            }
            long ts = Long.parseLong(line.substring(0, first));
            char level = line.charAt(first + 1);
            String text = line.substring(second + 1);
            return new Entry(ts, level, text);
        } catch (Throwable th) {
            return null;
        }
    }

    private static void append(@Nullable Context context, @NotNull Entry entry) {
        if (context == null) {
            return;
        }
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(new File(context.getFilesDir(), FILE), true);
            out.write((entry.ts + "\t" + entry.level + "\t" + entry.text + "\n").getBytes("UTF-8"));
        } catch (Throwable ignored) {
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /**
     * v1.10.439：内存里已经删掉过期条目、磁盘上还留着 —— 起一条后台线程把文件重写一遍。
     *
     * <p>为什么不在主线程直接写：3000 条 × 八九百字 ≈ 2.7MB，日志页每 2 秒就会触发一次清理，
     * 直接写会把主线程按在地上（430 之前"打开日志页黑屏"的同一类问题）。
     * 为什么不在外面加锁写：{@link #add} 里的 {@link #append} 也在同一把锁里，
     * 只有把**写文件整段**放回锁内，才不会出现"刚截断、新行还没落盘"的丢行。
     * 所以这条线程拿锁写、写完放手 —— 加 30 秒节流 + 单飞，任意时刻最多一条。
     */
    private static void rewriteAsync(@Nullable Context context) {
        if (context == null) {
            return;
        }
        final Context app;
        synchronized (LOCK) {
            if (syncBusy || System.currentTimeMillis() - lastRewrite < 30000L) {
                return;
            }
            syncBusy = true;
            Context c = context.getApplicationContext();
            app = c != null ? c : context;
        }
        try {
            Thread worker = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        synchronized (LOCK) {
                            rewrite(app);
                        }
                    } catch (Throwable ignored) {
                        // 写不进去就下次再说，绝不连累业务
                    } finally {
                        synchronized (LOCK) {
                            syncBusy = false;
                        }
                    }
                }
            }, "logbook-sync");
            worker.setDaemon(true);
            worker.start();
        } catch (Throwable th) {
            synchronized (LOCK) {
                syncBusy = false;
            }
        }
    }

    /** 按内存内容重写文件（清屏 / 撤回 / 定期压缩）。 */
    private static void rewrite(@Nullable Context context) {
        if (context == null) {
            return;
        }
        lastRewrite = System.currentTimeMillis();
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(new File(context.getFilesDir(), FILE), false);
            StringBuilder sb = new StringBuilder(entries.size() * 64);
            for (Entry entry : entries) {
                sb.append(entry.ts).append('\t').append(entry.level).append('\t')
                        .append(entry.text).append('\n');
            }
            out.write(sb.toString().getBytes("UTF-8"));
        } catch (Throwable ignored) {
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /**
     * v1.10.1：时间显示精确到秒，格式跟随系统区域与 12/24 小时制
     * （getBestDateTimePattern 会按 locale 给 "Hms" 挑合适的骨架）。
     */
    @Nullable
    static SimpleDateFormat timeFormat(@Nullable Context context) {
        try {
            Locale locale = context != null
                    ? context.getResources().getConfiguration().getLocales().get(0)
                    : Locale.getDefault();
            String pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "Hms");
            return new SimpleDateFormat(pattern, locale);
        } catch (Throwable th) {
            return new SimpleDateFormat("HH:mm:ss", Locale.US);
        }
    }

    @NotNull
    private static String sanitize(@NotNull String raw) {
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '\t' || ch == '\n' || ch == '\r') {
                sb.append(' ');
            } else {
                sb.append(ch);
            }
        }
        String text = sb.toString().trim();
        if (text.length() > 900) {
            text = text.substring(0, 900) + "…";
        }
        return text;
    }

    /** 供「诊断」区展示最近 N 行（时间正序，取尾部）。 */
    @Nullable
    static String tail(@Nullable Context context, int maxLines) {
        List<String> lines = new ArrayList<>();
        try {
            synchronized (LOCK) {
                ensureLoaded(context);
                if (prune(System.currentTimeMillis()) > 0) {
                    rewriteAsync(context);
                }
                SimpleDateFormat fmt = timeFormat(context);
                int from = Math.max(0, entries.size() - Math.max(maxLines, 1));
                for (int i = from; i < entries.size(); i++) {
                    Entry entry = entries.get(i);
                    lines.add(fmt.format(new Date(entry.ts)) + " " + entry.level + " " + entry.text);
                }
            }
        } catch (Throwable th) {
            return null;
        }
        if (lines.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(line);
        }
        return sb.toString();
    }
}
