#!/usr/bin/env bash
# 编译 src-v2（从 v1.3.1 字节码反编译 + 人工修复中的源码树）。
# 用法: ANDROID_SDK=/tmp/sdk KOTLIN_LIBS=/tmp/kl ./scripts/build-src.sh [--quiet]
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="${ANDROID_SDK:-/tmp/sdk}"
KL="${KOTLIN_LIBS:-/tmp/kl}"
SHIZUKU_JAR="${SHIZUKU_JAR:-/tmp/shizuku-all.jar}"
SRC="$ROOT/src-v2"
OUT="${OUT:-/tmp/src-out}"
LOG="${LOG:-/tmp/javac-src.log}"
mkdir -p "$OUT"
javac -Xmaxerrs 2000 -nowarn -source 8 -target 8 \
    -bootclasspath "$SDK/android-34/android.jar:$SDK/android-14/core-lambda-stubs.jar" \
    -classpath "$KL/kotlin-stdlib-1.9.24.jar:$KL/annotations-13.0.jar:$SHIZUKU_JAR" \
    -d "$OUT" $(find "$SRC" -name '*.java') 2>"$LOG"
rc=$?
echo "errors: $(grep -c 'error:' "$LOG" || true)"
grep 'error:' "$LOG" | sed 's|.*/animwidget/||; s|:.*||' | sort | uniq -c | sort -rn | head -15
exit $rc
