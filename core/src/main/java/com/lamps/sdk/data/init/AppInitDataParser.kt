package com.lamps.sdk.data.init

import org.json.JSONArray
import org.json.JSONObject

internal object AppInitDataParser {

    fun parse(body: String): Result<AppInitResponse> {
        return try {
            val json = JSONObject(body)
            val code = json.optInt("code", -1)
            val message = json.optString("message")
            if (code != 0) {
                return Result.failure(
                    IllegalStateException(message.ifBlank { "appInitData request failed, code=$code" })
                )
            }
            val data = json.optJSONObject("data")
                ?: return Result.failure(IllegalStateException("data is empty"))
            val appInitData = AppInitResponse(
                token = data.optString("token"),
                clientIp = data.optString("clientIp"),
                channelList = parseChannelList(data.optJSONArray("channelList")),
                rewardAdSlots = parseSlots(data.optJSONArray("rewardAdSlots")),
                monitorLinks = parseMonitorLinks(data.optJSONObject("monitorLinks"))
            )
            Result.success(appInitData)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun parseSlots(array: JSONArray?): List<RewardSlotResponse> {
        if (array == null) return emptyList()
        val list = ArrayList<RewardSlotResponse>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            list.add(
                RewardSlotResponse(
                    slotId = item.optString("slotId"),
                    type = RewardSlotType.from(item.optString("type")),
                    price = parsePrice(item),
                    channelName = item.optString("channelName"),
                    channelId = item.optString("channelId")
                )
            )
        }
        return list
    }

    private fun parseChannelList(array: JSONArray?): List<ChannelInfoResponse> {
        if (array == null) return emptyList()
        val list = ArrayList<ChannelInfoResponse>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            list.add(
                ChannelInfoResponse(
                    channelName = item.optString("channelName"),
                    channelId = item.optString("channelId"),
                    channelAppId = item.optString("channelAppId")
                )
            )
        }
        return list
    }

    private fun parseMonitorLinks(obj: JSONObject?): MonitorLinksResponse {
        val rem = stringList(obj?.optJSONArray("rem")).ifEmpty {
            stringList(obj?.optJSONArray("dm"))
        }
        return MonitorLinksResponse(
            rm = stringList(obj?.optJSONArray("rm")),
            pm = stringList(obj?.optJSONArray("pm")),
            cm = stringList(obj?.optJSONArray("cm")),
            wm = stringList(obj?.optJSONArray("wm")),
            dm = rem
        )
    }

    private fun parsePrice(item: JSONObject): Double {
        if (!item.has("price") || item.isNull("price")) return 0.0
        return when (val value = item.opt("price")) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }.coerceAtLeast(0.0)
    }

    private fun stringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = ArrayList<String>(array.length())
        for (i in 0 until array.length()) {
            val value = array.optString(i)
            if (value.isNotEmpty()) {
                list.add(value)
            }
        }
        return list
    }
}
