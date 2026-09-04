package com.lamps.sdk.debug

import android.content.Context
import com.lamps.sdk.BuildConfig
import com.lamps.sdk.config.SdkConfig
import com.lamps.sdk.core.SdkInitMetrics
import com.lamps.sdk.core.SdkRuntime
import com.lamps.sdk.data.sdk.init.SdkInitDispatcher
import com.lamps.sdk.data.sdk.provider.SdkProviderRegistry
import com.lamps.sdk.data.sdk.reward.SdkRewardDispatcher
import com.lamps.sdk.data.monitor.MonitorReportRecorder
import com.lamps.sdk.data.monitor.MonitorUtil
import com.lamps.sdk.utils.DeviceUtils
import com.lamps.sdk.utils.LampsApiHost
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LampsSdkDebugItem(
    val id: String,
    val title: String,
    val body: String,
    val expandedByDefault: Boolean = false
)

class LampsSdkDebugSection(
    val title: String,
    val body: String,
    val expandedByDefault: Boolean,
    val items: List<LampsSdkDebugItem> = emptyList()
)

/**
 * LampsSdkTools 使用的调试信息。不是稳定宿主 API。
 */
object LampsSdkDebug {
    private val EXPANDED_TITLES = setOf("SDK 状态", "SDK初始化入参")

    @JvmStatic
    fun apiEnvLabel(context: Context? = null): String = LampsApiHost.current(context).label

    @JvmStatic
    fun apiBaseUrl(context: Context? = null): String = LampsApiHost.baseUrl(context)

    @JvmStatic
    fun toggleApiEnv(context: Context): String {
        val env = LampsApiHost.toggle(context)
        return "${env.label} ${env.baseUrl}"
    }

    @JvmStatic
    fun buildInfo(context: Context): String {
        return buildSections(context).joinToString("\n\n") { section ->
            buildString {
                appendLine("========== ${section.title} ==========")
                if (section.body.isNotEmpty()) appendLine(section.body)
                section.items.forEach { item ->
                    appendLine()
                    appendLine("[${item.title}]")
                    append(item.body)
                }
            }.trimEnd()
        }
    }

    @JvmStatic
    fun buildSections(context: Context): List<LampsSdkDebugSection> {
        val app = context.applicationContext
        val config = SdkConfig.current
        val initData = config?.appInitData
        return listOf(
            section("SDK 状态") {
                line("sdkVersion", BuildConfig.SDK_VERSION)
                line("configInitialized", config != null)
                line("sdkReady", SdkRuntime.isReady())
                line("appInitDataLoaded", initData != null)
                SdkProviderRegistry.all().forEach { provider ->
                    line("${provider.name}Initialized", provider.isInitialized())
                }
            },
            section("SDK初始化入参") {
                line("appId", config?.appId.orEmpty())
                line("debug", config?.debug ?: false)
                line("initPangleSdk", config?.initPangleSdk ?: true)
                line("initYlhSdk", config?.initYlhSdk ?: true)
                line("initNoahSdk", config?.initNoahSdk ?: true)
                line("oaid", runCatching { config?.resolveOaid().orEmpty() }.getOrDefault(""))
                line("androidId", DeviceUtils.androidId(app))
                line("appVersion", DeviceUtils.appVersion(app))
                line("customData", config?.customData.orEmpty())
            },
            section("SDK初始化阶段耗时") {
                val timings = SdkInitMetrics.snapshots()
                if (timings.isEmpty()) {
                    appendLine("暂无初始化计时数据")
                } else {
                    timings.forEach { timing ->
                        appendLine("[${timing.name}]")
                        line("  start", formatTime(timing.startTimeMillis))
                        line("  end", timing.endTimeMillis?.let(::formatTime) ?: "-")
                        line("  duration", "${timing.durationMillis} ms")
                        line("  result", timing.result)
                    }
                }
                appendLine()
                appendLine("第三方 SDK:")
                val initSdkList = SdkInitDispatcher.getInitDataList()
                if (initSdkList.isEmpty()) {
                    appendLine("  暂无第三方 SDK 初始化数据")
                } else {
                    initSdkList.forEachIndexed { index, data ->
                        appendLine("  [${index}] ${data.provider.name}")
                        line("    channelName", data.channel.channelName)
                        line("    appId", data.appId)
                        line("    state", data.state.name)
                        line("    internalInit", config?.let { data.provider.shouldInitInternally(it) } ?: true)
                        line("    start", data.startTimeMillis?.let(::formatTime) ?: "-")
                        line("    end", data.endTimeMillis?.let(::formatTime) ?: "-")
                        line("    duration", data.durationMillis?.let { "$it ms" } ?: "-")
                        if (data.errorCode != null || !data.errorMessage.isNullOrEmpty()) {
                            line("    errorCode", data.errorCode ?: "-")
                            line("    errorMessage", data.errorMessage.orEmpty())
                        }
                    }
                }
            },
            section("服务端下发配置") {
                line("clientIp", initData?.clientIp.orEmpty())
                line("gameCenterPage", initData?.gameCenterPage.orEmpty())
                line("gamePlayPageTemplate", initData?.gamePlayPageTemplate.orEmpty())

                appendLine("rewardAdSlots:")
                if (initData?.rewardAdSlots.orEmpty().isEmpty()) {
                    appendLine("  []")
                } else {
                    initData?.rewardAdSlots?.forEachIndexed { index, slot ->
                        appendLine("  [$index]")
                        line("    slotId", slot.slotId)
                        line("    type", slot.type.name)
                        line("    price", slot.price)
                        line("    channelName", slot.channelName)
                        line("    channelId", slot.channelId)
                    }
                }
                appendLine("channelList:")
                if (initData?.channelList.orEmpty().isEmpty()) {
                    appendLine("  []")
                } else {
                    initData?.channelList?.forEachIndexed { index, channel ->
                        appendLine("  [$index]")
                        line("    channelName", channel.channelName)
                        line("    channelId", channel.channelId)
                        line("    channelAppId", channel.channelAppId)
                    }
                }
                appendLine("monitorLinks:")
                line("  rm", initData?.monitorLinks?.rm.orEmpty())
                line("  wm", initData?.monitorLinks?.wm.orEmpty())
                line("  pm", initData?.monitorLinks?.pm.orEmpty())
                line("  cm", initData?.monitorLinks?.cm.orEmpty())
                line("  dm", initData?.monitorLinks?.dm.orEmpty())
            },
            section("激励视频") {
                val rewardAds = SdkRewardDispatcher.getRewardDataList()
                if (rewardAds.isEmpty()) {
                    appendLine("暂无激励视频加载数据")
                } else {
                    rewardAds.forEachIndexed { index, ad ->
                        appendLine("[${index}] ${ad.channelName}")
                        line("  provider", ad.provider.name)
                        line("  slotId", ad.slotId)
                        line("  requestId", ad.requestId.ifEmpty { "-" })
                        line("  forwardSource", ad.forwardSource.ifEmpty { "-" })
                        line("  state", ad.state.name)
                        line("  slotType", ad.slot.type.name)
                        line("  price", ad.price)
                        line("  sdkPrice", ad.adData?.getPrice() ?: 0.0)
                        line("  slotPrice", ad.slot.price)
                        line("  hasAdData", ad.adData != null)
                        line("  isValid", ad.isValid)
                        line("  rewarded", ad.rewarded)
                        line("  loadStart", ad.loadStartTimeMillis?.let(::formatTime) ?: "-")
                        line("  loadEnd", ad.loadEndTimeMillis?.let(::formatTime) ?: "-")
                        line("  loadDuration", ad.loadDurationMillis?.let { "$it ms" } ?: "-")
                        line("  showStart", ad.showStartTimeMillis?.let(::formatTime) ?: "-")
                        line("  showEnd", ad.showEndTimeMillis?.let(::formatTime) ?: "-")
                        line("  showDuration", ad.showDurationMillis?.let { "$it ms" } ?: "-")
                        if (ad.errorCode != null || !ad.errorMessage.isNullOrEmpty()) {
                            line("  errorCode", ad.errorCode ?: "-")
                            line("  errorMessage", ad.errorMessage.orEmpty())
                        }
                    }
                }
            },
            monitorSection()
        )
    }

    private fun monitorSection(): LampsSdkDebugSection {
        val records = MonitorReportRecorder.snapshots()
        if (records.isEmpty()) {
            return LampsSdkDebugSection(
                title = "Monitor 上报",
                body = "暂无监测上报",
                expandedByDefault = false
            )
        }
        return LampsSdkDebugSection(
            title = "Monitor 上报",
            body = "",
            expandedByDefault = false,
            items = records.map { record ->
                val code = when {
                    !record.finished -> "-"
                    record.responseCode != null -> record.responseCode.toString()
                    else -> "error"
                }
                LampsSdkDebugItem(
                    id = "monitor-${record.id}",
                    title = "${record.event}  responseCode=$code  ${formatTime(record.startTimeMillis)}",
                    body = buildString {
                        line("event", record.event)
                        line("responseCode", record.responseCode ?: "-")
                        if (!record.error.isNullOrEmpty()) {
                            line("error", record.error)
                        }
                        line("url", record.url)
                    }.trimEnd()
                )
            }
        )
    }

    private fun section(
        title: String,
        build: StringBuilder.() -> Unit
    ): LampsSdkDebugSection {
        return LampsSdkDebugSection(
            title = title,
            body = buildString(build).trimEnd(),
            expandedByDefault = title in EXPANDED_TITLES
        )
    }

    private fun StringBuilder.line(name: String, value: Any?) {
        append(name).append(": ").appendLine(value)
    }

    private fun formatTime(timeMillis: Long): String {
        return SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
            .format(Date(timeMillis))
    }
}
