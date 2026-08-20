package com.lamps.sdk.utils

import android.util.Log
import com.lamps.sdk.config.LampsConfig

internal object SdkLog {
    private const val TAG = "LampsSdk"
    private val JSON_SECRET = Regex("\"(rewardSignKey)\"\\s*:\\s*\"(?:\\\\.|[^\"\\\\])*\"")
    private val QUERY_SECRET = Regex("(rewardSignKey)=([^&\\s]*)", RegexOption.IGNORE_CASE)

    /** 仅 debug 包 / setDebug(true) 时输出。 */
    fun d(message: String) {
        if (LampsConfig.current?.debug == true) {
            Log.d(TAG, redactSecrets(message))
        }
    }

    /** 与 debug 开关无关。 */
    fun w(message: String, throwable: Throwable? = null) {
        val safe = redactSecrets(message)
        if (throwable == null) {
            Log.w(TAG, safe)
        } else {
            Log.w(TAG, safe, redactThrowable(throwable))
        }
    }

    /** 与 debug 开关无关。 */
    fun e(message: String, throwable: Throwable? = null) {
        val safe = redactSecrets(message)
        if (throwable == null) {
            Log.e(TAG, safe)
        } else {
            Log.e(TAG, safe, redactThrowable(throwable))
        }
    }

    private fun redactSecrets(message: String): String {
        return QUERY_SECRET.replace(
            JSON_SECRET.replace(message, """"$1":"***""""),
            "$1=***"
        )
    }

    private fun redactThrowable(throwable: Throwable): Throwable {
        val redacted = Throwable(
            redactSecrets("${throwable.javaClass.simpleName}: ${throwable.message.orEmpty()}")
        )
        redacted.stackTrace = throwable.stackTrace
        return redacted
    }
}
