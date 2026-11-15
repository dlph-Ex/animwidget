package io.github.animwidget;
import android.widget.SeekBar.OnSeekBarChangeListener;
import kotlin.jvm.internal.Ref.IntRef;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import io.github.animwidget.ThemeManager;
import java.util.Arrays;
import java.util.Locale;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Lambda;
import kotlin.jvm.internal.Ref;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.text.CharsKt;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* compiled from: SettingsActivity.kt */
@Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"��i\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n��\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n��\n\u0002\u0010\u0002\n��\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\n*\u0001\u0011\u0018��2\u00020\u0001B\u0005¢\u0006\u0002\u0010\u0002J \u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u00182\u0006\u0010\u001a\u001a\u00020\u001bH\u0002J\u0010\u0010\u001c\u001a\u00020\u00162\u0006\u0010\u001d\u001a\u00020\u001eH\u0014J\b\u0010\u001f\u001a\u00020\u0016H\u0002J\b\u0010 \u001a\u00020\u0016H\u0002J\u0010\u0010!\u001a\u00020\u00162\u0006\u0010\"\u001a\u00020#H\u0016J\u0012\u0010$\u001a\u00020\u00162\b\u0010%\u001a\u0004\u0018\u00010&H\u0014J\b\u0010'\u001a\u00020\u0016H\u0014J\b\u0010(\u001a\u00020\u0016H\u0014J\b\u0010)\u001a\u00020\u0016H\u0014J\b\u0010*\u001a\u00020\u0016H\u0002J \u0010+\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u00182\u0006\u0010,\u001a\u00020\u001bH\u0002J\b\u0010-\u001a\u00020\u0016H\u0002J\b\u0010.\u001a\u00020\u0016H\u0002J\b\u0010/\u001a\u00020\u0016H\u0002J\b\u00100\u001a\u00020\u0016H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u000e¢\u0006\u0002\n��R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.¢\u0006\u0002\n��R\u000e\u0010\u0007\u001a\u00020\bX\u0082.¢\u0006\u0002\n��R\u000e\u0010\t\u001a\u00020\nX\u0082.¢\u0006\u0002\n��R\u000e\u0010\u000b\u001a\u00020\nX\u0082.¢\u0006\u0002\n��R\u000e\u0010\f\u001a\u00020\bX\u0082.¢\u0006\u0002\n��R\u000e\u0010\r\u001a\u00020\u000eX\u0082.¢\u0006\u0002\n��R\u000e\u0010\u000f\u001a\u00020\u0004X\u0082\u000e¢\u0006\u0002\n��R\u0010\u0010\u0010\u001a\u00020\u0011X\u0082\u0004¢\u0006\u0004\n\u0002\u0010\u0012R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082.¢\u0006\u0002\n��"}, d2 = {"Lio/github/animwidget/SettingsActivity;", "Landroid/app/Activity;", "()V", "appliedDark", "", "cbInvert", "Landroid/widget/Switch;", "langGroup", "Landroid/widget/RadioGroup;", "llColors", "Landroid/widget/LinearLayout;", "llPresets", "modeGroup", "seekContrast", "Landroid/widget/SeekBar;", "syncingControls", "themePoll", "io/github/animwidget/Runnable", "Lio/github/animwidget/Runnable;", "tvContrast", "Landroid/widget/TextView;", "addColorRow", "", "label", "", "key", "color", "", "attachBaseContext", "newBase", "Landroid/content/Context;", "buildColorRows", "buildPresets", "onConfigurationChanged", "newConfig", "Landroid/content/res/Configuration;", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onPause", "onPostResume", "onResume", "refreshThemeInPlace", "showColorEditor", "initial", "showDevInfo", "showReward", "syncControls", "updateContrastLabel"})
@SourceDebugExtension({"SMAP\nSettingsActivity.kt\nKotlin\n*S Kotlin\n*F\n+ 1 SettingsActivity.kt\nio/github/animwidget/SettingsActivity\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n+ 3 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n+ 4 _Arrays.kt\nkotlin/collections/ArraysKt___ArraysKt\n*L\n1#1,638:1\n1#2:639\n1864#3,3:640\n13330#4,2:643\n*S KotlinDebug\n*F\n+ 1 SettingsActivity.kt\nio/github/animwidget/SettingsActivity\n*L\n367#1:640,3\n576#1:643,2\n*E\n"})
/* loaded from: classes.jar:io/github/animwidget/SettingsActivity.class */
public final class SettingsActivity extends I18nActivity {
    TextView tvContrast;
    SeekBar seekContrast;
    Switch cbInvert;
    LinearLayout llPresets;
    LinearLayout modeGroup;
    /** v1.10.159：语言面板不再是 RadioGroup（系统圆圈打不过），改成三行自绘卡片。 */
    LinearLayout langGroup;
    /** 上一次点语言行的时间（连点闸，320ms）。 */
    private long lastLangPickAt;
    boolean syncingControls;
    boolean appliedDark;

    @NotNull
    private final Runnable themePoll = new Runnable() { // from class: io.github.animwidget.Runnable
        @Override // java.lang.Runnable
        public void run() {
            boolean dark = ThemeManager.INSTANCE.isDark(SettingsActivity.this);
            if (dark != SettingsActivity.this.appliedDark) {
                SettingsActivity.this.appliedDark = dark;
                Morph.crossfade(SettingsActivity.this, () -> SettingsActivity.this.refreshThemeInPlace());
            }
            SettingsActivity.this.getWindow().getDecorView().postDelayed(this, 500L);
        }
    };
    LinearLayout llColors;

    // ---------------------------------------------------------------- v1.10.47
    /** 「系统模式」折叠面板 */
    View btnModeToggle;

    /** v1.10.139：性能卡里「相册扫描线程数」的数值视图（改完要就地刷新） */
    @org.jetbrains.annotations.Nullable
    TextView perfThreadValue;

    /** v1.10.191：性能卡里「预热线程数」的数值视图 */
    @org.jetbrains.annotations.Nullable
    TextView perfPrewarmValue;

    // ================================================================ v1.10.272 副标题收起/展开
    /**
     * 性能卡里「带副标题的行」——收起动画的作用对象（顺序 = 从上到下，用于错峰）。
     * 每次 {@code bindPerfCard} 重建都重新收集（行是代码动态建的）。
     */
    private final java.util.List<FoldRow> perfFoldRows = new java.util.ArrayList<>();

    /** 当前进度：0 = 副标题全展开，1 = 全部收起。**单一真值**，反向就从它往回走。 */
    private float perfFoldP = 0.0f;

    /** 正在跑的那条时间轴（反向/重建都要先 cancel，否则两套进度打架）。 */
    @org.jetbrains.annotations.Nullable
    private android.animation.ValueAnimator perfFoldAnim;

    /**
     * 一个「主标题 + 副标题 + 右侧控件」的行，以及收起时需要的**首尾两个状态**。
     *
     * v1.10.273：这里不再只存"两个高度"，而是把决定布局的每一个量都存下来
     * （文字块自然高 / 副标题占位 / 标题高 / 控件高 / padding）——
     * 因为主标题在行内的位置是**布局**算出来的，只改行高而不管它，
     * 就会在最后一帧出现"事后补位"（用户报的那一跳）。
     */
    static final class FoldRow {
        View row;
        View title;
        View hint;
        @org.jetbrains.annotations.Nullable
        View control;
        /** 副标题自己往主标题里挪多远（px，视觉用，不参与布局）。 */
        float shiftPx;
        /** 副标题的自然占位高度（px）——**唯一**会随动画变化的几何量。 */
        float slot0;
    }
    /** v1.10.152（反馈 2）：调色页打开期间由它接管系统栏，设置页不要把它抢回去。 */
    boolean colorEditorOpen;
    TextView tvModeValue;
    /** 自定义配色方案网格（行 = LinearLayout，每行最多 5 个图标） */
    LinearLayout llSchemes;
    View btnSchemesToggle;
    /** 底部动作条（保存：取消/确认；长按：删除/取消） */
    View schemeBar;
    Button barDelete;
    Button barCancel;
    Button barConfirm;
    /** v1.10.371：「前移 / 后移」（长按方案后重排用） */
    Button barMovePrev;
    Button barMoveNext;
    /** 胶囊按钮区的横向滚动容器 + 它下面那条灰线 */
    android.widget.HorizontalScrollView barScroll;
    View barIndicator;
    /** v1.10.378：指示线当前宽度 / 目标宽度 / 正在跑的宽度过渡。长度变化一律 0.5s Ease 过渡。 */
    private float indWidth = -1.0f;
    private int indTarget = -1;
    /** 补位动画期间锁住「目标长度」：只更新位置，长度等收尾后再一次性过渡（否则目标每帧变）。 */
    private boolean indLengthLocked;
    /**
     * v1.10.381：补位动画期间**禁止 layoutBarPages() 改第 2 页宽度**。
     *
     * <p>实测（A13 日志，09:58:23 那一批）：一帧里会有**两次** updateBarIndicator，
     * 内容宽度在 `2208`（= 2 页）与 `1656`（= 1.5 页，动画中的值）之间**来回跳**。
     * 原因是滚动视口的 layout 回调每帧都会调 layoutBarPages()，
     * 而那一刻「消失中」的按钮还是 VISIBLE → moveCount=2 → 它把页宽又写回整页，
     * 下一帧动画再写回插值 —— 页面宽度每帧来回摆，就是用户看到的跳闪。
     */
    private boolean barReflowBusy;
    /** 正在跑的补位/淡入淡出动画（新的一次移动开始前先取消，避免两段动画同时改宽度）。 */
    @Nullable
    private android.animation.ValueAnimator barReflowAnim;
    /** v1.10.380 诊断：上一次落地时的线宽/位移，用来抓「单帧跳变」。 */
    private int indLastW = -1;
    private float indLastX = Float.NaN;
    @Nullable
    private android.animation.ValueAnimator indAnim;
    /** v1.10.372：动作条的**两页**（第1页 删除/更新/取消，第2页 前移/后移） */
    LinearLayout barPage1;
    LinearLayout barPage2;
    View spacerBottom;
    /** v1.10.522：底部让位的高度动画（null = 没有动画在跑）。 */
    @Nullable
    private android.animation.ValueAnimator spacerAnim;
    /** v1.10.522：让位动画的目标高度（px，0 = 收起）。触摸打断时用它判断方向。 */
    private int spacerTargetPx;
    /** v1.10.523：关闭动效只跑一次（连点返回不会 finish 两次）。 */
    private boolean closing;
    /** 底部让位的高度（dp）—— 与 activity_settings.xml 里 spacer_bottom 的写死值一致。 */
    private static final int SPACER_DP = 104;
    /** 0 展开 / 1 半折叠（只留第一行）/ 2 全折叠 */
    private int schemeFold;
    /** 待删除的方案 id（-1 = 当前是保存流程） */
    private long pendingDeleteId = -1L;
    /** v1.10.92：动作条里的「更新」作用于哪个方案（-1 = 不是更新模式）。 */
    private long pendingUpdateId = -1L;
    /** v1.10.91：对比度滑杆的动画（切方案时前进/后退）。 */
    @Nullable
    private android.animation.ValueAnimator contrastAnim;
    private boolean contrastAnimating;

    // v1.10.266：拖动/滑杆动画期间的「实时叠化」预览（只在设置页生效，松手即清）
    private android.animation.ValueAnimator contrastPreviewAnim;
    private long contrastPreviewAt;
    /**
     * v1.10.176（反馈）：叠化进行中，对比度滑杆的曲线**延后**执行。
     * 见 {@link #applyScheme(long)}。
     */
    private boolean deferContrastCurve;
    /** 占位图标的稳定 key（动画配对用） */
    private static final long KEY_ADD = Long.MIN_VALUE;

    // ---------------------------------------------------------------- v1.10.98 字体
    /** 「字体」折叠面板（与上面两张卡同一套 animateExpand） */
    View btnFontToggle;
    TextView tvFontValue;
    LinearLayout llFonts;
    /** 删除动画进行中：挡住连点 / 挡切换字体 */
    private boolean fontDeleting;
    private static final int REQ_FONT_IMPORT = 30640;

    /**
     * v1.10.513（需求①·限制模式）：设置页顶部一条**不显眼**的提示（不是弹窗、不挡操作）。
     *
     * <p>用代码插而不是改 {@code activity_settings.xml}：改布局会牵动 stable-ids / 门禁 [13] 的
     * 两卡几何对拍，而这条提示只在非原版签名时存在 —— 没必要让正常构建去背这个风险。</p>
     */
    private void insertIntegrityNotice() {
        if (!isRestrictedBuild()) {
            return;
        }
        View rootView = findViewById(R.id.root_settings);
        if (!(rootView instanceof ScrollView)) {
            return;
        }
        View child = ((ScrollView) rootView).getChildAt(0);
        if (!(child instanceof LinearLayout)) {
            return;
        }
        LinearLayout column = (LinearLayout) child;
        float d = getResources().getDisplayMetrics().density;
        ThemeColors c = ThemeManager.INSTANCE.colors(this);
        TextView tv = new TextView(this);
        tv.setText(R.string.integrity_hint);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
        tv.setTextColor(c.getTextMuted());
        tv.setBackground(ThemeManager.INSTANCE.rounded(this,
                ThemeManager.INSTANCE.withAlpha(c.getError(), 0.14f), 12.0f));
        int pad = Math.round(12.0f * d);
        tv.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Math.round(10.0f * d);
        column.addView(tv, Math.min(1, column.getChildCount()), lp);
    }

    @Override // android.app.Activity
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setI18nLayout(R.layout.activity_settings);
        // v1.10.370：设置页改成「内容铺到屏幕最上沿」—— 顶部不再是一条窗口底色的带，
        // 滚动的卡片会从状态栏下面穿过去（「关闭」胶囊用 liftIds 把 inset 补回来，位置不变）
        // v1.10.400：导航栏也刷透明 —— 底部那条浮动动作条按 bottomMargin 抬起来
        EdgeToEdge.INSTANCE.applyScrollUnderBars(this, R.id.root_settings,
                R.id.btn_settings_close, R.id.scheme_action_bar);
        insertIntegrityNotice();
        this.appliedDark = ThemeManager.INSTANCE.isDark(this);
        View viewFindViewById = findViewById(R.id.tv_contrast);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById, "findViewById(...)");
        this.tvContrast = (TextView) viewFindViewById;
        View viewFindViewById2 = findViewById(R.id.seek_contrast);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById2, "findViewById(...)");
        this.seekContrast = (SeekBar) viewFindViewById2;
        View viewFindViewById3 = findViewById(R.id.cb_invert);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById3, "findViewById(...)");
        this.cbInvert = (Switch) viewFindViewById3;
        View viewFindViewById4 = findViewById(R.id.ll_presets);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById4, "findViewById(...)");
        this.llPresets = (LinearLayout) viewFindViewById4;
        View viewFindViewById5 = findViewById(R.id.ll_colors);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById5, "findViewById(...)");
        this.llColors = (LinearLayout) viewFindViewById5;
        ((Button) findViewById(R.id.btn_dev_info)).setOnClickListener((v1) -> {
            startActivity(new android.content.Intent(this, DeveloperActivity.class));
        });
        // v1.10.0：设置页最底部的「日志」入口
        View logsEntry = findViewById(R.id.btn_logs_entry);
        if (logsEntry != null) {
            logsEntry.setOnClickListener((v2) -> {
                try {
                    startActivity(new android.content.Intent(SettingsActivity.this, LogActivity.class));
                } catch (Throwable ignored) {
                }
            });
        }
        View viewFindViewById6 = findViewById(R.id.rg_mode);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById6, "findViewById(...)");
        // v1.10.317（用户："将系统模式的选择页面改为界面语言一样的"）：
        // 面板从 RadioGroup + 系统 RadioButton 换成「界面语言」那套自绘卡片
        // （LanguageRowView：标题 + 副行 + 落笔式勾 + 主色 12% 淡底 + 涟漪）。
        // 选中态不再走 RadioGroup.check()，统一由 refreshModeRows() 按存储值刷 ——
        // 和语言行完全同一条路径，也就不会再出现「勾在 A、实际是 B」。
        this.modeGroup = (LinearLayout) viewFindViewById6;
        bindModeRows();
        refreshModeRows();
        View viewFindViewById7 = findViewById(R.id.rg_language);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById7, "findViewById(...)");
        // v1.10.159：这里是 LinearLayout（三行自绘卡片），不再是 RadioGroup
        this.langGroup = (LinearLayout) viewFindViewById7;
        bindLanguageRows();
        refreshLanguageRows();
        SeekBar seekBar = this.seekContrast;
        if (seekBar == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekContrast");
            seekBar = null;
        }
        seekBar.setProgress(ThemePrefs.INSTANCE.getContrastX100(this) + 100);
        updateContrastLabel();
        SeekBar seekBar2 = this.seekContrast;
        if (seekBar2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("seekContrast");
            seekBar2 = null;
        }
        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: io.github.animwidget.SettingsActivity.onCreate.4
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(@Nullable SeekBar seekBar3, int value, boolean fromUser) {
                if (!fromUser && !SettingsActivity.this.contrastAnimating) {
                    // v1.10.269：**程序性 setProgress 只刷数字**（syncContrastSlider / 初始赋值 / 切方案时的同步）。
                    // 上一版这里会一路走下去 → 调 previewContrastTo() → 临时对比度被设上却**永远没人清**
                    // （没有"松手"事件），于是后面切方案时旧预览值一直压着存储值 → 界面在两种颜色之间闪。
                    SettingsActivity.this.updateContrastLabel(value - 100);
                    return;
                }
                if (fromUser) {
                    // 手拖：动画交还给用户，别跟手抢
                    SettingsActivity.this.cancelContrastAnimation();
                } else if (SettingsActivity.this.contrastAnimating) {
                    // v1.10.91：切方案时的滑杆动画中间帧 —— 只刷数值，不写盘
                    SettingsActivity.this.updateContrastLabel(value - 100);
                    // v1.10.269（反馈「切方案时界面/文字闪烁，也没有叠化」）：**这里绝不能推预览**。
                    //
                    // 切方案的顺序是：Morph 截图 → applySchemeInner（已经把新方案的对比度应用到界面）
                    // → 遮罩 300ms 淡出 → 再跑滑杆曲线（纯表现层，存储里早就是新值了）。
                    // 如果在滑杆曲线期间推预览，界面会先按"旧方案的对比度"回退、再逐步推到新值 ——
                    // 于是用户看到的是：叠化刚结束，颜色又跳回去重新走一遍 = 闪烁。
                    // 滑杆曲线只负责**滑杆和数字**，颜色只在「手指真的在拖」时才实时预览。
                    return;
                }
                // v1.10.47：对比度改两位小数，内部按 0.01 存
                // v1.10.90：对比度属于配色方案 —— 如果当前勾着某个方案，顺手写回该方案，
                //           勾选状态保持不动（以前这里不动、键盘输入那边却会清勾选，两边行为不一致）
                // v1.10.92：**只改运行时对比度**，不擅自写进已保存的方案
                // （要落到方案里，长按那个方案点「更新」；或者存成新方案）
                SettingsActivity.this.writeContrast(value - 100);   // v1.10.179：预设模式下同时写共享值
                SettingsActivity.this.updateContrastLabel(value - 100);
                // v1.10.266：**拖动中就地换色**（原来只有松手才刷 → 手感是"定格的"）。
                // 走 110ms 的补间 + 纯 applyTo（不重建结构），看起来是连续叠化而不是一跳一跳。
                SettingsActivity.this.previewContrastTo((value - 100) / 100f);
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(@Nullable SeekBar seekBar3) {
                // v1.10.267：留下"拖动真的开始了"的证据 —— 下一轮若还说没实时，
                // 就能分清是「回调没来」还是「换色没生效」。
                Diag.warn(SettingsActivity.this, "A08", "对比度拖动开始 当前="
                        + ThemePrefs.INSTANCE.getContrastX100(SettingsActivity.this));
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(@Nullable SeekBar seekBar3) {
                // v1.10.203（反馈「拖完对比度有的地方还停在旧对比度」）：
                // 这里原来是**手写的一段局部刷新** —— 只刷了方案网格 + 语义色行 + 一次 applyTo。
                // 局部刷新天然是子集：性能卡那几行的 4.5% 淡底色（裸 ViewGroup，paint() 不认）、
                // 方案图标描边、已修改勾选、语言行都不在里面。统一走**总入口**。
                Diag.warn(SettingsActivity.this, "A08", "对比度拖动结束 值="
                        + ThemePrefs.INSTANCE.getContrastX100(SettingsActivity.this));
                SettingsActivity.this.clearContrastPreview();
                SettingsActivity.this.refreshThemeInPlace(false);
            }
        });
        Switch r0 = this.cbInvert;
        if (r0 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("cbInvert");
            r0 = null;
        }
        r0.setChecked(ThemePrefs.INSTANCE.getInvert(this));
        Switch r02 = this.cbInvert;
        if (r02 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("cbInvert");
            r02 = null;
        }
        r02.setOnCheckedChangeListener((v1, v2) -> {
            onCreate$lambda$3(SettingsActivity.this, v1, v2);
        });
        // v1.10.139（需求）：性能卡换成真正的「可折叠卡」（标题/摘要/箭头 + 同一套 animateExpand），
        // 卡内三行全部重画（数值行 + 两枚自绘开关）；折叠状态写进 prefs，返回时保持原样。
        try {
            bindPerfCard();
            bindPaletteCard();
            restoreModeFold();
        } catch (Throwable ignored) {
        }
        View langHeader = findViewById(R.id.row_language_header);
        View langToggle = findViewById(R.id.btn_language_toggle);
        langHeader.setOnClickListener((v2) -> {
            onCreate$lambda$4(SettingsActivity.this, langToggle, v2);
        });
        langToggle.setOnClickListener((v2) -> {
            onCreate$lambda$5(SettingsActivity.this, langToggle, v2);
        });

        // ---- v1.10.47：系统模式折叠面板 + 自定义配色方案 ----
        this.btnModeToggle = findViewById(R.id.btn_mode_toggle);
        this.tvModeValue = (TextView) findViewById(R.id.tv_mode_value);
        View modeHeader = findViewById(R.id.row_mode_header);
        if (this.btnModeToggle != null) {
            this.btnModeToggle.setOnClickListener((v) -> toggleModePanel());
        }
        if (modeHeader != null) {
            modeHeader.setOnClickListener((v) -> toggleModePanel());
        }
        updateModeValue();
        this.tvContrast.setOnClickListener((v) -> showContrastInput());

        // ---- v1.10.98：字体折叠面板
        this.btnFontToggle = findViewById(R.id.btn_font_toggle);
        this.tvFontValue = (TextView) findViewById(R.id.tv_font_value);
        this.llFonts = (LinearLayout) findViewById(R.id.ll_fonts);
        View fontHeader = findViewById(R.id.row_font_header);
        if (this.btnFontToggle != null) {
            this.btnFontToggle.setOnClickListener((v) -> toggleFontPanel());
        }
        if (fontHeader != null) {
            fontHeader.setOnClickListener((v) -> toggleFontPanel());
        }
        buildFontList();

        this.llSchemes = (LinearLayout) findViewById(R.id.ll_schemes);
        this.btnSchemesToggle = findViewById(R.id.btn_schemes_toggle);
        this.schemeBar = findViewById(R.id.scheme_action_bar);
        this.barDelete = (Button) findViewById(R.id.btn_scheme_delete);
        this.barCancel = (Button) findViewById(R.id.btn_scheme_cancel);
        this.barConfirm = (Button) findViewById(R.id.btn_scheme_confirm);
        this.barMovePrev = (Button) findViewById(R.id.btn_scheme_move_prev);
        this.barMoveNext = (Button) findViewById(R.id.btn_scheme_move_next);
        this.barScroll = (android.widget.HorizontalScrollView) findViewById(R.id.scheme_bar_scroll);
        this.barIndicator = findViewById(R.id.scheme_bar_indicator);
        this.barPage1 = (LinearLayout) findViewById(R.id.scheme_bar_page1);
        this.barPage2 = (LinearLayout) findViewById(R.id.scheme_bar_page2);
        if (this.barScroll != null) {
            // 两页的宽度 = 滚动视口宽度（页与页之间因此是**连续**的，不是分页控件）
            this.barScroll.addOnLayoutChangeListener((v0, l, t, r, bo, ol, ot, or, ob) -> layoutBarPages());
        }
        this.spacerBottom = findViewById(R.id.spacer_bottom);
        if (this.barMovePrev != null) {
            this.barMovePrev.setOnClickListener((v) -> moveSelected(-1));
        }
        if (this.barMoveNext != null) {
            this.barMoveNext.setOnClickListener((v) -> moveSelected(1));
        }
        if (this.barScroll != null) {
            // 灰线跟着滚动走：位置与长度都由「可视比例 + 滚动比例」现算（见 updateBarIndicator）
            this.barScroll.setOnScrollChangeListener((v0, x, y, ox, oy) -> updateBarIndicator());
        }
        if (this.btnSchemesToggle != null) {
            this.btnSchemesToggle.setOnClickListener((v) -> cycleFold());
        }
        if (this.barDelete != null) {
            this.barDelete.setOnClickListener((v) -> {
                long id = this.pendingDeleteId;
                hideActionBar();
                if (id >= 0) {
                    deleteScheme(id);
                }
            });
        }
        if (this.barCancel != null) {
            this.barCancel.setOnClickListener((v) -> hideActionBar());
        }
        if (this.barConfirm != null) {
            this.barConfirm.setOnClickListener((v) -> {
                final long updateId = this.pendingUpdateId;
                hideActionBar();
                if (updateId >= 0) {
                    updateSchemeFromCurrent(updateId);
                } else {
                    saveCurrentPalette();
                }
            });
        }
        buildSchemes(false, -1L);
        // v1.10.522：形状图形的颜色恒跟随系统，没有开关
        // （= 默认行为）。原来的 cb_shape_system / row_shape_system 连同监听一起拿掉，
        // 形状格与布局卡片一律取系统控件前景色，不再有第二个分支。
        buildPresets();
        buildColorRows();
        ((Button) findViewById(R.id.btn_reset_theme)).setOnClickListener((v1) -> {
            onCreate$lambda$9(SettingsActivity.this, v1);
        });
        ScrollView scroll = (ScrollView) findViewById(R.id.root_settings);
        // v1.10.522：补位动画**可被触摸/滑动打断**。
        // 手指一落下就把剩下的距离用 90ms 收尾（不是硬跳到底），并且**不吞事件** ——
        // 返回 false，滚动/fling/按下反馈全都照旧，打断后衔接自然。
        scroll.setOnTouchListener((v, e) -> {
            if (e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN
                    && this.spacerAnim != null) {
                animateBottomSpacer(this.spacerTargetPx > 0, 90L);
            }
            return false;
        });
        // v1.10.523：右上角那颗是自绘的 BackButton（不再是 TextView 胶囊）
        View closePill = findViewById(R.id.btn_settings_close);
        closePill.setOnClickListener((v1) -> closeWithAnimation());
        float closeOffset = 12 * getResources().getDisplayMetrics().density;
        Ref.IntRef lastScrollY = new Ref.IntRef();
        scroll.setOnScrollChangeListener((v3, v4, v5, v6, v7) -> {
            onCreate$lambda$12(lastScrollY, closePill, closeOffset, v3, v4, v5, v6, v7);
        });
        ThemeManager themeManager = ThemeManager.INSTANCE;
        View viewFindViewById8 = findViewById(android.R.id.content);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById8, "findViewById(...)");
        themeManager.applyTo(viewFindViewById8);
        if (!this.colorEditorOpen) {
            ThemeManager.INSTANCE.applySystemBars(this);
        }
        // v1.10.241（热修）：img_language 已从 ImageView 换成自绘的 IconTileView，
        // 颜色由 ThemeManager.paint() 统一下发。这里曾经是**硬转 ImageView**，
        // v1.10.240 一换控件类型 → 进设置页立刻 ClassCastException（“一点设置就闪退”）。
        // 现在只做 instanceof 兜底：类型换了也不崩，只是这里不再负责上色。
        View langIcon = findViewById(R.id.img_language);
        if (langIcon instanceof ImageView) {
            ((ImageView) langIcon).setImageTintList(ColorStateList.valueOf(ThemeManager.INSTANCE.colors(this).getPrimary()));
        }
        buildFontList();
    }


    /**
     * v1.10.317：选中一种系统模式（0 跟随系统 / 1 白天 / 2 黑夜）。
     *
     * 与 {@link #pickLanguage} 同一套闸：同步控件时不响应、点已选中的行什么都不做
     * （否则会白叠化一次）。切模式 = 整页叠化（v1.10.103），性能卡的重建统一由
     * refreshThemeInPlace 负责，这里不重复建。
     */
    private void pickMode(final int mode) {
        if (this.syncingControls) {
            return;
        }
        if (mode == ThemePrefs.INSTANCE.getMode(this)) {
            return;
        }
        ThemePrefs.INSTANCE.setMode(this, mode);
        Morph.crossfade(this, () -> this.refreshThemeInPlace());
    }

    /** 三行语言卡片：点一下就选，选中/未选中都在 {@link #refreshLanguageRows()} 里统一刷。 */
    private void bindLanguageRows() {
        bindLanguageRow(R.id.rb_lang_system, LocaleHelper.FOLLOW_SYSTEM);
        bindLanguageRow(R.id.rb_lang_zh, LocaleHelper.CHINESE);
        bindLanguageRow(R.id.rb_lang_en, LocaleHelper.ENGLISH);
    }

    private void bindLanguageRow(int viewId, final int mode) {
        View v = findViewById(viewId);
        if (!(v instanceof LanguageRowView)) {
            return;
        }
        ((LanguageRowView) v).setOnPick(() -> pickLanguage(mode));
    }

    // ============================================================ v1.10.317 系统模式三行

    /**
     * v1.10.317：系统模式面板 = 三行 {@link LanguageRowView}，和「界面语言」逐像素同一套。
     *
     * 两行文案的分工也照语言行的规矩来：
     *  · 「跟随系统」行的副行写**此刻实际生效**的是白天还是黑夜 ——
     *    这样不点开也知道会落成什么（语言行那边写的是"当前实际生效的语言"）；
     *  · 白天 / 黑夜两行的副行写明"忽略系统"，避免和「跟随系统」的语义混淆。
     */
    private void bindModeRows() {
        bindModeRow(R.id.rb_system, 0);
        bindModeRow(R.id.rb_light, 1);
        bindModeRow(R.id.rb_dark, 2);
    }

    private void bindModeRow(int viewId, final int mode) {
        View v = findViewById(viewId);
        if (!(v instanceof LanguageRowView)) {
            return;
        }
        ((LanguageRowView) v).setOnPick(() -> pickMode(mode));
    }

    /** 三行的选中态 + 配色一次刷完（进页面、切模式、换肤都走这里）。 */
    private void refreshModeRows() {
        ThemeColors c = ThemeManager.INSTANCE.colors(this);
        int mode = ThemePrefs.INSTANCE.getMode(this);
        // 「跟随系统」时实际生效的是哪一个（ThemeManager.isDark 已经算过系统+反转）
        boolean dark = ThemeManager.INSTANCE.isDark(this);
        String now = s(dark ? R.string.settings_dark : R.string.settings_light);
        // v1.10.319：三行的副行**恒定显示**（不再"选中才显示"）——
        // 界面语言那边三行都有副行，这里也必须都有，否则未选中的行会比语言行矮一截、结构也不一致。
        bindModeRowState(R.id.rb_system, s(R.string.settings_follow_system),
                s(R.string.settings_mode_now, now), mode == 0, c);
        bindModeRowState(R.id.rb_light, s(R.string.settings_light),
                s(R.string.settings_mode_light_hint), mode == 1, c);
        bindModeRowState(R.id.rb_dark, s(R.string.settings_dark),
                s(R.string.settings_mode_dark_hint), mode == 2, c);
    }

    private void bindModeRowState(int viewId, CharSequence title, CharSequence sub,
                                  boolean selected, ThemeColors c) {
        View v = findViewById(viewId);
        if (!(v instanceof LanguageRowView)) {
            return;
        }
        LanguageRowView row = (LanguageRowView) v;
        // 先配色再设选中态：applyTheme 按 checked 上色，顺序反了会让勾/淡底闪一下旧色
        row.applyTheme(c);
        row.bind(title, sub, selected, true);
    }

    /**
     * 选中一门语言。
     *
     * 三道闸，专治「重复点击 / 选中态与真实语言不同步」：
     *  ① 页面正在同步控件（`syncingControls`）时不响应；
     *  ② 320ms 内的第二次点击直接丢（连点不会连触发两次整页叠化）；
     *  ③ 点已经选中的那一行什么都不做 —— 不然会白叠化一次。
     */
    private void pickLanguage(final int mode) {
        if (this.syncingControls) {
            return;
        }
        long now = android.os.SystemClock.uptimeMillis();
        if (now - this.lastLangPickAt < 320L) {
            return;
        }
        if (mode == LocaleHelper.INSTANCE.get(this)) {
            return;
        }
        this.lastLangPickAt = now;
        LocaleHelper.INSTANCE.set(this, mode);
        Diag.warn(this, "A05", "语言档=" + mode + " 触发=点击");
        // v1.10.1：原地换文案 —— 不重建、不折叠菜单、不丢滚动位置
        // v1.10.99（反馈 2.2）：外面裹一层整页叠化，不要硬刷
        Morph.crossfade(this, () -> {
            applyLocaleInPlace();
            // v1.10.199（反馈）：性能卡里的行是**代码动态建的**，applyLocaleInPlace 只换静态文案，
            // 所以切语言后「相册扫描线程数 / 预热线程数 / 画布无关帧 / 导出后提前烘帧…」还是旧语言。
            // v1.10.202：rebuildSchemes 那一步已经在 refreshThemeInPlace 里重建了性能卡（换语言的文案
            // 也跟着一起去 —— 用的是 I18nActivity.s()，走新 locale），这里不再重复建一次。
            refreshThemeInPlace();
            syncControls();
            refreshLanguageRows();   // 选中态跟着新语言走
        });
    }

    private static final void onCreate$lambda$3(SettingsActivity this$0, CompoundButton compoundButton, boolean checked) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if (this$0.syncingControls) {
            return;
        }
        this$0.applyInvertChange(checked);
    }

    /**
     * v1.10.203（反馈「反转颜色右侧的开关，动画不该跟着整页叠化」）：
     * **把「开关自己的滑动」和「整页换色」在时间上拆开，一次只动一件事。**
     *
     * 演进过程（都是同一类错误的两面）：
     *   · v1.10.177 是硬刷 —— 页面没有叠化；
     *   · v1.10.187 改成「先把滑块一次性推到终点（jumpDrawablesToCurrentState）再叠化」——
     *     滑块不跳了，但它那 250ms 的滑动整段被那张不透明旧画面盖住，
     *     用户看到的是「按下去没反应，然后整页一变」，也就是这次的反馈；
     *   · 现在：先让 Switch 自己把位置动画走完（≈250ms，肉眼可见），再截图画叠化。
     *     快照里开关已经在终点、且还带着旧配色；遮罩淡出后整页（含开关 tint）一起变新。
     *
     * 连点用 token 兜住：窗口期内再点一次，只保留最后一次。
     */
    private void applyInvertChange(final boolean checked) {
        ThemePrefs.INSTANCE.setInvert(this, checked);
        final int token = ++this.invertToken;
        final View decor = getWindow().getDecorView();
        decor.postDelayed(() -> {
            if (token != this.invertToken || isFinishing()) {
                return;
            }
            // 与「切白天/黑夜」同一条路：整页叠化 + 总入口刷新（rebuildSchemes=false：图标对象不换）
            Morph.crossfade(this, () -> refreshThemeInPlace(false));
        }, INVERT_SETTLE_MS);
    }

    private static final void onCreate$toggleLanguage(SettingsActivity this$0, View langToggle, boolean expand) {
        LinearLayout radioGroup = this$0.langGroup;
        if (radioGroup == null) {
            Intrinsics.throwUninitializedPropertyAccessException("langGroup");
            return;
        }
        // v1.10.1：高度平滑过渡 + 内容淡入淡出，250ms；动画中途再点能顺滑反向
        this$0.animateExpand(radioGroup, expand);
        langToggle.animate().rotation(expand ? 180.0f : 0.0f).setDuration(160L).start();
    }

    /**
     * v1.10.203：反转开关「先走完自己的滑动、再叠化」的等待时长。
     * Switch 的拇指位置动画在 AOSP 里是 250ms，这里留 10ms 余量。
     */
    private static final long INVERT_SETTLE_MS = 260L;

    /** 连点闸：窗口期内再次切换只保留最后一次（避免两段叠化套在一起）。 */
    private int invertToken;

    @Nullable
    private android.animation.ValueAnimator expandAnimator;

    /** 通用折叠动画：只改高度 + alpha，结构不动（ValueAnimator 由 vsync 驱动，时长随系统动画缩放）。 */
    final void animateExpand(@NotNull final View view, final boolean expand) {
        if (this.expandAnimator != null) {
            this.expandAnimator.cancel();
            this.expandAnimator = null;
        }
        int width = view.getWidth() > 0 ? view.getWidth()
                : getResources().getDisplayMetrics().widthPixels;
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        final int target = expand ? view.getMeasuredHeight() : 0;
        int current = view.getLayoutParams().height;
        if (current < 0) {
            current = expand ? 0 : view.getHeight();
        }
        if (expand) {
            view.setVisibility(View.VISIBLE);
        }
        if (current == target) {
            if (!expand) {
                view.setVisibility(View.GONE);
            }
            return;
        }
        final int from = current;
        final float fromAlpha = expand ? 0.0f : 1.0f;
        final float toAlpha = expand ? 1.0f : 0.0f;
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(from, target);
        animator.setDuration(250L);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener((animation) -> {
            float fraction = animation.getAnimatedFraction();
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            lp.height = (int) animation.getAnimatedValue();
            view.setLayoutParams(lp);
            view.setAlpha(fromAlpha + (toAlpha - fromAlpha) * fraction);
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                ViewGroup.LayoutParams lp = view.getLayoutParams();
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                view.setLayoutParams(lp);
                view.setAlpha(1.0f);
                if (!expand) {
                    view.setVisibility(View.GONE);
                }
                SettingsActivity.this.expandAnimator = null;
            }
        });
        this.expandAnimator = animator;
        animator.start();
    }

    /** 语言原地切换后：重刷代码里设置过的文字。 */
    @Override
    protected void onTextsRefreshed() {
        try {
            updateContrastLabel();
            buildPresets();
            buildColorRows();
            buildSchemes(false, -1L);
            updateModeValue();
            applyLanguagePanelState();
            // v1.10.99（反馈 1）：字体列表的文字是代码里拼的，不重建就永远停在旧语言
            buildFontList();
            // v1.10.158：语言行的选中态/配色也一起刷新（深浅色切换时同样生效）
            refreshLanguageRows();
        } catch (Throwable ignored) {
        }
    }

    /** 选中项与展开状态不被语言切换影响。 */
    private void applyLanguagePanelState() {
        LinearLayout group = this.langGroup;
        if (group != null) {
            boolean expanded = group.getVisibility() == View.VISIBLE;
            group.setVisibility(expanded ? View.VISIBLE : View.GONE);
        }
    }

    private static final void onCreate$lambda$4(SettingsActivity this$0, View $langToggle, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        LinearLayout radioGroup = this$0.langGroup;
        if (radioGroup == null) {
            Intrinsics.throwUninitializedPropertyAccessException("langGroup");
            radioGroup = null;
        }
        onCreate$toggleLanguage(this$0, $langToggle, radioGroup.getVisibility() != 0);
    }

    private static final void onCreate$lambda$5(SettingsActivity this$0, View $langToggle, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        LinearLayout radioGroup = this$0.langGroup;
        if (radioGroup == null) {
            Intrinsics.throwUninitializedPropertyAccessException("langGroup");
            radioGroup = null;
        }
        onCreate$toggleLanguage(this$0, $langToggle, radioGroup.getVisibility() != 0);
    }

    // v1.10.522：onCreate$lambda$6（写 shape_follow_system）/ onCreate$lambda$7（点行切勾选）
    // 随「形状图形颜色跟随系统」开关一起删除。

    private static final void onCreate$lambda$9(SettingsActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        // v1.10.140：原来是 AlertDialog（Theme_Material_Dialog_Alert）—— 系统灰底、直角、
        // 青色文字、系统字体，和这个 App 完全不是一套。换成自绘的 ConfirmDialog。
        new ConfirmDialog(this$0)
                .title(this$0.s(R.string.settings_reset))
                .message(this$0.s(R.string.settings_reset_confirm))
                .confirm(this$0.s(R.string.settings_reset))
                .danger(true)
                .glyph(GlyphView.ALERT)
                .onConfirm(() -> onCreate$lambda$9$lambda$8(this$0, null, 0))
                .show();
    }

    private static final void onCreate$lambda$9$lambda$8(SettingsActivity this$0, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        ThemePrefs.INSTANCE.clearCustom(this$0);
        ThemePrefs.INSTANCE.setPreset(this$0, 0);
        this$0.writeContrast(0);
        ThemePrefs.INSTANCE.setInvert(this$0, false);
        this$0.refreshThemeInPlace();
    }

    /**
     * v1.10.523（需求）：左上返回按钮的**关闭动效**（原来是直接 finish()，硬切）。
     *
     * <p>顺序：按钮自己先缩一下 → 整页淡出 + 下沉 10dp → finish。180ms，
     * 用 Decelerate，和页面进入时的曲线同一量级；`closing` 保证连点只跑一次
     * （否则第二次点会在一段正在跑的动画上叠 withEndAction，可能 finish 两次）。</p>
     */
    private void closeWithAnimation() {
        if (this.closing) {
            return;
        }
        this.closing = true;
        final View back = findViewById(R.id.btn_settings_close);
        if (back != null) {
            back.animate().scaleX(0.88f).scaleY(0.88f).setDuration(90L).start();
        }
        final View content = findViewById(android.R.id.content);
        if (content == null) {
            finish();
            return;
        }
        content.animate()
                .alpha(0.0f)
                .translationY(10.0f * density())
                .setDuration(180L)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> finish())
                .start();
    }

    private static final void onCreate$showClose(View closePill) {
        if (closePill.getVisibility() == 0 && closePill.getAlpha() > 0.99f) {
            if (closePill.getTranslationY() == 0.0f) {
                return;
            }
        }
        closePill.animate().cancel();
        closePill.setVisibility(0);
        closePill.animate().alpha(1.0f).translationY(0.0f).setDuration(160L).start();
    }

    private static final void onCreate$hideClose(View closePill, float closeOffset) {
        if (closePill.getVisibility() != 0) {
            return;
        }
        closePill.animate().cancel();
        closePill.animate().alpha(0.0f).translationY(-closeOffset).setDuration(140L).withEndAction(() -> {
            onCreate$hideClose$lambda$11(closePill);
        }).start();
    }

    private static final void onCreate$hideClose$lambda$11(View $closePill) {
        $closePill.setVisibility(8);
    }

    private static final void onCreate$lambda$12(Ref.IntRef $lastScrollY, View $closePill, float $closeOffset, View view, int i, int y, int i2, int i3) {
        Intrinsics.checkNotNullParameter($lastScrollY, "$lastScrollY");
        if (y <= 0) {
            onCreate$showClose($closePill);
        } else if (y > $lastScrollY.element + 6) {
            onCreate$hideClose($closePill, $closeOffset);
        } else if (y < $lastScrollY.element - 6) {
            onCreate$showClose($closePill);
        }
        $lastScrollY.element = y;
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
        // v1.10.266：页面不可见时**必须**撤掉拖动预览，否则临时对比度会残留在进程里
        clearContrastPreview();
        // v1.10.91：离开页面时把滑杆动画收尾到目标值，别停在半路（存储里已经是目标值了）
        if (this.contrastAnimating) {
            cancelContrastAnimation();
            if (this.seekContrast != null) {
                this.seekContrast.setProgress(ThemePrefs.INSTANCE.getContrastX100(this) + 100);
            }
            updateContrastLabel();
        }
        getWindow().getDecorView().removeCallbacks(this.themePoll);
        super.onPause();
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().removeCallbacks(this.themePoll);
        getWindow().getDecorView().post(this.themePoll);
        // v1.10.1：语言变了原地换（不重建），再对齐主题
        syncLocale();
        boolean dark = ThemeManager.INSTANCE.isDark(this);
        if (dark != this.appliedDark) {
            this.appliedDark = dark;
            refreshThemeInPlace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void refreshThemeInPlace() {
        refreshThemeInPlace(true);
    }

    /**
     * @param rebuildSchemes false = 只换肤、不重建方案网格
     *                       （重建会换掉图标对象，勾选动画就看不见了 —— 点选方案时用 false）
     */
    public final void refreshThemeInPlace(boolean rebuildSchemes) {
        // v1.10.202（反馈「切系统深浅色后性能卡的选项框不亮、行底色也消失」）：
        // 性能卡是**代码动态建**的子树，里面有两种东西 paint() 管不着：
        //   ① 开关框（已改成自绘 + 接进 paint()，见 SwitchBoxView）；
        //   ② 每一行的极淡底色（rowBg 用正文色 4.5% 调的 GradientDrawable，裸 ViewGroup 没有 tag）。
        // ②不可能靠"记住每个入口都要重建"来保证 —— 换肤入口有六七个。
        // 所以在**唯一的总入口**这里重建一次：谁走到换肤，性能卡就是新的。
        // （bindPerfCard 内部按 foldState 恢复展开状态，不会把面板收起来。）
        try {
            bindPerfCard();
        } catch (Throwable ignored) {
        }
        // v1.10.90：对比度可能刚被方案改写，先把滑杆/数值同步过来再刷其它控件
        syncContrastSlider();
        buildPresets();
        buildColorRows();
        if (rebuildSchemes) {
            buildSchemes(false, -1L);
        }
        ThemeManager themeManager = ThemeManager.INSTANCE;
        View viewFindViewById = findViewById(android.R.id.content);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById, "findViewById(...)");
        themeManager.applyTo(viewFindViewById);
        if (!this.colorEditorOpen) {
            ThemeManager.INSTANCE.applySystemBars(this);
        }
        // v1.10.241（热修）：img_language 已从 ImageView 换成自绘的 IconTileView，
        // 颜色由 ThemeManager.paint() 统一下发。这里曾经是**硬转 ImageView**，
        // v1.10.240 一换控件类型 → 进设置页立刻 ClassCastException（“一点设置就闪退”）。
        // 现在只做 instanceof 兜底：类型换了也不崩，只是这里不再负责上色。
        View langIcon = findViewById(R.id.img_language);
        if (langIcon instanceof ImageView) {
            ((ImageView) langIcon).setImageTintList(ColorStateList.valueOf(ThemeManager.INSTANCE.colors(this).getPrimary()));
        }
        // v1.10.47：折叠面板的箭头/当前值文字也要跟着主题走
        ThemeColors colors = ThemeManager.INSTANCE.colors(this);
        // v1.10.90：不重建网格时（切方案/改对比度），图标描边色也要跟着刷，
        // 否则对比度一动、文字色变了，图标外圈还是旧的
        refreshSchemeTileRings(colors);
        // v1.10.92：「已修改」半透明勾选也要跟着刷
        refreshSchemeDirty();
        if (this.tvModeValue != null) {
            this.tvModeValue.setTextColor(colors.getTextMuted());
        }
        styleActionBar(colors);
        updateModeValue();
        // v1.10.159：语言行的颜色是**自绘**的（淡底/勾/文字分开设色），
        // 不在这里刷的话，深浅色一翻，语言面板还停在旧主题 —— 这是实测会出现的错位。
        refreshLanguageRows();
        // v1.10.317：系统模式三行也是自绘卡片，换肤时同样要重刷（与语言行同因同果）
        refreshModeRows();
    }

    @Override // android.app.Activity
    protected void onPostResume() {
        super.onPostResume();
        syncControls();
    }

    private final void syncControls() {
        this.syncingControls = true;
        // v1.10.317：系统模式三行与语言行同一套刷法（按存储值刷选中态，不用 RadioGroup.check）
        refreshModeRows();
        // v1.10.159：语言行的选中态不再靠 RadioGroup.check()，统一由这里按存储值刷
        refreshLanguageRows();
        Switch r0 = this.cbInvert;
        if (r0 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("cbInvert");
            r0 = null;
        }
        r0.setChecked(ThemePrefs.INSTANCE.getInvert(this));
        this.syncingControls = false;
        // v1.10.47：折叠面板下面那行「当前模式」文案
        updateModeValue();
    }


    /**
     * v1.10.92：当前运行时配色 + 对比度，和某个已保存方案**是否不一致**（= 有未保存改动）。
     *
     * 比对用 {@code getCustom}（存进去的覆盖值）而不是 {@code rawColors()}：
     * rawColors 会按当前黑白模式纠正表面/中性色（v1.10.83），切模式后会误判成「已修改」。
     */
    private boolean isSchemeDirty(long id) {
        return schemeDirtyReason(id) != null;
    }

    /**
     * v1.10.208：**「已修改」判定的唯一实现**，返回「哪里不一样」；一致返回 null。
     *
     * 为什么要返回原因：这个判定直接决定长按能不能看到「更新」按钮，
     * 判错了只能靠真机截图猜（v1.10.208 就是用户报「调完配色没有更新按钮」）。
     * 现在长按时把它打进 `A11 动作条 … 依据=…`，下次再出问题一眼定位到是哪个键。
     *
     * 规则（三条，与「什么算方案的完整配色」一一对应）：
     *   ① 单值语义色：生效值(flat) vs 存档；
     *      **存档里没有这个字段（老方案存不下来，值为 0）时**：现在也没自定义才算一致，
     *      一旦有了自定义就是「改过」—— 否则给老方案改错误色永远不会出现「更新」按钮。
     *   ② 底色（表面 / 中性变体）：**按模式**比（存档是深/浅两份，生效值是 liveBase(当前模式)）。
     *      flat 不参与 —— 它只是「上次激活方案时写进去的那个」，拿它比会误判（v1.10.202 的坑）。
     *   ③ 对比度：属于方案。
     * 异常一律当「一致」（页面不能因为判定挂掉），但不吞原因：见 catch 里的日志。
     */
    @Nullable
    private String schemeDirtyReason(long id) {
        if (id == -1L) {
            return null;
        }
        try {
            int[] c = ThemePrefs.INSTANCE.getScheme(this, id);
            if (c == null || c.length != ThemePrefs.SCHEME_FIELDS) {
                return null;
            }
            // ① 单值语义色
            String[] flatKeys = {"primary", "secondary", "tertiary", "slider", "error"};
            int[] flatIdx = {0, 1, 2, 5, 6};
            for (int i = 0; i < flatKeys.length; i++) {
                int want = c[flatIdx[i]];
                int live = ThemePrefs.INSTANCE.getCustom(this, flatKeys[i]);
                // v1.10.209（反馈「动了参数之后，如果是原来值就不要显示更新」）：
                // **把「无覆盖(0)」翻译成它真正生效的颜色再比**。
                // 拿 0 直接和存档比，就会把「回到默认」误判成已修改 —— 用户日志里的
                // 「slider=#000000≠#FE8019」就是这么来的（0 不是黑色，是"没有覆盖"）。
                int def = ThemeManager.INSTANCE.defaultRawValue(this, flatKeys[i]);
                int liveEff = live != 0 ? live : def;
                int wantEff = want != 0 ? want : def;
                if (liveEff != wantEff) {
                    return flatKeys[i] + " 生效=" + hex(liveEff)
                            + (live == 0 ? "（无覆盖→默认）" : "")
                            + "≠存档=" + hex(wantEff)
                            + (want == 0 ? "（存档未记录→按默认）" : "");
                }
            }
            // ② 底色：按模式比
            int[] stored = ThemePrefs.INSTANCE.getSchemeBases(this, id);
            if (stored == null) {
                stored = legacyBases(c[3], c[4]);
            }
            int[] now = currentBases();
            String[] baseTag = {"surface_d", "neutral_d", "surface_l", "neutral_l"};
            for (int i = 0; i < stored.length; i++) {
                if (now[i] != stored[i]) {
                    return baseTag[i] + "=" + hex(now[i]) + "≠" + hex(stored[i]);
                }
            }
            // ③ 对比度
            int liveContrast = ThemePrefs.INSTANCE.getContrastX100(this);
            int savedContrast = ThemePrefs.INSTANCE.getSchemeContrast(this, id);
            if (liveContrast != savedContrast) {
                return "对比度=" + liveContrast + "≠" + savedContrast;
            }
            return null;
        } catch (Throwable th) {
            Diag.warn(this, "A11", "已修改判定异常 方案=" + id + " " + th.getClass().getSimpleName());
            return null;
        }
    }

    /** 把「已修改」状态刷到所有方案图标上（半透明勾选）。 */
    private void refreshSchemeDirty() {
        try {
            LinearLayout host = this.llSchemes;
            if (host == null) {
                return;
            }
            long active = ThemePrefs.INSTANCE.getActiveScheme(this);
            boolean dirty = isSchemeDirty(active);
            for (int r = 0; r < host.getChildCount(); r++) {
                View row = host.getChildAt(r);
                if (!(row instanceof ViewGroup)) {
                    continue;
                }
                ViewGroup group = (ViewGroup) row;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    if (!(child instanceof SchemeTileView)) {
                        continue;
                    }
                    Object tag = child.getTag();
                    boolean isActive = tag instanceof Long && ((Long) tag) == active;
                    ((SchemeTileView) child).setDirty(isActive && dirty);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * v1.10.92：**覆盖保存到已保存方案**（编辑方案）。
     * 存的是「当前 6 个语义色 + 当前对比度」，存完就不再是「已修改」状态。
     */
    private void updateSchemeFromCurrent(final long id) {
        try {
            ThemeColors raw = ThemeManager.INSTANCE.rawColors(this);
            int[] c = {raw.getPrimary(), raw.getSecondary(), raw.getTertiary(),
                    raw.getSurface(), raw.getNeutral(), raw.getSlider(),
                    raw.getError()};
            int[] bases = currentBases();
            boolean ok = ThemePrefs.INSTANCE.updateScheme(
                    this, id, c, ThemePrefs.INSTANCE.getContrastX100(this));
            if (!ok) {
                return;
            }
            ThemePrefs.INSTANCE.setSchemeBases(this, id, bases);
            ThemePrefs.INSTANCE.setActiveScheme(this, id);
            Diag.warn(this, "A11", "方案 " + id + " 覆盖保存：surface="
                    + hex(c[3]) + " neutral=" + hex(c[4])
                    + "（深 " + hex(bases[0]) + "/" + hex(bases[1])
                    + "，浅 " + hex(bases[2]) + "/" + hex(bases[3]) + "）");
            refreshThemeInPlace(true);
            toast(s(R.string.settings_scheme_updated));
        } catch (Throwable ignored) {
        }
    }

    /** 只刷方案图标的描边色（不重建 View，勾选动画不会丢）。 */
    private void refreshSchemeTileRings(ThemeColors colors) {
        try {
            LinearLayout host = this.llSchemes;
            if (host == null) {
                return;
            }
            for (int r = 0; r < host.getChildCount(); r++) {
                View row = host.getChildAt(r);
                if (!(row instanceof ViewGroup)) {
                    continue;
                }
                ViewGroup group = (ViewGroup) row;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    if (child instanceof SchemeTileView) {
                        ((SchemeTileView) child).setRingColor(colors.getText());
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * v1.10.90：把对比度滑杆与数值同步到当前存储值。
     * 切换配色方案会连对比度一起换，不同步的话滑杆还停在旧位置。
     */
    private void syncContrastSlider() {
        try {
            // v1.10.176：叠化进行中先不动滑杆，等遮罩淡出由 applyScheme 的回调接手
            if (this.deferContrastCurve) {
                return;
            }
            animateContrastSliderTo(ThemePrefs.INSTANCE.getContrastX100(this));
        } catch (Throwable ignored) {
        }
    }

    /**
     * v1.10.91：对比度滑杆的**前进/后退动画**。
     *
     * 切换配色方案会连对比度一起换（v1.10.90），原来 `setProgress` 是瞬移 —— 这里改成
     * 从**当前进度**动到目标进度（贝塞尔 Easy Ease，时长随距离 180~420ms）：
     *  • 正在动的时候再切方案 → 取消当前动画、从当前位置接着动（不跳、不重置）；
     *  • 动画中间帧**不写盘**（否则一次滑动要写几十次 SharedPreferences），
     *    结束时只落一次最终值；
     *  • 用户自己拖滑杆 → 立刻取消动画，交还给手。
     */
    private void animateContrastSliderTo(final int x100) {
        final SeekBar bar = this.seekContrast;
        final int target = Math.max(0, Math.min(200, x100 + 100));
        if (bar == null) {
            updateContrastLabel(x100);
            return;
        }
        cancelContrastAnimation();
        final int from = bar.getProgress();
        if (from == target) {
            updateContrastLabel(x100);
            return;
        }
        int distance = Math.abs(target - from);
        long duration = Math.min(420L, 180L + distance * 240L / 200L);
        // v1.10.176：把曲线的量级写进日志，便于确认「叠化后串行」真的生效（A08）
        Diag.warn(this, "A08", "对比度滑杆 由=" + from + " 到=" + target
                + " 步进=" + distance + " 时长=" + duration + "ms"
                + " 叠化中=" + Morph.isCrossfading(this));
        final android.animation.ValueAnimator anim =
                android.animation.ValueAnimator.ofInt(from, target);
        anim.setDuration(duration);
        anim.setInterpolator((t) -> Ease.IN_OUT.at(t));
        this.contrastAnimating = true;
        anim.addUpdateListener((a) -> bar.setProgress(((Integer) a.getAnimatedValue()).intValue()));
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                this.cancelled = true;
            }

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (this.cancelled) {
                    return;
                }
                if (SettingsActivity.this.contrastAnim == anim) {
                    SettingsActivity.this.contrastAnim = null;
                }
                // 先顶到目标进度（此时仍处于 animating 状态 → 回调里只刷文字、不写盘）
                bar.setProgress(target);
                SettingsActivity.this.contrastAnimating = false;
                // v1.10.203（反馈「连续快速点两个方案，滑杆/数值不是最后那个方案的值」）：
                // 这里**不再落盘**。滑杆动画是表现层，存储的唯一主人是两个：
                //   ① 用户自己拖 / 键盘输入（写运行时对比度）；
                //   ② 激活方案（writeSchemeColors 把方案存档里的对比度投影过来）。
                // 原来任何一条还在跑的曲线到点就把自己的目标值写进 prefs ——
                // 它完全可能比「切到下一个方案」更晚到，于是把新方案的对比度覆盖回旧方案的值。
                // 现在动画只负责把滑块挪到目标位置 + 刷数字。
                SettingsActivity.this.updateContrastLabel(x100);
                SettingsActivity.this.clearContrastPreview();
            }
        });
        this.contrastAnim = anim;
        anim.start();
    }

    /**
     * v1.10.266：把「当前显示色 → 目标对比度」补间过去，并在补间期间**每帧换一次色**。
     *
     * 为什么不是每个 drag 事件都直接 applyTo：手拖一秒能有 60 个事件，每次都从当前值瞬间跳，
     * 看起来是"抖"不是"叠化"。这里用 110ms 的短补间把相邻两次事件缝起来，形成连续变化；
     * 而且只走 `applyTo`（纯换色），**不碰** `refreshThemeInPlace` —— 后者会重建性能卡/方案网格/色行，
     * 在那个频率下必然掉帧。
     */
    private void previewContrastTo(float level) {
        // v1.10.267：**不再补间**。上一版用 110ms 补间 + 每 22ms 刷一次，用户实测"还是松手才变"。
        // 原因有两层：① 我把档位算错了（`value/100` 而 progress 是 0~200，重心处就已经是 1.0，
        // 右半段被 clamp 成常数 → 看起来完全没动）；② 补间会把"跟手"变成"追手"。
        // 现在是真·实时：每个拖动回调都把临时档位写进去并立刻重上色（限 16ms 一次，够 60fps）。
        ThemeManager.setPreviewContrast(Float.valueOf(level));
        long now = android.os.SystemClock.uptimeMillis();
        if (now - this.contrastPreviewAt >= 16L) {
            this.contrastPreviewAt = now;
            this.applyContrastPreview();
        }
    }

    /** 只换色：整棵树重上色 + 系统栏，不动任何结构。 */
    private void applyContrastPreview() {
        try {
            View content = findViewById(android.R.id.content);
            if (content != null) {
                ThemeManager.INSTANCE.applyTo(content);
            }
            ThemeManager.INSTANCE.applySystemBars(this);
        } catch (Throwable ignored) {
        }
    }

    /** 结束预览：撤掉临时值，把**最终值**落到界面上（之后由调用方决定要不要完整刷新）。 */
    private void clearContrastPreview() {
        if (this.contrastPreviewAnim != null) {
            try {
                this.contrastPreviewAnim.cancel();
            } catch (Throwable ignored) {
            }
            this.contrastPreviewAnim = null;
        }
        if (ThemeManager.previewContrast() != null) {
            ThemeManager.setPreviewContrast(null);
            this.applyContrastPreview();
        }
    }

    /** 停掉滑杆动画（不落盘）。 */
    private void cancelContrastAnimation() {
        final android.animation.ValueAnimator anim = this.contrastAnim;
        this.contrastAnim = null;
        this.contrastAnimating = false;
        if (anim != null) {
            try {
                anim.cancel();
            } catch (Throwable ignored) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void updateContrastLabel() {
        updateContrastLabel(ThemePrefs.INSTANCE.getContrastX100(this));
    }

    /** v1.10.91：按指定 x100 刷数值（滑杆动画的中间帧要跟手，不能每帧去读存储）。 */
    private void updateContrastLabel(int x100) {
        TextView textView = this.tvContrast;
        if (textView == null) {
            return;
        }
        // v1.10.47：两位小数
        textView.setText(String.format(java.util.Locale.US, "%.2f", x100 / 100f));
    }

    // ================================================================ v1.10.47
    //  1) 「系统模式」可折叠面板
    //  2) 对比度两位小数 + 键盘直接输入
    //  3) 自定义配色方案（占位 / 保存 / 切换 / 长按删除 / 补位动画 / 按行折叠）

    private void toggleModePanel() {
        LinearLayout group = this.modeGroup;
        if (group == null) {
            return;
        }
        boolean expand = group.getVisibility() != View.VISIBLE;
        // 与「界面语言」完全同一套动画（animateExpand：只改高度 + alpha）
        animateExpand(group, expand);
        // v1.10.139：**折叠状态要记住**（原来返回设置页又被展开）
        saveFold("mode", expand);
        if (this.btnModeToggle != null) {
            this.btnModeToggle.animate().rotation(expand ? 180.0f : 0.0f).setDuration(160L).start();
        }
    }

    // ================================================================= v1.10.98 字体
    private static int fontAlpha(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * alpha);
        if (a <= 0) {
            a = 1;
        }
        return (a << 24) | (color & 0x00FFFFFF);
    }

    // ================================================================ v1.10.139 性能卡
    /** 折叠状态存取：key = mode / perf …，写在全局 settings prefs 里。 */
    private boolean foldState(@NotNull String key, boolean def) {
        try {
            return getSharedPreferences("settings", 0).getBoolean("fold_" + key, def);
        } catch (Throwable th) {
            return def;
        }
    }

    private void saveFold(@NotNull String key, boolean expanded) {
        try {
            getSharedPreferences("settings", 0).edit().putBoolean("fold_" + key, expanded).apply();
        } catch (Throwable ignored) {
        }
    }

    /** 按上次状态摆好（不播动画，避免进页面时闪一下）。 */
    private void restoreFoldNow(@NotNull View body, @Nullable View chevron, boolean expand) {
        ViewGroup.LayoutParams lp = body.getLayoutParams();
        if (lp == null) {
            lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        if (expand) {
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            body.setVisibility(View.VISIBLE);
            body.setAlpha(1.0f);
        } else {
            lp.height = 0;
            body.setVisibility(View.GONE);
            body.setAlpha(0.0f);
        }
        body.setLayoutParams(lp);
        if (chevron != null) {
            chevron.setRotation(expand ? 180.0f : 0.0f);
        }
    }

    private void restoreModeFold() {
        LinearLayout group = this.modeGroup;
        if (group == null) {
            return;
        }
        restoreFoldNow(group, this.btnModeToggle, foldState("mode", true));
    }

    private void bindPerfCard() {
        final View header = findViewById(R.id.row_perf_header);
        final View body = findViewById(R.id.ll_perf);
        final View chevron = findViewById(R.id.btn_perf_toggle);
        final TextView summary = (TextView) findViewById(R.id.tv_perf_value);
        if (header == null || body == null || summary == null || !(body instanceof LinearLayout)) {
            return;
        }
        LinearLayout host = (LinearLayout) body;
        // v1.10.202：重建前先把可能在跑的高度动画收掉 —— 它每帧都在改 host 的 lp.height，
        // 会和下面 restoreFoldNow 摆好的状态打架（展开动画跑到一半时正好换肤就能看到）。
        if (this.expandAnimator != null) {
            this.expandAnimator.cancel();
            this.expandAnimator = null;
        }
        // v1.10.272：副标题的收起动画每帧在改**行**的高度，行又要被重建 → 一律先停。
        if (this.perfFoldAnim != null) {
            this.perfFoldAnim.cancel();
            this.perfFoldAnim = null;
        }
        this.perfFoldRows.clear();
        host.removeAllViews();

        // ⓪ v1.10.272（需求）：副标题展开/收起的两个按钮，放在「相册扫描」之上，
        //    作用于本卡内**所有**带副标题的条目。
        host.addView(buildSubtitleControlRow());

        // ① 相册扫描线程数（点开数字输入）
        host.addView(buildValueRow(s(R.string.settings_scan_threads),
                s(R.string.settings_scan_threads_hint),
                s(R.string.settings_scan_threads_value,
                        new Object[]{Integer.valueOf(AnimatedPickerActivity.scanThreads(this))}),
                v -> openScanThreads()));

        // ①′ 预热线程数（v1.10.191：用户可调 4~8）
        host.addView(buildValueRow(s(R.string.settings_prewarm_threads),
                s(R.string.settings_prewarm_threads_hint),
                s(R.string.settings_prewarm_threads_value,
                        new Object[]{Integer.valueOf(WidgetPrewarm.threadSetting(this))}),
                v -> openPrewarmThreads(), 1));

        // ①″ 画布无关帧（v1.10.192）
        host.addView(buildSwitchRow(s(R.string.settings_nocanvas),
                s(R.string.settings_nocanvas_hint),
                WidgetRender.noCanvasPref(this),
                on -> {
                    SettingsActivity.this.getSharedPreferences("settings", 0)
                            .edit().putBoolean(WidgetRender.PREF_NOCANVAS, Boolean.TRUE.equals(on)).apply();
                    Diag.warn(SettingsActivity.this, "A05",
                            "画布无关帧=" + (Boolean.TRUE.equals(on) ? "开" : "关"));
                    SettingsActivity.this.refreshPerfSummary();
                }));

        // ①‴ 静态图 1:1 高清（v1.10.251）
        host.addView(buildSwitchRow(s(R.string.settings_hires_static),
                s(R.string.settings_hires_static_hint),
                WidgetRender.hiResStaticPref(this),
                on -> {
                    boolean v = Boolean.TRUE.equals(on);
                    WidgetRender.setHiResStatic(this, v);
                    Diag.warn(this, "A05", "静态图 1:1 高清(默认)=" + (v ? "开" : "关"));
                    refreshPerfSummary();
                    // v1.10.252（反馈）：开关只改「下次算画布用什么尺寸」，不主动推的话
                    // 桌面要等到拖动组件才变 —— 这里立刻重推一遍，改完回头就见效。
                    final Context app = getApplicationContext();
                    Thread t = new Thread(new Runnable() { // from class: io.github.animwidget.SettingsActivity$hiresRepush
                        @Override // java.lang.Runnable
                        public void run() {
                            WidgetRender.INSTANCE.repushAll(app, "静态图1:1高清=" + (v ? "开" : "关"));
                        }
                    }, "hires-repush");
                    t.start();
                }));

        // ①⁗ 动图分辨率（v1.10.254）—— 之前**根本没有**这一档，动图最多只能到「长边 1080」
        // v1.10.532：档位说明**只显示当前选中的那一档**。
        // 原来是一整串"标准=…；高清=…；1:1=…"，读起来要先自己对号入座；
        // 现在副标题跟着值走，选哪档显示哪档，改档时一起刷新（见 openAnimQuality）。
        final int aqNow = WidgetRender.animQuality(this);
        View aqRow = buildValueRow(s(R.string.settings_anim_quality),
                s(animQualityHintRes(aqNow)),
                animQualityLabel(aqNow),
                v -> openAnimQuality(), 2);
        this.animHintView = hintOf(aqRow);
        host.addView(aqRow);

        // ② 预装帧（A 方案）
        host.addView(buildSwitchRow(s(R.string.settings_lab_flip),
                s(R.string.settings_lab_flip_hint),
                WidgetFlip.enabled(this),
                on -> {
                    WidgetFlip.setEnabled(this, Boolean.TRUE.equals(on));
                    Diag.warn(this, "A05", "预装帧模式=" + (Boolean.TRUE.equals(on) ? "开" : "关"));
                    refreshPerfSummary();
                }));

        // ③ URI 推送（B 方案）
        host.addView(buildSwitchRow(s(R.string.settings_lab_uri),
                s(R.string.settings_lab_uri_hint),
                WidgetUriPush.enabled(this),
                on -> {
                    WidgetUriPush.setEnabled(this, Boolean.TRUE.equals(on));
                    Diag.warn(this, "A05", "URI 推送=" + (Boolean.TRUE.equals(on) ? "开" : "关"));
                    refreshPerfSummary();
                }));

        // ①‴ 导出后提前烘帧（v1.10.196，默认关）
        host.addView(buildSwitchRow(s(R.string.settings_heat_export),
                s(R.string.settings_heat_export_hint),
                WidgetRender.prefHeatOnExport(this),
                on -> {
                    SettingsActivity.this.getSharedPreferences("settings", 0)
                            .edit().putBoolean(WidgetRender.PREF_HEAT_ON_EXPORT,
                                    Boolean.TRUE.equals(on)).apply();
                    Diag.warn(SettingsActivity.this, "A05",
                            "导出后提前烘帧=" + (Boolean.TRUE.equals(on) ? "开" : "关"));
                    SettingsActivity.this.refreshPerfSummary();
                }));

        // ④ v1.10.497（用户要求 1）：**性能锚点**从首页搬到这里 ——
        // 主标题就是「性能锚点」，副标题用大白话讲清「它在替你做什么、关掉会怎样」。
        // 首页那一格（连同它上面那条分隔线）已经删除，见 activity_main.xml 的注释。
        host.addView(buildSwitchRow(s(R.string.settings_anchor),
                s(R.string.settings_anchor_hint),
                OverlayService.anchorEnabledFor(this),
                on -> {
                    boolean v = Boolean.TRUE.equals(on);
                    getSharedPreferences("settings", 0).edit()
                            .putBoolean(OverlayService.PREF_ANCHOR, v).apply();
                    Diag.warn(this, "A05", "性能锚点=" + (v ? "开" : "关"));
                    // 打开时要让服务把那个 1×1 窗口建起来；关掉由服务的 300ms 看门狗 removeAnchor() 收掉
                    // （与首页原来的行为一致：只在「开」的时候叫一次 refresh）。
                    if (v) {
                        OverlayService.Companion.refresh(this);
                    }
                    refreshPerfSummary();
                }));

        restoreFoldNow(host, chevron, foldState("perf", false));
        // v1.10.272：收集本卡所有「主标题 + 副标题」的行，并按上次的状态摆好（不播动画）
        collectFoldRows(host);
        restoreSubtitleFold();
        // v1.10.522：整张卡按「分组圆角」重新铺一遍（外侧 14dp / 相邻内侧 6dp），间距 8dp → 4dp
        stylePerfGroup(host);
        header.setOnClickListener(v -> {
            boolean expand = host.getVisibility() != View.VISIBLE;
            animateExpand(host, expand);
            saveFold("perf", expand);
            if (chevron != null) {
                chevron.animate().rotation(expand ? 180.0f : 0.0f).setDuration(160L).start();
            }
            refreshPerfSummary();
        });
        refreshPerfSummary();
    }

    // ================================================================ v1.10.272 副标题收起 / 展开

    /**
     * v1.10.272（需求）：性能卡顶部那两个按钮 —— 「展开副标题 / 收起副标题」。
     *
     * 放在「相册扫描」**之上**，作用于本卡内所有带副标题的条目（见 {@link #collectFoldRows}）。
     * 用 {@code pill_soft} tag → 走 ThemeManager 的统一上色，换肤/对比度都跟着变。
     */
    private View buildSubtitleControlRow() {
        final float dp = getResources().getDisplayMetrics().density;
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setTag("fold_ctrl");
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        // v1.10.522：这一行也算分组的一部分 —— 与下面第一张行卡之间 8dp → 4dp
        blp.bottomMargin = (int) (dp * 4.0f);
        bar.setLayoutParams(blp);

        Button expand = subtitleFoldButton(s(R.string.settings_subtitle_expand));
        Button collapse = subtitleFoldButton(s(R.string.settings_subtitle_collapse));
        // v1.10.522：两颗按钮本身也是**横向分组**（左端大、内侧小），间距 8dp → 2dp
        expand.setTag("group_pill_left");
        collapse.setTag("group_pill_right");
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(
                0, (int) (dp * 36.0f), 1.0f);
        lp1.setMarginEnd((int) (dp * 2.0f));
        bar.addView(expand, lp1);
        bar.addView(collapse, new LinearLayout.LayoutParams(0, (int) (dp * 36.0f), 1.0f));
        expand.setOnClickListener(v -> setSubtitleFold(false));
        collapse.setOnClickListener(v -> setSubtitleFold(true));
        return bar;
    }

    /**
     * v1.10.522：**性能卡的「分组圆角」**。
     *
     * <p>这张卡里每一行本来就是「一枚 14dp 圆角的浅色块 + 8dp 间距」，一列排下来像一串
     * 互不相干的胶囊。按统一规则重排：</p>
     * <ul>
     *   <li>整列拼成**一个容器** —— 第一行只圆上面两角、最后一行只圆下面两角（外侧 14dp），
     *       中间行四角统一收到 6dp（相邻内侧小）；</li>
     *   <li>行间距 8dp → **4dp**；「展开 / 收起」那一行与第一张行卡之间同样收到 4dp
     *       （那个间距在 {@link #buildSubtitleControlRow()} 里设）。</li>
     * </ul>
     *
     * <p>底色沿用原来的「正文色 4.5%」，只换圆角与间距 —— 对比度、深浅色、换肤链路都不动。</p>
     */
    private void stylePerfGroup(@NotNull LinearLayout host) {
        float dp = density();
        ThemeColors c = ThemeManager.INSTANCE.colors(this);
        int count = 0;
        for (int i = 0; i < host.getChildCount(); i++) {
            if (!"fold_ctrl".contentEquals(String.valueOf(host.getChildAt(i).getTag()))) {
                count++;
            }
        }
        if (count <= 0) {
            return;
        }
        int index = 0;
        for (int i = 0; i < host.getChildCount(); i++) {
            View child = host.getChildAt(i);
            if ("fold_ctrl".contentEquals(String.valueOf(child.getTag()))) {
                continue;   // 控制行没有底色，间距已在它自己的 LayoutParams 里
            }
            int slot;
            if (count == 1) {
                slot = ThemeManager.GROUP_SINGLE;
            } else if (index == 0) {
                slot = ThemeManager.GROUP_FIRST;
            } else if (index == count - 1) {
                slot = ThemeManager.GROUP_LAST;
            } else {
                slot = ThemeManager.GROUP_MIDDLE;
            }
            child.setBackground(ThemeManager.INSTANCE.grouped(this,
                    fontAlpha(c.getText(), 0.045f), slot, true, 14.0f, 6.0f));
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (lp instanceof LinearLayout.LayoutParams) {
                ((LinearLayout.LayoutParams) lp).bottomMargin =
                        index == count - 1 ? 0 : (int) (dp * 4.0f);
                child.setLayoutParams(lp);
            }
            index++;
        }
    }

    private Button subtitleFoldButton(String text) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextSize(12.5f);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setIncludeFontPadding(false);
        b.setPadding(0, 0, 0, 0);
        // tag 由调用方给（v1.10.522 起是 group_pill_left / group_pill_right）
        b.setTag("pill_soft");
        return b;
    }

    /** 收集「主标题 + 副标题（+ 右侧控件）」的行 —— 只有这类行才参与收起。 */
    private void collectFoldRows(@NotNull ViewGroup host) {
        for (int i = 0; i < host.getChildCount(); i++) {
            View child = host.getChildAt(i);
            if (!(child instanceof LinearLayout)) {
                continue;
            }
            if ("fold_ctrl".contentEquals(String.valueOf(child.getTag()))) {
                continue;
            }
            LinearLayout row = (LinearLayout) child;
            if (row.getChildCount() < 1) {
                continue;
            }
            View block = row.getChildAt(0);
            if (!(block instanceof LinearLayout) || ((LinearLayout) block).getChildCount() < 2) {
                continue;
            }
            LinearLayout texts = (LinearLayout) block;
            FoldRow fr = new FoldRow();
            fr.row = row;
            fr.title = texts.getChildAt(0);
            fr.hint = texts.getChildAt(1);
            fr.control = row.getChildCount() > 1 ? row.getChildAt(1) : null;
            perfFoldRows.add(fr);
        }
    }

    /**
     * 按存储的状态摆好（**不播动画**）。
     *
     * 关键点：收起态不需要任何测量 —— 副标题 {@code GONE} + 行高 {@code WRAP_CONTENT}，
     * 行自然就是单行高度。所以进页面时不会出现"先展开一下再收起来"的闪。
     */
    private void restoreSubtitleFold() {
        boolean collapsed = foldState("perf_subtitles", false);
        this.perfFoldP = collapsed ? 1.0f : 0.0f;
        for (FoldRow r : perfFoldRows) {
            if (collapsed) {
                r.hint.setVisibility(View.GONE);
                r.hint.setAlpha(0.0f);
            } else {
                r.hint.setVisibility(View.VISIBLE);
                r.hint.setAlpha(1.0f);
                r.hint.setTranslationY(0.0f);
                r.hint.setScaleX(1.0f);
                r.hint.setScaleY(1.0f);
            }
            // v1.10.274：静息 = 副标题占位回到 WRAP_CONTENT（展开）或 0（收起）。
            // 行永远是 WRAP_CONTENT，所以这里不碰行高，也就没有任何"补位"的空间。
            ViewGroup.LayoutParams hlp = r.hint.getLayoutParams();
            if (hlp != null) {
                hlp.height = collapsed ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;
                r.hint.setLayoutParams(hlp);
            }
        }
    }

    /**
     * 收起 / 展开（0 或 1）。**动画途中再点，从当前进度原路反向** —— 不跳。
     *
     * 时长按"剩余距离"算（{@link FoldMotion#durationFor}）：半途反向只跑一半时长，
     * 速度保持一致；如果每帧都从 0 重新计时，观感会变成"越点越快"。
     */
    private void setSubtitleFold(boolean collapsed) {
        float target = collapsed ? 1.0f : 0.0f;
        if (perfFoldRows.isEmpty()) {
            return;
        }
        if (perfFoldAnim != null) {
            perfFoldAnim.cancel();
            perfFoldAnim = null;
        }
        saveFold("perf_subtitles", collapsed);
        Diag.warn(this, "A05", "性能项副标题=" + (collapsed ? "收起" : "展开")
                + " 条目数=" + perfFoldRows.size());
        if (Math.abs(target - perfFoldP) < 0.001f) {
            applyFoldResting();
            return;
        }
        for (FoldRow r : perfFoldRows) {
            if (r.hint.getVisibility() != View.VISIBLE) {
                r.hint.setVisibility(View.VISIBLE);
            }
        }
        ensureFoldSizes();
        final float from = perfFoldP;
        final android.animation.ValueAnimator a =
                android.animation.ValueAnimator.ofFloat(from, target);
        a.setDuration(FoldMotion.durationFor(from, target));
        // 这里的线性是**时间轴**（等价 AE 的合成时间），不是运动曲线：缓动全部由每个元素
        // 自己的三次贝塞尔完成（FoldMotion.CURVE_HINT / CURVE_ROW）。
        // 之所以时间轴必须线性：动画途中反向时 p 从当前值原路退回，各元素曲线自动反着走，
        // 一帧都不会跳；若时间轴本身再套一层 easing，反向就会在"时间被重映射"处突变。
        a.setInterpolator(new android.view.animation.LinearInterpolator());
        a.addUpdateListener(animation -> {
            Object v = animation.getAnimatedValue();
            perfFoldP = v instanceof Float ? ((Float) v).floatValue() : target;
            applyFold(perfFoldP);
        });
        a.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // v1.10.272（自测抓到的）：cancel() 也会走 onAnimationEnd。
                // 半途反向时旧动画的 end 回调会带着**旧目标**再跑一次 applyFoldResting()，
                // 把刚接上的新方向直接甩到终点（肉眼就是"抖一下"）。只认当前这条。
                if (perfFoldAnim != animation) {
                    return;
                }
                perfFoldAnim = null;
                perfFoldP = target;
                applyFoldResting();
            }
        });
        this.perfFoldAnim = a;
        a.start();
    }

    /**
     * 把进度 p 摊到每一行、每个元素上（**纯函数**：同一个 p 永远得到同一帧画面）。
     *
     * 副标题：位移 + 缩放 + 淡出（曲线 {@link FoldMotion#hintCurve()}，窗口 0~62%）；
     * 行高：fullH → singleH（曲线 {@link FoldMotion#rowCurve()}，窗口 34~100%）；
     * 行与行之间按 {@link FoldMotion#STAGGER} 错峰，窗口互相重叠。
     */
    private void applyFold(float p) {
        int n = perfFoldRows.size();
        for (int i = 0; i < n; i++) {
            FoldRow r = perfFoldRows.get(i);
            float pi = FoldMotion.rowProgress(p, i, n);
            float h = FoldMotion.hintCurve().at(
                    FoldMotion.window(pi, FoldMotion.HINT_FROM, FoldMotion.HINT_TO));
            float rh = clamp01(FoldMotion.rowCurve().at(
                    FoldMotion.window(pi, FoldMotion.ROW_FROM, FoldMotion.ROW_TO)));
            // 位移允许轻微过冲（曲线带 +2.66%），淡出提前收尾
            float shift = Math.min(h, 1.06f);
            r.hint.setTranslationY(-r.shiftPx * shift);
            r.hint.setAlpha(clamp01(1.0f - h * FoldMotion.HINT_FADE_GAIN));
            r.hint.setScaleX(1.0f - FoldMotion.HINT_SCALE_X * h);
            r.hint.setScaleY(1.0f - FoldMotion.HINT_SCALE_Y * h);
            if (r.slot0 <= 0.0f) {
                continue;
            }
            // v1.10.274：**唯一会动的几何量是副标题的占位高度**（按行高曲线收）。
            // 行本身永远是 WRAP_CONTENT —— 高度由内容撑出来，不可能裁到标题；
            // 主标题 / 勾选框的纵向位置由布局每帧重算（连续性从这来，且不可能量错）。
            ViewGroup.LayoutParams hlp = r.hint.getLayoutParams();
            if (hlp != null) {
                int slot = Math.round(FoldMotion.slotHeight(r.slot0, rh));
                if (hlp.height != slot) {
                    hlp.height = slot;
                    r.hint.setLayoutParams(hlp);
                }
            }
        }
    }

    /** 动画收尾：把"显式高度"换回 WRAP_CONTENT，并让副标题在收起态彻底不占位。 */
    private void applyFoldResting() {
        boolean collapsed = perfFoldP >= 0.999f;
        for (FoldRow r : perfFoldRows) {
            // v1.10.273：静息态与"动画最后一帧"在数值上完全一致：
            //   · 位移归零 + 文字块 WRAP_CONTENT → 主标题落在 (内框 - 标题高)/2（= 中央）
            //   · 最后一帧的 位移 + (内框 - 文字块高)/2 算出来也是同一个数
            // 所以这里换回 WRAP_CONTENT 不会有任何像素跳动（验收判据：结束帧 == 结束后一帧）。
            // v1.10.274：静息 = 副标题占位回到 WRAP_CONTENT（展开）或 0（收起）。
            // 行永远是 WRAP_CONTENT，所以这里不碰行高，也就没有任何"补位"的空间。
            ViewGroup.LayoutParams hlp = r.hint.getLayoutParams();
            if (hlp != null) {
                hlp.height = collapsed ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;
                r.hint.setLayoutParams(hlp);
            }
            if (collapsed) {
                r.hint.setVisibility(View.GONE);
                r.hint.setAlpha(0.0f);
            } else {
                r.hint.setVisibility(View.VISIBLE);
                r.hint.setAlpha(1.0f);
                r.hint.setTranslationY(0.0f);
                r.hint.setScaleX(1.0f);
                r.hint.setScaleY(1.0f);
            }
        }
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }

    /**
     * 动画开始前算一次首尾状态 —— **只需要一个量**：副标题的自然占位高度。
     *
     * v1.10.274 起不再量标题高/控件高/块高：那三个数一旦和布局的实际值不一致，
     * 被钉死的文字块就会比行高而遭裁切（v1.10.273 用户看到的"标题被削掉半行"就是它）。
     * 现在行保持 `WRAP_CONTENT`，副标题占位从 `hint.getHeight()` 收到 0，
     * 其它位置全由布局算 —— 布局不会算错自己。
     */
    private void ensureFoldSizes() {
        final float dp = getResources().getDisplayMetrics().density;
        for (FoldRow r : perfFoldRows) {
            if (r.hint.getVisibility() != View.VISIBLE) {
                r.hint.setVisibility(View.VISIBLE);
            }
            ViewGroup.LayoutParams hlp = r.hint.getLayoutParams();
            if (hlp != null && hlp.height >= 0) {
                hlp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                r.hint.setLayoutParams(hlp);
            }
            if (r.hint.getHeight() > 0) {
                r.slot0 = r.hint.getHeight();
            } else {
                // 收起态点"展开"时，副标题当前高度是 0（还没布局出 WRAP_CONTENT），
                // getHeight() 拿不到真值 → **按它自己的宽度量一次自然高度**（换行数一致）。
                // 这里绝不能用"猜"：猜小了副标题文字就会被自己的高度裁掉。
                r.slot0 = measureHintSlot(r);
            }
            int titleH = r.title.getHeight() > 0 ? r.title.getHeight()
                    : (r.title instanceof TextView ? ((TextView) r.title).getLineHeight()
                            : (int) (dp * 20.0f));
            r.shiftPx = Math.max(dp * 8.0f, r.hint.getTop() + titleH * 0.45f);
        }
    }

    /** 按副标题**自己的宽度**量它的自然高度（含上内边距）；拿不到宽度时返回 0。 */
    private float measureHintSlot(@NotNull FoldRow r) {
        final float dp = getResources().getDisplayMetrics().density;
        int w = r.hint.getWidth();
        if (w <= 0 && r.row.getWidth() > 0) {
            w = r.row.getWidth() - r.row.getPaddingLeft() - r.row.getPaddingRight();
            if (r.control != null) {
                w -= r.control.getWidth() > 0 ? r.control.getWidth() : (int) (dp * 10.0f);
            }
            w -= (int) (dp * 10.0f);   // 右侧控件与文字块之间的间距
        }
        if (w <= 0) {
            w = getResources().getDisplayMetrics().widthPixels;
        }
        r.hint.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return Math.max(0.0f, r.hint.getMeasuredHeight());
    }

    private int measureRowHeight(@NotNull View row, int width) {
        row.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return row.getMeasuredHeight();
    }

    /**
     * v1.10.179（反馈 2）：配色卡 —— 「预设配色」改名「配色方案」，整张卡可折叠。
     *
     * 折叠方式与「界面语言 / 系统模式 / 字体 / 性能」**完全同一套**：
     * `animateExpand`（只改高度 + alpha，250ms Decelerate）+ 箭头 160ms 转 180° +
     * 折叠状态落盘（`fold_palette`），回来时用 `restoreFoldNow` 摆好、不播动画。
     */
    private void bindPaletteCard() {
        final View header = findViewById(R.id.row_palette_header);
        final View body = findViewById(R.id.ll_palette_body);
        final View chevron = findViewById(R.id.btn_palette_toggle);
        if (header == null || body == null) {
            return;
        }
        restoreFoldNow(body, chevron, foldState("palette", true));
        header.setOnClickListener(v -> {
            boolean expand = body.getVisibility() != View.VISIBLE;
            animateExpand(body, expand);
            saveFold("palette", expand);
            if (chevron != null) {
                chevron.animate().rotation(expand ? 180.0f : 0.0f).setDuration(160L).start();
            }
        });
        if (chevron != null) {
            chevron.setOnClickListener(v -> header.performClick());
        }
    }

    /**
     * v1.10.179（反馈 2）：写对比度的**唯一出口**。
     *
     * 「系统预设」之间共享一个对比度：第 1 个预设 +1，切到第 2 个预设仍是 +1。
     * 实现上就是「预设模式下顺手把共享值也写一遍」；自定义方案不走这里
     * （它有自己的 `scheme_contrast_<id>`，切方案时由 writeSchemeColors 恢复）。
     */
    private void writeContrast(int x100) {
        ThemePrefs.INSTANCE.setContrastX100(this, x100);
        if (ThemePrefs.INSTANCE.getActiveScheme(this) == -1L) {
            ThemePrefs.INSTANCE.setPresetContrastX100(this, x100);
        }
    }

    /** 摘要：`6 线程 · 预装帧 ✓ · URI 推送 ✓`（没开就显示 —，不额外造字符串）。 */
    private void refreshPerfSummary() {
        TextView view = (TextView) findViewById(R.id.tv_perf_value);
        if (view == null) {
            return;
        }
        view.setText(s(R.string.settings_scan_threads_value,
                new Object[]{Integer.valueOf(AnimatedPickerActivity.scanThreads(this))})
                + " · " + s(R.string.settings_lab_flip) + (WidgetFlip.enabled(this) ? " ✓" : " —")
                + " · " + s(R.string.settings_lab_uri) + (WidgetUriPush.enabled(this) ? " ✓" : " —"));
    }

    private void openScanThreads() {
        final int current = AnimatedPickerActivity.scanThreads(this);
        new NumDialog(this, s(R.string.settings_scan_threads), String.valueOf(current),
                "1 ~ 8", NumDialog.signedIntInputType())
                .onOk(text -> {
                    int typed = current;
                    try {
                        typed = Integer.parseInt(String.valueOf(text).trim());
                    } catch (Throwable ignored) {
                    }
                    int v = typed < 1 ? 1 : (typed > 8 ? 8 : typed);
                    SettingsActivity.this.getSharedPreferences("settings", 0)
                            .edit().putInt("scan_threads", v).apply();
                    TextView value = SettingsActivity.this.perfThreadValue;
                    if (value != null) {
                        value.setText(SettingsActivity.this.s(R.string.settings_scan_threads_value,
                                new Object[]{Integer.valueOf(v)}));
                    }
                    Diag.warn(SettingsActivity.this, "A05", "相册扫描线程数=" + v);
                    SettingsActivity.this.refreshPerfSummary();
                }).show();
    }

    /**
     * v1.10.191：预热线程数（4~8）。
     *
     * 预热是 MIN_PRIORITY，和桌面播放抢 CPU：太小 → 冷窗口长（改尺寸后画面追不上）；
     * 太大 → 播放被抢，反而掉帧。给用户自己找平衡点。
     */
    /** v1.10.254：动图分辨率当前档位的人话（行上显示真值，不是输入值）。 */
    private String animQualityLabel(int level) {
        if (level >= 2) {
            return s(R.string.settings_anim_quality_2);
        }
        if (level == 1) {
            return s(R.string.settings_anim_quality_1);
        }
        return s(R.string.settings_anim_quality_0);
    }

    /** v1.10.254：动图分辨率三档（0/1/2）。改完立刻重推，不用去桌面拖组件。 */
    private void openAnimQuality() {
        final int current = WidgetRender.animQuality(this);
        new NumDialog(this, s(R.string.settings_anim_quality), String.valueOf(current),
                "0 ~ 2", NumDialog.signedIntInputType())
                .onOk(text -> {
                    int typed = current;
                    try {
                        typed = Integer.parseInt(String.valueOf(text).trim());
                    } catch (Throwable ignored) {
                    }
                    int v = typed < 0 ? 0 : (typed > 2 ? 2 : typed);
                    WidgetRender.setAnimQuality(SettingsActivity.this, v);
                    int real = WidgetRender.animQuality(SettingsActivity.this);
                    Diag.warn(SettingsActivity.this, "A05", "动图分辨率=" + real);
                    TextView valueView = SettingsActivity.this.animValueView;
                    if (valueView != null) {
                        valueView.setText(SettingsActivity.this.animQualityLabel(real));
                    }
                    // v1.10.532：副标题一起换成新档的说明
                    TextView hintView = SettingsActivity.this.animHintView;
                    if (hintView != null) {
                        hintView.setText(SettingsActivity.this.s(
                                SettingsActivity.this.animQualityHintRes(real)));
                    }
                    refreshPerfSummary();
                    final Context app = getApplicationContext();
                    Thread t = new Thread(new Runnable() { // from class: io.github.animwidget.SettingsActivity$animQualityRepush
                        @Override // java.lang.Runnable
                        public void run() {
                            WidgetRender.INSTANCE.repushAll(app, "动图分辨率=" + real);
                        }
                    }, "anim-quality-repush");
                    t.start();
                })
                .show();
    }

    private void openPrewarmThreads() {
        final int current = WidgetPrewarm.threadSetting(this);
        // v1.10.497：范围上界按这台手机的核数写死显示（4 核机填 8 会被夹成 4，
        // 一直写「4 ~ 8」等于骗人）。下界同样是 4（线程太少时冷窗口追不上播放）。
        int ceiling = Math.max(4, Math.min(8, Runtime.getRuntime().availableProcessors()));
        new NumDialog(this, s(R.string.settings_prewarm_threads), String.valueOf(current),
                "4 ~ " + ceiling, NumDialog.signedIntInputType())
                .onOk(text -> {
                    int typed = current;
                    try {
                        typed = Integer.parseInt(String.valueOf(text).trim());
                    } catch (Throwable ignored) {
                    }
                    int v = typed < 4 ? 4 : (typed > 8 ? 8 : typed);
                    SettingsActivity.this.getSharedPreferences("settings", 0)
                            .edit().putInt(WidgetPrewarm.PREF_THREADS, v).apply();
                    // 实际生效值可能被核数夹住，显示真值而不是输入值（不许拿输入值当结果）
                    int real = WidgetPrewarm.threadSetting(SettingsActivity.this);
                    TextView value = SettingsActivity.this.perfPrewarmValue;
                    if (value != null) {
                        value.setText(SettingsActivity.this.s(R.string.settings_prewarm_threads_value,
                                new Object[]{Integer.valueOf(real)}));
                    }
                    Diag.warn(SettingsActivity.this, "A05", "预热线程数=" + v + "（下次烘帧起生效，实际值=" + real + "）");
                    SettingsActivity.this.refreshPerfSummary();
                }).show();
    }

    /** 卡内行底色：极淡的一层（比卡片再深/浅一点），靠间距与圆角分层，不用分割线。 */
    @NotNull
    private GradientDrawable rowBg(@NotNull ThemeColors c, float alpha) {
        float dp = getResources().getDisplayMetrics().density;
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(fontAlpha(c.getText(), alpha));
        d.setCornerRadius(14.0f * dp);
        return d;
    }

    @NotNull
    private LinearLayout.LayoutParams rowParams() {
        float dp = getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (dp * 8.0f);
        return lp;
    }

    @NotNull
    private LinearLayout rowShell() {
        float dp = getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int padH = (int) (dp * 12.0f);
        int padV = (int) (dp * 10.0f);
        row.setPadding(padH, padV, padH, padV);
        row.setLayoutParams(rowParams());
        row.setBackground(rowBg(ThemeManager.INSTANCE.colors(this), 0.045f));
        return row;
    }

    @NotNull
    private LinearLayout textBlock(@NotNull String title, @NotNull String hint) {
        float dp = getResources().getDisplayMetrics().density;
        ThemeColors c = ThemeManager.INSTANCE.colors(this);
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(14.5f);
        t.setTextColor(c.getText());
        TextView h = new TextView(this);
        h.setText(hint);
        h.setTextSize(11.0f);
        // v1.10.196（反馈）：性能卡里这些长副标题太抢眼 —— 统一压到低对比度的灰。
        // v1.10.197 修：只设颜色不够 —— 这一块没有 tag，ThemeManager.paint() 走默认分支
        // 会把它重新染成正文色（于是看起来和标题一样亮）。这里打 muted_quiet，让换肤链路认它。
        // v1.10.199：副标题 = 中性变体色板（按对比度自适应到 4.5:1），不是 alpha 叠在背景上
        h.setTag("muted_quiet");
        h.setTextColor(ThemeManager.INSTANCE.subtitleColor(this));
        h.setPadding(0, (int) (dp * 2.0f), 0, 0);
        texts.addView(t);
        texts.addView(h);
        return texts;
    }

    @NotNull
    /** v1.10.254：动图分辨率那一行的值视图（改完档位要立刻把新值写上）。 */
    private TextView animValueView;

    /** v1.10.532：动图分辨率那一行的**副标题**视图（只显示当前档的说明）。 */
    @Nullable
    private TextView animHintView;

    /** 当前档位对应的说明（0 标准 / 1 高清 / 2 1:1）。 */
    private int animQualityHintRes(int level) {
        if (level >= 2) {
            return R.string.settings_anim_quality_hint_2;
        }
        if (level == 1) {
            return R.string.settings_anim_quality_hint_1;
        }
        return R.string.settings_anim_quality_hint_0;
    }

    /** 从 {@link #buildValueRow} 造出来的行里取出副标题 TextView（结构：行 > 文本块 > [标题, 副标题]）。 */
    @Nullable
    private static TextView hintOf(@Nullable View row) {
        if (!(row instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) row;
        if (group.getChildCount() == 0) {
            return null;
        }
        View block = group.getChildAt(0);
        if (!(block instanceof ViewGroup)) {
            return null;
        }
        ViewGroup texts = (ViewGroup) block;
        if (texts.getChildCount() < 2) {
            return null;
        }
        View hint = texts.getChildAt(1);
        return hint instanceof TextView ? (TextView) hint : null;
    }

    private View buildValueRow(@NotNull String title, @NotNull String hint, @NotNull String value,
                              @Nullable View.OnClickListener onClick) {
        return buildValueRow(title, hint, value, onClick, 0);
    }

    /** v1.10.191：slot=1 → 值视图记到 perfPrewarmValue（预热线程数那一行）；v1.10.254 起 =2 动图分辨率。 */
    private View buildValueRow(@NotNull String title, @NotNull String hint, @NotNull String value,
                              @Nullable View.OnClickListener onClick, int valueSlot) {
        float dp = getResources().getDisplayMetrics().density;
        LinearLayout row = rowShell();
        row.addView(textBlock(title, hint));
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(13.0f);
        v.setTextColor(ThemeManager.INSTANCE.colors(this).getPrimary());
        v.setPadding((int) (dp * 10.0f), 0, 0, 0);
        if (valueSlot == 2) {
            this.animValueView = v;
        } else if (valueSlot == 1) {
            this.perfPrewarmValue = v;
        } else {
            this.perfThreadValue = v;
        }
        row.addView(v);
        if (onClick != null) {
            row.setClickable(true);
            row.setOnClickListener(onClick);
        }
        return row;
    }

    /**
     * 自绘开关：26dp 圆角框 + 勾，开=主色淡底 + 主色描边，关=极淡描边。
     *
     * v1.10.202（反馈「切系统深浅色后选项框只剩个空心灰框，点一下才亮」）：
     * 原来是 `FrameLayout + GradientDrawable` + `GlyphView`。颜色在建视图那一刻就烘进 drawable 了，
     * 而 `ThemeManager.paint()` 只认 tag（card / pill_soft / Text / Button…），
     * 碰不到一个裸的 FrameLayout —— 换肤后这一格永远停在旧配色（深色底上≈看不见）。
     * 而「切系统深浅色」走的是 `onConfigurationChanged` / `themePoll` → `refreshThemeInPlace`，
     * 那条路**根本不重建**性能卡，所以 v1.10.201 在别处补的重建也救不了它。
     *
     * 现在换成 {@link SwitchBoxView}：颜色在 onDraw 里按当前 ThemeColors 现算，并接进 paint() 链路，
     * 任何换肤入口（哪怕没人记得重建）都会画对。
     */
    @NotNull
    private View buildSwitchRow(@NotNull String title, @NotNull String hint, boolean initial,
                               @NotNull java.util.function.Consumer<Boolean> onChange) {
        final float dp = getResources().getDisplayMetrics().density;
        LinearLayout row = rowShell();
        row.addView(textBlock(title, hint));

        SwitchBoxView box = new SwitchBoxView(this);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                (int) (dp * 26.0f), (int) (dp * 26.0f));
        blp.setMarginStart((int) (dp * 10.0f));
        box.setLayoutParams(blp);
        box.setOn(initial);
        row.addView(box);

        row.setClickable(true);
        row.setOnClickListener(v -> {
            boolean next = !box.isOn();
            box.setOn(next);
            box.setScaleX(0.84f);
            box.setScaleY(0.84f);
            box.animate().scaleX(1.0f).scaleY(1.0f).setDuration(170L)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            onChange.accept(Boolean.valueOf(next));
        });
        return row;
    }

    private void toggleFontPanel() {
        LinearLayout host = this.llFonts;
        if (host == null) {
            return;
        }
        boolean expand = host.getVisibility() != View.VISIBLE;
        if (expand) {
            buildFontList();
        }
        // 与「界面语言」「系统模式」完全同一套动画
        animateExpand(host, expand);
        if (this.btnFontToggle != null) {
            this.btnFontToggle.animate().rotation(expand ? 180.0f : 0.0f).setDuration(160L).start();
        }
    }

    private String fontRoleLabel(String role) {
        if (Fonts.ROLE_ZH.equals(role)) {
            return s(R.string.font_role_zh);
        }
        if (Fonts.ROLE_EN.equals(role)) {
            return s(R.string.font_role_en);
        }
        return s(R.string.font_role_both);
    }

    // ---------------------------------------------------------------- 字体列表

    private static final android.view.animation.PathInterpolator EASE_OUT =
            new android.view.animation.PathInterpolator(0.20f, 0.62f, 0.28f, 1.00f);
    private static final android.view.animation.PathInterpolator EASE_IN_OUT =
            new android.view.animation.PathInterpolator(0.45f, 0.00f, 0.40f, 1.00f);

    /** 字体列表的一行：选中态拆成独立图层（药丸底 + 勾），才能做「从右侧淡入 / 淡出」。 */
    private static final class FontRow {
        final Fonts.Item item;
        final View row;
        final View pill;
        final GlyphView check;

        FontRow(Fonts.Item item, View row, View pill, GlyphView check) {
            this.item = item;
            this.row = row;
            this.pill = pill;
            this.check = check;
        }
    }

    private final java.util.List<FontRow> fontRows = new java.util.ArrayList<>();

    private void buildFontList() {
        buildFontList(null);
    }

    /**
     * 重建字体列表：跟随系统 / 内置 / 已导入 + 导入按钮 + 说明。
     *
     * @param animateInId 新加的条目 id（非空时做「从右侧淡入 + 下面按钮曲线回落」）
     */
    private void buildFontList(String animateInId) {
        final LinearLayout host = this.llFonts;
        if (host == null) {
            return;
        }
        host.removeAllViews();
        this.fontRows.clear();
        ThemeColors c = ThemeManager.INSTANCE.colors(this);
        float dp = getResources().getDisplayMetrics().density;
        String active = Fonts.activeId(this);
        // v1.10.156（反馈）：字体大小滑块放在字体列表**最上面**（“跟随系统”那一行之前）
        host.addView(buildFontSizeRow(c, dp));
        java.util.List<Fonts.Item> items = Fonts.items(this);
        for (int i = 0; i < items.size(); i++) {
            host.addView(buildFontRow(items.get(i), active, c));
        }

        Button importBtn = new Button(this);
        importBtn.setText(R.string.settings_font_import);
        importBtn.setTextSize(12.5f);
        importBtn.setAllCaps(false);
        importBtn.setTextColor(c.getPrimary());
        importBtn.setBackground(ThemeManager.INSTANCE.rounded(this, fontAlpha(c.getPrimary(), 0.12f), 999.0f));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (dp * 44.0f));
        ilp.topMargin = (int) (dp * 8.0f);
        importBtn.setLayoutParams(ilp);
        importBtn.setOnClickListener(v -> requestFontImport());
        host.addView(importBtn);

        TextView hint = new TextView(this);
        hint.setText(R.string.settings_font_longpress);
        hint.setTextSize(10.5f);
        hint.setTextColor(c.getTextMuted());
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = (int) (dp * 7.0f);
        hint.setLayoutParams(hlp);
        host.addView(hint);

        if (this.tvFontValue != null) {
            String name = Fonts.displayName(this, active);
            this.tvFontValue.setText(name == null ? s(R.string.settings_font_system) : name);
        }
        Fonts.applyTree(host);
        if (animateInId != null) {
            animateRowIn(host, animateInId);
        }
    }

    /**
     * 反馈 2「添加」：条目隐显 + 导入字体按钮曲线下移。
     * 做法是把新行做成淡入，然后把**它后面所有兄弟**从「-行高」的位移曲线归位 ——
     * 只用 translation（GPU 合成），不逐帧改布局，所以不会有一下一下的硬感。
     */
    private void animateRowIn(final LinearLayout host, final String id) {
        host.post(() -> {
            int index = -1;
            FontRow target = null;
            for (int i = 0; i < this.fontRows.size(); i++) {
                if (this.fontRows.get(i).item.id.equals(id)) {
                    index = i;
                    target = this.fontRows.get(i);
                    break;
                }
            }
            if (target == null || index < 0) {
                return;
            }
            final float dp = getResources().getDisplayMetrics().density;
            final View row = target.row;
            final int h = row.getHeight();
            if (h <= 0) {
                return;
            }
            row.setAlpha(0.0f);
            row.setTranslationX(16.0f * dp);
            row.animate().alpha(1.0f).translationX(0.0f)
                    .setDuration(240L).setInterpolator(EASE_IN_OUT).start();
            for (int i = index + 1; i < host.getChildCount(); i++) {
                View sibling = host.getChildAt(i);
                sibling.setTranslationY(-h);
                sibling.animate().translationY(0.0f)
                        .setDuration(300L)
                        .setStartDelay((i - index - 1) * 12L)
                        .setInterpolator(EASE_IN_OUT).start();
            }
        });
    }

    /**
     * v1.10.156（反馈）：**字体大小**四档滑块（小 / 默认 / 中 / 大）。
     *
     * 口径：在**每个 TextView 当前的 sp 值**上乘系数（0.86 / 1.00 / 1.14 / 1.28），
     * 不是把全应用统一设成一个字号；原值只记一次，反复刷新不会越放越大。
     * 胶囊按钮文本、日志页、导出历史页、导出日志弹窗不参与（见 Fonts.noScale）。
     */
    @NotNull
    private View buildFontSizeRow(@NotNull ThemeColors c, float dp) {
        // v1.10.162（反馈）：最外层换成 GlowRowLayout —— 它只负责在子视图画完之后补画光晕。
        // 内边距 / 背景 / 外边距 / 内部结构全部沿用原值，行高与位置逐像素不变。
        GlowRowLayout outer = new GlowRowLayout(this);
        int padH = (int) (dp * 12.0f);
        int padV = (int) (dp * 10.0f);
        outer.setPadding(padH, padV, padH, padV);
        outer.setBackground(rowBg(c, 0.045f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (dp * 8.0f);
        outer.setLayoutParams(lp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        outer.addView(row, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText(R.string.settings_font_size);
        title.setTextSize(14.5f);
        title.setTextColor(c.getText());
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        final TextView value = new TextView(this);
        final int startIndex = Fonts.textScaleIndex(this);
        value.setText(Fonts.textScaleLabelRes(startIndex));
        value.setTextSize(13.0f);
        value.setTextColor(c.getPrimary());
        head.addView(value);
        row.addView(head);

        final FontScaleSlider slider = new FontScaleSlider(this);
        slider.setIndex(startIndex);
        final int[] shownIndex = {startIndex};
        // v1.10.159（重做）：原生 SeekBar → 自绘 FontScaleSlider。
        // 拇指由本类自己画（inset = 半径 + 2dp，物理上不可能被裁）；
        // 按下不动、拖动跟手、松手用 260ms 曲线落档，**落定后才回调 onSettle**，
        // 也就是「先看见拇指走到位，再看见字号长大」，两段都不会被遮罩盖掉。
        slider.setListener(new FontScaleSlider.Listener() {
            @Override
            public void onPreview(int i) {
                setScaleValueLabel(value, i);
            }

            @Override
            public void onSettle(int i) {
                setScaleValueLabel(value, i);
                applyIndex(i, value, shownIndex);
            }
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = (int) (dp * 2.0f);
        row.addView(slider, slp);
        outer.setAnchor(slider);
        return outer;
    }

    /**
     * 右上角档位名。
     *
     * v1.10.160（反馈 3）：原来落档时把 alpha 从 0.45 动画回 1.0，用户看到的是
     * **明显闪一下**，而且跟页面的字号过渡对不上。现在只换文字，不动 alpha / 缩放 ——
     * 「一起变」交给 {@link Fonts#animateTextScale} 那条逐帧字号过渡去表达。
     */
    private void setScaleValueLabel(TextView tv, int i) {
        if (tv == null) {
            return;
        }
        tv.animate().cancel();
        tv.setAlpha(1.0f);
        tv.setScaleX(1.0f);
        tv.setScaleY(1.0f);
        tv.setText(Fonts.textScaleLabelRes(i));
    }

    /** 系统「减少动态效果」（动画时长倍数=0）时返回 0，否则按倍数收敛到 ≤1。 */
    private long motionMs(long base) {
        float scale = 1.0f;
        try {
            scale = android.provider.Settings.Global.getFloat(getContentResolver(),
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f);
        } catch (Throwable ignored) {
        }
        if (scale <= 0.0f) {
            return 0L;
        }
        if (scale > 1.0f) {
            scale = 1.0f;
        }
        long out = Math.round(base * scale);
        return out < 1L ? 1L : out;
    }

    /**
     * v1.10.159（重做）：语言面板刷新 —— 文案、选中态、配色一次刷完，不重建视图。
     *
     * 三行的口径（对照参考实现选的两条主流做法）：
     *  · 主行 = **语言自名**（"English" / "中文"），不随界面语言翻译 ——
     *    不然切到英文后「English」变成「英语」，用户就找不着自己熟悉的那个词了
     *    （Bitwarden `AppLanguage` 用 `localeName` + 自名同款）；
     *  · 次行 = **当前界面语言里对这门的写法**（Mastodon 用
     *    `getDisplayLanguage(Locale.getDefault())` 同款），两行重复时次行隐藏；
     *  · 「跟随系统」行的次行写**此刻实际生效**的语言 ——
     *    这就是「选中态与实际语言不同步」的正面答案：不点开系统设置也知道会落成什么。
     */
    private void refreshLanguageRows() {
        LinearLayout list = this.langGroup;
        if (list == null) {
            return;
        }
        ThemeColors c = ThemeManager.INSTANCE.colors(this);
        int mode = LocaleHelper.INSTANCE.get(this);
        // 界面现在真正在用的语言（I18nActivity 原地换语言会覆盖 getResources()）
        Locale ui = firstLocale(getResources().getConfiguration());
        // 系统语言：取 applicationContext，不受本页覆盖影响
        Locale sys = firstLocale(getApplicationContext().getResources().getConfiguration());
        Locale zh = new Locale("zh");
        Locale en = Locale.ENGLISH;

        bindLangRow(R.id.rb_lang_system, mode == LocaleHelper.FOLLOW_SYSTEM,
                s(R.string.settings_language_system),
                displayIn(sys, ui), c);
        bindLangRow(R.id.rb_lang_zh, mode == LocaleHelper.CHINESE,
                displayIn(zh, zh), differ(displayIn(zh, ui), displayIn(zh, zh)) ? displayIn(zh, ui) : "", c);
        bindLangRow(R.id.rb_lang_en, mode == LocaleHelper.ENGLISH,
                displayIn(en, en), differ(displayIn(en, ui), displayIn(en, en)) ? displayIn(en, ui) : "", c);
    }

    private void bindLangRow(int viewId, boolean selected, CharSequence title,
                             CharSequence sub, ThemeColors c) {
        View v = findViewById(viewId);
        if (!(v instanceof LanguageRowView)) {
            return;
        }
        LanguageRowView row = (LanguageRowView) v;
        row.applyTheme(c);
        row.bind(title, sub, selected, true);
    }

    private static Locale firstLocale(Configuration cfg) {
        android.os.LocaleList list = cfg.getLocales();
        if (list == null || list.isEmpty()) {
            return Locale.getDefault();
        }
        return list.get(0);
    }

    /** 用 `viewer` 这门语言写 `target` 的名字（ICU 数据，不是硬编码）。 */
    private static String displayIn(Locale target, Locale viewer) {
        try {
            return target.getDisplayLanguage(viewer);
        } catch (Throwable th) {
            return target.getLanguage();
        }
    }

    private static boolean differ(String a, String b) {
        return a != null && b != null && !a.equalsIgnoreCase(b);
    }

    /**
     * 落库 + 记日志 + **字号连续长大**。
     *
     * v1.10.159（重做）：原来这里是 `Morph.crossfade`（整页截图淡出），文字是**跳变**到新
     * 尺寸再被旧位图盖住 —— 感知就是「瞬间到达」，而且旧位图会把滑块刚走完的位移也盖掉。
     * 现在改成逐帧改 `setTextSize`（`Fonts.animateTextScale`），中间帧是真实排版。
     */
    private void applyIndex(int progress, TextView value, int[] shown) {
        if (shown[0] == progress) {
            return;
        }
        final int prev = shown[0];
        shown[0] = progress;
        Fonts.setTextScaleIndex(this, progress);
        if (value != null) {
            value.setText(Fonts.textScaleLabelRes(progress));
        }
        String[] names = {"小", "默认", "中", "大"};
        long dur = motionMs(240L);
        Diag.warn(this, "A07", "字号=" + names[progress]
                + " 系数=×" + String.format(java.util.Locale.US, "%.2f", Fonts.committedTextScale(this))
                + " 过渡=" + dur + "ms 由=" + names[prev]);
        Fonts.animateTextScale(getWindow().getDecorView(),
                Fonts.textScaleAt(prev), Fonts.textScaleAt(progress), dur, null);
    }

    private View buildFontRow(final Fonts.Item it, String active, ThemeColors c) {
        final float dp = getResources().getDisplayMetrics().density;
        boolean selected = it.id.equals(active);

        // v1.10.101（反馈 1）：行高必须**定死**。
        // 之前行是 WRAP_CONTENT，而药丸底是 MATCH_PARENT —— 面板做高度展开动画时
        // 父容器那几帧的高度是「固定像素」，FrameLayout 就把 MATCH_PARENT 的药丸撑满了整块，
        // 展开的一秒里能看到它一格一格被拉长。定高以后药丸永远是行高，跟父容器动画无关。
        // 高度不写死 44：跟着字号走（系统字体放大也不会裁字）。
        FrameLayout row = new FrameLayout(this);
        int lineH = Math.max(1, (int) (14.0f * dp * 1.28f));
        int rowH = Math.max((int) (dp * 44.0f), lineH + (int) (dp * 20.0f));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, rowH);
        rowLp.topMargin = (int) (dp * 3.0f);
        row.setLayoutParams(rowLp);

        // 选中态的药丸底：独立图层，切换时从右侧滑入/滑出
        final View pill = new View(this);
        pill.setBackground(ThemeManager.INSTANCE.rounded(this, fontAlpha(c.getPrimary(), 0.12f), 14.0f));
        pill.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        pill.setAlpha(selected ? 1.0f : 0.0f);
        pill.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        row.addView(pill);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(android.view.Gravity.CENTER_VERTICAL);
        content.setPadding((int) (dp * 12.0f), 0, (int) (dp * 12.0f), 0);
        content.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        row.addView(content);

        TextView name = new TextView(this);
        name.setText(it.name);
        name.setTextSize(14.0f);
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setTextColor(c.getText());
        // 名字要一直用它自己的字体（预览），所以不接受全局换字
        name.setTag("no_font");
        name.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        content.addView(name);

        TextView chip = new TextView(this);
        chip.setText(fontRoleLabel(it.role));
        chip.setTextSize(10.0f);
        chip.setTextColor(fontAlpha(c.getText(), 0.52f));
        chip.setPadding((int) (dp * 7.0f), (int) (dp * 2.0f),
                (int) (dp * 7.0f), (int) (dp * 2.0f));
        chip.setBackground(ThemeManager.INSTANCE.rounded(this, fontAlpha(c.getText(), 0.07f), 999.0f));
        content.addView(chip);

        GlyphView check = new GlyphView(this);
        check.setGlyph(GlyphView.CHECK);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                (int) (dp * 26.0f), (int) (dp * 22.0f));
        clp.leftMargin = (int) (dp * 6.0f);
        check.setLayoutParams(clp);
        check.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        content.addView(check);

        final FontRow holder = new FontRow(it, row, pill, check);
        this.fontRows.add(holder);

        row.setOnClickListener(v -> selectFontRow(holder));
        if (Fonts.canDelete(it)) {
            row.setOnLongClickListener(v -> {
                deleteFontRow(holder);
                return true;
            });
        } else {
            row.setOnLongClickListener(v -> {
                Toast.makeText(this, R.string.font_builtin_locked, Toast.LENGTH_SHORT).show();
                return true;
            });
        }
        Fonts.applyTree(content);
        android.graphics.Typeface preview = Fonts.previewTypeface(this, it.id);
        if (preview != null) {
            name.setTypeface(preview);
        }
        return row;
    }

    /**
     * 反馈 2「切换」：新选中的药丸底 + 勾从右侧淡入，被取消的从右侧淡出；
     * 整个页面的字体变化走 {@link Morph#crossfade} 叠化，不硬刷。
     */
    private void selectFontRow(final FontRow target) {
        if (this.fontDeleting) {
            return;
        }
        final String active = Fonts.activeId(this);
        if (target.item.id.equals(active)) {
            return;
        }
        Morph.crossfade(this, () -> {
            animateSelection(active, target);
            Fonts.setActiveId(SettingsActivity.this, target.item.id);
            Fonts.refresh(SettingsActivity.this.getWindow().getDecorView());
            if (SettingsActivity.this.tvFontValue != null) {
                SettingsActivity.this.tvFontValue.setText(target.item.name);
            }
        });
    }

    private void animateSelection(String previousId, FontRow target) {
        final float dp = getResources().getDisplayMetrics().density;
        for (int i = 0; i < this.fontRows.size(); i++) {
            final FontRow r = this.fontRows.get(i);
            if (!r.item.id.equals(previousId)) {
                continue;
            }
            r.check.animate().alpha(0.0f).translationX(14.0f * dp)
                    .setDuration(180L).setInterpolator(EASE_OUT).start();
            r.pill.animate().alpha(0.0f).translationX(14.0f * dp)
                    .setDuration(180L).setInterpolator(EASE_OUT)
                    .withEndAction(() -> {
                        r.pill.setVisibility(View.INVISIBLE);
                        r.pill.setTranslationX(0.0f);
                        r.check.setVisibility(View.INVISIBLE);
                        r.check.setTranslationX(0.0f);
                    }).start();
        }
        target.pill.setVisibility(View.VISIBLE);
        target.pill.setTranslationX(18.0f * dp);
        target.pill.setAlpha(0.0f);
        target.pill.animate().alpha(1.0f).translationX(0.0f)
                .setDuration(260L).setInterpolator(EASE_IN_OUT).start();
        target.check.setVisibility(View.VISIBLE);
        target.check.setTranslationX(18.0f * dp);
        target.check.setAlpha(0.0f);
        target.check.animate().alpha(1.0f).translationX(0.0f)
                .setDuration(260L).setStartDelay(36L).setInterpolator(EASE_IN_OUT).start();
    }

    /**
     * 反馈 2「删除 / 补位」：条目渐隐并向右滑走，**后面所有兄弟（含导入按钮）用 translation 曲线上移**，
     * 动画结束才真正删文件并重建列表 —— 只用 GPU 位移，不做逐帧高度动画，所以不会硬。
     */
    private void deleteFontRow(final FontRow holder) {
        if (this.fontDeleting) {
            return;
        }
        this.fontDeleting = true;
        final float dp = getResources().getDisplayMetrics().density;
        final View row = holder.row;
        final int h = row.getHeight() > 0 ? row.getHeight() : (int) (dp * 44.0f);
        int index = -1;
        LinearLayout host = this.llFonts;
        if (host != null) {
            index = host.indexOfChild(row);
        }
        row.animate().alpha(0.0f).translationX(14.0f * dp)
                .setDuration(190L).setInterpolator(EASE_OUT).start();
        if (host != null && index >= 0) {
            int step = 0;
            for (int i = index + 1; i < host.getChildCount(); i++) {
                View sibling = host.getChildAt(i);
                sibling.animate().translationY(-h)
                        .setDuration(300L)
                        .setStartDelay(step * 12L)
                        .setInterpolator(EASE_IN_OUT).start();
                step++;
            }
            host.postDelayed(() -> finishFontDelete(holder), 330L);
        } else {
            finishFontDelete(holder);
        }
    }

    private void finishFontDelete(final FontRow holder) {
        if (isFinishing() || isDestroyed()) {
            this.fontDeleting = false;
            return;
        }
        Fonts.remove(this, holder.item);
        Fonts.refresh(getWindow().getDecorView());
        this.fontDeleting = false;
        buildFontList();
        Toast.makeText(this, s(R.string.font_deleted, holder.item.name),
                Toast.LENGTH_SHORT).show();
    }


    private void requestFontImport() {
        try {
            android.content.Intent intent = new android.content.Intent(
                    android.content.Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, REQ_FONT_IMPORT);
        } catch (Throwable th) {
            Toast.makeText(this, R.string.font_import_fail, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_FONT_IMPORT || resultCode != RESULT_OK || data == null) {
            return;
        }
        android.net.Uri uri = data.getData();
        if (uri == null) {
            return;
        }
        int result = Fonts.importFont(this, uri);
        if (result == Fonts.OK) {
            // 只把它加进列表（带入场动画），不擅自替换当前字体
            buildFontList(Fonts.lastImportedId);
            Toast.makeText(this, s(R.string.font_import_ok, Fonts.lastImportedName),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        int msg = R.string.font_import_fail;
        if (result == Fonts.ERR_FILE) {
            msg = R.string.font_import_reject_file;
        } else if (result == Fonts.ERR_LANG) {
            msg = R.string.font_import_reject_lang;
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /** 折叠面板下面那行小字：当前是哪个模式（不展开也能看到）。 */
    private void updateModeValue() {
        int mode = ThemePrefs.INSTANCE.getMode(this);
        int res = mode == ThemePrefs.MODE_LIGHT ? R.string.settings_light
                : (mode == ThemePrefs.MODE_DARK ? R.string.settings_dark : R.string.settings_follow_system);
        if (this.tvModeValue != null) {
            this.tvModeValue.setText(s(res));
            this.tvModeValue.setTextColor(ThemeManager.INSTANCE.colors(this).getTextMuted());
        }
    }

    private float density() {
        return getResources().getDisplayMetrics().density;
    }

    private int tileHeight() {
        return (int) (56 * density());
    }

    private void toast(String message) {
        if (message == null || message.length() == 0) {
            return;
        }
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }

    /** 键盘直接输入对比度（点「对比度」右侧的数值）。 */
    private void showContrastInput() {
        final ThemeColors colors = ThemeManager.INSTANCE.colors(this);
        final float d = density();
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(1);
        ThemeManager.INSTANCE.clearDialogWindow(dialog);   // v1.10.71：圆角不露方底
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        int pad = (int) (20 * d);
        // v1.10.396：底部 20→14dp，让出的 6dp 给按钮行的投影留白（见下面 row 的高度）
        root.setPadding(pad, (int) (16 * d), pad, (int) (14 * d));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(colors.getSurface());
        bg.setCornerRadius(22.0f * d);
        root.setBackground(bg);

        TextView title = new TextView(this);
        title.setText(R.string.settings_contrast_label);
        title.setTextSize(17.0f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(colors.getText());
        root.addView(title);

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        input.setText(String.format(java.util.Locale.US, "%.2f",
                ThemePrefs.INSTANCE.getContrastX100(this) / 100f));
        input.setTextColor(colors.getText());
        input.setHintTextColor(colors.getTextMuted());
        input.setHint(R.string.settings_contrast_input_hint);
        input.setSingleLine(true);
        input.setTextSize(16.0f);
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(colors.getNeutral());
        inputBg.setCornerRadius(14.0f * d);
        input.setBackground(inputBg);
        int ip = (int) (14 * d);
        input.setPadding(ip, (int) (10 * d), ip, (int) (10 * d));
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(-1, -2);
        inputLp.topMargin = (int) (14 * d);
        root.addView(input, inputLp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(0);
        // v1.10.70：两个按钮**紧凑居中**（原来各占一半、贴到左右边，离中间太远）
        row.setGravity(android.view.Gravity.CENTER);
        // v1.10.396：这个弹窗是**手写副本**（不是 NumDialog），前几轮修投影全漏了它 ——
        // 行高写死成 48+6×2，按钮在行里居中，投影就落在行自己的界限内；
        // 行距 16→10、卡片底部 20→14，按钮位置和卡片高度一个像素都不变。
        row.setClipChildren(false);
        row.setClipToPadding(false);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, (int) (60 * d));
        rowLp.topMargin = (int) (10 * d);
        root.addView(row, rowLp);

        Button cancel = new Button(this);
        cancel.setText(R.string.cancel);
        cancel.setAllCaps(false);
        cancel.setTextSize(14.0f);
        cancel.setTextColor(colors.getText());
        cancel.setBackground(ThemeManager.INSTANCE.outlined(this, 0, colors.getNeutral(), 999.0f));
        cancel.setOnClickListener((v) -> dialog.dismiss());
        cancel.setMinWidth((int) (116 * d));
        cancel.setPadding((int) (26 * d), 0, (int) (26 * d), 0);
        cancel.setElevation(2.0f * d);   // v1.10.396：Material 的 2dp 投影显式补回来
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, (int) (48 * d));
        row.addView(cancel, clp);

        Button ok = new Button(this);
        ok.setText(R.string.settings_scheme_confirm);
        ok.setAllCaps(false);
        ok.setTextSize(14.0f);
        ok.setTextColor(colors.getOnPrimary());
        ok.setBackground(ThemeManager.INSTANCE.rounded(this, colors.getPrimary(), 999.0f));
        ok.setMinWidth((int) (116 * d));
        ok.setPadding((int) (26 * d), 0, (int) (26 * d), 0);
        ok.setOnClickListener((v) -> {
            float value;
            try {
                value = Float.parseFloat(input.getText().toString().replace(',', '.').trim());
            } catch (Throwable th) {
                toast(s(R.string.settings_contrast_invalid));
                return;
            }
            // v1.10.265：量程收窄到 Material 的 contrastLevel（-1.00 ~ +1.00）
            if (Float.isNaN(value) || value < -1.0f || value > 1.0f) {
                toast(s(R.string.settings_contrast_invalid));
                return;
            }
            int x100 = Math.round(value * 100f);
            // v1.10.90：**修「输入对比度后勾选图标消失」**
            // 老代码在这里 setActiveScheme(-1)，把刚勾上的方案清掉了。
            // 现在对比度是方案的一部分：写回当前方案，勾选保持不变；
            // 而且网格不重建（refreshThemeInPlace(false)），图标对象不换，勾选也不会闪。
            // v1.10.92：只改运行时对比度，不擅自写进已保存的方案
            SettingsActivity.this.writeContrast(x100);   // v1.10.179：键盘输入同样写共享值
            // v1.10.240（需求）：**确认时只要值和当前不同，滑杆就必须滑过去，不能瞬移**。
            // v1.10.91 的老注释担心「一边滑一边改数值」—— 那个担心从 v1.10.203 起已经不成立：
            // animateContrastSliderTo() 只挪进度、只在落地时刷数字，**绝不落盘**
            // （落盘只有上面 writeContrast 这一行，以及用户自己拖动）。
            // 值没变时 animateContrastSliderTo 内部会直接返回，不会白闪一下。
            SettingsActivity.this.animateContrastSliderTo(x100);
            SettingsActivity.this.updateContrastLabel(x100);
            SettingsActivity.this.refreshThemeInPlace(false);
            dialog.dismiss();
        });
        ok.setElevation(2.0f * d);       // v1.10.396：同上
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(-2, (int) (48 * d));
        olp.leftMargin = (int) (12 * d);
        olp.leftMargin = (int) (10 * d);
        row.addView(ok, olp);

        // v1.10.396：窗口以前和卡片一样大（下面 setLayout(width, -2)），卡片投影没地方画 →
        // 浅色下这张白卡片是硬边贴背景。现在包一层 18dp 透明外框、卡片给 10dp elevation，
        // 窗口宽度同步 +36dp，卡片宽度因此和以前完全一致。
        android.widget.FrameLayout shell = new android.widget.FrameLayout(this);
        shell.setClipChildren(false);
        shell.setClipToPadding(false);
        shell.setPadding((int) (18 * d), (int) (18 * d), (int) (18 * d), (int) (18 * d));
        shell.addView(root, new android.widget.FrameLayout.LayoutParams(
                -1, -2, android.view.Gravity.CENTER));
        root.setElevation(10.0f * d);
        root.setClickable(true);
        // 外框 = 原来「窗口外面那一圈」：点它仍然是取消
        shell.setClickable(true);
        shell.setOnClickListener((v) -> dialog.dismiss());
        dialog.setContentView(shell);
        // v1.10.176（反馈「对比度键盘输入页没跟随」）：这是个**独立 Dialog**，不在 Activity
        // 的视图树里 —— 不主动铺一次，标题/输入框永远停在系统字体 + 原始字号：
        //   · 「字体」设置（含「跟随系统」）传不进来；
        //   · 「字体大小」档位完全不吃（其余对话框都有这一行，只有这里漏了）。
        // 必须放在按钮也加完之后：applyTree 只处理当时已经在树上的节点。
        Fonts.applyTree(root);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
            window.setGravity(android.view.Gravity.CENTER);
            int cardW = getResources().getDisplayMetrics().widthPixels - (int) (64 * d);
            // 窗口 = 卡片 + 两侧投影留白（卡片宽度不变）
            window.setLayout(cardW + (int) (36 * d), -2);
            window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        input.requestFocus();
        input.setSelection(input.getText().length());
    }

    // ---------------------------------------------------------------- 配色方案网格

    /** 重新铺网格。@param animate 是否做位移动画  @param popId 哪个图标要「由不可见变大」地出现 */
    private void buildSchemes(boolean animate, long popId) {
        buildSchemes(animate, popId, -1L);
    }

    /**
     * @param animateActiveId 重建后要给哪个方案图标补一次「勾选进场」（删除后跳转到邻居时用，
     *                        这样它的出现动画和手点一下完全一致）
     */
    private void buildSchemes(boolean animate, long popId, long animateActiveId) {
        try {
            buildSchemesInner(animate, popId, animateActiveId);
        } catch (Throwable ignored) {
            // 主题页不能因为方案区的任何异常挂掉
        }
    }

    private void buildSchemesInner(boolean animate, long popId, long animateActiveId) {
        LinearLayout host = this.llSchemes;
        if (host == null) {
            return;
        }
        // v1.10.371：折叠态（行被 host 裁切但仍在布局中）也要能捕获位置，否则重排动画会没有起点
        java.util.HashMap<Long, int[]> before = animate ? captureSchemePositions() : null;
        host.removeAllViews();
        long[] ids = ThemePrefs.INSTANCE.getSchemeIds(this);
        long active = ThemePrefs.INSTANCE.getActiveScheme(this);
        ThemeColors colors = ThemeManager.INSTANCE.colors(this);
        int total = ids.length + 1;
        int rows = (total + 4) / 5;
        int tileH = tileHeight();
        for (int r = 0; r < rows; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(-1, tileH));
            for (int c = 0; c < 5; c++) {
                int idx = r * 5 + c;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1.0f);
                if (idx < ids.length) {
                    boolean sel = active == ids[idx];
                    // 需要「跳转动画」的那一个：先当未选中建出来，layout 完再 animateActive(true)
                    View tileView = makeSchemeTile(ids[idx], idx, sel && ids[idx] != animateActiveId, colors);
                    if (tileView instanceof SchemeTileView) {
                        // v1.10.92：当前方案的配色/对比度被改过 → 勾选标记画成半透明（「未保存」）
                        ((SchemeTileView) tileView).setDirty(sel && isSchemeDirty(ids[idx]));
                    }
                    row.addView(tileView, lp);
                } else if (idx == ids.length) {
                    row.addView(makeAddTile(colors), lp);
                } else {
                    row.addView(new View(this), lp);
                }
            }
            host.addView(row);
        }
        applyFoldState(false);
        final java.util.HashMap<Long, int[]> old = before;
        final long pop = popId;
        final long animActive = animateActiveId;
        final Runnable afterLayout = () -> {
            if (old != null && !old.isEmpty()) {
                animateSchemeMoves(old, pop);
            }
            if (animActive != -1L) {
                View tile = findSchemeTile(animActive);
                if (tile instanceof SchemeTileView) {
                    ((SchemeTileView) tile).animateActive(true);
                }
            }
        };
        if (old != null && !old.isEmpty()) {
            // 必须等这一轮 layout 完成再算位移：新建的 View 此刻 getLocationOnScreen 还是 0
            host.getViewTreeObserver().addOnPreDrawListener(
                    new android.view.ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            if (host.getViewTreeObserver().isAlive()) {
                                host.getViewTreeObserver().removeOnPreDrawListener(this);
                            }
                            afterLayout.run();
                            return true;
                        }
                    });
        } else if (animActive != -1L) {
            host.post(afterLayout);
        }
    }

    private SchemeTileView makeSchemeTile(final long id, int index, boolean selected, ThemeColors colors) {
        SchemeTileView tile = new SchemeTileView(this);
        // 自绘 View 默认不进无障碍树 —— 用户用「手机控制」点按时需要能按名字找到它
        tile.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        tile.setContentDescription(s(R.string.settings_scheme_tile_desc, new Object[]{Integer.valueOf(index + 1)}));
        int[] c = ThemePrefs.INSTANCE.getScheme(this, id);
        if (c != null && c.length == ThemePrefs.SCHEME_FIELDS) {
            tile.setQuadrants(c[0], c[1], c[2], c[5]);
        } else {
            tile.setQuadrants(colors.getPrimary(), colors.getSecondary(),
                    colors.getTertiary(), colors.getSlider());
        }
        tile.setRingColor(colors.getText());
        tile.setActiveVisual(selected);
        tile.setTag(Long.valueOf(id));
        tile.setOnClickListener((v) -> applyScheme(id));
        tile.setOnLongClickListener((v) -> {
            showActionBar(false, id);
            return true;
        });
        return tile;
    }

    /** 占位方案：圆形 = 主色与辅助色的中间色，中间一个「+」。 */
    private SchemeTileView makeAddTile(ThemeColors colors) {
        SchemeTileView tile = new SchemeTileView(this);
        tile.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        tile.setContentDescription(s(R.string.settings_scheme_add_desc));
        tile.setKind(SchemeTileView.KIND_ADD);
        tile.setQuadrants(between(colors.getPrimary(), colors.getSecondary()), 0, 0, 0);
        tile.setTag(Long.valueOf(KEY_ADD));
        tile.setOnClickListener((v) -> showActionBar(true, -1L));
        return tile;
    }

    private static int between(int a, int b) {
        return android.graphics.Color.rgb(
                (android.graphics.Color.red(a) + android.graphics.Color.red(b)) / 2,
                (android.graphics.Color.green(a) + android.graphics.Color.green(b)) / 2,
                (android.graphics.Color.blue(a) + android.graphics.Color.blue(b)) / 2);
    }

    private java.util.HashMap<Long, int[]> captureSchemePositions() {
        java.util.HashMap<Long, int[]> map = new java.util.HashMap<>();
        LinearLayout host = this.llSchemes;
        if (host == null) {
            return map;
        }
        for (int r = 0; r < host.getChildCount(); r++) {
            View row = host.getChildAt(r);
            if (!(row instanceof ViewGroup)) {
                continue;
            }
            ViewGroup group = (ViewGroup) row;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (!(child instanceof SchemeTileView) || !(child.getTag() instanceof Long)) {
                    continue;
                }
                if (child.getVisibility() != View.VISIBLE) {
                    continue;
                }
                int[] xy = new int[2];
                child.getLocationOnScreen(xy);
                if (xy[0] == 0 && xy[1] == 0) {
                    continue;
                }
                map.put((Long) child.getTag(), xy);
            }
        }
        return map;
    }

    private void animateSchemeMoves(java.util.HashMap<Long, int[]> before, long popId) {
        LinearLayout host = this.llSchemes;
        if (host == null) {
            return;
        }
        for (int r = 0; r < host.getChildCount(); r++) {
            View row = host.getChildAt(r);
            if (!(row instanceof ViewGroup)) {
                continue;
            }
            ViewGroup group = (ViewGroup) row;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (!(child instanceof SchemeTileView) || !(child.getTag() instanceof Long)) {
                    continue;
                }
                if (child.getVisibility() != View.VISIBLE) {
                    continue;
                }
                long key = (Long) child.getTag();
                if (key == popId) {
                    popIn(child);
                    continue;
                }
                int[] old = before.get(Long.valueOf(key));
                if (old == null) {
                    continue;
                }
                int[] now = new int[2];
                child.getLocationOnScreen(now);
                float dx = old[0] - now[0];
                float dy = old[1] - now[1];
                if (Math.abs(dx) < 1.0f && Math.abs(dy) < 1.0f) {
                    continue;
                }
                child.setTranslationX(dx);
                child.setTranslationY(dy);
                child.animate().translationX(0.0f).translationY(0.0f)
                        .setDuration(240L)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start();
            }
        }
    }

    /**
     * 补位/新增图标的入场：从肉眼不可见开始，在中央长大到略超原尺寸（1.05）再缩回 1.0，
     * 前段 ease-out、后段减速回弹 —— 就是 AE 里那种「过冲再落定」的手感。
     */
    private void popIn(final View view) {
        view.setAlpha(0.0f);
        view.setScaleX(0.08f);
        view.setScaleY(0.08f);
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.setDuration(430L);
        animator.setInterpolator(new android.view.animation.LinearInterpolator());
        animator.addUpdateListener((a) -> {
            float t = a.getAnimatedFraction();
            float s;
            if (t < 0.62f) {
                s = 0.08f + (1.05f - 0.08f) * easeOutCubic(t / 0.62f);
            } else {
                s = 1.05f - 0.05f * easeOutCubic((t - 0.62f) / 0.38f);
            }
            view.setScaleX(s);
            view.setScaleY(s);
            view.setAlpha(Math.min(1.0f, t / 0.18f));
        });
        animator.start();
    }

    private static float easeOutCubic(float x) {
        float t = 1.0f - Math.max(0.0f, Math.min(1.0f, x));
        return 1.0f - t * t * t;
    }

    /** 0 展开 / 1 半折叠（只留第一行）/ 2 全折叠。行数 > 1 才出现折叠箭头。 */
    /**
     * 折叠状态落地。
     *
     * <p>v1.10.372（用户要求「折叠时图标的缩放改回来」）：撤销 v1.10.371 的**纯裁切**方案 ——
     * 裁切会让圆饼保持原尺寸、「啪」地被切掉，手感不对。现在：
     * 行仍然是完整高度、母容器（ll_schemes）高度负责把布局收起来，
     * **每枚圆饼自己按比例缩小 + 整行淡出** —— 图标是「缩进去」的，而不是被切掉，
     * 而两种折叠（0→1 与 1→2）走的是同一段动画，不会出现「像刷新了一下」。
     */
    private void applyFoldState(boolean animate) {
        LinearLayout host = this.llSchemes;
        if (host == null) {
            return;
        }
        int rows = host.getChildCount();
        boolean showToggle = rows >= 2;
        if (this.btnSchemesToggle != null) {
            this.btnSchemesToggle.setVisibility(showToggle ? View.VISIBLE : View.GONE);
            // v1.10.385（用户要求）：箭头三态各自有方向 ——
            //   展开 = 0°（朝下「v」）、半折叠 = 90°（朝左）、全折叠 = 180°（朝上）。
            // 上一版半折叠和全折叠都是 180°，半折叠看不出「只收了一半」。
            // 旋转跟着折叠动画一起走（230ms、Ease.IN_OUT），不再瞬切。
            float target = this.schemeFold == 0 ? 0.0f : (this.schemeFold == 1 ? 90.0f : 180.0f);
            this.btnSchemesToggle.animate().cancel();
            if (animate) {
                this.btnSchemesToggle.animate().rotation(target).setDuration(230L)
                        .setInterpolator((t) -> Ease.IN_OUT.at(t)).start();
            } else {
                this.btnSchemesToggle.setRotation(target);
            }
        }
        if (!showToggle) {
            this.schemeFold = 0;
        }
        int tileH = tileHeight();
        for (int r = 0; r < rows; r++) {
            View row = host.getChildAt(r);
            ViewGroup.LayoutParams lp = row.getLayoutParams();
            if (lp != null && lp.height != tileH) {
                lp.height = tileH;
                row.setLayoutParams(lp);
            }
            row.setVisibility(View.VISIBLE);
        }
        host.setClipChildren(true);
        host.setClipToPadding(false);
        final int visibleRows = this.schemeFold == 0 ? rows : (this.schemeFold == 1 ? 1 : 0);
        final int target = visibleRows * tileH;
        ViewGroup.LayoutParams hlp = host.getLayoutParams();
        if (hlp == null) {
            return;
        }
        if (!animate) {
            hlp.height = target;
            host.setLayoutParams(hlp);
            for (int r = 0; r < rows; r++) {
                boolean vis = r < visibleRows;
                setRowTilesScale(host.getChildAt(r), vis ? 1.0f : HIDDEN_SCALE);
                host.getChildAt(r).setAlpha(vis ? 1.0f : 0.0f);
            }
            return;
        }
        final int from = host.getHeight();
        if (from == target) {
            // 高度没变也要把图标摆正（例如重建后直接落地）
            for (int r = 0; r < rows; r++) {
                boolean vis = r < visibleRows;
                setRowTilesScale(host.getChildAt(r), vis ? 1.0f : HIDDEN_SCALE);
                host.getChildAt(r).setAlpha(vis ? 1.0f : 0.0f);
            }
            return;
        }
        // 起点状态按「上一次可见了几行」反解：离开的行缩/淡出，进来的行缩/淡入，
        // 全程不动的行保持原样 —— 否则可见的那一行会在收起时突然闪一下。
        final int oldVisible = tileH > 0 ? Math.max(0, Math.min(rows, from / tileH)) : 0;
        final boolean collapsing = target < from;
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.setDuration(230L);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener((a) -> {
            float e = a.getAnimatedFraction();
            ViewGroup.LayoutParams p = host.getLayoutParams();
            if (p != null) {
                p.height = Math.round(from + (target - from) * e);
                host.setLayoutParams(p);
            }
            for (int r = 0; r < rows; r++) {
                boolean vis = r < visibleRows;
                boolean wasVis = r < oldVisible;
                float s;
                float alpha;
                if (collapsing && !vis && wasVis) {
                    s = 1.0f - (1.0f - HIDDEN_SCALE) * e;
                    alpha = 1.0f - e;
                } else if (!collapsing && vis && !wasVis) {
                    s = HIDDEN_SCALE + (1.0f - HIDDEN_SCALE) * e;
                    alpha = e;
                } else {
                    s = vis ? 1.0f : HIDDEN_SCALE;
                    alpha = vis ? 1.0f : 0.0f;
                }
                setRowTilesScale(host.getChildAt(r), s);
                host.getChildAt(r).setAlpha(alpha);
            }
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                ViewGroup.LayoutParams p = host.getLayoutParams();
                if (p != null) {
                    p.height = target;
                    host.setLayoutParams(p);
                }
                for (int r = 0; r < rows; r++) {
                    boolean vis = r < visibleRows;
                    setRowTilesScale(host.getChildAt(r), vis ? 1.0f : HIDDEN_SCALE);
                    host.getChildAt(r).setAlpha(vis ? 1.0f : 0.0f);
                }
            }
        });
        animator.start();
    }

    /** 折叠时的图标缩小比例（不是被裁切，是真的缩进去）。 */
    private static final float HIDDEN_SCALE = 0.14f;

    private static void setRowTilesScale(View row, float s) {
        if (!(row instanceof ViewGroup)) {
            return;
        }
        ViewGroup g = (ViewGroup) row;
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            if (c instanceof SchemeTileView) {
                c.setScaleX(s);
                c.setScaleY(s);
            }
        }
    }

    private void cycleFold() {
        LinearLayout host = this.llSchemes;
        int rows = host != null ? host.getChildCount() : 0;
        if (rows < 2) {
            this.schemeFold = 0;
            return;
        }
        this.schemeFold = (this.schemeFold + 1) % 3;
        applyFoldState(true);
    }

    // ---------------------------------------------------------------- 底部动作条

    private void styleActionBar(ThemeColors colors) {
        if (this.schemeBar != null) {
            this.schemeBar.setBackground(ThemeManager.INSTANCE.rounded(this, colors.getSurface(), 22.0f));
        }
        if (this.barDelete != null) {
            // v1.10.203：与 ThemeManager 里 tag="danger" 的按钮同源（错误色 / onError）
            this.barDelete.setBackground(ThemeManager.INSTANCE.rounded(this, colors.getError(), 999.0f));
            this.barDelete.setTextColor(colors.getOnError());
        }
        if (this.barCancel != null) {
            this.barCancel.setBackground(ThemeManager.INSTANCE.rounded(this, colors.getNeutral(), 999.0f));
            this.barCancel.setTextColor(colors.getText());
        }
        if (this.barConfirm != null) {
            this.barConfirm.setBackground(ThemeManager.INSTANCE.rounded(this, colors.getPrimary(), 999.0f));
            this.barConfirm.setTextColor(colors.getOnPrimary());
        }
        if (this.barMovePrev != null) {
            this.barMovePrev.setBackground(ThemeManager.INSTANCE.rounded(this, colors.getNeutral(), 999.0f));
            this.barMovePrev.setTextColor(colors.getText());
        }
        if (this.barMoveNext != null) {
            this.barMoveNext.setBackground(ThemeManager.INSTANCE.rounded(this, colors.getNeutral(), 999.0f));
            this.barMoveNext.setTextColor(colors.getText());
        }
        if (this.barIndicator != null) {
            // 灰线 = 正文色 28%（深浅色都看得见，又不抢按钮的视线）
            this.barIndicator.setBackground(ThemeManager.INSTANCE.rounded(this,
                    ThemeManager.INSTANCE.withAlpha(colors.getText(), 0.28f), 999.0f));
        }
        if (this.barScroll != null) {
            // v1.10.376（用户反馈「内容 4 角是 90° 蒙版」）：滚动视口改成**圆角裁切**。
            // 半径取 24dp = 按钮高度的一半 —— 静止时它正好与首尾胶囊的圆头重合（看不出裁切），
            // 一旦滑到一半、按钮被视口边缘切开，那条切口就是圆的，不再是直角。
            this.barScroll.setBackground(ThemeManager.INSTANCE.rounded(this, 0x00000000, 24.0f));
            this.barScroll.setClipToOutline(true);
        }
    }

    /**
     * v1.10.372（需求）：动作条两页按**滚动视口宽度**定宽。
     *
     * <p>第 2 页没有任何可用按钮时整页隐藏 —— 这样第 1 页铺满、不出现「空白页」；
     * 有可用按钮时两页各占一屏，滚动范围正好一屏，页与页之间是无缝的连续滑动。
     */
    private void layoutBarPages() {
        if (this.barScroll == null || this.barPage1 == null || this.barPage2 == null) {
            return;
        }
        if (this.barReflowBusy) {
            // 补位动画正在自己驱动页宽 —— 这里再写一次会把动画的插值顶掉（每帧来回摆 = 跳闪）
            return;
        }
        int vw = this.barScroll.getWidth();
        if (vw <= 0) {
            return;
        }
        ViewGroup.LayoutParams l1 = this.barPage1.getLayoutParams();
        if (l1 != null && l1.width != vw) {
            l1.width = vw;
            this.barPage1.setLayoutParams(l1);
        }
        // v1.10.375（用户要求）：第 2 页只有 1 个按钮时，**整页缩成半页**（不是「整页 + 空槽位」）。
        // 两枚按钮按权重铺满自己的页：2 枚 → 各占半页；1 枚 → 页宽减半、那一枚正好半页宽。
        int moveCount = 0;
        if (this.barMovePrev != null && this.barMovePrev.getVisibility() == View.VISIBLE) {
            moveCount++;
        }
        if (this.barMoveNext != null && this.barMoveNext.getVisibility() == View.VISIBLE) {
            moveCount++;
        }
        int want2 = moveCount <= 0 ? 0 : (moveCount == 2 ? vw : Math.round(vw * 0.5f));
        if (want2 > vw) {
            want2 = vw;   // 兜底：页宽永远不超过视口（否则按钮会被撑长）
        }
        ViewGroup.LayoutParams l2 = this.barPage2.getLayoutParams();
        if (l2 != null && l2.width != want2) {
            l2.width = want2;
            this.barPage2.setLayoutParams(l2);
        }
        // v1.10.376（用户反馈「缩成半页后按钮离边的距离和整页不一致」）：
        // 按钮自身的 marginEnd 只在「后面还有按钮」时才需要，最后那一枚必须归 0，
        // 由卡片自己的 12dp 内边距决定与边缘的距离 —— 整页/半页/单页三种情况都是 12dp。
        int gap = Math.round(8 * density());
        boolean hasPrev = this.barMovePrev != null && this.barMovePrev.getVisibility() == View.VISIBLE;
        boolean page2On = moveCount > 0;
        setMarginEnd(this.barMovePrev, hasPrev && moveCount == 2 ? gap : 0);
        setMarginEnd(this.barMoveNext, 0);
        setMarginEnd(this.barCancel, page2On ? gap : 0);
        updateBarIndicator();
    }

    private void setMarginEnd(View v, int px) {
        if (v == null || !(v.getLayoutParams() instanceof LinearLayout.LayoutParams)) {
            return;
        }
        LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) v.getLayoutParams();
        if (p.rightMargin != px) {
            p.rightMargin = px;
            v.setLayoutParams(p);
        }
    }

    /** v1.10.376：胶囊行平滑回到第 1 页（长按切到别的方案时，不再「啪」地跳回最左）。 */
    private void scrollBarToStart() {
        if (this.barScroll == null) {
            return;
        }
        final int from = this.barScroll.getScrollX();
        if (from == 0) {
            return;
        }
        android.animation.ValueAnimator a = android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
        a.setDuration(260L);
        a.setInterpolator((t) -> Ease.IN_OUT.at(t));
        a.addUpdateListener((an) -> {
            float e = (Float) an.getAnimatedValue();
            this.barScroll.scrollTo(Math.round(from * (1.0f - e)), 0);
        });
        a.start();
    }

    /**
     * v1.10.377（用户反馈「补位不是原地补位、也没有动画」）：**收/放同时进行**的补位动画。
     *
     * <p>上一版的做法是「渐隐 → 直接 GONE → 再动画页宽」，中间那一帧剩下那枚会**瞬间变成整页宽**，
     * 再被页宽动画缩回去 —— 看起来就是「跳一下」。
     *
     * <p>现在：消失的那枚在渐隐之后**保留占位**，和页宽一起按同一条曲线收到 0 ——
     * 另一枚的宽度始终不变（原地不动），只是随着空位收拢滑到位；到位后才真正移除。
     * 出现时完全镜像：页宽和那一枚一起长出来，另一枚原地不动，最后再渐显。
     */
    private void animateBarAfterMove(boolean oldPrev, boolean oldNext, boolean newPrev, boolean newNext) {
        if (this.barScroll == null || this.barPage2 == null) {
            return;
        }
        // 收刀：上一段补位动画、显式宽度、页宽全部还原成「权重铺满」的干净状态
        if (this.barReflowAnim != null) {
            try {
                this.barReflowAnim.cancel();
            } catch (Throwable ignored) {
            }
            this.barReflowAnim = null;
        }
        this.barReflowBusy = false;
        this.indLengthLocked = false;
        resetBarWeights();
        layoutBarPages();

        final int vw = this.barScroll.getWidth();
        if (vw <= 0) {
            return;
        }
        final boolean hidePrev = oldPrev && !newPrev;
        final boolean hideNext = oldNext && !newNext;
        final boolean showPrev = !oldPrev && newPrev;
        final boolean showNext = !oldNext && newNext;
        if (!hidePrev && !hideNext && !showPrev && !showNext) {
            refreshMoveButtons(this.pendingDeleteId);
            layoutBarPages();
            return;
        }
        final int count = (newPrev ? 1 : 0) + (newNext ? 1 : 0);
        final int targetW = count >= 2 ? vw : Math.round(vw * 0.5f);
        final int pageNow = this.barPage2.getWidth();
        // 补位期间：锁长度目标 + 禁止 layoutBarPages() 抢写页宽
        this.indLengthLocked = true;
        this.barReflowBusy = true;

        final View leaving = hidePrev ? this.barMovePrev : (hideNext ? this.barMoveNext : null);
        final View appearing = showPrev ? this.barMovePrev : (showNext ? this.barMoveNext : null);

        Runnable reflow = () -> {
            if (appearing != null) {
                setExplicitWidth(appearing, 0);
                appearing.setVisibility(View.VISIBLE);
                appearing.setAlpha(0.0f);
            }
            // v1.10.383：四个量（页宽 / 前移宽 / 后移宽 / 前移 marginEnd）一起推到**最终布局的精确值**，
            // 收尾那一帧不会再「补一下」（上一版就是差这 8dp 间距 + 4dp 宽度）。
            final int gap = Math.round(8 * density());
            final int mPrev1 = count == 2 ? gap : 0;
            final int slot = count <= 0 ? 0 : (targetW - mPrev1) / count;
            final int prev1 = newPrev ? slot : 0;
            final int next1 = newNext ? slot : 0;
            final int prev0 = this.barMovePrev == null ? 0 : this.barMovePrev.getWidth();
            final int next0 = this.barMoveNext == null ? 0 : this.barMoveNext.getWidth();
            final int mPrev0 = marginEndOf(this.barMovePrev);
            animateReflow(pageNow, targetW, prev0, prev1, next0, next1, mPrev0, mPrev1, 240L, () -> {
                if (leaving != null) {
                    leaving.setVisibility(View.GONE);
                    leaving.setAlpha(1.0f);
                }
                resetBarWeights();
                this.barReflowBusy = false;
                layoutBarPages();
                // 解锁后再算目标，且等下一帧（这一帧 content 宽度还是旧值）
                this.indLengthLocked = false;
                if (this.barScroll != null) {
                    this.barScroll.post(this::updateBarIndicator);
                } else {
                    updateBarIndicator();
                }
                if (appearing != null) {
                    fadeView(appearing, 1.0f, 160L, null);
                }
            });
        };
        if (leaving != null) {
            fadeView(leaving, 0.0f, 160L, reflow);
        } else {
            reflow.run();
        }
    }

    /** 把某个按钮钉成固定宽度（同时关掉权重），动画期间由我们自己逐帧改。 */
    private void setExplicitWidth(View v, int w) {
        if (v == null || !(v.getLayoutParams() instanceof LinearLayout.LayoutParams)) {
            return;
        }
        LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) v.getLayoutParams();
        if (p.width == w && p.weight == 0.0f) {
            return;
        }
        p.width = w;
        p.weight = 0.0f;
        v.setLayoutParams(p);
    }

    private void setPage2Width(int w) {
        if (this.barPage2 == null) {
            return;
        }
        ViewGroup.LayoutParams lp = this.barPage2.getLayoutParams();
        if (lp != null && lp.width != w) {
            lp.width = w;
            this.barPage2.setLayoutParams(lp);
        }
    }

    /** 动画结束后把两枚按钮还给「权重铺满」的正常状态。 */
    private void resetBarWeights() {
        if (this.barMovePrev != null && this.barMovePrev.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) this.barMovePrev.getLayoutParams();
            p.width = 0;
            p.weight = 1.0f;
            this.barMovePrev.setLayoutParams(p);
        }
        if (this.barMoveNext != null && this.barMoveNext.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) this.barMoveNext.getLayoutParams();
            p.width = 0;
            p.weight = 1.0f;
            this.barMoveNext.setLayoutParams(p);
        }
    }

    /**
     * v1.10.383：**四个量同一条 Ease 曲线**一起推到最终布局的精确值
     * （页宽 / 前移宽度 / 后移宽度 / 前移的 marginEnd），所以动画终点 = 静止布局，收尾零跳变。
     */
    private void animateReflow(final int page0, final int page1,
                               final int prev0, final int prev1,
                               final int next0, final int next1,
                               final int mPrev0, final int mPrev1,
                               long dur, final Runnable end) {
        setExplicitWidth(this.barMovePrev, prev0);
        setExplicitWidth(this.barMoveNext, next0);
        setMarginEnd(this.barMovePrev, mPrev0);
        android.animation.ValueAnimator a = android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
        a.setDuration(dur);
        a.setInterpolator((t) -> Ease.IN_OUT.at(t));
        a.addUpdateListener((an) -> {
            float e = (Float) an.getAnimatedValue();
            setPage2Width(Math.round(page0 + (page1 - page0) * e));
            setExplicitWidth(this.barMovePrev, Math.round(prev0 + (prev1 - prev0) * e));
            setExplicitWidth(this.barMoveNext, Math.round(next0 + (next1 - next0) * e));
            setMarginEnd(this.barMovePrev, Math.round(mPrev0 + (mPrev1 - mPrev0) * e));
            updateBarIndicator();
        });
        a.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (SettingsActivity.this.barReflowAnim == animation) {
                    SettingsActivity.this.barReflowAnim = null;
                }
                setPage2Width(page1);
                setExplicitWidth(SettingsActivity.this.barMovePrev, prev1);
                setExplicitWidth(SettingsActivity.this.barMoveNext, next1);
                setMarginEnd(SettingsActivity.this.barMovePrev, mPrev1);
                if (end != null) {
                    end.run();
                }
            }
        });
        this.barReflowAnim = a;
        a.start();
    }

    private static int marginEndOf(View v) {
        if (v == null || !(v.getLayoutParams() instanceof LinearLayout.LayoutParams)) {
            return 0;
        }
        return ((LinearLayout.LayoutParams) v.getLayoutParams()).rightMargin;
    }

    private void fadeView(final View v, final float to, long dur, final Runnable end) {
        if (v == null) {
            if (end != null) {
                end.run();
            }
            return;
        }
        android.animation.ValueAnimator a = android.animation.ValueAnimator.ofFloat(v.getAlpha(), to);
        a.setDuration(dur);
        a.setInterpolator((t) -> Ease.OUT.at(t));
        final boolean[] done = new boolean[1];
        a.addUpdateListener((an) -> v.setAlpha((Float) an.getAnimatedValue()));
        a.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (done[0]) {
                    return;
                }
                done[0] = true;
                v.setAlpha(to);
                if (end != null) {
                    end.run();
                }
            }
        });
        a.start();
    }

    /** 第 2 页宽度动画（按钮按权重跟着补位/退回），每帧同步灰线。 */
    /**
     * v1.10.371（需求）：胶囊按钮区下方那条**滑动指示线**。
     *
     * <p>长度 = 轨道宽度 × 可视比例（内容越宽线越短；完全放得下时线 = 满宽，仍然显示），
     * 位置 = （轨道宽 − 线宽）× 滚动比例 —— 滑到最右线也到最右，滑回最左线回最左。
     */
    private void updateBarIndicator() {
        if (this.barScroll == null || this.barIndicator == null) {
            return;
        }
        View content = this.barScroll.getChildCount() > 0 ? this.barScroll.getChildAt(0) : null;
        View track = this.barIndicator.getParent() instanceof View ? (View) this.barIndicator.getParent() : null;
        if (content == null || track == null) {
            return;
        }
        int vw = this.barScroll.getWidth();
        int cw = content.getWidth();
        int trackW = track.getWidth();
        if (vw <= 0 || cw <= 0 || trackW <= 0) {
            return;
        }
        // 目标长度：可视比例 × 轨道宽（内容越宽线越短）；下限 18dp
        float visible = Math.min(1.0f, vw / (float) cw);
        int target = Math.max(Math.round(trackW * visible), Math.round(18 * density()));
        if (this.indWidth < 0.0f) {
            // 首次（动作条刚打开）：直接就位，不从 0 长出来
            this.indWidth = target;
            this.indTarget = target;
        } else if (!this.indLengthLocked && this.indTarget != target) {
            // v1.10.378（用户要求：0.5s / AE 曲线）：长度不再瞬切，从**当前值**连续过渡到目标值
            this.indTarget = target;
            animateIndicatorWidth(target);
        }
        applyIndicatorGeometry(cw, vw, trackW);
    }

    /**
     * 只按当前 indWidth + 滚动比例摆放（位置即时跟随，长度由动画决定）。
     *
     * <p>v1.10.380：长度改用 **scaleX**（pivot 在左端）而不是逐帧改 LayoutParams ——
     * 改宽度会每帧触发一次父容器 layout，50 帧就是 50 次全宽重排，
     * 在这种「父容器里还有 ScrollView」的页面上很容易闪。缩放是纯绘制变换，不动布局。
     */
    private void applyIndicatorGeometry(int cw, int vw, int trackW) {
        int lineW = Math.max(1, Math.round(this.indWidth));
        float sx = Math.max(0.01f, Math.min(1.0f, lineW / (float) trackW));
        this.barIndicator.setPivotX(0.0f);
        this.barIndicator.setPivotY(this.barIndicator.getHeight() * 0.5f);
        if (Math.abs(this.barIndicator.getScaleX() - sx) > 0.0005f) {
            this.barIndicator.setScaleX(sx);
        }
        int range = Math.max(1, cw - vw);
        float frac = Math.max(0.0f, Math.min(1.0f, this.barScroll.getScrollX() / (float) range));
        float tx = (trackW - lineW) * frac;
        // v1.10.380 诊断（A13）：只在「单次调用里宽度或位移跳了 >7dp」时打一条，
        // 平时完全静默 —— 用来定位用户看到的「跳闪」到底是哪一项在跳。
        int jumpW = this.indLastW < 0 ? 0 : Math.abs(lineW - this.indLastW);
        int jumpX = Float.isNaN(this.indLastX) ? 0 : Math.round(Math.abs(tx - this.indLastX));
        if (jumpW > Math.round(7 * density()) || jumpX > Math.round(7 * density())) {
            Diag.warn(this, "A13", "指示线跳变 宽=" + jumpW + " 位=" + jumpX
                    + " 线宽=" + lineW + " 位移=" + Math.round(tx)
                    + " 锁=" + this.indLengthLocked
                    + " 内容=" + cw + " 视口=" + vw + " 轨=" + trackW
                    + " 滚动=" + this.barScroll.getScrollX() + " 比例="
                    + String.format(java.util.Locale.US, "%.3f", frac)
                    + " 目标=" + this.indTarget);
        }
        this.indLastW = lineW;
        this.indLastX = tx;
        this.barIndicator.setTranslationX(tx);
    }

    /** 指示线长度过渡：0.5s，Ease.IN_OUT（AE Easy Ease）。中途来新目标就从当前值续接。 */
    private void animateIndicatorWidth(final int target) {
        if (this.indAnim != null) {
            try {
                this.indAnim.cancel();
            } catch (Throwable ignored) {
            }
            this.indAnim = null;
        }
        final float from = this.indWidth;
        if (Math.abs(target - from) < 0.5f) {
            this.indWidth = target;
            return;
        }
        android.animation.ValueAnimator a = android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
        a.setDuration(500L);
        a.setInterpolator((t) -> Ease.IN_OUT.at(t));
        a.addUpdateListener((an) -> {
            float e = (Float) an.getAnimatedValue();
            SettingsActivity.this.indWidth = from + (target - from) * e;
            updateBarIndicator();
        });
        a.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (SettingsActivity.this.indAnim == animation) {
                    SettingsActivity.this.indAnim = null;
                    SettingsActivity.this.indWidth = target;
                    updateBarIndicator();
                }
            }
        });
        this.indAnim = a;
        a.start();
    }

    /** v1.10.371：把长按选中的方案前移/后移一位，并让两枚图标沿旧→新位置飞过去。 */
    private void moveSelected(int delta) {
        final long id = this.pendingDeleteId;
        if (id < 0) {
            return;
        }
        long[] before = ThemePrefs.INSTANCE.getSchemeIds(this);
        int oldIdx = indexOfId(before, id);
        if (oldIdx < 0) {
            return;
        }
        final boolean oldPrev = oldIdx > 0;
        final boolean oldNext = oldIdx < before.length - 1;
        if (!ThemePrefs.INSTANCE.moveScheme(this, id, delta)) {
            return;
        }
        long[] after = ThemePrefs.INSTANCE.getSchemeIds(this);
        int newIdx = indexOfId(after, id);
        final boolean newPrev = newIdx > 0;
        final boolean newNext = newIdx >= 0 && newIdx < after.length - 1;

        // 图标互换：buildSchemes(true) 会先 captureSchemePositions()、重建后由 animateSchemeMoves()
        // 把每枚图标从**原位置**平移过去（跨排也走同一条 dy 路径），不是原地替换。
        buildSchemes(true, -1L);
        // 动作条第 2 页：渐隐 / 补位 / 渐显（AE 曲线）
        animateBarAfterMove(oldPrev, oldNext, newPrev, newNext);
    }

    /** 前移/后移按钮的可用性：到边界就藏起来（不显示一个点了没反应的按钮）。 */
    private void refreshMoveButtons(long id) {
        long[] ids = ThemePrefs.INSTANCE.getSchemeIds(this);
        int idx = indexOfId(ids, id);
        boolean canMove = idx >= 0 && ids.length > 1;
        // v1.10.374（用户反馈「多留了一个空按钮位置」）：不适用就 **GONE，不占槽位**。
        // 「只占 1/2 宽」改由 layoutBarPages() 给按钮**定宽**实现（页宽的一半），
        // 这样只剩一个按钮时它是**居中**的 1/2 宽，而不是「左边一个按钮 + 右边一个空位」。
        if (this.barMovePrev != null) {
            this.barMovePrev.setVisibility(canMove && idx > 0 ? View.VISIBLE : View.GONE);
            this.barMovePrev.setAlpha(1.0f);
        }
        if (this.barMoveNext != null) {
            this.barMoveNext.setVisibility(canMove && idx < ids.length - 1 ? View.VISIBLE : View.GONE);
            this.barMoveNext.setAlpha(1.0f);
        }
    }

    private static int indexOfId(long[] ids, long id) {
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == id) {
                return i;
            }
        }
        return -1;
    }

    /**
     * v1.10.522：**底部让位的过渡动画**（设置页「补位」不再硬跳）。
     *
     * <h3>为什么会跳</h3>
     * 配色方案的动作条浮在底部，内容区末尾用一枚 104dp 的 {@code spacer_bottom} 给它让位。
     * 长按方案 → 弹出面板（动作条出现，spacer 变 VISIBLE）→ 把页面滑到最底 → 关掉面板
     * （动作条淡出，spacer 变 GONE）：**内容高度一下少 104dp**，ScrollView 在下一趟布局
     * 直接把 scrollY 夹到新的最大值 —— 用户看到的就是「滚动位置突然补位，没有过渡」。
     *
     * <h3>怎么修</h3>
     * 把 {@code setVisibility} 换成**高度插值**：spacer 在 240ms 内从 104dp 收到 0（或反过来），
     * ScrollView 每一帧只被夹一点点，滚动位置就跟着平滑地补回来。
     * 选高度动画而不是「滚 scrollY」的原因：
     * <ul>
     *   <li>不假设用户此刻在最底部 —— 内容高度变了但页没到底时，本来就不该动滚动位置；</li>
     *   <li>不跟手指抢事件：动画改的是布局参数，用户随时可以继续滚；</li>
     *   <li>打断时只需要「改目标 + 缩时长」，从当前高度接着走，不会跳帧
     *       （见 {@code root_settings} 上那个 ACTION_DOWN 监听）。</li>
     * </ul>
     *
     * @param shown true = 让位（动作条出现）；false = 收起
     * @param overrideDuration &gt; 0 时用这个时长（触摸打断时的快速收尾），否则按剩余距离算
     */
    private void animateBottomSpacer(boolean shown, long overrideDuration) {
        View spacer = this.spacerBottom;
        if (spacer == null) {
            return;
        }
        final int full = (int) (SPACER_DP * density());
        int from;
        if (spacer.getVisibility() == View.VISIBLE) {
            ViewGroup.LayoutParams lp0 = spacer.getLayoutParams();
            from = lp0 != null && lp0.height > 0 ? lp0.height : full;
        } else {
            from = 0;
        }
        final int target = shown ? full : 0;
        this.spacerTargetPx = target;
        // 先清字段再 cancel：cancel() 会同步回调 onAnimationEnd，
        // 字段已经换人，旧动画的 end 会被下面的判重挡掉（不会把 spacer 提前 GONE）。
        android.animation.ValueAnimator running = this.spacerAnim;
        if (running != null) {
            this.spacerAnim = null;
            running.cancel();
        }
        if (from == target) {
            setSpacerHeight(target, target > 0);
            return;
        }
        spacer.setVisibility(View.VISIBLE);   // 动画期间必须参与布局，否则改高度不生效
        long duration = overrideDuration > 0L
                ? overrideDuration
                : Math.max(90L, Math.round(240.0 * Math.abs(target - from) / full));
        final android.animation.ValueAnimator anim =
                android.animation.ValueAnimator.ofInt(from, target);
        anim.setDuration(duration);
        anim.setInterpolator(shown
                ? new android.view.animation.DecelerateInterpolator()
                : new android.view.animation.AccelerateDecelerateInterpolator());
        anim.addUpdateListener(a -> setSpacerHeight(((Integer) a.getAnimatedValue()).intValue(), true));
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator a) {
                if (SettingsActivity.this.spacerAnim != a) {
                    return;   // 已被新动画接管
                }
                SettingsActivity.this.spacerAnim = null;
                setSpacerHeight(target, target > 0);
            }
        });
        this.spacerAnim = anim;
        anim.start();
    }

    /** 把让位块的高度写成 px（visible=false 且高度为 0 时顺手 GONE，省掉一趟无效布局）。 */
    private void setSpacerHeight(int px, boolean visible) {
        View spacer = this.spacerBottom;
        if (spacer == null) {
            return;
        }
        ViewGroup.LayoutParams lp = spacer.getLayoutParams();
        if (lp != null && lp.height != px) {
            lp.height = px;
            spacer.setLayoutParams(lp);
        }
        int want = visible ? View.VISIBLE : View.GONE;
        if (spacer.getVisibility() != want) {
            spacer.setVisibility(want);
        }
    }

    /** @param save true = 「取消 / 确认」（保存当前配色）；false = 「删除 / 取消」 */
    private void showActionBar(boolean save, long id) {
        if (this.schemeBar == null) {
            return;
        }
        // v1.10.92：长按的正好是「当前选中且已有改动」的方案 → 多给一个「更新」按钮，
        // 把当前配色 + 对比度覆盖保存回这个方案（这就是「已保存方案可编辑」的入口）
        boolean canUpdate = !save && id >= 0
                && id == ThemePrefs.INSTANCE.getActiveScheme(this)
                && isSchemeDirty(id);
        this.pendingDeleteId = save ? -1L : id;
        this.pendingUpdateId = canUpdate ? id : -1L;
        // v1.10.203（反馈「复制当前配置后长按那个位置冒出『更新』」）：
        // 「更新」只该在「长按的正是当前选中、且真的有未保存改动」时出现。
        // 这一行把判定依据打出来，方便一眼确认是「数据真改了」还是判定错。
        String dirtyWhy = (!save && id >= 0) ? schemeDirtyReason(id) : null;
        Diag.warn(this, "A11", "动作条 方案=" + (save ? "新增" : String.valueOf(id))
                + " 已修改=" + (dirtyWhy != null)
                + " 可更新=" + canUpdate
                + " 依据=" + (dirtyWhy == null ? "一致" : dirtyWhy));
        if (this.barDelete != null) {
            this.barDelete.setVisibility(save ? View.GONE : View.VISIBLE);
        }
        if (this.barConfirm != null) {
            this.barConfirm.setVisibility(save || canUpdate ? View.VISIBLE : View.GONE);
            this.barConfirm.setText(canUpdate
                    ? R.string.settings_scheme_update
                    : R.string.settings_scheme_confirm);
        }
        if (this.barCancel != null) {
            this.barCancel.setVisibility(View.VISIBLE);
            this.barCancel.setText(R.string.cancel);
        }
        // v1.10.371：前移/后移只在「长按了某个方案」时出现；保存流程（save）没有前后可换
        if (save) {
            if (this.barMovePrev != null) {
                this.barMovePrev.setVisibility(View.GONE);
            }
            if (this.barMoveNext != null) {
                this.barMoveNext.setVisibility(View.GONE);
            }
        } else {
            refreshMoveButtons(id);
        }
        // 第 2 页（前移/后移）没有可用按钮时整页隐藏，第 1 页铺满；有则两页各一屏
        boolean hasMove = this.barMovePrev != null && this.barMovePrev.getVisibility() == View.VISIBLE;
        boolean hasMove2 = this.barMoveNext != null && this.barMoveNext.getVisibility() == View.VISIBLE;
        if (this.barPage2 != null) {
            this.barPage2.setVisibility(hasMove || hasMove2 ? View.VISIBLE : View.GONE);
        }
        if (this.barScroll != null) {
            // 每次打开都从「权重铺满」的干净状态开始（上一次动画的显式宽度不留给下一次）
            resetBarWeights();
            // 每次打开按当前内容重新就位（不做「从旧宽度长出来」的过渡）
            this.indWidth = -1.0f;
            this.indTarget = -1;
            // v1.10.376（需求三）：长按切到另一个方案时，从当前滑动进度**平滑**回第 1 页
            scrollBarToStart();
            this.barScroll.post(() -> {
                layoutBarPages();
                updateBarIndicator();
            });
        }
        styleActionBar(ThemeManager.INSTANCE.colors(this));
        // v1.10.522：底部让位改成**高度过渡**（见 animateBottomSpacer 的注释）
        animateBottomSpacer(true, -1L);
        // 先取消上一次的显示/隐藏动画，避免「正在淡出时又点开」被 withEndAction 强行 GONE
        this.schemeBar.animate().cancel();
        if (this.schemeBar.getVisibility() != View.VISIBLE) {
            this.schemeBar.setVisibility(View.VISIBLE);
            this.schemeBar.setAlpha(0.0f);
            this.schemeBar.setTranslationY(26 * density());
            this.schemeBar.animate().alpha(1.0f).translationY(0.0f).setDuration(200L)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        } else {
            this.schemeBar.setAlpha(1.0f);
            this.schemeBar.setTranslationY(0.0f);
        }
    }

    private void hideActionBar() {
        this.pendingDeleteId = -1L;
        this.pendingUpdateId = -1L;
        if (this.schemeBar == null || this.schemeBar.getVisibility() != View.VISIBLE) {
            animateBottomSpacer(false, -1L);
            return;
        }
        this.schemeBar.animate().cancel();
        this.schemeBar.animate().alpha(0.0f).translationY(26 * density()).setDuration(150L)
                .withEndAction(() -> {
                    this.schemeBar.setVisibility(View.GONE);
                    // v1.10.522：不再瞬间 GONE —— 见 animateBottomSpacer
                    animateBottomSpacer(false, -1L);
                }).start();
    }

    // ---------------------------------------------------------------- 方案存取

    /** 把某个方案的颜色写进语义色（不动界面）。返回是否成功。 */
    private boolean writeSchemeColors(long id) {
        int[] c = ThemePrefs.INSTANCE.getScheme(this, id);
        if (c == null || c.length != ThemePrefs.SCHEME_FIELDS) {
            return false;
        }
        ThemePrefs.INSTANCE.setCustom(this, "primary", c[0]);
        ThemePrefs.INSTANCE.setCustom(this, "secondary", c[1]);
        ThemePrefs.INSTANCE.setCustom(this, "tertiary", c[2]);
        ThemePrefs.INSTANCE.setCustom(this, "slider", c[5]);
        // v1.10.203：错误色也属于「一个方案的完整配色」。
        // 老方案（6 字段）没有这一项 → 写 0（= 未设置 → 用默认错误色），不能留着上一个方案的值。
        ThemePrefs.INSTANCE.setCustom(this, "error", c[6]);
        // v1.10.202（反馈「语义色没保存，却被套用到别的自定义方案上」）：
        // 底色（表面 / 中性变体）以前是**旁路键**——「按模式各一份」的 surface_d / surface_l 只在
        // 用户拖颜色时写，切方案时没人写、也没人清。于是：
        //   在方案 A 里把表面色拖了一下（没保存）→ 切到方案 B → B 的 surface 被写回它的存档值，
        //   但 surface_d 还是 A 那次拖出来的值，而 liveBase 优先看 surface_d →
        //   **B 显示的是 A 的那次改动**，看起来就是"没保存的颜色被套用到别的方案上"。
        // 现在：方案自己存这 4 个值（scheme_bases_<id>），切方案时一次性投影到生效通道。
        int[] bases = ThemePrefs.INSTANCE.getSchemeBases(this, id);
        if (bases == null) {
            // 老方案（v1.10.202 之前保存的）：存档里只有一份底色 → 按明暗方向拆成两份，
            // 结果与 201 及以前"看到的样子"一致；等用户保存/更新一次就换成真实的两份。
            bases = legacyBases(c[3], c[4]);
        }
        ThemePrefs.INSTANCE.applyBaseSnapshot(this, c[3], c[4],
                bases[0], bases[1], bases[2], bases[3]);
        ThemePrefs.INSTANCE.setActiveScheme(this, id);
        // v1.10.90：对比度也是方案的一部分 —— 切方案时一起恢复。
        int schemeContrast = ThemePrefs.INSTANCE.getSchemeContrast(this, id);
        ThemePrefs.INSTANCE.setContrastX100(this, schemeContrast);
        // v1.10.92：老方案（v1.10.89 之前保存的）没有自己的对比度记录 → 这里顺手补上，
        // 否则「改对比度」永远判不出「已修改」，用户就没法把新对比度存进这个老方案。
        ThemePrefs.INSTANCE.setSchemeContrast(this, id, schemeContrast);
        Diag.warn(this, "A11", "方案 " + id + " 生效：surface="
                + hex(c[3]) + "/" + hex(bases[0]) + "/" + hex(bases[2])
                + " neutral=" + hex(c[4]) + "/" + hex(bases[1]) + "/" + hex(bases[3])
                + "（flat/深/浅）");
        return true;
    }

    /** 老方案只存了一份底色 → 拆成「深色用哪个 / 浅色用哪个」（方向不对的那一侧用默认值）。 */
    @NotNull
    private int[] legacyBases(int surface, int neutral) {
        int[] out = new int[ThemePrefs.SCHEME_BASES];
        out[0] = ThemeManager.INSTANCE.baseForMode("surface", surface, true);
        out[1] = ThemeManager.INSTANCE.baseForMode("neutral", neutral, true);
        out[2] = ThemeManager.INSTANCE.baseForMode("surface", surface, false);
        out[3] = ThemeManager.INSTANCE.baseForMode("neutral", neutral, false);
        return out;
    }

    /** 当前生效的「按模式各一份」底色（存方案时用）。 */
    @NotNull
    private int[] currentBases() {
        int[] out = new int[ThemePrefs.SCHEME_BASES];
        out[0] = liveBaseOr("surface", true);
        out[1] = liveBaseOr("neutral", true);
        out[2] = liveBaseOr("surface", false);
        out[3] = liveBaseOr("neutral", false);
        return out;
    }

    /** 某个模式下生效的底色；没设过就用该模式的预设默认值（不拿另一个模式的值来顶）。 */
    private int liveBaseOr(@NotNull String key, boolean dark) {
        int live = ThemeManager.INSTANCE.liveBase(this, key, dark);
        return live != 0 ? live : ThemeManager.defaultBase(key, dark);
    }

    /**
     * v1.10.209：当前选中方案里，这个颜色**存档的原始值**；没有选中方案 / 没这一格 → 0。
     *
     * 底色取「当前模式」那一份（方案存档里深/浅各一份），其它色取扁平那一格。
     */
    private int schemeValueFor(@NotNull String key) {
        long active = ThemePrefs.INSTANCE.getActiveScheme(this);
        if (active == -1L) {
            return 0;
        }
        if (ThemePrefs.isBaseKey(key)) {
            int[] bases = ThemePrefs.INSTANCE.getSchemeBases(this, active);
            if (bases == null || bases.length < ThemePrefs.SCHEME_BASES) {
                return 0;
            }
            boolean dark = ThemeManager.INSTANCE.isDark(this);
            boolean surface = "surface".contentEquals(key);
            return surface ? (dark ? bases[0] : bases[2]) : (dark ? bases[1] : bases[3]);
        }
        int[] c = ThemePrefs.INSTANCE.getScheme(this, active);
        int idx = colorSlotIndex(key);
        return (c != null && idx >= 0 && idx < c.length) ? c[idx] : 0;
    }

    /** 语义色 → 方案存档里的下标（与 ThemePrefs 的 6/7 字段顺序一致）。 */
    private static int colorSlotIndex(@NotNull String key) {
        if ("primary".contentEquals(key)) {
            return 0;
        }
        if ("secondary".contentEquals(key)) {
            return 1;
        }
        if ("tertiary".contentEquals(key)) {
            return 2;
        }
        if ("surface".contentEquals(key)) {
            return 3;
        }
        if ("neutral".contentEquals(key)) {
            return 4;
        }
        if ("slider".contentEquals(key)) {
            return 5;
        }
        if ("error".contentEquals(key)) {
            return 6;
        }
        return -1;
    }

    private static String hex(int color) {
        return String.format(java.util.Locale.US, "#%06X", color & 0xFFFFFF);
    }

    /**
     * 点击方案 = 换配色 + 勾选动画（新选中的淡入放大，被取消的倒放收回）。
     * 这里刻意不重建网格：一重建图标对象就换了，动画会丢。
     */
    private void applyScheme(final long id) {
        // v1.10.201（反馈）：点「已经选中」的方案不该再跑一遍 —— 以前会重写一次颜色，
        // 看起来像"又生效了一次"。切到别的方案再切回来才应该重新应用。
        if (id == ThemePrefs.INSTANCE.getActiveScheme(this)) {
            Diag.warn(this, "A05", "方案 " + id + " 已是选中态 → 忽略这次点击");
            return;
        }
        // v1.10.99（反馈 2.2）：换配色方案 = 整页叠化（勾选圈的动画仍在遮罩下面跑）
        // v1.10.176（反馈「滑块的曲线融合了叠化，观感不好」）：
        //   对比度是方案的一部分，切方案时滑杆要走一条 Ease 曲线（v1.10.91）。
        //   可它跟整页叠化**同一段时间**跑：那张旧画面不透明、又盖在最上层，
        //   于是 300ms 里滑块全程被雾罩着，曲线等于看不见，还多出一层脏影。
        //   现在串行：先叠化换色（滑杆不动），遮罩消失后再走曲线 —— 一次只动一件事。
        this.deferContrastCurve = true;
        Morph.crossfade(this, () -> applySchemeInner(id), () -> {
            // v1.10.203：连点两个方案时，先结束的那层遮罩的回调会早于最后一层。
            // 只要还有遮罩挂在页面上，就把滑杆交给最后一层 —— 否则两条曲线互相 cancel，
            // 谁最后到就听谁的，看着就是「停在旧方案的值」。
            this.deferContrastCurve = Morph.isCrossfading(this);
            if (this.deferContrastCurve) {
                return;
            }
            animateContrastSliderTo(ThemePrefs.INSTANCE.getContrastX100(this));
            updateContrastLabel();
        });
    }

    private void applySchemeInner(final long id) {
        long prev = ThemePrefs.INSTANCE.getActiveScheme(this);
        if (!writeSchemeColors(id)) {
            return;
        }
        refreshThemeInPlace(false);
        ThemeColors colors = ThemeManager.INSTANCE.colors(this);
        if (prev != id) {
            View old = findSchemeTile(prev);
            if (old instanceof SchemeTileView) {
                ((SchemeTileView) old).setRingColor(colors.getText());
                ((SchemeTileView) old).animateActive(false);
            }
        }
        View now = findSchemeTile(id);
        if (now instanceof SchemeTileView) {
            ((SchemeTileView) now).setRingColor(colors.getText());
            ((SchemeTileView) now).animateActive(true);
        }
        updateAddTileColors(colors);
    }

    /** 占位图标的底色 = 主色与辅助色的中间色，换配色后要跟着变。 */
    private void updateAddTileColors(ThemeColors colors) {
        View add = findSchemeTile(KEY_ADD);
        if (add instanceof SchemeTileView) {
            ((SchemeTileView) add).setQuadrants(
                    between(colors.getPrimary(), colors.getSecondary()), 0, 0, 0);
        }
    }

    /** 把「当前配色」存为新方案（存未经对比度/反转处理的原始色）。 */
    private void saveCurrentPalette() {
        ThemeColors raw = ThemeManager.INSTANCE.rawColors(this);
        int[] c = {raw.getPrimary(), raw.getSecondary(), raw.getTertiary(),
                raw.getSurface(), raw.getNeutral(), raw.getSlider(),
                raw.getError()};
        // v1.10.202：底色「按模式各存一份」也进存档 —— 只在当前模式看过是不够的，
        // 否则换到另一个模式再切回来，这边又得靠猜。
        int[] bases = currentBases();
        long id = ThemePrefs.INSTANCE.addScheme(this, c);
        if (id < 0) {
            return;
        }
        ThemePrefs.INSTANCE.setSchemeBases(this, id, bases);
        ThemePrefs.INSTANCE.setActiveScheme(this, id);
        // v1.10.50：先把「预设那一行」重画（此时 hasCustom=true → 预设高亮自动撤掉），
        // 再重建方案网格。原来只调 buildSchemes，预设行还闪着「已选」，两处同时勾着。
        refreshThemeInPlace(false);
        // 新图标出现在原占位图标的位置（做一个「由小变大」的入场），占位图标滑到下一个空位
        buildSchemes(true, id);
        toast(s(R.string.settings_scheme_saved));
    }

    private void deleteScheme(final long id) {
        final long[] ids = ThemePrefs.INSTANCE.getSchemeIds(this);
        final int gap = ThemePrefs.indexOf(ids, id);
        final long activeBefore = ThemePrefs.INSTANCE.getActiveScheme(this);
        final boolean wasActive = activeBefore == id;
        View tile = findSchemeTile(id);
        Runnable finish = () -> {
            ThemePrefs.INSTANCE.removeScheme(SettingsActivity.this, id);
            long[] rest = ThemePrefs.INSTANCE.getSchemeIds(SettingsActivity.this);
            long pop = (gap >= 0 && gap < rest.length) ? rest[gap] : KEY_ADD;
            long nextActive = -1L;
            if (wasActive) {
                if (rest.length > 0) {
                    // 勾选跳到前一个；没有前一个（删的是第一个）就跳后一个
                    int idx = Math.max(0, Math.min(gap - 1, rest.length - 1));
                    nextActive = rest[idx];
                    writeSchemeColors(nextActive);
                    refreshThemeInPlace(false);
                } else {
                    // 最后一个方案也没了 → 跳预设配色第 1 个（和手点预设完全一样的切换）
                    ThemePrefs.INSTANCE.setPreset(SettingsActivity.this, 0);
                    ThemePrefs.INSTANCE.clearCustom(SettingsActivity.this);
                    ThemePrefs.INSTANCE.setActiveScheme(SettingsActivity.this, -1L);
                    // v1.10.203：还要把对比度换回「预设共享值」——
                    // 注释写的是"和手点预设完全一样"，但原来漏了这一步：
                    // 预设时对比度的主人是 preset_contrast_x100，不清掉就还是刚删掉那个方案的值，
                    // 于是滑杆停在一个「任何预设都不认」的数字上。
                    ThemePrefs.INSTANCE.setContrastX100(SettingsActivity.this,
                            ThemePrefs.INSTANCE.getPresetContrastX100(SettingsActivity.this));
                    refreshThemeInPlace(false);
                }
            }
            // 重建网格：其余图标补位 + 新勾选的那个走「点击同款」进场动画
            buildSchemes(true, pop, nextActive);
            toast(s(R.string.settings_scheme_deleted));
        };
        if (tile instanceof SchemeTileView) {
            ((SchemeTileView) tile).swallow(finish);
        } else {
            finish.run();
        }
    }

    private View findSchemeTile(long id) {
        LinearLayout host = this.llSchemes;
        if (host == null) {
            return null;
        }
        for (int r = 0; r < host.getChildCount(); r++) {
            View row = host.getChildAt(r);
            if (!(row instanceof ViewGroup)) {
                continue;
            }
            ViewGroup group = (ViewGroup) row;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof SchemeTileView && child.getTag() instanceof Long
                        && ((Long) child.getTag()) == id) {
                    return child;
                }
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void buildPresets() {
        // v1.10.388：「系统预设」小标题前的三色块符号（自绘，代替再写一个同字数标题）
        View presetGlyph = findViewById(R.id.glyph_presets);
        if (presetGlyph instanceof GlyphView) {
            // v1.10.522：「系统预设」左侧图标重新绘制 —— 三段色环（GlyphView.WHEEL）
            ((GlyphView) presetGlyph).setGlyph(GlyphView.WHEEL);
            ((GlyphView) presetGlyph).setTintColor(
                    ThemeManager.INSTANCE.withAlpha(ThemeManager.INSTANCE.colors(this).getText(), 0.62f));
        }
        LinearLayout linearLayout = this.llPresets;
        if (linearLayout == null) {
            Intrinsics.throwUninitializedPropertyAccessException("llPresets");
            linearLayout = null;
        }
        linearLayout.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        // v1.10.49：有自定义方案 / 自定义色时，预设这一行不显示选中框
        //（否则勾了自定义方案，预设里还戳着一个「已选」，用户反馈的问题就是这个）
        int selected = ThemePrefs.INSTANCE.hasCustom(this) ? -1 : ThemePrefs.INSTANCE.getPreset(this);
        ThemeColors colors = ThemeManager.INSTANCE.colors(this);
        Iterable $this$forEachIndexed$iv = ThemeManager.INSTANCE.getPresets();
        int index$iv = 0;
        for (Object item$iv : $this$forEachIndexed$iv) {
            int index = index$iv;
            index$iv++;
            if (index < 0) {
                CollectionsKt.throwIndexOverflow();
            }
            ThemeManager.Preset preset = (ThemeManager.Preset) item$iv;
            LinearLayout $this$buildPresets_u24lambda_u2428_u24lambda_u2421 = new LinearLayout(this);
            $this$buildPresets_u24lambda_u2428_u24lambda_u2421.setOrientation(1);
            $this$buildPresets_u24lambda_u2428_u24lambda_u2421.setGravity(1);
            // v1.10.388：行改成横向滚动 → 每格定宽（原先 weight=1 是均分整屏，滚动时会全挤在一起）
            $this$buildPresets_u24lambda_u2428_u24lambda_u2421.setLayoutParams(
                    new LinearLayout.LayoutParams((int) (64 * density), -2));
            View swatch = new View(this);
            int size = (int) (42 * density);
            LinearLayout.LayoutParams $this$buildPresets_u24lambda_u2428_u24lambda_u2422 = new LinearLayout.LayoutParams(size, size);
            $this$buildPresets_u24lambda_u2428_u24lambda_u2422.topMargin = (int) (6 * density);
            swatch.setLayoutParams($this$buildPresets_u24lambda_u2428_u24lambda_u2422);
            GradientDrawable $this$buildPresets_u24lambda_u2428_u24lambda_u2423 = new GradientDrawable();
            $this$buildPresets_u24lambda_u2428_u24lambda_u2423.setShape(1);
            $this$buildPresets_u24lambda_u2428_u24lambda_u2423.setColor(preset.getPrimary());
            if (index == selected) {
                $this$buildPresets_u24lambda_u2428_u24lambda_u2423.setStroke((int) (3 * density), colors.getText());
            }
            swatch.setBackground($this$buildPresets_u24lambda_u2428_u24lambda_u2423);
            swatch.setOnClickListener((v2) -> {
                buildPresets$lambda$28$lambda$24(SettingsActivity.this, index, v2);
            });
            TextView $this$buildPresets_u24lambda_u2428_u24lambda_u2427 = new TextView(this);
            $this$buildPresets_u24lambda_u2428_u24lambda_u2427.setText(s(preset.getLabelRes()));
            $this$buildPresets_u24lambda_u2428_u24lambda_u2427.setTextSize(10.0f);
            $this$buildPresets_u24lambda_u2428_u24lambda_u2427.setGravity(17);
            $this$buildPresets_u24lambda_u2428_u24lambda_u2427.setMaxLines(1);
            $this$buildPresets_u24lambda_u2428_u24lambda_u2427.setEllipsize(TextUtils.TruncateAt.END);
            $this$buildPresets_u24lambda_u2428_u24lambda_u2427.setTextColor(index == selected ? colors.getPrimary() : colors.getTextMuted());
            LinearLayout.LayoutParams $this$buildPresets_u24lambda_u2428_u24lambda_u2427_u24lambda_u2425 = new LinearLayout.LayoutParams(-1, -2);
            $this$buildPresets_u24lambda_u2428_u24lambda_u2427_u24lambda_u2425.topMargin = (int) (5 * density);
            $this$buildPresets_u24lambda_u2428_u24lambda_u2427.setLayoutParams($this$buildPresets_u24lambda_u2428_u24lambda_u2427_u24lambda_u2425);
            $this$buildPresets_u24lambda_u2428_u24lambda_u2427.setOnClickListener((v2) -> {
                buildPresets$lambda$28$lambda$27$lambda$26(SettingsActivity.this, index, v2);
            });
            $this$buildPresets_u24lambda_u2428_u24lambda_u2421.addView(swatch);
            $this$buildPresets_u24lambda_u2428_u24lambda_u2421.addView($this$buildPresets_u24lambda_u2428_u24lambda_u2427);
            LinearLayout linearLayout2 = this.llPresets;
            if (linearLayout2 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("llPresets");
                linearLayout2 = null;
            }
            linearLayout2.addView($this$buildPresets_u24lambda_u2428_u24lambda_u2421);
        }
    }

    private static final void buildPresets$lambda$28$lambda$24(SettingsActivity this$0, int $index, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.applyPreset($index);
    }

    private static final void buildPresets$lambda$28$lambda$27$lambda$26(SettingsActivity this$0, int $index, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.applyPreset($index);
    }

    /**
     * v1.10.179（反馈 2）：选一个**系统预设**（点圆点或点名字）。
     *
     * · v1.10.47：选内置预设 = 回到该预设的原始配色（只改 index 会被自定义色挡住 → 点了没反应）
     * · v1.10.99（反馈 2.2）：换配色 = 整页叠化
     * · v1.10.179：**预设之间共享一个对比度**。从自定义方案切回来时把共享值恢复；
     *   预设之间切换时这个值本来就是当前的，等于不变。
     * · v1.10.176 的规矩同样适用：叠化期间不许再起第二个动效 →
     *   滑杆曲线挂到叠化结束之后（否则曲线被那张不透明旧画面盖住）。
     */
    private void applyPreset(final int index) {
        ThemePrefs.INSTANCE.setPreset(this, index);
        ThemePrefs.INSTANCE.clearCustom(this);
        ThemePrefs.INSTANCE.setContrastX100(this, ThemePrefs.INSTANCE.getPresetContrastX100(this));
        this.deferContrastCurve = true;
        Morph.crossfade(this, () -> refreshThemeInPlace(), () -> {
            // v1.10.203：同 applyScheme —— 只让最后一层遮罩接手滑杆
            this.deferContrastCurve = Morph.isCrossfading(this);
            if (this.deferContrastCurve) {
                return;
            }
            animateContrastSliderTo(ThemePrefs.INSTANCE.getContrastX100(this));
            updateContrastLabel();
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void buildColorRows() {
        LinearLayout linearLayout = this.llColors;
        if (linearLayout == null) {
            Intrinsics.throwUninitializedPropertyAccessException("llColors");
            linearLayout = null;
        }
        linearLayout.removeAllViews();
        ThemeColors colors = ThemeManager.INSTANCE.colors(this);
        String string = s(R.string.settings_color_primary);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        addColorRow(string, "primary", colors.getPrimary());
        String string2 = s(R.string.settings_color_secondary);
        Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
        addColorRow(string2, "secondary", colors.getSecondary());
        String string3 = s(R.string.settings_color_tertiary);
        Intrinsics.checkNotNullExpressionValue(string3, "getString(...)");
        addColorRow(string3, "tertiary", colors.getTertiary());
        String string4 = s(R.string.settings_color_surface);
        Intrinsics.checkNotNullExpressionValue(string4, "getString(...)");
        addColorRow(string4, "surface", colors.getSurface());
        String string5 = s(R.string.settings_color_neutral);
        Intrinsics.checkNotNullExpressionValue(string5, "getString(...)");
        addColorRow(string5, "neutral", colors.getNeutral());
        // v1.10.47：滑块色（内置动图选择器右侧滑块，默认取辅助色）
        String string6 = s(R.string.settings_color_slider);
        Intrinsics.checkNotNullExpressionValue(string6, "getString(...)");
        addColorRow(string6, "slider", colors.getSlider());
        // v1.10.203：错误色（破坏性按钮 / 确认弹窗强调色 / 失败底）。
        // **只暴露这一个红**：错误提示底色 = 错误色压在表面上、on 色 = 黑或白，全部派生。
        // 曾经把「错误提示底色」也做成可编辑的第二个红 —— M3 的浅色 tone40/tone90 与
        // 深色 tone80/tone30 恰好让两个红在两个模式里对调，用户看到的就是「两者互相替换」。
        String stringError = s(R.string.settings_color_error);
        Intrinsics.checkNotNullExpressionValue(stringError, "getString(...)");
        addColorRow(stringError, "error", colors.getError());
        // v1.10.463（用户否决 462 的第二条：只做了底层取色函数，界面上没有这一行）：
        // 「预览虚线色」现在是真的一个语义色行 —— 键 = outline，存储走 ThemePrefs（档位 + 自定义色）。
        // 它和上面几行不是同一类东西：上面是调色板里的**位置**，这一行是**取色口径**
        // （跟随壁纸 / 默认 / 自定义），点开只显示属于它的三档选择器。
        // 色块 = 当前真正画出来的虚线色（OutlineColor.resolve 的结果，与预览框同一个函数）。
        addColorRow(tr("预览虚线色", "Preview outline"), "outline",
                OutlineSettings.currentColor(this));
    }

    /**
     * 滑块色的「作用范围」两段选择器（**只在滑块色的编辑页里出现**）。
     *
     * <p>v1.10.464（用户：「图17→滑块色的分段选择器并没有适配新组件」）：
     * 这里原来用的是另一个自绘控件 {@code SliderScopeSwitch}（满宽胶囊、指示块实底主色）——
     * 和虚线色那条新组件不是同一副样子。现在**全仓库只有一份分段选择器实现**：
     * 同一条 {@link SegmentedRow}，两段文字换成「仅动图选择器 / 全局」。
     * 老控件已删除（它只在这个位置用过）。</p>
     *
     * <p>这个方法只负责「造一个上过色、已选中正确段的控件」，回调由调用方装。</p>
     */
    private final SegmentedRow buildScopeRow(ThemeColors colors) {
        SegmentedRow row = new SegmentedRow(this);
        row.setLabels(new String[]{s(R.string.settings_slider_scope_picker),
                s(R.string.settings_slider_scope_global)});
        row.applyColors(colors);
        row.setSelected(ThemePrefs.INSTANCE.getSliderScope(this), false);
        return row;
    }

    private final void addColorRow(String label, String key, int color) {
        float density = getResources().getDisplayMetrics().density;
        ThemeColors colors = ThemeManager.INSTANCE.colors(this);
        LinearLayout $this$addColorRow_u24lambda_u2430 = new LinearLayout(this);
        $this$addColorRow_u24lambda_u2430.setOrientation(0);
        $this$addColorRow_u24lambda_u2430.setGravity(16);
        $this$addColorRow_u24lambda_u2430.setPadding((int) (14 * density), (int) (10 * density), (int) (12 * density), (int) (10 * density));
        $this$addColorRow_u24lambda_u2430.setClickable(true);
        $this$addColorRow_u24lambda_u2430.setOnClickListener((v4) -> {
            addColorRow$lambda$30$lambda$29(SettingsActivity.this, label, key, color, v4);
        });
        TextView $this$addColorRow_u24lambda_u2431 = new TextView(this);
        // v1.10.240 自测（小屏 + 大字体）：滑块色行多了一个作用范围芯片，
        // 320dp 屏 × 1.3 倍字号时标题只剩 ~32dp —— 这里改成单行省略，
        // 宁可截断标题也不让整行换行把色块挤到第二行。
        $this$addColorRow_u24lambda_u2431.setSingleLine(true);
        $this$addColorRow_u24lambda_u2431.setEllipsize(android.text.TextUtils.TruncateAt.END);
        $this$addColorRow_u24lambda_u2431.setText(label);
        $this$addColorRow_u24lambda_u2431.setTextColor(colors.getText());
        $this$addColorRow_u24lambda_u2431.setTextSize(15.0f);
        $this$addColorRow_u24lambda_u2431.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        View $this$addColorRow_u24lambda_u2434 = new View(this);
        $this$addColorRow_u24lambda_u2434.setLayoutParams(new LinearLayout.LayoutParams((int) (36 * density), (int) (36 * density)));
        GradientDrawable $this$addColorRow_u24lambda_u2434_u24lambda_u2432 = new GradientDrawable();
        $this$addColorRow_u24lambda_u2434_u24lambda_u2432.setCornerRadius(10.0f * density);
        $this$addColorRow_u24lambda_u2434_u24lambda_u2432.setColor(color);
        $this$addColorRow_u24lambda_u2434_u24lambda_u2432.setStroke((int) (1 * density), colors.getNeutral());
        $this$addColorRow_u24lambda_u2434.setBackground($this$addColorRow_u24lambda_u2434_u24lambda_u2432);
        $this$addColorRow_u24lambda_u2434.setOnClickListener((v4) -> {
            addColorRow$lambda$34$lambda$33(SettingsActivity.this, label, key, color, v4);
        });
        TextView $this$addColorRow_u24lambda_u2435 = new TextView(this);
        $this$addColorRow_u24lambda_u2435.setText("›");
        $this$addColorRow_u24lambda_u2435.setTextSize(20.0f);
        $this$addColorRow_u24lambda_u2435.setTextColor(colors.getTextMuted());
        $this$addColorRow_u24lambda_u2435.setPadding((int) (10 * density), 0, 0, 0);
        // v1.10.463（用户否决 462 的第三条：迁移做成了复制）：首页这一行**不再挂作用范围开关**。
        // 它现在只在「滑块色」的编辑页出现（showColorEditor 里按 key 建），首页只剩
        // 标题 → 色块 → 箭头。状态仍然只有一条：ThemePrefs.getSliderScope / setSliderScope。
        $this$addColorRow_u24lambda_u2430.addView($this$addColorRow_u24lambda_u2431);
        $this$addColorRow_u24lambda_u2430.addView($this$addColorRow_u24lambda_u2434);
        $this$addColorRow_u24lambda_u2430.addView($this$addColorRow_u24lambda_u2435);
        LinearLayout linearLayout = this.llColors;
        if (linearLayout == null) {
            Intrinsics.throwUninitializedPropertyAccessException("llColors");
            linearLayout = null;
        }
        linearLayout.addView($this$addColorRow_u24lambda_u2430);
    }

    private static final void addColorRow$lambda$30$lambda$29(SettingsActivity this$0, String $label, String $key, int $color, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter($label, "$label");
        Intrinsics.checkNotNullParameter($key, "$key");
        this$0.showColorEditor($label, $key, $color);
    }

    private static final void addColorRow$lambda$34$lambda$33(SettingsActivity this$0, String $label, String $key, int $color, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter($label, "$label");
        Intrinsics.checkNotNullParameter($key, "$key");
        this$0.showColorEditor($label, $key, $color);
    }

    /** v1.10.462：语言兜底（这两档文案暂不进 string 资源，避免动 R/stable-ids/arsc 三处）。 */
    private String tr(String zh, String en) {
        String lang = java.util.Locale.getDefault().getLanguage();
        return lang != null && lang.startsWith("zh") ? zh : en;
    }

    /**
     * v1.10.463：把编辑页的**附加控件**插到「简单变体」容器正下方（用户指定：容器下方，不是标题下方）。
     *
     * <p>锚点算位置（不写死下标）：462 用 `at + 1 + i` 一次插两条，两条选择器会互相挤位；
     * 现在一次只插一条，位置一律按锚点现算。</p>
     */
    private void insertUnderVariants(@org.jetbrains.annotations.NotNull LinearLayout root,
                                     @org.jetbrains.annotations.Nullable View anchor,
                                     @org.jetbrains.annotations.NotNull View row) {
        try {
            ViewGroup host = anchor != null && anchor.getParent() instanceof ViewGroup
                    ? (ViewGroup) anchor.getParent() : root;
            int at = anchor != null ? host.indexOfChild(anchor) + 1 : host.getChildCount();
            int gap = Math.round(10.0f * getResources().getDisplayMetrics().density);
            // v1.10.464（用户）：**靠左，不是居中**；宽度只占它自己要的那点，不再是一条满宽横条。
            // 摆位一律走 gravity=START（RTL 下自动靠右）；右边距只是「别顶到面板边」的余量。
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = android.view.Gravity.START;
            lp.topMargin = gap;
            lp.setMarginEnd(gap);
            host.addView(row, Math.min(at, host.getChildCount()), lp);
        } catch (Throwable th) {
            Diag.warn(this, "A12", "分段选择器插入失败: " + th.getClass().getSimpleName());
        }
    }

    /**
     * 三块区域置灰（用户口径：手控面板 / 颜色预览 / 简单变体，**文字除外**），带 180ms 叠化。
     * 只改 alpha 是假禁用，必须同时 setEnabled(false)，否则还能点。
     */
    private void applyOutlineLock(@org.jetbrains.annotations.Nullable View manual,
                                  @org.jetbrains.annotations.Nullable View preview,
                                  @org.jetbrains.annotations.Nullable View variants,
                                  boolean locked, boolean animate) {
        View[] views = {manual, preview, variants};
        for (View v : views) {
            if (v == null) {
                continue;
            }
            v.setEnabled(!locked);
            setTreeEnabled(v, !locked);
            if (animate) {
                v.animate().cancel();
                v.animate().alpha(locked ? 0.38f : 1.0f).setDuration(180L).start();
            } else {
                v.setAlpha(locked ? 0.38f : 1.0f);
            }
        }
    }

    private static void setTreeEnabled(@org.jetbrains.annotations.NotNull View v, boolean on) {
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                setTreeEnabled(g.getChildAt(i), on);
            }
        }
        v.setEnabled(on);
    }

    /**
     * v1.10.463：把面板自己的滚动条准备好（**滚动只有这一层**）。
     *
     * <p>布局里那个 ScrollView 是唯一该滚的东西：面板高度被下面那段「小屏兜底」夹到 86% 屏高后，
     * 它会拿到 weight 份额、真正滚起来；不超过 86% 时它按内容撑开，没有任何滚动。</p>
     *
     * <p>462 在面板外面又套了一层 ScrollView 当「视觉外框」，同时没清掉
     * {@code applyEditorTheme} 里给 root 刷的同一张玻璃底 —— 于是面板上叠了**两层圆角玻璃**：
     * 外框一层、内层 root 再一层（内缩 18dp 的第二个圆角），颜色被叠深一档，
     * 而且两层都会响应滚动。现在把外框删掉，滚动与底色都只剩一层。</p>
     */
    private void enableSheetScroll(@org.jetbrains.annotations.NotNull LinearLayout root) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof android.widget.ScrollView) {
                // OVER_SCROLL_ALWAYS：Android 12+ 就是系统橡皮筋（用户要的「整页可滚 + 回弹」）
                child.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
            }
        }
    }

    private final void showColorEditor(String label, String key, int initial) {
        final ThemeColors colors = ThemeManager.INSTANCE.colors(this);
        final float density = getResources().getDisplayMetrics().density;
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(1);
        ThemeManager.INSTANCE.clearDialogWindow(dialog);   // v1.10.71：圆角不露方底
        // v1.10.200：面板是独立 Dialog，Activity 的 resources 还停在旧 locale ——
        // 必须用 wrap 过的 inflater，面板里的「简单变体 / 取消 / 保存」才会跟着语言走
        Context wctx = LocaleHelper.INSTANCE.wrap(getApplicationContext());
        final View sheet = LayoutInflater.from(wctx).inflate(R.layout.dialog_color_editor, (ViewGroup) null);
        Intrinsics.checkNotNull(sheet, "null cannot be cast to non-null type android.widget.LinearLayout");
        LinearLayout root = (LinearLayout) sheet;

        // v1.10.463：**每行颜色点开只看到自己的控件**。
        //
        // 462 把「虚线色三档」和「滑块色作用范围」无条件插进这个共用弹窗 —— 于是点主色、
        // 点表面色、点错误色，都会看到两条与自己毫无关系的选择器（用户第一眼就否了）。
        // 现在：只在**本页对应的 key** 下才建对象；别的键连对象都不存在，插入代码也进不去。
        //   · outline（预览虚线色）→ 三条：跟随壁纸 / 默认 / 自定义
        //   · slider（滑块色）    → 两条：仅动图选择器 / 全局（同一个 SegmentedRow，只是换了两段文字）
        //   · 其它颜色            → 一条都不显示
        final boolean isOutline = "outline".contentEquals(key);
        final boolean isSlider = "slider".contentEquals(key);
        final SegmentedRow modeRow = isOutline ? new SegmentedRow(this) : null;
        final SegmentedRow scopeRow = isSlider ? buildScopeRow(colors) : null;
        // 三块「跟着档位置灰」的区域（文字不动：color_variants_label 不在其中）
        final View manualPanel = root.findViewById(R.id.color_picker_host);
        final View colorPreview = root.findViewById(R.id.color_preview);
        final View variantsBox = root.findViewById(R.id.color_variants);


        // v1.10.140（Z1~Z4）：底层 = 3 个巨型光轮 + 100% 高斯模糊；上面才是半透明的玻璃面板。
        // 光轮颜色 = 主色 / 辅助色 / 第三色各一轮，排列由 ColorAuroraView.nextOrder 保证
        // 不与最近 3 次进入重复；相位接上次退出的位置继续漂移。
        final ColorAuroraView aurora = new ColorAuroraView(this);
        int[] themePalette = new int[]{colors.getPrimary(), colors.getSecondary(), colors.getTertiary()};
        int[] order = ColorAuroraView.nextOrder(this);
        aurora.setRingColors(new int[]{themePalette[order[0]], themePalette[order[1]], themePalette[order[2]]});
        aurora.setBaseColor(colors.getBackground());
        // v1.10.153：相位必须**先读出来再打日志**。之前日志打在 setPhase 之前，
        // 打的是字段默认值 0 —— 于是「相位恒为 0」，看上去像保存链路坏了，其实是日志位置错了。
        float auroraPhase = ColorAuroraView.loadPhase(this);
        aurora.setPhase(auroraPhase);
        // v1.10.149（A5）：把这次的排列与相位写进日志，方便核对「不与最近 3 次重复」
        String[] slotNames = {"主色", "辅助色", "第三色"};
        Diag.warn(this, "A06", "调色背景 轮0=" + slotNames[order[0]] + " 轮1=" + slotNames[order[1]]
                + " 轮2=" + slotNames[order[2]] + " 相位=" + String.format(java.util.Locale.US, "%.1f", auroraPhase)
                + " 历史=" + ColorAuroraView.history(this));

        // v1.10.368 诊断：每次打开重置一次（几何变化就再打一条，前 10 拍连 top 色一起打）
        editorWinGeom = null;
        editorWinTicks = 0;
        this.colorEditorOpen = true;
        FrameLayout shell = new FrameLayout(this);
        shell.addView(aurora, new FrameLayout.LayoutParams(-1, -1));
        // v1.10.142（反馈 1）：面板从「几乎占满屏幕的 sheet」改成**四周留白的悬浮卡片** ——
        // 宽度 = 屏宽 - 36dp、高度 = 内容自适应（不再是被拉满的 88%）、离底 20dp。
        // 上一版把面板拉到 88% 高，内容只用到按钮，底下就拖出一大块「大下巴」，
        // 而且背景色雾只剩最上面一条 —— 高斯模糊根本看不见。
        final int screenH = getResources().getDisplayMetrics().heightPixels;
        final int screenW = getResources().getDisplayMetrics().widthPixels;
        final int maxSheetHeight = (int) (screenH * 0.86f);
        // v1.10.144（反馈 1）：固定**屏幕正中**（水平 + 垂直），不再贴底。
        // 上一版只给 BOTTOM|CENTER_HORIZONTAL，垂直方向被底部边距拉走，看起来像「错位」。
        FrameLayout.LayoutParams sheetLp = new FrameLayout.LayoutParams(
                screenW - Math.round(36.0f * density), ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER);
        // v1.10.463：**面板直接进 shell**（回到 461 的骨架），滚动交回布局里那个 ScrollView。
        //
        // 462 在面板外面又套了一层 ScrollView 当「视觉外框」，还把面板背景搬到外框上 ——
        // 可 `applyEditorTheme`（每次打开都会立刻跑一次）仍然给 root 刷同一张玻璃底，
        // 于是面板上出现两层内缩的圆角玻璃；而且两层容器都能滚（外框滚、内层也挂着 weight）。
        // 现在只剩一层：固定标题 + 可滚内容 + 固定按钮，超出 86% 屏高才滚（见下面兜底）。
        enableSheetScroll(root);
        shell.addView(sheet, sheetLp);
        dialog.setContentView(shell);
        // v1.10.461（用户要求）：**点空白处不再退出**。
        //
        // 原来这里 shell（面板外的光轮区域）绑的是「关闭」，用户想调色时误触一下就全丢了。
        // 现在这一层只做"吃事件"—— 点它什么都不发生，也不会穿透到下面的页面去。
        // 关闭只剩两个明确出口：面板里的「取消」/「保存」，以及系统返回键。
        // 只改这一处（showColorEditor 内部），**不碰任何别的页面/Dialog**：
        // ConfirmDialog、MaskGestureDialog、LogActivity 那几处的 setCanceledOnTouchOutside
        // 与点击处理完全没动（"不要误伤友军"）。
        shell.setOnClickListener(v1 -> {
            // 故意留空：吃掉落点，不关闭。
        });
        sheet.setClickable(true);

        // v1.10.155：撤销上一版的磨砂颗粒层，回到「背景色自带 alpha」的半透明面板。
        // 注意：不能用带着整体 alpha 的 Drawable（那会把这里的 alpha 抹成不透明），
        // 必须让填充色自己带 alpha —— 这才是 rgba 语义，后面的光轮才透得上来。
        // 不透明度：深色 0.52 / 浅色 0.64（比上一版再降 0.14）。
        // 描边：1px 半透明白（玻璃边缘受光），不再用灰色。
        final boolean sheetDark = ThemeManager.INSTANCE.isDark(this);
        GradientDrawable sheetBg = new GradientDrawable();
        sheetBg.setColor(fontAlpha(colors.getSurface(), sheetDark ? 0.52f : 0.64f));
        sheetBg.setCornerRadius(28.0f * density);
        sheetBg.setStroke(Math.max(1, Math.round(density)), 0x47FFFFFF);
        // v1.10.463：背景回到面板自己身上（462 挂在外面那层假 ScrollView 上，圆角和内容会一起滚）。
        // applyEditorTheme 里深浅色切换时也是刷这个同一个 Drawable 位置，不会出现两层底色。
        root.setBackground(sheetBg);
        // 这个面板是独立 Dialog，不在 Activity 的视图树里 —— 不主动铺一次字体，
        // 它就会一直是系统字体，和 App 其它页面不是一套（v1.10.140 顺带修掉）。
        Fonts.applyTree(sheet);
        TextView title = (TextView) sheet.findViewById(R.id.color_title);
        title.setText(label);
        title.setTextColor(colors.getText());
        TextView close = (TextView) sheet.findViewById(R.id.color_close);
        close.setTextColor(colors.getText());
        close.setOnClickListener((v1) -> closeColorEditor(dialog, sheet, aurora, null));
        final View preview = sheet.findViewById(R.id.color_preview);
        final EditText hex = (EditText) sheet.findViewById(R.id.color_hex);
        hex.setTextColor(colors.getText());
        hex.setHintTextColor(colors.getTextMuted());
        GradientDrawable $this$showColorEditor_u24lambda_u2438 = new GradientDrawable();
        $this$showColorEditor_u24lambda_u2438.setCornerRadius(14.0f * density);
        $this$showColorEditor_u24lambda_u2438.setColor(colors.getNeutral());
        hex.setBackground($this$showColorEditor_u24lambda_u2438);
        TextView copy = (TextView) sheet.findViewById(R.id.color_copy);
        TextView paste = (TextView) sheet.findViewById(R.id.color_paste);
        copy.setTextColor(colors.getTextMuted());
        paste.setTextColor(colors.getTextMuted());
        FrameLayout host = (FrameLayout) sheet.findViewById(R.id.color_picker_host);
        final HsvPickerView $this$showColorEditor_u24lambda_u2439 = new HsvPickerView(this);
        $this$showColorEditor_u24lambda_u2439.setColor(initial);
        $this$showColorEditor_u24lambda_u2439.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        host.addView($this$showColorEditor_u24lambda_u2439);
        final Ref.BooleanRef updatingHex = new Ref.BooleanRef();
        $this$showColorEditor_u24lambda_u2439.setOnColorChanged(new C00014(preview, updatingHex, hex, density, colors));
        hex.addTextChangedListener(new TextWatcher() { // from class: io.github.animwidget.SettingsActivity.showColorEditor.5
            @Override // android.text.TextWatcher
            public void beforeTextChanged(@Nullable CharSequence s, int start, int count, int after) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(@Nullable CharSequence s, int start, int before, int count) {
            }

            /* JADX WARN: Removed duplicated region for block: B:10:0x0018  */
            @Override // android.text.TextWatcher
            /*
                Code decompiled incorrectly, please refer to instructions dump.
                To view partially-correct add '--show-bad-code' argument
            */
            public void afterTextChanged(@org.jetbrains.annotations.Nullable android.text.Editable r9) {
                /*
                    r8 = this;
                    r0 = r8
                    kotlin.jvm.internal.Ref$BooleanRef r0 = r4
                    boolean r0 = r0.element
                    if (r0 == 0) goto Lb
                    return
                Lb:
                    r0 = r9
                    r1 = r0
                    if (r1 == 0) goto L17
                    java.lang.String r0 = r0.toString()
                    r1 = r0
                    if (r1 != 0) goto L1a
                L17:
                L18:
                    java.lang.String r0 = ""
                L1a:
                    java.lang.Integer r0 = io.github.animwidget.SettingsActivity.access$showColorEditor$parseHex(r0)
                    r1 = r0
                    if (r1 == 0) goto L27
                    int r0 = r0.intValue()
                    goto L29
                L27:
                    return
                L29:
                    r10 = r0
                    r0 = r8
                    io.github.animwidget.HsvPickerView r0 = r5
                    r1 = r10
                    r0.setColor(r1)
                    r0 = r8
                    android.view.View r0 = r6
                    r1 = r8
                    kotlin.jvm.internal.Ref$BooleanRef r1 = r4
                    r2 = r8
                    android.widget.EditText r2 = r7
                    r3 = r8
                    float r3 = r8
                    r4 = r8
                    io.github.animwidget.ThemeColors r4 = r9
                    r5 = r10
                    r6 = 0
                    io.github.animwidget.SettingsActivity.access$showColorEditor$render(r0, r1, r2, r3, r4, r5, r6)
                    return
                */
                android.text.Editable editable = r9;
                if (updatingHex.element) {
                    return;
                }
                String text = editable != null && editable.toString() != null ? editable.toString() : "";
                Integer parsed = showColorEditor$parseHex(text);
                if (parsed == null) {
                    return;
                }
                int value = parsed.intValue();
                $this$showColorEditor_u24lambda_u2439.setColor(value);
                showColorEditor$render(preview, updatingHex, hex, density, colors, value, false);
            }
        });
        hex.setOnEditorActionListener((v2, v3, v4) -> {
            return showColorEditor$lambda$42(hex, $this$showColorEditor_u24lambda_u2439, title, v3, v4);
        });
        copy.setOnClickListener((v2) -> {
            showColorEditor$lambda$43($this$showColorEditor_u24lambda_u2439, SettingsActivity.this, v2);
        });
        paste.setOnClickListener((v7) -> {
            showColorEditor$lambda$44(SettingsActivity.this, $this$showColorEditor_u24lambda_u2439, preview, updatingHex, hex, density, colors, v7);
        });
        GridLayout variants = (GridLayout) sheet.findViewById(R.id.color_variants);
        // v1.10.70：简单变体改成「只存种子色，点一下现算」。
        //   • 圆点画成三段：上半主色、左下第三色、右下辅助色（一个种子推出一整套配色）
        //   • 点击把**当前槽位**（primary/secondary/tertiary/surface/neutral/slider）算出的颜色填进来
        // 旧实现是 12 个硬编码颜色，和"简单变体"这个说法完全不符（点了也不可能推出整套配色）。
        final boolean darkMode = ThemeManager.INSTANCE.isDark(this);
        final int contrast = ThemePrefs.INSTANCE.getContrastX100(this);
        int dotSize = (int) (36 * density);
        int dotMargin = (int) (4 * density);
        for (final int seed : Palette.SEEDS) {
            final int[] palette = Palette.fromSeed(seed, darkMode, contrast);
            final int slotColor = Palette.slot(palette, key);
            VariantDot dot = new VariantDot(this, palette);
            GridLayout.LayoutParams lpDot = new GridLayout.LayoutParams();
            lpDot.width = dotSize;
            lpDot.height = dotSize;
            lpDot.setMargins(dotMargin, dotMargin, dotMargin, dotMargin);
            dot.setLayoutParams(lpDot);
            dot.setContentDescription(Integer.toHexString(seed | 0xFF000000));
            dot.setOnClickListener((v7) -> {
                showColorEditor$lambda$49$lambda$48$lambda$47($this$showColorEditor_u24lambda_u2439, slotColor, preview, updatingHex, hex, density, colors, v7);
            });
            variants.addView(dot);
        }
        TextView variantsLabel = (TextView) sheet.findViewById(R.id.color_variants_label);
        variantsLabel.setTextColor(colors.getTextMuted());

        // v1.10.463：**两条附加控件各归各页**，插在「简单变体」容器正下方（用户指定的位置）。
        // 这两段各自用 `!= null` 判定：modeRow 只在「预览虚线色」页建、scopeRow 只在「滑块色」页建，
        // 别的语义色页一个对象都没有 —— 这是 462 被否的第一条（串页）的结构性修法。
        if (modeRow != null) {
            // ---- 预览虚线色：三档预设（跟随壁纸 / 默认 / 自定义）
            modeRow.setLabels(OutlineSettings.modeLabels());
            modeRow.applyColors(colors);
            modeRow.setSelected(OutlineSettings.modeIndex(this), false);
            // 三块（手控面板 / 颜色预览 / 简单变体）只在这一页、且只有「自定义」档可手控
            applyOutlineLock(manualPanel, colorPreview, variantsBox,
                    OutlineSettings.modeIndex(this) != OutlineColor.Mode.CUSTOM.ordinal(), false);
            modeRow.setOnSegmentClick(index -> {
                boolean locked = index != OutlineColor.Mode.CUSTOM.ordinal();
                applyOutlineLock(manualPanel, colorPreview, variantsBox, locked, true);
                // v1.10.464（用户）：**切到哪一档，预览块就显示那一档的颜色** ——
                // 和首页色块同一个函数（OutlineSettings.colorForMode），所以「编辑页看到什么」
                // 与「保存后首页显示什么」永远是同一个数。非自定义档不再沿用上一次的自定义色。
                if (locked) {
                    int shown = OutlineSettings.colorForMode(this, index);
                    $this$showColorEditor_u24lambda_u2439.setColor(shown);
                    showColorEditor$render(preview, updatingHex, hex, density, colors, shown, true);
                } else if (ThemePrefs.INSTANCE.hasOutlineCustom(this)) {
                    // 自定义档且存过色：摆到那个色；没存过就保持当前这个色（切档不跳色）
                    int mine = OutlineSettings.customRgb(this);
                    $this$showColorEditor_u24lambda_u2439.setColor(mine);
                    showColorEditor$render(preview, updatingHex, hex, density, colors, mine, true);
                }
                Diag.warn(this, "A13", "预览虚线色档位=" + OutlineColor.Mode.values()[index].name()
                        + (locked ? "（三块已置灰，预览=" + OutlineColor.hex(OutlineSettings.colorForMode(this, index)) + "）"
                                  : "（可手控）"));
            });
            insertUnderVariants(root, variants, modeRow);
        }
        if (scopeRow != null) {
            // ---- 滑块色：作用范围（与虚线色三档**同一个控件**，v1.10.464）
            //
            // v1.10.467（用户：「没点保存，会被自动保存」）：这里以前是点一下就直接写盘，
            // 于是「改了作用范围 → 点取消 → 重新进来」还是新的 —— 对话框的语义必须是**保存才落盘**。
            // 现在这一页的写入只发生在 save.setOnClickListener 里（见下面的保存分支），
            // 点击只改**面板内**的选中态（控件自己滑过去），取消什么都不会发生。
            scopeRow.setOnSegmentClick(scope -> Diag.warn(this, "T30", "滑块色作用范围（面板内预览，保存才落盘）="
                    + (scope == ThemePrefs.SLIDER_SCOPE_GLOBAL ? "全局" : "仅动图选择器")));
            insertUnderVariants(root, variants, scopeRow);
        }
        Button cancel = (Button) sheet.findViewById(R.id.color_cancel);
        Button save = (Button) sheet.findViewById(R.id.color_save);
        // v1.10.142（反馈 3）：胶囊按钮也改成半透明材质 —— 面板本身是玻璃的，
        // 按钮却是两块实心色，断层很显眼。取消 = 中性色 55% + 淡描边；保存 = 主色 78%。
        cancel.setBackground(ThemeManager.INSTANCE.outlined(this, fontAlpha(colors.getNeutral(), 0.55f),
                fontAlpha(colors.getText(), 0.12f), 999.0f));
        cancel.setTextColor(colors.getText());
        save.setBackground(ThemeManager.INSTANCE.rounded(this, fontAlpha(colors.getPrimary(), 0.78f), 999.0f));
        save.setTextColor(colors.getOnPrimary());
        cancel.setOnClickListener((v1) -> closeColorEditor(dialog, sheet, aurora, null));
        save.setOnClickListener((v4) -> {
            // v1.10.463：「预览虚线色」不是往调色板里写一个数，而是「档位 + 自定义色」一起落盘，
            // 所以走自己的保存路径（通用那条会把档位丢掉，下次进来又跳回壁纸档）。
            if (modeRow != null) {
                saveOutlineColor(hex, modeRow, dialog, sheet, aurora);
                return;
            }
            // v1.10.467（用户：「没点保存，会被自动保存」）：作用范围也是这一页的一部分 ——
            // 和颜色一样，只有在点「保存」的这一刻才写盘。「取消」什么都不写。
            if (scopeRow != null) {
                ThemePrefs.INSTANCE.setSliderScope(this, scopeRow.getSelected());
                Diag.warn(this, "T30", "滑块色作用范围保存="
                        + (scopeRow.getSelected() == ThemePrefs.SLIDER_SCOPE_GLOBAL ? "全局" : "仅动图选择器"));
            }
            showColorEditor$lambda$51(hex, SettingsActivity.this, key, dialog, sheet, aurora, v4);
        });

        // ---- v1.10.205（需求）：在「取消」左边加一个「重置」
        //
        // 尺寸（v1.10.206 按反馈调整）：**宽度 = 取消按钮宽度的 1/3**、高度 = 取消按钮的高度（48dp）。
        //   取消按钮的宽度由 XML 的 minWidth(116dp) + 文字决定，只有布局完才知道，
        //   所以先给一个 1/3 的估值，第一次 global layout 后按真实宽度一次性校正。
        //   触摸槽比胶囊两边各多 8dp（且不小于 48dp），保证点击目标够大。
        // 材质（v1.10.206 按反馈调整）：不再是一圈孤零零的描边 —— 改成和「取消」同一套：
        //   中性变体的淡底（42%）+ 正文色 12% 的发丝线，图标用正文色。层级仍是：
        //   保存（主色实底）> 取消（中性 55% 淡底）> 重置（更淡的中性底 + 同款发丝线）。
        // 图标：自绘「圆环 + 箭头」（GlyphView.RESET）—— 24dp 放不下两个字，而 ↺ 这类字形不可控。
        //
        // 只给「滑块色 / 错误色」：这两个颜色的默认值在别处都不存在入口
        //（主色/辅助色/第三色点一下预设就能回去；表面/中性变体跟着模式走）。
        final boolean withReset = "slider".contentEquals(key) || "error".contentEquals(key);
        final FrameLayout resetPill = new FrameLayout(this);
        final GlyphView resetGlyph = new GlyphView(this);
        if (withReset) {
            LinearLayout buttonRow = (LinearLayout) cancel.getParent();
            if (buttonRow != null) {
                final FrameLayout hit = new FrameLayout(this);
                final int slotMin = Math.round(48.0f * density);
                hit.setLayoutParams(new LinearLayout.LayoutParams(slotMin, slotMin));
                hit.setContentDescription(s(R.string.color_editor_reset));
                hit.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                resetPill.setLayoutParams(new FrameLayout.LayoutParams(
                        Math.round(38.0f * density), Math.round(48.0f * density),
                        android.view.Gravity.CENTER));
                resetPill.setBackground(ThemeManager.INSTANCE.outlined(this,
                        fontAlpha(colors.getNeutral(), 0.42f),
                        fontAlpha(colors.getText(), 0.12f), 999.0f));
                resetGlyph.setGlyph(GlyphView.RESET);
                resetGlyph.setTintColor(colors.getText());
                resetPill.addView(resetGlyph, new FrameLayout.LayoutParams(-1, -1));
                hit.addView(resetPill);
                // 宽度 = 取消的 1/3（布局完成后按真实宽度校正一次）
                sheet.getViewTreeObserver().addOnGlobalLayoutListener(
                        new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                int cancelW = cancel.getWidth();
                                // 还没量出来就等下一轮（**先判再摘监听**：摘早了就永远校正不了）
                                if (cancelW <= 0) {
                                    return;
                                }
                                if (sheet.getViewTreeObserver().isAlive()) {
                                    sheet.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                                int w = Math.max(Math.round(30.0f * density), cancelW / 3);
                                ViewGroup.LayoutParams plp = resetPill.getLayoutParams();
                                plp.width = w;
                                resetPill.setLayoutParams(plp);
                                ViewGroup.LayoutParams hlp = hit.getLayoutParams();
                                hlp.width = Math.max(slotMin, w + Math.round(16.0f * density));
                                hit.setLayoutParams(hlp);
                            }
                        });
                hit.setOnClickListener((v0) -> {
                    // 与性能卡开关同款按压反馈（本项目不用 ripple）
                    resetPill.setScaleX(0.88f);
                    resetPill.setScaleY(0.88f);
                    resetPill.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150L)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                    // v1.10.207（反馈，这是我的逻辑错误）：
                    // 「重置」只是把这个**面板里**的颜色改回原值 —— 不写存储、不刷页面。
                    // 真正的落盘必须等「保存」；「取消」什么都不做（对话框的取消必须能撤销一切）。
                    //
                    // v1.10.209（反馈「动了参数之后，如果是原来值就不要显示更新」）：
                    // 原值 = **当前生效配色里这个颜色的存档值**（有选中方案就取方案的，否则取默认）。
                    // 之前一律取全局默认 —— 方案里那个颜色本来是自定义色时，一按重置就把配色
                    // 从"方案的样子"改成了"默认的样子"，于是必然显示「已修改」。
                    // 现在重置只是撤销你在面板里的改动，重置后保存一定回到「干净」。
                    // v1.10.244（反馈「辅助色蓝、滑块色粉，点重置说『已经是默认』」—— 这是真 bug）：
                    // 旧实现先看**当前方案存档**里这个色的值，有就还原成它。于是：
                    //   方案里的滑块色 = 粉 → 「重置」还原成粉 → 等于当前值 → 什么都不动，
                    //   而我上一版加的提示还嘴硬说「已经是默认色」。
                    // 这个按钮的语义就是**回到默认**（滑块色 = 辅助色、错误色 = M3 错误色），
                    // 所以直接取默认值，不再看方案存档。
                    int value = ThemeManager.INSTANCE.defaultValue(this, key);
                    // v1.10.243（反馈「滑块色/错误色的重置没反应」）：
                    // 这两个色的默认值往往**就是当前值**（滑块色默认=辅助色、错误色默认=M3 error），
                    // 按重置等于「让它保持默认」—— 视觉上就是什么都没发生。必须说一句人话，
                    // 否则分不清「已经是默认」和「按钮坏了」。
                    // 滑块色还多一层：作用范围＝仅动图选择器时，它在设置页/配置页本来就看不见。
                    final int beforeReset = $this$showColorEditor_u24lambda_u2439.getColor();
                    $this$showColorEditor_u24lambda_u2439.setColor(value);
                    showColorEditor$render(preview, updatingHex, hex, density,
                            ThemeManager.INSTANCE.colors(this), value, true);
                    final boolean unchanged = beforeReset == value;
                    final boolean scopeHint = "slider".contentEquals(key)
                            && ThemePrefs.INSTANCE.getSliderScope(this) == ThemePrefs.SLIDER_SCOPE_PICKER;
                    toast((unchanged ? s(R.string.color_editor_reset_already)
                                     : s(R.string.color_editor_reset_done))
                            + (scopeHint ? "\n" + s(R.string.color_editor_reset_scope_hint) : ""));
                    Diag.warn(this, "A10", "语义色重置（仅面板内） key=" + key
                            + " → " + hex(value)
                            + "（取自默认值）"
                            + (unchanged ? " 面板值未变（本来就是默认）" : "")
                            + (scopeHint ? " 作用范围=仅动图选择器" : "")
                            + " 等「保存」才落盘");
                });
                buttonRow.addView(hit, 0);
            }
        }

        // v1.10.145（反馈 3/4）：这个面板是独立 Dialog，不在设置页的轮询里 ——
        // 系统切到浅色/深色时它会一直停在打开那一刻的配色。这里自己起一个 500ms 的轮询，
        // 一发现深浅变了就整套重刷（面板底/文字/输入框/按钮/预览描边/光轮），
        // 同时把窗口的**状态栏和导航栏**也刷成主题底色（反馈 4）。
        final Runnable applyEditorTheme = () -> {
            ThemeColors c = ThemeManager.INSTANCE.colors(this);
            boolean dark = ThemeManager.INSTANCE.isDark(this);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(fontAlpha(c.getSurface(), dark ? 0.52f : 0.64f));
            bg.setCornerRadius(28.0f * density);
            bg.setStroke(Math.max(1, Math.round(density)), 0x47FFFFFF);
            root.setBackground(bg);
            title.setTextColor(c.getText());
            close.setTextColor(c.getText());
            hex.setTextColor(c.getText());
            hex.setHintTextColor(c.getTextMuted());
            GradientDrawable hexBg = new GradientDrawable();
            hexBg.setCornerRadius(14.0f * density);
            hexBg.setColor(fontAlpha(c.getNeutral(), 0.92f));
            hex.setBackground(hexBg);
            copy.setTextColor(c.getTextMuted());
            paste.setTextColor(c.getTextMuted());
            variantsLabel.setTextColor(c.getTextMuted());
            cancel.setBackground(ThemeManager.INSTANCE.outlined(this, fontAlpha(c.getNeutral(), 0.55f),
                    fontAlpha(c.getText(), 0.12f), 999.0f));
            cancel.setTextColor(c.getText());
            save.setBackground(ThemeManager.INSTANCE.rounded(this, fontAlpha(c.getPrimary(), 0.78f), 999.0f));
            save.setTextColor(c.getOnPrimary());
            // v1.10.463：两个自绘控件不在 Activity 的视图树里（弹窗独立窗口），
            // 面板开着切深浅色时要在这里重新下发一次，否则它们停在开面板那一刻的配色。
            if (modeRow != null) {
                modeRow.applyColors(c);
            }
            if (scopeRow != null) {
                scopeRow.applyColors(c);
            }
            // v1.10.205：重置胶囊是**自己建**的控件，不在 paint() 链里 ——
            // 面板开着切深浅色时，必须在这里一起重刷（否则就是 v1.10.201 那个"停在旧配色"的坑）
            if (withReset) {
                resetPill.setBackground(ThemeManager.INSTANCE.outlined(this,
                        fontAlpha(c.getNeutral(), 0.42f),
                        fontAlpha(c.getText(), 0.12f), 999.0f));
                resetGlyph.setTintColor(c.getText());
            }
            int[] live = new int[]{c.getPrimary(), c.getSecondary(), c.getTertiary()};
            aurora.setRingColors(new int[]{live[order[0]], live[order[1]], live[order[2]]});
            aurora.setBaseColor(c.getBackground());
            applyEditorSystemBars(this, dialog, dark, aurora.edgeColor(true), aurora.edgeColor(false));
        };
        applyEditorTheme.run();
        final android.os.Handler editorHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        final boolean[] editorDark = new boolean[]{ThemeManager.INSTANCE.isDark(this)};
        final Runnable[] editorWatch = new Runnable[1];
        editorWatch[0] = () -> {
            if (!dialog.isShowing()) {
                return;
            }
            boolean darkNow = ThemeManager.INSTANCE.isDark(this);
            if (darkNow != editorDark[0]) {
                editorDark[0] = darkNow;
                applyEditorTheme.run();
            }
            // 色雾在动：系统栏每 500ms 跟上一次，颜色始终贴着画面边缘
            applyEditorSystemBars(this, dialog, editorDark[0], aurora.edgeColor(true), aurora.edgeColor(false));
            editorHandler.postDelayed(editorWatch[0], 500L);
        };
        editorHandler.postDelayed(editorWatch[0], 500L);

        showColorEditor$render(preview, updatingHex, hex, density, colors, initial, true);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
        }
        Window window3 = dialog.getWindow();
        if (window3 != null) {
            // 压暗交给光轮自己（Z1 要的是「看得见背景」），不再盖一层系统 dim
            window3.setDimAmount(0.0f);
        }
        Window window5 = dialog.getWindow();
        if (window5 != null) {
            // v1.10.142：面板是悬浮卡片，键盘弹起时若 ADJUST_RESIZE 会把面板顶出屏幕
            // （内容比「屏高 - 键盘」高）。改成不顶起：键盘直接盖在下半部，
            // HEX 输入框在面板顶部，照样看得见；按下完成键会主动收起键盘。
            window5.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
            try {
                // 主题自带的窗口动画会和这里的滑入叠加，关掉只留一套
                window5.setWindowAnimations(0);
            } catch (Throwable ignored) {
            }
        }
        // 返回键也走滑出动画（默认会直接消失，面板和光轮会「啪」一下断掉）
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                closeColorEditor(dialog, sheet, aurora, null);
                return true;
            }
            return false;
        });
        dialog.setOnDismissListener((d) -> {
            // Z2：把退出的相位存下来，下次进来从这里继续（不是每次从头转）
            ColorAuroraView.savePhase(this, aurora.currentPhase());
            aurora.stop();
            editorHandler.removeCallbacks(editorWatch[0]);
            // 对话框窗口关掉后，系统栏颜色要还给设置页（有的 ROM 不会自动回退）
            this.colorEditorOpen = false;
            try {
                ThemeManager.INSTANCE.applySystemBars(this);
            } catch (Throwable ignored) {
            }
        });
        dialog.show();
        // show 之后再刷一次：窗口 attach 时系统会把系统栏的 UI flags 重置掉
        applyEditorSystemBars(this, dialog, ThemeManager.INSTANCE.isDark(this),
                aurora.edgeColor(true), aurora.edgeColor(false));
        hex.clearFocus();
        Window window6 = dialog.getWindow();
        if (window6 != null) {
            // v1.10.369（A12 读数驱动，实测 sdk=37）：
            //   decor=0,138 尺寸=1260x2662 屏=1260x2800 bars=0,0,0,138 fitTypes=0x107 fitSides=0xf
            //   → 窗口框被 fitInsets 按状态栏 138px 顶下去（高度也少了同样的 138），
            //     而底边一直到 2800 —— 所以「导航栏透了、状态栏没透」不是 ROM 特例，
            //     是同一个 displayFrame 只在上边被扣了 inset。
            //   平台侧：WindowLayout.computeFrames() 先用 attrs.getFitInsetsTypes() 收缩
            //   windowBounds，attachedFrame==null 的 Dialog 直接把收缩结果当 parentFrame，
            //   MATCH_PARENT 取的正是它 —— 所以只有这两个 setFitInsets* 能改这个框，
            //   IN_SCREEN / NO_LIMITS / setDecorFitsSystemWindows 都不行（字节码已逐行核对）。
            android.view.WindowManager.LayoutParams editorLp = window6.getAttributes();
            editorLp.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
            editorLp.height = android.view.WindowManager.LayoutParams.MATCH_PARENT;
            // NO_LIMITS 是干扰项：它在 parentFrame 拷好之后才把 displayFrame 炸成 ±100000
            editorLp.flags &= ~android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                editorLp.setFitInsetsTypes(0);
                editorLp.setFitInsetsSides(0);
                editorLp.layoutInDisplayCutoutMode =
                        android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
                window6.setDecorFitsSystemWindows(false);
            }
            window6.setAttributes(editorLp);
            // 主题里的 windowBackground 可能在 attach 时回填，show 之后再来一次
            window6.setBackgroundDrawable(new ColorDrawable(0));
        }
        // 小屏兜底：内容比 86% 屏高还高时，把面板钉到 86% 并让 ScrollView 接手滚动
        // （LinearLayout 里的 ScrollView 只有给了 weight 才会真的滚，wrap_content 时会顶破父容器）
        sheet.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                sheet.getViewTreeObserver().removeOnPreDrawListener(this);
                if (sheet.getHeight() > maxSheetHeight) {
                    ViewGroup.LayoutParams lp = sheet.getLayoutParams();
                    lp.height = maxSheetHeight;
                    sheet.setLayoutParams(lp);
                    for (int i = 0; i < root.getChildCount(); i++) {
                        View child = root.getChildAt(i);
                        if (child instanceof ScrollView) {
                            LinearLayout.LayoutParams slp = (LinearLayout.LayoutParams) child.getLayoutParams();
                            slp.height = 0;
                            slp.weight = 1.0f;
                            child.setLayoutParams(slp);
                        }
                    }
                    return false;
                }
                return true;
            }
        });
        // 入场（反馈 1 之后面板是悬浮卡片，不再是贴底 sheet）：光轮 260ms 淡入，
        // 卡片 300ms「缩放 0.94→1 + 上浮 26dp + 渐显」，与 ConfirmDialog 同一套手感
        aurora.setAlpha(0.0f);
        aurora.animate().alpha(1.0f).setDuration(260L).start();
        sheet.setAlpha(0.0f);
        sheet.setScaleX(0.94f);
        sheet.setScaleY(0.94f);
        sheet.setTranslationY(26.0f * density);
        sheet.post(() -> sheet.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f)
                .translationY(0.0f).setDuration(300L)
                .setInterpolator(EASE_OUT).start());
    }

    /**
     * v1.10.463：「预览虚线色」的保存 —— **档位 + 自定义色一次写**。
     *
     * <p>它和别的语义色不是一类：别的存的是「调色板里那个位置取什么值」，它存的是
     * 「取色口径」。所以只有「自定义」档才写颜色；其余档写颜色没有意义
     * （下次打开照样被壁纸 / 中性灰盖掉，用户会以为「保存没生效」）。</p>
     */
    private void saveOutlineColor(@org.jetbrains.annotations.NotNull EditText hex,
                                  @org.jetbrains.annotations.NotNull SegmentedRow modeRow,
                                  @org.jetbrains.annotations.NotNull Dialog dialog,
                                  @org.jetbrains.annotations.NotNull View sheet,
                                  @org.jetbrains.annotations.Nullable ColorAuroraView aurora) {
        Integer value = showColorEditor$parseHex(hex.getText().toString());
        if (value == null) {
            Toast.makeText(this, s(R.string.color_editor_invalid), 0).show();
            return;
        }
        final int mode = modeRow.getSelected();
        OutlineSettings.setModeIndex(this, mode);
        final boolean custom = mode == OutlineColor.Mode.CUSTOM.ordinal();
        if (custom) {
            OutlineSettings.setCustomRgb(this, value.intValue());
        }
        Diag.warn(this, "A13", "预览虚线色保存 档位=" + OutlineColor.Mode.values()[mode].name()
                + " 面板=" + hex(value.intValue())
                + (custom ? "（已写入 outline_custom）" : "（非自定义档，不写颜色）")
                + " 生效=" + OutlineColor.hex(OutlineSettings.currentColor(this)));
        closeColorEditor(dialog, sheet, aurora,
                () -> Morph.crossfade(this, () -> refreshThemeInPlace()));
    }

    /**
     * 调色面板收起来：**与打开完全镜像**（反馈 2/3）。
     *
     *   打开：卡片 alpha 0→1、缩放 0.94→1、从下方 26dp 上浮，300ms
     *   关闭：卡片 alpha 1→0、缩放 1→0.94、向下沉 26dp，220ms
     *
     * 上一版只让卡片淡出、光轮背景原地满亮，dismiss 那一刻整片紫色「啪」地消失，
     * 看上去就是「高斯背景残留了几帧」—— 现在背景跟着一起淡出，收尾才 dismiss。
     */
    private static final void closeColorEditor(final Dialog dialog, final View sheet,
                                               @Nullable final ColorAuroraView aurora,
                                               @Nullable final Runnable after) {
        if (sheet == null) {
            dismissQuiet(dialog);
            if (after != null) {
                after.run();
            }
            return;
        }
        // 连点「保存」「取消」只认第一次：动画跑起来就打个标记
        if (sheet.getTag() != null) {
            return;
        }
        if (sheet.getHeight() <= 0) {
            dismissQuiet(dialog);
            if (after != null) {
                after.run();
            }
            return;
        }
        sheet.setTag(Boolean.TRUE);
        float dp = sheet.getResources().getDisplayMetrics().density;
        // 背景（光轮 + 模糊）同步淡出：不留残影，也不会在 dismiss 时整片消失
        if (aurora != null) {
            aurora.animate().alpha(0.0f).setDuration(220L).start();
        }
        sheet.animate().alpha(0.0f).scaleX(0.94f).scaleY(0.94f)
                .translationY(26.0f * dp)
                .setDuration(220L)
                .setInterpolator(EASE_OUT)
                .withEndAction(() -> {
                    dismissQuiet(dialog);
                    if (after != null) {
                        after.run();
                    }
                }).start();
    }

    // ---------------------------------------------------------------- 调色窗边界诊断（v1.10.368）

    /** 上一次打过的几何串：只在几何变化时再打，避免每拍刷屏。 */
    private static String editorWinGeom;
    /** 已打过的拍数：前 10 拍连 top 色一起打（用来判定 ROM 认不认 setStatusBarColor）。 */
    private static int editorWinTicks;

    /**
     * v1.10.368（**只加指标，不改行为**）：把语义色编辑页那个 floating Dialog 的**真实边界**打出来。
     *
     * <p>要定死的三件事：
     * <ol>
     *   <li>窗口有没有覆盖状态栏区域（`decor` 的屏幕 y 与高度）；</li>
     *   <li>如果没覆盖，是不是 `fitInsetsTypes` 把父框收缩了（读 `LayoutParams` 的两位）；</li>
     *   <li>这台 ROM 还认不认 `setStatusBarColor`（前 10 拍的 `top=` 与同刻截图对比）。</li>
     * </ol>
     *
     * <p>判读（本机 1260×2800，状态栏约 100~130px）：
     * <pre>
     *   decor=0,0 尺寸=1260x2800                → 已覆盖；若仍有色带，问题在系统栏自绘/scrim，另案
     *   decor.y≈状态栏高 尺寸≈2800−状态栏高      → 窗口被 fitInsets 收缩（未覆盖）→ 改 setFitInsetsTypes(0)
     *   fitTypes=0x0 但 decor.y 仍≈状态栏高      → ROM 覆盖了 WMS 行为 → 只能换 content 覆盖层
     * </pre>
     */
    private static final void diagEditorWindow(Activity activity, Dialog dialog, int topColor) {
        try {
            Window w = dialog == null ? null : dialog.getWindow();
            if (w == null || activity == null) {
                return;
            }
            View decor = w.getDecorView();
            int[] loc = new int[2];
            decor.getLocationOnScreen(loc);
            DisplayMetrics dm = activity.getResources().getDisplayMetrics();
            String bars = "bars=?";
            android.view.WindowInsets root = decor.getRootWindowInsets();
            if (root != null) {
                android.graphics.Insets i = root.getInsets(android.view.WindowInsets.Type.systemBars());
                bars = "bars=" + i.left + "," + i.top + "," + i.right + "," + i.bottom;
            }
            android.view.WindowManager.LayoutParams lp = w.getAttributes();
            String fit;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                fit = "fitTypes=0x" + Integer.toHexString(lp.getFitInsetsTypes())
                        + " fitSides=0x" + Integer.toHexString(lp.getFitInsetsSides());
            } else {
                fit = "fitTypes=n/a";
            }
            String geom = "decor=" + loc[0] + "," + loc[1]
                    + " 尺寸=" + decor.getWidth() + "x" + decor.getHeight()
                    + " 屏=" + dm.widthPixels + "x" + dm.heightPixels
                    + " " + bars + " " + fit
                    + " flags=0x" + Integer.toHexString(lp.flags)
                    + " sdk=" + android.os.Build.VERSION.SDK_INT;
            boolean changed = !geom.equals(editorWinGeom);
            if (changed) {
                editorWinGeom = geom;
            }
            if (changed || editorWinTicks < 10) {
                editorWinTicks++;
                String msg = "调色窗 拍=" + editorWinTicks + " " + geom
                        + " top=#" + String.format(Locale.US, "%06X", topColor & 0x00FFFFFF);
                Diag.warn(activity, "A12", msg);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * v1.10.147（反馈 2）：状态栏 / 导航栏**跟着画面走**。
     *
     * 为什么不用透明：Dialog 是 floating 窗口，系统栏那块仍由系统自己绘制，
     * `setStatusBarColor(TRANSPARENT)` + edge-to-edge 在 ROM 上不一定听（上一版实测还是白条）。
     * 改成「把系统栏刷成紧挨着它的那条色雾的平均色」——颜色和画面边缘一致，
     * 视觉上就是接上的，而且完全不依赖 ROM 支持透明系统栏。
     * 色雾一直在动，所以调用方每 500ms 刷一次。
     *
     * 图标明暗仍按深浅模式走（浅色背景配深色图标），这个不跟着取样色变，避免闪烁。
     */
    private static final void applyEditorSystemBars(Activity activity, Dialog dialog, boolean dark,
                                                    int topColor, int bottomColor) {
        // v1.10.368：只加指标，不改行为 —— 先把「窗口到底覆盖没覆盖状态栏」量出来
        diagEditorWindow(activity, dialog, topColor);
        // v1.10.150（反馈 2）：系统栏颜色真正读的是**最上层 Activity 窗口**的属性，
        // floating Dialog 的设置在这台机器的 ROM 上会被忽略（实测：导航栏透了、状态栏没透）。
        // 所以两个窗口都设：Activity 决定系统栏颜色，Dialog 负责内容铺到系统栏下面。
        Window activityWindow = activity == null ? null : activity.getWindow();
        if (activityWindow != null) {
            try {
                activityWindow.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                // v1.10.152（反馈 2）：状态栏**只能**由 Activity 窗口定色（dialog 是 floating，
                // 设成透明后露出的是本页 decor 底色，仍然是纯色条）。所以直接把系统栏刷成
                // 画面**顶部那条色雾的平均色**，并且每 500ms 跟着色雾更新 —— 视觉上就连成一片。
                activityWindow.setStatusBarColor(topColor);
                activityWindow.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    activityWindow.setNavigationBarContrastEnforced(false);
                }
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    android.view.WindowInsetsController ac = activityWindow.getInsetsController();
                    if (ac != null) {
                        int light = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                        ac.setSystemBarsAppearance(dark ? 0 : light, light);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        try {
            // v1.10.149 的老判断（「NO_LIMITS 是让窗口画到系统栏区域的关键」）**已被证伪**：
            // v1.10.369 的 A12 读数（decor=0,138 / fitTypes=0x107）+ AOSP API 34 字节码显示，
            // 决定窗口框的是 fitInsetsTypes/Sides，NO_LIMITS 在 parentFrame 拷好之后才生效。
            // 真正的修法在 showColorEditor 的 editorLp 里（setFitInsetsTypes(0)）。
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                    | android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
            window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                // 否则系统会在浅色导航栏上再压一层灰，和画面接不上
                window.setNavigationBarContrastEnforced(false);
            }
            // v1.10.148（反馈 1）：两条通道**都设**。
            // 上一版只在 API 30+ 用 WindowInsetsController —— 在 Dialog 窗口上它常常拿不到
            // （getInsetsController() 返回 null），于是浅色模式下导航栏图标还是白的、几乎看不见。
            // 现在旧的 setSystemUiVisibility 也一起设，哪个生效都行。
            View decor = window.getDecorView();
            // 布局延伸到系统栏下 + 图标明暗，一次设全（26~29 也走这条）
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if (!dark) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decor.setSystemUiVisibility(flags);
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                window.setDecorFitsSystemWindows(false);
                android.view.WindowInsetsController controller = window.getInsetsController();
                if (controller != null) {
                    int light = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                    controller.setSystemBarsAppearance(dark ? 0 : light, light);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static final void dismissQuiet(Dialog dialog) {
        try {
            dialog.dismiss();
        } catch (Throwable ignored) {
        }
    }

    private static final void showColorEditor$lambda$37(Dialog $dialog, View it) {
        Intrinsics.checkNotNullParameter($dialog, "$dialog");
        $dialog.dismiss();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void showColorEditor$render(View preview, Ref.BooleanRef updatingHex, EditText hex, float density, ThemeColors colors, int value, boolean updateHex) {
        GradientDrawable $this$showColorEditor_u24render_u24lambda_u2440 = new GradientDrawable();
        $this$showColorEditor_u24render_u24lambda_u2440.setShape(0);
        $this$showColorEditor_u24render_u24lambda_u2440.setCornerRadius(16.0f * density);
        $this$showColorEditor_u24render_u24lambda_u2440.setColor(value);
        // v1.10.145（反馈 3）：预览块的描边按**实时**主题取色，深浅色切换后不会停在旧颜色上
        ThemeColors liveColors = ThemeManager.INSTANCE.colors(preview.getContext());
        $this$showColorEditor_u24render_u24lambda_u2440.setStroke(Math.max(1, Math.round(density)),
                ThemeManager.INSTANCE.withAlpha(liveColors.getText(), 0.20f));
        preview.setBackground($this$showColorEditor_u24render_u24lambda_u2440);
        if (updateHex && !updatingHex.element) {
            updatingHex.element = true;
            Object[] objArr = {Integer.valueOf(16777215 & value)};
            String str = String.format("#%06X", Arrays.copyOf(objArr, objArr.length));
            Intrinsics.checkNotNullExpressionValue(str, "format(...)");
            hex.setText(str);
            hex.setSelection(hex.getText().length());
            updatingHex.element = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Integer showColorEditor$parseHex(String raw) {
        Integer numValueOf;
        String text = StringsKt.removePrefix(StringsKt.trim(raw).toString(), "#");
        if (text.length() != 6) {
            return null;
        }
        try {
            numValueOf = Integer.valueOf((int) (4278190080L | Long.parseLong(text, CharsKt.checkRadix(16))));
        } catch (Throwable th) {
            numValueOf = null;
        }
        return numValueOf;
    }

    /* compiled from: SettingsActivity.kt */
    @Metadata(mv = {1, 9, 0}, k = 3, xi = 48, d1 = {"��\u000e\n��\n\u0002\u0010\u0002\n��\n\u0002\u0010\b\n��\u0010��\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\n¢\u0006\u0002\b\u0004"}, d2 = {"<anonymous>", "", "value", "", "invoke"})
    /* renamed from: io.github.animwidget.SettingsActivity$showColorEditor$4, reason: invalid class name and case insensitive filesystem */
    /* loaded from: classes.jar:io/github/animwidget/SettingsActivity$showColorEditor$4.class */
    final class C00014 implements Function1<Integer, Unit> {
        final /* synthetic */ View $preview;
        final /* synthetic */ Ref.BooleanRef $updatingHex;
        final /* synthetic */ EditText $hex;
        final /* synthetic */ float $density;
        final /* synthetic */ ThemeColors $colors;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        C00014(View $preview, Ref.BooleanRef $updatingHex, EditText $hex, float $density, ThemeColors $colors) {
            this.$preview = $preview;
            this.$updatingHex = $updatingHex;
            this.$hex = $hex;
            this.$density = $density;
            this.$colors = $colors;
        }

        public final Unit invoke(Integer value) {
            SettingsActivity.showColorEditor$render(this.$preview, this.$updatingHex, this.$hex, this.$density, this.$colors, value.intValue(), true);
            return Unit.INSTANCE;
        }
    }

    private static final boolean showColorEditor$lambda$42(EditText $hex, HsvPickerView $picker, TextView textView, int i, KeyEvent keyEvent) {
        Intrinsics.checkNotNullParameter($picker, "$picker");
        Integer numShowColorEditor$parseHex = showColorEditor$parseHex($hex.getText().toString());
        if (numShowColorEditor$parseHex == null) {
            return true;
        }
        int it = numShowColorEditor$parseHex.intValue();
        $picker.setColor(it);
        // v1.10.142：软键盘不再顶起窗口，所以「完成」时自己收起来
        try {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) $hex.getContext()
                            .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow($hex.getWindowToken(), 0);
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    private static final void showColorEditor$lambda$43(HsvPickerView $picker, SettingsActivity this$0, View it) {
        Intrinsics.checkNotNullParameter($picker, "$picker");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        int value = $picker.getColor();
        Object[] objArr = {Integer.valueOf(16777215 & value)};
        String text = String.format("#%06X", Arrays.copyOf(objArr, objArr.length));
        Intrinsics.checkNotNullExpressionValue(text, "format(...)");
        ClipboardManager clipboardManager = (ClipboardManager) this$0.getSystemService(ClipboardManager.class);
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("color", text));
        }
        Toast.makeText(this$0, this$0.s(R.string.color_editor_copied, new Object[]{text}), 0).show();
    }

    private static final void showColorEditor$lambda$44(SettingsActivity this$0, HsvPickerView $picker, View $preview, Ref.BooleanRef $updatingHex, EditText $hex, float $density, ThemeColors $colors, View it) {
        ClipData primaryClip;
        ClipData.Item itemAt;
        CharSequence charSequenceCoerceToText;
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter($picker, "$picker");
        Intrinsics.checkNotNullParameter($updatingHex, "$updatingHex");
        Intrinsics.checkNotNullParameter($colors, "$colors");
        ClipboardManager clipboardManager = (ClipboardManager) this$0.getSystemService(ClipboardManager.class);
        String text = (clipboardManager == null || (primaryClip = clipboardManager.getPrimaryClip()) == null || (itemAt = primaryClip.getItemAt(0)) == null || (charSequenceCoerceToText = itemAt.coerceToText(this$0)) == null) ? null : charSequenceCoerceToText.toString();
        String str = text;
        if (str == null) {
            str = "";
        }
        Integer numShowColorEditor$parseHex = showColorEditor$parseHex(str);
        if (numShowColorEditor$parseHex != null) {
            int value = numShowColorEditor$parseHex.intValue();
            $picker.setColor(value);
            showColorEditor$render($preview, $updatingHex, $hex, $density, $colors, value, true);
        }
    }

    private static final void showColorEditor$lambda$49$lambda$48$lambda$47(HsvPickerView $picker, int $value, View $preview, Ref.BooleanRef $updatingHex, EditText $hex, float $density, ThemeColors $colors, View it) {
        Intrinsics.checkNotNullParameter($picker, "$picker");
        Intrinsics.checkNotNullParameter($updatingHex, "$updatingHex");
        Intrinsics.checkNotNullParameter($colors, "$colors");
        $picker.setColor($value);
        showColorEditor$render($preview, $updatingHex, $hex, $density, $colors, $value, true);
    }

    private static final void showColorEditor$lambda$50(Dialog $dialog, View it) {
        Intrinsics.checkNotNullParameter($dialog, "$dialog");
        $dialog.dismiss();
    }

    private static final void showColorEditor$lambda$51(EditText $hex, SettingsActivity this$0, String $key, Dialog $dialog, View $sheet, ColorAuroraView $aurora, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter($key, "$key");
        Intrinsics.checkNotNullParameter($dialog, "$dialog");
        Integer value = showColorEditor$parseHex($hex.getText().toString());
        if (value == null) {
            Toast.makeText(this$0, this$0.s(R.string.color_editor_invalid), 0).show();
            return;
        }
        // v1.10.207（反馈：重置后点「取消」居然也生效了 —— 对话框语义必须是「保存才落盘」）：
        // 「重置」只改面板里的值，**落盘统一发生在这一处**。这里用一条更结实的判据决定怎么写：
        //   · 非底色键 + 值正好等于「没有覆盖值时的默认色」→ 删掉覆盖值（= 回到默认）；
        //   · 否则照旧写入。
        // 为什么不是"面板里记一个 resetArmed 标志"：那需要监听取色器/十六进制/变体圆点/粘贴
        // 四条输入路径去清标志，漏一条就会把"重置后又调过色"存成默认值。判据放在值上最稳。
        // 底色（表面/中性变体）**不走这条判断**：它们的"默认"按模式各一份，
        // 在浅色下选到浅色默认值就顺手把深色那份也清掉，属于误伤。
        boolean clearToDefault = !ThemePrefs.isBaseKey($key)
                && value.intValue() == ThemeManager.INSTANCE.defaultValue(this$0, $key);
        // v1.10.210（反馈「选完保存颜色会变一点，保存几次漂几次」）：
        // 面板里选的是**显示色**（已应用 invert + 对比度），存储要的是**原始色**。
        // 原来把显示色直接存进去 → 每保存一次就被 adjust 再处理一次 → 越存越亮/越暗。
        // 现在先反解：现有覆盖值显示出来就是选的色 → 原样保留（不动就不漂）；
        // 否则在 0..255 里找正向结果最接近的原始值。
        int raw = ThemeManager.INSTANCE.rawFromDisplay(this$0, $key, value.intValue());
        if (ThemePrefs.isBaseKey($key)) {
            ThemePrefs.INSTANCE.applyBaseEdit(this$0, $key, raw,
                    ThemeManager.INSTANCE.isDark(this$0));
        } else if (clearToDefault) {
            ThemePrefs.INSTANCE.resetKey(this$0, $key);
        } else {
            ThemePrefs.INSTANCE.setCustom(this$0, $key, raw);
        }
        // v1.10.199（取证，不是修复）：用户反馈「语义色保存无反应」。
        // 这一行把「写进去的值 / 立刻读回的值 / 当前解析出来的五个色」一次打全，
        // 用来区分是「数据没写进去」还是「写进去了但界面没刷」—— 不许靠猜。
        try {
            int back = ThemePrefs.INSTANCE.getCustom(this$0, $key);
            ThemeColors now = ThemeManager.INSTANCE.colors(this$0);
            Diag.warn(this$0, "A10", "语义色保存 key=" + $key
                    + " 面板=" + String.format(java.util.Locale.US, "#%06X", value.intValue() & 0xFFFFFF)
                    + " 存=" + String.format(java.util.Locale.US, "#%06X", raw & 0xFFFFFF)
                    + (clearToDefault ? "（值=默认 → 已删除覆盖值）" : "")
                    + " 读回=" + String.format(java.util.Locale.US, "#%06X", back & 0xFFFFFF)
                    + " 现值 primary=" + String.format(java.util.Locale.US, "#%06X", now.getPrimary() & 0xFFFFFF)
                    + " secondary=" + String.format(java.util.Locale.US, "#%06X", now.getSecondary() & 0xFFFFFF)
                    + " tertiary=" + String.format(java.util.Locale.US, "#%06X", now.getTertiary() & 0xFFFFFF)
                    + " neutral=" + String.format(java.util.Locale.US, "#%06X", now.getNeutral() & 0xFFFFFF)
                    + " surface=" + String.format(java.util.Locale.US, "#%06X", now.getSurface() & 0xFFFFFF)
                    + " error=" + String.format(java.util.Locale.US, "#%06X", now.getError() & 0xFFFFFF)
                    // 底色是推出来的（错误色 16% 压在表面上），一起打出来便于核对
                    // 「浅色下淡粉、深色下暗酒红，永远是一层底，不会和错误色互换」
                    + " error_box=" + String.format(java.util.Locale.US, "#%06X", now.getErrorContainer() & 0xFFFFFF));
        } catch (Throwable ignored) {
        }
        // v1.10.92：改了颜色**不再取消勾选**（v1.10.47 的老行为）——
        // 取消勾选就没法把这次修改存回那个方案（用户反馈「已保存方案不能被编辑」）。
        // 现在方案保持选中并标记为「已修改」，长按它点「更新」即可覆盖保存。
        // v1.10.167（反馈）：语义色「保存」后原来是**瞬间**刷新 —— 页面、图标、勾选标记
        // 一起硬跳，和「切换配色方案」那条叠化路径不一致。
        // 这里补上整页叠化（Morph：先截图 → 改数据 → 旧图 300ms 淡出）。
        // 时序是安全的：closeColorEditor 的回调在**关闭动画结束、Dialog 已 dismiss 之后**才执行，
        // 所以截图截到的是「面板已消失的页面旧配色」，不会把面板残影一起叠出来。
        closeColorEditor($dialog, $sheet, $aurora,
                () -> Morph.crossfade(this$0, () -> this$0.refreshThemeInPlace()));
    }
}
