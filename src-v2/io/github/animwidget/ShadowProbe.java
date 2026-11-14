package io.github.animwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import org.jetbrains.annotations.NotNull;

/**
 * v1.10.234：**S01 阴影探头** —— 在真机上量出两个我们一直只能靠猜的 Skia 常量。
 *
 * <h3>为什么要量</h3>
 * 阴影模型里只剩两个未知量，全部来自 Skia，而它们决定了"参考实机的剖面长什么样"：
 * <ol>
 *   <li><b>σ 与半径的关系</b>：是 {@code σ = r/√3}（= 0.57735r）还是 {@code σ = 0.57735r + 0.5}？
 *       差 3% 量级，但模型里 σ 直接决定宽度和形状。</li>
 *   <li><b>{@code extractAlpha} 带 MaskFilter 时外扩多少</b>：返回位图是"裁到原尺寸"还是
 *       "外扩 m"，m 又等于几个 σ。参考实现把整张结果拉伸到 {@code 画面+4u}，
 *       所以 m 直接决定它剖面里那个 δ（= 高斯 50% 点落在画面边外的距离）。</li>
 * </ol>
 *
 * <p>这两个量在 PC 上查不到（没有 Android 运行时），但**用户的手机上有** —— 所以让 App 自己量一次，
 * 打一行 {@code S01} 到日志里。用户把那一行发出来，模型里的最后一个自由参数就定了。</p>
 *
 * <h3>怎么量</h3>
 * <ul>
 *   <li><b>σ</b>：画一个**台阶边**（左半透明、右半不透明），模糊半径 r 已知。
 *       沿中线读 alpha 剖面，取 50% 点的位置就是台阶位置（校验用），
 *       取 16%/84% 两点距离 d → {@code σ ≈ d/2}。再和 {@code r/√3}、{@code 0.57735r+0.5} 对比。</li>
 *   <li><b>m</b>：对同一张位图调 {@code extractAlpha(blur, offset)}，读返回宽高与 offset。
 *       {@code m = (返回宽 - 原宽)/2}，再除以实测 σ 得 {@code m/σ}。</li>
 * </ul>
 *
 * 只跑一次（进程内静态锁），成本约几毫秒，结果进日志。
 */
final class ShadowProbe {

    private static final int SIZE = 256;       // 探头位图边长
    private static final int RADIUS = 16;      // BlurMaskFilter 半径（px）
    private static boolean done;

    private ShadowProbe() {
    }

    /** 跑一次并写一行 S01 到日志。失败也算"跑过"，不重试。 */
    static void runOnce(Context context) {
        if (done || context == null) {
            return;
        }
        synchronized (ShadowProbe.class) {
            if (done) {
                return;
            }
            done = true;
        }
        try {
            String detail = measure();
            Diag.warn(context.getApplicationContext(), "S01", detail);
        } catch (Throwable t) {
            try {
                Diag.warn(context.getApplicationContext(), "S01", "探头异常: " + t);
            } catch (Throwable ignored) {
            }
        }
        try {
            Diag.warn(context.getApplicationContext(), "S02", calibrateAndTable());
        } catch (Throwable t) {
        }
        try {
            verifyBakedShadow(context.getApplicationContext());
        } catch (Throwable t) {
            try {
                Diag.warn(context.getApplicationContext(), "S03", "探头异常: " + t);
            } catch (Throwable ignored) {
            }
        }
    }

    private static String measure() {
        // ① 台阶位图：x < SIZE/2 全透明，右侧不透明 —— 一条竖直的硬边
        Bitmap step = Bitmap.createBitmap(SIZE, SIZE / 4, Config.ARGB_8888);
        Canvas sc = new Canvas(step);
        Paint solid = new Paint();
        solid.setColor(Color.BLACK);
        sc.drawRect(new RectF(SIZE / 2.0f, 0.0f, SIZE, SIZE / 4.0f), solid);

        // ② 用同一个模糊半径量 σ（台阶 16%/84% 距离 / 2）
        Bitmap blurred = Bitmap.createBitmap(SIZE, SIZE / 4, Config.ARGB_8888);
        Canvas bc = new Canvas(blurred);
        Paint blur = new Paint();
        blur.setColor(Color.BLACK);
        blur.setMaskFilter(new BlurMaskFilter(RADIUS, BlurMaskFilter.Blur.NORMAL));
        bc.drawRect(new RectF(SIZE / 2.0f, 0.0f, SIZE, SIZE / 4.0f), blur);

        // ③ σ：取 10%/90% 两个交叉点（距离 = 2.563σ），亚像素线性插值 + 多行平均
        float sumSigma = 0.0f;
        int rows = 0;
        int peak0 = 0;
        for (int yy = 0; yy < SIZE / 4; yy += Math.max(1, SIZE / 32)) {
            for (int x = 0; x < SIZE; x++) {
                int a = (blurred.getPixel(x, yy) >> 24) & 0xFF;
                if (a > peak0) {
                    peak0 = a;
                }
            }
        }
        if (peak0 < 8) {
            peak0 = 255;
        }
        for (int yy = 0; yy < SIZE / 4; yy += Math.max(1, SIZE / 32)) {
            float lo10 = cross(blurred, yy, peak0 * 10 / 100);
            float hi90 = cross(blurred, yy, peak0 * 90 / 100);
            if (lo10 > 0.0f && hi90 > 0.0f && hi90 > lo10) {
                sumSigma += (hi90 - lo10) / 2.5631f;   // 10%→90% = 2.563σ
                rows++;
            }
        }
        float sigma = rows > 0 ? sumSigma / rows : -1.0f;
        int x50 = Math.round(cross(blurred, SIZE / 8, peak0 / 2));
        float byRoot = RADIUS / 1.7320508f;              // σ = r/√3
        float byPlus = RADIUS * 0.57735f + 0.5f;         // σ = 0.57735r + 0.5

        // ③ extractAlpha 的外扩量
        int[] off = new int[2];
        Paint p = new Paint();
        p.setMaskFilter(new BlurMaskFilter(RADIUS, BlurMaskFilter.Blur.NORMAL));
        Bitmap alpha = step.extractAlpha(p, off);
        int aw = alpha != null ? alpha.getWidth() : -1;
        int ah = alpha != null ? alpha.getHeight() : -1;
        int m = aw > 0 ? (aw - SIZE) / 2 : 0;
        float mOverSigma = sigma > 0.5f ? m / sigma : -1.0f;

        StringBuilder sb = new StringBuilder();
        sb.append("r=").append(RADIUS)
                .append(" 台阶 σ实测=").append(round(sigma))
                .append(" (r/√3=").append(round(byRoot))
                .append(", 0.57735r+0.5=").append(round(byPlus)).append(')')
                .append(" 台阶50%点=").append(x50)
                .append(" 峰=").append(peak0)
                .append(" 行数=").append(rows)
                .append(" | extractAlpha 源=").append(SIZE).append('x').append(SIZE / 4)
                .append(" 返回=").append(aw).append('x').append(ah)
                .append(" offset=").append(off[0]).append(',').append(off[1])
                .append(" → m=").append(m)
                .append(" m/σ=").append(round(mOverSigma));
        if (alpha != null && !alpha.isRecycled()) {
            alpha.recycle();
        }
        blurred.recycle();
        step.recycle();
        return sb.toString();
    }

    /**
     * 判据（v1.10.239 定稿，外部复核给的）：**两个量都要满足，且都写在最终合成 alpha 域**。
     *
     * <ul>
     *   <li>{@link #TOL_FINAL}：逐点最大偏差 ≤ 1.5/255（最终 alpha，即乘过强度之后用户真正看到的那层）；</li>
     *   <li>{@link #TOL_SHIFT}：最优平移 ≤ 0.25px。</li>
     * </ul>
     *
     * <p>为什么容限写在最终 alpha 域：mask 域（bake 的 0~255）里贴边斜率约 2.5/255·px⁻¹，
     * 0.25px 的采样步长残差就是 ~1.5 级 mask，直接在 mask 域卡 1.5 会贴线误报。
     * 换到最终 alpha（默认档乘 120/255 = 0.47）后，同样的 1.5/255 对应 mask ~3.2 级，
     * 留出半个像素的余量；而"用户看到的偏差"也正是这个域的量。</p>
     *
     * <p><b>单位</b>：本类的 L∞ 一律印成 {@code 0.00/255} 这种「级/255」，所以常量直接写 1.5f
     * （= 1.5/255 归一化）。别写成 1.5f/255f —— 那会小 255 倍，永远判 OK（v1.10.239 踩过）。</p>
     */
    private static final float TOL_FINAL = 1.5f;

    /** 最优平移容限（px）。像素中心口径下应当回到 0；稳定不为 0 才是真的系统偏移。 */
    private static final float TOL_SHIFT = 0.25f;

    /**
     * S03：**上机测我们自己烘焙出的阴影位图**（外部复核要求）。
     *
     * <p>为什么必须单独测：参考实现走 {@code extractAlpha + 拉伸}，我们走
     * {@code drawPath + BlurMaskFilter} —— **不是同一条代码路径**。模型说剖面应该是
     * {@code α(d)=A·Φ((δ−d)/σ)}，那就把这条路径真跑一遍，逐点比。</p>
     *
     * <p>每个 box 打两行：<b>S03</b> = 判定与外扩量，<b>S04</b> = 逐点实测/模型（0~255）。
     * 两个 box 的 {@code win = ⌊1.88σ+0.5⌋} 奇偶不同 —— Skia 的模糊是 3 遍整数盒，
     * 盒宽奇偶决定亚像素相位，只测一种会把相位偏差误当成模型偏差。</p>
     *
     * <p>v1.10.240（外部复核第 2 条）：第三个 box 是 <b>430×310</b> —— 宽与第二个相同、
     * 高度换一个奇数组合。上一轮 y 轴出现 −0.25 正好卡在判据上，需要区分它是
     * 「pad 取整的系统偏移」（那么 y 上会稳定复现）还是「这一组的采样相位」（那么换个高度就消失）。</p>
     */
    private static void verifyBakedShadow(@NotNull Context context) {
        int[][] boxes = {{400, 300, 40}, {430, 300, 43}, {430, 310, 43}};
        for (int[] b : boxes) {
            Result r = null;
            String summary;
            try {
                r = measureBox(b[0], b[1], b[2]);
                summary = r.summary();
            } catch (Throwable t) {
                summary = String.format(java.util.Locale.US, "box=%dx%d 烘焙失败: %s", b[0], b[1], t);
            }
            Diag.warn(context, "S03", summary);
            if (r != null) {
                try {
                    Diag.warn(context, "S04", r.profile());
                } catch (Throwable t) {
                    Diag.warn(context, "S04", "box=" + b[0] + " 剖面异常: " + t);
                }
            }
        }
    }

    /** 一次 S03 测量的全部结果（判定 + 明细），只烘一次位图。 */
    private static final class Result {
        int boxW;
        int boxH;
        int pad;
        int win;
        int base;          // 形内深处的 alpha：必须是 255（bake 的画笔 alpha）
        int edge;          // 紧贴画面外第 1 个像素的 alpha（0~255）
        float sigma;
        float delta;
        float blur;
        float aFrac;       // mask 域 → 最终 alpha 域的倍数（= a255/255）
        int[] profX;       // 画面外第 k 个像素的 alpha（k=0 紧贴画面边）
        int[] profY;
        float lnfX;        // L∞（最终 alpha 域）
        float lnfY;
        float offX;        // 渲染相对模型的偏移（px，正 = 烘出来的阴影更靠外）
        float offY;
        float fitCx;       // 拟合 σ 相对名义 σ 的倍数
        float fitCy;
        float fitLx;       // 拟合后的 L∞（诊断用）
        float fitLy;

        String summary() {
            boolean okBase = base == 255;
            boolean okLx = lnfX <= TOL_FINAL;
            boolean okLy = lnfY <= TOL_FINAL;
            boolean okSx = Math.abs(offX) <= TOL_SHIFT;
            boolean okSy = Math.abs(offY) <= TOL_SHIFT;
            return String.format(java.util.Locale.US,
                    "box=%dx%d pad=%d σ=%.2f δ=%.2f r=%.2f win=%d 基准=%d%s"
                            + " | x L∞=%.2f/255 偏%+.2f 拟合σ%.3f×=%.2f/255"
                            + " | y L∞=%.2f/255 偏%+.2f 拟合σ%.3f×=%.2f/255"
                            + " | 偏>0=偏外 | 贴边(k0) 实测=%d 模型=%.0f | 判定 x=%s y=%s",
                    boxW, boxH, pad, sigma, delta, blur, win, base, okBase ? "✓" : "✗",
                    lnfX, offX, fitCx, fitLx,
                    lnfY, offY, fitCy, fitLy,
                    edge, 255.0 * ShadowSpec.normalCdf((delta - 0.5) / sigma),
                    judge(okBase, okLx, okSx), judge(okBase, okLy, okSy));
        }

        String profile() {
            StringBuilder sb = new StringBuilder("box=");
            sb.append(boxW).append(" k0..").append(profX.length - 1).append(" 实测/模型(0~255) x:");
            pairs(sb, profX, delta, sigma);
            sb.append(" | y:");
            pairs(sb, profY, delta, sigma);
            return sb.toString();
        }

        private static void pairs(StringBuilder sb, int[] prof, float deltaPx, float sigmaPx) {
            for (int k = 0; k < prof.length; k++) {
                double d = k + 0.5;              // 像素中心，画面边在整数坐标
                int want = (int) Math.round(255.0 * ShadowSpec.normalCdf((deltaPx - d) / sigmaPx));
                sb.append(' ').append(prof[k]).append('/').append(want);
            }
        }

        private static String judge(boolean okBase, boolean okL, boolean okS) {
            if (!okBase) {
                return "FAIL(基准)";
            }
            if (okL && okS) {
                return "OK";
            }
            if (!okL && !okS) {
                return "FAIL(L∞+移)";
            }
            return okL ? "FAIL(移)" : "FAIL(L∞)";
        }
    }

    /**
     * 烘一张 box 尺寸的阴影并逐点比模型。
     *
     * <h3>口径（v1.10.239 改，外部复核第 1、2 条）</h3>
     * <ul>
     *   <li><b>像素中心</b>：{@code d = k + 0.5}。画面边在**整数**坐标上
     *       （bake 的 translate 用 {@code pad − δ}，位图又按整数偏移贴出去，
     *       所以像素 k 的中心离画面边正好 k+0.5）。v1.10.238 用 {@code d = k}，
     *       整条曲线被系统性错开半个像素 —— 那 0.5 会一直挂在"平移"列上，
     *       盖住真正的系统偏移。</li>
     *   <li><b>基准 = 255</b>：bake 的画笔 alpha 是 255（强度是贴到画布那一步才乘），
     *       所以模型对比基准必须是 255，并断言形内深处真的是 255。</li>
     *   <li><b>容限在最终 alpha 域</b>：差异先乘 {@code aFrac = a255/255} 再比 1.5/255。</li>
     * </ul>
     */
    private static Result measureBox(int boxW, int boxH, int radius) {
        ShadowSpec spec = ShadowSpec.DEFAULT;
        int a255 = ShadowSpec.alpha255(ShadowSpec.STRENGTH_MAX);
        float aFrac = a255 / 255.0f;
        float rangePx = spec.rangePx(boxW);
        float sigmaPx = ShadowSpec.sigmaPx(rangePx, spec.rho(), a255);
        float deltaPx = ShadowSpec.deltaPx(sigmaPx, spec.rho());
        float blurPx = ShadowSpec.blurRadiusPx(sigmaPx);
        int pad = (int) Math.ceil(rangePx) + 1;
        Path path = new Path();
        path.addRoundRect(new RectF(0.0f, 0.0f, boxW, boxH), radius, radius, Path.Direction.CW);
        ShadowLayer layer = ShadowLayer.obtain(path, boxW, boxH, deltaPx, blurPx, pad, 0, 0, 0, 0);
        if (layer == null) {
            throw new IllegalStateException("烘焙返回 null");
        }
        Bitmap bmp = layer.bitmap();
        if (bmp == null || bmp.isRecycled()) {
            throw new IllegalStateException("位图已回收");
        }
        int realPad = layer.pad();
        Result r = new Result();
        r.boxW = boxW;
        r.boxH = boxH;
        r.pad = realPad;
        r.sigma = sigmaPx;
        r.delta = deltaPx;
        r.blur = blurPx;
        r.aFrac = aFrac;
        r.win = (int) Math.floor(1.88 * sigmaPx + 0.5);      // Skia 盒宽 ≈ ⌊1.88σ+0.5⌋，奇偶＝相位
        r.base = (bmp.getPixel(realPad + boxW / 2, realPad + boxH / 2) >> 24) & 0xFF;
        r.profX = outside(bmp, realPad, boxH / 2, true);
        r.profY = outside(bmp, realPad, boxW / 2, false);
        r.edge = r.profX[0];
        float[] fx = devWithShift(r.profX, deltaPx, sigmaPx, aFrac);
        float[] fy = devWithShift(r.profY, deltaPx, sigmaPx, aFrac);
        // 搜索用的是"模型要平移多少"，取反才有物理含义：
        // 正 = 烘出来的阴影比模型更靠外（pad/取整把轮廓推出去了）。
        float ox = -fx[1];
        float oy = -fy[1];
        r.lnfX = fx[0];
        r.offX = ox == 0.0f ? 0.0f : ox;          // 别让 -0.00 印出来
        r.lnfY = fy[0];
        r.offY = oy == 0.0f ? 0.0f : oy;
        float[] cx = fitSigma(r.profX, deltaPx, sigmaPx, aFrac);
        float[] cy = fitSigma(r.profY, deltaPx, sigmaPx, aFrac);
        r.fitLx = cx[0];
        r.fitCx = cx[1];
        r.fitLy = cy[0];
        r.fitCy = cy[1];
        return r;
    }

    /**
     * 取画面**外**那一串像素的 alpha（0~255），k=0 紧贴画面边。
     *
     * @param mid   画面框内的取样位置（x 轴取中段行 = boxH/2，y 轴取中段列 = boxW/2，
     *              避开四角：角上两根阴影叠加，模型不成立）
     * @param axisX true = 沿 x 走一行；false = 沿 y 走一列
     */
    private static int[] outside(Bitmap bmp, int pad, int mid, boolean axisX) {
        int n = Math.max(pad - 1, 1);
        int[] out = new int[n];
        for (int k = 0; k < n; k++) {
            int i = pad - 1 - k;
            int px = axisX ? bmp.getPixel(i, pad + mid) : bmp.getPixel(pad + mid, i);
            out[k] = (px >> 24) & 0xFF;
        }
        return out;
    }

    /** 模型在像素 k（中心 d = k+0.5+shift）的 alpha（0~255 mask 域）。 */
    private static double wantAt(int k, float deltaPx, float sigmaPx, float shift) {
        double d = k + 0.5 + shift;                 // 像素中心
        return 255.0 * ShadowSpec.normalCdf((deltaPx - d) / sigmaPx);
    }

    /** 该 (σ, 平移) 下逐点最大偏差，**已换到最终 alpha 域**（乘 aFrac）。 */
    private static float worstAt(int[] prof, float deltaPx, float sigmaPx, float shift, float aFrac) {
        return worstAt(prof, deltaPx, sigmaPx, shift, aFrac, 1);
    }

    /** 同上，隔 step 个点取一个（拟合阶段只要形状，不必每点都算）。 */
    private static float worstAt(int[] prof, float deltaPx, float sigmaPx, float shift, float aFrac, int step) {
        float worst = 0.0f;
        for (int k = 0; k < prof.length; k += step) {
            float diff = (float) (Math.abs(prof[k] - wantAt(k, deltaPx, sigmaPx, shift)) * aFrac);
            if (diff > worst) {
                worst = diff;
            }
        }
        return worst;
    }

    /** 固定 σ，在 ±1px 内每 0.25px 搜平移，返回 {最小 L∞（最终 alpha）, 该平移量}。 */
    private static float[] devWithShift(int[] prof, float deltaPx, float sigmaPx, float aFrac) {
        float bestWorst = Float.MAX_VALUE;
        float bestShift = 0.0f;
        for (int i = -4; i <= 4; i++) {
            float shift = i * 0.25f;
            float worst = worstAt(prof, deltaPx, sigmaPx, shift, aFrac);
            if (worst < bestWorst) {
                bestWorst = worst;
                bestShift = shift;
            }
        }
        return new float[]{bestWorst, bestShift};
    }

    /**
     * 诊断：σ 也放开（±15%，0.5% 步长）＋平移（±1px，0.125px 步长）两参数联合拟合。
     *
     * <p>只用来说明"残差是形状不对还是宽度不对"：若拟合后 L∞ 明显下降、倍数稳定偏离 1，
     * 那是 Skia 的实际核宽与 {@code 0.57735r+0.5} 有偏差（要补偿的是半径映射，不是模型），
     * 若拟合后仍然大，那是形状本身就不像高斯（3 遍盒 vs 高斯），才是模型问题。</p>
     *
     * @return {拟合后 L∞, σ 倍数, 平移}
     */
    private static float[] fitSigma(int[] prof, float deltaPx, float sigmaPx, float aFrac) {
        // 粗搜：σ ±15%（步长 1%）、平移 ±1px（步长 0.25px），隔点取样
        float bestWorst = Float.MAX_VALUE;
        float bestC = 1.0f;
        float bestShift = 0.0f;
        for (int ci = -15; ci <= 15; ci++) {
            float c = 1.0f + ci * 0.01f;
            for (int si = -4; si <= 4; si++) {
                float shift = si * 0.25f;
                float worst = worstAt(prof, deltaPx, sigmaPx * c, shift, aFrac, 2);
                if (worst < bestWorst) {
                    bestWorst = worst;
                    bestC = c;
                    bestShift = shift;
                }
            }
        }
        // 细搜：在粗搜结果附近再走一遍（0.2% / 0.0625px），全点比较
        for (int ci = -5; ci <= 5; ci++) {
            float c = bestC + ci * 0.002f;
            for (int si = -4; si <= 4; si++) {
                float shift = bestShift + si * 0.0625f;
                float worst = worstAt(prof, deltaPx, sigmaPx * c, shift, aFrac);
                if (worst < bestWorst) {
                    bestWorst = worst;
                    bestC = c;
                    bestShift = shift;
                }
            }
        }
        return new float[]{bestWorst, bestC, bestShift};
    }

    /**
     * S02：**探头标定 + (m,σ) 小查表**（外部复核要求的两项）。
     *
     * <p>标定：把"已知 σ"的剖面直接写进位图（解析高斯 σ=9.74；再用 3 遍 box、盒宽 21、rms σ=10.49），
     * 用**同一段 10%/90% 代码**去量。读数与真值之差就是探头偏置 ——
     * 有了它才能判断 S01 的 σ=10.38 是"设备核更宽"还是"探头读高"。</p>
     *
     * <p>查表：对部署范围内的几个半径各测一次 extractAlpha 外扩量 m（m 只跟半径有关），
     * 门禁/模型里就能用实测 k=m/σ 而不是猜。</p>
     */
    private static String calibrateAndTable() {
        float calGauss = readSigma(analyticStep(9.74f));
        float calBox = readSigma(boxStep(21));
        StringBuilder sb = new StringBuilder();
        sb.append("标定 解析高斯σ=9.74→读").append(round(calGauss))
                .append("  3遍box(盒宽21,rmsσ=10.49)→读").append(round(calBox));
        sb.append(" | 查表 r:m ");
        int[] radii = {10, 16, 22, 28, 35, 40};
        for (int r : radii) {
            int[] off = new int[2];
            Paint p = new Paint();
            p.setMaskFilter(new BlurMaskFilter(r, BlurMaskFilter.Blur.NORMAL));
            Bitmap a = probeSource().extractAlpha(p, off);
            if (a != null) {
                sb.append(r).append(':').append((a.getWidth() - SIZE) / 2).append(' ');
                if (!a.isRecycled()) {
                    a.recycle();
                }
            }
        }
        return sb.toString();
    }

    private static Bitmap probeSource() {
        Bitmap b = Bitmap.createBitmap(SIZE, SIZE / 4, Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint();
        p.setColor(Color.BLACK);
        c.drawRect(new RectF(SIZE / 2.0f, 0.0f, SIZE, SIZE / 4.0f), p);
        return b;
    }

    /** 解析高斯的台阶剖面（alpha = 255·Φ((x−中心)/σ)），真值 σ 已知。 */
    private static Bitmap analyticStep(float sigma) {
        Bitmap b = Bitmap.createBitmap(SIZE, SIZE / 4, Config.ARGB_8888);
        float center = SIZE / 2.0f;
        for (int x = 0; x < SIZE; x++) {
            int a = Math.round(255.0f * (float) ShadowSpec.normalCdf((x - center) / sigma));
            if (a <= 0) {
                continue;
            }
            int argb = (a << 24) | 0x000000;
            for (int y = 0; y < SIZE / 4; y++) {
                b.setPixel(x, y, argb);
            }
        }
        return b;
    }

    /** 3 遍 box 的台阶剖面（盒宽 w，rms σ = √(3(w²−1)/12)），模拟 Android 的模糊核形状。 */
    private static Bitmap boxStep(int w) {
        int n = SIZE;
        float[] v = new float[n];
        for (int x = 0; x < n; x++) {
            v[x] = x >= n / 2 ? 255.0f : 0.0f;
        }
        for (int pass = 0; pass < 3; pass++) {
            float[] out = new float[n];
            int half = w / 2;
            for (int x = 0; x < n; x++) {
                float sum = 0.0f;
                int cnt = 0;
                for (int k = -half; k <= half; k++) {
                    int i = x + k;
                    if (i >= 0 && i < n) {
                        sum += v[i];
                        cnt++;
                    }
                }
                out[x] = cnt > 0 ? sum / cnt : 0.0f;
            }
            v = out;
        }
        Bitmap b = Bitmap.createBitmap(n, SIZE / 4, Config.ARGB_8888);
        for (int x = 0; x < n; x++) {
            int a = Math.round(v[x]);
            if (a <= 0) {
                continue;
            }
            int argb = (a << 24) | 0x000000;
            for (int y = 0; y < SIZE / 4; y++) {
                b.setPixel(x, y, argb);
            }
        }
        return b;
    }

    /** 用和 S01 完全相同的 10%/90% 亚像素方法读 σ。 */
    private static float readSigma(Bitmap b) {
        int peak = 0;
        for (int y = 0; y < b.getHeight(); y += Math.max(1, b.getHeight() / 8)) {
            for (int x = 0; x < b.getWidth(); x++) {
                int a = (b.getPixel(x, y) >> 24) & 0xFF;
                if (a > peak) {
                    peak = a;
                }
            }
        }
        if (peak < 8) {
            return -1.0f;
        }
        float sum = 0.0f;
        int rows = 0;
        for (int y = 0; y < b.getHeight(); y += Math.max(1, b.getHeight() / 8)) {
            float lo = cross(b, y, peak * 10 / 100);
            float hi = cross(b, y, peak * 90 / 100);
            if (lo > 0.0f && hi > lo) {
                sum += (hi - lo) / 2.5631f;
                rows++;
            }
        }
        b.recycle();
        return rows > 0 ? sum / rows : -1.0f;
    }

    /** 某一行 alpha 首次达到 thr 的位置（亚像素线性插值）；找不到返回 -1。 */
    private static float cross(Bitmap b, int y, int thr) {
        int prev = 0;
        for (int x = 0; x < b.getWidth(); x++) {
            int a = (b.getPixel(x, y) >> 24) & 0xFF;
            if (a >= thr) {
                if (x == 0 || a <= prev) {
                    return x;
                }
                float t = (thr - prev) / (float) (a - prev);
                return (x - 1) + t;
            }
            prev = a;
        }
        return -1.0f;
    }

    private static String round(float v) {
        return String.format(java.util.Locale.US, "%.2f", Float.valueOf(v));
    }
}
