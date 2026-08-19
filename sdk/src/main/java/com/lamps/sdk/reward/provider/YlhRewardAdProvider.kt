package com.lamps.sdk.reward.provider

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.lamps.sdk.core.LampsErrorCode
import com.lamps.sdk.data.sdk.channel.ThirdSdkInitCallback
import com.lamps.sdk.data.sdk.channel.YLHSdkManager
import com.lamps.sdk.reward.RewardAdCallback
import com.lamps.sdk.reward.RewardAdEnvironment
import com.lamps.sdk.reward.RewardAdErrorCode
import com.lamps.sdk.reward.RewardAdProvider
import com.lamps.sdk.reward.RewardAdSlot
import com.qq.e.ads.rewardvideo.RewardVideoAD
import com.qq.e.ads.rewardvideo.RewardVideoADListener
import com.qq.e.comm.util.AdError
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

internal class YlhRewardAdProvider : RewardAdProvider {
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var initializedAppId: String? = null

    override fun supports(slot: RewardAdSlot): Boolean {
        val channelName = slot.channelName.trim().lowercase(Locale.US)
        val channelId = slot.channelId.trim()
        return channelName == "ylh" ||
            channelName == "gdt" ||
            channelName == "优量汇" ||
            channelName == "优良汇" ||
            channelId == YLH_CHANNEL_ID ||
            channelId == YLH_BID_CHANNEL_ID
    }

    override fun loadAndShow(
        activity: Activity,
        slot: RewardAdSlot,
        environment: RewardAdEnvironment,
        callback: RewardAdCallback
    ) {
        val initializedWith = initializeIfNeeded(slot.appId, environment).getOrElse { error ->
            callback.onError(
                RewardAdErrorCode.PROVIDER_ERROR,
                error.message ?: "GDTAdSdk init failed"
            )
            return
        }
        if (initializedWith != slot.appId) {
            callback.onError(
                LampsErrorCode.YLH_SDK_INIT_FAILED,
                "GDTAdSdk is using another appId: $initializedWith"
            )
            return
        }
        loadRewardAd(activity, slot.slotId, callback)
    }

    @Synchronized
    private fun initializeIfNeeded(
        appId: String,
        environment: RewardAdEnvironment
    ): Result<String> {
        initializedAppId?.let { return Result.success(it) }
        var result: Result<String>? = null
        YLHSdkManager.initSdk(
            environment.application,
            appId,
            object : ThirdSdkInitCallback {
                override fun success() {
                    initializedAppId = appId
                    result = Result.success(appId)
                }

                override fun fail(code: Int, message: String?) {
                    result = Result.failure(
                        IllegalStateException(
                            "GDTAdSdk init failed: code=$code, message=${message.orEmpty()}"
                        )
                    )
                }
            }
        )
        return result ?: Result.failure(
            IllegalStateException("GDTAdSdk init did not return a result")
        )
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
                callback.onError(RewardAdErrorCode.LOAD_TIMEOUT, "YLH reward ad load timed out")
            }
        }
        mainHandler.postDelayed(timeout, LOAD_TIMEOUT_MS)

        lateinit var rewardAd: RewardVideoAD
        val listener = object : RewardVideoADListener {
            override fun onADLoad() {
                if (finished.get()) return
                mainHandler.removeCallbacks(timeout)
                loaded.set(true)
                if (rewardAd.hasShown() || !rewardAd.isValid) {
                    if (finished.compareAndSet(false, true)) {
                        callback.onError(
                            RewardAdErrorCode.PROVIDER_ERROR,
                            "YLH reward ad is invalid"
                        )
                    }
                    return
                }
                callback.onLoaded()
                runCatching { rewardAd.showAD(activity) }.onFailure { error ->
                    if (finished.compareAndSet(false, true)) {
                        callback.onError(
                            RewardAdErrorCode.PROVIDER_ERROR,
                            error.message ?: "YLH reward ad show failed"
                        )
                    }
                }
            }

            override fun onVideoCached() = Unit

            override fun onADShow() {
                if (!finished.get()) callback.onShown()
            }

            override fun onADExpose() = Unit

            override fun onReward(data: MutableMap<String, Any>?) {
                if (!finished.get()) callback.onRewarded()
            }

            override fun onADClick() = Unit

            override fun onVideoComplete() = Unit

            override fun onADClose() {
                if (finished.compareAndSet(false, true)) callback.onClosed()
            }

            override fun onError(error: AdError?) {
                mainHandler.removeCallbacks(timeout)
                if (finished.compareAndSet(false, true)) {
                    callback.onError(
                        error?.errorCode ?: RewardAdErrorCode.PROVIDER_ERROR,
                        error?.errorMsg
                    )
                }
            }
        }
        rewardAd = RewardVideoAD(activity, slotId, listener, false)
        rewardAd.loadAD()
    }

    private companion object {
        const val YLH_CHANNEL_ID = "349"
        const val YLH_BID_CHANNEL_ID = "348"
        const val LOAD_TIMEOUT_MS = 15_000L
    }
}
