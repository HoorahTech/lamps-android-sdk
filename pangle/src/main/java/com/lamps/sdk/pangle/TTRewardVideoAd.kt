package com.lamps.sdk.pangle

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.TTAdLoadType
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTRewardVideoAd as PangleRewardVideoAd
import com.lamps.sdk.data.sdk.channel.RewardAdSdkLoadCallback
import com.lamps.sdk.data.sdk.channel.RewardVideoAd
import com.lamps.sdk.data.sdk.reward.RewardAdErrorCode
import com.lamps.sdk.reward.RewardAdShowCallback
import com.lamps.sdk.utils.SdkLog
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @param serverBid 穿山甲 BD 模式：先用 SDK bidding token 请求服务端竞价拿 adm，再带 adm 加载。
 */
internal class TTRewardVideoAd(
    private val activity: Activity,
    private val slotId: String,
    private val serverBid: Boolean,
    private val requestId: String
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

    /** 服务端竞价出价，单位：分。SDK 实时价拿不到时用它参与比价。 */
    @Volatile
    private var serverBidPrice: Double = 0.0

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
            ?: serverBidPrice
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
        serverBidPrice = 0.0
        loadFinished.set(false)
        if (serverBid) {
            requestServerBid()
        } else {
            loadRewardVideo(adm = null)
        }
    }

    private fun requestServerBid() {
        TTRewardServerBidder.requestBid(
            requestId = requestId,
            slotId = slotId
        ) { result ->
            if (loadFinished.get() || loadCallback == null) return@requestBid
            result.fold(
                onSuccess = { bidAd ->
                    serverBidPrice = bidAd.price
                    SdkLog.d("pangle server bid success slotId=$slotId price=${bidAd.price}")
                    loadRewardVideo(bidAd.adm)
                },
                onFailure = { error ->
                    val code = if (error is TimeoutException) {
                        RewardAdErrorCode.SERVER_BID_TIMEOUT
                    } else {
                        RewardAdErrorCode.SERVER_BID_FAILED
                    }
                    finishLoad {
                        loadCallback?.onLoadFailed(
                            code,
                            error.message ?: "Pangle server bid failed",
                            this@TTRewardVideoAd
                        )
                    }
                }
            )
        }
    }

    private fun buildAdSlot(adm: String?): AdSlot {
        return AdSlot.Builder()
            .setCodeId(slotId)
            .setAdLoadType(TTAdLoadType.LOAD)
            .apply { if (!adm.isNullOrBlank()) withBid(adm) }
            .build()
    }

    private fun loadRewardVideo(adm: String?) {
        mainHandler.postDelayed(timeout, LOAD_TIMEOUT_MS)
        val adSlot = buildAdSlot(adm)
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
                        SdkLog.i(
                            "pangle onRewardArrived isRewardValid=$isRewardValid " +
                                "rewardType=$rewardType extraInfo=$extraInfo"
                        )
                        showCallback?.onAdRewarded()
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

    override fun release() {
        mainHandler.removeCallbacks(timeout)
        loadCallback = null
        showCallback = null
        rewardAd = null
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
