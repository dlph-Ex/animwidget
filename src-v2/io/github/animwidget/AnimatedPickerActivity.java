package io.github.animwidget;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 内置动图选择器（v1.10.36）。
 *
 * 为什么不用系统选择器：系统相册/文件选择器只能按 MIME 过滤，而
 *   GIF 的 MIME 不区分动图（都是 image/gif）
 *   APNG 是 image/png（和普通截图一样）
 *   动图 WebP 是 image/webp（和静态 WebP 一样）
 * 所以要么「只显示 GIF」（APNG/WebP 选不到），要么「所有图片都能点」。
 * 这里扫 MediaStore → 读文件头判真动图（GIF / APNG / 动图 WebP / AVIF 序列）→ 只摆真会动的。
 *
 * 右侧「快滑组件」按用户给的相册样式做：贴边的一整块（上下箭头）+ 紧贴它的日期胶囊，
 * 两者是同一个 View —— 时间就是滑块的一部分，不是独立的浮层。
 */
public final class AnimatedPickerActivity extends I18nActivity {

    private static final int REQ_PERM = 0x9E21;
    /** 一次最多列 400 个，避免超大相册把界面拖死 */
    private static final int MAX_ITEMS = 400;

    /**
     * v1.10.129：**相册扫描并行度**。
     * 每张候选只读 256 字节（PNG 才继续读到 64KB）判断「是不是真的会动」——
     * 这一步几乎全是 I/O（open/read/close 一次 2~10ms），串行扫几百张就是好几秒。
     * 4 路并行实测能把等待时间压到 1/3 左右，同时不至于把 MediaProvider 打爆。
     */
    private static final int SCAN_THREADS = 4;

    /**
     * v1.10.131（D4）：扫描线程数改由**设置页**控制（默认 4，范围 1~8）。
     * 相册极大时可以调到 6~8；小相册保持 4 省电。
     */
    static int scanThreads(Context context) {
        try {
            int v = context.getSharedPreferences("settings", 0).getInt("scan_threads", SCAN_THREADS);
            return v < 1 ? 1 : (v > 8 ? 8 : v);
        } catch (Throwable th) {
            return SCAN_THREADS;
        }
    }

    /** 页面被关掉时置位，扫描线程与工作线程都能尽快退出。 */
    private final java.util.concurrent.atomic.AtomicBoolean scanCancelled =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private final Handler ui = new Handler(Looper.getMainLooper());
    @Nullable
    private GridView grid;
    @Nullable
    private TextView status;
    /** v1.10.314：标题下的副行（「共 N 个动图 · 点一个直接用」）。扫描期间隐藏。 */
    @Nullable
    private TextView subtitle;
    @Nullable
    private ScrollPill pill;
    /** v1.10.68：最近一次滚动状态 —— 用来区分「用户真的在滑」和「setAdapter 触发的那一次」。 */
    private int lastScrollState = AbsListView.OnScrollListener.SCROLL_STATE_IDLE;
    private final List<Item> shown = new ArrayList<>();

    private static final class Item {
        final long id;
        final Uri uri;
        /** 修改时间（秒）—— 快滑时胶囊里显示的日期 */
        final long date;
        final int cell;
        /**
         * v1.10.176：MediaStore 里的宽高（读游标就有，**零额外 I/O**）。
         * 只用来算「清晰度」；为 0 时退回到解析容器拿到的尺寸。
         */
        final int width;
        final int height;
        /** 时长（ms）：-1 = 还没问过，0 = 问过了但读不到 */
        int durationMs = -1;
        /** v1.10.457：这个文件的帧率 ×10（由帧数 / 时长反算，0 = 还不知道）。 */
        int fpsX10;
        boolean durationEstimated;
        /** 容器里读到的尺寸（MediaStore 没给时兜底） */
        int probedWidth;
        int probedHeight;
        boolean durationTried;

        Item(long id, Uri uri, long date, int cell, int width, int height) {
            this.id = id;
            this.uri = uri;
            this.date = date;
            this.cell = cell;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * v1.10.314：网格几何（列数 + 格子边长）。
     *
     * 原来列数写死在 XML（numColumns=4），格子宽度又在 apply() 和 ThumbAdapter 里各算一遍
     * `(屏宽 − 6dp) / 4`：大屏上 4 列会把缩略图撑成大块，而「算出来的格子宽」与
     * GridView 真实列宽（stretchMode=columnWidth 会重新分配）差 1~2px，每格右下角留一条缝。
     *
     * 现在按「目标格子 ≈ 96dp」反推列数（384dp 宽的机器 → 4 列，与旧版一致），
     * 再按 GridView 真正可用的宽（屏宽 − 两侧 padding − 列间距）把格子宽算准；
     * GridView 与 adapter 吃同一份数，缝隙归零。
     */
    static final class GridMetrics {
        /** 目标格子边长（dp）：384dp 宽的机器正好 4 列 */
        private static final float TARGET_CELL_DP = 96f;
        final int columns;
        final int cell;
        final int spacing;
        final int pad;

        private GridMetrics(int columns, int cell, int spacing, int pad) {
            this.columns = columns;
            this.cell = cell;
            this.spacing = spacing;
            this.pad = pad;
        }

        @NotNull
        static GridMetrics of(@NotNull Context context) {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            float dp = dm.density > 0f ? dm.density : 1f;
            int width = dm.widthPixels > 0 ? dm.widthPixels : Math.round(360f * dp);
            int spacing = Math.max(1, Math.round(2f * dp));
            int pad = Math.max(1, Math.round(2f * dp));
            int columns = Math.round((width / dp) / TARGET_CELL_DP);
            columns = Math.max(3, Math.min(8, columns));
            int usable = width - pad * 2 - spacing * (columns - 1);
            int cell = Math.max(usable / Math.max(columns, 1), Math.round(64f * dp));
            return new GridMetrics(columns, cell, spacing, pad);
        }
    }

    /** v1.10.314：扫描进度回调（已查候选 / 候选总数），给状态行显示用。 */
    private interface ScanProgress {
        void onProgress(int done, int total);
    }

    /** v1.10.314：格子被点中（点格子 = 直接用这个动图）。 */
    private interface Pick {
        void onPick(@NotNull Item item);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashLog.install(this);
        setContentView(R.layout.activity_anim_picker);
        setI18nLayout(R.layout.activity_anim_picker);
        EdgeToEdge.INSTANCE.apply(this);
        this.grid = (GridView) findViewById(R.id.grid_anim_pick);
        this.status = (TextView) findViewById(R.id.tv_anim_pick_status);
        this.subtitle = (TextView) findViewById(R.id.tv_anim_pick_sub);
        Button back = (Button) findViewById(R.id.btn_anim_pick_back);
        if (back != null) {
            back.setOnClickListener((v) -> finish());
        }
        applyTheme();
        if (hasPermission()) {
            startScan();
        } else {
            requestPermissions(new String[]{permission()}, REQ_PERM);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncLocale();
        applyTheme();
    }

    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // v1.10.40：原地换肤（recreate 会重新扫描 + 可能把页面顶掉）
        syncLocale();
        applyTheme();
        if (this.grid != null) {
            android.widget.ListAdapter adapter = this.grid.getAdapter();
            if (adapter instanceof BaseAdapter) {
                ((BaseAdapter) adapter).notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_PERM) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startScan();
        } else if (this.status != null) {
            this.status.setText(R.string.anim_picker_no_perm);
        }
    }

    /** 整页跟随主题（黑白模式、主色/中性色）。 */
    private void applyTheme() {
        try {
            View content = findViewById(android.R.id.content);
            ThemeManager.INSTANCE.applyTo(content);
            ThemeManager.INSTANCE.applySystemBars(this);
            ThemeColors colors = ThemeManager.INSTANCE.colors(this);
            View root = findViewById(android.R.id.content);
            if (root != null) {
                root.setBackgroundColor(colors.getBackground());
            }
            TextView title = (TextView) findViewById(R.id.tv_anim_pick_title);
            if (title != null) {
                title.setTextColor(colors.getText());
            }
            if (this.status != null) {
                this.status.setTextColor(colors.getTextMuted());
            }
            if (this.subtitle != null) {
                this.subtitle.setTextColor(colors.getTextMuted());
            }
            if (this.pill != null) {
                this.pill.applyColors(colors);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onTextsRefreshed() {
        // v1.10.314：副行是代码里填的文案（共 N 个动图），切语言后按当前语言重填一遍
        TextView sub = this.subtitle;
        if (sub != null && sub.getVisibility() == View.VISIBLE) {
            sub.setText(s(R.string.anim_picker_count, this.shown.size()));
        }
    }

    private boolean hasPermission() {
        try {
            return checkSelfPermission(permission()) == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable th) {
            return false;
        }
    }

    private static String permission() {
        return Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        this.scanCancelled.set(true);
        // v1.10.176：把本次读到的「时长/清晰度」写回磁盘缓存（下次进页面零 I/O、零解析）
        AnimDuration.saveDisc(this);
        super.onDestroy();
    }

    private void startScan() {
        if (this.status != null) {
            this.status.setVisibility(View.VISIBLE);
            this.status.setText(R.string.anim_picker_scanning);
        }
        if (this.subtitle != null) {
            this.subtitle.setVisibility(View.GONE);
        }
        if (this.grid != null) {
            this.grid.setVisibility(View.GONE);
        }
        this.scanCancelled.set(false);
        new Thread(() -> {
            final List<Item> found = scan(this, this.scanCancelled, (done, total) -> {
                if (this.scanCancelled.get()) {
                    return;
                }
                this.ui.post(() -> {
                    TextView line = this.status;
                    // 只更新「还在显示扫描文案」的那一版，别把「没有动图/没有权限」覆盖掉
                    if (line != null && line.getVisibility() == View.VISIBLE && total > 0) {
                        line.setText(s(R.string.anim_picker_scan_progress, done, total));
                    }
                });
            });
            if (this.scanCancelled.get()) {
                return;
            }
            this.ui.post(() -> apply(found));
        }, "anim-scan-main").start();
    }

    private void apply(List<Item> found) {
        if (found.isEmpty()) {
            if (this.status != null) {
                this.status.setVisibility(View.VISIBLE);
                this.status.setText(R.string.anim_picker_empty);
            }
            if (this.grid != null) {
                this.grid.setVisibility(View.GONE);
            }
            return;
        }
        if (this.status != null) {
            this.status.setVisibility(View.GONE);
        }
        final GridView g = this.grid;
        if (g == null) {
            return;
        }
        // v1.10.314：列数/格子尺寸只在这里算一次，GridView 与 adapter 共用同一个几何。
        // 原来 apply() 和 ThumbAdapter 各算一遍 `(screen-6dp)/4`，一旦列数变了就会对不上。
        final GridMetrics m = GridMetrics.of(this);
        g.setNumColumns(m.columns);
        g.setHorizontalSpacing(m.spacing);
        g.setVerticalSpacing(m.spacing);
        g.setPaddingRelative(m.pad, m.pad, m.pad, m.pad);
        final List<Item> data = new ArrayList<>();
        int known = 0;
        for (Item item : found) {
            data.add(new Item(item.id, item.uri, item.date, m.cell, item.width, item.height));
            if (AnimDuration.cached(item.id, item.date) != null) {
                known++;
            }
        }
        // v1.10.176：封面左下角标签的命中情况（缓存命中 = 这次不会再去读文件）
        Diag.warn(this, "K06", "封面标签 共=" + found.size()
                + " 时长缓存命中=" + known + " 待解析=" + (found.size() - known));
        this.shown.clear();
        this.shown.addAll(data);
        g.setVisibility(View.VISIBLE);
        g.setAdapter(new ThumbAdapter(this, data, m, this::pickResult));
        if (this.subtitle != null) {
            this.subtitle.setText(s(R.string.anim_picker_count, data.size()));
            this.subtitle.setVisibility(View.VISIBLE);
        }
        g.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                AnimatedPickerActivity.this.lastScrollState = scrollState;
                if (scrollState == SCROLL_STATE_IDLE) {
                    AnimatedPickerActivity.this.pillFadeOut(700L);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (totalItemCount <= 0) {
                    return;
                }
                ScrollPill p = AnimatedPickerActivity.this.pill;
                if (p != null) {
                    p.update(firstVisibleItem, visibleItemCount, totalItemCount);
                    // v1.10.68：**只有用户真的在滑（拖动/惯性）才亮起滑块**。
                    // setAdapter() 触发的第一次 onScroll 是 IDLE 状态，以前也会 flash()
                    // —— 那正是「一进来滑块亮一下又消失」的直接原因。
                    if (AnimatedPickerActivity.this.lastScrollState != SCROLL_STATE_IDLE) {
                        p.flash();
                    }
                }
            }
        });
        // v1.10.314：格子自己接点击（为了按压涟漪能真的亮起来 —— 不接点击的 ListView/GridView
        // 子项拿不到 pressed 状态，foreground 涟漪就永远不会动）。键盘/无障碍走的仍然是
        // GridView 的 item click，两条路都汇到 pickResult()，不会重复触发。
        g.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= data.size()) {
                return;
            }
            pickResult(data.get(position));
        });
        setupPill(g, data);
    }

    /** 选中一个动图 → 回传 uri 并关页面。 */
    private void pickResult(@NotNull Item item) {
        Intent result = new Intent();
        result.setData(item.uri);
        setResult(RESULT_OK, result);
        finish();
    }

    // ------------------------------------------------------------------ 快滑组件

    private void setupPill(GridView g, List<Item> data) {
        try {
            FrameLayout root = (FrameLayout) findViewById(android.R.id.content);
            if (root == null) {
                return;
            }
            ScrollPill pill = new ScrollPill(this, g, data, () -> this.pillFadeOut(700L));
            // 关键：整块贴紧屏幕右边缘（rightMargin = 0），垂直居中
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
            // v1.10.315：退回贴边（v1.10.314 曾右移 12dp，实机反馈「没有贴边」）
            lp.rightMargin = 0;
            pill.setLayoutParams(lp);
            root.addView(pill);
            this.pill = pill;
            applyTheme();
            // v1.10.68：扫描完只把滑块摆到当前进度，**不显示**。
            // 用户不动它就不该出现（以前 reveal 会淡入再淡出，看起来像闪一下的 bug）。
            // （post 一帧：此刻 GridView 还没量过，getHeight() 还是 0）
            pill.post(pill::prepareHidden);
        } catch (Throwable ignored) {
        }
    }

    private void pillFadeOut(long delay) {
        ScrollPill p = this.pill;
        // v1.10.68：本来就没显示（或正在被拖）就别排淡出 —— 以前进入页面时会白排一次动画
        if (p == null || p.isDragging() || p.getVisibility() != View.VISIBLE) {
            return;
        }
        p.scheduleFadeOut(delay);
    }

    /**
     * v1.10.37：右侧快滑组件（按用户相册截图重做）。
     *
     *   ┌──────────────────┐
     *   │  2025年9月6日  │ ▲ │
     *   └──────────────────┘ ▼ │
     *
     * • 整个组件沿右侧上下移动，位置 = 当前浏览进度（不是固定居中）
     * • 整块都能按住拖（上一版只有右侧按钮块接收，按胶囊拖不动）
     * • 拖动时把 GridView 用「行 + 行内偏移」连续滚动，不再整格跳
     * • 不透明配色、尺寸缩小
     */
    private static final class ScrollPill extends View {
        private final GridView target;
        private final List<Item> data;
        private final Runnable onIdle;
        private final float density;
        private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path arrowUp = new Path();
        private final Path arrowDown = new Path();
        private final RectF rect = new RectF();
        // v1.10.311（方案 A）：滑块自身透明度状态机 —— 未触摸 0.55 / 按住 1.0，都带渐变。
        // 注意：日期投影**不跟它变**，恒 0.55（用户硬性要求）。
        private float barAlpha = 0.55f;
        private android.animation.ValueAnimator barAnim;
        // v1.10.312（用户："日期投影文本加短暂叠化：旧文渐隐→新文渐显"）
        private float textAlpha = 1.0f;
        private android.animation.ValueAnimator textAnim;

        private final int barWidth;
        private final int barHeight;
        private final int gap;
        private final int bubbleHeight;
        private final int bubblePadH;

        private final Runnable hideTask = this::fadeOut;
        private int total = 1;
        private int visible = 1;
        private int first = 0;
        private String label = "";
        private float bubbleWidth;
        private boolean dragging;
        private float downRawY;
        private float dragStartFraction;
        private int cellSize;
        @Nullable
        private ThemeColors colors;

        ScrollPill(Context context, GridView target, List<Item> data, Runnable onIdle) {
            super(context);
            this.target = target;
            this.data = data;
            this.onIdle = onIdle;
            this.density = context.getResources().getDisplayMetrics().density;
            // v1.10.315（回滚 v1.10.314 的放大）：
            // 314 按「48dp 触摸靶心」把胶囊从 36dp 放大到 44dp、并把组件右移 12dp 离屏 —
            // 实机反馈「滑块没有贴边，而且变大了不少」。**贴边和原来的大小是用户已定型的观感**，
            // 规范只是下限参考，不能拿它去改一个用户说"手感可以"的东西。这里全部退回 313 的数值。
            this.barWidth = Math.round(36 * this.density);
            this.barHeight = Math.round(76 * this.density);
            this.gap = Math.round(5 * this.density);
            this.bubbleHeight = Math.round(36 * this.density);
            this.bubblePadH = Math.round(14 * this.density);
            this.cellSize = Math.round(72 * this.density);
            // v1.10.174：跟随「字体大小」档位
            // v1.10.175：字号基数用 scaledDensity（含系统字体倍率）
            float sp = context.getResources().getDisplayMetrics().scaledDensity;
            this.textPaint.setTextSize(13 * sp * Fonts.textScale(context));
            this.textPaint.setAntiAlias(true);
            this.arrowPaint.setStyle(Paint.Style.STROKE);
            this.arrowPaint.setStrokeWidth(2.0f * this.density);
            this.arrowPaint.setStrokeCap(Paint.Cap.ROUND);
            this.arrowPaint.setStrokeJoin(Paint.Join.ROUND);
            this.barPaint.setStyle(Paint.Style.FILL);
            this.bubblePaint.setStyle(Paint.Style.FILL);
            // 兜底色（applyColors 之前不会显示）
            this.barPaint.setColor(0xFF5A6B92);
            this.bubblePaint.setColor(0xFF5A6B92);
            this.textPaint.setColor(0xFFFFFFFF);
            this.arrowPaint.setColor(0xFFFFFFFF);
            setAlpha(0.0f);
            setVisibility(GONE);
        }

        /**
         * v1.10.47：滑块色正式进配色系统。
         *   平时 = 滑块色（半透明）、拖动中 = 主色（不透明）；
         *   日期胶囊同理（平时辅助色半透明，拖动时主色）；文字/箭头按底色亮度自动取黑或白。
         */
        void applyColors(ThemeColors c) {
            this.colors = c;
            syncPaints();
            invalidate();
        }

        private void syncPaints() {
            ThemeColors c = this.colors;
            if (c == null) {
                return;
            }
            ThemeManager manager = ThemeManager.INSTANCE;
            // v1.10.301（用户："滑块没有完全适配滑块色（不是指透明度）"）：
            // 原来不拖动时把颜色自己压到 55%/62% —— 于是"看起来的颜色"是
            // 「滑块色 × 背景」的混合值，永远不是滑块色本身，箭头又是按**未压暗**的颜色
            // 算对比色，两者对不上 → 就是"没完全适配"。
            // 现在颜色一律用**原色**（拖动=主色 / 静止=滑块色、辅助色），
            // 淡入淡出完全交给 View 自己的 alpha 动画（进入页面不可见 → 拖动渐显 → 停手渐隐）。
            // v1.10.302（用户逐条指定）：
            //   · 滑块与日期投影**都用系统滑块色**，不许和主色/辅助色混用；
            //   · 滑块：未触摸 55%（半透明）、按住 100%（不透明）；
            //   · 日期投影：它是滑块的一部分，**永远保持半透明**，且不可触摸；
            //   · 渐变动画随下一版「动图选择页面大更新」一起做（样式也要一起改），这版只钉颜色语义。
            int slider = c.getSlider();
            this.barPaint.setColor(manager.withAlpha(slider, this.barAlpha));
            this.arrowPaint.setColor(manager.onColor(slider));
            this.bubblePaint.setColor(manager.withAlpha(slider, 0.55f));
            this.textPaint.setColor(manager.onColor(slider));
        }

        boolean isDragging() {
            return this.dragging;
        }

        void scheduleFadeOut(long delay) {
            removeCallbacks(this.hideTask);
            postDelayed(this.hideTask, delay);
        }

        /**
         * v1.10.68：**进入页面时只定位、不显示**。
         *
         * 以前这里调的是 reveal()：滑块先淡入、2.6 秒无人操作再淡出 —— 用户完全没动，
         * 却看到右侧滑块「亮一下又消失」，观感就是个 bug。现在首帧只把位置算好（保证第一次
         * 滚动时它出现在正确进度上），保持 GONE，直到用户真的开始滑才显示。
         */
        void prepareHidden() {
            removeCallbacks(this.hideTask);
            animate().cancel();
            applyPosition(currentFraction());
            setAlpha(0.0f);
            setVisibility(GONE);
        }

        /** v1.10.47：淡入 + 定位 + 一段时间不操作自动淡出（v1.10.68 起进入页面不再调用）。 */
        void reveal() {
            removeCallbacks(this.hideTask);
            animate().cancel();
            if (getVisibility() != VISIBLE) {
                setVisibility(VISIBLE);
            }
            setAlpha(1.0f);
            applyPosition(currentFraction());
            scheduleFadeOut(2600L);
        }

        void fadeOut() {
            if (this.dragging) {
                return;
            }
            animate().cancel();
            animate().alpha(0.0f).setDuration(240L).withEndAction(() -> {
                if (!this.dragging) {
                    setVisibility(GONE);
                }
            }).start();
        }

        void flash() {
            if (this.dragging) {
                return;
            }
            removeCallbacks(this.hideTask);
            if (getVisibility() != VISIBLE) {
                setVisibility(VISIBLE);
            }
            if (getAlpha() < 1.0f) {
                animate().cancel();
                setAlpha(1.0f);
            }
            // 组件位置 = 浏览进度
            applyPosition(currentFraction());
            // 停止滑动 1.6s 后自动淡出（原来只有「松手」才排淡出，扫描完不动就一直挂着）
            scheduleFadeOut(1600L);
        }

        void update(int firstVisible, int visibleCount, int totalCount) {
            this.first = firstVisible;
            this.visible = Math.max(visibleCount, 1);
            this.total = Math.max(totalCount, 1);
            refreshLabel(firstVisible);
            if (!this.dragging) {
                applyPosition(currentFraction());
            }
            invalidate();
        }

        private void refreshLabel(int firstVisible) {
            if (this.data.isEmpty()) {
                return;
            }
            int index = Math.min(Math.max(firstVisible, 0) / Math.max(this.target.getNumColumns(), 1),
                    this.data.size() - 1);
            String next = formatDate(this.data.get(index).date);
            if (!next.equals(this.label)) {
                setLabelFade(next);
                this.bubbleWidth = this.textPaint.measureText(this.label) + this.bubblePadH * 2f;
                requestLayout();
            }
        }

        private String formatDate(long seconds) {
            try {
                if (seconds <= 0L) {
                    return "";
                }
                return android.text.format.DateFormat.getMediumDateFormat(getContext())
                        .format(new java.util.Date(seconds * 1000L));
            } catch (Throwable th) {
                return "";
            }
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            int width = (int) (this.bubbleWidth + this.gap + this.barWidth);
            int height = Math.max(this.barHeight, this.bubbleHeight);
            setMeasuredDimension(Math.max(width, this.barWidth), height);
        }

        /** v1.10.311：滑块透明度渐变（150ms 按住、220ms 松开），投影不参与。 */
        private void animateBarAlpha(float to) {
            if (this.barAnim != null) {
                this.barAnim.cancel();
            }
            this.barAnim = android.animation.ValueAnimator.ofFloat(this.barAlpha, to);
            this.barAnim.setDuration(to > this.barAlpha ? 150L : 220L);
            this.barAnim.addUpdateListener(a -> {
                Object v = a.getAnimatedValue();
                if (v instanceof Float) {
                    this.barAlpha = ((Float) v).floatValue();
                    syncPaints();
                    invalidate();
                }
            });
            this.barAnim.start();
        }

        /** v1.10.312：投影文本换字时做一次叠化（旧 90ms 渐隐 → 换字 → 新 140ms 渐显）。 */
        private void setLabelFade(String next) {
            if (next == null || next.equals(this.label)) {
                this.label = next == null ? this.label : next;
                return;
            }
            if (this.textAnim != null) {
                this.textAnim.cancel();
            }
            this.textAnim = android.animation.ValueAnimator.ofFloat(this.textAlpha, 0.0f);
            this.textAnim.setDuration(90L);
            this.textAnim.addUpdateListener(a -> {
                Object v = a.getAnimatedValue();
                if (v instanceof Float) {
                    this.textAlpha = ((Float) v).floatValue();
                    invalidate();
                }
            });
            this.textAnim.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator a) {
                    label = next;
                    textAnim = android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
                    textAnim.setDuration(140L);
                    textAnim.addUpdateListener(aa -> {
                        Object v2 = aa.getAnimatedValue();
                        if (v2 instanceof Float) {
                            textAlpha = ((Float) v2).floatValue();
                            invalidate();
                        }
                    });
                    textAnim.start();
                }
            });
            this.textAnim.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float barLeft = getWidth() - this.barWidth;
            float barTop = (getHeight() - this.barHeight) / 2.0f;
            // v1.10.315：圆角退回 0.45×宽（313 的观感），不再用整颗半宽胶囊
            float radius = this.barWidth * 0.45f;
            this.rect.set(barLeft, barTop, getWidth(), barTop + this.barHeight);
            canvas.drawRoundRect(this.rect, radius, radius, this.barPaint);
            float cx = barLeft + this.barWidth / 2.0f;
            float upY = barTop + this.barHeight * 0.33f;
            float downY = barTop + this.barHeight * 0.67f;
            float w = 6 * this.density;
            float h = 4 * this.density;
            this.arrowUp.reset();
            this.arrowUp.moveTo(cx - w, upY + h);
            this.arrowUp.lineTo(cx, upY - h);
            this.arrowUp.lineTo(cx + w, upY + h);
            canvas.drawPath(this.arrowUp, this.arrowPaint);
            this.arrowDown.reset();
            this.arrowDown.moveTo(cx - w, downY - h);
            this.arrowDown.lineTo(cx, downY + h);
            this.arrowDown.lineTo(cx + w, downY - h);
            canvas.drawPath(this.arrowDown, this.arrowPaint);
            if (this.bubbleWidth > 0 && this.label.length() > 0) {
                float bubbleRight = barLeft - this.gap;
                float bubbleLeft = bubbleRight - this.bubbleWidth;
                float bubbleTop = (getHeight() - this.bubbleHeight) / 2.0f;
                this.rect.set(bubbleLeft, bubbleTop, bubbleRight, bubbleTop + this.bubbleHeight);
                float br = this.bubbleHeight / 2.0f;
                canvas.drawRoundRect(this.rect, br, br, this.bubblePaint);
                Paint.FontMetrics fm = this.textPaint.getFontMetrics();
                float baseline = bubbleTop + this.bubbleHeight / 2.0f - (fm.ascent + fm.descent) / 2.0f;
                this.textPaint.setAlpha(Math.round(255f * this.textAlpha));
                canvas.drawText(this.label, bubbleLeft + this.bubblePadH, baseline, this.textPaint);
            }
        }

        /**
         * 组件沿右侧移动到 fraction 对应的位置。
         * v1.10.39：范围严格对齐 GridView 的可见区域 ——
         * 顶端 = 第一行（不会冲到屏幕最上方），底端 = 最后一行。
         */
        private void applyPosition(float fraction) {
            if (this.target == null) {
                return;
            }
            float top = gridTopInParent();
            float track = Math.max(this.target.getHeight() - getHeight(), 1f);
            float f = Math.min(1.0f, Math.max(0.0f, fraction));
            setY(top + track * f);
        }

        /**
         * v1.10.47：拖动改成「手指 → 目标进度 → 列表滚到那里 → 滑块画在列表真实进度上」。
         *
         * 旧实现是「列表 scrollListBy(dy) + 滑块自己 setY(getY()+dy)」，两条腿各走各的：
         *   滑块的可移动范围（GridView 高度 − 滑块高度）比列表真正的可滚范围小，
         *   于是滑块先到底、列表还没到底 —— 松手后 snapToPosition 硬对齐，用户看到「抖一下才跳到底」。
         * 现在滑块位置永远由列表真实进度算出来，两边天然一致，松手不需要也不允许再对齐。
         */
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    if (this.total <= this.visible) {
                        return false;
                    }
                    // v1.10.311：日期投影是滑块的一部分但**不可触摸** —— 落在这里直接穿透
                    if (this.bubbleWidth > 0 && this.label.length() > 0) {
                        float left4 = getWidth() - this.barWidth - this.gap - this.bubbleWidth;
                        float right4 = getWidth() - this.barWidth - this.gap;
                        float top4 = (getHeight() - this.bubbleHeight) / 2.0f;
                        if (event.getX() >= left4 && event.getX() <= right4
                                && event.getY() >= top4 && event.getY() <= top4 + this.bubbleHeight) {
                            return false;
                        }
                    }
                    this.dragging = true;
                    animateBarAlpha(1.0f);
                    this.downRawY = event.getRawY();
                    this.dragStartFraction = currentFraction();
                    removeCallbacks(this.hideTask);
                    animate().cancel();
                    setAlpha(1.0f);
                    syncPaints();
                    invalidate();
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (!this.dragging) {
                        return false;
                    }
                    float track = trackPixels();
                    if (track <= 1.0f) {
                        return true;
                    }
                    // 目标进度 = 按下时的进度 + 手指位移 / 轨道长度（滑块跟手）
                    float target = this.dragStartFraction + (event.getRawY() - this.downRawY) / track;
                    float f = Math.min(1.0f, Math.max(0.0f, target));
                    int range = scrollRangePx();
                    if (range > 0) {
                        int want = (int) (f * range);
                        int have = scrollOffsetPx();
                        int delta = want - have;
                        if (delta != 0) {
                            // 逐像素滚动，不整格跳
                            try {
                                this.target.scrollListBy(delta);
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                    // 滑块永远画在「列表现在的进度」上 —— 页面实时跟随，不会和手指错位
                    applyPosition(currentFraction());
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (!this.dragging) {
                        return false;
                    }
                    this.dragging = false;
                    animateBarAlpha(0.55f);
                    syncPaints();
                    invalidate();
                    // 没错位就没有可对齐的东西：松手只做一次「按真实进度重画」
                    applyPosition(currentFraction());
                    if (this.onIdle != null) {
                        this.onIdle.run();
                    }
                    return true;
                }
                default:
                    return this.dragging;
            }
        }

        /**
         * v1.10.40：GridView 相对本 View 父容器的 top。
         * 原来直接用 target.getTop()，那是相对 LinearLayout 的坐标；
         * 而本 View 挂在 content(FrameLayout) 上 —— 两者差着状态栏/标题栏，
         * 所以拖动到底/到顶都会差一截（用户反馈「拖到顶没到顶」）。
         */
        private float gridTopInParent() {
            View parent = getParent() instanceof View ? (View) getParent() : null;
            float top = 0f;
            View child = this.target;
            int guard = 0;
            while (child != null && child != parent && guard++ < 16) {
                top += child.getTop();
                android.view.ViewParent vp = child.getParent();
                child = vp instanceof View ? (View) vp : null;
            }
            return top;
        }

        /** 滑块能移动的像素范围（GridView 可见高度 − 滑块自身高度）。 */
        private float trackPixels() {
            if (this.target == null) {
                return 0f;
            }
            return Math.max(this.target.getHeight() - getHeight(), 1f);
        }

        private int columns() {
            return Math.max(this.target.getNumColumns(), 1);
        }

        /**
         * v1.10.47：行高优先用「两个相邻行」的实测差值，测不到才退回「格子高度 + 2dp」。
         * 之前一律用密里算的 +2dp，与 GridView 真实行距差 1~2px，
         * 乘上几十行后进度估算就偏了 —— 这就是滑动时滑块抖的原因。
         */
        private int rowHeight() {
            try {
                int cols = columns();
                if (this.target.getChildCount() > cols) {
                    View a = this.target.getChildAt(0);
                    View b = this.target.getChildAt(cols);
                    if (a != null && b != null && b.getTop() > a.getTop()) {
                        return b.getTop() - a.getTop();
                    }
                }
                if (this.target.getChildCount() > 0) {
                    View child = this.target.getChildAt(0);
                    if (child != null && child.getHeight() > 0) {
                        this.cellSize = child.getHeight();
                    }
                }
            } catch (Throwable ignored) {
            }
            return Math.max(this.cellSize + (int) (2 * this.density), 1);
        }

        /**
         * v1.10.47：进度用「已滚动像素 / 可滚动像素」，两个值出自同一套估算（行号 × 行高），
         * 所以比值连续、单调：到顶 = 0、到底 = 1，中间不会因为行高估算偏差一跳一跳。
         * （View.computeVerticalScrollOffset 是 protected，取不到，只能自己算。）
         */
        private float currentFraction() {
            int range = scrollRangePx();
            if (range <= 0) {
                return 0.0f;
            }
            return Math.min(1.0f, Math.max(0.0f, scrollOffsetPx() / (float) range));
        }

        /** 已滚动像素：行号 × 行高 + 顶部内边距 − 第一个可见格子的 top。 */
        private int scrollOffsetPx() {
            if (this.target == null || this.target.getChildCount() == 0) {
                return 0;
            }
            View c0 = this.target.getChildAt(0);
            if (c0 == null) {
                return 0;
            }
            int cols = columns();
            int rowH = rowHeight();
            int first = this.target.getFirstVisiblePosition();
            return (first / cols) * rowH + this.target.getPaddingTop() - c0.getTop();
        }

        /** 列表真正能滚动的像素（行数 × 行高 − 视口高度）。 */
        private int scrollRangePx() {
            if (this.target == null) {
                return 0;
            }
            int cols = columns();
            int rows = (this.total + cols - 1) / cols;
            int rowH = rowHeight();
            int viewport = Math.max(this.target.getHeight()
                    - this.target.getPaddingTop() - this.target.getPaddingBottom(), 1);
            return Math.max(rows * rowH - viewport, 0);
        }

    }

    // ------------------------------------------------------------------ 扫描

    /**
     * v1.10.130：**动图探测结果缓存**。
     *
     * 实测（K04）：838 个候选、1225ms、4 线程 —— 684 张/秒，正好是串行 4.9s 的 1/4，
     * 说明瓶颈是「每张一次跨进程 open + 读文件头」。
     * 而这一步的结论对同一个文件是稳定的（只跟 修改时间 有关），没理由每次进页面重扫一遍。
     *
     * 存储：`files/anim_probe.txt`，每行 `<图片id> <修改时间> <0|1>`。
     * 命中条件：id 相同且修改时间相同 → 直接采用缓存结论，**一次 open 都不做**。
     * 每次扫完按「本次见到的候选」回写，等于自动做过期清理（删掉的照片自然消失）。
     */
    @org.jetbrains.annotations.NotNull
    private static java.io.File probeCacheFile(Context context) {
        return new java.io.File(context.getFilesDir(), "anim_probe.txt");
    }

    @org.jetbrains.annotations.NotNull
    private static java.util.HashMap<Long, long[]> loadProbeCache(Context context) {
        java.util.HashMap<Long, long[]> map = new java.util.HashMap<>();
        java.io.BufferedReader reader = null;
        try {
            reader = new java.io.BufferedReader(new java.io.FileReader(probeCacheFile(context)));
            String line;
            while ((line = reader.readLine()) != null) {
                int a = line.indexOf(' ');
                int b = line.lastIndexOf(' ');
                if (a <= 0 || b <= a) {
                    continue;
                }
                long id = Long.parseLong(line.substring(0, a));
                long date = Long.parseLong(line.substring(a + 1, b));
                long flag = Long.parseLong(line.substring(b + 1));
                map.put(Long.valueOf(id), new long[]{date, flag});
            }
        } catch (Throwable ignored) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable ignored) {
                }
            }
        }
        return map;
    }

    private static void saveProbeCache(Context context, @org.jetbrains.annotations.NotNull java.util.Map<Long, long[]> map) {
        java.io.BufferedWriter writer = null;
        java.io.File tmp = new java.io.File(context.getFilesDir(), "anim_probe.txt.tmp");
        try {
            writer = new java.io.BufferedWriter(new java.io.FileWriter(tmp));
            StringBuilder sb = new StringBuilder(32);
            int written = 0;
            for (java.util.Map.Entry<Long, long[]> e : map.entrySet()) {
                if (written >= 8000) {
                    break;
                }
                sb.setLength(0);
                sb.append(e.getKey().longValue()).append(' ')
                        .append(e.getValue()[0]).append(' ')
                        .append(e.getValue()[1]);
                writer.write(sb.toString());
                writer.newLine();
                written++;
            }
            writer.flush();
            writer.close();  // 必须先关：既保证落盘，也不泄漏文件描述符
            writer = null;   // 已关，finally 不用再关
            java.io.File target = probeCacheFile(context);
            if (!tmp.renameTo(target)) {
                tmp.delete();
            }
        } catch (Throwable ignored) {
            tmp.delete();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static List<Item> scan(Context context,
                                   @org.jetbrains.annotations.Nullable java.util.concurrent.atomic.AtomicBoolean cancelled,
                                   @org.jetbrains.annotations.Nullable ScanProgress progress) {
        List<Item> out = new ArrayList<>();
        List<Item> batch = new ArrayList<>();
        Cursor cursor = null;
        java.util.concurrent.ExecutorService pool = null;
        long started = android.os.SystemClock.elapsedRealtime();
        int candidates = 0;
        int cachedHits = 0;
        int probed = 0;
        final java.util.HashMap<Long, long[]> probeCache = loadProbeCache(context);
        // v1.10.131（D3）：写回只写「本次真的扫到的条目」，顺序即游标顺序（修改时间倒序）。
        // 这样文件永远镜像当前相册：删掉的照片自动消失，超 8000 条时被截掉的是最老的，
        // 而不是原来「HashMap 随机顺序截断」——那会把还热着的条目丢掉。
        final java.util.LinkedHashMap<Long, long[]> freshCache = new java.util.LinkedHashMap<>();
        try {
            Uri base = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.MIME_TYPE,
                    MediaStore.Images.Media.DATE_MODIFIED,
                    // v1.10.176：宽高直接从游标取（相册扫描时系统已经抽好了），
                    // 给封面左下角的「清晰度」用 —— 不用为此再开一次文件。
                    MediaStore.Images.Media.WIDTH,
                    MediaStore.Images.Media.HEIGHT
            };
            // 白名单查询 —— 只从「可能是动图」的格式里取候选。
            // JPEG/BMP/TIFF/ICO/DNG 这些不常见格式根本进不了结果集。
            String selection = MediaStore.Images.Media.MIME_TYPE
                    + " IN ('image/gif','image/png','image/webp','image/avif','image/heic','image/heif')"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + " IS NULL"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + " = ''"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + " = 'application/octet-stream'";
            cursor = context.getContentResolver().query(base, projection, selection, null,
                    MediaStore.Images.Media.DATE_MODIFIED + " DESC");
            if (cursor == null) {
                return out;
            }
            int threads = scanThreads(context);
            // v1.10.176：时长缓存先读进内存（同一个后台线程，之后按 id+修改时间直接命中）
            AnimDuration.loadDisc(context);
            int wi = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH);
            int hi = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT);
            // v1.10.314：候选总数（状态行「已查 x/y」的分母）；个别 provider 返回 -1，按 0 处理
            final int candidateTotal = Math.max(cursor.getCount(), 0);
            pool = java.util.concurrent.Executors.newFixedThreadPool(threads, r -> {
                Thread t = new Thread(r, "anim-scan");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY + 1);
                return t;
            });
            // 按窗口推进：攒一批候选 → 并行判定 → **按原顺序**回收命中。
            // 顺序必须保持（MediaStore 已经按修改时间倒序），否则网格里的顺序会乱跳。
            int window = threads * 8;
            // v1.10.314：进度节流时间戳（120ms 一次，避免几百次 setText 打满主线程）
            long lastEmit = 0L;
            while (cursor.moveToNext()) {
                if (cancelled != null && cancelled.get()) {
                    break;
                }
                long id = cursor.getLong(0);
                long date = cursor.getLong(2);
                int w = wi >= 0 ? cursor.getInt(wi) : 0;
                int h = hi >= 0 ? cursor.getInt(hi) : 0;
                candidates++;
                batch.add(new Item(id, ContentUris.withAppendedId(base, id), date, 0, w, h));
                if (batch.size() >= window) {
                    int[] stat = collectHits(context, batch, out, pool, cancelled, probeCache, freshCache);
                    cachedHits += stat[0];
                    probed += stat[1];
                    batch.clear();
                    long now = android.os.SystemClock.elapsedRealtime();
                    if (progress != null && candidateTotal > 0 && now - lastEmit >= 120L) {
                        lastEmit = now;
                        progress.onProgress(candidates, candidateTotal);
                    }
                    if (out.size() >= MAX_ITEMS) {
                        break;
                    }
                }
            }
            if (!batch.isEmpty() && out.size() < MAX_ITEMS) {
                int[] stat = collectHits(context, batch, out, pool, cancelled, probeCache, freshCache);
                cachedHits += stat[0];
                probed += stat[1];
            }
            if (progress != null && candidateTotal > 0 && (cancelled == null || !cancelled.get())) {
                progress.onProgress(candidates, candidateTotal);
            }
            if (cancelled == null || !cancelled.get()) {
                saveProbeCache(context, probeCache);
            }
        } catch (Throwable ignored) {
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Throwable ignored) {
                }
            }
            if (pool != null) {
                pool.shutdownNow();
            }
            // v1.10.129：把扫描代价量化出来（并行前后可以直接对比这一行）
            Diag.warn(context, "K04", "相册扫描 候选=" + candidates
                    + " 缓存命中=" + cachedHits + " 新探测=" + probed
                    + " 命中动图=" + out.size() + " 线程=" + scanThreads(context)
                    + " 用时=" + (android.os.SystemClock.elapsedRealtime() - started) + "ms"
                    + ((cancelled != null && cancelled.get()) ? "（已取消）" : ""));
        }
        return out;
    }

    /**
     * v1.10.129：把一批候选并行判定，命中结果**按原顺序**追加到 out。
     * 只等这一批（窗口 32 个）—— 好处是「凑够 MAX_ITEMS 就停」的原有语义没变，
     * 也不会一次性把几千个 InputStream 同时打开。
     */
    @org.jetbrains.annotations.NotNull
    private static int[] collectHits(Context context, List<Item> batch, List<Item> out,
                                     java.util.concurrent.ExecutorService pool,
                                     @org.jetbrains.annotations.Nullable java.util.concurrent.atomic.AtomicBoolean cancelled,
                                     @org.jetbrains.annotations.NotNull java.util.HashMap<Long, long[]> probeCache,
                                     @org.jetbrains.annotations.NotNull java.util.LinkedHashMap<Long, long[]> freshCache) {
        int n = batch.size();
        if (n <= 0) {
            return new int[]{0, 0};
        }
        final boolean[] hit = new boolean[n];
        final boolean[] fromCache = new boolean[n];
        List<java.util.concurrent.Future<?>> jobs = new ArrayList<>(n);
        int cached = 0;
        for (int i = 0; i < n; i++) {
            Item item = batch.get(i);
            long[] known = probeCache.get(Long.valueOf(item.id));
            if (known != null && known[0] == item.date) {
                hit[i] = known[1] == 1L;
                fromCache[i] = true;
                cached++;
                continue;
            }
            final int idx = i;
            final Uri uri = item.uri;
            jobs.add(pool.submit(() -> hit[idx] = looksAnimated(context, uri)));
        }
        for (java.util.concurrent.Future<?> job : jobs) {
            try {
                job.get();
            } catch (Throwable ignored) {
            }
        }
        int probed = 0;
        for (int i = 0; i < n; i++) {
            Item item = batch.get(i);
            if (!fromCache[i]) {
                probeCache.put(Long.valueOf(item.id), new long[]{item.date, hit[i] ? 1L : 0L});
                probed++;
            }
            freshCache.put(Long.valueOf(item.id), new long[]{item.date, hit[i] ? 1L : 0L});
            if (out.size() < MAX_ITEMS && hit[i]) {
                out.add(item);
            }
        }
        return new int[]{cached, probed};
    }

    /**
     * 读文件头判断「是不是真的会动」。
     * 先读 256 字节（GIF/WebP 足够），只有 PNG 才继续读到 64KB 找 acTL。
     */
    private static boolean looksAnimated(Context context, Uri uri) {
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
            if (in == null) {
                return false;
            }
            byte[] head = new byte[65536];
            int n = readUpTo(in, head, 0, 256);
            if (n < 12) {
                return false;
            }
            if (head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8') {
                return true;
            }
            boolean isPng = (head[0] & 0xff) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G';
            if (isPng) {
                n = readUpTo(in, head, n, head.length);
                return "apng".equals(MediaSniff.sniffBytes(head, n));
            }
            String sniffed = MediaSniff.sniffBytes(head, n);
            return "webp-anim".equals(sniffed) || "heif-anim".equals(sniffed);
        } catch (Throwable th) {
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static int readUpTo(InputStream in, byte[] buffer, int from, int to) throws java.io.IOException {
        int n = from;
        while (n < to) {
            int r = in.read(buffer, n, to - n);
            if (r < 0) {
                break;
            }
            n += r;
        }
        return n;
    }

    // ------------------------------------------------------------------ 网格

    /**
     * v1.10.176（反馈 2）：封面左下角的「播放时长 · 清晰度」。
     *
     * 三条硬约束：
     *   1. **不超出格子、不截断**：先按可用宽量；放不下就等比缩字号（缩得来才缩），
     *      缩到 0.72 还放不下就**换两行**（时长 / 清晰度），两条路都不加省略号；
     *   2. **跟随「字体」+「字体大小」**：基数 9.5sp，乘 scaledDensity（已含系统字体倍率）
     *      再乘 {@link Fonts#textScale}（应用内档位）；字体本身也走 Fonts.typeface；
     *   3. **整体半透明**：白字 90% + 一层柔和投影保证亮底封面也读得出来。
     *      **不用半透明色块垫底** —— 那是「拿半透明去覆盖」，只会糊。
     */
    private static final class MetaLabel extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float padH;
        private final float padV;
        private final float lineGap;

        @Nullable
        private String source;
        @Nullable
        private String line1;
        @Nullable
        private String line2;
        private int lines;
        private float textSize;
        private float lineHeight;
        private float blockWidth;

        @Nullable
        private String tfKey;
        private float lastSp = -1.0f;
        private float lastScale = -1.0f;

        MetaLabel(Context context) {
            super(context);
            float d = context.getResources().getDisplayMetrics().density;
            this.padH = 4.0f * d;
            this.padV = 2.0f * d;
            this.lineGap = 1.0f * d;
            this.paint.setColor(0xE6FFFFFF);
            this.paint.setShadowLayer(2.4f * d, 0.0f, 1.0f * d, 0x99000000);
        }

        /** 绑定路径：直接换字（不淡入，滚动时不闪）。 */
        void setTextNow(@Nullable String value) {
            boolean changed = value == null ? this.source != null : !value.equals(this.source);
            this.source = value;
            if (changed) {
                requestLayout();
                invalidate();
            }
        }

        /** 异步补到时长之后：淡入一次，提示「刚读到新信息」。 */
        void setTextFade(@Nullable String value) {
            String before = this.source;
            setTextNow(value);
            if (value != null && (before == null || !value.equals(before))) {
                setAlpha(0.0f);
                animate().alpha(1.0f).setDuration(140L).start();
            }
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            int avail = MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED
                    ? getResources().getDisplayMetrics().widthPixels
                    : MeasureSpec.getSize(widthSpec);
            measureText(avail);
            if (this.lines <= 0 || this.line1 == null) {
                setMeasuredDimension(0, 0);
                return;
            }
            int w = (int) Math.ceil(this.blockWidth + this.padH * 2.0f);
            int h = (int) Math.ceil(this.lineHeight * this.lines + this.padV * 2.0f);
            setMeasuredDimension(Math.min(w, avail), h);
        }

        private void measureText(int avail) {
            this.lines = 0;
            this.line1 = null;
            this.line2 = null;
            this.blockWidth = 0.0f;
            this.lineHeight = 0.0f;
            String text = this.source;
            if (text == null || text.isEmpty()) {
                return;
            }
            ensureTypeface();
            float sp = getResources().getDisplayMetrics().scaledDensity;
            float scale = Fonts.textScale(getContext());
            this.lastSp = sp;
            this.lastScale = scale;
            float base = 9.5f * sp * scale;
            float room = Math.max(8.0f, avail - this.padH * 2.0f);

            this.paint.setTextSize(base);
            float need = this.paint.measureText(text);
            if (need <= room) {
                this.textSize = base;
                this.line1 = text;
                this.lines = 1;
            } else {
                float shrink = need > 0.0f ? room / need : 1.0f;
                if (shrink >= 0.72f) {
                    // 稍微缩一点就能单行放下 → 缩，不换行（视觉上更稳）
                    this.textSize = base * shrink;
                    this.line1 = text;
                    this.lines = 1;
                } else {
                    // 缩太多就换两行（都不截断）。v1.10.457：文案变成「1.6s 10fps 240p」三段后，
                    // 按**最后一个空格**切 —— 上面留「时长 + 帧率」，下面单独放清晰度，
                    // 比按第一个空格切（上面只剩时长、下面挤两段）好读得多。
                    int sep = text.lastIndexOf(' ');
                    String a = sep > 0 ? text.substring(0, sep) : text;
                    String b = sep > 0 ? text.substring(sep + 1).replace("\u00B7", "").trim() : null;
                    this.lines = (b == null || b.isEmpty()) ? 1 : 2;
                    this.line1 = a;
                    this.line2 = this.lines == 2 ? b : null;
                    this.textSize = base;
                    float widest = this.paint.measureText(this.line1);
                    if (this.line2 != null) {
                        widest = Math.max(widest, this.paint.measureText(this.line2));
                    }
                    if (widest > room && widest > 0.0f) {
                        this.textSize = base * (room / widest);
                    }
                }
            }
            this.paint.setTextSize(this.textSize);
            Paint.FontMetrics fm = this.paint.getFontMetrics();
            this.lineHeight = fm.descent - fm.ascent + this.lineGap;
            this.blockWidth = this.paint.measureText(this.line1);
            if (this.line2 != null) {
                this.blockWidth = Math.max(this.blockWidth, this.paint.measureText(this.line2));
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (this.lines <= 0 || this.line1 == null) {
                return;
            }
            float sp = getResources().getDisplayMetrics().scaledDensity;
            float scale = Fonts.textScale(getContext());
            if (Math.abs(sp - this.lastSp) > 0.01f || Math.abs(scale - this.lastScale) > 0.0005f) {
                // 系统字体或档位变了：尺寸要重新量（invalidate 不会触发 measure）
                requestLayout();
                return;
            }
            ensureTypeface();
            this.paint.setTextSize(this.textSize);
            Paint.FontMetrics fm = this.paint.getFontMetrics();
            float baseline = this.padV - fm.ascent;
            canvas.drawText(this.line1, this.padH, baseline, this.paint);
            if (this.line2 != null) {
                canvas.drawText(this.line2, this.padH, baseline + this.lineHeight, this.paint);
            }
        }

        /** 自绘文字同样吃「字体」设置（跟随系统 → null = 系统字形）。 */
        private void ensureTypeface() {
            String id = Fonts.activeId(getContext());
            if (id != null && id.equals(this.tfKey)) {
                return;
            }
            this.tfKey = id;
            this.paint.setTypeface(Fonts.typeface(getContext(), false));
        }
    }

    private static final class ThumbAdapter extends BaseAdapter {
        private final Context context;
        private final List<Item> data;
        private final HashMap<Long, Bitmap> cache = new HashMap<>();
        private final ExecutorService pool = Executors.newFixedThreadPool(3);
        /** v1.10.176：时长解析单独一条低优先级线程（不跟缩略图抢 3 条通道）。 */
        private final ExecutorService metaPool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "anim-meta");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
        private final int cell;
        private final int placeholder;
        private final int labelMargin;
        /** v1.10.314：格子圆角半径 —— 跟「界面语言」卡片（14dp）同一族，格子小一号取 12dp */
        private final int radius;
        /** v1.10.314：点格子 → 直接选中（格子自己接点击，涟漪才有 pressed 状态可动） */
        @Nullable
        private final Pick onPick;

        ThumbAdapter(Context context, List<Item> data, GridMetrics metrics, @Nullable Pick onPick) {
            this.context = context;
            this.data = data;
            this.cell = metrics.cell;
            this.onPick = onPick;
            float density = context.getResources().getDisplayMetrics().density;
            this.labelMargin = Math.max(1, Math.round(5 * density));
            this.radius = Math.max(1, Math.round(12 * density));
            int color = 0x33000000;
            try {
                color = ThemeManager.INSTANCE.colors(context).getNeutral();
            } catch (Throwable ignored) {
            }
            this.placeholder = color;
        }

        /** v1.10.314：圆角占位底；配 {@code setClipToOutline(true)} 把缩略图裁进圆角里。 */
        @NotNull
        private GradientDrawable rounded(int color) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(color);
            bg.setCornerRadius(this.radius);
            return bg;
        }

        /**
         * v1.10.314：按压涟漪（主色 16% + 同半径遮罩）。
         *
         * 原来格子点了没有任何反馈，一屏 16 个方块只能靠"点了页面就关"来确认。
         * 涟漪放在 foreground：盖在缩略图之上，且被圆角遮罩裁掉，不会溢出格子。
         */
        @NotNull
        private Drawable ripple() {
            int color = 0x29000000;
            try {
                ThemeManager manager = ThemeManager.INSTANCE;
                color = manager.withAlpha(manager.colors(this.context).getPrimary(), 0.16f);
            } catch (Throwable ignored) {
            }
            return new RippleDrawable(ColorStateList.valueOf(color), null, rounded(0xFFFFFFFF));
        }

        @Override
        public int getCount() {
            return this.data.size();
        }

        @Override
        public Object getItem(int position) {
            return this.data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return this.data.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            FrameLayout cellView;
            ImageView view;
            MetaLabel label;
            if (convertView instanceof FrameLayout && ((FrameLayout) convertView).getChildCount() >= 2) {
                cellView = (FrameLayout) convertView;
                view = (ImageView) cellView.getChildAt(0);
                label = (MetaLabel) cellView.getChildAt(1);
            } else {
                cellView = new FrameLayout(this.context);
                cellView.setLayoutParams(new AbsListView.LayoutParams(this.cell, this.cell));
                view = new ImageView(this.context);
                view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                view.setBackground(rounded(this.placeholder));
                view.setClipToOutline(true);
                cellView.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                label = new MetaLabel(this.context);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.gravity = Gravity.BOTTOM | Gravity.START;
                lp.leftMargin = this.labelMargin;
                lp.rightMargin = this.labelMargin;
                lp.bottomMargin = this.labelMargin;
                cellView.addView(label, lp);
                cellView.setForeground(ripple());
                // v1.10.314：格子做成可点击的 —— ListView/GridView 的子项如果不接点击，
                // 就拿不到 pressed 状态，foreground 的涟漪永远不会动（配了等于没配）。
                cellView.setClickable(true);
            }
            Item item = this.data.get(position);
            cellView.setTag(Long.valueOf(item.id));
            // 每次绑定都换监听器：捕获的是**本次**的 item，不是复用时那个旧 item
            final Item bound = item;
            cellView.setOnClickListener(v -> {
                if (this.onPick != null) {
                    this.onPick.onPick(bound);
                }
            });
            Bitmap cached;
            synchronized (this.cache) {
                cached = this.cache.get(Long.valueOf(item.id));
            }
            if (cached != null && !cached.isRecycled()) {
                view.setImageBitmap(cached);
            } else {
                view.setImageDrawable(null);
                load(cellView, view, item);
            }
            // 已知时长就直接上；不知道就先把「清晰度」摆上（它来自游标，零 I/O）
            label.setTextNow(labelText(item));
            requestMeta(cellView, item);
            return cellView;
        }

        /**
         * 文案（v1.10.458 用户指定）：`1.6s 10fps 240p` —— 三个数字 + 单位，中间一个空格。
         * 缺一项只显示其余项；都没有就不显示。
         */
        @Nullable
        private static String labelText(Item item) {
            int w = item.width > 0 ? item.width : item.probedWidth;
            int h = item.height > 0 ? item.height : item.probedHeight;
            int ms = item.durationMs > 0 ? item.durationMs : 0;
            return AnimDuration.label(ms, item.fpsX10, w, h);
        }

        /**
         * v1.10.176：时长按需解析。
         *
         * 只对**真正上过屏**的格子解析（约一屏 16 个），单线程串行；同一个 id 只解析一次；
         * 解析完直接落内存/磁盘缓存，下次进页面 0 I/O。解析本身不解一帧像素（走容器块结构）。
         */
        private void requestMeta(final FrameLayout cellView, final Item item) {
            if (item.durationTried || item.durationMs >= 0) {
                return;
            }
            item.durationTried = true;
            this.metaPool.execute(() -> {
                final int[] found = AnimDuration.probe(this.context, item.uri, item.id, item.date);
                if (found != null) {
                    item.durationMs = found[0];
                    item.durationEstimated = found[1] != 0;
                    item.probedWidth = found[2];
                    item.probedHeight = found[3];
                    // v1.10.457：帧率 = 帧数 / 时长，**探测时顺手算**（只读容器块结构，
                    // 原来那次 I/O 一次都不多），不另外再打开文件。
                    item.fpsX10 = AnimDuration.fpsX10Of(found);
                } else {
                    item.durationMs = 0;
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    Object tag = cellView.getTag();
                    if (!(tag instanceof Long) || ((Long) tag).longValue() != item.id) {
                        return;
                    }
                    if (cellView.getChildCount() < 2) {
                        return;
                    }
                    View overlay = cellView.getChildAt(1);
                    if (overlay instanceof MetaLabel) {
                        ((MetaLabel) overlay).setTextFade(labelText(item));
                    }
                });
            });
        }

        private void load(final FrameLayout cellView, final ImageView view, final Item item) {
            cellView.setTag(Long.valueOf(item.id));
            this.pool.execute(() -> {
                final Bitmap bitmap = thumbnail(this.context, item);
                if (bitmap == null) {
                    return;
                }
                synchronized (this.cache) {
                    this.cache.put(Long.valueOf(item.id), bitmap);
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    Object tag = cellView.getTag();
                    if (tag instanceof Long && ((Long) tag).longValue() == item.id) {
                        view.setImageBitmap(bitmap);
                    }
                });
            });
        }

        private static Bitmap thumbnail(Context context, Item item) {
            try {
                if (Build.VERSION.SDK_INT >= 29) {
                    return context.getContentResolver().loadThumbnail(item.uri, new Size(360, 360), null);
                }
                return MediaStore.Images.Thumbnails.getThumbnail(context.getContentResolver(), item.id,
                        MediaStore.Images.Thumbnails.MINI_KIND, null);
            } catch (Throwable th) {
                return null;
            }
        }
    }
}
