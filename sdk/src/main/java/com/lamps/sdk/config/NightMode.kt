package com.lamps.sdk.config

/** Host day/night mode. Exposed to H5 as `night`: [NIGHT]=1, [DAY]=0. */
enum class NightMode(val value: Int) {
    DAY(0),
    NIGHT(1),
}
