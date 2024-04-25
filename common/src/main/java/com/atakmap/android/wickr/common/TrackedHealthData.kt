package com.atakmap.android.wickr.common

import kotlinx.serialization.Serializable

@Serializable
data class TrackedHealthData(
    var hr: Int?,
    var spO2: Int?,
    var hrAlert: Int?,
    var spO2Alert: Int?
)
