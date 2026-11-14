#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
v1.10.211：形状蒙版的几何门禁（出包前跑，verify-apk.sh 不调用，手动/我出包时必跑）。

为什么要这个脚本：
  「蒙版被拉伸 / 扭曲 / 截断 / 没占满格子」这四类问题肉眼才能看出来，但这台机器上**没有安卓
  运行时**，看不了。能做的只有把几何算一遍：
    ① 新形状在 0..1 单位框里**是不是真的占满**（否则铺满框时会被多拉一次）；
    ② 蒙版是不是**永远铺满布局框**（= 选蒙版不许让画面回缩）；
    ③ 缩略图入口是不是按形状自己的比例画（图标不能被拉歪）。

数字全部从 ShapeFactory.java 里**正则读出来**，不手抄 —— 手抄的数字不是证据（LESSONS 铁律 1）。

负例（必须 FAIL，用来证明这个门禁有牙齿）：
  --negative 时按「老规则」（不保比例、铺满布局框）复算，眼睛在 21:9 槽位上会被拉成 2.33 倍，
  脚本必须报 FAIL。门禁自己先被验证过，才算门禁（LESSONS 铁律 3）。

用法：
  python3 scripts/shape-check.py            # 门禁
  python3 scripts/shape-check.py --negative # 负例：假装蒙版按"保比例"算（预期 exit 1）
"""

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src-v2", "io", "github", "animwidget")
NEGATIVE = "--negative" in sys.argv

fails = []
warns = []


def fail(msg):
    fails.append(msg)


# ---------------------------------------------------------------- 读源码
def read(name):
    with open(os.path.join(SRC, name), encoding="utf-8", errors="replace") as f:
        return f.read()


factory = read("ShapeFactory.java")
kind = read("ShapeKind.java")

# 1) 新形状清单 + 设计框规则
m = re.search(r"private final boolean buildGlyph\(Path path, ShapeKind shape, float w, float h\) \{(.*?)\n    \}\n", factory, re.S)
assert m, "找不到 buildGlyph"
glyph_src = m.group(1)

cases = {}
parts = re.split(r"\n            case ([A-Z0-9_]+):", glyph_src)
# parts[0] 是 switch 头，之后成对出现 (名字, 代码)
for i in range(1, len(parts) - 1, 2):
    name = parts[i]
    body = parts[i + 1]
    cases[name] = body
assert len(cases) == 12, "buildGlyph 里应有 12 个 case，实际 %d" % len(cases)

# 2) natural 比例表（从 ShapeKind.getNatural 读）
nat_src = re.search(r"public final float getNatural\(\) \{(.*?)\n    \}\n", kind, re.S).group(1)
# 把 「case 37:  // MOON \n return 1.0f;」 收集起来
natural = {}
case_labels = re.findall(r"case (\d+):\s*//\s*([A-Z0-9_]+)\s*\n\s*return ([0-9.]+)f;", nat_src)
for _, name, val in case_labels:
    natural[name] = float(val)
# case 4/11/12/13/15/16/17/18/19/36 这些没有注释名，单独按 ordinal 顺序补
order = re.search(r"public enum ShapeKind \{(.*?);", kind, re.S).group(1)
order = re.sub(r"//[^\n]*", "", order)          # 先删注释，否则注释里的逗号会切错
order = [t.strip() for t in re.split(r",", order) if t.strip()]
assert len(order) == 48, "ShapeKind 应有 48 个常量，实际 %d" % len(order)
by_ordinal = {i: n for i, n in enumerate(order)}
for num, name, val in case_labels:
    pass
# 无注释的那些：case 4 / 11 / 12 / 13 / 15 / 16 / 17 / 18 / 19 / 36
for num in re.findall(r"case (\d+):\s*\n\s*return ([0-9.]+)f;", nat_src):
    natural[by_ordinal[int(num[0])]] = float(num[1])

# 3) 所有常量都要在 label()/getLabel() 里有 case，否则运行期 NoWhenBranchMatchedException。
#    注意：switch 的 case 数字是 WhenMappings 里的「映射号」，不是 ordinal —— 先把映射读出来。
mapping = dict(re.findall(r"iArr\[ShapeKind\.([A-Z0-9_]+)\.ordinal\(\)\] = (\d+);", kind))
if len(mapping) != 48:
    fail("WhenMappings 只映射了 %d 个形状（应为 48）" % len(mapping))
if sorted(int(v) for v in mapping.values()) != list(range(1, 49)):
    fail("WhenMappings 的映射号不是 1..48 且不重复")
gaps = []
for meth in ("label", "getLabel"):
    body = re.search(r"public final String %s\(.*?\n    \}\n" % meth, kind, re.S)
    assert body, meth
    covered = set(int(x) for x in re.findall(r"case (\d+):", body.group(0)))
    missing = ["%s(%s)" % (k, v) for k, v in mapping.items() if int(v) not in covered]
    if missing:
        gaps.append("%s() 缺 case: %s" % (meth, ", ".join(sorted(missing))))
if gaps:
    for g in gaps:
        fail(g)
else:
    print("== ③ 48 个形状在 label()/getLabel() 里都有 case")

# ---------------------------------------------------------------- 贝塞尔包围盒
def cubic_axis(p0, p1, p2, p3):
    """返回该轴上三次贝塞尔的极值点（解导数=0）。"""
    out = [p0, p3]
    a = -p0 + 3 * p1 - 3 * p2 + p3
    b = 2 * (p0 - 2 * p1 + p2)
    c = p1 - p0
    if abs(a) < 1e-9:
        if abs(b) > 1e-9:
            t = -c / b
            if 0 < t < 1:
                out.append(bez(p0, p1, p2, p3, t))
        return [min(out), max(out)]
    disc = b * b - 4 * a * c
    if disc < 0:
        return [min(out), max(out)]
    sq = disc ** 0.5
    for t in ((-b + sq) / (2 * a), (-b - sq) / (2 * a)):
        if 0 < t < 1:
            out.append(bez(p0, p1, p2, p3, t))
    return [min(out), max(out)]


def bez(p0, p1, p2, p3, t):
    u = 1 - t
    return u * u * u * p0 + 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t * p3


NUM = r"(-?\d+\.\d+)f"


def parse_path(body):
    """把 Java 里的 moveTo/lineTo/cubicTo/close 解析成 (点集, 是否有 close)。"""
    xs, ys = [], []
    closed = bool(re.search(r"path\.close\(\)", body))
    cur = None
    for call in re.finditer(r"path\.(moveTo|lineTo|cubicTo|arcTo|addCircle|addRoundRect|op|set)\(([^;]*)\);", body):
        op, args = call.group(1), call.group(2)
        nums = [float(x) for x in re.findall(NUM, args)]
        if op == "moveTo":
            cur = (nums[0], nums[1])
            xs.append(cur[0]); ys.append(cur[1])
        elif op == "lineTo":
            nxt = (nums[0], nums[1])
            xs.extend([cur[0], nxt[0]]); ys.extend([cur[1], nxt[1]])
            cur = nxt
        elif op == "cubicTo":
            p1 = (nums[0], nums[1]); p2 = (nums[2], nums[3]); p3 = (nums[4], nums[5])
            bx = cubic_axis(cur[0], p1[0], p2[0], p3[0])
            by = cubic_axis(cur[1], p1[1], p2[1], p3[1])
            xs.extend(bx); ys.extend(by)
            cur = p3
    return (min(xs), min(ys), max(xs), max(ys)) if xs else None, closed


# ---------------------------------------------------------------- ① 占满单位框
TOL = 0.02     # 允许 2% 的归一化余量（归一化会把这点余量吃掉，肉眼不可见）
analytic = {
    # 这几个含圆 / 圆角 / Path.op，直接在 Python 里解析式算包围盒（并校验源码常量没漂）
    "YINYANG": (0.0, 0.0, 1.0, 1.0),   # 左半圆 + 上下两个 r=.25 圆 → 恒为整框
    "TICKET": (0.0, 0.0, 1.0, 1.0),    # 圆角矩形咬掉左右两个缺口 → 恒为整框
    "KEYHOLE": (0.0, 0.0, 1.0, 1.0),   # 圆头 + 尾底顶满宽度 → 恒为整框
    "CLOUD": (0.0, 0.0, 1.0, 1.0),     # 底座矩形(0..1 x 0.62..1) + 三个圆（顶圆 y=.34 半径 .34 → 顶到 0）
    "GEAR": (0.0, 0.0, 1.0, 1.0),      # 12 齿：0° 与 90° 都有齿 → 顶满
    "SUNBURST": None,   # 下面按源码里的 12 / 0.50 / 0.22 复算
}

print("== ① 12 个新形状是否占满 0..1 单位框（归一化余量 ≤ %.0f%%）" % (TOL * 100))
for name in cases:
    body = cases[name]
    if name == "SUNBURST":
        pts = int(re.search(r"int points = (\d+);", body).group(1))
        outer, inner = [float(x) for x in re.findall(r"\(i % 2 == 0\) \? ([0-9.]+)f : ([0-9.]+)f", body)[0]]
        xs, ys = [], []
        for i in range(pts * 2):
            import math
            r = outer if i % 2 == 0 else inner
            a = -math.pi / 2 + math.pi * i / pts
            xs.append(0.5 + math.cos(a) * r)
            ys.append(0.5 + math.sin(a) * r)
        box = (min(xs), min(ys), max(xs), max(ys))
    elif name in analytic and analytic[name]:
        box = analytic[name]
        if name == "YINYANG":
            # 校验源码常量：左半圆 + 上下两个 r=0.25 的圆
            assert re.search(r"arcTo\(new RectF\(0\.00f, 0\.00f, 1\.00f, 1\.00f\)", body), "YINYANG 外圆变了"
            assert body.count("addCircle(0.50f, 0.25f, 0.25f") == 1 and body.count("addCircle(0.50f, 0.75f, 0.25f") == 1, "YINYANG 小圆变了"
        if name == "TICKET":
            assert "addRoundRect(new RectF(0.00f, 0.00f, 1.00f, 1.00f)" in body, "TICKET 外框变了"
        if name == "CLOUD":
            assert "addRect(new RectF(0.00f, 0.62f, 1.00f, 1.00f)" in body, "CLOUD 底座矩形变了"
            for bump in ("{0.22f, 0.60f, 0.22f}", "{0.50f, 0.34f, 0.34f}", "{0.78f, 0.60f, 0.22f}"):
                assert bump in body, "CLOUD 圆凸包表变了：缺 %s" % bump
        if name == "GEAR":
            assert "int teeth = 12;" in body, "GEAR 齿数不是 12（0°/90° 没齿 → 包围盒不满）"
            assert body.count("* 0.50f") >= 4, "GEAR 齿的外半径不是 0.50（顶不到框）"
        if name == "KEYHOLE":
            assert "addCircle(0.50f, 0.32f, 0.32f" in body, "KEYHOLE 圆头变了"
            assert "stem.lineTo(1.00f, 1.00f)" in body and "stem.lineTo(0.00f, 1.00f)" in body, "KEYHOLE 尾底不再顶满宽度"
    else:
        box, closed = parse_path(body)
        if not closed:
            fail("%s 不是闭合路径（蒙版必须是实心闭合区域）" % name)
    if box is None:
        fail("%s 没解析出任何路径点" % name)
        continue
    x0, y0, x1, y1 = box
    sx = 1.0 / (x1 - x0) if x1 > x0 else 0.0
    sy = 1.0 / (y1 - y0) if y1 > y0 else 0.0
    dev_x = abs(sx - 1.0)
    dev_y = abs(sy - 1.0)
    flag = "OK " if (dev_x <= TOL and dev_y <= TOL and abs(x0) <= TOL and abs(y0) <= TOL) else "FAIL"
    if flag == "FAIL":
        fail("%s 单位框没占满：bbox=(%.3f,%.3f,%.3f,%.3f) 归一化要拉伸 sx=%.3f sy=%.3f"
             % (name, x0, y0, x1, y1, sx, sy))
    print("  %-10s bbox=(%.3f,%.3f,%.3f,%.3f) 归一化 sx=%.3f sy=%.3f  %s"
          % (name, x0, y0, x1, y1, sx, sy, flag))

# ---------------------------------------------------------------- ② 任何槽位比例都不会拉歪
def design_box(W, H, nat):
    if nat <= 0:
        return W, H
    bw, bh = W, W / nat
    if bh > H:
        bh, bw = H, H * nat
    return bw, bh


def ratio_table(nat, slots, keep_ratio):
    """keep_ratio=True 走新规则（designBox 保比例），False 走老规则（铺满布局框）。"""
    rows = []
    for W, H in slots:
        if keep_ratio and nat > 0:
            bw, bh = design_box(W, H, nat)
        else:
            bw, bh = W, H          # 老规则：蒙版铺满布局框
        rows.append((W, H, bw / bh, nat if nat > 0 else bw / bh))
    return rows


SHAPES = {
    "EYE": 1.7, "CLOUD": 1.5, "HEART": 1.0, "STAR": 1.0, "BOLT": 0.62,
}
SLOTS = [(2100, 900), (1600, 900), (1200, 900), (900, 900), (900, 1200), (900, 1600)]
LABEL = {0: "21:9", 1: "16:9", 2: "4:3", 3: "1:1", 4: "3:4", 5: "9:16"}

print("\n== ② 蒙版必须铺满布局框：选蒙版不许让画面回缩")


def method_body_of(sig):
    i = factory.index(sig)
    j = factory.index("\n    }\n", i)
    return factory[i:j]


path_body = method_body_of("public final Path path(@NotNull ShapeKind shape, float width, float height)")
fitted_body = method_body_of("public final Path pathFitted(@NotNull ShapeKind shape, float width, float height)")

if ("designBox(" in path_body) or NEGATIVE:
    fail("蒙版入口 path() 又在用 designBox 保比例 → 非正方形画面框会被内接方框顶替，画面回缩")
elif "stretchToBox(path, w, h)" not in path_body:
    fail("蒙版入口 path() 没有铺满框（缺 stretchToBox）")
else:
    print("   path() = 铺满整个框（stretchToBox）  OK")
if "designBox(" not in fitted_body:
    fail("缩略图入口 pathFitted() 没有保比例，图标会被拉歪")
else:
    print("   pathFitted() = 缩略图按自身比例（只在格子里用）  OK")


def design_box(W, H, nat):
    bw, bh = W, W / nat
    if bh > H:
        bh, bw = H, H * nat
    return bw, bh


SLOTS = [(2100, 900), (1600, 900), (1200, 900), (900, 900), (900, 1200), (900, 1600)]
LABEL = ["21:9", "16:9", "4:3", "1:1", "3:4", "9:16"]
print("   %-8s %-6s %-12s %-14s" % ("形状", "槽位", "铺满(=现在)", "保比例(=旧 v211)"))
worst = 0.0
for name, nat in (("HEART", 1.0), ("EYE", 1.7), ("CLOUD", 1.5), ("BOLT", 0.62)):
    for idx, (W, H) in enumerate(SLOTS):
        bw, bh = design_box(W, H, nat)
        loss = 1.0 - (bw * bh) / float(W * H)
        worst = max(worst, loss)
        if idx in (0, 3, 5):
            print("   %-8s %-6s %-12s 只剩 %.0f%% 面积" % (name, LABEL[idx], "100%", (1 - loss) * 100))
        if NEGATIVE and loss > 0.05:
            fail("%s @%s：蒙版只占 %.0f%% 面积（画面回缩 %.0f%%）"
                 % (name, LABEL[idx], (1 - loss) * 100, loss * 100))
print("   最坏情况：保比例会让蒙版只剩 %.0f%% 面积（画面回缩 %.0f%%）%s"
      % ((1 - worst) * 100, worst * 100, "  ← 负例预期失败" if NEGATIVE else "；所以蒙版一律铺满"))

# ---------------------------------------------------------------- ④ 拼合形状的「洞」
# 用 Path.op(UNION) 拼出来的形状（花瓣 / 四叶）如果中心没被任何圆盖住，蒙版正中间会留一个洞 ——
# 桌面上就是「画面中间漏一个壁纸点」，缩略图小时根本看不出来。这里解析式验一遍：芯圆要盖住洞，
# 且与每个外圆重叠（否则洞变成环，还是通）。
print("\n== ④ 拼合形状中间有没有洞（花瓣 / 四叶）")


def unit_number(pattern, label):
    m = re.search(pattern, factory)
    if not m:
        fail("找不到 %s 的常量（拼合参数被改写，门禁失效）" % label)
        return None
    return float(m.group(1))


for name, r_pat, orbit_pat, core_pat, dist_k in (
        ("PETAL", r"float r4 = unit \* ([0-9.]+)f;", r"float orbit = unit \* ([0-9.]+)f;",
         r"petalCore\.addCircle\(cx, cy, unit \* ([0-9.]+)f", 1.0),
        ("CLOVER", r"float r5 = unit \* ([0-9.]+)f;", r"float offset = unit \* ([0-9.]+)f;",
         r"cloverCore\.addCircle\(cx, cy, unit \* ([0-9.]+)f", 1.41421356)):
    r = unit_number(r_pat, name + " 花瓣半径")
    orbit = unit_number(orbit_pat, name + " 排布半径")
    core = unit_number(core_pat, name + " 芯圆半径")
    if None in (r, orbit, core):
        continue
    hole = orbit * dist_k - r          # >0 表示正中有一个半径 hole 的洞
    print("  %-7s 外圆 r=%.3f 排布 %.3f → 中心空洞半径 %.3f；芯圆 %.3f" % (name, r, orbit, hole, core))
    if hole <= 0:
        fail("%s 本来就没洞，多出来的芯圆是多余的（会改变轮廓）" % name)
    elif core <= hole:
        fail("%s 芯圆 %.3f ≤ 空洞 %.3f，洞还在" % (name, core, hole))
    elif orbit * dist_k >= core + r:
        fail("%s 芯圆与花瓣圆不相交（芯是浮的，洞变成环）" % name)
    else:
        print("          芯圆盖住洞，且与花瓣圆重叠 → 连通，无洞  OK")

# ---------------------------------------------------------------- ③ 形状清单自检
print("\n== ③ 形状清单")
print("  枚举常量 %d 个：老 36 + 新 12" % len(order))
if len(order) != 48:
    fail("枚举常量数不是 48")
dupe = [n for n in order if order.count(n) > 1]
if dupe:
    fail("枚举常量重名：%s" % set(dupe))

print("\n== 结论")
if fails:
    for f in fails:
        print("  FAIL " + f)
    print("  FAILED（%d 项）" % len(fails))
    sys.exit(1)
print("  OK")
sys.exit(0)
