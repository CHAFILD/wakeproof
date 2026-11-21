package com.wakeproof.alarm.data.mapper

import com.wakeproof.alarm.data.local.entity.AlarmEntity
import com.wakeproof.alarm.domain.model.Alarm
import com.wakeproof.alarm.domain.model.Difficulty
import com.wakeproof.alarm.domain.model.SnoozePolicy
import com.wakeproof.alarm.domain.model.TaskType
import java.time.DayOfWeek

/**
 * Maps between the Room entity and the domain [Alarm]. Masks are the
 * single encoding of enum sets in the database, so they live here.
 */
object AlarmMapper {

    // --- Days of week: bit (DayOfWeek.value - 1), Monday = bit 0. ---

    fun daysOfWeekToMask(days: Set<DayOfWeek>): Int =
        days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

    fun maskToDaysOfWeek(mask: Int): Set<DayOfWeek> =
        DayOfWeek.values().filter { mask hasBit (it.value - 1) }.toSet()

    // --- Task types: bit per enum ordinal. ---

    fun taskTypesToMask(types: Set<TaskType>): Int =
        types.fold(0) { mask, type -> mask or (1 shl type.ordinal) }

    fun maskToTaskTypes(mask: Int): Set<TaskType> =
        TaskType.entries.filter { mask hasBit it.ordinal }.toSet()

    // --- Enum names with defensive fallbacks (unknown values never crash). ---

    fun decodeDifficulty(name: String): Difficulty =
        runCatching { Difficulty.valueOf(name) }.getOrDefault(Difficulty.MEDIUM)

    /** Nullable variant for the snooze-session difficulty (unset = null). */
    fun decodeDifficultyOrNull(name: String?): Difficulty? =
        name?.let(::decodeDifficulty)

    fun decodeSnoozePolicy(name: String): SnoozePolicy =
        runCatching { SnoozePolicy.valueOf(name) }.getOrDefault(SnoozePolicy.FORBIDDEN)

    // --- Entity <-> domain. ---

    fun toDomain(entity: AlarmEntity): Alarm = Alarm(
        id = entity.id,
        hour = entity.hour,
        minute = entity.minute,
        daysOfWeek = maskToDaysOfWeek(entity.daysOfWeekMask),
        isEnabled = entity.isEnabled,
        taskTypes = maskToTaskTypes(entity.taskTypesMask),
        difficulty = decodeDifficulty(entity.difficulty),
        snoozePolicy = decodeSnoozePolicy(entity.snoozePolicy),
        isVolumeRampEnabled = entity.isVolumeRampEnabled,
        isRecheckEnabled = entity.isRecheckEnabled,
        recheckDelayMin = entity.recheckDelayMin,
        taskCount = entity.taskCount,
        anchorEnabled = entity.anchorEnabled,
        anchorId = entity.anchorId,
        isSleepReminderEnabled = entity.isSleepReminderEnabled,
        sleepTargetHours = entity.sleepTargetHours,
        snoozeTargetAt = entity.snoozeTargetAt,
        isSnoozeUsed = entity.isSnoozeUsed,
        snoozeSessionTaskCount = entity.snoozeSessionTaskCount,
        snoozeSessionDifficulty = decodeDifficultyOrNull(entity.snoozeSessionDifficulty),
        snoozeSessionId = entity.snoozeSessionId
    )

    fun toEntity(alarm: Alarm): AlarmEntity = AlarmEntity(
        id = alarm.id,
        hour = alarm.hour,
        minute = alarm.minute,
        daysOfWeekMask = daysOfWeekToMask(alarm.daysOfWeek),
        isEnabled = alarm.isEnabled,
        taskTypesMask = taskTypesToMask(alarm.taskTypes),
        difficulty = alarm.difficulty.name,
        snoozePolicy = alarm.snoozePolicy.name,
        isVolumeRampEnabled = alarm.isVolumeRampEnabled,
        isRecheckEnabled = alarm.isRecheckEnabled,
        recheckDelayMin = alarm.recheckDelayMin,
        taskCount = alarm.taskCount,
        anchorEnabled = alarm.anchorEnabled,
        anchorId = alarm.anchorId,
        isSleepReminderEnabled = alarm.isSleepReminderEnabled,
        sleepTargetHours = alarm.sleepTargetHours,
        snoozeTargetAt = alarm.snoozeTargetAt,
        isSnoozeUsed = alarm.isSnoozeUsed,
        snoozeSessionTaskCount = alarm.snoozeSessionTaskCount,
        snoozeSessionDifficulty = alarm.snoozeSessionDifficulty?.name,
        snoozeSessionId = alarm.snoozeSessionId
    )

    private infix fun Int.hasBit(bitIndex: Int): Boolean = (this shr bitIndex) and 1 == 1
}
