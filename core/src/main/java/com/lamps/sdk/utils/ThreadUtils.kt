package com.lamps.sdk.utils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

internal object ThreadUtils {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val workExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "lamps-sdk").apply { isDaemon = true }
    }

    fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    fun runOnWork(block: () -> Unit) {
        workExecutor.execute(block)
    }
}
