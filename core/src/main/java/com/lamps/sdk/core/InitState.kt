package com.lamps.sdk.core

internal sealed class InitState {
    data object Uninitialized : InitState()
    data object Initialized : InitState()
    data object Starting : InitState()
    data object Ready : InitState()
    data class Failed(
        val code: Int,
        val message: String? = null
    ) : InitState()
}
