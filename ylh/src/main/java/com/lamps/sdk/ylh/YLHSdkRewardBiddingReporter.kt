package com.lamps.sdk.ylh

import com.lamps.sdk.data.sdk.channel.SdkChannel
import com.lamps.sdk.reward.LampsRewardAd
import com.lamps.sdk.utils.SdkLog
import kotlin.math.roundToInt

/**
 * 优量汇 SDK 激励视频客户端竞价结果回传。
 * 竞价结束、曝光前必须上报，否则影响计费。
 *
 * 价格单位均为分。输给第三方 ADN 时 ADN_ID 回传 `2`，输给其他优量汇 bidding 位时回传 `4`。
 */
internal object YLHSdkRewardBiddingReporter {

    private const val ADN_THIRD_PARTY = "2"
    private const val ADN_OTHER_YLH_BIDDING = "4"

    fun reportRewardBidding(allItems: List<LampsRewardAd>, winner: LampsRewardAd?) {
        val ylhItems = allItems.mapNotNull { item ->
            val ad = item.adData as? YLHRewardVideoAd ?: return@mapNotNull null
            if (!ad.hasLoadedAd()) return@mapNotNull null
            item to ad
        }
        if (ylhItems.isEmpty()) return

        val winnerPrice = winner?.price.toYlhBidPrice()
        val highestLossPrice = allItems
            .asSequence()
            .filter { it !== winner }
            .map { it.price.toYlhBidPrice() }
            .maxOrNull()
            ?: 0
        ylhItems.forEach { (item, ad) ->
            val isWin = winner != null && item === winner
            if (isWin) {
                ad.sendWinNotification(winnerPrice, highestLossPrice)
                SdkLog.d(
                    "优量汇激励视频竞价回传-胜-expectCost:$winnerPrice,highestLoss:$highestLossPrice"
                )
            } else {
                val lossPrice = winnerPrice.takeIf { it > 0 } ?: item.price.toYlhBidPrice()
                val adnId = if (SdkChannel.GDT.matches(winner?.channelName.orEmpty())) {
                    ADN_OTHER_YLH_BIDDING
                } else {
                    ADN_THIRD_PARTY
                }
                ad.sendLossNotification(lossPrice, adnId)
                SdkLog.d(
                    "优量汇激励视频竞价回传-负-winPrice:$lossPrice,adnId:$adnId,selfPrice:${item.price}"
                )
            }
        }
    }

    private fun Double?.toYlhBidPrice(): Int {
        if (this == null || this <= 0.0) return 0
        return roundToInt()
    }
}
