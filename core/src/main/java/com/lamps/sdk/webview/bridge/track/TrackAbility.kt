package com.lamps.sdk.webview.bridge.track

import com.lamps.sdk.utils.TrackSdk
import com.lamps.sdk.webview.LampsWebView
import com.lamps.sdk.webview.bridge.EMPTY_JSON_OBJ
import com.lamps.sdk.webview.bridge.LampsAbility
import com.lamps.sdk.webview.bridge.LampsNativeCallback
import com.lamps.sdk.webview.bridge.generateResult
import org.json.JSONObject

/**
 * WebView RIG 上报能力桥接。
 * 对应 heroes_android 中的 RigAbility，用于接收 JS 端发起的 RIG 性能/事件上报。
 */
class TrackAbility : LampsAbility {

    private val trackUtil = TrackAbilityUtil(TrackSdk::sendData)

    companion object {
        const val RIG_UPLOAD = "lamps.common.track"
    }

    override val names: Array<String> = arrayOf(RIG_UPLOAD)

    override fun executeAsync(
        webView: LampsWebView,
        methodName: String,
        params: JSONObject,
        callbackId: String?,
        callback: LampsNativeCallback
    ) {
        when (methodName) {
            RIG_UPLOAD -> handleRigUpload(webView, params, callbackId, callback)
        }
    }

    private fun handleRigUpload(
        webView: LampsWebView,
        params: JSONObject,
        callbackId: String?,
        callback: LampsNativeCallback
    ) {
        trackUtil.observe(webView)
        val action = params.optString("action")
        val type = params.optString("type")
        val data = jsonToHashMap(params)
        if (type == "onload") {
            trackUtil.cacheOnload(action, data)
        } else {
            TrackSdk.sendData(action, data)
        }
        callback.callback(generateResult(EMPTY_JSON_OBJ), callbackId)
    }

    override fun destroy() {
        trackUtil.destroy()
    }

    private fun jsonToHashMap(jsonObject: JSONObject): HashMap<String, Any> {
        val map = HashMap<String, Any>()
        val it: Iterator<*> = jsonObject.keys()
        while (it.hasNext()) {
            val key = it.next().toString()
            map[key] = jsonObject.opt(key) ?: ""
        }
        return map
    }

}
