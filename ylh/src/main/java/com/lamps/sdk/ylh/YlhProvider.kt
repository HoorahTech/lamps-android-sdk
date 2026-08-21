package com.lamps.sdk.ylh

import com.lamps.sdk.data.sdk.provider.ISdkProvider
import com.lamps.sdk.provider.LampsProvider

class YlhProvider : LampsProvider() {
    override fun createProvider(): ISdkProvider = YLHSdkProvider()
}
