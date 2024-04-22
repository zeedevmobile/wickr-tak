package com.atakmap.android.wickr.wear.main

import android.app.Application
import com.atakmap.android.wickr.wear.di.koinDataModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // start Koin
        startKoin {
            androidContext(this@MainApp)
            modules(
                listOf(
                    koinDataModule
                )
            )
        }
    }
}
