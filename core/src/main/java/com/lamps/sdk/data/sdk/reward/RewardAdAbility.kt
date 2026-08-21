package com.lamps.sdk.data.sdk.reward

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.lamps.sdk.LampsSdk
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.core.SdkRuntime
import com.lamps.sdk.reward.LampsRewardAd
import com.lamps.sdk.reward.RewardAdLoadCallback
import com.lamps.sdk.reward.RewardAdShowCallback
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
            sendError(
                webView,
                RewardAdErrorCode.FLOW_IN_PROGRESS,
                "reward ad flow is in progress",
                endFlow = false
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
            sendError(webView, failure.first, failure.second)
            return
        }

        val validCandidates = config!!.appInitData!!.rewardAdSlots
            .filter { it.slotId.isNotBlank() && it.appId.isNotBlank() }
        if (validCandidates.isEmpty()) {
            sendError(webView, RewardAdErrorCode.INVALID_SLOT, "slotId or appId is empty")
            return
        }

        runCatching {
            val forwardSource = params.optString("forward_source")
                .ifBlank { params.optString("forwardSource") }
            SdkRuntime.loadReward(
                activity!!,
                createLoadCallback(webView, activity),
                forwardSource
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

    private fun createLoadCallback(
        webView: LampsWebView,
        activity: Activity
    ): RewardAdLoadCallback {
        return object : RewardAdLoadCallback {
            override fun onAdLoadSuccess(ad: LampsRewardAd) {
                sendEvent(webView, "onLoadSuccess")
                ad.show(activity,createShowCallback(webView))
            }

            override fun onAdLoadFailed(code: Int, message: String?) {
                sendError(webView, code, message ?: "reward ad load failed")
            }
        }
    }

    private fun createShowCallback(webView: LampsWebView): RewardAdShowCallback {
        return object : RewardAdShowCallback {
            private var rewarded = false

            override fun onAdShown() {
                sendEvent(webView, "onShowSuccess")
            }

            override fun onAdRewarded() {
                rewarded = true
                sendEvent(webView, "onRewardArrived", rewardStatus = true)
            }

            override fun onAdClosed() {
                flowInProgress.set(false)
                sendEvent(webView, "onClose", rewardStatus = rewarded)
            }

            override fun onAdShowFailed(code: Int, message: String?) {
                sendError(
                    webView,
                    code,
                    message ?: "reward ad show failed",
                    callbackName = "onShowError"
                )
            }
        }
    }

    private fun sendError(
        webView: LampsWebView,
        code: Int,
        message: String,
        callbackName: String = "onLoadError",
        endFlow: Boolean = true
    ) {
        if (endFlow) {
            flowInProgress.set(false)
        }
        sendEvent(webView, callbackName, code, message)
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
