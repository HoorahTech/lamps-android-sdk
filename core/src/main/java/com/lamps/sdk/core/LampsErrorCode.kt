package com.lamps.sdk.core

object LampsErrorCode {
    const val NOT_INITIALIZED = 1001
    const val APP_ID_EMPTY = 1002
    const val OAID_EMPTY = 1003
    const val INVALID_CONTEXT = 1004
    const val APP_INIT_DATA_REQUEST_FAILED = 1005
    const val START_IN_PROGRESS = 1006
    const val TT_SDK_INIT_FAILED = 1007
    const val YLH_SDK_INIT_FAILED = 1008
    const val NOAH_SDK_INIT_FAILED = 1009
    const val THIRD_SDK_INIT_DISPATCH_FAILED = 1010

    /** 游戏中心地址或游戏页模板未下发，通常是初始化未完成。 */
    const val PAGE_URL_EMPTY = 1101
    const val GAME_ID_EMPTY = 1102
    const val PAGE_URL_INVALID = 1103
}
