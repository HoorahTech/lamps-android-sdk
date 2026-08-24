package com.lamps.sdk.utils

import java.io.BufferedReader
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal data class HttpResponse(
    val code: Int,
    val body: String
) {
    val isSuccessful: Boolean get() = code in 200..299
}

/**
 * 公共网络请求。path 可以是相对路径（拼到当前配置域名）或完整 http(s) 地址。
 */
internal object HttpUtils {

    fun get(
        path: String,
        query: Map<String, String?> = emptyMap(),
        headers: Map<String, String> = jsonHeaders()
    ): Result<HttpResponse> {
        return request("GET", path, query = query, headers = headers)
    }

    fun post(
        path: String,
        body: String? = null,
        query: Map<String, String?> = emptyMap(),
        headers: Map<String, String> = jsonHeaders()
    ): Result<HttpResponse> {
        return request("POST", path, query = query, body = body, headers = headers)
    }

    fun request(
        method: String,
        path: String,
        query: Map<String, String?> = emptyMap(),
        body: String? = null,
        headers: Map<String, String> = jsonHeaders(),
        connectTimeoutMs: Int = CONNECT_TIMEOUT_MS,
        readTimeoutMs: Int = READ_TIMEOUT_MS
    ): Result<HttpResponse> {
        return try {
            val url = URL(buildUrl(path, query))
            SdkLog.d("$method $url")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                doInput = true
                headers.forEach { (key, value) -> setRequestProperty(key, value) }
                if (body != null && method != "GET") {
                    doOutput = true
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }
            try {
                val httpCode = connection.responseCode
                val stream = if (httpCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: connection.inputStream
                }
                val responseBody = readBody(stream)
                SdkLog.d("http=$httpCode body=$responseBody")
                Result.success(HttpResponse(httpCode, responseBody))
            } finally {
                connection.disconnect()
            }
        } catch (t: Throwable) {
            SdkLog.e("http request failed: ${t.message}", t)
            Result.failure(t)
        }
    }

    fun buildUrl(path: String, query: Map<String, String?> = emptyMap()): String {
        val base = if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            LampsApiHost.baseUrl().trimEnd('/') + "/" + path.trimStart('/')
        }
        val queryString = query
            .filterValues { !it.isNullOrBlank() }
            .entries
            .joinToString("&") { (key, value) ->
                "$key=${URLEncoder.encode(value, "UTF-8")}"
            }
        if (queryString.isEmpty()) {
            return base
        }
        val separator = if (base.contains("?")) "?" else "?"
        return base + separator + queryString
    }

    fun jsonHeaders(): Map<String, String> {
        return mapOf(
            "Accept" to "application/json",
            "Content-Type" to "application/json"
        )
    }

    private fun readBody(stream: InputStream?): String {
        if (stream == null) return ""
        return stream.bufferedReader().use(BufferedReader::readText)
    }

    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 10_000
}
