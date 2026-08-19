package com.lamps.sdk.reward

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.lamps.sdk.LampsSdk
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.webview.LampsWebView
import com.lamps.sdk.webview.bridge.LampsAbility
import com.lamps.sdk.webview.bridge.LampsAbilityInstaller
import com.lamps.sdk.webview.bridge.LampsNativeCallback
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

internal object RewardAdAbilityInstaller : LampsAbilityInstaller() {
    override fun createAbilities(): Array<LampsAbility> = arrayOf(RewardAdAbility())
}

private class RewardAdAbility : LampsAbility {
    override val names: Array<String> = arrayOf(METHOD_SHOW_REWARDED_VIDEO)

    private val flowInProgress = AtomicBoolean(false)

    @Volatile
    private var destroyed = false

    override fun executeAsync(
        webView: LampsWebView,
        methodName: String,
        params: JSONObject,
        callbackId: String?,
        callback: LampsNativeCallback
    ) {
        if (!flowInProgress.compareAndSet(false, true)) {
            callback.callback(
                errorResult(RewardAdErrorCode.FLOW_IN_PROGRESS, "reward ad flow is in progress"),
                callbackId
            )
            return
        }

        val activity = webView.context.findActivity()
        val config = LampsConfig.current
        val failure = when {
            !LampsSdk.isSdkReady() || config == null ->
                RewardAdErrorCode.SDK_NOT_READY to "LampsSdk is not ready"

            activity == null ->
                RewardAdErrorCode.ACTIVITY_NOT_FOUND to "WebView is not attached to an Activity"

            config.appInitData?.rewardAdSlots.isNullOrEmpty() ->
                RewardAdErrorCode.SLOT_NOT_FOUND to "reward ad slot is empty"

            else -> null
        }
        if (failure != null) {
            flowInProgress.set(false)
            callback.callback(errorResult(failure.first, failure.second), callbackId)
            return
        }

        BuiltInRewardAdProviders.installAvailable()
        val candidates = config!!.appInitData!!.rewardAdSlots.map {
            RewardAdSlot(
                slotId = it.slotId,
                type = it.type,
                channelName = it.channelName,
                channelId = it.channelId,
                appId = it.appId
            )
        }
        val validCandidates = candidates.filter { it.slotId.isNotBlank() && it.appId.isNotBlank() }
        if (validCandidates.isEmpty()) {
            flowInProgress.set(false)
            callback.callback(
                errorResult(RewardAdErrorCode.INVALID_SLOT, "slotId or appId is empty"),
                callbackId
            )
            return
        }

        val selected = validCandidates.firstNotNullOfOrNullCompat { slot ->
            RewardAdProviderRegistry.find(slot)?.let { provider -> slot to provider }
        }
        if (selected == null) {
            flowInProgress.set(false)
            callback.callback(
                errorResult(
                    RewardAdErrorCode.PROVIDER_NOT_FOUND,
                    "no installed provider supports the configured reward slots"
                ),
                callbackId
            )
            return
        }

        val (slot, provider) = selected
        callback.callback(
            JSONObject()
                .put("code", 0)
                .put("message", "accepted")
                .put("data", JSONObject().put("accepted", true)),
            callbackId
        )

        runCatching {
            provider.loadAndShow(
                activity!!,
                slot,
                RewardAdEnvironment(
                    application = activity.application,
                    oaid = config.resolveOaid(),
                    debug = config.debug
                ),
                createProviderCallback(webView)
            )
        }.onFailure { error ->
            sendError(
                webView,
                RewardAdErrorCode.PROVIDER_ERROR,
                error.message ?: "reward ad provider failed"
            )
        }
    }

    override fun destroy() {
        destroyed = true
        flowInProgress.set(false)
    }

    private fun createProviderCallback(webView: LampsWebView): RewardAdCallback {
        return object : RewardAdCallback {
            private var rewarded = false
            private var shown = false

            override fun onLoaded() {
                sendEvent(webView, "onLoadSuccess")
            }

            override fun onShown() {
                shown = true
                sendEvent(webView, "onShowSuccess")
            }

            override fun onRewarded() {
                rewarded = true
                sendEvent(webView, "onRewardArrived", rewardStatus = true)
            }

            override fun onClosed() {
                flowInProgress.set(false)
                sendEvent(webView, "onClose", rewardStatus = rewarded)
            }

            override fun onError(code: Int, message: String?) {
                flowInProgress.set(false)
                sendEvent(
                    webView,
                    if (shown) "onShowError" else "onLoadError",
                    code,
                    message ?: "reward ad failed"
                )
            }
        }
    }

    private fun sendError(webView: LampsWebView, code: Int, message: String) {
        flowInProgress.set(false)
        sendEvent(webView, "onLoadError", code, message)
    }

    private fun sendEvent(
        webView: LampsWebView,
        callbackName: String,
        code: Int? = null,
        message: String? = null,
        rewardStatus: Boolean? = null
    ) {
        if (destroyed) return
        val payload = JSONObject().put("callbackName", callbackName)
        if (code != null || message != null) {
            payload.put(
                "data",
                JSONObject()
                    .put("errCode", code ?: RewardAdErrorCode.PROVIDER_ERROR)
                    .put("errMsg", message.orEmpty())
            )
        }
        if (rewardStatus != null) {
            payload.put("rewardStatus", rewardStatus)
        }
        webView.send(EVENT_REWARDED_VIDEO_STATUS, payload)
    }

    private fun errorResult(code: Int, message: String): JSONObject {
        return JSONObject()
            .put("code", code)
            .put("message", message)
            .put("data", JSONObject())
    }

    private companion object {
        const val METHOD_SHOW_REWARDED_VIDEO = "hra.ad.showRewardedVideo"
        const val EVENT_REWARDED_VIDEO_STATUS = "hoorah.ad.rewardedVideoStatus"
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

private inline fun <T, R> Iterable<T>.firstNotNullOfOrNullCompat(
    transform: (T) -> R?
): R? {
    for (element in this) {
        val result = transform(element)
        if (result != null) return result
    }
    return null
}
