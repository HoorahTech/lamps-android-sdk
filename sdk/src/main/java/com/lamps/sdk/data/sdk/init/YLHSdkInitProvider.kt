package com.lamps.sdk.data.sdk.init

import android.app.Application
import com.lamps.sdk.data.init.RewardSlotResponse
import com.lamps.sdk.data.sdk.channel.ThirdSdkInitCallback
import com.lamps.sdk.data.sdk.channel.YLHSdkManager
import java.util.Locale

internal class YLHSdkInitProvider : ISdkInitProvider {
    override val name: String = "优量汇 SDK 初始化"

    override fun supports(slot: RewardSlotResponse): Boolean {
        val channelName = slot.channelName.trim().lowercase(Locale.US)
        val channelId = slot.channelId.trim()
        return channelName == "ylh" ||
            channelName == "gdt" ||
            channelName == "优量汇" ||
            channelName == "优良汇" ||
            channelId == CHANNEL_ID ||
            channelId == BID_CHANNEL_ID
    }

    override fun initSdk(
        application: Application,
        slot: RewardSlotResponse,
        callback: ThirdSdkInitCallback
    ) {
        YLHSdkManager.initSdk(application, slot.appId, callback)
    }

    private companion object {
        const val CHANNEL_ID = "349"
        const val BID_CHANNEL_ID = "348"
    }
}
