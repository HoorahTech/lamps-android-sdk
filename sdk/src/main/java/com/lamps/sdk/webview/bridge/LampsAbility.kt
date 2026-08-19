package com.lamps.sdk.webview.bridge

import com.lamps.sdk.webview.LampsWebView
import org.json.JSONObject

interface LampsAbility {
    val names: Array<String>

    fun executeAsync(
        webView: LampsWebView,
        methodName: String,
        params: JSONObject,
        callbackId: String?,
        callback: LampsNativeCallback
    )

    fun executeSync(
        webView: LampsWebView,
        methodName: String,
        params: JSONObject
    ): JSONObject? = null

    fun destroy() = Unit
}
