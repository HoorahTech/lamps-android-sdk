package com.lamps.sdk.view

import android.content.Context
import android.widget.FrameLayout
import com.lamps.sdk.config.GameCenterConfig
import com.lamps.sdk.webview.LampsWebView

/** Game center view that hosts the configured game center H5 page. */
class GameCenterView(
    context: Context,
    private val url: String,
    config: GameCenterConfig = GameCenterConfig.Builder().build(),
) : FrameLayout(context) {

    private var webView: LampsWebView? = null

    init {
        val pageOptions = config.toPageOptions()
        val wv = LampsWebView(context).apply {
            displayMode = pageOptions.displayMode
            night = pageOptions.night
            loadUrl(this@GameCenterView.url)
        }
        webView = wv
        addView(wv, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun destroy() {
        webView?.let {
            it.onPause()
            it.destroy()
        }
        webView = null
        removeAllViews()
    }
}
