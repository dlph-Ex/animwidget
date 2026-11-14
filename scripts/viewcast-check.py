#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""布局控件类型 vs Java 强转 的一致性检查（v1.10.241）。

## 为什么要有

v1.10.240 把设置页的 `img_language` 从 `ImageView` 换成了自绘的 `IconTileView`，
但 Java 里还留着 `((ImageView) findViewById(R.id.img_language))` —— 一点设置就
ClassCastException，用户连日志都拿不到。编译期查不出来（findViewById 返回泛型 <T>），
字节码校验也查不出来（类型是运行期才对的）。这类错只能靠**比对布局与代码**。

## 规则

1. 扫 `res/layout/*.xml`：`android:id="@id/X"` 或 `@+id/X` → 元素标签名（控件类）。
2. 扫 `src-v2/**/*.java`：`((T) findViewById(R.id.X))` / `((T) view.findViewById(R.id.X))`。
3. 判定：T 必须是「该控件类」或它的**父类**。父子关系来自：本工程自定义 View 的
   `extends` 链 + Android 常见控件继承表。
4. 布局里找不到该 id → 跳过（可能在别的 layout / 动态创建的）。
退出码 0 = 无不一致。
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src-v2", "io", "github", "animwidget")
RES = os.path.join(ROOT, "patch-v1.4.0", "res", "layout")

# Android 常用控件继承表（只写我们工程里出现过的与它们的关键祖先）
BASE = {
    "View": "Object",
    "TextView": "View",
    "ImageView": "View",
    "Button": "TextView",
    "EditText": "TextView",
    "Switch": "TextView",          # 老 API 里 Switch 是 Button 的子类
    "CheckBox": "TextView",
    "RadioButton": "TextView",
    "ProgressBar": "View",
    "SeekBar": "ProgressBar",
    "LinearLayout": "View",
    "FrameLayout": "View",
    "RelativeLayout": "View",
    "ScrollView": "FrameLayout",
    "RadioGroup": "LinearLayout",
    "ViewGroup": "View",
    "ViewStub": "View",
    "SurfaceView": "View",
    "TextureView": "View",
    "ImageButton": "ImageView",
}


def read(p):
    with open(p, encoding="utf-8", errors="replace") as f:
        return f.read()


def java_files():
    for root, _, files in os.walk(SRC):
        for f in files:
            if f.endswith(".java"):
                yield os.path.join(root, f)


# 1) 自定义 View 的父类
custom = {}
for p in java_files():
    text = read(p)
    for m in re.finditer(r"(?:public |final )*(?:final )?class (\w+)(?:<[^>]*>)? extends ([\w.]+)", text):
        name = m.group(1)
        parent = m.group(2).split(".")[-1]
        custom[name] = parent


def ancestors(cls):
    """往上的类名集合（含自身）。"""
    chain = []
    seen = set()
    cur = cls
    while cur and cur not in seen and len(chain) < 40:
        seen.add(cur)
        chain.append(cur)
        cur = custom.get(cur) or BASE.get(cur)
    return chain


# 2) 布局里的 id → 控件类
id2class = {}
for fn in sorted(os.listdir(RES)):
    if not fn.endswith(".xml"):
        continue
    text = read(os.path.join(RES, fn))
    for m in re.finditer(r"<([\w.]+)[^>]*?android:id=\"@\+?id/(\w+)\"", text, re.S):
        cls = m.group(1).split(".")[-1]
        id2class.setdefault(m.group(2), set()).add(cls)

# 3) Java 里的强转
bad = 0
checked = 0
for p in java_files():
    text = read(p)
    for m in re.finditer(r"\(\s*\(([\w.]+)\)\s*(?:[\w.()]+\.)?findViewById\(R\.id\.(\w+)\)\s*\)", text):
        cast = m.group(1).split(".")[-1]
        wid = m.group(2)
        if wid not in id2class:
            continue
        checked += 1
        line = text[: m.start()].count("\n") + 1
        for cls in id2class[wid]:
            if cast in ancestors(cls):
                continue
            bad += 1
            print("   · %s:%d  R.id.%s 在布局里是 %s，代码却强转成 %s（不是父类）"
                  % (os.path.basename(p), line, wid, cls, cast))

print("   比对强转=%d 处，不一致=%d 处" % (checked, bad))
sys.exit(1 if bad else 0)
