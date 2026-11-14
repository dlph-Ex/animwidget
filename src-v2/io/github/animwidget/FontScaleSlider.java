package io.github.animwidget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.SeekBar;

/**
 * v1.10.159（反馈「重做」）：**完全自绘的四档滑块**，替换原生 SeekBar。
 *
 * 为什么弃用 SeekBar（三轮没做对的三条真实病根）：
 *  1. 原生 SeekBar 在 `ACTION_DOWN` 就把拇指瞬移到手指处 —— 这是「纯瞬间到达」的来源。
 *     上一版用 `setOnTouchListener` 吃掉了 DOWN，但**拇指仍然由平台 drawable 画**，
 *     两端还会被 View 边界裁掉，只能靠 `setPadding(14dp)` 去躲，属于打补丁。
 *  2. 滑到目标档的 220ms 曲线发生在 `Morph.crossfade` 的**不透明截图层下面** ——
 *     用户看到的是「一整套旧画面淡出」，拇指的位移被盖死，感知上就是「瞬间」。
 *     现在顺序反过来：先让拇指走完，再让字号连续长大，两段都看得见。
 *  3. 原生 SeekBar 的 progress 是整数，`setProgress` 只能一档一档跳；
 *     拖动时拇指无法连续跟手（跟手和分档是同一次赋值）。
 *
 * 本实现的语义（都可复算）：
 *  · 几何：`inset = thumbR + 2dp`，所以拇指圆心永远在 [inset, w-inset] 内，
 *    拇指**不可能**被裁掉（不是靠 padding 凑）；
 *  · 按下不动：`ACTION_DOWN` 只记坐标、把拇指按大 13%；
 *  · 拖动跟手：MOVE 把目标值 `valueTarget` 连续更新，每帧 `value += Δ×0.40` 逼近
 *    （60Hz 下约 60ms 收敛），既跟手又不跳；
 *  · 松手落档：`settleTo(stop)` 用 `Ease.IN_OUT` 走 260ms 到档位，**落定后才回调
 *    `onSettle`**，业务层（换字号）必须在这个回调之后动作；
 *  · 点轨道也算一次落档（走同一条曲线），不会瞬移；
 *  · 过档发 `CLOCK_TICK` 触感；`ANIMATOR_DURATION_SCALE=0`（减少动态效果）时全部时长归零；
 *  · 无障碍：class=SeekBar + RangeInfo(0..3) + SCROLL_FORWARD/BACKWARD + ACTION_SET_PROGRESS。
 */
public final class FontScaleSlider extends View {

    /** 档位变化回调。预览只在拇指路过/悬停时给，只有 `onSettle` 才允许落库。 */
    public interface Listener {
        void onPreview(int index);

        void onSettle(int index);
    }

    /** 四档：小 / 默认 / 中 / 大（与 {@link Fonts#TEXT_SCALES} 一一对应）。 */
    private static final int STOPS = 4;

    private static final long DUR_SETTLE = 260L;
    private static final long DUR_PRESS = 90L;

    private final Paint pTrack = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pThumb = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pLabelOn = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private final RectF oval = new RectF();
    private final Path fillPath = new Path();

    private final float dp;
    private final float thumbR;
    private final float trackH;
    private final float inset;
    private final float centerY;
    private final float labelGap;
    private final int touchSlop;
    /** 档位文字跟随「字体大小」档位（1.0 = 原始）。 */
    private float labelScale = 1.0f;
    private final float labelBasePx;

    private int index = 1;
    private float value = 1.0f;
    private float valueTarget = 1.0f;
    private boolean following;
    private float pressT;
    private boolean dragging;
    private float downX;
    private float downY;

    /** 负责画光晕的外层容器（见 {@link GlowRowLayout}）；为 null 时无人画光晕。 */
    private View glowHost;
    private Listener listener;
    private ValueAnimator settleAnim;
    private ValueAnimator pressAnim;

    private int cTrack;
    private int cTick;
    private int cFill;
    private int cLabel;
    private int cLabelOn;

    public FontScaleSlider(Context context) {
        super(context);
        this.dp = context.getResources().getDisplayMetrics().density;
        float sp = context.getResources().getDisplayMetrics().scaledDensity;
        this.thumbR = 10.5f * dp;
        this.trackH = 6.0f * dp;
        // 条目尺寸与 v1.10.159 完全一致（用户要求改回）：左右只留半个拇指再多 3.5dp
        this.inset = thumbR + 3.5f * dp;
        this.centerY = 4.0f * dp + thumbR;
        this.labelGap = 7.0f * dp;
        this.labelBasePx = 10.5f * sp;
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        pTrack.setStyle(Paint.Style.FILL);
        pFill.setStyle(Paint.Style.FILL);
        pThumb.setStyle(Paint.Style.FILL);
        pLabel.setTextSize(10.5f * sp);
        pLabel.setTextAlign(Paint.Align.CENTER);
        pLabelOn.setTextSize(10.5f * sp);
        pLabelOn.setTextAlign(Paint.Align.CENTER);
        pLabelOn.setFakeBoldText(true);

        setFocusable(true);
        setClickable(true);
        setHapticFeedbackEnabled(true);
        applyColors(ThemeManager.INSTANCE.colors(context));
        // 档位文字开局就按已存档位放好（之后由 Fonts.applyOne 逐帧喂过渡值）
        setTextScale(Fonts.committedTextScale(context));
    }

    // ------------------------------------------------------------------ 外观

    /** 主题换肤时调用（深浅色、主色变化都要重刷，不能只在构造时取一次）。 */
    public void applyColors(ThemeColors c) {
        if (c == null) {
            return;
        }
        cTrack = withAlpha(c.getText(), 0.12f);
        cTick = withAlpha(c.getText(), 0.34f);
        cFill = c.getPrimary();
        cLabel = withAlpha(c.getTextMuted(), 0.95f);
        cLabelOn = c.getPrimary();
        Typeface regular = Fonts.typeface(getContext(), false);
        pLabel.setTypeface(regular);
        Typeface bold = Fonts.typeface(getContext(), true);
        if (bold != null) {
            pLabelOn.setTypeface(bold);
        } else {
            pLabelOn.setFakeBoldText(true);
        }
        invalidateGlow();
    }

    // ------------------------------------------------- 供外层光晕层读取的几何

    /** 光晕层：由外层容器在 dispatchDraw 之后绘制（本控件自己装不下 19dp 光晕）。 */
    public void setGlowHost(View host) {
        this.glowHost = host;
    }

    /** 拇指圆心的 x（本控件局部坐标）。 */
    public float thumbCenterX() {
        float left = inset;
        float usable = Math.max(1.0f, getWidth() - inset * 2.0f);
        return left + usable * (value / (float) (STOPS - 1));
    }

    /** 光晕圆心的 y（本控件局部坐标，= 拇指圆心）。 */
    public float glowCenterY() {
        return centerY;
    }

    /** 当前拇指半径（按下时放大 13%）。 */
    public float thumbRadiusNow() {
        return thumbR * (1.0f + 0.13f * pressT);
    }

    /** 三层光晕的圈距（2.4dp，与 v1.10.160 一致）。 */
    public float glowRingGap() {
        return 2.4f * dp;
    }

    /** 光晕颜色（= 拇指主色）。 */
    public int glowColor() {
        return cFill;
    }

    /** 自己重画 + 让光晕层一起重画（否则拖动时光圈不跟手）。 */
    private void invalidateGlow() {
        invalidate();
        if (glowHost != null) {
            glowHost.invalidate();
        }
    }

    private void postInvalidateGlow() {
        postInvalidateOnAnimation();
        if (glowHost != null) {
            glowHost.postInvalidateOnAnimation();
        }
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    public int getIndex() {
        return index;
    }

    /** 外部同步（进入页面 / 换语言后重建）：不打动画。 */
    public void setIndex(int i) {
        this.index = clampIndex(i);
        this.value = this.index;
        this.valueTarget = this.index;
        this.following = false;
        if (this.settleAnim != null) {
            this.settleAnim.cancel();
            this.settleAnim = null;
        }
        invalidateGlow();
    }

    // ------------------------------------------------------------------ 测量

    private float centerY() {
        return centerY;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = resolveSize(Math.round(240.0f * dp), widthSpec);
        Paint.FontMetrics fm = pLabel.getFontMetrics();
        float labelH = fm.descent - fm.ascent;
        // v1.10.161：高度公式回到 v1.10.159（光晕现在只在 own bounds 内，不再需要额外空间）
        float height = centerY + thumbR + labelGap + labelH + 3.0f * dp;
        height = Math.max(height, 48.0f * dp);
        // v1.10.160（反馈 1 同类问题）：折叠面板做高度动画时父容器会给 AT_MOST(0..h)，
        // 跟着缩就会被「挤扁一帧」。自己定高，让父容器去裁（= 从上往下揭开）。
        int h = Math.round(height);
        if (MeasureSpec.getMode(heightSpec) == MeasureSpec.EXACTLY) {
            h = MeasureSpec.getSize(heightSpec);
        }
        setMeasuredDimension(width, h);
    }

    /**
     * v1.10.160（反馈 3）：轨道下方「小/默认/中/大」要跟着字号档位一起变。
     *
     * 由 {@link Fonts#applyOne} 统一喂进来 —— 那样「字号过渡动画」每帧都会把
     * 当前系数送到这里，四个字和整页文字一起长大，不用再写第二条动画。
     */
    public void setTextScale(float scale) {
        float s = scale <= 0.0f ? 1.0f : scale;
        if (Math.abs(s - this.labelScale) < 0.002f) {
            return;
        }
        this.labelScale = s;
        pLabel.setTextSize(labelBasePx * s);
        pLabelOn.setTextSize(labelBasePx * s);
        requestLayout();
        invalidateGlow();
    }

    // ------------------------------------------------------------------ 绘制

    @Override
    protected void onDraw(Canvas cv) {
        stepFollow();
        float cy = centerY();
        float left = inset;
        float right = getWidth() - inset;
        if (right <= left + 1.0f) {
            return;
        }
        float usable = right - left;
        float tx = left + usable * (value / (float) (STOPS - 1));

        // v1.10.165（反馈 图1/2/3）：**不再用「覆盖」**。
        //
        // 复盘两次失败：
        //  · v1.10.162/163：先画灰轨道，再用 drawRoundRect 画粉色已选段 ——
        //    圆角矩形两端都带 3dp 圆角，与轨道端点同宽时接缝对不齐，两端各漏一条灰；
        //  · v1.10.164：改成「整条先铺粉、再用灰盖右半」—— 但轨道灰是 **12% 透明**的，
        //    半透明盖在不透明粉上仍然是粉（整条变全粉，图1）；而且「灰矩形」与
        //    「灰圆头」在交界处重叠，两片 12% 叠加出可见的半月台阶（图2/3 那些边）。
        //
        // 正解：**只画一条几何精确的粉色路径**，不与灰色产生任何覆盖关系。
        // 形状 = 轨道 ∩ (x ≤ fillEnd)：左端完整半圆（半径 = trackH/2，圆心 left+rad，
        // 与轨道左端同一组数）、上下边贴住轨道上下边、右端默认**垂直直切**
        // （半圆收尾会在末端上下各留 3dp×3dp 的灰角，就是最初那个「漏一点」），
        // 只有真的走到最右档才补右端半圆。
        float rad = trackH * 0.5f;
        float fillEnd = tx + thumbR * 0.16f;
        if (fillEnd < left + trackH) {
            fillEnd = left + trackH;
        }
        if (fillEnd > right) {
            fillEnd = right;
        }

        // ① 轨道（灰，画在卡片底上，颜色语义不变）
        pTrack.setColor(cTrack);
        box.set(left, cy - rad, right, cy + rad);
        cv.drawRoundRect(box, rad, rad, pTrack);

        // ② 已选段：一条路径，一次填充
        boolean full = fillEnd >= right - 0.5f;
        fillPath.reset();
        fillPath.moveTo(left + rad, cy - rad);
        fillPath.lineTo(full ? right - rad : fillEnd, cy - rad);
        if (full) {
            oval.set(right - rad * 2.0f, cy - rad, right, cy + rad);
            // 右半圆：12 点 → 3 点 → 6 点（sweep 必须 180°，只写 90° 会缺下半圆）
            fillPath.arcTo(oval, -90.0f, 180.0f, false);
        } else {
            fillPath.lineTo(fillEnd, cy + rad);
        }
        fillPath.lineTo(left + rad, cy + rad);
        oval.set(left, cy - rad, left + rad * 2.0f, cy + rad);
        // 左半圆：6 点 → 9 点 → 12 点
        fillPath.arcTo(oval, 90.0f, 180.0f, false);
        fillPath.close();
        pFill.setColor(cFill);
        cv.drawPath(fillPath, pFill);

        // ③ 档位刻度点：只画在未选段上（已选段内不画，免得粉条上露灰点）
        for (int i = 0; i < STOPS; i++) {
            float x = left + usable * i / (float) (STOPS - 1);
            if (Math.abs(x - tx) < thumbR * 0.95f) {
                continue;
            }
            if (x < fillEnd) {
                continue;
            }
            pTrack.setColor(cTick);
            cv.drawCircle(x, cy, trackH * 0.36f, pTrack);
        }

        // v1.10.162（反馈）：光晕**不在这里画**。
        // 本控件的上下边只有 14.5dp，而完整光晕要 19dp —— 画在自己身上必被裁；
        // 上一版把它缩到 13.5dp 想「缩进空间里」，结果可见环只剩 3dp、alpha 5%，
        // 肉眼等于没有（用户判「直接把光晕删了」）。
        // 现在交给外层 GlowRowLayout 在子视图画完之后画：它的 bounds 是整行，
        // 光晕能完整画出来，而且仍然盖在轨道上、压在拇指下面。
        float r = thumbR * (1.0f + 0.13f * pressT);
        pThumb.setColor(cFill);
        cv.drawCircle(tx, cy, r, pThumb);

        // 档位文字：按控件自身几何 clamp，两端不会贴边被切
        Paint.FontMetrics fm = pLabel.getFontMetrics();
        float baseline = cy + thumbR + labelGap - fm.ascent;
        for (int i = 0; i < STOPS; i++) {
            String s = getContext().getString(Fonts.textScaleLabelRes(i));
            boolean on = i == index;
            Paint p = on ? pLabelOn : pLabel;
            p.setColor(on ? cLabelOn : cLabel);
            float tw = p.measureText(s);
            float minX = tw * 0.5f + 1.0f * dp;
            float maxX = getWidth() - tw * 0.5f - 1.0f * dp;
            float x = left + usable * i / (float) (STOPS - 1);
            if (x < minX) {
                x = minX;
            } else if (x > maxX) {
                x = maxX;
            }
            cv.drawText(s, x, baseline, p);
        }
    }

    /** 拖动跟手：每帧把 value 往 valueTarget 逼近 40%，60Hz 下约 60ms 收敛。 */
    private void stepFollow() {
        if (!following) {
            return;
        }
        float d = valueTarget - value;
        if (Math.abs(d) < 0.004f) {
            value = valueTarget;
            following = false;
        } else {
            value += d * 0.40f;
            postInvalidateGlow();
        }
        int ni = clampIndex(Math.round(value));
        if (ni != index) {
            index = ni;
            tick();
            if (listener != null) {
                listener.onPreview(ni);
            }
        }
    }

    // ------------------------------------------------------------------ 触摸

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!isEnabled()) {
            return false;
        }
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                cancelSettle();
                following = false;
                valueTarget = value;
                downX = e.getX();
                downY = e.getY();
                dragging = false;
                animatePress(true);
                // 注意：这里**不**抢父容器的手势。纵向拖动要让 ScrollView 正常接管，
                // 只有确认是横向拖动（MOVE 里判定）才 requestDisallowIntercept。
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (!dragging) {
                    float dx = Math.abs(e.getX() - downX);
                    float dy = Math.abs(e.getY() - downY);
                    if (dx > touchSlop && dx > dy) {
                        dragging = true;
                        if (getParent() != null) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                        }
                    }
                }
                if (dragging) {
                    float x = e.getX() - inset;
                    float usable = Math.max(1.0f, getWidth() - inset * 2.0f);
                    valueTarget = clampValue(x / usable * (STOPS - 1));
                    following = true;
                    postInvalidateGlow();
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                animatePress(false);
                int target = dragging ? clampIndex(Math.round(value)) : nearest(e.getX());
                boolean wasDragging = dragging;
                dragging = false;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                if (!wasDragging && Math.abs(target - index) == 0) {
                    // 点当前档：不改值，只把标签从「预览压暗」恢复
                    value = index;
                    following = false;
                    invalidateGlow();
                    deliverSettle(index);
                    return true;
                }
                settleTo(target);
                return true;
            }
            case MotionEvent.ACTION_CANCEL: {
                animatePress(false);
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                if (!dragging) {
                    // 被父容器抢走（用户在滑动页面），不算一次选择：回原位、不落库
                    following = false;
                    value = index;
                    valueTarget = index;
                    invalidateGlow();
                    return true;
                }
                int target = clampIndex(Math.round(value));
                dragging = false;
                settleTo(target);
                return true;
            }
            default:
                return true;
        }
    }

    private int nearest(float x) {
        float usable = Math.max(1.0f, getWidth() - inset * 2.0f);
        return clampIndex(Math.round((x - inset) / usable * (STOPS - 1)));
    }

    private void settleTo(final int target) {
        cancelSettle();
        following = false;
        boolean changed = target != index;
        index = target;
        valueTarget = target;
        if (changed) {
            tick();
            if (listener != null) {
                listener.onPreview(target);
            }
        }
        final float from = value;
        final float to = target;
        if (Math.abs(from - to) < 0.001f || dur(DUR_SETTLE) <= 0L) {
            value = to;
            invalidateGlow();
            deliverSettle(target);
            return;
        }
        ValueAnimator anim = ValueAnimator.ofFloat(0.0f, 1.0f);
        anim.setDuration(dur(DUR_SETTLE));
        anim.addUpdateListener(a -> {
            float t = Ease.IN_OUT.at(a.getAnimatedFraction());
            value = from + (to - from) * t;
            invalidateGlow();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                value = to;
                invalidateGlow();
                deliverSettle(target);
            }
        });
        settleAnim = anim;
        anim.start();
    }

    /**
     * 落档回调。**总是**通知业务层（哪怕目标就是当前档）——
     * 业务层自己用 `shown[0] == progress` 去重；这里如果也去重，
     * 「拖出去又拖回原档」会留下一个永远压暗的预览标签。
     */
    private void deliverSettle(int target) {
        if (listener != null) {
            listener.onSettle(target);
        }
    }

    private void cancelSettle() {
        if (settleAnim != null) {
            settleAnim.cancel();
            settleAnim = null;
        }
    }

    private void animatePress(boolean down) {
        if (pressAnim != null) {
            pressAnim.cancel();
            pressAnim = null;
        }
        final float from = pressT;
        final float to = down ? 1.0f : 0.0f;
        if (dur(DUR_PRESS) <= 0L) {
            pressT = to;
            invalidateGlow();
            return;
        }
        ValueAnimator anim = ValueAnimator.ofFloat(0.0f, 1.0f);
        anim.setDuration(dur(DUR_PRESS));
        anim.addUpdateListener(a -> {
            float t = Ease.OUT.at(a.getAnimatedFraction());
            pressT = from + (to - from) * t;
            invalidateGlow();
        });
        pressAnim = anim;
        anim.start();
    }

    private void tick() {
        try {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------ 无障碍

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (info == null) {
            return;
        }
        info.setClassName(SeekBar.class.getName());
        info.setEnabled(isEnabled());
        info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(
                AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT, 0.0f,
                (float) (STOPS - 1), (float) index));
        info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);
        String name = getContext().getString(R.string.settings_font_size);
        String now = getContext().getString(Fonts.textScaleLabelRes(index));
        info.setContentDescription(name + "，" + now);
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle args) {
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
            jumpTo(index + 1);
            return true;
        }
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            jumpTo(index - 1);
            return true;
        }
        if (action == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId()) {
            float v = args == null ? (float) index
                    : args.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, (float) index);
            jumpTo(Math.round(v));
            return true;
        }
        return super.performAccessibilityAction(action, args);
    }

    private void jumpTo(int target) {
        int t = clampIndex(target);
        if (t == index) {
            return;
        }
        settleTo(t);
    }

    // ------------------------------------------------------------------ 工具

    private float clampValue(float v) {
        if (v < 0.0f) {
            return 0.0f;
        }
        return v > (float) (STOPS - 1) ? (float) (STOPS - 1) : v;
    }

    private int clampIndex(int i) {
        if (i < 0) {
            return 0;
        }
        return i > STOPS - 1 ? STOPS - 1 : i;
    }

    /** 「减少动态效果」：系统动画时长倍数为 0 时全部归零。 */
    private long dur(long base) {
        float scale = 1.0f;
        try {
            scale = Settings.Global.getFloat(getContext().getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f);
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

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(255.0f * (alpha < 0.0f ? 0.0f : (alpha > 1.0f ? 1.0f : alpha)));
        return (color & 0x00FFFFFF) | (a << 24);
    }
}
