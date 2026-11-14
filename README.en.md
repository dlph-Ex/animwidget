# FrameSmith · User Guide

Turn videos, GIFs and still images into live widgets on your Android home screen. Each widget keeps its own media, frame rate, sharpness, mask shape, border and shadow.

**[中文](README.zh-CN.md)** ｜ Back to [entry page](README.md)

---

[Features](#features) · [Download](#download) · [Install](#install) · [Permissions](#permissions) · [Known limitations](#known-limitations) · [How it works](#how-it-works) · [Build](#build) · [Repository layout](#repository-layout) · [Feedback](#feedback) · [Sponsor](#sponsor) · [License](#license)

## Features

- **Media**: MP4, GIF, APNG, animated WebP and still images; the first frame previews as soon as you import. Video is hardware-decoded with BT.601 / 709 / 2020 colour conversion.
- **Per-widget settings**: frame rate 1–50 fps, sharpness 120–2560 px (8 px steps), quality 50–100, opacity, scale, content and mask rotation, offset, shadow curve, border.
- **35 mask shapes**: rect, rounded, squircle, circle, capsule, diamond, triangle, hexagon, octagon, stars, heart, teardrop, shield, cloud, bolt, gear, yin-yang and more; expand the picker to see every shape.
- **7 layouts**: original ratio, fill, 16:9, 4:3, 1:1, 9:16, 21:9.
- **Three quality tiers**: standard ~220k px over the bitmap channel; HD (long edge 1440, ~1.3M px); 1:1 at real card pixels, capped at 2.2M px, no upscaling on the home screen.
- **Refit on resize**: resize the widget on the home screen and it re-renders for the new cell instead of cropping.
- **When it plays**: playback starts only after the home screen has stayed visible for three seconds, and stops as soon as you leave the home screen or lock the device.
- **Look and language**: light and dark themes (follow the system or force one), colour schemes, contrast, inversion and font settings; the UI language follows the system or is pinned to Chinese or English.
- **Diagnostics**: logs keep the last 30 minutes (crashes included) and export as TXT or long images; the About sheet shows the version, signing certificate and an update check; the main screen can stop all playback at once.

## Download

| Version | Size | SHA-256 |
|---|---|---|
| v1.10.521 | 6,836,005 bytes (~6.5 MiB) | `1fcb8dd707b2552c750b038fff4b67cf08db64b1c81ba7f4b5f304b36435804d` |

- Direct download: <https://github.com/dlph-Ex/animwidget/releases/download/v1.10.521/AnimWidget-v1.10.521.apk>
- All releases: <https://github.com/dlph-Ex/animwidget/releases>

Requirements: Android 8.0+ (API 26), targetSdk 35.

Verify: run `sha256sum AnimWidget-v1.10.521.apk` and compare it with the table above.

Release certificate SHA-256 — check it in the app under "About → Signing cert":

`5E:9B:78:5C:93:2E:49:AD:62:1E:BE:FB:31:FB:14:66:59:2F:A9:62:88:29:55:A6:9C:EB:89:5D:04:EE:F4:D3`

## Install

1. Download `AnimWidget-v1.10.521.apk`.
2. Allow your browser or file manager to install unknown apps.
3. **vivo / iQOO**: a missing-ICP-filing warning is expected — tap "Authorize this installation". If Pure Mode blocks it, turn Pure Mode off under `Settings → Security → Pure Mode`.
4. Open the app and tap "＋ New widget" to add it to the home screen.

Upgrades are plain installs over the old version; widget configs and media stay in the app's private storage.

## Permissions

| Permission | Why | Optional |
|---|---|---|
| `FOREGROUND_SERVICE`<br>`FOREGROUND_SERVICE_SPECIAL_USE` | foreground service that delivers frames while playing | No |
| `POST_NOTIFICATIONS` | playback status notification | Yes |
| `SYSTEM_ALERT_WINDOW` | overlay used only by "show and adjust position" | Yes |
| `PACKAGE_USAGE_STATS` | detect whether the launcher is in front, to pause when hidden | Yes |
| `READ_MEDIA_IMAGES` (Android 13+)<br>`READ_EXTERNAL_STORAGE` (API 32 and below only) | pick and preview media | Yes |
| `INTERNET` | one GitHub Releases API request, only when the version row in About is tapped | normal permission |
| Shizuku (`moe.shizuku.manager.permission.API_V23`) | shell commands run locally by "background optimization"; unused unless Shizuku is installed | Yes |

No analytics SDK, no ads, nothing uploaded. Frames live as WebP in the app's private storage and are erased when you uninstall. Apart from that single update check, the app makes no network requests.

## Known limitations

- **Live wallpapers**: the wallpaper strip at the top of the editor can only use a static image; with a live wallpaper it is not the same picture as the one on screen.
- **Launchers without size reports**: when a launcher does not report `OPTION_APPWIDGET_SIZES`, the render uses an estimate.
- **Frame-rate ceiling**: frames are pushed through the launcher's rendering, so several high-frame-rate widgets together can end up below the configured rate.
- **Background limits**: some ROMs throttle background services; "background optimization (Shizuku)" or the battery whitelist helps.
- **Media limits**: video may last 40 seconds (the first 40 s are used); animations are not truncated, but a widget is capped at 2400 frames, and long animations have their frame rate recalculated from that 2400-frame budget.
- **Re-signed builds are limited**: the release build verifies its own signature; a build re-signed with another certificate stops pushing frames and exporting.

## How it works

- GIF, APNG and animated WebP decoders are written from scratch; video goes through MediaCodec.
- Frames are pushed through RemoteViews on two channels, bitmaps and files (URIs), picked according to what the launcher supports.
- The shadow curve is a decompiled reimplementation calibrated on real devices: one Gaussian tail, baked once and pasted per frame.
- Icons and controls are drawn entirely in code; colours are derived locally from an M3 colour wheel.
- The dex holds only the app, the Kotlin runtime and the optional Shizuku client (`kotlin/*`, `rikka/shizuku/*`) — no UI framework, no analytics or ad SDK.

## Build

The build does not use Gradle — it is one script:

```bash
./scripts/build-apk-src.sh 635 1.10.521   # javac → aapt2 → d8 → pack → apksig (v2 signature)
```

Output: `dist/AnimWidget-v1.10.521.apk`.

- Needs JDK 17, `aapt2` and Python 3.
- Toolchain jars (`android-all.jar`, `jdk-core-stubs.jar`, `r8.jar`, `apksig.jar`, `signapk-cli.jar`, `kotlin-stdlib.jar`, `annotations-13.0.jar`, `shizuku-all.jar`, `shizuku-aidl.jar`, `core-lambda-stubs.jar`) live in `toolchain/` and are not shipped; override with `TOOLCHAIN=/path`.
- The signing key belongs in `build-manual/debug.keystore` (alias `androiddebugkey`); it is not shipped — generate your own. The keystore and key passwords are the arguments on the `SignApk sign` line in the build script; edit that line if you bring your own key.
- Prefix with `OBFUSCATE=1` to build through R8 (renaming only, no shrinking or optimising).
- The Gradle files at the root only serve IDE import; the build does not use them.
- The built-in bilingual font is not shipped: without `patch-v1.4.0/res/font/anim_dual*` the build still works and the UI falls back to the system font; drop those three files back into `res/font/` to get the original glyphs.
- A self-built APK carries a different signature, so it cannot install over the official one — uninstall first, which clears configs and media.
- A self-built APK also trips the integrity check: only the release certificate is accepted, and a `BAD_CERT` build stops pushing frames and exporting. To make a self-built APK fully work, replace the `W` constant in `src-v2/io/github/animwidget/Integrity.java` with your own certificate's SHA-256 (eight ints).

## Repository layout

```
src-v2/         the only source tree (Java, io.github.animwidget)
patch-v1.4.0/   resources and manifest: res/, AndroidManifest.xml, stable-ids.txt
scripts/        build scripts: build-apk-src.sh, pack_apk.py, sync-r.py, r8-rules.pro
app/            legacy Gradle skeleton, not used by the build
```

- Start reading in `src-v2/`. The `patch-v1.4.0/` name is historical — it is today's resources and manifest, and the older tooling inside it is unused.
- Static gates, self-tests and working notes stay with the maintainer; the repository carries only four scripts.

## Feedback

Open an [issue](https://github.com/dlph-Ex/animwidget/issues) with your device model, OS version, launcher name, reproduction steps and the log exported from `Settings → Logs → Export log`.

## Sponsor

Entirely optional. Ko-fi: <https://ko-fi.com/frost_doplhin>. WeChat / Alipay: scan inside the app under `Settings → Developer → Donate`.

## License

MIT. Copyright (c) 2026 AnimWidget contributors. Full text in [LICENSE](LICENSE).
