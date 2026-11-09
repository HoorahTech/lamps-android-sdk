package com.lamps.sdk.webview.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
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
        isClickable = true
        contentDescription = GUIDE_TEXT
        addView(ArrowView(context), LayoutParams(dp(ARROW_WIDTH_DP), dp(ARROW_HEIGHT_DP)).apply {
            marginEnd = dp(ARROW_END_MARGIN_DP)
            bottomMargin = -dp(1)
        })
        addView(TextView(context).apply {
            text = GUIDE_TEXT
            textSize = 13f
            maxLines = 1
            setTextColor(TEXT_COLOR)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(8).toFloat()
                setColor(BUBBLE_COLOR)
            }
        }, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
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

    private companion object {
        const val GUIDE_TEXT = "可以拖动，点这里退出"
        const val ARROW_WIDTH_DP = 12
        const val ARROW_HEIGHT_DP = 6
        const val ARROW_END_MARGIN_DP = 37
        const val TEXT_COLOR = 0xFFFFFFFF.toInt()
        const val BUBBLE_COLOR = 0xE61A1A1A.toInt()
    }
}
