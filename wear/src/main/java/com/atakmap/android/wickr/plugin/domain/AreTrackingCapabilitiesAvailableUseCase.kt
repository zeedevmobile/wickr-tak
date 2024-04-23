package com.atakmap.android.wickr.plugin.domain

import com.atakmap.android.wickr.plugin.data.TrackingRepo

class AreTrackingCapabilitiesAvailableUseCase(private val trackingRepo: TrackingRepo) {

    operator fun invoke(): Boolean {
        return trackingRepo.hasCapabilities()
    }
}
