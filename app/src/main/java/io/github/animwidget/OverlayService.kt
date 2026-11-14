package io.github.animwidget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Movie
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

/**
 * 流畅覆盖层：在桌面上用悬浮窗播放，视频走 TextureView + MediaPlayer，
 * GIF / 图片自绘并套形状蒙版，不受 RemoteViews 的逐帧限制（可到屏幕刷新率）。
 *
 * 需要 SYSTEM_ALERT_WINDOW 权限：
 *   adb shell appops set io.github.animwidget SYSTEM_ALERT_WINDOW allow
 */
class OverlayService : Service() {

    companion object {
        private const val TAG = "OverlayService"
        private const val ACTION_REFRESH = "io.github.animwidget.overlay.REFRESH"
        private const val ACTION_EDIT = "io.github.animwidget.overlay.EDIT"
        private const val ACTION_STOP = "io.github.animwidget.overlay.STOP"
        private const val CHANNEL_ID = "overlay"
        private const val NOTIFICATION_ID = 2001
        private const val EXTRA_ID = "widget_id"

        fun refresh(context: Context) {
            if (context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .getBoolean("all_stopped", false)
            ) return
            try {
                val intent = Intent(context, OverlayService::class.java).setAction(ACTION_REFRESH)
                context.startForegroundService(intent)
            } catch (_: Throwable) {
            }
        }

        fun edit(context: Context, widgetId: Int) {
            try {
                val intent = Intent(context, OverlayService::class.java)
                    .setAction(ACTION_EDIT)
                    .putExtra(EXTRA_ID, widgetId)
                context.startForegroundService(intent)
            } catch (_: Throwable) {
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, OverlayService::class.java))
            } catch (_: Throwable) {
            }
        }
    }

    private class Holder(
        val config: WidgetConfig,
        val params: WindowManager.LayoutParams,
        val root: FrameLayout,
        var player: MediaPlayer? = null
    )

    private class Content(val view: View, val player: MediaPlayer? = null)

    private val holderThread = HandlerThread("overlay-loop", android.os.Process.THREAD_PRIORITY_DISPLAY)
    private lateinit var bgHandler: Handler
    private lateinit var mainHandler: Handler

    private val windows = HashMap<Int, Holder>()
    private val trackedViews = mutableSetOf<View>()
    private var editingId = -1
    private var lastGeneration = -1
    private var anchorView: View? = null
    private var windowsVisible = false

    private val windowManager by lazy { getSystemService(WindowManager::class.java) }

    override fun onCreate() {
        super.onCreate()
        holderThread.start()
        bgHandler = Handler(holderThread.looper)
        mainHandler = Handler(mainLooper)
        ensureChannel()
        mainHandler.postDelayed(watchdog, 300L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        if (!canDrawOverlays()) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_EDIT -> {
                val id = intent.getIntExtra(EXTRA_ID, -1)
                val config = WidgetStore.load(this, id)
                if (config != null && config.overlayEnabled) {
                    editingId = id
                    mainHandler.post { reload() }
                }
            }
            ACTION_STOP -> {
                getSharedPreferences("settings", MODE_PRIVATE)
                    .edit().putBoolean("all_stopped", true).apply()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> mainHandler.post { reload() }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        bgHandler.removeCallbacksAndMessages(null)
        windows.keys.toList().forEach { removeWindow(it) }
        for (view in trackedViews.toList()) {
            try {
                windowManager.removeView(view)
            } catch (_: Throwable) {
            }
        }
        trackedViews.clear()
        removeAnchor()
        holderThread.quitSafely()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ------------------------------------------------------------ 窗口管理

    private val watchdog = object : Runnable {
        override fun run() {
            DeskWatcher.tick(this@OverlayService)

            if (anchorEnabled()) ensureAnchor() else removeAnchor()

            // 编辑模式强制显示；否则只在桌面停留 >= 3 秒时显示
            val shouldShow = editingId >= 0 ||
                DeskWatcher.canPlay ||
                !DeskWatcher.hasAccess(this@OverlayService)

            if (shouldShow && !windowsVisible) {
                windowsVisible = true
                reload()
            } else if (!shouldShow && windowsVisible) {
                windowsVisible = false
                removeAllWindows()
            } else if (windowsVisible &&
                WidgetStore.generation(this@OverlayService) != lastGeneration &&
                editingId < 0
            ) {
                reload()
            }
            mainHandler.postDelayed(this, 700L)
        }
    }

    /** 1x1、屏幕外、全透明的锚点窗口：只用于让系统持续给本进程分配渲染资源。 */
    private fun ensureAnchor() {
        if (anchorView != null) return
        val view = View(this)
        val params = WindowManager.LayoutParams(
            1,
            1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = -20
            y = -20
            alpha = 0f
        }
        try {
            windowManager.addView(view, params)
            anchorView = view
        } catch (t: Throwable) {
            Log.w(TAG, "anchor addView failed", t)
        }
    }

    private fun removeAnchor() {
        val view = anchorView ?: return
        try {
            windowManager.removeView(view)
        } catch (_: Throwable) {
        }
        anchorView = null
    }

    private fun removeAllWindows() {
        for (id in windows.keys.toList()) removeWindow(id)
    }

    private fun reload() {
        if (!windowsVisible && editingId < 0) return

        // 兜底：先把所有跟踪过的窗口从 WindowManager 上摘掉，
        // 避免任何情况下（异常/重复刷新）残留第二个窗口。
        windows.values.forEach {
            try {
                it.player?.stop()
                it.player?.release()
            } catch (_: Throwable) {
            }
        }
        windows.clear()
        for (view in trackedViews.toList()) {
            try {
                windowManager.removeView(view)
            } catch (_: Throwable) {
            }
        }
        trackedViews.clear()

        // 同一时间只允许一个覆盖层窗口：多个组件都开覆盖层时只保留一个。
        val enabled = WidgetStore.all(this)
            .filter { it.overlayEnabled }
            .sortedBy { it.widgetId }
        val preferredId = getSharedPreferences("settings", MODE_PRIVATE)
            .getInt("last_overlay_id", -1)
        val chosen = when {
            editingId >= 0 -> enabled.firstOrNull { it.widgetId == editingId }
            preferredId >= 0 -> enabled.firstOrNull { it.widgetId == preferredId }
                ?: enabled.firstOrNull()
            else -> enabled.firstOrNull()
        }
        if (chosen != null) addWindow(chosen)

        lastGeneration = WidgetStore.generation(this)
        if (windows.isEmpty() && editingId < 0 && !anchorEnabled()) {
            stopSelf()
        }
    }

    private fun removeWindow(widgetId: Int) {
        val holder = windows.remove(widgetId) ?: return
        try {
            holder.player?.stop()
            holder.player?.release()
        } catch (_: Throwable) {
        }
        holder.player = null
        try {
            windowManager.removeView(holder.root)
        } catch (_: Throwable) {
        }
        trackedViews.remove(holder.root)
    }

    private fun addWindow(config: WidgetConfig) {
        val density = resources.displayMetrics.density
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        val isEditing = config.widgetId == editingId
        val visible = isEditing || config.overlayVisible

        // overlayWidth <= 0 表示这个配置从未定位过：默认居中偏上，
        // 绝不能落在 (0,0)，否则勾选后会在左上角冒出一个窗口。
        val positioned = config.overlayWidth > 0
        val widthPx = if (positioned) {
            (config.overlayWidth * density).toInt()
        } else {
            (screenWidth * 0.6f).toInt()
        }.coerceIn(160, screenWidth)

        val ratio = if (config.frameWidth > 0 && config.frameHeight > 0) {
            config.frameHeight.toFloat() / config.frameWidth
        } else {
            9f / 16f
        }
        val heightPx = (widthPx * ratio).toInt().coerceIn(120, screenHeight)
        val x: Int
        val y: Int
        if (!visible) {
            // 隐藏态：整个窗口移到屏幕外，配合 alpha=0，屏幕上完全不可见
            x = -(widthPx + 200)
            y = -(heightPx + 200)
        } else if (positioned) {
            x = (config.overlayX * density).toInt()
            y = (config.overlayY * density).toInt()
        } else {
            x = (screenWidth - widthPx) / 2
            y = (screenHeight - heightPx) / 3
        }

        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            (if (isEditing) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) or
            // 允许窗口坐标超出屏幕，否则系统会把 overlay 夹在屏幕内
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        val params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
            alpha = if (visible) config.shapeAlpha.coerceIn(0, 100) / 100f else 0f
        }

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.TRANSPARENT)

        // 隐藏态不加载视频/GIF，避免后台解码耗电
        val content = if (visible) {
            buildContent(config, widthPx, heightPx)
        } else {
            Content(View(this), null)
        }
        root.addView(
            content.view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        if (isEditing) {
            root.addView(buildEditFrame(config, params, root))
            attachDragAndScale(root, params)
        }

        try {
            windowManager.addView(root, params)
            trackedViews.add(root)
            windows[config.widgetId] = Holder(config, params, root, content.player)
        } catch (t: Throwable) {
            Log.w(TAG, "addView failed", t)
        }
    }

    private fun buildContent(config: WidgetConfig, widthPx: Int, heightPx: Int): Content {
        val uri = config.sourceUri.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }
        return when (config.kind) {
            MediaKind.VIDEO -> buildVideoView(config, uri, widthPx, heightPx)
            MediaKind.GIF -> {
                val movie = try {
                    uri?.let { u -> contentResolver.openInputStream(u)?.use { Movie.decodeStream(it) } }
                } catch (_: Throwable) {
                    null
                }
                Content(MediaOverlayView(this, null, movie, config.shape, config.shapeAlpha))
            }
            MediaKind.IMAGE -> {
                val bitmap = try {
                    uri?.let { u ->
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                        val sample = if (bounds.outWidth > widthPx) {
                            Integer.highestOneBit(bounds.outWidth / widthPx).coerceAtLeast(1)
                        } else 1
                        val options = BitmapFactory.Options().apply { inSampleSize = sample }
                        contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, options) }
                    }
                } catch (_: Throwable) {
                    null
                }
                Content(MediaOverlayView(this, bitmap, null, config.shape, config.shapeAlpha))
            }
        }
    }

    private fun buildVideoView(
        config: WidgetConfig,
        uri: Uri?,
        widthPx: Int,
        heightPx: Int
    ): Content {
        val texture = TextureView(this)
        val player = MediaPlayer()
        try {
            if (uri != null) {
                player.setDataSource(this, uri)
                player.isLooping = true
                player.setVolume(0f, 0f)
                player.setSurface(Surface(texture.surfaceTexture))
                player.prepareAsync()
                player.setOnPreparedListener {
                    texture.surfaceTexture?.setDefaultBufferSize(it.videoWidth, it.videoHeight)
                    applyVideoTransform(texture, it.videoWidth, it.videoHeight)
                    it.start()
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "video prepare failed", t)
        }
        texture.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                try {
                    player.setSurface(Surface(st))
                    if (!player.isPlaying) player.start()
                } catch (_: Throwable) {
                }
            }

            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                applyVideoTransform(texture, player.videoWidth, player.videoHeight)
            }

            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                try {
                    player.setSurface(null)
                } catch (_: Throwable) {
                }
                return true
            }

            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
        }

        // 占位：如果视频源不可用，退回显示第一帧
        val fallback = try {
            WidgetStore.loadFrame(this, config, 0, widthPx, heightPx)
        } catch (t: Throwable) {
            null
        }
        val container = FrameLayout(this)
        if (fallback != null) {
            container.addView(MediaOverlayView(this, fallback, null, config.shape, config.shapeAlpha))
        }
        container.addView(
            texture,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        return Content(container, player)
    }

    private fun applyVideoTransform(texture: TextureView, videoWidth: Int, videoHeight: Int) {
        if (videoWidth <= 0 || videoHeight <= 0) return
        val viewWidth = texture.width.toFloat()
        val viewHeight = texture.height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) return
        val scale = maxOf(viewWidth / videoWidth, viewHeight / videoHeight)
        val dx = (viewWidth - videoWidth * scale) / 2f
        val dy = (viewHeight - videoHeight * scale) / 2f
        val matrix = Matrix()
        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
        texture.setTransform(matrix)
    }

    private fun buildEditFrame(
        config: WidgetConfig,
        params: WindowManager.LayoutParams,
        root: FrameLayout
    ): View {
        val border = View(this)
        border.setBackgroundColor(Color.TRANSPARENT)
        border.background = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            setStroke(4, Color.argb(210, 255, 255, 255))
            cornerRadius = 18f
        }
        border.isClickable = false

        val done = TextView(this).apply {
            text = "✓"
            textSize = 16f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 40f
            }
            setOnClickListener {
                saveEditedPosition(config, params)
            }
        }

        val container = FrameLayout(this)
        container.addView(
            border,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        val size = (36 * resources.displayMetrics.density).toInt()
        val lp = FrameLayout.LayoutParams(size, size, Gravity.TOP or Gravity.END).apply {
            topMargin = (6 * resources.displayMetrics.density).toInt()
            rightMargin = (6 * resources.displayMetrics.density).toInt()
        }
        container.addView(done, lp)
        container.isClickable = false
        return container
    }

    private fun attachDragAndScale(root: FrameLayout, params: WindowManager.LayoutParams) {
        val density = resources.displayMetrics.density
        val scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val newWidth = (params.width * detector.scaleFactor).toInt()
                    .coerceIn((120 * density).toInt(), resources.displayMetrics.widthPixels)
                val ratio = params.height.toFloat() / params.width.toFloat()
                params.width = newWidth
                params.height = (newWidth * ratio).toInt().coerceAtLeast((80 * density).toInt())
                try {
                    windowManager.updateViewLayout(root, params)
                } catch (_: Throwable) {
                }
                return true
            }
        })

        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0

        root.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!scaleDetector.isInProgress) {
                        params.x = startX + (event.rawX - downRawX).toInt()
                        params.y = startY + (event.rawY - downRawY).toInt()
                        try {
                            windowManager.updateViewLayout(root, params)
                        } catch (_: Throwable) {
                        }
                    }
                }
            }
            true
        }
    }

    private fun saveEditedPosition(config: WidgetConfig, params: WindowManager.LayoutParams) {
        val density = resources.displayMetrics.density
        val updated = config.copy(
            overlayX = (params.x / density).toInt().coerceAtLeast(0),
            overlayY = (params.y / density).toInt().coerceAtLeast(0),
            overlayWidth = (params.width / density).toInt().coerceAtLeast(120)
        )
        WidgetStore.save(this, updated)
        editingId = -1
        mainHandler.post { reload() }
    }

    // ------------------------------------------------------------ 通知

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_channel),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_anim)
            .setContentTitle(getString(R.string.overlay_notif_title))
            .setContentText(getString(R.string.overlay_notif_text))
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_stat_anim),
                    getString(R.string.overlay_stop),
                    stop
                ).build()
            )
            .build()
    }

    private fun anchorEnabled(): Boolean =
        getSharedPreferences("settings", MODE_PRIVATE).getBoolean("anchor_enabled", true)

    private fun canDrawOverlays(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(this)
        } else {
            true
        }
}
