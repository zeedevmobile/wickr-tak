package com.atakmap.android.wickr.plugin.tracking

import com.atakmap.android.wickr.plugin.R
import com.samsung.android.service.health.tracking.HealthTracker.TrackerError
import com.samsung.android.service.health.tracking.HealthTracker.TrackerEventListener
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.ValueKey
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class HeartRateListener internal constructor() : BaseListener(), KoinComponent {

    private val trackerDataNotifier: TrackerDataNotifier = get()

    init {
        val trackerEventListener: TrackerEventListener = object : TrackerEventListener {
            override fun onDataReceived(list: List<DataPoint>) {
                for (dataPoint in list) {
                    readValuesFromDataPoint(dataPoint)
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

    fun readValuesFromDataPoint(dataPoint: DataPoint) {
        val hrData = HeartRateData()
        val hrIbiList = dataPoint.getValue(ValueKey.HeartRateSet.IBI_LIST)
        val hrIbiStatus = dataPoint.getValue(ValueKey.HeartRateSet.IBI_STATUS_LIST)
        hrData.status = dataPoint.getValue(ValueKey.HeartRateSet.HEART_RATE_STATUS)
        hrData.hr = dataPoint.getValue(ValueKey.HeartRateSet.HEART_RATE)
        if (hrIbiList != null && hrIbiList.size != 0) {
            hrData.ibi = hrIbiList[hrIbiList.size - 1] // Inter-Beat Interval (ms)
        }
        if (hrIbiStatus != null && hrIbiStatus.size != 0) {
            hrData.qIbi = hrIbiStatus.size - 1 // 1: bad, 0: good
        }
        trackerDataNotifier.notifyHeartRateTrackerObservers(hrData)
    }
}
