#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""布局 id 引用体检（v1.10.392）。

## 为什么要有这道门禁

v1.10.392 重写主页版式时，一段 Python 字符串替换把「新建组件」那条色带整块删掉了 ——
布局里没有 `@id/btn_add`，而 MainActivity 里还有
`((View) findViewById(R.id.btn_add)).setOnClickListener(...)`。
这在**编译期完全看不出来**（findViewById 返回泛型 <T>，字节码校验也查不出），
真机上就是启动瞬间 `NullPointerException`，用户连主页都进不去。

已有的 [9] 强转门禁只比对「布局里有、代码里也强制转换」的那些 id，
**布局里根本没有** 的 id 它直接跳过 —— 正好是这种错最需要拦的地方。

## 规则

`R.id.X`（不含 `android.R.id.*`）只要在 Java 里出现过，就必须满足其一：
  1. 某个 `res/layout/*.xml` 里有 `@id/X` 或 `@+id/X`；
  2. 在下面的 ALLOW 白名单里，且写明「为什么布局里没有」。

退出码 0 = 通过；1 = 有 id 只存在于代码里（多半是布局被改坏了）。
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src-v2", "io", "github", "animwidget")
RES = os.path.join(ROOT, "patch-v1.4.0", "res", "layout")

# 布局里没有、但确实只由代码创建的 id —— 每条都要写清为什么。
ALLOW = {
    "ly0": "布局模式芯片：ids.xml 声明，控件在 ConfigActivity 里按 LayoutMode 动态建",
    "ly1": "同上",
    "ly2": "同上",
    "ly3": "同上",
    "ly4": "同上",
    "ly5": "同上",
    "ly6": "同上",
    "ly7": "同上",
}


def main():
    in_layout = set()
    for fn in sorted(os.listdir(RES)):
        if not fn.endswith(".xml"):
            continue
        with open(os.path.join(RES, fn), encoding="utf-8", errors="replace") as f:
            in_layout |= set(re.findall(r"@\+?id/(\w+)", f.read()))

    used = {}
    for fn in sorted(os.listdir(SRC)):
        if not fn.endswith(".java"):
            continue
        with open(os.path.join(SRC, fn), encoding="utf-8", errors="replace") as f:
            text = f.read()
        # 排除 android.R.id.*（那是系统 id，不在我们的资源表里）
        text = text.replace("android.R.id.", "__SYS__R.id.")
        for m in re.finditer(r"(?<!__SYS__)R\.id\.(\w+)", text):
            used.setdefault(m.group(1), fn)

    bad = sorted(k for k in used if k not in in_layout and k not in ALLOW)
    print("  布局 id=%d  Java 引用=%d  代码独有(白名单)=%d"
          % (len(in_layout), len(used), len(used.keys() & ALLOW.keys())))
    if bad:
        for k in bad:
            print("  MISSING @id/%s   （%s 里引用了它，但没有任何布局声明它）" % (k, used[k]))
        print("  说明：布局被改动后代码还在找这个 id —— 真机上是 findViewById 返回 null，")
        print("        下一步要么 NPE，要么静默不生效。")
        return 1
    print("  OK：代码引用的每个 id 在布局里都有")
    return 0


if __name__ == "__main__":
    sys.exit(main())
