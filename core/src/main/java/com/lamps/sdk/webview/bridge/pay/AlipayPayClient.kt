package com.lamps.sdk.webview.bridge.pay

import android.app.Activity
import java.lang.reflect.InvocationTargetException

internal data class AlipayPayRawResult(
    val resultStatus: String,
    val result: String,
    val memo: String
)

/**
 * Calls Alipay App Pay through reflection. This module does not depend on the Alipay SDK;
 * [pay] only works when the host APK already contains [PAY_TASK_CLASS].
 */
internal object AlipayPayClient {
    const val PAY_TASK_CLASS = "com.alipay.sdk.app.PayTask"

    fun isAvailable(classLoader: ClassLoader): Boolean {
        return runCatching {
            Class.forName(PAY_TASK_CLASS, false, classLoader)
        }.isSuccess
    }

    fun pay(activity: Activity, orderInfo: String): AlipayPayRawResult {
        val clazz = Class.forName(PAY_TASK_CLASS, true, activity.classLoader)
        val task = clazz.getConstructor(Activity::class.java).newInstance(activity)
        val method = clazz.getMethod(
            "payV2",
            String::class.java,
            Boolean::class.javaPrimitiveType
        )
        val raw = try {
            method.invoke(task, orderInfo, true)
        } catch (error: InvocationTargetException) {
            throw error.targetException ?: error
        }
        val result = raw as? Map<*, *>
            ?: throw IllegalStateException("alipay payV2 returned empty result")
        return AlipayPayRawResult(
            resultStatus = result["resultStatus"]?.toString()?.trim().orEmpty(),
            result = result["result"]?.toString().orEmpty(),
            memo = result["memo"]?.toString().orEmpty()
        )
    }
}
