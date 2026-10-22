package com.lamps.sdk.webview

import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.lamps.sdk.utils.SdkLog
import com.lamps.sdk.webview.bridge.LampsAbility
import com.lamps.sdk.webview.bridge.LampsAbilityInstaller
import com.lamps.sdk.webview.bridge.LampsNativeCallback
import com.lamps.sdk.webview.bridge.bridgeReady.BridgeReadyAbility
import com.lamps.sdk.webview.view.LoadErrorView
import org.json.JSONObject

/**
 * 在 [container] 上盖一层兜底页。
 *
 * 主文档或 js/css 失败立即进入失败态，只能靠重试解除（Chromium 还会为错误页再回调一次加载开始和完成）。
 * 超过 [TIMEOUT_MS] 仍未完成只先盖上，不中断；加载完成或 H5 调 `lamps.common.bridgeReady` 后收起。
 */
internal class PageLoadGuard(
    private val webView: LampsWebView,
    private val container: FrameLayout,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable {
        SdkLog.w("web page load timeout url=$pageUrl")
        showError()
    }
    private var phase = Phase.IDLE
    private var pageUrl: String? = null
    private var errorView: LoadErrorView? = null
    private var destroyed = false

    init {
        // 自带 client 只服务内嵌模式的横滑嵌套，整页里替换不影响功能。
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (url != null && phase == Phase.IDLE) begin(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (phase == Phase.LOADING) dismiss()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request != null && phase == Phase.LOADING && request.isCritical()) {
                    fail(request.url, error?.description)
                }
            }
        }
        // 同名能力会替换掉先注册的 BridgeReadyAbility，这里包一层，先收起兜底页再交给原实现。
        webView.registerAbilityInstaller(object : LampsAbilityInstaller() {
            override fun createAbilities(): Array<LampsAbility> = arrayOf(ReadySignal())
        })
    }

    fun load(url: String) {
        begin(url)
        webView.loadUrl(url)
    }

    fun destroy() {
        destroyed = true
        handler.removeCallbacksAndMessages(null)
    }

    private fun begin(url: String) {
        if (destroyed) return
        phase = Phase.LOADING
        pageUrl = url
        errorView?.setLoading(true)
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, TIMEOUT_MS)
    }

    /** 超时只展示、不改状态，所以加载完成时仍走这里收起。 */
    private fun dismiss() {
        if (destroyed) return
        phase = Phase.IDLE
        handler.removeCallbacks(timeout)
        errorView?.let(container::removeView)
        errorView = null
    }

    private fun fail(url: Uri?, description: CharSequence?) {
        SdkLog.w("web page load failed url=$url error=$description")
        phase = Phase.FAILED
        handler.removeCallbacks(timeout)
        showError()
    }

    private fun showError() {
        if (destroyed) return
        errorView?.let {
            it.setLoading(false)
            return
        }
        val view = LoadErrorView(webView.context, webView.night) {
            pageUrl?.let(::load)
        }
        container.addView(view, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        errorView = view
    }

    private fun WebResourceRequest.isCritical(): Boolean {
        if (isForMainFrame) return true
        val path = url.path?.lowercase() ?: return false
        return path.endsWith(".js") || path.endsWith(".css")
    }

    private inner class ReadySignal(
        private val origin: LampsAbility = BridgeReadyAbility(),
    ) : LampsAbility by origin {
        override fun executeAsync(
            webView: LampsWebView,
            methodName: String,
            params: JSONObject,
            callbackId: String?,
            callback: LampsNativeCallback
        ) {
            dismiss()
            origin.executeAsync(webView, methodName, params, callbackId, callback)
        }
    }

    private enum class Phase { IDLE, LOADING, FAILED }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
