package com.lamps.sdk.data.behavior

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.SystemClock

/**
 * 进程在前台的累计毫秒数。所有 Activity 都 stop（退后台、熄屏）时停表，其余时间一直走。
 * 只在主线程读写。
 */
internal object ForegroundClock : Application.ActivityLifecycleCallbacks {
    /** 按实例记录而不是计数：注册前就已 start 的 Activity 之后 stop 时不会把计数减成负数。 */
    private val started = HashSet<Activity>()
    private var accumulated = 0L
    private var resumedAt = 0L
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        val app = context.applicationContext as? Application ?: return
        app.registerActivityLifecycleCallbacks(this)
        installed = true
    }

    fun now(): Long {
        return if (started.isEmpty()) accumulated
        else accumulated + SystemClock.elapsedRealtime() - resumedAt
    }

    /** 从 [now] 返回的 [mark] 到现在的前台秒数。 */
    fun secondsSince(mark: Long): Long = (now() - mark) / 1000

    override fun onActivityStarted(activity: Activity) {
        if (started.isEmpty()) resumedAt = SystemClock.elapsedRealtime()
        started.add(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        if (started.remove(activity) && started.isEmpty()) {
            accumulated += SystemClock.elapsedRealtime() - resumedAt
        }
    }

    override fun onActivityDestroyed(activity: Activity) = onActivityStopped(activity)
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
