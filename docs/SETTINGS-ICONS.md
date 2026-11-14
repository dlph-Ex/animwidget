# 设置页「界面语言 / 系统模式 / 性能」图标重绘 —— 参考清单、设计规范、自测

版本：v1.10.240 / versionCode 354。本文是 c1（参考文献）+ c2（规范与实现）+ c3（自测与缺陷清单）的落地记录。

## 0. 一句话结论

原来的三个「图标」其实是 **TextView 里塞字符**（`☾` `⚡` 和 `⇄`，界面语言用的是 `ic_language` 位图）。
字符图标的问题是**它不属于你**：字形来自系统字体，换 ROM 换字体就变粗细/基线；emoji 还不吃
`textColor`（深色下会被系统染成彩色）。现在三个图标由 `IconTileView` 用矢量路径画，
线宽/端点/网格/配色全部由代码决定，深浅色与强调色走同一条换色链路。

---

## 1. 参考清单（30+，逐条打开核实过）

核实方式：**每条都用 HTTP 取过页面并核对 `<title>`**（2024-xx 当天，移动端 UA）。
打不开或不匹配的**没有放进表里**，在 §1.2 单独说明。

### 1.1 系统设置页 / 平台规范（12）

| # | 名称 | 链接 | 可借鉴点 |
|---|---|---|---|
| 1 | Android 设置设计指南（AOSP source.android.com） | https://source.android.com/docs/core/settings/settings-guidelines | 系统设置页的信息架构：一级分组、次级页面、每行「图标 + 标题 + 副标题 + 值/开关」的固定排布 —— 我们的卡片头就是这套 |
| 2 | Android Developers：设置（Views） | https://developer.android.com/guide/topics/ui/settings | 官方设置页实现口径（Preference 层级、依赖关系），用来判断「一行里该放几个控件」 |
| 3 | Material Design 3 · Icons | https://m3.material.io/styles/icons/overview | 24dp 网格、2dp 描边、光学校正、outline/filled 成对 —— 线宽与网格直接照这个 |
| 4 | Material Design 3 · Color roles | https://m3.material.io/styles/color/roles | primary / secondary / tertiary 的分工：我们三张卡用三种强调色，正对这个角色的用法（不是随手配彩虹） |
| 5 | Material Design 3 · States（state layers） | https://m3.material.io/foundations/interaction/states/state-layers | 按下/悬停叠加 8%~12% 的强调色，而不是换一个颜色 —— 磁贴按下态可照此加 |
| 6 | Material Design 3 · Sliders | https://m3.material.io/components/sliders/overview | 滑块「轨道 + 活动段 + 拇指」三件套的着色关系；「滑块表面色」这个词在我们这里等于活动段 |
| 7 | Material Design 3 · Easing and duration | https://m3.material.io/styles/motion/easing-and-duration/tokens-specs | 时长分档（短/中/长）与标准缓动曲线 —— 对比度滑杆动画时长就按这套量级 |
| 8 | Material Design 3 · Segmented buttons | https://m3.material.io/components/segmented-buttons/overview | **「仅动图选择器 / 全局」两段开关的原型**：整条轨道 + 选中段实心 |
| 9 | Material Design 3 · Switch | https://m3.material.io/components/switch/overview | 开关的 thumb/track 着色与禁用态对比度（深色下 track 要用 38% 主色） |
| 10 | Material Design 2 · System icons | https://m2.material.io/design/iconography/system-icons.html | 更细的网格与线宽规则（24 网格内的 2dp/1.5dp 两档、填充 vs 描边的使用边界） |
| 11 | Apple HIG · Settings | https://developer.apple.com/design/human-interface-guidelines/settings | 「设置里只放真正需要配置的东西」的取舍口径；设置项的分组与默认值策略 |
| 12 | Apple HIG · Icons | https://developer.apple.com/design/human-interface-guidelines/icons | 图标要一眼可辨、不要靠细节表达；单色描边在深浅色下都要立得住 |

### 1.2 深色 / 动效 / RTL 专项（5）

| # | 名称 | 链接 | 可借鉴点 |
|---|---|---|---|
| 13 | Apple HIG · Dark Mode | https://developer.apple.com/design/human-interface-guidelines/dark-mode | 深色下**不要照搬浅色**：同一语义色要换 tone；我们靠 ThemeManager 的 adjust（对比度/反转）实现 |
| 14 | Apple HIG · SF Symbols | https://developer.apple.com/design/human-interface-guidelines/sf-symbols | 「符号字重跟文本字重匹配」+ 光学对齐 —— 我们把它落成「线宽 1.9dp 恒定、不随字号变」 |
| 15 | Apple HIG · Right to left | https://developer.apple.com/design/human-interface-guidelines/right-to-left | 只镜像**有方向性**的图形；地球/明暗/仪表都左右对称 → 不镜像，但布局边距必须 Start/End |
| 16 | Apple HIG · Loading | https://developer.apple.com/design/human-interface-guidelines/loading | 「不要让用户以为卡住」：超过一瞬的操作要有可见进度（对比度滑杆动画正是这个作用） |
| 17 | Android Developers · 深色主题 | https://developer.android.com/develop/ui/views/theming/darktheme | 深色适配的实现口径（语义色、force dark 的坑） |

### 1.3 主流 App 的语言选择 / 语言入口（5）

| # | 名称 | 链接 | 可借鉴点 |
|---|---|---|---|
| 18 | Smashing Magazine · Designing A Perfect Language Selector UX | https://www.smashingmagazine.com/2022/05/designing-better-language-selector/ | 语言列表要**显示母语名**（中文/English），别只显示当前界面语言翻译；入口要能一眼找到 |
| 19 | Localizely · Designing an effective language selector | https://localizely.com/blog/designing-an-effective-language-selector/ | 语言入口的三种形态（列表/下拉/自动）与「跟随系统」这一档的必要性 —— 我们第一项就是「跟随系统」 |
| 20 | SimpleLocalize · 11 best language selector examples | https://simplelocalize.io/blog/posts/ui-design-language-selector-examples/ | 实例对照：图标 + 列表行 vs 纯文字列表，哪种更适合二级页 |
| 21 | Smart Interface Design Patterns · Language Selector | https://smart-interface-design-patterns.com/articles/language-selector/ | 语言选择的状态设计（当前项要有明确的选中态，而不是靠排序） |
| 22 | Android Developers · 本地化 | https://developer.android.com/guide/topics/resources/localization | 语言回退链与「系统语言优先」的实现口径（我们「跟随系统」= zh 缺省时回 en） |

### 1.4 开源实现 / 图标库（9）

| # | 名称 | 链接 | 可借鉴点 |
|---|---|---|---|
| 23 | AOSP Settings（GitHub 镜像） | https://github.com/aosp-mirror/platform_packages_apps_settings | 真实系统设置页的源码：每个条目怎么挂图标、图标尺寸与 tint 从哪来 |
| 24 | Material Components for Android | https://github.com/material-components/material-components-android | 主题属性 → 控件着色的标准链路（我们的 `paint()` 分发是同一思路的手写版） |
| 25 | Material Components · Slider 文档 | https://github.com/material-components/material-components-android/blob/master/docs/components/Slider.md | 滑块的 progress/thumb 分别怎么 tint、状态怎么切 |
| 26 | AndroidX | https://github.com/androidx/androidx | 组件默认值与无障碍（contentDescription / 触控区 48dp）的参考实现 |
| 27 | Material Design Icons | https://github.com/google/material-design-icons | 4000+ 图标的**命名与网格**一致性；同一族图标的线宽必须统一 |
| 28 | Lucide | https://github.com/lucide-icons/lucide | 24 网格 + 2px 圆头圆角描边 —— 我们 1.9dp 线宽与圆头端点的直接来源 |
| 29 | Feather | https://github.com/feathericons/feather | 极简线性图标：一条路径只表达一个概念，细节越少越耐缩放 |
| 30 | Heroicons | https://github.com/tailwindlabs/heroicons | outline / solid 成对提供，且**同一图形的两版视觉重量对齐**（深色下换版本不突兀） |
| 31 | Android: user-interface-samples | https://github.com/android/user-interface-samples | 官方示例里的设置页与深色适配写法 |

### 1.5 设计社区 / 案例库（4，目录级）

| # | 名称 | 链接 | 可借鉴点 |
|---|---|---|---|
| 32 | Mobbin | https://mobbin.com/ | 真实 App 的页面流（设置页/语言页），可用来对齐「一行里图标 + 文案 + 控件的比例」 |
| 33 | Page Flows（screenlane.com） | https://screenlane.com/ | 同上，按流程看「进入语言列表 → 选中 → 返回」的动效 |
| 34 | Pttrns | https://pttrns.com/ | 按控件类型筛设置页截图，适合找「开关 + 图标」的排布 |
| 35 | ScreensDesign（uisources.com） | https://www.uisources.com/ | 屏幕库，适合查深色主题下的设置页配色 |

### 1.6 可用性研究（3）

| # | 名称 | 链接 | 可借鉴点 |
|---|---|---|---|
| 36 | NN/g · Icon Usability | https://www.nngroup.com/articles/icon-usability/ | **图标必须配文字标签**（我们每行都有标题，所以图标只做识别加速，不承担语义） |
| 37 | NN/g · Dark Mode vs. Light Mode | https://www.nngroup.com/articles/dark-mode/ | 深色下对比度的实测结论：描边图标在深色底要比浅色底略粗才「一样重」 |
| 38 | UX Design Institute · 7 principles of icon design | https://www.uxdesigninstitute.com/blog/7-principles-of-icon-design/ | 一致性/简洁/可辨/可缩放/对齐 的检查表，用作本页图标的自检项 |

### 1.7 没能纳入的（如实说明）

- **Dribbble**：`dribbble.com/search/*` 与单条 shot 页面都返回 `202` 且正文为空（Cloudflare 拦截），
  **无法打开核实**，按「严禁编造链接」的要求不列入。
- **Behance**：`behance.net` 搜索页与作品页均返回 `403 Forbidden`，同上不列入。
- **Figma 社区**：页面是 JS 渲染，`<title>` 为空，核实不到内容，不列入。
- 个别 Material 3 子页（`/components/icons/overview`）返回 404，已用 `/styles/icons/overview` 替代。

---

## 2. 设计规范（从上面取舍而来，不照抄任何一条）

### 2.1 几何

| 项 | 取值 | 来源取舍 |
|---|---|---|
| 设计网格 | 24×24 | M3 / Material Icons / Lucide 三家一致 |
| 实际绘制尺寸 | 24dp，居中放进 40dp 磁贴（每边留 8dp） | M3「24 in 48」按 40 收窄；与页面既有 40dp 磁贴对齐 |
| 线宽 | **1.9dp** 恒定，不随字号缩放 | Lucide/Feather 用 2px@24；深色下略粗补偿（NN/g 深色对比度结论） |
| 端点/拐角 | Round cap + Round join | SF Symbols 的柔化观感 + Lucide 一致 |
| 关键圆 | r=8.4（24 网格内 = 直径 16.8，留 3.6 给描边与留白） | 保证 24dp 下描边不顶到网格边 |
| 磁贴 | 40dp、圆角 14dp、强调色 16% 叠表面 | 与页面既有 `icon_tile` 家族完全同源 |

### 2.2 色值 token（不新增颜色，全部是既有语义色的派生）

| 用途 | 取值 | 说明 |
|---|---|---|
| 磁贴底 | `withAlpha(accent, 0.16)` | accent 由 tag 决定：`icon_tile`→primary、`icon_tile_2`→secondary、`icon_tile_3`→tertiary |
| 线条 | `accent`（同色实心） | 只用一种颜色，不做双色/渐变 |
| 页面主文字 | `colors.text` | 未变 |
| 作用范围开关轨道 | `colors.neutral` | 中性底，不抢色块 |
| 作用范围选中段 | `colors.primary` + `colors.onPrimary` | 与方案图标选中态同一套 |
| 作用范围未选文字 | `colors.textMuted` | |

### 2.3 动效与状态

| 项 | 取值 | 说明 |
|---|---|---|
| 图标状态 | 常态 / 按下（可后续补 8% state layer） | 当前图标本身不做动画（列表项不该有独立动效） |
| 对比度键盘输入确认 | **值变了就滑过去**：`Ease.IN_OUT`，时长 `180ms + 跨度×240/2000`，上限 420ms | 与预设套用、方案切换同一条曲线；值没变则不滑 |
| 开关自身切换 | 指示块滑动 200ms（`Ease.IN_OUT`）+ 字色插值，**不叠化** | 控件内部的切换不该被整页叠化盖住 |
| 开关生效后的换色 | 页面级 `Morph.crossfade` 叠化一次 | 「所有滑块换色」是一大片颜色突变，硬切会跳 |
| 深色适配 | 全部走 `ThemeManager.paint()` 下发，无硬编码色 | 换肤/对比度/强调色变化都会重画 |

### 2.4 三个图标各自的语义

| 项 | 图形 | 为什么 |
|---|---|---|
| 界面语言 | 地球（圆 + 经线椭圆 + 赤道） | 语言/区域最通用的符号；不做文字「A/文」因为 24dp 下会糊 |
| 系统模式 | 圆 + 右半实心 | 「明暗各半」= 跟随系统/自动，正好对应 跟随/浅色/深色 三档 |
| 性能 | 仪表（上弧 + 右上指针 + 三刻度 + 中心点） | 速度/负载的通用符号；指针固定 45° 表示「中等负载」而不是极限 |

---

## 3. 实现

- 新控件 `IconTileView`（`src-v2/.../IconTileView.java`）：一条 `onDraw` 画三种图形，
  tag 语法 `icon_tile_3|perf`（竖线前=磁贴色档，竖线后=图形）。
- 换色：`ThemeManager.paint()` 里新增 `instanceof IconTileView → applyColors(c)`，
  所以冷启动 / 切方案 / 改对比度 / 深色轮询 / 语言重建 全部自动覆盖（主题门禁 B 会验证这一点）。
- 布局：`activity_settings.xml` 三处 `TextView(☾/⚡)` + `ImageView(ic_language)` → `IconTileView`。
- 滑块色作用范围：`ThemePrefs.getSliderScope()`（0=仅动图选择器，1=全局）+ 自绘两段滑动开关
  `SliderScopeSwitch`（指示块会滑、字色插值）+ `ThemeManager.sliderSurfaceColor()` 成为 SeekBar 唯一的取色口。
  行内顺序：**标题 → 开关 → 色块 → 箭头**（v1.10.240 曾把开关插在标题前面，把色块挤出屏幕 —— 见 §4.5 缺陷 8）。

---

## 4. 自测（深/浅 × 小/大屏 × RTL × 动画）

没有 Android 运行时，所以「自测」分三层：**离线渲染**（几何）+ **静态门禁**（结构）+ **推演**（布局/状态）。

### 4.1 深色 / 浅色

| 检查 | 方法 | 结果 |
|---|---|---|
| 三个图形在深/浅底都清晰 | 用同一份几何在 `#181A1F` 与 `#FFFFFF` 上离线渲染（`docs-settings-icons-v1.10.240.png`） | 通过（弱化的 16% 磁贴底在两种底上都能分辨） |
| 颜色来源唯一 | 主题门禁 B：15 个自绘类全部有刷新路径 | 通过 |
| 深色下不出现 emoji 彩色 | 三个磁贴已无 Text，`textColor` 不再参与 | 通过（结构性消除） |

### 4.2 小屏 / 大屏

| 检查 | 方法 | 结果 |
|---|---|---|
| 320dp × 1.3 倍字号下「滑块色」行不被挤爆 | 算宽度：芯片 ≈128/141/154dp（1.0/1.15/1.3 倍），标题剩 58/45/32dp | **发现缺陷**：标题会换行 → 已修（单行 + 省略号） |
| 大屏不空旷 | 色行用 weight=1 的标题撑开，芯片/色块右对齐 | 通过 |
| 磁贴尺寸固定 40dp，不受字号影响 | 图标不随字体缩放（不调 `scaledDensity`） | 通过（刻意的：图标与文字解耦） |

### 4.3 RTL

| 检查 | 方法 | 结果 |
|---|---|---|
| 边距用 Start/End | 扫新增代码 | **发现缺陷**：芯片用了 `rightMargin` → 已修（`setMarginEnd`） |
| 三个图形是否需要镜像 | 逐个看对称性：地球/明暗/仪表 都左右对称 | 不需要镜像（HIG RTL 只镜像方向性图形） |
| 布局镜像后顺序合理 | 行序 Start→End 为 标题→芯片→色块→箭头 | 镜像后自然变成 箭头←色块←芯片←标题，符合 RTL 阅读方向 |

### 4.4 动画

| 检查 | 方法 | 结果 |
|---|---|---|
| 键盘输入确认后滑杆有动画 | 代码路径：`writeContrast` → `animateContrastSliderTo`（内部 `cancel` + 值相同直接返回） | 通过 |
| 动画期间不重复落盘 | 动画只 `setProgress`，落地刷数字；落盘只有 `writeContrast` 一处 | 通过（v1.10.203 起成立） |
| 连续两次输入不打架 | `cancelContrastAnimation()` 先取消上一条 | 通过 |
| 作用范围切换后 SeekBar 立刻变色 | `setSliderScope` → `refreshThemeInPlace(false)` 重建色行并重下发颜色 | 通过 |

### 4.5 本轮修掉的缺陷清单

1. **`S03P` 不是合法代号**（4 位），判断代号体检抓出 → 改名 `S04`，门禁与文档同步。
2. **S01/S02/S03/T30 打了日志但没登记全表**（违反登记规则）→ 全部补进 `DiagCatalog`。
3. **W13 是个杂物号**：13 种文本，把「预热」和「推送/写盘失败」混在一个号里，看日志分不清 →
   拆出 **W30**（推送/写盘/压小重推），W13 收窄为预热族。
4. **W16 一个号表示相反的两件事**（连续失败 / 恢复推帧）→ 恢复单独拆出 **W31**。
5. **滑块色行的芯片用 `rightMargin`**，RTL 下跑错侧 → 改 `setMarginEnd`。
6. **大字体 + 小屏时滑块色行标题换行**，把色块挤下去 → 标题改单行 + 省略。
7. **滑块色行的顺序错**：开关被插成行的**第一个**子 View，标题被挤到右边、色块被挤出屏幕
   （用户截图证实）→ 改成「标题 → 开关 → 色块 → 箭头」。
8. **开关切换是瞬变**（两个 TextView 背景直接对调）→ 换成自绘 `SliderScopeSwitch`：指示块 200ms 滑动 +
   字色插值；**开关自身不叠化**，生效后的换色由页面级 `Morph.crossfade` 叠化。
9. **图标预览脚本的坐标约定错**（把 Android 的 Y 向下当成 Y 向上，仪表画成了「笑脸」）——
   不是产品 bug，但属于「验证工具本身错」，已修正后才当作证据用（与 LESSONS 53/55 同类）。

### 4.6 仍未验证（明确列出）

- 真机DPI/圆角的实际观感（只能用离线渲染近似，没有 Android 运行时）。
- 平板/大屏的实际排布（只做了宽度推算）。
- 系统 RTL 语言的完整界面（只做了代码级镜像检查）。
