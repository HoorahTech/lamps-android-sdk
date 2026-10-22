package com.lamps.sdk.webview.view

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.lamps.sdk.utils.DeviceUtils

/** 页面没加载出来时盖在 WebView 上的兜底页：左上角返回 + 无网图标 + 文案 + 重新加载。 */
internal class LoadErrorView(
    context: Context,
    night: Boolean,
    onRetry: () -> Unit,
) : FrameLayout(context) {
    private val back = BackGlyph(context).apply {
        isClickable = true
        isFocusable = true
        contentDescription = "返回"
    }
    private val icon = NoNetworkIcon(context)
    private val title = textView(TITLE, 16f)
    private val message = textView(MESSAGE, 13f)
    private val retry = textView(RETRY, 14f).apply {
        isClickable = true
        isFocusable = true
        setOnClickListener { onRetry() }
    }
    private val resetRetry = Runnable { showLoading(false) }
    private var loadingSince = 0L

    init {
        isClickable = true
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            // 视觉重心略高于几何中心。
            setPadding(0, 0, 0, dp(48))
            addView(icon, LinearLayout.LayoutParams(dp(56), dp(56)))
            addView(title, wrapContent().apply { topMargin = dp(16) })
            addView(message, wrapContent().apply { topMargin = dp(6) })
            addView(retry, LinearLayout.LayoutParams(dp(112), dp(36)).apply { topMargin = dp(24) })
        }
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        back.setOnClickListener { findActivity()?.finish() }
        addView(back, LayoutParams(dp(44), dp(44)).apply {
            leftMargin = dp(4)
            topMargin = DeviceUtils.statusBarHeight(context)
        })
        applyNight(night)
    }

    private fun applyNight(night: Boolean) {
        val palette = if (night) NIGHT else DAY
        setBackgroundColor(palette.background)
        back.setColor(palette.title)
        back.background = RippleDrawable(ColorStateList.valueOf(palette.ripple), null, null)
        icon.setColors(palette.icon, palette.background)
        title.setTextColor(palette.title)
        message.setTextColor(palette.message)
        retry.setTextColor(palette.title)
        retry.background = RippleDrawable(
            ColorStateList.valueOf(palette.ripple),
            pill(Color.TRANSPARENT, palette.stroke),
            pill(Color.WHITE, null)
        )
    }

    /**
     * 断网时重试会瞬间失败，按钮至少保持 [MIN_LOADING_MS] 的加载态，避免文案一闪而过。
     */
    fun setLoading(loading: Boolean) {
        removeCallbacks(resetRetry)
        if (loading) {
            loadingSince = SystemClock.uptimeMillis()
            showLoading(true)
            return
        }
        val remaining = MIN_LOADING_MS - (SystemClock.uptimeMillis() - loadingSince)
        if (remaining > 0) postDelayed(resetRetry, remaining) else showLoading(false)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(resetRetry)
        super.onDetachedFromWindow()
    }

    private fun showLoading(loading: Boolean) {
        retry.text = if (loading) LOADING else RETRY
        retry.isEnabled = !loading
    }

    private fun textView(text: String, sizeSp: Float) = TextView(context).apply {
        this.text = text
        textSize = sizeSp
        gravity = Gravity.CENTER
    }

    private fun pill(fill: Int, stroke: Int?) = GradientDrawable().apply {
        cornerRadius = dp(18).toFloat()
        setColor(fill)
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun wrapContent() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun findActivity(): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    private class BackGlyph(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val path = Path()

        fun setColor(color: Int) {
            paint.color = color
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val density = resources.displayMetrics.density
            val cx = width / 2f
            val cy = height / 2f
            val half = 7f * density
            paint.strokeWidth = 2f * density
            path.reset()
            path.moveTo(cx + half / 2f, cy - half)
            path.lineTo(cx - half / 2f, cy)
            path.lineTo(cx + half / 2f, cy + half)
            canvas.drawPath(path, paint)
        }
    }

    /** Wi-Fi 三道弧加一条斜杠，斜杠两侧用背景色描边留出间隙。 */
    private class NoNetworkIcon(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
        private val arc = RectF()
        private var color = Color.GRAY
        private var gapColor = Color.WHITE

        fun setColors(color: Int, gapColor: Int) {
            this.color = color
            this.gapColor = gapColor
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val cx = w / 2f
            val cy = h * 0.76f
            val stroke = w * 0.055f

            paint.color = color
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = stroke
            for (ratio in ARC_RATIOS) {
                val r = w * ratio
                arc.set(cx - r, cy - r, cx + r, cy + r)
                canvas.drawArc(arc, 225f, 90f, false, paint)
            }
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, stroke, paint)

            paint.style = Paint.Style.STROKE
            paint.color = gapColor
            paint.strokeWidth = stroke * 3f
            canvas.drawLine(w * 0.22f, h * 0.18f, w * 0.78f, h * 0.86f, paint)
            paint.color = color
            paint.strokeWidth = stroke
            canvas.drawLine(w * 0.22f, h * 0.18f, w * 0.78f, h * 0.86f, paint)
        }
    }

    private class Palette(
        val background: Int,
        val icon: Int,
        val title: Int,
        val message: Int,
        val stroke: Int,
        val ripple: Int,
    )

    private companion object {
        const val TITLE = "网络连接失败"
        const val MESSAGE = "请检查网络设置后重试"
        const val RETRY = "重新加载"
        const val LOADING = "加载中…"
        const val MIN_LOADING_MS = 600L
        val ARC_RATIOS = floatArrayOf(0.18f, 0.34f, 0.5f)

        val DAY = Palette(
            background = 0xFFFFFFFF.toInt(),
            icon = 0xFFC8C8CC.toInt(),
            title = 0xFF222222.toInt(),
            message = 0xFF999999.toInt(),
            stroke = 0xFFDDDDDD.toInt(),
            ripple = 0x14000000,
        )
        val NIGHT = Palette(
            background = 0xFF1A1A1A.toInt(),
            icon = 0xFF4D4D52.toInt(),
            title = 0xFFE6E6E6.toInt(),
            message = 0xFF7A7A80.toInt(),
            stroke = 0xFF3D3D42.toInt(),
            ripple = 0x1FFFFFFF,
        )
    }
}
