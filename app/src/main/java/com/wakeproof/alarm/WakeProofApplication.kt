package com.wakeproof.alarm

import android.app.Application
import com.wakeproof.alarm.di.AppContainer
import com.wakeproof.alarm.util.AlarmNotifications

/**
 * Application entry point. Owns the manual DI container so that every
 * component (activities, services, receivers) can reach shared
 * dependencies. On start it creates the notification channel and keeps
 * AlarmManager in sync with the Room alarm table while the process
 * lives (reboot/timezone paths go through the broadcast receivers).
 */
class WakeProofApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        AlarmNotifications.ensureChannels(this)
        appContainer.alarmScheduler.startAutoSync(appContainer.applicationScope)
    }
}
