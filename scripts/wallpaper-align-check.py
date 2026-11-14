#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""壁纸底图对位的结构门禁（verify-apk.sh [34]，v1.10.519）。

## 为什么要有

v1.10.518 那个「预览里的壁纸整块上移 839px」的毛病，**不崩、不报、也没有任何日志**：
它只是把壁纸 centerCrop 进了预览控件自己那个 1260×738 的小框，于是裁剪窗口在位图里垂直居中
（从 `(bh-738)/2` 那一行开始画）。这种错只能靠「结构 + 数学」两头钉：

  · 数学：`tests/WallpaperPlacementTest.java` 逐条断言「视口顶端 → 目标顶端为 0」（门禁里 javac+java 跑）；
  · 结构：就是这个脚本 —— 一旦有人把写法搬回「ImageView + centerCrop」、把显示区尺寸的
    API 30 分支删掉（minSdk 26 会 NoSuchMethodError）、把解码挪回主线程、或者把
    「换壁纸先撤旧图」删掉，这里立刻红。

退出码 0 = 通过。
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src-v2", "io", "github", "animwidget")
LAYOUT = os.path.join(ROOT, "patch-v1.4.0", "res", "layout", "activity_config.xml")

problems = []
checks = 0


def read(path):
    with open(path, encoding="utf-8", errors="replace") as handle:
        return handle.read()


def strip_comments(text):
    """去掉 // 与 /* */ 注释。

    v1.10.519 自检当场抓到的教训（LESSONS #223 的同一类）：这些文件里有大段注释在**讨论**
    `getRealSize` / `getWallpaperId` / `deskWallpaperRefresh`，只查「名字还在不在」的判据
    会被注释喂饱 —— 把调用点删掉它照样绿。所以判据一律只看剥完注释的代码。
    """
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    text = re.sub(r"//[^\n]*", "", text)
    return text


def need(cond, message):
    global checks
    checks += 1
    if not cond:
        problems.append(message)


def body(source, signature):
    """粗切一个方法体：从签名那一行到「下一个缩进相同的 }」为止（够用，不做完整解析）。"""
    idx = source.find(signature)
    if idx < 0:
        return ""
    end = source.find("\n    }", idx)
    return source[idx:end if end > 0 else len(source)]


placement = read(os.path.join(SRC, "WallpaperPlacement.java"))
view_all = read(os.path.join(SRC, "WallpaperBackdropView.java"))
config_all = read(os.path.join(SRC, "ConfigActivity.java"))
layout = read(LAYOUT)
catalog_all = read(os.path.join(SRC, "DiagCatalog.java"))

view = strip_comments(view_all)
config = strip_comments(config_all)
placement = strip_comments(placement)
catalog = strip_comments(catalog_all)

# A. 布局：底图就是那个自绘 View，且**不许**再挂 scaleType（centerCrop 就是位移来源）
img_desk = re.search(r"<([\w.]+)[^>]*?@id/img_desk[^>]*?>", layout)
need(img_desk is not None, "布局里找不到 @id/img_desk")
tag = img_desk.group(1) if img_desk else ""
need(tag.endswith("WallpaperBackdropView"),
     "img_desk 在布局里不是 WallpaperBackdropView（现在=%s）—— 搬回 ImageView 就是退回 v1.10.518 的位移" % (tag or "缺失"))
need("scaleType" not in (img_desk.group(0) if img_desk else ""),
     "img_desk 上又出现了 scaleType —— centerCrop 以「预览控件自己」为框，壁纸会被垂直居中裁剪")

# B. 摆法必须是纯数学（没有 android 依赖才可能脱机对拍）
need("import android" not in placement,
     "WallpaperPlacement 里出现了 android 依赖 —— 摆法必须能脱机对拍，否则只能靠截图验")

# C. 两条显示区取法都在（minSdk 26：getMaximumWindowMetrics 是 API 30+）
need(re.search(r"\.getMaximumWindowMetrics\(", view) is not None,
     "WallpaperBackdropView 不再调用 getMaximumWindowMetrics（API 30+ 的整块显示区）")
need(re.search(r"\.getRealSize\(", view) is not None,
     "WallpaperBackdropView 缺 getRealSize 回退 —— minSdk 26 到 Android 10 会直接崩/拿到扣掉系统栏的尺寸")
need(re.search(r"SDK_INT\s*>=\s*30", view) is not None,
     "getMaximumWindowMetrics 的调用没有被 SDK_INT >= 30 圈住（26~29 上 NoSuchMethodError）")
need(re.search(r"\.getSize\(", view) is not None,
     "getRealSize 那条也没有 getSize/DisplayMetrics 兜底")

# D. 解码不在 UI 线程；取图只有一处
need("ExecutorService" in view and ".execute(" in view,
     "WallpaperBackdropView 里没有线程池 —— 整张壁纸（1260x2800 ARGB ≈ 14MB）不许在主线程解")
need(view.count("getDrawable()") == 1,
     "getDrawable() 不止一处（解码必须只有 loadBitmap 这一处，且跑在池线程里）")
need(re.search(r"POOL\.execute\(", view) is not None, "取图任务没有投到 pthread 池里")

# E. 换壁纸的判据是「代号」，不是「看文件时间」
# 冻结在 FLAG_SYSTEM 这一条上：v1.10.520 起类里还有第二条 getWallpaperId(FLAG_LOCK) 只用于诊断，
# 只查「有没有 getWallpaperId」会被它喂饱 —— 判缓存的必须**恰好**是系统那张。
need(re.search(r"\.getWallpaperId\(\s*WallpaperManager\.FLAG_SYSTEM", view) is not None,
     "WallpaperBackdropView 不再按 getWallpaperId(FLAG_SYSTEM) 判缓存 —— 换壁纸后会把旧图一直挂着")

# F. 换过壁纸要先撤旧图（判据 B：1 帧内不再显示旧壁纸）
reload = body(view, "public final void reload()")
need("this.bitmap = null" in reload,
     "reload() 里没有「先撤掉旧位图」—— 换壁纸后会有一次旧壁纸残留（判据 B 不允许）")
need("reload()" in reload or "reload" in reload, "reload() 结构异常")

# G. 接线：onResume 要真的 post 一次重取；Activity 里不再自己读壁纸
resume = body(config, "protected void onResume()")
need(re.search(r"post\(this\.deskWallpaperRefresh\)", resume) is not None,
     "ConfigActivity.onResume 没有真的重取壁纸底图 —— 用户换完壁纸回来看到的还是旧图")
need("getDrawable()" not in config,
     "ConfigActivity 里又直接读壁纸位图了 —— 取图/解码/对位必须只有 WallpaperBackdropView 一处")

# H. 旧写法不许回来
need("(ImageView) findViewById(R.id.img_desk)" not in config,
     "ConfigActivity 里又把 img_desk 当 ImageView 用了（视图已换成 WallpaperBackdropView）")
need('"W56"' in view, "WallpaperBackdropView 没有打 W56 —— 真机上「位图多大/显示区多大」就无从核对")
need('{"W56"' in catalog or '"W56"' in catalog, "DiagCatalog 里没有登记 W56")

if problems:
    for item in problems:
        print("   · " + item)
    sys.exit(1)

print("壁纸底图对位结构自检 OK（%d 项：整块显示区为框 / 两条显示区取法 / 后台解码 / 代号判缓存 / 换壁纸先撤旧图 / onResume 接线 / 旧写法未回潮 / W56 已登记）"
      % checks)
sys.exit(0)
