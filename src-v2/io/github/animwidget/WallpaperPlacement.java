package io.github.animwidget;

/**
 * v1.10.519（用户决定：**frame 用「整块显示区」**）：壁纸底图在显示区里的唯一摆法。
 *
 * <h3>为什么原来会错（v1.10.518 实测 Δy=+839px）</h3>
 *
 * <p>旧代码把壁纸 `<b>centerCrop</b> 到预览控件自身那个 1260×738 的小框`里。centerCrop 的缩放是
 * {@code max(vw/bw, vh/bh)}：当位图宽度正好等于屏宽时它取 1.0（不缩放），于是**裁剪窗口在框内垂直居中**，
 * 即从位图 {@code (bh-738)/2} 那一行开始画。桌面却是从 y=0 开始铺的 —— 两者相差的正是
 * {@code (bh-738)/2}，与「预览控件自己的高」无关，也与状态栏/导航栏无关。</p>
 *
 * <p>所以修法不是加一个修正量（那会在三键/手势、字体缩放下各错一次），而是换**参照系**：
 * 壁纸先按 center-crop 铺满**整块显示区**，预览只做一件事 —— 把「自己那块矩形在显示区里的位置」
 * 那一部分切出来画。于是「同一像素在桌面与预览里的 y」按定义就相等，Δy 恒为 0。</p>
 *
 * <h3>为什么这个类里没有一行 android 代码</h3>
 *
 * <p>摆法是可以脱机对拍的数学：{@code tests/WallpaperPlacementTest.java} 逐条断言它
 * （门禁 [34]）。粘在 View 里就只能靠真机截图验。</p>
 *
 * <p>输入输出都是整数像素：**先对源矩形取整，再由取整后的源矩形反推目标矩形**，
 * 这样缩放系数不为 1 时也不会出现「源和目标各自取整 → 采样漂移一像素」的缝。</p>
 */
public final class WallpaperPlacement {

    private WallpaperPlacement() {
    }

    /**
     * 铺满整块显示区所需的最小缩放（= cover）：宽、高各算一个比例，取**大**的那个。
     * 参数非法（0 / 负）时返回 1.0，让调用方退化成「原样画」，绝不返回 0 或 NaN。
     */
    public static double coverScale(int bmpW, int bmpH, int dispW, int dispH) {
        if (bmpW <= 0 || bmpH <= 0 || dispW <= 0 || dispH <= 0) {
            return 1.0;
        }
        double sx = dispW / (double) bmpW;
        double sy = dispH / (double) bmpH;
        return Math.max(sx, sy);
    }

    /** 位图左上角在显示区坐标系里的 x（cover 居中，可能为负 = 左右溢出被裁）。 */
    public static double originX(int bmpW, int dispW, double scale) {
        return (dispW - bmpW * scale) / 2.0;
    }

    /** 位图左上角在显示区坐标系里的 y（cover 居中，可能为负 = 上下溢出被裁）。 */
    public static double originY(int bmpH, int dispH, double scale) {
        return (dispH - bmpH * scale) / 2.0;
    }

    /**
     * 把「位于显示区坐标 (viewX, viewY, viewW×viewH) 的那块视口」切成两个矩形。
     *
     * @param bmpW 位图宽（px）
     * @param bmpH 位图高（px）
     * @param dispW 整块显示区宽（px，含系统栏）
     * @param dispH 整块显示区高（px，含系统栏）
     * @param viewX 视口左上角在**显示区坐标系**里的 x
     * @param viewY 视口左上角在**显示区坐标系**里的 y
     * @param out   长度 ≥ 8 的输出：{srcL, srcT, srcR, srcB, dstL, dstT, dstR, dstB}，
     *              dst 是**视口本地坐标**（直接可以交给 Canvas.drawBitmap）
     * @return false = 视口完全不在这张位图的覆盖范围内（不该画，别画成黑块）
     */
    public static boolean slice(int bmpW, int bmpH, int dispW, int dispH,
                                int viewX, int viewY, int viewW, int viewH, int[] out) {
        if (out == null || out.length < 8) {
            return false;
        }
        if (bmpW <= 0 || bmpH <= 0 || dispW <= 0 || dispH <= 0 || viewW <= 0 || viewH <= 0) {
            return false;
        }
        double s = coverScale(bmpW, bmpH, dispW, dispH);
        double ox = originX(bmpW, dispW, s);
        double oy = originY(bmpH, dispH, s);
        double coverR = ox + bmpW * s;
        double coverB = oy + bmpH * s;
        double x0 = Math.max(viewX, ox);
        double x1 = Math.min(viewX + (double) viewW, coverR);
        double y0 = Math.max(viewY, oy);
        double y1 = Math.min(viewY + (double) viewH, coverB);
        // 半个像素以内当作不可见：避免四舍五入后出现 0 宽的裁剪被画成一条黑线
        if (x1 - x0 < 0.5 || y1 - y0 < 0.5) {
            return false;
        }
        int srcL = clamp(round((x0 - ox) / s), 0, bmpW);
        int srcT = clamp(round((y0 - oy) / s), 0, bmpH);
        int srcR = clamp(round((x1 - ox) / s), srcL, bmpW);
        int srcB = clamp(round((y1 - oy) / s), srcT, bmpH);
        if (srcR <= srcL || srcB <= srcT) {
            return false;
        }
        out[0] = srcL;
        out[1] = srcT;
        out[2] = srcR;
        out[3] = srcB;
        // v1.10.519（脱机对拍当场抓到）：目标矩形**不能**由取整后的源矩形反推 ——
        // 那样边缘会落在 -1 或 viewH+1，框内就露一条 1px 的透明缝（底下是兜底底色，肉眼可见）。
        // 目标矩形直接取「可见交集」在视口里的位置：视口一定落在 cover 区域内时它恒等于整块视口，
        // 于是「无缝」是恒等式而不是靠夹取凑出来的。
        out[4] = clamp(round(x0 - viewX), 0, viewW);
        out[5] = clamp(round(y0 - viewY), 0, viewH);
        out[6] = clamp(round(x1 - viewX), out[4], viewW);
        out[7] = clamp(round(y1 - viewY), out[5], viewH);
        return true;
    }

    /** 四舍五入到整数像素（负数也按「远离 0 之外的四舍五入」处理，不截断）。 */
    public static int round(double v) {
        return (int) Math.floor(v + 0.5);
    }

    static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
