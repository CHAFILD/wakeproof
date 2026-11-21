package com.wakeproof.alarm.domain.task

import com.wakeproof.alarm.domain.model.Alarm
import com.wakeproof.alarm.domain.model.Difficulty
import com.wakeproof.alarm.domain.model.TaskType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Spec 4.2.2 session planner: 1-4 tasks, types repeat in a circle, the same
 * type never goes twice in a row (for two or more selected types), concrete
 * data matches the planned type, deterministic under a fixed seed.
 */
class SessionTaskPlannerTest {

    private fun planner(seed: Long): SessionTaskPlanner =
        SessionTaskPlanner(KotlinRandomSource.seeded(seed))

    @Test
    fun `queue length is clamped to the 1-4 range`() {
        val types = listOf(TaskType.MATH, TaskType.STROOP)
        assertEquals(1, planner(1).planSession(types, taskCount = 0, Difficulty.MEDIUM).size)
        assertEquals(2, planner(1).planSession(types, taskCount = 2, Difficulty.MEDIUM).size)
        assertEquals(4, planner(1).planSession(types, taskCount = 7, Difficulty.MEDIUM).size)
    }

    @Test
    fun `same type never repeats consecutively with two or more types`() {
        val combinations = listOf(
            listOf(TaskType.MATH, TaskType.MEMORY),
            listOf(TaskType.MATH, TaskType.STROOP),
            listOf(TaskType.MEMORY, TaskType.STROOP),
            listOf(TaskType.MATH, TaskType.MEMORY, TaskType.STROOP)
        )
        for (seed in 1..20L) {
            for (types in combinations) {
                for (count in Alarm.TASK_COUNT_MIN..Alarm.TASK_COUNT_MAX) {
                    val queue = planner(seed).planSession(types, count, Difficulty.MEDIUM)
                    assertEquals(count, queue.size)
                    queue.zipWithNext().forEach { (current, next) ->
                        assertTrue(
                            current.type != next.type,
                            "Consecutive same type $current -> $next (seed=$seed, count=$count)"
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `types repeat in a circle preserving the selected order`() {
        val queue = planner(1).planSession(
            listOf(TaskType.MATH, TaskType.STROOP),
            taskCount = 4,
            difficulty = Difficulty.MEDIUM
        )
        assertEquals(
            listOf(TaskType.MATH, TaskType.STROOP, TaskType.MATH, TaskType.STROOP),
            queue.map { it.type }
        )
        val threeTypes = planner(2).planSession(
            listOf(TaskType.MEMORY, TaskType.STROOP, TaskType.MATH),
            taskCount = 4,
            difficulty = Difficulty.MEDIUM
        )
        assertEquals(
            listOf(TaskType.MEMORY, TaskType.STROOP, TaskType.MATH, TaskType.MEMORY),
            threeTypes.map { it.type }
        )
    }

    @Test
    fun `single selected type fills the whole queue`() {
        val queue = planner(1).planSession(listOf(TaskType.MEMORY), 3, Difficulty.HARDCORE)
        assertEquals(3, queue.size)
        assertTrue(queue.all { it.type == TaskType.MEMORY })
    }

    @Test
    fun `planned data matches the planned task type`() {
        val queue = planner(5).planSession(
            listOf(TaskType.MATH, TaskType.MEMORY, TaskType.STROOP),
            taskCount = 3,
            difficulty = Difficulty.EASY
        )
        queue.forEach { task ->
            when (task.type) {
                TaskType.MATH -> assertTrue(task is TaskData.MathTask)
                TaskType.MEMORY -> assertTrue(task is TaskData.MemorySequence)
                TaskType.STROOP -> assertTrue(task is TaskData.StroopRound)
            }
        }
    }

    @Test
    fun `default alarm config yields two different tasks`() {
        // Spec 4.1.1/4.2.2 default: math + Stroop, 2 tasks.
        val alarm = Alarm()
        val queue = planner(1).planSession(alarm.taskTypes, alarm.taskCount, alarm.difficulty)
        assertEquals(2, queue.size)
        assertTrue(queue[0].type != queue[1].type)
    }

    @Test
    fun `memory tasks follow the difficulty sequence length`() {
        val queue = planner(3).planSession(listOf(TaskType.MEMORY), 4, Difficulty.HARDCORE)
        queue.forEach { task ->
            val sequence = task as TaskData.MemorySequence
            assertEquals(6, sequence.colors.size)
        }
    }

    @Test
    fun `planning is deterministic with a fixed seed`() {
        val types = listOf(TaskType.MATH, TaskType.MEMORY, TaskType.STROOP)
        val queueA = planner(2026L).planSession(types, 4, Difficulty.MEDIUM)
        val queueB = planner(2026L).planSession(types, 4, Difficulty.MEDIUM)
        assertEquals(queueA, queueB)
    }

    @Test
    fun `empty type set is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            planner(1).planSession(emptyList(), 2, Difficulty.MEDIUM)
        }
    }
}
