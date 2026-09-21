package com.hupu.games

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.lamps.sdk.LampsSdk
import com.lamps.sdk.config.GameCenterConfig
import com.lamps.sdk.config.NightMode
import com.lamps.sdk.view.GameCenterView

/**
 * 演示 LampsSdk.getGameCenterView() 的 Fragment，
 * 直接将返回的 View 添加到布局中，无需手动管理生命周期。
 *
 * 同时演示宿主日夜间切换时调用 GameCenterView.updateConfig() 同步 SDK 内嵌页。
 */
class GameCenterTabFragment : Fragment() {

    private var gameCenterView: GameCenterView? = null
    private var nightMode: NightMode = NightMode.NIGHT

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = FrameLayout(requireActivity())

        val view = LampsSdk.getGameCenterView(requireActivity(), demoGameCenterConfig(nightMode))
        if (view != null) {
            gameCenterView = view
            root.addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }

        root.addView(createNightModeToggle(), toggleLayoutParams())

        return root
    }

    override fun onDestroyView() {
        gameCenterView?.destroy()
        gameCenterView = null
        super.onDestroyView()
    }

    private fun createNightModeToggle(): Button {
        return Button(requireActivity()).apply {
            text = toggleText()
            setOnClickListener {
                nightMode = if (nightMode == NightMode.NIGHT) NightMode.DAY else NightMode.NIGHT
                text = toggleText()
                gameCenterView?.updateConfig(demoGameCenterConfig(nightMode))
            }
        }
    }

    private fun toggleText(): String {
        return if (nightMode == NightMode.NIGHT) "当前夜间，点击切日间" else "当前日间，点击切夜间"
    }

    private fun toggleLayoutParams(): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            val margin = (16 * resources.displayMetrics.density).toInt()
            marginEnd = margin
            bottomMargin = margin
        }
    }
}
