package io.github.animwidget

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView

/** 外观设置：深色模式、预设、对比度、反转、五个语义色。 */
class SettingsActivity : Activity() {

    private lateinit var tvContrast: TextView
    private lateinit var seekContrast: SeekBar
    private lateinit var cbInvert: CheckBox
    private lateinit var llPresets: LinearLayout
    private lateinit var llColors: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tvContrast = findViewById(R.id.tv_contrast)
        seekContrast = findViewById(R.id.seek_contrast)
        cbInvert = findViewById(R.id.cb_invert)
        llPresets = findViewById(R.id.ll_presets)
        llColors = findViewById(R.id.ll_colors)

        findViewById<Button>(R.id.btn_dev_info).setOnClickListener { showDevInfo() }
        findViewById<Button>(R.id.btn_reward).setOnClickListener { showReward() }

        val modeGroup = findViewById<RadioGroup>(R.id.rg_mode)
        when (ThemePrefs.getMode(this)) {
            ThemePrefs.MODE_LIGHT -> modeGroup.check(R.id.rb_light)
            ThemePrefs.MODE_DARK -> modeGroup.check(R.id.rb_dark)
            else -> modeGroup.check(R.id.rb_system)
        }
        modeGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rb_light -> ThemePrefs.MODE_LIGHT
                R.id.rb_dark -> ThemePrefs.MODE_DARK
                else -> ThemePrefs.MODE_SYSTEM
            }
            ThemePrefs.setMode(this, mode)
            recreate()
        }

        seekContrast.progress = ThemePrefs.getContrast(this) + 10
        updateContrastLabel()
        seekContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                ThemePrefs.setContrast(this@SettingsActivity, value - 10)
                updateContrastLabel()
                ThemeManager.applyTo(findViewById(android.R.id.content))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        cbInvert.isChecked = ThemePrefs.getInvert(this)
        cbInvert.setOnCheckedChangeListener { _, checked ->
            ThemePrefs.setInvert(this, checked)
            ThemeManager.applyTo(findViewById(android.R.id.content))
        }

        buildPresets()
        buildColorRows()

        findViewById<Button>(R.id.btn_reset_theme).setOnClickListener {
            ThemePrefs.clearCustom(this)
            ThemePrefs.setPreset(this, 0)
            ThemePrefs.setContrast(this, 0)
            ThemePrefs.setInvert(this, false)
            recreate()
        }
        findViewById<Button>(R.id.btn_close_settings).setOnClickListener { finish() }

        ThemeManager.applyTo(findViewById(android.R.id.content))
    }

    private fun showDevInfo() {
        val qq = "188019597"
        AlertDialog.Builder(this)
            .setTitle("开发者信息")
            .setMessage("开发者：Fox\nQQ：$qq\n版本：0.9.9\n\n点击「复制 QQ」可复制号码")
            .setPositiveButton("复制 QQ") { _, _ ->
                val clipboard = getSystemService(android.content.ClipboardManager::class.java)
                clipboard?.setPrimaryClip(
                    android.content.ClipData.newPlainText("QQ", qq)
                )
                android.widget.Toast.makeText(this, "已复制 QQ：$qq", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showReward() {
        val density = resources.displayMetrics.density
        val image = android.widget.ImageView(this).apply {
            setImageResource(R.drawable.reward_qr)
            adjustViewBounds = true
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            val size = (240 * density).toInt()
            layoutParams = android.widget.LinearLayout.LayoutParams(size, size)
        }
        val container = android.widget.FrameLayout(this).apply {
            val p = (16 * density).toInt()
            setPadding(p, p, p, p)
            addView(image)
        }
        AlertDialog.Builder(this)
            .setTitle("请我喝杯咖啡 ☕")
            .setView(container)
            .setPositiveButton("关闭", null)
            .show()
    }

    private fun updateContrastLabel() {
        tvContrast.text = "对比度：${ThemePrefs.getContrast(this)}"
    }

    private fun buildPresets() {
        llPresets.removeAllViews()
        val density = resources.displayMetrics.density
        val selected = ThemePrefs.getPreset(this)
        ThemeManager.presets.forEachIndexed { index, preset ->
            val view = View(this)
            val size = (40 * density).toInt()
            val margin = (6 * density).toInt()
            view.layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(margin, margin, margin, margin)
            }
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(preset.primary)
                if (index == selected) {
                    setStroke((3 * density).toInt(), 0xFF111318.toInt())
                }
            }
            view.background = drawable
            view.setOnClickListener {
                ThemePrefs.setPreset(this, index)
                recreate()
            }
            llPresets.addView(view)
        }
    }

    private fun buildColorRows() {
        llColors.removeAllViews()
        val colors = ThemeManager.colors(this)
        addColorRow("主色", "primary", colors.primary)
        addColorRow("辅助色", "secondary", colors.secondary)
        addColorRow("第三色", "tertiary", colors.tertiary)
        addColorRow("表面", "surface", colors.surface)
        addColorRow("中性变体", "neutral", colors.neutral)
    }

    private fun addColorRow(label: String, key: String, color: Int) {
        val density = resources.displayMetrics.density
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (8 * density).toInt(), 0, (8 * density).toInt())
        }
        val text = TextView(this).apply {
            this.text = label
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val swatch = View(this).apply {
            layoutParams = LinearLayout.LayoutParams((36 * density).toInt(), (36 * density).toInt())
            background = GradientDrawable().apply {
                cornerRadius = 10f * density
                setColor(color)
                setStroke((1 * density).toInt(), 0xFF808080.toInt())
            }
        }
        swatch.setOnClickListener { showPalette(label, key) }
        row.addView(text)
        row.addView(swatch)
        llColors.addView(row)
    }

    private fun showPalette(label: String, key: String) {
        val density = resources.displayMetrics.density
        val colors = intArrayOf(
            0xFF16A34A.toInt(), 0xFF15803D.toInt(), 0xFF4F46E5.toInt(), 0xFF7C3AED.toInt(),
            0xFF9333EA.toInt(), 0xFFDB2777.toInt(), 0xFFE11D48.toInt(), 0xFFF97316.toInt(),
            0xFFF59E0B.toInt(), 0xFF65A30D.toInt(), 0xFF0D9488.toInt(), 0xFF06B6D4.toInt(),
            0xFF0EA5E9.toInt(), 0xFF2563EB.toInt(), 0xFF111827.toInt(), 0xFF374151.toInt(),
            0xFF6B7280.toInt(), 0xFF9CA3AF.toInt(), 0xFFE7EAEE.toInt(), 0xFFFFFFFF.toInt(),
            0xFFB45309.toInt(), 0xFFD97706.toInt(), 0xFF059669.toInt(), 0xFF0891B2.toInt()
        )
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (rowIndex in 0 until 4) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (col in 0 until 6) {
                val index = rowIndex * 6 + col
                val swatch = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, (44 * density).toInt(), 1f).apply {
                        val m = (4 * density).toInt()
                        setMargins(m, m, m, m)
                    }
                    background = GradientDrawable().apply {
                        cornerRadius = 8f * density
                        setColor(colors[index])
                        setStroke((1 * density).toInt(), 0xFF808080.toInt())
                    }
                    setOnClickListener {
                        ThemePrefs.setCustom(this@SettingsActivity, key, colors[index])
                        recreate()
                    }
                }
                row.addView(swatch)
            }
            grid.addView(row)
        }
        val hex = EditText(this).apply {
            hint = "#RRGGBB"
            setSingleLine()
            val m = (8 * density).toInt()
            setPadding(m, m, m, m)
        }
        val picker = HsvPickerView(this).apply {
            color = ThemeManager.colors(this@SettingsActivity).primary
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (220 * density).toInt()
            )
            setOnTouchListener { _, _ -> false }
        }
        // 拖动取色时同步 hex 文本
        picker.setOnTouchListener { v, e ->
            v.onTouchEvent(e)
            hex.setText("#%06X".format(0xFFFFFF and picker.color))
            true
        }
        val current = ThemeManager.colors(this).primary
        hex.setText("#%06X".format(0xFFFFFF and current))
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((12 * density).toInt(), 0, (12 * density).toInt(), 0)
            addView(picker)
            addView(hex)
            addView(grid)
        }
        AlertDialog.Builder(this)
            .setTitle(label)
            .setView(container)
            .setPositiveButton("应用") { _, _ ->
                val raw = hex.text.toString().trim().removePrefix("#")
                val value = try {
                    if (raw.length == 6) (0xFF000000 or raw.toLong(16)).toInt() else 0
                } catch (_: Throwable) {
                    0
                }
                if (value != 0) {
                    ThemePrefs.setCustom(this, key, value)
                    recreate()
                }
            }
            .setNeutralButton("恢复预设") { _, _ ->
                ThemePrefs.setCustom(this, key, 0)
                recreate()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
