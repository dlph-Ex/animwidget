# 材料包：Android 桌面组件「零解码投帧」与 N>4 可行性评估（v2）

> 提交对象：外部技术评审（人或 AI）
> 目的：判断在**第三方 AppWidget（RemoteViews）**形态下，是否存在「不编码 / 不解码、直接把画面投到桌面」的通道；
> 若不存在，判断 N（同时播放的组件数）能否超过 4。
> 本文只陈述事实与实测数据，结论留空给评审方。
>
> **v2 修订**：① 更正一条我们原先写错的结论（Binder 传位图）；② 补上一条新能力（单文件动图 WebP 已实现并离线验证）；
> ③ 补两个新问题（launcher 里的 AnimatedImageDrawable 行为）。

---

## 0. 一句话现状

应用是「桌面动画组件」：用户选一段视频/GIF，App 把它逐帧合成（圆角蒙版/阴影/边框）后编码成 WebP 落到私有目录，
再通过 `RemoteViews.setImageViewUri(...)` + `AppWidgetManager.updateAppWidget(...)` **逐帧**切换图片，
launcher 每帧自己解码并上屏。

实测：**1280×720 每路解码 ≈ 0.65 个核；N=4 稳态 50fps 达标；N=5/6 崩。**
用户诉求：能否绕过「编码→解码」这一整段，直接把像素送到桌面。

---

## 1. 硬约束（评审请在此范围内提方案）

1. **不上采样**：画布长边 ≤ 素材长边。
2. 画布只由「素材比例 + 帧率档 + 长边上限」决定，用户拖动组件尺寸**不触发**重烘。
3. 画布上限：动图/视频长边 1920，图片 2560。
4. 必须保持：50fps 目标、圆角/阴影/边框（依赖像素 alpha）、N=4 路同时播放达标。
5. 必须是**标准 AppWidget**（可分发，不依赖自家 launcher、不需要 root/系统签名/无障碍）。
6. 已装机的 Shizuku 仅用于普通权限拿不到的查询，**不允许**用于帧投递（可讨论，默认否）。
7. 不接受：显示分频到 25fps、降分辨率保 N（用户已明确否决）。

---

## 2. 现有管线（数据流）

```
素材(video/gif)
   │  解码抽帧
   ▼
合成：圆角/阴影/边框/布局(fit/fill) → ARGB_8888
   │
   ▼  单遍烘焙（导出时一次性做完，按目标质量直出）
WebP(q≈93，带 alpha) 逐帧落盘   files/push/w<id>/<stamp>/frames/*.webp
   │                            （stamp = 渲染参数 + 素材指纹；换参数即换目录）
   ▼
RemoteViews.setImageViewUri(imageViewId, contentUri)   ← 每帧一次
   │
   ▼  AppWidgetManager.updateAppWidget(...)
launcher 进程：ImageView → content provider → ImageDecoder(ALLOCATOR_SHARED_MEMORY) → 纹理上传 → 上屏
```

节拍由我们自己驻留的服务按 fps 墙钟驱动（不是 launcher），所以帧率精度可控；瓶颈完全在 launcher 侧解码。

---

## 3. 实测数据（同一台机器，均为 50fps 目标）

| 场景 | 每帧解码 | 稳态 fps | 结论 |
|---|---|---|---|
| N=1，1080p 画布 | 12~13ms | 50.0 | 达标 |
| N=2 | 12~13ms | 50.0 | 达标 |
| N=4，1280×720 | ≈13ms（0.65 核/路） | 50.0 | **达标（当前上限）** |
| N=5 | 解码均涨到 2~3 倍 | 掉帧/降频 | 崩 |
| N=6 | 同上 | 崩 | 崩 |
| 1920×1080 单路 | ≈30ms（1.5 核） | 50.0 | 单路可行，N=4 不可能 |

其它实测：
- 解码池 4→6 线程**无收益**：瓶颈不是线程数。
- `ImageDecoder` + `ALLOCATOR_SHARED_MEMORY` 直解：只省 1.5~2ms/帧（占比小）。
- 后台烘焙线程（nice10，6 线程）会让解码均从 12~13ms 涨到 20~22ms、掉 3~5fps —— **CPU 是共享的**。
- 连续重复帧跳过解码（指纹比对）已做，收益取决于素材。
- 画布与「桌面格子」已解耦，拖动组件不再重烘。

**关于 N=5 的假设（H1，待证）**：vivo launcher 的 apply executor 可能是固定小池，
第 5 路抢不到槽位 → 前一份 `applyRemoteViews` 被 cancel → 端到端延迟翻倍。判别实验：
N=4 + 第 5 个组件只显示**静态图**（不更新），若 N=4 仍 50fps 则 H1 成立。

---

## 4. 已评估 / 已否决的方案

| 方案 | 结论 | 理由 |
|---|---|---|
| 解码池 4→6 | 否决 | 实测无收益 |
| JPEG 帧（解码更便宜） | 否决 | JPEG 无 alpha；RemoteViews 不能裁剪，圆角/阴影靠像素 alpha |
| 显示分频 25fps | 否决（用户） | 违反 50fps 目标 |
| 降分辨率保 N | 否决（用户） | 画质优先 |
| 帧做进 sprite sheet | 未实现 | ImageView 无法用 srcRect 取局部，也没有 matrix 能力 |
| `setImageViewBitmap`（Binder 传位图） | **待验证（原判断有误，见下）** | — |
| 一次性解码整段视频再推 | 不成立 | RemoteViews 只接受静态图/URI，没有视频播放面 |
| 自建 AppWidgetHost 覆盖桌面 | 未评估 | 需要 overlay 权限与常驻前台，体验风险大 |

**更正（v2）**：我们原先写「Binder 单笔 ≈1MB，1280×720 RGBA=3.6MB 传不过去，所以 `setImageViewBitmap` 否决」——
这条**不成立**。`Parcel` 对 >12KB 的位图走 ashmem、只传 fd；若位图本身是 ashmem + immutable，
理论上**零像素拷贝**。所以这条路是活的，真正待确认的是 launcher 侧拿到 Bitmap 后是否仍要每帧纹理上传。
请评审直接判定我们这条更正对不对。

---

## 5. 我方判断（**请逐条指认在哪家 ROM / 哪个 API 级别上不成立**）

1. 第三方 AppWidget 的 RemoteViews **没有 Surface / SurfaceView / TextureView / VideoView**，
   因此不存在「直接把合成流交给桌面」的官方通道。
2. RemoteViews 每帧内容只有两类载体：`Bitmap`（Binder）与 `Uri/Icon`（launcher 侧解码）。
   前者受 Binder 语义限制（见上文更正），后者必然解码。
3. `updateAppWidget` 的高频调用在多数 launcher 上会被合并/丢弃；实测本机 vivo 能承受 50 次/秒，
   但这不构成通用保证。
4. 「零解码」只能靠 launcher 侧自己持有解码器或共享内存（`Icon` 指向支持共享内存的 provider、
   或 launcher 复用 `ImageDecoder` 的输出缓冲）—— 需逐 ROM 验证。

以上四条是否有反例（尤其 HyperOS/MIUI、ColorOS、OriginOS、One UI）？

---

## 6. 新能力：单文件动图 WebP 已实现并离线验证

我们已能做**单 URI 动画**：把逐帧 WebP 的 `ALPH` + `VP8/VP8L` 块直接拼进 `ANMF`
（`VP8X` + `ANIM` + N×`ANMF`），不重新编码，纯 Java 手写容器。

离线验证（第三方解码器 Pillow 读回，逐帧逐字节比像素 + 比时长，有损/无损两条路）：
45 条断言全通过，帧数/画布/时长/循环次数均正确。

**这意味着一条新路线**：桌面组件只放一个 URI → launcher 的 `ImageView` 得到动图 WebP →
若其内部走 `AnimatedImageDrawable`，则**播放在 launcher 侧自持，零 IPC、零 updateAppWidget、
App 退出后仍在动**。这条路一旦成立，`PlaybackService` 可以下掉，N 的上限也不再受我们推送能力约束。

我们不确定的正是「launcher 里到底会不会自播」——见问题 9/10。

---

## 7. 想请教的具体问题（每条请给：可行 / 不可行 / 需真机验证 + 最小验证步骤）

1. 有没有第三方可用 API，把**已解码帧**交给 launcher 而不经 Binder 拷贝？
   （`Icon` + Bitmap 共享内存？RemoteViews 有没有 `HardwareBuffer`/`SurfaceControl` 的隐藏口子？Android 12~15 有无新增？）
2. `AdapterViewFlipper` + `RemoteViewsService`（collection widget）能否**逐帧翻页**，
   绕开「每帧一次 updateAppWidget」？每页仍是 ImageView + URI（仍解码），但省掉跨进程 update——这个判断对吗？
3. 有没有办法让 launcher **只解码一次、反复使用**（例如 N 帧拼成一张大图 + 裁剪窗口每帧换位置）？
   `ImageView` 能否靠 `setImageMatrix`/`setScaleType` + `clipChildren` 做到？
4. 如果必须解码：WebP vs AVIF vs HEIF vs PNG 在 `ImageDecoder` 下的**每帧解码耗时**（720p/1080p、带 alpha）？
   换格式的收益是否值得（代价是烘焙变慢）？
5. `ALLOCATOR_SHARED_MEMORY` 在 launcher 进程里对**同目录连续帧**是否有可复用输出缓冲？
6. 「一份数据、N 个组件共用」：同素材多组件时，launcher 侧能否共享纹理/解码结果（同一 URI / 同一 Icon 对象）？
7. AOSP 的 `AppWidgetServiceImpl` 是否有隐藏速率上限？各家 ROM 通常怎么改？
8. `setImageViewBitmap` + ashmem 共享 Bitmap 这条路，在 `AppWidgetHostView` 里是否真的比 URI 省？
   （我们按「省解码、仍上传纹理」理解，请纠正。）
9. **动图 WebP + launcher**：各家 launcher 的 `ImageView`/`RemoteViews` 是否会让动图自播？
   是走 `AnimatedImageDrawable` 还是只画第一帧？有没有动画帧率上限、循环限制、
   息屏/不可见时是否自动暂停？
10. 若动图自播成立：它是**每帧解码**还是**一次性解到内存**？一个 1080p、300 帧的动图 WebP
    在 launcher 进程里常驻内存大概多少？会不会被 launcher 当"大图"回收？

---

## 8. 设备 / ROM 事实

- 设备：vivo，launcher 包名 `com.bbk.launcher2`；`minSdk 26 / target 35`；Android 具体版本待补。
- **该 launcher 从不上报组件尺寸**：`OPTION_APPWIDGET_SIZES` 缺失，`OPTION_APPWIDGET_MIN/MAX_*` 恒为默认 110dp。
  应用只能按 dp 估算格子（例：130×115dp → 426×377px）。
- 屏幕 1260×2720 级别，三键导航。
- 应用全部页面锁竖屏。
- 桌面组件在 App 退出后仍被 launcher 持续绘制；App 进程回后台会被限流（无法后台全速烘）。

---

## 9. 我们希望的产出形式

- 对第 5 节四条判据逐条给出「成立 / 在 X 上不成立 + 依据」。
- 对第 7 节每个问题给出：**可行 / 不可行 / 需真机验证**，并给最小验证步骤（能 10 行代码验证的最好）。
- 若有「第三方 App 在桌面组件里做到 50fps 无解码」的实例，请指出其机制（可反编译/可观察的行为特征）。
- 若结论是「N>4 在标准 AppWidget 下不可能」，请直接说，并指出唯一可能的例外路径。
