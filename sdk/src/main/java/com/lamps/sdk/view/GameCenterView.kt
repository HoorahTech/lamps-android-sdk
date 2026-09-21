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

    /**
     * 运行时更新配置，供宿主在自身状态变化（如日夜间切换）时调用，调用时机由宿主控制。
     *
     * 目前生效字段：日夜间。`config` 未设 `setNightMode` 时按 `LampsConfig` 的 NightModeProvider
     * 现取，都没有则日间。`DisplayMode` 等创建时确定的字段会被忽略。
     *
     * 与创建时相同的值不会重复下发；`destroy()` 之后调用无效果；可从任意线程调用。
     */
    fun updateConfig(config: GameCenterConfig) {
        val pageOptions = config
            .withDisplayMode(GameCenterConfig.DisplayMode.EMBED)
            .toPageOptions()
        webView?.updateNight(pageOptions.night)
    }

    /** 释放内嵌 WebView。可重复调用，第二次及以后无效果。 */
    fun destroy() {
        val wv = webView ?: return
        webView = null
        // 不在这里 removeView：LampsWebView.destroy() 会在 bridge 清理完成之后自己脱离视图树。
        // 提前 detach 会让 bridge 里基于 ViewTreeObserver 的监听摘不掉，反而留下泄漏。
        wv.destroy()
        removeAllViews()
    }
}
