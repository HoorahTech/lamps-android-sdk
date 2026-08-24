package com.lamps.sdk.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.lamps.sdk.webview.LampsWebView

/**
 * 游戏中心自定义 View，内部包裹 LampsWebView 自动加载游戏中心 H5 页面。
 *
 * 外部集成方通过 [LampsSdk.getGameCenterView] 获取实例，直接添加到任意布局即可。
 * 生命周期（onResume/onPause/destroy）由自身通过 [onAttachedToWindow]、
 * [onDetachedFromWindow] 和 [onWindowVisibilityChanged] 自动管理。
 */
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

    override fun onDetachedFromWindow() {
        destroy()
        super.onDetachedFromWindow()
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
