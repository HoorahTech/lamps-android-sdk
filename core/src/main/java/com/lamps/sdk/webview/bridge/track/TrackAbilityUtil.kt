package com.lamps.sdk.webview.bridge.track

import android.view.ViewTreeObserver
import com.lamps.sdk.webview.LampsWebView

internal class TrackAbilityUtil(
    private val report: (String, HashMap<String, Any>) -> Unit
) {
    private val pendingOnloadData = mutableListOf<PendingEvent>()
    private var observedWebView: LampsWebView? = null
    private var observedTreeObserver: ViewTreeObserver? = null
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
        val treeObserver = webView.viewTreeObserver
        treeObserver.addOnWindowFocusChangeListener(listener)
        observedWebView = webView
        observedTreeObserver = treeObserver
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
        pendingOnloadData.forEach {
            it.data[VT] = timestamp
            it.data.remove(LT)
            it.reported = false
        }
    }

    private fun flush(clear: Boolean = false) {
        val timestamp = nowSeconds()
        pendingOnloadData.filterNot { it.reported }.forEach {
            it.data[LT] = timestamp
            report(it.action, it.data)
            it.reported = true
        }
        if (clear) {
            pendingOnloadData.clear()
        }
    }

    private fun removeObserver() {
        val listener = windowFocusListener
        if (listener != null) {
            // 注册用的 ViewTreeObserver 可能是 attach 前的浮动实例，attach 时会被合并，
            // 所以注册时那个和当前那个都要摘一次。
            setOf(observedTreeObserver, observedWebView?.viewTreeObserver)
                .filterNotNull()
                .filter { it.isAlive }
                .forEach { it.removeOnWindowFocusChangeListener(listener) }
        }
        observedWebView = null
        observedTreeObserver = null
        windowFocusListener = null
    }

    private fun nowSeconds(): String = (System.currentTimeMillis() / 1000L).toString()

    private data class PendingEvent(
        val action: String,
        val data: HashMap<String, Any>,
        var reported: Boolean = false
    )

    private companion object {
        const val LT = "lt"
        const val VT = "vt"
    }
}
