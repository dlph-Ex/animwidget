package io.github.animwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

/**
 * 单个小部件实例的配置页。
 * 两种入口：
 * 1) 从桌面添加小部件时（android:configure），Intent 带 EXTRA_APPWIDGET_ID；
 * 2) 从 MainActivity 点「编辑」，带 "widget_id"。
 */
class ConfigActivity : Activity() {

    companion object {
        const val EXTRA_WIDGET_ID = "widget_id"
        private const val REQUEST_PICK = 11
    }

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var base: WidgetConfig? = null

    private var pickedUri: Uri? = null
    private var pickedKind: MediaKind? = null
    private var pickedName: String? = null
    private var pendingKind: MediaKind = MediaKind.VIDEO

    private var sourceFps: Int? = null
    private var sourceWidth = 0
    private var sourceHeight = 0
    private var sourceDurationMs = 0L
    private var selectedShape = ShapeKind.ROUNDED
    private var shapesExpanded = false
    private var selectedLayout = LayoutMode.FILL
    private var selectedRotation = 0
    private var selectedShapeRotation = 0
    private var selectedFitContent = true
    private var busy = false

    private lateinit var imgPreview: ImageView
    private lateinit var pbPreview: android.widget.ProgressBar
    private lateinit var seekPlayback: SeekBar
    private lateinit var tvPlayTime: TextView
    private lateinit var tvResolution: TextView
    private lateinit var tvPick: TextView
    private lateinit var progress: ProgressBar
    private lateinit var tvFps: TextView
    private lateinit var tvEdge: TextView
    private lateinit var tvQuality: TextView
    private lateinit var tvAlpha: TextView
    private lateinit var seekFps: SeekBar
    private lateinit var seekEdge: SeekBar
    private lateinit var seekQuality: SeekBar
    private lateinit var seekAlpha: SeekBar
    private lateinit var seekScale: SeekBar
    private lateinit var seekShadow: SeekBar
    private lateinit var btnRotate: Button
    private lateinit var btnRotateShape: Button
    private lateinit var tvScale: TextView
    private lateinit var tvShadow: TextView
    private lateinit var cbBorder: CheckBox
    private lateinit var cbOverlay: CheckBox
    private lateinit var cbOverlayVisible: CheckBox
    private lateinit var btnSave: Button

    private val ui = Handler(Looper.getMainLooper())
    private var previewTask: Runnable? = null
    private var paramTask: Runnable? = null

    // 预览源图缓存：形状/透明度变化时只重新合成，不重复解码大文件
    private var previewSource: android.graphics.Bitmap? = null
    private var previewFrames: PreviewFrames.Result? = null
    private var previewFramesLoading = false
    private var refillTask: Runnable? = null
    private val previewExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

    @Volatile
    private var previewing = false

    @Volatile
    private var pendingPreview = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)
        try {
            val attrs = window.attributes
            attrs.preferredRefreshRate = 0f
            window.attributes = attrs
        } catch (_: Throwable) {
        }

        val fromPin = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val editId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        widgetId = if (fromPin != AppWidgetManager.INVALID_APPWIDGET_ID) fromPin else editId
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        if (fromPin != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
        }

        val loaded = WidgetStore.load(this, widgetId) ?: WidgetConfig(widgetId = widgetId)
        base = loaded
        selectedShape = loaded.shape
        selectedLayout = loaded.layout
        selectedRotation = loaded.rotation
        selectedShapeRotation = loaded.shapeRotation
        selectedFitContent = loaded.fitContent
        if (loaded.frameWidth > 0 && loaded.frameHeight > 0) {
            sourceWidth = loaded.frameWidth
            sourceHeight = loaded.frameHeight
        }

        imgPreview = findViewById(R.id.img_preview)
        pbPreview = findViewById(R.id.pb_preview)
        seekPlayback = findViewById(R.id.seek_playback)
        tvPlayTime = findViewById(R.id.tv_play_time)
        tvResolution = findViewById(R.id.tv_resolution)
        tvPick = findViewById(R.id.tv_pick)
        progress = findViewById(R.id.pb_preview)
        tvFps = findViewById(R.id.tv_fps)
        tvEdge = findViewById(R.id.tv_edge)
        tvQuality = findViewById(R.id.tv_quality)
        tvAlpha = findViewById(R.id.tv_alpha)
        seekFps = findViewById(R.id.seek_fps)
        seekEdge = findViewById(R.id.seek_edge)
        seekQuality = findViewById(R.id.seek_quality)
        seekAlpha = findViewById(R.id.seek_alpha)
        seekScale = findViewById(R.id.seek_scale)
        seekShadow = findViewById(R.id.seek_shadow)
        btnRotate = findViewById(R.id.btn_rotate)
        btnRotateShape = findViewById(R.id.btn_rotate_shape)
        tvScale = findViewById(R.id.tv_scale)
        tvShadow = findViewById(R.id.tv_shadow)
        cbBorder = findViewById(R.id.cb_border)
        cbOverlay = findViewById(R.id.cb_overlay)
        cbOverlayVisible = findViewById(R.id.cb_overlay_visible)
        btnSave = findViewById(R.id.btn_save)

        findViewById<Button>(R.id.btn_adapt).setOnClickListener { applyAutoParams() }
        findViewById<Button>(R.id.btn_video).setOnClickListener { pick(MediaKind.VIDEO) }
        findViewById<Button>(R.id.btn_gif).setOnClickListener { pick(MediaKind.GIF) }
        findViewById<Button>(R.id.btn_image).setOnClickListener { pick(MediaKind.IMAGE) }
        findViewById<Button>(R.id.btn_overlay_edit).setOnClickListener { openOverlayEditor() }
        btnSave.setOnClickListener { save() }

        seekFps.progress = loaded.fps.coerceIn(1, 50) - 1
        seekEdge.progress = ((loaded.maxEdge.coerceIn(120, 1920) - 120) / 8)
        seekQuality.progress = loaded.quality.coerceIn(50, 100) - 50
        seekAlpha.progress = loaded.shapeAlpha.coerceIn(0, 100)
        seekScale.progress = loaded.scalePercent.coerceIn(0, 100)
        seekShadow.progress = loaded.shadowPercent.coerceIn(0, 100)
        cbBorder.isChecked = loaded.border
        cbOverlay.isChecked = loaded.overlayEnabled
        cbOverlayVisible.isChecked = loaded.overlayVisible

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progressValue: Int, fromUser: Boolean) {
                updateLabels()
                if (seekBar?.id == R.id.seek_alpha) schedulePreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar?.id == R.id.seek_alpha) schedulePreview()
            }
        }
        seekFps.setOnSeekBarChangeListener(listener)
        seekEdge.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                updateLabels()
                schedulePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        seekQuality.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                updateLabels()
                schedulePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        seekAlpha.setOnSeekBarChangeListener(listener)
        seekScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                updateLabels()
                refreshPreviewNow()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        seekShadow.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                updateLabels()
                refreshPreviewNow()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        // 点标题可以直接键盘输入数值
        tvScale.setOnClickListener {
            showNumberDialog(getString(R.string.cfg_scale_dialog), seekScale.progress) { value ->
                seekScale.progress = value
                updateLabels()
                refreshPreviewNow()
            }
        }
        tvShadow.setOnClickListener {
            showNumberDialog(getString(R.string.cfg_shadow_dialog), seekShadow.progress) { value ->
                seekShadow.progress = value
                updateLabels()
                refreshPreviewNow()
            }
        }
        cbBorder.setOnCheckedChangeListener { _, _ -> schedulePreview() }

        updateRotateButtons()
        refreshFitButton()
        findViewById<Button>(R.id.btn_fit_content).setOnClickListener {
            if (fitContentLocked()) return@setOnClickListener
            selectedFitContent = !selectedFitContent
            refreshFitButton()
            refreshPreviewNow()
        }
        btnRotate.setOnClickListener { showRotateDialog(false) }
        btnRotateShape.setOnClickListener { showRotateDialog(true) }

        findViewById<Button>(R.id.btn_shapes_toggle).setOnClickListener {
            shapesExpanded = !shapesExpanded
            findViewById<Button>(R.id.btn_shapes_toggle).text =
                if (shapesExpanded) "收起形状" else "展开全部形状"
            buildShapeGrid()
        }
        buildShapeGrid()

        val layoutChips = intArrayOf(
            R.id.ly0, R.id.ly1, R.id.ly2, R.id.ly3,
            R.id.ly4, R.id.ly5, R.id.ly6, R.id.ly7
        )
        LayoutMode.values().forEachIndexed { index, mode ->
            if (index >= layoutChips.size) return@forEachIndexed
            val chip = findViewById<TextView>(layoutChips[index])
            chip.text = mode.label
            chip.setOnClickListener {
                selectedLayout = mode
                updatePreviewBox()
                refreshLayoutChips(layoutChips)
                refreshFitButton()
                refreshPreviewNow()
            }
        }
        layoutChips.forEachIndexed { index, id ->
            findViewById<TextView>(id).visibility =
                if (index < LayoutMode.values().size) View.VISIBLE else View.GONE
        }
        refreshLayoutChips(layoutChips)

        if (loaded.sourceUri.isNotEmpty() && loaded.kind != MediaKind.IMAGE) {
            val sourceUri = Uri.parse(loaded.sourceUri)
            Thread {
                val resolution = SourceFps.resolution(this, sourceUri, loaded.kind)
                val fps = if (loaded.kind == MediaKind.GIF) {
                    SourceFps.gif(this, sourceUri)
                } else {
                    SourceFps.video(this, sourceUri)
                }
                val duration = SourceFps.duration(this, sourceUri, loaded.kind)
                ui.post {
                    resolution?.split("x")?.let { parts ->
                        if (parts.size == 2) {
                            sourceWidth = parts[0].toIntOrNull() ?: sourceWidth
                            sourceHeight = parts[1].toIntOrNull() ?: sourceHeight
                        }
                    }
                    sourceFps = fps
                    sourceDurationMs = duration ?: 0L
                    tvResolution.text = buildSourceInfo(resolution, fps, duration)
                }
            }.start()
        }

        if (loaded.sourceName.isNotEmpty()) {
            tvPick.text = getString(R.string.cfg_picked, loaded.sourceName)
            if (loaded.frameWidth > 0 && loaded.frameHeight > 0) {
                tvResolution.text = "分辨率 ${loaded.frameWidth}x${loaded.frameHeight}"
            }
        }
        applyKindUi(loaded.kind)
        updateLabels()
        renderPreviewNow()

        findViewById<android.widget.FrameLayout>(R.id.preview_frame)
            .addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updatePreviewBox() }

        seekPlayback.max = (loaded.frameCount - 1).coerceAtLeast(0)
        seekPlayback.isEnabled = loaded.frameCount > 1
        seekPlayback.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                if (fromUser) renderPreviewFrameAt(value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { playing = false }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if ((previewFrames?.frames?.size ?: 0) > 1) startPreviewFramesPlayback()
            }
        })
        if (loaded.frameCount > 1) startPlayback()

        findViewById<android.widget.FrameLayout>(R.id.preview_frame).post {
            ensureWallpaperPermission()
            applyDeskBackground()
            updatePreviewBox()
            applyPreviewClip()
        }
        val tabContent = findViewById<TextView>(R.id.tab_content)
        val tabAppearance = findViewById<TextView>(R.id.tab_appearance)
        val llContent = findViewById<android.widget.LinearLayout>(R.id.ll_content)
        val llAppearance = findViewById<android.widget.LinearLayout>(R.id.ll_appearance)

        val indicator = findViewById<View>(R.id.tab_indicator)
        val colors = ThemeManager.colors(this)
        indicator.setBackgroundColor(colors.primary)
        indicator.post {
            val half = (indicator.parent as View).width / 2
            indicator.layoutParams = indicator.layoutParams.apply { width = half }
            indicator.requestLayout()
            indicator.translationX = 0f
        }

        fun selectTab(content: Boolean, animated: Boolean = true) {
            val target = if (content) llContent else llAppearance
            val other = if (content) llAppearance else llContent
            tabContent.setTextColor(if (content) colors.primary else colors.textMuted)
            tabAppearance.setTextColor(if (content) colors.textMuted else colors.primary)
            indicator.animate()
                .translationX(if (content) 0f else indicator.width.toFloat())
                .setDuration(if (animated) 180L else 0L)
                .start()
            if (!animated) {
                other.visibility = View.GONE
                target.visibility = View.VISIBLE
                target.alpha = 1f
                return
            }
            other.animate().alpha(0f).setDuration(90).withEndAction {
                other.visibility = View.GONE
                other.alpha = 1f
                target.alpha = 0f
                target.visibility = View.VISIBLE
                target.animate().alpha(1f).setDuration(160).start()
            }.start()
        }
        tabContent.setOnClickListener { selectTab(true) }
        tabAppearance.setOnClickListener { selectTab(false) }

        ThemeManager.applyTo(findViewById(android.R.id.content))
        selectTab(true)
    }

    // ------------------------------------------------------------ 控件状态

    private fun ensureWallpaperPermission() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= 33) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try {
                requestPermissions(arrayOf(permission), 200)
            } catch (_: Throwable) {
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200) applyDeskBackground()
    }

    private fun applyPreviewClip() {
        val frame = findViewById<android.widget.FrameLayout>(R.id.preview_frame)
        val radius = 0f
        frame.clipToOutline = true
        frame.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
        frame.invalidateOutline()
    }

    private fun applyDeskBackground() {
        try {
            val drawable = android.app.WallpaperManager.getInstance(this).drawable
            if (drawable != null) {
                findViewById<android.widget.ImageView>(R.id.img_desk).setImageDrawable(drawable)
            }
        } catch (_: Throwable) {
        }
    }

    /** 按素材自身信息自动设置帧率/清晰度，质量默认 100%。 */
    private fun applyAutoParams() {
        val fps = sourceFps
        if (fps != null && fps > 0) {
            seekFps.progress = (fps.coerceIn(1, 50) - 1)
        }
        if (sourceWidth > 0 && sourceHeight > 0) {
            // 源分辨率超过最高档时，直接取最高档（2.5K）
            val longest = maxOf(sourceWidth, sourceHeight).coerceIn(120, 2560)
            seekEdge.progress = ((longest - 120) / 8).coerceIn(0, 305)
        }
        seekQuality.progress = 50
        updateLabels()
    }

    private fun formatTime(millis: Long): String {
        val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        val hundredths = (millis % 1000L) / 10L
        return "%d:%02d.%02d".format(minutes, seconds, hundredths)
    }

    private fun buildSourceInfo(resolution: String?, fps: Int?, durationMs: Long? = null): String {
        val parts = mutableListOf<String>()
        if (!resolution.isNullOrBlank()) parts.add("分辨率 $resolution")
        if (fps != null && fps > 0) parts.add("原帧率 $fps fps")
        if (durationMs != null && durationMs > 0) parts.add("时长 ${formatTime(durationMs)}")
        return parts.joinToString(" · ")
    }

    private fun updateEmptyHint() {
        val empty = previewSource == null && (previewFrames?.frames?.isEmpty() != false)
        findViewById<TextView>(R.id.tv_preview_empty)?.visibility =
            if (empty) View.VISIBLE else View.GONE
    }

    private fun updatePreviewBox() {
        val frame = findViewById<android.widget.FrameLayout>(R.id.preview_frame)
        val box = findViewById<android.widget.FrameLayout>(R.id.preview_box)
        val parentWidth = frame.width
        val parentHeight = frame.height
        if (parentWidth <= 0 || parentHeight <= 0) return
        val ratio = if (selectedLayout.ratio > 0f) selectedLayout.ratio else 16f / 9f
        var boxWidth = (parentWidth * 0.72f).toInt()
        var boxHeight = (boxWidth / ratio).toInt()
        if (boxHeight > parentHeight * 0.88f) {
            boxHeight = (parentHeight * 0.88f).toInt()
            boxWidth = (boxHeight * ratio).toInt()
        }
        box.layoutParams = android.widget.FrameLayout.LayoutParams(
            boxWidth, boxHeight, android.view.Gravity.CENTER
        )
        box.requestLayout()
    }

    private fun updateLabels() {
        tvFps.text = getString(R.string.cfg_fps_value, seekFps.progress + 1)
        tvEdge.text = getString(R.string.cfg_edge_value, 120 + seekEdge.progress * 8)
        tvQuality.text = getString(R.string.cfg_quality_value, 50 + seekQuality.progress)
        tvAlpha.text = getString(R.string.cfg_alpha_value, seekAlpha.progress)
        tvScale.text = getString(R.string.cfg_scale_value, seekScale.progress)
        tvShadow.text = getString(R.string.cfg_shadow_value, seekShadow.progress)
    }

    private fun updateRotateButtons() {
        btnRotate.text = "内容 ${selectedRotation}°"
        btnRotateShape.text = "蒙版 ${selectedShapeRotation}°"
    }

    /** 填充 / 原始比例下，「适配画面」由布局本身决定，按钮只作提示。 */
    private fun fitContentLocked(): Boolean =
        selectedLayout == LayoutMode.FILL || selectedLayout == LayoutMode.FIT

    private fun refreshFitButton() {
        val button = findViewById<Button>(R.id.btn_fit_content)
        val locked = fitContentLocked()
        button.isEnabled = !locked
        button.alpha = if (locked) 0.55f else 1f
        button.text = when {
            locked -> getString(R.string.cfg_fit_auto)
            selectedFitContent -> getString(R.string.cfg_fit_on)
            else -> getString(R.string.cfg_fit_off)
        }
        button.isSelected = selectedFitContent
    }

    /** 参数变化：稍作防抖后重绘当前预览帧（没有预览帧就回到普通预览）。 */
    private fun refreshPreviewNow() {
        paramTask?.let { ui.removeCallbacks(it) }
        val task = Runnable {
            val frames = previewFrames
            if (frames != null && frames.frames.isNotEmpty()) {
                renderPreviewFrameAt(seekPlayback.progress)
            } else {
                schedulePreview()
            }
        }
        paramTask = task
        ui.postDelayed(task, 90L)
    }

    /** 0–100 的数值输入框。 */
    private fun showNumberDialog(title: String, value: Int, onApply: (Int) -> Unit) {
        val density = resources.displayMetrics.density
        val input = android.widget.EditText(this).apply {
            setText(value.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine()
            selectAll()
        }
        val box = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val p = (20 * density).toInt()
            setPadding(p, (8 * density).toInt(), p, 0)
            addView(input)
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("$title（0–100）")
            .setView(box)
            .setPositiveButton("应用") { _, _ ->
                val typed = input.text.toString().trim().toIntOrNull() ?: value
                onApply(typed.coerceIn(0, 100))
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showRotateDialog(isShape: Boolean) {
        val density = resources.displayMetrics.density
        val original = if (isShape) selectedShapeRotation else selectedRotation
        val seek = SeekBar(this).apply {
            max = 720
            progress = original + 360
        }
        val label = TextView(this).apply {
            text = "角度 $original°"
            gravity = android.view.Gravity.CENTER
            setPadding(0, (10 * density).toInt(), 0, (8 * density).toInt())
        }
        val input = android.widget.EditText(this).apply {
            setText(original.toString())
            hint = "-360 ~ 360"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            setSingleLine()
        }
        val box = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val p = (16 * density).toInt()
            setPadding(p, p, p, 0)
            addView(label)
            addView(seek)
            addView(input)
        }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                val angle = value - 360
                label.text = "角度 $angle°"
                input.setText(angle.toString())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        android.app.AlertDialog.Builder(this)
            .setTitle(if (isShape) "蒙版旋转（-360° ~ 360°）" else "内容旋转（-360° ~ 360°）")
            .setView(box)
            .setPositiveButton("应用") { _, _ ->
                val typed = input.text.toString().trim().toIntOrNull()
                val angle = (typed ?: (seek.progress - 360)).coerceIn(-360, 360)
                if (isShape) selectedShapeRotation = angle else selectedRotation = angle
                updateRotateButtons()
                refreshPreviewNow()
            }
            .setNegativeButton("取消") { _, _ ->
                if (isShape) selectedShapeRotation = original else selectedRotation = original
            }
            .show()
    }

    private fun buildShapeGrid() {
        val container = findViewById<android.widget.LinearLayout>(R.id.ll_shapes)
        container.removeAllViews()
        val list = if (shapesExpanded) ShapeKind.values().toList() else ShapeKind.values().take(8)
        val density = resources.displayMetrics.density
        var row: android.widget.LinearLayout? = null
        list.forEachIndexed { index, shape ->
            if (index % 4 == 0) {
                row = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                }
                container.addView(row)
            }
            val chip = ShapeChipView(this).apply {
                this.shape = shape
                this.chosen = shape == this@ConfigActivity.selectedShape
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0, (64 * density).toInt(), 1f
                ).apply {
                    val m = (3 * density).toInt()
                    setMargins(m, m, m, m)
                }
                setOnClickListener {
                    selectedShape = shape
                    buildShapeGrid()
                    schedulePreview()
                    if ((previewFrames?.frames?.size ?: 0) > 0) {
                        renderPreviewFrameAt(seekPlayback.progress)
                    }
                }
            }
            row?.addView(chip)
        }
    }

    private fun refreshLayoutChips(chips: IntArray) {
        LayoutMode.values().forEachIndexed { index, mode ->
            if (index >= chips.size) return@forEachIndexed
            findViewById<TextView>(chips[index]).isSelected = mode == selectedLayout
        }
    }

    private fun applyKindUi(kind: MediaKind) {
        val enabled = kind != MediaKind.IMAGE
        seekFps.isEnabled = enabled
        tvFps.alpha = if (enabled) 1f else 0.35f

        // 覆盖层只支持视频/GIF；图片时置灰并取消勾选
        val overlayAllowed = kind != MediaKind.IMAGE
        cbOverlay.isEnabled = overlayAllowed
        cbOverlayVisible.isEnabled = overlayAllowed
        if (!overlayAllowed) {
            cbOverlay.isChecked = false
            cbOverlayVisible.isChecked = false
        }
    }

    // ------------------------------------------------------------ 选素材

    private fun pick(kind: MediaKind) {
        pendingKind = kind
        val mime = when (kind) {
            MediaKind.VIDEO -> "video/*"
            MediaKind.GIF -> "image/gif"
            MediaKind.IMAGE -> "image/*"
        }
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mime
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try {
            startActivityForResult(intent, REQUEST_PICK)
        } catch (t: Throwable) {
            toast(t.message ?: getString(R.string.err_no_frames))
        }
    }

    @Deprecated("ActivityResult 需要 AndroidX，这里刻意零依赖")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PICK || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Throwable) {
        }
        pickedUri = uri
        pickedKind = pendingKind
        pickedName = displayName(uri)
        loadPreview(uri, pendingKind)
        val kind = pendingKind
        tvPick.text = getString(R.string.cfg_picked, pickedName ?: "")
        applyKindUi(kind)

        // 后台读取源帧率：视频用 MediaExtractor，GIF 解析帧延迟
        Thread {
            val fps = when (kind) {
                MediaKind.GIF -> SourceFps.gif(this, uri)
                MediaKind.VIDEO -> SourceFps.video(this, uri)
                else -> null
            }
            val resolution = SourceFps.resolution(this, uri, kind)
            val duration = SourceFps.duration(this, uri, kind)
            ui.post {
                tvResolution.text = buildSourceInfo(resolution, fps, duration)
                if (duration != null && duration > 40_000L) {
                    toast("素材超过 40 秒，已截取前 40 秒")
                }
                sourceFps = fps
                sourceDurationMs = duration ?: 0L
                resolution?.split("x")?.let { parts ->
                    if (parts.size == 2) {
                        sourceWidth = parts[0].toIntOrNull() ?: 0
                        sourceHeight = parts[1].toIntOrNull() ?: 0
                    }
                }
                applyAutoParams()
                loadPreviewFrames(uri, kind)
                if (fps != null && fps > 0) {
                    tvPick.text = getString(
                        R.string.cfg_picked_fps,
                        pickedName ?: "",
                        fps
                    )
                }
            }
        }.start()
    }

    private fun displayName(uri: Uri): String {
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) return cursor.getString(index) ?: ""
                    }
                }
        } catch (_: Throwable) {
        }
        return uri.lastPathSegment ?: ""
    }

    // ------------------------------------------------------------ 保存

    private fun save() {
        if (busy) return
        val current = base ?: return
        val kind = pickedKind ?: current.kind
        val sourceUri = pickedUri?.toString() ?: current.sourceUri
        if (sourceUri.isEmpty()) {
            toast(getString(R.string.cfg_need_source))
            return
        }

        val config = WidgetConfig(
            widgetId = widgetId,
            kind = kind,
            sourceName = pickedName ?: current.sourceName,
            sourceUri = sourceUri,
            fps = seekFps.progress + 1,
            maxEdge = 120 + seekEdge.progress * 8,
            quality = 50 + seekQuality.progress,
            frameCount = current.frameCount,
            frameWidth = current.frameWidth,
            frameHeight = current.frameHeight,
            shape = selectedShape,
            shapeAlpha = seekAlpha.progress,
            border = cbBorder.isChecked,
            layout = selectedLayout,
            rotation = selectedRotation,
            shapeRotation = selectedShapeRotation,
            fitContent = selectedFitContent,
            shadowPercent = seekShadow.progress,
            overlayEnabled = cbOverlay.isChecked && kind != MediaKind.IMAGE,
            overlayVisible = cbOverlayVisible.isChecked &&
                cbOverlay.isChecked && kind != MediaKind.IMAGE,
            scalePercent = seekScale.progress,
            overlayX = current.overlayX,
            overlayY = current.overlayY,
            overlayWidth = current.overlayWidth
        )

        val needsImport = pickedUri != null || current.frameCount == 0 ||
            current.sourceUri != sourceUri || current.kind != kind
        if (needsImport) {
            importAsync(config, Uri.parse(sourceUri))
        } else {
            WidgetStore.save(this, config)
            finishSave(config)
        }
    }

    private fun importAsync(config: WidgetConfig, uri: Uri) {
        setBusy(true)
        Thread {
            try {
                val result = MediaImporter.import(this, config, uri) { done, total ->
                    ui.post {
                        progress.max = total
                        progress.progress = done
                        tvPick.text = getString(R.string.cfg_importing, done, total)
                    }
                }
                val done = config.copy(
                    frameCount = result.frameCount,
                    frameWidth = result.width,
                    frameHeight = result.height
                )
                WidgetStore.save(this, done)
                ui.post {
                    setBusy(false)
                    finishSave(done)
                }
            } catch (t: Throwable) {
                ui.post {
                    setBusy(false)
                    toast(getString(R.string.status_failed, t.message ?: t.javaClass.simpleName))
                }
            }
        }.start()
    }

    private fun finishSave(config: WidgetConfig) {
        try {
            val manager = AppWidgetManager.getInstance(this)
            WidgetRender.render(this, manager, config, 0, null)
        } catch (_: Throwable) {
        }
        getSharedPreferences("settings", MODE_PRIVATE)
            .edit()
            .putBoolean("all_stopped", false)
            .putInt("last_overlay_id", if (config.overlayEnabled) config.widgetId else
                getSharedPreferences("settings", MODE_PRIVATE).getInt("last_overlay_id", -1))
            .apply()
        PlaybackService.start(this)
        OverlayService.refresh(this)
        if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            setResult(RESULT_OK, result)
        }
        toast(getString(R.string.cfg_saved))
        finish()
    }

    private fun openOverlayEditor() {
        val current = base ?: return
        if (current.frameCount <= 0) {
            toast(getString(R.string.cfg_need_source))
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            toast(getString(R.string.overlay_permission))
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (_: Throwable) {
            }
            return
        }
        getSharedPreferences("settings", MODE_PRIVATE)
            .edit()
            .putBoolean("all_stopped", false)
            .putInt("last_overlay_id", widgetId)
            .apply()
        val updated = current.copy(overlayEnabled = true, overlayVisible = true)
        WidgetStore.save(this, updated)
        OverlayService.edit(this, widgetId)
        finish()
    }

    private fun setBusy(value: Boolean) {
        busy = value
        btnSave.isEnabled = !value
        progress.visibility = if (value) View.VISIBLE else View.GONE
        if (!value) {
            progress.progress = 0
        }
    }

    // ------------------------------------------------------------ 预览

    /** 选完素材立即解码一张低分辨率首帧，不等保存/导入。 */
    private fun loadPreview(uri: Uri, kind: MediaKind) {
        Thread {
            val bitmap = try {
                when (kind) {
                    MediaKind.VIDEO -> {
                        val retriever = android.media.MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(this, uri)
                            retriever.getFrameAtTime(0)
                        } finally {
                            retriever.release()
                        }
                    }
                    MediaKind.GIF -> {
                        val movie = contentResolver.openInputStream(uri)?.use {
                            android.graphics.Movie.decodeStream(it)
                        }
                        if (movie != null) {
                            val w = movie.width().coerceAtLeast(1)
                            val h = movie.height().coerceAtLeast(1)
                            val b = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                            movie.setTime(0)
                            movie.draw(android.graphics.Canvas(b), 0f, 0f)
                            b
                        } else null
                    }
                    else -> contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(
                            it, null,
                            BitmapFactory.Options().apply { inSampleSize = 2 }
                        )
                    }
                }
            } catch (_: Throwable) {
                null
            }
            ui.post {
                previewSource?.recycle()
                previewSource = bitmap
                schedulePreview()
            }
        }.start()
    }

    private var playing = false
    private var playIndex = 0

    private fun startPlayback() {
        val config = base ?: return
        if (config.frameCount <= 1) return
        playing = true
        val tick = object : Runnable {
            override fun run() {
                if (!playing) return
                renderPlaybackFrame(playIndex % config.frameCount)
                playIndex++
                ui.postDelayed(this, (1000L / config.fps.coerceIn(1, 50)).coerceAtLeast(16L))
            }
        }
        ui.post(tick)
    }

    private fun renderPlaybackFrame(index: Int) {
        val config = base ?: return
        if (config.frameCount <= 0) return
        Thread {
            val frame = WidgetStore.loadFrame(this, config, index, 720, 720) ?: return@Thread
            val out = try {
                PreviewRenderer.render(
                    frame,
                    imgPreview.width.coerceAtLeast(360),
                    imgPreview.height.coerceAtLeast(180),
                    selectedLayout, selectedShape, seekAlpha.progress,
                    cbBorder.isChecked,
                    120 + seekEdge.progress * 8,
                    50 + seekQuality.progress,
                    seekScale.progress,
                    selectedRotation,
                    selectedShapeRotation,
                    selectedFitContent,
                    seekShadow.progress
                )
            } finally {
                frame.recycle()
            }
            ui.post {
                imgPreview.setImageBitmap(out)
                seekPlayback.progress = index
                tvPlayTime.text = formatTime(index.toLong() * 1000L / config.fps.coerceAtLeast(1))
            }
        }.start()
    }

    private fun loadPreviewFrames(uri: Uri, kind: MediaKind) {
        if (previewFramesLoading) return
        previewFramesLoading = true
        playing = false
        pbPreview.visibility = View.VISIBLE
        val fps = 1 + seekFps.progress
        val edge = 120 + seekEdge.progress * 8
        val quality = 50 + seekQuality.progress

        Thread {
            val result = PreviewFrames.load(this, uri, kind, fps, edge, quality) { done, total ->
                ui.post {
                    pbPreview.visibility = View.VISIBLE
                    tvPlayTime.text = "解析预览 %d/%d".format(done, total)
                }
            }
            val old = previewFrames?.frames
            ui.post {
                previewFramesLoading = false
                previewFrames = result
                old?.forEach { if (!it.isRecycled) it.recycle() }
                pbPreview.visibility = View.GONE
                applyPreviewFrames()
            }
        }.start()
    }

    private fun applyPreviewFrames() {
        val result = previewFrames ?: return
        if (result.frames.isEmpty()) return
        seekPlayback.max = (result.frames.size - 1).coerceAtLeast(0)
        seekPlayback.isEnabled = result.frames.size > 1
        playIndex = 0
        renderPreviewFrameAt(0)
        if (result.frames.size > 1) startPreviewFramesPlayback()
    }

    private fun startPreviewFramesPlayback() {
        val result = previewFrames ?: return
        if (result.frames.size <= 1) return
        playing = true
        val interval = (1000L / result.fps.coerceIn(1, 50)).coerceAtLeast(16L)
        val tick = object : Runnable {
            override fun run() {
                if (!playing) return
                val current = previewFrames ?: return
                if (current.frames.size <= 1) return
                val index = playIndex % current.frames.size
                renderPreviewFrameAt(index)
                seekPlayback.progress = index
                tvPlayTime.text = formatTime(
                    index.toLong() * 1000L / current.fps.coerceAtLeast(1)
                )
                playIndex = (playIndex + 1) % current.frames.size
                ui.postDelayed(this, interval)
            }
        }
        ui.post(tick)
    }

    private fun renderPreviewFrameAt(index: Int) {
        val result = previewFrames ?: return
        if (result.frames.isEmpty()) return
        val safe = ((index % result.frames.size) + result.frames.size) % result.frames.size
        val source = result.frames[safe]
        if (source.isRecycled) return
        val width = imgPreview.width.coerceAtLeast(360)
        val height = imgPreview.height.coerceAtLeast(180)
        previewExecutor.execute {
            val out = try {
                PreviewRenderer.render(
                    source, width, height,
                    selectedLayout, selectedShape, seekAlpha.progress,
                    cbBorder.isChecked,
                    120 + seekEdge.progress * 8,
                    50 + seekQuality.progress,
                    seekScale.progress,
                    selectedRotation,
                    selectedShapeRotation,
                    selectedFitContent,
                    seekShadow.progress
                )
            } catch (_: Throwable) {
                null
            } ?: return@execute
            ui.post {
                imgPreview.setImageBitmap(out)
                seekPlayback.progress = safe
                tvPlayTime.text = formatTime(
                    safe.toLong() * 1000L / result.fps.coerceAtLeast(1)
                )
            }
        }
    }

    /** 清晰度/质量变化后，500ms 防抖重新抽预览帧。 */
    private fun scheduleRefillFrames() {
        val uri = pickedUri ?: base?.sourceUri?.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }
        val kind = pickedKind ?: base?.kind ?: return
        if (uri == null) return
        refillTask?.let { ui.removeCallbacks(it) }
        val task = Runnable { loadPreviewFrames(uri, kind) }
        refillTask = task
        ui.postDelayed(task, 500L)
    }

    private fun schedulePreview() {
        pbPreview.visibility = View.VISIBLE
        previewTask?.let { ui.removeCallbacks(it) }
        val task = Runnable { renderPreviewNow() }
        previewTask = task
        ui.postDelayed(task, 120L)
    }

    private fun renderPreviewNow() {
        if (previewing) {
            pendingPreview = true
            return
        }
        val current = base ?: run {
            pbPreview.visibility = View.GONE
            return
        }
        val config = current.copy(
            shape = selectedShape,
            shapeAlpha = seekAlpha.progress,
            border = cbBorder.isChecked,
            layout = selectedLayout
        )
        val cached = previewSource
        if ((cached == null || cached.isRecycled) && config.frameCount <= 0) {
            pbPreview.visibility = View.GONE
            return
        }

        previewing = true
        Thread {
            try {
                val source: android.graphics.Bitmap = if (cached != null && !cached.isRecycled) {
                    cached
                } else {
                    val loaded = WidgetStore.loadFrame(this, config, 0, 720, 720)
                        ?: return@Thread
                    previewSource = loaded
                    loaded
                }

                val viewWidth = imgPreview.width.coerceAtLeast(360)
                val viewHeight = imgPreview.height.coerceAtLeast(180)
                val out = PreviewRenderer.render(
                    source,
                    viewWidth,
                    viewHeight,
                    selectedLayout,
                    selectedShape,
                    seekAlpha.progress,
                    cbBorder.isChecked,
                    120 + seekEdge.progress * 8,
                    50 + seekQuality.progress,
                    seekScale.progress,
                    selectedRotation,
                    selectedShapeRotation,
                    selectedFitContent,
                    seekShadow.progress
                )
                ui.post {
                    updateEmptyHint()
                    imgPreview.setImageBitmap(out)
                    pbPreview.visibility = View.GONE
                }
            } catch (_: Throwable) {
            } finally {
                previewing = false
                if (pendingPreview) {
                    pendingPreview = false
                    ui.post { renderPreviewNow() }
                } else {
                    ui.post { pbPreview.visibility = View.GONE }
                }
            }
        }.start()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
