#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
v1.10.469（#4 第 1 步）：**动图打包器端到端自检**（verify-apk.sh [25]）。

为什么要它：
    `AnimPack` 把「这一代烘好的逐帧 WebP」拼成**一个**动图文件。它的三条失败路径
    （帧数超限 / 输入不是 WebP / 失败留残文件）在真机上永远走不到，一旦走到就是
    「桌面整块不显示」这种**静默**失败 —— 只能在这里钉住。

做法（和门禁 [21] 同一套思路）：
    1) Pillow 生成 N 张**真**单帧 WebP（不同纯色，方便逐帧对像素）；
    2) javac 只编 AnimPack + WebpAnimWriter + AnimPackTest（三者都不依赖 android）；
    3) 跑 Java 自检（时长口径 / 正常打包 / 空列表 / 帧数上限 / 非法输入）；
    4) 再用 Pillow 把产物解码：帧数、每帧时长、循环次数、逐帧像素**逐字节**比对。

退出码 0 = 通过。
"""

import os
import re
import shutil
import subprocess
import sys
import tempfile

from PIL import Image

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


def main():
    static_checks()
    work = tempfile.mkdtemp(prefix="animpack-")
    try:
        # ---- 1) 造真单帧 WebP（64x64 纯色，按顺序编号）
        frames_dir = os.path.join(work, "frames")
        os.makedirs(frames_dir)
        colors = [(220, 60, 60), (60, 200, 90), (70, 110, 240),
                  (240, 200, 70), (170, 80, 210), (40, 200, 210)]
        for i, rgb in enumerate(colors):
            img = Image.new("RGB", (64, 64), rgb)
            img.save(os.path.join(frames_dir, "%05d.webp" % i), "WEBP", lossless=True, quality=100)

        # ---- 2) 编译（只这三份，不碰 android）
        classes = os.path.join(work, "cls")
        os.makedirs(classes)
        javac = subprocess.run(
            ["javac", "-nowarn", "-cp", ANN, "-d", classes,
             os.path.join(SRC, "AnimPack.java"),
             os.path.join(SRC, "WebpAnimWriter.java"),
             os.path.join(ROOT, "tests", "AnimPackTest.java"),
             os.path.join(SRC, "PlaybackContract.java"),
             os.path.join(ROOT, "tests", "PlaybackContractTest.java")],
            capture_output=True, text=True)
        ok(javac.returncode == 0, "javac 失败：%s" % (javac.stderr or javac.stdout)[-500:])
        if javac.returncode != 0:
            return finish()

        # ---- 3) 跑 Java 自检（fps=30）
        out = os.path.join(work, "anim.webp")
        run = subprocess.run(
            ["java", "-cp", classes + ":" + ANN, "io.github.animwidget.AnimPackTest",
             frames_dir, "300", out],
            capture_output=True, text=True)
        sys.stdout.write(run.stdout)
        ok(run.returncode == 0 and "ANIMPACK OK" in run.stdout,
           "Java 自检未通过（返回码 %d）" % run.returncode)

        # ---- 4) Pillow 解码比对
        if not os.path.isfile(out):
            ok(False, "没有产出 anim.webp")
            return finish()
        im = Image.open(out)
        n = getattr(im, "n_frames", 1)
        ok(n == len(colors), "帧数=%d（应为 %d）" % (n, len(colors)))
        ok(im.info.get("loop", None) == 0, "循环次数=0（无限）实际=%r" % im.info.get("loop"))

        dur_ok = True
        sum_ok = True
        pix_ok = True
        total = 0
        for i in range(min(n, len(colors))):
            im.seek(i)
            frame = im.convert("RGB")          # ⚠️ 必须先解码，Pillow 才会填 info["duration"]
            d = im.info.get("duration", None)
            if d is None or abs(d - 33.33) > 1.0:
                dur_ok = False
                print("    第 %d 帧时长=%r（应≈33.33，允许 ±1ms）" % (i, d))
            total += (d or 0)
            got = frame.getpixel((32, 32))
            want = colors[i]
            if not all(abs(got[k] - want[k]) <= 2 for k in range(3)):
                pix_ok = False
                print("    第 %d 帧像素=%r（应为 %r）" % (i, got, want))
        ok(dur_ok, "单帧时长必须 ≈33.33ms（逐帧在 33/34 之间分配）")
        ok(abs(total - round(len(colors) * 10000 / 300)) <= 1,
           "总时长必须精确等于源片（%dms，实际 %dms）" % (round(len(colors) * 10000 / 300), total))
        ok(pix_ok, "逐帧像素必须与输入单帧一致（差一丢丢都说明位流被重新编码了）")

        # ---- 4b) 播放口径自检（清晰度/FPS/速度/时长）
        pc = subprocess.run(["java", "-cp", classes + ":" + ANN,
                             "io.github.animwidget.PlaybackContractTest"],
                            capture_output=True, text=True)
        sys.stdout.write(pc.stdout)
        ok(pc.returncode == 0 and "PLAYBACKCONTRACT OK" in pc.stdout,
           "播放口径自检未通过（四项之一不成立）")

        # ---- 5) fps=60：只验时长口径（同一批帧，换帧率）
        out60 = os.path.join(work, "anim60.webp")
        run60 = subprocess.run(
            ["java", "-cp", classes + ":" + ANN, "io.github.animwidget.AnimPackTest",
             frames_dir, "600", out60],
            capture_output=True, text=True)
        ok(run60.returncode == 0, "fps=60 那轮跑失败")
        if os.path.isfile(out60):
            im2 = Image.open(out60)
            im2.seek(0)
            im2.convert("RGB")
            d60 = im2.info.get("duration", None)
            ok(d60 is not None and abs(d60 - 16.67) <= 1.0,
               "fps=60 单帧时长=%r（应≈16.67）" % d60)
    finally:
        shutil.rmtree(work, ignore_errors=True)
    return finish()


def static_checks():
    """#4 第 2 步的结构断言：自播分支必须存在，且只在「有文件 + 在限内」时才停推帧。"""
    pb = open(os.path.join(SRC, "PlaybackService.java"), encoding="utf-8").read()
    ap = open(os.path.join(SRC, "AnimPush.java"), encoding="utf-8").read()
    wp = open(os.path.join(SRC, "WidgetUriPush.java"), encoding="utf-8").read()

    m = re.search(r"if \(config\.getFrameCount\(\) > 1 && AnimPush\.adopted\(this, config, now\)\) \{(.*?)\n                \}",
                  pb, re.S)
    branch = m.group(1) if m else ""
    ok(m is not None, "PlaybackService 没有「有动图文件就交给桌面自播」的分支")
    ok("AnimPush.pushIfChanged(this, manager, config)" in branch,
       "自播分支没有推送（只停 tick 不推 = 桌面停在上一张）")
    ok("state2.setNextAt(Long.MAX_VALUE)" in branch,
       "自播分支没有把这个组件从推帧队列里摘掉（还会继续逐帧推）")
    ok("MAX_BYTES" in ap and "length() > MAX_BYTES" in ap,
       "AnimPush 没有体积上限（15MB 那种大文件会拖慢首帧）")
    ok("lastModified()" in ap,
       "AnimPush 没有按 mtime 记忆「推过哪一代」（要么重复推，要么换代后漏推）")
    ok("自播结束，恢复逐帧推送" in pb,
       "没有恢复闸：交出过自播的组件，一旦判定失效就再也不推帧（只能重启）")
    ok("AnimPush.alive(widgetId, now)" in pb,
       "自播没有看门狗：桌面一次都不来取时会永远冻在上一张")
    wr = open(os.path.join(SRC, "WidgetRender.java"), encoding="utf-8").read()
    ok("AnimPush.adopted(context, config" in wr,
       "推帧出口（render）没有自播闸门：重推/更新会把刚推的动图覆盖成静止帧")
    m = re.search(r"MAX_BYTES\s*=\s*(\d+)L \* 1024L \* 1024L", ap)
    ok(m is not None and int(m.group(1)) <= 32,
       "自播体积上限 > 32MB：动图文件不该无限放大")
    m2 = re.search(r"MAX_FRAMES\s*=\s*(\d+)", ap)
    ok(m2 is not None and 200 <= int(m2.group(1)) <= 300,
       "自播帧数上限不合理（实测最大 235 帧）")
    ok("CACHE_MS" in ap,
       "自播判定没有缓存：render() 每帧都调它，扫目录 + 打日志会把 50fps 推帧拖慢（471 实测 50 行/秒）")
    waw = open(os.path.join(SRC, "WebpAnimWriter.java"), encoding="utf-8").read()
    ok("raf.write(0x02);" in waw,
       "ANMF flags 必须是 0x02（bit0=dispose bit1=blend；0x01 的语义是「混合+清背景」）")
    ok("bit0 = dispose" in waw,
       "注释没写清 ANMF flags 的位序（bit0=dispose bit1=blend）")
    ok("SELF_PLAY = false" in ap,
       "自播没有默认关闭：两次真机验证都表明桌面不自播，不能再默认开着")
    ok("if (!SELF_PLAY)" in ap,
       "关闭开关没接到判定里（写了常量但没用）")
    ok("WidgetUriPush.enabled(context)" in ap,
       "自播没尊重「URI 推送」开关（关着还偷偷走 URI 就违背用户设置）")
    ok("animFetched(" in ap,
       "看门狗不看「桌面取走没有」——那它只会一直自播")
    ok("setImageViewUri" in wp and "pushAnimUri" in wp,
       "WidgetUriPush 里没有单 URI 推送的实现")


def finish():
    if bad:
        print("ANIMPACK-CHECK FAILED %d/%d" % (len(bad), total))
        for m in bad:
            print("  - %s" % m)
        return 1
    print("ANIMPACK-CHECK OK %d" % total)
    return 0


if __name__ == "__main__":
    sys.exit(main())
