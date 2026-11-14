#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""v1.10.497：「当前方案正在输出」弹窗的**结构示意**（不是截图）。

按 OutputBusyDialog 的真实尺寸（dp）画两套：浅色 / 深色。
背景是「页面截图 → 缩到 1/6 → 两遍盒式模糊」的等价做法，用来核对模糊浓度、
卡片宽度（屏宽 0.82，上限 340dp）与两行的间距；文字用工程自带字体，缺字形会直接画成方块。
"""
from PIL import Image, ImageDraw, ImageFont, ImageFilter

FONT_R = "/workspace/AnimWidget/patch-v1.4.0/res/font/anim_dual_r.ttf"
S = 2
W = 360 * S
H = 520 * S


def f(dp):
    return ImageFont.truetype(FONT_R, int(dp * S))


def with_alpha(c, a):
    return (c[0], c[1], c[2], int(255 * a))


def fake_page(dst, th):
    """伪造「编辑页」的一角：只为让模糊底有东西可糊（与真机布局无关）。"""
    d = ImageDraw.Draw(dst)
    d.rectangle((0, 0, W, H), fill=th["bg"])
    d.rounded_rectangle((16 * S, 12 * S, W - 16 * S, 96 * S), radius=18 * S, fill=th["card"])
    d.text((34 * S, 40 * S), "素材", font=f(13), fill=th["muted"])
    d.rounded_rectangle((16 * S, 108 * S, W - 16 * S, 200 * S), radius=18 * S, fill=th["card"])
    d.text((34 * S, 140 * S), "布局 · 形状", font=f(13), fill=th["muted"])
    for i in range(3):
        y = 216 * S + i * 46 * S
        d.rounded_rectangle((16 * S, y, W - 16 * S, y + 36 * S), radius=18 * S, fill=th["card"])
        d.text((34 * S, y + 10 * S), "参数 %d" % (i + 1), font=f(13), fill=th["muted"])


def blurred(dst):
    small = dst.resize((max(8, W // 6), max(8, H // 6)), Image.BILINEAR)
    small = small.filter(ImageFilter.GaussianBlur(3))
    return small.resize((W, H), Image.BILINEAR)


def panel(title, th):
    base = Image.new("RGB", (W, H), th["bg"])
    fake_page(base, th)
    img = blurred(base).convert("RGB")
    d = ImageDraw.Draw(img, "RGBA")
    # 压暗（与 BlurBackdropView 的 scrim 同值：深色 0x3A / 浅色 0x22）
    d.rectangle((0, 0, W, H), fill=(0, 0, 0, 0x3A if th["dark"] else 0x22))

    cw = min(int(W * 0.82), 340 * S)
    line_h = 22 * S
    box_h = 22 * S + line_h + 18 * S + 46 * S + 18 * S
    x0 = (W - cw) // 2
    y0 = (H - box_h) // 2
    # 玻璃卡：surface 94% + 1.2dp 描边
    d.rounded_rectangle((x0, y0, x0 + cw, y0 + box_h), radius=24 * S,
                        fill=with_alpha(th["surface"], 0.94),
                        outline=with_alpha(th["text"], 0.22), width=max(1, int(1.2 * S)))
    # 第一行
    text = "当前方案正在输出"
    tw = d.textlength(text, font=f(15))
    d.text((x0 + (cw - tw) / 2, y0 + 22 * S + 2 * S), text, font=f(15), fill=th["text"])
    # 第二行：确认
    by = y0 + 22 * S + line_h + 18 * S
    d.rounded_rectangle((x0 + 22 * S, by, x0 + cw - 22 * S, by + 46 * S), radius=23 * S,
                        fill=th["primary"])
    ok = "确认"
    ow = d.textlength(ok, font=f(14))
    d.text((x0 + (cw - ow) / 2, by + 14 * S), ok, font=f(14), fill=th["on_primary"])

    d.text((16 * S, H - 24 * S), title, font=f(11), fill=th["muted"])
    return img


LIGHT = dict(bg=(242, 242, 242), card=(255, 255, 255), surface=(255, 255, 255),
             text=(24, 24, 27), muted=(110, 110, 116), primary=(79, 70, 229),
             on_primary=(255, 255, 255), dark=False)
DARK = dict(bg=(16, 17, 20), card=(28, 30, 34), surface=(24, 26, 30),
            text=(238, 238, 240), muted=(150, 152, 158), primary=(129, 140, 248),
            on_primary=(12, 12, 16), dark=True)

a = panel("浅色：模糊底 + 卡片（入场 300ms Ease.OUT，退场 200ms Ease.IN_OUT）", LIGHT)
b = panel("深色：同一个「一条进度同时驱动卡片与模糊」", DARK)
out = Image.new("RGB", (W * 2 + 8 * S, H), (120, 120, 120))
out.paste(a, (0, 0))
out.paste(b, (W + 8 * S, 0))
out.save("/chat/outputs/output-busy-497.png")
print("wrote /chat/outputs/output-busy-497.png", out.size)
