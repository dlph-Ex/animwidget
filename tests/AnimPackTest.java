package io.github.animwidget;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * v1.10.469（#4 第 1 步）：{@link AnimPack} 的离线自检（纯 JDK，不依赖 android）。
 *
 * <p>为什么要有它：{@code AnimPack} 的三个「不该发生」分支（帧数超限、输入不是 WebP、
 * 失败留残文件）在真机上永远走不到，一旦走到就是「桌面整块不显示」这种静默失败。
 * 所以拿真实单帧 WebP（外部用 Pillow 生成）在这里把三条失败路径各跑一遍，
 * 再由 {@code scripts/anim-pack-check.py} 用 Pillow 把产物逐帧解码对像素与时长。</p>
 *
 * <p>用法：{@code java AnimPackTest <帧目录> <fps> <输出文件>}</p>
 */
public final class AnimPackTest {

    private static int pass;
    private static int fail;

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        int fpsX10 = Integer.parseInt(args[1]);
        File out = new File(args[2]);

        List<File> frames = new ArrayList<>();
        for (int i = 0; ; i++) {
            File f = new File(dir, String.format(Locale.US, "%05d.webp", Integer.valueOf(i)));
            if (!f.isFile()) {
                break;
            }
            frames.add(f);
        }
        ok(frames.size() >= 2, "输入帧数=" + frames.size() + "（至少要 2 帧）");

        // ① 每帧时长的口径（和播放侧同一个公式）
        ok(AnimPack.frameDurationMs(30) == 33, "fps=30 → 33ms（实际 " + AnimPack.frameDurationMs(30) + "）");
        ok(AnimPack.frameDurationMs(60) == 17, "fps=60 → 17ms（实际 " + AnimPack.frameDurationMs(60) + "）");
        ok(AnimPack.frameDurationMs(0) == 33, "fps=0 → 按 30fps 兜底 33ms（实际 " + AnimPack.frameDurationMs(0) + "）");
        ok(AnimPack.frameDurationMs(1) == 1000, "fps=1 → 1000ms（实际 " + AnimPack.frameDurationMs(1) + "）");

        // ② 正常打包
        int n = AnimPack.packFrames(frames, fpsX10, out);
        ok(n == frames.size(), "打包帧数=" + n + "（应为 " + frames.size() + "）");
        ok(out.isFile() && out.length() > 0L, "输出文件存在且非空");
        ok(AnimPack.looksLikeWebp(out), "输出文件头是 RIFF....WEBP");
        System.out.println("FRAMES " + n);

        // ②b 逐帧时长表：总和必须精确等于源片时长（v1.10.474）
        int[] durs = AnimPack.frameDurations(172, 300);
        long sum = 0L;
        int minD = 99;
        int maxD = 0;
        for (int d : durs) {
            sum += d;
            minD = Math.min(minD, d);
            maxD = Math.max(maxD, d);
        }
        ok(durs.length == 172, "逐帧时长表长度=172（实际 " + durs.length + "）");
        ok(sum == 5733L, "172 帧@30fps 总时长=5733ms（实际 " + sum + "）");
        ok(minD >= 33 && maxD <= 34, "单帧时长只在 33~34ms 之间浮动（实际 " + minD + "~" + maxD + "）");
        int[] d50 = AnimPack.frameDurations(235, 500);
        long s50 = 0L;
        for (int d : d50) {
            s50 += d;
        }
        ok(s50 == 4700L, "235 帧@50fps 总时长=4700ms（实际 " + s50 + "）");

        // ③ 空列表：不该产生文件
        File emptyOut = new File(dir, "empty.webp");
        ok(AnimPack.packFrames(new ArrayList<File>(), fpsX10, emptyOut) == AnimPack.ERR_EMPTY,
                "空列表 → ERR_EMPTY");
        ok(!emptyOut.exists(), "空列表不留文件");

        // ④ 帧数超过上限：不该产生文件（否则真机上会打出一个几十 MB 的东西）
        List<File> tooMany = new ArrayList<>();
        for (int i = 0; i <= AnimPack.MAX_FRAMES; i++) {
            tooMany.add(frames.get(0));
        }
        File bigOut = new File(dir, "toomany.webp");
        ok(AnimPack.packFrames(tooMany, fpsX10, bigOut) == AnimPack.ERR_TOO_MANY,
                "帧数 > " + AnimPack.MAX_FRAMES + " → ERR_TOO_MANY");
        ok(!bigOut.exists(), "超上限不留文件");

        // ⑤ 输入不是 WebP：必须失败，且不留残文件（半截 WebP 在桌面是「整块不显示」）
        File junk = new File(dir, "junk.webp");
        FileOutputStream fos = new FileOutputStream(junk);
        try {
            fos.write(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
        } finally {
            fos.close();
        }
        List<File> bad = new ArrayList<>();
        bad.add(junk);
        File badOut = new File(dir, "bad.webp");
        ok(AnimPack.packFrames(bad, fpsX10, badOut) < 0, "非法输入 → 负值");
        ok(!badOut.exists() && !new File(badOut.getAbsolutePath() + ".tmp").exists(),
                "失败之后不留残文件与 .tmp");

        System.out.println(fail == 0
                ? "ANIMPACK OK " + pass
                : "ANIMPACK FAILED " + fail + "/" + (pass + fail));
        if (fail > 0) {
            System.exit(1);
        }
    }

    private static void ok(boolean cond, String what) {
        if (cond) {
            pass++;
            System.out.println("  OK " + what);
        } else {
            fail++;
            System.out.println("  FAIL " + what);
        }
    }
}
