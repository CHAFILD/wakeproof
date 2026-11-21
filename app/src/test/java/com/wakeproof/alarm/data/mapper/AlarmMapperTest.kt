package com.wakeproof.alarm.data.mapper

import com.wakeproof.alarm.data.local.entity.AlarmEntity
import com.wakeproof.alarm.data.mapper.AlarmMapper.decodeDifficulty
import com.wakeproof.alarm.data.mapper.AlarmMapper.decodeSnoozePolicy
import com.wakeproof.alarm.data.mapper.AlarmMapper.daysOfWeekToMask
import com.wakeproof.alarm.data.mapper.AlarmMapper.maskToDaysOfWeek
import com.wakeproof.alarm.data.mapper.AlarmMapper.maskToTaskTypes
import com.wakeproof.alarm.data.mapper.AlarmMapper.taskTypesToMask
import com.wakeproof.alarm.data.mapper.AlarmMapper.toDomain
import com.wakeproof.alarm.data.mapper.AlarmMapper.toEntity
import com.wakeproof.alarm.domain.model.Alarm
import com.wakeproof.alarm.domain.model.Difficulty
import com.wakeproof.alarm.domain.model.SnoozePolicy
import com.wakeproof.alarm.domain.model.TaskType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

/**
 * Tests for the Room <-> domain mapping (spec 4.1.1): day-of-week bitmask,
 * task-type bitmask and a lossless full round trip.
 */
class AlarmMapperTest {

    @Test
    fun `days of week encode to bitmask`() {
        val mask = daysOfWeekToMask(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY))
        // Monday = bit 0, Wednesday = bit 2, Sunday = bit 6.
        assertEquals(0b1000101, mask)
    }

    @Test
    fun `empty day set encodes to zero`() {
        assertEquals(0, daysOfWeekToMask(emptySet()))
        assertEquals(emptySet<DayOfWeek>(), maskToDaysOfWeek(0))
    }

    @Test
    fun `days of week decode from bitmask`() {
        val days = maskToDaysOfWeek(0b1000101)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY), days)
    }

    @Test
    fun `task types encode and decode as bitmask`() {
        assertEquals(0b101, taskTypesToMask(setOf(TaskType.MATH, TaskType.STROOP)))
        assertEquals(setOf(TaskType.MATH, TaskType.STROOP), maskToTaskTypes(0b101))
        assertEquals(setOf(TaskType.MEMORY), maskToTaskTypes(0b010))
    }

    @Test
    fun `entity to domain preserves every field`() {
        val entity = AlarmEntity(
            id = 42,
            hour = 6,
            minute = 45,
            daysOfWeekMask = daysOfWeekToMask(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)),
            isEnabled = false,
            taskTypesMask = taskTypesToMask(setOf(TaskType.MEMORY, TaskType.STROOP)),
            difficulty = Difficulty.HARDCORE.name,
            snoozePolicy = SnoozePolicy.ONCE_HARDER.name,
            isVolumeRampEnabled = false,
            isRecheckEnabled = false,
            recheckDelayMin = 15,
            taskCount = 4,
            anchorEnabled = true,
            anchorId = "anchor-1",
            isSleepReminderEnabled = false,
            sleepTargetHours = 6
        )
        assertEquals(
            Alarm(
                id = 42,
                hour = 6,
                minute = 45,
                daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                isEnabled = false,
                taskTypes = setOf(TaskType.MEMORY, TaskType.STROOP),
                difficulty = Difficulty.HARDCORE,
                snoozePolicy = SnoozePolicy.ONCE_HARDER,
                isVolumeRampEnabled = false,
                isRecheckEnabled = false,
                recheckDelayMin = 15,
                taskCount = 4,
                anchorEnabled = true,
                anchorId = "anchor-1",
                isSleepReminderEnabled = false,
                sleepTargetHours = 6
            ),
            toDomain(entity)
        )
    }

    @Test
    fun `domain to entity to domain round trip is lossless`() {
        val alarm = Alarm(
            id = 7,
            hour = 8,
            minute = 5,
            daysOfWeek = setOf(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            isEnabled = true,
            taskTypes = setOf(TaskType.MATH, TaskType.MEMORY),
            difficulty = Difficulty.EASY,
            snoozePolicy = SnoozePolicy.ONCE,
            isVolumeRampEnabled = true,
            isRecheckEnabled = true,
            recheckDelayMin = 5,
            taskCount = 3,
            anchorEnabled = false,
            anchorId = null,
            isSleepReminderEnabled = true,
            sleepTargetHours = 9
        )
        assertEquals(alarm, toDomain(toEntity(alarm)))
    }

    @Test
    fun `unknown enum names fall back safely`() {
        assertEquals(Difficulty.MEDIUM, decodeDifficulty("NOT_A_MODE"))
        assertEquals(SnoozePolicy.FORBIDDEN, decodeSnoozePolicy("GARBAGE"))
    }

    @Test
    fun `all enum values survive their own name round trip`() {
        Difficulty.entries.forEach { assertTrue(decodeDifficulty(it.name) == it) }
        SnoozePolicy.entries.forEach { assertTrue(decodeSnoozePolicy(it.name) == it) }
    }

    @Test
    fun `nullable anchor id survives round trip`() {
        val withNull = Alarm(id = 1, anchorEnabled = false, anchorId = null)
        val withValue = Alarm(id = 2, anchorEnabled = true, anchorId = "nfc-007")
        assertNull(toEntity(withNull).anchorId)
        assertEquals("nfc-007", toDomain(toEntity(withValue)).anchorId)
    }
}
