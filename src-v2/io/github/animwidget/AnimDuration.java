package io.github.animwidget;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;

/**
 * v1.10.176（反馈 2）：动图选择页封面左下角的「播放时长 + 清晰度」。
 *
 * ## 为什么这样算时长（最小 CPU + 精确）
 *
 * 时长**不靠 MediaMetadataRetriever，也不解码任何一帧像素**，而是照我们自己在导出/预览
 * 用的那套容器解析走一遍块结构：
 *   · GIF        → {@link GifFrames#info(byte[])}（含「1cs = 10ms」与「0 延时按 100ms」规则）
 *   · APNG       → {@link ApngFrames#info(byte[])}（fcTL 的 delay_num/delay_den）
 *   · 动图 WebP  → {@link WebpAnimFrames#info(byte[])}（ANMF 里的 24 位时长）
 * 所以选择页上的时长和真正导出/播放出来的长度**是同一个口径**，不会出现「这里 11.8s、
 * 导出 75.7s」那种两套数字。（代价就一次顺序读文件，上限 {@link #MAX_BYTES}。）
 *
 * 补出来的延时（文件没写）一律带 `estimated` 标记，UI 上显示成「≈」，不允许当确切值。
 *
 * ## 缓存
 *
 * 结论只跟「图片 id + 修改时间」有关，所以落盘到 `files/anim_meta.txt`
 * （`<id> <修改时间> <时长ms> <是否估算> <宽> <高>`）。第二次进页面零 I/O 零解析。
 *
 * ## 大文件（v1.10.177）
 *
 * 只有 64KB 以内的文件才整个读进内存；更大的走 {@link AnimProbe} 的**流式**块结构，
 * 内存固定 64KB、**没有大小上限**（上一版 8MB 以上直接放弃，正是「大 GIF 没时长」的原因）。
 */
final class AnimDuration {

    /** 头部嗅探读多少字节；读不满就说明整个文件已经到手（可以走内存解析）。 */
    private static final int HEAD_BYTES = 64 * 1024;

    /** id → {修改时间, 时长ms, 是否估算, 宽, 高, 帧数}（v1.10.457 加帧数：选择器要显示 fps） */
    private static final java.util.HashMap<Long, long[]> MEM = new java.util.HashMap<>();
    private static boolean loaded;
    private static boolean dirty;

    private AnimDuration() {
    }

    private static File cacheFile(Context context) {
        return new File(context.getFilesDir(), "anim_meta.txt");
    }

    /** 读一次磁盘缓存（后台线程调用；只读一次）。 */
    static synchronized void loadDisc(Context context) {
        if (loaded) {
            return;
        }
        loaded = true;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(cacheFile(context)));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                // v1.10.457：多一列帧数。**旧缓存（5 列）直接丢弃**，下次进页面重探一次即可，
                // 不做「少列就补 0」——补 0 会让那些格子永远显示不出 fps，而且没人知道为什么。
                if (parts.length != 6) {
                    continue;
                }
                try {
                    MEM.put(Long.valueOf(Long.parseLong(parts[0])), new long[]{
                            Long.parseLong(parts[1]), Long.parseLong(parts[2]),
                            Long.parseLong(parts[3]), Long.parseLong(parts[4]),
                            Long.parseLong(parts[5])});
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /** 回写磁盘缓存（只在校验到变化时写）。 */
    static synchronized void saveDisc(Context context) {
        if (!dirty || MEM.isEmpty()) {
            return;
        }
        dirty = false;
        BufferedWriter writer = null;
        File tmp = new File(context.getFilesDir(), "anim_meta.txt.tmp");
        try {
            writer = new BufferedWriter(new FileWriter(tmp));
            for (java.util.Map.Entry<Long, long[]> e : MEM.entrySet()) {
                long[] v = e.getValue();
                writer.write(e.getKey().longValue() + " " + v[0] + " " + v[1] + " " + v[2]
                        + " " + v[3] + " " + v[4] + " " + v[5]);
                writer.newLine();
            }
            writer.flush();
            writer.close();
            writer = null;
            File target = cacheFile(context);
            if (!tmp.renameTo(target)) {
                tmp.delete();
            }
        } catch (Throwable ignored) {
            tmp.delete();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /** 命中缓存则返回 {时长ms, 是否估算, 宽, 高, 帧数}，否则 null。 */
    static synchronized int[] cached(long id, long date) {
        long[] v = MEM.get(Long.valueOf(id));
        if (v == null || v[0] != date || v.length < 6) {
            return null;
        }
        return new int[]{(int) v[1], (int) v[2], (int) v[3], (int) v[4], (int) v[5]};
    }

    private static synchronized void remember(long id, long date, int[] value) {
        MEM.put(Long.valueOf(id), new long[]{date, value[0], value[1], value[2], value[3],
                value.length > 4 ? value[4] : 0L});
        dirty = true;
    }

    /**
     * 解析一个素材的时长与尺寸。**必须在后台线程调用**。
     *
     * @return {时长ms, 是否估算, 宽, 高, 帧数}；认不出 / 读不到返回 null（UI 就不显示时长）
     *         <p>v1.10.457：多带一个帧数 —— 选择器那一行要显示 fps，而 fps = 帧数 / 时长。
     *         两条路（内存解析 / 流式解析）都只读容器块结构，**不解一帧像素**，代价与原来相同。
     */
    @org.jetbrains.annotations.Nullable
    static int[] probe(Context context, Uri uri, long id, long date) {
        int[] hit = cached(id, date);
        if (hit != null) {
            return hit;
        }
        byte[] head = readHead(context, uri);
        if (head == null || head.length < 8) {
            return null;
        }
        String sniff = MediaSniff.sniffBytes(head, head.length);
        int[] out;
        // v1.10.177：64KB 没读满 = 整个文件都在手里 → 走内存解析（最稳，规则一致）
        if (head.length < HEAD_BYTES) {
            out = parseBytes(sniff, head);
        } else {
            // 大文件：流式走块结构，内存固定 64KB，**没有大小上限**
            out = parseStream(context, uri, sniff);
            // v1.10.177：大文件走的是另一条路，必须留痕（成功/解不出都要能追）
            Diag.warn(context, "K07", "大文件时长 格式=" + sniff
                    + " 结果ms=" + (out == null ? -1 : out[0])
                    + " 估算=" + (out != null && out[1] != 0)
                    + " 尺寸=" + (out == null ? "-" : (out[2] + "x" + out[3])));
        }
        if (out != null) {
            remember(id, date, out);
        }
        return out;
    }

    /** 小文件（已整个在内存里）的解析：沿用原来的三个 info()，数字与导出侧完全一致。 */
    @org.jetbrains.annotations.Nullable
    private static int[] parseBytes(String sniff, byte[] bytes) {
        int[] out = null;
        if ("gif".equals(sniff)) {
            GifFrames.Meta meta = GifFrames.info(bytes);
            if (meta != null) {
                // 有「文件没写延时、我们按 100ms 补」的帧 → 整体标 ≈
                boolean estimated = meta.estimated || meta.filled > 0;
                out = new int[]{meta.totalDelayMs, estimated ? 1 : 0, meta.width, meta.height,
                        meta.frames};
            }
        } else if ("apng".equals(sniff)) {
            int[] info = ApngFrames.info(bytes);
            if (info != null) {
                out = new int[]{info[3], 0, info[0], info[1], info[2]};
            }
        } else if ("webp-anim".equals(sniff)) {
            int[] info = WebpAnimFrames.info(bytes);
            if (info != null) {
                out = new int[]{info[3], 0, info[0], info[1], info[2]};
            }
        }
        return out;
    }

    /** 大文件：交给流式解析（不解像素、不占大内存、无大小上限）。 */
    @org.jetbrains.annotations.Nullable
    private static int[] parseStream(Context context, Uri uri, String sniff) {
        if (!"gif".equals(sniff) && !"apng".equals(sniff) && !"webp-anim".equals(sniff)) {
            return null;
        }
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
            if (in == null) {
                return null;
            }
            return AnimProbe.probe(in, sniff);
        } catch (Throwable th) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /** 只读头部 {@link #HEAD_BYTES} 字节（读不满说明已经到文件尾）。 */
    @org.jetbrains.annotations.Nullable
    private static byte[] readHead(Context context, Uri uri) {
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
            if (in == null) {
                return null;
            }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(HEAD_BYTES);
            byte[] buf = new byte[1 << 16];
            while (out.size() < HEAD_BYTES) {
                int want = Math.min(buf.length, HEAD_BYTES - out.size());
                int n = in.read(buf, 0, want);
                if (n <= 0) {
                    break;
                }
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (Throwable th) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    // ------------------------------------------------------------------ 文案

    static String clarityText(int width, int height) {
        int shortEdge = Math.min(width, height);
        if (shortEdge <= 0) {
            return null;
        }
        if (shortEdge >= 2100 && shortEdge <= 2200) {
            return "4K";
        }
        return shortEdge + "P";
    }

    /** 合成一行：`2.4s · 480P`；缺一项就只显示另一项；都没有返回 null。 */
    @org.jetbrains.annotations.Nullable
    /**
     * v1.10.458（用户指定格式）：`1.6s 10fps 240p` —— **三个数字 + 单位，中间只留一个空格**。
     *
     * <p>为什么去掉「·」和「×」：一格只有一百多 px 宽，分隔符本身要吃掉两三个字符，
     * 而「1.6s / 10fps / 240p」的每个字符都是信息。空格间距比任何符号都小。
     *
     * <p>缺项就跳过（老文件读不到帧数 → 只显示 ms 和清晰度），**不显示占位符**，
     * 也不为了凑格式把加载失败写成 0。
     */
    static String label(int ms, int fpsX10, int width, int height) {
        String a = secondsText(ms);
        String b = fpsText(fpsX10);
        String c = clarityText(width, height);
        StringBuilder sb = new StringBuilder();
        if (a != null) {
            sb.append(a);
        }
        if (b != null) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(b);
        }
        if (c != null) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(c);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /**
     * 时长文本：**秒，最多一位小数**（`1.6s` / `4.7s` / `2s`）。
     *
     * <p>整数秒不带 `.0`（`2000ms → 2s`），因为是选择器里的一格窄条，多一个字符就要缩字号。
     * 不足 0.1s 的极端文件显示 `<0.1s` —— **不许四舍五入成 0s**，那是在说谎。
     */
    @org.jetbrains.annotations.Nullable
    static String secondsText(int ms) {
        if (ms <= 0) {
            return null;
        }
        int tenths = Math.round(ms / 100.0f);
        if (tenths <= 0) {
            return "<0.1s";
        }
        if (tenths % 10 == 0) {
            return (tenths / 10) + "s";
        }
        return String.format(java.util.Locale.US, "%.1fs", tenths / 10.0f);
    }

    /**
     * 帧率文本：帧率 ×10 整数化，整档省略小数（`10fps` / `24fps`），
     * 非整档保留一位（`33.3fps`）。**不显示 ≈** —— 它是由时长和帧数算出来的，
     * 那个 ≈ 已经在时长上表达过了。
     */
    @org.jetbrains.annotations.Nullable
    static String fpsText(int fpsX10) {
        if (fpsX10 <= 0) {
            return null;
        }
        if (fpsX10 % 10 == 0) {
            return (fpsX10 / 10) + "fps";
        }
        return String.format(java.util.Locale.US, "%.1ffps", fpsX10 / 10.0f);
    }

    /** v1.10.457：由 {时长ms, 估算, 宽, 高, 帧数} 反算帧率 ×10（读不到就是 0）。 */
    static int fpsX10Of(@org.jetbrains.annotations.Nullable int[] meta) {
        if (meta == null || meta.length < 5 || meta[0] <= 0 || meta[4] <= 0) {
            return 0;
        }
        long fpsX10 = Math.round(meta[4] * 10000.0 / meta[0]);
        // 夹到合理范围：超出 1~120fps 的一律当作读不准（不显示，也不拿它算别的）
        if (fpsX10 < 10 || fpsX10 > 1200) {
            return 0;
        }
        return (int) fpsX10;
    }
}
