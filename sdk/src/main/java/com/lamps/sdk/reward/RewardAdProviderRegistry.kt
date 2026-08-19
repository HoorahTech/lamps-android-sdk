package com.lamps.sdk.reward

import java.util.concurrent.CopyOnWriteArrayList

object RewardAdProviderRegistry {
    private val providers = CopyOnWriteArrayList<RewardAdProvider>()

    @JvmStatic
    @Synchronized
    fun register(provider: RewardAdProvider) {
        providers.firstOrNull { it.javaClass == provider.javaClass }?.let(providers::remove)
        providers.add(provider)
    }

    @JvmStatic
    @Synchronized
    fun unregister(provider: RewardAdProvider) {
        providers.remove(provider)
    }

    internal fun find(slot: RewardAdSlot): RewardAdProvider? {
        return providers.firstOrNull { provider ->
            runCatching { provider.supports(slot) }.getOrDefault(false)
        }
    }
}
