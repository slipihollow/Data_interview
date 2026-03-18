package com.datainterview.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activations")
data class Activation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,           // epoch millis
    val endTime: Long? = null,     // epoch millis, null if ongoing
    val scheduledStart: Long? = null,
    val scheduledEnd: Long? = null,
    val status: String = STATUS_ACTIVE,  // "active", "completed", "scheduled"
    val csvFilePath: String? = null,
    val uploadStatus: String? = null,    // null, "pending", "success", "failed"
    val eventCount: Int = 0
) {
    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_SCHEDULED = "scheduled"
    }
}
