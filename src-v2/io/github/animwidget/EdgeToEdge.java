package io.github.animwidget;

import android.app.Activity;
import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;

import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 全屏沉浸：SDK 35+ 强制 edge-to-edge。
 * 组件配置页的预览要顶到屏幕最上沿（没有那条白色横条），所以它不吃 top inset；
 * 主界面 / 设置页仍然留出状态栏高度。
 */
public final class EdgeToEdge {

    public static final EdgeToEdge INSTANCE = new EdgeToEdge();

    private EdgeToEdge() {
    }

    /**
     * v1.10.98：**整屏沉浸**（开屏 / 开发者页用）—— 内容铺满整块屏幕，
     * 状态栏和导航栏区域不吃 inset，背景才能真的流动到屏幕边缘。
     * 文字安全区由布局自己的 padding 负责。
     */
    public final void applyImmersive(@NotNull Activity activity) {
        Intrinsics.checkNotNullParameter(activity, "activity");
        if (Build.VERSION.SDK_INT < 35) {
            return;
        }
        Window window = activity.getWindow();
        window.setDecorFitsSystemWindows(false);
        View root = activity.findViewById(android.R.id.content);
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                view.setPadding(0, 0, 0, 0);
                return insets;
            }
        });
    }

    /** 只记一次的基础值（内边距 / margin）：insets 每次派发都改，不记住基准会一路累加。 */
    private static final java.util.WeakHashMap<View, int[]> BASE = new java.util.WeakHashMap<>();

    /**
     * v1.10.393：已经声明「内容铺到系统栏下面」的页面。
     *
     * <p>{@code ThemeManager.applySystemBars} 每次换肤都会重刷系统栏颜色 —— 它必须知道
     * 当前这页是不是真的铺到了屏幕边缘：铺了就把栏色刷成**透明**，没铺就刷成页面底色。
     * 这张表就是唯一的判据（WeakHashMap，页面销毁自动失效）。</p>
     */
    private static final java.util.WeakHashMap<Activity, Boolean> UNDERS = new java.util.WeakHashMap<>();

    /** 这一页是不是「内容铺到系统栏下面」（透明系统栏的前提）。 */
    public final boolean isUnderBars(@NotNull Activity activity) {
        return UNDERS.containsKey(activity);
    }

    /**
     * v1.10.370（用户要求「设置页面顶部也给全透明」）：**内容从屏幕最上沿开始滚**，
     * 系统栏 inset 从 content 挪到「滚动内容的 padding + 浮动控件的 margin」上。
     *
     * <p>为什么不能只把状态栏刷成透明：content 的顶部 padding 一旦留着，滚上去的卡片
     * 会在状态栏下沿**被切平**（截图里「系统模式」卡的上边缘是直角，不是圆角），
     * 状态栏那片永远只有窗口底色。要「内容从状态栏下面穿过去」，必须两件事一起做：
     * <ol>
     *   <li>content 顶部不占位（本方法）；</li>
     *   <li>状态栏色透明（{@code ThemeManager.applySystemBars} 里按页面分支）。</li>
     * </ol>
     *
     * @param scrollViewId 要铺到屏幕顶的 ScrollView
     * @param liftIds      原本靠 content 顶边定位的浮动控件（如设置页左上角「关闭」胶囊）——
     *                     顶边 inset 从 content 挪走后，把它们的 marginTop 补回来，位置才不变
     */
    public final void applyScrollUnderBars(@NotNull Activity activity, final int scrollViewId,
                                           final int... liftIds) {
        Intrinsics.checkNotNullParameter(activity, "activity");
        // v1.10.400：判据从「35+」放宽到「30+」—— 30 起 setDecorFitsSystemWindows + 栏色透明
        // 就足以把内容铺到两条系统栏下面（首页那条 applyUnderBars 已经是这个口径）。
        if (Build.VERSION.SDK_INT < 30) {
            return;
        }
        Window window = activity.getWindow();
        window.setDecorFitsSystemWindows(false);
        UNDERS.put(activity, Boolean.TRUE);
        final View root = activity.findViewById(android.R.id.content);
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                Intrinsics.checkNotNullParameter(view, "view");
                Intrinsics.checkNotNullParameter(insets, "insets");
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                // v1.10.400：**上下都不占位** —— 内容从 y=0 铺到屏幕最下沿，滚动时从状态栏
                // 和导航栏下面穿过去；inset 挪到「滚动内容的 padding」与「浮动控件的 margin」上。
                // 之前底部是让位的（padding=bars.bottom），于是导航栏那条永远是窗口底色 ——
                // 看着不透明，其实只是内容没铺过去。
                view.setPadding(bars.left, 0, bars.right, 0);

                View scroll = view.findViewById(scrollViewId);
                if (scroll instanceof android.widget.ScrollView && ((android.view.ViewGroup) scroll).getChildCount() > 0) {
                    View inner = ((android.view.ViewGroup) scroll).getChildAt(0);
                    int[] base = BASE.get(scroll);
                    if (base == null || base.length < 2) {
                        base = new int[]{inner.getPaddingTop(), inner.getPaddingBottom()};
                        BASE.put(scroll, base);
                    }
                    int wantTop = base[0] + bars.top;
                    int wantBottom = base[1] + bars.bottom;
                    if (inner.getPaddingTop() != wantTop || inner.getPaddingBottom() != wantBottom) {
                        inner.setPadding(inner.getPaddingLeft(), wantTop,
                                inner.getPaddingRight(), wantBottom);
                    }
                }

                for (int i = 0; i < liftIds.length; i++) {
                    View lift = view.findViewById(liftIds[i]);
                    if (lift == null) {
                        continue;
                    }
                    android.view.ViewGroup.LayoutParams lp = lift.getLayoutParams();
                    if (!(lp instanceof android.widget.FrameLayout.LayoutParams)) {
                        continue;
                    }
                    android.widget.FrameLayout.LayoutParams flp = (android.widget.FrameLayout.LayoutParams) lp;
                    int[] base = BASE.get(lift);
                    if (base == null || base.length < 2) {
                        base = new int[]{flp.topMargin, flp.bottomMargin};
                        BASE.put(lift, base);
                    }
                    // v1.10.400：底部贴边的浮动控件（设置页的方案动作条）要按 bottomMargin 抬，
                    // 否则底部 inset 从 content 挪走之后它会钻进手势条里。
                    boolean bottomAnchored = (flp.gravity & android.view.Gravity.BOTTOM) == android.view.Gravity.BOTTOM;
                    if (bottomAnchored) {
                        int want = base[1] + bars.bottom;
                        if (flp.bottomMargin != want) {
                            flp.bottomMargin = want;
                            lift.setLayoutParams(flp);
                        }
                    } else {
                        int want = base[0] + bars.top;
                        if (flp.topMargin != want) {
                            flp.topMargin = want;
                            lift.setLayoutParams(flp);
                        }
                    }
                }
                return insets;
            }
        });
    }

    /**
     * v1.10.393：主页的「真全屏」——内容从状态栏下面开始滚，系统栏刷透明。
     *
     * <p>和设置页那条（{@link #applyScrollUnderBars}）的区别只有两点：
     * <ul>
     *   <li>顶部 inset 挪到**滚动内容**的 paddingTop，底部 inset 挪到 paddingBottom
     *       —— 主页没有浮动控件（没有 liftIds），一屏内容滚到哪算哪；</li>
     *   <li>不要求 SDK≥35：30 起 {@code setDecorFitsSystemWindows(false)} 就能做到，
     *       26..29 交给系统默认（内容照样会被系统栏顶下去，不会钻到栏下面）。</li>
     * </ul>
     *
     * <p>为什么必须两件事一起做（跟设置页那条同一个原因）：只把系统栏刷透明而不铺到顶，
     * 状态栏那片就只剩窗口底色；只铺到顶而栏色不透明，内容会被那条带子盖住。</p>
     */
    public final void applyUnderBars(@NotNull Activity activity, final int scrollViewId) {
        Intrinsics.checkNotNullParameter(activity, "activity");
        if (Build.VERSION.SDK_INT < 30) {
            return;
        }
        Window window = activity.getWindow();
        window.setDecorFitsSystemWindows(false);
        UNDERS.put(activity, Boolean.TRUE);
        final View root = activity.findViewById(android.R.id.content);
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                Intrinsics.checkNotNullParameter(view, "view");
                Intrinsics.checkNotNullParameter(insets, "insets");
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                // 根部：上下都不占位（内容能画到 y=0 和屏幕最下沿），左右让位
                view.setPadding(bars.left, 0, bars.right, 0);

                View scroll = view.findViewById(scrollViewId);
                if (scroll instanceof android.widget.ScrollView
                        && ((android.view.ViewGroup) scroll).getChildCount() > 0) {
                    View inner = ((android.view.ViewGroup) scroll).getChildAt(0);
                    int[] base = BASE.get(scroll);
                    if (base == null) {
                        base = new int[]{inner.getPaddingTop(), inner.getPaddingBottom()};
                        BASE.put(scroll, base);
                    }
                    int top = base[0] + bars.top;
                    int bottom = base[1] + bars.bottom;
                    if (inner.getPaddingTop() != top || inner.getPaddingBottom() != bottom) {
                        inner.setPadding(inner.getPaddingLeft(), top, inner.getPaddingRight(), bottom);
                    }
                }
                return insets;
            }
        });
    }

    /**
     * v1.10.437：**整页铺到系统栏下面 + 两条栏刷透明**（日志页用；用户："导航栏改为透明"）。
     *
     * <p>和 {@link #applyUnderBars} 的区别：那一条要求页面里有一个 ScrollView，inset 直接加到
     * ScrollView 内层的 padding 上；日志页没有 ScrollView（正文是自绘控制台，自己管滚动），
     * 所以 inset 拆成两半用：
     * <ul>
     *   <li>**上下** 给页面（{@code page}）—— 标题不钻状态栏、控制台不钻导航栏；</li>
     *   <li>**左右** 给文字列（{@code contentColumn}）—— 竖屏锁死时正常就是 0，
     *       只有挖孔/刘海落在左边时才有值；这样**控制台本身可以铺满整幅宽度**，
     *       右侧滑块才真的贴在屏幕右边缘（给控制台留白就等于把滑块往里推）。</li>
     * </ul>
     *
     * <p>为什么透明栏必须配「内容铺到边」：栏透明之后那块地方露出的是窗口/页面底色，
     * 内容没铺过去就只剩一条色带（看起来跟不透明一模一样，白改）。反过来只铺内容不透明，
     * 内容会被栏挡住。两件事必须一起做。
     */
    public final void applyPageUnderBars(@NotNull Activity activity, @Nullable View page,
                                         @Nullable View contentColumn) {
        Intrinsics.checkNotNullParameter(activity, "activity");
        // 30 起 setDecorFitsSystemWindows(false) 才能自己接管 inset；以下交给系统默认。
        if (Build.VERSION.SDK_INT < 30) {
            return;
        }
        Window window = activity.getWindow();
        window.setDecorFitsSystemWindows(false);
        UNDERS.put(activity, Boolean.TRUE);
        final View pageView = page;
        final View columnView = contentColumn;
        final View root = activity.findViewById(android.R.id.content);
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                Intrinsics.checkNotNullParameter(view, "view");
                Intrinsics.checkNotNullParameter(insets, "insets");
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                // 根部：左右也不占位（左右 inset 挪给文字列），这样控制台能铺到屏幕物理边缘
                view.setPadding(0, 0, 0, 0);

                if (pageView != null) {
                    int[] base = BASE.get(pageView);
                    if (base == null || base.length < 2) {
                        base = new int[]{pageView.getPaddingTop(), pageView.getPaddingBottom()};
                        BASE.put(pageView, base);
                    }
                    int wantTop = base[0] + bars.top;
                    // v1.10.438：**底部不给页面**。控制台要一直铺到屏幕最下沿（用户："文本扩展到
                    // 导航栏部分"），导航栏那一条由控制台自己的 paddingBottom 让位（它自己收 inset）。
                    int wantBottom = base[1];
                    if (pageView.getPaddingTop() != wantTop || pageView.getPaddingBottom() != wantBottom) {
                        pageView.setPadding(pageView.getPaddingLeft(), wantTop,
                                pageView.getPaddingRight(), wantBottom);
                    }
                }

                if (columnView != null) {
                    int[] base = BASE.get(columnView);
                    if (base == null || base.length < 2) {
                        base = new int[]{columnView.getPaddingLeft(), columnView.getPaddingRight()};
                        BASE.put(columnView, base);
                    }
                    int wantLeft = base[0] + bars.left;
                    int wantRight = base[1] + bars.right;
                    if (columnView.getPaddingLeft() != wantLeft || columnView.getPaddingRight() != wantRight) {
                        columnView.setPadding(wantLeft, columnView.getPaddingTop(),
                                wantRight, columnView.getPaddingBottom());
                    }
                }
                return insets;
            }
        });
    }

    public final void apply(@NotNull Activity activity) {
        Intrinsics.checkNotNullParameter(activity, "activity");
        if (Build.VERSION.SDK_INT < 35) {
            return;
        }
        Window window = activity.getWindow();
        window.setDecorFitsSystemWindows(false);
        View root = activity.findViewById(android.R.id.content);
        final boolean ignoreTop = activity instanceof ConfigActivity;
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                Intrinsics.checkNotNullParameter(view, "view");
                Intrinsics.checkNotNullParameter(insets, "insets");
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                Intrinsics.checkNotNullExpressionValue(bars, "getInsets(...)");
                view.setPadding(bars.left, ignoreTop ? 0 : bars.top, bars.right, bars.bottom);
                return insets;
            }
        });
    }
}
