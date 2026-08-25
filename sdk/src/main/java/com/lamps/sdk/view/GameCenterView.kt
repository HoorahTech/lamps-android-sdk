package com.lamps.sdk.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.lamps.sdk.webview.LampsWebView

/** Game center view that hosts the configured game center H5 page. */
class GameCenterView @JvmOverloads constructor(
    context: Context,
    private val url: String,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var webView: LampsWebView? = null

    init {
        val wv = LampsWebView(context).apply {
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
