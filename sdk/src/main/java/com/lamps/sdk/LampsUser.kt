package com.lamps.sdk

/**
 * 媒体同步给 SDK 的当前登录用户。
 *
 * 每次 [LampsSdk.updateUser] 都按整份覆盖。空字符串表示该字段没有值。
 */
class LampsUser private constructor(
    val userId: String,
    val nickname: String,
    val avatar: String,
) {
    class Builder {
        private var userId: String = ""
        private var nickname: String = ""
        private var avatar: String = ""

        fun setUserId(userId: String?) = apply { this.userId = userId.orEmpty() }

        fun setNickname(nickname: String?) = apply { this.nickname = nickname.orEmpty() }

        fun setAvatar(avatar: String?) = apply { this.avatar = avatar.orEmpty() }

        fun build(): LampsUser = LampsUser(
            userId = userId,
            nickname = nickname,
            avatar = avatar,
        )
    }
}
