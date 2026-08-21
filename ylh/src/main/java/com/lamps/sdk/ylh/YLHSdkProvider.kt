package com.lamps.sdk.ylh

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

internal class YLHSdkProvider : ISdkProvider {
    override val name: String = SdkChannel.YLH.channelName

    override fun supports(slot: RewardSlotResponse): Boolean {
        return SdkChannel.YLH.matches(slot.channelName)
    }

    override fun shouldInitInternally(config: LampsConfig): Boolean = config.initYlhSdk

    override fun isInitialized(): Boolean = YLHSdkManager.isInitialized()

    override fun initSdk(
        application: Application,
        slot: RewardSlotResponse,
        callback: SdkInitCallback
    ) {
        YLHSdkManager.initSdk(application, slot.appId, callback)
    }

    override fun loadReward(
        activity: Activity,
        slot: RewardSlotResponse,
        callback: RewardAdSdkLoadCallback
    ) {
        YLHSdkManager.loadReward(activity, slot.slotId, callback)
    }

    override fun showReward(
        activity: Activity,
        ad: RewardVideoAd,
        callback: RewardAdShowCallback
    ) {
        val rewardAd = ad as? YLHRewardVideoAd
        if (rewardAd == null) {
            callback.onAdShowFailed(
                RewardAdErrorCode.PROVIDER_ERROR,
                "YLH reward ad data is invalid"
            )
            return
        }
        YLHSdkManager.showReward(activity, rewardAd, callback)
    }

    override fun reportRewardBidding(allItems: List<LampsRewardAd>, winner: LampsRewardAd?) {
        YLHSdkRewardBiddingReporter.reportRewardBidding(allItems, winner)
    }
}
