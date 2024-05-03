package com.atakmap.android.wickr.plugin.multisensortracking

import com.atakmap.android.wickr.plugin.R
import com.samsung.android.service.health.tracking.HealthTracker.TrackerError
import com.samsung.android.service.health.tracking.HealthTracker.TrackerEventListener
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.ValueKey
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class SpO2Listener internal constructor() : BaseListener(), KoinComponent {

    private val trackerDataNotifier: TrackerDataNotifier = get()

    init {
        val trackerEventListener: TrackerEventListener = object : TrackerEventListener {
            override fun onDataReceived(list: List<DataPoint>) {
                for (data in list) {
                    updateSpo2(data)
                }
            }

            override fun onFlushCompleted() {

            }

            override fun onError(trackerError: TrackerError) {
                setHandlerRunning(false)
                if (trackerError == TrackerError.PERMISSION_ERROR) {
                    trackerDataNotifier.notifyError(R.string.NoPermission)
                }
                if (trackerError == TrackerError.SDK_POLICY_ERROR) {
                    trackerDataNotifier.notifyError(R.string.SdkPolicyError)
                }
            }
        }

        setTrackerEventListener(trackerEventListener)
    }

    fun updateSpo2(dataPoint: DataPoint) {
        val status = dataPoint.getValue(ValueKey.SpO2Set.STATUS)
        var spo2Value = 0
        if (status == SpO2Status.MEASUREMENT_COMPLETED) spo2Value =
            dataPoint.getValue(ValueKey.SpO2Set.SPO2)
        trackerDataNotifier.notifySpO2TrackerObservers(status, spo2Value)
    }
}
