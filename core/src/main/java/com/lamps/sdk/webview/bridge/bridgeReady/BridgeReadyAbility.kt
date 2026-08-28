package com.lamps.sdk.webview.bridge.bridgeReady

import android.os.Build
import com.lamps.sdk.BuildConfig
import com.lamps.sdk.config.SdkConfig
import com.lamps.sdk.utils.DeviceUtils
import com.lamps.sdk.utils.LampsApiHost
import com.lamps.sdk.webview.LampsWebView
import com.lamps.sdk.webview.bridge.LampsAbility
import com.lamps.sdk.webview.bridge.LampsNativeCallback
import org.json.JSONObject

/** Supplies the host application's common runtime parameters to H5. */
internal class BridgeReadyAbility : LampsAbility {
    override val names: Array<String> = arrayOf(METHOD_BRIDGE_READY)

    override fun executeAsync(
        webView: LampsWebView,
        methodName: String,
        params: JSONObject,
        callbackId: String?,
        callback: LampsNativeCallback
    ) {
        callback.callback(buildParams(webView), callbackId)
    }

    private fun buildParams(webView: LampsWebView): JSONObject {
        val config = SdkConfig.current
        val context = config?.applicationContext ?: webView.context.applicationContext
        val metrics = context.resources.displayMetrics
        return JSONObject().apply {
            put("ua", DeviceUtils.userAgent(context))
            put("ip", config?.appInitData?.clientIp.orEmpty())
            put("mac", DeviceUtils.mac(context))
            put("imei", DeviceUtils.imei(context))
            put("os", "Android")
            put("platform", "Android")
            put("appid", DeviceUtils.appId(context))
            put("sdkVersion", BuildConfig.SDK_VERSION)
            put("phoneBrand", DeviceUtils.phoneBrand(context))
            put("network", DeviceUtils.networkType(context))
            put("androidId", DeviceUtils.androidId(context))
            put("oaid", DeviceUtils.oaid(context))
            put("appVer", DeviceUtils.appVersion(context))
            put("osVer", Build.VERSION.RELEASE.orEmpty())
            put("env", LampsApiHost.envName(context))
            put("packageName", context.packageName)
            put("clientWidth", metrics.widthPixels)
            put("clientHeight", metrics.heightPixels)
            put("density", metrics.density)
            put("statusBarHeight", DeviceUtils.statusBarHeight(context))
        }
    }

    private companion object {
        const val METHOD_BRIDGE_READY = "lamps.common.bridgeReady"
    }
}
