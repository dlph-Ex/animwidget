#!/usr/bin/env bash
# v1.3.7：素材选择器改成液态玻璃分段控件 + 编辑页滑块真·实时预览
#
# 用法：
#   ANDROID_SDK=/tmp/sdk KOTLIN_LIBS=/tmp/kl SHIZUKU_JAR=/tmp/shizuku-all.jar \
#     ./patch-v1.3.9/build.sh [versionCode] [versionName]
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="${ANDROID_SDK:-/tmp/sdk}"
KOTLIN_LIBS="${KOTLIN_LIBS:-/tmp/kl}"
SHIZUKU_JAR="${SHIZUKU_JAR:-/tmp/shizuku-all.jar}"
VERSION_CODE="${1:-72}"
VERSION_NAME="${2:-1.3.9}"

AJ="$SDK/android-34/android.jar"
D8_JAR="$SDK/android-14/lib/d8.jar"
APKSIGNER_JAR="$SDK/android-14/lib/apksigner.jar"
STDLIB="$KOTLIN_LIBS/kotlin-stdlib-1.9.24.jar"
ANNOT="$KOTLIN_LIBS/annotations-13.0.jar"
ASM="$ROOT/patch-v1.3.9/vendor/asm.jar"
AAPT2="${AAPT2:-aapt2}"

BASE_JAR="$ROOT/recovered/classes-v1.3.1.jar"
OUT="$(mktemp -d)"
KS="$ROOT/build-manual/debug.keystore"
SRC="$ROOT/patch-v1.3.9"

for f in "$AJ" "$D8_JAR" "$APKSIGNER_JAR" "$STDLIB" "$ANNOT" "$SHIZUKU_JAR" "$BASE_JAR" "$ASM"; do
    [[ -f "$f" ]] || { echo "缺少文件: $f" >&2; exit 1; }
done

echo "[1/8] aapt2: 编译资源"
rm -rf "$OUT/compiled.zip" "$OUT/res.apk"
(cd "$SRC" && "$AAPT2" compile --dir res -o "$OUT/compiled.zip")

echo "[2/8] aapt2: 链接资源（--stable-ids 保持资源 ID）"
(cd "$SRC" && "$AAPT2" link -o "$OUT/res.apk" -I "$AJ" \
    --manifest AndroidManifest.xml -R "$OUT/compiled.zip" \
    --stable-ids stable-ids.txt \
    --min-sdk-version 26 --target-sdk-version 35 \
    --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" --auto-add-overlay)

echo "[3/8] javac: 重编译补丁类"
mkdir -p "$OUT/patch_classes"
javac -nowarn -source 8 -target 8 -bootclasspath "$AJ" \
    -classpath "$BASE_JAR:$STDLIB:$ANNOT" -d "$OUT/patch_classes" \
    "$SRC/FrameComposer.java" "$SRC/ThemeManager.java" "$SRC/ThemePatch.java" \
    "$SRC/EdgeToEdge.java" "$SRC/MediaSelectorView.java" "$SRC/PreviewRenderer.java" "$SRC/FitAction.java"

echo "[4/8] ASM: SettingsActivity recreate → 原地换肤"
mkdir -p "$OUT/asm" "$OUT/base"
unzip -o -q "$BASE_JAR" 'io/github/animwidget/SettingsActivity.class' 'io/github/animwidget/ConfigActivity.class' -d "$OUT/base"
javac -nowarn -cp "$ASM" -d "$OUT/asm" "$SRC/Patch.java"
java -cp "$OUT/asm:$ASM" Patch "$OUT/base/io/github/animwidget/SettingsActivity.class" \
    "$OUT/patch_classes/io/github/animwidget/SettingsActivity.class" settings

echo "[5/8] ASM: ConfigActivity 预览防抖 120/90ms → 0ms"
java -cp "$OUT/asm:$ASM" Patch "$OUT/base/io/github/animwidget/ConfigActivity.class" \
    "$OUT/patch_classes/io/github/animwidget/ConfigActivity.class" delays

echo "[6/8] 重组 classes.jar"
python3 - "$OUT" <<'PY'
import sys, zipfile, glob, os
out = sys.argv[1]
src = 'recovered/classes-v1.3.1.jar'
dst = out + '/classes_patched.jar'
zin = zipfile.ZipFile(src)
zout = zipfile.ZipFile(dst, 'w', zipfile.ZIP_DEFLATED)
skip = ('io/github/animwidget/FrameComposer',
        'io/github/animwidget/ThemeManager',
        'io/github/animwidget/EdgeToEdge',
        'io/github/animwidget/MediaSelectorView',
        'io/github/animwidget/PreviewRenderer',
        'io/github/animwidget/SettingsActivity.class',
        'io/github/animwidget/ConfigActivity.class')
for info in zin.infolist():
    if info.filename.startswith(skip):
        continue
    zout.writestr(info, zin.read(info.filename))
n = 0
for path in glob.glob(out + '/patch_classes/io/github/animwidget/*.class'):
    zout.write(path, 'io/github/animwidget/' + os.path.basename(path))
    n += 1
zout.close()
print('  classes_patched.jar ok（替换/新增 %d 个类）' % n)
PY

echo "[7/8] d8: 生成 dex"
mkdir -p "$OUT/dex"
java -cp "$D8_JAR" com.android.tools.r8.D8 --release --min-api 26 --lib "$AJ" \
    --output "$OUT/dex" "$OUT/classes_patched.jar" "$STDLIB" "$SHIZUKU_JAR"
if [[ -f "$OUT/dex/classes2.dex" ]]; then
    echo "dex 被拆成多个文件" >&2
    exit 1
fi

echo "[8/8] 对齐打包 + 签名"
python3 "$ROOT/scripts/pack_apk.py" "$OUT/unsigned.apk" "$OUT/res.apk" "$OUT/dex/classes.dex"
mkdir -p "$ROOT/dist"
java -jar "$APKSIGNER_JAR" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
    --ks-key-alias androiddebugkey --out "$ROOT/dist/AnimWidget-v$VERSION_NAME.apk" "$OUT/unsigned.apk"

echo "--- 校验 ---"
java -jar "$APKSIGNER_JAR" verify --print-certs "$ROOT/dist/AnimWidget-v$VERSION_NAME.apk" | head -3
aapt dump badging "$ROOT/dist/AnimWidget-v$VERSION_NAME.apk" | head -2
ls -lh "$ROOT/dist/AnimWidget-v$VERSION_NAME.apk"
echo "build dir: $OUT"
