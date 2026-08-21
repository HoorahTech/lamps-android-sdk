package com.lamps.sdk.data.sdk.provider

import java.util.concurrent.CopyOnWriteArrayList

internal object SdkProviderRegistry {
    private val providers = CopyOnWriteArrayList<ISdkProvider>()

    @Synchronized
    fun register(provider: ISdkProvider) {
        providers.firstOrNull { it.javaClass == provider.javaClass }?.let(providers::remove)
        providers.add(provider)
    }

    fun all(): List<ISdkProvider> = providers.toList()
}
