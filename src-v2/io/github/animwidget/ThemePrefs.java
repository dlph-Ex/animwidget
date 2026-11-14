package io.github.animwidget;

import android.content.Context;
import android.content.SharedPreferences;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 主题偏好（键值全部存在 theme_presets 这个 SharedPreferences 里）。
 *
 * v1.10.47：
 *   • 对比度改两位小数 —— 内部存 x100 的整数（-100 ~ +100），旧键 contrast(int) 自动迁移；
 *   • 新增「滑块色」custom key = slider（默认取辅助色）；
 *   • 新增自定义配色方案库：scheme_ids + scheme_&lt;id&gt; + scheme_next + active_scheme。
 *     方案用自增 id 而不是下标 —— 删除后补位动画要靠稳定 id 认出「同一个图标」。
 */
public final class ThemePrefs {

    @NotNull
    public static final ThemePrefs INSTANCE = new ThemePrefs();

    @NotNull
    private static final String PREF = "theme_presets";
    public static final int MODE_SYSTEM = 0;
    public static final int MODE_LIGHT = 1;
    public static final int MODE_DARK = 2;

    /** 方案配色字段数：主色/辅助色/第三色/表面/中性/滑块/错误色 */
    public static final int SCHEME_FIELDS = 7;

    /**
     * v1.10.202 及以前：6 个字段（到「滑块」为止）。
     * v1.10.203 的早期构建写过 8 个字段（第 8 个是已废弃的 error_container）。
     * 读取时**三种长度都接受**，多的忽略、缺的补 0（= 未设置 → 用当前默认值）——
     * 升级后既不丢方案，也不会把「默认值」误当成用户设过的值。
     */
    public static final int SCHEME_FIELDS_LEGACY = 6;
    private static final int SCHEME_FIELDS_V203 = 8;

    /** v1.10.202：方案存档里「按模式各存一份」的底色数（surface_d, neutral_d, surface_l, neutral_l） */
    public static final int SCHEME_BASES = 4;

    private ThemePrefs() {
    }

    private SharedPreferences p(Context context) {
        return context.getSharedPreferences(PREF, 0);
    }

    public int getMode(Context context) {
        return p(context).getInt("mode", 0);
    }

    public void setMode(Context context, int value) {
        p(context).edit().putInt("mode", value).apply();
    }

    public int getPreset(Context context) {
        return p(context).getInt("preset", 0);
    }

    public void setPreset(Context context, int value) {
        p(context).edit().putInt("preset", value).apply();
    }

    /** 对比度（0.01 为单位）：-100 ~ +100，即 -1.00 ~ +1.00（v1.10.265 收窄到 Material 的 contrastLevel 量程）。 */
    public int getContrastX100(Context context) {
        SharedPreferences sp = p(context);
        if (sp.contains("contrast_x100")) {
            return clamp(sp.getInt("contrast_x100", 0), -100, 100);
        }
        // 旧版本存的是整数 -10..10，第一次读到就迁移
        int legacy = sp.getInt("contrast", 0);
        return clamp(legacy * 100, -100, 100);
    }

    public void setContrastX100(Context context, int value) {
        p(context).edit().putInt("contrast_x100", clamp(value, -100, 100)).apply();
    }

    public float getContrast(Context context) {
        return getContrastX100(context) / 100f;
    }

    /** 滑块色只覆盖内置动图选择器（默认，v1.10.47 起的老行为）。 */
    public static final int SLIDER_SCOPE_PICKER = 0;
    /** 滑块色覆盖全应用：所有 SeekBar 的表面色都取「滑块色」，而不是取主色。 */
    public static final int SLIDER_SCOPE_GLOBAL = 1;

    /**
     * v1.10.240（需求）：滑块色的作用范围。
     *
     * <p>老行为里 `slider` 这个色只给内置动图选择器的右侧滑块用（{@code AnimatedPickerActivity}），
     * 设置页/配置页的 SeekBar 一律取主色 —— 用户改「滑块色」时看不出变化，以为没生效。
     * 现在多一个「全局」档：全局时所有 SeekBar 的 progress/thumb 都取滑块色。</p>
     */
    public int getSliderScope(Context context) {
        return p(context).getInt("slider_scope", SLIDER_SCOPE_PICKER) == SLIDER_SCOPE_GLOBAL
                ? SLIDER_SCOPE_GLOBAL : SLIDER_SCOPE_PICKER;
    }

    public void setSliderScope(Context context, int value) {
        p(context).edit().putInt("slider_scope",
                value == SLIDER_SCOPE_GLOBAL ? SLIDER_SCOPE_GLOBAL : SLIDER_SCOPE_PICKER).apply();
    }

    // -------------------------------------------------- 预览虚线色（v1.10.463）

    /** 预览虚线色档位：跟随壁纸（壁纸主色的相反值）。与 {@link OutlineColor.Mode} 同序。 */
    public static final int OUTLINE_FOLLOW_WALLPAPER = 0;
    /** 预览虚线色档位：默认（中性灰按背景明暗整体偏移，不取反）。 */
    public static final int OUTLINE_DEFAULT = 1;
    /** 预览虚线色档位：自定义（用户给的实色，不再随壁纸变）。 */
    public static final int OUTLINE_CUSTOM = 2;

    /**
     * 预览虚线色的「取色口径」。
     *
     * <p>它和「滑块色 / 错误色」不是同一类东西：那些是调色板里的一个**位置**，虚线色是
     * 一条**判据**（跟随壁纸 / 默认 / 自定义）。所以存两个键：档位 + 自定义色 ——
     * 与 {@link OutlineColor#resolve} 的三个入参一一对应，UI 不再自己做分支。</p>
     *
     * <p>为什么放进 theme_presets 而不是 settings：它是「颜色」，要和其它语义色同生共死
     * （备份 / 清数据 / 只存在一个 SharedPreferences 里），也被门禁钉住
     * （这两个键只许本类读写，见 scripts/color-editor-scope-check.py）。</p>
     */
    public int getOutlineMode(Context context) {
        int v = p(context).getInt("outline_mode", OUTLINE_FOLLOW_WALLPAPER);
        return (v < OUTLINE_FOLLOW_WALLPAPER || v > OUTLINE_CUSTOM) ? OUTLINE_FOLLOW_WALLPAPER : v;
    }

    public void setOutlineMode(Context context, int value) {
        p(context).edit().putInt("outline_mode",
                (value < OUTLINE_FOLLOW_WALLPAPER || value > OUTLINE_CUSTOM)
                        ? OUTLINE_FOLLOW_WALLPAPER : value).apply();
    }

    /**
     * 自定义档的虚线色（ARGB，**0 表示从没设过**）。
     *
     * <p>不能用「0xRRGGBB 里 0 表示未设」—— 用户选纯黑（#000000）就是 0，会和「没设过」
     * 撞成同一个值。所以这里存的是带 alpha 的 ARGB（0xFF000000 ≠ 0）。</p>
     */
    public int getOutlineCustom(Context context) {
        return p(context).getInt("outline_custom", 0);
    }

    public void setOutlineCustom(Context context, int argb) {
        p(context).edit().putInt("outline_custom", 0xFF000000 | (argb & 0xFFFFFF)).apply();
    }

    public boolean hasOutlineCustom(Context context) {
        return getOutlineCustom(context) != 0;
    }

    public boolean getInvert(Context context) {
        return p(context).getBoolean("invert", false);
    }

    public void setInvert(Context context, boolean value) {
        p(context).edit().putBoolean("invert", value).apply();
    }

    /** 单个语义色覆盖（0 = 未覆盖，用预设值）。key: primary/secondary/tertiary/surface/neutral/slider */
    public int getCustom(Context context, String key) {
        return p(context).getInt(key, 0);
    }

    public void setCustom(Context context, String key, int value) {
        p(context).edit().putInt(key, value).apply();
    }

    // -------------------------------------------------- 底色通道（表面 / 中性变体）

    /**
     * v1.10.202：**「当前生效底色」的唯一写入口 A —— 用户改了一次底色**。
     *
     * 写 flat + 当前模式那一份；另一模式那一份**一律不动**（深色下拖纯白不该改到浅色）。
     * 同时记下「这个 flat 值是在哪个模式下写的」——
     * 只有当另一个模式从来没设过（那一份还是 0）时才会用到它：
     * 在深色下拖了个深色、切到浅色 → 按标记判出「那是另一个模式的值」→ 用浅色默认值，
     * 不会出现「白页面 + 黑卡片」（v1.10.83 那条可读性规则，靠标记实现才不丢用户颜色）。
     */
    public void applyBaseEdit(Context context, String key, int value, boolean dark) {
        if (!isBaseKey(key)) {
            setCustom(context, key, value);
            return;
        }
        p(context).edit()
                .putInt(key, value)
                .putInt(key + (dark ? "_d" : "_l"), value)
                .putInt(key + "_mode", dark ? 1 : 2)
                .apply();
    }

    /**
     * v1.10.202：**「当前生效底色」的唯一写入口 B —— 激活一个配色方案**。
     *
     * 六个值**一次性**写（一次 apply），因为它们必须描述同一个方案；
     * 只写其中几个，就又会变成「上一个谁改过就留着谁」的旁路键 ——
     * 用户反馈的「语义色没保存，却套用到别的自定义方案上」就是这么来的。
     */
    public void applyBaseSnapshot(Context context, int surface, int neutral,
                                 int surfaceDark, int neutralDark,
                                 int surfaceLight, int neutralLight) {
        p(context).edit()
                .putInt("surface", surface)
                .putInt("neutral", neutral)
                .putInt("surface_d", surfaceDark)
                .putInt("neutral_d", neutralDark)
                .putInt("surface_l", surfaceLight)
                .putInt("neutral_l", neutralLight)
                .apply();
    }

    // ------------------------------------------------ 单个语义色的「重置」

    /**
     * v1.10.205：这个颜色现在**有没有自定义值**（决定「重置」是不是幂等的空操作）。
     *
     * 底色要连「按模式各一份」一起看 —— 否则会出现「面板说没有自定义、可它就是不变」。
     */
    public boolean hasCustomKey(Context context, String key) {
        if (isBaseKey(key)) {
            return p(context).getInt(key, 0) != 0
                    || p(context).getInt(key + "_d", 0) != 0
                    || p(context).getInt(key + "_l", 0) != 0;
        }
        return p(context).getInt(key, 0) != 0;
    }

    /** 「重置」允许清的键（白名单，防止把 mode / active_scheme / 对比度之类顺手删了）。 */
    public static boolean isResettableKey(String key) {
        return "primary".contentEquals(key) || "secondary".contentEquals(key)
                || "tertiary".contentEquals(key) || "slider".contentEquals(key)
                || "error".contentEquals(key) || isBaseKey(key);
    }

    /**
     * v1.10.205：把某一个语义色恢复成「默认值」。
     *
     * 做法是**删掉覆盖值**，值由 {@link ThemeManager} 重新解析（预设色 / 该模式默认色）——
     * 所以「重置后的颜色」永远等于「本来就没有自定义时的颜色」，不存在两套算法算出两个值。
     *
     * 底色（表面 / 中性变体）必须**连按模式各一份 + 写入标记一起清**，
     * 否则 scoped 键还在，重置完看起来毫无变化（v1.10.202 那个坑）。
     *
     * @return 之前是否真的有自定义值（用于日志/幂等判断）
     */
    public boolean resetKey(Context context, String key) {
        if (!isResettableKey(key)) {
            return false;
        }
        boolean had = hasCustomKey(context, key);
        SharedPreferences.Editor editor = p(context).edit();
        if (isBaseKey(key)) {
            editor.remove(key).remove(key + "_d").remove(key + "_l").remove(key + "_mode");
        } else {
            editor.remove(key);
        }
        editor.apply();
        return had;
    }

    public static boolean isBaseKey(String key) {
        return "surface".contentEquals(key) || "neutral".contentEquals(key);
    }

    public boolean getShapeFollowSystem(Context context) {
        return p(context).getBoolean("shape_follow_system", true);
    }

    public void setShapeFollowSystem(Context context, boolean value) {
        p(context).edit().putBoolean("shape_follow_system", value).apply();
    }

    // ------------------------------------------------------------ 自定义配色方案

    /** 已保存方案的 id（按保存顺序，也是展示顺序）。 */
    @NotNull
    public long[] getSchemeIds(Context context) {
        String raw = p(context).getString("scheme_ids", "");
        if (raw == null || raw.length() == 0) {
            return new long[0];
        }
        String[] parts = raw.split(",");
        long[] out = new long[parts.length];
        int n = 0;
        for (int i = 0; i < parts.length; i++) {
            try {
                out[n++] = Long.parseLong(parts[i].trim());
            } catch (Throwable ignored) {
            }
        }
        if (n == parts.length) {
            return out;
        }
        long[] trimmed = new long[n];
        System.arraycopy(out, 0, trimmed, 0, n);
        return trimmed;
    }

    /**
     * v1.10.371（需求）：把某个方案在列表里**前移/后移一位**（delta = -1 / +1）。
     *
     * <p>只动 `scheme_ids` 的顺序 —— 展示顺序就是它，方案自己的配色按 id 存，不受影响。
     * 返回 true 表示真的换了（越界/找不到都返回 false，调用方据此决定要不要重画）。
     */
    public boolean moveScheme(@NotNull Context context, long id, int delta) {
        long[] ids = getSchemeIds(context);
        int idx = -1;
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == id) {
                idx = i;
                break;
            }
        }
        int to = idx + delta;
        if (idx < 0 || to < 0 || to >= ids.length) {
            return false;
        }
        long tmp = ids[idx];
        ids[idx] = ids[to];
        ids[to] = tmp;
        putSchemeIds(context, ids);
        return true;
    }

    private void putSchemeIds(Context context, long[] ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(ids[i]);
        }
        p(context).edit().putString("scheme_ids", sb.toString()).apply();
    }

    /**
     * v1.10.202：**方案自己的底色存档**（按模式各一份）。
     *
     * 配色方案以前只存 6 个色，底色的「按模式各一份」放在**全局键**里 ——
     * 于是「在方案 A 里改过表面色、还没保存」会跟着跑到方案 B 上（用户反馈的互相污染）。
     * 现在每个方案自己存 4 个：surface_d, neutral_d, surface_l, neutral_l。
     * 老方案没有这一项 → 返回 null，由调用方按它的存档值 + 明暗推断（行为与 201 一致）。
     */
    @Nullable
    public int[] getSchemeBases(Context context, long id) {
        if (id == -1L) {
            return null;
        }
        String raw = p(context).getString("scheme_bases_" + id, null);
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split(",");
        if (parts.length != SCHEME_BASES) {
            return null;
        }
        int[] out = new int[SCHEME_BASES];
        try {
            for (int i = 0; i < SCHEME_BASES; i++) {
                out[i] = (int) Long.parseLong(parts[i].trim());
            }
        } catch (Throwable th) {
            return null;
        }
        return out;
    }

    public void setSchemeBases(Context context, long id, @Nullable int[] bases) {
        if (id == -1L || bases == null || bases.length != SCHEME_BASES) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bases.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(bases[i]);
        }
        p(context).edit().putString("scheme_bases_" + id, sb.toString()).apply();
    }

    /** 方案配色（6 个色），不存在返回 null。 */
    @Nullable
    public int[] getScheme(Context context, long id) {
        String raw = p(context).getString("scheme_" + id, null);
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split(",");
        // v1.10.203：老存档 6 个字段、新存档 7 个（多了 error），另有一个早期构建写过的 8 字段版本。
        // 三种都接受，统一补齐成 SCHEME_FIELDS 长度返回 —— 调用方不用关心版本。
        if (parts.length != SCHEME_FIELDS && parts.length != SCHEME_FIELDS_LEGACY
                && parts.length != SCHEME_FIELDS_V203) {
            return null;
        }
        int[] out = new int[SCHEME_FIELDS];
        try {
            for (int i = 0; i < parts.length && i < SCHEME_FIELDS; i++) {
                out[i] = (int) Long.parseLong(parts[i].trim());
            }
        } catch (Throwable th) {
            return null;
        }
        return out;
    }

    /**
     * v1.10.90：**方案里存的对比度**（x100）。
     *
     * 对比度从此是配色方案的一部分：切方案时连对比度一起恢复，改对比度时写回当前方案。
     * 老方案（v1.10.89 及之前保存的）没有这一项 → 回退到当前全局值，等于「应用后对比度不变」，
     * 不会把用户现在的对比度意外冲掉；等用户再改一次对比度就自动补上。
     */
    public int getSchemeContrast(Context context, long id) {
        if (id == -1L) {
            return getContrastX100(context);
        }
        SharedPreferences sp = p(context);
        String key = "scheme_contrast_" + id;
        if (sp.contains(key)) {
            return clamp(sp.getInt(key, 0), -100, 100);
        }
        return getContrastX100(context);
    }

    /** 只写方案的对比度（不动全局值）。 */
    public void setSchemeContrast(Context context, long id, int value) {
        if (id == -1L) {
            return;
        }
        p(context).edit().putInt("scheme_contrast_" + id, clamp(value, -100, 100)).apply();
    }

    /**
     * v1.10.179（反馈 2）：**系统预设共享一个对比度**。
     *
     * 以前只有一个全局 `contrast_x100`，从「自定义方案」切回预设时会把方案自己的
     * 对比度带过去（预设被污染）；用户要的是：预设之间切来切去保持同一个值，
     * 自定义方案各存各的。所以给预设单开一个键：
     *   · 预设模式下发起的每一次改对比度 → 同时写这里（{@code writeContrast}）；
     *   · 切到任意预设 → 用这里恢复；
     *   · 老版本没有这个键 → 回退当前值起步，观感不变。
     */
    public int getPresetContrastX100(Context context) {
        SharedPreferences sp = p(context);
        if (sp.contains("preset_contrast_x100")) {
            return clamp(sp.getInt("preset_contrast_x100", 0), -100, 100);
        }
        return getContrastX100(context);
    }

    /** 写「系统预设共享对比度」（自定义方案不用这个）。 */
    public void setPresetContrastX100(Context context, int value) {
        p(context).edit().putInt("preset_contrast_x100", clamp(value, -100, 100)).apply();
    }

    /**
     * v1.10.92：**覆盖保存某个方案**（编辑已保存方案）。
     *
     * 这就是「已保存方案不能被编辑」的出入口：把当前 6 个颜色 + 当前对比度写回该方案。
     * 注意 v1.10.91 及之前是「改对比度就自动写回方案」—— 用户反馈那是**擅自保存**，
     * 现在改对比度只改运行时状态，必须显式调用本方法（或保存为新方案）才会落进方案里。
     */
    public boolean updateScheme(Context context, long id, int[] colors, int contrastX100) {
        if (id == -1L || colors == null || colors.length != SCHEME_FIELDS) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < colors.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(colors[i]);
        }
        p(context).edit()
                .putString("scheme_" + id, sb.toString())
                .putInt("scheme_contrast_" + id, clamp(contrastX100, -100, 100))
                .apply();
        return true;
    }

    /** 追加一个方案，返回新 id。 */
    public long addScheme(Context context, int[] colors) {
        if (colors == null || colors.length != SCHEME_FIELDS) {
            return -1L;
        }
        long id = p(context).getLong("scheme_next", 1L);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < colors.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(colors[i]);
        }
        long[] ids = getSchemeIds(context);
        long[] next = new long[ids.length + 1];
        System.arraycopy(ids, 0, next, 0, ids.length);
        next[ids.length] = id;
        SharedPreferences.Editor editor = p(context).edit();
        editor.putLong("scheme_next", id + 1L);
        editor.putString("scheme_" + id, sb.toString());
        // v1.10.90：对比度也是方案的一部分（存的是**当前**对比度）
        editor.putInt("scheme_contrast_" + id, getContrastX100(context));
        editor.apply();
        putSchemeIds(context, next);
        return id;
    }

    public void removeScheme(Context context, long id) {
        long[] ids = getSchemeIds(context);
        int at = indexOf(ids, id);
        if (at < 0) {
            return;
        }
        long[] next = new long[ids.length - 1];
        System.arraycopy(ids, 0, next, 0, at);
        System.arraycopy(ids, at + 1, next, at, ids.length - at - 1);
        p(context).edit().remove("scheme_" + id).remove("scheme_contrast_" + id)
                .remove("scheme_bases_" + id).apply();
        putSchemeIds(context, next);
        if (getActiveScheme(context) == id) {
            setActiveScheme(context, -1L);
        }
    }

    public static int indexOf(long[] ids, long id) {
        if (ids == null) {
            return -1;
        }
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == id) {
                return i;
            }
        }
        return -1;
    }

    public long getActiveScheme(Context context) {
        return p(context).getLong("active_scheme", -1L);
    }

    public void setActiveScheme(Context context, long value) {
        p(context).edit().putLong("active_scheme", value).apply();
    }

    /**
     * v1.10.49：当前是不是「自定义色」状态（有选中方案，或手动改过任一语义色）。
     * 用来决定预设配色那一行要不要显示选中框 —— 有自定义色时不能还戳着某个预设。
     */
    public boolean hasCustom(Context context) {
        if (getActiveScheme(context) != -1L) {
            return true;
        }
        String[] keys = {"primary", "secondary", "tertiary", "surface", "neutral", "slider",
                "error"};
        for (int i = 0; i < keys.length; i++) {
            if (p(context).getInt(keys[i], 0) != 0) {
                return true;
            }
        }
        return false;
    }

    /** 恢复预设：清掉所有语义色覆盖 + 方案选中态（方案库本身保留）。 */
    public void clearCustom(Context context) {
        p(context).edit()
                .remove("primary").remove("secondary").remove("tertiary")
                .remove("surface").remove("neutral").remove("slider")
                // v1.10.203：错误色同样是「当前生效的语义色」，恢复预设要一起清
                .remove("error")
                // v1.10.203 的早期构建把「错误提示底色」也存过一份，已废弃 —— 顺手清掉
                .remove("error_container")
                // v1.10.202：底色的「按模式各一份」和写入标记也必须一起清 ——
                // 以前这里漏了，于是「恢复预设 / 切回系统预设」之后表面色还是自定义的
                //（liveBase 先看 scoped 键，看到有值就一直用），看起来就像预设被污染了。
                .remove("surface_d").remove("neutral_d")
                .remove("surface_l").remove("neutral_l")
                .remove("surface_mode").remove("neutral_mode")
                .putLong("active_scheme", -1L)
                .apply();
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }
}
