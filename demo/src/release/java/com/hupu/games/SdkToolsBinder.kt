package com.hupu.games

import android.app.Activity
import android.view.View
import android.widget.Button

internal object SdkToolsBinder {
    fun bind(@Suppress("UNUSED_PARAMETER") activity: Activity, button: Button) {
        button.visibility = View.GONE
    }
}
