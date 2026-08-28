package com.lamps.sdk.webview.bridge.track

import android.view.ViewTreeObserver
import com.lamps.sdk.webview.LampsWebView

internal class TrackAbilityUtil(
    private val report: (String, HashMap<String, Any>) -> Unit
) {
    private val pendingOnloadData = mutableListOf<PendingEvent>()
    private var observedWebView: LampsWebView? = null
    private var windowFocusListener: ViewTreeObserver.OnWindowFocusChangeListener? = null

    fun observe(webView: LampsWebView) {
        if (observedWebView === webView) return
        removeObserver()
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { visible ->
            if (visible) {
                refreshPendingTimestamp()
            } else {
                flush()
            }
        }
        webView.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        observedWebView = webView
        windowFocusListener = listener
    }

    fun cacheOnload(action: String, data: HashMap<String, Any>) {
        data[VT] = nowSeconds()
        pendingOnloadData += PendingEvent(action, data)
    }

    fun destroy() {
        removeObserver()
        flush(clear = true)
    }

    private fun refreshPendingTimestamp() {
        val timestamp = nowSeconds()
        pendingOnloadData.forEach { it.data[VT] = timestamp }
    }

    private fun flush(clear: Boolean = false) {
        pendingOnloadData.toList().forEach { report(it.action, it.data) }
        if (clear) {
            pendingOnloadData.clear()
        }
    }

    private fun removeObserver() {
        val webView = observedWebView
        val listener = windowFocusListener
        if (webView != null && listener != null && webView.viewTreeObserver.isAlive) {
            webView.viewTreeObserver.removeOnWindowFocusChangeListener(listener)
        }
        observedWebView = null
        windowFocusListener = null
    }

    private fun nowSeconds(): String = (System.currentTimeMillis() / 1000L).toString()

    private data class PendingEvent(
        val action: String,
        val data: HashMap<String, Any>
    )

    private companion object {
        const val VT = "vt"
    }
}
