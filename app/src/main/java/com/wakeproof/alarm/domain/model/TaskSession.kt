package com.wakeproof.alarm.domain.model

import com.wakeproof.alarm.domain.task.TaskResult

/**
 * Outcome of one ringing session, spec 4.4 / 6. Stored as the result
 * column of the task_sessions table. This stage only writes CLEAN and
 * EMERGENCY; the other values arrive with the snooze, recheck and
 * boot-missed stages.
 */
enum class TaskSessionResult {
    /** Alarm dismissed by solving all tasks, no snooze, no emergency exit. */
    CLEAN,

    /** Alarm dismissed via snooze (later stage). */
    SNOOZED,

    /** Alarm dismissed through the emergency exit (spec 4.1.4). */
    EMERGENCY,

    /** Alarm never rang (device was off, later stage). */
    MISSED,

    /** Recheck call failed and was repeated with a full ring (later stage). */
    RECHECK_FAILED
}

/**
 * One persisted ringing session, spec 4.1.4 logging requirements and
 * section 6: time, alarm, how many tasks were solved before the exit,
 * session duration. The emergency phrase itself is never logged.
 */
data class TaskSession(
    /** Row id, 0 until persisted. */
    val id: Long = 0,
    val alarmId: Long,
    /** Wall clock (epoch ms) when the ringing UI started the session. */
    val startedAt: Long,
    /** Wall clock (epoch ms) when the session ended (clean or emergency). */
    val finishedAt: Long,
    val result: TaskSessionResult,
    /** Total wrong answers across the whole session, finished blocks + current. */
    val mistakes: Int,
    /** finishedAt - startedAt. */
    val durationMs: Long,
    /** Results of fully solved task blocks, in order. */
    val taskResults: List<TaskResult>
) {
    /** How many task blocks the user completed before the session ended. */
    val solvedTaskCount: Int get() = taskResults.size
}
