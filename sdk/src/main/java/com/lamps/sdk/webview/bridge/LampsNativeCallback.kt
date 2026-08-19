package com.lamps.sdk.webview.bridge

import org.json.JSONObject

fun interface LampsNativeCallback {
    fun callback(result: JSONObject?, callbackId: String?)
}
