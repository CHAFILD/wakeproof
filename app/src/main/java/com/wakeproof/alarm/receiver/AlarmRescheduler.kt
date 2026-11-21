package com.wakeproof.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log
import com.wakeproof.alarm.WakeProofApplication
import kotlinx.coroutines.launch

/**
 * Shared body for [BootReceiver] and [TimeChangedReceiver]: reschedules
 * every enabled alarm after a system event (spec section 5: reboot and
 * timezone/time changes must not lose schedules).
 *
 * Uses [BroadcastReceiver.goAsync] because the Room read is suspending;
 * the broadcast timeout (10 s) is far more than a query plus a handful
 * of setAlarmClock calls needs.
 */
internal object AlarmRescheduler {

    fun rescheduleAll(context: Context, reason: String, pendingResult: BroadcastReceiver.PendingResult) {
        val container = (context.applicationContext as WakeProofApplication).appContainer
        container.applicationScope.launch {
            try {
                container.alarmScheduler.rescheduleAllEnabled()
                Log.i(TAG, "Rescheduled all enabled alarms after $reason")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to reschedule alarms after $reason", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private const val TAG = "AlarmRescheduler"
}
