# 464 实施记录：三条界面反馈（预览同源 / 新形态 / 作用范围换成同一控件）

> 状态：**已实施、已出包**（vc578 / 1.10.464）。门禁 [24] 扩到 **46 条断言**（新增 12 条，含负例验证）。

## 1. 预览虚线色：首页色块与编辑页预览块「同一个数」

**用户原话**：选择非自定义时，首页颜色预览也要与编辑页相同预览颜色。

**原因**：463 只在「切到自定义」时才重画编辑页预览；切到「跟随壁纸 / 默认」时预览块停在旧色
（往往是上一次的自定义色），而首页色块是按档位算的 —— 两边必然对不上。

**改法（结构，不是补丁）**：
- `OutlineSettings.colorForMode(context, index)`：某一档在当前壁纸下会画成什么颜色；
- `currentColor()` 改为 `colorForMode(modeIndex)` —— 首页色块走它；
- 编辑页切**任何**一档都调 `colorForMode` 重画预览块 + HEX —— 预览走同一个函数。
- 「自定义」档且没存过色时仍保持当前色（切档不跳色），一旦保存就写进 `outline_custom`，
  之后两边依旧是同一个数。

## 2. 新组件形态：平行四边形 + 四角 45° 切角，靠左

**用户原话**：外观从圆角改为四边 45° 圆角平行四边形，并且此组件应靠左，而不是居中。

- `SegmentedRow` 的轮廓改成 **8 点折线**（`buildTrack`）：上边相对下边右移 `SKEW_DP = 5dp`
  （平行四边形），四个角各沿 45° 直切 `CHAMFER_DP = 5dp`（不是圆弧）；
- 高度 34dp；选中块不再是圆角矩形，而是**一条垂直色带被轨道形状裁剪**（`clipPath`）——
  落在两端时自动带上斜边与切角，动画过程中也不会有「方块压出轮廓」的破绽；
- **靠左**：`insertUnderVariants` 改成 `WRAP_CONTENT` + `gravity = START`（RTL 下自动靠右），
  宽度按文字实时量（`setLabels` 里 `measureText` + 段内 13dp padding），不再是满宽横条。

## 3. 滑块色的分段选择器换成同一控件

**用户原话**：图17→滑块色的分段选择器并没有适配新组件。

- `buildSliderScopeSwitch` / `SliderScopeSwitch.java` **整份删除**：
  作用范围改用 `buildScopeRow()` → 同一个 `SegmentedRow`，只是两段文字换成「仅动图选择器 / 全局」；
- `ThemeManager.paint` 里那条 `instanceof SliderScopeSwitch` 一起删掉（否则编译不过）；
- `verify-apk.sh` [2] 的「故意删除」白名单加 `ALLOW_GONE_5` 并写明原因；
  `theme-gate.py` 的 PAINT_ALLOW 里那条过期的 `SliderScopeSwitch` 也移除了。

现在全仓库**只有一份分段选择器实现**（门禁断言：`SliderScopeSwitch` 不许有任何代码引用）。

## 4. 门禁 [24] 新增的断言（46 条）

| 断言 | 防的是什么 |
|---|---|
| `CHAMFER_DP ≥ 3`、`SKEW_DP ≥ 2` | 切角/斜度被改小到看不出 → 又变回矩形 |
| `buildTrack` 用 `moveTo/lineTo`、全类无 `drawRoundRect` | 偷偷退回圆角胶囊 |
| `onDraw` 里 `clipPath(` | 选中块不被轨道裁剪 → 两端冒出方块 |
| `insertUnderVariants` 必须 `WRAP_CONTENT` 且 `Gravity.START`、不许 `MATCH_PARENT` | 「靠左」被改回满宽 |
| `currentColor` 必须走 `colorForMode` | 首页色块和预览块分头算 |
| 切档块里必须有 `OutlineSettings.colorForMode(` + `showColorEditor$render(` | 非自定义档预览不跟着档位 |
| `buildScopeRow` 里必须 `new SegmentedRow(`、`SliderScopeSwitch` 零引用 | 作用范围又用回另一套外观 |

负例实测：把 `CHAMFER_DP` 改 0 → FAILED；把 `WRAP_CONTENT/START` 改回 `MATCH_PARENT` → 两条 FAILED。

## 5. 真机验收（这次只需要看三眼）

1. 语义色 → 预览虚线色：切「跟随壁纸 / 默认」时，编辑页的色块**立刻**变成该档的颜色；
   保存后回首页，这一行的色块与刚才编辑页看到的是同一个颜色。
2. 同一页：控件是**斜边 + 四个 45° 切角**的平行四边形，宽度只包住文字、贴在左边。
3. 滑块色：作用范围也是同一个样式（不再是一条满宽圆角胶囊）。

---

## 6. 追加（v1.10.465）：选中块也是「同一个圆角平行四边形」

**用户原话**：选中图标也请改为同圆角平行四边形，而不是直角四边形。

464 的选中块是「一条**矩形**色带被轨道形状裁剪」：静止在中段时两条竖直边就是直角，
落在两端才顺带贴上斜边 —— 用户看到的正是中间段那个方形高亮。

改法（结构，不是描边）：
- `buildTrack` 改名为 `buildPolygon(out, left, top, right, bottom)`：把「盒子」当参数；
- 轨道 = `buildPolygon(trackPath, 0, 0, w, h)`；
- 选中块 = `buildPolygon(pillPath, left, pad, right, h - pad)`，盒子随动画在两根刻度间移动/变宽；
- 仍然 `clipPath(trackPath)` 裁一刀（小屏/大字体下盒子可能略超出轨道）；
- 不再有 `drawRect` —— 选中块在任何位置都是同一副 8 点轮廓。

门禁 [24] 相应改成：`onDraw` 里 `buildPolygon(` 至少出现 2 次、必须 `drawPath(this.pillPath`、
**不许** `drawRect(`、仍然必须 `clipPath(`。负例实测：把 pill 改回 `drawRect` → 两条 FAILED。

---

## 7. 追加（v1.10.466）：四个角改成**真圆弧**

**用户原话**：都不是圆角（附截图圈出四角的 45° 直切）。

465 那版把「45° 圆角」实现成了**沿 45° 直切**（chamfer）：轮廓是八边形，圆起来的地方一刀没有 ——
用户看到的就是「切角」，不是「圆角」。

改法：
- `buildPolygon` 改成**圆角平行四边形**：4 个顶点（上边右移 `SKEW_DP`），每个顶点取两条边上的
  切点 `d = r / tan(θ/2)`（θ 为内角，90° 时 d = r），再用**二次贝塞尔**（控制点 = 顶点）倒圆；
- 半径 `CORNER_DP = 9dp`，按盒子夹（`min(r, h/2, w/3)`），窄的选中块也不会倒成一团；
- 切点距离再夹 `0.45 × 最短边`，保证相邻两个圆角不会互相吃掉；
- 轨道与选中块**仍是同一个函数**（v1.10.465 那条不变量保留）。

门禁 [24]（50 条）：`CORNER_DP ≥ 5`、`buildPolygon` 里必须有 `quadTo(`（圆弧）、`onDraw` 里
`buildPolygon(` ≥ 2 次、必须 `drawPath(pillPath`、不许 `drawRect`。负例实测见 `LESSONS.md` 196。

---

## 8. 追加（v1.10.467）：没点「保存」就被写盘

**用户原话**：没点保存，会被自动保存。

原因：滑块色编辑页里那条「作用范围」选择器，点一下就直接 `ThemePrefs.setSliderScope(...)` 并立刻
重上色 —— 它沿用的是 464 之前 `SliderScopeSwitch` 的老行为，而对话框的语义是**保存才落盘**。
于是「改作用范围 → 点取消 → 再进来」还是新的。

改法：
- 点击只改**面板内**的选中态（控件自己滑过去），日志明确写「面板内预览，保存才落盘」；
- 写盘挪进 `save.setOnClickListener`：`ThemePrefs.setSliderScope(this, scopeRow.getSelected())`，
  然后走通用保存（叠化 + `refreshThemeInPlace` 让页面按新范围重上色）；
- 取消 → 一个字节都不写。

同一页的「预览虚线色」本来就是保存才写（档位 + 自定义色都在 `saveOutlineColor` 里），这次一并
写成门禁断言，防止以后又在点击回调里塞一句写盘。

门禁 [24] 新增 3 条（共 53 条）：
- `if (scopeRow != null) { … }` 里**不许**出现 `setSliderScope(`；
- `save.setOnClickListener` 里**必须**有 `setSliderScope(`；
- 虚线色分支里不许出现 `setOutlineMode(` / `setCustomRgb(`。

负例实测：把 `setSliderScope` 塞回点击回调 → 当场 FAILED。

---

## 9. 追加（v1.10.468）：重画「重置」符号

**用户原话**：重新绘制语义色重置符号 / 头部箭头怎么老是不在身子中央。

205 那版的几何是错的：

```java
path.moveTo(px, py);                       // 尖端放在**弧端点**上
path.lineTo(px - dx*len + nx*half, ...);   // 三角形向后铺开 → 压在环的笔画上
```

于是箭头成了「贴在环上的一块楔子」，而且只按**圆心**居中 —— 楔子凸出的那一截把视觉重心带偏，
整个图标在胶囊里看着偏下。

现在的几何：

| 项 | 值 |
|---|---|
| 环 | 半径 `0.42g`、描边 `1.9dp`、逆时针 `290°`、缺口在左上 |
| 箭头 | **底边中点 = 弧端点**，尖端沿切线伸出 `0.30g`，半宽 `0.16g`（实心三角） |
| 居中 | 把「弧 + 描边一半 + 箭头」的**外接框**中心平移到控件中心（`computeBounds`） |

门禁 [24] 加 3 条（共 56 条）：RESET 分支必须 `addArc(`、`moveTo(px + tx * len`（尖端伸出）、
`computeBounds(`（外接框居中）。

真机验收：语义色 → 点滑块色 / 错误色 → 左下角那个重置按钮，箭头应在环的笔画中线上、
整个图标在胶囊里居中。
