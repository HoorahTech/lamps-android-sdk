package com.lamps.sdk

import android.content.Context
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.core.InitCallback
import com.lamps.sdk.core.SdkRuntime
import com.lamps.sdk.view.GameCenterView

object LampsSdk {

    @JvmStatic
    fun init(context: Context, config: LampsConfig): Boolean {
        return SdkRuntime.init(context, config)
    }
    @JvmStatic
    fun startAsync(callback: InitCallback) {
        SdkRuntime.startAsync(callback)
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
    fun getGameCenterView(context: Context): GameCenterView? {
        return SdkRuntime.getGameCenterView(context)
    }
}
