# v1.3.6：配置页「上半屏预览」+ 干掉顶部白条

## 这次终于能动资源了

之前只能打二进制补丁，是因为资源编译链没打通。这一版起：

- 用 apktool 反编译出的**完整资源源码**（`patch-v1.3.6/res/`）作为唯一事实源；
- `aapt2 compile --dir res` + `aapt2 link --stable-ids stable-ids.txt`
  （`stable-ids.txt` 由原包 `public.xml` 的 355 条 ID 生成）；
- 已逐一比对：**355 个资源的 ID 与 v1.3.1 原包完全一致**，`classes.jar` 里 R 类的引用不会错位。

也就是说：以后布局、样式、字符串、颜色都能正常改，不用再二进制打补丁。

## 布局改动（`res/layout/activity_config.xml`）

| 元素 | 改前 | 改后 |
|---|---|---|
| 根 LinearLayout | `background="#fff7f8fa"` 写死浅色 | 去掉背景，交给主题（深色模式不再有浅色底） |
| `preview_frame` | `height=272dp` 固定 | `height=0dp` + `weight=1`，与下方 ScrollView 1:1 平分剩余空间 |
| `preview_area` | `height=220dp` + `layout_gravity=bottom` | `height=fill_parent`（撑满上半屏），预览框因此在**上半屏居中** |

固定高度只有 标签栏 48dp + 指示条 3dp + 分割线 1dp + 保存按钮 54dp = 106dp，
所以分界线落在屏幕高度约 **43%** 处（和参考图基本一致：壁纸预览占上半屏，下面是内容区）。

## 顶部白色横条

根因：`EdgeToEdge.apply()` 在 SDK 35+ 给 `android.R.id.content` 加了 `paddingTop = 状态栏高度`，
配置页顶部这块露出窗体背景色 → 白条。

改法（`EdgeToEdge.java` 重编译）：

```java
final boolean ignoreTop = activity instanceof ConfigActivity;
view.setPadding(bars.left, ignoreTop ? 0 : bars.top, bars.right, bars.bottom);
```

- 组件配置页：不吃顶部 inset → 壁纸预览直接顶到屏幕最上沿；
- 主界面 / 设置页：仍然留状态栏高度，不受影响。

## 其它

- 架构没动：标签页、素材选择器、形状网格、手势编辑器、预览/抽帧逻辑全部沿用；
- 阴影等比留白（v1.3.2）、原地换肤 + 不重建（v1.3.3）、`isDark` 只认系统资源（v1.3.4）、
  `area/box` 退化兜底（v1.3.5）都保留。

## 构建 / 校验

```bash
ANDROID_SDK=/tmp/sdk KOTLIN_LIBS=/tmp/kl SHIZUKU_JAR=/tmp/shizuku-all.jar \
  ./patch-v1.3.6/build.sh 69 1.3.6
```

- 资源 ID：355/355 一致；
- APK 回读：`versionCode=69`、`configChanges=0x200`、`preview_frame` = `0dp` + `weight 1`、
  `preview_area` = `fill_parent`、`preview_box` gravity 居中；
- `EdgeToEdge` 字节码含 `instanceof ConfigActivity` 分支；签名 `5e9b78…f4d3`。

## 可调参数

想让预览更高/更矮，只改 `preview_frame` 的 `android:layout_weight`（例如 1.2 就比内容区高），
或把 ScrollView 的 weight 改成 0.8。告诉我具体比例我直接出包。
