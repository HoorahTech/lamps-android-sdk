package com.lamps.sdk.webview.bridge.back

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.lamps.sdk.webview.LampsWebView
import org.json.JSONObject
import java.util.Collections
import java.util.WeakHashMap

internal object BackAbilityUtil {
    private val h5BackActivities = Collections.newSetFromMap(WeakHashMap<Activity, Boolean>())

    fun closePage(webView: LampsWebView) {
        findActivity(webView.context)?.finish()
    }

    fun markH5Back(webView: LampsWebView) {
        findActivity(webView.context)?.let(h5BackActivities::add)
    }

    fun handleBackPressed(activity: Activity, webView: LampsWebView): Boolean {
        if (h5BackActivities.contains(activity)) {
            webView.send(BackAbility.SEND_BACK, JSONObject())
            return true
        }
        if (webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return false
    }

    private fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            val base = current.baseContext
            if (base === current) break
            current = base
        }
        return current as? Activity
    }
}
