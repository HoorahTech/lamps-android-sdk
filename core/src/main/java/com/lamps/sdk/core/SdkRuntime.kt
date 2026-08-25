package com.lamps.sdk.core

import android.app.Activity
import android.content.Context
import com.lamps.sdk.config.SdkConfig
import com.lamps.sdk.data.init.AppInitDataLoader
import com.lamps.sdk.data.sdk.channel.SdkInitCallback
import com.lamps.sdk.data.sdk.init.SdkInitDispatcher
import com.lamps.sdk.data.sdk.reward.RewardAdErrorCode
import com.lamps.sdk.data.sdk.reward.SdkRewardDispatcher
import com.lamps.sdk.reward.LampsRewardAd
import com.lamps.sdk.reward.RewardAdLoadCallback
import com.lamps.sdk.reward.RewardAdShowCallback
import com.lamps.sdk.utils.LampsApiHost
import com.lamps.sdk.utils.SdkLog
import com.lamps.sdk.utils.ThreadUtils
import com.lamps.sdk.webview.LampsWebViewActivity
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

object SdkRuntime {
    private val runtime = AtomicReference<InitState>(InitState.Uninitialized)
    private val callbacks = CopyOnWriteArrayList<CoreInitCallback>()

    fun init(context: Context, config: SdkConfig): Boolean {
        if (runtime.get() != InitState.Uninitialized) {
            SdkLog.e("init failed: sdk has initialized:${runtime.get()}")
            return false
        }
        if (config.appId.isEmpty()) {
            SdkLog.e("init failed: appId is empty")
            return false
        }

        if (runtime.compareAndSet(InitState.Uninitialized, InitState.Initialized)) {
            SdkLog.e("init success")
            SdkConfig.init(context, config)
            LampsApiHost.restore(context.applicationContext)
            return true
        } else {
            SdkLog.e("init failed: sdk has initialized_v2:${runtime.get()}")
            return false
        }
    }


    fun startAsync(callback: CoreInitCallback) {
        callbacks.add(callback)
        when (val state = runtime.get()) {
            InitState.Starting -> {
                //不做任何事情，等待最终结果
            }

            InitState.Ready -> {
                //直接算成功
                notifySuccess()
            }

            InitState.Initialized -> {
                //去子线程开始执行
                ThreadUtils.runOnWork {
                    doStart()
                }
            }

            InitState.Uninitialized -> notifyFail(
                LampsErrorCode.NOT_INITIALIZED,
                "call init() before start(), current state=$state"
            )

            is InitState.Failed -> notifyFail(
                state.code,
                state.message ?: "call init() before start(), current state=$state"
            )
        }
    }

    /**
     * 对齐汇川 [com.noah.api.NoahSdk.checkInit]：
     * 已成功则立刻回调；进行中/尚未 start 则挂起等待结果；失败则立刻带回上次错误。
     */
    fun checkInit(callback: CoreInitCallback) {
        when (val state = runtime.get()) {
            InitState.Ready -> {
                callback.success()
            }

            is InitState.Failed -> {
                callback.fail(
                    state.code,
                    state.message ?: "call init() before start(), current state=$state"
                )
            }

            else -> {
                callbacks.add(callback)
            }
        }
    }

    fun isReady(): Boolean = runtime.get() == InitState.Ready

    fun loadReward(
        activity: Activity,
        callback: RewardAdLoadCallback,
        forwardSource: String = ""
    ) {
        if (!isReady()) {
            callback.onAdLoadFailed(
                RewardAdErrorCode.SDK_NOT_READY,
                "LampsSdk is not ready"
            )
            return
        }
        if (activity.isFinishing || activity.isDestroyed) {
            callback.onAdLoadFailed(
                RewardAdErrorCode.ACTIVITY_NOT_FOUND,
                "Activity is unavailable"
            )
            return
        }
        val config = SdkConfig.current
        if (config == null) {
            callback.onAdLoadFailed(
                RewardAdErrorCode.SDK_NOT_READY,
                "LampsSdk config is unavailable"
            )
            return
        }
        SdkRewardDispatcher.loadReward(activity, config, callback, forwardSource)
    }

    fun showReward(activity: Activity, ad: LampsRewardAd, callback: RewardAdShowCallback) {
        if (!isReady()) {
            callback.onAdShowFailed(
                RewardAdErrorCode.SDK_NOT_READY,
                "LampsSdk is not ready"
            )
            return
        }
        if (activity.isFinishing || activity.isDestroyed) {
            callback.onAdShowFailed(
                RewardAdErrorCode.ACTIVITY_NOT_FOUND,
                "Activity is unavailable"
            )
            return
        }
        SdkRewardDispatcher.showReward(activity, ad, callback)
    }

    private fun doStart() {
        SdkInitMetrics.reset()
        SdkInitMetrics.start(METRIC_TOTAL, "Lamps SDK 总初始化")
        runtime.set(InitState.Starting)
        val config = SdkConfig.current
        if (config == null) {
            notifyFail(LampsErrorCode.NOT_INITIALIZED, "call init() before start()")
            return
        }
        SdkInitMetrics.start(METRIC_OAID, "读取 OAID")
        val oaid = runCatching { config.resolveOaid() }.getOrDefault("")
        SdkInitMetrics.end(
            METRIC_OAID,
            if (oaid.isEmpty()) SdkInitMetrics.RESULT_SKIPPED else SdkInitMetrics.RESULT_SUCCESS
        )
        val loadResult = AppInitDataLoader.load(config)
        if (!loadResult.hasData) {
            notifyFail(LampsErrorCode.APP_INIT_DATA_REQUEST_FAILED, "appInitData request failed")
            return
        }
        SdkInitDispatcher.initSdk(config, object : SdkInitCallback {
            override fun success() {
                notifySuccess()
            }

            override fun fail(code: Int, message: String?) {
                notifyFail(code, message)
            }
        })
    }


    private fun notifySuccess() {
        SdkInitMetrics.end(METRIC_TOTAL, SdkInitMetrics.RESULT_SUCCESS)
        runtime.set(InitState.Ready)
        ThreadUtils.runOnMain {
            try {
                callbacks.forEach {
                    it.success()
                }
            } catch (t: Throwable) {
                SdkLog.w("callback threw: ${t.message}", t)
            }
        }
    }

    private fun notifyFail(code: Int = 0, message: String? = null) {
        SdkInitMetrics.end(METRIC_TOTAL, SdkInitMetrics.RESULT_FAILED)
        runtime.set(InitState.Failed(code = code, message = message))
        ThreadUtils.runOnMain {
            try {
                callbacks.forEach {
                    it.fail(code, message)
                }
            } catch (t: Throwable) {
                SdkLog.w("callback threw: ${t.message}", t)
            }
        }
    }


    fun navigateToGameCenter(context: Context) {
        val url = SdkConfig.current?.appInitData?.gameCenterPage
            ?.takeIf { it.isNotBlank() }
            ?: return
        context.startActivity(
            LampsWebViewActivity.buildIntent(
                context = context,
                url = url,
                title = "游戏中心"
            )
        )
    }

    fun getGameCenterUrl(): String? {
        val url = SdkConfig.current?.appInitData?.gameCenterPage
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return url
    }

    private const val METRIC_TOTAL = "lamps.total"
    private const val METRIC_OAID = "lamps.oaid"
}
