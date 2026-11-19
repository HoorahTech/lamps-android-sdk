package com.lamps.sdk.core

/**
 * 单个游戏、整页游戏中心的打开 / 退出。内嵌游戏中心不回调。
 */
fun interface BehaviorCallback {
    fun onBehavior(behavior: Behavior)
}
