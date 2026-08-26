package com.lamps.sdk.data.sdk.init

import android.app.Application
import com.lamps.sdk.config.SdkConfig
import com.lamps.sdk.core.LampsErrorCode
import com.lamps.sdk.core.SdkInitMetrics
import com.lamps.sdk.data.sdk.channel.SdkInitCallback
import com.lamps.sdk.data.sdk.provider.SdkProviderRegistry
import com.lamps.sdk.utils.ThreadUtils
import com.lamps.sdk.utils.SdkLog
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal object SdkInitDispatcher {
    private val initDataList = CopyOnWriteArrayList<SdkInitData>()

    fun getInitDataList(): List<SdkInitData> = initDataList.toList()

    fun initSdk(config: SdkConfig, callback: SdkInitCallback) {
        val application = config.applicationContext as? Application
        if (application == null) {
            callback.fail(LampsErrorCode.INVALID_CONTEXT, "application context is invalid")
            return
        }

        initDataList.clear()
        initDataList.addAll(createInitDataList(config))

        SdkLog.d(
            "third-party init dispatch: providers=${SdkProviderRegistry.all().map { it.name }}, " +
                "selected=${initDataList.map { it.provider.name }}"
        )

        SdkInitMetrics.start(METRIC_DISPATCH, "第三方 SDK 初始化分发")
        if (initDataList.isNotEmpty()) {
            dispatchInitData(application, initDataList, callback)
        } else {
            SdkLog.d("third-party init skipped: no matching channel providers")
            SdkInitMetrics.end(METRIC_DISPATCH, SdkInitMetrics.RESULT_SKIPPED)
            callback.success()
        }
    }

    private fun createInitDataList(config: SdkConfig): List<SdkInitData> {
        val appInitData = config.appInitData ?: return emptyList()
        return appInitData.channelList.mapNotNull { channel ->
            if (channel.channelAppId.isBlank()) return@mapNotNull null
            SdkProviderRegistry.all().firstOrNull { it.supports(channel) }?.let { provider ->
                SdkInitData(provider, channel)
            }
        }.distinctBy { it.provider.javaClass }
    }

    private fun dispatchInitData(
        application: Application,
        dataList: List<SdkInitData>,
        callback: SdkInitCallback
    ) {
        val remaining = AtomicInteger(dataList.size)
        val finished = AtomicBoolean(false)
        ThreadUtils.runOnMain {
            dataList.forEach { initData ->
                if (!initData.markInitializing()) return@forEach
                SdkLog.d(
                    "provider init start: ${initData.provider.name}, " +
                        "internal=${SdkConfig.current?.let(initData.provider::shouldInitInternally) ?: true}, " +
                        "appId=${initData.appId}"
                )
                initData.provider.initSdk(
                    application,
                    initData.channel,
                    createInitCallback(initData, remaining, finished, callback)
                )
            }
        }
    }

    private fun createInitCallback(
        initData: SdkInitData,
        remaining: AtomicInteger,
        finished: AtomicBoolean,
        callback: SdkInitCallback
    ): SdkInitCallback {
        return object : SdkInitCallback {
            override fun success() {
                if (!initData.markSuccess()) return
                SdkLog.d(
                    "provider init success: ${initData.provider.name}, " +
                        "duration=${initData.durationMillis ?: 0}ms"
                )
                if (
                    remaining.decrementAndGet() == 0 &&
                    finished.compareAndSet(false, true)
                ) {
                    SdkInitMetrics.end(METRIC_DISPATCH, SdkInitMetrics.RESULT_SUCCESS)
                    callback.success()
                }
            }

            override fun fail(code: Int, message: String?) {
                if (!initData.markFailed(code, message)) return
                SdkLog.e(
                    "provider init failed: ${initData.provider.name}, " +
                        "code=$code, message=${message.orEmpty()}"
                )
                finishWithFailure(finished, callback, code, message)
            }
        }
    }

    private fun finishWithFailure(
        finished: AtomicBoolean,
        callback: SdkInitCallback,
        code: Int,
        message: String?
    ) {
        if (finished.compareAndSet(false, true)) {
            SdkInitMetrics.end(METRIC_DISPATCH, SdkInitMetrics.RESULT_FAILED)
            callback.fail(code, message)
        }
    }

    private const val METRIC_DISPATCH = "thirdSdk.dispatch"
}
