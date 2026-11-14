#!/usr/bin/env bash
# v1.10.239：S03 探头判据的脱机自检（不需要 Android 运行时）。
#
# 为什么单独有个脚本：S03 的判据只在真机上跑，写错了只有用户跑一遍才发现。
# 已经错过两次（v237 的基准口径、v239 的容限单位 1.5f/255f 让判据永远 OK），
# 所以把「像素中心采样 / 乘 aFrac / 判据字符串 / 拟合搜索窗」抄一份到
# toolchain/ShadowProbeMathTest.java，用合成剖面注入 (σ 倍数, 偏移)，看判据认不认得出。
#
# 退出码 0 = 全部通过。verify-apk.sh 的 [7] 会调它。
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -nowarn -d "$OUT" "$ROOT/toolchain/ShadowProbeMathTest.java" || {
    echo "  ShadowProbeMathTest 编译失败"
    exit 1
}
java -cp "$OUT" ShadowProbeMathTest || exit 1
