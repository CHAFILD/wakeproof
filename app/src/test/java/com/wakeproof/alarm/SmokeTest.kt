package com.wakeproof.alarm

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Smoke test proving that the JUnit 5 platform is wired correctly
 * (useJUnitPlatform in app/build.gradle.kts). Domain tests arrive
 * with later stages.
 */
class SmokeTest {

    @Test
    fun `junit5 platform runs`() {
        assertTrue(true)
    }
}
