package com.lamps.sdk.data.sdk.channel

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.lamps.sdk.data.sdk.reward.RewardAdErrorCode
import com.lamps.sdk.reward.RewardAdShowCallback
import com.lamps.sdk.utils.SdkLog
import com.qq.e.ads.rewardvideo.RewardVideoAD
import com.qq.e.comm.constants.BiddingLossReason
import com.qq.e.comm.pi.IBidding
import com.qq.e.comm.util.AdError
import java.util.concurrent.atomic.AtomicBoolean

internal class YLHRewardVideoAd(
    activity: Activity,
    slotId: String
) : RewardVideoAd() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val loadFinished = AtomicBoolean(false)
    private val loaded = AtomicBoolean(false)
    private val listener = object : YLHRewardVideoADListenerAdapter() {
        override fun onADLoad() {
            val callback = loadCallback ?: return
            mainHandler.removeCallbacks(timeout)
            if (rewardAd.hasShown() || !rewardAd.isValid) {
                finishLoad {
                    callback.onLoadFailed(
                        RewardAdErrorCode.PROVIDER_ERROR,
                        "YLH reward ad is invalid",
                        this@YLHRewardVideoAd
                    )
                }
                return
            }
            finishLoad {
                loaded.set(true)
                callback.onLoadSuccess(this@YLHRewardVideoAd)
            }
        }

        override fun onADShow() {
            showCallback?.onAdShown()
        }

        override fun onADClick() {
            showCallback?.onAdClicked()
        }

        override fun onReward(data: MutableMap<String, Any>?) {
            showCallback?.onAdRewarded()
        }

        override fun onADClose() {
            showCallback?.onAdClosed()
            showCallback = null
        }

        override fun onError(error: AdError?) {
            mainHandler.removeCallbacks(timeout)
            val code = error?.errorCode ?: RewardAdErrorCode.PROVIDER_ERROR
            val message = error?.errorMsg
            if (finishLoad { loadCallback?.onLoadFailed(code, message, this@YLHRewardVideoAd) }) {
                return
            }
            showCallback?.onAdShowFailed(code, message)
            showCallback = null
        }
    }
    private val rewardAd: RewardVideoAD

    init {
        rewardAd = RewardVideoAD(activity, slotId, listener, false)
    }
    private val timeout = Runnable {
        finishLoad {
            loadCallback?.onLoadFailed(
                RewardAdErrorCode.LOAD_TIMEOUT,
                "YLH reward ad load timed out",
                this@YLHRewardVideoAd
            )
        }
    }

    @Volatile
    private var loadCallback: RewardAdSdkLoadCallback? = null

    @Volatile
    private var showCallback: RewardAdShowCallback? = null

    override fun getPrice(): Double {
        // 优量汇官方：getECPM() 单位是分；-1 表示无权限或后台异常。
        return rewardAd.ecpm.takeIf { it > 0 }?.toDouble() ?: 0.0
    }

    internal fun hasLoadedAd(): Boolean = loaded.get() && rewardAd.isValid && !rewardAd.hasShown()

    internal fun sendWinNotification(expectCostPrice: Int, highestLossPrice: Int) {
        runCatching {
            rewardAd.sendWinNotification(
                hashMapOf<String, Any>(
                    IBidding.EXPECT_COST_PRICE to expectCostPrice,
                    IBidding.HIGHEST_LOSS_PRICE to highestLossPrice
                )
            )
            rewardAd.setBidECPM(expectCostPrice)
        }.onFailure { SdkLog.w("ylh sendWinNotification failed", it) }
    }

    internal fun sendLossNotification(winPrice: Int, adnId: String) {
        runCatching {
            rewardAd.sendLossNotification(
                hashMapOf<String, Any>(
                    IBidding.WIN_PRICE to winPrice,
                    IBidding.LOSS_REASON to BiddingLossReason.LOW_PRICE,
                    IBidding.ADN_ID to adnId
                )
            )
        }.onFailure { SdkLog.w("ylh sendLossNotification failed", it) }
    }

    override fun loadAD(callback: RewardAdSdkLoadCallback) {
        loadCallback = callback
        showCallback = null
        loaded.set(false)
        loadFinished.set(false)
        mainHandler.postDelayed(timeout, LOAD_TIMEOUT_MS)
        rewardAd.loadAD()
    }

    override fun showAD(activity: Activity, callback: RewardAdShowCallback) {
        if (rewardAd.hasShown() || !rewardAd.isValid) {
            callback.onAdShowFailed(
                RewardAdErrorCode.PROVIDER_ERROR,
                "YLH reward ad is invalid or has already been shown"
            )
            return
        }
        loadCallback = null
        showCallback = callback
        runCatching { rewardAd.showAD(activity) }.onFailure { error ->
            showCallback = null
            callback.onAdShowFailed(
                RewardAdErrorCode.PROVIDER_ERROR,
                error.message ?: "YLH reward ad show failed"
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
