package io.github.animwidget;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.Nullable;

/**
 * v1.10.98（需求 5）：**开发者页面（整页重写）**。
 *
 * 原来的开发者信息只是一个小弹窗：灰色方块里几行字 + 三个按钮。
 * 现在改成和开屏同源的整页：
 *   · 背景 = 静态极光（同一个 {@link AuroraView}，冻结不动，省电）；
 *   · 结构 = 创作者名片 → 技术底座 → 调试与记录 → 关于；
 *   · 语言/字体/深浅色全部跟随应用设置（I18nActivity + ThemeManager + Fonts）。
 *
 * 原来弹窗里的能力（版本号、复制 QQ、赞赏、日志）一个不少，只是换了个讲法。
 */
public final class DeveloperActivity extends I18nActivity {

    private static final String QQ = "188019597";

    @Nullable
    private AuroraView aurora;

    /** v1.10.184：上一次应用过的深浅色（系统切模式后整页刷新用）。 */
    boolean appliedDark;

    /**
     * v1.10.184：**系统深浅色切换后的整页刷新**。
     *
     * 原来这一页只在 onCreate 里过一次主题：清单里它声明了 `configChanges="uiMode"`，
     * 所以系统切模式时 Activity 不会被重建，`onCreate` 里的上色也不会再跑 ——
     * 表现就是「赞赏弹层（900ms 轮询）跟上了，开发者页没跟上」。
     * 现在与主界面/设置页同一套：`onConfigurationChanged` 里整页叠化 + 500ms 轮询兜底。
     */
    private final Runnable themePoll = new Runnable() {
        @Override
        public void run() {
            boolean dark = ThemeManager.INSTANCE.isDark(DeveloperActivity.this);
            if (dark != DeveloperActivity.this.appliedDark) {
                DeveloperActivity.this.appliedDark = dark;
                refreshAfterThemeChange();
            }
            getWindow().getDecorView().postDelayed(this, 500L);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashLog.install(this);
        this.appliedDark = ThemeManager.INSTANCE.isDark(this);
        setContentView(R.layout.activity_developer);
        setI18nLayout(R.layout.activity_developer);
        EdgeToEdge.INSTANCE.applyImmersive(this);
        View content = findViewById(android.R.id.content);
        ThemeManager.INSTANCE.applyTo(content);
        ThemeManager.INSTANCE.applySystemBars(this);

        this.aurora = (AuroraView) findViewById(R.id.dev_aurora);
        if (this.aurora != null) {
            this.aurora.setFrozen(true);
        }
        styleScrim();
        styleHero();
        // v1.10.105（需求 2.1）：内容比手指慢半拍（低延迟），滚起来有重量
        ScrollView scroller = findScrollView(findViewById(android.R.id.content));
        if (scroller != null && scroller.getChildCount() > 0) {
            ScrollLag.attach(scroller, scroller.getChildAt(0));
        }
        buildCaps();
        styleAbout();

        View back = findViewById(R.id.dev_back);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }
        // v1.10.417（需求）：**调试与记录卡片的三枚按钮去掉阴影**。
        // 阴影来自 Material Button 主题自带的 stateListAnimator（默认 2dp，按下还会加高），
        // 不是背景 drawable 画的 —— 所以只清 elevation 不够，必须把 animator 也摘掉，
        // 否则按一下又浮起来。深浅色下都按同一个口径处理（这段代码不区分主题）。
        for (int id : new int[]{R.id.dev_btn_logs, R.id.dev_btn_intro, R.id.dev_btn_reward}) {
            View flat = findViewById(id);
            if (flat == null) {
                continue;
            }
            flat.setStateListAnimator(null);
            flat.setElevation(0.0f);
            flat.setTranslationZ(0.0f);
            if (flat instanceof Button) {
                ((Button) flat).setStateListAnimator(null);
            }
        }
        Button logs = (Button) findViewById(R.id.dev_btn_logs);
        if (logs != null) {
            logs.setOnClickListener(v -> startActivity(new Intent(this, LogActivity.class)));
        }
        Button intro = (Button) findViewById(R.id.dev_btn_intro);
        if (intro != null) {
            intro.setOnClickListener(v -> CreatorIntroActivity.open(this));
        }
        Button reward = (Button) findViewById(R.id.dev_btn_reward);
        if (reward != null) {
            reward.setOnClickListener(v -> RewardSheet.show(this));
        }
        // v1.10.138：两个实验开关已**移植到设置页**（性能卡里），这里不再重复添加；
        // 保留一段说明，避免以后有人以为开关丢了。
        //   · 预装帧（A 方案）→ WidgetFlip.setEnabled
        //   · URI 推送（B 方案）→ WidgetUriPush.setEnabled
        // v1.10.359：「复制 QQ」从 Button 改成 TextView，与上方「关于」并成一枚竖向胶囊
        TextView copy = (TextView) findViewById(R.id.dev_btn_copy);
        if (copy != null) {
            copy.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(ClipboardManager.class);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("QQ", QQ));
                }
                Toast.makeText(this, getString(R.string.dev_copied, QQ), Toast.LENGTH_SHORT).show();
            });
            // 下格：点击时**向下**倾斜
            QqPill.install(copy, false);
        }
        // 分隔线：普通 View 不走 ThemeManager 的 divider 分支（那只处理 ViewGroup），
        // 这里用「正文色 + 20% 透明度」手动给，深浅色都看得见。
        View pillLine = findViewById(R.id.dev_pill_divider);
        if (pillLine != null) {
            ThemeColors tc = ThemeManager.INSTANCE.colors(this);
            pillLine.setBackgroundColor((tc.getText() & 0x00FFFFFF) | 0x33000000);
        }
        View about = findViewById(R.id.dev_btn_about);
        if (about != null) {
            about.setOnClickListener(v -> AboutSheet.show(this));
            // 上格：点击时**向上**倾斜
            QqPill.install(about, true);
        }
    }

    /**
     * v1.10.183：赞赏弹层改成了「挂在 content 上的覆盖层」（不是 Dialog），
     * 拿不到自己的返回键回调，所以由宿主转一下：先收二维码、再关弹层。
     */
    @Override
    public void onBackPressed() {
        if (AboutSheet.handleBack()) {
            return;
        }
        if (RewardSheet.handleBack()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.appliedDark = ThemeManager.INSTANCE.isDark(this);
        refreshAfterThemeChange();
    }

    /**
     * 赞赏弹层开着的时候**不叠化**：它盖着整页，页面刷成什么样都看不见，
     * 而 Morph 的叠化位图会把弹层也一起拍进去（看起来就是弹层闪了一下旧配色）。
     * 这时改成「立刻刷页面 + 通知弹层自己刷」，弹层顺带重截模糊底。
     */
    private void refreshAfterThemeChange() {
        if (RewardSheet.isOpen()) {
            refreshThemeInPlace();
            RewardSheet.onHostThemeChanged();
            return;
        }
        // v1.10.366：「关于」弹层同样是覆盖层，而且它是**一次性把颜色烘进视图**建出来的。
        // 之前这里走的是 Morph 整页叠化 —— 叠化拍的是「旧配色的弹层」，所以弹层和模糊底
        // 全程纹丝不动，只有关掉之后才看到页面变了（用户原话）。现在改成：先把页面刷成新主题，
        // 再让弹层自己整块重建（不弹第二次）。
        if (AboutSheet.isOpen()) {
            refreshThemeInPlace();
            AboutSheet.onHostThemeChanged();
            return;
        }
        Morph.crossfade(this, () -> refreshThemeInPlace());
    }

    /** 整页换色：先把主题铺下去，再重建那些「按颜色手搓出来的」视图。 */
    void refreshThemeInPlace() {
        View content = findViewById(android.R.id.content);
        if (content != null) {
            ThemeManager.INSTANCE.applyTo(content);
        }
        ThemeManager.INSTANCE.applySystemBars(this);
        // v1.10.185 修正：极光的配色是**建 shader 时**取的主题色（AuroraView.buildShaders），
        // 只 invalidate 会继续用旧配色 —— 表现就是「文字/卡片已经是浅色了，
        // 背景还是一整片灰」。必须调 refreshPalette() 重新取色 + 重建 shader。
        if (this.aurora != null) {
            this.aurora.refreshPalette();
        }
        styleScrim();
        styleHero();
        buildCaps();
        View content2 = findViewById(android.R.id.content);
        if (content2 != null) {
            content2.invalidate();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 后台期间系统切了模式的话，回前台要补一次（不能直接把 appliedDark 覆盖掉）
        boolean dark = ThemeManager.INSTANCE.isDark(this);
        if (dark != this.appliedDark) {
            this.appliedDark = dark;
            refreshAfterThemeChange();
        }
        getWindow().getDecorView().removeCallbacks(this.themePoll);
        getWindow().getDecorView().post(this.themePoll);
    }

    @Override
    protected void onPause() {
        getWindow().getDecorView().removeCallbacks(this.themePoll);
        super.onPause();
    }

    @Nullable
    private static ScrollView findScrollView(@Nullable View view) {
        if (view instanceof ScrollView) {
            return (ScrollView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                ScrollView hit = findScrollView(group.getChildAt(i));
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * alpha);
        if (a <= 0) {
            a = 1;
        }
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private void styleScrim() {
        View scrim = findViewById(R.id.dev_scrim);
        if (scrim == null) {
            return;
        }
        int bg = ThemeManager.INSTANCE.colors(this).getBackground();
        boolean dark = ThemeManager.INSTANCE.isDark(this);
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                dark ? new int[]{withAlpha(bg, 0.44f), withAlpha(bg, 0.20f), withAlpha(bg, 0.92f)}
                     : new int[]{withAlpha(bg, 0.58f), withAlpha(bg, 0.26f), withAlpha(bg, 0.94f)});
        scrim.setBackground(gd);
    }

    private void styleHero() {
        LinearLayout hero = (LinearLayout) findViewById(R.id.dev_hero);
        if (hero == null) {
            return;
        }
        ThemeColors c = ThemeManager.INSTANCE.colors(this);
        float dp = getResources().getDisplayMetrics().density;
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{withAlpha(c.getPrimary(), 0.92f), withAlpha(c.getSecondary(), 0.86f)});
        gd.setCornerRadius(24.0f * dp);
        hero.setBackground(gd);
        TextView avatar = (TextView) findViewById(R.id.dev_avatar);
        if (avatar != null) {
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(0x33FFFFFF);
            avatar.setBackground(circle);
            avatar.setTextColor(c.getOnPrimary());
        }
        TextView name = (TextView) findViewById(R.id.dev_name_tv);
        if (name != null) {
            name.setTextColor(c.getOnPrimary());
        }
        TextView slogan = (TextView) findViewById(R.id.dev_slogan_tv);
        if (slogan != null) {
            slogan.setTextColor(withAlpha(c.getOnPrimary(), 0.82f));
        }
        TextView kicker = (TextView) findViewById(R.id.dev_kicker_tv);
        if (kicker != null) {
            kicker.setTextColor(withAlpha(c.getText(), 0.55f));
        }
    }

    /** 技术底座：8 条，绿色勾 + 一行字（v1.10.240 从 6 条扩到 8 条，见 docs/DEVELOPER-CAPS.md）。 */
    private void buildCaps() {
        LinearLayout host = (LinearLayout) findViewById(R.id.dev_caps_host);
        if (host == null) {
            return;
        }
        host.removeAllViews();
        float dp = getResources().getDisplayMetrics().density;
        ThemeColors c = ThemeManager.INSTANCE.colors(this);
        int[] caps = {R.string.dev_cap_1, R.string.dev_cap_2, R.string.dev_cap_3,
                R.string.dev_cap_4, R.string.dev_cap_5, R.string.dev_cap_6,
                R.string.dev_cap_7, R.string.dev_cap_8};
        for (int i = 0; i < caps.length; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding((int) (dp * 16.0f), (int) (dp * 7.0f), (int) (dp * 16.0f), (int) (dp * 7.0f));
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView check = new TextView(this);
            check.setText("✓");
            check.setTextSize(12.0f);
            check.setGravity(android.view.Gravity.CENTER);
            check.setTextColor(c.getPrimary());
            check.setLayoutParams(new LinearLayout.LayoutParams((int) (dp * 22.0f),
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            row.addView(check);

            TextView text = new TextView(this);
            text.setText(caps[i]);
            text.setTextSize(12.5f);
            text.setTextColor(c.getTextMuted());
            text.setLineSpacing(0.0f, 1.12f);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            text.setLayoutParams(tlp);
            row.addView(text);

            host.addView(row);
        }
        Fonts.applyTree(host);
    }

    private void styleAbout() {
        TextView version = (TextView) findViewById(R.id.dev_version_tv);
        if (version == null) {
            return;
        }
        String name = "";
        String code = "";
        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            name = info.versionName == null ? "" : info.versionName;
            // getLongVersionCode() 是 API 28；26/27 只能用老字段
            code = android.os.Build.VERSION.SDK_INT >= 28
                    ? String.valueOf(info.getLongVersionCode())
                    : String.valueOf(info.versionCode);
        } catch (Throwable th) {
            name = "";
            code = "";
        }
        version.setText(getString(R.string.dev_version, name) + "  ·  vc " + code
                + "  ·  minSdk 26");
        TextView contact = (TextView) findViewById(R.id.dev_contact_tv);
        if (contact != null) {
            contact.setText(getString(R.string.dev_qq_value) + "  ·  " + getString(R.string.dev_role));
        }
    }

    @Override
    protected void onTextsRefreshed() {
        View content = findViewById(android.R.id.content);
        ThemeManager.INSTANCE.applyTo(content);
        styleScrim();
        styleHero();
        buildCaps();
        styleAbout();
        Fonts.applyTree(content);
    }
}
