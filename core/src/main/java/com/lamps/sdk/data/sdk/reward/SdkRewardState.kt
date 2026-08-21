package com.lamps.sdk.data.sdk.reward

internal enum class SdkRewardState {
    PENDING,
    LOADING,
    LOAD_SUCCESS,
    LOAD_FAILED,
    BID_FAILED,
    SELECTED,
    SHOWING,
    SHOWN,
    SHOW_FAILED,
    CLOSED
}
