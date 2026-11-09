package com.lamps.sdk.webview.bridge.pay

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.lamps.sdk.utils.SdkLog
import com.lamps.sdk.webview.LampsWebView
import com.lamps.sdk.webview.bridge.LampsAbility
import com.lamps.sdk.webview.bridge.LampsNativeCallback
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * Third-party pay bridge. Only the Alipay channel is supported, and only when the host app
 * already ships the Alipay SDK.
 */
internal class ThirdPartyPayAbility : LampsAbility {
    override val names: Array<String> = arrayOf(METHOD_ORDER)

    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    private var destroyed = false

    override fun destroy() {
        destroyed = true
        executor.shutdownNow()
    }

    override fun executeAsync(
        webView: LampsWebView,
        methodName: String,
        params: JSONObject,
        callbackId: String?,
        callback: LampsNativeCallback
    ) {
        if (destroyed) return
        if (methodName != METHOD_ORDER) {
            callback.callback(messageResult("unsupported method: $methodName"), callbackId)
            return
        }

        val channel = params.optString("channel").trim()
        if (channel.isEmpty()) {
            callback.callback(messageResult("缺少 channel"), callbackId)
            return
        }
        if (!channel.equals(CHANNEL_ALIPAY, ignoreCase = true)) {
            callback.callback(messageResult("暂不支持该支付渠道: $channel"), callbackId)
            return
        }

        val orderInfo = params.optString("orderInfo").trim()
        if (orderInfo.isEmpty()) {
            callback.callback(messageResult("orderInfo 为空"), callbackId)
            return
        }

        val activity = webView.context.findActivity()
        if (activity == null || activity.isFinishing || activity.isDestroyed ||
            !AlipayPayClient.isAvailable(activity.classLoader)
        ) {
            callback.callback(messageResult(MSG_SDK_NOT_FOUND), callbackId)
            return
        }

        val guardedCallback = LampsNativeCallback { result, id ->
            if (!destroyed) callback.callback(result, id)
        }
        try {
            executor.execute {
                if (destroyed) return@execute
                try {
                    val payResult = AlipayPayClient.pay(activity, orderInfo)
                    guardedCallback.callback(payResultBody(payResult), callbackId)
                } catch (error: Exception) {
                    SdkLog.w("alipay pay unavailable: ${error.javaClass.simpleName}")
                    guardedCallback.callback(messageResult(MSG_SDK_NOT_FOUND), callbackId)
                } catch (error: NoClassDefFoundError) {
                    SdkLog.w("alipay pay unavailable: ${error.javaClass.simpleName}")
                    guardedCallback.callback(messageResult(MSG_SDK_NOT_FOUND), callbackId)
                }
            }
        } catch (error: RejectedExecutionException) {
            if (!destroyed) {
                callback.callback(messageResult(MSG_SDK_NOT_FOUND), callbackId)
            }
            SdkLog.w("alipay pay rejected: ${error.javaClass.simpleName}")
        }
    }

    private fun payResultBody(payResult: AlipayPayRawResult): JSONObject {
        val msg = when (payResult.resultStatus) {
            "9000" -> "success"
            "6001" -> "cancel"
            else -> "fail"
        }
        return JSONObject()
            .put("msg", msg)
            .put("channel", CHANNEL_ALIPAY)
            .put("resultStatus", payResult.resultStatus)
            .put("result", payResult.result)
            .put("memo", payResult.memo)
    }

    private fun messageResult(msg: String): JSONObject {
        return JSONObject().put("msg", msg)
    }

    internal companion object {
        const val METHOD_ORDER = "lamps.thirdPartyPay.order"
        private const val CHANNEL_ALIPAY = "alipay"
        private const val MSG_SDK_NOT_FOUND = "宿主未集成支付宝 SDK"
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
