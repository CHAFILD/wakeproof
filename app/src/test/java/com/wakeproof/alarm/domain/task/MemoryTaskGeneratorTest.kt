package com.wakeproof.alarm.domain.task

import com.wakeproof.alarm.domain.model.Difficulty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Spec 4.2.1 memory generator: sequence lengths 4/5/6 per difficulty,
 * palette limited to the four task colors, deterministic under a fixed seed.
 */
class MemoryTaskGeneratorTest {

    private val generator = MemoryTaskGenerator(KotlinRandomSource.seeded(SEED))

    @Test
    fun `sequence lengths follow difficulty 4 5 6`() {
        val expected = mapOf(
            Difficulty.EASY to 4,
            Difficulty.MEDIUM to 5,
            Difficulty.HARDCORE to 6
        )
        for ((difficulty, length) in expected) {
            repeat(DRAWS) {
                val sequence = generator.generate(difficulty)
                assertEquals(length, sequence.colors.size)
            }
        }
    }

    @Test
    fun `sequences use only the four task colors`() {
        for (difficulty in Difficulty.entries) {
            repeat(DRAWS) {
                generator.generate(difficulty).colors.forEach { color ->
                    assertTrue(TaskColor.entries.contains(color))
                }
            }
        }
    }

    @Test
    fun `all four colors occur across a deterministic draw stream`() {
        val seen = mutableSetOf<TaskColor>()
        repeat(DRAWS) {
            seen += generator.generate(Difficulty.MEDIUM).colors
        }
        assertEquals(TaskColor.entries.toSet(), seen)
    }

    @Test
    fun `generation is deterministic with a fixed seed`() {
        val first = MemoryTaskGenerator(KotlinRandomSource.seeded(SEED))
        val second = MemoryTaskGenerator(KotlinRandomSource.seeded(SEED))
        val streamA = List(DRAWS) { first.generate(Difficulty.MEDIUM) }
        val streamB = List(DRAWS) { second.generate(Difficulty.MEDIUM) }
        assertEquals(streamA, streamB)
    }

    private companion object {
        const val SEED = 42L
        const val DRAWS = 300
    }
}
