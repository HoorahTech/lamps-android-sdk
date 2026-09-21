package com.lamps.sdk.data.sdk.reward

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.lamps.sdk.config.SdkConfig
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
    private var flowStartTime: Long = 0L

    @Volatile
    private var destroyed = false

    /** 本次流程拿到的广告，destroy 时要断开它对 WebView 的回调引用。 */
    @Volatile
    private var pendingAd: LampsRewardAd? = null

    override fun executeAsync(
        webView: LampsWebView,
        methodName: String,
        params: JSONObject,
        callbackId: String?,
        callback: LampsNativeCallback
    ) {
        if (!tryAcquireFlow()) {
            sendEvent(webView, "onBusy")
            return
        }

        val activity = webView.context.findActivity()
        val config = SdkConfig.current
        val failure = when {
            !SdkRuntime.isReady() || config == null ->
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
            .filter { it.slotId.isNotBlank() }
        if (validCandidates.isEmpty()) {
            sendError(webView, RewardAdErrorCode.INVALID_SLOT, "slotId is empty")
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

    private fun tryAcquireFlow(): Boolean {
        val now = System.currentTimeMillis()
        if (flowInProgress.get() && now - flowStartTime <= 8000) {
            return false
        }
        flowInProgress.set(false)
        if (flowInProgress.compareAndSet(false, true)) {
            flowStartTime = now
            return true
        }
        return false
    }

    override fun destroy() {
        destroyed = true
        flowInProgress.set(false)
        // 正在播放的广告不能在这里 release：断开渠道 showCallback 会连带打掉挂在
        // SdkRewardDispatcher 展示回调上的 PM/DM/CM 上报，而 PM 和 DM 是带签名的结算口径。
        // 这种情况交给 onAdClosed / onAdShowFailed 收口，引用最多留到广告关闭。
        pendingAd?.takeIf { !it.isShowingOrShown }?.release()
        pendingAd = null
    }

    private fun createLoadCallback(
        webView: LampsWebView,
        activity: Activity
    ): RewardAdLoadCallback {
        return object : RewardAdLoadCallback {
            override fun onAdLoadSuccess(ad: LampsRewardAd) {
                // WebView 已销毁时不能再展示：内嵌模式下宿主 Activity 还活着，
                // 激励视频会盖在宿主界面上播，而且没有页面能收到奖励回调。
                if (destroyed) {
                    ad.release()
                    return
                }
                pendingAd = ad
                sendEvent(webView, "onLoadSuccess")
                ad.show(activity, createShowCallback(webView))
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
                pendingAd = null
                sendEvent(webView, "onClose", rewardStatus = rewarded)
            }

            override fun onAdShowFailed(code: Int, message: String?) {
                pendingAd = null
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
        const val METHOD_SHOW_REWARDED_VIDEO = "lamps.ad.showRewardedVideo"
        const val EVENT_REWARDED_VIDEO_STATUS = "lamps.ad.rewardedVideoStatus"
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
