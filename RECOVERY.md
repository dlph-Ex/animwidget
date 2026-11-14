# 源码丢失与恢复说明（重要）

## 现状

本工作区里的 `app/src/main/java` **只有 v1.1.0 的 Kotlin 源码**（无深色主题、无多语言、无手势编辑器、
无 MediaSelectorView、无 R8 配置）。v1.1.1 ~ v1.3.1 的源码在沙箱重置中丢失，`/tmp` 下只剩各次构建的
**中间产物**，因此 v1.3.2 是这样产出的：

| 目录 | 内容 |
|---|---|
| `recovered/classes-v1.3.1.jar` | v1.3.1 **R8 之前**的原始 class 包（未混淆、97 个类），可再 D8/R8 |
| `recovered/res-v1.3.1.apk` | v1.3.1 原样资源包（resources.arsc + 编译后 XML），资源 ID 与上面的 R 完全对应 |
| `recovered/res-src-v1.3.1/` | apktool 反编译出的资源源码（layout / values / drawable） |
| `recovered/src-v1.3.1/` | jadx 反编译出的 Java（38 个文件，**未修完**） |
| `recovered/mapping-v1.3.1.txt` | v1.3.1 的 R8 映射表 |
| `recovered/AndroidManifest-v1.3.1.xml` | 反编译出的 manifest（可读版） |
| `patch-v1.3.2/` | v1.3.2 的补丁源码与可复现构建脚本 |

> 只要 `recovered/classes-v1.3.1.jar` + `recovered/res-v1.3.1.apk` 在，就能继续出「补丁版」安装包，
> 不必等整份源码修好。

## 反编译源码的完成度

`recovered/src-v1.3.1/` 用 jadx（`--no-inline-kotlin-lambda`）反编译，语法基本正确，但直接 `javac` 仍有
**约 460 处错误**，集中在：

1. `r1` / `r2` / `this$0` 之类的**捕获变量名丢失**（jadx 把 Kotlin lambda 内联后留下的占位）；
2. `XxxActivity$themePoll$1` 这类**匿名类名被当成类型**（改回 `Runnable` / `BroadcastReceiver` / `LruCache`）；
3. 缺失 import（`Unit`、`InputStream`、`MediaExtractor`、`Window`…）；
4. `WhenMappings` 内部类缺 `static`、`$VALUES` 枚举合成字段缺失；
5. `super()` 不在构造函数首行。

修完这些即可恢复成可编译工程（不需要再写业务逻辑，全是机械修正）。

`ConfigActivity` 有 4 个方法（`applyKindUi` / `save` / `openContentGestureEditor` / `renderPreviewNow$lambda$86`）
和 `MediaImporter.importVideo` / `PreviewFrames.load` / `SourceFps.video` 被 jadx 跳过（类型推断 NPE），
这些在 `patch-v1.3.2/` 的同名构建里用 Vineflower 的反编译结果补齐过，需要时从
`/tmp/dec_java`（若还在）或重新用 Vineflower 生成。

## 下一步建议

1. 先把 `recovered/src-v1.3.1` 修到 `javac` 全绿 → 替换掉仓库里的 v1.1.0 Kotlin 源码，恢复 Gradle 工程；
2. 再恢复 R8（补上 XML 反射类的 keep 规则：`MediaSelectorView`、`ShapeChipView`、`HsvPickerView`、
   `AspectRatioFrameLayout`、`ContentGestureView`），APK 回到 ~400 KB；
3. 之后才继续做功能（设置页卡片分组、文本叠加、状态栏跟随等）。
