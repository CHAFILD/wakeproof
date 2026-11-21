package com.wakeproof.alarm.domain.session

import com.wakeproof.alarm.domain.model.TaskSession
import com.wakeproof.alarm.domain.model.TaskSessionResult
import com.wakeproof.alarm.domain.repository.TaskSessionRepository
import com.wakeproof.alarm.domain.task.TaskResult

/**
 * Records one ringing session into Room, spec 4.1.4 / 6: started when
 * the ringing UI appears, persisted ONCE when the session ends either
 * clean (all tasks solved), snoozed (spec 4.1.2) or through the
 * emergency exit. One-shot by design: double finishes (e.g. a
 * re-delivered event) never write twice. The emergency phrase itself
 * is never part of the record.
 *
 * A snoozed ring continues the SAME morning session row
 * ([continuationOf] > 0): its results are merged into that row and the
 * stored result stays SNOOZED - one morning is one row (spec 4.4), so
 * a snoozed morning never counts as clean. When the continued row has
 * vanished the recorder falls back to writing a normal row.
 */
class TaskSessionRecorder(
    private val alarmId: Long,
    private val repository: TaskSessionRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    /** Existing morning-session row this ring continues; 0 = new session. */
    private val continuationOf: Long = 0L
) {
    private val startedAt: Long = clock()
    private var finishedAt: Long? = null

    /**
     * Persists the session. Returns the stored value (with its row id)
     * or null when the session was already finished before.
     *
     * @param taskResults fully solved task blocks (partial progress of
     *   the current block is not included).
     * @param totalMistakes wrong answers of the whole session,
     *   including the unfinished block. In continuation mode this is
     *   the amount to APPEND.
     */
    suspend fun finish(
        result: TaskSessionResult,
        taskResults: List<TaskResult>,
        totalMistakes: Int
    ): TaskSession? {
        if (finishedAt != null) return null
        val now = clock()
        finishedAt = now
        return if (continuationOf > 0L) {
            repository.continueSession(
                sessionId = continuationOf,
                extraTaskResults = taskResults,
                extraMistakes = totalMistakes,
                finishedAtMillis = now
            ) ?: saveNewRow(result, taskResults, totalMistakes, now)
        } else {
            saveNewRow(result, taskResults, totalMistakes, now)
        }
    }

    private suspend fun saveNewRow(
        result: TaskSessionResult,
        taskResults: List<TaskResult>,
        totalMistakes: Int,
        now: Long
    ): TaskSession {
        val session = TaskSession(
            alarmId = alarmId,
            startedAt = startedAt,
            finishedAt = now,
            result = result,
            mistakes = totalMistakes,
            durationMs = (now - startedAt).coerceAtLeast(0L),
            taskResults = taskResults
        )
        val id = repository.saveSession(session)
        return session.copy(id = id)
    }
}
