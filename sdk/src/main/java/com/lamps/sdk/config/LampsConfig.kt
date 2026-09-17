package com.lamps.sdk.config

import com.lamps.sdk.core.CoreNightModeProvider
import com.lamps.sdk.core.CoreOaidProvider
import com.lamps.sdk.core.NightModeProvider
import com.lamps.sdk.core.OaidProvider

/**
 * SDK 初始化配置。
 *
 * [appId] 必填；OAID 通过 [Builder.setOaidProvider] 可选，在 start 时读取，空值不阻断启动。
 * 日夜间通过 [Builder.setNightModeProvider] 可选，每次打开游戏中心时读取。
 * [Builder.setDebug] 只控制日志，不影响接口地址。
 */
class LampsConfig private constructor(
    val appId: String,
    val oaidProvider: OaidProvider?,
    val nightModeProvider: NightModeProvider?,
    val debug: Boolean,
    val customData: Map<String, String>,
    val initPangleSdk: Boolean,
    val initYlhSdk: Boolean,
    val initNoahSdk: Boolean
) {
    internal fun toSdkConfig(): SdkConfig = SdkConfig(
        appId = appId,
        oaidProvider = oaidProvider?.let { provider ->
            CoreOaidProvider { provider.getOaid() }
        },
        nightModeProvider = nightModeProvider?.let { provider ->
            CoreNightModeProvider { provider.getNightMode() == NightMode.NIGHT }
        },
        debug = debug,
        customData = HashMap(customData),
        initPangleSdk = initPangleSdk,
        initYlhSdk = initYlhSdk,
        initNoahSdk = initNoahSdk
    )

    class Builder {
        private var appId: String = ""
        private var oaidProvider: OaidProvider? = null
        private var nightModeProvider: NightModeProvider? = null
        private var debug: Boolean = false
        private var customData: Map<String, String> = emptyMap()
        private var needInitPangleSdk: Boolean = true
        private var needInitYlhSdk: Boolean = true
        private var needInitNoahSdk: Boolean = true

        fun appId(appId: String) = apply { this.appId = appId }

        fun setOaidProvider(provider: OaidProvider) = apply { this.oaidProvider = provider }

        fun setNightModeProvider(provider: NightModeProvider) = apply {
            this.nightModeProvider = provider
        }

        fun setDebug(debug: Boolean) = apply { this.debug = debug }

        fun setCustomData(data: Map<String, String>) = apply {
            this.customData = HashMap(data)
        }

        fun setLampsInitPangleSdk(enable: Boolean) = apply { needInitPangleSdk = enable }

        fun setLampsInitYlhSdk(enable: Boolean) = apply { needInitYlhSdk = enable }

        fun setLampsInitNoahSdk(enable: Boolean) = apply { needInitNoahSdk = enable }

        fun build(): LampsConfig = LampsConfig(
            appId = appId.trim(),
            oaidProvider = oaidProvider,
            nightModeProvider = nightModeProvider,
            debug = debug,
            customData = HashMap(customData),
            initPangleSdk = needInitPangleSdk,
            initYlhSdk = needInitYlhSdk,
            initNoahSdk = needInitNoahSdk
        )
    }
}
