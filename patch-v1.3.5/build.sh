#!/usr/bin/env bash
# v1.3.3 补丁构建：在 v1.3.1 的 classes.jar / res.apk 基础上：
#   - 重编译 FrameComposer（阴影等比留白）、ThemeManager（原地重绘补页面底色）、ThemePatch（原地换肤入口）
#   - 用 ASM 把 SettingsActivity 里 5 处「切主题后 recreate()」改成 ThemePatch.inPlaceRefresh
#   - 资源二进制补丁：versionCode/Name、配置页写死背景改透明（configChanges 保持原样，不再触发重建）
#
# 用法：
#   ANDROID_SDK=/tmp/sdk KOTLIN_LIBS=/tmp/kl SHIZUKU_JAR=/tmp/shizuku-all.jar \
#     ./patch-v1.3.5/build.sh [versionCode] [versionName]
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="${ANDROID_SDK:-/tmp/sdk}"
KOTLIN_LIBS="${KOTLIN_LIBS:-/tmp/kl}"
SHIZUKU_JAR="${SHIZUKU_JAR:-/tmp/shizuku-all.jar}"
VERSION_CODE="${1:-68}"
VERSION_NAME="${2:-1.3.5}"

AJ="$SDK/android-34/android.jar"
D8_JAR="$SDK/android-14/lib/d8.jar"
APKSIGNER_JAR="$SDK/android-14/lib/apksigner.jar"
STDLIB="$KOTLIN_LIBS/kotlin-stdlib-1.9.24.jar"
ANNOT="$KOTLIN_LIBS/annotations-13.0.jar"
ASM="$ROOT/patch-v1.3.5/vendor/asm.jar"

BASE_JAR="$ROOT/recovered/classes-v1.3.1.jar"
BASE_RES="$ROOT/recovered/res-v1.3.1.apk"
OUT="$(mktemp -d)"
KS="$ROOT/build-manual/debug.keystore"
SRC="$ROOT/patch-v1.3.5"

for f in "$AJ" "$D8_JAR" "$APKSIGNER_JAR" "$STDLIB" "$ANNOT" "$SHIZUKU_JAR" "$BASE_JAR" "$BASE_RES" "$ASM"; do
    [[ -f "$f" ]] || { echo "缺少文件: $f" >&2; exit 1; }
done

echo "[1/6] javac: 重编译 FrameComposer / ThemeManager / ThemePatch"
mkdir -p "$OUT/patch_classes"
javac -nowarn -source 8 -target 8 -bootclasspath "$AJ" \
    -classpath "$BASE_JAR:$STDLIB:$ANNOT" -d "$OUT/patch_classes" \
    "$SRC/FrameComposer.java" "$SRC/ThemeManager.java" "$SRC/ThemePatch.java"

echo "[2/6] ASM: SettingsActivity 的 recreate() → 原地换肤"
mkdir -p "$OUT/asm"
unzip -o -q "$BASE_JAR" 'io/github/animwidget/SettingsActivity.class' -d "$OUT/base"
javac -nowarn -cp "$ASM" -d "$OUT/asm" "$SRC/Patch.java"
java -cp "$OUT/asm:$ASM" Patch "$OUT/base/io/github/animwidget/SettingsActivity.class" \
    "$OUT/patch_classes/io/github/animwidget/SettingsActivity.class"

echo "[3/6] 重组 classes.jar"
python3 - "$OUT" <<'PY'
import sys, zipfile, glob, os
out = sys.argv[1]
src = 'recovered/classes-v1.3.1.jar'
dst = out + '/classes_patched.jar'
zin = zipfile.ZipFile(src)
zout = zipfile.ZipFile(dst, 'w', zipfile.ZIP_DEFLATED)
skip_prefix = ('io/github/animwidget/FrameComposer',
               'io/github/animwidget/ThemeManager',
               'io/github/animwidget/SettingsActivity.class')
for info in zin.infolist():
    if info.filename.startswith(skip_prefix):
        continue
    zout.writestr(info, zin.read(info.filename))
added = 0
for path in glob.glob(out + '/patch_classes/io/github/animwidget/*.class'):
    name = 'io/github/animwidget/' + os.path.basename(path)
    zout.write(path, name)
    added += 1
zout.close()
print('  classes_patched.jar ok（替换/新增 %d 个类）' % added)
PY

echo "[4/6] 资源二进制补丁（versionCode/Name + 配置页背景透明，configChanges 不动）"
(cd "$SRC" && python3 patch_axml.py "$BASE_RES" "$OUT/res_patched.apk" \
    --code "$VERSION_CODE" --name "$VERSION_NAME" --config-changes keep)

echo "[5/6] d8: 生成 dex（未 R8 压缩）"
mkdir -p "$OUT/dex"
java -cp "$D8_JAR" com.android.tools.r8.D8 --release --min-api 26 --lib "$AJ" \
    --output "$OUT/dex" "$OUT/classes_patched.jar" "$STDLIB" "$SHIZUKU_JAR"
if [[ -f "$OUT/dex/classes2.dex" ]]; then
    echo "dex 被拆成多个文件，pack_apk.py 只支持单 dex" >&2
    exit 1
fi

echo "[6/6] 对齐打包 + 签名"
python3 "$ROOT/scripts/pack_apk.py" "$OUT/unsigned.apk" "$OUT/res_patched.apk" "$OUT/dex/classes.dex"
mkdir -p "$ROOT/dist"
java -jar "$APKSIGNER_JAR" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
    --ks-key-alias androiddebugkey --out "$ROOT/dist/AnimWidget-v$VERSION_NAME.apk" "$OUT/unsigned.apk"

echo "--- 校验 ---"
java -jar "$APKSIGNER_JAR" verify --print-certs "$ROOT/dist/AnimWidget-v$VERSION_NAME.apk" | head -3
aapt dump badging "$ROOT/dist/AnimWidget-v$VERSION_NAME.apk" | head -2
ls -lh "$ROOT/dist/AnimWidget-v$VERSION_NAME.apk"
echo "build dir: $OUT"
