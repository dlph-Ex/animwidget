# v1.3.4：主界面/设置页不跟随的真正原因

## 定位

`ThemeManager.isDark()` 在「跟随系统」时写的是：

```kotlin
val systemNight = (Resources.getSystem().configuration.uiMode and 48) == 32
val contextNight = (context.resources.configuration.uiMode and 48) == 32
return systemNight || contextNight          // ← 问题在这里
```

而三个 Activity 的 `attachBaseContext` 都被 `LocaleHelper.wrap()` 包过：

```kotlin
override fun attachBaseContext(newBase: Context) =
    super.attachBaseContext(LocaleHelper.wrap(newBase))   // createConfigurationContext(...)
```

`createConfigurationContext` 出来的 Resources 带着**创建那一刻的 Configuration 快照**。
在部分 ROM / Android 15 上，系统配置变更只会更新 Activity 自己那份 Resources，
这份被包裹的快照可能停在旧 uiMode。于是：

- 主界面是**很早以前**创建的（当时系统还是深色）→ `contextNight` 永远是 true；
- `||` 一运算 → `isDark()` 永远返回 true → 无论刷新多少次都还是深色；
- 组件配置页是**切完主题之后才打开**的，快照是新的 → 显示正常。

这正好对应「组件编辑页没问题了，但主应用的界面与设置页仍有问题」。

## 改法

| # | 文件 | 改动 |
|---|---|---|
| 1 | `ThemeManager.isDark` | 「跟随系统」改成**只认系统资源** `Resources.getSystem().configuration.uiMode`；只有当系统值为 `UI_MODE_NIGHT_UNDEFINED`（两个位都是 0）时才回退到 Activity 的 Configuration。彻底去掉「或」运算带来的陈旧锁死 |
| 2 | `ThemeManager.applyTo` | 内容页底色现在由 `android.R.id.content` **和它的第一层子视图**同时上色（v1.3.3 那次替换其实没生效，文件里还是旧写法，这次是确认改到的）；布局里写死的背景不会再挡住主题色 |
| 3 | `ThemeManager.settle()`（新增） | 换肤后遍历整棵树 `jumpDrawablesToCurrentState()` + `invalidate()`：结束残留的 ripple/pressed 动画，并强制重绘。录屏 6.0s 处设置页出现过半透明白色圆形鬼影，7.3s 才消失，就是残留 ripple 没被清理 |
| 4 | `ThemeManager.applySystemBars` | decor 铺主题底色 + `invalidate()`，并在下一帧再补一次 `invalidate()`（部分 ROM 改 window 背景后不立刻重绘） |

v1.3.3 的其它改动全部保留：`configChanges=uiMode`（不重建）、配置页根布局背景透明、设置页 5 处 `recreate()` 改原地换肤、`FrameComposer` 阴影等比留白。

## 构建 / 校验

```bash
ANDROID_SDK=/tmp/sdk KOTLIN_LIBS=/tmp/kl SHIZUKU_JAR=/tmp/shizuku-all.jar \
  ./patch-v1.3.4/build.sh 67 1.3.4
```

- `CheckClassAdapter`（SimpleVerifier + android.jar/stdlib 全 classpath）验证 `ThemeManager` / `SettingsActivity` / `ThemePatch` → 全部 OK
- APK 回读：`versionCode=67`、`configChanges=0x200`、`insetPreservingRatio` / `inPlaceRefresh` / `settle` 均在 dex 中
- 签名证书 SHA-256 `5e9b78…f4d3`（可直接覆盖安装）

## 验证要点

1. 主界面停着不动 → 下拉控制中心切深色/浅色 → 回到主界面：**应当立刻是新的颜色**（不需要滚动、不需要重进）。
2. 设置页同上；再在设置页里点「深色模式」的三个选项：页面不重建、不跳顶、语言卡不收起。
3. 组件配置页照旧验证一遍（回归）。
