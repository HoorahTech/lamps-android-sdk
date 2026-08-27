package com.lamps.sdk.core

internal sealed class InitState {
    object Uninitialized : InitState()
    object Initialized : InitState()
    object Starting : InitState()
    object Ready : InitState()
    data class Failed(
        val code: Int,
        val message: String? = null
    ) : InitState()
}
