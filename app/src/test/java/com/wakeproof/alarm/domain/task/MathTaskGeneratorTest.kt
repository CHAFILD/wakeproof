package com.wakeproof.alarm.domain.task

import com.wakeproof.alarm.domain.model.Difficulty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Spec 4.2.1 math generator: solvable integer examples with non-negative
 * answers, operand ranges per difficulty, only +/−/× operations,
 * deterministic under a fixed seed.
 */
class MathTaskGeneratorTest {

    private val generator = MathTaskGenerator(KotlinRandomSource.seeded(SEED))

    @Test
    fun `easy operands stay within 20 and multiplication within 10x10`() {
        repeat(DRAWS) {
            val task = generator.generate(Difficulty.EASY)
            val (a, b) = operands(task.expression)
            assertTrue(a in 1..20 && b in 1..20, task.expression)
            if (operator(task.expression) == "×") {
                assertTrue(a <= 10 && b <= 10, task.expression)
            }
        }
    }

    @Test
    fun `medium operands stay within 99 and multiplication within 15x15`() {
        repeat(DRAWS) {
            val task = generator.generate(Difficulty.MEDIUM)
            val (a, b) = operands(task.expression)
            assertTrue(a in 1..99 && b in 1..99, task.expression)
            if (operator(task.expression) == "×") {
                assertTrue(a <= 15 && b <= 15, task.expression)
            }
        }
    }

    @Test
    fun `hardcore operands stay within 999 and multiplication within 30x30`() {
        repeat(DRAWS) {
            val task = generator.generate(Difficulty.HARDCORE)
            val (a, b) = operands(task.expression)
            assertTrue(a in 1..999 && b in 1..999, task.expression)
            if (operator(task.expression) == "×") {
                assertTrue(a <= 30 && b <= 30, task.expression)
            }
        }
    }

    @Test
    fun `all examples are solvable and match the stored integer answer`() {
        for (difficulty in Difficulty.entries) {
            repeat(DRAWS) {
                val task = generator.generate(difficulty)
                // toInt() parsing inside evaluate() fails on any
                // non-integer component, so equality proves both
                // solvability and the integer answer contract.
                assertEquals(evaluate(task.expression), task.answer, task.expression)
            }
        }
    }

    @Test
    fun `answers are never negative`() {
        for (difficulty in Difficulty.entries) {
            repeat(DRAWS) {
                val task = generator.generate(difficulty)
                assertTrue(task.answer >= 0, task.expression)
            }
        }
    }

    @Test
    fun `only addition subtraction and multiplication are used`() {
        val allowed = setOf("+", "−", "×")
        val seen = mutableSetOf<String>()
        for (difficulty in Difficulty.entries) {
            repeat(DRAWS) {
                val op = operator(generator.generate(difficulty).expression)
                assertTrue(op in allowed, op)
                seen += op
            }
        }
        // All three operations occur across a deterministic draw stream.
        assertEquals(allowed, seen)
    }

    @Test
    fun `generation is deterministic with a fixed seed`() {
        val first = MathTaskGenerator(KotlinRandomSource.seeded(SEED))
        val second = MathTaskGenerator(KotlinRandomSource.seeded(SEED))
        val streamA = List(DRAWS) { first.generate(Difficulty.MEDIUM) }
        val streamB = List(DRAWS) { second.generate(Difficulty.MEDIUM) }
        assertEquals(streamA, streamB)
    }

    /** Evaluates "a OP b" expressions, proving solvability of every sample. */
    private fun evaluate(expression: String): Int {
        val parts = expression.split(" ")
        val a = parts[0].toInt()
        val b = parts[2].toInt()
        return when (parts[1]) {
            "+" -> a + b
            "−" -> a - b
            "×" -> a * b
            else -> throw IllegalArgumentException(expression)
        }
    }

    private fun operands(expression: String): Pair<Int, Int> {
        val parts = expression.split(" ")
        return parts[0].toInt() to parts[2].toInt()
    }

    private fun operator(expression: String): String = expression.split(" ")[1]

    private companion object {
        const val SEED = 20261121L
        const val DRAWS = 500
    }
}
