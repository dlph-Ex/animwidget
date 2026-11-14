#!/usr/bin/env bash
# APK 静态校验（v1.10.0 固化）：出包必跑。
#   1) 清单组件类是否真的在 dex 里（v1.8.10/11/12 三连崩的根因）
#   2) dex 类清单与上一版本的差异（丢类 = 运行期 NoClassDefFoundError）
#   3) R.java 里的资源 ID 与 resources.arsc 是否一致（手写 R 类必须对得上）
#   4) 字节码校验（ASM CheckClassAdapter）
# 用法: ./scripts/verify-apk.sh <apk> [上一个 apk]
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK="${1:?usage: verify-apk.sh <apk> [prev-apk]}"
PREV="${2:-}"
TC="$ROOT/toolchain"
TMP="$(mktemp -d)"
fail=0

echo "== [1] 清单组件 → dex"
aapt dump xmltree "$APK" AndroidManifest.xml 2>/dev/null \
  | grep -oE 'android:name\(0x[0-9a-f]+\)="[^"]+"' \
  | sed 's/.*="//; s/"$//' \
  | grep -E '^\.' | sed 's/^\./io.github.animwidget./' > "$TMP/components.txt"
aapt dump xmltree "$APK" AndroidManifest.xml 2>/dev/null \
  | grep -oE 'android:name\(0x[0-9a-f]+\)="io\.github\.animwidget[^"]*"' \
  | sed 's/.*="//; s/"$//' >> "$TMP/components.txt"
sort -u "$TMP/components.txt" -o "$TMP/components.txt"
java "$TC/DexCheck.java" "$APK" > "$TMP/classes.txt"
sort -u "$TMP/classes.txt" -o "$TMP/classes.txt"
missing=0
while read -r cls; do
  [ -z "$cls" ] && continue
  desc="L${cls//.//};"
  if ! grep -qx "$desc" "$TMP/classes.txt"; then
    echo "  MISSING $cls"
    missing=$((missing+1))
  fi
done < "$TMP/components.txt"
echo "  组件数=$(wc -l < "$TMP/components.txt") 缺失=$missing"
[ "$missing" -eq 0 ] || fail=1

echo "== [2] dex 类清单 vs 上一版本"
if [ -n "$PREV" ] && [ -f "$PREV" ]; then
  java "$TC/DexCheck.java" "$PREV" | sort -u > "$TMP/prev.txt"
  gone=$(comm -23 "$TMP/prev.txt" "$TMP/classes.txt")
  added=$(comm -13 "$TMP/prev.txt" "$TMP/classes.txt")
  echo "  上版=$(wc -l < "$TMP/prev.txt") 本版=$(wc -l < "$TMP/classes.txt")"
  echo "  新增: $(echo "$added" | grep -c . || true)   消失: $(echo "$gone" | grep -c . || true)"
  # 合成/桥接类（lambda、枚举 switch 映射）随代码改写消失是正常的，只对「真类」报警
  # 匿名内部类（Foo$1、Foo$1$1 …）会随源码增删监听器而重新编号，不属于「真类」，不报警。
  # v1.10.48 补充：SchemeTileView 里新增一个动画监听器 → $1$1 变成 $2$1，是编号漂移不是丢类。
  # v1.10.183 补充：**故意的改名/删除**必须写进白名单，并说明原因；
  # 白名单之外少一个「真类」照样 FAILED —— 保留门禁的原意（防 NoClassDefFoundError）。
  #   RewardDialog → RewardSheet（赞赏弹层不再是 Dialog，改名；调用点只有 DeveloperActivity）
  #   ShadowCurveView$OnCurveChanged → v1.10.222 曲线改成 6 个滑块，曲线图变成只读，
  #     这个「拖动回调」接口不再需要（调用点只有 ConfigActivity.setupShadowCurvePanel）
  #   FrameComposer$ShadowBands → v1.10.227 阴影改成复刻参考实现（一块烘好的位图 + 1 次 drawBitmap），
  #     「色带表」这个内部类随旧模型一起删除（调用点只有 FrameComposer.drawShadow）
  ALLOW_GONE_1="Lio/github/animwidget/RewardDialog;"
  ALLOW_GONE_2='Lio/github/animwidget/ShadowCurveView$OnCurveChanged;'
  ALLOW_GONE_3='Lio/github/animwidget/FrameComposer$ShadowBands;'
  #   v1.10.422：「升档推迟」整套机制删除（烘焙改成导出时一遍烘完），
  #     这个内部类（一条待升档任务）随之消失；调用点只有 WidgetPrewarm 自己。
  ALLOW_GONE_4='Lio/github/animwidget/WidgetPrewarm$Upgrade;'
  #   v1.10.464：作用范围改由 SegmentedRow 承担（用户：「滑块色的分段选择器没有适配新组件」），
  #     老的两段开关 SliderScopeSwitch 已删除 —— 同一形态只留一份实现。调用点只有滑块色的编辑页。
  ALLOW_GONE_5='Lio/github/animwidget/SliderScopeSwitch;'
  #     它的回调接口（两段开关专用）随之消失 —— 现在共用 SegmentedRow$OnSegmentClick。
  ALLOW_GONE_6='Lio/github/animwidget/SliderScopeSwitch$OnScopeChanged;'
  #   v1.10.499：输出中提示从 Dialog 改成**页内覆盖层** BusyOverlay（用户要求）。
  #     原类整体删除 —— 它的 Dialog.show() 要新窗口 + token 协商，且 capture() 在
  #     输入分发里做全屏截图（10MB 分配 + 整棵 View 树软件重绘），这是「拖滑块迟钝」的直接来源。
  ALLOW_GONE_7='Lio/github/animwidget/OutputBusyDialog;'
  real_gone=$(echo "$gone" | grep -v 'ExternalSyntheticLambda' | grep -v '\$WhenMappings' | grep -vE '\$[0-9]+(\$[0-9]+)*;$' | grep 'L' \
    | grep -vxF -e "$ALLOW_GONE_1" -e "$ALLOW_GONE_2" -e "$ALLOW_GONE_3" -e "$ALLOW_GONE_4" -e "$ALLOW_GONE_5" -e "$ALLOW_GONE_6" -e "$ALLOW_GONE_7")
  allowed=$(echo "$gone" | grep -xF -e "$ALLOW_GONE_1" -e "$ALLOW_GONE_2" -e "$ALLOW_GONE_3" -e "$ALLOW_GONE_4" -e "$ALLOW_GONE_5" -e "$ALLOW_GONE_6" -e "$ALLOW_GONE_7")
  if [ -n "$allowed" ]; then
    echo "  白名单(故意删除): $allowed"
  fi
  if [ -n "$real_gone" ]; then
    echo "$real_gone" | sed 's/^/  GONE /'
    fail=1
  fi
else
  echo "  (跳过：未给上一版 apk)"
fi

echo "== [3] R.java 资源 ID vs resources.arsc"
aapt2 dump resources "$APK" 2>/dev/null > "$TMP/res.txt"
badid=0
while read -r line; do
  type=$(echo "$line" | sed -nE 's/.*static final int ([a-z_0-9]+) = (0x[0-9a-f]+);/\2/p')
done < /dev/null
python3 - "$ROOT/src-v2/io/github/animwidget/R.java" "$TMP/res.txt" <<'PY'
import re, sys
rjava, res = sys.argv[1], sys.argv[2]
text = open(rjava).read()
# 按 class 分块
blocks = re.findall(r'public static final class (\w+) \{(.*?)\n    \}', text, re.S)
want = {}
for cls, body in blocks:
    for name, value in re.findall(r'static final int (\w+) = (0x[0-9a-f]+);', body):
        want[(cls, name)] = int(value, 16)
have = {}
for line in open(res):
    m = re.match(r'\s+resource (0x[0-9a-f]{8}) (\w+)/(\S+)\s*$', line)
    if m:
        have[(m.group(2), m.group(3))] = int(m.group(1), 16)
missing, mismatch = 0, 0
for (cls, name), value in sorted(want.items()):
    key = (cls, name)
    if key not in have:
        # style/attr 之类可能不带具体条目，忽略
        if cls in ('style', 'attr'):
            continue
        missing += 1
        print("  NO-SUCH-RES %s/%s" % (cls, name))
    elif have[key] != value:
        mismatch += 1
        print("  ID-MISMATCH %s/%s R=%#x arsc=%#x" % (cls, name, value, have[key]))
print("  R 条目=%d 缺失=%d 不一致=%d" % (len(want), missing, mismatch))
sys.exit(1 if (missing or mismatch) else 0)
PY
[ $? -eq 0 ] || fail=1

echo "== [4] 字节码校验"
mkdir -p "$TMP/classes"
AJ="$TC/android-all.jar"
javac -nowarn -source 8 -target 8 -bootclasspath "$AJ:$TC/jdk-core-stubs.jar:$TC/core-lambda-stubs.jar" \
  -classpath "$TC/kotlin-stdlib.jar:$TC/annotations-13.0.jar:$TC/shizuku-all.jar:$TC/shizuku-aidl.jar" \
  -d "$TMP/classes" $(find "$ROOT/src-v2" -name '*.java') 2>"$TMP/javac.log"
if [ -s "$TMP/javac.log" ] && grep -q "error:" "$TMP/javac.log"; then
  echo "  javac 失败"; head -20 "$TMP/javac.log"; fail=1
else
  java -cp "$TC/asm-9.7.jar:$TC/asm-tree-9.7.jar:$TC/asm-util-9.7.jar:$TC/asm-analysis-9.7.jar" \
    "$TC/ClassCheck.java" "$TMP/classes" "$AJ" "$TC/jdk-core-stubs.jar" "$TC/kotlin-stdlib.jar" \
    "$TC/annotations-13.0.jar" "$TC/shizuku-all.jar" "$TC/shizuku-aidl.jar" > "$TMP/check.txt" 2>&1
  tail -3 "$TMP/check.txt"
  # v1.10.131：**门禁本身有 bug** —— 原来这里只打印不看结果，bad=1 也照样输出「结论 OK」。
  if grep -qE 'bad=[1-9]' "$TMP/check.txt"; then
    echo "  字节码校验未通过（bad>0）"
    fail=1
  fi
fi

# v1.10.357：**运行时依赖门禁**。v1.10.356 犯过一次：R8 命令行里把 kotlin-stdlib 放进了
# `--lib`（只做类型推断、不打包），结果 APK 里一个 kotlin 类都没有 —— Kotlin 编译出来的
# 每一行都是 `Intrinsics.checkNotNullParameter`，**一启动就 NoClassDefFoundError**。
# 这个错误编译过、206 条断言过、[1] 组件齐全、[3] 资源一致，全都查不出来。
# 所以这里直接数类：dex 里的 kotlin 运行时类数必须和未混淆版同量级。
echo "== [4b] 运行时依赖（kotlin 运行时必须在 dex 里）"
KT=$(grep -c '^Lkotlin/' "$TMP/classes.txt" || true)
RIKKA=$(grep -c '^Lrikka/' "$TMP/classes.txt" || true)
echo "  kotlin 类=$KT  rikka 类=$RIKKA"
if [ "${KT:-0}" -lt 900 ]; then
  echo "  FAIL kotlin 类只有 ${KT:-0} 个（应 >=900）"
  echo "       → 多半是 dex 工具的 --lib / input 用反了：库文件必须作为 input 才会打进 dex"
  fail=1
fi
if [ "${RIKKA:-0}" -lt 20 ]; then
  echo "  FAIL rikka 类只有 ${RIKKA:-0} 个（应 >=20）：Shizuku SDK 没打进 dex"
  fail=1
fi
[ "${KT:-0}" -ge 900 ] && [ "${RIKKA:-0}" -ge 20 ] && echo "  OK 运行时依赖齐全"

echo "== [5] 主题结构门禁"
if python3 "$ROOT/scripts/theme-gate.py"; then
  :
else
  echo "  主题门禁未通过（旁路键 / 自绘控件没接换肤链路）"
  fail=1
fi

echo "== [6] 形状 / 阴影几何门禁（v1.10.211）"
if python3 "$ROOT/scripts/shape-check.py" | tail -1 | grep -qx "  OK"; then
  echo "  形状门禁 OK"
else
  python3 "$ROOT/scripts/shape-check.py" | tail -6
  echo "  形状门禁未通过（蒙版会被拉歪 / 没占满格子 / label 缺 case）"
  fail=1
fi
if python3 "$ROOT/scripts/shadow-check.py" | tail -1 | grep -qx "   OK"; then
  echo "  阴影门禁 OK"
else
  python3 "$ROOT/scripts/shadow-check.py" | tail -6
  echo "  阴影门禁未通过（上限不同步 / 角上盖不住）"
  fail=1
fi

echo "== [7] S03 探头判据自检（脱机，v1.10.239）"
if bash "$ROOT/scripts/probe-selftest.sh" > "$TMP/selftest.log" 2>&1 \
   && grep -q "^SELFTEST OK" "$TMP/selftest.log"; then
  echo "  OK（$(grep -c '^  OK ' "$TMP/selftest.log") 条断言：像素中心/最终 alpha 容限/0.25px 搜索都能判对）"
else
  tail -20 "$TMP/selftest.log"
  echo "  探头判据自检未通过（判据写错了会让 S03 永远报 OK 或永远报 FAIL）"
  fail=1
fi

echo "== [8] 判断代号体检（v1.10.240）"
if python3 "$ROOT/scripts/diag-audit.py" > "$TMP/diag.log" 2>&1; then
  echo "  OK（未登记 0 / 重复 0 / 多文本未复核 0）"
else
  tail -20 "$TMP/diag.log"
  echo "  判断代号体检未通过（有代号没登记、重复、或一个号塞了两种含义）"
  fail=1
fi

echo "== [9] 布局控件类型 vs Java 强转（v1.10.241）"
if python3 "$ROOT/scripts/viewcast-check.py" > "$TMP/cast.log" 2>&1; then
  tail -1 "$TMP/cast.log"
else
  cat "$TMP/cast.log"
  echo "  强转与布局不一致：运行时会 ClassCastException（v1.10.240 设置页闪退就是这个）"
  fail=1
fi

echo "== [10] 对比度颜色模型自检（v1.10.270）"
if bash "$ROOT/scripts/theme-math-check.sh" > "$TMP/thememath.log" 2>&1 \
   && grep -q "^THEMEMATH OK" "$TMP/thememath.log"; then
  echo "  OK（$(grep -c '^  OK ' "$TMP/thememath.log") 条断言：表面三层全档逐像素不动、层差恒定、任意中性变体的线条都看得见）"
else
  tail -20 "$TMP/thememath.log"
  echo "  颜色模型自检未通过（底被动过 / 线条会消失 / 默认外观被改坏）"
  fail=1
fi

echo "== [11] 折叠几何自检（v1.10.273）"
if bash "$ROOT/scripts/fold-math-check.sh" > "$TMP/foldmath.log" 2>&1 \
   && grep -q "^FOLDMATH OK" "$TMP/foldmath.log"; then
  echo "  OK（$(grep -c '^  OK ' "$TMP/foldmath.log") 条断言：末帧==静息帧、沿时间轴无台阶、主标题单调不来回）"
else
  tail -20 "$TMP/foldmath.log"
  echo "  折叠几何自检未通过（主标题会在结束时"补位"跳一下）"
  fail=1
fi

echo "== [12] 比例判据自检（v1.10.313）"
if bash "$ROOT/scripts/layout-math-check.sh" > "$TMP/layoutmath.log" 2>&1 \
   && grep -q "^LAYOUTMATH OK" "$TMP/layoutmath.log"; then
  echo "  OK（$(grep -c '^  OK ' "$TMP/layoutmath.log") 条断言：画布恒等格子、尺寸补 padding、近方形铺满、衬底不许回来）"
else
  tail -20 "$TMP/layoutmath.log"
  echo "  比例判据未通过（判据被复制成两处 / 旧画布又用 fitCenter 兜底）"
  fail=1
fi

echo "== [13] 设置页两卡几何对拍（v1.10.320）"
if python3 "$ROOT/scripts/settings-align-check.py" > "$TMP/align.log" 2>&1; then
  echo "  OK（界面语言 / 系统模式 两张卡的卡片·header·图标·标题·胶囊·行结构逐项相等）"
else
  tail -20 "$TMP/align.log"
  echo "  两张卡的内缩对不齐（改布局时漏了某一级 padding）"
  fail=1
fi

echo "== [14] 画布预算自校准自检（v1.10.321，v1.10.333 加档位豁免）"
if bash "$ROOT/scripts/bakeladder-math-check.sh" > "$TMP/bakeladder.log" 2>&1 \
   && grep -q "^BAKELADDER OK" "$TMP/bakeladder.log"; then
  echo "  OK（$(grep -o '[0-9]* 条断言' "$TMP/bakeladder.log" | head -1)：上限契约/落档/抖动免疫/单调/自校准上下限）"
else
  tail -20 "$TMP/bakeladder.log"
  echo "  预算自校准未通过（阶梯被复制到别处 / HIRES_AREA 没被豁免 / 自校准方向错了）"
  fail=1
fi

echo "== [15] 布局 id 引用体检（v1.10.392）"
if python3 "$ROOT/scripts/layout-id-check.py" > "$TMP/idcheck.log" 2>&1; then
  tail -1 "$TMP/idcheck.log"
else
  cat "$TMP/idcheck.log"
  echo "  代码引用了布局里不存在的 id（改版式时整块被删掉了？）"
  fail=1
fi

echo "== [16] 预读环自检（v1.10.410）"
if "$ROOT/scripts/ring-check.sh" > "$TMP/ring.log" 2>&1 \
   && grep -q "^RING OK" "$TMP/ring.log"; then
  echo "  OK（$(grep -o '[0-9]* 条断言' "$TMP/ring.log" | head -1)：零重复提交/不取未来帧/HOLD 自愈/lead 自适应/预算随路数长）"
else
  tail -20 "$TMP/ring.log"
  echo "  预读环未通过（解码池混了裸 lambda / 环没在删组件时释放 / HOLD 没有上限阀）"
  fail=1
fi

echo "== [17] 日志页版式（v1.10.436/437）"
# 432~435 的教训：日志页用「把 ScrollView 里的视图搬到新 page + 按文本递归置 GONE」补版式，
# 结果提示行被搬进 keep 子树、而扫描恰好跳过 keep 子树 —— 五个版本都没修掉。
# 436 又犯了一次反方向的错：直接把这行提示从布局里删了（用户："文本提示谁允许你删了"）。
# 所以这里钉三件事：
#   ① 提示行 tv_log_hint 必须**还在**布局里（它是需求，不是 bug）；
#   ② 旧正文 tv_log_body 必须不在（正文已经是自绘控制台）；
#   ③ dex 里不许再出现「只保留/自动清除/重启不会」—— 不许再用文案当选择器。
log17=0
aapt2 dump xmltree "$APK" --file res/layout/activity_log.xml > "$TMP/loglayout.txt" 2>/dev/null
if ! grep -q "@0x7f08007b" "$TMP/loglayout.txt"; then
  echo "  布局里没有提示行 tv_log_hint（0x7f08007b）—— 它被谁删了？"
  log17=1
fi
if grep -q "@0x7f080079" "$TMP/loglayout.txt"; then
  echo "  布局里还有旧正文 tv_log_body（0x7f080079）"
  log17=1
fi
for lit in 只保留 自动清除 重启不会; do
  if unzip -p "$APK" classes.dex | grep -a -q "$lit"; then
    echo "  dex 里还有按文本认人的字面量「$lit」"
    log17=1
  fi
done
if [ "$log17" -eq 0 ]; then
  echo "  OK（提示行在、旧正文不在、dex 无文本匹配字面量）"
else
  echo "  日志页版式又跑偏了"
  fail=1
fi

echo "== [18] 日志页刷新节流 / 惯性状态（v1.10.438）"
# 435 埋的雷：isScrolling() 用「速度绝对值 > 0.05」判断，而惯性跑完只 return 不清速度 ——
# 只要用户抛过一次列表，这个判据就永久为真，refresh() 从此一路 return：
# 清屏清了数据画面不动、撤回不显、新日志也不再出现（用户："清屏不了日志/撤回也有BUG"）。
CONSOLE="$ROOT/src-v2/io/github/animwidget/LogConsoleView.java"
LOGACT="$ROOT/src-v2/io/github/animwidget/LogActivity.java"
log18=0
# 只认「代码行」（判据要带 return/赋值前缀）—— 注释里引述旧写法不算命中（LESSONS 176.3）
# 只看代码行（去掉以 * 开头的 javadoc），否则注释里引述旧写法会把门禁自己点红（LESSONS 176.3）
CODE_ONLY="$(grep -v '^[[:space:]]*[*]' "$CONSOLE")"
if printf '%s\n' "$CODE_ONLY" | grep -Fq 'flingVx) + Math.abs(flingVy) > 0.05f'; then
  echo "  isScrolling() 又用速度绝对值当判据（惯性停下后永不归零）"
  log18=1
fi
if ! grep -q 'return barVisible || draggingBar || flinging;' "$CONSOLE"; then
  echo "  isScrolling() 没走 flinging 状态位"
  log18=1
fi
if [ "$(grep -c 'flinging = false' "$CONSOLE")" -lt 2 ]; then
  echo "  惯性停下时没有把 flinging 清掉（run 的停止分支 / ACTION_DOWN）"
  log18=1
fi
if [ "$(grep -c 'refresh(true)' "$LOGACT")" -lt 2 ]; then
  echo "  清屏/撤回没有走 refresh(true) —— 会被「正在滑动」的节流吞掉"
  log18=1
fi
if [ "$log18" -eq 0 ]; then
  echo "  OK（状态位判据 / 停下清零 / 清屏撤回强制刷新）"
else
  echo "  日志页刷新链又会被永久吞掉"
  fail=1
fi

echo "== [19] 日志两条承诺（v1.10.439/440）"
# ① 30 分钟保留窗口：内存/撤回快照/磁盘/时钟回调 四条路径都要按条目时间戳到期
# ② 逐条渐显不重播：来新日志时只有新行淡入，老行不许重播（"新日志生成文本页就闪一次"）
if bash "$ROOT/scripts/logttl-check.sh" > "$TMP/ttl.log" 2>&1 \
   && grep -q "^TTL OK" "$TMP/ttl.log" && grep -q "^ALIGN OK" "$TMP/ttl.log"; then
  T19="$(grep -o 'TTL OK [0-9]*' "$TMP/ttl.log" | awk '{print $3}')"
  A19="$(grep -o 'ALIGN OK [0-9]*' "$TMP/ttl.log" | awk '{print $3}')"
  echo "  OK（保留窗口 ${T19} 条：按条目时间戳/边界/时钟回调/清屏快照同 TTL/撤回不续命；逐条渐显 ${A19} 条：只淡新行）"
else
  tail -20 "$TMP/ttl.log"
  echo "  日志的保留窗口或渐显行为不符合承诺"
  fail=1
fi

echo "== [20] 导出收尾顺序与闸门（v1.10.452）"
# 446~451 六版都栽在「先烘后提交」这条时间线上：提交前帧还在 frames.new，
# 烘焙读 frames/ 就读不到，235 帧全失败、5ms 跑完、进度 0/235。
# 这一条把「提交必须在烘焙之前」+「三处一次性闸门」钉死，顺序被改回去就当场 FAILED。
if bash "$ROOT/scripts/bakeorder-check.sh" > "$TMP/bakeorder.log" 2>&1 \
   && grep -q "^BAKEORDER OK" "$TMP/bakeorder.log"; then
  N20="$(grep -o 'BAKEORDER OK [0-9]*' "$TMP/bakeorder.log" | awk '{print $3}')"
  echo "  OK（${N20} 条断言：提交先于烘焙 / 老路径已删 / bakeNow 必带帧数 / 三道闸门 / 帧文件判据 / 烘焙只用数字 x/y / save 闸门）"
else
  tail -20 "$TMP/bakeorder.log"
  echo "  导出收尾的顺序或闸门被改坏了（会重新变成「桌面没画面」）"
  fail=1
fi

echo "== [21] 动图 WebP 封装器（v1.10.455 / P1）"
# 桌面组件要「零 IPC + App 退出后还在动」，唯一的路是单文件动图 WebP + AnimatedImageDrawable。
# 这里验证我们自己的 ANMF 封装器：用 Pillow 生成真 WebP 单帧（有损/无损各一遍）→ Java 拼容器
# → Pillow 解码逐帧对像素与时长。写错一个 bit，文件"能保存"但第二帧起就全花，读代码看不出来。
if python3 "$ROOT/scripts/webp-anim-check.py" > "$TMP/webpanim.log" 2>&1 \
   && grep -q "^WEBPANIM OK" "$TMP/webpanim.log"; then
  N21="$(grep -o 'WEBPANIM OK [0-9]*' "$TMP/webpanim.log" | awk '{print $3}')"
  echo "  OK（${N21} 条断言：帧数/画布/逐帧像素逐字节相等/每帧时长/循环次数，有损+无损两条路）"
else
  tail -20 "$TMP/webpanim.log"
  echo "  动图 WebP 封装器拼出来的文件不能被第三方解码器正确读出"
  fail=1
fi

echo "== [22] 内容/overlay 分层指纹（v1.10.459）"
# 指纹是「overlay 能不能跨帧复用」的唯一判据：太敏感 = 每帧重建（白做），太迟钝 = 改了阴影还用旧
# overlay（画面错）。两种错都不崩不报，只能靠断言钉住。纯 JDK 可跑（FrameLayers 不依赖 android）。
fl_dir="$TMP/framelayers"
mkdir -p "$fl_dir"
if javac -nowarn -cp "$TC/annotations-13.0.jar" -d "$fl_dir" \
     "$ROOT/src-v2/io/github/animwidget/FrameLayers.java" "$ROOT/tests/FrameLayersTest.java" > "$TMP/fl.log" 2>&1 \
   && java -cp "$fl_dir:$TC/annotations-13.0.jar" io.github.animwidget.FrameLayersTest > "$TMP/fl-run.log" 2>&1 \
   && grep -q "^FRAMELAYERS OK" "$TMP/fl-run.log"; then
  N22="$(grep -o 'FRAMELAYERS OK [0-9]*' "$TMP/fl-run.log" | awk '{print $3}')"
  echo "  OK（${N22} 条断言：17 个决定项各自独立改指纹 + 浮点抖动免疫 + 空/null 曲线等价 + 重复一致）"
else
  cat "$TMP/fl.log" "$TMP/fl-run.log" 2>/dev/null | tail -20
  echo "  分层指纹判据不成立（overlay 复用会错）"
  fail=1
fi

echo "== [23] 预览虚线取色（v1.10.460）"
# 判据是用户的：壁纸「颜色领域最大」的相反值。这里钉住三件事 —— 取最大对噪声稳、
# 相反值把最强通道变最弱（色相相反）、读不到壁纸必须退回兜底色（框绝不能消失）。
oc="$TMP/outlinecolor"
mkdir -p "$oc"
if javac -nowarn -cp "$TC/annotations-13.0.jar" -d "$oc" \
     "$ROOT/src-v2/io/github/animwidget/OutlineColor.java" "$ROOT/tests/OutlineColorTest.java" > "$TMP/oc.log" 2>&1 \
   && java -cp "$oc:$TC/annotations-13.0.jar" io.github.animwidget.OutlineColorTest > "$TMP/oc-run.log" 2>&1 \
   && grep -q "^OUTLINECOLOR OK" "$TMP/oc-run.log"; then
  N23="$(grep -o 'OUTLINECOLOR OK [0-9]*' "$TMP/oc-run.log" | awk '{print $3}')"
  echo "  OK（${N23} 条断言：主色取最大/透明像素不参战/噪声不偏色/相反值色相反转/兜底色）"
else
  cat "$TMP/oc.log" "$TMP/oc-run.log" 2>/dev/null | tail -20
  echo "  虚线取色判据不成立（框会看不见或颜色乱）"
  fail=1
fi

echo "== [24] 语义色编辑页：每行只看到自己的控件（v1.10.463）"
# 462 的四个错里，只有这一条是**结构性**的：showColorEditor() 是所有颜色行共用的一个弹窗，
# 把两条选择器无条件插进去 —— 点主色、点表面色、点错误色，每一行都能看到两条无关的控件。
# 这条把四件事钉死：① outline 这一行真的存在（462 只做了底层函数、界面上没入口）；
# ② 控件只在自己的 key 下创建/插入；③ 首页色行不再挂作用范围开关（迁移不许是复制）；
# ④ 存储只有 ThemePrefs 一个口 + 选中块必须 180ms 滑过去（不许跳变）。
if python3 "$ROOT/scripts/color-editor-scope-check.py" > "$TMP/colorscope.log" 2>&1 \
   && grep -q "^COLORSCOPE OK" "$TMP/colorscope.log"; then
  N24="$(grep -o 'COLORSCOPE OK [0-9]*' "$TMP/colorscope.log" | awk '{print $3}')"
  echo "  OK（${N24} 条断言：outline 行存在 / 两条控件各归各页 / 首页不重复挂控件 / 只有一份分段实现 / 圆角平行四边形+靠左 / 180ms 滑动 / 预览与色块同源 / 选中块同形状）"
else
  cat "$TMP/colorscope.log"
  echo "  语义色编辑页又串页了（或「预览虚线色」这一行又没落地）"
  fail=1
fi

echo "== [25] 动图打包器（v1.10.469 / #4 第 1 步）"
# 逐帧 WebP → 一个动图文件。三条失败路径在真机永远走不到，一旦走到就是「桌面整块不显示」：
# 帧数超限 / 输入不是 WebP / 失败留下半截文件。这里用 Pillow 生成真帧 → Java 打包 → Pillow 逐帧对像素与时长。
if python3 "$ROOT/scripts/anim-pack-check.py" > "$TMP/animpack.log" 2>&1 \
   && grep -q "^ANIMPACK-CHECK OK" "$TMP/animpack.log"; then
  N25="$(grep -o 'ANIMPACK-CHECK OK [0-9]*' "$TMP/animpack.log" | awk '{print $3}')"
  NJ="$(grep -o 'ANIMPACK OK [0-9]*' "$TMP/animpack.log" | awk '{print $3}')"
  echo "  OK（${N25} 条断言 / Java 自检 ${NJ} 条：帧数/每帧时长=round(1000/fps)/循环无限/逐帧像素一致/上限与失败不留残文件）"
else
  tail -20 "$TMP/animpack.log"
  echo "  动图打包器拼出来的文件不可信（桌面会整块不显示的那种错）"
  fail=1
fi

echo "== [26] 编码/解码对拍（v1.10.478 / P0-2 否决后的测量）"
# P0-2「不透明内容帧 JPEG + 常驻 overlay」在几何上不成立（圆角必露方角），剩下的唯一形态是
# **我方进程内**「JPEG 解码 + 常驻蒙版合成」。它成立与否全押在「JPEG 解码比 WebP 快多少」这一个数上，
# 而这个数只能真机测（W48）。这里钉住的是：测法本身不许骗自己（同帧同尺寸同解码器 / 取中位数 /
# 合成那一步真的做了 / 不许接进播放路径）。
if python3 "$ROOT/scripts/decode-bench-check.py" > "$TMP/decodebench.log" 2>&1 \
   && grep -q "^DECODEBENCH-CHECK OK" "$TMP/decodebench.log"; then
  N26="$(grep -o 'DECODEBENCH-CHECK OK [0-9]*' "$TMP/decodebench.log" | awk '{print $3}')"
  NB="$(grep -o 'BENCHMATH OK [0-9]*' "$TMP/decodebench.log" | awk '{print $3}')"
  echo "  OK（${N26} 条断言 / Java 自检 ${NB} 条：中位数/单位/判据方向 / 同一张成品帧同尺寸同解码器 / 合成真的做了 blit+DST_IN / 不接进播放路径）"
else
  tail -20 "$TMP/decodebench.log"
  echo "  解码对拍的口径又不对了（会得出「JPEG 更便宜」的假结论）"
  fail=1
fi

echo "== [27] 同素材共享解码 N→1（v1.10.481 / P0-3·A）"
# 共享帧表最贵的错是「键算错」：症状是**某一帧画面不对**，不崩、不掉帧、不打日志。
# 这里钉住：先查表再解码 / 绝不 recycle 共享位图 / 键含内容签名但不含 mtime /
# 表有字节与条数上限 / 日志页导航栏 45% 透明（不透明度 0.55 + 取 navigationBars inset）。
if python3 "$ROOT/scripts/frame-share-check.py" > "$TMP/frameshare.log" 2>&1 \
   && grep -q "^FRAMESHARE-CHECK OK" "$TMP/frameshare.log"; then
  N27="$(grep -o 'FRAMESHARE-CHECK OK [0-9]*' "$TMP/frameshare.log" | awk '{print $3}')"
  NK="$(grep -o 'FRAMESHAREKEY OK [0-9]*' "$TMP/frameshare.log" | awk '{print $3}')"
  echo "  OK（${N27} 条断言 / Java 自检 ${NK} 条：键归一化与雪崩/先查表后解码/迟到结果也公布/绝不 recycle/表有上限/键不含 mtime 但 contentTag 保留 mtime/导航栏 45% 透明）"
else
  tail -20 "$TMP/frameshare.log"
  echo "  共享解码的键或顺序错了（症状是「某一帧画面不对」这种查不出来的错）"
  fail=1
fi

echo "== [28] 分层帧：JPEG 内容 + 常驻 alpha（v1.10.482 / P0-3·B）"
# 每帧解一张 JPEG + 一次 DST_IN 把形状/阴影乘回来（W48 实测 12.4ms → 6.6ms）。
# 错的方式都是「画面形状不对」而不崩：所以这里钉顺序（写完帧才写分层、播放先 ready）、
# 语义（DST_IN 而不是叠加）、回落（自检不过整代回落原 WebP 帧）、判据（MAE 不是最大值）。
if python3 "$ROOT/scripts/layer-frames-check.py" > "$TMP/layerframes.log" 2>&1 \
   && grep -q "^LAYERFRAMES-CHECK OK" "$TMP/layerframes.log"; then
  N28="$(grep -o 'LAYERFRAMES-CHECK OK [0-9]*' "$TMP/layerframes.log" | awk '{print $3}')"
  echo "  OK（${N28} 条断言：JPEG+DST_IN/就地乘 alpha/烘完才写/先 ready 再 compose/自检不过清产物/MAE 判据/整代回落）"
else
  tail -20 "$TMP/layerframes.log"
  echo "  分层帧的语义或顺序不对（症状是方角/黑底/整块透明这种「看得出但查不到」的错）"
  fail=1
fi

echo "== [29] 推帧统计口径（v1.10.489）"
# 487 的 `共享帧` 只有 9~25% 是**漏计**：真正省下的解码在「同一帧去重」里，现在必须打出来（`同帧=`）。
# 另有反向断言：尺寸变更里**不许**再出现「跳过重推」的判断 —— 拖动时暂停是所有桌面组件都有的
# 正常现象（用户已明确），488 拿它当 bug 修是误判。
if python3 "$ROOT/scripts/resize-push-check.py" > "$TMP/resizepush.log" 2>&1 \
   && grep -q "^PUSHSTATS-CHECK OK" "$TMP/resizepush.log"; then
  N29="$(grep -o 'PUSHSTATS-CHECK OK [0-9]*' "$TMP/resizepush.log" | awk '{print $3}')"
  echo "  OK（${N29} 条断言：同帧去重必须计数 / 尺寸变更保持原样、不许跳过重推）"
else
  tail -20 "$TMP/resizepush.log"
  echo "  统计口径不对，或又去「修」拖动时的正常暂停了"
  fail=1
fi

echo "== [30] 剩余性能空间（v1.10.495）"
# 编辑页顶部常驻那一行：算错就是误导用户往桌面堆组件。钉常量来源（71Mpx/s、1.5ms）、
# 夹取、共享折算、后台线程、跟随系统深浅色。
if python3 "$ROOT/scripts/perf-headroom-check.py" > "$TMP/perf.log" 2>&1 \
   && grep -q "^PERFHEADROOM-CHECK OK" "$TMP/perf.log"; then
  N30="$(grep -o 'PERFHEADROOM-CHECK OK [0-9]*' "$TMP/perf.log" | awk '{print $3}')"
  NJ="$(grep -o 'PERFMATH OK [0-9]*' "$TMP/perf.log" | awk '{print $2}')"
  echo "  OK（${N30} 条断言 / Java 自检 ${NJ} 条：实测常量/共享折算/0~100 夹取/不建议判定/后台线程/跟随深浅色）"
else
  tail -20 "$TMP/perf.log"
  echo "  剩余性能那一行不可信（会误导用户加组件）"
  fail=1
fi

echo "== [31] 性能页改版 / 主按钮两态 / 输出中锁参（v1.10.497）"
# 这一版全是「看得见的行为约定」：锚点搬家、按钮两态、参数能摸但不执行只弹窗。
# 没有一条编译能发现（findViewById 返回 null 才算，弹窗少一个 tag 谁也不会报错），所以逐条钉死。
if python3 "$ROOT/scripts/output-lock-check.py" > "$TMP/lock.log" 2>&1 \
   && grep -q "^OUTPUTLOCK-CHECK OK" "$TMP/lock.log"; then
  N31="$(grep -o 'OUTPUTLOCK-CHECK OK [0-9]*' "$TMP/lock.log" | awk '{print $3}')"
  echo "  OK（${N31} 条断言：锚点搬走且不悬空分隔线 / 预热线程数真开线程 / 副标题无黑话 / 按钮两态 / 锁参清单与逐帧模糊弹窗）"
else
  tail -30 "$TMP/lock.log"
  echo "  这一版的三件约定被破坏了（首页锚点/按钮文案/输出中拦参）"
  fail=1
fi

echo "== [32] 大素材导入：GIF 流式解码（v1.10.498）"
# 用户素材：1MP·945×1224·210MB 的 GIF。整份 readAll 会 OOM，而且 OOM 被吞成
# 「动图解码失败」—— 所以这一版把 GIF 探测/解码/时长全改成流式，并把
# 「写盘失败静默 return」「250MB 判断写在写完之后」这两处静默半截一起修掉。
if python3 "$ROOT/scripts/gif-stream-check.py" > "$TMP/gifstream.log" 2>&1 \
   && grep -q "^GIFSTREAM-CHECK OK" "$TMP/gifstream.log"; then
  N32="$(grep -o 'GIFSTREAM-CHECK OK [0-9]*' "$TMP/gifstream.log" | awk '{print $3}')"
  echo "  OK（${N32} 条断言：流式探测/解码存在且不整份读 / 复用同一份像素路径 / 导出与预览都分流 / 选图与时长与覆盖层三条入口 / 写盘失败不再静默 / 250MB 软预算 + 500MB 硬顶 / W51 已登记）"
else
  tail -20 "$TMP/gifstream.log"
  echo "  GIF 又被整份读进内存了（210MB 素材会 OOM 成「解码失败」）"
  fail=1
fi
# 逐字段对拍：流式 probe 与整份 byte[] 的 info 必须给出同一份时间轴（尺寸/帧数/时长/每帧延时）。
if bash "$ROOT/scripts/gifstream-selftest.sh" > "$TMP/gifself.log" 2>&1 \
   && grep -q "^GIFSTREAM-CHECK OK" "$TMP/gifself.log"; then
  echo "  OK（probe 对拍：$(grep -o 'files=[0-9]*' "$TMP/gifself.log") 个样本，流式与整份逐字段一致）"
elif grep -q "^GIFSTREAM-SELFTEST SKIP" "$TMP/gifself.log"; then
  echo "  跳过（没装 PIL，跑不了样本对拍）"
else
  tail -15 "$TMP/gifself.log"
  echo "  流式探测与整份读取的时间轴对不上（预览/导出会分叉）"
  fail=1
fi

echo "== [33] 签名自检（v1.10.513 / 需求①）"
# 需求：只有"用发行钥匙签出来的包"进完整功能，其它签名进限制模式。
# 这一段的判据全在**源码**里（Integrity.java / App.java / 清单 / DiagCatalog），
# 所以它连"源码被改坏"也能拦；钥匙库不在场时（外部协作者）用 INTEGRITY_NO_KEYSTORE=1 降级为弱证据，
# 发版机器不设那个开关 —— 缺钥匙库就红。
if python3 "$ROOT/scripts/integrity-check.py" > "$TMP/integrity.log" 2>&1; then
  tail -1 "$TMP/integrity.log" | sed 's/^OK（/  OK（/'
else
  cat "$TMP/integrity.log"
  echo "  签名自检缺失或被改坏（指纹/两条取法/多签名者判据/调用点/清单 .App/K52 K53 登记）"
  fail=1
fi
# 守门人自己也必须被验证（LESSONS #3）：23 个用例逐个制造"应该红"的改动，
# 确认门禁真的红在对应段落 —— 否则它就是一块"打印了但什么都没查"的牌子。
if python3 "$ROOT/scripts/integrity-check-selftest.py" > "$TMP/integrityself.log" 2>&1 \
   && grep -q "^INTEGRITY-SELFTEST OK" "$TMP/integrityself.log"; then
  echo "  OK（自检门禁正反对拍：$(grep -o '23 个用例[^）]*' "$TMP/integrityself.log" | head -1)）"
else
  tail -25 "$TMP/integrityself.log"
  echo "  自检门禁的姿态验证没过（用例该红的没红 / 该绿的没绿）"
  fail=1
fi

echo "== [34] 壁纸底图对位（v1.10.519）"
# 用户口径：「frame 用整块显示区」。旧写法（ImageView + centerCrop）以**预览控件自己**为框，
# 壁纸被垂直居中裁剪 → 整块上移 (bh-738)/2 像素（v1.10.518 实测 Δy=+839，不崩不报）。
# 这道门禁两头钉：①摆法是纯数学，脱机能对拍（视口顶端 → 目标顶端恒为 0）；
# ②结构上不许搬回旧写法（scaleType / 主线程解码 / 只在 onCreate 取一次 / 缺 API 26 回退）。
wp_dir="$TMP/wallpaperplace"
mkdir -p "$wp_dir"
if javac -nowarn -d "$wp_dir" \
     "$ROOT/src-v2/io/github/animwidget/WallpaperPlacement.java" \
     "$ROOT/tests/WallpaperPlacementTest.java" > "$TMP/wp.log" 2>&1 \
   && java -cp "$wp_dir" io.github.animwidget.WallpaperPlacementTest > "$TMP/wp-run.log" 2>&1 \
   && grep -q "^WALLPAPERPLACE OK" "$TMP/wp-run.log"; then
  N34="$(grep -o 'WALLPAPERPLACE OK [0-9]*' "$TMP/wp-run.log" | awk '{print $3}')"
  echo "  OK（${N34} 条断言：视口顶端→目标顶端恒为 0 / 矮图靠放大铺满 / 视差余量横裁 / 高图上下居中裁 / 缩放取整不露 1px 缝 / 视口出界不画）"
else
  cat "$TMP/wp.log" "$TMP/wp-run.log" 2>/dev/null | tail -20
  echo "  壁纸摆放算法不成立（预览与桌面会对不上）"
  fail=1
fi
if python3 "$ROOT/scripts/wallpaper-align-check.py" > "$TMP/wpalign.log" 2>&1; then
  tail -1 "$TMP/wpalign.log" | sed 's/^/  /'
else
  cat "$TMP/wpalign.log"
  echo "  壁纸底图的接线被改回去了（旧写法回潮 / 主线程解码 / 缺 API 26 回退）"
  fail=1
fi
# 守门人自己也要被验证（LESSONS #223）：9 个「坏但语法合法」的改动必须逐个红。
# 这一版加它就是被自己抓到的：三条判据原先只查「名字还在不在」，被注释喂饱 → 删掉调用点照样绿。
if python3 "$ROOT/scripts/wallpaper-align-selftest.py" > "$TMP/wpaself.log" 2>&1 \
   && grep -q "^WALLPAPERPLACE-SELFTEST OK" "$TMP/wpaself.log"; then
  echo "  OK（$(grep -o '10 个用例：1 正向 + 9 反向' "$TMP/wpaself.log" | head -1)）"
else
  tail -25 "$TMP/wpaself.log"
  echo "  结构门禁的姿态验证没过（该红的没红 / 该绿的没绿）"
  fail=1
fi

echo "== 结论"
if [ "$fail" -eq 0 ]; then
  echo "  OK"
else
  echo "  FAILED"
fi
rm -rf "$TMP"
exit "$fail"
