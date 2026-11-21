package com.wakeproof.alarm.domain.session

import com.wakeproof.alarm.domain.model.Difficulty
import com.wakeproof.alarm.domain.model.TaskType
import com.wakeproof.alarm.domain.task.KotlinRandomSource
import com.wakeproof.alarm.domain.task.TaskColor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * TaskSessionEngine (spec 4.2): math streaks with reset + new example,
 * memory replay of the SAME sequence after a wrong tap, Stroop streaks
 * with reset, block/task transitions, recorded TaskResults with
 * per-block mistakes and durations.
 */
class TaskSessionEngineTest {

    private class MutableClock(var now: Long = 1_000L) {
        val read: () -> Long = { now }
    }

    private fun engine(
        types: Collection<TaskType> = listOf(TaskType.MATH, TaskType.STROOP),
        count: Int = 2,
        difficulty: Difficulty = Difficulty.MEDIUM,
        seed: Long = 42L,
        clock: MutableClock = MutableClock()
    ): Pair<TaskSessionEngine, MutableClock> =
        TaskSessionEngine(
            selectedTypes = types,
            taskCount = count,
            difficulty = difficulty,
            random = KotlinRandomSource.seeded(seed),
            clock = clock.read
        ) to clock

    /** Parses "a + b" / "a − b" / "a × b" back into the answer. */
    private fun solve(expression: String): Int {
        val parts = expression.split(" ")
        assertEquals(3, parts.size, "Expression must be 'a op b': $expression")
        val a = parts[0].toInt()
        val b = parts[2].toInt()
        return when (parts[1]) {
            "+" -> a + b
            "−" -> a - b
            "×" -> a * b
            else -> throw IllegalArgumentException("Unknown op ${parts[1]}")
        }
    }

    /** Solves the current math block, optionally with wrong answers first. */
    private fun solveMathBlock(engine: TaskSessionEngine, wrongAnswers: Int = 0) {
        repeat(wrongAnswers) {
            val round = engine.state.round as RoundState.Math
            assertEquals(
                AnswerOutcome.MISTAKE,
                engine.onMathSubmit(solve(round.expression) + 1)
            )
        }
        while (engine.state.round is RoundState.Math && !engine.state.isFinished) {
            val round = engine.state.round as RoundState.Math
            val outcome = engine.onMathSubmit(solve(round.expression))
            assertTrue(
                outcome == AnswerOutcome.CORRECT || outcome == AnswerOutcome.BLOCK_DONE,
                "Unexpected outcome $outcome"
            )
        }
    }

    // --- Math -----------------------------------------------------------------

    @Test
    fun `math block needs required in row answers`() {
        val (engine, _) = engine(types = listOf(TaskType.MATH), count = 1)
        val round = engine.state.round as RoundState.Math
        assertEquals(3, round.requiredInRow, "MEDIUM math = 3 in a row (spec 4.2.1)")
        repeat(round.requiredInRow - 1) { i ->
            val r = engine.state.round as RoundState.Math
            engine.onMathSubmit(solve(r.expression))
            assertEquals(i + 1, (engine.state.round as RoundState.Math).solvedInRow)
        }
        val last = engine.state.round as RoundState.Math
        val outcome = engine.onMathSubmit(solve(last.expression))
        assertEquals(AnswerOutcome.BLOCK_DONE, outcome)
        assertTrue(engine.isFinished, "Single task session must finish after one block")
    }

    @Test
    fun `math mistake resets the streak and shows a new example`() {
        val (engine, _) = engine(types = listOf(TaskType.MATH), count = 1)
        repeat(2) {
            val r = engine.state.round as RoundState.Math
            engine.onMathSubmit(solve(r.expression))
        }
        assertEquals(2, (engine.state.round as RoundState.Math).solvedInRow)
        val before = (engine.state.round as RoundState.Math).expression
        val outcome = engine.onMathSubmit(solve(before) + 3)
        assertEquals(AnswerOutcome.MISTAKE, outcome)
        val after = engine.state.round as RoundState.Math
        assertEquals(0, after.solvedInRow)
        assertTrue(after.expression != before, "A mistake must produce a NEW example")
        assertEquals(1, engine.state.totalMistakes)
        assertFalse(engine.isFinished)
    }

    @Test
    fun `math difficulty changes the required streak`() {
        val (easy, _) = engine(
            types = listOf(TaskType.MATH), count = 1, difficulty = Difficulty.EASY
        )
        assertEquals(2, (easy.state.round as RoundState.Math).requiredInRow)
        val (hard, _) = engine(
            types = listOf(TaskType.MATH), count = 1, difficulty = Difficulty.HARDCORE
        )
        assertEquals(5, (hard.state.round as RoundState.Math).requiredInRow)
    }

    // --- Memory -----------------------------------------------------------------

    @Test
    fun `memory block completes after replaying the whole sequence`() {
        val (engine, clock) = engine(
            types = listOf(TaskType.MEMORY), count = 1, difficulty = Difficulty.MEDIUM
        )
        val sequence = engine.memorySequenceColors()
        assertEquals(5, sequence.size, "MEDIUM memory length (spec 4.2.1)")
        clock.now += 6_000L
        sequence.dropLast(1).forEachIndexed { i, color ->
            assertEquals(AnswerOutcome.CORRECT, engine.onMemoryTap(i, color))
        }
        assertEquals(
            AnswerOutcome.BLOCK_DONE,
            engine.onMemoryTap(sequence.lastIndex, sequence.last())
        )
        assertTrue(engine.isFinished)
        val result = engine.results.single()
        assertEquals(TaskType.MEMORY, result.taskType)
        assertEquals(6_000L, result.timeTakenMs)
        assertEquals(0, result.mistakes)
    }

    @Test
    fun `memory wrong tap replays the same sequence and counts a mistake`() {
        val (engine, _) = engine(types = listOf(TaskType.MEMORY), count = 1)
        val sequence = engine.memorySequenceColors()
        val wrongColor = TaskColor.entries.first { it != sequence[0] }
        assertEquals(AnswerOutcome.MISTAKE, engine.onMemoryTap(0, wrongColor))
        assertEquals(1, engine.state.totalMistakes)
        assertEquals(0, (engine.state.round as RoundState.Memory).solvedInRow)
        // The SAME sequence must be replayed (no dead ends, spec 1.4).
        assertEquals(sequence, engine.memorySequenceColors())
        // And the full correct replay still finishes the block.
        sequence.forEachIndexed { i, color ->
            val outcome = engine.onMemoryTap(i, color)
            if (i == sequence.lastIndex) assertEquals(AnswerOutcome.BLOCK_DONE, outcome)
        }
        assertTrue(engine.isFinished)
        assertEquals(1, engine.results.single().mistakes)
    }

    @Test
    fun `memory sequence length follows difficulty`() {
        val (engine, _) = engine(
            types = listOf(TaskType.MEMORY), count = 1, difficulty = Difficulty.EASY
        )
        assertEquals(4, (engine.state.round as RoundState.Memory).sequenceLength)
    }

    // --- Stroop -----------------------------------------------------------------

    @Test
    fun `stroop block needs ink answers in row and word differs from ink`() {
        val (engine, _) = engine(types = listOf(TaskType.STROOP), count = 1)
        var round = engine.state.round as RoundState.Stroop
        assertEquals(5, round.requiredInRow, "MEDIUM stroop rounds (spec 4.2.1)")
        while (true) {
            round = engine.state.round as? RoundState.Stroop ?: break
            assertTrue(round.wordColor != round.inkColor, "Word and ink must differ (spec 4.2.1)")
            val outcome = engine.onStroopSubmit(round.inkColor)
            if (engine.state.isFinished) {
                assertEquals(AnswerOutcome.BLOCK_DONE, outcome)
                break
            }
            assertEquals(AnswerOutcome.CORRECT, outcome)
        }
        assertTrue(engine.isFinished)
    }

    @Test
    fun `stroop wrong answer resets the streak`() {
        val (engine, _) = engine(types = listOf(TaskType.STROOP), count = 1)
        repeat(2) {
            val r = engine.state.round as RoundState.Stroop
            engine.onStroopSubmit(r.inkColor)
        }
        assertEquals(2, (engine.state.round as RoundState.Stroop).solvedInRow)
        val round = engine.state.round as RoundState.Stroop
        val wrong = TaskColor.entries.first { it != round.inkColor }
        assertEquals(AnswerOutcome.MISTAKE, engine.onStroopSubmit(wrong))
        val after = engine.state.round as RoundState.Stroop
        assertEquals(0, after.solvedInRow)
        assertEquals(1, engine.state.totalMistakes)
        assertFalse(engine.isFinished)
    }

    // --- Session flow ------------------------------------------------------------

    @Test
    fun `default session is math then stroop and finishes with results`() {
        val (engine, clock) = engine()
        assertEquals(2, engine.state.totalTasks)
        assertEquals(0, engine.state.taskIndex, "taskIndex is 0-based")
        assertTrue(engine.state.round is RoundState.Math)
        clock.now += 10_000L
        solveMathBlock(engine)
        assertTrue(engine.state.round is RoundState.Stroop, "Round-robin: second block is STROOP")
        clock.now += 5_000L
        while (!engine.isFinished) {
            val r = engine.state.round as RoundState.Stroop
            engine.onStroopSubmit(r.inkColor)
        }
        assertEquals(
            listOf(TaskType.MATH, TaskType.STROOP),
            engine.results.map { it.taskType }
        )
        assertEquals(listOf(10_000L, 5_000L), engine.results.map { it.timeTakenMs })
    }

    @Test
    fun `mistakes are tracked per block and for the whole session`() {
        val (engine, _) = engine()
        solveMathBlock(engine, wrongAnswers = 2)
        var stroopMistakeInjected = false
        while (!engine.isFinished) {
            val r = engine.state.round as RoundState.Stroop
            if (!stroopMistakeInjected && r.solvedInRow == 1) {
                val wrong = TaskColor.entries.first { it != r.inkColor }
                engine.onStroopSubmit(wrong)
                stroopMistakeInjected = true
            } else {
                engine.onStroopSubmit(r.inkColor)
            }
        }
        assertEquals(2, engine.results[0].mistakes)
        assertEquals(1, engine.results[1].mistakes)
        assertEquals(3, engine.state.totalMistakes)
    }

    @Test
    fun `submitting to the wrong round type throws`() {
        val (engine, _) = engine(types = listOf(TaskType.MATH), count = 1)
        assertThrows(IllegalStateException::class.java) {
            engine.onStroopSubmit(TaskColor.RED)
        }
    }

    @Test
    fun `empty type collection is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            TaskSessionEngine(
                selectedTypes = emptyList(),
                taskCount = 1,
                difficulty = Difficulty.MEDIUM,
                random = KotlinRandomSource.seeded(1L)
            )
        }
    }

    @Test
    fun `three tasks rotate through selected types without repetition`() {
        val (engine, _) = engine(
            types = listOf(TaskType.MATH, TaskType.MEMORY, TaskType.STROOP), count = 3
        )
        val seen = mutableListOf<TaskType>()
        while (!engine.isFinished) {
            when (engine.state.round) {
                is RoundState.Math -> {
                    seen += TaskType.MATH
                    solveMathBlock(engine)
                }
                is RoundState.Memory -> {
                    seen += TaskType.MEMORY
                    engine.memorySequenceColors().forEachIndexed { i, color ->
                        engine.onMemoryTap(i, color)
                    }
                }
                is RoundState.Stroop -> {
                    seen += TaskType.STROOP
                    while (engine.state.round is RoundState.Stroop && !engine.isFinished) {
                        val r = engine.state.round as RoundState.Stroop
                        engine.onStroopSubmit(r.inkColor)
                    }
                }
            }
        }
        assertEquals(listOf(TaskType.MATH, TaskType.MEMORY, TaskType.STROOP), seen)
        assertEquals(3, engine.results.size)
    }

    @Test
    fun `recorded result time never goes negative`() {
        // A hostile clock that jumps backwards must not produce a
        // negative duration.
        val clock = MutableClock(5_000L)
        val engine = TaskSessionEngine(
            selectedTypes = listOf(TaskType.MATH),
            taskCount = 1,
            difficulty = Difficulty.EASY,
            random = KotlinRandomSource.seeded(7L),
            clock = clock.read
        )
        clock.now = 1_000L // before the block start
        solveMathBlock(engine)
        assertTrue(engine.results.single().timeTakenMs >= 0)
    }
}
