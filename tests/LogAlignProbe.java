package io.github.animwidget;

/**
 * 「逐条渐显」跨刷新对齐的行为级测试（v1.10.440）。
 *
 * <p>用户报的现象：**每来一条新日志，整页文本闪一次**。
 * 根因是 {@code setText} 每次把 {@code shownAt} 全清零 → 所有可见行重播 0→1 的淡入。
 * 修法是把旧行的时刻搬到新行上（{@link LogAlign#carry}），这里逐条钉住它的行为。
 *
 * <p>跑法：`java io.github.animwidget.LogAlignProbe`，输出 `ALIGN OK n` 才算过。
 */
public final class LogAlignProbe {

    private static int checks;
    private static int fails;

    public static void main(String[] args) {
        // 三行老内容，第一条最新
        String[] a = {"06:31:29 D 011 aa", "06:31:25 D 011 bb", "06:31:22 D 011 cc"};
        long[] shown = {100L, 200L, 300L};

        // ① 来一条新日志（往前面插）：老的三行时刻原样保留，只有第 0 行是 0（才渐显）
        long[] r1 = LogAlign.carry(a, shown, new String[]{"06:31:33 D 011 nn", a[0], a[1], a[2]});
        expect("① 新行插入只淡新行", r1.length == 4 && r1[0] == 0L
                && r1[1] == 100L && r1[2] == 200L && r1[3] == 300L);

        // ② 行数到顶（最老的被挤掉）：长度不变、整体挪一行，时刻仍跟着行走
        long[] r2 = LogAlign.carry(a, shown, new String[]{"06:31:33 D 011 nn", a[0], a[1]});
        expect("② 到顶挤掉最老行也对得上", r2.length == 3 && r2[0] == 0L
                && r2[1] == 100L && r2[2] == 200L);

        // ③ 内容没变（重复刷新）：一条都不该重新渐显
        long[] r3 = LogAlign.carry(a, shown, a.clone());
        expect("③ 内容不变不重播", r3[0] == 100L && r3[1] == 200L && r3[2] == 300L);

        // ④ 清屏：新内容为空 → 空数组
        long[] r4 = LogAlign.carry(a, shown, new String[0]);
        expect("④ 清屏后为空", r4.length == 0);

        // ⑤ 老内容还在、前面又插了两条（例如撤回后新旧日志并存）：
        //    还在屏幕上的行不许重播，只有那两条新的渐显
        long[] r5 = LogAlign.carry(a, shown,
                new String[]{"07:00:01 E crash", "07:00:00 D 011 zz", a[0], a[1], a[2]});
        expect("⑤ 老行还在就只淡新行", r5[0] == 0L && r5[1] == 0L
                && r5[2] == 100L && r5[3] == 200L && r5[4] == 300L);

        // ⑤b 整屏内容真的换了（一条都对不上）→ 全部重新渐显，绝不搬错时刻
        long[] r5b = LogAlign.carry(a, shown,
                new String[]{"07:00:01 E crash", "07:00:00 D 011 zz", "07:00:00 D 011 yy"});
        expect("⑤b 完全换内容不搬错", r5b[0] == 0L && r5b[1] == 0L && r5b[2] == 0L);

        // ⑥ 一次来三条新日志：三条都是 0，老行照旧
        long[] r6 = LogAlign.carry(a, shown, new String[]{"06:31:41 x", "06:31:40 x", "06:31:39 x",
                a[0], a[1], a[2]});
        expect("⑥ 一次三条都渐显", r6[0] == 0L && r6[1] == 0L && r6[2] == 0L
                && r6[3] == 100L && r6[4] == 200L && r6[5] == 300L);

        // ⑦ 长度不同但前缀相同的行不算同一行（防止误搬）
        long[] r7 = LogAlign.carry(new String[]{"06:31:29 D 011 aa"}, new long[]{7L},
                new String[]{"06:31:29 D 011 aaaaa"});
        expect("⑦ 长度不同不认同一行", r7[0] == 0L);

        // ⑧ 空历史（第一次进页面）不崩、全 0
        long[] r8 = LogAlign.carry(new String[0], new long[0], a);
        expect("⑧ 首次进页面全渐显", r8.length == 3 && r8[0] == 0L && r8[1] == 0L && r8[2] == 0L);

        System.out.println((fails == 0 ? "ALIGN OK " : "ALIGN FAIL ") + checks + " 条断言");
        if (fails != 0) {
            System.exit(1);
        }
    }

    private static void expect(String name, boolean ok) {
        checks++;
        if (!ok) {
            fails++;
            System.out.println("  FAIL " + name);
        }
    }
}
