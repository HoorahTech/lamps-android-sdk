package com.lamps.sdk.config

import com.lamps.sdk.webview.GameCenterPageOptions
import com.lamps.sdk.webview.LampsWebView

/**
 * 游戏中心打开配置。由宿主在 [com.lamps.sdk.LampsSdk.navigateToGameCenter]
 * / [com.lamps.sdk.LampsSdk.getGameCenterView] 时传入，后续扩展字段加在 Builder 上。
 *
 * 日夜间优先用 [Builder.setNightMode]；未设则打开时从 [LampsConfig] 的 NightModeProvider 现取；都没有则默认日间。
 * [Builder.setHideTitle] 控制是否隐藏标题，默认不隐藏；打开时写入，运行时更新不生效。
 * [DisplayMode] 由 SDK 按打开方式写入，不要通过 Builder 设置。
 */
class GameCenterConfig private constructor(
    private val nightMode: NightMode?,
    internal val displayMode: DisplayMode?,
    private val hideTitle: Boolean,
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
        )
    }

    class Builder {
        private var nightMode: NightMode? = null
        private var hideTitle: Boolean = false

        fun setNightMode(nightMode: NightMode) = apply { this.nightMode = nightMode }

        /**
         * 是否隐藏标题。`true` 时 H5 收到 `hideTitle = 1`，`false` 时为 `0`。默认不隐藏。
         */
        fun setHideTitle(hideTitle: Boolean) = apply { this.hideTitle = hideTitle }

        fun build(): GameCenterConfig = GameCenterConfig(
            nightMode = nightMode,
            displayMode = null,
            hideTitle = hideTitle,
        )
    }
}
