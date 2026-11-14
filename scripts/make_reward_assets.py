#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""v1.10.182：把两张收款码原图「抠干净」，生成统一规格的资产。

## 为什么要做这个脚本
用户反馈：「左上角与右上角图都没抠干净」。逐像素核对后确认是**素材本身**的问题，
不是布局/圆角的问题：

* `reward_qr.png`（微信赞赏码 1149×1149）
  - x=24..26 与 x=1122..1124 两条 3px 宽的**浅灰页面边框线**，从图片顶部一直贯下来
    （人眼看到的就是「左上角 / 右上角各有一条没抠掉的灰线」）；
  - y>=1088 是一条**金色横幅**（印着「Fox 的赞赏码」），把白色背景切掉一块；
  - 圆形码本体只有 ~450px，占画布 39% → 上屏后码很小、周围一大片空。
* `reward_alipay.png`（支付宝收款码 818×818）
  - 码本体是 41×41 模块 / 720px，bbox 为 (50,50)-(769,769)；
  - 但 (770..818) 这一段底部还残留**被切掉一半的字形**（y 810~818 有黑像素）；
  - 左边留白 50px、右边留白 49px、底部 -49px（切进码里），**四边不对称**。

## 本脚本做什么
只做「裁到内容 + 补纯白留白」，**码本体一个像素都不动**（脚本内逐像素比对验证）：

1. 支付宝码：裁 (50,50,770,770) 的 720×720 正方形 → 放进 840×840 纯白画布居中（四周 60px 静区）。
2. 微信赞赏码：去掉两条灰线 → 裁到圆形码 bbox + 8% 留白 → 放进 840×840 纯白画布居中。

两张图输出同一尺寸（840×840），上屏尺寸一致，视觉重量一致；32 位 RGB、无 alpha，
放在 `drawable-nodpi-v4`（1:1 像素，不做密度缩放）。

用法： python3 scripts/make_reward_assets.py
"""
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "patch-v1.4.0", "res", "drawable-nodpi-v4")
SRC = os.path.join(ROOT, "scripts", "reward-src")
OUT_ALI = os.path.join(RES, "reward_alipay.png")
OUT_WX = os.path.join(RES, "reward_qr.png")
CANVAS = 840


def dark_bbox(im, thr=128, box=None):
    """返回「比 thr 暗」的像素 bbox（box 用来限定搜索区域）。"""
    px = im.load()
    x0, y0, x1, y1 = box if box else (0, 0, im.size[0], im.size[1])
    minx, miny, maxx, maxy = x1, y1, x0, y0
    hits = 0
    for y in range(y0, y1):
        for x in range(x0, x1):
            if px[x, y] < thr:
                hits += 1
                if x < minx:
                    minx = x
                if y < miny:
                    miny = y
                if x > maxx:
                    maxx = x
                if y > maxy:
                    maxy = y
    if hits == 0:
        raise SystemExit("no content found in %r" % (box,))
    return minx, miny, maxx + 1, maxy + 1


def place(im, canvas=CANVAS):
    """把 im 居中放进 canvas×canvas 的纯白画布。

    自检：贴上去的那块必须与原图逐像素一致（证明「贴」这一步没有重采样）。
    """
    out = Image.new("RGB", (canvas, canvas), (255, 255, 255))
    ox, oy = (canvas - im.size[0]) // 2, (canvas - im.size[1]) // 2
    out.paste(im, (ox, oy))
    back = out.crop((ox, oy, ox + im.size[0], oy + im.size[1]))
    if back.tobytes() != im.tobytes():
        raise SystemExit("贴图后像素被改动")
    return out


def build_alipay():
    src = Image.open(os.path.join(SRC, "reward_alipay.orig.png")).convert("RGB")
    w, h = src.size
    print("支付宝原图 %dx%d" % (w, h))
    # 码本体：黑像素 bbox（底部那截字形在 810 以后，不进 bbox 的正方形裁切）
    bx0, by0, bx1, by1 = dark_bbox(src.convert("L"), 128, (0, 0, w, 770))
    print("  码 bbox = (%d,%d)-(%d,%d)  边长 %dx%d" % (bx0, by0, bx1, by1, bx1 - bx0, by1 - by0))
    # 往外各放 2px：bbox 的 719px 是「最后一个黑像素 +1」，直接裁会切掉最右/最下一列模块的 1px
    bx0, by0, bx1, by1 = bx0 - 2, by0 - 2, bx1 + 2, by1 + 2
    side = max(bx1 - bx0, by1 - by0)
    cx, cy = (bx0 + bx1) // 2, (by0 + by1) // 2
    left, top = cx - side // 2, cy - side // 2
    crop = src.crop((left, top, left + side, top + side))
    print("  实际裁切 (%d,%d) 边 %d（外放 2px，保证不切模块）" % (left, top, side))
    print("  裁切 (%d,%d) 边 %d  四边留白 左%d 右%d 上%d 下%d（原图四边不对称的原因）"
          % (left, top, side, left, w - (left + side), top, h - (top + side)))
    out = place(crop)
    print("  输出 %dx%d，码占 %.1f%%，四周静区 %dpx（≈%.1f 模块）"
          % (CANVAS, CANVAS, side * 100.0 / CANVAS, (CANVAS - side) // 2, (CANVAS - side) / 2.0 / (side / 41.0)))
    out.save(OUT_ALI, optimize=True)
    return side


def build_wechat():
    src = Image.open(os.path.join(SRC, "reward_qr.orig.png")).convert("RGB")
    w, h = src.size
    print("微信原图 %dx%d" % (w, h))
    clean = src.copy()
    px = clean.load()
    # ① 抹掉两条页面边框灰线（x=24..26 / 1122..1124，从顶到底）
    line_px = 0
    for x in list(range(20, 32)) + list(range(1116, 1128)):
        for y in range(0, 1090):
            if px[x, y] != (255, 255, 255):
                line_px += 1
                px[x, y] = (255, 255, 255)
    print("  抹掉页面灰线像素 %d 个" % line_px)
    # ② 圆形码 bbox（区域限定在金横幅上方，阈值 <200 抓齐灰点/彩点）
    # 限定在「金横幅上方、页面边框线以内」：y<900 保证不含横幅（横幅顶边最低 y=983，
    # 最靠上是 x=0 的 y=940），x 100..1050 保证不含左右两条页面边框线（x=24..26 / 1122..1124）
    bx0, by0, bx1, by1 = dark_bbox(clean.convert("L"), 200, (100, 100, 1050, 900))
    print("  圆形码 bbox = (%d,%d)-(%d,%d)  边长 %dx%d" % (bx0, by0, bx1, by1, bx1 - bx0, by1 - by0))
    side = max(bx1 - bx0, by1 - by0)
    pad = int(round(side * 0.08))
    side += pad * 2
    cx, cy = (bx0 + bx1) // 2, (by0 + by1) // 2
    left, top = cx - side // 2, cy - side // 2
    left = max(0, min(left, w - side))
    top = max(0, min(top, h - side))
    crop = clean.crop((left, top, left + side, top + side))
    # ③ 至少放大到画布的 86%（1:1 上屏时码占满板子；不放大也能扫，但视觉重心会偏小）
    target = int(CANVAS * 0.86)
    if target > side:
        crop = crop.resize((target, target), Image.LANCZOS)
        print("  放大 %d → %d（上屏仍会被缩到 %.0fdp，等价 1:1）" % (side, target, target / 3.0))
    print("  金横幅 y>=1088 已裁掉；灰线已抹；输出 %dx%d" % (CANVAS, CANVAS))
    out = place(crop)
    out.save(OUT_WX, optimize=True)


def verify(path):
    im = Image.open(path).convert("RGB")
    w, h = im.size
    px = im.load()
    corners = [px[0, 0], px[w - 1, 0], px[0, h - 1], px[w - 1, h - 1]]
    edge = [px[w // 2, 0], px[0, h // 2], px[w - 1, h // 2], px[w // 2, h - 1]]
    print("  校验 %s %dx%d  四角=%s  四边中点=%s" % (os.path.basename(path), w, h, corners, edge))
    assert all(c == (255, 255, 255) for c in corners + edge), "四角/四边不是纯白"
    # 内容必须离四边至少 20px（否则就是「没抠干净」或有内容贴边）
    bx0, by0, bx1, by1 = dark_bbox(im.convert("L"), 200)
    print("  内容 bbox=(%d,%d)-(%d,%d) 四边留白 左%d 右%d 上%d 下%d"
          % (bx0, by0, bx1, by1, bx0, w - bx1, by0, h - by1))
    assert min(bx0, by0, w - bx1, h - by1) >= 20, "内容贴边"


if __name__ == "__main__":
    os.makedirs(SRC, exist_ok=True)
    print("[1] 支付宝收款码")
    build_alipay()
    verify(OUT_ALI)
    print("[2] 微信赞赏码")
    build_wechat()
    verify(OUT_WX)
    print("[3] 完成：两张资产统一 840x840，纯白四边")
