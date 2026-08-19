package com.lamps.sdk.data.sdk.init

import android.app.Application
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.core.LampsErrorCode
import com.lamps.sdk.core.SdkInitMetrics
import com.lamps.sdk.data.init.RewardSlotResponse
import com.lamps.sdk.data.sdk.channel.ThirdSdkInitCallback
import com.lamps.sdk.utils.ThreadUtils
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal object SdkInitDispatcher {
    private val providers = CopyOnWriteArrayList<ISdkInitProvider>()

    init {
        register(TTSdkInitProvider())
        register(YLHSdkInitProvider())
        register(NoahSdkInitProvider())
    }

    @Synchronized
    fun register(provider: ISdkInitProvider) {
        providers.firstOrNull { it.javaClass == provider.javaClass }?.let(providers::remove)
        providers.add(provider)
    }

    fun initSdk(config: LampsConfig, callback: ThirdSdkInitCallback) {
        val application = config.applicationContext as? Application
        if (application == null) {
            callback.fail(LampsErrorCode.INVALID_CONTEXT, "application context is invalid")
            return
        }
        val slots = config.appInitData?.rewardAdSlots
            .orEmpty()
            .distinctBy { it.appId }
        val tasks = buildTasks(slots)
        SdkInitMetrics.start(METRIC_DISPATCH, "第三方 SDK 初始化分发")
        if (tasks.isNotEmpty()) {
            dispatchTasks(application, tasks, callback)
        }else {
            SdkInitMetrics.end(METRIC_DISPATCH, SdkInitMetrics.RESULT_SKIPPED)
            callback.success()
        }
    }

    private fun buildTasks(slots: List<RewardSlotResponse>): List<SdkInitTask> {
        return providers.mapNotNull { provider ->
            slots.firstOrNull(provider::supports)?.let { slot ->
                SdkInitTask(provider, slot)
            }
        }
    }

    private fun dispatchTasks(
        application: Application,
        tasks: List<SdkInitTask>,
        callback: ThirdSdkInitCallback
    ) {
        val remaining = AtomicInteger(tasks.size)
        val finished = AtomicBoolean(false)
        ThreadUtils.runOnMain {
            tasks.forEach { task ->
                val metricKey = task.metricKey()
                SdkInitMetrics.start(metricKey, task.provider.name)
                runCatching {
                    task.provider.initSdk(
                        application,
                        task.slot,
                        createTaskCallback(metricKey, remaining, finished, callback)
                    )
                }.onFailure { error ->
                    SdkInitMetrics.end(metricKey, SdkInitMetrics.RESULT_FAILED)
                    finishWithFailure(
                        finished,
                        callback,
                        LampsErrorCode.THIRD_SDK_INIT_DISPATCH_FAILED,
                        error.message ?: "third sdk init dispatch failed"
                    )
                }
            }
        }
    }

    private fun createTaskCallback(
        metricKey: String,
        remaining: AtomicInteger,
        finished: AtomicBoolean,
        callback: ThirdSdkInitCallback
    ): ThirdSdkInitCallback {
        return object : ThirdSdkInitCallback {
            override fun success() {
                SdkInitMetrics.end(metricKey, SdkInitMetrics.RESULT_SUCCESS)
                if (
                    remaining.decrementAndGet() == 0 &&
                    finished.compareAndSet(false, true)
                ) {
                    SdkInitMetrics.end(METRIC_DISPATCH, SdkInitMetrics.RESULT_SUCCESS)
                    callback.success()
                }
            }

            override fun fail(code: Int, message: String?) {
                SdkInitMetrics.end(metricKey, SdkInitMetrics.RESULT_FAILED)
                finishWithFailure(finished, callback, code, message)
            }
        }
    }

    private fun finishWithFailure(
        finished: AtomicBoolean,
        callback: ThirdSdkInitCallback,
        code: Int,
        message: String?
    ) {
        if (finished.compareAndSet(false, true)) {
            SdkInitMetrics.end(METRIC_DISPATCH, SdkInitMetrics.RESULT_FAILED)
            callback.fail(code, message)
        }
    }

    private data class SdkInitTask(
        val provider: ISdkInitProvider,
        val slot: RewardSlotResponse
    ) {
        fun metricKey(): String = "thirdSdk.${provider.javaClass.simpleName}"
    }

    private const val METRIC_DISPATCH = "thirdSdk.dispatch"
}
