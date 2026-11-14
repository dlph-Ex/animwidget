# 问题单：GIF 140 帧只抽出 94 帧（预览与桌面同时少 46 帧）

## 1. 现象（用户实测）

素材：`V重装图A2.gif`，界面显示 `分辨率 256x256 · 140 帧 · 原帧率 30.0fps（每帧≈33ms）· 时长 0:04.66`

实测（日志原文，`Component_2601_25_06.txt` / `Component_2601_29_08.txt`）：

```
01:24:12 D L07 预览播放启动: 94 张, fps=30, 间隔=33ms
01:24:12 D L35 预览时长自检 94 张 × 33ms = 3102ms，素材=3133ms（差 -31ms） ✓
01:24:15 D P20 预览实测 30.3fps（目标 30）/91 帧/3003ms
```

- 140 帧 → **只有 94 帧**（少 46 帧，32.9%）
- 用户肉眼：4.66 秒的素材画面 3 秒就跑完（140/94 = 1.49×）
- **桌面组件用的是同一批帧，所以同样少 46 帧**

## 2. 已做/已排除（不要再查这些）

| 项 | 结论 |
|---|---|
| `MediaImporter.MAX_FRAMES = 1200` | 不是上限（94 ≪ 1200） |
| 40 秒截断 | 只对**视频**生效；动图靠帧数上限（`ConfigActivity:3674` 注释） |
| 帧去重 | 全仓 grep 无去重逻辑 |
| 预览时长自证 | `L35` 用的是 `result.getDurationMs()`（=帧数×间隔），自己算自己，永远打勾 → 不能用它判对错 |
| v1.10.303 已修 | 预览间隔改用 `sourceDurationMs / frames`（速度对了），但**帧数仍然是 94** |

## 3. 要分析的两段

### 3.1 `GifFrames.java` —— GIF 帧边界与时延累加
- 是否把「图像描述符」全部解析成帧（含只有局部区域的子帧）？
- `delay` 累加是否可能少算（例如 delay=0 的帧被合并/跳过）？
- 是否受 `MAX_FRAMES` 之外的长度/内存保护影响？

### 3.2 `PreviewFrames.java` —— 采样循环
```java
int frameCap = limit > 0 ? limit : Math.min(40 * safeFps / 10, MediaImporter.MAX_FRAMES);  // :118
```
- `safeFps` 的量纲（是 fps 还是 fps×10）？若按 fps×10 算，`40*300/10=1200` → 不设限；若按 fps 算，`40*30/10=120` → 上限 120，仍不是 94。
- 采样是按「目标 fps 的固定步长在时间轴上取整」还是按「每个源帧的时延推进」？前者在 GIF 时延不均时会**跳过帧**。
- `FrameTimeline.frameCount(durationMs, fps)` 与实际产出的关系。

## 4. 期望的正确行为（用户硬性要求）

- 帧数：**不得丢帧**（140 帧素材 → 140 帧帧表），除非用户把 fps 滑块调到更低（此时按目标 fps 重采样，且总时长不变）。
- 播放速度 / 总时长：与素材一致，误差 ≤ 0.01（用户要求"到小数点后 2 位"）。
- 桌面组件与预览必须用**同一份帧表**。

## 5. 交付与验收

- 改 `GifFrames.java` / `PreviewFrames.java`（必要时 `FrameTimeline.java`、`MediaImporter.java`）中相关方法，给完整文件或 unified diff。
- 不得新增第三方依赖；必须通过 `scripts/verify-apk.sh` 的 12 项门禁（[8] 判断代号体检会拒新增 diag 文本，[12] 会扫源码）。
- 验收日志：`L07 预览播放启动: 140 张 …` + `L35 140 张 × 33ms = 4660ms，素材=4660ms（差 0ms）✓` + `W13 预热开始 帧数=140`。

## 6. 相关文件（随本文件一起提供）

`GifFrames.java`、`PreviewFrames.java`、`FrameTimeline.java`、`MediaImporter.java`、`Gif.java`、`FrameUtil.java`
