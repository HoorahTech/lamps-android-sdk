package com.lamps.sdk.utils

import android.content.Context
import com.lamps.sdk.BuildConfig
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.data.init.AppInitDataCache

internal object LampsApiHost {
    const val DEV_URL = "https://api-dev.hoorahgo.com"
    const val PROD_URL = "https://api.hoorahgo.com"

    private const val PREFS_NAME = "lamps_sdk"
    private const val KEY_API_ENV = "api_env"

    enum class Env(val label: String, val baseUrl: String) {
        DEV("dev", DEV_URL),
        PROD("线上", PROD_URL)
    }

    @Volatile
    private var currentEnv: Env? = null

    fun restore(context: Context) {
        val saved = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_API_ENV, null)
        currentEnv = when (saved) {
            Env.DEV.name -> Env.DEV
            Env.PROD.name -> Env.PROD
            else -> defaultEnv()
        }
    }

    fun current(context: Context? = LampsConfig.current?.applicationContext): Env {
        currentEnv?.let { return it }
        if (context != null) {
            restore(context)
            return currentEnv ?: defaultEnv()
        }
        return defaultEnv()
    }

    fun baseUrl(context: Context? = null): String = current(context).baseUrl

    fun set(context: Context, env: Env) {
        currentEnv = env
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API_ENV, env.name)
            .apply()
        AppInitDataCache.clear()
        SdkLog.d("api env switched to ${env.label} ${env.baseUrl}")
    }

    fun toggle(context: Context): Env {
        val next = if (current(context) == Env.PROD) Env.DEV else Env.PROD
        set(context, next)
        return next
    }

    private fun defaultEnv(): Env {
        return if (BuildConfig.DEBUG) Env.DEV else Env.PROD
    }
}
