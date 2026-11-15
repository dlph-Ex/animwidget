package io.github.animwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v1.10.240：设置页卡片头的**线性图标**（界面语言 / 系统模式 / 性能）。
 *
 * <h3>为什么不再用字符（☾ ⚡ ⇄ 🎨）</h3>
 * 原来三个头像是 {@code TextView} 里塞 emoji / 几何字符：字形来自系统字体，机器一换字体就
 * 粗细不一、基线不一、深色下还会被系统染成彩色 emoji（emoji 不吃 textColor），
 * 于是「同一列图标看起来不是一个家族」。改成矢量路径后：线宽、端点、网格全由代码定，
 * 深浅色、对比度、强调色都走同一条 {@code applyColors}。
 *
 * <h3>绘制规格（见 docs/SETTINGS-ICONS.md，来自 30 份参考的取舍）</h3>
 * <ul>
 *   <li><b>24 单位网格</b>：实际内容 = 24dp，居中画在 40dp 磁贴里（每边留 8dp，
 *       和 Material 3「24dp icon in 48dp container」同一比例，按 40 收窄）。</li>
 *   <li><b>线宽 1.9dp</b>、<b>圆头圆角</b>（{@link Paint.Cap#ROUND} / {@link Paint.Join#ROUND}）：
 *       与 AOSP 设置页、SF Symbols Regular 的观感对齐；深色下 1.9 略粗一点，抵消暗底「线变细」。</li>
 *   <li><b>单色</b>：只取当前主题的一个强调色（tag 决定 primary / secondary / tertiary），
 *       磁贴底 = 该色 16% 叠在表面上 —— 和原来的配色家族完全一致，只是把字形换成路径。</li>
 *   <li><b>方向中性</b>：三个图标的几何都左右对称，RTL 下不需要镜像（布局边距走 Start/End）。</li>
 * </ul>
 *
 * <h3>tag 语法</h3>
 * {@code icon_tile|lang} / {@code icon_tile|mode} / {@code icon_tile_2|perf} ——
 * 竖线前是磁贴色（沿用 {@code ThemeManager} 已有的三档），竖线后是图形。
 * 用 tag 而不是自定义属性：aapt2 走 {@code --stable-ids}，新增 declare-styleable 会动资源表。
 */
public final class IconTileView extends View {

    /** 界面语言：地球（圆 + 经线椭圆 + 赤道）。 */
    public static final int ICON_LANGUAGE = 0;
    /** 系统模式：明暗各半的圆（= 跟随系统 / 自动）。 */
    public static final int ICON_MODE = 1;
    /** 性能：仪表盘（上弧 + 指针 + 三刻度）。 */
    public static final int ICON_PERF = 2;
    /**
     * v1.10.522：**配色方案** —— 调色板。
     *
     * <p>上一版是 {@code TextView} 里的 🎨：emoji 由系统字体决定，深色下会被染成彩色、
     * 尺寸与基线不受控，放在「界面语言 / 系统模式 / 性能」这一列里明显不是一个家族。
     * 现在改为矢量路径：板身取参考 #1（Material Symbols「palette」outline）的
     * 「椭圆板 + 拇指孔」骨架，颜料点换成自绘的三点布局（见 {@link #drawPalette}），
     * 线宽/端点/网格与另外三个图标完全同一套。</p>
     */
    public static final int ICON_PALETTE = 3;

    /** 设计网格边长（单位）。 */
    private static final float GRID = 24.0f;
    /** 线宽（dp）。 */
    private static final float STROKE_DP = 1.9f;
    /** 内容每边留白（dp）：40dp 磁贴 → 24dp 内容。 */
    private static final float INSET_DP = 8.0f;

    private final float density;
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint solid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Path path = new Path();
    /** v1.10.522：调色板拇指孔的临时路径（与 path 做差集，得到"咬掉一口"的板身）。 */
    private final Path thumb = new Path();

    private int icon = ICON_LANGUAGE;
    private int tint = 0xFF888888;

    public IconTileView(@NotNull Context context) {
        this(context, null);
    }

    public IconTileView(@NotNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconTileView(@NotNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.density = getResources().getDisplayMetrics().density;
        this.line.setStyle(Paint.Style.STROKE);
        this.line.setStrokeCap(Paint.Cap.ROUND);
        this.line.setStrokeJoin(Paint.Join.ROUND);
        this.line.setStrokeWidth(STROKE_DP * this.density);
        this.line.setColor(this.tint);
        this.solid.setStyle(Paint.Style.FILL);
        this.solid.setColor(this.tint);
    }

    /** 由 {@link ThemeManager#paint} 统一下发（换肤 / 对比度 / 强调色变化都走这里）。 */
    public void applyColors(@NotNull ThemeColors c) {
        String tag = tagOf();
        int accent = c.getPrimary();
        if (tag.startsWith("icon_tile_2")) {
            accent = c.getSecondary();
        } else if (tag.startsWith("icon_tile_3")) {
            accent = c.getTertiary();
        }
        this.tint = accent;
        this.line.setColor(accent);
        this.solid.setColor(accent);
        setBackground(ThemeManager.INSTANCE.rounded(getContext(),
                ThemeManager.INSTANCE.withAlpha(accent, 0.16f), 14.0f));
        invalidate();
    }

    @NotNull
    private String tagOf() {
        Object tag = getTag();
        return tag instanceof String ? (String) tag : "";
    }

    /** tag 里的图形名；没写就按「界面语言」画（不会崩，也不会空白）。 */
    private void resolveTag() {
        String tag = tagOf();
        int bar = tag.indexOf('|');
        String kind = bar >= 0 ? tag.substring(bar + 1) : "";
        if ("mode".contentEquals(kind)) {
            this.icon = ICON_MODE;
        } else if ("perf".contentEquals(kind)) {
            this.icon = ICON_PERF;
        } else if ("palette".contentEquals(kind)) {
            this.icon = ICON_PALETTE;
        } else {
            this.icon = ICON_LANGUAGE;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        resolveTag();
        invalidate();
    }

    @Override
    protected void onDraw(@NotNull Canvas canvas) {
        resolveTag();
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        float side = Math.min(w, h);
        // 内容边长（px）：40dp 磁贴 → 24dp；小到 24dp 时不再缩小，保证线宽可辨
        float inner = Math.max(side - INSET_DP * 2.0f * this.density, side * 0.62f);
        float s = inner / GRID;
        canvas.save();
        canvas.translate((w - inner) / 2.0f, (h - inner) / 2.0f);
        canvas.scale(s, s);
        // 画布已经缩放过，线宽要除回去才是恒定的 1.9dp
        this.line.setStrokeWidth(STROKE_DP * this.density / s);
        if (this.icon == ICON_MODE) {
            drawMode(canvas);
        } else if (this.icon == ICON_PERF) {
            drawGauge(canvas);
        } else if (this.icon == ICON_PALETTE) {
            drawPalette(canvas);
        } else {
            drawGlobe(canvas);
        }
        canvas.restore();
    }

    /** 地球：圆 + 经线椭圆 + 赤道。三点信息即可读成「语言 / 区域」，不做纬线（24 网格里会糊）。 */
    private void drawGlobe(@NotNull Canvas canvas) {
        float cx = 12.0f;
        float cy = 12.0f;
        float r = 8.4f;
        canvas.drawCircle(cx, cy, r, this.line);
        this.oval.set(cx - 3.9f, cy - r, cx + 3.9f, cy + r);
        canvas.drawOval(this.oval, this.line);
        canvas.drawLine(cx - r + 0.15f, cy, cx + r - 0.15f, cy, this.line);
    }

    /** 明暗各半：右半实心 + 全轮廓。这个字形在系统设置里专指「自动 / 跟随」三种状态的那一个。 */
    private void drawMode(@NotNull Canvas canvas) {
        float cx = 12.0f;
        float cy = 12.0f;
        float r = 8.4f;
        this.path.reset();
        this.oval.set(cx - r, cy - r, cx + r, cy + r);
        this.path.moveTo(cx, cy - r);
        // -90° 起、顺时针 180° = 右半圆；arcTo 后 close 会把弦补上
        this.path.arcTo(this.oval, -90.0f, 180.0f, false);
        this.path.close();
        canvas.drawPath(this.path, this.solid);
        canvas.drawCircle(cx, cy, r, this.line);
    }

    /**
     * v1.10.522：调色板 —— **参考骨架 + 自绘元素**的混合。
     *
     * <ul>
     *   <li><b>骨架（取自参考 #1，Material Symbols「palette」outline）</b>：
     *       椭圆板身 + 右下一处「拇指孔」的缺口，用 {@link Path#op} 做差集切出来；</li>
     *   <li><b>自绘部分</b>：三个实心颜料点，位置按「左上 → 上 → 左下」的不等边三角形摆
     *       （参考图是四点在正中排成一排），这样 24dp 里既不糊成一团，也不像一串省略号；</li>
     *   <li>线宽与端点沿用类里那一套（1.9dp / ROUND），和地球、明暗、仪表同一个家族。</li>
     * </ul>
     */
    private void drawPalette(@NotNull Canvas canvas) {
        this.oval.set(12.0f - 8.7f, 12.2f - 7.5f, 12.0f + 8.7f, 12.2f + 7.5f);
        this.path.reset();
        this.path.addOval(this.oval, Path.Direction.CW);
        this.thumb.reset();
        this.thumb.addCircle(17.2f, 16.1f, 2.25f, Path.Direction.CW);
        // 差集：整块椭圆减掉拇指孔 = 板身（缺口落在右下边缘，正是握板的位置）
        this.path.op(this.thumb, Path.Op.DIFFERENCE);
        canvas.drawPath(this.path, this.line);
        // 三个颜料点（自绘）：实心圆，半径 1.5（网格单位）= 24dp 内容里约 1.5dp
        canvas.drawCircle(8.5f, 9.9f, 1.5f, this.solid);
        canvas.drawCircle(13.3f, 8.4f, 1.5f, this.solid);
        canvas.drawCircle(8.9f, 14.8f, 1.5f, this.solid);
    }

    /** 仪表：下开口的上弧 + 指向右上 45° 的指针 + 三刻度 + 中心点。 */
    private void drawGauge(@NotNull Canvas canvas) {
        float cx = 12.0f;
        float cy = 14.0f;
        float r = 8.4f;
        this.oval.set(cx - r, cy - r, cx + r, cy + r);
        this.path.reset();
        this.path.arcTo(this.oval, 200.0f, 140.0f, false);
        canvas.drawPath(this.path, this.line);
        canvas.drawLine(cx, cy, cx + r * 0.62f, cy - r * 0.62f, this.line);
        for (int i = 0; i < 3; i++) {
            double a = Math.toRadians(200.0d + 70.0d * i);
            float cos = (float) Math.cos(a);
            float sin = (float) Math.sin(a);
            canvas.drawLine(cx + cos * (r - 2.7f), cy + sin * (r - 2.7f),
                    cx + cos * (r - 1.25f), cy + sin * (r - 1.25f), this.line);
        }
        canvas.drawCircle(cx, cy, 1.35f, this.solid);
    }
}
