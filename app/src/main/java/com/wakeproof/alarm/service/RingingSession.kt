package com.wakeproof.alarm.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-level handshake between [RingingService] and the ringing UI
 * (spec 4.1.3): the activity marks itself visible in onStart/onStop and
 * the service, when visibility is lost for longer than 2 seconds,
 * re-fires the full-screen intent and accelerates the volume ramp.
 *
 * A plain holder is enough: service and activity share the process.
 */
object RingingSession {

    private val _isRingingUiVisible = MutableStateFlow(false)

    /** True while the ringing screen is in the foreground. */
    val isRingingUiVisible: StateFlow<Boolean> = _isRingingUiVisible

    /** Called by RingingActivity.onStart. */
    fun onUiShown() {
        _isRingingUiVisible.value = true
    }

    /** Called by RingingActivity.onStop. */
    fun onUiHidden() {
        _isRingingUiVisible.value = false
    }
}
