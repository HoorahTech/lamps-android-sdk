package com.lamps.sdk.data.sdk.channel

import android.app.Activity
import com.lamps.sdk.reward.RewardAdShowCallback

internal abstract class RewardVideoAd {
    abstract fun getPrice(): Double

    abstract fun loadAD(callback: RewardAdSdkLoadCallback)

    abstract fun showAD(activity: Activity, callback: RewardAdShowCallback)
}

internal interface RewardAdSdkLoadCallback {
    fun onLoadSuccess(ad: RewardVideoAd)

    fun onLoadFailed(code: Int, message: String?)
}
