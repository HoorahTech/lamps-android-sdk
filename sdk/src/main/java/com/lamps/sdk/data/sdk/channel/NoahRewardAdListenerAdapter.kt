package com.lamps.sdk.data.sdk.channel

import com.noah.api.AdError
import com.noah.api.RewardedVideoAd

internal open class NoahRewardAdListenerAdapter : RewardedVideoAd.AdListener {
    override fun onAdError(error: AdError?) = Unit

    override fun onAdLoaded(ad: RewardedVideoAd?) = Unit

    override fun onAdShown(ad: RewardedVideoAd?) = Unit

    override fun onAdClicked(ad: RewardedVideoAd?) = Unit

    override fun onAdClosed(ad: RewardedVideoAd?) = Unit

    override fun onVideoStart(ad: RewardedVideoAd?) = Unit

    override fun onVideoEnd(ad: RewardedVideoAd?) = Unit

    override fun onRewarded(ad: RewardedVideoAd?) = Unit
}
