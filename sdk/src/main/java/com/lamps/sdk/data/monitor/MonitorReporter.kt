package com.lamps.sdk.data.monitor

import com.lamps.sdk.config.LampsConfig
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

internal object MonitorReporter {
    fun reportRmSuccess(rewardData: LampsRewardAd) {
        safeReport {
            val config = LampsConfig.current ?: return
            val rmList = config.appInitData?.monitorLinks?.rm
            MonitorUtil.report(
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
            val config = LampsConfig.current ?: return
            val rmList = config.appInitData?.monitorLinks?.rm
            MonitorUtil.report(
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
            val config = LampsConfig.current ?: return
            val wmList = config.appInitData?.monitorLinks?.wm
            MonitorUtil.report("WM", wmList, reportValues(rewardData))
        }
    }

    fun reportPm(rewardData: LampsRewardAd) {
        safeReport {
            val config = LampsConfig.current ?: return
            val pmList = config.appInitData?.monitorLinks?.pm
            MonitorUtil.report("PM", pmList, reportValues(rewardData), needSign = true)
        }
    }

    fun reportCm(rewardData: LampsRewardAd) {
        safeReport {
            val config = LampsConfig.current ?: return
            val cmList = config.appInitData?.monitorLinks?.cm
            MonitorUtil.report("CM", cmList, reportValues(rewardData))
        }
    }

    fun reportDm(rewardData: LampsRewardAd) {
        safeReport {
            val config = LampsConfig.current ?: return
            val dmList = config.appInitData?.monitorLinks?.dm
            MonitorUtil.report(
                "DM",
                dmList,
                reportValues(rewardData) + (ACTION to ACTION_REWARD_SUCCESS),
                needSign = true
            )
        }
    }

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
}
