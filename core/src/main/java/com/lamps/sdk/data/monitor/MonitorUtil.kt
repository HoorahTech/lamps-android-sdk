package com.lamps.sdk.data.monitor

import android.net.Uri
import com.lamps.sdk.BuildConfig
import com.lamps.sdk.config.SdkConfig
import com.lamps.sdk.data.monitor.MonitorConstant.ANDROID_ID
import com.lamps.sdk.data.monitor.MonitorConstant.APP_ID
import com.lamps.sdk.data.monitor.MonitorConstant.IMEI
import com.lamps.sdk.data.monitor.MonitorConstant.IP
import com.lamps.sdk.data.monitor.MonitorConstant.MAC
import com.lamps.sdk.data.monitor.MonitorConstant.NETWORK
import com.lamps.sdk.data.monitor.MonitorConstant.OAID
import com.lamps.sdk.data.monitor.MonitorConstant.OS
import com.lamps.sdk.data.monitor.MonitorConstant.PACKAGE_NAME
import com.lamps.sdk.data.monitor.MonitorConstant.PHONE_BRAND
import com.lamps.sdk.data.monitor.MonitorConstant.REM_SIGN
import com.lamps.sdk.data.monitor.MonitorConstant.SDK_VERSION
import com.lamps.sdk.data.monitor.MonitorConstant.SH
import com.lamps.sdk.data.monitor.MonitorConstant.SW
import com.lamps.sdk.data.monitor.MonitorConstant.TS
import com.lamps.sdk.data.monitor.MonitorConstant.UA
import com.lamps.sdk.utils.DeviceUtils
import com.lamps.sdk.utils.HttpUtils
import com.lamps.sdk.utils.SdkLog
import com.lamps.sdk.utils.ThreadUtils
import java.net.URLEncoder
import java.security.MessageDigest

object MonitorUtil {
    private val SIGN_KEYS = listOf(
        "appid",
        "forwardSource",
        "price",
        "requestId",
        "sdkVersion",
        "slotId",
    )

    fun report(
        event: String,
        urls: List<String>?,
        values: Map<String, String>,
        needSign: Boolean = false
    ) {
        if (urls.isNullOrEmpty()) return
        ThreadUtils.runOnWork {
            urls.forEach { template ->
                val url = replaceMacros(template, values, needSign)
                val recordId = MonitorReportRecorder.begin(event, url)
                HttpUtils.get(url, headers = mapOf("Accept" to "*/*")).fold(
                    onSuccess = { response ->
                        val error = if (response.isSuccessful) {
                            null
                        } else {
                            "http ${response.code}"
                        }
                        if (error != null) {
                            SdkLog.w("monitor $event failed: $error")
                        }
                        MonitorReportRecorder.complete(recordId, response.code, error)
                    },
                    onFailure = { error ->
                        SdkLog.w("monitor $event failed: ${error.message}", error)
                        MonitorReportRecorder.complete(recordId, null, error.message)
                    }
                )
            }
        }
    }

    private fun replaceMacros(
        url: String,
        values: Map<String, String>,
        needSign: Boolean
    ): String {
        var result = url
        values.forEach { (macro, value) ->
            if (result.contains(macro)) {
                result = result.replace(macro, encode(value))
            }
        }
        if (needSign && result.contains(REM_SIGN)) {
            result = result.replace(REM_SIGN, computeSign(result).orEmpty())
        }
        return result
    }

    /**
     * 从 URL query 取 7 个参数（缺失跳过）→ 按 key 字母升序拼接 → 尾部拼 token → MD5 小写 32 位。
     */
    private fun computeSign(url: String): String? {
        val token = SdkConfig.current?.appInitData?.token
        if (token.isNullOrEmpty()) {
            return null
        }
        return runCatching {
            val uri = Uri.parse(url)
            val payload = SIGN_KEYS.mapNotNull { key ->
                uri.getQueryParameter(key)?.takeIf { it.isNotEmpty() }?.let { "$key=$it" }
            }.joinToString("&") + token
            md5LowerHex(payload)
        }.getOrNull()
    }

    private fun md5LowerHex(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }

    fun buildDefaultValues(): Map<String, String> {
        val config = SdkConfig.current
        val context = config?.applicationContext
        val metrics = context?.resources?.displayMetrics
        return mapOf(
            TS to (System.currentTimeMillis() / 1000L).toString(),
            UA to DeviceUtils.userAgent(context),
            IP to config?.appInitData?.clientIp.orEmpty(),
            MAC to DeviceUtils.mac(context),
            SW to (metrics?.widthPixels ?: 0).toString(),
            SH to (metrics?.heightPixels ?: 0).toString(),
            IMEI to DeviceUtils.imei(context),
            OS to "Android",
            ANDROID_ID to context?.let(DeviceUtils::androidId).orEmpty(),
            OAID to DeviceUtils.oaid(context),
            APP_ID to DeviceUtils.appId(context),
            PACKAGE_NAME to context?.packageName.orEmpty(),
            SDK_VERSION to BuildConfig.SDK_VERSION,
            PHONE_BRAND to DeviceUtils.phoneBrand(context),
            NETWORK to DeviceUtils.networkType(context)
        )
    }
}