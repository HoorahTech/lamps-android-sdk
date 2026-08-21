package com.lamps.sdk.pangle

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
import com.lamps.sdk.reward.RewardAdShowCallback

internal class TTSdkProvider : ISdkProvider {
    override val name: String = SdkChannel.PANGLE.channelName

    override fun supports(slot: RewardSlotResponse): Boolean {
        return SdkChannel.PANGLE.matches(slot.channelName)
    }

    override fun shouldInitInternally(config: LampsConfig): Boolean = config.initPangleSdk

    override fun isInitialized(): Boolean = TTSdkManager.isInitialized()

    override fun initSdk(
        application: Application,
        slot: RewardSlotResponse,
        callback: SdkInitCallback
    ) {
        TTSdkManager.initSdk(application, slot.appId, callback)
    }

    override fun loadReward(
        activity: Activity,
        slot: RewardSlotResponse,
        callback: RewardAdSdkLoadCallback
    ) {
        TTSdkManager.loadReward(activity, slot.slotId, callback)
    }

    override fun showReward(
        activity: Activity,
        ad: RewardVideoAd,
        callback: RewardAdShowCallback
    ) {
        val rewardAd = ad as? TTRewardVideoAd
        if (rewardAd == null) {
            callback.onAdShowFailed(
                RewardAdErrorCode.PROVIDER_ERROR,
                "Pangle reward ad data is invalid"
            )
            return
        }
        TTSdkManager.showReward(activity, rewardAd, callback)
    }
}
