package io.github.animwidget;

/**
 * v1.10.481（P0-3 / A 共享解码）的脱机自检（verify-apk.sh [27] 的一部分）。
 *
 * <p>共享帧这张表最贵的错是**错键**：两个内容不同的帧算成同一个键 → 这一路显示另一路的画面。
 * 它不会崩、不会打日志、也不掉帧，只会在用户眼里「怎么有一帧不对」—— 所以必须在这里钉住：
 * 键里任何一个输入变了，键就必须变；帧号要按帧数归一化；拿不到签名时宁可不共享。</p>
 */
public final class FrameShareKeyTest {

    private static int total = 0;
    private static int bad = 0;

    public static void main(String[] args) {
        final String base = "FIT_ROUNDED_100_0_100_0_0_100_0_0_0_0_0_100_1|video|content://x/1.mp4|235|93|1280|VIDEO|1280x720";
        final long len = 118340L;
        final long head = 0x8f3a2b1c4d5e6f70L;

        String k = FrameShareKey.of(base, 117, 235, len, head);
        ok(k != null && k.length() > base.length(), "键没拼出来：" + k);
        eq("同输入同键", k, FrameShareKey.of(base, 117, 235, len, head));

        // 帧号归一化：235 帧里 237 就是 2
        eq("帧号归一化", FrameShareKey.of(base, 2, 235, len, head),
                FrameShareKey.of(base, 237, 235, len, head));
        // 负数帧号也要能归一化（播放头可能被 -1 回退过）
        eq("负帧号归一化", FrameShareKey.of(base, 233, 235, len, head),
                FrameShareKey.of(base, -2, 235, len, head));

        // 任何一个输入变了，键就必须变
        ok(!k.equals(FrameShareKey.of(base, 118, 235, len, head)), "帧号没参与键");
        ok(!k.equals(FrameShareKey.of(base, 117, 236, len, head)), "帧数没参与键");
        ok(!k.equals(FrameShareKey.of(base, 117, 235, len + 1L, head)), "文件长度没参与键");
        ok(!k.equals(FrameShareKey.of(base, 117, 235, len, head ^ 1L)), "内容指纹没参与键");
        ok(!k.equals(FrameShareKey.of(base + "x", 117, 235, len, head)), "身份串没参与键");

        // 拿不到签名 / 参数不全 → null（宁可不共享，也不要错键）
        eqs("空身份 → null", null, FrameShareKey.of(null, 0, 10, len, head));
        eqs("空串身份 → null", null, FrameShareKey.of("", 0, 10, len, head));
        eqs("帧数 0 → null", null, FrameShareKey.of(base, 0, 0, len, head));
        eqs("文件长度 0 → null", null, FrameShareKey.of(base, 0, 10, 0L, head));

        // FNV-1a：确定性 + 一点雪崩
        byte[] a = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        long h1 = FrameShareKey.fnv1a(a, a.length);
        eql("fnv 确定性", h1, FrameShareKey.fnv1a(a, a.length));
        eql("空数据必须等于 FNV 初值", 0xcbf29ce484222325L, FrameShareKey.fnv1a(a, 0));
        byte[] b = a.clone();
        b[3] = 9;
        ok(FrameShareKey.fnv1a(b, b.length) != h1, "改一个字节指纹没变（等于键可以撞）");
        ok(FrameShareKey.fnv1a(a, 4) != h1, "只算前 4 字节和全算必须不同");
        // 高位也要参与（64 位而不是 32 位）：只在**最后一个字节的最高位**不同的两份数据，
        // 指纹必须不同 —— 32 位实现最容易在这一位上露馅
        byte[] c1 = new byte[8];
        byte[] c2 = new byte[8];
        c2[7] = (byte) 0x80;
        ok(FrameShareKey.fnv1a(c1, 8) != FrameShareKey.fnv1a(c2, 8),
                "最高位变化没有影响指纹（有效位数不足 64）");

        // 头 2KB 的上限口径：HEAD_BYTES 必须是个合理的小值
        ok(FrameShareKey.HEAD_BYTES >= 512 && FrameShareKey.HEAD_BYTES <= 8192,
                "签名只读 " + FrameShareKey.HEAD_BYTES + " 字节：太小容易撞，太大白读");

        if (bad > 0) {
            System.out.println("FRAMESHAREKEY FAILED " + bad + "/" + total);
            System.exit(1);
        }
        System.out.println("FRAMESHAREKEY OK " + total);
    }

    private static void ok(boolean cond, String msg) {
        total++;
        if (!cond) {
            bad++;
            System.out.println("  FAIL " + msg);
        }
    }

    private static void eq(String what, String want, String got) {
        total++;
        if (want == null ? got != null : !want.equals(got)) {
            bad++;
            System.out.println("  FAIL " + what + "：want=" + want + " got=" + got);
        }
    }

    private static void eqs(String what, String want, String got) {
        eq(what, want, got);
    }

    private static void eql(String what, long want, long got) {
        total++;
        if (want != got) {
            bad++;
            System.out.println("  FAIL " + what + "：want=" + want + " got=" + got);
        }
    }
}
