package com.wakeproof.alarm.domain

import com.wakeproof.alarm.domain.model.Alarm
import com.wakeproof.alarm.domain.model.Difficulty
import com.wakeproof.alarm.domain.model.SnoozePolicy
import com.wakeproof.alarm.domain.model.TaskType
import com.wakeproof.alarm.domain.time.NextTriggerCalculator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * Tests for the next-trigger calculation shown on the alarm list card
 * (spec 4.1.1): today vs. next matching weekday, empty mask = daily,
 * disabled alarms have no trigger.
 */
class NextTriggerCalculatorTest {

    /** 2026-11-21 is a Saturday. */
    private val saturdayNoon: LocalDateTime = LocalDateTime.of(2026, 11, 21, 12, 0)

    @Test
    fun `disabled alarm has no trigger`() {
        val alarm = Alarm(hour = 7, minute = 30, isEnabled = false)
        assertNull(NextTriggerCalculator.nextTriggerDateTime(alarm, saturdayNoon))
        assertNull(NextTriggerCalculator.minutesUntilTrigger(alarm, saturdayNoon))
    }

    @Test
    fun `daily alarm later today fires today`() {
        val alarm = Alarm(hour = 23, minute = 15, daysOfWeek = emptySet())
        val trigger = NextTriggerCalculator.nextTriggerDateTime(alarm, saturdayNoon)
        assertEquals(LocalDateTime.of(2026, 11, 21, 23, 15), trigger)
    }

    @Test
    fun `daily alarm earlier today fires tomorrow`() {
        val alarm = Alarm(hour = 7, minute = 30, daysOfWeek = emptySet())
        val trigger = NextTriggerCalculator.nextTriggerDateTime(alarm, saturdayNoon)
        assertEquals(LocalDateTime.of(2026, 11, 22, 7, 30), trigger)
    }

    @Test
    fun `weekly alarm fires on next matching day`() {
        val alarm = Alarm(hour = 7, minute = 0, daysOfWeek = setOf(DayOfWeek.MONDAY))
        val trigger = NextTriggerCalculator.nextTriggerDateTime(alarm, saturdayNoon)
        // Next Monday after Saturday 2026-11-21 is 2026-11-23.
        assertEquals(LocalDateTime.of(2026, 11, 23, 7, 0), trigger)
    }

    @Test
    fun `weekly alarm later today fires today`() {
        // 2026-11-16 is a Monday.
        val mondayMorning: LocalDateTime = LocalDateTime.of(2026, 11, 16, 6, 0)
        val alarm = Alarm(hour = 8, minute = 0, daysOfWeek = setOf(DayOfWeek.MONDAY))
        val trigger = NextTriggerCalculator.nextTriggerDateTime(alarm, mondayMorning)
        assertEquals(LocalDateTime.of(2026, 11, 16, 8, 0), trigger)
    }

    @Test
    fun `all weekdays selected fires tomorrow when time has passed`() {
        val alarm = Alarm(hour = 9, minute = 0, daysOfWeek = DayOfWeek.values().toSet())
        val trigger = NextTriggerCalculator.nextTriggerDateTime(alarm, saturdayNoon)
        assertEquals(LocalDateTime.of(2026, 11, 22, 9, 0), trigger)
    }

    @Test
    fun `alarm exactly now fires tomorrow at same time`() {
        val alarm = Alarm(hour = 12, minute = 0, daysOfWeek = emptySet())
        val trigger = NextTriggerCalculator.nextTriggerDateTime(alarm, saturdayNoon)
        assertEquals(LocalDateTime.of(2026, 11, 22, 12, 0), trigger)
    }

    @Test
    fun `minutes until trigger counts full duration`() {
        val alarm = Alarm(hour = 7, minute = 30, daysOfWeek = emptySet())
        val minutes = NextTriggerCalculator.minutesUntilTrigger(alarm, saturdayNoon)
        // Saturday 12:00 -> Sunday 07:30 = 19 h 30 min = 1170 min.
        assertEquals(1170L, minutes)
    }

    @Test
    fun `two alarms on the same days at the same time resolve identically`() {
        // Spec edge case section 5: identical trigger times resolve to the
        // same moment; ordering of simultaneous alarms is a ringing-stage task.
        val first = Alarm(hour = 6, minute = 30, daysOfWeek = setOf(DayOfWeek.SUNDAY))
        val second = Alarm(hour = 6, minute = 30, daysOfWeek = setOf(DayOfWeek.SUNDAY))
        assertEquals(
            NextTriggerCalculator.nextTriggerDateTime(first, saturdayNoon),
            NextTriggerCalculator.nextTriggerDateTime(second, saturdayNoon)
        )
    }

    @Test
    fun `spec defaults match alarm defaults`() {
        val alarm = Alarm()
        assertEquals(setOf(TaskType.MATH, TaskType.STROOP), alarm.taskTypes)
        assertEquals(Difficulty.MEDIUM, alarm.difficulty)
        assertEquals(SnoozePolicy.FORBIDDEN, alarm.snoozePolicy)
        assertEquals(true, alarm.isVolumeRampEnabled)
        assertEquals(true, alarm.isRecheckEnabled)
        assertEquals(10, alarm.recheckDelayMin)
        assertEquals(2, alarm.taskCount)
    }
}
