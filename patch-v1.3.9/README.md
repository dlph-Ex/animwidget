# v1.3.7：液态玻璃分段控件 + 编辑页滑块实时预览

## 1. 素材类型选择（MediaSelectorView 重写）

参考你给的视频（Kyant 那套 SystemUI 的液态玻璃分段控件）复刻观感，**纯 Canvas 自绘、零第三方依赖**
（不能引 Compose / 模糊库，见下面「没做到的部分」）。

| 层 | 做法 |
|---|---|
| 轨道 | 半透明胶囊（dark: neutral@24%，light: neutral@35%）+ 发丝描边 |
| 选中块 | 玻璃胶囊：半透明白底 + 主色微染 + 顶部线性高光 + 1dp 描边 + 落地阴影 |
| 动画 | `ValueAnimator` 320ms + `DecelerateInterpolator(1.6)`；位移过程中横向拉伸 `7dp*sin(πt)` 再回弹（液态） |
| 文字 | 每个标签按「到选中位置的距离」在 旧色 ↔ 主色 之间插值，选中态自动加粗 |
| 深浅色 | 用 `applyColors()` 传进来的 textColor 亮度判断（>128 视为深色主题），六套预设 / 反转 / 对比度都直接生效 |

API 与旧版完全一致（`applyColors` / `setLabels` / `setSelected(MediaKind, boolean)` / `setSelected$default` /
`setOnKindClick` / `(Context, AttributeSet)` 构造），所以 `ConfigActivity` 不用改。

**没做到的部分**：视频里那块是真的「背景模糊 + 折射」（Compose Modifier.blur / RenderEffect 采样下层内容）。
普通 View 无法采样兄弟视图，所以这里用多层半透明 + 高光做视觉近似；真模糊需要 `RenderEffect` 加截图缓存，
在滚动/拖动时反而更卡，先不做。

## 2. 三个滑块实时化（不透明度 / 阴影 / 等比例缩放）

两处根因，都在「渲染链路」上：

1. **`PreviewRenderer.render()` 每次都做 WebP 编解码往返**——哪怕 quality=100 也会
   `compress → decodeByteArray`，一次 20~40ms。改为：等比缩放后直接 `FrameComposer.compose`，不压缩。
2. **防抖延迟**：`schedulePreview()` 用 `postDelayed(…, 120L)`、`refreshPreviewNow()` 用 `postDelayed(…, 90L)`，
   拖滑块时只有「手停下来」才出图。用 ASM 把这两处常量改成 `0L`（只改这两个方法里的 90/120，别处的 900ms 首次帧延迟不动）。

现在拖动时每一帧都重新合成（预览分辨率 ≤1080，单帧约 3~8ms），观感上是跟手的。

## 3. 构建 / 校验

```bash
ANDROID_SDK=/tmp/sdk KOTLIN_LIBS=/tmp/kl SHIZUKU_JAR=/tmp/shizuku-all.jar \
  ./patch-v1.3.7/build.sh 70 1.3.7
```

- `CheckClassAdapter`（SimpleVerifier + android.jar/stdlib/base jar）验证
  `MediaSelectorView`、`MediaSelectorView$1`、`PreviewRenderer`、`ConfigActivity`、`SettingsActivity` → 全部 OK；
- `javap` 确认 `schedulePreview()` / `refreshPreviewNow()` 的 `postDelayed` 参数已变成 `0l`；
- 资源 ID 仍与 v1.3.1 完全一致（355 条）；
- 签名 `5e9b78…f4d3`。

## 4. 兼容性

- 布局结构未动：上半屏预览（v1.3.6）、EdgeToEdge、主题原地换肤（v1.3.3/1.3.4）、阴影等比留白（v1.3.2）、
  尺寸退化兜底（v1.3.5）全部保留；
- 分段控件颜色全部来自主题，深色/浅色/六套预设/反转/对比度都会跟着变；
- 素材类型切换仍然走原来的 `onKindClick` → `pickedKind` → 预览刷新链路。
