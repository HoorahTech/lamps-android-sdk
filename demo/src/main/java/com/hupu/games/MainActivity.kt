package com.hupu.games

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.lamps.sdk.LampsSdk

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SdkToolsBinder.bind(this, findViewById(R.id.openSdkToolsButton))

        findViewById<Button>(R.id.navigateGameCenterButton).setOnClickListener {
            LampsSdk.navigateToGameCenter(this)
        }

        findViewById<Button>(R.id.getGameCenterFragmentButton).setOnClickListener {
            startActivity(Intent(this, DemoTabActivity::class.java))
        }
    }


    private companion object {
        const val DEMO_URL =
            "https://activity-static.hupu.com/colorbox-activities/activity-project-ai-1787297060404/index.html?t=1787557168381"
    }
}
