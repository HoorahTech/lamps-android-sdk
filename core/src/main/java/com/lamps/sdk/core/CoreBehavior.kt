package com.lamps.sdk.core

import org.json.JSONObject

/**
 * 行为回调数据。[event] 为 `OPEN` / `CLOSE`，[pageType] 为 `GAME` / `GAME_CENTER`，
 * 与 sdk 模块的同名枚举一一对应。
 */
class CoreBehavior(
    val event: String,
    val data: JSONObject,
    val pageType: String,
    val trackCustomData: Map<String, String>,
)
