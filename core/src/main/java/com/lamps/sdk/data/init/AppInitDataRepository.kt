package com.lamps.sdk.data.init

import com.lamps.sdk.utils.HttpUtils


internal class HttpAppInitDataRepository  {
     fun getAppInitData(request: AppInitRequest): Result<AppInitDataSnapshot> {
        return HttpUtils.get(
            path = APP_INIT_DATA_PATH,
            query = mapOf(
                "android_id" to request.androidId,
                "appid" to request.appId,
                "version" to request.version,
                "oaid" to request.oaid,
                "os" to request.os
            )
        ).fold(
            onSuccess = { response ->
                if (!response.isSuccessful) {
                    Result.failure(IllegalStateException("http ${response.code}: ${response.body}"))
                } else {
                    AppInitDataParser.parse(response.body).map { data ->
                        AppInitDataSnapshot(data = data, raw = response.body)
                    }
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    private companion object {
        const val APP_INIT_DATA_PATH = "/api/v1/advertisement/config"
    }
}
