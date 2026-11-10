package com.lamps.sdk.alipay

import com.lamps.sdk.pay.PayChannel
import com.lamps.sdk.pay.PayProvider

class AlipayProvider : PayProvider() {
    override fun createChannel(): PayChannel = AlipayPayChannel
}
