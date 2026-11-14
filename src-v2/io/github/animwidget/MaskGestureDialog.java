package io.github.animwidget;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 蒙版位置手势对话框（v1.6.0 新增）。
 *
 * 复用 {@link ContentGestureView}，但把拖动目标切成「蒙版」：
 * 拖动十字时改的是 maskOffsetX/YPercent，整块蒙版连同画面一起在布局框内平移，
 * 超出组件边界的部分由 FrameComposer 的 clipRect 裁掉 —— 预览即所见。
 *
 * 布局沿用 dialog_content_gesture.xml，把「等比例缩放（内容）」那一行隐藏掉。
 */
final class MaskGestureDialog {

    /** 拖动过程中持续回调（写进 ConfigActivity 的 selected 字段）。 */
    interface Listener {
        void onMaskOffset(float xPercent, float yPercent);
    }

    /**
     * v1.10.497（用户反馈）：**输出进行中，「完成」不许落值。**
     *
     * <p>这个面板是独立 Dialog，它的触摸事件根本不经过 Activity 的 `dispatchTouchEvent`，
     * 所以参数锁在它身上是失效的 —— 用户点「完成」时照旧提交，而且一句提示都没有。
     * 现在把「要不要拦」交给调用方（{@code ConfigActivity.outputRunning()}），
     * 拦下来时由调用方弹「当前方案正在输出」，弹窗确认后由调用方关掉这个面板。</p>
     */
    interface Gate {
        /**
         * @return true = 已拦下（**不要提交、也不要自己 dismiss**，交给调用方处理）
         */
        boolean blockDone(@NotNull Dialog dialog);
    }

    private MaskGestureDialog() {
    }

    static void show(@NotNull Activity activity, @Nullable Bitmap source, float aspect,
                     @NotNull LayoutMode layout, @NotNull ShapeKind shape, int alphaPercent, boolean border,
                     int shadowPercent, String shadowCurve, int contentRotation, int shapeRotation, boolean fitContent,
                     float contentOffsetX, float contentOffsetY, int contentScale,
                     float maskX, float maskY, @Nullable Listener listener, @Nullable Runnable onDismiss,
                     @Nullable Gate gate) {
        ThemeColors colors = ThemeManager.INSTANCE.colors(activity);
        float density = activity.getResources().getDisplayMetrics().density;

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ThemeManager.INSTANCE.clearDialogWindow(dialog);   // v1.10.71：圆角不露方底
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_content_gesture, null);
        dialog.setContentView(view);
        // v1.10.173（反馈）：独立 Dialog 不在 Activity 视图树里，必须自己铺一次字体，
        // 否则它既不跟随「字体」也不跟随「字体大小」（其它三个自绘弹窗早就铺了，这里漏了）。
        Fonts.applyTree(view);

        LinearLayout root = (LinearLayout) view;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(colors.getSurface());
        bg.setCornerRadius(24.0f * density);
        root.setBackground(bg);

        TextView title = (TextView) view.findViewById(R.id.gesture_title);
        TextView hint = (TextView) view.findViewById(R.id.gesture_hint);
        TextView close = (TextView) view.findViewById(R.id.gesture_close);
        TextView values = (TextView) view.findViewById(R.id.gesture_values);
        FrameLayout host = (FrameLayout) view.findViewById(R.id.gesture_host);
        Button reset = (Button) view.findViewById(R.id.gesture_reset);
        Button done = (Button) view.findViewById(R.id.gesture_done);

        // 蒙版位置不需要缩放滑块
        view.findViewById(R.id.gesture_scale_label).setVisibility(View.GONE);
        view.findViewById(R.id.gesture_scale).setVisibility(View.GONE);
        view.findViewById(R.id.gesture_scale_hint).setVisibility(View.GONE);   // v1.10.74

        title.setText(R.string.mask_gesture_title);
        hint.setText(R.string.mask_gesture_hint);
        title.setTextColor(colors.getText());
        hint.setTextColor(colors.getTextMuted());
        values.setTextColor(colors.getText());
        close.setTextColor(colors.getText());

        final float openMaskX = maskX;
        final float openMaskY = maskY;
        final boolean[] committed = {false};
        final ContentGestureView gesture = new ContentGestureView(activity);
        gesture.setDragMask(true);
        gesture.setSource(source);
        gesture.setLayout(layout);
        gesture.setShape(shape);
        gesture.setShapeRotation(shapeRotation);
        gesture.setFitContent(fitContent);
        gesture.setAlphaPercent(alphaPercent);
        gesture.setBorder(border);
        gesture.setShadowPercent(shadowPercent);
        gesture.setShadowCurve(shadowCurve);   // v1.10.221：阴影曲线跟着走，弹窗里的预览才一致
        gesture.setContentScalePercent(contentScale);
        gesture.setMaskRotation(contentRotation);   // 注意：这个方法名是反的，实际设置的是「内容旋转」
        gesture.setOffsetXPercent(contentOffsetX);
        gesture.setOffsetYPercent(contentOffsetY);
        gesture.setMaskOffsetXPercent(maskX);
        gesture.setMaskOffsetYPercent(maskY);

        AspectRatioFrameLayout box = new AspectRatioFrameLayout(activity);
        box.setRatio(aspect > 0.0f ? aspect : 1.0f);
        box.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        box.addView(gesture, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        host.setBackground(ThemeManager.INSTANCE.rounded(activity, colors.getNeutral(), 18.0f));
        host.addView(box);

        refreshValues(values, activity, gesture);

        gesture.setOnChange(new Function2<Float, Float, Unit>() {
            @Override
            public Unit invoke(Float ox, Float oy) {
                refreshValues(values, activity, gesture);
                if (listener != null) {
                    listener.onMaskOffset(ox.floatValue(), oy.floatValue());
                }
                return Unit.INSTANCE;
            }
        });

        reset.setBackground(ThemeManager.INSTANCE.outlined(activity, 0, colors.getNeutral(), 999.0f));
        reset.setTextColor(colors.getText());
        done.setBackground(ThemeManager.INSTANCE.rounded(activity, colors.getPrimary(), 999.0f));
        done.setTextColor(colors.getOnPrimary());
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gesture.resetParams();
            }
        });
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // v1.10.497（用户反馈）：输出进行中，这一下「完成」= 想保存 → 先问闸。
                // 拦下时 committed 保持 false，所以等它被关掉时 onDismiss 会把蒙版位置回滚
                // （既不落值，又等于退出了这个界面 —— 用户口径）。
                if (gate != null && gate.blockDone(dialog)) {
                    return;
                }
                // v1.8.4：只有「完成」才保留改动
                committed[0] = true;
                dialog.dismiss();
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface d) {
                // ContentGestureView 的 compose 结果由它自己在 onDetachedFromWindow 回收，
                // 原始素材位图归调用方（ConfigActivity）管，这里不碰。
                gesture.setSource(null);
                if (!committed[0] && listener != null) {
                    // 没点「完成」就关掉 → 回滚到打开时的位置，不再“悄悄保存”
                    listener.onMaskOffset(openMaskX, openMaskY);
                }
                if (onDismiss != null) {
                    onDismiss.run();
                }
            }
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.55f);
            window.setGravity(Gravity.CENTER);
        }
        dialog.show();
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
        Window shown = dialog.getWindow();
        if (shown != null) {
            shown.setLayout((int) (screenWidth - 24.0f * density), (int) (screenHeight * 0.9f));
        }
    }

    private static void refreshValues(TextView values, Activity activity, ContentGestureView gesture) {
        values.setText(activity.getString(R.string.mask_gesture_values,
                Float.valueOf(gesture.getMaskOffsetXPercent()), Float.valueOf(gesture.getMaskOffsetYPercent())));
    }
}
