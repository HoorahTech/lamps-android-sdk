package com.lamps.sdk.core

import android.os.SystemClock

internal object SdkInitMetrics {
    private val records = LinkedHashMap<String, MutableTimingRecord>()

    @Synchronized
    fun reset() {
        records.clear()
    }

    @Synchronized
    fun start(key: String, name: String) {
        records[key] = MutableTimingRecord(
            key = key,
            name = name,
            startTimeMillis = System.currentTimeMillis(),
            startElapsedMillis = SystemClock.elapsedRealtime()
        )
    }

    @Synchronized
    fun end(key: String, result: String) {
        val record = records[key] ?: return
        if (record.endElapsedMillis != null) return
        record.endTimeMillis = System.currentTimeMillis()
        record.endElapsedMillis = SystemClock.elapsedRealtime()
        record.result = result
    }

    @Synchronized
    fun snapshots(): List<SdkInitTiming> {
        val nowElapsed = SystemClock.elapsedRealtime()
        return records.values.map { record ->
            val endElapsed = record.endElapsedMillis
            SdkInitTiming(
                key = record.key,
                name = record.name,
                startTimeMillis = record.startTimeMillis,
                endTimeMillis = record.endTimeMillis,
                durationMillis = (endElapsed ?: nowElapsed) - record.startElapsedMillis,
                result = record.result ?: RESULT_RUNNING
            )
        }
    }

    private data class MutableTimingRecord(
        val key: String,
        val name: String,
        val startTimeMillis: Long,
        val startElapsedMillis: Long,
        var endTimeMillis: Long? = null,
        var endElapsedMillis: Long? = null,
        var result: String? = null
    )

    const val RESULT_SUCCESS = "SUCCESS"
    const val RESULT_FAILED = "FAILED"
    const val RESULT_SKIPPED = "SKIPPED"
    const val RESULT_CACHE_HIT = "CACHE_HIT"
    const val RESULT_CACHE_MISS = "CACHE_MISS"
    const val RESULT_RUNNING = "RUNNING"
}

internal data class SdkInitTiming(
    val key: String,
    val name: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long?,
    val durationMillis: Long,
    val result: String
)
