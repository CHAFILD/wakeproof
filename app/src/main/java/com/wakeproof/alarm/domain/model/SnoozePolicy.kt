package com.wakeproof.alarm.domain.model

/**
 * Snooze policy per alarm (spec 4.1.2). Interval is a constant 10 minutes
 * and the "once" limit applies to a single ringing session.
 * In HARDCORE mode snooze is always unavailable regardless of this setting.
 */
enum class SnoozePolicy {
    FORBIDDEN,
    ONCE,
    ONCE_HARDER
}
