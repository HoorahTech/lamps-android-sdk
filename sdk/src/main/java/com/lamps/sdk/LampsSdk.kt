package com.lamps.sdk

import android.app.Activity
import android.content.Context
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.core.InitCallback
import com.lamps.sdk.core.LampsErrorCode
import com.lamps.sdk.core.SdkRuntime
import com.lamps.sdk.reward.LampsRewardAd
import com.lamps.sdk.reward.RewardAdLoadCallback
import com.lamps.sdk.reward.RewardAdShowCallback

/**
 * Lamps SDK 门面。调用顺序对齐穿山甲：先 [init] 缓存配置，再 [start] 完成启动。
 *
 * 必须在用户同意隐私协议之后调用。OAID 由媒体传入，SDK 不采集、不内置 MSA 证书。
 */
object LampsSdk {

    /**
     * 仅校验并缓存配置，不发起网络请求。
     * 重复调用以第一次为准。
     *
     * @return true 表示配置已接受；false 表示 appId / oaidProvider 缺失或 Context 非法
     */
    @JvmStatic
    fun init(context: Context, config: LampsConfig): Boolean {
        return SdkRuntime.init(context, config)
    }

    /**
     * 读取 OAID 并启动 SDK。成功前不要请求广告能力。
     * OAID 为空时回调 [LampsErrorCode.OAID_EMPTY]。
     */
    @JvmStatic
    fun startAsync(callback: InitCallback) {
        SdkRuntime.startAsync(callback)
    }

    @JvmStatic
    fun isSdkReady(): Boolean = SdkRuntime.isReady()

    /**
     * 并发加载所有受支持的激励视频广告位，完成竞价后缓存最高价广告。
     */
    @JvmStatic
    fun loadReward(activity: Activity, callback: RewardAdLoadCallback) {
        SdkRuntime.loadReward(activity, callback)
    }

    /**
     * 展示 [loadReward] 回调给出的激励视频。也可直接调用 [LampsRewardAd.show]。
     */
    @JvmStatic
    fun showReward(activity: Activity, ad: LampsRewardAd, callback: RewardAdShowCallback) {
        SdkRuntime.showReward(activity, ad, callback)
    }

    /**
     * 更新已缓存配置。目前用于后续 [LampsConfig.Builder.setCustomData] 等增量字段，不改变 appId。
     */
    @JvmStatic
    fun updateConfig(config: LampsConfig) {
        SdkRuntime.updateConfig(config)
    }

    @JvmStatic
    fun getSdkVersion(): String = BuildConfig.SDK_VERSION
}
