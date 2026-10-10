package com.lamps.sdk.utils

import android.os.Build
import android.util.Base64
import com.lamps.sdk.BuildConfig
import com.lamps.sdk.config.SdkConfig
import com.lamps.sdk.core.CoreTrackCallback
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.CopyOnWriteArrayList
import java.util.zip.GZIPOutputStream

/** Sends tracking data to the endpoint selected by the SDK API environment. */
object TrackSdk {
    private const val TAG = "TrackSdk"
    private const val REPORT_PATH = "/api/v1/event/report"
    private val listeners = CopyOnWriteArrayList<Listener>()

    fun addListener(owner: Any, callback: CoreTrackCallback) {
        if (listeners.any { it.owner === owner }) return
        listeners.add(Listener(owner, callback))
    }

    fun removeListener(owner: Any) {
        listeners.removeAll { it.owner === owner }
    }

    @JvmStatic
    fun sendData(action: String, data: HashMap<String, Any>) {
        val payload = JSONObject().apply {
            buildDefaultValues().forEach { (key, value) ->
                put(key, value)
            }
            put("action", action)
            put("pdata", JSONObject().apply {
                data.forEach { (key, value) ->
                    put(key, value)
                }
            })
        }
        val body = payload.toString()
        SdkLog.d("$TAG params: $body")
        dispatch(action, body)
        ThreadUtils.runOnWork {
            doReport(body)
        }
    }

    private fun dispatch(action: String, payload: String) {
        listeners.forEach { listener ->
            runCatching { listener.callback.onTrack(action, payload) }
                .onFailure { error ->
                    SdkLog.e("$TAG track callback failed: ${error.message}", error)
                }
        }
    }

    private class Listener(
        val owner: Any,
        val callback: CoreTrackCallback
    )

    private fun buildDefaultValues(): Map<String, String> {
        val config = SdkConfig.current
        val context = config?.applicationContext
        return mapOf(
            "ts" to (System.currentTimeMillis() / 1000L).toString(),
            "ua" to DeviceUtils.userAgent(context),
            "ip" to config?.appInitData?.clientIp.orEmpty(),
            "mac" to DeviceUtils.mac(context),
            "os" to "Android",
            "appid" to DeviceUtils.appId(context),
            "sdkVersion" to BuildConfig.SDK_VERSION,
            "phoneBrand" to DeviceUtils.phoneBrand(context),
            "network" to DeviceUtils.networkType(context),
            "appVer" to context?.let(DeviceUtils::appVersion).orEmpty(),
            "osVer" to Build.VERSION.RELEASE.orEmpty(),
            "env" to LampsApiHost.envName(context),
            "imei" to DeviceUtils.imei(context),
            "androidId" to context?.let(DeviceUtils::androidId).orEmpty(),
            "oaid" to DeviceUtils.oaid(context),
            "packageName" to context?.packageName.orEmpty(),
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
