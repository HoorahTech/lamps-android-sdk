package com.lamps.sdk.noah

import android.app.Activity
import android.app.Application
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.data.init.RewardSlotResponse
import com.lamps.sdk.data.sdk.channel.RewardAdSdkLoadCallback
import com.lamps.sdk.data.sdk.channel.RewardVideoAd
import com.lamps.sdk.data.sdk.channel.SdkChannel
import com.lamps.sdk.data.sdk.channel.SdkInitCallback
import com.lamps.sdk.data.sdk.provider.ISdkProvider
import com.lamps.sdk.data.sdk.reward.RewardAdErrorCode
import com.lamps.sdk.reward.LampsRewardAd
import com.lamps.sdk.reward.RewardAdShowCallback

internal class NoahSdkProvider : ISdkProvider {
    override val name: String = SdkChannel.NOAH.channelName

    override fun supports(slot: RewardSlotResponse): Boolean {
        return SdkChannel.NOAH.matches(slot.channelName)
    }

    override fun shouldInitInternally(config: LampsConfig): Boolean = config.initNoahSdk

    override fun isInitialized(): Boolean = NoahSdkManager.isInitialized()

    override fun initSdk(
        application: Application,
        slot: RewardSlotResponse,
        callback: SdkInitCallback
    ) {
        NoahSdkManager.initSdk(
            application,
            slot.appId,
            callback
        )
    }

    override fun loadReward(
        activity: Activity,
        slot: RewardSlotResponse,
        callback: RewardAdSdkLoadCallback
    ) {
        NoahSdkManager.loadReward(activity, slot.slotId, callback)
    }

    override fun showReward(
        activity: Activity,
        ad: RewardVideoAd,
        callback: RewardAdShowCallback
    ) {
        val rewardAd = ad as? NoahRewardVideoAd
        if (rewardAd == null) {
            callback.onAdShowFailed(
                RewardAdErrorCode.PROVIDER_ERROR,
                "Noah reward ad data is invalid"
            )
            return
        }
        NoahSdkManager.showReward(activity, rewardAd, callback)
    }

    override fun reportRewardBidding(allItems: List<LampsRewardAd>, winner: LampsRewardAd?) {
        NoahSdkRewardBiddingReporter.reportRewardBidding(allItems, winner)
    }
}
