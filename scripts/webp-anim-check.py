#!/usr/bin/env python3
# v1.10.455：动图 WebP 封装器的端到端自检（Pillow 逐帧对像素与时长）。
#
# 为什么必须是"端到端"：这个封装器的正确性全在二进制细节里（chunk 顺序、24 位小端、
# padding、ANMF flags 的 bit 位置）。写错一位，文件仍然"能保存"，但解码器要么报错、
# 要么从第二帧开始全花 —— 靠读代码是看不出来的。所以这里做三件事：
#   1) 用 Pillow 生成 N 个真 WebP 单帧（有损 + 无损两条路都要覆盖，ALPH/VP8/VP8L 各走一遍）；
#   2) 调 Java 封装器把它们拼成一个动图 WebP；
#   3) 用 Pillow 解码拼出来的文件，逐帧比对**像素**（和原单帧解码结果逐字节相等）与**时长**。
#
# 输出最后一行固定是 "WEBPANIM OK <断言数>"，供 verify-apk.sh 判定。
import array
import os
import subprocess
import sys
import tempfile

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
W = H = 64
N = 8
FPS_X10 = 500          # 50.0fps → 20ms/帧
DUR_MS = 10000 // FPS_X10

ok = 0
bad = 0


def check(cond, what):
    global ok, bad
    if cond:
        ok += 1
    else:
        bad += 1
        print("  FAIL: " + what)


def make_frames(outdir, lossless):
    """生成 N 帧 RGBA：圆角蒙版 + 移动的方块 + 半透明阴影（= 我们烘焙帧的真实形态）。"""
    frames = []
    for i in range(N):
        img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        # 半透明阴影（逼出 ALPH）
        d.rounded_rectangle([4, 4, W - 4, H - 4], radius=14, fill=(0, 0, 0, 90))
        # 不透明内容
        d.rounded_rectangle([10, 10, W - 10, H - 10], radius=10,
                            fill=(30 + i * 20, 120, 200 - i * 15, 255))
        x = 14 + i * 5
        d.rectangle([x, 24, x + 10, 40], fill=(255, 240, 60, 255))
        path = os.path.join(outdir, "f%05d.webp" % i)
        if lossless:
            img.save(path, "WEBP", lossless=True, quality=100, method=4)
        else:
            img.save(path, "WEBP", quality=90, method=4)
        frames.append((path, img))
    return frames


def run_case(work, cls, lossless):
    tag = "lossless" if lossless else "lossy"
    src = os.path.join(work, tag)
    os.makedirs(src, exist_ok=True)
    frames = make_frames(src, lossless)
    out = os.path.join(work, tag + ".webp")
    cmd = ["java", "-cp", cls, "io.github.animwidget.WebpAnimWriterTest",
           out, str(FPS_X10)] + [p for p, _ in frames]
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        print(r.stdout.strip())
        print(r.stderr.strip()[:800])
        check(False, tag + " 封装器返回失败")
        return
    check(True, tag + " 封装器成功")
    check(os.path.getsize(out) > 100, tag + " 输出文件为空")

    # ---- 用 Pillow 解码拼出来的文件（= 第三方解码器的独立判断）
    im = Image.open(out)
    check(im.format == "WEBP", tag + " 不是 WEBP")
    check(getattr(im, "n_frames", 1) == N, tag + " 帧数=%s（期望 %d）" % (getattr(im, "n_frames", 1), N))
    check(im.size == (W, H), tag + " 画布=%s（期望 %s）" % (im.size, (W, H)))

    # 原单帧的基准解码结果（用同一个解码器解单帧，避免编解码差异干扰判断）
    base = [Image.open(p).convert("RGBA") for p, _ in frames]
    for i in range(N):
        im.seek(i)
        cur = im.convert("RGBA")
        same = cur.tobytes() == base[i].tobytes()
        check(same, tag + " 第 %d 帧像素与原帧不一致" % i)
        dur = im.info.get("duration", 0)
        check(abs(dur - DUR_MS) <= 1, tag + " 第 %d 帧时长=%s（期望 %d）" % (i, dur, DUR_MS))
    check(im.info.get("loop", 0) == 0, tag + " 循环次数=%s（期望 0=无限）" % im.info.get("loop", 0))


def check_decoder_construction(muxed, ref_frame0):
    """v1.10.456：按 App 内 WebpAnimFrames.decodeFrame 的**新逻辑**重建最小容器并解码。

    这一组断言盯的是真机日志里的「P10 动图: 解码出 0 帧」：
    旧代码把 ANMF 子块解析错了（把长度字段当成位流起点），而且重建最小 WebP 时
    丢掉了 ALPH 块。这里用同样的算法（子块遍历 → VP8X+ALPH+VP8）重建一次，
    用第三方解码器验证像素与原帧一致。
    """
    import struct
    d = open(muxed, "rb").read()
    pos = 12
    anmf = None
    while pos + 8 <= len(d):
        cc = d[pos:pos + 4]
        size = struct.unpack("<I", d[pos + 4:pos + 8])[0]
        if cc == b"ANMF":
            anmf = (pos + 8, size)
            break
        pos += 8 + size + (size & 1)
    check(anmf is not None, "封装结果里找不到 ANMF")
    if not anmf:
        return
    start, size = anmf
    w = int.from_bytes(d[start + 6:start + 9], "little") + 1
    h = int.from_bytes(d[start + 9:start + 12], "little") + 1
    flags = d[start + 15]
    # v1.10.476：位序按 libwebp 的 muxread.c 更正 —— bit0 = dispose(1=置背景)，
    # bit1 = blend(1=不与上一帧合成)。所以「不混合、不处置」= 0x02（旧断言写反了）。
    check((flags & 0x01) == 0, "ANMF flags bit0(dispose) 应为 0，实际 0x%02x" % flags)
    check(((flags >> 1) & 0x01) == 1, "ANMF flags bit1(blend) 应为 1（不混合），实际 0x%02x" % flags)
    end = start + size
    p = start + 16
    alpha = None
    bits = None
    seen = []
    while p + 8 <= end:
        cc = d[p:p + 4]
        sz = struct.unpack("<I", d[p + 4:p + 8])[0]
        total = 8 + sz + (sz & 1)
        seen.append(cc)
        if cc == b"ALPH":
            alpha = d[p:p + total]
        elif cc in (b"VP8 ", b"VP8L"):
            bits = d[p:p + total]
            break
        p += total
    check(seen[:1] == [b"ALPH"], "有损帧的第一个子块应为 ALPH，实际 %s" % seen[:1])
    check(bits is not None and bits[:4] == b"VP8 ", "ALPH 之后应能找到 VP8 位流块")
    if not bits or not alpha:
        return
    vp8x = (b"VP8X" + struct.pack("<I", 10) + bytes([0x10, 0, 0, 0])
            + (w - 1).to_bytes(3, "little") + (h - 1).to_bytes(3, "little"))
    body = b"WEBP" + vp8x + alpha + bits
    mini = os.path.join(os.path.dirname(muxed), "mini.webp")
    open(mini, "wb").write(b"RIFF" + struct.pack("<I", len(body)) + body)
    im = Image.open(mini).convert("RGBA")
    base = Image.open(ref_frame0).convert("RGBA")
    check(im.size == base.size, "重建最小容器尺寸不符 %s vs %s" % (im.size, base.size))
    check(im.tobytes() == base.tobytes(), "重建最小容器像素与原帧不一致（alpha 丢了？）")


def main():
    work = tempfile.mkdtemp(prefix="webpanim-")
    cls = os.path.join(work, "cls")
    os.makedirs(cls, exist_ok=True)
    src = [os.path.join(ROOT, "src-v2/io/github/animwidget/WebpAnimWriter.java"),
           os.path.join(ROOT, "tests/WebpAnimWriterTest.java")]
    r = subprocess.run(["javac", "-nowarn", "-source", "8", "-target", "8", "-d", cls] + src,
                       capture_output=True, text=True)
    if r.returncode != 0:
        print(r.stderr[:1500])
        print("WEBPANIM BAD 1")
        return 1
    run_case(work, cls, False)
    run_case(work, cls, True)
    check_decoder_construction(os.path.join(work, "lossy.webp"),
                               os.path.join(work, "lossy", "f00000.webp"))

    # 附加：单帧 WebP 的块序（ALPH 必须在 VP8 之前）—— 我们拼容器时依赖这条
    try:
        im = Image.open(os.path.join(work, "lossy", "f00000.webp"))
        check(im.mode in ("RGBA", "RGB"), "单帧 WEBP 可解")
    except Exception as e:  # noqa
        check(False, "单帧 WEBP 不可解: %s" % e)

    if bad == 0:
        print("WEBPANIM OK %d" % ok)
        return 0
    print("WEBPANIM BAD %d" % bad)
    return 1


if __name__ == "__main__":
    sys.exit(main())
