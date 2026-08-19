package com.lamps.sdk.data.sdk.channel

import com.qq.e.ads.rewardvideo.RewardVideoADListener
import com.qq.e.comm.util.AdError

internal open class YLHRewardVideoADListenerAdapter : RewardVideoADListener {
    override fun onADLoad() = Unit

    override fun onVideoCached() = Unit

    override fun onADShow() = Unit

    override fun onADExpose() = Unit

    override fun onReward(data: MutableMap<String, Any>?) = Unit

    override fun onADClick() = Unit

    override fun onVideoComplete() = Unit

    override fun onADClose() = Unit

    override fun onError(error: AdError?) = Unit
}
