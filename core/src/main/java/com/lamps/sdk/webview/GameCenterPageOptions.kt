package com.lamps.sdk.webview

import java.io.Serializable

/** Host-provided game center page options, mapped from sdk [com.lamps.sdk.config.GameCenterConfig]. */
class GameCenterPageOptions(
    val night: Boolean = false,
    val displayMode: String = "",
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
