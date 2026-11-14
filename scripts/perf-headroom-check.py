#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
v1.10.495「剩余性能空间」门禁（verify-apk.sh [30]）。

这一行是**给用户做决定用的**（还能不能再加组件），说错就是误导。钉四件事：
  A 算法可脱机自检（PerfMathTest）：共享折算、0~100 夹取、「不建议」判定方向；
  B 数据来源必须是实测常量（71 Mpx/s 解码、1.5ms 推送），不许拍脑袋；
  C 计算必须在后台线程（要遍历所有组件的画布尺寸，会走 IPC）；
  D UI 必须跟着系统深浅色/换肤刷新（用户明确要求「实时跟随系统模式并同步更新」）。

v1.10.496 追加两组：
  E **位置**：卡片必须在 ScrollView 里、「素材 / 布局」两项之上，且**不许**再出现在
    预览区与标签栏之间（那一格会把预览 FrameLayout 的 weight=1 高度挤掉一行，
    用户要求「背景可视空间保持在 1.10.494」）；
  F **不是纯文本**：卡片必须有百分比 / 分段表（PerfMeterView）/ 数值胶囊，
    并且分段表要接进 ThemeManager.paint()，否则换肤后它停在旧配色。
退出码 0 = 通过。
"""
import os, re, shutil, subprocess, sys, tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src-v2", "io", "github", "animwidget")
TC = os.path.join(ROOT, "toolchain")
ANN = os.path.join(TC, "annotations-13.0.jar")
total = 0
bad = []


def ok(cond, msg):
    global total
    total += 1
    if not cond:
        bad.append(msg)
        print("  FAIL %s" % msg)


def read(n):
    return open(os.path.join(SRC, n), encoding="utf-8").read()


def main():
    pm = read("PerfMath.java")
    pb = read("PerfBudget.java")
    ca = read("ConfigActivity.java")

    # B 常量必须是实测值
    ok("DECODE_MPX_PER_CORE = 71.0" in pm, "单核解码吞吐不是实测的 71 Mpx/s")
    ok("PUSH_MS = 1.5" in pm, "推送成本不是实测的 1.5ms")
    ok("RESERVE_CORES" in pm and "THERMAL_FACTOR" in pm,
       "没有系统预留/热降频系数（会把余量说满）")

    # A 夹取与判定
    ok("Math.max(0, Math.min(100, percent))" in pm or "Math.min(100, percent)" in pm,
       "百分比没有夹到 0~100（超预算会显示负数）")
    ok("tooHeavy" in pm, "没有「不建议再加」的判定")
    ok("shared" in pm and "decodeCores" in pm, "没有同素材共享的折算")

    # C 后台线程
    ok('new Thread(' in pb and '"perf-budget"' in pb, "剩余性能的估算没放到后台线程（会卡主线程）")
    ok("UI.post(" in pb, "后台算完没有回主线程更新 UI")

    # D 跟随系统模式
    ok("refreshHeadroom();" in ca, "配置页没有接刷新入口")
    ok(ca.count("refreshHeadroom()") >= 2, "只在 onResume 刷：换肤/系统深浅色切换时这一行不会更新")

    layout = open(os.path.join(ROOT, "patch-v1.4.0/res/layout/activity_config.xml"),
                  encoding="utf-8").read()
    ok('tag="muted"' in layout, "这一行没有 muted 标签，换肤时不会被重染色")

    # E 位置：在 ScrollView 里（标签栏之后）、「素材 / 布局」之前；不再占预览区与标签栏之间那一格
    ok("@+id/ll_perf_headroom" in layout, "布局里没有剩余性能卡的容器 ll_perf_headroom")
    if "@+id/ll_perf_headroom" in layout:
        card = layout.index("@+id/ll_perf_headroom")
        ok(card > layout.index("@id/tab_content"),
           "性能卡又回到了预览区与标签栏之间（那里会挤掉预览 FrameLayout 的 weight=1 高度）")
        ok(card < layout.index("@id/ll_content"),
           "性能卡不在「素材」上方：必须排在 ll_content 之前")
        ok(card < layout.index("@id/ll_appearance"),
           "性能卡不在「布局」上方：必须排在 ll_appearance 之前（两个 tab 共用同一张卡）")
        ok("@+id/tv_perf_headroom" in layout and layout.index("@+id/tv_perf_headroom") > card,
           "结论行 tv_perf_headroom 不在卡片里")

    # F 不是纯文本：百分比 + 分段表 + 数值胶囊，且分段表接进换肤链路
    for wid, what in (("@+id/tv_perf_pct", "百分比"), ("@+id/perf_meter", "分段余量表"),
                      ("@+id/tv_perf_used", "已用核数胶囊"), ("@+id/tv_perf_lanes", "路数胶囊")):
        ok(wid in layout, "卡片少了%s（%s）—— 又退回一行纯文字了" % (what, wid))
    ok("io.github.animwidget.PerfMeterView" in layout, "分段表不是自绘的 PerfMeterView")
    meter = read("PerfMeterView.java")
    ok("class PerfMeterView extends View" in meter, "PerfMeterView 不是自绘 View")
    ok("applyColors(" in meter, "PerfMeterView 没有 applyColors（接不进换肤链路）")
    tm = read("ThemeManager.java")
    ok("view instanceof PerfMeterView" in tm,
       "PerfMeterView 没接进 ThemeManager.paint()：换深浅色后会停在旧配色")

    # 脱机自检
    work = tempfile.mkdtemp(prefix="perfmath-")
    try:
        cls = os.path.join(work, "cls")
        os.makedirs(cls)
        jc = subprocess.run(["javac", "-nowarn", "-cp", ANN, "-d", cls,
                             os.path.join(SRC, "PerfMath.java"),
                             os.path.join(ROOT, "tests", "PerfMathTest.java")],
                            capture_output=True, text=True)
        ok(jc.returncode == 0, "javac 失败：%s" % (jc.stderr or jc.stdout)[-300:])
        if jc.returncode == 0:
            run = subprocess.run(["java", "-cp", cls + ":" + ANN,
                                  "io.github.animwidget.PerfMathTest"],
                                 capture_output=True, text=True)
            sys.stdout.write(run.stdout)
            ok(run.returncode == 0 and "PERFMATH OK" in run.stdout,
               "脱机自检未通过（返回码 %d）" % run.returncode)
    finally:
        shutil.rmtree(work, ignore_errors=True)

    if bad:
        print("PERFHEADROOM-CHECK FAILED %d/%d" % (len(bad), total))
        for m in bad:
            print("  - %s" % m)
        return 1
    print("PERFHEADROOM-CHECK OK %d" % total)
    return 0


if __name__ == "__main__":
    sys.exit(main())
