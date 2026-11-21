package com.wakeproof.alarm.domain.audio

import com.wakeproof.alarm.domain.audio.VolumeRamp.MAX_VOLUME
import com.wakeproof.alarm.domain.audio.VolumeRamp.START_VOLUME
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests of the volume ramp curve, spec 4.1.5: linear growth from 20%
 * to 100% with a fixed +5% step every 3 s; acceleration (spec 4.1.3)
 * only speeds up growth and can never exceed the 100% ceiling.
 */
class VolumeRampTest {

    @Test
    fun `normal curve reaches max from start in 16 steps`() {
        var volume = START_VOLUME
        var steps = 0
        while (volume < MAX_VOLUME) {
            volume = VolumeRamp.nextVolume(volume, accelerated = false)
            steps++
        }
        assertEquals(16, steps)
        assertEquals(MAX_VOLUME, volume)
    }

    @Test
    fun `normal curve matches 5 percent steps`() {
        assertEquals(0.25f, VolumeRamp.nextVolume(0.20f, accelerated = false))
        assertEquals(0.55f, VolumeRamp.nextVolume(0.50f, accelerated = false))
        assertEquals(1.00f, VolumeRamp.nextVolume(0.95f, accelerated = false))
    }

    @Test
    fun `volume never exceeds max`() {
        assertEquals(MAX_VOLUME, VolumeRamp.nextVolume(0.99f, accelerated = true))
        assertEquals(MAX_VOLUME, VolumeRamp.nextVolume(MAX_VOLUME, accelerated = false))
    }

    @Test
    fun `accelerated curve grows faster and reaches max in 8 steps`() {
        var volume = START_VOLUME
        var steps = 0
        while (volume < MAX_VOLUME) {
            volume = VolumeRamp.nextVolume(volume, accelerated = true)
            steps++
        }
        assertEquals(8, steps)
        assertEquals(MAX_VOLUME, volume)
    }

    @Test
    fun `acceleration continues from current volume`() {
        // Mid-ring at 45%, acceleration kicks in: next step is +10%,
        // i.e. the growth speeds up but never jumps straight to max.
        assertEquals(0.55f, VolumeRamp.nextVolume(0.45f, accelerated = true))
    }

    @Test
    fun `step count helper matches the loop`() {
        assertEquals(16, VolumeRamp.normalStepCountToMax())
    }

    @Test
    fun `intervals follow the spec`() {
        // Normal: 5% every 3 seconds (spec 4.1.5).
        assertEquals(3_000L, VolumeRamp.intervalMs(accelerated = false))
        // Accelerated: faster cadence than normal.
        assertTrue(VolumeRamp.intervalMs(accelerated = true) < VolumeRamp.intervalMs(accelerated = false))
    }

    @Test
    fun `curve stays within spec bounds`() {
        var volume = START_VOLUME
        repeat(100) {
            volume = VolumeRamp.nextVolume(volume, accelerated = it % 2 == 0)
        }
        assertEquals(MAX_VOLUME, volume)
        assertTrue(volume in START_VOLUME..MAX_VOLUME)
    }
}
