package io.github.animwidget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import android.os.SystemClock

/**
 * 多实例播放服务：
 * 单个 HandlerThread（DISPLAY 优先级）轮询所有小部件，各自维护帧号与下一次更新时间，
 * 因此不同实例可以用不同帧率、不同素材，互不影响。
 */
class PlaybackService : Service() {

    companion object {
        private const val ACTION_START = "io.github.animwidget.action.START"
        private const val ACTION_STOP = "io.github.animwidget.action.STOP"
        private const val CHANNEL_ID = "animated_widget"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context) {
            if (context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .getBoolean("all_stopped", false)
            ) return
            try {
                context.startForegroundService(
                    Intent(context, PlaybackService::class.java).setAction(ACTION_START)
                )
            } catch (_: Throwable) {
                // Android 12+ 后台启动受限，由用户打开 App 或点击小部件时再启动
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, PlaybackService::class.java))
            } catch (_: Throwable) {
            }
        }
    }

    private data class State(var index: Int = 0, var nextAt: Long = 0L)

    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var cache: FrameCache? = null

    private var running = false
    private var screenOn = true

    private var configs: MutableMap<Int, WidgetConfig> = HashMap()
    private val states = HashMap<Int, State>()
    private var lastGeneration = -1
    private var lastGenerationCheck = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> screenOn = false
                Intent.ACTION_SCREEN_ON -> screenOn = true
            }
        }
    }

    private val loop = object : Runnable {
        override fun run() {
            if (!running) return
            try {
                if (screenOn) tick() else handler?.postDelayed(this, 500L)
            } catch (t: Throwable) {
                // 单次异常不能让循环停掉
                handler?.postDelayed(this, 100L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        ensureChannel()

        val worker = HandlerThread("widget-loop", Process.THREAD_PRIORITY_DISPLAY)
        worker.start()
        thread = worker
        handler = Handler(worker.looper)

        val heapKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
        cache = FrameCache(heapKb / 6)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            getSharedPreferences("settings", MODE_PRIVATE)
                .edit().putBoolean("all_stopped", true).apply()
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (!running) {
            running = true
            reload(force = true)
            handler?.post(loop)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        isRunning = false
        handler?.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Throwable) {
        }
        thread?.quitSafely()
        thread = null
        handler = null
        cache?.clear()
        cache = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun tick() {
        DeskWatcher.tick(this)
        // 只有桌面得眼前才需要刷帧；其它界面/锁屏时暂停，省电
        if (DeskWatcher.hasAccess(this) && !DeskWatcher.canPlay) {
            handler?.postDelayed(loop, 600L)
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastGenerationCheck >= 500L) {
            lastGenerationCheck = now
            if (WidgetStore.generation(this) != lastGeneration) {
                reload(force = false)
            }
        }

        val manager = AppWidgetManager.getInstance(this)
        val ids = WidgetProvider.widgetIds(this)
        if (ids.isEmpty()) {
            running = false
            stopSelf()
            return
        }

        var nextAt = now + 1000L
        for (widgetId in ids) {
            val config = configs[widgetId] ?: continue
            if (config.frameCount <= 0) continue
            val state = states.getOrPut(widgetId) { State() }
            if (state.nextAt == 0L) state.nextAt = now

            if (now >= state.nextAt) {
                if (config.frameCount <= 1) {
                    if (state.index == 0) {
                        WidgetRender.render(this, manager, config, 0, cache)
                        state.index = 1
                    }
                    state.nextAt = Long.MAX_VALUE
                } else {
                    val cost = WidgetRender.render(this, manager, config, state.index, cache)
                    state.index = (state.index + 1) % config.frameCount
                    val interval = 1000L / config.fps.coerceIn(1, 50)
                    state.nextAt = SystemClock.elapsedRealtime() +
                        (interval - cost).coerceAtLeast(8L)
                }
            }
            if (state.nextAt in 1 until nextAt) nextAt = state.nextAt
        }

        val delay = (nextAt - SystemClock.elapsedRealtime()).coerceIn(4L, 1000L)
        handler?.postDelayed(loop, delay)
    }

    private fun reload(force: Boolean) {
        val generation = WidgetStore.generation(this)
        if (!force && generation == lastGeneration) return
        lastGeneration = generation
        configs = WidgetStore.all(this)
            .filter { it.frameCount > 0 }
            .associateBy { it.widgetId }
            .toMutableMap()
        states.clear()
        cache?.clear()
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_text)
            setShowBadge(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openApp = WidgetRender.contentIntent(this)
        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_stat_anim),
            getString(R.string.notif_stop),
            stopPendingIntent()
        ).build()

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_anim)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp)
            .addAction(stopAction)
            .build()
    }

    private fun stopPendingIntent(): PendingIntent {
        val intent = Intent(this, PlaybackService::class.java).setAction(ACTION_STOP)
        return PendingIntent.getService(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
