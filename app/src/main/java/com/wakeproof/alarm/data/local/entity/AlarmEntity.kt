package com.wakeproof.alarm.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room representation of an alarm (spec 4.1.1). Every per-alarm field of
 * the spec ships from day one so no migration is ever needed for missing
 * columns: enum sets are stored as bitmasks, enums as names.
 *
 * The snooze-state columns (spec 4.1.2, added in stage 5 / schema v4)
 * carry the armed snoozed ring across process death and reboots; their
 * transitions live in SnoozeMachine.
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
    val sleepTargetHours: Int,
    /** Epoch ms when the armed snoozed ring must fire; 0 = none armed. */
    @ColumnInfo(defaultValue = "0") val snoozeTargetAt: Long = 0L,
    /** Snooze limit of the current firing already consumed. */
    @ColumnInfo(defaultValue = "0") val isSnoozeUsed: Boolean = false,
    /** Task count fixed for the armed snoozed session; 0 = not set. */
    @ColumnInfo(defaultValue = "0") val snoozeSessionTaskCount: Int = 0,
    /** Difficulty name fixed for the armed snoozed session; null = not set. */
    val snoozeSessionDifficulty: String? = null,
    /** Morning session row the snoozed ring continues; 0 = none. */
    @ColumnInfo(defaultValue = "0") val snoozeSessionId: Long = 0L
) {
    companion object {
        const val TABLE_NAME = "alarms"
    }
}
