package com.lamps.sdk.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import com.lamps.sdk.data.behavior.BehaviorTracker
import com.lamps.sdk.data.behavior.GameCenterSession
import com.lamps.sdk.webview.bridge.back.BackAbilityUtil
import com.lamps.sdk.webview.view.StatusBarApplier

internal class CommonWebViewActivity : Activity() {
    private lateinit var webView: LampsWebView
    private lateinit var content: FrameLayout
    private lateinit var initialUrl: String
    private var session: GameCenterSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initialUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        if (initialUrl.isBlank()) {
            finish()
            return
        }

        setContentView(createContentView())
        StatusBarApplier.applyDefault(this, webView)
        webView.loadWithErrorGuard(content, initialUrl)
        session = BehaviorTracker.openCenter(pageOptions().trackCustomData.orEmpty())
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            webView.onResume()
        }
    }

    override fun onPause() {
        if (::webView.isInitialized) {
            webView.onPause()
        }
        super.onPause()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!::webView.isInitialized || !BackAbilityUtil.handleBackPressed(this, webView)) {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        session?.let(BehaviorTracker::closeCenter)
        if (::webView.isInitialized) {
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun createContentView(): View {
        webView = LampsWebView(this).apply {
            val options = pageOptions()
            displayMode = options.displayMode
            night = options.night
            hideTitle = options.hideTitle
            setTrackCustomData(pageOptions().trackCustomData.orEmpty())
        }
        content = FrameLayout(this).apply {
            addView(
                webView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
        return content
    }

    private fun pageOptions(): GameCenterPageOptions {
        @Suppress("DEPRECATION")
        return intent.getSerializableExtra(EXTRA_PAGE_OPTIONS) as? GameCenterPageOptions
            ?: GameCenterPageOptions()
    }

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_PAGE_OPTIONS = "extra_page_options"

        @JvmStatic
        @JvmOverloads
        fun buildIntent(
            context: Context,
            url: String,
            pageOptions: GameCenterPageOptions = GameCenterPageOptions(),
        ): Intent {
            return Intent(context, CommonWebViewActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_PAGE_OPTIONS, pageOptions)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        }
    }
}
