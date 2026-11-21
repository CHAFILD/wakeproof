package com.wakeproof.alarm

import android.app.Application
import com.wakeproof.alarm.di.AppContainer

/**
 * Application entry point. Owns the manual DI container so that every
 * component (activities, services, receivers) can reach shared dependencies.
 */
class WakeProofApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
