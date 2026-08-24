package com.hupu.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.lamps.sdk.LampsSdk
import com.lamps.sdk.view.GameCenterView

/**
 * 演示 LampsSdk.getGameCenterView() 的 Fragment，
 * 直接将返回的 View 添加到布局中，无需手动管理生命周期。
 */
class GameCenterTabFragment : Fragment() {

    private var gameCenterView: GameCenterView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = FrameLayout(requireActivity())

        val view = LampsSdk.getGameCenterView(requireActivity())
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

        return root
    }

    override fun onDestroyView() {
        gameCenterView?.destroy()
        gameCenterView = null
        super.onDestroyView()
    }
}
