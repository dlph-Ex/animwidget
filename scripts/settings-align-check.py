#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""设置页「界面语言 / 系统模式」两张卡片的内缩逐项对拍（v1.10.320）。

## 为什么要有

这两张卡的视觉必须**完全同构**（同样的卡片内缩、同样的 header 内缩、同样的行内缩、
同样的图标→标题间距），用户已经连着两轮指出「选中条目宽度不对」「请你对准」。
两次的病根都是同一类：只对了一级 padding，上一级没对。

   语言卡：card padding 10 + header padding 6 = 图标距卡片边 16dp
           card padding 10 + 容器 padding 4 = 胶囊距卡片边 14dp
   模式卡：card 横向是 0 的那一版 → 图标 6dp、胶囊 4dp（两处都错）

这个脚本把两条链路**逐项算成数**再相减，差值不为 0 就红。改布局时不用再靠眼睛。

用法：python3 scripts/settings-align-check.py [--verbose]
退出码 0 = 两张卡逐项相等。
"""
import os
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
XML = os.path.join(ROOT, "patch-v1.4.0", "res", "layout", "activity_settings.xml")
A = "{http://schemas.android.com/apk/res/android}"
VERBOSE = "--verbose" in sys.argv

# 期望值（用户认可的「界面语言」卡其实就是这些数）
ICON = 40.0     # IconTileView 边长
TITLE_GAP = 12.0  # 图标 → 标题容器 marginStart
GLYPH = 40.0    # 右侧箭头边长


def num(text):
    if text is None:
        return None
    text = text.strip()
    for unit in ("dip", "dp", "px"):
        if text.endswith(unit):
            text = text[: -len(unit)]
            break
    try:
        return float(text)
    except ValueError:
        return None


def pads(e):
    """(left, top, right, bottom) —— 同时认 padding 与 paddingStart/End/Left/Right。"""
    p = num(e.get(A + "padding")) or 0.0
    pl = num(e.get(A + "paddingLeft"))
    pr = num(e.get(A + "paddingRight"))
    ps = num(e.get(A + "paddingStart"))
    pe = num(e.get(A + "paddingEnd"))
    left = pl if pl is not None else (ps if ps is not None else p)
    right = pr if pr is not None else (pe if pe is not None else p)
    top = num(e.get(A + "paddingTop"))
    bottom = num(e.get(A + "paddingBottom"))
    return left, (p if top is None else top), right, (p if bottom is None else bottom)


def margin_start(e):
    m = num(e.get(A + "layout_marginStart"))
    if m is None:
        m = num(e.get(A + "layout_marginLeft"))
    return m or 0.0


def elems(root):
    out = []

    def walk(e):
        out.append(e)
        for c in list(e):
            walk(c)
    walk(root)
    return out


def main():
    tree = ET.parse(XML)
    root = tree.getroot()
    all_e = elems(root)
    parent = {}
    for p in all_e:
        for c in list(p):
            parent[c] = p

    def by_id(name):
        for e in all_e:
            if (e.get(A + "id") or "").endswith("/" + name):
                return e
        return None

    def card_of(container):
        e = container
        while e is not None:
            if e.get(A + "tag") == "card":
                return e
            e = parent.get(e)
        return None

    def header_of(container):
        """卡片里第一个带 id 且含 GlyphView 的行 —— 两张卡都是这个结构。"""
        card = card_of(container)
        for child in list(card):
            for g in list(child):
                if g.tag.endswith("GlyphView"):
                    return child
        return None

    specs = {}
    for name, cid, hid in (
        ("语言", "rg_language", "row_language_header"),
        ("模式", "rg_mode", "row_mode_header"),
    ):
        cont = by_id(cid)
        card = card_of(cont)
        # header 用 id 直接取（比结构猜测可靠），结构猜测只用来兜底
        header = by_id(hid)
        if header is None:
            header = header_of(cont)
        cp = pads(card)
        hp = pads(header)
        vp = pads(cont)
        icon = None
        title_wrap = None
        glyph = None
        for c in list(header):
            if icon is None and c.tag.endswith("IconTileView"):
                icon = c
            elif glyph is None and c.tag.endswith("GlyphView"):
                glyph = c
        for c in list(header):
            if c is icon:
                continue
            if icon is not None and c.tag.endswith("LinearLayout"):
                title_wrap = c
                break
        specs[name] = {
            "card_left": cp[0],
            "card_top": cp[1],
            "card_right": cp[2],
            "card_bottom": cp[3],
            "header_left": hp[0],
            "header_right": hp[2],
            "icon_left": cp[0] + hp[0],
            "title_left": cp[0] + hp[0] + ICON + margin_start(title_wrap) if title_wrap is not None else None,
            "chevron_right_inset": cp[2] + hp[2],
            "body_left": cp[0] + vp[0],
            "body_right": cp[2] + vp[2],
            "rows": [c.tag.split(".")[-1] for c in list(cont)],
        }

    keys = ["card_left", "card_top", "card_right", "card_bottom", "header_left", "header_right",
            "icon_left", "title_left", "chevron_right_inset", "body_left", "body_right"]
    bad = 0
    print("== 设置页两卡内缩对拍（单位 dp）")
    for k in keys:
        a = specs["语言"][k]
        b = specs["模式"][k]
        if a is None or b is None:
            print(f"  {k:20s} 取值失败：语言={a} 模式={b}")
            bad += 1
            continue
        d = abs(a - b)
        flag = "OK " if d < 0.001 else "差!"
        if d >= 0.001:
            bad += 1
        print(f"  {k:20s} 语言={a:6.1f}  模式={b:6.1f}  Δ={d:4.1f}  {flag}")
    # 行控件类型必须一致（都是自绘卡片，不许一个回去当 RadioButton）
    r1 = specs["语言"]["rows"]
    r2 = specs["模式"]["rows"]
    same = r1 == r2 and all(r == "LanguageRowView" for r in r1) and len(r1) == 3
    print(f"  行结构             语言={r1}  模式={r2}  {'OK' if same else '差!'}")
    if not same:
        bad += 1
    if VERBOSE:
        print("\n  明细：")
        for name, s in specs.items():
            print(f"    {name}: {s}")
    print("== 结论")
    if bad:
        print(f"  FAILED（{bad} 处不一致）")
        return 1
    print("  OK：两张卡的内缩逐项相等")
    return 0


if __name__ == "__main__":
    sys.exit(main())
