#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""v1.10.512：「判断代号」口令门的**结构示意**（不是截图）。

画的是 GlassPrompt + FrostedGlassDrawable 的真实层序与真实参数：
   ① 页面 → 缩到 1/6 → 两遍盒式模糊（r = min/26）→ 铺满全屏，再压一层 scrim（深 0x4A / 浅 0x2A）
   ② 卡片区域再糊一遍（r = min/14）当「玻璃内芯」
   ③ 表面色 surface @ 0.62（深）/ 0.66（浅），上淡下深
   ④ 左上镜面高光 + 底边内阴影 + 顶棱线 + 26dp 渐变描边 + 20dp 软投影
用来核对的就三件事：**底糊没糊、卡片透不透、卡片里比外面更糊**。
"""
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import numpy as np

FONT_R = "/workspace/AnimWidget/patch-v1.4.0/res/font/anim_dual_r.ttf"
FONT_B = "/workspace/AnimWidget/patch-v1.4.0/res/font/anim_dual_sb.ttf"
S = 2                      # 1dp -> 2px
W = 360 * S                # 屏宽 360dp
H = 780 * S                # 屏高 780dp
RADIUS = 26 * S
CARD_W = int(min(W * 0.84, 352 * S))

DARK = dict(dark=True, bg=-15658216, surface=-14934236, neutral=-14012875,
            text=-1710101, muted=-6511697, primary=0xFF22C55E, on_primary=-15658216,
            scrim=0x4A)
LIGHT = dict(dark=False, bg=-526086, surface=-1, neutral=-1578258,
             text=-14735049, muted=-9735552, primary=0xFF22C55E, on_primary=-15658216,
             scrim=0x2A)


def col(v, a=255):
    return ((v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF, a)


def rgb(v):
    return ((v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF)


def shade(v, factor):
    return tuple(min(255, max(0, int(c * factor))) for c in rgb(v))


def font(dp, bold=False):
    return ImageFont.truetype(FONT_B if bold else FONT_R, int(dp * S))


def fake_page(th):
    """伪造日志页：标题 + 按钮排 + 提示行 + 控制台文字。只为让模糊底有东西可糊。"""
    img = Image.new("RGB", (W, H), rgb(th["bg"]))
    d = ImageDraw.Draw(img)
    f13, f14, f11 = font(13), font(14), font(11)
    # 顶部：返回 + 标题
    d.ellipse((14 * S, 22 * S, 46 * S, 54 * S), fill=rgb(th["neutral"]))
    d.text((78 * S, 26 * S), "日志", font=font(18, True), fill=rgb(th["text"]))
    # 按钮排
    x = 16 * S
    for label in ("清屏", "撤回", "导出日志"):
        wpx = d.textlength(label, font=f13) + 30 * S
        d.rounded_rectangle((x, 74 * S, x + wpx, 112 * S), radius=999, fill=rgb(th["neutral"]))
        d.text((x + 15 * S, 84 * S), label, font=f13, fill=rgb(th["text"]))
        x += wpx + 10 * S
    # 提示行
    d.text((18 * S, 128 * S), "日志只保留 30 分钟，过期自动清掉", font=f13, fill=rgb(th["muted"]))
    # 控制台正文
    lines = [
        "J01 日志页打开",
        "W10 桌面播放 50.0fps 丢 0 · 1280x720",
        "L22 解析开始 685.gif 711 帧",
        "P12 预览 idx=0 起首圈丢到 131",
        "W48 WebP 21.1ms / JPEG 6.4ms",
        "B04 烘焙 593 帧 13.8s（8 线程 42 帧/s）",
        "E07 导出 593 帧 69s（单线程 144ms/帧）",
        "F12 动图延时(ms) 前8帧=[100,20,100,100]",
        "W33 堆上限 256MB 未开 largeHeap",
        "J11 判断代号入口",
        "S03 探头判据 像素中心 OK",
        "T26 时长 11842ms",
    ]
    y = 160 * S
    for i, line in enumerate(lines):
        d.text((18 * S, y), line, font=f11, fill=rgb(th["text"] if i % 3 else th["muted"]))
        y += 22 * S
    return img


def box_blur(img, r):
    """PIL 的 GaussianBlur 是近似的：把「两遍半径 r 的盒式模糊」折算成 sigma。"""
    return img.filter(ImageFilter.GaussianBlur(r * 0.816))


def prepare(th):
    page = fake_page(th)
    small = page.resize((max(8, W // 6), max(8, H // 6)), Image.BILINEAR)
    r1 = max(2, min(small.size) // 26)
    r2 = max(3, min(small.size) // 14)
    soft = box_blur(small, r1)
    frost = box_blur(soft, r2)
    return (soft.resize((W, H), Image.BILINEAR).convert("RGB"),
            frost.resize((W, H), Image.BILINEAR).convert("RGB"))


def vertical_gradient(w, h, stops):
    """stops = [(位置0~1, (r,g,b,a)), ...] 竖向渐变。"""
    ys = np.linspace(0.0, 1.0, h, dtype=np.float32)
    out = np.zeros((h, w, 4), dtype=np.float32)
    for i in range(len(stops) - 1):
        p0, c0 = stops[i]
        p1, c1 = stops[i + 1]
        m = (ys >= p0) & (ys <= p1)
        if not m.any():
            continue
        t = ((ys[m] - p0) / max(1e-6, p1 - p0))[:, None]
        for k in range(4):
            out[m, :, k] = c0[k] + (c1[k] - c0[k]) * t
    return Image.fromarray(out.astype(np.uint8), "RGBA")


def radial_glow(w, h, cx, cy, radius, color, peak):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    dist = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / max(1.0, radius)
    a = np.clip(1.0 - dist, 0.0, 1.0) ** 1.6 * peak
    arr = np.zeros((h, w, 4), dtype=np.uint8)
    arr[:, :, 0] = color[0]
    arr[:, :, 1] = color[1]
    arr[:, :, 2] = color[2]
    arr[:, :, 3] = (a * 255).astype(np.uint8)
    return Image.fromarray(arr, "RGBA")


def diag_gradient(w, h, colors):
    """左上 → 右下 的描边渐变（等价于 LinearGradient 的 0.62w 方向）。"""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    t = np.clip((xx / max(1, w) * 0.72 + yy / max(1, h) * 0.28), 0.0, 1.0)
    stops = [0.0, 0.38, 0.72, 1.0]
    out = np.zeros((h, w, 4), dtype=np.uint8)
    for i in range(len(stops) - 1):
        m = (t >= stops[i]) & (t <= stops[i + 1])
        f = (t[m] - stops[i]) / max(1e-6, stops[i + 1] - stops[i])
        c0, c1 = colors[i], colors[i + 1]
        for k in range(4):
            plane = out[:, :, k]
            plane[m] = (c0[k] + (c1[k] - c0[k]) * f).astype(np.uint8)
    return Image.fromarray(out, "RGBA")


def rounded_mask(w, h, r):
    m = Image.new("L", (w, h), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, w - 1, h - 1), radius=r, fill=255)
    return m


def render(th):
    soft, frost = prepare(th)
    img = soft.copy()
    # ① 系统压暗（scrim 一层）
    img = Image.alpha_composite(img.convert("RGBA"),
                                Image.new("RGBA", (W, H), (0, 0, 0, th["scrim"])))
    # 卡片尺寸（标题 24 + 输入框 46 + 按键 58 + 间距 + 内边距 = 194dp）
    # 标题 24 + 输入框上间距 14 + 输入框 44 + 行间距 12 + 按钮行 56 + 上下内边距 32
    card_h = int(24 * S + 14 * S + 44 * S + 12 * S + 56 * S + 32 * S)
    x0 = (W - CARD_W) // 2
    y0 = (H - card_h) // 2
    mask = rounded_mask(CARD_W, card_h, RADIUS)

    # ② 软投影（elevation 20dp）
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sm = Image.new("L", (CARD_W, card_h), 0)
    ImageDraw.Draw(sm).rounded_rectangle((0, 0, CARD_W - 1, card_h - 1), radius=RADIUS, fill=255)
    shadow.paste(Image.new("RGBA", (CARD_W, card_h),
                           (0, 0, 0, 110 if th["dark"] else 60)), (x0, y0 + 6 * S), sm)
    shadow = shadow.filter(ImageFilter.GaussianBlur(16 * S))
    img = Image.alpha_composite(img, shadow)

    # ③ 玻璃内芯：卡片那块再糊一遍（这就是「厚度」）
    core = frost.crop((x0, y0, x0 + CARD_W, y0 + card_h)).convert("RGBA")
    card = Image.new("RGBA", (CARD_W, card_h), (0, 0, 0, 0))
    card.paste(core, (0, 0), mask)

    # ④ 表面色：上 0.62 / 下 0.68（浅色 0.66 / 0.72），且往下压暗
    a_top = 0.62 if th["dark"] else 0.66
    a_bot = min(0.95, a_top + 0.06)
    top_col = col(th["surface"], int(a_top * 255))
    bot_col = tuple(list(shade(th["surface"], 0.78 if th["dark"] else 0.93)) + [int(a_bot * 255)])
    fill = vertical_gradient(CARD_W, card_h, [(0.0, top_col), (1.0, bot_col)])
    fill = Image.composite(fill, Image.new("RGBA", (CARD_W, card_h), (0, 0, 0, 0)), mask)
    card = Image.alpha_composite(card, fill)

    # ⑤ 左上镜面高光
    glow = radial_glow(CARD_W, card_h, CARD_W * 0.14, card_h * 0.10,
                       max(CARD_W, card_h) * 0.72,
                       (255, 255, 255), (0x1C if th["dark"] else 0x30) / 255.0)
    glow = Image.composite(glow, Image.new("RGBA", (CARD_W, card_h), (0, 0, 0, 0)), mask)
    card = Image.alpha_composite(card, glow)

    # ⑥ 底边内阴影（band = 26dp × 1.6）
    band = int(RADIUS * 1.6)
    band_grad = vertical_gradient(CARD_W, band, [(0.0, (0, 0, 0, 0)),
                                                 (1.0, (0, 0, 0, 0x52 if th["dark"] else 0x1A))])
    shadow_band = Image.new("RGBA", (CARD_W, card_h), (0, 0, 0, 0))
    shadow_band.paste(band_grad, (0, card_h - band))
    shadow_band = Image.composite(shadow_band,
                                  Image.new("RGBA", (CARD_W, card_h), (0, 0, 0, 0)), mask)
    card = Image.alpha_composite(card, shadow_band)

    img.paste(card, (x0, y0), mask)

    d = ImageDraw.Draw(img, "RGBA")
    # ⑦ 顶棱线（深色白线 / 浅色墨线，18~22% 高度内淡到 0）
    edge_h = max(int(18 * S), int(card_h * 0.22))
    edge_grad = vertical_gradient(CARD_W, edge_h,
                                  [(0.0, (255, 255, 255, 0x7A if th["dark"] else 0)),
                                   (1.0, (255, 255, 255, 0))] if th["dark"] else
                                  [(0.0, (0, 0, 0, 0x14)), (1.0, (0, 0, 0, 0))])
    edge_mask = Image.new("L", (CARD_W, card_h), 0)
    ImageDraw.Draw(edge_mask).rounded_rectangle((1, 1, CARD_W - 2, card_h - 2),
                                                radius=RADIUS - 1, outline=255,
                                                width=max(1, int(1.2 * S)))
    edge_layer = Image.new("RGBA", (CARD_W, card_h), (0, 0, 0, 0))
    edge_layer.paste(edge_grad, (0, 0))
    edge_layer.putalpha(Image.composite(edge_layer.split()[3], Image.new("L", (CARD_W, card_h), 0),
                                        edge_mask))
    img.alpha_composite(edge_layer, (x0, y0))

    # ⑧ 外圈渐变描边
    # v1.10.510：描边收口（最后一档不再是 0）
    rim_colors = ([(0x9E, 0xFF, 0xFF, 0xFF), (0x55, 0xFF, 0xFF, 0xFF),
                   (0x3C, 0xFF, 0xFF, 0xFF), (0x42, 0xFF, 0xFF, 0xFF)] if th["dark"] else
                  [(0x6E, 0x00, 0x00, 0x00), (0x46, 0x00, 0x00, 0x00),
                   (0x30, 0x00, 0x00, 0x00), (0x32, 0x00, 0x00, 0x00)])
    rim_grad = diag_gradient(CARD_W, card_h, [(c[1], c[2], c[3], c[0]) for c in rim_colors])
    rim_mask = Image.new("L", (CARD_W, card_h), 0)
    ImageDraw.Draw(rim_mask).rounded_rectangle((1, 1, CARD_W - 2, card_h - 2),
                                               radius=RADIUS - 1, outline=255,
                                               width=max(1, int(1.2 * S)))
    rim_layer = rim_grad.copy()
    rim_layer.putalpha(Image.composite(rim_grad.split()[3], Image.new("L", (CARD_W, card_h), 0),
                                       rim_mask))
    img.alpha_composite(rim_layer, (x0, y0))

    # ⑨ 内容：标题 / 输入框 / 两枚胶囊
    pad = 20 * S
    tx = x0 + pad
    ty = y0 + 18 * S
    d.text((tx, ty), "输入口令", font=font(17, True), fill=col(th["text"]))
    field_top = ty + 24 * S + 14 * S
    field_h = 44 * S
    fa = 0.55 if th["dark"] else 0.85
    d.rounded_rectangle((tx, field_top, x0 + CARD_W - pad, field_top + field_h),
                        radius=14 * S, fill=col(th["neutral"], int(fa * 255)))
    d.text((tx + 14 * S, field_top + 12 * S), "该页仅用于核对判断代号",
           font=font(16), fill=col(th["muted"], int(0.92 * 255)))
    row_top = field_top + field_h + 12 * S + 6 * S
    btn_h = 44 * S
    half = (CARD_W - pad * 2 - 10 * S) // 2
    d.rounded_rectangle((tx, row_top, tx + half, row_top + btn_h), radius=999,
                        fill=col(th["neutral"], int(0.85 * 255)))
    cw = d.textlength("取消", font=font(14))
    d.text((tx + (half - cw) / 2, row_top + 12 * S), "取消", font=font(14), fill=col(th["text"]))
    ok_x = tx + half + 10 * S
    d.rounded_rectangle((ok_x, row_top, x0 + CARD_W - pad, row_top + btn_h), radius=999,
                        fill=col(th["primary"]))
    cw2 = d.textlength("确定", font=font(14))
    d.text((ok_x + (half - cw2) / 2, row_top + 12 * S), "确定", font=font(14),
           fill=col(th["on_primary"]))
    return img


def main():
    panels = [render(LIGHT), render(DARK)]
    gap = 24
    out = Image.new("RGB", (W * 2 + gap, H), (24, 24, 24))
    out.paste(panels[0].convert("RGB"), (0, 0))
    out.paste(panels[1].convert("RGB"), (W + gap, 0))
    out.save("/workspace/AnimWidget/docs-diag-gate-v1.10.512.png")
    print("saved docs-diag-gate-v1.10.512.png", out.size)


if __name__ == "__main__":
    main()
