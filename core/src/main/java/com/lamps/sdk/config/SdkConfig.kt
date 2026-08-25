package com.lamps.sdk.config

import android.content.Context
import com.lamps.sdk.core.CoreOaidProvider
import com.lamps.sdk.data.init.AppInitResponse

/**
 * Core-side configuration snapshot. The public builder lives in the sdk module.
 */
class SdkConfig(
    val appId: String,
    val oaidProvider: CoreOaidProvider?,
    val debug: Boolean,
    val customData: Map<String, String>,
    val initPangleSdk: Boolean,
    val initYlhSdk: Boolean,
    val initNoahSdk: Boolean
) {
    @Volatile
    internal var applicationContext: Context? = null

    @Volatile
    internal var appInitData: AppInitResponse? = null

    fun resolveOaid(): String {
        return runCatching { oaidProvider?.getOaid()?.trim().orEmpty() }.getOrDefault("")
    }

    companion object {
        @Volatile
        private var instance: SdkConfig? = null

        @JvmStatic
        val current: SdkConfig?
            get() = instance

        internal fun init(context: Context, config: SdkConfig) {
            config.applicationContext = context.applicationContext
            instance = config
        }
    }
}
