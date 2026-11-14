#!/usr/bin/env python3
"""Gate [33]: static check of the in-app signature self test (Integrity.java).

Exit 0 = all checks hold. Any failed check exits 1 (this is a real gate, not a printout).

Expected fingerprint source:
  * default: the certificate in build-manual/debug.keystore (via keytool). Missing keystore = FAIL.
  * INTEGRITY_NO_KEYSTORE=1: use the fingerprint documented in HANDOFF (weak evidence, printed as such).
    For environments that must not hold the key (external contributors). Release machine must not set it.
"""
import os, re, subprocess, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src-v2", "io", "github", "animwidget")
MANIFEST = os.path.join(ROOT, "patch-v1.4.0", "AndroidManifest.xml")
KEYSTORE = os.path.join(ROOT, "build-manual", "debug.keystore")
DOC_FP = "5E:9B:78:5C:93:2E:49:AD:62:1E:BE:FB:31:FB:14:66:59:2F:A9:62:88:29:55:A6:9C:EB:89:5D:04:EE:F4:D3"
PKG = "io.github.animwidget"
CODES = ("K52", "K53")

errs, notes = [], []


def fail(msg):
    errs.append(msg)


def read(p):
    try:
        with open(p, encoding="utf-8") as f:
            return f.read()
    except OSError:
        return None


_STR_OR_COMMENT = re.compile(r'("(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])*\')|//[^\n]*|/\*.*?\*/', re.S)


def code_only(s):
    """Strip comments, keep string literals (LESSONS #176: only code lines count)."""
    return _STR_OR_COMMENT.sub(lambda m: m.group(1) or " ", s)


def keystore_fp():
    out = subprocess.run(
        ["keytool", "-list", "-v", "-keystore", KEYSTORE, "-storepass", "android",
         "-alias", "androiddebugkey"], capture_output=True, text=True)
    m = re.search(r"SHA-?256:\s*([0-9A-Fa-f:]{95})", out.stdout)
    return m.group(1).upper() if m else None


# ---- 1. Integrity class, both lookup paths -------------------------------------------------
integ_raw = read(os.path.join(SRC, "Integrity.java"))
if integ_raw is None:
    fail("[1] Integrity.java not found")
    integ = ""
else:
    integ = code_only(integ_raw)
    for tok in ("GET_SIGNING_CERTIFICATES", "GET_SIGNATURES"):
        if tok not in integ:
            fail("[1] Integrity.java lacks " + tok)
    if "SDK_INT >= 28" not in integ:
        fail("[1] no SDK_INT >= 28 branch separating the two lookup paths")
# v1.10.513：**只查 token 是不够的**（自检发现：删掉 28+ 分支的 return 也照样通过）。
# 所以再钉两条"分支真的接上了"：
if re.search(r"SDK_INT\s*>=\s*28\s*\)\s*\{\s*return\s+Api28\.contentSigners\(", integ) is None:
    fail("[1] the SDK_INT >= 28 branch does not return Api28.contentSigners(...)")
if re.search(r"getPackageInfo\(PKG,\s*PackageManager\.GET_SIGNATURES\)", integ) is None:
    fail("[1] the 26/27 branch does not read GET_SIGNATURES from getPackageInfo(PKG, ...)")

# ---- 3. multi-signer / history criteria -----------------------------------------------------
for tok in ("hasMultipleSigners()", "getSigningCertificateHistory()"):
    if tok not in integ:
        fail("[3] Integrity.java lacks " + tok)
if "getApkContentsSigners()" not in integ:
    fail("[3] Integrity.java lacks getApkContentsSigners()")
if re.search(r"signers\.length\s*!=\s*1", integ) is None:
    fail("[3] signer count != 1 is not enforced on the shared path")

# ---- 2. expected fingerprint == keystore certificate ---------------------------------------
m = re.search(r"int\[\]\s+\w+\s*=\s*\{([^}]*)\}", integ)
if not m:
    fail("[2] no int[] fingerprint words in Integrity.java")
else:
    words = re.findall(r"0x[0-9a-fA-F]{1,8}", m.group(1))
    if len(words) != 8:
        fail("[2] fingerprint must be exactly 8 int words, found %d" % len(words))
    else:
        got = ":".join("%02X" % b for w in words for b in int(w, 16).to_bytes(4, "big"))
        if os.path.exists(KEYSTORE):
            want, src = keystore_fp(), "keystore"
            if want is None:
                fail("[2] keytool could not read the keystore certificate")
        elif os.environ.get("INTEGRITY_NO_KEYSTORE") == "1":
            want, src = DOC_FP, "HANDOFF doc (weak evidence: keystore not present)"
        else:
            want, src = None, None
            fail("[2] build-manual/debug.keystore missing (set INTEGRITY_NO_KEYSTORE=1 only off the release machine)")
        if want is not None:
            if got != want:
                fail("[2] fingerprint mismatch\n      in code : %s\n      %s: %s" % (got, src, want))
            else:
                notes.append("fp=" + src.split(" ")[0])

# ---- readable 64-hex constant must not exist (spec 1.5) ---------------------------------------
for name in ("Integrity.java", "App.java"):
    raw = read(os.path.join(SRC, name)) or ""
    c = code_only(raw)
    if re.search(r"[0-9A-Fa-f]{64}", c) or re.search(r"(?:[0-9A-Fa-f]{2}:){15}[0-9A-Fa-f]{2}", c):
        fail("[2] %s contains a readable fingerprint string" % name)

# ---- 4. package constant == manifest package ---------------------------------------------
pm = re.search(r'String\s+PKG\s*=\s*"([^"]+)"', integ)
man = read(MANIFEST) or ""
mm = re.search(r'\bpackage\s*=\s*"([^"]+)"', man)
if not pm:
    fail("[4] package-name constant missing")
elif pm.group(1) != PKG:
    fail("[4] package constant is %s, expected %s" % (pm.group(1), PKG))
elif not mm:
    fail("[4] manifest has no package attribute")
elif mm.group(1) != pm.group(1):
    fail("[4] manifest package %s != Integrity %s" % (mm.group(1), pm.group(1)))
if not re.search(r'<application\b[^>]*android:name\s*=\s*"(?:\.App|%s\.App)"' % re.escape(PKG), man, re.S):
    fail("[4] manifest <application> is not android:name=\".App\"")

# ---- 5. call sites >= 3 ---------------------------------------------------------------------
def uses(name, pat):
    t = read(os.path.join(SRC, name))
    return t is not None and re.search(pat, code_only(t)) is not None

sites = []
if uses("App.java", r"Integrity\.evaluate\("): sites.append("App")
else: fail("[5] App.java does not call Integrity.evaluate(")
if uses("I18nActivity.java", r"Integrity\.restricted\("): sites.append("I18nActivity")
else: fail("[5] I18nActivity.java does not call Integrity.restricted(")
gate_files = [n for n in ("PlaybackService.java", "WidgetProvider.java", "ConfigActivity.java",
                          "WidgetRender.java")
              if uses(n, r"Integrity\.restricted\(")]
if not gate_files:
    fail("[5] none of PlaybackService / WidgetProvider / ConfigActivity calls Integrity.restricted(")
sites += [g[:-5] for g in gate_files]
if len(sites) < 3:
    fail("[5] call sites < 3: %s" % sites)

# ---- 6. failure path has no throw ----------------------------------------------------------
for name in ("Integrity.java", "App.java"):
    raw = read(os.path.join(SRC, name))
    if raw is not None and re.search(r"\bthrow\b", code_only(raw)):
        fail("[6] %s contains a throw statement" % name)

# ---- 7. diagnostic codes registered ---------------------------------------------------------
# Real audit (unique, one code one meaning) is gate [8]; here only: both literals exist in DiagCatalog.
cat = read(os.path.join(SRC, "DiagCatalog.java"))
if cat is None:
    fail("[7] DiagCatalog.java not found")
else:
    for c in CODES:
        if not re.search(r'"%s\b' % c, code_only(cat)):
            fail("[7] %s not registered in DiagCatalog.java" % c)

# ---- 8. 26/27 的"读不到签名"必须放行，不能误限制正版（v1.10.513）-----------------------
# 这一条是防"以后有人觉得'取不到就当异常'更安全"而把正版用户全限制掉。
if "UNREADABLE" not in integ:
    fail("[8] Integrity.java 没有 UNREADABLE 这一档（26/27 读不到签名会被误限制）")
if len(re.findall(r"\bUNREADABLE\b", integ)) < 3:
    fail("[8] UNREADABLE 只在声明处出现：26/27 的两条放行分支被删了（会误限制正版）")
if not uses("App.java", r'"K51'):
    fail("[8] App 没有为通过的情况记 K51（正版用户无法证明判定真的跑过）")
if not uses("App.java", r'"K54'):
    fail("[8] App 没有为 UNREADABLE 记 K54（放行时用户和我们都没线索）")
if re.search(r"return\s+evaluate\(c\)\s*!=\s*OK", integ):
    fail("[8] restricted() 又写成 evaluate(c) != OK（UNREADABLE 会被算成限制）")
if not re.search(r"return\s+s\s*==\s*BAD_CERT\s*\|\|\s*s\s*==\s*BAD_SIGNERS", integ):
    fail("[8] restricted() 没有显式收窄到 BAD_CERT / BAD_SIGNERS")
for c in ("K54", "K51"):
    if not re.search(r'"%s\b' % c, code_only(cat or "")):
        fail("[8] %s 未登记（26/27 的放行记录）" % c)

# ---- 9. 限制模式的两个动作必须真的接在唯一出口上（v1.10.513）--------------------------
# 导出：ConfigActivity.startExport 开头；推帧：WidgetRender.render 唯一出口 + WidgetProvider.onUpdate。
# 少了任何一处，"改包版"照样能导出/照样能在桌面动 —— 那需求①就是空转。
if not uses("WidgetRender.java", r"Integrity\.restricted\("):
    fail("[9] WidgetRender.render（唯一推帧出口）没有 Integrity.restricted 闸门")
if not uses("WidgetProvider.java", r"Integrity\.restricted\("):
    fail("[9] WidgetProvider.onUpdate 没有 Integrity.restricted 闸门")

# ---- 10. 用户可核对的身份行不许被删（v1.10.513）-------------------------------------
# 关于页必须有「签名证书」行；设置页必须有非官方提示。删掉这两处 = 用户没法自己核对。
# 判据必须钉**调用点**（addCertRow(list ...)），不能只查方法体里的字面量 ——
# 只查字面量的话，把调用删掉、方法留着，门禁照样通过（自检用例当场抓到过）。
if not uses("AboutSheet.java", r"addCertRow\(list"):
    fail("[10] 关于页没有「签名证书」行（用户无法自行核对）：addCertRow(list ...) 调用不见了")
if not uses("SettingsActivity.java", r"R\.string\.integrity_hint"):
    fail("[10] 设置页没有非官方构建提示")

# ---- result ---------------------------------------------------------------------------------
print("== [33] 签名自检")
if errs:
    for e in errs:
        print("  FAIL " + e)
    sys.exit(1)
print("OK（两条取法(含分支接线) + 多签名者/历史判据 + 指纹逐字符=%s + 包名一致 + 调用点 %d 处 [%s] + 无 throw + K52/K53/K54 已登记 + 26/27 读不到时放行 + 推帧/导出/设置页/关于页四处接线）"
      % (notes[0].split("=")[1] if notes else "?", len(sites), ",".join(sites)))
