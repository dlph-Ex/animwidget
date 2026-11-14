#!/usr/bin/env bash
# v1.10.270：对比度颜色模型的脱机自检（不需要 Android 运行时）。
#
# 为什么单独有个脚本：这次 bug 是「一个 token 被画在两种层上」——
#   卡片层钉住、页面底层被推走，中性变体夹在中间没跟着走，
#   浅色 level=1 时两者只差 1.003:1，画在页面底上的胶囊/描边/分割线整体消失。
# 这种错误截图只能证明「某块看不见」，证明不了是哪一层出的问题，所以判据必须落在数字上。
#
# 关键：这里**现编 src-v2 里真的 Contrast.java**（用 toolchain/android-all.jar 当 android.graphics
# 的实现），不是把算法抄一份到测试里 —— 抄一份就等于门禁在另一套模型上跑（LESSONS 4/86）。
#
# 用法：scripts/theme-math-check.sh   （退出码 0 = 全部断言通过）
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TC="$ROOT/toolchain"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

SRC="$ROOT/src-v2/io/github/animwidget/Contrast.java"
TM="$ROOT/src-v2/io/github/animwidget/ThemeManager.java"

# ① 输入常量必须还在 ThemeManager 里（模型跑在过期输入上 = 门禁自己先错）
for lit in -1 -526086 -1578258 -14735049 -9735552 -14934236 -15658216 -14012875 -1710101 -6511697; do
    if ! grep -q -- "$lit" "$TM"; then
        echo "  ThemeManager.java 里找不到默认色常量 $lit（ThemeMathTest 的输入已过期）"
        exit 1
    fi
done

# ② 只有一份颜色模型：老的通道乘法（adjust$map）不许复活
if grep -q 'float f = 1\.0f + (\$contrast \* 0\.035f)' "$TM"; then
    echo "  ThemeManager.java 里又出现了通道乘法（adjust\$map 的公式）—— 颜色模型必须只有一套"
    exit 1
fi

# ③ 编译真源码 + 测试
javac -nowarn -d "$OUT" -cp "$TC/android-all.jar:$TC/annotations-13.0.jar" \
    "$SRC" "$TC/ThemeMathTest.java" || {
    echo "  编译失败（Contrast.java / ThemeMathTest.java）"
    exit 1
}
java -cp "$OUT:$TC/android-all.jar" io.github.animwidget.ThemeMathTest
