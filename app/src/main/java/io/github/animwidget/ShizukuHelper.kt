package io.github.animwidget

import android.app.Activity
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * Shizuku 可选集成：
 * 用户授权后，以 shell 身份执行后台策略命令（Doze 白名单、standby bucket、AppOps）。
 * 没装 Shizuku / 未授权时，所有调用都静默失败，不影响主功能。
 */
object ShizukuHelper {

    private const val PERMISSION_REQUEST = 100

    @Volatile
    private var listenerRegistered = false

    fun init() {
        if (listenerRegistered) return
        listenerRegistered = true
        try {
            Shizuku.addBinderReceivedListenerSticky { }
        } catch (_: Throwable) {
        }
    }

    fun isReady(): Boolean = try {
        Shizuku.pingBinder()
    } catch (t: Throwable) {
        false
    }

    fun hasPermission(): Boolean = try {
        isReady() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (t: Throwable) {
        false
    }

    fun requestPermission(activity: Activity, callback: (Boolean) -> Unit) {
        try {
            if (!isReady()) {
                callback(false)
                return
            }
            if (hasPermission()) {
                callback(true)
                return
            }
            Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
                if (requestCode == PERMISSION_REQUEST) {
                    callback(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            }
            Shizuku.requestPermission(PERMISSION_REQUEST)
        } catch (t: Throwable) {
            callback(false)
        }
    }

    /** 执行多条 shell 命令，返回文字结果（后台线程调用）。 */
    fun runCommands(commands: List<String>): String {
        val report = StringBuilder()
        for (command in commands) {
            report.append("$ ").append(command).append('\n')
            try {
                val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val error = process.errorStream.bufferedReader().use { it.readText() }
                val code = process.waitFor()
                if (output.isNotBlank()) report.append(output.trim()).append('\n')
                if (error.isNotBlank()) report.append(error.trim()).append('\n')
                report.append("exit=").append(code).append('\n')
            } catch (t: Throwable) {
                report.append("失败: ").append(t.message ?: t.javaClass.simpleName).append('\n')
            }
        }
        return report.toString()
    }

    /**
     * 后台调度优化：
     * - 自己和桌面（launcher）都进 Doze 白名单、置为 active bucket
     * - 放宽后台 AppOps
     * - 固定性能模式（Android 11+，让 SoC 不降频）
     * - 关闭 cached_apps_freezer（避免后台进程被冻结）
     */
    fun backgroundCommands(packageName: String, launcherPackage: String?): List<String> {
        val commands = mutableListOf(
            "cmd deviceidle whitelist +$packageName",
            "am set-standby-bucket $packageName active",
            "cmd appops set $packageName RUN_IN_BACKGROUND allow",
            "cmd appops set $packageName RUN_ANY_IN_BACKGROUND allow"
        )
        if (!launcherPackage.isNullOrBlank() && launcherPackage != packageName) {
            commands += "cmd deviceidle whitelist +$launcherPackage"
            commands += "am set-standby-bucket $launcherPackage active"
        }
        commands += "cmd power set-fixed-performance-mode-enabled true"
        commands += "settings put global cached_apps_freezer disabled"
        return commands
    }
}
