package com.wakeproof.alarm.domain.model

import java.time.DayOfWeek

/**
 * Domain alarm model, spec 4.1.1. Contains every per-alarm field so later
 * stages (task count, QR/NFC anchor, sleep reminder) never need another
 * Room migration for this table.
 */
data class Alarm(
    val id: Long = 0,
    val hour: Int = DEFAULT_HOUR,
    val minute: Int = DEFAULT_MINUTE,
    /** Empty set means the alarm fires every day. */
    val daysOfWeek: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val isEnabled: Boolean = true,
    /** Spec 4.1.1 default: math + Stroop. */
    val taskTypes: Set<TaskType> = setOf(TaskType.MATH, TaskType.STROOP),
    /** Spec 4.1.1 default: medium (adaptive arrives with v1.1, spec 4.5). */
    val difficulty: Difficulty = Difficulty.MEDIUM,
    /** Spec 4.1.2 default: snooze forbidden. */
    val snoozePolicy: SnoozePolicy = SnoozePolicy.FORBIDDEN,
    /** Spec 4.1.5 default: volume ramp enabled. */
    val isVolumeRampEnabled: Boolean = true,
    /** Spec 4.3 default: recheck enabled. */
    val isRecheckEnabled: Boolean = true,
    /** Spec 4.3 allowed values: 5 / 10 / 15 minutes. */
    val recheckDelayMin: Int = RECHECK_DELAY_DEFAULT_MIN,
    /** Spec 4.2.2: 1-4 cognitive tasks per session, default 2. */
    val taskCount: Int = TASK_COUNT_DEFAULT,
    /** Spec 4.1.1 / 4.2.1: optional QR/NFC anchor. */
    val anchorEnabled: Boolean = false,
    /** Locally stored anchor identifier, null until registered. */
    val anchorId: String? = null,
    /** Spec 4.8.1 default: sleep reminder enabled. */
    val isSleepReminderEnabled: Boolean = true,
    /** Spec 4.8.1 allowed range 6-9 hours, default 8. */
    val sleepTargetHours: Int = SLEEP_TARGET_DEFAULT_HOURS,
    // --- Snooze state (spec 4.1.2), transitions in SnoozeMachine. ---

    /** Epoch ms when the armed snoozed ring must fire; 0 = none armed. */
    val snoozeTargetAt: Long = 0L,
    /** Snooze limit of the current firing already consumed. */
    val isSnoozeUsed: Boolean = false,
    /** Task count fixed for the armed snoozed session; 0 = not set. */
    val snoozeSessionTaskCount: Int = 0,
    /** Difficulty fixed for the armed snoozed session; null = not set. */
    val snoozeSessionDifficulty: Difficulty? = null,
    /** Morning session row the snoozed ring continues; 0 = none. */
    val snoozeSessionId: Long = 0L
) {
    companion object {
        const val DEFAULT_HOUR = 7
        const val DEFAULT_MINUTE = 0
        const val RECHECK_DELAY_DEFAULT_MIN = 10
        val RECHECK_DELAY_ALLOWED_MIN = setOf(5, 10, 15)
        const val TASK_COUNT_DEFAULT = 2
        const val TASK_COUNT_MIN = 1
        const val TASK_COUNT_MAX = 4
        const val SLEEP_TARGET_DEFAULT_HOURS = 8
    }
}
