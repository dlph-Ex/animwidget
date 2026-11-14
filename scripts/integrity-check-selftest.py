#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""门禁 [33] 的**正反对拍**（v1.10.513）。

为什么要它：`integrity-check.py` 是"签名自检的守门人"，而守门人本身从没被验证过就等于没有
（LESSONS #3：v1.10.131 那个门禁 `bad=1` 还打印"结论 OK"）。这里用一份**临时假仓库**
（只拷几个相关文件 + 真钥匙库）逐个制造"应该红"的改动，确认门禁真的会红、且红在对的段落。

用法：python3 scripts/integrity-check-selftest.py        （由 verify-apk.sh 的 [33] 段调用）
退出码 0 = 全部用例符合预期。
"""
import os
import shutil
import subprocess
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src-v2", "io", "github", "animwidget")
FILES = ["Integrity.java", "App.java", "I18nActivity.java", "ConfigActivity.java",
         "DiagCatalog.java", "AboutSheet.java", "SettingsActivity.java",
         "WidgetProvider.java", "WidgetRender.java"]
KEYSTORE = os.path.join(ROOT, "build-manual", "debug.keystore")

FAILS = []


def build_sandbox(tmp):
    os.makedirs(os.path.join(tmp, "src-v2", "io", "github", "animwidget"))
    os.makedirs(os.path.join(tmp, "patch-v1.4.0"))
    os.makedirs(os.path.join(tmp, "scripts"))
    os.makedirs(os.path.join(tmp, "build-manual"))
    for name in FILES:
        shutil.copy(os.path.join(SRC, name),
                    os.path.join(tmp, "src-v2", "io", "github", "animwidget", name))
    shutil.copy(os.path.join(ROOT, "patch-v1.4.0", "AndroidManifest.xml"),
                os.path.join(tmp, "patch-v1.4.0", "AndroidManifest.xml"))
    shutil.copy(os.path.join(ROOT, "scripts", "integrity-check.py"),
                os.path.join(tmp, "scripts", "integrity-check.py"))
    if os.path.exists(KEYSTORE):
        shutil.copy(KEYSTORE, os.path.join(tmp, "build-manual", "debug.keystore"))


def gate(tmp, env_extra=None):
    env = dict(os.environ)
    env.pop("INTEGRITY_NO_KEYSTORE", None)
    if env_extra:
        env.update(env_extra)
    p = subprocess.run([sys.executable, os.path.join(tmp, "scripts", "integrity-check.py")],
                       capture_output=True, text=True, env=env)
    return p.returncode, p.stdout + p.stderr


def patch(tmp, name, old, new, count=1):
    """在临时仓库里做一处替换；找不到就当作用例本身写错（直接失败）。"""
    path = os.path.join(tmp, "src-v2", "io", "github", "animwidget", name)
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    if old not in text:
        raise AssertionError("用例锚点在 %s 里找不到：%r" % (name, old[:48]))
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(text.replace(old, new, count))


def expect(label, mutate, want, keyword=None, env_extra=None):
    """跑一个用例：want=0 表示应该通过；want=1 表示应该红（且红在 keyword 那段）。"""
    tmp = tempfile.mkdtemp(prefix="integrity-self-")
    try:
        build_sandbox(tmp)
        if mutate is not None:
            mutate(tmp)
        code, out = gate(tmp, env_extra)
        ok = (code == want)
        if ok and want == 1 and keyword and keyword not in out:
            ok = False
            out += "\n（期望命中 %s，实际输出里没有）" % keyword
        if not ok:
            FAILS.append("%s → 退出码 %d（期望 %d）\n%s" % (label, code, want, out.strip()))
        print("  %-46s %s" % (label, "OK" if ok else "FAIL"))
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def main():
    print("== [33] 自检门禁的正反对拍")
    print("-- 应该通过的（正向）")
    expect("基线（真钥匙库在场）", None, 0)
    expect("throw / 64 位十六进制只在注释里", lambda t: comments_only(t), 0)
    expect("没钥匙库 + INTEGRITY_NO_KEYSTORE=1", lambda t: drop_keystore(t), 0,
           env_extra={"INTEGRITY_NO_KEYSTORE": "1"})

    print("-- 应该红的（反向：判据/接线被改坏）")
    expect("指纹改一位", lambda t: patch(t, "Integrity.java", "0x9ceb895d", "0x9ceb895e"), 1, "[2]")
    # 注意：只把 if 条件改假是**测不出**来的（同一条 SDK_INT >= 28 在 compute() 里还有一处），
    # 所以这里直接删掉 28+ 分支的接线 —— 也就是"分支还在、里面什么都不做"那种坏法。
    expect("删掉 28+ 分支的接线", lambda t: patch(t, "Integrity.java",
             "return Api28.contentSigners(pm, PKG);", "return null;"), 1, "[1]")
    expect("删掉 26/27 取法", lambda t: patch(t, "Integrity.java",
             "PackageManager.GET_SIGNATURES", "PackageManager.GET_META_DATA"), 1, "[1]")
    expect("删掉多签名者判据", lambda t: patch(t, "Integrity.java",
             "if (si.hasMultipleSigners()) {", "if (false) {"), 1, "[3]")
    expect("删掉轮换历史判据", lambda t: patch(t, "Integrity.java",
             "si.getSigningCertificateHistory()", "si.getApkContentsSigners()"), 1, "[3]")
    expect("签名者数量判据改宽", lambda t: patch(t, "Integrity.java",
             "signers.length != 1", "signers.length > 9"), 1, "[3]")
    # 真正的坏法不是改常量名，而是把"读不到就放行"改成"读不到就限制"
    expect("26/27 读不到时被改成限制", lambda t: patch(t, "Integrity.java",
             ": UNREADABLE;", ": BAD_SIGNERS;", 2), 1, "[8]")
    expect("restricted() 改回 != OK", lambda t: patch(t, "Integrity.java",
             "return s == BAD_CERT || s == BAD_SIGNERS;", "return s != OK;"), 1, "[8]")
    expect("包名常量被改", lambda t: patch(t, "Integrity.java",
             'String PKG = "io.github.animwidget"', 'String PKG = "io.github.animwidget.copy"'), 1, "[4]")
    expect("清单丢了 android:name=\".App\"", lambda t: patch_manifest(t), 1, "[4]")
    expect("App 不再求值", lambda t: patch(t, "App.java",
             "int r = Integrity.evaluate(this);", "int r = 0;"), 1, "[5]")
    expect("I18nActivity 不再判定", lambda t: patch(t, "I18nActivity.java",
             "this.restrictedBuild = Integrity.restricted(this);", "this.restrictedBuild = false;"), 1, "[5]")
    expect("推帧出口闸门被删", lambda t: patch(t, "WidgetRender.java",
             "if (Integrity.restricted(context)) {\n            return 0L;\n        }", ""), 1, "[9]")
    expect("onUpdate 闸门被删", lambda t: patch(t, "WidgetProvider.java",
             "if (Integrity.restricted(context)) {\n            return;\n        }", ""), 1, "[9]")
    expect("App 里出现 throw", lambda t: patch(t, "App.java",
             "int r = Integrity.evaluate(this);",
             "if (r == 0) { throw new IllegalStateException(); }\n        int r = Integrity.evaluate(this);"),
           1, "[6]")
    expect("App 里写可读指纹", lambda t: patch(t, "App.java",
             "int r = Integrity.evaluate(this);",
             'String s = "5e9b785c932e49ad621ebefb31fb1466592fa962882955a69ceb895d04eef4d3";\n'
             '        int r = Integrity.evaluate(this);'), 1, "[2]")
    expect("K52 未登记", lambda t: drop_catalog(t, "K52"), 1, "[7]")
    expect("K54 未登记", lambda t: drop_catalog(t, "K54"), 1, "[8]")
    # 注意：不能只把 R.string.about_cert 改个名 —— 文件里还有 about_cert_copied，
    # 那一串也以 R.string.about_cert 开头，改名会命中错的那个（此处踩过一次）。
    expect("关于页证书行被删", lambda t: patch(t, "AboutSheet.java",
             "addCertRow(list, c, d);", ""), 1, "[10]")
    expect("设置页提示被删", lambda t: patch(t, "SettingsActivity.java",
             "R.string.integrity_hint", "R.string.settings_title"), 1, "[10]")
    expect("没钥匙库又没设开关", lambda t: drop_keystore(t), 1, "[2]")

    print()
    if FAILS:
        for f in FAILS:
            print("FAIL " + f.replace("\n", "\n     "))
        print("INTEGRITY-SELFTEST FAILED")
        return 1
    print("INTEGRITY-SELFTEST OK（23 个用例：3 正向 + 20 反向）")
    return 0


def drop_keystore(tmp):
    p = os.path.join(tmp, "build-manual", "debug.keystore")
    if os.path.exists(p):
        os.remove(p)


def comments_only(tmp):
    """只在注释里写 throw / 64 位十六进制：门禁必须**不**红（它只认代码行，LESSONS #176）。"""
    patch(tmp, "App.java", "public class App extends Application {",
          "public class App extends Application {\n"
          "    // throw new IllegalStateException();\n"
          "    // 5e9b785c932e49ad621ebefb31fb1466592fa962882955a69ceb895d04eef4d3\n"
          "    /* throw 5e9b785c932e49ad621ebefb31fb1466592fa962882955a69ceb895d04eef4d3 */")


def patch_manifest(tmp):
    p = os.path.join(tmp, "patch-v1.4.0", "AndroidManifest.xml")
    with open(p, encoding="utf-8") as fh:
        text = fh.read()
    if ' android:name=".App"' not in text:
        raise AssertionError("清单里没有 .App，用例锚点过时")
    with open(p, "w", encoding="utf-8") as fh:
        fh.write(text.replace(' android:name=".App"', "", 1))


def drop_catalog(tmp, code):
    p = os.path.join(tmp, "src-v2", "io", "github", "animwidget", "DiagCatalog.java")
    with open(p, encoding="utf-8") as fh:
        lines = fh.readlines()
    keep = [ln for ln in lines if not ln.lstrip().startswith('{"' + code + '"')]
    if len(keep) == len(lines):
        raise AssertionError("DiagCatalog 里没有 %s，用例锚点过时" % code)
    with open(p, "w", encoding="utf-8") as fh:
        fh.writelines(keep)


if __name__ == "__main__":
    sys.exit(main())
