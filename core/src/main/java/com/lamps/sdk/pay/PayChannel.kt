package com.lamps.sdk.pay

import android.app.Activity
import org.json.JSONObject

/**
 * Optional pay module contract. [pay] is invoked on the bridge background thread.
 * The returned object is sent to H5 as the invoke callback body.
 */
interface PayChannel {
    val channel: String

    fun pay(activity: Activity, params: JSONObject): JSONObject

    companion object {
        const val ALIPAY = "alipay"
    }
}
