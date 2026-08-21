package com.lamps.sdk.core

/**
 * 由媒体提供 OAID。SDK 不内置 MSA 证书，只在 [com.lamps.sdk.LampsSdk.startAsync] 时读取。
 * 未设置或返回空时按空串继续启动，不视为失败。
 */
fun interface OaidProvider {
    fun getOaid(): String?
}
