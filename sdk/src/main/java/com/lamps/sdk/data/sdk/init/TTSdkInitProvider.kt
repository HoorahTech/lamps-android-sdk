package com.lamps.sdk.data.sdk.init

import android.app.Application
import com.lamps.sdk.data.init.RewardSlotResponse
import com.lamps.sdk.data.sdk.channel.TTSdkManager
import com.lamps.sdk.data.sdk.channel.ThirdSdkInitCallback
import java.util.Locale

internal class TTSdkInitProvider : ISdkInitProvider {
    override val name: String = "穿山甲 SDK 初始化"

    override fun supports(slot: RewardSlotResponse): Boolean {
        val channelName = slot.channelName.trim().lowercase(Locale.US)
        val channelId = slot.channelId.trim()
        return channelName == "pangle" ||
            channelName == "csj" ||
            channelName == "穿山甲" ||
            channelId == CHANNEL_ID ||
            channelId == BID_CHANNEL_ID
    }

    override fun initSdk(
        application: Application,
        slot: RewardSlotResponse,
        callback: ThirdSdkInitCallback
    ) {
        TTSdkManager.initSdk(application, slot.appId, callback)
    }

    private companion object {
        const val CHANNEL_ID = "2"
        const val BID_CHANNEL_ID = "327"
    }
}
