package com.lamps.sdk.pangle

import android.app.Activity
import android.app.Application
import com.bytedance.sdk.openadsdk.TTAdConfig
import com.bytedance.sdk.openadsdk.TTAdConstant
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTCustomController
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.core.LampsErrorCode
import com.lamps.sdk.data.sdk.channel.RewardAdSdkLoadCallback
import com.lamps.sdk.data.sdk.channel.SdkInitCallback
import com.lamps.sdk.reward.RewardAdShowCallback

object TTSdkManager {
    fun isInitialized(): Boolean = TTAdSdk.isInitSuccess()

    fun initSdk(
        application: Application,
        appId: String,
        callback: SdkInitCallback
    ) {
        if (LampsConfig.current?.initPangleSdk == false) {
            callback.success()
            return
        }
        if (isInitialized()) {
            callback.success()
            return
        }
        runCatching {
            val appName = application.applicationInfo
                .loadLabel(application.packageManager)
                .toString()
            val config = TTAdConfig.Builder()
                .appId(appId)
                .appName(appName)
                .paid(true)
                .titleBarTheme(TTAdConstant.TITLE_BAR_THEME_LIGHT)
                .allowShowNotify(true)
                .debug(LampsConfig.current?.debug?:false)
                .supportMultiProcess(false)
                .customController(object : TTCustomController() {
                    override fun isCanUseLocation(): Boolean = false

                    override fun alist(): Boolean = false

                    override fun userPrivacyConfig(): MutableMap<String, Any> {
                        return mutableMapOf("mcod" to "0")
                    }
                })
                .build()
            TTAdSdk.init(application, config)
            TTAdSdk.start(object : TTAdSdk.Callback {
                override fun success() {
                    callback.success()
                }

                override fun fail(code: Int, message: String?) {
                    callback.fail(
                        LampsErrorCode.TT_SDK_INIT_FAILED,
                        "TTAdSdk init failed: code=$code, message=${message.orEmpty()}"
                    )
                }
            })
        }.onFailure { error ->
            callback.fail(
                LampsErrorCode.TT_SDK_INIT_FAILED,
                error.message ?: "TTAdSdk init failed"
            )
        }
    }

    internal fun loadReward(
        activity: Activity,
        slotId: String,
        callback: RewardAdSdkLoadCallback
    ) {
        if (!isInitialized()) {
            callback.onLoadFailed(
                LampsErrorCode.TT_SDK_INIT_FAILED,
                "TTAdSdk is not initialized"
            )
            return
        }
        TTRewardVideoAd(activity, slotId).loadAD(callback)
    }

    internal fun showReward(
        activity: Activity,
        rewardAd: TTRewardVideoAd,
        callback: RewardAdShowCallback
    ) {
        rewardAd.showAD(activity, callback)
    }
}
