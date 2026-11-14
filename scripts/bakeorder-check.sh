#!/usr/bin/env bash
# v1.10.452：导出收尾的**顺序与闸门**静态自检。
#
# 为什么值得单独一条门禁：446~451 连续六个版本，"导出后桌面没画面"的根因一直在变，
# 但变的全是同一条时间线上的**顺序**（谁先提交、谁先烘、谁重开批次）。
# 这类 bug 编译能过、逻辑看着也对，只有真机跑几分钟才会现形；
# 而顺序一旦被下一版顺手改回去，用户又要等一个版本才能发现。
#
# 断言（全部读源码，不需要 APK）：
#   1) finishExport 里「提交」必须排在「烘焙」之前（这是 452 的核心）
#   2) 「先烘 → 再提交」的老两条路径必须不存在（startBakeThenCommit / commitWithBake）
#   3) 每个 WidgetPrewarm.bakeNow 调用点都必须显式带帧数（5 参数形式）——
#      只带画布的 4 参数形式会让烘焙拿导出开始时的 0 帧快照，549 那批就是这么白跑的
#   4) 提交 / 烘焙 / 导出完成 三处都有一次性闸门（AtomicBoolean.compareAndSet）
#   5) 烘焙前必须数一遍正式目录的帧文件，为 0 就放弃并留日志（不许静默）
#   6) save() 在本轮还没收尾时必须直接 return（不许重开一轮导出）
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CFG="$ROOT/src-v2/io/github/animwidget/ConfigActivity.java"
bad=0
ok=0

line_of() { grep -n "$1" "$CFG" | head -1 | cut -d: -f1; }

# ---- 1) finishExport 里 提交 在 烘焙 之前
fe_start=$(line_of 'void finishExport')
if [ -z "$fe_start" ]; then
  echo "  找不到 finishExport"; bad=$((bad+1))
else
  # 只在 finishExport 之后的 150 行窗口里找（它的函数体不到 40 行，够用且不受别的函数干扰）
  c=$(awk -v a="$fe_start" 'NR>a && NR<=a+150 && /commitPendingExport\(uiAlive\)/ {print NR; exit}' "$CFG")
  k=$(awk -v a="$fe_start" 'NR>a && NR<=a+150 && /bakeAfterCommit\(/ {print NR; exit}' "$CFG")
  if [ -z "$c" ] || [ -z "$k" ]; then
    echo "  finishExport 里找不到「提交」或「烘焙」调用（提交=$c 烘焙=$k）"; bad=$((bad+1))
  elif [ "$c" -ge "$k" ]; then
    echo "  顺序错了：提交(行$c) 必须排在 烘焙(行$k) 之前"; bad=$((bad+1))
  else
    ok=$((ok+1))
  fi
fi

# ---- 2) 老的两条「先烘后提」路径必须已经删掉
for gone in startBakeThenCommit commitWithBake; do
  n=$(grep -c "private void $gone" "$CFG" || true)
  if [ "${n:-0}" -ne 0 ]; then
    echo "  「先烘后提交」的老路径还在：$gone"; bad=$((bad+1))
  else
    ok=$((ok+1))
  fi
done

# ---- 3) bakeNow 调用点：必须 2 处、且都带帧数（5 参数）
calls=$(grep -n "WidgetPrewarm.bakeNow(" "$CFG" || true)
n_calls=$(printf '%s\n' "$calls" | grep -c . || true)
if [ "$n_calls" -ne 2 ]; then
  echo "  bakeNow 调用点=$n_calls（期望 2：bakeAfterCommit + awaitBake 重试）"; bad=$((bad+1))
else
  ok=$((ok+1))
fi
# 调用的写法有跨行（`bakeNow(this, retryCfg,` 换行接画布和帧数），所以按「起始行号 ±1 行」拼起来再判
while IFS=: read -r ln _rest; do
  [ -z "$ln" ] && continue
  joined=$(awk -v a="$ln" 'NR>=a && NR<=a+1' "$CFG" | tr -d ' \n')
  args=${joined#*bakeNow(}
  args=${args%%)*}
  commas=$(printf '%s' "$args" | tr -cd ',' | wc -c)
  # 5 个参数 = 4 个逗号：context, config, width, height, frames
  if [ "$commas" -ne 4 ]; then
    echo "  bakeNow 参数不是 5 个（少了帧数）：$joined"; bad=$((bad+1))
  else
    ok=$((ok+1))
  fi
done <<< "$calls"

# ---- 4) 三处一次性闸门
for gate in exportFinished commitDone bakeStarted; do
  if [ "$(grep -c "this\.$gate\.compareAndSet(false, true)" "$CFG")" -lt 1 ]; then
    echo "  缺一次性闸门：$gate"; bad=$((bad+1))
  else
    ok=$((ok+1))
  fi
done

# ---- 5) 烘焙前数正式目录 + 为 0 时留日志放弃
if ! grep -q "int onDisk = countFrameFiles(widgetId);" "$CFG"; then
  echo "  烘焙前没有数正式目录的帧文件"; bad=$((bad+1))
elif ! grep -q '提交后烘焙放弃：正式目录里没有帧文件' "$CFG"; then
  echo "  帧文件为 0 时没有留日志"; bad=$((bad+1))
else
  ok=$((ok+1))
fi

# ---- 5b) 烘焙进度只用数字：showBakeProgress 不许碰导出进度条（v1.10.453 用户要求）
body=$(sed -n '/private void showBakeProgress/,/^    }$/p' "$CFG")
if printf '%s' "$body" | grep -q 'pbParse'; then
  echo "  showBakeProgress 还在写导出进度条（烘焙必须只用数字 x/y）"; bad=$((bad+1))
elif ! printf '%s' "$body" | grep -q 'cfg_bake_running'; then
  echo "  showBakeProgress 没有写「正在烘焙 x/y」这一行"; bad=$((bad+1))
else
  ok=$((ok+1))
fi
# 烘焙阶段（bakeStage）里，syncTaskUi 必须把导出条收起来
if [ "$(grep -c 'this.bakeStage' "$CFG")" -lt 3 ]; then
  echo "  bakeStage 判据不足（要在 syncTaskUi 文本/进度条两处 + awaitBake/endBakeWait 都生效）"; bad=$((bad+1))
elif ! sed -n '/final void syncTaskUi(/,/^    }$/p' "$CFG" | grep -q 'hideTaskBar()'; then
  echo "  syncTaskUi 在烘焙阶段没有收起导出进度条"; bad=$((bad+1))
else
  ok=$((ok+1))
fi

# ---- 5c) save() 的闸门只许用「本轮导出在写盘」，不许用 parseTaskActive（v1.10.454）
# 452 用 `busy || parseTaskActive` 当闸门 → 预览解析期间点保存被静默吞掉
# （用户原话："怎么解析时还不能导出了"）。预览解析不是导出，必须放行。
sv_head=$(sed -n '/final void save()/,/^    }$/p' "$CFG")
if ! printf '%s' "$sv_head" | grep -q 'this.exportRunning'; then
  echo "  save() 的闸门没用 exportRunning（会被预览解析误挡）"; bad=$((bad+1))
elif printf '%s' "$sv_head" | grep -q 'this.parseTaskActive'; then
  echo "  save() 又用 parseTaskActive 当闸门了（解析期间点保存会被吞）"; bad=$((bad+1))
else
  ok=$((ok+1))
fi
# 三条退出路径都必须放开闸门，否则"失败一次以后再也点不动保存"
for rel in 'failExport' 'cancelExportQuietly' 'finishExport'; do
  if ! awk -v fn="$rel" 'index($0,"void "fn"(")>0 {f=1} f && /exportRunning = false/ {found=1} f && /^    }$/ {exit} END {exit found?0:1}' "$CFG"; then
    echo "  $rel 没有把 exportRunning 放回 false（之后保存会被永久挡住）"; bad=$((bad+1))
  else
    ok=$((ok+1))
  fi
done

# ---- 6) save() 在本轮未收尾时必须 return
sv=$(line_of 'private final void save()')
if [ -z "$sv" ]; then
  echo "  找不到 save()"; bad=$((bad+1))
elif ! awk -v s="$sv" 'NR>s && NR<s+40 && /保存被忽略/ {found=1} END {exit found?0:1}' "$CFG"; then
  echo "  save() 没有「本轮导出在写盘就忽略」的闸门（会重开一轮导出）"; bad=$((bad+1))
else
  ok=$((ok+1))
fi

# ---- 7) v1.10.520：**每个 bakeNow 调用点都必须接住返回值**
# 根因（真机日志 Component_2608_35_10）：bitmap 直推档下 bakeNow 里 `!needUri` 直接 return（没起批次），
# 而调用方照旧进 awaitBake 干等 → 进度定在 0/16、5 秒重开（还是跳过）、15 秒判停滞放行。
# 判据：bakeNow 的返回值必须被赋值给一个 boolean，且马上有 `if (!xxx)` 的分支做收尾。
naked=0
while IFS=: read -r ln text; do
  [ -z "$ln" ] && continue
  if ! echo "$text" | grep -qE '=[[:space:]]*WidgetPrewarm\.bakeNow\('; then
    echo "  第 $ln 行直接调 bakeNow 却没接返回值（批次没起也会被等）"; naked=$((naked+1))
  fi
done < <(grep -n "WidgetPrewarm\.bakeNow(" "$CFG")
if [ "$naked" -eq 0 ]; then ok=$((ok+1)); else bad=$((bad+1)); fi

# ---- 8) bakeAfterCommit：返回 false 必须直接收尾，不许进 awaitBake
bac=$(line_of 'private void bakeAfterCommit')
if [ -z "$bac" ]; then
  echo "  找不到 bakeAfterCommit"; bad=$((bad+1))
elif ! awk -v s="$bac" 'NR>s && NR<s+90 && /if \(!started\)/ {found=1} END {exit found?0:1}' "$CFG"; then
  echo "  bakeAfterCommit 没有「批次没起就收尾」的分支（会卡在 0/n）"; bad=$((bad+1))
elif ! awk -v s="$bac" 'NR>s && NR<s+90 && /finishSaveNow\(\)/ {found=1} END {exit found?0:1}' "$CFG"; then
  echo "  bakeAfterCommit 的「批次没起」分支里没有 finishSaveNow()（页面不会自己收尾）"; bad=$((bad+1))
else
  ok=$((ok+1))
fi

# ---- 9) awaitBake 的重开分支：重开被拒时立刻放行（不许再等 15 秒的停滞判据）
ab=$(line_of 'private void awaitBake')
if [ -z "$ab" ]; then
  echo "  找不到 awaitBake"; bad=$((bad+1))
elif ! awk -v s="$ab" 'NR>s && NR<s+120 && /if \(!restarted\)/ {found=1} END {exit found?0:1}' "$CFG"; then
  echo "  awaitBake 重开一批时没检查返回值（重开被拒会干等到 15 秒）"; bad=$((bad+1))
else
  ok=$((ok+1))
fi

if [ "$bad" -eq 0 ]; then
  echo "BAKEORDER OK $ok"
  exit 0
fi
echo "BAKEORDER BAD $bad"
exit 1
