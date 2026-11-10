package com.lamps.sdk.webview.bridge.pay

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.lamps.sdk.pay.PayChannel
import com.lamps.sdk.pay.PayChannelRegistry
import com.lamps.sdk.utils.SdkLog
import com.lamps.sdk.webview.LampsWebView
import com.lamps.sdk.webview.bridge.LampsAbility
import com.lamps.sdk.webview.bridge.LampsNativeCallback
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * Third-party pay bridge. Channels are provided by optional modules such as alipay.
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
        fun reply(result: JSONObject) {
            if (!destroyed) callback.callback(result, callbackId)
        }

        if (methodName != METHOD_ORDER) {
            reply(messageResult("unsupported method: $methodName"))
            return
        }

        val channelName = params.optString("channel").trim()
        if (channelName.isEmpty()) {
            reply(messageResult("缺少 channel"))
            return
        }
        val channel = PayChannelRegistry.find(channelName)
        if (channel == null) {
            reply(messageResult("暂不支持该支付渠道: $channelName"))
            return
        }

        val activity = webView.context.findActivity()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            reply(messageResult("当前页面不可用"))
            return
        }

        try {
            executor.execute {
                reply(pay(activity, channel, params))
            }
        } catch (error: RejectedExecutionException) {
            SdkLog.w("third party pay rejected: ${error.javaClass.simpleName}")
            reply(messageResult("支付失败"))
        }
    }

    private fun pay(activity: Activity, channel: PayChannel, params: JSONObject): JSONObject {
        if (destroyed || activity.isFinishing || activity.isDestroyed) {
            return messageResult("当前页面不可用")
        }
        return try {
            channel.pay(activity, params)
        } catch (error: Exception) {
            SdkLog.w("third party pay failed: ${error.javaClass.simpleName}")
            messageResult("支付失败")
        }
    }

    private fun messageResult(msg: String): JSONObject {
        return JSONObject().put("msg", msg)
    }

    internal companion object {
        const val METHOD_ORDER = "lamps.thirdPartyPay.order"
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
