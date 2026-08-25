package com.lamps.sdk.core

/** OAID provider supplied by the SDK host. */
fun interface OaidProvider {
    fun getOaid(): String?
}
