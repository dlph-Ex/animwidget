#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
v1.10.463：**「语义色编辑页：每行点开只看到自己的控件」门禁**（verify-apk.sh 调用）。

为什么需要它（这是 462 被用户否决的第一条，也是这一版唯一的结构性风险）：
    语义色编辑弹窗 `SettingsActivity.showColorEditor()` 是**所有**颜色行共用的一个 Dialog。
    462 把「预览虚线色三档」和「滑块色作用范围」两条选择器**无条件**插了进去 —— 于是点主色、
    点表面色、点错误色，每一行都能看到两条与自己无关的选择器。用户第一眼就否了。

    这类错不崩、不报，只会在「点哪一行」这条路径上露出来；而写的时候作者满脑子是
    「把控件插进去」，很容易再次无条件插入。所以把「控件只在自己的 key 下存在」变成断言：
        ① buildColorRows() 里必须真有 `outline` 这一行（462 的第二条：只做了底层取色函数）；
        ② SegmentedRow 只在 isOutline 为真时创建；作用范围那条只在 isSlider 为真时创建；
        ③ 两条插入语句各自待在 `if (modeRow/scopeSwitch != null)` 的块里，且互不串味；
        ④ 首页色行（addColorRow）里**不许**再出现作用范围开关（第三条：迁移做成了复制）；
        ⑤ outline_mode / outline_custom 只许 ThemePrefs 读写（单一存储口）；
        ⑥ SegmentedRow 必须有 180ms 的 ValueAnimator + Ease.IN_OUT（第四条：跳变无过渡）；
        ⑦ v1.10.464：形态 = 平行四边形 + 四角 45° 切角、按内容收窄且靠左、滑块色同用这份控件、
           非自定义档的编辑页预览必须与首页色块同源（OutlineSettings.colorForMode）。

退出码 0 = 通过；1 = 有违规（逐条打印是哪一条）。
"""

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src-v2", "io", "github", "animwidget")

total = 0
bad = []


def ok(cond, msg):
    """记一条断言；失败时把「期望什么」打出来。"""
    global total
    total += 1
    if not cond:
        bad.append(msg)
    return cond


def read(name):
    with open(os.path.join(SRC, name), encoding="utf-8", errors="replace") as f:
        return f.read()


def body_of(text, signature):
    """从签名处起，按大括号配对取整段方法体（跳过字符串/字符字面量与注释）。"""
    i = text.index(signature)
    j = text.index("{", i)
    depth = 0
    k = j
    n = len(text)
    in_str = in_chr = in_line = in_block = False
    while k < n:
        ch = text[k]
        nxt = text[k + 1] if k + 1 < n else ""
        if in_line:
            if ch == "\n":
                in_line = False
        elif in_block:
            if ch == "*" and nxt == "/":
                in_block = False
                k += 1
        elif in_str:
            if ch == "\\":
                k += 1
            elif ch == '"':
                in_str = False
        elif in_chr:
            if ch == "\\":
                k += 1
            elif ch == "'":
                in_chr = False
        else:
            if ch == "/" and nxt == "/":
                in_line = True
                k += 1
            elif ch == "/" and nxt == "*":
                in_block = True
                k += 1
            elif ch == '"':
                in_str = True
            elif ch == "'":
                in_chr = True
            elif ch == "{":
                depth += 1
            elif ch == "}":
                depth -= 1
                if depth == 0:
                    return text[j:k + 1]
        k += 1
    return text[j:]


def block_after(text, opener):
    """取 `if (xxx) {` 那一块的整段（opener 里写 `if (modeRow != null)`）。"""
    i = text.index(opener)
    return body_of(text[i:], opener)


def code_lines(text):
    """只留代码行（去掉 // 注释与 javadoc 行）——门禁不能被注释里引述的旧写法点红（LESSONS 176.3）。"""
    out = []
    for line in text.splitlines():
        s = line.strip()
        if s.startswith("//") or s.startswith("*") or s.startswith("/*"):
            continue
        # 行尾注释也去掉，避免「注释里提到某个类名」被当成代码
        s = re.sub(r"//.*$", "", s)
        out.append(s)
    return out


sa = read("SettingsActivity.java")
tp = read("ThemePrefs.java")
sr = read("SegmentedRow.java")
os_java = read("OutlineSettings.java")

show = body_of(sa, "private final void showColorEditor(")
rows = body_of(sa, "public final void buildColorRows()")
addrow = body_of(sa, "private final void addColorRow(")
save_outline = body_of(sa, "private void saveOutlineColor(")

# ---------------------------------------------------------------- ① 色行存在
ok(re.search(r'addColorRow\([^;]*"outline"', rows) is not None,
   "buildColorRows() 里没有 key=outline 的「预览虚线色」行（462 的错：只做了底层取色函数，界面上没入口）")
ok("OutlineSettings.currentColor(" in rows,
   "「预览虚线色」行的色块不是按 OutlineSettings.currentColor() 取的（会和预览框画出来的颜色分家）")

# ---------------------------------------------------------------- ② 控件按 key 建
ok('"outline".contentEquals(key)' in show,
   "showColorEditor() 里没有 isOutline 判据（分段选择器会串到所有颜色页）")
ok('"slider".contentEquals(key)' in show,
   "showColorEditor() 里没有 isSlider 判据（作用范围会串到所有颜色页）")

seg_create = [ln for ln in code_lines(show) if "new SegmentedRow(" in ln]
ok(len(seg_create) == 1,
   "showColorEditor() 里 new SegmentedRow 出现 %d 次（应恰好 1 次：虚线色三档）" % len(seg_create))
ok(all("isOutline" in ln for ln in seg_create),
   "虚线色三档的 SegmentedRow 不是由 isOutline 决定的 —— 它会出现在每一行颜色的编辑页里")

scope_create = [ln for ln in code_lines(show) if "buildScopeRow(" in ln]
ok(len(scope_create) == 1,
   "showColorEditor() 里 buildScopeRow 出现 %d 次（应恰好 1 次：滑块作用范围）" % len(scope_create))
ok(all("isSlider" in ln for ln in scope_create),
   "buildScopeRow() 不是由 isSlider 决定的 —— 作用范围会出现在每一行颜色的编辑页里")
# 两段文字的作用范围控件也必须是同一条 SegmentedRow（v1.10.464：用户说它没适配新组件）
scope_builder = body_of(sa, "private final SegmentedRow buildScopeRow(")
ok("new SegmentedRow(" in scope_builder,
   "作用范围又用回了别的控件（用户：滑块色的分段选择器没有适配新组件）")

# ---------------------------------------------------------------- ③ 插入各归各块
ok("if (modeRow != null) {" in show,
   "showColorEditor() 里没有 `if (modeRow != null)` 分支（虚线色三档没有按 key 收口）")
ok("if (scopeRow != null) {" in show,
   "showColorEditor() 里没有 `if (scopeRow != null)` 分支（作用范围没有按 key 收口）")

mode_block = block_after(show, "if (modeRow != null) {")
scope_block = block_after(show, "if (scopeRow != null) {")
ok("insertUnderVariants(root, variants, modeRow)" in mode_block,
   "虚线色三档没有插到「简单变体」容器下方（用户指定的是容器正下方）")
ok("insertUnderVariants(root, variants, scopeRow)" in scope_block,
   "作用范围没有插到「简单变体」容器下方")
ok("scopeRow" not in mode_block and "setSliderScope" not in mode_block,
   "虚线色的分支里混进了作用范围（串页）")
ok("modeRow" not in scope_block and "applyOutlineLock" not in scope_block,
   "作用范围的分支里混进了虚线色三档（串页）")

locks = [ln for ln in code_lines(show) if "applyOutlineLock(" in ln]
ok(len(locks) >= 2,
   "showColorEditor() 里找不到「三块置灰」的调用（applyOutlineLock）")
# 置灰只许出现在虚线色那一页：除 mode_block 之外的行不许有它
outside = [ln for ln in code_lines(show) if "applyOutlineLock(" in ln and ln not in code_lines(mode_block)]
ok(not outside,
   "applyOutlineLock 出现在虚线色分支之外（别的颜色页也会跟着置灰）：%s" % "; ".join(outside))

# ---------------------------------------------------------------- ③b 保存才落盘（v1.10.467）
# 用户：「没点保存，会被自动保存」。这一页的**任何**写入都必须发生在「保存」里 ——
# 点击只改面板内的选中态。作用范围曾经在点击时就写 ThemePrefs，取消之后照样生效。
save_click = body_of(show, "save.setOnClickListener(")
ok("setSliderScope(" not in scope_block,
   "作用范围在点击时就写盘了（没点保存就已经生效）")
ok("setSliderScope(" in save_click,
   "作用范围没有在「保存」里落盘（改了保存之后也不生效）")
ok("setOutlineMode(" not in mode_block and "setCustomRgb(" not in mode_block,
   "虚线色的档位/自定义色在点击时就写盘了（应该只在「保存」里写）")

# ---------------------------------------------------------------- ④ 首页不再挂控件 + 只有一份实现
ok("SegmentedRow" not in addrow and "SliderScopeSwitch" not in addrow,
   "首页色行（addColorRow）里还挂着分段选择器 —— 462 的「迁移做成了复制」")
ok(len(re.findall(r"buildScopeRow\(", sa)) == 2,
   "buildScopeRow 的出现次数不是「定义 1 + 调用 1」（说明还有别的地方在用它）")
slider_switch_users = [fn for fn in sorted(os.listdir(SRC))
                       if fn.endswith(".java")
                       and any("SliderScopeSwitch" in ln for ln in code_lines(read(fn)))]
ok(not slider_switch_users,
   "老的 SliderScopeSwitch 还有代码在用（%s）：同一形态不许有两份实现"
   % ",".join(slider_switch_users))

# ---------------------------------------------------------------- ⑤ 单一存储口
for key in ("outline_mode", "outline_custom"):
    users = []
    for fn in sorted(os.listdir(SRC)):
        if not fn.endswith(".java") or fn == "ThemePrefs.java":
            continue
        if '"%s"' % key in read(fn):
            users.append(fn)
    ok(not users,
       "键 %s 被 ThemePrefs 之外的类直接读写：%s（462 那套 settings 旁路键又回来了）" % (key, ",".join(users)))
ok('getInt("outline_mode"' in tp and 'getInt("outline_custom"' in tp,
   "ThemePrefs 里没有 outline_mode / outline_custom 的读写实现（颜色体系没落地）")
ok("hasOutlineCustom(" in tp and "hasOutlineCustom(" in os_java,
   "缺 hasOutlineCustom：切到「自定义」档时没法判断要不要把取色器挪到用户上次的颜色")

# ---------------------------------------------------------------- ⑥ 控件要滑
ok("ValueAnimator" in sr, "SegmentedRow 没有用 ValueAnimator")
ok("Ease.IN_OUT" in sr, "SegmentedRow 的过渡没有用 Ease.IN_OUT")
ok("180L" in sr, "SegmentedRow 的过渡时长不是 180ms")
ok("setPalette" not in sr, "SegmentedRow 还在用 462 的 setPalette（颜色没接进 ThemeManager.paint 链路）")
ok("instanceof SegmentedRow" in read("ThemeManager.java"),
   "ThemeManager.paint() 没接 SegmentedRow —— 换肤后它会停在旧配色")
ok("applyColors" in sr and "getPrimary()" in sr,
   "SegmentedRow.applyColors 没从 ThemeColors 取色（材质会与页面胶囊不一致）")

# ---------------------------------------------------------------- ⑦ 保存两条链
ok("OutlineSettings.setModeIndex(" in save_outline,
   "保存「预览虚线色」时没有写档位（下次打开会跳回跟随壁纸）")
ok("OutlineSettings.setCustomRgb(" in save_outline,
   "保存「预览虚线色」时没有写自定义色")
ok("OutlineColor.Mode.CUSTOM.ordinal()" in save_outline,
   "保存「预览虚线色」时没有区分「自定义/非自定义」档（非自定义档写颜色等于没保存）")

# ---------------------------------------------------------------- ⑧ 置灰必须是真禁用
# 「只改 alpha 不挡事件」是本仓库反复踩过的坑：置灰的三块里有两块是自绘控件，
# 它们重写了 onTouchEvent，不会自己看 isEnabled —— 必须在方法里显式判。
hsv = read("HsvPickerView.java")
ok("!isEnabled()" in body_of(hsv, "public boolean onTouchEvent("),
   "HsvPickerView.onTouchEvent 不看 isEnabled：非自定义档置灰后手指一拖还能改色（假禁用）")
ok("!isEnabled()" in body_of(sr, "public boolean onTouchEvent("),
   "SegmentedRow.onTouchEvent 不看 isEnabled：禁用后还能点（假禁用）")

# ---------------------------------------------------------------- ⑨ 形态/对齐/预览一致（v1.10.464）
# 用户口径：①外观是**平行四边形 + 四角 45° 切角**（不是圆角胶囊）；②**靠左**，不是满宽居中；
# ③滑块色的作用范围也换成同一个控件；④非自定义档时首页色块 == 编辑页预览块。
ok("CORNER_DP" in sr and "SKEW_DP" in sr,
   "SegmentedRow 没有「圆角半径 / 平行四边形斜度」这两个形状参数")
m_c = re.search(r"CORNER_DP\s*=\s*([0-9.]+)f", sr)
m_s = re.search(r"SKEW_DP\s*=\s*([0-9.]+)f", sr)
ok(m_c is not None and float(m_c.group(1)) >= 5.0,
   "圆角半径太小（<5dp）会被看成直角 —— 用户两次说过「都不是圆角」")
ok(m_s is not None and float(m_s.group(1)) >= 2.0,
   "斜度为 0 —— 那不是平行四边形，是矩形")
ok("quadTo(" in body_of(sr, "private void buildPolygon("),
   "四个角不是圆弧倒出来的（没有 quadTo）—— 又回去当直切角了")
track = body_of(sr, "private void buildPolygon(")
ok("lineTo" in track and "moveTo" in track,
   "SegmentedRow 的轮廓不是折线多边形（平行四边形 + 切角要靠 Path 逐点画）")
ok("drawRoundRect" not in sr,
   "SegmentedRow 还在画圆角矩形（用户要的是 45° 切角的平行四边形，不是圆角）")

draw = body_of(sr, "protected void onDraw(")
# v1.10.465（用户：「选中图标也请改为同圆角平行四边形，而不是直角四边形」）：
# 轨道与选中块必须由**同一个** buildPolygon 画出来，否则选中块迟早退回矩形色带。
ok(draw.count("buildPolygon(") >= 2,
   "选中块不是用同一个多边形函数画的（会变成直角四边形）")
ok("drawPath(this.pillPath" in draw,
   "选中块没有按自己的轮廓画（pillPath 没被 drawPath）")
ok("drawRect(" not in draw,
   "选中块还是矩形色带（用户要的是同形状的斜四边形）")
ok("clipPath(" in draw,
   "选中块没按轨道形状裁刀 —— 小屏/大字体下会溢出轨道")

ins = body_of(sa, "private void insertUnderVariants(")
ok("WRAP_CONTENT" in ins and "MATCH_PARENT" not in ins,
   "附加控件还是满宽（MATCH_PARENT）—— 用户要的是按内容收窄")
ok("Gravity.START" in ins,
   "附加控件不是靠左（gravity 不是 START）")

cur = body_of(os_java, "static int currentColor(")
ok("colorForMode(" in cur,
   "首页色块（currentColor）没有走 colorForMode —— 和编辑页预览不是同一个函数")
ok("OutlineSettings.colorForMode(" in mode_block,
   "切档时没有按「该档的颜色」重画编辑页预览（非自定义档会沿用上一次的自定义色）")
ok("showColorEditor$render(" in mode_block,
   "切档时没有重画编辑页的预览块")

# ---------------------------------------------------------------- ⑩ 重置符号（v1.10.468）
# 用户：「头部箭头怎么老是不在身子中央」。205 那版把尖端放在弧端点上、三角形向后铺开，
# 于是箭头压在环上像一块补丁；而且只按圆心居中，箭头凸出那一截把视觉重心带偏。
glyph = read("GlyphView.java")
reset = body_of(glyph, "if (this.glyph == RESET) {")
ok("addArc(" in reset,
   "重置符号的环不是圆弧（改回 CharSequence/字符了？）")
ok("moveTo(px + tx * len" in reset,
   "重置符号的箭头不是「尖端沿切线伸出」——会插在环上像一块补丁（用户指的就是这个）")
ok("computeBounds(" in reset,
   "重置符号没按整组图形（含描边）的外接框居中 —— 箭头会把视觉重心带偏")

# ---------------------------------------------------------------- 结论
if bad:
    print("COLORSCOPE FAILED %d/%d" % (len(bad), total))
    for m in bad:
        print("  FAIL %s" % m)
    sys.exit(1)
print("COLORSCOPE OK %d" % total)
sys.exit(0)
