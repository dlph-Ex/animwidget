# HANDOFF：导出到桌面后「先小一圈再恢复」的第一帧（v1.10.325 交接给外部分析）

> 这份文件是**给另一个 AI 看的完整材料**，不含寒暄。所有数字都来自真机日志原文，
> 每条都已标注出处（见 §4）。看不懂的地方按 §5 的代码地图去读源码，路径都给了行号。

---

## 1. 症状（用户原话，多次复述，一致）

> 「导出至桌面后、**未播放前**，有 **1 帧**画面先是**比本应的小一圈**，再**变回正常大小**。」
> 「概率缩」——不是每次都有，但换素材/重新导出后经常出现。

补充事实（我从日志确认的，不是猜）：
- 发生时机 = **导出完成回到桌面后的头几秒**，播放还没稳定命中缓存之前；
- 只影响**画面内容在卡片里的占比**（看起来"缩了"），卡片的格子大小不变；
- 恢复后一切正常，帧率 48~49fps、命中率 98~100%；
- **只在「原始比例 / 固定比例」这类非填充布局**下出现（fill 模式用户没报过）。

## 2. 设备与环境

| 项 | 值 |
|---|---|
| 机型 | vivo V2426A，Android 16 |
| 屏幕 | 1260×2800，density = **3.28** |
| 桌面 | `com.bbk.launcher2`（vivo 原厂） |
| 组件格子 | 202×196dp → **662×643 px**（比例 **1.030**） |
| 素材 | 视频 1080×1920（比例 0.563），420 帧 @ 50fps，FIT（原始比例）布局 |
| 编码 | WebP q97（JPEG 通道未启用） |
| 推送 | URI 推送（帧落盘 + content:// 路径） |

## 3. 硬约束（全部由实测得出，分析时不可推翻）

1. **桌面用 AT_MOST 量 RemoteViews**：`ImageView.onMeasure` 取 `min(drawable.intrinsic, specSize)`。
   → 位图不够大，整张卡片会被量小一圈（"贴左上、露壁纸"）。
2. **`intrinsic` = 位图像素 × density**。日志证据：
   `W22 … 桌面摆=1867x1815px(位图×3.28；≥格子才铺满，已富余 1205px)`，
   而画布是 569×553：`569 × 3.28 = 1866.3` ✔。
   → 位图只要 ≥ **格子px ÷ density**（本例 662/3.28 ≈ 202px）就盖得住格子。
3. **A 方案（v1.10.298，用户已验收）**：`fitViewFor()` **只按布局判**——
   `FILL → centerCrop`，其余**一律 fitCenter**。fitCenter 等比内接，几何上不可能裁；
   代价是比例不匹配时四周露壁纸（用户已接受）。**不要再引入"按比例差决定 scaleType"的判据**。
4. **画布必须与格子同形**（v1.10.279 定案）：`canvasFor` 的结果比例 = 格子比例，
   这样 `fitCenter` 在比例吻合时是恒等变换。门禁 [12] 会扫描这条。
5. **vivo 报的 dp 不含它自己的桌面内边距**：要 +20dp（宽）/+5dp（高）再加回去。
   日志：options 报 182×191dp → 实际 202×196dp → 662×643px。

## 4. 三份真机日志的关键行（原文）

### 4.1 第一次（18:41，组件 283）

```
18:41:49 F11 尺寸来源=竖屏 130x115dp×3.28 → 426x377px → 格子=426x377px 比例=1.130
18:41:51 W28 帧画布 组件=283 画布=426x377 格子=426x377 布局=FIT
18:41:51 W22 尺寸 options 全量 {appWidgetMinWidth=182; appWidgetMinHeight=191; …; appWidgetSizes=[]}
18:41:51 F11 尺寸来源=竖屏 202x196dp×3.28 → 662x643px → 格子=662x643px 比例=1.030
18:41:51 W22 尺寸变更 → 帧=0 画布=569x553(比例1.029) 格子=662x643(比例1.030)
18:41:53 W13 预热开始 画布=569x553 帧=420 质量=97 线程=6
18:41:54 W13 首帧命中 距画布变化=2733ms 距开烘=1512ms
18:41:59 W13 预热完成 完成=420 用时=6318ms 速率=66帧/秒
18:42:02 W10 组件 283 实测 49.3fps 画布=569x553 传输=URI 命中=98%
```

### 4.2 第二次（18:51，组件 284）—— 与上面同型

```
18:51:08 W28 帧画布 画布=426x377 格子=426x377      ← 导出这一拍用的还是 110dp 默认尺寸
18:51:08 W22 尺寸 options 全量 {appWidgetMinWidth=182; appWidgetMinHeight=191; …}
18:51:08 F11 202x196dp → 662x643px
18:51:08 W22 尺寸变更 → 画布=569x553 格子=662x643
18:51:11 W13 首帧命中 距画布变化=3133ms
```

### 4.3 第三次（19:29，组件 286，已装 v1.10.324 的「尺寸未知就跳一次」补丁）

```
19:29:04 W28 尺寸未知延后 id=286（launcher 仍报默认 110dp，等 700ms 重推）   ← 324 的补丁生效
19:29:04 F11 130x115dp → 426x377px
19:29:04 W28 帧画布 画布=426x377 格子=426x377                              ← 但下一拍又推了
19:29:04 W22 尺寸 options 全量 {182x191}
19:29:04 W22 尺寸变更 → 画布=569x553 格子=662x643
19:29:05 W32 重推单个组件 组件=286 原因=尺寸未知延后                        ← 324 的延时补推
19:29:05 W13 首帧命中 距画布变化=404ms 距开烘=0ms
19:29:05 W13 预热开始 画布=569x553
19:29:12 W13 预热完成 完成=420 用时=6938ms 速率=60帧/秒
19:29:12 W27 合成分解 … 编码均=55ms(421帧) JPEG=0/421 画布=569x553
```

**4.3 的结论**：324 的"只跳一次"不够 —— 跳过之后 20ms 的那一拍（播放服务每 20ms 调一次 `render`）
仍然按 426×377 推了错比例帧。325 已改成"只要 launcher 还报默认尺寸就一直跳，最多 5 秒"。

## 5. 代码地图（`/workspace/AnimWidget/src-v2/io/github/animwidget/`）

| 文件 | 函数 | 作用 / 注意点 |
|---|---|---|
| `WidgetRender.java` | `render(ctx, mgr, config, index, cache)` L1306 | **唯一推帧入口**。先看 `cachedUri`（命中就推 URI）→ 否则若 `busy/pending` 就"顶帧"（`nearestCached` 同画布 ±8 帧 → `nearestAnyCanvas` 同比例旧画布）→ 都不行就现场合成 |
| 同上 | `fitViewFor` L636 | A 方案唯一判据：`layout != FILL → fitCenter` |
| 同上 | `widgetSize` L927 / `computeWidgetSize` L947 | **3 秒缓存**（`SIZE_CACHE`）；DP 估算 + vivo padding +20/+5；真尺寸只有 launcher 在 options 里给 |
| 同上 | `canvasFor` L699 → `canvasFor0` → `renderCanvas` L1129 → `coverFloor` L659 | 画布链：素材长边上限 → 面积预算 → `coverFloor` 抬到"格子dp×1.30+24px"（intrinsic 下限），最后面积 cap 32 万 |
| 同上 | `budgetFor` L772（+`BakeCal`/`BakeLadder`） | 面积预算：fps≥45 压到 24 万；带 alpha 按内容占比折算；JPEG 通道放开到 220 万；**新增自校准系数** |
| 同上 | `skipWhileBoxUnknown` L1264（325 新增） | launcher 仍报默认 110dp 时这一拍不推；**只要还默认就一直跳**，硬上限 5 秒 |
| 同上 | `retrySmall` L1222 | 位图撞 Binder 后压到 15 万面积重推（已加"不得小于格子÷density"下限） |
| `WidgetUriPush.java` | `cachedUri` L612 / `nearestCached` L629 / `nearestAnyCanvas` L675,692 | 顶帧来源。`nearestAnyCanvas` 要求**比例 ±2%** 且（325 起）**像素不小于格子÷density** |
| 同上 | `pushCached` L903 / `countChanged` L883 | 推 URI；`countChanged` 只拦"换了素材（帧数变了）" |
| `WidgetPrewarm.java` | `bake` L312 / `maybeRepushEarly` L442 | 预热：从播放头+10 帧环形烘；每 24 帧（或 1/4）补推一次（最多 2 次）；结束再补推 |
| `ConfigActivity.java` | `finishSave` L4603 | 导出收尾：**323 起先 `invalidateSize` 再 `render(index=0)`** |

### 一次完整时序（导出完成 → 桌面可见）

```
finishSave()
  → WidgetRender.invalidateSize(id)          // 作废 3 秒尺寸缓存
  → render(index=0)
      box = widgetSize()                     // ← 这里可能仍是 launcher 的默认 110dp！
      canvas = canvasFor(box)                // 比例 = 格子比例；110dp → 1.130，真格子 1.030
      cachedUri(index) 命中？ → pushCached   // 命中就推
      否则 busy/pending → 顶旧画布             // 比例不符会退回现场合成
  → PlaybackService 每 20ms 再调 render(index++)
WidgetPrewarm.ensure() …（400ms 静置后开烘，6~7 秒烘完 420 帧）
```

## 6. 已试过的修法与结果（按版本）

| 版本 | 改法 | 结果 |
|---|---|---|
| v1.10.272 | 允许"任意比例旧画布"顶帧 | 画面收窄（已撤销） |
| v1.10.278 | 顶帧一律 centerCrop | 首帧"放大一下"（v1.10.300 改回 fitViewFor 同一判据） |
| v1.10.292 | 比例不一致就不顶帧；`nearestAnyCanvas` 加 ±2% 比例限制 | 好转，未根治 |
| v1.10.298 | A 方案：scaleType 只看布局（fitCenter 恒等不裁） | 截断问题解决；"小一圈"仍在 |
| v1.10.299 | 默认尺寸（≤112dp）按 420dp 兜底画布 | 未命中本场景（阈值在加 padding 之后判断，130>112） |
| v1.10.309 | `countChanged` 拦住"换了素材就顶旧画布" | 未根治（本例帧数没变时不拦） |
| v1.10.322 | 顶旧画布/压小重推都加"位图 ≥ 格子"下限 | **单位搞错**：按 px 而非 px÷density，严了 3.28 倍 |
| v1.10.323 | `finishSave` 前 `invalidateSize`；下限改 dp 口径 | 无效：那一刻 launcher **真的**还在报 110dp（不是缓存） |
| v1.10.324 | launcher 报默认 110dp → 这一拍不推，700ms 后补推（**只跳一次**） | 无效：跳过之后 20ms 的下一拍照旧推 426×377（日志 19:29:04 铁证） |
| **v1.10.325** | 同上，但**只要还默认就一直跳**（硬上限 5 秒），并每次记录 W28 | **待用户验证** |

## 7. 仍未解释/需要外部判断的点（按可能性排序）

1. **（已定案 v1.10.326）** 426×377 的帧 intrinsic = 1397×1237px > 格子，所以不是"卡片被量小"。
   真正机理：fitCenter 受宽度限制 → 画面高 = 377×(662/426) = **585.8px**，而格子高 643 →
   上下各留 **4.45%**，整幅画面缩到 **0.911 倍**，看起来就是"小一圈"。
   两侧本来就露壁纸，所以不会被看成"多了一圈壁纸"。
2. **1250ms 的空白期是否已被感知为"缩一下"**：325 会跳过最多 5 秒，桌面在此期间显示
   **上一帧/占位图**。若上一帧是占位图，"小一圈"会变成"app 图标闪一下"——需要用户确认哪个更糟。
3. **`appWidgetSizes=[]`（空）**：vivo 不提供真实像素尺寸，只能 dp 估算 + padding 反推。
   是否有更可靠的 API（`onAppWidgetOptionsChanged` 时机？`AppWidgetHost` 视角？）
   ——待外部给思路。
4. **（已更正）** 错帧时长**与编码速度无关**：首帧只需要第 0 帧现场合成（≈55ms），
   时长完全取决于 launcher 什么时候给真尺寸。`W27 编码均=55ms / 420 帧 6~7 秒` 影响的是
   **预热耗时**（"内容/占位拆两层"要解决的是另一件事），不能拿来解释这个"小一圈"。

## 8. 红线（改代码时不能碰）

- `fitViewFor` 只按布局判（A 方案）；不许再出现"画布比例 vs 格子比例"的第二套判据
  （门禁 [12] 会扫描 `Math.abs(ca - ba)` 这类式子）。
- 不许用 `setViewLayoutWidth`（vivo 忽略），画布下限靠 `coverFloor`。
- 不许给画布加"衬底"（v1.10.280 撤销过，透明素材会出灰雾）。
- 画布必须与格子同形；比例判据只在 `AspectMath` 一处。
- 新增诊断代号必须登记到 `DiagCatalog`，且"一个代号一种含义"（门禁 [8]）。
- 出包流程：`scripts/build-apk-src.sh <vc> <vn>` → `scripts/verify-apk.sh`（14 项门禁，bad=0/OK）
  → `sha256sum` → 拷到 `/chat/outputs/`。

## 9. 日志字段速查

```
W28 帧画布 组件=… 画布=WxH 格子=WxH 布局=… 素材=WxH     ← 这一拍用哪块画布
W22 尺寸变更 → 帧=N 画布=…(比例x) 格子=…(比例y) 画面=…(内接) 桌面摆=…px(位图×density)
W22 尺寸 options 全量 {appWidgetMinWidth=…, appWidgetSizes=[]}
F11 小部件 尺寸来源=竖屏 WxH dp×density → …px → 格子=…px 比例=…
W13 预热开始/完成/首帧命中/延后；W27 合成分解（含 编码均 / JPEG=x/y）
W30 推送失败；W32 重推（原因=预热完成 / 预热前段就绪 / 尺寸未知延后）
W10 组件 … 实测 fps / 命中% / 画布=…
```

## 10. 现有产物

- 最新 APK：`AnimWidget-v1.10.325.apk`（versionCode 440，SHA-256 `020e33c6…b987ee`）
- 三份原始日志：`/chat/attachments/Component_2618_42_11.txt`、`…_51_27.txt`、`…_29_22.txt`
- 相关源码：`/workspace/AnimWidget/src-v2/io/github/animwidget/{WidgetRender,WidgetUriPush,WidgetPrewarm,ConfigActivity}.java`
- 历史结论：`/workspace/AnimWidget/LESSONS.md` 第 87~98 条、`docs/HANDOFF-WIDGET-FIT.md`
