package com.lamps.sdk.webview

import java.io.Serializable

/** Host-provided game page options, mapped from sdk [com.lamps.sdk.config.GameConfig]. */
class GamePageOptions(
    val gameId: String = "",
    val encodedQuery: String? = null,
    val trackCustomData: Map<String, String> = emptyMap(),
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
