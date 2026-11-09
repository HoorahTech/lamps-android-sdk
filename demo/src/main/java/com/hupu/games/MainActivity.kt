package com.hupu.games

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import com.lamps.sdk.LampsSdk
import com.lamps.sdk.LampsUser
import com.lamps.sdk.config.GameCenterConfig
import com.lamps.sdk.config.NightMode

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SdkToolsBinder.bind(this, findViewById(R.id.openSdkToolsButton))

        findViewById<Button>(R.id.navigateGameCenterButton).setOnClickListener {
            val config = GameCenterConfig.Builder()
                .setNightMode(NightMode.NIGHT)
                .build()
            LampsSdk.navigateToGameCenter(this, config)
        }

        findViewById<Button>(R.id.navigateGameButton).setOnClickListener {
            val gameId = findViewById<EditText>(R.id.gameIdInput).text?.toString().orEmpty()
            LampsSdk.navigateToGame(this, gameId)
        }

        findViewById<Button>(R.id.getGameCenterFragmentButton).setOnClickListener {
            startActivity(Intent(this, DemoTabActivity::class.java))
        }

        findViewById<Button>(R.id.updateUserButton).setOnClickListener {
            val accepted = LampsSdk.updateUser(
                LampsUser.Builder()
                    .setUserId(textOf(R.id.userIdInput))
                    .setNickname(textOf(R.id.nicknameInput))
                    .setAvatar(textOf(R.id.avatarInput))
                    .build()
            )
            Toast.makeText(
                this,
                if (accepted) "已同步登录用户" else "userId 为空，未更新",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<Button>(R.id.clearUserButton).setOnClickListener {
            LampsSdk.clearUser()
            Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show()
        }
    }

    private fun textOf(id: Int): String {
        return findViewById<EditText>(id).text?.toString().orEmpty()
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
        .setHideTitle(true)
        .build()
}
