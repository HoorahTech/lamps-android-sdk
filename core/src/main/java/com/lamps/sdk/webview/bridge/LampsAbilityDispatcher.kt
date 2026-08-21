package com.lamps.sdk.webview.bridge

import android.os.Handler
import android.os.Looper
import com.lamps.sdk.utils.SdkLog
import com.lamps.sdk.webview.LampsWebView
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

internal class LampsAbilityDispatcher {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val abilities = ConcurrentHashMap<String, LampsAbility>()

    fun addInstaller(installer: LampsAbilityInstaller) {
        installer.createAbilities().forEach { ability ->
            ability.names.forEach { name ->
                abilities.put(name, ability)?.destroy()
            }
        }
    }

    fun invokeAsync(
        webView: LampsWebView,
        methodName: String,
        dataJson: String?,
        callbackId: String?,
        callback: LampsNativeCallback
    ) {
        mainHandler.post {
            val ability = abilities[methodName]
            if (ability == null) {
                callback.callback(unsupportedResult(methodName), callbackId)
                return@post
            }

            runCatching {
                ability.executeAsync(
                    webView,
                    methodName,
                    parseParams(dataJson),
                    callbackId,
                    callback
                )
            }.onFailure { error ->
                SdkLog.e("web ability failed: method=$methodName message=${error.message}", error)
                callback.callback(errorResult(error), callbackId)
            }
        }
    }

    fun invokeSync(
        webView: LampsWebView,
        methodName: String,
        dataJson: String?
    ): String {
        val ability = abilities[methodName]
            ?: return unsupportedResult(methodName).toString()

        return runCatching {
            ability.executeSync(webView, methodName, parseParams(dataJson))
                ?.toString()
                .orEmpty()
        }.getOrElse { error ->
            SdkLog.e("web ability failed: method=$methodName message=${error.message}", error)
            errorResult(error).toString()
        }
    }

    fun destroy() {
        abilities.values.toSet().forEach(LampsAbility::destroy)
        abilities.clear()
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun parseParams(dataJson: String?): JSONObject {
        if (dataJson.isNullOrBlank()) return JSONObject()
        val request = JSONObject(dataJson)
        return request.optJSONObject("data") ?: request
    }

    private fun unsupportedResult(methodName: String): JSONObject {
        SdkLog.e("unsupported web ability: method=$methodName")
        return JSONObject().put("canUse", "notSupport")
    }

    private fun errorResult(error: Throwable): JSONObject {
        return JSONObject()
            .put("code", ERROR_EXECUTION)
            .put("message", error.message.orEmpty())
            .put("data", JSONObject())
    }

    private companion object {
        const val ERROR_EXECUTION = 801
    }
}
