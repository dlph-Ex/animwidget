package io.github.animwidget;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v1.10.386（需求）：「关于」页**开源仓库行**点出来的小弹窗。
 *
 * <p>三个动作，排布按用户指定：
 * <pre>
 *   ┌──────────────────────────┐
 *   │ 开源仓库                  │
 *   │ 🔗 dlph_Ex/animwidget     │
 *   │ [ 复制 ]  [ 跳转 ]        │   ← 上排
 *   │ [      取消      ]        │   ← 下排
 *   └──────────────────────────┘
 * </pre>
 * 视觉沿用 App 现有语言：26dp 圆角卡片、胶囊按钮（跳转 = 主色实底）、
 * 入场与 {@link ConfirmDialog} 同一套（缩放 0.92→1 + 上浮 16dp + 渐显，240ms，{@link Ease#IN_OUT}）。
 */
final class RepoDialog {

    private RepoDialog() {
    }

    static void show(@NotNull final Activity activity, @NotNull final String url, @NotNull final String label) {
        final ThemeColors c = ThemeManager.INSTANCE.colors(activity);
        final float d = activity.getResources().getDisplayMetrics().density;

        // v1.10.418（用户截图：卡片整体偏左、偏上）：
        // **浮动窗的窗口尺寸由主题的 windowMinWidth/Height 决定，不是由我们的卡片决定** ——
        // 于是卡片被放进一个"比它宽一点、位置由系统算"的窗口里，看上去就是歪的。
        // 现在改成和 ConfirmDialog 同一套：非浮动主题 + 窗口铺满 + 卡片自己 Gravity.CENTER。
        // 位置只由 root 的中心决定，不再受任何主题尺寸属性影响。
        final Dialog dialog = new Dialog(activity, ThemeManager.INSTANCE.dialogTheme(activity));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0));
            // v1.10.442（用户："关闭动画结束后背景暗面才消失，应该改得适中一点"）：
            // 原来暗面是**系统窗口级的 dim**（FLAG_DIM_BEHIND + dimAmount）—— 它没法跟着动画走，
            // 只能等窗口 destory 时整块"啪"地消失，于是卡片收完了、暗面还在，最后一下特别突兀。
            // 现在改成**窗口内的一层黑色遮罩**，跟卡片一起淡入淡出（数值见 scrim / playOut）。
            w.setDimAmount(0.0f);
            w.setGravity(Gravity.CENTER);
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            try {
                w.setWindowAnimations(0);
            } catch (Throwable ignored) {
            }
        }

        // 暗面遮罩：整屏黑 + alpha 0.42（= 原来的 dimAmount）。跟着动画淡入淡出。
        final float dim = 0.42f;
        final View scrim = new View(activity);
        scrim.setBackgroundColor(android.graphics.Color.BLACK);
        scrim.setAlpha(0.0f);

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(d, 18), dp(d, 18), dp(d, 18), dp(d, 16));
        // 卡片不让「内边距」参与裁剪，否则最下面那枚按钮的投影会被切掉
        card.setClipToPadding(false);
        // v1.10.416（需求）：**加细边框，并跟着深浅模式走**。
        // 原来是纯色圆角块（rounded 只有填充、没有描边），在浅色模式下和页面底色几乎同色，
        // 卡片"没有边界"。这里换成与 ConfirmDialog 卡片同一块 GlassFrameDrawable：
        //   · 描边 = 1.2dp 渐变高光边（左上亮 → 右下几乎透明）；
        //   · 深色模式是白色高光（0x66FFFFFF 起），浅色模式自动换成墨色收边（0x44000000 起）
        //     —— 这个换色是 Drawable 里按 dark 标志算的，不需要调用点各写一套。
        // 填充稍稍提亮透明度（0.98）：这个弹窗没有模糊底，太透会和背后的暗化混成一片。
        card.setBackground(new GlassFrameDrawable(
                ThemeManager.INSTANCE.withAlpha(c.getSurface(), 0.98f),
                26.0f * d, 1.2f * d, ThemeManager.INSTANCE.isDark(activity)));
        // 卡片自己吃掉点击：点空白处不关（只有「取消」和点外面才关）
        card.setClickable(true);

        TextView title = new TextView(activity);
        title.setText(R.string.about_repo);
        title.setTextSize(14.0f);
        title.setTextColor(c.getText());
        title.getPaint().setFakeBoldText(true);
        card.addView(title, wrap());

        // 链接值：前面一个与文字等高的链接符号
        LinearLayout valueBox = new LinearLayout(activity);
        valueBox.setOrientation(LinearLayout.HORIZONTAL);
        valueBox.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams valueLp = wrap();
        valueLp.topMargin = dp(d, 8);
        card.addView(valueBox, valueLp);

        GlyphView glyph = new GlyphView(activity);
        glyph.setGlyph(GlyphView.LINK);
        glyph.setTintColor(ThemeManager.INSTANCE.withAlpha(c.getText(), 0.62f));
        LinearLayout.LayoutParams glyphLp = new LinearLayout.LayoutParams(dp(d, 16), dp(d, 16));
        glyphLp.rightMargin = dp(d, 6);
        valueBox.addView(glyph, glyphLp);

        TextView link = new TextView(activity);
        link.setText(label);
        link.setTextSize(13.0f);
        link.setTextColor(ThemeManager.INSTANCE.withAlpha(c.getText(), 0.62f));
        valueBox.addView(link, wrap());

        // 上排：复制 / 跳转
        LinearLayout top = new LinearLayout(activity);
        top.setOrientation(LinearLayout.HORIZONTAL);
        // v1.10.395：同上 —— 行内留 6dp 给投影，行距同步减 6dp，按钮位置不动
        top.setMinimumHeight(dp(d, 60));                      // 48 + 6×2
        top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        top.setClipChildren(false);
        top.setClipToPadding(false);
        LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(-1, dp(d, 60));
        topLp.topMargin = dp(d, 10);
        card.addView(top, topLp);

        Button copy = pill(activity, c.getNeutral(), c.getText(), R.string.about_copy);
        LinearLayout.LayoutParams copyLp = new LinearLayout.LayoutParams(0, dp(d, 48), 1.0f);
        copyLp.rightMargin = dp(d, 10);
        top.addView(copy, copyLp);

        Button open = pill(activity, c.getPrimary(), c.getOnPrimary(), R.string.about_open);
        top.addView(open, new LinearLayout.LayoutParams(0, dp(d, 48), 1.0f));
        // v1.10.393：上排两枚按钮把行高撑得正好等于按钮高度，2dp 投影上下会被切平。
        // 只放开裁剪，按钮位置与投影形状都不动（原因见 ThemeManager.exposeShadow）。
        ThemeManager.INSTANCE.exposeShadow(copy);
        ThemeManager.INSTANCE.exposeShadow(open);

        // 下排：取消
        Button cancel = pill(activity, c.getNeutral(), c.getText(), R.string.cancel);
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(-1, dp(d, 48));
        cancelLp.topMargin = dp(d, 10);
        card.addView(cancel, cancelLp);
        ThemeManager.INSTANCE.exposeShadow(cancel);

        // v1.10.441（用户："开源仓库对话框增加关闭动画，与其他确认对话框相同"）：
        // 原来四个出口（复制 / 跳转 / 取消 / 点外面）都是直接 dismiss —— 卡片"啪"一下消失。
        // 现在全部走 close()：先播 ConfirmDialog 那套退场（190ms / Ease.OUT / 缩到 0.95 /
        // 下沉 10dp / 渐隐），播完再真正 dismiss。closing 标记挡住连点。
        final boolean[] closing = new boolean[1];
        final Runnable close = new Runnable() {
            @Override
            public void run() {
                if (closing[0]) {
                    return;
                }
                closing[0] = true;
                playOut(dialog, card, scrim, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            dialog.dismiss();
                        } catch (Throwable ignored) {
                        }
                    }
                });
            }
        };

        copy.setOnClickListener(v -> {
            try {
                ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("url", url));
                }
                Toast.makeText(activity, R.string.about_copied, Toast.LENGTH_SHORT).show();
            } catch (Throwable ignored) {
            }
            close.run();
        });
        open.setOnClickListener(v -> {
            openUrl(activity, url);
            close.run();
        });
        cancel.setOnClickListener(v -> close.run());

        // v1.10.387（用户：图 1「这是什么雷霆比例」）：卡片没定宽 → 被两个 weight 按钮压缩成一条，
        // 「复制 / 跳转」竖排换行。现在按屏宽定宽（左右各留 40dp，上限 420dp），按钮单行。
        int screenW = activity.getResources().getDisplayMetrics().widthPixels;
        int cardW = Math.min(screenW - dp(d, 80), dp(d, 420));
        android.widget.FrameLayout root = new android.widget.FrameLayout(activity);
        root.setClickable(true);
        // 点卡片外面 = 关闭（非浮动窗口下系统的 touch-outside 不生效，得自己接）
        root.setOnClickListener(v -> close.run());
        android.widget.FrameLayout.LayoutParams cardLp = new android.widget.FrameLayout.LayoutParams(
                cardW, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.gravity = Gravity.CENTER;
        // 遮罩最底层、卡片压在上面（顺序决定 z 轴）
        root.addView(scrim, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(card, cardLp);
        dialog.setContentView(root, new android.view.ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // v1.10.417（需求）：系统换深浅色时整块重建 —— 颜色全在构建时取了一次，
        // 不重建就会一直停在旧配色。重建 = 先关掉这个窗口，再按新配置重新走一遍 show()。
        final UiModeWatcher[] watcher = new UiModeWatcher[1];
        watcher[0] = new UiModeWatcher(activity, new UiModeWatcher.Listener() {
            @Override
            public void onSystemModeChanged() {
                UiModeWatcher w = watcher[0];
                if (w != null) {
                    w.stop();
                }
                try {
                    dialog.dismiss();
                } catch (Throwable ignored) {
                }
                show(activity, url, label);
            }
        });
        watcher[0].start();
        dialog.setCancelable(false);
        dialog.setOnKeyListener((dlg, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                if (event.getAction() == android.view.KeyEvent.ACTION_UP) {
                    close.run();
                }
                return true;      // 上下都吃掉：默认的"直接 dismiss"不能跑
            }
            return false;
        });
        dialog.setOnDismissListener(closed -> {
            UiModeWatcher active = watcher[0];
            if (active != null) {
                active.stop();
            }
        });
        dialog.show();
        Window shownWindow = dialog.getWindow();
        if (shownWindow != null) {
            // 同 ConfirmDialog：有的 ROM 在 show() 里会用主题里的尺寸覆盖一次
            shownWindow.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
        // 入场：缩放 0.92 → 1 + 上浮 16dp + 渐显（与 ConfirmDialog 同一手感）
        card.setAlpha(0.0f);
        card.setScaleX(0.92f);
        card.setScaleY(0.92f);
        card.setTranslationY(16.0f * d);
        card.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).translationY(0.0f)
                .setDuration(240L)
                .setInterpolator(t -> Ease.IN_OUT.at(t))
                .start();
        // 入场暗面 220ms 淡进来（ConfirmDialog 的背景层也是 220ms）——不再"弹窗一出来天就黑了"
        scrim.animate().alpha(dim).setDuration(220L).start();
    }

    /**
     * v1.10.441：退场动画 —— **逐值对齐** {@code ConfirmDialog#playOut}，不另调曲线。
     *
     * <p>190ms、{@link Ease#OUT}、透明度 1→0、缩放 1→0.95、下沉 10dp，播完才 dismiss。
     * 进场的 240ms / 0.92→1 / 上浮 16dp 本来就和 ConfirmDialog 同一套，现在两头一致：
     * 用户要的"与其他确认对话框相同"就是这两组数字要一模一样。
     */
    private static void playOut(@NotNull final Dialog dialog, @NotNull final View card,
                                @Nullable final View scrim, @NotNull final Runnable after) {
        // 入场是 ViewPropertyAnimator（240ms）；万一用户在 240ms 内就点了关闭，
        // 两条动画会同时改 alpha/scale —— 先把它掐掉，退场永远从"完全打开"的状态出发。
        try {
            card.animate().cancel();
        } catch (Throwable ignored) {
        }
        // 暗面跟卡片一起淡出（和 ConfirmDialog 的背景层同一节奏：180ms）。
        // 这样窗口真正 dismiss 的时候什么都不剩，用户不会再看到"卡片没了、暗面还在"。
        if (scrim != null) {
            scrim.animate().alpha(0.0f).setDuration(180L).start();
        }
        final float lift = card.getResources().getDisplayMetrics().density * 10.0f;
        android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.setDuration(190L);
        animator.setInterpolator(null);
        animator.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(android.animation.ValueAnimator animation) {
                float t = Ease.OUT.at(animation.getAnimatedFraction());
                if (t < 0.0f) {
                    t = 0.0f;
                } else if (t > 1.0f) {
                    t = 1.0f;
                }
                card.setAlpha(1.0f - t);
                card.setScaleX(1.0f - 0.05f * t);
                card.setScaleY(1.0f - 0.05f * t);
                card.setTranslationY(lift * t);
            }
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

    /** 用系统默认浏览器打开（没有浏览器也不会崩）。 */
    static void openUrl(@NotNull Activity activity, @NotNull String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Throwable th) {
            Toast.makeText(activity, R.string.about_update_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private static Button pill(Activity activity, int bg, int fg, int textRes) {
        Button b = new Button(activity);
        b.setAllCaps(false);
        b.setSingleLine(true);
        b.setText(textRes);
        b.setTextSize(14.0f);
        b.setSingleLine(true);          // 不许折行（否则「复制」会变成竖排两行）
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setTextColor(fg);
        GradientDrawable gd = ThemeManager.INSTANCE.rounded(activity, bg, 999.0f);
        b.setBackground(gd);
        b.setStateListAnimator(null);
        // v1.10.394：显式给回 Material 的 2dp 投影（这里以前把它关成 0 了）
        b.setElevation(2.0f * activity.getResources().getDisplayMetrics().density);
        b.setPadding(dp(activity.getResources().getDisplayMetrics().density, 16), 0,
                dp(activity.getResources().getDisplayMetrics().density, 16), 0);
        return b;
    }

    private static int dp(float density, float value) {
        return Math.round(value * density);
    }

    private static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(-2, -2);
    }

    private static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }
}
