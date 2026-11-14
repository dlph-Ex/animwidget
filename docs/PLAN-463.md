# 463 实施记录：语义色编辑页（四项否决项的修法）

> 状态：**已实施、已出包**（vc577 / 1.10.463）。门禁新增 [24]（33 条断言，已做负例验证）。
> 462 被用户整体否决（「462 不要用」），本文件只记「462 错在哪 → 463 怎么改 → 怎么验」。

## 1. 四个否决项 → 四条结构性修法

| # | 462 的错 | 463 的改法 | 门禁断言 |
|---|---|---|---|
| 1 | 两条选择器无条件插进共用弹窗，点每一行颜色都能看到 | `final SegmentedRow modeRow = isOutline ? new SegmentedRow(this) : null;`、`scopeSwitch` 同理；插入各自待在 `if (modeRow != null)` / `if (scopeSwitch != null)` 块里 | [24] ③（含两条互不串味） |
| 2 | 只做了 `OutlineColor.resolve`，语义色列表里没有「预览虚线色」这一行 | `buildColorRows()` 末尾新增 `addColorRow("预览虚线色", "outline", OutlineSettings.currentColor())`；存储进 `ThemePrefs`（`outline_mode` / `outline_custom`） | [24] ①⑤ |
| 3 | 「迁移」做成了复制：首页滑块色行还挂着作用范围开关 | `addColorRow` 里那段开关代码整段删除；`buildSliderScopeSwitch` 只被编辑页调用（定义 1 + 调用 1） | [24] ④ |
| 4 | `SegmentedRow` 满宽横条、跳变无过渡 | 重做：高度 38dp、圆角 = 高度一半、外边距 10dp；材质逐项对齐页面胶囊（轨道 = 中性 55%、1dp 正文 12% 发丝线、选中块 = 主色 78%、选中字 = onPrimary）；选中块用 `ValueAnimator` + `Ease.IN_OUT` **180ms** 滑过去，文字色按覆盖度插值 | [24] ⑥ |

## 2. 新增/改动的文件

| 文件 | 改动 |
|---|---|
| `src-v2/.../ThemePrefs.java` | 新增 `outline_mode` / `outline_custom` 读写（**唯一存储口**）+ `hasOutlineCustom` |
| `src-v2/.../OutlineSettings.java` | **新类**：档位读写、自定义色、壁纸主色采样（48×48 + 进程内缓存）、`currentColor()` = `OutlineColor.resolve` |
| `src-v2/.../SegmentedRow.java` | 重做（形状/材质/滑动动画），颜色由 `ThemeManager.paint` 下发 |
| `src-v2/.../ThemeManager.java` | `paint()` 增加 `instanceof SegmentedRow → applyColors` |
| `src-v2/.../SettingsActivity.java` | 新增「预览虚线色」行；两条控件按 key 建/插；删掉首页开关；删掉 462 的外层 ScrollView（面板进 shell，滚动只留布局里那个 ScrollView + `OVER_SCROLL_ALWAYS`）；新增 `saveOutlineColor()`（档位 + 自定义色一次写） |
| `src-v2/.../ConfigActivity.java` | `applyOutlineFromWallpaper()` 改走 `OutlineSettings.mode/customRgb`，并把采样结果 `noteWallpaperBg()` 推给设置页 |
| `scripts/color-editor-scope-check.py` | **新门禁**（33 条断言，负例验证过 3 条） |
| `scripts/verify-apk.sh` | 注册为 [24] |

## 3. 语义色编辑页现在的行为（用户口径）

- 列表最后一行：**预览虚线色**，色块 = 当前真正画出来的虚线色（与预览框同一个函数算出来）。
- 点开这一行：只有它有三档选择器（跟随壁纸 / 默认 / 自定义），位置在「简单变体」容器正下方；
  非「自定义」档时，手控面板 / 颜色预览 / 简单变体三块置灰（alpha 0.38、180ms 叠化、真的 `setEnabled(false)`）。
- 点开**滑块色**：只有它有作用范围开关（仅动图选择器 / 全局），改完立即写盘并重上色页面。
- 点开其它任何颜色：一条附加控件都没有（对象都不创建）。
- 保存语义：`保存` 才落盘（档位 + 自定义色）；`取消` 什么都不写。
- 置灰是**真禁用**：`setEnabled(false)` 之外，两个自绘控件（`HsvPickerView` / `SegmentedRow`）
  的 `onTouchEvent` 自己判 `isEnabled()` —— 否则灰着也能拖色（462 遗留的假禁用，[24] 有断言）。

## 4. 真机验收清单（建议顺序）

1. 设置 → 语义色：最后一行能看到「预览虚线色」，色块颜色和配置页预览框虚线一致。
2. 点开它 → 三档在「简单变体」下面，切换时选中块**滑**过去（180ms），非自定义档三块变灰且点不动。
3. 选「自定义」→ 手控可用 → 选一个颜色 → 保存 → 回配置页看虚线是否就是这个色（日志 `W43 档位=CUSTOM`）。
4. 点开「滑块色」→ 只有作用范围开关；首页那一行**不再**有开关。
5. 点开「主色 / 表面色 / 错误色」→ 页面上除了取色器和简单变体，没有任何附加控件。
6. 深浅色切换（系统）时三档选择器与作用范围开关跟着换色（不重建页面）。
