package io.github.animwidget;

import android.content.Context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 未捕获异常记录（v1.6.1 → v1.10.439）。
 *
 * <p>v1.10.1 起崩溃**只进日志页那一份数据源**（{@link LogBook}，30 分钟 TTL），
 * 主界面「诊断」读的也是它。
 *
 * <p>v1.10.439（用户："不留任何残存数据"）：这里原来还会额外写一份
 * {@code files/last_error.txt}，而那个文件**没有任何过期机制** —— 崩溃一次就永久躺在
 * files 目录里，跟"只保留最近 30 分钟（含崩溃记录）"直接矛盾。现在不再写；
 * {@link #purgeLegacy} 负责把老版本留下的那份删掉。
 */
final class CrashLog {

    private static final String FILE = "last_error.txt";
    private static boolean installed;

    private CrashLog() {
    }

    /** 装一次钩子即可，重复调用无副作用。 */
    static synchronized void install(@NotNull Context context) {
        if (installed) {
            return;
        }
        installed = true;
        final Context app = context.getApplicationContext();
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable error) {
                save(app, thread, error);
                if (previous != null) {
                    previous.uncaughtException(thread, error);
                }
            }
        });
    }

    static void save(@NotNull Context context, @Nullable Thread thread, @NotNull Throwable error) {
        try {
            // v1.8.13：同步进 logcat，进不去应用时也能 adb 拿到堆栈
            android.util.Log.e("AnimWidget", "crash on " + (thread == null ? "?" : thread.getName()), error);
        } catch (Throwable ignored) {
        }
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
            pw.println("thread=" + (thread == null ? "?" : thread.getName()));
            error.printStackTrace(pw);
            pw.flush();
            // 崩溃也进日志页（同一份数据、同一个 30 分钟 TTL）
            LogBook.add(context, 'E', "崩溃 thread=" + (thread == null ? "?" : thread.getName()));
            LogBook.add(context, 'E', sw.toString());
            // v1.10.439：**不再写 last_error.txt**。堆栈已经在上面那两行里，
            // 而那个文件没有 TTL —— 写它就等于留一份永不过期的残存数据。
        } catch (Throwable ignored) {
            // 记录失败就算了，不能在这里再抛
        }
    }

    /** 旧版遗留的 last_error.txt 直接清除（数据源只在日志页，且那份有 30 分钟 TTL）。 */
    static void purgeLegacy(@NotNull Context context) {
        try {
            new File(context.getFilesDir(), FILE).delete();
        } catch (Throwable ignored) {
        }
    }

}
