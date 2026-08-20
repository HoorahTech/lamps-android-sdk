package com.lamps.sdk.utils

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.webkit.WebSettings

internal object DeviceUtils {

    fun androidId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        } catch (_: Throwable) {
            ""
        }
    }

    fun appVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }
    }

    fun userAgent(context: Context?): String {
        return try {
            if (context != null) {
                WebSettings.getDefaultUserAgent(context)
            } else {
                System.getProperty("http.agent").orEmpty()
            }
        } catch (_: Throwable) {
            System.getProperty("http.agent").orEmpty()
        }
    }

    fun networkType(context: Context?): String {
        if (context == null) return ""
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return ""
            val network = cm.activeNetwork ?: return ""
            val caps = cm.getNetworkCapabilities(network) ?: return ""
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
                else -> ""
            }
        } catch (_: Throwable) {
            ""
        }
    }
}
