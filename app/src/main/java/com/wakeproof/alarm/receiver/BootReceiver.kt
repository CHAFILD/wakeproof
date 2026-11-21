package com.wakeproof.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms every enabled alarm after the device finishes booting
 * (spec section 5: schedules survive reboots; AlarmManager pending
 * intents do not). Registered for BOOT_COMPLETED only; the missed-alarm
 * detection of spec section 5 arrives with the final polish stage.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        AlarmRescheduler.rescheduleAll(context, reason = "boot", pendingResult = goAsync())
    }
}
