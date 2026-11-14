# 蒙版位置手势：实施方案（✅ 已在 v1.6.0 落地）

> 实现结果与下面方案的差异，只有第 5 条：`applyAutoParams()`（一键适配素材参数）其实只重置
> 帧率 / 清晰度 / 质量，不碰任何偏移，所以「重选素材」时归零（与内容偏移一致），没有另加重置入口。

## 为什么现在能做

`src-v2/` 已 0 error，`scripts/build-apk-src.sh` 可从源码出包。
`WidgetConfig` 的所有调用方（`ConfigActivity` / `WidgetStore` / `WidgetRender` /
`OverlayService` / `PlaybackService`）都在源码树里，改字段不会再有 `copy$default` 描述符问题。

## 改动清单（按顺序）

### 1. `WidgetConfig`（27 字段 Data class，435 行）
- 新增 `maskOffsetXPercent: Float` / `maskOffsetYPercent: Float`（默认 0f）
- 同步：主构造器、`copy`、`copy$default`（位掩码 26/27）、`componentN`、`equals`、`hashCode`、`toString`
- JSON：`put("maskX", …)` / `put("maskY", …)`，读取 `optDouble("maskX", 0.0)`
- ⚠️ 所有 `copy$default(...)` 调用点要补两个参数（编译器会全部报出来，逐个补）

### 2. `FrameComposer.drawFrame`
```java
// 蒙版路径生成后
float mx = box.width() * (maskOffsetXPercent / 100f);
float my = box.height() * (maskOffsetYPercent / 100f);
path.offset(box.left + mx, box.top + my);   // 原来是 path.offset(box.left, box.top)
```
画布 `clipRect(area)` 保持不动 → 蒙版被限制在布局内容区内，偏移过大时自动裁切。

### 3. 渲染链路透传
- `WidgetRender.render(...)`：把 `config.getMaskOffsetXPercent()` 传给 `compose(...)`（新增重载）
- `OverlayService` / `MediaOverlayView`：同上
- `PreviewRenderer.render(...)`：新增两个参数

### 4. 手势 UI
- 新建 `MaskGestureDialog.java`：复用 `ContentGestureView`，标题「手势调整蒙版位置」，
  拖动写 `maskOffsetX/YPercent`，范围 ±50%
- `ConfigActivity`：在外观页「形状蒙版」卡片顶部加一行按钮（在现有「手势调整内容」按钮**上方**），
  点击打开 `MaskGestureDialog`；确认后 `refreshPreviewNow()`

### 5. 兼容
- 旧配置 JSON 无 `maskX/maskY` → 默认 0，行为不变
- 「一键适配素材参数」重置时把蒙版偏移一起归零

## 预估
- 后端（1~3）：一轮
- UI（4）：一轮
- 联调 + 出包：一轮
