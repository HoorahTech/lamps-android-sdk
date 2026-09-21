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

    /**
     * 释放渠道广告对象持有的加载/展示回调与待执行超时任务。
     *
     * 广告流程终结（关闭、展示失败、竞价失败）或宿主页面销毁时调用。渠道回调链路会一直指回调用方，
     * 不断开的话已销毁的 WebView 和 Activity 会被渠道 SDK 一直持有。调用后本实例不应再 show。
     */
    open fun release() = Unit
}

interface RewardAdSdkLoadCallback {
    fun onLoadSuccess(ad: RewardVideoAd)

    fun onLoadFailed(code: Int, message: String?, ad: RewardVideoAd? = null)
}
