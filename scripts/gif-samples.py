#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 GIF 对拍样本（v1.10.498）。

覆盖分支：普通多帧 / 透明+dispose / 混合延时 / 单帧 / 大尺寸少帧 / 全 0 延时 / 200 帧长序列。
这些文件同时被 scripts/gifstream-selftest.sh 用来对拍
「GifStream.probe（流式）」与「GifFrames.info（整份 byte[]）」的口径。
"""
import os
import sys

from PIL import Image, ImageDraw


def frames(w, h, n, fn):
    out = []
    for i in range(n):
        im = Image.new("RGB", (w, h), (0, 0, 0))
        fn(ImageDraw.Draw(im), i, w, h)
        out.append(im.convert("P", palette=Image.ADAPTIVE))
    return out


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "/tmp/gifs"
    os.makedirs(out, exist_ok=True)

    # 1 普通多帧
    fs = frames(64, 48, 10, lambda d, i, w, h: d.rectangle([i * 5, 4, i * 5 + 20, 40], fill=(200, 40, 40)))
    fs[0].save(os.path.join(out, "simple.gif"), save_all=True, append_images=fs[1:], duration=100, loop=0)

    # 2 透明 + dispose
    fr = []
    for i in range(8):
        im = Image.new("RGBA", (40, 40), (0, 0, 0, 0))
        ImageDraw.Draw(im).ellipse([i * 3, 5, i * 3 + 15, 20], fill=(30, 200, 120, 255))
        fr.append(im.convert("P", palette=Image.ADAPTIVE))
    fr[0].save(os.path.join(out, "transparent.gif"), save_all=True, append_images=fr[1:],
               duration=70, loop=0, disposal=2)

    # 3 混合延时
    fr = [Image.new("RGB", (32, 32), (i * 20, 50, 80)).convert("P", palette=Image.ADAPTIVE)
          for i in range(6)]
    fr[0].save(os.path.join(out, "delays.gif"), save_all=True, append_images=fr[1:],
               duration=[50, 80, 110, 140, 170, 200], loop=0)

    # 4 单帧
    Image.new("RGB", (20, 20), (10, 20, 30)).convert("P", palette=Image.ADAPTIVE).save(
        os.path.join(out, "single.gif"))

    # 5 大尺寸少帧（用户素材 945x1224 同尺寸族）
    fr = frames(945, 1224, 3, lambda d, i, w, h: d.rectangle([0, i * 100, w, i * 100 + 300],
                                                             fill=(i * 60, 120, 240)))
    fr[0].save(os.path.join(out, "big.gif"), save_all=True, append_images=fr[1:], duration=40, loop=0)

    # 6 全 0 延时（延时表全靠 fillDelay 补）
    fr = frames(50, 50, 5, lambda d, i, w, h: d.ellipse([i * 4, 10, i * 4 + 20, 30], fill=(255, 255, 0)))
    fr[0].save(os.path.join(out, "zerodelay.gif"), save_all=True, append_images=fr[1:], duration=0, loop=0)

    # 7 长序列
    fr = frames(120, 90, 200, lambda d, i, w, h: d.rectangle([(i * 7) % 110, 10, (i * 7) % 110 + 18, 80],
                                                             fill=(i % 255, 80, 200)))
    fr[0].save(os.path.join(out, "long.gif"), save_all=True, append_images=fr[1:], duration=33, loop=0)


if __name__ == "__main__":
    main()
