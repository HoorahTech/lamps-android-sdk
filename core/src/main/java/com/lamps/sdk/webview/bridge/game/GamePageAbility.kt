package com.lamps.sdk.webview.bridge.game

import android.net.Uri
import com.lamps.sdk.core.LampsErrorCode
import com.lamps.sdk.data.behavior.BehaviorTracker
import com.lamps.sdk.webview.GamePageOptions
import com.lamps.sdk.webview.GameWebViewActivity
import com.lamps.sdk.webview.LampsWebView
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
        val options = GamePageOptions(
            gameId = gameIdOf(url),
            trackCustomData = LinkedHashMap(webView.trackCustomData),
        )
        if (!isValidUrl(url)) {
            BehaviorTracker.gameOpenFailed(
                options.gameId,
                LampsErrorCode.PAGE_URL_INVALID,
                "invalid game url",
                options.trackCustomData,
            )
            callback.callback(JSONObject().put("msg", INVALID_URL_MESSAGE), callbackId)
            return
        }

        webView.context.startActivity(
            GameWebViewActivity.buildIntent(
                context = webView.context,
                url = url,
                pageOptions = options,
            )
        )
        callback.callback(JSONObject().put("msg", SUCCESS_MESSAGE), callbackId)
    }

    /** 游戏页是 hash 路由时 `gameId` 在 `#/play?gameId=802` 里，`#` 前后都要找。 */
    private fun gameIdOf(url: String): String = runCatching {
        val uri = Uri.parse(url)
        uri.getQueryParameter(GAME_ID_PARAM)
            ?: uri.fragment?.substringAfter('?', "")
                ?.let { Uri.parse("?$it").getQueryParameter(GAME_ID_PARAM) }
    }.getOrNull().orEmpty()

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
        const val GAME_ID_PARAM = "gameId"
    }
}
