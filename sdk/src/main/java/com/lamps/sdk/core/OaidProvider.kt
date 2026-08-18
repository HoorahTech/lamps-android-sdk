package com.lamps.sdk.core

/**
 * 由媒体提供 OAID。SDK 不内置 MSA 证书，只在 [com.lamps.sdk.LampsSdk.start] 时读取。
 */
fun interface OaidProvider {
    fun getOaid(): String?
}
