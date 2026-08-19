package com.lamps.sdk.data.sdk.provider

import android.app.Application
import android.app.Activity
import com.lamps.sdk.data.init.RewardSlotResponse
import com.lamps.sdk.data.sdk.channel.SdkInitCallback
import com.lamps.sdk.data.sdk.channel.RewardAdSdkLoadCallback
import com.lamps.sdk.data.sdk.channel.RewardVideoAd
import com.lamps.sdk.reward.RewardAdShowCallback

internal interface ISdkProvider {
    val name: String

    fun supports(slot: RewardSlotResponse): Boolean

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
}
