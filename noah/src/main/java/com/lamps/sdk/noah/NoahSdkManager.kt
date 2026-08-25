package com.lamps.sdk.noah

import android.app.Activity
import android.app.Application
import com.lamps.sdk.config.SdkConfig
import com.lamps.sdk.core.LampsErrorCode
import com.lamps.sdk.data.sdk.channel.RewardAdSdkLoadCallback
import com.lamps.sdk.data.sdk.channel.SdkInitCallback
import com.lamps.sdk.reward.RewardAdShowCallback
import com.noah.api.GlobalConfig
import com.noah.api.NoahSdk
import com.noah.api.NoahSdkConfig
import com.noah.api.InitCallback as NoahInitCallback

object NoahSdkManager {
    fun isInitialized(): Boolean = NoahSdk.isInitFinish()

    fun initSdk(
        application: Application,
        appId: String,
        callback: SdkInitCallback
    ) {
        if (SdkConfig.current?.initNoahSdk == false) {
            callback.success()
            return
        }
        if (isInitialized()) {
            callback.success()
            return
        }
        runCatching {
            val sdkConfig = NoahSdkConfig.Builder()
                .setAppKey(appId)
                .setOuterSettings(object : NoahSdkConfig.NoahOuterSettings() {
                    override fun getOAID(): String = SdkConfig.current?.resolveOaid()?:""

                    override fun getOAID2(): String = ""
                })
                .build()
            val globalConfig = GlobalConfig.newBuilder()
                .setDebug(SdkConfig.current?.debug?:false)
                .setAdTest(SdkConfig.current?.debug?:false)
                .setEnablePersonalRecommend(false)
                .build()
            NoahSdk.initAsync(
                application,
                sdkConfig,
                globalConfig,
                object : NoahInitCallback {
                    override fun success() {
                        callback.success()
                    }

                    override fun fail(code: Int, message: String?) {
                        callback.fail(
                            LampsErrorCode.NOAH_SDK_INIT_FAILED,
                            "NoahSdk init failed: code=$code, message=${message.orEmpty()}"
                        )
                    }
                }
            )
        }.onFailure { error ->
            callback.fail(
                LampsErrorCode.NOAH_SDK_INIT_FAILED,
                error.message ?: "NoahSdk init failed"
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
                LampsErrorCode.NOAH_SDK_INIT_FAILED,
                "NoahSdk is not initialized"
            )
            return
        }
        NoahRewardVideoAd(activity, slotId).loadAD(callback)
    }

    internal fun showReward(
        activity: Activity,
        rewardAd: NoahRewardVideoAd,
        callback: RewardAdShowCallback
    ) {
        rewardAd.showAD(activity, callback)
    }
}
