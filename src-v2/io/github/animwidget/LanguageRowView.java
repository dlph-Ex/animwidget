package io.github.animwidget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * v1.10.159（反馈「重做」）：**语言行自绘卡片**，替换系统 RadioButton。
 *
 * 上一版「换了壳≈没换」的根因：`RadioButton` 的圆圈是平台 drawable，
 * 行高又被 XML 定死 `48dp`（大字体直接裁字），选中态只能靠一个 13dp 的勾，
 * 视觉权重和普通列表行一样 —— 所以「一眼看不出选了哪个」。
 *
 * 这一版的结构（三行文案 + 右侧勾）：
 * ```
 * ┌──────────────────────────────────────────┐
 * │  简体中文                        ✓       │  ← 选中：主色 12% 淡底 + 主色文字
 * │  Chinese                                 │
 * ├──────────────────────────────────────────┤
 * │  跟随系统                                │  ← 未选中：无底色 + 正文色
 * │  English（当前实际生效）                  │
 * └──────────────────────────────────────────┘
 * ```
 * 关键点（都对着参考实现/规范改的，见回答里的清单）：
 *  · 行高 `minHeight=56dp` + `wrap_content`：大字体不裁字，也不会把行挤扁；
 *  · 标题单行 + `ellipsize=END`（Telegram `LanguageCell` 同款），
 *    副标题也单行省略，所以再长的语言名也不会换行把行撑高、更不会挤掉勾；
 *  · 勾用 {@link CheckMarkDrawable} 画，带**笔画生长**（180ms），
 *    选中/未选中只差一个 0→1 的 progress，不触发任何布局；
 *  · 按压反馈是 `RippleDrawable` 放在 foreground（盖在内容之上，
 *    不会被淡底的 pill 吃掉），并按 14dp 圆角裁剪；
 *  · RTL 全部走 `marginStart/End` + `gravity=START/END`，不会硬编码 left/right；
 *  · 无障碍：本行对外就是 `RadioButton`，`checkable/checked` 都设真值，
 *    TalkBack 会念「已选中」，而不是把两行文字当两个独立焦点。
 */
public final class LanguageRowView extends FrameLayout {

    private final View pill;
    private final LinearLayout content;
    private final TextView title;
    private final TextView sub;
    private final ImageView check;
    private final CheckMarkDrawable mark;

    private final float dp;
    private boolean checked;
    private ThemeColors colors;
    private Runnable pick;

    public LanguageRowView(Context context) {
        this(context, null);
    }

    public LanguageRowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.dp = context.getResources().getDisplayMetrics().density;
        setClickable(true);
        setFocusable(true);
        // 定高会裁字，这里只给下限
        setMinimumHeight(Math.round(56.0f * dp));

        pill = new View(context);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(14.0f * dp);
        bg.setColor(0x00000000);
        pill.setBackground(bg);
        pill.setAlpha(0.0f);
        addView(pill, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        // 右侧留 50dp 给勾：文字再长也被 ellipsize 收在勾左边，永远不会压到勾上
        content.setPaddingRelative(Math.round(16.0f * dp), Math.round(10.0f * dp),
                Math.round(50.0f * dp), Math.round(10.0f * dp));
        LayoutParams clp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        clp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        addView(content, clp);

        title = new TextView(context);
        title.setTextSize(15.0f);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        content.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        sub = new TextView(context);
        sub.setTextSize(11.5f);
        sub.setMaxLines(1);
        sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        sub.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Math.round(2.0f * dp);
        content.addView(sub, slp);

        mark = new CheckMarkDrawable(0, 13.0f * dp, 2.0f * dp);
        check = new ImageView(context);
        check.setImageDrawable(mark);
        check.setScaleType(ImageView.ScaleType.FIT_CENTER);
        check.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        LayoutParams ckp = new LayoutParams(Math.round(20.0f * dp), Math.round(20.0f * dp));
        ckp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        ckp.setMarginEnd(Math.round(16.0f * dp));
        addView(check, ckp);

        setOnClickListener(v -> {
            if (pick != null) {
                pick.run();
            }
        });
    }

    public void setOnPick(Runnable r) {
        this.pick = r;
    }

    public boolean isPicked() {
        return checked;
    }

    /**
     * @param titleText 主行（语言**自名**，不翻译）
     * @param subText   次行（当前界面语言里的写法 / 跟随系统的实际生效语言），空则隐藏
     */
    public void bind(CharSequence titleText, CharSequence subText, boolean selected, boolean animate) {
        title.setText(titleText);
        boolean hasSub = subText != null && subText.length() > 0;
        sub.setText(hasSub ? subText : "");
        sub.setVisibility(hasSub ? View.VISIBLE : View.GONE);
        Fonts.applyOne(title);
        Fonts.applyOne(sub);
        setPicked(selected, animate);
        setContentDescription(hasSub ? titleText + ", " + subText : String.valueOf(titleText));
    }

    /** 选中态切换：淡底 alpha + 勾的笔画生长，都只改绘制，不触发 requestLayout。 */
    public void setPicked(boolean selected, boolean animate) {
        boolean changed = selected != this.checked;
        this.checked = selected;
        setSelected(selected);
        title.setTextColor(selected ? primaryOn() : textOn());
        sub.setTextColor(selected ? subSelectedOn() : subOn());
        GradientDrawable bg = pill.getBackground() instanceof GradientDrawable
                ? (GradientDrawable) pill.getBackground() : null;
        if (bg != null) {
            bg.setColor(alpha(colors == null ? 0xFF6200EE : colors.getPrimary(), 0.12f));
        }
        mark.setColorAlpha(selected ? primaryOn() : 0);
        long dur = animate && changed ? 180L : 0L;
        pill.animate().cancel();
        check.animate().cancel();
        if (dur <= 0L) {
            pill.setAlpha(selected ? 1.0f : 0.0f);
            mark.setProgress(selected ? 1.0f : 0.0f);
            check.setAlpha(selected ? 1.0f : 0.0f);
            check.setScaleX(1.0f);
            check.setScaleY(1.0f);
            return;
        }
        android.animation.ValueAnimator a = android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
        a.setDuration(dur);
        final float fromAlpha = pill.getAlpha();
        final float toAlpha = selected ? 1.0f : 0.0f;
        final float fromMark = mark.getProgress();
        final float toMark = selected ? 1.0f : 0.0f;
        a.addUpdateListener(anim -> {
            float t = Ease.OUT.at(anim.getAnimatedFraction());
            pill.setAlpha(fromAlpha + (toAlpha - fromAlpha) * t);
            mark.setProgress(fromMark + (toMark - fromMark) * t);
            check.setAlpha(fromAlpha + (toAlpha - fromAlpha) * t);
        });
        a.start();
    }

    /** 主题/深浅色变化：颜色全部重取，不重建视图（不闪、不跳位）。 */
    public void applyTheme(ThemeColors c) {
        this.colors = c;
        if (c == null) {
            return;
        }
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(14.0f * dp);
        bg.setColor(alpha(c.getPrimary(), 0.12f));
        pill.setBackground(bg);
        mark.setColorAlpha(checked ? c.getPrimary() : 0);
        ColorStateList ripple = ColorStateList.valueOf(alpha(c.getPrimary(), 0.16f));
        GradientDrawable mask = new GradientDrawable();
        mask.setCornerRadius(14.0f * dp);
        mask.setColor(0xFFFFFFFF);
        setForeground(new RippleDrawable(ripple, null, mask));
        title.setTextColor(checked ? c.getPrimary() : c.getText());
        sub.setTextColor(checked ? alpha(c.getPrimary(), 0.72f) : alpha(c.getTextMuted(), 0.92f));
    }

    private int primaryOn() {
        return colors == null ? 0xFF6200EE : colors.getPrimary();
    }

    private int textOn() {
        return colors == null ? 0xDD000000 : colors.getText();
    }

    private int subOn() {
        return colors == null ? 0x99000000 : alpha(colors.getTextMuted(), 0.92f);
    }

    private int subSelectedOn() {
        return colors == null ? 0xAA6200EE : alpha(colors.getPrimary(), 0.72f);
    }

    private static int alpha(int color, float a) {
        int v = Math.round(255.0f * (a < 0.0f ? 0.0f : (a > 1.0f ? 1.0f : a)));
        return (color & 0x00FFFFFF) | (v << 24);
    }

    // ------------------------------------------------------------------ 测量

    /**
     * v1.10.160（反馈 1）：**行高只由自己的内容决定，忽略父容器给的高度上限**。
     *
     * 病象：面板展开时 `animateExpand` 逐帧把父容器高度从 0 设到目标值，
     * 子行如果吃 AT_MOST，就会有一帧被按「当前容器高度」重新量/撑开 ——
     * 肉眼看是「内部界面被拉伸一下」。字体列表以前也栽在同一处（行高定死才好的）。
     *
     * 做法：内容用 UNSPECIFIED 量一次拿到天然高度，然后**直接**定这个高度，
     * 父容器动画变成「从上面揭开」，而不是「把行挤压/拉伸」。
     */
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        if (MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED) {
            width = Math.round(320.0f * dp);
        }
        int innerW = Math.max(0, width - getPaddingLeft() - getPaddingRight());
        content.measure(MeasureSpec.makeMeasureSpec(innerW, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int h = Math.max(Math.round(56.0f * dp),
                content.getMeasuredHeight() + getPaddingTop() + getPaddingBottom());
        pill.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
        check.measure(MeasureSpec.makeMeasureSpec(Math.round(20.0f * dp), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Math.round(20.0f * dp), MeasureSpec.EXACTLY));
        if (MeasureSpec.getMode(heightSpec) == MeasureSpec.EXACTLY) {
            h = MeasureSpec.getSize(heightSpec);
            pill.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(width, h);
    }

    // ------------------------------------------------------------------ 无障碍

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (info == null) {
            return;
        }
        // 对外就是「一个单选按钮」，不是「一堆散的文字」
        info.setClassName("android.widget.RadioButton");
        info.setCheckable(true);
        info.setChecked(checked);
        info.setSelected(checked);
        info.setClickable(true);
        info.setEnabled(isEnabled());
    }
}
