package com.atakmap.android.wickr.plugin.di

import com.atakmap.android.wickr.plugin.data.CapabilityRepo
import com.atakmap.android.wickr.plugin.data.DefaultCapabilityRepo
import com.atakmap.android.wickr.plugin.data.DefaultMessageRepo
import com.atakmap.android.wickr.plugin.data.GetCapableNodes
import com.atakmap.android.wickr.plugin.data.MessageRepo
import com.atakmap.android.wickr.plugin.tracking.TrackerDataNotifier
import com.google.android.gms.wearable.Wearable
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val koinDataModule = module {

    single<CapabilityRepo> { DefaultCapabilityRepo(Wearable.getCapabilityClient(androidContext())) }

    single<MessageRepo> { DefaultMessageRepo(Wearable.getMessageClient(androidContext())) }

    single { GetCapableNodes(get()) }

    single { TrackerDataNotifier() }
}
