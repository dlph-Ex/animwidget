# 语义色编辑页「状态栏不能透明」—— 三份外部答案的交叉核对 + 字节码级验证（v1.10.367）

> 本文是对 C/G/D 三份外部答案的**逐条复核**。复核手段不是「读文档后同意」，而是：
> ①拿本机 `toolchain/android-all.jar`（AOSP **API 34** 完整框架实现）反编译 `android.view.WindowLayout#computeFrames`
> 与 `WindowManager$LayoutParams` 的构造器，**用字节码逐行验证**；②回到本仓库代码逐处核对。
> 标注规则：**【已验证】**= 我用字节码/代码确认过；**【存疑】**= 说法与字节码不符或只对一半；**【无证据】**= 我们的仓库里根本没有这条观测记录。

---

## 0.0 实测已闭合（v1.10.368 A12 读数，sdk=37）

```
A12 调色窗 拍=8 decor=0,138 尺寸=1260x2662 屏=1260x2800 bars=0,0,0,138 fitTypes=0x107 fitSides=0xf flags=0x81800302 sdk=37 top=#181E1E
```

- `decor.y = 138` = 状态栏高度，`尺寸 = 2800 − 138` → **判读表第 2 行命中**：窗口被 fitInsets 收缩。
- `fitTypes = 0x107` 含 `statusBars` 位 → 根因 100% 落在 `setFitInsetsTypes` 上。
- **底边一直到 2800**（盖住导航栏）→ 「导航栏透了、状态栏没透」不是 ROM 特例，是同一个 displayFrame 只在上边扣了 inset。
- `sdk=37` → 设备是 API 37（Android 17），此前只从 `Build.VERSION.RELEASE` 读到字符串。
- `bars bottom=138` → 3 按钮导航栏 138px（与实机截图 □○◁ 一致）。
- v1.10.369 已按此改：`setFitInsetsTypes(0)/setFitInsetsSides(0)` + 去掉 NO_LIMITS，其余不动（单变量）。

**改完的读数（v1.10.369，同一台机）—— 窗口层已闭合：**

```
A12 decor=0,0 尺寸=1260x2800 屏=1260x2800 bars=0,138,0,138 fitTypes=0x0 fitSides=0x0 flags=0x81800102 sdk=37
```

`decor` 从 `0,138 / 1260x2662` 变成 `0,0 / 1260x2800`，`fitTypes 0x107 → 0x0`；
`bars` 由 `0,0,0,138` 变成 `0,138,0,138`（窗口现在与状态栏重叠，inset 才报得出来）。
剩下的只有像素级验收：状态栏那一条现在应该是光轮本身，而不是任何窗口底色。

## 0. 结论（修订版）

1. **根因已定，是平台算法，不是这台 vivo 的 ROM 怪癖。**【已验证】
   `Dialog` 是 `TYPE_APPLICATION` 窗口且没有 attached frame，`WindowLayout.computeFrames()` 里
   `parentFrame = displayFrame`，而 `displayFrame = windowBounds − systemBars(fitInsetsTypes/Sides)`。
   `MATCH_PARENT` 取的是 **parentFrame 的尺寸** → 对话框内容**物理上顶不到 y=0**。
2. **我们试过的那六样（`setLayout(-1,-1)` / `FLAG_LAYOUT_IN_SCREEN` / `FLAG_LAYOUT_NO_LIMITS` /
   `setDecorFitsSystemWindows(false)` / `SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN` / `setStatusBarColor(TRANSPARENT)`）
   全都改不了「窗口框」**。【已验证】前三样改的是「选哪块矩形/事后膨胀」，后三样改的是「窗口内部怎么排」。
3. **正解的第一顺位是 D 家的 `LayoutParams#setFitInsetsTypes(0)`（+ `setFitInsetsSides(0)`），API 30+，约 8 行**。【已验证机制】
   它直接让 `calculateInsets` 返回 0 → `displayFrame = windowBounds`（整屏）→ parentFrame 整屏 → MATCH_PARENT 整屏。
4. **D 家的备用方案「`windowIsFloating=false`」机制存疑**。【存疑】见 §1 表格第 5 行。
5. **C/G/D 三家共同的一个前提「状态栏颜色会跟着光轮平均色变，所以 ROM 还认 setStatusBarColor」——我们没有任何证据。**【无证据】
   仓库里只有注释写的**设计意图**，没有一条日志、截图或用户反馈证明确实变了。这条不能当事实用。
6. **顺带查出一个我们自己的真缺陷**：`RewardSheet`/`AboutSheet` 的补边算法在 API 35+ **失效**
   （content 是靠 padding 让位的，`contentLoc−decorLoc = 0` → 不补边；而且没人关 `clipToPadding`）。
   → 从**设置页**打开赞赏弹层时，顶部会缺一条状态栏高度的模糊底。这是同一个病根的另一张脸。**【已验证（代码层）】**

---

## 1. 三份答案交叉核对表

| # | 结论 | 谁说的 | 判定 | 我的依据 |
|---|---|---|---|---|
| 1 | Dialog 覆盖不了状态栏是**平台算法**（frame 被 inset 收缩），不是 ROM 问题 | D | **【已验证】成立** | `computeFrames` 字节码：`Insets insets = insetsState.calculateInsets(windowBounds, attrs.getFitInsetsTypes(), attrs.isFitInsetsIgnoringVisibility())` → 逐边 `windowBounds ± insets` 写进 `displayFrame`；`attachedFrame==null` 时 `parentFrame.set(displayFrame)` |
| 2 | `MATCH_PARENT` 等于收缩后的父框 | D | **【已验证】** | 字节码后续直接 `parentFrame.width()/height()` 作为 w/h |
| 3 | `FLAG_LAYOUT_IN_SCREEN` 也救不了 | D（C 没提） | **【已验证】** | 该 flag 只决定 `parentFrame = layoutInScreen ? displayFrame : attachedFrame`；而 `displayFrame` 本身已经是收缩过的 |
| 4 | `FLAG_LAYOUT_NO_LIMITS` 是干扰项 | D | **【已验证】** | NO_LIMITS(0x200) 的判断在字节码 offset 613–681，把 `displayFrame` 炸成 ±100000 —— 但 `parentFrame` 早在 offset 197–261 就拷好了，之后不再更新 |
| 5 | 备用修法：主题里 `windowIsFloating=false` | D | **【存疑】** | `isFullscreen()` 字节码只看 `x==0 && y==0 && width==-1 && height==-1`，**不读 floating 标志**；`computeFrames` 的父框路径也没有 floating 判断。它可能只改变 WMS 的浮窗行为，不改变框。**不作主力方案** |
| 6 | 正解 = `setFitInsetsTypes(0)` | D（C 也独立提到） | **【已验证 API 34 存在】** | `android.view.WindowManager$LayoutParams#setFitInsetsTypes(int)` / `setFitInsetsSides(int)` 在 API-34 框架里确实存在；默认值 `mFitInsetsTypes = WindowInsets.Type.systemBars()`、`mFitInsetsSides = Side.all()`（构造器字节码） |
| 7 | 「换 content 覆盖层」是最终形态 | C / G / D | **【成立】**，但代价最大 | 与我们自己 183（赞赏）/359（关于）两次迁移的经验一致 |
| 8 | 补边要算 content 的 padding，并关 `clipToPadding` | C / D | **【已验证：我们代码确实缺】** | `EdgeToEdge.apply` 给 content 加 padding；`RewardSheet` 用 `contentLoc−decorLoc` 算 cover → 恒 0；`disableClip` 只关 `clipChildren`，全工程无一处对 content 关 `clipToPadding`（`grep setClipToPadding` 只有滚动容器） |
| 9 | 编辑页别照搬 `capture()` 截屏模糊底 | C / D | **【成立】** | 编辑页底层就是实时光轮；截屏底不含系统栏区域，会把同一个 bug 换个地方复现 |
| 10 | 记 `SDK_INT`，不要只看 `Build.VERSION.RELEASE` | G | **【成立】** | 我们「关于」页现在只显示 RELEASE（"Android 17"），日志里也没有 SDK_INT |
| 11 | 150/152 的记录说明 ROM 仍认 `setStatusBarColor`，所以「35 上无效」被否掉 | C（D 同调） | **【无证据】** | 仓库里没有任何「状态栏颜色跟着变成平均色」的日志/截图/用户原话；只有注释里的意图。**两家都把它当前提，这是最危险的一条** |
| 12 | 「导航栏透了、状态栏没透」说明 ROM 对顶/底处理不同 | C / D 都没解释 | **【我的解释，待证】** | 纯 AOSP 数学下顶和底会被同样收缩；最省事的解释是**这台机器是手势导航，bottom inset≈0**，所以底部本来就没什么可缩的。一条 `insets.getInsets(systemBars())` 日志即可定案 |

---

## 2. 证据原文（可独立复核）

工具链里就有 AOSP 实现，直接反编译：

```bash
cd /workspace/AnimWidget
javap -p -c -classpath toolchain/android-all.jar android.view.WindowLayout > /tmp/wl.txt
javap -p -c -classpath 'toolchain/android-all.jar' 'android.view.WindowManager$LayoutParams' > /tmp/lp.txt
```

`WindowLayout.computeFrames()` 关键片段（`/tmp/wl.txt`，offset 为字节码偏移）：

```
 65: aload_2 / aload 4
 69: invokevirtual LayoutParams.getFitInsetsTypes:()I
 76: invokevirtual InsetsState.calculateInsets:(Rect,int,boolean)Insets
 81: invokevirtual LayoutParams.getFitInsetsSides:()I
...  逐边掩码 1/2/4/8 → 取 insets.left/top/right/bottom，不在 sides 里的置 0
160: aload 16                      // outDisplayFrame
161: aload 4                       // windowBounds
167: iload 21 (left) iadd
175: iload 22 (top)  iadd          ← 顶边被状态栏高度顶下去
183: iload 23 (right) isub
191: iload 24 (bottom) isub
194: Rect.set(IIII)
197: aload 15                      // attachedFrame
199: ifnonnull 247                 // 有 attachedFrame 走 247
202: aload 17 / aload 16 / Rect.set // 否则 parentFrame = displayFrame
247: aload 17
249: iload 14                      // layoutInScreen = (flags & 256)==256
251: ifne 259 → 254: attachedFrame / 259: displayFrame
261: Rect.set
613: flags & 512 (FLAG_LAYOUT_NO_LIMITS) → 656-681: displayFrame = (±100000)   ← 晚了，parentFrame 已拷
```

`LayoutParams` 构造器默认值（`/tmp/lp.txt`）：

```
124: WindowInsets.Type.systemBars()  → mFitInsetsTypes      // 默认 = 状态栏+导航栏+标题栏
131: WindowInsets.Side.all()         → mFitInsetsSides      // 默认 = 四边全算
```

含义：**任何没显式改过 `fitInsetsTypes` 的窗口，其 `displayFrame` 四边都会扣掉系统栏**；
`MATCH_PARENT` 拿的是扣完的尺寸。所以「Dialog 顶不到状态栏」是**默认行为**，
我们不是遇到 ROM 怪癖，而是**从来就没改过这个开关**。

---

## 3. 修法（按代价排序）

### 3.1 首选：8 行，不动载体、不动生命周期

在 `SettingsActivity.showColorEditor()` 里，`dialog.show()` **之后**（现在 `setLayout(-1,-1)` 那个位置）：

```java
Window w = dialog.getWindow();
WindowManager.LayoutParams lp = w.getAttributes();
lp.width  = WindowManager.LayoutParams.MATCH_PARENT;
lp.height = WindowManager.LayoutParams.MATCH_PARENT;
lp.gravity = Gravity.TOP | Gravity.START;            // 整屏窗口不再需要 CENTER
lp.flags |=  WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
lp.flags &= ~WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;   // 删掉：它只影响事后 clamp
if (Build.VERSION.SDK_INT >= 30) {
    lp.setFitInsetsTypes(0);          // ★ 关键：父框不再被系统栏收缩
    lp.setFitInsetsSides(0);          // ★ 双保险
    lp.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
    w.setDecorFitsSystemWindows(false);   // decor 不再自己加 padding
}
w.setAttributes(lp);
w.setBackgroundDrawable(new ColorDrawable(0));  // show 之后再设一次（防主题 windowBackground 回填）
```

同时把 `applyEditorSystemBars()` 里**对 Activity 窗口每 500ms 的 `setStatusBarColor(topColor)` 停掉**
（target 35 上它是死代码；色带消失后它只会污染排查），只留 `WindowInsetsController` 的图标明暗。

> <35 的路径：`setFitInsetsTypes` 不存在，但 API 26–29 的 frame 算法是老 `DisplayPolicy`，
> `FLAG_LAYOUT_IN_SCREEN`/`NO_LIMITS` 在那里**是有效的**（现有代码即可），所以按 SDK 分支即可，不需要第二套方案。

### 3.2 备选：换 content 覆盖层（若 3.1 在这台 ROM 上被拦）

搬运 `RewardSheet` 那套（挂 `android.R.id.content` + 负 margin + 关 `clipChildren`），
但**必须补上三处**（这是 C/D 都指出的、我们代码里确实缺的）：

1. cover 要算 **content 的 padding**：`topCover = (contentLoc[1]-decorLoc[1]) + content.getPaddingTop()`；
2. `content.setClipToPadding(false)`（关闭时还原），否则 padding 区画不出来；
3. 编辑页**不要**用 `capture()` 截屏模糊底 —— 底层直接用 `ColorAuroraView`。

### 3.3 不要做的

- 不要再试 `TYPE_APPLICATION_PANEL` / `FLAG_LAYOUT_ATTACHED_IN_DECOR` / 各种 cutout 组合：前两个已被平台废弃，
  且 sub-window 会被父窗口裁剪，收益为负（C、G、D 三家口径一致，我认同）。
- 不要再用「把状态栏刷成平均色」当作解 —— 那是「看起来连上」，不是透明；而且 target 35 下连颜色都未必生效。

---

## 4. 已确认的**第二个缺陷**：补边在 API 35+ 失效

```
SettingsActivity.java:178   EdgeToEdge.INSTANCE.apply(this)
                            └─ root.setOnApplyWindowInsetsListener → view.setPadding(l, bars.top, r, bars.bottom)
RewardSheet.java:307-320    topCover = contentLoc[1] - decorLoc[1]     // 35+ 上 content 位置就是 0 → cover = 0
RewardSheet.disableClip()   只 setClipChildren(false)，全工程没有一处对 content setClipToPadding(false)
FrameLayout                 默认 clipToPadding = true，且 MATCH_PARENT 子视图排在 padding 内
```

⇒ 从**设置页**打开赞赏弹层：顶部缺一条状态栏高度的模糊底、且被裁剪。
（从开发者页打开不会：`applyImmersive` 把 content padding 设成 0。）

**修法**：把 cover 改成「位置差 + content 的四边 padding」，并临时 `setClipToPadding(false)`（detach 还原）。

---

## 5. 待验证清单（都只需要一条日志，不需要截图）

| 待验证 | 方法 | 判读 |
|---|---|---|
| 到底是不是 fitInsets 收缩 | `diagEditorWindow()`：`decor.getLocationOnScreen()[1]`、`decor` 尺寸、`decor.getRootWindowInsets().getInsets(systemBars())`、`lp.getFitInsetsTypes()/getFitInsetsSides()` | `decorTop ≈ statusBarHeight` 且 `fitTypes=0x7` → 根因确认 |
| 这台机器导航栏 inset 是不是 0 | 同一条日志里的 `insets.bottom` | ≈0 → 解释掉「导航栏透了」 |
| 系统是否仍认 `setStatusBarColor` | 日志里同时打 `topColor`，截图取状态栏一线像素 | 像素跟着 `topColor` 变 → 认；不变 → 不认（这条能一次性否掉/证实三家共同的前提） |
| 修完后是否真覆盖 | 同上日志 | `decorTop==0 && decorH==screenH` |
| 验收像素 | `adb exec-out screencap -p > s.png`，取 `y = statusBarHeight/2` 一行在 x=0/50/600/1200 | 四点不再同色、且与光轮同源，并随相位变化 |

---

## 6. 执行计划（两个 build 收敛，符合「加指标和改行为分开验证」）

- **Build #1（只加指标，不改行为）**：`diagEditorWindow()` 在 `show()` 后与 `editorWatch` 里各打一次；
  `applyEditorSystemBars` 顶部把本次 `topColor` 记进同一条 `A06`。
- **Build #2**：§3.1 的 8 行 + 停掉 500ms 刷色 + §4 的补边修复（三处一起）。
  验收：`A06` 里 `decorTop=0`、`fitTypes=0x0`，截图 y=状态栏中线一行非纯色且随相位变化。

---

## 7. 红线（不变）

1. 不许引 AndroidX / 第三方库；不许动 `targetSdk 35`。
2. 改行为先问，改缺陷直接改；但**加指标与改行为必须分两个 build**，否则读数无法归因。
3. 不接受「写了配置就算修好」：每条结论必须配一条可观测读数或像素判据。
4. 单机环境（vivo V2426A），不接受「换台设备就好了」收尾。

---

## 8. 给外部 AI 的复核请求（只剩 3 个真问题）

1. §3.1 那 8 行里，`lp.gravity = TOP|START` 对已有 `Gravity.CENTER` 的**面板子视图**没有影响吧？
   （面板是 `shell` 的子视图，`FrameLayout.LayoutParams.gravity = CENTER`，我判断不受窗口 gravity 影响。）
2. `setFitInsetsTypes(0)` 之后，`decor` 收到的 `WindowInsets` 会不会变成「无 inset」从而影响面板的 IME 行为？
   我们把 `SOFT_INPUT_ADJUST_NOTHING` 保留即可吗？
3. §4 的补边修复（padding + clipToPadding）在 API 26–34（非 edge-to-edge、content 不含系统栏）上
   会不会**反向**多补一圈？（我的算法里 `padding=0` 时退化成本来行为，请复核这个退化是否严密。）
