package com.lamps.sdk.data.sdk.channel

enum class SdkChannel(val id: String, val displayName: String) {
    CSJ("CSJ", "穿山甲"),
    GDT("GDT", "优量汇"),
    NOAH("NOAH", "汇川");

    fun matches(raw: String): Boolean {
        val trimmed = raw.trim()
        return trimmed.equals(id, ignoreCase = true) || trimmed.equals(displayName, ignoreCase = true)
    }
}
