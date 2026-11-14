package io.github.animwidget;

import android.graphics.Bitmap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 动图时间轴（v1.10.0）。
 *
 * 「源帧 → 输出帧」的唯一换算：源帧覆盖 [start, start+delay)，输出帧 j 的时间点是
 * j * 1000 / outFps，落在区间里的每一个 j 都要写一份。这样输出帧的时间轴
 * 严格等距（每帧 1000/outFps），而采样点又贴着素材的真实时长 ——
 * 预览、导出、桌面播放共用同一套数学，逐帧对得上。
 */
final class FrameTimeline {

    final int outFps;
    final int outCount;
    /** 原生模式：每个源帧恰好输出 1 份，不按时间轴重采样（不丢帧、不重复帧）。 */
    private final boolean perFrame;
    private int emitted;

    /** @param outFps 帧率 ×10（330 = 33.0fps，333 = 33.3fps） */
    FrameTimeline(int outFps, int outCount) {
        this(outFps, outCount, false);
    }

    FrameTimeline(int outFps, int outCount, boolean perFrame) {
        this.outFps = Math.max(outFps, 10);
        this.outCount = Math.max(outCount, 1);
        this.perFrame = perFrame;
    }

    /**
     * 预览与导出共用的唯一规划入口（保证两边是同一份帧表）。
     * 请求帧率 ≥ 素材自身平均帧率、且帧数不超上限 → 原生模式（帧数 = 源帧数，不丢帧）；
     * 否则按目标帧率在时间轴上重采样（总时长不变）。
     */
    static FrameTimeline plan(int frames, long durationMs, int wantedX10, int cap) {
        if (frames > 0 && durationMs > 0 && frames <= cap) {
            long nativeX10 = Math.round(frames * 10000.0 / durationMs);
            if (nativeX10 >= 10 && nativeX10 <= 500 && wantedX10 >= nativeX10) {
                return new FrameTimeline((int) nativeX10, frames, true);
            }
        }
        int fps = fitFps(durationMs, wantedX10, cap);
        return new FrameTimeline(fps, frameCount(durationMs, fps));
    }

    int emitted() {
        return this.emitted;
    }

    boolean done() {
        return this.emitted >= this.outCount;
    }

    /** @return 本源帧要写出的输出帧数量（0 表示被丢弃，例如用户把帧率压得比素材低） */
    int consume(long startMs, long delayMs) {
        if (this.perFrame) {
            if (this.emitted >= this.outCount) {
                return 0;
            }
            this.emitted++;
            return 1;
        }
        long end = startMs + Math.max(delayMs, 1L);
        // fps 可能是小数（×10 存在 outFps 里），所以按 /10000 换算
        int to = (int) Math.min(this.outCount, (end * this.outFps + 9999L) / 10000L);
        int n = to - this.emitted;
        if (n <= 0) {
            return 0;
        }
        this.emitted = to;
        return n;
    }

    /** 帧率上限约束（wanted 为 ×10）：时长越长，能均匀抽到的帧率越低（保证总帧数 ≤ cap）。 */
    static int fitFps(long durationMs, int wantedX10, int cap) {
        if (durationMs <= 0) {
            return Math.max(wantedX10, 10);
        }
        int maxByCap = Math.max((int) ((cap * 10000L) / durationMs), 10);
        return Math.max(Math.min(wantedX10, maxByCap), 10);
    }

    static int frameCount(long durationMs, int fpsX10) {
        if (durationMs <= 0) {
            return 1;
        }
        return Math.max((int) ((durationMs * fpsX10 + 9999L) / 10000L), 1);
    }
}

/** 动图统一入口：按格式分发到手写解码器，按时间轴采样（v1.10.0）。 */
final class AnimStream {

    interface Sink {
        /** @param times 这一帧要写出几份（同一源帧可能覆盖多个输出帧） */
        void onFrame(@NotNull Bitmap frame, int times);
    }

    private AnimStream() {
    }

    /**
     * @return 0 = 不认识的格式；>0 = 实际解出的源帧数
     */
    static int run(@Nullable byte[] bytes, int maxEdge, @NotNull FrameTimeline timeline, @NotNull Sink sink) {
        return run(bytes, maxEdge, timeline, sink, null);
    }

    /**
     * @param failure 非 null 时，sink/解码中途抛出的异常写进 failure[0]（原来被 catch 吞掉，
     *                解码悄悄停在半路，表现就是「实解 < 采样」）。Cancelled 一律继续上抛。
     */
    static int run(@Nullable byte[] bytes, int maxEdge, @NotNull FrameTimeline timeline, @NotNull Sink sink,
                   @Nullable Throwable[] failure) {
        if (bytes == null || bytes.length < 16) {
            return 0;
        }
        String sniff = MediaSniff.sniffBytes(bytes, bytes.length);
        final int[] clock = {0};
        final int[] count = {0};
        final FrameTimeline tl = timeline;
        final Sink out = sink;
        final int edge = maxEdge;
        try {
            if ("gif".equals(sniff)) {
                GifFrames.decode(bytes, edge, new GifFrames.Sink() {
                    @Override
                    public boolean onFrame(int index, @NotNull Bitmap frame, int delayMs) {
                        int start = clock[0];
                        clock[0] += Math.max(delayMs, 1);
                        int times = tl.consume(start, delayMs);
                        count[0]++;
                        if (times > 0) {
                            out.onFrame(frame, times);
                        }
                        // 解码器给的图由它自己回收，这里不要 recycle
                        return !tl.done();
                    }
                });
            } else if ("apng".equals(sniff)) {
                ApngFrames.each(bytes, edge, new ApngFrames.Sink() {
                    @Override
                    public boolean onFrame(int index, @NotNull Bitmap frame, int delayMs) {
                        int start = clock[0];
                        clock[0] += Math.max(delayMs, 1);
                        int times = tl.consume(start, delayMs);
                        count[0]++;
                        if (times > 0) {
                            out.onFrame(frame, times);
                        }
                        return !tl.done();
                    }
                });
            } else if ("webp-anim".equals(sniff)) {
                WebpAnimFrames.each(bytes, edge, new WebpAnimFrames.Sink() {
                    @Override
                    public boolean onFrame(int index, @NotNull Bitmap frame, int delayMs) {
                        int start = clock[0];
                        clock[0] += Math.max(delayMs, 1);
                        int times = tl.consume(start, delayMs);
                        count[0]++;
                        if (times > 0) {
                            out.onFrame(frame, times);
                        }
                        return !tl.done();
                    }
                });
            } else {
                return 0;
            }
        } catch (PreviewFrames.Cancelled cancelled) {
            throw cancelled;
        } catch (Throwable th) {
            if (failure != null && failure.length > 0) {
                failure[0] = th;
            }
            return count[0];
        }
        return count[0];
    }

    /**
     * v1.10.498：GIF 走流式解码（整份文件不进堆），采样/时间轴逻辑与
     * {@link #run(byte[], int, FrameTimeline, Sink, Throwable[])} 的 gif 分支逐行一致。
     *
     * <p>只在「嗅探结果是 gif」时调用；APNG / 动图 WebP 仍走 {@code run(byte[])}。
     */
    static int runGifStream(@Nullable java.io.InputStream raw, @Nullable int[] headDelays, int maxEdge,
                            @NotNull FrameTimeline timeline, @NotNull Sink sink,
                            @Nullable Throwable[] failure) {
        return runGifStream(raw, headDelays, maxEdge, timeline, sink, failure, false);
    }

    /** v1.10.501：{@code ownFrames = true} 时帧的所有权交给 sink（导出并行编码用）。 */
    static int runGifStream(@Nullable java.io.InputStream raw, @Nullable int[] headDelays, int maxEdge,
                            @NotNull FrameTimeline timeline, @NotNull Sink sink,
                            @Nullable Throwable[] failure, boolean ownFrames) {
        final int[] clock = {0};
        final int[] count = {0};
        final FrameTimeline tl = timeline;
        final Sink out = sink;
        try {
            GifStream.decode(raw, headDelays, maxEdge, new GifStream.Sink() {
                @Override
                public boolean onFrame(int index, @NotNull Bitmap frame, int delayMs) {
                    int start = clock[0];
                    clock[0] += Math.max(delayMs, 1);
                    int times = tl.consume(start, delayMs);
                    count[0]++;
                    if (times > 0) {
                        out.onFrame(frame, times);
                    }
                    return !tl.done();
                }
            }, ownFrames);
        } catch (PreviewFrames.Cancelled cancelled) {
            throw cancelled;
        } catch (Throwable th) {
            if (failure != null && failure.length > 0) {
                failure[0] = th;
            }
        }
        return count[0];
    }
}
