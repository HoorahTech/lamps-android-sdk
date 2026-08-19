package com.lamps.sdk.data.sdk.channel

import android.os.Bundle
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTRewardVideoAd as PangleRewardVideoAd

internal open class TTRewardVideoAdListenerAdapter : TTAdNative.RewardVideoAdListener {
    override fun onError(code: Int, message: String?) = Unit

    override fun onRewardVideoAdLoad(ad: PangleRewardVideoAd?) = Unit

    @Deprecated("Deprecated in Java")
    override fun onRewardVideoCached() = Unit

    override fun onRewardVideoCached(ad: PangleRewardVideoAd?) = Unit
}

internal open class TTRewardAdInteractionListenerAdapter :
    PangleRewardVideoAd.RewardAdInteractionListener {
    override fun onAdShow() = Unit

    override fun onAdVideoBarClick() = Unit

    override fun onAdClose() = Unit

    override fun onVideoComplete() = Unit

    override fun onVideoError() = Unit

    @Deprecated("Deprecated in Java")
    override fun onRewardVerify(
        rewardVerify: Boolean,
        rewardAmount: Int,
        rewardName: String?,
        errorCode: Int,
        errorMessage: String?
    ) = Unit

    override fun onRewardArrived(
        isRewardValid: Boolean,
        rewardType: Int,
        extraInfo: Bundle?
    ) = Unit

    override fun onSkippedVideo() = Unit
}
