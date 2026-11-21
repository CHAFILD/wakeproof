package com.wakeproof.alarm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room representation of an alarm (spec 4.1.1). Every per-alarm field of
 * the spec ships from day one so no migration is ever needed for missing
 * columns: enum sets are stored as bitmasks, enums as names.
 */
@Entity(tableName = AlarmEntity.TABLE_NAME)
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    /** Bitmask over java.time.DayOfWeek: bit (value - 1), Monday = bit 0. */
    val daysOfWeekMask: Int,
    val isEnabled: Boolean,
    /** Bitmask over TaskType ordinals: MATH = 1, MEMORY = 2, STROOP = 4. */
    val taskTypesMask: Int,
    /** One of Difficulty names. */
    val difficulty: String,
    /** One of SnoozePolicy names (spec 4.1.2). */
    val snoozePolicy: String,
    /** Spec 4.1.5 volume ramp. */
    val isVolumeRampEnabled: Boolean,
    /** Spec 4.3 recheck ring. */
    val isRecheckEnabled: Boolean,
    /** Spec 4.3 allowed values: 5 / 10 / 15. */
    val recheckDelayMin: Int,
    /** Spec 4.2.2: 1-4 tasks per session. */
    val taskCount: Int,
    /** Spec 4.1.1 / 4.2.1 QR/NFC anchor. */
    val anchorEnabled: Boolean,
    /** Locally stored anchor identifier, null until registered. */
    val anchorId: String?,
    /** Spec 4.8.1 sleep reminder. */
    val isSleepReminderEnabled: Boolean,
    /** Spec 4.8.1 target sleep hours, 6-9. */
    val sleepTargetHours: Int
) {
    companion object {
        const val TABLE_NAME = "alarms"
    }
}
