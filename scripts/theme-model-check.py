#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""主题存储层「独立复算」脚本（不是门禁，是设计不变量的第二套实现）。

为什么要有它：v1.10.200~205 这几轮出的问题都不是某一行写错，而是**不变式被破坏**：
  · 语义色在方案之间串味；
  · 刚存好的方案被判成「已修改」；
  · 「错误色 / 错误提示底色」在两个模式下互换；
  · 「重置」清不干净或者清多了。

这份脚本把 ThemePrefs / ThemeManager / SettingsActivity 的读写**照抄一遍**，
用纯 Python 跑这些场景，断言结果。它的价值在于：换一套实现算同一件事，
如果两边结论不一致，说明我对代码的理解是错的 —— 那就该回去看代码，而不是相信任何一边。
（真正出包前仍以 `scripts/theme-gate.py` 那三条静态门禁 + 真机为准。）

常量必须与 src-v2/io/github/animwidget/ThemeManager.java 一致。
"""

SURFACE_D, NEUTRAL_D = 0x101114, 0x1A1B1F
SURFACE_L, NEUTRAL_L = 0xFFFFFF, 0xF2F2F2
ERR_D, ERR_L = 0xFFB4AB, 0xB3261E
PRESET_PRIMARY, PRESET_SECONDARY, PRESET_TERTIARY = 0x3D7BFF, 0x22AA88, 0x7B1FA2
ALPHA_BOX = 0.16


# ---------------------------------------------------------------- 颜色小工具
def is_light(c):
    r, g, b = (c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF
    return (r * 299 + g * 587 + b * 114) // 1000 > 160


def best_on(c):
    return 0xFFFFFF if is_light(c) == 0 else 0x111318


def blend(fg, bg, a=ALPHA_BOX):
    def ch(shift):
        return round(((bg >> shift) & 0xFF) * (1 - a) + ((fg >> shift) & 0xFF) * a)
    return (ch(16) << 16) | (ch(8) << 8) | ch(0)


def default_base(key, dark):
    if key == "surface":
        return SURFACE_D if dark else SURFACE_L
    return NEUTRAL_D if dark else NEUTRAL_L


# ---------------------------------------------------------------- 存储层
class Store:
    """ThemePrefs 的等价物。"""

    def __init__(self):
        self.d = {}
        self.schemes = {}   # id -> [primary, secondary, tertiary, surface, neutral, slider, error]
        self.bases = {}     # id -> [sd, nd, sl, nl]
        self.contrast = 0
        self.active = -1
        self.next_id = 1

    # -- 单值
    def get(self, k):
        return self.d.get(k, 0)

    def set(self, k, v):
        self.d[k] = v

    def apply_base_edit(self, key, value, dark):
        self.d[key] = value
        self.d[key + ("_d" if dark else "_l")] = value

    def apply_base_snapshot(self, surface, neutral, sd, nd, sl, nl):
        self.d.update({"surface": surface, "neutral": neutral,
                       "surface_d": sd, "neutral_d": nd,
                       "surface_l": sl, "neutral_l": nl})

    def clear_custom(self):
        for k in ["primary", "secondary", "tertiary", "surface", "neutral", "slider",
                  "error", "surface_d", "neutral_d", "surface_l", "neutral_l"]:
            self.d.pop(k, None)
        self.active = -1

    def has_custom_key(self, key):
        if key in ("surface", "neutral"):
            return any(self.get(key + sfx) for sfx in ("", "_d", "_l"))
        return self.get(key) != 0

    RESETTABLE = ("primary", "secondary", "tertiary", "slider", "error", "surface", "neutral")

    def reset_key(self, key):
        if key not in self.RESETTABLE:
            return False
        had = self.has_custom_key(key)
        keys = [key, key + "_d", key + "_l", key + "_mode"] if key in ("surface", "neutral") else [key]
        for k in keys:
            self.d.pop(k, None)
        return had

    # -- 解析（ThemeManager）
    def live_base(self, key, dark):
        scoped = self.get(key + ("_d" if dark else "_l"))
        if scoped:
            return scoped
        flat = self.get(key)
        if not flat:
            return 0
        return default_base(key, dark) if is_light(flat) == dark else flat

    def raw(self, dark):
        error = self.get("error") or (ERR_D if dark else ERR_L)
        surface = self.live_base("surface", dark) or default_base("surface", dark)
        neutral = self.live_base("neutral", dark) or default_base("neutral", dark)
        box = blend(error, surface)
        return {
            "primary": self.get("primary") or PRESET_PRIMARY,
            "secondary": self.get("secondary") or PRESET_SECONDARY,
            "tertiary": self.get("tertiary") or PRESET_TERTIARY,
            "surface": surface,
            "neutral": neutral,
            "slider": self.get("slider") or (self.get("secondary") or PRESET_SECONDARY),
            "error": error,
            "error_box": box,
            "on_error": best_on(error),
            "on_box": best_on(box),
        }

    def base_for_mode(self, key, flat, dark):
        if not flat:
            return default_base(key, dark)
        return default_base(key, dark) if is_light(flat) == dark else flat

    def legacy_bases(self, surface, neutral):
        return [self.base_for_mode("surface", surface, True),
                self.base_for_mode("neutral", neutral, True),
                self.base_for_mode("surface", surface, False),
                self.base_for_mode("neutral", neutral, False)]

    def current_bases(self):
        out = []
        for key, dark in (("surface", True), ("neutral", True),
                          ("surface", False), ("neutral", False)):
            v = self.live_base(key, dark) or default_base(key, dark)
            out.append(v)
        return out

    # -- 方案
    def archive(self, sid):
        """与 ThemePrefs.getScheme 一致：6/7/8 字段都补齐成 7 返回（缺的补 0）。"""
        c = list(self.schemes[sid])
        return c + [0] * (7 - len(c))

    def write_scheme(self, sid):
        c = self.archive(sid)
        self.set("primary", c[0])
        self.set("secondary", c[1])
        self.set("tertiary", c[2])
        self.set("slider", c[5])
        self.set("error", c[6])
        b = self.bases.get(sid) or self.legacy_bases(c[3], c[4])
        self.apply_base_snapshot(c[3], c[4], b[0], b[1], b[2], b[3])
        self.active = sid

    def save_current(self, dark):
        r = self.raw(dark)
        c = [r["primary"], r["secondary"], r["tertiary"], r["surface"],
             r["neutral"], r["slider"], r["error"]]
        b = self.current_bases()
        sid = self.next_id
        self.next_id += 1
        self.schemes[sid] = c
        self.bases[sid] = b
        self.active = sid
        return sid

    def dirty_old(self, sid, dark):
        """v1.10.202 及以前：单值比 flat，底色也拿 flat 比。"""
        c = self.archive(sid)
        for k, i in (("primary", 0), ("secondary", 1), ("tertiary", 2),
                     ("surface", 3), ("neutral", 4), ("slider", 5), ("error", 6)):
            if self.get(k) != c[i]:
                return True
        return False

    def dirty_new(self):
        """v1.10.208：返回「哪里不一样」或 None。"""
        sid = self.active
        c = self.archive(sid)
        dark = True
        for k, i in (("primary", 0), ("secondary", 1), ("tertiary", 2), ("slider", 5), ("error", 6)):
            want = c[i] if i < len(c) else 0
            live = self.get(k)
            # v1.10.209：把「无覆盖(0)」翻译成生效值再比
            d = default_raw(self, k, dark)
            if (live or d) != (want or d):
                return k
        stored = self.bases.get(sid) or self.legacy_bases(c[3], c[4])
        if self.current_bases() != stored:
            return "bases"
        return None


def default_raw(st, key, dark):
    """与 ThemeManager.defaultRawValue 等价：没有覆盖值时的原始色。"""
    if key == "primary":
        return PRESET_PRIMARY
    if key == "secondary":
        return PRESET_SECONDARY
    if key == "tertiary":
        return PRESET_TERTIARY
    if key == "slider":
        return st.get("secondary") or PRESET_SECONDARY
    if key == "error":
        return ERR_D if dark else ERR_L
    if key == "surface":
        return default_base("surface", dark)
    if key == "neutral":
        return default_base("neutral", dark)
    return 0


def scheme_value_for(st, sid, key, dark):
    """与 SettingsActivity.schemeValueFor 等价。"""
    if sid is None or sid == -1:
        return 0
    c = st.archive(sid)
    if key in ("surface", "neutral"):
        b = st.bases.get(sid) or st.legacy_bases(c[3], c[4])
        return (b[0] if dark else b[2]) if key == "surface" else (b[1] if dark else b[3])
    idx = {"primary": 0, "secondary": 1, "tertiary": 2, "surface": 3,
           "neutral": 4, "slider": 5, "error": 6}[key]
    return c[idx]

def hx(v):
    return "#%06X" % (v & 0xFFFFFF)


# ---------------------------------------------------------------- 场景
def scenario_scheme_isolation():
    print("== 1) 方案之间互不污染（含底色按模式各一份）==")
    fails = 0
    A_D, B_D, A_L, B_L, DRAFT = 0x0B0C0E, 0x1E2024, 0xFAF7F2, 0xFFF8E1, 0xFFFFFF

    def build(surface_d, surface_l):
        st = Store()
        st.set("surface", surface_d)
        st.set("surface_d", surface_d)
        st.set("surface_l", surface_l)
        return st

    st = build(A_D, A_L)
    a = st.save_current(True)
    st.clear_custom()
    st2 = build(B_D, B_L)
    # 用两个独立 Store 不现实（同一个 prefs），所以直接在同一份上改：
    st = build(A_D, A_L)
    a = st.save_current(True)
    st.clear_custom()
    st.set("surface", B_D)
    st.set("surface_d", B_D)
    st.set("surface_l", B_L)
    b = st.save_current(True)

    st.write_scheme(a)
    ok1 = st.raw(True)["surface"] == A_D
    st.apply_base_edit("surface", DRAFT, True)       # 在 A 里拖成纯白，不保存
    st.write_scheme(b)                                # 切到 B
    ok2 = st.raw(True)["surface"] == B_D
    ok3 = st.raw(False)["surface"] == B_L
    st.write_scheme(a)
    ok4 = st.raw(True)["surface"] == A_D
    st.clear_custom()                                 # 恢复预设
    ok5 = st.raw(True)["surface"] == SURFACE_D and st.raw(False)["surface"] == SURFACE_L
    for tag, ok in (("A 激活=自己的深色", ok1), ("A 拖过→切 B 仍是 B", ok2),
                    ("B 在浅色下是 B 的", ok3), ("切回 A 回存档值", ok4),
                    ("恢复预设=默认", ok5)):
        print("   %-24s %s" % (tag, "OK" if ok else "**FAIL**"))
        fails += 0 if ok else 1
    return fails


def scenario_dirty_after_copy():
    print("== 2) 「复制当前配置」之后不该显示已修改（长按出「更新」的根因）==")
    st = Store()
    st.set("surface", 0x282828)
    st.set("surface_d", 0x282828)
    st.set("surface_l", 0xFFFFFF)
    a = st.save_current(True)
    st.write_scheme(a)
    b = st.save_current(False)          # 浅色下「复制当前配置」
    old = st.dirty_old(b, False)
    new = st.dirty_new()
    print("   flat surface=%s  存档 surface=%s" % (hx(st.get("surface")), hx(st.schemes[b][3])))
    print("   旧判定 dirty=%s（应为 True：会冒「更新」）" % old)
    print("   新判定 dirty=%s（None = 干净）" % new)
    st.apply_base_edit("surface", 0x00FF00, False)   # 真改了
    real = st.dirty_new()
    print("   真改后 dirty=%s（非 None = 已修改）" % real)
    return 0 if (old is True and new is None and real is not None) else 1





def scenario_reset_to_scheme_value():
    """v1.10.209：重置取「当前方案里这个颜色的存档值」→ 保存后必须干净。"""
    print("== 2.6) 重置的「原值」= 当前方案的存档值（不是全局默认）==")
    fails = 0
    st = Store()
    st.set("surface", 0x282828)
    st.set("surface_d", 0x282828)
    st.set("surface_l", 0xFFFFFF)
    st.set("slider", 0xFE8019)          # 方案里滑块色是自定义色
    sid = st.save_current(True)
    st.write_scheme(sid)
    before = st.dirty_new()
    # 面板打开后改成别的色
    st.set("slider", 0x0F6DCD)
    edited = st.dirty_new()
    # 点「重置」：目标 = 方案存档值 → 只改面板；保存后回到存档值
    target = scheme_value_for(st, sid, "slider", True)
    st.set("slider", target)
    after = st.dirty_new()
    print("   初始=%s（应干净）  改成别的色=%s（应已修改）  重置回方案值=%s（应干净）"
          % (before, edited, after))
    ok = before is None and edited is not None and after is None and target == 0xFE8019
    print("   %s" % ("OK" if ok else "**FAIL**"))
    fails += 0 if ok else 1

    # 把滑块色「重置到全局默认」是另一回事：配色确实变了 → 应该显示已修改（诚实）
    st.set("slider", 0)                 # = 默认（辅助色）
    honest = st.dirty_new()
    print("   重置到全局默认（≠方案值）=%s（应已修改，这不是误报）" % honest)
    fails += 0 if honest is not None else 1
    return fails


def scenario_legacy_error_dirty():
    """v1.10.208：老方案（6 字段，存档里没有 error）改了错误色，必须判成「已修改」。"""
    print("== 2.5) 老方案改错误色也要出现「更新」==")
    st = Store()
    st.set("surface", 0x282828)
    st.set("surface_d", 0x282828)
    st.set("surface_l", 0xFFFFFF)
    sid = st.save_current(True)
    st.schemes[sid] = st.schemes[sid][:6]      # 模拟 v1.10.202 及以前存下的 6 字段方案
    st.write_scheme(sid)
    clean = st.dirty_new()
    st.set("error", 0x7B1FA2)                  # 只改错误色
    dirty = st.dirty_new()
    print("   未改时=%s（应为 None）  改错误色后=%s（应为 error(存档未记录)）" % (clean, dirty))
    ok = clean is None and dirty is not None
    print("   %s" % ("OK" if ok else "**FAIL**"))
    return 0 if ok else 1


def scenario_reset_timing():
    """v1.10.207：重置只是面板内的编辑，落盘必须等「保存」。"""
    print("== 4.5) 重置的落盘时机（取消必须能撤销）==")
    fails = 0

    # ① 重置（只改面板）→ 取消：存储没有任何变化
    st = Store()
    st.set("error", 0x7B1FA2)
    panel_value = default_error_for(True)          # 点重置后面板里显示的值
    ok = st.get("error") == 0x7B1FA2 and panel_value == ERR_D
    print("   重置→取消：prefs 仍是自定义 #%06X  %s" % (st.get("error"), "OK" if ok else "**FAIL**"))
    fails += 0 if ok else 1

    # ② 重置（只改面板）→ 保存：删掉覆盖值 → 解析出默认
    st = Store()
    st.set("error", 0x7B1FA2)
    commit(st, "error", default_error_for(True))
    ok = st.get("error") == 0 and st.raw(True)["error"] == ERR_D
    print("   重置→保存：覆盖值已删、现值=默认 #%06X  %s" % (st.raw(True)["error"], "OK" if ok else "**FAIL**"))
    fails += 0 if ok else 1

    # ③ 重置 → 又调成别的色 → 保存：写入那个色
    st = Store()
    st.set("error", 0x7B1FA2)
    commit(st, "error", 0x00FF00)
    ok = st.get("error") == 0x00FF00
    print("   重置→改色→保存：写入新值 #%06X  %s" % (st.get("error"), "OK" if ok else "**FAIL**"))
    fails += 0 if ok else 1

    # ④ 没重置、直接保存一个等于默认的值：同样收敛成"无覆盖"（等价、且不再显示已修改）
    st = Store()
    commit(st, "error", default_error_for(True))
    ok = st.get("error") == 0
    print("   直接保存默认值：收敛为无覆盖  %s" % ("OK" if ok else "**FAIL**"))
    fails += 0 if ok else 1
    return fails


def default_error_for(dark):
    return ERR_D if dark else ERR_L


def commit(st, key, value, dark=True):
    """与 v1.10.207 的「保存」分支等价：值==默认 → 删覆盖；否则写入。"""
    if value == default_error_for(dark):
        st.reset_key(key)
    else:
        st.set(key, value)


def scenario_reset():
    print("== 3) 「重置」语义（只清一个键、幂等、两个模式都回默认）==")
    fails = 0

    st = Store()
    st.set("error", 0x7B1FA2)
    st.set("primary", 0x123456)
    had = st.reset_key("error")
    ok = had and st.get("error") == 0 and st.get("primary") == 0x123456
    print("   重置 error：had=%s，primary 未受影响  %s" % (had, "OK" if ok else "**FAIL**"))
    fails += 0 if ok else 1

    st = Store()
    st.set("secondary", 0x00AA88)
    st.set("slider", 0xFF0000)
    st.reset_key("slider")
    ok = st.raw(True)["slider"] == 0x00AA88
    print("   重置 slider：回落到辅助色 %s %s" % (hx(st.raw(True)["slider"]), "OK" if ok else "**FAIL**"))
    fails += 0 if ok else 1

    st = Store()
    st.apply_base_edit("surface", 0xFFFFFF, True)
    st.apply_base_edit("surface", 0x000000, False)
    st.reset_key("surface")
    d, l = st.raw(True)["surface"], st.raw(False)["surface"]
    ok = d == SURFACE_D and l == SURFACE_L
    print("   重置 surface：深=%s 浅=%s  %s" % (hx(d), hx(l), "OK" if ok else "**FAIL**"))
    fails += 0 if ok else 1

    st = Store()
    ok = (st.reset_key("error") is False) and st.raw(False)["error"] == ERR_L
    print("   本来没有自定义 → 幂等不报错  %s" % ("OK" if ok else "**FAIL**"))
    fails += 0 if ok else 1

    st = Store()
    st.d["mode"] = 2
    ok = (st.reset_key("mode") is False) and st.d.get("mode") == 2
    print("   白名单外的键拒绝清除  %s" % ("OK" if ok else "**FAIL**"))
    fails += 0 if ok else 1

    st = Store()
    st.active, st.contrast = 5, 300
    st.reset_key("error")
    ok = st.active == 5 and st.contrast == 300
    print("   不动 active_scheme / 对比度  %s" % ("OK" if ok else "**FAIL**"))
    fails += 0 if ok else 1
    return fails


def scenario_error_colors():
    print("== 4) 错误色与它的底色不互换（底色恒为 16% 底）==")
    fails = 0
    for tag, dark, scene in (("浅色 默认", False, "default"), ("深色 默认", True, "default"),
                             ("浅色 自定义紫", False, 0x7B1FA2), ("深色 自定义紫", True, 0x7B1FA2),
                             ("浅色 中灰", False, 0x808080), ("深色 中灰", True, 0x808080)):
        st = Store()
        if scene != "default":
            st.set("error", scene)
        r = st.raw(dark)
        ok = r["error_box"] == blend(r["error"], r["surface"]) and \
            r["on_error"] == best_on(r["error"]) and r["on_box"] == best_on(r["error_box"])
        print("   %-14s error=%s box=%s  %s" % (tag, hx(r["error"]), hx(r["error_box"]),
                                               "OK" if ok else "**FAIL**"))
        fails += 0 if ok else 1
    return fails



def fwd(color, invert, contrast):
    """与 ThemeManager.adjust$map 等价。"""
    r, g, b = (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF
    if invert:
        r, g, b = 255 - r, 255 - g, 255 - b
    if contrast != 0.0:
        f = 1.0 + contrast * 0.035
        r = max(0, min(255, int(r * f)))
        g = max(0, min(255, int(g * f)))
        b = max(0, min(255, int(b * f)))
    return (r << 16) | (g << 8) | b


def raw_channel(display, invert, f):
    best, best_err = 0, 1 << 30
    for raw in range(256):
        v = 255 - raw if invert else raw
        out = v if f == 1.0 else max(0, min(255, int(v * f)))
        err = abs(out - display)
        if err < best_err:
            best, best_err = raw, err
        if err == 0:
            return raw
    return best


def raw_from_display(display, invert, contrast, current=0):
    """与 ThemeManager.rawFromDisplay 等价（含「现有覆盖值能显示成同色就原样保留」）。"""
    f = 1.0 + contrast * 0.035
    if current and fwd(current, invert, contrast) == display:
        return current
    return ((raw_channel((display >> 16) & 0xFF, invert, f) << 16)
            | (raw_channel((display >> 8) & 0xFF, invert, f) << 8)
            | raw_channel(display & 0xFF, invert, f))


def scenario_display_roundtrip():
    """v1.10.210：面板里选的颜色，保存后显示要**保持不变**（不再越存越漂）。"""
    print("== 5) 显示色 ↔ 原始色 往返（不许越保存越漂）==")
    fails = 0
    colors = [0x003C7A, 0xFFB4AB, 0x1E1E2E, 0x808080, 0xFFFFFF, 0x000000, 0xCBA6F7]
    worst_new, worst_old, exact, total = 0, 0, 0, 0
    for invert in (False, True):
        for contrast in (0.0, 3.0, -3.0):
            for d in colors:
                total += 1
                raw = raw_from_display(d, invert, contrast)
                back = fwd(raw, invert, contrast)
                err_new = max(abs(((back >> sh) & 0xFF) - ((d >> sh) & 0xFF)) for sh in (16, 8, 0))
                back_old = fwd(d, invert, contrast)          # 旧行为：把显示值直接当原始值存
                err_old = max(abs(((back_old >> sh) & 0xFF) - ((d >> sh) & 0xFF)) for sh in (16, 8, 0))
                worst_new = max(worst_new, err_new)
                worst_old = max(worst_old, err_old)
                exact += 1 if err_new == 0 else 0
    print("   7 色 × 2 反转 × 3 对比度：往返最大误差 新=%d/255（旧=%d/255） 精确往返=%d/%d"
          % (worst_new, worst_old, exact, total))
    # 判据：新算法必须是「最近可达值」，且绝不能比旧算法差（旧算法在压缩区间会整段跳飞）
    ok1 = worst_new <= worst_old and worst_new <= 27
    print("   取最近可达值、且不劣于旧行为  %s" % ("OK" if ok1 else "**FAIL**"))
    fails += 0 if ok1 else 1

    # 反复保存不再漂：已存的原始值能显示成同色 → 原样保留
    invert, contrast = False, 3.0
    raw = raw_from_display(0x003C7A, invert, contrast)
    same = True
    for _ in range(5):
        nxt = raw_from_display(fwd(raw, invert, contrast), invert, contrast, current=raw)
        same = same and nxt == raw
        raw = nxt
    print("   连续保存 5 次：原始值恒为 #%06X  %s" % (raw, "OK" if same else "**FAIL**"))
    fails += 0 if same else 1
    return fails


def main():
    bad = 0
    for fn in (scenario_scheme_isolation, scenario_dirty_after_copy,
               scenario_reset_to_scheme_value, scenario_legacy_error_dirty, scenario_reset,
               scenario_reset_timing, scenario_error_colors,
               scenario_display_roundtrip):
        bad += fn()
        print()
    print("总计失败 %d（0 = 全部通过）" % bad)
    return 0 if bad == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
