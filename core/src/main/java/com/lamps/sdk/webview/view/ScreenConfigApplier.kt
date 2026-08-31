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
    val immersive: Boolean = DEFAULT_IMMERSIVE,
    val showStatusBar: Boolean = DEFAULT_SHOW_STATUS_BAR,
    val statusBarColor: Int = DEFAULT_STATUS_BAR_COLOR,
    val statusBarFontDark: Boolean = DEFAULT_STATUS_BAR_FONT_DARK,
    val orientation: Int? = DEFAULT_ORIENTATION
)

internal object ScreenConfigApplier {
    private val configs = WeakHashMap<Activity, ScreenConfig>()

    fun applyDefault(activity: Activity) {
        apply(activity, ScreenConfig())
    }

    fun apply(activity: Activity, config: ScreenConfig) {
        configs[activity] = config
        config.orientation?.let { activity.requestedOrientation = it }
        val window = activity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        when {
            config.immersive && config.showStatusBar -> applyImmersiveWithStatusBar(window, config)
            config.immersive -> applyImmersive(window)
            else -> applyNonImmersive(window, config)
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

    private fun applyImmersiveWithStatusBar(window: Window, config: ScreenConfig) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
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
                controller.show(WindowInsets.Type.statusBars())
                controller.hide(WindowInsets.Type.navigationBars())
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
        applyStatusBarStyle(window, config)
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
        window.statusBarColor = config.statusBarColor
        setStatusBarFontDark(window, config.statusBarFontDark)
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
    val immersive = parseBoolean(params, KEY_IMMERSIVE) ?: immersive
    val passedStatusBarStyle = hasValue(params, KEY_STATUS_BAR_COLOR) ||
        hasValue(params, KEY_STATUS_BAR_FONT_DARK)
    val showStatusBar = when {
        !immersive -> true
        passedStatusBarStyle -> true
        hasValue(params, KEY_IMMERSIVE) -> false
        else -> showStatusBar
    }
    return ScreenConfig(
        immersive = immersive,
        showStatusBar = showStatusBar,
        statusBarColor = parseColor(params, KEY_STATUS_BAR_COLOR) ?: statusBarColor,
        statusBarFontDark = parseBoolean(params, KEY_STATUS_BAR_FONT_DARK) ?: statusBarFontDark,
        orientation = parseOrientation(params) ?: orientation
    )
}

internal fun invalidScreenConfigMessage(params: JSONObject): String? {
    if (isInvalidBoolean(params, KEY_IMMERSIVE)) return "immersive is invalid"
    if (isInvalidBoolean(params, KEY_STATUS_BAR_FONT_DARK)) return "statusBarFontDark is invalid"
    if (isInvalidColor(params, KEY_STATUS_BAR_COLOR)) return "statusBarColor is invalid"
    if (isInvalidOrientation(params)) return "orientation is invalid"
    return null
}

internal const val DEFAULT_IMMERSIVE = true
internal const val DEFAULT_SHOW_STATUS_BAR = false
internal val DEFAULT_STATUS_BAR_COLOR = Color.TRANSPARENT
internal const val DEFAULT_STATUS_BAR_FONT_DARK = true
internal val DEFAULT_ORIENTATION: Int? = null

private const val KEY_IMMERSIVE = "immersive"
private const val KEY_STATUS_BAR_COLOR = "statusBarColor"
private const val KEY_STATUS_BAR_FONT_DARK = "statusBarFontDark"
private const val KEY_ORIENTATION = "orientation"

private const val ORIENTATION_PORTRAIT = "portrait"
private const val ORIENTATION_LANDSCAPE = "landscape"
private const val ORIENTATION_AUTO = "auto"
private const val ORIENTATION_DEFAULT = "default"

private fun parseBoolean(params: JSONObject, key: String): Boolean? {
    if (!hasValue(params, key)) return null
    return params.opt(key) as? Boolean
}

private fun parseColor(params: JSONObject, key: String): Int? {
    if (!hasValue(params, key)) return null
    val raw = params.opt(key) as? String ?: return null
    val color = raw.trim()
    if (color.isEmpty()) return null
    return runCatching { Color.parseColor(color) }.getOrNull()
}

private fun parseOrientation(params: JSONObject): Int? {
    val value = orientationValue(params) ?: return null
    return when (value) {
        ORIENTATION_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        ORIENTATION_AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
        ORIENTATION_DEFAULT -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        else -> null
    }
}

private fun isInvalidBoolean(params: JSONObject, key: String): Boolean {
    if (!hasValue(params, key)) return false
    return params.opt(key) !is Boolean
}

private fun isInvalidColor(params: JSONObject, key: String): Boolean {
    if (!hasValue(params, key)) return false
    val raw = params.opt(key)
    if (raw !is String) return true
    val color = raw.trim()
    if (color.isEmpty()) return false
    return runCatching { Color.parseColor(color) }.getOrNull() == null
}

private fun isInvalidOrientation(params: JSONObject): Boolean {
    val value = orientationValue(params) ?: return false
    return value != ORIENTATION_PORTRAIT &&
        value != ORIENTATION_LANDSCAPE &&
        value != ORIENTATION_AUTO &&
        value != ORIENTATION_DEFAULT
}

private fun orientationValue(params: JSONObject): String? {
    if (!hasValue(params, KEY_ORIENTATION)) return null
    val raw = params.opt(KEY_ORIENTATION)
    if (raw !is String) return ""
    return raw.trim().takeIf { it.isNotEmpty() }
}

private fun hasValue(params: JSONObject, key: String): Boolean {
    return params.has(key) && !params.isNull(key)
}
