package com.atakmap.android.wickr.plugin.di

import com.atakmap.android.wickr.plugin.data.CapabilityRepo
import com.atakmap.android.wickr.plugin.data.DefaultCapabilityRepo
import com.atakmap.android.wickr.plugin.data.DefaultMessageRepo
import com.atakmap.android.wickr.plugin.data.DefaultTrackingRepo
import com.atakmap.android.wickr.plugin.data.HealthTrackingServiceConnection
import com.atakmap.android.wickr.plugin.data.MessageRepo
import com.atakmap.android.wickr.plugin.data.TrackingRepo
import com.atakmap.android.wickr.plugin.domain.AreTrackingCapabilitiesAvailableUseCase
import com.atakmap.android.wickr.plugin.domain.GetCapableNodes
import com.atakmap.android.wickr.plugin.domain.MakeConnectionToHealthTrackingServiceUseCase
import com.atakmap.android.wickr.plugin.domain.SendMessageUseCase
import com.atakmap.android.wickr.plugin.domain.StopTrackingUseCase
import com.atakmap.android.wickr.plugin.domain.TrackHeartRateUseCase
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

    single<CapabilityRepo> { DefaultCapabilityRepo(Wearable.getCapabilityClient(androidContext())) }

    single<MessageRepo> { DefaultMessageRepo(Wearable.getMessageClient(androidContext())) }

    single<TrackingRepo> { DefaultTrackingRepo(get(), get(), get()) }

    single { HealthTrackingServiceConnection(androidContext(), get()) }

    single { GetCapableNodes(get()) }

    single { AreTrackingCapabilitiesAvailableUseCase(get()) }

    single { MakeConnectionToHealthTrackingServiceUseCase(get()) }

    single { SendMessageUseCase(get(), get(), get()) }

    single { TrackHeartRateUseCase(get()) }

    single { StopTrackingUseCase(get()) }
}
