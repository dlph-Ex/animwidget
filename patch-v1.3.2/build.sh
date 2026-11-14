#!/usr/bin/env bash
# v1.3.2 补丁构建：v1.3.1 的 classes.jar + res.apk 做底，重编译 FrameComposer，改资源二进制，D8 打包签名。
#
# 用法：
#   ANDROID_SDK=/tmp/sdk KOTLIN_LIBS=/tmp/kl SHIZUKU_JAR=/tmp/shizuku-all.jar \
#     ./patch-v1.3.2/build.sh [versionCode] [versionName]
#
# 默认 65 / 1.3.2。产物写到 dist/AnimWidget-v<versionName>.apk。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="${ANDROID_SDK:-/tmp/sdk}"
KOTLIN_LIBS="${KOTLIN_LIBS:-/tmp/kl}"
SHIZUKU_JAR="${SHIZUKU_JAR:-/tmp/shizuku-all.jar}"
VERSION_CODE="${1:-65}"
VERSION_NAME="${2:-1.3.2}"

AJ="$SDK/android-34/android.jar"
D8_JAR="$SDK/android-14/lib/d8.jar"
APKSIGNER_JAR="$SDK/android-14/lib/apksigner.jar"
STDLIB="$KOTLIN_LIBS/kotlin-stdlib-1.9.24.jar"
ANNOT="$KOTLIN_LIBS/annotations-13.0.jar"

BASE_JAR="$ROOT/recovered/classes-v1.3.1.jar"
BASE_RES="$ROOT/recovered/res-v1.3.1.apk"
OUT="$(mktemp -d)"
KS="$ROOT/build-manual/debug.keystore"

for f in "$AJ" "$D8_JAR" "$APKSIGNER_JAR" "$STDLIB" "$ANNOT" "$SHIZUKU_JAR" "$BASE_JAR" "$BASE_RES"; do
    [[ -f "$f" ]] || { echo "缺少文件: $f" >&2; exit 1; }
done

echo "[1/5] javac: 重编译 FrameComposer"
mkdir -p "$OUT/patch_classes"
javac -nowarn -source 8 -target 8 -bootclasspath "$AJ" \
    -classpath "$BASE_JAR:$STDLIB:$ANNOT" \
    -d "$OUT/patch_classes" "$ROOT/patch-v1.3.2/FrameComposer.java"

echo "[2/5] 替换 classes.jar 中的 FrameComposer*"
python3 - "$OUT" <<'PY'
import sys, zipfile
out = sys.argv[1]
src = 'recovered/classes-v1.3.1.jar'
dst = out + '/classes_patched.jar'
new = {
    'io/github/animwidget/FrameComposer.class': out + '/patch_classes/io/github/animwidget/FrameComposer.class',
    'io/github/animwidget/FrameComposer$WhenMappings.class': out + '/patch_classes/io/github/animwidget/FrameComposer$WhenMappings.class',
    'io/github/animwidget/FrameComposer$AnonymousClass1.class': out + '/patch_classes/io/github/animwidget/FrameComposer$AnonymousClass1.class',
}
zin = zipfile.ZipFile(src)
zout = zipfile.ZipFile(dst, 'w', zipfile.ZIP_DEFLATED)
for info in zin.infolist():
    if 'FrameComposer' in info.filename:
        continue
    zout.writestr(info, zin.read(info.filename))
for name, path in new.items():
    zout.write(path, name)
zout.close()
print('  classes_patched.jar ok')
PY

echo "[3/5] 资源二进制补丁（versionCode/Name、configChanges、配置页背景）"
(cd "$ROOT/patch-v1.3.2" && python3 patch_axml.py "$BASE_RES" "$OUT/res_patched.apk" \
    --code "$VERSION_CODE" --name "$VERSION_NAME")

echo "[4/5] d8: 生成 dex（未 R8 压缩）"
mkdir -p "$OUT/dex"
java -cp "$D8_JAR" com.android.tools.r8.D8 --release --min-api 26 --lib "$AJ" \
    --output "$OUT/dex" "$OUT/classes_patched.jar" "$STDLIB" "$SHIZUKU_JAR"
if [[ -f "$OUT/dex/classes2.dex" ]]; then
    echo "注意：dex 被拆成多个文件，pack_apk.py 只支持单 dex，请改用 R8 或减少输入" >&2
    exit 1
fi

echo "[5/5] 对齐打包 + 签名"
python3 "$ROOT/scripts/pack_apk.py" "$OUT/unsigned.apk" "$OUT/res_patched.apk" "$OUT/dex/classes.dex"
mkdir -p "$ROOT/dist"
java -jar "$APKSIGNER_JAR" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
    --ks-key-alias androiddebugkey --out "$ROOT/dist/AnimWidget-v$VERSION_NAME.apk" "$OUT/unsigned.apk"

echo "--- 校验 ---"
java -jar "$APKSIGNER_JAR" verify --print-certs "$ROOT/dist/AnimWidget-v$VERSION_NAME.apk" | head -3
aapt dump badging "$ROOT/dist/AnimWidget-v$VERSION_NAME.apk" | head -2
ls -lh "$ROOT/dist/AnimWidget-v$VERSION_NAME.apk"
echo "build dir: $OUT"
