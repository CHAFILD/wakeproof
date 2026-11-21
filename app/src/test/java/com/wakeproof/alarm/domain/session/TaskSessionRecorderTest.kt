package com.wakeproof.alarm.domain.session

import com.wakeproof.alarm.domain.model.TaskSession
import com.wakeproof.alarm.domain.model.TaskSessionResult
import com.wakeproof.alarm.domain.model.TaskType
import com.wakeproof.alarm.domain.repository.TaskSessionRepository
import com.wakeproof.alarm.domain.task.TaskResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * TaskSessionRecorder (spec 4.1.4 / 6): one session row per ringing,
 * CLEAN and EMERGENCY results, one-shot finish, duration from the
 * injected clock, mistakes of unfinished blocks included. Stage 5 adds
 * the snooze continuation: the snoozed ring merges into the SAME
 * morning row and the stored result stays SNOOZED (spec 4.4).
 */
class TaskSessionRecorderTest {

    private class FakeRepository : TaskSessionRepository {
        val saved = mutableListOf<TaskSession>()
        val updated = mutableListOf<TaskSession>()
        private val flow = MutableStateFlow<List<TaskSession>>(emptyList())

        override suspend fun saveSession(session: TaskSession): Long {
            val id = (saved.maxOfOrNull { it.id } ?: 0L) + 1L
            saved += session.copy(id = id) // mimic Room: the row keeps its generated id
            return id
        }

        override fun observeSessions(): Flow<List<TaskSession>> = flow

        override suspend fun getAllChronological(): List<TaskSession> = saved

        override suspend fun continueSession(
            sessionId: Long,
            extraTaskResults: List<TaskResult>,
            extraMistakes: Int,
            finishedAtMillis: Long
        ): TaskSession? {
            val index = saved.indexOfFirst { it.id == sessionId }
            if (index < 0) return null
            val existing = saved[index]
            val merged = existing.copy(
                finishedAt = finishedAtMillis,
                durationMs = (finishedAtMillis - existing.startedAt).coerceAtLeast(0L),
                mistakes = existing.mistakes + extraMistakes,
                taskResults = existing.taskResults + extraTaskResults
            )
            saved[index] = merged
            updated += merged
            return merged
        }
    }

    private class MutableClock(var now: Long = 100_000L) {
        val read: () -> Long = { now }
    }

    private val results = listOf(
        TaskResult(timeTakenMs = 4_000L, mistakes = 1, taskType = TaskType.MATH),
        TaskResult(timeTakenMs = 8_000L, mistakes = 0, taskType = TaskType.STROOP)
    )

    @Test
    fun `clean finish writes one row with all fields`() = runTest {
        val repository = FakeRepository()
        val clock = MutableClock()
        val recorder = TaskSessionRecorder(alarmId = 7L, repository = repository, clock = clock.read)
        clock.now += 25_000L
        val session = recorder.finish(TaskSessionResult.CLEAN, results, totalMistakes = 1)

        assertNotNull(session)
        val stored = repository.saved.single()
        assertEquals(7L, stored.alarmId)
        assertEquals(100_000L, stored.startedAt)
        assertEquals(125_000L, stored.finishedAt)
        assertEquals(25_000L, stored.durationMs)
        assertEquals(TaskSessionResult.CLEAN, stored.result)
        assertEquals(1, stored.mistakes)
        assertEquals(results, stored.taskResults)
        assertEquals(2, stored.solvedTaskCount)
    }

    @Test
    fun `finish returns the persisted row id`() = runTest {
        val repository = FakeRepository()
        val recorder = TaskSessionRecorder(alarmId = 1L, repository = repository)
        val session = recorder.finish(TaskSessionResult.CLEAN, emptyList(), 0)
        assertNotNull(session)
        assertEquals(repository.saved.single().id, session?.id)
    }

    @Test
    fun `emergency finish records solved tasks and current block mistakes`() = runTest {
        val repository = FakeRepository()
        val clock = MutableClock()
        val recorder = TaskSessionRecorder(alarmId = 3L, repository = repository, clock = clock.read)
        clock.now += 12_000L
        // One solved block + two mistakes inside the unfinished block.
        val session = recorder.finish(
            TaskSessionResult.EMERGENCY,
            taskResults = results.take(1),
            totalMistakes = 2
        )

        assertNotNull(session)
        val stored = repository.saved.single()
        assertEquals(TaskSessionResult.EMERGENCY, stored.result)
        assertEquals(1, stored.solvedTaskCount)
        assertEquals(2, stored.mistakes, "Mistakes of the unfinished block must be kept")
        assertEquals(12_000L, stored.durationMs)
    }

    @Test
    fun `finish is one-shot - second call writes nothing`() = runTest {
        val repository = FakeRepository()
        val clock = MutableClock()
        val recorder = TaskSessionRecorder(alarmId = 1L, repository = repository, clock = clock.read)
        assertNotNull(recorder.finish(TaskSessionResult.CLEAN, emptyList(), 0))
        assertNull(recorder.finish(TaskSessionResult.EMERGENCY, emptyList(), 0))
        assertEquals(1, repository.saved.size)
    }

    @Test
    fun `duration never goes negative with a backwards clock`() = runTest {
        val repository = FakeRepository()
        val clock = MutableClock(10_000L)
        val recorder = TaskSessionRecorder(alarmId = 1L, repository = repository, clock = clock.read)
        clock.now = 4_000L
        val session = recorder.finish(TaskSessionResult.CLEAN, emptyList(), 0)
        assertNotNull(session)
        assertTrue(repository.saved.single().durationMs >= 0)
    }

    // --- Snooze continuation (spec 4.1.2 / 4.4, stage 5) -------------------

    @Test
    fun `snoozed ring merges into the same morning row keeping SNOOZED`() = runTest {
        val repository = FakeRepository()
        val clock = MutableClock()

        // Morning ring #1: partial progress, then snooze.
        val first = TaskSessionRecorder(alarmId = 5L, repository = repository, clock = clock.read)
        clock.now += 10_000L
        val snoozed = first.finish(TaskSessionResult.SNOOZED, results.take(1), totalMistakes = 1)
        assertNotNull(snoozed)
        assertEquals(TaskSessionResult.SNOOZED, repository.saved.single().result)

        // Morning ring #2 (+10 min): solved - merges into the same row.
        clock.now += 10L * 60_000L
        val second = TaskSessionRecorder(
            alarmId = 5L,
            repository = repository,
            clock = clock.read,
            continuationOf = snoozed?.id ?: 0L
        )
        clock.now += 20_000L
        val merged = second.finish(TaskSessionResult.CLEAN, listOf(results[1]), totalMistakes = 2)
        assertNotNull(merged)

        val stored = repository.saved.single()
        assertEquals(1, repository.saved.size, "One morning = one session row")
        assertEquals(snoozed?.id, stored.id)
        assertEquals(TaskSessionResult.SNOOZED, stored.result, "A snoozed morning stays SNOOZED")
        assertEquals(100_000L, stored.startedAt, "startedAt of the whole morning")
        assertEquals(730_000L, stored.finishedAt, "finish of the last ring")
        assertEquals(630_000L, stored.durationMs)
        assertEquals(results, stored.taskResults, "Task blocks of both rings merged in order")
        assertEquals(3, stored.mistakes, "Mistakes of both rings summed")
        assertEquals(1, repository.updated.size)
    }

    @Test
    fun `continuation falls back to a new row when the morning row is gone`() = runTest {
        val repository = FakeRepository()
        val clock = MutableClock()
        val recorder = TaskSessionRecorder(
            alarmId = 2L,
            repository = repository,
            clock = clock.read,
            continuationOf = 999L // no such row
        )
        clock.now += 5_000L
        val session = recorder.finish(TaskSessionResult.CLEAN, emptyList(), 0)
        assertNotNull(session)
        assertEquals(1, repository.saved.size, "Fallback wrote a normal row")
        assertEquals(TaskSessionResult.CLEAN, repository.saved.single().result)
    }
}
