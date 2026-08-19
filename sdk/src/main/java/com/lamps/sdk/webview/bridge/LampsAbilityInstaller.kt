package com.lamps.sdk.webview.bridge

abstract class LampsAbilityInstaller {
    abstract fun createAbilities(): Array<LampsAbility>
}
