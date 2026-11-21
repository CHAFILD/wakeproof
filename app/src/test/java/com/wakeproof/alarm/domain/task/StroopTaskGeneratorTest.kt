package com.wakeproof.alarm.domain.task

import com.wakeproof.alarm.domain.model.Difficulty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Spec 4.2.1 Stroop generator: the word is always printed in a DIFFERENT
 * ink color, both colors come from the four-color palette, and the round
 * counts per task block follow the difficulty table.
 */
class StroopTaskGeneratorTest {

    private val generator = StroopTaskGenerator(KotlinRandomSource.seeded(SEED))

    @Test
    fun `ink color always differs from word color`() {
        for (difficulty in Difficulty.entries) {
            repeat(DRAWS) {
                val round = generator.generate(difficulty)
                assertNotEquals(round.wordColor, round.inkColor)
            }
        }
    }

    @Test
    fun `word and ink colors come from the four color palette`() {
        for (difficulty in Difficulty.entries) {
            repeat(DRAWS) {
                val round = generator.generate(difficulty)
                assertTrue(TaskColor.entries.contains(round.wordColor))
                assertTrue(TaskColor.entries.contains(round.inkColor))
            }
        }
    }

    @Test
    fun `rounds per task follow difficulty 3 5 7`() {
        val expected = mapOf(
            Difficulty.EASY to 3,
            Difficulty.MEDIUM to 5,
            Difficulty.HARDCORE to 7
        )
        for ((difficulty, rounds) in expected) {
            assertEquals(rounds, TaskDifficultyParams.forDifficulty(difficulty).stroopRounds)
        }
    }

    @Test
    fun `generation is deterministic with a fixed seed`() {
        val first = StroopTaskGenerator(KotlinRandomSource.seeded(SEED))
        val second = StroopTaskGenerator(KotlinRandomSource.seeded(SEED))
        val streamA = List(DRAWS) { first.generate(Difficulty.MEDIUM) }
        val streamB = List(DRAWS) { second.generate(Difficulty.MEDIUM) }
        assertEquals(streamA, streamB)
    }

    private companion object {
        const val SEED = 7L
        const val DRAWS = 400
    }
}
