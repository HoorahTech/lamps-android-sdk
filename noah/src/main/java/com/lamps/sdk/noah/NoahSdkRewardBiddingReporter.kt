package com.lamps.sdk.noah

import com.lamps.sdk.reward.LampsRewardAd
import com.lamps.sdk.utils.SdkLog
import kotlin.math.roundToInt

/**
 * 汇川 SDK 激励视频客户端竞价结果回传。
 * 竞价结束、曝光前必须上报，否则影响计费。
 */
internal object NoahSdkRewardBiddingReporter {

    fun reportRewardBidding(allItems: List<LampsRewardAd>, winner: LampsRewardAd?) {
        val noahItems = allItems.mapNotNull { item ->
            val ad = item.adData as? NoahRewardVideoAd ?: return@mapNotNull null
            if (!ad.hasLoadedAd()) return@mapNotNull null
            item to ad
        }
        if (noahItems.isEmpty()) return

        val winnerPrice = winner?.price.toNoahBidPrice()
        noahItems.forEach { (item, ad) ->
            val isWin = winner != null && item === winner
            if (isWin) {
                ad.sendWinNotification(winnerPrice)
                SdkLog.d("汇川激励视频竞价回传-胜-price:$winnerPrice")
            } else {
                val lossPrice = winnerPrice.takeIf { it > 0 } ?: item.price.toNoahBidPrice()
                ad.sendLossNotification(lossPrice)
                SdkLog.d(
                    "汇川激励视频竞价回传-负-lossPrice:$lossPrice,selfPrice:${item.price}"
                )
            }
        }
    }

    private fun Double?.toNoahBidPrice(): Int {
        if (this == null || this <= 0.0) return 0
        // LampsRewardAd.price 已统一为分，汇川 win/loss 回传同样用分。
        return roundToInt()
    }
}
