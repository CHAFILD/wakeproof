package com.wakeproof.alarm.domain.session

import com.wakeproof.alarm.domain.model.TaskSession
import com.wakeproof.alarm.domain.model.TaskSessionResult
import com.wakeproof.alarm.domain.repository.TaskSessionRepository
import com.wakeproof.alarm.domain.task.TaskResult

/**
 * Records one ringing session into Room, spec 4.1.4 / 6: started when
 * the ringing UI appears, persisted ONCE when the session ends either
 * clean (all tasks solved) or through the emergency exit. One-shot by
 * design: double finishes (e.g. a re-delivered event) never write two
 * rows. The emergency phrase itself is never part of the record.
 */
class TaskSessionRecorder(
    private val alarmId: Long,
    private val repository: TaskSessionRepository,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val startedAt: Long = clock()
    private var finishedAt: Long? = null

    /**
     * Persists the session. Returns the stored value or null when the
     * session was already finished before.
     *
     * @param taskResults fully solved task blocks (partial progress of
     *   the current block is not included).
     * @param totalMistakes wrong answers of the whole session,
     *   including the unfinished block.
     */
    suspend fun finish(
        result: TaskSessionResult,
        taskResults: List<TaskResult>,
        totalMistakes: Int
    ): TaskSession? {
        if (finishedAt != null) return null
        val now = clock()
        finishedAt = now
        val session = TaskSession(
            alarmId = alarmId,
            startedAt = startedAt,
            finishedAt = now,
            result = result,
            mistakes = totalMistakes,
            durationMs = (now - startedAt).coerceAtLeast(0L),
            taskResults = taskResults
        )
        repository.saveSession(session)
        return session
    }
}
