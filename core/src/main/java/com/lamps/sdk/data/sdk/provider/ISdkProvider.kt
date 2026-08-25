package com.lamps.sdk.data.sdk.provider

import android.app.Activity
import android.app.Application
import com.lamps.sdk.config.SdkConfig
import com.lamps.sdk.data.init.ChannelInfoResponse
import com.lamps.sdk.data.init.RewardSlotResponse
import com.lamps.sdk.data.sdk.channel.RewardAdSdkLoadCallback
import com.lamps.sdk.data.sdk.channel.RewardVideoAd
import com.lamps.sdk.data.sdk.channel.SdkInitCallback
import com.lamps.sdk.reward.LampsRewardAd
import com.lamps.sdk.reward.RewardAdShowCallback

interface ISdkProvider {
    val name: String

    fun supports(channel: ChannelInfoResponse): Boolean

    fun shouldInitInternally(config: SdkConfig): Boolean

    fun isInitialized(): Boolean = false

    fun initSdk(
        application: Application,
        channel: ChannelInfoResponse,
        callback: SdkInitCallback
    )

    fun loadReward(
        activity: Activity,
        slot: RewardSlotResponse,
        callback: RewardAdSdkLoadCallback
    )

    fun showReward(
        activity: Activity,
        ad: RewardVideoAd,
        callback: RewardAdShowCallback
    )

    fun reportRewardBidding(allItems: List<LampsRewardAd>, winner: LampsRewardAd?) = Unit
}
