package io.github.animwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.os.Bundle;
import android.widget.Toast;

/**
 * v1.10.245：**长按快捷方式「添加组件到桌面」的落点**。
 *
 * <h3>为什么需要它</h3>
 * 长按桌面图标弹出的菜单里有两类东西：
 * <ol>
 *   <li>系统/桌面自己加的（卸载、应用信息、分享、以及 OriginOS 的「原子组件」）——
 *       这类**只有桌面能决定**，App 加不了也挡不住；</li>
 *   <li>App 用 {@code android.app.shortcuts} 自己声明的快捷项 —— 这一类我们能加。</li>
 * </ol>
 * 所以这里补的是第 2 类里最有用的那一个：一键把组件钉到桌面
 * （{@link AppWidgetManager#requestPinAppWidget}，API 26+，系统会弹确认框；
 * 我们有 {@code android:configure} 配置页，确认后桌面会直接把配置页拉起来）。
 *
 * <p>没有 UI：透明主题、立刻 finish，只负责发起请求与兜底提示。</p>
 */
public final class PinWidgetActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dumpProviders();
        int result = WidgetPin.request(this);
        if (result == WidgetPin.UNSUPPORTED || result == WidgetPin.FAILED) {
            if (!openLauncherPicker()) {
                Toast.makeText(this, WidgetPin.hint(this), Toast.LENGTH_LONG).show();
            }
        } else {
            // v1.10.250：vivo 桌面"接受"了却什么都不做，所以路径提示无条件给
            Toast.makeText(this, WidgetPin.hint(this), Toast.LENGTH_LONG).show();
        }
        finish();
        overridePendingTransition(0, 0);
    }

    /**
     * v1.10.247：**把「系统眼里的我们」打出来**（S11）。
     *
     * <p>为什么需要：长按菜单里的「原子组件 / 小组件」那一项是桌面加的，App 端改不了它。
     * 但它一定依赖「系统能不能看到我们的 provider、以及这个 provider 带了哪些声明」。
     * 这一行把系统实际读到的字段原样打出来（可见性 / 预览 / 类别 / 是否需配置 / 目标格），
     * 以后要判断「是声明不对」还是「桌面根本不打算展示」，就不用猜。</p>
     */
    private void dumpProviders() {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            java.util.List<android.appwidget.AppWidgetProviderInfo> list =
                    manager.getInstalledProvidersForPackage(getPackageName(), null);
            StringBuilder sb = new StringBuilder();
            sb.append("组件声明 包=").append(getPackageName())
              .append(" 系统可见=").append(list == null ? -1 : list.size());
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    android.appwidget.AppWidgetProviderInfo info = list.get(i);
                    sb.append(" | #").append(i + 1)
                      .append(" ").append(info.provider == null ? "?" : info.provider.getClassName())
                      .append(" 最小=").append(info.minWidth).append('x').append(info.minHeight).append("dp")
                      .append(" 预览图=").append(info.previewImage != 0 ? "有" : "无")
                      .append(" 预览布局=").append(info.previewLayout != 0 ? "有" : "无")
                      .append(" 类别=").append(info.widgetCategory == 1 ? "home_screen" : String.valueOf(info.widgetCategory))
                      .append(" 需配置=").append(info.configure == null ? "否" : "是")
                      .append(" 可缩放=").append(info.resizeMode)
                      .append(" 目标格=").append(info.targetCellWidth).append('x').append(info.targetCellHeight);
                }
            }
            Diag.warn(this, "S11", sb.toString());
        } catch (Throwable t) {
            Diag.warn(this, "S11", "读取组件声明失败: " + t.getClass().getSimpleName());
        }
    }

    /** @return true = 桌面接受了「打开组件选择器」这个请求（用户会看到组件列表）。 */
    private boolean openLauncherPicker() {
        try {
            android.content.Intent pick = new android.content.Intent("android.appwidget.action.APPWIDGET_PICK");
            pick.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(pick);
            Diag.warn(this, "S10", "已请求桌面组件选择器（APPWIDGET_PICK）");
            return true;
        } catch (Throwable t) {
            Diag.warn(this, "S10", "桌面未开放组件选择器: " + t.getClass().getSimpleName());
            return false;
        }
    }
}
