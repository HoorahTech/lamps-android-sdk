package com.lamps.sdk.webview.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import kotlin.math.abs

internal class GameWebViewActionBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var onRestartClick: (() -> Unit)? = null
    private var onExitClick: (() -> Unit)? = null
    private var menuPopup: PopupWindow? = null
    private var isExpanded = true
    private var dockEdge = DockEdge.RIGHT
    private var isDragging = false
    private var isInitiallyPositioned = false
    private var downRawX = 0f
    private var downRawY = 0f
    private var downViewX = 0f
    private var downViewY = 0f
    private var safeInsetTop = 0
    private var safeInsetBottom = 0
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val collapseRunnable = Runnable { collapse() }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        background = roundedBackground(ACTION_BAR_COLOR, dp(16), BORDER_COLOR, dp(1))
        elevation = dp(2).toFloat()
        isClickable = true
        isFocusable = true
        showExpandedContent()
        setOnApplyWindowInsetsListener { _, insets ->
            updateSafeInsets(insets)
            if (isInitiallyPositioned) {
                post {
                    val container = parent as? View ?: return@post
                    y = clampToVerticalSafeArea(container, y)
                }
            }
            insets
        }
    }

    fun setOnRestartClickListener(listener: (() -> Unit)?) {
        onRestartClick = listener
    }

    fun setOnExitClickListener(listener: (() -> Unit)?) {
        onExitClick = listener
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        requestApplyInsets()
        post {
            if (!isInitiallyPositioned) {
                placeAtTopRight()
                isInitiallyPositioned = true
            }
            scheduleCollapse()
        }
    }

    override fun onDetachedFromWindow() {
        animate().cancel()
        menuPopup?.setOnDismissListener(null)
        menuPopup?.dismiss()
        menuPopup = null
        removeCallbacks(collapseRunnable)
        super.onDetachedFromWindow()
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (!isExpanded) {
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cancelCollapse()
                animate().cancel()
                isDragging = false
                captureTouchStart(event)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isDragging && hasExceededTouchSlop(event)) {
                    isDragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (isDragging) {
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> scheduleCollapse()
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isExpanded) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    cancelCollapse()
                    animate().cancel()
                }
                MotionEvent.ACTION_UP -> performClick()
            }
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cancelCollapse()
                animate().cancel()
                isDragging = false
                captureTouchStart(event)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isDragging && hasExceededTouchSlop(event)) {
                    isDragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (isDragging) {
                    moveTo(
                        downViewX + event.rawX - downRawX,
                        downViewY + event.rawY - downRawY
                    )
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val shouldSnapToEdge = isDragging
                parent?.requestDisallowInterceptTouchEvent(false)
                isDragging = false
                if (shouldSnapToEdge) {
                    snapToNearestEdge()
                } else {
                    scheduleCollapse()
                }
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        val handled = super.performClick()
        if (!isExpanded) {
            expand()
            return true
        }
        return handled
    }

    private fun showMenu() {
        cancelCollapse()
        menuPopup?.dismiss()

        val menuWidth = dp(MENU_WIDTH_DP)
        val content = LinearLayout(context).apply {
            orientation = VERTICAL
            background = roundedBackground(Color.WHITE, dp(6))
            addView(createMenuItem("重启") { onRestartClick?.invoke() }, menuItemParams())
            addView(
                View(context).apply { setBackgroundColor(MENU_DIVIDER_COLOR) },
                LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
            )
            addView(createMenuItem("退出") { onExitClick?.invoke() }, menuItemParams())
        }

        menuPopup = PopupWindow(
            content,
            menuWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            elevation = dp(8).toFloat()
            setOnDismissListener {
                menuPopup = null
                if (isExpanded) {
                    scheduleCollapse()
                }
            }
            showAsDropDown(
                this@GameWebViewActionBar,
                this@GameWebViewActionBar.width - menuWidth,
                dp(8)
            )
        }
    }

    private fun collapse() {
        if (!isExpanded || menuPopup?.isShowing == true || !isAttachedToWindow) {
            return
        }
        val container = parent as? View ?: return
        dockEdge = if (x + width / 2f < container.width / 2f) {
            DockEdge.LEFT
        } else {
            DockEdge.RIGHT
        }
        isExpanded = false
        showCollapsedContent()
        updateWidth(COLLAPSED_WIDTH_DP)

        post {
            val hiddenWidth = dp(COLLAPSED_WIDTH_DP - COLLAPSED_VISIBLE_WIDTH_DP)
            val targetX = if (dockEdge == DockEdge.LEFT) {
                -hiddenWidth.toFloat()
            } else {
                (container.width - dp(COLLAPSED_VISIBLE_WIDTH_DP)).toFloat()
            }
            y = clampToVerticalSafeArea(container, y)
            animate().x(targetX).setDuration(ANIMATION_DURATION_MS).start()
        }
    }

    private fun expand() {
        if (isExpanded) {
            return
        }
        val container = parent as? View ?: return
        isExpanded = true
        showExpandedContent()
        updateWidth(EXPANDED_WIDTH_DP)

        post {
            val edgeMargin = dp(EDGE_MARGIN_DP)
            val targetX = if (dockEdge == DockEdge.LEFT) {
                edgeMargin.toFloat()
            } else {
                (container.width - width - edgeMargin).coerceAtLeast(0).toFloat()
            }
            y = clampToVerticalSafeArea(container, y)
            animate().x(targetX).setDuration(ANIMATION_DURATION_MS).start()
            scheduleCollapse()
        }
    }

    private fun showExpandedContent() {
        removeAllViews()
        contentDescription = "游戏操作栏"
        addView(
            createActionButton(ActionGlyph.MORE, "更多操作") { showMenu() },
            weightedActionParams()
        )
        addView(
            View(context).apply { setBackgroundColor(DIVIDER_COLOR) },
            LayoutParams(dp(1), dp(19))
        )
        addView(
            createActionButton(ActionGlyph.EXIT, "退出") { onExitClick?.invoke() },
            weightedActionParams()
        )
    }

    private fun showCollapsedContent() {
        removeAllViews()
        contentDescription = "展开游戏操作栏"
        addView(
            CollapsedArrowView(context, dockEdge),
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    private fun createActionButton(
        glyph: ActionGlyph,
        description: String,
        onClick: () -> Unit
    ): FrameLayout {
        return FrameLayout(context).apply {
            isClickable = true
            isFocusable = true
            contentDescription = description
            foreground = selectableBackground(android.R.attr.selectableItemBackgroundBorderless)
            setOnClickListener { onClick() }
            addView(
                ActionGlyphView(context, glyph), FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
        }
    }

    private fun createMenuItem(label: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            text = label
            textSize = 16f
            setTextColor(MENU_TEXT_COLOR)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), 0, dp(20), 0)
            background = selectableBackground(android.R.attr.selectableItemBackground)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                menuPopup?.dismiss()
                onClick()
            }
        }
    }

    private fun placeAtTopRight() {
        val container = parent as? View ?: return
        x = (container.width - width - dp(EDGE_MARGIN_DP)).coerceAtLeast(0).toFloat()
        y = clampToVerticalSafeArea(container, dp(VERTICAL_SAFE_MARGIN_DP).toFloat())
    }

    private fun moveTo(targetX: Float, targetY: Float) {
        val container = parent as? View ?: return
        x = targetX.coerceIn(0f, (container.width - width).coerceAtLeast(0).toFloat())
        y = clampToVerticalSafeArea(container, targetY)
    }

    private fun snapToNearestEdge() {
        val container = parent as? View ?: return
        dockEdge = if (x + width / 2f < container.width / 2f) {
            DockEdge.LEFT
        } else {
            DockEdge.RIGHT
        }
        val edgeMargin = dp(EDGE_MARGIN_DP)
        val targetX = if (dockEdge == DockEdge.LEFT) {
            edgeMargin.toFloat()
        } else {
            (container.width - width - edgeMargin).coerceAtLeast(0).toFloat()
        }
        y = clampToVerticalSafeArea(container, y)
        animate()
            .x(targetX)
            .setDuration(ANIMATION_DURATION_MS)
            .start()
        scheduleCollapse()
    }

    private fun clampToVerticalSafeArea(container: View, targetY: Float): Float {
        val topSafeY = (safeInsetTop + dp(VERTICAL_SAFE_MARGIN_DP)).toFloat()
        val maxY = (
            container.height - height - safeInsetBottom - dp(VERTICAL_SAFE_MARGIN_DP)
        )
            .toFloat()
            .coerceAtLeast(topSafeY)
        return targetY.coerceIn(topSafeY, maxY)
    }

    private fun updateSafeInsets(insets: WindowInsets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val safeInsets = insets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            safeInsetTop = safeInsets.top
            safeInsetBottom = safeInsets.bottom
            return
        }

        @Suppress("DEPRECATION")
        var top = insets.stableInsetTop
        @Suppress("DEPRECATION")
        var bottom = insets.stableInsetBottom
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            insets.displayCutout?.let { cutout ->
                top = maxOf(top, cutout.safeInsetTop)
                bottom = maxOf(bottom, cutout.safeInsetBottom)
            }
        }
        safeInsetTop = top
        safeInsetBottom = bottom
    }

    private fun captureTouchStart(event: MotionEvent) {
        downRawX = event.rawX
        downRawY = event.rawY
        downViewX = x
        downViewY = y
    }

    private fun hasExceededTouchSlop(event: MotionEvent): Boolean {
        return abs(event.rawX - downRawX) > touchSlop ||
            abs(event.rawY - downRawY) > touchSlop
    }

    private fun updateWidth(widthDp: Int) {
        layoutParams = layoutParams.apply {
            width = dp(widthDp)
        }
    }

    private fun scheduleCollapse() {
        removeCallbacks(collapseRunnable)
        postDelayed(collapseRunnable, COLLAPSE_DELAY_MS)
    }

    private fun cancelCollapse() {
        removeCallbacks(collapseRunnable)
    }

    private fun weightedActionParams(): LayoutParams {
        return LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
    }

    private fun menuItemParams(): LayoutParams {
        return LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48))
    }

    private fun selectableBackground(attribute: Int) = context.obtainStyledAttributes(
        intArrayOf(attribute)
    ).let { attributes ->
        try {
            attributes.getDrawable(0)
        } finally {
            attributes.recycle()
        }
    }

    private fun roundedBackground(
        color: Int,
        radius: Int,
        strokeColor: Int? = null,
        strokeWidth: Int = 0
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = radius.toFloat()
            if (strokeColor != null) {
                setStroke(strokeWidth, strokeColor)
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private enum class DockEdge {
        LEFT,
        RIGHT
    }

    private enum class ActionGlyph {
        MORE,
        EXIT
    }

    private class ActionGlyphView(
        context: Context,
        private val glyph: ActionGlyph
    ) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeCap = Paint.Cap.ROUND
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val centerX = width / 2f
            val centerY = height / 2f
            when (glyph) {
                ActionGlyph.MORE -> {
                    paint.style = Paint.Style.FILL
                    val dotRadius = dp(1.6f)
                    val dotSpacing = dp(5.5f)
                    canvas.drawCircle(centerX - dotSpacing, centerY, dotRadius, paint)
                    canvas.drawCircle(centerX, centerY, dotRadius, paint)
                    canvas.drawCircle(centerX + dotSpacing, centerY, dotRadius, paint)
                }

                ActionGlyph.EXIT -> {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = dp(2f)
                    canvas.drawCircle(centerX, centerY, dp(7f), paint)
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(centerX, centerY, dp(2.2f), paint)
                }
            }
        }

        private fun dp(value: Float): Float {
            return value * resources.displayMetrics.density
        }
    }

    private class CollapsedArrowView(
        context: Context,
        private val dockEdge: DockEdge
    ) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = dp(2f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val path = Path()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val visibleWidth = dp(COLLAPSED_VISIBLE_WIDTH_DP.toFloat())
            val centerX = if (dockEdge == DockEdge.RIGHT) {
                visibleWidth / 2f
            } else {
                width - visibleWidth / 2f
            }
            val centerY = height / 2f
            val horizontalOffset = dp(3f)
            val verticalOffset = dp(5f)

            path.reset()
            if (dockEdge == DockEdge.RIGHT) {
                path.moveTo(centerX + horizontalOffset, centerY - verticalOffset)
                path.lineTo(centerX - horizontalOffset, centerY)
                path.lineTo(centerX + horizontalOffset, centerY + verticalOffset)
            } else {
                path.moveTo(centerX - horizontalOffset, centerY - verticalOffset)
                path.lineTo(centerX + horizontalOffset, centerY)
                path.lineTo(centerX - horizontalOffset, centerY + verticalOffset)
            }
            canvas.drawPath(path, paint)
        }

        private fun dp(value: Float): Float {
            return value * resources.displayMetrics.density
        }
    }

    private companion object {
        const val EXPANDED_WIDTH_DP = 86
        const val COLLAPSED_WIDTH_DP = 56
        const val COLLAPSED_VISIBLE_WIDTH_DP = 42
        const val EDGE_MARGIN_DP = 10
        const val VERTICAL_SAFE_MARGIN_DP = 10
        const val MENU_WIDTH_DP = 120
        const val COLLAPSE_DELAY_MS = 3_000L
        const val ANIMATION_DURATION_MS = 180L
        const val ACTION_BAR_COLOR = 0x3D000000
        const val BORDER_COLOR = 0x3DFFFFFF
        const val DIVIDER_COLOR = 0x5EFFFFFF
        const val MENU_DIVIDER_COLOR = 0xFFEDEDED.toInt()
        const val MENU_TEXT_COLOR = 0xFF222222.toInt()
    }
}
