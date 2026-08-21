package com.lamps.sdk.data.sdk.channel

import android.app.Activity
import com.lamps.sdk.reward.RewardAdShowCallback

abstract class RewardVideoAd {
    /**
     * 当前广告用于竞价的 eCPM，单位：分。
     * 平台未返回、无权限（如优量汇 -1）或解析失败时为 `0.0`。
     */
    abstract fun getPrice(): Double

    abstract fun loadAD(callback: RewardAdSdkLoadCallback)

    abstract fun showAD(activity: Activity, callback: RewardAdShowCallback)
}

interface RewardAdSdkLoadCallback {
    fun onLoadSuccess(ad: RewardVideoAd)

    fun onLoadFailed(code: Int, message: String?, ad: RewardVideoAd? = null)
}
