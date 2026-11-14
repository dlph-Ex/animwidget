package io.github.animwidget;

/** v1.10.460：预览虚线取色的离线自检（进门禁 [23]，不进 APK）。 */
public final class OutlineColorTest {

    private static int ok;
    private static int bad;

    private static void check(boolean cond, String what) {
        if (cond) {
            ok++;
        } else {
            bad++;
            System.out.println("FAIL " + what);
        }
    }

    public static void main(String[] args) {
        int[] solid = new int[100];
        for (int i = 0; i < solid.length; i++) {
            solid[i] = 0xFF3A7BC8;
        }
        check(OutlineColor.dominant(solid) == 0x3A7BC8, "纯色主色不对");
        check(OutlineColor.inverse(0x3E7CB8) == 0xFFC18347, "相反值算错");
        check((OutlineColor.outlineFor(0x3E7CB8) >>> 24) == 0xFF, "虚线必须不透明");

        int[] mix = new int[100];
        for (int i = 0; i < 70; i++) {
            mix[i] = 0xFF203040;
        }
        for (int i = 70; i < 100; i++) {
            mix[i] = 0xFFA0B0C0;
        }
        check(OutlineColor.dominant(mix) == 0x203040, "混色取最大失败");

        int[] withAlpha = new int[]{0x00000000, 0x00FFFFFF, 0xFF112233};
        check(OutlineColor.dominant(withAlpha) == 0x112233, "透明像素被算进主色");
        check(OutlineColor.dominant(new int[0]) == -1, "空数组应返回 -1");
        check(OutlineColor.dominant(new int[]{0, 0, 0}) == -1, "全透明应返回 -1");
        check(OutlineColor.outlineFor(-1) == OutlineColor.FALLBACK, "读不到壁纸应退回兜底色");
        check(OutlineColor.inverse(OutlineColor.inverse(0x123456) & 0xFFFFFF) == 0xFF123456,
                "两次取反没回到原色");

        // 「相反值」的正确判据不是「每通道差很多」（中间调的相反值每通道只差几十），
        // 而是**最亮的那一路变成最暗**：主色偏蓝 → 虚线就偏橙。用通道排序钉住它。
        int dom = 0x3E7CB8;
        int line = OutlineColor.outlineFor(dom);
        int domMax = maxChannel(dom);
        int lineMin = minChannel(line);
        check(domMax == lineMin, "相反值没把主色最强的那一路变成最弱（色相不相反）");
        int domMin = minChannel(dom);
        int lineMax = maxChannel(line);
        check(domMin == lineMax, "相反值没把主色最弱的那一路变成最强");

        // 同色系噪声必须聚成一桶（否则"最大"没有意义）
        int[] noisy = new int[200];
        for (int i = 0; i < noisy.length; i++) {
            int jitter = (i % 4) * 2;   // ±6 的抖动，仍在同一个 5 位桶附近
            noisy[i] = 0xFF000000 | ((0x3E + jitter) << 16) | ((0x7C + jitter) << 8) | (0xB8 + jitter);
        }
        int domNoisy = OutlineColor.dominant(noisy);
        check(domNoisy >= 0 && Math.abs(((domNoisy >> 16) & 0xff) - 0x3E) <= 12,
                "轻微抖动就把主色拉偏了");

        // ---- 默认方案（v1.10.462 用户逐字口径）
        int bright = OutlineColor.defaultFor(0xFFF2F2F2);   // 亮背景 → 压暗
        int dark = OutlineColor.defaultFor(0xFF101010);     // 暗背景 → 提亮
        int r1 = (bright >> 16) & 0xff;
        int g1 = (bright >> 8) & 0xff;
        int b1 = bright & 0xff;
        check(r1 == g1 && g1 == b1, "默认方案必须保持中性灰（色相不许变）");
        check(r1 < 0x80, "亮背景应把灰框压暗到中性灰以下");
        int r2 = (dark >> 16) & 0xff;
        check(r2 > 0x80, "暗背景应把灰框提亮到中性灰以上");
        check(((bright >>> 24) & 0xff) == 0xFF && ((dark >>> 24) & 0xff) == 0xFF,
                "默认方案必须不透明");
        check(OutlineColor.defaultFor(-1) == (0xFF000000 | OutlineColor.NEUTRAL_GRAY),
                "读不到背景应退回中性灰");
        check(OutlineColor.luminance(0xFFFFFFFF) > 0.99f, "白亮度应为 1");
        check(OutlineColor.luminance(0xFF000000) < 0.01f, "黑亮度应为 0");
        // 中灰背景（L≈0.5 附近）两档不该出现跳跃以外的第三种行为
        check(OutlineColor.luminance(0xFF808080) > 0.2f && OutlineColor.luminance(0xFF808080) < 0.3f,
                "中灰亮度不在预期区间（阈值判据会跑偏）");

        // ---- 三档口径收敛（v1.10.462）
        int bgBright = 0xF2F2F2;
        int bgDark = 0x101010;
        check(OutlineColor.resolve(OutlineColor.Mode.FOLLOW_WALLPAPER, bgBright, 0) ==
                        OutlineColor.outlineFor(bgBright),
                "跟随壁纸档没走到相反值");
        check(OutlineColor.resolve(OutlineColor.Mode.DEFAULT, bgBright, 0) ==
                        OutlineColor.defaultFor(bgBright),
                "默认档没走到中性灰明暗");
        check(OutlineColor.resolve(OutlineColor.Mode.CUSTOM, bgBright, 0x123456) ==
                        0xFF123456,
                "自定义档没给用户颜色 / 没强制不透明");
        check(OutlineColor.resolve(OutlineColor.Mode.CUSTOM, bgBright, 0x00123456) == 0xFF123456,
                "自定义档没有强制不透明（会变淡到看不见）");
        // 两个非自定义档在极端背景上必须给出不同结果（否则"三选一"是摆设）
        check(OutlineColor.resolve(OutlineColor.Mode.FOLLOW_WALLPAPER, bgDark, 0) !=
                        OutlineColor.resolve(OutlineColor.Mode.DEFAULT, bgDark, 0),
                "跟随壁纸与默认在暗背景上给出同一个颜色（三档没有区分度）");

        if (bad == 0) {
            System.out.println("OUTLINECOLOR OK " + ok);
        } else {
            System.out.println("OUTLINECOLOR BAD " + bad);
            System.exit(1);
        }
    }

    private static int maxChannel(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        if (r >= g && r >= b) {
            return 0;
        }
        return g >= b ? 1 : 2;
    }

    private static int minChannel(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        if (r <= g && r <= b) {
            return 0;
        }
        return g <= b ? 1 : 2;
    }

    private OutlineColorTest() {
    }
}
