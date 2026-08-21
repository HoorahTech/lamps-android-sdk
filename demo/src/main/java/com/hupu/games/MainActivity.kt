package com.hupu.games

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.lamps.sdk.LampsSdk
import com.lamps.sdk.reward.LampsRewardAd
import com.lamps.sdk.reward.RewardAdLoadCallback
import com.lamps.sdk.reward.RewardAdShowCallback
import com.lamps.sdk.webview.LampsWebViewActivity

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.openWebViewButton).setOnClickListener {
            startActivity(
                LampsWebViewActivity.buildIntent(
                    context = this,
                    url = DEMO_URL
                )
            )
        }
        SdkToolsBinder.bind(this, findViewById(R.id.openSdkToolsButton))
    }


    private companion object {
        const val DEMO_URL =
            "https://www.hoorahgo.com/mg/index.html#/play?demo=nba-career-sim"
    }
}
