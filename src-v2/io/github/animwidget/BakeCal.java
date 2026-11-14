package io.github.animwidget;

import android.content.Context;

/**
 * v1.10.321（B：`budgetFor` 面积模型 + 阶梯 + 自校准）—— Android 侧的胶水。
 *
 * 纯数学在 {@link BakeLadder}（离线可对拍，见 scripts/bakeladder-math-check.sh），
 * 这里只负责：读/写自校准系数、按实测吞吐反向修正、打一行 W34 留痕。
 *
 * ## 为什么需要自校准
 *
 * 画布预算以前是一串写死的数（24 万 / 90 万 / 220 万），依据是同一次实测的线性外推。
 * 但"这台机器烘一帧要多久"会随负载、发热、线程数、编码格式（WebP / JPEG）变化 ——
 * 写死的预算只在当时那台机器的那个状态下成立。表现就是 v1.10.291 那种账：
 *   预热 26 帧/秒 < 播放 50 帧/秒 → 命中率恒 0% → 桌面全程放顶帧/跳过。
 *
 * ## 一次都不许动的部分
 *
 * `HIRES_AREA`（220 万，全不透明帧走 JPEG 的 1:1 档）**不参与自校准**：
 * 那条路的编码成本已被实测证明可以忽略（JPEG q95 ≈ 5ms/帧），拿预热吞吐去压它只会白掉清晰度。
 *
 * v1.10.333 起**动图「1:1」档也不参与**（判据：{@link BakeLadder#calibrated}）：
 * 实测证明拿被并发污染的吞吐去反推"1:1"只会让桌面永久放大 1.36 倍，而一帧都没多出来。
 * 现在只有「标准（0）」和「高清（1）」两档会走系数与阶梯。
 */
public final class BakeCal {

    private static final String PREF = "settings";
    /**
     * v1.10.329：**每个组件一份系数**（原来是全局一份）。
     *
     * 实测（20:35:54~20:36:25 的日志）：同一个进程里，
     *   · 1:1 素材的 3×3 组件在 16.5 万画布上烘 130 帧/秒（远高于目标 58）
     *   · 但全局系数被别的组件/别的画布拖到 0.66，于是它也被压到 16 万画布（1.3 倍放大、发虚）
     *   · 随后按 1.06 一档往回爬（0.66→0.70→0.75→0.79），画布跟着从 386 涨到 445
     * 这就是"清晰度自己在抖"。全局系数等于把两个不同速度的组件绑在一个控制器上。
     */
    private static final String KEY_PREFIX = "bake_cal_w";

    private BakeCal() {
    }

    private static String key(int widgetId) {
        return KEY_PREFIX + widgetId;
    }

    /** 当前自校准系数（1.0 = 不压；越小 = 用得越多）。按组件独立。 */
    static float factor(Context context, int widgetId) {
        try {
            return BakeLadder.clamp(context.getSharedPreferences(PREF, 0)
                    .getFloat(key(widgetId), 1.0f));
        } catch (Throwable th) {
            return BakeLadder.MAX;
        }
    }

    /** 把「想烘多大面积」变成「这一拍真的用多大面积」（乘系数 + 夹阶梯）。 */
    static int budget(Context context, int widgetId, int wanted) {
        if (wanted <= 0) {
            return wanted;
        }
        return BakeLadder.budgetFor(factor(context, widgetId), wanted);
    }

    /** v1.10.334：这一档不需要系数时，把残留的旧系数复位成 1.0。 */
    static void resetIfStale(Context context, int widgetId) {
        try {
            float now = factor(context, widgetId);
            if (now >= 0.999f) {
                return;
            }
            context.getSharedPreferences(PREF, 0).edit().putFloat(key(widgetId), BakeLadder.MAX).apply();
            Diag.warn(context, "W34", "预算自校准 复位=本档不参与 系数=" + fmt(now)
                    + "→1.00（画布不看系数；不复位的话，以后切回高清会突然被压 " 
                    + Math.round((1f - now) * 100f) + "%）");
        } catch (Throwable ignored) {
        }
    }

    /**
     * 预热结束：用**这一批的实测吞吐**修正系数。
     *
     * @param targetFps 播放需要的帧率（组件配置的 fps）
     * @param spentMs   这批实际墙钟耗时
     * @param frames    这批真正烘出来的帧数（失败帧不算）
     * @param alone     这批是不是「独占且全程没给前台任务让路」（v1.10.333）
     * @param why       不是独占时的原因（只用于日志）
     */
    static void note(Context context, int widgetId, int targetFps, long spentMs, int frames,
                     boolean alone, String why) {
        try {
            // 样本太小就别参与：一两帧的耗时几乎全是调度噪声
            if (targetFps <= 0 || frames < 24 || spentMs < 400L) {
                return;
            }
            float measured = frames * 1000f / spentMs;      // 聚合烘速（帧/秒）
            float target = targetFps * 1.15f;               // 要留 15% 余量才叫"烘得比播得快"
            float now = factor(context, widgetId);
            // v1.10.333：**并发的账不许记到画质上**。
            // 见 WidgetPrewarm 的 BAKE_SLOT / YIELDED 注释：同一画布在"和导出撞车"时实测
            // 32 帧/秒、独占时 50 帧/秒 —— 拿前者去算比例控制会一步把系数砸到 0.57。
            if (!alone) {
                Diag.warn(context, "W34", "预算自校准 跳过=" + (why == null || why.isEmpty() ? "非独占" : why)
                        + " 实测=" + Math.round(measured) + "帧/秒 系数=" + fmt(now)
                        + "（不变）帧=" + frames + " 用时=" + spentMs + "ms");
                return;
            }
            float next = BakeLadder.nextFactor(now, measured, target);
            if (Math.abs(next - now) < 0.005f) {
                return;
            }
            context.getSharedPreferences(PREF, 0).edit().putFloat(key(widgetId), next).apply();
            Diag.warn(context, "W34", "预算自校准 实测=" + Math.round(measured) + "帧/秒"
                    + " 目标=" + Math.round(target) + "帧/秒"
                    + " 系数=" + fmt(now) + "→" + fmt(next)
                    + " 帧=" + frames + " 用时=" + spentMs + "ms");
        } catch (Throwable ignored) {
        }
    }

    private static String fmt(float v) {
        return String.format(java.util.Locale.US, "%.2f", v);
    }
}
