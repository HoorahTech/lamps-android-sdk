package com.lamps.sdk.data.behavior

import com.lamps.sdk.config.SdkConfig
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

internal data class BehaviorEventRecord(
    val id: Long,
    val event: String,
    val pageType: String,
    val data: String,
    val trackCustomData: Map<String, String>,
    val timeMillis: Long,
)

/** debug 工具里回看 [com.lamps.sdk.core.CoreBehaviorCallback] 收到的行为事件。 */
internal object BehaviorEventRecorder {
    private const val MAX_RECORDS = 100
    private val nextId = AtomicLong(1)
    private val records = CopyOnWriteArrayList<BehaviorEventRecord>()

    fun snapshots(): List<BehaviorEventRecord> {
        if (!isDebug()) return emptyList()
        return records.toList()
    }

    fun record(
        event: String,
        pageType: String,
        data: String,
        trackCustomData: Map<String, String>,
    ) {
        if (!isDebug()) return
        records.add(
            BehaviorEventRecord(
                id = nextId.getAndIncrement(),
                event = event,
                pageType = pageType,
                data = data,
                trackCustomData = LinkedHashMap(trackCustomData),
                timeMillis = System.currentTimeMillis(),
            )
        )
        trim()
    }

    private fun isDebug(): Boolean = SdkConfig.current?.debug == true

    private fun trim() {
        val overflow = records.size - MAX_RECORDS
        if (overflow > 0) {
            records.subList(0, overflow).clear()
        }
    }
}
