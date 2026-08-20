package com.lamps.sdk.tools

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.bytedance.tools.util.ToolsUtil
import com.qq.e.union.tools.ToolsActivity

internal object ChannelSdkTools {
    private const val NOAH_TEST_ACTIVITY = "com.noah.sdk.dg.external.AdTestActivity"

    fun startPangle(activity: Activity) {
        start("穿山甲调试工具", activity) {
            ToolsUtil.start(activity)
        }
    }

    fun startYlh(activity: Activity) {
        start("优量汇调试工具", activity) {
            activity.startActivity(Intent(activity, ToolsActivity::class.java))
        }
    }

    fun startNoah(activity: Activity) {
        start("汇川预览工具", activity) {
            activity.startActivity(
                Intent().setClassName(activity, NOAH_TEST_ACTIVITY)
            )
        }
    }

    private fun start(name: String, activity: Activity, block: () -> Unit) {
        runCatching(block).onFailure { error ->
            Toast.makeText(
                activity,
                "$name 打开失败: ${error.message.orEmpty()}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
