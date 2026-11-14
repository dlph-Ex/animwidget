# 一页纸：把「帧」从「画布」里解放出来（坐标系换算）

> 目的：让**拖动尺寸不再触发任何一帧重编码**，从而同时满足
> ①画面不长时间停留 ②清晰度不减 ③刷新率不变 ④质量原始。
> 本文只讲「坐标系怎么换、哪些语义会变、什么时候必须回落」。代码未动。

---

## 0. 一句话

**帧的画布 = 素材比例矩形**（不再等于组件格子）；格子的留白/缩放交给桌面
`ImageView`（`fitCenter`/`centerCrop`）+ `RemoteViews.setViewLayoutWidth/Height`（API 31+）完成。

拖动尺寸 → 帧目录不变 → **0 张图重编码**。

---

## 1. 现在的几何（以 `FrameComposer.drawFrame` 源码为准）

```
W×H      = 画布 = launcher 给的格子像素（W10/F11 里那个 1030x853）
area     = contentRect(srcW,srcH,W,H,layout)
             FIT      → 素材比例内接
             固定比例 → 该比例内接；1:1 放进 1.208 的格子 = 853×853 居中（左右各 88px 透明）
box      = insetPreservingRatio(area, margin)      margin = min(W,H) × 3.5% × 阴影强度%
path     = ShapeFactory.path(shape, box.w, box.h)，平移到 box
           → shapeRotation 绕 (W/2, H/2) 转
           → maskOffsetX/Y 平移 box.w×x%/100, box.h×y%/100
canvas.clipRect(area); canvas.clipPath(path)
drawBox  = path 包围盒（**不含** mask 偏移）inset(-2)
drawContent(drawBox, offsetX%, offsetY%, contentScale%, rotation, fit)
阴影     沿 path 外扩；边框 沿 path 描边（宽 = min(W,H) × 1.8%）
```

**关键观察**：`box` 本身已经是「area 的比例」——也就是说蒙版一直是「按 area 比例」画的，
不是按整块画布。所以 **只要把 area 换成「素材矩形」，整条几何链天然成立**。

---

## 2. 新几何

```
L        = 帧长边 = clamp( min(素材长边, 1080), 64, 4096 )
fw × fh  = 素材比例，长边 = L        ← 这块矩形就是新的「画布」
area'    = 整块 fw×fh（不再内接任何框）
box'     = insetPreservingRatio(area', margin')     margin' = min(fw,fh) × 3.5% × 阴影强度%
path' / drawBox' / drawContent / 阴影 / 边框：**逻辑逐行不变**
```

**结论：`compose()` 里没有一行需要改公式，只需要换调用方传进来的 `width/height`**
（从「格子像素」改成 `fw×fh`）。改动集中在：

| 位置 | 现在 | 改后 |
|---|---|---|
| `WidgetPrewarm.ensure(ctx, cfg, w, h)` | 传格子像素 | 传 `fw×fh` |
| `WidgetUriPush.stamp()` | `…_w+"x"+h` | `…_L`（画布尺寸退出指纹） |
| `WidgetRender.render()` | `renderCanvas(格子)` | `frameCanvas(素材比例, L)` |
| `FrameComposer.compose(frame, w, h, …)` | 格子 | `fw×fh` |

---

## 3. 桌面端怎么摆（等价性的关键）

`ImageView.setScaleType()` **不是** `@RemotableViewMethod`（AOSP `ImageView.java` 有注记，
`setScaleType` 无；SO 7895118 实测远程调用报 "The function setScaleType is not possible"）
→ **scaleType 只能写死在 layout XML 里**。

| 布局模式 | scaleType（XML） | ImageView 尺寸（RemoteViews 远程设） | 结果 |
|---|---|---|---|
| 原始比例（FIT） | `fitCenter` | `contentRect(src,格W,格H,FIT)` = `W'×H'`，`layout_gravity=center` | 与今天**像素级等价** |
| 固定比例（1:1 / 16:9 / 4:3 / 9:16 / 21:9） | `fitCenter` | `contentRect(src,格W,格H,该比例)` = `W'×H'` | 与今天**像素级等价**（那个 853×853 的框由 `setViewLayoutWidth` 显式给） |
| 填充（FILL） | `centerCrop` | `MATCH_PARENT` | 与今天等价（裁切交给桌面） |

用的 API：`RemoteViews.setViewLayoutWidth(id, px, COMPLEX_UNIT_PX)`、
`setViewLayoutHeight(...)`（AOSP `RemoteViews.java:7167/7206`，API 31+）。
**SDK < 31 → 自动回落旧通道**（`fitXY` 无精确尺寸就没法保证等价）。

---

## 4. 逐参数等价表

| 参数 | 旧语义 | 新语义 | 等价？ |
|---|---|---|---|
| `layout=FIT` | 素材内接进格子 | 帧=素材，桌面 fitCenter | ✅ 等价 |
| `layout=固定比例` | 内容 fit 进该比例内接框 | 同上（框由 `setViewLayoutWidth` 显式给） | ✅ 等价 |
| `layout=FILL` | 铺满格子并裁切 | 帧=素材，桌面 centerCrop | ✅ 等价 |
| `scalePercent` | 整块画布绕中心缩放 | 整块帧绕中心缩放 | ✅ 等价（等比） |
| `rotation` / `contentScale` / `offsetX,Y` | 内容在 drawBox 内变换 | 同 | ✅ 等价 |
| `maskOffsetX,Y` | 蒙版窗口平移 box% | 同（box'=素材矩形） | ✅ 等价（数值按素材矩形算） |
| `shapeRotation` | 蒙版绕 (W/2,H/2) 转 | 绕 (fw/2,fh/2) 转 | ✅ 等价（同中心） |
| `shape` 方形 | 铺满 area | 铺满素材矩形 | ✅ 等价（`layout=FIT` 时无条件等价） |
| `shape` 圆角 | 半径 ∝ min(area) | 半径 ∝ min(素材矩形) | `layout=FIT`：✅ 等价（两边同比缩放，`min` 同比例）；固定比例布局：⚠️ 见 §6 |
| `shape` 星/心/水滴/盾… | 铺满 area | 形状比例变 | `layout=FIT`：✅ **等价**（area 比例 = 素材比例，形状等比缩放）；固定比例布局：❌ 回落 |
| `border` | 沿 area 边描边，宽 = min(W,H)×1.8% | 沿素材矩形描边，同比例 | `layout=FIT`：✅ 等价；固定比例布局：❌ 回落 |
| `shadowPercent` | 沿 area 外扩，margin = min(W,H)×3.5% | 同比例 | `layout=FIT`：✅ 等价；固定比例布局：❌ 回落 |
| `alphaPercent` | 内容整体 alpha | 同 | ✅ 等价 |
| 素材比例 | 无关 | 帧比例 | ✅ 任意比例都成立 |

---

## 5. 通道选择（什么时候允许「零重烘」）

```
可走新通道 ⇔ SDK_INT ≥ 31 ∧ (
       layout == FIT                              // 全等价：蒙版/边框/阴影/星形都不受限制
    ∨ ( layout == FILL       ∧ shape == SQUARE )  // 铺满时只有方形蒙版等于「没蒙版」
    ∨ ( 固定比例布局 ∧ shape ∈ {SQUARE, ROUNDED}
        ∧ 圆角半径 ≤ 留白边距 m ∧ !border ∧ shadowPercent == 0 )
)
```

**为什么 `FIT` 无条件等价**：`area = contentRect(...,FIT)` 的宽高比**就是素材比例**，
而新帧的画布比例也是素材比例 → 两边所有「按 min(w,h) 取百分比」的量
（圆角半径、描边宽度、阴影 margin）同比缩放，缩放后数值完全一致。
所以 `FIT` 布局下，**星形蒙版、边框、阴影全都不用回落**。

`留白边距 m` = min( (格H − area.h)/2 , (格W − area.w)/2 )，
`area` 用 `contentRect(素材, 格子, layout)` 现算。

不满足 → **回落到今天的「画布烘焙」通道**，行为与 v1.10.190 完全一致（包括 30 秒重烘）。

---

## 6. 会变的地方（必须承认，不能瞒）

1. **圆角（只限固定比例布局）**：旧半径按 `min(布局框)` 算，新半径按 `min(素材矩形)` 算。
   只有当圆角**可见**（半径 > 留白边距）时才有肉眼差别；不可见时两条通道逐像素相同。
   `layout=FIT` 没有这个问题。
2. **星形/心形/水滴/盾形等 22 种非矩形蒙版（只限固定比例布局）**：帧里必须有 alpha 裁切，
   而桌面端没有任何裁剪能力（AOSP `SetViewOutlinePreferredRadiusAction.apply` 只设
   `setOutlineProvider`，**没有** `setClipToOutline`；`setClipToOutline` 也不是 remotable）
   → 只能留在旧通道，**拖动仍要重烘**。这是平台限制，不是取舍。
3. **边框 / 阴影**：同上 → 旧通道。
   （后续阶段可以做「装饰层」：单独一张静态图，重烘 **1 张 ≈150ms**，拖动结束补上。）
4. 第一次进配置页 / 第一次换素材，仍有一次**素材级**预热（一次，不是每次拖动）。

---

## 7. 真机判据（出包后看这三条）

1. 拖动/连续拖动过程中，日志**不再出现** `W13 预热开始` / `W13 预热完成`；
2. `W10 单帧` 恒定 2~5ms，**不再出现** 163~172ms；
3. 连续拖 10 次尺寸，`命中 ≥98%`、`迟到>8ms ≤1 次`。

---

## 8. 回退

设置页「性能」下新增开关 **「画布无关帧（零重烘）」**，默认开；关掉 = 旧通道。
出问题只需关开关，不用回退版本。

---

## 9. 与线程设置的关系

`预热线程数` 现在由用户可调（4~8，见 v1.10.191）。
新通道下预热只在「换素材/改可见装饰」时发生，线程数的意义变成
「第一次烘素材有多快」——仍然有用，保留。

---

## 10. 实现范围（v1.10.192 实际落地）

| 项 | 状态 |
|---|---|
| `layout = FIT`（原始比例）走帧画布 | ✅ 已做，逐像素等价 |
| 固定比例布局（1:1 / 16:9 …） | ⏳ 未做（需 `setViewLayoutWidth` 显式摆框 + 改 `fitView` 判定），继续走旧通道 |
| 填充布局 | ⏳ 同上 |
| 设置页开关「画布无关帧」 | ✅ 已做（默认开，关掉=旧通道） |
| `W28 帧画布` 诊断 | ✅ 已做（画布口径变了才打一条） |
| `W22 尺寸变更` 画布口径 | ✅ 已对齐（改调 `canvasFor`） |

## 11. v1.10.193 关键修正：画布比例必须等于 area 比例

v1.10.192 只对 FIT 放开，1:1 / 16:9 仍回落（旧通道，拖动照样重烘）。
v1.10.193 把「画布的比例」定成 **`contentRect()` 给 `area` 的比例**：

| 布局 | 帧画布比例 | area 比例 | 是否整块画布 |
|---|---|---|---|
| 原始比例 | 素材比例 | 素材比例（autoContentRect） | 是 |
| 1:1 / 16:9 / 4:3 / 9:16 / 21:9 | 布局比例 | 布局比例（内接框） | 是 |
| 填充 | ——（返回 null） | 格子比例 | 无法解耦 → 回落旧通道 |

比例一致 ⇒ `area` = 整块画布 ⇒ 所有按 `min(w,h)` 取百分比的量同比缩放后**数值不变**
⇒ 蒙版、边框、阴影、圆角半径、星形**全部逐像素等价**，不再需要任何限制条件。
桌面端 `fitCenter` 把「布局比例」的帧等比内接进格子，天然还原旧的 area 居中。
