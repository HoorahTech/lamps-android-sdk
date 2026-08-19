package com.lamps.sdk.data.sdk.init

import android.app.Application
import com.lamps.sdk.data.init.RewardSlotResponse
import com.lamps.sdk.data.sdk.channel.NoahSdkManager
import com.lamps.sdk.data.sdk.channel.ThirdSdkInitCallback
import java.util.Locale

internal class NoahSdkInitProvider : ISdkInitProvider {
    override val name: String = "汇川 SDK 初始化"

    override fun supports(slot: RewardSlotResponse): Boolean {
        val channelName = slot.channelName.trim().lowercase(Locale.US)
        return channelName == "noah" ||
            channelName == "huichuan" ||
            channelName == "汇川" ||
            slot.channelId.trim() == CHANNEL_ID
    }

    override fun initSdk(
        application: Application,
        slot: RewardSlotResponse,
        callback: ThirdSdkInitCallback
    ) {
        NoahSdkManager.initSdk(
            application,
            slot.appId,
            callback
        )
    }

    private companion object {
        const val CHANNEL_ID = "417"
    }
}
