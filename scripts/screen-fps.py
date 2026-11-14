#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
地面真值：**用屏录数「桌面到底画了多少帧」**（v1.10.490）

为什么需要它：
    三家外部复核（Claude / DeepSeek / ChatGPT）都指向同一件事 —— 我们自己的 `W10/W23`
    只能证明「我们发了多少次」，证明不了「桌面画了多少次」。AOSP 的语义是
    「每个 host 只保留一个 in-flight apply，新帧到达即 cancel 旧的」，被 cancel 的帧**永不显示**，
    所以「我们发 50fps」和「屏上 30fps」可以同时为真，且在我们的日志里看不出来。
    要判 `T_apply ≤ 1/(N·f)` 这条门槛，唯一的地面真值是**屏录帧差**。

用法：
    python3 scripts/screen-fps.py <录像.mp4> --region 0,300,360,360 --region 360,300,360,360 ...
    （region = x,y,w,h，像素坐标；可给多个，逐个统计）

输出：每块区域「每秒变了多少帧」+ 全体合计 + 录像本身的帧率。

判据：
    · 单块接近录像帧率、且等于我们推的 50fps ⇒ 这一路真的画出来了；
    · 单块明显低于 50（例如 30）⇒ 被 cancel 丢帧，桌面侧到顶（T_apply > 3.33ms）；
    · 注意：60/90/120Hz 屏上 50fps 必然是 vsync 交替（16.7/25ms 轮换），
      统计出来的「变化事件数/秒」应≈50，**不是**每帧都变。
"""

import argparse
import subprocess
import sys

import numpy as np


def probe_fps(path):
    try:
        out = subprocess.run(
            ["ffprobe", "-v", "error", "-select_streams", "v:0",
             "-show_entries", "stream=r_frame_rate,width,height",
             "-of", "default=nw=1", path],
            capture_output=True, text=True, check=True).stdout
    except Exception as e:
        print("ffprobe 失败：%s" % e)
        return None, 0, 0
    num, den, w, h = 0, 1, 0, 0
    for line in out.splitlines():
        if line.startswith("r_frame_rate="):
            a, b = line.split("=")[1].split("/")
            num, den = int(a), int(b)
        elif line.startswith("width="):
            w = int(line.split("=")[1])
        elif line.startswith("height="):
            h = int(line.split("=")[1])
    return (num / den if den else 0.0), w, h


def read_frames(path, w, h):
    """用 ffmpeg 把视频解成 rgb24 原始帧，逐帧给出（内存友好：一次一帧）。"""
    p = subprocess.Popen(
        ["ffmpeg", "-v", "error", "-i", path, "-f", "rawvideo", "-pix_fmt", "rgb24", "-"],
        stdout=subprocess.PIPE)
    n = w * h * 3
    try:
        while True:
            buf = p.stdout.read(n)
            if not buf or len(buf) < n:
                break
            yield np.frombuffer(buf, dtype=np.uint8).reshape(h, w, 3)
    finally:
        p.stdout.close()
        p.wait()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("video")
    ap.add_argument("--region", action="append", default=[],
                    help="x,y,w,h（可重复）")
    ap.add_argument("--thr", type=float, default=6.0,
                    help="判定「这一帧变了」的平均绝对差门限（默认 6/255）")
    ap.add_argument("--skip", type=float, default=0.5, help="开头跳过多少秒（拖动/弹出动画）")
    a = ap.parse_args()

    fps, w, h = probe_fps(a.video)
    if not fps:
        return 2
    regions = []
    for r in a.region:
        x, y, ww, hh = [int(v) for v in r.split(",")]
        regions.append((x, y, ww, hh))
    if not regions:
        regions = [(0, 0, w, h)]
    print("录像 %dx%d @ %.2ffps，区域 %d 块，门限 %.1f，跳过开头 %.1fs"
          % (w, h, fps, len(regions), a.thr, a.skip))

    prev = [None] * len(regions)
    events = [0] * len(regions)
    counted = 0
    start = int(round(a.skip * fps))
    idx = -1
    for fr in read_frames(a.video, w, h):
        idx += 1
        if idx < start:
            continue
        counted += 1
        for i, (x, y, ww, hh) in enumerate(regions):
            crop = fr[y:y + hh, x:x + ww].astype(np.int16)
            if prev[i] is None:
                prev[i] = crop
                continue
            d = np.abs(crop - prev[i]).mean()
            if d >= a.thr:
                events[i] += 1
                prev[i] = crop
    secs = counted / fps if fps else 0.0
    print("统计 %d 帧 / %.2f 秒" % (counted, secs))
    if secs <= 0:
        print("没读到帧")
        return 1
    total = 0
    for i, r in enumerate(regions):
        rate = events[i] / secs
        total += events[i]
        print("  区域%d %s → 变化 %.1f 次/秒（%d 次 / %.2fs）" % (i, r, rate, events[i], secs))
    print("合计变化 %.1f 次/秒（若 %d 路各 50fps，期望 %.0f 次/秒）"
          % (total / secs, len(regions), 50.0 * len(regions)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
