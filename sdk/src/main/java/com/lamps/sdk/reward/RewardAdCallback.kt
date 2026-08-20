package com.lamps.sdk.reward

interface RewardAdLoadCallback {
    fun onAdLoadSuccess(ad: LampsRewardAd)

    fun onAdLoadFailed(code: Int, message: String?)
}

interface RewardAdShowCallback {
    fun onAdShown() = Unit
    fun onAdRewarded() = Unit
    fun onAdClosed() = Unit
    fun onAdClicked() = Unit
    fun onAdShowFailed(code: Int, message: String?)
}
