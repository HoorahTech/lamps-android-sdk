package com.hupu.games

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import com.lamps.sdk.LampsSdk
import com.lamps.sdk.config.GameCenterConfig
import com.lamps.sdk.config.NightMode

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SdkToolsBinder.bind(this, findViewById(R.id.openSdkToolsButton))

        findViewById<Button>(R.id.navigateGameCenterButton).setOnClickListener {
            LampsSdk.navigateToGameCenter(this, selectedGameCenterConfig())
        }

        findViewById<Button>(R.id.navigateGameButton).setOnClickListener {
            val gameId = findViewById<EditText>(R.id.gameIdInput).text?.toString().orEmpty()
            LampsSdk.navigateToGame(this, gameId)
        }

        findViewById<Button>(R.id.getGameCenterFragmentButton).setOnClickListener {
            startActivity(Intent(this, DemoTabActivity::class.java))
        }
    }

    private fun selectedGameCenterConfig(): GameCenterConfig {
        val nightMode = when (findViewById<RadioGroup>(R.id.gameCenterNightModeGroup).checkedRadioButtonId) {
            R.id.nightModeDay -> NightMode.DAY
            R.id.nightModeNight -> NightMode.NIGHT
            else -> null
        }
        return demoGameCenterConfig(nightMode)
    }
}

internal fun demoGameCenterConfig(nightMode: NightMode? = null): GameCenterConfig {
    return GameCenterConfig.Builder()
        .apply { if (nightMode != null) setNightMode(nightMode) }
        .build()
}
