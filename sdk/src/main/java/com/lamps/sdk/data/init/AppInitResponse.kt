package com.lamps.sdk.data.init

internal class AppInitResponse(
    val token: String,
    val clientIp: String,
    val rewardAdSlots: List<RewardSlotResponse>,
    val monitorLinks: MonitorLinksResponse,
    val rewardSignKey: String
) {
    override fun toString(): String {
        return "AppInitResponse(token=$token, clientIp=$clientIp, " +
            "rewardAdSlots=$rewardAdSlots, monitorLinks=$monitorLinks)"
    }
}

internal class RewardSlotResponse(
    val slotId: String,
    val type: String,
    val channelName: String,
    val channelId: String,
    val appId: String
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
