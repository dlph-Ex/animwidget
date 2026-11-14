package io.github.animwidget

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock

/**
 * 桌面停留监测：只有当前台是桌面（launcher）且连续停留 >= 3 秒时才允许播放。
 * 使用 UsageStatsManager 读取最近的前台事件，需要「使用情况访问」权限：
 *   adb shell appops set io.github.animwidget GET_USAGE_STATS allow
 * 没权限时退化为「总是播放」，不影响功能。
 */
object DeskWatcher {

    private const val DESK_DELAY_MS = 3000L
    private const val EVENT_WINDOW_MS = 10 * 60 * 1000L

    @Volatile
    var canPlay: Boolean = false
        private set

    private var deskSince = 0L
    private var launcherPackage: String? = null
    private var lastTick = 0L
    private var lastForeground: String? = null

    fun hasAccess(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (t: Throwable) {
            false
        }
    }

    fun tick(context: Context) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastTick < 900L) return
        lastTick = now

        // 锁屏 / 息屏时立即停播
        val power = context.getSystemService(PowerManager::class.java)
        if (power != null && !power.isInteractive) {
            deskSince = 0L
            canPlay = false
            return
        }
        if (launcherPackage == null) {
            launcherPackage = resolveLauncher(context)
        }
        if (!hasAccess(context) || launcherPackage.isNullOrBlank()) {
            canPlay = true
            return
        }

        // 10 分钟内没有新事件时（一直停在桌面），沿用上一次的结果，避免误判成"不在桌面"
        val foreground = foregroundPackage(context) ?: lastForeground
        lastForeground = foreground
        if (foreground == launcherPackage) {
            if (deskSince == 0L) deskSince = now
            canPlay = now - deskSince >= DESK_DELAY_MS
        } else {
            deskSince = 0L
            canPlay = false
        }
    }

    fun reset() {
        deskSince = 0L
        canPlay = true
    }

    fun foreground(): String? = lastForeground

    fun launcher(context: Context): String? {
        if (launcherPackage == null) launcherPackage = resolveLauncher(context)
        return launcherPackage
    }

    private fun resolveLauncher(context: Context): String? = try {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager
            .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName
        if (!resolved.isNullOrBlank()) {
            resolved
        } else {
            // 个别 ROM 的 resolveActivity 返回空，退化为取第一个 HOME Activity
            context.packageManager.queryIntentActivities(intent, 0)
                .firstOrNull()
                ?.activityInfo
                ?.packageName
        }
    } catch (t: Throwable) {
        null
    }

    private fun foregroundPackage(context: Context): String? {
        return try {
            val usage = context.getSystemService(UsageStatsManager::class.java)
                ?: return null
            val end = System.currentTimeMillis()
            val events = usage.queryEvents(end - EVENT_WINDOW_MS, end)
                ?: return null
            var packageName: String? = null
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    packageName = event.packageName
                }
            }
            packageName
        } catch (t: Throwable) {
            null
        }
    }
}
