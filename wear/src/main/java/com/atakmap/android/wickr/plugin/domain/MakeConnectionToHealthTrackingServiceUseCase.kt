package com.atakmap.android.wickr.plugin.domain

import com.atakmap.android.wickr.plugin.data.ConnectionMessage
import com.atakmap.android.wickr.plugin.data.HealthTrackingServiceConnection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalCoroutinesApi::class)
class MakeConnectionToHealthTrackingServiceUseCase(private val healthTrackingServiceConnection: HealthTrackingServiceConnection) {

    operator fun invoke(): Flow<ConnectionMessage> = healthTrackingServiceConnection.connectionFlow
}
