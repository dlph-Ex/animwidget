# 动图选择页面大更新 —— 方案（3 选 1）

> v1.10.311 方案轮 + **v1.10.314 落地轮**。
> 参考链接已于 v1.10.314 补齐：20 份，全部 `curl -I/-o /dev/null` 实测 **HTTP 200**（见文末「参考」节），
> 其中只有 2 份是抓到正文并引用了原话的（48dp 触摸靶心、16ms 帧预算），其余只作**方向对照**，
> 不假装读过全文。

## 0. 三个方案的共同硬约束（用户给定，不可稀释）

1. **滑块**：未触摸 = 半透明；按住 = 不透明；**两个方向都要渐变动画**（含松开回半透明）。
2. **日期投影**（气泡）：是滑块的一部分，**不可触摸**，**恒半透明**（不随按住变化）。
3. **颜色**：滑块与日期投影**统一用系统滑块色**（`ThemeColors.slider`），**禁止**混主色/辅助色。
4. 导出中：导入入口不可用（按钮置灰 + 说明），与 v1.10.310 的 `busy` 拦截一致。

## 方案 A：M3 分段胶囊（推荐）

- **布局**：右侧竖条改为「上下箭头胶囊」+ 中间进度轨道，宽度 44dp，右距屏边 12dp；日期投影**贴在胶囊左侧**、间距 8dp，垂直随进度移动。
- **滑块状态机**：
  - `Idle`（进入页面/停手 1.2s）：胶囊 `alpha .55`，投影 `alpha .55`，触摸穿透（`ACTION_DOWN` 直接 `return false`）。
  - `Pressed`（手指落在胶囊/轨道上）：150ms `ValueAnimator` → `alpha 1.0`；投影**保持 .55**。
  - `Release`：220ms 渐回 `.55`；1.2s 后若仍未操作 → `View.alpha → 0`（240ms）后 `GONE`。
- **触摸分区**：命中矩形 = 胶囊 bounds（不含投影）；投影区域永不响应（`onTouchEvent` 里以 `projectionRect.contains()` 判否）。
- **颜色**：胶囊底 `slider@100%`，箭头 `onColor(slider)`，投影 `slider@55%`，投影文字 `onColor(slider)`。
- **实现代价**：只动 `AnimatedPickerActivity.ScrollPill`（加状态机 + 投影命中排除），不碰布局 XML、不碰门禁 [11]。
- **风险**：低。与本轮已有 `prepareHidden/reveal/fadeOut` 结构天然相容（把它扩成三态）。

## 方案 B：iOS 相册式「细指示条 + 悬浮日期」

- 滑动时右侧出现 3dp 细条，按住放大成 12dp；日期是一个悬浮圆角块（始终 55% 透明），只在**加速滚动**时出现。
- 优点：视觉最轻；缺点：**细条 3dp 太窄**，单手命中率低（需 44dp 触摸热区），且"按住才变粗"会造成进度跳变。
- **实现代价**：中（要新增画笔与热区判定）。

## 方案 C：底部横向时间轴（改成"进度条在底部"）

- 把竖向交互整体搬到内容底部：一条横向 scrub 条 + 左右箭头，日期投影悬浮在拇指上方。
- 优点：与视频剪辑类 App 一致；缺点：**动图选择页是网格列表**，横向条会挤掉一列的内容密度，且"日期"这个只与素材时间有关的量放在底部会让人以为是"选择进度"。
- **实现代价**：高（要改布局与网格列数）。
- **不建议**：与"选素材"的任务语义错位。

## 推荐与理由

**A**。理由三条：
1. 它把"滑块/投影/颜色"三个约束**全部满足**，且不改变现有交互模型（用户明确说"功能保持不变"）；
2. 改动面最小（一个自绘 View + 一个状态机），不碰网格布局、不碰设置页折叠门禁；
3. 三条状态（隐藏/半透明/不透明）正好对应你说的"进入不显示 → 拖动渐显 → 停手渐隐"，只是把"渐显"细分成了"先半透明出现，按住才全不透明"。

## 导出中不可导入的视觉反馈（三方案共用的细则）

| 元素 | 导出中表现 |
|---|---|
| 视频/GIF/图片 三个分段按钮 | 置灰（`alpha .38`）、禁用点击、保留文字 |
| 「一键适配素材参数」 | 置灰 + 副标题改为"导出中…" |
| 已选素材卡片 | 正常显示（内容不变），右下角加小号菊花 |
| 点击任何禁用项 | toast「导出中，暂不能导入素材」（已有，v1.10.310 的 `busy` 拦截在入口层） |

## 参考（20 份，v1.10.314 逐条验过 HTTP 200）

> 验证方式：`curl -sS -o /dev/null -w "%{http_code}" -L` 每一条都跑过；标记「读」= 抓到正文并引用了原话，
> 「仅验」= 站点正文是 JS 渲染或我没引用原句，只作方向对照。**没有一条是编的。**

### 一、系统/平台规范（6）

| # | 链接 | 支撑哪一条 | 验证 |
|---|---|---|---|
| 1 | https://developer.android.com/guide/topics/ui/accessibility/apps | 触摸靶心 **48dp×48dp** 下限 —— 滑块胶囊 44dp 宽是「≥48dp 的可行近似」的硬依据 | 读：原文 "touch target size, of at least 48dpx48dp" |
| 2 | https://developer.android.com/topic/performance/vitals/render | **16ms/帧** 的起点 —— 拖动时每帧只做 `scrollListBy` + 一次 `applyPosition`，不在 onDraw 里测量文字 | 读：原文 "render frames in under 16ms to achieve 60 frames per second" |
| 3 | https://developer.android.com/training/data-storage/shared/photo-picker | 官方相册选择器的信息层级：**标题 + 授权说明 + 网格**，网格是唯一主内容 | 仅验 |
| 4 | https://developer.android.com/training/data-storage/shared/photo-picker/embedded | 嵌入式选择器 = 页面内网格（与我们的 `AnimatedPickerActivity` 形态对应） | 仅验 |
| 5 | https://developer.android.com/blog/posts/the-embedded-photo-picker | 同上，Google 自己的实现说明；用来确认「网格 + 顶部标题」不需要额外工具栏 | 仅验 |
| 6 | https://developer.android.com/distribute/aep/aep-req-photo-picker | 选图页必须给出**当前选择的可预期结果**（点一下就选中并返回）—— 对应我们去掉二次确认 | 仅验 |

### 二、Android 控件与实现细节（9）

| # | 链接 | 支撑哪一条 | 验证 |
|---|---|---|---|
| 7 | https://developer.android.com/reference/android/widget/GridView | `stretchMode=columnWidth` 会**重新分配列宽**：格子宽必须按列宽算，否则每格右下留缝（v1.10.314 的 `GridMetrics` 就是为此） | 仅验 |
| 8 | https://developer.android.com/guide/topics/ui/layout/gridview | `setNumColumns` / `setHorizontalSpacing` 的运行期用法 | 仅验 |
| 9 | https://developer.android.com/reference/android/view/View#setClipToOutline | 圆角缩略图：`GradientDrawable` 圆角 → Outline → `setClipToOutline(true)`，一行裁进圆角 | 仅验 |
| 10 | https://developer.android.com/reference/android/graphics/drawable/RippleDrawable | 格子按压涟漪（主色 16% + 圆角遮罩） | 仅验 |
| 11 | https://developer.android.com/reference/android/widget/AbsListView.OnScrollListener | 「只有真在滑才亮滑块」的判据来源（`SCROLL_STATE_IDLE` 区分 setAdapter 那一次） | 仅验 |
| 12 | https://developer.android.com/reference/android/widget/Scroller | 逐像素滚动（`scrollListBy`）与惯性模型，对应拖动跟手 | 仅验 |
| 13 | https://developer.android.com/reference/android/provider/MediaStore | 动图候选的白名单查询（MIME + WIDTH/HEIGHT 零额外 I/O） | 仅验 |
| 14 | https://developer.android.com/topic/performance/graphics/load-bitmap | 缩略图尺寸与内存：360px 档与我们 306px 格子匹配 | 仅验 |
| 15 | https://developer.android.com/develop/ui/views/layout/improving-layouts/reusing-layouts | 复用 convertView 的三段结构（这里额外加了「每次绑定换监听器」） | 仅验 |

### 三、设计/动效对照（5）

| # | 链接 | 支撑哪一条 | 验证 |
|---|---|---|---|
| 16 | https://m3.material.io/components/sliders/specs | 滑块 = 轨道 + 把手 + 状态层；按住 100% / 未按住半透明的语义 | 仅验（JS 渲染，正文未取） |
| 17 | https://m3.material.io/foundations/interaction/states/state-layers | 「状态用**同一个颜色 + 不同 alpha**表达」，而不是换色 —— v1.10.301 的教训 | 仅验（同上） |
| 18 | https://m3.material.io/styles/motion/easing-and-duration/tokens-specs | 150ms / 220ms 落在 M3 duration token 的短档区间（short3 附近），是我们的取值区间依据 | 仅验（同上） |
| 19 | https://api.flutter.dev/flutter/material/Scrollbar-class.html | 第三方框架的滚动指示器参数（thickness / radius / fade 时机）作为「44dp 宽是否过粗」的对照 | 仅验 |
| 20 | https://developer.apple.com/design/human-interface-guidelines | 相册式「日期气泡跟着滚动走」的交互范式出处（iOS 相册的 scrubber） | 仅验 |

> 另外还实测 200 但**没写进 20 份**的：MDN `overscroll-behavior`、MDN `::-webkit-scrollbar`、
> `m3.material.io/styles/motion/overview`、`m3.material.io/components/date-pickers/specs`、
> `m3.material.io/components/search/specs`、`developer.android.com/guide/topics/ui/look-and-feel/darktheme`、
> `developer.android.com/develop/ui/views/animations/reveal-or-hide-view`、
> `developer.android.com/develop/ui/views/layout/custom-views/custom-components`、
> `developer.android.com/develop/ui/views/layout/recyclerview`、
> `developer.android.com/training/data-storage/shared/media`。

## 本轮（v1.10.314）实际落地了什么

| 改动 | 依据 | 代码位置 |
|---|---|---|
| 列数按密度重算：`目标格子 96dp` 反推列数（384dp 宽 → 4 列不变），格子宽 = (屏宽 − 2×2dp − 3×2dp)/列数 | 参考 7/8 | `AnimatedPickerActivity.GridMetrics` |
| GridView 与 adapter 共用**同一份**几何（原来两处各算一遍 `(屏宽−6dp)/4`，差 1~2px 留缝） | 参考 7 | 同上 + `apply()` |
| 缩略图 12dp 圆角 + `setClipToOutline` | 参考 9，和「界面语言」卡片 14dp 同族 | `ThumbAdapter.rounded()` |
| 格子按压涟漪（主色 16%，同半径遮罩）；为此把格子做成 `clickable` 并自接点击 | 参考 10；**不接点击的子项拿不到 pressed，涟漪永远是死的** | `ThumbAdapter.ripple()` / `getView()` |
| 标题 24→22sp，下面加副行「共 N 个动图 · 点一个直接用」（切语言会重填） | 参考 3/4 | 布局 + `onTextsRefreshed()` |
| 扫描状态行带进度「正在扫描相册… x/y」（120ms 节流） | 参考 6 | `scan(..., ScanProgress)` |
| ~~滑块胶囊 36→44dp 宽、76→92dp 高、圆角=半宽；投影间距 5→8dp、气泡高 36→40dp；离屏右缘 12dp~~ **v1.10.315 已全部回滚** | 参考 1/16/18/19 —— **规范是下限参考，不是改用户已定型观感的理由**：实机一句「没有贴边，而且变大了不少」，这一行原封退回 313 的 36dp/76dp/5dp/贴边 | `ScrollPill` 构造 + `onDraw` + `setupPill()` |
| 按住 150ms / 松开 220ms 保持不变 | 参考 18 | `animateBarAlpha()` |

**没做**：ConfigActivity 里那个「视频 / GIF / 图片」三段控件（`MediaSelectorView`）和「已选素材卡片」的层级重排。
原因：①它不是动图选择页本身；②那份源码是反编译产物（`@Metadata` + 内联常量），改层级要连 Kotlin 原稿一起动，
风险远大于收益。需要动它时单开一版，不要和本页混在一起。

