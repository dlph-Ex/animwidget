#!/usr/bin/env bash
# v1.10.279：组件「按什么比例铺」的判据自检（脱机，不需要 Android 运行时）。
#
# 守的是 v1.10.270~278 连续五版打转的那件事：调整大小后「画面被截断 / 四周一圈壁纸」。
# 病根不是公式难，而是同一条判据有**两个实现、两个作者**（合成层 + 桌面 scaleType），
# 而且「铺满」和「画面不裁」被当成同一层能同时做到的事（几何上不可能）。
#
# v1.10.279 的定案：
#   · 画布**永远**跟格子同形（FrameComposer 的 bleed：卡片=格子、画面完整居中、
#     四周用同一帧缩到 1/8 再双线性放大补满）→ 桌面的 scaleType 变成恒等，不再有第二作者；
#   · 只有固定比例布局（1:1 / 16:9 …）才可能需要 fitCenter（保卡片圆角）。
#
# 用法：scripts/layout-math-check.sh   （退出码 0 = 全部通过）
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

SRC="$ROOT/src-v2/io/github/animwidget/AspectMath.java"
WR="$ROOT/src-v2/io/github/animwidget/WidgetRender.java"
FC="$ROOT/src-v2/io/github/animwidget/FrameComposer.java"
WU="$ROOT/src-v2/io/github/animwidget/WidgetUriPush.java"
WP="$ROOT/src-v2/io/github/animwidget/WidgetProvider.java"

# ① 判据不许再被搬回 WidgetRender（搬回去就等于又开一个"作者"）
if grep -qE "Math\.abs\((ca|canvasAspect|sa) - (ba|boxAspect|b)\)" "$WR" "$FC" 2>/dev/null; then
    echo "  WidgetRender/FrameComposer 里又出现了手写的比例差 —— 判据必须只有 AspectMath 一处"
    exit 1
fi

# ② 应用侧不许再出现"旧画布退 fitCenter"的写法（v1.10.272 的那条腿，会把画面缩成中间一小块）
if grep -q "fitNow || lose" "$WR"; then
    echo "  WidgetRender 又拿 fitCenter 兜底旧画布了 —— 那会让画面缩成中间一小块"
    exit 1
fi

# ③ v1.10.280：衬底已撤销（透明底素材会透出一圈灰雾 "频散阴影"），谁加回来就红
if grep -q "boolean bleed = layout" "$FC"; then
    echo "  FrameComposer 又把衬底加回来了 —— 透明底素材会出轮廓灰雾（用户："啥阴"）"
    exit 1
fi

# ③' v1.10.283：**位图的 intrinsic 必须垫到不小于格子**（这轮"只贴左边"的最终解）。
# 桌面 AT_MOST 量 RemoteViews 时 ImageView 取 min(drawable.intrinsic, specSize)，
# intrinsic = 位图像素 × density/160 —— 位图不够大，视图就比格子小一圈（70%/85%/60% 全是它）。
# 钉死尺寸（setViewLayoutWidth）vivo 实测会忽略，所以必须靠这条"下限"自愈。
grep -q "coverFloor" "$WR" || {
    echo "  WidgetRender 里没有 coverFloor —— 位图又会小于格子，桌面按 intrinsic 摆成左上角一块"
    exit 1
}
grep -q "return coverFloor(context, c, box)" "$WR" || {
    echo "  canvasFor 没有把画布过 coverFloor（垫底没生效）"
    exit 1
}
if grep -q "setViewLayoutWidth" "$WU"; then
    echo "  WidgetUriPush 又在钉死视图尺寸 —— vivo 忽略该调用，钉了反而可能钉错（改用 coverFloor）"
    exit 1
fi

# ③'' v1.10.322（用户："导出后未播放前有 1 帧先小一圈再恢复"）：顶旧画布/压小位图都必须带格子下限。
# 位图比格子小 → vivo 按 intrinsic 把整张卡片量小一圈。两条路都不许再出现"不带下限"的调用。
grep -q "nearestAnyCanvas(context, config, index, size\[0\], size\[1\]," "$WR" || {
    echo "  WidgetRender 顶旧画布时没有传格子下限 —— 小于格子的旧画布会被推上去（卡片被量小一圈）"
    exit 1
}
grep -q "retrySmall(context, manager, base, config.getWidgetId(), bitmap2, minBmpW, minBmpH)" "$WR" || {
    echo "  retrySmall 没有传下限 —— 压小的位图会变成\"先小一圈\""
    exit 1
}

# ④ 原始比例的画布必须跟格子同形（不许再拿「素材/卡片形状」当画布）
if grep -q "canvasForSource(basis" "$WR"; then
    echo "  renderCanvas 又在按「卡片」算画布了 —— 画布必须永远跟格子同形"
    exit 1
fi
# v1.10.292：这里原来 grep 注释文字（"原始比例也要跟格子同形"），
# 交接时对方重写了注释 → 门禁误报。改成查**代码**：frameCanvas 里必须有 eff==FIT 的短路。
grep -q "if (eff == LayoutMode.FIT)" "$WR" || {
    echo "  frameCanvas 没有对原始比例短路（会走「画布无关帧」通道，画布就跟格子不同形了）"
    exit 1
}

# ④' v1.10.286：尺寸一变必须当场重合成（否则桌面停在旧画布上被拉伸 = "没实时渲染"）
grep -q "sizeStale" "$WR" && grep -q "notePushedSize" "$WR" && grep -q "sizeStale" "$WU" || {
    echo "  拖动改尺寸时不会当场重合成 —— 桌面会停在旧画布上被拉伸（用户：变长条没实时渲染）"
    exit 1
}

# ⑤ 占位图必须能被"下一拍当场合成"顶掉（v1.10.279 的 ①：导出后先显示应用图标）
grep -q "showingPlaceholder" "$WR" && grep -q "notePlaceholder" "$WP" || {
    echo "  占位图状态没有接进 render/send —— 保存后首帧会被「这拍不推」吞掉，桌面停在应用图标上"
    exit 1
}

cat > "$OUT/LayoutMathTest.java" <<'EOF'
package io.github.animwidget;

public final class LayoutMathTest {
    private static int pass = 0;
    private static int fail = 0;

    private static void eq(String what, boolean got, boolean want) {
        if (got == want) {
            pass++;
            System.out.println("  OK " + what);
        } else {
            fail++;
            System.out.println("  FAIL " + what + "：got=" + got + " want=" + want);
        }
    }

    private static void near(String what, float got, float want) {
        if (Math.abs(got - want) <= 1e-4f) {
            pass++;
            System.out.println("  OK " + what);
        } else {
            fail++;
            System.out.println("  FAIL " + what + "：got=" + got + " want=" + want);
        }
    }

    /** 和 WidgetRender 的调用点同一口径：w/(float)h。 */
    private static float r(int w, int h) {
        return w / (float) h;
    }

    public static void main(String[] args) {
        // ---- diff：非法输入必须是 -1（调用方按"不知道"处理，绝不当 0） ----
        near("diff(0,1)", AspectMath.diff(0.0f, 1.0f), -1.0f);
        near("diff(-1,1)", AspectMath.diff(-1.0f, 1.0f), -1.0f);
        near("diff(1,1)", AspectMath.diff(1.0f, 1.0f), 0.0f);
        near("diff 1:1 vs 16:9", AspectMath.diff(1.0f, 16.0f / 9.0f), 0.7778f);

        // ---- 桌面 scaleType：画布与格子同形时必须是 centerCrop（恒等）----
        // 原始比例的画布现在就是格子形状 → 这一族断言就是"永远恒等"
        eq("画布=格子(1096x1096) → centerCrop", AspectMath.needFitCenter(r(1096, 1096), r(1096, 1096)), false);
        eq("画布=格子(813x1079) → centerCrop", AspectMath.needFitCenter(r(813, 1079), r(813, 1079)), false);
        eq("画布=格子(597x626) → centerCrop", AspectMath.needFitCenter(r(597, 626), r(597, 626)), false);
        // 1% 的误差仍是 centerCrop（宁可裁 0.5% 也不能留一圈壁纸）
        eq("画布 1% 差 → centerCrop", AspectMath.needFitCenter(1.0f, 1.01f), false);
        // 5% 以内仍是 centerCrop：每边最多裁 2.5%，肉眼看不出
        eq("画布 4.9% 差 → centerCrop", AspectMath.needFitCenter(1.0f, 1.049f), false);
        // 6% 以上才改 fitCenter
        eq("1.0 vs 1.06(6%) → fitCenter", AspectMath.needFitCenter(1.0f, 1.06f), true);
        // 16:9 卡片进方格子 → fitCenter（保圆角，四周壁纸是设计）
        eq("16:9 画布 vs 方格子 → fitCenter", AspectMath.needFitCenter(r(1920, 1080), r(1030, 1030)), true);
        // 非法输入 → fitCenter（保守：宁可小一圈也不切边）
        eq("非法比例 → fitCenter", AspectMath.needFitCenter(0.0f, 1.0f), true);

        // ---- 遍历：只要画布是格子形状，桌面就必须是 centerCrop（恒等，不再二次裁切） ----
        int[][] boxes = {{1096, 1096}, {597, 626}, {1030, 1030}, {813, 1079}, {813, 626}, {380, 1988}};
        for (int[] b : boxes) {
            eq("恒等 " + b[0] + "x" + b[1], AspectMath.needFitCenter(r(b[0], b[1]), r(b[0], b[1])), false);
        }

        // ---- 固定比例布局：卡片比例 ≠ 格子比例时必须 fitCenter ----
        float[][] fixed = {{1.0f, 1.0f}, {16.0f / 9.0f, 4.0f / 3.0f}, {4.0f / 3.0f, 1.0f}, {9.0f / 16.0f, 1.0f}};
        for (float[] f : fixed) {
            eq("固定比例 " + f[0] + " vs 格子 " + f[1],
                    AspectMath.needFitCenter(f[0], f[1]), f[0] != f[1]);
        }

        // ---- v1.10.497：画布 → 格子的显示倍率（centerCrop = cover = max(格子/画布)）
        //      外部评审给的真实数字：画布 1280x720、格子 869x869 → 869/720 = 1.2101
        near("桌面倍率 720p 画布 vs 869 方格", AspectMath.displayScale(1280, 720, 869, 869), 1.2069444f);
        near("桌面倍率 1:1 画布（恒等）", AspectMath.displayScale(869, 869, 869, 869), 1.0f);
        near("桌面倍率 720p 画布 vs 1080 方格", AspectMath.displayScale(1280, 720, 1080, 1080), 1.5f);
        near("桌面倍率 画布比格子大 → <1（更锐）", AspectMath.displayScale(1280, 1280, 640, 640), 0.5f);
        near("桌面倍率 非法输入", AspectMath.displayScale(0, 720, 869, 869), 0.0f);
        eq("放大门槛 1.21 应判为「会糊」", AspectMath.displayScale(1280, 720, 869, 869) > AspectMath.DISPLAY_BLUR, true);
        eq("恒等 1.00 不应判为「会糊」", AspectMath.displayScale(869, 869, 869, 869) > AspectMath.DISPLAY_BLUR, false);

        System.out.println("  断言 " + (pass + fail) + " 条：通过 " + pass + "，失败 " + fail);
        if (fail > 0) {
            System.exit(1);
        }
        System.out.println("LAYOUTMATH OK");
    }
}
EOF

javac -nowarn -d "$OUT" "$SRC" "$OUT/LayoutMathTest.java" || {
    echo "  编译失败（AspectMath / LayoutMathTest）"
    exit 1
}
java -cp "$OUT" io.github.animwidget.LayoutMathTest
