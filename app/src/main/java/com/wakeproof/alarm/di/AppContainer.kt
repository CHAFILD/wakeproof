package com.wakeproof.alarm.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.wakeproof.alarm.MainActivity
import com.wakeproof.alarm.R
import com.wakeproof.alarm.data.local.WakeProofDatabase
import com.wakeproof.alarm.data.repository.AlarmRepositoryImpl
import com.wakeproof.alarm.data.repository.EmergencyPhraseRepositoryImpl
import com.wakeproof.alarm.data.repository.TaskSessionRepositoryImpl
import com.wakeproof.alarm.data.scheduler.AlarmSchedulerImpl
import com.wakeproof.alarm.domain.repository.AlarmRepository
import com.wakeproof.alarm.domain.repository.EmergencyPhraseRepository
import com.wakeproof.alarm.domain.repository.TaskSessionRepository
import com.wakeproof.alarm.domain.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency container (project rule: no Hilt).
 * Access it via [com.wakeproof.alarm.WakeProofApplication].
 */
class AppContainer(context: Context) {

    /** Process-lifetime scope for work that must outlive a screen/service. */
    val applicationScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: WakeProofDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            WakeProofDatabase::class.java,
            WakeProofDatabase.NAME
        )
            .addMigrations(
                WakeProofDatabase.MIGRATION_1_2,
                WakeProofDatabase.MIGRATION_2_3,
                WakeProofDatabase.MIGRATION_3_4
            )
            .build()
    }

    val alarmRepository: AlarmRepository by lazy {
        AlarmRepositoryImpl(database.alarmDao())
    }

    /** Ringing session log (spec 4.1.4 / 6), written by TaskSessionRecorder. */
    val taskSessionRepository: TaskSessionRepository by lazy {
        TaskSessionRepositoryImpl(database.taskSessionDao())
    }

    /** Local emergency exit phrase (spec 4.1.4). */
    val emergencyPhraseRepository: EmergencyPhraseRepository by lazy {
        EmergencyPhraseRepositoryImpl(
            context.applicationContext,
            R.array.emergency_phrase_dictionary,
            database.emergencyPhraseDao()
        )
    }

    val alarmScheduler: AlarmScheduler by lazy {
        AlarmSchedulerImpl(
            context.applicationContext,
            alarmRepository
        ) { alarmId ->
            // Opens the alarm list when the status-bar alarm icon is tapped.
            PendingIntent.getActivity(
                context.applicationContext,
                REQUEST_CODE_SHOW_ALARM_PREFIX + alarmId.toInt(),
                Intent(context.applicationContext, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    companion object {
        /**
         * Keeps show-intent request codes clear of the fire-intent codes
         * (alarm ids). Alarm ids are small autoincrement values.
         */
        private const val REQUEST_CODE_SHOW_ALARM_PREFIX = 100_000
    }
}
