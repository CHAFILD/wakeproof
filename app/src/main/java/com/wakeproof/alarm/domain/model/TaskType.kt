package com.wakeproof.alarm.domain.model

/**
 * Cognitive task types the user must solve to dismiss the alarm
 * (spec 4.2.1). Stored in Room as a bitmask of ordinals.
 */
enum class TaskType {
    MATH,
    MEMORY,
    STROOP
}
