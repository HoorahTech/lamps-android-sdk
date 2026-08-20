package com.lamps.sdk.tools

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.lamps.sdk.debug.LampsSdkDebug
import com.lamps.sdk.debug.LampsSdkDebugItem
import com.lamps.sdk.debug.LampsSdkDebugSection

class LampsSdkToolsActivity : Activity() {
    private lateinit var sectionsContainer: LinearLayout
    private val expandedState = mutableMapOf<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createContentView())
        renderInfo()
    }

    override fun onResume() {
        super.onResume()
        if (::sectionsContainer.isInitialized) renderInfo()
    }

    private fun createContentView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        root.addView(
            createTitleBar(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
            )
        )

        sectionsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(24))
        }
        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                createChannelToolsBar(),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                sectionsContainer,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        root.addView(
            ScrollView(this).apply { addView(scrollContent) },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        return root
    }

    private fun createTitleBar(): View {
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.rgb(245, 245, 245))
            addView(
                ImageButton(this@LampsSdkToolsActivity).apply {
                    setImageResource(R.drawable.lamps_ic_arrow_back)
                    contentDescription = "返回"
                    setBackgroundColor(Color.TRANSPARENT)
                    setPadding(dp(14), dp(14), dp(14), dp(14))
                    setOnClickListener { finish() }
                },
                LinearLayout.LayoutParams(
                    dp(52),
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            addView(
                TextView(this@LampsSdkToolsActivity).apply {
                    text = "Lamps SDK Tools"
                    textSize = 17f
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            )
            addView(
                ImageButton(this@LampsSdkToolsActivity).apply {
                    setImageResource(R.drawable.lamps_ic_content_copy)
                    contentDescription = "复制"
                    setBackgroundColor(Color.TRANSPARENT)
                    setPadding(dp(14), dp(14), dp(14), dp(14))
                    setOnClickListener { copyInfo() }
                },
                LinearLayout.LayoutParams(
                    dp(52),
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
    }

    private fun createChannelToolsBar(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
            addView(createChannelToolButton("穿山甲调试工具") {
                ChannelSdkTools.startPangle(this@LampsSdkToolsActivity)
            })
            addView(createChannelToolButton("优量汇调试工具") {
                ChannelSdkTools.startYlh(this@LampsSdkToolsActivity)
            })
            addView(createChannelToolButton("汇川预览工具") {
                ChannelSdkTools.startNoah(this@LampsSdkToolsActivity)
            })
        }
    }

    private fun createChannelToolButton(title: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = title
            isAllCaps = false
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4)
                bottomMargin = dp(4)
            }
        }
    }

    private fun copyInfo() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("Lamps SDK Tools", LampsSdkDebug.buildInfo(this))
        )
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
    }

    private fun renderInfo() {
        val sections = LampsSdkDebug.buildSections(this)
        sectionsContainer.removeAllViews()
        sections.forEach { section ->
            val expanded = expandedState.getOrPut(section.title) { section.expandedByDefault }
            sectionsContainer.addView(createSectionView(section, expanded))
        }
    }

    private fun createSectionView(section: LampsSdkDebugSection, expanded: Boolean): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (expanded) View.VISIBLE else View.GONE
        }
        if (section.body.isNotEmpty()) {
            content.addView(
                createBodyView(section.body, nested = false),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        section.items.forEach { item ->
            val itemExpanded = expandedState.getOrPut(item.id) { item.expandedByDefault }
            content.addView(
                createItemView(item, itemExpanded),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        val arrowView = createArrowView(expanded)
        val header = createHeaderView(section.title, arrowView) {
            val next = expandedState[section.title] != true
            expandedState[section.title] = next
            arrowView.text = if (next) "▼" else "▶"
            content.visibility = if (next) View.VISIBLE else View.GONE
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                header,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                content,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun createItemView(item: LampsSdkDebugItem, expanded: Boolean): View {
        val bodyView = createBodyView(item.body, nested = true).apply {
            visibility = if (expanded) View.VISIBLE else View.GONE
        }
        val arrowView = createArrowView(expanded)
        val header = createHeaderView(item.title, arrowView, nested = true) {
            val next = expandedState[item.id] != true
            expandedState[item.id] = next
            arrowView.text = if (next) "▼" else "▶"
            bodyView.visibility = if (next) View.VISIBLE else View.GONE
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                header,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                bodyView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun createHeaderView(
        title: String,
        arrowView: TextView,
        nested: Boolean = false,
        onClick: () -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(if (nested) Color.rgb(250, 250, 250) else Color.rgb(245, 245, 245))
            setPadding(
                dp(if (nested) 24 else 12),
                dp(if (nested) 8 else 10),
                dp(12),
                dp(if (nested) 8 else 10)
            )
            isClickable = true
            isFocusable = true
            addView(
                arrowView,
                LinearLayout.LayoutParams(dp(24), ViewGroup.LayoutParams.WRAP_CONTENT)
            )
            addView(
                TextView(this@LampsSdkToolsActivity).apply {
                    text = title
                    textSize = if (nested) 13f else 15f
                    setTextColor(Color.BLACK)
                    typeface = if (nested) Typeface.MONOSPACE else Typeface.DEFAULT_BOLD
                },
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )
            setOnClickListener { onClick() }
        }
    }

    private fun createArrowView(expanded: Boolean): TextView {
        return TextView(this).apply {
            text = if (expanded) "▼" else "▶"
            textSize = 12f
            setTextColor(Color.rgb(80, 80, 80))
            gravity = Gravity.CENTER
        }
    }

    private fun createBodyView(body: String, nested: Boolean): TextView {
        return TextView(this).apply {
            text = body
            setTextColor(Color.rgb(32, 32, 32))
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setPadding(dp(if (nested) 28 else 16), dp(8), dp(16), dp(16))
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
