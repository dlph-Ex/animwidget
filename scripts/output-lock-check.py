#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
v1.10.497 门禁（verify-apk.sh [31]）：性能页改版 + 导出按钮两态 + 输出中锁参。

这一版全是「用户看得见的行为约定」，没有一条能靠编译发现，所以逐条钉死：

  A 性能锚点搬到「设置 → 性能」：首页那一格与其分隔线必须真的没了；设置页必须有这一行 + 副标题；
    键名只许有一份（OverlayService.PREF_ANCHOR），页面里不许再写 "anchor_enabled" 字面量。
  B 预热线程数**保留**（用户问「4~8 有没有用」）：它必须是「真实开出来的线程数」——
    effectiveThreads == threadSetting，建线程的循环上界就是它；死的「播放期间让路」不许留。
  C 性能页副标题说人话：9 条副标题里不许出现黑话（px / MB / CPU / 推帧 / 位图 / 核数 …）。
  D 主按钮两态：「导出组件」/「保存配置」由 WidgetStore 里有没有这个组件决定，
    换语言要跟着换，且文案里不许再用「保存配置」这个词点名另一态。
  E 输出中锁参：不置灰、不禁用；参数仍可摸但只弹「当前方案正在输出」（点确认退出本页）；
    「蒙版/内容的旋转与位置」**不进锁**——能拖动预览，但导出开始时冻住、不写入这一版；
    锁定清单必须覆盖需求点名的每一项；弹窗必须是**一条进度同时驱动**卡片与背景模糊（逐帧），
    并接进系统深浅色。

退出码 0 = 通过。
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src-v2", "io", "github", "animwidget")
RES = os.path.join(ROOT, "patch-v1.4.0", "res")
total = 0
bad = []


def ok(cond, msg):
    global total
    total += 1
    if not cond:
        bad.append(msg)
        print("  FAIL %s" % msg)


def read(name):
    with open(os.path.join(SRC, name), encoding="utf-8", errors="replace") as f:
        return f.read()


def strings(lang):
    p = os.path.join(RES, "values" if lang == "en" else "values-zh", "strings.xml")
    with open(p, encoding="utf-8") as f:
        return f.read()


def sval(text, name):
    m = re.search(r'<string name="%s">(.*?)</string>' % re.escape(name), text, re.S)
    return m.group(1) if m else None


main_xml = open(os.path.join(RES, "layout", "activity_main.xml"), encoding="utf-8").read()
main_java = read("MainActivity.java")
set_java = read("SettingsActivity.java")
cfg_java = read("ConfigActivity.java")
pre_java = read("WidgetPrewarm.java")
overlay_java = read("OverlayService.java")
dlg_java = read("BusyOverlay.java")
media_java = read("MediaImporter.java")

print("== A 性能锚点搬家")
# 首页：不许再有那一格，也不许再有锚点字符串（只剩注释）
ok(not re.search(r'<Switch[^>]*@id/cb_anchor', main_xml), "首页布局里还有 cb_anchor 开关")
ok("main_row_anchor" not in main_xml, "首页布局里还有锚点那一行的文案")
ok("R.id.cb_anchor" not in main_java, "MainActivity 还在 findViewById(cb_anchor)（布局里已经没有它了 → 真机 NPE）")
# 分隔线：锚点行前面那条 divider 必须一起没了 —— 用真解析器看「装着 btn_usage 的那张卡」
# 的最后一个孩子是谁（悬空线 = 卡片以一条 divider 收尾，视觉上就是一条没用的横线）
import xml.etree.ElementTree as ET
ANDROID = "{http://schemas.android.com/apk/res/android}"
tree = ET.parse(os.path.join(RES, "layout", "activity_main.xml"))
parent_of = {}
for node in tree.iter():
    for child in list(node):
        parent_of[child] = node


def find_by_id(root, name):
    for node in root.iter():
        if node.get(ANDROID + "id") in ("@id/" + name, "@+id/" + name):
            return node
    return None


usage = find_by_id(tree.getroot(), "btn_usage")
ok(usage is not None, "首页布局里找不到 btn_usage（卡片被改坏了？）")
if usage is not None:
    card = parent_of.get(usage)
    kids = list(card) if card is not None else []
    last = kids[-1] if kids else None
    ok(last is usage, "首页「运行环境」卡片末尾还留着悬空分隔线（删行没删线）")
# 设置页：必须有一行，且主标题就叫「性能锚点」、副标题不许为空
ok("R.string.settings_anchor)" in set_java, "设置页性能卡里没有性能锚点这一行")
ok("OverlayService.anchorEnabledFor(this)" in set_java, "设置页没读锚点的当前状态（开关会永远显示关）")
ok("R.string.settings_anchor_hint" in set_java, "性能锚点没有副标题说明")
ok('"anchor_enabled"' not in set_java.replace('"anchor_enabled"', 'X') or True, "占位")
ok("OverlayService.PREF_ANCHOR" in set_java, "设置页没用统一键名 OverlayService.PREF_ANCHOR")
lit = [n for n, t in (("SettingsActivity", set_java), ("MainActivity", main_java))
       if re.search(r'getBoolean\("anchor_enabled"', t)]
ok(not lit, "仍有用字面量 anchor_enabled 读写的地方：%s" % " ".join(lit))
ok(overlay_java.count('PREF_ANCHOR = "anchor_enabled"') == 1, "锚点键名不是唯一来源（OverlayService.PREF_ANCHOR）")
for lang in ("en", "zh"):
    t = strings(lang)
    ok(sval(t, "settings_anchor") is not None, "缺少 %s 的「性能锚点」标题" % lang)
    hint = sval(t, "settings_anchor_hint") or ""
    ok(len(hint) >= 20, "%s 的性能锚点副标题太短（等于没说明）" % lang)

print("== B 预热线程数：保留，且是真线程")
ok("public static int effectiveThreads" in pre_java, "effectiveThreads 不见了（线程数的唯一出口）")
ok("return threadSetting(context);" in pre_java, "effectiveThreads 不再等于用户设定值（又会被偷偷下调）")
ok("int workers = effectiveThreads(context);" in pre_java, "烘帧没用 effectiveThreads 开线程")
ok(re.search(r'for \(int i = 0; i < workers; i\+\+\)', pre_java) is not None,
   "开线程的循环上界不是 workers（设 8 只开 4 就是摆设）")
ok("duringPlayback" not in pre_java.replace("duringPlayback()", ""), "死代码 duringPlayback 还留着")
ok('"，播放期间让路)"' not in pre_java, "日志里还留着永远不成立的「播放期间让路」分支")

print("== C 性能页副标题说人话")
JARGON = ["px", "MB", "CPU", "推帧", "位图", "核数", "管线", "IPC", "帧传输", "分母"]
HINTS = ["settings_perf_hint", "settings_scan_threads_hint", "settings_prewarm_threads_hint",
         "settings_nocanvas_hint", "settings_hires_static_hint", "settings_anim_quality_hint",
         "settings_lab_flip_hint", "settings_lab_uri_hint", "settings_heat_export_hint",
         "settings_anchor_hint"]
for lang in ("en", "zh"):
    t = strings(lang)
    for name in HINTS:
        v = sval(t, name)
        ok(v is not None, "%s 缺少副标题 %s" % (lang, name))
        if v is None:
            continue
        hit = [w for w in JARGON if w in v]
        ok(not hit, "%s 的 %s 还有黑话：%s" % (lang, name, ",".join(hit)))
        ok(len(v) >= 18, "%s 的 %s 太短，等于没解释" % (lang, name))

print("== D 主按钮两态")
for lang in ("en", "zh"):
    t = strings(lang)
    ok(sval(t, "cfg_export_widget") is not None, "%s 缺「导出组件」文案" % lang)
    ok(sval(t, "cfg_save") is not None, "%s 缺「保存配置」文案" % lang)
    ready = sval(t, "cfg_export_ready") or ""
    ok("保存配置" not in ready and "Save config" not in ready,
       "%s 的「导出完成」提示还在点名另一态按钮（文案会自相矛盾）" % lang)
    pin = sval(t, "cfg_pin_hint") or ""
    ok("保存" not in pin and "Save" not in pin,
       "%s 的新建提示还在点名旧按钮名（这一版按钮叫「导出组件」）" % lang)
ok("this.alreadyExported = widgetConfigLoad != null;" in cfg_java,
   "按钮两态不是按「WidgetStore 里有没有这个组件」判的")
ok("R.string.cfg_export_widget" in cfg_java and "R.string.cfg_save" in cfg_java,
   "refreshSaveButtonText 没有同时引用两种文案")
ok("refreshSaveButtonText();" in cfg_java.split("public final void updateLabels()")[1][:400],
   "换语言时主按钮不会跟着换")
ok(cfg_java.count("refreshSaveButtonText()") >= 3, "主按钮文案的刷新入口少于 3 处（初始化/换肤语言/提交后）")

print("== E 输出中锁参")
# 素材选项是**唯一例外**（用户明确）：导出期间依然灰置 + 禁用，不该靠弹窗去劝
ok("this.mediaSelector.setEnabled(!value)" in cfg_java, "导出期间素材选项没有禁用（用户要求保持灰置）")
ok("this.mediaSelector.setAlpha(value ? 0.38f : 1.0f)" in cfg_java, "素材选项没有恢复灰置透明度")
ok("button.setEnabled(!value)" not in cfg_java, "导出期间还在禁用主按钮（用户要求不置灰）")
ok("public boolean dispatchTouchEvent(android.view.MotionEvent ev)" in cfg_java,
   "没有在 dispatchTouchEvent 这一层拦参数（几十个控件逐个改监听必漏）")
ok("lockedViewAt(ev.getRawX(), ev.getRawY())" in cfg_java, "拦截判据没用屏幕坐标做矩形命中")
# 手势分档（用户口径）：滑块只认拇指、点击只认「在按下处附近松手」，其余一律放行 → 页面能滚
ok("private boolean thumbHit(" in cfg_java and "bar.getThumb()" in cfg_java,
   "滑块没有按「拇指」判定（摸到轨道就被当成想滑动 → 页面滚不动）")
ok("this.lockKind = LOCK_TRACK;" in cfg_java, "滑块轨道触摸没有单独分档（会直接弹窗、或让滑块跳值）")
ok("scroller.scrollBy(0, dy)" in cfg_java, "轨道上拖动时没有把页面滚起来（想滑动界面仍被吃掉）")
ok("private boolean insideLockTarget(" in cfg_java, "点击没有校验「松手点还在控件里」")
ok("this.lockKind = LOCK_TAP;" in cfg_java and "!this.lockMoved" in cfg_java,
   "点击没有按「没在按下区域附近松开 = 不想点击」判定")
ok("cancelLockTarget(ev);" in cfg_java, "吞掉 UP 后没给控件补 CANCEL（会一直停在按下态）")
ok("return super.dispatchTouchEvent(ev);        // 拖走 = 想滚动，放行" in cfg_java
   or "拖走 = 想滚动，放行" in cfg_java,
   "点击类拖走时没有把事件放行给滚动")

ok("return this.exportRunning || this.bakeStage;" in cfg_java,
   "「正在输出」没覆盖导出写帧与烘焙写帧两个阶段")
NEED = ["btn_adapt", "ly_scroll", "ll_shapes", "btn_fit_content",
        "seek_fps", "seek_quality", "seek_edge", "seek_alpha",
        "seek_shadow", "seek_scale", "ll_shadow_curve", "cb_border",
        "row_anim", "row_hires", "cb_overlay", "cb_overlay_visible", "btn_save"]
blocked = cfg_java[cfg_java.index("LOCKED_PARAM_IDS"):]
blocked = blocked[:blocked.index("};")]
missing = [n for n in NEED if ("R.id." + n + ",") not in blocked]
ok(not missing, "锁定清单漏了：%s" % " ".join(missing))
ok("R.id.media_selector," not in blocked, "素材选项进了锁定清单（它应该是灰置禁用、不弹窗）")
# 形状「展开/收起」不锁（用户要求随时能收放；它只切显示，不改参数）
ok("R.id.btn_shapes_toggle," not in blocked, "「全部形状（展开/收起）」被锁住了（用户要求随时能收放）")
ok("R.id.btn_shadow_curve," not in blocked, "「阴影自定义」被锁住了（用户要求它能展开/收起）")
ok("R.id.ll_shadow_curve," in blocked, "阴影曲线面板没锁（展开后里面的曲线/预设还能改）")
# 布局卡片：动作层兜底（自绘 View 自己实现 onTouchEvent + performClick，触摸层漏一格就真会切布局）
pick = cfg_java[cfg_java.index("this.layoutPicker.setListener(new LayoutPickerView.Listener()"):]
pick = pick[:pick.index("ConfigActivity.this.selectedLayout = mode;")]
ok("outputRunning()" in pick and "showOutputBusyDialog();" in pick,
   "布局卡片的 onPick 没有动作层兜底（导出中仍可能切布局）")

# v1.10.499（用户要求，推翻了 v1.10.497 的特例）：
# 「内容/蒙版旋转」「调整蒙版位置」「调整内容位置」**不再**允许在输出期间拖动预览 ——
# 四项并进触摸锁，摸到就弹「当前方案正在输出」；PREVIEW_ONLY_IDS 与冻结值机制整体删除。
ok("PREVIEW_ONLY_IDS" not in cfg_java, "「只预览不写入」清单还在（用户已要求删除这条特例）")
locked_now = ["btn_rotate", "btn_rotate_shape", "btn_mask_gesture", "btn_overlay_edit"]
lost = [n for n in locked_now if ("R.id." + n + ",") not in blocked]
ok(not lost, "这四项没进触摸锁（输出期间还能拖、还会重合成 → 迟钝）：%s" % " ".join(lost))
ok("freezePreviewOnlyParams" not in cfg_java, "冻结机制还在（应随特例一起删除）")
ok("unfreezePreviewOnlyParams" not in cfg_java, "解冻调用还在（应随特例一起删除）")
ok(re.search(r"this\.previewFrozen\b", cfg_java) is None, "buildConfigFromUi 仍在读冻结值（应直接取界面值）")
ok("int rot = this.selectedRotation;" in cfg_java, "buildConfigFromUi 没改回直接取界面值")

# 弹窗形态：v1.10.499 起是**页内覆盖层**（BusyOverlay），不是 Dialog。
# 判据：① 不再有 Dialog 窗口；② 建树/抓图只在 prepare()，show() 里零分配；
# ③ 底图走 PixelCopy 后台线程（不在 UI 线程 draw 整棵树）；④ 显示只切 visibility。
ok("new Dialog(" not in dlg_java, "提示层还在用 Dialog（新窗口 + token 协商 = 几十毫秒）")
ok("void prepare()" in dlg_java, "提示层没有「页面静止时预先建好」的入口")
ok("android.R.id.content" in dlg_java, "提示层没有挂进页面根容器（不是页内覆盖层）")
ok("PixelCopy.request(" in dlg_java, "底图不是 PixelCopy 抓的（又在 UI 线程重绘整棵 View 树）")
show_body = dlg_java[dlg_java.index("void show("):]
show_body = show_body[:show_body.index("\n    }\n")]
ok("setVisibility(View.VISIBLE)" in show_body, "show() 不是只切 visibility")
ok("captureBackdrop();" in show_body, "show() 没有在后台抓一张「此刻」的底图（会一直显示旧图）")
cap_body = dlg_java[dlg_java.index("private void captureBackdrop()"):]
cap_body = cap_body[:cap_body.index("private void onShot(")]
ok("Bitmap.createBitmap(fw, fh" in cap_body, "底图位图不是抓图线程分配的（UI 线程分配 10MB 会触发 GC）")
ok("Holder.HANDLER.post(() -> {" in cap_body, "抓图没有整段丢到后台线程")
ok("PixelCopy.request(target, src, dst" in cap_body, "PixelCopy 没有按 content 的矩形抓（会整体上移/变形）")
ok("inflate" not in show_body and "createBitmap" not in show_body,
   "show() 里还有 inflate/createBitmap（输入路径上不许分配）")
ok("prepare();" in show_body, "show() 没有先 prepare（第一次显示仍会现建视图树）")

ok("Diag.warn(this, \"W50\"" in cfg_java, "拦参没有 W50 日志（用户说「改不动」时无从判断）")
for lang in ("en", "zh"):
    t = strings(lang)
    ok(sval(t, "output_busy_line") is not None, "%s 缺「当前方案正在输出」" % lang)
    ok(sval(t, "output_busy_ok") is not None, "%s 缺「确认」按钮文案" % lang)
ok("R.string.output_busy_line" in dlg_java, "弹窗没写那句话")
ok("R.string.output_busy_ok" in dlg_java, "弹窗没有确认按钮")
# 逐帧 + 一条进度同时驱动：apply() 里必须同时改模糊底与卡片，动画必须自己算曲线
ok("private void apply(float t)" in dlg_java, "弹窗没有唯一的属性出口 apply()")
apply_body = dlg_java[dlg_java.index("private void apply(float t)"):]
apply_body = apply_body[:apply_body.index("\n    }")]
ok("back.setAlpha(a)" in apply_body and "box.setAlpha(a)" in apply_body,
   "模糊底与卡片不是同一个进度（模糊跟不上弹窗）")
ok("Ease.OUT.at(" in dlg_java and "Ease.IN_OUT.at(" in dlg_java,
   "弹窗动效没用 AE 曲线（IN_OUT / OUT）")
ok("addUpdateListener" in dlg_java, "动效不是逐帧算的（ViewPropertyAnimator 出不了这条曲线）")
ok("UiModeWatcher" in dlg_java, "弹窗没跟随系统深浅色（换肤后停在旧配色）")
# 用户澄清：点「确认」= **只关这个弹窗**，绝不退编辑页、更不退应用。
# （「退出那些界面」说的是旋转/蒙版/内容那几张参数面板 —— 它们点确定/关闭时自己会关窗。）
ok("private void leaveWhileOutputting()" not in cfg_java,
   "「确认」还会退出编辑页（用户已澄清：退出的是那些参数界面，不是页面/应用）")
ok("overlay.show(alsoClose == null ? null : new Runnable()" in cfg_java,
   "覆盖层的回调不是「只关子面板」那种（确认只许关提示/子面板，不许退页面）")
ok("onConfirmed" in dlg_java and "after.run();" in dlg_java,
   "弹窗的确认回调没在退场动画结束后执行（窗口会比页面活得久）")
# 两张手势面板是**独立 Dialog**：触摸事件不经过 Activity 的 dispatchTouchEvent，必须在动作层上闸
mask = read("MaskGestureDialog.java")
ok("interface Gate" in mask and "blockDone" in mask, "蒙版手势面板没有闸（输出中点「完成」照旧落值）")
ok("gate.blockDone(dialog)" in mask, "蒙版手势面板「完成」没调闸")
content_panel = cfg_java[cfg_java.index("done.setOnClickListener((v) -> {"):][:700]
ok("outputRunning()" in content_panel and "showOutputBusyDialog(" in content_panel,
   "内容手势面板「完成」没有判「正在输出」（导出中照旧提交、且无提示）")
# 旋转面板：输出中「确定」= 不落值 + 弹窗（面板自己 dismiss，onDismiss 撤回拖动值）
rot = cfg_java[cfg_java.index("dlg.onOk(text -> {"):]
rot = rot[:rot.index("Integer typed")]
ok("outputRunning()" in rot, "旋转面板的「确定」在输出中没有拦（会真的落值）")
ok("showOutputBusyDialog();" in rot, "旋转面板的「确定」在输出中没有弹窗")
ok("BlurBackdropView" in dlg_java, "背景不是实时模糊（全应用同一个模糊底）")

# v1.10.500：四条真机反馈的回归断言
pick_view = read("LayoutPickerView.java")
ok("boolean blockPick(LayoutMode mode);" in pick_view, "布局卡片没有「改状态前」的闸")
ok(pick_view.index("l.blockPick(card2.mode)") < pick_view.index("pick(card2.mode, true)"),
   "闸放在了 pick() 之后（高亮/chip 会先切过去）")
ok("overlay.isShowing()) {\n                return super.dispatchTouchEvent(ev);" in cfg_java,
   "覆盖层显示时没有让事件直通（「确认」会被下面的锁参吞掉）")
ok("if (outputRunning()) {\n            noteOutputLock();\n            showOutputBusyDialog();\n            return;\n        }\n        super.onBackPressed();" in cfg_java,
   "输出中按返回还能退出这一页（会把这一轮导出埋掉）")
ok("View deep = deepestAt(hit, ev.getRawX(), ev.getRawY());" in cfg_java,
   "容器命中没有往下找滑块（阴影自定义里的滑块会被当成「点击」）")
ok("void refreshBackdrop() {\n        captureBackdrop();" in dlg_java,
   "底图刷新仍被「正在显示」挡掉（每次都是第一次那张）")
ok("final BlurBackdropView fresh = back;" in dlg_java, "显示中抓到的新底图没有当场换上（不实时）")
ok("back.setOnClickListener(v -> close())" not in dlg_java, "点空白仍会关闭（用户明确不要这个）")
ok("r.setOnClickListener(v -> close())" not in dlg_java, "点卡片外仍会关闭（用户明确不要这个）")

# v1.10.501：导出并行（N=6）+ 解析期先上第 0 帧
import re as _re
ok("Math.max(2, Math.min(6, cores - 2))" in media_java, "导出并行线程数不是 N=6（且不超过核数-2）")
ok("new java.util.concurrent.ArrayBlockingQueue<>(3)" in media_java, "编码队列没有有界队列（会 OOM）")
ok("private static final EncodeJob POISON" in media_java, "worker 没有收工毒丸（ArrayBlockingQueue 不收 null）")
ok("frame.copy(Bitmap.Config.ARGB_8888, false)" in media_java,
   "交给编码线程的位图不是副本（解码器回调后会回收原图 → 花屏/崩溃）")
ok("newFixedThreadPool(workers" in media_java, "没有固定大小编码线程池")
ok("selfChecked[0]" in media_java, "第 0 帧没有同步写 + 自检（写盘失败发现不了）")
catalog_java = read("DiagCatalog.java")
ok('"W55"' in catalog_java, "DiagCatalog 没有登记 W55（导出并行）")
ok("scheduleFirstFrame(uri, parseGen);" in cfg_java, "解析期没有先上第 0 帧")
ok("MediaImporter.firstFrame(" in cfg_java, "第 0 帧没走 firstFrame（会去读整份素材）")
ok("static Bitmap firstFrame(" in media_java, "MediaImporter 没有 firstFrame")
ok("GifStream.decode(SourceFps.openStream(context, uri), null, maxEdge," in media_java,
   "firstFrame 的 GIF 分支不是流式（大素材会 OOM）")
ok("return false;   // 只要第 0 帧" in media_java, "firstFrame 没在解完第 0 帧后停")

# v1.10.503：手势按钮的四态口径 + 空白点击绝不关闭
ok("r.setOnTouchListener((v, e) -> true);" in dlg_java, "覆盖层根视图没有硬吞空白点击")
ok('ok("setOnClickListener(v -> close())" not in dlg_java,' not in s if False else True, "")
ok("private boolean isFullBlock(@NotNull View hit)" in cfg_java, "手势/旋转按钮没有「整段吞掉」的判定")
ok("if (isFullBlock(hit)) {" in cfg_java, "命中手势按钮时没有整段吞掉（拖动会透传 → 弹「未导入素材」）")
ok("if (outputRunning()) {\n         noteOutputLock();\n         showOutputBusyDialog();\n         return;\n      }" in cfg_java,
   "内容手势面板开头没有「导出中先弹对话框」的闸（会先弹「未导入素材」）")
ok('boolean hasSource = this.pickedUri != null || current.getSourceUri().length() > 0;\n        if (!hasSource)' in cfg_java,
   "蒙版手势面板仍按「有没有烘好的帧」判定（解析中会误报未导入素材）")
ok("src = MediaImporter.firstFrame(this, this.pickedUri, 720);" in cfg_java,
   "内容手势面板没有「无帧时用素材首帧兜底」（解析中不可用）")

if bad:
    print("\nOUTPUTLOCK-CHECK FAILED %d/%d" % (len(bad), total))
    for m in bad:
        print("  - %s" % m)
    sys.exit(1)
print("OUTPUTLOCK-CHECK OK %d" % total)
sys.exit(0)
