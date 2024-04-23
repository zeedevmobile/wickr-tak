package com.atakmap.android.wickr.wear.data

import com.atakmap.android.wickr.common.TrackedData
import kotlinx.coroutines.flow.Flow

interface TrackingRepo {

    fun hasCapabilities(): Boolean
    suspend fun track(): Flow<TrackerMessage>
    fun stopTracking()
    fun getValidHrData(): ArrayList<TrackedData>
}
