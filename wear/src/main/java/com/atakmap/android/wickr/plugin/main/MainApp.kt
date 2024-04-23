package com.atakmap.android.wickr.plugin.main

import android.app.Application
import com.atakmap.android.wickr.plugin.di.koinDataModule
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
