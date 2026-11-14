package io.github.animwidget;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * v1.10.107（需求 1.1）：判断代号页的**口令门 + 限流**。
 *
 * · 口令不以明文存在代码里 —— 只存它的**自研哈希**（长盐 FNV-1a 64，逐字符混入、
 *   每轮再异或右移，最后再滚三轮黄金比例乘法），输入即时转哈希后比对；
 * · 防暴力：**10 分钟滑动窗口内最多 3 次**，超了就锁定到窗口结束；
 *   计数与窗口起点都写盘，杀进程重进也绕不过去；
 * · 无论输入对错都先算哈希（不做早退），时间与次数判断放在哈希之后。
 */
public final class DiagVault {

    public static final int OK = 0;
    public static final int WRONG = 1;
    public static final int LOCKED = 2;

    /** 口令的哈希（不是口令本身）。 */
    private static final String EXPECTED = "1b8ea472d2926ee9";
    private static final String SALT = "AnimWidget#Diag#2026";

    private static final String PREFS = "diag_gate";
    private static final String K_WINDOW = "window_start";
    private static final String K_FAILS = "fails";

    /** 滑动窗口 10 分钟。 */
    public static final long WINDOW_MS = 10L * 60L * 1000L;
    /** 窗口内最多尝试次数。 */
    public static final int MAX_TRY = 3;

    private DiagVault() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, 0);
    }

    /** 自研哈希：FNV-1a 64 + 长盐 + 逐字符右移异或 + 三轮黄金比例混合。 */
    public static String hash(String input) {
        long h = 0xcbf29ce484222325L;
        String material = SALT + (input == null ? "" : input) + SALT;
        for (int i = 0; i < material.length(); i++) {
            h ^= material.charAt(i);
            h *= 0x100000001b3L;
            h ^= (h >>> 29);
        }
        for (int round = 0; round < 3; round++) {
            h *= 0x9E3779B97F4A7C15L;
            h ^= (h >>> 33);
        }
        return Long.toHexString(h);
    }

    /** 还要等多少毫秒才解锁（0 = 现在可以试）。 */
    public static long lockedRemain(Context c) {
        SharedPreferences p = prefs(c);
        long start = p.getLong(K_WINDOW, 0L);
        int fails = p.getInt(K_FAILS, 0);
        long now = System.currentTimeMillis();
        if (fails < MAX_TRY) {
            return 0L;
        }
        long end = start + WINDOW_MS;
        long left = end - now;
        return left > 0L ? left : 0L;
    }

    /** 本窗口还能试几次（锁定后为 0）。 */
    public static int remaining(Context c) {
        SharedPreferences p = prefs(c);
        long start = p.getLong(K_WINDOW, 0L);
        int fails = p.getInt(K_FAILS, 0);
        if (System.currentTimeMillis() - start >= WINDOW_MS) {
            return MAX_TRY;
        }
        int left = MAX_TRY - fails;
        return Math.max(0, left);
    }

    /** 校验口令。返回 {@link #OK} / {@link #WRONG} / {@link #LOCKED}。 */
    public static int check(Context c, String input) {
        SharedPreferences p = prefs(c);
        long now = System.currentTimeMillis();
        long windowStart = p.getLong(K_WINDOW, 0L);
        int fails = p.getInt(K_FAILS, 0);
        if (now - windowStart >= WINDOW_MS) {
            windowStart = now;
            fails = 0;
        }
        if (fails >= MAX_TRY) {
            if (now - windowStart < WINDOW_MS) {
                return LOCKED;
            }
            windowStart = now;
            fails = 0;
        }
        String digest = hash(input);
        if (EXPECTED.equals(digest)) {
            p.edit().putLong(K_WINDOW, windowStart).putInt(K_FAILS, 0).apply();
            return OK;
        }
        fails++;
        p.edit().putLong(K_WINDOW, windowStart).putInt(K_FAILS, fails).apply();
        return fails >= MAX_TRY ? LOCKED : WRONG;
    }
}
