package com.lamps.sdk.config

import com.lamps.sdk.webview.GamePageOptions

/**
 * 单个游戏打开配置。由宿主在 [com.lamps.sdk.LampsSdk.navigateToGame] 时传入，后续扩展字段加在 Builder 上。
 *
 * [Builder.setEncodedQuery] 透传 deeplink 原始 query，按原编码合并到游戏页链接。
 * [Builder.setTrackCustomData] 的字段会平铺进本次打开对应的行为回调。
 */
class GameConfig private constructor(
    internal val encodedQuery: String?,
    internal val trackCustomData: Map<String, String>,
) {
    class Builder {
        private var encodedQuery: String? = null
        private var trackCustomData: Map<String, String> = emptyMap()

        /**
         * deeplink 的原始 query，通常传 `uri.encodedQuery`。SDK 按原编码合并，不重新编码和解码。
         * 其中的 `gameId` 会被忽略，避免和模板里的 `gameId` 重复。
         */
        fun setEncodedQuery(encodedQuery: String?) = apply { this.encodedQuery = encodedQuery }

        /**
         * 本次打开携带的自定义数据。打开成功、打开失败和退出时，每个字段直接写进行为回调的 `data`。
         * 空 map 不额外写字段。
         */
        fun setTrackCustomData(data: Map<String, String>) = apply { this.trackCustomData = LinkedHashMap(data) }

        fun build(): GameConfig = GameConfig(
            encodedQuery = encodedQuery,
            trackCustomData = trackCustomData,
        )
    }

    internal fun toPageOptions(gameId: String): GamePageOptions = GamePageOptions(
        gameId = gameId,
        encodedQuery = encodedQuery,
        trackCustomData = LinkedHashMap(trackCustomData),
    )
}
