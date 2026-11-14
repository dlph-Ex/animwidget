package io.github.animwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * v1.10.478：**编码/解码对拍（W48）** —— P0-2 被否决之后唯一还没被量过的那笔账。
 *
 * <h3>为什么要做这个实验</h3>
 * 外部两份复核（DeepSeek / Claude）各自独立指出同一件事：我们一直把「每帧解码 12~13ms」
 * 记成 **launcher 侧的账**（那是 v1.10.342 立项「共享位图」时的假设），但在
 * {@code 传输=位图共享} 这条通道上，解码其实发生在**我们自己的 BMP_POOL**：
 * {@link WidgetUriPush.DecodeTask} → {@code decodeFrameFile} →
 * {@code ImageDecoder(ALLOCATOR_SHARED_MEMORY)}，日志里的 {@code 直解=173/172 零拷贝=173}
 * 就是证据（`零拷贝` 与 `解码总数` 相等 ⇒ 那 13ms 全在我们进程里）。
 * 所以 ROI 必须按「我方每帧解码成本」重算，而 W47 的吞吐口径（46.1Mpx/s）也是我们自己
 * 解出来的上限，不是桌面的。
 *
 * <p>这笔账剩下的唯一未知量是：**同一张画面，JPEG 解码到底比带 alpha 的 WebP 快多少**。
 * 外部给的数是 4.7ms vs 23.5ms（5.0×），但那是别人的机器；Claude 的说法是「没有证据，
 * 先做一个小实验：同一批帧离线再出 JPEG q93，在真机同一线程上分别测 ImageDecoder 耗时」。
 * 这里就是那个实验，只不过跑在真机上、用真帧：</p>
 *
 * <pre>
 *   ① 取这一代**中间那一帧**的成品文件（播放时真正要解的就是它）→ 计 WebP 解码耗时；
 *   ② 把同一张画面按 q95 编成 JPEG（alpha 丢掉）              → 计 JPEG 解码耗时；
 *   ③ 把 ② 的结果用**常驻蒙版**（= 成品帧自己的 alpha：阴影+圆角+挖洞）
 *      合成回带 alpha 的画布                                  → 计合成耗时。
 * </pre>
 *
 * <p>③ 是 P0-2 唯一还站得住的形态：**分层不放在布局里（那样圆角必然露方角），
 * 放在我方进程内**。如果 `JPEG + 合成 < WebP`，这条路才有谈的价值；否则说明瓶颈不在解码器，
 * 而在文件 IO 与排队（那就该看 {@code 延迟均 − 解码均} 的差，而不是换编码）。</p>
 *
 * <p>只打一行日志、只跑一次（每一代烘焙结束时），不改任何播放行为。</p>
 */
final class DecodeBench {

    /** JPEG 质量：与 {@link WidgetUriPush#JPEG_QUALITY} 同一个值（q95）。 */
    static final int QUALITY = 95;
    /** 样本数：取中位数（首轮常被类加载/页缓存污染，中位数比均值稳）。 */
    static final int ROUNDS = 5;
    /**
     * 门槛（v1.10.479 下调）。
     *
     * <p>478 是 10 万 px，结果真机上那一代是 240×240（57.6k）和 286×286（81.8k），
     * **一次都没测到** —— 而且跳过时不打任何日志，看起来就像功能没接上。
     * 现在降到 4 万 px（报一位小数，小画布上的差异也看得见），并且跳过必留一行原因。</p>
     */
    static final int MIN_PIXELS = 40000;
    static final int MAX_PIXELS = 4000000;

    private DecodeBench() {
    }

    /**
     * 跑一次对拍。失败就什么都不打（这是诊断，不是功能）。
     *
     * @param frameFile 这一代中间那一帧的成品文件（.webp，可能其实是 JPEG，按内容嗅探）
     */
    static void probe(@Nullable Context context, @Nullable File frameFile, int widgetId,
                      int idx, int count, int w, int h) {
        if (context == null) {
            return;
        }
        long px = (long) w * (long) h;
        if (px < MIN_PIXELS || px > MAX_PIXELS) {
            // v1.10.479：**不许静默**。478 这两个门槛把真机上那一代整个吞掉了（240×240 / 286×286），
            // 日志里连一行都没有，看起来就像功能没接上。
            try {
                Diag.warn(context, "W48", BenchMath.skip(widgetId, w, h, MIN_PIXELS, MAX_PIXELS));
            } catch (Throwable ignored) {
            }
            return;
        }
        if (frameFile == null || !frameFile.isFile() || frameFile.length() <= 0L) {
            return;
        }
        Bitmap src = null;
        Bitmap mask = null;
        Bitmap jpegBmp = null;
        Bitmap canvas = null;
        try {
            src = decodeFile(frameFile);
            if (src == null || src.isRecycled()) {
                return;
            }
            int bw = src.getWidth();
            int bh = src.getHeight();
            // 蒙版 = 成品帧自身的 alpha（阴影 + 圆角 + 挖洞，就是「不随帧变化的那一层」）。
            // 生产里它是烘一次常驻的；这里用同一张帧的 alpha 代替，口径一致。
            mask = src.extractAlpha();
            if (mask == null) {
                return;
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(64 * 1024, bw * bh / 8));
            if (!src.compress(Bitmap.CompressFormat.JPEG, QUALITY, bos)) {
                return;
            }
            final byte[] jpeg = bos.toByteArray();
            bos.close();
            if (jpeg.length <= 0) {
                return;
            }
            canvas = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
            // ImageDecoder.createSource(ByteBuffer) 要 direct buffer：先建一次，之后每轮 rewind 复用
            java.nio.ByteBuffer direct = java.nio.ByteBuffer.allocateDirect(jpeg.length);
            direct.put(jpeg);
            // 预热：两种解码各跑一次把类加载/页缓存/驱动初始化吃掉，不计入样本
            jpegBmp = decodeBuf(direct);
            if (jpegBmp == null) {
                return;
            }
            Bitmap warm = decodeFile(frameFile);
            if (warm != null && !warm.isRecycled()) {
                warm.recycle();
            }
            blendOnce(canvas, jpegBmp, mask);

            long[] webp = new long[ROUNDS];
            long[] jp = new long[ROUNDS];
            long[] bl = new long[ROUNDS];

            // ① WebP（带 alpha）—— 就是播放时那一张
            for (int i = 0; i < ROUNDS; i++) {
                long t0 = SystemClock.elapsedRealtimeNanos();
                Bitmap b = decodeFile(frameFile);
                webp[i] = SystemClock.elapsedRealtimeNanos() - t0;
                if (b != null && !b.isRecycled()) {
                    b.recycle();
                }
            }
            // ② JPEG（同尺寸、同像素，只是没有 alpha）
            for (int i = 0; i < ROUNDS; i++) {
                long t0 = SystemClock.elapsedRealtimeNanos();
                Bitmap b = decodeBuf(direct);
                jp[i] = SystemClock.elapsedRealtimeNanos() - t0;
                if (b != null && !b.isRecycled()) {
                    b.recycle();
                }
            }
            // ③ 合成（我方进程内：一次 blit + 一次 DST_IN）
            for (int i = 0; i < ROUNDS; i++) {
                long t0 = SystemClock.elapsedRealtimeNanos();
                blendOnce(canvas, jpegBmp, mask);
                bl[i] = SystemClock.elapsedRealtimeNanos() - t0;
            }
            // 纯逻辑（中位数 / 单位 / 判据）在 BenchMath 里，脱机自检钉住
            Diag.warn(context, "W48", BenchMath.line(widgetId, idx, count, bw, bh,
                    BenchMath.median(webp), BenchMath.median(jp), BenchMath.median(bl)));
        } catch (Throwable ignored) {
            // 诊断失败不影响任何东西
        } finally {
            if (jpegBmp != null && !jpegBmp.isRecycled()) {
                jpegBmp.recycle();
            }
            if (src != null && !src.isRecycled()) {
                src.recycle();
            }
            if (mask != null && !mask.isRecycled()) {
                mask.recycle();
            }
            if (canvas != null && !canvas.isRecycled()) {
                canvas.recycle();
            }
        }
    }

    /** 合成：把不透明的 JPEG 铺满画布，再用蒙版把它裁成成品帧的形状。 */
    private static void blendOnce(@NotNull Bitmap target, @NotNull Bitmap jpeg, @NotNull Bitmap mask) {
        target.eraseColor(0);
        Canvas canvas = new Canvas(target);
        Rect box = new Rect(0, 0, target.getWidth(), target.getHeight());
        Paint p = new Paint();
        canvas.drawBitmap(jpeg, (Rect) null, box, p);
        Paint cut = new Paint();
        cut.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(mask, (Rect) null, box, cut);
    }

    /** 与播放路径同一个解码器（API30+ 走共享内存，和 {@code decodeShared} 同口径）。 */
    @Nullable
    private static Bitmap decodeFile(@NotNull File f) {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.ImageDecoder.Source src = android.graphics.ImageDecoder.createSource(f);
                return android.graphics.ImageDecoder.decodeBitmap(src,
                        (decoder, info, source) -> decoder.setAllocator(
                                android.graphics.ImageDecoder.ALLOCATOR_SHARED_MEMORY));
            }
            return BitmapFactory.decodeFile(f.getAbsolutePath());
        } catch (Throwable th) {
            return BitmapFactory.decodeFile(f.getAbsolutePath());
        }
    }

    /** 复用同一个 direct buffer（只 rewind，不重新分配 —— 分配不能算进 JPEG 的账里）。 */
    @Nullable
    private static Bitmap decodeBuf(@NotNull java.nio.ByteBuffer bb) {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                bb.rewind();
                android.graphics.ImageDecoder.Source src = android.graphics.ImageDecoder.createSource(bb);
                return android.graphics.ImageDecoder.decodeBitmap(src,
                        (decoder, info, source) -> decoder.setAllocator(
                                android.graphics.ImageDecoder.ALLOCATOR_SHARED_MEMORY));
            }
            byte[] copy = new byte[bb.capacity()];
            bb.rewind();
            bb.get(copy);
            return BitmapFactory.decodeByteArray(copy, 0, copy.length);
        } catch (Throwable th) {
            byte[] copy = new byte[bb.capacity()];
            bb.rewind();
            bb.get(copy);
            return BitmapFactory.decodeByteArray(copy, 0, copy.length);
        }
    }
}
