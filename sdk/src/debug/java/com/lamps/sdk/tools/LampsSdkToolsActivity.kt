package com.lamps.sdk.tools

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.lamps.sdk.BuildConfig
import com.lamps.sdk.LampsSdk
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.core.SdkInitMetrics
import com.lamps.sdk.data.sdk.channel.NoahSdkManager
import com.lamps.sdk.data.sdk.channel.TTSdkManager
import com.lamps.sdk.data.sdk.channel.YLHSdkManager
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
                Button(this@LampsSdkToolsActivity).apply {
                    text = "返回"
                    setOnClickListener { finish() }
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
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
                Button(this@LampsSdkToolsActivity).apply {
                    text = "刷新"
                    setOnClickListener { renderInfo() }
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
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
