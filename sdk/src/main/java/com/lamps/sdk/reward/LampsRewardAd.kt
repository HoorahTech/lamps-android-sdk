package com.lamps.sdk.reward

import android.app.Activity
import com.lamps.sdk.core.SdkRuntime
import com.lamps.sdk.data.init.RewardSlotResponse
import com.lamps.sdk.data.sdk.channel.RewardVideoAd
import com.lamps.sdk.data.sdk.provider.ISdkProvider
import com.lamps.sdk.data.sdk.reward.SdkRewardState

/**
 * 激励视频广告句柄。由 [com.lamps.sdk.LampsSdk.loadReward] 加载成功后回调给宿主。
 *
 * 对外只暴露展示所需信息与 [show]；平台广告对象和状态机不对宿主开放。
 */
class LampsRewardAd internal constructor(
    internal val provider: ISdkProvider,
    internal val slot: RewardSlotResponse,
    internal val requestId: String,
    internal val forwardSource: String
) {
    @Volatile
    internal var state: SdkRewardState = SdkRewardState.PENDING
        private set

    @Volatile
    internal var adData: RewardVideoAd? = null
        private set

    /**
     * 当前广告用于竞价的价格，单位：分。
     * 优先取平台 SDK 实时 eCPM，拿不到或无效时回落到接口下发的 `price`（同样按分）。
     */
    val price: Double
        get() {
            val sdkPrice = adData?.getPrice() ?: 0.0
            if (sdkPrice > 0.0) return sdkPrice
            return slot.price.takeIf { it > 0.0 } ?: 0.0
        }

    /** 服务端下发的广告位 ID。 */
    val slotId: String
        get() = slot.slotId

    /** 渠道名称，例如穿山甲、优量汇、汇川。 */
    val channelName: String
        get() = slot.channelName

    /** 广告是否仍可展示。已展示、已关闭或加载失败时为 `false`。 */
    val isValid: Boolean
        get() = adData != null &&
            (state == SdkRewardState.LOAD_SUCCESS || state == SdkRewardState.SELECTED)

    @Volatile
    internal var errorCode: Int? = null
        private set

    @Volatile
    internal var errorMessage: String? = null
        private set

    @Volatile
    internal var loadStartTimeMillis: Long? = null
        private set

    @Volatile
    internal var loadEndTimeMillis: Long? = null
        private set

    @Volatile
    internal var showStartTimeMillis: Long? = null
        private set

    @Volatile
    internal var showEndTimeMillis: Long? = null
        private set

    @Volatile
    internal var rewarded: Boolean = false
        private set

    internal val loadDurationMillis: Long?
        get() = duration(loadStartTimeMillis, loadEndTimeMillis)

    internal val showDurationMillis: Long?
        get() = duration(showStartTimeMillis, showEndTimeMillis)

    /**
     * 展示当前激励视频。同一实例只能展示一次。
     */
    fun show(activity: Activity, callback: RewardAdShowCallback) {
        SdkRuntime.showReward(activity, this, callback)
    }

    @Synchronized
    internal fun markLoading(): Boolean {
        if (state != SdkRewardState.PENDING) return false
        state = SdkRewardState.LOADING
        loadStartTimeMillis = System.currentTimeMillis()
        return true
    }

    @Synchronized
    internal fun markLoadSuccess(ad: RewardVideoAd): Boolean {
        if (state != SdkRewardState.LOADING) return false
        this.adData = ad
        state = SdkRewardState.LOAD_SUCCESS
        loadEndTimeMillis = System.currentTimeMillis()
        return true
    }

    @Synchronized
    internal fun markLoadFailed(
        code: Int,
        message: String?,
        ad: RewardVideoAd? = null
    ): Boolean {
        if (state != SdkRewardState.LOADING) return false
        if (ad != null) adData = ad
        state = SdkRewardState.LOAD_FAILED
        errorCode = code
        errorMessage = message
        loadEndTimeMillis = System.currentTimeMillis()
        return true
    }

    @Synchronized
    internal fun markBidFailed(code: Int, message: String?): Boolean {
        if (state != SdkRewardState.LOAD_SUCCESS) return false
        state = SdkRewardState.BID_FAILED
        errorCode = code
        errorMessage = message
        return true
    }

    @Synchronized
    internal fun markSelected(): Boolean {
        if (state != SdkRewardState.LOAD_SUCCESS) return false
        state = SdkRewardState.SELECTED
        return true
    }

    @Synchronized
    internal fun markShowing(): Boolean {
        if (state != SdkRewardState.SELECTED) return false
        state = SdkRewardState.SHOWING
        showStartTimeMillis = System.currentTimeMillis()
        return true
    }

    @Synchronized
    internal fun markShown(): Boolean {
        if (state != SdkRewardState.SHOWING) return false
        state = SdkRewardState.SHOWN
        return true
    }

    @Synchronized
    internal fun markRewarded() {
        rewarded = true
    }

    @Synchronized
    internal fun markClosed() {
        if (state != SdkRewardState.SHOWN && state != SdkRewardState.SHOWING) return
        state = SdkRewardState.CLOSED
        showEndTimeMillis = System.currentTimeMillis()
    }

    @Synchronized
    internal fun markShowFailed(code: Int, message: String?) {
        if (state != SdkRewardState.SHOWING && state != SdkRewardState.SHOWN) return
        state = SdkRewardState.SHOW_FAILED
        errorCode = code
        errorMessage = message
        showEndTimeMillis = System.currentTimeMillis()
    }

    private fun duration(start: Long?, end: Long?): Long? {
        if (start == null || end == null) return null
        return (end - start).coerceAtLeast(0L)
    }
}
