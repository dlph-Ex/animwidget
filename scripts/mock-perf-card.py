#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""v1.10.496：剩余性能卡的**结构示意**（不是截图）。

按 activity_config.xml 里的尺寸（dp）与 PerfBudget 的结论文案、两套配色各画一张，
用来在装机前核对：标题/百分比/分段表/胶囊/结论行的比例与换行位置。
不支持中文的字体在这里会直接画出方块，所以必须用工程自带的 anim_dual_r.ttf。
"""
from PIL import Image, ImageDraw, ImageFont

FONT_R = "/workspace/AnimWidget/patch-v1.4.0/res/font/anim_dual_r.ttf"
S = 2                      # 1dp = 2px
W = 360 * S                # 屏宽（dp→px）
PAD_X, PAD_T, PAD_B = 14 * S, 12 * S, 12 * S


def f(dp, bold=False):
    return ImageFont.truetype(FONT_R, int(dp * S))


def with_alpha(color, a):
    return (color[0], color[1], color[2], int(255 * a))


def body(w_inner, th):
    """在透明层上画卡片内部（左上角为原点），返回 (层, 高度)。"""
    layer = Image.new("RGBA", (int(w_inner), 300 * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    y = 0

    # 第一行：标题 + 大号百分比
    d.text((0, 3 * S), th["title"], font=f(12), fill=th["muted"])
    pct = "%d%%" % th["percent"]
    pw = d.textlength(pct, font=f(19, True))
    d.text((w_inner - pw, y), pct, font=f(19, True), fill=th["accent"])
    y += 26 * S

    # 分段表：10 格，剩余百分比点亮前 N 格
    top, h = y + 9 * S, 10 * S
    gap = min(h * 0.55, w_inner * 0.02)
    seg = (w_inner - gap * 9) / 10.0
    filled = int(round(th["percent"] * 10 / 100.0))
    if filled == 0 and th["percent"] > 0:
        filled = 1
    for i in range(10):
        sx = i * (seg + gap)
        d.rounded_rectangle((sx, top, sx + seg, top + h), radius=h / 2,
                            fill=th["accent"] if i < filled else th["track"])
    y = top + h + 9 * S

    # 胶囊两枚
    cx = 0
    for label in (th["chip_used"], th["chip_lanes"]):
        cw = d.textlength(label, font=f(11)) + 18 * S
        d.rounded_rectangle((cx, y, cx + cw, y + 17 * S), radius=8.5 * S,
                            fill=with_alpha(th["chip"], 0.14))
        d.text((cx + 9 * S, y + 1.5 * S), label, font=f(11), fill=th["chip"])
        cx += cw + 6 * S
    y += 17 * S + 8 * S

    # 结论行（最多 2 行，超出用 …）
    out, buf = [], ""
    for ch in th["detail"]:
        if d.textlength(buf + ch, font=f(11)) <= w_inner:
            buf += ch
        else:
            out.append(buf)
            buf = ch
    out.append(buf)
    if len(out) > 2:
        out = out[:2]
        while d.textlength(out[1] + "…", font=f(11)) > w_inner:
            out[1] = out[1][:-1]
        out[1] += "…"
    for i, ln in enumerate(out):
        d.text((0, y + i * 16 * S), ln, font=f(11), fill=th["detail_color"])
    y += len(out) * 16 * S
    return layer.crop((0, 0, int(w_inner), int(y))), int(y)


def panel(title, th):
    w = W - 32 * S
    layer, h_inner = body(w - 2 * PAD_X, th)
    card_h = h_inner + PAD_T + PAD_B

    img = Image.new("RGB", (W, card_h + 96 * S), th["bg"])
    d = ImageDraw.Draw(img, "RGBA")
    d.text((16 * S, 8 * S), title, font=f(12), fill=th["muted"])

    top = 28 * S
    d.rounded_rectangle((16 * S, top, 16 * S + w, top + card_h), radius=22 * S, fill=th["surface"])
    img.paste(layer, (16 * S + PAD_X, top + PAD_T), layer)

    # 卡片下面紧跟「素材」分区标题：示意它确实排在素材项上方
    d.text((16 * S, top + card_h + 26 * S), "素材", font=f(12), fill=th["muted"])
    return img


LIGHT = dict(
    bg=(242, 242, 242), surface=(255, 255, 255), title="剩余性能空间",
    percent=62, accent=(79, 70, 229), muted=(96, 96, 102), track=(226, 226, 230),
    chip_used="已用 1.6/4.2 核", chip_lanes="2 路播放",
    detail="还能再加 1 个 1280×720·50fps",
)
LIGHT["detail_color"] = LIGHT["muted"]
LIGHT["chip"] = LIGHT["accent"]

DARK = dict(
    bg=(16, 17, 20), surface=(26, 28, 32), title="剩余性能空间",
    percent=14, accent=(239, 68, 68), muted=(152, 154, 160), track=(58, 60, 66),
    chip_used="已用 3.6/4.2 核", chip_lanes="4 路播放",
    detail="不建议再加 1280×720·50fps：单路就要 1.2 核，超出余量",
)
DARK["detail_color"] = DARK["accent"]
DARK["chip"] = (129, 140, 248)   # 深色下主色的可读变体（tag="chip_value" 走 primary）

a = panel("浅色 / 宽裕（主色）", LIGHT)
b = panel("深色 / 超预算（错误色）", DARK)
out = Image.new("RGB", (W, a.height + b.height + 6 * S), (120, 120, 120))
out.paste(a, (0, 0))
out.paste(b, (0, a.height + 6 * S))
out.save("/chat/outputs/perf-card-496.png")
print("wrote /chat/outputs/perf-card-496.png", out.size)
