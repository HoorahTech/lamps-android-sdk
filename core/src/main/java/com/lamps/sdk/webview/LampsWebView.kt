package com.lamps.sdk.webview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.lamps.sdk.BuildConfig
import com.lamps.sdk.config.SdkConfig
import com.lamps.sdk.data.sdk.reward.RewardAdAbilityInstaller
import com.lamps.sdk.webview.bridge.LampsAbilityInstaller
import com.lamps.sdk.webview.bridge.LampsWebViewBridge
import com.lamps.sdk.webview.bridge.CommonAbilityInstaller
import com.lamps.sdk.webview.bridge.nightmode.NightModeEvent
import org.json.JSONObject

open class LampsWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.webViewStyle
) : WebView(context, attrs, defStyleAttr) {
    private val BRIDGE_NAME = "androidBridge"
    private val bridge = LampsWebViewBridge(this)

    var displayMode: String = ""

    /**
     * Host day/night. Exposed to H5 as `night`: 1 night, 0 day.
     *
     * Set this before loading the page. For runtime changes use [updateNight] so the already
     * loaded page is notified as well.
     */
    var night: Boolean = false

    private var destroyed = false

    init {
        initSettings()
        addJavascriptInterface(bridge, BRIDGE_NAME)
        bridge.registerAbilityInstaller(RewardAdAbilityInstaller)
        bridge.registerAbilityInstaller(CommonAbilityInstaller())
        webViewClient = WebViewClient()
        webChromeClient = WebChromeClient()
    }

    fun registerAbilityInstaller(installer: LampsAbilityInstaller) {
        bridge.registerAbilityInstaller(installer)
    }

    /**
     * Updates day/night at runtime.
     *
     * Stores the value so a later `lamps.common.bridgeReady` returns it, and notifies the already
     * loaded page through [NightModeEvent.SEND_NIGHT_MODE_CHANGE]. Same-value calls are ignored.
     * Safe to call from any thread.
     */
    fun updateNight(night: Boolean) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            post { updateNight(night) }
            return
        }
        if (destroyed || this.night == night) return
        this.night = night
        send(
            NightModeEvent.SEND_NIGHT_MODE_CHANGE,
            JSONObject().put("night", if (night) 1 else 0)
        )
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

    /**
     * Releases the WebView. Idempotent: later calls are ignored, so a host that destroys the
     * embedded view itself and a container that destroys it again cannot hit a torn-down WebView.
     */
    override fun destroy() {
        if (destroyed) return
        destroyed = true
        removeJavascriptInterface(BRIDGE_NAME)
        bridge.destroy()
        stopLoading()
        onPause()
        clearHistory()
        // WebView.destroy() requires the view to be detached first.
        (parent as? ViewGroup)?.removeView(this)
        removeAllViews()
        CookieManager.getInstance().flush()
        super.destroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initSettings() {
        setWebContentsDebuggingEnabled(SdkConfig.current?.debug == true)
        settings.apply {
            userAgentString = "$userAgentString LampsSDK/${BuildConfig.SDK_VERSION}"
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
