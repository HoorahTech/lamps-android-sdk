package com.lamps.sdk

import android.content.Context
import com.lamps.sdk.config.GameCenterConfig
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.core.InitCallback
import com.lamps.sdk.core.SdkRuntime
import com.lamps.sdk.view.GameCenterView

object LampsSdk {

    @JvmStatic
    fun init(context: Context, config: LampsConfig): Boolean {
        return SdkRuntime.init(context, config.toSdkConfig())
    }

    @JvmStatic
    fun startAsync(callback: InitCallback) {
        SdkRuntime.startAsync(object : com.lamps.sdk.core.CoreInitCallback {
            override fun success() = callback.success()

            override fun fail(code: Int, message: String?) = callback.fail(code, message)
        })
    }

    @JvmStatic
    fun isSdkReady(): Boolean = SdkRuntime.isReady()

    @JvmStatic
    fun getSdkVersion(): String = BuildConfig.SDK_VERSION

    @JvmStatic
    @JvmOverloads
    fun navigateToGameCenter(
        context: Context,
        config: GameCenterConfig = GameCenterConfig.Builder().build()
    ) {
        SdkRuntime.navigateToGameCenter(
            context,
            config.withDisplayMode(GameCenterConfig.DisplayMode.PAGE)
                .toPageOptions()
        )
    }

    @JvmStatic
    fun navigateToGame(context: Context, gameId: String) {
        SdkRuntime.navigateToGame(context, gameId)
    }

    @JvmStatic
    @JvmOverloads
    fun getGameCenterView(
        context: Context,
        config: GameCenterConfig = GameCenterConfig.Builder().build()
    ): GameCenterView? {
        return SdkRuntime.getGameCenterUrl()?.let {
            GameCenterView(
                context,
                it,
                config.withDisplayMode(GameCenterConfig.DisplayMode.EMBED)
            )
        }
    }
}
