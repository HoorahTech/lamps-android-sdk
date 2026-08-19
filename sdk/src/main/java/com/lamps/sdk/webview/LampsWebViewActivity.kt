package com.lamps.sdk.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView

class LampsWebViewActivity : Activity() {
    private lateinit var webView: LampsWebView
    private lateinit var titleView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_HIDE_STATUS_BAR, false)) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        if (url.isBlank()) {
            finish()
            return
        }

        setContentView(createContentView())
        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (titleView.text.isNullOrBlank()) {
                    titleView.text = title?.trim().orEmpty()
                }
            }
        }
        webView.loadUrl(url)
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
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        titleView = TextView(this).apply {
            text = intent.getStringExtra(EXTRA_TITLE).orEmpty()
            textSize = 17f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }
        val backView = TextView(this).apply {
            text = "‹"
            textSize = 36f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }
        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(backView, LinearLayout.LayoutParams(dp(48), dp(48)))
            addView(
                titleView,
                LinearLayout.LayoutParams(0, dp(48), 1f)
            )
            addView(View(this@LampsWebViewActivity), LinearLayout.LayoutParams(dp(48), dp(48)))
            visibility = if (
                intent.getBooleanExtra(EXTRA_HIDE_NAVIGATION_BAR, true)
            ) View.GONE else View.VISIBLE
        }
        root.addView(
            titleBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        webView = LampsWebView(this)
        root.addView(
            webView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        return root
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_HIDE_NAVIGATION_BAR = "extra_hide_navigation_bar"
        const val EXTRA_HIDE_STATUS_BAR = "extra_hide_status_bar"

        @JvmStatic
        @JvmOverloads
        fun buildIntent(
            context: Context,
            url: String,
            title: String? = null,
            isNavigationBarHidden: Boolean = true,
            isStatusBarHidden: Boolean = false
        ): Intent {
            return Intent(context, LampsWebViewActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_HIDE_NAVIGATION_BAR, isNavigationBarHidden)
                putExtra(EXTRA_HIDE_STATUS_BAR, isStatusBarHidden)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        }
    }
}
