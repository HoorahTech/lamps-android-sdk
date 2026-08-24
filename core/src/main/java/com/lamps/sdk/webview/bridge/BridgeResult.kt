package com.lamps.sdk.webview.bridge

import org.json.JSONObject

internal val EMPTY_JSON_OBJ = JSONObject()

internal fun generateResult(
    innerResult: Any?,
    code: Int = 0,
    message: String = ""
): JSONObject {
    return JSONObject()
        .put("code", code)
        .put("msg", message)
        .put("data", innerResult)
}
