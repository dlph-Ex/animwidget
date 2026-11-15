package io.github.animwidget;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v1.10.359：「关于」弹层。
 *
 * <h3>为什么是弹层而不是新 Activity</h3>
 * 用户要求「弹出/关闭动画直接复用赞助界面的；背景高斯模糊、巨型光轮也与赞助一致」。
 * 赞助页（{@link RewardSheet}）本身就是挂在宿主 content 上的覆盖层，模糊底与弹簧入场都在
 * 那一层实现 —— 于是一个 Activity 就没有「复用」可言，只能照着重写一遍。这里做成同样的
 * 覆盖层，直接拿 {@link BlurBackdropView} 和 {@link SpringAnim} 的现成件。
 *
 * <h3>本阶段的范围（用户明确要求）</h3>
 * <ul>
 *   <li>**不做任何跳转**：仓库 / 许可证 / 致谢 都只有按压反馈，点了不动作；</li>
 *   <li>v1.10.364（用户要求）：**关闭按钮整个删掉**，改成页面最下方一行提示
 *       「点击空白处关闭页面」；关闭只有「点面板外的模糊底」和返回键两条路。
 *       「关于」小字挪到顶部应用卡的**右下角**（用户圈的是蓝框，不是白框）。</li>
 *   <li>返回键等价于关闭。</li>
 * </ul>
 */
public final class AboutSheet {

    /** 同一时刻只允许一个。 */
    private static AboutSheet current;

    private static final float PAD = 20.0f;
    private static final float RADIUS_CARD = 28.0f;
    private static final float RADIUS_TILE = 14.0f;
    private static final float RADIUS_ROW = 14.0f;
    private static final float TILE = 46.0f;
    private static final float ROW_H = 54.0f;

    private final Activity host;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private FrameLayout root;
    private BlurBackdropView backdrop;
    private LinearLayout panel;
    private boolean closing;
    /** 检测更新进行中（防连点）。 */
    private boolean updateChecking;
    /** true = 这次重建**不跑入场动画**（换肤重建用：不能又弹一次）。 */
    private boolean quiet;

    private AboutSheet(Activity activity) {
        this.host = activity;
    }

    // ------------------------------------------------------------------ 入口 / 返回

    public static void show(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        AboutSheet open = current;
        if (open != null) {
            if (open.host == activity) {
                return;
            }
            open.detach();
        }
        new AboutSheet(activity).attach();
    }

    /** 返回键：true = 被弹层消费掉了。 */
    public static boolean handleBack() {
        AboutSheet open = current;
        if (open == null) {
            return false;
        }
        if (open.root == null || open.root.getParent() == null) {
            open.detach();
            return false;
        }
        open.close();
        return true;
    }

    /**
     * v1.10.366：**宿主换肤之后必须叫一下这里**。
     *
     * <p>这个弹层不是「跟着主题自动重画」的：它在 {@link #attachInner()} 里把
     * {@link ThemeColors} 一次烘进每个 GradientDrawable 和每处 setTextColor，
     * 之后没人再碰它们。所以系统切换深浅色时，页面已经换了、**弹层还是旧配色**
     * （模糊底是一张旧截图，也是旧的）—— 用户看到的就是「关闭界面后才变」。
     *
     * <p>修法：整块拆掉重建（重新截图 + 全新配色），但不放入场动画 —— 换肤不该再弹一次。
     */
    public static void onHostThemeChanged() {
        AboutSheet open = current;
        if (open == null || open.root == null || open.root.getParent() == null) {
            return;
        }
        open.rebuild();
    }

    public static boolean isOpen() {
        AboutSheet open = current;
        return open != null && open.root != null && open.root.getParent() != null;
    }

    // ------------------------------------------------------------------ 搭建

    private void attach() {
        try {
            attachInner();
        } catch (Throwable th) {
            Diag.warn(this.host, "N01", "关于弹层构建异常: " + th.getClass().getSimpleName());
            detach();
        }
    }

    private void attachInner() {
        View contentView = this.host.findViewById(android.R.id.content);
        if (!(contentView instanceof ViewGroup)) {
            return;
        }
        ViewGroup content = (ViewGroup) contentView;
        if (content.getWidth() <= 0 || content.getHeight() <= 0) {
            return;
        }
        final ThemeColors c = ThemeManager.INSTANCE.colors(this.host);
        final boolean dark = ThemeManager.INSTANCE.isDark(this.host);
        final float d = this.host.getResources().getDisplayMetrics().density;

        // 模糊底：先截图、再挂载，所以不会把自己画进去
        Bitmap shot = capture(content);
        FrameLayout layer = new FrameLayout(this.host);
        layer.setClickable(true);
        this.root = layer;
        this.backdrop = new BlurBackdropView(this.host, shot, dark ? 0x52000000 : 0x2E000000);
        this.backdrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                close();
            }
        });
        layer.addView(this.backdrop, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ---- 面板：居中卡片
        LinearLayout card = new LinearLayout(this.host);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(c.getSurface(), RADIUS_CARD, d));
        int pad = Math.round(PAD * d);
        // v1.10.365：底部从 18dp 收到 12dp —— 提示行下面那一大片空白就是它撑出来的
        card.setPadding(pad, Math.round(14.0f * d), pad, Math.round(12.0f * d));
        // v1.10.367（用户反馈：界面内也会关）：**面板自己必须吃掉点击**。
        // 不设 clickable 时，点在卡内没有子控件的空白处，命中测试会一路落空 ——
        // 事件穿透到下面的模糊底（全屏 + 关闭监听），表现就是「点卡片空白处也关页面」。
        card.setClickable(true);
        this.panel = card;

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        lp.leftMargin = pad;
        lp.rightMargin = pad;
        layer.addView(card, lp);

        // ---- 应用卡：图标 + 名称 +（右端）关闭 + 关于
        //
        // v1.10.363（用户截图上那两根红箭头）：上一版「关闭（左）/ 关于（右）」单独占一行，
        // 箭头却都指进**下面这张应用卡** —— 两个都要落到这张卡的右端、垂直居中：
        //   · 关闭：从标题行右侧水平移进卡内（箭头 = 横穿）；
        //   · 关于：原地垂直下移一行（箭头 = 一条几乎垂直的线）。
        LinearLayout appCard = new LinearLayout(this.host);
        appCard.setOrientation(LinearLayout.HORIZONTAL);
        appCard.setGravity(Gravity.CENTER_VERTICAL);
        appCard.setBackground(rounded(c.getNeutral(), RADIUS_TILE, d));
        appCard.setPadding(Math.round(14.0f * d), Math.round(14.0f * d),
                Math.round(14.0f * d), Math.round(14.0f * d));
        LinearLayout.LayoutParams appLp = matchWrap();
        card.addView(appCard, appLp);

        // 图标：用应用自己的启动图标，避免再造一套视觉
        android.widget.ImageView icon = new android.widget.ImageView(this.host);
        try {
            icon.setImageDrawable(this.host.getPackageManager()
                    .getApplicationIcon(this.host.getPackageName()));
        } catch (Throwable ignored) {
        }
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(
                Math.round(TILE * d), Math.round(TILE * d));
        iconLp.rightMargin = Math.round(12.0f * d);
        appCard.addView(icon, iconLp);

        LinearLayout nameBox = new LinearLayout(this.host);
        nameBox.setOrientation(LinearLayout.VERTICAL);
        appCard.addView(nameBox, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView appName = new TextView(this.host);
        appName.setText(R.string.app_name);
        appName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);
        appName.setTextColor(c.getText());
        nameBox.addView(appName, matchWrap());

        TextView tagline = new TextView(this.host);
        tagline.setText(R.string.about_tagline);
        tagline.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
        // 不能再给 neutral：这张卡本身就是 neutral，同色 = 隐形（列表右侧的值文字踩过同一个坑）
        tagline.setTextColor(withAlpha(c.getText(), 0.62f));
        tagline.setMaxLines(1);
        tagline.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tagLp = matchWrap();
        tagLp.topMargin = Math.round(3.0f * d);
        nameBox.addView(tagline, tagLp);

        // 关于：卡内**右下角**的小字（v1.10.364：用户圈蓝框的位置就在「关闭」下面一整行，
        // 也就是贴着卡片下内边距，而不是与名字同一行）。关闭按钮已按用户要求删除。
        TextView title = new TextView(this.host);
        title.setText(R.string.about_title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.0f);
        title.setTextColor(withAlpha(c.getText(), 0.55f));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.leftMargin = Math.round(10.0f * d);
        // 横向排列里给子视图单独指定 gravity=BOTTOM，就贴在行内容的底边
        titleLp.gravity = Gravity.BOTTOM;
        appCard.addView(title, titleLp);

        // ---- 信息列表
        LinearLayout list = new LinearLayout(this.host);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackground(rounded(c.getNeutral(), RADIUS_TILE, d));
        LinearLayout.LayoutParams listLp = matchWrap();
        listLp.topMargin = Math.round(14.0f * d);
        card.addView(list, listLp);

        String system = "Android " + (Build.VERSION.RELEASE == null ? "?" : Build.VERSION.RELEASE);
        // 注意 last：**只有最后一行才传 true**。上一版全传反了，
        // 结果只在「系统」下面画了一条线 —— 用户一眼就看出来「分隔线都没有」。
        // v1.10.386（需求）：版本行点一下 = 检测更新（前面挂一个「刷新」符号）
        addRow(list, c, d, R.string.about_version, versionLine(), false,
                GlyphView.REFRESH, this::checkUpdate);
        addRow(list, c, d, R.string.about_system, system, false);
        // v1.10.386（需求）：开源仓库行点一下 = 复制 / 跳转 / 取消（前面挂一个链接符号）
        addRow(list, c, d, R.string.about_repo, this.host.getString(R.string.about_repo_value),
                false, GlyphView.LINK, this::showRepoDialog);
        addRow(list, c, d, R.string.about_license, R.string.about_license_value, false);
        addRow(list, c, d, R.string.about_thanks, R.string.about_thanks_value, false);
        addRow(list, c, d, R.string.about_qq, R.string.about_qq_value, false);
        // v1.10.513（需求①）：**用户可核对的身份**放在最后两行。
        // 「非官方构建」只在签名不符时出现；「签名证书」永远显示 —— 它是用户唯一能自己核对的凭据
        // （和 Release 说明里那串十六进制对着看）。不弹窗、不打断，改包的人也不好把这两行悄悄删掉
        // 而不引起注意（删了门禁 [33] 会红）。
        if (Integrity.restricted(this.host)) {
            addRow(list, c, d, R.string.integrity_unofficial,
                    this.host.getString(R.string.integrity_hint), false);
        }
        addCertRow(list, c, d);

        // ---- 页面最下方的一行提示（v1.10.364 用户要求）
        // 底色**沿用表面色**（就是卡片自己的 surface），文字色与「关于」小字同源；
        // 关闭按钮已删掉 —— 现在关闭只有「点面板外的空白」和返回键两条路，所以把这句写出来。
        TextView hint = new TextView(this.host);
        hint.setText(R.string.about_close_hint);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
        hint.setTextColor(withAlpha(c.getText(), 0.55f));
        hint.setGravity(Gravity.CENTER);
        hint.setBackground(rounded(c.getSurface(), RADIUS_TILE, d));
        // 不再给上下内边距：这一行只要占住文字本身的高度（原来上下各 10dp，
        // 加上 12dp 边距 + 18dp 卡底，合计约 66dp 的空白带，用户截图说「太多了」）
        hint.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = Math.round(8.0f * d);
        card.addView(hint, hintLp);

        // 挂到宿主上
        content.addView(layer, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        current = this;
        if (this.quiet) {
            // 换肤重建：直接以静止姿态接上（模糊底已经换成新主题的了）
            this.quiet = false;
            if (this.backdrop != null) {
                this.backdrop.setAlpha(1.0f);
            }
            if (this.panel != null) {
                this.panel.setScaleX(1.0f);
                this.panel.setScaleY(1.0f);
                this.panel.setTranslationY(0.0f);
                this.panel.setAlpha(1.0f);
            }
        } else {
            playEnter();
        }
    }

    /**
     * v1.10.516（用户：「显示不全」）：**签名证书**单独一行 —— 标题一行、完整指纹两行、点一下复制。
     *
     * <p>为什么不能走 {@link #addRow}：它把值固定成 {@code maxLines=1} + 末尾省略号，
     * 95 个字符必然被吃掉一半；而 515 那种「只显示前 8 组」用户也没法逐段核对。</p>
     *
     * <p>排版口径：标题 13sp（与其它行同号）+ 指纹 10.5sp、最多 3 行（窄屏兜底，绝不省略），
     * 行高用"最小 54dp + 自适应"—— 两行指纹时正好落在 54dp 里，三行时自然变高，不会被裁。</p>
     */
    private void addCertRow(LinearLayout parent, final ThemeColors c, float d) {
        LinearLayout row = new LinearLayout(this.host);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setClickable(true);
        row.setFocusable(true);
        row.setMinimumHeight(Math.round(ROW_H * d));
        row.setPadding(Math.round(14.0f * d), Math.round(9.0f * d),
                Math.round(12.0f * d), Math.round(9.0f * d));
        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this.host);
        title.setText(R.string.about_cert);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.0f);
        title.setTextColor(c.getText());
        row.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final String full = Integrity.fingerprintFull(this.host);
        TextView value = new TextView(this.host);
        value.setText(full == null ? this.host.getString(R.string.integrity_cert_unknown)
                : twoLines(full));
        value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        value.setTextColor(c.getText());
        value.setAlpha(0.62f);
        // v1.10.517：**行数不设上限**。516 写了 maxLines(3)，一旦某台窄屏把每行又折成两行，
        // 就会在第三行截断 —— 那正是"显示不全"的复现路径。这里宁可变高也不许裁。
        value.setLineSpacing(0.0f, 1.06f);
        LinearLayout.LayoutParams valueLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        valueLp.topMargin = Math.round(3.0f * d);
        row.addView(value, valueLp);

        pressFeedback(row);
        if (full != null) {
            row.setOnClickListener(x -> copyCert(full));
        }
    }

    /**
     * v1.10.517（用户反馈「显示不全」的真实原因）：把 95 字符的点分指纹**按字节对齐**折成两行。
     *
     * <p>自动换行是按"能塞多少字"折的，实测正好切在字节中间（{@code …88:29:5} / {@code 5:A6:…}），
     * 把 {@code 55} 劈成两半 —— 内容是全的，但看起来就像少了一截。这里显式在**第 16 个字节之后**
     * 插入换行（16 字节 = 47 个字符），两行都是 47 字符，常见宽度下必放得下，且永远切在冒号处。</p>
     */
    @NotNull
    private static String twoLines(@NotNull String dotted) {
        // 32 字节 × 2 + 31 个冒号 = 95 个字符；第 16 字节末尾的冒号在下标 47。
        //
        // v1.10.522：**换行处的冒号不能丢**。v1.10.517 折行时用
        // {@code substring(0, 47) + "\n" + substring(48)} —— 47 是「第 16 个字节之后的那个冒号」
        // 的下标，于是两行各吃掉半个分隔符：上行末尾没有冒号、下行又少一个字节的分隔。
        // 现在第一行**连那个冒号一起带走**（0..47，共 48 字符），第二行从下标 48 开始，
        // 折点永远落在「:」之后，逐字节核对不会以为少了一段。
        if (dotted.length() < 48) {
            return dotted;
        }
        return dotted.substring(0, 48) + "\n" + dotted.substring(48);
    }

    /** 点一下把完整指纹放进剪贴板 —— 除了截图，多一条能拿去逐段核对的路径。 */
    private void copyCert(@NotNull String full) {
        try {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    this.host.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(android.content.ClipData.newPlainText("cert", full));
            }
            Toast.makeText(this.host, R.string.about_cert_copied, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }

    /**
     * 一行「标题 …… 值」。本阶段**只做按压反馈、不跳转**（用户明确要求），
     * 所以 onClick 里什么都不做 —— 但保留可点状态，让按下有回弹。
     */
    private void addRow(LinearLayout parent, final ThemeColors c, float d,
                        int titleRes, int valueRes, boolean last) {
        addRow(parent, c, d, titleRes, this.host.getString(valueRes), last, -1, null);
    }

    private void addRow(LinearLayout parent, final ThemeColors c, float d,
                        int titleRes, String value, boolean last) {
        addRow(parent, c, d, titleRes, value, last, -1, null);
    }

    /**
     * v1.10.386：可带「值前缀图形」和点击动作的行。
     *
     * @param glyph  {@link GlyphView} 的图形代号，-1 = 不要前缀（开源仓库行 = LINK，版本行 = RESET）
     * @param action 点击动作，null = 只有按压反馈（本阶段「许可证 / 致谢」仍是这种）
     */
    private void addRow(LinearLayout parent, final ThemeColors c, float d,
                        int titleRes, String value, boolean last, int glyph, @Nullable Runnable action) {
        addRowBody(parent, c, d, titleRes, value, last, glyph, action);
    }

    private void addRowBody(LinearLayout parent, final ThemeColors c, float d,
                            int titleRes, String value, boolean last, int glyph, @Nullable Runnable action) {
        LinearLayout row = new LinearLayout(this.host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClickable(true);
        row.setFocusable(true);
        row.setPadding(Math.round(14.0f * d), 0, Math.round(12.0f * d), 0);
        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(ROW_H * d)));

        TextView k = new TextView(this.host);
        k.setText(titleRes);
        k.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.0f);
        k.setTextColor(c.getText());
        row.addView(k, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView v = new TextView(this.host);
        v.setText(value);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.0f);
        // 值不能再用 neutral：列表底色本身就是 neutral，同色 = 隐形（上一版右侧全空就是它）
        v.setTextColor(c.getText());
        v.setAlpha(0.62f);
        v.setMaxLines(1);
        v.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (glyph < 0) {
            row.addView(v, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        } else {
            // v1.10.386：值左边挂一个「与文本等高」的自绘图形（链接 / 检测更新）
            LinearLayout valueBox = new LinearLayout(this.host);
            valueBox.setOrientation(LinearLayout.HORIZONTAL);
            valueBox.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(valueBox, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            GlyphView gv = new GlyphView(this.host);
            gv.setGlyph(glyph);
            gv.setTintColor(withAlpha(c.getText(), 0.62f));
            int side = Math.round(18.0f * d);
            LinearLayout.LayoutParams gvLp = new LinearLayout.LayoutParams(side, side);
            gvLp.rightMargin = Math.round(5.0f * d);
            valueBox.addView(gv, gvLp);
            valueBox.addView(v, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        pressFeedback(row);
        if (action != null) {
            row.setOnClickListener(x -> action.run());
        }

        if (!last) {
            View line = new View(this.host);
            // 分隔线：从正文色往背景混 18% —— 深浅色都看得见，且不会抢视线
            line.setBackgroundColor(blend(c.getText(), c.getSurface(), 0.18f));
            LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Math.round(d * 0.7f)));
            lineLp.leftMargin = Math.round(14.0f * d);
            lineLp.rightMargin = Math.round(14.0f * d);
            parent.addView(line, lineLp);
        }
    }

    /** 「1.10.360 · vc 475 · minSdk 26」：运行时读，避免像上一版那样把版本号写死在资源里。 */
    private String versionLine() {
        String name = "";
        String code = "";
        try {
            android.content.pm.PackageInfo info = this.host.getPackageManager()
                    .getPackageInfo(this.host.getPackageName(), 0);
            name = info.versionName == null ? "" : info.versionName;
            code = Build.VERSION.SDK_INT >= 28
                    ? String.valueOf(info.getLongVersionCode())
                    : String.valueOf(info.versionCode);
        } catch (Throwable ignored) {
        }
        return name + " · vc " + code + " · minSdk 26";
    }

    /** 按压反馈：只做轻微缩放（不是 Q 弹，Q 弹留给入口胶囊）。 */
    private void pressFeedback(final View v) {
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, android.view.MotionEvent e) {
                switch (e.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        view.animate().cancel();
                        view.animate().alpha(0.62f).setDuration(70L).start();
                        return true;
                    case android.view.MotionEvent.ACTION_UP:
                        view.animate().cancel();
                        view.animate().alpha(1.0f).setDuration(140L).start();
                        // v1.10.386：触摸被这里吃掉后 onClick 不会自己触发 —— 手动补一次，
                        // 否则「开源仓库 / 版本」两行的点击动作永远进不去。
                        view.performClick();
                        return true;
                    case android.view.MotionEvent.ACTION_CANCEL:
                        view.animate().cancel();
                        view.animate().alpha(1.0f).setDuration(140L).start();
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    /**
     * 拆掉旧面板、按当前主题重建一份。
     *
     * <p>先 `removeView` 再重截模糊底：{@code capture()} 会把 content 上的所有子视图
     * 一起画进去（包括这个覆盖层），不先摘掉就会拍到「旧弹层套旧弹层」。
     */
    private void rebuild() {
        View old = this.root;
        if (this.closing || old == null) {
            return;
        }
        ViewGroup area = (ViewGroup) old.getParent();
        if (area == null) {
            detach();
            return;
        }
        this.quiet = true;
        this.ui.removeCallbacksAndMessages(null);
        area.removeView(old);
        this.root = null;
        this.panel = null;
        this.backdrop = null;
        // 此刻宿主页已经按新主题上过色（宿主先调 refreshThemeInPlace 再调这里），
        // 所以重新截到的模糊底就是新主题的底
        attachInner();
        try {
            ThemeSwitch.refreshed(this.host, this.host.getClass().getSimpleName(), 2,
                    ThemeSwitch.layers(), "AboutSheet 重建 深色=" + ThemeManager.INSTANCE.isDark(this.host));
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------ 仓库 / 检测更新（v1.10.386）

    /** 「开源仓库」行：复制 / 跳转 / 取消。 */
    private void showRepoDialog() {
        try {
            RepoDialog.show(this.host, UpdateChecker.REPO_URL,
                    this.host.getString(R.string.about_repo_value));
        } catch (Throwable th) {
            Diag.warn(this.host, "N01", "仓库弹窗构建异常: " + th.getClass().getSimpleName());
        }
    }

    /** 「版本」行：向 GitHub Releases 问最新 tag，比当前 versionName 大就提示去下载。 */
    private void checkUpdate() {
        if (this.updateChecking) {
            return;
        }
        this.updateChecking = true;
        Toast.makeText(this.host, R.string.about_update_checking, Toast.LENGTH_SHORT).show();
        UpdateChecker.check(this.host, (ok, tag, error) -> {
            this.updateChecking = false;
            if (!ok) {
                Diag.warn(this.host, "N01", "检测更新失败 原因=" + error);
                Toast.makeText(this.host, R.string.about_update_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            String cur = currentVersionName();
            if (UpdateChecker.compare(tag, cur) > 0) {
                new ConfirmDialog(this.host)
                        .title(this.host.getString(R.string.about_update_title))
                        .message(this.host.getString(R.string.about_update_found, tag, cur))
                        .confirm(this.host.getString(R.string.about_update_open))
                        .cancel(this.host.getString(R.string.cancel))
                        .glyph(GlyphView.REFRESH)
                        .onConfirm(() -> RepoDialog.openUrl(this.host, UpdateChecker.RELEASES_URL))
                        .show();
            } else {
                Toast.makeText(this.host,
                        this.host.getString(R.string.about_update_latest, cur),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String currentVersionName() {
        try {
            android.content.pm.PackageInfo info = this.host.getPackageManager()
                    .getPackageInfo(this.host.getPackageName(), 0);
            return info.versionName == null ? "" : info.versionName;
        } catch (Throwable th) {
            return "";
        }
    }

    // ------------------------------------------------------------------ 动画

    private void playEnter() {
        if (this.backdrop != null) {
            this.backdrop.setAlpha(0.0f);
            this.backdrop.animate().alpha(1.0f).setDuration(200L).start();
        }
        if (this.panel == null) {
            return;
        }
        this.panel.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        this.panel.setAlpha(0.0f);
        // 与赞助页同一套：0.86 → 过冲 → 1.0，同时从下方 26dp 弹上来
        SpringAnim.popIn(this.panel, 0.86f, px(26.0f), 140L, SpringAnim.POP);
        this.ui.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (AboutSheet.this.panel != null) {
                    AboutSheet.this.panel.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }
        }, SpringAnim.POP.durationMs() + 30L);
    }

    private void close() {
        if (this.closing) {
            return;
        }
        this.closing = true;
        if (this.backdrop != null) {
            this.backdrop.animate().alpha(0.0f).setDuration(160L).start();
        }
        if (this.panel != null) {
            this.panel.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            SpringAnim.popOut(this.panel, 0.90f, px(18.0f), new Runnable() {
                @Override
                public void run() {
                    detach();
                }
            }, SpringAnim.CLOSE);
        } else {
            detach();
        }
    }

    private void detach() {
        this.closing = false;
        this.ui.removeCallbacksAndMessages(null);
        FrameLayout layer = this.root;
        this.root = null;
        this.panel = null;
        this.backdrop = null;
        if (current == this) {
            current = null;
        }
        if (layer != null) {
            try {
                ViewGroup parent = (ViewGroup) layer.getParent();
                if (parent != null) {
                    parent.removeView(layer);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    // ------------------------------------------------------------------ 小工具

    /** 把当前 content 画进一张位图（给模糊底用）。**先截、再挂载**。 */
    private Bitmap capture(ViewGroup content) {
        try {
            Bitmap b = Bitmap.createBitmap(content.getWidth(), content.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(b);
            content.draw(canvas);
            return b;
        } catch (Throwable th) {
            return null;
        }
    }

    private static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static GradientDrawable rounded(int color, float radiusDp, float density) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(color);
        bg.setCornerRadius(radiusDp * density);
        return bg;
    }

    /** 取某个颜色的同色浅化版（alpha 0~1）：在 neutral 卡上做「比底色亮一档」的胶囊用。 */
    private static int withAlpha(int color, float alpha) {
        int a = Math.round(alpha * 255.0f);
        if (a <= 0) {
            a = 1;
        }
        if (a > 255) {
            a = 255;
        }
        return (a << 24) | (color & 0x00FFFFFF);
    }

    /** 两色线性混合（用来给分隔线取一个「比底色深一点」的值）。 */
    private static int blend(int a, int b, float t) {
        float k = Math.max(0.0f, Math.min(1.0f, t));
        int r = Math.round(Color.red(a) * k + Color.red(b) * (1.0f - k));
        int g = Math.round(Color.green(a) * k + Color.green(b) * (1.0f - k));
        int bl = Math.round(Color.blue(a) * k + Color.blue(b) * (1.0f - k));
        return Color.rgb(r, g, bl);
    }

    private int px(float dp) {
        return Math.round(dp * this.host.getResources().getDisplayMetrics().density);
    }
}
