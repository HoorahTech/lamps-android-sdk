package com.lamps.sdk.webview.bridge.network

import android.net.Uri
import android.webkit.CookieManager
import com.lamps.sdk.utils.HttpUtils
import com.lamps.sdk.utils.SdkLog
import com.lamps.sdk.webview.LampsWebView
import com.lamps.sdk.webview.bridge.EMPTY_JSON_OBJ
import com.lamps.sdk.webview.bridge.LampsAbility
import com.lamps.sdk.webview.bridge.LampsNativeCallback
import com.lamps.sdk.webview.bridge.generateResult
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

/**
 * Network ability that uses raw HttpURLConnection (no OkHttp dependency).
 */
internal class NetworkAbility : LampsAbility {
    override val names: Array<String> = arrayOf(METHOD_COMMON_REQUEST)

    private val executor = Executors.newCachedThreadPool()

    override fun executeAsync(
        webView: LampsWebView,
        methodName: String,
        params: JSONObject,
        callbackId: String?,
        callback: LampsNativeCallback
    ) {
        val url = params.optString("url")
        val data = params.optJSONObject("data")
        val method = params.optString("method")
        val header = params.optJSONObject("header")

        if (url.isNullOrEmpty() || method.isNullOrEmpty()) {
            callback.callback(
                generateResult(EMPTY_JSON_OBJ, NetworkErrCode.JSON_PARSE_FAIL, "url or method is invalid"),
                callbackId
            )
            return
        }

        executor.execute {
            try {
                performRequest(webView, url, method, data, header, callback, callbackId)
            } catch (error: Exception) {
                SdkLog.e("network ability failed: ${error.message}", error)
                callback.callback(
                    generateResult(EMPTY_JSON_OBJ, NetworkErrCode.networkThrowableToCode(error), error.message.orEmpty()),
                    callbackId
                )
            }
        }
    }

    private fun performRequest(
        webView: LampsWebView,
        url: String,
        method: String,
        data: JSONObject?,
        header: JSONObject?,
        callback: LampsNativeCallback,
        callbackId: String?
    ) {
        var contentType: String? = null
        header?.keys()?.forEach { key ->
            if ("Content-Type".equals(key, true)) {
                contentType = header.optString(key)
            }
        }

        val isGet = method.lowercase() == "get"

        if (!isGet && contentType.isNullOrEmpty()) {
            callback.callback(
                generateResult(EMPTY_JSON_OBJ, NetworkErrCode.JSON_PARSE_FAIL, "contentType is invalid"),
                callbackId
            )
            return
        }

        // Build the target URL with query params for GET
        val targetUrl = if (isGet) {
            buildUrlWithQuery(url, data)
        } else {
            url
        }

        val connection = URL(targetUrl).openConnection() as HttpURLConnection
        try {
            connection.apply {
                HttpUtils.attachDebugSsl(this)
                requestMethod = if (isGet) "GET" else "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doInput = true
                instanceFollowRedirects = false

                // Apply headers (excluding Referer and Content-Type which is set separately)
                header?.keys()?.forEach { key ->
                    if (!key.equals("Referer", true) && !key.equals("Content-Type", true)) {
                        setRequestProperty(key, header.optString(key))
                    }
                }
                contentType?.let { setRequestProperty("Content-Type", it) }

                // Sync cookies from Android's CookieManager
                val cookieManager = CookieManager.getInstance()
                val cookies = cookieManager.getCookie(url)
                if (!cookies.isNullOrEmpty()) {
                    setRequestProperty("Cookie", cookies)
                }
            }

            // Write body for POST
            if (!isGet) {
                connection.doOutput = true
                val body = if (contentType?.contains("application/x-www-form-urlencoded", ignoreCase = true) == true) {
                    buildFormBody(data)
                } else {
                    (data ?: JSONObject()).toString()
                }
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(body)
                }
            }

            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage ?: ""

            // Save cookies from response
            val setCookieHeader = connection.getHeaderField("Set-Cookie")
            if (!setCookieHeader.isNullOrEmpty()) {
                CookieManager.getInstance().setCookie(url, setCookieHeader)
            }

            val responseBody = readBody(connection)

            val result = JSONObject()
                .put("status", responseCode)
                .put("statusText", responseMessage)
                .put("data", Uri.encode(responseBody, "UTF-8"))
            callback.callback(result, callbackId)
        } catch (error: Exception) {
            SdkLog.e("network ability request failed: ${error.message}", error)
            callback.callback(
                generateResult(EMPTY_JSON_OBJ, NetworkErrCode.networkThrowableToCode(error), error.message.orEmpty()),
                callbackId
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun buildUrlWithQuery(baseUrl: String, data: JSONObject?): String {
        if (data == null || data.length() == 0) return baseUrl
        val queryString = data.keys().asSequence().joinToString("&") { key ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(data.optString(key), "UTF-8")}"
        }
        val separator = if (baseUrl.contains("?")) "&" else "?"
        return "$baseUrl$separator$queryString"
    }

    private fun buildFormBody(data: JSONObject?): String {
        if (data == null || data.length() == 0) return ""
        return data.keys().asSequence().joinToString("&") { key ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(data.optString(key), "UTF-8")}"
        }
    }

    private fun readBody(connection: HttpURLConnection): String {
        val stream = try {
            connection.inputStream
        } catch (e: Exception) {
            connection.errorStream
        } ?: return ""
        return stream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
    }

    private companion object {
        const val METHOD_COMMON_REQUEST = "lamps.common.request"
        const val CONNECT_TIMEOUT_MS = 20_000
        const val READ_TIMEOUT_MS = 20_000
    }
}
