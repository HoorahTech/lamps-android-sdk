package com.lamps.sdk.core

import android.content.Context
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.data.init.AppInitDataLoader
import com.lamps.sdk.utils.SdkLog
import com.lamps.sdk.utils.ThreadUtils
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

internal object SdkRuntime {
    private val runtime = AtomicReference<InitState>(InitState.Uninitialized)
    private val callbacks = CopyOnWriteArrayList<InitCallback>()

    fun init(context: Context, config: LampsConfig): Boolean {
        if (runtime.get() != InitState.Uninitialized) {
            SdkLog.e("init failed: sdk has initialized:${runtime.get()}")
            return false
        }
        if (config.appId.isEmpty()) {
            SdkLog.e("init failed: appId is empty")
            return false
        }
        if (config.oaidProvider == null) {
            SdkLog.e("init failed: oaidProvider is required")
            return false
        }

        if (runtime.compareAndSet(InitState.Uninitialized, InitState.Initialized)) {
            SdkLog.e("init success")
            LampsConfig.init(context, config)
            return true
        } else {
            SdkLog.e("init failed: sdk has initialized_v2:${runtime.get()}")
            return false
        }
    }


    fun startAsync(callback: InitCallback) {
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
    fun checkInit(callback: InitCallback) {
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

    fun updateConfig(config: LampsConfig) {
        val current = LampsConfig.current ?: return
        LampsConfig.replace(current.mergedWith(config))
    }

    private fun doStart() {
        runtime.set(InitState.Starting)
        val config = LampsConfig.current
        if (config == null) {
            notifyFail(LampsErrorCode.NOT_INITIALIZED, "call init() before start()")
            return
        }
        if (config.resolveOaid().isEmpty()) {
            notifyFail(LampsErrorCode.OAID_EMPTY, "oaid is required")
            return
        }
        if (AppInitDataLoader.load(config)) {
            runtime.set(InitState.Ready)
            notifySuccess()
        } else {
            notifyFail(LampsErrorCode.APP_INIT_DATA_REQUEST_FAILED, "appInitData request failed")
        }
    }


    private fun notifySuccess() {
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
}
