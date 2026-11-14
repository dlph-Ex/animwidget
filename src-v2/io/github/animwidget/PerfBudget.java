package io.github.animwidget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Locale;

/**
 * v1.10.495：**剩余性能空间**的估算与上屏（v1.10.496 起改成一张卡片）。
 *
 * <p>文案只说「我们这一侧还能不能再装一路」，因为它只算得到我们自己：解码（71 Mpx/s/核，实测）、
 * 推送（1.5ms/次，实测）、以及同素材共享带来的折算。桌面侧能不能画出来我们测不到，
 * 所以不做承诺（这一点在 {@link PerfMath} 的类注释里写死了）。</p>
 *
 * <p>计算在**后台线程**跑：要遍历所有组件的画布尺寸（`WidgetRender.canvasSize` 会走
 * `getAppWidgetOptions` 的 IPC），放在主线程会让这一行变成卡顿源。</p>
 *
 * <p>v1.10.496 的两处变化：</p>
 * <ul>
 *   <li>上屏目标是**一整张卡**（百分比 / 分段表 / 两枚胶囊 / 一行结论），不再是一个
 *       {@code TextView} —— 所以 {@link #refresh} 收的是卡片根视图，由 {@link Model} 供数；</li>
 *   <li>所有文案走 {@link I18nActivity#s(int, Object...)}：{@code applyLocaleInPlace()} 之后
 *       Activity 自己的 resources 还停在旧 locale，只有那个入口拿得到「当前界面语言」
 *       （v1.10.200 踩过一次，见 {@code I18nActivity} 的类注释）。</li>
 * </ul>
 */
final class PerfBudget {

    private PerfBudget() {
    }

    private static final Handler UI = new Handler(Looper.getMainLooper());

    /**
     * 只让**最后一次**估算上屏。
     *
     * <p>刷新入口有三个（onResume / 换肤 / 换语言），换肤那一路还会被 500ms 深色轮询反复触发，
     * 后台线程算完的顺序不保证与发起顺序一致。没有这道闸的话，先发后到的那次会把新数字盖回旧数字，
     * 表现是「数字偶尔跳回上一次的值」—— 这种闪一下的错最难被复现，所以直接按序号丢弃。</p>
     */
    private static final java.util.concurrent.atomic.AtomicInteger SEQ =
            new java.util.concurrent.atomic.AtomicInteger();

    /** 画布未知时的候选口径（与 {@link PerfMath} 的默认值一致：720p·50fps）。 */
    private static final int DEFAULT_W = 1280;
    private static final int DEFAULT_H = 720;
    private static final int DEFAULT_FPS = 50;

    /** 「还能加几个」到这个数以上就不报具体数字了（否则会写出 PerfMath 的 99 封顶）。 */
    private static final int PLENTY = 20;

    /**
     * 一次估算的结果（纯数据，无 View 引用 —— 后台线程只造它，主线程才碰界面）。
     */
    static final class Model {
        /** 剩余百分比 0~100。 */
        final int percent;
        /** 已用核数。 */
        final double used;
        /** 可用核数（预算）。 */
        final double budget;
        /** 参与统计的组件路数（播放中）。 */
        final int lanes;
        /** 还能再加几个「同参数」的组件（0 表示到顶）。 */
        final int more;
        /** 单路候选本身就已超出余量。 */
        final boolean tooHeavy;
        /** 候选单路的核数。 */
        final double candidate;
        /** 0 = 宽裕，1 = 偏紧，2 = 到顶/超预算（百分比与分段表共用这一档配色）。 */
        final int tone;
        /** 候选口径（画布与帧率），文案里要写出来 —— 「还能加 3 个」不写清楚是什么，等于没说。 */
        final int specW;
        final int specH;
        final int specFps;

        Model(int percent, double used, double budget, int lanes, int more, boolean tooHeavy,
              double candidate, int tone, int specW, int specH, int specFps) {
            this.percent = percent;
            this.used = used;
            this.budget = budget;
            this.lanes = lanes;
            this.more = more;
            this.tooHeavy = tooHeavy;
            this.candidate = candidate;
            this.tone = tone;
            this.specW = specW;
            this.specH = specH;
            this.specFps = specFps;
        }
    }

    /**
     * 异步刷新整张卡。{@code root} 是 {@code R.id.ll_perf_headroom} 那张卡的根视图；
     * 里面的子控件每次重新 find（4 个，比缓存一堆字段再造一个「谁持有 Activity」的坑便宜）。
     */
    static void refresh(@Nullable final I18nActivity host, @Nullable final View root, final int selfId) {
        if (host == null || root == null) {
            return;
        }
        final Context app = host.getApplicationContext();
        final int seq = SEQ.incrementAndGet();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Model model;
                try {
                    model = measure(app, selfId);
                } catch (Throwable th) {
                    model = null;          // 算不出来就不编数字：界面显示「暂不估算」
                }
                final Model out = model;
                UI.post(new Runnable() {
                    @Override
                    public void run() {
                        if (SEQ.get() != seq) {
                            return;        // 有更新的一次在路上，这次的结果已经过期
                        }
                        try {
                            apply(host, root, out);
                        } catch (Throwable ignored) {
                        }
                    }
                });
            }
        }, "perf-budget").start();
    }

    /**
     * 同步估算（后台线程调；脱机自检/日志也可用）。
     *
     * <p>口径：遍历所有组件（含多格组件的每一格），同「素材 + 画布 + 帧率」的只算一次解码
     * （这就是 481 起共享解码的真实效果），推送每路都算。</p>
     */
    @NotNull
    static Model measure(@NotNull Context context, int selfId) {
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        int[] ids = MultiCell.allIds(context);
        double used = 0.0;
        int lanes = 0;
        HashSet<String> decoded = new HashSet<>();
        int selfW = 0;
        int selfH = 0;
        int selfFps = 0;
        for (int id : ids) {
            WidgetConfig cfg = WidgetStore.INSTANCE.load(context, id);
            if (cfg == null) {
                continue;
            }
            int[] canvas = WidgetRender.INSTANCE.canvasSize(context, id);
            int w = canvas != null && canvas.length >= 2 ? canvas[0] : 0;
            int h = canvas != null && canvas.length >= 2 ? canvas[1] : 0;
            int fps = cfg.getFps();
            if (w <= 0 || h <= 0 || fps <= 0) {
                continue;               // 画布还没定（桌面还没报尺寸）→ 这一路没有可算的账
            }
            lanes++;
            String key = cfg.getSourceUri() + "|" + cfg.getQuality() + "|" + w + "x" + h + "|" + fps;
            boolean shared = !decoded.add(key);           // 同素材同画布同帧率：解码只算一次
            used += PerfMath.decodeCores((long) w * h, fps, shared) + PerfMath.pushCores(fps);
            if (id == selfId) {
                selfW = w;
                selfH = h;
                selfFps = fps;
            }
        }
        // 候选 = 这一路自己；新建（还没保存）时按默认口径 720p·50fps 估
        int specW = selfW > 0 ? selfW : DEFAULT_W;
        int specH = selfH > 0 ? selfH : DEFAULT_H;
        int specFps = selfFps > 0 ? selfFps : DEFAULT_FPS;
        double candidate = PerfMath.decodeCores((long) specW * specH, specFps, false)
                + PerfMath.pushCores(specFps);
        PerfMath.Verdict v = PerfMath.verdict(used, cores, candidate);

        int tone;
        if (v.tooHeavy || v.more <= 0) {
            tone = 2;                   // 到顶 / 单路就放不下：这一档必须刺眼，否则用户还要再点一次
        } else if (v.percent < 25) {
            tone = 1;                   // 偏紧
        } else {
            tone = 0;
        }
        return new Model(v.percent, v.used, v.budget, lanes, v.more, v.tooHeavy, candidate,
                tone, specW, specH, specFps);
    }

    /** 主线程：把 {@link Model} 摊到卡的四个位置（百分比 / 分段表 / 两枚胶囊 / 结论行）。 */
    private static void apply(@NotNull I18nActivity host, @NotNull View root, @Nullable Model m) {
        ThemeColors c = ThemeManager.INSTANCE.colors(host);
        View pctView = root.findViewById(R.id.tv_perf_pct);
        View meterView = root.findViewById(R.id.perf_meter);
        View usedView = root.findViewById(R.id.tv_perf_used);
        View lanesView = root.findViewById(R.id.tv_perf_lanes);
        View detailView = root.findViewById(R.id.tv_perf_headroom);

        if (m == null) {
            if (pctView instanceof TextView) {
                ((TextView) pctView).setText(host.s(R.string.perf_pct_loading));
                ((TextView) pctView).setTextColor(c.getTextMuted());
            }
            if (meterView instanceof PerfMeterView) {
                ((PerfMeterView) meterView).setPercent(-1);
                ((PerfMeterView) meterView).setTone(0);
            }
            if (usedView instanceof TextView) {
                ((TextView) usedView).setText(host.s(R.string.perf_chip_na));
            }
            if (lanesView instanceof TextView) {
                ((TextView) lanesView).setText(host.s(R.string.perf_chip_na));
            }
            if (detailView instanceof TextView) {
                ((TextView) detailView).setText(host.s(R.string.perf_unknown));
                ((TextView) detailView).setTextColor(c.getTextMuted());
            }
            return;
        }

        final int accent = m.tone == 2 ? c.getError()
                : (m.tone == 1 ? c.getTertiary() : c.getPrimary());
        if (pctView instanceof TextView) {
            ((TextView) pctView).setText(m.percent + "%");
            ((TextView) pctView).setTextColor(accent);
        }
        if (meterView instanceof PerfMeterView) {
            ((PerfMeterView) meterView).setPercent(m.percent);
            ((PerfMeterView) meterView).setTone(m.tone);
        }
        String spec = m.specW + "×" + m.specH + "·" + m.specFps + "fps";
        if (usedView instanceof TextView) {
            ((TextView) usedView).setText(host.s(R.string.perf_chip_used,
                    one(m.used), one(m.budget)));
        }
        if (lanesView instanceof TextView) {
            ((TextView) lanesView).setText(host.s(R.string.perf_chip_lanes, Integer.valueOf(m.lanes)));
        }
        if (detailView instanceof TextView) {
            String line;
            if (m.tooHeavy) {
                line = host.s(R.string.perf_detail_over, spec, one(m.candidate));
            } else if (m.more <= 0) {
                line = host.s(R.string.perf_detail_full, spec);
            } else if (m.more >= PLENTY) {
                // PerfMath 把 more 夹在 99：直接写「还能再加 99 个」会被当成 bug。
                line = host.s(R.string.perf_detail_plenty, spec);
            } else {
                line = host.s(R.string.perf_detail_more, Integer.valueOf(m.more), spec);
            }
            ((TextView) detailView).setText(line);
            ((TextView) detailView).setTextColor(m.tone == 2 ? c.getError() : c.getTextMuted());
        }
    }

    /** 核数一律一位小数（0.9 / 3.6），不跟着系统语言变小数点 —— 这些是量纲，不是货币。 */
    private static String one(double value) {
        return String.format(Locale.US, "%.1f", Double.valueOf(value));
    }
}
