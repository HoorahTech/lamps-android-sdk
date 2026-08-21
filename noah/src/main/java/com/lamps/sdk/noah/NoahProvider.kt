package com.lamps.sdk.noah

import com.lamps.sdk.data.sdk.provider.ISdkProvider
import com.lamps.sdk.provider.LampsProvider

class NoahProvider : LampsProvider() {
    override fun createProvider(): ISdkProvider = NoahSdkProvider()
}
