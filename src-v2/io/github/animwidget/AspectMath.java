package io.github.animwidget;

/**
 * v1.10.278：**比例判据的唯一算术出处**（纯函数，不碰 Android 类，可脱机门禁）。
 *
 * 背景（LESSONS）：v1.10.270~277 连续四个版本在「调整组件大小后画面被截断 / 留一圈壁纸」
 * 上打转，根因不是公式难，而是**同一条判据有两个实现、两个作者**：
 *   · 合成层按「原始比例」把卡片画成比格子小的一块；
 *   · 桌面 ImageView 的 scaleType（fitCenter / centerCrop）又各自裁一遍。
 * 日志于是自相矛盾（「留壁纸」与实际「在裁」同时出现），我按日志下的结论全是错的。
 *
 * 现在两个问题彻底拆开：
 *   ① 画布该多宽 → **永远跟格子同形**（v1.10.279 起，原始比例也一样）。「铺满」由合成层
 *      的衬底实现（FrameComposer 的 bleed：卡片=格子、画面完整居中、四周用同一帧柔化补满），
 *      不再用"裁掉画面多出的边"去凑铺满 —— 用户对裁切的称呼就是"截断"。
 *   ② 桌面该用哪种 scaleType（{@link #needFitCenter}）→ 只由「画布比例 vs 格子比例」定；
 *      相等（≤5%）时 centerCrop 是**恒等变换**，桌面不多不少铺满；不等时才 fitCenter
 *      （固定比例布局 1:1/16:9 的卡片不能被切掉圆角）。
 *
 * 这两个函数被 WidgetRender（合成/画布）、WidgetProvider（W22 日志）共用，
 * 并由 scripts/layout-math-check.sh 用真实编译产物 + 断言表守住（门禁 [12]）。
 */
public final class AspectMath {

    /**
     * 「画布与格子同比例」的容差；超过才需要 fitCenter。
     *
     * 5% ⇒ 桌面每边最多裁 2.5%（1030 宽即 26px，肉眼看不出），换来的是**绝不留壁纸条**；
     * 而 fitCenter 在同样误差下会留出一样宽的一条壁纸，用户对那条壁纸的读法就是「没铺满」。
     * 素材比这更不匹配（16:9 进方格子 = 78%）时才 fitCenter —— 那时候保卡片圆角更重要。
     */
    public static final float SAME_ASPECT_TOLERANCE = 0.05f;

    private AspectMath() {
    }

    /**
     * 两个宽高比的相对差（以较小者为分母，正数）。
     * 任一侧非正 → 返回 -1（调用方据此走"不知道"分支，绝不当 0 处理）。
     */
    public static float diff(float a, float b) {
        if (!(a > 0.0f) || !(b > 0.0f)) {
            return -1.0f;
        }
        float lo = Math.min(a, b);
        return Math.abs(a - b) / lo;
    }

    /** 画布比例 vs 格子比例：true = 桌面必须 fitCenter（否则 centerCrop 会切掉卡片边缘）。 */
    public static boolean needFitCenter(float canvasRatio, float boxRatio) {
        float d = diff(canvasRatio, boxRatio);
        return d < 0.0f || d > SAME_ASPECT_TOLERANCE;
    }

    /**
     * v1.10.497（外部评审 Q5 落地）：**画布 → 桌面格子的显示倍率**（centerCrop 口径）。
     *
     * <p>为什么必须单独算一项：{@code PlaybackContract.contentScale} 判的是「素材 → 画布」，
     * 桌面上还会再缩放一次。`widget_media.xml` 的 ImageView 是 `scaleType="centerCrop"`
     * ⇒ 桌面按 **cover** 缩放：{@code max(格子宽/画布宽, 格子高/画布高)}。</p>
     *
     * <p>于是会出现「清晰度=OK 但仍然糊」：画布 1280×720（对 720p 源是 1.00×），
     * 格子 869×869 → cover = 869/720 = **1.21×**，桌面把画布放大 21%。
     * 解法只有两个：勾「高清 1:1」（画布按格子真实像素烘）或关「画布无关帧」（画布按格子比例）。</p>
     *
     * <p>≥1.0 = 画布被放大（越大越糊）；&lt;1.0 = 被缩小（更锐，但会裁掉边缘）。非法输入返回 0。</p>
     */
    static float displayScale(int canvasW, int canvasH, int boxW, int boxH) {
        if (canvasW <= 0 || canvasH <= 0 || boxW <= 0 || boxH <= 0) {
            return 0.0f;
        }
        return Math.max(boxW / (float) canvasW, boxH / (float) canvasH);
    }

    /** 桌面上被放大的门槛：>1.05（多 5% 就看得出来软）。 */
    static final float DISPLAY_BLUR = 1.05f;
}
