package com.lamps.sdk.alipay

import android.app.Activity
import com.alipay.sdk.app.PayTask
import com.lamps.sdk.pay.PayChannel
import com.lamps.sdk.utils.SdkLog
import org.json.JSONObject

internal object AlipayPayChannel : PayChannel {
    override val channel: String = PayChannel.ALIPAY

    override fun pay(activity: Activity, params: JSONObject): JSONObject {
        val orderInfo = params.optString("orderInfo").trim()
        if (orderInfo.isEmpty()) {
            return JSONObject().put("msg", "orderInfo 为空")
        }
        return try {
            val raw = PayTask(activity).payV2(orderInfo, true)
            val resultStatus = raw?.get("resultStatus")?.trim().orEmpty()
            val msg = when (resultStatus) {
                "9000" -> "success"
                "6001" -> "cancel"
                else -> "fail"
            }
            JSONObject()
                .put("msg", msg)
                .put("channel", channel)
                .put("resultStatus", resultStatus)
                .put("result", raw?.get("result").orEmpty())
                .put("memo", raw?.get("memo").orEmpty())
        } catch (error: Throwable) {
            SdkLog.w("alipay pay failed: ${error.javaClass.simpleName}")
            JSONObject().put("msg", "支付宝支付失败")
        }
    }
}
