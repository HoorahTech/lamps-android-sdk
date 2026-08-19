package com.lamps.sdk.reward

import com.lamps.sdk.utils.SdkLog
import java.util.concurrent.atomic.AtomicBoolean

internal object BuiltInRewardAdProviders {
    private val installed = AtomicBoolean(false)

    fun installAvailable() {
        if (!installed.compareAndSet(false, true)) return
        PROVIDERS.forEach { descriptor ->
            runCatching {
                Class.forName(descriptor.sdkMarker, false, javaClass.classLoader)
                val providerClass = Class.forName(
                    descriptor.providerClass,
                    true,
                    javaClass.classLoader
                )
                val constructor = providerClass.getDeclaredConstructor()
                constructor.isAccessible = true
                RewardAdProviderRegistry.register(
                    constructor.newInstance() as RewardAdProvider
                )
            }.onFailure {
                SdkLog.d("reward provider unavailable: ${descriptor.name}")
            }
        }
    }

    private data class ProviderDescriptor(
        val name: String,
        val sdkMarker: String,
        val providerClass: String
    )

    private val PROVIDERS = arrayOf(
        ProviderDescriptor(
            name = "Pangle",
            sdkMarker = "com.bytedance.sdk.openadsdk.TTAdSdk",
            providerClass = "com.lamps.sdk.reward.provider.PangleRewardAdProvider"
        ),
        ProviderDescriptor(
            name = "YLH",
            sdkMarker = "com.qq.e.comm.managers.GDTAdSdk",
            providerClass = "com.lamps.sdk.reward.provider.YlhRewardAdProvider"
        ),
        ProviderDescriptor(
            name = "Noah",
            sdkMarker = "com.noah.api.NoahSdk",
            providerClass = "com.lamps.sdk.reward.provider.NoahRewardAdProvider"
        )
    )
}
