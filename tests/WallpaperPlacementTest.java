package io.github.animwidget;

/**
 * v1.10.519：壁纸底图对位的**离线**对拍（进 APK 门禁 [34]，不进 APK）。
 *
 * <p>为什么必须脱机对拍：这个算法的唯一直观验收是「拿桌面截图和编辑页截图各量一次 Δy」，
 * 而 Δy 的错法**不崩、不报、只是慢慢错位**（v1.10.518 就是这样漂了 839px 没人发现）。
 * 所以把摆法抽成纯函数，在这里把「哪些情形下 Δy 按定义必须是 0」逐条钉住：
 * 位图=显示区 / 位图更矮（靠放大铺满）/ 位图更宽（视差余量）/ 位图更高（上下被裁）/ 视口在屏幕中部或底部。</p>
 *
 * <p>跑法（门禁里就是这么跑的，只需要 JDK）：</p>
 * <pre>
 *   javac -d /tmp/wp WallpaperPlacement.java WallpaperPlacementTest.java
 *   java -cp /tmp/wp io.github.animwidget.WallpaperPlacementTest
 * </pre>
 */
public final class WallpaperPlacementTest {

    private static int ok;

    private static int bad;

    public static void main(String[] args) {
        // ---- 1. 位图 = 显示区：这就是「缩放 1.000、图左上 0,0」的情形，Δy 按定义 0 ----
        int[] out = new int[8];
        check("位图=显示区 视口在顶部：源=目标=整块视口", slice(1260, 2800, 1260, 2800, 0, 0, 1260, 738, out)
                && out[0] == 0 && out[1] == 0 && out[2] == 1260 && out[3] == 738
                && out[4] == 0 && out[5] == 0 && out[6] == 1260 && out[7] == 738);

        // ---- 2. 视口在屏幕中部：源矩形必须整体下移相同的 y（桌面同一像素就在这里）----
        check("位图=显示区 视口在 y=1031：源从第 1031 行开始", slice(1260, 2800, 1260, 2800, 0, 1031, 1260, 738, out)
                && out[1] == 1031 && out[3] == 1769 && out[5] == 0 && out[7] == 738);

        // ---- 3. 位图比显示区矮（v1.10.518 那台机器的反解值 2416）：靠放大铺满，视口顶端仍然是位图第 0 行 ----
        check("位图 2416 高 < 显示区 2800：顶端仍从第 0 行起（Δy=0）", slice(1260, 2416, 1260, 2800, 0, 0, 1260, 738, out)
                && out[1] == 0 && out[5] == 0 && out[7] == 738 && out[4] == 0 && out[6] == 1260);
        check("位图 2416 高：视口在屏幕最底时源矩形正好到 2416（底边对齐不越界）",
                slice(1260, 2416, 1260, 2800, 0, 2062, 1260, 738, out)
                        && out[3] == 2416 && out[5] == 0 && out[7] == 738);
        check("位图 2416 高：缩放 = 2800/2416（不是 1.0，否则底部会露一条缝）",
                Math.abs(WallpaperPlacement.coverScale(1260, 2416, 1260, 2800) - 2800.0 / 2416.0) < 1e-9);

        // ---- 4. 视差余量：位图比屏幕宽 → 横向居中裁掉两边，仍铺满 ----
        check("位图 1336x2800（视差余量）：左右各裁 38px，画面仍铺满",
                slice(1336, 2800, 1260, 2800, 0, 0, 1260, 738, out)
                        && out[0] == 38 && out[2] == 1298 && out[4] == 0 && out[6] == 1260);

        // ---- 5. 位图比显示区高：上下居中裁 ----
        check("位图 1260x3600：顶部被裁 400px，视口顶端从第 400 行起",
                slice(1260, 3600, 1260, 2800, 0, 0, 1260, 738, out)
                        && out[1] == 400 && out[5] == 0 && out[7] == 738);

        // ---- 6. 小图：放大 2 倍铺满 ----
        check("位图 630x1400：缩放 2.0，视口顶端仍 0", slice(630, 1400, 1260, 2800, 0, 0, 1260, 738, out)
                && out[1] == 0 && out[3] == 369 && out[5] == 0 && out[7] == 738
                && Math.abs(WallpaperPlacement.coverScale(630, 1400, 1260, 2800) - 2.0) < 1e-9);

        // ---- 7. 视口完全在显示区外：不许画（画出来就是一条黑边）----
        check("视口在屏幕上方之外 → 不画", !slice(1260, 2800, 1260, 2800, 0, -2000, 1260, 738, out));
        check("视口在屏幕下方之外 → 不画", !slice(1260, 2800, 1260, 2800, 0, 2800, 1260, 738, out));

        // ---- 8. 非法输入绝不返回 true / NaN ----
        check("位图 0 尺寸 → 不画", !slice(0, 0, 1260, 2800, 0, 0, 1260, 738, out));
        check("视口 0 尺寸 → 不画", !slice(1260, 2800, 1260, 2800, 0, 0, 0, 738, out));
        check("coverScale 非法输入退化为 1.0（不是 0 / NaN）",
                WallpaperPlacement.coverScale(0, 0, 0, 0) == 1.0
                        && !Double.isNaN(WallpaperPlacement.coverScale(0, 5, 7, 0)));

        // ---- 9. 位图=显示区时，整条 y 轴上「源行号 = 视口顶」且目标恒从 0 起 ----
        boolean identity = true;
        for (int y = 0; y + 738 <= 2800; y += 137) {
            if (!slice(1260, 2800, 1260, 2800, 0, y, 1260, 738, out)
                    || out[1] != y || out[5] != 0 || out[7] != 738) {
                identity = false;
                break;
            }
        }
        check("位图=显示区：y 轴上每一处都满足「源起点=视口顶、目标顶=0」", identity);

        // ---- 10. 缩放 ≠ 1 时行数守恒：源高 ≈ 视口高/s，且目标矩形**恒定铺满视口**（不留 1px 缝）----
        boolean rows = true;
        for (int y = 0; y + 738 <= 2800; y += 211) {
            if (!slice(1260, 2416, 1260, 2800, 0, y, 1260, 738, out)) {
                rows = false;
                break;
            }
            int srcH = out[3] - out[1];
            if (out[4] != 0 || out[5] != 0 || out[6] != 1260 || out[7] != 738
                    || srcH < 630 || srcH > 640) {
                rows = false;
                break;
            }
        }
        check("缩放 1.159：目标恒铺满视口（无 1px 缝）、源高 ≈ 视口高/缩放", rows);

        // ---- 11. 「视口顶端 → 目标顶端为 0」在任意位图尺寸下都必须成立（这就是 Δy=0 的数学形式）----
        int[][] shapes = {
                {1260, 2800}, {1260, 2416}, {1260, 3600}, {1336, 2800}, {630, 1400}, {2520, 5600}, {1000, 2000},
        };
        boolean topAlwaysZero = true;
        for (int[] shape : shapes) {
            if (!slice(shape[0], shape[1], 1260, 2800, 0, 0, 1260, 738, out) || out[5] != 0) {
                topAlwaysZero = false;
                break;
            }
        }
        check("7 种位图尺寸：视口顶端时目标顶恒为 0（壁纸不会被整体上移/下移）", topAlwaysZero);

        // ---- 11b. 铺满不变量：只要视口落在显示区内，目标矩形就必须正好是整块视口（没有 1px 缝）----
        boolean filled = true;
        for (int[] shape : shapes) {
            for (int y = 0; y + 738 <= 2800; y += 173) {
                if (!slice(shape[0], shape[1], 1260, 2800, 0, y, 1260, 738, out)
                        || out[4] != 0 || out[5] != 0 || out[6] != 1260 || out[7] != 738) {
                    filled = false;
                    break;
                }
            }
            if (!filled) {
                break;
            }
        }
        check("7 种位图尺寸 × 逐档 y：目标矩形恒为整块视口（缩放取整也不露缝）", filled);

        // ---- 12. 四舍五入到「最近像素」而不是截断：截断会让画面缓慢上漂 ----
        check("round(0.5)=1 round(1.4)=1 round(2.6)=3", WallpaperPlacement.round(0.5) == 1
                && WallpaperPlacement.round(1.4) == 1 && WallpaperPlacement.round(2.6) == 3);

        if (bad == 0) {
            System.out.println("WALLPAPERPLACE OK " + ok);
        } else {
            System.out.println("WALLPAPERPLACE FAILED ok=" + ok + " bad=" + bad);
            System.exit(1);
        }
    }

    private static boolean slice(int bmpW, int bmpH, int dispW, int dispH,
                                 int viewX, int viewY, int viewW, int viewH, int[] out) {
        return WallpaperPlacement.slice(bmpW, bmpH, dispW, dispH, viewX, viewY, viewW, viewH, out);
    }

    private static void check(String name, boolean cond) {
        if (cond) {
            ok++;
        } else {
            bad++;
            System.out.println("FAIL " + name);
        }
    }
}
