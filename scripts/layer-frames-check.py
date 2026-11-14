#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
v1.10.482（P0-3 / B）：**分层帧**（JPEG 内容 + 常驻 alpha）的门禁（verify-apk.sh [28]）。

它为什么必须钉死：
    这条路把「每帧解一张带 alpha 的 WebP」换成「解一张 JPEG + 一次 DST_IN」。
    一旦 alpha 没乘上、或者乘早了乘晚了，症状是**画面形状不对**（方角、黑底、整块透明），
    但那不是崩溃，日志里也只有一行自检 —— 只能靠这里的结构断言兜住：

  A 顺序：烘焙必须「写完帧文件之后」再写分层产物，且在第 0 帧做自检；
          播放必须「先 ready() 再 compose()」，失败就落回原路径
  B 语义：合成必须是 JPEG 铺满 + DST_IN(alpha)（不是 SRC_OVER，也不是把 alpha 画在上层）
  C 安全：分层产物写失败 / 自检不过 ⇒ 必须删掉产物并让 ready()=false（不许「半套」产物）
  D 判据：自检只许比**叠完之后**的像素，用 MAE（最大值会被 JPEG 边缘振铃带偏，必然 FAIL）
  E 回落：这一代 FAIL 时，播放链必须还能走原来的 WebP 帧（不依赖分层）

退出码 0 = 通过。
"""

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src-v2", "io", "github", "animwidget")

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
    lf = code_only(read("LayerFrames.java"))
    up = code_only(read("WidgetUriPush.java"))
    wp = code_only(read("WidgetPrewarm.java"))
    cat = read("DiagCatalog.java")

    # ---- B 语义：JPEG 铺满 + DST_IN(alpha)
    ok("DST_IN" in lf, "合成没用 DST_IN（形状/阴影就乘不回去）")
    ok("SRC_OVER" not in lf, "合成里出现了 SRC_OVER：alpha 会变成叠加而不是相乘")
    ok("inMutable" in lf, "没用 inMutable 就地乘 alpha（每帧多一张全画布位图，50fps 下很贵）")
    ok("alphaOf(" in lf and "& 0xFF000000" in lf,
       "alpha 不是从成品帧的像素里抽的（那就是另一张图，不是「同一帧的形状」）")
    ok("extractAlpha()" not in lf,
       "又用回 extractAlpha() 了：它给的是 ALPHA_8，PNG 编码在老机型上会静默失败（482 的真机教训）")
    ok("CompressFormat.JPEG" in lf, "内容帧没编成 JPEG（这条路的前提就是它）")

    # ---- A 顺序：烘焙写顺序 + 播放先 ready
    i_write = wp.find("LayerFrames.write(")
    i_done = wp.find("done.incrementAndGet()", i_write) if i_write > 0 else -1
    ok(i_write > 0, "烘焙里没调用 LayerFrames.write（分层产物根本没写）")
    ok(0 < i_write < i_done, "分层产物必须在帧文件写成功之后、记账之前写")
    # ---- G 顺序：共享表必须在分层之前查（484 把分层放前面 → 共享帧=0/0、解码×301，
    #        A 的收益整个丢掉）
    i_share = up.find("FrameShare.get(shareKey)")
    i_lay = up.find("LayerFrames.ready(")
    ok(0 <= i_share < i_lay,
       "共享表必须在分层之前查：反了就是「两路各解各的 JPEG」，解的次数又回到 N 倍")
    i_ready = up.find("LayerFrames.ready(")
    i_compose = up.find("LayerFrames.compose(")
    i_dec = up.find("decodeFrameFile(")
    ok(0 < i_ready < i_compose, "播放链必须先 ready() 再 compose()")
    ok(i_compose < i_dec, "分层帧必须在普通解码**之前**试（放后面等于永远走老路）")

    # ---- C 安全：写失败/自检不过要清干净
    ok("clear(dir)" in lf, "自检不过时必须清掉分层产物（否则半套产物会让播放链读出残帧）")
    ok("mark(dir).isFile()" in lf, "没有「这一代可用」的标记文件：半套产物照样会被用上")
    ok("if (!ON" in lf or "!ON " in lf, "没有总开关（出问题时无法一键回退）")

    # ---- D 判据：MAE 而不是最大值
    ok("MAX_MEAN_DIFF" in lf, "自检判据不是平均差（最大值会被 JPEG 边缘振铃带偏，必然 FAIL）")
    m = re.search(r"MAX_MEAN_DIFF = ([0-9.]+)", lf)
    ok(m is not None and 0.5 <= float(m.group(1)) <= 12.0,
       "MAE 门限不在 0.5~12 之间（太低=永远回落，太高=错帧也放行）")
    ok("meanDiff(" in lf and "getPixels(" in lf, "自检没逐像素比（那就不叫自检）")

    # ---- E 回落：FAIL 时这一代必须还能用老帧
    ok("return pass;" in lf, "自检结果没返回给调用方（烘焙那边拿不到「这一代行不行」）")
    ok("ready(dirNow, idx)" in up, "播放链没有按「这一代+这一帧」判断，缺某帧会读空")

    # ---- F 建立时机：不许绑在 idx==0 上（483 的错：烘焙从 起点=10 绕一圈才到 0，
    #        结果 235 帧里只有最后 10 帧有 c*.jpg，真机日志 分层=11/11）
    ok("idx != 0 && !mark(dir).isFile()" not in lf,
       "又按「等第 0 帧」建立了：烘焙起点不是 0 时，整代只有尾巴几帧能用")
    ok("ESTABLISH" in lf, "建立 alpha/自检没有串行锁（两个线程会互相读到半张 alpha）")
    ok("FAILED" in lf, "自检没过时代内没有「不再重试」的标记（会 235 次重复自检）")

    # ---- 日志登记
    ok('"W49"' in cat, "W49 没登记进 DiagCatalog（判断代号体检会失败）")
    ok("分层帧 自检=" in cat, "DiagCatalog 的 W49 条目没有示例行")

    finish()


def finish():
    if bad:
        print("LAYERFRAMES-CHECK FAILED %d/%d" % (len(bad), total))
        for m in bad:
            print("  - %s" % m)
        return 1
    print("LAYERFRAMES-CHECK OK %d" % total)
    return 0


if __name__ == "__main__":
    sys.exit(main())
