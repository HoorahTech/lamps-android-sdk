package com.lamps.sdk.core

interface CoreInitCallback {
    fun success()
    fun fail(code: Int, message: String?)
}
