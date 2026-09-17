package com.lamps.sdk.config

import com.lamps.sdk.webview.GameCenterPageOptions

/**
 * 游戏中心打开配置。由宿主在 [com.lamps.sdk.LampsSdk.navigateToGameCenter]
 * / [com.lamps.sdk.LampsSdk.getGameCenterView] 时传入，后续扩展字段加在 Builder 上。
 *
 * [NightMode] 由宿主设置；[DisplayMode] 由 SDK 按打开方式写入，不要通过 Builder 设置。
 */
class GameCenterConfig private constructor(
    val nightMode: NightMode,
    internal val displayMode: DisplayMode?,
) {
    enum class NightMode(val value: Int) {
        DAY(0),
        NIGHT(1),
    }

    enum class DisplayMode(val value: String) {
        PAGE("page"),
        EMBED("embed"),
    }

    internal fun withDisplayMode(displayMode: DisplayMode): GameCenterConfig {
        return GameCenterConfig(
            nightMode = nightMode,
            displayMode = displayMode,
        )
    }

    internal fun toPageOptions(): GameCenterPageOptions {
        return GameCenterPageOptions(
            night = nightMode == NightMode.NIGHT,
            displayMode = displayMode?.value.orEmpty(),
        )
    }

    class Builder {
        private var nightMode: NightMode = NightMode.DAY

        fun setNightMode(nightMode: NightMode) = apply { this.nightMode = nightMode }

        fun build(): GameCenterConfig = GameCenterConfig(
            nightMode = nightMode,
            displayMode = null,
        )
    }
}
