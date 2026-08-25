package com.lamps.sdk.utils

import android.util.Base64
import com.lamps.sdk.BuildConfig
import com.lamps.sdk.config.SdkConfig
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

/** Sends tracking data to the endpoint selected by the SDK API environment. */
object TrackSdk {
    private const val TAG = "TrackSdk"
    private const val REPORT_PATH = "/api/v1/event/report"

    @JvmStatic
    fun sendData(type: String, data: HashMap<String, Any>) {
        val payload = JSONObject().apply {
            buildDefaultValues().forEach { (key, value) ->
                put(key, value)
            }
            put("type", type)
            put("pdata", JSONObject().apply {
                data.forEach { (key, value) ->
                    put(key, value)
                }
            })
        }
        ThreadUtils.runOnWork {
            doReport(payload.toString())
        }
    }

    private fun buildDefaultValues(): Map<String, String> {
        val config = SdkConfig.current
        val context = config?.applicationContext
        return mapOf(
            "ts" to (System.currentTimeMillis() / 1000L).toString(),
            "ua" to DeviceUtils.userAgent(context),
            "ip" to config?.appInitData?.clientIp.orEmpty(),
            "mac" to DeviceUtils.mac(context),
            "imei" to DeviceUtils.imei(context),
            "os" to "Android",
            "androidId" to context?.let(DeviceUtils::androidId).orEmpty(),
            "oaid" to DeviceUtils.oaid(context),
            "appid" to DeviceUtils.appId(context),
            "sdkVersion" to BuildConfig.SDK_VERSION,
            "phoneBrand" to DeviceUtils.phoneBrand(context),
            "network" to DeviceUtils.networkType(context)
        )
    }

    private fun doReport(data: String) {
        val compressed = ByteArrayOutputStream().use { output ->
            GZIPOutputStream(output).use { gzip ->
                gzip.write(data.toByteArray(Charsets.UTF_8))
            }
            output.toByteArray()
        }
        val body = Base64.encodeToString(compressed, Base64.NO_WRAP)
        val url = "${LampsApiHost.baseUrl().trimEnd('/')}$REPORT_PATH"
        SdkLog.d("$TAG report: $url")
        HttpUtils.post(
            url,
            body = body,
            headers = mapOf("Content-Type" to "application/json")
        ).fold(
            onSuccess = { response ->
                SdkLog.w("$TAG report success: ${response.code}")
            },
            onFailure = { error ->
                SdkLog.e("$TAG report failed: ${error.message}", error)
            }
        )
    }
}
