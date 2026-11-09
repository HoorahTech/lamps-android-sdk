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
import com.lamps.sdk.utils.SdkLog
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
        SdkRuntime.registerTrackCallback(
            callback,
            CoreTrackCallback { action, payload -> callback.onTrack(action, payload) }
        )
    }

    @JvmStatic
    fun unregisterTrackCallback(callback: TrackCallback) {
        SdkRuntime.unregisterTrackCallback(callback)
    }

    /**
     * 抛出激励视频监测事件：RM 填充、WM 竞胜、PM 曝光、CM 点击、DM 发奖。
     * 同一实例重复注册只会回调一次。回调发生在产生事件的线程，接入方应尽快返回。
     */
    @JvmStatic
    fun registerRewardMonitorCallback(callback: RewardMonitorCallback) {
        SdkRuntime.registerRewardMonitorCallback(
            callback,
            CoreRewardMonitorCallback { event, payload ->
                callback.onRewardMonitor(event, payload)
            }
        )
    }

    @JvmStatic
    fun unregisterRewardMonitorCallback(callback: RewardMonitorCallback) {
        SdkRuntime.unregisterRewardMonitorCallback(callback)
    }

    /**
     * 同步当前登录用户。整份覆盖，可在 init 前后调用，只保存在进程内存。
     * userId 去掉空白后为空时忽略本次调用，并保留上一份快照。
     *
     * @return 是否写入成功
     */
    @JvmStatic
    fun updateUser(user: LampsUser?): Boolean {
        if (user == null) {
            SdkLog.e("updateUser ignored: user is null")
            return false
        }
        return SdkRuntime.updateUser(user.userId, user.nickname, user.avatar)
    }

    /** 清除当前登录用户。退出登录时调用。 */
    @JvmStatic
    fun clearUser() {
        SdkRuntime.clearUser()
    }

    @JvmStatic
    fun isSdkReady(): Boolean = SdkRuntime.isReady()

    @JvmStatic
    fun getSdkVersion(): String = BuildConfig.SDK_VERSION

    @JvmStatic
    @JvmOverloads
    fun navigateToGameCenter(
        context: Context,
        config: GameCenterConfig = GameCenterConfig.Builder().build(),
        encodedQuery: String? = null,
    ) {
        SdkRuntime.navigateToGameCenter(
            context,
            config.withDisplayMode(GameCenterConfig.DisplayMode.PAGE)
                .toPageOptions(),
            encodedQuery,
        )
    }

    @JvmStatic
    @JvmOverloads
    fun navigateToGame(
        context: Context,
        gameId: String,
        encodedQuery: String? = null,
    ) {
        SdkRuntime.navigateToGame(context, gameId, encodedQuery)
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
