package io.github.animwidget

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var llWidgets: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var tvBackend: TextView
    private lateinit var tvDiag: TextView
    private lateinit var btnToggle: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ShizukuHelper.init()

        llWidgets = findViewById(R.id.ll_widgets)
        tvEmpty = findViewById(R.id.tv_empty)
        tvBackend = findViewById(R.id.tv_backend)
        tvDiag = findViewById(R.id.tv_diag)
        btnToggle = findViewById(R.id.btn_toggle)

        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.btn_add).setOnClickListener { addWidget() }
        findViewById<Button>(R.id.btn_shizuku).setOnClickListener { runShizuku() }
        findViewById<Button>(R.id.btn_battery).setOnClickListener { requestBatteryWhitelist() }

        val anchor = findViewById<CheckBox>(R.id.cb_anchor)
        anchor.isChecked = getSharedPreferences("settings", MODE_PRIVATE)
            .getBoolean("anchor_enabled", true)
        anchor.setOnCheckedChangeListener { _, checked ->
            getSharedPreferences("settings", MODE_PRIVATE)
                .edit().putBoolean("anchor_enabled", checked).apply()
            if (checked) OverlayService.refresh(this)
        }
        findViewById<Button>(R.id.btn_usage).setOnClickListener { requestUsageAccess() }
        btnToggle.setOnClickListener { togglePlayback() }
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyTo(findViewById(android.R.id.content))
        refresh()
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        if (!prefs.getBoolean("all_stopped", false) &&
            prefs.getBoolean("anchor_enabled", true) &&
            Settings.canDrawOverlays(this)
        ) {
            OverlayService.refresh(this)
        }
        updateDiagnostics()
    }

    private fun refresh() {
        val configs = WidgetStore.all(this)
        llWidgets.removeAllViews()
        tvEmpty.visibility = if (configs.isEmpty()) View.VISIBLE else View.GONE

        val liveIds = WidgetProvider.widgetIds(this).toSet()
        for (config in configs) {
            llWidgets.addView(buildRow(config, config.widgetId in liveIds))
        }
        val stopped = getSharedPreferences("settings", MODE_PRIVATE)
            .getBoolean("all_stopped", false)
        btnToggle.text = getString(
            if (stopped) R.string.main_start_all else R.string.main_stop_all
        )
    }

    private fun buildRow(config: WidgetConfig, installed: Boolean): View {
        val density = resources.displayMetrics.density
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((12 * density).toInt(), (10 * density).toInt(), (8 * density).toInt(), (10 * density).toInt())
            setBackgroundResource(R.drawable.card_bg)
            tag = "card"
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = (8 * density).toInt()
            layoutParams = lp
        }
        val info = TextView(this).apply {
            text = getString(
                if (installed) R.string.main_row else R.string.main_row_missing,
                config.sourceName.ifBlank { config.kind.name },
                config.shape.label,
                config.frameCount,
                config.fps
            )
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val edit = Button(this).apply {
            text = getString(R.string.main_edit)
            setOnClickListener { openConfig(config.widgetId) }
        }
        val delete = Button(this).apply {
            text = getString(R.string.main_delete)
            setOnClickListener { confirmDelete(config) }
        }
        row.addView(info)
        row.addView(edit)
        row.addView(delete)
        return row
    }

    private fun addWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, WidgetProvider::class.java)
        if (!manager.isRequestPinAppWidgetSupported) {
            toast(getString(R.string.main_add_manual))
            return
        }
        val requested = try {
            manager.requestPinAppWidget(provider, null, null)
        } catch (_: Throwable) {
            false
        }
        if (!requested) toast(getString(R.string.main_add_manual))
    }

    private fun openConfig(widgetId: Int) {
        startActivity(
            Intent(this, ConfigActivity::class.java)
                .putExtra(ConfigActivity.EXTRA_WIDGET_ID, widgetId)
        )
    }

    private fun confirmDelete(config: WidgetConfig) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.main_delete_confirm, config.sourceName))
            .setPositiveButton(R.string.main_delete) { _, _ ->
                WidgetStore.delete(this, config.widgetId)
                refresh()
                toast(getString(R.string.main_deleted))
                // 桌面上的小部件需要用户在桌面上移除
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun togglePlayback() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val stopped = prefs.getBoolean("all_stopped", false)
        if (!stopped) {
            prefs.edit().putBoolean("all_stopped", true).apply()
            PlaybackService.stop(this)
            OverlayService.stop(this)
            toast(getString(R.string.main_stopped_all))
        } else {
            prefs.edit().putBoolean("all_stopped", false).apply()
            if (WidgetProvider.widgetIds(this).isNotEmpty()) PlaybackService.start(this)
            OverlayService.refresh(this)
            toast(getString(R.string.main_started_all))
        }
        btnToggle.postDelayed({ refresh() }, 400L)
    }

    private fun updateDiagnostics() {
        DeskWatcher.tick(this)
        tvDiag.text = buildString {
            append("桌面监测：")
            append(if (DeskWatcher.hasAccess(this@MainActivity)) "已授权" else "未授权")
            append("\n桌面包名：").append(DeskWatcher.launcher(this@MainActivity) ?: "未知")
            append("\n当前前台：").append(DeskWatcher.foreground() ?: "未知")
            append("\n播放条件：").append(if (DeskWatcher.canPlay) "满足" else "不满足（需停留 3 秒）")
        }
    }

    // ------------------------------------------------------------ Shizuku

    private fun runShizuku() {
        ShizukuHelper.init()
        if (!ShizukuHelper.isReady()) {
            tvBackend.text = getString(R.string.shizuku_missing)
            return
        }
        ShizukuHelper.requestPermission(this) { granted ->
            runOnUiThread {
                if (!granted) {
                    tvBackend.text = getString(R.string.shizuku_denied)
                    return@runOnUiThread
                }
                tvBackend.text = getString(R.string.shizuku_running)
                val pkg = packageName
                val launcher = resolveLauncherPackage()
                Thread {
                    val report = ShizukuHelper.runCommands(
                        ShizukuHelper.backgroundCommands(pkg, launcher)
                    )
                    runOnUiThread { tvBackend.text = report }
                }.start()
            }
        }
    }

    private fun resolveLauncherPackage(): String? {
        return try {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            packageManager
                .resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo
                ?.packageName
        } catch (_: Throwable) {
            null
        }
    }

    private fun requestUsageAccess() {
        if (DeskWatcher.hasAccess(this)) {
            tvBackend.text = getString(R.string.usage_ok)
            return
        }
        try {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } catch (t: Throwable) {
            tvBackend.text = getString(R.string.usage_ok)
        }
    }

    private fun requestBatteryWhitelist() {
        val power = getSystemService(PowerManager::class.java)
        if (power != null && power.isIgnoringBatteryOptimizations(packageName)) {
            tvBackend.text = getString(R.string.battery_ok)
            return
        }
        try {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        } catch (t: Throwable) {
            toast(t.message ?: getString(R.string.battery_failed))
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
