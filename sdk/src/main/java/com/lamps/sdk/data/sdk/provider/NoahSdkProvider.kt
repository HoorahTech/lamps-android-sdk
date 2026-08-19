package com.lamps.sdk.data.sdk.provider

import android.app.Activity
import android.app.Application
import com.lamps.sdk.data.init.RewardSlotResponse
import com.lamps.sdk.data.sdk.channel.NoahRewardVideoAd
import com.lamps.sdk.data.sdk.channel.NoahSdkManager
import com.lamps.sdk.data.sdk.channel.SdkInitCallback
import com.lamps.sdk.data.sdk.reward.RewardAdErrorCode
import com.lamps.sdk.data.sdk.channel.RewardAdSdkLoadCallback
import com.lamps.sdk.data.sdk.channel.RewardVideoAd
import com.lamps.sdk.reward.RewardAdShowCallback
import java.util.Locale

internal class NoahSdkProvider : ISdkProvider {
    override val name: String = "汇川 SDK 初始化"

    override fun supports(slot: RewardSlotResponse): Boolean {
        val channelName = slot.channelName.trim().lowercase(Locale.US)
        return channelName == "汇川"
    }

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
