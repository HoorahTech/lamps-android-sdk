package com.lamps.sdk.data.behavior

import org.json.JSONObject

/**
 * 一个游戏的使用数据：游玩时长，以及激励视频的请求、曝光、点击和 eCPM。
 * 游戏页打开时创建并开始计时，关闭时 [finish] 写入时长；游戏中心也用它按游戏汇总多局。
 */
internal class GameSession(
    val id: String,
    val trackCustomData: Map<String, String> = emptyMap(),
) {
    private val openedAt = ForegroundClock.now()

    /** 游玩时长，秒。 */
    var duration = 0L
    var adReq = 0
    var adShow = 0
    var adClk = 0

    /** 每次曝光的 eCPM 累加，单位分。 */
    var ecpmSum = 0.0

    fun finish() {
        duration = ForegroundClock.secondsSince(openedAt)
    }

    /** 把同一游戏另一局的数据累加进来。 */
    fun add(other: GameSession) {
        duration += other.duration
        adReq += other.adReq
        adShow += other.adShow
        adClk += other.adClk
        ecpmSum += other.ecpmSum
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("duration", duration)
        .put("ad_req", adReq)
        .put("ad_show", adShow)
        .put("ad_clk", adClk)
        .put("price", ecpmSum)
}
