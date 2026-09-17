package com.lamps.sdk.core

import com.lamps.sdk.config.NightMode

/** Night mode provider supplied by the SDK host. Invoked each time game center opens. */
fun interface NightModeProvider {
    fun getNightMode(): NightMode
}
