#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""`wallpaper-align-check.py` 的姿态验证（LESSONS #223：守门人自己也要被验证）。

做法：把「门禁 + 它读的那 5 个文件」复制到临时目录，逐个制造**应该红**的改动，
确认门禁真的红在对应段落、以及原样时必须绿。凡是「打印了但什么都没查」的牌子，这里立刻暴露。

退出码 0 = 姿态全对（3 项正向 + N 项反向）。
"""
import os
import shutil
import subprocess
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src-v2", "io", "github", "animwidget")
FILES = [
    ("scripts/wallpaper-align-check.py", "scripts/wallpaper-align-check.py"),
    ("src-v2/io/github/animwidget/WallpaperPlacement.java", "src-v2/io/github/animwidget/WallpaperPlacement.java"),
    ("src-v2/io/github/animwidget/WallpaperBackdropView.java", "src-v2/io/github/animwidget/WallpaperBackdropView.java"),
    ("src-v2/io/github/animwidget/ConfigActivity.java", "src-v2/io/github/animwidget/ConfigActivity.java"),
    ("src-v2/io/github/animwidget/DiagCatalog.java", "src-v2/io/github/animwidget/DiagCatalog.java"),
    ("patch-v1.4.0/res/layout/activity_config.xml", "patch-v1.4.0/res/layout/activity_config.xml"),
]

ok = 0
bad = 0


def build(tree):
    for rel, dest in FILES:
        target = os.path.join(tree, dest)
        os.makedirs(os.path.dirname(target), exist_ok=True)
        shutil.copyfile(os.path.join(ROOT, rel), target)


def run(tree):
    script = os.path.join(tree, "scripts", "wallpaper-align-check.py")
    proc = subprocess.run([sys.executable, script], capture_output=True, text=True)
    return proc.returncode, (proc.stdout or "") + (proc.stderr or "")


def expect(name, mutate, want_red):
    global ok, bad
    tree = tempfile.mkdtemp(prefix="wpself-")
    try:
        build(tree)
        if mutate is not None:
            mutate(tree)
        code, out = run(tree)
        red = code != 0
        if red == want_red:
            ok += 1
            print("  OK  %s → %s" % (name, "红" if red else "绿"))
        else:
            bad += 1
            print("  FAIL %s → 期望%s，实际%s" % (name, "红" if want_red else "绿", "红" if red else "绿"))
            for line in out.strip().splitlines()[:6]:
                print("       " + line)
    finally:
        shutil.rmtree(tree, ignore_errors=True)


def patch(path, old, new):
    with open(path, encoding="utf-8", errors="replace") as handle:
        text = handle.read()
    if old not in text:
        raise SystemExit("自检脚本自己坏了：找不到要替换的片段\n" + old[:80])
    with open(path, "w", encoding="utf-8", errors="replace") as handle:
        handle.write(text.replace(old, new, 1))


def main():
    root = os.path.join(ROOT)

    def mutate_layout_scale_type(tree):
        patch(os.path.join(tree, "patch-v1.4.0/res/layout/activity_config.xml"),
              'android:id="@id/img_desk"',
              'android:id="@id/img_desk" android:scaleType="centerCrop"')

    def mutate_no_real_size(tree):
        patch(os.path.join(tree, "src-v2/io/github/animwidget/WallpaperBackdropView.java"),
              "display.getRealSize(point);", "display.getSize(point);")

    def mutate_no_id(tree):
        # 注意：要改的是**代码里的调用点**（.getWallpaperId(），不是注释里那个名字 ——
        # 上一版这里改到了注释，门禁保持绿色是正确行为（改注释当然不该红）。
        patch(os.path.join(tree, "src-v2/io/github/animwidget/WallpaperBackdropView.java"),
              ".getWallpaperId(", ".getWallpaperIdXX(")

    def mutate_keep_old_bitmap(tree):
        patch(os.path.join(tree, "src-v2/io/github/animwidget/WallpaperBackdropView.java"),
              "this.bitmap = null;\n            this.shownId = Integer.MIN_VALUE;",
              "this.shownId = Integer.MIN_VALUE;")

    def mutate_no_resume_hook(tree):
        patch(os.path.join(tree, "src-v2/io/github/animwidget/ConfigActivity.java"),
              "getWindow().getDecorView().post(this.deskWallpaperRefresh);",
              "// hook removed")

    def mutate_activity_reads_bitmap(tree):
        patch(os.path.join(tree, "src-v2/io/github/animwidget/ConfigActivity.java"),
              "backdrop.reload();",
              "backdrop.reload(); android.app.WallpaperManager.getInstance(this).getDrawable();")

    def mutate_main_thread_decode(tree):
        patch(os.path.join(tree, "src-v2/io/github/animwidget/WallpaperBackdropView.java"),
              "POOL.execute(", "POOL.submitNow(")

    def mutate_no_diag(tree):
        patch(os.path.join(tree, "src-v2/io/github/animwidget/DiagCatalog.java"),
              '{"W56"', '{"W57"')

    def mutate_android_import(tree):
        patch(os.path.join(tree, "src-v2/io/github/animwidget/WallpaperPlacement.java"),
              "public final class WallpaperPlacement {",
              "import android.graphics.Bitmap;\n\npublic final class WallpaperPlacement {")

    print("== 正向（原样必须绿）")
    expect("原始工程", None, False)

    print("== 反向（每一处「坏但语法合法」的改动都必须红）")
    expect("把 scaleType 挂回 img_desk", mutate_layout_scale_type, True)
    expect("删掉 getRealSize 回退（只留 getSize）", mutate_no_real_size, True)
    expect("不按壁纸代号判缓存", mutate_no_id, True)
    expect("换壁纸不清旧位图", mutate_keep_old_bitmap, True)
    expect("onResume 不重取壁纸", mutate_no_resume_hook, True)
    expect("Activity 又自己读壁纸位图", mutate_activity_reads_bitmap, True)
    expect("解码挪回主线程（不投递线程池）", mutate_main_thread_decode, True)
    expect("W56 没登记", mutate_no_diag, True)
    expect("摆法里混进 android 依赖", mutate_android_import, True)

    if bad == 0:
        print("WALLPAPERPLACE-SELFTEST OK（%d 个用例：1 正向 + %d 反向）" % (ok, ok - 1))
        return 0
    print("WALLPAPERPLACE-SELFTEST FAILED ok=%d bad=%d" % (ok, bad))
    return 1


if __name__ == "__main__":
    sys.exit(main())
