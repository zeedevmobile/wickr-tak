package com.atakmap.android.wickr.wear.domain

import com.atakmap.android.wickr.wear.data.ConnectionMessage
import com.atakmap.android.wickr.wear.data.HealthTrackingServiceConnection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class MakeConnectionToHealthTrackingServiceUseCase : KoinComponent {

    private val healthTrackingServiceConnection: HealthTrackingServiceConnection = get()

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<ConnectionMessage> = healthTrackingServiceConnection.connectionFlow
}
