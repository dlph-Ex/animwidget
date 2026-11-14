# B 方案：把反编译源码修到可编译（进度）

目标：`src-v2/` 用 `javac` 全绿 → 之后给 `WidgetConfig` 加字段（蒙版偏移）就不会撞
`copy$default` 描述符，`OverlayService` / `WidgetRender` 也能一起改。

## 当前状态（第 8 轮进行中）

- `src-v2/` = jadx（`--no-inline-kotlin-lambda`）反编译 v1.3.1 的 33 个类 + `R.java`
- 已修：`ConfigActivity.displayName(Uri)`（jadx 生成 `?? r11` 坏代码，按语义重写）
- 第 2 轮已清：`PlaybackService`（9）、`MediaOverlayView`（9）、`WidgetStore`（21）→ 0；
  另外 9 个文件里 `WhenMappings` 内部类的 `static`/重复修饰符问题、`Result.constructor-impl`、
  `@Metadata` 里裸露的 `WidgetStore.KEY_GENERATION` 全部修掉（这些会*级联*出大量假错误）。
- 第 3 轮又做了 4 类**机械性**批处理（占错误大头）：
  1. 补 60+ 个常用类的 import（`Unit`/`ArrayList`/`Options`/`InputStream`/`MediaMetadataRetriever`/`MediaExtractor`…）；
  2. 匿名类被当类型：`XxxActivity$themePoll$1` → `Runnable`、`FrameCache$cache$1` → `LruCache<String,Bitmap>`；
  3. `this.this$0` → `XxxActivity.this`（jadx 合成字段在 Java 源码里不存在）；
  4. `$lambda$N(r1, …)` → `$lambda$N(XxxActivity.this, …)`（捕获实例丢失）。
- 编译结果：**227 处错误**（起始 463，已降 51%），按文件：

| 文件 | 错误数 |
|---|---|
| ConfigActivity | 126 |
| SettingsActivity | 39 |
| MainActivity | 16 |
| OverlayService | 15 |
| ConfigActivity 两个内部类 | 约 20 |
| 其余 | 约 49 |

> 注意：修完语法错误后 javac 才会做语义分析，所以错误数会「先降后跳」——9 个语法错清零后
> 从 9 跳回 423 属于正常现象，看的是**真实语义错误**这条曲线（463 → 423）。

错误类型主要是三类（都不是业务逻辑问题）：

1. **捕获变量名丢失**：`r1 / r2 / this$0` 出现在 lambda 里（jadx 内联 Kotlin lambda 的残留），
   需要按「被调方法的形参表 + 外层作用域」逐个推断；
2. **匿名类被当成类型**：`XxxActivity$themePoll$1` / `$screenReceiver$1`，改回 `Runnable` / `BroadcastReceiver` / `LruCache`；
3. **缺失 import / 静态上下文**：`Unit`、`InputStream`、`MediaExtractor`、`Window`… 以及
   `this` 用在静态方法里。

另外 4 个方法 + 3 个方法被 jadx 跳过（类型推断 NPE），必须从 Vineflower 输出移植：
`ConfigActivity.applyKindUi / save / openContentGestureEditor / renderPreviewNow$lambda$86`、
`MediaImporter.importVideo`、`PreviewFrames.load`、`SourceFps.video`。

## 分轮计划

- **第 2 轮**：先修小文件（PlaybackService / MediaOverlayView / WidgetStore / MainActivity / PreviewFrames / SourceFps / MediaImporter），
  把小文件清零（约 120 处）。
- **第 3 轮**：SettingsActivity（66）+ OverlayService（42）。
- **第 4 轮**：ConfigActivity（186）+ 移植那 7 个被跳过的方法。
- **第 5 轮**：全绿后接 `scripts/build-src.sh` 进打包流程（不再依赖 `recovered/classes.jar`），
  然后加 `WidgetConfig.maskOffsetX/Y` + 蒙版手势对话框 + `FrameComposer` 重载 + `WidgetRender`/`OverlayService` 传递。

## 工具

```bash
ANDROID_SDK=/tmp/sdk KOTLIN_LIBS=/tmp/kl ./scripts/build-src.sh
```


## 后遗症：9 处「Method not decompiled」桩方法（v1.6.3 修完）

jadx 对少数方法只留了 `throw new UnsupportedOperationException("Method not decompiled: ...")`，
编译期完全看不出来，真机上一调用就崩。v1.6.2 的崩溃日志抓到了第一处
（`ConfigActivity.updateMediaVisibility()` → 配置页一启动就崩 → 启动器判定配置失败 →
组件被丢弃 → 表现为「添加不了组件」）。v1.6.3 把 9 处全部按 jadx 留下的伪代码补回：

| 方法 | 原来会崩在 |
|---|---|
| `ConfigActivity.updateMediaVisibility()` | 进配置页（`applyKindUi`） |
| `ConfigActivity.updateEmptyHint()` | 预览空态提示刷新 |
| `ConfigActivity.ensureWallpaperPermission()` | 点选素材前的权限申请 |
| `ConfigActivity.scheduleRefillFrames()` | 选完素材后补抽帧 |
| `ConfigActivity.buildShapeGrid$lambda…39()` | 点任意形状格 |
| `ConfigActivity.openContentGestureEditor$lambda$61()` | 关闭内容手势对话框 |
| `ConfigActivity.AnonymousClass20.onStopTrackingTouch()` | 预览进度条松手 |
| `SettingsActivity.AnonymousClass5.afterTextChanged()` | 设置页颜色十六进制输入 |
| `ShizukuHelper.hasPermission()` | 所有 Shizuku 按钮 |

**教训**：编译 0 error 不等于能跑。以后每轮至少要 grep
`Method not decompiled` + `UnsupportedOperationException`，并在真机上把
每个页面点一遍（现在 `CrashLog` 会把崩因写进诊断区，返工成本低很多）。

---

## 工具链固化（v1.8.10）

`/tmp` 反复被清空导致 SDK/kotlin/shizuku jar 丢失。v1.8.10 起全部固化到
`toolchain/`（工作区内，随项目存活）：

| 文件 | 来源 |
|---|---|
| `android-all.jar` | robolectric android-all 14（API 34 完整框架，含 res/arsc；javac/aapt2 -I/d8 --lib 通用） |
| `jdk-core-stubs.jar` | JDK17 `java.base` 抽出的 java/javax 存根（android-all 刻意不含 java.**） |
| `core-lambda-stubs.jar` | JDK17 `java.lang.invoke` 存根（javac bootclasspath 用） |
| `r8.jar` | com.android.tools:r8:8.1.56（含 D8；dl.google.com 被墙，走 maven.aliyun.com/repository/google） |
| `apksig.jar` + `signapk-cli.jar` | apksig 8.3.2 库 + 自写 SignApk（sign/verify，打印证书 SHA-256） |
| `kotlin-stdlib.jar` | kotlin-stdlib 1.9.21（jadx_deps 复用） |
| `annotations-13.0.jar` | jadx_deps 复用 |
| `shizuku-all.jar` | **dev.rikka.shizuku:api:12.2.0 + provider:12.2.0 合并**（13.x 把 `Shizuku.newProcess` 私有化了） |

构建：`./scripts/build-apk-src.sh <versionCode> <versionName>`（不再需要任何 /tmp 路径）。
注意：ASM CheckClassAdapter 校验必须把 android-all/kotlin/shizuku 放 classpath，
否则全部误报 ClassNotFoundException。资源 ID 稳定集实为 **372 条**（早前摘要写的 374 是误记）。

> **血泪教训（v1.8.10/1.8.11 启动秒崩）**：shizuku `api` 构件**不含**
> `rikka.shizuku.ShizukuProvider`（在单独的 `provider` 构件里）。Manifest 注册了
> 这个 provider，类不在 dex 里 → 进程一启动就 ClassNotFoundException，
> 应用图标都点不开。教训：换三方 jar 后必须 diff 新旧 APK 的类清单
> （`jadx --no-res` 解两个包，`find | diff`），尤其 Manifest 里注册的组件类。
>
> **完整根因（v1.8.13 结案）**：缺的其实是一整套 —— Provider + `moe.shizuku.server.*`
> AIDL 存根 + `ShizukuApiConstants` + `shared/BuildConfig`。手机上 Shizuku 存活时，
> 进程一起来 Manager 就通过 Provider 推 binder，客户端 `Shizuku` 类处理 binder 要加载
> `IShizukuService.Stub` → NoClassDefFoundError → 任何入口（图标/挂件/广播）全部秒崩。
> 修法：`toolchain/shizuku-aidl.jar`（从老包反编译源码重编的 18 个类，SERVER_VERSION=12），
> 构建脚本已同时进 javac classpath 和 d8 输入。验证标准：**dex 类集与已知好包逐项 diff 为零**。
