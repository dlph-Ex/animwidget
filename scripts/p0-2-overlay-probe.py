#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P0-2「不透明内容帧 JPEG + 常驻 alpha overlay」几何验证（v1.10.478 起）

外部两份复核（DeepSeek / Claude）都判断这条方案在**圆角蒙版**下几何不成立。
这个脚本用**我们自己的几何与参数**（FrameComposer.drawFrame + ShadowSpec 逐行照抄）
把三种合成路径算出来，叠到真实壁纸上比像素：

  A 旧帧       ：阴影 + DST_OUT 挖洞 + 内容按路径裁剪 + 边框  → 一张带 alpha 的帧
  B 分层-不透明 ：内容 = 矩形不透明（JPEG 的前提）+ overlay（阴影/边框/洞）叠在上面
  C 分层-带 alpha：同上，但内容自己按路径裁过（= 继续用带 alpha 的 WebP）

判据不是「层内自检」（那样必然等于旧帧，恒 0），而是**叠壁纸比**：
  over(over(wall, content), overlay)   vs   over(wall, 旧帧)

用法：
  python3 scripts/p0-2-overlay-probe.py [输出前缀]

  内容与「照片档」壁纸都是**程序合成**的，不读任何用户素材。

输出：<前缀>.png（对比图）、<前缀>.txt（数值）
"""
import sys
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFont

# ---------------------------------------------------------------- 真实参数（照抄代码）
CANVAS_W, CANVAS_H = 879, 869          # 真机 launcher 组件格（vivo com.bbk.launcher2）
SRC_W, SRC_H = 1280, 720               # 235 帧 50fps 那批素材的像素
SHADOW_STRENGTH = 100                  # 日志：阴影=100
ALPHA_PERCENT = 100
MASK_ROUND_UNIT = 0.18                 # ShapeFactory.buildLegacy case ROUNDED: r = unit*0.18
SHADOW_RANGE = 0.05                    # ShadowSpec 默认范围 L = 5% 画面框宽
SHADOW_RHO = 0.55                      # ShadowSpec 默认紧实度 ρ
SHADOW_CUT_INSET = 2.0                 # FrameComposer.SHADOW_CUT_INSET
BORDER = False                         # 日志里的那一档没开边框
# 合成内容：**不使用任何用户素材**（早先拿过一张截图当内容，被用户当场指出）。
# 只要满足三条即可：四角都不是纯白/纯黑、有亮部暗部、有高频细节（细线/噪点）——
# 判「方角露出来没有」靠的就是角上有没有跟壁纸不同的东西。
CONTENT_SEED = 20261109


def z_for(alpha255):
    """z = −Φ⁻¹(CUT/α)，与 ShadowSpec.zFor 同式（CUT = 1/255，二分即可）。"""
    p = 1.0 / 255.0 / (alpha255 / 255.0)      # = 1/alpha255
    lo, hi = 0.0, 8.0
    for _ in range(200):
        mid = (lo + hi) / 2.0
        if norm_cdf(mid) < 1.0 - p:
            lo = mid
        else:
            hi = mid
    return (lo + hi) / 2.0


def norm_cdf(x):
    return 0.5 * (1.0 + math.erf(x / math.sqrt(2.0)))


def _cdf_arr(x):
    x = np.asarray(x, dtype=np.float64)
    return 0.5 * (1.0 + np.vectorize(math.erf)(x / math.sqrt(2.0)))


def content_rect_fit(sw, sh, w, h):
    """FrameComposer.contentRect：内接（原始比例布局）。"""
    nat = sw / sh
    bw, bh = w, w / nat
    if bh > h:
        bh, bw = h, h * nat
    left, top = (w - bw) / 2.0, (h - bh) / 2.0
    return left, top, left + bw, top + bh


def inset_preserving_ratio(rect, margin):
    """FrameComposer.insetPreservingRatio：按比例内缩（不为负）。"""
    if margin <= 0.01:
        return rect
    ax0, ay0, ax1, ay1 = rect
    aw, ah = ax1 - ax0, ay1 - ay0
    maxw, maxh = aw - margin * 2.0, ah - margin * 2.0
    if maxw <= 1.0 or maxh <= 1.0:
        return rect
    ratio = aw / ah
    w = maxw
    h = maxw / ratio
    if h > maxh:
        h = maxh
        w = maxh * ratio
    cx, cy = (ax0 + ax1) / 2.0, (ay0 + ay1) / 2.0
    return (cx - w / 2.0, cy - h / 2.0, cx + w / 2.0, cy + h / 2.0)


def rounded_sdf(xs, ys, rect, r):
    """圆角矩形有符号距离（内负外正）。rect = (x0, y0, x1, y1)。"""
    x0, y0, x1, y1 = rect
    cx, cy = (x0 + x1) / 2.0, (y0 + y1) / 2.0
    qx = np.abs(xs - cx) - ((x1 - x0) / 2.0 - r)
    qy = np.abs(ys - cy) - ((y1 - y0) / 2.0 - r)
    outside = np.hypot(np.maximum(qx, 0.0), np.maximum(qy, 0.0))
    inside = np.minimum(np.maximum(qx, qy), 0.0)
    return outside + inside - r


def sdf_grid(w, h, rect, r):
    xs = np.arange(w, dtype=np.float64)
    ys = np.arange(h, dtype=np.float64)
    gx, gy = np.meshgrid(xs, ys)
    return rounded_sdf(gx, gy, rect, r)


def path_mask(rect, r, w, h, ss=4):
    """路径填充蒙版（4× 超采样抗锯齿），返回 float32 [0,1]。"""
    big = Image.new('L', (w * ss, h * ss), 0)
    d = ImageDraw.Draw(big)
    d.rounded_rectangle([rect[0] * ss, rect[1] * ss, rect[2] * ss, rect[3] * ss],
                        radius=r * ss, fill=255)
    return np.asarray(big.resize((w, h), Image.LANCZOS), dtype=np.float32) / 255.0


def compose_old(content, wall, cfg):
    """旧帧（FrameComposer）：阴影 → 挖洞 → 内容裁剪 → 边框，再叠壁纸。"""
    w, h, rect, r = cfg['w'], cfg['h'], cfg['box'], cfg['r']
    inside = path_mask(rect, r, w, h)
    # 阴影 = 形状外的单层高斯尾：α(d) = A·Φ((δ−d)/σ)
    alpha255 = int(round((SHADOW_STRENGTH + 20) * ALPHA_PERCENT / 100.0))
    L = SHADOW_RANGE * (cfg['area'][2] - cfg['area'][0])
    sigma = max(L / (SHADOW_RHO + z_for(alpha255)), 0.35)
    delta = SHADOW_RHO * sigma
    d = sdf_grid(w, h, rect, r)
    A = alpha255 / 255.0
    shadow_a = np.where(d > 0, A * _cdf_arr((delta - d) / sigma), 0.0)
    shadow_a = np.clip(shadow_a, 0.0, 1.0) * (1.0 - inside)   # 内部不画（挖洞等价）
    frame = np.zeros((h, w, 4), dtype=np.float32)
    frame[..., :3] = 0.0                                    # 阴影是黑的
    frame[..., 3] = shadow_a
    # 内容（不透明）按路径裁剪
    cmask = inside[..., None]
    frame[..., :3] = content * cmask + frame[..., :3] * (1.0 - cmask)
    frame[..., 3] = np.maximum(frame[..., 3], inside)
    if BORDER:
        frame = add_border(frame, cfg)
    return over(wall, frame)


def compose_layered(content_layer, wall, cfg):
    """分层：content 一层 + overlay（阴影/边框/洞）叠上去。"""
    return over(over(wall, content_layer), cfg['overlay'])


def over(bottom, top):
    """Porter-Duff SRC_OVER（float RGBA，均为 0..1）。"""
    ta = top[..., 3:4]
    out_a = ta + bottom[..., 3:4] * (1.0 - ta)
    safe = np.where(out_a > 1e-6, out_a, 1.0)
    out_rgb = (top[..., :3] * ta + bottom[..., :3] * bottom[..., 3:4] * (1.0 - ta)) / safe
    return np.concatenate([out_rgb, out_a], axis=-1)


def add_border(frame, cfg):
    """边框：白 α=200，线宽 = max(min(canvas)*0.018, 2)。（这一档默认关）"""
    w, h = cfg['w'], cfg['h']
    sw = max(min(w, h) * 0.018, 2.0)
    ring = path_mask(cfg['box'], cfg['r'], w, h) - path_mask(
        inset_preserving_ratio(cfg['box'], sw / 2.0), max(cfg['r'] - sw / 2.0, 0.0), w, h)
    ring = np.clip(ring, 0.0, 1.0)[..., None]
    lay = np.zeros((h, w, 4), dtype=np.float32)
    lay[..., 0:3] = 1.0
    lay[..., 3:4] = ring * 200.0 / 255.0
    return over(frame, lay)


def build_cfg(w, h, area, box, r):
    # 像素对齐：路径/内容都用同一组整数边界，避免脚本自己造出 1px 缝
    boxi = (round(box[0]), round(box[1]), round(box[2]), round(box[3]))
    cfg = {'w': w, 'h': h, 'area': area, 'box': boxi, 'boxf': box, 'r': r}
    inside = path_mask(boxi, r, w, h)
    alpha255 = int(round((SHADOW_STRENGTH + 20) * ALPHA_PERCENT / 100.0))
    L = SHADOW_RANGE * (area[2] - area[0])
    sigma = max(L / (SHADOW_RHO + z_for(alpha255)), 0.35)
    delta = SHADOW_RHO * sigma
    d = sdf_grid(w, h, boxi, r)
    A = alpha255 / 255.0
    shadow_a = np.where(d > 0, A * _cdf_arr((delta - d) / sigma), 0.0)
    shadow_a = np.clip(shadow_a, 0.0, 1.0) * (1.0 - inside)
    ov = np.zeros((h, w, 4), dtype=np.float32)
    ov[..., 3] = shadow_a
    cfg['overlay'] = ov
    cfg['inside'] = inside
    cfg['sigma'] = sigma
    cfg['radius_px'] = r
    return cfg


def metrics(a, b, tag):
    """叠壁纸后的差异：RGB 通道最大差。"""
    d = np.abs(a[..., :3] - b[..., :3]) * 255.0
    dm = d.max(axis=-1)
    return {'tag': tag,
            'gt8': float((dm > 8).mean() * 100.0),
            'gt40': float((dm > 40).mean() * 100.0),
            'max': float(dm.max()),
            'corner_area': float(cfg_corner_area)}


cfg_corner_area = 0.0


def synth_frame():
    """合成一帧「像视频」的画面：多云天空的渐变 + 几团柔光 + 一层细网格 + 一点噪点。"""
    rng = np.random.default_rng(CONTENT_SEED)
    yy = np.linspace(0.0, 1.0, SRC_H)[:, None]
    xx = np.linspace(0.0, 1.0, SRC_W)[None, :]
    # 底色：上蓝下暖（夕阳感），保证四角都不是纯白也不是纯黑
    base = np.stack([
        0.62 - 0.42 * yy + 0.04 * xx,
        0.56 - 0.40 * yy + 0.02 * xx,
        0.52 - 0.34 * yy + 0.00 * xx,
    ], axis=-1)
    # 几团柔光（用高斯抹开，模拟云/灯）
    glow = np.zeros((SRC_H, SRC_W), np.float32)
    for _ in range(7):
        cx, cy = rng.uniform(0.08, 0.92), rng.uniform(0.05, 0.95)
        rad = rng.uniform(0.06, 0.22)
        amp = rng.uniform(0.25, 0.75)
        g = np.exp(-(((xx - cx) ** 2) / (2 * rad ** 2) + ((yy - cy) ** 2) / (2 * rad ** 2)))
        glow += amp * g
    glow = np.clip(glow, 0, 1.2)
    # 细网格（高频细节：方角/圆角边界上最容易看出差别）
    grid = 0.030 * (((np.floor(xx * 90) + np.floor(yy * 50)) % 2) * 2 - 1)
    # 暗角（四角偏暗 → 与浅色壁纸对比强，方角一眼可见）
    # 暗角只做一点点：压太狠会让四角跟深色壁纸撞在一起，方角就「看不见」了 ——
    # 这个演示要证明的是**几何**（四角的像素来源被换掉），不是靠调色把它显出来
    vig = 1.0 - 0.22 * ((xx - 0.5) ** 2 + (yy - 0.5) ** 2) * 2.0
    # 四个角各带一点不同色相（真实画面四角不会跟壁纸同色，这里也一样，但保持中间调）
    tint = np.stack([
        0.10 * np.exp(-((xx ** 2) / 0.02 + (yy ** 2) / 0.02)) - 0.08 * np.exp(-(((xx - 1) ** 2) / 0.02 + (yy ** 2) / 0.02)),
        0.06 * np.exp(-(((xx - 1) ** 2) / 0.02 + (yy ** 2) / 0.02)) - 0.05 * np.exp(-((xx ** 2) / 0.02 + ((yy - 1) ** 2) / 0.02)),
        0.12 * np.exp(-((xx ** 2) / 0.02 + ((yy - 1) ** 2) / 0.02)) - 0.06 * np.exp(-(((xx - 1) ** 2) / 0.02 + ((yy - 1) ** 2) / 0.02)),
    ], axis=-1)
    rgb = (base + glow[..., None] * np.array([0.30, 0.26, 0.18]) + grid[..., None] + tint) * vig[..., None]
    rgb = np.clip(rgb, 0.0, 1.0)
    rgb = np.clip(rgb + rng.normal(0, 0.012, rgb.shape), 0.0, 1.0)
    return Image.fromarray((rgb * 255).astype(np.uint8))


def font(size):
    for path in ('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',
                 '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'):
        try:
            return ImageFont.truetype(path, size)
        except Exception:
            pass
    return ImageFont.load_default()


def draw_labels(img, labels, panel, top):
    d = ImageDraw.Draw(img)
    f = font(17)
    for i, txt in enumerate(labels):
        if txt:
            d.text((i * panel + 8, top), txt, fill=(240, 240, 240), font=f)


def name_en(name):
    return {'原始比例': 'FIT (letterbox)', '铺满': 'FILL'}.get(name, name)


def main():
    global cfg_corner_area
    # 用法：python3 scripts/p0-2-overlay-probe.py [输出前缀]
    # 内容与壁纸全部由脚本自己生成，不读任何用户素材（外部图片一律不碰）。
    out_prefix = sys.argv[2] if len(sys.argv) > 2 else (sys.argv[1] if len(sys.argv) > 1
                                                       else '/tmp/p0-2-overlay-probe')

    # 内容：**程序合成**的一帧（16:9，四角都是画面不是白底）。src_path 现在只用于壁纸那张「照片档」，
    # 不传也照样跑。
    im = synth_frame()

    wallpapers = {}
    # 1 浅色（白底壁纸，阴影看得最清楚）
    yy = np.linspace(0, 1, CANVAS_H)[:, None, None]
    xx = np.linspace(0, 1, CANVAS_W)[None, :, None]
    wallpapers['light'] = np.concatenate([
        0.93 - 0.10 * yy + 0 * xx, 0.95 - 0.08 * yy + 0 * xx, 0.98 - 0.06 * yy + 0 * xx], axis=-1)
    # 2 深色
    wallpapers['dark'] = np.concatenate([
        0.06 + 0.06 * yy + 0 * xx, 0.09 + 0.05 * yy + 0 * xx, 0.13 + 0.04 * yy + 0 * xx], axis=-1)
    # 3 照片（内容本身模糊放大当壁纸，最接近真实观感）
    ph = im.resize((CANVAS_W, CANVAS_H), Image.LANCZOS).resize((CANVAS_W // 8, CANVAS_H // 8))
    ph = ph.resize((CANVAS_W, CANVAS_H), Image.BICUBIC)
    wallpapers['photo'] = np.asarray(ph, dtype=np.float32) / 255.0
    for k in list(wallpapers):
        wl = wallpapers[k]
        wl = np.concatenate([wl, np.ones((CANVAS_H, CANVAS_W, 1), dtype=np.float32)], axis=-1)
        wallpapers[k] = wl

    # ---------------------------------------------------------------- 配置 A：原始比例（日志里那一档）
    area = content_rect_fit(SRC_W, SRC_H, CANVAS_W, CANVAS_H)
    L = SHADOW_RANGE * (area[2] - area[0])
    free = max(0.0, min(area[0], CANVAS_W - area[2], area[1], CANVAS_H - area[3]))
    margin = max(0.0, L - free)
    box = inset_preserving_ratio(area, margin)
    rA = MASK_ROUND_UNIT * min(box[2] - box[0], box[3] - box[1])
    cfgA = build_cfg(CANVAS_W, CANVAS_H, area, box, rA)

    # ---------------------------------------------------------------- 配置 B：铺满（无 letterbox，DeepSeek 的满铺档）
    areaB = (0.0, 0.0, float(CANVAS_W), float(CANVAS_H))
    LB = SHADOW_RANGE * CANVAS_W
    boxB = inset_preserving_ratio(areaB, LB)
    rB = MASK_ROUND_UNIT * min(boxB[2] - boxB[0], boxB[3] - boxB[1])
    cfgB = build_cfg(CANVAS_W, CANVAS_H, areaB, boxB, rB)

    rows = []
    txt = []
    for name, cfg in (('原始比例', cfgA), ('铺满', cfgB)):
        boxw = cfg['box'][2] - cfg['box'][0]
        boxh = cfg['box'][3] - cfg['box'][1]  # 已是整数边界
        corner = 4.0 * cfg['r'] ** 2 * (1.0 - math.pi / 4.0)
        cfg_corner_area = corner
        content = np.asarray(im.resize((int(round(boxw)), int(round(boxh))), Image.LANCZOS),
                             dtype=np.float32) / 255.0
        layer_alpha = np.zeros((CANVAS_H, CANVAS_W, 4), dtype=np.float32)
        x0, y0 = int(cfg['box'][0]), int(cfg['box'][1])
        x1, y1 = int(cfg['box'][2]), int(cfg['box'][3])
        layer_alpha[y0:y1, x0:x1, :3] = content
        layer_alpha[y0:y1, x0:x1, 3] = 1.0                       # B：矩形不透明（JPEG 前提）
        layer_c = layer_alpha.copy()
        ins = np.zeros((CANVAS_H, CANVAS_W), dtype=np.float32)
        ins[y0:y1, x0:x1] = cfg['inside'][y0:y1, x0:x1]
        layer_c[..., 3] *= ins                                   # C：内容自己带 alpha
        for wp_name, wall in wallpapers.items():
            old = compose_old_for(cfg, layer_alpha, wall)
            newB = compose_layered(layer_alpha, wall, cfg)
            newC = compose_layered(layer_c, wall, cfg)
            mB = metrics(old, newB, '%s/%s/B' % (name, wp_name))
            mC = metrics(old, newC, '%s/%s/C' % (name, wp_name))
            txt.append('[%s|%s] 圆角半径=%.1fpx 四角方角面积=%.0fpx² (%.2f%%画布)  '
                       'B: >8=%.2f%% >40=%.2f%% max=%.0f   C: >8=%.2f%% max=%.0f'
                       % (name, wp_name, cfg['r'], corner,
                          corner * 100.0 / (CANVAS_W * CANVAS_H),
                          mB['gt8'], mB['gt40'], mB['max'], mC['gt8'], mC['max']))
            rows.append((name, wp_name, wall, old, newB, cfg, layer_c))

    # ---------------------------------------------------------------- 出图
    def to_img(a):
        return Image.fromarray((np.clip(a[..., :3], 0, 1) * 255).astype(np.uint8))

    panel = 300
    cols = 3
    head = 30
    rowh = 24
    grid = Image.new('RGB', (panel * cols, head + (panel + rowh) * len(rows)), (24, 24, 24))
    draw_labels(grid, ['old frame (as shipped)', 'B: opaque rect + overlay', 'diff (red = wrong)'],
                panel, 4)
    for i, (name, wp, wall, old, newB, cfg, _lc) in enumerate(rows):
        top = head + i * (panel + rowh) + rowh
        draw_labels(grid, ['%s / %s' % (name_en(name), wp), '', ''], panel, top - rowh + 2)
        for j, (imx, tag) in enumerate(((to_img(old), 'old'), (to_img(newB), 'B'))):
            grid.paste(imx.resize((panel, panel), Image.LANCZOS), (j * panel, top))
        d = np.abs(old[..., :3] - newB[..., :3]).max(axis=-1)
        heat = np.zeros((CANVAS_H, CANVAS_W, 3), dtype=np.float32)
        heat[..., 0] = np.clip(d * 3.0, 0, 1)
        heat[..., 1] = np.clip(d * 0.4, 0, 1)
        grid.paste(to_img(np.concatenate([heat, np.ones((CANVAS_H, CANVAS_W, 1), np.float32)], -1))
                   .resize((panel, panel), Image.LANCZOS), (2 * panel, top))
    grid.save(out_prefix + '.png')

    # 角部放大图（左上角 260×260 区域 ×2）
    cfg = rows[0][5]
    wall = rows[0][2]
    old = rows[0][3]
    newB = rows[0][4]
    x0 = int(cfg['box'][0]) - 4
    y0 = int(cfg['box'][1]) - 4
    z = 170
    zoom = 4
    a = to_img(old).crop((x0, y0, x0 + z, y0 + z)).resize((z * zoom, z * zoom), Image.NEAREST)
    b = to_img(newB).crop((x0, y0, x0 + z, y0 + z)).resize((z * zoom, z * zoom), Image.NEAREST)
    # 第三块：C（内容自己带 alpha）= 我方进程内合成那条路 → 应与旧帧一致
    wall0 = rows[0][2]
    lay_c = rows[0][6]
    trio_w = z * zoom
    trio = Image.new('RGB', (trio_w * 3 + 24, 40 + trio_w), (24, 24, 24))
    trio.paste(a, (0, 40))
    trio.paste(b, (trio_w + 12, 40))
    if lay_c is not None:
        cimg = to_img(compose_layered(lay_c, wall0, cfg)).crop((x0, y0, x0 + z, y0 + z))
        trio.paste(cimg.resize((trio_w, trio_w), Image.NEAREST), (trio_w * 2 + 24, 40))
    dd = ImageDraw.Draw(trio)
    dd.text((6, 12), 'old: rounded (r=%.0fpx)' % cfg['r'], fill=(235, 235, 235), font=font(18))
    dd.text((trio_w + 18, 12), 'B: opaque rect + overlay', fill=(255, 140, 120), font=font(18))
    dd.text((trio_w * 2 + 30, 12), 'C: alpha content + overlay', fill=(150, 230, 150), font=font(18))
    trio.save(out_prefix + '-corner.png')

    txt.append('')
    txt.append('板式：左=旧帧（期望外观） 中=方案B 右=差异热图(红=差>0)')
    txt.append('角部放大：%s-corner.png（左=旧帧，右=方案B）' % out_prefix)
    out = '\n'.join(txt)
    open(out_prefix + '.txt', 'w').write(out + '\n')
    print(out)


def compose_old_for(cfg, layer_alpha, wall):
    """旧帧：阴影 → 内容按路径裁剪 → （边框）→ 叠壁纸。"""
    w, h = cfg['w'], cfg['h']
    frame = cfg['overlay'].copy()
    cmask = cfg['inside'][..., None]
    frame[..., :3] = layer_alpha[..., :3] * cmask + frame[..., :3] * (1.0 - cmask)
    frame[..., 3] = np.maximum(frame[..., 3], cfg['inside'])
    if BORDER:
        frame = add_border(frame, cfg)
    return over(wall, frame)


if __name__ == '__main__':
    main()
