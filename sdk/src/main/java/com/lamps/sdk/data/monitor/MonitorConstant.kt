package com.lamps.sdk.data.monitor

import android.os.Build
import com.lamps.sdk.BuildConfig
import com.lamps.sdk.config.LampsConfig
import com.lamps.sdk.utils.DeviceUtils

internal object MonitorConstant {
    //内部自动获取
    const val TS = "__TS__"
    const val UA = "__UA__"
    const val IP = "__IP__"
    const val MAC = "__MAC__"
    const val SW = "__SW__"
    const val SH = "__SH__"
    const val IMEI = "__IMEI__"
    const val OS = "__OS__"
    const val ANDROID_ID = "__ANDROIDID__"
    const val OAID = "__OAID__"
    const val APP_ID = "__APPID__"
    const val SDK_VERSION = "__SDK_VERSION__"
    const val PHONE_BRAND = "__PHONE_BRAND__"
    const val NETWORK = "__NETWORK__"

    //动态变化
    const val REQUEST_ID = "__REQUEST_ID__"
    const val FORWARD_SOURCE = "__FORWARD_SOURCE__"

    const val PRICE = "__PRICE__"
    const val UNION_NAME = "__UNION_NAME__"
    const val SLOT_ID = "__SLOTID__"

    const val REM_SIGN = "__REM_SIGN__"
    const val ACTION = "__ACTION__"
    const val ACTION_REWARD_SUCCESS = "30"
}