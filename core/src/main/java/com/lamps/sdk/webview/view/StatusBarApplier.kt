package com.lamps.sdk.webview.view

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import com.lamps.sdk.utils.DeviceUtils
import com.lamps.sdk.webview.LampsWebView
import org.json.JSONObject

internal data class StatusBarConfig(
    val showStatusBar: Boolean = true,
    val statusBarImmersive: Boolean = true,
    val backgroundColor: Int = Color.TRANSPARENT,
    val statusBarFontStyle: Int = 1
) {
    companion object {
        fun from(params: JSONObject) = StatusBarConfig(
            showStatusBar = bool(params, "showStatusBar") ?: true,
            statusBarImmersive = bool(params, "statusBarImmersive") ?: true,
            backgroundColor = color(params, "backgroundColor") ?: Color.TRANSPARENT,
            statusBarFontStyle = fontStyle(params) ?: 1
        )
    }
}

internal object StatusBarApplier {
    fun applyDefault(activity: Activity, webView: LampsWebView) {
        apply(activity, webView, StatusBarConfig())
    }

    fun apply(activity: Activity, webView: LampsWebView, params: JSONObject) {
        apply(activity, webView, StatusBarConfig.from(params))
    }

    fun apply(activity: Activity, webView: LampsWebView, config: StatusBarConfig) {
        applyWindow(activity.window, config)
        (webView.parent as? View)?.setBackgroundColor(config.backgroundColor)
        val lp = webView.layoutParams as? ViewGroup.MarginLayoutParams
            ?: FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT
        lp.height = ViewGroup.LayoutParams.MATCH_PARENT
        lp.topMargin = if (config.statusBarImmersive) 0 else DeviceUtils.statusBarHeight(activity)
        webView.layoutParams = lp
    }

    private fun applyWindow(window: Window, config: StatusBarConfig) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                if (config.showStatusBar) show(WindowInsets.Type.statusBars())
                else hide(WindowInsets.Type.statusBars())
                hide(WindowInsets.Type.navigationBars())
                setSystemBarsAppearance(
                    if (config.statusBarFontStyle == 1) {
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    } else {
                        0
                    },
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            }
        } else {
            @Suppress("DEPRECATION")
            var flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            if (!config.showStatusBar) flags = flags or View.SYSTEM_UI_FLAG_FULLSCREEN
            if (config.statusBarFontStyle == 1) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = flags
        }
    }
}

internal fun invalidStatusBarMessage(params: JSONObject): String? {
    if (invalidBool(params, "showStatusBar")) return "showStatusBar is invalid"
    if (invalidBool(params, "statusBarImmersive")) return "statusBarImmersive is invalid"
    if (invalidColor(params, "backgroundColor")) return "backgroundColor is invalid"
    if (invalidFontStyle(params)) return "statusBarFontStyle is invalid"
    return null
}

private fun JSONObject.hasValue(key: String) = has(key) && !isNull(key)

private fun bool(params: JSONObject, key: String): Boolean? {
    if (!params.hasValue(key)) return null
    return params.opt(key) as? Boolean
}

private fun color(params: JSONObject, key: String): Int? {
    if (!params.hasValue(key)) return null
    val raw = (params.opt(key) as? String)?.trim().orEmpty()
    if (raw.isEmpty()) return null
    return runCatching { Color.parseColor(raw) }.getOrNull()
}

private fun fontStyle(params: JSONObject): Int? {
    if (!params.hasValue("statusBarFontStyle")) return null
    val value = (params.opt("statusBarFontStyle") as? Number)?.toInt() ?: return null
    return value.takeIf { it == 0 || it == 1 }
}

private fun invalidBool(params: JSONObject, key: String): Boolean {
    return params.hasValue(key) && params.opt(key) !is Boolean
}

private fun invalidColor(params: JSONObject, key: String): Boolean {
    if (!params.hasValue(key)) return false
    val raw = params.opt(key) as? String ?: return true
    if (raw.trim().isEmpty()) return false
    return runCatching { Color.parseColor(raw.trim()) }.getOrNull() == null
}

private fun invalidFontStyle(params: JSONObject): Boolean {
    if (!params.hasValue("statusBarFontStyle")) return false
    return fontStyle(params) == null
}
