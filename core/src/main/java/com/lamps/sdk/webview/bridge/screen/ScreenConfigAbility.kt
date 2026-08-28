package com.lamps.sdk.webview.bridge.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.lamps.sdk.webview.LampsWebView
import com.lamps.sdk.webview.view.ScreenConfigApplier
import com.lamps.sdk.webview.bridge.EMPTY_JSON_OBJ
import com.lamps.sdk.webview.bridge.LampsAbility
import com.lamps.sdk.webview.bridge.LampsNativeCallback
import com.lamps.sdk.webview.bridge.generateResult
import com.lamps.sdk.webview.view.isInvalidOrientation
import com.lamps.sdk.webview.view.merge
import org.json.JSONObject

internal class ScreenConfigAbility : LampsAbility {
    override val names: Array<String> = arrayOf(METHOD_SCREEN_CONFIG)

    override fun executeAsync(
        webView: LampsWebView,
        methodName: String,
        params: JSONObject,
        callbackId: String?,
        callback: LampsNativeCallback
    ) {
        val activity = webView.context.findActivity()
        if (activity == null) {
            callback.callback(
                generateResult(EMPTY_JSON_OBJ, ERROR_NO_ACTIVITY, "activity not found"),
                callbackId
            )
            return
        }
        if (isInvalidOrientation(params)) {
            callback.callback(
                generateResult(EMPTY_JSON_OBJ, ERROR_INVALID_PARAM, "orientation is invalid"),
                callbackId
            )
            return
        }
        val config = ScreenConfigApplier.current(activity).merge(params)
        ScreenConfigApplier.apply(activity, config)
        callback.callback(generateResult(EMPTY_JSON_OBJ), callbackId)
    }

    private companion object {
        const val METHOD_SCREEN_CONFIG = "lamps.common.screenConfig"
        const val ERROR_NO_ACTIVITY = 801
        const val ERROR_INVALID_PARAM = 801
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
