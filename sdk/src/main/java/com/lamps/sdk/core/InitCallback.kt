package com.lamps.sdk.core

interface InitCallback {
    fun success()
    fun fail(code: Int, message: String?)
}
