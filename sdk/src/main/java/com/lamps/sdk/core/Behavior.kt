package com.lamps.sdk.core

import org.json.JSONObject

/**
 * 一次打开或退出回调。
 *
 * [data] 是埋点参数，每个回调各拿一份副本。[trackCustomData] 是宿主传入的原始数据，
 * 其中每个字段也已经写进 [data]。
 */
class Behavior(
    val event: BehaviorEvent,
    val pageType: BehaviorPageType,
    val trackCustomData: Map<String, String>,
    val data: JSONObject,
)
