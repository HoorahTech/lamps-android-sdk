package com.lamps.sdk.data.init

import android.os.SystemClock
import com.lamps.sdk.BuildConfig
import com.lamps.sdk.config.SdkConfig
import com.lamps.sdk.core.SdkInitMetrics
import com.lamps.sdk.utils.DeviceUtils
import com.lamps.sdk.utils.SdkLog
import com.lamps.sdk.utils.ThreadUtils
import java.util.concurrent.atomic.AtomicLong

internal object AppInitDataLoader {
    private val lastSuccessElapsed = AtomicLong(0L)

    fun load(config: SdkConfig): AppInitLoadResult {
        SdkInitMetrics.start(METRIC_LOAD, "初始化数据加载")
        val cacheSucceeded = applyCache(config)
        if (cacheSucceeded) {
            SdkInitMetrics.end(METRIC_LOAD, SdkInitMetrics.RESULT_SUCCESS)
            ThreadUtils.runOnWork { applyRemote(config, updateCurrent = false) }
            return AppInitLoadResult(hasData = true)
        }

        val result = AppInitLoadResult(
            hasData = applyRemote(config, updateCurrent = true)
        )
        SdkInitMetrics.end(
            METRIC_LOAD,
            if (result.hasData) SdkInitMetrics.RESULT_SUCCESS else SdkInitMetrics.RESULT_FAILED
        )
        return result
    }

    /**
     * 进入游戏容器时调用。距上次成功更新超过 5 分钟才再请求；
     * 请求在工作线程执行，不阻塞页面打开。
     */
    fun refreshIfStale() {
        val config = SdkConfig.current ?: return
        ThreadUtils.runOnWork {
            val last = lastSuccessElapsed.get()
            val elapsed = SystemClock.elapsedRealtime() - last
            if (last != 0L && elapsed < REFRESH_INTERVAL_MS) {
                SdkLog.d("skip appInitData refresh: last update ${elapsed}ms ago")
                return@runOnWork
            }
            applyRemote(config, updateCurrent = true, trackMetrics = false)
        }
    }

    private fun applyCache(config: SdkConfig): Boolean {
        SdkInitMetrics.start(METRIC_CACHE, "读取初始化数据缓存")
        val raw = AppInitDataCache.read()
        if (raw.isNullOrBlank()) {
            SdkLog.d("no local appInitData cache")
            SdkInitMetrics.end(METRIC_CACHE, SdkInitMetrics.RESULT_CACHE_MISS)
            return false
        }
        return AppInitDataParser.parse(raw).fold(
            onSuccess = { response ->
                if (isValid(response)) {
                    config.appInitData = response
                    SdkLog.d("loaded appInitData from cache")
                    SdkInitMetrics.end(METRIC_CACHE, SdkInitMetrics.RESULT_CACHE_HIT)
                    true
                } else {
                    SdkLog.w("appInitData cache is invalid")
                    SdkInitMetrics.end(METRIC_CACHE, SdkInitMetrics.RESULT_FAILED)
                    false
                }
            },
            onFailure = { error ->
                SdkLog.w("appInitData cache parse failed: ${error.message}")
                SdkInitMetrics.end(METRIC_CACHE, SdkInitMetrics.RESULT_FAILED)
                false
            }
        )
    }

    private fun applyRemote(
        config: SdkConfig,
        updateCurrent: Boolean,
        trackMetrics: Boolean = true
    ): Boolean {
        if (trackMetrics) {
            SdkInitMetrics.start(METRIC_REMOTE, "请求远端初始化数据")
        }
        val context = config.applicationContext
        if (context == null) {
            if (trackMetrics) {
                SdkInitMetrics.end(METRIC_REMOTE, SdkInitMetrics.RESULT_SKIPPED)
            }
            return false
        }
        val request = AppInitRequest(
            androidId = DeviceUtils.androidId(context),
            appId = config.appId,
            version = BuildConfig.SDK_VERSION,
            oaid = config.resolveOaid(),
            os = "Android"
        )
        return HttpAppInitDataRepository().getAppInitData(request).fold(
            onSuccess = { snapshot ->
                if (isValid(snapshot.data)) {
                    AppInitDataCache.write(snapshot.raw)
                    if (updateCurrent) {
                        config.appInitData = snapshot.data
                    }
                    lastSuccessElapsed.set(SystemClock.elapsedRealtime())
                    SdkLog.d("appInitData updated from server, updateCurrent=$updateCurrent")
                    if (trackMetrics) {
                        SdkInitMetrics.end(METRIC_REMOTE, SdkInitMetrics.RESULT_SUCCESS)
                    }
                    true
                } else {
                    SdkLog.e("remote appInitData is invalid")
                    if (trackMetrics) {
                        SdkInitMetrics.end(METRIC_REMOTE, SdkInitMetrics.RESULT_FAILED)
                    }
                    false
                }
            },
            onFailure = { error ->
                SdkLog.e("fetch appInitData failed: ${error.message}")
                if (trackMetrics) {
                    SdkInitMetrics.end(METRIC_REMOTE, SdkInitMetrics.RESULT_FAILED)
                }
                false
            }
        )
    }

    private fun isValid(data: AppInitResponse): Boolean {
        return data.rewardAdSlots.all { slot ->
                slot.slotId.isNotBlank()
            } &&
            data.channelList.all { channel ->
                channel.channelName.isNotBlank() && channel.channelAppId.isNotBlank()
            }
    }

    private const val METRIC_LOAD = "appInitData.load"
    private const val METRIC_CACHE = "appInitData.cache"
    private const val METRIC_REMOTE = "appInitData.remote"
    private const val REFRESH_INTERVAL_MS = 5 * 60 * 1000L
}

internal data class AppInitLoadResult(
    val hasData: Boolean
)
