package com.atakmap.android.wickr.plugin.domain

import com.atakmap.android.wickr.plugin.data.TrackingRepo

class StopTrackingUseCase(private val trackingRepo: TrackingRepo) {

    operator fun invoke() {
        trackingRepo.stopTracking()
    }
}
