package com.hupu.games

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.lamps.sdk.LampsSdk

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SdkToolsBinder.bind(this, findViewById(R.id.openSdkToolsButton))

        findViewById<Button>(R.id.navigateGameCenterButton).setOnClickListener {
            LampsSdk.navigateToGameCenter(this)
        }

        findViewById<Button>(R.id.navigateGameButton).setOnClickListener {
            val gameId = findViewById<EditText>(R.id.gameIdInput).text?.toString().orEmpty()
            LampsSdk.navigateToGame(this, gameId)
        }

        findViewById<Button>(R.id.getGameCenterFragmentButton).setOnClickListener {
            startActivity(Intent(this, DemoTabActivity::class.java))
        }
    }
}
