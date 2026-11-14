#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
v1.10.489：**推帧统计口径** 的门禁（verify-apk.sh [29]）。

背景（两次误判的存档）：
  1) 487 的日志里 `共享帧=` 只有 9~25%，看起来像「共享没生效」；其实真正省下的解码在
     「同一帧去重」那条路上，而那个数**没有被计数**。日志口径骗了读日志的人（也就是我）。
  2) 488 我把「拖动尺寸时画面短暂停」当 bug 修了（画布没变就不重推）。用户当场纠正：
     **拖动时暂停是所有桌面组件都有的正常现象**（桌面在 resize 期间不更新子视图），
     而且暂停结束后与其它组件的播放进度仍然对齐 —— 不需要修。
     所以这里加一条**反向断言**：尺寸变更的处理里不许再出现任何「跳过重推」的判断。

退出码 0 = 通过。
"""

import os
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
    prov = code_only(read("WidgetProvider.java"))
    render = code_only(read("WidgetRender.java"))
    fs = code_only(read("FrameShare.java"))
    up = code_only(read("WidgetUriPush.java"))

    # ---- 计数口径：同一帧去重必须报出来
    ok("noteFlightHit(" in fs and "noteFlightHit(" in up,
       "同一帧去重的命中没被计数（日志会让人以为共享没生效）")
    ok("同帧=" in fs, "统计行里没有 `同帧=`（487 的误判就是这么来的）")
    ok("FLIGHT_HITS" in fs, "没有同帧命中的计数器")
    ok("合计省=" in fs, "没有「表命中+同帧」的合计（读的人要自己加）")

    # ---- 反向断言：拖动尺寸的处理保持原样，不许再有「跳过重推」的判断
    ok("sizePushNeeded" not in prov,
       "尺寸变更里又出现 `sizePushNeeded` 了：拖动时暂停是正常现象，不是 bug（用户已明确）")
    ok("LAST_CANVAS" not in render and "LAST_FIT" not in render,
       "WidgetRender 里又留了「上次画布/口径」的记录：那是 488 误修的残留")
    ok("WidgetUriPush.forgetLast(widgetId)" in prov,
       "尺寸变更必须继续清掉「同一张不重推」的去重（否则桌面停在旧格子那一版上被裁）")

    # ---- 多格组件必须进播放链（v1.10.492：漏了它就是「加完多格组件一次帧都不推」）
    prov2 = read("WidgetProvider.java")
    pb = read("PlaybackService.java")
    wr = read("WidgetRender.java")
    ok("MultiCell.allIds(" in prov2, "widgetIds() 没并上多格 provider（多格组件不播）")
    ok("MultiCell.allIds(" in pb, "孤儿清理没把多格算活着（会把它的缓存删光）")
    ok("MultiCell.allIds(" in wr, "repushAll 漏了多格")
    ok("MultiCellProvider" in read("MultiCell.java"), "MultiCell 不认 MultiCellProvider")

    finish()


def finish():
    if bad:
        print("PUSHSTATS-CHECK FAILED %d/%d" % (len(bad), total))
        for m in bad:
            print("  - %s" % m)
        return 1
    print("PUSHSTATS-CHECK OK %d" % total)
    return 0


if __name__ == "__main__":
    sys.exit(main())
