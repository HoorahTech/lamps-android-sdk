package com.lamps.sdk.reward

import android.app.Activity
import android.app.Application

data class RewardAdSlot(
    val slotId: String,
    val type: String,
    val channelName: String,
    val channelId: String,
    val appId: String
)

data class RewardAdEnvironment(
    val application: Application,
    val oaid: String,
    val debug: Boolean
)

interface RewardAdProvider {
    /**
     * Returns true when this provider can consume the server-issued slot.
     */
    fun supports(slot: RewardAdSlot): Boolean

    /**
     * Loads and immediately shows a rewarded video.
     */
    fun loadAndShow(
        activity: Activity,
        slot: RewardAdSlot,
        environment: RewardAdEnvironment,
        callback: RewardAdCallback
    )
}

interface RewardAdCallback {
    fun onLoaded() = Unit

    fun onShown() = Unit

    fun onRewarded() = Unit

    fun onClosed() = Unit

    fun onError(code: Int, message: String?)
}
