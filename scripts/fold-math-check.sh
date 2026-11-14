#!/usr/bin/env bash
# v1.10.273：副标题收起/展开的几何自检（脱机，不需要 Android 运行时）。
#
# 守的是用户验收的那一条：「结束帧与结束后一帧完全一致」。
# 上一版的病根是主标题位置由**布局**决定（gravity=center_vertical），
# 动画只改行高 → 最后一帧切回 WRAP_CONTENT 时"补位"跳一下。
# 现在几何是 FoldMotion 里的纯函数，这个脚本现编真源码，沿时间轴逐点对拍。
#
# 用法：scripts/fold-math-check.sh   （退出码 0 = 全部通过）
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TC="$ROOT/toolchain"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

SRC="$ROOT/src-v2/io/github/animwidget/FoldMotion.java"
EASE="$ROOT/src-v2/io/github/animwidget/Ease.java"

# ①' v1.10.274：钉住文字块高度 + 手动位移那套已经删掉了（会把文字裁掉），
#     谁再把它加回来就立刻红。
if grep -q "blockShift" "$ROOT/src-v2/io/github/animwidget/SettingsActivity.java"; then
    echo "  SettingsActivity 里又出现了"钉块高 + 手动位移"的写法 —— 那会把主标题裁掉"
    exit 1
fi

# ① 曲线必须还是"手调"的那两条（被谁顺手换成系统默认就立刻红）
grep -q "new Ease(0.20f, 0.95f, 0.30f, 1.10f)" "$SRC" || {
    echo "  FoldMotion 里的副标题曲线不是选型时定的那条 (0.20,0.95,0.30,1.10)"
    exit 1
}
# ② 折叠动效自己的代码里只许出现"线性时间轴"（缓动在各元素贝塞尔上）；
#    其它动画（箭头旋转、开关回弹）用系统缓动是另一回事，不在本门禁范围内。
FOLD_BODY="$(awk '/private void setSubtitleFold/,/^    }$/' "$ROOT/src-v2/io/github/animwidget/SettingsActivity.java")"
if echo "$FOLD_BODY" | grep -qE "DecelerateInterpolator|AccelerateDecelerateInterpolator|AccelerateInterpolator|setInterpolator\([^\\n]*Ease"; then
    echo "  折叠动效里出现了系统默认缓动（时间轴必须线性、缓动挂在元素贝塞尔上）"
    exit 1
fi
echo "$FOLD_BODY" | grep -q "LinearInterpolator" || {
    echo "  折叠动效的时间轴不是线性的 —— 反向会因"时间重映射"而突变"
    exit 1
}

javac -nowarn -d "$OUT" -cp "$TC/android-all.jar:$TC/annotations-13.0.jar" \
    "$SRC" "$EASE" "$TC/FoldMathTest.java" || {
    echo "  编译失败（FoldMotion / FoldMathTest）"
    exit 1
}
java -cp "$OUT:$TC/android-all.jar" io.github.animwidget.FoldMathTest
