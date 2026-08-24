package com.hupu.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.lamps.sdk.LampsSdk
import java.util.Locale

/**
 * 简单的列表 Fragment，用于 TabLayout 演示中的非游戏中心页面。
 */
class SimpleListFragment : Fragment() {

    companion object {
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_ITEM_COUNT = "extra_item_count"
        private const val EXTRA_GAME_CENTER_AT_FIFTH = "extra_game_center_at_fifth"
        private const val GAME_CENTER_POSITION = 4

        @JvmStatic
        fun newInstance(
            title: String,
            itemCount: Int = 20,
            gameCenterAtFifth: Boolean = false
        ): SimpleListFragment {
            return SimpleListFragment().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_TITLE, title)
                    putInt(EXTRA_ITEM_COUNT, itemCount)
                    putBoolean(EXTRA_GAME_CENTER_AT_FIFTH, gameCenterAtFifth)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val title = arguments?.getString(EXTRA_TITLE) ?: "列表"
        val itemCount = arguments?.getInt(EXTRA_ITEM_COUNT) ?: 20
        val gameCenterAtFifth = arguments?.getBoolean(EXTRA_GAME_CENTER_AT_FIFTH) == true

        val listView = ListView(requireActivity())
        listView.adapter = object : BaseAdapter() {
            override fun getCount(): Int = itemCount

            override fun getItem(position: Int): Any = position + 1

            override fun getItemId(position: Int): Long = position.toLong()

            override fun getViewTypeCount(): Int = 2

            override fun getItemViewType(position: Int): Int {
                return if (gameCenterAtFifth && position == GAME_CENTER_POSITION) 1 else 0
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                if (getItemViewType(position) == 1) {
                    return LampsSdk.getGameCenterView(requireActivity())?.apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(240)
                        )
                    } ?: createTextView(position + 1)
                }
                return (convertView as? TextView ?: createTextView(position + 1)).apply {
                    text = itemText(position + 1)
                }
            }

            private fun createTextView(index: Int): TextView {
                return TextView(requireActivity()).apply {
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                    textSize = 16f
                    text = itemText(index)
                }
            }

            private fun itemText(index: Int): String {
                return String.format(Locale.getDefault(), "%s - 第 %d 条内容", title, index)
            }
        }
        return listView
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

}
