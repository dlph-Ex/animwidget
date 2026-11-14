package io.github.animwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * v1.10.99（需求 2 / 2.1）：**自绘的「折叠三角」与「勾」**。
 *
 * 为什么不用字符：`▾` `✓` 这些字形是字体里的方块字，
 *   · 基线不在视觉中心 → 40dp 的点击框里永远是偏的（截图里能直接看出来）；
 *   · 大小跟着字号走、笔画粗细不受控；
 *   · 换了自定义字体后形状还会变（甚至变豆腐）。
 * 所以这里全部用 Path 画：几何中心对齐、线宽固定 dp、圆头圆角，
 * 旋转仍然交给 View 自己的 rotation 属性（180° = 朝上），代码不用改。
 */
public final class GlyphView extends View {

    public static final int CHEVRON = 0;
    public static final int CHECK = 1;
    /** v1.10.140：确认弹窗的「感叹号」（自绘，不用字符）。 */
    public static final int ALERT = 2;
    /**
     * v1.10.182：赞赏页头部的**咖啡杯**（自绘）。
     *
     * 原来那一格是 `TextView` 打 ☕ —— emoji 由系统字体决定：vivo 上是暗橄榄色的一团，
     * 深浅色下都不跟主题、还带彩色阴影，看起来像「没渲染完」。这里改成 Path 画的杯子，
     * 颜色跟着 primary 走，任何 ROM 都一样。
     */
    public static final int CUP = 3;
    /**
     * v1.10.205：语义色编辑面板的「重置」（回到默认值）。
     *
     * 画的是「逆时针开口的圆环 + 箭头」：按钮只有 24dp 宽（= 取消按钮的半径），
     * 放不下两个字，而字形字符（↺）在不同 ROM 上大小/基线不同 —— 所以用 Path 画。
     *
     * <p>v1.10.468（用户：「头部箭头怎么老是不在身子中央」）：205 那版把**尖端**放在弧端点上、
     * 三角形向后铺开，于是箭头是「压在环上的一块楔子」，而且中心偏；另外只按圆心居中，
     * 箭头凸出去那一截会把视觉重心带偏。现在：底边中点落在弧端点上、尖端沿切线伸出去，
     * 最后按**整组图形（含描边）的外接框**居中。</p>
     */
    public static final int RESET = 4;
    /** v1.10.386：**链接符号**（「关于」页开源仓库那一行的前缀）。两个交叠圆环，斜 45°。 */
    public static final int LINK = 5;
    /**
     * v1.10.387：**检测更新**（环形箭头）。上一版借用了 {@link #RESET}（重置用的逆时针环），
     * 18dp 里那圈缺口 + 小尾巴被读成「一个 C」——用户原话「怎么看都不像」。
     * 这里改成标准的「圆圈 + 实心箭头」：弧 290°、箭头按切线方向实心填，
     * 直径占满 13dp，缩到 9dp 也认得出是「刷新/更新」。
     */
    public static final int REFRESH = 6;
    /**
     * v1.10.388：「系统预设」小标题前的**三色块符号**（三个小圆点，代表一组预设）。
     * 用它而不是再写一个四字标题：主标题「配色方案」也是四个字，两个同字数标题会互相抢。
     */
    public static final int SWATCHES = 7;

    private final Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    /** v1.10.468：箭头的独立路径（弧要描边、箭头要填充，两条路径共用一次平移）。 */
    private final Path tipPath = new Path();

    private int glyph = CHEVRON;
    /** 图形本体的宽度（dp），与控件框无关：框多大都居中。 */
    private float glyphDp = 9.0f;
    /** v1.10.140：非 0 时用它上色（确认弹窗的图标砖要 main/danger 色），0 = 原来的默认色。 */
    private int tint = 0;

    public GlyphView(Context context) {
        super(context);
    }

    public GlyphView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GlyphView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setGlyph(int value) {
        this.glyph = value;
        this.glyphDp = value == CHECK ? 12.5f
                : (value == ALERT ? 14.0f
                : (value == CUP ? 26.0f
                : (value == RESET ? 15.0f
                : (value == LINK ? 11.0f : (value == REFRESH ? 12.5f : 9.0f)))));
        invalidate();
    }

    /** v1.10.140：指定图形颜色（0 = 回落到默认规则）。 */
    public void setTintColor(int color) {
        this.tint = color;
        invalidate();
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * alpha);
        if (a <= 0) {
            a = 1;
        }
        return (a << 24) | (color & 0x00FFFFFF);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0.0f || h <= 0.0f) {
            return;
        }
        float dp = getResources().getDisplayMetrics().density;
        ThemeColors c = ThemeManager.INSTANCE.colors(getContext());
        float g = this.glyphDp * dp;
        float cx = w / 2.0f;
        float cy = h / 2.0f;

        this.pen.setStyle(Paint.Style.STROKE);
        this.pen.setStrokeCap(Paint.Cap.ROUND);
        this.pen.setStrokeJoin(Paint.Join.ROUND);
        this.pen.setStrokeWidth((this.glyph == CHECK ? 1.9f
                : (this.glyph == CUP ? 1.70f
                : (this.glyph == REFRESH ? 1.75f
                : (this.glyph == RESET ? 1.90f : 1.45f)))) * dp);
        this.pen.setColor(this.tint != 0 ? this.tint
                : (this.glyph == CHECK ? c.getPrimary() : withAlpha(c.getText(), 0.62f)));

        this.path.reset();
        if (this.glyph == CHEVRON) {
            float half = g / 2.0f;
            float drop = g * 0.30f;
            this.path.moveTo(cx - half, cy - drop);
            this.path.lineTo(cx, cy + drop);
            this.path.lineTo(cx + half, cy - drop);
            canvas.drawPath(this.path, this.pen);
            return;
        }
        if (this.glyph == REFRESH) {
            float rr = g * 0.40f;
            float start = -60.0f;
            float sweep = 285.0f;
            android.graphics.RectF circle = new android.graphics.RectF(cx - rr, cy - rr, cx + rr, cy + rr);
            canvas.drawArc(circle, start, sweep, false, this.pen);
            // 箭头：在弧的终点，沿切线方向实心三箭头
            double end = Math.toRadians(start + sweep);
            float px = cx + rr * (float) Math.cos(end);
            float py = cy + rr * (float) Math.sin(end);
            float tx = (float) -Math.sin(end);
            float ty = (float) Math.cos(end);
            float nx = (float) Math.cos(end);
            float ny = (float) Math.sin(end);
            float L = g * 0.30f;   // 箭头长度
            float W = g * 0.115f;  // 半宽
            this.path.reset();
            this.path.moveTo(px + tx * L, py + ty * L);
            this.path.lineTo(px - nx * W, py - ny * W);
            this.path.lineTo(px + nx * W, py + ny * W);
            this.path.close();
            int keepStyle = this.pen.getStyle() == Paint.Style.FILL ? 0 : 1;
            this.pen.setStyle(Paint.Style.FILL);
            canvas.drawPath(this.path, this.pen);
            this.pen.setStyle(Paint.Style.STROKE);
            if (keepStyle == 0) {
                this.pen.setStyle(Paint.Style.FILL);
            }
            return;
        }
        if (this.glyph == SWATCHES) {
            float r = g * 0.16f;
            float step = g * 0.34f;
            this.pen.setStyle(Paint.Style.FILL);
            for (int i = -1; i <= 1; i++) {
                this.pen.setAlpha(i == 0 ? 255 : 150);
                canvas.drawCircle(cx + step * i, cy, r, this.pen);
            }
            this.pen.setAlpha(255);
            this.pen.setStyle(Paint.Style.STROKE);
            return;
        }
        if (this.glyph == LINK) {
            // 两个交叠的圆环，整体斜 45° —— 11dp 里也读得出「链条」
            float r = g * 0.30f;
            float dx = g * 0.26f;
            canvas.save();
            canvas.rotate(-45.0f, cx, cy);
            canvas.drawCircle(cx - dx, cy, r, this.pen);
            canvas.drawCircle(cx + dx, cy, r, this.pen);
            canvas.restore();
            return;
        }
        if (this.glyph == CHECK) {
            float half = g / 2.0f;
            float rise = g * 0.36f;
            this.path.moveTo(cx - half, cy + rise * 0.10f);
            this.path.lineTo(cx - half * 0.22f, cy + rise);
            this.path.lineTo(cx + half, cy - rise);
            canvas.drawPath(this.path, this.pen);
            return;
        }
        if (this.glyph == CUP) {
            // v1.10.182：平顶杯身（微收腰 + 圆底）+ 单段二次曲线把手 + 杯垫 + 两道热气。
            // 几何全部按 g 的比例算，并且整体左移 0.05g —— 把手在右侧要占地方，
            // 不移的话视觉重心会偏右（截图里一眼就能看出来）。
            float rim = g * 0.28f;
            float bodyH = g * 0.34f;
            float x0 = cx - g * 0.05f;
            float yRim = cy - g * 0.11f;
            float yBottom = yRim + bodyH;
            this.path.reset();
            this.path.moveTo(x0 - rim, yRim);
            this.path.lineTo(x0 - rim * 0.80f, yBottom);
            this.path.quadTo(x0 - rim * 0.76f, yBottom + g * 0.07f,
                    x0 - rim * 0.46f, yBottom + g * 0.07f);
            this.path.lineTo(x0 + rim * 0.46f, yBottom + g * 0.07f);
            this.path.quadTo(x0 + rim * 0.76f, yBottom + g * 0.07f,
                    x0 + rim * 0.80f, yBottom);
            this.path.lineTo(x0 + rim, yRim);
            this.path.lineTo(x0 - rim, yRim);
            canvas.drawPath(this.path, this.pen);
            // 把手
            this.path.reset();
            float hy1 = yRim + bodyH * 0.16f;
            float hy2 = yRim + bodyH * 0.86f;
            this.path.moveTo(x0 + rim * 0.90f, hy1);
            this.path.quadTo(x0 + rim + g * 0.34f, (hy1 + hy2) / 2.0f,
                    x0 + rim * 0.90f, hy2);
            canvas.drawPath(this.path, this.pen);
            // 杯垫
            this.path.reset();
            this.path.moveTo(x0 - rim * 1.22f, yBottom + g * 0.22f);
            this.path.lineTo(x0 + rim * 1.30f, yBottom + g * 0.22f);
            canvas.drawPath(this.path, this.pen);
            // 热气（两道反向小弧）
            this.path.reset();
            this.path.moveTo(x0 - rim * 0.40f, yRim - g * 0.12f);
            this.path.quadTo(x0 - rim * 0.40f - g * 0.09f, yRim - g * 0.22f,
                    x0 - rim * 0.40f, yRim - g * 0.34f);
            canvas.drawPath(this.path, this.pen);
            this.path.reset();
            this.path.moveTo(x0 + rim * 0.30f, yRim - g * 0.12f);
            this.path.quadTo(x0 + rim * 0.30f + g * 0.09f, yRim - g * 0.22f,
                    x0 + rim * 0.30f, yRim - g * 0.34f);
            canvas.drawPath(this.path, this.pen);
            return;
        }
        if (this.glyph == RESET) {
            // v1.10.468：**逆时针圆环 + 贴切线伸出的箭头**（几何全在「圆心 = 原点」的局部坐标里算，
            // 最后统一平移，所以不管控件框多大都精确居中）。
            float r = g * 0.42f;
            float start = 180.0f;
            float sweep = -290.0f;              // 逆时针 290°，缺口在左上
            this.path.reset();
            this.path.addArc(-r, -r, r, r, start, sweep);
            // 弧的终点（= 缺口的一端），箭头就长在这里
            double a = Math.toRadians(start + sweep);
            float px = r * (float) Math.cos(a);
            float py = r * (float) Math.sin(a);
            // 逆时针运动方向（角度减小的切线）：(sinθ, -cosθ)
            float tx = (float) Math.sin(a);
            float ty = (float) -Math.cos(a);
            float len = g * 0.30f;              // 箭头长度（尖端沿切线伸出去，不是压在环上）
            float half = g * 0.16f;             // 箭头半宽
            this.tipPath.reset();
            this.tipPath.moveTo(px + tx * len, py + ty * len);   // 尖端
            this.tipPath.lineTo(px - ty * half, py + tx * half); // 底边中点 = 弧端点
            this.tipPath.lineTo(px + ty * half, py - tx * half);
            this.tipPath.close();
            // 光学居中：把「弧 + 描边一半 + 箭头」的外接框中心挪到控件中心。
            // 只按圆心居中时，箭头凸出去的那一截会让整个图标看着偏上（用户指出的那个偏差）。
            float sw = this.pen.getStrokeWidth();
            android.graphics.RectF box = new android.graphics.RectF();
            this.path.computeBounds(box, true);
            box.inset(-sw / 2.0f, -sw / 2.0f);
            android.graphics.RectF box2 = new android.graphics.RectF();
            this.tipPath.computeBounds(box2, true);
            box.union(box2.left, box2.top, box2.right, box2.bottom);
            int save = canvas.save();
            canvas.translate(cx - box.centerX(), cy - box.centerY());
            canvas.drawPath(this.path, this.pen);
            this.pen.setStyle(Paint.Style.FILL);       // 箭头实心
            canvas.drawPath(this.tipPath, this.pen);
            this.pen.setStyle(Paint.Style.STROKE);
            canvas.restoreToCount(save);
            return;
        }
        // v1.10.140 ALERT：竖杠 + 圆点（几何全按 dp 算，任何字号下都居中）
        this.pen.setStrokeWidth(2.05f * dp);
        float top = cy - g * 0.46f;
        float bottom = cy + g * 0.08f;
        this.path.moveTo(cx, top);
        this.path.lineTo(cx, bottom);
        canvas.drawPath(this.path, this.pen);
        this.pen.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy + g * 0.40f, 1.15f * dp, this.pen);
    }
}
