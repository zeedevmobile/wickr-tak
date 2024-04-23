package com.atakmap.android.wickr.wear.domain

import com.atakmap.android.wickr.wear.data.TrackingRepo
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class StopTrackingUseCase : KoinComponent {

    private val trackingRepo: TrackingRepo = get()

    operator fun invoke() {
        trackingRepo.stopTracking()
    }
}
