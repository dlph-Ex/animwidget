package io.github.animwidget;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * v1.10.107（需求 1）：**判断代号页**（全判断代号查看）。
 *
 * 逐条固定五段、顺序不可变：生成前提 → 生成要求 → 因为什么而生成 → 判断文本 → 完整含义。
 * · 每种代号只出现一次，独占一行开头；
 * · 代号之间是一条**居中短分隔线**（宽度 44%、不贴边、8% 透明度），不抢易读性；
 * · 支持导出 TXT（格式与日志导出一致，落在下载目录并写入「历史导出」）；
 * · **不提供**应用内长图/截屏导出 —— 想要图直接系统长截屏即可，所以本页不设 FLAG_SECURE；
 * · 整页固定中文简体 + 英文代号，**不跟随应用语言与字体**（故意继承 Activity 而不是 I18nActivity，
 *   并且每个 TextView 都打 no_font 标记，避免被全局换字覆盖）。
 */
public final class DiagCatalogActivity extends Activity {

    @Nullable
    private TextView hintView;

    /** v1.10.185：本页颜色全是手搓的（不走 applyTo），换肤只能整页重建；重建后打 H02。 */
    boolean appliedDark;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashLog.install(this);
        EdgeToEdge.INSTANCE.apply(this);
        this.appliedDark = ThemeManager.INSTANCE.isDark(this);
        applyPage();
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            ThemeSwitch.checkNow(this, "configChange");
        } catch (Throwable ignored) {
        }
        boolean dark = ThemeManager.INSTANCE.isDark(this);
        if (dark != this.appliedDark) {
            this.appliedDark = dark;
            applyPage();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeSwitch.arm(this);
    }

    @Override
    protected void onPause() {
        ThemeSwitch.disarm(this);
        super.onPause();
    }

    private void applyPage() {
        ThemeColors c = ThemeManager.INSTANCE.colors(this);
        setContentView(buildPage(c));
        View content = findViewById(android.R.id.content);
        if (content != null) {
            content.setBackgroundColor(c.getBackground());
        }
        ThemeManager.INSTANCE.applySystemBars(this);
        try {
            ThemeSwitch.refreshed(this, getClass().getSimpleName(), 1, ThemeSwitch.layers(),
                    "方式=整页重建");
        } catch (Throwable ignored) {
        }
    }

    private View buildPage(ThemeColors c) {
        float dp = getResources().getDisplayMetrics().density;

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        // 铺满整块内容区（否则 LinearLayout 只包内容，四周会露出窗口底色）
        page.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        page.setPadding((int) (dp * 16.0f), (int) (dp * 52.0f), (int) (dp * 16.0f), 0);
        page.setBackgroundColor(c.getBackground());

        // 标题行
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("判断代号");
        title.setTextSize(24.0f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(c.getText());
        title.setTag("no_font");
        title.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        head.addView(title);

        Button back = new Button(this);
        back.setText("返回");
        back.setTextSize(13.0f);
        back.setAllCaps(false);
        back.setTextColor(c.getText());
        back.setTypeface(Typeface.DEFAULT);
        back.setTag("no_font");
        back.setBackground(ThemeManager.INSTANCE.rounded(this, c.getNeutral(), 999.0f));
        back.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) (dp * 42.0f)));
        back.setOnClickListener(v -> finish());
        head.addView(back);
        page.addView(head);

        TextView sub = new TextView(this);
        sub.setText("共 " + DiagCatalog.count() + " 个代号 · 固定五段 · 不跟随语言与字体");
        sub.setTextSize(11.0f);
        sub.setTypeface(Typeface.DEFAULT);
        sub.setTag("no_font");
        sub.setTextColor(c.getTextMuted());
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = (int) (dp * 6.0f);
        sub.setLayoutParams(subLp);
        page.addView(sub);

        // 列表
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, (int) (dp * 10.0f), 0, (int) (dp * 12.0f));
        scroll.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scroll.setLayoutParams(scrollLp);
        page.addView(scroll);

        for (int i = 0; i < DiagCatalog.count(); i++) {
            if (i > 0) {
                list.addView(divider(c, dp));
            }
            list.addView(entry(DiagCatalog.ENTRIES[i], c, dp));
        }

        // 底部：导出 TXT（胶囊）
        Button export = new Button(this);
        export.setText("导出 TXT");
        export.setTextSize(13.5f);
        export.setAllCaps(false);
        export.setTypeface(Typeface.DEFAULT);
        export.setTag("no_font");
        export.setTextColor(c.getPrimary());
        export.setBackground(ThemeManager.INSTANCE.rounded(this,
                withAlpha(c.getPrimary(), 0.14f), 999.0f));
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (dp * 48.0f));
        elp.topMargin = (int) (dp * 8.0f);
        elp.bottomMargin = (int) (dp * 16.0f);
        export.setLayoutParams(elp);
        export.setOnClickListener(v -> exportTxt());
        page.addView(export);
        return page;
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * alpha);
        if (a <= 0) {
            a = 1;
        }
        return (a << 24) | (color & 0x00FFFFFF);
    }

    /** 代号之间的分隔线：只占中间一小段，不贴边、不抢读。 */
    private View divider(ThemeColors c, float dp) {
        View line = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int) dp));
        lp.topMargin = (int) (dp * 10.0f);
        lp.bottomMargin = (int) (dp * 10.0f);
        lp.leftMargin = (int) (getResources().getDisplayMetrics().widthPixels * 0.28f);
        lp.rightMargin = (int) (getResources().getDisplayMetrics().widthPixels * 0.28f);
        line.setLayoutParams(lp);
        line.setBackgroundColor(withAlpha(c.getText(), 0.08f));
        return line;
    }

    private View entry(String[] row, ThemeColors c, float dp) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView code = new TextView(this);
        code.setText(row[0]);
        code.setTextSize(14.0f);
        code.setTypeface(Typeface.DEFAULT_BOLD);
        code.setTag("no_font");
        code.setTextColor(c.getPrimary());
        code.setLetterSpacing(0.06f);
        box.addView(code);

        for (int i = 0; i < DiagCatalog.FIELD_NAMES.length; i++) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lineLp.topMargin = (int) (dp * (i == 0 ? 6.0f : 3.0f));
            line.setLayoutParams(lineLp);

            TextView key = new TextView(this);
            key.setText(DiagCatalog.FIELD_NAMES[i]);
            key.setTextSize(10.5f);
            key.setTypeface(Typeface.DEFAULT);
            key.setTag("no_font");
            key.setTextColor(c.getTextMuted());
            key.setLayoutParams(new LinearLayout.LayoutParams((int) (dp * 86.0f),
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            line.addView(key);

            TextView value = new TextView(this);
            value.setText(row[i + 1]);
            value.setTextSize(12.5f);
            value.setTypeface(Typeface.DEFAULT);
            value.setTag("no_font");
            value.setTextColor(c.getText());
            value.setLineSpacing(0.0f, 1.15f);
            value.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            line.addView(value);

            box.addView(line);
        }
        return box;
    }

    private String buildText() {
        StringBuilder sb = new StringBuilder();
        String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                .format(new Date());
        sb.append("AnimWidget 判断代号表\n");
        sb.append("导出时间：").append(stamp).append('\n');
        sb.append("条目数：").append(DiagCatalog.count()).append('\n');
        for (int i = 0; i < DiagCatalog.count(); i++) {
            String[] row = DiagCatalog.ENTRIES[i];
            sb.append("----------------------------------------\n");
            sb.append(row[0]).append('\n');
            for (int f = 0; f < DiagCatalog.FIELD_NAMES.length; f++) {
                sb.append(DiagCatalog.FIELD_NAMES[f]).append('：')
                        .append(row[f + 1]).append('\n');
            }
        }
        return sb.toString();
    }

    private void exportTxt() {
        try {
            String where = LogExporter.exportTxt(this, buildText());
            Toast.makeText(this, "已导出：" + where + "\n（可在「历史导出」中查看）",
                    Toast.LENGTH_LONG).show();
        } catch (Throwable th) {
            Toast.makeText(this, "导出失败：" + th.getClass().getSimpleName(),
                    Toast.LENGTH_LONG).show();
        }
    }
}
