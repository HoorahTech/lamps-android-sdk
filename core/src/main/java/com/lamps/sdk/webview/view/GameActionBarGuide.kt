package com.lamps.sdk.webview.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * 本机第一次进入游戏容器时，在操作栏下方展示一次气泡。
 * 点气泡、碰到操作栏，或 5 秒后消失，并恢复操作栏自动收起。
 */
internal class GameActionBarGuide private constructor(
    private val bar: GameWebViewActionBar,
    private val bubble: GameActionBarGuideView
) {
    private var dismissed = false
    private val dismissRunnable = Runnable { dismiss(resumeCollapse = true) }

    fun followAnchor() {
        val width = bubble.width.takeIf { it > 0 } ?: bubble.measuredWidth
        if (width == 0) return
        val gap = dp(8)
        val edge = dp(8).toFloat()
        bubble.x = (bar.x + bar.width - width).coerceAtLeast(edge)
        bubble.y = bar.y + bar.height + gap
    }

    fun dismiss(resumeCollapse: Boolean) {
        if (dismissed) return
        dismissed = true
        bar.removeCallbacks(dismissRunnable)
        (bubble.parent as? ViewGroup)?.removeView(bubble)
        bar.onGuideDismissed(this)
        if (resumeCollapse) bar.resumeAutoCollapse()
    }

    private fun dp(value: Int): Int {
        return (value * bubble.resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val VISIBLE_MS = 5_000L
        private const val PREFS_NAME = "lamps_sdk"
        private const val KEY_SHOWN = "game_action_bar_guide_shown"

        fun tryShow(bar: GameWebViewActionBar): GameActionBarGuide? {
            if (isShown(bar.context)) return null
            val parent = bar.parent as? ViewGroup ?: return null
            parent.clipChildren = false
            val bubble = GameActionBarGuideView(bar.context)
            parent.addView(
                bubble,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
            bubble.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val guide = GameActionBarGuide(bar, bubble)
            guide.followAnchor()
            bar.pauseAutoCollapse()
            bubble.setOnClickListener { guide.dismiss(resumeCollapse = true) }
            markShown(bar.context)
            bar.postDelayed(guide.dismissRunnable, VISIBLE_MS)
            return guide
        }

        private fun isShown(context: Context): Boolean {
            return prefs(context).getBoolean(KEY_SHOWN, false)
        }

        private fun markShown(context: Context) {
            prefs(context).edit().putBoolean(KEY_SHOWN, true).apply()
        }

        private fun prefs(context: Context) =
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
