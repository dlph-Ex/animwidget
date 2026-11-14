package io.github.animwidget;

/**
 * v1.10.272：**性能项「副标题收起 / 展开」的动效参数**（曲线、时长、错峰全在这一个文件里）。
 *
 * <h3>为什么单独一个类</h3>
 * 用户要求「时长、错峰延迟、曲线参数集中放一处，方便微调」。这个类就是那"一处"：
 * 改手感只动这里的四个常量 + 两条曲线，{@code SettingsActivity} 那侧只按
 * 「进度 p → 每个元素各自的值」计算，不再有任何魔法数字。
 *
 * <h3>动效结构（收起方向；展开 = 整段逆向回放）</h3>
 * <ol>
 *   <li><b>副标题</b>：朝主标题方向位移 + 缩放 + 淡出，最终"收进主标题内部"
 *       —— 窗口 <b>0 ~ 62%</b>（285ms），曲线 {@link #CURVE_HINT}；</li>
 *   <li><b>整行高度</b>：主标题 + 右侧勾选框/输入框一起收拢到单行
 *       —— 窗口 <b>34% ~ 100%</b>（304ms），曲线 {@link #CURVE_ROW}；</li>
 *   <li>两段**重叠 28%**（副标题还在往里收的时候，行高已经开始降），
 *       不是"先做完 A 再做 B"；</li>
 *   <li>多个条目**逐行错峰** {@link #STAGGER}（4.5% ≈ 21ms/行），行与行之间也有重叠。</li>
 * </ol>
 *
 * <h3>为什么"整段时间轴"是线性的（不是系统默认缓动）</h3>
 * 用户要求"禁用 linear / ease / ease-in-out 等系统默认缓动"，指的是**看得见的运动**。
 * 这里 {@code ValueAnimator} 只当**时间轴**用（输出 0~1 的原始时间进度 p），
 * 所有缓动由每个元素自己的三次贝塞尔曲线完成 —— 这正是 AE 合成里"线性时间轴 +
 * 每个属性各挂一条曲线"的做法。用线性时间轴还有一个必须的理由：
 * **动画途中反向时 p 从当前值原路退回，各元素的曲线自动反着走，一帧都不会跳**。
 * 如果连时间轴都再套一层 easing，反向就会在"时间被重映射"的地方突变。
 *
 * <h3>曲线是怎么选的（检索 + 复算，不是拍脑袋）</h3>
 * 候选曲线来自三家权威源码（全部用 curl 抓的原文件，参数逐条抄下来）：
 * <ul>
 *   <li>AndroidX Compose {@code EasingFunctions.kt} —— 27 条命名曲线（EaseOutExpo / Quint …）；</li>
 *   <li>Material 3 {@code MotionTokens.kt} —— emphasized 家族 + 时长表（250/300/350/400…）；</li>
 *   <li>Flutter {@code curves.dart} —— 21 条 Cubic，含 easeOutBack / easeOutExpo。</li>
 * </ul>
 * 论文级候选（20+ 组、参数、来源、手感）见 docs/FOLD-CURVES.md。
 *
 * <p><b>主选</b>：{@link #CURVE_HINT} = cubic-bezier(0.20, 0.95, 0.30, 1.10)
 * —— 在 Flutter easeOutBack (0.175,0.885,0.32,1.275) 的基础上把过冲从 27.5% 收到
 * **2.66%**（复算：峰值出现在 69% 时间点，90% 位移在 32% 时间点完成）。手机小面积折叠
 * 的位移只有 30~60dp，需要"起手快、尾部拖长"，但过冲绝对不能大 —— 2~3% 刚好是
 * "看得出有生命、看不出在弹"的那个量；<b>次选</b> {@link #CURVE_HINT_ALT} =
 * EaseOutQuint (0.22,1,0.36,1)，无过冲、更稳，觉得"抖"就换它。</p>
 *
 * <p><b>行高</b>用 M3 {@code EaseEmphasized} (0.2, 0, 0, 1)：速度峰在 11.8%、
 * 90% 位移在 54% —— 起步柔、尾部长，让"高度变矮"这件事被读成"结构在收拢"而不是"被剪掉"；
 * 次选 {@link #CURVE_ROW_ALT} = M3 decelerate (0.05,0.7,0.1,1)（起手更急，适合条目少的卡）。</p>
 */
final class FoldMotion {

    private FoldMotion() {
    }

    // ------------------------------------------------------------------ 时间
    /** 整段时长（ms）。收 / 展共用；反向时按剩余比例缩短，速度保持一致。 */
    static final long DURATION_MS = 460L;

    /** 每行错峰占 p 的比例（×460ms ≈ 21ms/行）。6 行以内总跨度 ≤ 105ms。 */
    static final float STAGGER = 0.045f;

    /** 反向时的最短时长：再短会显得"直接跳到位"，没有收的过程。 */
    static final long MIN_DURATION_MS = 120L;

    /**
     * v1.10.273：**慢放倍率（只给验收用）**，出厂 1.0。
     *
     * 用户的验收方法是"把时长调到 3~5 倍慢放，主标题位置应随进度平滑变化，
     * 结束帧与结束后一帧完全一致"。把这里改成 4.0f 重新编译一次即可复现；
     * 改回去就是正常速度。**除这一处，没有任何地方能让时长变形。**
     */
    static final float DEBUG_SLOW = 1.0f;

    // ------------------------------------------------------------------ 窗口
    /** 副标题窗口：0% → 62%（≈285ms）。 */
    static final float HINT_FROM = 0.00f;
    static final float HINT_TO = 0.62f;

    /** 行高窗口：34% → 100%（≈304ms，与副标题重叠 28%）。 */
    static final float ROW_FROM = 0.34f;
    static final float ROW_TO = 1.00f;

    // ------------------------------------------------------------------ 曲线
    /**
     * 副标题（主选）：cubic-bezier(0.20, 0.95, 0.30, 1.10)。
     * 复算：90% 位移在 32% 时间点、过冲峰值 +2.66% 出现在 69% 时间点、之后长尾落回。
     */
    static final Ease CURVE_HINT = new Ease(0.20f, 0.95f, 0.30f, 1.10f);

    /** 副标题（次选）：Compose / Flutter {@code EaseOutQuint}——无过冲、更克制。 */
    static final Ease CURVE_HINT_ALT = new Ease(0.22f, 1.00f, 0.36f, 1.00f);

    /** 行高（主选）：M3 {@code EaseEmphasized}——起步柔、尾部长。 */
    static final Ease CURVE_ROW = new Ease(0.20f, 0.00f, 0.00f, 1.00f);

    /** 行高（次选）：M3 {@code emphasizedDecelerate}——起手更急。 */
    static final Ease CURVE_ROW_ALT = new Ease(0.05f, 0.70f, 0.10f, 1.00f);

    /**
     * 一键切到次选曲线（false = 主选）。留这个开关是为了"觉得弹就换稳的"，
     * 不用满地找参数 —— 见 docs/FOLD-CURVES.md 的对比表。
     */
    static final boolean USE_ALT_CURVES = false;

    static Ease hintCurve() {
        return USE_ALT_CURVES ? CURVE_HINT_ALT : CURVE_HINT;
    }

    static Ease rowCurve() {
        return USE_ALT_CURVES ? CURVE_ROW_ALT : CURVE_ROW;
    }

    // ------------------------------------------------------------------ 形状
    /** 副标题收进主标题时的缩放（X 只收一点，Y 收得多 → 读作"被吸进标题里"）。 */
    static final float HINT_SCALE_X = 0.16f;
    static final float HINT_SCALE_Y = 0.34f;

    /** 副标题淡出比位移**提前**收尾（h × 1.15 就到 0），避免"已经到位还在半透明"。 */
    static final float HINT_FADE_GAIN = 1.15f;

    /** 区段归一化：p 落到 [from, to] 之外的夹成 0/1。 */
    static float window(float p, float from, float to) {
        if (to <= from) {
            return p >= to ? 1.0f : 0.0f;
        }
        float v = (p - from) / (to - from);
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }

    /** 第 i 行自己的进度（错峰 + 压缩窗口，保证最后一行正好在 p=1 收尾）。 */
    static float rowProgress(float p, int index, int count) {
        float stagger = STAGGER;
        float span = 1.0f - stagger * Math.max(count - 1, 0);
        if (span < 0.25f) {
            // 条目特别多时别把窗口压没了：错峰自动缩小
            span = 0.25f;
            stagger = (1.0f - span) / Math.max(count - 1, 1);
        }
        float v = (p - index * stagger) / span;
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }

    // ================================================================== 几何（纯函数）
    //
    // v1.10.274：**主标题的位置不再由我们算，而是交给布局**。
    //
    // 上一版（v1.10.273）的教训：为了"主标题从第一帧起就动"，我把文字块 lp.height 钉成
    // 自然高度、再手动加一个位移。只要量到的 titleH / controlH / blockFullH 有一点偏差
    // 或过期（换字号、换肤重建、量的时候宽度还没定），块就会比行高，
    // 被父布局从上下一裁 —— 用户截图里"标题被削掉半行、某几行只剩右边的数值/勾选框"就是这个。
    //
    // 现在只有一个会变的量：**副标题的占位高度**。
    //   · 行保持 WRAP_CONTENT —— 高度永远是内容撑出来的，**不可能裁到内容**；
    //   · 副标题的 lp.height 从它的自然高度收到 0（按行高曲线）；
    //   · 主标题、勾选框、数值视图的纵向位置由 `gravity=center_vertical` 每帧重算 ——
    //     这就是"连续性"的来源：它的位置是副标题占位高度的连续函数；
    //   · 收完时块高 = 标题高，行内框 = max(标题高, 控件高)，主标题正好在中央，
    //     与静息布局**同一个算式**，所以不存在"事后补位"。
    // 代价：块高 ≥ 控件高 的那段时间（约前 20%），主标题本来就不该动（它已经在行内框顶部），
    // 之后才是连续下移到中央 —— 这与"条目变矮、标题随之居中"的物理直觉一致。

    /** 副标题在某个进度下的占位高度（rowEase=0 是原高，=1 是 0）。 */
    static float slotHeight(float slot0, float rowEase) {
        float v = slot0 * (1.0f - rowEase);
        return v < 0.0f ? 0.0f : v;
    }

    /**
     * 文字块的**自然**高度 = 标题高 + 副标题占位。
     *
     * 注意 **没有单独的"间距"项**：副标题的上内边距（2dp）在 `hint.getHeight()` 里面，
     * 占位收到 0 时它一起没了（`lp.height = 0` 时 padding 不再计入）。
     * v1.10.274 的自检一开始把间距当成独立的一项，于是多行副标题的末帧差了 3.5px
     * —— 门禁当场报红，正好说明"模型必须和布局同一个算式"。
     */
    static float blockHeight(float titleH, float slot) {
        return titleH + slot;
    }

    /** 行内框高度 = max(文字块自然高, 右侧控件高)。 */
    static float innerHeight(float blockH, float controlH) {
        return Math.max(blockH, controlH);
    }

    /** 行高（含上下 padding）。 */
    static float rowHeight(float padV, float innerH) {
        return padV * 2.0f + innerH;
    }

    /**
     * 主标题在行内的可视位置（相对行顶）= 自检判据。
     *
     * 完全按布局的规则写：行内框里居中 → `padV + (innerH - blockH)/2`。
     * v1.10.273 那种"钉住块高 + 手动位移"在这一步就多了一个自由变量，
     * 只要那个变量和布局不一致就会裁内容；这里没有自由变量。
     */
    static float titleTop(float padV, float titleH, float slot, float controlH) {
        float blockH = blockHeight(titleH, slot);
        return padV + (innerHeight(blockH, controlH) - blockH) * 0.5f;
    }

    /** 展开/收起的目标进度 → 这次动画该跑多久（按剩余比例，保持"同一速度"）。 */
    static long durationFor(float from, float to) {
        float d = Math.abs(to - from);
        long ms = Math.round(DURATION_MS * d * DEBUG_SLOW);
        return Math.max(MIN_DURATION_MS, ms);
    }
}
