#!/usr/bin/env bash
# GIF 流式解码自检（v1.10.498）：
#   用同一批 GIF 分别走
#     A) GifFrames.info(byte[])  —— 老的整份 byte[] 路径
#     B) GifStream.probe(InputStream) —— 新的流式路径
#   逐字段对拍：尺寸 / 帧数 / 总时长 / estimated / filled / 每一帧延时。
#
# 这两条路径必须完全一致：只要差一帧或差 10ms，预览与导出的时间轴就会分叉。
# 需要：javac、python3+PIL（没有 PIL 就跳过，不算失败）。
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TC="$ROOT/toolchain"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

python3 - "$TMP/gifs" <<'PY' || true
PY

if ! python3 -c "import PIL" >/dev/null 2>&1; then
  echo "GIFSTREAM-SELFTEST SKIP 没装 PIL"
  exit 0
fi

mkdir -p "$TMP/gifs" "$TMP/cls"
python3 "$ROOT/scripts/gif-samples.py" "$TMP/gifs"

javac -nowarn -source 8 -target 8 \
  -bootclasspath "$TC/android-all.jar:$TC/jdk-core-stubs.jar:$TC/core-lambda-stubs.jar" \
  -classpath "$TC/kotlin-stdlib.jar:$TC/annotations-13.0.jar:$TC/shizuku-all.jar:$TC/shizuku-aidl.jar" \
  -d "$TMP/cls" \
  $(find "$ROOT/src-v2" -name '*.java') "$ROOT/toolchain/GifStreamCheck.java" >/dev/null 2>&1

java -cp "$TMP/cls:$TC/android-all.jar:$TC/kotlin-stdlib.jar:$TC/annotations-13.0.jar" \
  io.github.animwidget.GifStreamCheck "$TMP/gifs"
