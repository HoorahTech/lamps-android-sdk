package com.lamps.sdk.core

import com.lamps.sdk.utils.SdkLog
import java.util.concurrent.atomic.AtomicReference

internal data class SdkUser(
    val userId: String,
    val nickname: String,
    val avatar: String,
)

/**
 * 进程内登录用户快照。不写磁盘，init 前后都可写。
 */
internal object SdkUserStore {
    private val current = AtomicReference<SdkUser?>()

    fun update(userId: String, nickname: String, avatar: String): Boolean {
        val id = userId.trim()
        if (id.isEmpty()) {
            SdkLog.e("updateUser ignored: userId is empty")
            return false
        }
        current.set(
            SdkUser(
                userId = id,
                nickname = nickname.trim(),
                avatar = avatar.trim(),
            )
        )
        return true
    }

    fun clear() {
        current.set(null)
    }

    fun current(): SdkUser? = current.get()
}
