package com.lamps.sdk.webview.bridge.network

import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLProtocolException

internal object NetworkErrCode {
    const val UNKNOWN = 100
    const val JSON_PARSE_FAIL = 801
    const val REQUEST_TIMEOUT = -1001
    const val REQUEST_SSL_ERROR = -1200
    const val REQUEST_NET_UNREACHABLE = -1009
    const val REQUEST_UNKNOWN_HOST = -1000

    fun networkThrowableToCode(t: Throwable): Int {
        return when (t) {
            is TimeoutException,
            is SocketTimeoutException -> REQUEST_TIMEOUT

            is SSLProtocolException,
            is SSLHandshakeException -> REQUEST_SSL_ERROR

            is UnknownHostException -> REQUEST_UNKNOWN_HOST

            is SocketException,
            is ConnectException -> REQUEST_NET_UNREACHABLE

            else -> UNKNOWN
        }
    }
}
