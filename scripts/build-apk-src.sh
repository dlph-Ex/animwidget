#!/usr/bin/env bash
# 从 src-v2（自维护源码）直接出包，不再依赖 recovered/classes-v1.3.1.jar。
# 用法: ANDROID_SDK=/tmp/sdk KOTLIN_LIBS=/tmp/kl SHIZUKU_JAR=/tmp/shizuku-all.jar \
#         ./scripts/build-apk-src.sh [versionCode] [versionName]
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# v1.8.10 起：工具链固化在 toolchain/（/tmp 经常被清空，不再依赖）
TC="${TOOLCHAIN:-$ROOT/toolchain}"
VC="${1:-74}"; VN="${2:-1.5.0}"
# AJ：robolectric android-all（API 34 完整框架，含 res/arsc；javac/aapt2 -I/d8 --lib 通用）
# JDKSTUBS：JDK17 抽出的 java/javax 存根（android-all 刻意不含 java.**）
AJ="$TC/android-all.jar"; JDKSTUBS="$TC/jdk-core-stubs.jar"
D8="$TC/r8.jar"; APKSIG="$TC/apksig.jar"; SIGNCLI="$TC/signapk-cli.jar"
STD="$TC/kotlin-stdlib.jar"; ANN="$TC/annotations-13.0.jar"; SHIZUKU_JAR="$TC/shizuku-all.jar"
# v1.8.13：AIDL 存根（api/provider 构件都不含，缺了进程在 Shizuku 推 binder 时秒崩）
AIDL="$TC/shizuku-aidl.jar"
STUBS="$TC/core-lambda-stubs.jar"
RES="$ROOT/patch-v1.4.0/res"; MAN="$ROOT/patch-v1.4.0/AndroidManifest.xml"; IDS="$ROOT/patch-v1.4.0/stable-ids.txt"
OUT="$(mktemp -d)"; KS="$ROOT/build-manual/debug.keystore"; AAPT2="${AAPT2:-aapt2}"

echo "[1/5] javac: 编译 src-v2"
mkdir -p "$OUT/classes"
javac -nowarn -source 8 -target 8 -bootclasspath "$AJ:$JDKSTUBS:$STUBS" \
  -classpath "$STD:$ANN:$SHIZUKU_JAR:$AIDL" -d "$OUT/classes" $(find "$ROOT/src-v2" -name '*.java')

echo "[2/5] aapt2: 资源"
(cd "$RES/.." && "$AAPT2" compile --dir res -o "$OUT/res.zip")
(cd "$RES/.." && "$AAPT2" link -o "$OUT/res.apk" -I "$AJ" --manifest "$MAN" -R "$OUT/res.zip" \
   --stable-ids "$IDS" --min-sdk-version 26 --target-sdk-version 35 \
   --version-code "$VC" --version-name "$VN" --auto-add-overlay)

# v1.10.356：**OBFUSCATE=1 时改走 R8 混淆**（默认仍是 d8，保证随时能回退到未混淆包）。
# 混淆规则在 scripts/r8-rules.pro：只改名、不裁剪、不优化（-dontshrink -dontoptimize），
# 因为「裁剪」会把反射/系统调用的代码当成无用代码删掉，而用户只有一台手机、测不出来。
if [ "${OBFUSCATE:-0}" = "1" ]; then
  echo "[3/5] r8: dex (obfuscated)"
  (cd "$OUT/classes" && jar cf "$OUT/classes.jar" .)
  mkdir -p "$OUT/dex"
  # ⚠️ 这里 `$STD`(kotlin-stdlib) / `$SHIZUKU_JAR` / `$AIDL` 必须作为 **input**，
  # 不能放在 `--lib`。`--lib` 是「只用来做类型推断、不打包」——放错的后果是
  # APK 里根本没有 kotlin 运行时，Kotlin 编译出来的每一行 Intrinsics.checkNotNullParameter
  # 都会 NoClassDefFoundError，**一启动就崩**（v1.10.356 就是这个错误）。
  # `--lib` 只留「本机不会打包」的东西：android-all / jdk stubs / lambda stubs。
  java -cp "$D8" com.android.tools.r8.R8 --release --min-api 26 \
     --lib "$AJ" --lib "$JDKSTUBS" --lib "$STUBS" --lib "$ANN" \
     --output "$OUT/dex" --pg-conf "$ROOT/scripts/r8-rules.pro" \
     "$OUT/classes.jar" "$STD" "$SHIZUKU_JAR" "$AIDL"
else
  echo "[3/5] d8: dex"
  (cd "$OUT/classes" && jar cf "$OUT/classes.jar" .)
  mkdir -p "$OUT/dex"
  java -cp "$D8" com.android.tools.r8.D8 --release --min-api 26 --lib "$AJ" --lib "$JDKSTUBS" --output "$OUT/dex" \
     "$OUT/classes.jar" "$STD" "$SHIZUKU_JAR" "$AIDL"
fi

echo "[4/5] pack + sign"
mkdir -p "$ROOT/dist"
python3 "$ROOT/scripts/pack_apk.py" "$OUT/unsigned.apk" "$OUT/res.apk" "$OUT/dex/classes.dex"
java -cp "$APKSIG:$SIGNCLI" SignApk sign "$KS" android android androiddebugkey \
   "$OUT/unsigned.apk" "$ROOT/dist/AnimWidget-v$VN.apk"

echo "[5/5] verify"
java -cp "$APKSIG:$SIGNCLI" SignApk verify "$ROOT/dist/AnimWidget-v$VN.apk"
aapt dump badging "$ROOT/dist/AnimWidget-v$VN.apk" | head -2
ls -lh "$ROOT/dist/AnimWidget-v$VN.apk"; echo "build dir: $OUT"
