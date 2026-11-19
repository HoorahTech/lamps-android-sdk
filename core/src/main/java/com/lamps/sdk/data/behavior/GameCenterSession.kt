package com.lamps.sdk.data.behavior

/**
 * 一次整页游戏中心从打开到关闭。时长只算游戏中心自身，不累加其中的游戏；
 * 期间玩过的游戏记在 [games]，同一游戏多局合并为一条。
 */
internal class GameCenterSession(
    val trackCustomData: Map<String, String> = emptyMap(),
) {
    private val openedAt = ForegroundClock.now()
    private val gamesById = LinkedHashMap<String, GameSession>()

    val games: Collection<GameSession>
        get() = gamesById.values

    fun record(session: GameSession) {
        gamesById.getOrPut(session.id) { GameSession(session.id) }.add(session)
    }

    fun elapsedSeconds(): Long = ForegroundClock.secondsSince(openedAt)
}
