package io.github.animwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * v1.10.202：**自绘的开关框**（26dp 圆角方框 + 勾）。
 *
 * 为什么重写：v1.10.202 之前性能卡里的开关是「FrameLayout + GradientDrawable 背景 + GlyphView」，
 * 颜色在建视图那一刻**烘焙进 drawable**。而 `ThemeManager.paint()` 只认 tag（card / pill_soft /
 * divider / Text / Button…），碰不到一个裸的 FrameLayout + GradientDrawable ——
 * 于是「切深浅色 → 页面其它地方都换了、只有这几个框还留着旧配色的描边」，
 * 旧配色在深色底上接近不可见，用户看到的就是「选项框没了，点一下才回来」
 * （点一下会重新 applySwitchLook，才把新配色画上去）。
 *
 * 现在：颜色**不烘焙**，全部在 `onDraw` 里按当前 ThemeColors 现算；
 * 同时接进 `ThemeManager.paint()`（和 FontScaleSlider / LanguageRowView 同一条链），
 * 任何换肤入口（冷启动 applyTo / 切预设 / 改对比度 / 500ms 轮询 / 叠化里的原地刷新）
 * 都会把这一格重新上色，不需要谁记得「重建一次」。
 */
public final class SwitchBoxView extends View {

    private final Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    private boolean on;
    /** 最近一次画上去的配色（只做痕迹，不作为绘制依据 —— 依据永远是 ThemeManager 的当前值）。 */
    private ThemeColors colors;

    public SwitchBoxView(Context context) {
        super(context);
    }

    public SwitchBoxView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwitchBoxView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * 换肤链路入口（ThemeManager.paint → 这里 → invalidate）。
     *
     * 这里**不缓存色值**：绘制时永远向 ThemeManager 要当前的。少一个「存了旧色没人更新」的位置。
     */
    public void applyColors(ThemeColors c) {
        this.colors = c;
        invalidate();
    }

    public boolean isOn() {
        return this.on;
    }

    public void setOn(boolean value) {
        if (this.on == value) {
            return;
        }
        this.on = value;
        invalidate();
    }

    public void toggle() {
        setOn(!this.on);
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
        // 每次都取当前配色（不做「构造时取一次」的缓存）—— 这一条就是本次修复的核心：
        // 开关框的颜色不再烘焙在任何 drawable / 字段里，换肤后任何一次重绘都是对的。
        ThemeColors c = ThemeManager.INSTANCE.colors(getContext());
        this.colors = c;
        float radius = 8.5f * dp;
        // ① 框：开 = 主色淡底 + 主色描边；关 = 透明底 + 极淡正文色描边
        this.pen.setStyle(Paint.Style.FILL);
        if (this.on) {
            this.pen.setColor(withAlpha(c.getPrimary(), 0.18f));
        } else {
            this.pen.setColor(0x00000000);
        }
        canvas.drawRoundRect(0.0f, 0.0f, w, h, radius, radius, this.pen);

        this.pen.setStyle(Paint.Style.STROKE);
        this.pen.setStrokeCap(Paint.Cap.ROUND);
        this.pen.setStrokeJoin(Paint.Join.ROUND);
        if (this.on) {
            this.pen.setStrokeWidth(Math.max(1.0f, 1.6f * dp));
            this.pen.setColor(c.getPrimary());
        } else {
            this.pen.setStrokeWidth(Math.max(1.0f, 1.3f * dp));
            this.pen.setColor(withAlpha(c.getText(), 0.26f));
        }
        // 描边画在边界内（stroke 有一半在外侧，不内缩就会被父容器的圆角/裁剪啃掉一点）
        float inset = this.pen.getStrokeWidth() / 2.0f;
        canvas.drawRoundRect(inset, inset, w - inset, h - inset, radius, radius, this.pen);

        if (!this.on) {
            return;
        }
        // ② 勾：几何与 GlyphView.CHECK 完全一致（12.5dp / 1.9dp / 圆头），视觉不变
        float g = 12.5f * dp;
        float half = g / 2.0f;
        float rise = g * 0.36f;
        float cx = w / 2.0f;
        float cy = h / 2.0f;
        this.pen.setStyle(Paint.Style.STROKE);
        this.pen.setStrokeWidth(1.9f * dp);
        this.pen.setColor(c.getPrimary());
        this.path.reset();
        this.path.moveTo(cx - half, cy + rise * 0.10f);
        this.path.lineTo(cx - half * 0.22f, cy + rise);
        this.path.lineTo(cx + half, cy - rise);
        canvas.drawPath(this.path, this.pen);
    }
}
