#!/usr/bin/env python3
"""把 aapt 产出的资源 APK + classes.dex 重新打包成「对齐」的未签名 APK。

为什么需要这一步：
- Android 11+ 要求 resources.arsc 不能压缩（targetSdk R+）。
- resources.arsc / classes.dex 这类未压缩条目最好按 4 字节对齐。
aapt1（Debian 的 aapt）不保证这两点，所以这里手写 zip 结构，
给需要 4 字节对齐的 STORED 条目补一个 0xD935 的 padding extra field。
"""
import struct
import sys
import zipfile
import zlib

ALIGN = 4
EXTRA_ID = 0xD935


def pack(out_path: str, res_apk: str, dex_path: str) -> None:
    entries = []
    with zipfile.ZipFile(res_apk) as zin:
        for info in zin.infolist():
            entries.append(
                dict(
                    name=info.filename,
                    data=zin.read(info.filename),
                    method=info.compress_type,
                    date_time=info.date_time,
                    external_attr=info.external_attr,
                )
            )

    with open(dex_path, "rb") as fh:
        dex = fh.read()
    entries.append(
        dict(
            name="classes.dex",
            data=dex,
            method=zipfile.ZIP_STORED,
            date_time=(2024, 1, 1, 0, 0, 0),
            external_attr=0o644 << 16,
        )
    )

    for entry in entries:
        if entry["name"] in ("resources.arsc", "classes.dex"):
            entry["method"] = zipfile.ZIP_STORED

    def dos_time(date_time):
        year, month, day, hour, minute, second = date_time
        return (
            (hour << 11) | (minute << 5) | (second // 2),
            ((year - 1980) << 9) | (month << 5) | day,
        )

    central = []
    with open(out_path, "wb") as out:
        for entry in entries:
            name = entry["name"].encode("utf-8")
            offset = out.tell()
            base = offset + 30 + len(name)

            extra = b""
            if entry["method"] == zipfile.ZIP_STORED and base % ALIGN:
                pad = (-(base + 4)) % ALIGN
                extra = struct.pack("<HH", EXTRA_ID, pad) + b"\x00" * pad

            data = entry["data"]
            if entry["method"] == zipfile.ZIP_DEFLATED:
                # ZIP 需要 raw deflate（wbits=-15），zlib.compress 会带 zlib 头
                compressor = zlib.compressobj(9, zlib.DEFLATED, -15)
                payload = compressor.compress(data) + compressor.flush()
            else:
                payload = data
            crc = zlib.crc32(data) & 0xFFFFFFFF
            time_field, date_field = dos_time(entry["date_time"])

            out.write(
                struct.pack(
                    "<IHHHHHIIIHH",
                    0x04034B50,
                    20,
                    0,
                    entry["method"],
                    time_field,
                    date_field,
                    crc,
                    len(payload),
                    len(data),
                    len(name),
                    len(extra),
                )
            )
            out.write(name)
            out.write(extra)
            out.write(payload)

            central.append(
                dict(
                    name=name,
                    method=entry["method"],
                    time=time_field,
                    date=date_field,
                    crc=crc,
                    csize=len(payload),
                    usize=len(data),
                    attr=entry["external_attr"],
                    offset=offset,
                )
            )

        central_start = out.tell()
        for item in central:
            out.write(
                struct.pack(
                    "<IHHHHHHIIIHHHHHII",
                    0x02014B50,
                    20,
                    20,
                    0,
                    item["method"],
                    item["time"],
                    item["date"],
                    item["crc"],
                    item["csize"],
                    item["usize"],
                    len(item["name"]),
                    0,
                    0,
                    0,
                    0,
                    item["attr"],
                    item["offset"],
                )
            )
            out.write(item["name"])

        central_size = out.tell() - central_start
        out.write(
            struct.pack(
                "<IHHHHIIH",
                0x06054B50,
                0,
                0,
                len(central),
                len(central),
                central_size,
                central_start,
                0,
            )
        )


if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("usage: pack_apk.py <out.apk> <res.apk> <classes.dex>", file=sys.stderr)
        raise SystemExit(2)
    pack(sys.argv[1], sys.argv[2], sys.argv[3])
    print("packed:", sys.argv[1])
