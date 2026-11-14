# P0-2 落地计划：不透明内容帧（JPEG）+ 常驻 alpha overlay

> ## ⛔ v1.10.478 起**否决**（不要再按本文件开工）
>
> 外部两份复核（DeepSeek / Claude）独立判定：这条方案在**圆角蒙版**下**几何上不成立** ——
> overlay 只能往上叠、擦不掉下面的内容，不透明 JPEG 的方角与阴影半透明区会直接透出来。
> 我们用真实几何（879×869 画布 / 1280×720 素材 / 阴影 100 / 圆角半径 73px）复算并出图证实：
> 四角方角面积 0.60% 画布（铺满档 2.22%）、`>8` 差异 0.82%（铺满档 2.52%）、**最大 205~239**。
> 而且它的**有效域**（无圆角/无阴影/铺满/无 letterbox/素材无 alpha）**正好是** v1.10.292 起
> `alphaState` 已经在走 JPEG 的那个域 ⇒ 增益 ≈ 0。
>
> 完整证据、两条纠正（13ms 的归属、§2 耦合已过期）、以及**唯一还站得住的形态**
> （分层放进**我方进程**：JPEG 解码 + 常驻蒙版合成）见 `docs/REVIEW-P0-2-EXTERNAL.md`。
> 本文件以下内容保留作历史记录，第 2 节的「层内像素自检（W46）」也已作废 ——
> 它恒等于 0，真实差异只发生在**叠壁纸**的时候（判据改为 `scripts/p0-2-overlay-probe.py`）。

---

> 来源：`docs/REVIEW-CONSOLIDATED-447.md` P0-2。用户决议（本版）：**先做这条**，共享解码（P0-3）押后。
> 目标：把「每帧解码 12–13ms」降到 **4–6ms** —— 内容帧变成**完全不透明**的 JPEG（解码便宜 3 倍），
> 圆角 / 阴影 / 边框交给一张**常驻**的 alpha overlay（只解一次、永不重解）。

---

## 1. 为什么这条路成立（与 N5 的"否决 JPEG"不矛盾）

`HANDOFF-ZERO-DECODE-N5.md` 否决的是「**整帧**用 JPEG」：整帧要自带圆角/阴影的 alpha，JPEG 给不了。
P0-2 用的是另一种分工：

```
FrameLayout{
    content : ImageView  ← 每帧换 URI，JPEG，完全不透明（方图，无圆角、无阴影）
    overlay : ImageView  ← 常驻，一张带 alpha 的 WebP/PNG：中间挖圆角透明洞，外圈画阴影+边框
}
```
桌面按子 View 顺序合成：content 在下、overlay 在上 → 洞里的内容透出来，洞外是阴影/边框。
**alpha 全部由 overlay 提供，内容帧不需要 alpha**，所以 JPEG 合法。

前提（必须由判据把关，不能靠假设）：
1. 内容帧**完全不透明**（有 letterbox/黑边 → 透明区域会露出空白）→ 用现有 `alphaState` 判；
2. 画布尺寸**不随拖动变化**（否则 overlay 的描边被缩放拉粗）→ 现有"画布与格子解耦"已满足；
3. layoutId 保持不变（host 走 `canRecycleView()/reapply()`，overlay 不重解）。

---

## 2. 分两步，第一步不动布局、不碰桌面

### 第 1 步（本版做）：应用内**像素等价**自检 + 分层合成

不改 RemoteViews、不加 id，只在 `FrameComposer` 里做两件事：

1. **把 overlay 拆成两张常驻位图**（同一代只生成一次，指纹 = 现有 `FrameLayers.fingerprint`）：
   - `shadowLayer`：`drawFrame(shadow=强度, border=false, 内容=空)` → 只有阴影；
   - `borderLayer`：`drawFrame(shadow=0, border=true, 内容=空)` → 只有边框。
2. 每帧合成改成：画 `shadowLayer` → 画内容（`drawFrame(shadow=0, border=false)`）→ 画 `borderLayer`。

**验收（硬判据）**：对同一帧，用「旧单趟 compose」与「新三层合成」各出一张图，**逐像素比较**：

```
W46 分层自检 组件=… 差异=N 像素 / 最大=…   （N 必须为 0，否则自动回退旧路径）
```
- N = 0 → 保持分层，下一版才接 RemoteViews 双 ImageView + JPEG 内容；
- N > 0 → 立刻回退旧路径并记录差异位置（说明拆分顺序与原实现不等价，先修这个）。

### 第 2 步（下一版）：接 RemoteViews

1. 布局加 `widget_overlay`（FrameLayout 第二个子 View，`scaleType` 与 content 一致）；
2. `sync-r.py` + `stable-ids` + arsc 三处同步（这个仓库最容易出事的改动，单独一版）；
3. overlay 只在**换代**时推一次（`setImageViewBitmap` 常驻图，和 content 同一画布尺寸）；
4. 内容帧按 `alphaState` 选编码：全不透明 → JPEG(q92~95)、否则 → 仍用带 alpha 的 WebP；
5. 真机验收：`W23 共享=解码均` 应从 12–13ms 掉到 4–6ms，且画面与 475 逐像素一致（同素材同参数录屏比对）。

---

## 3. 风险与回退

| 风险 | 处理 |
|---|---|
| overlay 与 content 的缩放不一致（两者 intrinsic 不同 → 边缘错位） | 两张图**同画布尺寸**、同 scaleType；R 层加断言 |
| JPEG 有损 → 边缘出现色渗出（紧贴 overlay 的洞边） | q92 以上 + overlay 边缘留 1px 重叠；像素自检覆盖 |
| launcher 复用 RemoteViews 时只 reapply content | overlay 不变时不需要 reapply；换代时两张一起推 |
| 拆分顺序与原实现不等价 | 第 1 步的 W46 逐像素自检直接拦住 |

## 4. 与"四项口径"的关系

这条**只改每帧的解码成本**，不改画布、不改帧率、不改时长 —— 所以 `W47` 的四项必须**一个都不变**。
任何变化都说明分层改到了不该改的地方。
