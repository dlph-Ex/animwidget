package io.github.animwidget;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 日志正文「逐条渐显」的跨刷新对齐（v1.10.440）。
 *
 * <p>为什么单独一个类：这段是纯逻辑（没有任何 Android 调用），可以脱离设备直接测。
 * 背景（用户："新日志生成文本页就闪一次"）：
 * {@code LogConsoleView.setText} 以前每次刷新都把 {@code shownAt} 清零 ——
 * 每来一条新日志，屏幕上三十几行一起从 0 淡到 1，肉眼就是整页闪一下。
 * 现在把上一批的"第一次出现时刻"按行搬到新一批上，**只有真正新增的行才是 0**。
 *
 * <p>为什么要"对齐"而不是简单右移一位：日志行数到顶（3000 行）之后，来一条新日志
 * 会同时挤掉最老的一条，两批长度相等，但内容整体挪了一行。
 */
final class LogAlign {

    /** 认人只需要长度 + 前 48 字（日志行都以 `时间 级别 编号 ` 开头，不整串比省时间）。 */
    private static final int HEAD = 48;

    private LogAlign() {
    }

    static boolean sameLine(@Nullable String a, @Nullable String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int n = Math.min(HEAD, a.length());
        return a.regionMatches(0, b, 0, n);
    }

    /**
     * 把上一批的渐显时刻搬到新一批（返回与 {@code next} 等长的新数组）。
     *
     * <p>搬不动的行给 0 —— 它们会被当成"新行"重新渐显一次，这正是我们想要的
     * （清屏后撤回、换语言重排这类情况，整屏内容确实变了）。
     *
     * @param prevLines 上一批的行（0 位是最新的）
     * @param prevShown 上一批每行的首次显示时刻
     * @param next      下一批的行
     * @return 与 next 等长的时刻数组
     */
    @NotNull
    static long[] carry(@Nullable String[] prevLines, @Nullable long[] prevShown,
                        @NotNull String[] next) {
        long[] out = new long[next.length];
        if (prevLines == null || prevShown == null || prevLines.length == 0 || next.length == 0) {
            return out;
        }
        int usable = Math.min(prevLines.length, prevShown.length);
        // 新日志是"往前面插"的：先在新数组前几格里找到"原来那第一条"落在哪
        int shift = -1;
        int window = Math.min(8, next.length - 1);
        for (int k = 0; k <= window; k++) {
            if (sameLine(next[k], prevLines[0])) {
                shift = k;
                break;
            }
        }
        if (shift < 0) {
            return out;
        }
        for (int i = 0; i < usable; i++) {
            int j = shift + i;
            if (j >= next.length || !sameLine(next[j], prevLines[i])) {
                break;      // 一旦对不上就停：宁可让后面的行重新渐显，也不要搬错时刻
            }
            out[j] = prevShown[i];
        }
        return out;
    }
}
