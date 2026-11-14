#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把 R.java 里的资源 id 与「当前资源表实际分配的 id」对齐（v1.10.244）。

## 为什么需要

`aapt2 link --stable-ids` 只保证**已知名字**保持原 id；**新增**的名字由 aapt2 重新分配，
而每次再新增一个资源，它的 id 还可能变（我们这几轮就遇到三次：slider_scope、dev_cap、color_editor_reset）。
R.java 是手写的，javac 会把 id 当**编译期常量内联**进 dex，所以必须与本次 link 的结果一致 ——
否则运行期找不到资源（Resources$NotFoundException）或拿到别的资源。

手工抄 id 已经抄错过两次，这个脚本把「link → dump → 改 R.java」串起来：

  1. aapt2 compile + link（与 build-apk-src.sh 同参数）；2. dump resources；3. 只改 R.java 里
  **已经存在**的条目；4. 报告「R.java 有、资源表没有」和「资源表有、R.java 没有」的两类不一致。

用法：python3 scripts/sync-r.py [--dry]
退出码 0 = 已对齐（或本来就是对齐的）。
"""
import os
import re
import subprocess
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TC = os.path.join(ROOT, "toolchain")
RESDIR = os.path.join(ROOT, "patch-v1.4.0")
RJAVA = os.path.join(ROOT, "src-v2", "io", "github", "animwidget", "R.java")
DRY = "--dry" in sys.argv
FILL = "--fill" in sys.argv


def run(cmd, cwd=None):
    p = subprocess.run(cmd, cwd=cwd, capture_output=True, text=True)
    if p.returncode != 0:
        print(p.stdout[-2000:])
        print(p.stderr[-2000:])
        raise SystemExit("命令失败: %s" % " ".join(cmd))
    return p.stdout


tmp = tempfile.mkdtemp()
reszip = os.path.join(tmp, "res.zip")
resapk = os.path.join(tmp, "res.apk")
run(["aapt2", "compile", "--dir", "res", "-o", reszip], cwd=RESDIR)
run(["aapt2", "link", "-o", resapk,
     "-I", os.path.join(TC, "android-all.jar"),
     "--manifest", "AndroidManifest.xml", "-R", reszip,
     "--stable-ids", "stable-ids.txt",
     "--min-sdk-version", "26", "--target-sdk-version", "35",
     "--version-code", "1", "--version-name", "0", "--auto-add-overlay"], cwd=RESDIR)
dump = run(["aapt2", "dump", "resources", resapk])

have = {}
for line in dump.splitlines():
    m = re.match(r"\s+resource (0x[0-9a-f]{8}) ([a-z_]+)/(\S+)\s*$", line)
    if m:
        have[(m.group(2), m.group(3))] = m.group(1)

src = open(RJAVA, encoding="utf-8").read()
changed = 0
missing_in_arsc = []
# 逐个 class 块处理，才能知道 type（R.string.xxx 的 type 就是 string）
new = src
for cls, body in re.findall(r"public static final class (\w+) \{(.*?)\n    \}", src, re.S):
    for name, value in re.findall(r"static final int (\w+) = (0x[0-9a-f]+);", body):
        key = (cls, name)
        real = have.get(key)
        if real is None:
            if cls not in ("style", "attr"):
                missing_in_arsc.append("%s/%s" % (cls, name))
            continue
        if real != value:
            changed += 1
            old_decl = "public static final int %s = %s;" % (name, value)
            new_decl = "public static final int %s = %s;" % (name, real)
            new = new.replace(old_decl, new_decl, 1)
            print("   %s/%s  %s → %s" % (cls, name, value, real))

unlisted = [k for k in have if k[0] not in ("style", "attr")
            and not re.search(r"static final int %s = " % re.escape(k[1]), src)]

print("   资源表条目=%d，本次改动=%d" % (len(have), changed))
if missing_in_arsc:
    print("   **R.java 有、资源表没有**（名字写错 / 资源被删）：%s" % " ".join(missing_in_arsc))
if unlisted:
    print("   **资源表有、R.java 没有**（新资源忘了登记）：%s"
          % " ".join("%s/%s" % k for k in sorted(unlisted)))
def insert_entry(text, cls, name, rid):
    """把一条 entry 插进对应 class 块（块不存在就新建一个）。"""
    line = "        public static final int %s = %s;\n" % (name, rid)
    m = re.search(r"(public static final class %s \{(.*?)\n    \})" % re.escape(cls), text, re.S)
    if m:
        body = m.group(0)
        patched = body.replace("\n    }", "\n" + line + "    }", 1)
        return text.replace(body, patched, 1)
    block = "    public static final class %s {\n%s    }\n" % (cls, line)
    idx = text.rstrip().rfind("\n}")
    return text[:idx] + "\n" + block + text[idx:]

if FILL and unlisted:
    for cls, name in sorted(unlisted):
        new = insert_entry(new, cls, name, have[(cls, name)])
        print("   + %s/%s = %s" % (cls, name, have[(cls, name)]))
    open(RJAVA, "w", encoding="utf-8").write(new)
    print("   已补写 R.java")
elif not DRY and changed:
    open(RJAVA, "w", encoding="utf-8").write(new)
    print("   已写入 R.java")
sys.exit(1 if (missing_in_arsc or unlisted) else 0)
