package com.lamps.sdk.webview.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import android.widget.TextView

internal class GameActionBarGuideView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = VERTICAL
        gravity = Gravity.END
        clipChildren = false
        isClickable = true
        contentDescription = GUIDE_TEXT
        addView(ArrowView(context), LayoutParams(dp(ARROW_WIDTH_DP), dp(ARROW_HEIGHT_DP)).apply {
            marginEnd = dp(ARROW_END_MARGIN_DP)
            bottomMargin = -dp(1)
        })
        addView(contentRow(), LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

    private fun contentRow(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(4).toFloat()
                setColor(BUBBLE_COLOR)
            }
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            elevation = dp(4).toFloat()
            addView(TextView(context).apply {
                text = GUIDE_TEXT
                textSize = 14f
                maxLines = 1
                includeFontPadding = false
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(TEXT_COLOR)
            }, LayoutParams(LayoutParams.WRAP_CONTENT, dp(TEXT_LINE_HEIGHT_DP)))
            addView(CloseIconView(context), LayoutParams(dp(12), dp(12)).apply {
                marginStart = dp(4)
            })
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private class ArrowView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BUBBLE_COLOR
            style = Paint.Style.FILL
        }
        private val path = Path()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            path.reset()
            path.moveTo(width / 2f, 0f)
            path.lineTo(width.toFloat(), height.toFloat())
            path.lineTo(0f, height.toFloat())
            path.close()
            canvas.drawPath(path, paint)
        }
    }

    private class CloseIconView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CLOSE_COLOR
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.SQUARE
            strokeWidth = dp(1f)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val inset = (width - dp(8f)) / 2f
            canvas.drawLine(inset, inset, width - inset, height - inset, paint)
            canvas.drawLine(width - inset, inset, inset, height - inset, paint)
        }

        private fun dp(value: Float): Float {
            return value * resources.displayMetrics.density
        }
    }

    private companion object {
        const val GUIDE_TEXT = "可移动位置，按住拖动"
        const val ARROW_WIDTH_DP = 11
        const val ARROW_HEIGHT_DP = 6
        const val ARROW_END_MARGIN_DP = 16
        const val TEXT_LINE_HEIGHT_DP = 21
        const val TEXT_COLOR = 0xFF111111.toInt()
        const val BUBBLE_COLOR = 0xFFFFFFFF.toInt()
        const val CLOSE_COLOR = 0xFF89909F.toInt()
    }
}
