package io.github.animwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

/**
 * v1.10.430（用户指定）：**日志控制台** —— 不换行、双向滑动、滚到哪排到哪。
 *
 * <p>为什么必须自绘：原来是一个 TextView 塞 3000 行，而每行现在有 700~900 字，
 * 在手机宽度下会被折成 12~15 个视觉行 → 实际要排 **3.5 万~4.5 万行**，
 * 一次 StaticLayout 就把主线程按死（ANR → 黑屏回桌面）。而"只显示末尾 3000 行"这个限制
 * 是在**峰值之后**才生效的，救不了它。
 *
 * <p>这里只画可见区间：行高固定、每行一个字符串、永不换行，
 * 所以 3000 行和 3 行的绘制量一样（十几行）。行数一行不减，导出仍是全量。
 *
 * <p>右侧一根**四边直角的灰色滑块**（用户指定样式）：高度按可见比例，按住能拖、拖到底就到底，
 * 手指离开 900ms 后淡出，不挡内容。
 */
final class LogConsoleView extends View {

    private String[] lines = new String[0];
    private float scrollX;
    private float scrollY;
    private float contentW;
    private float contentH;
    private final float lineH;
    private final float ascent;
    /** v1.10.437：正文左边距（16dp，与上面标题/按钮/提示的左边对齐）。 */
    private final float textX;
    /** 正文右边距（12dp，只为了让文字不压到滑块下面）。 */
    private final float textRight;
    private final int barW;
    private final int barMinH;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean barVisible;
    private boolean hasContent;
    /** v1.10.434：手指按着或在惯性中 —— 这期间不要换内容（换一次闪一次）。 */
    private boolean busy;
    private float vx;
    private float vy;
    private long lastAt;
    private float flingVx;
    private float flingVy;
    private long flingStart;
    /** v1.10.432：每行"第一次进入可见区"的时刻，用来做逐条渐显。 */
    private long[] shownAt = new long[0];
    /**
     * v1.10.440（用户："导航栏没有透明，要么就是文本还没延伸"）：正文**要能画到屏幕最下沿**，
     * 所以底部那一段不再从 View 的 padding 里扣，改成内容末尾的留白 ——
     * 平时字可以铺到三个键底下（这才叫"延伸到导航栏"），滚到底时又刚好能让最后一行停在栏上面。
     */
    private int trailSpace;
    /**
     * v1.10.438：**惯性有没有在跑**。
     *
     * <p>这是 435 埋下的一个致命 bug：{@link #isScrolling()} 当时用
     * `Math.abs(flingVx) + Math.abs(flingVy) > 0.05f` 判断，可是 {@link #fling} 结束时
     * **只 return，不清速度** —— 于是只要用户抛过一次列表，这两个速度就永久非 0，
     * `isScrolling()` 永远是 true，日志页的 {@code refresh()} 从此一路 return：
     * 清屏清了数据但画面不刷（用户："清屏不了日志"）、撤回还原了也不刷、新日志也不再出现。
     * 现在用显式状态位，并且停下时把速度归零。
     */
    private boolean flinging;
    /** v1.10.431：松手后的惯性 —— 速度大缓冲大、速度小缓冲小；到边即停。 */
    private final Runnable fling = new Runnable() {
        @Override
        public void run() {
            long now = SystemClock.uptimeMillis();
            float t = (now - flingStart) / 1000.0f;      // 秒
            if (t > 1.2f || (Math.abs(flingVx) < 1.0f && Math.abs(flingVy) < 1.0f)) {
                flinging = false;
                flingVx = 0.0f;
                flingVy = 0.0f;
                return;
            }
            // 指数衰减：初速越大走得越远（曲线与速度成正比，不是固定时长）
            float decay = (float) Math.exp(-4.0f * t);
            float stepX = flingVx * decay * 16.0f / 1000.0f * 1000.0f;
            float stepY = flingVy * decay * 16.0f;
            scrollX += stepX;
            scrollY += stepY;
            clamp();
            invalidate();
            postDelayed(this, 16L);
        }
    };
    private boolean draggingBar;
    private float lastX;
    private float lastY;

    private final Runnable hideBar = new Runnable() {
        @Override
        public void run() {
            barVisible = false;
            invalidate();
        }
    };

    LogConsoleView(Context context) {
        super(context);
        float d = context.getResources().getDisplayMetrics().density;
        paint.setTypeface(Typeface.MONOSPACE);
        paint.setTextSize(11.5f * context.getResources().getDisplayMetrics().scaledDensity);
        paint.setColor(0xFFDDDDDD);
        Paint.FontMetrics fm = paint.getFontMetrics();
        this.lineH = (fm.descent - fm.ascent) + 3.0f * d;   // 与旧 TextView 的 lineSpacingExtra=3dp 对齐
        this.ascent = fm.ascent;
        // v1.10.437（用户："右侧滑块没有贴边，你加个自适应不就行了吗"）：
        // 滑块贴的是**这个 View 的右边缘**，而 View 现在铺满整幅屏幕宽度（布局里不再给左右 padding），
        // 所以滑块天然贴屏幕右边。文字的 16dp 左边距改在下面用 Paint 画出来 ——
        // 边距在内、边缘在外，两者互不干扰，换机型/换密度都成立。
        this.textX = 16.0f * d;
        this.textRight = 12.0f * d;
        this.barW = Math.max(6, Math.round(10.0f * d));  // v1.10.433：6dp 仍然偏细，再宽一档
        this.barMinH = Math.max(Math.round(36.0f * d), barW * 8);
        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setColor(0x88888888);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    /**
     * v1.10.438（用户："文本扩展到导航栏部分"）：真正能画日志的高度 = 去掉上下 padding。
     *
     * <p>现在控制台一直铺到屏幕最下沿（页面不再留 24dp 底边），底部那段导航栏高度
     * 由 **View 自己的 paddingBottom** 让出来 —— 这个 padding 是随系统 inset 实时给的
     * （见 LogActivity.installConsole），三键/手势/平板各不相同，不用写死任何数字。
     */
    private int availH() {
        return Math.max(0, getHeight() - getPaddingTop() - getPaddingBottom());
    }

    /** 换内容。行序与 LogBook.renderTail 一致：第 0 行是最新的。 */
    void setText(String text) {
        if (text == null) {
            text = "";
        }
        // v1.10.440（用户："新日志生成文本页就闪一次"）：**别整屏重播渐显**。
        // 原来是每次 setText 都把 shownAt 全部清零 → 每来一条新日志，屏幕上三十几行一起
        // 从 0 淡到 1，肉眼就是"整页闪一下"。现在把旧行的时间戳按行对齐搬过来，
        // 只有真正新增的那几行是 0（它们才渐显）。
        String[] next = text.isEmpty() ? new String[0] : text.split("\n", -1);
        long[] prev = this.shownAt;
        String[] prevLines = this.lines;
        this.lines = next;
        // 旧行的时间戳按行搬过来（见 LogAlign 的注释），只有新增的行才渐显
        this.shownAt = LogAlign.carry(prevLines, prev, next);
        this.contentH = this.lines.length * this.lineH + this.trailSpace;
        // v1.10.436：**不再对每一行跑 measureText**。
        // 3000 行 × 700~900 字 = 一次刷新要量 200 多万字的字形宽度，主线程上百毫秒，
        // 每 2 秒来一次（打开日志页"黑屏/卡死"的另一半就是这个）。
        // 这里用等宽字体的性质算：ASCII 行宽 = 字数 × 一个字宽（精确）；
        // 含中文/全角字符的行（日志里很少）才真去量。总代价从"百万字量字形"降到"扫一遍字符"。
        float cell = paint.measureText("M");
        float w = 0.0f;
        for (int i = 0; i < this.lines.length; i++) {
            String s = this.lines[i];
            int n = s.length();
            if (n <= 0) {
                continue;
            }
            if (n * cell * 2.0f <= w) {
                continue;          // 就算整行都是全角也超不过当前最大宽度
            }
            float m = isAscii(s) ? n * cell : paint.measureText(s);
            if (m > w) {
                w = m;
            }
        }
        this.contentW = w + this.textX + this.textRight;
        if (!this.hasContent) {
            // v1.10.431：只有**第一次**灌内容时回到顶部。
            // 之前每次 refresh（每 2 秒）都归零，用户往下翻两行就会被弹回最新处。
            this.scrollX = 0.0f;
            this.scrollY = 0.0f;
            this.hasContent = true;
        }
        clamp();
        invalidate();
    }

    /** v1.10.440：底部末尾留白（= 导航栏高度，由页面的 inset 监听现算）。 */
    void setTrailSpace(int px) {
        int want = Math.max(0, px);
        if (this.trailSpace == want) {
            return;
        }
        this.trailSpace = want;
        this.contentH = this.lines.length * this.lineH + this.trailSpace;
        clamp();
        invalidate();
    }

    /** 全是 ASCII？（等宽字体下这样才能用「字数 × 字宽」直接算宽度） */
    private static boolean isAscii(String s) {
        for (int i = 0, n = s.length(); i < n; i++) {
            if (s.charAt(i) >= 0x80) {
                return false;
            }
        }
        return true;
    }

    /** v1.10.434：滚动中？滚动中不要换内容 —— 那是频闪的真正来源（每次 refresh 都整屏重画）。 */
    boolean isBusy() {
        return busy || draggingBar || flinging;
    }

    /** v1.10.435：正在拖 / 惯性中 —— 这段时间别换内容（换内容才是那个频闪）。 */
    boolean isScrolling() {
        return barVisible || draggingBar || flinging;
    }

    void setConsoleTextColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    void setBarColor(int color) {
        barPaint.setColor(color);
        invalidate();
    }

    private void clamp() {
        float maxX = Math.max(0.0f, contentW - getWidth());
        float maxY = Math.max(0.0f, contentH - availH());
        if (scrollX < 0.0f) {
            scrollX = 0.0f;
        } else if (scrollX > maxX) {
            scrollX = maxX;
        }
        if (scrollY < 0.0f) {
            scrollY = 0.0f;
        } else if (scrollY > maxY) {
            scrollY = maxY;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (lines.length == 0) {
            return;
        }
        int avail = availH();
        if (avail <= 0) {
            return;
        }
        clamp();   // padding 可能晚到（inset 分发晚于第一次 setText）
        // 只画可见区间（上下各多 2 行，滚动时不出白边）
        int first = Math.max(0, (int) (scrollY / lineH) - 2);
        int last = Math.min(lines.length - 1, (int) ((scrollY + avail) / lineH) + 2);
        long now = SystemClock.uptimeMillis();
        boolean animating = false;
        canvas.save();
        // 正文区就是 avail 那块：底部 pad 那一条（导航栏）不许画字，否则字会钻到三个键底下
        canvas.clipRect(0, getPaddingTop(), getWidth(), getPaddingTop() + avail);
        canvas.translate(-scrollX, getPaddingTop() - scrollY);
        int baseAlpha = paint.getAlpha();
        for (int i = first; i <= last; i++) {
            // v1.10.432（需求）：**逐条渐显** —— 某一行第一次进入可见区时从 0 淡到 1（140ms），
            // 滚出去再滚回来不会重播（shownAt 记着），所以往回翻不会有"整屏在闪"。
            if (i < shownAt.length) {
                if (shownAt[i] == 0L) {
                    shownAt[i] = now;
                }
                // v1.10.434：**按用户要求改回原来的渐显**（0 → 1，140ms，每行只在第一次进入时播）。
                float t = (now - shownAt[i]) / 140.0f;
                if (t < 1.0f) {
                    paint.setAlpha(Math.max(1, Math.round(baseAlpha * t)));
                    animating = true;
                } else {
                    paint.setAlpha(baseAlpha);
                }
            }
            canvas.drawText(lines[i], textX, i * lineH - ascent, paint);
        }
        paint.setAlpha(baseAlpha);
        canvas.restore();
        if (animating) {
            postInvalidateOnAnimation();
        }
        if (barVisible) {
            drawBar(canvas);
        }
    }

    private void drawBar(Canvas canvas) {
        int h = availH();
        if (h <= 0 || contentH <= 0.0f) {
            return;
        }
        int trackRight = getWidth();   // v1.10.431：贴右边缘，不再留 2dp 缝
        int x0 = trackRight - barW;
        float ratio = Math.min(1.0f, h / contentH);
        int thumbH = Math.max(barMinH, Math.round(h * ratio));
        float maxScroll = Math.max(1.0f, contentH - h);
        int thumbY = getPaddingTop() + Math.round((h - thumbH) * (scrollY / maxScroll));
        canvas.drawRect(x0, thumbY, trackRight, thumbY + thumbH, barPaint);
    }

    /** 滑块的位置 ↔ 滚动位置。 */
    private void scrollToThumb(float y) {
        int h = availH();
        if (h <= 0) {
            return;
        }
        float ratio = Math.min(1.0f, (float) h / Math.max(1.0f, contentH));
        int thumbH = Math.max(barMinH, Math.round(h * ratio));
        float maxScroll = Math.max(0.0f, contentH - h);
        float travel = Math.max(1.0f, h - thumbH);
        float p = (y - getPaddingTop() - thumbH / 2.0f) / travel;
        p = Math.max(0.0f, Math.min(1.0f, p));
        scrollY = p * maxScroll;
        clamp();
        invalidate();
    }

    private boolean inBar(float x) {
        return x >= getWidth() - barW * 3.0f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // v1.10.432：**必须禁掉父容器拦截**。ScrollView 的 onInterceptTouchEvent 是在子 View
                // 之前跑的，`setOnTouchListener` 根本管不到它 —— 所以上一版只有斜着滑能生效
                //（ScrollView 只拦纵向）。这一句是纵向能滑的关键。
                busy = true;
                flinging = false;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                removeCallbacks(hideBar);
                removeCallbacks(fling);
                barVisible = true;
                lastX = event.getX();
                lastY = event.getY();
                vx = 0.0f;
                vy = 0.0f;
                lastAt = event.getEventTime();
                draggingBar = inBar(event.getX());
                if (draggingBar) {
                    scrollToThumb(event.getY());
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (draggingBar) {
                    scrollToThumb(event.getY());
                } else {
                    // 上下左右都能滑：手指往哪拖，内容往哪走
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;
                    scrollX -= dx;
                    scrollY -= dy;
                    lastX = event.getX();
                    lastY = event.getY();
                    long nowT = event.getEventTime();
                    long dt = Math.max(1L, nowT - lastAt);
                    lastAt = nowT;
                    // 速度（px/ms），用指数平均压一下抖动
                    vx = vx * 0.7f + (-dx / dt) * 0.3f;
                    vy = vy * 0.7f + (-dy / dt) * 0.3f;
                    clamp();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                draggingBar = false;
                busy = false;
                postDelayed(hideBar, 900L);
                // v1.10.431（用户：松开急停没有手感）：按松手速度给一段惯性。
                // 速度大 → 缓冲大；速度小 → 几乎立刻停。到边界就停住，绝不越界。
                if (Math.abs(vy) > 0.25f || Math.abs(vx) > 0.25f) {
                    flingVx = vx;
                    flingVy = vy;
                    flingStart = SystemClock.uptimeMillis();
                    flinging = true;
                    post(fling);
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }
}
