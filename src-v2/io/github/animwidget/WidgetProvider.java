package io.github.animwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.widget.RemoteViews;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* compiled from: WidgetProvider.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��4\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0010\b\n��\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0015\n\u0002\b\u0003\u0018�� \u00122\u00020\u0001:\u0001\u0012B\u0005¢\u0006\u0002\u0010\u0002J(\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0016J\u0018\u0010\r\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u000e\u001a\u00020\u000fH\u0016J\u0010\u0010\u0010\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0016J \u0010\u0011\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\u000e\u001a\u00020\u000fH\u0016"}, d2 = {"Lio/github/animwidget/WidgetProvider;", "Landroid/appwidget/AppWidgetProvider;", "()V", "onAppWidgetOptionsChanged", "", "context", "Landroid/content/Context;", "appWidgetManager", "Landroid/appwidget/AppWidgetManager;", "appWidgetId", "", "newOptions", "Landroid/os/Bundle;", "onDeleted", "appWidgetIds", "", "onDisabled", "onUpdate", "Companion"})
/* loaded from: classes.jar:io/github/animwidget/WidgetProvider.class */
public final class WidgetProvider extends AppWidgetProvider {

    @NotNull
    public static final Companion Companion = new Companion(null);

    /**
     * v1.10.121（反馈「播放后比例明显收缩」）：**尺寸变更静置 500ms 再采信**。
     *
     * vivo 桌面在拖拽组件时会连着送来好几个尺寸（日志实测：182x260dp → 248x260dp 相隔 1 秒），
     * 我们每来一个就按它合成、推送 -> 画面会出现一次「变窄再变回来」的收缩。
     * 这里攒一下：500ms 内又来了新尺寸就把上一次的作废，只按最后那个尺寸重推。
     */
    private static final android.os.Handler SIZE_HANDLER =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private static final java.util.HashMap<Integer, Runnable> SIZE_PENDING = new java.util.HashMap<>();
    // v1.10.287：500 → 150ms。松手后到“开始烘新画布”之间只剩这一段去抖；
    // 单帧（静态图）预热已不再另等 800ms（见 WidgetPrewarm.ensure），多帧仍由预热自己的
    // SETTLE_MS 挡住拖动期的连开多批，所以这里可以放心缩短。
    // 若日志里一次拖动出现 >3 条 W22「尺寸变更」，说明桌面送尺寸比 150ms 更密，再调回 250。
    private static final long SIZE_SETTLE_MS = 150L;

    /** v1.10.132：options 全量只打一次/组件 —— warnThrottled 是**全局单槽**，紧跟着 W22 打必被吞。 */
    private static final java.util.HashSet<Integer> OPTIONS_DUMPED = new java.util.HashSet<>();

    @Override // android.appwidget.AppWidgetProvider
    public void onUpdate(@NotNull Context context, @NotNull AppWidgetManager appWidgetManager, @NotNull int[] appWidgetIds) {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(appWidgetManager, "appWidgetManager");
        Intrinsics.checkNotNullParameter(appWidgetIds, "appWidgetIds");
        // v1.8.11：系统可能为这个小部件广播拉起一个纯净进程，崩溃钩子补装（幂等）。
        CrashLog.install(context);
        // A02：进程活着走到这 = provider/广播链路没死（v1.8.13，同步 logcat）
        Diag.warn(context, "A02", "onUpdate 进入, 组件数=" + appWidgetIds.length);
        // v1.10.513（需求①·限制模式）：非原版签名不刷新桌面、也不拉起播放服务。
        // 放在 A02 之后：日志里仍能看到"系统叫醒过我们"，只是这一拍什么都不做。
        if (Integrity.restricted(context)) {
            return;
        }
        for (int widgetId : appWidgetIds) {
            try {
                WidgetConfig config = WidgetStore.INSTANCE.load(context, widgetId);
                if (config != null) {
                    WidgetRender.INSTANCE.render(context, appWidgetManager, config, 0, null);
                } else {
                    showPlaceholder(context, appWidgetManager, widgetId);
                }
            } catch (Throwable th) {
                // 广播里抛异常 = 进程死（闪退），单个组件坏了不能拖死全部
                Diag.warn(context, "W03", "onUpdate 组件 " + widgetId + " 异常: "
                        + th.getClass().getSimpleName()
                        + (th.getMessage() != null ? "/" + th.getMessage() : ""));
                CrashLog.save(context, Thread.currentThread(), th);
            }
        }
        PlaybackService.Companion.start(context);
    }

    /**
     * 还没有配置的组件：显示应用图标，点一下进主界面挑素材，而不是留一块看不见的空白。
     *
     * v1.10.124（反馈「添加为组件后，会有一帧缩小组件整体比例」）：这里原来用 {@code widget_media}
     * 的 **centerCrop** —— 占位图铺满整格；而配好之后的真实帧是「素材形状」的卡片（更矮），
     * 于是保存完成的那一下，画面从「铺满」跳到「卡片」，看着就是**整体缩了一圈**。
     * 现在占位也用 fitCenter（{@code widget_media_fit}），占位与正式内容形状一致，不会再跳。
     */
    private final void showPlaceholder(Context context, AppWidgetManager manager, int widgetId) {
        try {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_media_fit);
            views.setImageViewResource(R.id.widget_image, R.drawable.widget_preview);
            views.setOnClickPendingIntent(R.id.widget_image, WidgetRender.INSTANCE.contentIntent(context));
            manager.updateAppWidget(widgetId, views);
            // v1.10.279：记下"桌面此刻挂的是占位图"——下一拍 render 必须当场合成一帧顶掉它，
            // 不许走"这拍不推"（那会把这张图一直挂在桌面上，直到预热完成）
            WidgetUriPush.notePlaceholder(widgetId);
        } catch (Throwable th) {
        }
    }

    @Override // android.appwidget.AppWidgetProvider
    public void onAppWidgetOptionsChanged(@NotNull Context context, @NotNull AppWidgetManager appWidgetManager, int appWidgetId, @NotNull Bundle newOptions) {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(appWidgetManager, "appWidgetManager");
        Intrinsics.checkNotNullParameter(newOptions, "newOptions");
        CrashLog.install(context);
        final Context app = context.getApplicationContext();
        final int id = appWidgetId;
        Runnable pending;
        synchronized (SIZE_PENDING) {
            pending = SIZE_PENDING.remove(Integer.valueOf(id));
        }
        if (pending != null) {
            SIZE_HANDLER.removeCallbacks(pending);
        }
        Runnable job = new Runnable() {
            @Override
            public void run() {
                synchronized (SIZE_PENDING) {
                    SIZE_PENDING.remove(Integer.valueOf(id));
                }
                applySizeChange(app, id);
            }
        };
        synchronized (SIZE_PENDING) {
            SIZE_PENDING.put(Integer.valueOf(id), job);
        }
        SIZE_HANDLER.postDelayed(job, SIZE_SETTLE_MS);
    }

    /** v1.10.121：尺寸静置之后才真正采信（见 {@link #SIZE_SETTLE_MS}）。 */
    private static void applySizeChange(@NotNull Context context, int widgetId) {
        try {
            // v1.10.57：尺寸变了 → 先作废 3 秒的尺寸缓存，否则这一下还是按旧尺寸合成
            WidgetRender.INSTANCE.invalidateSize(widgetId);
            // v1.10.194：画布固定后帧的 URI 不会再变，必须清掉「同一张不重推」的去重记录，
            // 否则 resize 这一次根本不推 —— 桌面会停在旧格子渲染的那一版上被裁。
            WidgetUriPush.forgetLast(widgetId);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            WidgetConfig config = WidgetStore.INSTANCE.load(context, widgetId);
            if (config == null) {
                return;
            }
            // v1.10.121：把「格子比例 / 素材比例 / 会留多少黑边」直接写进日志 ——
            // 用户看到的「比例收缩」到底是格子变了、还是原始比例模式的留边，一行就能分清。
            // v1.10.131（C1）：把桌面塞进来的 options 全量打出来（拆成独立方法，见下）
            dumpOptions(context, manager, widgetId);
            // v1.10.123：**接当前播放位置**，不再回到第 0 帧（否则拖大一下就像视频「重头又快进了一遍」）
            int at = PlaybackService.lastIndex(widgetId);
            Diag.warnThrottled(context, "W22", "尺寸变更 → 当前位置 id=" + widgetId
                    + " 帧=" + at + " " + describeFit(context, manager, config));
            WidgetRender.INSTANCE.render(context, manager, config, at, null);
        } catch (Throwable th) {
            Diag.warn(context, "W03", "尺寸变更组件 " + widgetId + " 异常: "
                    + th.getClass().getSimpleName()
                    + (th.getMessage() != null ? "/" + th.getMessage() : ""));
            CrashLog.save(context, Thread.currentThread(), th);
        }
    }

    /**
     * v1.10.131（C1）：**把桌面塞进来的 options 全量打出来**。
     *
     * vivo 不给 OPTION_APPWIDGET_SIZES，我们只能 dp×density 估；但各家桌面偶尔会塞私有键
     * （如 xxx_widget_width）。这一行是「查情报」的入口 —— 看到键名才知道能不能用。
     * 单独成方法是为了让字节码结构简单（ASM 分析器对「循环里套 try/catch」会误报 bad）。
     */
    private static void dumpOptions(@NotNull Context context, @NotNull AppWidgetManager manager, int widgetId) {
        synchronized (OPTIONS_DUMPED) {
            if (OPTIONS_DUMPED.contains(Integer.valueOf(widgetId))) {
                return;
            }
            OPTIONS_DUMPED.add(Integer.valueOf(widgetId));
        }
        Bundle opt = null;
        try {
            opt = manager.getAppWidgetOptions(widgetId);
        } catch (Throwable ignored) {
            return;
        }
        if (opt == null || opt.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder(96);
        try {
            for (String key : opt.keySet()) {
                sb.append(key).append('=').append(describeValue(opt.get(key))).append("; ");
            }
        } catch (Throwable ignored) {
            sb.append("(遍历失败)");
        }
        Diag.warn(context, "W22", "尺寸 options 全量 id=" + widgetId + " { " + sb + "}");
    }

    @NotNull
    private static String describeValue(@Nullable Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return String.valueOf(value);
        } catch (Throwable th) {
            return value.getClass().getSimpleName();
        }
    }

    /**
     * 「画布 / 卡片 / 格子 / 素材」四件套 —— 只做诊断，算不出来就少说两句。
     * v1.10.124：原始比例模式下**画布=卡片**（四周本来透明），格子多出来的部分由桌面居中透出壁纸。
     */
    @NotNull
    private static String describeFit(@NotNull Context context, @NotNull AppWidgetManager manager,
                                      @NotNull WidgetConfig config) {
        try {
            int[] cell = WidgetRender.INSTANCE.widgetSize(context, manager, config.getWidgetId());
            // v1.10.192：和真正推上去的画布对齐（原始比例走「帧画布」，与格子无关）
            int[] canvas = WidgetRender.canvasFor(context, config, cell);
            StringBuilder sb = new StringBuilder(72);
            sb.append("画布=").append(canvas[0]).append('x').append(canvas[1])
                    .append("(比例").append(fmt(canvas[0], canvas[1])).append(')')
                    .append(" 格子=").append(cell[0]).append('x').append(cell[1])
                    .append("(比例").append(fmt(cell[0], cell[1])).append(')');
            int fw = config.getFrameWidth();
            int fh = config.getFrameHeight();
            if (fw > 0 && fh > 0) {
                sb.append(" 素材=").append(fw).append('x').append(fh)
                        .append("(比例").append(fmt(fw, fh)).append(')');
            }
            // v1.10.278：这一段**只输出算出来的事实，不再复述策略**。
            // v1.10.271~277 的教训：文案写着「由桌面 fitCenter 留壁纸」，判据改了 4 次却没人改文案，
            // 于是日志自己跟自己打架 —— 我按日志下的结论全错，用户按日志看到的和眼睛看到的也对不上。
            //
            // 现在三个量全部来自代码，不来自注释：
            //   · 卡片 = FrameComposer.contentRect(...)（合成层真正用的矩形，唯一出处）；
            //   · 桌面视图 = 画布比例 vs 格子比例（相等→centerCrop 恒等；不等→fitCenter 保卡片）；
            //   · 铺满 = 布局==FIT（卡片=格子，画面居中、四周衬底 —— 见 FrameComposer 的 bleed）
            try {
                LayoutMode eff = WidgetRender.effLayout(config, cell);
                sb.append(eff == LayoutMode.FIT
                        ? " 布局=原始比例(画面完整内接,不裁)"
                        : " 布局=" + eff.name());
                if (fw > 0 && fh > 0) {
                    android.graphics.RectF pic = FrameComposer.INSTANCE.contentRect(
                            fw, fh, canvas[0], canvas[1], eff);
                    int cw = Math.round(pic.width());
                    int ch = Math.round(pic.height());
                    sb.append(" 画面=").append(cw).append('x').append(ch);
                    if (cw != canvas[0] || ch != canvas[1]) {
                        // v1.10.286：把"画面占格子多少"直接算出来打 —— 用户说"显示不全"时，
                        // 这一行必须能区分"被裁了"和"留了边"，不许再有含糊文案。
                        sb.append("(内接：占格子宽").append(Math.round(cw * 100.0f / canvas[0]))
                                .append("%、高").append(Math.round(ch * 100.0f / canvas[1]))
                                .append("%；其余透明露壁纸，一个像素都没裁)");
                    }
                }
                boolean fit = WidgetRender.INSTANCE.fitViewFor(config, canvas, cell);
                sb.append(" 桌面视图=").append(fit ? "fitCenter(保卡片,四周壁纸)" : "centerCrop(=格子,恒等)");
                float ca = canvas[0] / (float) Math.max(canvas[1], 1);
                float ba = cell[0] / (float) Math.max(cell[1], 1);
                if (ba > 0.0f) {
                    sb.append(" 画布/格子比例差=")
                            .append(String.format(java.util.Locale.US, "%.1f%%",
                                    Math.abs(ca - ba) / Math.min(ca, ba) * 100.0f));
                }
            } catch (Throwable ignored) {
            }
            // v1.10.280：**视图尺寸有没有被钉住** —— 这是这轮"怎么改都截断"的真正变量。
            // 桌面用 AT_MOST 量 RemoteViews 时，ImageView 取的是 drawable 的 intrinsic 尺寸
            // （= 位图像素 × density/160），于是位图被贴在左上角、右下留一条壁纸。
            try {
                // v1.10.283：不再钉尺寸（vivo 实测忽略 setViewLayoutWidth），改成算**桌面实际会摆多大**
                // = 位图像素 × density/160（实测 3.28）。只要它 ≥ 格子，桌面就会把视图顶到格子，
                // 所以这里直接打出来，一眼能看出"富余多少"。
                float dd = context.getResources().getDisplayMetrics().density;
                long intrinsic = Math.round(canvas[0] * dd);
                sb.append(" 桌面摆=").append(intrinsic).append('x')
                        .append(Math.round(canvas[1] * dd)).append("px(位图×").append(
                                String.format(java.util.Locale.US, "%.2f", dd))
                        .append("；≥格子才铺满");
                if (intrinsic >= cell[0]) {
                    sb.append("，已富余 ").append(intrinsic - cell[0]).append("px)");
                } else {
                    sb.append("，**还差 ").append(cell[0] - intrinsic).append("px**)");
                }
            } catch (Throwable ignored) {
            }
            return sb.toString();
        } catch (Throwable th) {
            return "画布=?";
        }
    }

    @NotNull
    private static String fmt(int w, int h) {
        return String.format(java.util.Locale.US, "%.3f", w / (float) Math.max(h, 1));
    }

    @Override // android.appwidget.AppWidgetProvider
    public void onDeleted(@NotNull Context context, @NotNull int[] appWidgetIds) {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(appWidgetIds, "appWidgetIds");
        for (int widgetId : appWidgetIds) {
            WidgetStore.INSTANCE.delete(context, widgetId);
        }
    }

    @Override // android.appwidget.AppWidgetProvider
    public void onDisabled(@NotNull Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
        PlaybackService.Companion.stop(context);
    }

    /* compiled from: WidgetProvider.kt */
    @Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��\u0016\n\u0002\u0018\u0002\n\u0002\u0010��\n\u0002\b\u0002\n\u0002\u0010\u0015\n��\n\u0002\u0018\u0002\b\u0086\u0003\u0018��2\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J\u000e\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006"}, d2 = {"Lio/github/animwidget/WidgetProvider$Companion;", "", "()V", "widgetIds", "", "context", "Landroid/content/Context;"})
    /* loaded from: classes.jar:io/github/animwidget/WidgetProvider$Companion.class */
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker $constructor_marker) {
            this();
        }

        private Companion() {
        }

        @NotNull
        public final int[] widgetIds(@NotNull Context context) {
            Intrinsics.checkNotNullParameter(context, "context");
            // v1.10.492：单格 + 多格都要（多格是另一个 receiver，漏了它这一路就不播）
            return MultiCell.allIds(context);
        }
    }
}
