package com.lamps.sdk.pangle

import android.app.Activity
import android.app.Application
import com.lamps.sdk.config.SdkConfig
import com.lamps.sdk.data.init.ChannelInfoResponse
import com.lamps.sdk.data.init.RewardSlotResponse
import com.lamps.sdk.data.sdk.channel.RewardAdSdkLoadCallback
import com.lamps.sdk.data.sdk.channel.RewardVideoAd
import com.lamps.sdk.data.sdk.channel.SdkChannel
import com.lamps.sdk.data.sdk.channel.SdkInitCallback
import com.lamps.sdk.data.sdk.provider.ISdkProvider
import com.lamps.sdk.data.sdk.reward.RewardAdErrorCode
import com.lamps.sdk.reward.RewardAdShowCallback

internal class TTSdkProvider : ISdkProvider {
    override val name: String = SdkChannel.CSJ.name

    override fun supports(channel: ChannelInfoResponse): Boolean {
        return SdkChannel.CSJ.matches(channel.channelName)
    }

    override fun shouldInitInternally(config: SdkConfig): Boolean = config.initPangleSdk

    override fun isInitialized(): Boolean = TTSdkManager.isInitialized()

    override fun initSdk(
        application: Application,
        channel: ChannelInfoResponse,
        callback: SdkInitCallback
    ) {
        TTSdkManager.initSdk(application, channel.channelAppId, callback)
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
