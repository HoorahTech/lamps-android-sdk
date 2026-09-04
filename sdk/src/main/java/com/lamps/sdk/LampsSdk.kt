package com.lamps.sdk

import android.content.Context
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
    fun navigateToGameCenter(context: Context) {
        SdkRuntime.navigateToGameCenter(context)
    }

    @JvmStatic
    fun navigateToGame(context: Context, gameId: String) {
        SdkRuntime.navigateToGame(context, gameId)
    }

    @JvmStatic
    fun getGameCenterView(context: Context): GameCenterView? {
        return SdkRuntime.getGameCenterUrl()?.let { GameCenterView(context, it) }
    }
}
