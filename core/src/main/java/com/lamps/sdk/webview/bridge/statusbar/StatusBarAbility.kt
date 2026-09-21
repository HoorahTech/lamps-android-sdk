package com.lamps.sdk.webview.bridge.statusbar

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.lamps.sdk.webview.LampsWebView
import com.lamps.sdk.webview.bridge.EMPTY_JSON_OBJ
import com.lamps.sdk.webview.bridge.LampsAbility
import com.lamps.sdk.webview.bridge.LampsNativeCallback
import com.lamps.sdk.webview.bridge.generateResult
import com.lamps.sdk.webview.view.StatusBarApplier
import com.lamps.sdk.webview.view.invalidStatusBarMessage
import org.json.JSONObject

internal class StatusBarAbility : LampsAbility {
    override val names: Array<String> = arrayOf(METHOD_STATUS_BAR)

    override fun executeAsync(
        webView: LampsWebView,
        methodName: String,
        params: JSONObject,
        callbackId: String?,
        callback: LampsNativeCallback
    ) {
        // 内嵌模式下这个 Activity 是宿主的，改窗口会连带隐藏宿主的系统导航栏，直接拒绝。
        if (webView.displayMode == LampsWebView.DISPLAY_MODE_EMBED) {
            callback.callback(
                generateResult(EMPTY_JSON_OBJ, ERROR_EMBED_MODE, "embed mode not supported"),
                callbackId
            )
            return
        }
        val activity = webView.context.findActivity()
        if (activity == null) {
            callback.callback(
                generateResult(EMPTY_JSON_OBJ, ERROR_NO_ACTIVITY, "activity not found"),
                callbackId
            )
            return
        }
        val invalidMessage = invalidStatusBarMessage(params)
        if (invalidMessage != null) {
            callback.callback(
                generateResult(EMPTY_JSON_OBJ, ERROR_INVALID_PARAM, invalidMessage),
                callbackId
            )
            return
        }
        StatusBarApplier.apply(activity, webView, params)
        callback.callback(generateResult(EMPTY_JSON_OBJ), callbackId)
    }

    private companion object {
        const val METHOD_STATUS_BAR = "lamps.common.statusBar"
        const val ERROR_NO_ACTIVITY = 801
        const val ERROR_INVALID_PARAM = 801
        const val ERROR_EMBED_MODE = 801
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        val base = current.baseContext
        if (base === current) break
        current = base
    }
    return current as? Activity
}
