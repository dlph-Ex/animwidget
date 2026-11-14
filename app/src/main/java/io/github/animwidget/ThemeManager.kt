package io.github.animwidget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView

/** 主题偏好：深色模式、预设、对比度、反转、五个语义色。 */
object ThemePrefs {
    private const val PREF = "theme_presets"
    const val MODE_SYSTEM = 0
    const val MODE_LIGHT = 1
    const val MODE_DARK = 2

    private fun p(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun getMode(context: Context): Int = p(context).getInt("mode", MODE_SYSTEM)
    fun setMode(context: Context, value: Int) = p(context).edit().putInt("mode", value).apply()

    fun getPreset(context: Context): Int = p(context).getInt("preset", 0)
    fun setPreset(context: Context, value: Int) = p(context).edit().putInt("preset", value).apply()

    fun getContrast(context: Context): Int = p(context).getInt("contrast", 0)
    fun setContrast(context: Context, value: Int) = p(context).edit().putInt("contrast", value).apply()

    fun getInvert(context: Context): Boolean = p(context).getBoolean("invert", false)
    fun setInvert(context: Context, value: Boolean) = p(context).edit().putBoolean("invert", value).apply()

    fun getCustom(context: Context, key: String): Int = p(context).getInt(key, 0)
    fun setCustom(context: Context, key: String, value: Int) = p(context).edit().putInt(key, value).apply()

    fun clearCustom(context: Context) {
        p(context).edit()
            .remove("primary").remove("secondary").remove("tertiary")
            .remove("surface").remove("neutral")
            .apply()
    }

}

data class ThemeColors(
    val background: Int,
    val surface: Int,
    val neutral: Int,
    val primary: Int,
    val secondary: Int,
    val tertiary: Int,
    val text: Int,
    val textMuted: Int,
    val onPrimary: Int
)

object ThemeManager {

    data class Preset(
        val name: String,
        val primary: Int,
        val secondary: Int,
        val tertiary: Int
    )

    val presets = listOf(
        Preset("中性", 0xFF16A34A.toInt(), 0xFF0EA5E9.toInt(), 0xFFF59E0B.toInt()),
        Preset("蓝紫", 0xFF4F46E5.toInt(), 0xFF06B6D4.toInt(), 0xFFF59E0B.toInt()),
        Preset("森绿", 0xFF15803D.toInt(), 0xFF65A30D.toInt(), 0xFF0D9488.toInt()),
        Preset("暖沙", 0xFFB45309.toInt(), 0xFFD97706.toInt(), 0xFF7C3AED.toInt()),
        Preset("玫红", 0xFFDB2777.toInt(), 0xFF9333EA.toInt(), 0xFFF97316.toInt()),
        Preset("高对比", 0xFF111827.toInt(), 0xFF4B5563.toInt(), 0xFF9CA3AF.toInt())
    )

    fun isDark(context: Context): Boolean {
        return when (ThemePrefs.getMode(context)) {
            ThemePrefs.MODE_DARK -> true
            ThemePrefs.MODE_LIGHT -> false
            else -> (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
    }

    fun colors(context: Context): ThemeColors {
        val dark = isDark(context)
        val preset = presets[ThemePrefs.getPreset(context).coerceIn(0, presets.size - 1)]
        val primary = pick(context, "primary", preset.primary)
        val secondary = pick(context, "secondary", preset.secondary)
        val tertiary = pick(context, "tertiary", preset.tertiary)
        val surfaceBase = if (dark) 0xFF1C1F24.toInt() else 0xFFFFFFFF.toInt()
        val neutralBase = if (dark) 0xFF2A2E35.toInt() else 0xFFE7EAEE.toInt()
        val surface = pick(context, "surface", surfaceBase)
        val neutral = pick(context, "neutral", neutralBase)
        val background = if (dark) 0xFF111318.toInt() else 0xFFF7F8FA.toInt()
        val text = if (dark) 0xFFE5E7EB.toInt() else 0xFF1F2937.toInt()
        val muted = if (dark) 0xFF9CA3AF.toInt() else 0xFF6B7280.toInt()

        val c = ThemeColors(
            background = background,
            surface = surface,
            neutral = neutral,
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            text = text,
            textMuted = muted,
            onPrimary = if (isLight(primary)) 0xFF111318.toInt() else 0xFFFFFFFF.toInt()
        )
        return adjust(c, ThemePrefs.getContrast(context), ThemePrefs.getInvert(context))
    }

    private fun pick(context: Context, key: String, fallback: Int): Int {
        val stored = ThemePrefs.getCustom(context, key)
        return if (stored != 0) stored else fallback
    }

    private fun isLight(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return (r * 299 + g * 587 + b * 114) / 1000 > 160
    }

    private fun adjust(colors: ThemeColors, contrast: Int, invert: Boolean): ThemeColors {
        fun map(color: Int): Int {
            var c = if (invert) {
                Color.rgb(255 - Color.red(color), 255 - Color.green(color), 255 - Color.blue(color))
            } else color
            if (contrast != 0) {
                val f = 1f + contrast * 0.035f
                c = Color.rgb(
                    (Color.red(c) * f).toInt().coerceIn(0, 255),
                    (Color.green(c) * f).toInt().coerceIn(0, 255),
                    (Color.blue(c) * f).toInt().coerceIn(0, 255)
                )
            }
            return c
        }
        return colors.copy(
            background = map(colors.background),
            surface = map(colors.surface),
            neutral = map(colors.neutral),
            primary = map(colors.primary),
            secondary = map(colors.secondary),
            tertiary = map(colors.tertiary),
            text = map(colors.text),
            textMuted = map(colors.textMuted),
            onPrimary = if (isLight(colors.primary) && invert) 0xFF111318.toInt() else colors.onPrimary
        )
    }

    /** 给整棵视图树着色。 */
    fun applyTo(root: View) {
        val c = colors(root.context)
        paint(root, c)
    }

    private fun paint(view: View, c: ThemeColors) {
        when (view) {
            is ViewGroup -> {
                if (view.id == R.id.preview_frame || "card" == view.tag) {
                    // 预览框/卡片保留圆角背景，不覆盖
                } else {
                    view.setBackgroundColor(c.background)
                }
                for (i in 0 until view.childCount) paint(view.getChildAt(i), c)
            }
            is Button -> {
                view.background = rounded(c.primary, 18f)
                view.setTextColor(c.onPrimary)
            }
            is RadioButton -> {
                view.setTextColor(c.text)
            }
            is CheckBox -> view.setTextColor(c.text)
            is TextView -> view.setTextColor(c.text)
            is SeekBar -> {
                view.progressTintList = android.content.res.ColorStateList.valueOf(c.primary)
                view.thumbTintList = android.content.res.ColorStateList.valueOf(c.primary)
            }
            is ProgressBar -> {
                view.indeterminateTintList = android.content.res.ColorStateList.valueOf(c.primary)
            }
            is ImageView -> {
                // 预览图/图标不涂色，避免覆盖画面
            }
            else -> {
                // 自定义色块/按钮保持原样
            }
        }
    }

    fun rounded(color: Int, radiusDp: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radiusDp
        }
}
