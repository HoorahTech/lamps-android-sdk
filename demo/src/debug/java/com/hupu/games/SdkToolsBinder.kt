package com.hupu.games

import android.app.Activity
import android.widget.Button
import com.lamps.sdk.LampsSdkTools

internal object SdkToolsBinder {
    fun bind(activity: Activity, button: Button) {
        button.setOnClickListener {
            LampsSdkTools.startActivity(activity)
        }
    }
}
