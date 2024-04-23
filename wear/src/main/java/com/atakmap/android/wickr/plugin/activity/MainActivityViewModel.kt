package com.atakmap.android.wickr.plugin.activity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atakmap.android.wickr.common.TrackedData
import com.atakmap.android.wickr.plugin.data.ConnectionMessage
import com.atakmap.android.wickr.plugin.data.TrackerMessage
import com.atakmap.android.wickr.plugin.domain.AreTrackingCapabilitiesAvailableUseCase
import com.atakmap.android.wickr.plugin.domain.MakeConnectionToHealthTrackingServiceUseCase
import com.atakmap.android.wickr.plugin.domain.SendMessageUseCase
import com.atakmap.android.wickr.plugin.domain.StopTrackingUseCase
import com.atakmap.android.wickr.plugin.domain.TrackHeartRateUseCase
import com.samsung.android.service.health.tracking.HealthTrackerException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

private const val TAG = "MainViewModel"

class MainActivityViewModel : ViewModel(), KoinComponent {

    private val makeConnectionToHealthTrackingServiceUseCase: MakeConnectionToHealthTrackingServiceUseCase =
        get()
    private val areTrackingCapabilitiesAvailableUseCase: AreTrackingCapabilitiesAvailableUseCase =
        get()
    private val stopTrackingUseCase: StopTrackingUseCase = get()
    private val sendMessageUseCase: SendMessageUseCase = get()
    private val trackHeartRateUseCase: TrackHeartRateUseCase = get()

    private val _messageSentToast = MutableSharedFlow<Boolean>()
    val messageSentToast = _messageSentToast.asSharedFlow()

    private val _trackingState = MutableStateFlow(
        TrackingState(
            trackingRunning = false,
            trackingError = false,
            valueHR = "-",
            valueIBI = arrayListOf(),
            message = ""
        )
    )
    val trackingState: StateFlow<TrackingState> = _trackingState

    private val _connectionState =
        MutableStateFlow(ConnectionState(connected = false, message = "", null))
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private var currentHR = "-"
    private var currentIBI = ArrayList<Int>(4)

    fun stopTracking() {
        stopTrackingUseCase()
        trackingJob?.cancel()
        _trackingState.value = TrackingState(
            trackingRunning = false,
            trackingError = false,
            valueHR = "-",
            valueIBI = arrayListOf(),
            message = ""
        )
    }

    fun setUpTracking() {
        Log.i(TAG, "setUpTracking()")
        viewModelScope.launch {
            makeConnectionToHealthTrackingServiceUseCase().collect { connectionMessage ->
                Log.i(TAG, "makeConnectionToHealthTrackingServiceUseCase().collect")
                when (connectionMessage) {
                    is ConnectionMessage.ConnectionSuccessMessage -> {
                        Log.i(TAG, "ConnectionMessage.ConnectionSuccessMessage")
                        _connectionState.value = ConnectionState(
                            connected = true,
                            message = "Connected to Health Tracking Service",
                            connectionException = null
                        )
                    }

                    is ConnectionMessage.ConnectionFailedMessage -> {
                        Log.i(TAG, "Connection: Sth went wrong")
                        _connectionState.value = ConnectionState(
                            connected = false,
                            message = "Connection to Health Tracking Service failed",
                            connectionException = connectionMessage.exception
                        )
                    }

                    is ConnectionMessage.ConnectionEndedMessage -> {
                        Log.i(TAG, "Connection ended")
                        _connectionState.value = ConnectionState(
                            connected = false,
                            message = "Connection ended. Try again later",
                            connectionException = null
                        )
                    }
                }
            }
        }
    }

    fun sendMessage() {
        viewModelScope.launch {
            if (sendMessageUseCase()) {
                _messageSentToast.emit(true)
            } else {
                _messageSentToast.emit(false)
            }
        }
    }

    private fun processExerciseUpdate(trackedData: TrackedData) {

        val hr = trackedData.hr
        val ibi = trackedData.ibi
        Log.i(TAG, "last HeartRate: $hr, last IBI: $ibi")
        currentHR = hr.toString()
        currentIBI = ibi

        _trackingState.value = TrackingState(
            trackingRunning = true,
            trackingError = false,
            valueHR = if (hr > 0) hr.toString() else "-",
            valueIBI = ibi,
            message = ""
        )
    }

    private var trackingJob: Job? = null

    fun startTracking() {
        trackingJob?.cancel()
        Log.i(TAG, "trackHeartRate()")
        if (areTrackingCapabilitiesAvailableUseCase()) {
            trackingJob = viewModelScope.launch {
                trackHeartRateUseCase().collect { trackerMessage ->
                    when (trackerMessage) {
                        is TrackerMessage.DataMessage -> {
                            processExerciseUpdate(trackerMessage.trackedData)
                            Log.i(TAG, "TrackerMessage.DataReceivedMessage")
                        }

                        is TrackerMessage.FlushCompletedMessage -> {
                            Log.i(TAG, "TrackerMessage.FlushCompletedMessage")
                            _trackingState.value = TrackingState(
                                trackingRunning = false,
                                trackingError = false,
                                valueHR = "-",
                                valueIBI = arrayListOf(),
                                message = ""
                            )
                        }

                        is TrackerMessage.TrackerErrorMessage -> {
                            Log.i(TAG, "TrackerMessage.TrackerErrorMessage")
                            _trackingState.value = TrackingState(
                                trackingRunning = false,
                                trackingError = true,
                                valueHR = "-",
                                valueIBI = arrayListOf(),
                                message = trackerMessage.trackerError
                            )
                        }

                        is TrackerMessage.TrackerWarningMessage -> {
                            Log.i(TAG, "TrackerMessage.TrackerWarningMessage")
                            _trackingState.value = TrackingState(
                                trackingRunning = true,
                                trackingError = false,
                                valueHR = "-",
                                valueIBI = currentIBI,
                                message = trackerMessage.trackerWarning
                            )
                        }
                    }
                }
            }
        } else {
            _trackingState.value = TrackingState(
                trackingRunning = false,
                trackingError = true,
                valueHR = "-",
                valueIBI = arrayListOf(),
                message = "HR tracking capabilities not available"
            )
        }
    }
}

data class ConnectionState(
    val connected: Boolean, val message: String, val connectionException: HealthTrackerException?
)

data class TrackingState(
    val trackingRunning: Boolean,
    val trackingError: Boolean,
    val valueHR: String,
    val valueIBI: ArrayList<Int>,
    val message: String
)
