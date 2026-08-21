package com.lamps.sdk.data.sdk.channel

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.TTAdLoadType
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTRewardVideoAd as PangleRewardVideoAd
import com.lamps.sdk.data.sdk.reward.RewardAdErrorCode
import com.lamps.sdk.reward.RewardAdShowCallback
import java.util.concurrent.atomic.AtomicBoolean

internal class TTRewardVideoAd(
    private val activity: Activity,
    private val slotId: String
) : RewardVideoAd() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val loadFinished = AtomicBoolean(false)
    private val timeout = Runnable {
        finishLoad {
            loadCallback?.onLoadFailed(
                RewardAdErrorCode.LOAD_TIMEOUT,
                "Pangle reward ad load timed out",
                this@TTRewardVideoAd
            )
        }
    }

    @Volatile
    private var rewardAd: PangleRewardVideoAd? = null

    @Volatile
    private var loadCallback: RewardAdSdkLoadCallback? = null

    @Volatile
    private var showCallback: RewardAdShowCallback? = null

    override fun getPrice(): Double {
        val extra = rewardAd?.mediaExtraInfo ?: return 0.0
        // CSJ 客户端竞价读 price；GroMore 读 ecpm。官方竞价口径均为分。
        // 不读 cpm / 商品 effective_price，避免和竞价 eCPM 混单位。
        return readFen(extra, "price")
            ?: readFen(extra, "ecpm")
            ?: readFen(extra, "eCPM")
            ?: 0.0
    }

    private fun readFen(extra: Map<String, Any>, key: String): Double? {
        val raw = when (val value = extra[key]) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        } ?: return null
        return raw.takeIf { it > 0.0 }
    }

    override fun loadAD(callback: RewardAdSdkLoadCallback) {
        loadCallback = callback
        showCallback = null
        rewardAd = null
        loadFinished.set(false)
        mainHandler.postDelayed(timeout, LOAD_TIMEOUT_MS)
        val adSlot = AdSlot.Builder()
            .setCodeId(slotId)
            .setAdLoadType(TTAdLoadType.LOAD)
            .build()
        TTAdSdk.getAdManager()
            .createAdNative(activity)
            .loadRewardVideoAd(adSlot, object : TTRewardVideoAdListenerAdapter() {
                override fun onError(code: Int, message: String?) {
                    mainHandler.removeCallbacks(timeout)
                    finishLoad { loadCallback?.onLoadFailed(code, message, this@TTRewardVideoAd) }
                }

                override fun onRewardVideoAdLoad(ad: PangleRewardVideoAd?) {
                    if (loadFinished.get()) return
                    mainHandler.removeCallbacks(timeout)
                    if (ad == null) {
                        finishLoad {
                            loadCallback?.onLoadFailed(
                                RewardAdErrorCode.PROVIDER_ERROR,
                                "Pangle reward ad is null",
                                this@TTRewardVideoAd
                            )
                        }
                        return
                    }
                    rewardAd = ad
                    finishLoad {
                        loadCallback?.onLoadSuccess(this@TTRewardVideoAd)
                    }
                }
            })
    }

    override fun showAD(activity: Activity, callback: RewardAdShowCallback) {
        val ad = rewardAd
        if (ad == null) {
            callback.onAdShowFailed(
                RewardAdErrorCode.PROVIDER_ERROR,
                "Pangle reward ad is null"
            )
            return
        }
        loadCallback = null
        showCallback = callback
        runCatching {
            ad.setRewardAdInteractionListener(
                object : TTRewardAdInteractionListenerAdapter() {
                    override fun onAdShow() {
                        showCallback?.onAdShown()
                    }

                    override fun onAdVideoBarClick() {
                        showCallback?.onAdClicked()
                    }

                    override fun onAdClose() {
                        showCallback?.onAdClosed()
                        showCallback = null
                    }

                    override fun onVideoError() {
                        showCallback?.onAdShowFailed(
                            RewardAdErrorCode.PROVIDER_ERROR,
                            "Pangle reward video playback failed"
                        )
                        showCallback = null
                    }

                    override fun onRewardArrived(
                        isRewardValid: Boolean,
                        rewardType: Int,
                        extraInfo: Bundle?
                    ) {
                        if (isRewardValid) showCallback?.onAdRewarded()
                    }
                }
            )
            ad.showRewardVideoAd(activity)
        }.onFailure { error ->
            showCallback = null
            callback.onAdShowFailed(
                RewardAdErrorCode.PROVIDER_ERROR,
                error.message ?: "Pangle reward ad show failed"
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
        const val LOAD_TIMEOUT_MS = 1_500L
    }
}
