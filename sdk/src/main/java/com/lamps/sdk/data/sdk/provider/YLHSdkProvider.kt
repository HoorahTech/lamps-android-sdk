package com.lamps.sdk.data.sdk.provider

import android.app.Activity
import android.app.Application
import com.lamps.sdk.data.init.RewardSlotResponse
import com.lamps.sdk.data.sdk.channel.SdkInitCallback
import com.lamps.sdk.data.sdk.channel.YLHSdkManager
import com.lamps.sdk.data.sdk.reward.RewardAdErrorCode
import com.lamps.sdk.data.sdk.channel.RewardAdSdkLoadCallback
import com.lamps.sdk.data.sdk.channel.RewardVideoAd
import com.lamps.sdk.reward.RewardAdShowCallback
import com.lamps.sdk.data.sdk.channel.YLHRewardVideoAd
import java.util.Locale

internal class YLHSdkProvider : ISdkProvider {
    override val name: String = "优量汇 SDK 初始化"

    override fun supports(slot: RewardSlotResponse): Boolean {
        val channelName = slot.channelName.trim().lowercase(Locale.US)
        return channelName == "优量汇"
    }

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
}
