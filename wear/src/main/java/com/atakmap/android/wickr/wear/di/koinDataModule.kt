package com.atakmap.android.wickr.wear.di

import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.atakmap.android.wickr.wear.data.CapabilityRepo
import com.atakmap.android.wickr.wear.data.DefaultCapabilityRepo
import com.atakmap.android.wickr.wear.data.DefaultMessageRepo
import com.atakmap.android.wickr.wear.data.MessageRepo
import com.google.android.gms.wearable.Wearable
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val koinDataModule = module {

    single<SharedPreferences> {
        PreferenceManager.getDefaultSharedPreferences(
            androidContext().createDeviceProtectedStorageContext()
        )
    }

    single<CapabilityRepo> { DefaultCapabilityRepo(Wearable.getCapabilityClient(androidContext())) }

    single<MessageRepo> { DefaultMessageRepo(Wearable.getMessageClient(androidContext())) }
}
