package com.lamps.sdk.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.lamps.sdk.BuildConfig
import com.lamps.sdk.config.SdkConfig
import com.lamps.sdk.data.behavior.GameSession
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
    private val NEST_SCROLL_NAME = "LampsNestedScroll"
    private val bridge = LampsWebViewBridge(this)
    private val horizontalNesting = HorizontalNesting(this)
    private var pageLoadObserver: PageLoadObserver? = null
    private var loadGuard: PageLoadGuard? = null

    /** 展示形态，取值见 [DISPLAY_MODE_PAGE] / [DISPLAY_MODE_EMBED]，未指定为空串。 */
    var displayMode: String = ""

    /**
     * Host day/night. Exposed to H5 as `night`: 1 night, 0 day.
     *
     * Set this before loading the page. For runtime changes use [updateNight] so the already
     * loaded page is notified as well.
     */
    var night: Boolean = false

    /**
     * Whether the host wants the H5 title hidden.
     *
     * Exposed to H5 as `hideTitle`: 1 hidden, 0 shown. Set before loading the page.
     */
    var hideTitle: Boolean = false

    /** 所在游戏页的本局数据，激励视频按它计数；不在游戏页时为空。 */
    internal var gameSession: GameSession? = null

    /**
     * 打开本页时携带的自定义数据。游戏中心页里再打开单个游戏时，带给该游戏的行为回调。
     */
    internal var trackCustomData: Map<String, String> = emptyMap()

    fun setTrackCustomData(data: Map<String, String>) {
        trackCustomData = LinkedHashMap(data)
    }

    private var destroyed = false

    init {
        initSettings()
        addJavascriptInterface(bridge, BRIDGE_NAME)
        addJavascriptInterface(horizontalNesting, NEST_SCROLL_NAME)
        bridge.registerAbilityInstaller(RewardAdAbilityInstaller)
        bridge.registerAbilityInstaller(CommonAbilityInstaller())
        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                horizontalNesting.onPageStarted()
                pageLoadObserver?.onPageStarted(url)
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                horizontalNesting.install()
                pageLoadObserver?.onPageFinished(url)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                pageLoadObserver?.onReceivedError(request, error)
            }
        }
        webChromeClient = WebChromeClient()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        horizontalNesting.onDispatchTouchEvent(ev)
        val handled = super.dispatchTouchEvent(ev)
        horizontalNesting.afterDispatchTouchEvent()
        return handled
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
        loadGuard?.updateNight(night)
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
     * 打开 [url]，并用 [container] 上的兜底页接住主文档或 js/css 加载失败。
     * 内嵌模式只显示重新加载，不放返回，避免关掉宿主页面。
     *
     * 兜底页只挂在第一次传入的 [container] 上，之后再调用沿用它。[destroy] 之后调用无效果。
     */
    fun loadWithErrorGuard(container: FrameLayout, url: String) {
        if (destroyed) return
        val guard = loadGuard ?: PageLoadGuard(this, container).also { loadGuard = it }
        guard.load(url)
    }

    internal fun setPageLoadObserver(observer: PageLoadObserver?) {
        pageLoadObserver = observer
    }

    /**
     * Releases the WebView. Idempotent: later calls are ignored, so a host that destroys the
     * embedded view itself and a container that destroys it again cannot hit a torn-down WebView.
     */
    override fun destroy() {
        if (destroyed) return
        destroyed = true
        loadGuard?.destroy()
        loadGuard = null
        horizontalNesting.destroy()
        removeJavascriptInterface(BRIDGE_NAME)
        removeJavascriptInterface(NEST_SCROLL_NAME)
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

    companion object {
        /** 整页模式：WebView 独占宿主 Activity，可以改窗口。 */
        const val DISPLAY_MODE_PAGE = "page"

        /** 内嵌模式：WebView 只是宿主布局里的一块，窗口归宿主，SDK 不得修改。 */
        const val DISPLAY_MODE_EMBED = "embed"
    }
}
