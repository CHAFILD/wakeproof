package com.wakeproof.alarm.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wakeproof.alarm.domain.model.Alarm
import com.wakeproof.alarm.domain.repository.AlarmRepository
import com.wakeproof.alarm.domain.scheduler.AlarmScheduler
import com.wakeproof.alarm.domain.time.NextTriggerCalculator
import com.wakeproof.alarm.receiver.AlarmReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * AlarmManager-backed implementation of [AlarmScheduler].
 *
 * Uses [AlarmManager.setAlarmClock] with an [AlarmManager.AlarmClockInfo]:
 * exact delivery that bypasses Doze and makes the system show the alarm
 * icon in the status bar while a schedule is active (spec section 2).
 * setAlarmClock does not need SCHEDULE_EXACT_ALARM; USE_EXACT_ALARM is
 * declared in the manifest per spec section 3.
 *
 * @param showIntentFactory builds the PendingIntent launched when the
 * user taps the status-bar alarm icon (injected by the DI container so
 * the data layer never references Activity classes).
 */
class AlarmSchedulerImpl(
    private val context: Context,
    private val alarmRepository: AlarmRepository,
    private val showIntentFactory: (alarmId: Long) -> PendingIntent
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Ids currently registered in AlarmManager (process-local mirror). */
    private val scheduledIds = mutableSetOf<Long>()

    /** Guards mirror updates so concurrent syncs cannot interleave. */
    private val syncMutex = Mutex()

    override fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled || alarm.id <= 0) {
            cancel(alarm.id)
            return
        }
        val now = LocalDateTime.now()
        val nextTrigger = NextTriggerCalculator.nextTriggerDateTime(alarm, now)
        if (nextTrigger == null) {
            cancel(alarm.id)
            return
        }
        val triggerAtMillis = nextTrigger
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val fireIntent = alarmPendingIntent(alarm.id)
        alarmManager?.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, showIntentFactory(alarm.id)),
            fireIntent
        )
        synchronized(scheduledIds) { scheduledIds.add(alarm.id) }
        Log.i(TAG, "Scheduled alarm id=${alarm.id} at $nextTrigger")
    }

    override fun cancel(alarmId: Long) {
        alarmManager?.cancel(alarmPendingIntent(alarmId))
        synchronized(scheduledIds) { scheduledIds.remove(alarmId) }
        Log.i(TAG, "Cancelled alarm id=$alarmId")
    }

    override suspend fun rescheduleAllEnabled() {
        val alarms = alarmRepository.observeAlarms().first()
        syncMutex.withLock { mirrorToAlarmManager(alarms) }
    }

    override fun startAutoSync(scope: CoroutineScope) {
        scope.launch {
            alarmRepository.observeAlarms().collect { alarms ->
                syncMutex.withLock { mirrorToAlarmManager(alarms) }
            }
        }
    }

    /**
     * Mirrors the Room table into AlarmManager: every enabled alarm is
     * (re)scheduled, everything that disappeared or became disabled
     * since the previous pass is cancelled. Idempotent, safe to rerun.
     */
    private fun mirrorToAlarmManager(alarms: List<Alarm>) {
        val enabledIds = alarms.filter { it.isEnabled }.map { it.id }.toSet()
        val previouslyScheduled = synchronized(scheduledIds) {
            val copy = scheduledIds.toSet()
            scheduledIds.clear()
            scheduledIds.addAll(enabledIds)
            copy
        }
        // Cancel disabled or deleted alarms (deleted = known id is gone).
        (previouslyScheduled - enabledIds).forEach { cancel(it) }
        alarms.filter { it.isEnabled }.forEach { schedule(it) }
    }

    /**
     * Pending broadcast that fires [AlarmReceiver]. The alarm id rides
     * in the extras; FLAG_UPDATE_CURRENT refreshes them on every
     * schedule. PendingIntents are matched by action + component +
     * requestCode (extras are not part of the identity), so a cancel
     * call targets the same slot regardless of extras.
     */
    private fun alarmPendingIntent(alarmId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_FIRE_ALARM
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        private const val TAG = "AlarmScheduler"
    }
}
