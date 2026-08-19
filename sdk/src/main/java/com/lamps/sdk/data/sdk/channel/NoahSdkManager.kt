package com.lamps.sdk.data.sdk.channel

import android.app.Application
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.core.LampsErrorCode
import com.noah.api.GlobalConfig
import com.noah.api.NoahSdk
import com.noah.api.NoahSdkConfig
import com.noah.api.InitCallback as NoahInitCallback

object NoahSdkManager {
    fun isInitialized(): Boolean = NoahSdk.isInitFinish()

    fun initSdk(
        application: Application,
        appId: String,
        callback: ThirdSdkInitCallback
    ) {
        if (isInitialized()) {
            callback.success()
            return
        }
        runCatching {
            val sdkConfig = NoahSdkConfig.Builder()
                .setAppKey(appId)
                .setOuterSettings(object : NoahSdkConfig.NoahOuterSettings() {
                    override fun getOAID(): String = LampsConfig.current?.resolveOaid()?:""

                    override fun getOAID2(): String = ""
                })
                .build()
            val globalConfig = GlobalConfig.newBuilder()
                .setDebug(LampsConfig.current?.debug?:false)
                .setAdTest(false)
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
}