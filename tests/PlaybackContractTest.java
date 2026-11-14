package io.github.animwidget;

import java.util.Locale;

/**
 * v1.10.474：{@link PlaybackContract} 的离线自检（纯 JDK，不依赖 android）。
 *
 * <p>四项口径里「清晰度」和「时长」最容易在改动中被悄悄破坏，而且现象只是"有点糊/有点快"。
 * 这里用真实出现过的三档数据钉住判定：546×602@30fps/172 帧、1280×720@50fps/235 帧、
 * 以及"每帧都写 33ms 会短 57ms"这个反例。</p>
 */
public final class PlaybackContractTest {

    private static int pass;
    private static int fail;

    public static void main(String[] args) {
        // ① 1:1：画布 = 素材，FIT → 清晰度必须 OK
        PlaybackContract.Verdict v1 = PlaybackContract.check(546, 602, 546, 602, false,
                300, 172, AnimPack.frameDurations(172, 300));
        ok(v1.sharp, "546×602 → 546×602 应判 1:1（实际 " + v1.canvasText + "）");

        // ② 画布只有素材一半 → 必须 FAIL
        PlaybackContract.Verdict v2 = PlaybackContract.check(1092, 1204, 546, 602, false,
                300, 172, AnimPack.frameDurations(172, 300));
        ok(!v2.sharp, "画布缩到一半必须判 FAIL（实际 " + v2.canvasText + "）");

        // ③ FILL：画布 600×600 / 素材 1200×600 → 按大的那维算，仍是 1:1
        PlaybackContract.Verdict v3 = PlaybackContract.check(1200, 600, 600, 600, true,
                300, 10, AnimPack.frameDurations(10, 300));
        ok(v3.sharp, "FILL 下 600×600 对 1200×600 应判 1:1（实际 " + v3.canvasText + "）");

        // ④ 30fps 172 帧：逐帧分配后总时长必须精确 5733ms
        int[] d = AnimPack.frameDurations(172, 300);
        PlaybackContract.Verdict v4 = PlaybackContract.check(546, 602, 546, 602, false, 300, 172, d);
        ok(v4.fps, "30fps 的平均帧时长必须精确（实际 " + v4.fpsText + "）");
        ok(v4.speed && v4.duration, "30fps/172 帧的播放速度与时长必须成立（实际 " + v4.speedText + "）");
        ok(PlaybackContract.totalMs(d) == 5733L, "172 帧总时长=5733ms（实际 "
                + PlaybackContract.totalMs(d) + "ms）");

        // ⑤ 反例：每帧都写 33ms（旧写法）→ 必须判 FAIL（5676 vs 5733）
        int[] flat = new int[172];
        for (int i = 0; i < flat.length; i++) {
            flat[i] = 33;
        }
        PlaybackContract.Verdict v5 = PlaybackContract.check(546, 602, 546, 602, false, 300, 172, flat);
        ok(!v5.speed, "每帧写 33ms 必须判 FAIL（旧写法短 57ms，实际 " + v5.speedText + "）");

        // ⑥ 50fps / 235 帧：总时长 4700ms
        int[] d50 = AnimPack.frameDurations(235, 500);
        ok(PlaybackContract.totalMs(d50) == 4700L, "235 帧@50fps 总时长=4700ms（实际 "
                + PlaybackContract.totalMs(d50) + "ms）");

        System.out.println(fail == 0
                ? String.format(Locale.US, "PLAYBACKCONTRACT OK %d", Integer.valueOf(pass))
                : "PLAYBACKCONTRACT FAILED " + fail + "/" + (pass + fail));
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
