package com.lamps.sdk.data.init

internal class AppInitResponse(
    val token: String,
    val clientIp: String,
    val rewardAdSlots: List<RewardSlotResponse>,
    val monitorLinks: MonitorLinksResponse
)

internal class RewardSlotResponse(
    val slotId: String,
    val type: String,
    val channelName: String,
    val channelId: String
)

internal class MonitorLinksResponse(
    val rm: List<String>,
    val pm: List<String>,
    val cm: List<String>,
    val dm: List<String>,
    val wm: List<String>
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
)
