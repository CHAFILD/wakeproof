package com.wakeproof.alarm.presentation.navigation

/**
 * Route names for the single NavHost graph. Bottom navigation is
 * intentionally not used (stage plan): the main screen top bar hosts
 * the navigation actions.
 */
object Destinations {

    /** Alarm id meaning "create a new alarm" in the editor route. */
    const val NEW_ALARM_ID = -1L
    const val ARG_ALARM_ID = "alarmId"

    const val ALARMS = "alarms"
    const val STATS = "stats"
    const val SLEEP = "sleep"
    const val ALARM_EDITOR = "alarm_editor?$ARG_ALARM_ID={$ARG_ALARM_ID}"

    /** Route to the editor for an existing alarm or a new one. */
    fun alarmEditor(alarmId: Long): String = "alarm_editor?$ARG_ALARM_ID=$alarmId"
}
