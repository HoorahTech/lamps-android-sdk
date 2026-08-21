package com.lamps.sdk.config

import android.content.Context
import com.lamps.sdk.core.OaidProvider
import com.lamps.sdk.data.init.AppInitResponse

/**
 * SDK 初始化配置。
 *
 * [appId] 必填；OAID 通过 [Builder.setOaidProvider] 可选，在 start 时读取，空值不阻断启动。
 * [Builder.setDebug] 只控制日志，不影响接口地址。
 * [Builder.setLampsInitPangleSdk]、[Builder.setLampsInitYlhSdk]、[Builder.setLampsInitNoahSdk]
 * 控制是否由 Lamps 内部初始化对应渠道；默认 true。传 false 时跳过内部初始化并视为该渠道已成功。
 */
class LampsConfig private constructor(
    val appId: String,
    val oaidProvider: OaidProvider?,
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

    class Builder {
        private var appId: String = ""
        private var oaidProvider: OaidProvider? = null
        private var debug: Boolean = false
        private var customData: Map<String, String> = emptyMap()
        private var needInitPangleSdk: Boolean = true
        private var needInitYlhSdk: Boolean = true
        private var needInitNoahSdk: Boolean = true

        fun appId(appId: String) = apply { this.appId = appId }

        fun setOaidProvider(provider: OaidProvider) = apply { this.oaidProvider = provider }

        fun setDebug(debug: Boolean) = apply { this.debug = debug }

        fun setCustomData(data: Map<String, String>) = apply {
            this.customData = HashMap(data)
        }

        /**
         * 是否由 Lamps 内部初始化穿山甲。默认 true。
         * 传 false 时跳过 TTAdSdk.init，该渠道视为初始化成功。
         */
        fun setLampsInitPangleSdk(enable: Boolean) = apply {
            this.needInitPangleSdk = enable
        }

        /**
         * 是否由 Lamps 内部初始化优量汇。默认 true。
         * 传 false 时跳过 GDTAdSdk.init，该渠道视为初始化成功。
         */
        fun setLampsInitYlhSdk(enable: Boolean) = apply {
            this.needInitYlhSdk = enable
        }

        /**
         * 是否由 Lamps 内部初始化汇川。默认 true。
         * 传 false 时跳过 NoahSdk.init，该渠道视为初始化成功。
         */
        fun setLampsInitNoahSdk(enable: Boolean) = apply {
            this.needInitNoahSdk = enable
        }

        fun build(): LampsConfig {
            return LampsConfig(
                appId = appId.trim(),
                oaidProvider = oaidProvider,
                debug = debug,
                customData = HashMap(customData),
                initPangleSdk = needInitPangleSdk,
                initYlhSdk = needInitYlhSdk,
                initNoahSdk = needInitNoahSdk
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
    }
}
