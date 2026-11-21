package com.wakeproof.alarm.domain.streak

import com.wakeproof.alarm.domain.model.TaskSession
import com.wakeproof.alarm.domain.model.TaskSessionResult

/**
 * Streak over the persisted ringing-session history (spec 4.4).
 *
 * A streak is the number of consecutive CLEAN mornings. Any other
 * outcome breaks it: SNOOZED, EMERGENCY, MISSED, RECHECK_FAILED.
 * A morning with no session at all (alarms disabled, vacation) neither
 * grows nor breaks the streak - the spec resets the streak only on the
 * listed negative outcomes.
 *
 * Input order does not matter: sessions are sorted by start time
 * (stable) before counting. With two sessions started at the same
 * millisecond the insertion order is preserved via the session id.
 */
object StreakCalculator {

    /** Consecutive CLEAN sessions counted backwards from the newest. */
    fun currentStreak(sessions: List<TaskSession>): Int {
        val newestFirst = sessions.sortedWith(
            compareByDescending<TaskSession> { it.startedAt }.thenByDescending { it.id }
        )
        var streak = 0
        for (session in newestFirst) {
            if (session.result != TaskSessionResult.CLEAN) break
            streak += 1
        }
        return streak
    }

    /** The longest CLEAN run ever recorded in the history. */
    fun recordStreak(sessions: List<TaskSession>): Int {
        val oldestFirst = sessions.sortedWith(
            compareBy<TaskSession> { it.startedAt }.thenBy { it.id }
        )
        var best = 0
        var run = 0
        for (session in oldestFirst) {
            if (session.result == TaskSessionResult.CLEAN) {
                run += 1
                if (run > best) best = run
            } else {
                run = 0
            }
        }
        return best
    }
}
