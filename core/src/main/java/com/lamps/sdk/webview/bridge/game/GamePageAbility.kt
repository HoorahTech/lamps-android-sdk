package com.lamps.sdk.webview.bridge.game

import android.net.Uri
import com.lamps.sdk.webview.LampsWebView
import com.lamps.sdk.webview.GameWebViewActivity
import com.lamps.sdk.webview.bridge.LampsAbility
import com.lamps.sdk.webview.bridge.LampsNativeCallback
import org.json.JSONObject

/** Opens a game page in the SDK's full-screen WebView container. */
class GamePageAbility : LampsAbility {
    override val names: Array<String> = arrayOf(METHOD_OPEN)

    override fun executeAsync(
        webView: LampsWebView,
        methodName: String,
        params: JSONObject,
        callbackId: String?,
        callback: LampsNativeCallback
    ) {
        val url = params.optString("url").trim()
        if (!isValidUrl(url)) {
            callback.callback(JSONObject().put("msg", INVALID_URL_MESSAGE), callbackId)
            return
        }

        webView.context.startActivity(
            GameWebViewActivity.buildIntent(
                context = webView.context,
                url = url
            )
        )
        callback.callback(JSONObject().put("msg", SUCCESS_MESSAGE), callbackId)
    }

    private fun isValidUrl(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        return (uri.scheme.equals("http", ignoreCase = true) ||
            uri.scheme.equals("https", ignoreCase = true)) &&
            !uri.host.isNullOrBlank()
    }

    private companion object {
        const val METHOD_OPEN = "lamps.game.open"
        const val SUCCESS_MESSAGE = "success"
        const val INVALID_URL_MESSAGE = "url 无效，须为完整 http(s) URL"
    }
}
