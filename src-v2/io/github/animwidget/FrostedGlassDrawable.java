package io.github.animwidget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.jetbrains.annotations.Nullable;

/**
 * v1.10.509（用户口径「我要的是半透明**毛玻璃**，不是半透明面板」）：**玻璃卡片表面**。
 *
 * <p>它和 {@link GlassFrameDrawable} 的区别只有一条，但正是缺了就"看着不像玻璃"的那一条：</p>
 * <ul>
 *   <li>{@code GlassFrameDrawable} 是「半透明色块 + 渐变描边」—— 底下是什么就透出什么，
 *       所以它看起来永远像一块**贴纸 / 面板**（v1.10.504~508 连续五版都栽在这）；</li>
 *   <li>这里多画一层 **frost 内芯**：把背后的页面在**卡片所在的那块区域**再糊一遍（比周围更糊），
 *       于是卡片内部是「更细的色雾」、卡片外面还留着一点结构 —— 这是真实毛玻璃的物理行为
 *       （磨砂面自己会散射，越厚的玻璃越糊），肉眼一看就知道"这块是有厚度的玻璃"。</li>
 * </ul>
 *
 * <p>层序（从下到上，全部裁在 26dp 圆角里）：</p>
 * <ol>
 *   <li>frost 内芯（{@code host} 的屏幕矩形 → 糊图坐标，逐帧现算，跟得上入场动画和键盘位移）；</li>
 *   <li>表面色：上淡下深的两段渐变（同一块玻璃，上下厚度不同）；</li>
 *   <li>左上镜面高光（很淡的径向渐变）+ 底边内阴影（做出"边"）；</li>
 *   <li>顶部 1.2dp 棱线高光（浅色主题不做：白卡上的白线等于没画）；</li>
 *   <li>外圈 1.2dp 渐变描边（色板与 {@code GlassFrameDrawable} 完全一致，观感统一）。</li>
 * </ol>
 *
 * <p>**为什么不用 {@code setClipToOutline} + 两个叠起来的 View**：卡片里要放 EditText，
 * 中间夹一层"只画不碰"的 View 会改触摸分发的顺序；一个 Drawable 自己画完，命中区域一点不变。</p>
 */
public final class FrostedGlassDrawable extends Drawable {

    private final Paint frostPaint = new Paint(Paint.ANTI_ALIAS_FLAG
            | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF outer = new RectF();
    private final RectF rimRect = new RectF();
    private final Rect src = new Rect();
    private final Path clip = new Path();
    private final int[] loc = new int[2];

    @Nullable
    private Shader fillShader;
    @Nullable
    private Shader sheenShader;
    @Nullable
    private Shader innerShadowShader;
    @Nullable
    private Shader edgeShader;

    /** 表面色（半透明填充用），不含 alpha。 */
    private final int surface;
    private final float radiusPx;
    private final float strokePx;
    private final boolean dark;
    /** 表面不透明度（0~1）。毛玻璃就该透；0.9 以上就是面板了。 */
    private final float fillAlpha;
    /** 卡片自己的 View —— 用来算它此刻在屏幕上的矩形（入场动画 / 键盘上移都算得对）。 */
    @Nullable
    private final View host;

    @Nullable
    private Bitmap frost;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private int originX;
    private int originY;
    private int alpha = 255;

    public FrostedGlassDrawable(int surface, float radiusPx, float strokePx, boolean dark,
                               float fillAlpha, @Nullable View host) {
        this.surface = surface;
        this.radiusPx = radiusPx;
        this.strokePx = Math.max(1.0f, strokePx);
        this.dark = dark;
        this.fillAlpha = Math.max(0.05f, Math.min(0.95f, fillAlpha));
        this.host = host;
        this.fillPaint.setStyle(Paint.Style.FILL);
        this.rimPaint.setStyle(Paint.Style.STROKE);
        this.rimPaint.setStrokeWidth(this.strokePx);
        this.sheenPaint.setStyle(Paint.Style.FILL);
        this.innerShadowPaint.setStyle(Paint.Style.FILL);
        this.edgePaint.setStyle(Paint.Style.STROKE);
        this.edgePaint.setStrokeWidth(Math.max(1.0f, strokePx));
    }

    /**
     * 把「更糊的那张底图」接进来（v1.10.509）。
     *
     * @param frost     1/6 缩放的整屏模糊图（比背景底图糊得更重）
     * @param originX   frost 左上角在**屏幕**上的 x
     * @param originY   frost 左上角在**屏幕**上的 y
     * @param scaleX    frost 像素 / 屏幕像素（横向）
     * @param scaleY    frost 像素 / 屏幕像素（纵向）
     */
    public void setFrost(@Nullable Bitmap frost, int originX, int originY,
                         float scaleX, float scaleY) {
        this.frost = frost;
        this.originX = originX;
        this.originY = originY;
        this.scaleX = scaleX > 0.0f ? scaleX : 1.0f;
        this.scaleY = scaleY > 0.0f ? scaleY : 1.0f;
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        this.outer.set(bounds);
        // ---- 表面色：上淡下深 ----
        // 同一块玻璃，越往下越厚、越沉。浅色主题往下压成浅灰（白→灰的坡），
        // 深色主题往下压得更深（保留表面色的色相，只降亮度）。
        int top = withAlpha(this.surface, this.fillAlpha);
        int bottom = withAlpha(shade(this.surface, this.dark ? 0.78f : 0.93f),
                Math.min(0.95f, this.fillAlpha + 0.06f));
        this.fillShader = new LinearGradient(bounds.left, bounds.top, bounds.left, bounds.bottom,
                top, bottom, Shader.TileMode.CLAMP);
        this.fillPaint.setShader(this.fillShader);

        // ---- 左上镜面高光：玻璃受光面 ----
        float glowRadius = Math.max(bounds.width(), bounds.height()) * 0.72f;
        int sheenCore = this.dark ? 0x1CFFFFFF : 0x30FFFFFF;
        this.sheenShader = new RadialGradient(
                bounds.left + bounds.width() * 0.14f, bounds.top + bounds.height() * 0.10f,
                glowRadius, new int[]{sheenCore, 0x00FFFFFF}, new float[]{0.0f, 1.0f},
                Shader.TileMode.CLAMP);
        this.sheenPaint.setShader(this.sheenShader);

        // ---- 底边内阴影：卡片的"下沿厚度" ----
        float band = Math.min(bounds.height() * 0.5f, this.radiusPx * 1.6f);
        int shadowColor = this.dark ? 0x52000000 : 0x1A000000;
        this.innerShadowShader = new LinearGradient(bounds.left, bounds.bottom - band,
                bounds.left, bounds.bottom, 0x00000000, shadowColor, Shader.TileMode.CLAMP);
        this.innerShadowPaint.setShader(this.innerShadowShader);

        // ---- 顶部棱线高光（深色主题才画）----
        if (this.dark) {
            this.edgeShader = new LinearGradient(bounds.left, bounds.top,
                    bounds.left, bounds.top + Math.max(18.0f, bounds.height() * 0.22f),
                    new int[]{0x7AFFFFFF, 0x00FFFFFF}, new float[]{0.0f, 1.0f},
                    Shader.TileMode.CLAMP);
        } else {
            // 浅色主题：白玻璃上再画白线等于没画，改画一道极淡的墨色"起线"，
            // 顶边才不会和后面的模糊底粘在一起。
            this.edgeShader = new LinearGradient(bounds.left, bounds.top,
                    bounds.left, bounds.top + Math.max(14.0f, bounds.height() * 0.16f),
                    new int[]{0x14000000, 0x00000000}, new float[]{0.0f, 1.0f},
                    Shader.TileMode.CLAMP);
        }
        this.edgePaint.setShader(this.edgeShader);

        // ---- 圆角裁切路径 + 外圈渐变描边（色板与 GlassFrameDrawable 一致）----
        this.clip.reset();
        this.clip.addRoundRect(this.outer, this.radiusPx, this.radiusPx, Path.Direction.CW);
        // v1.10.510：**描边要收口**。原来最后一档是 0x00（完全透明），于是右下两条边
        // 一条线都不画 —— 圆角只剩下左上那一截，看起来就是「四个角没做出来」。
        // 现在最小也留一档很淡的线，轮廓闭合成一圈，只是光感仍然从左上往右下衰减。
        int[] colors = this.dark
                ? new int[]{0x9EFFFFFF, 0x55FFFFFF, 0x3CFFFFFF, 0x42FFFFFF}
                : new int[]{0x6E000000, 0x46000000, 0x30000000, 0x32000000};
        this.rimPaint.setShader(new LinearGradient(bounds.left, bounds.top,
                bounds.left + bounds.width() * 0.62f, bounds.bottom,
                colors, new float[]{0.0f, 0.38f, 0.72f, 1.0f}, Shader.TileMode.CLAMP));
    }

    /**
     * 把「糊图里对应卡片的那一块」算进 {@link #src}。
     *
     * <p>逐帧现算，而不是 show() 时算一次：入场动画有 scale/translation、键盘弹出会整卡上移，
     * 算一次的话玻璃内芯就会"错位"——肉眼看不出错位（都是糊的），但会看出**卡片里外糊度不一致**，
     * 那正好就是"不像玻璃"的来源。</p>
     */
    private void sampleFrost() {
        this.src.setEmpty();
        Bitmap bitmap = this.frost;
        View view = this.host;
        if (bitmap == null || bitmap.isRecycled() || view == null) {
            return;
        }
        int w = view.getWidth();
        int h = view.getHeight();
        if (w <= 0 || h <= 0 || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
            return;
        }
        view.getLocationOnScreen(this.loc);
        int left = Math.round((this.loc[0] - this.originX) * this.scaleX);
        int top = Math.round((this.loc[1] - this.originY) * this.scaleY);
        int right = Math.round((this.loc[0] - this.originX + w) * this.scaleX);
        int bottom = Math.round((this.loc[1] - this.originY + h) * this.scaleY);
        // 夹进图内再交给 drawBitmap（越界那一条只是不画，不会崩）
        if (right <= 0 || bottom <= 0 || left >= bitmap.getWidth() || top >= bitmap.getHeight()) {
            return;
        }
        this.src.set(Math.max(0, left), Math.max(0, top),
                Math.min(bitmap.getWidth(), right), Math.min(bitmap.getHeight(), bottom));
        if (this.src.width() <= 0 || this.src.height() <= 0) {
            this.src.setEmpty();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.outer.width() <= 0.0f || this.outer.height() <= 0.0f) {
            return;
        }
        // ⚠️ v1.10.510：**整层都裁进圆角路径**。
        // 上一版只把「高光 / 内阴影」裁了，忘了 ① 的 frost 位图和 ② 的表面填充：
        // `drawBitmap(bitmap, src, dst, paint)` 的 dst 是**矩形**，它不认圆角 ——
        // 于是卡片四个角各留下一块「方的」补丁，圆的描边从补丁内侧才起弧，
        // 看起来就是「边角没做掉 / 四角没填满」（用户截图指的就是这四处）。
        canvas.save();
        canvas.clipPath(this.clip);
        // ① 玻璃内芯：卡片那块"再糊一遍"的采样（没有底图就退化成纯半透明表面）
        Bitmap bitmap = this.frost;
        if (bitmap != null && !bitmap.isRecycled()) {
            sampleFrost();
            if (!this.src.isEmpty()) {
                this.frostPaint.setAlpha(this.alpha);
                canvas.drawBitmap(bitmap, this.src, this.outer, this.frostPaint);
            }
        }
        // ② 表面色（上淡下深）
        this.fillPaint.setAlpha(this.alpha);
        canvas.drawRoundRect(this.outer, this.radiusPx, this.radiusPx, this.fillPaint);
        // ③ 高光 / 内阴影（已经裁过，但留着也无害 —— 这里只求层序清楚）
        this.sheenPaint.setAlpha(this.alpha);
        canvas.drawRect(this.outer, this.sheenPaint);
        float band = Math.min(this.outer.height() * 0.5f, this.radiusPx * 1.6f);
        this.innerShadowPaint.setAlpha(this.alpha);
        canvas.drawRect(this.outer.left, this.outer.bottom - band,
                this.outer.right, this.outer.bottom, this.innerShadowPaint);
        canvas.restore();
        // ④ 顶部棱线（描边只留上沿，靠 shader 自己在 18~22% 高度内淡到 0）
        float half = this.strokePx / 2.0f;
        this.rimRect.set(this.outer.left + half, this.outer.top + half,
                this.outer.right - half, this.outer.bottom - half);
        this.edgePaint.setAlpha(this.alpha);
        canvas.drawRoundRect(this.rimRect, Math.max(0.0f, this.radiusPx - half),
                Math.max(0.0f, this.radiusPx - half), this.edgePaint);
        // ⑤ 外圈渐变描边
        this.rimPaint.setAlpha(this.alpha);
        canvas.drawRoundRect(this.rimRect, Math.max(0.0f, this.radiusPx - half),
                Math.max(0.0f, this.radiusPx - half), this.rimPaint);
    }

    @Override
    public void getOutline(Outline outline) {
        // setElevation 的软投影靠它：不给圆角轮廓就会画成方块
        outline.setRoundRect((int) Math.ceil(this.outer.left), (int) Math.ceil(this.outer.top),
                (int) Math.floor(this.outer.right), (int) Math.floor(this.outer.bottom),
                Math.max(0.0f, this.radiusPx));
    }

    @Override
    public void setAlpha(int value) {
        this.alpha = Math.max(0, Math.min(255, value));
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.fillPaint.setColorFilter(colorFilter);
        this.rimPaint.setColorFilter(colorFilter);
        this.frostPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return this.alpha >= 255 && this.fillAlpha >= 1.0f
                ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT;
    }

    /** 把一个颜色的亮度压到 [factor]（保留色相），用于"越往下越厚"的那一段。 */
    private static int shade(int color, float factor) {
        int r = Math.round(((color >> 16) & 0xFF) * factor);
        int g = Math.round(((color >> 8) & 0xFF) * factor);
        int b = Math.round((color & 0xFF) * factor);
        return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * alpha);
        if (a <= 0) {
            a = 1;
        }
        return (a << 24) | (color & 0x00FFFFFF);
    }
}
