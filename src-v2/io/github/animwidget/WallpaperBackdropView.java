package io.github.animwidget;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowMetrics;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * v1.10.519：组件编辑页顶部的**桌面壁纸底图**，按「整块显示区」对位。
 *
 * <p>取代原来的 {@code ImageView scaleType=centerCrop}。那个写法有两个结构性错误，
 * 都不是「调一调参数」能修的：</p>
 *
 * <ol>
 *   <li><b>参照系错了</b>：centerCrop 以**预览控件自己**（1260×738）为框，壁纸在位图里垂直居中裁剪，
 *       而桌面是从屏幕 y=0 开始铺 —— 于是整块壁纸被上移 {@code (bh-738)/2} 像素。
 *       现在先按 cover 铺满**整块显示区**，再把「本 View 在显示区里的那块矩形」切出来
 *       （数学全在 {@link WallpaperPlacement}，脱机可对拍）。</li>
 *   <li><b>只在 onCreate 之后取一次</b>：用户换完壁纸回到本页，看到的还是旧图
 *       （{@code WallpaperManager} 没有可靠的变更回调）。现在按
 *       {@code getWallpaperId(FLAG_SYSTEM)} 判代号：换过就**先撤掉旧图**（判据 B：1 帧内不再显示旧壁纸）
 *       再去后台重取，没换就一行活都不干。</li>
 * </ol>
 *
 * <p>三条硬约束（都是这台机器 / 这个工程特有的）：</p>
 * <ul>
 *   <li>{@code minSdk 26}：{@code getMaximumWindowMetrics()} 是 API 30+，26~29 必须回退
 *       {@code Display.getRealSize()}，否则老系统直接 {@code NoSuchMethodError}；</li>
 *   <li>**不在 UI 线程解整张壁纸**（1260×2800 ARGB ≈ 14MB）：主线程取到的只是「已经解好的位图引用」，
 *       解码在单线程池里做；</li>
 *   <li>**绝不 recycle** 系统给的位图（{@code BitmapDrawable.getBitmap()} 可能是 WallpaperManager
 *       自己缓存的实例，回收它会把别处的取色/壁纸链路打断）。</li>
 * </ul>
 */
public final class WallpaperBackdropView extends View {

    /** 位图取到（或取不到）时回调一次；参数为 null = 这次没读到壁纸。 */
    public interface OnWallpaperListener {
        void onWallpaper(@Nullable Bitmap bitmap);
    }

    /**
     * 解码线程：单线程 + 守护线程。壁纸解码一次 12~14MB，
     * 并发没有意义（同一时刻只可能有一张壁纸），单线程还能保证「后发起的不会先到」。
     */
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "wallpaper-backdrop");
        thread.setDaemon(true);
        return thread;
    });

    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);

    private final Rect src = new Rect();

    private final Rect dst = new Rect();

    /** slice() 的输出缓冲（8 个整数），复用以避免每帧分配。 */
    private final int[] slice = new int[8];

    /** getLocationOnScreen 的输出缓冲。 */
    private final int[] where = new int[2];

    @Nullable
    private Bitmap bitmap;

    /** 幕上这张位图对应的壁纸代号；{@link Integer#MIN_VALUE} = 还没有。 */
    private int shownId = Integer.MIN_VALUE;

    /** 正在加载的代号（同一代不重复发起）。 */
    private int loadingId = Integer.MIN_VALUE;

    private boolean loading;

    /** 每次 reload 自增：迟到结果回来时对不上就丢弃（不 recycle，见类注释）。 */
    private int generation;

    @Nullable
    private OnWallpaperListener listener;

    public WallpaperBackdropView(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public WallpaperBackdropView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    public WallpaperBackdropView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
    }

    public final void setOnWallpaperListener(@Nullable OnWallpaperListener next) {
        this.listener = next;
    }

    /** 幕上正在显示的那张（可能为 null：壁纸读不到时露出父布局的兜底底色）。 */
    @Nullable
    public final Bitmap currentBitmap() {
        Bitmap bmp = this.bitmap;
        return bmp == null || bmp.isRecycled() ? null : bmp;
    }

    /**
     * 让本视图跟上「当前壁纸」。
     *
     * <p>幂等：代号没变就直接返回（切页面、回来、换肤都不会白白解一次 14MB）。</p>
     */
    public final void reload() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        final Context app = context.getApplicationContext();
        final int id = wallpaperId(app);
        boolean changed = id != this.shownId;
        if (!changed && this.bitmap != null) {
            return;
        }
        if (changed && this.bitmap != null) {
            // 判据 B：换过壁纸，这一帧就不许再看到旧壁纸 —— 先撤图（父布局底色顶上），再去取新的。
            this.bitmap = null;
            this.shownId = Integer.MIN_VALUE;
            invalidate();
        }
        if (this.loading && id == this.loadingId) {
            return;
        }
        this.loading = true;
        this.loadingId = id;
        final int gen = ++this.generation;
        POOL.execute(() -> {
            Bitmap loaded = null;
            String why = null;
            try {
                loaded = loadBitmap(app);
                if (loaded == null) {
                    why = "取到空图";
                }
            } catch (Throwable th) {
                why = th.getClass().getSimpleName();
            }
            final Bitmap result = loaded;
            final String reason = why;
            post(() -> deliver(gen, id, result, reason));
        });
    }

    private void deliver(int gen, int id, @Nullable Bitmap result, @Nullable String reason) {
        if (gen != this.generation) {
            // 迟到的旧结果：丢弃。**不 recycle** —— 可能是系统缓存的那张。
            return;
        }
        this.loading = false;
        if (result == null || result.isRecycled() || result.getWidth() <= 0 || result.getHeight() <= 0) {
            Diag.warn(getContext(), "W56", "壁纸底图 读不到 原因=" + (reason == null ? "空图" : reason)
                    + " 代号=" + id);
            invalidate();
            return;
        }
        this.bitmap = result;
        this.shownId = id;
        invalidate();
        logPlacement(result, id);
        OnWallpaperListener target = this.listener;
        if (target != null) {
            target.onWallpaper(result);
        }
    }

    private void logPlacement(Bitmap bmp, int id) {
        try {
            int[] disp = displaySize(getContext());
            getLocationOnScreen(this.where);
            double scale = WallpaperPlacement.coverScale(bmp.getWidth(), bmp.getHeight(), disp[0], disp[1]);
            Diag.warn(getContext(), "W56", "壁纸底图 位图=" + bmp.getWidth() + "x" + bmp.getHeight()
                    + " 显示区=" + disp[0] + "x" + disp[1]
                    + " 缩放=" + String.format(java.util.Locale.US, "%.3f", scale)
                    + " 图左上=" + WallpaperPlacement.round(WallpaperPlacement.originX(bmp.getWidth(), disp[0], scale))
                    + "," + WallpaperPlacement.round(WallpaperPlacement.originY(bmp.getHeight(), disp[1], scale))
                    + " 预览=" + this.where[0] + "," + this.where[1] + " " + getWidth() + "x" + getHeight()
                    + " 代号=" + id
                    // v1.10.520（用户：「编辑页不是桌面本身壁纸」）：这一句是为了区分
                    // 「读到的就是桌面那张」还是「读到了锁屏那张 / 桌面是动态壁纸（静态底图本来就不是屏上那张）」。
                    + " 锁屏代号=" + lockId(getContext())
                    + " 动态壁纸=" + (liveWallpaper(getContext()) ? "有" : "无"));
        } catch (Throwable ignored) {
        }
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        Bitmap bmp = this.bitmap;
        if (bmp == null || bmp.isRecycled() || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        int[] disp = displaySize(getContext());
        getLocationOnScreen(this.where);
        if (!WallpaperPlacement.slice(bmp.getWidth(), bmp.getHeight(), disp[0], disp[1],
                this.where[0], this.where[1], getWidth(), getHeight(), this.slice)) {
            return;
        }
        this.src.set(this.slice[0], this.slice[1], this.slice[2], this.slice[3]);
        this.dst.set(this.slice[4], this.slice[5], this.slice[6], this.slice[7]);
        int save = canvas.save();
        canvas.clipRect(0, 0, getWidth(), getHeight());
        canvas.drawBitmap(bmp, this.src, this.dst, this.paint);
        canvas.restoreToCount(save);
    }

    @Override // android.view.View
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 位置/尺寸一变，切哪一块就变了（判据 C/D：三键↔手势、字体缩放下仍要对位）
        invalidate();
    }

    // ---------------------------------------------------------------- 读壁纸

    /** 壁纸代号：换壁纸时它变（API 24+，本工程 minSdk 26 恒可用）。读不到返回 -1。 */
    static int wallpaperId(Context context) {
        try {
            return WallpaperManager.getInstance(context).getWallpaperId(WallpaperManager.FLAG_SYSTEM);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    /** 锁屏壁纸代号（只用于 W56 诊断：桌面与锁屏不同壁纸时，这一对数会不一样）。 */
    static int lockId(Context context) {
        try {
            return WallpaperManager.getInstance(context).getWallpaperId(WallpaperManager.FLAG_LOCK);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    /**
     * 桌面是不是**动态壁纸**（v1.10.520）。
     *
     * <p>为什么值得单独记：桌面设成动态壁纸时，{@code getDrawable()} 给的是系统存的
     * 静态底图（可能是主题自带的占位图），和屏上正在跑的那张未必是同一张 ——
     * 用户口径「编辑页不是桌面本身壁纸」十有八九要落在这一格上。</p>
     */
    static boolean liveWallpaper(Context context) {
        try {
            return WallpaperManager.getInstance(context).getWallpaperInfo() != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 在后台线程里取位图。优先直接拿 BitmapDrawable 的位图（零拷贝），拿不到才画进一张新位图。 */
    @Nullable
    private static Bitmap loadBitmap(Context context) {
        WallpaperManager manager = WallpaperManager.getInstance(context);
        Drawable drawable = manager.getDrawable();
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof BitmapDrawable) {
            Bitmap direct = ((BitmapDrawable) drawable).getBitmap();
            if (direct != null && direct.getWidth() > 0 && direct.getHeight() > 0) {
                return direct;
            }
        }
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return out;
    }

    /**
     * **整块显示区**尺寸（含状态栏与导航栏 —— 判据是「桌面壁纸铺到屏幕最上沿」，不是可见区）。
     *
     * <p>API 30+ 用 {@code getMaximumWindowMetrics()}；26~29 回退 {@code Display.getRealSize()}
     * （{@code getSize()} 会扣掉系统栏，用它就回到「少一块」的老问题里）；再退一步用
     * {@code DisplayMetrics}。</p>
     */
    static int[] displaySize(Context context) {
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                if (Build.VERSION.SDK_INT >= 30) {
                    WindowMetrics metrics = wm.getMaximumWindowMetrics();
                    if (metrics != null) {
                        Rect bounds = metrics.getBounds();
                        if (bounds.width() > 0 && bounds.height() > 0) {
                            return new int[]{bounds.width(), bounds.height()};
                        }
                    }
                }
                Display display = wm.getDefaultDisplay();
                if (display != null) {
                    Point point = new Point();
                    try {
                        display.getRealSize(point);
                    } catch (Throwable ignored) {
                        display.getSize(point);
                    }
                    if (point.x > 0 && point.y > 0) {
                        return new int[]{point.x, point.y};
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return new int[]{dm.widthPixels, dm.heightPixels};
    }
}
