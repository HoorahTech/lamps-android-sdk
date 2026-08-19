package com.lamps.demo

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.lamps.sdk.LampsSdk
import com.lamps.sdk.LampsSdkTools
import com.lamps.sdk.webview.LampsWebViewActivity

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val statusText = findViewById<TextView>(R.id.statusText)
        statusText.text = buildString {
            append("sdkVersion=").append(LampsSdk.getSdkVersion()).append('\n')
            append("ready=").append(LampsSdk.isSdkReady())
        }
        findViewById<Button>(R.id.openWebViewButton).setOnClickListener {
            startActivity(
                LampsWebViewActivity.buildIntent(
                    context = this,
                    url = DEMO_URL
                )
            )
        }
        findViewById<Button>(R.id.openSdkToolsButton).setOnClickListener {
            LampsSdkTools.startActivity(this)
        }
    }

    private companion object {
        const val DEMO_URL =
            "https://www.hoorahgo.com/mg/index.html#/play?demo=nba-career-sim"
    }
}
