package com.atakmap.android.wickr.plugin.data

import android.content.Context
import android.util.Log
import com.atakmap.android.wickr.common.TrackedHealthData
import com.atakmap.android.wickr.plugin.R
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@OptIn(ExperimentalCoroutinesApi::class)

class DefaultTrackingRepo(
    private val coroutineScope: CoroutineScope,
    private val healthTrackingServiceConnection: HealthTrackingServiceConnection,
    private val context: Context,
) : TrackingRepo {

    companion object {
        private const val TAG = "DefaultTrackingRepo"
    }

    private val trackingType = HealthTrackerType.HEART_RATE
    private var listenerSet = false
    private var healthTrackingService: HealthTrackingService? = null

    var errors: HashMap<String, Int> = hashMapOf(
        "0" to R.string.error_initial_state,
        "-2" to R.string.error_wearable_movement_detected,
        "-3" to R.string.error_wearable_detached,
        "-8" to R.string.error_low_ppg_signal,
        "-10" to R.string.error_low_ppg_signal_even_more,
        "-999" to R.string.error_other_sensor_running,
        "SDK_POLICY_ERROR" to R.string.SDK_POLICY_ERROR,
        "PERMISSION_ERROR" to R.string.PERMISSION_ERROR
    )

    private val maxValuesToKeep = 40
    private var heartRateTracker: HealthTracker? = null
    private var validHrData = ArrayList<TrackedHealthData>()

    override fun getValidHrData(): ArrayList<TrackedHealthData> {
        return validHrData
    }

    private fun isHRValid(hrStatus: Int): Boolean {
        return hrStatus == 1
    }

    private fun trimDataList() {
        val howManyElementsToRemove = validHrData.size - maxValuesToKeep
        repeat(howManyElementsToRemove) { validHrData.removeFirstOrNull() }
    }

    @ExperimentalCoroutinesApi
    override suspend fun track(): Flow<TrackerMessage> = callbackFlow {
        val updateListener = object : HealthTracker.TrackerEventListener {
            override fun onDataReceived(dataPoints: MutableList<DataPoint>) {

            /*    for (dataPoint in dataPoints) {
                    var trackedData: TrackedHealthData? = null
                    val hrValue = dataPoint.getValue(ValueKey.HeartRateSet.HEART_RATE)
                    val hrStatus = dataPoint.getValue(ValueKey.HeartRateSet.HEART_RATE_STATUS)

                    if (isHRValid(hrStatus)) {
                        trackedData = TrackedHealthData()
                        trackedData.hr = hrValue
                        Log.i(TAG, "valid HR: $hrValue")
                    } else {
                        coroutineScope.runCatching {
                            trySendBlocking(TrackerMessage.TrackerWarningMessage(getError(hrStatus.toString())))
                        }
                    }

                    val validIbiList = getValidIbiList(dataPoint)
                    if (validIbiList.size > 0) {
                        if (trackedData == null) trackedData = TrackedHealthData()
                        trackedData.ibi.addAll(validIbiList)
                    }

                    if ((isHRValid(hrStatus) || validIbiList.size > 0) && trackedData != null) {
                        coroutineScope.runCatching {
                            trySendBlocking(TrackerMessage.DataMessage(trackedData))
                        }
                    }
                    if (trackedData != null) {
                        validHrData.add(trackedData)
                    }
                }
                trimDataList()*/
            }

            fun getError(errorKeyFromTracker: String): String {
                val str = errors.getValue(errorKeyFromTracker)
                return context.resources.getString(str)
            }

            override fun onFlushCompleted() {
                Log.i(TAG, "onFlushCompleted()")
                coroutineScope.runCatching {
                    trySendBlocking(TrackerMessage.FlushCompletedMessage)
                }
            }

            override fun onError(trackerError: HealthTracker.TrackerError?) {
                Log.i(TAG, "onError()")
                coroutineScope.runCatching {
                    trySendBlocking(TrackerMessage.TrackerErrorMessage(getError(trackerError.toString())))
                }
            }
        }

        heartRateTracker = healthTrackingService!!.getHealthTracker(trackingType)

        setListener(updateListener)

        awaitClose {
            Log.i(TAG, "Tracking flow awaitClose()")
            stopTracking()
        }
    }

    override fun stopTracking() {
        unsetListener()
    }

    private fun unsetListener() {
        if (listenerSet) {
            heartRateTracker?.unsetEventListener()
            listenerSet = false
        }
    }

    private fun setListener(listener: HealthTracker.TrackerEventListener) {
        if (!listenerSet) {
            heartRateTracker?.setEventListener(listener)
            listenerSet = true
        }
    }

    override fun hasCapabilities(): Boolean {
        Log.i(TAG, "hasCapabilities()")
        healthTrackingService = healthTrackingServiceConnection.getHealthTrackingService()
        val trackers: List<HealthTrackerType> =
            healthTrackingService!!.trackingCapability.supportHealthTrackerTypes
        return trackers.contains(trackingType)
    }
}

sealed class TrackerMessage {
    class DataMessage(val trackedData: TrackedHealthData) : TrackerMessage()
    object FlushCompletedMessage : TrackerMessage()
    class TrackerErrorMessage(val trackerError: String) : TrackerMessage()
    class TrackerWarningMessage(val trackerWarning: String) : TrackerMessage()
}
