package io.github.animwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Movie;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 预览帧解析（v1.9.9 起落盘 spool，v1.10.0 改「按时间采样 + 流式解码」）。
 *
 * 关键点（v1.10.0）：
 * 1) 抽帧不再按「帧序号 × 1000/fps」去猜源帧，而是按【每帧真实显示时长】累计的时间轴
 *    采样：源帧 i 覆盖 [t0, t0+d)，就把它写进 t ∈ [t0, t0+d) 区间里的每一个输出帧。
 *    于是输出帧的时间点永远对得上原素材，变量延时 GIF 也不会错位。
 * 2) GIF / APNG / 动图 WebP 全部走手写解码器（流式，一次只驻留一帧），
 *    不再经过 Movie.setTime（它自己会改帧时长，取回来的是上一帧 → 抽帧感）。
 * 3) 帧仍以无损 PNG 落盘，内存与帧数无关。
 */
public final class PreviewFrames {

    /** v1.10.304：L06 的**唯一出口**（预览两处异常都走这里，避免同一个代号出现两种文本）。 */
    private static void logExtractFailure(@NotNull android.content.Context context, @NotNull Throwable th) {
        Diag.warn(context, "L06", "抽帧中途异常: " + th.getClass().getSimpleName());
    }

    @NotNull
    public static final PreviewFrames INSTANCE = new PreviewFrames();

    /** v1.10.4：取消检查（离开应用/换素材时立刻停手，不再白跑）。 */
    public interface Cancel {
        boolean isCancelled();
    }

    private static final Cancel NEVER = new Cancel() {
        @Override
        public boolean isCancelled() {
            return false;
        }
    };

    /** 内部：抽帧被取消。 */
    static final class Cancelled extends RuntimeException {
    }

    /** 抽帧进度回调：done/total + 目前抽好的帧 + 本次采样帧率。 */
    public interface Progress {
        void invoke(int done, int total, @NotNull List<String> framesSoFar, int fps);
    }

    public static final class Result {
        @NotNull
        private final List<String> frames;
        /** 帧率 ×10（330 = 33.0fps）—— 播放间隔按这个精确算。 */
        private final int fpsX10;
        private final long durationMs;

        public Result(@NotNull List<String> list, int fpsX10, long durationMs) {
            this.frames = list;
            this.fpsX10 = fpsX10;
            this.durationMs = durationMs;
        }

        @NotNull
        public List<String> getFrames() {
            return this.frames;
        }

        /** 显示/兼容用：四舍五入到整数帧率。 */
        public int getFps() {
            return Math.max(Math.round(this.fpsX10 / 10.0f), 1);
        }

        /** 精确帧率 ×10。 */
        public int getFpsX10() {
            return this.fpsX10;
        }

        public long getDurationMs() {
            return this.durationMs;
        }

        @NotNull
        public String toString() {
            return "Result(frames=" + this.frames.size() + ", fpsX10=" + this.fpsX10 + ", durationMs=" + this.durationMs + ')';
        }
    }

    private PreviewFrames() {
    }

    @NotNull
    public static Result load$default(PreviewFrames previewFrames, Context context, Uri uri, MediaKind mediaKind,
                                      int fps, int maxEdge, int quality, int limit, Progress progress,
                                      int mask, Object obj) {
        if ((mask & 64) != 0) {
            limit = 0;
        }
        return previewFrames.load(context, uri, mediaKind, fps, maxEdge, quality, limit, progress, NEVER);
    }

    @NotNull
    public Result load(@NotNull Context context, @NotNull Uri uri, @NotNull MediaKind kind, int fps,
                       int maxEdge, int quality, int limit, @NotNull Progress onProgress) {
        return load(context, uri, kind, fps, maxEdge, quality, limit, onProgress, NEVER);
    }

    @NotNull
    public Result load(@NotNull Context context, @NotNull Uri uri, @NotNull MediaKind kind, int fpsX10,
                       int maxEdge, int quality, int limit, @NotNull Progress onProgress,
                       @NotNull Cancel cancel) {
        int safeFps = clamp(fpsX10, 10, 500);
        int frameCap = limit > 0 ? limit : Math.min(40 * safeFps / 10, MediaImporter.MAX_FRAMES);
        // v1.10.26（需求 5）：预览帧原来被硬钳到 720 —— 编辑页画布实测 907×510，
        // 等于把 720 的帧放大 1.26 倍上屏，肉眼必然发虚（F10 日志里「源=720x405 画布px=907x510」就是证据）。
        // 提到 960：刚好盖住 907 画布（缩小而非放大），又不至于像 1080 那样把 50fps 播放拖下去。
        int edge = kind == MediaKind.IMAGE ? clamp(maxEdge, 120, 2560) : clamp(maxEdge, 120, 960);
        int q = clamp(quality, 50, 100);

        File pfRoot = new File(context.getFilesDir(), "pf");
        pfRoot.mkdirs();
        long nowMs = System.currentTimeMillis();
        File[] olds = pfRoot.listFiles();
        if (olds != null) {
            for (File stale : olds) {
                if (nowMs - stale.lastModified() > 600000L) {
                    try {
                        deleteRecursive(stale);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        File spoolDir = new File(pfRoot, "b" + nowMs + "-" + (int) (Math.random() * 1000));
        spoolDir.mkdirs();

        final ArrayList<String> frames = new ArrayList<>();
        int effectiveFps = safeFps;
        try {
            switch (kind) {
                case IMAGE:
                    loadImage(context, uri, edge, spoolDir, frames, safeFps, onProgress);
                    break;
                case GIF:
                    effectiveFps = loadAnimated(context, uri, safeFps, edge, frameCap, spoolDir, frames, onProgress, cancel);
                    break;
                case VIDEO:
                default:
                    effectiveFps = loadVideo(context, uri, safeFps, edge, frameCap, limit, spoolDir, frames, onProgress, cancel);
                    break;
            }
        } catch (Cancelled cancelled) {
            // v1.10.4：被取消（离开应用/换素材）—— 立刻停手并删掉本批临时帧
            Diag.warn(context, "L26", "抽帧被取消，已清理本批临时文件");
            frames.clear();
            deleteRecursive(spoolDir);
            return new Result(new ArrayList<String>(), safeFps, 0L);
        } catch (Throwable th) {
            logExtractFailure(context, th);
            // 已经发布的帧不回收（可能正在上屏），只丢引用
            frames.clear();
        }

        // v1.10.169：**这里漏了 ×10**。
        // effectiveFps 是帧率×10（loadAnimated 返回 fitFps 的结果，333 = 33.3fps；
        // 同一个文件里 frameCount() 就是用 `durationMs * fpsX10 / 10000` 换算的）。
        // 原来写成 `* 1000L / effectiveFps`，24 帧 / 333 会算成 72ms（应为 720ms）——
        // 正好差 10 倍，且只有「24 帧这种小素材」才容易被 L35 自检抓出来。
        long durationMs = frames.isEmpty() ? 0L : (long) frames.size() * 10000L / Math.max(effectiveFps, 1);
        if (kind == MediaKind.GIF) {
            Diag.warn(context, "G10", "GIF 出帧 " + frames.size() + " 张, " + FrameUtil.fpsX10Text(effectiveFps));
        } else if (kind == MediaKind.VIDEO) {
            Diag.warn(context, "V10", "视频出帧 " + frames.size() + " 张, " + FrameUtil.fpsX10Text(effectiveFps));
        }
        return new Result(frames, effectiveFps, durationMs);
    }

    // ------------------------------------------------------------------ 图片

    private void loadImage(Context context, Uri uri, int edge, File spoolDir, List<String> frames,
                           int fps, Progress onProgress) throws java.io.IOException {
        // v1.10.9：单帧素材也先报 0/1 —— 否则进度条只有一个瞬间的 100%，看起来像「没动过」
        onProgress.invoke(0, 1, frames, fps);
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        InputStream in = context.getContentResolver().openInputStream(uri);
        if (in != null) {
            try {
                BitmapFactory.decodeStream(in, null, bounds);
            } finally {
                closeQuietly(in);
            }
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IllegalStateException("image decode failed");
        }
        BitmapFactory.Options opts = ColorFix.options();
        opts.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, edge, edge);
        InputStream in2 = context.getContentResolver().openInputStream(uri);
        if (in2 == null) {
            throw new IllegalStateException("image decode failed");
        }
        Bitmap decoded;
        try {
            decoded = BitmapFactory.decodeStream(in2, null, opts);
        } finally {
            closeQuietly(in2);
        }
        if (decoded == null) {
            throw new IllegalStateException("image decode failed");
        }
        Bitmap normalized = ColorFix.toSrgb(decoded);
        if (normalized != decoded && !decoded.isRecycled()) {
            decoded.recycle();
        }
        decoded = normalized;
        Diag.warn(context, "C01", "图片色彩 " + ColorFix.describe(decoded) + " 采样 " + ColorFix.sample(decoded));
        Bitmap scaled = FrameUtil.fit(decoded, edge);
        frames.add(saveFrame(scaled, spoolDir, frames.size(), true));
        if (scaled != decoded && !scaled.isRecycled()) {
            scaled.recycle();
        }
        if (!decoded.isRecycled()) {
            decoded.recycle();
        }
        onProgress.invoke(1, 1, frames, fps);
    }

    // ------------------------------------------------------------------ 动图（GIF / APNG / WebP）

    /**
     * 读一次文件，按真实每帧时长采样。
     * @return 实际采样帧率（= 用户帧率与帧数上限二者的小者）
     */
    private int loadAnimated(Context context, Uri uri, int safeFps, int edge, int frameCap, File spoolDir,
                             List<String> frames, Progress onProgress, Cancel cancel) {
        // v1.10.498：GIF 走**流式**（同导出侧）。预览原来也 readAll 整份文件，
        // 210MB 的 GIF 光「打开预览」就会 OOM，而 OOM 被吞成 "read failed"。
        String sniff = MediaSniff.sniff(context, uri);
        final boolean streamGif = "gif".equals(sniff);
        byte[] bytes = null;
        SourceFps.Anim anim;
        if (streamGif) {
            anim = SourceFps.probeAnimGifStream(context, uri);
            Diag.warn(context, "W51", "动图探测(流式) " + (anim != null ? anim.toString() : "null"));
        } else {
            bytes = SourceFps.readAll(context, uri);
            if (bytes == null) {
                throw new IllegalStateException("read failed");
            }
            anim = SourceFps.probeAnim(bytes);
        }
        if (anim == null) {
            // 不是能识别的动图：退回静态首帧（至少能看见东西），并留编号
            Bitmap still = decodeStill(context, uri, bytes);
            if (still == null) {
                Diag.warn(context, "G03", "动图: 无法识别且静态解码失败");
                throw new IllegalStateException("anim decode failed");
            }
            Bitmap scaled = FrameUtil.fit(still, edge);
            frames.add(saveFrame(scaled, spoolDir, frames.size(), false));
            if (scaled != still && !scaled.isRecycled()) {
                scaled.recycle();
            }
            if (!still.isRecycled()) {
                still.recycle();
            }
            Diag.warn(context, "G03", "动图: 退回静态首帧(1 帧)");
            onProgress.invoke(1, 1, frames, safeFps);
            return safeFps;
        }
        // v1.10.133：**动图不再按 40 秒截断**。
        // 实测（K05）：947 帧 / 75.74s 的动图被砍成 500 帧 / 40s → 播到一半就跳回开头，
        // 而它的体积只有 ~28MB，根本没到 250MB 预算。时长上限本来该由「帧数上限」隐含保证：
        // fitFps 会把帧率降到 frameCap 以内，时长保持不变。
        int durationMs = Math.max(anim.durationMs, 1);
        final FrameTimeline timeline = FrameTimeline.plan(anim.frames, durationMs, safeFps, frameCap);
        final int outFps = timeline.outFps;
        final int outCount = timeline.outCount;
        Diag.warn(context, "K01", "动图探测 " + anim + " → 采样 " + FrameUtil.fpsX10Text(outFps) + "/" + outCount + " 帧");
        // v1.10.27（需求 3）：动图帧率的原始依据（前 8 帧延时）—— 觉得「不准」时贴这一行定位
        try {
            int[] ds = anim.delays;
            if (ds != null && ds.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(ds.length, 8); i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append(ds[i]);
                }
                Diag.warn(context, "F12", "动图延时(ms) 前" + Math.min(ds.length, 8) + "帧=[" + sb
                        + "] 共" + anim.frames + "帧 总" + anim.durationMs + "ms"
                        + (anim.estimated ? " (推算，文件里没有可信延时)" : " (精确，来自帧头)")
                        + (anim.filled > 0
                            ? " **其中 " + anim.filled + "/" + anim.frames + " 帧的延时时我们补的（文件没写），不是读到的**"
                            : ""));
            }
        } catch (Throwable ignored) {
        }
        final List<String> out = frames;
        final long[] lastReport = {0L};
        final Progress progress = onProgress;

        final Spool spool = new Spool(spoolDir, frames);
        final Throwable[] failure = new Throwable[1];
        int decoded;
        try {
            AnimStream.Sink sink = new AnimStream.Sink() {
                @Override
                public void onFrame(@NotNull Bitmap frame, int times) {
                    if (cancel.isCancelled()) {
                        throw new Cancelled();
                    }
                    // v1.10.122：编码/写盘丢给并行线程池，落盘顺序仍与解码顺序一致
                    spool.submit(frame, times, false);
                    long now = System.currentTimeMillis();
                    if (now - lastReport[0] > 60L) {
                        lastReport[0] = now;
                        // 进度回调只是 UI 通知，它抛异常绝不能把抽帧停在半路
                        try {
                            progress.invoke(Math.min(timeline.emitted(), outCount), outCount, out, outFps);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            };
            if (streamGif) {
                decoded = AnimStream.runGifStream(SourceFps.openStream(context, uri), anim.delays, edge,
                        timeline, sink, failure);
            } else {
                decoded = AnimStream.run(bytes, edge, timeline, sink, failure);
            }
        } catch (Throwable th) {
            spool.abort();
            throw th;
        }
        spool.finish();
        if (failure[0] != null) {
            logExtractFailure(context, failure[0]);
        }
        // v1.10.136：**实解传落盘帧数**。采样会把 947 帧抽成 592 帧（受帧率上限约束），
        // 解码器仍然吐 946 张 —— 拿它和「采样 592」比必然报 ⚠。要比的是「真正留下来的张数」。
        animCheck(context, "预览", anim, frames.size(), outFps, outCount, cancel.isCancelled());
        if (decoded == 0 || frames.isEmpty()) {
            Diag.warn(context, "P10", "动图: 解码出 0 帧（探测=" + anim + "）");
            // 兜底：手写解码器万一有漏，至少把静态首帧画出来（绝不空着）
            Bitmap still = decodeStill(context, uri, bytes);
            if (still != null) {
                Bitmap scaled = FrameUtil.fit(still, edge);
                frames.add(saveFrame(scaled, spoolDir, 0, false));
                if (scaled != still && !scaled.isRecycled()) {
                    scaled.recycle();
                }
                if (!still.isRecycled()) {
                    still.recycle();
                }
                Diag.warn(context, "P11", "动图: 解码失败，退回静态首帧");
            }
        }
        onProgress.invoke(Math.min(timeline.emitted(), outCount), outCount, out, outFps);
        return outFps;
    }

    // ------------------------------------------------------------------ 视频

    private int loadVideo(Context context, Uri uri, int safeFps, int edge, int frameCap, int limit,
                          File spoolDir, List<String> frames, Progress onProgress, Cancel cancel) {
        long durationMs = 0L;
        try {
            MediaMetadataRetriever probe = new MediaMetadataRetriever();
            try {
                probe.setDataSource(context, uri);
                durationMs = parseLong(probe.extractMetadata(9));
            } finally {
                probe.release();
            }
        } catch (Throwable ignored) {
        }
        long usableMs = Math.min(durationMs, 40000L);
        int effectiveFps = safeFps;
        final int[] fpsHolder = new int[1];
        int planned;
        if (durationMs > 0L) {
            effectiveFps = fitFps(usableMs, safeFps, frameCap);
            planned = clamp(frameCount(usableMs, effectiveFps), 1, frameCap);
        } else {
            planned = limit > 0 ? limit : clamp(frameCount(5000L, safeFps), 1, frameCap);
            Diag.warn(context, "V07", "视频: 读不到时长，按 5 秒抽帧");
        }
        Diag.warn(context, "V04", "视频: 元数据 时长=" + durationMs + "ms → " + planned + " 帧@" + effectiveFps + "fps");
        fpsHolder[0] = effectiveFps;

        // v1.10.2：顺序解码（快 20 倍以上，色彩也按 color standard 走）
        final int plannedFinal = planned;
        final long[] lastReport = {0L};
        final long startMs = System.currentTimeMillis();
        final boolean[] sawFrame = {false};
        final Spool spool = new Spool(spoolDir, frames);
        int emitted;
        try {
            emitted = VideoFrames.extract(context, uri, effectiveFps, planned, edge,
                new VideoFrames.Sink() {
                    @Override
                    public boolean onFrame(int index, @NotNull Bitmap frame) {
                        if (cancel.isCancelled()) {
                            // v1.10.25：这里原来 throw new Cancelled()，但 VideoFrames 的
                            // catch(Throwable) 会把它当成「顺序解码不可用」→ 白跑一遍慢 20 倍的
                            // 逐帧 seek（日志里 C05 Cancelled → V11 → V06）。改成正常收工，
                            // 取消由下方统一抛，语义清晰。
                            return false;
                        }
                        sawFrame[0] = true;
                        // v1.10.122：同上 —— 解码顺序不变，编码写盘并行
                        spool.submit(frame, 1, false);
                        long now = System.currentTimeMillis();
                        if (now - lastReport[0] > 80L) {
                            lastReport[0] = now;
                            onProgress.invoke(Math.min(index + 1, plannedFinal), plannedFinal, frames, fpsHolder[0]);
                        }
                        return true;
                    }
                });
        } catch (Throwable th) {
            spool.abort();
            throw th;
        }
        spool.finish();
        // v1.10.25：取消（离开页面/换素材/开始导出）不算解码失败，直接走取消清理，不回退 seek
        if (cancel.isCancelled()) {
            throw new Cancelled();
        }
        if (emitted > 0 && sawFrame[0]) {
            Diag.warn(context, "V10", "视频出帧 " + frames.size() + " 张, " + FrameUtil.fpsX10Text(effectiveFps)
                    + "（顺序解码 " + (System.currentTimeMillis() - startMs) + "ms，并行编码=" + spool.investedMs() + "ms, 边长上限=" + edge + "）");
            onProgress.invoke(frames.size(), plannedFinal, frames, effectiveFps);
            return effectiveFps;
        }
        // 回退：逐帧 seek（老路，慢但兼容所有编码）
        Diag.warn(context, "V11", "顺序解码不可用，回退逐帧 seek");
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        final Spool seekSpool = new Spool(spoolDir, frames);
        try {
            retriever.setDataSource(context, uri);
            long firstFrameStart = System.currentTimeMillis();
            for (int index = 0; index < planned; index++) {
                Bitmap frame = retriever.getFrameAtTime((long) index * 100000L / effectiveFps,
                        MediaMetadataRetriever.OPTION_CLOSEST);
                if (index == 0) {
                    Diag.warn(context, "V06", "视频: 首帧耗时 " + (System.currentTimeMillis() - firstFrameStart)
                            + "ms" + (frame == null ? " (返回 null)" : ""));
                }
                if (frame == null) {
                    Diag.warnThrottled(context, "V05", "视频: 第 " + index + " 帧 getFrameAtTime 返回 null");
                } else {
                    if (index == 0) {
                        Diag.warn(context, "C01", "视频帧色彩 " + ColorFix.describe(frame) + " 采样 " + ColorFix.sample(frame));
                    }
                    Bitmap normalized = ColorFix.toSrgb(frame);
                    if (normalized != frame && !frame.isRecycled()) {
                        frame.recycle();
                    }
                    frame = normalized;
                    Bitmap scaled = FrameUtil.fit(frame, edge);
                    seekSpool.submit(scaled, 1, false);
                    if (scaled != frame && !scaled.isRecycled()) {
                        scaled.recycle();
                    }
                    if (!frame.isRecycled()) {
                        frame.recycle();
                    }
                }
                if (cancel.isCancelled()) {
                    throw new Cancelled();
                }
                onProgress.invoke(index + 1, planned, frames, effectiveFps);
            }
        } catch (Throwable th) {
            seekSpool.abort();
            throw th;
        } finally {
            try {
                retriever.release();
            } catch (Throwable ignored) {
            }
        }
        seekSpool.finish();
        return effectiveFps;
    }

    // ------------------------------------------------------------------ 工具

    /** @param wanted/cap 均为帧率×10（350 = 35fps） */
    int fitFps(long durationMs, int wantedX10, int cap) {
        if (durationMs <= 0) {
            return Math.max(wantedX10, 10);
        }
        int maxByCap = Math.max((int) ((cap * 10000L) / durationMs), 10);
        return Math.max(Math.min(wantedX10, maxByCap), 10);
    }

    int frameCount(long durationMs, int fpsX10) {
        return Math.max((int) (((durationMs * (long) fpsX10) + 9999L) / 10000L), 1);
    }

    private static int sampleSize(int sw, int sh, int tw, int th) {
        int sample = 1;
        while (sw / (sample * 2) >= tw && sh / (sample * 2) >= th) {
            sample *= 2;
        }
        return sample;
    }

    /**
     * 帧写盘。
     * v1.10.4：动图/视频改成 WebP(95) —— 720px 一张 PNG 要 40~70ms（131 帧就 8 秒），
     * WebP 只要 10~20ms，体积还小一个数量级；质量 95 肉眼看不出差别（保留 alpha）。
     * 单帧图片仍用无损 PNG。
     */
    @NotNull
    private static String spoolName(int index, boolean lossless) {
        return String.format(java.util.Locale.US, lossless ? "%05d.png" : "%05d.webp", Integer.valueOf(index));
    }

    private static String saveFrame(@NotNull Bitmap source, @NotNull File dir, int index, boolean lossless) {
        File f = new File(dir, spoolName(index, lossless));
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(f);
            if (lossless) {
                source.compress(Bitmap.CompressFormat.PNG, 100, out);
            } else {
                Bitmap.CompressFormat format = android.os.Build.VERSION.SDK_INT >= 30
                        ? Bitmap.CompressFormat.WEBP_LOSSY : Bitmap.CompressFormat.WEBP;
                source.compress(format, MediaImporter.PREVIEW_QUALITY, out);
            }
        } catch (Throwable th) {
            // 写盘失败也要把路径返回，上层至少还能显示前几帧
        } finally {
            closeQuietly(out);
        }
        return f.getAbsolutePath();
    }

    /** 播放时按需解码一帧（调用方负责 recycle）。 */
    @Nullable
    public static Bitmap decodeFrame(@NotNull String path) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeFile(path, opts);
        } catch (Throwable th) {
            return null;
        }
    }

    /**
     * v1.10.56：**按预览框尺寸解码**（和 v1.10.54 给桌面组件做的同一件事）。
     *
     * 原来播放时一律把 spool 的整帧（最长边最大 960）解出来，再缩到预览框（~600×300）里合成 ——
     * 白白多解 2~4 倍像素，每帧多 3~8ms；动图播放一拍只有 10000/fpsX10 ms（30fps = 33ms），
     * 解码 + 合成一旦超过这个数就会丢帧（日志 P12「预览合成跟不上」）。
     * API 28+ 用 ImageDecoder.setTargetSize 直接解到「刚好盖住预览框」的尺寸（保留比例 + 15% 余量）。
     */
    @Nullable
    public static Bitmap decodeFrame(@NotNull String path, int targetW, int targetH) {
        if (android.os.Build.VERSION.SDK_INT >= 28 && targetW > 0 && targetH > 0) {
            try {
                android.graphics.ImageDecoder.Source src =
                        android.graphics.ImageDecoder.createSource(new File(path));
                Bitmap decoded = android.graphics.ImageDecoder.decodeBitmap(src,
                        (decoder, info, source) -> {
                            // v1.10.58（致命 bug 修复）：分配器必须**先**无条件设成软件。
                            // 原来只在「需要缩放」的分支里设，帧比预览框小时提前 return，
                            // 结果拿到硬件位图 → FrameComposer 在软件 Canvas 上画 → 抛
                            // IllegalArgumentException → 预览一帧都出不来（动图预览全黑）。
                            decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE);
                            int w = info.getSize().getWidth();
                            int h = info.getSize().getHeight();
                            if (w <= 0 || h <= 0) {
                                return;
                            }
                            float cover = Math.max((float) targetW / w, (float) targetH / h);
                            float k = Math.min(1.0f, cover * 1.15f);
                            if (k >= 0.999f) {
                                return;
                            }
                            decoder.setTargetSize(Math.max(1, Math.round(w * k)), Math.max(1, Math.round(h * k)));
                        });
                if (decoded != null) {
                    Bitmap fixed = ColorFix.toSrgb(decoded);
                    if (fixed != null) {
                        return fixed;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return decodeFrame(path);
    }

    /**
     * v1.10.132（B5）：**动图「帧数 / 帧延时」核对**。
     *
     * 用户要验的就是这两件事，所以直接摆到一起：
     *   · 帧数：文件头声明的 frames  vs  实际解出来的张数；
     *   · 延时：文件头声明的总时长      vs  逐帧延时之和（没有延时表就写明）。
     * 任一不一致会带 ⚠，看一眼就知道是「解码少给了帧」还是「延时表读错了」。
     */
    static void animCheck(@NotNull Context context, @NotNull String where,
                          @NotNull SourceFps.Anim anim, int decoded, int outFpsX10, int outCount) {
        animCheck(context, where, anim, decoded, outFpsX10, outCount, false);
    }

    /** @param cancelled true = 预览/导出被中途取消，此时「实解 < 采样」是正常的，不能报 ⚠ */
    static void animCheck(@NotNull Context context, @NotNull String where,
                          @NotNull SourceFps.Anim anim, int decoded, int outFpsX10, int outCount,
                          boolean cancelled) {
        long delaySum = 0L;
        int delayCount = 0;
        if (anim.delays != null) {
            for (int d : anim.delays) {
                delaySum += Math.max(0, d);
                delayCount++;
            }
        }
        int fpsX10 = Math.max(outFpsX10, 10);
        // 采样帧数对应的时长（这才是「导出/预览实际覆盖了多久」）
        long outMs = (long) outCount * 10000L / fpsX10;
        boolean countBad = outCount > 0 && decoded > 0 && Math.abs(decoded - outCount) > 1;
        long tolerance = Math.max(120L, anim.durationMs / 20L);
        boolean timeBad = anim.durationMs > 0 && Math.abs(outMs - anim.durationMs) > tolerance;
        StringBuilder sb = new StringBuilder(128);
        sb.append("动图核对[").append(where).append("] 头 ")
                .append(anim.frames).append("帧/").append(anim.durationMs).append("ms")
                .append(" 采样 ").append(fpsX10 / 10.0f).append("fps/").append(outCount).append("帧")
                .append("(").append(outMs).append("ms)")
                .append(" 实解=").append(decoded);
        if (delayCount > 0) {
            sb.append(" 延时和=").append(delaySum).append("ms/").append(delayCount).append("帧");
        }
        if (countBad) {
            sb.append(cancelled ? "（已取消，只落了部分）" : " ⚠实解与采样不符");
        }
        if (timeBad) {
            sb.append(" ⚠时长不符");
        }
        Diag.warn(context, "K05", sb.toString());
    }

    /**
     * v1.10.122：预览抽帧的**编码并行段**。
     *
     * 解码必须顺序（MediaCodec / 动图解出来的顺序就是时间顺序），但解码之后的
     * 「缩放 + WebP 编码 + 写盘」互不依赖 —— 实测 720px 一帧 10~20ms，串行时它把整条
     * 流水线堵住。这里交给固定线程池，用 Future **按提交顺序**回收，落盘帧号与串行时完全一致。
     *
     * 两条纪律：
     *   · 交给后台之前**先 copy 一份** —— 解码器可能复用同一块 Bitmap（GIF 解码器尤其）；
     *   · 在飞任务有上限（线程数×2），内存不会被几百帧撑爆。
     */
    private static final class Spool {

        private final File dir;
        private final List<String> frames;
        private final java.util.concurrent.ExecutorService pool;
        private final java.util.ArrayDeque<java.util.concurrent.Future<String>> queue =
                new java.util.ArrayDeque<>();
        private final int maxInFlight;
        private int next;
        private long investedMs;

        Spool(@NotNull File dir, @NotNull List<String> frames) {
            this.dir = dir;
            this.frames = frames;
            int cores = Runtime.getRuntime().availableProcessors();
            int threads = Math.max(2, Math.min(4, cores - 1));
            this.maxInFlight = threads * 2;
            this.pool = java.util.concurrent.Executors.newFixedThreadPool(threads, r -> {
                Thread t = new Thread(r, "anim-spool");
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            });
        }

        /** 提交一帧（times = 这一帧要占几拍，动图帧时长 > 1 拍时 >1）。 */
        void submit(@NotNull Bitmap source, int times, boolean lossless) {
            int count = Math.max(1, times);
            int first = this.next;
            this.next += count;
            if (source.isRecycled()) {
                // 位图已被回收：补占位路径，保证帧号不错位（否则后面所有帧都要挪一位）
                for (int i = 0; i < count; i++) {
                    frames.add(new File(dir, spoolName(first + i, lossless)).getAbsolutePath());
                }
                return;
            }
            Bitmap copy;
            try {
                copy = source.copy(Bitmap.Config.ARGB_8888, false);
            } catch (Throwable th) {
                copy = null;
            }
            if (copy == null) {
                // 拷不出来（硬件位图 / OOM）：退回同步，保证这一帧不丢
                for (int i = 0; i < count; i++) {
                    frames.add(saveFrame(source, dir, first + i, lossless));
                }
                return;
            }
            final Bitmap job = copy;
            final int start = first;
            final int span = count;
            queue.add(pool.submit(() -> {
                try {
                    String path = saveFrame(job, dir, start, lossless);
                    // 同一帧重复多拍：编码一次，其余直接复制文件（原来每拍都重编一遍）
                    for (int i = 1; i < span; i++) {
                        copyFile(new File(path), new File(dir, spoolName(start + i, lossless)));
                    }
                    return path;
                } finally {
                    if (!job.isRecycled()) {
                        job.recycle();
                    }
                }
            }));
            trim();
            drainReady();
        }

        /** 在飞任务到达上限：按顺序等最早那一个（保证落盘顺序）。 */
        private void trim() {
            while (queue.size() >= this.maxInFlight) {
                joinHead();
            }
        }

        /** 已完成的按顺序收，不完全就等下一帧再说。 */
        private void drainReady() {
            while (!queue.isEmpty() && queue.peek().isDone()) {
                joinHead();
            }
        }

        private void joinHead() {
            java.util.concurrent.Future<String> head = queue.poll();
            if (head == null) {
                return;
            }
            long t0 = System.currentTimeMillis();
            try {
                frames.add(head.get());
            } catch (Throwable th) {
                // 编码/写盘异常：补一个占位路径，帧号不能错位
                frames.add(new File(dir, spoolName(frames.size(), false)).getAbsolutePath());
            }
            this.investedMs += System.currentTimeMillis() - t0;
        }

        /** 收尾：把还在跑的全部按顺序回收，然后关线程池。 */
        void finish() {
            while (!queue.isEmpty()) {
                joinHead();
            }
            pool.shutdown();
        }

        /** 取消：不再等，直接丢掉在飞任务。 */
        void abort() {
            queue.clear();
            pool.shutdownNow();
        }

        long investedMs() {
            return this.investedMs;
        }
    }

    /** 复制一个已经编好的帧文件（动图同一帧占多拍时用）。 */
    private static void copyFile(@NotNull File from, @NotNull File to) {
        java.io.FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new java.io.FileInputStream(from);
            out = new FileOutputStream(to);
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        } catch (Throwable ignored) {
            // 复制失败就当这拍缺一张：上层按帧号读取时会走到兜底
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    private static void deleteRecursive(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursive(child);
            }
        }
        file.delete();
    }

    private static long parseLong(String value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (Throwable th) {
            return 0L;
        }
    }

    /**
     * 静态首帧兜底（v1.10.498）：流式路径下没有整份字节，就从 uri 解一张。
     */
    @org.jetbrains.annotations.Nullable
    private static Bitmap decodeStill(Context context, Uri uri, @org.jetbrains.annotations.Nullable byte[] bytes) {
        try {
            if (bytes != null) {
                return ColorFix.toSrgb(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
            }
            java.io.InputStream in = SourceFps.openStream(context, uri);
            if (in == null) {
                return null;
            }
            try {
                return ColorFix.toSrgb(BitmapFactory.decodeStream(in));
            } finally {
                closeQuietly(in);
            }
        } catch (Throwable th) {
            return null;
        }
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }
}
