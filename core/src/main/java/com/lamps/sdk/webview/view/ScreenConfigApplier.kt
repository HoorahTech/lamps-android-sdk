package com.lamps.sdk.webview.view

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import org.json.JSONObject
import java.util.WeakHashMap

internal data class ScreenConfig(
    val immersive: Boolean? = null,
    val statusBarColor: Int? = null,
    val statusBarFontDark: Boolean? = null,
    val orientation: Int? = null
)

internal object ScreenConfigApplier {
    private val configs = WeakHashMap<Activity, ScreenConfig>()

    fun applyDefault(activity: Activity) {
        apply(
            activity,
            ScreenConfig(
                immersive = true,
                statusBarColor = Color.TRANSPARENT,
                statusBarFontDark = true
            )
        )
    }

    fun apply(activity: Activity, config: ScreenConfig) {
        configs[activity] = config
        config.orientation?.let { activity.requestedOrientation = it }
        val window = activity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        when (config.immersive) {
            true -> applyImmersive(window)
            false -> applyNonImmersive(window, config)
            null -> applyStatusBarStyle(window, config)
        }
    }

    fun reapply(activity: Activity) {
        val config = configs[activity] ?: return
        apply(activity, config)
    }

    fun current(activity: Activity): ScreenConfig {
        return configs[activity] ?: ScreenConfig()
    }

    private fun applyImmersive(window: Window) {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    private fun applyNonImmersive(window: Window, config: ScreenConfig) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
            window.insetsController?.show(WindowInsets.Type.systemBars())
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
        applyStatusBarStyle(window, config)
    }

    private fun applyStatusBarStyle(window: Window, config: ScreenConfig) {
        val statusBarColor = config.statusBarColor
            ?.takeUnless { it == Color.TRANSPARENT }
        if (statusBarColor != null) {
            window.statusBarColor = statusBarColor
        } else if (config.immersive == false) {
            window.statusBarColor = Color.WHITE
        }
        val fontDark = config.statusBarFontDark ?: inferFontDark(config)
        setStatusBarFontDark(window, fontDark)
    }

    private fun inferFontDark(config: ScreenConfig): Boolean {
        val color = config.statusBarColor?.takeUnless { it == Color.TRANSPARENT }
        if (color != null) {
            val luminance =
                (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
            return luminance >= 128
        }
        return config.immersive == false
    }

    private fun setStatusBarFontDark(window: Window, fontDark: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appearance = if (fontDark) {
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            } else {
                0
            }
            window.insetsController?.setSystemBarsAppearance(
                appearance,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            val decorView = window.decorView
            @Suppress("DEPRECATION")
            val flags = decorView.systemUiVisibility
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility = if (fontDark) {
                flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
    }
}

internal fun ScreenConfig.merge(params: JSONObject): ScreenConfig {
    val immersive = when {
        params.has("immersive") -> flexibleBoolean(params, "immersive")
        params.has("fullScreen") -> flexibleBoolean(params, "fullScreen")
        else -> this.immersive
    }
    val statusBarColor = if (params.has("statusBarColor")) {
        parseColor(params.optString("statusBarColor")) ?: this.statusBarColor
    } else {
        this.statusBarColor
    }
    val statusBarFontDark = parseStatusBarFontDark(params) ?: this.statusBarFontDark
    val orientation = parseOrientation(params) ?: this.orientation
    return ScreenConfig(immersive, statusBarColor, statusBarFontDark, orientation)
}

internal fun parseOrientation(params: JSONObject): Int? {
    if (!params.has("orientation")) return null
    val raw = params.opt("orientation") ?: return null
    val value = raw.toString().trim().lowercase()
    return when (value) {
        "portrait", "0", "vertical" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        "landscape", "1", "horizontal" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        "sensor", "auto" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
        "unspecified", "default" -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        else -> null
    }
}

internal fun isInvalidOrientation(params: JSONObject): Boolean {
    if (!params.has("orientation")) return false
    val raw = params.opt("orientation") ?: return false
    if (raw.toString().trim().isEmpty()) return false
    return parseOrientation(params) == null
}

private fun parseStatusBarFontDark(params: JSONObject): Boolean? {
    if (params.has("statusBarStyle")) {
        return when (params.optString("statusBarStyle").trim().lowercase()) {
            "dark" -> true
            "light" -> false
            else -> null
        }
    }
    if (params.has("statusBarFontDark")) {
        return flexibleBoolean(params, "statusBarFontDark")
    }
    if (params.has("statusBarFontColor")) {
        val color = parseColor(params.optString("statusBarFontColor")) ?: return null
        val luminance = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
        return luminance < 128
    }
    return null
}

private fun flexibleBoolean(params: JSONObject, key: String): Boolean {
    return when (val value = params.opt(key)) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value.equals("true", true) || value == "1"
        else -> params.optBoolean(key)
    }
}

private fun parseColor(value: String): Int? {
    val color = value.trim()
    if (color.isEmpty()) return null
    return runCatching { Color.parseColor(color) }.getOrNull()
}
