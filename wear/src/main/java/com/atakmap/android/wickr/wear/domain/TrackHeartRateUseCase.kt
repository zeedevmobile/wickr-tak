package com.atakmap.android.wickr.wear.domain

import com.atakmap.android.wickr.wear.data.TrackerMessage
import com.atakmap.android.wickr.wear.data.TrackingRepo
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class TrackHeartRateUseCase : KoinComponent {

    private val trackingRepo: TrackingRepo = get()

    suspend operator fun invoke(): Flow<TrackerMessage> = trackingRepo.track()
}
