package com.lamps.sdk.tools

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.lamps.sdk.BuildConfig
import com.lamps.sdk.LampsSdk
import com.lamps.sdk.R
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.core.SdkInitMetrics
import com.lamps.sdk.data.sdk.channel.NoahSdkManager
import com.lamps.sdk.data.sdk.channel.TTSdkManager
import com.lamps.sdk.data.sdk.channel.YLHSdkManager
import com.lamps.sdk.data.sdk.init.SdkInitDispatcher
import com.lamps.sdk.data.sdk.reward.SdkRewardDispatcher
import com.lamps.sdk.utils.DeviceUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LampsSdkToolsActivity : Activity() {
    private lateinit var infoView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createContentView())
        renderInfo()
    }

    override fun onResume() {
        super.onResume()
        if (::infoView.isInitialized) renderInfo()
    }

    private fun createContentView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        root.addView(
            createTitleBar(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
            )
        )

        infoView = TextView(this).apply {
            setTextColor(Color.rgb(32, 32, 32))
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setPadding(dp(16), dp(12), dp(16), dp(24))
        }
        root.addView(
            ScrollView(this).apply { addView(infoView) },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        return root
    }

    private fun createTitleBar(): View {
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.rgb(245, 245, 245))
            addView(
                ImageButton(this@LampsSdkToolsActivity).apply {
                    setImageResource(R.drawable.lamps_ic_arrow_back)
                    contentDescription = "返回"
                    setBackgroundColor(Color.TRANSPARENT)
                    setPadding(dp(14), dp(14), dp(14), dp(14))
                    setOnClickListener { finish() }
                },
                LinearLayout.LayoutParams(
                    dp(52),
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            addView(
                TextView(this@LampsSdkToolsActivity).apply {
                    text = "Lamps SDK Tools"
                    textSize = 17f
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            )
            addView(
                ImageButton(this@LampsSdkToolsActivity).apply {
                    setImageResource(R.drawable.lamps_ic_content_copy)
                    contentDescription = "复制"
                    setBackgroundColor(Color.TRANSPARENT)
                    setPadding(dp(14), dp(14), dp(14), dp(14))
                    setOnClickListener { copyInfo() }
                },
                LinearLayout.LayoutParams(
                    dp(52),
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
    }

    private fun copyInfo() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("Lamps SDK Tools", infoView.text)
        )
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
    }

    private fun renderInfo() {
        val config = LampsConfig.current
        val initData = config?.appInitData
        infoView.text = buildString {
            section("SDK 状态")
            line("sdkVersion", BuildConfig.SDK_VERSION)
            line("configInitialized", config != null)
            line("sdkReady", LampsSdk.isSdkReady())
            line("appInitDataLoaded", initData != null)
            line("pangleInitialized", TTSdkManager.isInitialized())
            line("ylhInitialized", YLHSdkManager.isInitialized())
            line("noahInitialized", NoahSdkManager.isInitialized())

            section("基础参数")
            line("appId", config?.appId.orEmpty())
            line("debug", config?.debug ?: false)
            line("oaid", runCatching { config?.resolveOaid().orEmpty() }.getOrDefault(""))
            line("androidId", DeviceUtils.androidId(application))
            line("appVersion", DeviceUtils.appVersion(application))
            line("customData", config?.customData.orEmpty())

            section("Application")
            line("class", application.javaClass.name)
            line("packageName", packageName)
            line(
                "label",
                applicationInfo.loadLabel(packageManager).toString()
            )
            line(
                "version",
                runCatching {
                    packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
                }.getOrDefault("")
            )

            section("接口 Init Data")
            line("token", initData?.token.orEmpty())
            line("clientIp", initData?.clientIp.orEmpty())

            appendLine("rewardAdSlots:")
            if (initData?.rewardAdSlots.isNullOrEmpty()) {
                appendLine("  []")
            } else {
                initData?.rewardAdSlots?.forEachIndexed { index, slot ->
                    appendLine("  [$index]")
                    line("    appId", slot.appId)
                    line("    slotId", slot.slotId)
                    line("    type", slot.type)
                    line("    channelName", slot.channelName)
                    line("    channelId", slot.channelId)
                }
            }

            appendLine("monitorLinks:")
            line("  rm", initData?.monitorLinks?.rm.orEmpty())
            line("  pm", initData?.monitorLinks?.pm.orEmpty())
            line("  cm", initData?.monitorLinks?.cm.orEmpty())
            line("  dm", initData?.monitorLinks?.dm.orEmpty())
            line("  wm", initData?.monitorLinks?.wm.orEmpty())

            section("初始化阶段耗时")
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

            val initSdkList = SdkInitDispatcher.getInitDataList()
            if (initSdkList.isEmpty()) {
                appendLine()
                appendLine("第三方 SDK:")
                appendLine("  暂无第三方 SDK 初始化数据")
            } else {
                appendLine()
                appendLine("第三方 SDK:")
                initSdkList.forEachIndexed { index, data ->
                    appendLine("  [${index}] ${data.provider.name}")
                    line("    channelName", data.slot.channelName)
                    line("    appId", data.slot.appId)
                    line("    state", data.state.name)
                    line("    start", data.startTimeMillis?.let(::formatTime) ?: "-")
                    line("    end", data.endTimeMillis?.let(::formatTime) ?: "-")
                    line("    duration", data.durationMillis?.let { "$it ms" } ?: "-")
                    if (data.errorCode != null || !data.errorMessage.isNullOrEmpty()) {
                        line("    errorCode", data.errorCode ?: "-")
                        line("    errorMessage", data.errorMessage.orEmpty())
                    }
                }
            }

            section("激励视频")
            val rewardAds = SdkRewardDispatcher.getRewardDataList()
            if (rewardAds.isEmpty()) {
                appendLine("暂无激励视频加载数据")
            } else {
                rewardAds.forEachIndexed { index, ad ->
                    appendLine("[${index}] ${ad.channelName}")
                    line("  provider", ad.provider.name)
                    line("  slotId", ad.slotId)
                    line("  state", ad.state.name)
                    line("  price", ad.price)
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
        }
    }

    private fun StringBuilder.section(title: String) {
        if (isNotEmpty()) appendLine()
        appendLine("========== $title ==========")
    }

    private fun StringBuilder.line(name: String, value: Any?) {
        append(name).append(": ").appendLine(value)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun formatTime(timeMillis: Long): String {
        return SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
            .format(Date(timeMillis))
    }
}
