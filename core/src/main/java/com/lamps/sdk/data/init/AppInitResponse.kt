package com.lamps.sdk.data.init

internal class AppInitResponse(
    val token: String,
    val clientIp: String,
    val channelList: List<ChannelInfoResponse>,
    val rewardAdSlots: List<RewardSlotResponse>,
    val monitorLinks: MonitorLinksResponse
) {
    override fun toString(): String {
        return "AppInitResponse(token=$token, clientIp=$clientIp, " +
            "channelList=$channelList, rewardAdSlots=$rewardAdSlots, monitorLinks=$monitorLinks)"
    }
}

internal fun AppInitResponse.getChannelAppId(channelName: String): String? {
    return channelList.firstOrNull { it.channelName == channelName }?.channelAppId
}

enum class RewardSlotType {
    PD,
    BD;

    companion object {
        fun from(raw: String): RewardSlotType {
            return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: PD
        }
    }
}

class ChannelInfoResponse(
    val channelName: String,
    val channelId: String,
    val channelAppId: String
)

class RewardSlotResponse(
    val slotId: String,
    val type: RewardSlotType,
    val price: Double,
    val channelName: String,
    val channelId: String
)

internal class MonitorLinksResponse(
    val rm: List<String>,
    val pm: List<String>,
    val cm: List<String>,
    val wm: List<String>,
    val dm: List<String>
)

internal data class AppInitRequest(
    val androidId: String,
    val appId: String,
    val version: String,
    val oaid: String,
    val os: String
)

internal data class AppInitDataSnapshot(
    val data: AppInitResponse,
    val raw: String
) {
    override fun toString(): String = "AppInitDataSnapshot(data=$data)"
}
