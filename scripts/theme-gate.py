#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
v1.10.203：把两条「结构不变量」变成构建期门禁（verify-apk.sh 会调用）。

为什么需要它：v1.10.200/201 那两轮的问题（语义色串味、换肤后开关框停在旧配色）
都不是某一行写错，而是**结构塌了**——
  ① 某个「生效色」键在切方案时没人重写（旁路键不串味才怪）；
  ② 某个自绘控件的颜色没接进换肤链路（换肤后它就停在旧配色）。
这两种错不报错、不崩，只在真机上按特定顺序操作才露出来。静态扫描能把它们挡在出包之前。

退出码 0 = 通过；1 = 有违规（脚本会逐条打印「哪个键 / 哪个类」）。
"""

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src-v2", "io", "github", "animwidget")

# 允许「自绘控件没接进 ThemeManager.paint()」的名单 —— 每条必须写清为什么。
# 判据：这些类的颜色要么根本不由主题决定，要么它们的上色是「调用方主动 push」且调用方已在刷新链路里。
PAINT_ALLOW = {
    "AuroraView": "绘制时自己取 ThemeManager.colors（每个光轮一次），换肤只需 invalidate",
    "ColorAuroraView": "调色页背景光轮，构造时按传入色画，生命周期只有一次调色会话",
    "HsvPickerView": "色相/明度控件，颜色由被编辑的那个色决定，与主题无关",
    "HoleView": "删除动画里的黑洞遮罩，颜色由动画逐帧给",
    "TuneMarkView": "装饰刻度，颜色由调用方在构造时给一次",
    "VariantDot": "变体小圆点，颜色由调用方 push",
    "SpinRingView": "加载环，颜色在 start() 时给一次",
    "GlowRowLayout": "光晕装饰层，颜色由调用方 push",
    "ContentGestureView": "手势命中层，不绘制内容",
    "MediaOverlayView": "预览叠加层，颜色由媒体内容决定",
    "BlurBackdropView": "模糊底，颜色由截图决定",
    "GlassFrameDrawable": "不是 View（Drawable），由调用方给色",
    "SchemeTileView": "方案图标：颜色由 refreshSchemeTileRings / buildSchemes 主动 push",
    "CheckMarkDrawable": "不是 View（Drawable），颜色由调用方给色",
    "SwitchBoxView": "已在 ThemeManager.paint() 里",
    "FontScaleSlider": "已在 ThemeManager.paint() 里",
    "LanguageRowView": "已在 ThemeManager.paint() 里",
    "AspectRatioFrameLayout": "容器，只做测量，不绘制",
    "MediaSelectorView": "颜色由调用方 push：ConfigActivity.refreshThemeInPlace() 里 applyColors(primary/neutral/text/onPrimary)",
    "ShapeChipView": "颜色由调用方 push：ConfigActivity.refreshThemeInPlace() → buildShapeGrid() 整块重建",
    "WallpaperBackdropView": "画的是壁纸本身（v1.10.519），一个像素都不来自主题色；换壁纸走 W56 的那条链路，与换肤无关",
}


def read(name):
    with open(os.path.join(SRC, name), encoding="utf-8", errors="replace") as f:
        return f.read()


def method_body(text, signature):
    """从签名处截到下一个「四个空格 + private/protected/public/final/static」的行，够用。"""
    i = text.index(signature)
    j = i + len(signature)
    while True:
        m = re.compile(r"\n    (?:@|private |protected |public |static |final |void |boolean |int )").search(text, j)
        if not m:
            return text[i:]
        return text[i:m.start()]


def gate_theme_keys():
    """① 生效色键：ThemeManager 读的每个键，必须由 writeSchemeColors 重写。"""
    tm = read("ThemeManager.java")
    read_keys = set(re.findall(r'\b(?:pick|liveBase)\(context,\s*"([a-z_]+)"', tm))
    sa = read("SettingsActivity.java")
    body = method_body(sa, "private boolean writeSchemeColors(long id)")
    written = set(re.findall(r'setCustom\(this,\s*"([a-z_]+)"', body))
    if "applyBaseSnapshot" in body:
        written |= {"surface", "neutral"}   # 底色的六键快照由 ThemePrefs 一次写全
    bad = sorted(read_keys - written)
    # 派生/只读键（不进方案的）在这里显式排除，并写明原因
    derived_only = set()
    bad = [k for k in bad if k not in derived_only]
    return read_keys, written, bad


def gate_drawn_views():
    """② 自绘控件：凡是 override onDraw 的类，必须能被换肤链路刷新。"""
    classes = {}
    for fn in sorted(os.listdir(SRC)):
        if not fn.endswith(".java"):
            continue
        text = read(fn)
        for m in re.finditer(r"public (?:final )?class (\w+) extends (\w+)", text):
            name, base = m.group(1), m.group(2)
            # 只关心「自己绘制」的控件；Activity / Dialog / Adapter / Service 不是控件
            if base.endswith("Activity") or base.endswith("Dialog") or base.endswith("Adapter") \
                    or base.endswith("Service") or base.endswith("ServiceImpl"):
                continue
            classes[name] = (fn, text)
    tm = read("ThemeManager.java")
    offenders = []
    checked = 0
    for name, (fn, text) in classes.items():
        if "protected void onDraw(" not in text and "public void onDraw(" not in text:
            continue
        checked += 1
        if name in PAINT_ALLOW:
            continue
        if re.search(r"instanceof %s\b" % re.escape(name), tm):
            continue
        body = ""
        for sig in ("protected void onDraw(", "public void onDraw("):
            if sig in text:
                body = method_body(text, sig)
                break
        if "ThemeManager.INSTANCE.colors" in body:
            continue
        offenders.append((name, fn))
    return checked, offenders


def gate_base_writers():
    """③ 底色「按模式各一份」的 4 个键只许 ThemePrefs 写（防止再出现旁路写入口）。"""
    calls = ['putInt("surface_d"', 'putInt("surface_l"',
             'putInt("neutral_d"', 'putInt("neutral_l"']
    bad = []
    for fn in sorted(os.listdir(SRC)):
        if not fn.endswith(".java") or fn == "ThemePrefs.java":
            continue
        text = read(fn)
        for c in calls:
            if c in text:
                bad.append(fn)
                break
    return bad


def main():
    read_keys, written, bad = gate_theme_keys()
    print("== 主题门禁 A：生效色键必须被「激活方案」重写")
    print("   ThemeManager 读的键=%d：%s" % (len(read_keys), ",".join(sorted(read_keys))))
    print("   writeSchemeColors 重写的键=%d：%s" % (len(written), ",".join(sorted(written))))
    if bad:
        print("   **违规**：切方案时没人重写 → 这些键会串味：%s" % ",".join(bad))
        for k in bad:
            print("      · %s（在 ThemeManager.pick/liveBase 里读，但 writeSchemeColors 不写）" % k)
    else:
        print("   结论 OK：读到的每个键都在激活方案时被重写")

    checked, offenders = gate_drawn_views()
    print("== 主题门禁 B：自绘控件的颜色必须能被换肤链路刷新")
    print("   扫描 onDraw 的类=%d，白名单=%d" % (checked, len(PAINT_ALLOW)))
    if offenders:
        print("   **违规**（既没在 ThemeManager.paint() 里，也不在自己 onDraw 里取当前色）：")
        for name, fn in offenders:
            print("      · %s（%s）" % (name, fn))
    else:
        print("   结论 OK：每个自绘控件都有刷新路径")

    base_writers = gate_base_writers()
    print("== 主题门禁 C：底色的「按模式各一份」只许 ThemePrefs 写")
    if base_writers:
        print("   **违规**：这些文件直接写了 surface_d/_l 之类的键（旁路写入口）：%s"
              % ",".join(base_writers))
    else:
        print("   结论 OK：只有 ThemePrefs 里的两个入口（applyBaseSnapshot / applyBaseEdit）能写")

    return 0 if (not bad and not offenders and not base_writers) else 1


if __name__ == "__main__":
    sys.exit(main())
