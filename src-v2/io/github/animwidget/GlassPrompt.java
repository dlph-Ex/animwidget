package io.github.animwidget;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v1.10.509：**全屏毛玻璃 + 居中玻璃卡片的「输入口令」模态框**。
 *
 * <p>为什么又开一个新类（前五版都在 {@code LogActivity.openDiagGate()} 里原地改）：</p>
 *
 * <p>用户的否掉理由一路很具体：504「外观变了/不跟键盘」→ 505「面板不透明」→ 506「只是改回原样」
 * → 507「界面半透明不是背景」→ 508「借用了上次的 / 我要的是半透明毛玻璃」。
 * 把这五条并起来只有一句话：**屏幕上看不到"毛玻璃"这件事发生**。
 * 504~508 的卡片都只是「半透明色块 + 描边」—— 背后是**清晰**的页面，透出来的还是那个页面，
 * 所以哪怕 alpha 调到 0.90 也像面板，调到 0.60 就像"字飘在页面上面"。</p>
 *
 * <p>这一版把缺的那一半补上（三件事同时成立才算毛玻璃）：</p>
 * <ol>
 *   <li>**整屏真的糊**：开窗前把日志页拍成 1/6 的小图，两遍横竖盒式模糊 →
 *       全屏铺开当底图（{@code ConfirmDialog} 用的就是这套语言，用户从没否过它），再压一层很淡的暗；</li>
 *   <li>**卡片是玻璃**：{@link FrostedGlassDrawable} —— 表面 0.62 不透明度 + **卡片区域内再糊一遍**
 *       的内芯 + 顶棱线 + 底内阴影 + 26dp 圆角渐变描边 + 软投影；</li>
 *   <li>**居中且跟键盘**：窗口铺满整屏（非浮窗主题），卡片自己居中；键盘不用系统的 PAN/RESIZE，
 *       改成读 {@code WindowInsets.Type.ime()} 把卡片**只上移到刚好的位置**
 *       （508 的 PAN 会把整窗上推一个键盘的高度，卡片飞到屏幕上半截，看起来就是"不跟手"）。</li>
 * </ol>
 *
 * <p>窗口主题必须用 {@link ThemeManager#dialogTheme(Context)}（非浮窗 + 半透明 + 无系统 dim）：
 * 只有非浮窗窗口才能真的铺到系统栏下面，模糊底才不会在状态栏那一条断掉（LESSONS #219 / v1.10.416）。</p>
 *
 * <p>用法：</p>
 * <pre>
 *   new GlassPrompt(this)
 *       .title(getString(R.string.log_diag_pass_title))
 *       .hint(getString(R.string.log_diag_pass_hint))
 *       .onSubmit(text -&gt; { ... return true 关闭 / false 留在原地 ... })
 *       .show();
 * </pre>
 */
final class GlassPrompt {

    /** 提交回调：返回 true = 已经处理完，关窗；false = 留在原地（口令错时用）。 */
    interface OnSubmit {
        boolean onSubmit(@NotNull String text);
    }

    /** 背景底图的缩放比（1/6：够糊、又只是 0.5MB 级别的小图）。 */
    private static final float CAPTURE_SCALE = 1.0f / 6.0f;
    /** 卡片表面不透明度：毛玻璃就该透，0.9 以上是面板（504~508 的病灶）。 */
    private static final float SURFACE_ALPHA_DARK = 0.62f;
    private static final float SURFACE_ALPHA_LIGHT = 0.66f;

    @NotNull
    private final Activity activity;
    private final float dp;

    @Nullable
    private CharSequence title;
    @Nullable
    private CharSequence hint;
    @Nullable
    private CharSequence confirmText;
    @Nullable
    private CharSequence cancelText;
    @Nullable
    private OnSubmit onSubmit;
    @Nullable
    private Runnable onCancel;
    @Nullable
    private Runnable onClosed;

    private ThemeColors colors;
    private boolean dark;

    @Nullable
    private Dialog dialog;
    @Nullable
    private FrameLayout root;
    @Nullable
    private View backdrop;
    @Nullable
    private FrameLayout stage;
    @Nullable
    private LinearLayout card;
    @Nullable
    private EditText input;
    @Nullable
    private FrostedGlassDrawable surface;

    /** 1/6 小图，糊得轻：整屏底图。 */
    @Nullable
    private Bitmap soft;
    /** 1/6 小图，糊得重：卡片内芯（"玻璃有厚度"就靠它）。 */
    @Nullable
    private Bitmap heavy;
    private int originX;
    private int originY;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private int sourceWidth;
    private int sourceHeight;

    /** 键盘高度（px，0 = 没弹）。 */
    private int imeHeight;
    private boolean closing;
    /** 换深浅色后整块重建：这一次不播入场动画（不然像"又弹了一次"）。 */
    private boolean quietNext;
    /** v1.10.512：重建进行中（防止一次换肤被两个回调各拆一遍）。 */
    private boolean rebuilding;
    @Nullable
    private String keptText;

    GlassPrompt(@NotNull Activity activity) {
        this.activity = activity;
        this.dp = activity.getResources().getDisplayMetrics().density;
        this.colors = ThemeManager.INSTANCE.colors(activity);
        this.dark = ThemeManager.INSTANCE.isDark(activity);
    }

    @NotNull
    GlassPrompt title(@Nullable CharSequence value) {
        this.title = value;
        return this;
    }

    @NotNull
    GlassPrompt hint(@Nullable CharSequence value) {
        this.hint = value;
        return this;
    }

    @NotNull
    GlassPrompt confirm(@Nullable CharSequence value) {
        this.confirmText = value;
        return this;
    }

    @NotNull
    GlassPrompt cancel(@Nullable CharSequence value) {
        this.cancelText = value;
        return this;
    }

    @NotNull
    GlassPrompt onSubmit(@Nullable OnSubmit listener) {
        this.onSubmit = listener;
        return this;
    }

    @NotNull
    GlassPrompt onCancel(@Nullable Runnable listener) {
        this.onCancel = listener;
        return this;
    }

    /** 无论怎么关（取消 / 提交 / 返回键），最后都会走一次（调用方用来放掉自己的引用）。 */
    @NotNull
    GlassPrompt onClosed(@Nullable Runnable listener) {
        this.onClosed = listener;
        return this;
    }

    /** 口令错了：清空输入框但**不关窗**（508 的行为，用户没提过异议）。 */
    void clearInput() {
        EditText field = this.input;
        if (field != null) {
            field.setText("");
            field.requestFocus();
        }
    }

    // ------------------------------------------------------------------ 开窗

    void show() {
        if (this.activity.isFinishing()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 17 && this.activity.isDestroyed()) {
            return;
        }
        this.colors = ThemeManager.INSTANCE.colors(this.activity);
        this.dark = ThemeManager.INSTANCE.isDark(this.activity);
        capture();
        build();
        showWindow();
        if (this.quietNext) {
            this.quietNext = false;
            View back = this.backdrop;
            if (back != null) {
                back.setAlpha(1.0f);
            }
            View box = this.card;
            if (box != null) {
                box.setAlpha(1.0f);
                box.setScaleX(1.0f);
                box.setScaleY(1.0f);
                box.setTranslationY(0.0f);
            }
        } else {
            playIn();
        }
    }

    /**
     * 拍下当前页面 → 两张 1/6 的模糊图。
     *
     * <p>**直接画进 1/6 的小位图**，不是「先截 1080×2400 的 10.4MB 再缩」：
     * 后者是 v1.10.499 之前「拖滑块迟钝」的直接来源（LESSONS 记录的第一现场）。
     * 这里 Canvas 先 scale(1/6) 再 draw，像素工作量是 1/36，分配只有 ~0.5MB。</p>
     */
    private void capture() {
        Window window = this.activity.getWindow();
        View source = window != null ? window.getDecorView() : null;
        if (source == null) {
            source = this.activity.findViewById(android.R.id.content);
        }
        if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
            return;
        }
        int fullW = source.getWidth();
        int fullH = source.getHeight();
        int smallW = Math.max(12, Math.round(fullW * CAPTURE_SCALE));
        int smallH = Math.max(12, Math.round(fullH * CAPTURE_SCALE));
        Bitmap base;
        try {
            base = Bitmap.createBitmap(smallW, smallH, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(base);
            canvas.scale((float) smallW / fullW, (float) smallH / fullH);
            source.draw(canvas);
        } catch (Throwable th) {
            return;
        }
        this.sourceWidth = fullW;
        this.sourceHeight = fullH;
        this.scaleX = (float) smallW / fullW;
        this.scaleY = (float) smallH / fullH;
        try {
            int[] a = new int[smallW * smallH];
            int[] b = new int[smallW * smallH];
            base.getPixels(a, 0, smallW, 0, 0, smallW, smallH);
            // 轻糊：整屏底图（保留一点页面结构感，才看得出"这是被糊过的日志页"）
            int r1 = Math.max(2, Math.min(smallW, smallH) / 26);
            AuroraView.blurH(a, b, smallW, smallH, r1);
            AuroraView.blurV(b, a, smallW, smallH, r1);
            AuroraView.blurH(a, b, smallW, smallH, r1);
            AuroraView.blurV(b, a, smallW, smallH, r1);
            base.setPixels(a, 0, smallW, 0, 0, smallW, smallH);
            this.soft = base;
            // 重糊：卡片内芯（在轻糊的基础上再糊两遍、半径更大 → 色雾级）
            Bitmap another = Bitmap.createBitmap(smallW, smallH, Bitmap.Config.ARGB_8888);
            int r2 = Math.max(3, Math.min(smallW, smallH) / 14);
            AuroraView.blurH(a, b, smallW, smallH, r2);
            AuroraView.blurV(b, a, smallW, smallH, r2);
            AuroraView.blurH(a, b, smallW, smallH, r2);
            AuroraView.blurV(b, a, smallW, smallH, r2);
            another.setPixels(a, 0, smallW, 0, 0, smallW, smallH);
            this.heavy = another;
        } catch (Throwable th) {
            // 模糊失败不算失败：底图还是那张 1/6 的清晰小图，观感退化成"磨砂但不糊"
            this.soft = base;
            this.heavy = null;
        }
    }

    private void build() {
        FrameLayout root = new FrameLayout(this.activity);
        root.setClickable(true);
        // 点空白 = 取消（和 ConfirmDialog 一致；卡片自己可点，吞掉卡内点击）
        root.setOnClickListener(v -> cancel());
        this.root = root;

        this.backdrop = new Backdrop(this.activity);
        root.addView(this.backdrop, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // stage 只负责"整体上移让键盘"（翻译它，不翻译卡片 —— 卡片的 translationY 归入场动画用）
        FrameLayout stage = new FrameLayout(this.activity);
        stage.setClipChildren(false);
        this.stage = stage;
        root.addView(stage, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout card = new LinearLayout(this.activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setClickable(true);
        // v1.10.511（用户「稍微改小几 dp」）：只收尺寸 —— 内边距 / 行距 / 按钮高 / 卡片宽
        // 各收 2~4dp，层序、颜色、圆角、玻璃材质一个字都没动。
        int padH = (int) (this.dp * 20.0f);
        card.setPadding(padH, (int) (this.dp * 18.0f), padH, (int) (this.dp * 14.0f));
        FrostedGlassDrawable glass = new FrostedGlassDrawable(
                this.colors.getSurface(), 26.0f * this.dp, 1.2f * this.dp, this.dark,
                this.dark ? SURFACE_ALPHA_DARK : SURFACE_ALPHA_LIGHT, card);
        glass.setFrost(this.heavy, this.originX, this.originY, this.scaleX, this.scaleY);
        this.surface = glass;
        card.setBackground(glass);
        card.setElevation(this.dp * 20.0f);
        this.card = card;

        // ① 标题（与 ConfirmDialog 同一字号/字重）
        if (this.title != null && this.title.length() > 0) {
            TextView titleView = new TextView(this.activity);
            titleView.setText(this.title);
            titleView.setTextSize(17.0f);
            titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            titleView.setTextColor(this.colors.getText());
            card.addView(titleView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        // ② 输入框：走全应用统一的「圆角淡底输入框」（NumDialog 那一套），
        //    不用下划线 —— 玻璃面上一条 1dp 的主色线会被背景糊掉，看着像没画。
        EditText field = new EditText(this.activity);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setSingleLine(true);
        if (this.hint != null) {
            field.setHint(this.hint);
        }
        field.setTextSize(16.0f);
        field.setTextColor(this.colors.getText());
        field.setHintTextColor(withAlpha(this.colors.getTextMuted(), 0.92f));
        GradientDrawable fieldBg = new GradientDrawable();
        fieldBg.setColor(withAlpha(this.colors.getNeutral(), this.dark ? 0.55f : 0.85f));
        fieldBg.setCornerRadius(14.0f * this.dp);
        field.setBackground(fieldBg);
        field.setPadding((int) (this.dp * 14.0f), (int) (this.dp * 11.0f),
                (int) (this.dp * 14.0f), (int) (this.dp * 11.0f));
        if (this.keptText != null) {
            field.setText(this.keptText);
            field.setSelection(field.getText().length());
            this.keptText = null;
        }
        LinearLayout.LayoutParams fieldLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fieldLp.topMargin = (int) (this.dp * 14.0f);
        card.addView(field, fieldLp);
        this.input = field;

        // ③ 两颗胶囊按钮（中性淡底 / 主色实底，按下 0.965）
        LinearLayout row = new LinearLayout(this.activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight((int) (this.dp * 56.0f));   // 44 + 6×2
        row.setClipChildren(false);
        row.setClipToPadding(false);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (this.dp * 56.0f));
        rowLp.topMargin = (int) (this.dp * 12.0f);
        card.addView(row, rowLp);

        String cancelLabel = this.cancelText != null ? this.cancelText.toString()
                : this.activity.getString(R.string.cancel);
        String okLabel = this.confirmText != null ? this.confirmText.toString()
                : this.activity.getString(android.R.string.ok);

        Button cancelButton = pill(cancelLabel, this.colors.getText(),
                withAlpha(this.colors.getNeutral(), 0.85f));
        cancelButton.setOnClickListener(v -> cancel());
        row.addView(cancelButton, new LinearLayout.LayoutParams(
                0, (int) (this.dp * 44.0f), 1.0f));

        Button ok = pill(okLabel, this.colors.getOnPrimary(), this.colors.getPrimary());
        ok.setOnClickListener(v -> submit());
        LinearLayout.LayoutParams okLp = new LinearLayout.LayoutParams(
                0, (int) (this.dp * 44.0f), 1.0f);
        okLp.setMarginStart((int) (this.dp * 10.0f));
        row.addView(ok, okLp);

        // 按钮投影落在行内留白里（LESSONS v1.10.393/394：这行 wrap_content 会把投影切平）
        cancelButton.setElevation(2.0f * this.dp);
        ok.setElevation(2.0f * this.dp);
        ThemeManager.INSTANCE.exposeShadow(cancelButton);
        ThemeManager.INSTANCE.exposeShadow(ok);

        Fonts.markNoScale(root);   // 日志页不参与字号缩放，弹窗跟着不缩

        int cardWidth = (int) Math.min(
                this.activity.getResources().getDisplayMetrics().widthPixels * 0.84f,
                this.dp * 352.0f);
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(
                cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.gravity = Gravity.CENTER;
        stage.addView(card, cardLp);

        installIme(root);
    }

    /**
     * 键盘避让（v1.10.509 的核心之一）。
     *
     * <p>508 用 {@code SOFT_INPUT_ADJUST_PAN}：键盘一弹，窗口整个上移一个键盘的高度，
     * 卡片从"屏幕中间"飞到"屏幕上半截"—— 用户说"不跟键盘腾位"就是指这个。</p>
     *
     * <p>现在窗口**一个像素都不动**（{@code SOFT_INPUT_ADJUST_NOTHING}），只把卡片（stage）
     * 上移到"卡片下沿刚好离开键盘 12dp"的位置：键盘 300dp 时如果卡片下面本来已经空了 290dp，
     * 那只需要上移 22dp。位移量随键盘高度连续变化，看起来才是"跟着键盘"。</p>
     *
     * <p>两个信号取大的：API 30+ 的 {@code ime()} inset 是唯一权威来源；
     * 老系统 / 某些 ROM 不给这个 inset，就用"可见区域少了多少"兜底（经典做法）。
     * 两者都小于 80dp 一律当 0 —— 导航栏才 48dp，别把它当成键盘。</p>
     */
    private void installIme(@NotNull final FrameLayout root) {
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                onIme(insets.getInsets(WindowInsets.Type.ime()).bottom);
                return insets;
            });
        }
        // 兜底：布局一变就量一次"可见区域被吃掉了多少"
        final ViewTreeObserver.OnGlobalLayoutListener listener =
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        FrameLayout view = GlassPrompt.this.root;
                        if (view == null || view.getHeight() <= 0) {
                            return;
                        }
                        Rect frame = new Rect();
                        view.getWindowVisibleDisplayFrame(frame);
                        int missing = view.getHeight() - frame.height();
                        if (missing >= GlassPrompt.this.dp * 80.0f) {
                            if (missing > GlassPrompt.this.imeHeight) {
                                onIme(missing);
                            }
                        } else if (Build.VERSION.SDK_INT < 30) {
                            // 没有 ime() inset 的老系统：键盘收起来时要有人把位移还回去
                            onIme(0);
                        }
                    }
                };
        root.getViewTreeObserver().addOnGlobalLayoutListener(listener);
    }

    private void onIme(int px) {
        if (px < 0) {
            px = 0;
        }
        if (px < this.dp * 80.0f) {
            px = 0;   // 导航栏 / 状态栏，不是键盘
        }
        if (px == this.imeHeight) {
            return;
        }
        this.imeHeight = px;
        applyShift();
    }

    private void applyShift() {
        FrameLayout stage = this.stage;
        FrameLayout rootView = this.root;
        LinearLayout box = this.card;
        if (stage == null || rootView == null || box == null) {
            return;
        }
        int windowH = rootView.getHeight();
        int cardH = box.getHeight();
        float shift = 0.0f;
        if (this.imeHeight > 0 && windowH > 0 && cardH > 0) {
            // 卡片居中：下沿到窗口底的距离就是 (H - h)/2
            float below = (windowH - cardH) / 2.0f;
            float need = this.imeHeight - below + this.dp * 12.0f;
            float limit = Math.max(0.0f, (windowH - cardH) / 2.0f - this.dp * 10.0f);
            shift = Math.max(0.0f, Math.min(need, limit));
        }
        stage.animate().translationY(-shift).setDuration(220L)
                .setInterpolator(new DecelerateInterpolator(1.6f)).start();
    }

    private void showWindow() {
        FrameLayout rootView = this.root;
        if (rootView == null) {
            return;
        }
        Dialog window = new Dialog(this.activity, ThemeManager.INSTANCE.dialogTheme(this.activity));
        this.dialog = window;
        window.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ThemeManager.INSTANCE.clearDialogWindow(window);
        window.setContentView(rootView);
        window.setCanceledOnTouchOutside(false);
        window.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                cancel();
                return true;
            }
            return false;
        });
        window.setOnDismissListener(d -> release());

        Window win = window.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(0));
            win.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            try {
                // 非浮窗窗口默认还会给两条系统栏留内边距 → 模糊底断在栏上（v1.10.416 的教训）
                if (Build.VERSION.SDK_INT >= 30) {
                    win.setDecorFitsSystemWindows(false);
                    WindowManager.LayoutParams lp = win.getAttributes();
                    lp.setFitInsetsTypes(0);
                    lp.setFitInsetsSides(0);
                    lp.layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                    win.setAttributes(lp);
                } else {
                    win.getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
                }
                win.setStatusBarColor(Color.TRANSPARENT);
                win.setNavigationBarColor(Color.TRANSPARENT);
                if (Build.VERSION.SDK_INT >= 29) {
                    win.setNavigationBarContrastEnforced(false);
                }
            } catch (Throwable ignored) {
            }
            win.setDimAmount(0.0f);   // 暗是我们自己压的（底图 + scrim），系统别再叠一层
            win.setGravity(Gravity.CENTER);
            // 窗口不许自己动 —— 键盘避让由 applyShift() 负责
            win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            try {
                win.setWindowAnimations(0);   // 系统那 150ms fade+scale 会和我们自己的曲线打架
            } catch (Throwable ignored) {
            }
        }
        window.show();
        Window shown = window.getWindow();
        if (shown != null) {
            // 有的 ROM 在 show() 里按主题宽高覆盖一次，show 之后再钉一遍
            shown.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
        // 底图在屏幕上的原点：卡片内芯要按"卡片此刻的屏幕矩形"去采样，原点必须是对的
        rootView.post(() -> {
            pinOrigin();
            EditText field = this.input;
            if (field != null) {
                field.requestFocus();
            }
        });
    }

    /**
     * 记下底图左上角在**屏幕**上的位置（v1.10.509）。
     *
     * <p>拍的是 Activity 的 decorView，画的是另一个窗口（Dialog）—— 两个窗口都可能不在 (0,0)。
     * 直接假设 (0,0) 的后果是卡片内芯采样到旁边那一块：糊的，看不出来错位，
     * 但"卡片里比外面更糊"这个唯一的玻璃特征就没了。</p>
     */
    private void pinOrigin() {
        Window window = this.activity.getWindow();
        View source = window != null ? window.getDecorView() : null;
        if (source == null) {
            return;
        }
        int[] loc = new int[2];
        try {
            source.getLocationOnScreen(loc);
        } catch (Throwable th) {
            return;
        }
        this.originX = loc[0];
        this.originY = loc[1];
        FrostedGlassDrawable glass = this.surface;
        if (glass != null) {
            glass.setFrost(this.heavy, this.originX, this.originY, this.scaleX, this.scaleY);
        }
        applyShift();
    }

    // ------------------------------------------------------------------ 动效

    private void playIn() {
        final View back = this.backdrop;
        final View box = this.card;
        if (back != null) {
            back.setAlpha(0.0f);
        }
        if (box == null) {
            return;
        }
        final float lift = this.dp * 18.0f;
        box.setAlpha(0.0f);
        box.setScaleX(0.92f);
        box.setScaleY(0.92f);
        box.setTranslationY(lift);
        box.post(() -> {
            View backdropView = GlassPrompt.this.backdrop;
            if (backdropView != null) {
                backdropView.animate().alpha(1.0f).setDuration(220L).start();
            }
            android.animation.ValueAnimator animator =
                    android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
            animator.setDuration(300L);
            animator.setInterpolator(null);
            animator.addUpdateListener(animation -> {
                float t = Ease.IN_OUT.at(animation.getAnimatedFraction());
                if (t < 0.0f) {
                    t = 0.0f;
                } else if (t > 1.0f) {
                    t = 1.0f;
                }
                box.setAlpha(t);
                box.setScaleX(0.92f + 0.08f * t);
                box.setScaleY(0.92f + 0.08f * t);
                box.setTranslationY(lift * (1.0f - t));
            });
            animator.start();
        });
    }

    private void playOut(@NotNull final Runnable after) {
        final View box = this.card;
        final View back = this.backdrop;
        if (box == null) {
            after.run();
            return;
        }
        final float lift = this.dp * 10.0f;
        if (back != null) {
            back.animate().alpha(0.0f).setDuration(180L).start();
        }
        android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.setDuration(190L);
        animator.setInterpolator(null);
        animator.addUpdateListener(animation -> {
            float t = Ease.OUT.at(animation.getAnimatedFraction());
            if (t < 0.0f) {
                t = 0.0f;
            } else if (t > 1.0f) {
                t = 1.0f;
            }
            box.setAlpha(1.0f - t);
            box.setScaleX(1.0f - 0.05f * t);
            box.setScaleY(1.0f - 0.05f * t);
            box.setTranslationY(lift * t);
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            private boolean done;

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (this.done) {
                    return;
                }
                this.done = true;
                after.run();
            }
        });
        animator.start();
    }

    // ------------------------------------------------------------------ 收尾

    private void submit() {
        if (this.closing) {
            return;
        }
        EditText field = this.input;
        String text = field != null && field.getText() != null
                ? field.getText().toString() : "";
        OnSubmit listener = this.onSubmit;
        boolean close = true;
        if (listener != null) {
            try {
                close = listener.onSubmit(text);
            } catch (Throwable th) {
                CrashLog.save(this.activity, null, th);
                close = true;
            }
        }
        if (close) {
            closeWithAnimation(null);
        }
    }

    /** 点「取消」/ 点空白 / 返回键。 */
    void cancel() {
        if (this.closing) {
            return;
        }
        closeWithAnimation(this.onCancel);
    }

    private void closeWithAnimation(@Nullable final Runnable after) {
        if (this.closing) {
            return;
        }
        this.closing = true;
        playOut(() -> {
            dismiss();
            if (after != null) {
                after.run();
            }
        });
    }

    void dismiss() {
        Dialog window = this.dialog;
        this.dialog = null;
        if (window != null) {
            try {
                window.dismiss();
            } catch (Throwable ignored) {
            }
        }
        release();
    }

    /** 关窗后把两张全屏底图还回去（1/6 小图各 ~0.5MB，一直挂着会被算成"大对象"）。 */
    private void release() {
        Bitmap a = this.soft;
        this.soft = null;
        if (a != null && !a.isRecycled()) {
            a.recycle();
        }
        Bitmap b = this.heavy;
        this.heavy = null;
        FrostedGlassDrawable glass = this.surface;
        this.surface = null;
        if (glass != null) {
            glass.setFrost(null, 0, 0, 1.0f, 1.0f);
        }
        if (b != null && !b.isRecycled()) {
            b.recycle();
        }
        this.input = null;
        this.card = null;
        this.stage = null;
        this.backdrop = null;
        this.root = null;
        this.imeHeight = 0;
        Runnable closed = this.onClosed;
        this.onClosed = null;
        if (closed != null) {
            try {
                closed.run();
            } catch (Throwable ignored) {
            }
        }
    }

    // ------------------------------------------------------------------ 换肤

    /**
     * 系统深浅色变了：把已经输入的口令接住，整块拆了重开。
     *
     * <p>不重开的话，卡片颜色 / 底图 / 描边全停在旧模式（NumDialog 在 v1.10.394 栽过同样的坑），
     * 用户看到的是"切了黑白，弹窗还是白的"。{@code quietNext} 保证这次不播入场动画 ——
     * 只是换色，不该像"又弹了一次"。</p>
     *
     * <p>v1.10.512：调用方必须**等页面换完色**再进来（LogActivity 里排到 applyTheme() 之后）。
     * 本方法一进来就 {@link #capture()} 截整屏底图 —— 早一步截到的就是旧主题的页面，
     * 结果就是用户报的「切系统模式，卡片变了、背景没更新」。</p>
     */
    void onSystemThemeChanged() {
        if (this.closing || this.rebuilding) {
            return;
        }
        Dialog window = this.dialog;
        if (window == null || !window.isShowing()) {
            return;
        }
        if (this.activity.isFinishing()
                || (Build.VERSION.SDK_INT >= 17 && this.activity.isDestroyed())) {
            dismiss();
            return;
        }
        this.rebuilding = true;
        try {
            EditText field = this.input;
            this.keptText = field != null && field.getText() != null
                    ? field.getText().toString() : null;
            window.setOnDismissListener(null);
            window.setOnKeyListener(null);
            Dialog old = this.dialog;
            this.dialog = null;
            this.input = null;
            this.card = null;
            this.stage = null;
            this.backdrop = null;
            this.surface = null;
            try {
                old.dismiss();
            } catch (Throwable ignored) {
            }
            Bitmap a = this.soft;
            this.soft = null;
            if (a != null && !a.isRecycled()) {
                a.recycle();
            }
            Bitmap b = this.heavy;
            this.heavy = null;
            if (b != null && !b.isRecycled()) {
                b.recycle();
            }
            this.quietNext = true;
            this.closing = false;
        } finally {
            this.rebuilding = false;
        }
        // 底图与卡片都在 show() 里按**当前**主题重新拍/重新取色
        show();
    }

    // ------------------------------------------------------------------ 小工具

    /**
     * 背景：整屏 1/6 模糊图 + 一层很淡的压暗。
     *
     * <p>压暗用 {@code drawColor} 一层而不是两层（v1.10.140 的教训：截不到图时叠两层会暗一倍）。</p>
     */
    private final class Backdrop extends View {
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        private final Rect dst = new Rect();
        private final int scrim;

        Backdrop(@NotNull Context context) {
            super(context);
            this.scrim = GlassPrompt.this.dark ? 0x4A000000 : 0x2A000000;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            Bitmap bitmap = GlassPrompt.this.soft;
            int w = getWidth();
            int h = getHeight();
            if (bitmap != null && !bitmap.isRecycled() && w > 0 && h > 0) {
                this.dst.set(0, 0, w, h);
                canvas.drawBitmap(bitmap, null, this.dst, this.paint);
            }
            canvas.drawColor(this.scrim);
        }
    }

    /** 胶囊按钮：圆角 999、按下 0.965 回弹（全应用同一套）。 */
    @NotNull
    private Button pill(@NotNull String label, int textColor, int backgroundColor) {
        Button button = new Button(this.activity);
        button.setText(label);
        button.setTextSize(14.0f);
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setPadding((int) (this.dp * 8.0f), 0, (int) (this.dp * 8.0f), 0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setBackground(ThemeManager.INSTANCE.rounded(this.activity, backgroundColor, 999.0f));
        button.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.965f).scaleY(0.965f).setDuration(90L).start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150L).start();
            }
            return false;
        });
        return button;
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * alpha);
        if (a <= 0) {
            a = 1;
        }
        return (a << 24) | (color & 0x00FFFFFF);
    }
}
