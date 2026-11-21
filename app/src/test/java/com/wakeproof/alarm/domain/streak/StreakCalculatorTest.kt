package com.wakeproof.alarm.domain.streak

import com.wakeproof.alarm.domain.model.TaskSession
import com.wakeproof.alarm.domain.model.TaskSessionResult
import com.wakeproof.alarm.domain.model.TaskType
import com.wakeproof.alarm.domain.task.TaskResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Streak calculator (spec 4.4 / section 8 item 4): accrual over
 * consecutive CLEAN mornings and every reset variant (SNOOZED,
 * EMERGENCY, MISSED, RECHECK_FAILED), plus the record streak.
 */
class StreakCalculatorTest {

    private var clock: Long = 1_742_000_000_000L

    private fun session(result: TaskSessionResult): TaskSession {
        val startedAt = clock
        clock += 60_000L
        return TaskSession(
            id = startedAt,
            alarmId = 1L,
            startedAt = startedAt,
            finishedAt = startedAt + 30_000L,
            result = result,
            mistakes = 0,
            durationMs = 30_000L,
            taskResults = listOf(
                TaskResult(timeTakenMs = 30_000L, mistakes = 0, taskType = TaskType.MATH)
            )
        )
    }

    private fun clean() = session(TaskSessionResult.CLEAN)

    // --- Accrual -----------------------------------------------------------

    @Test
    fun `empty history has zero streak`() {
        assertEquals(0, StreakCalculator.currentStreak(emptyList()))
        assertEquals(0, StreakCalculator.recordStreak(emptyList()))
    }

    @Test
    fun `single clean morning gives streak of one`() {
        assertEquals(1, StreakCalculator.currentStreak(listOf(clean())))
    }

    @Test
    fun `three clean mornings in a row give streak of three`() {
        val history = listOf(clean(), clean(), clean())
        assertEquals(3, StreakCalculator.currentStreak(history))
        assertEquals(3, StreakCalculator.recordStreak(history))
    }

    // --- Reset variants ------------------------------------------------------

    @Test
    fun `snoozed morning resets the streak`() {
        val history = listOf(clean(), clean(), session(TaskSessionResult.SNOOZED))
        assertEquals(0, StreakCalculator.currentStreak(history))
    }

    @Test
    fun `emergency morning resets the streak`() {
        val history = listOf(clean(), session(TaskSessionResult.EMERGENCY))
        assertEquals(0, StreakCalculator.currentStreak(history))
    }

    @Test
    fun `missed morning resets the streak`() {
        val history = listOf(clean(), session(TaskSessionResult.MISSED))
        assertEquals(0, StreakCalculator.currentStreak(history))
    }

    @Test
    fun `recheck failed morning resets the streak`() {
        val history = listOf(clean(), session(TaskSessionResult.RECHECK_FAILED))
        assertEquals(0, StreakCalculator.currentStreak(history))
    }

    @Test
    fun `streak restarts after a reset - only clean mornings after it count`() {
        val history = listOf(
            clean(),
            session(TaskSessionResult.SNOOZED),
            clean(),
            clean()
        )
        assertEquals(2, StreakCalculator.currentStreak(history))
    }

    @Test
    fun `a day without an alarm neither grows nor breaks the streak`() {
        val history = listOf(clean(), clean()) // gap with no sessions
        assertEquals(2, StreakCalculator.currentStreak(history))
    }

    // --- Record streak ---------------------------------------------------------

    @Test
    fun `record streak keeps the best run across resets`() {
        val history = listOf(
            clean(),
            clean(),
            session(TaskSessionResult.EMERGENCY),
            clean(),
            clean(),
            clean(),
            session(TaskSessionResult.SNOOZED)
        )
        assertEquals(0, StreakCalculator.currentStreak(history))
        assertEquals(3, StreakCalculator.recordStreak(history))
    }

    @Test
    fun `record is at least the current streak`() {
        val history = listOf(clean(), clean(), clean())
        val record = StreakCalculator.recordStreak(history)
        assertEquals(StreakCalculator.currentStreak(history), record)
    }

    // --- Input order robustness ---------------------------------------------

    @Test
    fun `unordered input is sorted by start time`() {
        val first = clean()
        val snoozed = session(TaskSessionResult.SNOOZED)
        val third = clean()
        val fourth = clean()
        // Same history, deliberately scrambled input orders.
        assertEquals(2, StreakCalculator.currentStreak(listOf(snoozed, first, third, fourth)))
        assertEquals(2, StreakCalculator.recordStreak(listOf(third, snoozed, fourth, first)))
    }
}
