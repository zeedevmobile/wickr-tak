package com.atakmap.android.wickr.common

import kotlinx.serialization.Serializable

const val MESSAGE_PATH_WEAR_HR_DATA = "/msg/wear_hr_data"
const val MESSAGE_PATH_WEAR_SPO2_DATA = "/msg/wear_spo2_data"

@Serializable
data class WearTrackedHrData(
    var hr: Int,
    var abnormal: Boolean
)

@Serializable
data class WearTrackedSpO2Data(
    var sPo2: Int,
    var abnormal: Boolean
)
