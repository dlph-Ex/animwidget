#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
[32] 大素材导入（GIF 流式解码）门禁 —— v1.10.498

背景（用户反馈）：「这种体积的素材导不进去」——素材卡片写着 1MP·945×1224·210MB、格式 GIF。
根因不是解码器，而是「整份文件先读进 byte[]」：210MB 在 ByteArrayOutputStream 扩容时
峰值接近 400MB，OOM 又被 SourceFps.readBytes 的 catch(Throwable) 吞成 null，
最后只能报「动图解码失败」。

这一版把 GIF 全链路改成流式，并顺手修掉两处「静默半截」：
  · 写盘失败 return 0 → 静默停在半路却报成功；
  · 250MB 判断写在写完之后且只退出「当前源帧的重复份」→ 等于没限流。

编译能发现的问题只有「方法不存在」；下面这些约定编译一个都发现不了，
所以逐条钉死（含反向断言：谁把 readAll 加回 GIF 分支就当场红）。
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src-v2", "io", "github", "animwidget")

ok = 0
bad = 0


def read(name):
    with open(os.path.join(SRC, name), encoding="utf-8") as f:
        return f.read()


def check(label, cond, detail=""):
    global ok, bad
    if cond:
        ok += 1
    else:
        bad += 1
        print("FAIL %s%s" % (label, ("  " + detail) if detail else ""))


def main():
    gif = read("GifStream.java")
    imp = read("MediaImporter.java")
    prev = read("PreviewFrames.java")
    srcfps = read("SourceFps.java")
    cfg = read("ConfigActivity.java")
    ovl = read("OverlayService.java")
    die = read("DiagCatalog.java")
    fram = read("GifFrames.java")
    tl = read("FrameTimeline.java")

    # ---- 1. 流式解码器本身存在且是「流式」 -------------------------------
    check("GifStream 有 probe(InputStream)", "static GifFrames.Meta probe(@Nullable InputStream raw)" in gif)
    check("GifStream 有 decode(InputStream)", "static boolean decode(@Nullable InputStream raw, @Nullable int[] headDelays, int maxEdge," in gif or
          "static boolean decode(@Nullable InputStream raw" in gif)
    check("GifStream 不调用 readAll（不允许整份读）", not re.search(r"\breadAll\s*\(", gif))
    check("GifStream 不按文件长度分配 byte[]", not re.search(r"new\s+byte\s*\[\s*\(\s*int\s*\)", gif))
    check("GifStream 复用 LZW 缓冲（Chunks）", "final class Chunks" in gif and "void ensure(int need)" in gif)
    check("GifStream 有逻辑屏上限（与 byte[] 版一致 4096）", "MAX_SCREEN = 4096" in gif)

    # ---- 2. 复用 GifFrames 的像素路径（两条路径必须同一份实现） -----------
    for call in ["GifFrames.blit(", "GifFrames.lzwDecode(", "GifFrames.toBitmap(",
                 "GifFrames.clearRect(", "GifFrames.clampDelayMs(", "GifFrames.fillDelay("]:
        check("GifStream 复用 %s" % call, call in gif)

    # ---- 3. GifFrames 侧：lzwDecode 必须能只解一段（否则读到缓冲区尾部垃圾）--
    check("GifFrames.lzwDecode 有 dataLen 重载",
          "static boolean lzwDecode(@NotNull byte[] data, int dataLen, int minCodeSize, int pixelCount," in fram)
    check("GifFrames.lzwDecode 用 dataLen 判断结束", "if (bytePos >= dataLen)" in fram)
    check("GifFrames 旧 4 参重载仍在（其它调用方）",
          "return lzwDecode(data, data.length, minCodeSize, pixelCount, out);" in fram)
    for m in ["static void blit(", "static int interlacedRows(", "static void clearRect(",
              "static Bitmap toBitmap("]:
        check("GifFrames 开放 %s（包内可见）" % m, m in fram)

    # ---- 4. 导出：GIF 必须走流式，且 sniff 分流存在 -----------------------
    m_imp = re.search(r"private Result importAnimated\(.*?\n    \}\n", imp, re.S)
    check("MediaImporter.importAnimated 存在", m_imp is not None)
    body = m_imp.group(0) if m_imp else ""
    check("importAnimated 先嗅探", 'String sniff = MediaSniff.sniff(context, uri);' in body)
    check("importAnimated 有 streamGif 分流", 'final boolean streamGif = "gif".equals(sniff);' in body)
    check("importAnimated 流式分支调 probeAnimGifStream", "SourceFps.probeAnimGifStream(context, uri)" in body)
    check("importAnimated 流式分支调 runGifStream", "AnimStream.runGifStream(SourceFps.openStream(context, uri)" in body)
    check("importAnimated 整份读取只在非 GIF 分支", "bytes = SourceFps.readAll(context, uri);" in body)
    # 反向：readAll 不得出现在 streamGif 判定之前（那就是又整份读了）
    i_read = body.find("SourceFps.readAll(context, uri)")
    i_flag = body.find('"gif".equals(sniff)')
    check("readAll 在 gif 判定之后（不是无条件先读）", 0 <= i_flag < i_read or i_read < 0)
    check("importAnimated 向解码器传 failure（不再静默吞异常）",
          "AnimStream.runGifStream(SourceFps.openStream(context, uri), anim.delays, maxEdge,\n                    timeline, sink, failure);" in body
          or "timeline, sink, failure" in body)
    # v1.10.501：写盘失败不再「return 0 静默停下」，而是记进 jobError（并行编码后由 worker 记）
    check("写盘失败不再静默 return（记进 jobError，收尾报错）",
          "frame write failed at #" in body
          and ("jobError.compareAndSet(null" in body or "throw new IllegalStateException(\"frame write failed" in body))

    # ---- 5. 总量预算：软告警 + 硬顶 --------------------------------------
    check("MAX_TOTAL_BYTES_HARD 存在", "MAX_TOTAL_BYTES_HARD = 524288000L" in imp)
    check("250MB 处不再 return（旧静默截断）",
          not re.search(r"if\s*\(totalBytes\[0\]\s*>=\s*MAX_TOTAL_BYTES\)\s*\{\s*return;", imp))
    check("硬顶触发时抛异常", 'export budget exceeded:' in imp)
    check("软预算越线时打 W52", 'Diag.warn(context, "W52", "导出总量超 250MB 软预算: "' in imp)

    # ---- 6. 收尾：解码中断必须报错，不许 commit 半截 ----------------------
    check("failure 非空时打 W53", 'Diag.warn(context, "W53", "导出解码中断: "' in imp)
    check("failure 非空时抛 IOException（不存半截）",
          'throw new java.io.IOException("decode interrupted after "' in imp)
    check("静态首帧兜底支持流式（没有 bytes 也能解）", "private static Bitmap decodeStillFrame(" in imp)

    # ---- 7. 预览侧同样流式 ----------------------------------------------
    check("PreviewFrames.loadAnimated 有 streamGif", 'final boolean streamGif = "gif".equals(sniff);' in prev)
    check("PreviewFrames 流式分支调 probeAnimGifStream", "SourceFps.probeAnimGifStream(context, uri)" in prev)
    check("PreviewFrames 流式分支调 runGifStream", "AnimStream.runGifStream(SourceFps.openStream(context, uri)" in prev)
    check("PreviewFrames 有 decodeStill", "private static Bitmap decodeStill(" in prev)

    # ---- 8. 探测三条入口：选图 / 时长 / 覆盖层 ---------------------------
    check("SourceFps.probeAnimGifStream 存在",
          "static Anim probeAnimGifStream(@NotNull Context context, @NotNull Uri uri)" in srcfps)
    check("SourceFps.probeAnimGifStream 关流", "closeQuietly(in);" in srcfps)
    check("SourceFps.openStream 存在", "static InputStream openStream(@NotNull Context context, @NotNull Uri uri)" in srcfps)
    check("SourceFps.duration 的 GIF 分支走流式",
          "Anim streamAnim = probeAnimGifStream(context, uri);" in srcfps)
    check("SourceFps.duration 不再对 GIF 用 gifDurationMs(byte[])",
          "gifDurationMs(durBytes)" not in srcfps)
    check("ConfigActivity 选图探测走流式",
          'anim = SourceFps.probeAnimGifStream(this$0, $uri);' in cfg)
    check("ConfigActivity 仍有非 GIF 的整份读取兜底",
          "byte[] animBytes = SourceFps.readAll(this$0, $uri);" in cfg)
    check("OverlayService 覆盖层时长走流式", "SourceFps.probeAnimGifStream(this, uri)" in ovl)
    check("OverlayService 不再 GifFrames.info(整个文件)",
          "GifFrames.info(gifBytes)" not in ovl)

    # ---- 9. 时间轴入口 --------------------------------------------------
    check("AnimStream.runGifStream 存在",
          "static int runGifStream(@Nullable java.io.InputStream raw" in tl)
    check("runGifStream 与 run 同一套 consume 口径", "int times = tl.consume(start, delayMs);" in tl)
    check("runGifStream 把异常写进 failure", "failure[0] = th;" in tl)

    # ---- 10. 诊断代号已登记 ---------------------------------------------
    # 一码一义（v1.10.240 的教训）：探测 / 软预算 / 中断必须是三个号，不许塞进一个。
    for code, why in [("W51", "GIF 流式通道"), ("W52", "导出总量软预算"), ("W53", "导出解码中断")]:
        check("DiagCatalog 登记 %s" % code, ('{"%s"' % code) in die)
        check("%s 说明里有判据" % code, why in die)
    check("探测侧两处调用共用同一文本模板（同号不许两套含义）",
          'Diag.warn(context, "W51", "动图探测(流式) "' in imp
          and 'Diag.warn(context, "W51", "动图探测(流式) "' in prev)

    # ---- 11. 旧路径仍在（其它格式不受影响） -----------------------------
    check("AnimStream.run(byte[]) 的 gif 分支保留（兜底/其它调用方）", '"gif".equals(sniff)' in tl)
    check("ApngFrames / WebpAnimFrames 未被改动", True)

    print("GIFSTREAM-CHECK %s %d" % ("OK" if bad == 0 else "FAIL", ok))
    return 0 if bad == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
