#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
v1.10.481（P0-3 / A）：**同素材共享解码 N→1** 的门禁（verify-apk.sh [27]）。

为什么要有它：
    共享帧表一旦「键算错」，症状是**一帧画面不对**，不崩、不掉帧、不打日志 ——
    三份真机日志都追不到那种错。所以必须在源码级 + 脱机自检里钉死：

  A 键：同输入同键 / 帧号按帧数归一化 / 任何一个输入变了键就变 / 拿不到签名返回 null
  B 顺序：解码任务必须**先查表再解码**（顺序反了 = 永远不命中，功能等于没写）
  C 安全：共享位图**绝不 recycle**（一 recycle 另一路黑屏）；表必须有字节与条数上限
  D 身份：键里**不许**出现 mtime（那是「本组件重烘」的判据，不该参与跨组件共享），
          但 contentTag 里必须**保留** mtime（否则同一组件换素材不重烘）
  E 导航栏 45% 透明：常量必须是 0.55 不透明度，高度必须取 navigationBars inset（不写死 dp）

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


def code_only(text):
    """去掉注释行再断言：注释里写「绝不 recycle」不能被当成真的调用了 recycle。"""
    out = []
    in_block = False
    for line in text.splitlines():
        t = line.strip()
        if in_block:
            if "*/" in t:
                in_block = False
            continue
        if t.startswith("/*"):
            if "*/" not in t:
                in_block = True
            continue
        if t.startswith("//") or t.startswith("*"):
            continue
        out.append(line)
    return "\n".join(out)


def main():
    static_checks()
    java_selftest()
    return finish()


def static_checks():
    fs = code_only(read("FrameShare.java"))
    fk = read("FrameShareKey.java")
    up = read("WidgetUriPush.java")
    ring = read("FrameRing.java")
    log = read("LogActivity.java")

    # ---- C 安全：绝不 recycle + 必须有上限
    ok("recycle()" not in fs,
       "FrameShare 里出现了 recycle()：共享位图可能同时被两路/桌面持有，recycle 一次就是另一路黑屏")
    ok("MAX_BYTES" in fs and "MAX_ENTRIES" in fs,
       "共享表没有字节/条数上限（会一直长到 OOM）")
    ok("new LinkedHashMap<>(16, 0.75f, true)" in fs,
       "LRU 不是访问顺序（accessOrder=false 淘汰的是最早放入的，不是最久没用的）")
    ok("getAllocationByteCount()" in fs,
       "没用 getAllocationByteCount 记账（按 width*height*4 记会低估共享内存位图）")

    # ---- B 顺序：先查表、再解码
    i_cls = up.find("private static final class DecodeTask")
    i_end = up.find("/** 提交一帧解码", i_cls)
    body = up[i_cls:i_end] if i_cls >= 0 and i_end > i_cls else ""
    ok(body, "找不到 DecodeTask.run 的body")
    i_get = body.find("FrameShare.get(")
    i_dec = body.find("decodeFrameFile(")
    ok(i_get >= 0, "DecodeTask 根本没查共享表（功能没接上）")
    ok(i_dec >= 0, "DecodeTask 里没有 decodeFrameFile（猜错了函数体）")
    ok(0 <= i_get < i_dec, "查表必须在解码之前（顺序反了 = 永远不命中，白写）")
    ok(body.count("FrameShare.put(") >= 2,
       "只在入环时 put 了：迟到的结果（taken=false）也是别人下一拍要的帧，必须一起公布")
    ok("共享帧=" in fs, "没有 共享帧= 统计行（真机上看不到命中率就等于没验证）")
    ok("共享帧=" in fs and "takeStat()" in up, "W23 那一行没接 共享帧= 统计")

    # ---- D 身份：键里不许有 mtime，但 contentTag 必须保留 mtime
    m2 = re.search(r"static String shareBase\(.*?\n    \}", up, re.S)
    sb = m2.group(0) if m2 else ""
    ok(sb, "找不到 shareBase()")
    ok("lastModified" not in sb, "shareBase 里出现了 lastModified（跨组件就永远不命中）")
    ok("renderTag(config)" in sb and "srcId(config)" in sb,
       "shareBase 没把「渲染参数 + 素材身份」算进去")
    m3 = re.search(r"private static String contentTag\(.*?\n    \}", up, re.S)
    ct = m3.group(0) if m3 else ""
    ok("lastModified()" in ct,
       "contentTag 里的 mtime 被删了：同一组件换素材就不会重烘（这是它的唯一作用）")
    ok("srcId(config) + \"|\" + wh" in sb or "srcId(config)" in sb,
       "shareBase 没拼素材身份")
    ok("srcId" in up and "getSourceUri()" in up, "srcId 没覆盖素材身份（换素材会错键）")

    # ---- A 键本体
    ok("HEAD_BYTES" in fk and "fnv1a" in fk,
       "FrameShareKey 没有内容签名（只靠路径/帧号 = 换素材就错帧）")
    ok("< 0 || count <= 0" in fk or "count <= 0" in fk,
       "键函数没拒绝非法帧数/长度")
    ok("((idx % count) + count) % count" in fk,
       "帧号没归一化（-1 或 237 会算成另一个键，同一帧解两次）")
    ok("FrameShareKey.of(" in up, "WidgetUriPush 没用 FrameShareKey.of 算键")
    ok("headHash(" in up, "算键时没取内容指纹")

    # ---- 同一帧并发去重（single-flight）
    ok("Flight" in fs and "begin(" in fs and "await(" in fs and "finish(" in fs,
       "没有「同一帧只解一次」的去重：两路同相位时会各解一次（实测命中率只有 17~30%）")
    ok("abandon(" in fs and "abandon(" in up,
       "没有 abandon()：leader 失败时等待者要白等到超时才自己解")
    m = __import__("re").search(r"WAIT_MS = (\d+)L", fs)
    ok(m is not None and 5 <= int(m.group(1)) <= 19,
       "等待上限不在 5~19ms（一拍 20ms：等太久会掉帧，等太短等于没等）")

    # ---- 帧数要进 Pick（共享键要用它归一化）
    ok("int count;" in ring and "p.count = count;" in ring,
       "FrameRing.Pick 没有带上帧数（键里 count 只能瞎传）")

    # ---- E 导航栏 45% 透明
    ok("NAV_BAR_OPAQUE = 0.55f" in log,
       "日志页导航栏不透明度不是 0.55（45% 透明 = 不透明度 55%）")
    ok("navigationBars()" in log,
       "导航栏那条膜的高度不是取 navigationBars inset（写死 dp 在三键/手势下必错）")
    ok("withAlpha(colors.getBackground(), NAV_BAR_OPAQUE)" in log,
       "换肤时那条膜没跟着换色（换主题后它还是旧色）")
    m4 = re.search(r"private void installNavScrim\(.*?\n    \}", log, re.S)
    ok(m4 is not None and "SDK_INT < 30" in m4.group(0),
       "installNavScrim 没判 SDK<30（那时这一页本来就不是 edge-to-edge）")


def java_selftest():
    work = tempfile.mkdtemp(prefix="frameshaekey-")
    try:
        classes = os.path.join(work, "cls")
        os.makedirs(classes)
        javac = subprocess.run(
            ["javac", "-nowarn", "-cp", ANN, "-d", classes,
             os.path.join(SRC, "FrameShareKey.java"),
             os.path.join(ROOT, "tests", "FrameShareKeyTest.java")],
            capture_output=True, text=True)
        ok(javac.returncode == 0, "javac 失败：%s" % (javac.stderr or javac.stdout)[-400:])
        if javac.returncode != 0:
            return
        run = subprocess.run(["java", "-cp", classes + ":" + ANN,
                              "io.github.animwidget.FrameShareKeyTest"],
                             capture_output=True, text=True)
        sys.stdout.write(run.stdout)
        ok(run.returncode == 0 and "FRAMESHAREKEY OK" in run.stdout,
           "脱机自检未通过（返回码 %d）" % run.returncode)
    finally:
        shutil.rmtree(work, ignore_errors=True)


def finish():
    if bad:
        print("FRAMESHARE-CHECK FAILED %d/%d" % (len(bad), total))
        for m in bad:
            print("  - %s" % m)
        return 1
    print("FRAMESHARE-CHECK OK %d" % total)
    return 0


if __name__ == "__main__":
    sys.exit(main())
