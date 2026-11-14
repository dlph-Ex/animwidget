# 交接文档：安卓桌面组件「画面铺不满 / 被截断」的完整方案

> 项目：帧工坊 FrameSmith（`io.github.animwidget`），AppWidget 播放逐帧动画/图片。
> 源码 `src-v2/`，构建 `scripts/build-apk-src.sh <versionCode> <versionName>`，门禁 `scripts/verify-apk.sh dist/xxx.apk`。
> 目标机：vivo V2426A / Android 16 / 1260×2800 / density 3.28。桌面包名 `com.bbk.launcher2`。

---

## 0. 一句话结论

**组件"摆多大、摆在哪"根本不由我们的 XML 决定。** vivo 桌面用 `AT_MOST` 量 RemoteViews 根视图，
`ImageView.onMeasure` 在 `AT_MOST` 下取的是 **drawable 的 intrinsic 尺寸**（= 位图像素 × density/160 = ×3.28），
不是父容器尺寸。所以：**位图多大，组件就多大，且固定在格子左上角**。
改画布比例、改 `scaleType`、改圆角/比例模式全部无效——那些只改"位图内容"，不改"位图被摆多大"。

唯一有效的手段：**让位图的 intrinsic ≥ 格子**。桌面取 `min(intrinsic, specSize)`，
intrinsic 够大时它自己会把视图顶到格子大小；多出来的部分由 `centerCrop` 缩回去（不裁内容）。

而"有点宽/有点窄就显示不全"剩下的一半，是**布局语义**问题：`原始比例` 模式按内接画，
格子与素材比例差得越多，留边越宽（实测 1:0.714 素材放进 2.575 的格子 → 画面只占 28% 宽度）。
见第 3 节。

---

## 1. 已证实的桌面机制（含实测数字，别再用猜的）

### 1.1 intrinsic = 位图像素 × 3.28，且贴左上

| 来源 | 格子（白框） | 画面实际占的 | 比值 |
|---|---|---|---|
| 截图 1 | 1096×1096 px | 660×656 px，贴左上 | 660 ÷ 201px位图 = **3.28** |
| 截图 2 | 862×1096 px | 862×625 px，贴左上 | 862 ÷ 262px位图 = **3.28** |
| 截图 3（日志） | 1030×1079 px | 卡面约 70% | 画布 230px × 3.28 = 754 |

3.28 = `densityDpi / 160`。位图经 `BitmapDrawable(res, bitmap)` 加载时被按 density 放大，
`Intrinsic = bitmapPx × density/160`。

### 1.2 `AT_MOST` 下 ImageView 取 intrinsic

`ImageView.onMeasure` → `resolveAdjustedSize(desired, max, spec)`：
UNSPECIFIED 取 desired、**AT_MOST 取 min(desired, specSize)**、EXACTLY 取 specSize。
`desired` 来自 drawable 的 intrinsic。`FrameLayout(match_parent)` 在 `AT_MOST` 下又跟随子视图，
于是整个根视图塌成"位图缩略图"大小，被桌面 `FrameLayout` 按 `top|start` 摆在格子左上角。

### 1.3 `RemoteViews.setViewLayoutWidth/Height`（API 31+）在 vivo 被忽略

v1.10.280 实测：钉了 `(格子dp + 22dp/+8dp) ×1.06`，日志确认调用发出，卡面仍是 70%。
**结论：不要用这条。** 门禁已断言禁止再出现。

---

## 2. 现存代码怎么算（可直接照抄）

### 2.1 画布尺寸必须"垫底"（`WidgetRender.coverFloor`）

```java
// 画布 = max(按素材/布局算出来的, 格子dp × 1.30 + 24px)，面积封顶 32 万像素
static int[] coverFloor(Context context, int[] canvas, int[] box) {
    float d = context.getResources().getDisplayMetrics().density; // 3.28
    int minW = Math.round(box[0] / d * 1.30f) + 24;
    int minH = Math.round(box[1] / d * 1.30f) + 24;
    float k = Math.max(minW / (float) canvas[0], minH / (float) canvas[1]);
    if (k <= 1.02f) return canvas;
    int w = Math.round(canvas[0] * k), h = Math.round(canvas[1] * k);
    long area = (long) w * h, cap = 320000L;
    if (area > cap) { double s = Math.sqrt(cap / (double) area); w = (int)Math.round(w*s); h = (int)Math.round(h*s); }
    return new int[]{w, h};
}

static int[] canvasFor(Context ctx, WidgetConfig cfg, int[] box) {
    return coverFloor(ctx, canvasFor0(ctx, cfg, box, eff = cfg.getLayout()), box);
}
```

要点：
- 1.30 的余量是为了吸收"桌面报的格子 dp 偏小"（实测 vivo 少报 20dp/5dp，且不同组件不同类型不一样）。
- 余量只花面积，不损失画面：intrinsic 比格子大时桌面 `centerCrop` 缩小，不裁内容。
- 面积封顶 32 万（≈430×750）保证预热/编码仍在预算内（实测 20 帧 36ms → 80~120ms）。

### 2.2 画布比例必须 == 格子比例

否则桌面 `centerCrop` 会二次裁切。`renderCanvas` 里**不要**为任何布局做"按素材比例修形"。
`frameCanvas()`（"画布无关帧"通道）对 `LayoutMode.FIT` 必须 `return null`，
否则会退回"画布比例=素材比例"的老路。

### 2.3 桌面 scaleType 的唯一判据（`AspectMath`，纯函数，有门禁）

```java
public static final float SAME_ASPECT_TOLERANCE = 0.05f; // 每边最多裁 2.5%

public static float diff(float a, float b) {                 // 任一侧非正 → -1
    if (!(a > 0) || !(b > 0)) return -1f;
    return Math.abs(a - b) / Math.min(a, b);
}
public static boolean needFitCenter(float canvasRatio, float boxRatio) {
    float d = diff(canvasRatio, boxRatio);
    return d < 0f || d > SAME_ASPECT_TOLERANCE;
}
```
- 相等（≤5%）→ `centerCrop`（位图与格子同形，centerCrop 是**恒等变换**，不多裁也不少铺）；
- 不等（16:9 卡片进方格子）→ `fitCenter`（保卡片圆角，四周壁纸是设计）。

### 2.4 日志字段（用来判定，不用来猜）

`W22 尺寸变更 → … 画布=… 格子=… 素材=… 布局=… 画面=… 桌面视图=… 画布/格子比例差=… 桌面摆=…px(位图×3.28；≥格子才铺满，已富余/还差 Xpx)`

`桌面摆` 是"桌面实际会给我们多大"的计算值 = `画布 × density`。
**只有 `≥格子` 才可能铺满；出现 `还差 Xpx` 就是垫底不够**。

---

## 3. 剩下的真问题：`原始比例` 内接必然留边（实测）

```
11:17:43  格子=380x853(0.445)  素材=1828x2560(0.714)  画面=380x532  → 上下各留 161px
11:17:45  格子=380x400(0.950)  素材=1828x2560(0.714)  画面=286x400  → 左右各留  47px
11:18:01  格子=1030x400(2.575) 素材=1828x2560(0.714)  画面=286x400  → 左右各留 372px
```
素材比例固定，格子一"有点宽/有点窄"，内接出来就是一条壁纸 → 用户说的"显示不全"。

**几何事实：同一图层里，"铺满格子"与"画面一刀不裁"不可兼得。**
曾试过用"同一帧缩到 1/8 再放大"做衬底填满（v1.10.279）——素材是透明底贴纸时，
柔化副本会从透明区透出来，变成沿轮廓的一圈灰雾（用户："这画面边是啥阴"），已撤销。

### 最终验收标准（用户 2026-xx 明确给定，逐字）

> **无论布局蒙版，无论桌面组件宽度，画面必须显示全（填充除外），不能出现丁点画面截断。**

据此定案（v1.10.285 已实施）：

```java
// FrameComposer.contentRect()  —— 原始比例：完整内接，一格像素都不切
if (layout == LayoutMode.FIT) {
    return autoContentRect(sourceWidth, sourceHeight, width, height);
}

// FrameComposer.drawFrame()   —— 除「填充」外，内容一律内接（fit），不再默认 cover
switch (layout) {
    case FIT:   z = true;  break;   // 完整
    case FILL:  z = fitContent; break;   // 填充=cover（可被"内容适配"救成内接）
    default:    z = true;  break;   // 1:1/4:3/16:9/9:16… 也内接（以前是 cover，会裁）
}
```

三条硬约束：

1. **画布比例必须 == 格子比例**（否则桌面 `centerCrop` 会二次裁切，你没法控制）；
2. **画布 intrinsic 必须 ≥ 格子**（`coverFloor`），否则桌面按 intrinsic 摆 → 贴左上、留白；
3. **除「填充」外一律内接**：多出来的方向是**透明的**（露壁纸），这是"必须显示全"的唯一几何解。

> 关于「蒙版无作用」：`蒙版 = 形状路径(path)`。v1.10.284 把原始比例改成 cover（卡片=整块画布）时，
> 圆角/描边落在画布边缘之外，**路径裁剪看不见，看起来就是蒙版失效**；v1.10.285 回到内接后，
> 路径重新落在画面边界上，圆角/描边/阴影恢复可见。若素材本身的四角是透明的（贴纸 GIF），
> 圆角仍然"看不见效果"——那不是 bug，是没有可被裁的像素。

## 4. 验收标准（用户在 vivo 上验）

1. 拖成很宽、很高、近方各一次 → **画面四面贴住格子**，不露壁纸条。
2. 换一个有透明底的贴纸 GIF → **不允许出现沿轮廓的灰雾**（衬底方案已禁）。
3. 组件刚加到桌面 / 刚保存导出 → **第一拍就是真画面**，不许停在 `widget_preview`
   （应用图标，深色方块+绿三角）。这需要"占位图状态"：
   `WidgetUriPush.notePlaceholder(id)`（推占位时）/ `send()` 里清位 /
   `render()` 里 `if (!showingPlaceholder(id)) { noteSkip(); return 0; }`。
   已实现，门禁断言禁止断开。
4. 日志里 `桌面摆` 必须 `≥格子`，且 `画布/格子比例差 ≤ 0.5%`。

---

## 5. 别再踩的坑（都实测过，重跑一遍要花 7 个版本）

| 试过的做法 | 结果 |
|---|---|
| 改 `ImageView` 的 `scaleType`（fitCenter/centerCrop） | 无效。位图够大时才有意义，位图小时尺寸由 intrinsic 决定 |
| 改画布比例（画布=素材/卡片比例） | 只是改变"那条壁纸"的宽度，看起来像"截断" |
| `RemoteViews.setViewLayoutWidth/Height` | vivo 忽略；且钉错比不钉更糟 |
| 把 `minWidth/minHeight` 当成格子 | vivo 报的值**已扣掉它自己的 padding**（实测少 20dp/5dp） |
| 用"同帧降采样放大"做衬底填满 | 透明底素材 → 轮廓灰雾 |
| 靠桌面 `fitCenter` 留边"反正居中" | 位图小于格子时贴左上，不是居中 |
| 只改判据阈值（10%/20%） | 判据不是变量；尺寸/比例模式才是 |

**方法论**：任何"桌面侧观感"问题，先量出两个数 —— **格子像素** 与 **画面实际像素**，
两者相除若等于 `density/160`，答案就是 `AT_MOST + intrinsic`，不用再猜。

---

## 6. 门禁（`scripts/verify-apk.sh`，共 12 项）

- `[12] layout-math-check.sh`：现编真 `AspectMath.java` 跑 22 条断言，
  并**扫源码**：`WidgetRender` 不许手写比例差、不许 `fitNow || lose`、
  `coverFloor` 必须在 `canvasFor` 链路里、`setViewLayoutWidth` 不许回来、
  衬底不许回来、占位图状态不许断开。
- 其余 11 项：字节码 312 类 bad=0、R 资源 738 项缺失 0、主题 A/B/C、
  形状/阴影、探头 33 条、折叠几何 35 条、对比度 13 条、diag 代号体检。

构建：`bash scripts/build-apk-src.sh 398 1.10.283`（aarch64 PRoot + JDK17 + toolchain/ 内的 aapt2/r8/apksig，无需 Gradle/SDK）。
签名：`build-manual/debug.keystore`，证书 SHA-256 `5e9b785c932e49ad621ebefb31fb1466592fa962882955a69ceb895d04eef4d3`（自 1.1.1 未变，可覆盖安装）。
当前包：**v1.10.289 / versionCode 404** / 6,561,279 字节 / SHA-256 `59e2ac68445320951322490d40dd99818077993460e86a9c58887e696f0bb6c3`。

---

## 7. 建议接手顺序

1. 先跑 `scripts/verify-apk.sh`，确认 12 项全绿（基线）。
2. 第 3 节的定案**已在 v1.10.285 实施**（`contentRect` 的 FIT 分支 + `drawFrame` 的 fit 开关两处）。
3. 装到 vivo V2426A 上拖成宽/高/方三种格子，对日志 `桌面摆`、`画面`、`格子` 三个字段。
4. 若"铺满"仍不够（画面 < 格子），只调 `coverFloor` 里的 `1.30f` 与 `+24`，
   日志的 `还差 Xpx` 就是确切缺口，按比例加即可（不要动别处）。

---

## 8. 未解决：尺寸变化后"更新太慢"（有实测数字）

```
11:45:43 W13 预热完成 组件=231 画布=434x1448 完成=1 用时=133ms 速率=7帧/秒
11:45:40 W22 尺寸变更 画布=434x1448 格子=597x1988 …
11:38:08 W13 预热延后 画布刚变（等静置 800ms 再烘，避免拖动连开多批）
11:45:46 W23 推帧合计 渲染=0 次/3002ms tick=38 调度=37 均隔=1002ms（预算=33ms）
```
三段延迟叠加，用户体感就是"推得太慢 / 变长条不重画"：

| 环节 | 现状 | 问题 |
|---|---|---|
| 尺寸去抖 | `SIZE_SETTLE_MS = 500ms`（WidgetProvider）+ 预热 `800ms`（WidgetPrewarm） | 拖动一次要等 1.3s |
| 预热合成 | 单帧 133ms（画布 434×1448，静态图高清档） | 7 帧/秒，推不动 |
| tick 基线 | `均隔=1001ms`（`组件数=0` 时的空转节拍） | 恢复后第一拍要等 1s |

### 建议（给接手人）

1. **拖动期即时预览**：尺寸一变，不要等预热——用**最近邻旧画布**（任意比例）一次 `Bitmap.createScaledBitmap` 到新画布尺寸直接推（约 2~5ms），
   比例不完全正确但"立刻有反应"；预热完成后再用真画布替换。（`WidgetUriPush.nearestLoose` 已存在，只需把 `sizeStale` 分支里改成"缩放旧画布后推"，而不是现合成。）
2. **预热降档**：画布面积上限从 32 万降到 12 万（`coverFloor` 的 `cap`），单帧合成 133ms → 约 50ms；
   静态图 1:1 高清档（`HIRES_AREA`）在拖动期间临时禁用。
3. **去抖改短**：`SIZE_SETTLE_MS` 500 → 150ms，预热去抖 800 → 250ms。
4. **别在 tick 里现合成**：`render()` 的 "现合成" 只允许在**空闲**（无 tick 压力）时做，否则会压帧率。

验收：拖动过程中画面每 100ms 内至少有 1 次更新；松手后 300ms 内画面正确。

---

## 9. 未解决（主项）：301 帧动图一盘预热 8.8 秒 / 46MB，拖动一次烧 ~15 秒

实测（12:07，组件 235，画布 900×745，301 帧）：

```
W13 预热完成 组件=235 画布=900x745 完成=301 用时=8804ms 速率=34帧/秒
            缓存=本组27421KB+旧18846KB=合计46267KB
W27 合成分解 帧=299 合计均=4ms 内容均=4ms 编码均=154ms(300帧)   ← 编码只占 154ms
W13 预热完成 组件=235 画布=597x626…（本批作废）4739ms
W13 预热完成 组件=235 画布=813x626…（本批作废） 355ms
W13 预热完成 组件=235 画布=900x546…（本批作废）1792ms
W10 组件 235 实测 29.9fps / 画布=900x745 / 命中=100%
```

结论：
- **更正（v1.10.288）**：`编码均=154ms(300帧)` 是**每帧平均**，不是总量。301 × 154ms ≈ 46 秒 CPU，
  摊在 `线程=6` 上 ≈ 7.7 秒墙钟，与实测 8.8s 对得上 —— **编码占约 88%**，是最大头（之前写成"不在编码"是错的）。
  合成本身只有 4ms/帧。
- **每换一档画布就整盘重写 301 张（46MB）**；13 秒内烧了 4 批、其中 3 批作废。
- v1.10.287 的补丁只对 `frameCount<=1` 生效，对 301 帧这块**完全没动**。

### v1.10.288 已做（3 处，只动 `WidgetPrewarm`）

1. `SETTLE_MS` 800 → 1500ms（只对多帧生效）：实测相邻两档尺寸只隔 1~2 秒，800ms 挡不住，
   一次拖动开 4 批、烧 ~15 秒、3 批作废。
2. 去掉 `bake()` 里那 250ms `sleep`（`ensure` 已经静置过，纯白等）。
3. **烘焙顺序改成环形、起点 = 播放头 + 10 帧**（日志新增 `起点=`）。
   病根证据：`12:07:17 W10 组件 235 命中=0%(0/86) 跳过=77次` —— 旧画布与新画布都只有"开头一段"，
   `nearestCached` 只往回找 8 帧、`nearestLoose` 只找 ±8，全落空 → 每拍跳过 → **冻屏**，
   直到播放头绕回开头。环形烘之后前沿应始终领先播放头。

### 方案（按优先级，A 是主项）

**A. ~~按需窗口烘焙~~ —— 结论：不做滚动删除（v1.10.289 定案）**

读完 `PlaybackService.java` 后否掉了"边播边删旧窗口"这半：

- 播放是**严格正向循环**，帧号按墙钟算 `due = (elapsed × fps) % count`；301 帧 @30fps 十秒一圈。
- 现在烘一次之后播放几乎零成本（单帧 2~3ms）。**一旦删掉播过的帧，每转一圈就要整盘重烘** ——
  每帧线程耗时约 175ms × 301 ≈ 53 线程·秒，约 5 个核常驻满载，还会烫和费电。
- 烘速上限 34fps，只比播放 30fps 高一点，没有余量，删帧必然抖。
- A 想要的"首屏快"，**v1.10.288 的"从播放头环形烘"已经覆盖**，而且不删帧。
- `磁盘 < 10MB` 这条验收**靠调度做不到**：27MB ÷ 301 ≈ 91KB/帧，要进 10MB 得每帧 ≤34KB，
  那只能降画质或缩画布面积 —— 属于画质取舍，要用户拍板，不要偷偷改。

**B. 拖动期不烘（立刻少烧 15 秒 CPU，风险最低）**
- `WidgetPrewarm.ensure()` 里：若"最近 1.5s 内出现过 `W22 尺寸变更`"，**只推旧画布、不烘**；
- `SIZE_SETTLE_MS` 多帧恢复 500ms、`SETTLE_MS` 多帧 800 → 1500ms；
- 收益：13 秒内 4 批 → 1 批。

**C. 画布比例吸附（可选）**
- 画布比例吸附到 1/16 档（0.70/0.75/0.80…），拖动中间档共用一块画布；
- 收益：一次拖动 4 档 4 批 → 1 档 1 批。

### 给接手人的建议顺序
1. 先 B（1 小时内，只动 `WidgetPrewarm.ensure` + 两个常量）；
2. 再 A（半天，动 `bake()` 的循环范围 + 窗口滚动 + `render()` 的补窗调用）；
3. C 只在"A 做完还嫌换画布次数多"时再上。

### 验收数字（下一版必须给出）
- 301 帧动图首次上屏时间：**< 2s**（现在 8.8s）；
- `磁盘帧` 峰值：**< 10MB**（现在 29MB）；
- 一次拖动（4 档）烧掉的预热批次：**≤1**（现在 4）；
- 播放帧率不掉：`W10` ≥ 目标 fps，`迟到>8ms` 仍为 0~3 次。

---

## 10. v1.10.288 / v1.10.289 尚未真机验证（重要）

- 288（静置 1500ms、环形烘 `起点=`）**还没有对应日志**：交接包里的摘录是 287/12:07 那份，
  所以第 9 节"首次上屏 <2s"目前没有任何实测支撑。
- 289 补了量尺：每次尺寸变化后，第一次在新画布上命中缓存时打一行

```
W13 首帧命中 组件=235 距画布变化=…ms 距开烘=…ms
```

  `距画布变化` 就是**首次上屏时间**（第 9 节验收项）。只写日志，不改行为；
  钩子挂在 `WidgetRender.render()` 的 `hit != null` 分支里（缓存命中那一刻），单帧静态图也会打。

### 下一轮要拿的三样数据

1. `W13 预热开始 … 起点=`（确认环形起点 = 播放头 +10）
2. `W13 首帧命中 … 距画布变化=`（首次上屏时间，目标 <2s）
3. `W10 组件 … 命中=… 跳过=…`（跳过应从 77 降到个位数）
