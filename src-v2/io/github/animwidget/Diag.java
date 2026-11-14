package io.github.animwidget;

import android.content.Context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 定位式筛查（v1.8.8）。
 *
 * 每个「我觉得可能出问题」的分支都写一行带编号的记录：
 *   G01 GIF: Movie 解不出来      G02 GIF: 尺寸为 0
 *   G03 GIF: 退回静态首帧(1 帧)  G04 GIF: 出帧 0 张
 *   G10 GIF 出帧 N 张            G12 GIF: 元数据探测完成
 *   V01 视频: 出帧 0 张          V02 视频: 元数据探测开始
 *   V03 视频: 元数据探测完成     V04 视频: setDataSource 成功
 *   V05 视频: 帧返回 null        V06 视频: 首帧耗时
 *   V07 视频: 读不到时长         V10 视频出帧 N 张
 *   P01 缩略图: 解码失败         L01 预览帧: 解析异常(上层)
 *   L02 预览帧: 开始解析         L03 预览帧: 跳过/强制复位
 *   L04 预览帧: 应用 N 张        L05 预览帧: 首帧上屏
 *   L06 抽帧中途异常(被吞)       L07 预览播放启动
 *   L08 预览播放心跳             L09 预览播放停止(带调用点)
 *   L10 新素材 ≤1 帧不回退旧素材  L12 迟到发布作废
 *   M01 嗅探改判(MP4进GIF页等)  M02 GIF 页拒绝非动图
 *   M03 选完进入 onActivityResult M04 嗅探结果
 *   M05 探测完成                 M06 探测超时先行开抽
 *   R05 参数变化触发重解析       R06 探测后补一轮重解析
 *   P02 APNG 出帧/导出           P04 动图 WebP 退静态
 *   P05 动图 WebP 出帧/导出      M07 迟到探测作废
 *   R01 预览合成异常
 *   R02 回放帧读取异常           R03 回放帧合成异常
 *   W01 推帧循环异常             W02 小部件帧合成异常
 *   W03 小部件广播异常           O01 悬浮窗绘制异常
 *   C01 配置页启动               S01 阴影: 走 alpha 轮廓
 *   S02 阴影: 走蒙版路径         S03 阴影: 素材没有透明边
 *   F01 合成: 画布退化           F02 合成: 内容框退化
 *   F10 比例自检(素材 vs 画布)
 *   K01 动图探测+采样帧数        K02 头部尺寸(立即上屏)
 *   K03 帧率兜底 10fps           P10 动图解码 0 帧
 *   P11 解码失败退静态首帧       P12 预览合成跟不上丢帧
 *   J01 日志页打开
 *   T01 开始导出/保存失败  T02 导出取消/丢弃结果  T03 导出中改参数→重开
 *   T04 导出失败           T05 旧任务作废/提交失败  T06 导出完成(含布局)
 *   T07 离开应用(取消+释放)
 *   C01 色彩诊断(色彩空间+中央像素)
 *   P20 预览实测帧率       W10 桌面实测帧率
 *   W11 进入受限状态       W12 恢复不受限
 *   L20 原地切语言失败     L21 回前台重新开始解析(已废弃)
 *   L22 任务UI状态         L23 重复解析请求被忽略
 *   L24 谁调用了 loadPreviewFrames   L25 迟到结果目标一致→接手
 *   L26 抽帧被取消已清理   L27 不在前台延后播放   L28 回前台重新开始解析
 *   T11 导出开始时的任务 UI 可见性
 *   C07 HDR(PQ/HLG) 近似映射   L29 帧率×10 生效值
 *   T12 保存瞬间的布局/形状/缩放/帧率
 *   L30 导出中拒绝启动预览播放   L31 导出中不上屏(保持冻结)
 *   （v1.10.18：播放进度条灰置由 syncTaskUi 统一强制）
 *   O09 覆盖层刷新(权限/id)  O10 覆盖层服务启动  O11 覆盖层判定
 *   T13 保存时的 overlay/visible 开关
 *   M08 重复的选取回调被忽略
 *   C03 视频解码参数(旋转/色彩标准/范围)  C04 顺序解码出帧数
 *   C05 顺序解码失败回退   V11 视频回退逐帧 seek
 *   L32 任务进度(每 10% 一条，含控件真实值)  V12 导出耗时/帧数/质量
 *   F11 画质链路(源/预览帧/画布/小部件渲染 px)  M09 图片页拦动图(已废)
 *   M10 图片页识别到动图→自动按动图导入(已并回 M09)
 *   A03 日志导出写入失败(回退应用目录)  （v1.10.47：A12/A13/A14/A15/A16 已删除）
 *   v1.10.56：T20 离开应用时导出不再被取消（待确认的直接提交）/ T21 后台提交生效
 *             T22 导出完成但页面不可见→自动提交 / T23 提交后首帧读不出来 / T24 导出收尾异常
 *             W14 小部件取不到帧（帧号/配置帧数/文件是否存在/渲染尺寸）
 *   v1.10.65：W19 桌面状态查询异常（后台线程）/ W10 带画布·单帧耗时·预算·暂停次数
 *             （v1.10.69 起「均衡模式」开关已删除，桌面查询永远走后台线程）
 * 记录同时进 files/diag.txt，主界面「诊断」区直接显示最近几行。
 */
final class Diag {

    private static final String FILE = "diag.txt";
    private static long lastStamp;
    private static String lastCode;

    private Diag() {
    }

    static void warn(@NotNull Context context, @NotNull String code, @NotNull String detail) {
        try {
            String line = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()) + " " + code + " " + detail;
            // v1.8.13：同步进 logcat —— 应用进不去时 diag.txt 拿不到，
            // adb logcat -s AnimWidget 可以直接看编号链
            android.util.Log.i("AnimWidget", code + " " + detail);
            // v1.10.1（需求 6）：日志只留一个数据源 —— 编号也进日志页（30 分钟 TTL），
            // 不再另写 diag.txt（否则会出现「超过时限还在显示」的第二份数据）。
            LogBook.add(context, 'D', code + " " + detail);
        } catch (Throwable ignored) {
        }
    }

    /** 同一编号 3 秒内只记一次，避免逐帧刷屏。 */
    static void warnThrottled(@NotNull Context context, @NotNull String code, @NotNull String detail) {
        long now = System.currentTimeMillis();
        if (code.equals(lastCode) && now - lastStamp < 3000L) {
            return;
        }
        lastCode = code;
        lastStamp = now;
        warn(context, code, detail);
    }

    @Nullable
    static String tail(@NotNull Context context, int maxLines) {
        // 主界面「诊断」区读的就是日志页那份数据（同一份、同一时限）
        return LogBook.tail(context, maxLines);
    }

    static void clear(@NotNull Context context) {
        LogBook.wipe(context);
    }
}
