#!/usr/bin/env bash
# 日志两条承诺的**行为级**自检：30 分钟保留窗口（v1.10.439）+ 逐条渐显不重播（v1.10.440）。
#
# 为什么要有：这是用户唯一能直接观察到的"数据生命周期"承诺 ——
# 「只保留最近 30 分钟（含崩溃记录），到时间自动清除，重启不重置」。
# 436 起我连着三版都在改版式，没人验过这个承诺，结果真漏了三处（见 LESSONS 180）。
#
# 做法：把真正的 LogBook.java 和探针一起编译，用反射跑 prune/undo 的真实路径
# （file IO 传 null context 自动跳过），逐条断言：按条目时间戳过期、30:00 边界、
# 时钟回调、清屏快照同 TTL、撤回不续命、过期快照撤回不 NPE。
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TC="$ROOT/toolchain"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

javac -nowarn -source 8 -target 8 \
  -bootclasspath "$TC/android-all.jar:$TC/jdk-core-stubs.jar:$TC/core-lambda-stubs.jar" \
  -classpath "$TC/kotlin-stdlib.jar:$TC/annotations-13.0.jar" \
  -d "$OUT" \
  "$ROOT/src-v2/io/github/animwidget/LogBook.java" \
  "$ROOT/src-v2/io/github/animwidget/LogAlign.java" \
  "$ROOT/tests/LogBookTtlProbe.java" \
  "$ROOT/tests/LogAlignProbe.java" || exit 1

rc=0
java -cp "$OUT:$TC/android-all.jar:$TC/kotlin-stdlib.jar:$TC/annotations-13.0.jar" \
  io.github.animwidget.LogBookTtlProbe || rc=1
java -cp "$OUT:$TC/android-all.jar:$TC/kotlin-stdlib.jar:$TC/annotations-13.0.jar" \
  io.github.animwidget.LogAlignProbe || rc=1
exit $rc
