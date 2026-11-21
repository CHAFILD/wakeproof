package com.wakeproof.alarm.di

import android.content.Context
import androidx.room.Room
import com.wakeproof.alarm.data.local.WakeProofDatabase

/**
 * Manual dependency container (project rule: no Hilt).
 * Access it via [com.wakeproof.alarm.WakeProofApplication].
 *
 * Later stages add repositories and use cases here as single properties.
 */
class AppContainer(context: Context) {

    val database: WakeProofDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            WakeProofDatabase::class.java,
            WakeProofDatabase.NAME
        ).build()
    }
}
