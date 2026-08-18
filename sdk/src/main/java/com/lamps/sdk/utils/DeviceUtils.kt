package com.lamps.sdk.utils

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

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
}
