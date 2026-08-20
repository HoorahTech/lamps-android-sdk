package com.lamps.sdk.data.monitor

import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.data.monitor.MonitorConstant.FORWARD_SOURCE
import com.lamps.sdk.data.monitor.MonitorConstant.PRICE
import com.lamps.sdk.data.monitor.MonitorConstant.REQUEST_ID
import com.lamps.sdk.data.monitor.MonitorConstant.SLOT_ID
import com.lamps.sdk.data.monitor.MonitorConstant.UNION_NAME
import com.lamps.sdk.reward.LampsRewardAd

internal object MonitorReporter {
    fun reportRmSuccess(rewardData: LampsRewardAd) {
        val config = LampsConfig.current ?: return
        val rmList = config.appInitData?.monitorLinks?.rm
        MonitorUtil.report(rmList, reportValues(rewardData))
    }

    fun reportRmFail(rewardData: LampsRewardAd) {
        val config = LampsConfig.current ?: return
        val rmList = config.appInitData?.monitorLinks?.rm
        MonitorUtil.report(rmList, reportValues(rewardData))
    }

    fun reportWm(rewardData: LampsRewardAd) {
        val config = LampsConfig.current ?: return
        val wmList = config.appInitData?.monitorLinks?.wm
        MonitorUtil.report(wmList, reportValues(rewardData))
    }

    fun reportPm(rewardData: LampsRewardAd) {
        val config = LampsConfig.current ?: return
        val pmList = config.appInitData?.monitorLinks?.pm
        MonitorUtil.report(pmList, reportValues(rewardData))
    }

    fun reportCm(rewardData: LampsRewardAd) {
        val config = LampsConfig.current ?: return
        val cmList = config.appInitData?.monitorLinks?.cm
        MonitorUtil.report(cmList, reportValues(rewardData))
    }

    fun reportDm(rewardData: LampsRewardAd) {
        val config = LampsConfig.current ?: return
        val dmList = config.appInitData?.monitorLinks?.dm
        MonitorUtil.report(dmList, reportValues(rewardData))
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
