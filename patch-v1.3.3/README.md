# v1.3.3：切深浅色不再重建页面（原地换肤）

承接 v1.3.2。v1.3.2 为了「一定能跟手」把三个 Activity 的 `configChanges` 清空，代价是系统切深浅色时页面重建
→ 未保存的素材/滑块/滚动位置/展开状态全没了（你录屏里就是从「外观」页被打回「内容」页顶部、已选素材也丢了）。

v1.3.3 换回**原地重新着色**，不重建任何页面。

## 改了什么

| # | 位置 | 改动 |
|---|---|---|
| 1 | `AndroidManifest.xml` | `configChanges` **恢复为 `uiMode(0x200)`**（不再置 0），系统切深浅色时只回调 `onConfigurationChanged`，Activity 不重建 |
| 2 | `res/layout/activity_config.xml` | 根布局写死的 `#fff7f8fa` 改透明（保留）。这是 v1.3.x 的回归：v1.2.8 的 `ThemeManager.applyTo` 会把第一个子 View 的背景刷成主题色，v1.3.x 去掉了这段，于是配置页永远是浅色底 |
| 3 | `ThemeManager.applyTo` | 当传入的 root 是 `android.R.id.content` 时，把它的背景刷成主题背景色（等价于 v1.2.8 那套做法，但不覆盖 preview 区域） |
| 4 | `ThemeManager.applySystemBars` | 额外设置 `decor.setBackgroundColor(主题背景)` + `invalidate()`，避免窗口背景改了但没重绘 |
| 5 | `ThemeManager.dialogTheme` | 用 `android.R.style.Theme_Material[_Light]_Dialog_Alert` 常量（反编译残留的非法写法修掉） |
| 6 | `SettingsActivity`（ASM 字节码补丁） | 5 处 `recreate()` 改成 `ThemePatch.inPlaceRefresh(this)`：切深色模式、选预设、重置主题、改自定义颜色都不再重建设置页 → 滚动位置、语言卡片展开状态、滚动隐藏的关闭胶囊全部保留 |
| 7 | 新增 `ThemePatch` | 反射调用 `SettingsActivity.refreshThemeInPlace()`，调用后同步 `appliedDark`，避免 500ms 轮询再触发一次重绘 |

语言切换（`onCreate$lambda$2`）与 `onResume` 里的 `needsRecreate()` **仍然保留 recreate**——换语言必须重新 inflate。

保留 v1.3.2 的 `FrameComposer` 阴影等比留白修复。

## 为什么这次能跟手

- 系统切深浅色：`configChanges=uiMode` → `onConfigurationChanged` → `refreshThemeInPlace()`（配置页还会重刷
  形状网格/布局 chip/液态选择器/标签色）；即使某 ROM 不回调，三个页面各自 500ms 的 `themePoll` 也会用
  `Resources.getSystem()` 的 uiMode 兜底触发同一条路径；
- 页面底色：`applyTo(content)` 直接刷 `android.R.id.content` 背景 + `applySystemBars` 刷 decor/window，
  配置页根布局不再有写死浅色挡住；
- 状态保持：全程只调 `setTextColor / setBackground / applyColors`，不 `removeAllViews` 除预设与颜色点这类
  可重建的小组件，不 `recreate`，所以滚动位置、当前标签页、菜单折叠状态、已选素材、滑块数值都不动。

## 构建

```bash
ANDROID_SDK=/tmp/sdk KOTLIN_LIBS=/tmp/kl SHIZUKU_JAR=/tmp/shizuku-all.jar \
  ./patch-v1.3.3/build.sh 66 1.3.3
```

依赖：JDK 17、android.jar、d8.jar、apksigner.jar、kotlin-stdlib、annotations、shizuku-all，
外加 `vendor/asm.jar`（ASM 9.7.1，只用于改 SettingsActivity 的 4 个方法体）。

产物：`dist/AnimWidget-v1.3.3.apk`（versionCode 66 / 1.3.3）。

## 校验

- `javac` 编译通过；`CheckClassAdapter`（SimpleVerifier + android.jar/stdlib 全 classpath）
  对 `SettingsActivity`、`ThemeManager`、`ThemePatch`、`FrameComposer` 全部通过 → 字节码栈帧/类型合法；
- 成品 APK 回读：`configChanges=0x200`、`versionCode=66`、配置页根背景 `0x0`、dex 内含 `ThemePatch/inPlaceRefresh/insetPreservingRatio`；
- 签名证书 SHA-256 `5e9b78…f4d3`，与历史版本一致，可直接覆盖安装。
