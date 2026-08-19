package com.lamps.sdk.data.sdk.channel

interface ThirdSdkInitCallback {
    fun success()

    fun fail(code: Int, message: String?)
}
