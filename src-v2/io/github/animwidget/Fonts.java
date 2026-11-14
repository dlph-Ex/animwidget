package io.github.animwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v1.10.98（需求 4）：**双语字体系统**。
 *
 * · 内置一款：`res/font/anim_dual` = Manrope（拉丁）+ 思源黑体 Noto Sans SC（中文）
 *   在构建期用 fontTools 合并成一个字体家族，两个字重（400 / 600），
 *   所以「中英混排」是同一套字形观感，不是英文一套中文另一套硬拼；
 * · 导入：系统文件管理器选 .ttf / .otf → 魔数校验 → Typeface 能否加载 → 字形覆盖检测
 *   （必须含中文或拉丁，否则直接拦截）→ 复制到 `files/fonts/`；
 * · 应用：全应用 TextView 统一换字（日志正文 `tv_log_body` 除外）；
 * · 删除：只有自己导入的能删，内置 / 跟随系统不能删。
 *
 * 只用框架 API（Typeface.Builder / Paint.hasGlyph，均 API 26 起），零第三方依赖。
 */
public final class Fonts {

    public static final String ID_SYSTEM = "system";
    public static final String ID_BUILTIN = "builtin";

    public static final String ROLE_ZH = "zh";
    public static final String ROLE_EN = "en";
    public static final String ROLE_BOTH = "both";

    public static final int OK = 0;
    public static final int ERR_FILE = -1;
    public static final int ERR_LANG = -2;
    public static final int ERR_LOAD = -3;

    private static final String PREF = "font_id";
    private static final String PREFS = "settings";
    private static final String META = "font_meta";

    private static final Map<String, Typeface> CACHE = new HashMap<>();
    private static final WeakHashMap<View, String> STAMP = new WeakHashMap<>();

    /**
     * v1.10.156（反馈）：**字体大小**四档（小 / 默认 / 中 / 大）。
     *
     * 口径是「在**当前 sp 值**上乘系数」，不是设一个固定字号 —— 每个 TextView 原来的大小
     * 保持相对关系，只是整体缩放。原值只记一次（BASE_SIZE），重复 applyTree 不会越放越大。
     */
    private static final float[] TEXT_SCALES = {0.86f, 1.00f, 1.14f, 1.28f};
    private static final String PREF_SCALE = "font_scale";
    private static final WeakHashMap<TextView, Float> BASE_SIZE = new WeakHashMap<>();

    /**
     * v1.10.159：**过渡系数**。>0 时覆盖持久化档位，用于「拖完滑块让字号连续长大」。
     *
     * 为什么不用 `Morph.crossfade`：那是「旧画面位图淡出」，文字是**跳变**到新尺寸再被
     * 盖住 —— 用户看到的就是「瞬间到达」。这里改成真的逐帧改 `setTextSize`，
     * 字号在 240ms 里连续长大，中间帧是真实排版，不是两张静态图叠化。
     *
     * 只在动画期用；动画结束必须 {@link #clearPreviewScale()}，否则持久化档位失效。
     */
    private static volatile float PREVIEW = -1.0f;
    private static android.animation.ValueAnimator SCALE_ANIM;

    public static void setPreviewScale(float scale) {
        PREVIEW = scale;
    }

    public static void clearPreviewScale() {
        PREVIEW = -1.0f;
    }

    /** 落库档位对应的系数（日志用，不受过渡值影响）。 */
    public static float committedTextScale(@NotNull Context context) {
        return TEXT_SCALES[textScaleIndex(context)];
    }

    /** 档位 → 系数（0 小 / 1 默认 / 2 中 / 3 大）。 */
    public static float textScaleAt(int index) {
        int i = index < 0 ? 0 : (index > 3 ? 3 : index);
        return TEXT_SCALES[i];
    }

    /**
     * 让整棵树的字号从 `from` 连续过渡到 `to`（系数，不是档位）。
     *
     * @param durationMs ≤0 表示「减少动态效果」：直接跳到终值
     * @param end        动画结束回调（可能在主线程下一帧）
     */
    public static void animateTextScale(@Nullable final View root, final float from,
                                        final float to, long durationMs,
                                        @Nullable final Runnable end) {
        // 先把静态引用摘掉再 cancel：cancel 会同步回调 onAnimationEnd，
        // 若那时 SCALE_ANIM 还等于旧动画，就会把「旧档位」落值一遍 —— 连点时能看到一跳。
        android.animation.ValueAnimator previous = SCALE_ANIM;
        SCALE_ANIM = null;
        if (previous != null) {
            previous.cancel();
        }
        if (root == null || durationMs <= 0L) {
            clearPreviewScale();
            applyTree(root);
            if (end != null) {
                end.run();
            }
            return;
        }
        final android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
        anim.setDuration(durationMs);
        anim.addUpdateListener(a -> {
            float t = Ease.IN_OUT.at(a.getAnimatedFraction());
            PREVIEW = from + (to - from) * t;
            applyTree(root);
        });
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // 被新的一次接管（或被取消）：不要落值，交给新的那次接着跑
                if (SCALE_ANIM != anim) {
                    return;
                }
                SCALE_ANIM = null;
                PREVIEW = -1.0f;
                applyTree(root);
                if (end != null) {
                    end.run();
                }
            }
        });
        SCALE_ANIM = anim;
        anim.start();
    }

    public static int textScaleIndex(@NotNull Context context) {
        int value = context.getSharedPreferences(PREFS, 0).getInt(PREF_SCALE, 1);
        return value < 0 ? 0 : (value > 3 ? 3 : value);
    }

    public static void setTextScaleIndex(@NotNull Context context, int index) {
        int value = index < 0 ? 0 : (index > 3 ? 3 : index);
        context.getSharedPreferences(PREFS, 0).edit().putInt(PREF_SCALE, value).apply();
    }

    public static float textScale(@NotNull Context context) {
        float preview = PREVIEW;
        if (preview > 0.0f) {
            return preview;
        }
        return TEXT_SCALES[textScaleIndex(context)];
    }

    /** 档位名（0 小 / 1 默认 / 2 中 / 3 大）。 */
    public static int textScaleLabelRes(int index) {
        if (index <= 0) {
            return R.string.settings_font_size_small;
        }
        if (index == 1) {
            return R.string.settings_font_size_default;
        }
        if (index == 2) {
            return R.string.settings_font_size_medium;
        }
        return R.string.settings_font_size_large;
    }

    /**
     * 单独作用在一个 TextView 上：原始大小 × 档位系数。
     * 胶囊按钮（Button）不参与 —— 用户明确要求按钮文本不动。
     */
    public static void applyTextScale(@Nullable TextView tv) {
        if (tv == null || tv instanceof android.widget.Button) {
            return;
        }
        if (noScale(tv)) {
            return;
        }
        Float base = BASE_SIZE.get(tv);
        if (base == null) {
            base = Float.valueOf(tv.getTextSize());
            BASE_SIZE.put(tv, base);
            if (base.floatValue() <= 0.0f) {
                return;
            }
        }
        float target = base.floatValue() * textScale(tv.getContext());
        if (Math.abs(tv.getTextSize() - target) > 0.5f) {
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, target);
        }
    }

    /** 沿父链找「no_font_scale」标记：日志页 / 导出历史页 / 导出日志弹窗整棵子树不缩放。 */
    private static boolean noScale(@NotNull View view) {
        android.view.ViewParent parent = view.getParent();
        int guard = 0;
        while (parent != null && guard++ < 48) {
            if (parent instanceof View && "no_font_scale".equals(((View) parent).getTag())) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    /** 给某个页面/弹窗的根节点打上「不参与字号缩放」。 */
    public static void markNoScale(@Nullable View root) {
        if (root != null) {
            root.setTag("no_font_scale");
        }
    }

    /** 最近一次成功导入的条目（给 UI 弹 toast / 自动选中用）。 */
    public static String lastImportedId;
    public static String lastImportedName;

    private Fonts() {
    }

    /** 字体条目（列表里的一行）。 */
    public static final class Item {
        public final String id;
        public final String name;
        public final String role;
        public final boolean builtin;

        Item(String id, String name, String role, boolean builtin) {
            this.id = id;
            this.name = name;
            this.role = role;
            this.builtin = builtin;
        }
    }

    public static String activeId(@NotNull Context c) {
        return c.getSharedPreferences(PREFS, 0).getString(PREF, ID_SYSTEM);
    }

    public static void setActiveId(@NotNull Context c, String id) {
        c.getSharedPreferences(PREFS, 0).edit().putString(PREF, id).apply();
    }

    private static File dir(@NotNull Context c) {
        File d = new File(c.getFilesDir(), "fonts");
        if (!d.exists()) {
            d.mkdirs();
        }
        return d;
    }

    private static SharedPreferences meta(@NotNull Context c) {
        return c.getSharedPreferences(META, 0);
    }

    /** 全量列表：跟随系统 / 内置 / 已导入。 */
    @NotNull
    public static List<Item> items(@NotNull Context c) {
        List<Item> out = new ArrayList<>();
        out.add(new Item(ID_SYSTEM, c.getString(R.string.settings_font_system), ROLE_BOTH, true));
        out.add(new Item(ID_BUILTIN, c.getString(R.string.settings_font_builtin), ROLE_BOTH, true));
        File[] files = dir(c).listFiles();
        if (files != null) {
            SharedPreferences m = meta(c);
            for (File f : files) {
                String key = f.getName();
                String raw = m.getString(key, null);
                if (raw == null) {
                    continue;
                }
                String[] parts = raw.split("\u0001");
                String name = parts.length > 0 ? parts[0] : key;
                String role = parts.length > 1 ? parts[1] : ROLE_BOTH;
                out.add(new Item("user:" + key, name, role, false));
            }
        }
        return out;
    }

    @Nullable
    public static String displayName(@NotNull Context c, String id) {
        for (Item it : items(c)) {
            if (it.id.equals(id)) {
                return it.name;
            }
        }
        return null;
    }

    public static boolean canDelete(@NotNull Item it) {
        return !it.builtin;
    }

    /** 主字体（不带字重）；跟随系统时返回 null。 */
    @Nullable
    private static Typeface userBase(@NotNull Context c, String id) {
        if (id == null || ID_SYSTEM.equals(id) || ID_BUILTIN.equals(id)) {
            return null;
        }
        Typeface cached = CACHE.get(id);
        if (cached != null) {
            return cached;
        }
        Typeface built = null;
        if (id.startsWith("user:")) {
            File f = new File(dir(c), id.substring(5));
            if (f.exists()) {
                built = new Typeface.Builder(f).build();
            }
        }
        if (built != null) {
            CACHE.put(id, built);
        }
        return built;
    }

    /**
     * 内置双语字体：优先走 `res/font/anim_dual`（框架按字重挑 400 / 600）；
     * 万一资源路径在这个 ROM 上取不到（压缩/裁剪），退回「把 raw 资源解出来再加载」，
     * 保证内置字体一定能用。
     */
    @Nullable
    private static Typeface builtin(@NotNull Context c, boolean bold) {
        String key = ID_BUILTIN + "|" + bold;
        Typeface cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Typeface family = null;
        try {
            family = c.getResources().getFont(R.font.anim_dual);
        } catch (Throwable th) {
            family = null;
        }
        Typeface out = null;
        if (family != null) {
            out = Typeface.create(family, bold ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (out == null) {
            File f = ensureBuiltinFile(c, bold);
            if (f != null) {
                out = new Typeface.Builder(f).build();
            }
        }
        if (out != null) {
            CACHE.put(key, out);
        }
        return out;
    }

    /** 把内置字体从资源解到私有目录（只在资源路径失败时用）。 */
    @Nullable
    private static File ensureBuiltinFile(@NotNull Context c, boolean bold) {
        File f = new File(c.getFilesDir(), bold ? "builtin_sb.ttf" : "builtin_r.ttf");
        if (f.exists() && f.length() > 100000L) {
            return f;
        }
        int res = bold ? R.font.anim_dual_sb : R.font.anim_dual_r;
        InputStream in = null;
        try {
            in = c.getResources().openRawResource(res);
            OutputStream out = new FileOutputStream(f);
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
            out.close();
            in.close();
            return f;
        } catch (Throwable th) {
            return null;
        }
    }

    /** 当前字体 + 指定字重。 */
    @Nullable
    public static Typeface typeface(@NotNull Context c, boolean bold) {
        String id = activeId(c);
        if (ID_SYSTEM.equals(id)) {
            return null;
        }
        if (ID_BUILTIN.equals(id)) {
            return builtin(c, bold);
        }
        Typeface base = userBase(c, id);
        if (base == null) {
            return null;
        }
        return Typeface.create(base, bold ? Typeface.BOLD : Typeface.NORMAL);
    }

    /** 字体列表里「用该字体自己显示自己名字」的预览字体（跟随系统返回 null）。 */
    @Nullable
    public static Typeface previewTypeface(@NotNull Context c, String id) {
        if (id == null || ID_SYSTEM.equals(id)) {
            return null;
        }
        String key = id + "|preview";
        Typeface cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Typeface out = ID_BUILTIN.equals(id) ? builtin(c, false) : userBase(c, id);
        if (out != null) {
            CACHE.put(key, out);
        }
        return out;
    }

    /** 把字体铺满整棵视图树（Activity 的 decorView 或任意子树）。 */
    public static void applyTree(@Nullable View root) {
        if (root == null) {
            return;
        }
        applyOne(root);
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            int n = g.getChildCount();
            for (int i = 0; i < n; i++) {
                applyTree(g.getChildAt(i));
            }
        }
    }

    /**
     * 单个视图：只处理 TextView 家族（Button / EditText / CheckBox / RadioButton / Switch 都是它）。
     * tag=="mono" 一律跳过 —— 需求 4.1「日志字体不变」。
     *
     * <p>v1.10.436：日志页正文已改成自绘控制台（{@link LogConsoleView}，不是 TextView），
     * 原来那条"跳过 `tv_log_body`"的 id 判据随布局一起删了 —— 留着它只是让
     * [15] 布局 id 体检指向一个已经不存在的控件。
     */
    public static void applyOne(@Nullable View v) {
        // v1.10.160：自绘滑块下方的「小/默认/中/大」也要跟着字号档位走。
        // 走这里而不是各写一条动画 —— applyTree 在字号过渡里每帧都跑，天然同步。
        if (v instanceof FontScaleSlider) {
            ((FontScaleSlider) v).setTextScale(textScale(v.getContext()));
            return;
        }
        if (!(v instanceof TextView)) {
            // v1.10.174（反馈）：字号档位变化后，**自绘控件（非 TextView）也必须重绘**，
            // 否则它们的 onDraw 里就算乘了 textScale，也一直停在旧字号上（图1 那类卡片）。
            // 只对叶子 View invalidate 一次，代价忽略；ViewGroup 由子节点各自处理。
            if (!(v instanceof ViewGroup)) {
                v.invalidate();
            }
            return;
        }
        TextView tv = (TextView) v;
        Object tag = tv.getTag();
        if ("mono".equals(tag) || "no_font".equals(tag)) {
            return;
        }
        // v1.10.156：先按档位缩放（基于它自己的原始 sp），再套字体
        applyTextScale(tv);
        Paint paint = tv.getPaint();
        Typeface cur = paint == null ? null : paint.getTypeface();
        boolean bold = cur != null && cur.isBold();
        String id = activeId(tv.getContext());
        if (ID_SYSTEM.equals(id)) {
            // 从「别的字体」切回跟随系统：把贴过标签的视图还原
            if (STAMP.remove(tv) != null) {
                tv.setTypeface(null, bold ? Typeface.BOLD : Typeface.NORMAL);
            }
            return;
        }
        String stamp = id + "|" + bold;
        if (stamp.equals(STAMP.get(tv))) {
            return;
        }
        Typeface tf = typeface(tv.getContext(), bold);
        if (tf == null) {
            return;
        }
        STAMP.put(tv, stamp);
        tv.setTypeface(tf);
    }

    /** 字体换了以后，把某个 Activity 的界面刷新一遍。 */
    public static void refresh(@Nullable View root) {
        CACHE.clear();
        applyTree(root);
    }

    // ------------------------------------------------------------------ 导入

    /** 魔数：TrueType(0x00010000 / "true")、OpenType("OTTO")、字体集("ttcf")。 */
    private static boolean isFontMagic(byte[] h) {
        if (h.length < 4) {
            return false;
        }
        int b0 = h[0] & 0xFF;
        int b1 = h[1] & 0xFF;
        int b2 = h[2] & 0xFF;
        int b3 = h[3] & 0xFF;
        if (b0 == 0x00 && b1 == 0x01 && b2 == 0x00 && b3 == 0x00) {
            return true;
        }
        String tag = new String(new char[]{(char) b0, (char) b1, (char) b2, (char) b3});
        return "true".equals(tag) || "OTTO".equals(tag) || "ttcf".equals(tag);
    }

    /** 字形覆盖：中文 / 拉丁 / 两者。都不沾 → null（拦截）。 */
    @Nullable
    private static String roleOf(@NotNull Typeface tf) {
        Paint p = new Paint();
        p.setTypeface(tf);
        boolean zh = p.hasGlyph("中") && p.hasGlyph("国") && p.hasGlyph("字");
        boolean en = p.hasGlyph("A") && p.hasGlyph("z") && p.hasGlyph("0");
        if (zh && en) {
            return ROLE_BOTH;
        }
        if (zh) {
            return ROLE_ZH;
        }
        if (en) {
            return ROLE_EN;
        }
        return null;
    }

    @Nullable
    private static String queryName(@NotNull Context c, Uri uri) {
        Cursor cur = c.getContentResolver().query(uri, null, null, null, null);
        if (cur == null) {
            return null;
        }
        String name = null;
        if (cur.moveToFirst()) {
            int idx = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (idx >= 0) {
                name = cur.getString(idx);
            }
        }
        cur.close();
        return name;
    }

    private static String prettyName(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "font";
        }
        String s = raw;
        int dot = s.lastIndexOf('.');
        if (dot > 0) {
            s = s.substring(0, dot);
        }
        // 去掉我们自己加的时间戳前缀
        int us = s.indexOf('_');
        if (us > 0 && us < 20) {
            boolean digits = true;
            for (int i = 0; i < us; i++) {
                if (!Character.isDigit(s.charAt(i))) {
                    digits = false;
                    break;
                }
            }
            if (digits) {
                s = s.substring(us + 1);
            }
        }
        return s.isEmpty() ? "font" : s;
    }

    /**
     * 导入字体。返回 {@link #OK} / {@link #ERR_FILE} / {@link #ERR_LANG} / {@link #ERR_LOAD}。
     * 失败时不会留下任何文件。
     */
    public static int importFont(@NotNull Context c, @NotNull Uri uri) {
        InputStream in = null;
        File dst = null;
        try {
            in = c.getContentResolver().openInputStream(uri);
            if (in == null) {
                return ERR_LOAD;
            }
            byte[] head = new byte[4];
            int got = 0;
            while (got < 4) {
                int r = in.read(head, got, 4 - got);
                if (r <= 0) {
                    break;
                }
                got += r;
            }
            if (got < 4 || !isFontMagic(head)) {
                return ERR_FILE;
            }
            String display = queryName(c, uri);
            String pretty = prettyName(display);
            String safe = pretty.replaceAll("[\\\\/:*?\"<>|\\s]", "_");
            if (safe.isEmpty()) {
                safe = "font";
            }
            dst = new File(dir(c), System.currentTimeMillis() + "_" + safe + ".ttf");
            InputStream src = c.getContentResolver().openInputStream(uri);
            if (src == null) {
                return ERR_LOAD;
            }
            OutputStream out = new FileOutputStream(dst);
            byte[] buf = new byte[8192];
            int n;
            while ((n = src.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
            out.close();
            src.close();
            Typeface tf = new Typeface.Builder(dst).build();
            if (tf == null) {
                return ERR_LOAD;
            }
            String role = roleOf(tf);
            if (role == null) {
                return ERR_LANG;
            }
            meta(c).edit().putString(dst.getName(), pretty + "\u0001" + role).apply();
            CACHE.clear();
            lastImportedId = "user:" + dst.getName();
            lastImportedName = pretty;
            return OK;
        } catch (Throwable th) {
            return ERR_LOAD;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable closeError) {
                    in = null;
                }
            }
            if (dst != null && dst.exists()) {
                // 只有「没写进 meta」的半成品才删（成功时 meta 里有记录）
                SharedPreferences m = meta(c);
                if (!m.contains(dst.getName())) {
                    dst.delete();
                }
            }
        }
    }

    /** 删除一个用户导入的字体（内置 / 跟随系统不动）。 */
    public static void remove(@NotNull Context c, @NotNull Item it) {
        if (it.builtin) {
            return;
        }
        String key = it.id.startsWith("user:") ? it.id.substring(5) : it.id;
        File f = new File(dir(c), key);
        if (f.exists()) {
            f.delete();
        }
        meta(c).edit().remove(key).apply();
        CACHE.remove(it.id);
        if (it.id.equals(activeId(c))) {
            setActiveId(c, ID_SYSTEM);
        }
    }
}
