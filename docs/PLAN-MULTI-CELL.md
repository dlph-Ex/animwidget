# 多格组件（1 个 host 装 6 路）实施计划

> 依据：三家外部复核一致 —— 6 个独立组件每秒 300 次 `updateAppWidget`，AOSP 每个 host 只保留
> 一个 in-flight apply、新帧即 cancel，门槛 `T_apply ≤ 1/(N·f) = 3.33ms`（未测）。
> 合并成 1 个 host 后 apply 降到 50 次/秒，门槛回到 20ms。**纹理带宽 1.106GB/s 不变**（物理下限）。

## 已落地（v1.10.490 / vc604）
- `res/layout/widget_multi.xml`：2 行 × 3 列，6 个 ImageView（`widget_cell0..5`），`centerCrop`
- `R.java`：新增 `layout.widget_multi` + 6 个 id，并用 `scripts/sync-r.py` 与资源表对齐（改了 8 条）
- **没有任何行为变化**：这个布局现在还没被任何代码引用，选择器里也看不到它

## 第 2 片（v1.10.491 / vc605，已可见可测）
1. ✅ `res/xml/widget_multi_info.xml`（4×2 默认、双向缩放、`configure=ConfigActivity`）
2. ✅ `AndroidManifest.xml` 增 `receiver .MultiCellProvider` → 组件列表出现「多格组件」
3. ✅ 「这一路是多格」的判据 = `AppWidgetManager.getAppWidgetInfo(id).provider`（系统事实，
   不存标记；`MultiCell.isMulti` 按 widgetId 缓存）。**没有**动 `WidgetConfig`（那个类是
   data class 编译产物，加字段风险大、收益为零）
4. ✅ `WidgetUriPush.pushCached` 多格分支：一次 `RemoteViews(widget_multi)` + 6 次
   `setImageViewBitmap(同一张)` → **每拍 1 次 apply**
   → 判据：`W23 渲染=` 从 900 次/3s 变成 **150 次/3s**（6 格仍在动）
5. ⚠️ 多格强制走位图通道（`wantShared` 直接返回 true）——URI 只能填一个格子

## 第 3 片（可选）
- 每格独立素材/相位：需要 6 份 frame 序列与 6 个解码任务；上传带宽不变，解码会回到 6 倍
- 配置页入口 + 每格选素材
- 性能组：预读深度 `lead`、共享表容量、分层帧开关（**不是线程数**，见下）

## 关于「核数自定义」——实测结论：不是瓶颈，不建议放进性能组
六路 720p50 的真机数据（`Component_2608_58_26.txt`）：
- 解码 **152 次 / 3 秒**（900 次推送），均 11ms ⇒ 约 **0.56 核**
- 推送 300 次/秒 × 1.4ms ⇒ 约 **0.42 核**
- 解码池 4 线程，`排队峰` 只有 5~16，`唤醒迟到均=0`
⇒ 解码线程数加不加都一样；加了反而抢 tick 线程。真正能调的是「预读深度」和「共享表容量」。
