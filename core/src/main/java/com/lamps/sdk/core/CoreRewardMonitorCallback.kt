package com.lamps.sdk.core

fun interface CoreRewardMonitorCallback {
    fun onRewardMonitor(event: String, payload: String)
}
