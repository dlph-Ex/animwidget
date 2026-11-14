# 交接材料：帧工坊（AnimWidget）反编译 / 重建 / 改造 —— 给外部 AI 的全量事实

> 读者：外部 AI（或外部工程师）。**只依赖本文件 + 仓库本身**即可开工，不需要再问我任何环境问题。
> 基线：`v1.10.512`（versionCode 626），产物 `dist/AnimWidget-v1.10.512.apk`，6,819,621 字节，
> SHA-256 `717998619ce486fb67f92d2e8bb72a2ba3c23bb8c05bd0b7f22b30bb9f08a285`。
> 本文件里出现的每个数字都是**实测**，不是估计；凡"没实测过"的地方我都明写了。
> 注：材料快照是 v1.10.512；签名自检已落到 **v1.10.514**，实现与验收见 `SECURITY-SIGNATURE-ANTITAMPER.md` §8。

---

## 0. 一句话现状

这是一个**曾经丢失源码、靠反编译救回来、现在以手写 Java 源码树自维护**的 Android 桌面动图组件 App。
出包**不走 Gradle**，走仓库里的 shell 脚本：`javac → aapt2 → d8 → 自写 zip 打包 → apksig 签名 → 33 段静态门禁`。
所以对外部 AI 的要求不是"写功能"，而是**在一条没有 IDE、没有设备、没有 Gradle 的链路上，改动可验证地落进 APK**。

---

## 1. 产品与身份（对外可核对的事实）

| 项 | 值 |
| --- | --- |
| 应用名 | 帧工坊 · FrameSmith |
| package | `io.github.animwidget` |
| versionName / versionCode（当前） | `1.10.512` / `626` |
| minSdk / targetSdk（APK 实际声明） | `26` / `35` |
| compileSdk（清单里写的） | `34` |
| 签名方案 | **只有 v2**（`SignApk verify` 实测 `v1=false v2=true`） |
| 签名证书 SHA-256 | `5E:9B:78:5C:93:2E:49:AD:62:1E:BE:FB:31:FB:14:66:59:2F:A9:62:88:29:55:A6:9C:EB:89:5D:04:EE:F4:D3` |
| dex 类数（实测） | 1854 |
| `classes.dex` / `resources.arsc` | Stored（不压缩），CRC32 分别是 `0xe8a669bd` / `0xad5668c6` |
| 依赖 | 无第三方 SDK；只有 Kotlin 运行时（必需）、`org.jetbrains.annotations`、Shizuku（可选） |

---

## 2. 目录结构（哪些能改、哪些别碰）

| 路径 | 作用 | 可否修改 |
| --- | --- | --- |
| `src-v2/io/github/animwidget/*.java` | **唯一的源码树**，140 个 `.java`，全部同一个包（无子包） | ✅ 主战场 |
| `patch-v1.4.0/res/` | 资源源码（51 个文件，7 个子目录：`values/`、`values-zh/`、`layout/`、`drawable/`、`drawable-nodpi-v4/`、`font/`、`xml/`） | ✅ 只加不改名 |
| `patch-v1.4.0/AndroidManifest.xml` | 清单源码（9 条 uses-permission、`<queries>`、10 个 Activity 全 `portrait`、共 14 个组件） | ✅ 谨慎 |
| `patch-v1.4.0/stable-ids.txt` | 692 行 `包名:类型/名字=0x7f......`，aapt2 `--stable-ids` 的输入 | ✅ 只追加 |
| `src-v2/io/github/animwidget/R.java` | **手写**的 R 类（资源 id 被 javac 内联进 dex） | ✅ 必须与 arsc 一致 |
| `toolchain/` | 155MB 固化工具链（见第 4 节） | ❌ 别动 |
| `scripts/` | 构建 / 门禁 / 对拍脚本（见第 5、7 节） | ✅ 可加脚本 |
| `build-manual/` | 本机签名密钥 `debug.keystore` + 历史手工构建中间物 | ⚠️ 密钥**绝不能外传**，已在 `.gitignore` |
| `recovered/` | 当年反编译的原始产物（`classes-v1.3.1.jar`、`src-v1.3.1/`、`res-src-v1.3.1/`、`mapping-v1.3.1.txt`、`AndroidManifest-v1.3.1.xml`） | ❌ 只作参考 |
| `dist/` | 出包目录，命名固定 `AnimWidget-v<versionName>.apk` | ✅ 产物 |
| `app/`、`build.gradle.kts`、`.github/workflows/build.yml` | **v1.1.0 的老 Gradle 骨架，已失效**（`versionCode = 43`，没有真实源集） | ❌ 不要用它构建（见 7.1 的风险） |
| `LESSONS.md` | 221 条踩坑记录，**动代码前必读**（第 9 节给索引） | ✅ 只追加 |
| `docs/` | 性能分析与方案文档（如 `ANALYSIS-499-LATENCY.md`） | ✅ |

---

## 3. 源码树的来历（反编译这一段必须知道）

1. v1.1.1~v1.3.1 的原始源码丢失；用 **jadx**（`--no-inline-kotlin-lambda`）反编译 `classes-v1.3.1.jar`，
   得到 `recovered/src-v1.3.1/`（38 个文件，语法基本正确）；
2. 资源用 **apktool** 反编译，得到 `recovered/res-src-v1.3.1/`；
3. 之后**逐版手工修补**，形成 `src-v2/`（现在是唯一的真源码，不再依赖 `recovered/`）。

### 3.1 jadx 输出的四类典型伤（改这些文件时一定会遇到）

| 症状 | 表现 | 正确修法 |
| --- | --- | --- |
| Kotlin lambda 内联 | 出现 `r1` / `r2` / `this$0` / `$this$xxx` 之类占位名 | 按调用点改名；不要靠"能编译"就放过，命名要还原语义 |
| `Intrinsics.checkNotNullParameter` | 每个 Kotlin 函数开头一行 | **保留**（`kotlin-stdlib` 在 dex 里，不能删） |
| `copy$default` / `$default` | 合成方法一堆 | 保留；新增调用走公开方法 |
| 类型推断失败被跳过 | 如早期 `importVideo` / `PreviewFrames.load` | 该函数必须按字节码手写回来，不能靠 jadx 重跑 |

### 3.2 反向链路（在**你自己**的环境里跑，本仓库没有 jadx/apktool 二进制）

```bash
# ① 清单与资源
aapt dump badging   app.apk | head -5
aapt dump xmltree   app.apk AndroidManifest.xml      # 清单树
aapt2 dump resources app.apk > res.txt               # 资源 id 全表（和 R.java 对拍用）

# ② 资源源码
apktool d -f -o resout app.apk

# ③ 代码（Kotlin lambda 不要内联，否则还原不出来）
jadx -d javaout --no-inline-kotlin-lambda app.apk

# ④ 结构核对
unzip -l app.apk | head -30                          # 注意 resources.arsc / classes.dex 必须是 Stored
unzip -v app.apk | grep -E "resources.arsc|classes.dex"
```

> 本仓库特有的坑：`R.java` 是**手写**的，jadx 生成的 `R` 不可用；反编译后必须用
> `patch-v1.4.0/stable-ids.txt` + `scripts/sync-r.py` 对齐（见 6 节）。

---

## 4. 工具链（全部在 `toolchain/`，与 `/tmp` 无关，可离线）

| 文件 | 是什么 | 用途 |
| --- | --- | --- |
| `android-all.jar` | Robolectric 的 API 34 完整框架（含 `res/arsc`） | javac 的 `-bootclasspath`、aapt2 `-I`、d8 `--lib` |
| `jdk-core-stubs.jar` | JDK 17 抽出的 `java.**` 存根（`android-all.jar` 刻意不含 `java.**`） | 同上，缺了 `String`/`List` 都编不过 |
| `core-lambda-stubs.jar` | `java.lang.invoke` 存根（lambda 编译期需要） | 同上 |
| `kotlin-stdlib.jar` | Kotlin 运行时（`ThemeManager` 等类本来是 Kotlin） | **既进 `-classpath` 也进 d8 的 input** |
| `annotations-13.0.jar` | `org.jetbrains.annotations`（`@NotNull` / `@Nullable`） | `-classpath` |
| `shizuku-all.jar` / `shizuku-aidl.jar` | Shizuku SDK 与 AIDL 存根 | `-classpath` + d8 input |
| `r8.jar` | d8 / R8 | dex；`OBFUSCATE=1` 时走 R8 混淆 |
| `apksig.jar` + `signapk-cli.jar` | APK 签名/验签 CLI | 签名与 `verify` |
| `DexCheck.java` / `ClassCheck.java` | 类清单导出 / ASM 字节码校验 | 门禁 [1][2][4] 的实现 |
| `asm-*.jar` | ASM 9.7（`ClassCheck` 用） | 门禁 [4] |

实测版本：`javac 17.0.20.1`、`openjdk 17.0.20.1`、`Python 3.12.3`、`aapt2`（Debian 包，`aapt version` 报 `2.19-debian`）。

**语言级别是 Java 8**：`javac -source 8 -target 8`。所以**不许**用 `var`、`switch` 表达式、`record`、
text block、`instanceof` 模式匹配；lambda 与 `diamond` 可以用。

---

## 5. 正向链路：源码 → APK（一条命令，五步）

```bash
./scripts/build-apk-src.sh <versionCode> <versionName>
# 例：./scripts/build-apk-src.sh 627 1.10.513
```

内部五步（`build-apk-src.sh` 原样）：

```bash
# [1/5] javac（源码 8 / 目标 8，bootclasspath 用 android-all + jdk stubs + lambda stubs）
javac -nowarn -source 8 -target 8 \
  -bootclasspath "toolchain/android-all.jar:toolchain/jdk-core-stubs.jar:toolchain/core-lambda-stubs.jar" \
  -classpath    "toolchain/kotlin-stdlib.jar:toolchain/annotations-13.0.jar:toolchain/shizuku-all.jar:toolchain/shizuku-aidl.jar" \
  -d <OUT>/classes $(find src-v2 -name '*.java')

# [2/5] aapt2 资源（注意：compile 的输入目录是 patch-v1.4.0/res，不是仓库根的 res/）
aapt2 compile --dir patch-v1.4.0/res -o <OUT>/res.zip
aapt2 link -o <OUT>/res.apk -I toolchain/android-all.jar \
  --manifest patch-v1.4.0/AndroidManifest.xml \
  -R <OUT>/res.zip \
  --stable-ids patch-v1.4.0/stable-ids.txt \
  --min-sdk-version 26 --target-sdk-version 35 \
  --version-code <VC> --version-name <VN> --auto-add-overlay

# [3/5] 先把 class 打成 jar（脚本里是在 <OUT>/classes 下 jar cf），再 d8
(cd <OUT>/classes && jar cf <OUT>/classes.jar .)
# 默认走 d8；OBFUSCATE=1 时改 R8，规则 scripts/r8-rules.pro
java -cp toolchain/r8.jar com.android.tools.r8.D8 --release --min-api 26 \
  --lib toolchain/android-all.jar --lib toolchain/jdk-core-stubs.jar \
  --output <OUT>/dex <OUT>/classes.jar toolchain/kotlin-stdlib.jar toolchain/shizuku-all.jar toolchain/shizuku-aidl.jar

# [4/5] 自写 zip 打包（resources.arsc / classes.dex 必须 Stored，且 4 字节对齐补 0xD935 extra）
python3 scripts/pack_apk.py <OUT>/unsigned.apk <OUT>/res.apk <OUT>/dex/classes.dex

# [5/5] 签名 + 验签
java -cp "toolchain/apksig.jar:toolchain/signapk-cli.jar" SignApk sign \
  build-manual/debug.keystore android android androiddebugkey \
  <OUT>/unsigned.apk dist/AnimWidget-v<VN>.apk
```

**三条容易踩的硬要求**

1. `resources.arsc` 必须 Stored 且 4 字节对齐（Android 11+ 要求；`pack_apk.py` 就是为这个手写的 zip 结构）；
2. `kotlin-stdlib.jar` / `shizuku-*.jar` 必须作为 **d8 的 input**，不能只放 `--lib`（放错的后果：
   APK 里没有 Kotlin 运行时，**一启动就 `NoClassDefFoundError`**，编译与门禁全都查不出来）；
3. 签名只能走上面这条命令（v2 only，实测 `v1=false v2=true`）。minSdk 26 所以不需要 v1。

---

## 6. 资源 id 的三条硬规则（这是本项目最容易被外部 AI 改坏的地方）

1. **名字不能改，只能加**：`stable-ids.txt` 保证「已分配过的名字」在每次 link 时拿到同一个 id。
   改名 = 旧 id 消失 = 运行期 `Resources$NotFoundException`；
2. **新增资源后必须跑 `python3 scripts/sync-r.py`**：它执行 `aapt2 compile+link` → `aapt2 dump resources`
   → 把 `R.java` 里**已存在**的条目改成实际分配的 id，并报告两边不一致的条目；
3. **`R.java` 是手写的**：id 会被 javac 当编译期常量内联进 dex，所以 `R.java` 与 `resources.arsc`
   必须逐个相等（门禁 [3] 就是在查这个，852 条资源全对）。

> 遗留债（实测）：`ConfigActivity.java` 里还有 **5 处** `0x7f08xxxx` 硬编码（第 1918、3317、3321、3325、3329 行），
> 没换成 `R.id.*`。另外 `R.java` 里 `tv_log_body = 0x7f080079`、`tv_log_hint = 0x7f08007b` 这两个常量
> **不许漂移** —— 门禁 [17] 判的是布局 XML 里 `tv_log_hint` 必须在、`tv_log_body` 必须不在，id 一漂它就判错。

---

## 7. 验证：33 段门禁（编号 [1]~[32]，其中 [4] 含 [4b]）

```bash
./scripts/verify-apk.sh dist/AnimWidget-v<新>.apk dist/AnimWidget-v<旧>.apk
# 最后必须看到：  == 结论
#                 OK
```

| 段 | 查什么 | 红线（一旦红了说明什么） |
| --- | --- | --- |
| [1] 清单组件 → dex | 清单里声明的 14 个组件类（10 Activity + 2 Service + 1 Receiver + 1 Provider）必须在 dex 里 | 少了就是启动/拉组件时 `ClassNotFoundException` |
| [2] 类清单 vs 上版 | 不能凭空少「真类」（匿名/$ 合成类可少） | 少一个真类 = 某条路径 `NoClassDefFoundError`；故意删除要加白名单并写理由 |
| [3] R.java vs arsc | 852 条资源 id 必须逐个一致 | 不一致 = 运行期拿到别的资源或崩 |
| [4] 字节码校验 | ASM `CheckClassAdapter`，`bad=0` | `bad>0` 是真实字节码损坏 |
| [4b] 运行时依赖 | dex 里 kotlin 类 ≥900、rikka 类 ≥20 | 不满足 = 库被当 `--lib` 用了 |
| [5] 主题结构 | 生效色键必须被激活方案重写；自绘控件必须有刷新路径 | 换肤链路断开（改色不生效） |
| [6] 形状/阴影几何 | 蒙版不被拉歪、阴影覆盖角 | 画面几何错 |
| [7] S03 探头判据 | 33 条离屏断言 | 判据写错会让检测永远 OK/永远 FAIL |
| [8] 判断代号体检 | 日志/诊断代号必须登记、不重复、一码一义 | 新加日志代号没登记 → 红 |
| [9] 强转 vs 布局 | `findViewById` 的强转类型必须与布局控件一致 | 不一致 = `ClassCastException` 闪退 |
| [10] 对比度颜色模型 | 13 条断言 | 颜色模型被改动 |
| [11] 折叠几何 | 35 条断言 | 标题收尾跳变 |
| [12] 比例判据 | 29 条断言 | 画布/格子比例错 |
| [13] 设置页两卡对拍 | 卡片·header·图标·标题·胶囊·行结构逐项相等 | 版式错位 |
| [14] 画布预算自校准 | 206 条断言 | 预算阶梯被复制/方向错 |
| [15] 布局 id 体检 | 代码引用的 id 必须在布局里存在 | 改版式删了控件 → 红 |
| [16] 预读环 | 17 条断言 | 环重复提交/取未来帧 |
| [17] 日志页版式 | 提示行在、旧正文不在、dex 无"按文案认人"的字面量 | 又用文案当选择器 |
| [18] 日志页刷新节流 | 惯性状态位、清屏/撤回强制刷新 | 刷新被"正在滑动"永久吞掉 |
| [19] 日志两条承诺 | TTL 10 条 + 逐条渐显 9 条 | 保留窗口/渐显行为变了 |
| [20] 导出收尾顺序 | 17 条断言（提交先于烘焙、三处闸门） | 顺序反了 = 桌面没画面 |
| [21] 动图 WebP 封装 | 52 条（Pillow 解码对拍，有损+无损） | ANMF 写错位 |
| [22] 分层指纹 | 20 条 | overlay 复用判据失准 |
| [23] 预览虚线取色 | 25 条 | 虚线色取错 |
| [24] 语义色编辑页 | 56 条 | 控件串页/形状不一致 |
| [25] 动图打包器 | 28 + 18 条 | 帧数/时长/循环写错 |
| [26] 编码解码对拍 | 31 + 46 条 | 单位/判据方向错 |
| [27] 共享解码 N→1 | 32 + 19 条 | 键归一化/回收错 |
| [28] 分层帧 JPEG+alpha | 24 条 | 自检不过/产物残留 |
| [29] 推帧统计口径 | 11 条 | 同帧去重计数错 |
| [30] 剩余性能空间 | 26 条 | 实测常量/夹取错 |
| [31] 性能页/按钮两态/锁参 | 179 条 | 锁参清单漏项（新增滑块必须同步进 `LOCKED_PARAM_IDS`） |
| [32] GIF 流式解码 | 60 条 + 7 个样本逐字段对拍 | 退回整份读、写盘失败不报错 |
| [33] 签名自检（v1.10.513 起） | 指纹 = 钥匙库证书、28+/26 两条取法、多签名者/轮换判据、调用点 ≥3、清单 `.App`、K51/K52/K53/K54 已登记、无 `throw` | 被改一位 / 少一条判据 / 少调用点 / 丢了清单属性 / 未登记代号 → 红 |
| [33b] 自检门禁正反对拍（v1.10.514 起） | `scripts/integrity-check-selftest.py`：23 个用例（3 正向 + 20 反向），逐个验证门禁真的会红在对的段落 | 门禁自身被改弱（只查 token / 只查声明）→ 红 |
| [33c] 限制模式接线 | 导出、推帧唯一出口、`onUpdate`、设置页提示、关于页证书行 —— 五处都在 | 任何一处被删 → 红 |

### 7.1 一个必须告知的风险点（与安全需求直接相关）

`.github/workflows/build.yml` 现在跑的是 `gradle assembleDebug` —— 构建的是 `app/` 里 **v1.1.0 的老骨架**
（`versionCode 43`），而且 `assembleDebug` 会用**公共的 AOSP debug key** 签名。
这既是"假产物"，也会让用户对"签名"这件事失去判断力。**建议直接停用这个 workflow**（见第 10 节）。

---

## 8. 12 条不变量（改代码前先背下来）

1. 所有类都在 `io.github.animwidget` 一个包里，不建子包；
2. Java 8 语言级别（见第 4 节禁用列表）；
3. 只允许 toolchain 里那 4 个库，禁止新增第三方依赖（离线环境，无 Maven）；
4. 资源**只加不改名**；新增后跑 `sync-r.py`；
5. `R.java` 手写，必须与 arsc 一致；
6. 清单里声明的组件类必须在 dex 里；
7. 不许删「真类」（要删就加进 `verify-apk.sh` 的白名单并写理由）；
8. 每条新日志/诊断代号必须登记进 `DiagCatalog`；
9. 布局 id 与强转类型必须一致；
10. 自绘控件的颜色必须能走换肤链路刷新；
11. `versionCode` 单调递增，产物命名 `dist/AnimWidget-v<versionName>.apk`；
12. 改完必须自己跑 `verify-apk.sh`，并在交付里贴出**结论行**。

---

## 9. 动代码前必读的 6 条 LESSONS（完整 221 条在 `LESSONS.md`）

| 编号 | 一句话 |
| --- | --- |
| #219 | 本应用的对话框主题是 `windowIsFloating=false`，`AlertDialog` 会贴顶；且 `windowIsTranslucent` 不响应 `ADJUST_RESIZE` |
| #220 | 「半透明卡片」≠「毛玻璃」：背后要真糊、卡片里要比外面**更糊**（`FrostedGlassDrawable`） |
| #221 | 「把页面拍下来」的界面（模糊底/缩略图/分享长图），重建点必须排在页面换色**之后** |
| #176 系列 | 门禁判据只认代码行，注释里引述旧写法会把门禁自己点红 |
| #3 | 门禁本身必须先被验证（曾出现 `bad=1` 仍打印"结论 OK"） |
| #1 | 不许拿"自己补的默认值"当客观证据（日志里的数字必须能追到来源） |

---

## 10. 外部 AI 的边界（很重要，别越界交付）

**你能做**：读全部源码/脚本/资源；给出精确 diff；跑 `javac` 编译验证；跑 `verify-apk.sh`；
跑对拍脚本（`gif-stream-check.py`、`webp-anim-check.py` 等）；用 `aapt2 dump` 核对资源 id。

**你不能做**：**跑不了 App**（没有设备/模拟器）、截不了图、测不了帧率与手感、无法真机验证键盘/模糊/动画。
所以：**任何"看起来对"的结论都必须落成静态证据**（门禁段落、assert 条数、id 对拍），
不许写"应该没问题""测试通过"这类没有证据的话。

**交付格式**（按这个给，我这边才敢合）：

1. 变更清单：文件 + 行区间 + 为什么；
2. 每处改动对应的门禁段落（哪一段会拦住回退）；
3. 自己跑过的命令与结果（`javac` 无 error；`verify-apk.sh` 结论行）；
4. 无法验证的部分**显式列出**（例如"键盘避让需要真机确认"），不要伪装成已验证。

---

## 11. 两条安全需求

见 **[`SECURITY-SIGNATURE-ANTITAMPER.md`](SECURITY-SIGNATURE-ANTITAMPER.md)**：①应用内签名校验；②反制「改包、加广告、冒名」。
那份文档里有现状风险、API 取舍、失败行为档位、以及必须由我拍板的两件事。
