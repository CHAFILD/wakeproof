package com.wakeproof.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.wakeproof.alarm.WakeProofApplication
import com.wakeproof.alarm.service.RingingService
import kotlinx.coroutines.launch

/**
 * Fired by AlarmManager when a scheduled alarm triggers (spec 4.1.3).
 * Verifies the alarm still exists and is enabled, immediately arms the
 * next occurrence (so the schedule survives even if this ringing session
 * never completes) and starts the ringing foreground service.
 *
 * Starting a mediaPlayback FGS from here is allowed on Android 12+:
 * the app is in the exact-alarm delivery exemption list (setAlarmClock).
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE_ALARM) return
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0L) return

        val container = (context.applicationContext as WakeProofApplication).appContainer
        val pendingResult = goAsync()
        container.applicationScope.launch {
            try {
                val alarm = container.alarmRepository.getAlarmById(alarmId)
                if (alarm == null || !alarm.isEnabled) {
                    // Stale pending intent (alarm disabled/deleted after scheduling).
                    Log.w(TAG, "Alarm id=$alarmId is gone or disabled, cancelling")
                    container.alarmScheduler.cancel(alarmId)
                    return@launch
                }
                // Arm the next occurrence before ringing starts.
                container.alarmScheduler.schedule(alarm)
                ContextCompat.startForegroundService(
                    context,
                    RingingService.startIntent(context, alarmId)
                )
            } catch (t: Throwable) {
                // A failing receiver would crash the whole process; a missed
                // ring start is bad but must not take the app down.
                Log.e(TAG, "Failed to start ringing for id=$alarmId", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"

        const val ACTION_FIRE_ALARM = "com.wakeproof.alarm.action.FIRE_ALARM"
        const val EXTRA_ALARM_ID = "com.wakeproof.alarm.extra.ALARM_ID"
    }
}
