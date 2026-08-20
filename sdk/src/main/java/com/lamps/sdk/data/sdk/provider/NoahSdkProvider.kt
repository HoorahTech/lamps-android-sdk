package com.lamps.sdk.data.sdk.provider

import android.app.Activity
import android.app.Application
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.data.init.RewardSlotResponse
import com.lamps.sdk.data.sdk.channel.NoahRewardVideoAd
import com.lamps.sdk.data.sdk.channel.NoahSdkManager
import com.lamps.sdk.data.sdk.channel.SdkChannel
import com.lamps.sdk.data.sdk.channel.SdkInitCallback
import com.lamps.sdk.data.sdk.reward.RewardAdErrorCode
import com.lamps.sdk.data.sdk.channel.RewardAdSdkLoadCallback
import com.lamps.sdk.data.sdk.channel.RewardVideoAd
import com.lamps.sdk.reward.RewardAdShowCallback

internal class NoahSdkProvider : ISdkProvider {
    override val name: String = SdkChannel.NOAH.channelName

    override fun supports(slot: RewardSlotResponse): Boolean {
        return SdkChannel.NOAH.matches(slot.channelName)
    }

    override fun shouldInitInternally(config: LampsConfig): Boolean = config.initNoahSdk

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
        NoahSdkManager.loadReward(activity, slot.slotId,  callback)
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
}
