package com.lamps.sdk.core

/**
 * Receives reward-video monitor events the SDK submits.
 * [event] is `RM`, `WM`, `PM`, `CM`, or `DM`.
 * [payload] is the plaintext JSON of the fields written into the monitor URL.
 */
fun interface RewardMonitorCallback {
    fun onRewardMonitor(event: String, payload: String)
}
