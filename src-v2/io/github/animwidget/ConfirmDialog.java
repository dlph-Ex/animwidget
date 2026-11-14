package io.github.animwidget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v1.10.140：**全应用统一的「确认」对话框**（替换系统 AlertDialog）。
 *
 * 为什么要有它：`AlertDialog` 用的是 `android.R.style.Theme_Material_Dialog_Alert` ——
 * 一块系统灰底、直角、青色文字按钮、系统默认字体，和这个 App 的
 * 「毛玻璃卡片 + 淡底圆角块 + 自绘图形」完全不是一套东西。
 * 「恢复预设」这种破坏性操作却偏偏弹出一个 2014 年样子的方框，是最刺眼的一处。
 *
 * 视觉与动效**复用已有那套语言**（不新造风格）：
 *   · 卡片 = {@link GlassFrameDrawable}（半透明 surface + 1.2dp 渐变高光边），圆角 26dp，elevation 14dp；
 *   · 头部 = 46dp 图标砖（accent 16% 淡底 + 圆角 15dp）+ {@link GlyphView} 自绘感叹号；
 *   · 按钮 = 胶囊（取消走 neutral 淡底、确认走 accent 实底），按下 0.965 回弹；
 *   · 动效 = 背景模糊渐显 220ms + 卡片「缩放 0.92→1 + 上浮 18dp + 渐显」300ms，
 *     曲线用 {@link Ease#IN_OUT}（与设置页展开、字体列表同一根曲线）。
 *
 * 用法：
 * <pre>
 *   new ConfirmDialog(this)
 *       .title(getString(R.string.settings_reset))
 *       .message(getString(R.string.settings_reset_confirm))
 *       .confirm(getString(R.string.settings_reset))
 *       .danger(true)
 *       .onConfirm(() -> { ... })
 *       .show();
 * </pre>
 */
public final class ConfirmDialog {

    /**
     * v1.10.203：破坏性弹窗的强调色**不再硬编码** —— 取语义色里的「错误色」，
     * 跟着配色方案 / 深浅色 / 反转 / 对比度一起变。
     * 保留这个常量只是为了老调用方不炸（值 = 浅色模式下的默认错误色）。
     */
    public static final int DANGER = 0xFFB3261E;

    private final Activity activity;
    private Dialog dialog;
    private ThemeColors colors;
    private final float dp;
    private boolean dark;   // v1.10.417：换肤重建时要跟着刷新（原 final）

    @Nullable
    private CharSequence title;
    @Nullable
    private CharSequence message;
    @Nullable
    private CharSequence confirmText;
    @Nullable
    private CharSequence cancelText;
    private boolean danger;
    private int glyph = GlyphView.ALERT;
    @Nullable
    private Runnable onConfirm;
    @Nullable
    private Runnable onCancel;

    @Nullable
    private BlurBackdropView backdrop;
    @Nullable
    private View card;
    private boolean closing;
    /** v1.10.417：系统换深浅色时整块重建 —— 这一次不播入场动画（不然像"又弹了一次"）。 */
    private boolean quietNext;
    /** v1.10.417：实时跟随系统深浅色。 */
    @Nullable
    private UiModeWatcher uiMode;

    public ConfirmDialog(@NotNull Activity activity) {
        this.activity = activity;
        this.colors = ThemeManager.INSTANCE.colors(activity);
        this.dp = activity.getResources().getDisplayMetrics().density;
        this.dark = ThemeManager.INSTANCE.isDark(activity);
        this.dialog = newDialog();
    }

    /** 造一个窗口（重建换肤时会再造一个，所以不能写在构造函数里只做一次）。 */
    @NotNull
    private Dialog newDialog() {
        Dialog d = new Dialog(this.activity, ThemeManager.INSTANCE.dialogTheme(this.activity));
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ThemeManager.INSTANCE.clearDialogWindow(d);
        return d;
    }

    public ConfirmDialog title(@Nullable CharSequence value) {
        this.title = value;
        return this;
    }

    public ConfirmDialog message(@Nullable CharSequence value) {
        this.message = value;
        return this;
    }

    public ConfirmDialog confirm(@Nullable CharSequence value) {
        this.confirmText = value;
        return this;
    }

    public ConfirmDialog cancel(@Nullable CharSequence value) {
        this.cancelText = value;
        return this;
    }

    /** true = 危险操作：图标砖与确认按钮走 error 色。 */
    public ConfirmDialog danger(boolean value) {
        this.danger = value;
        return this;
    }

    public ConfirmDialog glyph(int value) {
        this.glyph = value;
        return this;
    }

    public ConfirmDialog onConfirm(@Nullable Runnable value) {
        this.onConfirm = value;
        return this;
    }

    public ConfirmDialog onCancel(@Nullable Runnable value) {
        this.onCancel = value;
        return this;
    }

    public void show() {
        if (this.activity.isFinishing()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 17 && this.activity.isDestroyed()) {
            return;
        }

        // v1.10.203：危险弹窗的强调色 / 图标砖底色 / 字色全部来自语义色（错误色 + 它的容器色）
        final int accent = this.danger ? this.colors.getError() : this.colors.getPrimary();
        final int onAccent = this.danger ? this.colors.getOnError() : this.colors.getOnPrimary();
        final int accentTile = this.danger ? this.colors.getErrorContainer()
                : withAlpha(accent, 0.16f);
        final int accentGlyph = this.danger ? this.colors.getOnErrorContainer() : accent;

        FrameLayout root = new FrameLayout(this.activity);
        root.setClickable(true);
        root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel();
            }
        });

        // 背景：把当前页面截下来糊掉（与赞赏弹窗同一个 BlurBackdropView），
        // 截不到就退化成纯 scrim —— 绝不出现「弹窗下面是黑屏」。
        this.backdrop = new BlurBackdropView(this.activity, capture(),
                this.dark ? 0x3A000000 : 0x22000000);
        this.backdrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel();
            }
        });
        root.addView(this.backdrop, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout box = new LinearLayout(this.activity);
        box.setOrientation(LinearLayout.VERTICAL);
        // v1.10.395：底部内边距 18→12，把 6dp 让给按钮行的投影留白（见 actions.setPadding）
        box.setPadding((int) (this.dp * 22.0f), (int) (this.dp * 22.0f),
                (int) (this.dp * 22.0f), (int) (this.dp * 12.0f));
        box.setClickable(true);
        box.setBackground(new GlassFrameDrawable(
                withAlpha(this.colors.getSurface(), 0.94f), 26.0f * this.dp, 1.2f * this.dp, this.dark));
        box.setElevation(this.dp * 14.0f);

        // ① 图标砖 + 标题（一行）：没有标题时只留图标，正文顶上
        LinearLayout head = new LinearLayout(this.activity);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout tile = new FrameLayout(this.activity);
        GradientDrawable tileBg = new GradientDrawable();
        tileBg.setShape(GradientDrawable.RECTANGLE);
        tileBg.setCornerRadius(15.0f * this.dp);
        tileBg.setColor(accentTile);
        tile.setBackground(tileBg);
        head.addView(tile, new LinearLayout.LayoutParams(
                (int) (this.dp * 46.0f), (int) (this.dp * 46.0f)));
        GlyphView mark = new GlyphView(this.activity);
        mark.setGlyph(this.glyph);
        mark.setTintColor(accentGlyph);
        tile.addView(mark, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (this.title != null && this.title.length() > 0) {
            TextView titleView = new TextView(this.activity);
            titleView.setText(this.title);
            titleView.setTextSize(17.0f);
            titleView.setTextColor(this.colors.getText());
            titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            tlp.setMarginStart((int) (this.dp * 14.0f));
            titleView.setLayoutParams(tlp);
            head.addView(titleView);
        }
        box.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ② 正文：左对齐贴着图标砖的左边（M3 的 dialog 就是标题/正文同一左基线）
        if (this.message != null && this.message.length() > 0) {
            TextView body = new TextView(this.activity);
            body.setText(this.message);
            body.setTextSize(14.0f);
            body.setTextColor(withAlpha(this.colors.getText(), 0.80f));
            body.setLineSpacing(this.dp * 4.0f, 1.0f);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            blp.topMargin = (int) (this.dp * 14.0f);
            body.setLayoutParams(blp);
            box.addView(body);
        }

        // ③ 按钮行：取消（淡底）/ 确认（accent 实底），等宽胶囊
        LinearLayout actions = new LinearLayout(this.activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        // v1.10.395：行内留 6dp 给 2dp 投影（上一版只放开裁剪开关，真机上投影仍在按钮
        // 下沿被切平）。行高 = 46+12，topMargin 20→14、卡片底部 18→12，按钮位置不动。
        actions.setMinimumHeight((int) (this.dp * 58.0f));   // 46 + 6×2
        actions.setClipChildren(false);
        actions.setClipToPadding(false);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (this.dp * 58.0f));
        alp.topMargin = (int) (this.dp * 14.0f);
        actions.setLayoutParams(alp);

        String cancelLabel = this.cancelText != null ? this.cancelText.toString()
                : this.activity.getString(R.string.cancel);
        String confirmLabel = this.confirmText != null ? this.confirmText.toString()
                : this.activity.getString(android.R.string.ok);

        Button cancelButton = pill(cancelLabel, this.colors.getText(), this.colors.getNeutral());
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel();
            }
        });
        actions.addView(cancelButton, new LinearLayout.LayoutParams(
                0, (int) (this.dp * 46.0f), 1.0f));

        Button confirmButton = pill(confirmLabel, onAccent, accent);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirm();
            }
        });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                0, (int) (this.dp * 46.0f), 1.0f);
        clp.setMarginStart((int) (this.dp * 10.0f));
        actions.addView(confirmButton, clp);
        box.addView(actions);
        // v1.10.393：按钮行的 wrap_content 高度正好等于按钮高度，2dp 投影的上下两边
        // 会被行的边界切平（浅色模式下最明显，就是「阴影被界面截掉」）。
        // 只放开裁剪：投影照旧画，按钮位置一个像素都不动；卡片自己有 22/18dp 内边距，
        // 投影落在那里正合适。
        // v1.10.394：Material 那 2dp 投影在这两个按钮上从来没出现过（弹窗不在换肤链路里，
        // stateListAnimator 没人替它跑）。显式给回标准值，配合上面放开裁剪 → 四边完整。
        cancelButton.setElevation(2.0f * this.dp);
        confirmButton.setElevation(2.0f * this.dp);
        ThemeManager.INSTANCE.exposeShadow(cancelButton);
        ThemeManager.INSTANCE.exposeShadow(confirmButton);

        this.card = box;
        int cardWidth = (int) Math.min(
                this.activity.getResources().getDisplayMetrics().widthPixels * 0.88f, this.dp * 380.0f);
        FrameLayout.LayoutParams boxLp = new FrameLayout.LayoutParams(
                cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        boxLp.gravity = Gravity.CENTER;
        root.addView(box, boxLp);

        Fonts.applyTree(box);
        this.dialog.setContentView(root);
        this.dialog.setCanceledOnTouchOutside(false);
        // 返回键也要走反向动画（默认直接 dismiss，卡片会「啪」地消失，且模糊底图来不及回收）
        this.dialog.setOnKeyListener(new android.content.DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(android.content.DialogInterface d, int keyCode, android.view.KeyEvent event) {
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK
                        && event.getAction() == android.view.KeyEvent.ACTION_UP) {
                    cancel();
                    return true;
                }
                return false;
            }
        });
        // 兜底：任何路径的 dismiss 都要把全屏位图还回去
        this.dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface d) {
                BlurBackdropView back = ConfirmDialog.this.backdrop;
                if (back != null) {
                    back.release();
                    ConfirmDialog.this.backdrop = null;
                }
                if (ConfirmDialog.this.uiMode != null) {
                    ConfirmDialog.this.uiMode.stop();
                }
            }
        });

        Window window = this.dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            // v1.10.416：**让窗口内容真的铺到系统栏下面**。
            // 只有 FLAG_LAYOUT_IN_SCREEN + setLayout(MATCH_PARENT, ...) 还不够：
            // 主题（Theme_Material_Dialog_Alert）的 decor 仍会给状态栏/导航栏留出内边距，
            // 于是模糊底只盖住中间那块，两条栏露出没糊的页面。
            // 这两句是唯一可靠的开关（API 30+ 用 setDecorFitsSystemWindows，之前用 systemUiVisibility）。
            try {
                if (Build.VERSION.SDK_INT >= 30) {
                    window.setDecorFitsSystemWindows(false);
                    // 再补一道：**这个窗口不接受任何系统栏 inset 的收缩**。
                    // 光有 decorFits=false 在部分 ROM 的浮动对话框上仍会被内缩（实测 416 那版
                    // 状态栏那一条还是没糊），把 fitInsetsTypes 直接清零最稳。
                    WindowManager.LayoutParams lp = window.getAttributes();
                    lp.setFitInsetsTypes(0);
                    lp.setFitInsetsSides(0);
                    lp.layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                    window.setAttributes(lp);
                } else {
                    window.getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
                }
                // 栏色透明，否则系统会用主题里的栏底色在模糊底上盖一条不透明的带
                window.setStatusBarColor(Color.TRANSPARENT);
                window.setNavigationBarColor(Color.TRANSPARENT);
                if (Build.VERSION.SDK_INT >= 29) {
                    window.setNavigationBarContrastEnforced(false);
                }
            } catch (Throwable ignored) {
            }
            window.setDimAmount(0.0f);
            window.setGravity(Gravity.CENTER);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            try {
                // 主题自带的 dialog 动画（fade+scale 150ms）会和这里自己的 300ms 曲线叠加，
                // 叠出来是「先被系统缩一下再被我们展开」—— 关掉，只留一套动画。
                window.setWindowAnimations(0);
            } catch (Throwable ignored) {
            }
        }
        this.dialog.show();
        Window shown = this.dialog.getWindow();
        if (shown != null) {
            // 有的 ROM 在 show() 里会用主题里的 width 覆盖一次，show 之后再钉一遍
            shown.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        if (this.quietNext) {
            // 重建换肤：直接以静止姿态接上（模糊底已经是新主题的）
            this.quietNext = false;
            if (this.backdrop != null) {
                this.backdrop.setAlpha(1.0f);
            }
            box.setAlpha(1.0f);
            box.setScaleX(1.0f);
            box.setScaleY(1.0f);
            box.setTranslationY(0.0f);
        } else {
            playIn(box);
        }
        // v1.10.417：注册「系统深浅色」监听。弹窗开着的时候用户从控制中心切深色，
        // 以前这块会一直保持旧配色（颜色全在构造时取了一次）；现在整块重建。
        if (this.uiMode == null) {
            this.uiMode = new UiModeWatcher(this.activity, new UiModeWatcher.Listener() {
                @Override
                public void onSystemModeChanged() {
                    rebuildForTheme();
                }
            });
        }
        this.uiMode.start();
    }

    /** v1.10.417：系统深浅色变了 —— 拆掉重开，颜色/主题/模糊底全部按新配置重算。 */
    private void rebuildForTheme() {
        if (this.closing || !this.dialog.isShowing()) {
            return;
        }
        Dialog old = this.dialog;
        // 不跑「下滑淡出」那套：这次只是换色，用户不该看到一次关闭动画
        old.setOnDismissListener(null);
        old.setOnKeyListener(null);
        BlurBackdropView back = this.backdrop;
        this.backdrop = null;
        this.card = null;
        try {
            old.dismiss();
        } catch (Throwable ignored) {
        }
        if (back != null) {
            back.release();
        }
        // 颜色是「每次调 colors() 现算」的，所以重建 = 跟随。
        // dark 也必须跟着刷 —— 它决定模糊底的 scrim 浓度和玻璃卡的描边色，
        // 漏了它就是「配色变了、底色和边框还是旧模式的那一套」。
        this.dark = ThemeManager.INSTANCE.isDark(this.activity);
        this.colors = ThemeManager.INSTANCE.colors(this.activity);
        this.quietNext = true;
        this.dialog = newDialog();
        show();
    }

    /** 胶囊按钮：全大写关掉、字体跟着全局设置、按下 0.965 回弹。 */
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
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(0.965f).scaleY(0.965f).setDuration(90L).start();
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150L).start();
                }
                return false;
            }
        });
        return button;
    }

    private void playIn(@NotNull final View box) {
        final float lift = this.dp * 18.0f;
        box.setAlpha(0.0f);
        box.setScaleX(0.92f);
        box.setScaleY(0.92f);
        box.setTranslationY(lift);
        final BlurBackdropView back = this.backdrop;
        if (back != null) {
            back.setAlpha(0.0f);
        }
        box.post(new Runnable() {
            @Override
            public void run() {
                if (back != null) {
                    back.animate().alpha(1.0f).setDuration(220L).start();
                }
                ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);
                animator.setDuration(300L);
                animator.setInterpolator(null);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
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
                    }
                });
                animator.start();
            }
        });
    }

    private void playOut(@NotNull final Runnable after) {
        final View box = this.card;
        final BlurBackdropView back = this.backdrop;
        if (box == null) {
            after.run();
            return;
        }
        final float lift = this.dp * 10.0f;
        if (back != null) {
            back.animate().alpha(0.0f).setDuration(180L).start();
        }
        ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.setDuration(190L);
        animator.setInterpolator(null);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
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
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean done;

            @Override
            public void onAnimationEnd(Animator animation) {
                if (this.done) {
                    return;
                }
                this.done = true;
                after.run();
            }
        });
        animator.start();
    }

    /** 点「取消」/点外面/返回键：走反向动画，不执行回调。 */
    public void cancel() {
        if (this.closing) {
            return;
        }
        this.closing = true;
        playOut(new Runnable() {
            @Override
            public void run() {
                Runnable callback = ConfirmDialog.this.onCancel;
                dismiss();
                if (callback != null) {
                    callback.run();
                }
            }
        });
    }

    /** 点「确认」：先收起窗口再执行回调，回调里重建 UI 也不会盖住动画。 */
    private void confirm() {
        if (this.closing) {
            return;
        }
        this.closing = true;
        playOut(new Runnable() {
            @Override
            public void run() {
                Runnable callback = ConfirmDialog.this.onConfirm;
                dismiss();
                if (callback != null) {
                    callback.run();
                }
            }
        });
    }

    private void dismiss() {
        try {
            this.dialog.dismiss();
        } catch (Throwable ignored) {
        }
        BlurBackdropView back = this.backdrop;
        if (back != null) {
            back.release();
            this.backdrop = null;
        }
    }

    public void onBackPressed() {
        cancel();
    }

    /**
     * 把当前页面画进一张位图（给 BlurBackdropView 做模糊底）。
     *
     * <p>v1.10.416：**从 `android.R.id.content` 改成 decorView**。
     * 现场：删除配置的确认框弹出来时，状态栏那一条是「清晰的页面底色」，只有它下面是糊的。
     * 原因就是 content 只覆盖内容区 —— 状态栏那 24~30dp 是由 **窗口的 decor 背景**画的
     * （`ThemeManager.applySystemBars` 里 `decor.setBackgroundColor(...)`），
     * content 自己的背景是透明的，所以截出来的图**顶上是一条全透明**，
     * BlurBackdropView 按整块画出来就露出下面没糊的页面。
     * 改成截 decorView 之后：整屏都有像素（含页面渐变/底色），模糊底才盖满整个窗口。
     */
    @Nullable
    private Bitmap capture() {
        Window w = this.activity.getWindow();
        View src = w != null ? w.getDecorView() : null;
        if (src == null) {
            src = this.activity.findViewById(android.R.id.content);
        }
        if (src == null || src.getWidth() <= 0 || src.getHeight() <= 0) {
            return null;
        }
        try {
            Bitmap shot = Bitmap.createBitmap(src.getWidth(), src.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(shot);
            src.draw(canvas);
            return shot;
        } catch (Throwable th) {
            return null;
        }
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * alpha);
        if (a <= 0) {
            a = 1;
        }
        return (a << 24) | (color & 0x00FFFFFF);
    }
}
