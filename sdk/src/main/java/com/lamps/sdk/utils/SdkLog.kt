package com.lamps.sdk.utils

import android.util.Log
import com.lamps.sdk.config.LampsConfig

internal object SdkLog {
    private const val TAG = "LampsSdk"

    /** 仅 debug 包 / setDebug(true) 时输出。 */
    fun d(message: String) {
        if (LampsConfig.current?.debug == true) {
            Log.d(TAG, message)
        }
    }


    /** 与 debug 开关无关。 */
    fun w(message: String, throwable: Throwable? = null) {
        if (throwable == null) {
            Log.w(TAG, message)
        } else {
            Log.w(TAG, message, throwable)
        }
    }

    /** 与 debug 开关无关。 */
    fun e(message: String, throwable: Throwable? = null) {
        if (throwable == null) {
            Log.e(TAG, message)
        } else {
            Log.e(TAG, message, throwable)
        }
    }
}
