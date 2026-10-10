package com.lamps.sdk.data.monitor

import com.lamps.sdk.config.SdkConfig
import com.lamps.sdk.core.CoreRewardMonitorCallback
import com.lamps.sdk.data.monitor.MonitorConstant.ACTION
import com.lamps.sdk.data.monitor.MonitorConstant.ACTION_REWARD_SUCCESS
import com.lamps.sdk.data.monitor.MonitorConstant.CODE
import com.lamps.sdk.data.monitor.MonitorConstant.FORWARD_SOURCE
import com.lamps.sdk.data.monitor.MonitorConstant.IS_SUCCESS
import com.lamps.sdk.data.monitor.MonitorConstant.IS_SUCCESS_NO
import com.lamps.sdk.data.monitor.MonitorConstant.IS_SUCCESS_YES
import com.lamps.sdk.data.monitor.MonitorConstant.PRICE
import com.lamps.sdk.data.monitor.MonitorConstant.REQUEST_ID
import com.lamps.sdk.data.monitor.MonitorConstant.SLOT_ID
import com.lamps.sdk.data.monitor.MonitorConstant.UNION_NAME
import com.lamps.sdk.reward.LampsRewardAd
import com.lamps.sdk.utils.SdkLog
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList

internal object MonitorReporter {
    private val listeners = CopyOnWriteArrayList<Listener>()

    fun addListener(owner: Any, callback: CoreRewardMonitorCallback) {
        if (listeners.any { it.owner === owner }) return
        listeners.add(Listener(owner, callback))
    }

    fun removeListener(owner: Any) {
        listeners.removeAll { it.owner === owner }
    }
    fun reportRmSuccess(rewardData: LampsRewardAd) {
        safeReport {
            val config = SdkConfig.current ?: return
            val rmList = config.appInitData?.monitorLinks?.rm
            dispatchReport(
                "RM",
                rmList,
                reportValues(rewardData) + mapOf(
                    IS_SUCCESS to IS_SUCCESS_YES,
                    CODE to ""
                )
            )
        }
    }

    fun reportRmFail(rewardData: LampsRewardAd) {
        safeReport {
            val config = SdkConfig.current ?: return
            val rmList = config.appInitData?.monitorLinks?.rm
            dispatchReport(
                "RM",
                rmList,
                reportValues(rewardData) + mapOf(
                    IS_SUCCESS to IS_SUCCESS_NO,
                    CODE to rewardData.errorCode?.toString().orEmpty()
                )
            )
        }
    }

    fun reportWm(rewardData: LampsRewardAd) {
        safeReport {
            val config = SdkConfig.current ?: return
            val wmList = config.appInitData?.monitorLinks?.wm
            dispatchReport("WM", wmList, reportValues(rewardData))
        }
    }

    fun reportPm(rewardData: LampsRewardAd) {
        safeReport {
            val config = SdkConfig.current ?: return
            val pmList = config.appInitData?.monitorLinks?.pm
            dispatchReport("PM", pmList, reportValues(rewardData), needSign = true)
        }
    }

    fun reportCm(rewardData: LampsRewardAd) {
        safeReport {
            val config = SdkConfig.current ?: return
            val cmList = config.appInitData?.monitorLinks?.cm
            dispatchReport("CM", cmList, reportValues(rewardData))
        }
    }

    fun reportDm(rewardData: LampsRewardAd) {
        safeReport {
            val config = SdkConfig.current ?: return
            val dmList = config.appInitData?.monitorLinks?.dm
            dispatchReport(
                "DM",
                dmList,
                reportValues(rewardData) + (ACTION to ACTION_REWARD_SUCCESS),
                needSign = true
            )
        }
    }

    private fun dispatchReport(
        event: String,
        urls: List<String>?,
        values: Map<String, String>,
        needSign: Boolean = false
    ) {
        val payload = toPayload(values)
        listeners.forEach { listener ->
            runCatching { listener.callback.onRewardMonitor(event, payload) }
                .onFailure { error ->
                    SdkLog.e("reward monitor callback failed: ${error.message}", error)
                }
        }
        MonitorUtil.report(event, urls, values, needSign)
    }

    private fun toPayload(values: Map<String, String>): String {
        val json = JSONObject()
        PAYLOAD_FIELDS.forEach { (macro, name) ->
            if (values.containsKey(macro)) {
                json.put(name, values.getValue(macro))
            }
        }
        return json.toString()
    }

    private class Listener(
        val owner: Any,
        val callback: CoreRewardMonitorCallback
    )

    private inline fun safeReport(block: () -> Unit) {
        runCatching(block).onFailure { error ->
            SdkLog.w("monitor report failed: ${error.message}", error)
        }
    }

    private fun reportValues(ad: LampsRewardAd): Map<String, String> {
        return MonitorUtil.buildDefaultValues() + mapOf(
            REQUEST_ID to ad.requestId,
            FORWARD_SOURCE to ad.forwardSource,
            PRICE to ad.price.asMonitorPrice(),
            UNION_NAME to ad.channelName,
            SLOT_ID to ad.slotId
        )
    }

    private fun Double.asMonitorPrice(): String {
        return if (this % 1.0 == 0.0) toLong().toString() else toString()
    }

    private val PAYLOAD_FIELDS = listOf(
        MonitorConstant.TS to "ts",
        MonitorConstant.UA to "ua",
        MonitorConstant.IP to "ip",
        MonitorConstant.MAC to "mac",
        MonitorConstant.SW to "sw",
        MonitorConstant.SH to "sh",
        MonitorConstant.IMEI to "imei",
        MonitorConstant.OS to "os",
        MonitorConstant.ANDROID_ID to "androidId",
        MonitorConstant.OAID to "oaid",
        MonitorConstant.APP_ID to "appid",
        MonitorConstant.PACKAGE_NAME to "packageName",
        MonitorConstant.SDK_VERSION to "sdkVersion",
        MonitorConstant.PHONE_BRAND to "phoneBrand",
        MonitorConstant.NETWORK to "network",
        REQUEST_ID to "requestId",
        FORWARD_SOURCE to "forwardSource",
        PRICE to "price",
        UNION_NAME to "unionName",
        SLOT_ID to "slotId",
        IS_SUCCESS to "isSuccess",
        CODE to "code",
        ACTION to "action"
    )
}

/** Host registration entry. Listeners stay in [MonitorReporter]. */
object RewardMonitor {
    fun addListener(owner: Any, callback: CoreRewardMonitorCallback) {
        MonitorReporter.addListener(owner, callback)
    }

    fun removeListener(owner: Any) {
        MonitorReporter.removeListener(owner)
    }
}
