package com.wakeproof.alarm.domain.snooze

import com.wakeproof.alarm.domain.model.Alarm
import com.wakeproof.alarm.domain.model.Difficulty
import com.wakeproof.alarm.domain.model.SnoozePolicy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Snooze machine (spec 4.1.2 / section 8 item 2): three policies, the
 * 10-minute interval, the escalation rule (+1 task -> 4-task cap ->
 * one difficulty level up), the once-per-firing limit and its reset,
 * and the HARDCORE override that always forbids snooze.
 */
class SnoozeMachineTest {

    private val now = 1_000_000L

    private fun alarm(
        policy: SnoozePolicy = SnoozePolicy.ONCE,
        difficulty: Difficulty = Difficulty.MEDIUM,
        taskCount: Int = 2
    ) = Alarm(
        id = 1L,
        snoozePolicy = policy,
        difficulty = difficulty,
        taskCount = taskCount
    )

    // --- Availability: three policies -----------------------------------

    @Test
    fun `forbidden policy never allows snooze`() {
        assertFalse(SnoozeMachine.canSnooze(alarm(policy = SnoozePolicy.FORBIDDEN)))
    }

    @Test
    fun `once policy allows the first snooze`() {
        assertTrue(SnoozeMachine.canSnooze(alarm(policy = SnoozePolicy.ONCE)))
    }

    @Test
    fun `once harder policy allows the first snooze`() {
        assertTrue(SnoozeMachine.canSnooze(alarm(policy = SnoozePolicy.ONCE_HARDER)))
    }

    // --- Availability: hardcore override --------------------------------

    @Test
    fun `hardcore forbids snooze even with once policy`() {
        assertFalse(
            SnoozeMachine.canSnooze(alarm(policy = SnoozePolicy.ONCE, difficulty = Difficulty.HARDCORE))
        )
    }

    @Test
    fun `hardcore forbids snooze even with once harder policy`() {
        assertFalse(
            SnoozeMachine.canSnooze(alarm(policy = SnoozePolicy.ONCE_HARDER, difficulty = Difficulty.HARDCORE))
        )
    }

    // --- Availability: once-per-firing limit ----------------------------

    @Test
    fun `used limit blocks the second snooze`() {
        val used = alarm().copy(isSnoozeUsed = true)
        assertFalse(SnoozeMachine.canSnooze(used))
    }

    @Test
    fun `armed snooze blocks another press`() {
        val armed = alarm().copy(snoozeTargetAt = now + SnoozeMachine.INTERVAL_MS)
        assertFalse(SnoozeMachine.canSnooze(armed))
    }

    // --- Interval --------------------------------------------------------

    @Test
    fun `snooze target is exactly 10 minutes ahead`() {
        val decision = SnoozeMachine.decideSnooze(alarm(), now)
        assertEquals(now + SnoozeMachine.INTERVAL_MS, decision.targetAtMillis)
        assertEquals(10L, SnoozeMachine.INTERVAL_MINUTES)
    }

    // --- ONCE: same parameters -------------------------------------------

    @Test
    fun `once snoozes with the same task count and difficulty`() {
        val decision = SnoozeMachine.decideSnooze(
            alarm(policy = SnoozePolicy.ONCE, difficulty = Difficulty.EASY, taskCount = 3),
            now
        )
        assertEquals(3, decision.sessionTaskCount)
        assertEquals(Difficulty.EASY, decision.sessionDifficulty)
    }

    // --- ONCE_HARDER escalation -------------------------------------------

    @Test
    fun `once harder adds one task`() {
        val decision = SnoozeMachine.decideSnooze(
            alarm(policy = SnoozePolicy.ONCE_HARDER, difficulty = Difficulty.MEDIUM, taskCount = 2),
            now
        )
        assertEquals(3, decision.sessionTaskCount)
        assertEquals(Difficulty.MEDIUM, decision.sessionDifficulty)
    }

    @Test
    fun `once harder with three tasks reaches four`() {
        val decision = SnoozeMachine.decideSnooze(
            alarm(policy = SnoozePolicy.ONCE_HARDER, difficulty = Difficulty.EASY, taskCount = 3),
            now
        )
        assertEquals(4, decision.sessionTaskCount)
        assertEquals(Difficulty.EASY, decision.sessionDifficulty)
    }

    @Test
    fun `once harder with four tasks raises difficulty one level`() {
        val decision = SnoozeMachine.decideSnooze(
            alarm(policy = SnoozePolicy.ONCE_HARDER, difficulty = Difficulty.EASY, taskCount = 4),
            now
        )
        assertEquals(4, decision.sessionTaskCount)
        assertEquals(Difficulty.MEDIUM, decision.sessionDifficulty)
    }

    @Test
    fun `once harder medium with four tasks escalates to hardcore`() {
        val decision = SnoozeMachine.decideSnooze(
            alarm(policy = SnoozePolicy.ONCE_HARDER, difficulty = Difficulty.MEDIUM, taskCount = 4),
            now
        )
        assertEquals(4, decision.sessionTaskCount)
        assertEquals(Difficulty.HARDCORE, decision.sessionDifficulty)
    }

    @Test
    fun `hardcore is the escalation ceiling - no difficulty above it`() {
        val decision = SnoozeMachine.decideSnooze(
            alarm(policy = SnoozePolicy.ONCE_HARDER, difficulty = Difficulty.HARDCORE, taskCount = 4),
            now
        )
        assertEquals(4, decision.sessionTaskCount)
        assertEquals(Difficulty.HARDCORE, decision.sessionDifficulty)
    }

    // --- Firing transitions ----------------------------------------------

    @Test
    fun `snoozed ring firing consumes the target but keeps limit and session params`() {
        val armed = alarm(policy = SnoozePolicy.ONCE_HARDER, taskCount = 2).copy(
            snoozeTargetAt = now,
            isSnoozeUsed = true,
            snoozeSessionTaskCount = 3,
            snoozeSessionDifficulty = Difficulty.MEDIUM,
            snoozeSessionId = 42L
        )
        val fired = SnoozeMachine.onSnoozeRingFired(armed)
        assertEquals(0L, fired.snoozeTargetAt, "Only the pending target is consumed")
        assertEquals(3, fired.snoozeSessionTaskCount, "Params stay for the ringing UI to read")
        assertEquals(Difficulty.MEDIUM, fired.snoozeSessionDifficulty)
        assertTrue(fired.isSnoozeUsed, "The limit stays consumed for this firing")
        assertEquals(42L, fired.snoozeSessionId, "The morning session link stays for the merge")
    }

    @Test
    fun `regular firing resets the once-per-firing limit`() {
        val afterSnoozeMorning = alarm().copy(
            snoozeTargetAt = 0L,
            isSnoozeUsed = true,
            snoozeSessionTaskCount = 3,
            snoozeSessionDifficulty = Difficulty.MEDIUM,
            snoozeSessionId = 42L
        )
        val fresh = SnoozeMachine.onRegularRingFired(afterSnoozeMorning)
        assertEquals(0L, fresh.snoozeTargetAt)
        assertFalse(fresh.isSnoozeUsed, "A new firing gets a fresh snooze limit")
        assertEquals(0, fresh.snoozeSessionTaskCount)
        assertNull(fresh.snoozeSessionDifficulty)
        assertEquals(0L, fresh.snoozeSessionId)
        assertTrue(SnoozeMachine.canSnooze(fresh))
    }

    // --- Escalation ladder -------------------------------------------------

    @Test
    fun `difficulty ladder is easy then medium then hardcore`() {
        assertEquals(Difficulty.MEDIUM, Difficulty.EASY.nextHarder())
        assertEquals(Difficulty.HARDCORE, Difficulty.MEDIUM.nextHarder())
        assertEquals(Difficulty.HARDCORE, Difficulty.HARDCORE.nextHarder())
    }
}
