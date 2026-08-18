package com.lamps.demo

import android.app.Application
import android.util.Log
import com.lamps.sdk.LampsSdk
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.core.InitCallback

class DemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val accepted = LampsSdk.init(
            this,
            LampsConfig.Builder()
                .appId("demo_app_id")
                .setOaidProvider { DEMO_OAID }
                .setDebug(true)
                .setCustomData(mapOf("source" to "demo"))
                .build()
        )
        Log.i(TAG, "init accepted=$accepted")

        LampsSdk.startAsync(object : InitCallback {
            override fun success() {
                Log.i(TAG, "start success, ready=${LampsSdk.isSdkReady()}")
            }

            override fun fail(code: Int, message: String?) {
                Log.e(TAG, "start fail code=$code message=$message")
            }
        })
    }

    companion object {
        const val TAG = "LampsDemo"
        const val DEMO_OAID = "demo-oaid-from-media"
    }
}
