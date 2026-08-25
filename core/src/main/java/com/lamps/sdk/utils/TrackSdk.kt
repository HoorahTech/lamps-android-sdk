package com.lamps.sdk.utils

import java.net.URLEncoder

/** Sends tracking data to the endpoint selected by the SDK API environment. */
object TrackSdk {
    private const val TAG = "TrackSdk"
    private const val REPORT_PATH = "/api/v1/event/report"

    @JvmStatic
    fun sendData(type: String, data: HashMap<String, Any>) {
        val params = HashMap<String, String>().apply {
            put("type", type)
            data.forEach { (key, value) ->
                put(key, value.toString())
            }
        }
        ThreadUtils.runOnWork {
            doReport(params)
        }
    }

    private fun doReport(params: HashMap<String, String>) {
        val query = params.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }
        val url = "${LampsApiHost.baseUrl().trimEnd('/')}$REPORT_PATH?$query"
        SdkLog.d("$TAG report: $url")
        HttpUtils.get(url).fold(
            onSuccess = { response ->
                SdkLog.w("$TAG report success: ${response.code}")
            },
            onFailure = { error ->
                SdkLog.e("$TAG report failed: ${error.message}", error)
            }
        )
    }
}
