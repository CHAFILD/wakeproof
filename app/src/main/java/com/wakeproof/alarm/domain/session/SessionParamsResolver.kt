package com.wakeproof.alarm.domain.session

import com.wakeproof.alarm.domain.model.Alarm
import com.wakeproof.alarm.domain.model.Difficulty

/** Number of cognitive tasks and their difficulty for one ringing session. */
data class SessionParams(
    val taskCount: Int,
    val difficulty: Difficulty
)

/**
 * Resolves the effective session parameters (spec 4.2.1 / 4.2.2):
 *
 * - an armed SNOOZED ring uses the parameters fixed at snooze time
 *   (same params for ONCE, +1 task or one difficulty level up for
 *   ONCE_HARDER - spec 4.1.2);
 * - HARDCORE always rings with 4 tasks regardless of the configured
 *   count (spec 4.2.1 hardcore definition);
 * - anything else uses the alarm's configured values.
 */
object SessionParamsResolver {

    fun resolve(alarm: Alarm): SessionParams {
        val snoozeCount = alarm.snoozeSessionTaskCount
        val snoozeDifficulty = alarm.snoozeSessionDifficulty
        if (snoozeCount > 0) {
            return SessionParams(
                taskCount = snoozeCount.coerceIn(Alarm.TASK_COUNT_MIN, Alarm.TASK_COUNT_MAX),
                difficulty = snoozeDifficulty ?: alarm.difficulty
            )
        }
        if (alarm.difficulty == Difficulty.HARDCORE) {
            return SessionParams(taskCount = Alarm.TASK_COUNT_MAX, difficulty = Difficulty.HARDCORE)
        }
        return SessionParams(
            taskCount = alarm.taskCount.coerceIn(Alarm.TASK_COUNT_MIN, Alarm.TASK_COUNT_MAX),
            difficulty = alarm.difficulty
        )
    }
}
