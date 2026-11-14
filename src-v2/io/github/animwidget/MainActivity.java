package io.github.animwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kotlin.Metadata;
import kotlin.NoWhenBranchMatchedException;
import kotlin.Unit;
import kotlin.collections.ArraysKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Lambda;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* compiled from: MainActivity.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��i\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0002\b\u0005*\u0001\n\u0018��2\u00020\u0001B\u0005¢\u0006\u0002\u0010\u0002J\b\u0010\u0010\u001a\u00020\u0011H\u0002J\u0010\u0010\u0012\u001a\u00020\u00112\u0006\u0010\u0013\u001a\u00020\u0014H\u0014J\u0018\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u0004H\u0002J\u0010\u0010\u001a\u001a\u00020\u00112\u0006\u0010\u0017\u001a\u00020\u0018H\u0002J\u0010\u0010\u001b\u001a\u00020\u00112\u0006\u0010\u001c\u001a\u00020\u001dH\u0016J\u0012\u0010\u001e\u001a\u00020\u00112\b\u0010\u001f\u001a\u0004\u0018\u00010 H\u0014J\b\u0010!\u001a\u00020\u0011H\u0014J\b\u0010\"\u001a\u00020\u0011H\u0014J\u0010\u0010#\u001a\u00020\u00112\u0006\u0010$\u001a\u00020%H\u0002J\b\u0010&\u001a\u00020\u0011H\u0002J\b\u0010'\u001a\u00020\u0011H\u0002J\b\u0010(\u001a\u00020\u0011H\u0002J\b\u0010)\u001a\u00020\u0011H\u0002J\n\u0010*\u001a\u0004\u0018\u00010+H\u0002J\b\u0010,\u001a\u00020\u0011H\u0002J\u0010\u0010-\u001a\u00020\u00112\u0006\u0010.\u001a\u00020+H\u0002J\b\u0010/\u001a\u00020\u0011H\u0002J\b\u00100\u001a\u00020\u0011H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.¢\u0006\u0002\n��R\u000e\u0010\u0007\u001a\u00020\bX\u0082.¢\u0006\u0002\n��R\u0010\u0010\t\u001a\u00020\nX\u0082\u0004¢\u0006\u0004\n\u0002\u0010\u000bR\u000e\u0010\f\u001a\u00020\rX\u0082.¢\u0006\u0002\n��R\u000e\u0010\u000e\u001a\u00020\rX\u0082.¢\u0006\u0002\n��R\u000e\u0010\u000f\u001a\u00020\rX\u0082.¢\u0006\u0002\n��"}, d2 = {"Lio/github/animwidget/MainActivity;", "Landroid/app/Activity;", "()V", "appliedDark", "", "btnToggle", "Landroid/widget/Button;", "llWidgets", "Landroid/widget/LinearLayout;", "themePoll", "io/github/animwidget/Runnable", "Lio/github/animwidget/Runnable;", "tvBackend", "Landroid/widget/TextView;", "tvDiag", "tvEmpty", "addWidget", "", "attachBaseContext", "newBase", "Landroid/content/Context;", "buildRow", "Landroid/view/View;", "config", "Lio/github/animwidget/WidgetConfig;", "installed", "confirmDelete", "onConfigurationChanged", "newConfig", "Landroid/content/res/Configuration;", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onPause", "onResume", "openConfig", "widgetId", "", "refresh", "refreshThemeInPlace", "requestBatteryWhitelist", "requestUsageAccess", "resolveLauncherPackage", "", "runShizuku", "toast", "message", "togglePlayback", "updateDiagnostics"})
@SourceDebugExtension({"SMAP\nMainActivity.kt\nKotlin\n*S Kotlin\n*F\n+ 1 MainActivity.kt\nio/github/animwidget/MainActivity\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,334:1\n1#2:335\n*E\n"})
/* loaded from: classes.jar:io/github/animwidget/MainActivity.class */
public final class MainActivity extends I18nActivity {
    LinearLayout llWidgets;
    TextView tvEmpty;
    TextView tvBackend;
    TextView tvDiag;
    Button btnToggle;
    /** v1.10.392：01 区块的三格仪表读数（组件总数 / 正在播放 / 帧总数）。 */
    TextView tvHeroNum;
    TextView tvHeroPlay;
    TextView tvHeroFrames;
    /** v1.10.392：02 区块右栏的三个状态字（只读查询，不改任何状态）。 */
    TextView tvShizukuState;
    TextView tvBatteryState;
    TextView tvUsageState;
    boolean appliedDark;
    /**
     * v1.10.316：本轮列出来的「桌面上已移除」配置（供顶部「清理已移除的配置（N）」一键删除）。
     * 每次 refresh 重建，不留跨轮状态。
     */
    private final List<WidgetConfig> missingConfigs = new ArrayList<>();

    @NotNull
    private final Runnable themePoll = new Runnable() { // from class: io.github.animwidget.Runnable
        @Override // java.lang.Runnable
        public void run() {
            boolean dark = ThemeManager.INSTANCE.isDark(MainActivity.this);
            if (dark != MainActivity.this.appliedDark) {
                MainActivity.this.appliedDark = dark;
                Morph.crossfade(MainActivity.this, () -> MainActivity.this.refreshThemeInPlace());
            }
            MainActivity.this.getWindow().getDecorView().postDelayed(this, 500L);
        }
    };

    @Override // android.app.Activity
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashLog.install(this);
        // A01：主界面进程拉起（v1.8.13，同步 logcat，应用进不去时排查用）
        Diag.warn(this, "A01", "主界面启动");
        // v1.10.3：清掉上次进程被杀残留的导出暂存（不碰已生效的 frames）
        try {
            WidgetStore.INSTANCE.cleanupStaging(this);
        } catch (Throwable ignored) {
        }
        setContentView(R.layout.activity_main);
        setI18nLayout(R.layout.activity_main);
        // v1.10.393：主页也要真全屏 —— 内容滚到状态栏/导航栏下面，两条栏刷透明
        // （栏色由 ThemeManager.applySystemBars 按 EdgeToEdge.isUnderBars 统一决定）。
        // 顶部 inset 落在滚动内容的 paddingTop、底部落在 paddingBottom，
        // 所以静止时标题不会钻到状态栏里，滚动时才从栏下面穿过去。
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            EdgeToEdge.INSTANCE.applyUnderBars(this, R.id.scroll_main);
        } else {
            EdgeToEdge.INSTANCE.apply(this);
        }
        this.appliedDark = ThemeManager.INSTANCE.isDark(this);
        ShizukuHelper.INSTANCE.init();
        View viewFindViewById = findViewById(R.id.ll_widgets);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById, "findViewById(...)");
        this.llWidgets = (LinearLayout) viewFindViewById;
        View viewFindViewById2 = findViewById(R.id.tv_empty);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById2, "findViewById(...)");
        this.tvEmpty = (TextView) viewFindViewById2;
        View viewFindViewById3 = findViewById(R.id.tv_backend);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById3, "findViewById(...)");
        this.tvBackend = (TextView) viewFindViewById3;
        View viewFindViewById4 = findViewById(R.id.tv_diag);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById4, "findViewById(...)");
        this.tvDiag = (TextView) viewFindViewById4;
        View viewFindViewById5 = findViewById(R.id.btn_toggle);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById5, "findViewById(...)");
        this.btnToggle = (Button) viewFindViewById5;
        // v1.10.392：01 区块的实时计数与 02 区块右栏的三个状态字（都是只读展示）
        this.tvHeroNum = (TextView) findViewById(R.id.tv_hero_num);
        this.tvHeroPlay = (TextView) findViewById(R.id.tv_hero_play);
        this.tvHeroFrames = (TextView) findViewById(R.id.tv_hero_frames);
        this.tvShizukuState = (TextView) findViewById(R.id.tv_shizuku_state);
        this.tvBatteryState = (TextView) findViewById(R.id.tv_battery_state);
        this.tvUsageState = (TextView) findViewById(R.id.tv_usage_state);
        // v1.10.102（反馈 3）：点一下先让图标动画完整播完，再进设置页。
        // 之前是「点完立刻 startActivity」→ 页面瞬间被盖住，动画等于没有。
        // 冷却期内连点：既不重播动画，也不会开出第二个设置页。
        View settingsMark = findViewById(R.id.btn_settings);
        settingsMark.setOnClickListener((v1) -> {
            if (v1 instanceof TuneMarkView) {
                if (!((TuneMarkView) v1).nudge()) {
                    return;
                }
            }
            v1.postDelayed(() -> onCreate$lambda$0(MainActivity.this, v1),
                    TuneMarkView.OPEN_DELAY);
        });
        ((View) findViewById(R.id.btn_add)).setOnClickListener((v1) -> {
            onCreate$lambda$1(MainActivity.this, v1);
        });
        ((View) findViewById(R.id.btn_shizuku)).setOnClickListener((v1) -> {
            onCreate$lambda$2(MainActivity.this, v1);
        });
        ((View) findViewById(R.id.btn_battery)).setOnClickListener((v1) -> {
            onCreate$lambda$3(MainActivity.this, v1);
        });
        // v1.10.392：锚点原来是首页「02 运行环境」表格里的一格 Switch。
        // v1.10.497（用户要求）：整格搬进「设置 → 性能 → 性能锚点」（带副标题），
        // 首页这边连同分隔线一起删除 —— 这里不再有 cb_anchor，任何代码都不许再 findViewById 它。
        // v1.10.69：均衡模式开关**已删除**。
        // 实测（日志 05:45:17 均衡 / 05:45:51 零掉帧）两种模式帧率完全一样（97~99/100 帧、单帧 4~8ms），
        // 一个勾了看不出差别的开关只是负担；省电真正靠的是「离开桌面立即停播」。
        ((View) findViewById(R.id.btn_usage)).setOnClickListener((v1) -> {
            onCreate$lambda$5(MainActivity.this, v1);
        });
        Button button = this.btnToggle;
        if (button == null) {
            Intrinsics.throwUninitializedPropertyAccessException("btnToggle");
            button = null;
        }
        button.setOnClickListener((v1) -> {
            onCreate$lambda$6(MainActivity.this, v1);
        });
        // v1.10.98（需求 3）：第一次进入应用 → 先给一屏「开屏说明」，返回即回到首页
        if (CreatorIntroActivity.shouldShow(this)) {
            CreatorIntroActivity.open(this);
        }
    }

    private static final void onCreate$lambda$0(MainActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.startActivity(new Intent(this$0, (Class<?>) SettingsActivity.class));
    }

    private static final void onCreate$lambda$1(MainActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.addWidget();
    }

    private static final void onCreate$lambda$2(MainActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.runShizuku();
    }

    private static final void onCreate$lambda$3(MainActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.requestBatteryWhitelist();
    }

    private static final void onCreate$lambda$4(MainActivity this$0, CompoundButton compoundButton, boolean checked) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.getSharedPreferences("settings", 0).edit().putBoolean("anchor_enabled", checked).apply();
        if (checked) {
            OverlayService.Companion.refresh(this$0);
        }
    }

    private static final void onCreate$lambda$5(MainActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.requestUsageAccess();
    }

    private static final void onCreate$lambda$6(MainActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.togglePlayback();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void refreshThemeInPlace() {
        ThemeManager themeManager = ThemeManager.INSTANCE;
        View viewFindViewById = findViewById(android.R.id.content);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById, "findViewById(...)");
        themeManager.applyTo(viewFindViewById);
        ThemeManager.INSTANCE.applySystemBars(this);
    }

    @Override // android.app.Activity, android.content.ComponentCallbacks
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        Intrinsics.checkNotNullParameter(newConfig, "newConfig");
        super.onConfigurationChanged(newConfig);
        this.appliedDark = ThemeManager.INSTANCE.isDark(this);
        // v1.10.103（反馈）：系统切深浅色 / 配置变化 → 整页叠化，不要硬刷
        Morph.crossfade(this, () -> refreshThemeInPlace());
    }

    @Override // android.app.Activity
    protected void onStart() {
        super.onStart();
        DeskWatcher.setAppForeground(true);
    }

    @Override // android.app.Activity
    protected void onStop() {
        DeskWatcher.setAppForeground(false);
        super.onStop();
    }

    @Override // android.app.Activity
    protected void onPause() {
        getWindow().getDecorView().removeCallbacks(this.themePoll);
        super.onPause();
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().removeCallbacks(this.themePoll);
        getWindow().getDecorView().post(this.themePoll);
        // v1.10.1（需求 9）：语言变了就地换文案，不重建、不丢状态、不闪
        syncLocale();
        boolean dark = ThemeManager.INSTANCE.isDark(this);
        if (dark != this.appliedDark) {
            this.appliedDark = dark;
            refreshThemeInPlace();
            return;
        }
        refresh();
        SharedPreferences prefs = getSharedPreferences("settings", 0);
        if (!prefs.getBoolean("all_stopped", false) && prefs.getBoolean(OverlayService.PREF_ANCHOR, true) && Settings.canDrawOverlays(this)) {
            OverlayService.Companion.refresh(this);
        }
        updateDiagnostics();
    }

    private final void refresh() {
        // v1.10.316：先读一次「系统认为还活着的组件 id」，全流程只用这一份（原来 refresh 里读了两次，
        // 两次之间桌面状态可能已经变，列表和判据会不一致）。
        int[] liveArray = WidgetProvider.Companion.widgetIds(this);
        Set liveSet = new HashSet();
        for (int liveId : liveArray) {
            liveSet.add(Integer.valueOf(liveId));
        }
        // 桌面已经移除的配置：能确定的话直接清掉（见 purgeDeadConfigs 的三条护栏）
        int purged = purgeDeadConfigs(liveSet, liveArray.length > 0);

        List configs = WidgetStore.INSTANCE.all(this);
        LinearLayout linearLayout = this.llWidgets;
        if (linearLayout == null) {
            Intrinsics.throwUninitializedPropertyAccessException("llWidgets");
            linearLayout = null;
        }
        linearLayout.removeAllViews();
        TextView textView = this.tvEmpty;
        if (textView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("tvEmpty");
            textView = null;
        }
        textView.setVisibility(configs.isEmpty() && liveArray.length == 0 ? 0 : 8);
        Set liveIds = liveSet;
        this.missingConfigs.clear();
        for (Object configObj : configs) {
            WidgetConfig config = (WidgetConfig) configObj;
            boolean live = liveIds.contains(Integer.valueOf(config.getWidgetId()));
            LinearLayout linearLayout2 = this.llWidgets;
            if (linearLayout2 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("llWidgets");
                linearLayout2 = null;
            }
            linearLayout2.addView(buildRow(config, live));
            if (!live) {
                this.missingConfigs.add(config);
            }
        }
        if (!this.missingConfigs.isEmpty()) {
            addPurgeRow();
        }
        if (purged > 0) {
            toast(getString(R.string.main_purged, new Object[]{Integer.valueOf(purged)}));
        }
        // 桌面上已经有、但还没配置过的组件（启动器跳过配置页时会出现）也要列出来，
        // 否则用户加了组件却在 App 里找不到入口去配它。
        List<Integer> orphans = new ArrayList<>();
        Set<Integer> configuredIds = new HashSet<>();
        for (Object configObj2 : configs) {
            configuredIds.add(Integer.valueOf(((WidgetConfig) configObj2).getWidgetId()));
        }
        for (int liveId : WidgetProvider.Companion.widgetIds(this)) {
            if (!configuredIds.contains(Integer.valueOf(liveId))) {
                orphans.add(Integer.valueOf(liveId));
            }
        }
        for (Integer orphanId : orphans) {
            LinearLayout linearLayout3 = this.llWidgets;
            if (linearLayout3 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("llWidgets");
                linearLayout3 = null;
            }
            linearLayout3.addView(buildRow(unconfigured(liveIdConfig(orphanId.intValue())), true));
        }
        boolean stopped = getSharedPreferences("settings", 0).getBoolean("all_stopped", false);
        Button button = this.btnToggle;
        if (button == null) {
            Intrinsics.throwUninitializedPropertyAccessException("btnToggle");
            button = null;
        }
        button.setText(getString(stopped ? R.string.main_start_all : R.string.main_stop_all));
        // v1.10.392：01 区块的主数字 = 列表里实际有几行（配置 + 桌面有但没配的）；
        // 副行 = 其中几个真在播放（全停时一律 0，因为此时一个都不播）。
        // 全是本地已有读数，没有新增任何查询。
        int total = configs.size() + orphans.size();
        // 「在播放」只数**既有配置、又在桌面上**的那些 —— liveArray 里可能有还没配置的
        // （启动器跳过配置页），它们并不会动；全停时一律 0。
        int playing = 0;
        if (!stopped) {
            for (Object configObj3 : configs) {
                if (liveIds.contains(Integer.valueOf(((WidgetConfig) configObj3).getWidgetId()))) {
                    playing++;
                }
            }
        }
        int frames = 0;
        for (Object configObj3 : configs) {
            int n = ((WidgetConfig) configObj3).getFrameCount();
            if (n > 0) {
                frames += n;
            }
        }
        setHeroValue(this.tvHeroNum, total);
        setHeroValue(this.tvHeroPlay, playing);
        setHeroValue(this.tvHeroFrames, frames);
        updateEnvStates();
        ThemeManager themeManager = ThemeManager.INSTANCE;
        View viewFindViewById = findViewById(android.R.id.content);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById, "findViewById(...)");
        themeManager.applyTo(viewFindViewById);
        ThemeManager.INSTANCE.applySystemBars(this);
    }

    /**
     * v1.10.316：清掉「桌面组件已经不在了」的配置。
     *
     * 用户问的是「桌面已移除的为什么还显示在这里」。根因是 {@code WidgetProvider.onDeleted}
     * 是唯一的自动删除路径，而 vivo 桌面（com.bbk.launcher2）并不总是把它发出来 —
     * 于是配置和帧文件就一直留着。
     *
     * 为什么不直接"看不见就删"：`getAppWidgetIds()` 在某些时机（刚开机、系统服务没就绪、
     * 换启动器）会返回**空数组**，那时所有配置看起来都"不在桌面上"，一键全删就是灾难。
     * 所以只在这一个条件下自动删：**本次至少看见 1 个活着的组件** —— 说明服务确实在回话，
     * 那么缺的那个就是真没了。零存活时一个都不删，改由列表顶部「清理已移除的配置（N）」手动确认。
     */
    private final int purgeDeadConfigs(Set liveSet, boolean serviceAlive) {
        if (!serviceAlive) {
            return 0;
        }
        try {
            List configs = WidgetStore.INSTANCE.all(this);
            if (configs.isEmpty()) {
                return 0;
            }
            int purged = 0;
            for (Object configObj : configs) {
                WidgetConfig config = (WidgetConfig) configObj;
                if (!liveSet.contains(Integer.valueOf(config.getWidgetId()))) {
                    WidgetStore.INSTANCE.delete(this, config.getWidgetId());
                    purged++;
                }
            }
            return purged;
        } catch (Throwable th) {
            return 0;
        }
    }

    /** v1.10.316：列表顶部的「清理已移除的配置（N）」——零存活时的显式出口。 */
    private final void addPurgeRow() {
        LinearLayout host = this.llWidgets;
        if (host == null || this.missingConfigs.isEmpty()) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        Button button = new Button(this);
        // v1.10.392：这一行也住进新版式的表格里 —— 左边缘 42dp、高 48dp、
        // 62% 灰的文字（不是主色链接：它是清理动作，不该比「编辑」更亮）。
        button.setPadding((int) (42 * density), 0, (int) (20 * density), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, (int) (48 * density));
        button.setLayoutParams(lp);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setIncludeFontPadding(false);
        button.setGravity(16);
        button.setTextSize(11.5f);
        button.setText(getString(R.string.main_purge_missing,
                new Object[]{Integer.valueOf(this.missingConfigs.size())}));
        button.setTag("ghost_dim");
        button.setOnClickListener((v) -> confirmPurgeMissing());
        host.addView(button, 0);
    }

    private final void confirmPurgeMissing() {
        final int n = this.missingConfigs.size();
        if (n <= 0) {
            return;
        }
        new ConfirmDialog(this)
                .title(getString(R.string.main_purge_title))
                .message(getString(R.string.main_purge_confirm, new Object[]{Integer.valueOf(n)}))
                .confirm(getString(R.string.main_delete))
                .danger(true)
                .glyph(GlyphView.ALERT)
                .onConfirm(() -> purgeMissingNow())
                .show();
    }

    private final void purgeMissingNow() {
        int n = 0;
        for (Object configObj : new ArrayList(this.missingConfigs)) {
            WidgetConfig config = (WidgetConfig) configObj;
            try {
                WidgetStore.INSTANCE.delete(this, config.getWidgetId());
                n++;
            } catch (Throwable ignored) {
            }
        }
        this.missingConfigs.clear();
        refresh();
        toast(getString(R.string.main_purged, new Object[]{Integer.valueOf(n)}));
    }

    /** 桌面有、无配置的组件：构造一个默认配置，名字用「待配置」占位。 */
    private final WidgetConfig liveIdConfig(int widgetId) {
        return new WidgetConfig(widgetId, null, null, null, 0, 0, 0, 0, 0, 0, null, 0, false, null,
                false, false, 0, 0, 0, 0, 0, 0, 0, false, 0.0f, 0.0f, 0, 0.0f, 0.0f, null, 536870910, null);
    }

    private final WidgetConfig unconfigured(WidgetConfig config) {
        String label = getString(R.string.main_row_unconfigured);
        Intrinsics.checkNotNullExpressionValue(label, "getString(...)");
        return new WidgetConfig(config.getWidgetId(), config.getKind(), label, config.getSourceUri(),
                config.getFps(), config.getMaxEdge(), config.getQuality(), config.getFrameCount(),
                config.getFrameWidth(), config.getFrameHeight(), config.getShape(), config.getShapeAlpha(),
                config.getBorder(), config.getLayout(), config.getOverlayEnabled(), config.getOverlayVisible(),
                config.getOverlayX(), config.getOverlayY(), config.getOverlayWidth(), config.getScalePercent(),
                config.getShadowPercent(), config.getRotation(), config.getShapeRotation(),
                config.getFitContent(), config.getOffsetXPercent(), config.getOffsetYPercent(),
                config.getContentScalePercent(), config.getMaskOffsetXPercent(), config.getMaskOffsetYPercent(),
                config.getShadowCurve());
    }

    /**
     * v1.10.392：组件行 —— 从「卡片」改成**表格行**（与 02 运行环境同一套线）。
     *
     * <p>行内仍是两行信息 + 右侧两枚动作：
     * <pre>
     *   名称                                   [编辑] [删除]
     *   形状 · 120 帧 · 24 fps
     * </pre>
     * 但行本身没有背景、没有圆角：左边缘 42dp 落在全页那条栏线上，行间一条贯通
     * 左右边缘的 1dp 发丝线。动作按钮 34dp / 11.5sp；「编辑」是软胶囊，「删除」
     * 是 62% 灰的文字（删除不该比编辑更显眼，它还有一层确认弹窗兜着）。
     * 颜色全部走 tag（pill_soft / ghost_dim / dim），换配色这一页跟着换。
     */
    private final View buildRow(WidgetConfig config, boolean installed) throws NoWhenBranchMatchedException {
        float density = getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(0);
        row.setGravity(16);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        // v1.10.392：行不再是卡片。原来每行一块 22dp 圆角的 surface —— 一屏三行就是
        // 三个悬浮块，和「不要卡片、只用线分栏」的版式直接打架。现在：
        //   左边缘 42dp（与 01/02/03 各区块的正文同一条栏线）、右边缘 20dp、
        //   行高 58dp、行间一条贯通左右边缘的发丝线（与 02 表格同一种线）。
        row.setBackground(null);
        row.setPadding((int) (42 * density), (int) (9 * density), (int) (20 * density), (int) (9 * density));
        row.setMinimumHeight((int) (58 * density));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        row.setLayoutParams(lp);

        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(1);
        textBox.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));

        TextView title = new TextView(this);
        String sourceName = config.getSourceName();
        String name = StringsKt.isBlank(sourceName) ? config.getKind().name() : sourceName;
        if (!installed) {
            name = name + getString(R.string.main_row_removed);
        }
        title.setText(name);
        title.setTextSize(13.5f);
        title.setMaxLines(1);
        title.setEllipsize(TextUtils.TruncateAt.END);
        textBox.addView(title, new LinearLayout.LayoutParams(-1, -2));

        String shape = config.getShape().label(this);
        String meta = config.getKind() == MediaKind.IMAGE
                ? getString(R.string.main_row_meta_image, new Object[]{shape})
                : getString(R.string.main_row_meta, new Object[]{shape,
                Integer.valueOf(config.getFrameCount()), Integer.valueOf(config.getFps())});
        TextView metaView = new TextView(this);
        metaView.setText(meta);
        metaView.setTextSize(11.5f);
        metaView.setTag("dim");
        metaView.setMaxLines(1);
        metaView.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(-1, -2);
        metaLp.topMargin = (int) (3 * density);
        textBox.addView(metaView, metaLp);

        Button edit = new Button(this);
        edit.setLayoutParams(new LinearLayout.LayoutParams(-2, (int) (34 * density)));
        edit.setMinHeight(0);
        edit.setMinimumHeight(0);
        edit.setMinWidth(0);
        edit.setMinimumWidth(0);
        edit.setIncludeFontPadding(false);
        edit.setTextSize(11.5f);
        edit.setAllCaps(false);
        edit.setPadding((int) (14 * density), 0, (int) (14 * density), 0);
        edit.setText(getString(R.string.main_edit));
        edit.setTag("pill_soft");
        edit.setOnClickListener((v2) -> buildRow$lambda$11$lambda$10(MainActivity.this, config, v2));

        Button delete = new Button(this);
        delete.setLayoutParams(new LinearLayout.LayoutParams(-2, (int) (34 * density)));
        delete.setMinHeight(0);
        delete.setMinimumHeight(0);
        delete.setMinWidth(0);
        delete.setMinimumWidth(0);
        delete.setIncludeFontPadding(false);
        delete.setTextSize(11.5f);
        delete.setAllCaps(false);
        delete.setPadding((int) (12 * density), 0, (int) (12 * density), 0);
        delete.setText(getString(R.string.main_delete));
        delete.setTag("ghost_dim");
        delete.setOnClickListener((v2) -> buildRow$lambda$13$lambda$12(MainActivity.this, config, v2));

        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(-2, (int) (34 * density));
        editLp.leftMargin = (int) (8 * density);
        row.addView(textBox);
        row.addView(edit, editLp);
        row.addView(delete);

        // v1.10.392：行与行之间那条线必须**贯通左右边缘**（同 02 区块的表格线）。
        // 所以返回的是「行 + 线」这一格，而不是行本身 —— 线在行外面才不会被行的内边距缩掉。
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(1);
        cell.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        cell.addView(row);
        View line = new View(this);
        line.setTag("divider");
        line.setBackgroundColor(0);
        line.setLayoutParams(new LinearLayout.LayoutParams(-1, Math.max(1, (int) (1 * density))));
        cell.addView(line);
        return cell;
    }

    private static final void buildRow$lambda$11$lambda$10(MainActivity this$0, WidgetConfig $config, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter($config, "$config");
        this$0.openConfig($config.getWidgetId());
    }

    private static final void buildRow$lambda$13$lambda$12(MainActivity this$0, WidgetConfig $config, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter($config, "$config");
        this$0.confirmDelete($config);
    }

    private final void addWidget() {
        // v1.10.250：与长按快捷方式共用 WidgetPin —— 一份逻辑、一种提示。
        // 关键实测：vivo 桌面 isRequestPinAppWidgetSupported()=true 但 requestPin 无动作，
        // 所以**不能**只在"不支持"时才提示；路径提示一律给，否则用户点完没有任何反馈。
        int result = WidgetPin.request(this);
        String reason = null;
        if (result == WidgetPin.UNSUPPORTED) {
            reason = getString(R.string.main_add_unsupported);
        } else if (result == WidgetPin.REJECTED) {
            reason = getString(R.string.main_add_rejected);
        } else if (result == WidgetPin.FAILED) {
            reason = getString(R.string.main_add_failed);
        }
        String hint = WidgetPin.hint(this);
        toast(reason == null ? hint : getString(R.string.main_add_reason, new Object[]{reason}) + "\n" + hint);
    }

    private final void openConfig(int widgetId) {
        startActivity(new Intent(this, (Class<?>) ConfigActivity.class).putExtra(ConfigActivity.EXTRA_WIDGET_ID, widgetId));
    }

    private final void confirmDelete(final WidgetConfig config) {
        // v1.10.140：与「恢复预设」同一个 ConfirmDialog（危险色），不再弹系统方框
        new ConfirmDialog(this)
                .title(getString(R.string.main_delete))
                .message(getString(R.string.main_delete_confirm, new Object[]{config.getSourceName()}))
                .confirm(getString(R.string.main_delete))
                .danger(true)
                .glyph(GlyphView.ALERT)
                .onConfirm(() -> confirmDelete$lambda$14(MainActivity.this, config, null, 0))
                .show();
    }

    private static final void confirmDelete$lambda$14(MainActivity this$0, WidgetConfig $config, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter($config, "$config");
        WidgetStore.INSTANCE.delete(this$0, $config.getWidgetId());
        this$0.refresh();
        String string = this$0.getString(R.string.main_deleted);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        this$0.toast(string);
    }

    private final void togglePlayback() {
        SharedPreferences prefs = getSharedPreferences("settings", 0);
        boolean stopped = prefs.getBoolean("all_stopped", false);
        if (!stopped) {
            prefs.edit().putBoolean("all_stopped", true).apply();
            PlaybackService.Companion.stop(this);
            OverlayService.Companion.stop(this);
            String string = getString(R.string.main_stopped_all);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            toast(string);
        } else {
            prefs.edit().putBoolean("all_stopped", false).apply();
            if (!(WidgetProvider.Companion.widgetIds(this).length == 0)) {
                PlaybackService.Companion.start(this);
            }
            OverlayService.Companion.refresh(this);
            String string2 = getString(R.string.main_started_all);
            Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
            toast(string2);
        }
        Button button = this.btnToggle;
        if (button == null) {
            Intrinsics.throwUninitializedPropertyAccessException("btnToggle");
            button = null;
        }
        button.postDelayed(() -> {
            togglePlayback$lambda$15(MainActivity.this);
        }, 400L);
    }

    private static final void togglePlayback$lambda$15(MainActivity this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.refresh();
    }

    @Override
    protected void onTextsRefreshed() {
        try {
            refresh();
            updateDiagnostics();
        } catch (Throwable ignored) {
        }
    }

    /**
     * v1.10.392：仪表格里的数字。
     *
     * <p>30sp 等宽字在 1/3 屏宽（约 82dp）里只装得下 4 位数；5 位起降到 24sp，
     * 否则「12840 帧」会把右边那条竖线顶穿。两种字号都是 4 位数封顶前的实测值。</p>
     */
    private static void setHeroValue(TextView view, int value) {
        if (view == null) {
            return;
        }
        String s = String.valueOf(value);
        view.setText(s);
        view.setTextSize(s.length() >= 5 ? 24.0f : 30.0f);
    }

    /**
     * v1.10.392：02 区块右栏的三个状态字。
     *
     * <p>三个都是**只读查询**（Shizuku 是否连着 / 电池是否放行 / 使用情况访问是否授权），
     * 不触发权限申请、不改任何状态 —— 点行仍然是原来的行为。每次 refresh 刷一次，
     * 所以从系统设置页返回时 onResume → refresh → 这里，用户能立刻看到状态变了。</p>
     */
    private final void updateEnvStates() {
        if (this.tvShizukuState != null) {
            boolean ready = false;
            try {
                ready = ShizukuHelper.INSTANCE.isReady();
            } catch (Throwable ignored) {
            }
            this.tvShizukuState.setText(getString(ready
                    ? R.string.main_state_ready : R.string.main_state_idle));
        }
        if (this.tvBatteryState != null) {
            boolean ok = false;
            try {
                PowerManager pm = (PowerManager) getSystemService(PowerManager.class);
                ok = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
            } catch (Throwable ignored) {
            }
            this.tvBatteryState.setText(getString(ok
                    ? R.string.main_state_on : R.string.main_state_off));
        }
        if (this.tvUsageState != null) {
            boolean granted = false;
            try {
                granted = DeskWatcher.INSTANCE.hasAccess(this);
            } catch (Throwable ignored) {
            }
            this.tvUsageState.setText(getString(granted
                    ? R.string.diag_granted : R.string.diag_denied));
        }
    }

    private final void updateDiagnostics() {
        DeskWatcher.INSTANCE.tick(this);
        String granted = getString(DeskWatcher.INSTANCE.hasAccess(this) ? R.string.diag_granted : R.string.diag_denied);
        Intrinsics.checkNotNullExpressionValue(granted, "getString(...)");
        String unknown = getString(R.string.diag_unknown);
        Intrinsics.checkNotNullExpressionValue(unknown, "getString(...)");
        String condition = getString(DeskWatcher.INSTANCE.getCanPlay() ? R.string.diag_yes : R.string.diag_no);
        Intrinsics.checkNotNullExpressionValue(condition, "getString(...)");
        TextView textView = this.tvDiag;
        if (textView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("tvDiag");
            textView = null;
        }
        StringBuilder $this$updateDiagnostics_u24lambda_u2416 = new StringBuilder();
        TextView textView2 = textView;
        $this$updateDiagnostics_u24lambda_u2416.append(getString(R.string.diag_monitor, new Object[]{granted}));
        StringBuilder sbAppend = $this$updateDiagnostics_u24lambda_u2416.append("\n");
        Object[] objArr = new Object[1];
        String strLauncher = DeskWatcher.INSTANCE.launcher(this);
        if (strLauncher == null) {
            strLauncher = unknown;
        }
        objArr[0] = strLauncher;
        sbAppend.append(getString(R.string.diag_launcher, objArr));
        StringBuilder sbAppend2 = $this$updateDiagnostics_u24lambda_u2416.append("\n");
        Object[] objArr2 = new Object[1];
        String strForeground = DeskWatcher.INSTANCE.foreground();
        if (strForeground == null) {
            strForeground = unknown;
        }
        objArr2[0] = strForeground;
        sbAppend2.append(getString(R.string.diag_foreground, objArr2));
        $this$updateDiagnostics_u24lambda_u2416.append("\n").append(getString(R.string.diag_condition, new Object[]{condition}));
        String string = $this$updateDiagnostics_u24lambda_u2416.toString();
        Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
        // v1.10.1（需求 6）：崩溃报告统一进「日志」页，主界面不再显示、也不再留旧文件
        CrashLog.purgeLegacy(this);
        // v1.10.2（需求 6）：主界面只显示「桌面监测状态」，不再显示任何日志/崩溃正文
        // —— 日志（含崩溃）只在「设置 → 日志」页，单一数据源。
        textView2.setText(string);
    }

    private final void runShizuku() {
        ShizukuHelper.INSTANCE.init();
        if (!ShizukuHelper.INSTANCE.isReady()) {
            TextView textView = this.tvBackend;
            if (textView == null) {
                Intrinsics.throwUninitializedPropertyAccessException("tvBackend");
                textView = null;
            }
            textView.setText(getString(R.string.shizuku_missing));
            return;
        }
        ShizukuHelper.INSTANCE.requestPermission(this, new AnonymousClass1(this));
    }

    /* compiled from: MainActivity.kt */
    @Metadata(mv = {1, 9, 0}, k = 3, xi = 48, d1 = {"��\u000e\n��\n\u0002\u0010\u0002\n��\n\u0002\u0010\u000b\n��\u0010��\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\n¢\u0006\u0002\b\u0004"}, d2 = {"<anonymous>", "", "granted", "", "invoke"})
    /* renamed from: io.github.animwidget.MainActivity$runShizuku$1, reason: invalid class name */
    /* loaded from: classes.jar:io/github/animwidget/MainActivity$runShizuku$1.class */
    static final class AnonymousClass1 implements Function1<Boolean, Unit> {
        final MainActivity this$0;

        AnonymousClass1(MainActivity outer) {
            this.this$0 = outer;
        }

        public final Unit invoke(Boolean granted) {
            final boolean g = granted != null && granted.booleanValue();
            this.this$0.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    invoke$lambda$2(g, AnonymousClass1.this.this$0);
                }
            });
            return Unit.INSTANCE;
        }

        private static final void invoke$lambda$2(boolean $granted, MainActivity this$0) {
            Intrinsics.checkNotNullParameter(this$0, "this$0");
            if (!$granted) {
                TextView textView = this$0.tvBackend;
                if (textView == null) {
                    Intrinsics.throwUninitializedPropertyAccessException("tvBackend");
                    textView = null;
                }
                textView.setText(this$0.getString(R.string.shizuku_denied));
                return;
            }
            TextView textView2 = this$0.tvBackend;
            if (textView2 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("tvBackend");
                textView2 = null;
            }
            textView2.setText(this$0.getString(R.string.shizuku_running));
            String pkg = this$0.getPackageName();
            String launcher = this$0.resolveLauncherPackage();
            new Thread(() -> {
                invoke$lambda$2$lambda$1(pkg, launcher, this$0);
            }).start();
        }

        private static final void invoke$lambda$2$lambda$1(String $pkg, String $launcher, MainActivity this$0) {
            Intrinsics.checkNotNullParameter(this$0, "this$0");
            ShizukuHelper shizukuHelper = ShizukuHelper.INSTANCE;
            ShizukuHelper shizukuHelper2 = ShizukuHelper.INSTANCE;
            Intrinsics.checkNotNull($pkg);
            String report = shizukuHelper.runCommands(shizukuHelper2.backgroundCommands($pkg, $launcher));
            this$0.runOnUiThread(() -> {
                invoke$lambda$2$lambda$1$lambda$0(this$0, report);
            });
        }

        private static final void invoke$lambda$2$lambda$1$lambda$0(MainActivity this$0, String $report) {
            Intrinsics.checkNotNullParameter(this$0, "this$0");
            Intrinsics.checkNotNullParameter($report, "$report");
            TextView textView = this$0.tvBackend;
            if (textView == null) {
                Intrinsics.throwUninitializedPropertyAccessException("tvBackend");
                textView = null;
            }
            textView.setText($report);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final String resolveLauncherPackage() {
        String str;
        ActivityInfo activityInfo;
        try {
            Intent home = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME");
            Intrinsics.checkNotNullExpressionValue(home, "addCategory(...)");
            ResolveInfo resolveInfoResolveActivity = getPackageManager().resolveActivity(home, 65536);
            str = (resolveInfoResolveActivity == null || (activityInfo = resolveInfoResolveActivity.activityInfo) == null) ? null : activityInfo.packageName;
        } catch (Throwable th) {
            str = null;
        }
        return str;
    }

    private final void requestUsageAccess() {
        if (DeskWatcher.INSTANCE.hasAccess(this)) {
            TextView textView = this.tvBackend;
            if (textView == null) {
                Intrinsics.throwUninitializedPropertyAccessException("tvBackend");
                textView = null;
            }
            textView.setText(getString(R.string.usage_ok));
            return;
        }
        try {
            startActivity(new Intent("android.settings.USAGE_ACCESS_SETTINGS"));
        } catch (Throwable th) {
            TextView textView2 = this.tvBackend;
            if (textView2 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("tvBackend");
                textView2 = null;
            }
            textView2.setText(getString(R.string.usage_ok));
        }
    }

    private final void requestBatteryWhitelist() {
        PowerManager power = (PowerManager) getSystemService(PowerManager.class);
        if (power != null && power.isIgnoringBatteryOptimizations(getPackageName())) {
            TextView textView = this.tvBackend;
            if (textView == null) {
                Intrinsics.throwUninitializedPropertyAccessException("tvBackend");
                textView = null;
            }
            textView.setText(getString(R.string.battery_ok));
            return;
        }
        try {
            // v1.10.355（上架合规）：**不再用 REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 权限**。
            //
            // 原来跳的是 `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` —— 那个入口要求 manifest
            // 里声明同名权限，而 Google Play 对该权限有品类白名单（VoIP、系统工具等），
            // 桌面小组件不在其中，属于「声明了就可能被人工审核驳回」的高风险项。
            //
            // 现在改成：
            //   · API 30+ → `ACTION_APP_BATTERY_SETTINGS`（直接跳到**本应用**的电池设置页，
            //     用户在那里关掉「后台限制/省电策略」），**不需要任何权限**；
            //   · 更低版本 → `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`（电池优化列表页），
            //     同样不需要权限，代价是用户要自己在列表里找。
            // `PowerManager.isIgnoringBatteryOptimizations()` 是**查询**，本来就不需要该权限。
            int sdk = android.os.Build.VERSION.SDK_INT;
            if (sdk >= 30) {
                // 用字符串常量：编译用的 android-all 存根里没有 ACTION_APP_BATTERY_SETTINGS
                Intent battery = new Intent("android.settings.APP_BATTERY_SETTINGS");
                battery.putExtra("android.intent.extra.PACKAGE_NAME", getPackageName());
                startActivity(battery);
            } else {
                startActivity(new Intent(
                        "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS"));
            }
        } catch (Throwable t) {
            String message = t.getMessage();
            if (message == null) {
                message = getString(R.string.battery_failed);
                Intrinsics.checkNotNullExpressionValue(message, "getString(...)");
            }
            toast(message);
        }
    }

    private final void toast(String message) {
        Toast.makeText(this, message, 0).show();
    }
}
