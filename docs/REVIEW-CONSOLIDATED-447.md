# 四家评审汇总与下一步（v1.10.447 决策稿）

> 输入：DeepSeek（源码级，AOSP main/14/15）、Claude-1、Claude-2、ChatGPT（契约级）。
> 本文只保留「能改变我们做法」的部分，并如实记录**我方被推翻的结论**。

---

## 一、四家共识（可以当事实用）

| 问题 | 结论 |
|---|---|
| 有没有「不编码不解码」的通道 | **没有**。`RemoteViews` 全类无 `Surface`/`HardwareBuffer`/`SurfaceControl`；`INFLATER_FILTER` 只放行 `@RemoteView` 注解类，无法注入自定义 View |
| launcher 能否拿硬件位图 | **不可能**：`ImageView.getDrawableFromUri` 硬编码 `ALLOCATOR_SOFTWARE`（AOSP 14/15 均如此）→ 纹理上传不可省 |
| 跨组件共享解码结果 | **没有**（`Drawable` 缓存只对 `android.resource://` 生效）；唯一可共享的是「我们自己解码后发的同一个 Bitmap」 |
| `AdapterViewFlipper` 自动翻页 | **不可用**：`setFlipInterval/setAutoStart` 非 `@RemotableViewMethod` → `ActionException`；`AppWidgetHostView` 里没有任何 `advance()` 调用点 |
| sprite sheet + 裁剪 | **不可用**：无 `srcRect`、`setImageMatrix` 非 remotable |

## 二、我方被推翻的三条（必须改口）

1. **「Binder 1MB，位图传不过去」——错。** `Parcel` 对 >12KB 位图走 ashmem、传 fd；若位图本身是 ashmem + immutable（我们 `ALLOCATOR_SHARED_MEMORY` 的产物正是），**只传 fd、零像素拷贝**。真实上限是 `6 × 屏幕像素`（本机 ≈20.5MB），720p 单帧 3.6MB 安全。
   → `setImageViewBitmap` 这条「把解码搬出 launcher」的路，**是活的**。
2. **「AOSP 有隐藏频率上限」——错。** `AppWidgetServiceImpl` 无 rate limiter（逐字查零命中）。真正丢弃点是 launcher 的 `AppWidgetHostView.applyRemoteViews`：新帧到达即 `mLastExecutionSignal.cancel()`。**这是框架行为，不是 ROM 差异**。
3. **「N=5 崩是因为核数与带宽」——归因可疑。** 4×0.65=2.6 核，5 路 3.25 核是线性的，不会产生 2~3 倍延迟。**串联+取消（H1）才解释得通**：vivo 的 apply executor 很可能是个固定小池，第 5 路抢不到槽位 → 前一份被 cancel → 端到端延迟翻倍。
   且我们当初「解码池 4→6 无收益」测的是**我们自己进程**的池；稳态播放时我们根本不该解码（解码在 launcher）。**那条结论不能用来支撑「核数上限」。**

## 三、改动优先级（合并四家意见）

**P0（当天可做，成本低、可能直接解 N=5）**
1. 播放期零后台工作（烘焙只在息屏/充电）——已有 restricted 机制，确认没漏。
2. **不透明内容帧 + 静态 alpha overlay**：`FrameLayout{ content(每帧换 URI，JPEG) , overlay(蒙版/阴影/边框，常驻) }`；layoutId 不变 → host 走 `canRecycleView()/reapply()`，overlay 永不重解。估计每帧解码 12–13ms → **4–6ms**。
3. 同素材 N 路改 `setImageViewBitmap` + 共享 Bitmap（解码 N→1，传输 fd dup）。
4. **更新错相**：N 个组件按 20/N ms 错开，避免同一毫秒撞同一个 executor 槽位（一行代码）。

**P1（1~2 天，架构级）**
- **单文件动画 WebP**：一次 `setImageViewUri` → `AnimatedImageDrawable` 自播 → 零 IPC、`PlaybackService` 可下掉、App 退出后仍在动。
  ⚠️ 我们目前**没有动画 WebP 编码器**（Android 无公开编码 API、本工程无 NDK）。
  ✅ 但有解：**自写 RIFF/ANMF 封装器**——把已经烘好的单帧 WebP 的 `ALPH`/`VP8 `/`VP8L` 块直接拼进 `ANMF`，纯 Java ~200 行，不需要 libwebp。
  验证手段已验证可行：本机 PIL 支持读写动画 WebP（可逐帧比对像素与时长）。

**P2（研究性）**
- `setImageViewBitmap` 全量替代 URI：我们解码（`BitmapFactory.inBitmap` 环形复用 2~3 块）→ fd 给 launcher → launcher 只做纹理上传。风险：system_server 每帧 mmap 的 churn；复用缓冲时要 2~3 轮转，否则录屏会看到撕裂。

## 四、判别实验（先证明 H1，再谈改造）

| # | 做法 | H1 成立的预期 |
|---|---|---|
| E1 | N=4 + 第 5 个组件只显示**静态图**（不更新） | N=4 仍稳 50fps |
| E2 | N=5，其中一路降到 25fps | 整体立刻恢复 |
| E3 | `adb shell top -H -b -n1 -p $(pidof com.bbk.launcher2)`，N=4/N=5 各 10s | 热线程数不变、单线程负载翻倍 |
| E4 | `cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq` + `dumpsys thermalservice` | 频率平稳（若断崖则是 H2 热限） |

地面真值一律用**屏录帧差**（240fps），不要用我们自己的发送日志——`W10/W23` 只能证明我们发了多少，证明不了 launcher 画了多少。

## 五、我建议的下一步

1. **立刻做 E1**（无需发版：加一个只显示静态图的第 5 个组件，看 N=4 是否仍 50fps）——这一步决定后面所有投入方向。
2. 我这边同步开工两件不需要真机的事：
   - **P0-4 更新错相**（小、可回退）；
   - **动画 WebP 封装器 + PIL 离线自检**（先把「单文件零 IPC」这条赌注的可行性钉死，再谈接进导出链）。
3. P0-2（静态 overlay 拆分）排在其后：它会改观感与 RemoteViews 布局，必须配真机录屏比对。
