#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
v1.10.478（P0-2 否决后的第一步）：**编码/解码对拍**（verify-apk.sh [26]）。

为什么要它：
    P0-2「不透明内容帧 JPEG + 常驻 overlay」被外部两份复核判死在几何上（圆角必露方角），
    这条路剩下的唯一形态是**在我方进程内**做「JPEG 解码 + 常驻蒙版合成」。
    它成立不成立，全押在一个数上：**同一张画面，JPEG 解码比带 alpha 的 WebP 快多少**。
    这个数只能在真机上测（`W48`），所以这一版只做测量、不动播放。

这个门禁钉三件事：
    A 脱机自检（BenchMathTest）：中位数 / 单位 / 判据方向 —— 日志写错最容易骗自己的地方；
    B 对拍的结构断言：必须在**同一张成品帧、同一像素数、同一线程**上比较，样本取中位数，
      并且合成那一步必须真的做了（blit + DST_IN），否则「JPEG 更便宜」是白算的；
    C 不许接进播放路径：这一版 W48 只许在烘焙末尾打一行，`WidgetUriPush` / `PlaybackService`
      里不许出现 DecodeBench（出现了 = 有人拿真机播放做实验）。

退出码 0 = 通过。
"""

import os
import re
import shutil
import subprocess
import sys
import tempfile

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


def read(name):
    return open(os.path.join(SRC, name), encoding="utf-8").read()


def main():
    static_checks()
    java_selftest()
    return finish()


def static_checks():
    db = read("DecodeBench.java")
    bm = read("BenchMath.java")
    wp = read("WidgetPrewarm.java")
    up = read("WidgetUriPush.java")
    pb = read("PlaybackService.java")

    # ---- B 结构：同帧、同尺寸、同解码器、取中位数、真的合成了
    ok("bakedFrame(" in wp and "DecodeBench.probe(" in wp,
       "预热里没接入对拍（DecodeBench.probe 没被调用）")
    ok("int mid = count / 2" in wp,
       "没取「中间那一帧」（取首帧会被首帧预热污染，取末帧可能刚写完没落页缓存）")
    ok("ImageDecoder" in db and "ALLOCATOR_SHARED_MEMORY" in db,
       "测的不是播放路径那个解码器（必须和 decodeShared 同口径：ImageDecoder 共享内存）")
    ok("extractAlpha()" in db,
       "蒙版不是从成品帧自己的 alpha 取的 —— 那测的就不是「阴影+圆角+挖洞」那一层")
    ok("CompressFormat.JPEG" in db and "QUALITY" in db,
       "没有把同一张画面编成 JPEG 再解（不编就看不出编码器差异）")
    ok("PorterDuff.Mode.DST_IN" in db,
       "合成那一步没做 DST_IN —— 那 JPEG 出来就是方角，等于没算「裁圆角」的成本")
    ok("eraseColor(0)" in db,
       "合成前没清画布：第二圈开始会把上一圈的结果叠上去（越测越快，纯假数）")
    ok("ROUNDS" in db and "median(" in db,
       "样本没取中位数（首轮类加载/页缓存污染会直接进结论）")
    ok("warm" in db.lower(),
       "没有预热轮（冷启动的第一次解码会把均值抬高一倍以上）")
    ok("Math.abs" not in db, "解码对拍里不该有容差判断（这是测量，不是判据）")

    # ---- B 结构：两个尺寸必须一致（同一张画面），否则「快多少」没意义
    ok("src.getWidth()" in db and "getHeight()" in db,
       "没取成品帧的真实像素尺寸（日志里的画布必须是量到的那张，不是配置里的）")

    # ---- A 纯逻辑：日志口径在 BenchMath 里
    ok("BenchMath.line(" in db and "BenchMath.median(" in db,
       "日志口径没走 BenchMath（纯逻辑必须可脱机自检）")
    ok("import android" not in bm,
       "BenchMath 不许 import android（否则脱机自检编不过）")
    for key in ("解码对拍", "WebP=", "JPEG=", "合成=", "合计=", "省=", "判据="):
        ok(key in bm, "日志里缺字段 %r（读日志的人要能一眼看出瓶颈在哪）" % key)

    # ---- C 不许接进播放路径
    for name, src in (("WidgetUriPush.java", up), ("PlaybackService.java", pb)):
        ok("DecodeBench" not in src,
           "%s 里出现了 DecodeBench：这一版只许在烘焙末尾测一次，不许拿播放做实验" % name)
    m = re.search(r"static final int ROUNDS = (\d+)", db)
    ok(m is not None and 3 <= int(m.group(1)) <= 9,
       "样本数不在 3~9 之间（太少被抖动主导，太多拖慢导出）")
    m2 = re.search(r"static final int MIN_PIXELS = (\d+)", db)
    ok(m2 is not None and 20000 <= int(m2.group(1)) <= 60000,
       "最小像素门槛不在 2 万~6 万之间（478 定 10 万，真机上 240x240/286x286 一次都没测到）")

    # ---- D 不许静默跳过（478 的教训：门槛把整代吞掉，日志里连一行都没有）
    ok("BenchMath.skip(" in db,
       "画布超出门槛时没打「跳过」那一行 —— 用户只会以为功能没接上")
    ok("px < MIN_PIXELS" in db or "px<MIN_PIXELS" in db,
       "门槛判断不见了（或改成不比较像素数了）")
    ok("degrade" not in db, "解码对拍里不该有降级逻辑（这是一次性测量）")

    # ---- 诊断必须登记（DiagCatalog 里要有 W48）
    cat = open(os.path.join(SRC, "DiagCatalog.java"), encoding="utf-8").read()
    ok('"W48"' in cat, "W48 没登记进 DiagCatalog（判断代号体检会失败）")
    ok("解码对拍" in cat, "DiagCatalog 的 W48 条目里没有示例行")


def java_selftest():
    work = tempfile.mkdtemp(prefix="benchmath-")
    try:
        classes = os.path.join(work, "cls")
        os.makedirs(classes)
        javac = subprocess.run(
            ["javac", "-nowarn", "-cp", ANN, "-d", classes,
             os.path.join(SRC, "BenchMath.java"),
             os.path.join(ROOT, "tests", "BenchMathTest.java")],
            capture_output=True, text=True)
        ok(javac.returncode == 0, "javac 失败：%s" % (javac.stderr or javac.stdout)[-400:])
        if javac.returncode != 0:
            return
        run = subprocess.run(["java", "-cp", classes + ":" + ANN,
                              "io.github.animwidget.BenchMathTest"],
                             capture_output=True, text=True)
        sys.stdout.write(run.stdout)
        ok(run.returncode == 0 and "BENCHMATH OK" in run.stdout,
           "脱机自检未通过（返回码 %d）" % run.returncode)
    finally:
        shutil.rmtree(work, ignore_errors=True)


def finish():
    if bad:
        print("DECODEBENCH-CHECK FAILED %d/%d" % (len(bad), total))
        for m in bad:
            print("  - %s" % m)
        return 1
    print("DECODEBENCH-CHECK OK %d" % total)
    return 0


if __name__ == "__main__":
    sys.exit(main())
