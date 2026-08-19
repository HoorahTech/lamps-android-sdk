package com.lamps.sdk.webview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.reward.RewardAdAbilityInstaller
import com.lamps.sdk.webview.bridge.LampsAbilityInstaller
import com.lamps.sdk.webview.bridge.LampsWebViewBridge

open class LampsWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.webViewStyle
) : WebView(context, attrs, defStyleAttr) {
    private val BRIDGE_NAME = "androidBridge"
    private val bridge = LampsWebViewBridge(this)

    init {
        initSettings()
        addJavascriptInterface(bridge, BRIDGE_NAME)
        bridge.registerAbilityInstaller(RewardAdAbilityInstaller)
        webViewClient = WebViewClient()
        webChromeClient = WebChromeClient()
    }

    fun registerAbilityInstaller(installer: LampsAbilityInstaller) {
        bridge.registerAbilityInstaller(installer)
    }

    /**
     * Dispatches a native event to HoorahBridge, with HupuBridge as a legacy fallback.
     */
    @JvmOverloads
    fun send(
        methodName: String,
        params: Any? = null,
        callback: ValueCallback<String>? = null
    ) {
        bridge.send(methodName, params, callback)
    }

    override fun destroy() {
        removeJavascriptInterface(BRIDGE_NAME)
        bridge.destroy()
        stopLoading()
        loadUrl("about:blank")
        clearHistory()
        removeAllViews()
        super.destroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initSettings() {
        setWebContentsDebuggingEnabled(LampsConfig.current?.debug == true)
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            loadsImagesAutomatically = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            allowFileAccess = false
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            textZoom = 100
        }
        CookieManager.getInstance().setAcceptCookie(true)
    }
}
