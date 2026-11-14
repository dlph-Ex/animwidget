package io.github.animwidget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * 历史导出（v1.10.38）。
 *
 * 列出所有导出过的日志文件：
 *  • 已删除的文件不显示（读取时按存在性过滤）
 *  • 不可撤回（这里没有撤销概念，只有删除）
 *  • 每条右侧有「删除」，删掉文件 + 记录
 *  • 点击一条 → 优先跳到系统文件管理对应的目录，打不开再尝试直接打开文件
 */
public final class ExportHistoryActivity extends I18nActivity {

    /**
     * v1.10.89：**并发删除**的参数（短时间连删多条，动画互不打断）。
     *
     * 关键设计：
     *  • 平移不再是「一条动画从 0 演到 -gap」。v1.10.87/88 那样做，第二条删除要么被
     *    `animating` 挡掉（你连点第二下没反应），要么得把在跑的动画重启（掉速 → 「断」）。
     *    现在**每行一个临界阻尼弹簧**，删一条只是把受影响行的 target 往前挪一格：
     *    正在跑的行速度不丢、轨迹连续，新删除的行接着往新目标收敛。
     *  • 缩放（按下 → 保持 → 过冲回弹）仍是一轮 300ms 的贝塞尔关键帧，
     *    只在「这一行第一次需要动」时按一下；正在按的行不重复按（连点不乱抖）。
     *  • 真正落库（删记录 + 重建列表）推迟到**所有黑洞播完、弹簧收敛**之后一次做完，
     *    所以动画期间列表绝不会被 removeAllViews 打断。
     */
    /** 补位延迟：等黑洞把卡片盖住，下方条目才开始往上顶。 */
    private static final long SHIFT_DELAY = 300L;
    /** 错开：越靠下的行晚一点点起步（最多 54ms），保留「多米诺」感。 */
    private static final long SHIFT_STAGGER = 18L;
    private static final long SHIFT_STAGGER_MAX = 54L;
    /** 按下缩放：幅度 / 时长 / 关键帧位置（与 {@link Ease} 配合）。 */
    private static final float PRESS_SHRINK = 0.045f;
    private static final long PRESS_DURATION = 300L;
    private static final float K_SHRINK_END = 0.30f;
    private static final float K_REGROW_START = 0.56f;
    /**
     * 平移弹簧：临界阻尼（DAMP ≈ 2·√STIFF）。
     *
     * ω = √400 = 20 rad/s → 90% 位移约 212ms，与 v1.10.88 那条贝塞尔（216ms）
     * 几乎一致，所以观感不变；但「终点 = target 变量」，改目标不会打断正在跑的运动。
     */
    private static final float SPRING_STIFF = 400.0f;
    private static final float SPRING_DAMP = 40.0f;
    /**
     * 收尾吸附阈值：剩不到 1.2px 且速度很小时直接落位。
     *
     * 临界阻尼弹簧的尾巴是指数衰减，光靠「等它自己停」会拖到 700ms 以外（肉眼早就看不出差别，
     * 但列表重建会被一直推迟）。1.2px 在一条 190px 高的卡片上完全看不出来，直接吸附。
     */
    private static final float SNAP_EPS = 1.8f;
    private static final float SNAP_VEPS = 45.0f;
    /** 收尾重试上限（等弹簧收敛，最多约 0.6s）。 */
    private static final int FLUSH_MAX_RETRY = 20;

    /** 一行的运动状态（弹簧 + 按一下）。 */
    private static final class RowMotion {
        final View slot;
        float x;
        float v;
        float target;
        /** 本波次里的起步延迟（ns），用于错开。 */
        long delayNanos;
        boolean pressed;
        /** 排到「补位波次启动」时再按一下（否则会先缩一圈、干等 300ms 才开始移动）。 */
        boolean wantPress;
        @Nullable
        ValueAnimator press;

        RowMotion(View slot) {
            this.slot = slot;
        }
    }

    @Nullable
    private LinearLayout list;
    @Nullable
    private TextView empty;
    /** 每条一层的「行容器」（FrameLayout）—— 黑洞就挂在这里面，靠容器自身坐标，永远不偏。 */
    private final List<View> slots = new ArrayList<>();
    /** 与 slots 平行：每行的弹簧/按下状态。 */
    private final List<RowMotion> motion = new ArrayList<>();
    /** slot → 待删除的记录。动画全部收尾后一次性落库（v1.10.89 并发删除的核心）。 */
    private final LinkedHashMap<View, ExportHistory.Entry> pending = new LinkedHashMap<>();
    /** 正在播的黑洞个数（都播完才允许落库重建）。 */
    private int holes;
    /** 补位延迟已经排上了（避免连点时排多次）。 */
    private boolean motionScheduled;
    /** 弹簧循环是否在跑。 */
    private boolean stepping;
    private long lastFrameNanos;
    /** 补位波次起点（算每行起步延迟用）。 */
    private long waveStartNanos;
    private int flushRetries;
    private int animSeq;

    /** 弹簧驱动：只在有行没到位时挂在 Choreographer 上（不做无谓的每帧回调）。 */
    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!ExportHistoryActivity.this.stepping) {
                return;
            }
            stepMotion(frameTimeNanos);
            if (ExportHistoryActivity.this.stepping) {
                Choreographer.getInstance().postFrameCallback(this);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashLog.install(this);
        setContentView(R.layout.activity_export_history);
        // v1.10.156：导出历史页不参与「字体大小」缩放
        Fonts.markNoScale(findViewById(android.R.id.content));
        setI18nLayout(R.layout.activity_export_history);
        EdgeToEdge.INSTANCE.apply(this);
        this.list = (LinearLayout) findViewById(R.id.ll_export_history);
        this.empty = (TextView) findViewById(R.id.tv_export_history_empty);
        Button back = (Button) findViewById(R.id.btn_export_history_back);
        if (back != null) {
            back.setOnClickListener((v) -> finish());
        }
        applyTheme();
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncLocale();
        applyTheme();
        render();
    }

    @Override
    public void onConfigurationChanged(@NotNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // v1.10.40：原地换肤（recreate 会让页面被顶掉）
        applyTheme();
        render();
    }

    private ThemeColors colors() {
        try {
            return ThemeManager.INSTANCE.colors(this);
        } catch (Throwable th) {
            return null;
        }
    }

    private void applyTheme() {
        try {
            View content = findViewById(android.R.id.content);
            ThemeManager.INSTANCE.applyTo(content);
            ThemeManager.INSTANCE.applySystemBars(this);
            ThemeColors c = colors();
            if (c != null) {
                content.setBackgroundColor(c.getBackground());
                TextView title = (TextView) findViewById(R.id.tv_export_history_title);
                if (title != null) {
                    title.setTextColor(c.getText());
                }
                if (this.empty != null) {
                    this.empty.setTextColor(c.getTextMuted());
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void render() {
        LinearLayout container = this.list;
        // v1.10.89：只要还有待删条目在播动画，就不要重建列表（会把动画整个打断）
        if (container == null || !this.pending.isEmpty()) {
            return;
        }
        container.removeAllViews();
        this.slots.clear();
        this.motion.clear();
        this.holes = 0;
        this.flushRetries = 0;
        List<ExportHistory.Entry> items = ExportHistory.list(this);
        if (this.empty != null) {
            this.empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (items.isEmpty()) {
            return;
        }
        ThemeColors c = colors();
        int textColor = c != null ? c.getText() : 0xFFE8E8E8;
        int mutedColor = c != null ? c.getTextMuted() : 0xFF9AA0A6;
        int cardColor = c != null ? c.getSurface() : 0xFF1C1C22;
        int primary = c != null ? c.getPrimary() : 0xFF6750A4;
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < items.size(); i++) {
            final ExportHistory.Entry entry = items.get(i);
            final int rowIndex = i;
            // v1.10.87：**每条套一层 FrameLayout 行容器**（尺寸与卡片一致，只负责装间距）。
            // 黑洞 addView 进这一层 → 它的坐标系就是 (0,0)-(w,h)，不需要任何屏幕坐标换算。
            final FrameLayout slot = new FrameLayout(this);
            LinearLayout.LayoutParams slotLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            slotLp.bottomMargin = (int) (8 * density);
            slot.setLayoutParams(slotLp);
            slot.setClipChildren(true);

            final LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(cardColor);
            bg.setCornerRadius(14 * density);
            card.setBackground(bg);
            card.setPadding((int) (14 * density), (int) (12 * density),
                    (int) (10 * density), (int) (12 * density));
            card.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

            LinearLayout column = new LinearLayout(this);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            TextView nameView = new TextView(this);
            nameView.setText(entry.name);
            nameView.setTextSize(13.5f);
            nameView.setTextColor(textColor);
            nameView.setTypeface(Typeface.MONOSPACE);
            nameView.setSingleLine(true);
            column.addView(nameView);
            TextView metaView = new TextView(this);
            metaView.setText(buildMeta(entry));
            metaView.setTextSize(11.0f);
            metaView.setTextColor(mutedColor);
            column.addView(metaView);
            card.addView(column);

            Button delete = new Button(this);
            delete.setText(R.string.export_history_delete);
            delete.setTextSize(12.0f);
            delete.setAllCaps(false);
            delete.setTextColor(primary);
            GradientDrawable pill = new GradientDrawable();
            pill.setColor(0x00000000);
            pill.setCornerRadius(16 * density);
            delete.setBackground(pill);
            delete.setPadding((int) (10 * density), 0, (int) (10 * density), 0);
            delete.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, (int) (38 * density)));
            delete.setOnClickListener((v) -> deleteWithAnimation(slot, card, entry, rowIndex));
            card.addView(delete);

            card.setOnClickListener((v) -> openEntry(entry));
            // v1.10.42：长按 = 详情（复制路径 / 拉起 MT / 删除）
            card.setOnLongClickListener((v) -> {
                showPathDialog(entry);
                return true;
            });
            slot.addView(card);
            container.addView(slot);
            this.slots.add(slot);
            this.motion.add(new RowMotion(slot));
        }
    }

    /**
     * v1.10.89：删一条 —— **渐出黑洞 + 吞噬条目 + 下方条目补位**，且**支持连删**。
     *
     * 单条的时间线（{@link HoleView}）不变：点淡入 110 → 长大 230（340 盖满 → 卡片隐身）
     * → 停 60 → 自隐 240，共 640ms。
     *
     * 并发时发生的事：
     *  • 再点第二条 → 只是往 {@code pending} 里加一条，重算所有行的目标位移（累积一格），
     *    正在跑的弹簧不会被取消、不重置速度 → 前一条的动画继续跑完，不断；
     *  • 已经隐藏的卡片不会再被点（透明但能吃事件）→ {@code pending.containsKey(slot)} 直接忽略；
     *  • 所有黑洞播完且弹簧收敛后，才一次性删记录 + 重建列表（见 {@link #tryFlush}）。
     *
     * 坐标：黑洞 addView 到**这一条自己的行容器**（FrameLayout，尺寸=卡片），
     * 于是它的坐标系永远是 (0,0)-(w,h)。v1.10.84 挂 LinearLayout 量出 0 高、
     * v1.10.85/86 挂根布局算屏幕坐标偏半个行高 —— 这里彻底不算坐标。
     */
    private void deleteWithAnimation(final FrameLayout slot, final View card,
                                     final ExportHistory.Entry entry, final int index) {
        final LinearLayout container = this.list;
        if (container == null || this.pending.containsKey(slot)) {
            return;   // 这一条已经在删了，别重复播
        }
        final int seq = ++this.animSeq;
        try {
            float density = getResources().getDisplayMetrics().density;
            ThemeColors c = colors();
            int cardColor = cardColorOf(card, c != null ? c.getSurface() : 0xFF1C1C22);
            int neutral = c != null ? c.getNeutral() : cardColor;
            int holeColor = HoleView.holeColorFor(cardColor, neutral);
            int rowHeight = slot.getHeight();
            int rowGap = bottomMarginOf(slot);
            this.pending.put(slot, entry);
            // W24：删除自检（三色 + 对比度 + 行高 + 本次连删条数）
            Diag.warn(this, "W24", "删除动画 卡片=" + hex(cardColor) + " 中性=" + hex(neutral)
                    + " 黑洞=" + hex(holeColor)
                    + " 对比度=" + String.format(Locale.US, "%.2f", HoleView.contrast(holeColor, cardColor))
                    + " 行高=" + rowHeight + " 间距=" + rowGap
                    + " 连删=" + this.pending.size());
            if (rowHeight <= 0) {
                // 还没量出来（极端情况）：不演了，只把这一条落库，别的 pending 不动
                this.pending.remove(slot);
                try {
                    ExportHistory.remove(this, entry);
                } catch (Throwable ignored) {
                }
                if (this.pending.isEmpty()) {
                    render();
                }
                return;
            }
            // 多条同时删时，越晚点的那条盖在上层（Z 只影响绘制顺序，不带投影）
            slot.setZ(1.0f + this.pending.size() * 0.1f);
            final HoleView hole = new HoleView(this, holeColor);
            hole.setCornerRadius(14 * density);
            // 高度给**明确像素**：FrameLayout 只有在存在 2 个以上 MATCH_PARENT 子 View 时才会二次测量，
            // 而 MATCH_PARENT 高度在没有内容的自绘 View 上会量成 0（v1.10.84 就是栽在这）。
            slot.addView(hole, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, rowHeight));
            this.holes++;
            retargetAll(index);
            scheduleMotion();
            hole.play(new Runnable() {
                @Override
                public void run() {
                    // 黑洞盖满的瞬间：卡片立刻隐身，接下来渐隐露出的是「空位」而不是卡片。
                    // 用 INVISIBLE 而不是 alpha=0 —— 前者还保留布局高度，而且**彻底不接触摸**，
                    // 否则透明卡片还会响应删除/打开（alpha=0 的 View 照样吃事件）。
                    try {
                        card.setAlpha(0.0f);
                        card.setVisibility(View.INVISIBLE);
                    } catch (Throwable ignored) {
                    }
                }
            }, new Runnable() {
                @Override
                public void run() {
                    ExportHistoryActivity.this.onHoleFinished();
                }
            });
            // 兜底：万一动画回调被系统吞掉（个别 ROM 在过渡动画里会丢回调），3s 后强制收尾
            container.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (seq == ExportHistoryActivity.this.animSeq
                            && !ExportHistoryActivity.this.pending.isEmpty()) {
                        Diag.warn(ExportHistoryActivity.this, "W24", "删除动画超时收尾（回调丢失）");
                        ExportHistoryActivity.this.holes = 0;
                        ExportHistoryActivity.this.flushNow();
                    }
                }
            }, 3000L);
        } catch (Throwable th) {
            Diag.warn(this, "W24", "删除动画异常： " + th);
            this.pending.remove(slot);
            try {
                ExportHistory.remove(this, entry);
            } catch (Throwable ignored) {
            }
            if (this.pending.isEmpty()) {
                render();
            }
        }
    }

    /** 一个黑洞播完了；全部播完才开始尝试收尾。 */
    private void onHoleFinished() {
        if (this.holes > 0) {
            this.holes--;
        }
        tryFlush();
    }

    /**
     * 准备落库：还要等弹簧收敛（补位动画可能比黑洞晚一点点结束）。
     * 收敛就一次性删记录 + 重建列表，列表重建那一帧所有行都正好在最终位置，不会跳。
     */
    private void tryFlush() {
        if (this.pending.isEmpty()) {
            return;
        }
        if (this.holes > 0) {
            return;
        }
        if (anyUnsettled() && this.flushRetries < FLUSH_MAX_RETRY) {
            this.flushRetries++;
            LinearLayout container = this.list;
            if (container != null) {
                container.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ExportHistoryActivity.this.tryFlush();
                    }
                }, 32L);
                return;
            }
        }
        flushNow();
    }

    /** 所有补位都到位了吗（位移 + 速度都要小）。 */
    private boolean anyUnsettled() {
        for (int i = 0; i < this.motion.size(); i++) {
            RowMotion m = this.motion.get(i);
            if (Math.abs(m.target - m.x) > 1.5f || Math.abs(m.v) > 50.0f) {
                return true;
            }
        }
        return false;
    }

    /** 真正删除 + 重建列表（一次性，动画期间绝不调用）。 */
    private void flushNow() {
        this.flushRetries = 0;
        this.holes = 0;
        removeEntriesAndRender();
    }

    /** 把 pending 里的记录**一次性**删掉并重建列表（快照 → 清空 → 落库 → 重建）。 */
    private void removeEntriesAndRender() {
        List<ExportHistory.Entry> entries = new ArrayList<>(this.pending.values());
        this.pending.clear();
        if (entries.isEmpty()) {
            render();
            return;
        }
        try {
            for (int i = 0; i < entries.size(); i++) {
                ExportHistory.remove(this, entries.get(i));
            }
            toast(getString(R.string.export_history_deleted));
        } catch (Throwable ignored) {
        }
        render();
    }

    // ───────────────────────── 补位：弹簧 + 按一下 ─────────────────────────

    /**
     * 重算每一行的目标位移。
     *
     * 「本行上方有多少条待删」= 这一行要往上挪几格 —— 因为连删可以任意顺序，
     * 所以每次有新的删除就把全表重算一遍（O(n)，几十条无所谓），
     * 目标变了的行：按一下（缩放关键帧），并交给弹簧去追。
     */
    private void retargetAll(int fromIndex) {
        float shift = 0.0f;
        int n = Math.min(this.slots.size(), this.motion.size());
        for (int i = 0; i < n; i++) {
            RowMotion m = this.motion.get(i);
            View slot = this.slots.get(i);
            float target = -shift;
            boolean moved = Math.abs(target - m.target) > 0.01f;
            // 注意：idle 必须在改 target **之前**判断，否则「原地待命」的行永远算不 idle，
            // 错开就全变成 0 了。
            boolean idle = Math.abs(m.v) < 1.0f && Math.abs(m.x - m.target) < 0.5f;
            m.target = target;
            if (this.pending.containsKey(slot)) {
                // 这一行自己要被删：它对下面的行贡献一格位移（卡片已经隐身，但布局高度还在）
                shift += slot.getHeight() + bottomMarginOf(slot);
                continue;
            }
            if (!moved) {
                continue;
            }
            // 错开：越靠下的行晚一点起步（只对本波次里「还没动起来」的行生效）
            long stagger = Math.min(SHIFT_STAGGER * Math.max(0, i - fromIndex), SHIFT_STAGGER_MAX);
            m.delayNanos = idle ? stagger * 1000000L : 0L;
            if (this.stepping) {
                // 已经在补位中（连删）：立刻按，跟正在跑的行节奏一致
                pressRow(m);
            } else {
                // 还没起步（第一波）：攒到波次启动那一刻再按，避免「先缩一圈干等 300ms」
                m.wantPress = true;
            }
        }
    }

    /** 排上补位延迟（只在第一次删除时排；连删时弹簧已经在跑，新目标立即生效）。 */
    private void scheduleMotion() {
        if (this.stepping || this.motionScheduled) {
            return;
        }
        LinearLayout container = this.list;
        if (container == null) {
            return;
        }
        this.motionScheduled = true;
        container.postDelayed(new Runnable() {
            @Override
            public void run() {
                ExportHistoryActivity.this.motionScheduled = false;
                ExportHistoryActivity.this.startStepping();
            }
        }, SHIFT_DELAY);
    }

    private void startStepping() {
        if (this.stepping) {
            return;
        }
        this.stepping = true;
        this.lastFrameNanos = 0L;
        this.waveStartNanos = 0L;
        // 波次启动这一帧，把「要动」的行一起按下去（缩放和位移同步开始，才是「缩着移动」）
        for (int i = 0; i < this.motion.size(); i++) {
            RowMotion m = this.motion.get(i);
            if (m.wantPress) {
                m.wantPress = false;
                pressRow(m);
            }
        }
        Choreographer.getInstance().postFrameCallback(this.frameCallback);
    }

    /**
     * 一帧的弹簧积分（半隐式欧拉，dt 夹到 32ms —— 掉帧时也不会把弹簧炸开）。
     *
     * 因为是「改目标」而不是「重启动画」，所以在飞行的行即使被第二条删除改目标，
     * 位置和速度都是连续接过去的。
     */
    private void stepMotion(long nowNanos) {
        float dt;
        if (this.lastFrameNanos == 0L) {
            this.lastFrameNanos = nowNanos;
            this.waveStartNanos = nowNanos;
            dt = 0.016f;
        } else {
            dt = (nowNanos - this.lastFrameNanos) / 1.0e9f;
            this.lastFrameNanos = nowNanos;
            if (dt <= 0.0f) {
                dt = 0.016f;
            } else if (dt > 0.032f) {
                dt = 0.032f;
            }
        }
        long elapsed = nowNanos - this.waveStartNanos;
        boolean active = false;
        for (int i = 0; i < this.motion.size(); i++) {
            RowMotion m = this.motion.get(i);
            if (elapsed < m.delayNanos) {
                active = true;   // 还在错开等待里
                continue;
            }
            float dx = m.target - m.x;
            if (Math.abs(dx) < SNAP_EPS && Math.abs(m.v) < SNAP_VEPS) {
                if (m.x != m.target) {
                    m.x = m.target;
                    m.slot.setTranslationY(m.x);
                }
                m.v = 0.0f;
                continue;
            }
            float acc = SPRING_STIFF * dx - SPRING_DAMP * m.v;
            m.v += acc * dt;
            m.x += m.v * dt;
            m.slot.setTranslationY(m.x);
            active = true;
        }
        if (!active) {
            this.stepping = false;
            this.waveStartNanos = 0L;
        }
    }

    /**
     * 「按一下再回弹」：1.0 → 0.955 →（过冲 1.014）→ 1.0，三段贝塞尔关键帧。
     *
     * 起点取**当前实际缩放**而不是写死 1.0，所以连点时不会先跳回原大小；
     * 正在按的行不重复按（等它按完），避免两条删除撞在一起时乱抖。
     */
    private void pressRow(final RowMotion m) {
        if (m.pressed) {
            return;
        }
        final View v = m.slot;
        final float s0 = v.getScaleX();
        final float lo = Math.min(s0, 1.0f - PRESS_SHRINK);
        m.pressed = true;
        ValueAnimator anim = ValueAnimator.ofFloat(0.0f, 1.0f);
        anim.setDuration(PRESS_DURATION);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float p = ((Float) animation.getAnimatedValue()).floatValue();
                float s;
                if (p <= K_SHRINK_END) {
                    s = s0 + (lo - s0) * Ease.IN_OUT.at(p / K_SHRINK_END);
                } else if (p <= K_REGROW_START) {
                    s = lo;
                } else {
                    float k = Math.min(1.0f, (p - K_REGROW_START) / (1.0f - K_REGROW_START));
                    s = lo + (1.0f - lo) * Ease.OVERSHOOT.at(k);
                }
                v.setScaleX(s);
                v.setScaleY(s);
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                try {
                    v.setScaleX(1.0f);
                    v.setScaleY(1.0f);
                } catch (Throwable ignored) {
                }
                m.pressed = false;
                m.press = null;
            }
        });
        m.press = anim;
        anim.start();
    }

    @Override
    protected void onDestroy() {
        // 页面关了：停掉弹簧循环，并且把「正在动画删除」的记录落库，
        // 否则用户删了却还在列表里（下次进来又冒出来）
        this.stepping = false;
        for (int i = 0; i < this.motion.size(); i++) {
            RowMotion m = this.motion.get(i);
            if (m.press != null) {
                m.press.cancel();
                m.press = null;
            }
        }
        flushWithoutUi();
        super.onDestroy();
    }

    /** 只落库、不重建 UI（退出页面用；任何异常都吞掉，不能影响销毁流程）。 */
    private void flushWithoutUi() {
        if (this.pending.isEmpty()) {
            return;
        }
        Object[] values = this.pending.values().toArray();
        this.pending.clear();
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (!(value instanceof ExportHistory.Entry)) {
                continue;
            }
            try {
                ExportHistory.remove(this, (ExportHistory.Entry) value);
            } catch (Throwable ignored) {
                // 退出路径：删不掉就算了，绝不影响销毁
            }
        }
    }

    /** 条目**实际**背景色（用户自定义配色时 theme.surface 之外还可能有别的来源）。 */
    private int cardColorOf(View card, int fallback) {
        try {
            android.graphics.drawable.Drawable bg = card.getBackground();
            if (bg instanceof GradientDrawable) {
                android.content.res.ColorStateList csl = ((GradientDrawable) bg).getColor();
                if (csl != null) {
                    return csl.getDefaultColor();
                }
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private int bottomMarginOf(View v) {
        try {
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                return ((ViewGroup.MarginLayoutParams) lp).bottomMargin;
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static String hex(int color) {
        return String.format(Locale.US, "#%08X", color);
    }

    private String buildMeta(ExportHistory.Entry entry) {
        String time;
        try {
            time = DateFormat.getMediumDateFormat(this).format(new Date(entry.time))
                    + " " + DateFormat.getTimeFormat(this).format(new Date(entry.time));
        } catch (Throwable th) {
            time = "";
        }
        String size = entry.bytes < 1024L
                ? entry.bytes + " B"
                : String.format(Locale.US, "%.1f KB", entry.bytes / 1024.0);
        String kind = entry.images ? "长图" : "TXT";
        return time + " · " + size + " · " + kind;
    }

    /**
     * v1.10.38：点击 → 先尝试跳到文件管理里的对应目录（DocumentsUI 的目录 URI），
     * 失败再直接打开文件。两条路都失败就提示。
     */
    /**
     * v1.10.41：点击条目 = 直接跳到文件管理的对应目录（和 MT 管理器的「在文件管理中显示」同一个 Intent）。
     *
     * 系统面板之所以会列出网盘/音乐/WPS，是因为它们都声明了 `*\/*` 的 VIEW；
     * 但只要我们自己用 PackageManager 把「文件管理器」这个目标挑出来、用 setComponent 直启，
     * 就不会弹出那一屏乱七八糟的应用了。挑不到才退回系统面板。
     */
    private void openEntry(ExportHistory.Entry entry) {
        // v1.10.43：改回第一版 —— 点击就直接打开这个文件（交给系统选择打开方式）。
        // 上一版跳「目录 URI」，落进 vivo 文件管理的一个只显示文件夹的分类视图，更没法用。
        // 需要「跳目录 / 在 MT 中打开」的话长按条目即可。
        if (!openFile(entry)) {
            toast(getString(R.string.export_history_open_fail));
        }
    }

    /** 定位并直接启动文件管理器（不弹选择器）；定位失败才退回系统面板。 */
    private boolean openFolderDirect(ExportHistory.Entry entry) {
        try {
            String dir = entry.path != null && entry.path.contains("/Pictures") ? "Pictures" : "Download";
            Uri folder = android.provider.DocumentsContract.buildDocumentUri(
                    "com.android.externalstorage.documents", "primary:" + dir);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(folder, "vnd.android.document/directory");

            android.content.pm.PackageManager pm = getPackageManager();
            List<android.content.pm.ResolveInfo> targets = pm.queryIntentActivities(intent, 0);
            android.content.pm.ResolveInfo picked = null;
            android.content.pm.ResolveInfo onlyOne = null;
            int count = 0;
            for (android.content.pm.ResolveInfo info : targets) {
                if (info.activityInfo == null) {
                    continue;
                }
                count++;
                onlyOne = info;
                String pkg = info.activityInfo.packageName.toLowerCase(Locale.US);
                String label = "";
                try {
                    label = String.valueOf(info.loadLabel(pm));
                } catch (Throwable ignored) {
                }
                boolean looksLikeFileManager = pkg.contains("documentsui")
                        || pkg.contains("filemanager") || pkg.contains("file.manager")
                        || pkg.contains("fileexplorer") || pkg.contains("fileexplorer")
                        || label.contains("文件管理") || label.contains("在文件管理")
                        || label.toLowerCase(Locale.US).contains("files");
                if (looksLikeFileManager) {
                    picked = info;
                    break;
                }
            }
            if (picked == null && count == 1) {
                picked = onlyOne;   // 只有一个候选时直接用它
            }
            if (picked != null && picked.activityInfo != null) {
                intent.setComponent(new android.content.ComponentName(
                        picked.activityInfo.packageName, picked.activityInfo.name));
                startActivity(intent);
                return true;
            }
            // 实在挑不出来：交给系统面板（至少能用）
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    /** 目录 URI 打不开时的兜底：把路径给人看，附带复制/用其他应用打开。 */
    private void showPathDialog(final ExportHistory.Entry entry) {
        try {
            new android.app.AlertDialog.Builder(this)
                    .setTitle(entry.name)
                    .setMessage(entry.path + "\n\n" + buildMeta(entry))
                    .setPositiveButton(R.string.export_history_copy_path, (dialog, which) ->
                            copyPath(entry.path))
                    .setNeutralButton(R.string.export_history_open_mt, (dialog, which) -> {
                        if (!openInMt(entry)) {
                            copyPath(entry.path);
                        }
                    })
                    .setNegativeButton(R.string.export_history_open_folder, (dialog, which) -> {
                        if (!openFolderDirect(entry)) {
                            toast(getString(R.string.export_history_open_fail));
                        }
                    })
                    .show();
        } catch (Throwable th) {
            toast(getString(R.string.export_history_open_fail));
        }
    }

    private void copyPath(String path) {
        try {
            android.content.ClipboardManager cm =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(android.content.ClipData.newPlainText("path", path));
                toast(getString(R.string.export_history_copied));
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * v1.10.42：复制完整路径 + 拉起 MT。
     * MT 没有「按路径定位」的对外接口，所以按它自己的用法来：路径进剪贴板，
     * 用户在 MT 顶部路径栏粘贴即可跳转（Claude 给的方案，实测可行）。
     * Manifest 里必须声明 <queries><package android:name="bin.mt.plus"/></queries>，
     * 否则 Android 11+ 的 getLaunchIntentForPackage 直接返回 null。
     */
    private boolean openInMt(ExportHistory.Entry entry) {
        copyPath(entry.path);
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage("bin.mt.plus");
            if (launch == null) {
                return false;
            }
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    private boolean openFolder(ExportHistory.Entry entry) {
        try {
            String dir = entry.path != null && entry.path.contains("/Pictures") ? "Pictures" : "Download";
            // v1.10.39：用 DocumentsContract 构造（documentId 里的冒号必须编码成 %3A，
            // 手拼 "primary:Download" 在部分 ROM 上解析不了 → 才会掉到「打开方式」弹窗）
            Uri folder = android.provider.DocumentsContract.buildDocumentUri(
                    "com.android.externalstorage.documents", "primary:" + dir);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(folder, "vnd.android.document/directory");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    private boolean openFile(ExportHistory.Entry entry) {
        try {
            Uri uri = null;
            if (entry.uri != null && entry.uri.length() > 0) {
                uri = Uri.parse(entry.uri);
            }
            if (uri == null) {
                return false;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, entry.images ? "image/png" : "text/plain");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            // v1.10.44：必须走 createChooser —— 直接 startActivity 会命中系统记住的默认应用
            // （你这台把「vivo 文档」设成了总是，所以一点就进去，选不到 MT）
            startActivity(Intent.createChooser(intent, getString(R.string.export_history_open_with)));
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    private void toast(@Nullable String message) {
        if (message == null || message.length() == 0) {
            return;
        }
        try {
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onTextsRefreshed() {
        render();
    }

    @NotNull
    @Override
    public String toString() {
        return "ExportHistoryActivity";
    }
}
