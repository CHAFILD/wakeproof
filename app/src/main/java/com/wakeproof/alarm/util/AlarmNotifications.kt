package com.wakeproof.alarm.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wakeproof.alarm.R
import com.wakeproof.alarm.presentation.ui.ringing.RingingActivity

/**
 * Notification plumbing for the ringing foreground service (spec 4.1.3,
 * stage 2): one channel «Будильник» and the dedicated FGS notification
 * that carries the full-screen intent launching [RingingActivity] over
 * the lock screen.
 *
 * The channel itself is silent and vibration-free: sound is played by
 * the service through AudioAttributes USAGE_ALARM and vibration follows
 * the volume ramp (spec 4.1.5), so a channel sound would only double it.
 */
object AlarmNotifications {

    const val CHANNEL_RINGING = "alarm_ringing"
    const val RINGING_NOTIFICATION_ID = 1001

    private const val REQUEST_CODE_FULLSCREEN = 2001

    /** Creates the channel; safe to call on every app start. */
    fun ensureChannels(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_RINGING,
            context.getString(R.string.ringing_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.ringing_channel_description)
            setSound(null, null)
            enableVibration(false)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** The FGS notification shown while the alarm is ringing. */
    fun buildRingingNotification(context: Context, alarmId: Long): android.app.Notification {
        val fullScreenIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_FULLSCREEN,
            RingingActivity.launchIntent(context, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_RINGING)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(context.getString(R.string.ringing_notification_title))
            .setContentText(context.getString(R.string.ringing_notification_text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            // Full lock-screen visibility: the whole screen is the UI anyway.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .build()
    }

    /** Re-posts the ringing notification to re-fire the full-screen intent. */
    fun repostRinging(context: Context, alarmId: Long) {
        try {
            NotificationManagerCompat.from(context)
                .notify(RINGING_NOTIFICATION_ID, buildRingingNotification(context, alarmId))
        } catch (security: SecurityException) {
            // POST_NOTIFICATIONS revoked: the full-screen intent is still
            // delivered, only the collapsed notification is dropped.
        }
    }

    /** Cancels the ringing notification (alarm dismissed). */
    fun cancelRinging(context: Context) {
        NotificationManagerCompat.from(context).cancel(RINGING_NOTIFICATION_ID)
    }
}
