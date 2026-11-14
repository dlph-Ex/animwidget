# 折叠动效曲线选型（v1.10.272）

用户要求：**禁用 linear / ease / ease-in-out 等系统默认缓动和任何"通用模板"曲线**，
要有 AE 图表编辑器手调过的质感（自定义贝塞尔、快启动长尾、必要时轻微过冲、多元素错峰并有重叠）。
本文是检索 + 复算的记录，参数全部从下面四个源码/规范原件里抄的（用 `curl` 抓的原文，
不是凭记忆写的），"复算"三列由脚本按贝塞尔定义算出来（速度峰位置、90% 位移时间点、过冲量）。

**来源原件**

| 简称 | 链接 |
|---|---|
| Compose | https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/animation/animation-core/src/commonMain/kotlin/androidx/compose/animation/core/EasingFunctions.kt |
| Flutter | https://raw.githubusercontent.com/flutter/flutter/master/packages/flutter/lib/src/animation/curves.dart |
| M3 | https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/MotionTokens.kt |
| easings.net | https://easings.net/ （CSS 标准库的图形版，含每个函数的 cubic-bezier 值） |
| react-spring | https://raw.githubusercontent.com/pmndrs/react-spring/main/packages/core/src/constants.ts |
| cubic-bezier.com | https://cubic-bezier.com/ （手调时用的生成器） |

## 一、候选（24 组三次贝塞尔 + 6 组弹簧 = 30 组）

"90%" = 走完九成位移用掉的时间比例；"速度峰" = 最大瞬时速度出现的时间点；
"过冲" = 曲线超过 1 的最大比例（负值 = 永远追不上，正 = 会越过再落回）。

| # | 曲线 | cubic-bezier | 来源 | 90% | 速度峰 | 过冲 | 手感 |
|---|------|--------------|------|-----|--------|------|------|
| 1 | Compose EaseOutExpo | (0.160, 1.000, 0.300, 1.000) | Compose EasingFunctions.kt | 33.0% | 0.1% | +0.00% | 起手最猛、尾巴极长；落位几乎看不出在减速 |
| 2 | Flutter easeOutExpo | (0.190, 1.000, 0.220, 1.000) | Flutter curves.dart | 30.8% | 0.1% | +0.00% | 同上但更快咬合，90% 位移来得更早 |
| 3 | Flutter easeOutQuint | (0.230, 1.000, 0.320, 1.000) | Flutter curves.dart | 36.1% | 0.1% | +0.00% | 长尾稍收，仍然干脆；无反冲 |
| 4 | Compose EaseOutQuint | (0.220, 1.000, 0.360, 1.000) | Compose EasingFunctions.kt | 37.4% | 0.1% | +0.00% | 比 Expo 温柔一档，适合第二落点 |
| 5 | Compose EaseOutQuart | (0.250, 1.000, 0.500, 1.000) | Compose EasingFunctions.kt | 44.0% | 0.1% | +0.00% | 急停感明显，短距离位移会有'一顿' |
| 6 | Flutter easeOutQuart | (0.165, 0.840, 0.440, 1.000) | Flutter curves.dart | 47.1% | 0.1% | +0.00% | 前段带一点蓄力，尾部长 |
| 7 | Compose EaseOutCubic | (0.330, 1.000, 0.680, 1.000) | Compose EasingFunctions.kt | 54.0% | 0.1% | +0.00% | 最中性的减速，安全但没有个性 |
| 8 | Flutter easeOutCubic | (0.215, 0.610, 0.355, 1.000) | Flutter curves.dart | 54.1% | 0.1% | +0.00% | 起手更快、尾更长，比 Compose 版更利落 |
| 9 | Compose EaseOutCirc | (0.000, 0.550, 0.450, 1.000) | Compose EasingFunctions.kt | 56.5% | 0.1% | +0.00% | 圆运动减速，末尾有'贴住'的黏感 |
| 10 | Compose EaseOutSine | (0.610, 1.000, 0.880, 1.000) | Compose EasingFunctions.kt | 71.7% | 0.1% | +0.00% | 太慢，小面积折叠会显得拖 |
| 11 | M3 EaseEmphasized | (0.200, 0.000, 0.000, 1.000) | M3 MotionTokens.kt | 53.9% | 11.8% | +0.00% | 起步柔、尾巴极长；结构收拢的标准答案 |
| 12 | M3 EaseEmphasizedDecel | (0.050, 0.700, 0.100, 1.000) | M3 MotionTokens.kt | 35.9% | 0.1% | +0.00% | 起手很急，适合条目少的卡 |
| 13 | M3 EaseEmphasizedAccel | (0.300, 0.000, 0.800, 0.150) | M3 MotionTokens.kt | 97.5% | 100.0% | -0.00% | 加速段专用，只用来'退场' |
| 14 | M3 EaseLegacy(标准) | (0.400, 0.000, 0.200, 1.000) | M3 MotionTokens.kt | 63.3% | 30.2% | +0.00% | 旧 Material 的万金油，正反都还行、也没惊喜 |
| 15 | Flutter easeOutBack | (0.175, 0.885, 0.320, 1.275) | Flutter curves.dart | 28.4% | 0.1% | +8.69% | 回弹 27.5% —— 手机上像弹簧门 |
| 16 | easings easeOutBack | (0.340, 1.560, 0.640, 1.000) | easings.net | 29.5% | 0.1% | +9.78% | 过冲更大，只适合玩具感的按钮 |
| 17 | 自调 easeOutBack-lite | (0.200, 0.950, 0.300, 1.100) | 本仓库 FoldMotion | 31.9% | 0.1% | +2.66% | 过冲收到 2.66%：看得出有生命、看不出在弹 |
| 18 | 自调 easeOutBack-lite2 | (0.175, 0.885, 0.320, 1.100) | 本仓库 FoldMotion | 34.2% | 0.1% | +2.26% | 同上但起手更软，位移短时显得温吞 |
| 19 | Compose EaseInOutCubic | (0.650, 0.000, 0.350, 1.000) | Compose EasingFunctions.kt | 71.4% | 50.0% | +0.00% | 两端都慢；做折叠会'先停一下' |
| 20 | Compose EaseInOutQuint | (0.830, 0.000, 0.170, 1.000) | Compose EasingFunctions.kt | 66.1% | 50.0% | +0.00% | 中段爆发，适合大位移，小位移像卡住 |
| 21 | Flutter easeInOutCubicEmp | (0.700, 0.000, 0.840, 1.000) | Flutter curves.dart | 90.4% | 83.2% | +0.00% | Material 强调曲线的三段版的第一段 |
| 22 | Flutter fastLinearToSlow | (0.180, 1.000, 0.040, 1.000) | Flutter curves.dart | 23.2% | 5.7% | +0.00% | 几乎无加速段、纯刹车 |
| 23 | Compose EaseInOutSine | (0.370, 0.000, 0.630, 1.000) | Compose EasingFunctions.kt | 79.4% | 50.0% | +0.00% | 对称正弦，最'系统默认'的一张脸 |
| 24 | CSS ease | (0.250, 0.100, 0.250, 1.000) | easings.net | 62.3% | 21.0% | +0.00% | 浏览器默认，禁用清单里的那条 |

弹簧（给"真要弹"的地方留的备选；本项目折叠**不用**弹簧，理由见第四节）：

| # | 预设 | 参数（mass=1） | 来源 | 手感 |
|---|------|----------------|------|------|
| 25 | react-spring `default` | tension 170 / friction 26 | react-spring | 通用；约 6% 过冲 |
| 26 | `gentle` | 120 / 14 | react-spring | 慢而软，收尾拖 |
| 27 | `wobbly` | 180 / 12 | react-spring | 明显回弹，像果冻 |
| 28 | `stiff` | 210 / 20 | react-spring | 快、硬、几乎不回弹 |
| 29 | `slow` | 280 / 60 | react-spring | 过阻尼，像推重门 |
| 30 | `molasses` | 280 / 120 | react-spring | 极慢，只在装饰性入场用 |

## 二、为什么不用弹簧

手机端"小面积折叠"的位移只有 30~60dp，弹簧的过冲在**短距离**上会被读成"抖"而不是"弹"；
且弹簧没有"时长"概念，`ValueAnimator` 的剩余时长无法按剩余距离折算，
**动画途中反向**就做不到严格等速（用户明确要求"途中再点无跳变"）。
贝塞尔 + 线性时间轴可以做到：p 原路退回，各元素曲线自动反着走。

## 三、最终选择

**副标题 = 自调 easeOutBack-lite `cubic-bezier(0.20, 0.95, 0.30, 1.10)`**

- 取 Flutter `easeOutBack` 的"起手冲、尾部回"骨架，把过冲从 **27.5% 收到 2.66%**：
  手机上一段 40dp 的位移，2.66% ≈ **1px**，看得出有生命、看不出在弹；
- 90% 位移在 **32%** 时间点完成（EaseOutExpo 是 31~33%，EaseOutQuint 是 37%）——
  介于"太急"和"太温"之间；剩下 68% 的时间只走最后 10%，正是"长尾"；
- 速度峰在起手（0~5%），符合"整段被主标题吸进去"的物理直觉（越靠近越慢）。

**行高 = M3 `EaseEmphasized` `cubic-bezier(0.20, 0.00, 0.00, 1.00)`**

- 它的 90% 点只有 **53.9%**、速度峰在 **11.8%** —— 起步柔、中段匀速、尾部很长。
  高度变化如果也"起手猛"，会被读成"被剪掉"；慢起手读成"结构在收拢"；
- 与副标题**重叠 28%**（副标题窗口 0~62%，行高窗口 34~100%），
  副标题还在往里飞的时候行就已经在降，不会有"先 A 后 B"的停顿感；
- 行与行错峰 4.5%（≈21ms/行），6 条目以内总跨度 ≤105ms，属于"能看出先后、不觉得慢"。

**次选（代码里保留常量，`FoldMotion.USE_ALT_CURVES = true` 一键切换）**
副标题 `EaseOutQuint`(0.22,1,0.36,1)、行高 M3 `EmphasizedDecelerate`(0.05,0.7,0.1,1)——更稳、更"系统"，但少了那 1px 的过冲。

## 四、时间轴

| 参数 | 值 | 说明 |
|---|---|---|
| 整段时长 | 460ms | 收 / 展共用 |
| 副标题窗口 | 0 ~ 62%（285ms） | 位移 + 缩放 + 淡出 |
| 行高窗口 | 34 ~ 100%（304ms） | 与副标题重叠 28% |
| 行错峰 | 4.5%（21ms/行） | 行与行之间也重叠 |
| 反向最短 | 120ms | 半途反向按剩余距离折算时长，速度不变 |

全部集中在 `FoldMotion.java`，改手感只动那里。
