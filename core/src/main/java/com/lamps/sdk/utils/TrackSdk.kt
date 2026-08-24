package com.lamps.sdk.utils

import java.net.URLEncoder

/**
 * 最简单的 RigSdk 实现：写死链接，直接上报。
 */
object TrackSdk {
    private const val TAG = "TrackSdk"
    private const val DEFAULT_RIG_URL = "https://rig.hupu.com/report"

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
        val url = "$DEFAULT_RIG_URL?$query"
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
