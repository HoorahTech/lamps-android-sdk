package com.lamps.sdk.pangle

import com.lamps.sdk.data.sdk.provider.ISdkProvider
import com.lamps.sdk.provider.LampsProvider

class PangleProvider : LampsProvider() {
    override fun createProvider(): ISdkProvider = TTSdkProvider()
}
