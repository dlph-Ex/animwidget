#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
阴影模型对比图（**模型复算，不是设备截图**）。

三块画布尺寸相同（480×320，就是"组件区域"），画面框每边内缩 = 该版本的留白：

  A v1.10.227      留白 2×模糊半径 = 5.71% 宽（照抄参考实现 pad，外圈是空的）
  B v1.10.228 默认  留白 1×模糊半径 = 2.86% 宽（= 阴影可见延伸，A 的一半）
  C v1.10.228 投影感 扩散 2% + 模糊 2.9%（贴边更深、更实）

剖面公式（两个版本共用，都是参考实现那条高斯）：
    α(d) = (强度+20)/255 · Φ((扩散 − d)/σ),   σ = 模糊半径/√3
"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFont

W, H = 480, 320
R = 44
BG = (196, 216, 152)
CARD = (120, 128, 200)
SCALE = 2

ALPHA_BIAS = 20
STRENGTH = 100
SIGMA_PER_BLUR = 1.0 / math.sqrt(3.0)


def sd_round_rect(x, y, w, h, r):
    cx, cy = w / 2.0, h / 2.0
    qx = abs(x - cx) - (cx - r)
    qy = abs(y - cy) - (cy - r)
    ax, ay = max(qx, 0.0), max(qy, 0.0)
    return math.hypot(ax, ay) + min(max(qx, qy), 0.0) - r


def alpha_at(d, width, dilate_frac, blur_frac, strength=STRENGTH):
    blur = max(blur_frac * width, 0.5)
    sigma = blur * SIGMA_PER_BLUR
    dilate = dilate_frac * width
    a = (strength + ALPHA_BIAS) / 255.0
    return a * 0.5 * math.erfc((d - dilate) / (sigma * math.sqrt(2.0)))


def inset_of(width, dilate_frac, blur_frac, strength=STRENGTH, vis=5.0 / 255.0, edge=0.0):
    cut = max(vis, edge * alpha_at(0.0, width, dilate_frac, blur_frac, strength))
    lo, hi = 0.0, 1.0
    for _ in range(60):
        mid = (lo + hi) / 2.0
        if alpha_at(mid * width, width, dilate_frac, blur_frac, strength) > cut:
            lo = mid
        else:
            hi = mid
    return hi * width


def render(dilate_frac, blur_frac, inset_frac):
    """inset_frac = 每边留白占画布宽的比例（画布 = 组件区域）。"""
    inset = inset_frac * W
    img = Image.new("RGB", (W * SCALE, H * SCALE), BG)
    px = img.load()
    iw, ih = W - inset * 2.0, H - inset * 2.0
    for j in range(H * SCALE):
        y = j / float(SCALE)
        for i in range(W * SCALE):
            x = i / float(SCALE)
            d = sd_round_rect(x - inset, y - inset, iw, ih, R * iw / W)
            if d <= 0:
                px[i, j] = CARD
            else:
                # 超出留白就是被截断了（收边那道沿），不能再画
                a = alpha_at(d, W, dilate_frac, blur_frac) if d <= inset else 0.0
                px[i, j] = (int(BG[0] * (1 - a)), int(BG[1] * (1 - a)), int(BG[2] * (1 - a)))
    return img


def label(img, text):
    dr = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("DejaVuSans.ttf", 21)
    except Exception:
        font = ImageFont.load_default()
    dr.rectangle([0, 0, img.width, 32], fill=(24, 26, 32))
    dr.text((9, 6), text, fill=(235, 238, 245), font=font)
    return img


def pad(img, m=12):
    out = Image.new("RGB", (img.width + m * 2, img.height + m * 2), (24, 26, 32))
    out.paste(img, (m, m))
    return out


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "docs-shadow-v1.10.233.png")

    panels = [
        ("default  2.6% / 3.0% + edge 35%  (3.4%)", 0.026, 0.030),
        ("wide-ish soft  1.3% / 3.5% (4.8%, no edge)", 0.013, 0.035),
        ("wide  0 / 5.7%  (5.7%)", 0.0, 0.057),
        ("save-space  1.2% / 0.3% + edge 40%  (1.2%)", 0.012, 0.003),
    ]
    panels = [(t, d, b, inset_of(W, d, b, edge=0.40 if t.startswith("save-space")
                                 else (0.35 if t.startswith("default") else 0.0)) / W)
              for (t, d, b) in panels]
    imgs = [pad(label(render(d, b, i), t)) for (t, d, b, i) in panels]
    gap = 14
    total = Image.new("RGB", (sum(p.width for p in imgs) + gap * (len(imgs) - 1), max(p.height for p in imgs)),
                      (18, 20, 24))
    x = 0
    for p in imgs:
        total.paste(p, (x, 0))
        x += p.width + gap
    total.save(out)
    print("wrote", out, total.size)

    print("\n四档参数（画布宽 %d）：" % W)
    print("   %-22s %-9s %-9s %-9s %-9s" % ("档位", "扩散", "模糊px", "留白px", "贴边α/255"))
    for (t, d, b, ins) in panels:
        print("   %-22s %-9.2f %-9.1f %-9.1f %-9.1f"
              % (t, d * 100, b * W, ins * W, alpha_at(0.0, W, d, b) * 255))
    print("\n   留白 = 阴影可见延伸（α 掉到 5/255 的距离），不是固定值。")


if __name__ == "__main__":
    main()
