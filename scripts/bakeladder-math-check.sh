#!/usr/bin/env bash
# v1.10.321：画布预算（BakeLadder）的离线对拍 —— 不需要 Android 运行时。
#
# 守的是三件事：
#   1. 系数 1.0 时不许无缘无故掉画质（量化误差在 ±5% 内、且不超过理论预算 105%）；
#   2. 阶梯能挡住 1%~3% 的尺寸抖动（±1.5% 的理论值必须落同一档）；
#   3. 自校准只会往下调、有下限、追不上时一定降。
#
# 用法：scripts/bakeladder-math-check.sh   （退出码 0 = 全部通过）
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

SRC="$ROOT/src-v2/io/github/animwidget/BakeLadder.java"
TEST="$ROOT/toolchain/BakeLadderMathTest.java"

# ① 纯函数里不许出现 Android 依赖（一旦出现，这个门禁就再也跑不了）
if grep -qE "^import android\." "$SRC"; then
    echo "  BakeLadder 里又出现了 android 依赖 —— 纯数学必须能脱机跑"
    exit 1
fi
# ② 预算只许由这一个文件夹（别处再写一套阶梯＝判据复制）
if grep -rq "LADDER" "$ROOT/src-v2/io/github/animwidget/WidgetRender.java"; then
    echo "  WidgetRender 里又出现阶梯 —— 预算夹取只许在 BakeLadder 一处"
    exit 1
fi
# ③ HIRES_AREA 那档必须被豁免（靠的是 WidgetRender 里的显式早退）
grep -q "if (base >= HIRES_AREA)" "$ROOT/src-v2/io/github/animwidget/WidgetRender.java" || {
    echo "  WidgetRender 里没有 HIRES_AREA 早退 —— JPEG 1:1 档会被自校准压掉"
    exit 1
}
# ④ v1.10.333：动图 1:1 档的豁免必须由 BakeLadder.calibrated 判（不许在 WidgetRender 里另写一套判据）
grep -q "BakeLadder.calibrated(lvl, config.getFrameCount())" \
    "$ROOT/src-v2/io/github/animwidget/WidgetRender.java" || {
    echo "  WidgetRender 里没有走 BakeLadder.calibrated —— 1:1 档会被阶梯/系数重新压小"
    exit 1
}
# ⑤ 自校准必须带上「独占」判据（并发的账不许记到画质上）
# v1.10.412：自校准只许喂**第一遍**的数（spentPass1/bakedPass1）——
# 第二遍升档故意跑在 nice10，把它算进吞吐等于"用慢速度升级画质 ⇒ 判定画布烘不动"。
grep -q "BakeCal.note(context, widgetId, config.getFps(), spentPass1, bakedPass1, alone, why)" \
    "$ROOT/src-v2/io/github/animwidget/WidgetPrewarm.java" || {
    echo "  预热没有把 alone/why 传给 BakeCal.note（且必须用第一遍的 spentPass1/bakedPass1）"
    exit 1
}
# ⑤b v1.10.413：`work()` 里的「文件已存在就跳过」必须让 PASS_FORCE 生效。
#     407~412 的第二遍就是在这一行被整帧跳过的（实测首遍 6419ms / 总用时 6430ms → 画布一直 q75）。
grep -q '!WidgetUriPush.passForce()' "$ROOT/src-v2/io/github/animwidget/WidgetPrewarm.java" || {
    echo "  work() 的跳过条件没有接 PASS_FORCE —— 第二遍会空转，画布永远停在第一遍的质量"
    exit 1
}
# ⑤c v1.10.413：第二遍的优先级必须在 work() 里设（线程体里设了也会被 work() 覆盖）
# v1.10.422：烘焙只剩一遍（导出时按目标质量直出），所以 work() 里**必须**无条件用 DEFAULT，
# 而「升档推迟」那一整套（upgradePass / deferUpgrade / PENDING_UPGRADES）必须已经不存在。
grep -q 'Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)' \
    "$ROOT/src-v2/io/github/animwidget/WidgetPrewarm.java" || {
    echo "  work() 里没有 DEFAULT 优先级 —— 单遍烘焙应该全速跑"
    exit 1
}
# 注意：要排除注释行 —— Java 里那句"这套机制已删除"的注释本身就含这些名字
if grep -v '^\s*//' "$ROOT/src-v2/io/github/animwidget/WidgetPrewarm.java" \
    | grep -q 'upgradePass\|deferUpgrade\|PENDING_UPGRADES'; then
    echo "  「升档推迟」的代码又回来了 —— v1.10.422 起烘焙只有一遍，这套机制已删除"
    exit 1
fi
# ⑥ 预热串行化不许被删（并发时 18 线程抢 8 核，总吞吐反而低 20%+）
grep -q "BAKE_SLOT.acquire()" "$ROOT/src-v2/io/github/animwidget/WidgetPrewarm.java" || {
    echo "  WidgetPrewarm 里没有 BAKE_SLOT —— 多个组件会重新同时开烘"
    exit 1
}
# ⑦ v1.10.334：1:1 档根本不算系数，就不许调 BakeCal.note（否则日志会假报「系数 0.55→0.57」）
grep -q "if (BakeLadder.calibrated(WidgetRender.animQuality(context, config), config.getFrameCount()))" \
    "$ROOT/src-v2/io/github/animwidget/WidgetPrewarm.java" || {
    echo "  预热的自校准没有按档位分流 —— 1:1 档会算出（且打印）一个没人用的系数"
    exit 1
}

javac -nowarn -d "$OUT" "$SRC" "$TEST" 2>"$OUT/err" || {
    cat "$OUT/err"
    echo "  离线对拍编译失败"
    exit 1
}
if java -cp "$OUT" BakeLadderMathTest > "$OUT/run.log" 2>&1; then
    cat "$OUT/run.log"
    echo "BAKELADDER OK"
else
    cat "$OUT/run.log"
    exit 1
fi
