# 只用一台手机，把 APK 发到 GitHub —— 完整操作手册

> 适用：没有任何电脑、只有 Android 手机 + 浏览器。
> 目标：让别人能从 GitHub 下载安装你的 APK；你自己以后每次发版只需 3 步。

---

## 0. 你要发布的文件

| 文件 | 大小 | 用途 | 放哪 |
|---|---|---|---|
| `AnimWidget-v1.10.355.apk` | 6,614,600 字节 | 安装包本体 | **Release 附件**（不要放进代码仓库） |
| `AnimWidget-signing.keystore` | 2,666 字节 | **签名密钥，丢了就再也无法覆盖升级** | **私有仓库 / 自己手机备份**，绝不能公开 |

SHA-256（给用户核对用）：
```
298a325bcd2b6bac454436aaf954b5d5e6282708dcb46b8cb45bb4739dd9d389
```
证书 SHA-256（以后核对是否同一把钥匙签名）：
```
5E:9B:78:5C:93:2E:49:AD:62:1E:BE:FB:31:FB:14:66:59:2F:A9:62:88:29:55:A6:9C:EB:89:5D:04:EE:F4:D3
```

---

## 1. 先把两个文件存到手机（5 分钟）

在 Kelivo 的对话里，点这两个文件：

- `kelivo://chat/outputs/AnimWidget-v1.10.355.apk`
- `kelivo://chat/outputs/AnimWidget-signing.keystore`

点开后选「保存」或「下载」。**一定要保存到「下载 / Download」目录**，不要留在应用私有目录 ——
GitHub 上传时的文件选择器进不去 `Android/data/...`。

**验证是否存好了**：打开手机自带的「文件管理」→「下载」，应该能看到这两个文件。
APK 应该显示 6.31 MB（6,614,600 字节）。

> 如果 Kelivo 只给「分享」不给「保存」：分享 → 选「文件管理」或「保存到文件」→ 选「下载」。

---

## 2. GitHub 账号（已有就跳过）

手机浏览器打开 **https://github.com**（用 **Chrome**，不要用微信/QQ 内置浏览器，它们打不开）。

点 **Sign up**，用邮箱注册。记住：
- 用户名会成为下载地址的一部分，例如 `https://github.com/你的用户名/animwidget`
- 邮箱要能收到验证码

注册完先别急着建仓库，把这些设置做完：

1. 右上角头像 → **Settings** → 左边最下面 **Developer settings** → **Personal access tokens** → **Tokens (classic)**
   （这一步可以先不做，网页上传不需要。只在以后想用命令行时才要。）

---

## 3. 建仓库（2 分钟）

1. 浏览器打开 **https://github.com/new**
2. **Repository name**：`animwidget`
3. **Description**：`把视频/动图/图片做成安卓桌面动态组件`
4. 选 **Public**（公开）
5. **勾上** `Add a README file`
6. 点绿色 **Create repository**

建好后浏览器地址是：
```
https://github.com/你的用户名/animwidget
```

> **手机浏览器必做**：Chrome 菜单（右上角三个点）→ 勾选 **「桌面版网站」**。
> 不勾的话，GitHub 的部分按钮会藏起来或排版错乱。

---

## 4. 写 README（复制粘贴，10 分钟）

在仓库首页，README 那一块右上角有个**铅笔图标 ✏️**，点它。
把下面整段**全选替换**粘贴进去，把 `你的用户名` 改成你自己的，然后点 **Commit changes**。

```markdown
# 帧工坊 / FrameSmith

把**视频 / 动图 / 图片**做成安卓桌面的动态组件。零第三方依赖，不联网，不收集任何数据。

## 下载

到 [Releases](../../releases/latest) 下载最新的 `.apk` 安装。

| 版本 | 大小 | SHA-256（下载后可核对） |
|---|---|---|
| v1.10.355 | 6,614,600 字节 | `298a325bcd2b6bac454436aaf954b5d5e6282708dcb46b8cb45bb4739dd9d389` |

## 安装

1. 下载 APK
2. 点开安装。系统提示「未知来源」时，允许当前浏览器/文件管理器安装应用
3. **vivo / iQOO 用户**：如果提示「应用来源未知，已拦截」，去
   `设置 → 安全 → 更多安全设置 → 安装未知应用`，给浏览器打开权限；
   如果开了「纯净模式」，需要先关闭（`设置 → 安全 → 纯净模式`）

**系统要求**：Android 8.0（API 26）及以上。

## 功能

- 视频 ≤1080p / ≤50fps / ≤40s，动图与静态图同样支持
- 48 种形状、阴影曲线、边框、旋转、偏移
- 「1:1」档：预览 = 桌面 = 原清晰度 / 原帧率 / 原时长
- 桌面组件**拖动改尺寸**后自动按新尺寸重新适配，一像素都不裁

## 权限说明（逐条，都是必要的）

| 权限 | 用途 | 能不能拒绝 |
|---|---|---|
| `FOREGROUND_SERVICE` / `..._SPECIAL_USE` | 桌面组件逐帧投递服务，只在桌面可见时运行 | 拒绝则无法播放 |
| `POST_NOTIFICATIONS` | 显示「正在播放」通知，可随时点停止 | 可拒绝，但播放状态不可见 |
| `SYSTEM_ALERT_WINDOW` | 仅在「调整位置」功能里，用一个可拖动的浮层调组件位置 | **可拒绝**，不影响正常播放 |
| `PACKAGE_USAGE_STATS` | 判断「桌面是否在前台」，不在桌面时停止播放以省电 | **可拒绝**，会退化成一直播放（更耗电） |
| `READ_MEDIA_IMAGES` | 选择图片素材时预览相册 | 可拒绝，改用文件选择器 |
| `moe.shizuku.manager.permission.API_V23` | **可选**。装了 Shizuku 时用于更高精度的桌面刷新；没装也能正常用 | 可拒绝 |

**本应用不联网、不上传任何数据、没有广告、没有统计 SDK。**
所有素材与帧缓存都在应用私有目录，卸载即删除。

## 已知限制

- 「50fps + 1:1 清晰度」目前主要在 vivo（`com.bbk.launcher2`）上验证过；
  其它品牌桌面可能退化成 30fps 或 URI 通道（会自适应，不会崩）
- 拖动组件尺寸后，过渡期约 1~2 秒会先播「上一代分辨率」再切到原生，属于设计行为
- 高帧率会更耗电，桌面不可见时会自动停

## 版本历史

- **v1.10.355** (versionCode 470)：移除 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 权限（改为跳系统电池设置页）
- **v1.10.354** (469)：过渡期改用「上一代帧 + 一次绘制」，把过渡期合成从 26ms 压到约 18ms
- **v1.10.353** (468)：修过渡期回退会「跳到未来帧」；过渡期不再叠加第二层阴影
- **v1.10.352** (467)：修 `minBmpW` 多除 density 导致的「卡片被量小一圈」
- **v1.10.351** (466)：过渡期改为现渲染直推，不再顶帧

## 反馈

到 [Issues](../../issues) 提，请附上：手机型号、系统版本、桌面名称、以及应用内
`设置 → 诊断日志 → 导出` 生成的那份文件。
```

---

## 5. 发 Release（最关键，5 分钟）

1. 浏览器打开 `https://github.com/你的用户名/animwidget/releases/new`
2. **Choose a tag** 输入框里写：`v1.10.355`，然后点下面出现的 **Create new tag: v1.10.355 on publish**
3. **Release title** 写：`v1.10.355`
4. **Describe this release** 里贴：

```
- 移除 REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 权限（改为跳系统电池设置页）
- SHA-256: 298a325bcd2b6bac454436aaf954b5d5e6282708dcb46b8cb45bb4739dd9d389
- 大小: 6,614,600 字节
- 支持 Android 8.0+
```

5. 页面下方 **Attach binaries by dropping them here or selecting them**
   —— 点 **selecting them**
6. 系统会弹出文件选择器：
   - 如果默认停在「图片/视频」，点左上角菜单切到 **「文件」**
   - 进入 **下载 / Download**
   - 选中 `AnimWidget-v1.10.355.apk`
7. **等文件名出现在页面上、上传进度走完**
8. 点绿色 **Publish release**

发布后地址是：
```
https://github.com/你的用户名/animwidget/releases/latest
```
把这个链接发给别人即可（`/latest` 会自动指向最新版）。

---

## 6. 以后每次更新（3 步，2 分钟）

1. 我这边出新 APK → 你保存到「下载」
2. 打开 `https://github.com/你的用户名/animwidget/releases/new`
3. tag 填新版本号（如 `v1.10.356`）→ 标题同 → 描述贴 SHA-256 和大小 → 上传 APK → Publish

**不需要动代码仓库，不需要碰 README**（除非想更新版本历史表）。

---

## 7. 手机上一定会遇到的 8 个坑

| # | 现象 | 原因 | 怎么解决 |
|---|---|---|---|
| 1 | GitHub 页面按钮找不到 / 排版错乱 | 手机版网页简化了 | Chrome 菜单 → **桌面版网站** |
| 2 | 上传时文件选择器里看不到 APK | APK 在应用私有目录 | 先「下载」到 `Download/`，再从「文件」里选 |
| 3 | 点上传没反应 | 上传中，进度条很小 | 等 10~30 秒，别重复点 |
| 4 | Publish 后附件是空的 | 没等上传完就点了 Publish | 回 Release 页面 → 右上 Edit → 重新上传 |
| 5 | 别人下载 APK 提示「有风险，已阻止」 | Chrome 对未知来源 APK 的警告 | 点「仍然下载」；这是正常警告 |
| 6 | 安装时提示「应用来源未知」 | Android 默认禁止侧载 | `设置 → 安全 → 安装未知应用`，给对应应用开权限 |
| 7 | **vivo 提示「纯净模式已拦截」** | vivo 的纯净模式只允许商店安装 | `设置 → 安全 → 纯净模式` 关闭；或把安装器加入白名单 |
| 8 | 微信里点链接打不开 GitHub | 微信屏蔽外链 | 复制链接到 Chrome 打开 |

---

## 8. 签名密钥：这是唯一「丢了就完蛋」的东西

你现在的 APK 用 `AnimWidget-signing.keystore` 签名（别名 `androiddebugkey`，口令 `android`，
有效期到 **2054-03-07**）。

**为什么重要**：Android 只允许「同包名 + 同签名」覆盖安装。
换了钥匙，老用户必须先卸载再装 —— 所有组件配置都会丢。

**备份方案（手机可做，二选一）**：

- **方案 A（推荐）**：新建一个 **Private** 仓库（`https://github.com/new` → 选 Private），
  把 `AnimWidget-signing.keystore` 上传进去（Add file → Upload files）。
  私有仓库别人看不到，等于免费保险箱。
- **方案 B**：把文件发到你自己的邮箱 / 存到网盘，标题写「帧工坊签名密钥 勿删」。

**绝对不要**把 keystore 放进公开仓库 —— 任何人都能拿它签一个同包名的假冒 APK 发出去。

---

## 9. 不要做的 4 件事

1. **不要把 APK 直接提交进代码仓库**（`Add file → Upload files` 传 APK）。
   每发一版仓库就多 6.6MB，几十版之后 clone 会非常慢，而且 GitHub 单文件超 100MB 会直接拒收。
   —— 用 Release 附件，那是专门放二进制的地方。
2. **不要动 `.gitignore` 以外的东西**。你只需要 README + Releases。
3. **不要为了「看起来正式」去填假的应用商店信息**。GitHub 分发不需要。
4. **不要改包名**（`io.github.animwidget`）。改了就是另一个应用，老用户无法升级。

---

## 10. 可选：让下载页更好看

在仓库里新建一个 `index.html`，然后 Settings → Pages → Source 选 `main` 分支 → Save，
几分钟后 `https://你的用户名.github.io/animwidget/` 就能访问。

**不建议现在做**：你只有一台手机，维护两套页面只会增加出错面。
先用 Releases 跑通，等有 100 个用户了再说。

---

## 附：发布检查清单（打印/截图存手机）

```
[ ] APK 已保存到 下载/
[ ] keystore 已备份到私有仓库或邮箱
[ ] 仓库已建（Public + README）
[ ] README 已粘贴并 Commit
[ ] Release tag = v1.10.355
[ ] Release 描述里写了 SHA-256 和大小
[ ] APK 已作为附件上传（不是提交进仓库）
[ ] 点过 Publish release
[ ] 用另一台设备/浏览器打开 /releases/latest 试下载
[ ] 下载后核对 SHA-256 一致
```
