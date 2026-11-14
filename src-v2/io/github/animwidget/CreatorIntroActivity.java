package io.github.animwidget;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jetbrains.annotations.Nullable;

/**
 * v1.10.98（需求 3）：**首次进入的开屏说明**。
 *
 * 观感目标：「超级创作者」。
 *   · 背景 = {@link AuroraView}：5 个巨型光斑按 sin/cos 复合轨迹缓慢流动，
 *     整层在 1/6 分辨率做两遍横竖盒式模糊再放大 —— 100% 高斯模糊，结构全抹平；
 *   · 前景 = 一点点错峰入场的文字（520ms，三次贝塞尔 OUT，不是线性淡入），
 *     序号/能力卡/主按钮各有自己的节奏；
 *   · 语言与深浅色全部跟随系统（基类 I18nActivity + ThemeManager）。
 *
 * 只在第一次进入时出现（`settings/intro_seen`）；开发者页可以「重看开屏」。
 */
public final class CreatorIntroActivity extends I18nActivity {

    static final String PREFS = "settings";
    static final String SEEN = "intro_seen";

    @Nullable
    private AuroraView aurora;
    @Nullable
    private ValueAnimator breath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashLog.install(this);
        this.appliedDark = ThemeManager.INSTANCE.isDark(this);
        setContentView(R.layout.activity_intro);
        setI18nLayout(R.layout.activity_intro);
        EdgeToEdge.INSTANCE.applyImmersive(this);
        // 进来就算看过了：中途被杀不会反复弹。
        // 先记住「这是不是第一次」，光轮配色要用它。
        boolean firstRun = !getSharedPreferences(PREFS, 0).getBoolean(SEEN, false);
        getSharedPreferences(PREFS, 0).edit().putBoolean(SEEN, true).apply();

        View content = findViewById(android.R.id.content);
        ThemeManager.INSTANCE.applyTo(content);
        ThemeManager.INSTANCE.applySystemBars(this);

        this.aurora = (AuroraView) findViewById(R.id.intro_aurora);
        // v1.10.113（需求）：**只有新用户第一次进的那次开屏**才用壁纸采样的 5 色；
        // 之后的开屏（包括开发者页「重看开屏」）一律按当前系统配色方案 —— 保持品牌一致。
        if (this.aurora != null && firstRun) {
            int[] ring = WallpaperPalette.ensure(this);
            if (ring != null) {
                this.aurora.setRingColors(ring);
            }
        }
        styleScrim();
        styleGlass();
        styleTitle();
        buildFeatures();
        styleBadge();

        View skip = findViewById(R.id.intro_skip);
        View cta = findViewById(R.id.intro_cta);
        if (skip != null) {
            skip.setOnClickListener(v -> leave());
        }
        if (cta != null) {
            cta.setOnClickListener(v -> leave());
        }
        playEntrance();
    }

    /**
     * v1.10.184：系统深浅色切换后整页刷新。
     *
     * 这一页也声明了 `configChanges="uiMode"`，系统切模式时不会重建 → 原来只在 onCreate
     * 过的主题就再也不会跑了。与主界面/设置页同一套：`onConfigurationChanged` 叠化 + 轮询兜底。
     */
    private final Runnable themePoll = new Runnable() {
        @Override
        public void run() {
            boolean dark = ThemeManager.INSTANCE.isDark(CreatorIntroActivity.this);
            if (dark != CreatorIntroActivity.this.appliedDark) {
                CreatorIntroActivity.this.appliedDark = dark;
                Morph.crossfade(CreatorIntroActivity.this, () -> refreshThemeInPlace());
            }
            getWindow().getDecorView().postDelayed(this, 500L);
        }
    };

    boolean appliedDark;

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.appliedDark = ThemeManager.INSTANCE.isDark(this);
        Morph.crossfade(this, () -> refreshThemeInPlace());
    }

    void refreshThemeInPlace() {
        View content = findViewById(android.R.id.content);
        if (content != null) {
            ThemeManager.INSTANCE.applyTo(content);
        }
        ThemeManager.INSTANCE.applySystemBars(this);
        // 同上：极光的 shader 是建的时候取色的，必须重新取
        if (this.aurora != null) {
            this.aurora.refreshPalette();
        }
        styleScrim();
        styleGlass();
        styleTitle();
        buildFeatures();
        styleBadge();
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean dark = ThemeManager.INSTANCE.isDark(this);
        if (dark != this.appliedDark) {
            this.appliedDark = dark;
            Morph.crossfade(this, () -> refreshThemeInPlace());
        }
        getWindow().getDecorView().removeCallbacks(this.themePoll);
        getWindow().getDecorView().post(this.themePoll);
    }

    @Override
    protected void onPause() {
        getWindow().getDecorView().removeCallbacks(this.themePoll);
        super.onPause();
    }

    private void leave() {
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * alpha);
        if (a <= 0) {
            a = 1;
        }
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private void styleScrim() {
        View scrim = findViewById(R.id.intro_scrim);
        if (scrim == null) {
            return;
        }
        int bg = ThemeManager.INSTANCE.colors(this).getBackground();
        // 深色：轻压（保住极光的颜色）；浅色：压得重一点（保住文字对比度）
        boolean dark = ThemeManager.INSTANCE.isDark(this);
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                dark ? new int[]{withAlpha(bg, 0.30f), withAlpha(bg, 0.08f), withAlpha(bg, 0.85f)}
                     : new int[]{withAlpha(bg, 0.45f), withAlpha(bg, 0.15f), withAlpha(bg, 0.90f)});
        scrim.setBackground(gd);
    }

    /** 能力卡做成半透明磨砂玻璃：压在流动背景上才有「悬浮」的感觉。 */
    private void styleGlass() {
        View glass = findViewById(R.id.intro_glass);
        if (glass == null) {
            return;
        }
        ThemeColors c = ThemeManager.INSTANCE.colors(this);
        float dp = getResources().getDisplayMetrics().density;
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(22.0f * dp);
        gd.setColor(withAlpha(c.getSurface(), 0.74f));
        gd.setStroke(Math.max(1, (int) dp), withAlpha(c.getText(), 0.10f));
        glass.setBackground(gd);
    }

    private void styleTitle() {
        final TextView title = (TextView) findViewById(R.id.intro_title);
        if (title == null) {
            return;
        }
        title.post(() -> {
            ThemeColors c = ThemeManager.INSTANCE.colors(CreatorIntroActivity.this);
            int w = title.getWidth();
            if (w <= 0) {
                return;
            }
            LinearGradient shader = new LinearGradient(0.0f, 0.0f, w, title.getHeight(),
                    new int[]{c.getText(), mixColor(c.getText(), c.getPrimary(), 0.62f)},
                    new float[]{0.0f, 1.0f}, Shader.TileMode.CLAMP);
            title.getPaint().setShader(shader);
            title.invalidate();
        });
    }

    private static int mixColor(int a, int b, float t) {
        int aa = Math.round(((a >>> 24) & 0xFF) + (((b >>> 24) & 0xFF) - ((a >>> 24) & 0xFF)) * t);
        int ar = Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int ag = Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int ab = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (aa << 24) | (ar << 16) | (ag << 8) | ab;
    }

    private void styleBadge() {
        TextView badge = (TextView) findViewById(R.id.intro_badge);
        if (badge == null) {
            return;
        }
        String version = "";
        try {
            String v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            version = v == null ? "" : v;
        } catch (Throwable th) {
            version = "";
        }
        badge.setText(version.isEmpty() ? "AnimWidget" : ("AnimWidget " + version));
    }

    /** 三张能力卡（代码构建，避免为一个列表塞 6 个 id）。 */
    private void buildFeatures() {
        LinearLayout host = (LinearLayout) findViewById(R.id.intro_feat_host);
        if (host == null) {
            return;
        }
        host.removeAllViews();
        float dp = getResources().getDisplayMetrics().density;
        String[] glyphs = {"▶", "▦", "◔"};
        String[] tags = {"icon_tile", "icon_tile_2", "icon_tile_3"};
        int[] titles = {R.string.intro_f1_t, R.string.intro_f2_t, R.string.intro_f3_t};
        int[] descs = {R.string.intro_f1_d, R.string.intro_f2_d, R.string.intro_f3_d};
        for (int i = 0; i < 3; i++) {
            if (i > 0) {
                View divider = new View(this);
                divider.setTag("divider_soft");
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int) dp));
                dlp.leftMargin = (int) (dp * 18.0f);
                dlp.rightMargin = (int) (dp * 18.0f);
                divider.setLayoutParams(dlp);
                host.addView(divider);
            }
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding((int) (dp * 12.0f), (int) (dp * 10.0f), (int) (dp * 12.0f), (int) (dp * 10.0f));
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView icon = new TextView(this);
            icon.setText(glyphs[i]);
            icon.setTextSize(17.0f);
            icon.setGravity(android.view.Gravity.CENTER);
            icon.setTag(tags[i]);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams((int) (dp * 40.0f), (int) (dp * 40.0f));
            icon.setLayoutParams(ilp);
            row.addView(icon);

            LinearLayout texts = new LinearLayout(this);
            texts.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            tlp.leftMargin = (int) (dp * 12.0f);
            texts.setLayoutParams(tlp);

            TextView t = new TextView(this);
            t.setText(titles[i]);
            t.setTextSize(14.5f);
            t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
            texts.addView(t);

            TextView d = new TextView(this);
            d.setText(descs[i]);
            d.setTextSize(11.5f);
            d.setTag("muted");
            LinearLayout.LayoutParams dlp2 = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dlp2.topMargin = (int) (dp * 2.0f);
            d.setLayoutParams(dlp2);
            texts.addView(d);

            row.addView(texts);
            host.addView(row);
        }
        // 代码新增的视图也要上色（只按 tag 刷，别盖住外层卡片的底）+ 上字体
        ThemeManager.INSTANCE.styleTree(host);
        Fonts.applyTree(host);
    }

    /** 错峰入场：每个元素 42ms 递进，520ms，三次贝塞尔 OUT。 */
    private void playEntrance() {
        float dp = getResources().getDisplayMetrics().density;
        int[] ids = {R.id.intro_kicker, R.id.intro_title, R.id.intro_sub, R.id.intro_glass,
                R.id.intro_badge, R.id.intro_cta};
        PathInterpolator ease = new PathInterpolator(0.20f, 0.62f, 0.28f, 1.00f);
        for (int i = 0; i < ids.length; i++) {
            View v = findViewById(ids[i]);
            if (v == null) {
                continue;
            }
            v.setAlpha(0.0f);
            v.setTranslationY(dp * (i == 5 ? 26.0f : 16.0f));
            v.animate().alpha(1.0f).translationY(0.0f)
                    .setStartDelay(70L + i * 52L)
                    .setDuration(520L)
                    .setInterpolator(ease)
                    .start();
        }
        // 主按钮轻轻呼吸，作为收尾的一点「活气」
        View cta = findViewById(R.id.intro_cta);
        if (cta != null) {
            ValueAnimator a = ValueAnimator.ofFloat(0.0f, 1.0f);
            a.setDuration(2100L);
            a.setRepeatCount(ValueAnimator.INFINITE);
            a.setRepeatMode(ValueAnimator.REVERSE);
            a.setStartDelay(700L);
            a.setInterpolator(new PathInterpolator(0.45f, 0.00f, 0.40f, 1.00f));
            a.addUpdateListener(animation -> {
                float f = (float) animation.getAnimatedValue();
                float s = 1.0f + 0.014f * f;
                cta.setScaleX(s);
                cta.setScaleY(s);
            });
            this.breath = a;
            a.start();
        }
    }

    @Override
    protected void onTextsRefreshed() {
        styleScrim();
        styleGlass();
        styleTitle();
        styleBadge();
        buildFeatures();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.breath != null) {
            this.breath.cancel();
            this.breath = null;
        }
    }

    /** 首次进入才需要（MainActivity 里调用）。 */
    static boolean shouldShow(android.content.Context c) {
        return !c.getSharedPreferences(PREFS, 0).getBoolean(SEEN, false);
    }

    static void open(android.app.Activity from) {
        from.startActivity(new Intent(from, CreatorIntroActivity.class));
    }
}
