package io.github.animwidget;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.app.AlertDialog;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 日志页（v1.10.0）。
 *
 * 规格对齐：
 * • 全屏页（与设置页一致）：标题 / 两排按钮 / 提示行 / 空态 / 正文，一页竖排（v1.10.436 起没有
 *   ScrollView，正文是自绘控制台 {@link LogConsoleView}，自己管滚动）；
 * • 正文不换行、可上下左右滑，只画可见那几行；
 * • 深浅色 / 中英文 / 系统配色跟随（attachBaseContext 走 LocaleHelper，主题走 ThemeManager）；
 * • 日志只保留 30 分钟（{@link LogBook#TTL_MS}）：打开期间每 2 秒按时间戳实时清理过期的；
 * • 「清屏」不弹确认、无日志时置灰；「撤回」只能撤回最近一次，退出页面后失效。
 */
public final class LogActivity extends I18nActivity {

    @NotNull
    private final Handler ui = new Handler(Looper.getMainLooper());

    /** v1.10.430：自绘日志控制台（不换行 / 双向滑动 / 只排可见行）。
     *  v1.10.436：**它就是唯一的正文视图** —— 原来那个塞 3000 行的 TextView 已从布局里删除。 */
    private LogConsoleView console;
    @Nullable
    private TextView tvEmpty;
    @Nullable
    private Button btnClear;
    @Nullable
    private Button btnUndo;
    @Nullable
    private Button btnExport;
    @Nullable
    private Dialog exportDialog;
    @Nullable
    private View exportCard;
    @Nullable
    private TextView exportTitle;
    @Nullable
    private TextView exportHint;
    @Nullable
    private Button exportTxt;
    @Nullable
    private Button exportTxtOpen;
    @Nullable
    private Button exportImages;
    private boolean exporting;
    @Nullable
    private String lastText;
    /** v1.10.91：正文的淡入/淡出（清屏=渐隐，撤回=反向渐显；可从任意透明度接着反向）。 */
    @Nullable
    private android.animation.ValueAnimator fadeAnimator;
    /** 当前这一段是不是「淡出」（用来挡住连点清屏）。 */
    private boolean fadingOut;
    private static final long FADE_OUT_MS = 260L;
    private static final long FADE_IN_MS = 300L;
    private static final long FADE_IN_QUICK_MS = 180L;

    /**
     * v1.10.481（用户要求）：**日志页导航栏 45% 透明**。
     *
     * <p>口径：45% 透明 = 不透明度 55% ⇒ 页面底色 × 0.55（255×0.55 ≈ 140）。</p>
     *
     * <p>为什么自己画一条膜，而不是 {@code window.setNavigationBarColor}：这一页是 edge-to-edge
     * （内容铺到系统栏下面、两条栏刷透明），而 targetSdk 35 起导航栏色在 35+ 系统上对
     * edge-to-edge 页面已经失效；三键/手势两种导航方式下「系统栏」占的高度也不一样。
     * 自己在**导航栏 inset 那一条**上铺一层半透明膜，三种情况都看得见，且换肤时能跟着换色。</p>
     */
    private static final float NAV_BAR_OPAQUE = 0.55f;
    /** 导航栏那条半透明膜（v1.10.481）。 */
    @Nullable
    private View navScrim;

    @NotNull
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            refresh();
            LogActivity.this.ui.postDelayed(this, 2000L);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashLog.install(this);
        setContentView(R.layout.activity_log);
        // v1.10.156：日志页不参与「字体大小」缩放（用户要求：不涉及系统日志页任何参数）
        Fonts.markNoScale(findViewById(android.R.id.content));
        setI18nLayout(R.layout.activity_log);
        // v1.10.437（用户："日志页导航栏改为透明"）：整页铺到系统栏下面，两条栏刷成透明。
        // 页面上下留白由 {@link EdgeToEdge#applyPageUnderBars} 按 inset 补，左右留给文字列 ——
        // 控制台因此可以铺满整幅宽度，右侧滑块才贴屏幕边（见 installConsole 的注释）。
        View contentRoot = findViewById(android.R.id.content);
        View pageView = contentRoot instanceof ViewGroup && ((ViewGroup) contentRoot).getChildCount() > 0
                ? ((ViewGroup) contentRoot).getChildAt(0) : null;
        View columnView = pageView instanceof ViewGroup && ((ViewGroup) pageView).getChildCount() > 0
                ? ((ViewGroup) pageView).getChildAt(0) : null;
        EdgeToEdge.INSTANCE.applyPageUnderBars(this, pageView, columnView);
        installNavScrim(contentRoot);
        installConsole();
        this.tvEmpty = (TextView) findViewById(R.id.tv_log_empty);
        Button back = (Button) findViewById(R.id.btn_log_back);
        this.btnClear = (Button) findViewById(R.id.btn_log_clear);
        this.btnUndo = (Button) findViewById(R.id.btn_log_undo);
        this.btnExport = (Button) findViewById(R.id.btn_log_export);
        if (this.btnExport != null) {
            this.btnExport.setOnClickListener((view) -> showExportDialog());
        }
        // v1.10.38：历史导出（列出所有导出过的文件，可删除、可跳转）
        Button history = (Button) findViewById(R.id.btn_log_history);
        if (history != null) {
            history.setOnClickListener((view) -> startActivity(
                    new Intent(LogActivity.this, ExportHistoryActivity.class)));
        }
        if (back != null) {
            back.setOnClickListener((view) -> finish());
        }
        // v1.10.107（需求 1）：判断代号入口（口令门 + 10 分钟 3 次限流）
        View diagEntry = findViewById(R.id.btn_log_diag);
        if (diagEntry != null) {
            diagEntry.setOnClickListener((view) -> openDiagGate());
        }
        if (this.btnClear != null) {
            // v1.10.91：清屏 = **纯渐隐**（不再缩小，也不再有黑洞）；撤回 = 反向渐显
            this.btnClear.setOnClickListener((view) -> clearWithFade());
        }
        if (this.btnUndo != null) {
            this.btnUndo.setOnClickListener((view) -> undoWithFade());
        }
        applyTheme();
        LogBook.add(this, 'D', "J01 日志页打开");
        // v1.10.262：打开日志页顺便拍一张内存快照，用户不用等 60 秒的周期
        PlaybackService.reportMemory(this, "日志页");
        refresh();
    }

    /**
     * v1.10.481：在导航栏那一条铺一层「页面底色 @ 45% 透明」的膜。
     *
     * <p>高度由 {@code WindowInsets.Type.navigationBars().bottom} 实时决定（三键 48dp / 手势条
     * 24dp 自动跟上），不写死 dp。低于 30 的机型本来就没有 edge-to-edge（
     * {@code applyPageUnderBars} 直接 return），这里也跳过。</p>
     */
    private void installNavScrim(@Nullable View contentRoot) {
        if (android.os.Build.VERSION.SDK_INT < 30) {
            return;
        }
        if (!(contentRoot instanceof FrameLayout)) {
            return;
        }
        try {
            View scrim = new View(this);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, Gravity.BOTTOM);
            ((FrameLayout) contentRoot).addView(scrim, lp);
            scrim.setOnApplyWindowInsetsListener((view, insets) -> {
                int bottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                ViewGroup.LayoutParams p = view.getLayoutParams();
                if (p != null && p.height != bottom) {
                    p.height = bottom;
                    view.setLayoutParams(p);
                }
                return insets;
            });
            this.navScrim = scrim;
        } catch (Throwable ignored) {
        }
    }

    /** v1.10.509：打开中的口令门（换深浅色时要原地重建，所以要留引用）。 */
    @Nullable
    private GlassPrompt diagGate;

    /**
     * 判断代号页的口令门（v1.10.509：**真正的毛玻璃**）。
     *
     * <p>前五版（504~508）都只是「半透明色块 + 描边」压在**清晰**的页面上 ——
     * 透出来的还是那个页面，所以不管把 alpha 调到 0.9 还是 0.6，看上去都是"面板"。
     * 用户连续五次说的其实是同一句话：**看不到"糊"这件事发生**。</p>
     *
     * <p>这一版把三件事一起做到（细节见 {@link GlassPrompt}）：</p>
     * <ol>
     *   <li>**整屏真糊**：开窗前把页面拍成 1/6 小图、盒式模糊两遍铺满全屏，再压一层很淡的暗；</li>
     *   <li>**卡片是玻璃**：{@link FrostedGlassDrawable} —— 表面 0.62/0.66 不透明度，
     *       卡片所在那块区域**再糊一遍**当内芯，加顶棱线 / 底内阴影 / 26dp 渐变描边 / 20dp 软投影；</li>
     *   <li>**居中且跟键盘**：窗口铺满整屏、自己一个像素都不动，键盘高度由 {@code ime()} inset 现算，
     *       卡片只上移到"刚好离开键盘 12dp"—— 不是整窗上推一个键盘的高度（508 的硬伤）。</li>
     * </ol>
     *
     * <p>口令校验 / 限流 / 锁定三条分支的判据一个都没动，只换了展现方式：
     * 错口令仍然**不关窗**、清空输入框、toast 剩余次数。</p>
     */
    private void openDiagGate() {
        long locked = DiagVault.lockedRemain(this);
        if (locked > 0L) {
            Toast.makeText(this,
                    getString(R.string.log_diag_pass_locked, (int) Math.ceil(locked / 1000.0)),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // 上一块还没关（连点入口）就先收掉：不要叠两层模糊底图
        GlassPrompt previous = this.diagGate;
        if (previous != null) {
            previous.dismiss();
            this.diagGate = null;
        }
        final GlassPrompt gate = new GlassPrompt(this)
                .title(getString(R.string.log_diag_pass_title))
                .hint(getString(R.string.log_diag_pass_hint))
                .cancel(getString(R.string.cancel))
                .confirm(getString(android.R.string.ok));
        // 关闭（取消 / 提交 / 返回键）后把引用放掉 —— 这里用 final 局部量做身份比对，
        // 免得"上一块正在播关闭动画"时把刚打开的这一块的引用清成 null
        gate.onClosed(() -> {
            if (this.diagGate == gate) {
                this.diagGate = null;
            }
        });
        gate.onSubmit(text -> {
            int result = DiagVault.check(this, text);
            if (result == DiagVault.OK) {
                startActivity(new Intent(this, DiagCatalogActivity.class));
                return true;
            }
            if (result == DiagVault.LOCKED) {
                long left = DiagVault.lockedRemain(this);
                Toast.makeText(this,
                        getString(R.string.log_diag_pass_locked, (int) Math.ceil(left / 1000.0)),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
            Toast.makeText(this,
                    getString(R.string.log_diag_pass_wrong) + " · "
                            + getString(R.string.log_diag_pass_remain, DiagVault.remaining(this)),
                    Toast.LENGTH_SHORT).show();
            // 口令错：留在原地、清空输入框（不关窗）
            GlassPrompt open = this.diagGate;
            if (open != null) {
                open.clearInput();
            }
            return false;
        });
        this.diagGate = gate;
        gate.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // v1.10.1（需求 9）：语言变了就地换文案，不重建、不丢状态、不闪
        syncLocale();
        applyTheme();
        refresh();
        this.ui.removeCallbacks(this.ticker);
        this.ui.postDelayed(this.ticker, 2000L);
    }

    @Override
    protected void onStart() {
        super.onStart();
        DeskWatcher.setAppForeground(true);
    }

    @Override
    protected void onStop() {
        DeskWatcher.setAppForeground(false);
        super.onStop();
    }

    @Override
    protected void onPause() {
        this.ui.removeCallbacks(this.ticker);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        this.ui.removeCallbacks(this.ticker);
        // v1.10.91：把淡入淡出停掉并复位透明度，避免下次进来正文是半透明的
        cancelFade();
        if (this.console != null) {
            this.console.setAlpha(1.0f);
        }
        // v1.10.509：口令门是独立窗口，页面走了必须一起收掉（否则两张底图一直挂着）
        GlassPrompt gate = this.diagGate;
        this.diagGate = null;
        if (gate != null) {
            gate.dismiss();
        }
        // 用户规格：退出日志页后撤回入口失效
        LogBook.forgetUndo();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // v1.10.40：原地换肤，不 recreate —— 重建会把页面顶掉（用户反馈「切黑白后返回菜单」）
        // v1.10.47：这里原来把导出弹窗 dismiss 掉，用户看到的就是「切系统模式弹窗被折叠」。
        //           现在弹窗原地换肤：不关闭、不重建、不丢状态。
        retintExportDialog();
        this.lastText = null;
        applyTheme();
        refresh();
        // v1.10.512：**口令门必须在页面换完色之后才重开**。
        // 它的整屏底图是把「这一页」拍下来糊出来的，重开时那一刻截到什么色就是什么色。
        // v1.10.509~511 把这条放在 applyTheme() 之前 —— 卡片自己换了色、底图还是旧主题那一张，
        // 用户看到的就是「切系统模式，背景没有更新」。
        // 再往后放一个消息：applyTheme 里 applyTo/applySystemBars 都是同步写属性，
        // 但排在同一个回调末尾的 refresh() 可能还要走一次查找，放到下一轮最稳。
        final GlassPrompt gate = this.diagGate;
        if (gate != null) {
            this.ui.post(gate::onSystemThemeChanged);
        }
    }

    @Override
    protected void onTextsRefreshed() {
        this.lastText = null;
        refresh();
    }

    private void applyTheme() {
        try {
            View content = findViewById(android.R.id.content);
            ThemeManager.INSTANCE.applyTo(content);
            ThemeManager.INSTANCE.applySystemBars(this);
            ThemeColors colors = ThemeManager.INSTANCE.colors(this);
            // v1.10.437：提示行（"只保留最近 30 分钟的日志…"）是用户要的，恢复并跟着换肤。
            TextView hint = (TextView) findViewById(R.id.tv_log_hint);
            if (hint != null) {
                hint.setTextColor(colors.getTextMuted());
            }
            if (this.console != null) {
                this.console.setConsoleTextColor(colors.getText());
                this.console.setBarColor(ThemeManager.INSTANCE.withAlpha(colors.getText(), 0.35f));
            }
            if (this.tvEmpty != null) {
                this.tvEmpty.setTextColor(colors.getTextMuted());
            }
            // v1.10.481：导航栏那条膜跟着换肤（同一个口径：页面底色 × 0.55）
            if (this.navScrim != null) {
                this.navScrim.setBackgroundColor(
                        ThemeManager.INSTANCE.withAlpha(colors.getBackground(), NAV_BAR_OPAQUE));
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * v1.10.91：清屏 —— **纯渐隐**。
     *
     * 关键点（不然就会「撤回后新日志消失」）：
     *   ① **点下去就把清屏落库**（快照 = 此刻的全部日志），淡出只是让旧文字退场；
     *      所以淡出这 260ms 里新产生的日志不会被打包进快照、也不会被抹掉；
     *   ② 按钮状态立刻刷新（不等动画结束），否则「清屏→马上撤回」这 260ms 里撤回按钮还是灰的，
     *      用户会以为没反应；
     *   ③ 淡出期间 {@link #refresh()} 暂停换文字（边渐隐边跳字很难看），淡出结束再一次性换上
     *      「清屏之后的内容」（通常是空，若期间有新日志就让它们再淡入）。
     */
    private void clearWithFade() {
        if (this.fadingOut) {
            return;   // 已经在淡出，连点不叠加
        }
        cancelFade();
        if (!LogBook.clear(this)) {
            // 没有日志可清（例如按钮状态过时）：把透明度收回来，别停在半透明
            fadeBody(1.0f, FADE_IN_QUICK_MS, null);
            return;
        }
        syncButtons();
        toast(getString(R.string.log_cleared));
        fadeBody(0.0f, FADE_OUT_MS, new Runnable() {
            @Override
            public void run() {
                LogActivity.this.lastText = null;
                LogActivity.this.refresh(true);
                LogActivity.this.revealAfterClear();
            }
        });
    }

    /** 淡出结束后：这期间若又有新日志，让它们淡入（不能让它们凭空出现）。 */
    private void revealAfterClear() {
        final View body = this.console;
        if (body == null) {
            return;
        }
        if (LogBook.hasEntries(this)) {
            fadeBody(1.0f, FADE_IN_QUICK_MS, null);
        } else {
            body.setAlpha(1.0f);
        }
    }

    /**
     * v1.10.91：撤回 —— **反向渐显**。
     *
     * 先取消正在跑的淡出（保留当前透明度当起点，所以「清屏→立刻撤回」是从半透明直接反向，
     * 不会先黑一下再亮），再把数据恢复（{@link LogBook#undo} 是**合并**：撤回的旧日志
     * 与清屏后新产生的日志按时间并存），最后淡入。
     */
    private void undoWithFade() {
        cancelFade();
        final boolean hadClear = LogBook.canUndo();
        if (!LogBook.undo(this)) {
            // 撤回不了（状态被别处清掉）：同样把透明度收回来
            fadeBody(1.0f, FADE_IN_QUICK_MS, null);
            return;
        }
        this.lastText = null;
        final View body = this.console;
        // v1.10.92：**修「撤回没有渐显」**。
        // 清屏淡出结束后 revealAfterClear() 会把正文透明度复位成 1（不然之后新日志会看不见），
        // 于是撤回时 from==target，fadeBody 判定「不用动」→ 直接跳到全不透明，看起来没有动画。
        // 所以：确认这次撤回的是「已经落库的清屏」，且正文当前几乎不透明（= 不在淡出途中），
        // 就把它按回 0，再淡入 —— 这才是「清屏渐隐」的镜像。
        if (body != null && hadClear && body.getAlpha() > 0.9f) {
            body.setAlpha(0.0f);
        }
        refresh(true);
        toast(getString(R.string.log_undone));
        fadeBody(1.0f, FADE_IN_MS, null);
    }

    /** 正文淡入/淡出：总是从**当前透明度**出发，中途反向也不跳。 */
    private void fadeBody(final float target, long duration, @Nullable final Runnable onDone) {
        final View body = this.console;
        if (body == null) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        final float from = clampAlpha(body.getAlpha());
        if (Math.abs(from - target) < 0.01f) {
            body.setAlpha(target);
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        final android.animation.ValueAnimator anim =
                android.animation.ValueAnimator.ofFloat(from, target);
        anim.setDuration(duration);
        anim.setInterpolator((t) -> Ease.IN_OUT.at(t));
        anim.addUpdateListener((a) -> body.setAlpha(
                clampAlpha(((Float) a.getAnimatedValue()).floatValue())));
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                this.cancelled = true;
            }

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (this.cancelled) {
                    return;
                }
                if (LogActivity.this.fadeAnimator == anim) {
                    LogActivity.this.fadeAnimator = null;
                }
                LogActivity.this.fadingOut = false;
                body.setAlpha(target);
                if (onDone != null) {
                    onDone.run();
                }
            }
        });
        this.fadeAnimator = anim;
        this.fadingOut = target < 0.5f;
        anim.start();
    }

    /**
     * 停掉当前淡入/淡出。注意：被取消的动画**不会**执行 onDone
     * （所以「淡出途中撤回」绝不会把清屏之外的副作用跑完）。
     */
    private void cancelFade() {
        final android.animation.ValueAnimator anim = this.fadeAnimator;
        this.fadeAnimator = null;
        this.fadingOut = false;
        if (anim != null) {
            try {
                anim.cancel();
            } catch (Throwable ignored) {
            }
        }
    }

    private static float clampAlpha(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }

    /**
     * v1.10.436：**日志页版式回到 XML，代码只塞一个控制台**。
     *
     * <p>430~435 那段是把"外层 ScrollView 拦手势"当成版式问题在运行时绕：把 ScrollView 里的
     * 内容一个个搬进新建的 page、再把 ScrollView 摘空。它引出两个真 bug：
     * <ol>
     *   <li><b>黑屏</b>：整段包在一个 catch(Throwable) 里，只要中间某步抛异常，
     *       内容已经搬走、page 还没挂上去 —— 页面就空了（用户看到的就是"打开日志黑屏"）。</li>
     *   <li><b>提示行永远藏不掉</b>：`tv_log_hint` 被一起搬进 page，而 435 的文本扫描
     *       恰好"跳过 keep 子树"，于是每一版都扫不到它（用户："给你版本号干什么"）。</li>
     * </ol>
     * 现在 activity_log.xml 本身就是竖排页：标题 / 两排按钮 / 提示行 / 空态 / `root_log`（FrameLayout，
     * height=0 + weight=1）——控制台直接住进 root_log，**没有 ScrollView、没有搬视图**。
     * 版式问题在版式里解决，不在运行时猜。
     *
     * <p>v1.10.437：`root_log` 是**整幅宽度**（页面横向 padding 只加在上半部分的文字列上），
     * 滑块画在控制台的右边缘 → 贴屏幕右边。
     */
    private void installConsole() {
        try {
            View host = findViewById(R.id.root_log);
            if (!(host instanceof ViewGroup)) {
                return;
            }
            ViewGroup box = (ViewGroup) host;
            box.removeAllViews();
            this.console = new LogConsoleView(this);
            box.addView(this.console, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            // v1.10.440（用户："导航栏没有透明，要么就是文本还没延伸"）：
            // 438/439 把导航栏高度做成了控制台的 paddingBottom —— 那是"让位"，字到栏上沿就停了，
            // 用户看到的还是"文本没延伸"。现在改成**内容末尾留白**（setTrailSpace）：
            //   · 正文本身一直画到屏幕最下沿，可以铺到三个键底下（这才叫延伸）；
            //   · 滚到底时末尾那段留白刚好把最后一行顶到栏上面，不会永远读不到最后一行。
            // 高度按系统 inset 现算（三键/手势/平板各不相同），不写死。
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                this.console.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                    @Override
                    public android.view.WindowInsets onApplyWindowInsets(View v, android.view.WindowInsets insets) {
                        int bottom = insets.getInsets(
                                android.view.WindowInsets.Type.systemBars()
                                        | android.view.WindowInsets.Type.displayCutout()).bottom;
                        if (v instanceof LogConsoleView) {
                            ((LogConsoleView) v).setTrailSpace(bottom);
                        }
                        return insets;
                    }
                });
            }
        } catch (Throwable th) {
            // 控制台起不来也不许让页面空着（顺带把原因留在日志里，用户下次能导出）
            th.printStackTrace();
            LogBook.add(this, 'W', "J03 控制台装配失败 " + th.getClass().getSimpleName());
        }
    }

    /** 按当前落盘内容重绘（内容没变就不换）。 */
    private void refresh() {
        refresh(false);
    }

    /**
     * @param force true = 跳过"正在滑动就不换内容"的节流。
     *
     * <p>v1.10.438：清屏/撤回**必须**立刻生效 —— 原来它们走的是同一条节流路径，
     * 撞上"正在滑动"（用户刚抛过列表，见 LogConsoleView.isScrolling 的注释）时
     * 整个 refresh 被 return 掉：数据清了画面不动，用户看到的就是"清屏不了日志"。
     */
    private void refresh(boolean force) {
        // v1.10.91：淡入淡出期间不换内容（否则「边渐隐边跳字」）；结束时会统一刷一次
        if (this.fadeAnimator != null) {
            return;
        }
        // v1.10.429：只拼"要显示的那部分"（末尾 3000 行），不再先把整份日志拼成一个大字符串。
        // v1.10.435：正在滑动/惯性时不换内容 —— refresh 每 2 秒整块替换，撞上滑动就是那次"频闪"
        // v1.10.438：只有"定时刷新"才节流；清屏/撤回走 force=true，绝不能吞。
        if (!force && this.console != null && this.console.isScrolling()) {
            return;
        }
        String[] tail = LogBook.renderTail(this, 3000);
        String text = tail != null ? tail[0] : "";
        boolean truncated = tail != null && "1".equals(tail[1]);
        if (text == null) {
            text = "";
        }
        boolean empty = text.length() == 0;
        if (this.tvEmpty != null) {
            this.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
        if (this.console != null && !text.equals(this.lastText)) {
            this.lastText = text;
            // v1.10.408：**只渲染末尾 3000 行**（导出入口仍读全量）。
            // v1.10.436：那个"一次性 setText 3000 行"的 TextView 已经从布局里删掉了 ——
            // 主线程不再跑巨型 StaticLayout，黑屏/卡死的那一半成因一起消失。
            String shown = text;
            if (truncated) {
                // v1.10.409：截断了就明说，别让人以为日志丢了（导出仍是全量）
                shown = "（只显示最近 3000 行；完整内容请用「导出」）\n\n" + shown;
            }
            this.console.setText(shown);   // v1.10.430：真正显示的是自绘控制台
        }
        syncButtons();
    }

    /**
     * 只刷三个按钮的可用态（v1.10.91 抽出来：清屏点下去就立刻生效，
     * 不等 260ms 淡出结束 —— 否则「清屏→马上撤回」这段时间撤回按钮还是灰的，点不动）。
     */
    private void syncButtons() {
        boolean hasLog = LogBook.hasEntries(this);
        if (this.btnClear != null) {
            this.btnClear.setEnabled(hasLog);
            this.btnClear.setAlpha(hasLog ? 1.0f : 0.45f);
        }
        if (this.btnUndo != null) {
            boolean can = LogBook.canUndo();
            this.btnUndo.setEnabled(can);
            this.btnUndo.setAlpha(can ? 1.0f : 0.45f);
        }
        // v1.10.27：没有日志时「导出日志」必须不可用
        if (this.btnExport != null) {
            boolean can = hasLog && !this.exporting;
            this.btnExport.setEnabled(can);
            this.btnExport.setAlpha(can ? 1.0f : 0.45f);
        }
    }

    /**
     * v1.10.27（需求 2）：导出日志对话框 —— 居中弹出 + 缩放淡入（220ms，减速曲线），
     * 两个选项：全文 TXT / 分批长图（每张 100 行，PNG 无损）。
     *
     * v1.10.47：弹窗内的控件都留了引用，切系统黑白时可以原地换肤（{@link #retintExportDialog()}）。
     */
    private void showExportDialog() {
        String text = LogBook.render(this);
        if (text == null || text.trim().length() == 0) {
            toast(getString(R.string.log_export_empty));
            return;
        }
        final String snapshot = text;
        final Dialog dialog = new Dialog(this);
        this.exportDialog = dialog;
        dialog.setOnDismissListener((d) -> {
            if (this.exportDialog == d) {
                this.exportDialog = null;
            }
            this.exportCard = null;
            this.exportTitle = null;
            this.exportHint = null;
            this.exportTxt = null;
            this.exportTxtOpen = null;
            this.exportImages = null;
        });
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ThemeManager.INSTANCE.clearDialogWindow(dialog);   // v1.10.71：圆角不露方底
        View content = getLayoutInflater().inflate(R.layout.dialog_log_export, null);
        Fonts.markNoScale(content);   // 导出日志弹窗同样不缩
        this.exportCard = content;
        View titleView = content.findViewWithTag("exportTitle");
        if (titleView instanceof TextView) {
            this.exportTitle = (TextView) titleView;
        }
        View hintView = content.findViewWithTag("exportHint");
        if (hintView instanceof TextView) {
            this.exportHint = (TextView) hintView;
        }
        this.exportTxt = (Button) content.findViewById(R.id.btn_export_txt);
        if (this.exportTxt != null) {
            this.exportTxt.setOnClickListener((v) -> {
                dialog.dismiss();
                doExport(snapshot, false, false);
            });
        }
        this.exportImages = (Button) content.findViewById(R.id.btn_export_images);
        if (this.exportImages != null) {
            this.exportImages.setOnClickListener((v) -> {
                dialog.dismiss();
                doExport(snapshot, true, false);
            });
        }
        // v1.10.35：「导出并打开」—— 系统文本查看器打开一次后，这个文件才会进
        // vivo「文档」的「最近文件」（那份列表是使用记录，不是 MediaStore 索引）
        this.exportTxtOpen = (Button) content.findViewById(R.id.btn_export_txt_open);
        if (this.exportTxtOpen != null) {
            this.exportTxtOpen.setOnClickListener((v) -> {
                dialog.dismiss();
                doExport(snapshot, false, true);
            });
        }
        applyExportTheme();
        dialog.setContentView(content);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        float density = getResources().getDisplayMetrics().density;
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
            // v1.10.28：加一层压暗，弹窗和背后的日志页一眼分得开（之前看着像「两个页面」）
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.6f);
            window.setGravity(Gravity.CENTER);
            int maxWidth = getResources().getDisplayMetrics().widthPixels - (int) (56 * density);
            window.setLayout(Math.min(maxWidth, (int) (340 * density)), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        // 流畅的弹出动画：0.86 → 1.0 缩放 + 淡入
        content.setAlpha(0.0f);
        content.setScaleX(0.86f);
        content.setScaleY(0.86f);
        content.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f)
                .setDuration(220L)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /** 弹窗配色（弹出时 + 每次切黑白时都走一遍，原地改颜色，不动结构）。 */
    private void applyExportTheme() {
        ThemeColors colors = ThemeManager.INSTANCE.colors(this);
        float density = getResources().getDisplayMetrics().density;
        // Layout 里那个 card_surface 是固定浅色 drawable，不跟黑白模式 ——
        // 深色主题下就成了「白底 + 白字」。这里整块用代码画：底色取当前主题的 surface，圆角 20dp。
        if (this.exportCard != null) {
            android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
            cardBg.setColor(colors.getSurface());
            cardBg.setCornerRadius(20 * density);
            this.exportCard.setBackground(cardBg);
        }
        if (this.exportTitle != null) {
            this.exportTitle.setTextColor(colors.getText());
        }
        if (this.exportHint != null) {
            this.exportHint.setTextColor(colors.getTextMuted());
        }
        if (this.exportTxt != null) {
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(colors.getPrimary());
            bg.setCornerRadius(25 * density);
            this.exportTxt.setBackground(bg);
            this.exportTxt.setTextColor(colors.getOnPrimary());
        }
        if (this.exportTxtOpen != null) {
            this.exportTxtOpen.setBackground(ThemeManager.INSTANCE.outlined(this, 0, colors.getNeutral(), 23.0f));
            this.exportTxtOpen.setTextColor(colors.getText());
        }
        if (this.exportImages != null) {
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(colors.getNeutral());
            bg.setCornerRadius(25 * density);
            this.exportImages.setBackground(bg);
            this.exportImages.setTextColor(colors.getText());
        }
    }

    /** v1.10.47：切系统黑白时原地给导出弹窗换肤（不 dismiss，避免「弹窗被折叠」）。 */
    private void retintExportDialog() {
        try {
            if (this.exportDialog != null && this.exportDialog.isShowing()) {
                applyExportTheme();
            }
        } catch (Throwable ignored) {
        }
    }

    /** 导出在后台线程做（长图几十张会有一两秒），完成后主线程提示保存位置。 */
    private void doExport(final String text, final boolean images, final boolean openAfter) {
        if (this.exporting) {
            return;
        }
        this.exporting = true;
        refresh();
        toast(getString(R.string.log_export_busy));
        // v1.10.28：导出期间停掉 2 秒一次的整页重绘（几千行 setText 会抢主线程）
        this.ui.removeCallbacks(this.ticker);
        new Thread(() -> {
            String ok = null;
            String error = null;
            try {
                ok = images ? LogExporter.exportImages(LogActivity.this, text)
                        : LogExporter.exportTxt(LogActivity.this, text);
            } catch (Throwable th) {
                error = th.getMessage() != null ? th.getMessage() : th.getClass().getSimpleName();
            }
            final String okMsg = ok;
            final String errMsg = error;
            LogActivity.this.ui.post(() -> {
                LogActivity.this.exporting = false;
                LogActivity.this.refresh();
                LogActivity.this.ui.removeCallbacks(LogActivity.this.ticker);
                LogActivity.this.ui.postDelayed(LogActivity.this.ticker, 2000L);
                if (errMsg != null) {
                    toast(getString(R.string.log_export_fail, new Object[]{errMsg}));
                } else {
                    toast(getString(R.string.log_export_done, new Object[]{okMsg}));
                    if (openAfter) {
                        openLastExport();
                    }
                }
            });
        }).start();
    }

    /** v1.10.35：用系统查看器打开刚导出的 txt —— 让它在「最近文件」里留下记录。 */
    private void openLastExport() {
        try {
            Uri uri = LogExporter.getLastUri();
            if (uri == null) {
                return;
            }
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(uri, "text/plain");
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(view);
        } catch (Throwable ignored) {
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
}
