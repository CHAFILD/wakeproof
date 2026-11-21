package com.wakeproof.alarm.domain.audio

/**
 * Pure model of the alarm volume ramp, spec 4.1.5:
 * linear growth from 20% to 100%, step +5% every 3 s (vibration follows
 * the same steps). When the ringing UI is dismissed by the user leaving
 * the app (spec 4.1.3) the growth is only *accelerated*, never jumping
 * above the 100% ceiling.
 *
 * Pure Kotlin so the curve is unit-testable without Android.
 */
object VolumeRamp {

    /** Spec 4.1.5: ramp starts at 20%. */
    const val START_VOLUME = 0.2f

    /** Spec 4.1.5: ramp ceiling, never exceeded. */
    const val MAX_VOLUME = 1.0f

    /** Normal curve: +5% per step. */
    const val NORMAL_STEP = 0.05f

    /** Normal curve: one step every 3 seconds. */
    const val NORMAL_INTERVAL_MS = 3_000L

    /**
     * Accelerated curve (user left the ringing screen, spec 4.1.3):
     * doubles the step and fires it six times more often, so full
     * volume is reached in a couple of seconds at most.
     */
    const val ACCELERATED_STEP = 0.10f

    const val ACCELERATED_INTERVAL_MS = 500L

    /** Interval between steps for the given mode. */
    fun intervalMs(accelerated: Boolean): Long =
        if (accelerated) ACCELERATED_INTERVAL_MS else NORMAL_INTERVAL_MS

    /**
     * Next volume value after one step from [current]. Clamped to
     * [MAX_VOLUME]; the result never decreases and never exceeds 100%.
     */
    fun nextVolume(current: Float, accelerated: Boolean): Float {
        val step = if (accelerated) ACCELERATED_STEP else NORMAL_STEP
        val next = current + step
        return next.coerceIn(current, MAX_VOLUME)
    }

    /**
     * Number of normal-mode steps needed to reach 100% from 20%
     * (16 steps × 3 s = 48 s — the "linear within a minute" curve
     * of spec 4.1.5 with the prescribed fixed step).
     */
    fun normalStepCountToMax(): Int {
        var volume = START_VOLUME
        var steps = 0
        while (volume < MAX_VOLUME) {
            volume = nextVolume(volume, accelerated = false)
            steps++
        }
        return steps
    }
}
