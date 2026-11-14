package io.github.animwidget;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v1.10.499：编辑页「当前方案正在输出」提示 —— **页内覆盖层**（不是 {@code Dialog}）。
 *
 * <p>为什么必须换掉 Dialog（真机日志 12:34:56–12:36:05，导出中拖内容/蒙版旋转滑块）：
 * 原来那条路是 {@code dispatchTouchEvent(ACTION_DOWN)} → {@code new OutputBusyDialog} →
 * {@code capture()}（{@code Bitmap.createBitmap(1080×2400)} + 把整棵 View 树软件重绘一遍，含 887×685
 * 的预览图）+ 4 遍盒式模糊 + inflate + {@code dialog.show()}（新窗口、token 协商、首帧同步）。
 * 这一整串全在 UI 线程、且在**输入分发里**，导出正占着核时就是肉眼可见的「按下去半天才弹」。</p>
 *
 * <p>现在的分工：</p>
 * <ul>
 *   <li><b>准备（只在页面静止时做一次）</b>：建好的视图树挂进 {@code android.R.id.content}、{@code GONE}；
 *       底图用 {@link PixelCopy} 在**后台线程**抓（GPU→CPU 拷贝，不需要 UI 线程重绘），
 *       拿到后交给 {@link BlurBackdropView} 做一次 1/6 缩放 + 4 遍盒式模糊（几毫秒，且不在输入路径上）。</li>
 *   <li><b>显示（触摸那一下）</b>：{@code setVisibility(VISIBLE)} + 入场动画，**零分配、零 I/O、零 inflate**。</li>
 *   <li><b>收尾</b>：退场动画走完才 {@code GONE}；确认回调在退场之后执行（与原实现同一语义）。</li>
 * </ul>
 *
 * <p>底图抓不到（窗口还没画、PixelCopy 失败）→ 退化成纯压暗，不影响「参数照样被拦住」。</p>
 */
final class BusyOverlay {

    /** 底图抓取用独立线程：PixelCopy 的回调线程由我们指定，绝不占用 UI 线程。 */
    private static final class Holder {
        static final HandlerThread THREAD = new HandlerThread("busy-overlay-shot");

        static {
            THREAD.start();
        }

        static final Handler HANDLER = new Handler(THREAD.getLooper());
    }

    @NotNull
    private final Activity activity;
    @NotNull
    private ThemeColors colors;
    private final float dp;
    private boolean dark;

    @Nullable
    private FrameLayout root;
    @Nullable
    private BlurBackdropView backdrop;
    /** 正在显示时抓到的新底图先放这里，等收起后再换上（显示中直接替换会闪）。 */
    @Nullable
    private BlurBackdropView staged;
    @Nullable
    private LinearLayout card;
    @Nullable
    private TextView line;
    @Nullable
    private Button ok;
    @Nullable
    private android.animation.ValueAnimator running;
    @Nullable
    private UiModeWatcher uiMode;

    private boolean closing;
    private boolean prepared;
    private boolean capturing;
    /** 换肤重建时不播入场动画（否则看起来像「又弹了一次」）。 */
    private boolean quietNext;

    @Nullable
    private Runnable onConfirmed;
    @Nullable
    private Runnable onHidden;

    BusyOverlay(@NotNull Activity activity) {
        this.activity = activity;
        this.colors = ThemeManager.INSTANCE.colors(activity);
        this.dp = activity.getResources().getDisplayMetrics().density;
        this.dark = ThemeManager.INSTANCE.isDark(activity);
    }

    boolean isShowing() {
        FrameLayout r = this.root;
        return r != null && r.getVisibility() == View.VISIBLE;
    }

    // ------------------------------------------------------------------ 准备

    /**
     * 建视图树并挂进页面（{@code GONE}），同时排一次底图抓取。
     * 可以在页面静止的任何时刻调用（重复调用是幂等的）。
     */
    void prepare() {
        if (this.prepared) {
            return;
        }
        try {
            FrameLayout content = this.activity.findViewById(android.R.id.content);
            if (content == null) {
                return;
            }
            FrameLayout r = new FrameLayout(this.activity);
            // v1.10.503（用户再次确认）：**点空白绝不关闭**，只有「确认」与系统返回键关它。
            // 用 OnTouchListener 直接吃掉事件（比「空 OnClickListener」更硬：连点击语义都不产生，
            // 也不会因为某个子 View 没设 clickable 而漏到页面下面去）。
            // 注意它不会抢子 View 的事件：ViewGroup 先分发给子 View，没人接手才走到这里。
            r.setClickable(false);
            r.setOnTouchListener((v, e) -> true);
            r.setVisibility(View.GONE);
            r.setFocusable(true);
            r.setFocusableInTouchMode(true);

            // 底图先占位（纯压暗）；抓到了再替换
            BlurBackdropView back = new BlurBackdropView(this.activity, null,
                    this.dark ? 0x3A000000 : 0x22000000);
            back.setOnClickListener(v -> {
                // 同上：空白处点击不关闭
            });
            r.addView(back, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            this.backdrop = back;

            LinearLayout box = new LinearLayout(this.activity);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER_HORIZONTAL);
            box.setPadding((int) (this.dp * 22.0f), (int) (this.dp * 22.0f),
                    (int) (this.dp * 22.0f), (int) (this.dp * 18.0f));
            box.setClickable(true);
            box.setOnClickListener(v -> {
                // 吃掉点击：卡片内部不该触发「点外面就关」
            });
            int cardWidth = (int) Math.min(
                    this.activity.getResources().getDisplayMetrics().widthPixels * 0.82f, this.dp * 340.0f);
            FrameLayout.LayoutParams boxLp = new FrameLayout.LayoutParams(
                    cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
            boxLp.gravity = Gravity.CENTER;
            r.addView(box, boxLp);
            this.card = box;

            TextView text = new TextView(this.activity);
            text.setText(this.activity.getString(R.string.output_busy_line));
            text.setTextSize(15.0f);
            text.setGravity(Gravity.CENTER);
            text.setLineSpacing(this.dp * 3.0f, 1.0f);
            box.addView(text, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            this.line = text;

            Button button = new Button(this.activity);
            button.setText(this.activity.getString(R.string.output_busy_ok));
            button.setTextSize(14.0f);
            button.setAllCaps(false);
            button.setMinHeight(0);
            button.setMinimumHeight(0);
            button.setMinWidth(0);
            button.setMinimumWidth(0);
            button.setPadding(0, 0, 0, 0);
            // 按下 0.965 回弹：和全应用的胶囊按钮同一套手感
            button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int action = event.getActionMasked();
                    if (action == MotionEvent.ACTION_DOWN) {
                        v.animate().scaleX(0.965f).scaleY(0.965f).setDuration(90L).start();
                    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150L).start();
                    }
                    return false;
                }
            });
            button.setOnClickListener(v -> close());
            LinearLayout.LayoutParams okLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, (int) (this.dp * 46.0f));
            okLp.topMargin = (int) (this.dp * 18.0f);
            box.addView(button, okLp);
            this.ok = button;

            applyTheme();
            Fonts.applyTree(box);
            apply(0.0f);

            content.addView(r, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            this.root = r;
            this.prepared = true;
            captureBackdrop();
        } catch (Throwable ignored) {
            // 建不起来只会让「提示」看不到，参数照样被拦住；不许把页面拖崩
        }
    }

    /**
     * 后台抓一张底图（页面当前画面），抓不到就保持纯压暗。
     *
     * <p>{@link PixelCopy} 只做 GPU→CPU 的拷贝，不要求 UI 线程重绘整棵树 —— 这正是原来
     * {@code capture()} 里 {@code decorView.draw(canvas)} 那几十毫秒的来源。</p>
     */
    private void captureBackdrop() {
        FrameLayout r = this.root;
        if (r == null) {
            return;
        }
        if (this.capturing) {
            return;
        }
        android.view.Window window = this.activity.getWindow();
        View decor = window != null ? window.getDecorView() : null;
        // v1.10.502（用户反馈「模糊背景有明显上移、不是当前界面」）：
        // 原来抓的是**整窗**（decor），却画在 `android.R.id.content` 这块区域里 ——
        // 两者在窗口里的原点/高度并不相同（content 不含状态栏/导航栏），
        // 位图被拉伸到 content 的矩形上，画面就整体上移且轻微变形。
        // 现在按「覆盖层自己那块矩形」抓：抓哪儿画哪儿，一个像素都不错位。
        View anchor = r.getWidth() > 0 ? r : this.activity.findViewById(android.R.id.content);
        if (anchor == null || anchor.getWidth() <= 0 || anchor.getHeight() <= 0 || decor == null) {
            return;     // 布局还没完成：下次 prepare/refresh 再来
        }
        final int[] loc = new int[2];
        anchor.getLocationInWindow(loc);
        int w = anchor.getWidth();
        int h = anchor.getHeight();
        int left = Math.max(0, loc[0]);
        int top = Math.max(0, loc[1]);
        // 夹进窗口范围，PixelCopy 传越界矩形会直接抛异常
        w = Math.min(w, Math.max(1, decor.getWidth() - left));
        h = Math.min(h, Math.max(1, decor.getHeight() - top));
        final android.graphics.Rect src = new android.graphics.Rect(left, top, left + w, top + h);
        this.capturing = true;
        // v1.10.502：**连位图分配也放到后台线程**。10MB 的 createBitmap 放在 UI 线程会触发 GC，
        // 那一帧正好是用户按下手指的那一帧 —— 几何只读一次（必须 UI 线程），剩下全交给抓图线程。
        final android.view.Window target = window;
        final int fw = w;
        final int fh = h;
        Holder.HANDLER.post(() -> {
            Bitmap shot = null;
            try {
                shot = Bitmap.createBitmap(fw, fh, Bitmap.Config.ARGB_8888);
                if (Build.VERSION.SDK_INT >= 26 && target != null) {
                    final Bitmap dst = shot;
                    PixelCopy.request(target, src, dst, new PixelCopy.OnPixelCopyFinishedListener() {
                        @Override
                        public void onPixelCopyFinished(int result) {
                            Holder.HANDLER.post(() -> onShot(dst, result == PixelCopy.SUCCESS));
                        }
                    }, Holder.HANDLER);
                } else {
                    final Bitmap dst = shot;
                    Holder.HANDLER.post(() -> onShot(dst, false));
                }
            } catch (Throwable th) {
                this.capturing = false;
                if (shot != null && !shot.isRecycled()) {
                    shot.recycle();
                }
            }
        });
    }

    private void onShot(@NotNull final Bitmap shot, final boolean ok) {
        final BlurBackdropView old = this.backdrop;
        final int scrim = this.dark ? 0x3A000000 : 0x22000000;
        this.activity.runOnUiThread(() -> {
            this.capturing = false;
            FrameLayout r = this.root;
            if (r == null || this.activity.isFinishing()) {
                if (!shot.isRecycled()) {
                    shot.recycle();
                }
                return;
            }
            if (!ok) {
                if (!shot.isRecycled()) {
                    shot.recycle();
                }
                return;     // 保持纯压暗
            }
            BlurBackdropView back = new BlurBackdropView(this.activity, shot, scrim);
            back.setAlpha(old != null ? old.getAlpha() : 0.0f);
            back.setOnClickListener(v -> {
                // 空白处点击不关闭（只有「确认」与系统返回键关它）
            });
            if (isShowing()) {
                // v1.10.502（用户要求「实时」）：显示期间抓到的新图**当场换上**。
                // 入场动画正在跑（300ms），换图发生在 ~40ms 处，肉眼看不到接缝；
                // 透明度沿用旧的，后续帧由 apply(t) 继续驱动。
                final BlurBackdropView fresh = back;
                fresh.setAlpha(old != null ? old.getAlpha() : 1.0f);
                r.removeView(old);
                r.addView(fresh, 0, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                this.backdrop = fresh;
                if (old != null) {
                    old.release();
                }
                return;
            }
            r.removeView(old);
            r.addView(back, 0, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            this.backdrop = back;
            if (old != null) {
                old.release();
            }
        });
    }

    /**
     * 刷新底图（页面静止时调用）。**显示中也会抓**，只是不当场替换 —— 抓到的新底图进 {@link #staged}，
     * 本次收起时再换上。原来「显示中直接 return」是「每次模糊都是第一次那张」的根因：
     * 导出一开始用户先点了参数，后续几次抓图全被这句挡掉了。
     */
    void refreshBackdrop() {
        captureBackdrop();
    }

    // ------------------------------------------------------------------ 显示 / 收起

    void show(@Nullable Runnable confirmed) {
        this.onConfirmed = confirmed;
        prepare();
        FrameLayout r = this.root;
        if (r == null) {
            return;
        }
        this.closing = false;
        r.setVisibility(View.VISIBLE);
        r.bringToFront();
        // v1.10.502：显示的同时在**后台**抓一张「此刻」的图（PixelCopy 只做 GPU→CPU 拷贝，
        // 不碰 UI 线程），到手后当场换上。这样模糊背景就是当前界面，而不是上一次那张。
        captureBackdrop();
        if (this.quietNext) {
            this.quietNext = false;
            apply(1.0f);
        } else {
            playIn();
        }
        if (this.uiMode == null) {
            this.uiMode = new UiModeWatcher(this.activity, new UiModeWatcher.Listener() {
                @Override
                public void onSystemModeChanged() {
                    rebuildForTheme();
                }
            });
        }
        this.uiMode.start();
    }

    /** 「确认」/返回/点外面都走这一条：先退场动画，再隐藏，最后执行确认回调（与原实现同序）。 */
    void close() {
        if (this.closing || !isShowing()) {
            return;
        }
        this.closing = true;
        playOut(new Runnable() {
            @Override
            public void run() {
                hideNow();
                Runnable after = BusyOverlay.this.onConfirmed;
                BusyOverlay.this.onConfirmed = null;
                if (after != null) {
                    try {
                        after.run();
                    } catch (Throwable ignored) {
                    }
                }
            }
        });
    }

    private void hideNow() {
        cancelAnim();
        FrameLayout r = this.root;
        if (r != null) {
            r.setVisibility(View.GONE);
            // 把显示期间抓到的新底图换上，下一次弹出的就是「这一刻」的页面
            BlurBackdropView fresh = this.staged;
            if (fresh != null) {
                this.staged = null;
                BlurBackdropView old = this.backdrop;
                r.removeView(old);
                fresh.setAlpha(old != null ? old.getAlpha() : 0.0f);
                r.addView(fresh, 0, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                this.backdrop = fresh;
                if (old != null) {
                    old.release();
                }
            }
        }
        // 收起后页面通常又静止了：排一次抓图，下次弹出就是最新的
        try {
            r.postDelayed(this::refreshBackdrop, 250L);
        } catch (Throwable ignored) {
        }
        Runnable hidden = this.onHidden;
        this.onHidden = null;
        if (hidden != null) {
            try {
                hidden.run();
            } catch (Throwable ignored) {
            }
        }
    }

    /** 返回键：显示时吃掉并收起（返回 false = 没显示，交给页面）。 */
    boolean handleBack() {
        if (!isShowing()) {
            return false;
        }
        close();
        return true;
    }

    void destroy() {
        cancelAnim();
        if (this.uiMode != null) {
            this.uiMode.stop();
            this.uiMode = null;
        }
        BlurBackdropView back = this.backdrop;
        this.backdrop = null;
        if (back != null) {
            back.release();
        }
        BlurBackdropView pending = this.staged;
        this.staged = null;
        if (pending != null) {
            pending.release();
        }
        FrameLayout r = this.root;
        this.root = null;
        this.prepared = false;
        if (r != null) {
            ViewGroup parent = (ViewGroup) r.getParent();
            if (parent != null) {
                parent.removeView(r);
            }
        }
    }

    // ------------------------------------------------------------------ 视觉

    private void applyTheme() {
        LinearLayout box = this.card;
        if (box != null) {
            box.setBackground(new GlassFrameDrawable(
                    withAlpha(this.colors.getSurface(), 0.94f), 24.0f * this.dp, 1.2f * this.dp, this.dark));
            box.setElevation(this.dp * 14.0f);
        }
        TextView text = this.line;
        if (text != null) {
            text.setTextColor(this.colors.getText());
        }
        Button button = this.ok;
        if (button != null) {
            button.setTextColor(this.colors.getOnPrimary());
            button.setBackground(ThemeManager.INSTANCE.rounded(this.activity, this.colors.getPrimary(), 999.0f));
            button.setElevation(2.0f * this.dp);
            ThemeManager.INSTANCE.exposeShadow(button);
        }
    }

    /** 唯一的「写属性」出口：卡片与模糊底用同一个 t，所以模糊永远跟着弹窗走。 */
    private void apply(float t) {
        float p = t < 0.0f ? 0.0f : t;
        float a = p > 1.0f ? 1.0f : p;
        BlurBackdropView back = this.backdrop;
        if (back != null) {
            back.setAlpha(a);
        }
        LinearLayout box = this.card;
        if (box != null) {
            box.setAlpha(a);
            float scale = 0.88f + 0.12f * p;
            box.setScaleX(scale);
            box.setScaleY(scale);
            box.setTranslationY(this.dp * 16.0f * (1.0f - p));
        }
    }

    /** 入场曲线：主曲线 {@link Ease#OUT}（快出慢收）+ 尾段约 0.8% 的过冲。 */
    private static float inCurve(float f) {
        float e = Ease.OUT.at(f);
        if (f > 0.6f) {
            e += 0.05f * Ease.OVERSHOOT.at((f - 0.6f) / 0.4f);
        }
        return e;
    }

    private void cancelAnim() {
        android.animation.ValueAnimator animator = this.running;
        this.running = null;
        if (animator != null) {
            try {
                animator.cancel();
            } catch (Throwable ignored) {
            }
        }
    }

    private void playIn() {
        cancelAnim();
        if (animationsOff()) {
            apply(1.0f);
            return;
        }
        final android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.setDuration(300L);
        // **线性**驱动：getAnimatedFraction() 拿到的已经是插值后的值，不设 null 会和 Ease 叠成两层
        animator.setInterpolator(null);
        animator.addUpdateListener(a -> apply(inCurve(a.getAnimatedFraction())));
        animator.start();
        this.running = animator;
    }

    private void playOut(@NotNull final Runnable after) {
        cancelAnim();
        if (animationsOff()) {
            apply(0.0f);
            after.run();
            return;
        }
        final android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.setDuration(200L);
        animator.setInterpolator(null);
        animator.addUpdateListener(a -> apply(1.0f - Ease.IN_OUT.at(a.getAnimatedFraction())));
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                after.run();
            }
        });
        animator.start();
        this.running = animator;
    }

    private boolean animationsOff() {
        try {
            return android.provider.Settings.Global.getFloat(
                    this.activity.getContentResolver(),
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f) <= 0.0f;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 系统深浅色变了：颜色现算重刷（不重建视图、不重抓底图，避免「又弹一次」的观感）。 */
    private void rebuildForTheme() {
        if (this.closing) {
            return;
        }
        this.dark = ThemeManager.INSTANCE.isDark(this.activity);
        this.colors = ThemeManager.INSTANCE.colors(this.activity);
        applyTheme();
        this.quietNext = true;
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * alpha);
        if (a <= 0) {
            a = 1;
        }
        return (a << 24) | (color & 0x00FFFFFF);
    }
}
