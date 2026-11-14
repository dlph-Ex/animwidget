package io.github.animwidget;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 语言原地切换基类（v1.10.1，需求 9）。
 *
 * 以前切语言 = 重建 Activity：会折叠展开的菜单、回到第 1 页、丢失滚动位置，还闪一下。
 * 现在改成两步：
 *   1) 换掉本页的 Resources（{@link #getResources()} 覆盖）—— 之后所有 getString 立刻是新语言；
 *   2) 用新语言重新 inflate 一份**同一布局**，按 id 把文字类属性搬回活着的视图树
 *      （只搬文字，不动结构/状态），再由子类补刷动态文字。
 * 文字是原地 setText，所以不闪、不丢状态、长度变化由布局自己平滑过渡。
 */
public class I18nActivity extends Activity {

    @Nullable
    private Resources i18nResources;
    private int i18nLayout;
    /**
     * v1.10.513：本进程是不是原版签名（false = 限制模式）。
     * 在 {@code onCreate} 里读一次缓存；子类要按它改界面的地方读这个字段，不要再各调一遍。
     */
    private boolean restrictedBuild;

    @Override
    protected void onCreate(@Nullable android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // v1.10.513（需求①）：所有页面都经过这里 —— 发行身份在这里落到界面状态。
        this.restrictedBuild = Integrity.restricted(this);
    }

    /** true = 非原版签名，进入限制模式（不推帧 / 不导出）。 */
    protected final boolean isRestrictedBuild() {
        return this.restrictedBuild;
    }

    @Override
    protected void attachBaseContext(@NotNull Context newBase) {
        // 系统「应用语言」设置过本应用语言时先同步过来（只读）
        LocaleHelper.syncFromSystem(newBase);
        super.attachBaseContext(LocaleHelper.INSTANCE.wrap(newBase));
    }

    @Override
    @NotNull
    public Resources getResources() {
        Resources overridden = this.i18nResources;
        return overridden != null ? overridden : super.getResources();
    }

    /** 子类在 onCreate 里登记自己的布局，原地换语言才能取到新文案。 */
    protected final void setI18nLayout(int layoutRes) {
        this.i18nLayout = layoutRes;
    }

    /**
     * v1.10.185：所有页面（除 DiagCatalogActivity）都继承本类，这里统一记录「发现换肤」。
     * 子类自己的 onConfigurationChanged 都会调 super，所以 H01 不会漏。
     */
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            ThemeSwitch.checkNow(this, "configChange");
        } catch (Throwable ignored) {
        }
    }

    /** v1.10.185：进入前台就接管「模式变了没」的盯盘（H 系列日志的统一来源）。 */
    @Override
    protected void onPause() {
        ThemeSwitch.disarm(this);
        super.onPause();
    }

    /** v1.10.98：回到前台再兜一遍字体（别的页面改过字体设置时不会留旧字形）。 */
    @Override
    protected void onResume() {
        super.onResume();
        ThemeSwitch.arm(this);
        try {
            Fonts.applyTree(getWindow().getDecorView());
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    /**
     * v1.10.98（需求 4）：每次 setContentView 之后把自定义字体铺满整棵树。
     * 放在这里而不是每个 Activity 里各写一遍 —— 所有页面自动都吃到字体设置。
     */
    @Override
    public void onContentChanged() {
        super.onContentChanged();
        try {
            Fonts.applyTree(getWindow().getDecorView());
        } catch (Throwable th) {
            // 字体失败只影响观感，绝不影响页面（不新增诊断号）
            th.printStackTrace();
        }
    }

    /** 语言变了：原地换文案（成功返回 true，失败则调用方回退到 recreate）。 */
    public final boolean applyLocaleInPlace() {
        try {
            Context wrapped = LocaleHelper.INSTANCE.wrap(getApplicationContext());
            this.i18nResources = wrapped.getResources();
            if (this.i18nLayout != 0) {
                View fresh = LayoutInflater.from(wrapped).inflate(this.i18nLayout, null);
                copyTexts(fresh, getWindow().getDecorView());
                applyIdless(fresh);
            }
            onTextsRefreshed();
            return true;
        } catch (Throwable th) {
            Diag.warn(this, "L20", "原地切换语言失败: " + th.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * v1.10.200（反馈）：**按「当前界面语言」取字符串**。
     *
     * `applyLocaleInPlace()` 只是把「影子布局」的文本抄到现有视图上，Activity 自己的
     * resources 仍然停在旧 locale —— 于是所有「代码里 setText / 动态建的行 / 独立 Dialog」
     * 拿到的还是旧语言（性能卡、语义色行、颜色编辑器的取消/保存…都踩过）。
     * 统一走这里：i18nResources 就是 wrap 过的新 locale 资源。
     */
    protected final String s(int resId) {
        try {
            return this.i18nResources != null ? this.i18nResources.getString(resId) : getString(resId);
        } catch (Throwable th) {
            return getString(resId);
        }
    }

    protected final String s(int resId, Object... args) {
        try {
            return this.i18nResources != null
                    ? this.i18nResources.getString(resId, args) : getString(resId, args);
        } catch (Throwable th) {
            return getString(resId, args);
        }
    }

    /** 子类在这里重刷「代码里 setText 的那部分」文字。 */
    protected void onTextsRefreshed() {
    }

    /** 语言检查 + 原地切换（放在 onResume 里调）。 */
    protected final void syncLocale() {
        if (!LocaleHelper.INSTANCE.needsRecreate(this)) {
            return;
        }
        if (!applyLocaleInPlace()) {
            recreate();
        }
    }

    /**
     * v1.10.99（反馈 1 / 1.1）：**没有 id 的静态文字也要跟着换语言**。
     *
     * 之前只按 id 搬文案，可布局里「页面标题」「卡片标题」「说明」这些根本没写 id，
     * 于是：切英文 → 它们变英文；切回跟随系统 → 其余文字变中文，它们还挂着英文。
     * （首页标题 `@string/app_name` 也是同一个原因。）
     *
     * 现在补一条**按树路径对齐**的通道：fresh 树和 live 树同一份布局 → 路径一一对应，
     * 只处理「live 这一格没有 id」的文字节点；结构对不上（动态列表、行数不同）就整支放弃，
     * 交给子类自己的 onTextsRefreshed 重建。
     */
    private void applyIdless(@Nullable View fresh) {
        if (fresh == null) {
            return;
        }
        View content = getWindow().getDecorView().findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) content;
        if (group.getChildCount() != 1) {
            return;
        }
        copyIdless(fresh, group.getChildAt(0));
    }

    private static void copyIdless(@Nullable View fresh, @Nullable View live) {
        if (fresh == null || live == null) {
            return;
        }
        if (fresh.getClass() != live.getClass()) {
            return;
        }
        if (live instanceof ViewGroup) {
            ViewGroup f = (ViewGroup) fresh;
            ViewGroup l = (ViewGroup) live;
            if (f.getChildCount() != l.getChildCount()) {
                return;
            }
            for (int i = 0; i < f.getChildCount(); i++) {
                copyIdless(f.getChildAt(i), l.getChildAt(i));
            }
            return;
        }
        if (live.getId() != View.NO_ID) {
            return;
        }
        CharSequence cd = fresh.getContentDescription();
        if (cd != null && cd.length() > 0) {
            live.setContentDescription(cd);
        }
        if (!(live instanceof TextView) || !(fresh instanceof TextView)) {
            return;
        }
        TextView lv = (TextView) live;
        TextView fv = (TextView) fresh;
        CharSequence text = fv.getText();
        CharSequence now = lv.getText();
        if (text != null && text.length() > 0 && now != null && now.length() > 0) {
            lv.setText(text);
        }
        CharSequence hint = fv.getHint();
        if (hint != null && hint.length() > 0) {
            lv.setHint(hint);
        }
    }

    /** 把 fresh 树里的文字按 id 搬到活着的树上（只搬文字，不搬结构）。 */
    private static void copyTexts(@Nullable View fresh, @Nullable View liveRoot) {
        if (fresh == null || liveRoot == null) {
            return;
        }
        Map<Integer, String[]> texts = new HashMap<>();
        collect(fresh, texts);
        apply(liveRoot, texts);
    }

    private static void collect(@Nullable View view, @NotNull Map<Integer, String[]> out) {
        if (view == null) {
            return;
        }
        int id = view.getId();
        if (id != View.NO_ID) {
            String text = view instanceof TextView ? string(((TextView) view).getText()) : null;
            String hint = view instanceof TextView ? string(((TextView) view).getHint()) : null;
            String cd = string(view.getContentDescription());
            if (text != null || hint != null || cd != null) {
                out.put(id, new String[]{text, hint, cd});
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                collect(group.getChildAt(i), out);
            }
        }
    }

    private static void apply(@Nullable View view, @NotNull Map<Integer, String[]> texts) {
        if (view == null) {
            return;
        }
        int id = view.getId();
        if (id != View.NO_ID) {
            String[] values = texts.get(id);
            if (values != null) {
                if (values[0] != null && view instanceof TextView) {
                    ((TextView) view).setText(values[0]);
                }
                if (values[1] != null && view instanceof TextView) {
                    ((TextView) view).setHint(values[1]);
                }
                if (values[2] != null) {
                    view.setContentDescription(values[2]);
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                apply(group.getChildAt(i), texts);
            }
        }
    }

    @Nullable
    private static String string(@Nullable CharSequence value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.length() == 0 ? null : text;
    }
}
