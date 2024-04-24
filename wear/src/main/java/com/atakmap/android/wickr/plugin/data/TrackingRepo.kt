package com.atakmap.android.wickr.plugin.data

import com.atakmap.android.wickr.common.TrackedHrData
import kotlinx.coroutines.flow.Flow

interface TrackingRepo {

    fun hasCapabilities(): Boolean

    suspend fun track(): Flow<TrackerMessage>

    fun stopTracking()

    fun getValidHrData(): ArrayList<TrackedHrData>
}
