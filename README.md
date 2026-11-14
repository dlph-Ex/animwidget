# 帧工坊 / FrameSmith

把视频、动图、图片做成安卓桌面的动态组件。每个组件独立配置素材、帧率、清晰度、蒙版形状、边框与阴影。

Turn videos, GIFs and still images into live widgets on your Android home screen. Each widget keeps its own media, frame rate, sharpness, mask shape, border and shadow.

> 本文中英对照：每节先中文，再 English；列表与表格在同一条目内用「 / 」分隔。
> Every section is Chinese first, English second; list items and table cells carry both languages, separated by " / ".

**目录 / Contents**：[功能](#功能--features) · [下载](#下载--download) · [安装](#安装--install) · [权限](#权限--permissions) · [已知限制](#已知限制--known-limitations) · [技术要点](#技术要点--how-it-works) · [构建](#构建--build) · [仓库结构](#仓库结构--repository-layout) · [反馈](#反馈--feedback) · [赞赏](#赞赏--sponsor) · [许可证](#许可证--license)

## 功能 / Features

- **素材 / Media**：MP4、GIF、APNG、动图 WebP 与静态图；导入后立刻看到首帧。视频硬件解码，按 BT.601 / 709 / 2020 转换色彩。/ MP4, GIF, APNG, animated WebP and still images; the first frame previews as soon as you import. Video is hardware-decoded with BT.601 / 709 / 2020 colour conversion.
- **单个组件可调 / Per widget**：帧率 1–50 fps、清晰度 120–2560 px（8 px 一档）、压缩质量 50–100、不透明度、等比例缩放、内容与蒙版旋转、偏移、阴影曲线、细边框。/ Frame rate 1–50 fps, sharpness 120–2560 px (8 px steps), quality 50–100, opacity, uniform scale, content and mask rotation, offset, shadow curve, thin border.
- **35 种蒙版形状 / 35 mask shapes**：方形、圆角、超椭圆、圆形、胶囊、菱形、三角、六边形、八边形、星形、心形、水滴、盾形、云、闪电、齿轮、太极等，可选择展开全部形状。/ Rect, rounded, squircle, circle, capsule, diamond, triangle, hexagon, octagon, stars, heart, teardrop, shield, cloud, bolt, gear, yin-yang and more; expand the picker to see every shape.
- **7 种布局 / 7 layouts**：原始比例、填充、16:9、4:3、1:1、9:16、21:9。/ Original ratio, fill, 16:9, 4:3, 1:1, 9:16, 21:9.
- **三档出图 / Three quality tiers**：标准约 22 万像素，走位图通道；高清长边 1440、约 130 万像素；1:1 按卡片真实像素，上限 220 万像素，桌面不再放大。/ Standard (~220k px, bitmap channel); HD (long edge 1440, ~1.3M px); 1:1 (real card pixels, capped at 2.2M px, no upscaling on the home screen).
- **拖动即适配 / Refit on resize**：在桌面改组件尺寸后，按新格子的真实像素重新出图，不裁画面。/ Resize the widget on the home screen and it re-renders at the new cell's real pixels instead of cropping.
- **播放时机 / When it plays**：桌面停留满 3 秒才开始播放；离开桌面、锁屏或息屏立即停止。/ Playback starts only after the home screen has stayed visible for three seconds, and stops as soon as you leave the home screen, lock the device, or turn the screen off.
- **外观与语言 / Look and language**：深浅色可跟随系统或手动切换，配色方案、对比度、反转、字体大小可调；界面语言可跟随系统，也可固定为中文或 English。/ Light and dark themes (follow the system or force one), colour schemes, contrast, inversion and font scale; the UI language follows the system or is pinned to Chinese or English.
- **自查 / Diagnostics**：日志只保留最近 30 分钟（含崩溃记录），可导出 TXT 或长图；「关于」页显示版本、签名证书与更新检查；主界面可一键停止全部播放。/ Logs keep the last 30 minutes (crashes included) and export as TXT or long images; the About sheet shows the version, signing certificate and an update check; the main screen can stop all playback at once.

## 下载 / Download

| 版本 / Version | 体积 / Size | SHA-256 |
|---|---|---|
| v1.10.520 | 6,836,005 bytes（约 6.5 MiB） | `824fe9ccd11f149498313b5153f1951a267a0f92fd707464615121b28c68ec78` |

- 直接下载 / Direct download：<https://github.com/dlph-Ex/animwidget/releases/download/v1.10.520/AnimWidget-v1.10.520.apk>
- 全部版本 / All releases：<https://github.com/dlph-Ex/animwidget/releases>

系统要求 / Requirements：Android 8.0 及以上（API 26），targetSdk 35。

校验下载 / Verify the download：`sha256sum AnimWidget-v1.10.520.apk`，结果应与上表一致。/ Run `sha256sum AnimWidget-v1.10.520.apk` and compare it with the table above.

发行证书 SHA-256：安装后在应用内「关于 → 签名证书」即可核对。/ Release certificate SHA-256 — check it in the app under "About → Signing cert".

`5E:9B:78:5C:93:2E:49:AD:62:1E:BE:FB:31:FB:14:66:59:2F:A9:62:88:29:55:A6:9C:EB:89:5D:04:EE:F4:D3`

## 安装 / Install

1. 下载 `AnimWidget-v1.10.520.apk`。/ Download `AnimWidget-v1.10.520.apk`.
2. 允许浏览器或文件管理器「安装未知来源应用」。/ Allow your browser or file manager to install unknown apps.
3. **vivo / iQOO**：会提示「未备案」，点「授权本次安装」；若被「纯净模式」拦截，到 `设置 → 安全 → 纯净模式` 关闭后再装。/ A missing-ICP-filing warning is expected — tap "Authorize this installation". If Pure Mode blocks it, turn Pure Mode off under `Settings → Security → Pure Mode`.
4. 安装后打开应用，点「＋ 新建组件」按提示添加到桌面。/ Open the app and tap "＋ New widget" to add it to the home screen.

升级直接覆盖安装即可，组件配置与素材都保存在应用私有目录里。/ Upgrades are plain installs over the old version; widget configs and media stay in the app's private storage.

## 权限 / Permissions

| 权限 / Permission | 用途 / Why | 可拒绝 / Optional |
|---|---|---|
| `FOREGROUND_SERVICE`<br>`FOREGROUND_SERVICE_SPECIAL_USE` | 播放期间的前台服务，逐帧投递 / foreground service that delivers frames while playing | 否 / No |
| `POST_NOTIFICATIONS` | 播放状态通知 / playback status notification | 是 / Yes |
| `SYSTEM_ALERT_WINDOW` | 仅「显示并调整位置」用的悬浮层 / overlay used only by "show and adjust position" | 是 / Yes |
| `PACKAGE_USAGE_STATS` | 判断桌面是否在前台，切走就停播 / detect whether the launcher is in front, to pause when hidden | 是 / Yes |
| `READ_MEDIA_IMAGES`<br>`READ_EXTERNAL_STORAGE`（仅 API ≤ 32 / API 32 and below） | 选择与预览素材 / pick and preview media | 是 / Yes |
| `INTERNET` | 只在「关于」页点版本行时，向 GitHub Releases API 请求一次 / one GitHub Releases API request, only when the version row in About is tapped | 普通权限 / normal permission |
| Shizuku（`moe.shizuku.manager.permission.API_V23`） | 仅「后台优化」在本地执行 shell 命令；不装 Shizuku 就完全用不到 / shell commands run locally by "background optimization"; unused unless Shizuku is installed | 是 / Yes |

无统计 SDK、无广告、不上传任何数据。帧以 WebP 存放在应用私有目录，卸载应用即全部清除。除「关于」页那一次更新检测外，应用不发起任何网络请求。
/ No analytics SDK, no ads, nothing uploaded. Frames live as WebP in the app's private storage and are erased when you uninstall. Apart from that single update check, the app makes no network requests.

## 已知限制 / Known limitations

- **动态壁纸 / Live wallpapers**：组件编辑页顶部的壁纸底图只能取到静态图；屏上是动态壁纸时，它与屏幕上的画面不是同一张。/ The wallpaper strip at the top of the editor can only use a static image; with a live wallpaper it is not the same picture as the one on screen.
- **桌面不报尺寸 / Launchers without size reports**：启动器不报 `OPTION_APPWIDGET_SIZES`（如部分 vivo 机型）时先按估算值出图，改一次尺寸就按真实像素重出。/ When a launcher does not report `OPTION_APPWIDGET_SIZES` (some vivo builds), the first render uses an estimate and switches to real pixels once the widget is resized.
- **帧率上限 / Frame-rate ceiling**：逐帧推帧要经过启动器渲染，多个高帧率组件同时播放时，实际帧率可能低于设定值。/ Frames are pushed through the launcher's rendering, so several high-frame-rate widgets together can end up below the configured rate.
- **后台限制 / Background limits**：部分 ROM 会限制后台服务，可用「后台优化（Shizuku）」或电池白名单改善。/ Some ROMs throttle background services; "background optimization (Shizuku)" or the battery whitelist helps.
- **素材上限 / Media limits**：单段素材最长 40 秒（超出截取前 40 秒）；单组件帧数上限 2400，设定帧率算出的帧数超过 2400 时会自动下调帧率。/ A clip may last 40 seconds (the first 40 s are used); a widget is capped at 2400 frames, and the frame rate is lowered automatically when the configured rate would exceed that.
- **重签会被限制 / Re-signed builds are limited**：发行包会校验自身签名，用别的证书重签后，推帧与导出会停止。/ The release build verifies its own signature; a build re-signed with another certificate stops pushing frames and exporting.

## 技术要点 / How it works

- 手写 GIF / APNG / 动图 WebP 解码，视频走 MediaCodec 硬解。/ GIF, APNG and animated WebP decoders are written from scratch; video goes through MediaCodec.
- RemoteViews 逐帧推帧，位图与文件（URI）双通道，按启动器支持情况选择。/ Frames are pushed through RemoteViews on two channels, bitmaps and files (URIs), picked according to what the launcher supports.
- 阴影曲线来自反编译复刻加真机标定，单层高斯尾，烘一次、每帧只贴一次。/ The shadow curve is a decompiled reimplementation calibrated on real devices: one Gaussian tail, baked once and pasted per frame.
- 图标与控件全部代码绘制，配色由 M3 色轮在本地推导。/ Icons and controls are drawn entirely in code; colours are derived locally from an M3 colour wheel.
- dex 里只有应用代码、Kotlin 运行时与可选的 Shizuku 客户端（`kotlin/*`、`rikka/shizuku/*`），没有 UI 框架，也没有统计或广告 SDK。/ The dex holds only the app, the Kotlin runtime and the optional Shizuku client (`kotlin/*`, `rikka/shizuku/*`) — no UI framework, no analytics or ad SDK.

## 构建 / Build

出包不使用 Gradle，一条脚本：/ The build does not use Gradle — it is one script:

```bash
./scripts/build-apk-src.sh 634 1.10.520   # javac → aapt2 → d8 → 打包 → apksig（v2 签名）
```

产物 / Output：`dist/AnimWidget-v1.10.520.apk`。

- 需要 JDK 17、`aapt2` 与 Python 3。/ Needs JDK 17, `aapt2` and Python 3.
- 工具链 jar（`android-all.jar`、`jdk-core-stubs.jar`、`r8.jar`、`apksig.jar`、`signapk-cli.jar`、`kotlin-stdlib.jar`、`annotations-13.0.jar`、`shizuku-all.jar`、`shizuku-aidl.jar`、`core-lambda-stubs.jar`）放在 `toolchain/`，不随仓库分发；可用 `TOOLCHAIN=/path` 指定。/ Toolchain jars (`android-all.jar`, `jdk-core-stubs.jar`, `r8.jar`, `apksig.jar`, `signapk-cli.jar`, `kotlin-stdlib.jar`, `annotations-13.0.jar`, `shizuku-all.jar`, `shizuku-aidl.jar`, `core-lambda-stubs.jar`) live in `toolchain/` and are not shipped; override with `TOOLCHAIN=/path`.
- 签名钥匙放在 `build-manual/debug.keystore`（别名 `androiddebugkey`，口令 `android`），不随仓库分发，自己生成即可。/ The signing key belongs in `build-manual/debug.keystore` (alias `androiddebugkey`, password `android`); it is not shipped — generate your own.
- 加 `OBFUSCATE=1` 可改走 R8 混淆（只改名，不裁剪、不优化）。/ Prefix with `OBFUSCATE=1` to build through R8 (renaming only, no shrinking or optimising).
- 根目录的 Gradle 文件只供 IDE 打开工程，出包不用它们。/ The Gradle files at the root only serve IDE import; the build does not use them.
- 内置双语字体**不随仓库分发**：`patch-v1.4.0/res/font/anim_dual*` 缺失时也能出包，界面回退系统字体；想还原原字形就把这三个文件放回 `res/font/`。/ The built-in bilingual font is **not shipped**: without `patch-v1.4.0/res/font/anim_dual*` the build still works and the UI falls back to the system font; drop those three files back into `res/font/` to get the original glyphs.
- 自己出的包签名与发行证书不同，不能覆盖安装官方包，需要先卸载（配置与素材会清空）。/ A self-built APK carries a different signature, so it cannot install over the official one — uninstall first, which clears configs and media.
- 自建包还会**撞上签名自检**：`Integrity` 只认发行证书，换钥匙即判为 `BAD_CERT`，推帧与导出会被停用。要让自建包正常工作，得把 `src-v2/io/github/animwidget/Integrity.java` 里的 `W` 常量换成你自己的证书 SHA-256（8 个 int）。/ A self-built APK also trips the integrity check: only the release certificate is accepted, and a `BAD_CERT` build stops pushing frames and exporting. To make a self-built APK fully work, replace the `W` constant in `src-v2/io/github/animwidget/Integrity.java` with your own certificate's SHA-256 (eight ints).

## 仓库结构 / Repository layout

```
src-v2/         唯一源码树（Java，io.github.animwidget）/ the only source tree
patch-v1.4.0/   资源与清单：res/、AndroidManifest.xml、stable-ids.txt / resources and manifest
scripts/        出包脚本：build-apk-src.sh、pack_apk.py、sync-r.py、r8-rules.pro / build scripts
app/            早期 Gradle 骨架，不参与出包 / legacy Gradle skeleton, not used by the build
```

- 读代码从 `src-v2/` 开始；`patch-v1.4.0/` 的目录名是历史遗留，实际就是当前这一代的资源与清单，里面除 `res/`、`AndroidManifest.xml`、`stable-ids.txt` 之外的旧工具不参与出包。/ Start reading in `src-v2/`. The `patch-v1.4.0/` name is historical — it is today's resources and manifest, and the older tooling inside it is unused.
- 静态门禁、自检脚本与过程记录只保留在维护者本地，不随仓库分发；仓库里只有 4 个脚本。/ Static gates, self-tests and working notes stay with the maintainer; the repository carries only four scripts.

## 反馈 / Feedback

请到 [Issues](https://github.com/dlph-Ex/animwidget/issues) 提，附上：机型与系统版本、启动器名称、复现步骤，以及应用内「设置 → 日志 → 导出日志」生成的文件。
/ Open an issue with your device model, OS version, launcher name, reproduction steps and the log exported from `Settings → Logs → Export log`.

## 赞赏 / Sponsor

完全自愿，不影响任何功能。Ko-fi：<https://ko-fi.com/frost_doplhin>。微信 / 支付宝：应用内「设置 → 开发者 → 赞赏」直接扫码。
/ Entirely optional. Ko-fi: <https://ko-fi.com/frost_doplhin>. WeChat / Alipay: scan inside the app under `Settings → Developer → Donate`.

## 许可证 / License

MIT。Copyright (c) 2026 AnimWidget contributors。全文见 [LICENSE](LICENSE)。
/ MIT. Copyright (c) 2026 AnimWidget contributors. Full text in [LICENSE](LICENSE).
