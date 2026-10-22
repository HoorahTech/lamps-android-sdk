package com.lamps.sdk.pangle

import android.os.Handler
import android.os.Looper
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.SlotType
import com.bytedance.sdk.openadsdk.TTAdLoadType
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.lamps.sdk.BuildConfig
import com.lamps.sdk.config.SdkConfig
import com.lamps.sdk.utils.HttpUtils
import com.lamps.sdk.utils.SdkLog
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/** 穿山甲服务端竞价结果。[price] 单位：分。 */
internal class TTServerBidAd(
    val adm: String,
    val price: Double
)

/**
 * 穿山甲激励视频服务端竞价。先取 SDK bidding token，再请求服务端拿 adm。
 *
 * 不走埋点、初始化共用的单线程，排队会吃掉竞价超时。
 */
internal object TTRewardServerBidder {
    private const val BID_PATH = "/api/v1/advertisement/bid"
    private const val BID_TIMEOUT_MS = 3_000L
    private const val HTTP_TIMEOUT_MS = 1_500

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "lamps-tt-bid").apply { isDaemon = true }
    }

    /** [callback] 回到主线程。超时或 adm 为空都视为竞价失败，且只回调一次。 */
    fun requestBid(
        requestId: String,
        slotId: String,
        callback: (Result<TTServerBidAd>) -> Unit
    ) {
        val finished = AtomicBoolean(false)
        val timeout = Runnable {
            if (!finished.compareAndSet(false, true)) return@Runnable
            SdkLog.w("pangle server bid timed out, slotId=$slotId")
            callback(Result.failure(TimeoutException("Pangle server bid timed out")))
        }
        mainHandler.postDelayed(timeout, BID_TIMEOUT_MS)
        executor.execute {
            val result = runCatching {
                val token = getToken(slotId)
                check(token.isNotEmpty()) { "bidding token is empty" }
                getAdmFromNet(requestId, slotId, token).getOrThrow()
            }
            if (!finished.compareAndSet(false, true)) return@execute
            mainHandler.removeCallbacks(timeout)
            result.onFailure { error ->
                SdkLog.w("pangle server bid failed, slotId=$slotId: ${error.message}")
            }
            mainHandler.post { callback(result) }
        }
    }

    private fun getToken(slotId: String): String {
        val adSlot = AdSlot.Builder()
            .setCodeId(slotId)
            .setAdLoadType(TTAdLoadType.LOAD)
            .build()
        return TTAdSdk.getAdManager()
            .getBiddingToken(adSlot, false, SlotType.TYPE_REWARD_VIDEO)
            ?.trim()
            .orEmpty()
    }

    private fun getAdmFromNet(requestId: String, slotId: String, token: String): Result<TTServerBidAd> {
        val config = SdkConfig.current
            ?: return Result.failure(IllegalStateException("sdk config is null"))
        val body = JSONObject()
            .put("os", "Android")
            .put("version", BuildConfig.SDK_VERSION)
            .put("appid", config.appId)
            .put("requestId", requestId)
            .put("ttToken", token)
            .put("slotId", slotId)
            .put("placementType", 20)
            .toString()
        return HttpUtils.request(
            method = "POST",
            path = BID_PATH,
            query = emptyMap(),
            body = body,
            headers = HttpUtils.jsonHeaders(),
            connectTimeoutMs = HTTP_TIMEOUT_MS,
            readTimeoutMs = HTTP_TIMEOUT_MS
        ).mapCatching { response ->
            check(response.isSuccessful) { "http ${response.code}: ${response.body}" }
            parse(response.body)
        }
    }

    private fun parse(body: String): TTServerBidAd {
        val ads = JSONObject(body).optJSONObject("data")?.optJSONObject("ads")
        val adm = ads?.optString("adm").orEmpty()
        check(adm.isNotBlank()) { "server bid no fill: $body" }
        val price = when (val value = ads?.opt("price")) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }.coerceAtLeast(0.0)
        return TTServerBidAd(adm = adm, price = price)
    }
}
