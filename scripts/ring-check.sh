#!/usr/bin/env bash
# v1.10.410：共享位图「预读环」（FrameRing）的离线对拍 —— 不需要 Android 运行时。
#
# 守的是四件事：
#   1. 预读环是纯数学（不许出现 android 依赖），否则这个门禁再也跑不了；
#   2. **解码池的每一个提交点都必须包一层 Pri** —— PriorityBlockingQueue 在 offer/siftUp 时
#      会把元素强转 Comparable，混进一个裸 lambda 就在提交侧抛 ClassCastException，
#      而两个现合成入口都 catch(Throwable)，后果是「那条通道静默失效」而不是报错；
#   3. 稳态零重复解码（旧逻辑每拍无条件重排 +1/+2 → 解码× 是推的 2 倍）；
#   4. 永不取未来帧 / HOLD 有自愈上限 / lead 随延迟自适应 / 预算随路数长。
#
# 用法：scripts/ring-check.sh   （退出码 0 = 全部通过）
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

SRC="$ROOT/src-v2/io/github/animwidget/FrameRing.java"
PUSH="$ROOT/src-v2/io/github/animwidget/WidgetUriPush.java"
TEST="$ROOT/toolchain/FrameRingCheck.java"

# ① 纯函数里不许出现 Android 依赖
if grep -qE "^import android\." "$SRC"; then
    echo "  FrameRing 里又出现了 android 依赖 —— 预读环必须能脱机跑"
    exit 1
fi
# ② 解码池只许有一个提交入口（裸 lambda 进优先级队列 = ClassCastException）
if grep -n "BMP_POOL.execute(() ->" "$PUSH" | grep -v "^[0-9]*: *\*" >/dev/null 2>&1; then
    echo "  WidgetUriPush 里又出现 BMP_POOL.execute(() -> ...) —— 优先级队列只收 Pri，裸 lambda 会抛 CCE"
    grep -n "BMP_POOL.execute(() ->" "$PUSH"
    exit 1
fi
grep -q "new Pri(p.dist\[i\]" "$PUSH" || {
    echo "  解码提交没有按紧迫度包 Pri —— 这一拍要的帧会排在预读帧后面"
    exit 1
}
grep -q "watchful(1, () -> {" "$PUSH" || {
    echo "  现合成入口没有走 watchful —— 它会被优先级队列拒绝"
    exit 1
}
# ②b 环必须在组件删除时释放（否则删掉的组件一直占内存预算）
grep -q "forgetRing(widgetId)" "$PUSH" || {
    echo "  clearWidget 里没有 forgetRing —— 删组件的环永远不会释放"
    exit 1
}
# ②c HOLD 必须有上限阀（不许冻屏）
grep -q "pick.holdStreak <= FrameRing.HOLD_LIMIT" "$PUSH" || {
    echo "  推送里没有 HOLD 上限阀 —— 环里只有未来帧时会一直不推（冻屏）"
    exit 1
}
# ②d v1.10.411：canvasFor0 里**不许**调会去算画布的 wantShared（会无限递归），只能调 sharedForCanvas
RENDER="$ROOT/src-v2/io/github/animwidget/WidgetRender.java"
if grep -q "WidgetUriPush.wantShared(" "$RENDER"; then
    echo "  WidgetRender 里又出现 wantShared( —— canvasFor0 → wantShared → canvasFor → canvasFor0 会栈溢出"
    grep -n "WidgetUriPush.wantShared(" "$RENDER"
    exit 1
fi
grep -q "WidgetUriPush.sharedForCanvas(" "$RENDER" || {
    echo "  WidgetRender 里没有 sharedForCanvas —— 1:1 档会被桌面解码预算重新夹小"
    exit 1
}
# ②e 共享位图的判据不许再挂回「格子面积」当主判据（那是 v1.10.344 的错判据）
grep -q "canvasPx \* Math.max(config.getFps(), 1) >= SHARED_PX_PER_SEC" "$PUSH" || {
    echo "  wantShared 的判据不是「画布像素 × 帧率」—— 别退回按格子判断"
    exit 1
}

javac -nowarn -d "$OUT" "$SRC" "$TEST" 2>"$OUT/err" || {
    cat "$OUT/err"
    echo "  离线对拍编译失败"
    exit 1
}
if java -cp "$OUT" io.github.animwidget.FrameRingCheck > "$OUT/run.log" 2>&1; then
    cat "$OUT/run.log"
    echo "RING OK"
else
    cat "$OUT/run.log"
    exit 1
fi
