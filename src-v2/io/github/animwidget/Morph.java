package io.github.animwidget;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.WeakHashMap;

/**
 * v1.10.99（反馈 2.2）：**整页叠化**。
 *
 * 切语言 / 切字体 / 切配色时，整页的文字形状和颜色会成片变化，直接刷会「啪」一下很硬。
 * 做法：先把当前画面画成一张位图盖在最上层 → 改数据 → 位图 260ms 淡出。
 * 新旧两版画面叠化，视觉上像一次转场，而不是一次刷新。
 *
 * 只对「用户主动切换」用；后台轮询（深浅色跟随）不走这里，避免莫名闪。
 *
 * v1.10.176（反馈）：**叠化期间不许再有第二个动效**。
 * 切配色方案时对比度滑杆会走一条 Ease 曲线，可它跑在同一段时间里，
 * 被这张不透明的旧画面盖住 —— 用户看到的是「滑块在雾里挪」，观感很脏。
 * 现在给 `crossfade` 加了 `after`：需要「等叠化结束再动」的动效
 * （滑杆曲线、勾选动画）一律挂到 `after` 里串行执行。
 */
public final class Morph {

    /** 每个 Activity 当前挂着的遮罩（连点时先把旧的摘掉，不会叠两层）。 */
    private static final WeakHashMap<Activity, ImageView> LIVE = new WeakHashMap<>();

    /** v1.10.176：叠化淡出时长（`after` 回调也按这个节奏串行）。 */
    public static final long FADE_MS = 300L;

    private Morph() {
    }

    public static void crossfade(final Activity activity, final Runnable change) {
        crossfade(activity, change, null);
    }

    /**
     * v1.10.176：带「叠化结束」回调的版本。
     *
     * @param after 遮罩彻底消失后再跑；没截图成功 / 页面不可见时**立刻**跑（此时 = 没有叠化）
     */
    public static void crossfade(final Activity activity, final Runnable change,
                                 final Runnable after) {
        if (change == null) {
            if (after != null) {
                after.run();
            }
            return;
        }
        if (activity == null || activity.isFinishing()) {
            change.run();
            if (after != null) {
                after.run();
            }
            return;
        }
        // 页面在后台（系统切深浅色时常见）就别截图了，直接改
        if (!activity.getWindow().getDecorView().isShown()) {
            change.run();
            if (after != null) {
                after.run();
            }
            return;
        }
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)
                || content.getWidth() <= 0 || content.getHeight() <= 0) {
            change.run();
            if (after != null) {
                after.run();
            }
            return;
        }
        final FrameLayout host = (FrameLayout) content;
        ImageView stale = LIVE.remove(activity);
        if (stale != null) {
            host.removeView(stale);
            // v1.10.176：连点切换时旧遮罩会被直接摘掉，它那张整屏 ARGB 位图
            // （1080×2400 约 10MB）原来没人回收 —— 连点几次就是几十 MB 垃圾。
            recycleVeil(stale);
        }
        Bitmap made = null;
        try {
            made = Bitmap.createBitmap(content.getWidth(), content.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(made);
            content.draw(canvas);
        } catch (Throwable th) {
            if (made != null && !made.isRecycled()) {
                made.recycle();
            }
            made = null;
        }
        change.run();
        if (made == null) {
            if (after != null) {
                after.run();
            }
            return;
        }
        final Bitmap shot = made;
        final ImageView veil = new ImageView(activity);
        veil.setImageBitmap(shot);
        veil.setScaleType(ImageView.ScaleType.FIT_XY);
        veil.setClickable(false);
        veil.setFocusable(false);
        // v1.10.101（反馈 2）：遮罩必须 1:1 盖在截图原位上。
        // android.R.id.content 被 EdgeToEdge 塞进了状态栏/导航栏的 padding，
        // 如果遮罩用 MATCH_PARENT 就会被挤进 padding 里再被 FIT_XY 缩一遍 ——
        // 看起来就是「一切换整屏被压缩」。这里用精确像素 + 负 margin 抵消 padding。
        FrameLayout.LayoutParams vlp = new FrameLayout.LayoutParams(
                shot.getWidth(), shot.getHeight());
        vlp.leftMargin = -content.getPaddingLeft();
        vlp.topMargin = -content.getPaddingTop();
        host.addView(veil, vlp);
        LIVE.put(activity, veil);
        veil.animate().alpha(0.0f).setDuration(FADE_MS)
                .setInterpolator(new PathInterpolator(0.34f, 0.0f, 0.20f, 1.0f))
                .withEndAction(() -> {
                    host.removeView(veil);
                    if (!shot.isRecycled()) {
                        shot.recycle();
                    }
                    LIVE.remove(activity);
                    if (after != null) {
                        after.run();
                    }
                }).start();
    }

    /** 当前是否有一层叠化遮罩还盖在页面上（诊断 / 串行判断用）。 */
    public static boolean isCrossfading(final Activity activity) {
        if (activity == null) {
            return false;
        }
        ImageView veil = LIVE.get(activity);
        return veil != null && veil.getParent() != null;
    }

    /** 摘掉遮罩时把它的整屏位图一起回收（先置空 drawable，重复回收也安全）。 */
    private static void recycleVeil(final ImageView veil) {
        try {
            android.graphics.drawable.Drawable d = veil.getDrawable();
            veil.setImageDrawable(null);
            if (d instanceof android.graphics.drawable.BitmapDrawable) {
                Bitmap bitmap = ((android.graphics.drawable.BitmapDrawable) d).getBitmap();
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
