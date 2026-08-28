package com.lamps.sdk.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.lamps.sdk.webview.view.ScreenConfigApplier

internal class CommonWebViewActivity : Activity() {
    private lateinit var webView: LampsWebView
    private lateinit var initialUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScreenConfigApplier.applyDefault(this)

        initialUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        if (initialUrl.isBlank()) {
            finish()
            return
        }

        setContentView(createContentView())
        webView.loadUrl(initialUrl)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            ScreenConfigApplier.reapply(this)
        }
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
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
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
        return webView
    }

    companion object {
        const val EXTRA_URL = "extra_url"

        @JvmStatic
        @JvmOverloads
        fun buildIntent(
            context: Context,
            url: String,
        ): Intent {
            return Intent(context, CommonWebViewActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        }
    }
}
