# v1.3.2 补丁构建说明（二进制级补丁 + 单类重编译）

## 背景

沙箱里 **v1.1.1 ~ v1.3.1 的源码全部丢失**，只剩：

- `app/src/main/java/...`（**v1.1.0** 的 Kotlin 源码，不含深色主题 / 多语言 / 手势 / 选择器 / R8）
- `recovered/` —— 从 v1.3.1（versionCode 64）**字节码反编译**出来的产物：
  - `classes-v1.3.1.jar`：R8 之前的原始 class 包（可再次 D8/R8，未混淆）
  - `res-v1.3.1.apk`：原样资源包（resources.arsc + 编译后的 XML，ID 与 classes 中的 R 完全对应）
  - `res-src-v1.3.1/`：apktool 反编译出的资源源码（仅供阅读/参考）
  - `src-v1.3.1/`：jadx 反编译出的 Java（**未修完，约 460 处编译错误**，见 RECOVERY.md）
  - `mapping-v1.3.1.txt`：R8 映射表

因此 v1.3.2 **不是**用仓库里的 Gradle 工程构建的，而是：

> 用 v1.3.1 的 `classes-v1.3.1.jar` + `res-v1.3.1.apk` 做底，重编译修改过的 `FrameComposer.java`，
> 再用二进制方式改 `AndroidManifest.xml` / `layout/activity_config.xml` 里的几个值，最后 D8 + 对齐 + 原签名。

## v1.3.2 改了什么

| 位置 | 改动 | 目的 |
|---|---|---|
| `FrameComposer.drawFrame()` | 阴影留白从「四边各内缩 margin 像素」改成 **`insetPreservingRatio()`：按 area 宽高比等比内缩** | 修「开阴影 + 原始比例布局时蒙版 ≠ 画面比例，画面在蒙版内出现留边」 |
| `AndroidManifest.xml` 三个 Activity | `android:configChanges` 由 `uiMode(0x200)` 改为 `0` | 系统深浅色切换时 Activity 真正重建 → 整棵树按新主题重新 inflate/着色，不再依赖 in-place 重绘 |
| `res/layout/activity_config.xml` 根 LinearLayout | `android:background` 由 `#fff7f8fa`（写死浅色）改为透明 | 配置页背景交给 window background（`applySystemBars` 会随主题刷新），这是深色模式在配置页不变暗的直接原因 |

其余参数（布局 / 形状 / 旋转 / 等比缩放 / 手势偏移 / 边框 / 不透明度 / 帧率 / 清晰度）代码路径未改。

## 如何重新构建

```bash
# 依赖（见 scripts/build-apk.sh 顶部说明）
ANDROID_SDK=/tmp/sdk KOTLIN_LIBS=/tmp/kl SHIZUKU_JAR=/tmp/shizuku-all.jar \
  ./patch-v1.3.2/build.sh
```

脚本做的事：

1. `javac` 编译 `patch-v1.3.2/FrameComposer.java`（classpath = `recovered/classes-v1.3.1.jar` + kotlin-stdlib + annotations）；
2. 用重编译出的 3 个 class 替换 `classes-v1.3.1.jar` 中对应的 `FrameComposer*`；
3. `python3 patch-v1.3.2/patch_axml.py` 打资源补丁（改 versionCode/versionName/configChanges/背景色），产出 `res_patched.apk`；
4. `d8` 出 dex（**未做 R8 压缩**，见下）；
5. `scripts/pack_apk.py` 对齐打包 → `apksigner` 用 `build-manual/debug.keystore` 签名。

## 已知取舍

- **APK 变大**：这一版没跑 R8（体积 2.5 MB vs 382 KB）。原工程的 keep 规则随源码一起丢失，
  盲跑 R8 有把 XML 里反射引用的 `MediaSelectorView` / `ShapeChipView` / `AspectRatioFrameLayout` /
  `ContentGestureView` / `HsvPickerView` 改名的风险（会直接 inflate 崩溃），所以先出**未压缩包**保功能。
- **Activity 会 recreate**：系统深浅色切换时配置页会重建，未保存的临时选择会回到已保存的配置。
  这是刻意换来的「一定会跟手」——in-place 重绘路线在真机上已经失败过三次。
- `FrameComposer$compose$1` 被 `FrameComposer$AnonymousClass1` 取代（等价），旧 class 已从 jar 中删除。
