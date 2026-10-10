package com.lamps.sdk.core

/**
 * Receives every tracking event the SDK submits.
 * [payload] is the plaintext JSON posted to the report API, before gzip and Base64.
 */
fun interface TrackCallback {
    fun onTrack(action: String, payload: String)
}
