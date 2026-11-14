#!/usr/bin/env bash
# 没有 Android Studio / Gradle 时的手工打包脚本（aarch64 Linux 也可用）。
#
# 依赖：
#   - JDK 17（javac / jar / keytool / java）
#   - python3
#   - aapt（Ubuntu: apt install aapt）
#   - Android SDK 解出的 platform android.jar 与 build-tools 的 d8.jar / apksigner.jar
#   - Kotlin 编译器（kotlin-compiler-embeddable + 依赖 jar）
#
# 用法：
#   ANDROID_SDK=/tmp/sdk KOTLIN_LIBS=/tmp/kl ./scripts/build-apk.sh
#
# 环境变量：
#   ANDROID_SDK   含 android-34/android.jar 与 android-14/lib/d8.jar 的目录
#   KOTLIN_LIBS   含 kotlin-compiler-embeddable / kotlin-stdlib 的目录
#   OUT           中间产物目录，默认 <repo>/build-manual
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="${ANDROID_SDK:-/tmp/sdk}"
KOTLIN_LIBS="${KOTLIN_LIBS:-/tmp/kl}"
SHIZUKU_JAR="${SHIZUKU_JAR:-/tmp/shizuku/shizuku-all.jar}"
OUT="${OUT:-$ROOT/build-manual}"

AJ="$SDK/android-34/android.jar"
D8_JAR="$SDK/android-14/lib/d8.jar"
APKSIGNER_JAR="$SDK/android-14/lib/apksigner.jar"
STDLIB="$KOTLIN_LIBS/kotlin-stdlib-1.9.24.jar"

for f in "$AJ" "$D8_JAR" "$APKSIGNER_JAR" "$STDLIB" "$SHIZUKU_JAR"; do
    if [[ ! -f "$f" ]]; then
        echo "缺少文件: $f" >&2
        exit 1
    fi
done

rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex" "$ROOT/dist"

# AGP 用 namespace 指定包名，而 aapt1 需要 manifest 里的 package 属性：构建时临时注入
sed 's|<manifest xmlns:android="http://schemas.android.com/apk/res/android">|<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="io.github.animwidget">|' \
    "$ROOT/app/src/main/AndroidManifest.xml" > "$OUT/AndroidManifest.xml"

echo "[1/6] aapt: 生成 R.java"
aapt package -f -m -J "$OUT/gen" -M "$OUT/AndroidManifest.xml" -S "$ROOT/app/src/main/res" \
    -I "$AJ" --min-sdk-version 26 --target-sdk-version 34 --version-code 43 --version-name 1.1.0

echo "[2/6] javac: 编译 R.java"
javac -source 8 -target 8 -nowarn -bootclasspath "$AJ" -d "$OUT/classes" \
    "$OUT/gen/io/github/animwidget/R.java"

echo "[3/6] kotlinc: 编译 Kotlin"
java -Xmx2g -cp "$KOTLIN_LIBS/*" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
    -no-stdlib -jvm-target 1.8 \
    -classpath "$AJ:$STDLIB:$SHIZUKU_JAR:$OUT/classes" \
    -d "$OUT/classes" "$ROOT/app/src/main/java"

echo "[4/6] aapt: 打包资源"
aapt package -f -M "$OUT/AndroidManifest.xml" -S "$ROOT/app/src/main/res" -I "$AJ" \
    -F "$OUT/res.apk" --min-sdk-version 26 --target-sdk-version 34 \
    --version-code 43 --version-name 1.1.0

echo "[5/6] d8: 生成 dex"
(cd "$OUT/classes" && jar cf "$OUT/classes.jar" .)
java -cp "$D8_JAR" com.android.tools.r8.D8 --release --min-api 26 --lib "$AJ" \
    --output "$OUT/dex" "$OUT/classes.jar" "$STDLIB" "$SHIZUKU_JAR"

echo "[6/6] 对齐打包 + 签名"
python3 "$ROOT/scripts/pack_apk.py" "$OUT/unsigned.apk" "$OUT/res.apk" "$OUT/dex/classes.dex"
KS="$OUT/debug.keystore"
if [[ ! -f "$KS" ]]; then
    keytool -genkeypair -keystore "$KS" -storepass android -keypass android \
        -alias androiddebugkey -dname "CN=Android Debug,O=Android,C=US" \
        -keyalg RSA -keysize 2048 -validity 10000 >/dev/null 2>&1
fi
java -jar "$APKSIGNER_JAR" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
    --ks-key-alias androiddebugkey --out "$ROOT/dist/AnimWidget-debug.apk" "$OUT/unsigned.apk"

echo "--- 校验 ---"
java -jar "$APKSIGNER_JAR" verify --print-certs "$ROOT/dist/AnimWidget-debug.apk" | head -3
aapt dump badging "$ROOT/dist/AnimWidget-debug.apk" | head -5
ls -lh "$ROOT/dist/AnimWidget-debug.apk"
