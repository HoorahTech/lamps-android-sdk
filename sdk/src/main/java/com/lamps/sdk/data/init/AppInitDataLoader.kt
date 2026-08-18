package com.lamps.sdk.data.init

import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.utils.DeviceUtils
import com.lamps.sdk.utils.SdkLog
import org.json.JSONObject

internal object AppInitDataLoader {

    fun load(config: LampsConfig): Boolean {
        applyCache(config)
        applyRemote(config)
        return config.appInitData != null
    }

    private fun applyCache(config: LampsConfig) {
        val raw = AppInitDataCache.read()
        if (raw.isNullOrBlank()) {
            SdkLog.d("no local appInitData cache")
            return
        }
        AppInitDataParser.parse(raw).fold(
            onSuccess = { response ->
                config.appInitData = response
                SdkLog.d("loaded appInitData from cache")
            },
            onFailure = { error ->
                SdkLog.w("appInitData cache parse failed: ${error.message}")
            }
        )
    }

    private fun applyRemote(config: LampsConfig) {
        val context = config.applicationContext ?: return
        val request = AppInitRequest(
            androidId = DeviceUtils.androidId(context),
            appId = config.appId,
            version = DeviceUtils.appVersion(context),
            oaid = config.resolveOaid(),
            os = "Android"
        )
        HttpAppInitDataRepository().getAppInitData(request).fold(
            onSuccess = { snapshot ->
                AppInitDataCache.write(snapshot.raw)
                config.appInitData = snapshot.data
                SdkLog.d("appInitData updated from server")
            },
            onFailure = { error ->
                SdkLog.e("fetch appInitData failed: ${error.message}")
            }
        )
    }
}
