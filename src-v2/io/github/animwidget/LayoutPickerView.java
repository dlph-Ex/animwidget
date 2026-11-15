package io.github.animwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.ViewOutlineProvider;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * 布局选择器（v1.8.0 重做）。
 *
 * 每张卡片里画两块圆角图形（v1.10.93 按用户给的图 2 重做）：
 *   · **后景**＝该布局的框比例（原始比例/填充用组件比例），取法与尺寸规则与以前一致，
 *     只整体缩到 0.88 给前景腾地方；
 *   · **前景**＝**素材比例**（实时跟着素材变），面积与后景挂钩，所以两块体量相当；
 *     素材未知、或按素材比例放不进这一格 → 退化成用户指定的**默认标准 3:2**；
 *   · 两块并排、少量重叠（38%）、右下错开，整体包围盒在格子里居中，
 *     前景最后再夹回格子 —— **比例精确、不越界、不出现错位**。
 * 滑动停止后自动吸附到最近的卡片，点一下也能直接选中。
 */
public final class LayoutPickerView extends HorizontalScrollView {

    /** 选中年份布局时回调。 */
    interface Listener {
        void onPick(LayoutMode mode);

        /** v1.10.500：**先问闸再改自身选中态** —— 输出中必须连高亮都不许动。 */
        boolean blockPick(LayoutMode mode);
    }

    private static final int COLOR_PHOTO_DARK = 0xFFE9E9EE;
    private static final int COLOR_PHOTO_LIGHT = 0xFFD3D3DC;

    /** 「原始比例」卡片的默认前景：3:2（素材未知 / 放不进格子时用）。 */
    private static final float DEFAULT_MEDIA_RATIO = 1.5f;
    /** 其余卡片的默认前景：**2:4**（= 0.5 竖版，用户 v1.10.95 指定）。 */
    private static final float DEFAULT_OTHER_RATIO = 0.5f;
    /** 后景相对原尺寸的系数（给前景腾地方）。 */
    private static final float BACK_SCALE = 0.88f;
    /** 前景面积 = 后景面积 × 这个系数（两块体量相当，像图 2）。 */
    private static final float FRONT_AREA_RATIO = 0.85f;
    /** 前景与后景的重叠量（占前景宽度）。 */
    private static final float FRONT_OVERLAP = 0.38f;
    /** 前景相对后景的竖直错开（占后景高度）。 */
    private static final float FRONT_DY = 0.18f;
    /** 前景最大尺寸（占格子）—— 保证一定装得下。 */
    private static final float FRONT_MAX_W = 0.50f;
    private static final float FRONT_MAX_H = 0.66f;

    private final LinearLayout row;
    private final List<Card> cards = new ArrayList<>();
    private final Paint paint = new Paint(1);
    private final RectF rect = new RectF();
    private final Path arrowPath = new Path();
    /** v1.10.522：卡片底的「分组圆角」路径（四角半径可以各不相同）。 */
    private final Path cardPath = new Path();

    private LayoutMode selected = LayoutMode.FIT;
    private float mediaRatio = 1.0f;
    /** v1.10.95：**精确素材比例**（0 = 还没有素材）。所有卡片共用：0 时各卡片画自己的默认前景。 */
    private float exactMediaRatio = 0.0f;
    private float widgetRatio = 1.7777778f;
    private Listener listener;
    private ThemeColors colors;

    public LayoutPickerView(Context context) {
        super(context);
        setHorizontalScrollBarEnabled(false);
        // v1.8.1：允许滑出边界多一点、松手自然回弹（原来自己写吸附，反而像"被拉回"）
        setOverScrollMode(OVER_SCROLL_ALWAYS);
        setClipToPadding(false);
        int pad = dp(2.0f);
        setPadding(pad, 0, pad, 0);
        row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        // HorizontalScrollView 是 FrameLayout 的子类，子 View 必须用 FrameLayout.LayoutParams，
        // 否则 onMeasure 里强转会 ClassCastException。
        addView(row, new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        colors = ThemeManager.INSTANCE.colors(context);
        // v1.8.4：滑动到两端时卡片会被视口按直角裁切。把视口本身做圆角裁剪，
        // 进出的卡片边缘就跟着圆角走。
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                float r = 18.0f * view.getResources().getDisplayMetrics().density;
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), r);
            }
        });
        build(context);

    }

    public LayoutPickerView(Context context, AttributeSet attrs) {
        this(context);
    }

    /** 每个布局一张卡片：宽 146dp、高 196dp（比原来的 chip 高一截）。 */
    private void build(Context context) {
        float density = getResources().getDisplayMetrics().density;
        int cardWidth = (int) (146.0f * density);
        int cardHeight = (int) (150.0f * density);
        int gap = (int) (8.0f * density);
        LayoutMode[] all = LayoutMode.values();
        for (int index = 0; index < all.length; index++) {
            LayoutMode mode = all[index];
            // v1.10.522：卡片之间改**分组圆角** —— 最左/最右两张的外侧角大（18dp），
            // 相邻的内侧角小（8dp），中间的卡片四角都是 8dp。
            int slot = all.length <= 1 ? ThemeManager.GROUP_SINGLE
                    : (index == 0 ? ThemeManager.GROUP_FIRST
                    : (index == all.length - 1 ? ThemeManager.GROUP_LAST : ThemeManager.GROUP_MIDDLE));
            Card card = new Card(context, mode, slot);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cardWidth, cardHeight);
            lp.rightMargin = gap;
            row.addView(card, lp);
            cards.add(card);
            card.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Card card2 = (Card) v;
                    // v1.10.500（用户反馈「弹了对话框但布局还是切了」）：
                    // 原来 pick() 先把 selected 改成新值、再通知 listener；listener 里判 outputRunning
                    // 已经太晚 —— 选中高亮与 chip 行已经切了，只有 selectedLayout 没变（预览框不动）。
                    // 现在闸放在**改状态之前**：blockPick 说不行就一个像素都不变。
                    Listener l = listener;
                    if (l != null && l.blockPick(card2.mode)) {
                        return;
                    }
                    card2.animate().scaleX(0.94f).scaleY(0.94f).setDuration(70L).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            card2.animate().scaleX(1.0f).scaleY(1.0f).setDuration(110L).start();
                        }
                    }).start();
                    pick(card2.mode, true);
                }
            });
        }
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void setSelected(LayoutMode mode) {
        pick(mode, false);
    }

    private void pick(LayoutMode mode, boolean notify) {
        if (mode == null) {
            return;
        }
        this.selected = mode;
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).invalidate();
        }
        if (notify && listener != null) {
            listener.onPick(mode);
        }
    }

    LayoutMode getSelected() {
        return selected;
    }

    void setMediaRatio(float ratio) {
        if (ratio <= 0.0f) {
            return;
        }
        if (Math.abs(this.mediaRatio - ratio) < 0.001f) {
            return;
        }
        this.mediaRatio = ratio;
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).invalidate();
        }
    }

    /** v1.10.95：精确素材比例（0 = 没有素材）。布局示意图用它决定前景画素材比例还是默认形状。 */
    void setMediaExactRatio(float ratio) {
        float value = ratio < 0.0f ? 0.0f : ratio;
        if (Math.abs(this.exactMediaRatio - value) < 0.001f) {
            return;
        }
        this.exactMediaRatio = value;
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).invalidate();
        }
    }

    void setWidgetRatio(float ratio) {
        if (ratio <= 0.0f) {
            return;
        }
        if (Math.abs(this.widgetRatio - ratio) < 0.001f) {
            return;
        }
        this.widgetRatio = ratio;
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).invalidate();
        }
    }

    void applyTheme() {
        this.colors = ThemeManager.INSTANCE.colors(getContext());
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).invalidate();
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    /** 单张卡片：上面画比例示意，下面两行字。 */
    private final class Card extends View {

        final LayoutMode mode;
        /** v1.10.522：在横排里的位置（决定四个角哪个大）。 */
        final int slot;
        /** 每张卡自己的四角半径（分组圆角），onDraw 里直接用。 */
        private final float[] cornerRadii = new float[8];

        /** v1.10.95：layoutBlocks() 算出来的两块矩形（画的时候直接用）。 */
        private float bkX;
        private float bkY;
        private float bkW;
        private float bKH;
        private float frX;
        private float frY;
        private float frW;
        private float frH;

        Card(Context context, LayoutMode mode, int slot) {
            super(context);
            this.mode = mode;
            this.slot = slot;
        }

        private float downX;
        private float downY;
        private boolean pressed;
        /** v1.10.176：上一次打日志时的真实字号（px）。 */
        private float loggedTitle = -1.0f;
        private float loggedHint = -1.0f;
        private long loggedAt;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float slop = 10.0f * getResources().getDisplayMetrics().density;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    downY = event.getY();
                    pressed = true;
                    invalidate();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (pressed && (Math.abs(event.getX() - downX) > slop || Math.abs(event.getY() - downY) > slop)) {
                        pressed = false;
                        invalidate();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (pressed) {
                        pressed = false;
                        invalidate();
                        performClick();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    pressed = false;
                    invalidate();
                    return true;
                default:
                    return super.onTouchEvent(event);
            }
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            ThemeColors c = colors != null ? colors : ThemeManager.INSTANCE.colors(getContext());
            boolean isSelected = mode == selected;
            float density = getResources().getDisplayMetrics().density;
            float w = getWidth();
            float h = getHeight();
            float radius = 18.0f * density;
            // v1.10.522：分组圆角 —— 外侧 18dp、相邻内侧 8dp。
            // 横排里「第一个」只圆左端两角、「最后一个」只圆右端两角，中间四角 8dp。
            float outer = radius;
            float inner = 8.0f * density;
            float tl;
            float tr;
            float br;
            float bl;
            if (slot == ThemeManager.GROUP_FIRST) {
                tl = outer; tr = inner; br = inner; bl = outer;
            } else if (slot == ThemeManager.GROUP_LAST) {
                tl = inner; tr = outer; br = outer; bl = inner;
            } else if (slot == ThemeManager.GROUP_MIDDLE) {
                tl = inner; tr = inner; br = inner; bl = inner;
            } else {
                tl = outer; tr = outer; br = outer; bl = outer;
            }
            this.cornerRadii[0] = tl; this.cornerRadii[1] = tl;
            this.cornerRadii[2] = tr; this.cornerRadii[3] = tr;
            this.cornerRadii[4] = br; this.cornerRadii[5] = br;
            this.cornerRadii[6] = bl; this.cornerRadii[7] = bl;

            // 卡片底 + 选中描边
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(isSelected ? c.getSurface() : c.getNeutral());
            rect.set(0.0f, 0.0f, w, h);
            cardPath.reset();
            cardPath.addRoundRect(rect, this.cornerRadii, Path.Direction.CW);
            canvas.drawPath(cardPath, paint);
            if (isSelected) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.0f * density);
                paint.setColor(c.getPrimary());
                rect.set(1.0f * density, 1.0f * density, w - 1.0f * density, h - 1.0f * density);
                cardPath.reset();
                cardPath.addRoundRect(rect, this.cornerRadii, Path.Direction.CW);
                canvas.drawPath(cardPath, paint);
            }

            if (pressed) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0x22FFFFFF);
                rect.set(0.0f, 0.0f, w, h);
                cardPath.reset();
                cardPath.addRoundRect(rect, this.cornerRadii, Path.Direction.CW);
                canvas.drawPath(cardPath, paint);
            }

            // v1.10.174（反馈 图1）：布局卡片的两行字**跟随字号档位**，并且不再贴底。
            // 原来：标题 14dp / 副标题 10.5dp 写死；基线写死 h-26dp / h-10dp ——
            //   · 不跟随「字体大小」，整页其它字放大后这里显得又小又挤；
            //   · 副标题字底离卡片边只剩约 7dp，而左右 padding 是 14dp（图1 白框框的就是这行）。
            // 现在：两行按档位缩放，超卡片可用宽度就按比例缩回；字块整体从底边按 14dp 往上排，
            // 示意图区域自动让位到标题上方。
            float pad = 14.0f * density;
            // v1.10.175（反馈「字体字号都没跟系统」）：**字号基数必须是 scaledDensity**。
            // density 只是屏幕密度，系统「字体大小」的倍率在 scaledDensity 里（= density × fontScale）。
            // 之前这里用 density，所以系统把字体调大，卡片里的字一动不动 —— 几何（pad/卡片尺寸）
            // 仍然用 density，只有字号换成 sp，这样布局不变、字跟着系统走。
            float sp = getResources().getDisplayMetrics().scaledDensity;
            float scale = Fonts.textScale(getContext());
            String title = mode.label(getContext());
            String hintText = hint();
            float avail = Math.max(1.0f, w - pad * 2.0f);
            float titleSize = 14.0f * sp * scale;
            float hintSize = 10.5f * sp * scale;

            paint.setTextAlign(Paint.Align.CENTER);
            // v1.10.176（反馈）：自绘的两行字也要用「字体」设置里选的字体
            // （跟随系统 → null，走系统默认字形；选了内置/导入字体 → 同一套字形）。
            android.graphics.Typeface tfBold = Fonts.typeface(getContext(), true);
            android.graphics.Typeface tfNormal = Fonts.typeface(getContext(), false);
            paint.setTypeface(tfBold != null ? tfBold : android.graphics.Typeface.DEFAULT_BOLD);
            paint.setTextSize(titleSize);
            float titleW = paint.measureText(title);
            if (titleW > avail && titleW > 0.0f) {
                titleSize *= avail / titleW;
            }
            paint.setTypeface(tfNormal != null ? tfNormal : android.graphics.Typeface.DEFAULT);
            paint.setTextSize(hintSize);
            float hintW = paint.measureText(hintText);
            if (hintW > avail && hintW > 0.0f) {
                hintSize *= avail / hintW;
            }
            // v1.10.176：把**真实落点**写进日志（不是自己补的系数）——
            // 系统字体或档位一动，这里的 dp 必须跟着变，下一轮直接用日志判定。
            logSizesOnce(titleSize, hintSize, sp, density);

            // 从底边往上排：副标题 → 标题
            paint.setTextSize(hintSize);
            Paint.FontMetrics hfm = paint.getFontMetrics();
            float hintBase = (h - pad) - hfm.descent;
            float hintTop = hintBase + hfm.ascent;
            paint.setTextSize(titleSize);
            Paint.FontMetrics tfm = paint.getFontMetrics();
            float titleBase = hintTop - 3.0f * density - tfm.descent;
            float titleTop = titleBase + tfm.ascent;

            // 示意图区域：让到标题上方
            float areaBottom = titleTop - 8.0f * density;
            RectF area = new RectF(pad, pad, w - pad,
                    Math.max(pad + 1.0f, areaBottom));
            drawIllustration(canvas, area, density, c, isSelected);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(isSelected ? c.getPrimary() : c.getText());
            paint.setTextSize(titleSize);
            canvas.drawText(title, w / 2.0f, titleBase, paint);

            paint.setColor(c.getTextMuted());
            paint.setTextSize(hintSize);
            canvas.drawText(hintText, w / 2.0f, hintBase, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        /** v1.10.176：真实落点日志（同一组尺寸只打一次，跟随系统/档位一改就会再打一条）。 */
        private void logSizesOnce(float titlePx, float hintPx, float sp, float density) {
            if (Math.abs(titlePx - this.loggedTitle) < 0.5f
                    && Math.abs(hintPx - this.loggedHint) < 0.5f) {
                return;
            }
            if (android.os.SystemClock.uptimeMillis() - this.loggedAt < 600L) {
                return;
            }
            this.loggedTitle = titlePx;
            this.loggedHint = hintPx;
            this.loggedAt = android.os.SystemClock.uptimeMillis();
            // Diag 是同步写文件，不能在 onDraw 里直接调 → post 到下一帧
            final String detail = "字号落点 布局卡片"
                    + " 标题=" + one(titlePx / density) + "dp"
                    + " 副标题=" + one(hintPx / density) + "dp"
                    + " 卡片=" + one(getWidth() / density) + "x" + one(getHeight() / density) + "dp"
                    + " density=" + one(density) + " scaledDensity=" + one(sp)
                    + " 档位=×" + one(Fonts.textScale(getContext()));
            post(() -> Diag.warn(getContext(), "A09", detail));
        }

        private String one(float value) {
            return String.format(java.util.Locale.US, "%.2f", Float.valueOf(value));
        }

        private String hint() {
            if (mode == LayoutMode.FIT) {
                return getContext().getString(R.string.layout_hint_fit);
            }
            if (mode == LayoutMode.FILL) {
                return getContext().getString(R.string.layout_hint_fill);
            }
            return getContext().getString(R.string.layout_hint_ratio);
        }

        /**
         * 示意图（v1.10.95）。
         *
         * **箭头（↗↙ 两枚圆头）只画在「原始比例」卡片里**，填充和其它卡片一律没有。
         * 所有卡片共用同一套几何，两块钱币式叠放、比例精确、不越位：
         *   · 后景＝该布局的框比例（原始比例/填充用组件比例）；
         *   · 前景＝**素材比例**；没有素材、或按素材比例放不进这一格 → 用各自的默认形状：
         *       原始比例 = **3:2**，其余卡片 = **2:4（0.5 竖版）**；
         *   · 前景面积与后景挂钩（体量相当）、与后景重叠 38%、竖直错开 18%，
         *     整体包围盒在格子里居中，最后把前景夹回格子（只平移，不改比例）。
         */
        private void drawIllustration(Canvas canvas, RectF area, float density, ThemeColors c, boolean selected) {
            // v1.10.97：「原始比例」「填充」都只留**一块**居中图形，自动放大/缩小铺满单格。
            //   原始比例 = 素材比例（默认 3:2）+ ↗↙ 箭头
            //   填充     = 组件比例 + 剪刀（**只在这一块上**，不再跨两块）
            if (mode == LayoutMode.FIT || mode == LayoutMode.FILL) {
                drawSingleBlock(canvas, area, density, c, mode == LayoutMode.FILL, selected);
                return;
            }
            float frameRatio = mode.getRatio() > 0.0f ? mode.getRatio() : widgetRatio;
            if (frameRatio <= 0.0f) {
                frameRatio = DEFAULT_MEDIA_RATIO;
            }
            float frontRatio = resolveFrontRatio(area);
            boolean usesDefault = frontRatio != exactMediaRatio;
            layoutBlocks(area, frameRatio, frontRatio);
            float corner = 10.0f * density;
            boolean dark = isDarkColor(c.getBackground());
            paint.setStyle(Paint.Style.FILL);

            // ---- 后景
            // v1.10.522：形状图形的颜色恒跟随系统，只有这一种口径
            paint.setColor(systemControlColor(c));
            rect.set(this.bkX, this.bkY, this.bkX + this.bkW, this.bkY + this.bKH);
            canvas.drawRoundRect(rect, corner, corner, paint);

            // ---- 前景（其它卡片保持原来的深色块：选中用主色、未选辅助色）
            final int frontColor = selected ? c.getPrimary() : c.getSecondary();
            paint.setColor(frontColor);
            rect.set(this.frX, this.frY, this.frX + this.frW, this.frY + this.frH);
            canvas.drawRoundRect(rect, corner, corner, paint);

            if (!usesDefault) {
                // 其它卡片沿用原来那个「任意照片」小标记（用默认图形时不画）
                drawPhotoGlyph(canvas, this.frX, this.frY, this.frW, this.frH);
            }
        }

        /**
         * v1.10.97：**单块画法**（原始比例 / 填充共用）。
         *
         * · 只有一块，**单格中央**，按比例**自动放大/缩小铺满这一格**（等比顶到宽或高）；
         * · 原始比例：比例 = 素材比例（未知或 <0.4 / >2.5 时用 3:2），图形里画 ↗↙ 箭头；
         * · 填充：比例 = 组件比例，图形里画「剪刀」——**剪刀只在这一个图层上**，不再跨两块。
         */
        private void drawSingleBlock(Canvas canvas, RectF area, float density, ThemeColors c, boolean fill,
                                   boolean selected) {
            float ratio;
            if (fill) {
                ratio = widgetRatio > 0.0f ? widgetRatio : DEFAULT_MEDIA_RATIO;
            } else {
                ratio = exactMediaRatio;
                if (ratio <= 0.01f || ratio < 0.4f || ratio > 2.5f) {
                    ratio = DEFAULT_MEDIA_RATIO;
                }
            }
            // v1.10.180（反馈 1）：图形**整体缩小并居中**，四周留呼吸空间。
            // 原来 w 直接吃满 area（卡片宽 − 2×14dp），于是图形几乎顶到卡片边；
            // 现在取 area 的 74%（卡片宽约 60%）并且纵向也留 20%，
            // 素材比例再扁/再竖都只是「在更小的框里等比缩放」，不会贴边。
            float w = area.width() * 0.74f;
            float h = w / ratio;
            if (h > area.height() * 0.80f) {
                h = area.height() * 0.80f;
                w = h * ratio;
            }
            float x = area.centerX() - w / 2.0f;
            float y = area.centerY() - h / 2.0f;
            float corner = 10.0f * density;
            boolean dark = isDarkColor(c.getBackground());

            // v1.10.525：**「原始比例」和「填充」这两张单块卡用同一条配色规则**。
            //
            //   · 选中   = 主色混白（比 522 更深一档：浅色 0.62 / 深色 0.55）——
            //             522 用 0.80 调出来的那一块几乎和卡片白底同色，选中了也看不出来；
            //   · 未选中 = 系统控件前景色（中性灰）—— 522 用「辅助色混白」得到的是桃色，
            //             印在灰色的未选中卡片上像"这一格还亮着"。
            //
            // 两张卡因此完全对称：不看边框，只看图形颜色就能判断谁被选中。
            final int blockColor = selected
                    ? mixToWhite(c.getPrimary(), dark ? 0.55f : 0.62f)
                    : systemControlColor(c);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(blockColor);
            rect.set(x, y, x + w, y + h);
            canvas.drawRoundRect(rect, corner, corner, paint);

            if (fill) {
                drawScissors(canvas, x, y, w, h, density, blockColor);
            } else {
                // v1.10.525：未选中时底色是深灰，原来那支「按底色压暗」的墨会看不见 ——
                // 与剪刀同一口径：底色暗 → 用亮墨；底色亮 → 用压暗的墨。
                int ink = isDarkColor(blockColor) ? 0x8CFFFFFF : mixToBlack(blockColor, 0.45f);
                drawStretchArrows(canvas, x, y, w, h, density, ink);
            }
        }

        /**
         * 填充卡片的「剪刀」箭头 —— 与 v1.10.92 的画法**逐字一致**
         * （对角线交叉 + 两个箭头帽都在右侧；画在后景/框上）。
         */
        private void drawScissors(Canvas canvas, float bx, float by, float bw, float bh,
                                 float density, int blockColor) {
            // 块深 → 浅墨；块浅 → 深墨（与旧版 dark ? 白 : 黑 的观感一致）
            paint.setColor(isDarkColor(blockColor) ? 0x88FFFFFF : 0x55000000);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.4f * density);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            float cx = bx + bw / 2.0f;
            float cy = by + bh / 2.0f;
            float arm = Math.min(bw, bh) * 0.26f;
            arrowPath.reset();
            arrowPath.moveTo(cx - arm, cy - arm * 0.62f);
            arrowPath.lineTo(cx + arm, cy + arm * 0.62f);
            arrowPath.moveTo(cx - arm, cy + arm * 0.62f);
            arrowPath.lineTo(cx + arm, cy - arm * 0.62f);
            canvas.drawPath(arrowPath, paint);
            canvas.drawPath(arrowHead(cx + arm, cy + arm * 0.62f, 1.0f, 1.0f, arm * 0.42f), paint);
            canvas.drawPath(arrowHead(cx + arm, cy - arm * 0.62f, 1.0f, -1.0f, arm * 0.42f), paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeCap(Paint.Cap.BUTT);
        }

        /**
         * 两块矩形的统一几何（v1.10.95）：后景按框比例、前景按素材比例，
         * 面积挂钩 + 38% 重叠 + 竖直错开 18% + 包围盒居中 + 前景夹回格子。
         */
        private void layoutBlocks(RectF area, float frameRatio, float frontRatio) {
            boolean portraitFront = frontRatio < 1.0f;

            // 后景
            float bw;
            float bh;
            if (frameRatio >= 1.0f) {
                bw = area.width() * 0.66f;
                bh = bw / frameRatio;
                if (bh > area.height() * 0.74f) {
                    bh = area.height() * 0.74f;
                    bw = bh * frameRatio;
                }
            } else {
                bh = area.height() * 0.80f;
                bw = bh * frameRatio;
                if (bw > area.width() * 0.66f) {
                    bw = area.width() * 0.66f;
                    bh = bw / frameRatio;
                }
            }
            bw *= 0.88f;
            bh *= 0.88f;
            float bx = portraitFront ? area.right - bw : area.left;
            float by = portraitFront ? area.bottom - bh : area.top;

            // 前景
            float frontArea = bw * bh * 0.85f;
            float fw = (float) Math.sqrt(frontArea * frontRatio);
            float fh = fw / frontRatio;
            if (fw > area.width() * 0.50f) {
                fw = area.width() * 0.50f;
                fh = fw / frontRatio;
            }
            if (fh > area.height() * 0.66f) {
                fh = area.height() * 0.66f;
                fw = fh * frontRatio;
            }
            float fx;
            float fy;
            if (!portraitFront) {
                fx = bx + bw - fw * 0.62f;
                fy = by + bh * 0.18f;
            } else {
                fx = bx - fw * 0.62f;
                fy = by + bh - bh * 0.18f - fh;
            }

            // 包围盒居中
            float ux0 = Math.min(bx, fx);
            float ux1 = Math.max(bx + bw, fx + fw);
            float uy0 = Math.min(by, fy);
            float uy1 = Math.max(by + bh, fy + fh);
            float dx = area.centerX() - (ux0 + ux1) / 2.0f;
            float dy = area.centerY() - (uy0 + uy1) / 2.0f;
            bx += dx;
            by += dy;
            fx += dx;
            fy += dy;
            // 夹回格子
            ux0 = Math.min(bx, fx);
            ux1 = Math.max(bx + bw, fx + fw);
            uy0 = Math.min(by, fy);
            uy1 = Math.max(by + bh, fy + fh);
            float sx = 0.0f;
            float sy = 0.0f;
            if (ux0 < area.left) {
                sx = area.left - ux0;
            } else if (ux1 > area.right) {
                sx = area.right - ux1;
            }
            if (uy0 < area.top) {
                sy = area.top - uy0;
            } else if (uy1 > area.bottom) {
                sy = area.bottom - uy1;
            }
            bx += sx;
            by += sy;
            fx += sx;
            fy += sy;
            if (fx < area.left) {
                fx = area.left;
            }
            if (fy < area.top) {
                fy = area.top;
            }
            if (fx + fw > area.right) {
                fx = area.right - fw;
            }
            if (fy + fh > area.bottom) {
                fy = area.bottom - fh;
            }

            this.bkX = bx;
            this.bkY = by;
            this.bkW = bw;
            this.bKH = bh;
            this.frX = fx;
            this.frY = fy;
            this.frW = fw;
            this.frH = fh;
        }

        /** 前景比例：素材比例；没有素材 / 放不进这一格 → 其它卡片用 **2:4**。 */
        private float resolveFrontRatio(RectF area) {
            final float fallback = DEFAULT_OTHER_RATIO;
            float media = exactMediaRatio;
            if (media <= 0.01f) {
                return fallback;
            }
            float maxW = area.width() * 0.50f;
            float maxH = area.height() * 0.66f;
            if (maxW / media <= maxH) {
                return media;
            }
            if (maxH * media <= maxW) {
                return media;
            }
            return fallback;
        }

        /**
         * ↗↙ 两枚箭头（只给「原始比例」用）。v1.10.179 按用户给的参考图重画：
         *
         * · **头是 90° 直角**：尖端两条尾互相垂直（各自与杆成 45°），不再是两枚细细的「须」；
         * · **杆是 45°**，从中心留一点缺口出发到尖端；
         * · **两条尾都比杆短**（杆长 = 0.78×arm，尾长 = 0.62×arm），一眼能看出「头」和「身」；
         * · 圆角不变：两端 ROUND cap + 拐点 ROUND join；
         * · **适配图形**：arm 由图形**短边**反推（`0.5×min(fw,fh) − 线宽×0.75`），
         *   所以素材比例是竖版（镜像）时箭头自动缩小，永远在图形内部、不越界也不贴边。
         */
        private void drawStretchArrows(Canvas canvas, float fx, float fy, float fw, float fh,
                                       float density, int ink) {
            float cx = fx + fw / 2.0f;
            float cy = fy + fh / 2.0f;
            float min = Math.min(fw, fh);
            float stroke = Math.max(1.9f * density, min * 0.075f);
            // 尖端到中心 = arm。v1.10.180（反馈 1）：0.44 → **0.42**。
            // 图形已经缩到卡片宽的约 60%，箭头再按 0.44 会紧贴图形四边；
            // 0.42 时两支箭头占短边约 84%，四周还留 8% 的呼吸，既不贴边也不显小。
            // v1.10.522：符号再小一档、并且**保底留边**。
            // 0.42 → 0.34：箭头尖到图形短边中心只有 16% 的余量，素材比例再极端（很扁/很竖）
            // 也不会顶到卡片边缘；第二项用 2.2 倍线宽而不是 0.75 倍，保证描边外侧还有一圈空。
            float arm = Math.min(min * 0.34f, min * 0.5f - stroke * 2.2f);
            if (arm < 2.0f * density) {
                arm = Math.max(min * 0.22f, 1.0f);   // 极小图形的兜底（仍按比例，不越界）
            }
            float barb = arm * 0.55f;    // 尾比身短：尾 : 身 ≈ 0.7
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setColor(ink);
            arrowPath.reset();
            addArrow(arrowPath, cx, cy, arm, barb, 1.0f, -1.0f);    // ↗
            addArrow(arrowPath, cx, cy, arm, barb, -1.0f, 1.0f);    // ↙
            canvas.drawPath(arrowPath, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeCap(Paint.Cap.BUTT);
        }

        /**
         * 把一支箭头追加进 path：45° 的杆 + 尖端两条互相垂直（90°）的尾。
         *
         * @param dx dy 尖端方向（↗ = 1,-1；↙ = -1,1）
         */
        private void addArrow(Path path, float cx, float cy, float arm, float barb,
                              float dx, float dy) {
            float tipX = cx + dx * arm;
            float tipY = cy + dy * arm;
            // 杆：中心留 22% 缺口，两支箭头不连成一条直线
            path.moveTo(cx + dx * arm * 0.22f, cy + dy * arm * 0.22f);
            path.lineTo(tipX, tipY);
            // 两条尾：一条水平、一条垂直，在尖端与杆各成 45°
            path.moveTo(tipX - dx * barb, tipY);
            path.lineTo(tipX, tipY);
            path.lineTo(tipX, tipY - dy * barb);
        }

        /** 「任意照片」小标记（其它卡片用默认形状时不画）。 */
        private void drawPhotoGlyph(Canvas canvas, float fx, float fy, float fw, float fh) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x99FFFFFF);
            float gx = fx + fw * 0.5f;
            float gy = fy + fh * 0.5f;
            float gr = Math.min(fw, fh) * 0.18f;
            canvas.drawCircle(gx - gr * 0.45f, gy - gr * 0.35f, gr * 0.40f, paint);
            Path glyph = new Path();
            glyph.moveTo(gx - gr, gy + gr * 0.7f);
            glyph.lineTo(gx - gr * 0.1f, gy - gr * 0.05f);
            glyph.lineTo(gx + gr * 0.35f, gy + gr * 0.35f);
            glyph.lineTo(gx + gr, gy - gr * 0.15f);
            glyph.lineTo(gx + gr, gy + gr * 0.7f);
            glyph.close();
            canvas.drawPath(glyph, paint);
        }

        /** 把颜色往黑色混（t=0 原色，1 纯黑）—— 浅色前景块上的箭头用它压暗。 */
        private int mixToBlack(int color, float t) {
            float k = t < 0.0f ? 0.0f : (t > 1.0f ? 1.0f : t);
            int r = (int) (((color >> 16) & 0xFF) * (1.0f - k));
            int g = (int) (((color >> 8) & 0xFF) * (1.0f - k));
            int b = (int) ((color & 0xFF) * (1.0f - k));
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        /** 把颜色往白色混（t=0 原色，1 纯白）。 */
        private int mixToWhite(int color, float t) {
            float k = t < 0.0f ? 0.0f : (t > 1.0f ? 1.0f : t);
            int r = (int) (((color >> 16) & 0xFF) + (255 - ((color >> 16) & 0xFF)) * k);
            int g = (int) (((color >> 8) & 0xFF) + (255 - ((color >> 8) & 0xFF)) * k);
            int b = (int) ((color & 0xFF) + (255 - (color & 0xFF)) * k);
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }


        /** 与 ConfigActivity.systemControlColor() 同一套逻辑：取系统控件前景色。 */
        private int systemControlColor(ThemeColors c) {
            int fallback = c.getText();
            try {
                boolean dark = ThemeManager.INSTANCE.isDark(getContext());
                android.view.ContextThemeWrapper themed = new android.view.ContextThemeWrapper(getContext(),
                        dark ? android.R.style.Theme_Material : android.R.style.Theme_Material_Light);
                android.content.res.TypedArray ta = themed.obtainStyledAttributes(new int[]{android.R.attr.colorControlNormal});
                int color = ta.getColor(0, fallback);
                ta.recycle();
                return color == 0 ? fallback : color;
            } catch (Throwable th) {
                return fallback;
            }
        }

        private boolean isDarkColor(int color) {
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            return (0.299d * r + 0.587d * g + 0.114d * b) < 128.0d;
        }

        private Path arrowHead(float x, float y, float dx, float dy, float size) {
            Path head = new Path();
            float nx = -dy;
            float ny = dx;
            head.moveTo(x, y);
            head.lineTo(x - dx * size + nx * size * 0.5f, y - dy * size + ny * size * 0.5f);
            head.moveTo(x, y);
            head.lineTo(x - dx * size - nx * size * 0.5f, y - dy * size - ny * size * 0.5f);
            return head;
        }
    }
}
