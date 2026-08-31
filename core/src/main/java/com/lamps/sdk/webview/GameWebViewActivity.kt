package com.lamps.sdk.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.lamps.sdk.webview.bridge.back.BackAbilityUtil
import com.lamps.sdk.webview.view.GameWebViewActionBar
import com.lamps.sdk.webview.view.StatusBarApplier

internal class GameWebViewActivity : Activity() {
    private lateinit var webView: LampsWebView
    private lateinit var initialUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initialUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        if (initialUrl.isBlank()) {
            finish()
            return
        }

        setContentView(createContentView())
        StatusBarApplier.applyDefault(this, webView)
        attachActionBar()
        webView.loadUrl(initialUrl)
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
        if (::webView.isInitialized) {
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun createContentView(): View {
        webView = LampsWebView(this)
        return FrameLayout(this).apply {
            addView(
                webView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
    }

    private fun attachActionBar() {
        addContentView(
            GameWebViewActionBar(this).apply {
                setOnRestartClickListener {
                    webView.stopLoading()
                    webView.loadUrl(initialUrl)
                }
                setOnExitClickListener { finish() }
            },
            FrameLayout.LayoutParams(dp(86), dp(32), Gravity.TOP or Gravity.START)
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val EXTRA_URL = "extra_url"

        @JvmStatic
        @JvmOverloads
        fun buildIntent(
            context: Context,
            url: String,
        ): Intent {
            return Intent(context, GameWebViewActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        }
    }
}
