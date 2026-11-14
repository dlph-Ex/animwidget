package io.github.animwidget;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowInsets;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * v1.10.183：**赞赏弹层第三次重做 —— 不再是 Dialog**。
 *
 * ## 为什么把 Dialog 整个扔掉
 * 181/182 两版都有同一个现象：弹层四周留一圈「没被处理过」的页面，表现为
 *   · 左右各约 10dp 的原始页面（模糊层在它里面，看起来像「两个图层」）；
 *   · 顶部一条状态栏高度的原始页面；底部面板之外还露出下一张卡片 —— 一点没模糊；
 *   · 面板贴在窗口底部，于是「左下右下角」是直角。
 * 根因不在布局代码里，在 **Dialog 的窗口**：`setLayout(MATCH_PARENT, MATCH_PARENT)` +
 * `FLAG_LAYOUT_IN_SCREEN` + `setDecorFitsSystemWindows(false)` 全写了，窗口仍然不是整屏
 * —— 我们画的任何东西都只能填满「窗口」，窗口之外就是原样的页面，
 * 于是「模糊 / 压暗」永远缺一圈，怎么调都不对。
 *
 * 现在改成：**把整棵覆盖层挂到 Activity 的 content 视图上**（普通 View，不是窗口）。
 *   · 覆盖层与页面在同一个窗口、同一套坐标系 → 模糊底与背景 1:1 对齐，四周不可能漏；
 *   · 非沉浸的系统上 content 不含状态栏/导航栏 → 用负 margin 把覆盖层往外顶满窗口
 *     （同时临时关掉祖先的 clipChildren），仍然是整屏；
 *   · 返回键由 {@link #handleBack()} 接（{@code DeveloperActivity.onBackPressed} 里调一句）。
 *
 * ## 形态：**居中的卡片**（用户要求「移到屏幕中央」+「左下右下角要是圆角」）
 * 四角统一 28dp 圆角、宽度 min(屏宽−40dp, 560dp)、高度上限安全区的 86%，
 * 超出只让内部 ScrollView 滚。拖拽条去掉了 —— 居中卡片上的拖拽条是错误暗示。
 *
 * ## 其余（v1.10.182 已做、这版保留）
 * · 模糊底 {@link BlurBackdropView}（截 content → 缩 1/6 → 四遍盒式模糊 → 叠压暗）；
 * · 图标底板随主题、品牌色不动；两张收款码按像素重抠（840×840 纯白四边）；
 * · 关闭按钮是独立胶囊；行有选中态；
 * · 二维码**后台预解码** + 展开动画**只动高度**（不碰 alpha / elevation），并打 `N02` 帧统计。
 */
public final class RewardSheet {

    /** 同一时刻只允许一个（重复点「赞赏」不会叠两层）。 */
    private static RewardSheet current;

    /** Ko-fi 主页：按用户给的链接**原样**使用，不做任何改写。 */
    private static final String KOFI_URL = "https://ko-fi.com/frost_doplhin";

    // ---------------------------------------------------------------- 尺寸体系（dp）
    private static final float PAD = 20.0f;
    /** 卡片圆角：四角一致（这是「左下右下角是圆角」那一条）。 */
    private static final float RADIUS_CARD = 28.0f;
    private static final float RADIUS_TILE = 14.0f;
    private static final float RADIUS_ROW = 16.0f;
    private static final float RADIUS_ICON = 11.0f;
    private static final float RADIUS_PLATE = 20.0f;
    private static final float RADIUS_PILL = 999.0f;
    private static final float TILE = 44.0f;
    private static final float ICON_BOX = 36.0f;
    private static final float ROW_MIN = 60.0f;

    private static final PathInterpolator EASE_STD =
            new PathInterpolator(0.20f, 0.00f, 0.00f, 1.00f);
    private static final PathInterpolator EASE_SOFT =
            new PathInterpolator(0.20f, 0.62f, 0.28f, 1.00f);

    private final Activity host;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private final List<View> rows = new ArrayList<>();
    private final List<View> iconPlates = new ArrayList<>();
    private final List<GlyphView> chevrons = new ArrayList<>();
    private final List<TextView> rowTitles = new ArrayList<>();
    private final List<TextView> rowHints = new ArrayList<>();
    /** 被临时关掉 clipChildren 的祖先（detach 时还原）。 */
    private final List<ViewGroup> unclipped = new ArrayList<>();

    private FrameLayout root;
    private FrameLayout safe;
    private BlurBackdropView backdrop;
    private LinearLayout panel;
    private ScrollView scroller;
    private LinearLayout box;
    private FrameLayout headTile;
    private GlyphView headCup;
    private TextView title;
    private TextView subtitle;
    private TextView section;
    private TextView closePill;
    private LinearLayout qrPanel;
    private FrameLayout qrPlate;
    private ImageView qrImage;
    private TextView qrCaption;
    private View zoomHost;

    private Bitmap qrAlipay;
    private Bitmap qrWechat;
    private boolean qrPrefetched;
    private int qrPx;

    /**
     * 赞赏码是否真的在包里。两张码图不随仓库分发（避免公开分发收款码），
     * 从源码自建且没有自己放入码图时资源不存在 —— 这时不展开码块，也不去解图，
     * 免得 {@code setImageResource} 抛 NotFoundException 把整个弹层带崩。
     */
    private Boolean qrResOk;

    /** 宿主 Activity 的 content 视图（重截模糊底要用；detach 时清掉）。 */
    private ViewGroup parentContent;

    private int qrSource;
    private boolean closing;
    private boolean currentDark;
    private int bottomInset;
    private int pad;

    // ---- 帧统计（N02）
    private boolean probing;
    private long probeLastNanos;
    private final List<Integer> probeGaps = new ArrayList<>();
    private final Choreographer.FrameCallback probeTick = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!RewardSheet.this.probing) {
                return;
            }
            long last = RewardSheet.this.probeLastNanos;
            if (last != 0L) {
                RewardSheet.this.probeGaps.add((int) ((frameTimeNanos - last) / 1000000L));
            }
            RewardSheet.this.probeLastNanos = frameTimeNanos;
            Choreographer.getInstance().postFrameCallback(this);
        }
    };

    private final Runnable themePoll = new Runnable() {
        @Override
        public void run() {
            try {
                if (RewardSheet.this.panel != null
                        && ThemeManager.INSTANCE.isDark(RewardSheet.this.host) != RewardSheet.this.currentDark) {
                    RewardSheet.this.refreshThemeNow();
                }
            } catch (Throwable ignored) {
            }
            ui.postDelayed(this, 900L);
        }
    };

    private RewardSheet(Activity activity) {
        this.host = activity;
    }

    // ---------------------------------------------------------------- 入口 / 返回键

    public static void show(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        RewardSheet open = current;
        if (open != null) {
            if (open.host == activity) {
                return;
            }
            open.detach();
        }
        new RewardSheet(activity).attach();
    }

    /**
     * 返回键：true = 这次返回被弹层吃掉（先收二维码、再关卡）。
     * 覆盖层不是窗口，拿不到 `onBackPressed`，只能由宿主 Activity 转一下。
     */
    public static boolean handleBack() {
        RewardSheet open = current;
        if (open == null) {
            return false;
        }
        if (open.root == null || open.root.getParent() == null) {
            open.detach();
            return false;
        }
        open.onBack();
        return true;
    }

    /** 弹层当前是否是打开状态（宿主换肤时决定要不要走整页叠化）。 */
    public static boolean isOpen() {
        RewardSheet open = current;
        return open != null && open.root != null && open.root.getParent() != null;
    }

    /**
     * v1.10.184：**宿主页换肤之后必须叫一下这里**。
     *
     * 弹层的配色本来靠 900ms 轮询，能自己跟上；但底下的**模糊底是一张静态截图**，
     * 不重截就会一直压着旧主题的页面（浅色下底下一片黑）。这里立刻重刷配色，
     * 并在 90ms 后（页面已经重新布局完）重截一次模糊底。
     */
    public static void onHostThemeChanged() {
        RewardSheet open = current;
        if (open == null || open.root == null) {
            return;
        }
        open.refreshThemeNow();
    }

    private void refreshThemeNow() {
        applyColors();
        // H02（第 2 层）**延后 260ms 再打**：宿主那一层的 H02 可能比我晚一点落地，
        // 立刻打会把「宿主=跟随」写错（v1.10.185 实测日志里就是这么出现的假信息）。
        this.ui.removeCallbacks(this.h2Log);
        this.ui.postDelayed(this.h2Log, 260L);
        this.ui.removeCallbacks(this.backdropRecapture);
        this.ui.postDelayed(this.backdropRecapture, 90L);
    }

    /** 第 2 层的跟随记录（等宿主层 H02 落地后再写，避免假「未跟随」）。 */
    private final Runnable h2Log = new Runnable() {
        @Override
        public void run() {
            if (RewardSheet.this.root == null) {
                return;
            }
            String hostPage = RewardSheet.this.host.getClass().getSimpleName();
            String extra = "宿主=" + (ThemeSwitch.followedRecently(hostPage, 1500L) ? "跟随" : "未跟随")
                    + " 背景刷新=已刷";
            try {
                ThemeSwitch.refreshed(RewardSheet.this.host, "RewardSheet", 2,
                        ThemeSwitch.layers(), extra);
            } catch (Throwable ignored) {
            }
        }
    };

    /** 换肤后重截模糊底（先把覆盖层藏起来：content.draw 会把自己也画进去）。 */
    private final Runnable backdropRecapture = new Runnable() {
        @Override
        public void run() {
            final FrameLayout layer = RewardSheet.this.root;
            final ViewGroup area = RewardSheet.this.parentContent;
            if (layer == null || area == null || RewardSheet.this.backdrop == null) {
                return;
            }
            layer.setVisibility(View.INVISIBLE);
            Bitmap shot = capture(area);
            layer.setVisibility(View.VISIBLE);
            if (shot == null) {
                Diag.warn(RewardSheet.this.host, "N01", "赞赏弹层换肤 模糊底重截失败（保留旧图）");
                return;
            }
            RewardSheet.this.backdrop.setShot(shot);
            Diag.warn(RewardSheet.this.host, "N01", "赞赏弹层换肤 模糊底重截 "
                    + shot.getWidth() + "x" + shot.getHeight());
        }
    };

    // ---------------------------------------------------------------- 挂载 / 卸载

    private void attach() {
        try {
            attachInner();
        } catch (Throwable th) {
            Diag.warn(this.host, "N01", "赞赏弹层构建异常: " + th.getClass().getSimpleName());
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
        long t0 = SystemClock.uptimeMillis();
        Bitmap shot = capture(content);
        // 注意：BlurBackdropView 糊完就把原图回收了（全屏 ARGB 十几 MB），所以宽高要先取
        final int shotW = shot != null ? shot.getWidth() : 0;
        final int shotH = shot != null ? shot.getHeight() : 0;
        final boolean hadShot = shot != null;
        build(shot);
        if (this.root == null) {
            return;
        }
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        // 非沉浸的系统：content 不含状态栏/导航栏 → 负 margin 往外顶满窗口 + 关掉祖先裁剪
        int[] contentLoc = new int[2];
        content.getLocationOnScreen(contentLoc);
        View decor = this.host.getWindow().getDecorView();
        int[] decorLoc = new int[2];
        decor.getLocationOnScreen(decorLoc);
        int topCover = Math.max(0, contentLoc[1] - decorLoc[1]);
        int bottomCover = Math.max(0,
                (decorLoc[1] + decor.getHeight()) - (contentLoc[1] + content.getHeight()));
        if (topCover > 0 || bottomCover > 0) {
            lp.topMargin = -topCover;
            lp.bottomMargin = -bottomCover;
            disableClip(content, decor);
        }
        content.addView(this.root, lp);
        this.parentContent = content;
        current = this;
        playEnter();
        Diag.warn(this.host, "N01", "赞赏弹层 模糊底=" + (hadShot ? "已截" : "退化")
                + " 图=" + (hadShot ? shotW + "x" + shotH : "-")
                + " 补边=" + topCover + "/" + bottomCover
                + " 屏=" + this.host.getResources().getDisplayMetrics().widthPixels
                + "x" + this.host.getResources().getDisplayMetrics().heightPixels
                + " 构建=" + (SystemClock.uptimeMillis() - t0) + "ms"
                + " 深色=" + this.currentDark);
    }

    /** 把 content 到 decor 这条链上的 clipChildren 临时关掉（只有补边时才需要）。 */
    private void disableClip(View content, View decor) {
        View v = content;
        int guard = 0;
        while (v != null && guard++ < 8) {
            if (v instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) v;
                if (g.getClipChildren()) {
                    g.setClipChildren(false);
                    this.unclipped.add(g);
                }
            }
            if (v == decor) {
                break;
            }
            ViewParent p = v.getParent();
            v = p instanceof View ? (View) p : null;
        }
    }

    private void restoreClip() {
        for (int i = 0; i < this.unclipped.size(); i++) {
            this.unclipped.get(i).setClipChildren(true);
        }
        this.unclipped.clear();
    }

    /**
     * 把当前页面画进一张位图（给 BlurBackdropView 做模糊底）。**先截、再挂载**，不会截到自己。
     *
     * v1.10.183：截图按 **1/2** 画（画布 scale），全屏 ARGB 从 14MB → 3.5MB，
     * 软件绘制耗时也降到约 1/4。糊完只看得到低频结构，1/2 与 1:1 的模糊结果在屏幕尺度上
     * 基本等价（BlurBackdropView 内部还要再缩 1/6）。
     */
    private Bitmap capture(View content) {
        try {
            final float scale = 0.5f;
            int w = Math.max(8, Math.round(content.getWidth() * scale));
            int h = Math.max(8, Math.round(content.getHeight() * scale));
            Bitmap shot = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(shot);
            canvas.scale(w / (float) content.getWidth(), h / (float) content.getHeight());
            content.draw(canvas);
            return shot;
        } catch (Throwable th) {
            return null;
        }
    }

    private void playEnter() {
        if (this.backdrop != null) {
            this.backdrop.setAlpha(0.0f);
            this.backdrop.animate().alpha(1.0f).setDuration(200L)
                    .setInterpolator(EASE_SOFT).start();
        }
        final LinearLayout card = this.panel;
        if (card == null) {
            return;
        }
        // hardware layer：卡片子树只渲一次，之后 alpha/scale 是图层的变换 —— 不会每帧 saveLayer
        card.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        card.setAlpha(0.0f);
        card.setScaleX(0.86f);
        card.setScaleY(0.86f);
        // v1.10.265：**换成真弹簧**（欠阻尼阶跃响应，过冲 ≈12% 再回弹落定）。
        // 旧的 `.scaleX(1).setInterpolator(EASE_STD)` 是单调减速曲线 —— 只有"快→慢"，
        // 没有质量感，就是用户说的"普通、大众、随便"。
        // 起始缩放 0.86 比原来的 0.96 更小，过冲才看得见（0.86 → 1.03 → 1.00）。
        float dy = px(26.0f);
        SpringAnim.popIn(card, 0.86f, dy, 140L, SpringAnim.POP);
        card.postDelayed(new Runnable() { // from class: io.github.animwidget.RewardSheet$restoreLayer
            @Override // java.lang.Runnable
            public void run() {
                if (RewardSheet.this.panel != null) {
                    RewardSheet.this.panel.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }
        }, SpringAnim.POP.durationMs() + 30L);
    }

    private void close() {
        if (this.closing) {
            return;
        }
        if (this.zoomHost != null) {
            closeZoom();
            return;
        }
        this.closing = true;
        this.probing = false;
        final LinearLayout card = this.panel;
        if (card != null) {
            card.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            // v1.10.265：收起也走弹簧，但用 ζ≈0.82 的"硬"配方（几乎不回弹、更利落），
            // 并且是"往下掉 + 缩小"—— 与弹出的"从下往上弹"形成方向上的呼应。
            SpringAnim.popOut(card, 0.90f, px(18.0f), this::detach, SpringAnim.CLOSE);
        } else {
            detach();
        }
        if (this.backdrop != null) {
            this.backdrop.animate().alpha(0.0f).setDuration(160L)
                    .setInterpolator(EASE_SOFT).start();
        }
    }

    private void onBack() {
        if (this.zoomHost != null) {
            closeZoom();
            return;
        }
        LinearLayout target = this.qrPanel;
        if (target != null && target.getVisibility() == View.VISIBLE) {
            this.qrSource = 0;
            collapseQr();
            paintRowBackgrounds(ThemeManager.INSTANCE.colors(this.host));
            return;
        }
        close();
    }

    private void detach() {
        this.closing = true;
        this.probing = false;
        this.ui.removeCallbacksAndMessages(null);
        View zoom = this.zoomHost;
        this.zoomHost = null;
        if (zoom != null && this.root != null) {
            this.root.removeView(zoom);
        }
        releaseQrBitmaps();
        BlurBackdropView back = this.backdrop;
        this.backdrop = null;
        if (back != null) {
            back.release();
        }
        this.parentContent = null;
        FrameLayout layer = this.root;
        this.root = null;
        if (layer != null) {
            ViewParent p = layer.getParent();
            if (p instanceof ViewGroup) {
                ((ViewGroup) p).removeView(layer);
            }
        }
        restoreClip();
        if (current == this) {
            current = null;
        }
    }

    // ---------------------------------------------------------------- 构建

    private void build(Bitmap shot) {
        final ThemeColors c = ThemeManager.INSTANCE.colors(this.host);
        final float dp = this.host.getResources().getDisplayMetrics().density;
        final int screenW = this.host.getResources().getDisplayMetrics().widthPixels;
        this.currentDark = ThemeManager.INSTANCE.isDark(this.host);
        this.pad = px(PAD);

        FrameLayout root = new FrameLayout(this.host);
        root.setClickable(true);
        root.setOnClickListener(v -> close());
        this.root = root;

        this.backdrop = new BlurBackdropView(this.host, shot, scrimColor());
        this.backdrop.setOnClickListener(v -> close());
        root.addView(this.backdrop, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        FrameLayout safe = new FrameLayout(this.host);
        root.addView(safe, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.safe = safe;
        safe.setOnApplyWindowInsetsListener((v, insets) -> {
            int l;
            int t;
            int r;
            int b;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                l = bars.left;
                t = bars.top;
                r = bars.right;
                b = bars.bottom;
            } else {
                l = insets.getSystemWindowInsetLeft();
                t = insets.getSystemWindowInsetTop();
                r = insets.getSystemWindowInsetRight();
                b = insets.getSystemWindowInsetBottom();
            }
            if (this.bottomInset != b || v.getPaddingTop() != t
                    || v.getPaddingLeft() != l || v.getPaddingRight() != r) {
                this.bottomInset = b;
                v.setPadding(l, t, r, b);
                final LinearLayout boxRef = this.box;
                if (boxRef != null) {
                    boxRef.post(this::clampIfClosed);
                }
            }
            return insets;
        });

        LinearLayout panel = new LinearLayout(this.host);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setClickable(true);
        panel.setBackground(cardBackground(c, dp));
        final int cardW = Math.max(px(240.0f),
                Math.min(screenW - this.pad * 2, px(560.0f)));
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(
                cardW, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.gravity = Gravity.CENTER;
        safe.addView(panel, plp);
        this.panel = panel;

        ScrollView scroller = new ScrollView(this.host);
        scroller.setClipToPadding(false);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        panel.addView(scroller, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        this.scroller = scroller;

        LinearLayout box = new LinearLayout(this.host);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(this.pad, px(24.0f), this.pad, px(24.0f));
        scroller.addView(box, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        this.box = box;

        buildHeader(c, dp);
        buildSection(c);
        buildRows(c);
        buildQr(c, cardW);
        buildClose(c);

        Fonts.applyTree(box);

        // 上限：第一次拿到正高度时夹一次（post 会在 attach 阶段跑，那时高度还是 0）
        box.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom - top <= 0) {
                    return;
                }
                v.removeOnLayoutChangeListener(this);
                clampSheetHeight();
            }
        });

        startQrPreload();
        this.ui.removeCallbacks(this.themePoll);
        this.ui.postDelayed(this.themePoll, 900L);
    }

    private void buildHeader(ThemeColors c, float dp) {
        LinearLayout header = new LinearLayout(this.host);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout tile = new FrameLayout(this.host);
        tile.setBackground(ThemeManager.INSTANCE.rounded(this.host,
                ThemeManager.INSTANCE.withAlpha(c.getPrimary(), 0.14f), RADIUS_TILE));
        GlyphView cup = new GlyphView(this.host);
        cup.setGlyph(GlyphView.CUP);
        cup.setTintColor(c.getPrimary());
        cup.setContentDescription(this.host.getString(R.string.reward_title));
        tile.addView(cup, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        header.addView(tile, new LinearLayout.LayoutParams(px(TILE), px(TILE)));
        this.headTile = tile;
        this.headCup = cup;

        this.title = new TextView(this.host);
        this.title.setText(R.string.reward_title);
        this.title.setTextSize(19.0f);
        // 真字重加粗：保留家族（系统字体/自定义字体），只把 weight 提升到 BOLD
        this.title.setTypeface(this.title.getTypeface(), Typeface.BOLD);
        this.title.setTextColor(c.getText());
        this.title.setMaxLines(2);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tlp.setMarginStart(px(14.0f));
        header.addView(this.title, tlp);

        this.box.addView(header);

        // 说明文字**整行铺满**：原来缩在标题那一列里，两行被挤成很窄的一条，
        // 视觉上像「附件」。整行铺开后行宽翻倍，段落节奏才对。
        this.subtitle = new TextView(this.host);
        this.subtitle.setText(R.string.reward_subtitle);
        this.subtitle.setTextSize(12.5f);
        this.subtitle.setTextColor(c.getTextMuted());
        this.subtitle.setLineSpacing(4.0f * dp, 1.0f);
        LinearLayout.LayoutParams stlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stlp.topMargin = px(10.0f);
        this.box.addView(this.subtitle, stlp);
    }

    private void buildSection(ThemeColors c) {
        this.section = new TextView(this.host);
        this.section.setText(R.string.reward_section_support);
        this.section.setTextSize(11.5f);
        this.section.setTextColor(c.getTextMuted());
        this.section.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px(20.0f);
        lp.bottomMargin = px(10.0f);
        this.box.addView(this.section, lp);
    }

    private void buildRows(ThemeColors c) {
        LinearLayout list = new LinearLayout(this.host);
        list.setOrientation(LinearLayout.VERTICAL);
        this.box.addView(list);

        list.addView(buildRow(c, R.drawable.ic_brand_alipay, "支",
                R.string.reward_method_alipay, R.string.reward_method_alipay_hint, () -> showQr(1)));
        list.addView(buildRow(c, R.drawable.ic_brand_kofi, "K",
                R.string.reward_method_kofi, R.string.reward_method_kofi_hint, this::openKofi), rowLp());
        list.addView(buildRow(c, R.drawable.ic_brand_wechat, "赏",
                R.string.reward_method_qr, R.string.reward_method_qr_hint, () -> showQr(2)), rowLp());
    }

    private LinearLayout.LayoutParams rowLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px(8.0f);
        return lp;
    }

    /**
     * 一行支持方式：品牌图标底板（随主题）+ 标题/说明 + 矢量箭头。
     *
     * 底板颜色由 {@link #iconPlateColor()} 给，品牌 logo 用原始品牌色，**不改色**。
     */
    private View buildRow(ThemeColors c, int iconRes, String fallbackGlyph,
                          int titleRes, int hintRes, Runnable onClick) {
        String titleText = this.host.getString(titleRes);
        String hintText = this.host.getString(hintRes);

        LinearLayout row = new LinearLayout(this.host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(px(ROW_MIN));
        int padH = px(12.0f);
        row.setPadding(padH, px(12.0f), padH, px(12.0f));
        row.setBackground(ripple(rowBackground(c), 0.08f));
        row.setClickable(true);
        row.setFocusable(true);
        row.setContentDescription(titleText + "，" + hintText);

        FrameLayout plate = new FrameLayout(this.host);
        plate.setBackground(plateBackground());
        row.addView(plate, new LinearLayout.LayoutParams(px(ICON_BOX), px(ICON_BOX)));

        Drawable brand = null;
        try {
            brand = this.host.getResources().getDrawable(iconRes, this.host.getTheme());
        } catch (Throwable ignored) {
            brand = null;
        }
        if (brand != null) {
            ImageView icon = new ImageView(this.host);
            icon.setImageDrawable(brand);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int inner = px(ICON_BOX * 0.58f);
            FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(inner, inner);
            ilp.gravity = Gravity.CENTER;
            plate.addView(icon, ilp);
            icon.setContentDescription(titleText);
        } else {
            TextView glyph = new TextView(this.host);
            glyph.setText(fallbackGlyph);
            glyph.setTextSize(15.0f);
            glyph.setGravity(Gravity.CENTER);
            glyph.setTextColor(0xF0111111);
            plate.addView(glyph, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        LinearLayout column = new LinearLayout(this.host);
        column.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        clp.setMarginStart(px(12.0f));
        clp.setMarginEnd(px(8.0f));
        row.addView(column, clp);

        TextView titleView = new TextView(this.host);
        titleView.setText(titleText);
        titleView.setTextSize(15.0f);
        titleView.setTextColor(c.getText());
        titleView.setMaxLines(1);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        column.addView(titleView);

        TextView hintView = new TextView(this.host);
        hintView.setText(hintText);
        hintView.setTextSize(12.0f);
        hintView.setTextColor(c.getTextMuted());
        hintView.setMaxLines(2);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = px(2.0f);
        column.addView(hintView, hlp);

        GlyphView arrow = new GlyphView(this.host);
        arrow.setGlyph(GlyphView.CHEVRON);
        arrow.setTintColor(ThemeManager.INSTANCE.withAlpha(c.getText(), 0.40f));
        boolean rtl = this.host.getResources().getConfiguration().getLayoutDirection()
                == View.LAYOUT_DIRECTION_RTL;
        arrow.setRotation(rtl ? 90.0f : -90.0f);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(px(18.0f), px(18.0f));
        alp.gravity = Gravity.CENTER_VERTICAL;
        row.addView(arrow, alp);

        row.setOnClickListener(v -> {
            v.animate().cancel();
            v.animate().scaleX(0.985f).scaleY(0.985f).setDuration(70L)
                    .withEndAction(() -> v.animate().scaleX(1.0f).scaleY(1.0f)
                            .setDuration(110L).start())
                    .start();
            if (onClick != null) {
                onClick.run();
            }
        });

        this.rows.add(row);
        this.iconPlates.add(plate);
        this.chevrons.add(arrow);
        this.rowTitles.add(titleView);
        this.rowHints.add(hintView);
        return row;
    }

    private void buildQr(ThemeColors c, int cardW) {
        this.qrPanel = new LinearLayout(this.host);
        this.qrPanel.setOrientation(LinearLayout.VERTICAL);
        // 12dp 的间隔做成**内部 paddingTop**：折叠时高度 0 会把 padding 一起裁掉，
        // 展开时它跟着高度长出来 —— 这样动画中途不会多出一段 12dp 的空跳。
        this.qrPanel.setPadding(0, px(12.0f), 0, 0);
        this.qrPanel.setVisibility(View.GONE);
        this.box.addView(this.qrPanel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        this.qrPlate = new FrameLayout(this.host);
        int platePad = px(16.0f);
        this.qrPlate.setPadding(platePad, platePad, platePad, platePad);
        this.qrPlate.setBackground(plateBackground());
        LinearLayout.LayoutParams pllp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pllp.gravity = Gravity.CENTER_HORIZONTAL;
        this.qrPanel.addView(this.qrPlate, pllp);

        // 可用内宽用**卡片实际宽度**算（不是屏宽）：卡片居中且比屏窄，用屏宽会算出贴边的码
        int inner = cardW - this.pad * 2 - platePad * 2;
        this.qrPx = Math.max(px(176.0f), Math.min(px(232.0f), inner));

        this.qrImage = new ImageView(this.host);
        this.qrImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        this.qrImage.setContentDescription(this.host.getString(R.string.reward_qr_caption));
        this.qrPlate.addView(this.qrImage,
                new FrameLayout.LayoutParams(this.qrPx, this.qrPx));
        this.qrImage.setOnClickListener(v -> zoom(this.qrSource));

        this.qrCaption = new TextView(this.host);
        this.qrCaption.setText(R.string.reward_qr_caption);
        this.qrCaption.setTextSize(11.5f);
        this.qrCaption.setGravity(Gravity.CENTER);
        this.qrCaption.setTextColor(c.getTextMuted());
        LinearLayout.LayoutParams caplp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        caplp.topMargin = px(10.0f);
        this.qrPanel.addView(this.qrCaption, caplp);
    }

    /** 关闭：独立胶囊（不是整条文字）。 */
    private void buildClose(ThemeColors c) {
        this.closePill = new TextView(this.host);
        this.closePill.setText(R.string.dev_close);
        this.closePill.setTextSize(14.0f);
        this.closePill.setGravity(Gravity.CENTER);
        this.closePill.setLetterSpacing(0.04f);
        this.closePill.setTextColor(c.getText());
        this.closePill.setPadding(px(26.0f), 0, px(26.0f), 0);
        this.closePill.setBackground(ripple(closePillBackground(c), 0.10f));
        this.closePill.setClickable(true);
        this.closePill.setFocusable(true);
        this.closePill.setContentDescription(this.host.getString(R.string.dev_close));
        this.closePill.setOnClickListener(v -> close());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, px(44.0f));
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.topMargin = px(16.0f);
        this.box.addView(this.closePill, lp);
    }

    // ---------------------------------------------------------------- 主题

    private int scrimColor() {
        // 模糊底之上再压一层：深色 32% / 浅色 18%，保证面板与文字的对比度
        return this.currentDark ? 0x52000000 : 0x2E000000;
    }

    /** 卡片底：**四角**统一 28dp 圆角（居中卡片，四个角都要圆），发丝描边。 */
    private Drawable cardBackground(ThemeColors c, float dp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(c.getSurface());
        bg.setCornerRadius(RADIUS_CARD * dp);
        bg.setStroke(Math.max(1, Math.round(1.2f * dp)),
                ThemeManager.INSTANCE.withAlpha(c.getText(), 0.07f));
        return bg;
    }

    /** 支持方式行的底：surface 上叠 5% 文字色（浅色下变深、深色下变亮，天然跟主题）。 */
    private Drawable rowBackground(ThemeColors c) {
        return ThemeManager.INSTANCE.outlined(this.host,
                ThemeManager.INSTANCE.withAlpha(c.getText(), 0.055f),
                ThemeManager.INSTANCE.withAlpha(c.getText(), 0.055f), RADIUS_ROW);
    }

    /**
     * 行的选中态：二维码展开时，那一行换成「主色 10% 底 + 主色 45% 描边」，
     * 让人一眼知道下面这张码是哪一种方式（不新增任何控件，只换背景）。
     */
    private void paintRowBackgrounds(ThemeColors c) {
        for (int i = 0; i < this.rows.size(); i++) {
            boolean selected = isRowSelected(i);
            Drawable bg = selected
                    ? ThemeManager.INSTANCE.outlined(this.host,
                            ThemeManager.INSTANCE.withAlpha(c.getPrimary(), 0.10f),
                            ThemeManager.INSTANCE.withAlpha(c.getPrimary(), 0.45f), RADIUS_ROW)
                    : rowBackground(c);
            this.rows.get(i).setBackground(ripple(bg, 0.08f));
        }
    }

    /** 行序：0=支付宝（码 1）、1=Ko-fi、2=赞赏码（码 2）。 */
    private boolean isRowSelected(int index) {
        if (this.qrPanel == null || this.qrPanel.getVisibility() != View.VISIBLE) {
            return false;
        }
        if (index == 0) {
            return this.qrSource == 1;
        }
        if (index == 2) {
            return this.qrSource == 2;
        }
        return false;
    }

    /**
     * 品牌图标底板：**随主题**，品牌色不动。
     * 浅色 = 纯白；深色 = 91% 白（柔白，约 #E9E9E9），描边 6% / 12% 黑。
     */
    private int iconPlateColor() {
        return this.currentDark ? 0xE8FFFFFF : 0xFFFFFFFF;
    }

    private Drawable plateBackground() {
        int fill = iconPlateColor();
        int stroke = this.currentDark ? 0x1F000000 : 0x11000000;
        return ThemeManager.INSTANCE.outlined(this.host, fill, stroke, RADIUS_ICON);
    }

    private Drawable qrPlateBackground() {
        return ThemeManager.INSTANCE.outlined(this.host, 0xFFFFFFFF, 0x14000000, RADIUS_PLATE);
    }

    private Drawable closePillBackground(ThemeColors c) {
        return ThemeManager.INSTANCE.outlined(this.host,
                ThemeManager.INSTANCE.withAlpha(c.getText(), 0.06f),
                ThemeManager.INSTANCE.withAlpha(c.getText(), 0.10f), RADIUS_PILL);
    }

    private Drawable ripple(Drawable content, float alpha) {
        try {
            return new RippleDrawable(ColorStateList.valueOf(
                    ThemeManager.INSTANCE.withAlpha(
                            ThemeManager.INSTANCE.colors(this.host).getText(), alpha)),
                    content, null);
        } catch (Throwable th) {
            return content;
        }
    }

    /** 主题变化（系统深/浅切换）后整体重刷。 */
    private void applyColors() {
        ThemeColors c = ThemeManager.INSTANCE.colors(this.host);
        this.currentDark = ThemeManager.INSTANCE.isDark(this.host);
        if (this.backdrop != null) {
            this.backdrop.setScrimColor(scrimColor());
        }
        if (this.panel != null) {
            this.panel.setBackground(cardBackground(c,
                    this.host.getResources().getDisplayMetrics().density));
        }
        if (this.headTile != null) {
            this.headTile.setBackground(ThemeManager.INSTANCE.rounded(this.host,
                    ThemeManager.INSTANCE.withAlpha(c.getPrimary(), 0.14f), RADIUS_TILE));
        }
        if (this.headCup != null) {
            this.headCup.setTintColor(c.getPrimary());
        }
        if (this.title != null) {
            this.title.setTextColor(c.getText());
        }
        if (this.subtitle != null) {
            this.subtitle.setTextColor(c.getTextMuted());
        }
        if (this.section != null) {
            this.section.setTextColor(c.getTextMuted());
        }
        if (this.qrCaption != null) {
            this.qrCaption.setTextColor(c.getTextMuted());
        }
        if (this.closePill != null) {
            this.closePill.setTextColor(c.getText());
            this.closePill.setBackground(ripple(closePillBackground(c), 0.10f));
        }
        paintRowBackgrounds(c);
        for (int i = 0; i < this.iconPlates.size(); i++) {
            this.iconPlates.get(i).setBackground(plateBackground());
        }
        for (int i = 0; i < this.chevrons.size(); i++) {
            this.chevrons.get(i).setTintColor(
                    ThemeManager.INSTANCE.withAlpha(c.getText(), 0.40f));
        }
        for (int i = 0; i < this.rowTitles.size(); i++) {
            this.rowTitles.get(i).setTextColor(c.getText());
        }
        for (int i = 0; i < this.rowHints.size(); i++) {
            this.rowHints.get(i).setTextColor(c.getTextMuted());
        }
        if (this.qrPlate != null) {
            this.qrPlate.setBackground(qrPlateBackground());
        }
    }

    // ---------------------------------------------------------------- 尺寸 / 动画

    private int px(float dpValue) {
        return Math.round(dpValue * this.host.getResources().getDisplayMetrics().density);
    }

    /** 卡片高度上限：安全区高度的 86%（居中卡片，四边都要留出来）。 */
    private int maxViewport() {
        View area = this.safe;
        int h = (area != null && area.getHeight() > 0)
                ? area.getHeight() - area.getPaddingTop() - area.getPaddingBottom()
                : this.host.getResources().getDisplayMetrics().heightPixels;
        return Math.max(px(200.0f), Math.round(h * 0.86f));
    }

    /**
     * 卡片高度 = min(内容高, 上限)；超出只让内部 ScrollView 滚。
     * 位置由 `Gravity.CENTER` 决定，所以高度一变就是「上下同时长」。
     */
    private void clampSheetHeight() {
        ScrollView sc = this.scroller;
        LinearLayout content = this.box;
        if (sc == null || content == null) {
            return;
        }
        int wanted = content.getHeight();
        if (wanted <= 0) {
            return;
        }
        setHeight(sc, Math.min(wanted, maxViewport()));
    }

    /** 二维码展开/收起动画期间不许夹（会和动画抢同一个 LayoutParams.height）。 */
    private void clampIfClosed() {
        LinearLayout qr = this.qrPanel;
        if (qr != null && qr.getVisibility() == View.VISIBLE) {
            return;
        }
        clampSheetHeight();
    }

    private void setHeight(View view, int height) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp != null && lp.height != height) {
            lp.height = height;
            view.setLayoutParams(lp);
        }
    }

    // ---------------------------------------------------------------- 二维码预解码

    private void startQrPreload() {
        if (!qrAvailable()) {
            Diag.warn(this.host, "N03", "赞赏码不在包里（自建包未放入码图）→ 不预解码");
            return;
        }
        final long t0 = SystemClock.uptimeMillis();
        Thread worker = new Thread(() -> {
            Bitmap a = null;
            Bitmap b = null;
            String err = "";
            try {
                a = BitmapFactory.decodeResource(this.host.getResources(), R.drawable.reward_alipay);
                b = BitmapFactory.decodeResource(this.host.getResources(), R.drawable.reward_qr);
            } catch (Throwable th) {
                err = " 异常=" + th.getClass().getSimpleName();
            }
            final Bitmap fa = a;
            final Bitmap fb = b;
            final String ferr = err;
            final long cost = SystemClock.uptimeMillis() - t0;
            this.ui.post(() -> {
                if (this.closing) {
                    recycle(fa);
                    recycle(fb);
                    return;
                }
                this.qrAlipay = fa;
                this.qrWechat = fb;
                this.qrPrefetched = fa != null && fb != null;
                Diag.warn(this.host, "N03", "赞赏码预解码 支付宝=" + dim(fa)
                        + " 微信=" + dim(fb) + " 耗时=" + cost + "ms" + ferr);
            });
        }, "reward-qr-preload");
        worker.setDaemon(true);
        worker.start();
    }

    /** 两张码图在不在资源表里（自建包可能没有）。只查一次，结果缓存。 */
    private boolean qrAvailable() {
        Boolean cached = this.qrResOk;
        if (cached != null) {
            return cached;
        }
        boolean ok = true;
        try {
            this.host.getResources().getDrawable(R.drawable.reward_qr, this.host.getTheme());
            this.host.getResources().getDrawable(R.drawable.reward_alipay, this.host.getTheme());
        } catch (Throwable th) {
            ok = false;
        }
        this.qrResOk = ok;
        return ok;
    }

    private static String dim(Bitmap b) {
        return b == null ? "失败" : b.getWidth() + "x" + b.getHeight();
    }

    private static void recycle(Bitmap b) {
        try {
            if (b != null && !b.isRecycled()) {
                b.recycle();
            }
        } catch (Throwable ignored) {
        }
    }

    private void releaseQrBitmaps() {
        if (this.qrImage != null) {
            this.qrImage.setImageDrawable(null);
        }
        recycle(this.qrAlipay);
        recycle(this.qrWechat);
        this.qrAlipay = null;
        this.qrWechat = null;
    }

    /** 把当前来源的码图贴上去（预解码好了就零解码）。 */
    private void bindQr(int source) {
        if (this.qrImage == null) {
            return;
        }
        Bitmap bmp = source == 1 ? this.qrAlipay : this.qrWechat;
        if (bmp != null && !bmp.isRecycled()) {
            this.qrImage.setImageBitmap(bmp);
            return;
        }
        try {
            this.qrImage.setImageResource(source == 1
                    ? R.drawable.reward_alipay : R.drawable.reward_qr);
            Diag.warn(this.host, "N03", "赞赏码未预解码就用到 → 主线程兜底解码 来源=" + source);
        } catch (Throwable th) {
            // 自建包没放码图时资源不存在：置空即可，不崩。
            this.qrImage.setImageDrawable(null);
            this.qrResOk = Boolean.FALSE;
        }
    }

    private void showQr(int source) {
        LinearLayout target = this.qrPanel;
        if (target == null) {
            return;
        }
        if (!qrAvailable()) {
            Diag.warn(this.host, "N03", "赞赏码不在包里（自建包未放入码图）→ 不展开");
            return;
        }
        boolean open = target.getVisibility() == View.VISIBLE;
        if (open && this.qrSource == source) {
            this.qrSource = 0;
            collapseQr();
            paintRowBackgrounds(ThemeManager.INSTANCE.colors(this.host));
            return;
        }
        this.qrSource = source;
        bindQr(source);
        paintRowBackgrounds(ThemeManager.INSTANCE.colors(this.host));
        if (open) {
            return;   // 已展开：只换图，不动画
        }
        expandQr();
    }

    /** 量一次展开后的高度（只测不布局）。 */
    private int measureQrBlock() {
        LinearLayout target = this.qrPanel;
        if (target == null || this.box == null) {
            return 0;
        }
        int w = this.box.getWidth() - this.box.getPaddingLeft() - this.box.getPaddingRight();
        if (w <= 0) {
            w = this.qrPx + px(32.0f);
        }
        target.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return target.getMeasuredHeight();
    }

    /**
     * 展开：**只动 qrPanel 的高度**，面板整体跟着长。
     *
     * 为什么不提前把视口定死：那会让面板瞬间变高（顶边一下跳上去），比现在更难看。
     * 视口够（绝大多数机型）时这条路径结束时 `clampSheetHeight()` 算出的高度**恰好等于**当前
     * 高度，不会产生第二跳；只有内容真的超高时才走「视口长高 + 滚到底」。
     */
    private void expandQr() {
        final LinearLayout target = this.qrPanel;
        final ScrollView sc = this.scroller;
        final LinearLayout content = this.box;
        if (target == null || sc == null || content == null) {
            return;
        }
        final int blockH = measureQrBlock();
        target.setVisibility(View.VISIBLE);
        if (blockH <= 0) {
            setHeight(target, ViewGroup.LayoutParams.WRAP_CONTENT);
            clampSheetHeight();
            return;
        }
        final int contentH = content.getHeight();
        final int maxVp = maxViewport();
        final int targetBox = contentH + blockH;

        if (this.qrCaption != null) {
            // 只有这一行小字淡入（单个 TextView，不会触发 saveLayer）
            this.qrCaption.setAlpha(0.0f);
            this.qrCaption.animate().alpha(1.0f).setStartDelay(70L).setDuration(200L).start();
        }

        if (targetBox <= maxVp) {
            setHeight(sc, ViewGroup.LayoutParams.WRAP_CONTENT);
            setHeight(target, 0);
            target.post(() -> {
                if (this.closing) {
                    return;
                }
                animateHeight(target, 0, blockH, 240, () -> {
                    setHeight(target, ViewGroup.LayoutParams.WRAP_CONTENT);
                    final ScrollView s = this.scroller;
                    if (s != null) {
                        s.post(this::clampSheetHeight);
                    }
                    finishProbe();
                });
            });
        } else {
            setHeight(target, blockH);
            animateHeight(sc, contentH, maxVp, 240, () -> {
                final ScrollView s = this.scroller;
                if (s != null) {
                    s.smoothScrollTo(0, Math.max(0, targetBox - maxVp));
                }
                finishProbe();
            });
            Diag.warn(this.host, "N02", "赞赏码展开超上限 内容=" + targetBox
                    + "px 视口=" + maxVp + "px 走滚动揭示");
        }
    }

    private void collapseQr() {
        final LinearLayout target = this.qrPanel;
        final ScrollView sc = this.scroller;
        if (target == null) {
            return;
        }
        int from = target.getHeight();
        if (from <= 0) {
            target.setVisibility(View.GONE);
            this.qrSource = 0;
            paintRowBackgrounds(ThemeManager.INSTANCE.colors(this.host));
            clampSheetHeight();
            return;
        }
        if (sc != null) {
            setHeight(sc, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (sc.getScrollY() > 0) {
                sc.scrollTo(0, 0);
            }
        }
        animateHeight(target, from, 0, 200, () -> {
            target.setVisibility(View.GONE);
            setHeight(target, ViewGroup.LayoutParams.WRAP_CONTENT);
            final ScrollView s = this.scroller;
            if (s != null) {
                s.post(this::clampSheetHeight);
            }
            finishProbe();
        });
    }

    private void animateHeight(final View target, int from, int to, int durationMs,
                               final Runnable end) {
        final ViewGroup.LayoutParams lp = target.getLayoutParams();
        android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofInt(from, to);
        anim.setDuration(durationMs);
        anim.setInterpolator(EASE_STD);
        anim.addUpdateListener(a -> {
            lp.height = (Integer) a.getAnimatedValue();
            target.setLayoutParams(lp);
        });
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (end != null) {
                    end.run();
                }
            }
        });
        startProbe();
        anim.start();
    }

    // ---------------------------------------------------------------- 帧统计（N02）

    private void startProbe() {
        this.probeGaps.clear();
        this.probeLastNanos = 0L;
        this.probing = true;
        Choreographer.getInstance().postFrameCallback(this.probeTick);
    }

    /** 动画结束后打点：真实渲染帧间隔的 p50/p95/最大/迟到数。 */
    private void finishProbe() {
        this.probing = false;
        try {
            Choreographer.getInstance().removeFrameCallback(this.probeTick);
        } catch (Throwable ignored) {
        }
        List<Integer> gaps = new ArrayList<>(this.probeGaps);
        if (gaps.isEmpty()) {
            return;
        }
        java.util.Collections.sort(gaps);
        int n = gaps.size();
        int p50 = gaps.get(n / 2);
        int p95 = gaps.get(Math.min(n - 1, (int) Math.ceil(n * 0.95) - 1));
        int max = gaps.get(n - 1);
        int late = 0;
        for (int i = 0; i < n; i++) {
            if (gaps.get(i) > 17) {
                late++;
            }
        }
        Diag.warn(this.host, "N02", "赞赏码滑出 帧=" + n + " p50=" + p50 + "ms p95=" + p95
                + "ms 最大=" + max + "ms 迟到>16.7=" + late
                + " 预解码=" + (this.qrPrefetched ? "是" : "否"));
    }

    /** 点二维码放大：只动「遮罩颜色」和「白板缩放」，不碰大位图的 alpha。 */
    private void zoom(int source) {
        final FrameLayout root = this.root;
        if (root == null || this.zoomHost != null) {
            return;
        }
        Bitmap bmp = source == 1 ? this.qrAlipay : this.qrWechat;
        Drawable fallback = this.qrImage == null ? null : this.qrImage.getDrawable();
        if ((bmp == null || bmp.isRecycled()) && fallback == null) {
            return;
        }
        FrameLayout host = new FrameLayout(this.host);
        host.setClickable(true);
        host.setOnClickListener(v -> closeZoom());
        this.zoomHost = host;

        final View scrim = new View(this.host);
        scrim.setBackgroundColor(0x00000000);
        host.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        FrameLayout plate = new FrameLayout(this.host);
        int inner = px(18.0f);
        plate.setPadding(inner, inner, inner, inner);
        plate.setBackground(ThemeManager.INSTANCE.rounded(this.host, 0xFFFFFFFF, 24.0f));

        ImageView image = new ImageView(this.host);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (bmp != null && !bmp.isRecycled()) {
            image.setImageBitmap(bmp);
        } else {
            image.setImageDrawable(fallback);
        }
        image.setContentDescription(this.host.getString(R.string.reward_method_qr));

        int screenW = this.host.getResources().getDisplayMetrics().widthPixels;
        int screenH = this.host.getResources().getDisplayMetrics().heightPixels;
        int side = Math.round(Math.min(screenW, screenH) * 0.86f);
        side = Math.min(side, px(420.0f));
        plate.addView(image, new FrameLayout.LayoutParams(side, side));

        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(
                side + inner * 2, side + inner * 2);
        plp.gravity = Gravity.CENTER;
        host.addView(plate, plp);
        root.addView(host, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final int scrimFull = 0xCC000000;
        plate.setScaleX(0.94f);
        plate.setScaleY(0.94f);
        android.animation.ValueAnimator anim =
                android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
        anim.setDuration(220L);
        anim.setInterpolator(EASE_STD);
        anim.addUpdateListener(a -> {
            float f = a.getAnimatedFraction();
            int alpha = Math.round(0xCC * f);
            scrim.setBackgroundColor((alpha << 24) | (scrimFull & 0x00FFFFFF));
            float s = 0.94f + 0.06f * f;
            plate.setScaleX(s);
            plate.setScaleY(s);
        });
        anim.start();
    }

    private void closeZoom() {
        final View hostView = this.zoomHost;
        final FrameLayout root = this.root;
        if (hostView == null) {
            return;
        }
        this.zoomHost = null;
        hostView.animate().alpha(0.0f).setDuration(160L).setInterpolator(EASE_SOFT)
                .withEndAction(() -> {
                    if (root != null) {
                        root.removeView(hostView);
                    }
                }).start();
    }

    private void openKofi() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(KOFI_URL));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.host.startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException | IllegalArgumentException th) {
            try {
                Toast.makeText(this.host, R.string.reward_open_failed, Toast.LENGTH_SHORT).show();
            } catch (Throwable ignored) {
            }
        }
    }
}
