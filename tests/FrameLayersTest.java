package io.github.animwidget;

/**
 * v1.10.459：分层指纹的离线自检（进 APK 门禁 [22]，不进 APK）。
 *
 * <p>为什么值得单独测：指纹是「overlay 能不能跨帧复用」的**唯一判据**。
 * 它错成"太敏感"→ 每帧都重建 overlay，等于没做；错成"太迟钝"→ 改了阴影还用旧 overlay，
 * 画面直接错。这两种错都不会崩、也不会报错，只能靠断言钉住。
 */
public final class FrameLayersTest {

    public static void main(String[] args) {
        int ok = 0;
        int bad = 0;

        String base = fp("FIT", "ROUNDED", 100, false, 100, 0, 0, true, 12, "",
                0f, 0f, 100, 0f, 0f, 600, 600);
        String again = fp("FIT", "ROUNDED", 100, false, 100, 0, 0, true, 12, "",
                0f, 0f, 100, 0f, 0f, 600, 600);
        if (base.equals(again)) {
            ok++;
        } else {
            bad++;
            System.out.println("FAIL 同样的参数给出不同指纹");
        }

        // 决定 overlay 的参数：每一个单独变都必须改指纹
        String[][] cases = {
                {"布局", fp("FILL", "ROUNDED", 100, false, 100, 0, 0, true, 12, "", 0f, 0f, 100, 0f, 0f, 600, 600)},
                {"蒙版", fp("FIT", "CIRCLE", 100, false, 100, 0, 0, true, 12, "", 0f, 0f, 100, 0f, 0f, 600, 600)},
                {"透明度", fp("FIT", "ROUNDED", 90, false, 100, 0, 0, true, 12, "", 0f, 0f, 100, 0f, 0f, 600, 600)},
                {"边框", fp("FIT", "ROUNDED", 100, true, 100, 0, 0, true, 12, "", 0f, 0f, 100, 0f, 0f, 600, 600)},
                {"缩放", fp("FIT", "ROUNDED", 100, false, 90, 0, 0, true, 12, "", 0f, 0f, 100, 0f, 0f, 600, 600)},
                {"内容旋转", fp("FIT", "ROUNDED", 100, false, 100, 15, 0, true, 12, "", 0f, 0f, 100, 0f, 0f, 600, 600)},
                {"蒙版旋转", fp("FIT", "ROUNDED", 100, false, 100, 0, 30, true, 12, "", 0f, 0f, 100, 0f, 0f, 600, 600)},
                {"适配内容", fp("FIT", "ROUNDED", 100, false, 100, 0, 0, false, 12, "", 0f, 0f, 100, 0f, 0f, 600, 600)},
                {"阴影强度", fp("FIT", "ROUNDED", 100, false, 100, 0, 0, true, 40, "", 0f, 0f, 100, 0f, 0f, 600, 600)},
                {"阴影曲线", fp("FIT", "ROUNDED", 100, false, 100, 0, 0, true, 12, "0.4,0.6", 0f, 0f, 100, 0f, 0f, 600, 600)},
                {"水平偏移", fp("FIT", "ROUNDED", 100, false, 100, 0, 0, true, 12, "", 5f, 0f, 100, 0f, 0f, 600, 600)},
                {"垂直偏移", fp("FIT", "ROUNDED", 100, false, 100, 0, 0, true, 12, "", 0f, -5f, 100, 0f, 0f, 600, 600)},
                {"内容缩放率", fp("FIT", "ROUNDED", 100, false, 100, 0, 0, true, 12, "", 0f, 0f, 120, 0f, 0f, 600, 600)},
                {"蒙版水平偏移", fp("FIT", "ROUNDED", 100, false, 100, 0, 0, true, 12, "", 0f, 0f, 100, 3f, 0f, 600, 600)},
                {"蒙版垂直偏移", fp("FIT", "ROUNDED", 100, false, 100, 0, 0, true, 12, "", 0f, 0f, 100, 0f, 3f, 600, 600)},
                {"画布宽", fp("FIT", "ROUNDED", 100, false, 100, 0, 0, true, 12, "", 0f, 0f, 100, 0f, 0f, 601, 600)},
                {"画布高", fp("FIT", "ROUNDED", 100, false, 100, 0, 0, true, 12, "", 0f, 0f, 100, 0f, 0f, 600, 601)},
        };
        for (String[] c : cases) {
            if (c[1] != null && !c[1].equals(base)) {
                ok++;
            } else {
                bad++;
                System.out.println("FAIL 改「" + c[0] + "」后指纹没变（overlay 会被错误复用）");
            }
        }

        // 浮点抖动不算变化（0.30 与 0.30000001 必须同指纹）
        String a = fp("FIT", "ROUNDED", 100, false, 100, 0, 0, true, 12, "", 0.30f, 0f, 100, 0f, 0f, 600, 600);
        String b = fp("FIT", "ROUNDED", 100, false, 100, 0, 0, true, 12, "", 0.30000001f, 0f, 100, 0f, 0f, 600, 600);
        if (a.equals(b)) {
            ok++;
        } else {
            bad++;
            System.out.println("FAIL 浮点抖动导致指纹变化（会每帧重建 overlay）");
        }

        // 空曲线与 null 曲线等价
        String n = fp("FIT", "ROUNDED", 100, false, 100, 0, 0, true, 12, null, 0f, 0f, 100, 0f, 0f, 600, 600);
        String e = fp("FIT", "ROUNDED", 100, false, 100, 0, 0, true, 12, "", 0f, 0f, 100, 0f, 0f, 600, 600);
        if (n.equals(e)) {
            ok++;
        } else {
            bad++;
            System.out.println("FAIL null 曲线与空曲线指纹不同");
        }

        if (bad == 0) {
            System.out.println("FRAMELAYERS OK " + ok);
        } else {
            System.out.println("FRAMELAYERS BAD " + bad);
            System.exit(1);
        }
    }

    private static String fp(String layout, String shape, int alpha, boolean border, int scale,
                             int rotation, int shapeRotation, boolean fit, int shadow,
                             String curve, float ox, float oy, int contentScale,
                             float mx, float my, int w, int h) {
        return FrameLayers.fingerprint(layout, shape, alpha, border, scale, rotation, shapeRotation,
                fit, shadow, curve, ox, oy, contentScale, mx, my, w, h);
    }

    private FrameLayersTest() {
    }
}
