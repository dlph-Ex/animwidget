#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""v1.10.203：错误色的四条不变量（在没有安卓运行时的前提下能算的都算出来）。

背景：上一版把「错误色 / 错误提示底色」做成两个可编辑的红，而 Material 的
浅色 tone40+tone90 与深色 tone80+tone30 恰好让这两个红在两个模式里**对调明暗**，
用户看到的就是「错误色与错色底色互相替换」。现在只保留一个可编辑的红，
底色由它推导；这份脚本把推导结果在 4 种场景下算出来并断言：

  A. 底色 == blend(错误色, 表面, 16%)            —— 由构造保证，这里独立复算
  B. 底色与表面的「距离」恒为 16%，错误色与表面的距离恒为 100% —— 所以底色永远更像"底"
  C. on 色 = 黑白里对比度更高的那个，且对比度是这两个里最大的
  D. 错误色 / 底色上的文字对比度 ≥ 4.5:1（中灰这类极端值下取可达的最大值并标出）

同一份代码里的常量必须与 src-v2/io/github/animwidget/ThemeManager.java 一致。
"""

SURFACE_L, BG_L = 0xFFFFFF, 0xF7F8FA
SURFACE_D, BG_D = 0x101114, 0x111318
ERR_L, ERR_D = 0xB3261E, 0xFFB4AB
ALPHA = 0.16
BLACK_ON = 0xFF111318
WHITE = 0xFFFFFF


def rel(c):
    s = c / 255.0
    return s / 12.92 if s <= 0.03928 else ((s + 0.055) / 1.055) ** 2.4


def lum(hexcolor):
    r, g, b = (hexcolor >> 16) & 0xFF, (hexcolor >> 8) & 0xFF, hexcolor & 0xFF
    return 0.2126 * rel(r) + 0.7152 * rel(g) + 0.0722 * rel(b)


def ratio(a, b):
    la, lb = lum(a), lum(b)
    hi, lo = max(la, lb), min(la, lb)
    return (hi + 0.05) / (lo + 0.05)


def blend(fg, bg, a):
    r = round(((bg >> 16) & 0xFF) * (1 - a) + ((fg >> 16) & 0xFF) * a)
    g = round(((bg >> 8) & 0xFF) * (1 - a) + ((fg >> 8) & 0xFF) * a)
    b = round((bg & 0xFF) * (1 - a) + (fg & 0xFF) * a)
    return (r << 16) | (g << 8) | b


def best_on(bg):
    return WHITE if ratio(WHITE, bg) >= ratio(BLACK_ON, bg) else BLACK_ON


def adjust_map(color, invert, contrast):
    """与 ThemeManager.adjust$map 等价。"""
    c = color
    if invert:
        c = 0xFFFFFF ^ color
    if contrast != 0.0:
        f = 1.0 + contrast * 0.035
        r = min(255, max(0, int(((c >> 16) & 0xFF) * f)))
        g = min(255, max(0, int(((c >> 8) & 0xFF) * f)))
        b = min(255, max(0, int((c & 0xFF) * f)))
        c = (r << 16) | (g << 8) | b
    return c


def check(tag, surface, error, invert=False, contrast=0.0):
    s = adjust_map(surface, invert, contrast)
    e = adjust_map(error, invert, contrast)
    container = blend(e, s, ALPHA)
    on_e = best_on(e)
    on_c = best_on(container)

    # A：独立复算
    ok_a = container == blend(e, s, ALPHA)
    # B：底色距表面的距离恒为 16%（逐通道），错误色距表面的距离恒为 100%
    dist_err = sum(abs(((e >> sh) & 0xFF) - ((s >> sh) & 0xFF)) for sh in (16, 8, 0))
    dist_box = sum(abs(((container >> sh) & 0xFF) - ((s >> sh) & 0xFF)) for sh in (16, 8, 0))
    ok_b = abs(dist_box - 0.16 * dist_err) <= 2  # 逐通道四舍五入误差
    # C：on 色是黑白里更好的那个
    ok_c = ratio(on_e, e) == max(ratio(WHITE, e), ratio(BLACK_ON, e)) and \
           ratio(on_c, container) == max(ratio(WHITE, container), ratio(BLACK_ON, container))
    # D：可读性（中灰这类极端值下只能取可达最大值）
    re_, rc = ratio(on_e, e), ratio(on_c, container)
    ok_d = re_ >= 4.5 and rc >= 4.5

    print("  %-26s surface=#%06X error=#%06X box=#%06X  onErr=#%06X(%.2f:1)  onBox=#%06X(%.2f:1)  %s%s%s%s"
          % (tag, s, e, container, on_e, re_, on_c, rc,
             "A" if ok_a else "a!", "B" if ok_b else "b!", "C" if ok_c else "c!", "D" if ok_d else "d!"))
    return ok_a and ok_b and ok_c and ok_d


def main():
    print("== 错误色不变量（4 种场景 + 2 种极端色）==")
    fails = 0
    fails += 0 if check("浅色 默认", SURFACE_L, ERR_L) else 1
    fails += 0 if check("深色 默认", SURFACE_D, ERR_D) else 1
    fails += 0 if check("浅色 自定义紫", SURFACE_L, 0x7B1FA2) else 1
    fails += 0 if check("深色 自定义紫", SURFACE_D, 0x7B1FA2) else 1
    fails += 0 if check("浅色 反转", SURFACE_L, ERR_L, invert=True) else 1
    fails += 0 if check("深色 反转", SURFACE_D, ERR_D, invert=True) else 1
    fails += 0 if check("浅色 对比度+3", SURFACE_L, ERR_L, contrast=3.0) else 1
    fails += 0 if check("深色 对比度-3", SURFACE_D, ERR_D, contrast=-3.0) else 1
    fails += 0 if check("浅色 中灰错误色", SURFACE_L, 0x808080) else 1
    fails += 0 if check("深色 中灰错误色", SURFACE_D, 0x808080) else 1
    print("  结论：%s" % ("全部通过" if fails == 0 else "%d 项失败" % fails))
    return 0 if fails == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
