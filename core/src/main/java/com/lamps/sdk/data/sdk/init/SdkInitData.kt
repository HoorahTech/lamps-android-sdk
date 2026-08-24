package com.lamps.sdk.data.sdk.init

import com.lamps.sdk.data.init.ChannelInfoResponse
import com.lamps.sdk.data.sdk.provider.ISdkProvider

internal class SdkInitData(
    val provider: ISdkProvider,
    val channel: ChannelInfoResponse
) {
    val appId: String
        get() = channel.channelAppId

    @Volatile
    var state: SdkInitState = SdkInitState.PENDING
        private set

    @Volatile
    var errorCode: Int? = null
        private set

    @Volatile
    var errorMessage: String? = null
        private set

    @Volatile
    var startTimeMillis: Long? = null
        private set

    @Volatile
    var endTimeMillis: Long? = null
        private set

    val durationMillis: Long?
        get() {
            val start = startTimeMillis ?: return null
            val end = endTimeMillis ?: return null
            return (end - start).coerceAtLeast(0L)
        }

    @Synchronized
    fun markInitializing(): Boolean {
        if (state != SdkInitState.PENDING) return false
        state = SdkInitState.INITIALIZING
        startTimeMillis = System.currentTimeMillis()
        return true
    }

    @Synchronized
    fun markSuccess(): Boolean {
        if (state != SdkInitState.INITIALIZING) return false
        state = SdkInitState.SUCCESS
        endTimeMillis = System.currentTimeMillis()
        return true
    }

    @Synchronized
    fun markFailed(code: Int, message: String?): Boolean {
        if (state != SdkInitState.INITIALIZING) return false
        state = SdkInitState.FAILED
        errorCode = code
        errorMessage = message
        endTimeMillis = System.currentTimeMillis()
        return true
    }
}

internal enum class SdkInitState {
    PENDING,
    INITIALIZING,
    SUCCESS,
    FAILED
}
