package com.lamps.sdk.data.sdk.channel

enum class SdkChannel(val id: String, val name: String) {
    CSJ("CSJ", "穿山甲"),
    GDT("GDT", "优量汇"),
    HUICHUAN("HUICHUAN", "汇川");

    fun matches(raw: String): Boolean {
        val trimmed = raw.trim()
        return trimmed.equals(id, ignoreCase = true) || trimmed.equals(name, ignoreCase = true)
    }
}
