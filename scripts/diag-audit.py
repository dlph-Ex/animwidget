#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""判断代号体检（v1.10.240）。

对话里要求的「逐对核对判断代号」不可能靠肉眼做 100×100 对；能自动化的部分是**冲突**：
同一个代号被用在两处不同含义上（或同一处含义用了两个代号），以及「打了日志但没登记」。
这三类是误判/漏判的真源头，其余的「互斥」靠每条的「生成前提」字段判断，人工复核。

检查项：
  A 同一代号 → 多套判断文本（= 一个代号两个含义，最难查的那种）
  B 源码里出现、全表里没有（未登记）
  C 全表里有、源码里从不出现（幽灵条目）
  D 全表内重复代号
  E 代号格式不合法（不是 1 字母 + 2 位数字）
退出码 0 = 无 A/B/D/E；C 只提示不判失败。
"""
import os
import re
import sys
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src-v2", "io", "github", "animwidget")
CAT = os.path.join(SRC, "DiagCatalog.java")

BRANCH_OK = {
    "A12": "颜色编辑页结构：分段选择器插入成功/失败各一条，都是「这一页搭起来的结果」",
    "A13": "颜色编辑页的两个档位动作（虚线色档位、滑块作用范围），都是「用户改了哪个档」",
    "W43": "预览虚线取色：成功一条（主色→相反值）、失败一条（保留原色），两条都是「这次取色结果如何」，失败那条存在的意义是让「框没变色」能追到原因",
    "W56": "壁纸底图对位：取到一条（位图/显示区/缩放/图左上/预览区/代号）、读不到一条（附原因与代号）—— 两条都是「这一次底图摆成什么样」，读不到那条存在的意义是让「预览区只剩兜底底色」能追到是权限、锁屏还是空图（v1.10.519）",
    "W44": "动图打包：成功一条（帧数/体积/耗时）、失败一条、放弃一条、异常一条 —— 四条都是「这次打包的结果如何」，放弃那条存在的意义是让「桌面还在逐帧推」能追到原因",
    "W45": "动图自播：#4 第 2 步的推送一条、桌面取走一条、推送失败一条 —— 三条都是「这一代到底交给桌面没有」，失败那条存在的意义是让「还停在上一张」能追到原因",
    "W47": "播放口径：正常一条（四项判定）、异常一条，两条都是「这一代按什么口径播」，异常那条存在的意义是让「改完更糊/更慢」能追到哪一项被破坏",
    "A02": "onUpdate 进入：单格 provider 与多格 provider 各一条，含义相同（「系统广播把这一路叫醒了」），多格那条只多一个标注，便于在日志里区分是哪一种组件",
    "W49": "分层帧：自检一条、跳过一条（等第0帧）、失败一条 —— 都是「这一代分层帧到底成没成」，失败那条存在的意义是 482 静默失败过一次（连 W49 都没有）",
    "W48": "编码/解码对拍：测到一条（WebP/JPEG/合成三个数）、跳过一条（画布太小/太大，附门槛）—— 两条都是「这一代到底量到了没有」，跳过那条存在的意义是 478 静默跳过、真机上一次都没测到",
    "A05": "设置项变更：每一类设置各一条，含义相同（“改了哪个值”）",
    "A08": "对比度滑杆族：滑杆曲线参数 / 拖动开始 / 拖动结束，都是「对比度滑杆这一刻在干什么」",
    "A10": "调色页保存/重置，同一动作的两个方向",
    "A11": "方案生效/覆盖保存/已修改判定异常，同一“方案动作”族",
    "C01": "配置页启动与两种取色，都是“这一帧的色彩口径”",
    "F11": "尺寸来源与画质链路，两条都是“尺寸/画质口径”",
    "G03": "动图退回静态首帧的三条路径（导出/静态解码失败/1 帧）",
    "L12": "迟到结果作废：预览与合成两条并行链各报一次",
    "L30": "任务中/导出中拒绝播放，同一判定的两种触发场景",
    "L31": "冻结期间丢弃帧的四种来源，判定同一条规则",
    "L33": "冻结期重绘的两个分支（有冻结帧/无冻结帧）",
    "L34": "播放条被拒写的两种原因（任务中/贴右端）",
    "N01": "赞赏弹层：模糊底与构建异常，都是“弹层自检”",
    "N02": "赞赏码：超上限与滑出耗时，都是“赞赏码动画自检”",
    "N03": "赞赏码解码：兜底解码与预解码完成",
    "P11": "解码失败退回静态帧：预览与导出两条",
    "P23": "无损自检：回读失败/尺寸不一致/帧数 0/异常",
    "S03": "S03 正常汇总行 + 异常行，同一个探头的两种结局",
    "T02": "导出取消：异常与丢弃结果",
    "T05": "提交导出：失败/旧任务作废/改用快照提交",
    "T06": "导出完成：保存布局与完成两行",
    "T20": "离开应用：后台继续/直接提交/提交失败",
    "T21": "后台提交：生效与首帧推送失败",
    "T26": "导出时长：正常与自检异常",
    "T27": "导出参数：动图与视频各一条",
    "V10": "视频出帧与导出进度",
    "V11": "顺序解码不可用后回退逐帧 seek（两处调用同一含义）",
    "W03": "onUpdate 与尺寸变更两条异常入口",
    "W13": "预热族：开始/完成/重置失败/预装帧装载与回退/每帧体积/延后",
    "W32": "重推组件：全部（开关变更）/单个（预热收尾），都是「按运行时口径重推一次」",
    "W33": "进程内存：正常探针行 / 探针本身抛异常，同一含义（「这次没读到」）",
    "W34": "预算自校准：调了系数 / 没调并写明为什么（前台让路、并发预热）—— 都是「这一批的实测吞吐怎么影响系数」，跳过也是这个判定的一个结局（v1.10.333）",
    "W22": "尺寸变更：位置行与 options 全量行",
    "W24": "删除动画：参数/超时收尾/异常",
    "W28": "帧画布口径同一判据的两种结局：正常行记「这一拍用哪块画布/格子/布局」；"
           "「尺寸未知延后」记 launcher 还在报默认 110dp、这一拍主动不算画布（v1.10.324，"
           "按 110dp 算出的比例会和真格子差 9.7%，推上去就是\"先小一圈\"）",
    "W29": "导出后预热：开始/跳过两种原因/异常",
    "W30": "推送族：URI 推送/updateAppWidget/写盘/压小重推",
    "W31": "恢复推帧（两处调用同一含义）",
    "T30": "作用范围开关：正常切换与芯片创建失败兜底，同一个开关的两条记录",
    "S10": "钉组件快捷方式：成功发起 / 桌面不支持 / 异常，同一个动作的三种结局",
    "S11": "组件声明自检：正常一行 / 读取失败，同一个探针的两种结局",
}

fails = []


def read(p):
    with open(p, encoding="utf-8", errors="replace") as f:
        return f.read()


cat = read(CAT)
# 全表：{"代号", "前提", ...}
entries = re.findall(r'\{"([A-Z][0-9]{2})",\s*"([^"]*)",\s*"([^"]*)",\s*"([^"]*)",\s*"([^"]*)"', cat)
known = defaultdict(list)
for code, pre, req, why, text in entries:
    known[code].append({"pre": pre, "text": text})

# 源码里的调用点（跳过 DiagCatalog 自己）
sites = defaultdict(list)
for fn in sorted(os.listdir(SRC)):
    if not fn.endswith(".java") or fn == "DiagCatalog.java":
        continue
    text = read(os.path.join(SRC, fn))
    for m in re.finditer(r'Diag\.warn\w*\(\s*[^,]+,\s*"([A-Z][0-9]{2})"\s*,\s*(.{0,160})', text, re.S):
        code = m.group(1)
        frag = m.group(2)
        frag = frag.split("\n")[0][:90]
        sites[code].append("%s: %s" % (fn, frag.strip()))

print("== 判断代号体检")
print("   全表条目=%d 个代号，源码调用点=%d 个代号"
      % (len(entries), len(sites)))

# A 同一代号多含义：同号出现多套文本 → 必须进 BRANCH_OK 并写明「为什么这仍是同一个含义」。
# 没进白名单就判失败，避免以后又往一个号里塞第二种含义（v1.10.240 的 W13/W16 就是这么坏的）。
print("\n== A 同一代号 → 多套文本（必须已在 BRANCH_OK 里说明过）")
multi = []
for code, frags in sorted(sites.items()):
    keys = set(re.sub(r'[^0-9A-Za-z\u4e00-\u9fff]', '', f.split(": ", 1)[-1])[:12] for f in frags)
    if len(keys) > 1:
        multi.append(code)
unreviewed = [c for c in multi if c not in BRANCH_OK]
stale = [c for c in BRANCH_OK if c not in multi]
print("   多文本代号=%d，已复核=%d" % (len(multi), len(multi) - len(unreviewed)))
if unreviewed:
    fails.append("A：%d 个多文本代号没复核（%s）" % (len(unreviewed), " ".join(unreviewed)))
    for c in unreviewed:
        print("   · %s" % c)
        for f in sorted(sites[c]):
            print("       %s" % f)
else:
    print("   OK 全部已复核（BRANCH_OK 里有理由）")
if stale:
    print("   提示：BRANCH_OK 里这些号已经不再多文本，可删：%s" % " ".join(stale))

# B 未登记
print("\n== B 源码有、全表没有")
unreg = [c for c in sorted(sites) if c not in known]
if unreg:
    fails.append("B：未登记 %s" % " ".join(unreg))
    for c in unreg:
        print("   · %s  %s" % (c, sites[c][0]))
else:
    print("   OK 全部已登记")

# C 幽灵条目
print("\n== C 全表有、源码没有")
ghost = [c for c in sorted(known) if c not in sites]
print("   %s" % ("OK 无" if not ghost else "提示（%d 个）：%s" % (len(ghost), " ".join(ghost))))

# D 重复代号
print("\n== D 全表内重复")
dups = [c for c, v in known.items() if len(v) > 1]
if dups:
    fails.append("D：全表重复 %s" % " ".join(dups))
    print("   · %s" % " ".join(dups))
else:
    print("   OK 无重复")

# E 格式
print("\n== E 代号格式")
bad = [c for c in list(sites) + list(known) if not re.fullmatch(r"[A-Z][0-9]{2}", c)]
if bad:
    fails.append("E：格式非法 %s" % " ".join(sorted(set(bad))))
    print("   · %s" % " ".join(sorted(set(bad))))
else:
    print("   OK 全部是 1 字母 + 2 位数字")

print("\n== 结论")
if fails:
    for f in fails:
        print("   FAIL " + f)
    sys.exit(1)
print("   OK")
