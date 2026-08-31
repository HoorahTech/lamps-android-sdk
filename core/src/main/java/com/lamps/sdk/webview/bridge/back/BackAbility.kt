package com.lamps.sdk.webview.bridge.back

import com.lamps.sdk.webview.LampsWebView
import com.lamps.sdk.webview.bridge.EMPTY_JSON_OBJ
import com.lamps.sdk.webview.bridge.LampsAbility
import com.lamps.sdk.webview.bridge.LampsNativeCallback
import com.lamps.sdk.webview.bridge.generateResult
import org.json.JSONObject

/**
 * Page back / close ability aligned with heroes `comp_basic_webview` BackAbility.
 *
 * - [BACK]: H5 asks native to close the current Activity
 * - [MARK_H5_BACK]: H5 takes over the system back button for the host Activity
 * - [SEND_BACK]: native-to-H5 event when the user presses back after marking
 */
internal class BackAbility : LampsAbility {
    override val names: Array<String> = arrayOf(BACK, MARK_H5_BACK)

    override fun executeAsync(
        webView: LampsWebView,
        methodName: String,
        params: JSONObject,
        callbackId: String?,
        callback: LampsNativeCallback
    ) {
        when (methodName) {
            BACK -> BackAbilityUtil.closePage(webView)
            MARK_H5_BACK -> BackAbilityUtil.markH5Back(webView)
        }
        callback.callback(generateResult(EMPTY_JSON_OBJ), callbackId)
    }

    internal companion object {
        const val BACK = "lamps.ui.pageclose"
        const val MARK_H5_BACK = "lamps.common.markh5back"
        const val SEND_BACK = "lamps.common.onback"
    }
}
