package com.lamps.sdk.reward.provider

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.lamps.sdk.core.LampsErrorCode
import com.lamps.sdk.data.sdk.channel.NoahSdkManager
import com.lamps.sdk.data.sdk.channel.ThirdSdkInitCallback
import com.lamps.sdk.reward.RewardAdCallback
import com.lamps.sdk.reward.RewardAdEnvironment
import com.lamps.sdk.reward.RewardAdErrorCode
import com.lamps.sdk.reward.RewardAdProvider
import com.lamps.sdk.reward.RewardAdSlot
import com.noah.api.RequestInfo
import com.noah.api.RewardedVideoAd
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

internal class NoahRewardAdProvider : RewardAdProvider {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val initLock = Any()
    private val initCallbacks = ArrayList<(Int?, String?) -> Unit>()

    private var initState = InitState.IDLE
    private var initializedAppId: String? = null

    override fun supports(slot: RewardAdSlot): Boolean {
        val channelName = slot.channelName.trim().lowercase(Locale.US)
        return channelName == "noah" ||
            channelName == "huichuan" ||
            channelName == "汇川" ||
            slot.channelId.trim() == NOAH_CHANNEL_ID
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
                    immediateError = "NoahSdk is using another appId: $initializedAppId"
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
                callback(LampsErrorCode.NOAH_SDK_INIT_FAILED, immediateError)
            }
            return
        }
        if (!shouldStart) return

        NoahSdkManager.initSdk(
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
                callback.onError(RewardAdErrorCode.LOAD_TIMEOUT, "Noah reward ad load timed out")
            }
        }
        mainHandler.postDelayed(timeout, LOAD_TIMEOUT_MS)

        RewardedVideoAd.getAd(
            activity,
            slotId,
            RequestInfo(),
            object : RewardedVideoAd.AdListener {
                override fun onAdError(error: com.noah.api.AdError?) {
                    mainHandler.removeCallbacks(timeout)
                    if (finished.compareAndSet(false, true)) {
                        callback.onError(
                            error?.errorCode ?: RewardAdErrorCode.PROVIDER_ERROR,
                            error?.errorMessage
                        )
                    }
                }

                override fun onAdLoaded(ad: RewardedVideoAd?) {
                    if (finished.get()) return
                    mainHandler.removeCallbacks(timeout)
                    loaded.set(true)
                    if (ad == null || !ad.isValid) {
                        if (finished.compareAndSet(false, true)) {
                            callback.onError(
                                RewardAdErrorCode.PROVIDER_ERROR,
                                "Noah reward ad is null or invalid"
                            )
                        }
                        return
                    }
                    callback.onLoaded()
                    runCatching { ad.show() }.onFailure { error ->
                        if (finished.compareAndSet(false, true)) {
                            callback.onError(
                                RewardAdErrorCode.PROVIDER_ERROR,
                                error.message ?: "Noah reward ad show failed"
                            )
                        }
                    }
                }

                override fun onAdShown(ad: RewardedVideoAd?) {
                    if (!finished.get()) callback.onShown()
                }

                override fun onAdClicked(ad: RewardedVideoAd?) = Unit

                override fun onAdClosed(ad: RewardedVideoAd?) {
                    if (finished.compareAndSet(false, true)) callback.onClosed()
                }

                override fun onVideoStart(ad: RewardedVideoAd?) = Unit

                override fun onVideoEnd(ad: RewardedVideoAd?) = Unit

                override fun onRewarded(ad: RewardedVideoAd?) {
                    if (!finished.get()) callback.onRewarded()
                }
            }
        )
    }

    private enum class InitState {
        IDLE,
        INITIALIZING,
        READY
    }

    private companion object {
        const val NOAH_CHANNEL_ID = "417"
        const val LOAD_TIMEOUT_MS = 15_000L
    }
}
