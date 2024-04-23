package com.atakmap.android.wickr.wear.domain

import com.atakmap.android.wickr.wear.data.TrackingRepo
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class AreTrackingCapabilitiesAvailableUseCase : KoinComponent {

    private val trackingRepo: TrackingRepo = get()

    operator fun invoke(): Boolean {
        return trackingRepo.hasCapabilities()
    }
}
