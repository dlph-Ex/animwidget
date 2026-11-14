package io.github.animwidget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * 素材类型选择：液态玻璃分段控件。
 *
 * 结构（和 Kyant 那套 SystemUI 的观感对齐，但不依赖 Compose / 第三方库）：
 *   1. 外层胶囊：半透明底色 + 发丝描边；
 *   2. 选中块：玻璃胶囊（半透明白 + 主色微染 + 顶部高光 + 落地阴影）；
 *   3. 切换时胶囊按 Decelerate 插值滑动，并在中段横向拉伸（液态），松手回弹；
 *   4. 文字颜色在 旧色 <-> 主色 之间按距离插值。
 *
 * 深浅色靠 textColor 的亮度判断，颜色全部来自 ThemeManager.applyColors，
 * 所以六套预设 + 反转 + 对比度都直接生效。
 */
public final class MediaSelectorView extends View {

    private static final MediaKind[] KINDS = {MediaKind.VIDEO, MediaKind.GIF, MediaKind.IMAGE};

    private String[] labels = {"视频", "GIF", "图片"};
    private Function1<? super MediaKind, Unit> onKindClick;

    private int primary = 0xFF16A34A;
    private int neutral = 0xFFE7EAEE;
    private int textColor = 0xFF1F2937;
    private int onPrimary = 0xFFFFFFFF;

    private MediaKind selected;
    private int fromIndex = -1;
    private int toIndex = -1;
    private float progress = 1.0f;

    private ValueAnimator animator;

    private final float density;
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glassPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF trackRect = new RectF();
    private final RectF pillRect = new RectF();
    private final RectF shineRect = new RectF();

    public MediaSelectorView(Context context) {
        this(context, null);
    }

    public MediaSelectorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.density = getResources().getDisplayMetrics().density;
        trackPaint.setStyle(Paint.Style.FILL);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(Math.max(1f, density));
        glassPaint.setStyle(Paint.Style.FILL);
        shinePaint.setStyle(Paint.Style.FILL);
        shadowPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        setClickable(true);
        setFocusable(true);
    }

    public Function1<? super MediaKind, Unit> getOnKindClick() {
        return onKindClick;
    }

    public void setOnKindClick(Function1<? super MediaKind, Unit> callback) {
        this.onKindClick = callback;
    }

    public void applyColors(int primary, int neutral, int text, int onPrimary) {
        this.primary = primary;
        this.neutral = neutral;
        this.textColor = text;
        this.onPrimary = onPrimary;
        invalidate();
    }

    public void setLabels(String[] values) {
        if (values != null && values.length > 0) {
            this.labels = values;
            invalidate();
        }
    }

    public void setSelected(MediaKind kind, boolean animate) {
        int index = indexOf(kind);
        if (index < 0) {
            this.selected = null;
            this.fromIndex = -1;
            this.toIndex = -1;
            this.progress = 1.0f;
            cancelAnimator();
            invalidate();
            return;
        }
        boolean same = this.selected == kind;
        this.selected = kind;
        int start = (fromIndex >= 0 && progress < 1f) ? roundCurrent() : this.toIndex;
        if (start < 0) {
            start = index;
        }
        if (!animate || same) {
            cancelAnimator();
            this.fromIndex = index;
            this.toIndex = index;
            this.progress = 1.0f;
            invalidate();
            return;
        }
        cancelAnimator();
        // 第一次选中没有"起点"，就让它原地弹一下（拉伸回弹），也有反馈
        this.fromIndex = start == index ? index : start;
        this.toIndex = index;
        this.progress = 0.0f;
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimator.setDuration(320L);
        valueAnimator.setInterpolator(new DecelerateInterpolator(1.6f));
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                MediaSelectorView.this.progress = ((Float) animation.getAnimatedValue()).floatValue();
                MediaSelectorView.this.invalidate();
            }
        });
        valueAnimator.start();
        this.animator = valueAnimator;
    }

    public static /* synthetic */ void setSelected$default(MediaSelectorView view, MediaKind kind, boolean animate, int mask, Object marker) {
        if ((mask & 2) != 0) {
            animate = true;
        }
        view.setSelected(kind, animate);
    }

    public MediaKind getSelectedKind() {
        return selected;
    }

    private void cancelAnimator() {
        ValueAnimator running = this.animator;
        if (running != null) {
            running.cancel();
            this.animator = null;
        }
    }

    private static int indexOf(MediaKind kind) {
        if (kind == null) {
            return -1;
        }
        for (int i = 0; i < KINDS.length; i++) {
            if (KINDS[i] == kind) {
                return i;
            }
        }
        return -1;
    }

    private int roundCurrent() {
        float value = fromIndex + (toIndex - fromIndex) * progress;
        return Math.round(value);
    }

    private boolean isDark() {
        int r = Color.red(textColor);
        int g = Color.green(textColor);
        int b = Color.blue(textColor);
        return (r * 299 + g * 587 + b * 114) / 1000 > 128;
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    private static int blend(int c1, int c2, float t) {
        float inv = 1f - t;
        return Color.argb(
                (int) (Color.alpha(c1) * inv + Color.alpha(c2) * t),
                (int) (Color.red(c1) * inv + Color.red(c2) * t),
                (int) (Color.green(c1) * inv + Color.green(c2) * t),
                (int) (Color.blue(c1) * inv + Color.blue(c2) * t));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        boolean dark = isDark();
        float radius = height / 2f;
        float inset = Math.max(3f * density, height * 0.06f);

        // 1. 轨道：一条安静的浅色底，不带描边，避免和卡片打架
        trackRect.set(0f, 0f, width, height);
        trackPaint.setColor(dark ? withAlpha(0xFFFFFF, 0x12) : withAlpha(neutral, 0x8C));
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint);

        float segment = (width - inset * 2f) / KINDS.length;
        float position = fromIndex < 0 ? -1f : fromIndex + (toIndex - fromIndex) * progress;

        // 2. 玻璃胶囊
        if (position >= 0f) {
            float stretch = (float) Math.sin(Math.PI * Math.min(1f, Math.max(0f, progress))) * 7f * density;
            float left = inset + segment * position - stretch;
            float right = left + segment + stretch * 2f;
            float top = inset;
            float bottom = height - inset;
            pillRect.set(left, top, right, bottom);
            float pillRadius = (bottom - top) / 2f;

            // 落地阴影（浅色下才需要，深色里加了会发灰）
            if (!dark) {
                shineRect.set(pillRect.left, pillRect.top + 1.2f * density, pillRect.right, pillRect.bottom + 2.2f * density);
                shadowPaint.setColor(0x1F000000);
                canvas.drawRoundRect(shineRect, pillRadius, pillRadius, shadowPaint);
            }

            // 实心胶囊：neutral 与主色按比例混合，不叠多层半透明（那样会发脏）
            int base = dark ? neutral : 0xFFFFFFFF;
            int pillColor = blend(base, primary, dark ? 0.30f : 0.12f);
            glassPaint.setColor(dark ? withAlpha(pillColor, 0xF2) : pillColor);
            canvas.drawRoundRect(pillRect, pillRadius, pillRadius, glassPaint);

            // 顶部一条极细高光，只做"凸起"暗示
            shinePaint.setShader(new LinearGradient(0f, top, 0f, top + (bottom - top) * 0.45f,
                    dark ? 0x1CFFFFFF : 0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(pillRect, pillRadius, pillRadius, shinePaint);
            shinePaint.setShader(null);

            // 主色细描边
            strokePaint.setColor(withAlpha(primary, dark ? 0x59 : 0x3D));
            strokePaint.setStrokeWidth(Math.max(1f, density));
            canvas.drawRoundRect(pillRect, pillRadius, pillRadius, strokePaint);
        }

        // 3. 文字
        float textSize = Math.min(height * 0.34f, 16f * density);
        textPaint.setTextSize(textSize);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = height / 2f - (metrics.ascent + metrics.descent) / 2f;
        for (int i = 0; i < KINDS.length && i < labels.length; i++) {
            float centerX = inset + segment * (i + 0.5f);
            float distance = position < 0f ? 1f : Math.min(1f, Math.abs(position - i));
            float selectedWeight = 1f - Math.min(1f, distance * 1.6f);
            int unselected = dark ? withAlpha(textColor, 0x99) : withAlpha(textColor, 0x8C);
            textPaint.setColor(blend(unselected, primary, selectedWeight));
            textPaint.setFakeBoldText(selectedWeight > 0.5f);
            canvas.drawText(labels[i], centerX, baseline, textPaint);
        }
        textPaint.setFakeBoldText(false);
    }

    private RectF shadowRect() {
        return shineRect;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                return true;
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                return true;
            case MotionEvent.ACTION_UP:
                setPressed(false);
                int index = indexAt(event.getX());
                if (index >= 0) {
                    MediaKind kind = KINDS[index];
                    // 先自己滑动（这是手感来源），再把事件交给 Activity
                    setSelected(kind, true);
                    Function1<? super MediaKind, Unit> callback = this.onKindClick;
                    if (callback != null) {
                        callback.invoke(kind);
                    }
                }
                performClick();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private int indexAt(float x) {
        int width = getWidth();
        if (width <= 0) {
            return -1;
        }
        float inset = Math.max(3f * density, getHeight() * 0.06f);
        float segment = (width - inset * 2f) / KINDS.length;
        if (segment <= 0f) {
            return -1;
        }
        int index = (int) ((x - inset) / segment);
        return (index < 0 || index >= KINDS.length) ? -1 : index;
    }
}
