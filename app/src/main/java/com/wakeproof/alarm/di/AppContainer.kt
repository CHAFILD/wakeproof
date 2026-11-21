package com.wakeproof.alarm.di

import android.content.Context
import androidx.room.Room
import com.wakeproof.alarm.data.local.WakeProofDatabase
import com.wakeproof.alarm.data.repository.AlarmRepositoryImpl
import com.wakeproof.alarm.domain.repository.AlarmRepository

/**
 * Manual dependency container (project rule: no Hilt).
 * Access it via [com.wakeproof.alarm.WakeProofApplication].
 *
 * Later stages add use cases and services here as single properties.
 */
class AppContainer(context: Context) {

    val database: WakeProofDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            WakeProofDatabase::class.java,
            WakeProofDatabase.NAME
        )
            .addMigrations(WakeProofDatabase.MIGRATION_1_2)
            .build()
    }

    val alarmRepository: AlarmRepository by lazy {
        AlarmRepositoryImpl(database.alarmDao())
    }
}
