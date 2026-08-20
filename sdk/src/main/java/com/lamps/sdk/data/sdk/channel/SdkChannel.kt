package com.lamps.sdk.data.sdk.channel

internal enum class SdkChannel(val channelName: String) {
    PANGLE("穿山甲"),
    YLH("优量汇"),
    NOAH("汇川");

    fun matches(raw: String): Boolean {
        return raw.trim().equals(channelName, ignoreCase = true)
    }
}
