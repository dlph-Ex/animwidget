package io.github.animwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

/**
 * 导入（把素材抽成帧文件，桌面播放用）。
 *
 * v1.10.0：动图部分和预览走同一条「时间轴采样 + 流式解码」链路
 * （{@link AnimStream} / {@link FrameTimeline}），
 * 因此「预览什么样、导出就是什么样、桌面播放就是什么速度」。
 */
public final class MediaImporter {

    @NotNull
    public static final MediaImporter INSTANCE = new MediaImporter();
    public static final int MAX_FRAMES = 2400;   // v1.10.423：40 秒 @50fps = 2000 帧，1200 的老上限会把它压成 30fps（画布连带跳到 1920）
    private static final long MAX_TOTAL_BYTES = 262144000L;
    /**
     * v1.10.498：250MB 从「硬截断」降级为「软预算」（只告警），真正的硬顶放这里。
     * 原来的判断写在写盘之后、而且只退出「当前源帧的重复份」循环 ——
     * 下一帧进来没有前置检查照样写，等于完全没有限流。
     */
    private static final long MAX_TOTAL_BYTES_HARD = 524288000L;

    /**
     * v1.10.501：导出编码线程数。
     *
     * <p>真机实测（685×887 q95，593 帧）：单线程导出 69 秒 = 116ms/帧；
     * 同一台机器、同一素材，烘焙用 8 线程同样的 593 帧只要 13.8 秒（W13/W27），
     * 单帧编码 144ms、CPU 75%。差值 5 倍全部来自「一个线程串行做编码」。
     * 取 6：6 / 0.144 ≈ 42 帧/秒，正好等于烘焙实测吞吐，同时给 UI 与预览留 2 核。</p>
     */
    private static final int ENCODE_THREADS = 6;

    public static final class Result {
        private final int frameCount;
        private final int width;
        private final int height;
        /** 帧率 ×10（333 = 33.3fps）。 */
        private final int fpsX10;

        public Result(int frameCount, int width, int height, int fpsX10) {
            this.frameCount = frameCount;
            this.width = width;
            this.height = height;
            this.fpsX10 = fpsX10;
        }

        public final int getFpsX10() {
            return this.fpsX10;
        }

        public final int getFrameCount() {
            return this.frameCount;
        }

        public final int getWidth() {
            return this.width;
        }

        public final int getHeight() {
            return this.height;
        }

        public final int getFps() {
            return Math.max(Math.round(this.fpsX10 / 10.0f), 1);
        }

        @NotNull
        public String toString() {
            return "Result(frameCount=" + this.frameCount + ", width=" + this.width + ", height=" + this.height
                    + ", fpsX10=" + this.fpsX10 + ')';
        }
    }

    private MediaImporter() {
    }

    /** v1.10.1：导出取消检查（离开应用立即取消本轮）。 */
    public interface Cancel {
        boolean isCancelled();
    }

    private static final Cancel NEVER = new Cancel() {
        @Override
        public boolean isCancelled() {
            return false;
        }
    };

    /** Kotlin 侧叫 import（Java 关键字，所以方法名带 m 前缀）。 */
    @NotNull
    public Result m9import(@NotNull Context context, @NotNull WidgetConfig config, @NotNull Uri uri,
                           @NotNull Function2<? super Integer, ? super Integer, Unit> onProgress) throws java.io.IOException {
        Result result = m9import(context, config, uri, onProgress, NEVER);
        if (result == null) {
            throw new java.io.InterruptedIOException("cancelled");
        }
        return result;
    }

    /**
     * v1.10.1：可取消的导出。取消返回 null（调用方负责清理暂存目录）。
     * 所有帧写进 frames.new（暂存），全部成功后才由调用方 commit —— 取消不会毁掉旧组件。
     */
    @Nullable
    public Result m9import(@NotNull Context context, @NotNull WidgetConfig config, @NotNull Uri uri,
                           @NotNull Function2<? super Integer, ? super Integer, Unit> onProgress,
                           @NotNull Cancel cancel) throws java.io.IOException {
        return m9import(context, config, uri, 0, onProgress, cancel);
    }

    /**
     * v1.10.26（需求 4）：fpsX10 &gt; 0 时用调用方给的确切帧率（337 = 33.7fps）。
     * 原来一律 config.getFps()*10 —— 33.7fps 的素材会被导出成 34.0fps（快 0.9%）。
     */
    @Nullable
    public Result m9import(@NotNull Context context, @NotNull WidgetConfig config, @NotNull Uri uri,
                           int fpsX10,
                           @NotNull Function2<? super Integer, ? super Integer, Unit> onProgress,
                           @NotNull Cancel cancel) throws java.io.IOException {
        // v1.10.1：只清暂存目录（旧帧在全部成功前不动）
        WidgetStore.INSTANCE.discardStaging(context, config.getWidgetId());
        int frameId = config.getWidgetId();
        // v1.10.8：fps 参数按 ×10 传入（调用方给精确帧率），这里只做范围钳制
        int fps = fpsX10 > 0 ? clamp(fpsX10, 10, 500) : clamp(config.getFps() * 10, 10, 500);
        int edge = clamp(config.getMaxEdge(), 120, 2560);
        int quality = clamp(config.getQuality(), 50, 100);
        MediaKind kind = config.getKind();
        if (kind == MediaKind.IMAGE) {
            return importImage(context, frameId, uri, fps, edge, quality, onProgress, cancel);
        }
        if (kind == MediaKind.GIF) {
            return importAnimated(context, frameId, uri, fps, edge, quality, onProgress, cancel);
        }
        return importVideo(context, frameId, uri, fps, edge, quality, onProgress, cancel);
    }

    // ------------------------------------------------------------------ 图片

    private Result importImage(Context context, int frameId, Uri uri, int fps, int maxEdge, int quality,
                               Function2<? super Integer, ? super Integer, Unit> onProgress, Cancel cancel) throws java.io.IOException {
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
            throw new IllegalStateException(context.getString(R.string.err_image_decode));
        }
        BitmapFactory.Options opts = ColorFix.options();
        opts.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge, maxEdge);
        InputStream in2 = context.getContentResolver().openInputStream(uri);
        if (in2 == null) {
            throw new IllegalStateException(context.getString(R.string.err_image_decode));
        }
        Bitmap decoded;
        try {
            decoded = BitmapFactory.decodeStream(in2, null, opts);
        } finally {
            closeQuietly(in2);
        }
        if (decoded == null) {
            throw new IllegalStateException(context.getString(R.string.err_image_decode));
        }
        Bitmap normalized = ColorFix.toSrgb(decoded);
        if (normalized != decoded && !decoded.isRecycled()) {
            decoded.recycle();
        }
        decoded = normalized;
        Bitmap scaled = FrameUtil.fit(decoded, maxEdge);
        int width = scaled.getWidth();
        int height = scaled.getHeight();
        // v1.10.59：单帧图片 → 质量 100 时就是**真无损**（2.5K 以内原图直出、无色差）
        long bytes = writeFrame(scaled, stagingTemp(context, frameId, 0), quality, true);
        selfCheck(context, scaled, stagingTemp(context, frameId, 0), 0, quality, true);
        if (scaled != decoded && !scaled.isRecycled()) {
            scaled.recycle();
        }
        if (!decoded.isRecycled()) {
            decoded.recycle();
        }
        if (bytes <= 0) {
            throw new IllegalArgumentException(context.getString(R.string.err_image_decode));
        }
        onProgress.invoke(Integer.valueOf(1), Integer.valueOf(1));
        return new Result(compact(context, frameId), width, height, fps);
    }

    // ------------------------------------------------------------------ 动图

    private Result importAnimated(Context context, int frameId, Uri uri, int fps, int maxEdge, int quality,
                                  Function2<? super Integer, ? super Integer, Unit> onProgress, Cancel cancel)
            throws java.io.IOException {
        // v1.10.498：**GIF 改流式**，不再把整份文件读进堆。
        // 210MB 的 GIF 走 readAll，一次 ByteArrayOutputStream 扩容就是 400MB+ 的峰值；
        // OOM 又被 readBytes 的 catch(Throwable) 吞成 null → 上层只能报「动图解码失败」，
        // 用户看到的就是「这种体积的素材导不进去」。GIF 是顺序块结构，流式内存恒定。
        String sniff = MediaSniff.sniff(context, uri);
        final boolean streamGif = "gif".equals(sniff);
        byte[] bytes = null;
        SourceFps.Anim anim = null;
        if (streamGif) {
            anim = SourceFps.probeAnimGifStream(context, uri);
            Diag.warn(context, "W51", "动图探测(流式) " + (anim != null ? anim.toString() : "null"));
        } else {
            bytes = SourceFps.readAll(context, uri);
            if (bytes == null) {
                throw new IllegalStateException(context.getString(R.string.err_gif_decode));
            }
            anim = SourceFps.probeAnim(bytes);
        }
        if (anim == null) {
            // 认不出来的「动图」：退回静态首帧，至少组件能显示一张图
            Bitmap still = decodeStillFrame(context, uri, bytes);
            if (still == null) {
                throw new IllegalStateException(context.getString(R.string.err_gif_decode));
            }
            Bitmap scaled = FrameUtil.fit(still, maxEdge);
            int width = scaled.getWidth();
            int height = scaled.getHeight();
            writeFrame(scaled, stagingTemp(context, frameId, 0), quality);
            if (scaled != still && !scaled.isRecycled()) {
                scaled.recycle();
            }
            if (!still.isRecycled()) {
                still.recycle();
            }
            onProgress.invoke(Integer.valueOf(1), Integer.valueOf(1));
            Diag.warn(context, "G03", "导出: 动图无法识别，退回静态首帧");
            return new Result(compact(context, frameId), width, height, 1);
        }
        // v1.10.133：同预览 —— 动图不截时长，靠帧数上限保证规模
        int durationMs = Math.max(anim.durationMs, 1);
        final FrameTimeline timeline = FrameTimeline.plan(anim.frames, durationMs, fps, MAX_FRAMES);
        final int outFps = timeline.outFps;
        final int outCount = timeline.outCount;
        final int[] size = {0, 0};
        final int q = quality;
        // v1.10.59：帧数少 → 质量 100 走真无损（用户要「不压缩」就必须真无损）
        final boolean losslessBig = outCount <= 40;
        Diag.warn(context, "T27", "动图导出参数 边长=" + maxEdge + " 帧数=" + outCount
                + " 质量=" + q + (q >= 100
                        ? (losslessBig ? "（无损）" : "（帧数多：边长≤720 时仍无损，超过则退回 q95）")
                        : ""));

        // ------------------------------------------------------------------
        // v1.10.501：**导出并行编码**（用户要求 N=6）
        //
        // 实测同一台机器、同一素材（685×887 / 593 帧）：
        //   单线程导出 69s（116ms/帧） vs 8 线程烘焙 13.8s（23ms/帧，单帧编码仍 144ms）
        // —— 5 倍差，全部来自「编码没有并行」。
        //
        // 为什么可以并行：解码必须串行（GIF 是流式 LZW），但**每一帧写的是独立文件**
        // （stagingTemp(frameId, idx)），不存在写盘顺序问题；帧号用原子自增分配即可。
        // 内存：队列 3 + 在飞 6 ≈ 9 张位图 ×2.43MB ≈ 22MB（256MB 堆内可接受）。
        // ------------------------------------------------------------------
        final int cores = Runtime.getRuntime().availableProcessors();
        final int workers = Math.max(2, Math.min(6, cores - 2));
        final java.util.concurrent.ArrayBlockingQueue<EncodeJob> jobs =
                new java.util.concurrent.ArrayBlockingQueue<>(3);
        final java.util.concurrent.ExecutorService encoders =
                java.util.concurrent.Executors.newFixedThreadPool(workers, r -> {
                    Thread t = new Thread(r, "anim-encode");
                    try {
                        // 与烘焙同档：编码是持续的重活，低优先级会被系统反复让出核心
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                    } catch (Throwable ignored) {
                    }
                    return t;
                });
        final java.util.concurrent.atomic.AtomicInteger written =
                new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicLong totalBytes =
                new java.util.concurrent.atomic.AtomicLong();
        final java.util.concurrent.atomic.AtomicBoolean overBudget =
                new java.util.concurrent.atomic.AtomicBoolean();
        final java.util.concurrent.atomic.AtomicReference<Throwable> jobError =
                new java.util.concurrent.atomic.AtomicReference<>();
        final long[] lastPost = {0L};
        final boolean[] selfChecked = {false};
        Diag.warn(context, "W55", "导出并行 编码线程=" + workers + " 核=" + cores
                + " 队列=3 帧数=" + outCount);

        Runnable worker = () -> {
            while (true) {
                EncodeJob job;
                try {
                    job = jobs.take();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (job == POISON) {
                    return;     // 毒丸：收工
                }
                try {
                    int base = written.getAndAdd(job.times);
                    for (int k = 0; k < job.times; k++) {
                        int idx = base + k;
                        long n = writeFrame(job.bmp, stagingTemp(context, frameId, idx), q, losslessBig);
                        if (n <= 0) {
                            jobError.compareAndSet(null,
                                    new IllegalStateException("frame write failed at #" + idx));
                            return;
                        }
                        long total = totalBytes.addAndGet(n);
                        if (total >= MAX_TOTAL_BYTES) {
                            overBudget.set(true);
                        }
                        if (total >= MAX_TOTAL_BYTES_HARD) {
                            jobError.compareAndSet(null, new IllegalStateException(
                                    "export budget exceeded: " + (total / (1024L * 1024L)) + "MB"));
                            return;
                        }
                        int done = written.get();
                        long now = System.currentTimeMillis();
                        if (done >= outCount || now - lastPost[0] >= 100L) {
                            lastPost[0] = now;
                            try {
                                onProgress.invoke(Integer.valueOf(done), Integer.valueOf(outCount));
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                } finally {
                    if (job.bmp != null && !job.bmp.isRecycled()) {
                        job.bmp.recycle();
                    }
                }
            }
        };
        for (int i = 0; i < workers; i++) {
            encoders.execute(worker);
        }

        final Throwable[] failure = new Throwable[1];
        AnimStream.Sink sink = new AnimStream.Sink() {
            @Override
            public void onFrame(@NotNull Bitmap frame, int times) {
                if (cancel.isCancelled() || jobError.get() != null) {
                    return;
                }
                if (size[0] == 0) {
                    size[0] = frame.getWidth();
                    size[1] = frame.getHeight();
                }
                // 第 0 帧同步写 + 自检：后面全异步，自检只做这一次（写盘失败要立刻知道）
                if (!selfChecked[0]) {
                    selfChecked[0] = true;
                    int base = written.getAndAdd(times);
                    for (int k = 0; k < times; k++) {
                        long n = writeFrame(frame, stagingTemp(context, frameId, base + k), q, losslessBig);
                        if (n <= 0) {
                            jobError.compareAndSet(null,
                                    new IllegalStateException("frame write failed at #" + (base + k)));
                            return;
                        }
                        totalBytes.addAndGet(n);
                    }
                    selfCheck(context, frame, stagingTemp(context, frameId, base), 0, q, losslessBig);
                    try {
                        onProgress.invoke(Integer.valueOf(written.get()), Integer.valueOf(outCount));
                    } catch (Throwable ignored) {
                    }
                    return;
                }
                Bitmap copy;
                try {
                    // 解码器回调返回后会立刻回收原图，所以必须拷一份交给编码线程
                    copy = frame.copy(Bitmap.Config.ARGB_8888, false);
                } catch (Throwable th) {
                    copy = null;
                }
                if (copy == null) {
                    jobError.compareAndSet(null, new IllegalStateException("frame copy failed"));
                    return;
                }
                try {
                    jobs.put(new EncodeJob(copy, times));
                } catch (InterruptedException ie) {
                    if (!copy.isRecycled()) {
                        copy.recycle();
                    }
                    Thread.currentThread().interrupt();
                }
            }
        };
        try {
            if (streamGif) {
                AnimStream.runGifStream(SourceFps.openStream(context, uri), anim.delays, maxEdge,
                        timeline, sink, failure);
            } else {
                AnimStream.run(bytes, maxEdge, timeline, sink, failure);
            }
        } finally {
            // 收工：出错时先排空队列（把还没编码的副本回收掉），再给每个 worker 投一颗毒丸
            if (jobError.get() != null) {
                EncodeJob drop;
                while ((drop = jobs.poll()) != null) {
                    if (drop != POISON && drop.bmp != null && !drop.bmp.isRecycled()) {
                        drop.bmp.recycle();
                    }
                }
            }
            for (int i = 0; i < workers; i++) {
                try {
                    if (!jobs.offer(POISON, 2, java.util.concurrent.TimeUnit.SECONDS)) {
                        break;
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            encoders.shutdown();
            try {
                if (!encoders.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                    encoders.shutdownNow();
                }
            } catch (InterruptedException ie) {
                encoders.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (overBudget.get()) {
            Diag.warn(context, "W52", "导出总量超 250MB 软预算: "
                    + (totalBytes.get() / (1024L * 1024L)) + "MB 帧=" + written.get() + "/" + outCount);
        }
        Diag.warn(context, "P02", "动图导出 " + written.get() + " 帧, " + FrameUtil.fpsX10Text(outFps) + ", 源=" + anim);
        Throwable dead = jobError.get();
        if (dead == null && failure[0] != null) {
            dead = failure[0];
        }
        if (dead != null) {
            Diag.warn(context, "W53", "导出解码中断: " + dead.getClass().getSimpleName()
                    + (dead.getMessage() != null ? "/" + dead.getMessage() : "")
                    + " 已写=" + written.get() + "/" + outCount);
            // 半截动画比明确失败更糟：宁可报错让用户重试，也不 commit 残缺配置
            throw new java.io.IOException("decode interrupted after " + written.get() + " frames", dead);
        }
        // v1.10.132（B5）：导出这一侧也做同样的核对（导出走的是另一条解码路径）
        PreviewFrames.animCheck(context, "导出", anim, written.get(), outFps,
                Math.max(1, outCount));
        if (written.get() <= 0) {
            // 兜底：静态首帧，组件至少能显示一张图
            Bitmap still = decodeStillFrame(context, uri, bytes);
            if (still == null) {
                throw new IllegalStateException(context.getString(R.string.err_gif_decode));
            }
            Bitmap scaled = FrameUtil.fit(still, maxEdge);
            int width = scaled.getWidth();
            int height = scaled.getHeight();
            writeFrame(scaled, stagingTemp(context, frameId, 0), quality);
            if (scaled != still && !scaled.isRecycled()) {
                scaled.recycle();
            }
            if (!still.isRecycled()) {
                still.recycle();
            }
            Diag.warn(context, "P11", "动图导出失败，退回静态首帧");
            return new Result(compact(context, frameId), width, height, 1);
        }
        return new Result(compact(context, frameId), size[0], size[1], outFps);
    }

    /** 毒丸：让 worker 收工（ArrayBlockingQueue 不收 null，必须用真对象）。 */
    private static final EncodeJob POISON = new EncodeJob(null, 0);

    /** 一帧待编码的任务（位图是解码帧的副本，由 worker 负责回收）。 */
    private static final class EncodeJob {
        final Bitmap bmp;
        final int times;

        EncodeJob(Bitmap bmp, int times) {
            this.bmp = bmp;
            this.times = times;
        }
    }

    /**
     * v1.10.501：**只解第一帧**（编辑页「解析期先上第 0 帧」用）。
     *
     * <p>为什么要单开一条：解析 593 帧要 19 秒，这段时间预览区是空的（`L22 解析开始` →
     * `L05 首帧上屏` 之间什么都没有）。用户要求「导入后立刻能看到第一帧」，那就不能等解析完 ——
     * 这里只读一帧，GIF/APNG/动图 WebP 都是「解到第 0 帧就返回 false」直接停。</p>
     *
     * @return 已缩到 maxEdge 内的首帧（调用方负责 recycle）；解不出来返回 null
     */
    @Nullable
    static Bitmap firstFrame(@NotNull Context context, @NotNull Uri uri, int maxEdge) {
        String sniff = MediaSniff.sniff(context, uri);
        final Bitmap[] out = {null};
        try {
            if ("gif".equals(sniff)) {
                GifStream.decode(SourceFps.openStream(context, uri), null, maxEdge,
                        new GifStream.Sink() {
                            @Override
                            public boolean onFrame(int index, @NotNull Bitmap frame, int delayMs) {
                                out[0] = copyOf(frame);
                                return false;   // 只要第 0 帧
                            }
                        });
                return out[0];
            }
            byte[] bytes = SourceFps.readAll(context, uri);
            if (bytes == null) {
                return null;
            }
            if ("apng".equals(sniff)) {
                ApngFrames.each(bytes, maxEdge, new ApngFrames.Sink() {
                    @Override
                    public boolean onFrame(int index, @NotNull Bitmap frame, int delayMs) {
                        out[0] = copyOf(frame);
                        return false;
                    }
                });
            } else if ("webp-anim".equals(sniff)) {
                WebpAnimFrames.each(bytes, maxEdge, new WebpAnimFrames.Sink() {
                    @Override
                    public boolean onFrame(int index, @NotNull Bitmap frame, int delayMs) {
                        out[0] = copyOf(frame);
                        return false;
                    }
                });
            } else {
                Bitmap still = ColorFix.toSrgb(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                if (still != null) {
                    out[0] = FrameUtil.fit(still, maxEdge);
                }
            }
        } catch (Throwable ignored) {
        }
        return out[0];
    }

    /** 解码器回调返回后会立刻回收原图 —— 要留下来的必须自己拷一份。 */
    @Nullable
    private static Bitmap copyOf(@Nullable Bitmap src) {
        if (src == null) {
            return null;
        }
        try {
            return src.copy(Bitmap.Config.ARGB_8888, false);
        } catch (Throwable th) {
            return null;
        }
    }

    /** 静态首帧（无法识别的动图兜底）；流式路径下 bytes 为 null 时从 uri 解。 */
    @Nullable
    private static Bitmap decodeStillFrame(Context context, Uri uri, @Nullable byte[] bytes) {
        try {
            if (bytes != null) {
                return ColorFix.toSrgb(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
            }
            InputStream in = context.getContentResolver().openInputStream(uri);
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

    // ------------------------------------------------------------------ 视频

    private Result importVideo(Context context, int frameId, Uri uri, int fps, int maxEdge, int quality,
                               Function2<? super Integer, ? super Integer, Unit> onProgress, Cancel cancel) {
        long startMs = System.currentTimeMillis();
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
        if (durationMs <= 0L) {
            throw new IllegalArgumentException(context.getString(R.string.err_no_duration));
        }
        long usableMs = Math.min(durationMs, 40000L);
        final int effectiveFps = FrameTimeline.fitFps(usableMs, fps, MAX_FRAMES);
        final int planned = Math.max(Math.min(FrameTimeline.frameCount(usableMs, effectiveFps), MAX_FRAMES), 2);
        final int[] written = {0};
        final int[] size = {0, 0};
        final long[] totalBytes = {0L};
        final int q = quality;
        final int edge = maxEdge;
        final boolean losslessBig = planned <= 40;
        Diag.warn(context, "T27", "视频导出参数 边长=" + edge + " 计划帧=" + planned
                + " 质量=" + q + (q >= 100
                        ? (losslessBig ? "（无损）" : "（帧数多：边长≤720 时仍无损，超过则退回 q95）")
                        : ""));

        // v1.10.77：**解码一帧就立刻交给编码线程池**，不再「解码→编码→写盘」串行等。
        // 每帧 WebP 编码约 40~50ms（800×720 q95），而解码只要 5~10ms —— 串行时编码把整条流水线堵死。
        // 现在解码线程只负责 copy 一份（~1ms）并投递，N 个编码线程并行写盘（帧号即文件名，乱序安全）。
        final int cores = Runtime.getRuntime().availableProcessors();
        final int workers = Math.max(1, Math.min(4, cores - 1));
        final java.util.concurrent.ExecutorService pool = workers > 1
                ? java.util.concurrent.Executors.newFixedThreadPool(workers, r -> {
                    Thread t = new Thread(r, "export-enc");
                    t.setDaemon(true);
                    return t;
                })
                : null;
        // 在飞帧上限：既限线程排队，也限内存（大素材自动退回「一帧一停」的串行节奏）
        final java.util.concurrent.Semaphore inflight = new java.util.concurrent.Semaphore(Math.max(2, workers * 2));
        final java.util.concurrent.atomic.AtomicInteger done = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicLong totalBytesAtomic = new java.util.concurrent.atomic.AtomicLong();
        final java.util.concurrent.atomic.AtomicBoolean failed = new java.util.concurrent.atomic.AtomicBoolean();
        Diag.warn(context, "V13", "视频导出并行编码：核=" + cores + " 线程=" + Math.max(workers, 1));

        int emitted = VideoFrames.extract(context, uri, effectiveFps, planned, edge, new VideoFrames.Sink() {
            @Override
            public boolean onFrame(int index, @NotNull Bitmap frame) {
                if (cancel.isCancelled() || failed.get()) {
                    return false;
                }
                if (size[0] == 0) {
                    size[0] = frame.getWidth();
                    size[1] = frame.getHeight();
                }
                // VideoFrames 在 onFrame 返回后会 recycle 原帧，所以这里必须拿一份自己的副本
                final Bitmap copy;
                try {
                    copy = frame.copy(Bitmap.Config.ARGB_8888, false);
                } catch (Throwable th) {
                    return false;
                }
                if (copy == null) {
                    return false;
                }
                final int frameIndex = written[0]++;
                final java.io.File target = stagingTemp(context, frameId, frameIndex);
                Runnable job = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            long bytes = writeFrame(copy, target, q, losslessBig);
                            if (bytes <= 0L) {
                                failed.set(true);
                                return;
                            }
                            if (frameIndex == 0) {
                                selfCheck(context, copy, target, 0, q, losslessBig);
                            }
                            totalBytesAtomic.addAndGet(bytes);
                            int n = done.incrementAndGet();
                            onProgress.invoke(Integer.valueOf(n), Integer.valueOf(planned));
                        } finally {
                            if (!copy.isRecycled()) {
                                copy.recycle();
                            }
                            inflight.release();
                        }
                    }
                };
                try {
                    inflight.acquire();
                } catch (Throwable ignored) {
                }
                // 单帧 > 8MB（例如 2560×1440）就当场写，别让 N 帧同时在飞把内存顶爆
                boolean bigFrame = copy.getByteCount() > 8 * 1024 * 1024;
                try {
                    if (pool != null && !bigFrame) {
                        pool.execute(job);
                    } else {
                        job.run();
                    }
                } catch (Throwable th) {
                    inflight.release();
                    failed.set(true);
                    return false;
                }
                return totalBytesAtomic.get() < MAX_TOTAL_BYTES;
            }
        });
        // 等所有编码线程写完再判定结果（进度条最后那一格就是它们）
        if (pool != null) {
            pool.shutdown();
            try {
                pool.awaitTermination(120L, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Throwable ignored) {
            }
        }
        written[0] = padTail(context, frameId, done.get(), planned, q);
        if (emitted > 0 && written[0] > 0) {
            Diag.warn(context, "V10", "视频导出 " + written[0] + "/" + planned + " 帧, " + FrameUtil.fpsX10Text(effectiveFps) + "（顺序解码）");
            // v1.10.25：导出耗时（用户看到「进度条动了」之外，也要能判断卡多久）
            Diag.warn(context, "V12", "导出耗时 " + (System.currentTimeMillis() - startMs) + "ms / "
                    + written[0] + " 帧, 质量=" + q + " 边长=" + edge);
            if (written[0] < planned / 2 && !cancel.isCancelled()) {
                // 明显被截断（内存/磁盘/编码失败）→ 报错，不要静默存一个残废配置
                Diag.warn(context, "P22", "导出被截断: " + written[0] + "/" + planned + " 帧");
                throw new IllegalStateException(context.getString(R.string.err_export_truncated,
                        new Object[]{Integer.valueOf(written[0]), Integer.valueOf(planned)}));
            }
            return new Result(compact(context, frameId), size[0], size[1], effectiveFps);
        }
        Diag.warn(context, "V11", "视频导出：顺序解码不可用，回退逐帧 seek");

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            long fallbackBytes = 0L;
            int width = 0;
            int height = 0;
            int count = 0;
            for (int index = 0; index < planned; index++) {
                if (cancel.isCancelled()) {
                    return null;
                }
                Bitmap frame = retriever.getFrameAtTime((long) index * 1000000L / effectiveFps,
                        MediaMetadataRetriever.OPTION_CLOSEST);
                if (frame != null) {
                    Bitmap normalized = ColorFix.toSrgb(frame);
                    if (normalized != frame && !frame.isRecycled()) {
                        frame.recycle();
                    }
                    frame = normalized;
                    Bitmap scaled = FrameUtil.fit(frame, maxEdge);
                    if (width == 0) {
                        width = scaled.getWidth();
                        height = scaled.getHeight();
                    }
                    long bytes = writeFrame(scaled, stagingTemp(context, frameId, count), quality);
                    if (scaled != frame && !scaled.isRecycled()) {
                        scaled.recycle();
                    }
                    if (!frame.isRecycled()) {
                        frame.recycle();
                    }
                    if (bytes <= 0) {
                        break;
                    }
                    count++;
                    fallbackBytes += bytes;
                }
                onProgress.invoke(Integer.valueOf(index + 1), Integer.valueOf(planned));
                if (fallbackBytes >= MAX_TOTAL_BYTES) {
                    break;
                }
            }
            int paddedCount = padTail(context, frameId, count, planned, quality);
            if (paddedCount != count) {
                count = paddedCount;
            }
            return new Result(compact(context, frameId), width, height, effectiveFps);
        } finally {
            try {
                retriever.release();
            } catch (Throwable ignored) {
            }
        }
    }

    // ------------------------------------------------------------------ 工具

    /**
     * 帧写盘（WebP），返回字节数；0 表示失败。
     * v1.10.7：质量 100 → 用**无损 WebP**。有损 WebP 固定 4:2:0 色度抽样，
     * 纯色/渐变素材会出现可测的色差（用户反馈「依旧有色差」）。默认参数就是 100。
     */
    /**
     * v1.10.132：**补齐结尾**。
     *
     * 实测（T26）：25588ms 的原片、计划 768 帧，解码器只吐到 766 帧 → 循环短 55ms。
     * 差 1~2 帧属于「结尾少给一张」：把最后一张复制过去，画面只是多停一拍，
     * 但循环总时长与素材严格对齐（T26 回到 0ms）。
     * 差得多（超过 max(2, 2%)）说明是真截断，交给 P22 处理，这里不补。
     */
    private int padTail(@NotNull Context context, int frameId, int written, int planned, int quality) {
        int missing = planned - written;
        if (written <= 0 || missing <= 0 || missing > Math.max(2, planned / 50)) {
            return written;
        }
        java.io.File last = stagingTemp(context, frameId, written - 1);
        if (!last.exists() || last.length() <= 0L) {
            return written;
        }
        int padded = written;
        for (int i = 0; i < missing; i++) {
            java.io.File target = stagingTemp(context, frameId, written + i);
            if (!copyFileQuick(last, target)) {
                break;
            }
            padded++;
        }
        if (padded > written) {
            Diag.warn(context, "V14", "结尾补帧 " + written + " → " + padded + "/" + planned
                    + " 帧（解码器少给 " + missing + " 张，复制末帧）");
        }
        return padded;
    }

    private static boolean copyFileQuick(@NotNull java.io.File from, @NotNull java.io.File to) {
        java.io.FileInputStream in = null;
        java.io.FileOutputStream out = null;
        try {
            in = new java.io.FileInputStream(from);
            out = new java.io.FileOutputStream(to);
            byte[] buf = new byte[32 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
            return to.length() > 0L;
        } catch (Throwable th) {
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /** v1.10.128：这一次写盘到底会不会走无损（writeFrame 与自检共用同一套判定）。 */
    static boolean isLosslessQuality(int quality, int w, int h, boolean allowBigLossless) {
        return quality >= 100 && (allowBigLossless || Math.max(w, h) <= 720);
    }

    /**
     * v1.10.128：**无损自检** —— 把刚写好的第 0 帧解码回来，与内存里的原帧逐像素比对。
     *
     * 目的：把「质量 100 到底是不是真无损」从推断变成实测数据（日志 P23）：
     *   · 无损应当 最大通道差=0、差异像素=0；
     *   · 有损（帧数多退回 q95）会给出一个可量化的偏移，正好用来判断色差有多大。
     * 只查第 0 帧一次，900×562 约 20~40ms。
     */
    static void selfCheck(@Nullable Context context, @Nullable Bitmap source, @Nullable File file,
                          int index, int quality, boolean allowBigLossless) {
        if (context == null || source == null || file == null || index != 0 || source.isRecycled()) {
            return;
        }
        Bitmap back = null;
        try {
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
            opt.inScaled = false;
            back = BitmapFactory.decodeFile(file.getAbsolutePath(), opt);
            if (back == null) {
                Diag.warn(context, "P23", "无损自检 回读失败 " + file.getName());
                return;
            }
            int w = source.getWidth();
            int h = source.getHeight();
            if (back.getWidth() != w || back.getHeight() != h) {
                Diag.warn(context, "P23", "无损自检 尺寸不一致 源=" + w + "x" + h
                        + " 回读=" + back.getWidth() + "x" + back.getHeight());
                return;
            }
            boolean lossless = isLosslessQuality(quality, w, h, allowBigLossless);
            int maxDiff = 0;
            long diffPixels = 0L;
            int rows = Math.max(1, Math.min(h, 4096 / Math.max(w, 1)));
            int[] a = new int[w * rows];
            int[] b = new int[w * rows];
            for (int y = 0; y < h; y += rows) {
                int n = Math.min(rows, h - y) * w;
                source.getPixels(a, 0, w, 0, y, w, Math.min(rows, h - y));
                back.getPixels(b, 0, w, 0, y, w, Math.min(rows, h - y));
                for (int i = 0; i < n; i++) {
                    int pa = a[i];
                    int pb = b[i];
                    if (pa == pb) {
                        continue;
                    }
                    diffPixels++;
                    int d = Math.abs(((pa >>> 24) & 0xFF) - ((pb >>> 24) & 0xFF));
                    int r = Math.abs(((pa >>> 16) & 0xFF) - ((pb >>> 16) & 0xFF));
                    int g = Math.abs(((pa >>> 8) & 0xFF) - ((pb >>> 8) & 0xFF));
                    int bl = Math.abs((pa & 0xFF) - (pb & 0xFF));
                    int m = Math.max(Math.max(d, r), Math.max(g, bl));
                    if (m > maxDiff) {
                        maxDiff = m;
                    }
                }
            }
            long total = (long) w * h;
            Diag.warn(context, "P23", "无损自检 帧=0 边长=" + w + "x" + h
                    + " 质量=" + quality + (lossless ? "(应为无损)" : "(有损)")
                    + " 最大通道差=" + maxDiff
                    + " 差异像素=" + diffPixels + "/" + total
                    + "(" + (total > 0 ? (diffPixels * 100L / total) : 0L) + "%)"
                    + (lossless && maxDiff == 0 ? " ✓ 逐像素一致" : ""));
        } catch (Throwable th) {
            Diag.warn(context, "P23", "无损自检异常 " + th.getClass().getSimpleName());
        } finally {
            if (back != null && !back.isRecycled()) {
                back.recycle();
            }
        }
    }

    private long writeFrame(@NotNull Bitmap bitmap, @NotNull File target, int quality) {
        return writeFrame(bitmap, target, quality, false);
    }

    /**
     * @param allowBigLossless v1.10.59：帧数少（≤40）时允许大尺寸也走**无损**。
     *        质量 100 ＝「不压缩」，UI 上也是这么写的，就必须真的无损 ——
     *        有损 WebP 固定 4:2:0 色度抽样，纯色/渐变素材一定有色差（用户底线：不得有色差）。
     *        帧数多时仍按体积预算退回 q95（否则 1080p 无损一帧 ~1MB，几百帧直接撞 250MB 上限）。
     */
    private long writeFrame(@NotNull Bitmap bitmap, @NotNull File target, int quality, boolean allowBigLossless) {
        FileOutputStream out = null;
        try {
            // v1.10.58：硬件位图不能 compress()（会抛异常 → 静默 0 字节 → 导出 0 帧）。
            // 解码器现在都保证给软件位图，这里只是最后一道保险。
            Bitmap safe = bitmap;
            try {
                if (bitmap.getConfig() == Bitmap.Config.HARDWARE) {
                    Bitmap copy = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                    if (copy != null) {
                        safe = copy;
                    }
                }
            } catch (Throwable ignored) {
            }
            out = new FileOutputStream(target);
            // v1.10.19：只有小尺寸才用无损（720px 以内，一帧 ~1MB）；
            // 1080p 无损一帧约 18MB，十几帧就撞上 250MB 总量上限 → 导出被截断、存成 0~13 帧。
            boolean lossless = isLosslessQuality(quality, safe.getWidth(), safe.getHeight(), allowBigLossless);
            Bitmap.CompressFormat format = lossless ? losslessWebpFormat() : webpFormat();
            // v1.10.25：有损 WebP 的编码质量封顶 95。q100 会让 libwebp 走最贵的搜索/熵编码，
            // 1080p 实测 ~240ms/帧（402 帧 = 99 秒，用户以为卡死）；q95 肉眼无差，速度 2 倍以上。
            int encode = lossless ? 100 : Math.min(clamp(quality, 0, 100), 95);
            if (!safe.compress(format, encode, out)) {
                return 0L;
            }
            out.flush();
            return target.length();
        } catch (Throwable th) {
            // v1.10.21：写盘失败原来是静默 return 0（导出只出 10 帧，没有任何痕迹）
            android.util.Log.w("AnimWidget", "P21 writeFrame failed: " + th);
            return 0L;
        } finally {
            closeQuietly(out);
        }
    }

    private File stagingTemp(Context context, int frameId, int index) {
        return WidgetStore.INSTANCE.stagingFile(context, frameId, "tmp_", index);
    }

    private Bitmap.CompressFormat losslessWebpFormat() {
        if (Build.VERSION.SDK_INT >= 30) {
            return Bitmap.CompressFormat.WEBP_LOSSLESS;
        }
        return Bitmap.CompressFormat.WEBP;
    }

    /** 预览 spool 的编码质量：97（再高对肉眼无意义，但 4:2:0 的色度损失已不可见）。 */
    static final int PREVIEW_QUALITY = 97;

    private Bitmap.CompressFormat webpFormat() {
        if (Build.VERSION.SDK_INT >= 30) {
            return Bitmap.CompressFormat.WEBP_LOSSY;
        }
        return Bitmap.CompressFormat.WEBP;
    }

    private int sampleSize(int sw, int sh, int tw, int th) {
        int sample = 1;
        while (sw / (sample * 2) >= tw && sh / (sample * 2) >= th) {
            sample *= 2;
        }
        return sample;
    }

    /** tmp_ 前缀 → f 前缀（按名字排序后顺序改名）。 */
    private int compact(Context context, int frameId) {
        File dir = WidgetStore.INSTANCE.stagingDir(context, frameId);
        File[] temps = dir.listFiles();
        if (temps == null) {
            return 0;
        }
        List<File> list = new ArrayList<>();
        for (File file : temps) {
            if (file.getName().startsWith("tmp_")) {
                list.add(file);
            }
        }
        list.sort(new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return a.getName().compareTo(b.getName());
            }
        });
        int count = 0;
        for (File file : list) {
            File target = WidgetStore.INSTANCE.stagingFile(context, frameId, "f", count);
            if (!file.getAbsolutePath().equals(target.getAbsolutePath())) {
                file.renameTo(target);
            }
            count++;
        }
        return count;
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

    private static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
