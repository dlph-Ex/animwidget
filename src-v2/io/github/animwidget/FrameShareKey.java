package io.github.animwidget;

/**
 * v1.10.481（P0-3「A：同素材共享解码 N→1」）：**共享帧的键 + 内容签名**（纯逻辑，可脱机自检）。
 *
 * <h3>为什么需要「键」而不是直接用文件路径</h3>
 * 每个组件都有自己的帧目录（`files/push/w<id>/<stamp>/`），所以两个组件用**同一个素材、
 * 同一套参数**时，帧文件是**两份字节完全相同**的副本 —— 路径不同，内容一样。
 * 按路径做键，等于永远不命中；按内容做键，才能「解一次、两路都用」。
 *
 * <h3>为什么不能直接用 {@code stamp}</h3>
 * `stamp = renderTag + "_" + contentTag + "_" + WxH`，而 {@code contentTag} 里含
 * **首帧文件的 mtime**（用来做「用户换了素材就重烘」的失效判据）。两个组件分别导入同一段视频，
 * mtime 必然不同 ⇒ stamp 不同 ⇒ 键不同 ⇒ 还是永远不命中。
 *
 * <p>所以键里改用「跨组件稳定」的身份，并且额外挂一个**逐帧内容签名**来替代 mtime 的失效作用：</p>
 * <pre>
 *   键 = renderTag | srcId（素材身份，不含 mtime） | 画布 WxH | 帧号/帧数 | 文件长度-前 2KB 的 FNV64
 * </pre>
 *
 * <p>「文件长度 + 前 2KB 指纹」是这套方案唯一的正确性保险：两段不同的视频在同一个帧号上
 * 长度和开头 2KB 都撞车的概率可以忽略；撞车了也只是「显示错帧」，不是崩溃。反过来说，
 * 只要签名不同，键就不同 —— 失败方向是**不命中**（慢 12ms），不是**错帧**。</p>
 */
final class FrameShareKey {

    /** 指纹只读文件头部这么多字节：够区分内容，又不增加可感知的 IO。 */
    static final int HEAD_BYTES = 2048;

    private FrameShareKey() {
    }

    /**
     * 拼键。返回 null = 这一帧不该走共享（参数不全时宁可不共享，也不要错键）。
     *
     * @param base  「跨组件稳定」的部分：renderTag | srcId | WxH（不含 mtime）
     * @param idx   帧号（会按 count 归一化：235 帧的动画里 237 就是 2）
     * @param count 帧数
     * @param len   这一帧文件长度（字节）
     * @param head  这一帧文件前 {@link #HEAD_BYTES} 字节的 FNV-1a 64 位
     */
    static String of(String base, int idx, int count, long len, long head) {
        if (base == null || base.isEmpty() || count <= 0 || len <= 0L) {
            return null;
        }
        int i = ((idx % count) + count) % count;
        StringBuilder sb = new StringBuilder(base.length() + 48);
        sb.append(base).append('|').append(i).append('/').append(count)
                .append('|').append(len).append('-').append(Long.toHexString(head));
        return sb.toString();
    }

    /**
     * FNV-1a 64 位。为什么不用 `String.hashCode`：那个是 32 位，而且只看字符；
     * 这里要的是「内容改一个字节就变」，FNV-1a 的雪崩性足够且零依赖。
     */
    static long fnv1a(byte[] data, int len) {
        if (data == null) {
            return 0L;
        }
        long h = 0xcbf29ce484222325L;
        int n = Math.min(len, data.length);
        for (int i = 0; i < n; i++) {
            h ^= (long) (data[i] & 0xFF);
            h *= 0x100000001b3L;
        }
        return h;
    }
}
