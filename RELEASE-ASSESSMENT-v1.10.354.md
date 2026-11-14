# 帧工坊 v1.10.354 上架可行性评估

> 评估对象：`dist/AnimWidget-v1.10.354.apk` / versionCode 469 / 6,614,600 字节
> 评估口径：能不能过审 + 过了之后会不会塌。不含主观喜好。

---

## 结论先行

**技术上可以上架，但现在上架是拿一台设备的数据去赌一百种 ROM。**

分市场：

| 目标 | 结论 | 主要卡点 |
|---|---|---|
| 酷安 / 个人分发 | **可以上** | 无硬门槛，标注"实验性"即可 |
| 国内商店（vivo/小米/华为/OPPO） | **收窄权限后可以** | 多余权限、隐私政策、耗电说明 |
| Google Play | **现在不行** | 4 个敏感权限同时出现，几乎必然人工审核驳回 |

---

## 一、硬指标（客观，已核实）

| 项 | 状态 | 备注 |
|---|---|---|
| package | `io.github.animwidget` | |
| minSdk / targetSdk | 26 / **35** | Play 2025 要求 35，已满足 |
| 64 位 | 纯 Java，d8 出包 | 无 native lib，天然支持 |
| 体积 | 6.6 MB | 正常 |
| 第三方依赖 | 0（除 Shizuku SDK） | 加分项 |
| 签名 | 自签 debug keystore | **上架必须换正式签名** |
| 崩溃收集 | 本地 `CrashLog`，**无上报** | 上架后等于瞎 |
| 功能完整度 | 视频/GIF/图片、48 种形状、阴影/边框、1:1、50fps、导出 | 完整 |
| 门禁 | [1]~[14] 全过，bad=0，206 条断言 | 强 |
| 真机稳态 | `拍=150 命中新=150 跳过=0 帧隔 p50=20ms` | 干净 |

---

## 二、合规：有四条硬伤，其中三条是"白送的"

### 1. 权限：**本条初版结论是错的，已更正**

初版写「`SYSTEM_ALERT_WINDOW` / `PACKAGE_USAGE_STATS` 零引用，可删」。**核实 API 调用后，两条都在用：**

| 权限 | 实际用途 | 依据 |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | 悬浮窗拖动调整位置 | `OverlayService`：`WindowManager.addView(root, params)`，`type=2038` |
| `PACKAGE_USAGE_STATS` | 判断桌面是否在前台 | `DeskWatcher`：`UsageStatsManager.queryEvents()` |
| `READ_EXTERNAL_STORAGE` | Android 12 及以下浏览相册 | `maxSdkVersion="32"`，运行时请求 |

误判原因：权限名和 API 名是两套名字。代码里出现的是 `canDrawOverlays()`、`2038`、
`UsageStatsManager`，不会出现权限名字符串。**按字符串 grep 权限，等于没查。**

**v1.10.355 真正删掉的只有一条**：`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`（Play 品类白名单限制）。
改为跳 `APP_BATTERY_SETTINGS` / `IGNORE_BATTERY_OPTIMIZATION_SETTINGS`，两者都不需要权限。

剩余三条的正确处理方式是「按需请求 + 用途说明」，而不是删：
- `SYSTEM_ALERT_WINDOW`：只在用户点「调整位置」时引导开启，不主动弹
- `PACKAGE_USAGE_STATS`：属于特殊权限，需在隐私政策里写明「仅用于判断桌面是否可见」
- `READ_EXTERNAL_STORAGE`：已被 `maxSdkVersion="32"` 框住，Play 不会因此警示

### 2. Shizuku 是最大变量

```
moe.shizuku.manager.permission.API_V23
```

用途必须能一句话说清，且必须存在**不用 Shizuku 也能完整使用**的路径。
Google Play 对"ADB 级权限"的应用会人工审核，通常要求：功能说明视频 + 降级路径 + 不强制。

### 3. REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 大概率被拒

Play 只允许特定品类（VoIP、系统工具、自动化）直接请求。桌面组件播动画不在列表里。
**建议改成引导用户手动去系统设置里加白名单，不直接用 REQUEST 权限。**

### 4. FOREGROUND_SERVICE_SPECIAL_USE

必须提交用途说明（Play Console 的 Declarations 表单）。写法：
"桌面小组件动画的逐帧投递服务，仅在桌面可见时运行，用户可随时停止。"
这是能过的，但要走流程。

### 5. 还缺

- **隐私政策 URL**（必须；你读写相册）
- **内容分级**（用户可导入任意视频 → 需要年龄分级 + 用户内容免责）
- **正式签名证书**（现在是自签 debug）

---

## 三、工程风险：比合规更致命

### 1. 全部结论来自一台设备

vivo V2426A / Android 16 / `com.bbk.launcher2`。

而整个 50fps 方案建立在**四条未文档化的 launcher 行为**上：

1. launcher 用 `AT_MOST` 量 RemoteViews（不是 EXACTLY）
2. `ImageView.onMeasure` 取 `min(drawable.intrinsic, specSize)` → 位图必须 ≥ 格子
3. `setImageViewBitmap` + `asShared` 走 fd，不撞 Binder 1MB
4. 每 20ms 一次 `updateAppWidget` 不被限流/合并

**这四条任何一条在别的 ROM 上不成立，功能就塌。** 而你只有一个样本。
这不是"可能有问题"，是"一定有问题，只是不知道在哪一条"。

### 2. 50fps 常驻 = 高耗电

稳态实测 2.6~3 核持续解码。商店会给"耗电"标签，用户会投诉。
已有 `W11 进入受限状态`（桌面不可见时停推），这是对的，但需要：
- 明确的省电档（30fps / 静止时冻结）
- 设置页写清楚"高帧率会更耗电"

### 3. 没有远程崩溃统计

`CrashLog` 只存在用户手机上。上架后一个崩溃跑一万次，你也不会知道。
Play Console 有 ANR/崩溃面板，国内商店大多没有 → 要么自建上报，要么接受"瞎飞"。

### 4. 静态门禁覆盖不到真机行为

351 → 352 → 353 → 354 四版才把过渡期压下去。每一版的缺陷都是：
**编译通过、206 条断言通过、`verify-apk` OK、真机不过。**

这说明验证闭环缺一环。上架后你没有"我这边跑一遍"的机会。

### 5. `dist/` 里 400+ 个 APK（约 2.4 GB）

说明迭代节奏是"一天几十版"，还没收敛到"可发布版本"。这本身不是错误，
但上架意味着要冻结一个版本、打 tag、写 release notes —— 现在的节奏还没到那一步。

---

## 四、上架前必做（按性价比排序）

| # | 事项 | 工作量 | 收益 |
|---|---|---|---|
| 1 | 删掉 3 个无用权限 | 10 分钟 | **最高**（直接减少审核风险） |
| 2 | 3 台不同 ROM 摸底（小米/华为/OPPO），只测「能不能播 + 拖动后会不会崩」 | 1 天 | **最高**（决定要不要上） |
| 3 | 降级策略：launcher 不配合时退到 30fps / URI 通道并提示用户 | 1~2 天 | 高 |
| 4 | 隐私政策 + 权限说明页 | 半天 | 必须 |
| 5 | 换正式签名 + 冻结版本 + release notes | 半天 | 必须 |
| 6 | `dist/` 清理，只留最近 3 个 | 5 分钟 | 工程卫生 |

---

## 五、一句话判断

**这是一个「在一台手机上打磨到极致」的作品，还不是「一款可以上架的产品」。**

差距不在功能——功能是完整的，稳态数据是漂亮的。
差距在**验证面**：所有精妙设计都建立在 vivo launcher 的四条未文档化行为上，而你只有一个样本。

- 想稳：先上酷安，让真实用户帮你做兼容性摸底，出问题能改。
- 想上主流商店：先补第 1、2 项，再谈别的。
- Google Play：删掉 `SYSTEM_ALERT_WINDOW` / `PACKAGE_USAGE_STATS` / `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
  三个权限、Shizuku 降为可选功能，否则四敏感权限同框，必被人工驳回。

---

## 附：评估时核实过的原始事实

- `patch-v1.4.0/AndroidManifest.xml` 权限清单（9 条）
- `SYSTEM_ALERT_WINDOW` / `PACKAGE_USAGE_STATS` 在 `src-v2/` 全仓库零引用
- 导入走 `Intent.ACTION_OPEN_DOCUMENT`（SAF），不依赖 `READ_MEDIA_VIDEO`
- `CrashLog.install(...)` 在 OverlayService / PlaybackService / WidgetProvider 均已挂载（本地）
- `build-apk-src.sh` 传 `--target-sdk-version 35 --min-sdk-version 26`
- `verify-apk.sh`：组件 14 / 缺失 0 / dex 类 1678→1678 / checked=323 bad=0
- 最新稳态日志：`拍=150 命中新=150 现渲染=0 邻帧=0 跳过=0 帧隔 p50=20ms`
