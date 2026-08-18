package com.lamps.sdk.data.init

import android.content.Context
import com.lamps.sdk.config.LampsConfig

internal object AppInitDataCache {
    private val PREFS_NAME = "lamps_sdk"
    private val KEY_APP_INIT_DATA = "app_init_data"
    private val prefs by lazy {
        LampsConfig.current?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun read(): String? = prefs?.getString(KEY_APP_INIT_DATA, null)

    fun write(raw: String) {
        prefs?.edit()?.putString(KEY_APP_INIT_DATA, raw)?.commit()
    }
}
