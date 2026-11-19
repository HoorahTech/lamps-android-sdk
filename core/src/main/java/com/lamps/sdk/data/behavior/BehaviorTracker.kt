package com.lamps.sdk.data.behavior

import com.lamps.sdk.core.CoreBehavior
import com.lamps.sdk.core.CoreBehaviorCallback
import com.lamps.sdk.utils.SdkLog
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 单个游戏、整页游戏中心的打开 / 退出回传。内嵌游戏中心不回传。
 *
 * 页面创建时调 open，页面销毁时调 close；时长按 [ForegroundClock] 计。
 * 游戏关闭时，本局数据同时记到正在访问的整页游戏中心，游戏中心退出时一起带上。
 */
internal object BehaviorTracker {
    private val listeners = CopyOnWriteArrayList<Listener>()

    /** 正在访问的整页游戏中心；游戏数据记到最近打开的那个。只在主线程读写。 */
    private val centers = ArrayList<GameCenterSession>()

    fun addListener(owner: Any, callback: CoreBehaviorCallback) {
        if (listeners.any { it.owner === owner }) return
        listeners.add(Listener(owner, callback))
    }

    fun removeListener(owner: Any) {
        listeners.removeAll { it.owner === owner }
    }

    fun openGame(gameId: String, trackCustomData: Map<String, String> = emptyMap()): GameSession {
        val data = copyData(trackCustomData)
        emitOpen(BehaviorPageType.GAME, gameId, trackCustomData = data)
        return GameSession(gameId, data)
    }

    fun closeGame(session: GameSession) {
        session.finish()
        centers.lastOrNull()?.record(session)
        emitClose(BehaviorPageType.GAME, session.duration, listOf(session), session.trackCustomData)
    }

    fun gameOpenFailed(
        gameId: String,
        code: Int,
        msg: String,
        trackCustomData: Map<String, String> = emptyMap(),
    ) {
        emitOpen(BehaviorPageType.GAME, gameId, code, msg, copyData(trackCustomData))
    }

    fun openCenter(trackCustomData: Map<String, String> = emptyMap()): GameCenterSession {
        val session = GameCenterSession(copyData(trackCustomData))
        emitOpen(
            BehaviorPageType.GAME_CENTER,
            gameId = null,
            trackCustomData = session.trackCustomData,
        )
        centers.add(session)
        return session
    }

    fun closeCenter(session: GameCenterSession) {
        if (!centers.remove(session)) return
        emitClose(
            BehaviorPageType.GAME_CENTER,
            session.elapsedSeconds(),
            session.games,
            session.trackCustomData,
        )
    }

    fun centerOpenFailed(
        code: Int,
        msg: String,
        trackCustomData: Map<String, String> = emptyMap(),
    ) {
        emitOpen(
            BehaviorPageType.GAME_CENTER,
            gameId = null,
            code = code,
            msg = msg,
            trackCustomData = copyData(trackCustomData),
        )
    }

    private fun emitOpen(
        pageType: BehaviorPageType,
        gameId: String?,
        code: Int? = null,
        msg: String? = null,
        trackCustomData: Map<String, String> = emptyMap(),
    ) {
        emit(
            BehaviorEvent.OPEN,
            pageType,
            JSONObject()
                .put("type", pageType.value)
                .put("status", if (code == null) "成功" else "失败")
                .putOpt("id", gameId)
                .putOpt("code", code)
                .putOpt("msg", msg)
                .apply { trackCustomData.forEach { (key, value) -> put(key, value) } },
            trackCustomData,
        )
    }

    private fun emitClose(
        pageType: BehaviorPageType,
        duration: Long,
        games: Collection<GameSession>,
        trackCustomData: Map<String, String>,
    ) {
        emit(
            BehaviorEvent.CLOSE,
            pageType,
            JSONObject()
                .put("type", pageType.value)
                .put("duration", duration)
                .put("game_list", JSONArray(games.map(GameSession::toJson)))
                .apply { trackCustomData.forEach { (key, value) -> put(key, value) } },
            trackCustomData,
        )
    }

    private fun copyData(trackCustomData: Map<String, String>): Map<String, String> = LinkedHashMap(trackCustomData)

    /** 每个回调各拿一份 [data] 和 [trackCustomData] 副本，接入方修改不会影响其他回调。 */
    private fun emit(
        event: BehaviorEvent,
        pageType: BehaviorPageType,
        data: JSONObject,
        trackCustomData: Map<String, String>,
    ) {
        val payload = data.toString()
        SdkLog.d("behavior $event: $payload")
        listeners.forEach { listener ->
            runCatching {
                listener.callback.onBehavior(
                    CoreBehavior(
                        event = event.name,
                        data = JSONObject(payload),
                        pageType = pageType.name,
                        trackCustomData = LinkedHashMap(trackCustomData),
                    )
                )
            }.onFailure { error ->
                SdkLog.e("behavior callback failed: ${error.message}", error)
            }
        }
    }

    private class Listener(
        val owner: Any,
        val callback: CoreBehaviorCallback
    )
}
