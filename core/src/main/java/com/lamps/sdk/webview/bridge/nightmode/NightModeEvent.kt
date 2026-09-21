package com.lamps.sdk.webview.bridge.nightmode

/**
 * Native-to-H5 day/night events. There is no H5-callable method here: H5 reads the initial value
 * from `lamps.common.bridgeReady`, and later changes arrive through [SEND_NIGHT_MODE_CHANGE].
 */
internal object NightModeEvent {
    /** Payload: `{"night": 1}` for night, `{"night": 0}` for day. */
    const val SEND_NIGHT_MODE_CHANGE = "lamps.common.onnightmodechange"
}
