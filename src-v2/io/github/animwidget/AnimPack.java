package io.github.animwidget;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * v1.10.469（#4 接线 · 第 1 步）：把**逐帧烘焙产物**装进**一个**动图 WebP。
 *
 * <h3>为什么要有这一步</h3>
 * 现在的播放链是：{@code PlaybackService} 每 33ms 合成/取一帧 → {@code setImageViewUri} 推一次
 * → 桌面解码。App 一退出，动画就停。而 launcher 的 ImageView 本来就会自播动图 WebP
 * （用户已实测确认），所以只要能给出**一个**动图文件，就能做到「零 IPC、退出后还在动」。
 *
 * <p>本类只做**纯逻辑**：帧列表 → {@link WebpAnimWriter#mux} → 单文件，外加两个闸门
 * （帧数、体积）。android 那一侧（去哪儿找帧、日志）在 {@code WidgetPrewarm} 里，
 * 这样离线自检可以用真实文件跑（门禁 [25]）。</p>
 *
 * <h3>为什么给上限，且失败要删文件</h3>
 * 动图里每一帧都是**全画布** WebP，300 帧 600×626 大约 12~25MB。文件太大时
 * launcher 首帧出画面会明显变慢（要先把文件读进来），所以宁可不打、退回逐帧推送。
 * 失败的半成品必须删掉：桌面拿到半截 WebP 不会报错，只会**整块不显示**（静默失败）。
 */
final class AnimPack {

    /** 打成之后与帧文件同目录、同一个名字 —— {@code FrameProvider} 直接能发。 */
    static final String NAME = "anim.webp";

    /** 单文件上限 24MB：再大首帧解码会拖慢桌面。 */
    static final long MAX_BYTES = 24L * 1024L * 1024L;
    /** 帧数上限 600：再多的话文件体积与首帧时间都不划算。 */
    static final int MAX_FRAMES = 600;
    /** fps 读不出来时按 30 算（和播放侧的兜底一致）。 */
    static final int DEFAULT_FPS = 30;

    /** 没有帧可打。 */
    static final int ERR_EMPTY = -1;
    /** 帧数超过 {@link #MAX_FRAMES}。 */
    static final int ERR_TOO_MANY = -2;
    /** 打出来超过 {@link #MAX_BYTES}。 */
    static final int ERR_TOO_BIG = -3;
    /** 封装器失败（输入不是合法的单帧 WebP / 尺寸不一致 / 写盘出错）。 */
    static final int ERR_MUX = -4;
    /** 打出来的文件头不是 RIFF....WEBP（写半截）。 */
    static final int ERR_NOT_WEBP = -5;

    private AnimPack() {
    }

    /**
     * 每帧时长（毫秒）。
     *
     * <p>与播放侧同一个口径：{@code fps} 是整数档位，33.3fps 的素材在配置里存 33，
     * 所以这里只能给 30ms/33ms 这种整数；WebP 规范下限 10ms，小于它的直接夹住。</p>
     */
    static int frameDurationMs(int fps) {
        return frameDurationMsX10(Math.max(fps, 0) * 10);
    }

    /** 同上，但吃 ×10 的帧率（33.3fps → 333）。 */
    static int frameDurationMsX10(int fpsX10) {
        int x10 = fpsX10 > 0 ? fpsX10 : DEFAULT_FPS * 10;
        return Math.max(10, Math.round(10000.0f / x10));
    }

    /**
     * v1.10.474：**逐帧时长表**（累计时间对齐源片）。
     *
     * <p>容器里只能写整数毫秒，30fps = 33.333ms 必然除不尽。若每帧都写 33ms，
     * 172 帧比原片短 57ms（1.7%）。这里按**累计时间**分配：第 i 帧的时长 =
     * round((i+1)·T/N) − round(i·T/N)，单帧误差 ≤1ms，**总时长 = T（误差 0）**。</p>
     */
    static int[] frameDurations(int count, int fpsX10) {
        int n = Math.max(count, 0);
        int[] out = new int[n];
        if (n == 0) {
            return out;
        }
        int x10 = fpsX10 > 0 ? fpsX10 : DEFAULT_FPS * 10;
        long total = Math.round(n * 10000.0 / x10);
        long acc = 0L;
        for (int i = 0; i < n; i++) {
            long target = Math.round((i + 1) * total / (double) n);
            int d = (int) Math.max(10L, target - acc);
            out[i] = d;
            acc += d;
        }
        return out;
    }

    /**
     * 把一批**同尺寸**单帧 WebP 拼成一个动图文件。
     *
     * @param frames 有序帧文件
     * @param fps    帧率（用于算每帧时长）
     * @param out    目标文件（由 {@link WebpAnimWriter} 自己先写 .tmp 再改名）
     * @return 成功 = 帧数（&gt; 0）；失败 = 负数（{@code ERR_*}），且**不会留下残文件**
     */
    static int packFrames(@NotNull List<File> frames, int fpsX10, @NotNull File out) {
        if (frames.isEmpty()) {
            return ERR_EMPTY;
        }
        if (frames.size() > MAX_FRAMES) {
            deleteQuiet(out);
            return ERR_TOO_MANY;
        }
        int n = WebpAnimWriter.mux(out, frames, frameDurations(frames.size(), fpsX10),
                WebpAnimWriter.LOOP_FOREVER);
        if (n <= 0) {
            deleteQuiet(out);
            return ERR_MUX;
        }
        if (out.length() > MAX_BYTES) {
            deleteQuiet(out);
            return ERR_TOO_BIG;
        }
        if (!looksLikeWebp(out)) {
            deleteQuiet(out);
            return ERR_NOT_WEBP;
        }
        return n;
    }

    /** 头 12 字节必须是 `RIFF <size> WEBP`。写半截的 WebP 桌面不会报错、只会不显示。 */
    static boolean looksLikeWebp(@NotNull File file) {
        FileInputStream in = null;
        try {
            if (!file.isFile() || file.length() < 12L) {
                return false;
            }
            byte[] head = new byte[12];
            in = new FileInputStream(file);
            int read = 0;
            while (read < head.length) {
                int n = in.read(head, read, head.length - read);
                if (n <= 0) {
                    return false;
                }
                read += n;
            }
            return head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                    && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
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

    /** 失败原因（日志用；未知码也给一个可读的）。 */
    @NotNull
    static String reason(int code) {
        switch (code) {
            case ERR_EMPTY:
                return "没有帧";
            case ERR_TOO_MANY:
                return "帧数超过 " + MAX_FRAMES;
            case ERR_TOO_BIG:
                return "文件超过 " + (MAX_BYTES / 1024L / 1024L) + "MB";
            case ERR_MUX:
                return "封装失败（帧不是合法单帧 WebP / 尺寸不一致）";
            case ERR_NOT_WEBP:
                return "打出来不是 WebP 头（写半截）";
            default:
                return "未知原因 " + code;
        }
    }

    private static void deleteQuiet(File file) {
        if (file == null) {
            return;
        }
        try {
            if (file.exists()) {
                file.delete();
            }
            File tmp = new File(file.getAbsolutePath() + ".tmp");
            if (tmp.exists()) {
                tmp.delete();
            }
        } catch (Throwable ignored) {
        }
    }
}
