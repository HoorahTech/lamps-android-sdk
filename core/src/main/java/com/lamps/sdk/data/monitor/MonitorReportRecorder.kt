package com.lamps.sdk.data.monitor

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import com.lamps.sdk.config.SdkConfig

internal data class MonitorReportRecord(
    val id: Long,
    val event: String,
    val url: String,
    val startTimeMillis: Long,
    val responseCode: Int? = null,
    val error: String? = null,
    val finished: Boolean = false
)

internal object MonitorReportRecorder {
    private const val MAX_RECORDS = 100
    private const val INVALID_ID = -1L
    private val nextId = AtomicLong(1)
    private val records = CopyOnWriteArrayList<MonitorReportRecord>()

    fun snapshots(): List<MonitorReportRecord> {
        if (!isDebug()) return emptyList()
        return records.toList()
    }

    fun begin(event: String, url: String): Long {
        if (!isDebug()) return INVALID_ID
        val id = nextId.getAndIncrement()
        records.add(
            MonitorReportRecord(
                id = id,
                event = event,
                url = url,
                startTimeMillis = System.currentTimeMillis()
            )
        )
        trim()
        return id
    }

    fun complete(id: Long, responseCode: Int?, error: String?) {
        if (id == INVALID_ID || !isDebug()) return
        val index = records.indexOfFirst { it.id == id }
        if (index < 0) return
        records[index] = records[index].copy(
            responseCode = responseCode,
            error = error,
            finished = true
        )
    }

    private fun isDebug(): Boolean = SdkConfig.current?.debug == true

    private fun trim() {
        val overflow = records.size - MAX_RECORDS
        if (overflow > 0) {
            records.subList(0, overflow).clear()
        }
    }
}
