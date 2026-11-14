package io.github.animwidget;

import android.app.Application;

/**
 * v1.10.513：进程入口（清单 {@code android:name=".App"}）。
 *
 * <p>只做一件事：进程起来最早的那一刻把发行身份判定算一次并缓存（{@link Integrity#evaluate}），
 * 顺带把结果落一条诊断记录。之后所有调用点读的都是这份缓存 —— 判定只跑一次，
 * 不让"多点校验"变成"多次开销"。</p>
 *
 * <p>失败**不阻断启动**（需求口径：B 限制模式）：真正限制能力的地方在
 * {@code ConfigActivity.startExport}、{@code WidgetRender.render} 与 {@code WidgetProvider.onUpdate}，
 * 它们各自读同一个缓存。</p>
 *
 * <p>v1.10.515：四个结果各有一个**独立的小方法**只负责打日志。这不是洁癖 ——
 * 判断代号体检（{@code scripts/diag-audit.py}）抓调用点时只看「代号 + 其后 160 字符」，
 * 四条 {@code Diag.warn} 挤在一个 if/else 链里时，后三条会被第一条的片段吃掉，
 * 于是它们会被误判成"全表有、源码没有"的幽灵条目。拆开就没有这个窗口。</p>
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        int r = Integrity.evaluate(this);
        if (r == Integrity.OK) {
            logGenuine();
        } else if (r == Integrity.BAD_CERT) {
            logCertMismatch();
        } else if (r == Integrity.UNREADABLE) {
            logUnreadable();
        } else {
            logSignerAnomaly();
        }
    }

    /**
     * K51：原版包每次启动一条（**正证据**）。
     *
     * <p>没有它，正版用户只能靠"K52 没出现"来推断判定跑过，而"没出现"既可能是通过、
     * 也可能是这段代码根本没执行 —— 两者在证据上不能区分。带上证书前 8 字节之后，
     * 用户拿它与 Release 说明里的指纹逐段对照，一眼就能确认"这份是官方包 + 自检真的跑了"。</p>
     */
    private void logGenuine() {
        String head = Integrity.headHex(this);
        Diag.warn(this, "K51", "发行身份 OK 证书=" + (head == null ? "?" : head));
    }

    /**
     * K52：验到了证书，而且**确定不是**我们那把钥匙（改包重签名最典型的一种）。
     *
     * <p>这一条一旦出现，限制模式就会生效：界面只剩基础功能（不向桌面推帧、不允许导出）。
     * 它与 K53 的分工必须保持：K52 是"确定不对"，K53 是"没法确认是我们"。</p>
     */
    private void logCertMismatch() {
        Diag.warn(this, "K52", "签名证书不符，限制模式");
    }

    /**
     * K54：只可能出现在 API 26/27（那里 {@code GET_SIGNATURES} 可能取不到签名者）。
     *
     * <p>这一档**不限制功能**（否则所有正版老系统用户都会被误限制），它只是留个记录，
     * 告诉我们这台机器上读不到签名。看到它不用做任何事，但看到 K52/K53 就有事了。</p>
     */
    private void logUnreadable() {
        Diag.warn(this, "K54", "签名不可读(26/27)，放行");
    }

    /**
     * K53：情况"不正常"而不是"确定不对" —— 包名被改、签名者不止一个、轮换过密钥。
     *
     * <p>同样进限制模式。它和 K52 必须能在日志页分开搜，所以两条各自一个方法、各自一个代号。</p>
     */
    private void logSignerAnomaly() {
        Diag.warn(this, "K53", "包名/签名者异常，限制模式");
    }
}
