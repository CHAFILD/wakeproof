package com.wakeproof.alarm.domain.session

import com.wakeproof.alarm.domain.model.Alarm
import com.wakeproof.alarm.domain.model.Difficulty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Effective session parameters (spec 4.2.1 / 4.2.2 / 4.1.2): HARDCORE
 * always rings with 4 tasks, an armed snoozed ring uses the parameters
 * fixed at snooze time, everything else uses the alarm configuration.
 */
class SessionParamsResolverTest {

    @Test
    fun `regular alarm uses its configured task count and difficulty`() {
        val params = SessionParamsResolver.resolve(
            Alarm(id = 1L, taskCount = 2, difficulty = Difficulty.EASY)
        )
        assertEquals(2, params.taskCount)
        assertEquals(Difficulty.EASY, params.difficulty)
    }

    @Test
    fun `hardcore always rings with four tasks regardless of configuration`() {
        val params = SessionParamsResolver.resolve(
            Alarm(id = 1L, taskCount = 1, difficulty = Difficulty.HARDCORE)
        )
        assertEquals(4, params.taskCount, "Spec 4.2.1: hardcore = 4 tasks")
        assertEquals(Difficulty.HARDCORE, params.difficulty)
    }

    @Test
    fun `armed snoozed ring uses the parameters fixed at snooze time`() {
        val params = SessionParamsResolver.resolve(
            Alarm(id = 1L, taskCount = 2, difficulty = Difficulty.EASY).copy(
                snoozeSessionTaskCount = 3,
                snoozeSessionDifficulty = Difficulty.MEDIUM
            )
        )
        assertEquals(3, params.taskCount)
        assertEquals(Difficulty.MEDIUM, params.difficulty)
    }

    @Test
    fun `snoozed ring without fixed difficulty falls back to the alarm difficulty`() {
        val params = SessionParamsResolver.resolve(
            Alarm(id = 1L, taskCount = 3, difficulty = Difficulty.MEDIUM).copy(
                snoozeSessionTaskCount = 3,
                snoozeSessionDifficulty = null
            )
        )
        assertEquals(3, params.taskCount)
        assertEquals(Difficulty.MEDIUM, params.difficulty)
    }
}
