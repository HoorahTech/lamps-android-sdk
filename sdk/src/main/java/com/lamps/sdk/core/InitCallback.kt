package com.lamps.sdk.core

/** Initialization result callback exposed to SDK hosts. */
interface InitCallback {
    fun success()
    fun fail(code: Int, message: String?)
}
