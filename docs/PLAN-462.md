# 462 实施清单：语义色编辑页（分段选择器迁移 + 预览虚线色预设 + 滚动/橡皮筋 + 联动置灰）

> ⚠️ v1.10.463 记录：本文的 UI 四项在 462 里做得不对（串页/复制/风格不符/缺色行），已被 `docs/PLAN-463.md` 取代；
> 下文只保留「已就绪的底层」与「落点定位」，不要再照着它改界面。

> 状态：**口径层已完成并有断言**（`OutlineColor.resolve()`，25 条，门禁 [23]）；
> UI 四项**未实施**。本文是给下一次修改用的精确落点，不含猜测。

## 0. 已经就绪的东西（不要重复实现）

| 能力 | 位置 | 说明 |
|---|---|---|
| 三档口径收敛 | `OutlineColor.resolve(mode, backgroundRgb, customRgb)` | 跟随壁纸 / 默认 / 自定义，UI 只调它 |
| 壁纸主色 | `OutlineColor.dominant(int[])` | 5 位/通道分桶，48×48 采样 |
| 默认档 | `OutlineColor.defaultFor(bg)` | 中性灰 + 亮度阈值 0.5 + 明暗偏移 |
| 现行接线 | `ConfigActivity.applyOutlineFromWallpaper()` | v1.10.460 的「跟随壁纸」，日志 `W43` |

## 1. 页面骨架（先做，风险最高的一步）

现状：`SettingsActivity.showColorEditor()`

- `sheet` = `R.layout.dialog_color_editor` 膨胀出来的 `LinearLayout`（约 4362 行）
- `shell` = `FrameLayout`，底下垫 `ColorAuroraView`（约 4384 行）
- `maxSheetHeight = screenH * 0.86f`（约 4396 行）
- `sheetLp = (screenW-36dp, WRAP_CONTENT, CENTER)`，`shell.addView(sheet, sheetLp)`（约 4400 行）

改法（**必须按顺序**）：

1. 新建 `ScrollView sv`；`sv.setFillViewport(true)`；`sv.setOverScrollMode(View.OVER_SCROLL_ALWAYS)`
   —— Android 12+ 的 `OVER_SCROLL_ALWAYS` 就是系统级橡皮筋拉伸，不需要自绘。
2. **把面板背景搬到 sv 上**（`sheet` 的背景置透明、padding 保留在 sheet）：
   否则滚动时圆角/底色会跟着内容一起走，视觉立刻穿帮。
3. `sheetLp` 改成 `(screenW-36dp, Math.min(内容高, maxSheetHeight), CENTER)`；
   高度用 `View.measure(UNSPECIFIED)` 量一次再夹，**不要**写成 WRAP（WRAP 下 ScrollView 永不滚动）。
4. `shell.addView(sv, sheetLp)`，再把 `sheet` 放进 `sv`。
5. 校验点：面板能在不改变外观的前提下上下滚动；到顶/到底有回弹；浅色/深色下圆角与底色都不跑。

## 2. 分段选择器迁移

- 源：滑块色表页里「仅动图选择器 / 全局」那个控件（先在 `SettingsActivity` 里搜它的构建代码）。
- 做法：**抽成独立控件类**（暂名 `SegmentedRow`），两处共用同一份代码 ——
  复制粘贴必然出现"两处行为不一致"，这是这个仓库反复踩过的坑（LESSONS 190 的同类）。
- 位置：`简单变体` 那个**容器**（`llPresets`）正下方，不是其标题文字下方。
- 样式：与页面现有卡片同一套圆角/描边/选中态配色，全部走 `ThemeColors`，
  在 `refreshThemeInPlace()` 里也要重刷（深/浅切换实时更新，不重建页面）。

## 3. 预览虚线色预设组件

- 语义色列表新增一个键 `outline`（`ThemeColors` 键表 + `ThemePrefs` 读写 + 语义色行）。
- `简单变体` 下方放一条预设组件（**复用 `SegmentedRow`**，三个选项）：
  `跟随壁纸` / `默认` / `自定义`
- 选中 `自定义` 时才显示取色入口，写入 `ThemePrefs` 的 `outline_custom`。
- 生效处：`ConfigActivity.applyOutlineFromWallpaper()` 里把
  `OutlineColor.outlineFor(dominant)` 改成 `OutlineColor.resolve(mode, dominant, custom)`。
  `W43` 日志加上 `档位=`。

## 4. 联动置灰（带叠化）

- 触发：`mode != CUSTOM`
- 目标：`手控面板` / `颜色预览` / `简单变体` 三块（**文字除外**）
- 做法：`alpha 1.0 ↔ 0.38`，`animate().setDuration(180ms)`；同时 `setEnabled(false)` +
  `setClickable(false)`（只改 alpha 不挡事件，是假禁用）。
- 恢复时同样 180ms 叠化回来；不要用 `setVisibility`（会跳版）。

## 5. 门禁

- [23] 已覆盖口径层（25 条）。UI 三项建议各加一条静态断言：
  ① `showColorEditor` 里出现 `OVER_SCROLL_ALWAYS`；② 只允许一个 `SegmentedRow` 实现（不许复制）；
  ③ `mode != CUSTOM` 的分支里必须同时出现 `setEnabled(false)` 与 `animate()`。
