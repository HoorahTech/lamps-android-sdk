package com.lamps.sdk.data.sdk.init

import android.app.Application
import com.lamps.sdk.data.init.RewardSlotResponse
import com.lamps.sdk.data.sdk.channel.ThirdSdkInitCallback

internal interface ISdkInitProvider {
    val name: String

    fun supports(slot: RewardSlotResponse): Boolean

    fun initSdk(
        application: Application,
        slot: RewardSlotResponse,
        callback: ThirdSdkInitCallback
    )
}
