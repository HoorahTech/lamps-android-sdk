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

    /** [requestId] 为本次 loadReward 的请求 ID，同一轮竞价的所有广告位共用，与上报埋点一致。 */
    fun loadReward(
        activity: Activity,
        slot: RewardSlotResponse,
        requestId: String,
        callback: RewardAdSdkLoadCallback
    )

    fun showReward(
        activity: Activity,
        ad: RewardVideoAd,
        callback: RewardAdShowCallback
    )

    fun reportRewardBidding(allItems: List<LampsRewardAd>, winner: LampsRewardAd?) = Unit
}
