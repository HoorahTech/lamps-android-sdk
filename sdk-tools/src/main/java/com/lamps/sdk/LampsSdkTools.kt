package com.lamps.sdk

import android.app.Activity
import android.content.Intent
import com.lamps.sdk.tools.LampsSdkToolsActivity

object LampsSdkTools {
    @JvmStatic
    fun startActivity(activity: Activity) {
        activity.startActivity(Intent(activity, LampsSdkToolsActivity::class.java))
    }
}
