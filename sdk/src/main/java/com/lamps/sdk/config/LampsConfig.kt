package com.lamps.sdk.config

import android.content.Context
import com.lamps.sdk.core.OaidProvider
import com.lamps.sdk.data.init.AppInitDataSnapshot
import com.lamps.sdk.data.init.AppInitResponse
import com.lamps.sdk.utils.SdkLog

/**
 * SDK 初始化配置。
 *
 * [appId] 必填；OAID 通过 [Builder.setOaidProvider] 必传，在 start 时读取。
 * [Builder.setDebug] 只控制日志，不影响接口地址。
 */
class LampsConfig private constructor(
    val appId: String,
    val oaidProvider: OaidProvider?,
    val debug: Boolean,
    val customData: Map<String, String>,
    private val debugSpecified: Boolean,
    private val customDataSpecified: Boolean
) {

    @Volatile
    internal var applicationContext: Context? = null

    @Volatile
    internal var appInitData: AppInitResponse? = null

    fun resolveOaid(): String {
        return oaidProvider?.getOaid()?.trim().orEmpty()
    }

    fun mergedWith(other: LampsConfig): LampsConfig {
        return LampsConfig(
            appId = if (other.appId.isNotEmpty()) other.appId else appId,
            oaidProvider = other.oaidProvider ?: oaidProvider,
            debug = if (other.debugSpecified) other.debug else debug,
            customData = if (other.customDataSpecified) other.customData else customData,
            debugSpecified = true,
            customDataSpecified = true
        ).also { merged ->
            merged.applicationContext = applicationContext
            merged.appInitData = appInitData
        }
    }

    class Builder {
        private var appId: String = ""
        private var oaidProvider: OaidProvider? = null
        private var debug: Boolean = false
        private var debugSpecified: Boolean = false
        private var customData: Map<String, String> = emptyMap()
        private var customDataSpecified: Boolean = false

        fun appId(appId: String) = apply { this.appId = appId }

        fun setOaidProvider(provider: OaidProvider) = apply { this.oaidProvider = provider }

        fun setDebug(debug: Boolean) = apply {
            this.debug = debug
            this.debugSpecified = true
        }

        fun setCustomData(data: Map<String, String>) = apply {
            this.customData = HashMap(data)
            this.customDataSpecified = true
        }

        fun build(): LampsConfig {
            return LampsConfig(
                appId = appId.trim(),
                oaidProvider = oaidProvider,
                debug = debug,
                customData = HashMap(customData),
                debugSpecified = debugSpecified,
                customDataSpecified = customDataSpecified
            )
        }
    }

    companion object {
        @Volatile
        internal var current: LampsConfig? = null
            private set

        internal fun init(context: Context, config: LampsConfig) {
            config.applicationContext = context.applicationContext
            current = config
        }

        internal fun replace(config: LampsConfig) {
            val previous = current
            if (previous != null) {
                config.applicationContext = config.applicationContext ?: previous.applicationContext
                config.appInitData = config.appInitData ?: previous.appInitData
            }
            current = config
        }
    }
}
