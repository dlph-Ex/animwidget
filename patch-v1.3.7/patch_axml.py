#!/usr/bin/env python3
"""对已编译的资源包做二进制 AXML 补丁（不重新编译资源，资源 ID 全部保持不变）。

用法:
    python3 patch_axml.py <in_res.apk> <out_res.apk> [--code 65] [--name 1.3.2]

改动：
1. AndroidManifest.xml: versionCode / versionName
2. AndroidManifest.xml: 三个 Activity 的 android:configChanges 置 0（改为系统重建）
3. res/layout/activity_config.xml: 根 LinearLayout 的写死背景色 #fff7f8fa -> 透明
"""
import re
import struct
import sys
import zipfile

import axml


def write_int(b, attr_off, value):
    struct.pack_into('<I', b, attr_off + 16, value)


CONFIG_CHANGES = 'keep'  # keep | 0


def patch_manifest(data, code, name):
    b = bytearray(data)
    for elem, off, attrs in axml.walk(bytes(b)):
        for (aname, a, raw, dtype, dat, ans) in attrs:
            if elem == 'manifest' and aname == 'versionCode':
                write_int(b, a, code)
            if CONFIG_CHANGES != 'keep' and elem == 'activity' and aname == 'configChanges':
                write_int(b, a, int(CONFIG_CHANGES))
    raw = bytes(b)
    # versionName 字符串池：UTF-8 或 UTF-16LE，长度必须一致才能原地替换
    old = None
    pool = axml.parse(raw)[1]
    for s in pool:
        if re.fullmatch(r'\d+\.\d+\.\d+', s) and len(s) == len(name):
            old = s
            break
    if old is None or old == name:
        return bytes(b)
    if old.encode('utf-8') in raw:
        b = bytearray(raw.replace(old.encode('utf-8'), name.encode('utf-8')))
    else:
        b = bytearray(raw.replace(old.encode('utf-16-le'), name.encode('utf-16-le')))
    return bytes(b)


def patch_layout(data):
    b = bytearray(data)
    for elem, off, attrs in axml.walk(bytes(b)):
        if elem == 'LinearLayout':
            for (aname, a, raw, dtype, dat, ans) in attrs:
                if aname == 'background' and dat == 0xfff7f8fa:
                    write_int(b, a, 0)
            break
    return bytes(b)


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        return 2
    src, dst = sys.argv[1], sys.argv[2]
    code, name = 66, '1.3.3'
    args = sys.argv[3:]
    global CONFIG_CHANGES
    for i, a in enumerate(args):
        if a == '--code':
            code = int(args[i + 1])
        elif a == '--name':
            name = args[i + 1]
        elif a == '--config-changes':
            CONFIG_CHANGES = args[i + 1]

    zin = zipfile.ZipFile(src)
    zout = zipfile.ZipFile(dst, 'w', zipfile.ZIP_DEFLATED)
    for info in zin.infolist():
        data = zin.read(info.filename)
        if info.filename == 'AndroidManifest.xml':
            data = patch_manifest(data, code, name)
        elif info.filename == 'res/layout/activity_config.xml':
            data = patch_layout(data)
        zi = zipfile.ZipInfo(info.filename, date_time=info.date_time)
        zi.compress_type = (zipfile.ZIP_STORED
                            if info.compress_type == zipfile.ZIP_STORED
                            else zipfile.ZIP_DEFLATED)
        zi.external_attr = info.external_attr
        zout.writestr(zi, data)
    zout.close()
    print('patched res:', dst, 'versionCode=%d versionName=%s' % (code, name))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
