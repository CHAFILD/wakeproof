package com.wakeproof.alarm.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room representation of one ringing session (spec 4.1.4 logging and
 * section 6). Enum result stored as its name; per-task results as a
 * compact "TYPE|timeMs|mistakes" ';'-separated string (see
 * TaskResultsCodec) - no JSON dependency is allowed at this stage.
 */
@Entity(
    tableName = TaskSessionEntity.TABLE_NAME,
    indices = [Index("alarmId")]
)
data class TaskSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alarmId: Long,
    /** Epoch ms. */
    val startedAt: Long,
    /** Epoch ms. */
    val finishedAt: Long,
    /** One of TaskSessionResult names. */
    val result: String,
    val mistakes: Int,
    val durationMs: Long,
    /** Encoded solved task blocks, oldest first. */
    val taskResults: String
) {
    companion object {
        const val TABLE_NAME = "task_sessions"
    }
}
