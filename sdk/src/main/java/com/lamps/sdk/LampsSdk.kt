package com.lamps.sdk

import android.content.Context
import com.lamps.sdk.config.GameCenterConfig
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.core.CoreRewardMonitorCallback
import com.lamps.sdk.core.CoreTrackCallback
import com.lamps.sdk.core.InitCallback
import com.lamps.sdk.core.RewardMonitorCallback
import com.lamps.sdk.core.SdkRuntime
import com.lamps.sdk.core.TrackCallback
import com.lamps.sdk.data.monitor.RewardMonitor
import com.lamps.sdk.utils.TrackSdk
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

    /**
     * 抛出 SDK 提交的全部埋点。同一实例重复注册只会回调一次。
     * 回调发生在产生事件的线程，接入方应尽快返回，再自行异步上报。
     */
    @JvmStatic
    fun registerTrackCallback(callback: TrackCallback) {
        TrackSdk.addListener(
            callback,
            CoreTrackCallback { action, payload -> callback.onTrack(action, payload) }
        )
    }

    @JvmStatic
    fun unregisterTrackCallback(callback: TrackCallback) {
        TrackSdk.removeListener(callback)
    }

    /**
     * 抛出激励视频监测事件：RM 填充、WM 竞胜、PM 曝光、CM 点击、DM 发奖。
     * 同一实例重复注册只会回调一次。回调发生在产生事件的线程，接入方应尽快返回。
     */
    @JvmStatic
    fun registerRewardMonitorCallback(callback: RewardMonitorCallback) {
        RewardMonitor.addListener(
            callback,
            CoreRewardMonitorCallback { event, payload -> callback.onRewardMonitor(event, payload) }
        )
    }

    @JvmStatic
    fun unregisterRewardMonitorCallback(callback: RewardMonitorCallback) {
        RewardMonitor.removeListener(callback)
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
