package com.atakmap.android.wickr.wear.di

import com.atakmap.android.wickr.wear.data.CapabilityRepo
import com.atakmap.android.wickr.wear.data.DefaultCapabilityRepo
import com.atakmap.android.wickr.wear.data.DefaultMessageRepo
import com.atakmap.android.wickr.wear.data.HealthTrackingServiceConnection
import com.atakmap.android.wickr.wear.data.MessageRepo
import com.atakmap.android.wickr.wear.domain.GetCapableNodes
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
val koinDataModule = module {

    single { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    single { Wearable.getMessageClient(androidContext()) }
    single { Wearable.getCapabilityClient(androidContext()) }

    single<CapabilityRepo> { DefaultCapabilityRepo(get()) }

    single<MessageRepo> { DefaultMessageRepo(get()) }

    single { HealthTrackingServiceConnection(androidContext(), get()) }

    single { GetCapableNodes(get()) }
}
