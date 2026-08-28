package com.lamps.sdk.webview.bridge

import com.lamps.sdk.webview.bridge.bridgeReady.BridgeReadyAbility
import com.lamps.sdk.webview.bridge.network.NetworkAbility
import com.lamps.sdk.webview.bridge.track.TrackAbility
import com.lamps.sdk.webview.bridge.game.GamePageAbility

/**
 * A general-purpose ability installer that can hold all common abilities.
 * NetworkAbility is one of the abilities registered here.
 */
class CommonAbilityInstaller : LampsAbilityInstaller() {

    override fun createAbilities(): Array<LampsAbility> {
        return arrayOf(NetworkAbility(), TrackAbility(), GamePageAbility(), BridgeReadyAbility())
    }
}
