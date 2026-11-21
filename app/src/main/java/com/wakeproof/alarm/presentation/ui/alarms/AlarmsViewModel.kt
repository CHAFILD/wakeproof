package com.wakeproof.alarm.presentation.ui.alarms

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI state of the main (alarms) screen. Stage 1 replaces the counter
 * with the real alarm list loaded from Room.
 */
data class AlarmsUiState(
    val alarmCount: Int = 0
)

/**
 * ViewModel for the alarms list. Kept wired from Stage 0 so the screen
 * structure (MVVM + StateFlow) does not change later.
 */
class AlarmsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmsUiState())
    val uiState: StateFlow<AlarmsUiState> = _uiState.asStateFlow()
}
