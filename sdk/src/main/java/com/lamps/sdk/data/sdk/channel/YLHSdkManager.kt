package com.lamps.sdk.data.sdk.channel

import android.app.Activity
import android.app.Application
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.core.LampsErrorCode
import com.lamps.sdk.reward.RewardAdShowCallback
import com.qq.e.comm.managers.GDTAdSdk
import com.qq.e.comm.managers.setting.GlobalSetting

object YLHSdkManager {
    @Volatile
    private var initialized = false

    fun isInitialized(): Boolean = initialized || GlobalSetting.getChannel() != null

    fun initSdk(
        application: Application,
        appId: String,
        callback: SdkInitCallback
    ) {
        if (initialized || LampsConfig.current?.initYlhSdk == false || GlobalSetting.getChannel() != null) {
            initialized = true
            callback.success()
            return
        }
        runCatching {
            // 默认采用最小权限配置；后续接入宿主隐私开关时在这里统一更新。
            GlobalSetting.setPersonalizedState(1)
            GlobalSetting.setExtraUserData(hashMapOf("shakable" to "0"))
            GlobalSetting.setEnableCollectAppInstallStatus(false)
            GlobalSetting.setConvOptimizeInfo(hashMapOf("hieib" to false))
            GlobalSetting.setAgreeReadPrivacyInfo(
                hashMapOf(
                    "mipaddr" to false,
                    "wipaddr" to false
                )
            )
            GDTAdSdk.init(application, appId)
        }.onSuccess {
            initialized = true
            callback.success()
        }.onFailure { error ->
            callback.fail(
                LampsErrorCode.YLH_SDK_INIT_FAILED,
                error.message ?: "GDTAdSdk init failed"
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
                LampsErrorCode.YLH_SDK_INIT_FAILED,
                "GDTAdSdk is not initialized"
            )
            return
        }
        YLHRewardVideoAd(activity, slotId).loadAD(callback)
    }

    internal fun showReward(
        activity: Activity,
        rewardAd: YLHRewardVideoAd,
        callback: RewardAdShowCallback
    ) {
        rewardAd.showAD(activity, callback)
    }
}
