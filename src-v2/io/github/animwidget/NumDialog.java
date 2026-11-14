package io.github.animwidget;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v1.10.70：**全应用统一的「键盘输入」对话框**（图2 那个样子）。
 *
 * 结构：圆角卡片 = 标题 + 圆角输入框 + 紧凑居中的「取消 / 确认」。
 * 以前各处各写一套（AlertDialog + 裸 EditText，按钮还占满整行、离中间很远），
 * 现在统一走这里：数字（清晰度/帧率/角度）、小数（对比度）、文本都能用。
 *
 * 用法：
 * <pre>
 *   new NumDialog(this, "旋转角度", "45", "-360 ~ 360", InputType.TYPE_CLASS_NUMBER | TYPE_NUMBER_FLAG_SIGNED)
 *       .addTop(labelOrSeekBar)      // 可选：上方附加控件
 *       .onOk(text -> { ... })
 *       .show();
 * </pre>
 */
final class NumDialog {

    interface OnText {
        void onText(@NotNull String text);
    }

    private final Context context;
    /** v1.10.394：不再是 final —— 切系统深浅色时要按新模式重算一遍。 */
    private ThemeColors colors;
    private final float density;
    private final Dialog dialog;
    private final LinearLayout root;
    private final EditText input;
    /** v1.10.394：卡片外面那层透明外框（只用来放投影 + 吃掉空白点击）。 */
    private final android.widget.FrameLayout shell;
    @Nullable
    private TextView titleView;
    @Nullable
    private Button cancelBtn;
    @Nullable
    private Button okBtn;
    /**
     * 卡片的基础投影高度（dp）：隔离态要按进度把它淡掉。
     *
     * <p>取 10 而不是 ConfirmDialog 的 14：这里的窗口只比卡片大 2×18dp，
     * 投影再高就会顶到窗口边界（又是「被截掉」）。10dp 的软边正好落在这 18dp 里。</p>
     */
    private static final float CARD_ELEVATION_DP = 10.0f;
    /** 外框留白（dp）：投影要画在这圈里，窗口因此比卡片宽/高 2 倍这个值。 */
    private static final int SHADOW_PAD_DP = 18;
    /**
     * v1.10.395：**按钮行自己的留白**（dp）。
     *
     * <p>上一版只放开了父容器的裁剪开关（`clipChildren/clipToPadding = false`），
     * 真机上投影仍然在按钮的下沿被切平 —— 也就是说在那个 Dialog 窗口里，
     * 「放开裁剪」这条路没走通（原因还没定位：可能被窗口层重新套上了）。
     * 换一条不依赖开关的路：把按钮行的**内边距**撑大，投影就落在行自己的界限里，
     * 谁裁剪都切不到它。为了让按钮位置一个像素都不动，行下方的 topMargin 与卡片
     * 底部内边距同步减掉同样的值（12 + 6 = 18，和原来一样）。</p>
     */
    private static final int SHADOW_RING_DP = 6;
    /** 上一次「是不是深色」，用来判断系统模式真的变了。 */
    private boolean lastDark;
    /** 主题变化后，调用方附加视图（角度文字 / 滑条）的重新上色钩子。 */
    private final java.util.List<Runnable> recolorHooks = new java.util.ArrayList<>();
    private final Runnable themePoll = new Runnable() {
        @Override
        public void run() {
            try {
                boolean dark = ThemeManager.INSTANCE.isDark(NumDialog.this.context);
                if (dark != NumDialog.this.lastDark) {
                    NumDialog.this.lastDark = dark;
                    NumDialog.this.applyPalette();
                }
            } catch (Throwable ignored) {
            }
            NumDialog.this.root.postDelayed(this, 500L);
        }
    };
    @Nullable
    private OnText onOk;

    // ---- v1.10.214：拖动滑块时的「只留滑块」隔离态 ----
    /** 拖动期间要保持不透明的控件（旋转对话框的「角度 N°」+ 滑条）。空 = 不启用隔离态。 */
    @Nullable
    private View[] isolateKeeps;
    @Nullable
    private android.animation.ValueAnimator isolateAnim;
    /** 0 = 正常显示，1 = 内容全透明（只剩滑条）。 */
    private float isolateT;
    /** 正常态的窗口灰滤镜强度（show() 时从系统读，-1 = 还没读到）。 */
    private float dimBase = -1.0f;
    @Nullable
    private Runnable onClosed;

    NumDialog(@NotNull Context context, @NotNull String title, @NotNull String initial,
              @Nullable String hint, int inputType) {
        this.context = context;
        this.colors = ThemeManager.INSTANCE.colors(context);
        this.density = context.getResources().getDisplayMetrics().density;

        this.dialog = new Dialog(context);
        this.dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // v1.10.71：窗口底色透明 —— 否则圆角卡片四周会露出系统方底（白角）
        ThemeManager.INSTANCE.clearDialogWindow(this.dialog);

        this.root = new LinearLayout(context);
        this.root.setOrientation(LinearLayout.VERTICAL);
        int padH = px(20);
        this.root.setPadding(padH, px(16), padH, px(18 - SHADOW_RING_DP));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(this.colors.getSurface());
        bg.setCornerRadius(22.0f * this.density);
        this.root.setBackground(bg);

        TextView titleView = new TextView(context);
        this.titleView = titleView;
        titleView.setText(title);
        titleView.setTextSize(18.0f);
        titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleView.setTextColor(this.colors.getText());
        this.root.addView(titleView);

        this.input = new EditText(context);
        this.input.setInputType(inputType);
        this.input.setText(initial);
        if (hint != null) {
            this.input.setHint(hint);
        }
        this.input.setSingleLine(true);
        this.input.setTextSize(16.0f);
        this.input.selectAll();
        this.input.setTextColor(this.colors.getText());
        this.input.setHintTextColor(this.colors.getTextMuted());
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(this.colors.getNeutral());
        inputBg.setCornerRadius(14.0f * this.density);
        this.input.setBackground(inputBg);
        this.input.setPadding(px(14), px(12), px(14), px(12));
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(-1, -2);
        inputLp.topMargin = px(14);
        this.root.addView(this.input, inputLp);

        // v1.10.394：卡片外面再包一层**透明外框**。
        //
        // 原因：这个弹窗的窗口以前是 `窗口 == 卡片`（show() 里 setLayout(width, WRAP_CONTENT)），
        // 窗口边界就是卡片边界 —— 卡片的投影**一个像素都画不出来**（不是被截断，是根本没地方画），
        // 所以浅色下这张白卡片是「贴」在背景上的硬边。现在窗口比卡片大一圈（外框留白），
        // 卡片自己也给了 elevation，投影就画在这圈留白里；卡片宽度 = 窗口宽 - 2×留白，
        // 所以**卡片尺寸和按钮位置一个像素都没动**。
        this.shell = new android.widget.FrameLayout(context);
        this.shell.setClipChildren(false);
        this.shell.setClipToPadding(false);
        this.shell.setPadding(px(SHADOW_PAD_DP), px(SHADOW_PAD_DP), px(SHADOW_PAD_DP), px(SHADOW_PAD_DP));
        this.shell.addView(this.root, new android.widget.FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        this.root.setElevation(CARD_ELEVATION_DP * this.density);
        // 卡片自己要吃掉空白处的点击（否则会冒泡到外框，外框点一下等于取消）
        this.root.setClickable(true);
        // 外框 = 原来「窗口外面的那圈」：点它就取消（以前窗口和卡片一样大，
        // 点外面是系统按"窗口外"处理的；现在窗口大了一圈，得自己接住）
        this.shell.setClickable(true);
        this.shell.setOnClickListener(v -> this.dialog.dismiss());
        // v1.10.214：内容视图延后到 show() 里挂（隔离态需要在挂之前决定包不包外层）。
        this.dialog.setContentView(this.shell);

        // v1.10.173（反馈「字体大小有些页面根本没跟随」）：这是个**独立 Dialog**，
        // 不在 Activity 的视图树里 —— 不主动铺一次，它永远是系统字体、也不吃字号档位。
        // （ConfirmDialog / RewardSheet 早就有这一行，NumDialog / MaskGestureDialog 漏了。）
        Fonts.applyTree(this.root);
    }

    /** 在输入框下面追加控件（如旋转用的 SeekBar 就放上面，用 addTop）。 */
    /** 输入框本身（旋转对话框要把 SeekBar 的进度同步进去）。 */
    @NotNull
    final EditText input() {
        return this.input;
    }

    @NotNull
    public final NumDialog addTop(@NotNull View view) {
        // 插在标题之后、输入框之前
        this.root.addView(view, 1);
        return this;
    }

    /**
     * v1.10.214：拖动 [keep] 时，卡片底色和其它子视图淡出、窗口灰滤镜撤掉，
     * 只留 [keep]（滑条/拇指）保持原透明度；松手后原样恢复。
     *
     * <p>实现要点（都是"不能卡、不能跳"的）：</p>
     * <ul>
     *   <li>只改 alpha 与窗口 dimAmount，**不改布局** —— 滑条位置一帧都不会动，不会触发重排；</li>
     *   <li>进度只用一个 float [isolateT]，动画永远从**当前值**接续；反向打断时按
     *       |差值| 缩短时长（等速感），所以连按十次也不会跳；</li>
     *   <li>窗口仍然是触摸模态的（只有宽 × wrap 的窗口，但没有 NOT_TOUCH_MODAL），
     *       所以背景虽然露出来，却依然**不可交互**。</li>
     * </ul>
     */
    @NotNull
    public final NumDialog isolateWhileDrag(@NotNull View... keeps) {
        this.isolateKeeps = keeps;
        return this;
    }

    /** v1.10.214：对话框关闭时回调（无论确认还是取消）。旋转对话框用它做「取消＝撤回实时预览」。 */
    @NotNull
    public final NumDialog onDismiss(@Nullable Runnable listener) {
        this.onClosed = listener;
        return this;
    }

    /** 开始/结束拖动（滑条的 onStartTrackingTouch / onStopTrackingTouch 调用）。 */
    public final void setDragging(boolean dragging) {
        float target = dragging ? 1.0f : 0.0f;
        if (Math.abs(this.isolateT - target) < 0.004f) {
            applyIsolate(target);
            return;
        }
        android.animation.ValueAnimator running = this.isolateAnim;
        if (running != null) {
            running.cancel();
            this.isolateAnim = null;
        }
        final float from = this.isolateT;
        long full = dragging ? 150L : 240L;
        android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofFloat(from, target);
        anim.setDuration(Math.max(60L, (long) (full * Math.abs(target - from))));
        anim.setInterpolator(dragging
                ? new android.view.animation.DecelerateInterpolator()
                : new android.view.animation.AccelerateDecelerateInterpolator());
        anim.addUpdateListener(a -> applyIsolate(((Float) a.getAnimatedValue()).floatValue()));
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator a) {
                if (NumDialog.this.isolateAnim == a) {
                    NumDialog.this.isolateAnim = null;
                }
            }
        });
        this.isolateAnim = anim;
        anim.start();
    }

    /** 这个子视图是不是「拖动期间必须留着」的那几个（角度文字 + 滑条）。 */
    private static boolean isKept(@Nullable View[] keeps, @NotNull View child) {
        if (keeps == null) {
            return false;
        }
        for (View keep : keeps) {
            if (keep == child) {
                return true;
            }
        }
        return false;
    }

    /** 把隔离进度套到视图上：内容 alpha = 1-t，灰滤镜 = base×(1-t)，keeps 恒为 1。 */
    private void applyIsolate(float t) {
        this.isolateT = t;
        float content = 1.0f - t;
        android.graphics.drawable.Drawable bg = this.root.getBackground();
        if (bg != null) {
            bg.setAlpha(Math.round(255 * content));
        }
        // v1.10.394：投影也要跟着卡片一起淡掉 —— 否则拖动滑条时卡片没了，
        // 半空中还浮着一圈阴影，看起来像脏点。
        this.root.setElevation(CARD_ELEVATION_DP * this.density * content);
        View[] keeps = this.isolateKeeps;
        for (int i = 0; i < this.root.getChildCount(); i++) {
            View child = this.root.getChildAt(i);
            child.setAlpha(isKept(keeps, child) ? 1.0f : content);
        }
        Window window = this.dialog.getWindow();
        if (window != null && this.dimBase >= 0.0f) {
            window.setDimAmount(this.dimBase * content);
        }
    }

    @NotNull
    public final NumDialog onOk(@NotNull OnText listener) {
        this.onOk = listener;
        return this;
    }

    /** 小数键盘（对比度那种）。 */
    static int decimalInputType() {
        return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED;
    }

    /** 整数键盘（可为负）。 */
    static int signedIntInputType() {
        return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;
    }

    void show() {
        LinearLayout row = new LinearLayout(this.context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);   // 紧凑居中，不再贴到左右两边
        // v1.10.395：行内留一圈给投影（见 SHADOW_RING_DP）；同时把两个裁剪开关也关掉，
        // 两条路一起走。行高 = 50 + 6 + 6，topMargin 18→12，卡片底部 18→12，
        // 于是按钮位置和卡片高度都和上一版完全一致。
        row.setMinimumHeight(px(50 + SHADOW_RING_DP * 2));
        row.setClipChildren(false);
        row.setClipToPadding(false);
        // 高度写死成「按钮高 + 两圈留白」——不靠 wrap_content + minimumHeight 的推导，
        // 免得某个 ROM 的 LinearLayout 忽略 suggested minimum 又把这圈吃回去。
        LinearLayout.LayoutParams rowLp =
                new LinearLayout.LayoutParams(-1, px(50 + SHADOW_RING_DP * 2));
        rowLp.topMargin = px(18 - SHADOW_RING_DP);
        this.root.addView(row, rowLp);

        Button cancel = new Button(this.context);
        cancel.setText(R.string.cfg_cancel);
        cancel.setAllCaps(false);
        cancel.setTextSize(14.0f);
        cancel.setTextColor(this.colors.getText());
        cancel.setBackground(ThemeManager.INSTANCE.outlined(this.context, 0, this.colors.getNeutral(), 999.0f));
        cancel.setMinWidth(px(116));
        cancel.setPadding(px(26), 0, px(26), 0);
        cancel.setOnClickListener(v -> this.dialog.dismiss());
        row.addView(cancel, new LinearLayout.LayoutParams(-2, px(50)));

        Button ok = new Button(this.context);
        ok.setText(R.string.settings_scheme_confirm);
        ok.setAllCaps(false);
        ok.setTextSize(14.0f);
        ok.setTextColor(this.colors.getOnPrimary());
        ok.setBackground(ThemeManager.INSTANCE.rounded(this.context, this.colors.getPrimary(), 999.0f));
        ok.setMinWidth(px(116));
        ok.setPadding(px(26), 0, px(26), 0);
        LinearLayout.LayoutParams okLp = new LinearLayout.LayoutParams(-2, px(50));
        okLp.leftMargin = px(12);
        ok.setOnClickListener(v -> {
            OnText listener = this.onOk;
            String text = this.input.getText() != null ? this.input.getText().toString() : "";
            // v1.10.214：**先回调再关窗**。以前是「先 dismiss 再回调」，而 dismiss 会先触发
            // onDismissListener —— 旋转对话框在 onDismiss 里做「取消＝撤回」，于是「确认」
            // 会先被当成取消撤回一次、再被确认写回一次（同一帧内，肉眼看不见，但语义是错的）。
            if (listener != null) {
                listener.onText(text);
            }
            this.dialog.dismiss();
        });
        row.addView(ok, okLp);
        this.cancelBtn = cancel;
        this.okBtn = ok;
        // v1.10.393：行宽高都正好等于按钮，投影四边都会被行切掉（见 ThemeManager.exposeShadow）。
        // v1.10.394：这两个按钮是 `new Button(context)` 现建的，根本没进过换肤链路，
        // Material 那 2dp 投影其实**从来没出现过**（stateListAnimator 没人替它跑）。
        // 这里按 Material 的标准值显式给回来，再放开裁剪 —— 四边都是完整软边。
        cancel.setElevation(2.0f * this.density);
        ok.setElevation(2.0f * this.density);
        ThemeManager.INSTANCE.exposeShadow(cancel);
        ThemeManager.INSTANCE.exposeShadow(ok);

        // v1.10.176：按钮是**加在这里**的，而构造器末尾已经 applyTree 过一次 ——
        // 那时它们还不存在，所以取消/确认一直用系统字体（选了字体也不换）。
        // 在 show() 里把整棵树再铺一遍（幂等，代价可忽略）。
        Fonts.applyTree(this.root);

        this.dialog.show();
        try {
            Window window = this.dialog.getWindow();
            if (window != null) {
                int screen = this.context.getResources().getDisplayMetrics().widthPixels;
                int card = Math.min((int) (screen * 0.88f), px(360));
                // 窗口 = 卡片 + 两侧投影留白（卡片宽度因此和以前完全一致）
                int win = Math.min(card + px(SHADOW_PAD_DP * 2), screen - px(6));
                window.setLayout(win, WindowManager.LayoutParams.WRAP_CONTENT);
            }
        } catch (Throwable ignored) {
        }
        // v1.10.214：隔离态要能撤掉窗口的灰滤镜 —— 先把基准值读出来（系统当前用了多少就记多少）
        if (this.isolateKeeps != null && this.isolateKeeps.length > 0) {
            try {
                Window window = this.dialog.getWindow();
                if (window != null) {
                    WindowManager.LayoutParams lp = window.getAttributes();
                    if ((lp.flags & WindowManager.LayoutParams.FLAG_DIM_BEHIND) == 0) {
                        lp.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                        lp.dimAmount = 0.6f;
                        window.setAttributes(lp);
                    }
                    this.dimBase = lp.dimAmount > 0.01f ? lp.dimAmount : 0.6f;
                }
            } catch (Throwable ignored) {
            }
            applyIsolate(this.isolateT);
        }
        // v1.10.394：跟随系统深浅色。
        // 这个弹窗是**独立窗口**，不在 Activity 的视图树里 —— 页面那边 500ms 轮询换肤时
        // 它一动不动（内容旋转 / 蒙版旋转 / 对比度这几个弹窗就是这么"卡"在旧模式里的）。
        // 所以它自己也要轮询：模式一变就按新模式重铺一遍配色。
        this.lastDark = ThemeManager.INSTANCE.isDark(this.context);
        this.root.postDelayed(this.themePoll, 500L);
        this.dialog.setOnDismissListener(d -> {
            try {
                this.root.removeCallbacks(this.themePoll);
            } catch (Throwable ignored) {
            }
            android.animation.ValueAnimator running = this.isolateAnim;
            if (running != null) {
                running.cancel();
                this.isolateAnim = null;
            }
            Runnable closed = this.onClosed;
            if (closed != null) {
                closed.run();
            }
        });
    }

    /**
     * v1.10.394：按**当前**主题重铺这个弹窗的全部配色（模式变了就调一次）。
     *
     * <p>只改颜色与背景，不动任何布局与位置 —— 标题、输入框、两个按钮原地换色。</p>
     */
    private void applyPalette() {
        ThemeColors c = ThemeManager.INSTANCE.colors(this.context);
        this.colors = c;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(c.getSurface());
        bg.setCornerRadius(22.0f * this.density);
        this.root.setBackground(bg);
        TextView title = this.titleView;
        if (title != null) {
            title.setTextColor(c.getText());
        }
        this.input.setTextColor(c.getText());
        this.input.setHintTextColor(c.getTextMuted());
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(c.getNeutral());
        inputBg.setCornerRadius(14.0f * this.density);
        this.input.setBackground(inputBg);
        Button cancel = this.cancelBtn;
        if (cancel != null) {
            cancel.setTextColor(c.getText());
            cancel.setBackground(ThemeManager.INSTANCE.outlined(this.context, 0, c.getNeutral(), 999.0f));
        }
        Button ok = this.okBtn;
        if (ok != null) {
            ok.setTextColor(c.getOnPrimary());
            ok.setBackground(ThemeManager.INSTANCE.rounded(this.context, c.getPrimary(), 999.0f));
        }
        // 调用方附加的视图（旋转弹窗的「角度 N°」+ 滑条）由它自己重上色
        for (Runnable hook : this.recolorHooks) {
            try {
                hook.run();
            } catch (Throwable ignored) {
            }
        }
        // 隔离态（拖动滑条时卡片淡出）要在换底之后重新套一次，否则会跳回不透明
        applyIsolate(this.isolateT);
    }

    /**
     * v1.10.394：注册「模式变了之后、这些附加视图怎么重新上色」。
     *
     * <p>旋转弹窗把「角度 N°」文字和滑条塞进来，它们不在 NumDialog 的配色表里，
     * 只能由调用方自己按新主题刷一遍（和 ConfigActivity 其它滑条用的是同一个出口）。</p>
     */
    @NotNull
    public final NumDialog onThemeChanged(@NotNull Runnable hook) {
        this.recolorHooks.add(hook);
        return this;
    }

    private int px(int dp) {
        return (int) (dp * this.density);
    }
}
