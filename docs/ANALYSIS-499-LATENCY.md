# 材料包：编辑页「首帧不显示 / 预览抖动 / 导出时拖滑块迟钝」

> 用途：交给外部 AI 做独立分析。**本文件只是材料与量化账，不代表今天的代码已经改动**（用户明确要求先不修）。
> 版本：v1.10.498（versionCode 612）真机日志；素材 `685.gif`。
> 原始日志：`Component_2612_37_06.txt`（1035 行，2025-xx-xx 12:34:18–12:37:04）。

---

## 0. 三句话结论（后面每条都有代码与日志支撑）

1. **「第一帧不显示」不是渲染坏了，是「解析期 19 秒根本没有画面」**：`loadPreviewFrames` 只把
   `pendingFirstFrame` 清掉，整段解析不画任何帧；`L05 首帧上屏` 出现在**解析 100% 的同一秒**。
   另外播放启动那一次 tick 的 `idx=0` 被 `previewRenderBusy` 丢掉了（`P12 … idx=0`，
   实际画面由 `applyPreviewFrames → renderPreviewFrameAt(0)` 那条直调补上），
   所以「首帧」即使出现，也是解析结束之后、且首圈掉帧严重（idx=2 一路丢到 131）。
2. **「预览抖动」是算力不够，不是时钟错**：预览合成一帧要 **~25–60ms**，而 50fps 只有 **20ms** 预算，
   实测 22.9→36.5fps，每 3 秒丢 45–87 帧。丢帧判断在 `renderPreviewFrameAt`，唯一线程
   `previewExecutor`（单线程）里还塞了「预取 3 帧」的同步解码。
3. **「导出时拖滑块迟钝」与素材字节体积无关，与「触摸那一刻同步截了一张全屏图」有关**：
   `dispatchTouchEvent → showOutputBusyDialog → new OutputBusyDialog → capture()`
   在 **输入分发路径上** 做了 `Bitmap.createBitmap(1080×2400)` + 把整棵 View 树软件重绘一遍
   （含预览大图）+ 4 遍盒式模糊 + Dialog inflate + 300ms 入场动画。这一串全部在 UI 线程同步执行。

**用户自己的判断「可能跟素材体积有关」需要纠正**：这次日志里的素材是 685×887、711 帧、50fps，
真正决定成本的是 **像素数 × 帧数 × 每帧要跑几遍编码**，不是文件的 MB 数
（210MB 那个 945×1224 素材如果帧数少，导出反而更快）。

---

## 1. 素材与设备

| 项 | 值 | 来源 |
|---|---|---|
| 格式 | GIF | `M04 嗅探结果: gif` / 界面卡片 `GIF` |
| 像素 | **685×887**（0.61 MP） | `K02 头部尺寸 685x887`、`K01 动图探测` |
| 帧数 | **711**（源）→ **593**（采样 50fps） | `K05 头 711帧/11850ms 采样 50.0fps/593帧` |
| 时长 | **11850 ms** | `F12` / `K01` |
| 源帧率 | **50.0 fps**（每帧 16ms，前 8 帧延时 `[20,10,20,20,10,20,20,10]`） | `F12`、`G12` |
| 派生帧率 | 50.0fps（沿用源帧率） | `L29 生效帧率=50.0fps` |
| 导出边长 | **887**（= 素材长边，非 720） | `T01 边长=887`、`T27` |
| 导出质量 | 滑块 100，但因边长 >720 退回 **有损 q95** | `T27 质量=100（…超过则退回 q95）` |
| 落盘体积 | **190 MB / 593 帧 ≈ 328 KB/帧** | `W33 磁盘帧=190MB` |
| 设备 | 8 核，PSS 209–229MB，Java 堆上限 **256MB**，整机 15.35GB | `W33 java=11/256MB PSS=209MB`、`W27 核: 0..7` |
| 桌面格子 | 1095×1095 → 拖到 662×643 等 | `F11`/`W22` |

> 注意：**Java 堆上限只有 256MB**（未声明 `largeHeap`），而 593 帧×328KB = 190MB 是**磁盘**不是堆，别混。
> 但对「预合成缓存」来说这是硬天花板：`FrameCache` 预算 = `maxMemory()/4` ≈ **64MB**，
> 一张预览合成帧 = 887×685×4B ≈ **2.43MB** → 缓存只能放 **约 26 帧**，而循环有 **593 帧**（0.5 秒的内容）。

---

## 2. 时间线（从日志重建）

```
12:34:24  M03 选图(GIF) → M04 嗅探 gif → K02 头部尺寸 685x887
12:34:25  G12/M05 元数据：711帧 / 11850ms / 50fps
          L02 预览帧: 开始解析 kind=GIF fps=50.0 边长=887
          L22 任务UI: 解析开始 … 预览卡片=0
12:34:25 ────────────────────────────────────────────────┐
          L32 解析进度 1/593 … 538/593 (19 秒)            │ ← 这 19 秒预览区**空**
12:34:44 ────────────────────────────────────────────────┘
12:34:44  L04 预览帧: 应用 593 张 fps=50.0
          L07 预览播放启动: 593 张, fps=50, 间隔=19ms
          L05 预览帧: 首帧上屏 685x887        ← 直到这里才有画面
          P12 预览合成跟不上，丢一帧 idx=0    ← ★「第一帧」当场被丢
          P12 … idx=2 / 4 / 9 / 11 / 12 / 13 / 14 / 16 / 21 / 26 …
12:34:47  P20 预览实测 22.9fps（目标 50）/69 帧/3009ms，合成跟不上丢 87 帧
12:34:50  P20 预览实测 36.5fps /110 帧/3011ms，丢 45 帧
12:34:53  P20 预览实测 35.3fps /107 帧/3027ms，丢 46 帧
12:34:56  P20 预览实测 35.5fps /107 帧/3013ms，丢 46 帧
12:34:56  T01 开始导出 gen=1 fps=50 边长=887 质量=100 布局=FIT
          T27 帧数=593   P23 无损自检 差异像素=97%（有损）
          L09 预览播放停止, 来自=startExport
12:34:57 ────────────────────────────────────────────────┐
          L32 导出进度 1/593 … 593/593 (69 秒)            │ ← 用户在这 69 秒里拖滑块
12:36:05 ────────────────────────────────────────────────┘
12:36:05  P02 动图导出 593 帧 / K05 核对一致 / T22 直接生效
12:36:05  W13 预热开始 线程=8 → 12:36:20 W13 预热完成 用时=13822ms 速率=42帧/秒
          W27 合成分解 编码均=144ms(593帧) 编码CPU=75%  ← 8 线程并行时单帧仍 144ms
12:36:20  W48 解码对拍 WebP=21.1ms JPEG=6.4ms 合成=0.2ms
12:36:44  W22 尺寸变更（桌面拖格子）… 50fps 推帧全程 W10/W23 正常
```

### 2.1 单位成本汇总（同一台机器、同一素材）

| 动作 | 单帧耗时 | 依据 |
|---|---|---|
| WebP 解码 685×887 | **21.1 ms** | `W48 WebP=21.1ms` |
| JPEG 解码同尺寸 | 6.4 ms | `W48 JPEG=6.4ms` |
| 预览合成（含解码+合成） | **≈25 ms**（21.1 + 4） | `W48` + `W27 内容均=4ms` |
| 预览**预取** 3 帧（串行） | **≈63 ms** | `prefetchPreview` 循环 3 次 decodeFrame(21ms) |
| 导出单帧（解码+缩放+编码 q95+写盘） | **116 ms** | 69s ÷ 593 |
| 烘焙单帧（8 线程并行） | 23 ms（墙钟） | 13.8s ÷ 593；单帧编码 144ms / 8 线程 |
| 桌面推帧（共享位图，解码在池里） | 2.4–3.2 ms 推 + 9–10 ms 解码 | `W10` / `W23` |

**关键反差：同样 593 帧，导出 69 秒，烘焙只 13.8 秒（5 倍差）。**
导出是一个线程串行干「流式解码 GIF → 缩放 → 编码 WebP → 写盘」，
而烘焙是 8 线程并行编码。

---

## 3. 代码地图（调用链 + 关键摘录）

> 文件均在 `src-v2/io/github/animwidget/`。行号对应 v1.10.498 源码。

### 3.1 预览播放：每一次 tick 都走「缓存命中 or 单飞重活」

`ConfigActivity.startPreviewFramesPlayback()` 7028–7149：

```java
// 7046-7051
final int fpsX10 = this.paramsDragging ? coerceIn(effectiveFpsX10(),10,500)
                                       : coerceIn(result.getFpsX10(),10,500);
long iv = Math.max(10000L / fpsX10, 4L);          // 50fps → 200
// 7063-7068：再按「素材真实时长 / 帧数」覆盖，得到 19ms
long srcMs = this.sourceDurationMs > 0 ? this.sourceDurationMs : result.getDurationMs();
iv = Math.max(srcMs / n, 1L);
// 7112-7116：墙钟归一算下标
long step  = spanMs > 0 ? (elapsed * total) / spanMs : elapsed / interval;
int  index = (int) (step % total);
ConfigActivity.this.renderPreviewFrameAt(index);
// 7144：下一拍
ui.postDelayed(this, Math.max(nextAt - SystemClock.elapsedRealtime(), 1L));
```

`ConfigActivity.renderFramePath(String path, int index)` 7222–7302（**核心热点**）：

```java
final int[] canvas = previewCanvas();                 // 7223  ← 见 3.2：887×685
final String composedKey = width+"x"+height+"|"+paramsKey()+"|"+path;  // 7232 ← 每帧拼串
Bitmap hit = composedCache().get(composedKey);        // 7234
if (hit != null && !hit.isRecycled()) { publishPreviewFrame(gen, hit, index); return; } // 7237 0 成本

if (!previewRenderBusy.compareAndSet(false, true)) {  // 7242 单飞：上一帧没画完就**丢这一帧**
    if (playing) { fpsWindowDropped++; Diag.warnThrottled("P12", "预览合成跟不上，丢一帧 idx="+index); }
    else { previewDirty = true; }
    return;
}
previewExecutor.execute(() -> {                       // 7255 PreviewExecutor = **newSingleThreadExecutor**
    Bitmap source = previewCache().get(cacheKey);     // 7260
    if (source == null) {
        source = PreviewFrames.decodeFrame(path, width, height);  // 7266 WebP 解码 ≈21ms
        previewCache().put(cacheKey, source);
    }
    out = composePreview(source, width, height);      // 7277 ≈4ms
    ...
    finally { previewRenderBusy.set(false); }         // 7282
    composedCache().put(composedKey, composed);       // 7287
    ui.post(() -> publishPreviewFrame(generation, composed, index)); // 7292
    prefetchPreview(index);                           // 7300 ← ★ 同一线程里再解 3 帧（≈63ms）
});
```

`prefetchPreview` 7450–7481：`for (k=1..3) decodeFrame(next, canvas[0], canvas[1])` —— **同步跑在
`previewExecutor` 这唯一一条线程上**，下一帧的重活排在它后面。

`publishPreviewFrame` 7424–7447：

```java
if (this.busy && index != this.previewFrozenIndex) {  // 7430 导出期间：非冻结帧一律丢弃
    Diag.warnThrottled("L31", "导出中：丢弃非冻结帧上屏 idx="+index);
    return;
}
if (generation == previewGeneration.get() || playing) {
    imgPreview.setImageBitmap(out);                    // 7438
    fpsWindowFrames++;
    if (!previewFirstLogged) { previewFirstLogged = true; Diag.warn("L05", "预览帧: 首帧上屏 …"); }
}
```

### 3.2 预览合成尺寸 = 素材尺寸（不是预览控件尺寸）

`previewCanvas()` 7172–7189：

```java
int w = previewWidth();  int h = previewHeight();     // ≈ 屏幕宽 × 卡片高
int src = sourceEdge();                               // 887
if (src > 0) {
    float limit = src;                                // v1.10.255：不再放大素材
    if (max(w,h) > limit) { k = limit/max(w,h); w*=k; h*=k; }
}
return {w,h};                                         // → 887×685 = 607k px
```

结论：预览每帧按 **887×685（607k px）** 合成，而屏幕上实际显示的预览框远小于此
（`F10 桌面=426x377`，`previewWidth()` 取 ImageView 宽）。**这里每帧多算了至少 2–3 倍像素。**

### 3.3 首帧：解析期没有「先上一帧」的代码路径

`loadPreviewFrames` 6513–6603：

```java
this.pendingFirstFrame = false;      // 6535  ← 只清标记，解析期间不画任何帧
this.parseTaskActive = true;         // 6539
...
new Thread(... loadPreviewFrames$lambda$72 ...).start();   // 6595
```

`pendingFirstFrame = true` 只出现在**打开已保存配置**那条路径（1259，`frameCount>1` 时 900ms 后 `startPlayback(true)`），
与「刚导入新素材」无关。所以「导入后第一帧」在 19 秒解析窗口里必然空白——
除非另写一条「解第 0 帧先上屏」的通道（`loadPreview`/`P01` 那条只用于缩略图，本次日志里没被调用）。

而解析完成后 `P12 … idx=0` 说明：**连播放的第 0 帧都被丢了**（`previewRenderBusy`
在上一轮收尾时还挂着，或 `prefetch` 占着线程）。

### 3.4 导出：单线程串行，UI 线程反而空闲

`ConfigActivity.runExport` → `MediaImporter.m9import` → `importAnimated` 206–…：

```java
// 206-225：GIF 走流式（v1.10.498），其余整份读取
String sniff = MediaSniff.sniff(context, uri);
final boolean streamGif = "gif".equals(sniff);
...
// 303-306：解码在**导出线程**里串行做
AnimStream.runGifStream(SourceFps.openStream(context, uri), anim.delays, maxEdge, timeline, sink, failure);
```

Sink 里每帧：`writeFrame(frame, …, q, losslessBig)`（710/720）= `Bitmap.compress(WEBP, 95)` + 写盘，
主线程每帧收一次 `onProgress` → `postToUi`。
**导出线程优先级 `THREAD_PRIORITY_BACKGROUND`（`runExport`），单线程；没有并行编码。**

### 3.5 触摸锁参 → 弹窗：在输入分发里做全屏截图

`ConfigActivity.dispatchTouchEvent` 6035–6122：

```java
if (action == ACTION_DOWN) {
    View hit = lockedViewAt(ev.getRawX(), ev.getRawY());   // 6044 遍历 20 个锁定控件 + getGlobalVisibleRect
    if (hit == null) return super.dispatchTouchEvent(ev);
    if (hit instanceof SeekBar) {
        if (thumbHit((SeekBar) hit, ...)) {                // 6053
            lockKind = LOCK_SLIDE; noteOutputLock();       // W50
            showOutputBusyDialog();                        // 6057 ★ 同步建弹窗
        } else { lockKind = LOCK_TRACK; }                  // 6060 摸轨道：吞 DOWN，自己 scrollBy
        return true;                                       // 6062
    }
    ...
}
```

`showOutputBusyDialog()` 6133–6155 → `new OutputBusyDialog(activity, alsoClose).show()`。

`OutputBusyDialog.show()` 100–272（节选）：

```java
this.backdrop = new BlurBackdropView(this.activity, capture(), …);  // 120 ← capture() 在这里
...
Window window = dialog.getWindow(); window.addFlags(FLAG_LAYOUT_IN_SCREEN); ...
dialog.show();                                                     // 272
```

`OutputBusyDialog.capture()` 462–480：

```java
Bitmap shot = Bitmap.createBitmap(src.getWidth(), src.getHeight(), ARGB_8888); // 472 全屏 ≈10.4MB
Canvas canvas = new Canvas(shot);
src.draw(canvas);                                                               // 475 整棵 View 树软件重绘
```

`BlurBackdropView.build()` 30–77：缩到 1/6 → `AuroraView.blurH/blurV` 各两遍（4 遍盒式模糊）。

> 这一整串 = 分配 10MB + 全屏软件重绘（含预览大图）+ 4 遍模糊 + inflate + 300ms 动画，
> **全部发生在 ACTION_DOWN 的处理里**。导出正在跑时 CPU 被编码占住，这段延迟被放大到肉眼可感。

---

## 4. 三条现象的量化归因

### A. 首帧不显示

**日志顺序（该文件是新→旧排列，按时间正读是自下而上）：**

```
12:34:44 K05 核对 → G10 出帧 593 张 → L32 100%
12:34:44 L04 预览帧: 应用 593 张 fps=50.0
12:34:44 L07 预览播放启动: 593 张, fps=50, 间隔=19ms
12:34:44 L35 预览时长自检 ✓
12:34:44 P12 预览合成跟不上，丢一帧 idx=0        ← 播放第 0 拍被丢
12:34:44 L05 预览帧: 首帧上屏 685x887            ← 画面到这里才出现
12:34:44 P12 idx=2 → 12:34:47 idx=131 …（一路丢）
```

| 证据 | 结论 |
|---|---|
| `L22 解析开始`(12:34:25) 到 `L05 首帧上屏`(12:34:44) 隔 **19 秒** | 解析窗口内预览区没有任何帧；代码里确实没有「解析期先画第 0 帧」的通道 |
| `applyPreviewFrames` 7021 先 `renderPreviewFrameAt(0)`，7023 又 `startPreviewFramesPlayback()` | 两条请求撞在同一帧：前者占住 `previewRenderBusy`，后者的 tick 打 `P12 idx=0` |
| `P12 idx=2…131` 持续 3 秒 | 首圈几乎每两帧丢一帧（`P20` 22.9fps）——用户感受就是「第一帧卡住不入 / 画面一抖一抖」 |
| `previewFirstLogged`(7000) 每次应用帧表重置 | 只有一次 L05，后续导入素材时无法再用日志判断首帧时间 |

**根因**：预览只有「解析完成 → 有帧表 → 才能渲染」这一条通道；
播放启动与「首帧直调」互相抢单飞锁；且首页缓存是冷的（见 B）。

### B. 预览抖动（22.9–36.5fps vs 目标 50）

单帧预算：`1000/50 = 20ms`。
单帧实际：
- 合成缓存未命中时：`decode 21.1ms + compose ~4ms = 25ms`（已超预算 25%）；
- 且 `previewExecutor` 是**单线程**，`prefetchPreview(index)` 在同一线程再解 **3 帧 ≈63ms**；
- 于是连续 miss 时，一拍的实际占用 **≈88ms**，而下一拍 20ms 后就到 → `previewRenderBusy` 为 true → 丢帧。

`composedCache` 只能放 **≈26 帧**（64MB ÷ 2.43MB），循环 593 帧 → **命中率上限 ≈4.4%**。
所以「第一圈之后丝滑」的前提（v1.10.59 注释里写的）在这份素材上**不成立**。

### C. 导出时拖滑块迟钝

两个独立来源，都要一起处理：
1. **输入路径同步截图**（3.5）：`capture()` 10MB 分配 + 全屏 `draw()`；
2. **导出 116ms/帧 × 单线程**：任何时刻 UI 线程的竞争者都有一整核跑满；
   同时 `postToUi` 每帧一次进度回调（593 次，`onProgress.invoke` 在导出线程 → `ui.post`）。

注意：用户要求「滑块迅速透明」是**提示本身**，保留不动；迟钝来自「弹窗为什么要现做一张全屏截图」。

---

## 5. 根因假设与判据（给外部 AI 排序用）

| 编号 | 假设 | 判据（低成本验证） | 当前证据 |
|---|---|---|---|
| H1 | 预览合成尺寸按素材（887×685）而非显示尺寸，导致解码+合成超预算 | 把 `previewCanvas()` 上限改成预览控件像素，看 `P20` 是否到 50fps | `W48 WebP=21.1ms` 已超 20ms 预算 |
| H2 | `prefetchPreview` 与渲染共用单线程，是丢帧的主因之一 | 临时 return 掉 `prefetchPreview`，看 `P12` 数量 | 3 帧 ×21ms ≈63ms，占一拍 3 倍 |
| H3 | `composedCache` 预算太小（26 帧），593 帧循环必然大量 miss | 打印命中率（可在 `renderFramePath` 计数） | 64MB / 2.43MB = 26 |
| H4 | 第 0 帧被 `previewRenderBusy` 挡住（启动时缓存冷、线程忙） | 启动瞬间强制同步出第 0 帧后再开拍 | `P12 idx=0` 实锤 |
| H5 | 解析期没有「先上第 0 帧」的通道 | 在 `loadPreviewFrames` 开头解一帧上屏 | `L05` 只在解析结束时出现 |
| H6 | 弹窗 `capture()` 全屏截图做在输入分发里 | 真机 trace：`dispatchTouchEvent` 耗时；或临时去掉 capture 看手感 | 代码路径 3.5 |
| H7 | 导出单线程串行编码，导致 116ms/帧，拉长「迟钝窗口」 | 与烘焙对比：同 593 帧 8 线程只需 13.8s | 导出 69s vs 烘焙 13.8s |
| H8 | `L31` 在导出期间冻结预览，用户误以为「界面卡住」 | 看 W50/L31 行；模糊弹窗是否在导出全程常态显示 | 设计如此，但体验上是「慢」 |

---

## 6. 「零迟钝」的硬指标（建议写成门禁）

用户原话：「无论素材体积有多大，必须做到零迟钝，毫秒级反馈」。
工程上要落成可测的数字，否则永远在感觉里打架：

| 场景 | 指标 | 测法 |
|---|---|---|
| 任意触摸 DOWN → 视觉反馈（按下/弹窗/透明） | **≤ 16 ms**（1 帧） | `dispatchTouchEvent` 内不许有分配/截图/inflate；trace 打点 |
| 滑块拖动跟手 | **≥ 58 fps**（每帧 ≤17ms） | 导出运行中拖 `seekScale`/`seekAlpha`，帧率计 |
| 导入后第一帧 | **≤ 300 ms**（与素材体积/帧数无关） | `onActivityResult` → 首帧 `setImageBitmap` 的时间差 |
| 预览播放 | 目标 fps 的 **≥95%**，丢帧 ≤5% | `P20` 每秒一条 |
| 导出期间的 UI | 交互帧率 ≥55fps，弹窗可见 ≤32ms | 同滑块测试 |
| 导出总时长 | 不设绝对上限，但**必须并行**（≥4 线程编码） | 与烘焙速率对齐（≈42 帧/s） |

**设计红线**（任何一条违反，就不可能有毫秒级反馈）：
1. 输入分发路径（`dispatchTouchEvent`/`onTouchEvent`/`OnClickListener`）里**禁止**：
   `Bitmap.createBitmap`、`View.draw` 到离屏 Canvas、`LayoutInflater.inflate`、文件 I/O、`Thread.join`、锁等待。
2. 预览的时间轴必须能**在没有合成任务时也前进**（落后就跳帧，但不许把渲染队列拖长）。
3. 任何「为了好看」的全屏效果（模糊、玻璃、过冲动画）都必须**预先构建**，不能按需现做。

---

## 7. 需要铲除 / 重构的代码清单（按优先级）

> 用户要求「铲除与这问题相关的代码，以及其他功能的底层隐患」。以下每条都给「为什么」，
> 但**今天没有动代码**，等外部 AI 意见 + 用户批准。

### P0（直接决定三条现象）

| # | 位置 | 问题 | 建议 |
|---|---|---|---|
| 1 | `OutputBusyDialog.capture()` 462–480 + `show()` 120 | 全屏截图 + 软件重绘在输入分发里同步做 | **删掉 capture**：改为页面静止时缓存一张底图（或干脆不模糊，用纯色 scrim）；弹窗实例**预创建**，`show()` 只做 visibility |
| 2 | `ConfigActivity.showOutputBusyDialog` 6133 | `new OutputBusyDialog(...)` 在 ACTION_DOWN 里同步建 | 首帧前预创建 + 复用；触摸路径只 `post` 一次「请求显示」 |
| 3 | `ConfigActivity.prefetchPreview` 7450 + `previewExecutor` 380 | 预取 3 帧同步占用唯一渲染线程 | 移出渲染线程；或改成「只在空闲 tick 预取 1 帧」；或删除（缓存命中率本来只有 4%） |
| 4 | `ConfigActivity.previewCanvas()` 7172 | 按素材尺寸合成（607k px），而非预览控件尺寸 | 上限 = `min(预览控件像素, 素材)`，预计降到 1/3 像素 |
| 5 | `ConfigActivity.renderFramePath` 7242 单飞丢帧 | 丢帧但**不降级**：既不降尺寸也不跳缓存 | 落后时直接上「最近一帧已合成缓存」或降分辨率重合成 |
| 6 | `ConfigActivity.composedCache()` 7215 / `previewCache()` 7488 | 两份额度各 `heap/4`，实际可用 26 帧；50fps 下 0.5s | 统一预算 + 以「帧数」而非「KB」为口径；或改环形数组 + 预分配（593 帧全量不现实，按 1 秒窗口设计） |
| 7 | `ConfigActivity.loadPreviewFrames` 6535 | 解析期不画任何东西；`pendingFirstFrame` 与「新导入」无关 | 解析一开始就解第 0 帧上屏（复用导出的流式解码，单帧成本 ≈30ms） |
| 8 | `MediaImporter` 导出串行（`runExport` + `writeFrame` 710） | 单线程 116ms/帧，是「迟钝窗口」的长度来源 | 解码串行、**编码并行**（有界队列、保序写盘），对齐烘焙的 8 线程模型 |

### P1（底层隐患，与本次症状有耦合）

| # | 位置 | 隐患 |
|---|---|---|
| 9 | `ConfigActivity.startPreviewFramesPlayback` 7046–7080 | 两套时钟并用：`iv`（整数 ms）与 `spanMs`（素材时长归一）。注释里已承认 33.3→33 会丢零头；两套口径同时存在，任何一处改动都会让 L35 自检撒谎 |
| 10 | `renderFramePath` 7232 `paramsKey()` | 每帧拼一个 ~80 字符的 key（含两次 `append(selectedShape)`，见 7194 与 7206 **重复**）—— 每帧一次字符串分配，且 key 语义冗余 |
| 11 | `ConfigActivity.dispatchTouchEvent` 5893/5915 | 每个事件 `new Rect()`、`getGlobalVisibleRect` 遍历 20 个控件、`thumbHit` 取 drawable bounds；DOWN 之外还会重复走 `LOCK_TRACK` 的 `scrollBy`。虽不是主因，但都在 UI 线程 |
| 12 | `OutputBusyDialog` + `BlurBackdropView` | 10MB 位图的生命周期、`UiModeWatcher` 重建、`apply(t)` 双驱动 —— 一个「提示」背了三套动画与一层截图层 |
| 13 | `ConfigActivity.busy` 期间的 `L31` | 导出中所有非冻结帧被丢；预览完全静止 → 用户认为「界面死了」 |
| 14 | `MediaImporter.writeFrame` 720 | 每帧 `compress` 前可能 `bitmap.copy(ARGB_8888)`（HARDWARE 兜底），一次多余全尺寸拷贝 |
| 15 | `GifStream.toBitmap`（`GifFrames.java` 608） | 每帧两次分配：`createBitmap(全尺寸)` + `createScaledBitmap(目标尺寸)`；887×685 每次 ≈2.4MB + 1.6MB，593 帧 = 2.4GB 的分配量（GC 压力） |
| 16 | `AnimStream` 每帧 `onProgress.invoke` → `ui.post` | 593 次跨线程投递，未节流（导出进度条只需 ~10Hz） |
| 17 | `W33 java=11/256MB` | 堆上限 256MB、未声明 `largeHeap`；缓存全部按堆比例分配，一旦预览缓存上调就会 OOM |
| 18 | `ConfigActivity` 的 `previewFirstLogged` | 只记一次 L05，重新导入素材时不再记录 → 排查「首帧到底什么时候上屏」时缺证据 |

---

## 8. 已排除的方向（避免外部 AI 重复劳动）

| 排除项 | 为什么 |
|---|---|
| 素材字节体积（210MB）导致迟钝 | 本次素材是 685×887/711 帧；成本由像素×帧数决定。210MB 那个已经在 v1.10.498 改成流式解码，不再读整份内存 |
| 预览帧率「时钟算错」 | `L35` 自检 ✓（593×19ms=11850ms vs 素材 11860ms，差 10ms）；`P20` 实测 fps 与「丢帧数」严格对得上 |
| 桌面推帧性能 | `W10` 50.0fps、`W23` 单帧 2.4–3.2ms、掉帧 0 —— 桌面这条链是健康的 |
| 编码质量太高导致慢 | 滑块 100 实际走 q95（887>720）；`W27` 单帧编码 144ms 是全尺寸 WebP 的正常量级，不是 bug |
| 导出参数锁（v1.10.497）设计错误 | 逻辑正确（W50 一条），但它**恰好在输入路径上建弹窗**，把「锁参」变成了性能问题 |
| 线程太多抢核 | `W23 频率均=2025MHz(掉0%)` 时是健康的；`12:37:01` 的 1262MHz(掉38%) 是后台受限，与本问题无关 |

---

## 9. 请外部 AI 回答的问题（按重要性）

1. 在「593 帧 × 887×685 × 50fps」这个具体规模下，预览要做到 50fps，**必须**把合成尺寸降到多少？
   给出像素预算反推公式（含 WebP 解码 ~21ms 的实测常数）。
2. `prefetchPreview` 3 帧、`previewCache` 与 `composedCache` 各 `heap/4` 的设计，在 256MB 堆上
   最优的「缓存容量 / 预取深度 / 降级策略」组合是什么？请给可执行参数，不要泛泛而谈。
3. 「触摸 → 弹窗可见 ≤32ms」在不删模糊效果的前提下，怎么实现？
   （预截图？RenderEffect？`WindowManager` 预挂载？）请给 Android 8.1–15 都成立的方案。
4. 导出并行化的最小改动方案：GIF 流式解码是顺序的，编码可并行 ——
   有界队列 + 保序落盘的正确写法与队列深度怎么定？（对照烘焙 8 线程 42 帧/s 的实测）
5. 「解析期先上第 0 帧」会不会与 `previewGeneration`/`extractGeneration` 的代次机制冲突？怎么接最干净？
6. 第 0 帧被 `previewRenderBusy` 丢掉：是「启动前同步渲染一帧」好，还是「播放 tick 允许等待一次」好？
7. P1 列表里哪几条是**真正的隐患**（会随素材变大而恶化），哪几条只是风格问题？

---

## 10. 复现与验收

**复现**（真机）：
1. 选 685.gif（685×887、711 帧、50fps）→ 看解析进度到 100% 的这段时间预览区是否空白；
2. 解析完成后看第一帧出现得是否干脆（对照 `P12 idx=0` 与首圈 `idx=2…131`）——当前是「一秒里只出几帧」；
3. 观察 3 秒一条的 `P20 预览实测`，目标 50fps，当前 22.9–36.5fps；
4. 点「导出组件」，在导出的 69 秒里拖「内容/蒙版旋转」滑块，记录「手指按下 → 弹窗出现」的时间。

**日志锚点**（验收时只看这几行）：
`L22 → L05`（首帧延迟）、`P20`（预览实测 fps 与丢帧数）、`P12 idx=`（丢的是哪一帧）、
`T01/T27/P02`（导出参数与帧数）、`W50`（锁参命中一次）、`W27 编码均`（单帧成本）、
`W48 WebP=`（解码常数）。

**建议门禁**（等方案定稿后落成脚本）：
- 源码断言：`dispatchTouchEvent` 调用链里出现 `createBitmap`/`draw(` → FAIL；
  `prefetchPreview` 出现在 `previewExecutor` 任务体内 → FAIL；
  `previewCanvas()` 上限 > 预览控件像素 → FAIL；
- 真机断言（日志）：`P20` 的 fps ≥ 目标 ×0.95 且「丢 N 帧」= 0；
  `L22→L05` 的间隔 ≤ 300ms（素材 1000 帧级也一样）。

---

## 11. 附：日志中值得直接引用的 12 行

```
12:34:25 D L02 预览帧: 开始解析 kind=GIF fps=50.0 边长=887
12:34:44 D P12 预览合成跟不上，丢一帧 idx=0     ← 日志文件是新→旧，此行在 L05 之后
12:34:44 D L05 预览帧: 首帧上屏 685x887
12:34:47 D P20 预览实测 22.9fps（目标 50）/69 帧/3009ms，合成跟不上丢 87 帧
12:34:56 D P20 预览实测 35.5fps（目标 50）/107 帧/3013ms，合成跟不上丢 46 帧
12:34:56 D T01 开始导出 gen=1 fps=50 边长=887 质量=100 布局=FIT
12:34:56 D T27 动图导出参数 边长=887 帧数=593 质量=100（帧数多：边长≤720 时仍无损，超过则退回 q95）
12:36:05 D L32 导出进度 593/593 (100%)
12:36:20 D W13 预热完成 组件=134 画布=685x887 完成=593 失败=0 用时=13822ms 速率=42帧/秒 CPU=76%
12:36:20 D W27 合成分解 帧=995 合计均=4ms … 编码均=144ms(593帧) 编码CPU=75%
12:36:20 D W48 解码对拍 组件=134 帧=296/593 画布=685x887 WebP=21.1ms JPEG=6.4ms 合成=0.2ms 合计=6.6ms 省=69%
12:37:04 D W33 进程内存 原因=日志页 java=11/256MB PSS=209MB native=45MB 磁盘帧=190MB 本机=3083/15350MB
```
