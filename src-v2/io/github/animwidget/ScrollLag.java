package io.github.animwidget;

import android.view.View;
import android.widget.ScrollView;

/**
 * v1.10.105（需求 2.1）：**滚动跟随延迟（低）**。
 *
 * ScrollView 的内容在手指拖动时立刻到位，看起来「很脆」。
 * 这里让内容比滚动位置慢一点（每帧追 {@link #DEFAULT_FACTOR}），
 * 松手后几帧内自己追上 —— 低延迟、几乎察觉不到，但滚动的"重量"出来了。
 *
 * 实现只用 translationY（GPU 合成，不触发重新布局），并且与 Android 12+ 的
 * overscroll 拉伸（EdgeEffect stretch）叠加互不干扰。
 */
public final class ScrollLag {

    /** 每帧追赶比例：越大延迟越小。0.30 ≈ 100ms 内追上。 */
    public static final float DEFAULT_FACTOR = 0.30f;

    private ScrollLag() {
    }

    public static void attach(final ScrollView scrollView, final View content) {
        attach(scrollView, content, DEFAULT_FACTOR);
    }

    public static void attach(final ScrollView scrollView, final View content, final float factor) {
        if (scrollView == null || content == null) {
            return;
        }
        final float k = (factor <= 0.0f || factor > 0.9f) ? DEFAULT_FACTOR : factor;
        final float[] target = {0.0f};
        final float[] smooth = {0.0f};
        final boolean[] running = {false};
        final Runnable tick = new Runnable() {
            @Override
            public void run() {
                float diff = target[0] - smooth[0];
                if (Math.abs(diff) < 0.6f) {
                    smooth[0] = target[0];
                    content.setTranslationY(0.0f);
                    running[0] = false;
                    return;
                }
                smooth[0] += diff * k;
                // 内容还没追上滚动量 → 视觉上落后一点
                content.setTranslationY(target[0] - smooth[0]);
                content.postOnAnimation(this);
            }
        };
        scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldX, oldY) -> {
            target[0] = scrollY;
            if (!running[0]) {
                running[0] = true;
                content.postOnAnimation(tick);
            }
        });
    }
}
