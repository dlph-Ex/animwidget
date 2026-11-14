package io.github.animwidget;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;

import java.security.MessageDigest;
import java.util.Arrays;

/**
 * v1.10.513：**发行身份自检**（需求①：应用内签名校验）。
 *
 * <p>口径（与 {@code SECURITY-SIGNATURE-ANTITAMPER.md} 一致）：</p>
 * <ul>
 *   <li>只认「用我们这把钥匙签出来的包」；其它签名（重打包 / 重签名 / 二次发行）→ **限制模式**；</li>
 *   <li>**不闪退、不 throw、不弹"检测到破解"** —— 失败路径只是一次返回码；</li>
 *   <li>结果按进程缓存一次（{@link #evaluate}），各调用点只读缓存。</li>
 * </ul>
 *
 * <p>四条判据：</p>
 * <ol>
 *   <li><b>A</b> 包名 == {@link #PKG}（冒名者常改包名，而改包名必须改清单 → 必然重签名）；</li>
 *   <li><b>B</b> 签名者恰好 1 个（多签名者 / 追加签名是绕过单值比较的老手法）；</li>
 *   <li><b>C</b> 没有密钥轮换历史（{@code hasPastSigningCertificates()}）；</li>
 *   <li><b>D</b> 那个签名者的证书 SHA-256 与 {@link #W} 逐字节相等。</li>
 * </ol>
 *
 * <p><b>为什么不自己算 dex 的 CRC/哈希</b>：v2 签名覆盖整个 APK 的字节流，插广告必然改 dex、
 * 改 dex 必然破坏签名、装得上就必然重签名 —— 所以"验签"本身就是完整性校验，再叠一层 CRC
 * 只是多一个可 patch 的点（详见规格文档 §2.2）。</p>
 */
final class Integrity {

    /** 判定结果：0 = 原版。其余值各自对应一个诊断代号。 */
    static final int OK = 0;
    /** 签名者只有一个，但不是我们的证书 → K52。 */
    static final int BAD_CERT = 1;
    /** 包名 / 签名者数量 / 轮换历史异常 → K53。 */
    static final int BAD_SIGNERS = 2;
    /**
     * v1.10.513：**读不到签名**（只可能出现在 API 26/27：{@code GET_SIGNATURES} 为空/抛异常）。
     *
     * <p>为什么它必须和"异常"分开：26/27 上"读不到"是**平台行为差异**，不是攻击特征，
     * 而且我们自己的包是 v2-only —— 如果 26/27 读不到，那么**所有正版用户**都读不到，
     * 判异常就会 100% 误限制。所以这一档**不限制**，只记 K54 供我们判断。
     * 26/27 上真被改包的往往是 apktool + jarsigner 产物（带 v1 签名 → 反而读得到 → 走 K52）。</p>
     */
    static final int UNREADABLE = 3;

    private static final String PKG = "io.github.animwidget";

    /**
     * 发行证书 SHA-256（8 个大端 int，等价于
     * {@code 5E:9B:78:5C:93:2E:49:AD:62:1E:BE:FB:31:FB:14:66:59:2F:A9:62:88:29:55:A6:9C:EB:89:5D:04:EE:F4:D3}）。
     *
     * <p>拆成 int 而不是写一个 64 字符常量：这是提高"搜字符串就改"的成本，**不是**安全边界。
     * 真正的边界是"改包必须重签名"这一条物理事实。</p>
     */
    private static final int[] W = {
            0x5e9b785c, 0x932e49ad, 0x621ebefb, 0x31fb1466,
            0x592fa962, 0x882955a6, 0x9ceb895d, 0x04eef4d3
    };

    /** -1 = 还没算过；0..2 = 缓存结果（volatile：多线程读，算一次）。 */
    private static volatile int state = -1;

    private Integrity() {
    }

    /** 算一次并缓存。重复调用是廉价的。 */
    static int evaluate(Context c) {
        int s = state;
        if (s >= 0) {
            return s;
        }
        try {
            s = compute(c);
        } catch (Throwable t) {
            // 任何意外都按"异常"处理（保守）。想让极端 ROM 上的正版用户放行，把这里改成 OK。
            s = BAD_SIGNERS;
        }
        state = s;
        return s;
    }

    /**
     * 是否必须进限制模式。首次调用会懒求值。
     *
     * <p>只有"确认证书不对（K52）"和"确认签名结构异常（K53）"才限制；
     * {@link #UNREADABLE} 放行（见那个常量的注释）—— 这一句就是"绝不误限制正版"的落点，
     * 门禁 [33] 会盯着它不许被改回 {@code != OK}。</p>
     */
    static boolean restricted(Context c) {
        int s = evaluate(c);
        return s == BAD_CERT || s == BAD_SIGNERS;
    }

    /** 只给"关于页"显示用：当前包的证书 SHA-256（小写十六进制，无分隔），失败返回 null。 */
    @org.jetbrains.annotations.Nullable
    static String fingerprint(Context c) {
        try {
            Signature[] signers = signers(c);
            if (signers == null || signers.length < 1 || signers[0] == null) {
                return null;
            }
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(signers[0].toByteArray());
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                int v = b & 0xFF;
                if (v < 16) {
                    sb.append('0');
                }
                sb.append(Integer.toHexString(v));
            }
            return sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * v1.10.516（用户反馈「显示不全」）：**完整**点分指纹 {@code 5E:9B:...:F4:D3}（95 字符）。
     *
     * <p>515 只在关于页显示前 8 组，用户拿不到可以逐段核对的全串（和 Release 说明对不上）；
     * 现在整串交给关于页，由它排成两行、点一下可复制。</p>
     */
    @org.jetbrains.annotations.Nullable
    static String fingerprintFull(Context c) {
        String hex = fingerprint(c);
        if (hex == null || hex.length() < 64) {
            return null;
        }
        StringBuilder sb = new StringBuilder(95);
        for (int i = 0; i < 32; i++) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(hex.substring(i * 2, i * 2 + 2).toUpperCase(java.util.Locale.US));
        }
        return sb.toString();
    }

    /**
     * v1.10.515：**正版也打**的那一行日志要用的短指纹（前 8 字节，大写无分隔）。
     *
     * <p>为什么要它：K52/K53 只在"不是原版"时出现，正版用户看不到任何东西就没法判断
     * "判定到底跑了没有"。K51 这一行是"判定执行过且通过"的唯一正证据。</p>
     */
    @org.jetbrains.annotations.Nullable
    static String headHex(Context c) {
        String hex = fingerprint(c);
        if (hex == null || hex.length() < 16) {
            return null;
        }
        return hex.substring(0, 16).toUpperCase(java.util.Locale.US);
    }

    /** 取"当前 APK 的实际签名者"：28+ 与 26/27 两条路。 */
    @org.jetbrains.annotations.Nullable
    private static Signature[] signers(Context c) {
        PackageManager pm = c.getPackageManager();
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                return Api28.contentSigners(pm, PKG);
            }
            PackageInfo pi = pm.getPackageInfo(PKG, PackageManager.GET_SIGNATURES);
            return pi != null ? pi.signatures : null;
        } catch (Exception e) {
            // v1.10.513：getPackageInfo 抛的是**受检**的 NameNotFoundException ——
            // 这里必须就地接住（调用方 compute() 把它归到 K53），不能往上抛（失败路径不许抛）。
            return null;
        }
    }

    /**
     * API 28+ 专用（{@code SigningInfo} 是新类）：单独放一个内部类，26/27 上这个类根本不会被加载，
     * 免得老 ROM 的校验器在解析 {@code compute()} 时提前碰到它。
     */
    private static final class Api28 {

        private Api28() {
        }

        @org.jetbrains.annotations.Nullable
        static Signature[] contentSigners(PackageManager pm, String pkg) {
            PackageInfo pi;
            try {
                pi = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES);
            } catch (Exception e) {
                return null;
            }
            if (pi == null) {
                return null;
            }
            SigningInfo si = pi.signingInfo;
            if (si == null) {
                return null;
            }
            // B：多签名者 → 视为异常（此时 getSigningCertificateHistory() 也会返回 null）
            if (si.hasMultipleSigners()) {
                return null;
            }
            // C：轮换过密钥 → 视为异常。
            // hasPastSigningCertificates() 是这件事的**权威判据**；下面那两行是冗余复核，
            // 只在"有历史"时才会取到长度 >1 的数组（framework 行为见 SECURITY 文档的字节码证据）。
            if (si.hasPastSigningCertificates()) {
                return null;
            }
            Signature[] hist = si.getSigningCertificateHistory();
            if (hist != null && hist.length > 1) {
                return null;
            }
            return si.getApkContentsSigners();
        }
    }

    private static int compute(Context c) {
        // A：包名
        if (!PKG.equals(c.getPackageName())) {
            return BAD_SIGNERS;
        }
        Signature[] signers;
        try {
            signers = signers(c);
        } catch (Exception e) {
            // 取签名抛异常：26/27 上归 UNREADABLE（放行 + K54），28+ 归 BAD_SIGNERS。
            return Build.VERSION.SDK_INT >= 28 ? BAD_SIGNERS : UNREADABLE;
        }
        // B（两条取法共用）：必须恰好一个签名者。
        // 26/27 上"根本没有签名信息"→ UNREADABLE（放行，别误限制）；28+ → BAD_SIGNERS。
        if (signers == null || signers.length == 0) {
            return Build.VERSION.SDK_INT >= 28 ? BAD_SIGNERS : UNREADABLE;
        }
        if (signers.length != 1 || signers[0] == null) {
            return BAD_SIGNERS;
        }
        return sameCert(signers[0].toByteArray()) ? OK : BAD_CERT;
    }

    private static boolean sameCert(byte[] certDer) {
        byte[] want = new byte[32];
        byte[] got = null;
        try {
            for (int i = 0; i < 8; i++) {
                int v = W[i];
                want[i * 4] = (byte) (v >>> 24);
                want[i * 4 + 1] = (byte) (v >>> 16);
                want[i * 4 + 2] = (byte) (v >>> 8);
                want[i * 4 + 3] = (byte) v;
            }
            got = MessageDigest.getInstance("SHA-256").digest(certDer);
            return MessageDigest.isEqual(want, got);
        } catch (Exception e) {
            return false;
        } finally {
            Arrays.fill(want, (byte) 0);
            if (got != null) {
                Arrays.fill(got, (byte) 0);
            }
        }
    }
}
