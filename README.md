# 帧工坊 / FrameSmith

Turn **video / GIF / images** into live animated widgets on your Android home screen.

把**视频 / 动图 / 图片**做成安卓桌面的动态组件：每个组件独立配置素材、帧率、清晰度、蒙版形状、边框与阴影，多个组件互不影响。

## Download / 下载

| Version | Size | SHA-256 |
|---|---|---|
| [v1.10.520](https://github.com/dlph-Ex/animwidget/releases/latest) | 6,836,005 bytes | `824fe9ccd11f149498313b5153f1951a267a0f92fd707464615121b28c68ec78` |

Requires Android 8.0+ (API 26) / 系统要求 Android 8.0 及以上（`minSdk 26` / `targetSdk 35`）。

也可以自己出包：见下方 [Build / 构建](#build--构建)。

## Install / 安装

1. 拿到 `.apk`（自己构建，或从构建者处获取）/ get the `.apk`
2. 允许当前浏览器或文件管理器「安装未知来源应用」/ enable "install from unknown sources"
3. **vivo / iQOO**：会提示「未备案」，点「授权本次安装」；
   若被「纯净模式」拦截，去 `设置 → 安全 → 纯净模式` 关闭。
   A warning about missing ICP filing (备案) is expected — tap **"Authorize this installation"**.

## Features / 功能

- 视频 ≤1080p / ≤50fps / ≤40s；GIF 与静态图
- **35 种蒙版形状**（方形 / 圆角 / 超椭圆 / 圆形 / 胶囊 / 菱形 / 三角 / 六边形 / 八边形 / 星形 / 心形 / 水滴 / 盾形 / 云 / 闪电 / 齿轮 / 太极 …），另支持阴影曲线、边框、整体旋转与偏移
- 7 种布局档：原始比例 / 填充 / 16:9 / 4:3 / 1:1 / 9:16 / 21:9
- 「1:1」档：预览 = 桌面 = 原清晰度 / 原帧率 / 原时长
- 桌面组件拖动改尺寸后自动适配，不裁切画面
- 素材导入即预览首帧；编辑页顶部按「整块显示区」对位显示当前壁纸

## Permissions / 权限

| Permission | Why / 用途 | Optional? |
|---|---|---|
| `FOREGROUND_SERVICE`<br>`FOREGROUND_SERVICE_SPECIAL_USE` | per-frame widget delivery service / 组件逐帧投递服务 | No / 否 |
| `POST_NOTIFICATIONS` | playback status notification / 播放状态通知 | Yes / 可拒绝 |
| `SYSTEM_ALERT_WINDOW` | only for the "adjust position" overlay / 仅「调整位置」功能 | Yes / 可拒绝 |
| `PACKAGE_USAGE_STATS` | detect whether the launcher is in foreground, to pause when hidden / 判断桌面是否前台，不在则停播省电 | Yes / 可拒绝 |
| `READ_MEDIA_IMAGES`<br>`READ_EXTERNAL_STORAGE` (≤ Android 12) | pick and preview image/video assets / 选择与预览图片、视频素材 | Yes / 可拒绝 |
| `INTERNET` | **只有**「关于」页点版本行时的更新检测：向 GitHub Releases API 发一次请求，取最新版本号比对 | 普通权限，无授权弹窗 |

**无统计 SDK、无广告、不上传任何数据。** 除上面那次「检测更新」之外，应用不会发起任何网络请求。
/ **No analytics, no ads, no data upload.** The only network request is that update check, and only when you tap it.

## Build / 构建

本工程**不使用 Gradle**，出包是一条脚本：

```bash
./scripts/build-apk-src.sh <versionCode> <versionName>   # javac → aapt2 → d8 → zip → apksig（v2 签名）
```

- 需要 JDK 17 与 `aapt2`；编译用的 `android-all.jar` / `d8.jar` / `kotlin-stdlib.jar` 等工具链放在 `toolchain/`，**不随仓库分发**；
- 产物是**自签名** APK（签名钥匙不在仓库内），所以首次安装需要允许「未知来源」；
- 静态门禁（`verify-apk.sh` 与各 `*-check`）、自检（`tests/`）与过程记录保存在维护者本地，**不随仓库分发**，仓库里只有出包必需的脚本。

源码结构（重要）：**`src-v2/` 是唯一的源码树**，资源与清单在 `patch-v1.4.0/`（目录名是历史遗留，实际就是当前这一代的 `res/` + `AndroidManifest.xml` + `stable-ids.txt`）。
`app/` 下那套 Gradle 骨架与其中的 `.kt` 文件是早期遗留，**不参与出包**，读代码时不要从那里看。

## Sponsor / 赞赏

完全自愿，不影响任何功能。

- Ko-fi：<https://ko-fi.com/frost_doplhin>
- 微信 / 支付宝：在应用内「关于 → 赞赏」里直接扫码（收款码只随应用分发，不在仓库里）

<!-- 应用内赞赏弹层用的就是 res/drawable-nodpi-v4/reward_qr.png 与 reward_alipay.png；v1.10.520 起这两张不再复制到仓库 -->

## Feedback / 反馈

Open an [issue](../../issues) with your device model, OS version, launcher name, and the diagnostic log exported from `Settings → Diagnostic log → Export`.
/ 到 Issues 提，请附手机型号、系统版本、桌面名称，以及应用内 `设置 → 诊断日志 → 导出` 生成的文件。

## License

MIT — Copyright (c) 2026 AnimWidget contributors. 见 [LICENSE](LICENSE)。
