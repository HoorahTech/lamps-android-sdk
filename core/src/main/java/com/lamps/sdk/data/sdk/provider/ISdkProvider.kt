package com.lamps.sdk.data.sdk.provider

import android.app.Activity
import android.app.Application
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.data.init.RewardSlotResponse
import com.lamps.sdk.data.sdk.channel.RewardAdSdkLoadCallback
import com.lamps.sdk.data.sdk.channel.RewardVideoAd
import com.lamps.sdk.data.sdk.channel.SdkInitCallback
import com.lamps.sdk.reward.LampsRewardAd
import com.lamps.sdk.reward.RewardAdShowCallback

interface ISdkProvider {
    val name: String

    fun supports(slot: RewardSlotResponse): Boolean

    fun shouldInitInternally(config: LampsConfig): Boolean

    fun isInitialized(): Boolean = false

    fun initSdk(
        application: Application,
        slot: RewardSlotResponse,
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
