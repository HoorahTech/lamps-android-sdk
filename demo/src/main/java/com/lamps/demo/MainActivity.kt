package com.lamps.demo

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.lamps.sdk.LampsSdk

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val statusText = findViewById<TextView>(R.id.statusText)
        statusText.text = buildString {
            append("sdkVersion=").append(LampsSdk.getSdkVersion()).append('\n')
            append("ready=").append(LampsSdk.isSdkReady())
        }
    }
}
