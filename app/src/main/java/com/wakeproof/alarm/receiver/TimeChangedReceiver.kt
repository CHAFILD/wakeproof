package com.wakeproof.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms every enabled alarm after a timezone change or a manual/NTP
 * clock change (spec section 5): trigger times are stored as local
 * wall-clock values and must be recomputed under the new rules.
 */
class TimeChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED ->
                AlarmRescheduler.rescheduleAll(context, reason = intent.action ?: "time change", pendingResult = goAsync())
            else -> return
        }
    }
}
