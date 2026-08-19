package com.lamps.sdk.reward.provider

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.TTAdLoadType
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTRewardVideoAd
import com.lamps.sdk.core.LampsErrorCode
import com.lamps.sdk.data.sdk.channel.TTSdkManager
import com.lamps.sdk.data.sdk.channel.ThirdSdkInitCallback
import com.lamps.sdk.reward.RewardAdCallback
import com.lamps.sdk.reward.RewardAdEnvironment
import com.lamps.sdk.reward.RewardAdErrorCode
import com.lamps.sdk.reward.RewardAdProvider
import com.lamps.sdk.reward.RewardAdSlot
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

internal class PangleRewardAdProvider : RewardAdProvider {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val initLock = Any()
    private val initCallbacks = ArrayList<(Int?, String?) -> Unit>()

    private var initState = InitState.IDLE
    private var initializedAppId: String? = null

    override fun supports(slot: RewardAdSlot): Boolean {
        val channelName = slot.channelName.trim().lowercase(Locale.US)
        val channelId = slot.channelId.trim()
        return channelName == "pangle" ||
            channelName == "csj" ||
            channelName == "穿山甲" ||
            channelId == PANGLE_CHANNEL_ID ||
            channelId == PANGLE_BID_CHANNEL_ID
    }

    override fun loadAndShow(
        activity: Activity,
        slot: RewardAdSlot,
        environment: RewardAdEnvironment,
        callback: RewardAdCallback
    ) {
        ensureInitialized(slot.appId, environment) { code, message ->
            if (code != null) callback.onError(code, message)
            else loadRewardAd(activity, slot.slotId, callback)
        }
    }

    private fun ensureInitialized(
        appId: String,
        environment: RewardAdEnvironment,
        callback: (Int?, String?) -> Unit
    ) {
        var shouldStart = false
        var immediateError: String? = null
        synchronized(initLock) {
            when {
                initState == InitState.READY && initializedAppId == appId -> {
                    mainHandler.post { callback(null, null) }
                    return
                }

                initState != InitState.IDLE && initializedAppId != appId -> {
                    immediateError = "TTAdSdk is using another appId: $initializedAppId"
                }

                else -> {
                    initCallbacks.add(callback)
                    if (initState == InitState.IDLE) {
                        initState = InitState.INITIALIZING
                        initializedAppId = appId
                        shouldStart = true
                    }
                }
            }
        }
        if (immediateError != null) {
            mainHandler.post {
                callback(LampsErrorCode.TT_SDK_INIT_FAILED, immediateError)
            }
            return
        }
        if (!shouldStart) return

        TTSdkManager.initSdk(
            environment.application,
            appId,
            object : ThirdSdkInitCallback {
                override fun success() {
                    finishInitialization(null, null)
                }

                override fun fail(code: Int, message: String?) {
                    finishInitialization(code, message)
                }
            }
        )
    }

    private fun finishInitialization(code: Int?, message: String?) {
        val callbacks: List<(Int?, String?) -> Unit>
        synchronized(initLock) {
            initState = if (code == null) InitState.READY else InitState.IDLE
            if (code != null) initializedAppId = null
            callbacks = initCallbacks.toList()
            initCallbacks.clear()
        }
        mainHandler.post { callbacks.forEach { it(code, message) } }
    }

    private fun loadRewardAd(
        activity: Activity,
        slotId: String,
        callback: RewardAdCallback
    ) {
        val loaded = AtomicBoolean(false)
        val finished = AtomicBoolean(false)
        val timeout = Runnable {
            if (!loaded.get() && finished.compareAndSet(false, true)) {
                callback.onError(RewardAdErrorCode.LOAD_TIMEOUT, "Pangle reward ad load timed out")
            }
        }
        mainHandler.postDelayed(timeout, LOAD_TIMEOUT_MS)

        val adSlot = AdSlot.Builder()
            .setCodeId(slotId)
            .setAdLoadType(TTAdLoadType.LOAD)
            .build()
        TTAdSdk.getAdManager()
            .createAdNative(activity)
            .loadRewardVideoAd(adSlot, object : TTAdNative.RewardVideoAdListener {
                override fun onError(code: Int, message: String?) {
                    mainHandler.removeCallbacks(timeout)
                    if (finished.compareAndSet(false, true)) {
                        callback.onError(code, message)
                    }
                }

                override fun onRewardVideoAdLoad(ad: TTRewardVideoAd?) {
                    if (finished.get()) return
                    mainHandler.removeCallbacks(timeout)
                    loaded.set(true)
                    if (ad == null) {
                        if (finished.compareAndSet(false, true)) {
                            callback.onError(
                                RewardAdErrorCode.PROVIDER_ERROR,
                                "Pangle reward ad is null"
                            )
                        }
                        return
                    }
                    callback.onLoaded()
                    ad.setRewardAdInteractionListener(
                        createInteractionListener(finished, callback)
                    )
                    runCatching { ad.showRewardVideoAd(activity) }.onFailure { error ->
                        if (finished.compareAndSet(false, true)) {
                            callback.onError(
                                RewardAdErrorCode.PROVIDER_ERROR,
                                error.message ?: "Pangle reward ad show failed"
                            )
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onRewardVideoCached() = Unit

                override fun onRewardVideoCached(ad: TTRewardVideoAd?) = Unit
            })
    }

    private fun createInteractionListener(
        finished: AtomicBoolean,
        callback: RewardAdCallback
    ): TTRewardVideoAd.RewardAdInteractionListener {
        return object : TTRewardVideoAd.RewardAdInteractionListener {
            override fun onAdShow() {
                if (!finished.get()) callback.onShown()
            }

            override fun onAdVideoBarClick() = Unit

            override fun onAdClose() {
                if (finished.compareAndSet(false, true)) callback.onClosed()
            }

            override fun onVideoComplete() = Unit

            override fun onVideoError() {
                if (finished.compareAndSet(false, true)) {
                    callback.onError(
                        RewardAdErrorCode.PROVIDER_ERROR,
                        "Pangle reward video playback failed"
                    )
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onRewardVerify(
                rewardVerify: Boolean,
                rewardAmount: Int,
                rewardName: String?,
                errorCode: Int,
                errorMessage: String?
            ) = Unit

            override fun onRewardArrived(
                isRewardValid: Boolean,
                rewardType: Int,
                extraInfo: Bundle?
            ) {
                if (isRewardValid && !finished.get()) callback.onRewarded()
            }

            override fun onSkippedVideo() = Unit
        }
    }

    private enum class InitState {
        IDLE,
        INITIALIZING,
        READY
    }

    private companion object {
        const val PANGLE_CHANNEL_ID = "2"
        const val PANGLE_BID_CHANNEL_ID = "327"
        const val LOAD_TIMEOUT_MS = 15_000L
    }
}
