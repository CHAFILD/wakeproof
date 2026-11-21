package com.wakeproof.alarm.domain.task

import kotlin.random.Random

/**
 * Injectable source of randomness so generators are fully deterministic
 * under test (fixed seed) while production uses [Random.Default].
 */
interface RandomSource {
    /** Uniform integer in [from, until). */
    fun nextInt(from: Int, until: Int): Int
}

/**
 * Production implementation backed by kotlin.random; pure Kotlin,
 * no Android dependencies (spec: domain layer).
 */
class KotlinRandomSource(
    private val random: Random = Random.Default
) : RandomSource {

    override fun nextInt(from: Int, until: Int): Int = random.nextInt(from, until)

    companion object {
        /** Convenience factory for seeded (test/dev) sources. */
        fun seeded(seed: Long): KotlinRandomSource = KotlinRandomSource(Random(seed))
    }
}
