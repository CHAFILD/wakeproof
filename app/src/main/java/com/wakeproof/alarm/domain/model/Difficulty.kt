package com.wakeproof.alarm.domain.model

/**
 * Task difficulty mode (spec 4.2.1). Adaptive mode arrives with v1.1
 * (spec 4.5) and is intentionally absent in v1.0.
 */
enum class Difficulty {
    EASY,
    MEDIUM,
    HARDCORE
}
