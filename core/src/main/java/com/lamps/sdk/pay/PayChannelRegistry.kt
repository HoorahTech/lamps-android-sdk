package com.lamps.sdk.pay

import java.util.concurrent.CopyOnWriteArrayList

internal object PayChannelRegistry {
    private val channels = CopyOnWriteArrayList<PayChannel>()

    @Synchronized
    fun register(channel: PayChannel) {
        channels.removeAll { it.channel.equals(channel.channel, ignoreCase = true) }
        channels.add(channel)
    }

    fun find(channel: String): PayChannel? {
        return channels.firstOrNull { it.channel.equals(channel, ignoreCase = true) }
    }

    fun contains(channel: String): Boolean = find(channel) != null

    fun all(): List<PayChannel> = channels.toList()
}
