package io.github.animwidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

import org.jetbrains.annotations.NotNull;

/**
 * v1.10.250：**「把组件钉到桌面」的唯一样板**（主界面按钮 / 长按快捷方式 共用）。
 *
 * <h3>为什么要收敛成一处</h3>
 * 原来 {@code MainActivity.addWidget()} 与 {@code PinWidgetActivity} 各自写了一遍
 * "判断桌面支持 → requestPin → 报错"，两份逻辑、两种提示。更要紧的是实测发现：
 * <b>vivo 桌面的 {@code isRequestPinAppWidgetSupported()} 返回 true，但
 * {@code requestPinAppWidget()} 什么也不做</b>（日志里之后没有任何组件创建），
 * 于是「只在不支持时才给提示」的写法在 vivo 上等于永远不给提示。
 *
 * <h3>现在的规则</h3>
 * <ul>
 *   <li>先问桌面：不支持 → {@link #UNSUPPORTED}，支持 → 真发一次请求；</li>
 *   <li>返回值只说明"请求被接受"，**不代表组件真的上了桌面** —— 所以调用方一律还要给
 *       一句按品牌的**手动路径**（vivo 原子组件 / 小米小部件 / OPPO 插件），
 *       那句才是保证能落地的东西。</li>
 * </ul>
 */
public final class WidgetPin {

    /** 请求被接受（vivo 会返回这个，但可能什么都不做）。 */
    public static final int ACCEPTED = 0;
    /** 桌面不支持 requestPinAppWidget。 */
    public static final int UNSUPPORTED = 1;
    /** 桌面明确拒绝（requestPinAppWidget 返回 false）。 */
    public static final int REJECTED = 2;
    /** 抛异常。 */
    public static final int FAILED = 3;

    private WidgetPin() {
    }

    /** 发一次钉组件请求；结果只表示"桌面怎么回应"，不表示组件到位。 */
    public static int request(@NotNull Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            if (manager == null || !manager.isRequestPinAppWidgetSupported()) {
                Diag.warn(context, "S10", "钉组件：桌面不支持 requestPinAppWidget（品牌=" + brand() + "）");
                return UNSUPPORTED;
            }
            boolean ok = manager.requestPinAppWidget(
                    new ComponentName(context, WidgetProvider.class), null, null);
            Diag.warn(context, "S10", "钉组件：已请求（品牌=" + brand()
                    + "，桌面回应=" + (ok ? "接受" : "拒绝") + "；接受≠真的钉上，看随后有没有 A02）");
            return ok ? ACCEPTED : REJECTED;
        } catch (Throwable t) {
            Diag.warn(context, "S10", "钉组件失败: " + t.getClass().getSimpleName());
            return FAILED;
        }
    }

    /** 按品牌给出「从桌面加组件」的真实路径（各家的名字都不一样）。 */
    @NotNull
    public static String hint(@NotNull Context context) {
        switch (brand()) {
            case "vivo":
                return context.getString(R.string.pin_widget_manual_vivo);
            case "xiaomi":
                return context.getString(R.string.pin_widget_manual_xiaomi);
            case "oppo":
                return context.getString(R.string.pin_widget_manual_oppo);
            default:
                return context.getString(R.string.pin_widget_unsupported);
        }
    }

    /** vivo/iQOO、小米/Redmi/POCO、OPPO/一加/realme 归三类，其余走通用文案。 */
    @NotNull
    public static String brand() {
        String m = android.os.Build.MANUFACTURER == null ? "" : android.os.Build.MANUFACTURER.toLowerCase(java.util.Locale.US);
        String b = android.os.Build.BRAND == null ? "" : android.os.Build.BRAND.toLowerCase(java.util.Locale.US);
        if (m.contains("vivo") || m.contains("iqoo") || b.contains("vivo") || b.contains("iqoo")) {
            return "vivo";
        }
        if (m.contains("xiaomi") || m.contains("redmi") || m.contains("poco")) {
            return "xiaomi";
        }
        if (m.contains("oppo") || m.contains("oneplus") || m.contains("realme")) {
            return "oppo";
        }
        return "other";
    }
}
