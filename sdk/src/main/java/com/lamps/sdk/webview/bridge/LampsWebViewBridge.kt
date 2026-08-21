package com.lamps.sdk.webview.bridge

import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import com.lamps.sdk.utils.SdkLog
import com.lamps.sdk.webview.LampsWebView
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class LampsWebViewBridge internal constructor(
    private val webView: LampsWebView
) {
    private val dispatcher = LampsAbilityDispatcher()
    private val messageIds = ConcurrentHashMap<String, Boolean>()

    @JavascriptInterface
    fun callNativeAsync(methodName: String, dataJson: String?, callBackSig: String?) {
        SdkLog.d(
            "web bridge H5->Native async method=$methodName callbackSig=$callBackSig data=$dataJson"
        )
        dispatcher.invokeAsync(
            webView,
            methodName,
            dataJson,
            callBackSig,
            LampsNativeCallback(::callNativeBack)
        )
    }

    @JavascriptInterface
    fun callNativeSync(methodName: String, dataJson: String?): String {
        SdkLog.d("web bridge H5->Native sync method=$methodName data=$dataJson")
        val result = dispatcher.invokeSync(webView, methodName, dataJson)
        SdkLog.d("web bridge Native->H5 sync method=$methodName result=$result")
        return result
    }

    /**
     * New protocol:
     * androidBridge.postMessage({type, id, method, data})
     */
    @JavascriptInterface
    fun postMessage(messageString: String?) {
        if (messageString.isNullOrBlank()) {
            SdkLog.w("web bridge postMessage is empty")
            return
        }

        runCatching {
            val message = JSONObject(messageString)
            val type = message.optString("type")
            if (type != TYPE_REQUEST) {
                SdkLog.w("unsupported web bridge message type: $type")
                return
            }

            val messageId = message.optString("id")
            val methodName = message.optString("method")
            val dataJson = JSONObject()
                .put("data", message.optJSONObject("data") ?: JSONObject())
                .toString()

            if (messageId.isNotEmpty()) {
                messageIds[messageId] = true
            }
            SdkLog.d(
                "web bridge H5->Native postMessage type=$type id=$messageId method=$methodName data=$dataJson"
            )
            dispatcher.invokeAsync(
                webView,
                methodName,
                dataJson,
                messageId,
                LampsNativeCallback(::callNativeBack)
            )
        }.onFailure { error ->
            SdkLog.e("web bridge postMessage parse failed: ${error.message}", error)
        }
    }

    fun registerAbilityInstaller(installer: LampsAbilityInstaller) {
        dispatcher.addInstaller(installer)
    }

    @JvmOverloads
    fun send(
        methodName: String,
        params: Any? = null,
        callback: ValueCallback<String>? = null
    ) {
        val paramsJson = when (params) {
            null -> "null"
            is JSONObject -> params.toString()
            else -> JSONObject.wrap(params)?.toString() ?: "null"
        }
        SdkLog.d("web bridge Native->H5 event method=$methodName params=$paramsJson")
        callJs(methodName, paramsJson, callback)
    }

    internal fun destroy() {
        dispatcher.destroy()
        messageIds.clear()
    }

    private fun callNativeBack(result: JSONObject?, callBackSig: String?) {
        if (callBackSig.isNullOrEmpty()) return

        if (messageIds.remove(callBackSig) != null) {
            val response = JSONObject()
                .put("type", TYPE_RESPONSE)
                .put("id", callBackSig)
                .put("method", "")
                .put("data", result ?: JSONObject())
            SdkLog.d("web bridge Native->H5 postMessage response=$response")
            sendNativeMessage(response)
        } else {
            SdkLog.d("web bridge Native->H5 async callback sig=$callBackSig result=$result")
            callJs(callBackSig, result?.toString().orEmpty())
        }
    }

    private fun callJs(
        methodName: String,
        paramsJson: String,
        callback: ValueCallback<String>? = null
    ) {
        val quotedMethod = JSONObject.quote(methodName)
        val quotedParams = JSONObject.quote(paramsJson)
        val script = """
            (function() {
                var raw = $quotedParams;
                if (window.HoorahBridge && typeof window.HoorahBridge._handle_ === 'function') {
                    var payload = raw;
                    try { payload = JSON.parse(raw); } catch (ignored) {}
                    window.HoorahBridge._handle_($quotedMethod, payload);
                } else if (window.HupuBridge && typeof window.HupuBridge._handle_ === 'function') {
                    window.HupuBridge._handle_($quotedMethod, raw);
                }
            })();
        """.trimIndent()
        executeJavascript(script, callback)
    }

    private fun sendNativeMessage(message: JSONObject) {
        val script = """
            (function() {
                if (typeof window.receiveNativeMessage === 'function') {
                    window.receiveNativeMessage(${JSONObject.quote(message.toString())});
                }
            })();
        """.trimIndent()
        executeJavascript(script)
    }

    private fun executeJavascript(
        script: String,
        callback: ValueCallback<String>? = null
    ) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            webView.evaluateJavascript(script, callback)
        } else {
            webView.post {
                webView.evaluateJavascript(script, callback)
            }
        }
    }

    private companion object {
        const val TYPE_REQUEST = "request"
        const val TYPE_RESPONSE = "response"
    }
}
