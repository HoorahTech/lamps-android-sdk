package com.lamps.sdk.config

import com.lamps.sdk.webview.GameCenterPageOptions
import com.lamps.sdk.webview.LampsWebView

/**
 * 游戏中心打开配置。由宿主在 [com.lamps.sdk.LampsSdk.navigateToGameCenter]
 * / [com.lamps.sdk.LampsSdk.getGameCenterView] 时传入，后续扩展字段加在 Builder 上。
 *
 * 日夜间优先用 [Builder.setNightMode]；未设则打开时从 [LampsConfig] 的 NightModeProvider 现取；都没有则默认日间。
 * [Builder.setHideTitle] 控制是否隐藏标题，默认不隐藏；打开时写入，运行时更新不生效。
 * [Builder.setEncodedQuery] 透传 deeplink 原始 query，打开时按原编码合并到游戏中心链接，运行时更新不生效。
 * [Builder.setTrackCustomData] 的字段会平铺进整页游戏中心的行为回调。内嵌游戏中心自身不回调，页内打开的单个游戏仍带上这份数据。运行时更新不生效。
 * [DisplayMode] 由 SDK 按打开方式写入，不要通过 Builder 设置。
 */
class GameCenterConfig private constructor(
    private val nightMode: NightMode?,
    internal val displayMode: DisplayMode?,
    private val hideTitle: Boolean,
    internal val encodedQuery: String?,
    internal val trackCustomData: Map<String, String>,
) {
    enum class DisplayMode(val value: String) {
        PAGE(LampsWebView.DISPLAY_MODE_PAGE),
        EMBED(LampsWebView.DISPLAY_MODE_EMBED),
    }

    internal fun withDisplayMode(displayMode: DisplayMode): GameCenterConfig {
        return GameCenterConfig(
            nightMode = nightMode,
            displayMode = displayMode,
            hideTitle = hideTitle,
            encodedQuery = encodedQuery,
            trackCustomData = trackCustomData,
        )
    }

    internal fun toPageOptions(): GameCenterPageOptions {
        val resolved = nightMode ?: if (SdkConfig.current?.resolveIsNight() == true) {
            NightMode.NIGHT
        } else {
            NightMode.DAY
        }
        return GameCenterPageOptions(
            night = resolved == NightMode.NIGHT,
            displayMode = displayMode?.value.orEmpty(),
            hideTitle = hideTitle,
            encodedQuery = encodedQuery,
            trackCustomData = LinkedHashMap(trackCustomData),
        )
    }

    class Builder {
        private var nightMode: NightMode? = null
        private var hideTitle: Boolean = false
        private var encodedQuery: String? = null
        private var trackCustomData: Map<String, String> = emptyMap()

        fun setNightMode(nightMode: NightMode) = apply { this.nightMode = nightMode }

        /**
         * 是否隐藏标题。`true` 时 H5 收到 `hideTitle = 1`，`false` 时为 `0`。默认不隐藏。
         */
        fun setHideTitle(hideTitle: Boolean) = apply { this.hideTitle = hideTitle }

        /**
         * deeplink 的原始 query，通常传 `uri.encodedQuery`。打开时按原编码合并到游戏中心链接，不重新编码和解码。
         * 运行时 [com.lamps.sdk.view.GameCenterView.updateConfig] 不生效。
         */
        fun setEncodedQuery(encodedQuery: String?) = apply { this.encodedQuery = encodedQuery }

        /**
         * 本次打开携带的自定义数据。整页游戏中心的打开成功、打开失败和退出时，每个字段直接写进行为回调的 `data`。
         * 内嵌游戏中心自身不回调；页内打开的单个游戏仍带上这份数据。空 map 不额外写字段。`updateConfig` 不生效。
         */
        fun setTrackCustomData(data: Map<String, String>) = apply { this.trackCustomData = LinkedHashMap(data) }

        fun build(): GameCenterConfig = GameCenterConfig(
            nightMode = nightMode,
            displayMode = null,
            hideTitle = hideTitle,
            encodedQuery = encodedQuery,
            trackCustomData = trackCustomData,
        )
    }
}
