package com.atakmap.android.wickr.plugin.domain

import com.atakmap.android.wickr.plugin.data.TrackerMessage
import com.atakmap.android.wickr.plugin.data.TrackingRepo
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent

class TrackHeartRateUseCase(private val trackingRepo: TrackingRepo) : KoinComponent {

    suspend operator fun invoke(): Flow<TrackerMessage> = trackingRepo.track()
}
