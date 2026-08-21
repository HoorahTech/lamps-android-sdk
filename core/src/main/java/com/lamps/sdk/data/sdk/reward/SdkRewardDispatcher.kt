package com.lamps.sdk.data.sdk.reward

import android.app.Activity
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.data.init.RewardSlotResponse
import com.lamps.sdk.data.monitor.MonitorReporter
import com.lamps.sdk.data.sdk.channel.RewardAdSdkLoadCallback
import com.lamps.sdk.data.sdk.channel.RewardVideoAd
import com.lamps.sdk.data.sdk.provider.SdkProviderRegistry
import com.lamps.sdk.reward.LampsRewardAd
import com.lamps.sdk.reward.RewardAdLoadCallback
import com.lamps.sdk.reward.RewardAdShowCallback
import com.lamps.sdk.utils.ThreadUtils
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal object SdkRewardDispatcher {
    private val rewardDataList = CopyOnWriteArrayList<LampsRewardAd>()
    private val loading = AtomicBoolean(false)

    fun getRewardDataList(): List<LampsRewardAd> = rewardDataList.toList()

    fun loadReward(
        activity: Activity,
        config: LampsConfig,
        callback: RewardAdLoadCallback,
        forwardSource: String = ""
    ) {
        if (!loading.compareAndSet(false, true)) {
            callback.onAdLoadFailed(
                RewardAdErrorCode.FLOW_IN_PROGRESS,
                "reward ad load is in progress"
            )
            return
        }
        rewardDataList.clear()
        val slots = config.appInitData?.rewardAdSlots.orEmpty()
        if (slots.isEmpty()) {
            finishLoadValidationFailed(
                callback,
                RewardAdErrorCode.SLOT_NOT_FOUND,
                "reward ad slot is empty"
            )
            return
        }

        val validSlots = slots.filter { it.slotId.isNotBlank() && it.appId.isNotBlank() }
        if (validSlots.isEmpty()) {
            finishLoadValidationFailed(
                callback,
                RewardAdErrorCode.INVALID_SLOT,
                "slotId or appId is empty"
            )
            return
        }

        rewardDataList.addAll(
            createRewardDataList(
                validSlots,
                UUID.randomUUID().toString().replace("-", ""),
                forwardSource
            )
        )
        if (rewardDataList.isEmpty()) {
            finishLoadValidationFailed(
                callback,
                RewardAdErrorCode.PROVIDER_NOT_FOUND,
                "no provider supports the configured reward slots"
            )
            return
        }
        dispatchLoad(activity, rewardDataList, callback)
    }

    fun showReward(
        activity: Activity,
        rewardData: LampsRewardAd,
        callback: RewardAdShowCallback
    ) {
        val adData = rewardData.adData
        if (adData == null) {
            callback.onAdShowFailed(
                RewardAdErrorCode.AD_NOT_LOADED,
                "reward ad has not been loaded"
            )
            return
        }
        if (!rewardData.markShowing()) {
            callback.onAdShowFailed(
                RewardAdErrorCode.FLOW_IN_PROGRESS,
                "reward ad cannot be shown in state ${rewardData.state}"
            )
            return
        }
        ThreadUtils.runOnMain {
            runCatching {
                rewardData.provider.showReward(
                    activity,
                    adData,
                    createShowCallback(rewardData, callback)
                )
            }.onFailure { error ->
                rewardData.markShowFailed(
                    RewardAdErrorCode.PROVIDER_ERROR,
                    error.message
                )
                callback.onAdShowFailed(
                    RewardAdErrorCode.PROVIDER_ERROR,
                    error.message ?: "reward ad show failed"
                )
            }
        }
    }


    private fun createRewardDataList(
        slots: List<RewardSlotResponse>,
        requestId: String,
        forwardSource: String
    ): List<LampsRewardAd> {
        return slots.mapNotNull { slot ->
            SdkProviderRegistry.all().firstOrNull { it.supports(slot) }?.let { provider ->
                LampsRewardAd(provider, slot, requestId, forwardSource)
            }
        }
    }

    private fun dispatchLoad(
        activity: Activity,
        dataList: List<LampsRewardAd>,
        callback: RewardAdLoadCallback
    ) {
        val remaining = AtomicInteger(dataList.size)
        val finished = AtomicBoolean(false)

        ThreadUtils.runOnMain {
            dataList.forEach { rewardData ->
                val loadCallback = object : RewardAdSdkLoadCallback {
                    override fun onLoadSuccess(ad: RewardVideoAd) {
                        if (!rewardData.markLoadSuccess(ad)) return
                        MonitorReporter.reportRmSuccess(rewardData)
                        completeLoad(remaining, finished, callback)
                    }

                    override fun onLoadFailed(code: Int, message: String?, ad: RewardVideoAd?) {
                        if (!rewardData.markLoadFailed(code, message, ad)) return
                        MonitorReporter.reportRmFail(rewardData)
                        completeLoad(remaining, finished, callback)
                    }
                }
                if (!rewardData.markLoading()) {
                    completeLoad(remaining, finished, callback)
                    return@forEach
                }
                runCatching {
                    rewardData.provider.loadReward(
                        activity,
                        rewardData.slot,
                        loadCallback
                    )
                }.onFailure { error ->
                    loadCallback.onLoadFailed(
                        RewardAdErrorCode.PROVIDER_ERROR,
                        error.message ?: "reward ad load failed",
                        null
                    )
                }
            }
        }
    }



    private fun completeLoad(
        remaining: AtomicInteger,
        finished: AtomicBoolean,
        callback: RewardAdLoadCallback
    ) {
        if (remaining.decrementAndGet() != 0 || !finished.compareAndSet(false, true)) return
        ThreadUtils.runOnMain {
            loading.set(false)
            val winner = rewardDataList
                .filter { it.state == SdkRewardState.LOAD_SUCCESS }
                .maxWithOrNull(
                    compareBy<LampsRewardAd> { it.price }
                )
            if (winner == null) {
                val failures = rewardDataList.filter { it.state == SdkRewardState.LOAD_FAILED }
                callback.onAdLoadFailed(
                    RewardAdErrorCode.ALL_SDK_LOAD_FAILED,
                    buildString {
                        append("all reward ad SDKs failed to load")
                        failures.forEachIndexed { index, failure ->
                            if (index == 0) append(": ") else append("; ")
                            append(failure.channelName)
                            append("(")
                            append(failure.slotId)
                            append(") code=")
                            append(failure.errorCode)
                            append(", message=")
                            append(failure.errorMessage.orEmpty())
                        }
                    }
                )
                return@runOnMain
            }
            val bidItems = rewardDataList.toList()
            SdkProviderRegistry.all().forEach { provider ->
                provider.reportRewardBidding(bidItems, winner)
            }
            MonitorReporter.reportWm(winner)
            winner.markSelected()
            rewardDataList.forEach { candidate ->
                if (candidate !== winner) {
                    candidate.markBidFailed(
                        RewardAdErrorCode.BID_FAILED,
                        "reward ad lost bidding"
                    )
                }
            }
            callback.onAdLoadSuccess(winner)
        }
    }

    private fun createShowCallback(
        rewardData: LampsRewardAd,
        callback: RewardAdShowCallback
    ): RewardAdShowCallback {
        return object : RewardAdShowCallback {
            override fun onAdShown() {
                rewardData.markShown()
                MonitorReporter.reportPm(rewardData)
                callback.onAdShown()
            }

            override fun onAdRewarded() {
                rewardData.markRewarded()
                MonitorReporter.reportDm(rewardData)
                callback.onAdRewarded()
            }

            override fun onAdClosed() {
                rewardData.markClosed()
                callback.onAdClosed()
            }

            override fun onAdClicked() {
                MonitorReporter.reportCm(rewardData)
                callback.onAdClicked()
            }

            override fun onAdShowFailed(code: Int, message: String?) {
                rewardData.markShowFailed(code, message)
                callback.onAdShowFailed(code, message)
            }
        }
    }

    private fun finishLoadValidationFailed(
        callback: RewardAdLoadCallback,
        code: Int,
        message: String
    ) {
        loading.set(false)
        callback.onAdLoadFailed(code, message)
    }
}
