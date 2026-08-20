package com.lamps.sdk.data.sdk.channel

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.lamps.sdk.data.sdk.reward.RewardAdErrorCode
import com.lamps.sdk.reward.RewardAdShowCallback
import com.lamps.sdk.utils.SdkLog
import com.noah.api.AdError
import com.noah.api.BiddingLossReason
import com.noah.api.RequestInfo
import com.noah.api.RewardedVideoAd
import java.util.concurrent.atomic.AtomicBoolean

internal class NoahRewardVideoAd(
    private val activity: Activity,
    private val slotId: String
) : RewardVideoAd() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val loadFinished = AtomicBoolean(false)
    private val timeout = Runnable {
        finishLoad {
            loadCallback?.onLoadFailed(
                RewardAdErrorCode.LOAD_TIMEOUT,
                "Noah reward ad load timed out",
                this@NoahRewardVideoAd
            )
        }
    }

    @Volatile
    private var rewardAd: RewardedVideoAd? = null

    @Volatile
    private var loadCallback: RewardAdSdkLoadCallback? = null

    @Volatile
    private var showCallback: RewardAdShowCallback? = null

    override fun getPrice(): Double {
        return rewardAd?.price?.takeIf { it > 0.0 } ?: 0.0
    }

    internal fun hasLoadedAd(): Boolean = rewardAd != null

    internal fun sendWinNotification(price: Int) {
        val ad = rewardAd ?: return
        runCatching { ad.sendWinNotification(price) }
            .onFailure { SdkLog.w("noah sendWinNotification failed", it) }
    }

    internal fun sendLossNotification(price: Int) {
        val ad = rewardAd ?: return
        runCatching { ad.sendLossNotification(price, BiddingLossReason.LOW_PRICE) }
            .onFailure { SdkLog.w("noah sendLossNotification failed", it) }
    }

    override fun loadAD(callback: RewardAdSdkLoadCallback) {
        loadCallback = callback
        showCallback = null
        rewardAd = null
        loadFinished.set(false)
        mainHandler.postDelayed(timeout, LOAD_TIMEOUT_MS)
        RewardedVideoAd.getAd(
            activity,
            slotId,
            RequestInfo(),
            object : NoahRewardAdListenerAdapter() {
                override fun onAdError(error: AdError?) {
                    mainHandler.removeCallbacks(timeout)
                    finishLoad {
                        loadCallback?.onLoadFailed(
                            error?.errorCode ?: RewardAdErrorCode.PROVIDER_ERROR,
                            error?.errorMessage,
                            this@NoahRewardVideoAd
                        )
                    }
                }

                override fun onAdLoaded(ad: RewardedVideoAd?) {
                    if (loadFinished.get()) return
                    mainHandler.removeCallbacks(timeout)
                    if (ad == null || !ad.isValid) {
                        finishLoad {
                            loadCallback?.onLoadFailed(
                                RewardAdErrorCode.PROVIDER_ERROR,
                                "Noah reward ad is null or invalid",
                                this@NoahRewardVideoAd
                            )
                        }
                        return
                    }
                    rewardAd = ad
                    finishLoad {
                        loadCallback?.onLoadSuccess(this@NoahRewardVideoAd)
                    }
                }
            }
        )
    }

    override fun showAD(activity: Activity, callback: RewardAdShowCallback) {
        val ad = rewardAd
        if (ad == null || !ad.isValid) {
            callback.onAdShowFailed(
                RewardAdErrorCode.PROVIDER_ERROR,
                "Noah reward ad is invalid"
            )
            return
        }
        loadCallback = null
        showCallback = callback
        runCatching {
            ad.setAdListener(
                object : NoahRewardAdListenerAdapter() {
                    override fun onAdError(error: AdError?) {
                        showCallback?.onAdShowFailed(
                            error?.errorCode ?: RewardAdErrorCode.PROVIDER_ERROR,
                            error?.errorMessage
                        )
                        showCallback = null
                    }

                    override fun onAdShown(ad: RewardedVideoAd?) {
                        showCallback?.onAdShown()
                    }

                    override fun onAdClicked(ad: RewardedVideoAd?) {
                        showCallback?.onAdClicked()
                    }

                    override fun onAdClosed(ad: RewardedVideoAd?) {
                        showCallback?.onAdClosed()
                        showCallback = null
                    }

                    override fun onRewarded(ad: RewardedVideoAd?) {
                        showCallback?.onAdRewarded()
                    }
                }
            )
            ad.show()
        }.onFailure { error ->
            showCallback = null
            callback.onAdShowFailed(
                RewardAdErrorCode.PROVIDER_ERROR,
                error.message ?: "Noah reward ad show failed"
            )
        }
    }

    private fun finishLoad(action: () -> Unit): Boolean {
        if (!loadFinished.compareAndSet(false, true)) return false
        action()
        loadCallback = null
        return true
    }

    private companion object {
        const val LOAD_TIMEOUT_MS = 15_000L
    }
}
