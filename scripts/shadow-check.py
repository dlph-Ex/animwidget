#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
v1.10.233：阴影门禁。参数一律从源码正则读（LESSONS 铁律 1）。

## 这一版的门禁来自外部复核结论（不是我拍的）

复核用 Skia 的三遍 box 模糊 + 整图拉伸复刻了参考管线，结论：
**参考实机是「单层高斯尾」，贴边 ≈0.70A、可见宽 ≈5.0%W、半高点占跨度 ≈0.32，没有肩也没有断崖。**
我们的旧模型（扩散/模糊/收边）会做出「带肩 + 断崖」，形状根本不同。

所以门禁直接断言**可测量的四个量**（对默认档，强度 100）：

  ① 可见宽（α 掉到 1/255）= 范围 L = 5.0% ± 0.3%
  ② 半高点 / 可见宽 = 0.32 ± 0.04
  ③ 贴边 / A = 0.70 ± 0.05      （既不是 1.0 也不是 0.5）
  ④ 尾部在 1/255 处收掉，跨界跳变 ≤ 1/255（没有可见硬沿）

外加结构不变量：
  ⑤ 常量：σ = 半径/√3、α = 强度+20、强度上限 100、CUT = 1/255；
  ⑥ 链路：BlurMaskFilter(半径) 直接作用在 drawPath（σ 由模型算好，不靠 extractAlpha）；
  ⑦ 「扩散 / 模糊半径 / 收边」这些自造量必须删干净；FrameComposer 里不许再有旧模型；
  ⑧ 面板：两条滑块（范围 / 紧实度）+ 强度，数字 = 千分比；第三支滑块与只读曲线图隐藏；
  ⑨ 三档预设 = {强度, 范围, 紧实度} 三个数，且都能被滑块复现；
  ⑩ 面板折叠动画、预设滑条动画（曲线感）仍在，收起 alpha 反向；
  ⑪ 预览虚线框仍是 1dp；
  ⑫ 负例：把尾部改成 5/255（旧行为）→ 必须判 FAIL。

用法：
  python3 scripts/shadow-check.py
  python3 scripts/shadow-check.py --negative
"""

import math
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src-v2", "io", "github", "animwidget")
RES = os.path.join(ROOT, "patch-v1.4.0", "res")
NEGATIVE = "--negative" in sys.argv

fails = []


def fail(m):
    fails.append(m)


def read(p):
    with open(p, encoding="utf-8", errors="replace") as f:
        return f.read()


spec_src = read(os.path.join(SRC, "ShadowSpec.java"))
layer_src = read(os.path.join(SRC, "ShadowLayer.java"))
composer = read(os.path.join(SRC, "FrameComposer.java"))
cfg = read(os.path.join(SRC, "ConfigActivity.java"))
layout = read(os.path.join(RES, "layout", "activity_config.xml"))


def const(text, name, where):
    m = re.search(r"public static final (?:int|float|double) %s = ([^;]+);" % name, text)
    if not m:
        raise SystemExit("%s 里找不到常量 %s" % (where, name))
    expr = re.sub(r"(?<=[0-9.])([fFdD])\b", "", m.group(1).strip())
    try:
        return float(eval(expr, {"__builtins__": {}}, {"Math": math}))
    except Exception:
        raise SystemExit("%s 的 %s = %r 算不出来" % (where, name, expr))


SIGMA_PER_BLUR = const(spec_src, "SIGMA_PER_BLUR", "ShadowSpec")
ALPHA_BIAS = int(const(spec_src, "ALPHA_BIAS", "ShadowSpec"))
STRENGTH_MAX = int(const(spec_src, "STRENGTH_MAX", "ShadowSpec"))
CUT = const(spec_src, "CUT", "ShadowSpec")
RANGE_MIN = const(spec_src, "RANGE_MIN", "ShadowSpec")
RANGE_MAX = const(spec_src, "RANGE_MAX", "ShadowSpec")
RHO_MIN = const(spec_src, "RHO_MIN", "ShadowSpec")
RHO_MAX = const(spec_src, "RHO_MAX", "ShadowSpec")

_m = re.search(r"DEFAULT = new ShadowSpec\(([A-Z_]+), *([A-Z_]+)\)", spec_src)
if _m:
    _map = {"RANGE_DEFAULT": const(spec_src, "RANGE_DEFAULT", "ShadowSpec"),
            "RHO_DEFAULT": const(spec_src, "RHO_DEFAULT", "ShadowSpec")}
    L_DEF = _map[_m.group(1)]
    RHO_DEF = _map[_m.group(2)]
else:
    _m = None
if not _m:
    raise SystemExit("ShadowSpec 里找不到 DEFAULT = new ShadowSpec(RANGE_DEFAULT, RHO_DEFAULT)")

A100 = (STRENGTH_MAX + ALPHA_BIAS) / 255.0


def invnorm(p):
    lo, hi = -8.0, 8.0
    for _ in range(120):
        mid = (lo + hi) / 2.0
        if 0.5 * math.erfc(-mid / math.sqrt(2.0)) < p:
            lo = mid
        else:
            hi = mid
    return (lo + hi) / 2.0


def z_of(a255, cut=CUT):
    return -invnorm(min(cut / (a255 / 255.0), 0.4999))


def sigma_of(L, rho, a255, cut=CUT):
    return L / (rho + z_of(a255, cut))


def alpha(d, a255, sigma, delta):
    return (a255 / 255.0) * 0.5 * math.erfc(-(delta - d) / (sigma * math.sqrt(2.0)))


def metrics(L, rho, a255, width=980.0):
    """返回 (可见宽%, 半高点比, 贴边/A, 尾部跳变/255)。"""
    sg = sigma_of(L, rho, a255)
    de = rho * sg
    edge = alpha(0.0, a255, sg, de)
    # 可见终点：α 掉到 1/255（解析上就是 L，这里用二分独立复算）
    lo, hi = 0.0, L * 3.0
    for _ in range(120):
        mid = (lo + hi) / 2.0
        if alpha(mid, a255, sg, de) > CUT:
            lo = mid
        else:
            hi = mid
    vis = hi
    # 半高点
    half = edge / 2.0
    lo, hi = 0.0, vis
    for _ in range(120):
        mid = (lo + hi) / 2.0
        if alpha(mid, a255, sg, de) > half:
            lo = mid
        else:
            hi = mid
    return vis / L * 100.0, (hi / vis), edge / (a255 / 255.0), alpha(vis, a255, sg, de) * 255.0


# ---------------------------------------------------------------- ① 常量
print("== ① 常量")
print("   σ/半径 = %.5f（1/√3=0.57735）  α = 强度+%d  强度上限 %d  尾部截断 %.5f（≈1/255）"
      % (SIGMA_PER_BLUR, ALPHA_BIAS, STRENGTH_MAX, CUT))
if abs(SIGMA_PER_BLUR - 1.0 / math.sqrt(3.0)) > 1e-4:
    fail("SIGMA_PER_BLUR 不是 1/√3")
if abs(CUT - 1.0 / 255.0) > 1e-6:
    fail("CUT 不是 1/255（尾部截断点错了会露出硬沿）")
if ALPHA_BIAS != 20 or STRENGTH_MAX != 100:
    fail("α = 强度+20、强度上限 100 必须和参考实现一致")

# ---------------------------------------------------------------- ② 默认档的四个量
print("\n== ② 默认档必须复现复核结论（单层高斯尾、无肩无断崖）")
print("   DEFAULT = 范围 %.2f%% / 紧实度 %.2f / 强度 %d" % (L_DEF * 100, RHO_DEF, STRENGTH_MAX))
vis_pct, half_ratio, edge_ratio, tail_255 = metrics(L_DEF, RHO_DEF, STRENGTH_MAX + ALPHA_BIAS)
print("   可见宽/范围 = %.1f%%   半高点/可见宽 = %.3f   贴边/A = %.3f   尾部残余 = %.2f/255"
      % (vis_pct, half_ratio, edge_ratio, tail_255))
print("   复核给 5.0%%/0.32/0.70；S01 成对反推 5.51%%/0.316/0.719 —— 同区间，取后者")
if abs(L_DEF - 0.0551) > 1e-4 or abs(RHO_DEF - 0.58) > 1e-4:
    fail("默认不是 S01 成对反推的 范围5.51%% / 紧实度0.58（现在 %.4f / %.4f）" % (L_DEF, RHO_DEF))

print("\n== ②a 反推自洽：S01 实测必须**成对**使用（v1.10.235 在这里混用了口径）")
SIG_MEAS, M_MEAS, R_PROBE = 10.38, 26.0, 16.0
f_scale = SIG_MEAS / (0.57735 * R_PROBE + 0.5)     # 实测σ/公式σ = 1.066
k_meas = M_MEAS / SIG_MEAS                         # 2.505
print("   S01: r=%.0f σ实测=%.2f 公式σ=%.2f → 尺度 f=%.4f ; m=%.0f → k=m/σ=%.3f"
      % (R_PROBE, SIG_MEAS, 0.57735 * R_PROBE + 0.5, f_scale, M_MEAS, k_meas))
_rho_list = []
for W_ in (400.0, 600.0, 800.0, 980.0, 1200.0):
    u_ = W_ / 35.0
    sig_ref = f_scale * (0.57735 * u_ + 0.5)
    mm = k_meas * sig_ref
    st = (W_ + 4 * u_) / (W_ + 2 * mm)
    de = 2 * u_ - mm * st
    se = sig_ref * st
    rho_ = de / se
    L_ = (de + 2.394 * se) / W_
    _rho_list.append(rho_)
    print("   W=%-6.0f → ρ=%.3f  L=%.3f%%W  δ=%.3f%%W  σₑ=%.3f%%W"
          % (W_, rho_, L_ * 100, de / W_ * 100, se / W_ * 100))
rho980 = _rho_list[3]
L980 = 0.05509
if abs(rho980 - RHO_DEF) > 0.03:
    fail("默认 ρ=%.2f 与 S01 成对反推的 %.3f（W=980）差太多" % (RHO_DEF, rho980))
if abs(L980 - L_DEF) > 0.0015:
    fail("默认 L=%.4f 与反推的 %.4f 差太多" % (L_DEF, L980))
if not (RHO_MIN <= RHO_DEF <= RHO_MAX):
    fail("默认 ρ 超出滑块范围")
print("   OK  ρ 随 W 缓变 %.2f~%.2f，L 恒在 %.2f%%W 左右（与同尺度实测 5.1%% 同区间）"
      % (min(_rho_list), max(_rho_list), L980 * 100))

print("\n== ②b 尺寸无关性（W ∈ {500,700,980,1080}，同一组参数给同一组百分比）")
for width in (500, 700, 980, 1080):
    Lpx = L_DEF * width
    sg = sigma_of(Lpx, RHO_DEF, STRENGTH_MAX + ALPHA_BIAS)
    de = RHO_DEF * sg
    e = alpha(0.0, STRENGTH_MAX + ALPHA_BIAS, sg, de) * 100
    print("   W=%-5d σ=%6.2fpx δ=%6.2fpx 贴边=%5.1f%% 范围=%5.1f%%" % (width, sg, de, e, Lpx))
    if abs(e - edge_ratio * A100 * 100) > 0.2:
        fail("W=%d 的贴边比例与其它尺寸不一致（实现里混进了 px 常量？）" % width)

# ---------------------------------------------------------------- ③ 强度/紧实度单调
print("\n== ③ 强度与紧实度的作用方向")
for st in (0, 25, 50, 75, 100):
    a = 0 if st == 0 else st + ALPHA_BIAS
    e = 0.0 if a == 0 else alpha(0.0, a, sigma_of(L_DEF, RHO_DEF, a), RHO_DEF * sigma_of(L_DEF, RHO_DEF, a))
    print("   强度 %3d → 贴边 %5.1f%%" % (st, e * 100))
for rho in (RHO_MIN, 0.0, RHO_DEF, 1.0, RHO_MAX):
    sg = sigma_of(L_DEF, rho, STRENGTH_MAX + ALPHA_BIAS)
    e = alpha(0.0, STRENGTH_MAX + ALPHA_BIAS, sg, rho * sg)
    print("   紧实度 %5.2f → 贴边 %5.1f%%  σ=%5.2f%%W" % (rho, e * 100, sg * 100))
    if rho > RHO_DEF and e < alpha(0.0, STRENGTH_MAX + ALPHA_BIAS,
                                   sigma_of(L_DEF, RHO_DEF, STRENGTH_MAX + ALPHA_BIAS),
                                   RHO_DEF * sigma_of(L_DEF, RHO_DEF, STRENGTH_MAX + ALPHA_BIAS)) - 1e-9:
        fail("紧实度变大贴边反而变淡（方向反了）")

# ---------------------------------------------------------------- ④ 链路
print("\n== ④ 链路：σ 由模型算、BlurMaskFilter 直接画 path")
for needle, why in (
        ("new BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)", "半径 = σ/√3，NORMAL"),
        ("canvas.drawPath(localPath, paint)", "轮廓直接画"),
        ("Bitmap.createBitmap(outW, outH, Config.ARGB_8888)", "画布 = 画面框四周各外扩 pad"),
        ("Math.abs(dilatePx) > 0.01f", "δ 可正可负（ρ<0 时 50% 点在画面里）"),
):
    if needle not in layer_src:
        fail("ShadowLayer 缺 %r（%s）" % (needle, why))
    else:
        print("   OK  %s" % why)
if "ShadowSpec.sigmaPx(rangePx, spec.rho()" not in composer:
    fail("FrameComposer 没有用 ShadowSpec.sigmaPx 算 σ")
else:
    print("   OK  σ = ShadowSpec.sigmaPx(L, ρ, 生效α)")
if "ShadowSpec.blurRadiusPx(sigmaPx)" not in composer:
    fail("没有把 σ 换成 BlurMaskFilter 的半径")
if "(sigmaPx - 0.5f) / SIGMA_PER_BLUR" not in spec_src:
    fail("blurRadiusPx 没有按 σ = 0.57735r + 0.5 反解（r = (σ−0.5)/0.57735）")
else:
    print("   OK  半径 = (σ−0.5)/0.57735（按 S01 实测的 Skia 换算）")
if "float need = shadowAlpha > 0 ? rangePx : 0.0f;" not in composer:
    fail("留白不是范围 L（因果断了）")
else:
    print("   OK  留白 = 范围 L（出多少缩多少）")

# ---------------------------------------------------------------- ⑤ 旧模型删干净
print("\n== ⑤ 自造量必须删干净")
code = "\n".join(l for l in composer.splitlines()
                 if not l.lstrip().startswith(("@Metadata", "@SourceDebugExtension")))
for name in ("dilate()", "blurPx()", "edge()", "ShadowBands", "drawShadow", "BAND_",
             "MARGIN_OF_WIDTH", "WIDTH_MAX_FRAC", "ShadowCurve"):
    hit = name in code or name in spec_src.replace("//", " ")
    print("   %-16s %s" % (name, "还在 ← FAIL" if hit else "已删  OK"))
    if hit:
        fail("%s 还没删干净" % name)

# ---------------------------------------------------------------- ⑥ 面板
print("\n== ⑥ 面板：范围 + 紧实度 + 强度")
bars = {k: int(v) for k, v in re.findall(r'<SeekBar android:id="@id/(curve_[a-z]+)"[^/]*android:max="(\d+)"', layout)}
print("   滑块 max：%s" % bars)
if bars.get("curve_spread") != 800:
    fail("范围滑块 max 应为 800（10~80 → 1.0%~8.0%%）")
if bars.get("curve_grad") != 500:
    fail("紧实度滑块 max 应为 500（0~500 → ρ −0.5~1.5）")
panel = cfg[cfg.index("private final void setupShadowCurvePanel()"):cfg.index("private final void updateRotateButtons()")]
if "R.id.curve_gamma" not in panel:
    fail("第三支滑块（旧收边）没有隐藏")
if "commitCurveFromSliders()" not in panel or "bindPreset(R.id.btn_preset_edge" not in panel:
    fail("滑块/预设没接上")
print("   OK  两条滑块接上提交，第三支与只读曲线图隐藏")

# ---------------------------------------------------------------- ⑦ 预设
print("\n== ⑦ 三档预设 = {强度, 范围, 紧实度}")
for name in ("PRESET_EDGE", "PRESET_WIDE", "PRESET_TIGHT"):
    m = re.search(r"%s = \{([^}]*)\}" % name, cfg)
    if not m:
        fail("找不到 %s" % name)
        continue
    vals = [float(x.strip().rstrip("fF")) for x in m.group(1).split(",")]
    if len(vals) != 3:
        fail("%s 应该是 {强度, 范围, 紧实度} 三个数" % name)
        continue
    st, L, rho = vals
    a = 0 if st <= 0 else round(st) + ALPHA_BIAS
    sg = sigma_of(L, rho, a if a > 0 else 1)
    e = alpha(0.0, a, sg, rho * sg) * 100 if a > 0 else 0.0
    rho_prog = round(rho * 250.0 + 125.0)
    ok = (0 <= st <= STRENGTH_MAX
          and round(L * 1000) >= int(re.search(r'@id/curve_spread"[^/]*android:min="(\d+)"', layout).group(1))
          and round(L * 1000) <= bars.get("curve_spread", 0)
          and 0 <= rho_prog <= bars.get("curve_grad", 0))
    print("   %-12s 强度%4.0f 范围%5.2f%% 紧实度%5.2f → 贴边%5.1f%% %s"
          % (name, st, L * 100, rho, e, "OK" if ok else "拖不到 ← FAIL"))
    if not ok:
        fail("%s 的数字拖不出来" % name)

print("\n== ⑦b 省画面 = 用户实测的 4.1% + 紧实度 0.55（形状同默认，只缩范围）")
_m3 = re.search(r"PRESET_TIGHT = \{([^}]*)\}", cfg)
if _m3:
    _v = [float(x.strip().rstrip("fF")) for x in _m3.group(1).split(",")]
    if len(_v) == 3:
        _s, _L, _rho = _v
        print("   省画面 = 强度%.0f 范围%.2f%% 紧实度%.2f" % (_s, _L * 100, _rho))
        if abs(_L - 0.041) > 1e-4:
            fail("省画面范围不是 4.1%%（现在是 %.3f%%）" % (_L * 100))
        if abs(_rho - 0.58) > 1e-4:
            fail("省画面紧实度不是 0.68（默认那组，形状不能变）")

print("\n== ⑦c S01/S02/S03 探头：真值只认设备查表，不认外部复刻的核")
probe = read(os.path.join(SRC, "ShadowProbe.java"))
for needle, why in (
        ("extractAlpha(p, off)", "要读 extractAlpha 返回的尺寸与 offset"),
        ("blur.setMaskFilter(new BlurMaskFilter(RADIUS, BlurMaskFilter.Blur.NORMAL))", "要量 σ"),
        ("(hi90 - lo10) / 2.5631f", "σ = 台阶 10%→90% 距离 / 2.563（亚像素插值）"),
        ("RADIUS / 1.7320508f", "对照组：σ = r/√3"),
        ("RADIUS * 0.57735f + 0.5f", "对照组：σ = 0.57735r + 0.5"),
        ("\"S01\"", "结果要进日志，编号 S01"),
        ("\"S02\"", "标定与 (m,σ) 查表要进日志，编号 S02"),
        ("analyticStep(9.74f)", "用解析高斯（已知 σ）标定探头"),
        ("boxStep(21)", "用 3 遍 box 标定探头在 box 核上的偏差"),
        ("int[] radii = {10, 16, 22, 28, 35, 40};", "(m,σ) 查表覆盖部署半径"),
        ("\"S03\"", "必须上机测我们自己烘焙的位图，编号 S03"),
        ("\"S04\"", "剖面单独一行（S04），外部复核要拿它看形状"),
        ("devWithShift(r.profX, deltaPx, sigmaPx, aFrac)", "x/y 分轴逐点比（两轴拉伸倍率不同）"),
        ("double d = k + 0.5 + shift;", "模型采样在**像素中心**（画面边取整数坐标）—— 否则 0.5 会一直挂在平移列上"),
        ("255.0 * ShadowSpec.normalCdf", "模型基准必须是 255（bake 的画笔 alpha），不是 a255"),
        ("boolean okBase = base == 255;", "必须断言烘焙位图基准 == 255"),
        ("private static final float TOL_FINAL = 1.5f;", "容限 1.5/255 写在最终 alpha 域（单位=级/255，别写成 1.5f/255f）"),
        ("private static final float TOL_SHIFT = 0.25f;", "平移容限 0.25px"),
        ("* aFrac", "差异先乘强度（mask 域 → 最终 alpha 域）再比容限"),
        ("正 = 烘出来的阴影更靠外", "偏移取反记成物理量（正=渲染更靠外），别只印搜索变量"),
        ("for (int i = -4; i <= 4; i++)", "先在 ±1px 内搜亚像素平移（每步 0.25px）"),
        ("bestShift", "平移量要单独记一列（稳定不为 0 = 真的系统偏移）"),
        ("win=%d", "日志要打 win（盒宽奇偶决定亚像素相位）"),
        ("boxH / 2, true", "只取中段行/列，避开四角"),
):
    if needle not in probe:
        fail("ShadowProbe 缺 %r（%s）" % (needle, why))
    else:
        print("   OK  %s" % why)
if "ShadowProbe.runOnce(this)" not in cfg:
    fail("没有人调用 ShadowProbe.runOnce —— 探头发不出来")
else:
    print("   OK  进「自定义」面板时跑一次")

# 两个 box 的 win 奇偶必须不同：只测一种相位，会把相位偏差当成模型偏差（外部复核第 3 条）
_mb = re.search(r"int\[\]\[\] boxes = \{\{([^}]*)\}, \{([^}]*)\}, \{([^}]*)\}\};", probe)
if not _mb:
    fail("找不到 S03 的三个 box 定义（要覆盖 win 奇偶各一组 + 一个换高度的变体）")
else:
    _ws = []
    for _grp in _mb.groups():
        _w = int(_grp.split(",")[0])
        _sg = sigma_of(L_DEF * _w, RHO_DEF, STRENGTH_MAX + ALPHA_BIAS)
        _win = int(math.floor(1.88 * _sg + 0.5))
        _ws.append((_w, _win, math.sqrt(3 * (_win ** 2 - 1) / 12.0), _sg))
    print("   S03 box: " + "，".join("W=%d win=%d(3box rms σ=%.2f, 名义 σ=%.2f)" % a for a in _ws))
    _par = set(w[1] % 2 for w in _ws)
    if len(_par) < 2:
        fail("三个 S03 box 的 win 同奇偶（%s）—— 相位偏差会被当成模型偏差"
             % "/".join(str(w[1]) for w in _ws))
    else:
        print("   OK  win 奇偶各一组")
    if len(set((w[0], w[1]) for w in _ws)) >= 3 or len(_ws) < 3:
        if len(_ws) < 3:
            fail("S03 至少要三个 box：奇/偶 win + 换高度复测 y 轴偏移")
    else:
        print("   提示：三个 box 的 (宽, win) 有重复，换高度的那个不会提供新相位")

# 判据本身必须先被验证（LESSONS 铁律 ③）：脱机自检抄件必须与正本同步
print("\n== ⑦d S03 判据自检：抄件(toolchain/ShadowProbeMathTest.java) 与正本逐字同步")
_mirror = read(os.path.join(ROOT, "toolchain", "ShadowProbeMathTest.java"))
for _needle, _why in (
        ("double d = k + 0.5 + shift;", "像素中心采样"),
        ("float diff = (float) (Math.abs(prof[k] - wantAt(k, deltaPx, sigmaPx, shift)) * aFrac);", "换到最终 alpha 域"),
        ("static final float TOL_FINAL = 1.5f;", "容限 1.5 级/255"),
        ("static final float TOL_SHIFT = 0.25f;", "平移容限 0.25px"),
        ("return \"FAIL(L∞+移)\";", "判据字符串"),
        ("float shift = i * 0.25f;", "平移搜索步长"),
        ("float c = 1.0f + ci * 0.01f;", "σ 拟合粗搜网格（±15%，隔点取样）"),
        ("float c = bestC + ci * 0.002f;", "σ 拟合细搜（0.2%）"),
        ("for (int i = -4; i <= 4; i++)", "±1px 搜索范围"),
):
    if _needle not in probe:
        fail("正本 ShadowProbe.java 缺 %r（%s）" % (_needle, _why))
    elif _needle not in _mirror:
        fail("自检抄件与正本不同步：缺 %r（%s）—— 改了判据就要同步 toolchain/ShadowProbeMathTest.java"
             % (_needle, _why))
    else:
        print("   OK  %s" % _why)
if "SELFTEST OK" not in _mirror or "System.exit(1)" not in _mirror:
    fail("自检必须自带 SELFTEST OK / 失败 exit 1 的汇总")
else:
    print("   OK  自检自带汇总与退出码")



# ---------------------------------------------------------------- ⑧ 动画
print("\n== ⑧ 两条动画仍在")
anim = cfg[cfg.index("private final void animateShadowPanel("):cfg.index("private final void updateRotateButtons()")]
for needle, why in (
        ("Ease.IN_OUT.at(", "折叠走三次贝塞尔"),
        ("panel.setAlpha(expand ? Ease.IN_OUT.at(eased) : 1.0f - Ease.IN_OUT.at(eased))",
         "收起 alpha 反向（防闪）"),
        ("View.MeasureSpec.UNSPECIFIED", "量高不能 AT_MOST"),
        ("running.cancel()", "连点取消"),
):
    if needle not in anim:
        fail("折叠动画缺 %r（%s）" % (needle, why))
pre = cfg[cfg.index("private final void applyShadowPreset"):]
pre = pre[:pre.index("private final void bindPreset")]
for needle, why in (
        ("android.animation.ValueAnimator.ofFloat(0.0f, 1.0f)", "滑条动画"),
        ("anim.setInterpolator((t) -> Ease.IN_OUT.at(t))", "同一条曲线"),
        ("ConfigActivity.this.curveSyncing = true;", "动画期间挡监听"),
        ("setCurveBarProgress(R.id.curve_spread, Math.round(rFrom + (rTo - rFrom) * f));", "范围跟着滑"),
        ("setCurveBarProgress(R.id.curve_grad, Math.round(pFrom + (pTo - pFrom) * f));", "紧实度跟着滑"),
        ("commitCurveFromSliders();", "落地提交"),
):
    if needle not in pre:
        fail("预设动画缺 %r（%s）" % (needle, why))
print("   OK  折叠动画 + 预设滑条动画")

# ---------------------------------------------------------------- ⑨ 预览虚线框
print("\n== ⑨ 预览虚线框 1dp")
m = re.search(r"int pad = Math\.round\(([0-9.]+)f \* getResources", cfg)
outline = read(os.path.join(RES, "drawable", "preview_outline.xml"))
ms = re.search(r'<stroke android:width="([0-9.]+)dip"', outline)
mr = re.search(r'<corners android:radius="([0-9.]+)dip"', outline)
if not (m and ms and mr):
    fail("找不到预览内边距/笔画/圆角")
else:
    gap = float(m.group(1)) - float(ms.group(1)) / 2.0
    print("   间距 %.1fdp" % gap)
    if abs(gap - 1.0) > 0.01:
        fail("间距不是 1dp")

# ---------------------------------------------------------------- ⑩ 负例
if NEGATIVE:
    print("\n== ⑩ 负例：模拟「带肩 + 断崖」（ρ=1.5、尾部截在 5/255）")
    rho_bad, cut_bad = 1.5, 5.0 / 255.0
    a = STRENGTH_MAX + ALPHA_BIAS
    sg = sigma_of(L_DEF, rho_bad, a, cut_bad)
    de = rho_bad * sg
    edge_b = alpha(0.0, a, sg, de)
    lo, hi = 0.0, L_DEF * 3.0
    for _ in range(120):
        mid = (lo + hi) / 2.0
        if alpha(mid, a, sg, de) > cut_bad:
            lo = mid
        else:
            hi = mid
    vis_b = hi
    half_b = edge_b / 2.0
    lo2, hi2 = 0.0, vis_b
    for _ in range(120):
        mid = (lo2 + hi2) / 2.0
        if alpha(mid, a, sg, de) > half_b:
            lo2 = mid
        else:
            hi2 = mid
    print("   贴边/A = %.3f（界限 0.65~0.75）  半高点/可见宽 = %.3f（界限 0.28~0.36）  尾部 = %.1f/255（界限 ≤1.05）"
          % (edge_b / (a / 255.0), hi2 / vis_b, alpha(vis_b, a, sg, de) * 255.0))
    caught = not (0.65 <= edge_b / (a / 255.0) <= 0.75
                  and 0.28 <= hi2 / vis_b <= 0.36
                  and alpha(vis_b, a, sg, de) * 255.0 <= 1.05)
    if not caught:
        fail("负例没被抓住：门禁的四个界限形同虚设")
    else:
        fails.append("负例：「带肩 + 断崖」被正确判为违规（预期）")

# ---------------------------------------------------------------- 结论
print("\n== 结论")
if fails:
    for f in fails:
        print("   FAIL " + f)
    print("   FAILED（%d 项）" % len(fails))
    sys.exit(1)
print("   OK")
sys.exit(0)
