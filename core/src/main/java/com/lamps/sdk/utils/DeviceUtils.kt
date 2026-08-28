package com.lamps.sdk.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.webkit.WebSettings
import com.lamps.sdk.config.SdkConfig
import java.net.NetworkInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal object DeviceUtils {
    private const val PREFS_NAME = "lamps_sdk"
    private const val KEY_ANDROID_ID = "android_id"
    private const val KEY_USER_AGENT = "user_agent"
    private const val KEY_OAID = "oaid"
    private const val KEY_APP_ID = "app_id"
    private const val KEY_PHONE_BRAND = "phone_brand"
    private const val KEY_IMEI = "imei"
    private const val INVALID_MAC = "02:00:00:00:00:00"
    private const val NETWORK_WIFI = "wifi"
    private const val NETWORK_2G = "2g"
    private const val NETWORK_3G = "3g"
    private const val NETWORK_4G = "4g"
    private const val NETWORK_5G = "5g"
    private const val NETWORK_UNKNOWN = "unknown"

    private val memory = ConcurrentHashMap<String, String>()

    @Volatile
    private var memoryMac: String? = null

    fun androidId(context: Context): String {
        return cached(context, KEY_ANDROID_ID) {
            try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                    ?.takeIf { it.isNotEmpty() }
                    ?: UUID.randomUUID().toString()
            } catch (_: Throwable) {
                UUID.randomUUID().toString()
            }
        }
    }

    fun userAgent(context: Context?): String {
        return cached(context, KEY_USER_AGENT) {
            try {
                if (context != null) {
                    WebSettings.getDefaultUserAgent(context)
                } else {
                    System.getProperty("http.agent").orEmpty()
                }
            } catch (_: Throwable) {
                System.getProperty("http.agent").orEmpty()
            }
        }
    }

    fun oaid(context: Context?): String {
        return cached(context, KEY_OAID) {
            SdkConfig.current?.resolveOaid().orEmpty()
        }
    }

    fun appId(context: Context?): String {
        return SdkConfig.current?.appId.orEmpty()
    }

    fun phoneBrand(context: Context?): String {
        return cached(context, KEY_PHONE_BRAND) {
            Build.BRAND.orEmpty()
        }
    }

    fun imei(context: Context?): String {
        return cached(context, KEY_IMEI) { fetchImei(context) }
    }

    fun mac(context: Context?): String {
        memoryMac?.let { return it }
        val value = fetchMac(context)
        memoryMac = value
        return value
    }

    fun appVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }
    }

    fun statusBarHeight(context: Context?): Int {
        if (context == null) return 0
        return try {
            val resources = context.resources
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
        } catch (_: Throwable) {
            0
        }
    }

    fun networkType(context: Context?): String {
        if (context == null) return NETWORK_UNKNOWN
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return NETWORK_UNKNOWN
            val network = cm.activeNetwork ?: return NETWORK_UNKNOWN
            val caps = cm.getNetworkCapabilities(network) ?: return NETWORK_UNKNOWN
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NETWORK_WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> cellularNetworkType(context)
                else -> NETWORK_UNKNOWN
            }
        } catch (_: Throwable) {
            NETWORK_UNKNOWN
        }
    }

    @Suppress("DEPRECATION")
    private fun cellularNetworkType(context: Context): String {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return NETWORK_UNKNOWN
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            telephony.dataNetworkType
        } else {
            telephony.networkType
        }
        return when (type) {
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_IDEN,
            TelephonyManager.NETWORK_TYPE_GSM -> NETWORK_2G

            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_TD_SCDMA,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_EHRPD -> NETWORK_3G

            TelephonyManager.NETWORK_TYPE_LTE -> NETWORK_4G
            TelephonyManager.NETWORK_TYPE_NR -> NETWORK_5G
            else -> NETWORK_UNKNOWN
        }
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun fetchImei(context: Context?): String {
        if (context == null || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return ""
        if (!hasPermission(context, Manifest.permission.READ_PHONE_STATE)) return ""
        return try {
            val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                ?: return ""
            val value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telephony.imei
            } else {
                @Suppress("DEPRECATION")
                telephony.deviceId
            }
            value.orEmpty()
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * 对齐虎扑广告 MAC 策略：
     * Android 10 及以下读网卡 MAC；Android 11 及以上有定位权限时读路由 BSSID，否则占位。
     */
    @SuppressLint("HardwareIds", "MissingPermission")
    private fun fetchMac(context: Context?): String {
        if (context == null) return INVALID_MAC
        return try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                if (hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    routerMac(context) ?: INVALID_MAC
                } else {
                    INVALID_MAC
                }
            } else {
                wifiInterfaceMac() ?: INVALID_MAC
            }
        } catch (_: Throwable) {
            INVALID_MAC
        }
    }

    private fun wifiInterfaceMac(): String? {
        return NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .firstNotNullOfOrNull { nif ->
                val name = nif.name.orEmpty()
                if (!name.startsWith("wlan") && name != "eth0") return@firstNotNullOfOrNull null
                formatMac(nif.hardwareAddress)
            }
    }

    @Suppress("DEPRECATION")
    private fun routerMac(context: Context): String? {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null
        return formatMacAddress(wifi.connectionInfo?.bssid)
    }

    private fun formatMac(bytes: ByteArray?): String? {
        if (bytes == null || bytes.isEmpty()) return null
        return formatMacAddress(bytes.joinToString(":") { "%02x".format(it) })
    }

    private fun formatMacAddress(mac: String?): String? {
        val value = mac?.trim().orEmpty()
        if (value.isEmpty() || value == INVALID_MAC || value == "00:00:00:00:00:00") return null
        return value
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun cached(context: Context?, key: String, fetch: () -> String): String {
        memory[key]?.let { return it }
        val prefs = context?.applicationContext
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs?.getString(key, null)
        if (!stored.isNullOrEmpty()) {
            memory[key] = stored
            return stored
        }
        val value = fetch()
        if (value.isNotEmpty()) {
            memory[key] = value
            prefs?.edit()?.putString(key, value)?.apply()
        }
        return value
    }
}
