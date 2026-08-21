package com.lamps.sdk.data.sdk.channel

interface SdkInitCallback {
    fun success()
    fun fail(code: Int, message: String?)
}

