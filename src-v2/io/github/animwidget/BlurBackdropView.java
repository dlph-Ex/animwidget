package io.github.animwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

/**
 * v1.10.104（需求 1）：**弹窗背后的「100% 高斯模糊」背景**。
 *
 * 传入调用方页面的截图，先缩到 1/8 再做两遍横竖盒式模糊（复用 {@link AuroraView} 的模糊核），
 * 放大回全屏绘制 —— 结构信息被彻底抹平，等价于高斯模糊全屏；
 * 最后叠一层半透明压暗色，保证弹窗卡片与文字对比度。
 */
public final class BlurBackdropView extends View {

    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private int scrimColor;
    private Bitmap source;
    private Bitmap blurred;

    public BlurBackdropView(Context context, Bitmap shot, int scrimColor) {
        super(context);
        this.source = shot;
        this.scrimColor = scrimColor;
        build();
    }

    private void build() {
        Bitmap src = this.source;
        if (src == null || src.isRecycled() || src.getWidth() <= 0 || src.getHeight() <= 0) {
            return;
        }
        // 1/6 + 较小半径：既够糊，又留得住「这是被模糊的页面」的结构感
        int w = Math.max(8, src.getWidth() / 6);
        int h = Math.max(8, src.getHeight() / 6);
        Bitmap small = null;
        try {
            small = Bitmap.createScaledBitmap(src, w, h, true);
            int[] a = new int[w * h];
            int[] b = new int[w * h];
            small.getPixels(a, 0, w, 0, 0, w, h);
            int r = Math.max(2, Math.min(w, h) / 20);
            AuroraView.blurH(a, b, w, h, r);
            AuroraView.blurV(b, a, w, h, r);
            AuroraView.blurH(a, b, w, h, r);
            AuroraView.blurV(b, a, w, h, r);
            small.setPixels(a, 0, w, 0, 0, w, h);
            this.blurred = small;
            small = null;
            // v1.10.183：糊完就不需要原图了。全屏 ARGB 是十几 MB，一直挂着会被系统算进
            // 「大对象」压力（上一版 ConfirmDialog/赞赏页都留着）。release() 里对 null 是安全的。
            Bitmap raw = this.source;
            this.source = null;
            if (raw != null && !raw.isRecycled()) {
                raw.recycle();
            }
        } catch (Throwable th) {
            this.blurred = null;
        } finally {
            if (small != null && !small.isRecycled()) {
                small.recycle();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        Bitmap b = this.blurred;
        if (b != null && !b.isRecycled()) {
            canvas.drawBitmap(b, null, new Rect(0, 0, w, h), this.paint);
        }
        // v1.10.140：原来 else 分支也画了一次 scrim，下面又画一次 ——
        // 截不到图时会叠两层，比设计值暗一倍。现在统一只叠一层。
        canvas.drawColor(this.scrimColor);
    }

    /**
     * v1.10.182：主题切换后重设压暗色（赞赏弹层 900ms 轮询会用）。
     * 原来是 final —— 深色/浅色切换后压暗层不跟，等于浅色主题下还压着一层更黑的膜。
     */
    public void setScrimColor(int color) {
        if (this.scrimColor != color) {
            this.scrimColor = color;
            invalidate();
        }
    }

    /**
     * v1.10.184：**换一张底图**（系统深浅色切换后页面重新上色，模糊底必须跟着重截，
     * 否则弹层底下还压着旧主题的页面）。旧图在这里一起回收。
     */
    public void setShot(Bitmap shot) {
        Bitmap oldBlur = this.blurred;
        this.blurred = null;
        if (oldBlur != null && !oldBlur.isRecycled()) {
            oldBlur.recycle();
        }
        Bitmap oldSrc = this.source;
        this.source = null;
        if (oldSrc != null && !oldSrc.isRecycled()) {
            oldSrc.recycle();
        }
        this.source = shot;
        build();
        invalidate();
    }

    /** 弹窗关闭时释放截图（否则全屏位图会一直挂着）。 */
    public void release() {
        if (this.blurred != null && !this.blurred.isRecycled()) {
            this.blurred.recycle();
        }
        this.blurred = null;
        if (this.source != null && !this.source.isRecycled()) {
            this.source.recycle();
        }
        this.source = null;
    }
}
